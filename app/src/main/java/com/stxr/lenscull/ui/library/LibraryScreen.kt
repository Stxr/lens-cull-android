package com.stxr.lenscull.ui.library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.paging.compose.collectAsLazyPagingItems
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.ProjectSourceType
import com.stxr.lenscull.domain.ScanState
import com.stxr.lenscull.ui.components.FilterBar
import com.stxr.lenscull.ui.components.PhotoGrid
import com.stxr.lenscull.ui.components.PhotoPreviewPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val projects by viewModel.projects.collectAsState()
  val activeProject by viewModel.activeProject.collectAsState()

  if (activeProject == null) {
    ProjectHomeScreen(
      projects = projects,
      onCreate = viewModel::createProject,
      onOpen = viewModel::openProject,
      onDelete = viewModel::deleteProject,
    )
    return
  }
  if (activeProject!!.sourceType == ProjectSourceType.UNCONFIGURED) {
    ProjectSetupScreen(
      project = activeProject!!,
      onBack = viewModel::closeProject,
      onAllStorage = viewModel::configureAllStorage,
      onDirectory = viewModel::configureDirectory,
    )
    return
  }

  val photos = viewModel.photos.collectAsLazyPagingItems()
  val selected by viewModel.selectedPhoto.collectAsState()
  val filter by viewModel.filter.collectAsState()
  val folders by viewModel.folders.collectAsState()
  val count by viewModel.photoCount.collectAsState()
  val scanState by viewModel.scanState.collectAsState()
  val message by viewModel.message.collectAsState()
  val ratingConsentRequest by viewModel.ratingConsentRequest.collectAsState()
  val snackbar = remember { SnackbarHostState() }
  val focusRequester = remember { FocusRequester() }
  var menuExpanded by remember { mutableStateOf(false) }

  val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    viewModel.refreshPermission()
    if (Environment.isExternalStorageManager()) viewModel.startScan()
  }
  val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    viewModel.startScan()
  }
  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    uri?.let(viewModel::exportBackup)
  }
  val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(viewModel::restoreBackup)
  }

  fun startScan() {
    if (!Environment.isExternalStorageManager()) {
      runCatching {
        settingsLauncher.launch(
          Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${context.packageName}".toUri()),
        )
      }.onFailure {
        settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
      }
    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      viewModel.startScan()
    }
  }

  fun selectOffset(offset: Int) {
    val items = photos.itemSnapshotList.items
    if (items.isEmpty()) return
    val current = items.indexOfFirst { it.id == selected?.id }.takeIf { it >= 0 } ?: 0
    viewModel.select(items.getOrNull((current + offset).coerceIn(0, items.lastIndex)))
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  LaunchedEffect(activeProject!!.id, activeProject!!.sourceType, activeProject!!.sourcePath, count) {
    if (count == 0 && Environment.isExternalStorageManager()) viewModel.startScan()
  }
  LaunchedEffect(message) {
    message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .focusRequester(focusRequester)
      .focusable()
      .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
          Key.Zero -> { viewModel.setRating(0); true }
          Key.One -> { viewModel.setRating(1); true }
          Key.Two -> { viewModel.setRating(2); true }
          Key.Three -> { viewModel.setRating(3); true }
          Key.Four -> { viewModel.setRating(4); true }
          Key.Five -> { viewModel.setRating(5); true }
          Key.P -> { viewModel.setFlag(CullFlag.PICKED); true }
          Key.X -> { viewModel.setFlag(CullFlag.REJECTED); true }
          Key.DirectionLeft -> { selectOffset(-1); true }
          Key.DirectionRight -> { selectOffset(1); true }
          else -> false
        }
      },
    snackbarHost = { SnackbarHost(snackbar) },
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::closeProject) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回项目") }
            Text(activeProject!!.name, style = MaterialTheme.typography.titleLarge)
            Text("  $count 张", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        actions = {
          if (scanState is ScanState.Running) {
            IconButton(onClick = viewModel::cancelScan) { Icon(Icons.Rounded.Cancel, "取消扫描") }
          } else {
            IconButton(onClick = ::startScan) { Icon(Icons.Rounded.Refresh, "扫描照片") }
          }
          Box {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Rounded.MoreVert, "更多") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
              DropdownMenuItem(
                text = { Text("导出选片备份") },
                leadingIcon = { Icon(Icons.Rounded.Backup, null) },
                onClick = { menuExpanded = false; exportLauncher.launch("lenscull-backup.json") },
              )
              DropdownMenuItem(
                text = { Text("恢复选片备份") },
                leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) },
                onClick = { menuExpanded = false; restoreLauncher.launch(arrayOf("application/json", "text/json")) },
              )
            }
          }
        },
      )
    },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      ScanStatus(scanState, ::startScan)
      if (scanState !is ScanState.PermissionRequired) {
        FilterBar(
          filter = filter,
          folders = folders,
          onToggleFormat = viewModel::toggleFormat,
          onRating = viewModel::setRatingFilter,
          onFlag = viewModel::setFlagFilter,
          onFolder = viewModel::setFolder,
          onDateRange = viewModel::setDateRange,
          onSort = viewModel::setSort,
          onClear = viewModel::clearFilters,
        )
        HorizontalDivider()
      }
      if (scanState is ScanState.PermissionRequired) {
        PermissionGate(::startScan, Modifier.weight(1f))
      } else {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
          val expanded = maxWidth >= 840.dp
          if (expanded) {
            Row(Modifier.fillMaxSize()) {
              PhotoGrid(
                photos = photos,
                selectedId = selected?.id,
                onSelect = viewModel::select,
                previewFile = viewModel::previewFile,
                modifier = Modifier.weight(0.44f),
              )
              androidx.compose.material3.VerticalDivider()
              PhotoPreviewPanel(
                photo = selected,
                previewFile = viewModel::previewFile,
                onRating = viewModel::setRating,
                onFlag = viewModel::setFlag,
                onRetrySync = viewModel::retryRatingSync,
                onBack = null,
                onPrevious = { selectOffset(-1) },
                onNext = { selectOffset(1) },
                modifier = Modifier.weight(0.56f),
              )
            }
          } else if (selected == null) {
            PhotoGrid(photos, null, viewModel::select, viewModel::previewFile)
          } else {
            PhotoPreviewPanel(
              photo = selected,
              previewFile = viewModel::previewFile,
              onRating = viewModel::setRating,
              onFlag = viewModel::setFlag,
              onRetrySync = viewModel::retryRatingSync,
              onBack = { viewModel.select(null) },
              onPrevious = { selectOffset(-1) },
              onNext = { selectOffset(1) },
            )
          }
        }
      }
    }
  }

  if (ratingConsentRequest != null) {
    AlertDialog(
      onDismissRequest = viewModel::dismissRatingConsent,
      title = { Text("允许写入照片评分？") },
      text = {
        Text(
          "LensCull 会把 JPEG、PNG、WebP 的星级写入图片内的 XMP，把 RW2/DNG 星级写入同名 .xmp sidecar。写入前会创建临时副本并校验；不会删除、移动或重命名照片。",
        )
      },
      confirmButton = { Button(onClick = viewModel::confirmMetadataWrite) { Text("允许安全写入") } },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = viewModel::savePendingRatingLocally) { Text("仅保存在本应用") }
      },
    )
  }
}

@Composable
private fun ScanStatus(state: ScanState, onPermission: () -> Unit) {
  when (state) {
    is ScanState.Running -> Column(Modifier.fillMaxWidth()) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
      Text(
        "正在扫描：${state.indexed} 张照片，${state.failures} 个异常",
        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
      )
    }
    is ScanState.Complete -> Text(
      "扫描完成：${state.indexed} 张照片，${state.failures} 个异常",
      Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      style = MaterialTheme.typography.labelMedium,
    )
    is ScanState.Failed -> Text(
      "扫描失败：${state.message}",
      Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      color = MaterialTheme.colorScheme.error,
    )
    ScanState.PermissionRequired, ScanState.Idle -> Unit
  }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(Modifier.widthIn(max = 520.dp).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(Icons.Rounded.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary)
      Text("访问照片库", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
      Text(
        "LensCull 需要“管理所有文件”权限，才能扫描内部共享存储、SD 卡和 OTG 中的 JPEG、RW2 等照片。不会访问网络，也不会删除或移动原片。",
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(onClick = onGrant) { Text("前往系统设置授权") }
    }
  }
}
