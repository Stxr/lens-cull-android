package com.stxr.lenscull.data

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.stxr.lenscull.domain.LibraryFilter
import com.stxr.lenscull.domain.RatingMode
import com.stxr.lenscull.domain.SortDirection
import com.stxr.lenscull.domain.SortField

object LibraryQueryBuilder {
  fun build(projectId: String, filter: LibraryFilter): SupportSQLiteQuery {
    val where = mutableListOf("project_photos.projectId = ?")
    val arguments = mutableListOf<Any>(projectId)

    if (filter.formats.isEmpty()) {
      where += "0"
    } else {
      where += "format IN (${filter.formats.joinToString { "?" }})"
      arguments.addAll(filter.formats.map { it.name })
    }
    when (filter.ratingMode) {
      RatingMode.ALL -> Unit
      RatingMode.UNRATED -> where += "rating = 0"
      RatingMode.AT_LEAST -> {
        where += "rating >= ?"
        arguments += filter.minimumRating.coerceIn(1, 5)
      }
    }
    filter.flag?.let {
      where += "flag = ?"
      arguments += it.name
    }
    filter.folderPrefix?.takeIf(String::isNotBlank)?.let {
      val folder = it.trimEnd('/')
      where += "(parentPath = ? OR parentPath LIKE ? ESCAPE '\\')"
      arguments += folder
      arguments += "${escapeLike(folder)}/%"
    }
    filter.capturedAfter?.let {
      where += "capturedAt >= ?"
      arguments += it
    }
    filter.capturedBefore?.let {
      where += "capturedAt <= ?"
      arguments += it
    }

    val orderColumn = when (filter.sortField) {
      SortField.CAPTURED_AT -> "COALESCE(capturedAt, modifiedAt)"
      SortField.FILE_NAME -> "displayName COLLATE NOCASE"
      SortField.RATING -> "rating"
    }
    val direction = if (filter.sortDirection == SortDirection.ASCENDING) "ASC" else "DESC"
    val sql = buildString {
      append("SELECT photos.* FROM photos INNER JOIN project_photos ON photos.id = project_photos.photoId")
      if (where.isNotEmpty()) append(" WHERE ${where.joinToString(" AND ")}")
      append(" ORDER BY $orderColumn $direction, canonicalPath ASC")
    }
    return SimpleSQLiteQuery(sql, arguments.toTypedArray())
  }

  private fun escapeLike(value: String): String =
    value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
