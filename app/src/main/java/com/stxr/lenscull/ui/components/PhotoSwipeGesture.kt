package com.stxr.lenscull.ui.components

import kotlin.math.abs
import kotlin.math.max

internal enum class PhotoSwipeDirection {
  PREVIOUS,
  NEXT,
}

internal const val BASE_SCALE_TOLERANCE = 1.01f

internal fun detectPhotoSwipe(
  deltaX: Float,
  deltaY: Float,
  viewportWidthPx: Float,
  touchSlopPx: Float,
  enabled: Boolean,
): PhotoSwipeDirection? {
  if (!enabled || viewportWidthPx <= 0f) return null

  val horizontalDistance = abs(deltaX)
  val minimumDistance = max(touchSlopPx * 4f, viewportWidthPx * 0.12f)
  val isHorizontal = horizontalDistance > abs(deltaY) * 1.5f
  if (horizontalDistance < minimumDistance || !isHorizontal) return null

  return if (deltaX < 0f) PhotoSwipeDirection.NEXT else PhotoSwipeDirection.PREVIOUS
}
