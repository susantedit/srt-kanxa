package com.srtxcheats.core

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager
import com.srtxcheats.model.DeviceMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.LinkedList

class PerformanceMonitor(private val context: Context) {

    private val _metrics = MutableStateFlow(DeviceMetrics())
    val metrics: StateFlow<DeviceMetrics> = _metrics.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // CPU calculation state
    private var lastTotalCpuTime = 0L
    private var lastIdleCpuTime = 0L

    // FPS calculation state using Choreographer
    private val frameTimeHistory = LinkedList<Float>()
    private val fpsHistory = LinkedList<Float>()
    private var frameCount = 0
    private var lastFpsTimestampNs = 0L
    private var currentCalculatedFps: Int? = null
    private var currentFrameTimeMs: Float? = null

    // Battery state
    private var batteryLevel: Int = 100
    private var isCharging: Boolean = false
    private var batteryTempC: Float? = null

    private val choreographerCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFpsTimestampNs == 0L) {
                lastFpsTimestampNs = frameTimeNanos
            } else {
                val deltaNs = frameTimeNanos - lastFpsTimestampNs
                frameCount++
                if (deltaNs >= 1_000_000_000L) { // 1 second elapsed
                    val calculatedFps = ((frameCount * 1_000_000_000L) / deltaNs).toInt()
                    val frameTime = if (calculatedFps > 0) 1000f / calculatedFps else 16.6f
                    currentCalculatedFps = calculatedFps
                    currentFrameTimeMs = frameTime

                    synchronized(fpsHistory) {
                        if (fpsHistory.size >= 30) fpsHistory.removeFirst()
                        fpsHistory.add(calculatedFps.toFloat())

                        if (frameTimeHistory.size >= 30) frameTimeHistory.removeFirst()
                        frameTimeHistory.add(frameTime)
                    }

                    frameCount = 0
                    lastFpsTimestampNs = frameTimeNanos
                }
            }
            if (monitorJob?.isActive == true) {
                try {
                    Choreographer.getInstance().postFrameCallback(this)
                } catch (_: Exception) {}
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent ?: return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100) / scale
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            if (rawTemp > 0) {
                batteryTempC = rawTemp / 10.0f
            }
        }
    }

    fun startMonitoring(samplingIntervalMs: Long = 1000L) {
        if (monitorJob?.isActive == true) return

        // Register battery receiver
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
        } catch (_: Exception) {}

        // Start Choreographer callback on Main thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                lastFpsTimestampNs = 0L
                frameCount = 0
                Choreographer.getInstance().postFrameCallback(choreographerCallback)
            } catch (_: Exception) {}
        }

        monitorJob = scope.launch {
            while (isActive) {
                val currentMetrics = collectMetrics()
                _metrics.value = currentMetrics
                delay(samplingIntervalMs)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null

        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Choreographer.getInstance().removeFrameCallback(choreographerCallback)
            } catch (_: Exception) {}
        }
    }

    private fun collectMetrics(): DeviceMetrics {
        // 1. RAM usage
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        var ramUsedGb = 0f
        var ramTotalGb = 0f
        if (actManager != null) {
            actManager.getMemoryInfo(memInfo)
            val totalBytes = memInfo.totalMem
            val availBytes = memInfo.availMem
            val usedBytes = totalBytes - availBytes
            ramTotalGb = totalBytes / (1024f * 1024f * 1024f)
            ramUsedGb = usedBytes / (1024f * 1024f * 1024f)
        }

        // 2. CPU Usage
        val cpuUsage = readCpuUsage()

        // 3. CPU Clock
        val cpuClock = readCpuClockGhz()

        // 4. Display Refresh Rate
        val displayHz = readDisplayRefreshRate()

        // 5. Thermal Status
        val thermalStatus = readThermalStatus()

        val fpsCopy: List<Float>
        val frameTimeCopy: List<Float>
        synchronized(fpsHistory) {
            fpsCopy = fpsHistory.toList()
            frameTimeCopy = frameTimeHistory.toList()
        }

        return DeviceMetrics(
            fps = currentCalculatedFps,
            displayHz = displayHz,
            cpuUsagePercent = cpuUsage,
            cpuClockGhz = cpuClock,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            ramUsedGb = ramUsedGb,
            ramTotalGb = ramTotalGb,
            batteryPercent = batteryLevel,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            thermalStatus = thermalStatus,
            frameTimeMs = currentFrameTimeMs,
            rollingFps = fpsCopy,
            rollingFrameTime = frameTimeCopy
        )
    }

    private fun readCpuUsage(): Int? {
        return try {
            val file = File("/proc/stat")
            if (!file.canRead()) return null

            val reader = BufferedReader(FileReader(file))
            val firstLine = reader.readLine()
            reader.close()

            if (firstLine != null && firstLine.startsWith("cpu ")) {
                val parts = firstLine.trim().split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    val iowait = parts[5].toLong()
                    val irq = parts[6].toLong()
                    val softirq = parts[7].toLong()

                    val totalTime = user + nice + system + idle + iowait + irq + softirq
                    val idleTime = idle + iowait

                    if (lastTotalCpuTime != 0L) {
                        val totalDelta = totalTime - lastTotalCpuTime
                        val idleDelta = idleTime - lastIdleCpuTime
                        if (totalDelta > 0) {
                            val usage = ((totalDelta - idleDelta) * 100f / totalDelta).toInt()
                            lastTotalCpuTime = totalTime
                            lastIdleCpuTime = idleTime
                            return usage.coerceIn(0, 100)
                        }
                    }
                    lastTotalCpuTime = totalTime
                    lastIdleCpuTime = idleTime
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun readCpuClockGhz(): Float? {
        val paths = arrayOf(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq",
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
        )
        for (p in paths) {
            try {
                val file = File(p)
                if (file.canRead()) {
                    val line = file.readText().trim()
                    val khz = line.toLongOrNull()
                    if (khz != null && khz > 0) {
                        return (khz / 1_000_000f)
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun readDisplayRefreshRate(): Int {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                dm?.getDisplay(Display.DEFAULT_DISPLAY)
            } else {
                @Suppress("DEPRECATION")
                wm?.defaultDisplay
            }
            val rate = display?.refreshRate?.toInt() ?: 60
            if (rate > 0) rate else 60
        } catch (_: Exception) {
            60
        }
    }

    private fun readThermalStatus(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                return when (pm.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
                    PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                    PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                    PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                    else -> "NORMAL"
                }
            }
        }
        return "NORMAL"
    }
}
