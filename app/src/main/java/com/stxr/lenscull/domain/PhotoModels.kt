package com.stxr.lenscull.domain

enum class PhotoFormat(
  val displayName: String,
  val extensions: Set<String>,
  val mimeTypes: Set<String>,
) {
  JPEG("JPEG", setOf("jpg", "jpeg"), setOf("image/jpeg")),
  PNG("PNG", setOf("png"), setOf("image/png")),
  WEBP("WebP", setOf("webp"), setOf("image/webp")),
  HEIC("HEIC", setOf("heic", "heif"), setOf("image/heic", "image/heif")),
  DNG("DNG", setOf("dng"), setOf("image/x-adobe-dng", "image/dng")),
  RW2("RW2", setOf("rw2"), setOf("image/x-panasonic-rw2", "image/rw2"));

  val isRaw: Boolean get() = this == DNG || this == RW2
  val supportsEmbeddedRating: Boolean get() = this == JPEG || this == PNG || this == WEBP
}

enum class CullFlag { UNFLAGGED, PICKED, REJECTED }

enum class RatingSyncState { SYNCED, LOCAL_ONLY, PENDING, FAILED, CONFLICT }

enum class PreviewState { UNKNOWN, READY, MISSING, FAILED }

enum class RatingMode { ALL, UNRATED, AT_LEAST }

enum class SortField { CAPTURED_AT, FILE_NAME, RATING }

enum class SortDirection { ASCENDING, DESCENDING }

data class LibraryFilter(
  val formats: Set<PhotoFormat> = PhotoFormat.entries.toSet(),
  val ratingMode: RatingMode = RatingMode.ALL,
  val minimumRating: Int = 0,
  val flag: CullFlag? = null,
  val folderPrefix: String? = null,
  val capturedAfter: Long? = null,
  val capturedBefore: Long? = null,
  val sortField: SortField = SortField.CAPTURED_AT,
  val sortDirection: SortDirection = SortDirection.DESCENDING,
)

data class ExifSummary(
  val width: Int? = null,
  val height: Int? = null,
  val cameraMake: String? = null,
  val cameraModel: String? = null,
  val lensModel: String? = null,
  val focalLengthMm: Double? = null,
  val focalLength35Mm: Int? = null,
  val aperture: Double? = null,
  val exposureSeconds: Double? = null,
  val iso: Int? = null,
  val capturedAt: Long? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val orientationDegrees: Int = 0,
)

data class PhotoAsset(
  val id: String,
  val volumeName: String,
  val canonicalPath: String,
  val displayName: String,
  val parentPath: String,
  val format: PhotoFormat,
  val mimeType: String,
  val fileSizeBytes: Long,
  val modifiedAt: Long,
  val capturedAt: Long?,
  val rating: Int,
  val flag: CullFlag,
  val ratingSyncState: RatingSyncState,
  val previewState: PreviewState,
  val previewError: String?,
  val exif: ExifSummary,
)

sealed interface ScanState {
  data object Idle : ScanState
  data object PermissionRequired : ScanState
  data class Running(val scanned: Int, val indexed: Int, val failures: Int) : ScanState
  data class Complete(val indexed: Int, val failures: Int, val finishedAt: Long) : ScanState
  data class Failed(val message: String) : ScanState
}
