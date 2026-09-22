package com.srtxcheats.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.srtxcheats.core.GameDetector
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.InstalledApp
import com.srtxcheats.ui.components.GlassCard
import com.srtxcheats.ui.theme.AmoledBackground
import com.srtxcheats.ui.theme.GamingAmber
import com.srtxcheats.ui.theme.GamingGreen
import com.srtxcheats.ui.theme.NeonCyan
import com.srtxcheats.ui.theme.TextMuted
import com.srtxcheats.ui.theme.TextPrimary
import com.srtxcheats.ui.theme.TextSecondary

@Composable
fun GameSelectorScreen(
    currentSelectedPackage: String,
    onGameSelected: (String, String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val apps = GameDetector.getInstalledLaunchableApps(context)
        appsList = apps
        isLoading = false
    }

    val filteredApps = remember(appsList, searchQuery) {
        if (searchQuery.isBlank()) appsList
        else appsList.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .padding(16.dp)
            .testTag("game_selector_screen")
    ) {
        Text(
            text = "SELECT TARGET GAME",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = "Choose which game the SRT X CHEATS overlay & profile applies to",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_search_input"),
            placeholder = { Text("Search installed games or apps...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
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
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Quick Options for Free Fire & Free Fire MAX
        Text(
            text = "POPULAR GAMING TARGETS",
            color = GamingAmber,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        val isFfMaxInstalled = remember { GameDetector.isPackageInstalled(context, GameProfile.FREE_FIRE_MAX_PACKAGE) }
        val isFfInstalled = remember { GameDetector.isPackageInstalled(context, GameProfile.FREE_FIRE_PACKAGE) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetGameCard(
                title = "Free Fire MAX",
                pkg = GameProfile.FREE_FIRE_MAX_PACKAGE,
                isSelected = currentSelectedPackage == GameProfile.FREE_FIRE_MAX_PACKAGE,
                isInstalled = isFfMaxInstalled,
                modifier = Modifier.weight(1f),
                onClick = {
                    onGameSelected(GameProfile.FREE_FIRE_MAX_PACKAGE, "Free Fire MAX")
                }
            )

            PresetGameCard(
                title = "Free Fire",
                pkg = GameProfile.FREE_FIRE_PACKAGE,
                isSelected = currentSelectedPackage == GameProfile.FREE_FIRE_PACKAGE,
                isInstalled = isFfInstalled,
                modifier = Modifier.weight(1f),
                onClick = {
                    onGameSelected(GameProfile.FREE_FIRE_PACKAGE, "Free Fire")
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "INSTALLED APPLICATIONS (${filteredApps.size})",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching applications found", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName == currentSelectedPackage
                    InstalledAppItem(
                        app = app,
                        isSelected = isSelected,
                        onClick = {
                            onGameSelected(app.packageName, app.appName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PresetGameCard(
    title: String,
    pkg: String,
    isSelected: Boolean,
    isInstalled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0x3300E5FF) else Color(0x330E1424))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonCyan else Color(0x22FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FF9100)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Gamepad, contentDescription = null, tint = GamingAmber, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) NeonCyan else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        isSelected && isInstalled -> "SELECTED • INSTALLED ✓"
                        isSelected -> "SELECTED"
                        isInstalled -> "INSTALLED ✓"
                        else -> "Tap to select"
                    },
                    color = if (isInstalled || isSelected) GamingGreen else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = GamingGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun InstalledAppItem(
    app: InstalledApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x2B00E5FF) else Color(0x660B0F1A))
            .border(
                width = 1.dp,
                color = if (isSelected) NeonCyan else Color(0x11FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                val iconBmp = remember(app) {
                    try { app.icon?.toBitmap(64, 64) } catch (_: Exception) { null }
                }

                if (iconBmp != null) {
                    Image(
                        bitmap = iconBmp.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x331E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gamepad, contentDescription = null, tint = TextMuted)
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.appName,
                            color = if (isSelected) NeonCyan else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (app.isGame) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FF9100))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("GAME", color = GamingAmber, fontSize = 7.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(
                        text = app.packageName,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
