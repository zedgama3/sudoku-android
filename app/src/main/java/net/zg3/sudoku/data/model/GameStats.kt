package net.zg3.sudoku.data.model

import net.zg3.sudoku.engine.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeMillis: Long? = null,
    val totalTimeMillisWon: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    val averageTimeMillis: Long? get() = if (won > 0) totalTimeMillisWon / won else null
}

@Serializable
data class GameStats(
    val perDifficulty: Map<Difficulty, DifficultyStats> = emptyMap()
) {
    fun forDifficulty(d: Difficulty): DifficultyStats =
        perDifficulty[d] ?: DifficultyStats()
}
