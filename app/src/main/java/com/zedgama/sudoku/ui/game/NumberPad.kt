package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.engine.Board

@Composable
fun NumberPad(
    board: Board,
    armedDigit: Int?,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = IntArray(10)
    for (i in 0 until 81) counts[board[i].value]++

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (d in 1..9) {
            val remaining = 9 - counts[d]
            val armed = d == armedDigit
            Button(
                onClick = { onDigit(d) },
                enabled = remaining > 0 || armed,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 0.dp, vertical = 8.dp
                ),
                colors = if (armed) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.toString(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (remaining > 0) remaining.toString() else " ",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 0.dp)
                    )
                }
            }
        }
    }
}
