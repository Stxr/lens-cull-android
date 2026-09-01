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
import com.stxr.lenscull.domain.CullProject
import com.stxr.lenscull.domain.LibraryFilter
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.ProjectSourceType
import com.stxr.lenscull.domain.RatingMode
import com.stxr.lenscull.domain.ScanState
import com.stxr.lenscull.domain.SortDirection
import com.stxr.lenscull.domain.SortField
import com.stxr.lenscull.scan.ScanWorker
import com.stxr.lenscull.scan.StorageLocationResolver
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
  private val projectRepository = app.container.projectRepository
  private val backupManager = CatalogBackupManager(application.contentResolver, app.container.database.photoDao(), repository)
  private val workManager = WorkManager.getInstance(application)
  private val settingsRepository = app.container.settingsRepository

  val filter = MutableStateFlow(LibraryFilter())
  private val activeProjectId = MutableStateFlow<String?>(null)
  private val selectedId = MutableStateFlow<String?>(null)
  private val permissionRefresh = MutableStateFlow(0)
  private val operationMessage = MutableStateFlow<String?>(null)
  private val pendingRating = MutableStateFlow<Int?>(null)

  val projects: StateFlow<List<CullProject>> = projectRepository.observeProjects()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
  val activeProject: StateFlow<CullProject?> = activeProjectId.flatMapLatest { id ->
    if (id == null) flowOf(null) else projectRepository.observeProject(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  val photos: Flow<PagingData<PhotoAsset>> = combine(activeProjectId, filter) { projectId, currentFilter ->
    projectId to currentFilter
  }.flatMapLatest { (projectId, currentFilter) ->
    if (projectId == null) flowOf(PagingData.empty()) else repository.photos(projectId, currentFilter)
  }.cachedIn(viewModelScope)
  val selectedPhoto: StateFlow<PhotoAsset?> = selectedId.flatMapLatest { id ->
    if (id == null) flowOf(null) else repository.observePhoto(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
  val photoCount: StateFlow<Int> = activeProjectId.flatMapLatest { id ->
    if (id == null) flowOf(0) else repository.observeCount(id)
  }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
  val folders: StateFlow<List<String>> = activeProjectId.flatMapLatest { id ->
    if (id == null) flowOf(emptyList()) else repository.observeFolders(id)
  }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val scanState: StateFlow<ScanState> = combine(
    activeProjectId.flatMapLatest { id ->
      if (id == null) flowOf(emptyList()) else workManager.getWorkInfosForUniqueWorkFlow(ScanWorker.uniqueWork(id))
    },
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
  fun startScan() {
    val project = activeProject.value ?: return
    if (project.sourceType == ProjectSourceType.UNCONFIGURED) return
    ScanWorker.enqueue(getApplication(), project.id, project.sourcePath)
  }
  fun cancelScan() { activeProjectId.value?.let { ScanWorker.cancel(getApplication(), it) } }
  fun select(photo: PhotoAsset?) { selectedId.value = photo?.id }

  fun openProject(project: CullProject) {
    selectedId.value = null
    filter.value = LibraryFilter()
    activeProjectId.value = project.id
  }

  fun closeProject() {
    selectedId.value = null
    activeProjectId.value = null
  }

  fun createProject(name: String) = viewModelScope.launch {
    val project = projectRepository.create(name)
    openProject(project)
  }

  fun deleteProject(project: CullProject) = viewModelScope.launch {
    if (activeProjectId.value == project.id) closeProject()
    projectRepository.delete(project.id)
  }

  fun configureAllStorage() = viewModelScope.launch {
    val id = activeProjectId.value ?: return@launch
    projectRepository.configure(id, ProjectSourceType.ALL_STORAGE, null)
  }

  fun configureDirectory(uri: Uri) {
    val context = getApplication<Application>()
    val path = runCatching { StorageLocationResolver.resolveTree(context, uri) }.getOrNull()
    if (path == null) {
      operationMessage.value = "无法解析所选目录，请选择内部存储或已挂载存储卡中的目录"
      return
    }
    viewModelScope.launch {
      val id = activeProjectId.value ?: return@launch
      projectRepository.configure(id, ProjectSourceType.DIRECTORY, path)
    }
  }

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
