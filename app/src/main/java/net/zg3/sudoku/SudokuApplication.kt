package net.zg3.sudoku

import android.app.Application
import net.zg3.sudoku.di.AppContainer

class SudokuApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
