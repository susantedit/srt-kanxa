package com.srtxcheats.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootExecutor {

    private var rootAvailableCache: Boolean? = null

    fun isRootAvailable(): Boolean {
        rootAvailableCache?.let { return it }

        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                rootAvailableCache = true
                return true
            }
        }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            val available = exitCode == 0
            rootAvailableCache = available
            available
        } catch (_: Exception) {
            rootAvailableCache = false
            false
        }
    }

    suspend fun executeCommand(command: String): ShizukuManager.CommandResult = withContext(Dispatchers.IO) {
        if (!isRootAvailable()) {
            return@withContext ShizukuManager.CommandResult(-1, "", "Root (su) not found")
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText().trim() }
            val exitCode = process.waitFor()
            ShizukuManager.CommandResult(exitCode, output, error)
        } catch (e: Exception) {
            ShizukuManager.CommandResult(-1, "", e.message ?: "Failed to execute root command")
        }
    }
}
