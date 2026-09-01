package com.stxr.lenscull.ui

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.stxr.lenscull.scan.StorageLocationResolver
import com.stxr.lenscull.ui.library.ProjectHomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectFlowTest {
  @get:Rule val composeRule = createComposeRule()

  @Test fun emptyLibraryRequiresProjectCreationFirst() {
    var createdName: String? = null
    composeRule.setContent {
      MaterialTheme {
        ProjectHomeScreen(emptyList(), { createdName = it }, {}, {})
      }
    }

    composeRule.onNodeWithText("先创建一个选片项目").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("新建项目").performClick()
    composeRule.onNodeWithText("项目名称").performTextInput("婚礼精选")
    composeRule.onNodeWithText("创建", useUnmergedTree = true).performClick()
    composeRule.runOnIdle { assertEquals("婚礼精选", createdName) }
  }

  @Test fun selectedPrimaryStorageDirectoryResolvesToFilePath() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3APictures")
    val path = StorageLocationResolver.resolveTree(context, uri)
    check(path != null && path.endsWith("/Pictures")) { "Unexpected directory path: $path" }
  }
}
