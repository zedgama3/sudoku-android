package com.zedgama.sudoku.engine

sealed class Hint {
    /** The player has a wrong value on the board; point at it. */
    data class WrongCell(val index: Int, val correct: Int) : Hint()

    /** A teachable logical deduction placing [LogicalStep.value] at [LogicalStep.cellIndex]. */
    data class Logical(val step: LogicalStep) : Hint()

    /** No human-ladder step available; just reveal a cell. */
    data class Reveal(val index: Int, val value: Int) : Hint()
}

object HintEngine {

    /**
     * @param current  current board values (0 = empty), including player entries
     * @param solution the puzzle's solution
     * @param selected the cell the player has selected, preferred for reveals
     */
    fun hint(current: IntArray, solution: IntArray, selected: Int? = null): Hint? {
        // 1. Fix errors first
        for (i in 0 until 81) {
            if (current[i] != 0 && current[i] != solution[i]) {
                return Hint.WrongCell(i, solution[i])
            }
        }
        if (current.all { it != 0 }) return null

        // 2. Teach a logical step
        val step = LogicalSolver.firstPlacement(current)
        if (step != null) return Hint.Logical(step)

        // 3. Fall back to revealing a cell
        val index = if (selected != null && current[selected] == 0) {
            selected
        } else {
            (0 until 81).first { current[it] == 0 }
        }
        return Hint.Reveal(index, solution[index])
    }
}
