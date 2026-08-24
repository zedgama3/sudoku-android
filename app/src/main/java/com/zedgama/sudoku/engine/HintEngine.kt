package com.zedgama.sudoku.engine

/**
 * The placement a hint escalation sequence works toward. Pinned when the sequence starts so
 * every tier (technique list, digit, placement) refers to the same move.
 */
data class PinnedHint(
    val cellIndex: Int,
    val digit: Int,
    val techniques: List<Technique>,   // empty when [isReveal]
    val isReveal: Boolean = false      // the ladder stalled; the flow degrades to a reveal
)

object HintEngine {

    /** Player entries that differ from the solution, in reading order. */
    fun wrongCells(current: IntArray, solution: IntArray): List<Int> =
        (0 until 81).filter { current[it] != 0 && current[it] != solution[it] }

    /**
     * Chooses the placement an escalation sequence works toward.
     * Returns null only when the board has no empty cells.
     *
     * @param current  current board values (0 = empty), including player entries
     * @param solution the puzzle's solution
     * @param selected the cell the player has selected, preferred for reveals
     */
    fun pinTarget(current: IntArray, solution: IntArray, selected: Int? = null): PinnedHint? {
        if (current.all { it != 0 }) return null
        val chain = LogicalSolver.placementChain(current)
        if (chain != null && chain.placement.value == solution[chain.placement.cellIndex]) {
            return PinnedHint(chain.placement.cellIndex, chain.placement.value, chain.techniques)
        }
        // Ladder stalled (or disagreed with the solution): fall back to revealing a cell.
        val index = if (selected != null && current[selected] == 0) {
            selected
        } else {
            (0 until 81).first { current[it] == 0 }
        }
        return PinnedHint(index, solution[index], emptyList(), isReveal = true)
    }
}
