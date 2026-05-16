package com.autoagents.app.ui.youtube

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoagents.app.R
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeScreen(
    initialUrl: String?,
    viewModel: YoutubeViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsState()
    var input by remember { mutableStateOf(initialUrl?.takeIf { it.contains("youtu", ignoreCase = true) }.orEmpty()) }

    LaunchedEffect(initialUrl) {
        val url = initialUrl?.takeIf { it.contains("youtu", ignoreCase = true) }
        if (!url.isNullOrBlank()) {
            input = url
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.youtube_title), fontWeight = FontWeight.SemiBold) })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(stringResource(R.string.youtube_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                enabled = !ui.isProcessing && input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.run(input) }
            ) {
                Icon(Icons.Filled.PlayCircle, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(stringResource(R.string.youtube_run))
            }
            if (ui.isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(ui.message ?: stringResource(R.string.youtube_processing))
                }
            } else if (!ui.message.isNullOrBlank()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        ui.message!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
            val summary = ui.summary
            if (!summary.isNullOrBlank()) {
                Text("요약 결과", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MarkdownText(
                    markdown = summary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!ui.isProcessing && ui.message.isNullOrBlank()) {
                Text(stringResource(R.string.youtube_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
