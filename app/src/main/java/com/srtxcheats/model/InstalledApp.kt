package com.srtxcheats.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isGame: Boolean = false,
    val isPopularGame: Boolean = false
)
