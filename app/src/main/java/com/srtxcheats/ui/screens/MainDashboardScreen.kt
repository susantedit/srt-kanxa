package com.srtxcheats.ui.screens

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.srtxcheats.core.DualSimManager
import com.srtxcheats.core.GameFocusModeManager
import com.srtxcheats.core.NetworkBooster
import com.srtxcheats.core.ScreenCaptureManager
import com.srtxcheats.core.SignalRadarScanner
import com.srtxcheats.core.SystemCleaner
import com.srtxcheats.ui.overlay.MarkerConstants
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.srtxcheats.core.GameDetector
import com.srtxcheats.core.MemoryOptimizer
import com.srtxcheats.core.RamBooster
import com.srtxcheats.core.RootExecutor
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.sensitivity.SensitivityEngine
import com.srtxcheats.sensitivity.SensitivityBoostResult
import com.srtxcheats.sensitivity.SensitivitySupportStatus
import com.srtxcheats.model.DeviceMetrics
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import com.srtxcheats.service.OverlayService
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboardScreen(
    metrics: DeviceMetrics,
    profile: GameProfile,
    logoBitmap: Bitmap?,
    isOverlayServiceActive: Boolean,
    keyDurationText: String = "ACTIVE",
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onSelectGameClicked: () -> Unit,
    onGameSelected: (String, String) -> Unit = { _, _ -> },
    onSensitivityLevelSelected: (SensitivityLevel) -> Unit,
    onSensitivityPercentSelected: (Int) -> Unit = {},
    onTogglePlus50Boost: (Boolean) -> Unit = {},
    onToggleFreeStyle800: (Boolean) -> Unit = {},
    onToggleDragMode: (Boolean) -> Unit = {},
    onToggleUltraProMax: (Boolean) -> Unit = {},
    onSaveFreeStyleDpi: (Int) -> Unit = {},
    onToggleGameBoost: (Boolean) -> Unit,
    onToggleGamingMode: (Boolean) -> Unit = {},
    onTogglePerformanceBoost: (Boolean) -> Unit = {},
    onOpenShizuku: () -> Unit,
    onOpenTouchTest: () -> Unit,
    onOpenRadar: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp <= 370
    val contentPadding = if (isCompactScreen) 10.dp else 16.dp
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showGameNotFoundDialog by remember { mutableStateOf(false) }
    var ramBoostReport by remember { mutableStateOf<RamBooster.BoostResult?>(null) }
    var isOptimizingRam by remember { mutableStateOf(false) }

    // Permission state refresh
    var hasOverlayPermission by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true) }
    var hasNotificationPermission by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var hasUsagePermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var isShizukuAuthorized by remember { mutableStateOf(ShizukuManager.isAuthorized()) }
    var hasWriteSettings by remember { mutableStateOf(SensitivityEngine.hasWriteSettingsPermission(context)) }
    var hasWriteSecureSettings by remember { mutableStateOf(SensitivityEngine.hasWriteSecureSettingsPermission(context)) }
    var isRootAvailable by remember { mutableStateOf(RootExecutor.isRootAvailable()) }

    fun refreshPermissions() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
        hasUsagePermission = checkUsageStatsPermission(context)
        isShizukuAuthorized = ShizukuManager.isAuthorized()
        hasWriteSettings = SensitivityEngine.hasWriteSettingsPermission(context)
        hasWriteSecureSettings = SensitivityEngine.hasWriteSecureSettingsPermission(context)
        isRootAvailable = RootExecutor.isRootAvailable()
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                showPermissionDialog = true
                return
            }
        }
        onStartOverlay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .testTag("main_dashboard_screen")
    ) {
        // App Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = "SRT X CHEATS Logo",
                        modifier = Modifier
                            .size(if (isCompactScreen) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .border(1.dp, NeonCyan, CircleShape)
                    )
                }
                Column {
                    Text(
                        text = "SRT X CHEATS",
                        color = TextPrimary,
                        fontSize = if (isCompactScreen) 16.sp else 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "FPS PERFORMANCE PANEL",
                        color = NeonCyan,
                        fontSize = if (isCompactScreen) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Shizuku Connection Badge
            val shizukuState = ShizukuManager.checkState(context)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33101726))
                    .border(
                        0.5.dp,
                        if (shizukuState == ShizukuManager.ShizukuState.CONNECTED) GamingGreen else Color(0x33FFFFFF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onOpenShizuku)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (shizukuState) {
                                    ShizukuManager.ShizukuState.CONNECTED -> GamingGreen
                                    ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> GamingAmber
                                    ShizukuManager.ShizukuState.NOT_INSTALLED -> TextMuted
                                }
                            )
                    )
                    Text(
                        text = when (shizukuState) {
                            ShizukuManager.ShizukuState.CONNECTED -> "SHIZUKU CONNECTED"
                            ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> "SHIZUKU OFF"
                            ShizukuManager.ShizukuState.NOT_INSTALLED -> "SHIZUKU"
                        },
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // License Key Validity & Duration Card
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0x1800E5FF),
            border = BorderStroke(0.7.dp, Color(0x3300E5FF))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200E676)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "License",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ACTIVATION KEY STATUS",
                            color = TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "AUTHORIZED & ACTIVE",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x2200E5FF),
                    border = BorderStroke(0.5.dp, Color(0x5500E5FF))
                ) {
                    Text(
                        text = keyDurationText,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Game Card (Per-Game profile target)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowAccent = true
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3300E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = "Game",
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TARGET GAME PROFILE",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = profile.appName.ifBlank { "Free Fire MAX" },
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile.packageName.ifBlank { "com.dts.freefiremax" },
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val isTargetInstalled = remember(profile.packageName) {
                            GameDetector.isPackageInstalled(context, profile.packageName)
                        }
                        val detectedFreeFire = remember(profile.packageName) {
                            GameDetector.findInstalledFreeFire(context)
                        }

                        if (isTargetInstalled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GamingGreen)
                                )
                                Text("INSTALLED & READY", color = GamingGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (detectedFreeFire != null && detectedFreeFire.first != profile.packageName) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GamingAmber)
                                )
                                Text(
                                    text = "Found ${detectedFreeFire.second}",
                                    color = GamingAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "[Switch]",
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        onGameSelected(detectedFreeFire.first, detectedFreeFire.second)
                                    }
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TextMuted)
                                )
                                Text("NOT DETECTED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSelectGameClicked,
                    modifier = Modifier.testTag("select_game_button"),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(NeonCyan, Color(0x3300E5FF))))
                ) {
                    Text("CHANGE GAME", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boost & Launch Action Button
            Button(
                onClick = {
                    val outcome = GameDetector.launchGameDetailed(context, profile.packageName)
                    when (outcome) {
                        is GameDetector.LaunchResult.Success -> {
                            checkOverlayPermissionAndStart()
                            onToggleGameBoost(true)
                            if (outcome.switchedFromFallback) {
                                Toast.makeText(
                                    context,
                                    "Detected and launched ${outcome.appName}!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onGameSelected(outcome.launchedPackage, outcome.appName)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Launching ${outcome.appName} with FPS Overlay...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        is GameDetector.LaunchResult.NotFound -> {
                            showGameNotFoundDialog = true
                        }
                        is GameDetector.LaunchResult.Error -> {
                            Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("boost_and_launch_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GamingAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BOOST & LAUNCH GAME",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PRIMARY DASHBOARD ENGINE SWITCHES: Gaming Mode & Performance Boost (Persisted in DataStore)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("primary_engine_modes_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "PRIMARY ENGINE CONTROLS",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        color = Color(0x1A00E5FF),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0x3300E5FF))
                    ) {
                        Text(
                            text = "DATASTORE SYNCED",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Switch 1: Gaming Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (profile.isGamingModeActive) Color(0x1400E5FF) else Color(0x0AFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (profile.isGamingModeActive) Color(0x3300E5FF) else Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = "Gaming Mode",
                                tint = if (profile.isGamingModeActive) NeonCyan else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Gaming Mode",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (profile.isGamingModeActive) Color(0x3300E5FF) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = if (profile.isGamingModeActive) "ACTIVE" else "STANDBY",
                                        color = if (profile.isGamingModeActive) NeonCyan else TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "CPU priority scheduling & notification suppression",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = profile.isGamingModeActive,
                        onCheckedChange = { onToggleGamingMode(it) },
                        modifier = Modifier.testTag("gaming_mode_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                // Switch 2: Performance Boost
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (profile.isPerformanceBoostActive) Color(0x14FF9100) else Color(0x0AFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (profile.isPerformanceBoostActive) Color(0x33FF9100) else Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Performance Boost",
                                tint = if (profile.isPerformanceBoostActive) GamingAmber else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Performance Boost",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (profile.isPerformanceBoostActive) Color(0x33FF9100) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = if (profile.isPerformanceBoostActive) "MAX FREQ" else "OFF",
                                        color = if (profile.isPerformanceBoostActive) GamingAmber else TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Instant GPU clock boost & RAM cache acceleration",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = profile.isPerformanceBoostActive,
                        onCheckedChange = { onTogglePerformanceBoost(it) },
                        modifier = Modifier.testTag("performance_boost_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GamingAmber,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                // Switch 3: iQOO & iPhone 200% Touch Ultra Mode
                var is200TouchActive by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (is200TouchActive) Color(0x1400E676) else Color(0x0AFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (is200TouchActive) Color(0x3300E676) else Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "200% Touch Ultra",
                                tint = if (is200TouchActive) GamingGreen else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "iQOO & iPhone 200% Touch",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (is200TouchActive) Color(0x3300E676) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = if (is200TouchActive) "200% SENSI" else "OFF",
                                        color = if (is200TouchActive) GamingGreen else TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "LSQ2 velocity tracker & 7/7 speed. Feels like 200% in Free Fire even at 0",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = is200TouchActive,
                        onCheckedChange = { active ->
                            is200TouchActive = active
                            coroutineScope.launch {
                                val engine = SensitivityEngine(context)
                                if (active) {
                                    val res = engine.applyIphoneIqooUltraMode()
                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                } else {
                                    engine.restoreOriginal()
                                    Toast.makeText(context, "Touch sensitivity restored to 1.0X", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GamingGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GAME ASSISTANT TOOLBELT (Deep Clean, DND Focus Mode, Screenshot, Screen Record)
        val focusModeManager = remember { GameFocusModeManager(context) }
        val screenCaptureManager = remember { ScreenCaptureManager(context) }
        val systemCleaner = remember { SystemCleaner(context) }
        val dualSimManager = remember { DualSimManager(context) }
        val networkBooster = remember { NetworkBooster(context) }

        val focusState by focusModeManager.focusState.collectAsState()
        val captureState by screenCaptureManager.captureState.collectAsState()
        val isRecording = captureState.isRecording
        val recordDuration = captureState.recordingDurationSec
        var cleanStatus by remember { mutableStateOf<String?>(null) }
        var isCleaning by remember { mutableStateOf(false) }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_assistant_toolbelt_card"),
            glowAccent = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2200E676)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GamingGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "GAME ASSISTANT TOOLBELT",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        color = Color(0x1A00E676),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0x3300E676))
                    ) {
                        Text(
                            text = "PRO FF SUITE",
                            color = GamingGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Focus Mode Total Silence Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFocusActive) Color(0x22FF5252) else Color(0x0AFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isFocusActive) Color(0x33FF5252) else Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFocusActive) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isFocusActive) GamingCrimson else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Game Focus Mode (Total Silence)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isFocusActive) {
                                    Surface(
                                        color = Color(0x33FF5252),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "ZERO INTERRUPTIONS",
                                            color = GamingCrimson,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Blocks calls, heads-up notifications & vibration until disabled",
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    Switch(
                        checked = focusState.isFocusModeActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val ok = focusModeManager.enableFocusMode()
                                if (!ok && !focusModeManager.hasDndPermission()) {
                                    focusModeManager.openDndSettings()
                                }
                            } else {
                                focusModeManager.disableFocusMode()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GamingCrimson,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                // Quick Action Buttons Grid (Clean All, Screenshot, Screen Record)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Deep Clean Button
                    Button(
                        onClick = {
                            if (!isCleaning) {
                                isCleaning = true
                                coroutineScope.launch {
                                    val res = systemCleaner.cleanAll()
                                    cleanStatus = "Freed ${res.freedRamMb}MB (${res.killedProcessesCount} tasks killed)"
                                    isCleaning = false
                                    Toast.makeText(context, cleanStatus ?: "Cleaned!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2200E5FF)),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCleaning) "CLEANING..." else "CLEAN ALL",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Screenshot Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val path = screenCaptureManager.captureScreenshot()
                                if (path != null) {
                                    Toast.makeText(context, "Screenshot saved to Gallery!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Screenshot requires Shizuku/Root authorization", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF9100)),
                        border = BorderStroke(1.dp, GamingAmber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GamingAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SCREENSHOT", color = GamingAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Screen Record Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (isRecording) {
                                    val path = screenCaptureManager.stopRecording()
                                    if (path != null) {
                                        Toast.makeText(context, "Saved recording to Movies!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val started = screenCaptureManager.startRecording()
                                    if (!started) {
                                        Toast.makeText(context, "Recording requires Shizuku/Root authorization", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0x33FF5252) else Color(0x22101726)
                        ),
                        border = BorderStroke(1.dp, if (isRecording) GamingCrimson else Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isRecording) GamingCrimson else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRecording) "${recordDuration}s" else "RECORD",
                            color = if (isRecording) GamingCrimson else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 360° NETWORK RADAR & SMART DUAL SIM PROMO CARD
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenRadar() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2200E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "360° SIGNAL RADAR & DUAL SIM",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0x2200E5FF),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "360° HUD",
                                    color = NeonCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Find tower direction, switch fastest SIM & ping SEA gaming cluster",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                }

                Button(
                    onClick = { onOpenRadar() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("SCAN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real Telemetry Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME HARDWARE TELEMETRY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "NEVER FABRICATED",
                color = GamingGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hardware Telemetry Cards Grid (including Frame Time)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val cardWidth = Modifier.weight(1f)

            // FPS
            DashboardTelemetryCard(
                label = "FPS",
                value = metrics.fpsDisplay,
                unit = if (metrics.fps != null) "FPS" else "",
                accentColor = if ((metrics.fps ?: 0) >= 55) GamingGreen else GamingAmber,
                description = "Choreographer rate",
                modifier = cardWidth
            )

            // FRAME TIME
            DashboardTelemetryCard(
                label = "FRAME TIME",
                value = metrics.frameTimeDisplay,
                unit = "",
                accentColor = Color(0xFF80D8FF),
                description = "Vsync render interval",
                modifier = cardWidth
            )

            // DISPLAY Hz
            DashboardTelemetryCard(
                label = "DISPLAY",
                value = "${metrics.displayHz}",
                unit = "Hz",
                accentColor = NeonCyan,
                description = "Panel refresh rate",
                modifier = cardWidth
            )

            // CPU
            DashboardTelemetryCard(
                label = "CPU",
                value = metrics.cpuUsageDisplay,
                unit = "",
                accentColor = Color(0xFF00E5FF),
                description = "${metrics.cpuCores} active cores",
                modifier = cardWidth
            )

            // CPU CLOCK
            DashboardTelemetryCard(
                label = "CPU CLOCK",
                value = metrics.cpuClockDisplay,
                unit = "",
                accentColor = Color(0xFFB388FF),
                description = "Kernel cpufreq",
                modifier = cardWidth
            )

            // RAM
            DashboardTelemetryCard(
                label = "RAM",
                value = metrics.ramDisplay,
                unit = "",
                accentColor = Color(0xFF64FFDA),
                description = "${metrics.ramPercent}% used",
                modifier = cardWidth
            )

            // TEMPERATURE
            DashboardTelemetryCard(
                label = "TEMPERATURE",
                value = metrics.batteryTempDisplay,
                unit = "",
                accentColor = if ((metrics.batteryTempC ?: 0f) > 42f) GamingCrimson else Color(0xFFFFD180),
                description = "Thermal: ${metrics.thermalStatus}",
                modifier = cardWidth
            )

            // BATTERY
            DashboardTelemetryCard(
                label = "BATTERY",
                value = "${metrics.batteryPercent}%",
                unit = if (metrics.isCharging) "CHARGING ⚡" else "DISCHARGING",
                accentColor = if (metrics.isCharging) GamingGreen else Color(0xFFB0BEC5),
                description = "System battery status",
                modifier = cardWidth
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Overlay Controller Card
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
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Overlay",
                        tint = if (isOverlayServiceActive) GamingGreen else TextMuted
                    )
                    Column {
                        Text(
                            text = "GAMING HUD OVERLAY",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isOverlayServiceActive) "STATUS: ACTIVE ON SCREEN" else "STATUS: STOPPED",
                            color = if (isOverlayServiceActive) GamingGreen else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (isOverlayServiceActive) {
                    Button(
                        onClick = onStopOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("stop_overlay_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = GamingCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", color = GamingCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { checkOverlayPermissionAndStart() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("start_overlay_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SENSITIVITY BOOST (Real Device-Level Input Response Multiplier Engine)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val sensitivityEngine = remember { SensitivityEngine(context) }
            var isApplyingTouch by remember { mutableStateOf(false) }
            var sensitivityResult by remember { mutableStateOf<SensitivityBoostResult?>(null) }
            val shizukuAuth = ShizukuManager.isAuthorized()

            val currentRequestedMult = SensitivityLevel.getMultiplierForPercent(profile.sensitivityPercent)
            val requestedMultStr = String.format(java.util.Locale.US, "%.1fX", currentRequestedMult)
            val actualSupportedMultStr = sensitivityResult?.let {
                String.format(java.util.Locale.US, "%.1fX", it.actualSupportedMultiplier)
            } ?: requestedMultStr

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SENSITIVITY BOOST",
                        color = TextPrimary,
                        fontSize = if (isCompactScreen) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Current: ${profile.sensitivityPercent}% (${profile.sensitivityLevel.displayName} • $requestedMultStr)",
                        color = NeonCyan,
                        fontSize = if (isCompactScreen) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onOpenTouchTest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TOUCH TEST", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multiplier Comparison Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0x1800E5FF),
                border = BorderStroke(0.6.dp, Color(0x3300E5FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Phone Default", color = Color(0xFF90A4AE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("1.0X", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Boost", color = Color(0xFF90A4AE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("$requestedMultStr req", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Actual Supported", color = Color(0xFF90A4AE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = actualSupportedMultStr,
                            color = if (sensitivityResult?.supportStatus == SensitivitySupportStatus.PARTIALLY_SUPPORTED) GamingAmber else GamingGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Master Slider (0% - 100%)
            Slider(
                value = profile.sensitivityPercent.toFloat(),
                onValueChange = { newVal ->
                    val pct = newVal.toInt()
                    onSensitivityPercentSelected(pct)
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0x3300E5FF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4 Preset Buttons: LOW (0% / 1.0X), MEDIUM (50% / 2.0X), HIGH (75% / 3.0X), ULTRA HIGH (100% / 5.0X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensitivityLevel.values().forEach { lvl ->
                    val isSelected = profile.sensitivityPercent == lvl.targetPercent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan else Color(0x221E293B))
                            .border(1.dp, if (isSelected) NeonCyan else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable {
                                onSensitivityLevelSelected(lvl)
                                onSensitivityPercentSelected(lvl.targetPercent)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lvl.displayName,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = if (isCompactScreen) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${lvl.targetPercent}% (${lvl.multiplierLabel})",
                                color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = if (isCompactScreen) 7.5.sp else 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shizuku / System Status Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0x1800E5FF),
                border = BorderStroke(0.6.dp, Color(0x3300E5FF))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Shizuku:",
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (shizukuAuth) "Connected" else "Unauthorized",
                                color = if (shizukuAuth) GamingGreen else GamingAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Sensitivity:",
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile.sensitivityLevel.displayName,
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    if (!shizukuAuth) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Shizuku permission required for supported system-level sensitivity controls.",
                            color = GamingAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons: [ APPLY SENSITIVITY ], [ RESTORE SENSITIVITY ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isApplyingTouch = true
                                    val res = sensitivityEngine.applySensitivity(profile.sensitivityPercent)
                                    sensitivityResult = res
                                    isApplyingTouch = false
                                }
                            },
                            enabled = !isApplyingTouch,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text(
                                text = if (isApplyingTouch) "VERIFYING..." else "APPLY SENSITIVITY",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isApplyingTouch = true
                                    val restore = sensitivityEngine.restoreOriginal()
                                    sensitivityResult = SensitivityBoostResult(
                                        isSuccess = restore.success,
                                        appliedPercent = 0,
                                        level = SensitivityLevel.LOW,
                                        requestedMultiplier = 1.0f,
                                        actualSupportedMultiplier = 1.0f,
                                        supportStatus = SensitivitySupportStatus.RESTORED,
                                        message = restore.message,
                                        verifiedSettings = restore.restoredSettings,
                                        unsupportedSettings = emptyList(),
                                        failedSettings = emptyList()
                                    )
                                    onSensitivityPercentSelected(0)
                                    onSensitivityLevelSelected(SensitivityLevel.LOW)
                                    isApplyingTouch = false
                                }
                            },
                            enabled = !isApplyingTouch,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x66FF1744)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "RESTORE",
                                color = GamingCrimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Detailed Status & Verified System Changes
                    sensitivityResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.message,
                            color = if (result.isSuccess) GamingGreen else GamingCrimson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (result.verifiedSettings.isNotEmpty()) {
                            Text(
                                text = "System changes: Verified (${result.verifiedSettings.size} parameters active)",
                                color = GamingGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (result.unsupportedSettings.isNotEmpty()) {
                            Text(
                                text = "Unsupported: ${result.unsupportedSettings.joinToString(", ")}",
                                color = GamingAmber,
                                fontSize = 8.5.sp
                            )
                        }
                        if (result.failedSettings.isNotEmpty()) {
                            Text(
                                text = "Failed: ${result.failedSettings.joinToString(", ")}",
                                color = GamingCrimson,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Legitimate device-level input responsiveness via Shizuku & Android system settings",
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RAM BOOST & MEMORY OPTIMIZATION Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RAM BOOSTER & PROCESS KILLER",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Deep Background Memory Cleaning & Kernel Cache Flush",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isOptimizingRam = true
                            val result = RamBooster.performBoost(context)
                            ramBoostReport = result
                            isOptimizingRam = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GamingGreen),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isOptimizingRam,
                    modifier = Modifier.testTag("ram_boost_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOptimizingRam) "CLEANING RAM..." else "BOOST RAM NOW",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Report if available
            ramBoostReport?.let { report ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3300E676))
                        .border(0.5.dp, GamingGreen, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAM BOOST COMPLETE! +${report.freedMb} MB FREED ⚡",
                                color = GamingGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (report.summary.contains("Kernel")) {
                                Text(
                                    text = "KERNEL CACHE DROPPED",
                                    color = NeonCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Available RAM: ${report.beforeAvailMb} MB ➔ ${report.afterAvailMb} MB (Stopped ${report.killedCount} background processes)",
                            color = TextPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GAME BOOST Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GAME BOOST",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.isGameBoostActive) {
                            Text(
                                text = "● ACTIVE",
                                color = GamingAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = "Priority overlay, high refresh mode & touch responsiveness",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Switch(
                    checked = profile.isGameBoostActive,
                    onCheckedChange = { active ->
                        onToggleGameBoost(active)
                        if (active) {
                            checkOverlayPermissionAndStart()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GamingAmber
                    ),
                    modifier = Modifier.testTag("game_boost_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PERMISSION CENTER (Section 10)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Security", tint = NeonCyan)
                    Text(
                        text = "PERMISSION CENTER",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = { refreshPermissions() }) {
                    Text("REFRESH", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Permission 1: Overlay Permission
            PermissionCenterRow(
                title = "Overlay Permission",
                description = "Required to display floating FPS panel over games",
                isGranted = hasOverlayPermission,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission 2: Notifications
            PermissionCenterRow(
                title = "Notification Permission",
                description = "Required to keep foreground telemetry service alive",
                isGranted = hasNotificationPermission,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission 3: Usage Access
            PermissionCenterRow(
                title = "Usage Access",
                description = "Allows detecting active game without root",
                isGranted = hasUsagePermission,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission 4: Shizuku Authorization
            PermissionCenterRow(
                title = "Shizuku Privileged Access",
                description = "Unlocks system pointer speed and battery telemetry",
                isGranted = isShizukuAuthorized,
                onGrantClick = onOpenShizuku
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Permission Dialog for Display Over Other Apps
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = "OVERLAY PERMISSION REQUIRED",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "SRT X CHEATS needs Android's Display Over Other Apps permission to display the FPS panel over your selected game.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("GRANT PERMISSION", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF0F1523)
        )
    }

    // Game Not Detected Dialog with options to Pick App, Open Play Store, or Start Overlay
    if (showGameNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { showGameNotFoundDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GamingAmber)
                    Text(
                        text = "GAME NOT DETECTED",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SRT X CHEATS could not locate '${profile.appName}' (${profile.packageName}) on this device.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• If you have Free Fire or Free Fire MAX under another name, tap 'Choose Installed Game'.\n• Or start the FPS overlay directly and switch to your game from the home screen.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGameNotFoundDialog = false
                        onSelectGameClicked()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("CHOOSE INSTALLED GAME", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            showGameNotFoundDialog = false
                            checkOverlayPermissionAndStart()
                            Toast.makeText(context, "FPS Overlay activated! Switch to your game.", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("START OVERLAY", color = GamingGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            showGameNotFoundDialog = false
                            val target = if (profile.packageName.contains("freefire")) profile.packageName else "com.dts.freefireth"
                            GameDetector.openInPlayStore(context, target)
                        }
                    ) {
                        Text("PLAY STORE", color = GamingAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = Color(0xFF0F1523)
        )
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun PermissionCenterRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x22101726))
            .border(0.5.dp, if (isGranted) GamingGreen.copy(alpha = 0.4f) else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isGranted) "✓ GRANTED" else "✕ NOT GRANTED",
                    color = if (isGranted) GamingGreen else Color(0xFFFF5252),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = description,
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        if (!isGranted) {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("GRANT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DashboardTelemetryCard(
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33101726))
            .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    color = accentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}
