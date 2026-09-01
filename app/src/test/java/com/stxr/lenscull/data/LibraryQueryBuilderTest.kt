package com.stxr.lenscull.data

import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.LibraryFilter
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.RatingMode
import com.stxr.lenscull.domain.SortDirection
import com.stxr.lenscull.domain.SortField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQueryBuilderTest {
  @Test fun `builds parameterized compound filter`() {
    val query = LibraryQueryBuilder.build(
      "project-1",
      LibraryFilter(
        formats = setOf(PhotoFormat.JPEG, PhotoFormat.RW2),
        ratingMode = RatingMode.AT_LEAST,
        minimumRating = 3,
        flag = CullFlag.PICKED,
        folderPrefix = "/photos/100%",
        sortField = SortField.RATING,
        sortDirection = SortDirection.ASCENDING,
      ),
    )
    assertTrue(query.sql.contains("format IN (?, ?)"))
    assertTrue(query.sql.contains("rating >= ?"))
    assertTrue(query.sql.contains("ORDER BY rating ASC"))
    assertEquals(7, query.argCount)
    assertTrue(query.sql.contains("project_photos.projectId = ?"))
    assertTrue(query.sql.contains("parentPath = ? OR parentPath LIKE ?"))
  }
}
