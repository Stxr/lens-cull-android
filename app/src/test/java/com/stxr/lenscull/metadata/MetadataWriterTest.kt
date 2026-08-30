package com.stxr.lenscull.metadata

import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.ExifSummary
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MetadataWriterTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun `writes and updates a RAW sidecar without touching RAW`() = runTest {
    val raw = temporaryFolder.newFile("P1000001.RW2").apply { writeText("raw-placeholder") }
    val writer = MetadataWriter(kotlinx.coroutines.Dispatchers.Unconfined)
    val result = writer.writeRating(asset(raw, PhotoFormat.RW2), 4)
    assertEquals(MetadataWriteResult.Synced, result)
    assertEquals("raw-placeholder", raw.readText())
    assertEquals(4, XmpRating.read(File(raw.parentFile, "P1000001.xmp").readBytes()))
  }

  @Test fun `detects same basename RAW conflicts`() = runTest {
    val rw2 = temporaryFolder.newFile("same.RW2")
    temporaryFolder.newFile("same.DNG")
    val result = MetadataWriter(kotlinx.coroutines.Dispatchers.Unconfined).writeRating(asset(rw2, PhotoFormat.RW2), 3)
    assertTrue(result is MetadataWriteResult.Conflict)
  }

  private fun asset(file: File, format: PhotoFormat) = PhotoAsset(
    id = "primary:${file.absolutePath}",
    volumeName = "primary",
    canonicalPath = file.absolutePath,
    displayName = file.name,
    parentPath = file.parent.orEmpty(),
    format = format,
    mimeType = format.mimeTypes.first(),
    fileSizeBytes = file.length(),
    modifiedAt = file.lastModified(),
    capturedAt = null,
    rating = 0,
    flag = CullFlag.UNFLAGGED,
    ratingSyncState = RatingSyncState.LOCAL_ONLY,
    previewState = PreviewState.UNKNOWN,
    previewError = null,
    exif = ExifSummary(),
  )
}
