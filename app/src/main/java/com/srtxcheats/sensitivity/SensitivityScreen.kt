package com.srtxcheats.sensitivity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.sensitivity.touch.TouchCurve
import com.srtxcheats.sensitivity.touch.TouchEnginePhase
import com.srtxcheats.sensitivity.touch.TouchSensitivityConfig
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingCrimson
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensitivityScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { SensitivityViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidthDp < 360.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSmallScreen) 10.dp else 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF))
                                .border(1.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Sensitivity",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SENSITIVITY BOOST",
                                color = Color.White,
                                fontSize = if (isSmallScreen) 16.sp else 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "DEVICE-LEVEL INPUT RESPONSE ENGINE",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x2000E5FF),
                        border = BorderStroke(0.6.dp, Color(0x4000E5FF)),
                        modifier = Modifier.clickable { viewModel.loadInitialState() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(13.dp))
                            Text("AUDIT", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 1. SHIZUKU STATUS & HARDWARE COMPATIBILITY
                // ==========================================
                val caps = uiState.capabilities
                val isShizukuOk = caps?.isShizukuAuthorized == true
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowAccent = isShizukuOk
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isShizukuOk) Icons.Default.Security else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (isShizukuOk) GamingGreen else GamingAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (isShizukuOk) "SHIZUKU PRIVILEGE: ACTIVE" else "SHIZUKU PERMISSION REQUIRED",
                                    color = if (isShizukuOk) GamingGreen else GamingAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${caps?.manufacturer ?: "Android"} ${caps?.model ?: "Device"} (SDK ${caps?.sdkInt ?: 0}) • ${caps?.maxRefreshRate?.toInt() ?: 60}Hz",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        if (!isShizukuOk) {
                            Button(
                                onClick = { viewModel.requestShizukuPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = GamingAmber),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("AUTHORIZE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x2200E676),
                                border = BorderStroke(0.5.dp, GamingGreen)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    color = GamingGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isShizukuOk) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Shizuku permission required for supported system-level sensitivity controls.",
                            color = GamingAmber,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 2. MULTIPLIER COMPARISON BANNER
                // ==========================================
                val requestedMult = String.format(java.util.Locale.US, "%.1fX", uiState.requestedMultiplier)
                val supportedMult = String.format(java.util.Locale.US, "%.1fX", uiState.actualSupportedMultiplier)
                val statusColor = when (uiState.supportStatus) {
                    SensitivitySupportStatus.FULLY_SUPPORTED -> GamingGreen
                    SensitivitySupportStatus.PARTIALLY_SUPPORTED -> GamingAmber
                    SensitivitySupportStatus.LIMITED -> NeonCyan
                    SensitivitySupportStatus.FAILED -> GamingCrimson
                    SensitivitySupportStatus.RESTORED -> Color(0xFF81D4FA)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x1800E5FF),
                    border = BorderStroke(0.8.dp, Color(0x4400E5FF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("PHONE DEFAULT", color = Color(0xFF78909C), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                Text("1.0X", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CURRENT BOOST", color = Color(0xFF78909C), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                Text("$requestedMult requested", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ACTUAL SUPPORTED", color = Color(0xFF78909C), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                Text(supportedMult, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "STATUS",
                                color = Color(0xFF90A4AE),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = statusColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, statusColor)
                            ) {
                                Text(
                                    text = uiState.supportStatus.label,
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // IPHONE & IQOO 200% ULTRA TOUCH MODE (SPECIALIZED FF PRESET)
                // ==========================================
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowAccent = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x33FF9100)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = GamingAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "iQOO & iPHONE 200% ULTRA TOUCH",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Simulates 200% Sensi even with 0 in Free Fire",
                                        color = GamingAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = Color(0x22FF9100),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.6.dp, GamingAmber)
                            ) {
                                Text(
                                    text = "200% EMULATION",
                                    color = GamingAmber,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Calibrates LSQ2 zero-friction velocity tracker, 7/7 max pointer speed, 100ms multi-tap delay, 0ms touch damping delay, and high-frequency touch polling so 1cm swipe turns 360° even at 0 in-game sensitivity.",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp
                        )

                        Button(
                            onClick = { viewModel.applyIphoneIqooUltraMode() },
                            enabled = !uiState.isApplying && !uiState.isRestoring,
                            colors = ButtonDefaults.buttonColors(containerColor = GamingAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text(
                                text = if (uiState.isApplying) "CALIBRATING 200% TOUCH..." else "ACTIVATE 200% TOUCH RESPONSE",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // REAL TOUCH ENGINE — per-axis live tuning (grab + re-inject)
                // ==========================================
                val enginePhase = uiState.enginePhase
                val engineStatusText = when (enginePhase) {
                    TouchEnginePhase.GRABBED -> "ENGINE LIVE — INJECTING"
                    TouchEnginePhase.STARTING -> "STARTING..."
                    TouchEnginePhase.BINDING -> "BINDING SERVICE..."
                    TouchEnginePhase.DETECTING -> "DETECTING DEVICES..."
                    TouchEnginePhase.BOUND -> "READY (idle)"
                    TouchEnginePhase.UNAVAILABLE -> "SHIZUKU REQUIRED"
                    TouchEnginePhase.ERROR -> "ERROR"
                    TouchEnginePhase.IDLE -> "IDLE"
                }
                val engineColor = when (enginePhase) {
                    TouchEnginePhase.GRABBED -> GamingGreen
                    TouchEnginePhase.ERROR, TouchEnginePhase.UNAVAILABLE -> GamingCrimson
                    TouchEnginePhase.IDLE -> Color(0xFF90A4AE)
                    else -> GamingAmber
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowAccent = uiState.engineActive
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x2200E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("REAL TOUCH ENGINE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text("Grabs the digitizer & re-injects scaled touch", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = engineColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, engineColor)
                        ) {
                            Text(engineStatusText, color = engineColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    val engineDetail = uiState.engineDevice?.let { "Device: $it" } ?: uiState.engineMessage
                    if (engineDetail != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(engineDetail, color = Color(0xFF90A4AE), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sensitivity X
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SENSITIVITY X", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(java.util.Locale.US, "%.1fx", uiState.gainX), color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = TouchSensitivityConfig.gainToSlider(uiState.gainX).toFloat(),
                        onValueChange = { viewModel.setGainX(TouchSensitivityConfig.sliderToGain(it.toInt())) },
                        valueRange = 0f..TouchSensitivityConfig.GAIN_SLIDER_MAX.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0x33FFFFFF)),
                        modifier = Modifier.fillMaxWidth().testTag("touch_gain_x_slider")
                    )

                    // Sensitivity Y
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SENSITIVITY Y", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(java.util.Locale.US, "%.1fx", uiState.gainY), color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = TouchSensitivityConfig.gainToSlider(uiState.gainY).toFloat(),
                        onValueChange = { viewModel.setGainY(TouchSensitivityConfig.sliderToGain(it.toInt())) },
                        valueRange = 0f..TouchSensitivityConfig.GAIN_SLIDER_MAX.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0x33FFFFFF)),
                        modifier = Modifier.fillMaxWidth().testTag("touch_gain_y_slider")
                    )

                    // Smoothing
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SMOOTHING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(java.util.Locale.US, "%.2f", uiState.smoothing), color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = TouchSensitivityConfig.smoothingToSlider(uiState.smoothing).toFloat(),
                        onValueChange = { viewModel.setSmoothing(TouchSensitivityConfig.sliderToSmoothing(it.toInt())) },
                        valueRange = 0f..TouchSensitivityConfig.SMOOTH_SLIDER_MAX.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0x33FFFFFF)),
                        modifier = Modifier.fillMaxWidth().testTag("touch_smoothing_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Response curve
                    Text("RESPONSE CURVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TouchCurve.values().forEach { c ->
                            val selected = uiState.curve == c
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setCurve(c) }
                                    .testTag("curve_${c.name.lowercase()}"),
                                color = if (selected) Color(0x2500E5FF) else Color(0x181E293B),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) NeonCyan else Color(0x25FFFFFF))
                            ) {
                                Text(
                                    text = TouchCurve.label(c),
                                    color = if (selected) NeonCyan else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detect (safe) + Stop (turn-off)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.detectTouchDevices() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = BorderStroke(1.dp, Color(0x6600E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("btn_detect_touch")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DETECT", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { viewModel.disableTouchEngine() },
                            enabled = uiState.engineActive || enginePhase == TouchEnginePhase.STARTING,
                            colors = ButtonDefaults.buttonColors(containerColor = GamingCrimson, disabledContainerColor = Color(0x33FF1744)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("btn_stop_engine")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STOP ENGINE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "X/Y scale how far a swipe travels vs. your finger. Applied live while the engine runs; saved for the next Apply.",
                        color = Color(0xFF78909C),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 3. MASTER SENSITIVITY SLIDER & PRESETS
                // ==========================================
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowAccent = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SENSITIVITY BOOST",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Current: ${uiState.sliderPercent}% (${uiState.currentLevel.displayName} • $requestedMult)",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2200E5FF),
                            border = BorderStroke(0.5.dp, Color(0x6600E5FF))
                        ) {
                            Text(
                                text = "${uiState.sliderPercent}%",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = uiState.sliderPercent.toFloat(),
                        onValueChange = { newVal -> viewModel.onSliderChange(newVal.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sensitivity_boost_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0% (1.0X)", color = Color(0xFF607D8B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        Text("50% (2.0X)", color = Color(0xFF607D8B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        Text("75% (3.0X)", color = Color(0xFF607D8B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        Text("100% (5.0X)", color = Color(0xFF607D8B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Presets Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SensitivityLevel.values().forEach { lvl ->
                            val isSelected = uiState.currentLevel == lvl
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonCyan else Color(0x25FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectPreset(lvl) }
                                    .testTag("preset_${lvl.name.lowercase()}"),
                                color = if (isSelected) Color(0x2500E5FF) else Color(0x181E293B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = lvl.displayName,
                                        color = if (isSelected) NeonCyan else Color.White,
                                        fontSize = if (isSmallScreen) 9.sp else 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = lvl.multiplierLabel,
                                        color = if (isSelected) Color.White else Color(0xFF90A4AE),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ==========================================
                    // 4. ACTION BUTTONS: APPLY & RESTORE
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.applySensitivity() },
                            enabled = !uiState.isApplying && !uiState.isRestoring,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp)
                                .testTag("btn_apply_sensitivity")
                        ) {
                            if (uiState.isApplying) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VERIFYING...", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("APPLY SENSITIVITY", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.restoreSensitivity() },
                            enabled = !uiState.isApplying && !uiState.isRestoring,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_restore_sensitivity")
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RESTORE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Status Message
                    AnimatedVisibility(
                        visible = uiState.statusMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.isError) Color(0x22FF1744) else Color(0x2200E676),
                                border = BorderStroke(0.6.dp, if (uiState.isError) GamingCrimson else GamingGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (uiState.isError) GamingCrimson else GamingGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = uiState.statusMessage.orEmpty(),
                                        color = if (uiState.isError) Color(0xFFFF8A80) else Color(0xFFB9F6CA),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 5. SYSTEM CHANGES & VERIFICATION LOG
                // ==========================================
                val lastRes = uiState.lastResult
                if (lastRes != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TRANSACTIONAL VERIFICATION REPORT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Verified Settings
                        if (lastRes.verifiedSettings.isNotEmpty()) {
                            Text("VERIFIED & ACTIVE (${lastRes.verifiedSettings.size})", color = GamingGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            lastRes.verifiedSettings.forEach { item ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GamingGreen, modifier = Modifier.size(12.dp))
                                    Text(item, color = Color(0xFFECEFF1), fontSize = 9.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Unsupported Settings
                        if (lastRes.unsupportedSettings.isNotEmpty()) {
                            Text("UNSUPPORTED BY DEVICE (${lastRes.unsupportedSettings.size})", color = Color(0xFF90A4AE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            lastRes.unsupportedSettings.forEach { item ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(12.dp))
                                    Text(item, color = Color(0xFFB0BEC5), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ==========================================
                // 6. POLICY & INTEGRITY NOTICE
                // ==========================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x10FFFFFF),
                    border = BorderStroke(0.5.dp, Color(0x18FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(15.dp))
                        Text(
                            text = "Operates strictly at the Android system level. Does not modify game files, memory, or processes.",
                            color = Color(0xFF90A4AE),
                            fontSize = 8.5.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
