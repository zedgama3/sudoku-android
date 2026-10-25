package net.zg3.sudoku.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import net.zg3.sudoku.data.model.SavedGame
import net.zg3.sudoku.engine.Difficulty
import net.zg3.sudoku.engine.GeneratedPuzzle
import net.zg3.sudoku.engine.Generator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.nullable
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Hands out puzzles (one pre-generated per difficulty kept warm so "New Game"
 * is instant) and owns the saved-game store.
 */
class GameRepository(
    private val scope: CoroutineScope,
    context: Context
) {

    private val generator = Generator()
    private val cache = ConcurrentHashMap<Difficulty, GeneratedPuzzle>()

    private val savedGameStore: DataStore<SavedGame?> = DataStoreFactory.create(
        serializer = JsonSerializer(SavedGame.serializer().nullable, null),
        scope = CoroutineScope(scope.coroutineContext + SupervisorJob() + Dispatchers.IO),
        produceFile = { File(context.filesDir, "saved_game.json") }
    )

    val savedGameFlow: Flow<SavedGame?> = savedGameStore.data

    suspend fun loadSavedGame(): SavedGame? = savedGameStore.data.first()

    suspend fun saveGame(game: SavedGame) {
        savedGameStore.updateData { game }
    }

    suspend fun clearSavedGame() {
        savedGameStore.updateData { null }
    }

    init {
        Difficulty.entries.forEach(::refill)
    }

    suspend fun nextPuzzle(difficulty: Difficulty): GeneratedPuzzle {
        val cached = cache.remove(difficulty)
        if (cached != null) {
            refill(difficulty)
            return cached
        }
        val puzzle = withContext(Dispatchers.Default) { generator.generate(difficulty) }
        refill(difficulty)
        return puzzle
    }

    private fun refill(difficulty: Difficulty) {
        if (cache.containsKey(difficulty)) return
        scope.launch(Dispatchers.Default) {
            cache.putIfAbsent(difficulty, generator.generate(difficulty))
        }
    }
}
