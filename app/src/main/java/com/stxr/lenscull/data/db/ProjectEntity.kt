package com.stxr.lenscull.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.stxr.lenscull.domain.ProjectSourceType

@Entity(tableName = "projects", primaryKeys = ["id"], indices = [Index("updatedAt")])
data class ProjectEntity(
  val id: String,
  val name: String,
  val sourceType: ProjectSourceType,
  val sourcePath: String?,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "project_photos",
  primaryKeys = ["projectId", "photoId"],
  foreignKeys = [
    ForeignKey(
      entity = ProjectEntity::class,
      parentColumns = ["id"],
      childColumns = ["projectId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = PhotoEntity::class,
      parentColumns = ["id"],
      childColumns = ["photoId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("photoId"), Index("scanGeneration")],
)
data class ProjectPhotoEntity(
  val projectId: String,
  val photoId: String,
  val scanGeneration: String,
)
