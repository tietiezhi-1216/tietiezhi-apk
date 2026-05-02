package com.tietiezhi.apk.ui.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tietiezhi.apk.data.remote.dto.management.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(vm: FeaturesViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("技能", "MCP", "代理", "钩子", "定时")
    
    LaunchedEffect(Unit) { vm.loadAll() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能") },
                actions = {
                    IconButton(onClick = { vm.loadAll() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> SkillsTab(uiState.skills, { vm.loadSkill(it) })
                    1 -> McpTab(uiState.mcpServers)
                    2 -> AgentsTab(uiState.agents, { vm.killAgent(it) })
                    3 -> HooksTab(uiState.hooks)
                    4 -> CronTab(uiState.cronJobs, { vm.deleteCronJob(it) }, { n, m, e -> vm.createCronJob(n, m, e) })
                }
            }
        }
        
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show error and clear
                vm.clearError()
            }
        }
    }
}

@Composable
fun SkillsTab(skills: List<SkillInfo>, onLoad: (String) -> Unit) {
    if (skills.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无技能")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(skills) { skill ->
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(skill.name) },
                    supportingContent = { Text(skill.description, maxLines = if (expanded) Int.MAX_VALUE else 2) },
                    leadingContent = {
                        Icon(
                            if (skill.has_mcp) Icons.Default.Extension else Icons.Default.Psychology,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        if (!expanded) {
                            IconButton(onClick = { onLoad(skill.name) }) {
                                Icon(Icons.Default.PlayArrow, "加载")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (skill.has_mcp && skill.mcp_servers.isNotEmpty()) {
                    Text(
                        "MCP服务器: ${skill.mcp_servers.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun McpTab(servers: List<McpServer>) {
    if (servers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无已连接的MCP服务器")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(servers) { server ->
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(server.name) },
                    supportingContent = {
                        Column {
                            Text("工具数量: ${server.tools.size}")
                            if (expanded) {
                                server.tools.forEach { tool ->
                                    Text("• ${tool.name}: ${tool.description}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "展开")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun AgentsTab(agents: List<AgentInfo>, onKill: (String) -> Unit) {
    if (agents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无运行中的代理")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(agents) { agent ->
                ListItem(
                    headlineContent = { Text(agent.label.ifEmpty { agent.spawn_id }) },
                    supportingContent = {
                        Text("状态: ${agent.status} | 启动时间: ${agent.started_at}")
                    },
                    trailingContent = {
                        IconButton(onClick = { onKill(agent.spawn_id) }) {
                            Icon(Icons.Default.Stop, "终止", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun HooksTab(hooks: List<HookRule>) {
    if (hooks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无Hook规则")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(hooks) { hook ->
                ListItem(
                    headlineContent = { Text("事件: ${hook.event}") },
                    supportingContent = {
                        Text("匹配器: ${hook.matcher} | 类型: ${hook.type}")
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun CronTab(
    jobs: List<CronJob>,
    onDelete: (String) -> Unit,
    onCreate: (String, String, Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("60000") }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("创建定时任务") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("消息内容") })
                    OutlinedTextField(value = interval, onValueChange = { interval = it }, label = { Text("间隔(毫秒)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreate(name, message, interval.toLongOrNull() ?: 60000)
                    showDialog = false
                    name = ""
                    message = ""
                    interval = "60000"
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }
    
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (jobs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无定时任务")
                    }
                }
            } else {
                items(jobs) { job ->
                    ListItem(
                        headlineContent = { Text(job.name) },
                        supportingContent = {
                            Text("调度: ${job.schedule} | 运行次数: ${job.run_count}")
                        },
                        leadingContent = {
                            Icon(
                                if (job.enabled) Icons.Default.Schedule else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (job.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(job.id) }) {
                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "添加定时任务")
            }
        }
    }
}
