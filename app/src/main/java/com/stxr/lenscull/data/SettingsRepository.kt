package com.stxr.lenscull.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lensCullSettings by preferencesDataStore(name = "lenscull-settings")

class SettingsRepository(private val context: Context) {
  val metadataWriteConsent: Flow<Boolean> = context.lensCullSettings.data
    .map { preferences -> preferences[METADATA_WRITE_CONSENT] ?: false }

  suspend fun setMetadataWriteConsent(consented: Boolean) {
    context.lensCullSettings.edit { it[METADATA_WRITE_CONSENT] = consented }
  }

  private companion object {
    val METADATA_WRITE_CONSENT = booleanPreferencesKey("metadata_write_consent")
  }
}
