package com.tietiezhi.terminal.proot

import android.content.Context
import com.tietiezhi.terminal.rootfs.RootfsManager
import com.tietiezhi.terminal.session.TerminalSession
import com.tietiezhi.terminal.manager.TerminalManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ProotManager - Manages PRoot environment for rootless chroot
 * 
 * PRoot allows user-space implementation of chroot, mount --bind, etc.
 * This enables running a full Linux distribution without root access.
 */
class ProotManager(private val context: Context) {
    
    enum class ProotStatus {
        NOT_READY,
        INSTALLING,
        READY,
        RUNNING,
        ERROR
    }
    
    data class ProotState(
        val status: ProotStatus = ProotStatus.NOT_READY,
        val prootPath: String? = null,
        val rootfsPath: String? = null,
        val error: String? = null
    )
    
    private val _state = MutableStateFlow(ProotState())
    val state: StateFlow<ProotState> = _state.asStateFlow()
    
    private val rootfsManager = RootfsManager.getInstance(context)
    
    private var currentSession: TerminalSession? = null
    
    // PRoot binary and library directory
    private val prootDir = File(context.filesDir, "proot")
    private val prootBinary = File(prootDir, "proot")
    private val libDir = File(prootDir, "lib")
    
    companion object {
        private const val PROOT_ASSET_DIR = "proot/arm64-v8a"
        private const val PROOT_BINARY = "proot"
        private const val LIBTALLOC = "libtalloc.so.2"
        
        @Volatile
        private var instance: ProotManager? = null
        
        fun getInstance(context: Context): ProotManager {
            return instance ?: synchronized(this) {
                instance ?: ProotManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        checkStatus()
    }
    
    fun checkStatus() {
        val rootfsState = rootfsManager.state.value
        
        _state.value = _state.value.copy(
            status = when {
                !prootBinary.exists() -> ProotStatus.NOT_READY
                rootfsState.status != RootfsManager.RootfsStatus.INSTALLED -> ProotStatus.NOT_READY
                else -> ProotStatus.READY
            },
            prootPath = prootBinary.absolutePath,
            rootfsPath = rootfsManager.getRootfsPath()
        )
    }
    
    /**
     * Install proot binary and libraries from assets
     */
    suspend fun setupProotBinary(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = _state.value.copy(status = ProotStatus.INSTALLING)
            
            prootDir.mkdirs()
            libDir.mkdirs()
            
            // Copy proot binary
            val prootOut = prootBinary
            context.assets.open("$PROOT_ASSET_DIR/$PROOT_BINARY").use { input ->
                FileOutputStream(prootOut).use { output ->
                    input.copyTo(output)
                }
            }
            prootOut.setExecutable(true, false)
            prootOut.setReadable(true, false)
            
            // Copy libtalloc.so.2
            val tallocOut = File(libDir, LIBTALLOC)
            try {
                context.assets.open("$PROOT_ASSET_DIR/$LIBTALLOC").use { input ->
                    FileOutputStream(tallocOut).use { output ->
                        input.copyTo(output)
                    }
                }
                tallocOut.setReadable(true, false)
            } catch (_: Exception) {
                // libtalloc might not exist in assets, proot might work without it
            }
            
            checkStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = ProotStatus.ERROR,
                error = "Failed to setup proot: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Start a new PRoot session with Ubuntu environment
     */
    fun startProotSession(): TerminalSession? {
        val currentState = _state.value
        
        if (currentState.status != ProotStatus.READY) {
            return null
        }
        
        val prootPath = currentState.prootPath ?: return null
        val rootfsPath = currentState.rootfsPath ?: return null
        
        // Build PRoot command arguments
        val prootArgs = buildProotArgs(rootfsPath)
        
        // Build environment
        val env = buildEnvironment()
        
        // Create session with PRoot
        val session = TerminalManager.createSession(
            shellPath = prootPath,
            workingDirectory = "/root",
            environment = env,
            args = prootArgs
        )
        
        currentSession = session
        _state.value = _state.value.copy(status = ProotStatus.RUNNING)
        
        return session
    }
    
    /**
     * Build PRoot command arguments - optimized for Ubuntu rootfs
     */
    private fun buildProotArgs(rootfsPath: String): List<String> {
        val args = mutableListOf<String>()
        
        // Simulate root user (critical for apt and many programs)
        args.add("-0")
        
        // Root filesystem
        args.add("-r")
        args.add(rootfsPath)
        
        // Working directory
        args.add("-w")
        args.add("/root")
        
        // Link2symlink support - needed for hardlink handling
        args.add("--link2symlink")
        
        // Kill proot when shell exits
        args.add("--kill-on-exit")
        
        // Bind /dev for urandom and other devices
        args.add("-b")
        args.add("/dev:/dev")
        
        // Bind Android data directory for config sharing
        args.add("-b")
        args.add("${context.filesDir.parent}:/android_data")
        
        // Bind external storage if available
        val sdcard = File("/sdcard")
        if (sdcard.exists()) {
            args.add("-b")
            args.add("/sdcard:/sdcard")
        }
        
        // Shell to execute inside PRoot
        args.add("/bin/bash")
        args.add("--login")
        
        return args
    }
    
    /**
     * Build environment variables for PRoot
     */
    private fun buildEnvironment(): Map<String, String> {
        return mapOf(
            "HOME" to "/root",
            "PWD" to "/root",
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
            "LC_ALL" to "en_US.UTF-8",
            "SHELL" to "/bin/bash",
            "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            // PRoot specific - disable seccomp (not supported on Android)
            "PROOT_NO_SECCOMP" to "1"
        )
    }
    
    /**
     * Execute a single command in PRoot environment
     */
    suspend fun executeInProot(command: String): Result<String> {
        val session = startProotSession() ?: return Result.failure(
            IllegalStateException("PRoot not ready")
        )
        
        return withContext(Dispatchers.IO) {
            try {
                session.writeToSession("$command\n")
                kotlinx.coroutines.delay(1000)
                val output = session.emulator.getScreenText()
                Result.success(output)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Stop the current PRoot session
     */
    fun stopProotSession() {
        currentSession?.let { session ->
            session.finish()
            currentSession = null
        }
        _state.value = _state.value.copy(status = ProotStatus.READY)
    }
    
    fun getCurrentSession(): TerminalSession? = currentSession
    fun isRunning(): Boolean = _state.value.status == ProotStatus.RUNNING
    fun isReady(): Boolean = _state.value.status == ProotStatus.READY
}
