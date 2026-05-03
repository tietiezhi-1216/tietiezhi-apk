package com.tietiezhi.terminal.session

import android.os.Build
import com.tietiezhi.terminal.emulator.TerminalEmulator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Terminal session that manages a shell process and terminal emulator
 */
class TerminalSession(
    private val shellPath: String = "/system/bin/sh",
    private val workingDirectory: String = "/",
    private val environment: Map<String, String> = emptyMap(),
    private val processArgs: List<String> = emptyList()
) {
    enum class SessionState {
        STARTING,
        RUNNING,
        PAUSED,
        FINISHED,
        ERROR
    }

    private var mProcess: Process? = null
    private var mEmulator: TerminalEmulator = TerminalEmulator()
    private var mOutputJob: Job? = null
    private var mProcessExitJob: Job? = null
    
    private val mSessionId = nextSessionId()
    
    private val _sessionState = MutableStateFlow(SessionState.STARTING)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _processExitCode = MutableStateFlow<Int?>(null)
    val processExitCode: StateFlow<Int?> = _processExitCode.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val emulator: TerminalEmulator get() = mEmulator
    val sessionId: Int get() = mSessionId
    
    private val mPtyInputStream: PipedInputStream = PipedInputStream(8192)
    private val mPtyOutputStream: PipedOutputStream = PipedOutputStream()
    
    private val mIsSessionRunning = AtomicBoolean(false)
    
    init {
        try {
            mPtyInputStream.connect(mPtyOutputStream)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to create PTY: ${e.message}"
            _sessionState.value = SessionState.ERROR
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                _sessionState.value = SessionState.STARTING
                
                val builder = ProcessBuilder()
                val env = builder.environment()
                
                // Set environment variables
                env["TERM"] = "xterm-256color"
                env["COLORTERM"] = "truecolor"
                env["TERM_PROGRAM"] = "tietiezhi-terminal"
                env["PWD"] = workingDirectory
                env["HOME"] = workingDirectory
                
                // Add custom environment
                environment.forEach { (key, value) ->
                    env[key] = value
                }
                
                // Build command
                val cmd = mutableListOf(shellPath)
                if (processArgs.isNotEmpty()) {
                    cmd.addAll(processArgs)
                }
                
                builder.command(cmd)
                builder.directory(java.io.File(workingDirectory))
                builder.redirectErrorStream(false)
                
                // Start process
                mProcess = builder.start()
                mIsSessionRunning.set(true)
                _sessionState.value = SessionState.RUNNING
                
                // Handle output from process
                mOutputJob = launch {
                    try {
                        val inputStream = mProcess!!.inputStream
                        val buffer = ByteArray(4096)
                        while (isActive && mIsSessionRunning.get()) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead > 0) {
                                mEmulator.write(buffer, 0, bytesRead)
                            } else if (bytesRead == -1) {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        if (mIsSessionRunning.get()) {
                            _errorMessage.value = "Output error: ${e.message}"
                        }
                    }
                }
                
                // Handle stderr
                launch {
                    try {
                        val errorStream = mProcess!!.errorStream
                        val buffer = ByteArray(4096)
                        while (isActive && mIsSessionRunning.get()) {
                            val bytesRead = errorStream.read(buffer)
                            if (bytesRead > 0) {
                                // Write stderr to emulator (typically shown in red)
                                mEmulator.write(buffer, 0, bytesRead)
                            } else if (bytesRead == -1) {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore stderr errors
                    }
                }
                
                // Write to process from PTY
                launch {
                    try {
                        val outputStream = mProcess!!.outputStream
                        val buffer = ByteArray(4096)
                        while (isActive && mIsSessionRunning.get()) {
                            val bytesRead = mPtyInputStream.read(buffer)
                            if (bytesRead > 0) {
                                outputStream.write(buffer, 0, bytesRead)
                                outputStream.flush()
                            } else if (bytesRead == -1) {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        if (mIsSessionRunning.get()) {
                            _errorMessage.value = "Input error: ${e.message}"
                        }
                    }
                }
                
                // Wait for process to exit
                mProcessExitJob = launch {
                    val exitCode = mProcess!!.waitFor()
                    mIsSessionRunning.set(false)
                    _processExitCode.value = exitCode
                    _sessionState.value = SessionState.FINISHED
                    mEmulator.write("\r\n[Process exited with code $exitCode]\r\n")
                }
                
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _sessionState.value = SessionState.ERROR
            }
        }
    }

    fun getMasterInputStream(): InputStream = mPtyInputStream
    fun getMasterOutputStream(): OutputStream = mPtyOutputStream

    fun writeToSession(data: ByteArray) {
        if (_sessionState.value == SessionState.RUNNING) {
            try {
                mPtyOutputStream.write(data)
                mPtyOutputStream.flush()
            } catch (e: Exception) {
                _errorMessage.value = "Write error: ${e.message}"
            }
        }
    }

    fun writeToSession(text: String) {
        writeToSession(text.toByteArray())
    }

    fun writeToSession(keyCode: Int) {
        val bytes = when (keyCode) {
            TerminalEmulator.KEY_CODE_ESCAPE -> byteArrayOf(0x1B)
            TerminalEmulator.KEY_CODE_TAB -> byteArrayOf(0x09)
            TerminalEmulator.KEY_CODE_ENTER -> byteArrayOf(0x0D)
            TerminalEmulator.KEY_CODE_BACKSPACE -> byteArrayOf(0x7F)
            TerminalEmulator.KEY_CODE_ARROW_UP -> byteArrayOf(0x1B, 0x5B, 0x41)
            TerminalEmulator.KEY_CODE_ARROW_DOWN -> byteArrayOf(0x1B, 0x5B, 0x42)
            TerminalEmulator.KEY_CODE_ARROW_RIGHT -> byteArrayOf(0x1B, 0x5B, 0x43)
            TerminalEmulator.KEY_CODE_ARROW_LEFT -> byteArrayOf(0x1B, 0x5B, 0x44)
            TerminalEmulator.KEY_CODE_F1 -> byteArrayOf(0x1B, 0x4F, 0x50)
            TerminalEmulator.KEY_CODE_F2 -> byteArrayOf(0x1B, 0x4F, 0x51)
            TerminalEmulator.KEY_CODE_F3 -> byteArrayOf(0x1B, 0x4F, 0x52)
            TerminalEmulator.KEY_CODE_F4 -> byteArrayOf(0x1B, 0x4F, 0x53)
            TerminalEmulator.KEY_CODE_F5 -> byteArrayOf(0x1B, 0x5B, 0x31, 0x35, 0x7E)
            TerminalEmulator.KEY_CODE_F6 -> byteArrayOf(0x1B, 0x5B, 0x31, 0x37, 0x7E)
            TerminalEmulator.KEY_CODE_F7 -> byteArrayOf(0x1B, 0x5B, 0x31, 0x38, 0x7E)
            TerminalEmulator.KEY_CODE_F8 -> byteArrayOf(0x1B, 0x5B, 0x31, 0x39, 0x7E)
            TerminalEmulator.KEY_CODE_F9 -> byteArrayOf(0x1B, 0x5B, 0x32, 0x30, 0x7E)
            TerminalEmulator.KEY_CODE_F10 -> byteArrayOf(0x1B, 0x5B, 0x32, 0x31, 0x7E)
            TerminalEmulator.KEY_CODE_F11 -> byteArrayOf(0x1B, 0x5B, 0x32, 0x33, 0x7E)
            TerminalEmulator.KEY_CODE_F12 -> byteArrayOf(0x1B, 0x5B, 0x32, 0x34, 0x7E)
            TerminalEmulator.KEY_CODE_HOME -> byteArrayOf(0x1B, 0x5B, 0x48)
            TerminalEmulator.KEY_CODE_END -> byteArrayOf(0x1B, 0x5B, 0x46)
            TerminalEmulator.KEY_CODE_PAGE_UP -> byteArrayOf(0x1B, 0x5B, 0x35, 0x7E)
            TerminalEmulator.KEY_CODE_PAGE_DOWN -> byteArrayOf(0x1B, 0x5B, 0x36, 0x7E)
            TerminalEmulator.KEY_CODE_INSERT -> byteArrayOf(0x1B, 0x5B, 0x32, 0x7E)
            TerminalEmulator.KEY_CODE_DELETE -> byteArrayOf(0x1B, 0x5B, 0x33, 0x7E)
            else -> {
                if (keyCode and TerminalEmulator.KEY_MODIFIER_CTRL != 0) {
                    val baseCode = keyCode and 0xFF
                    byteArrayOf((baseCode - 64).toByte())
                } else {
                    byteArrayOf(keyCode.toByte())
                }
            }
        }
        writeToSession(bytes)
    }

    fun resize(columns: Int, rows: Int) {
        if (columns <= 0 || rows <= 0) return
        mEmulator.resize(columns, rows)
    }

    fun suspendSession() {
        if (_sessionState.value == SessionState.RUNNING) {
            try {
                mProcess?.destroy()
                _sessionState.value = SessionState.PAUSED
            } catch (e: Exception) {
                _errorMessage.value = "Suspend error: ${e.message}"
            }
        }
    }

    fun resumeSession(scope: CoroutineScope) {
        if (_sessionState.value == SessionState.PAUSED) {
            start(scope)
        }
    }

    fun finish() {
        mIsSessionRunning.set(false)
        _sessionState.value = SessionState.FINISHED
        
        try {
            mProcess?.destroy()
        } catch (_: Exception) {}
        
        try {
            mPtyInputStream.close()
        } catch (_: Exception) {}
        
        try {
            mPtyOutputStream.close()
        } catch (_: Exception) {}
        
        mOutputJob?.cancel()
        mProcessExitJob?.cancel()
    }

    val isRunning: Boolean
        get() = _sessionState.value == SessionState.RUNNING

    val isFinished: Boolean
        get() = _sessionState.value == SessionState.FINISHED

    companion object {
        private val sessionCounter = AtomicInteger(1)
        
        private fun nextSessionId(): Int = sessionCounter.getAndIncrement()
    }
}
