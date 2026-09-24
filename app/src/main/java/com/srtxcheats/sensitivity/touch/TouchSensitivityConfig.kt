package com.srtxcheats.sensitivity.touch

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.srtxcheats.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * User-facing configuration for the real touch-sensitivity engine.
 *
 * Slider mapping (matches the reference panel's `sliderX`/`sliderY`, max=25 shown
 * as "1.0x"): multiplier = [GAIN_MIN] + progress * [GAIN_STEP], so progress 0→0.5x,
 * 5→1.0x (default), 25→3.0x. Smoothing is 0..100 shown as 0.00..1.00 (default 72).
 */
data class TouchSensitivityConfig(
    val enabled: Boolean = false,
    val gainX: Float = 1.0f,
    val gainY: Float = 1.0f,
    val smoothing: Float = 0.72f,
    val curve: TouchCurve = TouchCurve.LINEAR,
    val globalMode: Boolean = true,
    val perAppPackages: Set<String> = emptySet(),
) {
    companion object {
        const val GAIN_MIN = 0.5f
        const val GAIN_MAX = 3.0f
        const val GAIN_STEP = 0.1f
        const val GAIN_SLIDER_MAX = 25
        const val GAIN_SLIDER_DEFAULT = 5 // → 1.0x

        const val SMOOTH_SLIDER_MAX = 100
        const val SMOOTH_SLIDER_DEFAULT = 72 // → 0.72

        fun sliderToGain(progress: Int): Float =
            (GAIN_MIN + progress.coerceIn(0, GAIN_SLIDER_MAX) * GAIN_STEP)
                .coerceIn(GAIN_MIN, GAIN_MAX)

        fun gainToSlider(gain: Float): Int =
            (((gain.coerceIn(GAIN_MIN, GAIN_MAX) - GAIN_MIN) / GAIN_STEP)).toInt()
                .coerceIn(0, GAIN_SLIDER_MAX)

        fun sliderToSmoothing(progress: Int): Float =
            (progress.coerceIn(0, SMOOTH_SLIDER_MAX) / SMOOTH_SLIDER_MAX.toFloat())

        fun smoothingToSlider(smoothing: Float): Int =
            (smoothing.coerceIn(0f, 1f) * SMOOTH_SLIDER_MAX).toInt()
    }
}

/**
 * DataStore-backed store for [TouchSensitivityConfig], reusing the app's single
 * `srt_x_cheats_settings` preferences file and the same key/flow idiom as
 * [com.srtxcheats.data.DataStoreManager].
 */
class TouchSensitivityConfigStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("touch_sens_enabled")
        val GAIN_X = floatPreferencesKey("touch_sens_gain_x")
        val GAIN_Y = floatPreferencesKey("touch_sens_gain_y")
        val SMOOTHING = floatPreferencesKey("touch_sens_smoothing")
        val CURVE = intPreferencesKey("touch_sens_curve")
        val GLOBAL_MODE = booleanPreferencesKey("touch_sens_global_mode")
        val PER_APP = stringSetPreferencesKey("touch_sens_per_app")
    }

    val configFlow: Flow<TouchSensitivityConfig> = context.dataStore.data.map { prefs ->
        TouchSensitivityConfig(
            enabled = prefs[Keys.ENABLED] ?: false,
            gainX = prefs[Keys.GAIN_X] ?: 1.0f,
            gainY = prefs[Keys.GAIN_Y] ?: 1.0f,
            smoothing = prefs[Keys.SMOOTHING] ?: 0.72f,
            curve = TouchCurve.fromInt(prefs[Keys.CURVE] ?: 0),
            globalMode = prefs[Keys.GLOBAL_MODE] ?: true,
            perAppPackages = prefs[Keys.PER_APP] ?: emptySet(),
        )
    }

    suspend fun current(): TouchSensitivityConfig = configFlow.first()

    suspend fun save(config: TouchSensitivityConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED] = config.enabled
            prefs[Keys.GAIN_X] = config.gainX
            prefs[Keys.GAIN_Y] = config.gainY
            prefs[Keys.SMOOTHING] = config.smoothing
            prefs[Keys.CURVE] = TouchCurve.toInt(config.curve)
            prefs[Keys.GLOBAL_MODE] = config.globalMode
            prefs[Keys.PER_APP] = config.perAppPackages
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLED] = enabled }
    }
}
