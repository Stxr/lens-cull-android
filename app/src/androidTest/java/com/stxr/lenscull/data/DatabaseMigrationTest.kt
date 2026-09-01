package com.stxr.lenscull.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stxr.lenscull.data.db.LensCullDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
  @get:Rule
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    LensCullDatabase::class.java,
  )

  @Test fun migratesCatalogFromVersion1ToProjectSchema() {
    helper.createDatabase(DATABASE_NAME, 1).close()
    helper.runMigrationsAndValidate(DATABASE_NAME, 2, true, LensCullDatabase.MIGRATION_1_2).use { db ->
      db.query("SELECT COUNT(*) FROM projects").use { cursor -> check(cursor.moveToFirst()) }
      db.query("SELECT COUNT(*) FROM project_photos").use { cursor -> check(cursor.moveToFirst()) }
    }
  }

  private companion object { const val DATABASE_NAME = "migration-projects-test" }
}
