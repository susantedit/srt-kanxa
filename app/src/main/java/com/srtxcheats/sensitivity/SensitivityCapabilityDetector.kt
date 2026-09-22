package com.srtxcheats.sensitivity

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.core.SystemCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceSensitivityCapabilities(
    val androidVersion: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val displayRefreshRate: Float,
    val maxRefreshRate: Float,
    val isShizukuInstalled: Boolean,
    val isShizukuRunning: Boolean,
    val isShizukuAuthorized: Boolean,
    val supportsPointerSpeed: Boolean,
    val supportsTouchSensitivity: Boolean,
    val supportsLongPressTimeout: Boolean,
    val supportsMultiPressTimeout: Boolean,
    val supportsPeakRefreshRate: Boolean,
    val supportsAnimationScale: Boolean,
    val supportsVelocityTracker: Boolean,
    val baselinePointerSpeed: Int,
    val baselineLongPressTimeout: Int,
    val baselineMultiPressTimeout: Int,
    val maxAchievableMultiplier: Float,
    val supportedSettings: List<String>,
    val unsupportedSettings: List<String>
)

object SensitivityCapabilityDetector {

    suspend fun detectCapabilities(context: Context): DeviceSensitivityCapabilities = withContext(Dispatchers.IO) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { context.display } catch (_: Throwable) { wm?.defaultDisplay }
        } else {
            @Suppress("DEPRECATION")
            wm?.defaultDisplay
        }

        val currentHz = display?.refreshRate ?: 60f
        val supportedModes = display?.supportedModes ?: emptyArray()
        val maxHz = supportedModes.maxOfOrNull { it.refreshRate } ?: currentHz

        val shizukuState = ShizukuManager.checkState(context)
        val isShizukuInstalled = shizukuState != ShizukuManager.ShizukuState.NOT_INSTALLED
        val isShizukuRunning = shizukuState == ShizukuManager.ShizukuState.CONNECTED
        val isShizukuAuthorized = ShizukuManager.isAuthorized()

        val supported = mutableListOf<String>()
        val unsupported = mutableListOf<String>()

        var supPointerSpeed = false
        var supTouchSensitivity = false
        var supLongPress = false
        var supMultiPress = false
        var supPeakRefresh = false
        var supAnimScale = false
        var supVelocityTracker = false

        var basePointerSpeed = 0
        var baseLongPress = 400
        var baseMultiPress = 300

        if (isShizukuAuthorized) {
            // 1. System Pointer Speed
            val pointerVal = SystemCommandExecutor.readSetting("system", "pointer_speed")
            if (pointerVal != null) {
                supPointerSpeed = true
                basePointerSpeed = pointerVal.toIntOrNull() ?: 0
                supported.add("settings put system pointer_speed (Baseline: $basePointerSpeed)")
            } else {
                unsupported.add("settings put system pointer_speed (Inaccessible)")
            }

            // 2. OEM Touch Sensitivity / Glove Mode Layer
            val secTouch = SystemCommandExecutor.readSetting("secure", "touch_sensitivity")
            val sysTouch = SystemCommandExecutor.readSetting("system", "high_touch_sensitivity")
            val gloveTouch = SystemCommandExecutor.readSetting("secure", "glove_mode")
            if (secTouch != null || sysTouch != null || gloveTouch != null) {
                supTouchSensitivity = true
                supported.add("OEM Touch Sensitivity Layer (Hardware sampling boost)")
            } else {
                unsupported.add("OEM Touch Sensitivity Layer (Not exposed on ${Build.MANUFACTURER})")
            }

            // 3. Long Press Timeout
            val longPressVal = SystemCommandExecutor.readSetting("secure", "long_press_timeout")
            if (longPressVal != null) {
                supLongPress = true
                baseLongPress = longPressVal.toIntOrNull() ?: 400
                supported.add("settings put secure long_press_timeout (Baseline: ${baseLongPress}ms)")
            } else {
                unsupported.add("settings put secure long_press_timeout (Vendor locked)")
            }

            // 4. Multi-press Timeout
            val multiPressVal = SystemCommandExecutor.readSetting("secure", "multi_press_timeout")
            if (multiPressVal != null) {
                supMultiPress = true
                baseMultiPress = multiPressVal.toIntOrNull() ?: 300
                supported.add("settings put secure multi_press_timeout (Baseline: ${baseMultiPress}ms)")
            } else {
                unsupported.add("settings put secure multi_press_timeout (Vendor locked)")
            }

            // 5. Peak Refresh Rate
            if (maxHz > 60f) {
                val peakVal = SystemCommandExecutor.readSetting("system", "peak_refresh_rate")
                if (peakVal != null) {
                    supPeakRefresh = true
                    supported.add("settings put system peak_refresh_rate (${maxHz.toInt()}Hz)")
                } else {
                    unsupported.add("settings put system peak_refresh_rate (Inaccessible)")
                }
            } else {
                unsupported.add("Panel Refresh Rate Boost (Hardware 60Hz only)")
            }

            // 6. Animation Scaling
            val animVal = SystemCommandExecutor.readSetting("global", "window_animation_scale")
            if (animVal != null) {
                supAnimScale = true
                supported.add("settings put global window_animation_scale")
            } else {
                unsupported.add("settings put global window_animation_scale (Inaccessible)")
            }

            // 7. Input Native Velocity Tracker Strategy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val testRes = SystemCommandExecutor.execute("device_config get input_native_boot touch.filter.velocity_tracker_strategy")
                if (testRes.success && !testRes.stdout.contains("Invalid", ignoreCase = true)) {
                    supVelocityTracker = true
                    supported.add("device_config input_native_boot velocity_tracker")
                } else {
                    unsupported.add("Input Native Velocity Tracker (device_config disabled)")
                }
            } else {
                unsupported.add("Input Native Velocity Tracker (Requires Android 11+)")
            }
        } else {
            unsupported.add("All system sensitivity controls (Shizuku authorization required)")
        }

        // Calculate maximum real achievable multiplier based on supported parameters:
        // Baseline = 1.0X
        // Pointer speed: +1.5X
        // Touch sensitivity: +1.0X
        // Long/multi press: +0.7X
        // Peak refresh: +0.5X
        // Animation scale: +0.3X
        var maxAchievable = 1.0f
        if (supPointerSpeed) maxAchievable += 1.5f
        if (supTouchSensitivity) maxAchievable += 1.0f
        if (supLongPress || supMultiPress) maxAchievable += 0.7f
        if (supPeakRefresh) maxAchievable += 0.5f
        if (supAnimScale) maxAchievable += 0.3f
        val clampedMaxAchievable = maxAchievable.coerceIn(1.0f, 5.0f)

        DeviceSensitivityCapabilities(
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER ?: "Android",
            model = Build.MODEL ?: "Device",
            displayRefreshRate = currentHz,
            maxRefreshRate = maxHz,
            isShizukuInstalled = isShizukuInstalled,
            isShizukuRunning = isShizukuRunning,
            isShizukuAuthorized = isShizukuAuthorized,
            supportsPointerSpeed = supPointerSpeed,
            supportsTouchSensitivity = supTouchSensitivity,
            supportsLongPressTimeout = supLongPress,
            supportsMultiPressTimeout = supMultiPress,
            supportsPeakRefreshRate = supPeakRefresh,
            supportsAnimationScale = supAnimScale,
            supportsVelocityTracker = supVelocityTracker,
            baselinePointerSpeed = basePointerSpeed,
            baselineLongPressTimeout = baseLongPress,
            baselineMultiPressTimeout = baseMultiPress,
            maxAchievableMultiplier = clampedMaxAchievable,
            supportedSettings = supported,
            unsupportedSettings = unsupported
        )
    }
}
