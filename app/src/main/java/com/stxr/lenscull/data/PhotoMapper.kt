package com.stxr.lenscull.data

import com.stxr.lenscull.data.db.PhotoEntity
import com.stxr.lenscull.domain.ExifSummary
import com.stxr.lenscull.domain.PhotoAsset

fun PhotoEntity.toDomain(): PhotoAsset = PhotoAsset(
  id = id,
  volumeName = volumeName,
  canonicalPath = canonicalPath,
  displayName = displayName,
  parentPath = parentPath,
  format = format,
  mimeType = mimeType,
  fileSizeBytes = fileSizeBytes,
  modifiedAt = modifiedAt,
  capturedAt = capturedAt,
  rating = rating,
  flag = flag,
  ratingSyncState = ratingSyncState,
  previewState = previewState,
  previewError = previewError,
  exif = ExifSummary(
    width = width,
    height = height,
    cameraMake = cameraMake,
    cameraModel = cameraModel,
    lensModel = lensModel,
    focalLengthMm = focalLengthMm,
    focalLength35Mm = focalLength35Mm,
    aperture = aperture,
    exposureSeconds = exposureSeconds,
    iso = iso,
    capturedAt = capturedAt,
    latitude = latitude,
    longitude = longitude,
    orientationDegrees = orientationDegrees,
  ),
)
