package com.srtxcheats.sensitivity.touch

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.srtxcheats.BuildConfig
import com.srtxcheats.core.GameDetector
import com.srtxcheats.core.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.File

/**
 * App-process controller for the real touch-sensitivity engine.
 *
 * Owns the Shizuku [Shizuku.UserServiceArgs] bind lifecycle, resolves the native
 * `libtouchgrab.so` path from this app's install dir, pushes [TouchSensitivityConfig]
 * to the privileged [TouchSensitivityUserService], and exposes an honest [state]
 * for the UI (main screen + overlay) to observe.
 *
 * It also runs the app-side watchdog: the engine is force-stopped if the service
 * process dies, and — in per-app mode — when the target game leaves the foreground.
 * The native helper's own PDEATHSIG + heartbeat guards are the last-resort backstop
 * if this process is killed outright.
 *
 * Process-wide singleton: every UI surface and [com.srtxcheats.sensitivity.SensitivityEngine]
 * must drive the *same* bound service, so callers share one instance via [getInstance].
 */
class TouchSensitivityController private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val bindMutex = Mutex()

    private val _state = MutableStateFlow(TouchEngineState())
    val state: StateFlow<TouchEngineState> = _state.asStateFlow()

    @Volatile private var service: ITouchSensitivityService? = null
    @Volatile private var bindContinuation: kotlinx.coroutines.CompletableDeferred<ITouchSensitivityService?>? = null
    @Volatile private var watchdogJob: Job? = null
    @Volatile private var lastConfig: TouchSensitivityConfig? = null

    // --- public API -----------------------------------------------------------

    /** True when Shizuku (or root) is available and authorized to host the service. */
    fun isPrivileged(): Boolean = ShizukuManager.isAuthorized()

    /**
     * Enumerate touch devices without grabbing. Candidates arrive on [state].
     * Safe: the native helper never grabs in `--detect` mode.
     */
    suspend fun detect() {
        val svc = ensureBound() ?: return
        _state.update { it.copy(phase = TouchEnginePhase.DETECTING, candidates = emptyList(), message = null) }
        val bin = binaryPath()
        if (bin == null) {
            fail("libtouchgrab.so not found in ${appContext.applicationInfo.nativeLibraryDir}")
            return
        }
        runCatching { svc.detect(bin) }
            .onFailure { fail("detect failed: ${it.message}") }
    }

    /**
     * Start (or restart) the grab → transform → inject loop with [config].
     * No-op unless [TouchSensitivityConfig.enabled]; call [stop] to disable.
     */
    suspend fun start(config: TouchSensitivityConfig) {
        if (!config.enabled) {
            stop()
            return
        }
        lastConfig = config
        val svc = ensureBound() ?: return
        val bin = binaryPath() ?: run {
            fail("libtouchgrab.so not found in ${appContext.applicationInfo.nativeLibraryDir}")
            return
        }
        _state.update { it.copy(phase = TouchEnginePhase.STARTING, message = null) }
        val ok = runCatching {
            svc.start(
                bin,
                config.gainX,
                config.gainY,
                config.smoothing,
                TouchCurve.toInt(config.curve),
                config.globalMode,
                config.perAppPackages.toTypedArray(),
            )
        }.onFailure { fail("start failed: ${it.message}") }.isSuccess
        if (ok) startWatchdog(config)
    }

    /** Live-update the transform without restarting the grab. */
    suspend fun updateConfig(config: TouchSensitivityConfig) {
        lastConfig = config
        if (!config.enabled) {
            stop()
            return
        }
        val svc = service ?: run {
            // Not bound yet — a start() will pick up lastConfig.
            start(config)
            return
        }
        runCatching {
            svc.updateConfig(
                config.gainX,
                config.gainY,
                config.smoothing,
                TouchCurve.toInt(config.curve),
                config.globalMode,
                config.perAppPackages.toTypedArray(),
            )
        }.onFailure { fail("updateConfig failed: ${it.message}") }
        restartWatchdog(config)
    }

    /** Release the grab and stop injecting. Safe to call when already stopped. */
    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        val svc = service
        scope.launch {
            runCatching { svc?.stop() }
            _state.update {
                if (it.phase == TouchEnginePhase.ERROR) it
                else it.copy(phase = TouchEnginePhase.BOUND, devicePath = null, message = "Stopped")
            }
        }
    }

    /** Full teardown: stop, tell the service to exit, unbind. */
    fun shutdown() {
        watchdogJob?.cancel()
        watchdogJob = null
        val svc = service
        scope.launch {
            runCatching { svc?.stop() }
            runCatching { svc?.unregisterStatus(statusCallback) }
        }
        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
        service = null
        _state.update { TouchEngineState() }
    }

    // --- binding ---------------------------------------------------------------

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, TouchSensitivityUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("touchgrab")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = if (binder != null && binder.pingBinder()) {
                ITouchSensitivityService.Stub.asInterface(binder)
            } else null
            service = svc
            if (svc != null) {
                runCatching { svc.registerStatus(statusCallback) }
                _state.update { it.copy(phase = TouchEnginePhase.BOUND, message = null) }
            } else {
                _state.update { it.copy(phase = TouchEnginePhase.ERROR, message = "Bind returned dead binder") }
            }
            bindContinuation?.complete(svc)
            bindContinuation = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Service process died — the native helper's PDEATHSIG has already
            // released the grab. Reflect it and drop our reference.
            service = null
            watchdogJob?.cancel()
            watchdogJob = null
            _state.update {
                it.copy(phase = TouchEnginePhase.IDLE, devicePath = null, message = "Service disconnected")
            }
            bindContinuation?.complete(null)
            bindContinuation = null
        }
    }

    private suspend fun ensureBound(): ITouchSensitivityService? {
        service?.let { return it }
        if (!ShizukuManager.isAuthorized()) {
            _state.update {
                it.copy(phase = TouchEnginePhase.UNAVAILABLE, message = "Shizuku not authorized")
            }
            return null
        }
        return bindMutex.withLock {
            service?.let { return@withLock it }
            _state.update { it.copy(phase = TouchEnginePhase.BINDING, message = null) }
            val deferred = kotlinx.coroutines.CompletableDeferred<ITouchSensitivityService?>()
            bindContinuation = deferred
            val requested = runCatching {
                Shizuku.bindUserService(userServiceArgs, connection)
            }
            if (requested.isFailure) {
                bindContinuation = null
                fail("bindUserService failed: ${requested.exceptionOrNull()?.message}")
                return@withLock null
            }
            withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
                ?: run {
                    bindContinuation = null
                    fail("Timed out binding user service")
                    null
                }
        }
    }

    private fun binaryPath(): String? {
        val dir = appContext.applicationInfo.nativeLibraryDir ?: return null
        val f = File(dir, "libtouchgrab.so")
        return if (f.exists()) f.absolutePath else null
    }

    // --- status callback -------------------------------------------------------

    private val statusCallback = object : ITouchStatusCallback.Stub() {
        override fun onStatus(status: String?, detail: String?) {
            when (status) {
                "grabbed" -> _state.update {
                    it.copy(phase = TouchEnginePhase.GRABBED, devicePath = detail ?: it.devicePath, message = null)
                }
                "detect_complete_no_grab" -> _state.update {
                    it.copy(phase = TouchEnginePhase.BOUND, message = "Detection complete")
                }
                "heartbeat_timeout" -> _state.update {
                    it.copy(phase = TouchEnginePhase.BOUND, devicePath = null, message = "Heartbeat timeout — released")
                }
                else -> Log.d(TAG, "status=$status detail=$detail")
            }
        }

        override fun onDeviceResolved(path: String?, name: String?, xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
            _state.update {
                it.copy(
                    devicePath = path?.takeIf { p -> p.isNotEmpty() } ?: it.devicePath,
                    deviceName = name?.takeIf { n -> n.isNotEmpty() } ?: it.deviceName,
                    range = if (xMax > xMin && yMax > yMin) IntRangeBox(xMin, xMax, yMin, yMax) else it.range,
                )
            }
        }

        override fun onCandidate(path: String?, score: Int, name: String?, xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
            val c = TouchCandidate(path.orEmpty(), score, name.orEmpty(), xMin, xMax, yMin, yMax)
            _state.update { it.copy(candidates = (it.candidates + c).sortedByDescending { s -> s.score }) }
        }

        override fun onError(code: String?, message: String?) {
            fail("[$code] ${message.orEmpty()}")
        }

        override fun onStopped(reason: String?) {
            _state.update {
                if (it.phase == TouchEnginePhase.ERROR) it
                else it.copy(phase = TouchEnginePhase.BOUND, devicePath = null, message = "Stopped: ${reason.orEmpty()}")
            }
        }
    }

    // --- watchdog --------------------------------------------------------------

    private fun startWatchdog(config: TouchSensitivityConfig) {
        watchdogJob?.cancel()
        // Global mode grabs everywhere; only per-app mode needs foreground gating,
        // and only if usage-stats access is granted (otherwise we can't observe it).
        if (config.globalMode || config.perAppPackages.isEmpty()) return
        if (!GameDetector.hasUsageStatsPermission(appContext)) return

        watchdogJob = scope.launch {
            val targets = config.perAppPackages
            while (true) {
                delay(WATCHDOG_INTERVAL_MS)
                val fg = GameDetector.getForegroundPackageName(appContext) ?: continue
                // Ignore our own UI/overlay coming to front while tuning.
                if (fg == appContext.packageName) continue
                if (fg !in targets) {
                    Log.i(TAG, "Target left foreground ($fg) — auto-stopping engine")
                    stop()
                    break
                }
            }
        }
    }

    private fun restartWatchdog(config: TouchSensitivityConfig) {
        if (_state.value.phase == TouchEnginePhase.GRABBED) startWatchdog(config)
    }

    private fun fail(message: String) {
        Log.w(TAG, message)
        _state.update { it.copy(phase = TouchEnginePhase.ERROR, message = message) }
    }

    companion object {
        private const val TAG = "TouchSensCtl"
        private const val BIND_TIMEOUT_MS = 8000L
        private const val WATCHDOG_INTERVAL_MS = 1500L

        @Volatile private var instance: TouchSensitivityController? = null

        fun getInstance(context: Context): TouchSensitivityController =
            instance ?: synchronized(this) {
                instance ?: TouchSensitivityController(context).also { instance = it }
            }
    }
}

/** Digitizer ABS range surfaced to the UI. */
data class IntRangeBox(val xMin: Int, val xMax: Int, val yMin: Int, val yMax: Int)

/** A touch-device candidate from `--detect`, for the UI to show / pick. */
data class TouchCandidate(
    val path: String,
    val score: Int,
    val name: String,
    val xMin: Int,
    val xMax: Int,
    val yMin: Int,
    val yMax: Int,
)

enum class TouchEnginePhase {
    IDLE,          // never bound / after disconnect
    UNAVAILABLE,   // Shizuku not authorized
    BINDING,       // bind in flight
    BOUND,         // service alive, not grabbing
    DETECTING,     // enumerating devices, no grab
    STARTING,      // start() issued, awaiting "grabbed"
    GRABBED,       // engine live, injecting
    ERROR,
}

/** Observable engine status for the UI. */
data class TouchEngineState(
    val phase: TouchEnginePhase = TouchEnginePhase.IDLE,
    val devicePath: String? = null,
    val deviceName: String? = null,
    val range: IntRangeBox? = null,
    val candidates: List<TouchCandidate> = emptyList(),
    val message: String? = null,
) {
    val isActive: Boolean get() = phase == TouchEnginePhase.GRABBED
}
