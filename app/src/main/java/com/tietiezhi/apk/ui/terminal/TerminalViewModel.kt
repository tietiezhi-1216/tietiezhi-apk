package com.tietiezhi.apk.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.apk.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import javax.inject.Inject

data class TerminalUiState(
    val output: String = "",
    val isRunning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()
    
    private var process: Process? = null
    private var outputReader: BufferedReader? = null
    private var errorReader: BufferedReader? = null
    private var inputWriter: PrintWriter? = null
    
    init {
        startShell()
    }
    
    private fun startShell() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRunning = true, output = "欢迎使用终端\n", error = null) }
                
                withContext(Dispatchers.IO) {
                    val processBuilder = ProcessBuilder("sh")
                    processBuilder.redirectErrorStream(false)
                    process = processBuilder.start()
                    
                    outputReader = BufferedReader(InputStreamReader(process!!.inputStream))
                    errorReader = BufferedReader(InputStreamReader(process!!.errorStream))
                    inputWriter = PrintWriter(process!!.outputStream, true)
                    
                    // 读取输出
                    launch {
                        try {
                            var line: String? = outputReader?.readLine()
                            while (_uiState.value.isRunning && line != null) {
                                _uiState.update { state ->
                                    state.copy(output = state.output + line + "\n")
                                }
                                line = outputReader?.readLine()
                            }
                        } catch (_: Exception) {}
                    }
                    
                    // 读取错误
                    launch {
                        try {
                            var line: String? = errorReader?.readLine()
                            while (_uiState.value.isRunning && line != null) {
                                _uiState.update { state ->
                                    state.copy(output = state.output + "\u001B[31m$line\u001B[0m\n")
                                }
                                line = errorReader?.readLine()
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRunning = false, error = e.message) }
            }
        }
    }
    
    fun executeCommand(command: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(output = it.output + "\$ $command\n") }
                withContext(Dispatchers.IO) {
                    inputWriter?.println(command)
                    inputWriter?.flush()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(output = it.output + "错误: ${e.message}\n") }
            }
        }
    }
    
    fun clearOutput() {
        _uiState.update { it.copy(output = "") }
    }
    
    override fun onCleared() {
        super.onCleared()
        _uiState.update { it.copy(isRunning = false) }
        try {
            process?.destroy()
            outputReader?.close()
            errorReader?.close()
            inputWriter?.close()
        } catch (_: Exception) {}
    }
}
