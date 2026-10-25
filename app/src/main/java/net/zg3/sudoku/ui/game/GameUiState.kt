package net.zg3.sudoku.ui.game

import net.zg3.sudoku.data.model.InputMode
import net.zg3.sudoku.data.model.TapAction
import net.zg3.sudoku.engine.Board
import net.zg3.sudoku.engine.Difficulty
import net.zg3.sudoku.engine.HintHighlight
import net.zg3.sudoku.engine.Technique

data class HintUi(
    val message: String,
    val highlight: HintHighlight = HintHighlight(),
    val techniques: List<Technique> = emptyList(),  // non-empty → rendered as clickable chips
    val progress: Pair<Int, Int>? = null,     // (step, of) for the walkthrough; null for mistake/notice cards
    val previous: List<String> = emptyList()  // earlier steps of the same walkthrough, oldest first
)

data class GameUiState(
    val loading: Boolean = true,
    val board: Board = Board.empty(),
    val difficulty: Difficulty = Difficulty.EASY,
    val selectedCell: Int? = null,
    val armedDigit: Int? = null,          // digit-first mode: the digit being painted
    val notesMode: Boolean = false,       // swap: while on, tap and long-press trade actions
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val tapAction: TapAction = TapAction.MARK,
    val elapsedMillis: Long = 0L,
    val paused: Boolean = false,
    val mistakes: Int = 0,
    val mistakeChecking: Boolean = true,
    val hintsUsed: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isWon: Boolean = false,
    val isNewBest: Boolean = false,
    val isFailed: Boolean = false,
    val mistakeLimit: Int? = null,
    val errorCells: Set<Int> = emptySet(),
    val hint: HintUi? = null,
    val showTimer: Boolean = true,
    val hintLogging: Boolean = false,
    val flagNotice: String? = null        // dev: brief confirmation after flagging a hint
) {
    /** Whether a plain tap marks right now (the swap toggle flips the setting). */
    val tapMarks: Boolean
        get() = (tapAction == TapAction.MARK) != notesMode

    /** Digit to highlight everywhere on the board. */
    val highlightDigit: Int?
        get() = armedDigit ?: selectedCell?.let { board[it].value.takeIf { v -> v != 0 } }
}
