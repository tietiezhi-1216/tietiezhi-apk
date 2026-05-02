package com.tietiezhi.apk.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.model.MessageRole
import com.tietiezhi.apk.ui.theme.*

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    val darkTheme = isSystemInDarkTheme()
    val bgColor = when {
        isUser -> UserBubble
        darkTheme -> AssistantBubbleDark
        else -> AssistantBubble
    }
    val textColor = when {
        isUser -> UserBubbleText
        darkTheme -> AssistantBubbleTextDark
        else -> androidx.compose.ui.graphics.Color(0xFF191C1A)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
