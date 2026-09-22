package com.srtxcheats.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    enum class ShizukuState {
        NOT_INSTALLED,
        SERVICE_NOT_RUNNING,
        CONNECTED
    }

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )

    data class CapabilityCheck(
        val isConnected: Boolean,
        val isAuthorized: Boolean,
        val performanceApiAvailable: Boolean,
        val gameModeAvailable: Boolean,
        val systemOptAvailable: Boolean,
        val unsupportedCount: Int,
        val unsupportedDetails: List<String>
    )

    suspend fun detectCapabilities(): CapabilityCheck = withContext(Dispatchers.IO) {
        val authorized = isAuthorized()
        if (!authorized) {
            return@withContext CapabilityCheck(
                isConnected = false,
                isAuthorized = false,
                performanceApiAvailable = false,
                gameModeAvailable = false,
                systemOptAvailable = false,
                unsupportedCount = 2,
                unsupportedDetails = listOf(
                    "Game File Modification [STRICTLY FORBIDDEN BY POLICY]",
                    "Game Memory Injection [STRICTLY FORBIDDEN BY POLICY]"
                )
            )
        }

        // Test Performance API / dump sys
        val dumpRes = executeCommand("cmd statusbar --help")
        val perfAvailable = dumpRes.exitCode == 0 || executeCommand("which dumpsys").exitCode == 0

        // Test Game Mode command
        val gmRes = executeCommand("cmd game mode --help")
        val gmAvailable = gmRes.exitCode == 0 || !gmRes.output.contains("not found", ignoreCase = true)

        // Test System Optimization (settings command)
        val optRes = executeCommand("settings get system pointer_speed")
        val optAvailable = optRes.exitCode == 0

        CapabilityCheck(
            isConnected = true,
            isAuthorized = true,
            performanceApiAvailable = perfAvailable,
            gameModeAvailable = gmAvailable,
            systemOptAvailable = optAvailable,
            unsupportedCount = 2,
            unsupportedDetails = listOf(
                "Game Binary Injection [PROHIBITED BY ANTI-CHEAT]",
                "Game Memory Patching [PROHIBITED BY SECURITY MODEL]"
            )
        )
    }

    fun checkState(context: Context): ShizukuState {
        // Check if Shizuku app is installed
        val isInstalled = try {
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalled) {
            return ShizukuState.NOT_INSTALLED
        }

        // Check if binder is running
        return try {
            if (Shizuku.pingBinder()) {
                ShizukuState.CONNECTED
            } else {
                ShizukuState.SERVICE_NOT_RUNNING
            }
        } catch (_: Throwable) {
            ShizukuState.SERVICE_NOT_RUNNING
        }
    }

    fun isAuthorized(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Throwable) {}
    }

    fun openShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                openShizukuWebsite(context)
            }
        } catch (_: Exception) {
            openShizukuWebsite(context)
        }
    }

    fun openShizukuWebsite(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Safely executes an authorized command using Shizuku.newProcess.
     * Never crashes if unauthorized or unsupported.
     */
    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!isAuthorized()) {
            return@withContext CommandResult(-1, "", "Shizuku not authorized or not running")
        }

        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText().trim() }
            val exitCode = process.waitFor()
            CommandResult(exitCode, output, error)
        } catch (e: Throwable) {
            CommandResult(-1, "", e.message ?: "Operation not supported on this device")
        }
    }

    /**
     * Safe privileged query for device information.
     */
    suspend fun readDeviceProperties(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!isAuthorized()) return@withContext emptyMap()

        val props = mutableMapOf<String, String>()
        val queries = listOf(
            "Device Model" to "getprop ro.product.model",
            "Hardware SOC" to "getprop ro.soc.model",
            "Manufacturer" to "getprop ro.product.manufacturer",
            "Android Version" to "getprop ro.build.version.release",
            "Board" to "getprop ro.product.board"
        )

        for ((key, cmd) in queries) {
            val res = executeCommand(cmd)
            if (res.exitCode == 0 && res.output.isNotBlank()) {
                props[key] = res.output
            }
        }
        props
    }
}
