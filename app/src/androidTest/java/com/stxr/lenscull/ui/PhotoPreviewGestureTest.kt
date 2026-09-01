package com.stxr.lenscull.ui

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.ExifSummary
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState
import com.stxr.lenscull.ui.components.PhotoPreviewPanel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoPreviewGestureTest {
  @get:Rule val composeRule = createComposeRule()

  @Test fun previewSupportsLeftAndRightSwipeNavigation() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val image = File(context.cacheDir, "swipe-navigation.jpg")
    val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    try {
      image.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) }
    } finally {
      bitmap.recycle()
    }
    val photo = PhotoAsset(
      id = "test:${image.absolutePath}",
      volumeName = "test",
      canonicalPath = image.absolutePath,
      displayName = image.name,
      parentPath = image.parent.orEmpty(),
      format = PhotoFormat.JPEG,
      mimeType = "image/jpeg",
      fileSizeBytes = image.length(),
      modifiedAt = image.lastModified(),
      capturedAt = null,
      rating = 0,
      flag = CullFlag.UNFLAGGED,
      ratingSyncState = RatingSyncState.LOCAL_ONLY,
      previewState = PreviewState.READY,
      previewError = null,
      exif = ExifSummary(),
    )
    var previousCount = 0
    var nextCount = 0

    composeRule.setContent {
      MaterialTheme {
        PhotoPreviewPanel(
          photo = photo,
          previewFile = { Result.success(image) },
          onRating = {},
          onFlag = {},
          onRetrySync = {},
          onBack = null,
          onPrevious = { previousCount++ },
          onNext = { nextCount++ },
        )
      }
    }

    val preview = composeRule.onNodeWithContentDescription(image.name)
    preview.performTouchInput { swipeLeft() }
    composeRule.runOnIdle { assertEquals(1, nextCount) }
    preview.performTouchInput { swipeRight() }
    composeRule.runOnIdle { assertEquals(1, previousCount) }
    image.delete()
  }

  @Test fun fullscreenPreviewTogglesRatingMenuAndSupportsSwipe() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val image = File(context.cacheDir, "fullscreen-navigation.jpg")
    val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    try {
      image.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) }
    } finally {
      bitmap.recycle()
    }
    val photo = PhotoAsset(
      id = "test:${image.absolutePath}", volumeName = "test", canonicalPath = image.absolutePath,
      displayName = image.name, parentPath = image.parent.orEmpty(), format = PhotoFormat.JPEG,
      mimeType = "image/jpeg", fileSizeBytes = image.length(), modifiedAt = image.lastModified(),
      capturedAt = null, rating = 0, flag = CullFlag.UNFLAGGED,
      ratingSyncState = RatingSyncState.LOCAL_ONLY, previewState = PreviewState.READY,
      previewError = null, exif = ExifSummary(),
    )
    val nextPhoto = photo.copy(id = "test:next", displayName = "fullscreen-next.jpg")
    val displayedPhoto = mutableStateOf(photo)
    var previousCount = 0
    var nextCount = 0
    var selectedRating = -1
    composeRule.setContent {
      MaterialTheme {
        PhotoPreviewPanel(
          displayedPhoto.value, { Result.success(image) }, { selectedRating = it }, {}, {}, null,
          {
            previousCount++
            displayedPhoto.value = photo
          },
          {
            nextCount++
            displayedPhoto.value = nextPhoto
          },
          fullscreenPhotos = listOf(photo, nextPhoto),
          onFullscreenSelect = { selected ->
            if (selected.id == nextPhoto.id) nextCount++ else previousCount++
            displayedPhoto.value = selected
          },
        )
      }
    }

    composeRule.onNodeWithContentDescription("全屏预览").performClick()
    composeRule.onNodeWithContentDescription("退出全屏").assertIsDisplayed()
    val fullscreen = composeRule.onNodeWithContentDescription("全屏 ${image.name}")
    fullscreen.performTouchInput { click() }
    composeRule.onNodeWithText("再次点击照片可隐藏评分菜单").assertIsDisplayed()
    composeRule.onAllNodesWithContentDescription("3 星").onLast().performClick()
    composeRule.runOnIdle { assertEquals(3, selectedRating) }
    fullscreen.performTouchInput { click() }
    composeRule.onAllNodesWithText("再次点击照片可隐藏评分菜单").assertCountEquals(0)
    fullscreen.performTouchInput { swipeLeft() }
    composeRule.onNodeWithContentDescription("全屏 ${nextPhoto.displayName}").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("全屏 ${nextPhoto.displayName}").performTouchInput { swipeRight() }
    composeRule.onNodeWithContentDescription("全屏 ${photo.displayName}").assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, nextCount)
      assertEquals(1, previousCount)
    }
    image.delete()
  }
}
