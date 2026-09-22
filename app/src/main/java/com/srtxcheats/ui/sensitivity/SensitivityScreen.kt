package com.srtxcheats.ui.sensitivity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SensitivityScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    com.srtxcheats.sensitivity.SensitivityScreen(
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
