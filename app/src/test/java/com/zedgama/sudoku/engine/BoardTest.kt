package com.zedgama.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    @Test
    fun parseRoundTrip() {
        val board = Board.fromString(TestPuzzles.KNOWN_PUZZLE)
        assertEquals(TestPuzzles.KNOWN_PUZZLE, board.values().toBoardString())
        assertEquals(5, board[0].value)
        assertTrue(board[0].isGiven)
        assertTrue(!board[2].isGiven)
    }

    @Test
    fun houseIndexing() {
        assertEquals(0, boxOf(0))
        assertEquals(0, boxOf(20))   // row 2, col 2
        assertEquals(4, boxOf(40))   // center
        assertEquals(8, boxOf(80))
        assertEquals(4, rowOf(40))
        assertEquals(4, colOf(40))
    }

    @Test
    fun conflictDetection() {
        var board = Board.fromString(TestPuzzles.KNOWN_PUZZLE)
        assertTrue(board.conflictingCells().isEmpty())
        // Put a 5 next to the given 5 at index 0 (row 0)
        board = board.with(2, Cell(value = 5))
        val conflicts = board.conflictingCells()
        assertTrue(0 in conflicts)
        assertTrue(2 in conflicts)
    }

    @Test
    fun withReplacesSingleCell() {
        val board = Board.empty().with(40, Cell(value = 7))
        assertEquals(7, board[40].value)
        assertEquals(0, board[39].value)
    }
}
