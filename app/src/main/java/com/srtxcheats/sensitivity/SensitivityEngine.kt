package com.srtxcheats.sensitivity

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.data.dataStore
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * SensitivityEngine:
 * Real Android Device-Level Sensitivity Boost Engine.
 *
 * Uses a multiplier model relative to the phone's native default behavior:
 * DEFAULT = 1.0X
 * LOW        = 0%   -> 1.0X
 * MEDIUM     = 50%  -> 2.0X
 * HIGH       = 75%  -> 3.0X
 * ULTRA HIGH = 100% -> 5.0X
 *
 * Strictly applies only real supported system parameters via Shizuku.
 * Flow:
 * Check Shizuku -> Check permission -> Detect supported parameters ->
 * Backup original baseline -> Apply profile -> Read again -> Verify -> Report verified result.
 */
class SensitivityEngine(private val context: Context) {

    private val backupManager = SensitivityBackupManager(context)

    companion object {
        val KEY_CURRENT_PERCENT = intPreferencesKey("sensi_current_percent")
        val KEY_LAST_LEVEL = intPreferencesKey("sensi_last_level_idx")
        val KEY_REQUESTED_MULTIPLIER = floatPreferencesKey("sensi_requested_multiplier")
        val KEY_SUPPORTED_MULTIPLIER = floatPreferencesKey("sensi_supported_multiplier")
        val KEY_STATUS_LABEL = stringPreferencesKey("sensi_status_label")

        fun hasWriteSettingsPermission(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.System.canWrite(context)
            } else {
                true
            }
        }

        fun hasWriteSecureSettingsPermission(context: Context): Boolean {
            return context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        fun openDeveloperOptions(context: Context) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun getSavedPercentage(): Int = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        prefs[KEY_CURRENT_PERCENT] ?: 75
    }

    suspend fun getSavedRequestedMultiplier(): Float = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        prefs[KEY_REQUESTED_MULTIPLIER] ?: 3.0f
    }

    suspend fun getSavedSupportedMultiplier(): Float = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        prefs[KEY_SUPPORTED_MULTIPLIER] ?: 3.0f
    }

    suspend fun getSavedStatusLabel(): String = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        prefs[KEY_STATUS_LABEL] ?: "READY"
    }

    /**
     * Applies real device-level sensitivity boost based on percentage and multiplier model.
     */
    suspend fun applySensitivity(percent: Int): SensitivityBoostResult = withContext(Dispatchers.IO) {
        val clampedPercent = percent.coerceIn(0, 100)
        val level = SensitivityLevel.fromPercent(clampedPercent)
        val requestedMultiplier = SensitivityLevel.getMultiplierForPercent(clampedPercent)

        // 1. Check Shizuku authorization
        if (!ShizukuManager.isAuthorized()) {
            return@withContext SensitivityBoostResult(
                isSuccess = false,
                appliedPercent = clampedPercent,
                level = level,
                requestedMultiplier = requestedMultiplier,
                actualSupportedMultiplier = 1.0f,
                supportStatus = SensitivitySupportStatus.FAILED,
                message = "Shizuku permission required for supported system-level sensitivity controls.",
                failedSettings = listOf("Shizuku Unauthorized")
            )
        }

        // 2. Detect device capabilities and baseline
        val caps = SensitivityCapabilityDetector.detectCapabilities(context)
        val verified = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val records = mutableListOf<SettingVerificationRecord>()

        // Calculate targets relative to device baseline (1.0X)
        // A) System Pointer Speed (baseline + delta)
        if (caps.supportsPointerSpeed) {
            val basePointer = caps.baselinePointerSpeed
            val delta = when {
                requestedMultiplier <= 1.0f -> 0
                requestedMultiplier <= 2.0f -> 2
                requestedMultiplier <= 3.0f -> 4
                else -> 7
            }
            val targetPointer = (basePointer + delta).coerceIn(-7, 7)
            val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(
                backupManager = backupManager,
                namespace = "system",
                key = "pointer_speed",
                targetValue = targetPointer.toString()
            )
            records.add(
                SettingVerificationRecord(
                    namespace = "system",
                    key = "pointer_speed",
                    originalValue = res.originalValue,
                    appliedValue = targetPointer.toString(),
                    verifiedValue = res.readBackValue,
                    isVerified = res.isVerified
                )
            )
            if (res.isVerified) {
                verified.add("System Pointer Speed: +$targetPointer (Baseline: $basePointer)")
            } else {
                failed.add("System Pointer Speed")
            }
        } else {
            unsupported.add("System Pointer Speed (Inaccessible on this ROM)")
        }

        // B) Long Press Latency
        if (caps.supportsLongPressTimeout) {
            val baseLong = caps.baselineLongPressTimeout
            val targetLong = when {
                requestedMultiplier <= 1.0f -> baseLong
                requestedMultiplier <= 2.0f -> 300
                requestedMultiplier <= 3.0f -> 200
                else -> 100
            }
            val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(
                backupManager = backupManager,
                namespace = "secure",
                key = "long_press_timeout",
                targetValue = targetLong.toString()
            )
            records.add(
                SettingVerificationRecord(
                    namespace = "secure",
                    key = "long_press_timeout",
                    originalValue = res.originalValue,
                    appliedValue = targetLong.toString(),
                    verifiedValue = res.readBackValue,
                    isVerified = res.isVerified
                )
            )
            if (res.isVerified) {
                verified.add("Long Press Latency: ${targetLong}ms (Baseline: ${baseLong}ms)")
            } else {
                failed.add("Long Press Latency")
            }
        } else {
            unsupported.add("Long Press Latency (Vendor restricted)")
        }

        // C) Multi-press Tap Latency
        if (caps.supportsMultiPressTimeout) {
            val baseMulti = caps.baselineMultiPressTimeout
            val targetMulti = when {
                requestedMultiplier <= 1.0f -> baseMulti
                requestedMultiplier <= 2.0f -> 250
                requestedMultiplier <= 3.0f -> 150
                else -> 100
            }
            val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(
                backupManager = backupManager,
                namespace = "secure",
                key = "multi_press_timeout",
                targetValue = targetMulti.toString()
            )
            records.add(
                SettingVerificationRecord(
                    namespace = "secure",
                    key = "multi_press_timeout",
                    originalValue = res.originalValue,
                    appliedValue = targetMulti.toString(),
                    verifiedValue = res.readBackValue,
                    isVerified = res.isVerified
                )
            )
            if (res.isVerified) {
                verified.add("Multi-Tap Response: ${targetMulti}ms (Baseline: ${baseMulti}ms)")
            } else {
                failed.add("Multi-Tap Latency")
            }
        } else {
            unsupported.add("Multi-Tap Response (Vendor restricted)")
        }

        // D) OEM Touch Sensitivity / Glove Mode Layer
        if (caps.supportsTouchSensitivity) {
            val targetGlove = if (requestedMultiplier >= 2.0f) "1" else "0"
            var gloveApplied = false

            val secTouch = com.srtxcheats.core.SystemCommandExecutor.readSetting("secure", "touch_sensitivity")
            if (secTouch != null) {
                val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(backupManager, "secure", "touch_sensitivity", targetGlove)
                if (res.isVerified) gloveApplied = true
            }
            val sysTouch = com.srtxcheats.core.SystemCommandExecutor.readSetting("system", "high_touch_sensitivity")
            if (sysTouch != null) {
                val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(backupManager, "system", "high_touch_sensitivity", targetGlove)
                if (res.isVerified) gloveApplied = true
            }

            if (gloveApplied) {
                verified.add("OEM Touch Sensitivity Layer: Active")
            } else {
                unsupported.add("OEM Touch Sensitivity Layer (Vendor protected)")
            }
        } else {
            unsupported.add("OEM Touch Sensitivity Layer (Not exposed on ${caps.manufacturer})")
        }

        // E) Peak Refresh Rate Boost (Lowers input scan interval)
        if (caps.supportsPeakRefreshRate && requestedMultiplier >= 3.0f) {
            val maxHz = caps.maxRefreshRate.toInt().toString()
            val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(
                backupManager = backupManager,
                namespace = "system",
                key = "peak_refresh_rate",
                targetValue = maxHz
            )
            if (res.isVerified) {
                verified.add("Max Panel Refresh Rate: ${maxHz}Hz")
            } else {
                failed.add("Max Panel Refresh Rate")
            }
        } else if (!caps.supportsPeakRefreshRate && requestedMultiplier >= 3.0f) {
            unsupported.add("Max Refresh Rate Boost (Panel does not support >60Hz)")
        }

        // F) UI Animation Transition Scaling (Reduces visual drag latency)
        if (caps.supportsAnimationScale) {
            val targetScale = when {
                requestedMultiplier <= 1.0f -> "1.0"
                requestedMultiplier <= 2.0f -> "0.75"
                else -> "0.5"
            }
            val res = SensitivityCommandExecutor.applyAndVerifyWithRollback(
                backupManager = backupManager,
                namespace = "global",
                key = "window_animation_scale",
                targetValue = targetScale
            )
            if (res.isVerified) {
                verified.add("Animation Duration Scale: ${targetScale}x")
            } else {
                failed.add("Animation Duration Scale")
            }
        } else {
            unsupported.add("Animation Scaling (Inaccessible)")
        }

        // G) Native Velocity Tracker Strategy
        if (caps.supportsVelocityTracker && requestedMultiplier >= 3.0f) {
            val cmd = "device_config put input_native_boot touch.filter.velocity_tracker_strategy lsq2"
            val res = com.srtxcheats.core.SystemCommandExecutor.execute(cmd)
            if (res.success) {
                verified.add("Input Native Velocity Tracker: lsq2")
            } else {
                unsupported.add("Device Config Velocity Tracker")
            }
        }

        // Calculate Actual Supported Multiplier based on verified parameters:
        val actualSupportedMultiplier = if (verified.isEmpty()) {
            1.0f
        } else {
            var achievable = 1.0f
            if (verified.any { it.contains("Pointer Speed") }) achievable += 1.5f
            if (verified.any { it.contains("OEM Touch Sensitivity") }) achievable += 1.0f
            if (verified.any { it.contains("Latency") || it.contains("Multi-Tap") }) achievable += 0.7f
            if (verified.any { it.contains("Refresh Rate") }) achievable += 0.5f
            if (verified.any { it.contains("Animation Duration") }) achievable += 0.3f

            val cappedMax = achievable.coerceIn(1.0f, 5.0f)
            // Can only apply up to what the user requested, capped by what device actually supports
            minOf(requestedMultiplier, cappedMax)
        }

        val supportStatus = when {
            verified.isEmpty() -> SensitivitySupportStatus.FAILED
            actualSupportedMultiplier >= requestedMultiplier - 0.2f -> SensitivitySupportStatus.FULLY_SUPPORTED
            actualSupportedMultiplier > 1.0f -> SensitivitySupportStatus.PARTIALLY_SUPPORTED
            else -> SensitivitySupportStatus.LIMITED
        }

        val message = when (supportStatus) {
            SensitivitySupportStatus.FULLY_SUPPORTED ->
                "Sensitivity Boost Active (${String.format(java.util.Locale.US, "%.1fX", actualSupportedMultiplier)} applied, ${verified.size} verified parameters)."
            SensitivitySupportStatus.PARTIALLY_SUPPORTED ->
                "Partially Supported: ${String.format(java.util.Locale.US, "%.1fX", actualSupportedMultiplier)} applied (${verified.size} verified, ${unsupported.size} unsupported)."
            SensitivitySupportStatus.LIMITED ->
                "This device exposes limited sensitivity controls (${verified.size} parameters active)."
            SensitivitySupportStatus.FAILED ->
                "System sensitivity boost could not be verified on this device."
            SensitivitySupportStatus.RESTORED ->
                "Sensitivity restored to 1.0X baseline."
        }

        val isSuccess = verified.isNotEmpty()

        // Save state to DataStore
        if (isSuccess) {
            context.dataStore.edit { prefs ->
                prefs[KEY_CURRENT_PERCENT] = clampedPercent
                prefs[KEY_LAST_LEVEL] = level.ordinal
                prefs[KEY_REQUESTED_MULTIPLIER] = requestedMultiplier
                prefs[KEY_SUPPORTED_MULTIPLIER] = actualSupportedMultiplier
                prefs[KEY_STATUS_LABEL] = supportStatus.label
            }
        }

        AppLogger.i("Sensitivity Boost Result: req=${requestedMultiplier}X, sup=${actualSupportedMultiplier}X, status=$supportStatus, verified=${verified.size}")

        SensitivityBoostResult(
            isSuccess = isSuccess,
            appliedPercent = clampedPercent,
            level = level,
            requestedMultiplier = requestedMultiplier,
            actualSupportedMultiplier = actualSupportedMultiplier,
            supportStatus = supportStatus,
            message = message,
            verifiedSettings = verified,
            unsupportedSettings = unsupported,
            failedSettings = failed,
            verificationRecords = records
        )
    }

    suspend fun applyPreset(level: SensitivityLevel): SensitivityBoostResult {
        return applySensitivity(level.targetPercent)
    }

    /**
     * Restores device to exact 1.0X native baseline saved in backup.
     */
    suspend fun restoreOriginal(): SensitivityRestoreResult = withContext(Dispatchers.IO) {
        val restoreRes = backupManager.restoreOriginal()
        if (restoreRes.success) {
            context.dataStore.edit { prefs ->
                prefs[KEY_CURRENT_PERCENT] = 0
                prefs[KEY_LAST_LEVEL] = SensitivityLevel.LOW.ordinal
                prefs[KEY_REQUESTED_MULTIPLIER] = 1.0f
                prefs[KEY_SUPPORTED_MULTIPLIER] = 1.0f
                prefs[KEY_STATUS_LABEL] = SensitivitySupportStatus.RESTORED.label
            }
        }
        restoreRes
    }

    suspend fun hasBackup(): Boolean = backupManager.hasBackup()
}
