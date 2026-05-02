package com.tietiezhi.apk.data.remote.dto.management

import kotlinx.serialization.Serializable

// ==================== Config ====================
@Serializable
data class ConfigResponse(
    val llm: LlmConfig? = null,
    val server: ServerConfig? = null,
    val agent: AgentConfig? = null,
    val channels: ChannelConfig? = null,
    val scheduler: Boolean = false,
    val heartbeat: Boolean = false,
    val hooks: Boolean = false,
    val subagent: Boolean = false,
    val sandbox: Boolean = false
)

@Serializable
data class LlmConfig(
    val base_url: String = "",
    val api_key: String = "",
    val model: String = "",
    val cheap_model: String = "",
    val provider: String = ""
)

@Serializable
data class ServerConfig(
    val port: Int = 18178,
    val host: String = "localhost"
)

@Serializable
data class AgentConfig(
    val max_tool_calls: Int = 20,
    val loop_detection: Boolean = true,
    val compression: Boolean = false
)

@Serializable
data class ChannelConfig(
    val feishu: Boolean = false,
    val telegram: Boolean = false
)

@Serializable
data class ConfigUpdateRequest(
    val llm: LlmConfigUpdate? = null
)

@Serializable
data class LlmConfigUpdate(
    val base_url: String? = null,
    val api_key: String? = null,
    val model: String? = null,
    val cheap_model: String? = null
)

// ==================== Skills ====================
@Serializable
data class SkillsResponse(
    val skills: List<SkillInfo> = emptyList(),
    val total: Int = 0
)

@Serializable
data class SkillInfo(
    val name: String = "",
    val description: String = "",
    val has_mcp: Boolean = false,
    val mcp_servers: List<String> = emptyList(),
    val allowed_tools: List<String> = emptyList()
)

@Serializable
data class SkillLoadRequest(
    val name: String
)

// ==================== MCP ====================
@Serializable
data class McpResponse(
    val servers: List<McpServer> = emptyList(),
    val total: Int = 0
)

@Serializable
data class McpServer(
    val name: String = "",
    val tools: List<ToolInfo> = emptyList()
)

@Serializable
data class ToolInfo(
    val name: String = "",
    val description: String = ""
)

// ==================== Agents ====================
@Serializable
data class AgentsResponse(
    val agents: List<AgentInfo> = emptyList(),
    val total: Int = 0
)

@Serializable
data class AgentInfo(
    val spawn_id: String = "",
    val session_key: String = "",
    val status: String = "",
    val label: String = "",
    val started_at: String = "",
    val ended_at: String? = null,
    val error: String? = null
)

// ==================== Hooks ====================
@Serializable
data class HooksResponse(
    val rules: List<HookRule> = emptyList(),
    val total: Int = 0,
    val enabled: Boolean = false
)

@Serializable
data class HookRule(
    val index: Int = 0,
    val event: String = "",
    val matcher: String = "",
    val type: String = "",
    val command: String? = null,
    val script: String? = null,
    val timeout: Int = 30
)

// ==================== Cron ====================
@Serializable
data class CronListResponse(
    val jobs: List<CronJob> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CronJob(
    val id: String = "",
    val name: String = "",
    val message: String = "",
    val schedule: ScheduleInfo = ScheduleInfo(),
    val enabled: Boolean = true,
    val delete_after_run: Boolean = false,
    val created_at: String = "",
    val last_run_at: String? = null,
    val next_run_at: String? = null,
    val run_count: Int = 0,
    val mode: String = "isolated"
)

@Serializable
data class ScheduleInfo(
    val kind: String = "",
    val at: String? = null,
    val every_ms: Long? = null,
    val expr: String? = null,
    val tz: String? = null
)

@Serializable
data class CronCreateRequest(
    val name: String,
    val message: String,
    val kind: String = "every",
    val at: String? = null,
    val every_ms: Long? = null,
    val expr: String? = null
)

// ==================== Workspace ====================
@Serializable
data class WorkspaceResponse(
    val files: List<WorkspaceFile> = emptyList(),
    val total: Int = 0,
    val base_path: String = ""
)

@Serializable
data class WorkspaceFile(
    val path: String = "",
    val is_dir: Boolean = false,
    val size: Long = 0,
    val modified: String = ""  // 服务端返回 time.Time 的 JSON 字符串或数字
)

@Serializable
data class FileContent(
    val path: String = "",
    val content: String = "",
    val size: Long = 0
)

@Serializable
data class FileUpdateRequest(
    val path: String,
    val content: String
)

// ==================== Status ====================
@Serializable
data class StatusResponse(
    val timestamp: Long = 0,
    val model: String = "",
    val features: StatusFeatures? = null,
    val counts: StatusCounts? = null
)

@Serializable
data class StatusFeatures(
    val scheduler: Boolean = false,
    val heartbeat: Boolean = false,
    val hooks: Boolean = false,
    val subagent: Boolean = false,
    val sandbox: Boolean = false,
    val feishu: Boolean = false,
    val telegram: Boolean = false
)

@Serializable
data class StatusCounts(
    val skills: Int = 0,
    val mcp_servers: Int = 0,
    val agents: Int = 0,
    val hooks: Int = 0,
    val cron_jobs: Int = 0
)

// ==================== Sessions ====================
@Serializable
data class SessionsResponse(
    val sessions: List<SessionInfo> = emptyList(),
    val total: Int = 0
)

@Serializable
data class SessionInfo(
    val key: String = "",
    val messages: Int = 0
)
