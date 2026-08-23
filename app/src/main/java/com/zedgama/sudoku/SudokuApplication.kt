package com.zedgama.sudoku

import android.app.Application
import com.zedgama.sudoku.di.AppContainer

class SudokuApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
