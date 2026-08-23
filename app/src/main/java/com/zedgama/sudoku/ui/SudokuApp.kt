package com.zedgama.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zedgama.sudoku.SudokuApplication
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.ui.game.GameScreen
import com.zedgama.sudoku.ui.game.GameViewModel
import com.zedgama.sudoku.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class GameRoute(val difficulty: String)

@Composable
fun SudokuApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SudokuApplication

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                continueInfo = null,
                onContinue = {},
                onNewGame = { difficulty ->
                    navController.navigate(GameRoute(difficulty.name))
                }
            )
        }
        composable<GameRoute> { entry ->
            val route = entry.toRoute<GameRoute>()
            val difficulty = Difficulty.valueOf(route.difficulty)
            val vm: GameViewModel = viewModel {
                GameViewModel(app.container.gameRepository, difficulty)
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
