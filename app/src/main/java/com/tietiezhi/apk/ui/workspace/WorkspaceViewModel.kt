package com.tietiezhi.apk.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import com.tietiezhi.apk.data.remote.api.ManagementApi
import com.tietiezhi.apk.data.remote.dto.management.FileContent
import com.tietiezhi.apk.data.remote.dto.management.FileUpdateRequest
import com.tietiezhi.apk.data.remote.dto.management.WorkspaceFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceUiState(
    val files: List<WorkspaceFile> = emptyList(),
    val currentPath: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class EditorUiState(
    val fileContent: FileContent? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val managementApi: ManagementApi,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()
    
    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()
    
    init {
        loadWorkspace("")
    }
    
    fun loadWorkspace(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = managementApi.listWorkspace()
                // 过滤当前路径的文件
                val files = if (path.isEmpty()) {
                    response.files.filter { !it.path.contains("/") }
                } else {
                    response.files.filter { it.path.startsWith("$path/") && !it.path.substringAfter("$path/").contains("/") }
                }
                _uiState.update { it.copy(files = files, currentPath = path, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun navigateTo(path: String) {
        loadWorkspace(path)
    }
    
    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current.isNotEmpty()) {
            val parent = current.substringBeforeLast('/', "")
            loadWorkspace(parent)
        }
    }
    
    fun loadFile(path: String) {
        viewModelScope.launch {
            _editorState.update { it.copy(isLoading = true, error = null) }
            try {
                val content = managementApi.getFile(path)
                _editorState.update { it.copy(fileContent = content, isLoading = false) }
            } catch (e: Exception) {
                _editorState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun saveFile(path: String, content: String) {
        viewModelScope.launch {
            _editorState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            try {
                managementApi.updateFile(FileUpdateRequest(path, content))
                _editorState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _editorState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
    
    fun clearEditorState() {
        _editorState.update { EditorUiState() }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _editorState.update { it.copy(error = null) }
    }
}
