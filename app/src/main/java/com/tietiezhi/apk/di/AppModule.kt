package com.tietiezhi.apk.di

import android.content.Context
import androidx.room.Room
import com.tietiezhi.apk.data.local.db.AppDatabase
import com.tietiezhi.apk.data.local.db.dao.ChatDao
import com.tietiezhi.apk.data.local.db.dao.MessageDao
import com.tietiezhi.apk.data.remote.api.ChatApi
import com.tietiezhi.apk.data.remote.api.ManagementApi
import com.tietiezhi.apk.data.remote.interceptor.AuthInterceptor
import com.tietiezhi.apk.data.remote.interceptor.BaseUrlInterceptor
import com.tietiezhi.apk.data.repository.ChatRepositoryImpl
import com.tietiezhi.apk.domain.repository.ChatRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides @Singleton
    fun provideBaseUrlInterceptor(): BaseUrlInterceptor = BaseUrlInterceptor()

    @Provides @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor()

    @Provides @Singleton
    fun provideOkHttpClient(auth: AuthInterceptor, baseUrl: BaseUrlInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(baseUrl)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val factory = json.asConverterFactory("application/json".toMediaType())
        return Retrofit.Builder()
            .baseUrl("http://localhost:18178/")  // placeholder, BaseUrlInterceptor will override
            .client(client)
            .addConverterFactory(factory)
            .build()
    }

    @Provides @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides @Singleton
    fun provideManagementApi(retrofit: Retrofit): ManagementApi = retrofit.create(ManagementApi::class.java)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "tietiezhi.db").build()

    @Provides fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
    @Provides fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides @Singleton
    fun provideChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl
}
