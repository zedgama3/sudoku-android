package net.zg3.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import net.zg3.sudoku.data.model.AutoSelect
import net.zg3.sudoku.data.model.InputMode
import net.zg3.sudoku.data.model.TapAction
import net.zg3.sudoku.data.model.ThemeMode
import net.zg3.sudoku.data.model.UserSettings
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
        val TAP_ACTION = stringPreferencesKey("tap_action")
        val ALLOW_OVERWRITE = booleanPreferencesKey("allow_overwrite")
        val TAP_REMOVES_MARK = booleanPreferencesKey("tap_removes_mark")
        val AUTO_SELECT = stringPreferencesKey("auto_select")
        val HINT_LOGGING = booleanPreferencesKey("hint_logging")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
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
            autoRemoveNotes = p[Keys.AUTO_REMOVE_NOTES] ?: true,
            tapAction = p[Keys.TAP_ACTION]?.let { runCatching { TapAction.valueOf(it) }.getOrNull() }
                ?: TapAction.MARK,
            allowOverwrite = p[Keys.ALLOW_OVERWRITE] ?: false,
            tapRemovesMark = p[Keys.TAP_REMOVES_MARK] ?: true,
            autoSelect = p[Keys.AUTO_SELECT]?.let { runCatching { AutoSelect.valueOf(it) }.getOrNull() }
                ?: AutoSelect.WHEN_FILLED,
            hintLogging = p[Keys.HINT_LOGGING] ?: false,
            setupDone = p[Keys.SETUP_DONE] ?: false
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

    suspend fun setTapAction(action: TapAction) {
        context.settingsDataStore.edit { it[Keys.TAP_ACTION] = action.name }
    }

    suspend fun setAllowOverwrite(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALLOW_OVERWRITE] = enabled }
    }

    suspend fun setTapRemovesMark(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TAP_REMOVES_MARK] = enabled }
    }

    suspend fun setAutoSelect(mode: AutoSelect) {
        context.settingsDataStore.edit { it[Keys.AUTO_SELECT] = mode.name }
    }

    suspend fun setHintLogging(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HINT_LOGGING] = enabled }
    }

    suspend fun setSetupDone() {
        context.settingsDataStore.edit { it[Keys.SETUP_DONE] = true }
    }
}
