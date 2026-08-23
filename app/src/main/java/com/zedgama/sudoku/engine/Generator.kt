package com.zedgama.sudoku.engine

import kotlin.random.Random

data class GeneratedPuzzle(
    val givens: String,       // 81 chars, '0' = empty
    val solution: String,     // 81 chars
    val difficulty: Difficulty
)

/**
 * Generates unique-solution puzzles: fill a random complete grid, dig clues out while
 * preserving uniqueness, then grade; retry until the grade matches the request.
 */
class Generator(private val random: Random = Random.Default) {

    fun generate(difficulty: Difficulty): GeneratedPuzzle {
        var fallback: GeneratedPuzzle? = null
        repeat(MAX_ATTEMPTS) {
            val solution = Solver.randomFullGrid(random)
            val puzzle = dig(solution, difficulty)
            val grade = Grader.grade(puzzle)
            val result = GeneratedPuzzle(puzzle.toBoardString(), solution.toBoardString(), grade)
            if (grade == difficulty) return result
            // Keep the closest miss as a fallback
            if (fallback == null ||
                distance(grade, difficulty) < distance(fallback!!.difficulty, difficulty)
            ) {
                fallback = result.copy(difficulty = difficulty)
            }
        }
        return fallback!!
    }

    private fun distance(a: Difficulty, b: Difficulty): Int =
        kotlin.math.abs(a.ordinal - b.ordinal)

    /** Remove clues while the puzzle stays uniquely solvable, down to a per-difficulty floor. */
    private fun dig(solution: IntArray, difficulty: Difficulty): IntArray {
        val puzzle = solution.copyOf()
        var clues = 81
        val floor = clueFloor(difficulty)
        val symmetric = difficulty == Difficulty.EASY || difficulty == Difficulty.MEDIUM

        if (symmetric) {
            val pairs = (0..40).shuffled(random)
            for (p in pairs) {
                if (clues <= floor) break
                val q = 80 - p
                val savedP = puzzle[p]
                val savedQ = puzzle[q]
                if (savedP == 0) continue
                puzzle[p] = 0
                var removed = 1
                if (q != p && savedQ != 0) {
                    puzzle[q] = 0
                    removed++
                }
                if (Solver.countSolutions(puzzle, limit = 2) != 1) {
                    puzzle[p] = savedP
                    if (q != p) puzzle[q] = savedQ
                } else {
                    clues -= removed
                }
            }
        } else {
            val positions = (0..80).shuffled(random)
            for (p in positions) {
                if (clues <= floor) break
                val saved = puzzle[p]
                if (saved == 0) continue
                puzzle[p] = 0
                if (Solver.countSolutions(puzzle, limit = 2) != 1) {
                    puzzle[p] = saved
                } else {
                    clues--
                }
            }
        }
        return puzzle
    }

    private fun clueFloor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 40
        Difficulty.MEDIUM -> 32
        Difficulty.HARD -> 25
        Difficulty.EXPERT -> 17
    }

    private companion object {
        const val MAX_ATTEMPTS = 300
    }
}
