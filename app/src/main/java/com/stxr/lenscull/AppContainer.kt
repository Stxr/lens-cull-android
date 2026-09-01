package com.stxr.lenscull

import android.content.Context
import com.stxr.lenscull.data.PhotoCatalogRepository
import com.stxr.lenscull.data.ProjectRepository
import com.stxr.lenscull.data.SettingsRepository
import com.stxr.lenscull.data.db.LensCullDatabase
import com.stxr.lenscull.metadata.ExifMetadataReader
import com.stxr.lenscull.metadata.MetadataWriter
import com.stxr.lenscull.preview.RawPreviewExtractor
import com.stxr.lenscull.scan.StorageScanner

class AppContainer(context: Context) {
  val database: LensCullDatabase = LensCullDatabase.create(context)
  private val exifMetadataReader = ExifMetadataReader()
  private val metadataWriter = MetadataWriter()
  private val rawPreviewExtractor = RawPreviewExtractor(context)
  val settingsRepository = SettingsRepository(context)
  val photoRepository = PhotoCatalogRepository(database.photoDao(), metadataWriter, rawPreviewExtractor)
  val projectRepository = ProjectRepository(database.projectDao())
  val storageScanner = StorageScanner(context, database.photoDao(), exifMetadataReader)
}
