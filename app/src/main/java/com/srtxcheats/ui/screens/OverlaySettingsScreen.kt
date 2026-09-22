package com.srtxcheats.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.model.DeviceMetrics
import com.srtxcheats.model.GameProfile
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.overlay.CollapsedOverlayPill
import com.srtxcheats.ui.overlay.LargeLiquidGlassMenu
import com.srtxcheats.ui.overlay.ScreenMarkerOverlay
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingCrimson
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverlaySettingsScreen(
    profile: GameProfile,
    logoBitmap: Bitmap?,
    onUpdateScale: (Float) -> Unit,
    onUpdateMenuSize: (Float) -> Unit = {},
    onUpdateAlpha: (Float) -> Unit,
    onUpdateTransparency: (Float) -> Unit = {},
    onUpdateGlassBlur: (String) -> Unit = {},
    onSensitivityPercentChange: (Int) -> Unit = {},
    onToggleMetric: (String, Boolean) -> Unit,
    onToggleCrosshair: (Boolean) -> Unit,
    onUpdateCrosshairSettings: (String, Long, Int) -> Unit,
    onUpdateCrosshairOffset: (Int, Int) -> Unit = { _, _ -> },
    onToggleGraph: (Boolean) -> Unit
) {
    var previewExpanded by remember { mutableStateOf(false) }

    val mockMetrics = remember {
        DeviceMetrics(
            fps = 60,
            frameTimeMs = 16.7f,
            displayHz = 120,
            cpuUsagePercent = 34,
            cpuClockGhz = 2.40f,
            ramUsedGb = 4.7f,
            ramTotalGb = 8.0f,
            batteryPercent = 82,
            isCharging = false,
            batteryTempC = 37.0f,
            rollingFps = listOf(58f, 59f, 60f, 60f, 59f, 60f, 60f, 60f, 58f, 60f),
            rollingFrameTime = listOf(17.2f, 16.9f, 16.6f, 16.6f, 16.9f, 16.6f, 16.6f, 16.6f, 17.2f, 16.6f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("overlay_settings_screen")
    ) {
        Text(
            text = "OVERLAY HUD CONFIGURATION",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = "Configure overlay appearance, marker position, and gaming telemetry",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Interactive Preview Box
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowAccent = true
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE INTERACTIVE PREVIEW",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (previewExpanded) "Tap collapse icon inside" else "Tap pill to open large menu",
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (previewExpanded) 340.dp else 80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF030508))
                    .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!previewExpanded) {
                    CollapsedOverlayPill(
                        metrics = mockMetrics,
                        logoBitmap = logoBitmap,
                        scale = profile.overlayScale,
                        alpha = profile.glassTransparency,
                        onClick = { previewExpanded = true }
                    )
                } else {
                    LargeLiquidGlassMenu(
                        metrics = mockMetrics,
                        profile = profile,
                        logoBitmap = logoBitmap,
                        menuSize = profile.expandedMenuSize,
                        alpha = profile.glassTransparency,
                        onCollapse = { previewExpanded = false },
                        onSensitivityChange = {},
                        onSensitivityPercentChange = onSensitivityPercentChange,
                        onToggleRamBoost = {},
                        onToggleGameBoost = {},
                        onToggleMetric = { key ->
                            val cur = when (key) {
                                "fps" -> profile.showFps
                                "frame_time" -> profile.showFrameTime
                                "cpu" -> profile.showCpu
                                "clock" -> profile.showClock
                                "ram" -> profile.showRam
                                "temp" -> profile.showTemp
                                "battery" -> profile.showBattery
                                else -> true
                            }
                            onToggleMetric(key, !cur)
                        },
                        onToggleGraph = { onToggleGraph(!profile.showGraph) },
                        onToggleCrosshair = { onToggleCrosshair(!profile.showCrosshair) },
                        onUpdateMenuSize = onUpdateMenuSize,
                        onUpdateTransparency = onUpdateTransparency,
                        onOpenApp = {}
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Independent Size Controls: Logo Size vs Expanded Menu Size
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "EXPANDED MENU SIZE (${(profile.expandedMenuSize * 100).toInt()}%)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Controls the size of the expanded panel independently from the logo",
                color = TextMuted,
                fontSize = 10.sp
            )
            Slider(
                value = profile.expandedMenuSize,
                onValueChange = { onUpdateMenuSize(it) },
                valueRange = 0.4f..1.8f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0x331E293B)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "COLLAPSED LOGO SCALE (${(profile.overlayScale * 100).toInt()}%)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Keeps the collapsed overlay badge small and unobtrusive while gaming",
                color = TextMuted,
                fontSize = 10.sp
            )
            Slider(
                value = profile.overlayScale,
                onValueChange = { onUpdateScale(it) },
                valueRange = 0.8f..1.2f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF80D8FF),
                    activeTrackColor = Color(0xFF80D8FF),
                    inactiveTrackColor = Color(0x331E293B)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Glass Transparency & Dynamic Blur
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "HUD SURFACE TRANSPARENCY (${(profile.glassTransparency * 100).toInt()}%)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Frosted glass opacity allowing full visibility of game underneath",
                color = TextMuted,
                fontSize = 10.sp
            )
            Slider(
                value = profile.glassTransparency,
                onValueChange = {
                    onUpdateTransparency(it)
                    onUpdateAlpha(it)
                },
                valueRange = 0.2f..0.95f,
                colors = SliderDefaults.colors(
                    thumbColor = GamingAmber,
                    activeTrackColor = GamingAmber,
                    inactiveTrackColor = Color(0x331E293B)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DYNAMIC GLASS BLUR",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Automatically switches to lightweight translucent glass on low-power devices",
                color = TextMuted,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("OFF", "LOW", "MEDIUM", "HIGH").forEach { mode ->
                    val isSel = profile.glassBlur == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonCyan else Color(0x221E293B))
                            .border(0.8.dp, if (isSel) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { onUpdateGlassBlur(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visible Telemetry Metric Toggles (including Frame Time)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ACTIVE TELEMETRY METRICS",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toggle metrics displayed on the expanded panel",
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricFilterChip("FPS", profile.showFps) { onToggleMetric("fps", it) }
                MetricFilterChip("Frame Time (ms)", profile.showFrameTime) { onToggleMetric("frame_time", it) }
                MetricFilterChip("CPU Usage", profile.showCpu) { onToggleMetric("cpu", it) }
                MetricFilterChip("CPU Clock", profile.showClock) { onToggleMetric("clock", it) }
                MetricFilterChip("RAM", profile.showRam) { onToggleMetric("ram", it) }
                MetricFilterChip("Temperature", profile.showTemp) { onToggleMetric("temp", it) }
                MetricFilterChip("Battery", profile.showBattery) { onToggleMetric("battery", it) }
                MetricFilterChip("Display Hz", profile.showDisplayHz) { onToggleMetric("display", it) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rolling Performance Graph
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ROLLING PERFORMANCE GRAPH",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rolling canvas graph of FPS and frame-time latency",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Switch(
                    checked = profile.showGraph,
                    onCheckedChange = { onToggleGraph(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screen Center Marker (Harmless Visual Reticle)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SCREEN CENTER MARKER",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Harmless visual center reticle (Never interacts with game code)",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Switch(
                    checked = profile.showCrosshair,
                    onCheckedChange = { onToggleCrosshair(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GamingCrimson
                    )
                )
            }

            if (profile.showCrosshair) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RETICLE STYLE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DOT", "CROSS", "CIRCLE", "RETICLE").forEach { style ->
                        val isSel = profile.crosshairStyle == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) GamingCrimson else Color(0x221E293B))
                                .border(0.8.dp, if (isSel) GamingCrimson else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                .clickable {
                                    onUpdateCrosshairSettings(style, profile.crosshairColor, profile.crosshairSizeDp)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style,
                                color = if (isSel) Color.White else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RETICLE COLOR",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val colors = listOf(
                        0xFF00E5FF to "Cyan",
                        0xFFFF1744 to "Crimson",
                        0xFF00E676 to "Green",
                        0xFFFFD600 to "Yellow",
                        0xFFFFFFFF to "White"
                    )
                    colors.forEach { (cVal, _) ->
                        val isSel = profile.crosshairColor == cVal
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(cVal))
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) Color.White else Color(0x33000000),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateCrosshairSettings(profile.crosshairStyle, cVal, profile.crosshairSizeDp)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // MARKER POSITION (X and Y Offset Controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MARKER POSITION (X / Y)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300E5FF))
                            .clickable { onUpdateCrosshairOffset(0, 0) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("RESET (0, 0)", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // X Offset Slider
                Text(
                    text = "X Position: ${profile.crosshairOffsetX} px",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = profile.crosshairOffsetX.toFloat(),
                    onValueChange = { onUpdateCrosshairOffset(it.toInt(), profile.crosshairOffsetY) },
                    valueRange = -300f..300f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                // Y Offset Slider
                Text(
                    text = "Y Position: ${profile.crosshairOffsetY} px",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = profile.crosshairOffsetY.toFloat(),
                    onValueChange = { onUpdateCrosshairOffset(profile.crosshairOffsetX, it.toInt()) },
                    valueRange = -300f..300f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MetricFilterChip(
    label: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = { onToggle(!isSelected) },
        label = {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
            selectedLabelColor = NeonCyan,
            containerColor = Color(0x221E293B),
            labelColor = TextMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) NeonCyan else Color(0x22FFFFFF),
            borderWidth = 0.8.dp
        )
    )
}
