package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEscalationTest {

    private val puzzle = TestPuzzles.parse(TestPuzzles.KNOWN_PUZZLE)
    private val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)

    private fun wrongDigitFor(index: Int): Int = solution[index] % 9 + 1

    @Test
    fun normalTrackChargesOnlySecondAndThirdPress() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()

        val o1 = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o1 is HintEscalation.Outcome.ShowTechniques)
        assertFalse(o1.charged)

        val o2 = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o2 is HintEscalation.Outcome.ShowDigit)
        assertTrue(o2.charged)

        val o3 = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o3 is HintEscalation.Outcome.Place)
        assertTrue(o3.charged)
        val place = o3 as HintEscalation.Outcome.Place
        assertEquals(solution[place.cellIndex], place.digit)
        assertEquals((o2 as HintEscalation.Outcome.ShowDigit).digit, place.digit)

        // Board unchanged (placement not applied): a fourth press re-emits uncharged.
        val o4 = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o4 is HintEscalation.Outcome.Place)
        assertFalse(o4.charged)
    }

    @Test
    fun pinSurvivesUnrelatedBoardChanges() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val o1 = esc.onHintPressed(current, solution, mistakeChecking = true)
        val pinnedCell = (o1 as HintEscalation.Outcome.ShowTechniques).pinned.cellIndex

        // The player correctly fills some other cell.
        val other = (0 until 81).first { current[it] == 0 && it != pinnedCell }
        current[other] = solution[other]
        esc.onBoardChanged(current, solution)

        assertEquals(pinnedCell, esc.pinned?.cellIndex)
        val o2 = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o2 is HintEscalation.Outcome.ShowDigit)
        assertEquals(solution[pinnedCell], (o2 as HintEscalation.Outcome.ShowDigit).digit)
    }

    @Test
    fun playerPlacingPinnedDigitResetsForFree() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val o1 = esc.onHintPressed(current, solution, mistakeChecking = true)
        val pin = (o1 as HintEscalation.Outcome.ShowTechniques).pinned

        current[pin.cellIndex] = pin.digit
        esc.onBoardChanged(current, solution)

        assertEquals(0, esc.level)
        assertNull(esc.pinned)
        val next = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(next is HintEscalation.Outcome.ShowTechniques)
        assertFalse(next.charged)
    }

    @Test
    fun mistakeTrackCountsHighlightsThenFixesAll() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val empties = (0 until 81).filter { current[it] == 0 }
        val bad = listOf(empties[0], empties[3])
        bad.forEach { current[it] = wrongDigitFor(it) }

        val o1 = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(o1 is HintEscalation.Outcome.ShowMistakeCount)
        assertFalse(o1.charged)
        assertEquals(2, (o1 as HintEscalation.Outcome.ShowMistakeCount).count)

        val o2 = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(o2 is HintEscalation.Outcome.HighlightMistakes)
        assertTrue(o2.charged)
        assertEquals(bad, (o2 as HintEscalation.Outcome.HighlightMistakes).cells)

        val o3 = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(o3 is HintEscalation.Outcome.FixMistakes)
        assertTrue(o3.charged)
        val fixes = (o3 as HintEscalation.Outcome.FixMistakes).corrections
        assertEquals(bad, fixes.map { it.first })
        fixes.forEach { (cell, correct) -> assertEquals(solution[cell], correct) }

        // Apply the corrections; the mistake track resets and the normal track begins.
        fixes.forEach { (cell, correct) -> current[cell] = correct }
        esc.onBoardChanged(current, solution)
        assertEquals(0, esc.mistakeLevel)
        val o4 = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(o4 is HintEscalation.Outcome.ShowTechniques)
        assertFalse(o4.charged)
    }

    @Test
    fun normalLevelSurvivesMistakeDetour() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        esc.onHintPressed(current, solution, mistakeChecking = false) // normal level 1

        val pinnedCell = esc.pinned!!.cellIndex
        val bad = (0 until 81).first { current[it] == 0 && it != pinnedCell }
        current[bad] = wrongDigitFor(bad)
        esc.onBoardChanged(current, solution)

        val detour = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(detour is HintEscalation.Outcome.ShowMistakeCount)

        // Player erases the mistake themselves.
        current[bad] = 0
        esc.onBoardChanged(current, solution)
        assertEquals(0, esc.mistakeLevel)

        // Normal track picks up where it left off: level 2, charged.
        val o = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(o is HintEscalation.Outcome.ShowDigit)
        assertTrue(o.charged)
        assertEquals(solution[pinnedCell], (o as HintEscalation.Outcome.ShowDigit).digit)
    }

    @Test
    fun mistakeCheckingOnFixesFirstWrongImmediately() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val empties = (0 until 81).filter { current[it] == 0 }
        current[empties[2]] = wrongDigitFor(empties[2])
        current[empties[0]] = wrongDigitFor(empties[0])

        val o = esc.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o is HintEscalation.Outcome.FixFirstWrong)
        assertTrue(o.charged)
        assertEquals(empties[0], (o as HintEscalation.Outcome.FixFirstWrong).cellIndex)
        assertEquals(solution[empties[0]], o.correct)
        assertEquals(0, esc.level)
        assertEquals(0, esc.mistakeLevel)
    }

    @Test
    fun fullBoardYieldsNone() {
        val esc = HintEscalation()
        val o = esc.onHintPressed(solution, solution, mistakeChecking = true)
        assertTrue(o is HintEscalation.Outcome.None)
    }

    @Test
    fun restoreContinuesTheSequence() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        esc.onHintPressed(current, solution, mistakeChecking = true)
        esc.onHintPressed(current, solution, mistakeChecking = true) // level 2

        val restored = HintEscalation()
        restored.restore(esc.level, esc.pinned, esc.mistakeLevel)
        val o = restored.onHintPressed(current, solution, mistakeChecking = true)
        assertTrue(o is HintEscalation.Outcome.Place)
        assertTrue(o.charged)
        assertEquals(esc.pinned!!.cellIndex, (o as HintEscalation.Outcome.Place).cellIndex)
    }

    @Test
    fun restoreWithoutPinResetsLevel() {
        val esc = HintEscalation()
        esc.restore(2, null, 0)
        assertEquals(0, esc.level)
        val o = esc.onHintPressed(puzzle, solution, mistakeChecking = true)
        assertTrue(o is HintEscalation.Outcome.ShowTechniques)
        assertFalse(o.charged)
    }
}
