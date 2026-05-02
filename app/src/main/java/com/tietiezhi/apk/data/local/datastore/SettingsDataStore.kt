package com.tietiezhi.apk.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext val ctx: Context) {
    private object K {
        val SERVER = stringPreferencesKey("server_address")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model_name")
        val CHEAP_MODEL = stringPreferencesKey("cheap_model")
        val LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val STREAMING = booleanPreferencesKey("streaming")
        val THEME = intPreferencesKey("theme_mode")
        val LOCAL_MODE = booleanPreferencesKey("local_mode")
        val LOCAL_PORT = intPreferencesKey("local_port")
        val FEATURE_CRON = booleanPreferencesKey("feature_cron")
        val FEATURE_HOOK = booleanPreferencesKey("feature_hook")
        val FEATURE_AGENT = booleanPreferencesKey("feature_agent")
        val FEATURE_SANDBOX = booleanPreferencesKey("feature_sandbox")
        val FEATURE_HEARTBEAT = booleanPreferencesKey("feature_heartbeat")
    }

    private fun <T> Flow<Preferences>.get(key: Preferences.Key<T>, default: T): Flow<T> =
        catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[key] ?: default }

    val serverAddress: Flow<String> = ctx.dataStore.data.get(K.SERVER, "http://localhost:18178")
    val apiKey: Flow<String> = ctx.dataStore.data.get(K.API_KEY, "")
    val modelName: Flow<String> = ctx.dataStore.data.get(K.MODEL, "default")
    val cheapModel: Flow<String> = ctx.dataStore.data.get(K.CHEAP_MODEL, "")
    val llmBaseUrl: Flow<String> = ctx.dataStore.data.get(K.LLM_BASE_URL, "")
    val llmApiKey: Flow<String> = ctx.dataStore.data.get(K.LLM_API_KEY, "")
    val llmModel: Flow<String> = ctx.dataStore.data.get(K.LLM_MODEL, "")
    val streaming: Flow<Boolean> = ctx.dataStore.data.get(K.STREAMING, true)
    val themeMode: Flow<Int> = ctx.dataStore.data.get(K.THEME, 0)
    val localMode: Flow<Boolean> = ctx.dataStore.data.get(K.LOCAL_MODE, true)
    val localPort: Flow<Int> = ctx.dataStore.data.get(K.LOCAL_PORT, 18178)
    val featureCron: Flow<Boolean> = ctx.dataStore.data.get(K.FEATURE_CRON, true)
    val featureHook: Flow<Boolean> = ctx.dataStore.data.get(K.FEATURE_HOOK, true)
    val featureAgent: Flow<Boolean> = ctx.dataStore.data.get(K.FEATURE_AGENT, true)
    val featureSandbox: Flow<Boolean> = ctx.dataStore.data.get(K.FEATURE_SANDBOX, true)
    val featureHeartbeat: Flow<Boolean> = ctx.dataStore.data.get(K.FEATURE_HEARTBEAT, true)

    suspend fun setServer(v: String) { ctx.dataStore.edit { it[K.SERVER] = v } }
    suspend fun setApiKey(v: String) { ctx.dataStore.edit { it[K.API_KEY] = v } }
    suspend fun setModel(v: String) { ctx.dataStore.edit { it[K.MODEL] = v } }
    suspend fun setCheapModel(v: String) { ctx.dataStore.edit { it[K.CHEAP_MODEL] = v } }
    suspend fun setLlmBaseUrl(v: String) { ctx.dataStore.edit { it[K.LLM_BASE_URL] = v } }
    suspend fun setLlmApiKey(v: String) { ctx.dataStore.edit { it[K.LLM_API_KEY] = v } }
    suspend fun setLlmModel(v: String) { ctx.dataStore.edit { it[K.LLM_MODEL] = v } }
    suspend fun setStreaming(v: Boolean) { ctx.dataStore.edit { it[K.STREAMING] = v } }
    suspend fun setTheme(v: Int) { ctx.dataStore.edit { it[K.THEME] = v } }
    suspend fun setLocalMode(v: Boolean) { ctx.dataStore.edit { it[K.LOCAL_MODE] = v } }
    suspend fun setLocalPort(v: Int) { ctx.dataStore.edit { it[K.LOCAL_PORT] = v } }
    suspend fun setFeatureCron(v: Boolean) { ctx.dataStore.edit { it[K.FEATURE_CRON] = v } }
    suspend fun setFeatureHook(v: Boolean) { ctx.dataStore.edit { it[K.FEATURE_HOOK] = v } }
    suspend fun setFeatureAgent(v: Boolean) { ctx.dataStore.edit { it[K.FEATURE_AGENT] = v } }
    suspend fun setFeatureSandbox(v: Boolean) { ctx.dataStore.edit { it[K.FEATURE_SANDBOX] = v } }
    suspend fun setFeatureHeartbeat(v: Boolean) { ctx.dataStore.edit { it[K.FEATURE_HEARTBEAT] = v } }
}
