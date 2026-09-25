package com.srtxcheats.macro

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingCrimson
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan

/**
 * Macro & auto-fire screen. Two gaming tools driven by the privileged touch service:
 *  - **Auto-clicker** — pick a screen point, set 10–100 clicks/second, fire (looping or a fixed burst).
 *  - **Recorder / player** — record real touches, save by name, replay at 25–400% speed.
 *
 * Everything has an explicit OFF: STOP for auto-click/playback, STOP RECORDING for capture. State comes
 * from [MacroViewModel] which mirrors what the uid-2000 service is actually doing.
 */
@Composable
fun MacroScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel = remember { MacroViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp.dp < 360.dp

    // Aspect ratio of the auto-click point picker, matched to the real screen so the dot maps 1:1.
    val screenAspect = (configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat())
        .coerceIn(0.4f, 1.2f)

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
                // ---- Header ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Default.Bolt, contentDescription = "Macro", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "MACRO & AUTO-FIRE",
                            color = Color.White,
                            fontSize = if (isSmallScreen) 16.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "RECORD • REPLAY • AUTO-CLICK  (FREE FIRE READY)",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ---- Shizuku status ----
                if (!uiState.isPrivileged) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = GamingAmber, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("SHIZUKU PERMISSION REQUIRED", color = GamingAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Macros inject touches through the privileged service.", color = Color(0xFF90A4AE), fontSize = 9.sp)
                                }
                            }
                            Button(
                                onClick = { viewModel.requestShizukuPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = GamingAmber),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("AUTHORIZE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ---- Live status banner ----
                MacroStatusBanner(uiState)

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // AUTO-CLICKER
                // ==========================================
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = uiState.phase == MacroPhase.AUTOCLICK) {
                    SectionHeader(
                        icon = Icons.Default.TouchApp,
                        title = "AUTO-CLICKER",
                        subtitle = "Rapid taps at one point — ${MacroTiming.MIN_CPS}–${MacroTiming.MAX_CPS} clicks/sec",
                        accent = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("TARGET POINT — tap the box to place your fire button", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Tap-to-place point picker, mirroring the phone screen's proportions.
                    var boxSize by remember { mutableStateOf(IntSize.Zero) }
                    val markerColor = if (uiState.phase == MacroPhase.AUTOCLICK) GamingGreen else NeonCyan
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(screenAspect)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x14000000))
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                            .onSizeChanged { boxSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures { offset: Offset ->
                                    if (boxSize.width > 0 && boxSize.height > 0) {
                                        viewModel.setAutoClickPoint(
                                            offset.x / boxSize.width,
                                            offset.y / boxSize.height
                                        )
                                    }
                                }
                            }
                            .testTag("autoclick_point_picker")
                    ) {
                        // Marker at the current normalized point, kept fully inside the box.
                        val dotSize = 18.dp
                        val travelX = (maxWidth - dotSize).coerceAtLeast(0.dp)
                        val travelY = (maxHeight - dotSize).coerceAtLeast(0.dp)
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .offset(x = travelX * uiState.autoClickX, y = travelY * uiState.autoClickY)
                                .clip(CircleShape)
                                .background(markerColor.copy(alpha = 0.25f))
                                .border(2.dp, markerColor, CircleShape)
                        )
                        Text(
                            text = "X ${(uiState.autoClickX * 100).toInt()}%   Y ${(uiState.autoClickY * 100).toInt()}%",
                            color = Color(0xFF90A4AE),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CPS slider.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SPEED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${uiState.cps} clicks/s", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = uiState.cps.toFloat(),
                        onValueChange = { viewModel.setCps(it.toInt()) },
                        valueRange = MacroTiming.MIN_CPS.toFloat()..MacroTiming.MAX_CPS.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0x33FFFFFF)),
                        modifier = Modifier.fillMaxWidth().testTag("autoclick_cps_slider")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${MacroTiming.MIN_CPS}/s", color = Color(0xFF607D8B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        Text("${MacroTiming.MAX_CPS}/s", color = Color(0xFF607D8B), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Loop / burst.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LOOP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.loop) "Fire until STOP" else "Fire ${uiState.burstCount} taps then stop",
                                color = Color(0xFF90A4AE), fontSize = 8.5.sp
                            )
                        }
                        Switch(
                            checked = uiState.loop,
                            onCheckedChange = { viewModel.setLoop(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = Color(0xFF90A4AE),
                                uncheckedTrackColor = Color(0x22FFFFFF)
                            ),
                            modifier = Modifier.testTag("macro_loop_switch")
                        )
                    }

                    if (!uiState.loop) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BURST", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${uiState.burstCount} taps", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = uiState.burstCount.toFloat(),
                            onValueChange = { viewModel.setBurstCount(it.toInt()) },
                            valueRange = 1f..500f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0x33FFFFFF)),
                            modifier = Modifier.fillMaxWidth().testTag("autoclick_burst_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.startAutoClick() },
                            enabled = uiState.phase != MacroPhase.AUTOCLICK && !uiState.isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = GamingGreen, disabledContainerColor = Color(0x3300E676)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("btn_start_autoclick")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("START", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { viewModel.stopMacro() },
                            enabled = uiState.isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = GamingCrimson, disabledContainerColor = Color(0x33FF1744)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("btn_stop_autoclick")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STOP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // RECORDER
                // ==========================================
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = uiState.isRecording) {
                    SectionHeader(
                        icon = Icons.Default.FiberManualRecord,
                        title = "MACRO RECORDER",
                        subtitle = "Capture your moves — the game keeps responding",
                        accent = if (uiState.isRecording) GamingCrimson else GamingAmber
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.isRecording) {
                        val transition = rememberInfiniteTransition(label = "rec")
                        val pulse by transition.animateFloat(
                            initialValue = 0.35f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(GamingCrimson.copy(alpha = pulse))
                            )
                            Text("● REC — perform your combo, then STOP", color = GamingCrimson, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text(
                            text = "Tap RECORD, play the sequence you want automated (e.g. gloo-wall + shoot), then STOP to name & save it.",
                            color = Color(0xFFB0BEC5), fontSize = 9.sp, lineHeight = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (!uiState.isRecording) {
                        Button(
                            onClick = { viewModel.startRecording() },
                            enabled = !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = GamingCrimson, disabledContainerColor = Color(0x33FF1744)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_record")
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RECORD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_stop_record")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP RECORDING", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // SAVED MACROS + PLAYBACK
                // ==========================================
                GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = uiState.phase == MacroPhase.PLAYING) {
                    SectionHeader(
                        icon = Icons.Default.PlayArrow,
                        title = "SAVED MACROS",
                        subtitle = "Replay a recording at your chosen speed",
                        accent = GamingGreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Playback speed.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PLAYBACK SPEED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${uiState.playSpeedPercent}%", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = uiState.playSpeedPercent.toFloat(),
                        onValueChange = { viewModel.setSpeedPercent(it.toInt()) },
                        valueRange = MacroTiming.MIN_SPEED_PERCENT.toFloat()..MacroTiming.MAX_SPEED_PERCENT.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0x33FFFFFF)),
                        modifier = Modifier.fillMaxWidth().testTag("playback_speed_slider")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${MacroTiming.MIN_SPEED_PERCENT}% (slower)", color = Color(0xFF607D8B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("100%", color = Color(0xFF607D8B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("${MacroTiming.MAX_SPEED_PERCENT}% (faster)", color = Color(0xFF607D8B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.macros.isEmpty()) {
                        Text(
                            text = "No saved macros yet. Record one above to get started.",
                            color = Color(0xFF78909C), fontSize = 9.5.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.macros.forEach { macro ->
                                MacroRow(
                                    macro = macro,
                                    isPlaying = uiState.activeMacroId == macro.id && uiState.phase == MacroPhase.PLAYING,
                                    enabled = !uiState.isBusy || (uiState.activeMacroId == macro.id),
                                    onPlay = { viewModel.play(macro) },
                                    onStop = { viewModel.stopMacro() },
                                    onDelete = { viewModel.deleteMacro(macro) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Global stop — kills any playback or auto-click.
                    OutlinedButton(
                        onClick = { viewModel.stopMacro() },
                        enabled = uiState.isRunning,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GamingCrimson),
                        border = BorderStroke(1.dp, if (uiState.isRunning) GamingCrimson else Color(0x33FF1744)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("btn_stop_all")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP PLAYBACK", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ---- Footer ----
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
                            text = "Macros replay synthetic touches through the privileged input path. Recording never grabs the screen, so the game stays fully playable while you capture.",
                            color = Color(0xFF90A4AE), fontSize = 8.5.sp, lineHeight = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ---- Save-recording dialog ----
    if (uiState.pendingRecording != null) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.discardRecording() },
            containerColor = Color(0xFF0E1522),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0BEC5),
            title = { Text("SAVE MACRO", fontWeight = FontWeight.Black, fontSize = 15.sp) },
            text = {
                Column {
                    Text("Name this recording so you can replay it later.", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Gloo + Headshot", color = Color(0xFF607D8B)) },
                        modifier = Modifier.fillMaxWidth().testTag("macro_name_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveRecording(name) },
                    colors = ButtonDefaults.buttonColors(containerColor = GamingGreen),
                    modifier = Modifier.testTag("btn_save_macro")
                ) {
                    Text("SAVE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardRecording() }) {
                    Text("DISCARD", color = GamingCrimson, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun MacroStatusBanner(uiState: MacroUiState) {
    val (label, color) = when (uiState.phase) {
        MacroPhase.RECORDING -> "RECORDING" to GamingCrimson
        MacroPhase.PLAYING -> "PLAYING MACRO" to GamingGreen
        MacroPhase.AUTOCLICK -> "AUTO-CLICKING" to GamingGreen
        MacroPhase.ERROR -> "ERROR" to GamingCrimson
        MacroPhase.IDLE -> "IDLE" to Color(0xFF90A4AE)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = when (uiState.phase) {
                        MacroPhase.ERROR -> Icons.Default.ErrorOutline
                        MacroPhase.IDLE -> Icons.Default.Security
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null, tint = color, modifier = Modifier.size(16.dp)
                )
                Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
            val msg = uiState.message
            if (!msg.isNullOrBlank()) {
                Text(
                    text = msg,
                    color = if (uiState.isError) Color(0xFFFF8A80) else Color(0xFFB0BEC5),
                    fontSize = 9.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MacroRow(
    macro: MacroSummary,
    isPlaying: Boolean,
    enabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isPlaying) Color(0x1500E676) else Color(0x141E293B),
        border = BorderStroke(1.dp, if (isPlaying) GamingGreen else Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(macro.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    text = "${macro.frameCount} frames • ${"%.1f".format(macro.durationMs / 1000f)}s",
                    color = Color(0xFF90A4AE), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace
                )
            }
            // Play / Stop toggle.
            Surface(
                shape = CircleShape,
                color = if (isPlaying) GamingCrimson else GamingGreen,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(enabled = enabled) { if (isPlaying) onStop() else onPlay() }
                    .testTag("btn_play_${macro.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // Delete.
            Surface(
                shape = CircleShape,
                color = Color(0x22FF1744),
                border = BorderStroke(0.8.dp, GamingCrimson),
                modifier = Modifier
                    .size(34.dp)
                    .clickable(enabled = !isPlaying) { onDelete() }
                    .testTag("btn_delete_${macro.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GamingCrimson, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
