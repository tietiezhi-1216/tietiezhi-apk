package com.tietiezhi.apk.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedLLM by remember { mutableStateOf(false) }
    var expandedFeatures by remember { mutableStateOf(false) }
    
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar("错误: $it")
            vm.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 运行模式
            Text("运行模式", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = state.localMode,
                    onClick = { vm.setLocalMode(true) },
                    label = { Text("本地模式") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = !state.localMode,
                    onClick = { vm.setLocalMode(false) },
                    label = { Text("远程模式") }
                )
            }
            
            HorizontalDivider()
            
            if (state.localMode) {
                // 本地服务控制
                Text("本地服务", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (state.serverRunning) "运行中" else "已停止",
                            color = if (state.serverRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text("端口: ${state.localPort}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        if (state.serverRunning) {
                            Button(
                                onClick = { vm.stopServer() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(Modifier.width(4.dp))
                                Text("停止")
                            }
                        } else {
                            Button(onClick = { vm.startServer() }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(4.dp))
                                Text("启动")
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = state.localPort.toString(),
                    onValueChange = { vm.setLocalPort(it.toIntOrNull() ?: 18178) },
                    label = { Text("端口") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 远程服务器
                Text("远程服务器", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.serverAddress,
                    onValueChange = vm::setServer,
                    label = { Text("服务器地址") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = vm::setApiKey,
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            HorizontalDivider()
            
            // LLM 配置
            ListItem(
                headlineContent = { Text("LLM 配置") },
                supportingContent = { Text("Base URL, API Key, Model") },
                leadingContent = { Icon(Icons.Default.Psychology, null) },
                trailingContent = {
                    IconButton(onClick = { expandedLLM = !expandedLLM }) {
                        Icon(if (expandedLLM) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                },
                modifier = Modifier.clickable { expandedLLM = !expandedLLM }
            )
            
            if (expandedLLM) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.llmBaseUrl,
                            onValueChange = vm::setLlmBaseUrl,
                            label = { Text("Base URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.llmApiKey,
                            onValueChange = vm::setLlmApiKey,
                            label = { Text("API Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.llmModel,
                            onValueChange = vm::setLlmModel,
                            label = { Text("Model") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.cheapModel,
                            onValueChange = vm::setCheapModel,
                            label = { Text("Cheap Model") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { vm.saveLlmConfig() },
                            enabled = !state.isLoading,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("保存配置")
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // 功能开关
            ListItem(
                headlineContent = { Text("功能开关") },
                supportingContent = { Text("定时任务、心跳、Hook、子代理、沙箱") },
                leadingContent = { Icon(Icons.Default.ToggleOn, null) },
                trailingContent = {
                    IconButton(onClick = { expandedFeatures = !expandedFeatures }) {
                        Icon(if (expandedFeatures) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                },
                modifier = Modifier.clickable { expandedFeatures = !expandedFeatures }
            )
            
            if (expandedFeatures) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        FeatureSwitch("定时任务", state.featureCron, vm::setFeatureCron)
                        FeatureSwitch("心跳", state.featureHeartbeat, vm::setFeatureHeartbeat)
                        FeatureSwitch("Hook", state.featureHook, vm::setFeatureHook)
                        FeatureSwitch("子代理", state.featureAgent, vm::setFeatureAgent)
                        FeatureSwitch("沙箱", state.featureSandbox, vm::setFeatureSandbox)
                    }
                }
            }
            
            HorizontalDivider()
            
            // 模型配置
            Text("模型配置", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.modelName,
                onValueChange = vm::setModel,
                label = { Text("默认模型名称") },
                modifier = Modifier.fillMaxWidth()
            )
            
            HorizontalDivider()
            
            // 流式输出
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("流式输出", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.streaming, onCheckedChange = vm::setStreaming)
            }
            
            HorizontalDivider()
            
            // 主题
            Text("主题", style = MaterialTheme.typography.titleMedium)
            Row {
                listOf("跟随系统" to 0, "亮色" to 1, "暗色" to 2).forEach { (label, mode) ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { vm.setTheme(mode) },
                        label = { Text(label) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            
            HorizontalDivider()
            
            // 关于
            Text("关于", style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("铁铁汁 AI Agent") },
                supportingContent = { Text("版本 1.2.4") },
                leadingContent = { Icon(Icons.Default.Info, null) }
            )
            
            OutlinedButton(
                onClick = { /* Open GitHub */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Code, null)
                Spacer(Modifier.width(8.dp))
                Text("GitHub 仓库")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
