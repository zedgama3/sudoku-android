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
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.heightIn
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zedgama.sudoku.data.model.TapAction
import com.zedgama.sudoku.engine.Technique

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: (() -> Unit)? = null,
    onNewGame: (() -> Unit)? = null,
    onOpenTechnique: ((Technique) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onForeground()
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                mistakeLimit = state.mistakeLimit,
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
                hint = state.hint?.highlight,
                paused = state.paused,
                onCellTap = viewModel::onCellTap,
                onCellLongPress = { index ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onCellLongPress(index)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(Modifier.height(8.dp))

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
                // Swap toggle: while on, tap and long-press trade actions. It shows what a tap
                // switches *to*, so with tap=Mark it reads as "Enter" and vice versa.
                FilledIconToggleButton(
                    checked = state.notesMode,
                    onCheckedChange = { viewModel.onToggleNotes() }
                ) {
                    if (state.tapAction == TapAction.MARK) {
                        Icon(Icons.Filled.Pin, contentDescription = "Tap enters numbers")
                    } else {
                        Icon(Icons.Filled.Edit, contentDescription = "Tap marks notes")
                    }
                }
                IconButton(onClick = viewModel::onHint) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = "Hint")
                }
            }

            Spacer(Modifier.height(8.dp))

            NumberPad(
                board = state.board,
                armedDigit = state.armedDigit,
                onDigit = { d ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onDigit(d)
                },
                onDigitLongPress = { d ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onDigitLongPress(d)
                }
            )

            Spacer(Modifier.height(8.dp))

            // Hint banner / paused notice, anchored to the bottom of the screen so showing or
            // hiding it never moves the board, toolbar or number pad.
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.paused -> Text(
                        "Paused",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.hint != null -> HintCard(
                        hint = state.hint!!,
                        onTechniqueClick = { technique ->
                            viewModel.onOpenExplainer()
                            onOpenTechnique?.invoke(technique)
                        },
                        onFlag = if (state.hintLogging) viewModel::onFlagHint else null
                    )
                }
            }
            state.flagNotice?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
        }

        if (state.isWon) {
            WinDialog(
                difficulty = state.difficulty,
                elapsedMillis = state.elapsedMillis,
                mistakes = state.mistakes,
                hintsUsed = state.hintsUsed,
                isNewBest = state.isNewBest,
                onNewGame = { onNewGame?.invoke() },
                onHome = onBack
            )
        }

        if (state.isFailed) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {},
                title = { Text("Out of mistakes") },
                text = { Text("You've made ${state.mistakes} mistakes. Try this puzzle again or start a new one.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = viewModel::onRetry) {
                        Text("Retry")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { onNewGame?.invoke() }) {
                        Text("New Game")
                    }
                }
            )
        }
    }
}
