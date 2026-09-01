package com.stxr.lenscull.data

import com.stxr.lenscull.data.db.ProjectDao
import com.stxr.lenscull.data.db.ProjectEntity
import com.stxr.lenscull.domain.CullProject
import com.stxr.lenscull.domain.ProjectSourceType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val projectDao: ProjectDao) {
  fun observeProjects(): Flow<List<CullProject>> = projectDao.observeAll().map { projects -> projects.map { it.toDomain() } }
  fun observeProject(id: String): Flow<CullProject?> = projectDao.observeById(id).map { it?.toDomain() }

  suspend fun create(name: String): CullProject {
    val now = System.currentTimeMillis()
    val entity = ProjectEntity(
      id = UUID.randomUUID().toString(),
      name = name.trim().ifBlank { "未命名项目" },
      sourceType = ProjectSourceType.UNCONFIGURED,
      sourcePath = null,
      createdAt = now,
      updatedAt = now,
    )
    projectDao.upsert(entity)
    return entity.toDomain()
  }

  suspend fun configure(id: String, sourceType: ProjectSourceType, sourcePath: String?) {
    val project = projectDao.getById(id) ?: return
    projectDao.upsert(project.copy(sourceType = sourceType, sourcePath = sourcePath, updatedAt = System.currentTimeMillis()))
  }

  suspend fun delete(id: String) = projectDao.delete(id)

  private fun ProjectEntity.toDomain() = CullProject(id, name, sourceType, sourcePath, createdAt, updatedAt)
}
