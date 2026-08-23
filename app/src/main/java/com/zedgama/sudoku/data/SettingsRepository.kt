package com.zedgama.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zedgama.sudoku.data.model.InputMode
import com.zedgama.sudoku.data.model.ThemeMode
import com.zedgama.sudoku.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val INPUT_MODE = stringPreferencesKey("input_mode")
        val MISTAKE_CHECKING = booleanPreferencesKey("mistake_checking")
        val MISTAKE_LIMIT = booleanPreferencesKey("mistake_limit")
        val SHOW_TIMER = booleanPreferencesKey("show_timer")
        val AUTO_REMOVE_NOTES = booleanPreferencesKey("auto_remove_notes")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { p ->
        UserSettings(
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            inputMode = p[Keys.INPUT_MODE]?.let { runCatching { InputMode.valueOf(it) }.getOrNull() }
                ?: InputMode.CELL_FIRST,
            mistakeChecking = p[Keys.MISTAKE_CHECKING] ?: true,
            mistakeLimitEnabled = p[Keys.MISTAKE_LIMIT] ?: false,
            showTimer = p[Keys.SHOW_TIMER] ?: true,
            autoRemoveNotes = p[Keys.AUTO_REMOVE_NOTES] ?: true
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setInputMode(mode: InputMode) {
        context.settingsDataStore.edit { it[Keys.INPUT_MODE] = mode.name }
    }

    suspend fun setMistakeChecking(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MISTAKE_CHECKING] = enabled }
    }

    suspend fun setMistakeLimit(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MISTAKE_LIMIT] = enabled }
    }

    suspend fun setShowTimer(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_TIMER] = enabled }
    }

    suspend fun setAutoRemoveNotes(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_REMOVE_NOTES] = enabled }
    }
}
