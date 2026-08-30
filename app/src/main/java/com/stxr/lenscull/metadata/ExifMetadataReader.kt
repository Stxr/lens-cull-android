package com.stxr.lenscull.metadata

import androidx.exifinterface.media.ExifInterface
import com.stxr.lenscull.domain.ExifSummary
import com.stxr.lenscull.domain.PhotoFormat
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ParsedMetadata(val exif: ExifSummary, val rating: Int?)

class ExifMetadataReader {
  fun read(file: File, format: PhotoFormat): ParsedMetadata {
    val exif = ExifInterface(file)
    val dimensions = readDimensions(exif)
    val latLong = exif.latLong
    val capturedAt = readCapturedAt(exif)
    val iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
      .takeIf { it > 0 }
      ?: if (format == PhotoFormat.RW2) {
        exif.getAttributeInt(ExifInterface.TAG_RW2_ISO, 0).takeIf { it > 0 }
      } else null
    val embeddedRating = runCatching {
      XmpRating.read(exif.getAttributeBytes(ExifInterface.TAG_XMP))
    }.getOrNull()
    return ParsedMetadata(
      exif = ExifSummary(
        width = dimensions.first,
        height = dimensions.second,
        cameraMake = exif.clean(ExifInterface.TAG_MAKE),
        cameraModel = exif.clean(ExifInterface.TAG_MODEL),
        lensModel = exif.clean(ExifInterface.TAG_LENS_MODEL),
        focalLengthMm = exif.positiveDouble(ExifInterface.TAG_FOCAL_LENGTH),
        focalLength35Mm = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0).takeIf { it > 0 },
        aperture = exif.positiveDouble(ExifInterface.TAG_F_NUMBER),
        exposureSeconds = exif.positiveDouble(ExifInterface.TAG_EXPOSURE_TIME),
        iso = iso,
        capturedAt = capturedAt,
        latitude = latLong?.getOrNull(0),
        longitude = latLong?.getOrNull(1),
        orientationDegrees = exif.rotationDegrees,
      ),
      rating = embeddedRating,
    )
  }

  fun readSidecarRating(photo: File): Int? {
    val sidecar = sidecarFor(photo)
    if (!sidecar.isFile) return null
    return XmpRating.read(sidecar.readBytes())
  }

  fun sidecarFor(photo: File): File = File(photo.parentFile, "${photo.nameWithoutExtension}.xmp")

  private fun readDimensions(exif: ExifInterface): Pair<Int?, Int?> {
    val width = exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0)
      .takeIf { it > 0 }
      ?: exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).takeIf { it > 0 }
    val height = exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0)
      .takeIf { it > 0 }
      ?: exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).takeIf { it > 0 }
    return width to height
  }

  private fun readCapturedAt(exif: ExifInterface): Long? {
    val raw = exif.clean(ExifInterface.TAG_DATETIME_ORIGINAL)
      ?: exif.clean(ExifInterface.TAG_DATETIME_DIGITIZED)
      ?: exif.clean(ExifInterface.TAG_DATETIME)
      ?: return null
    return runCatching {
      val local = LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US))
      val offset = exif.clean(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
        ?.let { runCatching { ZoneOffset.of(it) }.getOrNull() }
      if (offset != null) local.toInstant(offset).toEpochMilli()
      else local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
  }

  private fun ExifInterface.clean(tag: String): String? =
    getAttribute(tag)?.trim()?.takeIf { it.isNotEmpty() }

  private fun ExifInterface.positiveDouble(tag: String): Double? =
    getAttributeDouble(tag, Double.NaN).takeIf { it.isFinite() && it > 0.0 }
}
