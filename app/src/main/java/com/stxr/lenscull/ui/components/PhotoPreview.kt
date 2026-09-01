package com.stxr.lenscull.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.RatingSyncState
import com.stxr.lenscull.metadata.ExifDisplayFormatter
import java.io.File
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhotoPreviewPanel(
  photo: PhotoAsset?,
  previewFile: suspend (PhotoAsset) -> Result<File>,
  onRating: (Int) -> Unit,
  onFlag: (CullFlag) -> Unit,
  onRetrySync: () -> Unit,
  onBack: (() -> Unit)?,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (photo == null) {
    Box(modifier.fillMaxSize().background(Color(0xFF111317)), contentAlignment = Alignment.Center) {
      Text("选择一张照片开始选片", color = Color(0xFFB7BAC1))
    }
    return
  }
  val preview by produceState<Result<File>?>(null, photo.id, photo.modifiedAt) {
    value = previewFile(photo)
  }
  var showExif by remember(photo.id) { mutableStateOf(false) }
  var showInfoSheet by remember(photo.id) { mutableStateOf(false) }
  val holdModifier = Modifier.pointerInput(photo.id) {
    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
      val endedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          val pointer = event.changes.firstOrNull { it.id == down.id }
          if (pointer?.pressed != true ||
            (pointer.position - down.position).getDistance() > viewConfiguration.touchSlop
          ) {
            return@withTimeoutOrNull true
          }
        }
      }
      if (endedBeforeLongPress == null) {
        showExif = true
        do {
          val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
        showExif = false
      }
    }
  }

  Column(modifier.fillMaxSize().background(Color(0xFF0D0F12))) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回网格", tint = Color.White) } }
      Text(
        photo.displayName,
        modifier = Modifier.weight(1f),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.Medium,
      )
      if (photo.ratingSyncState != RatingSyncState.SYNCED) {
        Icon(Icons.Rounded.SyncProblem, "评分同步状态：${photo.ratingSyncState}", tint = Color(0xFFFFC857))
        if (photo.ratingSyncState in setOf(RatingSyncState.FAILED, RatingSyncState.CONFLICT, RatingSyncState.PENDING)) {
          TextButton(onClick = onRetrySync) { Text("重试同步", color = Color(0xFFFFC857)) }
        }
      }
      IconButton(onClick = { showInfoSheet = true }) { Icon(Icons.Rounded.Info, "完整 EXIF", tint = Color.White) }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
      when {
        preview == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        preview!!.isSuccess -> key(photo.id) {
          val zoomState = rememberCoilZoomState()
          val swipeModifier = Modifier.pointerInput(photo.id, zoomState) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
              val startedAtBaseScale = zoomState.zoomable.userTransform.scaleX <= BASE_SCALE_TOLERANCE
              var lastPosition = down.position
              var multiplePointers = false
              do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                multiplePointers = multiplePointers || event.changes.count { it.pressed } > 1
                val pointer = event.changes.firstOrNull { it.id == down.id }
                if (pointer != null) lastPosition = pointer.position
              } while (pointer?.pressed == true)

              when (
                detectPhotoSwipe(
                  deltaX = lastPosition.x - down.position.x,
                  deltaY = lastPosition.y - down.position.y,
                  viewportWidthPx = size.width.toFloat(),
                  touchSlopPx = viewConfiguration.touchSlop,
                  enabled = startedAtBaseScale && !multiplePointers,
                )
              ) {
                PhotoSwipeDirection.PREVIOUS -> onPrevious()
                PhotoSwipeDirection.NEXT -> onNext()
                null -> Unit
              }
            }
          }
          CoilZoomAsyncImage(
            model = preview!!.getOrNull(),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize().then(holdModifier).then(swipeModifier),
            zoomState = zoomState,
          )
        }
        else -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Rounded.Close, null, Modifier.size(44.dp), tint = Color(0xFFFF8A80))
          Text(preview!!.exceptionOrNull()?.message ?: "无法预览", color = Color.White, modifier = Modifier.padding(12.dp))
        }
      }
      IconButton(onClick = onPrevious, modifier = Modifier.align(Alignment.CenterStart)) {
        Icon(Icons.Rounded.ChevronLeft, "上一张", Modifier.size(42.dp), tint = Color.White)
      }
      IconButton(onClick = onNext, modifier = Modifier.align(Alignment.CenterEnd)) {
        Icon(Icons.Rounded.ChevronRight, "下一张", Modifier.size(42.dp), tint = Color.White)
      }
      if (showExif) ExifOverlay(photo, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
    Surface(color = Color(0xFF181B20), contentColor = Color.White) {
      Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        RatingBar(photo.rating, onRating)
        Spacer(Modifier.width(18.dp))
        FilledTonalButton(onClick = { onFlag(CullFlag.PICKED) }) {
          Icon(Icons.Rounded.TaskAlt, null)
          Spacer(Modifier.width(5.dp))
          Text(if (photo.flag == CullFlag.PICKED) "已保留" else "保留 (P)")
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onFlag(CullFlag.REJECTED) }) {
          Icon(Icons.Rounded.Close, null)
          Spacer(Modifier.width(5.dp))
          Text(if (photo.flag == CullFlag.REJECTED) "已淘汰" else "淘汰 (X)")
        }
      }
    }
  }

  if (showInfoSheet) {
    ExifInfoSheet(photo = photo, onDismiss = { showInfoSheet = false })
  }
}

@Composable
private fun ExifOverlay(photo: PhotoAsset, modifier: Modifier = Modifier) {
  Surface(modifier, color = Color(0xE6111317), contentColor = Color.White, shape = MaterialTheme.shapes.medium, shadowElevation = 8.dp) {
    Column(Modifier.padding(14.dp)) {
      ExifDisplayFormatter.rows(photo.exif, photo.fileSizeBytes).forEach { row ->
        Row(Modifier.padding(vertical = 2.dp)) {
          Text(row.label, color = Color(0xFFB7BAC1), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(68.dp))
          Text(row.value, style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifInfoSheet(photo: PhotoAsset, onDismiss: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
      Text(photo.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text(photo.canonicalPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      HorizontalDivider(Modifier.padding(vertical = 16.dp))
      ExifDisplayFormatter.rows(photo.exif, photo.fileSizeBytes).forEach { row ->
        val isLocation = row.label == "地点" && photo.exif.latitude != null && photo.exif.longitude != null
        Row(
          Modifier.fillMaxWidth().clickable(enabled = isLocation) {
            if (isLocation) {
              val uri = "geo:${photo.exif.latitude},${photo.exif.longitude}?q=${photo.exif.latitude},${photo.exif.longitude}".toUri()
              runCatching {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
              }
            }
          }.padding(vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(row.label, Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(row.value, Modifier.weight(1f))
          if (isLocation) Icon(Icons.Rounded.Map, "打开地图")
        }
      }
      Spacer(Modifier.size(24.dp))
    }
  }
}
