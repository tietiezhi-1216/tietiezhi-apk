package com.tietiezhi.apk.server

import android.app.*
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

        val binary = copyBinary()
        if (binary != null && binary.exists()) {
            try {
                val dataDir = getDir("tietiezhi", MODE_PRIVATE)
                val configDir = File(dataDir, "configs")
                configDir.mkdirs()
                
                // 创建默认配置文件（如果不存在）
                val configFile = File(configDir, "config.yaml")
                if (!configFile.exists()) {
                    configFile.writeText(createDefaultConfig(dataDir.absolutePath, port))
                }
                
                // 创建数据目录
                File(dataDir, "data/workspace").mkdirs()
                File(dataDir, "data/sessions").mkdirs()
                File(dataDir, "data/cron").mkdirs()
                File(dataDir, "data/subagents").mkdirs()

                ProcessBuilder(binary.absolutePath, "-c", configFile.absolutePath)
                    .apply {
                        environment()["PORT"] = port.toString()
                        redirectErrorStream(true)
                    }
                    .start()
                    .also { process = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createDefaultConfig(dataDir: String, port: Int): String = """
# tietiezhi 默认配置
server:
  host: "0.0.0.0"
  port: $port

llm:
  provider: "openai"
  base_url: ""
  api_key: ""
  model: "default"

agent:
  max_tool_calls: 20
  system_prompt: "你是一个有用的AI助手"
  loop_detection: true

channels:
  feishu:
    enabled: false

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

hooks:
  enabled: false
  rules: []

subagent:
  enabled: true
  path: "$dataDir/data/subagents"
  timeout: 300

approval:
  enabled: false

observability:
  enabled: false

sandbox:
  enabled: false
""".trimIndent()

    private fun copyBinary(): File? {
        return try {
            val dest = File(filesDir, "tietiezhi-server")
            if (!dest.exists()) {
                assets.open("libtietiezhi-server.so").use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                dest.setExecutable(true)
            }
            dest
        } catch (e: Exception) {
            try {
                val src = File(applicationInfo.nativeLibraryDir, "libtietiezhi-server.so")
                if (src.exists()) {
                    val dest = File(filesDir, "tietiezhi-server")
                    if (!dest.exists()) {
                        src.copyTo(dest)
                        dest.setExecutable(true)
                    }
                    dest
                } else null
            } catch (_: Exception) { null }
        }
    }

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
