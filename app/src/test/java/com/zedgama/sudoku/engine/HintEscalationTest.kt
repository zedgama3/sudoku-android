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

    private fun step(o: HintEscalation.Outcome): HintEscalation.Outcome.Step {
        assertTrue("expected a Step, got $o", o is HintEscalation.Outcome.Step)
        return o as HintEscalation.Outcome.Step
    }

    @Test
    fun normalTrackChargesOnceOnFirstAdvance() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()

        val o1 = step(esc.onHintPressed(current, solution, mistakeChecking = true))
        assertEquals(0, o1.index)
        assertFalse(o1.charged)
        assertFalse(esc.charged)

        val o2 = step(esc.onHintPressed(current, solution, mistakeChecking = true))
        assertEquals(1, o2.index)
        assertTrue(o2.charged)
        assertTrue(esc.charged)

        val o3 = step(esc.onHintPressed(current, solution, mistakeChecking = true))
        assertEquals(2, o3.index)
        assertFalse(o3.charged)

        val last = o1.plan.steps.lastIndex
        var latest = o3
        while (latest.index < last) {
            latest = step(esc.onHintPressed(current, solution, mistakeChecking = true))
            assertFalse(latest.charged)
        }
        // At the end: re-emits the same index, still free.
        val again = step(esc.onHintPressed(current, solution, mistakeChecking = true))
        assertEquals(last, again.index)
        assertFalse(again.charged)
    }

    @Test
    fun pinSurvivesUnrelatedBoardChanges() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val o1 = step(esc.onHintPressed(current, solution, mistakeChecking = true))
        val target = o1.plan.cellIndex
        step(esc.onHintPressed(current, solution, mistakeChecking = true)) // index 1
        val indexBefore = esc.stepIndex

        // The player correctly fills some other cell.
        val other = (0 until 81).first { current[it] == 0 && it != target }
        current[other] = solution[other]
        esc.onBoardChanged(current, solution)

        assertEquals(target, esc.plan?.cellIndex)
        assertEquals(indexBefore, esc.stepIndex)
        assertEquals(HintEngine.planFor(current, solution, target), esc.plan)
    }

    @Test
    fun playerPlacingTargetDigitResetsForFree() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val o1 = step(esc.onHintPressed(current, solution, mistakeChecking = true))

        current[o1.plan.cellIndex] = o1.plan.digit
        esc.onBoardChanged(current, solution)

        assertNull(esc.plan)
        assertEquals(0, esc.stepIndex)
        assertFalse(esc.charged)

        val next = step(esc.onHintPressed(current, solution, mistakeChecking = true))
        assertEquals(0, next.index)
        assertFalse(next.charged)
    }

    @Test
    fun targetHoldingWrongDigitKeepsPlan() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        step(esc.onHintPressed(current, solution, mistakeChecking = false))
        val o2 = step(esc.onHintPressed(current, solution, mistakeChecking = false))
        val target = o2.plan.cellIndex
        val planBefore = esc.plan

        current[target] = wrongDigitFor(target)
        esc.onBoardChanged(current, solution)
        assertEquals(planBefore, esc.plan)
        assertEquals(1, esc.stepIndex)

        val detour = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(detour is HintEscalation.Outcome.ShowMistakeCount)

        current[target] = 0
        esc.onBoardChanged(current, solution)
        assertEquals(planBefore, esc.plan)
        assertEquals(0, esc.mistakeLevel)

        val o3 = step(esc.onHintPressed(current, solution, mistakeChecking = false))
        assertEquals(2, o3.index)
        assertFalse(o3.charged)
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
        val o4 = step(esc.onHintPressed(current, solution, mistakeChecking = false))
        assertEquals(0, o4.index)
        assertFalse(o4.charged)
    }

    @Test
    fun normalLevelSurvivesMistakeDetour() {
        val esc = HintEscalation()
        val current = puzzle.copyOf()
        val o1 = step(esc.onHintPressed(current, solution, mistakeChecking = false))
        val target = o1.plan.cellIndex

        val bad = (0 until 81).first { current[it] == 0 && it != target }
        current[bad] = wrongDigitFor(bad)
        esc.onBoardChanged(current, solution)

        val detour = esc.onHintPressed(current, solution, mistakeChecking = false)
        assertTrue(detour is HintEscalation.Outcome.ShowMistakeCount)

        // Player erases the mistake themselves.
        current[bad] = 0
        esc.onBoardChanged(current, solution)
        assertEquals(0, esc.mistakeLevel)

        // Normal track picks up where it left off: step 2, and this is the one charge.
        val o = step(esc.onHintPressed(current, solution, mistakeChecking = false))
        assertEquals(1, o.index)
        assertTrue(o.charged)
        assertEquals(target, o.plan.cellIndex)
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
        assertNull(esc.plan)
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
        esc.onHintPressed(current, solution, mistakeChecking = true) // index 1, charged

        val restored = HintEscalation()
        restored.restore(esc.plan, esc.stepIndex, esc.charged, esc.mistakeLevel)
        val o = step(restored.onHintPressed(current, solution, mistakeChecking = true))
        assertEquals(2, o.index)
        assertFalse(o.charged)
        assertEquals(esc.plan!!.cellIndex, o.plan.cellIndex)
    }

    @Test
    fun restoreClampsStepIndex() {
        val esc = HintEscalation()
        val plan = HintEngine.plan(puzzle, solution)!!
        esc.restore(plan, 99, true, 0)
        assertEquals(plan.steps.lastIndex, esc.stepIndex)
    }

    @Test
    fun restoreWithoutPlanResetsState() {
        val esc = HintEscalation()
        esc.restore(null, 2, true, 0)
        assertNull(esc.plan)
        assertEquals(0, esc.stepIndex)
        assertFalse(esc.charged)
        val o = step(esc.onHintPressed(puzzle, solution, mistakeChecking = true))
        assertEquals(0, o.index)
        assertFalse(o.charged)
    }
}
