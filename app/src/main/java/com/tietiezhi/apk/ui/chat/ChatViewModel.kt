package com.tietiezhi.apk.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import com.tietiezhi.apk.data.remote.interceptor.AuthInterceptor
import com.tietiezhi.apk.data.remote.interceptor.BaseUrlInterceptor
import com.tietiezhi.apk.data.repository.ChatRepositoryImpl
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val settings: SettingsDataStore,
    private val repoImpl: ChatRepositoryImpl,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val authInterceptor: AuthInterceptor
) : ViewModel() {
    private val chatId: String = "default"

    val messages: StateFlow<List<Message>> = repo.getMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    // Server status
    private val _serverReady = MutableStateFlow(false)
    val serverReady: StateFlow<Boolean> = _serverReady.asStateFlow()
    
    private val _serverError = MutableStateFlow<String?>(null)
    val serverError: StateFlow<String?> = _serverError.asStateFlow()

    init {
        viewModelScope.launch {
            settings.modelName.collect { repoImpl.currentModel = it }
        }
        viewModelScope.launch {
            settings.streaming.collect { repoImpl.streamingEnabled = it }
        }
        viewModelScope.launch {
            combine(settings.serverAddress, settings.localMode, settings.localPort) { addr, local, port ->
                Triple(addr, local, port)
            }.collect { (addr, local, port) ->
                val url = if (local) "http://localhost:$port" else addr
                repoImpl.serverUrl = url
                baseUrlInterceptor.baseUrl = "$url/"
            }
        }
        viewModelScope.launch {
            settings.apiKey.collect { key ->
                repoImpl.apiKey = key
                authInterceptor.apiKey = key
            }
        }
        
        // Start server health check
        startServerHealthCheck()
    }

    /**
     * Check server health periodically
     */
    private fun startServerHealthCheck() {
        viewModelScope.launch {
            while (true) {
                checkServerHealth()
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    private suspend fun checkServerHealth() {
        try {
            val url = repoImpl.serverUrl
            if (url.isEmpty()) {
                _serverReady.value = false
                _serverError.value = "服务器地址未配置"
                return
            }
            
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url("$url/v1/models")
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                _serverReady.value = true
                _serverError.value = null
            } else {
                _serverReady.value = false
                _serverError.value = "服务器响应异常: ${response.code}"
            }
        } catch (e: Exception) {
            _serverReady.value = false
            _serverError.value = "服务器未就绪: ${e.message}"
        }
    }

    fun updateInput(text: String) { _inputText.value = text }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isGenerating.value) return
        
        // Check if server is ready before sending
        if (!_serverReady.value) {
            return
        }
        
        _inputText.value = ""
        _isGenerating.value = true
        _streamingContent.value = ""

        viewModelScope.launch {
            try {
                repo.sendMessage(chatId, text).collect { chunk ->
                    if (chunk.isEmpty()) return@collect
                    _streamingContent.value += chunk
                }
            } catch (_: Exception) {
                // Handle error silently
            } finally {
                _isGenerating.value = false
                _streamingContent.value = ""
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch {
            repo.stopGeneration()
            _isGenerating.value = false
            _streamingContent.value = ""
        }
    }
}
