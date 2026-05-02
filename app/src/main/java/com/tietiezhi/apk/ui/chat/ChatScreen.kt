package com.tietiezhi.apk.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tietiezhi.apk.domain.model.Message
import com.tietiezhi.apk.domain.model.MessageRole
import com.tietiezhi.apk.ui.chat.components.ChatInput
import com.tietiezhi.apk.ui.chat.components.MessageBubble
import com.tietiezhi.apk.ui.chat.components.StreamingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, vm: ChatViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsState()
    val inputText by vm.inputText.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val streamingContent by vm.streamingContent.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("对话") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } })
        },
        bottomBar = {
            ChatInput(value = inputText, onValueChange = vm::updateInput,
                onSend = vm::sendMessage, isGenerating = isGenerating, onStop = vm::stopGeneration)
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
            items(messages, key = { it.id }) { msg -> MessageBubble(message = msg) }
            if (isGenerating && streamingContent.isNotBlank()) {
                item { MessageBubble(message = Message(chatId = "", role = MessageRole.ASSISTANT, content = streamingContent)) }
            } else if (isGenerating) {
                item { StreamingIndicator() }
            }
        }
    }
}
