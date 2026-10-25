package net.zg3.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GeneratorTest {

    @Test
    fun generatedPuzzlesAreUniqueAndConsistent() {
        val generator = Generator(Random(1234))
        for (difficulty in Difficulty.entries) {
            repeat(5) {
                val p = generator.generate(difficulty)
                val puzzle = TestPuzzles.parse(p.givens)
                val solution = TestPuzzles.parse(p.solution)

                assertEquals("unique solution", 1, Solver.countSolutions(puzzle))
                // Givens must be a subset of the solution
                for (i in 0 until 81) {
                    if (puzzle[i] != 0) assertEquals(solution[i], puzzle[i])
                }
                // Solution must actually solve the puzzle
                val solved = Solver.solve(puzzle)!!
                assertTrue(solved.contentEquals(solution))
            }
        }
    }

    @Test
    fun easyPuzzlesAreSinglesSolvableWithManyClues() {
        val generator = Generator(Random(99))
        repeat(3) {
            val p = generator.generate(Difficulty.EASY)
            val puzzle = TestPuzzles.parse(p.givens)
            assertTrue("clue count", puzzle.count { it != 0 } >= 36)
            val result = LogicalSolver.solve(puzzle)
            assertTrue("solvable with singles", result.solved)
            assertTrue(result.techniques.all { it.rank <= 1 })
        }
    }

    @Test
    fun generationIsReasonablyFast() {
        val generator = Generator(Random(7))
        val start = System.nanoTime()
        for (difficulty in Difficulty.entries) {
            generator.generate(difficulty)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("4 puzzles in ${elapsedMs}ms", elapsedMs < 20_000)
    }
}
