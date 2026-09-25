package com.srtxcheats.sensitivity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.sensitivity.touch.TouchCurve
import com.srtxcheats.sensitivity.touch.TouchEnginePhase
import com.srtxcheats.sensitivity.touch.TouchSensitivityConfig
import com.srtxcheats.sensitivity.touch.TouchSensitivityConfigStore
import com.srtxcheats.sensitivity.touch.TouchSensitivityController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensitivityUiState(
    val sliderPercent: Int = 75,
    val currentLevel: SensitivityLevel = SensitivityLevel.HIGH,
    val requestedMultiplier: Float = 3.0f,
    val actualSupportedMultiplier: Float = 3.0f,
    val supportStatus: SensitivitySupportStatus = SensitivitySupportStatus.FULLY_SUPPORTED,
    val isApplying: Boolean = false,
    val isRestoring: Boolean = false,
    val lastResult: SensitivityBoostResult? = null,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val capabilities: DeviceSensitivityCapabilities? = null,
    val hasBackup: Boolean = false,
    // --- real touch engine (advanced per-axis live tuning) ---
    val gainX: Float = 1.0f,
    val gainY: Float = 1.0f,
    val smoothing: Float = 0.72f,
    val curve: TouchCurve = TouchCurve.LINEAR,
    val engineActive: Boolean = false,
    val enginePhase: TouchEnginePhase = TouchEnginePhase.IDLE,
    val engineMessage: String? = null,
    val engineDevice: String? = null,
    val engineCandidateCount: Int = 0,
)

class SensitivityViewModel(private val context: Context) : ViewModel() {

    private val engine = SensitivityEngine(context)
    private val touchController = TouchSensitivityController.getInstance(context)
    private val touchConfigStore = TouchSensitivityConfigStore(context)

    private val _uiState = MutableStateFlow(SensitivityUiState())
    val uiState: StateFlow<SensitivityUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        observeEngine()
    }

    /** Fold the real engine's honest status into the UI state as it changes. */
    private fun observeEngine() {
        viewModelScope.launch {
            touchController.state.collect { st ->
                _uiState.value = _uiState.value.copy(
                    engineActive = st.isActive,
                    enginePhase = st.phase,
                    engineMessage = st.message,
                    engineDevice = st.deviceName ?: st.devicePath,
                    engineCandidateCount = st.candidates.size
                )
            }
        }
    }

    fun loadInitialState() {
        viewModelScope.launch {
            val savedPercent = engine.getSavedPercentage()
            val savedReq = engine.getSavedRequestedMultiplier()
            val savedSup = engine.getSavedSupportedMultiplier()
            val caps = SensitivityCapabilityDetector.detectCapabilities(context)
            val hasBkp = engine.hasBackup()
            val touchCfg = runCatching { touchConfigStore.current() }.getOrDefault(TouchSensitivityConfig())
            _uiState.value = _uiState.value.copy(
                sliderPercent = savedPercent,
                currentLevel = SensitivityLevel.fromPercent(savedPercent),
                requestedMultiplier = savedReq,
                actualSupportedMultiplier = savedSup,
                capabilities = caps,
                hasBackup = hasBkp,
                gainX = touchCfg.gainX,
                gainY = touchCfg.gainY,
                smoothing = touchCfg.smoothing,
                curve = touchCfg.curve
            )
        }
    }

    fun onSliderChange(newPercent: Int) {
        val clamped = newPercent.coerceIn(0, 100)
        val level = SensitivityLevel.fromPercent(clamped)
        val reqMult = SensitivityLevel.getMultiplierForPercent(clamped)
        _uiState.value = _uiState.value.copy(
            sliderPercent = clamped,
            currentLevel = level,
            requestedMultiplier = reqMult
        )
    }

    fun selectPreset(level: SensitivityLevel) {
        onSliderChange(level.targetPercent)
    }

    fun applySensitivity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, statusMessage = null)
            val result = engine.applySensitivity(_uiState.value.sliderPercent)
            val caps = SensitivityCapabilityDetector.detectCapabilities(context)
            val hasBkp = engine.hasBackup()
            val touchCfg = runCatching { touchConfigStore.current() }.getOrDefault(TouchSensitivityConfig())

            _uiState.value = _uiState.value.copy(
                isApplying = false,
                lastResult = result,
                requestedMultiplier = result.requestedMultiplier,
                actualSupportedMultiplier = result.actualSupportedMultiplier,
                supportStatus = result.supportStatus,
                statusMessage = result.message,
                isError = !result.isSuccess,
                capabilities = caps,
                hasBackup = hasBkp,
                gainX = touchCfg.gainX,
                gainY = touchCfg.gainY,
                smoothing = touchCfg.smoothing,
                curve = touchCfg.curve
            )
        }
    }

    fun restoreSensitivity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, statusMessage = null)
            val result = engine.restoreOriginal()
            val caps = SensitivityCapabilityDetector.detectCapabilities(context)
            val hasBkp = engine.hasBackup()

            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                sliderPercent = 0,
                currentLevel = SensitivityLevel.LOW,
                requestedMultiplier = 1.0f,
                actualSupportedMultiplier = 1.0f,
                supportStatus = SensitivitySupportStatus.RESTORED,
                statusMessage = result.message,
                isError = !result.success,
                capabilities = caps,
                hasBackup = hasBkp,
                gainX = 1.0f,
                gainY = 1.0f
            )
        }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission(1001)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            loadInitialState()
        }
    }

    fun applyIphoneIqooUltraMode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, statusMessage = null)
            val result = engine.applyIphoneIqooUltraMode()
            val caps = SensitivityCapabilityDetector.detectCapabilities(context)
            val hasBkp = engine.hasBackup()
            val touchCfg = runCatching { touchConfigStore.current() }.getOrDefault(TouchSensitivityConfig())

            _uiState.value = _uiState.value.copy(
                isApplying = false,
                sliderPercent = 100,
                currentLevel = SensitivityLevel.ULTRA_HIGH,
                lastResult = result,
                requestedMultiplier = result.requestedMultiplier,
                actualSupportedMultiplier = result.actualSupportedMultiplier,
                supportStatus = result.supportStatus,
                statusMessage = result.message,
                isError = !result.isSuccess,
                capabilities = caps,
                hasBackup = hasBkp,
                gainX = touchCfg.gainX,
                gainY = touchCfg.gainY,
                smoothing = touchCfg.smoothing,
                curve = touchCfg.curve
            )
        }
    }

    // ==========================================================================
    // Real touch engine — advanced per-axis live tuning (X / Y / smoothing / curve)
    // ==========================================================================

    fun setGainX(gain: Float) = updateTouchConfig { it.copy(gainX = clampGain(gain)) }

    fun setGainY(gain: Float) = updateTouchConfig { it.copy(gainY = clampGain(gain)) }

    fun setSmoothing(value: Float) = updateTouchConfig { it.copy(smoothing = value.coerceIn(0f, 1f)) }

    fun setCurve(curve: TouchCurve) = updateTouchConfig { it.copy(curve = curve) }

    private fun clampGain(g: Float) =
        g.coerceIn(TouchSensitivityConfig.GAIN_MIN, TouchSensitivityConfig.GAIN_MAX)

    private fun updateTouchConfig(mutate: (TouchSensitivityConfig) -> TouchSensitivityConfig) {
        viewModelScope.launch {
            val base = runCatching { touchConfigStore.current() }.getOrDefault(TouchSensitivityConfig())
            val next = mutate(base)
            _uiState.value = _uiState.value.copy(
                gainX = next.gainX,
                gainY = next.gainY,
                smoothing = next.smoothing,
                curve = next.curve
            )
            runCatching { touchConfigStore.save(next) }
            // Live-update the running grab (no restart); no-op when not active.
            if (touchController.state.value.isActive) {
                runCatching { touchController.updateConfig(next.copy(enabled = true)) }
            }
        }
    }

    /** Enumerate touch devices without grabbing — safe diagnostic, never locks touch. */
    fun detectTouchDevices() {
        viewModelScope.launch { runCatching { touchController.detect() } }
    }

    /** Turn the real touch engine OFF (release the grab) without touching the settings backup. */
    fun disableTouchEngine() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, statusMessage = null)
            engine.disableTouchEngine()
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                statusMessage = "Real Touch Engine stopped — grab released.",
                isError = false
            )
        }
    }
}
