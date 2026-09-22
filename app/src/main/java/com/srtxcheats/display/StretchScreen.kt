package com.srtxcheats.display

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingCrimson
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StretchScreen(
    onNavigateToShizuku: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val resolutionManager = remember { DisplayResolutionManager(context) }
    val densityManager = remember { DisplayDensityManager(context) }
    val backupManager = remember { DisplayBackupManager(context) }

    val backup by backupManager.backupFlow.collectAsState(initial = DisplayBackup())
    val metricsInfo = remember { resolutionManager.getDisplayMetrics() }
    val presets = remember(metricsInfo.physicalWidth, metricsInfo.physicalHeight) {
        resolutionManager.generatePresets(metricsInfo.physicalWidth, metricsInfo.physicalHeight)
    }

    var selectedPreset by remember { mutableStateOf(presets.firstOrNull() ?: ResolutionPreset("Native", 1080, 2400, "1:1")) }
    var selectedDpi by remember { mutableStateOf(densityManager.getCurrentDensity()) }
    var selectedMode by remember { mutableStateOf(StretchMode.VERTICAL) }

    var isApplying by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    var showApplyDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val isShizukuConnected = remember { ShizukuManager.checkState(context) == ShizukuManager.ShizukuState.CONNECTED }
    val isShizukuAuthorized = remember { ShizukuManager.isAuthorized() }

    // On screen launch: ensure original baseline is saved
    LaunchedEffect(Unit) {
        backupManager.saveBaselineIfEmpty(
            width = metricsInfo.physicalWidth,
            height = metricsInfo.physicalHeight,
            density = densityManager.getCurrentDensity(),
            orientation = if (isLandscape) 2 else 1
        )
    }

    val calculatedTarget = remember(selectedPreset, selectedDpi, isLandscape, selectedMode) {
        StretchCalculator.calculate(
            physicalWidth = metricsInfo.physicalWidth,
            physicalHeight = metricsInfo.physicalHeight,
            selectedWidth = selectedPreset.width,
            selectedHeight = selectedPreset.height,
            selectedDpi = selectedDpi,
            isLandscape = isLandscape,
            mode = selectedMode
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("stretch_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STRETCH SCREEN",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SYSTEM DISPLAY CONFIGURATION & DPI SCALING",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x2200E5FF))
                    .clickable {
                        coroutineScope.launch {
                            val activeDpi = densityManager.queryWmDensityViaShizuku() ?: densityManager.getCurrentDensity()
                            selectedDpi = activeDpi
                            statusMessage = "Display state refreshed"
                            isErrorMessage = false
                        }
                    }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Feedback Banner
        AnimatedVisibility(visible = statusMessage != null) {
            statusMessage?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isErrorMessage) GamingCrimson.copy(alpha = 0.2f) else GamingGreen.copy(alpha = 0.2f))
                        .border(1.dp, if (isErrorMessage) GamingCrimson else GamingGreen, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isErrorMessage) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isErrorMessage) GamingCrimson else GamingGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = TextPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Card 1: CURRENT DISPLAY
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CURRENT DISPLAY",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isShizukuAuthorized) GamingGreen else GamingAmber)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isShizukuAuthorized) "Shizuku Authorized" else "Shizuku Disconnected",
                        color = if (isShizukuAuthorized) GamingGreen else GamingAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DisplayInfoItem("Resolution", "${metricsInfo.currentWidth} × ${metricsInfo.currentHeight}")
                DisplayInfoItem("Density", "${densityManager.getCurrentDensity()} DPI")
                DisplayInfoItem("Orientation", if (isLandscape) "Landscape" else "Portrait")
            }

            if (!isShizukuAuthorized) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22FFA000))
                        .clickable { onNavigateToShizuku() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Shizuku permission required for system display scaling",
                        color = GamingAmber,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("AUTHORIZE >", color = NeonCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: TARGET CONFIGURATION
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TARGET CONFIGURATION",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Choose resolution preset, density and stretch mode",
                color = TextSecondary,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resolution Presets
            Text("RESOLUTION PRESET", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { preset ->
                    val isSel = selectedPreset.resolutionKey == preset.resolutionKey
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedPreset = preset },
                        label = {
                            Text(
                                text = preset.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
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
                            selected = isSel,
                            borderColor = if (isSel) NeonCyan else Color(0x22FFFFFF),
                            borderWidth = 0.8.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DPI Presets
            Text("DISPLAY DENSITY (DPI)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                densityManager.generateRecommendedDpi(densityManager.getCurrentDensity()).take(5).forEach { dpi ->
                    val isSel = selectedDpi == dpi
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NeonCyan else Color(0x221E293B))
                            .border(0.8.dp, if (isSel) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { selectedDpi = dpi }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$dpi",
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stretch Mode
            Text("STRETCH MODE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    StretchMode.VERTICAL to "Vertical / Cutout",
                    StretchMode.HORIZONTAL to "Horizontal",
                    StretchMode.BOTH to "Both / Full"
                ).forEach { (mode, label) ->
                    val isSel = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) GamingAmber else Color(0x221E293B))
                            .border(0.8.dp, if (isSel) GamingAmber else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { selectedMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calculated Output Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1800E5FF))
                    .border(0.8.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CALCULATED TARGET:", color = NeonCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${calculatedTarget.targetWidth} × ${calculatedTarget.targetHeight} @ ${calculatedTarget.targetDensity} DPI",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 3: ACTION BUTTONS
        GlassCard(modifier = Modifier.fillMaxWidth(), glowAccent = true) {
            Text(
                text = "SYSTEM DISPLAY ACTIONS",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showApplyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_stretch_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                enabled = !isApplying && isShizukuAuthorized
            ) {
                if (isApplying) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APPLYING STRETCH...", fontWeight = FontWeight.Black, fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APPLY STRETCH", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showRestoreDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("restore_display_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GamingCrimson
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingCrimson),
                enabled = !isApplying && isShizukuAuthorized
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESTORE ORIGINAL DISPLAY", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 4: SAFETY & BACKUP STATUS
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = GamingGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FAIL-SAFE BACKUP SYSTEM",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (backup.isValid) {
                val dateStr = remember(backup.savedAt) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(backup.savedAt))
                }
                Text(
                    text = "Original configuration backed up: ${backup.formattedResolution} @ ${backup.originalDensity} DPI ($dateStr)",
                    color = GamingGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Original baseline will be captured automatically before modification.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "WARNING: Changing display configuration may temporarily change screen scaling, aspect ratio, UI size, or touch coordinates. You can restore your display at any time from this screen or from the Floating Menu.",
                color = TextSecondary,
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirmation Dialog for Apply Stretch
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = {
                Text("Apply Display Stretch?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        "Resolution: ${calculatedTarget.targetWidth} × ${calculatedTarget.targetHeight}\n" +
                        "DPI: ${calculatedTarget.targetDensity}\n" +
                        "Mode: ${calculatedTarget.mode.name}\n\n" +
                        "Your original display configuration (${backup.formattedResolution}) has been safely backed up.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApplyDialog = false
                        isApplying = true
                        coroutineScope.launch {
                            val result = DisplayCommandExecutor.applyStretch(
                                targetWidth = calculatedTarget.targetWidth,
                                targetHeight = calculatedTarget.targetHeight,
                                targetDensity = calculatedTarget.targetDensity,
                                backup = backup
                            )
                            isApplying = false
                            when (result) {
                                is DisplayExecutionResult.Success -> {
                                    statusMessage = result.message
                                    isErrorMessage = false
                                }
                                is DisplayExecutionResult.Failure -> {
                                    statusMessage = result.error
                                    isErrorMessage = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("APPLY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = Color(0xFF0D1424)
        )
    }

    // Confirmation Dialog for Restore
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text("Restore Original Display?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Restore the hardware baseline display configuration: ${backup.formattedResolution} @ ${backup.originalDensity} DPI?",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        isApplying = true
                        coroutineScope.launch {
                            val result = DisplayCommandExecutor.restoreOriginal(backup)
                            isApplying = false
                            when (result) {
                                is DisplayExecutionResult.Success -> {
                                    statusMessage = result.message
                                    isErrorMessage = false
                                }
                                is DisplayExecutionResult.Failure -> {
                                    statusMessage = result.error
                                    isErrorMessage = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GamingCrimson, contentColor = Color.White)
                ) {
                    Text("RESTORE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = Color(0xFF0D1424)
        )
    }
}

@Composable
fun DisplayInfoItem(label: String, value: String) {
    Column {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
