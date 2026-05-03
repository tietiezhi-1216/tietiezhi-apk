package com.tietiezhi.apk.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tietiezhi.terminal.emulator.Cell
import com.tietiezhi.terminal.emulator.TextStyle
import com.tietiezhi.terminal.rootfs.RootfsManager
import com.tietiezhi.terminal.session.TerminalSession
import com.tietiezhi.terminal.view.TerminalKeyEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    vm: TerminalViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val commandInput by vm.commandInput.collectAsState()
    val density = LocalDensity.current
    
    var showSettings by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    
    val fontSize = uiState.fontSize
    val lineHeight = with(density) { (fontSize * 1.2).sp.toDp() }
    val charWidth = with(density) { (fontSize * 0.6).sp.toDp() }
    
    // Calculate terminal dimensions based on available space
    var terminalColumns by remember { mutableIntStateOf(80) }
    var terminalRows by remember { mutableIntStateOf(24) }
    
    LaunchedEffect(uiState.fontSize) {
        vm.resizeTerminal(terminalColumns, terminalRows)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("终端${if (uiState.isProotMode) " (Proot)" else ""}")
                        if (uiState.sessionCount > 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge { Text("${uiState.sessionCount})") }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Font size controls
                    IconButton(onClick = { vm.decreaseFontSize() }) {
                        Icon(Icons.Default.TextDecrease, "减小字体")
                    }
                    Text("${uiState.fontSize.toInt()}sp", style = MaterialTheme.typography.labelSmall)
                    IconButton(onClick = { vm.increaseFontSize() }) {
                        Icon(Icons.Default.TextIncrease, "增大字体")
                    }
                    
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                    
                    IconButton(onClick = { showNewSessionDialog = true }) {
                        Icon(Icons.Default.Add, "新建会话")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Session tabs
            if (uiState.sessionCount > 1) {
                SessionTabs(
                    sessions = (1..uiState.sessionCount).toList(),
                    activeSession = uiState.activeSessionId ?: 1,
                    onSessionSelected = { vm.switchToSession(it) },
                    onSessionClosed = { vm.closeSession(it) }
                )
            }
            
            // Status bar
            TerminalStatusBar(
                isRunning = uiState.isRunning,
                isProotMode = uiState.isProotMode,
                rootfsStatus = uiState.rootfsStatus,
                progress = uiState.rootfsProgress,
                columns = terminalColumns,
                rows = terminalRows
            )
            
            // Terminal output
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { /* Focus keyboard */ }
                        )
                    }
            ) {
                // Check rootfs status
                when (uiState.rootfsStatus) {
                    RootfsManager.RootfsStatus.NOT_INSTALLED -> {
                        RootfsSetupPrompt(
                            onInstall = { vm.installRootfs() },
                            onDownload = { vm.downloadRootfs() }
                        )
                    }
                    RootfsManager.RootfsStatus.INSTALLING,
                    RootfsManager.RootfsStatus.DOWNLOADING -> {
                        RootfsProgressIndicator(
                            status = uiState.rootfsStatus,
                            progress = uiState.rootfsProgress
                        )
                    }
                    else -> {
                        // Show terminal content
                        TerminalContent(
                            screenLines = uiState.output,
                            cursorRow = uiState.cursorRow,
                            cursorCol = uiState.cursorCol,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            charWidth = charWidth
                        )
                    }
                }
            }
            
            // Error display
            uiState.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        "错误: $error",
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Virtual keyboard
            VirtualKeyboard(
                onKeyEvent = { event ->
                    when (event) {
                        is TerminalKeyEvent.Text -> vm.sendText(event.text)
                        is TerminalKeyEvent.Enter -> vm.sendKey(13) // CR
                        is TerminalKeyEvent.Backspace -> vm.sendKey(127) // DEL
                        is TerminalKeyEvent.CursorUp -> vm.sendKey(0x80)
                        is TerminalKeyEvent.CursorDown -> vm.sendKey(0x81)
                        is TerminalKeyEvent.CursorLeft -> vm.sendKey(0x82)
                        is TerminalKeyEvent.CursorRight -> vm.sendKey(0x83)
                        is TerminalKeyEvent.SpecialKey -> {
                            when (event.key) {
                                "Tab" -> vm.sendKey(9)
                                "Esc" -> vm.sendKey(27)
                                else -> {} // Handle Ctrl/Alt modifiers
                            }
                        }
                    }
                }
            )
            
            // Command input
            CommandInput(
                value = commandInput,
                onValueChange = { vm.onCommandInputChange(it) },
                onExecute = { vm.executeCommand(commandInput) },
                enabled = uiState.isRunning
            )
        }
    }
    
    // Settings dialog
    if (showSettings) {
        TerminalSettingsDialog(
            fontSize = uiState.fontSize,
            onFontSizeChange = { vm.setFontSize(it) },
            onDismiss = { showSettings = false }
        )
    }
    
    // New session dialog
    if (showNewSessionDialog) {
        NewSessionDialog(
            onShellSession = {
                vm.newSession()
                showNewSessionDialog = false
            },
            onProotSession = {
                vm.newProotSession()
                showNewSessionDialog = false
            },
            onDismiss = { showNewSessionDialog = false }
        )
    }
}

@Composable
private fun SessionTabs(
    sessions: List<Int>,
    activeSession: Int,
    onSessionSelected: (Int) -> Unit,
    onSessionClosed: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sessions.forEach { sessionId ->
                FilterChip(
                    selected = sessionId == activeSession,
                    onClick = { onSessionSelected(sessionId) },
                    label = { Text("终端 $sessionId") },
                    trailingIcon = {
                        if (sessions.size > 1) {
                            IconButton(
                                onClick = { onSessionClosed(sessionId) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TerminalStatusBar(
    isRunning: Boolean,
    isProotMode: Boolean,
    rootfsStatus: RootfsManager.RootfsStatus,
    progress: Float,
    columns: Int,
    rows: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = buildString {
                    append(if (isRunning) "运行中" else "已停止")
                    if (isProotMode) append(" | PRoot")
                },
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${columns}x${rows}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "xterm-256color",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun TerminalContent(
    screenLines: List<List<Cell>>,
    cursorRow: Int,
    cursorCol: Int,
    fontSize: Float,
    lineHeight: Dp,
    charWidth: Dp
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(screenLines.size) {
        if (screenLines.isNotEmpty()) {
            listState.animateScrollToItem(screenLines.size - 1)
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        items(screenLines.indices.toList()) { rowIndex ->
            Row(
                modifier = Modifier.height(lineHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screenLines.getOrNull(rowIndex)?.forEachIndexed { colIndex, cell ->
                    TerminalCell(
                        cell = cell,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        charWidth = charWidth,
                        isCursor = rowIndex == cursorRow && colIndex == cursorCol
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalCell(
    cell: Cell,
    fontSize: Float,
    lineHeight: Dp,
    charWidth: Dp,
    isCursor: Boolean
) {
    val foreColor = if (cell.foreColor in TextStyle.ANSI_COLORS.indices) {
        TextStyle.ANSI_COLORS[cell.foreColor]
    } else {
        Color(0xFFD4D4D4)
    }
    
    val backColor = if (cell.backColor in TextStyle.ANSI_COLORS.indices) {
        TextStyle.ANSI_COLORS[cell.backColor]
    } else {
        Color.Transparent
    }
    
    val displayChar = if (cell.concealed) ' ' else cell.char
    val effectiveForeColor = if (cell.inverse) backColor else foreColor
    val effectiveBackColor = if (cell.inverse) foreColor else backColor
    
    val textDecoration = when {
        cell.underline -> TextDecoration.Underline
        cell.strikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }
    
    Box(
        modifier = Modifier
            .width(charWidth)
            .height(lineHeight)
            .background(if (effectiveBackColor != Color.Transparent) effectiveBackColor else Color.Transparent),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isCursor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
        Text(
            text = displayChar.toString(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = effectiveForeColor,
                textDecoration = textDecoration
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun VirtualKeyboard(
    onKeyEvent: (TerminalKeyEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2D2D2D),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(4.dp)
        ) {
            // Control keys row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Ctrl", "Alt", "Tab", "Esc").forEach { key ->
                    KeyButton(
                        label = key,
                        onClick = { onKeyEvent(TerminalKeyEvent.SpecialKey(key)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Arrow keys row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                KeyButton("▲", { onKeyEvent(TerminalKeyEvent.CursorUp) }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                KeyButton("▼", { onKeyEvent(TerminalKeyEvent.CursorDown) }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                KeyButton("◀", { onKeyEvent(TerminalKeyEvent.CursorLeft) }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                KeyButton("▶", { onKeyEvent(TerminalKeyEvent.CursorRight) }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A4A4A)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommandInput(
    value: String,
    onValueChange: (String) -> Unit,
    onExecute: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$ ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (enabled) onExecute() }
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            "输入命令...",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    innerTextField()
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onExecute,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(Icons.Default.PlayArrow, "执行")
                Spacer(modifier = Modifier.width(4.dp))
                Text("执行")
            }
        }
    }
}

@Composable
private fun RootfsSetupPrompt(
    onInstall: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Ubuntu 环境未安装",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "安装 Ubuntu rootfs 以在终端中运行完整的 Linux 环境",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onInstall) {
            Icon(Icons.Default.InstallDesktop, "安装")
            Spacer(modifier = Modifier.width(8.dp))
            Text("从本地安装")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(onClick = onDownload) {
            Icon(Icons.Default.CloudDownload, "下载")
            Spacer(modifier = Modifier.width(8.dp))
            Text("从网络下载")
        }
    }
}

@Composable
private fun RootfsProgressIndicator(
    status: RootfsManager.RootfsStatus,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            when (status) {
                RootfsManager.RootfsStatus.INSTALLING -> "正在安装 Ubuntu..."
                RootfsManager.RootfsStatus.DOWNLOADING -> "正在下载 Ubuntu..."
                else -> "处理中..."
            },
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TerminalSettingsDialog(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("终端设置") },
        text = {
            Column {
                Text("字体大小")
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 8f..24f,
                    steps = 7
                )
                Text("${fontSize.toInt()}sp", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

@Composable
private fun NewSessionDialog(
    onShellSession: () -> Unit,
    onProotSession: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建会话") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShellSession,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Terminal, "Shell")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shell 会话 (Android)")
                }
                
                OutlinedButton(
                    onClick = onProotSession,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Laptop, "Ubuntu")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ubuntu 会话 (PRoot)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
