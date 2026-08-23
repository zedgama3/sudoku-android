package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.zedgama.sudoku.engine.Difficulty

@Composable
fun WinDialog(
    difficulty: Difficulty,
    elapsedMillis: Long,
    mistakes: Int,
    hintsUsed: Int,
    isNewBest: Boolean,
    onNewGame: () -> Unit,
    onHome: (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Puzzle solved!") },
        text = {
            Column {
                Text("Difficulty: ${difficultyLabel(difficulty)}")
                Text("Time: ${formatElapsed(elapsedMillis)}")
                Text("Mistakes: $mistakes")
                Text("Hints used: $hintsUsed")
                if (isNewBest) {
                    Text(
                        "New personal best!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewGame) { Text("New Game") }
        },
        dismissButton = {
            if (onHome != null) {
                TextButton(onClick = onHome) { Text("Home") }
            }
        }
    )
}
