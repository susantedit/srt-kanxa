package com.srtxcheats.sensitivity.touch

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.InputEvent
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowManager

/**
 * Re-injects transformed touch frames as [MotionEvent]s via the hidden
 * `InputManager.injectInputEvent` API. Runs inside the Shizuku user-service
 * process (uid 2000 = com.android.shell), which holds INJECT_EVENTS — the same
 * path `adb shell input` uses.
 *
 * The native helper grabs the touchscreen exclusively, so the app would receive
 * no touches at all unless we synthesize the full gesture stream here. This
 * diffs consecutive [TouchFrame]s into ACTION_DOWN / POINTER_DOWN / MOVE /
 * POINTER_UP / UP, mapping ABS coordinates to display pixels with rotation via
 * [CoordinateMapper].
 *
 * Every reflective call is guarded; on any failure it reports through [onFailure]
 * and injection stops rather than crashing the service.
 */
class InputInjector(
    private val context: Context,
    private val range: CoordinateMapper.Range,
    private val onFailure: (String) -> Unit,
) {
    private val injectMethod: java.lang.reflect.Method?
    private val inputManagerInstance: Any?

    // Gesture state.
    private var downTime = 0L
    private val trackingToPointerId = LinkedHashMap<Int, Int>()
    private val lastCoords = HashMap<Int, Pair<Float, Float>>() // trackingId -> screen px
    private val usedPointerIds = HashSet<Int>()

    // Cached display geometry, refreshed on demand.
    @Volatile private var displayW = 0
    @Volatile private var displayH = 0
    @Volatile private var rotation = 0

    init {
        var method: java.lang.reflect.Method? = null
        var instance: Any? = null
        try {
            val imClass = Class.forName("android.hardware.input.InputManager")
            instance = imClass.getMethod("getInstance").invoke(null)
            method = imClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "InputManager.injectInputEvent unavailable", t)
        }
        injectMethod = method
        inputManagerInstance = instance
        refreshDisplay()
    }

    fun isReady(): Boolean = injectMethod != null && inputManagerInstance != null

    /** Re-read display size + rotation (call on rotation changes / grab start). */
    @Suppress("DEPRECATION")
    fun refreshDisplay() {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val display = wm.defaultDisplay ?: return
            val pt = android.graphics.Point()
            display.getRealSize(pt)
            displayW = pt.x
            displayH = pt.y
            rotation = display.rotation
        } catch (t: Throwable) {
            Log.w(TAG, "refreshDisplay failed", t)
        }
    }

    /**
     * Apply one transformed frame. Emits the MotionEvents needed to move from the
     * previous frame's pointer set to this one.
     */
    fun submitFrame(frame: TouchFrame) {
        if (!isReady()) return
        val now = SystemClock.uptimeMillis()

        val newTrackingIds = frame.pointers.map { it.trackingId }.toSet()
        val oldTrackingIds = trackingToPointerId.keys.toSet()

        // Screen coords for this frame.
        val screenByTracking = HashMap<Int, Pair<Float, Float>>()
        for (p in frame.pointers) {
            screenByTracking[p.trackingId] =
                CoordinateMapper.toScreen(p.x, p.y, range, displayW, displayH, rotation)
        }

        // 1. Pointers that lifted.
        val lifted = oldTrackingIds - newTrackingIds
        for (tid in lifted) {
            emitPointerUp(tid, now)
        }

        // 2. Pointers that pressed.
        val pressed = newTrackingIds - oldTrackingIds
        for (tid in pressed) {
            val coord = screenByTracking[tid] ?: continue
            emitPointerDown(tid, coord, now)
        }

        // 3. If any pointers remain, emit a MOVE with everyone's current position.
        lastCoords.putAll(screenByTracking.filterKeys { it in trackingToPointerId })
        if (trackingToPointerId.isNotEmpty() && (pressed.isEmpty() || trackingToPointerId.size > pressed.size)) {
            emitMove(now)
        } else if (trackingToPointerId.isNotEmpty() && pressed.isNotEmpty()) {
            // Fresh presses already injected DOWN with coords; a follow-up MOVE
            // keeps the stream consistent for multi-finger starts.
            emitMove(now)
        }
    }

    /** Release every held pointer (called on stop / teardown). */
    fun releaseAll() {
        if (trackingToPointerId.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val ids = trackingToPointerId.keys.toList()
        for (tid in ids) {
            emitPointerUp(tid, now)
        }
        trackingToPointerId.clear()
        lastCoords.clear()
        usedPointerIds.clear()
    }

    private fun allocatePointerId(): Int {
        var id = 0
        while (id in usedPointerIds) id++
        usedPointerIds.add(id)
        return id
    }

    private fun emitPointerDown(tid: Int, coord: Pair<Float, Float>, now: Long) {
        val isFirst = trackingToPointerId.isEmpty()
        if (isFirst) downTime = now
        val pointerId = allocatePointerId()
        trackingToPointerId[tid] = pointerId
        lastCoords[tid] = coord

        val action = if (isFirst) {
            MotionEvent.ACTION_DOWN
        } else {
            val index = trackingToPointerId.keys.indexOf(tid)
            MotionEvent.ACTION_POINTER_DOWN or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        }
        buildAndInject(action, now)
    }

    private fun emitPointerUp(tid: Int, now: Long) {
        if (!trackingToPointerId.containsKey(tid)) return
        val isLast = trackingToPointerId.size == 1
        val action = if (isLast) {
            MotionEvent.ACTION_UP
        } else {
            val index = trackingToPointerId.keys.indexOf(tid)
            MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        }
        buildAndInject(action, now)

        val pid = trackingToPointerId.remove(tid)
        if (pid != null) usedPointerIds.remove(pid)
        lastCoords.remove(tid)
    }

    private fun emitMove(now: Long) {
        buildAndInject(MotionEvent.ACTION_MOVE, now)
    }

    /**
     * Build a MotionEvent describing the full current pointer set and inject it.
     * The action's index bits (for POINTER_DOWN/UP) are already encoded by the
     * caller against the [trackingToPointerId] insertion order.
     */
    private fun buildAndInject(action: Int, now: Long) {
        val n = trackingToPointerId.size
        if (n == 0) return

        val props = arrayOfNulls<MotionEvent.PointerProperties>(n)
        val coords = arrayOfNulls<MotionEvent.PointerCoords>(n)

        var i = 0
        for ((tid, pointerId) in trackingToPointerId) {
            val pp = MotionEvent.PointerProperties()
            pp.id = pointerId
            pp.toolType = MotionEvent.TOOL_TYPE_FINGER
            props[i] = pp

            val (x, y) = lastCoords[tid] ?: Pair(0f, 0f)
            val pc = MotionEvent.PointerCoords()
            pc.x = x
            pc.y = y
            pc.pressure = 1.0f
            pc.size = 1.0f
            coords[i] = pc
            i++
        }

        var event: MotionEvent? = null
        try {
            event = MotionEvent.obtain(
                downTime,
                now,
                action,
                n,
                props,
                coords,
                0, // metaState
                0, // buttonState
                1.0f, // xPrecision
                1.0f, // yPrecision
                0, // deviceId (0 = virtual)
                0, // edgeFlags
                InputDevice.SOURCE_TOUCHSCREEN,
                0, // flags
            )
            inject(event)
        } catch (t: Throwable) {
            onFailure("inject_failed: ${t.message}")
        } finally {
            event?.recycle()
        }
    }

    private fun inject(event: MotionEvent) {
        val m = injectMethod ?: return
        val im = inputManagerInstance ?: return
        try {
            m.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
        } catch (t: Throwable) {
            onFailure("inject_invoke_failed: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "InputInjector"
        // android.hardware.input.InputManager.INJECT_INPUT_EVENT_MODE_ASYNC
        private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
    }
}
