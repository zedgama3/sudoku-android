package com.zedgama.sudoku.data.model

import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.Cell
import com.zedgama.sudoku.engine.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class BoardSnapshot(
    val values: String,        // 81 chars
    val notes: List<Int>       // 81 bitmasks
)

@Serializable
data class SavedGame(
    val givens: String,        // 81 chars, '0' = empty
    val solution: String,      // 81 chars
    val current: String,       // 81 chars, current values incl. player entries
    val notes: List<Int>,      // 81 bitmasks
    val difficulty: Difficulty,
    val elapsedMillis: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val undoStack: List<BoardSnapshot> = emptyList(),
    val redoStack: List<BoardSnapshot> = emptyList()
)

fun Board.toSnapshot(): BoardSnapshot = BoardSnapshot(
    values = cells.joinToString("") { it.value.toString() },
    notes = cells.map { it.notes }
)

fun boardFrom(givens: String, snapshot: BoardSnapshot): Board = Board(
    List(81) { i ->
        Cell(
            value = snapshot.values[i] - '0',
            isGiven = givens[i] != '0',
            notes = snapshot.notes[i]
        )
    }
)

fun SavedGame.toBoard(): Board = boardFrom(givens, BoardSnapshot(current, notes))
