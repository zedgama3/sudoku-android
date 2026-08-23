package com.zedgama.sudoku.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.Cell
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.engine.Generator
import com.zedgama.sudoku.engine.Hint
import com.zedgama.sudoku.engine.HintEngine
import com.zedgama.sudoku.engine.boxOf
import com.zedgama.sudoku.engine.colOf
import com.zedgama.sudoku.engine.rowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val requestedDifficulty: Difficulty
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState(loading = true))
    val state: StateFlow<GameUiState> = _state

    private var solution: IntArray = IntArray(81)
    private val undoStack = ArrayDeque<Board>()
    private val redoStack = ArrayDeque<Board>()

    init {
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) {
                Generator().generate(requestedDifficulty)
            }
            solution = IntArray(81) { puzzle.solution[it] - '0' }
            _state.update {
                GameUiState(
                    loading = false,
                    board = Board.fromGivens(puzzle.givens),
                    difficulty = requestedDifficulty
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { s ->
                    if (!s.paused && !s.isWon && !s.loading) {
                        s.copy(elapsedMillis = s.elapsedMillis + 1000)
                    } else s
                }
            }
        }
    }

    fun onCellTap(index: Int) {
        val s = _state.value
        if (s.loading || s.paused || s.isWon) return
        if (s.inputMode == InputMode.DIGIT_FIRST && s.armedDigit != null) {
            applyDigit(index, s.armedDigit)
        } else {
            _state.update { it.copy(selectedCell = index, hint = null) }
        }
    }

    fun onDigit(digit: Int) {
        val s = _state.value
        if (s.loading || s.paused || s.isWon) return
        if (s.inputMode == InputMode.DIGIT_FIRST) {
            _state.update {
                it.copy(armedDigit = if (it.armedDigit == digit) null else digit, hint = null)
            }
        } else {
            s.selectedCell?.let { applyDigit(it, digit) }
        }
    }

    fun onErase() {
        val s = _state.value
        if (s.loading || s.paused || s.isWon) return
        val index = s.selectedCell ?: return
        val cell = s.board[index]
        if (cell.isGiven || (cell.value == 0 && cell.notes == 0)) return
        pushUndo(s.board)
        updateBoard(s.board.with(index, cell.copy(value = 0, notes = 0)))
    }

    fun onToggleNotes() {
        _state.update { it.copy(notesMode = !it.notesMode) }
    }

    fun onUndo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.board)
        updateBoard(previous, countMistake = false)
    }

    fun onRedo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.board)
        updateBoard(next, countMistake = false)
    }

    fun onTogglePause() {
        _state.update { s -> if (s.isWon) s else s.copy(paused = !s.paused, hint = null) }
    }

    fun onHint() {
        val s = _state.value
        if (s.loading || s.paused || s.isWon) return
        val current = s.board.values()
        when (val hint = HintEngine.hint(current, solution, s.selectedCell)) {
            is Hint.WrongCell -> {
                pushUndo(s.board)
                val fixed = s.board.with(hint.index, s.board[hint.index].copy(value = hint.correct, notes = 0))
                _state.update {
                    it.copy(
                        selectedCell = hint.index,
                        hintsUsed = it.hintsUsed + 1,
                        hint = HintUi("That cell was wrong — corrected to ${hint.correct}.", listOf(hint.index))
                    )
                }
                updateBoard(clearPeerNotes(fixed, hint.index, hint.correct))
            }
            is Hint.Logical -> {
                pushUndo(s.board)
                val step = hint.step
                val placed = s.board.with(step.cellIndex, s.board[step.cellIndex].copy(value = step.value, notes = 0))
                _state.update {
                    it.copy(
                        selectedCell = step.cellIndex,
                        hintsUsed = it.hintsUsed + 1,
                        hint = HintUi(step.message, step.involvedCells)
                    )
                }
                updateBoard(clearPeerNotes(placed, step.cellIndex, step.value))
            }
            is Hint.Reveal -> {
                pushUndo(s.board)
                val placed = s.board.with(hint.index, s.board[hint.index].copy(value = hint.value, notes = 0))
                _state.update {
                    it.copy(
                        selectedCell = hint.index,
                        hintsUsed = it.hintsUsed + 1,
                        hint = HintUi("Revealed: ${hint.value} goes here.", listOf(hint.index))
                    )
                }
                updateBoard(clearPeerNotes(placed, hint.index, hint.value))
            }
            null -> Unit
        }
    }

    private fun applyDigit(index: Int, digit: Int) {
        val s = _state.value
        val cell = s.board[index]
        if (cell.isGiven) {
            _state.update { it.copy(selectedCell = index, hint = null) }
            return
        }
        pushUndo(s.board)
        val newBoard: Board
        var mistake = false
        if (s.notesMode && cell.value == 0) {
            newBoard = s.board.with(index, cell.copy(notes = cell.notes xor (1 shl (digit - 1))))
        } else if (cell.value == digit) {
            // Tapping the same digit clears the cell
            newBoard = s.board.with(index, cell.copy(value = 0))
        } else {
            newBoard = clearPeerNotes(
                s.board.with(index, cell.copy(value = digit, notes = 0)),
                index, digit
            )
            mistake = s.mistakeChecking && solution[index] != digit
        }
        _state.update { it.copy(selectedCell = index, hint = null) }
        updateBoard(newBoard, countMistake = mistake)
    }

    /** Remove [digit] from the notes of all peers of [index]. */
    private fun clearPeerNotes(board: Board, index: Int, digit: Int): Board {
        val bit = 1 shl (digit - 1)
        var result = board
        for (i in 0 until 81) {
            if (i == index) continue
            val c = result[i]
            if (c.value == 0 && c.notes and bit != 0 &&
                (rowOf(i) == rowOf(index) || colOf(i) == colOf(index) || boxOf(i) == boxOf(index))
            ) {
                result = result.with(i, c.copy(notes = c.notes and bit.inv()))
            }
        }
        return result
    }

    private fun pushUndo(board: Board) {
        undoStack.addLast(board)
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
    }

    private fun updateBoard(board: Board, countMistake: Boolean = false) {
        val errors = if (_state.value.mistakeChecking) {
            (0 until 81).filter { i ->
                val v = board[i].value
                v != 0 && !board[i].isGiven && v != solution[i]
            }.toSet()
        } else {
            board.conflictingCells()
        }
        val won = board.isFull && (0 until 81).all { board[it].value == solution[it] }
        _state.update { s ->
            s.copy(
                board = board,
                mistakes = s.mistakes + if (countMistake) 1 else 0,
                errorCells = errors,
                isWon = won,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                armedDigit = if (won) null else s.armedDigit,
                selectedCell = if (won) null else s.selectedCell
            )
        }
    }

    private companion object {
        const val MAX_UNDO = 100
    }
}
