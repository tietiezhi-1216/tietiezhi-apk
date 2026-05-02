package com.tietiezhi.apk.data.remote.api

import com.tietiezhi.apk.data.remote.dto.management.*
import retrofit2.http.*

interface ManagementApi {
    // Config
    @GET("v1/config")
    suspend fun getConfig(): ConfigResponse

    @PUT("v1/config")
    suspend fun updateConfig(@Body request: ConfigUpdateRequest): ConfigResponse

    // Skills
    @GET("v1/skills")
    suspend fun listSkills(): List<SkillInfo>

    @POST("v1/skills/load")
    suspend fun loadSkill(@Body request: SkillLoadRequest): Map<String, String>

    // MCP
    @GET("v1/mcp")
    suspend fun listMcpServers(): List<McpServer>

    // Agents
    @GET("v1/agents")
    suspend fun listAgents(): List<AgentInfo>

    @DELETE("v1/agents/{id}")
    suspend fun killAgent(@Path("id") id: String): Map<String, String>

    // Hooks
    @GET("v1/hooks")
    suspend fun listHooks(): List<HookRule>

    // Cron
    @GET("v1/cron")
    suspend fun listCronJobs(): List<CronJob>

    @POST("v1/cron")
    suspend fun createCronJob(@Body request: CronCreateRequest): CronJob

    @DELETE("v1/cron/{id}")
    suspend fun deleteCronJob(@Path("id") id: String): Map<String, String>

    // Workspace
    @GET("v1/workspace")
    suspend fun listWorkspace(@Query("path") path: String = ""): List<WorkspaceFile>

    @GET("v1/workspace/file")
    suspend fun getFile(@Query("path") path: String): FileContent

    @PUT("v1/workspace/file")
    suspend fun updateFile(@Body request: FileUpdateRequest): Map<String, String>

    // Status
    @GET("v1/status")
    suspend fun getStatus(): StatusResponse

    // Models
    @GET("v1/models")
    suspend fun listModels(): Map<String, List<Map<String, String>>>

    // Sessions
    @GET("v1/sessions")
    suspend fun listSessions(): List<SessionInfo>

    @POST("v1/sessions")
    suspend fun createSession(@Body request: Map<String, String>): SessionInfo
}
