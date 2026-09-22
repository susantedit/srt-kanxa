package com.srtxcheats.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.model.GameProfile

@Composable
fun FireMacroOverlay(
    profile: GameProfile,
    onDragDelta: (Float, Float) -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onUpdateSize: (Int) -> Unit,
    onUpdateColor: (Long) -> Unit,
    onUpdateBoundaryRadius: (Float) -> Unit,
    onUpdateSensX: (Float) -> Unit,
    onUpdateSensY: (Float) -> Unit,
    onUpdateAlpha: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val macroColor = Color(profile.fireMacroColor)
    val alpha = profile.fireMacroAlpha.coerceIn(0.15f, 1.0f)

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Central Visual Target + Dashed Boundary Ring
        val targetSizeDp = profile.fireMacroSizeDp.dp
        val boundaryRadiusDp = profile.fireMacroBoundaryRadius.dp
        val canvasSizeDp = ((profile.fireMacroBoundaryRadius * 2) + 40).coerceAtLeast(80f).dp

        Box(
            modifier = Modifier
                .size(canvasSizeDp)
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(canvasSizeDp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val strokePx = 2.dp.toPx()
                val radiusPx = boundaryRadiusDp.toPx()

                // Dashed boundary ring
                drawCircle(
                    color = macroColor.copy(alpha = 0.75f),
                    radius = radiusPx,
                    center = center,
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )

                // Outer guide circle
                drawCircle(
                    color = macroColor.copy(alpha = 0.25f),
                    radius = radiusPx,
                    center = center
                )

                // Central Cross & Bullseye
                val innerR = (targetSizeDp.toPx() / 2f).coerceAtMost(radiusPx * 0.8f)
                drawCircle(
                    color = macroColor,
                    radius = innerR * 0.5f,
                    center = center,
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawCircle(
                    color = macroColor,
                    radius = 3.5.dp.toPx(),
                    center = center
                )
                // Cross lines extending slightly
                val crossLen = innerR * 0.85f
                drawLine(macroColor, Offset(center.x, center.y - crossLen), Offset(center.x, center.y + crossLen), strokeWidth = 2.dp.toPx())
                drawLine(macroColor, Offset(center.x - crossLen, center.y), Offset(center.x + crossLen, center.y), strokeWidth = 2.dp.toPx())
            }

            // Central icon / badge for quick interaction
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(macroColor.copy(alpha = 0.2f))
                    .border(1.dp, macroColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Target",
                    tint = macroColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Draggable Control Panel Window
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xEE090D18).copy(alpha = alpha),
            border = androidx.compose.foundation.BorderStroke(1.dp, macroColor.copy(alpha = 0.6f)),
            tonalElevation = 8.dp,
            modifier = Modifier.width(260.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Header / Drag bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag",
                            tint = macroColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "FIRE MACRO SENSI",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Master Toggle & Position X/Y Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "POS: X ${profile.fireMacroPosX} | Y ${profile.fireMacroPosY}",
                        color = Color(0xFF90A4AE),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Switch(
                        checked = profile.fireMacroEnabled,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = macroColor,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.size(34.dp, 20.dp)
                    )
                }

                // Expanded Controls
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp)
                    ) {
                        // 1. Boundary / Radius Size Slider
                        Text(
                            text = "BOUNDARY RADIUS: ${profile.fireMacroBoundaryRadius.toInt()} DP",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.fireMacroBoundaryRadius,
                            onValueChange = onUpdateBoundaryRadius,
                            valueRange = 25f..120f,
                            colors = SliderDefaults.colors(thumbColor = macroColor, activeTrackColor = macroColor),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        )

                        // 2. Macro Size Slider
                        Text(
                            text = "TARGET SIZE: ${profile.fireMacroSizeDp} DP",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.fireMacroSizeDp.toFloat(),
                            onValueChange = { onUpdateSize(it.toInt()) },
                            valueRange = 30f..100f,
                            colors = SliderDefaults.colors(thumbColor = macroColor, activeTrackColor = macroColor),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        )

                        // 3. Sensitivity Multiplier X & Y
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SENSI X: ${String.format("%.2f", profile.fireMacroSensX)}X",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = profile.fireMacroSensX,
                                    onValueChange = onUpdateSensX,
                                    valueRange = 0.2f..4.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)),
                                    modifier = Modifier.fillMaxWidth().height(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SENSI Y: ${String.format("%.2f", profile.fireMacroSensY)}X",
                                    color = Color(0xFFFF4081),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = profile.fireMacroSensY,
                                    onValueChange = onUpdateSensY,
                                    valueRange = 0.2f..4.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF4081), activeTrackColor = Color(0xFFFF4081)),
                                    modifier = Modifier.fillMaxWidth().height(28.dp)
                                )
                            }
                        }

                        // 4. Button Transparency Slider
                        Text(
                            text = "BUTTON TRANSPARENCY: ${(profile.fireMacroAlpha * 100).toInt()}%",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = profile.fireMacroAlpha,
                            onValueChange = onUpdateAlpha,
                            valueRange = 0.15f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = macroColor, activeTrackColor = macroColor),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        )

                        // 5. Macro Color Swatches
                        Text(
                            text = "MACRO COLOR",
                            color = Color(0xFFB0BEC5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0xFFFF1744 to "Red",
                                0xFF00E5FF to "Cyan",
                                0xFF00E676 to "Green",
                                0xFFFFEA00 to "Yellow",
                                0xFFFF6D00 to "Orange",
                                0xFFD500F9 to "Purple",
                                0xFFFFFFFF to "White",
                                0xFFFF4081 to "Pink"
                            ).forEach { (clr, _) ->
                                val isSelected = profile.fireMacroColor == clr
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(clr))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onUpdateColor(clr) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
