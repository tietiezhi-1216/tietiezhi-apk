package com.tietiezhi.apk.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.domain.model.Chat
import com.tietiezhi.apk.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(private val repo: ChatRepository) : ViewModel() {
    val chats = repo.getAllChats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createChat(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repo.createChat(Chat())
            onCreated(id)
        }
    }

    fun deleteChat(id: String) {
        viewModelScope.launch { repo.deleteChat(id) }
    }

    fun renameChat(id: String, title: String) {
        viewModelScope.launch {
            val chat = repo.getChatById(id) ?: return@launch
            repo.updateChat(chat.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }
}
