package com.example.mychatroom.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "setting_prefs")

class SettingManager(private val context: Context) {

    companion object {
        val THEME_MODE = booleanPreferencesKey("theme_mode")
    }

    suspend fun saveThemeMode(
        themeMode: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode
        }
    }

    fun getThemeMode(): Flow<Boolean> = context.dataStore.data.map { it[THEME_MODE] ?: false }

}