package com.tietiezhi.apk.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
        else -> Color(0xFF191C1A)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .padding(12.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                MarkdownText(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MarkdownText(text: String, color: Color, style: TextStyle) {
    Text(
        text = parseSimpleMarkdown(text),
        color = color,
        style = style
    )
}

private fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        
        lines.forEachIndexed { lineIndex, line ->
            when {
                // Code block markers
                line.startsWith("```") -> {
                    if (line.length > 3 && line.substring(3).isNotBlank()) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE0E0E0), color = Color(0xFF333333))) {
                            append(line.substring(3))
                        }
                    }
                }
                // List items
                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("• ")
                    append(parseInlineMarkdown(line.substring(2)))
                }
                // Headers
                line.matches(Regex("^#{1,6}\\s.*")) -> {
                    val level = line.takeWhile { it == '#' }.length
                    val content = line.substring(level + 1)
                    withStyle(SpanStyle(fontWeight = if (level <= 2) FontWeight.Bold else FontWeight.Medium)) {
                        append(content)
                    }
                }
                else -> {
                    append(parseInlineMarkdown(line))
                }
            }
            
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun parseInlineMarkdown(text: String): String {
    var result = text
    // Remove bold markers
    result = result.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    // Remove inline code markers but keep content
    result = result.replace(Regex("`([^`]+)`"), "$1")
    // Remove italic markers
    result = result.replace(Regex("\\*([^*]+)\\*"), "$1")
    return result
}
