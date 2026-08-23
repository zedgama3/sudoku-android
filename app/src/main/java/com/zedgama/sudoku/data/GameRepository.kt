package com.zedgama.sudoku.data

import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.engine.GeneratedPuzzle
import com.zedgama.sudoku.engine.Generator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Hands out puzzles, keeping one pre-generated puzzle per difficulty warm
 * so "New Game" is instant.
 */
class GameRepository(private val scope: CoroutineScope) {

    private val generator = Generator()
    private val cache = ConcurrentHashMap<Difficulty, GeneratedPuzzle>()

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
