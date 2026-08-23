package com.zedgama.sudoku.ui

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
import com.zedgama.sudoku.SudokuApplication
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.ui.game.GameScreen
import com.zedgama.sudoku.ui.game.GameViewModel
import com.zedgama.sudoku.ui.home.ContinueInfo
import com.zedgama.sudoku.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class GameRoute(val difficulty: String, val resume: Boolean = false)

@Composable
fun SudokuApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SudokuApplication

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val saved by app.container.gameRepository.savedGameFlow
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
                }
            )
        }
        composable<GameRoute> { entry ->
            val route = entry.toRoute<GameRoute>()
            val difficulty = Difficulty.valueOf(route.difficulty)
            val vm: GameViewModel = viewModel {
                GameViewModel(
                    app.container.gameRepository,
                    difficulty,
                    route.resume,
                    createSavedStateHandle()
                )
            }
            GameScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNewGame = {
                    navController.navigate(GameRoute(difficulty.name)) {
                        popUpTo<HomeRoute>()
                    }
                }
            )
        }
    }
}
