package com.zedgama.sudoku.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.engine.Difficulty
import com.zedgama.sudoku.ui.game.difficultyLabel
import com.zedgama.sudoku.ui.game.formatElapsed

data class ContinueInfo(val difficulty: Difficulty, val elapsedMillis: Long)

@Composable
fun HomeScreen(
    continueInfo: ContinueInfo?,
    onContinue: () -> Unit,
    onNewGame: (Difficulty) -> Unit,
    onStats: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (onStats != null) {
                IconButton(onClick = onStats) {
                    Icon(Icons.Filled.BarChart, contentDescription = "Statistics")
                }
            }
            if (onSettings != null) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        Text("Sudoku", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(48.dp))

        if (continueInfo != null) {
            Card(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Continue", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${difficultyLabel(continueInfo.difficulty)} · " +
                                formatElapsed(continueInfo.elapsedMillis),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Text(
            "New Game",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        for (difficulty in Difficulty.entries) {
            Button(
                onClick = { onNewGame(difficulty) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(difficultyLabel(difficulty), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
