package com.zedgama.sudoku.engine

object Grader {

    /**
     * Grades by the hardest human technique required:
     * singles only -> EASY (with plenty of clues) or MEDIUM,
     * locked candidates -> MEDIUM, pairs -> HARD, ladder stalls -> EXPERT.
     */
    fun grade(puzzle: IntArray): Difficulty {
        val result = LogicalSolver.solve(puzzle)
        if (!result.solved) return Difficulty.EXPERT
        val maxRank = result.techniques.maxOfOrNull { it.rank } ?: 1
        val clues = puzzle.count { it != 0 }
        return when {
            maxRank <= 1 -> if (clues >= 36) Difficulty.EASY else Difficulty.MEDIUM
            maxRank == 2 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
    }
}
