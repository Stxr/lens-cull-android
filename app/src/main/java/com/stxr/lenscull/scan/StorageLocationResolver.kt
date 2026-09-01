package com.stxr.lenscull.scan

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import java.io.File

object StorageLocationResolver {
  fun resolveTree(context: Context, uri: Uri): String? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
    val separator = documentId.indexOf(':')
    val volumeId = if (separator >= 0) documentId.substring(0, separator) else documentId
    val relativePath = if (separator >= 0) documentId.substring(separator + 1) else ""
    val root = if (volumeId.equals("primary", ignoreCase = true)) {
      Environment.getExternalStorageDirectory()
    } else {
      context.getSystemService(StorageManager::class.java).storageVolumes
        .firstOrNull { it.uuid.equals(volumeId, ignoreCase = true) }
        ?.directory
    } ?: return null
    return File(root, relativePath).canonicalPath
  }
}
