package com.tietiezhi.apk.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("设置") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 运行模式
            Text("运行模式", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                FilterChip(selected = state.localMode, onClick = { vm.setLocalMode(true) }, label = { Text("本地模式") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = !state.localMode, onClick = { vm.setLocalMode(false) }, label = { Text("远程模式") })
            }

            HorizontalDivider()

            if (state.localMode) {
                Text("本地服务", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = state.localPort.toString(), onValueChange = { vm.setLocalPort(it.toIntOrNull() ?: 18178) },
                    label = { Text("端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            } else {
                Text("远程服务器", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = state.serverAddress, onValueChange = vm::setServer,
                    label = { Text("服务器地址") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.apiKey, onValueChange = vm::setApiKey,
                    label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider()

            // 模型
            Text("模型配置", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = state.modelName, onValueChange = vm::setModel,
                label = { Text("模型名称") }, modifier = Modifier.fillMaxWidth())

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
                    FilterChip(selected = state.themeMode == mode, onClick = { vm.setTheme(mode) }, label = { Text(label) })
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}
