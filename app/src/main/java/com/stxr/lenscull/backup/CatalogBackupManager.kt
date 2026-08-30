package com.stxr.lenscull.backup

import android.content.ContentResolver
import android.net.Uri
import com.stxr.lenscull.data.PhotoCatalogRepository
import com.stxr.lenscull.data.db.PhotoDao
import com.stxr.lenscull.domain.CullFlag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupEntry(val id: String, val path: String, val rating: Int, val flag: String)

@Serializable
data class CatalogBackup(val schemaVersion: Int = 1, val createdAt: Long, val entries: List<BackupEntry>)

data class RestoreResult(val restored: Int, val missing: Int)

class CatalogBackupManager(
  private val contentResolver: ContentResolver,
  private val photoDao: PhotoDao,
  private val repository: PhotoCatalogRepository,
) {
  private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

  suspend fun exportTo(uri: Uri) {
    val entries = photoDao.allForBackup().map {
      BackupEntry(id = it.id, path = it.canonicalPath, rating = it.rating, flag = it.flag.name)
    }
    val payload = json.encodeToString(CatalogBackup(createdAt = System.currentTimeMillis(), entries = entries))
    checkNotNull(contentResolver.openOutputStream(uri, "wt")) { "无法打开备份目标" }
      .bufferedWriter().use { it.write(payload) }
  }

  suspend fun restoreFrom(uri: Uri): RestoreResult {
    val payload = checkNotNull(contentResolver.openInputStream(uri)) { "无法打开备份文件" }
      .bufferedReader().use { json.decodeFromString<CatalogBackup>(it.readText()) }
    require(payload.schemaVersion == 1) { "不支持的备份版本 ${payload.schemaVersion}" }
    var restored = 0
    var missing = 0
    payload.entries.forEach { entry ->
      val target = photoDao.getById(entry.id) ?: photoDao.getByPath(entry.path)
      if (target == null) {
        missing += 1
      } else {
        repository.setFlag(target.id, runCatching { CullFlag.valueOf(entry.flag) }.getOrDefault(CullFlag.UNFLAGGED))
        repository.setRatingLocal(target.id, entry.rating.coerceIn(0, 5))
        restored += 1
      }
    }
    return RestoreResult(restored, missing)
  }
}
