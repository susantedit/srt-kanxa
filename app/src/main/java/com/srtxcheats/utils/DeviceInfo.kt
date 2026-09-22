package com.srtxcheats.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.srtxcheats.core.ShizukuManager

enum class DeviceCompatibilityLevel {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    UNSUPPORTED
}

data class DeviceSpecs(
    val hwid: String,
    val manufacturer: String,
    val model: String,
    val brand: String,
    val androidRelease: String,
    val sdkInt: Int,
    val physicalWidth: Int,
    val physicalHeight: Int,
    val currentDpi: Int,
    val orientation: String,
    val compatibilityLevel: DeviceCompatibilityLevel,
    val compatibilityReason: String
)

object DeviceInfo {

    @SuppressLint("HardwareIds")
    fun getHwid(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (!androidId.isNullOrBlank()) {
            androidId
        } else {
            // Fallback pseudo identifier
            "DEV_${Build.BOARD}_${Build.ID.take(8)}"
        }
    }

    @Suppress("DEPRECATION")
    fun getDeviceSpecs(context: Context): DeviceSpecs {
        val hwid = getHwid(context)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val orientation = if (isLandscape) "LANDSCAPE" else "PORTRAIT"

        val shizukuState = ShizukuManager.checkState(context)
        val isShizukuAuthorized = ShizukuManager.isAuthorized()

        val (compatLevel, reason) = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N -> {
                Pair(DeviceCompatibilityLevel.UNSUPPORTED, "Android 7.0 (API 24) or higher required")
            }
            shizukuState == ShizukuManager.ShizukuState.CONNECTED && isShizukuAuthorized -> {
                Pair(DeviceCompatibilityLevel.SUPPORTED, "Full Shizuku authorization active. System scaling available.")
            }
            shizukuState == ShizukuManager.ShizukuState.CONNECTED && !isShizukuAuthorized -> {
                Pair(DeviceCompatibilityLevel.PARTIALLY_SUPPORTED, "Shizuku running but requires permission authorization.")
            }
            shizukuState == ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> {
                Pair(DeviceCompatibilityLevel.PARTIALLY_SUPPORTED, "Shizuku installed but service not started.")
            }
            else -> {
                Pair(DeviceCompatibilityLevel.PARTIALLY_SUPPORTED, "Shizuku not detected. Install Shizuku for system display & touch features.")
            }
        }

        return DeviceSpecs(
            hwid = hwid,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            physicalWidth = metrics.widthPixels,
            physicalHeight = metrics.heightPixels,
            currentDpi = metrics.densityDpi,
            orientation = orientation,
            compatibilityLevel = compatLevel,
            compatibilityReason = reason
        )
    }
}
