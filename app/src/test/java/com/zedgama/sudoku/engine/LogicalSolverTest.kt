package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicalSolverTest {

    @Test
    fun solvesEasyPuzzleWithSingles() {
        val result = LogicalSolver.solve(TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE))
        assertTrue(result.solved)
        assertTrue(result.techniques.isNotEmpty())
    }

    @Test
    fun nakedSingleDetected() {
        // Fill row 0 except one cell: the empty cell is a naked single
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val puzzle = solution.copyOf()
        puzzle[4] = 0
        val grid = CandidateGrid(puzzle)!!
        val step = LogicalSolver.nextStep(grid)
        assertNotNull(step)
        assertEquals(Technique.NAKED_SINGLE, step!!.technique)
        assertEquals(4, step.cellIndex)
        assertEquals(solution[4], step.value)
    }

    @Test
    fun firstPlacementMatchesSolution() {
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val step = LogicalSolver.firstPlacement(puzzle)
        assertNotNull(step)
        assertEquals(solution[step!!.cellIndex], step.value)
    }

    @Test
    fun fullLogicalSolveMatchesBacktrackingSolver() {
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
        val grid = CandidateGrid(puzzle)!!
        while (!grid.isFull) {
            val step = LogicalSolver.nextStep(grid) ?: break
            if (step.value != 0) {
                assertEquals(
                    "placement at ${step.cellIndex}",
                    TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)[step.cellIndex],
                    step.value
                )
            }
        }
        assertTrue(grid.isFull)
    }

    @Test
    fun conflictingPuzzleYieldsNullGrid()  {
        val puzzle = IntArray(81)
        puzzle[0] = 3
        puzzle[8] = 3
        assertEquals(null, CandidateGrid(puzzle))
    }
}
