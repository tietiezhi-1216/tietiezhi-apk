package com.tietiezhi.apk.data.repository

import com.tietiezhi.apk.data.local.db.dao.ChatDao
import com.tietiezhi.apk.data.local.db.dao.MessageDao
import com.tietiezhi.apk.data.local.db.entity.toDomain
import com.tietiezhi.apk.data.local.db.entity.toEntity
import com.tietiezhi.apk.data.remote.api.ChatApi
import com.tietiezhi.apk.data.remote.dto.ChatMessageDto
import com.tietiezhi.apk.data.remote.dto.ChatRequest
import com.tietiezhi.apk.domain.model.Chat
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.model.MessageRole
import com.tietiezhi.apk.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val api: ChatApi
) : ChatRepository {

    var currentModel = "default"
    var streamingEnabled = true
    var serverUrl = "http://localhost:18178"
    var apiKey = ""
    private var currentCall: okhttp3.Call? = null

    fun updateConfig(model: String, streaming: Boolean, server: String, key: String) {
        currentModel = model; streamingEnabled = streaming; serverUrl = server; apiKey = key
    }

    override fun getAllChats(): Flow<List<Chat>> = chatDao.getAll().map { it.map { e -> e.toDomain() } }
    override suspend fun getChatById(id: String): Chat? = chatDao.getById(id)?.toDomain()
    override suspend fun createChat(chat: Chat): String { chatDao.insert(chat.toEntity()); return chat.id }
    override suspend fun updateChat(chat: Chat) = chatDao.update(chat.toEntity())
    override suspend fun deleteChat(id: String) = chatDao.deleteById(id)
    override fun getMessages(chatId: String): Flow<List<Message>> = messageDao.getByChatId(chatId).map { it.map { e -> e.toDomain() } }
    override suspend fun addMessage(message: Message) = messageDao.insert(message.toEntity())
    override suspend fun updateMessage(message: Message) = messageDao.update(message.toEntity())
    override suspend fun deleteMessage(id: String) = messageDao.deleteById(id)

    override fun sendMessage(chatId: String, content: String): Flow<String> = flow {
        messageDao.insert(Message(chatId = chatId, role = MessageRole.USER, content = content).toEntity())
        val history = listOf(ChatMessageDto(role = "user", content = content))

        if (streamingEnabled) {
            emit("")
            val client = OkHttpClient.Builder().readTimeout(120, TimeUnit.SECONDS).build()
            val jsonBody = Json.encodeToString(ChatRequest.serializer(),
                ChatRequest(model = currentModel, messages = history, stream = true))
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$serverUrl/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body).build()
            val call = client.newCall(req)
            currentCall = call
            withContext(Dispatchers.IO) {
                val resp = call.execute()
                val fullContent = StringBuilder()
                resp.body?.byteStream()?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") return@forEachLine
                            try {
                                val node = Json.decodeFromString<com.tietiezhi.apk.data.remote.dto.ChatResponse>(data)
                                val delta = node.choices.firstOrNull()?.delta?.content
                                if (delta != null) { fullContent.append(delta) }
                            } catch (_: Exception) {}
                        }
                    }
                }
                messageDao.insert(Message(chatId = chatId, role = MessageRole.ASSISTANT, content = fullContent.toString()).toEntity())
            }
        } else {
            val resp = api.chatCompletions(ChatRequest(model = currentModel, messages = history, stream = false))
            val reply = resp.choices.firstOrNull()?.message?.content ?: ""
            messageDao.insert(Message(chatId = chatId, role = MessageRole.ASSISTANT, content = reply).toEntity())
            emit(reply)
        }
    }

    override suspend fun stopGeneration() { currentCall?.cancel(); currentCall = null }
}
