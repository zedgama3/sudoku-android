package com.zedgama.sudoku.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedgama.sudoku.data.GameRepository
import com.zedgama.sudoku.data.SettingsRepository
import com.zedgama.sudoku.data.StatsRepository
import com.zedgama.sudoku.data.model.InputMode
import com.zedgama.sudoku.data.model.SavedGame
import com.zedgama.sudoku.data.model.boardFrom
import com.zedgama.sudoku.data.model.toBoard
import com.zedgama.sudoku.data.model.toSnapshot
import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.engine.HintEngine
import com.zedgama.sudoku.engine.HintEscalation
import com.zedgama.sudoku.engine.HintHighlight
import com.zedgama.sudoku.engine.boxOf
import com.zedgama.sudoku.engine.colOf
import com.zedgama.sudoku.engine.rowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
    private val requestedDifficulty: Difficulty,
    resume: Boolean = false,
    savedStateHandle: SavedStateHandle? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val statsRepository: StatsRepository? = null
) : ViewModel() {

    // After process death the nav back stack is restored and this ViewModel is
    // recreated with its original arguments; without this flag a "new game"
    // route would generate a fresh puzzle and clobber the auto-saved one.
    private val resume: Boolean =
        resume || savedStateHandle?.get<Boolean>(KEY_STARTED) == true

    init {
        savedStateHandle?.set(KEY_STARTED, true)
    }

    private val _state = MutableStateFlow(GameUiState(loading = true))
    val state: StateFlow<GameUiState> = _state

    private var givens: String = ""
    private var solutionString: String = ""
    private var solution: IntArray = IntArray(81)
    private val undoStack = ArrayDeque<Board>()
    private val redoStack = ArrayDeque<Board>()
    private val hintEscalation = HintEscalation()
    private var inForeground = true
    private var autoRemoveNotes = true
    private var resultRecorded = false

    init {
        viewModelScope.launch {
            val saved = if (this@GameViewModel.resume) repository.loadSavedGame() else null
            if (saved != null) {
                restore(saved)
            } else {
                val puzzle = repository.nextPuzzle(requestedDifficulty)
                givens = puzzle.givens
                solutionString = puzzle.solution
                solution = IntArray(81) { puzzle.solution[it] - '0' }
                _state.update {
                    GameUiState(
                        loading = false,
                        board = Board.fromGivens(puzzle.givens),
                        difficulty = requestedDifficulty
                    )
                }
                persist()
                statsRepository?.recordGameStarted(requestedDifficulty)
            }
            startTimer()
        }
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.settings.collect { s ->
                    autoRemoveNotes = s.autoRemoveNotes
                    _state.update {
                        it.copy(
                            inputMode = s.inputMode,
                            mistakeChecking = s.mistakeChecking,
                            showTimer = s.showTimer,
                            mistakeLimit = if (s.mistakeLimitEnabled) MISTAKE_LIMIT else null,
                            armedDigit = if (s.inputMode == InputMode.DIGIT_FIRST) it.armedDigit else null
                        )
                    }
                }
            }
        }
    }

    /** Restart the same puzzle after running out of mistakes. */
    fun onRetry() {
        undoStack.clear()
        redoStack.clear()
        hintEscalation.reset()
        resultRecorded = false
        _state.update {
            it.copy(
                board = Board.fromGivens(givens),
                selectedCell = null,
                armedDigit = null,
                elapsedMillis = 0,
                mistakes = 0,
                hintsUsed = 0,
                isFailed = false,
                errorCells = emptySet(),
                canUndo = false,
                canRedo = false,
                hint = null
            )
        }
        persist()
    }

    private fun restore(saved: SavedGame) {
        givens = saved.givens
        solutionString = saved.solution
        solution = IntArray(81) { saved.solution[it] - '0' }
        undoStack.clear()
        redoStack.clear()
        saved.undoStack.forEach { undoStack.addLast(boardFrom(saved.givens, it)) }
        saved.redoStack.forEach { redoStack.addLast(boardFrom(saved.givens, it)) }
        val board = saved.toBoard()
        val current = board.values()
        val plan = saved.hintTargetCell.takeIf { it in 0 until 81 && current[it] != solution[it] }
            ?.let { HintEngine.planFor(current, solution, it) }
        hintEscalation.restore(plan, saved.hintStepIndex, saved.hintCharged, saved.mistakeHintLevel)
        _state.update {
            GameUiState(
                loading = false,
                board = board,
                difficulty = saved.difficulty,
                elapsedMillis = saved.elapsedMillis,
                mistakes = saved.mistakes,
                hintsUsed = saved.hintsUsed,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                errorCells = (0 until 81).filter { i ->
                    val v = board[i].value
                    v != 0 && !board[i].isGiven && v != solution[i]
                }.toSet(),
                hint = deriveHint(board)
            )
        }
    }

    /** Called from the UI on lifecycle ON_START / ON_STOP. */
    fun onForeground() {
        inForeground = true
    }

    fun onBackground() {
        inForeground = false
        persist()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { s ->
                    if (inForeground && !s.paused && !s.isWon && !s.loading) {
                        s.copy(elapsedMillis = s.elapsedMillis + 1000)
                    } else s
                }
            }
        }
    }

    private fun persist() {
        val s = _state.value
        if (s.loading || s.isWon) return
        val game = SavedGame(
            givens = givens,
            solution = solutionString,
            current = s.board.cells.joinToString("") { it.value.toString() },
            notes = s.board.cells.map { it.notes },
            difficulty = s.difficulty,
            elapsedMillis = s.elapsedMillis,
            mistakes = s.mistakes,
            hintsUsed = s.hintsUsed,
            undoStack = undoStack.map { it.toSnapshot() },
            redoStack = redoStack.map { it.toSnapshot() },
            hintTargetCell = hintEscalation.plan?.cellIndex ?: -1,
            hintStepIndex = hintEscalation.stepIndex,
            hintCharged = hintEscalation.charged,
            mistakeHintLevel = hintEscalation.mistakeLevel
        )
        viewModelScope.launch { repository.saveGame(game) }
    }

    fun onCellTap(index: Int) {
        val s = _state.value
        if (s.loading || s.paused || s.isWon || s.isFailed) return
        if (s.inputMode == InputMode.DIGIT_FIRST && s.armedDigit != null) {
            applyDigit(index, s.armedDigit)
        } else {
            _state.update { it.copy(selectedCell = index) }
        }
    }

    fun onDigit(digit: Int) {
        val s = _state.value
        if (s.loading || s.paused || s.isWon || s.isFailed) return
        if (s.inputMode == InputMode.DIGIT_FIRST) {
            _state.update {
                it.copy(armedDigit = if (it.armedDigit == digit) null else digit)
            }
        } else {
            s.selectedCell?.let { applyDigit(it, digit) }
        }
    }

    fun onErase() {
        val s = _state.value
        if (s.loading || s.paused || s.isWon || s.isFailed) return
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
        // Keep the hint card: the escalation state behind it persists, and the player may
        // have paid for the tier being shown.
        _state.update { s -> if (s.isWon) s else s.copy(paused = !s.paused) }
    }

    /** The card is a projection of escalation state; nothing else writes GameUiState.hint except notices. */
    private fun deriveHint(board: Board): HintUi? {
        if (hintEscalation.mistakeLevel > 0) {
            val wrong = HintEngine.wrongCells(board.values(), solution)
            if (wrong.isNotEmpty()) return when (hintEscalation.mistakeLevel) {
                1 -> HintUi(
                    if (wrong.size == 1) "There is 1 incorrect cell." else "There are ${wrong.size} incorrect cells."
                )
                else -> HintUi(
                    if (wrong.size == 1) "This cell is incorrect." else "These cells are incorrect.",
                    HintHighlight(primary = wrong)
                )
            }
        }
        val plan = hintEscalation.plan ?: return null
        val step = hintEscalation.currentStep ?: return null
        return HintUi(
            step.text,
            step.highlight,
            step.techniques,
            progress = hintEscalation.stepIndex + 1 to plan.steps.size
        )
    }

    fun onHint() {
        val s = _state.value
        if (s.loading || s.paused || s.isWon || s.isFailed) return
        val current = s.board.values()
        val outcome =
            hintEscalation.onHintPressed(current, solution, s.mistakeChecking, s.selectedCell)
        when (outcome) {
            is HintEscalation.Outcome.FixFirstWrong -> placeHint(
                index = outcome.cellIndex,
                digit = outcome.correct,
                charged = true,
                message = "That cell was wrong — corrected to ${outcome.correct}."
            )
            is HintEscalation.Outcome.Step -> {
                _state.update {
                    it.copy(
                        hintsUsed = it.hintsUsed + if (outcome.charged) 1 else 0,
                        // The last step names the digit but never enters it: selecting the target
                        // puts the player one tap away, and the selection outline sits on top of
                        // the hint tint so both still read.
                        selectedCell = if (outcome.index == outcome.plan.steps.lastIndex) {
                            outcome.plan.cellIndex
                        } else {
                            it.selectedCell
                        },
                        hint = deriveHint(it.board)
                    )
                }
                persist()
            }
            is HintEscalation.Outcome.ShowMistakeCount -> {
                _state.update { it.copy(hint = deriveHint(it.board)) }
                persist()
            }
            is HintEscalation.Outcome.HighlightMistakes -> {
                _state.update {
                    it.copy(
                        hintsUsed = it.hintsUsed + if (outcome.charged) 1 else 0,
                        hint = deriveHint(it.board)
                    )
                }
                persist()
            }
            is HintEscalation.Outcome.FixMistakes -> {
                pushUndo(s.board)
                var board = s.board
                for ((index, correct) in outcome.corrections) {
                    board = board.with(index, board[index].copy(value = correct, notes = 0))
                    if (autoRemoveNotes) board = clearPeerNotes(board, index, correct)
                }
                val cells = outcome.corrections.map { it.first }
                _state.update {
                    it.copy(
                        selectedCell = cells.first(),
                        hintsUsed = it.hintsUsed + if (outcome.charged) 1 else 0
                    )
                }
                updateBoard(
                    board,
                    notice = HintUi(
                        if (cells.size == 1) "Corrected 1 cell." else "Corrected ${cells.size} cells.",
                        HintHighlight(primary = cells)
                    )
                )
            }
            HintEscalation.Outcome.None -> Unit
        }
    }

    private fun placeHint(index: Int, digit: Int, charged: Boolean, message: String) {
        val s = _state.value
        pushUndo(s.board)
        val placed = s.board.with(index, s.board[index].copy(value = digit, notes = 0))
        _state.update {
            it.copy(
                selectedCell = index,
                hintsUsed = it.hintsUsed + if (charged) 1 else 0
            )
        }
        updateBoard(
            clearPeerNotes(placed, index, digit),
            notice = HintUi(message, HintHighlight(primary = listOf(index)))
        )
    }

    /** Pauses the game before opening the technique explainer, keeping the hint card intact. */
    fun onOpenExplainer() {
        _state.update { it.copy(paused = true) }
    }

    private fun applyDigit(index: Int, digit: Int) {
        val s = _state.value
        val cell = s.board[index]
        if (cell.isGiven) {
            _state.update { it.copy(selectedCell = index) }
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
            val placed = s.board.with(index, cell.copy(value = digit, notes = 0))
            newBoard = if (autoRemoveNotes) clearPeerNotes(placed, index, digit) else placed
            mistake = s.mistakeChecking && solution[index] != digit
        }
        _state.update { it.copy(selectedCell = index) }
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

    private fun updateBoard(board: Board, countMistake: Boolean = false, notice: HintUi? = null) {
        hintEscalation.onBoardChanged(board.values(), solution)
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
            val newMistakes = s.mistakes + if (countMistake) 1 else 0
            val failed = s.isFailed ||
                (!won && s.mistakeLimit != null && newMistakes >= s.mistakeLimit)
            s.copy(
                board = board,
                mistakes = newMistakes,
                errorCells = errors,
                isWon = won,
                isFailed = failed,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                armedDigit = if (won) null else s.armedDigit,
                selectedCell = if (won) null else s.selectedCell,
                hint = notice ?: deriveHint(board)
            )
        }
        val after = _state.value
        when {
            won -> {
                if (!resultRecorded) {
                    resultRecorded = true
                    viewModelScope.launch {
                        repository.clearSavedGame()
                        val newBest =
                            statsRepository?.recordWin(after.difficulty, after.elapsedMillis)
                                ?: false
                        _state.update { it.copy(isNewBest = newBest) }
                    }
                }
            }
            after.isFailed -> {
                if (!resultRecorded) {
                    resultRecorded = true
                    viewModelScope.launch { statsRepository?.recordLoss(after.difficulty) }
                }
                persist()
            }
            else -> persist()
        }
    }

    private companion object {
        const val MAX_UNDO = 100
        const val MISTAKE_LIMIT = 3
        const val KEY_STARTED = "game_started"
    }
}
