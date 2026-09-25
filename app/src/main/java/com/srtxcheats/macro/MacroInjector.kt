package com.srtxcheats.macro

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.WindowManager

/**
 * Single-pointer, screen-space tap injector for the auto-clicker. Runs inside the Shizuku
 * user-service process (uid 2000 = com.android.shell, which holds INJECT_EVENTS), reusing the same
 * hidden `InputManager.injectInputEvent` path as [com.srtxcheats.sensitivity.touch.InputInjector].
 *
 * Recorded gesture playback goes through InputInjector (multi-touch, ABS-mapped); this simpler
 * injector only synthesizes taps at absolute screen pixels for the "N clicks/second" auto-fire.
 */
class MacroInjector(
    private val context: Context,
    private val onFailure: (String) -> Unit,
) {
    private val injectMethod: java.lang.reflect.Method?
    private val inputManagerInstance: Any?

    @Volatile private var displayW = 0
    @Volatile private var displayH = 0

    private var downTime = 0L

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

    fun screenWidth(): Int = displayW
    fun screenHeight(): Int = displayH

    @Suppress("DEPRECATION")
    fun refreshDisplay() {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val display = wm.defaultDisplay ?: return
            val pt = android.graphics.Point()
            display.getRealSize(pt)
            displayW = pt.x
            displayH = pt.y
        } catch (t: Throwable) {
            Log.w(TAG, "refreshDisplay failed", t)
        }
    }

    /** Press down at screen pixel (x,y). Returns the down time to pair with [injectUp]. */
    fun injectDown(x: Float, y: Float): Long {
        downTime = SystemClock.uptimeMillis()
        emit(MotionEvent.ACTION_DOWN, downTime, x, y)
        return downTime
    }

    fun injectUp(x: Float, y: Float) {
        emit(MotionEvent.ACTION_UP, SystemClock.uptimeMillis(), x, y)
    }

    private fun emit(action: Int, eventTime: Long, x: Float, y: Float) {
        if (!isReady()) return
        val m = injectMethod ?: return
        val im = inputManagerInstance ?: return
        var event: MotionEvent? = null
        try {
            event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            m.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
        } catch (t: Throwable) {
            onFailure("autoclick_inject_failed: ${t.message}")
        } finally {
            event?.recycle()
        }
    }

    companion object {
        private const val TAG = "MacroInjector"
        private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
    }
}
