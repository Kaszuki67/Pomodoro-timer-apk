package com.simplepomodoro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val WORK_DURATION = intPreferencesKey("work_duration")
    val SHORT_BREAK_DURATION = intPreferencesKey("short_break_duration")
    val LONG_BREAK_DURATION = intPreferencesKey("long_break_duration")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
}

class SettingsRepository(private val context: Context) {
    
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            workDuration = preferences[PreferencesKeys.WORK_DURATION] ?: 25 * 60,
            shortBreakDuration = preferences[PreferencesKeys.SHORT_BREAK_DURATION] ?: 5 * 60,
            longBreakDuration = preferences[PreferencesKeys.LONG_BREAK_DURATION] ?: 15 * 60,
            keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true
        )
    }
    
    suspend fun updateWorkDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WORK_DURATION] = seconds
        }
    }
    
    suspend fun updateShortBreakDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHORT_BREAK_DURATION] = seconds
        }
    }
    
    suspend fun updateLongBreakDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LONG_BREAK_DURATION] = seconds
        }
    }
    
    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = enabled
        }
    }
}

data class Settings(
    val workDuration: Int = 25 * 60,
    val shortBreakDuration: Int = 5 * 60,
    val longBreakDuration: Int = 15 * 60,
    val keepScreenOn: Boolean = true
)
