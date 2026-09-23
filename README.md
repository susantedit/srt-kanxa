# SRT X CHEATS — Ultimate Free Fire Game Assistant & FPS Panel

A production-ready Android Game Assistant and FPS Performance Panel built with Kotlin and Jetpack Compose. Tailored specifically for Free Fire competitive players with legitimate hardware telemetry, directional network radar, touch sensitivity calibration, and floating HUD overlay.

---

## 📥 Download Ready-to-Install APK
- **Latest Successful Build**: [GitHub Actions Run 35814424064](https://github.com/susantedit/srt-kanxa/actions/runs/35814424064)
- **Artifact**: **SRT-X-CHEATS-Debug-APK** (34.5 MB)
- Click the run link above, scroll to **Artifacts** at the bottom, and click `SRT-X-CHEATS-Debug-APK` to download `app-debug.apk`.

---

## 🚀 Key Features

### 1. 360° Signal Radar Scanner & Smart Dual SIM Switch
- **360° Directional Tower Scanner**: Uses device rotation vector and magnetic azimuth sensors combined with real-time cellular signal strength (`TelephonyManager.allCellInfo`). Maps signal power (in dBm) across twelve 30° sectors to locate the best cell tower direction.
- **Smart Dual SIM Switcher**: Queries `SubscriptionManager` to display real-time signal levels (in dBm) for both SIM cards with carrier identification and one-tap access to system SIM/data settings.
- **Gaming Ping & Latency Tester**: Measures direct TCP socket ping against Free Fire Singapore/SEA clusters (`sg.freefiremobile.com`), Cloudflare DNS (`1.1.1.1`), and Google DNS (`8.8.8.8`).
- **Low-Latency Wi-Fi Lock**: Engages `WifiManager.WIFI_MODE_FULL_LOW_LATENCY` to minimize packet buffering and jitter.

### 2. Game Focus Mode (Total Silence DND)
- Suppresses incoming calls, heads-up notifications, and haptic vibrations during competitive matches.
- Directly controls `NotificationManager.INTERRUPTION_FILTER_NONE` and `AudioManager.RINGER_MODE_SILENT`.

### 3. High-Quality Screen Capture & 16Mbps Screen Recording
- **Instant Screenshot**: Captures clean screenshots and indexes them immediately into the Android MediaStore Gallery.
- **High-Bitrate Screen Recording**: Records 60fps gameplay at 16Mbps bitrate (`screenrecord --bit-rate 16000000`) with live duration display in the floating HUD. Saved to `Movies/SRT_Recordings`.

### 4. Aim Assist & Customizable Mid Crosshair
- **Center Reset Calibration**: One-tap `RESET MID (0,0)` to ensure pixel-perfect screen centering.
- **Free Fire Tactical Presets**:
  - 🎯 **Drag Headshot** (Cyan)
  - 🔭 **AWM Sniper** (Crimson)
  - 💥 **Shotgun Spread** (Amber)
  - ⚡ **Cyber Assist** (Neon Green)
  - **Center Pip Dot** (Pure White)
- **Customizable Controls**: Dynamic size slider (12dp–60dp), 360° marker rotation slider, position nudge buttons, and 20 distinct reticle styles.

### 5. iQOO & iPhone 200% Touch Ultra Mode
- Calibrates touch responsiveness so that even when in-game Free Fire sensitivity is set to 0, flicking and dragging feels like 200% maximum high sensitivity.
- Sets system pointer speed to 7/7.
- Applies LSQ2 zero-friction velocity tracking (`device_config put input_native_boot touch.filter.velocity_tracker_strategy lsq2`).
- Removes touch smoothing delays (`touch.filter.level 0`) and sets `debug.touch.press_threshold 0`.
- Forces display touch polling rate to 120Hz/144Hz and eliminates render animation delays (`window_animation_scale 0.0`).

### 6. Deep Clean All (RAM & Cache Optimizer)
- Terminates background application tasks via `ActivityManager.killBackgroundProcesses`.
- Trims caches using privileged system commands and reports verified freed memory in MB.

### 7. High-Volume Security Alarm on Invalid Credentials
- Bundled audio alert (`wrong_api_sound.mp3`).
- When invalid license credentials or unauthorized bypass attempts are detected, device volume across `STREAM_ALARM`, `STREAM_MUSIC`, and `STREAM_NOTIFICATION` is raised to 100%.
- Employs Android's hardware `LoudnessEnhancer` audio effect with +35 dB acoustic gain amplification (`targetGain = 3500 mB`), ensuring the alert is clearly audible.
- Automatically repeats 5 times consecutively.

---

## 📥 Downloading the APK

### Via GitHub Actions (Automated CI Builds)
1. Navigate to the [Actions tab on GitHub](https://github.com/susantedit/srt-kanxa/actions).
2. Select the latest workflow run: **Build & Release Android APK**.
3. Under the **Artifacts** section at the bottom of the run page, click **SRT-X-CHEATS-Debug-APK** to download the zip containing the ready-to-install APK.

---

## 🛠️ Building Locally

### Prerequisites
- [Android Studio Ladybug | 2024.2+](https://developer.android.com/studio)
- JDK 17 (Temurin or OpenJDK)
- Android SDK 36 (minSdk 24)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/susantedit/srt-kanxa.git
   cd srt-kanxa
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. The compiled APK will be generated at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
4. Install on your connected device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Permissions & Privacy
- **Draw Over Other Apps**: Enables floating in-game HUD overlay.
- **Do Not Disturb (Notification Policy)**: Enables Game Focus Mode total silence.
- **Access Fine Location & Read Phone State**: Required by Android `TelephonyManager` to measure cellular signal strength and Dual SIM slot data.
- **Shizuku / Root (Optional)**: Enables deep cache trimming, screen recording, and system-level velocity tracker calibration.
