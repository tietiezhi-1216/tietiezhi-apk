package com.tietiezhi.apk.domain.repository

import com.tietiezhi.apk.domain.model.Chat
import com.tietiezhi.apk.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllChats(): Flow<List<Chat>>
    suspend fun getChatById(id: String): Chat?
    suspend fun createChat(chat: Chat): String
    suspend fun updateChat(chat: Chat)
    suspend fun deleteChat(id: String)
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(id: String)
    suspend fun sendMessage(chatId: String, content: String): Flow<String>
    suspend fun stopGeneration()
}
