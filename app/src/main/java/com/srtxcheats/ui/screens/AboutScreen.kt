package com.srtxcheats.ui.screens

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary

@Composable
fun AboutScreen(
    logoBitmap: Bitmap?,
    onResetSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("about_screen")
    ) {
        // Logo & Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap.asImageBitmap(),
                    contentDescription = "SRT Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonCyan, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SRT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            Column {
                Text(
                    text = "SRT X CHEATS",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SYSTEM UTILITY & RESOLUTION PANEL • v1.0.0",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Package: com.srtxcheats.panel.fpsonly",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Compliance & Safety Statement
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowAccent = true
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = GamingGreen)
                Text(
                    text = "SAFETY, POLICY & INTEGRITY NOTICE",
                    color = GamingGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SRT X CHEATS is an Android system display scaling and touch responsiveness utility. In strict compliance with security and integrity principles:",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val points = listOf(
                "Zero Game File Modification: Never alters or edits third-party APKs, assets, or data.",
                "Zero Memory Injection: Does not inject code or patch third-party game memory.",
                "Authorized System Operations: Uses legitimate Shizuku ADB permissions authorized by the device owner.",
                "Fail-Safe Display Backup: Always captures original wm size and wm density prior to scaling.",
                "Quick Restore: Restore display resolution and DPI anytime from app or floating menu."
            )

            points.forEach { pt ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GamingGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(pt, color = TextPrimary, fontSize = 10.5.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Hardware Details
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                Text(
                    text = "DEVICE HARDWARE ARCHITECTURE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val specs = listOf(
                "Device Model" to "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                "Board / Hardware" to "${Build.BOARD} / ${Build.HARDWARE}",
                "Android Version" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                "CPU Architecture" to Build.SUPPORTED_ABIS.joinToString(", "),
                "Display Brand" to Build.BRAND.uppercase(),
                "Kernel Build" to Build.ID
            )

            specs.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = TextSecondary, fontSize = 11.sp)
                    Text(
                        value,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Settings Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "DATA & PREFERENCES",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Restore all overlay dimensions, sensitivity profiles, and metric toggles to factory default.",
                color = TextSecondary,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onResetSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RESET TO FACTORY DEFAULTS", color = Color(0xFFFF1744), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
