package com.tietiezhi.apk.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import com.tietiezhi.apk.data.repository.ChatRepositoryImpl
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val settings: SettingsDataStore,
    private val repoImpl: ChatRepositoryImpl
) : ViewModel() {
    // 默认使用 single 对话
    private val chatId: String = "default"

    val messages: StateFlow<List<Message>> = repo.getMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settings.modelName, settings.streaming, settings.serverAddress, settings.apiKey) { m, s, u, k ->
                repoImpl.updateConfig(m, s, u, k)
            }.collect()
        }
    }

    fun updateInput(text: String) { _inputText.value = text }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isGenerating.value) return
        _inputText.value = ""
        _isGenerating.value = true
        _streamingContent.value = ""

        viewModelScope.launch {
            try {
                repo.sendMessage(chatId, text).collect { chunk ->
                    if (chunk.isEmpty()) return@collect
                    _streamingContent.value += chunk
                }
            } catch (_: Exception) {} finally {
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
