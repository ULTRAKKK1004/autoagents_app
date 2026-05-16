package com.autoagents.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoagents.app.App
import com.autoagents.app.data.prefs.LlmSettings
import com.autoagents.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: LlmSettings = SettingsRepository.defaultSettings(),
    val savedSnack: String? = null,
    val testResult: String? = null,
    val isTesting: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val app = App.get()
    private val repo = app.settings

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui

    init {
        viewModelScope.launch {
            repo.settings.collect { s -> _ui.value = _ui.value.copy(settings = s) }
        }
    }

    fun edit(transform: (LlmSettings) -> LlmSettings) {
        _ui.value = _ui.value.copy(settings = transform(_ui.value.settings))
    }

    fun save(message: String) {
        val cur = _ui.value.settings
        viewModelScope.launch {
            repo.update { cur }
            _ui.value = _ui.value.copy(savedSnack = message)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repo.resetDefaults()
            _ui.value = _ui.value.copy(savedSnack = "기본값으로 복원되었습니다.")
        }
    }

    fun consumeSnack() {
        _ui.value = _ui.value.copy(savedSnack = null)
    }

    fun testConnection() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isTesting = true, testResult = null)
            val result = app.llmClient.pingConnection()
            _ui.value = _ui.value.copy(isTesting = false, testResult = result)
        }
    }
}
