package com.example.memostodowidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "memos_settings")

data class MemosSettings(
    val serverUrl: String = "",
    val accessToken: String = ""
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && accessToken.isNotBlank()

    val hasServerUrl: Boolean
        get() = serverUrl.isNotBlank()
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val accessToken = stringPreferencesKey("access_token")
    }

    val settings = context.settingsDataStore.data.map { preferences ->
        MemosSettings(
            serverUrl = preferences[Keys.serverUrl].orEmpty(),
            accessToken = preferences[Keys.accessToken].orEmpty()
        )
    }

    suspend fun getSettings(): MemosSettings = settings.first()

    suspend fun save(serverUrl: String, accessToken: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.serverUrl] = normalizeServerUrl(serverUrl)
            preferences[Keys.accessToken] = accessToken.trim()
        }
    }

    private fun normalizeServerUrl(value: String): String = value.trim().trimEnd('/')
}

