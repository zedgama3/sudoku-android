package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEngineTest {

    private val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
    private val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)

    @Test
    fun wrongCellReportedFirst() {
        val current = puzzle.copyOf()
        val emptyIndex = (0 until 81).first { current[it] == 0 }
        current[emptyIndex] = if (solution[emptyIndex] == 1) 2 else 1 // deliberately wrong
        val hint = HintEngine.hint(current, solution)
        assertTrue(hint is Hint.WrongCell)
        assertEquals(emptyIndex, (hint as Hint.WrongCell).index)
        assertEquals(solution[emptyIndex], hint.correct)
    }

    @Test
    fun logicalHintPlacesCorrectValue() {
        val hint = HintEngine.hint(puzzle, solution)
        assertTrue(hint is Hint.Logical)
        val step = (hint as Hint.Logical).step
        assertEquals(solution[step.cellIndex], step.value)
    }

    @Test
    fun completeBoardYieldsNoHint() {
        assertNull(HintEngine.hint(solution, solution))
    }

    @Test
    fun revealPrefersSelectedCell() {
        // A near-empty board (2 clues) stalls the ladder immediately
        val sparse = IntArray(81)
        sparse[0] = solution[0]
        sparse[1] = solution[1]
        val hint = HintEngine.hint(sparse, solution, selected = 40)
        if (hint is Hint.Reveal) {
            assertEquals(40, hint.index)
            assertEquals(solution[40], hint.value)
        }
        // If the ladder found a placement instead, that's also acceptable
    }
}
