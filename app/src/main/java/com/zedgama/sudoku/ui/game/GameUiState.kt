package com.zedgama.sudoku.ui.game

import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.Difficulty

enum class InputMode { CELL_FIRST, DIGIT_FIRST }

data class HintUi(val message: String, val cells: List<Int>)

data class GameUiState(
    val loading: Boolean = true,
    val board: Board = Board.empty(),
    val difficulty: Difficulty = Difficulty.EASY,
    val selectedCell: Int? = null,
    val armedDigit: Int? = null,          // digit-first mode: the digit being painted
    val notesMode: Boolean = false,
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val elapsedMillis: Long = 0L,
    val paused: Boolean = false,
    val mistakes: Int = 0,
    val mistakeChecking: Boolean = true,
    val hintsUsed: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isWon: Boolean = false,
    val errorCells: Set<Int> = emptySet(),
    val hint: HintUi? = null,
    val showTimer: Boolean = true
) {
    /** Digit to highlight everywhere on the board. */
    val highlightDigit: Int?
        get() = armedDigit ?: selectedCell?.let { board[it].value.takeIf { v -> v != 0 } }
}
