package com.srtxcheats.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary

data class TouchPointInfo(
    val id: Long,
    val x: Float,
    val y: Float,
    val pressure: Float,
    val history: List<Offset>
)

@Composable
fun TouchTestScreen() {
    val activePointers = remember { mutableStateMapOf<Long, TouchPointInfo>() }
    var touchEventCounter by remember { mutableStateOf(0L) }
    var maxSimultaneousTouches by remember { mutableStateOf(0) }

    val pointerColors = remember {
        listOf(
            Color(0xFF00E5FF), // Cyan
            Color(0xFFFF1744), // Crimson
            Color(0xFF00E676), // Green
            Color(0xFFFF9100), // Amber
            Color(0xFFB388FF), // Purple
            Color(0xFFFFD600), // Yellow
            Color(0xFF00B0FF), // Blue
            Color(0xFFFF4081), // Pink
            Color(0xFF1DE9B6), // Teal
            Color(0xFFFF6D00)  // Deep Orange
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .padding(16.dp)
            .testTag("touch_test_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MULTI-TOUCH & RESPONSE TEST",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Screen sampling visualizer & multi-finger tracking",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    activePointers.clear()
                    touchEventCounter = 0
                    maxSimultaneousTouches = 0
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFFF1744), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", color = Color(0xFFFF1744), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time telemetry bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0B101C))
                .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CURRENT TOUCHES", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${activePointers.size} POINTERS",
                    color = if (activePointers.isNotEmpty()) GamingGreen else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column {
                Text("MAX SIMULTANEOUS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$maxSimultaneousTouches FINGERS",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column {
                Text("EVENTS LOGGED", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$touchEventCounter",
                    color = GamingAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Touch Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF030508))
                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        touchEventCounter++

                        val downId = down.id.value
                        val initList = mutableListOf(down.position)
                        activePointers[downId] = TouchPointInfo(
                            id = downId,
                            x = down.position.x,
                            y = down.position.y,
                            pressure = down.pressure,
                            history = initList
                        )
                        if (activePointers.size > maxSimultaneousTouches) {
                            maxSimultaneousTouches = activePointers.size
                        }

                        do {
                            val event = awaitPointerEvent()
                            touchEventCounter++

                            // Update active pointers
                            val currentIds = event.changes.filter { it.pressed }.map { it.id.value }.toSet()
                            activePointers.keys.retainAll(currentIds)

                            for (change in event.changes) {
                                if (change.pressed) {
                                    val id = change.id.value
                                    val old = activePointers[id]
                                    val newHistory = (old?.history ?: emptyList()).takeLast(40) + change.position
                                    activePointers[id] = TouchPointInfo(
                                        id = id,
                                        x = change.position.x,
                                        y = change.position.y,
                                        pressure = change.pressure,
                                        history = newHistory
                                    )
                                }
                            }

                            if (activePointers.size > maxSimultaneousTouches) {
                                maxSimultaneousTouches = activePointers.size
                            }
                        } while (event.changes.any { it.pressed })

                        activePointers.clear()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridStep = 80f
                var gx = gridStep
                while (gx < w) {
                    drawLine(Color(0x0CFFFFFF), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                    gx += gridStep
                }
                var gy = gridStep
                while (gy < h) {
                    drawLine(Color(0x0CFFFFFF), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                    gy += gridStep
                }

                // Render each active pointer
                activePointers.values.forEachIndexed { index, pointer ->
                    val color = pointerColors[index % pointerColors.size]

                    // Draw motion trail
                    if (pointer.history.size > 1) {
                        val path = Path()
                        pointer.history.forEachIndexed { i, pt ->
                            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                        }
                        drawPath(path, color = color.copy(alpha = 0.6f), style = Stroke(width = 6f))
                    }

                    // Outer tracking circle (with pressure scaling)
                    val baseRadius = 38f
                    val outerRadius = baseRadius + (pointer.pressure * 20f).coerceIn(0f, 40f)
                    drawCircle(
                        color = color.copy(alpha = 0.25f),
                        radius = outerRadius,
                        center = Offset(pointer.x, pointer.y)
                    )
                    drawCircle(
                        color = color,
                        radius = outerRadius,
                        center = Offset(pointer.x, pointer.y),
                        style = Stroke(width = 2.5f)
                    )

                    // Center dot
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(pointer.x, pointer.y)
                    )

                    // Crosshair alignment lines
                    drawLine(
                        color = color.copy(alpha = 0.4f),
                        start = Offset(pointer.x - outerRadius - 15f, pointer.y),
                        end = Offset(pointer.x + outerRadius + 15f, pointer.y),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = color.copy(alpha = 0.4f),
                        start = Offset(pointer.x, pointer.y - outerRadius - 15f),
                        end = Offset(pointer.x, pointer.y + outerRadius + 15f),
                        strokeWidth = 1.5f
                    )
                }
            }

            if (activePointers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0x3300E5FF),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TOUCH OR DRAG MULTIPLE FINGERS HERE",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Measures hardware input multi-touch and touch latency",
                            color = Color(0x33FFFFFF),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
