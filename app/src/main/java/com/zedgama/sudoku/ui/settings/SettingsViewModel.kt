package com.zedgama.sudoku.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedgama.sudoku.data.SettingsRepository
import com.zedgama.sudoku.data.model.InputMode
import com.zedgama.sudoku.data.model.ThemeMode
import com.zedgama.sudoku.data.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings()
    )

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setTheme(mode) }
    fun setInputMode(mode: InputMode) = viewModelScope.launch { repository.setInputMode(mode) }
    fun setMistakeChecking(v: Boolean) = viewModelScope.launch { repository.setMistakeChecking(v) }
    fun setMistakeLimit(v: Boolean) = viewModelScope.launch { repository.setMistakeLimit(v) }
    fun setShowTimer(v: Boolean) = viewModelScope.launch { repository.setShowTimer(v) }
    fun setAutoRemoveNotes(v: Boolean) = viewModelScope.launch { repository.setAutoRemoveNotes(v) }
}
