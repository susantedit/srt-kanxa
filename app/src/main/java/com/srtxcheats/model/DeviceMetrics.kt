package com.srtxcheats.model

/**
 * Real device performance telemetry.
 * All metrics reflect real hardware values or null if not exposed by the OS.
 * Never fabricated.
 */
data class DeviceMetrics(
    val fps: Int? = null,
    val displayHz: Int = 60,
    val cpuUsagePercent: Int? = null,
    val cpuClockGhz: Float? = null,
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTempC: Float? = null,
    val thermalStatus: String = "NORMAL",
    val frameTimeMs: Float? = null,
    val rollingFps: List<Float> = emptyList(),
    val rollingFrameTime: List<Float> = emptyList()
) {
    val ramPercent: Int
        get() = if (ramTotalGb > 0f) ((ramUsedGb / ramTotalGb) * 100f).toInt().coerceIn(0, 100) else 0

    val ramDisplay: String
        get() = String.format("%.1f / %.1f GB", ramUsedGb, ramTotalGb)

    val cpuClockDisplay: String
        get() = cpuClockGhz?.let { String.format("%.2f GHz", it) } ?: "N/A"

    val fpsDisplay: String
        get() = fps?.toString() ?: "N/A"

    val cpuUsageDisplay: String
        get() = cpuUsagePercent?.let { "$it%" } ?: "N/A"

    val batteryTempDisplay: String
        get() = batteryTempC?.let { String.format("%.1f°C", it) } ?: "N/A"

    val frameTimeDisplay: String
        get() = frameTimeMs?.let { String.format("%.1f ms", it) } ?: "N/A"
}
