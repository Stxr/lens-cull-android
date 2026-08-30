package com.stxr.lenscull.metadata

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface MetadataWriteResult {
  data object Synced : MetadataWriteResult
  data class LocalOnly(val reason: String) : MetadataWriteResult
  data class Conflict(val reason: String) : MetadataWriteResult
  data class Failed(val reason: String) : MetadataWriteResult
}

class MetadataWriter(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  suspend fun writeRating(photo: PhotoAsset, rating: Int): MetadataWriteResult = withContext(ioDispatcher) {
    require(rating in 0..5)
    val source = File(photo.canonicalPath)
    if (!source.isFile) return@withContext MetadataWriteResult.Failed("照片文件不存在")
    when {
      photo.format.supportsEmbeddedRating -> writeEmbedded(source, rating)
      photo.format.isRaw -> writeSidecar(source, photo.format, rating)
      else -> MetadataWriteResult.LocalOnly("${photo.format.displayName} 暂不支持安全写回")
    }
  }

  private fun writeEmbedded(source: File, rating: Int): MetadataWriteResult {
    if (!source.canWrite()) return MetadataWriteResult.Failed("照片文件不可写")
    val suffix = ".${source.extension.ifBlank { "img" }}"
    val temporary = File.createTempFile(".${source.name}.lenscull-", suffix, source.parentFile)
    var stage = "复制临时文件"
    return try {
      Files.copy(source.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING)
      stage = "合并 XMP"
      val exif = ExifInterface(temporary)
      val merged = XmpRating.merge(exif.getAttributeBytes(ExifInterface.TAG_XMP), rating)
      exif.setAttribute(ExifInterface.TAG_XMP, merged.toString(Charsets.UTF_8))
      stage = "保存 XMP"
      exif.saveAttributes()
      stage = "回读评分"
      val verified = XmpRating.read(ExifInterface(temporary).getAttributeBytes(ExifInterface.TAG_XMP))
      check(verified == rating) { "评分写入校验失败" }
      stage = "校验图片"
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(temporary.absolutePath, bounds)
      check(bounds.outWidth > 0 && bounds.outHeight > 0) { "评分写入后图片无法解码" }
      stage = "同步并替换原文件"
      FileChannel.open(temporary.toPath(), StandardOpenOption.WRITE).use { it.force(true) }
      atomicReplace(temporary, source)
      MetadataWriteResult.Synced
    } catch (error: XmpFormatException) {
      MetadataWriteResult.Failed("现有 XMP 无法解析，原文件未修改")
    } catch (error: AtomicMoveNotSupportedException) {
      MetadataWriteResult.Failed("当前存储不支持安全的原子替换")
    } catch (error: Exception) {
      MetadataWriteResult.Failed("$stage：${error.message ?: "写入评分失败"}")
    } finally {
      temporary.delete()
    }
  }

  private fun writeSidecar(source: File, format: PhotoFormat, rating: Int): MetadataWriteResult {
    val rawSiblings = source.parentFile?.listFiles().orEmpty().count {
      it.nameWithoutExtension.equals(source.nameWithoutExtension, ignoreCase = true) &&
        it.extension.lowercase() in setOf("rw2", "dng")
    }
    if (rawSiblings > 1) {
      return MetadataWriteResult.Conflict("同名 RAW 文件会争用一份 XMP sidecar")
    }
    if (format !in setOf(PhotoFormat.RW2, PhotoFormat.DNG)) {
      return MetadataWriteResult.LocalOnly("此格式不使用 XMP sidecar")
    }
    val sidecar = File(source.parentFile, "${source.nameWithoutExtension}.xmp")
    val temporary = File(source.parentFile, ".${sidecar.name}.lenscull.tmp")
    return try {
      val existing = sidecar.takeIf(File::isFile)?.readBytes()
      val merged = XmpRating.merge(existing, rating)
      temporary.outputStream().use { output ->
        output.write(merged)
        output.flush()
      }
      FileChannel.open(temporary.toPath(), StandardOpenOption.WRITE).use { it.force(true) }
      check(XmpRating.read(temporary.readBytes()) == rating) { "sidecar 评分校验失败" }
      atomicReplace(temporary, sidecar)
      MetadataWriteResult.Synced
    } catch (error: XmpFormatException) {
      MetadataWriteResult.Failed("现有 sidecar 无法解析，未覆盖")
    } catch (error: AtomicMoveNotSupportedException) {
      MetadataWriteResult.Failed("当前存储不支持安全的原子替换")
    } catch (error: Exception) {
      MetadataWriteResult.Failed(error.message ?: "sidecar 写入失败")
    } finally {
      temporary.delete()
    }
  }

  private fun atomicReplace(temporary: File, destination: File) {
    Files.move(
      temporary.toPath(),
      destination.toPath(),
      StandardCopyOption.ATOMIC_MOVE,
      StandardCopyOption.REPLACE_EXISTING,
    )
  }
}
