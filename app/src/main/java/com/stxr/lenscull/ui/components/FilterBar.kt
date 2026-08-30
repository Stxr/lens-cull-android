package com.stxr.lenscull.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.LibraryFilter
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.RatingMode
import com.stxr.lenscull.domain.SortDirection
import com.stxr.lenscull.domain.SortField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
  filter: LibraryFilter,
  folders: List<String>,
  onToggleFormat: (PhotoFormat) -> Unit,
  onRating: (RatingMode, Int) -> Unit,
  onFlag: (CullFlag?) -> Unit,
  onFolder: (String?) -> Unit,
  onDateRange: (Long?, Long?) -> Unit,
  onSort: (SortField, SortDirection) -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var formatMenu by remember { mutableStateOf(false) }
  var ratingMenu by remember { mutableStateOf(false) }
  var flagMenu by remember { mutableStateOf(false) }
  var folderMenu by remember { mutableStateOf(false) }
  var sortMenu by remember { mutableStateOf(false) }
  var dateDialog by remember { mutableStateOf(false) }
  val dateState = rememberDateRangePickerState(
    initialSelectedStartDateMillis = filter.capturedAfter,
    initialSelectedEndDateMillis = filter.capturedBefore,
  )

  Row(modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
    MenuChip("格式 ${filter.formats.size}", Icons.Rounded.ImageSearch, { formatMenu = true }) {
      DropdownMenu(expanded = formatMenu, onDismissRequest = { formatMenu = false }) {
        PhotoFormat.entries.forEach { format ->
          DropdownMenuItem(
            text = { Text(format.displayName) },
            leadingIcon = { Checkbox(format in filter.formats, onCheckedChange = null) },
            onClick = { onToggleFormat(format) },
          )
        }
      }
    }
    MenuChip(ratingLabel(filter), Icons.Rounded.Star, { ratingMenu = true }) {
      DropdownMenu(expanded = ratingMenu, onDismissRequest = { ratingMenu = false }) {
        DropdownMenuItem(text = { Text("全部星级") }, onClick = { onRating(RatingMode.ALL, 0); ratingMenu = false })
        DropdownMenuItem(text = { Text("未评分") }, onClick = { onRating(RatingMode.UNRATED, 0); ratingMenu = false })
        (1..5).forEach { rating ->
          DropdownMenuItem(text = { Text("至少 $rating 星") }, onClick = {
            onRating(RatingMode.AT_LEAST, rating); ratingMenu = false
          })
        }
      }
    }
    MenuChip(flagLabel(filter.flag), Icons.Rounded.ImageSearch, { flagMenu = true }) {
      DropdownMenu(expanded = flagMenu, onDismissRequest = { flagMenu = false }) {
        listOf<CullFlag?>(null, CullFlag.UNFLAGGED, CullFlag.PICKED, CullFlag.REJECTED).forEach { flag ->
          DropdownMenuItem(text = { Text(flagLabel(flag)) }, onClick = { onFlag(flag); flagMenu = false })
        }
      }
    }
    MenuChip(filter.folderPrefix?.substringAfterLast('/') ?: "全部目录", Icons.Rounded.Folder, { folderMenu = true }) {
      DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
        DropdownMenuItem(text = { Text("全部目录") }, onClick = { onFolder(null); folderMenu = false })
        folders.take(100).forEach { folder ->
          DropdownMenuItem(text = { Text(folder) }, onClick = { onFolder(folder); folderMenu = false })
        }
      }
    }
    AssistChip(
      onClick = { dateDialog = true },
      label = { Text(if (filter.capturedAfter == null) "日期" else "已选日期") },
      leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null) },
    )
    Spacer(Modifier.width(8.dp))
    MenuChip(sortLabel(filter), Icons.AutoMirrored.Rounded.Sort, { sortMenu = true }) {
      DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
        SortField.entries.forEach { field ->
          DropdownMenuItem(text = { Text(sortFieldLabel(field)) }, onClick = {
            onSort(field, filter.sortDirection); sortMenu = false
          })
        }
        DropdownMenuItem(
          text = { Text(if (filter.sortDirection == SortDirection.ASCENDING) "改为降序" else "改为升序") },
          onClick = {
            onSort(filter.sortField, if (filter.sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING)
            sortMenu = false
          },
        )
      }
    }
    IconButton(onClick = onClear) { Icon(Icons.Rounded.ClearAll, "清空筛选") }
  }

  if (dateDialog) {
    DatePickerDialog(
      onDismissRequest = { dateDialog = false },
      confirmButton = {
        TextButton(onClick = {
          onDateRange(dateState.selectedStartDateMillis, dateState.selectedEndDateMillis?.plus(DAY_MILLIS - 1))
          dateDialog = false
        }) { Text("确定") }
      },
      dismissButton = {
        TextButton(onClick = { onDateRange(null, null); dateDialog = false }) { Text("清除") }
      },
    ) { DateRangePicker(state = dateState) }
  }
}

@Composable
private fun MenuChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  menu: @Composable () -> Unit,
) {
  androidx.compose.foundation.layout.Box(Modifier.padding(end = 8.dp)) {
    AssistChip(onClick = onClick, label = { Text(label, maxLines = 1) }, leadingIcon = { Icon(icon, null) })
    menu()
  }
}

private fun ratingLabel(filter: LibraryFilter): String = when (filter.ratingMode) {
  RatingMode.ALL -> "全部星级"
  RatingMode.UNRATED -> "未评分"
  RatingMode.AT_LEAST -> "${filter.minimumRating} 星以上"
}

private fun flagLabel(flag: CullFlag?): String = when (flag) {
  null -> "全部标记"
  CullFlag.UNFLAGGED -> "未标记"
  CullFlag.PICKED -> "保留"
  CullFlag.REJECTED -> "淘汰"
}

private fun sortLabel(filter: LibraryFilter): String =
  "${sortFieldLabel(filter.sortField)} ${if (filter.sortDirection == SortDirection.ASCENDING) "↑" else "↓"}"

private fun sortFieldLabel(field: SortField): String = when (field) {
  SortField.CAPTURED_AT -> "拍摄时间"
  SortField.FILE_NAME -> "文件名"
  SortField.RATING -> "评分"
}

private const val DAY_MILLIS = 86_400_000L
