package com.srtxcheats.ui.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.display.DisplayBackupManager
import com.srtxcheats.display.DisplayCommandExecutor
import com.srtxcheats.display.DisplayExecutionResult
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.model.DeviceMetrics
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.sensitivity.SensitivityEngine
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun FloatingOverlayContent(
    metrics: DeviceMetrics,
    profile: GameProfile,
    logoBitmap: Bitmap?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onSensitivityChange: (SensitivityLevel) -> Unit,
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
    onOpenApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val logoScale = profile.overlayScale.coerceIn(0.7f, 1.4f)
    val menuSize = profile.expandedMenuSize.coerceIn(0.4f, 1.8f)
    val alpha = profile.glassTransparency.coerceIn(0.20f, 0.95f)

    Box(modifier = modifier) {
        if (!isExpanded) {
            // Collapsed Pill View: Compact, draggable anywhere
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
            // Expanded Menu View: Draggable via Top Drag Bar, gesture collapse via iPhone Home Bar
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
                onOpenApp = onOpenApp
            )
        }
    }
}

/**
 * Collapsed Overlay Pill:
 * Compact, lightweight: [ LOGO ] 60 FPS
 */
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
                    contentDescription = "SRT Logo",
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

/**
 * Large Liquid Glass Expanded Menu:
 * Fully responsive for 320-360 DPI devices, non-blocking iPhone-style gesture system.
 */
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
    onOpenApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val displayBackupManager = remember { DisplayBackupManager(context) }
    val sensitivityEngine = remember { SensitivityEngine(context) }

    // Dynamic Responsive Sizing for 320-360 DPI screens
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSmallDpiPhone = screenWidth < 360.dp

    val baseWidth = if (isSmallDpiPhone) 260.dp else 290.dp
    val maxAllowedWidth = if (isLandscape) (screenWidth * 0.60f).coerceIn(240.dp, 380.dp) else (screenWidth * 0.90f).coerceIn(220.dp, 360.dp)
    val panelWidth = (baseWidth * menuSize).coerceIn(210.dp, maxAllowedWidth)
    val maxScrollHeight = if (isLandscape) (screenHeight * 0.82f) else (screenHeight * 0.72f)

    val panelShape = RoundedCornerShape(18.dp)

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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ==========================================
            // 1. TOP DRAG HANDLE & HEADER (Window Move Area)
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
                    .background(Color(0x2000E5FF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
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
                                contentDescription = "SRT Logo",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Text(
                            text = "SRT X CHEATS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isSmallDpiPhone) 12.sp else 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Drag Indicator in center
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag Window",
                        tint = Color(0x8800E5FF),
                        modifier = Modifier.size(18.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenApp,
                            modifier = Modifier.size(26.dp)
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
                            modifier = Modifier.size(26.dp)
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
            // 2. SCROLLABLE INNER MENU CONTENT
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxScrollHeight)
                    .padding(horizontal = if (isSmallDpiPhone) 10.dp else 12.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Key Expiry Duration Badge
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x2200E5FF),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0x4400E5FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(13.dp))
                            Text("LICENSE STATUS", color = Color(0xFF90A4AE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = keyDurationText,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Real-time Telemetry Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (profile.showFps) {
                        LiquidMetricCard(
                            label = "FPS",
                            value = metrics.fps?.toString() ?: "60",
                            valueColor = if ((metrics.fps ?: 0) >= 55) Color(0xFF00E676) else Color(0xFFFF9100),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.showCpu) {
                        LiquidMetricCard(
                            label = "CPU",
                            value = metrics.cpuUsageDisplay,
                            valueColor = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.showRam) {
                        LiquidMetricCard(
                            label = "RAM",
                            value = "${String.format("%.1f", metrics.ramUsedGb)}G",
                            valueColor = Color(0xFFB388FF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.showTemp) {
                        LiquidMetricCard(
                            label = "TEMP",
                            value = metrics.batteryTempDisplay,
                            valueColor = if ((metrics.batteryTempC ?: 0f) > 42f) Color(0xFFFF1744) else Color(0xFFFFD180),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // REAL DEVICE SENSITIVITY BOOST SLIDER
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.8.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp)),
                    color = Color(0x350C1220),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SENSITIVITY BOOST",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${profile.sensitivityLevel.displayName} (${profile.sensitivityLevel.multiplierLabel})",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "${profile.sensitivityPercent}%",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Master Slider (0% - 100%)
                        Slider(
                            value = profile.sensitivityPercent.toFloat(),
                            onValueChange = { onSensitivityPercentChange(it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color(0x3300E5FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4 Presets: LOW (0%), MEDIUM (50%), HIGH (75%), ULTRA HIGH (100%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SensitivityLevel.values().forEach { lvl ->
                                val isSel = profile.sensitivityPercent == lvl.targetPercent
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF00E5FF) else Color(0x221E293B))
                                        .border(0.5.dp, if (isSel) Color(0xFF00E5FF) else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                        .clickable {
                                            onSensitivityChange(lvl)
                                            onSensitivityPercentChange(lvl.targetPercent)
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = lvl.displayName,
                                            color = if (isSel) Color.Black else Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = lvl.multiplierLabel,
                                            color = if (isSel) Color.Black else Color(0xFF90A4AE),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Apply Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00E5FF))
                                    .clickable {
                                        coroutineScope.launch {
                                            val res = sensitivityEngine.applySensitivity(profile.sensitivityPercent)
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "APPLY SENSITIVITY",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FF5252))
                                    .border(0.5.dp, Color(0xFFFF5252), RoundedCornerShape(6.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            val res = sensitivityEngine.restoreOriginal()
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "RESTORE",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ==========================================
                // PRESERVED: STRETCH SCREEN & DISPLAY RECOVERY
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.8.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp)),
                    color = Color(0x330A101D),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("STRETCH & DISPLAY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (ShizukuManager.isAuthorized()) "Shizuku Ready" else "No Shizuku",
                                color = if (ShizukuManager.isAuthorized()) Color(0xFF00E676) else Color(0xFFFF9100),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FF1744))
                                    .border(0.6.dp, Color(0xFFFF1744), RoundedCornerShape(6.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            val backup = displayBackupManager.getBackup()
                                            val res = DisplayCommandExecutor.restoreOriginal(backup)
                                            when (res) {
                                                is DisplayExecutionResult.Success -> Toast.makeText(context, "Original Display Restored", Toast.LENGTH_SHORT).show()
                                                is DisplayExecutionResult.Failure -> Toast.makeText(context, res.error, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("RESET DISPLAY", color = Color(0xFFFF5252), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3300E5FF))
                                    .border(0.6.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                    .clickable { onOpenApp() }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("STRETCH PANEL", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // RAM & Game Boost Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LiquidActionBadge(
                        title = "RAM CLEANER",
                        status = if (profile.lastRamFreedMb > 0) "+${profile.lastRamFreedMb}MB ⚡" else "CLEAN",
                        isActive = profile.isRamBoostActive,
                        activeColor = Color(0xFF00E676),
                        modifier = Modifier.weight(1f),
                        onClick = onToggleRamBoost
                    )

                    LiquidActionBadge(
                        title = "GAME BOOST",
                        status = if (profile.isGameBoostActive) "ACTIVE" else "IDLE",
                        isActive = profile.isGameBoostActive,
                        activeColor = Color(0xFFFF9100),
                        modifier = Modifier.weight(1f),
                        onClick = onToggleGameBoost
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Metric Toggle Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricToggleChip(label = "FPS", active = profile.showFps) { onToggleMetric("fps") }
                    MetricToggleChip(label = "CPU", active = profile.showCpu) { onToggleMetric("cpu") }
                    MetricToggleChip(label = "RAM", active = profile.showRam) { onToggleMetric("ram") }
                    MetricToggleChip(label = "TEMP", active = profile.showTemp) { onToggleMetric("temp") }
                    MetricToggleChip(label = "GRAPH", active = profile.showGraph) { onToggleGraph() }
                    MetricToggleChip(label = "MARKER", active = profile.showCrosshair) { onToggleCrosshair() }
                }

                // Crosshair Offset Adjustments
                if (profile.showCrosshair) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .border(0.6.dp, Color(0x4400E5FF), RoundedCornerShape(6.dp)),
                        color = Color(0x25060D1A)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("MARKER OFFSET (PX)", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("X: ${profile.crosshairOffsetX}", color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(42.dp))
                                listOf(-10, -1, 1, 10).forEach { delta ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x221E293B))
                                            .clickable {
                                                onUpdateCrosshairOffset(
                                                    (profile.crosshairOffsetX + delta).coerceIn(-400, 400),
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("Y: ${profile.crosshairOffsetY}", color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(42.dp))
                                listOf(-10, -1, 1, 10).forEach { delta ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x221E293B))
                                            .clickable {
                                                onUpdateCrosshairOffset(
                                                    profile.crosshairOffsetX,
                                                    (profile.crosshairOffsetY + delta).coerceIn(-400, 400)
                                                )
                                            }
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (delta > 0) "+$delta" else "$delta", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Rolling FPS Graph
                if (profile.showGraph && metrics.rollingFps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    RollingPerformanceGraph(
                        fpsData = metrics.rollingFps,
                        frameTimeData = metrics.rollingFrameTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Menu Size & Transparency Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SIZE: ${(profile.expandedMenuSize * 100).toInt()}%",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.expandedMenuSize,
                            onValueChange = { onUpdateMenuSize(it) },
                            valueRange = 0.4f..1.6f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ALPHA: ${(profile.glassTransparency * 100).toInt()}%",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.glassTransparency,
                            onValueChange = { onUpdateTransparency(it) },
                            valueRange = 0.2f..0.95f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                        )
                    }
                }
            }

            // ==========================================
            // 3. IPHONE-STYLE GESTURE SYSTEM (Home Bar)
            // ==========================================
            // Dedicated bottom gesture bar with proper Android gesture recognition:
            // Swipe UP / DOWN / LEFT / RIGHT or TAP cleanly collapses or minimizes overlay.
            // Never blocks or conflicts with button clicks, scroll views, or sliders above.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
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
                                    // Valid swipe gesture detected (Swipe up, down, left, right)
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
                // iPhone-style visual Home Indicator Capsule
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xAAFFFFFF))
                )
            }
        }
    }
}

@Composable
fun LiquidMetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(0.6.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
        color = Color(0x350A101C),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color(0xFF78909C),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun LiquidActionBadge(
    title: String,
    status: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 0.8.dp,
                color = if (isActive) activeColor else Color(0x22FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        color = if (isActive) activeColor.copy(alpha = 0.15f) else Color(0x220A101C),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = Color(0xFFB0BEC5),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    color = if (isActive) activeColor else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else Color(0xFF546E7A))
            )
        }
    }
}

@Composable
fun MetricToggleChip(
    label: String,
    active: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Color(0x3300E5FF) else Color(0x221E293B))
            .border(0.5.dp, if (active) Color(0xFF00E5FF) else Color(0x22FFFFFF), RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (active) Color(0xFF00E5FF) else Color(0xFF90A4AE),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
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

            // Baseline 60 FPS line
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
