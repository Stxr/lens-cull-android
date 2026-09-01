package com.stxr.lenscull.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.stxr.lenscull.data.db.PhotoDao
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.LibraryFilter
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState
import com.stxr.lenscull.metadata.MetadataWriteResult
import com.stxr.lenscull.metadata.MetadataWriter
import com.stxr.lenscull.preview.RawPreviewExtractor
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PhotoCatalogRepository(
  private val photoDao: PhotoDao,
  private val metadataWriter: MetadataWriter,
  private val previewExtractor: RawPreviewExtractor,
) {
  fun photos(projectId: String, filter: LibraryFilter): Flow<PagingData<PhotoAsset>> =
    Pager(PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false)) {
      photoDao.pagingSource(LibraryQueryBuilder.build(projectId, filter))
    }.flow.map { paging -> paging.map { it.toDomain() } }

  fun observePhoto(id: String): Flow<PhotoAsset?> = photoDao.observeById(id).map { it?.toDomain() }
  fun observeCount(): Flow<Int> = photoDao.observeCount()
  fun observeCount(projectId: String): Flow<Int> = photoDao.observeCount(projectId)
  fun observeFolders(): Flow<List<String>> = photoDao.observeFolders()
  fun observeFolders(projectId: String): Flow<List<String>> = photoDao.observeFolders(projectId)

  suspend fun setRating(id: String, rating: Int) {
    require(rating in 0..5)
    val entity = photoDao.getById(id) ?: return
    photoDao.updateRating(id, rating, RatingSyncState.PENDING)
    val result = metadataWriter.writeRating(entity.toDomain().copy(rating = rating), rating)
    val state = when (result) {
      MetadataWriteResult.Synced -> RatingSyncState.SYNCED
      is MetadataWriteResult.LocalOnly -> RatingSyncState.LOCAL_ONLY
      is MetadataWriteResult.Conflict -> RatingSyncState.CONFLICT
      is MetadataWriteResult.Failed -> RatingSyncState.FAILED
    }
    photoDao.updateRatingSyncState(id, state)
  }

  suspend fun setFlag(id: String, flag: CullFlag) = photoDao.updateFlag(id, flag)

  suspend fun setRatingLocal(id: String, rating: Int) {
    require(rating in 0..5)
    photoDao.updateRating(id, rating, RatingSyncState.LOCAL_ONLY)
  }

  suspend fun previewFile(photo: PhotoAsset): Result<File> {
    val result = runCatching { previewExtractor.resolve(photo) }
    result.fold(
      onSuccess = { photoDao.updatePreviewState(photo.id, PreviewState.READY, null) },
      onFailure = { photoDao.updatePreviewState(photo.id, PreviewState.FAILED, it.message) },
    )
    return result
  }
}
