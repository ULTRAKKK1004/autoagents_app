package com.autoagents.app.ui.settings

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
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoagents.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.settings_saved)

    LaunchedEffect(ui.savedSnack) {
        ui.savedSnack?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSnack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_llm_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = ui.settings.endpoint,
                onValueChange = { v -> viewModel.edit { it.copy(endpoint = v) } },
                label = { Text(stringResource(R.string.settings_endpoint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ui.settings.token,
                onValueChange = { v -> viewModel.edit { it.copy(token = v) } },
                label = { Text(stringResource(R.string.settings_token)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ui.settings.model,
                onValueChange = { v -> viewModel.edit { it.copy(model = v) } },
                label = { Text(stringResource(R.string.settings_model)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ui.settings.ytModel,
                onValueChange = { v -> viewModel.edit { it.copy(ytModel = v) } },
                label = { Text(stringResource(R.string.settings_yt_model)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ui.settings.ytSummaryApi,
                onValueChange = { v -> viewModel.edit { it.copy(ytSummaryApi = v) } },
                label = { Text(stringResource(R.string.settings_yt_api)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ui.settings.keywords,
                onValueChange = { v -> viewModel.edit { it.copy(keywords = v) } },
                label = { Text(stringResource(R.string.settings_keywords)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.save(savedMessage) }
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(stringResource(R.string.settings_save))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.reset() }
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(stringResource(R.string.settings_reset))
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.isTesting,
                onClick = { viewModel.testConnection() }
            ) {
                if (ui.isTesting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text("연결 테스트 중…")
                } else {
                    Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(stringResource(R.string.settings_test_connection))
                }
            }
            ui.testResult?.let {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(it, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
