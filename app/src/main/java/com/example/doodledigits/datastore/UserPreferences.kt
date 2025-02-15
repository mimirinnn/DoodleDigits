package com.example.doodledigits.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// 🔹 Ініціалізація DataStore (зберігається у файлі "user_preferences")
private val Context.dataStore by preferencesDataStore("user_preferences")

class UserPreferences(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_MODE = booleanPreferencesKey("theme_mode") // true = Dark, false = Light
        val SAVE_PHOTOS = booleanPreferencesKey("save_photos") // true = Save, false = Don't save
    }

    // Отримання поточного значення теми
    val themeMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: false // За замовчуванням світла тема
    }

    // Отримання статусу "Зберігати фото"
    val savePhotos: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SAVE_PHOTOS] ?: false
    }

    // Оновлення теми (збереження в DataStore)
    suspend fun setThemeMode(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = isDark
        }
    }

    // Оновлення "Зберігати фото"
    suspend fun setSavePhotos(save: Boolean) {
        dataStore.edit { preferences ->
            preferences[SAVE_PHOTOS] = save
        }
    }

    // Отримання теми (синхронно, корисно при запуску)
    fun getThemeModeSync(): Boolean = runBlocking {
        dataStore.data.first()[THEME_MODE] ?: false
    }
}
