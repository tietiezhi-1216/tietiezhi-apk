package com.tietiezhi.apk.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tietiezhi.apk.domain.model.Chat

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false
)

fun ChatEntity.toDomain() = Chat(id, title, createdAt, updatedAt, isPinned)
fun Chat.toEntity() = ChatEntity(id, title, createdAt, updatedAt, isPinned)
