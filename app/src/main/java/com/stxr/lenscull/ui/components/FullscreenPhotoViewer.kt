package com.stxr.lenscull.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.stxr.lenscull.domain.PhotoAsset
import java.io.File
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun FullscreenPhotoViewer(
  photo: PhotoAsset,
  photos: List<PhotoAsset>,
  previewFile: suspend (PhotoAsset) -> Result<File>,
  onRating: (Int) -> Unit,
  onSelect: (PhotoAsset) -> Unit,
  onDismiss: () -> Unit,
) {
  val pagerPhotos = remember(photos, photo.id) {
    if (photos.any { it.id == photo.id }) photos else listOf(photo)
  }
  val initialPage = pagerPhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
  val pagerState = rememberPagerState(initialPage = initialPage) { pagerPhotos.size }
  val selectedId by rememberUpdatedState(photo.id)
  val currentOnSelect by rememberUpdatedState(onSelect)
  var ratingMenuVisible by remember(photo.id) { mutableStateOf(false) }

  LaunchedEffect(pagerState, pagerPhotos) {
    snapshotFlow { pagerState.settledPage }
      .distinctUntilChanged()
      .collect { page ->
        val selected = pagerPhotos.getOrNull(page)
        if (selected != null && selected.id != selectedId) {
          ratingMenuVisible = false
          currentOnSelect(selected)
        }
      }
  }

  LaunchedEffect(photo.id, pagerPhotos) {
    val targetPage = pagerPhotos.indexOfFirst { it.id == photo.id }
    if (targetPage >= 0 && targetPage != pagerState.currentPage && !pagerState.isScrollInProgress) {
      pagerState.scrollToPage(targetPage)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { pagerPhotos[it].id },
      ) { page ->
        FullscreenPhotoPage(
          photo = pagerPhotos[page],
          previewFile = previewFile,
          onTap = { ratingMenuVisible = !ratingMenuVisible },
        )
      }

      IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
        Icon(Icons.Rounded.Close, "退出全屏", tint = Color.White)
      }

      if (ratingMenuVisible) {
        val visiblePhoto = pagerPhotos.getOrNull(pagerState.currentPage) ?: photo
        Surface(
          modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
          color = Color(0xD914171C),
          contentColor = Color.White,
        ) {
          Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(visiblePhoto.displayName, maxLines = 1)
            RatingBar(visiblePhoto.rating, onRating, modifier = Modifier.padding(top = 10.dp))
            Text("再次点击照片可隐藏评分菜单", color = Color.LightGray, modifier = Modifier.padding(top = 6.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun FullscreenPhotoPage(
  photo: PhotoAsset,
  previewFile: suspend (PhotoAsset) -> Result<File>,
  onTap: () -> Unit,
) {
  val preview by produceState<Result<File>?>(null, photo.id, photo.modifiedAt) { value = previewFile(photo) }
  Box(Modifier.fillMaxSize()) {
    if (preview?.isSuccess == true) {
      val zoomState = rememberCoilZoomState()
      CoilZoomAsyncImage(
        model = preview!!.getOrNull(),
        contentDescription = "全屏 ${photo.displayName}",
        modifier = Modifier.fillMaxSize(),
        zoomState = zoomState,
        onTap = { onTap() },
      )
    } else if (preview?.isFailure == true) {
      Text(
        "无法预览：${preview!!.exceptionOrNull()?.message.orEmpty()}",
        color = Color.White,
        modifier = Modifier.align(Alignment.Center),
      )
    }
  }
}
