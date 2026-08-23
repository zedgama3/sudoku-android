package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.engine.Difficulty
import java.util.Locale

fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

fun difficultyLabel(d: Difficulty): String =
    d.name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun GameTopBar(
    difficulty: Difficulty,
    mistakes: Int,
    mistakeChecking: Boolean,
    mistakeLimit: Int?,
    elapsedMillis: Long,
    showTimer: Boolean,
    paused: Boolean,
    onBack: (() -> Unit)?,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            difficultyLabel(difficulty),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        if (mistakeChecking) {
            Text(
                if (mistakeLimit != null) "Mistakes: $mistakes/$mistakeLimit"
                else "Mistakes: $mistakes",
                style = MaterialTheme.typography.bodyMedium,
                color = if (mistakes > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        if (showTimer) {
            Text(
                formatElapsed(elapsedMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onTogglePause) {
                Icon(
                    if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (paused) "Resume" else "Pause"
                )
            }
        }
    }
}
