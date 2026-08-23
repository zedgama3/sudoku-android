package com.zedgama.sudoku.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedgama.sudoku.data.StatsRepository
import com.zedgama.sudoku.data.model.GameStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(repository: StatsRepository) : ViewModel() {
    val stats: StateFlow<GameStats> = repository.stats.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), GameStats()
    )
}
