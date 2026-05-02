package com.tietiezhi.apk.data.remote.api

import com.tietiezhi.apk.data.remote.dto.management.*
import retrofit2.http.*

interface ManagementApi {
    // Config
    @GET("v1/config")
    suspend fun getConfig(): ConfigResponse

    @PUT("v1/config")
    suspend fun updateConfig(@Body request: ConfigUpdateRequest): ApiResponse

    // Skills
    @GET("v1/skills")
    suspend fun listSkills(): SkillsResponse

    @POST("v1/skills/load")
    suspend fun loadSkill(@Body request: SkillLoadRequest): ApiResponse

    // MCP
    @GET("v1/mcp")
    suspend fun listMcpServers(): McpResponse

    // Agents
    @GET("v1/agents")
    suspend fun listAgents(): AgentsResponse

    @DELETE("v1/agents/{id}")
    suspend fun killAgent(@Path("id") id: String): ApiResponse

    // Hooks
    @GET("v1/hooks")
    suspend fun listHooks(): HooksResponse

    // Cron
    @GET("v1/cron")
    suspend fun listCronJobs(): CronListResponse

    @POST("v1/cron")
    suspend fun createCronJob(@Body request: CronCreateRequest): ApiResponse

    @DELETE("v1/cron/{id}")
    suspend fun deleteCronJob(@Path("id") id: String): ApiResponse

    // Workspace
    @GET("v1/workspace")
    suspend fun listWorkspace(): WorkspaceResponse

    @GET("v1/workspace/file")
    suspend fun getFile(@Query("path") path: String): FileContent

    @PUT("v1/workspace/file")
    suspend fun updateFile(@Body request: FileUpdateRequest): ApiResponse

    // Status
    @GET("v1/status")
    suspend fun getStatus(): StatusResponse

    // Sessions
    @GET("v1/sessions")
    suspend fun listSessions(): SessionsResponse
}

// 通用 API 响应
@kotlinx.serialization.Serializable
data class ApiResponse(
    val success: Boolean = false,
    val message: String? = null
)
