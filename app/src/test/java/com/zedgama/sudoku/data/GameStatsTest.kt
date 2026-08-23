package com.zedgama.sudoku.data

import com.zedgama.sudoku.data.model.DifficultyStats
import com.zedgama.sudoku.data.model.GameStats
import com.zedgama.sudoku.engine.Difficulty
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStatsTest {

    @Test
    fun jsonRoundTripWithEnumKeys() {
        val stats = GameStats(
            perDifficulty = mapOf(
                Difficulty.EASY to DifficultyStats(played = 3, won = 2, bestTimeMillis = 90_000),
                Difficulty.EXPERT to DifficultyStats(played = 1)
            )
        )
        val json = Json.encodeToString(GameStats.serializer(), stats)
        assertEquals(stats, Json.decodeFromString(GameStats.serializer(), json))
    }

    @Test
    fun averageAndDefaults() {
        val stats = GameStats()
        assertEquals(DifficultyStats(), stats.forDifficulty(Difficulty.HARD))
        assertNull(DifficultyStats().averageTimeMillis)
        assertEquals(
            50L,
            DifficultyStats(won = 2, totalTimeMillisWon = 100).averageTimeMillis
        )
    }
}
