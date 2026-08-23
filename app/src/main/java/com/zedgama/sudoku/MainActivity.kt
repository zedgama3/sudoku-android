package com.zedgama.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.ui.game.GameScreen
import com.zedgama.sudoku.ui.game.GameViewModel
import com.zedgama.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                val vm: GameViewModel = viewModel { GameViewModel(Difficulty.MEDIUM) }
                GameScreen(viewModel = vm)
            }
        }
    }
}
