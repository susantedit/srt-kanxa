package com.srtxcheats.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun ScreenMarkerOverlay(
    style: String = "CROSS", // DOT, CROSS, CIRCLE, RETICLE
    colorLong: Long = 0xFF00E5FF,
    sizeDp: Int = 24,
    offsetX: Int = 0,
    offsetY: Int = 0,
    modifier: Modifier = Modifier
) {
    val markerColor = Color(colorLong)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .size(sizeDp.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            when (style) {
                "DOT" -> {
                    drawCircle(
                        color = markerColor,
                        radius = 3.5f,
                        center = center
                    )
                }
                "CIRCLE" -> {
                    drawCircle(
                        color = markerColor,
                        radius = radius * 0.75f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = markerColor,
                        radius = 2.5f,
                        center = center
                    )
                }
                "RETICLE" -> {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.8f),
                        radius = radius * 0.8f,
                        center = center,
                        style = Stroke(width = 1.8f)
                    )
                    drawCircle(color = markerColor, radius = 2f, center = center)
                    drawLine(markerColor, Offset(center.x, 0f), Offset(center.x, radius * 0.35f), strokeWidth = 2f)
                    drawLine(markerColor, Offset(center.x, size.height), Offset(center.x, size.height - radius * 0.35f), strokeWidth = 2f)
                    drawLine(markerColor, Offset(0f, center.y), Offset(radius * 0.35f, center.y), strokeWidth = 2f)
                    drawLine(markerColor, Offset(size.width, center.y), Offset(size.width - radius * 0.35f, center.y), strokeWidth = 2f)
                }
                else -> { // "CROSS"
                    val gap = 5f
                    drawLine(markerColor, Offset(center.x, center.y - gap), Offset(center.x, 0f), strokeWidth = 2.2f)
                    drawLine(markerColor, Offset(center.x, center.y + gap), Offset(center.x, size.height), strokeWidth = 2.2f)
                    drawLine(markerColor, Offset(center.x - gap, center.y), Offset(0f, center.y), strokeWidth = 2.2f)
                    drawLine(markerColor, Offset(center.x + gap, center.y), Offset(size.width, center.y), strokeWidth = 2.2f)
                    drawCircle(color = markerColor, radius = 1.5f, center = center)
                }
            }
        }
    }
}
