package com.srtxcheats.sensitivity

import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.core.SystemCommandExecutor
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExecutionVerificationResult(
    val isVerified: Boolean,
    val namespace: String,
    val key: String,
    val originalValue: String?,
    val targetValue: String,
    val readBackValue: String?,
    val errorMessage: String? = null
)

object SensitivityCommandExecutor {

    /**
     * Executes a system parameter change with strict transactional safety:
     * 1. READ ORIGINAL value
     * 2. BACKUP original value (via SensitivityBackupManager)
     * 3. WRITE new value
     * 4. READ AGAIN from system
     * 5. VERIFY against expected target value
     * 6. If verification fails -> ROLL BACK immediately to original value.
     */
    suspend fun applyAndVerifyWithRollback(
        backupManager: SensitivityBackupManager,
        namespace: String,
        key: String,
        targetValue: String
    ): ExecutionVerificationResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) {
            return@withContext ExecutionVerificationResult(
                isVerified = false,
                namespace = namespace,
                key = key,
                originalValue = null,
                targetValue = targetValue,
                readBackValue = null,
                errorMessage = "Shizuku not authorized"
            )
        }

        // 1. READ ORIGINAL
        val originalValue = SystemCommandExecutor.readSetting(namespace, key)

        // 2. BACKUP
        backupManager.backupSettingIfAbsent(namespace, key)

        // 3. WRITE
        val writeSuccess = SystemCommandExecutor.writeSetting(namespace, key, targetValue)
        if (!writeSuccess) {
            AppLogger.w("Write failed for $namespace $key = $targetValue")
            return@withContext ExecutionVerificationResult(
                isVerified = false,
                namespace = namespace,
                key = key,
                originalValue = originalValue,
                targetValue = targetValue,
                readBackValue = originalValue,
                errorMessage = "Write command failed"
            )
        }

        // 4. READ AGAIN
        val readBack = SystemCommandExecutor.readSetting(namespace, key)

        // 5. VERIFY
        val verified = readBack != null && readBack.trim() == targetValue.trim()

        if (!verified) {
            // 6. ROLL BACK IMMEDIATELY
            AppLogger.e("Verification failed for $namespace $key: expected '$targetValue', got '$readBack'! Rolling back to: '$originalValue'")
            if (originalValue != null) {
                SystemCommandExecutor.writeSetting(namespace, key, originalValue)
            } else {
                SystemCommandExecutor.execute("settings delete $namespace $key")
            }
            return@withContext ExecutionVerificationResult(
                isVerified = false,
                namespace = namespace,
                key = key,
                originalValue = originalValue,
                targetValue = targetValue,
                readBackValue = readBack,
                errorMessage = "Verification failed: read back '$readBack' != expected '$targetValue'. Rolled back."
            )
        }

        AppLogger.i("Successfully verified system setting $namespace $key = $targetValue")
        ExecutionVerificationResult(
            isVerified = true,
            namespace = namespace,
            key = key,
            originalValue = originalValue,
            targetValue = targetValue,
            readBackValue = readBack,
            errorMessage = null
        )
    }
}
