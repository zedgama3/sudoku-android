package com.zedgama.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SolverTest {

    @Test
    fun solvesKnownPuzzle() {
        val solution = Solver.solve(TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE))
        assertNotNull(solution)
        assertArrayEquals(TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION), solution)
    }

    @Test
    fun knownPuzzleIsUnique() {
        assertEquals(1, Solver.countSolutions(TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)))
    }

    @Test
    fun conflictingGivensReturnNull() {
        val puzzle = IntArray(81)
        puzzle[0] = 5
        puzzle[1] = 5 // same row
        assertNull(Solver.solve(puzzle))
        assertEquals(0, Solver.countSolutions(puzzle))
    }

    @Test
    fun emptyGridSolvesAndHasManySolutions() {
        assertNotNull(Solver.solve(IntArray(81)))
        assertEquals(2, Solver.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun multiSolutionGridDetected() {
        // Remove all 1s and 2s from a full solution: their positions can be swapped,
        // so at least two solutions exist.
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        for (i in 0 until 81) if (puzzle[i] == 1 || puzzle[i] == 2) puzzle[i] = 0
        assertEquals(2, Solver.countSolutions(puzzle, limit = 2))
    }

    @Test
    fun randomFullGridIsValidAndComplete() {
        val grid = Solver.randomFullGrid(Random(42))
        assertTrue(grid.all { it in 1..9 })
        // A full valid grid is its own unique solution
        assertEquals(1, Solver.countSolutions(grid.copyOf()))
        // Verify no duplicates in any house
        for (i in 0 until 81) {
            for (j in i + 1 until 81) {
                if (rowOf(i) == rowOf(j) || colOf(i) == colOf(j) || boxOf(i) == boxOf(j)) {
                    assertTrue("cells $i and $j duplicate", grid[i] != grid[j])
                }
            }
        }
    }

    @Test
    fun randomGridsDifferAcrossSeeds() {
        val a = Solver.randomFullGrid(Random(1))
        val b = Solver.randomFullGrid(Random(2))
        assertTrue(!a.contentEquals(b))
    }
}
