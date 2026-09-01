package com.stxr.lenscull.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoSwipeGestureTest {
  @Test
  fun `left swipe opens next photo`() {
    assertEquals(
      PhotoSwipeDirection.NEXT,
      detectPhotoSwipe(-180f, 12f, viewportWidthPx = 1_000f, touchSlopPx = 16f, enabled = true),
    )
  }

  @Test
  fun `right swipe opens previous photo`() {
    assertEquals(
      PhotoSwipeDirection.PREVIOUS,
      detectPhotoSwipe(180f, -20f, viewportWidthPx = 1_000f, touchSlopPx = 16f, enabled = true),
    )
  }

  @Test
  fun `short and vertical gestures do not navigate`() {
    assertNull(detectPhotoSwipe(80f, 4f, viewportWidthPx = 1_000f, touchSlopPx = 16f, enabled = true))
    assertNull(detectPhotoSwipe(150f, 130f, viewportWidthPx = 1_000f, touchSlopPx = 16f, enabled = true))
  }

  @Test
  fun `navigation is disabled while image is zoomed`() {
    assertNull(detectPhotoSwipe(-180f, 0f, viewportWidthPx = 1_000f, touchSlopPx = 16f, enabled = false))
  }
}
