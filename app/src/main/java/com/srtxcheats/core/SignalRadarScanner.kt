package com.srtxcheats.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import com.srtxcheats.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class RadarSector(
    val sectorIndex: Int,       // 0 to 11 (each 30 degrees)
    val startAngleDeg: Float,
    val endAngleDeg: Float,
    val sampleCount: Int,
    val averageSignalDbm: Int,
    val isStrongest: Boolean
)

data class RadarScanState(
    val currentHeadingDeg: Float = 0f,
    val currentSignalDbm: Int = -100,
    val bestHeadingDeg: Float = 0f,
    val bestSignalDbm: Int = -120,
    val scanProgressPercent: Int = 0,
    val sectors: List<RadarSector> = emptyList(),
    val isScanning: Boolean = false,
    val recommendationText: String = "Rotate 360° to locate strongest network direction"
)

class SignalRadarScanner(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val _radarState = MutableStateFlow(RadarScanState())
    val radarState: StateFlow<RadarScanState> = _radarState.asStateFlow()

    // 12 sectors of 30 degrees each
    private val sectorSamples = Array(12) { mutableListOf<Int>() }
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var isScanning = false

    fun startScan() {
        if (isScanning) return
        isScanning = true
        for (list in sectorSamples) {
            list.clear()
        }

        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        _radarState.value = _radarState.value.copy(
            isScanning = true,
            scanProgressPercent = 0,
            recommendationText = "Slowly rotate 360° to scan signal tower alignment..."
        )
        AppLogger.i("Signal Radar Scanner started.")
    }

    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        sensorManager.unregisterListener(this)
        _radarState.value = _radarState.value.copy(isScanning = false)
        AppLogger.i("Signal Radar Scanner stopped.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isScanning) return

        var azimuthDeg = 0f
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            if (azimuthDeg < 0) azimuthDeg += 360f
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            azimuthDeg = event.values[0]
        }

        // Measure live signal strength
        val currentDbm = readCurrentSignalDbm()
        val sectorIdx = ((azimuthDeg / 30f).toInt()).coerceIn(0, 11)
        sectorSamples[sectorIdx].add(currentDbm)

        // Calculate statistics
        var sampledSectorCount = 0
        var highestAvgDbm = -150
        var bestAngle = 0f

        val computedSectors = mutableListOf<RadarSector>()
        for (i in 0 until 12) {
            val samples = sectorSamples[i]
            if (samples.isNotEmpty()) {
                sampledSectorCount++
                val avg = samples.average().roundToInt()
                if (avg > highestAvgDbm) {
                    highestAvgDbm = avg
                    bestAngle = (i * 30f) + 15f
                }
            }
        }

        for (i in 0 until 12) {
            val samples = sectorSamples[i]
            val avg = if (samples.isNotEmpty()) samples.average().roundToInt() else -120
            val isStrongest = (avg == highestAvgDbm && samples.isNotEmpty())
            computedSectors.add(
                RadarSector(
                    sectorIndex = i,
                    startAngleDeg = i * 30f,
                    endAngleDeg = (i + 1) * 30f,
                    sampleCount = samples.size,
                    averageSignalDbm = avg,
                    isStrongest = isStrongest
                )
            )
        }

        val progress = ((sampledSectorCount / 12f) * 100).roundToInt().coerceIn(0, 100)
        val rec = if (progress >= 75) {
            "Strongest network aligned at ${bestAngle.toInt()}° (${highestAvgDbm} dBm). Face this direction for lowest ping!"
        } else {
            "Scanning... rotate full 360° ($progress% complete)"
        }

        _radarState.value = RadarScanState(
            currentHeadingDeg = azimuthDeg,
            currentSignalDbm = currentDbm,
            bestHeadingDeg = bestAngle,
            bestSignalDbm = highestAvgDbm,
            scanProgressPercent = progress,
            sectors = computedSectors,
            isScanning = true,
            recommendationText = rec
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun readCurrentSignalDbm(): Int {
        // 1. Try Wi-Fi first if connected
        if (wifiManager != null && wifiManager.isWifiEnabled) {
            try {
                val info = wifiManager.connectionInfo
                if (info != null && info.networkId != -1) {
                    val rssi = info.rssi
                    if (rssi in -120..-10) return rssi
                }
            } catch (_: Exception) {}
        }

        // 2. Try Cellular signal
        try {
            val cells = telephonyManager?.allCellInfo
            if (!cells.isNullOrEmpty()) {
                for (cell in cells) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
                        val nrStrength = cell.cellSignalStrength as? CellSignalStrengthNr
                        if (nrStrength != null && nrStrength.dbm in -140..-40) {
                            return nrStrength.dbm
                        }
                    } else if (cell is CellInfoLte) {
                        val lteStrength = cell.cellSignalStrength
                        if (lteStrength.dbm in -140..-40) {
                            return lteStrength.dbm
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return -85 // Baseline reference
    }
}
