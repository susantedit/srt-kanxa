package com.srtxcheats.sensitivity.touch

import android.content.Context
import android.os.RemoteCallbackList
import android.util.Log
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

    override fun destroy() {
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
