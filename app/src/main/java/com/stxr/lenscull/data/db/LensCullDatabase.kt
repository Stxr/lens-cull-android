package com.stxr.lenscull.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LensCullDatabase : RoomDatabase() {
  abstract fun photoDao(): PhotoDao

  companion object {
    fun create(context: Context): LensCullDatabase =
      Room.databaseBuilder(context, LensCullDatabase::class.java, "lenscull.db")
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
  }
}
