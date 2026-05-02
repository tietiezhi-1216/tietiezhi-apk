package com.tietiezhi.apk.ui.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import com.tietiezhi.apk.data.remote.api.ManagementApi
import com.tietiezhi.apk.data.remote.dto.management.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeaturesUiState(
    val skills: List<SkillInfo> = emptyList(),
    val mcpServers: List<McpServer> = emptyList(),
    val agents: List<AgentInfo> = emptyList(),
    val hooks: List<HookRule> = emptyList(),
    val cronJobs: List<CronJob> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "http://localhost:18178"
)

@HiltViewModel
class FeaturesViewModel @Inject constructor(
    private val managementApi: ManagementApi,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeaturesUiState())
    val uiState: StateFlow<FeaturesUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.serverAddress,
                settingsDataStore.localPort,
                settingsDataStore.localMode
            ) { addr, port, localMode ->
                if (localMode) "http://localhost:$port" else addr
            }.collect { url ->
                _uiState.update { it.copy(serverUrl = url) }
            }
        }
    }
    
    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val skills = managementApi.listSkills()
                val mcpServers = managementApi.listMcpServers()
                val agents = managementApi.listAgents()
                val hooks = managementApi.listHooks()
                val cronJobs = managementApi.listCronJobs()
                _uiState.update { 
                    it.copy(
                        skills = skills,
                        mcpServers = mcpServers,
                        agents = agents,
                        hooks = hooks,
                        cronJobs = cronJobs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun loadSkill(name: String) {
        viewModelScope.launch {
            try {
                managementApi.loadSkill(SkillLoadRequest(name))
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun killAgent(id: String) {
        viewModelScope.launch {
            try {
                managementApi.killAgent(id)
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun deleteCronJob(id: String) {
        viewModelScope.launch {
            try {
                managementApi.deleteCronJob(id)
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun createCronJob(name: String, message: String, everyMs: Long) {
        viewModelScope.launch {
            try {
                managementApi.createCronJob(
                    CronCreateRequest(
                        name = name,
                        message = message,
                        kind = "interval",
                        every_ms = everyMs
                    )
                )
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
