package com.stxr.lenscull.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.PhotoAsset
import com.stxr.lenscull.domain.RatingSyncState
import java.io.File

@Composable
fun PhotoGrid(
  photos: LazyPagingItems<PhotoAsset>,
  selectedId: String?,
  onSelect: (PhotoAsset) -> Unit,
  previewFile: suspend (PhotoAsset) -> Result<File>,
  modifier: Modifier = Modifier,
) {
  if (photos.itemCount == 0 && photos.loadState.refresh !is androidx.paging.LoadState.Loading) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("没有符合条件的照片", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
  }
  LazyVerticalGrid(
    columns = GridCells.Adaptive(148.dp),
    modifier = modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(count = photos.itemCount, key = photos.itemKey { it.id }) { index ->
      photos[index]?.let { photo ->
        PhotoGridItem(photo, photo.id == selectedId, onSelect, previewFile)
      }
    }
    if (photos.loadState.append is androidx.paging.LoadState.Loading) {
      item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }
  }
}

@Composable
private fun PhotoGridItem(
  photo: PhotoAsset,
  selected: Boolean,
  onSelect: (PhotoAsset) -> Unit,
  previewFile: suspend (PhotoAsset) -> Result<File>,
) {
  val preview by produceState<Result<File>?>(initialValue = null, photo.id, photo.modifiedAt) {
    value = previewFile(photo)
  }
  val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .border(3.dp, borderColor, RoundedCornerShape(10.dp))
      .clickable { onSelect(photo) },
    tonalElevation = if (selected) 4.dp else 1.dp,
  ) {
    Column {
      Box(Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF17191D))) {
        when {
          preview == null -> CircularProgressIndicator(Modifier.size(26.dp).align(Alignment.Center), strokeWidth = 2.dp)
          preview!!.isSuccess -> AsyncImage(
            model = preview!!.getOrNull(),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          else -> Icon(Icons.Rounded.BrokenImage, "预览失败", Modifier.size(42.dp).align(Alignment.Center), tint = Color.LightGray)
        }
        Text(
          photo.format.displayName,
          modifier = Modifier.align(Alignment.TopStart).padding(6.dp).background(Color(0xB0000000), RoundedCornerShape(4.dp)).padding(5.dp, 2.dp),
          color = Color.White,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
        )
        when (photo.flag) {
          CullFlag.PICKED -> Icon(Icons.Rounded.CheckCircle, "保留", Modifier.align(Alignment.TopEnd).padding(6.dp), tint = Color(0xFF5ADB8B))
          CullFlag.REJECTED -> Icon(Icons.Rounded.Close, "淘汰", Modifier.align(Alignment.TopEnd).padding(6.dp), tint = Color(0xFFFF6B6B))
          CullFlag.UNFLAGGED -> Unit
        }
        if (photo.ratingSyncState in setOf(RatingSyncState.FAILED, RatingSyncState.CONFLICT, RatingSyncState.PENDING)) {
          Icon(Icons.Rounded.SyncProblem, "评分未同步", Modifier.align(Alignment.BottomEnd).padding(6.dp), tint = Color(0xFFFFC857))
        }
      }
      Text(
        photo.displayName,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelMedium,
      )
      Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
        repeat(5) { index ->
          Text(if (index < photo.rating) "★" else "☆", color = if (index < photo.rating) Color(0xFFFFC857) else Color.Gray)
        }
      }
    }
  }
}
