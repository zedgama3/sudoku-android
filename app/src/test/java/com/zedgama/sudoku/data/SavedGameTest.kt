package com.zedgama.sudoku.data

import com.zedgama.sudoku.data.model.BoardSnapshot
import com.zedgama.sudoku.data.model.SavedGame
import com.zedgama.sudoku.data.model.toBoard
import com.zedgama.sudoku.data.model.toSnapshot
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.engine.TestPuzzles
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedGameTest {

    private fun sample(): SavedGame {
        val notes = MutableList(81) { 0 }
        notes[3] = 0b101 // notes 1 and 3
        return SavedGame(
            givens = TestPuzzles.KNOWN_PUZZLE,
            solution = TestPuzzles.KNOWN_SOLUTION,
            current = TestPuzzles.KNOWN_PUZZLE,
            notes = notes,
            difficulty = Difficulty.HARD,
            elapsedMillis = 123_000,
            mistakes = 2,
            hintsUsed = 1,
            undoStack = listOf(BoardSnapshot(TestPuzzles.KNOWN_PUZZLE, List(81) { 0 }))
        )
    }

    @Test
    fun jsonRoundTrip() {
        val game = sample()
        val json = Json.encodeToString(SavedGame.serializer(), game)
        val decoded = Json.decodeFromString(SavedGame.serializer(), json)
        assertEquals(game, decoded)
    }

    @Test
    fun boardRoundTrip() {
        val game = sample()
        val board = game.toBoard()
        assertEquals(5, board[0].value)
        assertTrue(board[0].isGiven)
        assertEquals(0b101, board[3].notes)
        val snapshot = board.toSnapshot()
        assertEquals(game.current, snapshot.values)
        assertEquals(game.notes, snapshot.notes)
    }
}
