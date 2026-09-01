package com.stxr.lenscull.scan

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import com.stxr.lenscull.data.db.PhotoDao
import com.stxr.lenscull.data.db.PhotoEntity
import com.stxr.lenscull.data.db.ProjectPhotoEntity
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState
import com.stxr.lenscull.metadata.ExifMetadataReader
import java.io.File
import java.net.URLConnection
import java.nio.file.Files
import java.util.ArrayDeque
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class ScanProgress(val scanned: Int, val indexed: Int, val failures: Int)
data class ScanResult(val indexed: Int, val failures: Int)

class StorageScanner(
  private val context: Context,
  private val photoDao: PhotoDao,
  private val metadataReader: ExifMetadataReader,
) {
  suspend fun scan(projectId: String, sourcePath: String?, onProgress: suspend (ScanProgress) -> Unit): ScanResult {
    check(Environment.isExternalStorageManager()) { "需要授予管理所有文件权限" }
    val generation = UUID.randomUUID().toString()
    val roots = sourcePath?.let { path ->
      val directory = File(path)
      check(directory.isDirectory && directory.canRead()) { "无法读取项目目录：$path" }
      listOf(directory to volumeName(directory))
    } ?: storageRoots()
    var scanned = 0
    var indexed = 0
    var failures = 0
    val batch = mutableListOf<PhotoEntity>()
    val projectBatch = mutableListOf<ProjectPhotoEntity>()
    try {
      roots.forEach { root ->
        walk(root) { file, volumeName ->
          currentCoroutineContext().ensureActive()
          scanned += 1
          val format = PhotoFormatClassifier.fromFileName(file.name) ?: return@walk
          val id = "$volumeName:${file.absolutePath}"
          val existing = photoDao.getById(id)
          val entity = runCatching {
            val parsed = metadataReader.read(file, format)
            val externalRating = if (format.isRaw) metadataReader.readSidecarRating(file) else parsed.rating
            val preserveLocal = existing?.ratingSyncState in setOf(
              RatingSyncState.LOCAL_ONLY,
              RatingSyncState.PENDING,
              RatingSyncState.FAILED,
              RatingSyncState.CONFLICT,
            )
            PhotoEntity(
              id = id,
              volumeName = volumeName,
              canonicalPath = file.absolutePath,
              displayName = file.name,
              parentPath = file.parent.orEmpty(),
              format = format,
              mimeType = mimeType(file, format),
              fileSizeBytes = file.length(),
              modifiedAt = file.lastModified(),
              capturedAt = parsed.exif.capturedAt,
              rating = if (preserveLocal) existing!!.rating else externalRating ?: existing?.rating ?: 0,
              flag = existing?.flag ?: com.stxr.lenscull.domain.CullFlag.UNFLAGGED,
              ratingSyncState = if (preserveLocal) existing!!.ratingSyncState else RatingSyncState.SYNCED,
              previewState = existing?.previewState ?: PreviewState.UNKNOWN,
              previewError = existing?.previewError,
              width = parsed.exif.width,
              height = parsed.exif.height,
              cameraMake = parsed.exif.cameraMake,
              cameraModel = parsed.exif.cameraModel,
              lensModel = parsed.exif.lensModel,
              focalLengthMm = parsed.exif.focalLengthMm,
              focalLength35Mm = parsed.exif.focalLength35Mm,
              aperture = parsed.exif.aperture,
              exposureSeconds = parsed.exif.exposureSeconds,
              iso = parsed.exif.iso,
              latitude = parsed.exif.latitude,
              longitude = parsed.exif.longitude,
              orientationDegrees = parsed.exif.orientationDegrees,
              scanGeneration = generation,
            )
          }.getOrElse { error ->
            failures += 1
            PhotoEntity(
              id = id,
              volumeName = volumeName,
              canonicalPath = file.absolutePath,
              displayName = file.name,
              parentPath = file.parent.orEmpty(),
              format = format,
              mimeType = mimeType(file, format),
              fileSizeBytes = file.length(),
              modifiedAt = file.lastModified(),
              capturedAt = existing?.capturedAt,
              rating = existing?.rating ?: 0,
              flag = existing?.flag ?: com.stxr.lenscull.domain.CullFlag.UNFLAGGED,
              ratingSyncState = existing?.ratingSyncState ?: RatingSyncState.LOCAL_ONLY,
              previewState = PreviewState.FAILED,
              previewError = error.message ?: "无法读取照片",
              scanGeneration = generation,
            )
          }
          batch += entity
          projectBatch += ProjectPhotoEntity(projectId, id, generation)
          indexed += 1
          if (batch.size >= BATCH_SIZE) {
            photoDao.upsertAll(batch.toList())
            photoDao.upsertProjectPhotos(projectBatch.toList())
            batch.clear()
            projectBatch.clear()
            onProgress(ScanProgress(scanned, indexed, failures))
          }
        }
      }
      if (batch.isNotEmpty()) {
        photoDao.upsertAll(batch)
        photoDao.upsertProjectPhotos(projectBatch)
      }
      photoDao.deleteProjectPhotosNotSeen(projectId, generation)
      onProgress(ScanProgress(scanned, indexed, failures))
      return ScanResult(indexed, failures)
    } catch (cancelled: CancellationException) {
      throw cancelled
    }
  }

  private fun storageRoots(): List<Pair<File, String>> {
    val manager = context.getSystemService(StorageManager::class.java)
    val volumes = manager.storageVolumes.mapNotNull { volume ->
      volume.directory?.takeIf(File::canRead)?.let { it to (volume.uuid ?: "primary") }
    }
    return volumes.distinctBy { runCatching { it.first.canonicalPath }.getOrDefault(it.first.absolutePath) }
  }

  private fun volumeName(file: File): String {
    val path = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
    return storageRoots().firstOrNull { (root, _) -> path.startsWith(root.absolutePath.trimEnd('/') + "/") || path == root.absolutePath }?.second
      ?: "selected"
  }

  private suspend fun walk(root: Pair<File, String>, onFile: suspend (File, String) -> Unit) {
    val queue = ArrayDeque<File>()
    queue += root.first
    while (queue.isNotEmpty()) {
      currentCoroutineContext().ensureActive()
      val directory = queue.removeFirst()
      if (shouldSkip(directory)) continue
      directory.listFiles()?.forEach { child ->
        when {
          child.isDirectory && !Files.isSymbolicLink(child.toPath()) -> queue += child
          child.isFile -> onFile(child, root.second)
        }
      }
    }
  }

  private fun shouldSkip(directory: File): Boolean {
    val normalized = directory.absolutePath.replace('\\', '/')
    return normalized.contains("/Android/data/") ||
      normalized.endsWith("/Android/data") ||
      normalized.contains("/Android/obb/") ||
      normalized.endsWith("/Android/obb")
  }

  private fun mimeType(file: File, format: com.stxr.lenscull.domain.PhotoFormat): String =
    URLConnection.guessContentTypeFromName(file.name) ?: format.mimeTypes.first()

  private companion object { const val BATCH_SIZE = 50 }
}
