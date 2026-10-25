package net.zg3.sudoku.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.zg3.sudoku.R
import net.zg3.sudoku.data.model.InputMode
import net.zg3.sudoku.data.model.TapAction
import net.zg3.sudoku.ui.settings.SettingsViewModel

/**
 * Shown once, on first launch: the few settings that change how the board responds to a tap, so a
 * new player isn't surprised by the defaults. Choices are saved as they're made.
 */
@Composable
fun SetupScreen(viewModel: SettingsViewModel, onDone: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Welcome to ${stringResource(R.string.app_name)}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 32.dp)
            )
            Text(
                "Pick how you like to play. You can change these anytime in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Question(
                "Entering numbers",
                listOf(
                    InputMode.CELL_FIRST to Choice("Pick a cell, then a number", "Tap a square, then tap a number below"),
                    InputMode.DIGIT_FIRST to Choice("Pick a number, then cells", "Choose a number below, then tap every square it goes in")
                ),
                settings.inputMode,
                viewModel::setInputMode
            )
            Question(
                "A quick tap…",
                listOf(
                    TapAction.ENTER to Choice("Enters the number", "Long-press to add a pencil note instead"),
                    TapAction.MARK to Choice("Adds a pencil note", "Long-press to enter the number instead")
                ),
                settings.tapAction,
                viewModel::setTapAction
            )
            Question(
                "Mistakes",
                listOf(
                    true to Choice("Show them right away", "Wrong numbers are highlighted as soon as you enter them"),
                    false to Choice("Let me find them", "Nothing is flagged; a hint will point out mistakes if you ask")
                ),
                settings.mistakeChecking,
                viewModel::setMistakeChecking
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.completeSetup()
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) { Text("Start playing") }
        }
    }
}

private data class Choice(val title: String, val detail: String)

@Composable
private fun <T> Question(
    title: String,
    options: List<Pair<T, Choice>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { (value, choice) ->
            val isSelected = value == selected
            OutlinedCard(
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(value) }
                        .padding(12.dp)
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(choice.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            choice.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
