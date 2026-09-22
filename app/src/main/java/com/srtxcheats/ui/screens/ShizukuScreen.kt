package com.srtxcheats.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.srtxcheats.service.ShizukuShellService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun ShizukuScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var shizukuState by remember { mutableStateOf(ShizukuManager.checkState(context)) }
    var isAuthorized by remember { mutableStateOf(ShizukuManager.isAuthorized()) }
    var deviceProps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var capabilities by remember { mutableStateOf<ShizukuManager.CapabilityCheck?>(null) }
    var testResultOutput by remember { mutableStateOf<String?>(null) }
    var isExecutingCommand by remember { mutableStateOf(false) }
    val shellState by ShizukuShellService.serviceState.collectAsState()
    var customShellCmd by remember { mutableStateOf("") }

    fun refreshStatus() {
        shizukuState = ShizukuManager.checkState(context)
        isAuthorized = ShizukuManager.isAuthorized()
        coroutineScope.launch {
            capabilities = ShizukuManager.detectCapabilities()
            if (isAuthorized) {
                deviceProps = ShizukuManager.readDeviceProperties()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("shizuku_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SHIZUKU & ADB CONTROL",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Privileged system telemetry & safe hardware tuning",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = { refreshStatus() },
                modifier = Modifier.height(36.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(NeonCyan, Color(0x3300E5FF)))
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REFRESH", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shizuku Service Status Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowAccent = shizukuState == ShizukuManager.ShizukuState.CONNECTED && isAuthorized
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
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (shizukuState) {
                                    ShizukuManager.ShizukuState.CONNECTED -> GamingGreen
                                    ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> GamingAmber
                                    ShizukuManager.ShizukuState.NOT_INSTALLED -> GamingCrimson
                                }
                            )
                    )
                    Column {
                        Text(
                            text = "SHIZUKU SERVICE",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (shizukuState) {
                                ShizukuManager.ShizukuState.CONNECTED -> "RUNNING & BINDER CONNECTED"
                                ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> "INSTALLED (SERVICE NOT RUNNING)"
                                ShizukuManager.ShizukuState.NOT_INSTALLED -> "NOT INSTALLED"
                            },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action buttons based on state
                when (shizukuState) {
                    ShizukuManager.ShizukuState.NOT_INSTALLED -> {
                        Button(
                            onClick = { ShizukuManager.openShizukuWebsite(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("INSTALL", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> {
                        Button(
                            onClick = { ShizukuManager.openShizukuApp(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = GamingAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("OPEN SHIZUKU", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ShizukuManager.ShizukuState.CONNECTED -> {
                        if (!isAuthorized) {
                            Button(
                                onClick = { ShizukuManager.requestPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("AUTHORIZE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3300E676))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("GRANTED", color = GamingGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // REAL-TIME SHIZUKU SHELL SERVICE CONSOLE
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowAccent = shellState.isRunning
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (shellState.isRunning) GamingGreen else TextMuted)
                    )
                    Column {
                        Text(
                            text = "SHIZUKU SHELL ENGINE",
                            color = if (shellState.isRunning) NeonCyan else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (shellState.isRunning) {
                                "Running • ${shellState.sensitivityMultiplier}x Sensi • ${if (shellState.isTouchLatencyReduced) "Ultra-Low Latency" else "Standard"}"
                            } else {
                                "Service Inactive • Tap START for live ADB shell tuning"
                            },
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        if (shellState.isRunning) {
                            ShizukuShellService.stop(context)
                        } else {
                            ShizukuShellService.start(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (shellState.isRunning) GamingCrimson else GamingGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (shellState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (shellState.isRunning) "STOP" else "START",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // REAL-TIME SENSITIVITY MULTIPLIERS (1.0x to 8.0x)
            Text(
                text = "REAL-TIME SENSITIVITY MULTIPLIER (SHELL/ADB)",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val multipliers = listOf(1.0f, 1.5f, 2.0f, 3.0f, 5.0f, 8.0f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                multipliers.forEach { mult ->
                    val isSelected = shellState.sensitivityMultiplier == mult
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonCyan else Color(0x221E293B))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonCyan else Color(0x44334155),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (!shellState.isRunning) {
                                    ShizukuShellService.start(context)
                                }
                                ShizukuShellService.setMultiplier(context, mult)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mult}x",
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SYSTEM TOUCH LATENCY REDUCTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x330F172A))
                    .border(0.6.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SYSTEM TOUCH LATENCY REDUCTION",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "100ms timeout • 120Hz lock • Glove touch • lsq2 tracking",
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }

                        Switch(
                            checked = shellState.isTouchLatencyReduced,
                            onCheckedChange = { enabled ->
                                if (!shellState.isRunning) {
                                    ShizukuShellService.start(context)
                                }
                                ShizukuShellService.setTouchLatencyReduction(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0x44334155)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player Mode Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!shellState.isRunning) {
                                    ShizukuShellService.start(context)
                                }
                                ShizukuShellService.setDragMode(context, !shellState.isDragOptimizationActive)
                            },
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    listOf(
                                        if (shellState.isDragOptimizationActive) NeonCyan else Color(0x44334155),
                                        if (shellState.isDragOptimizationActive) NeonCyan else Color(0x44334155)
                                    )
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (shellState.isDragOptimizationActive) Color(0x3300E5FF) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = if (shellState.isDragOptimizationActive) "DRAG ACTIVE 🔥" else "DRAG HEADSHOT",
                                color = if (shellState.isDragOptimizationActive) NeonCyan else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (!shellState.isRunning) {
                                    ShizukuShellService.start(context)
                                }
                                ShizukuShellService.setFreestyleMode(context, !shellState.isFreestyleActive)
                            },
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    listOf(
                                        if (shellState.isFreestyleActive) GamingAmber else Color(0x44334155),
                                        if (shellState.isFreestyleActive) GamingAmber else Color(0x44334155)
                                    )
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (shellState.isFreestyleActive) Color(0x33FF9100) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = if (shellState.isFreestyleActive) "800% ACTIVE 🌀" else "FREESTYLE 360",
                                color = if (shellState.isFreestyleActive) GamingAmber else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CUSTOM SHELL COMMAND EXECUTION
            Text(
                text = "EXECUTE CUSTOM SHELL COMMAND (SHIZUKU/ADB)",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = customShellCmd,
                    onValueChange = { customShellCmd = it },
                    placeholder = { Text("e.g. settings get system pointer_speed", color = TextMuted, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x44334155),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )

                Button(
                    onClick = {
                        if (customShellCmd.isNotBlank()) {
                            if (!shellState.isRunning) {
                                ShizukuShellService.start(context)
                            }
                            ShizukuShellService.executeCommand(context, customShellCmd)
                            customShellCmd = ""
                        }
                    },
                    modifier = Modifier.height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RUN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // REAL-TIME SHELL EXECUTION LOG CONSOLE
            if (shellState.recentLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "REAL-TIME SHELL EXECUTION LOG",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF06090F))
                        .border(0.6.dp, Color(0x33334155), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        shellState.recentLogs.forEach { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.contains("⚡") || logLine.contains("🎯")) NeonCyan else if (logLine.contains("🌀")) GamingAmber else TextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SHIZUKU CAPABILITY CHECK BOX
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SHIZUKU CAPABILITY CHECK",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isAuthorized) "REAL TIME" else "AWAITING BINDER",
                    color = if (isAuthorized) GamingGreen else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val cap = capabilities
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x400C1322))
                    .border(0.6.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CapabilityRow(
                    title = "SHIZUKU",
                    status = when (shizukuState) {
                        ShizukuManager.ShizukuState.CONNECTED -> "CONNECTED"
                        ShizukuManager.ShizukuState.SERVICE_NOT_RUNNING -> "NOT RUNNING"
                        ShizukuManager.ShizukuState.NOT_INSTALLED -> "NOT INSTALLED"
                    },
                    isSuccess = shizukuState == ShizukuManager.ShizukuState.CONNECTED
                )

                CapabilityRow(
                    title = "PERFORMANCE API",
                    status = if (cap?.performanceApiAvailable == true) "AVAILABLE" else "NOT AVAILABLE",
                    isSuccess = cap?.performanceApiAvailable == true
                )

                CapabilityRow(
                    title = "GAME MODE",
                    status = if (cap?.gameModeAvailable == true) "AVAILABLE" else "NOT AVAILABLE",
                    isSuccess = cap?.gameModeAvailable == true
                )

                CapabilityRow(
                    title = "SUPPORTED SYSTEM OPTIMIZATION",
                    status = if (cap?.systemOptAvailable == true) "AVAILABLE" else "NOT AVAILABLE",
                    isSuccess = cap?.systemOptAvailable == true
                )

                CapabilityRow(
                    title = "UNSUPPORTED FEATURES",
                    status = "${cap?.unsupportedCount ?: 2} RESTRICTED",
                    isSuccess = true,
                    overrideColor = Color(0xFFFF9100)
                )

                cap?.unsupportedDetails?.forEach { detail ->
                    Text(
                        text = "• $detail",
                        color = Color(0xFFB0BEC5),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safe Privileged System Telemetry (if authorized)
        if (isAuthorized && deviceProps.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PRIVILEGED SYSTEM PROPERTIES (SHIZUKU)",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                deviceProps.forEach { (prop, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(prop, color = TextSecondary, fontSize = 11.sp)
                        Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Safe Tuning & Diagnostics Action
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SAFE DEVICE OPTIMIZATION EXECUTION",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tests safe system property reads and verification. Never crashes if restricted.",
                color = TextSecondary,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isExecutingCommand = true
                            val res = ShizukuManager.executeCommand("dumpsys battery | grep -i temperature")
                            testResultOutput = if (res.exitCode == 0 && res.output.isNotBlank()) {
                                "Output: ${res.output}"
                            } else {
                                "Result: Operation not supported on this device or restricted."
                            }
                            isExecutingCommand = false
                        }
                    },
                    enabled = isAuthorized && !isExecutingCommand,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DIAGNOSE BATTERY", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isExecutingCommand = true
                            val res = ShizukuManager.executeCommand("settings get system pointer_speed")
                            testResultOutput = if (res.exitCode == 0 && res.output.isNotBlank()) {
                                "Pointer speed setting: ${res.output} (Normal range: 0-7)"
                            } else {
                                "Result: Operation not supported on this device"
                            }
                            isExecutingCommand = false
                        }
                    },
                    enabled = isAuthorized && !isExecutingCommand,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E676)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("READ POINTER SPEED", color = GamingGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            testResultOutput?.let { out ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06090F))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(out, color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step-by-step Wireless Debugging Guide
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "WIRELESS DEBUGGING SETUP GUIDE",
                color = GamingAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val steps = listOf(
                "1. Enable Developer Options on your device (Settings > About Phone > Tap 'Build Number' 7 times)",
                "2. In Developer Options, enable 'Wireless Debugging' (ensure connected to Wi-Fi)",
                "3. Open Shizuku and tap 'Pairing' under Wireless Debugging",
                "4. Enter the 6-digit Wi-Fi pairing code displayed by Android",
                "5. Tap 'Start' in Shizuku",
                "6. Return to SRT X CHEATS and tap [AUTHORIZE]"
            )

            steps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .padding(top = 4.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Text(step, color = TextSecondary, fontSize = 10.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF9100)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = GamingAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DEV OPTIONS", color = GamingAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { ShizukuManager.openShizukuApp(context) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN SHIZUKU", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CapabilityRow(
    title: String,
    status: String,
    isSuccess: Boolean,
    overrideColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFFCFD8DC),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        val color = overrideColor ?: if (isSuccess) GamingGreen else Color(0xFFFF5252)
        Text(
            text = "${if (isSuccess && overrideColor == null) "✓ " else ""}$status",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
