package com.srtxcheats.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object MarkerConstants {
    val MARKER_STYLES = listOf(
        "FF_DRAG_HEADSHOT" to "🎯 FF Drag Headshot Aim",
        "FF_SNIPER_AWM" to "🔭 FF AWM Quick-Scope",
        "FF_DYNAMIC_SPREAD" to "💥 FF Shotgun Spread",
        "FF_CYBER_ASSIST" to "⚡ FF Cyber Assist",
        "CROSS" to "Classic Cross",
        "DOT" to "Center Dot",
        "CIRCLE" to "Precision Circle",
        "RETICLE" to "Tactical Reticle",
        "CHEVRON" to "Chevron V",
        "DIAMOND" to "Diamond",
        "T_SHAPE" to "T-Bar Cross",
        "BULLSEYE" to "Bullseye Rings",
        "BOX" to "Square Box",
        "TRIANGLE" to "Delta Triangle",
        "PLUS" to "Cross Plus",
        "STAR" to "Star Aim",
        "TARGET_DOT" to "Target Dot",
        "QUAD_LINE" to "Quad Line",
        "TRIANGLE_DOT" to "Triangle Pip",
        "HEXAGON" to "Cyber Hexagon",
        "OCTAGON" to "Combat Octagon",
        "CROSSHAIR_DOT" to "Cross Dot",
        "PRECISION" to "Precision Hair",
        "SNIPER" to "Mil-Dot Sniper"
    )

    val MARKER_COLORS = listOf(
        0xFF00E5FF to "Cyan",
        0xFF00E676 to "Neon Green",
        0xFFFF1744 to "Electric Red",
        0xFFFFEA00 to "Bright Yellow",
        0xFFFF6D00 to "Orange Flame",
        0xFFD500F9 to "Neon Purple",
        0xFFFF4081 to "Vivid Pink",
        0xFFAEEA00 to "Lime Shock",
        0xFF00B0FF to "Deep Sky Blue",
        0xFFFFFFFF to "Pure White",
        0xFFFFAB00 to "Amber Gold",
        0xFF651FFF to "Electric Violet",
        0xFF69F0AE to "Mint Green",
        0xFFFF6E40 to "Sunset Coral",
        0xFFFFD700 to "Royal Gold",
        0xFFFF1493 to "Hot Pink",
        0xFF00F5D4 to "Aqua Marine",
        0xFFFF007F to "Magenta",
        0xFF40E0D0 to "Turquoise",
        0xFFDC143C to "Crimson"
    )
}

@Composable
fun ScreenMarkerOverlay(
    style: String = "CROSS",
    colorLong: Long = 0xFF00E5FF,
    sizeDp: Int = 24,
    offsetX: Int = 0,
    offsetY: Int = 0,
    rotateDeg: Float = 0f,
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val markerColor = Color(colorLong)
    val touchAreaSize = (sizeDp + 24).coerceAtLeast(44)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(touchAreaSize.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(sizeDp.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            rotate(degrees = rotateDeg, pivot = center) {
                when (style) {
                    "DOT" -> {
                        drawCircle(color = markerColor, radius = radius * 0.28f, center = center)
                    }
                    "CIRCLE" -> {
                        drawCircle(color = markerColor, radius = radius * 0.75f, center = center, style = Stroke(width = 2.2f))
                        drawCircle(color = markerColor, radius = 2.5f, center = center)
                    }
                    "RETICLE" -> {
                        drawCircle(color = markerColor.copy(alpha = 0.85f), radius = radius * 0.78f, center = center, style = Stroke(width = 1.8f))
                        drawCircle(color = markerColor, radius = 2.2f, center = center)
                        drawLine(markerColor, Offset(center.x, 0f), Offset(center.x, radius * 0.35f), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(center.x, size.height), Offset(center.x, size.height - radius * 0.35f), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(0f, center.y), Offset(radius * 0.35f, center.y), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(size.width, center.y), Offset(size.width - radius * 0.35f, center.y), strokeWidth = 2.2f)
                    }
                    "CHEVRON" -> {
                        val path = Path().apply {
                            moveTo(center.x - radius * 0.65f, center.y + radius * 0.45f)
                            lineTo(center.x, center.y - radius * 0.45f)
                            lineTo(center.x + radius * 0.65f, center.y + radius * 0.45f)
                        }
                        drawPath(path, color = markerColor, style = Stroke(width = 2.5f))
                        drawCircle(color = markerColor, radius = 2f, center = center)
                    }
                    "DIAMOND" -> {
                        val path = Path().apply {
                            moveTo(center.x, center.y - radius * 0.8f)
                            lineTo(center.x + radius * 0.8f, center.y)
                            lineTo(center.x, center.y + radius * 0.8f)
                            lineTo(center.x - radius * 0.8f, center.y)
                            close()
                        }
                        drawPath(path, color = markerColor, style = Stroke(width = 2.2f))
                        drawCircle(color = markerColor, radius = 2.2f, center = center)
                    }
                    "T_SHAPE" -> {
                        val gap = 4f
                        drawLine(markerColor, Offset(center.x, center.y + gap), Offset(center.x, size.height), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(center.x - gap, center.y), Offset(0f, center.y), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(center.x + gap, center.y), Offset(size.width, center.y), strokeWidth = 2.2f)
                        drawCircle(color = markerColor, radius = 1.8f, center = center)
                    }
                    "BULLSEYE" -> {
                        drawCircle(color = markerColor, radius = radius * 0.85f, center = center, style = Stroke(width = 1.8f))
                        drawCircle(color = markerColor, radius = radius * 0.45f, center = center, style = Stroke(width = 1.6f))
                        drawCircle(color = markerColor, radius = 2.8f, center = center)
                    }
                    "BOX" -> {
                        val inset = radius * 0.25f
                        drawRect(
                            color = markerColor,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            style = Stroke(width = 2.2f)
                        )
                        drawCircle(color = markerColor, radius = 2.2f, center = center)
                    }
                    "TRIANGLE" -> {
                        val path = Path().apply {
                            moveTo(center.x, center.y - radius * 0.85f)
                            lineTo(center.x + radius * 0.75f, center.y + radius * 0.7f)
                            lineTo(center.x - radius * 0.75f, center.y + radius * 0.7f)
                            close()
                        }
                        drawPath(path, color = markerColor, style = Stroke(width = 2.2f))
                    }
                    "PLUS" -> {
                        val gap = 6f
                        drawLine(markerColor, Offset(center.x, center.y - gap), Offset(center.x, 0f), strokeWidth = 2.4f)
                        drawLine(markerColor, Offset(center.x, center.y + gap), Offset(center.x, size.height), strokeWidth = 2.4f)
                        drawLine(markerColor, Offset(center.x - gap, center.y), Offset(0f, center.y), strokeWidth = 2.4f)
                        drawLine(markerColor, Offset(center.x + gap, center.y), Offset(size.width, center.y), strokeWidth = 2.4f)
                    }
                    "STAR" -> {
                        val len = radius * 0.85f
                        val diag = len * 0.55f
                        drawLine(markerColor, Offset(center.x, center.y - len), Offset(center.x, center.y + len), strokeWidth = 2f)
                        drawLine(markerColor, Offset(center.x - len, center.y), Offset(center.x + len, center.y), strokeWidth = 2f)
                        drawLine(markerColor, Offset(center.x - diag, center.y - diag), Offset(center.x + diag, center.y + diag), strokeWidth = 1.4f)
                        drawLine(markerColor, Offset(center.x - diag, center.y + diag), Offset(center.x + diag, center.y - diag), strokeWidth = 1.4f)
                    }
                    "TARGET_DOT" -> {
                        drawCircle(color = markerColor, radius = radius * 0.7f, center = center, style = Stroke(width = 2f))
                        drawCircle(color = markerColor, radius = radius * 0.28f, center = center)
                    }
                    "QUAD_LINE" -> {
                        val bracket = radius * 0.5f
                        drawLine(markerColor, Offset(0f, bracket), Offset(0f, 0f), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(0f, 0f), Offset(bracket, 0f), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(size.width - bracket, 0f), Offset(size.width, 0f), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(size.width, 0f), Offset(size.width, bracket), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(0f, size.height - bracket), Offset(0f, size.height), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(0f, size.height), Offset(bracket, size.height), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(size.width - bracket, size.height), Offset(size.width, size.height), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(size.width, size.height), Offset(size.width, size.height - bracket), strokeWidth = 2.2f)
                        drawCircle(color = markerColor, radius = 2f, center = center)
                    }
                    "TRIANGLE_DOT" -> {
                        val path = Path().apply {
                            moveTo(center.x, center.y - radius * 0.8f)
                            lineTo(center.x + radius * 0.7f, center.y + radius * 0.65f)
                            lineTo(center.x - radius * 0.7f, center.y + radius * 0.65f)
                            close()
                        }
                        drawPath(path, color = markerColor, style = Stroke(width = 2.2f))
                        drawCircle(color = markerColor, radius = 2.5f, center = center)
                    }
                    "HEXAGON" -> {
                        val hexPath = Path()
                        for (i in 0 until 6) {
                            val angle = Math.toRadians((i * 60.0) - 30.0)
                            val x = center.x + (radius * 0.82f * cos(angle)).toFloat()
                            val y = center.y + (radius * 0.82f * sin(angle)).toFloat()
                            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                        }
                        hexPath.close()
                        drawPath(hexPath, color = markerColor, style = Stroke(width = 2.2f))
                        drawCircle(color = markerColor, radius = 2.2f, center = center)
                    }
                    "OCTAGON" -> {
                        val octPath = Path()
                        for (i in 0 until 8) {
                            val angle = Math.toRadians((i * 45.0) - 22.5)
                            val x = center.x + (radius * 0.85f * cos(angle)).toFloat()
                            val y = center.y + (radius * 0.85f * sin(angle)).toFloat()
                            if (i == 0) octPath.moveTo(x, y) else octPath.lineTo(x, y)
                        }
                        octPath.close()
                        drawPath(octPath, color = markerColor, style = Stroke(width = 2.2f))
                        drawCircle(color = markerColor, radius = 2.2f, center = center)
                    }
                    "CROSSHAIR_DOT" -> {
                        drawLine(markerColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 2.2f)
                        drawLine(markerColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 2.2f)
                        drawCircle(color = markerColor, radius = 3.2f, center = center)
                    }
                    "PRECISION" -> {
                        val gap = radius * 0.4f
                        drawLine(markerColor, Offset(center.x, center.y - gap), Offset(center.x, 0f), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(center.x, center.y + gap), Offset(center.x, size.height), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(center.x - gap, center.y), Offset(0f, center.y), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(center.x + gap, center.y), Offset(size.width, center.y), strokeWidth = 1.6f)
                        drawCircle(color = markerColor, radius = 1.2f, center = center)
                    }
                    "FF_DRAG_HEADSHOT" -> {
                        // Free Fire Drag Headshot: Center precision dot, vertical drag flick ladder, and subtle brackets
                        drawCircle(color = markerColor, radius = 2.4f, center = center)
                        // Vertical flick trajectory ticks
                        val tickStep = radius * 0.28f
                        for (i in 1..3) {
                            val y = center.y - (i * tickStep)
                            val tickWidth = (4f - i) * 2.5f
                            drawLine(markerColor, Offset(center.x - tickWidth, y), Offset(center.x + tickWidth, y), strokeWidth = 1.8f)
                        }
                        // Horizontal stabilizers
                        drawLine(markerColor, Offset(center.x - radius * 0.7f, center.y), Offset(center.x - radius * 0.25f, center.y), strokeWidth = 2f)
                        drawLine(markerColor, Offset(center.x + radius * 0.25f, center.y), Offset(center.x + radius * 0.7f, center.y), strokeWidth = 2f)
                        // Lower stop mark
                        drawLine(markerColor, Offset(center.x, center.y + radius * 0.25f), Offset(center.x, center.y + radius * 0.6f), strokeWidth = 2f)
                    }
                    "FF_SNIPER_AWM" -> {
                        // Quick Scope Diamond with Mil-Dot lines
                        val path = Path().apply {
                            moveTo(center.x, center.y - radius * 0.65f)
                            lineTo(center.x + radius * 0.65f, center.y)
                            lineTo(center.x, center.y + radius * 0.65f)
                            lineTo(center.x - radius * 0.65f, center.y)
                            close()
                        }
                        drawPath(path, color = markerColor, style = Stroke(width = 1.8f))
                        drawCircle(color = markerColor, radius = 1.8f, center = center)
                        drawLine(markerColor, Offset(center.x, 0f), Offset(center.x, center.y - radius * 0.65f), strokeWidth = 1.5f)
                        drawLine(markerColor, Offset(center.x, center.y + radius * 0.65f), Offset(center.x, size.height), strokeWidth = 1.5f)
                        drawLine(markerColor, Offset(0f, center.y), Offset(center.x - radius * 0.65f, center.y), strokeWidth = 1.5f)
                        drawLine(markerColor, Offset(center.x + radius * 0.65f, center.y), Offset(size.width, center.y), strokeWidth = 1.5f)
                    }
                    "FF_DYNAMIC_SPREAD" -> {
                        // Shotgun & SMG Spread Circle with 4-corner tick marks
                        drawCircle(color = markerColor.copy(alpha = 0.6f), radius = radius * 0.8f, center = center, style = Stroke(width = 1.8f))
                        drawCircle(color = markerColor, radius = 2.2f, center = center)
                        val offsetD = radius * 0.57f
                        drawLine(markerColor, Offset(center.x - offsetD, center.y - offsetD), Offset(center.x - offsetD - 4f, center.y - offsetD - 4f), strokeWidth = 2f)
                        drawLine(markerColor, Offset(center.x + offsetD, center.y - offsetD), Offset(center.x + offsetD + 4f, center.y - offsetD - 4f), strokeWidth = 2f)
                        drawLine(markerColor, Offset(center.x - offsetD, center.y + offsetD), Offset(center.x - offsetD - 4f, center.y + offsetD + 4f), strokeWidth = 2f)
                        drawLine(markerColor, Offset(center.x + offsetD, center.y + offsetD), Offset(center.x + offsetD + 4f, center.y + offsetD + 4f), strokeWidth = 2f)
                    }
                    "FF_CYBER_ASSIST" -> {
                        // Dual Ring Cyber Aim Assist
                        drawCircle(color = markerColor, radius = radius * 0.45f, center = center, style = Stroke(width = 2.2f))
                        drawCircle(color = markerColor.copy(alpha = 0.4f), radius = radius * 0.85f, center = center, style = Stroke(width = 1.2f))
                        drawCircle(color = markerColor, radius = 1.5f, center = center)
                        val cross = radius * 0.25f
                        drawLine(markerColor, Offset(center.x, center.y - radius * 0.85f), Offset(center.x, center.y - cross), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(center.x, center.y + cross), Offset(center.x, center.y + radius * 0.85f), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(center.x - radius * 0.85f, center.y), Offset(center.x - cross, center.y), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(center.x + cross, center.y), Offset(center.x + radius * 0.85f, center.y), strokeWidth = 1.6f)
                    }
                    "SNIPER" -> {
                        drawCircle(color = markerColor, radius = radius * 0.85f, center = center, style = Stroke(width = 2f))
                        drawLine(markerColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.6f)
                        drawLine(markerColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.6f)
                        val dotStep = radius * 0.3f
                        drawCircle(color = markerColor, radius = 1.5f, center = Offset(center.x + dotStep, center.y))
                        drawCircle(color = markerColor, radius = 1.5f, center = Offset(center.x - dotStep, center.y))
                        drawCircle(color = markerColor, radius = 1.5f, center = Offset(center.x, center.y + dotStep))
                        drawCircle(color = markerColor, radius = 1.5f, center = Offset(center.x, center.y - dotStep))
                    }
                    else -> { // "CROSS"
                        val gap = 5f
                        drawLine(markerColor, Offset(center.x, center.y - gap), Offset(center.x, 0f), strokeWidth = 2.4f)
                        drawLine(markerColor, Offset(center.x, center.y + gap), Offset(center.x, size.height), strokeWidth = 2.4f)
                        drawLine(markerColor, Offset(center.x - gap, center.y), Offset(0f, center.y), strokeWidth = 2.4f)
                        drawLine(markerColor, Offset(center.x + gap, center.y), Offset(size.width, center.y), strokeWidth = 2.4f)
                        drawCircle(color = markerColor, radius = 1.8f, center = center)
                    }
                }
            }
        }
    }
}
