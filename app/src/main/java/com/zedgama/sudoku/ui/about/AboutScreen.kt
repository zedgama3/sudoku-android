package com.zedgama.sudoku.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zedgama.sudoku.BuildConfig
import com.zedgama.sudoku.R

private const val SOURCE_URL = "https://github.com/zedgama3/sudoku-android"
private const val DEVELOPER = "Jared Fowkes"
private const val EMAIL = "zedgama3@gmail.com"

/** App version and build, who made it, where the source lives, and the licenses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    // "-dirty" builds have no matching commit to link to.
    val commit = BuildConfig.GIT_SHA
    val commitUrl = commit.takeIf { it != "unknown" && !it.endsWith("-dirty") }
        ?.let { "$SOURCE_URL/commit/$it" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "No ads, no tracking, no network access. Everything stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionLabel("Build")
            InfoRow(
                "Version",
                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.BUILD_TYPE}"
            )
            InfoRow("Commit", commit, url = commitUrl)

            SectionLabel("Developer")
            InfoRow("Made by", DEVELOPER)
            InfoRow("Email", EMAIL, url = "mailto:$EMAIL")

            SectionLabel("Source code")
            InfoRow(
                "Open source on GitHub. Bug reports and pull requests are welcome",
                SOURCE_URL.removePrefix("https://"),
                url = SOURCE_URL
            )

            SectionLabel("Licenses")
            InfoRow(
                "This app",
                "Mozilla Public License 2.0",
                url = "$SOURCE_URL/blob/main/LICENSE"
            )
            InfoRow(
                "AndroidX, Jetpack Compose and Kotlin libraries",
                "Apache License 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
    )
}

/** A caption over a value; the value is shown as a link and opens [url] when one is given. */
@Composable
private fun InfoRow(caption: String, value: String, url: String? = null) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (url != null) {
                    // No browser or mail app installed: nothing sensible to do, so ignore the tap.
                    Modifier.clickable { runCatching { uriHandler.openUri(url) } }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp)
    ) {
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (url != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
