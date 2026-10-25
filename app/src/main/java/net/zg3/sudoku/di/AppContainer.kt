package net.zg3.sudoku.di

import android.app.Application
import net.zg3.sudoku.data.GameRepository
import net.zg3.sudoku.data.HintLogger
import net.zg3.sudoku.data.SettingsRepository
import net.zg3.sudoku.data.StatsRepository
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
