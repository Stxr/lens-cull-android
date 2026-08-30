package com.stxr.lenscull.preview

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RawPreviewExtractor(
  context: Context,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val cacheDirectory = File(context.cacheDir, "raw-previews").apply { mkdirs() }

  suspend fun resolve(photo: PhotoAsset): File = withContext(ioDispatcher) {
    val source = File(photo.canonicalPath)
    if (!photo.format.isRaw) return@withContext source
    val cacheKey = sha256("${photo.canonicalPath}:${photo.fileSizeBytes}:${photo.modifiedAt}")
    val cached = File(cacheDirectory, "$cacheKey.jpg")
    if (cached.isFile && cached.length() > 0) return@withContext cached
    val exif = ExifInterface(source)
    val bytes = when (photo.format) {
      PhotoFormat.RW2 -> exif.getAttributeBytes(ExifInterface.TAG_RW2_JPG_FROM_RAW) ?: exif.thumbnailBytes
      PhotoFormat.DNG -> exif.thumbnailBytes
      else -> null
    } ?: error("RAW 文件没有可用的内嵌 JPEG 预览")
    require(bytes.size > 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
      "内嵌预览不是 JPEG"
    }
    val temporary = File(cacheDirectory, "$cacheKey.tmp")
    temporary.writeBytes(bytes)
    if (!temporary.renameTo(cached)) {
      temporary.delete()
      error("无法缓存 RAW 预览")
    }
    cached
  }

  private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
