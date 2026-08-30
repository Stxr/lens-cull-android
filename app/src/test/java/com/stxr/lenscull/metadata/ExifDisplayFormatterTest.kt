package com.stxr.lenscull.metadata

import com.stxr.lenscull.domain.ExifSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifDisplayFormatterTest {
  @Test fun `formats photographer focused fields`() {
    val rows = ExifDisplayFormatter.rows(
      ExifSummary(
        width = 6000,
        height = 4000,
        lensModel = "LEICA 12-60mm",
        focalLengthMm = 35.0,
        aperture = 2.8,
        exposureSeconds = 1.0 / 250.0,
        iso = 800,
      ),
      fileSizeBytes = 24L * 1024 * 1024,
    )
    assertTrue(rows.any { it.value == "6000 × 4000" })
    assertTrue(rows.any { it.value == "35mm" })
    assertTrue(rows.any { it.value == "1/250s" })
    assertEquals("24.0 MB", rows.first { it.label == "大小" }.value)
  }
}
