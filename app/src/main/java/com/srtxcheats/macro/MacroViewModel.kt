package com.srtxcheats.macro

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.sensitivity.touch.TouchSensitivityController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI state for the Macro / auto-fire screen.
 *
 * Two independent gaming tools share this screen, both driven by the privileged touch service:
 *  - **Auto-clicker** — taps a chosen screen point at [cps] clicks/second (the user's "10-100 click/s"),
 *    looping until stopped or, when [loop] is off, for [burstCount] taps.
 *  - **Macro recorder / player** — records the user's real touches (Free Fire combos, gloo-wall placement,
 *    drag-shoot, etc.), saves them by name, and replays them at [playSpeedPercent] of the recorded speed.
 */
data class MacroUiState(
    val isPrivileged: Boolean = false,
    val phase: MacroPhase = MacroPhase.IDLE,
    val message: String? = null,
    val isError: Boolean = false,

    // Saved macros (newest first). Summaries only — frames are loaded lazily on play.
    val macros: List<MacroSummary> = emptyList(),

    // Auto-clicker controls.
    val cps: Int = MacroTiming.DEFAULT_CPS,
    val autoClickX: Float = 0.5f,     // normalized 0..1 across screen width
    val autoClickY: Float = 0.5f,     // normalized 0..1 across screen height
    val burstCount: Int = 50,

    // Playback controls (shared loop flag with the auto-clicker).
    val loop: Boolean = true,
    val playSpeedPercent: Int = MacroTiming.DEFAULT_SPEED_PERCENT,
    val activeMacroId: String? = null,

    // A just-finished recording awaiting a name from the user. Non-null → show the save dialog.
    val pendingRecording: String? = null,
) {
    val isRecording: Boolean get() = phase == MacroPhase.RECORDING
    val isRunning: Boolean get() = phase == MacroPhase.PLAYING || phase == MacroPhase.AUTOCLICK
    val isBusy: Boolean get() = isRecording || isRunning
}

/**
 * Screen-scoped controller for the macro subsystem. Mirrors [com.srtxcheats.sensitivity.SensitivityViewModel]'s
 * shape: constructed with `remember { MacroViewModel(context) }`, holds a single [MacroUiState] flow, and folds
 * the privileged service's [TouchSensitivityController.macroState] into it so the UI always reflects what the
 * uid-2000 process is actually doing.
 *
 * All persistence ([MacroStore]) is blocking IO and runs on [Dispatchers.IO]; the privileged calls are suspend
 * and hop threads inside the controller.
 */
class MacroViewModel(private val context: Context) : ViewModel() {

    private val controller = TouchSensitivityController.getInstance(context)
    private val store = MacroStore(context)

    private val _uiState = MutableStateFlow(MacroUiState())
    val uiState: StateFlow<MacroUiState> = _uiState.asStateFlow()

    init {
        refreshPrivilege()
        loadMacros()
        observeMacroState()
    }

    /** Fold the privileged service's honest macro status into the UI as it changes. */
    private fun observeMacroState() {
        viewModelScope.launch {
            controller.macroState.collect { st ->
                _uiState.update {
                    it.copy(
                        phase = st.phase,
                        message = st.message ?: it.message,
                        isError = st.phase == MacroPhase.ERROR,
                        // Playback/auto-click ended → clear the active highlight.
                        activeMacroId = if (st.phase == MacroPhase.PLAYING) it.activeMacroId else null,
                    )
                }
                // A recording that finished via the service broadcast (not our stopRecording() return)
                // still carries the serialized macro in lastRecorded — surface it as a save prompt.
                if (st.phase == MacroPhase.IDLE && !st.lastRecorded.isNullOrEmpty() &&
                    _uiState.value.pendingRecording == null
                ) {
                    _uiState.update { it.copy(pendingRecording = st.lastRecorded) }
                }
            }
        }
    }

    fun refreshPrivilege() {
        _uiState.update { it.copy(isPrivileged = ShizukuManager.isAuthorized()) }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission(REQUEST_CODE)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            refreshPrivilege()
        }
    }

    fun loadMacros() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { store.summaries() }
            _uiState.update { it.copy(macros = list) }
        }
    }

    // --- recording --------------------------------------------------------------

    /** Start capturing the user's touches. The game keeps responding (no grab), so they play normally. */
    fun startRecording() {
        if (!ensurePrivileged()) return
        _uiState.update { it.copy(message = "Recording — play your moves, then Stop.", isError = false) }
        viewModelScope.launch { controller.startRecording() }
    }

    /**
     * Stop recording. The serialized macro is returned directly by the service; if non-empty we open the
     * save dialog by parking it in [MacroUiState.pendingRecording].
     */
    fun stopRecording() {
        viewModelScope.launch {
            val serialized = controller.stopRecording()
            if (serialized.isBlank()) {
                _uiState.update { it.copy(message = "Nothing recorded — no touches captured.", isError = true) }
            } else {
                _uiState.update { it.copy(pendingRecording = serialized, message = "Recording captured — name it to save.", isError = false) }
            }
        }
    }

    /** Persist the pending recording under [name]. No-op if there is nothing pending. */
    fun saveRecording(name: String) {
        val serialized = _uiState.value.pendingRecording ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { store.saveRecorded(serialized, name) }
            if (saved != null) {
                _uiState.update { it.copy(pendingRecording = null, message = "Saved \"${saved.name}\".", isError = false) }
                loadMacros()
            } else {
                _uiState.update { it.copy(message = "Could not save recording.", isError = true) }
            }
        }
    }

    /** Throw away the pending recording without saving. */
    fun discardRecording() {
        _uiState.update { it.copy(pendingRecording = null, message = "Recording discarded.", isError = false) }
    }

    // --- playback ---------------------------------------------------------------

    /** Replay a saved macro at the current [MacroUiState.playSpeedPercent] and loop setting. */
    fun play(summary: MacroSummary) {
        if (!ensurePrivileged()) return
        val speed = _uiState.value.playSpeedPercent
        val loop = _uiState.value.loop
        viewModelScope.launch {
            val serialized = withContext(Dispatchers.IO) {
                store.read(summary.id)?.let { MacroCodec.encode(it) }
            }
            if (serialized.isNullOrEmpty()) {
                _uiState.update { it.copy(message = "Macro \"${summary.name}\" could not be loaded.", isError = true) }
                return@launch
            }
            _uiState.update { it.copy(activeMacroId = summary.id, message = "Playing \"${summary.name}\".", isError = false) }
            controller.playMacro(serialized, speed, loop)
        }
    }

    fun deleteMacro(summary: MacroSummary) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(summary.id) }
            _uiState.update { it.copy(message = "Deleted \"${summary.name}\".", isError = false) }
            loadMacros()
        }
    }

    fun renameMacro(summary: MacroSummary, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.rename(summary.id, newName) }
            loadMacros()
        }
    }

    // --- auto-clicker -----------------------------------------------------------

    /** Start the auto-clicker at the configured point / rate / loop. */
    fun startAutoClick() {
        if (!ensurePrivileged()) return
        val s = _uiState.value
        viewModelScope.launch {
            controller.startAutoClick(s.autoClickX, s.autoClickY, s.cps, s.loop, s.burstCount)
        }
    }

    // --- shared stop ------------------------------------------------------------

    /** Turn OFF any running auto-click or playback. Does not affect recording or the sensitivity grab. */
    fun stopMacro() {
        controller.stopMacro()
        _uiState.update { it.copy(activeMacroId = null) }
    }

    /** Master off: stop playback/auto-click and, if recording, stop that too (prompting to save). */
    fun stopEverything() {
        if (_uiState.value.isRecording) stopRecording()
        stopMacro()
    }

    // --- setters ----------------------------------------------------------------

    fun setCps(cps: Int) = _uiState.update { it.copy(cps = MacroTiming.clampCps(cps)) }

    fun setSpeedPercent(percent: Int) =
        _uiState.update { it.copy(playSpeedPercent = MacroTiming.clampSpeedPercent(percent)) }

    fun setLoop(loop: Boolean) = _uiState.update { it.copy(loop = loop) }

    fun setBurstCount(count: Int) = _uiState.update { it.copy(burstCount = count.coerceIn(1, 9999)) }

    /** Set the auto-click target as a normalized (0..1) screen position. */
    fun setAutoClickPoint(nx: Float, ny: Float) = _uiState.update {
        it.copy(autoClickX = nx.coerceIn(0f, 1f), autoClickY = ny.coerceIn(0f, 1f))
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun ensurePrivileged(): Boolean {
        if (ShizukuManager.isAuthorized()) return true
        _uiState.update { it.copy(isPrivileged = false, message = "Shizuku permission required.", isError = true) }
        return false
    }

    companion object {
        private const val REQUEST_CODE = 1002
    }
}
