package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HintPlannerTest {

    private val solution = TestPuzzles.parse(TestPuzzles.KNOWN_SOLUTION)

    /** The solution with [target] cleared: every other cell is filled. */
    private fun boardWithout(target: Int): IntArray = solution.copyOf().also { it[target] = 0 }

    private fun nakedSingle(cell: Int) = LogicalStep(
        Technique.NAKED_SINGLE, cell, solution[cell], listOf(cell),
        "naked single", digits = listOf(solution[cell])
    )

    private fun hiddenSingle(cell: Int, house: Int) = LogicalStep(
        Technique.HIDDEN_SINGLE, cell, solution[cell], LogicalSolver.houseCells(house),
        "hidden single", house = house, digits = listOf(solution[cell])
    )

    private fun lockedCandidates() = LogicalStep(
        Technique.LOCKED_CANDIDATES, 0, 0, listOf(0, 1),
        "locked candidates", house = 18, eliminationHouse = 0,
        eliminatedCells = listOf(3, 4), digits = listOf(7)
    )

    private fun nakedPair() = LogicalStep(
        Technique.NAKED_PAIR, 0, 0, listOf(0, 1),
        "naked pair", house = 0, eliminationHouse = 0,
        eliminatedCells = listOf(2, 3), digits = listOf(2, 6)
    )

    private fun hiddenPair() = LogicalStep(
        Technique.HIDDEN_PAIR, 0, 0, listOf(0, 1),
        "hidden pair", house = 0, eliminationHouse = 0,
        eliminatedCells = listOf(0, 1), digits = listOf(2, 6)
    )

    @Test
    fun nakedSingleLadderIsFourSteps() {
        val target = 40
        val current = boardWithout(target)
        val plan = HintPlanner.build(HintChain(emptyList(), nakedSingle(target)), current)
        assertEquals(4, plan.steps.size)
        assertEquals(listOf(Technique.NAKED_SINGLE), plan.steps[0].techniques)
        assertEquals(peersOf(target).filter { current[it] != 0 }, plan.steps[2].highlight.secondary)
        assertTrue(plan.steps[2].text.startsWith("Its row, column and box"))
    }

    @Test
    fun hiddenSingleLadderIsFiveSteps() {
        val target = 40
        val house = 4 // row 5, which contains cell 40
        val current = boardWithout(target)
        val plan = HintPlanner.build(HintChain(emptyList(), hiddenSingle(target, house)), current)
        assertEquals(5, plan.steps.size)
        assertEquals(LogicalSolver.houseCells(house), plan.steps[1].highlight.secondary)
        assertEquals(solution[target], plan.steps[2].highlight.digit)
    }

    @Test
    fun lockedCandidatesPrefixAddsTwoSteps() {
        val target = 40
        val current = boardWithout(target)
        val chain = HintChain(listOf(lockedCandidates()), nakedSingle(target))
        val plan = HintPlanner.build(chain, current)
        assertEquals(6, plan.steps.size) // intro + 2 + 3
        assertEquals(chain.techniques, plan.steps[0].techniques)
        assertEquals(listOf(Technique.LOCKED_CANDIDATES), plan.steps[1].techniques)
        assertTrue(plan.steps[4].text.startsWith("After those eliminations"))
    }

    @Test
    fun nakedPairPrefixAddsTwoSteps() {
        val target = 40
        val chain = HintChain(listOf(nakedPair()), nakedSingle(target))
        val plan = HintPlanner.build(chain, boardWithout(target))
        assertEquals(6, plan.steps.size)
        assertEquals(chain.techniques, plan.steps[0].techniques)
        assertEquals(listOf(Technique.NAKED_PAIR), plan.steps[1].techniques)
        assertTrue(plan.steps[4].text.startsWith("After those eliminations"))
    }

    @Test
    fun hiddenPairPrefixAddsTwoSteps() {
        val target = 40
        val chain = HintChain(listOf(hiddenPair()), nakedSingle(target))
        val plan = HintPlanner.build(chain, boardWithout(target))
        assertEquals(6, plan.steps.size)
        assertEquals(chain.techniques, plan.steps[0].techniques)
        assertEquals(listOf(Technique.HIDDEN_PAIR), plan.steps[1].techniques)
        assertTrue(plan.steps[4].text.startsWith("After those eliminations"))
    }

    @Test
    fun revealPlanIsTwoSteps() {
        val plan = HintPlanner.reveal(40, solution[40])
        assertEquals(2, plan.steps.size)
        assertTrue(plan.isReveal)
        assertTrue(plan.techniques.isEmpty())
    }

    @Test
    fun everyLadderEndsOnTheTargetWithoutFilling() {
        val target = 40
        val current = boardWithout(target)
        val plans = listOf(
            HintPlanner.build(HintChain(emptyList(), nakedSingle(target)), current),
            HintPlanner.build(HintChain(emptyList(), hiddenSingle(target, 4)), current),
            HintPlanner.build(HintChain(listOf(lockedCandidates()), nakedSingle(target)), current),
            HintPlanner.build(HintChain(listOf(nakedPair()), nakedSingle(target)), current),
            HintPlanner.build(HintChain(listOf(hiddenPair()), nakedSingle(target)), current),
            HintPlanner.reveal(target, solution[target])
        )
        for (plan in plans) {
            assertEquals(listOf(plan.cellIndex), plan.steps.last().highlight.primary)
            assertEquals(plan.digit, plan.steps.last().highlight.digit)
            assertEquals(HintHighlight(), plan.steps.first().highlight)
            assertTrue(plan.steps.size >= 2)
        }
    }
}
