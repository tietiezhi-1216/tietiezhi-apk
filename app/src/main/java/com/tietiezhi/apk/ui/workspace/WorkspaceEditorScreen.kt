package com.tietiezhi.apk.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceEditorScreen(
    filePath: String,
    onBack: () -> Unit,
    vm: WorkspaceViewModel = hiltViewModel()
) {
    val editorState by vm.editorState.collectAsState()
    var editedContent by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(filePath) {
        vm.loadFile(filePath)
    }
    
    LaunchedEffect(editorState.fileContent) {
        editorState.fileContent?.let {
            editedContent = it.content
            hasChanges = false
        }
    }
    
    LaunchedEffect(editorState.saveSuccess) {
        if (editorState.saveSuccess) {
            snackbarHostState.showSnackbar("保存成功")
            hasChanges = false
        }
    }
    
    LaunchedEffect(editorState.error) {
        editorState.error?.let {
            snackbarHostState.showSnackbar("错误: $it")
            vm.clearError()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            vm.clearEditorState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("编辑文件", style = MaterialTheme.typography.titleMedium)
                        Text(
                            filePath.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (hasChanges) {
                        Text("有未保存更改", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = { vm.saveFile(filePath, editedContent) },
                        enabled = !editorState.isSaving && hasChanges
                    ) {
                        if (editorState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, "保存")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                editorState.isLoading -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        tonalElevation = 1.dp
                    ) {
                        BasicTextField(
                            value = editedContent,
                            onValueChange = { 
                                editedContent = it
                                hasChanges = true
                            },
                            modifier = Modifier.fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            textStyle = TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (editedContent.isEmpty()) {
                                    Text(
                                        "在此输入文件内容...",
                                        style = TextStyle(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        }
    }
}
