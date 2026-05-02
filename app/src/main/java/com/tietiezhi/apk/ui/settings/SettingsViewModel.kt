package com.tietiezhi.apk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val serverAddress: String = "http://localhost:18178",
    val apiKey: String = "",
    val modelName: String = "default",
    val streaming: Boolean = true,
    val themeMode: Int = 0,
    val localMode: Boolean = true,
    val localPort: Int = 18178
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val ds: SettingsDataStore) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ds.serverAddress.collect { _state.update { s -> s.copy(serverAddress = it) } }
        }
        viewModelScope.launch {
            ds.apiKey.collect { _state.update { s -> s.copy(apiKey = it) } }
        }
        viewModelScope.launch {
            ds.modelName.collect { _state.update { s -> s.copy(modelName = it) } }
        }
        viewModelScope.launch {
            ds.streaming.collect { _state.update { s -> s.copy(streaming = it) } }
        }
        viewModelScope.launch {
            ds.themeMode.collect { _state.update { s -> s.copy(themeMode = it) } }
        }
        viewModelScope.launch {
            ds.localMode.collect { _state.update { s -> s.copy(localMode = it) } }
        }
        viewModelScope.launch {
            ds.localPort.collect { _state.update { s -> s.copy(localPort = it) } }
        }
    }

    fun setServer(v: String) { viewModelScope.launch { ds.setServer(v) } }
    fun setApiKey(v: String) { viewModelScope.launch { ds.setApiKey(v) } }
    fun setModel(v: String) { viewModelScope.launch { ds.setModel(v) } }
    fun setStreaming(v: Boolean) { viewModelScope.launch { ds.setStreaming(v) } }
    fun setTheme(v: Int) { viewModelScope.launch { ds.setTheme(v) } }
    fun setLocalMode(v: Boolean) { viewModelScope.launch { ds.setLocalMode(v) } }
    fun setLocalPort(v: Int) { viewModelScope.launch { ds.setLocalPort(v) } }
}
