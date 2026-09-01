package com.stxr.lenscull.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PhotoEntity::class, ProjectEntity::class, ProjectPhotoEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LensCullDatabase : RoomDatabase() {
  abstract fun photoDao(): PhotoDao
  abstract fun projectDao(): ProjectDao

  companion object {
    fun create(context: Context): LensCullDatabase =
      Room.databaseBuilder(context, LensCullDatabase::class.java, "lenscull.db")
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS projects (id TEXT NOT NULL, name TEXT NOT NULL, sourceType TEXT NOT NULL, sourcePath TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_updatedAt ON projects(updatedAt)")
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS project_photos (projectId TEXT NOT NULL, photoId TEXT NOT NULL, scanGeneration TEXT NOT NULL, PRIMARY KEY(projectId, photoId), FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(photoId) REFERENCES photos(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_project_photos_photoId ON project_photos(photoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_project_photos_scanGeneration ON project_photos(scanGeneration)")
      }
    }
  }
}
