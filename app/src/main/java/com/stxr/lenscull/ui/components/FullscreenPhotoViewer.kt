package com.stxr.lenscull.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.stxr.lenscull.domain.PhotoAsset
import java.io.File

@Composable
fun FullscreenPhotoViewer(
  photo: PhotoAsset,
  previewFile: suspend (PhotoAsset) -> Result<File>,
  onRating: (Int) -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onDismiss: () -> Unit,
) {
  val preview by produceState<Result<File>?>(null, photo.id, photo.modifiedAt) { value = previewFile(photo) }
  var ratingMenuVisible by remember(photo.id) { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
      if (preview?.isSuccess == true) {
        key(photo.id) {
          val zoomState = rememberCoilZoomState()
          val gestureModifier = Modifier.pointerInput(photo.id, zoomState) {
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

              val deltaX = lastPosition.x - down.position.x
              val deltaY = lastPosition.y - down.position.y
              val swipe = detectPhotoSwipe(
                deltaX = deltaX,
                deltaY = deltaY,
                viewportWidthPx = size.width.toFloat(),
                touchSlopPx = viewConfiguration.touchSlop,
                enabled = startedAtBaseScale && !multiplePointers,
              )
              when (swipe) {
                PhotoSwipeDirection.PREVIOUS -> onPrevious()
                PhotoSwipeDirection.NEXT -> onNext()
                null -> if (!multiplePointers && kotlin.math.hypot(deltaX, deltaY) <= viewConfiguration.touchSlop) {
                  ratingMenuVisible = !ratingMenuVisible
                }
              }
            }
          }
          CoilZoomAsyncImage(
            model = preview!!.getOrNull(),
            contentDescription = "全屏 ${photo.displayName}",
            modifier = Modifier.fillMaxSize().then(gestureModifier),
            zoomState = zoomState,
          )
        }
      } else if (preview?.isFailure == true) {
        Text("无法预览：${preview!!.exceptionOrNull()?.message.orEmpty()}", color = Color.White, modifier = Modifier.align(Alignment.Center))
      }

      IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
        Icon(Icons.Rounded.Close, "退出全屏", tint = Color.White)
      }

      if (ratingMenuVisible) {
        Surface(
          modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
          color = Color(0xD914171C),
          contentColor = Color.White,
        ) {
          Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(photo.displayName, maxLines = 1)
            RatingBar(photo.rating, onRating, modifier = Modifier.padding(top = 10.dp))
            Text("再次点击照片可隐藏评分菜单", color = Color.LightGray, modifier = Modifier.padding(top = 6.dp))
          }
        }
      }
    }
  }
}
