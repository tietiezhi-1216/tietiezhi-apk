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
        val STREAMING = booleanPreferencesKey("streaming")
        val THEME = intPreferencesKey("theme_mode")
        val LOCAL_MODE = booleanPreferencesKey("local_mode")
        val LOCAL_PORT = intPreferencesKey("local_port")
    }

    private fun <T> Flow<Preferences>.get(key: Preferences.Key<T>, default: T): Flow<T> =
        catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[key] ?: default }

    val serverAddress: Flow<String> = ctx.dataStore.data.get(K.SERVER, "http://localhost:18178")
    val apiKey: Flow<String> = ctx.dataStore.data.get(K.API_KEY, "")
    val modelName: Flow<String> = ctx.dataStore.data.get(K.MODEL, "default")
    val streaming: Flow<Boolean> = ctx.dataStore.data.get(K.STREAMING, true)
    val themeMode: Flow<Int> = ctx.dataStore.data.get(K.THEME, 0)
    val localMode: Flow<Boolean> = ctx.dataStore.data.get(K.LOCAL_MODE, true)
    val localPort: Flow<Int> = ctx.dataStore.data.get(K.LOCAL_PORT, 18178)

    suspend fun setServer(v: String) { ctx.dataStore.edit { it[K.SERVER] = v } }
    suspend fun setApiKey(v: String) { ctx.dataStore.edit { it[K.API_KEY] = v } }
    suspend fun setModel(v: String) { ctx.dataStore.edit { it[K.MODEL] = v } }
    suspend fun setStreaming(v: Boolean) { ctx.dataStore.edit { it[K.STREAMING] = v } }
    suspend fun setTheme(v: Int) { ctx.dataStore.edit { it[K.THEME] = v } }
    suspend fun setLocalMode(v: Boolean) { ctx.dataStore.edit { it[K.LOCAL_MODE] = v } }
    suspend fun setLocalPort(v: Int) { ctx.dataStore.edit { it[K.LOCAL_PORT] = v } }
}
