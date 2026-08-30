package com.stxr.lenscull.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
  @RawQuery(observedEntities = [PhotoEntity::class])
  fun pagingSource(query: SupportSQLiteQuery): PagingSource<Int, PhotoEntity>

  @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
  fun observeById(id: String): Flow<PhotoEntity?>

  @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): PhotoEntity?

  @Query("SELECT * FROM photos WHERE canonicalPath = :path LIMIT 1")
  suspend fun getByPath(path: String): PhotoEntity?

  @Upsert
  suspend fun upsertAll(photos: List<PhotoEntity>)

  @Query("UPDATE photos SET rating = :rating, ratingSyncState = :syncState WHERE id = :id")
  suspend fun updateRating(id: String, rating: Int, syncState: RatingSyncState)

  @Query("UPDATE photos SET ratingSyncState = :syncState WHERE id = :id")
  suspend fun updateRatingSyncState(id: String, syncState: RatingSyncState)

  @Query("UPDATE photos SET flag = :flag WHERE id = :id")
  suspend fun updateFlag(id: String, flag: CullFlag)

  @Query("UPDATE photos SET previewState = :state, previewError = :error WHERE id = :id")
  suspend fun updatePreviewState(id: String, state: PreviewState, error: String?)

  @Query("DELETE FROM photos WHERE scanGeneration != :scanGeneration")
  suspend fun deleteNotSeen(scanGeneration: String): Int

  @Query("SELECT COUNT(*) FROM photos")
  fun observeCount(): Flow<Int>

  @Query("SELECT DISTINCT parentPath FROM photos ORDER BY parentPath")
  fun observeFolders(): Flow<List<String>>

  @Query("SELECT * FROM photos ORDER BY canonicalPath")
  suspend fun allForBackup(): List<PhotoEntity>
}
