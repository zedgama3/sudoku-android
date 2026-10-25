package net.zg3.sudoku.engine

/**
 * State machine for the hint flow.
 *
 * Normal track (no wrong entries): the first press builds a [HintPlan] for the easiest cell on the
 * board and shows step 1 (free). Each further press advances one step; the single charge for the
 * plan is taken the first time the player advances past step 1, and a press at the last step
 * re-emits it for free. No step ever fills the cell — the last one just names the digit.
 *
 * The plan is the source of truth for the card: it stays pinned through taps, notes and digits,
 * is rebuilt (with the step index clamped) after any clean board change so the reasoning stays
 * true, and clears only when the target cell holds the target digit or the game is reset.
 *
 * Mistake track (wrong entries present and mistake checking off): level 1 counts the wrong
 * cells (free), level 2 highlights them (+1), level 3 corrects them all (+1). With mistake
 * checking on, wrong cells are already visible, so a press simply corrects the first one (+1).
 * While the board holds a wrong entry the plan is left exactly as it was.
 */
class HintEscalation {

    var plan: HintPlan? = null
        private set
    var stepIndex: Int = 0
        private set
    var charged: Boolean = false
        private set
    var mistakeLevel: Int = 0
        private set

    val currentStep: HintStep? get() = plan?.steps?.getOrNull(stepIndex)

    sealed class Outcome {
        abstract val charged: Boolean

        /** Show [index] of [plan]'s steps. */
        data class Step(
            val plan: HintPlan,
            val index: Int,
            override val charged: Boolean
        ) : Outcome()

        data class ShowMistakeCount(val count: Int) : Outcome() {
            override val charged: Boolean get() = false
        }

        data class HighlightMistakes(val cells: List<Int>, override val charged: Boolean) : Outcome()

        /** Corrects every wrong cell for a single charge; (cellIndex, correct) in reading order. */
        data class FixMistakes(
            val corrections: List<Pair<Int, Int>>,
            override val charged: Boolean
        ) : Outcome()

        /** Legacy path when mistake checking is on: fix the first wrong cell immediately. */
        data class FixFirstWrong(val cellIndex: Int, val correct: Int) : Outcome() {
            override val charged: Boolean get() = true
        }

        data object None : Outcome() {
            override val charged: Boolean get() = false
        }
    }

    fun onHintPressed(
        current: IntArray,
        solution: IntArray,
        mistakeChecking: Boolean,
        selected: Int? = null
    ): Outcome {
        val wrong = HintEngine.wrongCells(current, solution)
        if (wrong.isNotEmpty()) {
            if (mistakeChecking) {
                val cell = wrong.first()
                return Outcome.FixFirstWrong(cell, solution[cell])
            }
            val advanced = mistakeLevel < 3
            if (advanced) mistakeLevel++
            return when (mistakeLevel) {
                1 -> Outcome.ShowMistakeCount(wrong.size)
                2 -> Outcome.HighlightMistakes(wrong, charged = advanced)
                else -> Outcome.FixMistakes(wrong.map { it to solution[it] }, charged = advanced)
            }
        }

        if (current.all { it != 0 }) return Outcome.None
        val existing = plan
        if (existing == null) {
            val fresh = HintEngine.plan(current, solution, selected) ?: return Outcome.None
            plan = fresh
            stepIndex = 0
            charged = false
            return Outcome.Step(fresh, 0, charged = false)
        }
        if (stepIndex < existing.steps.lastIndex) {
            stepIndex++
            val chargeNow = !charged
            charged = true
            return Outcome.Step(existing, stepIndex, charged = chargeNow)
        }
        return Outcome.Step(existing, stepIndex, charged = false)   // at the end: re-emit, free
    }

    /**
     * Call after every board change. Clears the plan once the target digit sits in the target
     * cell, rebuilds it (clamping the step index) after any other clean change, and leaves it
     * untouched while the board holds a wrong entry. The mistake track resets once the board is
     * clean again.
     */
    fun onBoardChanged(current: IntArray, solution: IntArray) {
        val p = plan
        val wrong = HintEngine.wrongCells(current, solution)
        if (p != null) {
            when {
                // solved by the player
                current[p.cellIndex] == p.digit -> {
                    plan = null
                    stepIndex = 0
                    charged = false
                }
                // board is unreliable (incl. a wrong digit in the target): keep the plan pinned as is
                wrong.isNotEmpty() -> Unit
                else -> {
                    val fresh = HintEngine.planFor(current, solution, p.cellIndex)
                    if (fresh != p) {
                        plan = fresh
                        stepIndex = stepIndex.coerceIn(0, fresh.steps.lastIndex)
                    }
                }
            }
        }
        if (mistakeLevel != 0 && wrong.isEmpty()) mistakeLevel = 0
    }

    fun reset() {
        plan = null
        stepIndex = 0
        charged = false
        mistakeLevel = 0
    }

    fun restore(plan: HintPlan?, stepIndex: Int, charged: Boolean, mistakeLevel: Int) {
        this.plan = plan
        this.stepIndex = if (plan == null) 0 else stepIndex.coerceIn(0, plan.steps.lastIndex)
        this.charged = if (plan == null) false else charged
        this.mistakeLevel = mistakeLevel.coerceIn(0, 3)
    }
}
