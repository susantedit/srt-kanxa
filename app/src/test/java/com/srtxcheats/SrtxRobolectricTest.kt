package com.srtxcheats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.srtxcheats.R
import com.srtxcheats.model.DeviceMetrics
import com.srtxcheats.model.GameProfile
import com.srtxcheats.model.SensitivityLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SrtxRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SRT X CHEATS", appName)
  }

  @Test
  fun `verify game profile defaults`() {
    val profile = GameProfile.defaultFor(
      GameProfile.FREE_FIRE_MAX_PACKAGE,
      "Free Fire MAX"
    )
    assertEquals("Free Fire MAX", profile.appName)
    assertEquals("com.dts.freefiremax", profile.packageName)
    assertEquals(SensitivityLevel.HIGH, profile.sensitivityLevel)
    assertEquals(true, profile.showFps)
    assertEquals(true, profile.isOverlayEnabled)
  }

  @Test
  fun `verify device metrics display formatting`() {
    val metrics = DeviceMetrics(
      fps = 60,
      displayHz = 120,
      cpuUsagePercent = 42,
      cpuClockGhz = 2.45f,
      ramUsedGb = 4.2f,
      ramTotalGb = 8.0f,
      batteryTempC = 36.5f
    )
    assertEquals("60", metrics.fpsDisplay)
    assertEquals("42%", metrics.cpuUsageDisplay)
    assertEquals("2.45 GHz", metrics.cpuClockDisplay)
    assertEquals("36.5°C", metrics.batteryTempDisplay)
  }

  @Test
  fun `verify gaming mode and performance boost default states`() {
    val profile = GameProfile.defaultFor("com.dts.freefiremax", "Free Fire MAX")
    assertEquals(false, profile.isGamingModeActive)
    assertEquals(false, profile.isPerformanceBoostActive)
  }
}
