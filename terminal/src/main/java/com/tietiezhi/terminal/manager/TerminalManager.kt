package com.tietiezhi.terminal.manager

import android.content.Context
import com.tietiezhi.terminal.session.TerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * TerminalManager - Singleton managing terminal sessions
 */
object TerminalManager {
    private const val TAG = "TerminalManager"
    
    private var mApplicationContext: Context? = null
    private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val mSessions = mutableMapOf<Int, TerminalSession>()
    private val mActiveSessionId = MutableStateFlow<Int?>(null)
    private val mNextSessionId = java.util.concurrent.atomic.AtomicInteger(1)
    
    val activeSessionId: StateFlow<Int?> = mActiveSessionId.asStateFlow()
    
    val activeSession: TerminalSession?
        get() = mActiveSessionId.value?.let { mSessions[it] }
    
    val allSessions: List<TerminalSession>
        get() = mSessions.values.toList()
    
    data class SessionInfo(
        val id: Int,
        val session: TerminalSession,
        val title: String = "Terminal $id"
    )
    
    val sessionList: StateFlow<List<SessionInfo>> = mActiveSessionId.map { activeId ->
        mSessions.map { (id, session) ->
            SessionInfo(
                id = id,
                session = session,
                title = "Terminal $id"
            )
        }
    }.stateIn(mScope, SharingStarted.Eagerly, emptyList())
    
    fun initialize(context: Context) {
        mApplicationContext = context.applicationContext
    }
    
    fun createSession(
        shellPath: String = "/system/bin/sh",
        workingDirectory: String = "/",
        environment: Map<String, String> = emptyMap(),
        args: List<String> = emptyList()
    ): TerminalSession {
        val session = TerminalSession(
            shellPath = shellPath,
            workingDirectory = workingDirectory,
            environment = environment,
            processArgs = args
        )
        
        val sessionId = mNextSessionId.getAndIncrement()
        mSessions[sessionId] = session
        
        // Auto-start the session
        session.start(mScope)
        
        // Set as active if this is the first session
        if (mActiveSessionId.value == null) {
            mActiveSessionId.value = sessionId
        }
        
        return session
    }
    
    fun createProotSession(
        rootfsPath: String,
        prootPath: String,
        workingDirectory: String = "/root"
    ): TerminalSession {
        val environment = mapOf(
            "HOME" to workingDirectory,
            "PWD" to workingDirectory,
            "TERM" to "xterm-256color"
        )
        
        val args = listOf(
            "-r", rootfsPath,
            "-w", workingDirectory,
            "-b", "/proc:/proc",
            "-b", "/sys:/sys",
            "-b", "/dev:/dev",
            "--kill-on-exit",
            "/bin/bash", "--login"
        )
        
        return createSession(
            shellPath = prootPath,
            workingDirectory = workingDirectory,
            environment = environment,
            args = args
        )
    }
    
    fun setActiveSession(sessionId: Int) {
        if (mSessions.containsKey(sessionId)) {
            mActiveSessionId.value = sessionId
        }
    }
    
    fun closeSession(sessionId: Int) {
        mSessions[sessionId]?.let { session ->
            session.finish()
            mSessions.remove(sessionId)
            
            // If closing active session, switch to another
            if (mActiveSessionId.value == sessionId) {
                mActiveSessionId.value = mSessions.keys.firstOrNull()
            }
        }
    }
    
    fun closeAllSessions() {
        mSessions.values.forEach { it.finish() }
        mSessions.clear()
        mActiveSessionId.value = null
    }
    
    fun getSession(sessionId: Int): TerminalSession? = mSessions[sessionId]
    
    fun nextSession() {
        val current = mActiveSessionId.value ?: return
        val keys = mSessions.keys.toList()
        val currentIndex = keys.indexOf(current)
        val nextIndex = (currentIndex + 1) % keys.size
        mActiveSessionId.value = keys[nextIndex]
    }
    
    fun previousSession() {
        val current = mActiveSessionId.value ?: return
        val keys = mSessions.keys.toList()
        val currentIndex = keys.indexOf(current)
        val prevIndex = if (currentIndex <= 0) keys.size - 1 else currentIndex - 1
        mActiveSessionId.value = keys[prevIndex]
    }
    
    fun splitSession(): TerminalSession {
        // Create a new session in the same directory as current
        val currentSession = activeSession
        val workingDir = currentSession?.emulator?.let { 
            // Get current working directory from emulator state
            "/" 
        } ?: "/"
        
        return createSession(workingDirectory = workingDir)
    }
    
    // Cleanup on app termination
    fun cleanup() {
        closeAllSessions()
        mApplicationContext = null
    }
}
