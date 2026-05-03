package com.tietiezhi.apk.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tietiezhi.apk.MainActivity
import java.io.File

class TietiezhiService : Service() {

    private var process: Process? = null
    private var currentPort: Int = 18178

    companion object {
        const val CHANNEL_ID = "tietiezhi_server"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_PORT = "extra_port"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentPort = intent.getIntExtra(EXTRA_PORT, 18178)
                startServer(currentPort)
            }
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    private fun startServer(port: Int) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("铁铁汁")
            .setContentText("本地服务运行中 - 端口 $port")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "停止服务",
                PendingIntent.getService(
                    this, 0,
                    Intent(this, TietiezhiService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Start server via proot in Ubuntu environment
        startServerViaProot(port)
    }

    /**
     * Start the Go server via proot inside Ubuntu rootfs
     */
    private fun startServerViaProot(port: Int) {
        try {
            val prootBinary = File(filesDir, "proot/proot")
            val rootfsPath = File(filesDir, "ubuntu").absolutePath
            
            if (!prootBinary.exists()) {
                android.util.Log.e("TietiezhiService", "Proot binary not found")
                return
            }
            
            if (!File(rootfsPath, "bin/bash").exists()) {
                android.util.Log.e("TietiezhiService", "Rootfs not found or incomplete")
                return
            }
            
            // Update config with current port
            updateServerConfig(port)
            
            // Server command to run inside proot
            val serverCmd = "/opt/tietiezhi/tietiezhi-server -c /opt/tietiezhi/configs/config.yaml"
            
            // Build proot command
            val prootArgs = listOf(
                prootBinary.absolutePath,
                "-0",                          // Simulate root user
                "-r", rootfsPath,               // Root filesystem
                "-w", "/root",                  // Working directory
                "--link2symlink",              // Hardlink support
                "--kill-on-exit",              // Kill when shell exits
                "-b", "/dev:/dev",              // Bind /dev
                "/bin/bash", "-c", serverCmd
            )
            
            val processBuilder = ProcessBuilder(prootArgs)
            processBuilder.environment().apply {
                put("PROOT_NO_SECCOMP", "1")
                put("HOME", "/root")
                put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
                put("LANG", "en_US.UTF-8")
                put("LC_ALL", "en_US.UTF-8")
            }
            processBuilder.redirectErrorStream(true)
            
            process = processBuilder.start()
            android.util.Log.i("TietiezhiService", "Server started via proot with PID: ${process?.hashCode()}")
            
        } catch (e: Exception) {
            android.util.Log.e("TietiezhiService", "Failed to start server via proot", e)
        }
    }

    /**
     * Update server configuration in rootfs
     */
    private fun updateServerConfig(port: Int) {
        try {
            val configDir = File(filesDir, "ubuntu/opt/tietiezhi/configs")
            configDir.mkdirs()
            
            val dataDir = File(filesDir, "tietiezhi")
            dataDir.mkdirs()
            
            val configFile = File(configDir, "config.yaml")
            configFile.writeText(createDefaultConfig(dataDir.absolutePath, port))
            
            // Create data directories
            File(dataDir, "data/workspace").mkdirs()
            File(dataDir, "data/sessions").mkdirs()
            File(dataDir, "data/cron").mkdirs()
            File(dataDir, "data/subagents").mkdirs()
            
        } catch (e: Exception) {
            android.util.Log.e("TietiezhiService", "Failed to update config", e)
        }
    }

    private fun createDefaultConfig(dataDir: String, port: Int): String = """
server:
  host: "0.0.0.0"
  port: $port

llm:
  provider: "openai"
  base_url: ""
  api_key: ""
  model: ""

agent:
  max_tool_calls: 20
  system_prompt: "你是一个有用的AI助手"
  loop_detection: true

memory:
  type: "markdown"
  path: "$dataDir/data/workspace"

skills:
  path: "$dataDir/data/workspace/skills"

scheduler:
  enabled: true
  path: "$dataDir/data/cron"
  exec_timeout: 300

heartbeat:
  enabled: true
  interval: 30

log:
  level: "info"
  format: "text"

session:
  max_history_turns: 20
  persist_path: "$dataDir/data/sessions"
  auto_save_seconds: 60

subagent:
  enabled: true
  path: "$dataDir/data/subagents"
  timeout: 300
""".trimIndent()

    private fun stopServer() {
        process?.destroy()
        process = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "铁铁汁服务",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        process?.destroy()
    }
}
