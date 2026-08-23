package com.zedgama.sudoku.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.zedgama.sudoku.data.model.DifficultyStats
import com.zedgama.sudoku.data.model.GameStats
import com.zedgama.sudoku.engine.Difficulty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import java.io.File

class StatsRepository(scope: CoroutineScope, context: Context) {

    private val store: DataStore<GameStats> = DataStoreFactory.create(
        serializer = JsonSerializer(GameStats.serializer(), GameStats()),
        scope = CoroutineScope(scope.coroutineContext + SupervisorJob() + Dispatchers.IO),
        produceFile = { File(context.filesDir, "stats.json") }
    )

    val stats: Flow<GameStats> = store.data

    suspend fun recordGameStarted(difficulty: Difficulty) {
        update(difficulty) { it.copy(played = it.played + 1) }
    }

    /** Records a win; returns true when [timeMillis] is a new personal best. */
    suspend fun recordWin(difficulty: Difficulty, timeMillis: Long): Boolean {
        var newBest = false
        update(difficulty) { s ->
            newBest = s.bestTimeMillis == null || timeMillis < s.bestTimeMillis
            val streak = s.currentStreak + 1
            s.copy(
                won = s.won + 1,
                bestTimeMillis = if (newBest) timeMillis else s.bestTimeMillis,
                totalTimeMillisWon = s.totalTimeMillisWon + timeMillis,
                currentStreak = streak,
                bestStreak = maxOf(s.bestStreak, streak)
            )
        }
        return newBest
    }

    suspend fun recordLoss(difficulty: Difficulty) {
        update(difficulty) { it.copy(currentStreak = 0) }
    }

    private suspend fun update(
        difficulty: Difficulty,
        transform: (DifficultyStats) -> DifficultyStats
    ) {
        store.updateData { stats ->
            val current = stats.forDifficulty(difficulty)
            stats.copy(perDifficulty = stats.perDifficulty + (difficulty to transform(current)))
        }
    }
}
