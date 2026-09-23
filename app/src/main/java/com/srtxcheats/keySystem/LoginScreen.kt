package com.srtxcheats.keySystem

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingCrimson
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary
import com.srtxcheats.utils.DeviceInfo

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    logoBitmap: Bitmap?,
    onActivate: (String) -> Unit,
    onSuccessDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    val hwid = remember { DeviceInfo.getHwid(context) }

    androidx.compose.runtime.LaunchedEffect(uiState) {
        if (uiState is LoginUiState.InvalidKey ||
            uiState is LoginUiState.HwidMismatch ||
            uiState is LoginUiState.KeyExpired ||
            (uiState is LoginUiState.Error && !uiState.message.contains("Please enter", ignoreCase = true))
        ) {
            com.srtxcheats.security.SecurityAlarmSoundPlayer.playAlarm5Times(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo & Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = "SRT Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SRT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SRT X CHEATS",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = "PREMIUM ACCESS AUTHORIZATION",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Activation Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowAccent = true,
                contentPadding = 20.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "ENTER YOUR LICENSE KEY",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("key_input_field"),
                    placeholder = {
                        Text(
                            text = "SRT_XXXXXXXXXXXX",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NeonCyan)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x3300E5FF),
                        focusedContainerColor = Color(0x330C1220),
                        unfocusedContainerColor = Color(0x220C1220),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    enabled = uiState !is LoginUiState.Loading && uiState !is LoginUiState.AutoLogin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status & Error Messages
                AnimatedVisibility(visible = uiState !is LoginUiState.Idle) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when (uiState) {
                            is LoginUiState.Loading, is LoginUiState.AutoLogin -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x2200E5FF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = NeonCyan,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (uiState is LoginUiState.AutoLogin) "Verifying saved license session..." else "Authenticating license key...",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            is LoginUiState.Success -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x2200E676))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GamingGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "ACCESS GRANTED • ${uiState.response.type ?: "PREMIUM"}",
                                            color = GamingGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (uiState.response.daysLeft != null) {
                                            Text(
                                                text = "Active subscription: ${uiState.response.daysLeft} days remaining",
                                                color = TextPrimary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                            is LoginUiState.KeyExpired -> {
                                StatusAlert(
                                    title = "LICENSE EXPIRED",
                                    message = "This license key has expired. Please renew your access at cheats.xo.je.",
                                    color = GamingCrimson
                                )
                            }
                            is LoginUiState.HwidMismatch -> {
                                StatusAlert(
                                    title = "DEVICE BINDING MISMATCH",
                                    message = "This key is registered to another device. Reset binding on website.",
                                    color = GamingAmber
                                )
                            }
                            is LoginUiState.LoginLimitReached -> {
                                StatusAlert(
                                    title = "LOGIN LIMIT REACHED",
                                    message = "Max simultaneous sessions reached for this license key.",
                                    color = GamingAmber
                                )
                            }
                            is LoginUiState.InvalidKey -> {
                                StatusAlert(
                                    title = "INVALID LICENSE KEY",
                                    message = "The key entered does not exist or was typed incorrectly.",
                                    color = GamingCrimson
                                )
                            }
                            is LoginUiState.ServerOffline -> {
                                StatusAlert(
                                    title = "SERVER TEMPORARILY OFFLINE",
                                    message = "Authentication server is currently waking up or updating. Please retry in 30 seconds.",
                                    color = GamingAmber
                                )
                            }
                            is LoginUiState.NetworkError -> {
                                StatusAlert(
                                    title = "NETWORK COMMUNICATION ERROR",
                                    message = uiState.error,
                                    color = GamingAmber
                                )
                            }
                            is LoginUiState.Error -> {
                                StatusAlert(
                                    title = "AUTHENTICATION ERROR",
                                    message = uiState.message,
                                    color = GamingCrimson
                                )
                            }
                            else -> {}
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Activate Button
                Button(
                    onClick = {
                        if (uiState is LoginUiState.Success) {
                            onSuccessDismiss()
                        } else {
                            onActivate(keyInput)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_key_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState is LoginUiState.Success) GamingGreen else NeonCyan,
                        contentColor = Color.Black
                    ),
                    enabled = uiState !is LoginUiState.Loading && (uiState is LoginUiState.Success || keyInput.isNotBlank())
                ) {
                    Text(
                        text = if (uiState is LoginUiState.Success) "CONTINUE TO DASHBOARD" else "ACTIVATE KEY",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Get Key Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have a license key?",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KeyRepository.GET_KEY_URL))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GamingAmber
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GamingAmber.copy(alpha = 0.5f))
                    ) {
                        Text("GET KEY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Strict Online Verification & HWID Binding Info
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.6.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("HWID", hwid)
                        clipboard?.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "HWID copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    },
                color = Color(0x180A101C),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STRICT REAL-TIME AUTH", color = NeonCyan, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Text("TAP HWID TO COPY", color = TextMuted, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hwid,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusAlert(
    title: String,
    message: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Column {
            Text(title, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Text(message, color = TextPrimary, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
    }
}
