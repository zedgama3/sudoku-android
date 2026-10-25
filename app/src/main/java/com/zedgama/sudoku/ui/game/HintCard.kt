package com.zedgama.sudoku.ui.game

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.engine.Technique

/**
 * The hint banner. Plain text for most tiers; when [HintUi.techniques] is non-empty, the
 * technique names render as clickable chips (in order) that open the full-screen explainer.
 * Earlier steps of the same walkthrough sit below the current one, newest first and dimmed;
 * the card scrolls rather than growing past [MAX_HEIGHT]. [onFlag] (dev hint logging) adds a
 * flag button in the corner.
 */
@Composable
fun HintCard(
    hint: HintUi,
    onTechniqueClick: (Technique) -> Unit,
    onFlag: (() -> Unit)? = null
) {
    val onCard = MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.heightIn(max = MAX_HEIGHT)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                hint.progress?.let { (step, of) ->
                    Text(
                        "Step $step of $of",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    hint.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCard,
                    textAlign = TextAlign.Center,
                    // keep clear of the flag button
                    modifier = if (onFlag != null) Modifier.padding(horizontal = 28.dp) else Modifier
                )
                if (hint.techniques.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        hint.techniques.forEachIndexed { index, technique ->
                            if (index > 0) {
                                Text(
                                    "→",
                                    color = onCard,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                            AssistChip(
                                onClick = { onTechniqueClick(technique) },
                                label = { Text(technique.label) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = onCard)
                            )
                        }
                    }
                }
                hint.previous.withIndex().reversed().forEach { (index, text) ->
                    HorizontalDivider(
                        color = onCard.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        "Step ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard.copy(alpha = PREVIOUS_ALPHA)
                    )
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = onCard.copy(alpha = PREVIOUS_ALPHA),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (onFlag != null) {
                IconButton(
                    onClick = onFlag,
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = "Flag this hint",
                        tint = onCard.copy(alpha = PREVIOUS_ALPHA),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private val MAX_HEIGHT = 220.dp
private const val PREVIOUS_ALPHA = 0.6f
