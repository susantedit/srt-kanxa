package com.srtxcheats.display

import android.content.Context
import android.content.res.Configuration
import com.srtxcheats.core.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DisplayStatus(
    val currentWidth: Int,
    val currentHeight: Int,
    val currentDpi: Int,
    val orientation: String,
    val isStretched: Boolean,
    val backupAvailable: Boolean,
    val backupResolution: String,
    val backupDpi: Int,
    val shizukuConnected: Boolean,
    val shizukuAuthorized: Boolean
)

class DisplayStateMonitor(
    private val context: Context,
    private val resolutionManager: DisplayResolutionManager,
    private val densityManager: DisplayDensityManager,
    private val backupManager: DisplayBackupManager
) {

    suspend fun getDisplayStatus(): DisplayStatus = withContext(Dispatchers.IO) {
        val metrics = resolutionManager.getDisplayMetrics()
        val currentDpi = densityManager.getCurrentDensity()
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val backup = backupManager.getBackup()

        val isShizukuRunning = ShizukuManager.checkState(context) == ShizukuManager.ShizukuState.CONNECTED
        val isShizukuAuth = ShizukuManager.isAuthorized()

        // Check if resolution or density deviates from original baseline
        val isStretched = backup.isValid &&
                (metrics.currentWidth != backup.originalWidth ||
                 metrics.currentHeight != backup.originalHeight ||
                 currentDpi != backup.originalDensity)

        DisplayStatus(
            currentWidth = metrics.currentWidth,
            currentHeight = metrics.currentHeight,
            currentDpi = currentDpi,
            orientation = if (isLandscape) "Landscape" else "Portrait",
            isStretched = isStretched,
            backupAvailable = backup.isValid,
            backupResolution = backup.formattedResolution,
            backupDpi = backup.originalDensity,
            shizukuConnected = isShizukuRunning,
            shizukuAuthorized = isShizukuAuth
        )
    }
}
