package com.tietiezhi.apk.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.model.MessageRole
import com.tietiezhi.apk.ui.chat.components.ChatInput
import com.tietiezhi.apk.ui.chat.components.MessageBubble
import com.tietiezhi.apk.ui.chat.components.StreamingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onMenuClick: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    val messages by vm.messages.collectAsState()
    val inputText by vm.inputText.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val streamingContent by vm.streamingContent.collectAsState()
    val serverReady by vm.serverReady.collectAsState()
    val serverError by vm.serverError.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("铁铁汁") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "菜单")
                    }
                },
                actions = {
                    // Server status indicator
                    ServerStatusBadge(
                        isReady = serverReady,
                        error = serverError
                    )
                }
            )
        },
        bottomBar = {
            Column {
                // Server error banner
                if (!serverReady && serverError != null) {
                    ServerErrorBanner(error = serverError!!)
                }
                ChatInput(
                    value = inputText,
                    onValueChange = vm::updateInput,
                    onSend = vm::sendMessage,
                    isGenerating = isGenerating,
                    onStop = vm::stopGeneration,
                    enabled = serverReady
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }
            if (isGenerating && streamingContent.isNotBlank()) {
                item {
                    MessageBubble(message = Message(chatId = "", role = MessageRole.ASSISTANT, content = streamingContent))
                }
            } else if (isGenerating) {
                item { StreamingIndicator() }
            }
        }
    }
}

@Composable
private fun ServerStatusBadge(
    isReady: Boolean,
    error: String?
) {
    val color = if (isReady) Color(0xFF4CAF50) else Color(0xFFFF9800)
    val text = if (isReady) "在线" else "离线"
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ServerErrorBanner(error: String) {
    Surface(
        color = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error ?: "服务器未就绪",
                color = Color(0xFFE65100),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
