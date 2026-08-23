package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: (() -> Unit)? = null,
    onNewGame: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameTopBar(
                difficulty = state.difficulty,
                mistakes = state.mistakes,
                mistakeChecking = state.mistakeChecking,
                elapsedMillis = state.elapsedMillis,
                showTimer = state.showTimer,
                paused = state.paused,
                onBack = onBack,
                onTogglePause = viewModel::onTogglePause
            )

            Spacer(Modifier.height(8.dp))

            BoardCanvas(
                board = state.board,
                selectedCell = state.selectedCell,
                errorCells = state.errorCells,
                highlightDigit = state.highlightDigit,
                hintCells = state.hint?.cells ?: emptyList(),
                paused = state.paused,
                onCellTap = viewModel::onCellTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            // Hint banner / paused notice
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                when {
                    state.paused -> Text(
                        "Paused",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.hint != null -> Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            state.hint!!.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    else -> Spacer(Modifier.height(32.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::onUndo, enabled = state.canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = viewModel::onRedo, enabled = state.canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = viewModel::onErase) {
                    Icon(Icons.Filled.Backspace, contentDescription = "Erase")
                }
                FilledIconToggleButton(
                    checked = state.notesMode,
                    onCheckedChange = { viewModel.onToggleNotes() }
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Notes mode")
                }
                IconButton(onClick = viewModel::onHint) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = "Hint")
                }
            }

            Spacer(Modifier.height(8.dp))

            NumberPad(
                board = state.board,
                armedDigit = state.armedDigit,
                onDigit = viewModel::onDigit
            )

            Spacer(Modifier.height(12.dp))
        }

        if (state.isWon) {
            WinDialog(
                difficulty = state.difficulty,
                elapsedMillis = state.elapsedMillis,
                mistakes = state.mistakes,
                hintsUsed = state.hintsUsed,
                isNewBest = false,
                onNewGame = { onNewGame?.invoke() },
                onHome = onBack
            )
        }
    }
}
