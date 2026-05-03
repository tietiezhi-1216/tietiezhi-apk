package com.tietiezhi.terminal.rootfs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * RootfsManager - Manages Ubuntu rootfs installation
 */
class RootfsManager(private val context: Context) {
    
    enum class RootfsStatus {
        NOT_INSTALLED,
        DOWNLOADING,
        INSTALLING,
        INSTALLED,
        ERROR
    }
    
    data class RootfsState(
        val status: RootfsStatus = RootfsStatus.NOT_INSTALLED,
        val progress: Float = 0f,
        val error: String? = null,
        val version: String = ""
    )
    
    private val _state = MutableStateFlow(RootfsState())
    val state: StateFlow<RootfsState> = _state.asStateFlow()
    
    private val rootfsDir: File
        get() = File(context.filesDir, "ubuntu")
    
    private val rootfsArchive: File
        get() = File(context.filesDir, "ubuntu-24.04-arm64.tar.gz")
    
    companion object {
        private const val ROOTFS_VERSION = "1.0.0"
        private const val ROOTFS_ASSET_PATH = "rootfs"
        private const val ROOTFS_ARCHIVE = "ubuntu-24.04-arm64.tar.gz"
        private const val ROOTFS_INFO = "rootfs-info.json"
        private const val ROOTFS_DOWNLOAD_URL = "https://github.com/tietiezhi-1216/tietiezhi-apk/releases/download/rootfs-v1/ubuntu-24.04-arm64.tar.gz"
        
        @Volatile
        private var instance: RootfsManager? = null
        
        fun getInstance(context: Context): RootfsManager {
            return instance ?: synchronized(this) {
                instance ?: RootfsManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        checkInstallation()
    }
    
    val isInstalled: Boolean
        get() = rootfsDir.exists() && File(rootfsDir, "bin/bash").exists()
    
    fun getRootfsPath(): String = rootfsDir.absolutePath
    
    private fun checkInstallation() {
        _state.value = _state.value.copy(
            status = if (isInstalled) RootfsStatus.INSTALLED else RootfsStatus.NOT_INSTALLED,
            version = if (isInstalled) ROOTFS_VERSION else ""
        )
    }
    
    /**
     * Download rootfs from GitHub and install
     */
    suspend fun downloadAndInstall() {
        withContext(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(
                    status = RootfsStatus.DOWNLOADING,
                    progress = 0f,
                    error = null
                )
                
                // Download rootfs archive
                val url = URL(ROOTFS_DOWNLOAD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                
                val totalSize = connection.contentLength.toLong()
                var downloadedSize = 0L
                
                connection.inputStream.use { input ->
                    FileOutputStream(rootfsArchive).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            
                            if (totalSize > 0) {
                                _state.value = _state.value.copy(
                                    progress = (downloadedSize.toFloat() / totalSize) * 0.5f
                                )
                            }
                        }
                    }
                }
                
                // Extract the downloaded archive
                extractRootfs()
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = RootfsStatus.ERROR,
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Install rootfs from assets (if available)
     */
    suspend fun installFromAssets() {
        withContext(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(
                    status = RootfsStatus.INSTALLING,
                    progress = 0f,
                    error = null
                )
                
                val assetFile = "$ROOTFS_ASSET_PATH/$ROOTFS_ARCHIVE"
                
                try {
                    context.assets.open(assetFile).close()
                } catch (e: Exception) {
                    // No rootfs in assets, fall back to download
                    downloadAndInstall()
                    return@withContext
                }
                
                // Copy from assets to local file
                context.assets.open(assetFile).use { input ->
                    FileOutputStream(rootfsArchive).use { output ->
                        input.copyTo(output)
                    }
                }
                
                extractRootfs()
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = RootfsStatus.ERROR,
                    error = "Installation failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Extract rootfs archive to installation directory
     */
    private suspend fun extractRootfs() {
        withContext(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(
                    status = RootfsStatus.INSTALLING,
                    progress = 0.5f
                )
                
                if (rootfsDir.exists()) {
                    rootfsDir.deleteRecursively()
                }
                rootfsDir.mkdirs()
                
                FileInputStream(rootfsArchive).use { fileInput ->
                    GzipCompressorInputStream(fileInput).use { gzipInput ->
                        TarArchiveInputStream(gzipInput).use { tarInput ->
                            var entry = tarInput.nextTarEntry
                            var processedEntries = 0
                            
                            while (entry != null) {
                                val destFile = File(rootfsDir, entry.name)
                                
                                if (!destFile.canonicalPath.startsWith(rootfsDir.canonicalPath)) {
                                    entry = tarInput.nextTarEntry
                                    continue
                                }
                                
                                if (entry.isDirectory) {
                                    destFile.mkdirs()
                                } else {
                                    destFile.parentFile?.mkdirs()
                                    FileOutputStream(destFile).use { output ->
                                        tarInput.copyTo(output)
                                    }
                                    
                                    destFile.setReadable(true, false)
                                    if ((entry.mode and 73) != 0) {
                                        destFile.setExecutable(true, false)
                                    }
                                    destFile.setWritable(true, true)
                                }
                                
                                processedEntries++
                                if (processedEntries % 200 == 0) {
                                    _state.value = _state.value.copy(
                                        progress = 0.5f + minOf(0.5f, processedEntries / 8000f)
                                    )
                                }
                                
                                entry = tarInput.nextTarEntry
                            }
                        }
                    }
                }
                
                makeExecutable()
                
                // Delete archive after extraction
                rootfsArchive.delete()
                
                _state.value = _state.value.copy(
                    status = RootfsStatus.INSTALLED,
                    progress = 1f,
                    version = ROOTFS_VERSION
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = RootfsStatus.ERROR,
                    error = "Extraction failed: ${e.message}"
                )
            }
        }
    }
    
    private fun makeExecutable() {
        val executableDirs = listOf("bin", "sbin", "usr/bin", "usr/sbin", "usr/libexec")
        for (dir in executableDirs) {
            val dirFile = File(rootfsDir, dir)
            if (dirFile.isDirectory) {
                dirFile.listFiles()?.forEach { file ->
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }
        }
    }
    
    fun uninstall() {
        try {
            rootfsDir.deleteRecursively()
            rootfsArchive.delete()
            _state.value = RootfsState(status = RootfsStatus.NOT_INSTALLED)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                status = RootfsStatus.ERROR,
                error = "Uninstall failed: ${e.message}"
            )
        }
    }
    
    fun getRootfsSize(): Long {
        return if (rootfsDir.exists()) {
            rootfsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
    }
    
    fun getRootfsInfo(): JSONObject? {
        return try {
            val info = context.assets.open("$ROOTFS_ASSET_PATH/$ROOTFS_INFO")
                .bufferedReader().use { it.readText() }
            JSONObject(info)
        } catch (e: Exception) {
            null
        }
    }
}
