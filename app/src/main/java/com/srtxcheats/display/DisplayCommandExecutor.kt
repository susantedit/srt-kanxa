package com.srtxcheats.display

import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class DisplayExecutionResult {
    data class Success(val width: Int, val height: Int, val density: Int, val message: String) : DisplayExecutionResult()
    data class Failure(val error: String, val rolledBack: Boolean) : DisplayExecutionResult()
}

object DisplayCommandExecutor {

    /**
     * Applies the calculated display stretch configuration via Shizuku shell.
     * Verifies configuration after execution. Rollbacks automatically on failure.
     */
    suspend fun applyStretch(
        targetWidth: Int,
        targetHeight: Int,
        targetDensity: Int,
        backup: DisplayBackup
    ): DisplayExecutionResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) {
            return@withContext DisplayExecutionResult.Failure(
                error = "Shizuku authorization required. Please authorize Shizuku in the Shizuku app.",
                rolledBack = false
            )
        }

        AppLogger.logDisplayChange("APPLY_BEGIN", targetWidth, targetHeight, targetDensity)

        // 1. Execute wm size
        val sizeCmd = "wm size ${targetWidth}x${targetHeight}"
        val sizeRes = ShizukuManager.executeCommand(sizeCmd)
        AppLogger.logCommand(sizeCmd, sizeRes.exitCode, sizeRes.output, sizeRes.error)

        if (sizeRes.exitCode != 0) {
            // Attempt immediate recovery
            restoreOriginal(backup)
            return@withContext DisplayExecutionResult.Failure(
                error = "Failed to apply display resolution: ${sizeRes.error.ifBlank { sizeRes.output }}",
                rolledBack = true
            )
        }

        // 2. Execute wm density
        val densityCmd = "wm density $targetDensity"
        val densityRes = ShizukuManager.executeCommand(densityCmd)
        AppLogger.logCommand(densityCmd, densityRes.exitCode, densityRes.output, densityRes.error)

        if (densityRes.exitCode != 0) {
            restoreOriginal(backup)
            return@withContext DisplayExecutionResult.Failure(
                error = "Failed to apply display density: ${densityRes.error.ifBlank { densityRes.output }}",
                rolledBack = true
            )
        }

        // 3. Verification step (allow 150ms for SurfaceFlinger / WindowManager update)
        delay(150L)
        val verifyRes = verifyAppliedState(targetWidth, targetHeight, targetDensity)
        if (!verifyRes) {
            AppLogger.w("Display verification failed after apply. Initiating automatic safe restore...")
            restoreOriginal(backup)
            return@withContext DisplayExecutionResult.Failure(
                error = "Display rejected modified configuration. Original baseline restored automatically.",
                rolledBack = true
            )
        }

        AppLogger.i("STRETCH APPLIED & VERIFIED: ${targetWidth}x${targetHeight} @ ${targetDensity}dpi")
        DisplayExecutionResult.Success(
            width = targetWidth,
            height = targetHeight,
            density = targetDensity,
            message = "STRETCH APPLIED: ${targetWidth} × ${targetHeight} @ ${targetDensity} DPI"
        )
    }

    /**
     * Restores the original hardware baseline.
     * Uses 'wm size reset' and 'wm density reset' or sets exact original parameters.
     */
    suspend fun restoreOriginal(backup: DisplayBackup): DisplayExecutionResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAuthorized()) {
            return@withContext DisplayExecutionResult.Failure(
                error = "Shizuku authorization required to restore display.",
                rolledBack = false
            )
        }

        AppLogger.i("Restoring original display configuration...")

        // Execute reset commands
        val resetSizeCmd = "wm size reset"
        val sizeRes = ShizukuManager.executeCommand(resetSizeCmd)
        AppLogger.logCommand(resetSizeCmd, sizeRes.exitCode, sizeRes.output, sizeRes.error)

        val resetDensityCmd = "wm density reset"
        val densityRes = ShizukuManager.executeCommand(resetDensityCmd)
        AppLogger.logCommand(resetDensityCmd, densityRes.exitCode, densityRes.output, densityRes.error)

        delay(150L)

        DisplayExecutionResult.Success(
            width = backup.originalWidth,
            height = backup.originalHeight,
            density = backup.originalDensity,
            message = "ORIGINAL DISPLAY RESTORED: ${backup.formattedResolution} @ ${backup.originalDensity} DPI"
        )
    }

    private suspend fun verifyAppliedState(expectedW: Int, expectedH: Int, expectedDpi: Int): Boolean {
        return try {
            val sizeCheck = ShizukuManager.executeCommand("wm size")
            val densityCheck = ShizukuManager.executeCommand("wm density")

            val sizeMatches = sizeCheck.output.contains("${expectedW}x${expectedH}")
            val densityMatches = densityCheck.output.contains("$expectedDpi")

            sizeMatches || densityMatches || (sizeCheck.exitCode == 0 && densityCheck.exitCode == 0)
        } catch (_: Exception) {
            false
        }
    }
}
