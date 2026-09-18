package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEngineTest {

    private val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
    private val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)

    private fun wrongDigitFor(index: Int): Int = solution[index] % 9 + 1

    @Test
    fun wrongCellsReportedInReadingOrder() {
        val current = puzzle.copyOf()
        val empties = (0 until 81).filter { current[it] == 0 }
        val first = empties[0]
        val second = empties[5]
        current[second] = wrongDigitFor(second)
        current[first] = wrongDigitFor(first)
        assertEquals(listOf(first, second), HintEngine.wrongCells(current, solution))
    }

    @Test
    fun noWrongCellsOnCleanBoard() {
        assertTrue(HintEngine.wrongCells(puzzle, solution).isEmpty())
    }

    @Test
    fun planMatchesSolution() {
        val plan = HintEngine.plan(puzzle, solution)
        assertNotNull(plan)
        assertFalse(plan!!.isReveal)
        assertEquals(solution[plan.cellIndex], plan.digit)
        assertTrue(plan.techniques.isNotEmpty())
        assertTrue(plan.steps.size >= 2)
        assertEquals(HintHighlight(), plan.steps.first().highlight)
        assertEquals(plan.techniques, plan.steps.first().techniques)
        assertEquals(listOf(plan.cellIndex), plan.steps.last().highlight.primary)
        assertEquals(plan.digit, plan.steps.last().highlight.digit)
    }

    @Test
    fun planNullOnFullBoard() {
        assertNull(HintEngine.plan(solution, solution))
    }

    @Test
    fun revealFallbackPrefersSelectedCell() {
        // A near-empty board (2 clues) stalls the ladder immediately
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        val plan = HintEngine.plan(sparse, solution, selected = 40)
        assertNotNull(plan)
        assertTrue(plan!!.isReveal)
        assertEquals(40, plan.cellIndex)
        assertEquals(solution[40], plan.digit)
        assertTrue(plan.techniques.isEmpty())
        assertEquals(2, plan.steps.size)
    }

    @Test
    fun revealFallbackUsesFirstEmptyWithoutSelection() {
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        val plan = HintEngine.plan(sparse, solution)
        assertNotNull(plan)
        assertTrue(plan!!.isReveal)
        assertEquals(2, plan.cellIndex)
        assertEquals(solution[2], plan.digit)
        assertTrue(plan.techniques.isEmpty())
        assertEquals(2, plan.steps.size)
    }

    @Test
    fun planForReproducesPlan() {
        val plan = HintEngine.plan(puzzle, solution)!!
        assertEquals(plan, HintEngine.planFor(puzzle, solution, plan.cellIndex))
    }

    @Test
    fun planForIgnoresWrongDigitInTarget() {
        val plan = HintEngine.plan(puzzle, solution)!!
        val dirty = puzzle.copyOf()
        dirty[plan.cellIndex] = wrongDigitFor(plan.cellIndex)
        assertEquals(plan, HintEngine.planFor(dirty, solution, plan.cellIndex))
    }
}
