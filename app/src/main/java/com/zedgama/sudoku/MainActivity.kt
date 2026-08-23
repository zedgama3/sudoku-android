package com.zedgama.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zedgama.sudoku.ui.SudokuApp
import com.zedgama.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                SudokuApp()
            }
        }
    }
}
