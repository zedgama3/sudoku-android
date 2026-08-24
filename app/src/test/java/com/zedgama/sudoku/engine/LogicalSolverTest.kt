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

    @Test
    fun placementChainMatchesSolution() {
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val chain = LogicalSolver.placementChain(puzzle)
        assertNotNull(chain)
        assertTrue(chain!!.placement.value != 0)
        assertEquals(solution[chain.placement.cellIndex], chain.placement.value)
        assertTrue(chain.eliminations.all { it.value == 0 })
        assertEquals(chain.techniques.distinct(), chain.techniques)
        assertEquals(chain.placement.technique, chain.techniques.last())
    }

    @Test
    fun placementChainPicksLowestDigit() {
        // Two naked singles at once: digit 5 (cell 0) and digit 7 (cell 4)
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val puzzle = solution.copyOf()
        puzzle[0] = 0 // solution 5
        puzzle[4] = 0 // solution 7
        val chain = LogicalSolver.placementChain(puzzle)!!
        assertEquals(0, chain.placement.cellIndex)
        assertEquals(5, chain.placement.value)
    }

    @Test
    fun placementChainBreaksDigitTiesByCellIndex() {
        // Two naked singles with the same digit 7, at cells 4 and 10
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val puzzle = solution.copyOf()
        puzzle[4] = 0  // solution 7
        puzzle[10] = 0 // solution 7
        val chain = LogicalSolver.placementChain(puzzle)!!
        assertEquals(4, chain.placement.cellIndex)
        assertEquals(7, chain.placement.value)
    }

    @Test
    fun placementChainStallsOnSparseBoard() {
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        assertEquals(null, LogicalSolver.placementChain(sparse))
    }

    @Test
    fun enumerateSinglesDoesNotMutateGrid() {
        val grid = CandidateGrid(TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE))!!
        val valuesBefore = grid.values.copyOf()
        val candsBefore = grid.cands.copyOf()
        val singles = LogicalSolver.enumerateSingles(grid)
        assertTrue(singles.isNotEmpty())
        assertTrue(grid.values.contentEquals(valuesBefore))
        assertTrue(grid.cands.contentEquals(candsBefore))
        // Deduped by (cell, digit)
        val keys = singles.map { it.cellIndex * 10 + it.value }
        assertEquals(keys.distinct(), keys)
    }
}
