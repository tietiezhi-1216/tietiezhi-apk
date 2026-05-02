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

        // 从 jniLibs 复制二进制到可执行目录
        val binary = copyBinary()
        if (binary != null && binary.exists()) {
            try {
                val dataDir = getDir("tietiezhi", MODE_PRIVATE)
                val configDir = File(dataDir, "configs")
                configDir.mkdirs()

                ProcessBuilder(binary.absolutePath, "-c", File(configDir, "config.yaml").absolutePath)
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
            // 从 jniLibs 方式不行，尝试直接执行
            try {
                val abi = android.os.Build.SUPPORTED_ABIS.first()
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
