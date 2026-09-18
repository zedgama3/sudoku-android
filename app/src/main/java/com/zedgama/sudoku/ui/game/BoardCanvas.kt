package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.HintHighlight
import com.zedgama.sudoku.engine.boxOf
import com.zedgama.sudoku.engine.colOf
import com.zedgama.sudoku.engine.rowOf

@Composable
fun BoardCanvas(
    board: Board,
    selectedCell: Int?,
    errorCells: Set<Int>,
    highlightDigit: Int?,
    paused: Boolean,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: HintHighlight? = null
) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)

    Canvas(
        modifier = modifier.pointerInput(paused) {
            if (!paused) {
                detectTapGestures { offset ->
                    val cell = size.width / 9f
                    val col = (offset.x / cell).toInt().coerceIn(0, 8)
                    val row = (offset.y / cell).toInt().coerceIn(0, 8)
                    onCellTap(row * 9 + col)
                }
            }
        }
    ) {
        val cell = size.width / 9f
        val bold = TextStyle(fontSize = (cell * 0.55f).toSp(), fontWeight = FontWeight.Bold)
        val normal = TextStyle(fontSize = (cell * 0.55f).toSp(), fontWeight = FontWeight.Normal)
        val noteStyle = TextStyle(fontSize = (cell * 0.24f).toSp())

        fun cellRect(index: Int): Offset =
            Offset(colOf(index) * cell, rowOf(index) * cell)

        // Backgrounds
        if (!paused) {
            val selRow = selectedCell?.let(::rowOf)
            val selCol = selectedCell?.let(::colOf)
            val selBox = selectedCell?.let(::boxOf)
            val hintPrimary = hint?.primary?.toSet()
            val hintSecondary = hint?.secondary?.toSet()
            val hintDigit = hint?.digit

            // Cell fill, first match wins:
            //   1. error                errorContainer          (a wrong entry must never be hidden)
            //   2. hint primary         tertiary  @ 0.40
            //   3. hint secondary       tertiary  @ 0.18        (same hue as primary, softer; must stay
            //                                                    distinct from the gray selection shading)
            //   4. hint digit instance  secondaryContainer      (board[i].value == hint.digit; reads like
            //                                                    the same-digit highlight the player knows)
            //   5. selected cell        primaryContainer
            //   6. same digit           secondaryContainer      (armed / selected digit)
            //   7. selected row/col/box surfaceVariant @ 0.55
            // Then the selected cell always gets a primary-colored inset stroke on top, so selecting a
            // hinted cell keeps its hint tint and still reads as selected.
            for (i in 0 until 81) {
                val bg: Color? = when {
                    i in errorCells -> colors.errorContainer
                    hintPrimary != null && i in hintPrimary -> colors.tertiary.copy(alpha = 0.40f)
                    hintSecondary != null && i in hintSecondary ->
                        colors.tertiary.copy(alpha = 0.18f)
                    hintDigit != null && board[i].value == hintDigit ->
                        colors.secondaryContainer
                    i == selectedCell -> colors.primaryContainer
                    highlightDigit != null && board[i].value == highlightDigit ->
                        colors.secondaryContainer
                    selectedCell != null &&
                        (rowOf(i) == selRow || colOf(i) == selCol || boxOf(i) == selBox) ->
                        colors.surfaceVariant.copy(alpha = 0.55f)
                    else -> null
                }
                if (bg != null) {
                    drawRect(bg, topLeft = cellRect(i), size = Size(cell, cell))
                }
            }

            if (selectedCell != null) {
                val inset = 1.dp.toPx()
                val origin = cellRect(selectedCell)
                drawRect(
                    colors.primary,
                    topLeft = Offset(origin.x + inset, origin.y + inset),
                    size = Size(cell - 2 * inset, cell - 2 * inset),
                    style = Stroke(2.dp.toPx())
                )
            }
        }

        // Grid lines
        val thin = 1.dp.toPx()
        val thick = 2.5.dp.toPx()
        for (i in 0..9) {
            val isThick = i % 3 == 0
            val width = if (isThick) thick else thin
            val color = if (isThick) colors.onSurface else colors.outline.copy(alpha = 0.5f)
            val pos = i * cell
            drawLine(color, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = width)
            drawLine(color, Offset(0f, pos), Offset(size.width, pos), strokeWidth = width)
        }

        if (paused) return@Canvas

        // Digits and notes
        for (i in 0 until 81) {
            val c = board[i]
            val origin = cellRect(i)
            if (c.value != 0) {
                val style = when {
                    i in errorCells -> normal.copy(color = colors.error)
                    c.isGiven -> bold.copy(color = colors.onSurface)
                    else -> normal.copy(color = colors.primary)
                }
                val layout = textMeasurer.measure(c.value.toString(), style)
                drawText(
                    layout,
                    topLeft = Offset(
                        origin.x + (cell - layout.size.width) / 2f,
                        origin.y + (cell - layout.size.height) / 2f
                    )
                )
            } else if (c.notes != 0) {
                val third = cell / 3f
                for (d in 1..9) {
                    if (c.notes and (1 shl (d - 1)) == 0) continue
                    val style = if (d == highlightDigit) {
                        noteStyle.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                    } else {
                        noteStyle.copy(color = colors.onSurfaceVariant)
                    }
                    val layout = textMeasurer.measure(d.toString(), style)
                    val nx = origin.x + ((d - 1) % 3) * third + (third - layout.size.width) / 2f
                    val ny = origin.y + ((d - 1) / 3) * third + (third - layout.size.height) / 2f
                    drawText(layout, topLeft = Offset(nx, ny))
                }
            }
        }
    }
}
