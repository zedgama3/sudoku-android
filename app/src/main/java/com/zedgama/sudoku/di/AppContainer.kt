package com.zedgama.sudoku.di

import android.app.Application
import com.zedgama.sudoku.data.GameRepository
import com.zedgama.sudoku.data.HintLogger
import com.zedgama.sudoku.data.SettingsRepository
import com.zedgama.sudoku.data.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File

class AppContainer(val application: Application) {
    val applicationScope = CoroutineScope(SupervisorJob())
    val gameRepository by lazy { GameRepository(applicationScope, application) }
    val settingsRepository by lazy { SettingsRepository(application) }
    val statsRepository by lazy { StatsRepository(applicationScope, application) }
    val hintLogger by lazy {
        val root = application.getExternalFilesDir(null) ?: application.filesDir
        HintLogger(File(root, "hintlog"))
    }
}
