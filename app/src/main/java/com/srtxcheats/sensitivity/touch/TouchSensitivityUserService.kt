package com.srtxcheats.sensitivity.touch

import android.content.Context
import android.os.RemoteCallbackList
import android.os.SystemClock
import android.util.Log
import com.srtxcheats.macro.Macro
import com.srtxcheats.macro.MacroCodec
import com.srtxcheats.macro.MacroFrame
import com.srtxcheats.macro.MacroInjector
import com.srtxcheats.macro.MacroPointer
import com.srtxcheats.macro.MacroTiming
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The privileged half of the sensitivity engine, hosted by Shizuku in a uid-2000
 * ("shell") process. It execs the native `touchgrab` helper, reads its event
 * stream, applies [TouchTransform], and re-injects via [InputInjector] — all
 * in-process so there is no per-event IPC.
 *
 * Shizuku instantiates this with the single-Context constructor and calls
 * [destroy] (transaction 16777114) at shutdown. Every teardown path releases the
 * grab: the native helper's own PDEATHSIG/heartbeat guards are the last resort,
 * but we also close its stdin (EOF → clean ungrab) and destroy the process here.
 */
class TouchSensitivityUserService(private val context: Context) :
    ITouchSensitivityService.Stub() {

    @Suppress("unused")
    constructor() : this(fetchSystemContext())

    private val callbacks = RemoteCallbackList<ITouchStatusCallback>()

    private val running = AtomicBoolean(false)
    private val currentDevicePath = AtomicReference<String?>(null)
    private val configRef = AtomicReference(Snapshot())

    @Volatile private var process: Process? = null
    @Volatile private var heartbeatThread: Thread? = null
    @Volatile private var readerThread: Thread? = null
    @Volatile private var transform: TouchTransform? = null
    @Volatile private var injector: InputInjector? = null
    @Volatile private var activeRange: TouchGrabEvent.Range? = null

    // --- Macro / auto-fire state (independent of the sensitivity grab above) ---
    // macroRunning covers auto-click + playback; recording is tracked separately so
    // stopping one never disturbs the other, and neither ever touches the grab.
    private val macroRunning = AtomicBoolean(false)
    private val recording = AtomicBoolean(false)
    @Volatile private var macroThread: Thread? = null
    @Volatile private var recordThread: Thread? = null
    @Volatile private var recordProcess: Process? = null
    @Volatile private var recordHeartbeatThread: Thread? = null
    @Volatile private var macroInjector: MacroInjector? = null
    @Volatile private var playbackInjector: InputInjector? = null

    // Recording accumulation (guarded by recordLock).
    private val recordLock = Any()
    private val recordedFrames = ArrayList<MacroFrame>()
    private var recordRange: TouchGrabEvent.Range? = null
    private var recordStartMs = 0L

    /** Config held until a TOUCHGRAB_RANGE lets us build [TransformParams]. */
    private data class Snapshot(
        val gainX: Float = 1.0f,
        val gainY: Float = 1.0f,
        val smoothing: Float = 0.72f,
        val curve: Int = 0,
        val globalMode: Boolean = true,
        val perApp: List<String> = emptyList(),
    )

    override fun detect(binPath: String) {
        Thread {
            var proc: Process? = null
            try {
                proc = ProcessBuilder(binPath, "--detect").redirectErrorStream(false).start()
                BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                    for (line in lines) dispatch(TouchGrabProtocol.parse(line))
                }
                proc.waitFor()
            } catch (t: Throwable) {
                notifyError("detect_failed", t.message ?: "unknown")
            } finally {
                try { proc?.destroy() } catch (_: Throwable) {}
            }
        }.apply { isDaemon = true; name = "touchgrab-detect" }.start()
    }

    override fun start(
        binPath: String,
        gainX: Float,
        gainY: Float,
        smoothing: Float,
        curve: Int,
        globalMode: Boolean,
        perAppPackages: Array<String>?,
    ) {
        if (running.get()) stop()

        configRef.set(Snapshot(gainX, gainY, smoothing, curve, globalMode, perAppPackages?.toList() ?: emptyList()))

        val proc: Process = try {
            ProcessBuilder(binPath, "--auto").redirectErrorStream(false).start()
        } catch (t: Throwable) {
            notifyError("exec_failed", t.message ?: "cannot exec $binPath")
            return
        }
        process = proc
        activeRange = null
        running.set(true)

        startHeartbeat(proc.outputStream)
        startReader(proc)
    }

    private fun startHeartbeat(stdin: OutputStream) {
        Thread {
            try {
                while (running.get()) {
                    stdin.write('.'.code)
                    stdin.flush()
                    Thread.sleep(HEARTBEAT_INTERVAL_MS)
                }
            } catch (_: Throwable) {
                // Pipe closed on teardown.
            }
        }.apply {
            isDaemon = true
            name = "touchgrab-heartbeat"
            heartbeatThread = this
            start()
        }
    }

    private fun startReader(proc: Process) {
        Thread {
            var reason = "ended"
            try {
                BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                    for (line in lines) {
                        if (!running.get()) break
                        handleEvent(TouchGrabProtocol.parse(line))
                    }
                }
                reason = "stream_closed"
            } catch (t: Throwable) {
                reason = "reader_error: ${t.message}"
            } finally {
                teardown(reason)
            }
        }.apply {
            isDaemon = true
            name = "touchgrab-reader"
            readerThread = this
            start()
        }
    }

    private fun handleEvent(ev: TouchGrabEvent) {
        when (ev) {
            is TouchGrabEvent.Range -> {
                activeRange = ev
                buildEngine(ev)
                dispatch(ev)
            }
            is TouchGrabEvent.Device -> {
                currentDevicePath.set(ev.path)
                dispatch(ev)
            }
            is TouchGrabEvent.Abs, is TouchGrabEvent.SynReport -> {
                val frame = transform?.onEvent(ev)
                if (frame != null) injector?.submitFrame(frame)
            }
            else -> dispatch(ev)
        }
    }

    private fun buildEngine(range: TouchGrabEvent.Range) {
        val snap = configRef.get()
        transform = TouchTransform(paramsFrom(snap, range))
        injector = InputInjector(
            context = context,
            range = CoordinateMapper.Range(range.xMin, range.xMax, range.yMin, range.yMax),
            onFailure = { msg -> notifyError("injector", msg) },
        ).also {
            if (!it.isReady()) notifyError("injector", "injectInputEvent unavailable in this process")
        }
    }

    private fun paramsFrom(snap: Snapshot, range: TouchGrabEvent.Range) = TransformParams(
        gainX = snap.gainX,
        gainY = snap.gainY,
        smoothing = snap.smoothing,
        curve = TouchCurve.fromInt(snap.curve),
        xMin = range.xMin,
        xMax = range.xMax,
        yMin = range.yMin,
        yMax = range.yMax,
    )

    override fun updateConfig(
        gainX: Float,
        gainY: Float,
        smoothing: Float,
        curve: Int,
        globalMode: Boolean,
        perAppPackages: Array<String>?,
    ) {
        val snap = Snapshot(gainX, gainY, smoothing, curve, globalMode, perAppPackages?.toList() ?: emptyList())
        configRef.set(snap)
        val range = activeRange ?: return
        transform?.updateParams(paramsFrom(snap, range))
    }

    override fun stop() = teardown("stopped")

    override fun isRunning(): Boolean = running.get()

    override fun currentDevice(): String? = currentDevicePath.get()

    override fun registerStatus(cb: ITouchStatusCallback?) {
        if (cb != null) callbacks.register(cb)
    }

    override fun unregisterStatus(cb: ITouchStatusCallback?) {
        if (cb != null) callbacks.unregister(cb)
    }

    // --- Macro / auto-fire (see ITouchSensitivityService transactions 9..14) ---

    override fun startAutoClick(
        nx: Float,
        ny: Float,
        clicksPerSecond: Int,
        loop: Boolean,
        burstCount: Int,
    ) {
        stopMacroInternal()

        val inj = MacroInjector(context) { msg -> broadcastMacro("error", msg) }
        if (!inj.isReady()) {
            broadcastMacro("error", "injectInputEvent unavailable in this process")
            return
        }
        inj.refreshDisplay()
        macroInjector = inj

        val cps = MacroTiming.clampCps(clicksPerSecond)
        val period = MacroTiming.clickPeriodMs(cps)
        val hold = MacroTiming.clickHoldMs(cps)
        val fx = nx.coerceIn(0f, 1f)
        val fy = ny.coerceIn(0f, 1f)

        macroRunning.set(true)
        Thread {
            var count = 0
            try {
                broadcastMacro("autoclick", "cps=$cps")
                while (macroRunning.get()) {
                    var w = inj.screenWidth()
                    var h = inj.screenHeight()
                    if (w <= 0 || h <= 0) {
                        inj.refreshDisplay()
                        w = inj.screenWidth(); h = inj.screenHeight()
                        if (w <= 0 || h <= 0) { Thread.sleep(period); continue }
                    }
                    val x = fx * w
                    val y = fy * h
                    inj.injectDown(x, y)
                    try {
                        Thread.sleep(hold)
                    } finally {
                        // Always pair the UP with the DOWN so an interrupt never
                        // leaves a stuck finger on screen.
                        inj.injectUp(x, y)
                    }
                    count++
                    if (!loop && burstCount > 0 && count >= burstCount) break
                    val gap = period - hold
                    if (gap > 0) Thread.sleep(gap)
                }
            } catch (_: InterruptedException) {
                // Stopped from stopMacro()/teardown — clean exit.
            } catch (t: Throwable) {
                broadcastMacro("error", "autoclick: ${t.message}")
            } finally {
                macroRunning.set(false)
                macroInjector = null
                broadcastMacro("stopped", "autoclick")
            }
        }.apply { isDaemon = true; name = "macro-autoclick"; macroThread = this; start() }
    }

    override fun startMacroPlayback(macroData: String, speedPercent: Int, loop: Boolean) {
        stopMacroInternal()

        val macro = MacroCodec.decode("playback", macroData)
        if (macro == null || macro.isEmpty) {
            broadcastMacro("error", "empty or invalid macro")
            return
        }
        if (!macro.hasRange) {
            broadcastMacro("error", "macro has no digitizer range")
            return
        }

        val inj = InputInjector(
            context = context,
            range = CoordinateMapper.Range(macro.xMin, macro.xMax, macro.yMin, macro.yMax),
            onFailure = { msg -> broadcastMacro("error", msg) },
        )
        if (!inj.isReady()) {
            broadcastMacro("error", "injectInputEvent unavailable in this process")
            return
        }
        inj.refreshDisplay()
        playbackInjector = inj

        val speed = MacroTiming.clampSpeedPercent(speedPercent)
        macroRunning.set(true)
        Thread {
            try {
                broadcastMacro("playing", "frames=${macro.frameCount} speed=$speed")
                do {
                    var prevAt = 0L
                    for (frame in macro.frames) {
                        if (!macroRunning.get()) break
                        val wait = MacroTiming.scaledDelayMs(frame.atMs - prevAt, speed)
                        if (wait > 0) Thread.sleep(wait)
                        prevAt = frame.atMs
                        inj.submitFrame(
                            TouchFrame(frame.pointers.map { PointerOut(it.slot, it.trackingId, it.x, it.y) }),
                        )
                    }
                    inj.releaseAll()
                } while (loop && macroRunning.get())
            } catch (_: InterruptedException) {
                // Stopped — clean exit.
            } catch (t: Throwable) {
                broadcastMacro("error", "playback: ${t.message}")
            } finally {
                try { inj.releaseAll() } catch (_: Throwable) {}
                macroRunning.set(false)
                playbackInjector = null
                broadcastMacro("stopped", "playback")
            }
        }.apply { isDaemon = true; name = "macro-playback"; macroThread = this; start() }
    }

    override fun startRecording(binPath: String) {
        if (recording.get()) {
            broadcastMacro("error", "already recording")
            return
        }
        val proc: Process = try {
            ProcessBuilder(binPath, "--monitor").redirectErrorStream(false).start()
        } catch (t: Throwable) {
            broadcastMacro("error", "record exec failed: ${t.message ?: "cannot exec $binPath"}")
            return
        }
        recordProcess = proc
        synchronized(recordLock) {
            recordedFrames.clear()
            recordRange = null
            recordStartMs = 0L
        }
        recording.set(true)
        startRecordHeartbeat(proc.outputStream)

        Thread {
            // Identity transform (gain 1, smoothing 0) → the raw ABS coordinates the
            // user actually touched, rebuilt once the RANGE line gives us the axes.
            var localTransform: TouchTransform? = null
            try {
                broadcastMacro("recording_started", "")
                BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                    for (line in lines) {
                        if (!recording.get()) break
                        when (val ev = TouchGrabProtocol.parse(line)) {
                            is TouchGrabEvent.Range -> {
                                synchronized(recordLock) { recordRange = ev }
                                localTransform = TouchTransform(identityParamsFor(ev))
                            }
                            is TouchGrabEvent.Abs, is TouchGrabEvent.SynReport -> {
                                val t = localTransform
                                if (t != null) {
                                    val frame = t.onEvent(ev)
                                    if (frame != null) appendRecordedFrame(frame)
                                }
                            }
                            is TouchGrabEvent.Error -> broadcastMacro("error", "${ev.code} ${ev.message ?: ""}".trim())
                            else -> {}
                        }
                    }
                }
            } catch (t: Throwable) {
                broadcastMacro("error", "record: ${t.message}")
            }
        }.apply { isDaemon = true; name = "macro-record"; recordThread = this; start() }
    }

    override fun stopRecording(): String {
        if (!recording.compareAndSet(true, false)) return ""
        // Closing stdin gives the --monitor helper a clean EOF → it exits and
        // closes its stdout, unblocking our reader; destroy() is the backstop.
        try { recordProcess?.outputStream?.close() } catch (_: Throwable) {}
        try { recordProcess?.destroy() } catch (_: Throwable) {}
        val t = recordThread
        recordThread = null
        try { t?.interrupt() } catch (_: Throwable) {}
        try { t?.join(600) } catch (_: Throwable) {}
        recordProcess = null
        recordHeartbeatThread = null

        val frames: List<MacroFrame>
        val range: TouchGrabEvent.Range?
        synchronized(recordLock) {
            frames = ArrayList(recordedFrames)
            range = recordRange
        }
        if (frames.isEmpty() || range == null) {
            broadcastMacro("stopped", "recording_empty")
            return ""
        }
        val macro = Macro(
            id = "rec",
            name = "Recording",
            xMin = range.xMin, xMax = range.xMax, yMin = range.yMin, yMax = range.yMax,
            frames = frames,
            createdAtMs = System.currentTimeMillis(),
        )
        val serialized = MacroCodec.encode(macro)
        broadcastMacro("recorded", serialized)
        return serialized
    }

    override fun stopMacro() = stopMacroInternal()

    override fun isMacroActive(): Boolean = macroRunning.get() || recording.get()

    /** Stop a running auto-click / playback. Leaves recording and the grab untouched. */
    private fun stopMacroInternal() {
        if (macroRunning.compareAndSet(true, false)) {
            val t = macroThread
            macroThread = null
            try { t?.interrupt() } catch (_: Throwable) {}
            try { t?.join(500) } catch (_: Throwable) {}
            try { playbackInjector?.releaseAll() } catch (_: Throwable) {}
            playbackInjector = null
            macroInjector = null
        }
    }

    /** Force-stop an in-progress recording without emitting the captured macro (used on destroy). */
    private fun stopRecordingQuietly() {
        if (!recording.compareAndSet(true, false)) return
        try { recordProcess?.outputStream?.close() } catch (_: Throwable) {}
        try { recordProcess?.destroy() } catch (_: Throwable) {}
        val t = recordThread
        recordThread = null
        try { t?.interrupt() } catch (_: Throwable) {}
        try { t?.join(400) } catch (_: Throwable) {}
        recordProcess = null
        recordHeartbeatThread = null
    }

    private fun startRecordHeartbeat(stdin: OutputStream) {
        Thread {
            try {
                while (recording.get()) {
                    stdin.write('.'.code)
                    stdin.flush()
                    Thread.sleep(HEARTBEAT_INTERVAL_MS)
                }
            } catch (_: Throwable) {
                // Pipe closed on stop — expected.
            }
        }.apply {
            isDaemon = true
            name = "macro-record-heartbeat"
            recordHeartbeatThread = this
            start()
        }
    }

    private fun appendRecordedFrame(frame: TouchFrame) {
        val now = SystemClock.uptimeMillis()
        synchronized(recordLock) {
            if (recordStartMs == 0L) recordStartMs = now
            val atMs = now - recordStartMs
            recordedFrames.add(
                MacroFrame(atMs, frame.pointers.map { MacroPointer(it.slot, it.trackingId, it.x, it.y) }),
            )
        }
    }

    private fun identityParamsFor(range: TouchGrabEvent.Range) = TransformParams(
        gainX = 1.0f,
        gainY = 1.0f,
        smoothing = 0.0f,
        curve = TouchCurve.fromInt(0),
        xMin = range.xMin,
        xMax = range.xMax,
        yMin = range.yMin,
        yMax = range.yMax,
    )

    override fun destroy() {
        stopMacroInternal()
        stopRecordingQuietly()
        teardown("destroy")
        try { callbacks.kill() } catch (_: Throwable) {}
        System.exit(0)
    }

    private fun teardown(reason: String) {
        if (!running.compareAndSet(true, false)) {
            try { injector?.releaseAll() } catch (_: Throwable) {}
            return
        }
        // Closing stdin gives the native helper a clean EOF → it ungrabs itself.
        try { process?.outputStream?.close() } catch (_: Throwable) {}
        try { injector?.releaseAll() } catch (_: Throwable) {}
        try { process?.destroy() } catch (_: Throwable) {}
        process = null
        transform = null
        injector = null
        activeRange = null
        currentDevicePath.set(null)
        heartbeatThread = null
        readerThread = null
        notifyStopped(reason)
    }

    // --- callback fan-out ---

    private fun dispatch(ev: TouchGrabEvent) {
        broadcast { cb ->
            when (ev) {
                is TouchGrabEvent.Status -> cb.onStatus(ev.status, ev.detail)
                is TouchGrabEvent.Error -> cb.onError(ev.code, ev.message)
                is TouchGrabEvent.Device -> {
                    val r = activeRange
                    cb.onDeviceResolved(ev.path, "", r?.xMin ?: 0, r?.xMax ?: 0, r?.yMin ?: 0, r?.yMax ?: 0)
                }
                is TouchGrabEvent.Range ->
                    cb.onDeviceResolved(currentDevicePath.get() ?: "", "", ev.xMin, ev.xMax, ev.yMin, ev.yMax)
                is TouchGrabEvent.Candidate ->
                    cb.onCandidate(ev.path, ev.score, ev.name, ev.xMin, ev.xMax, ev.yMin, ev.yMax)
                is TouchGrabEvent.Ready -> cb.onStatus("ready", null)
                is TouchGrabEvent.Name -> cb.onStatus("name", ev.name)
                else -> {}
            }
        }
    }

    private fun notifyError(code: String, message: String) {
        Log.w(TAG, "error $code: $message")
        broadcast { it.onError(code, message) }
    }

    private fun notifyStopped(reason: String) {
        broadcast { it.onStopped(reason) }
    }

    private fun broadcastMacro(kind: String, detail: String) {
        broadcast { it.onMacroEvent(kind, detail) }
    }

    private inline fun broadcast(action: (ITouchStatusCallback) -> Unit) {
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                try { action(callbacks.getBroadcastItem(i)) } catch (_: Throwable) {}
            }
        } finally {
            try { callbacks.finishBroadcast() } catch (_: Throwable) {}
        }
    }

    companion object {
        private const val TAG = "TouchSensSvc"
        private const val HEARTBEAT_INTERVAL_MS = 500L

        /** Best-effort system context for the no-arg constructor path. */
        private fun fetchSystemContext(): Context {
            return try {
                val atClass = Class.forName("android.app.ActivityThread")
                val systemMain = atClass.getMethod("systemMain").invoke(null)
                atClass.getMethod("getSystemContext").invoke(systemMain) as Context
            } catch (t: Throwable) {
                throw IllegalStateException("No Context available for user service", t)
            }
        }
    }
}
