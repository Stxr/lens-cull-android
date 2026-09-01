package com.stxr.lenscull.scan

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageLocationResolverTest {
  @Test fun selectedPrimaryStorageDirectoryResolvesToFilePath() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3APictures")
    val path = StorageLocationResolver.resolveTree(context, uri)
    check(path != null && path.endsWith("/Pictures")) { "Unexpected directory path: $path" }
  }
}
