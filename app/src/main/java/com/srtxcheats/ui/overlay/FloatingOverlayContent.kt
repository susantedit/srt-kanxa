package com.srtxcheats.ui.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.display.DisplayBackupManager
import com.srtxcheats.display.DisplayCommandExecutor
import com.srtxcheats.display.DisplayExecutionResult
import com.srtxcheats.model.DeviceMetrics
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.sensitivity.SensitivityEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun FloatingOverlayContent(
    metrics: DeviceMetrics,
    profile: GameProfile,
    logoBitmap: Bitmap?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onSensitivityChange: (SensitivityLevel) -> Unit = {},
    onSensitivityPercentChange: (Int) -> Unit = {},
    keyDurationText: String = "ACTIVE",
    onToggleRamBoost: () -> Unit = {},
    onToggleGameBoost: () -> Unit = {},
    onToggleMetric: (String) -> Unit = {},
    onToggleGraph: () -> Unit = {},
    onToggleCrosshair: () -> Unit = {},
    onUpdateCrosshairOffset: (Int, Int) -> Unit = { _, _ -> },
    onUpdateMenuSize: (Float) -> Unit = {},
    onUpdateTransparency: (Float) -> Unit = {},
    onSelectTab: (String) -> Unit = {},
    onSensiReductionChange: (Int) -> Unit = {},
    onRestoreSensi: () -> Unit = {},
    onRestoreDisplay: () -> Unit = {},
    onUpdateMarkerStyle: (String) -> Unit = {},
    onUpdateMarkerColor: (Long) -> Unit = {},
    onUpdateMarkerRotate: (Float) -> Unit = {},
    onToggleFireMacro: (Boolean) -> Unit = {},
    onOpenApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val logoScale = profile.overlayScale.coerceIn(0.7f, 1.4f)
    val menuSize = profile.expandedMenuSize.coerceIn(0.4f, 1.8f)
    val alpha = profile.glassTransparency.coerceIn(0.20f, 0.95f)

    Box(modifier = modifier) {
        if (!isExpanded) {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                }
            ) {
                CollapsedOverlayPill(
                    metrics = metrics,
                    logoBitmap = logoBitmap,
                    scale = logoScale,
                    alpha = alpha,
                    onClick = onToggleExpand
                )
            }
        } else {
            LargeLiquidGlassMenu(
                metrics = metrics,
                profile = profile,
                logoBitmap = logoBitmap,
                menuSize = menuSize,
                alpha = alpha,
                keyDurationText = keyDurationText,
                onDragDelta = onDragDelta,
                onCollapse = onToggleExpand,
                onSensitivityChange = onSensitivityChange,
                onSensitivityPercentChange = onSensitivityPercentChange,
                onUpdateCrosshairOffset = onUpdateCrosshairOffset,
                onToggleRamBoost = onToggleRamBoost,
                onToggleGameBoost = onToggleGameBoost,
                onToggleMetric = onToggleMetric,
                onToggleGraph = onToggleGraph,
                onToggleCrosshair = onToggleCrosshair,
                onUpdateMenuSize = onUpdateMenuSize,
                onUpdateTransparency = onUpdateTransparency,
                onSelectTab = onSelectTab,
                onSensiReductionChange = onSensiReductionChange,
                onRestoreSensi = onRestoreSensi,
                onRestoreDisplay = onRestoreDisplay,
                onUpdateMarkerStyle = onUpdateMarkerStyle,
                onUpdateMarkerColor = onUpdateMarkerColor,
                onUpdateMarkerRotate = onUpdateMarkerRotate,
                onToggleFireMacro = onToggleFireMacro,
                onOpenApp = onOpenApp
            )
        }
    }
}

@Composable
fun CollapsedOverlayPill(
    metrics: DeviceMetrics,
    logoBitmap: Bitmap?,
    scale: Float,
    alpha: Float,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(24.dp)
    val fps = metrics.fps ?: 60
    val fpsColor = when {
        fps >= 55 -> Color(0xFF00E676)
        fps >= 30 -> Color(0xFFFFD600)
        else -> Color(0xFFFF1744)
    }

    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(pillShape)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF00E5FF), Color(0x3300E5FF), Color(0x15FFFFFF))
                ),
                shape = pillShape
            ),
        shape = pillShape,
        color = Color(0xDD0A0F1D).copy(alpha = alpha),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = (10 * scale).dp.coerceIn(8.dp, 16.dp),
                vertical = (6 * scale).dp.coerceIn(4.dp, 10.dp)
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)
        ) {
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap.asImageBitmap(),
                    contentDescription = "TEAM_SRT Logo",
                    modifier = Modifier
                        .size((22 * scale).dp.coerceIn(16.dp, 32.dp))
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size((20 * scale).dp.coerceIn(16.dp, 28.dp))
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }

            Text(
                text = "$fps",
                color = fpsColor,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = (13 * scale).coerceIn(11f, 17f).sp
            )

            Text(
                text = "FPS",
                color = Color(0xFF78909C),
                fontWeight = FontWeight.Bold,
                fontSize = (9 * scale).coerceIn(8f, 12f).sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LargeLiquidGlassMenu(
    metrics: DeviceMetrics,
    profile: GameProfile,
    logoBitmap: Bitmap?,
    menuSize: Float = 1.0f,
    alpha: Float = 0.95f,
    keyDurationText: String = "ACTIVE",
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onCollapse: () -> Unit = {},
    onSensitivityChange: (SensitivityLevel) -> Unit = {},
    onSensitivityPercentChange: (Int) -> Unit = {},
    onUpdateCrosshairOffset: (Int, Int) -> Unit = { _, _ -> },
    onToggleRamBoost: () -> Unit = {},
    onToggleGameBoost: () -> Unit = {},
    onToggleMetric: (String) -> Unit = {},
    onToggleGraph: () -> Unit = {},
    onToggleCrosshair: () -> Unit = {},
    onUpdateMenuSize: (Float) -> Unit = {},
    onUpdateTransparency: (Float) -> Unit = {},
    onSelectTab: (String) -> Unit = {},
    onSensiReductionChange: (Int) -> Unit = {},
    onRestoreSensi: () -> Unit = {},
    onRestoreDisplay: () -> Unit = {},
    onUpdateMarkerStyle: (String) -> Unit = {},
    onUpdateMarkerColor: (Long) -> Unit = {},
    onUpdateMarkerRotate: (Float) -> Unit = {},
    onToggleFireMacro: (Boolean) -> Unit = {},
    onOpenApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val displayBackupManager = remember { DisplayBackupManager(context) }
    val sensitivityEngine = remember { SensitivityEngine(context) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSmallDpiPhone = screenWidth < 360.dp

    val baseWidth = if (isSmallDpiPhone) 270.dp else 305.dp
    val maxAllowedWidth = if (isLandscape) (screenWidth * 0.65f).coerceIn(250.dp, 400.dp) else (screenWidth * 0.92f).coerceIn(230.dp, 370.dp)
    val panelWidth = (baseWidth * menuSize).coerceIn(220.dp, maxAllowedWidth)
    val maxScrollHeight = if (isLandscape) (screenHeight * 0.78f) else (screenHeight * 0.70f)

    val panelShape = RoundedCornerShape(18.dp)
    var activeTab by remember(profile.overlayTab) { mutableStateOf(profile.overlayTab) }

    // Live clock ticker
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(1000L)
        }
    }

    Surface(
        modifier = Modifier
            .width(panelWidth)
            .clip(panelShape)
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xCC00E5FF),
                        Color(0x3300E5FF),
                        Color(0x15FFFFFF),
                        Color(0x6600E5FF)
                    )
                ),
                shape = panelShape
            ),
        shape = panelShape,
        color = Color(0xEE060912).copy(alpha = alpha),
        tonalElevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ==========================================
            // 1. TOP HEADER & DRAG HANDLE: (LOGO) TEAM_SRT
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .background(Color(0x2200E5FF))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "TEAM_SRT Logo",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "TEAM_SRT",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isSmallDpiPhone) 12.sp else 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag Window",
                        tint = Color(0x8800E5FF),
                        modifier = Modifier.size(18.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenApp,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Open App",
                                tint = Color(0xFF80D8FF),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandLess,
                                contentDescription = "Minimize",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. TAB NAVIGATION: ABOUT | SENSI | STRETCH | MARKER
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x18000000))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    "ABOUT" to "About",
                    "SENSI" to "Sensi",
                    "STRETCH" to "Stretch",
                    "MARKER" to "Marker"
                )

                tabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = activeTab.equals(tabKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF00E5FF) else Color(0x201E293B))
                            .border(0.6.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                            .clickable {
                                activeTab = tabKey
                                onSelectTab(tabKey)
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // 3. SCROLLABLE TAB CONTENT
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxScrollHeight)
                    .padding(horizontal = if (isSmallDpiPhone) 10.dp else 12.dp, vertical = 6.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (activeTab.uppercase()) {
                    "ABOUT" -> {
                        // ----------------------------------------------------
                        // ABOUT SECTION: SYSTEM MONITORS & FPS GRAPH
                        // ----------------------------------------------------
                        // Optional Rolling FPS Graph on top/left
                        if (profile.showGraph && metrics.rollingFps.isNotEmpty()) {
                            RollingPerformanceGraph(
                                fpsData = metrics.rollingFps,
                                frameTimeData = metrics.rollingFrameTime,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Telemetry toggles and live readouts
                        Text(
                            text = "SYSTEM MONITORS",
                            color = Color(0xFF00E5FF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val fpsVal = metrics.fps ?: 60
                        val hzVal = metrics.displayHz
                        val cpuVal = metrics.cpuUsageDisplay
                        val clockVal = if (currentTimeString.isNotEmpty()) currentTimeString else "00:00:00"
                        val batVal = "${metrics.batteryPercent}%"
                        val tempVal = metrics.batteryTempDisplay

                        MonitorToggleRow(
                            label = "FPS %",
                            currentValue = "$fpsVal.00%",
                            valueColor = if (fpsVal >= 55) Color(0xFF00E676) else Color(0xFFFFD600),
                            checked = profile.showFps,
                            onToggle = { onToggleMetric("fps") }
                        )

                        MonitorToggleRow(
                            label = "Refresh HZ",
                            currentValue = "$hzVal Hz",
                            valueColor = Color(0xFF00E5FF),
                            checked = profile.showDisplayHz,
                            onToggle = { onToggleMetric("display") }
                        )

                        MonitorToggleRow(
                            label = "CPU",
                            currentValue = cpuVal,
                            valueColor = Color(0xFF80D8FF),
                            checked = profile.showCpu,
                            onToggle = { onToggleMetric("cpu") }
                        )

                        MonitorToggleRow(
                            label = "Clock",
                            currentValue = clockVal,
                            valueColor = Color.White,
                            checked = profile.showClock,
                            onToggle = { onToggleMetric("clock") }
                        )

                        MonitorToggleRow(
                            label = "Battery %",
                            currentValue = batVal,
                            valueColor = Color(0xFFB388FF),
                            checked = profile.showBattery,
                            onToggle = { onToggleMetric("battery") }
                        )

                        MonitorToggleRow(
                            label = "Battery Temp",
                            currentValue = tempVal,
                            valueColor = if ((metrics.batteryTempC ?: 0f) > 42f) Color(0xFFFF1744) else Color(0xFFFFD180),
                            checked = profile.showTemp,
                            onToggle = { onToggleMetric("temp") }
                        )

                        MonitorToggleRow(
                            label = "FPS Graph",
                            currentValue = if (profile.showGraph) "ACTIVE" else "OFF",
                            valueColor = if (profile.showGraph) Color(0xFF00E676) else Color(0xFF78909C),
                            checked = profile.showGraph,
                            onToggle = onToggleGraph
                        )
                    }

                    "SENSI" -> {
                        // ----------------------------------------------------
                        // SENSI SECTION: -0% to -100% SLIDER & RESTORE
                        // ----------------------------------------------------
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.8.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp)),
                            color = Color(0x300A1120),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SENSI STEPS",
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${profile.sensiReductionPercent}%",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Exact 5 Steps: -0%, -25%, -50%, -75%, -100%
                                val steps = listOf(0, -25, -50, -75, -100)
                                val currentStepIndex = steps.indexOf(profile.sensiReductionPercent).let { if (it == -1) 0 else it }

                                Slider(
                                    value = currentStepIndex.toFloat(),
                                    onValueChange = {
                                        val idx = it.toInt().coerceIn(0, 4)
                                        val selectedPercent = steps[idx]
                                        onSensiReductionChange(selectedPercent)
                                    },
                                    valueRange = 0f..4f,
                                    steps = 3,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00E5FF),
                                        activeTrackColor = Color(0xFF00E5FF),
                                        inactiveTrackColor = Color(0x3300E5FF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    steps.forEach { stepVal ->
                                        val isSel = profile.sensiReductionPercent == stepVal
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) Color(0xFF00E5FF) else Color(0x221E293B))
                                                .clickable { onSensiReductionChange(stepVal) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$stepVal%",
                                                color = if (isSel) Color.Black else Color(0xFF90A4AE),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Restore Sensi Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x33FF5252))
                                        .border(0.8.dp, Color(0xFFFF5252), RoundedCornerShape(6.dp))
                                        .clickable {
                                            onSensiReductionChange(0)
                                            onRestoreSensi()
                                            coroutineScope.launch {
                                                val res = sensitivityEngine.restoreOriginal()
                                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                                        Text(
                                            text = "RESTORE SENSI",
                                            color = Color(0xFFFF8A80),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Device Level Multiplier Presets
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.6.dp, Color(0x2200E5FF), RoundedCornerShape(8.dp)),
                            color = Color(0x25060D1A)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("SYSTEM MULTIPLIER PRESETS", color = Color(0xFF90A4AE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    SensitivityLevel.values().forEach { lvl ->
                                        val isSel = profile.sensitivityPercent == lvl.targetPercent
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) Color(0xFF00E5FF) else Color(0x221E293B))
                                                .clickable {
                                                    onSensitivityChange(lvl)
                                                    onSensitivityPercentChange(lvl.targetPercent)
                                                }
                                                .padding(vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lvl.displayName,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "STRETCH" -> {
                        // ----------------------------------------------------
                        // STRETCH SECTION: RESTORE DISPLAY & INFO
                        // ----------------------------------------------------
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.8.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp)),
                            color = Color(0x300A1120),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "DISPLAY RESOLUTION & STRETCH",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Engine Status:", color = Color(0xFF78909C), fontSize = 8.5.sp)
                                    Text(
                                        text = if (ShizukuManager.isAuthorized()) "Shizuku Active ⚡" else "No Shizuku",
                                        color = if (ShizukuManager.isAuthorized()) Color(0xFF00E676) else Color(0xFFFF9100),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Restore Display Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x33FF1744))
                                        .border(0.8.dp, Color(0xFFFF1744), RoundedCornerShape(6.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                val backup = displayBackupManager.getBackup()
                                                val res = DisplayCommandExecutor.restoreOriginal(backup)
                                                when (res) {
                                                    is DisplayExecutionResult.Success -> {
                                                        Toast.makeText(context, "Native Display Restored", Toast.LENGTH_SHORT).show()
                                                        onRestoreDisplay()
                                                    }
                                                    is DisplayExecutionResult.Failure -> {
                                                        Toast.makeText(context, res.error, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF1744), modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "RESTORE DISPLAY",
                                            color = Color(0xFFFF5252),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2200E5FF))
                                        .border(0.6.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                        .clickable { onOpenApp() }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "OPEN STRETCH CONTROLS",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    "MARKER" -> {
                        // ----------------------------------------------------
                        // MARKER SECTION: ON/OFF, POSITION X/Y, LOGOS, COLORS, ROTATE
                        // ----------------------------------------------------
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.8.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp)),
                            color = Color(0x300A1120),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Master Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "MARKER ON / OFF",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Switch(
                                        checked = profile.showCrosshair,
                                        onCheckedChange = { onToggleCrosshair() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color(0xFF00E5FF)
                                        ),
                                        modifier = Modifier.size(34.dp, 20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Position X/Y Dynamic Coordinates Display & Nudge
                                Text(
                                    text = "POSITION X: ${profile.crosshairOffsetX} | Y: ${profile.crosshairOffsetY}",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("X:", color = Color(0xFF78909C), fontSize = 8.sp, modifier = Modifier.width(14.dp))
                                    listOf(-10, -1, 1, 10).forEach { delta ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x221E293B))
                                                .clickable {
                                                    onUpdateCrosshairOffset(
                                                        (profile.crosshairOffsetX + delta).coerceIn(-500, 500),
                                                        profile.crosshairOffsetY
                                                    )
                                                }
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (delta > 0) "+$delta" else "$delta", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("Y:", color = Color(0xFF78909C), fontSize = 8.sp, modifier = Modifier.width(14.dp))
                                    listOf(-10, -1, 1, 10).forEach { delta ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x221E293B))
                                                .clickable {
                                                    onUpdateCrosshairOffset(
                                                        profile.crosshairOffsetX,
                                                        (profile.crosshairOffsetY + delta).coerceIn(-500, 500)
                                                    )
                                                }
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (delta > 0) "+$delta" else "$delta", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Marker Rotate Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("MARKER ROTATE", color = Color(0xFFB0BEC5), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${profile.markerRotateDeg.toInt()}°", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = profile.markerRotateDeg,
                                    onValueChange = onUpdateMarkerRotate,
                                    valueRange = 0f..360f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)),
                                    modifier = Modifier.fillMaxWidth().height(26.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Marker Logo Selector (20 Types)
                                Text("MARKER LOGO (20 TYPES)", color = Color(0xFFB0BEC5), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MarkerConstants.MARKER_STYLES.forEach { (styleKey, styleName) ->
                                        val isSel = profile.crosshairStyle == styleKey
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) Color(0xFF00E5FF) else Color(0x221E293B))
                                                .border(0.5.dp, if (isSel) Color(0xFF00E5FF) else Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                                                .clickable { onUpdateMarkerStyle(styleKey) }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = styleName,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Marker Color Selector (20 Colors)
                                Text("MARKER COLOR (20 COLORS)", color = Color(0xFFB0BEC5), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MarkerConstants.MARKER_COLORS.forEach { (colorVal, _) ->
                                        val isSel = profile.crosshairColor == colorVal
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorVal))
                                                .border(
                                                    width = if (isSel) 2.dp else 0.5.dp,
                                                    color = if (isSel) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { onUpdateMarkerColor(colorVal) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ==========================================
                // 4. FIRE MACRO SENSI FLOATING OVERLAY TOGGLE
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.7.dp, if (profile.fireMacroEnabled) Color(0xFFFF1744) else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onToggleFireMacro(!profile.fireMacroEnabled) },
                    color = if (profile.fireMacroEnabled) Color(0x22FF1744) else Color(0x180A101C)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = if (profile.fireMacroEnabled) Color(0xFFFF1744) else Color(0xFF78909C), modifier = Modifier.size(14.dp))
                            Column {
                                Text("FIRE MACRO SENSI OVERLAY", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (profile.fireMacroEnabled) "Floating Target Active" else "Tap to Spawn Window",
                                    color = if (profile.fireMacroEnabled) Color(0xFFFF5252) else Color(0xFF78909C),
                                    fontSize = 7.5.sp
                                )
                            }
                        }
                        Switch(
                            checked = profile.fireMacroEnabled,
                            onCheckedChange = { onToggleFireMacro(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFFFF1744)
                            ),
                            modifier = Modifier.size(32.dp, 18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ==========================================
                // 5. MENU TRANSPARENCY & MENU SIZE SLIDERS
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MENU SIZE: ${(profile.expandedMenuSize * 100).toInt()}%",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.expandedMenuSize,
                            onValueChange = { onUpdateMenuSize(it) },
                            valueRange = 0.4f..1.6f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)),
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TRANSPARENCY: ${(profile.glassTransparency * 100).toInt()}%",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.glassTransparency,
                            onValueChange = { onUpdateTransparency(it) },
                            valueRange = 0.20f..0.95f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 6. IPHONE-STYLE GESTURE SYSTEM (Home Bar)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(Color(0x18000000))
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        var totalDragY = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDragEnd = {
                                val gestureThreshold = 22f
                                if (abs(totalDragY) > gestureThreshold || abs(totalDragX) > gestureThreshold) {
                                    onCollapse()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                            }
                        )
                    }
                    .clickable { onCollapse() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xAAFFFFFF))
                )
            }
        }
    }
}

@Composable
fun MonitorToggleRow(
    label: String,
    currentValue: String,
    valueColor: Color,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = Color(0x22101827)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    color = Color(0xFFB0BEC5),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "-",
                    color = Color(0xFF546E7A),
                    fontSize = 8.sp
                )
                Text(
                    text = currentValue,
                    color = valueColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFF00E5FF),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0x22FFFFFF)
                ),
                modifier = Modifier.size(30.dp, 16.dp)
            )
        }
    }
}

@Composable
fun RollingPerformanceGraph(
    fpsData: List<Float>,
    frameTimeData: List<Float>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(0.6.dp, Color(0x2200E5FF), RoundedCornerShape(8.dp)),
        color = Color(0x40050811),
        shape = RoundedCornerShape(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val width = size.width
            val height = size.height
            if (fpsData.size < 2) return@Canvas

            val targetY = height * 0.4f
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                strokeWidth = 1f
            )

            val stepX = width / (fpsData.size - 1).coerceAtLeast(1)
            val maxFps = 120f
            val path = Path()

            fpsData.forEachIndexed { index, fps ->
                val x = index * stepX
                val normalizedY = (1f - (fps.coerceIn(0f, maxFps) / maxFps)) * height
                if (index == 0) {
                    path.moveTo(x, normalizedY)
                } else {
                    path.lineTo(x, normalizedY)
                }
            }

            drawPath(
                path = path,
                color = Color(0xFF00E5FF),
                style = Stroke(width = 2f)
            )
        }
    }
}
