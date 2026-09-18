package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun easiestChainMatchesSolution() {
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val chain = LogicalSolver.easiestChain(puzzle)
        assertNotNull(chain)
        assertTrue(chain!!.placement.value != 0)
        assertEquals(solution[chain.placement.cellIndex], chain.placement.value)
        assertTrue(chain.eliminations.all { it.value == 0 })
        assertEquals(chain.techniques.distinct(), chain.techniques)
        assertEquals(chain.placement.technique, chain.techniques.last())
    }

    @Test
    fun easiestChainPrefersNakedSingleWithMostFilledPeers() {
        // Cells 0 and 1 are peers, so each sees 19 filled peers; cell 80 sees all 20.
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val puzzle = solution.copyOf()
        puzzle[0] = 0
        puzzle[1] = 0
        puzzle[80] = 0
        val chain = LogicalSolver.easiestChain(puzzle)!!
        assertEquals(80, chain.placement.cellIndex)
        assertEquals(solution[80], chain.placement.value)
    }

    @Test
    fun easiestChainBreaksTiesByCellIndex() {
        // 0 and 40 are not peers, so both see 20 filled peers: the lower index wins.
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val puzzle = solution.copyOf()
        puzzle[0] = 0
        puzzle[40] = 0
        val chain = LogicalSolver.easiestChain(puzzle)!!
        assertEquals(0, chain.placement.cellIndex)
        assertEquals(solution[0], chain.placement.value)
    }

    @Test
    fun easiestChainAgreesWithComparatorOnRealPuzzle() {
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
        val chain = LogicalSolver.easiestChain(puzzle)!!
        // Re-apply the chain's eliminations to reach the grid the placement was picked from.
        val grid = CandidateGrid(puzzle)!!
        repeat(chain.eliminations.size) { LogicalSolver.nextStep(grid) }
        val expected = LogicalSolver.enumerateSingles(grid).minWith(
            compareBy(
                { if (it.technique == Technique.NAKED_SINGLE) 0 else 1 },
                {
                    if (it.technique == Technique.NAKED_SINGLE)
                        -peersOf(it.cellIndex).count { p -> grid.values[p] != 0 }
                    else
                        LogicalSolver.houseCells(it.house).count { c -> grid.values[c] == 0 }
                },
                { it.cellIndex },
                { it.value }
            )
        )
        assertEquals(expected.cellIndex, chain.placement.cellIndex)
        assertEquals(expected.value, chain.placement.value)
        assertEquals(expected.technique, chain.placement.technique)
    }

    @Test
    fun chainForReachesRequestedTarget() {
        val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
        val target = LogicalSolver.easiestChain(puzzle)!!.placement.cellIndex
        val chain = LogicalSolver.chainFor(puzzle, target)
        assertNotNull(chain)
        assertEquals(target, chain!!.placement.cellIndex)
    }

    @Test
    fun chainForReturnsNullWhenTargetNeverBecomesSingle() {
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        assertNull(LogicalSolver.chainFor(sparse, 40))
    }

    @Test
    fun easiestChainStallsOnSparseBoard() {
        val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        assertEquals(null, LogicalSolver.easiestChain(sparse))
    }

    @Test
    fun eliminationStepsCarryHouseAndEliminatedCells() {
        // KNOWN_PUZZLE solves on singles alone, so these two boards (generated, unique-solution)
        // stand in for the elimination ladder: between them they exercise all three techniques.
        val boards = listOf(
            "000004610050206000020001070200000403006000000070020000000003020904700001060109000",
            "048312000500000000020004060000000003004060800300200070200000600030100902000500004"
        )
        val seen = mutableSetOf<Technique>()
        for (givens in boards) {
            val grid = CandidateGrid(TestPuzzles.parse(givens))!!
            while (!grid.isFull) {
                val step = LogicalSolver.nextStep(grid) ?: break
                if (step.value != 0) continue
                seen.add(step.technique)
                assertTrue("house set for ${step.technique}", step.house != -1)
                assertTrue("eliminationHouse set for ${step.technique}", step.eliminationHouse != -1)
                assertTrue("eliminated cells for ${step.technique}", step.eliminatedCells.isNotEmpty())
                assertTrue("digits for ${step.technique}", step.digits.isNotEmpty())
                if (step.technique != Technique.HIDDEN_PAIR) {
                    // A hidden pair clears extra candidates from the pattern cells themselves;
                    // the other two always eliminate outside the pattern.
                    assertTrue(
                        "eliminated disjoint from pattern for ${step.technique}",
                        step.eliminatedCells.none { it in step.involvedCells }
                    )
                }
            }
        }
        assertEquals(
            setOf(
                Technique.LOCKED_CANDIDATES,
                Technique.NAKED_PAIR,
                Technique.HIDDEN_PAIR
            ),
            seen
        )
    }

}
