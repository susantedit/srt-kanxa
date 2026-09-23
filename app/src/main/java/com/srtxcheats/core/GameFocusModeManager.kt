package com.srtxcheats.core

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FocusModeState(
    val isFocusModeActive: Boolean = false,
    val hasDndPermission: Boolean = false,
    val blockedCallsCount: Int = 0,
    val blockedNotificationsCount: Int = 0,
    val statusMessage: String = "Normal Mode"
)

class GameFocusModeManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var previousInterruptionFilter: Int? = null
    private var previousRingerMode: Int? = null

    private val _focusState = MutableStateFlow(FocusModeState())
    val focusState: StateFlow<FocusModeState> = _focusState.asStateFlow()

    init {
        updatePermissionStatus()
    }

    fun hasDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    fun updatePermissionStatus() {
        _focusState.value = _focusState.value.copy(
            hasDndPermission = hasDndPermission()
        )
    }

    /**
     * Opens system Notification Policy Access settings page.
     */
    fun openDndSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Activates Game Focus Mode:
     * - Suppresses all calls, SMS, WhatsApp notifications and alerts completely.
     * - Silences ringer.
     */
    fun enableFocusMode(): Boolean {
        if (!hasDndPermission()) {
            openDndSettings()
            _focusState.value = _focusState.value.copy(
                hasDndPermission = false,
                statusMessage = "DND Permission Required"
            )
            return false
        }

        try {
            // Save baseline
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                previousInterruptionFilter = notificationManager.currentInterruptionFilter
                // Total Silence: INTERRUPTION_FILTER_NONE blocks calls, notifications, alarms
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }

            previousRingerMode = audioManager.ringerMode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

            _focusState.value = FocusModeState(
                isFocusModeActive = true,
                hasDndPermission = true,
                statusMessage = "FOCUS MODE ACTIVE: Total Silence (Zero Calls/Alerts)"
            )
            AppLogger.i("Game Focus Mode enabled: INTERRUPTION_FILTER_NONE + RINGER_MODE_SILENT")
            return true
        } catch (e: Exception) {
            AppLogger.w("Failed to activate Game Focus Mode", e)
            return false
        }
    }

    /**
     * Deactivates Game Focus Mode and restores original system audio and notification policies.
     */
    fun disableFocusMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasDndPermission()) {
                val originalFilter = previousInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(originalFilter)
            }

            val originalRinger = previousRingerMode ?: AudioManager.RINGER_MODE_NORMAL
            audioManager.ringerMode = originalRinger

            _focusState.value = FocusModeState(
                isFocusModeActive = false,
                hasDndPermission = hasDndPermission(),
                statusMessage = "Normal Mode (Notifications Restored)"
            )
            AppLogger.i("Game Focus Mode disabled: restored system baseline")
        } catch (e: Exception) {
            AppLogger.w("Failed to restore notification baseline", e)
        }
    }

    fun toggleFocusMode(): Boolean {
        return if (_focusState.value.isFocusModeActive) {
            disableFocusMode()
            false
        } else {
            enableFocusMode()
        }
    }
}
