package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.engine.Board

/** Digit buttons; a long-press does the opposite of a tap (enter vs mark) in cell-first mode. */
@Composable
fun NumberPad(
    board: Board,
    armedDigit: Int?,
    onDigit: (Int) -> Unit,
    onDigitLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = IntArray(10)
    for (i in 0 until 81) counts[board[i].value]++
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (d in 1..9) {
            val remaining = 9 - counts[d]
            val armed = d == armedDigit
            val enabled = remaining > 0 || armed
            // Surface + combinedClickable rather than Button, which has no long-click.
            val container = when {
                armed -> colors.primary
                enabled -> colors.secondaryContainer
                else -> colors.onSurface.copy(alpha = 0.12f)
            }
            val content = when {
                armed -> colors.onPrimary
                enabled -> colors.onSecondaryContainer
                else -> colors.onSurface.copy(alpha = 0.38f)
            }
            Surface(
                color = container,
                contentColor = content,
                shape = ButtonDefaults.shape,
                modifier = Modifier
                    .weight(1f)
                    .clip(ButtonDefaults.shape)
                    .combinedClickable(
                        enabled = enabled,
                        onClick = { onDigit(d) },
                        onLongClick = { onDigitLongPress(d) }
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(d.toString(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (remaining > 0) remaining.toString() else " ",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
