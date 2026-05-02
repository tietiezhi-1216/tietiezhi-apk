package com.tietiezhi.apk.data.remote.dto.management

import kotlinx.serialization.Serializable

// Config
@Serializable
data class ConfigResponse(
    val llm: LlmConfig? = null,
    val server: ServerConfig? = null,
    val features: FeaturesConfig? = null
)

@Serializable
data class LlmConfig(
    val base_url: String = "",
    val api_key: String = "",
    val model: String = "",
    val cheap_model: String = ""
)

@Serializable
data class ServerConfig(
    val port: Int = 18178,
    val host: String = "localhost"
)

@Serializable
data class FeaturesConfig(
    val cron: Boolean = true,
    val hook: Boolean = true,
    val agent: Boolean = true,
    val sandbox: Boolean = true,
    val heartbeat: Boolean = true
)

@Serializable
data class ConfigUpdateRequest(
    val llm: LlmConfigUpdate? = null,
    val features: FeaturesConfigUpdate? = null
)

@Serializable
data class LlmConfigUpdate(
    val base_url: String? = null,
    val api_key: String? = null,
    val model: String? = null,
    val cheap_model: String? = null
)

@Serializable
data class FeaturesConfigUpdate(
    val cron: Boolean? = null,
    val hook: Boolean? = null,
    val agent: Boolean? = null,
    val sandbox: Boolean? = null,
    val heartbeat: Boolean? = null
)

// Skills
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

// MCP
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

// Agents
@Serializable
data class AgentInfo(
    val spawn_id: String = "",
    val session_key: String = "",
    val status: String = "",
    val label: String = "",
    val started_at: String = ""
)

// Hooks
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

// Cron
@Serializable
data class CronJob(
    val id: String = "",
    val name: String = "",
    val message: String = "",
    val schedule: String = "",
    val enabled: Boolean = true,
    val mode: String = "agent",
    val run_count: Int = 0
)

@Serializable
data class CronCreateRequest(
    val name: String,
    val message: String,
    val kind: String = "interval",
    val at: String? = null,
    val every_ms: Long? = null,
    val expr: String? = null
)

// Workspace
@Serializable
data class WorkspaceFile(
    val path: String = "",
    val is_dir: Boolean = false,
    val size: Long = 0,
    val modified: Long = 0
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

// Status
@Serializable
data class StatusResponse(
    val model: String = "",
    val features: FeaturesConfig? = null,
    val counts: CountsInfo? = null
)

@Serializable
data class CountsInfo(
    val skills: Int = 0,
    val agents: Int = 0,
    val hooks: Int = 0,
    val cron: Int = 0
)

// Sessions
@Serializable
data class SessionInfo(
    val session_key: String = "",
    val chat_id: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)
