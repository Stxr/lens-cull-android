package com.stxr.lenscull.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.exifinterface.media.ExifInterface
import com.stxr.lenscull.MainActivity
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.ExifSummary
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState
import com.stxr.lenscull.metadata.MetadataWriteResult
import com.stxr.lenscull.metadata.MetadataWriter
import com.stxr.lenscull.metadata.XmpRating
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test fun packageNameIsStable() {
    assertEquals("com.stxr.lenscull", InstrumentationRegistry.getInstrumentation().targetContext.packageName)
  }

  @Test fun libraryShellIsVisible() {
    composeRule.onAllNodesWithText("LensCull", substring = true).onFirst().assertIsDisplayed()
  }

  @Test fun embeddedRatingsAreWrittenAndReadableOnAndroid() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val fixtures = listOf(
      Triple(PhotoFormat.JPEG, Bitmap.CompressFormat.JPEG, "jpg"),
      Triple(PhotoFormat.PNG, Bitmap.CompressFormat.PNG, "png"),
      Triple(PhotoFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSLESS, "webp"),
    )
    fixtures.forEach { (format, compression, extension) ->
      val image = File(context.cacheDir, "metadata-writer-test.$extension")
      val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
      try {
        image.outputStream().use { output -> check(bitmap.compress(compression, 95, output)) }
      } finally {
        bitmap.recycle()
      }
      val photo = PhotoAsset(
        id = "test:${image.absolutePath}",
        volumeName = "test",
        canonicalPath = image.absolutePath,
        displayName = image.name,
        parentPath = image.parent.orEmpty(),
        format = format,
        mimeType = format.mimeTypes.first(),
        fileSizeBytes = image.length(),
        modifiedAt = image.lastModified(),
        capturedAt = null,
        rating = 0,
        flag = CullFlag.UNFLAGGED,
        ratingSyncState = RatingSyncState.LOCAL_ONLY,
        previewState = PreviewState.UNKNOWN,
        previewError = null,
        exif = ExifSummary(),
      )

      try {
        assertEquals("$format write result", MetadataWriteResult.Synced, MetadataWriter().writeRating(photo, 4))
        val packet = ExifInterface(image).getAttributeBytes(ExifInterface.TAG_XMP)
        assertEquals("$format XMP rating", 4, XmpRating.read(packet))
      } finally {
        image.delete()
      }
    }
  }
}
