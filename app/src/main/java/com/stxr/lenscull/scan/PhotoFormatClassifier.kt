package com.stxr.lenscull.scan

import com.stxr.lenscull.domain.PhotoFormat

object PhotoFormatClassifier {
  fun fromFileName(fileName: String): PhotoFormat? {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (extension.isBlank()) return null
    return PhotoFormat.entries.firstOrNull { extension in it.extensions }
  }

  fun fromMimeType(mimeType: String?): PhotoFormat? {
    val normalized = mimeType?.substringBefore(';')?.trim()?.lowercase() ?: return null
    return PhotoFormat.entries.firstOrNull { normalized in it.mimeTypes }
  }

  fun classify(fileName: String, mimeType: String? = null): PhotoFormat? =
    fromFileName(fileName) ?: fromMimeType(mimeType)
}
