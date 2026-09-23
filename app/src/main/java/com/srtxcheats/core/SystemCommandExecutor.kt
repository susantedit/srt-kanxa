package com.srtxcheats.core

import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SystemCommandExecutor:
 * Legitimate Android system settings executor via Shizuku/privileged process.
 * Guarantees WRITE -> READ BACK -> VERIFY -> ROLLBACK on verification failure.
 */
object SystemCommandExecutor {

    data class ExecutionResult(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
        val exitCode: Int
    )

    /**
     * Executes an arbitrary shell command through authorized Shizuku binder.
     */
    suspend fun execute(command: String): ExecutionResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) {
            return@withContext ExecutionResult(
                success = false,
                stdout = "",
                stderr = "Privileged service (Shizuku or Root) is not authorized or running",
                exitCode = -1
            )
        }

        val res = ShizukuManager.executeCommand(command)
        ExecutionResult(
            success = res.exitCode == 0,
            stdout = res.output,
            stderr = res.error,
            exitCode = res.exitCode
        )
    }

    /**
     * Reads a system setting (system, secure, or global).
     */
    suspend fun readSetting(namespace: String = "system", key: String): String? = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) return@withContext null
        val cmd = "settings get $namespace $key"
        val result = execute(cmd)
        if (result.success && result.stdout.isNotBlank() && !result.stdout.contains("null", ignoreCase = true)) {
            result.stdout.trim()
        } else {
            null
        }
    }

    /**
     * Writes a system setting.
     */
    suspend fun writeSetting(namespace: String = "system", key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) return@withContext false
        val cmd = "settings put $namespace $key $value"
        val result = execute(cmd)
        result.success
    }

    /**
     * Verifies that a setting currently holds the expected value.
     */
    suspend fun verifySetting(namespace: String = "system", key: String, expectedValue: String): Boolean = withContext(Dispatchers.IO) {
        val currentValue = readSetting(namespace, key)
        currentValue != null && currentValue.trim() == expectedValue.trim()
    }

    /**
     * Safe transaction:
     * 1. WRITE setting
     * 2. READ BACK setting
     * 3. VERIFY against expected value
     * 4. If verification fails, ROLL BACK to original value immediately.
     */
    suspend fun writeAndVerifyWithRollback(
        namespace: String = "system",
        key: String,
        newValue: String,
        previousValue: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val writeSuccess = writeSetting(namespace, key, newValue)
        if (!writeSuccess) {
            AppLogger.w("Failed to write setting $namespace $key = $newValue")
            return@withContext false
        }

        val verified = verifySetting(namespace, key, newValue)
        if (!verified) {
            AppLogger.e("Verification failed for $namespace $key! Rolling back to previous value: $previousValue")
            if (previousValue != null) {
                writeSetting(namespace, key, previousValue)
            } else {
                execute("settings delete $namespace $key")
            }
            return@withContext false
        }

        AppLogger.i("Verified setting $namespace $key = $newValue successfully")
        true
    }
}
