package com.zedgama.sudoku.di

import android.app.Application
import com.zedgama.sudoku.data.GameRepository
import com.zedgama.sudoku.data.SettingsRepository
import com.zedgama.sudoku.data.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AppContainer(val application: Application) {
    val applicationScope = CoroutineScope(SupervisorJob())
    val gameRepository by lazy { GameRepository(applicationScope, application) }
    val settingsRepository by lazy { SettingsRepository(application) }
    val statsRepository by lazy { StatsRepository(applicationScope, application) }
}
