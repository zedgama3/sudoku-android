package com.zedgama.sudoku.engine

enum class Technique(val rank: Int, val label: String) {
    NAKED_SINGLE(1, "Naked single"),
    HIDDEN_SINGLE(1, "Hidden single"),
    LOCKED_CANDIDATES(2, "Locked candidates"),
    NAKED_PAIR(3, "Naked pair"),
    HIDDEN_PAIR(3, "Hidden pair"),
}

/** A single deduction. Placement steps have value != 0; elimination steps only refine candidates. */
data class LogicalStep(
    val technique: Technique,
    val cellIndex: Int,                 // placement target, or a representative pattern cell
    val value: Int,                     // placed digit, or 0 for eliminations
    val involvedCells: List<Int>,       // the PATTERN cells only
    val message: String,
    val house: Int = -1,                // 0..26 house the pattern is read in; -1 for naked single
    val eliminationHouse: Int = -1,     // house the candidates are removed from (eliminations)
    val eliminatedCells: List<Int> = emptyList(),
    val digits: List<Int> = emptyList() // digits the pattern is about
)

data class LogicalResult(val solved: Boolean, val techniques: Set<Technique>)

/** The ordered deductions leading from a position to a tie-broken placement. */
data class HintChain(
    val eliminations: List<LogicalStep>,   // value == 0, in application order
    val placement: LogicalStep             // value != 0
) {
    /** Distinct techniques in order of first application, ending with the placement's. */
    val techniques: List<Technique>
        get() = (eliminations.map { it.technique } + placement.technique).distinct()
}

/**
 * Solves with a small ladder of human techniques, in order of difficulty:
 * naked single, hidden single, locked candidates (pointing/claiming), naked pair, hidden pair.
 */
object LogicalSolver {

    /** Houses 0..8 = rows, 9..17 = cols, 18..26 = boxes. */
    private val houses: Array<IntArray> = Array(27) { h ->
        when {
            h < 9 -> IntArray(9) { h * 9 + it }
            h < 18 -> IntArray(9) { it * 9 + (h - 9) }
            else -> {
                val b = h - 18
                val base = (b / 3) * 27 + (b % 3) * 3
                IntArray(9) { base + (it / 3) * 9 + it % 3 }
            }
        }
    }

    fun houseName(h: Int): String = when {
        h < 9 -> "row ${h + 1}"
        h < 18 -> "column ${h - 8}"
        else -> "box ${h - 17}"
    }

    /** The 9 cell indexes of house [h] (0..8 rows, 9..17 cols, 18..26 boxes). */
    fun houseCells(h: Int): List<Int> = houses[h].toList()

    fun solve(puzzle: IntArray): LogicalResult {
        val grid = CandidateGrid(puzzle) ?: return LogicalResult(false, emptySet())
        val used = mutableSetOf<Technique>()
        while (!grid.isFull) {
            val step = nextStep(grid) ?: return LogicalResult(false, used)
            used.add(step.technique)
        }
        return LogicalResult(true, used)
    }

    /**
     * Advances the grid by one deduction (applying it) and returns the step, or null if the ladder stalls.
     */
    fun nextStep(grid: CandidateGrid): LogicalStep? =
        nakedSingle(grid)
            ?: hiddenSingle(grid)
            ?: lockedCandidates(grid)
            ?: nakedPair(grid)
            ?: hiddenPair(grid)

    /**
     * Runs the ladder from [puzzle] until it produces a placement, silently applying eliminations.
     * Returns null if no placement can be derived.
     */
    fun firstPlacement(puzzle: IntArray): LogicalStep? {
        val grid = CandidateGrid(puzzle) ?: return null
        while (true) {
            val step = nextStep(grid) ?: return null
            if (step.value != 0) return step
        }
    }

    /**
     * All rank-1 placements available right now, without applying any of them.
     * Deduped by (cell, digit); a naked single wins attribution over a hidden single.
     */
    fun enumerateSingles(grid: CandidateGrid): List<LogicalStep> {
        val steps = mutableListOf<LogicalStep>()
        val seen = HashSet<Int>() // cellIndex * 10 + digit
        for (i in 0 until 81) {
            if (grid.values[i] != 0) continue
            val c = grid.cands[i]
            if (Integer.bitCount(c) == 1) {
                val d = Integer.numberOfTrailingZeros(c) + 1
                if (seen.add(i * 10 + d)) {
                    steps.add(
                        LogicalStep(
                            Technique.NAKED_SINGLE, i, d, listOf(i),
                            "Naked single: $d is the only candidate left in this cell.",
                            digits = listOf(d)
                        )
                    )
                }
            }
        }
        for (h in 0 until 27) {
            val cells = houses[h]
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                var target = -1
                var count = 0
                for (i in cells) {
                    if (grid.values[i] == 0 && grid.cands[i] and bit != 0) {
                        target = i
                        if (++count > 1) break
                    }
                }
                if (count == 1 && seen.add(target * 10 + d)) {
                    steps.add(
                        LogicalStep(
                            Technique.HIDDEN_SINGLE, target, d, cells.toList(),
                            "Hidden single: $d fits only one cell in ${houseName(h)}.",
                            house = h,
                            digits = listOf(d)
                        )
                    )
                }
            }
        }
        return steps
    }

    /** Runs eliminations until [pick] returns a single, then returns the chain; null if it stalls. */
    private inline fun chain(
        puzzle: IntArray,
        pick: (List<LogicalStep>, CandidateGrid) -> LogicalStep?
    ): HintChain? {
        val grid = CandidateGrid(puzzle) ?: return null
        val eliminations = mutableListOf<LogicalStep>()
        while (true) {
            pick(enumerateSingles(grid), grid)?.let { return HintChain(eliminations, it) }
            eliminations.add(nextElimination(grid) ?: return null)
        }
    }

    /** The easiest available placement: see [easiestFirst]. */
    fun easiestChain(puzzle: IntArray): HintChain? =
        chain(puzzle) { singles, grid -> singles.minWithOrNull(easiestFirst(grid)) }

    /** The chain that makes [target] a single, if the ladder gets there without placing anything else. */
    fun chainFor(puzzle: IntArray, target: Int): HintChain? =
        chain(puzzle) { singles, _ -> singles.firstOrNull { it.cellIndex == target } }

    /**
     * Naked singles before hidden singles; among naked singles the most filled peers (easiest to
     * see); among hidden singles the house with the fewest empty cells; then cell index, then digit.
     * Elimination count is implicit: the loop stops the first time any single exists.
     */
    private fun easiestFirst(grid: CandidateGrid): Comparator<LogicalStep> = compareBy(
        { if (it.technique == Technique.NAKED_SINGLE) 0 else 1 },
        {
            if (it.technique == Technique.NAKED_SINGLE)
                -peersOf(it.cellIndex).count { p -> grid.values[p] != 0 }
            else
                houseCells(it.house).count { c -> grid.values[c] == 0 }
        },
        { it.cellIndex },
        { it.value }
    )

    private fun nextElimination(g: CandidateGrid): LogicalStep? =
        lockedCandidates(g) ?: nakedPair(g) ?: hiddenPair(g)

    private fun nakedSingle(g: CandidateGrid): LogicalStep? {
        for (i in 0 until 81) {
            if (g.values[i] != 0) continue
            val c = g.cands[i]
            if (Integer.bitCount(c) == 1) {
                val d = Integer.numberOfTrailingZeros(c) + 1
                g.place(i, d)
                return LogicalStep(
                    Technique.NAKED_SINGLE, i, d, listOf(i),
                    "Naked single: $d is the only candidate left in this cell.",
                    digits = listOf(d)
                )
            }
        }
        return null
    }

    private fun hiddenSingle(g: CandidateGrid): LogicalStep? {
        for (h in 0 until 27) {
            val cells = houses[h]
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                var target = -1
                var count = 0
                for (i in cells) {
                    if (g.values[i] == 0 && g.cands[i] and bit != 0) {
                        target = i
                        if (++count > 1) break
                    }
                }
                if (count == 1) {
                    g.place(target, d)
                    return LogicalStep(
                        Technique.HIDDEN_SINGLE, target, d, cells.toList(),
                        "Hidden single: $d fits only one cell in ${houseName(h)}.",
                        house = h,
                        digits = listOf(d)
                    )
                }
            }
        }
        return null
    }

    private fun lockedCandidates(g: CandidateGrid): LogicalStep? {
        // Pointing: candidates for d in a box confined to one row/col eliminate d elsewhere in that line.
        for (b in 0 until 9) {
            val cells = houses[18 + b]
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val spots = cells.filter { g.values[it] == 0 && g.cands[it] and bit != 0 }
                if (spots.size < 2) continue
                val rows = spots.map { rowOf(it) }.distinct()
                val cols = spots.map { colOf(it) }.distinct()
                val lineHouse: Int = when {
                    rows.size == 1 -> rows[0]
                    cols.size == 1 -> 9 + cols[0]
                    else -> -1
                }
                val line: IntArray? = if (lineHouse >= 0) houses[lineHouse] else null
                if (line != null) {
                    val eliminated = mutableListOf<Int>()
                    for (i in line) {
                        if (boxOf(i) != b && g.values[i] == 0 && g.cands[i] and bit != 0) {
                            g.eliminate(i, d)
                            eliminated.add(i)
                        }
                    }
                    if (eliminated.isNotEmpty()) {
                        return LogicalStep(
                            Technique.LOCKED_CANDIDATES, spots[0], 0, spots,
                            "Locked candidates: in box ${b + 1}, $d is confined to one line, removing it elsewhere on that line.",
                            house = 18 + b,
                            eliminationHouse = lineHouse,
                            eliminatedCells = eliminated.toList(),
                            digits = listOf(d)
                        )
                    }
                }
            }
        }
        // Claiming: candidates for d in a row/col confined to one box eliminate d elsewhere in that box.
        for (h in 0 until 18) {
            val cells = houses[h]
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val spots = cells.filter { g.values[it] == 0 && g.cands[it] and bit != 0 }
                if (spots.size < 2) continue
                val boxes = spots.map { boxOf(it) }.distinct()
                if (boxes.size == 1) {
                    val eliminated = mutableListOf<Int>()
                    for (i in houses[18 + boxes[0]]) {
                        if (i !in spots && g.values[i] == 0 && g.cands[i] and bit != 0) {
                            g.eliminate(i, d)
                            eliminated.add(i)
                        }
                    }
                    if (eliminated.isNotEmpty()) {
                        return LogicalStep(
                            Technique.LOCKED_CANDIDATES, spots[0], 0, spots,
                            "Locked candidates: in ${houseName(h)}, $d is confined to one box, removing it from the rest of the box.",
                            house = h,
                            eliminationHouse = 18 + boxes[0],
                            eliminatedCells = eliminated.toList(),
                            digits = listOf(d)
                        )
                    }
                }
            }
        }
        return null
    }

    private fun nakedPair(g: CandidateGrid): LogicalStep? {
        for (h in 0 until 27) {
            val cells = houses[h]
            val open = cells.filter { g.values[it] == 0 }
            for (a in open.indices) for (b in a + 1 until open.size) {
                val i = open[a]
                val j = open[b]
                val mask = g.cands[i]
                if (Integer.bitCount(mask) == 2 && g.cands[j] == mask) {
                    val eliminated = mutableListOf<Int>()
                    for (k in open) {
                        if (k == i || k == j) continue
                        if (g.cands[k] and mask != 0) {
                            var m = g.cands[k] and mask
                            while (m != 0) {
                                val bit = m and -m
                                m -= bit
                                g.eliminate(k, Integer.numberOfTrailingZeros(bit) + 1)
                            }
                            eliminated.add(k)
                        }
                    }
                    if (eliminated.isNotEmpty()) {
                        return LogicalStep(
                            Technique.NAKED_PAIR, i, 0, listOf(i, j),
                            "Naked pair in ${houseName(h)}: two cells share the same two candidates, removing them from the rest of the house.",
                            house = h,
                            eliminationHouse = h,
                            eliminatedCells = eliminated.toList(),
                            digits = (1..9).filter { d -> mask and (1 shl (d - 1)) != 0 }
                        )
                    }
                }
            }
        }
        return null
    }

    private fun hiddenPair(g: CandidateGrid): LogicalStep? {
        for (h in 0 until 27) {
            val cells = houses[h]
            // For each digit, the set of open cells that can take it
            val spotsByDigit = Array(10) { emptyList<Int>() }
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                spotsByDigit[d] = cells.filter { g.values[it] == 0 && g.cands[it] and bit != 0 }
            }
            for (d1 in 1..8) for (d2 in d1 + 1..9) {
                val s1 = spotsByDigit[d1]
                if (s1.size != 2 || s1 != spotsByDigit[d2]) continue
                val pairMask = (1 shl (d1 - 1)) or (1 shl (d2 - 1))
                var changed = false
                for (i in s1) {
                    var extra = g.cands[i] and pairMask.inv()
                    while (extra != 0) {
                        val bit = extra and -extra
                        extra -= bit
                        g.eliminate(i, Integer.numberOfTrailingZeros(bit) + 1)
                        changed = true
                    }
                }
                if (changed) {
                    return LogicalStep(
                        Technique.HIDDEN_PAIR, s1[0], 0, s1,
                        "Hidden pair in ${houseName(h)}: $d1 and $d2 can only go in the same two cells, clearing their other candidates.",
                        house = h,
                        eliminationHouse = h,
                        eliminatedCells = s1,
                        digits = listOf(d1, d2)
                    )
                }
            }
        }
        return null
    }
}

/** Mutable value + candidate-mask grid used by [LogicalSolver]. */
class CandidateGrid private constructor(
    val values: IntArray,
    val cands: IntArray
) {
    val isFull: Boolean get() = values.all { it != 0 }

    fun place(index: Int, digit: Int) {
        values[index] = digit
        cands[index] = 0
        val bit = 1 shl (digit - 1)
        for (i in 0 until 81) {
            if (values[i] == 0 &&
                (rowOf(i) == rowOf(index) || colOf(i) == colOf(index) || boxOf(i) == boxOf(index))
            ) {
                cands[i] = cands[i] and bit.inv()
            }
        }
    }

    fun eliminate(index: Int, digit: Int) {
        cands[index] = cands[index] and (1 shl (digit - 1)).inv()
    }

    companion object {
        /** Returns null if the givens conflict. */
        operator fun invoke(puzzle: IntArray): CandidateGrid? {
            if (hasConflict(puzzle)) return null
            val values = puzzle.copyOf()
            val cands = IntArray(81)
            for (i in 0 until 81) {
                if (values[i] != 0) continue
                var used = 0
                for (j in 0 until 81) {
                    if (values[j] != 0 &&
                        (rowOf(i) == rowOf(j) || colOf(i) == colOf(j) || boxOf(i) == boxOf(j))
                    ) {
                        used = used or (1 shl (values[j] - 1))
                    }
                }
                cands[i] = 0x1FF and used.inv()
            }
            return CandidateGrid(values, cands)
        }

        private fun hasConflict(puzzle: IntArray): Boolean {
            for (i in 0 until 81) {
                if (puzzle[i] == 0) continue
                for (j in i + 1 until 81) {
                    if (puzzle[j] == puzzle[i] &&
                        (rowOf(i) == rowOf(j) || colOf(i) == colOf(j) || boxOf(i) == boxOf(j))
                    ) return true
                }
            }
            return false
        }
    }
}
