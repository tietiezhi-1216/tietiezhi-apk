package com.tietiezhi.apk.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tietiezhi.terminal.emulator.Cell
import com.tietiezhi.terminal.manager.TerminalManager
import com.tietiezhi.terminal.proot.ProotManager
import com.tietiezhi.terminal.rootfs.RootfsManager
import com.tietiezhi.terminal.session.TerminalSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalUiState(
    val output: List<List<Cell>> = emptyList(),
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val isRunning: Boolean = false,
    val isProotMode: Boolean = false,
    val rootfsStatus: RootfsManager.RootfsStatus = RootfsManager.RootfsStatus.NOT_INSTALLED,
    val rootfsProgress: Float = 0f,
    val error: String? = null,
    val fontSize: Float = 14f,
    val sessionCount: Int = 1,
    val activeSessionId: Int? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val prootManager = ProotManager.getInstance(application)
    private val rootfsManager = RootfsManager.getInstance(application)
    
    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()
    
    private val _commandInput = MutableStateFlow("")
    val commandInput: StateFlow<String> = _commandInput.asStateFlow()
    
    init {
        TerminalManager.initialize(application)
        observeSessions()
        observeRootfs()
    }
    
    private fun observeSessions() {
        viewModelScope.launch {
            TerminalManager.sessionList.collect { sessions ->
                _uiState.update { it.copy(sessionCount = sessions.size) }
            }
        }
        
        viewModelScope.launch {
            TerminalManager.activeSessionId.collect { sessionId ->
                _uiState.update { it.copy(activeSessionId = sessionId) }
                
                sessionId?.let { id ->
                    TerminalManager.getSession(id)?.let { session ->
                        observeSession(session)
                    }
                }
            }
        }
    }
    
    private fun observeSession(session: TerminalSession) {
        viewModelScope.launch {
            session.sessionState.collect { state ->
                _uiState.update { it.copy(isRunning = state == TerminalSession.SessionState.RUNNING) }
            }
        }
        
        viewModelScope.launch {
            session.emulator.screenUpdates.collect { screen ->
                _uiState.update { it.copy(output = screen) }
            }
        }
        
        viewModelScope.launch {
            session.emulator.cursorPosition.collect { pos ->
                _uiState.update { it.copy(cursorRow = pos.first, cursorCol = pos.second) }
            }
        }
        
        viewModelScope.launch {
            session.errorMessage.collect { error ->
                _uiState.update { it.copy(error = error) }
            }
        }
    }
    
    private fun observeRootfs() {
        viewModelScope.launch {
            rootfsManager.state.collect { state ->
                _uiState.update { 
                    it.copy(
                        rootfsStatus = state.status,
                        rootfsProgress = state.progress,
                        isProotMode = state.status == RootfsManager.RootfsStatus.INSTALLED
                    )
                }
            }
        }
    }
    
    fun startShell() {
        val session = TerminalManager.createSession()
        TerminalManager.setActiveSession(session.sessionId)
    }
    
    fun startProot() {
        val session = prootManager.startProotSession()
        session?.let {
            TerminalManager.setActiveSession(it.sessionId)
        }
    }
    
    fun executeCommand(command: String) {
        val session = TerminalManager.activeSession ?: return
        
        if (command.isNotEmpty()) {
            session.writeToSession(command)
            session.writeToSession("\n")
        }
        
        _commandInput.value = ""
    }
    
    fun sendKey(keyCode: Int) {
        TerminalManager.activeSession?.writeToSession(keyCode)
    }
    
    fun sendText(text: String) {
        TerminalManager.activeSession?.writeToSession(text)
    }
    
    fun onCommandInputChange(input: String) {
        _commandInput.value = input
    }
    
    fun clearOutput() {
        TerminalManager.activeSession?.emulator?.let { emulator ->
            // Clear the terminal screen
            emulator.reset()
        }
    }
    
    fun setFontSize(size: Float) {
        _uiState.update { it.copy(fontSize = size) }
    }
    
    fun increaseFontSize() {
        val current = _uiState.value.fontSize
        if (current < 24f) {
            setFontSize(current + 2f)
        }
    }
    
    fun decreaseFontSize() {
        val current = _uiState.value.fontSize
        if (current > 8f) {
            setFontSize(current - 2f)
        }
    }
    
    // Session management
    fun newSession() {
        startShell()
    }
    
    fun newProotSession() {
        startProot()
    }
    
    fun switchToSession(sessionId: Int) {
        TerminalManager.setActiveSession(sessionId)
    }
    
    fun closeSession(sessionId: Int) {
        TerminalManager.closeSession(sessionId)
    }
    
    fun nextSession() {
        TerminalManager.nextSession()
    }
    
    fun previousSession() {
        TerminalManager.previousSession()
    }
    
    // Rootfs management
    fun installRootfs() {
        viewModelScope.launch {
            rootfsManager.downloadAndInstall()
        }
    }
    
    fun downloadRootfs() {
        viewModelScope.launch {
            rootfsManager.downloadAndInstall()
        }
    }
    
    fun resizeTerminal(columns: Int, rows: Int) {
        TerminalManager.activeSession?.resize(columns, rows)
    }
    
    override fun onCleared() {
        super.onCleared()
        TerminalManager.closeAllSessions()
    }
}
