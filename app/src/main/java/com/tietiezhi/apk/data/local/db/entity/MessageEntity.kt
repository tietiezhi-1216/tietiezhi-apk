package com.tietiezhi.apk.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.model.MessageRole

@Entity(tableName = "messages", foreignKeys = [
    ForeignKey(entity = ChatEntity::class, parentColumns = ["id"], childColumns = ["chatId"], onDelete = ForeignKey.CASCADE)
])
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false
)

fun MessageEntity.toDomain() = Message(id, chatId, MessageRole.valueOf(role), content, timestamp, isStreaming)
fun Message.toEntity() = MessageEntity(id, chatId, role.name, content, timestamp, isStreaming)
