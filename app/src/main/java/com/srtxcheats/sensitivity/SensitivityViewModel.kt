package com.srtxcheats.sensitivity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.model.SensitivityLevel
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
    val hasBackup: Boolean = false
)

class SensitivityViewModel(private val context: Context) : ViewModel() {

    private val engine = SensitivityEngine(context)

    private val _uiState = MutableStateFlow(SensitivityUiState())
    val uiState: StateFlow<SensitivityUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        viewModelScope.launch {
            val savedPercent = engine.getSavedPercentage()
            val savedReq = engine.getSavedRequestedMultiplier()
            val savedSup = engine.getSavedSupportedMultiplier()
            val caps = SensitivityCapabilityDetector.detectCapabilities(context)
            val hasBkp = engine.hasBackup()
            _uiState.value = _uiState.value.copy(
                sliderPercent = savedPercent,
                currentLevel = SensitivityLevel.fromPercent(savedPercent),
                requestedMultiplier = savedReq,
                actualSupportedMultiplier = savedSup,
                capabilities = caps,
                hasBackup = hasBkp
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

            _uiState.value = _uiState.value.copy(
                isApplying = false,
                lastResult = result,
                requestedMultiplier = result.requestedMultiplier,
                actualSupportedMultiplier = result.actualSupportedMultiplier,
                supportStatus = result.supportStatus,
                statusMessage = result.message,
                isError = !result.isSuccess,
                capabilities = caps,
                hasBackup = hasBkp
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
                hasBackup = hasBkp
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
                hasBackup = hasBkp
            )
        }
    }
}
