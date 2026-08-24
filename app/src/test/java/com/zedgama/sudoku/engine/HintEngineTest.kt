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
    fun pinTargetMatchesSolution() {
        val pin = HintEngine.pinTarget(puzzle, solution)
        assertNotNull(pin)
        assertFalse(pin!!.isReveal)
        assertEquals(solution[pin.cellIndex], pin.digit)
        assertTrue(pin.techniques.isNotEmpty())
    }

    @Test
    fun pinTargetNullOnFullBoard() {
        assertNull(HintEngine.pinTarget(solution, solution))
    }

    @Test
    fun revealFallbackPrefersSelectedCell() {
        // A near-empty board (2 clues) stalls the ladder immediately
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        val pin = HintEngine.pinTarget(sparse, solution, selected = 40)
        assertNotNull(pin)
        assertTrue(pin!!.isReveal)
        assertEquals(40, pin.cellIndex)
        assertEquals(solution[40], pin.digit)
        assertTrue(pin.techniques.isEmpty())
    }

    @Test
    fun revealFallbackUsesFirstEmptyWithoutSelection() {
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        val pin = HintEngine.pinTarget(sparse, solution)
        assertNotNull(pin)
        assertTrue(pin!!.isReveal)
        assertEquals(2, pin.cellIndex)
        assertEquals(solution[2], pin.digit)
    }
}
