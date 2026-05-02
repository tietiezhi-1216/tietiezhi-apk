package com.tietiezhi.apk.ui.chatlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tietiezhi.apk.domain.model.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    vm: ChatListViewModel = hiltViewModel()
) {
    val chats by vm.chats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("铁铁汁") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.createChat(onChatClick) }) {
                Icon(Icons.Default.Add, contentDescription = "新会话")
            }
        }
    ) { padding ->
        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无会话，点击 + 开始", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(chats, key = { it.id }) { chat ->
                    ChatItem(chat = chat,
                        onClick = { onChatClick(chat.id) },
                        onDelete = { vm.deleteChat(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatItem(chat: Chat, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(chat.title, maxLines = 1) },
        supportingContent = {
            Text(
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(chat.updatedAt),
                style = MaterialTheme.typography.labelSmall
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Text("⋮")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("删除") }, onClick = { onDelete(); showMenu = false })
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
