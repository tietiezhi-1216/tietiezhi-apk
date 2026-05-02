package com.tietiezhi.apk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val state = combine(ds.serverAddress, ds.apiKey, ds.modelName, ds.streaming, ds.themeMode, ds.localMode, ds.localPort) {
        s, k, m, st, t, l, p -> SettingsState(s, k, m, st, t, l, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setServer(v: String) { viewModelScope.launch { ds.setServer(v) } }
    fun setApiKey(v: String) { viewModelScope.launch { ds.setApiKey(v) } }
    fun setModel(v: String) { viewModelScope.launch { ds.setModel(v) } }
    fun setStreaming(v: Boolean) { viewModelScope.launch { ds.setStreaming(v) } }
    fun setTheme(v: Int) { viewModelScope.launch { ds.setTheme(v) } }
    fun setLocalMode(v: Boolean) { viewModelScope.launch { ds.setLocalMode(v) } }
    fun setLocalPort(v: Int) { viewModelScope.launch { ds.setLocalPort(v) } }
}
