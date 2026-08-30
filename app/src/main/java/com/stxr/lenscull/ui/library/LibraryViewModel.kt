package com.stxr.lenscull.ui.library

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stxr.lenscull.LensCullApplication
import com.stxr.lenscull.backup.CatalogBackupManager
import com.stxr.lenscull.backup.RestoreResult
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.LibraryFilter
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.RatingMode
import com.stxr.lenscull.domain.ScanState
import com.stxr.lenscull.domain.SortDirection
import com.stxr.lenscull.domain.SortField
import com.stxr.lenscull.scan.ScanWorker
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as LensCullApplication
  private val repository = app.container.photoRepository
  private val backupManager = CatalogBackupManager(application.contentResolver, app.container.database.photoDao(), repository)
  private val workManager = WorkManager.getInstance(application)
  private val settingsRepository = app.container.settingsRepository

  val filter = MutableStateFlow(LibraryFilter())
  private val selectedId = MutableStateFlow<String?>(null)
  private val permissionRefresh = MutableStateFlow(0)
  private val operationMessage = MutableStateFlow<String?>(null)
  private val pendingRating = MutableStateFlow<Int?>(null)

  val photos: Flow<PagingData<PhotoAsset>> = filter.flatMapLatest(repository::photos).cachedIn(viewModelScope)
  val selectedPhoto: StateFlow<PhotoAsset?> = selectedId.flatMapLatest { id ->
    if (id == null) flowOf(null) else repository.observePhoto(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
  val photoCount: StateFlow<Int> = repository.observeCount()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
  val folders: StateFlow<List<String>> = repository.observeFolders()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val scanState: StateFlow<ScanState> = combine(
    workManager.getWorkInfosForUniqueWorkFlow(ScanWorker.UNIQUE_WORK),
    permissionRefresh,
  ) { works, _ ->
    if (!Environment.isExternalStorageManager()) return@combine ScanState.PermissionRequired
    val work = works.maxByOrNull { it.runAttemptCount }
    when (work?.state) {
      WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ScanState.Running(0, 0, 0)
      WorkInfo.State.RUNNING -> ScanState.Running(
        scanned = work.progress.getInt(ScanWorker.KEY_SCANNED, 0),
        indexed = work.progress.getInt(ScanWorker.KEY_INDEXED, 0),
        failures = work.progress.getInt(ScanWorker.KEY_FAILURES, 0),
      )
      WorkInfo.State.SUCCEEDED -> ScanState.Complete(
        indexed = work.outputData.getInt(ScanWorker.KEY_INDEXED, 0),
        failures = work.outputData.getInt(ScanWorker.KEY_FAILURES, 0),
        finishedAt = System.currentTimeMillis(),
      )
      WorkInfo.State.FAILED -> ScanState.Failed(work.outputData.getString(ScanWorker.KEY_ERROR) ?: "扫描失败")
      else -> ScanState.Idle
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialScanState())

  val message: StateFlow<String?> = operationMessage
  val ratingConsentRequest: StateFlow<Int?> = pendingRating
  val metadataWriteConsent: StateFlow<Boolean> = settingsRepository.metadataWriteConsent
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  fun refreshPermission() { permissionRefresh.value += 1 }
  fun startScan() = ScanWorker.enqueue(getApplication())
  fun cancelScan() = ScanWorker.cancel(getApplication())
  fun select(photo: PhotoAsset?) { selectedId.value = photo?.id }

  fun setRating(rating: Int) {
    val id = selectedId.value ?: return
    if (!metadataWriteConsent.value) {
      pendingRating.value = rating
      return
    }
    viewModelScope.launch { repository.setRating(id, rating) }
  }

  fun confirmMetadataWrite() {
    val rating = pendingRating.value ?: return
    val id = selectedId.value ?: return
    pendingRating.value = null
    viewModelScope.launch {
      settingsRepository.setMetadataWriteConsent(true)
      repository.setRating(id, rating)
    }
  }

  fun savePendingRatingLocally() {
    val rating = pendingRating.value ?: return
    val id = selectedId.value ?: return
    pendingRating.value = null
    viewModelScope.launch { repository.setRatingLocal(id, rating) }
  }

  fun dismissRatingConsent() { pendingRating.value = null }

  fun setFlag(flag: CullFlag) {
    val id = selectedId.value ?: return
    viewModelScope.launch { repository.setFlag(id, flag) }
  }

  fun retryRatingSync() {
    val photo = selectedPhoto.value ?: return
    if (!metadataWriteConsent.value) {
      pendingRating.value = photo.rating
      return
    }
    viewModelScope.launch { repository.setRating(photo.id, photo.rating) }
  }

  suspend fun previewFile(photo: PhotoAsset): Result<File> = repository.previewFile(photo)

  fun toggleFormat(format: PhotoFormat) {
    filter.value = filter.value.let { current ->
      val updated = current.formats.toMutableSet().apply {
        if (!add(format)) remove(format)
      }
      current.copy(formats = updated)
    }
  }

  fun setRatingFilter(mode: RatingMode, minimum: Int = 0) {
    filter.value = filter.value.copy(ratingMode = mode, minimumRating = minimum)
  }

  fun setFlagFilter(flag: CullFlag?) { filter.value = filter.value.copy(flag = flag) }
  fun setFolder(folder: String?) { filter.value = filter.value.copy(folderPrefix = folder) }
  fun setDateRange(start: Long?, end: Long?) {
    filter.value = filter.value.copy(capturedAfter = start, capturedBefore = end)
  }
  fun setSort(field: SortField, direction: SortDirection) {
    filter.value = filter.value.copy(sortField = field, sortDirection = direction)
  }
  fun clearFilters() { filter.value = LibraryFilter() }

  fun exportBackup(uri: Uri) = viewModelScope.launch {
    operationMessage.value = runCatching { backupManager.exportTo(uri) }
      .fold({ "备份已导出" }, { "备份失败：${it.message}" })
  }

  fun restoreBackup(uri: Uri) = viewModelScope.launch {
    operationMessage.value = runCatching { backupManager.restoreFrom(uri) }
      .fold(::restoreMessage, { "恢复失败：${it.message}" })
  }

  fun clearMessage() { operationMessage.value = null }

  private fun initialScanState(): ScanState =
    if (Environment.isExternalStorageManager()) ScanState.Idle else ScanState.PermissionRequired

  private fun restoreMessage(result: RestoreResult): String =
    "已恢复 ${result.restored} 项${if (result.missing > 0) "，${result.missing} 项未找到" else ""}"
}
