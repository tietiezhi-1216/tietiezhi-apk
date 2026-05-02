package com.tietiezhi.apk.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    var apiKey: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = if (apiKey.isNotBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .build()
        } else chain.request()
        return chain.proceed(req)
    }
}
