package com.tietiezhi.apk.domain.model

import java.util.UUID

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新会话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
