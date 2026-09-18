package com.zedgama.sudoku.engine

object HintEngine {

    /** Player entries that differ from the solution, in reading order. */
    fun wrongCells(current: IntArray, solution: IntArray): List<Int> =
        (0 until 81).filter { current[it] != 0 && current[it] != solution[it] }

    /**
     * Builds the walkthrough for the easiest target on the board, or null when the board is full.
     *
     * @param current  current board values (0 = empty), including player entries
     * @param solution the puzzle's solution
     * @param selected the cell the player has selected, preferred for reveals
     */
    fun plan(current: IntArray, solution: IntArray, selected: Int? = null): HintPlan? {
        if (current.all { it != 0 }) return null
        val chain = LogicalSolver.easiestChain(current)
        if (chain != null && chain.placement.value == solution[chain.placement.cellIndex]) {
            return HintPlanner.build(chain, current)
        }
        val index = selected?.takeIf { current[it] == 0 } ?: (0 until 81).first { current[it] == 0 }
        return HintPlanner.reveal(index, solution[index])
    }

    /**
     * Plan for a fixed target (rebuild after a board change, or restore). Computed with the target
     * cleared so a wrong entry sitting in it does not poison the reasoning. Always returns a plan:
     * the digit comes from the solution, so the reveal ladder is the floor.
     */
    fun planFor(current: IntArray, solution: IntArray, target: Int): HintPlan {
        val board = current.copyOf().also { it[target] = 0 }
        val chain = LogicalSolver.chainFor(board, target)
        return if (chain != null && chain.placement.value == solution[target]) {
            HintPlanner.build(chain, board)
        } else {
            HintPlanner.reveal(target, solution[target])
        }
    }
}
