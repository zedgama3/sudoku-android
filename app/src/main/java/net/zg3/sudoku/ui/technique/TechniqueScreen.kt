package net.zg3.sudoku.ui.technique

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.zg3.sudoku.engine.HintHighlight
import net.zg3.sudoku.engine.Technique
import net.zg3.sudoku.ui.game.BoardCanvas

/** Full-screen explainer for one solving technique, opened from the hint card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueScreen(technique: Technique, onBack: () -> Unit) {
    val info = techniqueInfo(technique)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(technique.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("What it is")
            Text(info.whatItIs, style = MaterialTheme.typography.bodyMedium)

            SectionLabel("How to spot it")
            Text(info.howToSpot, style = MaterialTheme.typography.bodyMedium)

            SectionLabel("Example")
            BoardCanvas(
                board = info.example.board,
                selectedCell = null,
                errorCells = emptySet(),
                highlightDigit = info.example.highlightDigit,
                paused = false,
                onCellTap = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                hint = HintHighlight(
                    primary = info.example.hintCells,
                    secondary = info.example.secondaryCells
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                info.example.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}
