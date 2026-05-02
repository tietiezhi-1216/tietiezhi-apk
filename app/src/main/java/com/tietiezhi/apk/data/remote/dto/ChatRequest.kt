package com.tietiezhi.apk.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
    val temperature: Double = 0.7
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)
