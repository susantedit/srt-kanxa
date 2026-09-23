package com.srtxcheats.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.core.DualSimManager
import com.srtxcheats.core.NetworkBooster
import com.srtxcheats.core.ShizukuManager
import com.srtxcheats.core.SignalRadarScanner
import com.srtxcheats.core.SimSlotInfo
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NetworkRadarScreen(
    dualSimManager: DualSimManager,
    radarScanner: SignalRadarScanner,
    networkBooster: NetworkBooster,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val simState by dualSimManager.simState.collectAsState()
    val radarState by radarScanner.radarState.collectAsState()
    val netStatus by networkBooster.networkState.collectAsState()

    var isOptimizingTcp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dualSimManager.queryDualSimStatus()
        networkBooster.sampleNetworkMetrics()
    }

    DisposableEffect(Unit) {
        onDispose {
            radarScanner.stopScan()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ==============================================================
        // 1. HEADER: NETWORK RADAR & SMART DUAL SIM
        // ==============================================================
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
                        .background(Color(0x3300E5FF))
                        .border(1.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = "Radar",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "360° NETWORK RADAR",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SIGNAL COMPASS & DUAL SIM SWITCH",
                        color = NeonCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Quick Refresh Button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        coroutineScope.launch {
                            dualSimManager.queryDualSimStatus()
                            networkBooster.sampleNetworkMetrics()
                            Toast.makeText(context, "Network metrics refreshed", Toast.LENGTH_SHORT).show()
                        }
                    },
                color = Color(0x22101726),
                border = BorderStroke(0.6.dp, Color(0x3300E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Text("REFRESH", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==============================================================
        // 2. 360° SIGNAL RADAR HUD COMPASS
        // ==============================================================
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowAccent = radarState.isScanning
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "360° CELLULAR & WI-FI SIGNAL SCANNER",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rotate device 360° to find strongest signal alignment",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }

                    if (radarState.isScanning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x3300E676),
                            border = BorderStroke(0.8.dp, GamingGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GamingGreen)
                                )
                                Text("SCANNING", color = GamingGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Radar Canvas HUD
                RadarCanvasHUD(
                    headingDeg = radarState.currentHeadingDeg,
                    bestHeadingDeg = radarState.bestHeadingDeg,
                    currentDbm = radarState.currentSignalDbm,
                    bestDbm = radarState.bestSignalDbm,
                    sectors = radarState.sectors,
                    isScanning = radarState.isScanning,
                    modifier = Modifier.size(230.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recommendation Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x2200E5FF),
                    border = BorderStroke(0.7.dp, Color(0x5500E5FF))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CellTower, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "SIGNAL ALIGNMENT RECOMMENDATION",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = radarState.recommendationText,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (radarState.isScanning) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { radarState.scanProgressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = NeonCyan,
                                trackColor = Color(0x22FFFFFF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start / Stop Radar Scan Button
                Button(
                    onClick = {
                        if (radarState.isScanning) {
                            radarScanner.stopScan()
                        } else {
                            radarScanner.startScan()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (radarState.isScanning) Color(0xFFFF1744) else NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (radarState.isScanning) Icons.Default.NetworkCheck else Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (radarState.isScanning) "STOP 360° RADAR SCAN" else "START 360° SIGNAL RADAR SCAN",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==============================================================
        // 3. SMART DUAL SIM SWITCH PANEL
        // ==============================================================
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            Icon(Icons.Default.SimCard, contentDescription = "Dual SIM", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "SMART DUAL SIM SWITCH",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (simState.isDualSimActive) "Dual SIM detected (${simState.simCount} slots active)" else "Single SIM / Carrier active",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }

                    Button(
                        onClick = {
                            dualSimManager.openMobileDataSettings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        border = BorderStroke(0.6.dp, NeonCyan),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SWITCH SIM", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SIM Slot Cards
                if (simState.simList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        simState.simList.forEach { sim ->
                            SimSlotCard(
                                sim = sim,
                                isRecommended = simState.recommendedSlot == sim.slotIndex,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x18101726),
                        border = BorderStroke(0.5.dp, Color(0x22FFFFFF))
                    ) {
                        Text(
                            text = "No SIM cards detected or READ_PHONE_STATE permission pending.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Dual SIM AI Recommendation box
                if (simState.isDualSimActive && simState.recommendationReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x2200E676),
                        border = BorderStroke(0.6.dp, GamingGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GamingGreen, modifier = Modifier.size(15.dp))
                            Text(
                                text = "STABILITY TIP: ${simState.recommendationReason}",
                                color = Color(0xFFE8F5E9),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==============================================================
        // 4. LOW-LATENCY WI-FI LOCK & RELAY PING TESTER
        // ==============================================================
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = GamingAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = "HARDWARE LOW-LATENCY WI-FI LOCK",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Locks Wi-Fi chip in high-perf mode to stop ping spikes",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }

                    Switch(
                        checked = netStatus.isLowLatencyActive,
                        onCheckedChange = { active ->
                            if (active) {
                                networkBooster.acquireLowLatencyLock()
                                Toast.makeText(context, "Hardware Low-Latency Wi-Fi Lock Active", Toast.LENGTH_SHORT).show()
                            } else {
                                networkBooster.releaseLowLatencyLock()
                                Toast.makeText(context, "Low-Latency Lock Released", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GamingAmber
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ping results across game relays
                Text(
                    text = "FREE FIRE & COMPETITIVE GAMING RELAY LATENCY",
                    color = TextMuted,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (netStatus.pingResults.isNotEmpty()) {
                    netStatus.pingResults.forEach { ping ->
                        PingMetricRow(ping = ping)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                networkBooster.sampleNetworkMetrics()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33101726)),
                        border = BorderStroke(0.6.dp, NeonCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TEST LIVE RELAY LATENCY", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TCP Gaming Buffer Optimization (Privileged via Shizuku/Root)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x22101726),
                    border = BorderStroke(0.5.dp, Color(0x3300E5FF))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TCP BUFFER LOW-LATENCY TWEAKS",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Optimizes kernel TCP socket buffers and scaling flags",
                                color = TextMuted,
                                fontSize = 8.5.sp
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isOptimizingTcp = true
                                    if (ShizukuManager.isAuthorized()) {
                                        ShizukuManager.executeCommand("sysctl -w net.ipv4.tcp_low_latency=1")
                                        ShizukuManager.executeCommand("sysctl -w net.ipv4.tcp_window_scaling=1")
                                        Toast.makeText(context, "TCP Kernel Buffers tuned for low latency!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Requires Shizuku or Root authorization", Toast.LENGTH_SHORT).show()
                                    }
                                    isOptimizingTcp = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(6.dp),
                            enabled = !isOptimizingTcp,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isOptimizingTcp) "TUNING..." else "APPLY TWEAKS",
                                color = Color.Black,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SimSlotCard(
    sim: SimSlotInfo,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isRecommended) Color(0x2200E5FF) else Color(0x18101726),
        border = BorderStroke(
            width = if (isRecommended) 1.2.dp else 0.6.dp,
            color = if (isRecommended) NeonCyan else Color(0x22FFFFFF)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SIM ${sim.slotIndex + 1}",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                if (sim.isDefaultData) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x3300E676),
                        border = BorderStroke(0.5.dp, GamingGreen)
                    ) {
                        Text(
                            text = "DATA",
                            color = GamingGreen,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sim.carrierName,
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = sim.networkType,
                color = TextSecondary,
                fontSize = 8.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Signal dBm readout
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${sim.signalDbm}",
                    color = when {
                        sim.signalDbm > -85 -> GamingGreen
                        sim.signalDbm > -105 -> GamingAmber
                        else -> Color(0xFFFF1744)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "dBm",
                    color = TextMuted,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            // Signal bar meter (0 to 4 bars)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (bar in 1..4) {
                    val isFilled = bar <= sim.signalLevel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isFilled) {
                                    if (sim.signalLevel >= 3) GamingGreen else GamingAmber
                                } else {
                                    Color(0x22FFFFFF)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun PingMetricRow(ping: com.srtxcheats.core.PingResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = Color(0x18101726),
        border = BorderStroke(0.5.dp, Color(0x15FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(ping.serverName, color = TextPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                Text(ping.host, color = TextMuted, fontSize = 8.sp)
            }

            val pingColor = when {
                ping.pingMs < 50 -> GamingGreen
                ping.pingMs < 100 -> GamingAmber
                else -> Color(0xFFFF1744)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(pingColor)
                )
                Text(
                    text = if (ping.isReachable) "${ping.pingMs} ms" else "TIMEOUT",
                    color = pingColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun RadarCanvasHUD(
    headingDeg: Float,
    bestHeadingDeg: Float,
    currentDbm: Int,
    bestDbm: Int,
    sectors: List<com.srtxcheats.core.RadarSector>,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - 10f

        // Outer concentric tactical rings
        drawCircle(
            color = Color(0x2200E5FF),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = Color(0x1800E5FF),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = Color(0x1200E5FF),
            radius = radius * 0.36f,
            center = center,
            style = Stroke(width = 1f)
        )

        // Crosshairs
        drawLine(Color(0x2000E5FF), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1f)
        drawLine(Color(0x2000E5FF), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1f)

        // 12 Sectors Signal Rendering
        sectors.forEach { sector ->
            val sectorColor = when {
                sector.sampleCount == 0 -> Color(0x0600E5FF)
                sector.averageSignalDbm > -80 -> Color(0x5500E676)
                sector.averageSignalDbm > -100 -> Color(0x55FFD600)
                else -> Color(0x55FF1744)
            }

            drawArc(
                color = sectorColor,
                startAngle = sector.startAngleDeg - 90f,
                sweepAngle = 30f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Outline for sector
            drawArc(
                color = if (sector.isStrongest) Color(0xFF00E5FF) else Color(0x1500E5FF),
                startAngle = sector.startAngleDeg - 90f,
                sweepAngle = 30f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = if (sector.isStrongest) 2.5f else 1f)
            )
        }

        // Radar Sweep Beam (animated when scanning)
        if (isScanning) {
            val sweepRad = Math.toRadians((sweepAngle - 90.0))
            val sweepEnd = Offset(
                center.x + (radius * cos(sweepRad)).toFloat(),
                center.y + (radius * sin(sweepRad)).toFloat()
            )
            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0x1100E5FF)),
                    center = center,
                    radius = radius
                ),
                start = center,
                end = sweepEnd,
                strokeWidth = 2.5f
            )
        }

        // Optimal Heading Reticle Pointer (Peak Signal Direction)
        if (bestDbm > -140) {
            val bestRad = Math.toRadians((bestHeadingDeg - 90.0))
            val targetPos = Offset(
                center.x + (radius * 0.88f * cos(bestRad)).toFloat(),
                center.y + (radius * 0.88f * sin(bestRad)).toFloat()
            )
            drawCircle(Color(0xFFFF1744), radius = 6f, center = targetPos)
            drawCircle(Color(0xFF00E5FF), radius = 10f, center = targetPos, style = Stroke(width = 2f))
        }

        // Device Azimuth Needle (Current Heading)
        rotate(degrees = headingDeg, pivot = center) {
            val needlePath = Path().apply {
                moveTo(center.x, center.y - radius + 4f)
                lineTo(center.x + 6f, center.y - radius + 18f)
                lineTo(center.x - 6f, center.y - radius + 18f)
                close()
            }
            drawPath(needlePath, color = Color(0xFF00E5FF))
        }

        // Center HUD Core
        drawCircle(Color(0xFF0A0F1D), radius = 26f, center = center)
        drawCircle(Color(0xFF00E5FF), radius = 26f, center = center, style = Stroke(width = 1.8f))
        drawCircle(Color(0xFF00E5FF), radius = 3.5f, center = center)
    }
}
