package com.srtxcheats.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "srt_x_cheats_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_SELECTED_PACKAGE = stringPreferencesKey("selected_package")
        private val KEY_SELECTED_APP_NAME = stringPreferencesKey("selected_app_name")
        private val KEY_OVERLAY_SCALE = floatPreferencesKey("overlay_scale")
        private val KEY_EXPANDED_MENU_SIZE = floatPreferencesKey("expanded_menu_size")
        private val KEY_OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
        private val KEY_GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        private val KEY_GLASS_BLUR = stringPreferencesKey("glass_blur")
        private val KEY_OVERLAY_POS_X = intPreferencesKey("overlay_pos_x")
        private val KEY_OVERLAY_POS_Y = intPreferencesKey("overlay_pos_y")
        private val KEY_SHOW_FPS = booleanPreferencesKey("show_fps")
        private val KEY_SHOW_FRAME_TIME = booleanPreferencesKey("show_frame_time")
        private val KEY_SHOW_CPU = booleanPreferencesKey("show_cpu")
        private val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        private val KEY_SHOW_RAM = booleanPreferencesKey("show_ram")
        private val KEY_SHOW_TEMP = booleanPreferencesKey("show_temp")
        private val KEY_SHOW_BATTERY = booleanPreferencesKey("show_battery")
        private val KEY_SHOW_DISPLAY_HZ = booleanPreferencesKey("show_display_hz")
        private val KEY_SHOW_GRAPH = booleanPreferencesKey("show_graph")
        private val KEY_SHOW_CROSSHAIR = booleanPreferencesKey("show_crosshair")
        private val KEY_CROSSHAIR_STYLE = stringPreferencesKey("crosshair_style")
        private val KEY_CROSSHAIR_COLOR = longPreferencesKey("crosshair_color")
        private val KEY_CROSSHAIR_SIZE = intPreferencesKey("crosshair_size")
        private val KEY_CROSSHAIR_OFFSET_X = intPreferencesKey("crosshair_offset_x")
        private val KEY_CROSSHAIR_OFFSET_Y = intPreferencesKey("crosshair_offset_y")
        private val KEY_SENSITIVITY_LEVEL = stringPreferencesKey("sensitivity_level")
        private val KEY_SENSITIVITY_PERCENT = intPreferencesKey("sensitivity_percent")
        private val KEY_PLUS_50_BOOST_ACTIVE = booleanPreferencesKey("plus_50_boost_active")
        private val KEY_FREE_STYLE_800_ACTIVE = booleanPreferencesKey("free_style_800_active")
        private val KEY_DRAG_MODE_ACTIVE = booleanPreferencesKey("drag_mode_active")
        private val KEY_ULTRA_PRO_MAX_ACTIVE = booleanPreferencesKey("ultra_pro_max_active")
        private val KEY_FREE_STYLE_DPI = intPreferencesKey("free_style_dpi")
        private val KEY_TOUCH_OPTIMIZATION_ACTIVE = booleanPreferencesKey("touch_opt_active")
        private val KEY_RAM_BOOST_ACTIVE = booleanPreferencesKey("ram_boost_active")
        private val KEY_LAST_RAM_FREED_MB = longPreferencesKey("last_ram_freed_mb")
        private val KEY_GAME_BOOST_ACTIVE = booleanPreferencesKey("game_boost_active")
        private val KEY_GAMING_MODE_ACTIVE = booleanPreferencesKey("gaming_mode_active")
        private val KEY_PERFORMANCE_BOOST_ACTIVE = booleanPreferencesKey("performance_boost_active")
        private val KEY_SAMPLING_INTERVAL_MS = intPreferencesKey("sampling_interval_ms")
        private val KEY_OVERLAY_TAB = stringPreferencesKey("overlay_tab")
        private val KEY_SENSI_REDUCTION_PERCENT = intPreferencesKey("sensi_reduction_percent")
        private val KEY_MARKER_ROTATE_DEG = floatPreferencesKey("marker_rotate_deg")
        private val KEY_FIRE_MACRO_ENABLED = booleanPreferencesKey("fire_macro_enabled")
        private val KEY_FIRE_MACRO_POS_X = intPreferencesKey("fire_macro_pos_x")
        private val KEY_FIRE_MACRO_POS_Y = intPreferencesKey("fire_macro_pos_y")
        private val KEY_FIRE_MACRO_SIZE_DP = intPreferencesKey("fire_macro_size_dp")
        private val KEY_FIRE_MACRO_COLOR = longPreferencesKey("fire_macro_color")
        private val KEY_FIRE_MACRO_BOUNDARY_RADIUS = floatPreferencesKey("fire_macro_boundary_radius")
        private val KEY_FIRE_MACRO_SENS_X = floatPreferencesKey("fire_macro_sens_x")
        private val KEY_FIRE_MACRO_SENS_Y = floatPreferencesKey("fire_macro_sens_y")
        private val KEY_FIRE_MACRO_ALPHA = floatPreferencesKey("fire_macro_alpha")

        // Helper to get package-specific key
        private fun pkgKey(pkg: String, base: String) = "${pkg.replace('.', '_')}_$base"
    }

    val selectedPackageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
    }

    val selectedAppNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_APP_NAME] ?: "Free Fire MAX"
    }

    val overlayScaleFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_OVERLAY_SCALE] ?: 1.0f
    }

    val expandedMenuSizeFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXPANDED_MENU_SIZE] ?: 1.0f
    }

    val overlayAlphaFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_OVERLAY_ALPHA] ?: 0.85f
    }

    val glassTransparencyFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_TRANSPARENCY] ?: 0.85f
    }

    val glassBlurFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_BLUR] ?: "MEDIUM"
    }

    val overlayPositionFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map { prefs ->
        Pair(prefs[KEY_OVERLAY_POS_X] ?: 50, prefs[KEY_OVERLAY_POS_Y] ?: 200)
    }

    val sensitivityLevelFlow: Flow<SensitivityLevel> = context.dataStore.data.map { prefs ->
        try {
            val name = prefs[KEY_SENSITIVITY_LEVEL] ?: SensitivityLevel.HIGH.name
            SensitivityLevel.valueOf(name)
        } catch (_: Exception) {
            SensitivityLevel.HIGH
        }
    }

    val sensitivityPercentFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SENSITIVITY_PERCENT] ?: 150
    }

    val touchOptimizationActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOUCH_OPTIMIZATION_ACTIVE] ?: true
    }

    val ramBoostActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RAM_BOOST_ACTIVE] ?: true
    }

    val gameBoostActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GAME_BOOST_ACTIVE] ?: false
    }

    val gamingModeActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GAMING_MODE_ACTIVE] ?: false
    }

    val performanceBoostActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERFORMANCE_BOOST_ACTIVE] ?: false
    }

    val samplingIntervalMsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAMPLING_INTERVAL_MS] ?: 1000
    }

    val gameProfileFlow: Flow<GameProfile> = context.dataStore.data.map { prefs ->
        val pkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
        val name = prefs[KEY_SELECTED_APP_NAME] ?: "Free Fire MAX"

        // Per-game specific overrides if stored, otherwise global
        val pSensKey = intPreferencesKey(pkgKey(pkg, "sens_percent"))
        val pRamBoostKey = booleanPreferencesKey(pkgKey(pkg, "ram_boost"))
        val pGameBoostKey = booleanPreferencesKey(pkgKey(pkg, "game_boost"))
        val pGamingModeKey = booleanPreferencesKey(pkgKey(pkg, "gaming_mode"))
        val pPerfBoostKey = booleanPreferencesKey(pkgKey(pkg, "perf_boost"))
        val pScaleKey = floatPreferencesKey(pkgKey(pkg, "scale"))
        val pMenuSizeKey = floatPreferencesKey(pkgKey(pkg, "menu_size"))
        val pAlphaKey = floatPreferencesKey(pkgKey(pkg, "alpha"))

        val sensPct = prefs[pSensKey] ?: (prefs[KEY_SENSITIVITY_PERCENT] ?: 150)
        val ramBoost = prefs[pRamBoostKey] ?: (prefs[KEY_RAM_BOOST_ACTIVE] ?: true)
        val gameBoost = prefs[pGameBoostKey] ?: (prefs[KEY_GAME_BOOST_ACTIVE] ?: false)
        val gamingMode = prefs[pGamingModeKey] ?: (prefs[KEY_GAMING_MODE_ACTIVE] ?: false)
        val perfBoost = prefs[pPerfBoostKey] ?: (prefs[KEY_PERFORMANCE_BOOST_ACTIVE] ?: false)
        val scale = prefs[pScaleKey] ?: (prefs[KEY_OVERLAY_SCALE] ?: 1.0f)
        val menuSize = prefs[pMenuSizeKey] ?: (prefs[KEY_EXPANDED_MENU_SIZE] ?: 1.0f)
        val alpha = prefs[pAlphaKey] ?: (prefs[KEY_GLASS_TRANSPARENCY] ?: (prefs[KEY_OVERLAY_ALPHA] ?: 0.85f))
        val blur = prefs[KEY_GLASS_BLUR] ?: "MEDIUM"

        val sensLevel = SensitivityLevel.fromPercent(sensPct)

        GameProfile(
            packageName = pkg,
            appName = name,
            isOverlayEnabled = true,
            showFps = prefs[KEY_SHOW_FPS] ?: true,
            showFrameTime = prefs[KEY_SHOW_FRAME_TIME] ?: true,
            showCpu = prefs[KEY_SHOW_CPU] ?: true,
            showClock = prefs[KEY_SHOW_CLOCK] ?: true,
            showRam = prefs[KEY_SHOW_RAM] ?: true,
            showTemp = prefs[KEY_SHOW_TEMP] ?: true,
            showBattery = prefs[KEY_SHOW_BATTERY] ?: true,
            showDisplayHz = prefs[KEY_SHOW_DISPLAY_HZ] ?: true,
            showGraph = prefs[KEY_SHOW_GRAPH] ?: false,
            showCrosshair = prefs[KEY_SHOW_CROSSHAIR] ?: false,
            crosshairStyle = prefs[KEY_CROSSHAIR_STYLE] ?: "CROSS",
            crosshairColor = prefs[KEY_CROSSHAIR_COLOR] ?: 0xFF00E5FF,
            crosshairSizeDp = prefs[KEY_CROSSHAIR_SIZE] ?: 24,
            crosshairOffsetX = prefs[KEY_CROSSHAIR_OFFSET_X] ?: 0,
            crosshairOffsetY = prefs[KEY_CROSSHAIR_OFFSET_Y] ?: 0,
            sensitivityLevel = sensLevel,
            sensitivityPercent = sensPct,
            isPlus50BoostActive = prefs[KEY_PLUS_50_BOOST_ACTIVE] ?: false,
            isFreeStyle800Active = prefs[KEY_FREE_STYLE_800_ACTIVE] ?: false,
            isDragModeActive = prefs[KEY_DRAG_MODE_ACTIVE] ?: false,
            isUltraProMaxActive = prefs[KEY_ULTRA_PRO_MAX_ACTIVE] ?: false,
            freeStyleDpi = prefs[KEY_FREE_STYLE_DPI] ?: 720,
            isRamBoostActive = ramBoost,
            lastRamFreedMb = prefs[KEY_LAST_RAM_FREED_MB] ?: 0L,
            isGameBoostActive = gameBoost,
            isGamingModeActive = gamingMode,
            isPerformanceBoostActive = perfBoost,
            overlayScale = scale,
            expandedMenuSize = menuSize,
            overlayAlpha = alpha,
            glassTransparency = alpha,
            glassBlur = blur,
            overlayPosX = prefs[KEY_OVERLAY_POS_X] ?: 50,
            overlayPosY = prefs[KEY_OVERLAY_POS_Y] ?: 200,
            overlayTab = prefs[KEY_OVERLAY_TAB] ?: "ABOUT",
            sensiReductionPercent = prefs[KEY_SENSI_REDUCTION_PERCENT] ?: 0,
            markerRotateDeg = prefs[KEY_MARKER_ROTATE_DEG] ?: 0f,
            fireMacroEnabled = prefs[KEY_FIRE_MACRO_ENABLED] ?: false,
            fireMacroPosX = prefs[KEY_FIRE_MACRO_POS_X] ?: 150,
            fireMacroPosY = prefs[KEY_FIRE_MACRO_POS_Y] ?: 350,
            fireMacroSizeDp = prefs[KEY_FIRE_MACRO_SIZE_DP] ?: 70,
            fireMacroColor = prefs[KEY_FIRE_MACRO_COLOR] ?: 0xFFFF1744,
            fireMacroBoundaryRadius = prefs[KEY_FIRE_MACRO_BOUNDARY_RADIUS] ?: 60f,
            fireMacroSensX = prefs[KEY_FIRE_MACRO_SENS_X] ?: 1.0f,
            fireMacroSensY = prefs[KEY_FIRE_MACRO_SENS_Y] ?: 1.0f,
            fireMacroAlpha = prefs[KEY_FIRE_MACRO_ALPHA] ?: 0.85f
        )
    }

    suspend fun saveSelectedGame(packageName: String, appName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PACKAGE] = packageName
            prefs[KEY_SELECTED_APP_NAME] = appName
        }
    }

    suspend fun saveGameProfile(profile: GameProfile) {
        context.dataStore.edit { prefs ->
            val pkg = profile.packageName
            prefs[KEY_SELECTED_PACKAGE] = pkg
            prefs[KEY_SELECTED_APP_NAME] = profile.appName

            // Store per-game values
            prefs[intPreferencesKey(pkgKey(pkg, "sens_percent"))] = profile.sensitivityPercent
            prefs[booleanPreferencesKey(pkgKey(pkg, "ram_boost"))] = profile.isRamBoostActive
            prefs[booleanPreferencesKey(pkgKey(pkg, "game_boost"))] = profile.isGameBoostActive
            prefs[booleanPreferencesKey(pkgKey(pkg, "gaming_mode"))] = profile.isGamingModeActive
            prefs[booleanPreferencesKey(pkgKey(pkg, "perf_boost"))] = profile.isPerformanceBoostActive
            prefs[floatPreferencesKey(pkgKey(pkg, "scale"))] = profile.overlayScale
            prefs[floatPreferencesKey(pkgKey(pkg, "menu_size"))] = profile.expandedMenuSize
            prefs[floatPreferencesKey(pkgKey(pkg, "alpha"))] = profile.glassTransparency

            // Also keep global keys in sync
            prefs[KEY_SENSITIVITY_PERCENT] = profile.sensitivityPercent
            prefs[KEY_SENSITIVITY_LEVEL] = profile.sensitivityLevel.name
            prefs[KEY_RAM_BOOST_ACTIVE] = profile.isRamBoostActive
            prefs[KEY_GAME_BOOST_ACTIVE] = profile.isGameBoostActive
            prefs[KEY_GAMING_MODE_ACTIVE] = profile.isGamingModeActive
            prefs[KEY_PERFORMANCE_BOOST_ACTIVE] = profile.isPerformanceBoostActive
            prefs[KEY_OVERLAY_SCALE] = profile.overlayScale
            prefs[KEY_EXPANDED_MENU_SIZE] = profile.expandedMenuSize
            prefs[KEY_OVERLAY_ALPHA] = profile.overlayAlpha
            prefs[KEY_GLASS_TRANSPARENCY] = profile.glassTransparency
            prefs[KEY_GLASS_BLUR] = profile.glassBlur
            prefs[KEY_SHOW_FPS] = profile.showFps
            prefs[KEY_SHOW_FRAME_TIME] = profile.showFrameTime
            prefs[KEY_SHOW_CPU] = profile.showCpu
            prefs[KEY_SHOW_CLOCK] = profile.showClock
            prefs[KEY_SHOW_RAM] = profile.showRam
            prefs[KEY_SHOW_TEMP] = profile.showTemp
            prefs[KEY_SHOW_BATTERY] = profile.showBattery
            prefs[KEY_SHOW_DISPLAY_HZ] = profile.showDisplayHz
            prefs[KEY_SHOW_GRAPH] = profile.showGraph
            prefs[KEY_SHOW_CROSSHAIR] = profile.showCrosshair
        }
    }

    suspend fun saveOverlayCustomization(scale: Float, alpha: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_SCALE] = scale
            prefs[KEY_OVERLAY_ALPHA] = alpha
            prefs[KEY_GLASS_TRANSPARENCY] = alpha
        }
    }

    suspend fun saveOverlayPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_POS_X] = x
            prefs[KEY_OVERLAY_POS_Y] = y
        }
    }

    suspend fun saveSensitivityLevel(level: SensitivityLevel) {
        val pct = level.targetPercent
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY_LEVEL] = level.name
            prefs[KEY_SENSITIVITY_PERCENT] = pct
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[intPreferencesKey(pkgKey(currentPkg, "sens_percent"))] = pct
        }
    }

    suspend fun saveSensitivityPercent(percent: Int) {
        val level = SensitivityLevel.fromPercent(percent)
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY_PERCENT] = percent.coerceIn(0, 100)
            prefs[KEY_SENSITIVITY_LEVEL] = level.name
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[intPreferencesKey(pkgKey(currentPkg, "sens_percent"))] = percent.coerceIn(0, 100)
        }
    }

    suspend fun setPlus50BoostActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLUS_50_BOOST_ACTIVE] = active
        }
    }

    suspend fun setFreeStyle800Active(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FREE_STYLE_800_ACTIVE] = active
        }
    }

    suspend fun setUltraProMaxActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ULTRA_PRO_MAX_ACTIVE] = active
        }
    }

    suspend fun setDragModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRAG_MODE_ACTIVE] = active
        }
    }

    suspend fun saveLastRamFreedMb(freedMb: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_RAM_FREED_MB] = freedMb
        }
    }

    suspend fun saveFreeStyleDpi(dpi: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FREE_STYLE_DPI] = dpi
        }
    }

    suspend fun setTouchOptimizationActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOUCH_OPTIMIZATION_ACTIVE] = active
        }
    }

    suspend fun setRamBoostActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RAM_BOOST_ACTIVE] = active
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[booleanPreferencesKey(pkgKey(currentPkg, "ram_boost"))] = active
        }
    }

    suspend fun setGameBoostActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GAME_BOOST_ACTIVE] = active
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[booleanPreferencesKey(pkgKey(currentPkg, "game_boost"))] = active
        }
    }

    suspend fun setGamingModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GAMING_MODE_ACTIVE] = active
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[booleanPreferencesKey(pkgKey(currentPkg, "gaming_mode"))] = active
        }
    }

    suspend fun setPerformanceBoostActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERFORMANCE_BOOST_ACTIVE] = active
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[booleanPreferencesKey(pkgKey(currentPkg, "perf_boost"))] = active
        }
    }

    suspend fun updateMetricToggle(metric: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (metric) {
                "fps" -> prefs[KEY_SHOW_FPS] = enabled
                "frame_time" -> prefs[KEY_SHOW_FRAME_TIME] = enabled
                "cpu" -> prefs[KEY_SHOW_CPU] = enabled
                "clock" -> prefs[KEY_SHOW_CLOCK] = enabled
                "ram" -> prefs[KEY_SHOW_RAM] = enabled
                "temp" -> prefs[KEY_SHOW_TEMP] = enabled
                "battery" -> prefs[KEY_SHOW_BATTERY] = enabled
                "display" -> prefs[KEY_SHOW_DISPLAY_HZ] = enabled
                "graph" -> prefs[KEY_SHOW_GRAPH] = enabled
                "crosshair" -> prefs[KEY_SHOW_CROSSHAIR] = enabled
            }
        }
    }

    suspend fun updateCrosshairSettings(style: String, color: Long, sizeDp: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CROSSHAIR_STYLE] = style
            prefs[KEY_CROSSHAIR_COLOR] = color
            prefs[KEY_CROSSHAIR_SIZE] = sizeDp
        }
    }

    suspend fun saveCrosshairOffset(offsetX: Int, offsetY: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CROSSHAIR_OFFSET_X] = offsetX
            prefs[KEY_CROSSHAIR_OFFSET_Y] = offsetY
        }
    }

    suspend fun saveOverlayScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_SCALE] = scale
        }
    }

    suspend fun saveExpandedMenuSize(size: Float) {
        context.dataStore.edit { prefs ->
            val s = size.coerceIn(0.1f, 2.0f)
            prefs[KEY_EXPANDED_MENU_SIZE] = s
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[floatPreferencesKey(pkgKey(currentPkg, "menu_size"))] = s
        }
    }

    suspend fun saveOverlayAlpha(alpha: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_ALPHA] = alpha
            prefs[KEY_GLASS_TRANSPARENCY] = alpha
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[floatPreferencesKey(pkgKey(currentPkg, "alpha"))] = alpha
        }
    }

    suspend fun saveGlassTransparency(transparency: Float) {
        context.dataStore.edit { prefs ->
            val a = transparency.coerceIn(0.0f, 1.0f)
            prefs[KEY_GLASS_TRANSPARENCY] = a
            prefs[KEY_OVERLAY_ALPHA] = a
            val currentPkg = prefs[KEY_SELECTED_PACKAGE] ?: GameProfile.FREE_FIRE_MAX_PACKAGE
            prefs[floatPreferencesKey(pkgKey(currentPkg, "alpha"))] = a
        }
    }

    suspend fun saveGlassBlur(blur: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_BLUR] = blur
        }
    }

    suspend fun saveCrosshairSettings(style: String, color: Long, sizeDp: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CROSSHAIR_STYLE] = style
            prefs[KEY_CROSSHAIR_COLOR] = color
            prefs[KEY_CROSSHAIR_SIZE] = sizeDp
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun setSamplingInterval(ms: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAMPLING_INTERVAL_MS] = ms
        }
    }

    suspend fun saveOverlayTab(tab: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_TAB] = tab
        }
    }

    suspend fun saveSensiReduction(reduction: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSI_REDUCTION_PERCENT] = reduction
        }
    }

    suspend fun saveMarkerRotate(deg: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MARKER_ROTATE_DEG] = deg
        }
    }

    suspend fun saveFireMacroEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_ENABLED] = enabled
        }
    }

    suspend fun saveFireMacroPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_POS_X] = x
            prefs[KEY_FIRE_MACRO_POS_Y] = y
        }
    }

    suspend fun saveFireMacroSize(sizeDp: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_SIZE_DP] = sizeDp
        }
    }

    suspend fun saveFireMacroColor(color: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_COLOR] = color
        }
    }

    suspend fun saveFireMacroBoundaryRadius(radius: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_BOUNDARY_RADIUS] = radius
        }
    }

    suspend fun saveFireMacroSensX(sensX: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_SENS_X] = sensX
        }
    }

    suspend fun saveFireMacroSensY(sensY: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_SENS_Y] = sensY
        }
    }

    suspend fun saveFireMacroAlpha(alpha: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRE_MACRO_ALPHA] = alpha.coerceIn(0.1f, 1.0f)
        }
    }
}
