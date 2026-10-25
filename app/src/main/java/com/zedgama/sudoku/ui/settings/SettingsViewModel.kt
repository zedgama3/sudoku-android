package com.zedgama.sudoku.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedgama.sudoku.data.HintLogger
import com.zedgama.sudoku.data.SettingsRepository
import com.zedgama.sudoku.data.model.AutoSelect
import com.zedgama.sudoku.data.model.InputMode
import com.zedgama.sudoku.data.model.TapAction
import com.zedgama.sudoku.data.model.ThemeMode
import com.zedgama.sudoku.data.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val hintLogger: HintLogger? = null
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings()
    )

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setTheme(mode) }
    fun setInputMode(mode: InputMode) = viewModelScope.launch { repository.setInputMode(mode) }
    fun setMistakeChecking(v: Boolean) = viewModelScope.launch { repository.setMistakeChecking(v) }
    fun setMistakeLimit(v: Boolean) = viewModelScope.launch { repository.setMistakeLimit(v) }
    fun setShowTimer(v: Boolean) = viewModelScope.launch { repository.setShowTimer(v) }
    fun setAutoRemoveNotes(v: Boolean) = viewModelScope.launch { repository.setAutoRemoveNotes(v) }
    fun setTapAction(a: TapAction) = viewModelScope.launch { repository.setTapAction(a) }
    fun setAllowOverwrite(v: Boolean) = viewModelScope.launch { repository.setAllowOverwrite(v) }
    fun setTapRemovesMark(v: Boolean) = viewModelScope.launch { repository.setTapRemovesMark(v) }
    fun setAutoSelect(m: AutoSelect) = viewModelScope.launch { repository.setAutoSelect(m) }
    fun setHintLogging(v: Boolean) = viewModelScope.launch { repository.setHintLogging(v) }
    fun clearHintLog() = viewModelScope.launch { hintLogger?.clear() }
}
