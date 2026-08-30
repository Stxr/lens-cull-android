package com.stxr.lenscull.metadata

import com.stxr.lenscull.domain.ExifSummary
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ExifDisplayRow(val label: String, val value: String)

object ExifDisplayFormatter {
  fun rows(exif: ExifSummary, fileSizeBytes: Long): List<ExifDisplayRow> = buildList {
    if (exif.width != null && exif.height != null) add(ExifDisplayRow("分辨率", "${exif.width} × ${exif.height}"))
    add(ExifDisplayRow("大小", formatBytes(fileSizeBytes)))
    listOfNotNull(exif.cameraMake, exif.cameraModel).distinct().joinToString(" ").takeIf(String::isNotBlank)
      ?.let { add(ExifDisplayRow("相机", it)) }
    exif.lensModel?.let { add(ExifDisplayRow("镜头", it)) }
    exif.focalLengthMm?.let { focal ->
      val equivalent = exif.focalLength35Mm?.let { "（等效 ${it}mm）" }.orEmpty()
      add(ExifDisplayRow("焦距", "${trim(focal)}mm$equivalent"))
    }
    exif.aperture?.let { add(ExifDisplayRow("光圈", "f/${trim(it)}")) }
    exif.exposureSeconds?.let { add(ExifDisplayRow("快门", formatExposure(it))) }
    exif.iso?.let { add(ExifDisplayRow("ISO", it.toString())) }
    exif.capturedAt?.let {
      add(ExifDisplayRow("拍摄时间", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(it))))
    }
    if (exif.latitude != null && exif.longitude != null) {
      add(ExifDisplayRow("地点", String.format(Locale.US, "%.6f, %.6f", exif.latitude, exif.longitude)))
    }
  }

  fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return String.format(Locale.US, "%.1f KB", kib)
    val mib = kib / 1024.0
    if (mib < 1024) return String.format(Locale.US, "%.1f MB", mib)
    return String.format(Locale.US, "%.2f GB", mib / 1024.0)
  }

  private fun formatExposure(seconds: Double): String =
    if (seconds >= 1.0) "${trim(seconds)}s" else "1/${(1.0 / seconds).roundToInt()}s"

  private fun trim(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)
}
