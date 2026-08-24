package com.zedgama.sudoku.engine

/**
 * State machine for the tiered hint flow.
 *
 * Normal track (no wrong entries): level 1 names the techniques leading to the next placement
 * (free), level 2 reveals its digit (+1), level 3 places it (+1). The target is pinned on the
 * first press so all tiers describe the same move, and the sequence resets only once the pinned
 * digit sits in the pinned cell — whether a hint put it there or the player did — so nobody is
 * ever charged twice for the same hint.
 *
 * Mistake track (wrong entries present and mistake checking off): level 1 counts the wrong
 * cells (free), level 2 highlights them (+1), level 3 corrects them all (+1). With mistake
 * checking on, wrong cells are already visible, so a press simply corrects the first one (+1).
 *
 * A press at an already-max level re-emits the same outcome without charging.
 */
class HintEscalation {

    var level: Int = 0
        private set
    var pinned: PinnedHint? = null
        private set
    var mistakeLevel: Int = 0
        private set

    sealed class Outcome {
        abstract val charged: Boolean

        data class ShowTechniques(val pinned: PinnedHint) : Outcome() {
            override val charged: Boolean get() = false
        }

        data class ShowDigit(val digit: Int, override val charged: Boolean) : Outcome()

        data class Place(
            val cellIndex: Int,
            val digit: Int,
            val techniques: List<Technique>,
            val isReveal: Boolean,
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
        val pin = pinned ?: HintEngine.pinTarget(current, solution, selected) ?: return Outcome.None
        pinned = pin
        val advanced = level < 3
        if (advanced) level++
        return when (level) {
            1 -> Outcome.ShowTechniques(pin)
            2 -> Outcome.ShowDigit(pin.digit, charged = advanced)
            else -> Outcome.Place(pin.cellIndex, pin.digit, pin.techniques, pin.isReveal, charged = advanced)
        }
    }

    /**
     * Call after every board change. Resets the normal track once the pinned digit sits in the
     * pinned cell, and the mistake track once no wrong cells remain.
     */
    fun onBoardChanged(current: IntArray, solution: IntArray) {
        val pin = pinned
        if (pin != null && current[pin.cellIndex] == pin.digit) {
            pinned = null
            level = 0
        }
        if (mistakeLevel != 0 && HintEngine.wrongCells(current, solution).isEmpty()) {
            mistakeLevel = 0
        }
    }

    fun reset() {
        level = 0
        pinned = null
        mistakeLevel = 0
    }

    fun restore(level: Int, pinned: PinnedHint?, mistakeLevel: Int) {
        this.pinned = pinned
        this.level = if (pinned == null) 0 else level.coerceIn(0, 3)
        this.mistakeLevel = mistakeLevel.coerceIn(0, 3)
    }
}
