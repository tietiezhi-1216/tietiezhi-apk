package com.tietiezhi.apk.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val model: String = ""
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: MessageDto? = null,
    val delta: DeltaDto? = null,
    val finish_reason: String? = null
)

@Serializable
data class MessageDto(
    val role: String = "",
    val content: String = ""
)

@Serializable
data class DeltaDto(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String = "",
    val owned_by: String = ""
)
