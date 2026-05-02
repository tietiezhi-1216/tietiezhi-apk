package com.tietiezhi.apk.data.remote.api

import com.tietiezhi.apk.data.remote.dto.*
import retrofit2.http.*

interface ChatApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse

    @GET("v1/models")
    suspend fun listModels(): ModelsResponse
}
