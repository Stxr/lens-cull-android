package com.stxr.lenscull.scan

import com.stxr.lenscull.domain.PhotoFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoFormatClassifierTest {
  @Test fun `classifies supported extensions case insensitively`() {
    assertEquals(PhotoFormat.JPEG, PhotoFormatClassifier.fromFileName("DSC_0001.JPEG"))
    assertEquals(PhotoFormat.RW2, PhotoFormatClassifier.fromFileName("P1000123.rW2"))
    assertEquals(PhotoFormat.HEIC, PhotoFormatClassifier.fromFileName("portrait.heif"))
  }

  @Test fun `uses MIME type when extension is missing`() {
    assertEquals(PhotoFormat.DNG, PhotoFormatClassifier.classify("image", "image/x-adobe-dng"))
  }

  @Test fun `rejects unsupported and misleading extensions`() {
    assertNull(PhotoFormatClassifier.fromFileName("clip.mp4"))
    assertNull(PhotoFormatClassifier.fromFileName("photo.jpg.tmp"))
  }
}
