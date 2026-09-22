package com.srtxcheats.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srtxcheats.ui.theme.GlassBorderSubtle
import com.srtxcheats.ui.theme.GlassCardBackground
import com.srtxcheats.ui.theme.GlassCardBorder
import com.srtxcheats.ui.theme.NeonCyan

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = GlassCardBackground,
    borderColor: Color = GlassCardBorder,
    glowAccent: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (glowAccent) {
        Brush.linearGradient(
            listOf(
                NeonCyan.copy(alpha = 0.6f),
                GlassBorderSubtle,
                Color(0x1000E5FF)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                borderColor,
                GlassBorderSubtle
            )
        )
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isSmallDpi = configuration.screenWidthDp <= 370
    val effectivePadding = if (contentPadding == 16.dp && isSmallDpi) 10.dp else contentPadding

    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                border = BorderStroke(1.dp, borderBrush),
                shape = shape
            ),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(effectivePadding),
            content = content
        )
    }
}
