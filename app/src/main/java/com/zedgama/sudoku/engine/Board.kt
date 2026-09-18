package com.zedgama.sudoku.engine

fun rowOf(index: Int): Int = index / 9
fun colOf(index: Int): Int = index % 9
fun boxOf(index: Int): Int = (index / 27) * 3 + (index % 9) / 3

/** The 20 cells sharing a row, column or box with [index]. */
fun peersOf(index: Int): List<Int> = (0 until 81).filter {
    it != index && (rowOf(it) == rowOf(index) || colOf(it) == colOf(index) || boxOf(it) == boxOf(index))
}

data class Cell(
    val value: Int = 0,        // 0 = empty, 1..9
    val isGiven: Boolean = false,
    val notes: Int = 0         // bit (d-1) set = note for digit d
)

data class Board(val cells: List<Cell>) {

    init {
        require(cells.size == 81) { "Board must have 81 cells, got ${cells.size}" }
    }

    operator fun get(index: Int): Cell = cells[index]

    fun with(index: Int, cell: Cell): Board {
        val copy = cells.toMutableList()
        copy[index] = cell
        return Board(copy)
    }

    fun values(): IntArray = IntArray(81) { cells[it].value }

    val isFull: Boolean get() = cells.all { it.value != 0 }

    /** Indices of non-given cells whose value conflicts with a peer (row/col/box duplicate). */
    fun conflictingCells(): Set<Int> {
        val result = mutableSetOf<Int>()
        for (i in 0 until 81) {
            val v = cells[i].value
            if (v == 0) continue
            for (j in 0 until 81) {
                if (j == i) continue
                if (cells[j].value == v &&
                    (rowOf(i) == rowOf(j) || colOf(i) == colOf(j) || boxOf(i) == boxOf(j))
                ) {
                    result.add(i)
                    break
                }
            }
        }
        return result
    }

    companion object {
        fun empty(): Board = Board(List(81) { Cell() })

        /** Parse an 81-char string; '0' or '.' means empty. Non-empty cells are givens. */
        fun fromString(s: String): Board {
            require(s.length == 81) { "Expected 81 chars, got ${s.length}" }
            return Board(s.map { ch ->
                val v = if (ch == '.' || ch == '0') 0 else (ch - '0')
                require(v in 0..9) { "Invalid char '$ch'" }
                Cell(value = v, isGiven = v != 0)
            })
        }

        /** Build a playing board from a givens string; all non-zero cells become givens. */
        fun fromGivens(givens: String): Board = fromString(givens)
    }
}

fun IntArray.toBoardString(): String = joinToString("") { it.toString() }
