package com.tietiezhi.apk.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import com.tietiezhi.apk.data.remote.api.ManagementApi
import com.tietiezhi.apk.data.remote.dto.management.ConfigUpdateRequest
import com.tietiezhi.apk.data.remote.dto.management.LlmConfigUpdate
import com.tietiezhi.apk.data.remote.interceptor.AuthInterceptor
import com.tietiezhi.apk.data.remote.interceptor.BaseUrlInterceptor
import com.tietiezhi.apk.server.TietiezhiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val serverAddress: String = "http://localhost:18178",
    val apiKey: String = "",
    val modelName: String = "default",
    val cheapModel: String = "",
    val llmBaseUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "",
    val streaming: Boolean = true,
    val themeMode: Int = 0,
    val localMode: Boolean = true,
    val localPort: Int = 18178,
    val serverRunning: Boolean = false,
    val featureCron: Boolean = true,
    val featureHook: Boolean = true,
    val featureAgent: Boolean = true,
    val featureSandbox: Boolean = true,
    val featureHeartbeat: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ds: SettingsDataStore,
    private val managementApi: ManagementApi,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val authInterceptor: AuthInterceptor,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        observeSettings()
    }
    
    private fun observeSettings() {
        viewModelScope.launch { ds.serverAddress.collect { _state.update { s -> s.copy(serverAddress = it) } } }
        viewModelScope.launch { ds.apiKey.collect { 
            _state.update { s -> s.copy(apiKey = it) }
            authInterceptor.apiKey = it
        } }
        viewModelScope.launch { ds.modelName.collect { _state.update { s -> s.copy(modelName = it) } } }
        viewModelScope.launch { ds.cheapModel.collect { _state.update { s -> s.copy(cheapModel = it) } } }
        viewModelScope.launch { ds.llmBaseUrl.collect { _state.update { s -> s.copy(llmBaseUrl = it) } } }
        viewModelScope.launch { ds.llmApiKey.collect { _state.update { s -> s.copy(llmApiKey = it) } } }
        viewModelScope.launch { ds.llmModel.collect { _state.update { s -> s.copy(llmModel = it) } } }
        viewModelScope.launch { ds.streaming.collect { _state.update { s -> s.copy(streaming = it) } } }
        viewModelScope.launch { ds.themeMode.collect { _state.update { s -> s.copy(themeMode = it) } } }
        viewModelScope.launch { ds.localMode.collect { _state.update { s -> s.copy(localMode = it) } } }
        viewModelScope.launch { 
            combine(ds.localPort, ds.localMode, ds.serverAddress) { port, local, addr ->
                if (local) "http://localhost:$port/" else "$addr/"
            }.collect { url ->
                baseUrlInterceptor.baseUrl = url
            }
        }
        viewModelScope.launch { ds.localPort.collect { _state.update { s -> s.copy(localPort = it) } } }
        viewModelScope.launch { ds.featureCron.collect { _state.update { s -> s.copy(featureCron = it) } } }
        viewModelScope.launch { ds.featureHook.collect { _state.update { s -> s.copy(featureHook = it) } } }
        viewModelScope.launch { ds.featureAgent.collect { _state.update { s -> s.copy(featureAgent = it) } } }
        viewModelScope.launch { ds.featureSandbox.collect { _state.update { s -> s.copy(featureSandbox = it) } } }
        viewModelScope.launch { ds.featureHeartbeat.collect { _state.update { s -> s.copy(featureHeartbeat = it) } } }
    }
    
    fun setServer(v: String) { viewModelScope.launch { ds.setServer(v) } }
    fun setApiKey(v: String) { viewModelScope.launch { ds.setApiKey(v) } }
    fun setModel(v: String) { viewModelScope.launch { ds.setModel(v) } }
    fun setCheapModel(v: String) { viewModelScope.launch { ds.setCheapModel(v) } }
    fun setLlmBaseUrl(v: String) { viewModelScope.launch { ds.setLlmBaseUrl(v) } }
    fun setLlmApiKey(v: String) { viewModelScope.launch { ds.setLlmApiKey(v) } }
    fun setLlmModel(v: String) { viewModelScope.launch { ds.setLlmModel(v) } }
    fun setStreaming(v: Boolean) { viewModelScope.launch { ds.setStreaming(v) } }
    fun setTheme(v: Int) { viewModelScope.launch { ds.setTheme(v) } }
    fun setLocalMode(v: Boolean) { viewModelScope.launch { ds.setLocalMode(v) } }
    fun setLocalPort(v: Int) { viewModelScope.launch { ds.setLocalPort(v) } }
    fun setFeatureCron(v: Boolean) { viewModelScope.launch { ds.setFeatureCron(v) } }
    fun setFeatureHook(v: Boolean) { viewModelScope.launch { ds.setFeatureHook(v) } }
    fun setFeatureAgent(v: Boolean) { viewModelScope.launch { ds.setFeatureAgent(v) } }
    fun setFeatureSandbox(v: Boolean) { viewModelScope.launch { ds.setFeatureSandbox(v) } }
    fun setFeatureHeartbeat(v: Boolean) { viewModelScope.launch { ds.setFeatureHeartbeat(v) } }
    
    fun startServer() {
        val port = _state.value.localPort
        val intent = Intent(ctx, TietiezhiService::class.java).apply {
            action = TietiezhiService.ACTION_START
            putExtra(TietiezhiService.EXTRA_PORT, port)
        }
        ctx.startForegroundService(intent)
        _state.update { it.copy(serverRunning = true) }
    }
    
    fun stopServer() {
        val intent = Intent(ctx, TietiezhiService::class.java).apply {
            action = TietiezhiService.ACTION_STOP
        }
        ctx.startService(intent)
        _state.update { it.copy(serverRunning = false) }
    }
    
    fun saveLlmConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                managementApi.updateConfig(
                    ConfigUpdateRequest(
                        llm = LlmConfigUpdate(
                            base_url = _state.value.llmBaseUrl.ifBlank { null },
                            api_key = _state.value.llmApiKey.ifBlank { null },
                            model = _state.value.llmModel.ifBlank { null },
                            cheap_model = _state.value.cheapModel.ifBlank { null }
                        )
                    )
                )
                _state.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
