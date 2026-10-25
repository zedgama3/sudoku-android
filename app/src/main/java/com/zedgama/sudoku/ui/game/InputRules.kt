package com.zedgama.sudoku.ui.game

import com.zedgama.sudoku.data.model.AutoSelect
import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.Cell
import com.zedgama.sudoku.engine.colOf
import com.zedgama.sudoku.engine.rowOf
import kotlin.math.abs
import kotlin.math.max

/**
 * The cell after putting [digit] into [cell], or null when the gesture leaves it untouched (the
 * tap then only selects). Entering never clears a value: values leave only via Erase or Undo.
 */
fun resolveEntry(
    cell: Cell,
    digit: Int,
    asMark: Boolean,
    allowOverwrite: Boolean,
    tapRemovesMark: Boolean
): Cell? {
    if (cell.isGiven) return null
    if (asMark) {
        if (cell.value != 0) return null
        val bit = 1 shl (digit - 1)
        return when {
            cell.notes and bit == 0 -> cell.copy(notes = cell.notes or bit)
            tapRemovesMark -> cell.copy(notes = cell.notes and bit.inv())
            else -> null
        }
    }
    if (cell.value == digit) return null
    if (cell.value != 0 && !allowOverwrite) return null
    return cell.copy(value = digit, notes = 0)
}

/**
 * The cell to select after arming [digit], or null to leave the selection alone. Marks don't
 * count as filled. Picks the match nearest [selected] (king-move distance, ties in reading
 * order), or the first match when nothing is selected.
 */
fun pickAutoSelectCell(board: Board, selected: Int?, digit: Int, mode: AutoSelect): Int? {
    if (mode == AutoSelect.NEVER) return null
    val current = selected?.let { board[it].value } ?: 0
    if (current == digit) return null
    if (mode == AutoSelect.WHEN_FILLED && current == 0) return null
    val matches = (0 until 81).filter { board[it].value == digit }
    if (selected == null) return matches.firstOrNull()
    return matches.minByOrNull {
        max(abs(rowOf(it) - rowOf(selected)), abs(colOf(it) - colOf(selected)))
    }
}
