package com.srtxcheats.model

data class GameProfile(
    val packageName: String,
    val appName: String,
    val isOverlayEnabled: Boolean = true,
    val showFps: Boolean = true,
    val showFrameTime: Boolean = true,
    val showCpu: Boolean = true,
    val showRam: Boolean = true,
    val showTemp: Boolean = true,
    val showBattery: Boolean = true,
    val showClock: Boolean = true,
    val showDisplayHz: Boolean = true,
    val showGraph: Boolean = false,
    val showCrosshair: Boolean = false,
    val crosshairStyle: String = "CROSS", // DOT, CROSS, CIRCLE, RETICLE
    val crosshairColor: Long = 0xFF00E5FF, // Cyan default
    val crosshairSizeDp: Int = 24,
    val crosshairOffsetX: Int = 0, // Dynamic X position offset in px from screen center
    val crosshairOffsetY: Int = 0, // Dynamic Y position offset in px from screen center
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.HIGH,
    val sensitivityPercent: Int = 75, // 0 - 100% slider, default 75%
    val isPlus50BoostActive: Boolean = false, // +50% Overclock boost
    val isFreeStyle800Active: Boolean = false, // 800% Free Style Player mode
    val isDragModeActive: Boolean = false, // Drag Headshot Player mode
    val isUltraProMaxActive: Boolean = false, // Ultra Pro Max External High Sensi via Shizuku/ADB (no DPI needed)
    val freeStyleDpi: Int = 720, // Free Style recommended DPI
    val isRamBoostActive: Boolean = true,
    val lastRamFreedMb: Long = 0L,
    val isGameBoostActive: Boolean = false,
    val isGamingModeActive: Boolean = false,
    val isPerformanceBoostActive: Boolean = false,
    val overlayScale: Float = 1.0f,
    val expandedMenuSize: Float = 1.0f, // 10% - 200% independent menu size
    val overlayAlpha: Float = 0.85f,
    val glassTransparency: Float = 0.85f, // 0% - 100%
    val glassBlur: String = "MEDIUM", // OFF, LOW, MEDIUM, HIGH
    val overlayPosX: Int = 50,
    val overlayPosY: Int = 200
) {
    val effectiveSensitivityPercent: Int
        get() = when {
            isUltraProMaxActive -> 999
            isFreeStyle800Active -> 800
            isDragModeActive -> 500
            isPlus50BoostActive -> (sensitivityPercent + 50).coerceIn(0, 250)
            else -> sensitivityPercent.coerceIn(0, 200)
        }

    companion object {
        const val FREE_FIRE_PACKAGE = "com.dts.freefireth"
        const val FREE_FIRE_MAX_PACKAGE = "com.dts.freefiremax"

        fun defaultFor(pkg: String, name: String): GameProfile {
            return GameProfile(
                packageName = pkg,
                appName = name,
                isOverlayEnabled = true,
                showFps = true,
                showFrameTime = true,
                showCpu = true,
                showRam = true,
                showTemp = true,
                showBattery = true,
                showClock = true,
                showDisplayHz = true,
                showGraph = false,
                sensitivityLevel = SensitivityLevel.HIGH,
                sensitivityPercent = 150,
                isPlus50BoostActive = false,
                isFreeStyle800Active = false,
                freeStyleDpi = 720,
                isRamBoostActive = true,
                isGameBoostActive = false,
                isGamingModeActive = false,
                isPerformanceBoostActive = false,
                overlayScale = 1.0f,
                expandedMenuSize = 1.0f,
                overlayAlpha = 0.85f,
                glassTransparency = 0.85f,
                glassBlur = "MEDIUM"
            )
        }
    }
}
