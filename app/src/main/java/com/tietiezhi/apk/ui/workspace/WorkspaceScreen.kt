package com.tietiezhi.apk.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tietiezhi.apk.data.remote.dto.management.WorkspaceFile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    onFileClick: (String) -> Unit,
    vm: WorkspaceViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val editableExtensions = listOf(".md", ".txt", ".yaml", ".json", ".yml", ".toml", ".conf", ".sh", ".py", ".go")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("工作区", style = MaterialTheme.typography.titleMedium)
                        if (uiState.currentPath.isNotEmpty()) {
                            Text(
                                uiState.currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.currentPath.isNotEmpty()) {
                        IconButton(onClick = { vm.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回上级")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadWorkspace(uiState.currentPath) }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("加载失败: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { vm.loadWorkspace(uiState.currentPath) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                uiState.files.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("目录为空")
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.files) { file ->
                            FileItem(
                                file = file,
                                onClick = {
                                    if (file.is_dir) {
                                        val newPath = if (uiState.currentPath.isEmpty()) file.path
                                                      else "${uiState.currentPath}/${file.path}"
                                        vm.navigateTo(newPath)
                                    } else {
                                        val fullPath = if (uiState.currentPath.isEmpty()) file.path
                                                      else "${uiState.currentPath}/${file.path}"
                                        if (editableExtensions.any { fullPath.endsWith(it) }) {
                                            onFileClick(fullPath)
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(file: WorkspaceFile, onClick: () -> Unit) {
    val icon = when {
        file.is_dir -> Icons.Default.Folder
        file.path.endsWith(".md") -> Icons.Default.Description
        file.path.endsWith(".json") || file.path.endsWith(".yaml") || file.path.endsWith(".yml") -> Icons.Default.DataObject
        file.path.endsWith(".txt") -> Icons.Default.TextSnippet
        else -> Icons.Default.InsertDriveFile
    }
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    ListItem(
        headlineContent = { Text(file.path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!file.is_dir) {
                    Text(formatFileSize(file.size))
                }
                Text(dateFormat.format(Date(file.modified * 1000)))
            }
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            if (file.is_dir) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
