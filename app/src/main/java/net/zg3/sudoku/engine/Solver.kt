package net.zg3.sudoku.engine

import kotlin.random.Random

/**
 * Backtracking solver over a mutable bitmask representation.
 * Cells are indices 0..80; values 1..9; 0 = empty.
 */
object Solver {

    private const val ALL = 0x1FF

    /** First solution, or null if the puzzle has none (including conflicting givens). */
    fun solve(puzzle: IntArray): IntArray? {
        val search = Search(puzzle) ?: return null
        search.search(limit = 1, random = null)
        return search.firstSolution
    }

    /** Number of solutions, capped at [limit]. Returns 0 for conflicting givens. */
    fun countSolutions(puzzle: IntArray, limit: Int = 2): Int {
        val search = Search(puzzle) ?: return 0
        return search.search(limit = limit, random = null)
    }

    /** A uniformly-shuffled complete grid. */
    fun randomFullGrid(random: Random): IntArray {
        val search = Search(IntArray(81))!!
        search.search(limit = 1, random = random)
        return search.firstSolution!!
    }

    private class Search private constructor(
        val values: IntArray,
        val rowMask: IntArray,
        val colMask: IntArray,
        val boxMask: IntArray
    ) {
        var firstSolution: IntArray? = null

        fun search(limit: Int, random: Random?): Int {
            // MRV: find the empty cell with the fewest candidates
            var best = -1
            var bestCands = 0
            var bestCount = 10
            for (i in 0 until 81) {
                if (values[i] != 0) continue
                val cands = ALL and (rowMask[i / 9] or colMask[i % 9] or boxMask[boxOf(i)]).inv()
                val n = Integer.bitCount(cands)
                if (n == 0) return 0
                if (n < bestCount) {
                    bestCount = n
                    best = i
                    bestCands = cands
                    if (n == 1) break
                }
            }
            if (best == -1) {
                if (firstSolution == null) firstSolution = values.copyOf()
                return 1
            }

            val r = best / 9
            val c = best % 9
            val b = boxOf(best)
            val digits = IntArray(bestCount)
            var m = bestCands
            var k = 0
            while (m != 0) {
                val bit = m and -m
                m -= bit
                digits[k++] = Integer.numberOfTrailingZeros(bit) + 1
            }
            if (random != null) digits.shuffle(random)

            var found = 0
            for (d in digits) {
                val bit = 1 shl (d - 1)
                values[best] = d
                rowMask[r] = rowMask[r] or bit
                colMask[c] = colMask[c] or bit
                boxMask[b] = boxMask[b] or bit
                found += search(limit - found, random)
                values[best] = 0
                rowMask[r] = rowMask[r] and bit.inv()
                colMask[c] = colMask[c] and bit.inv()
                boxMask[b] = boxMask[b] and bit.inv()
                if (found >= limit) return found
            }
            return found
        }

        companion object {
            /** Returns null if the givens conflict. */
            operator fun invoke(puzzle: IntArray): Search? {
                require(puzzle.size == 81)
                val values = puzzle.copyOf()
                val rowMask = IntArray(9)
                val colMask = IntArray(9)
                val boxMask = IntArray(9)
                for (i in 0 until 81) {
                    val v = values[i]
                    if (v == 0) continue
                    val bit = 1 shl (v - 1)
                    val r = i / 9
                    val c = i % 9
                    val b = boxOf(i)
                    if (rowMask[r] and bit != 0 || colMask[c] and bit != 0 || boxMask[b] and bit != 0) {
                        return null
                    }
                    rowMask[r] = rowMask[r] or bit
                    colMask[c] = colMask[c] or bit
                    boxMask[b] = boxMask[b] or bit
                }
                return Search(values, rowMask, colMask, boxMask)
            }
        }
    }
}

private fun IntArray.shuffle(random: Random) {
    for (i in size - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val t = this[i]
        this[i] = this[j]
        this[j] = t
    }
}
