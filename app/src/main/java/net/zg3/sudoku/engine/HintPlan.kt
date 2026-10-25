package net.zg3.sudoku.engine

/** What the board tints for one step. Cells are indexes 0..80. */
data class HintHighlight(
    val primary: List<Int> = emptyList(),    // strong tint (the cell(s) the step is about)
    val secondary: List<Int> = emptyList(),  // soft tint (a house, peers, supporting cells)
    val digit: Int? = null                   // tints every placed instance of this digit
)

data class HintStep(
    val text: String,
    val highlight: HintHighlight = HintHighlight(),
    val techniques: List<Technique> = emptyList()   // rendered as chips when non-empty
)

/** A walkthrough toward one placement. Never mutates the board; the last step names the digit. */
data class HintPlan(
    val cellIndex: Int,
    val digit: Int,
    val techniques: List<Technique>,   // empty when [isReveal]
    val isReveal: Boolean,
    val steps: List<HintStep>          // size >= 2
)

/**
 * Turns a [HintChain] into an ordered walkthrough: an intro naming the techniques, two steps per
 * elimination in the chain, then the placement ladder. The last step always names the digit and
 * tints the target cell — it never fills anything in.
 */
object HintPlanner {

    fun build(chain: HintChain, current: IntArray): HintPlan {
        val placement = chain.placement
        val steps = mutableListOf<HintStep>()
        steps.add(intro(chain))
        chain.eliminations.forEach { steps.addAll(eliminationSteps(it)) }
        steps.addAll(placementSteps(placement, chain.eliminations.isNotEmpty(), current))
        return HintPlan(
            cellIndex = placement.cellIndex,
            digit = placement.value,
            techniques = chain.techniques,
            isReveal = false,
            steps = steps
        )
    }

    fun reveal(cellIndex: Int, digit: Int): HintPlan = HintPlan(
        cellIndex = cellIndex,
        digit = digit,
        techniques = emptyList(),
        isReveal = true,
        steps = listOf(
            HintStep("No standard technique applies here — the next step reveals the digit."),
            HintStep(
                "It's a $digit. Enter it here.",
                HintHighlight(primary = listOf(cellIndex), digit = digit)
            )
        )
    )

    private fun intro(chain: HintChain): HintStep {
        val techniques = chain.techniques
        val text = if (techniques.size == 1) {
            "Try a ${techniques.first().label.lowercase()}."
        } else {
            "This one takes a few steps:"
        }
        return HintStep(text, HintHighlight(), techniques)
    }

    private fun eliminationSteps(step: LogicalStep): List<HintStep> {
        val house = step.house
        val houseCells = LogicalSolver.houseCells(house)
        val d1 = step.digits.getOrElse(0) { 0 }
        val d2 = step.digits.getOrElse(1) { 0 }
        return when (step.technique) {
            Technique.LOCKED_CANDIDATES -> listOf(
                HintStep(
                    "In ${LogicalSolver.houseName(house)}, $d1 can only go in these cells.",
                    HintHighlight(step.involvedCells, houseCells, d1),
                    listOf(step.technique)
                ),
                HintStep(
                    "So $d1 can't go anywhere else in ${LogicalSolver.houseName(step.eliminationHouse)}.",
                    HintHighlight(step.eliminatedCells, step.involvedCells, d1)
                )
            )
            Technique.NAKED_PAIR -> listOf(
                HintStep(
                    "In ${LogicalSolver.houseName(house)}, these two cells can only be $d1 or $d2.",
                    HintHighlight(step.involvedCells, houseCells),
                    listOf(step.technique)
                ),
                HintStep(
                    "So $d1 and $d2 come off the other cells in ${LogicalSolver.houseName(house)}.",
                    HintHighlight(step.eliminatedCells, step.involvedCells)
                )
            )
            Technique.HIDDEN_PAIR -> listOf(
                HintStep(
                    "In ${LogicalSolver.houseName(house)}, $d1 and $d2 fit only in these two cells.",
                    HintHighlight(step.involvedCells, houseCells),
                    listOf(step.technique)
                ),
                HintStep(
                    "So those two cells can't hold anything else.",
                    HintHighlight(step.involvedCells)
                )
            )
            // Placements never reach here; a placement-shaped step contributes no elimination text.
            Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE -> emptyList()
        }
    }

    private fun placementSteps(
        placement: LogicalStep,
        afterEliminations: Boolean,
        current: IntArray
    ): List<HintStep> {
        val cell = placement.cellIndex
        val d = placement.value
        return when (placement.technique) {
            Technique.HIDDEN_SINGLE -> {
                val h = placement.house
                val houseCells = LogicalSolver.houseCells(h)
                val name = LogicalSolver.houseName(h)
                listOf(
                    HintStep("Look at $name.", HintHighlight(secondary = houseCells)),
                    HintStep(
                        "Where can $d go in $name?",
                        HintHighlight(secondary = houseCells, digit = d)
                    ),
                    HintStep(
                        "Only this cell can take $d.",
                        HintHighlight(listOf(cell), houseCells, d)
                    ),
                    HintStep("Enter $d here.", HintHighlight(primary = listOf(cell), digit = d))
                )
            }
            else -> {
                val filledPeers = peersOf(cell).filter { current[it] != 0 }
                listOf(
                    HintStep("Look at this cell.", HintHighlight(primary = listOf(cell))),
                    HintStep(
                        if (afterEliminations) {
                            "After those eliminations, only one candidate is left here."
                        } else {
                            "Its row, column and box already contain every other digit."
                        },
                        HintHighlight(listOf(cell), filledPeers)
                    ),
                    HintStep(
                        "It's a $d. Enter it here.",
                        HintHighlight(primary = listOf(cell), digit = d)
                    )
                )
            }
        }
    }
}
