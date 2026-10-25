package net.zg3.sudoku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import net.zg3.sudoku.SudokuApplication
import net.zg3.sudoku.data.model.ThemeMode
import net.zg3.sudoku.data.model.UserSettings
import net.zg3.sudoku.engine.Difficulty
import net.zg3.sudoku.engine.Technique
import net.zg3.sudoku.ui.about.AboutScreen
import net.zg3.sudoku.ui.game.GameScreen
import net.zg3.sudoku.ui.game.GameViewModel
import net.zg3.sudoku.ui.home.ContinueInfo
import net.zg3.sudoku.ui.home.HomeScreen
import net.zg3.sudoku.ui.settings.SettingsScreen
import net.zg3.sudoku.ui.settings.SettingsViewModel
import net.zg3.sudoku.ui.stats.StatsScreen
import net.zg3.sudoku.ui.stats.StatsViewModel
import net.zg3.sudoku.ui.technique.TechniqueScreen
import net.zg3.sudoku.ui.theme.SudokuTheme
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class GameRoute(val difficulty: String, val resume: Boolean = false)

@Serializable
data class TechniqueRoute(val technique: String)

@Serializable
object StatsRoute

@Serializable
object SettingsRoute

@Serializable
object AboutRoute

@Composable
fun SudokuApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SudokuApplication
    val container = app.container

    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = UserSettings())
    val darkTheme = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SudokuTheme(darkTheme = darkTheme) {
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                val saved by container.gameRepository.savedGameFlow
                    .collectAsStateWithLifecycle(initialValue = null)
                HomeScreen(
                    continueInfo = saved?.let { ContinueInfo(it.difficulty, it.elapsedMillis) },
                    onContinue = {
                        saved?.let {
                            navController.navigate(GameRoute(it.difficulty.name, resume = true))
                        }
                    },
                    onNewGame = { difficulty ->
                        navController.navigate(GameRoute(difficulty.name))
                    },
                    onStats = { navController.navigate(StatsRoute) },
                    onSettings = { navController.navigate(SettingsRoute) }
                )
            }
            composable<GameRoute> { entry ->
                val route = entry.toRoute<GameRoute>()
                val difficulty = Difficulty.valueOf(route.difficulty)
                val vm: GameViewModel = viewModel {
                    GameViewModel(
                        container.gameRepository,
                        difficulty,
                        route.resume,
                        createSavedStateHandle(),
                        container.settingsRepository,
                        container.statsRepository,
                        container.hintLogger
                    )
                }
                GameScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNewGame = {
                        navController.navigate(GameRoute(difficulty.name)) {
                            popUpTo<HomeRoute>()
                        }
                    },
                    onOpenTechnique = { technique ->
                        navController.navigate(TechniqueRoute(technique.name))
                    }
                )
            }
            composable<TechniqueRoute> { entry ->
                val route = entry.toRoute<TechniqueRoute>()
                TechniqueScreen(
                    technique = Technique.valueOf(route.technique),
                    onBack = { navController.popBackStack() }
                )
            }
            composable<StatsRoute> {
                val vm: StatsViewModel = viewModel { StatsViewModel(container.statsRepository) }
                StatsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable<SettingsRoute> {
                val vm: SettingsViewModel =
                    viewModel { SettingsViewModel(container.settingsRepository, container.hintLogger) }
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate(AboutRoute) }
                )
            }
            composable<AboutRoute> {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
