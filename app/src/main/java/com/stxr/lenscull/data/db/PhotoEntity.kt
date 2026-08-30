package com.stxr.lenscull.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState

@Entity(
  tableName = "photos",
  indices = [
    Index(value = ["canonicalPath"], unique = true),
    Index(value = ["capturedAt"]),
    Index(value = ["format"]),
    Index(value = ["rating"]),
    Index(value = ["flag"]),
    Index(value = ["scanGeneration"]),
  ],
)
data class PhotoEntity(
  @PrimaryKey val id: String,
  val volumeName: String,
  val canonicalPath: String,
  val displayName: String,
  val parentPath: String,
  val format: PhotoFormat,
  val mimeType: String,
  val fileSizeBytes: Long,
  val modifiedAt: Long,
  val capturedAt: Long?,
  val rating: Int = 0,
  val flag: CullFlag = CullFlag.UNFLAGGED,
  val ratingSyncState: RatingSyncState = RatingSyncState.LOCAL_ONLY,
  val previewState: PreviewState = PreviewState.UNKNOWN,
  val previewError: String? = null,
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
  val latitude: Double? = null,
  val longitude: Double? = null,
  val orientationDegrees: Int = 0,
  val scanGeneration: String,
)
