package com.zedgama.sudoku.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zedgama.sudoku.BuildConfig
import com.zedgama.sudoku.data.model.AutoSelect
import com.zedgama.sudoku.data.model.InputMode
import com.zedgama.sudoku.data.model.TapAction
import com.zedgama.sudoku.data.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            SectionLabel("Appearance")
            Text("Theme", style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { viewModel.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                    ) {
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            SectionLabel("Gameplay")
            Text("Input mode", style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = settings.inputMode == InputMode.CELL_FIRST,
                    onClick = { viewModel.setInputMode(InputMode.CELL_FIRST) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Cell first") }
                SegmentedButton(
                    selected = settings.inputMode == InputMode.DIGIT_FIRST,
                    onClick = { viewModel.setInputMode(InputMode.DIGIT_FIRST) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Digit first") }
            }

            ChoiceRow(
                title = "Tap action",
                subtitle = "A long-press does the other",
                options = listOf(TapAction.MARK to "Mark", TapAction.ENTER to "Enter"),
                selected = settings.tapAction,
                onSelect = viewModel::setTapAction
            )
            if (settings.inputMode == InputMode.DIGIT_FIRST) {
                ChoiceRow(
                    title = "Select a matching cell when choosing a number",
                    subtitle = "Moves the selection to the nearest cell with that number. " +
                        "\"When filled\" only moves it off a cell that holds a different number",
                    options = listOf(
                        AutoSelect.ALWAYS to "Always",
                        AutoSelect.WHEN_FILLED to "When filled",
                        AutoSelect.NEVER to "Never"
                    ),
                    selected = settings.autoSelect,
                    onSelect = viewModel::setAutoSelect
                )
            }
            SwitchRow(
                "Allow overwriting entries",
                "Entering a number replaces the one already there. When off, erase it first",
                settings.allowOverwrite,
                viewModel::setAllowOverwrite
            )
            SwitchRow(
                "Tap removes a mark",
                "Marking a number that's already marked removes it. When off, marks only come off with erase",
                settings.tapRemovesMark,
                viewModel::setTapRemovesMark
            )

            SwitchRow(
                "Mistake checking",
                "Highlight entries that don't match the solution. When off, hints can find mistakes for you",
                settings.mistakeChecking,
                viewModel::setMistakeChecking
            )
            SwitchRow(
                "Mistake limit",
                "End the game after 3 mistakes",
                settings.mistakeLimitEnabled,
                viewModel::setMistakeLimit
            )
            SwitchRow(
                "Show timer",
                "Display elapsed time while playing",
                settings.showTimer,
                viewModel::setShowTimer
            )
            SwitchRow(
                "Auto-remove notes",
                "Clear notes in the row, column, and box when a digit is placed",
                settings.autoRemoveNotes,
                viewModel::setAutoRemoveNotes
            )

            if (BuildConfig.DEBUG) {
                SectionLabel("Developer")
                SwitchRow(
                    "Hint logging",
                    "Record each hint with its board (up to 10 MB). Pull with `task hintlog`",
                    settings.hintLogging,
                    viewModel::setHintLogging
                )
                TextButton(onClick = { viewModel.clearHintLog() }) { Text("Clear hint log") }
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    subtitle: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(Modifier.padding(top = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) { Text(label) }
            }
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

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
