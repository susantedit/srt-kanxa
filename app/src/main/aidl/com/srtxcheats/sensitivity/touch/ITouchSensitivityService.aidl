// Privileged touch-sensitivity service, hosted in the Shizuku user-service
// process (uid 2000 "shell"). The app process binds this and drives the engine.
//
// Config is passed as primitives rather than a Parcelable so we don't need the
// kotlin-parcelize plugin, and so a version skew between the app and a cached
// user-service APK can't cause a Parcelable ClassNotFound across the boundary.
//
// curve: 0 = Linear, 1 = Ease (smoothstep), 2 = Aggressive (expo). See TouchCurve.
package com.srtxcheats.sensitivity.touch;

import com.srtxcheats.sensitivity.touch.ITouchStatusCallback;

interface ITouchSensitivityService {
    // Reserved transaction Shizuku's server calls to ask the user-service process
    // to tear down and exit. Must keep this exact transaction id.
    void destroy() = 16777114;

    // Enumerate /dev/input via `binPath --detect`. Never grabs. Results arrive
    // on the registered callback as onCandidate/onDeviceResolved/onStatus.
    void detect(String binPath) = 1;

    // Start the grab+transform+inject loop using the helper at binPath.
    // gainX/gainY: 0.5..3.0 displacement multipliers. smoothing: 0..1 EMA factor.
    void start(String binPath, float gainX, float gainY, float smoothing,
               int curve, boolean globalMode, in String[] perAppPackages) = 2;

    // Live-update the transform without restarting the grab.
    void updateConfig(float gainX, float gainY, float smoothing,
                      int curve, boolean globalMode, in String[] perAppPackages) = 3;

    // Release the grab and stop injecting. Safe to call when already stopped.
    void stop() = 4;

    // True while the grab loop is active.
    boolean isRunning() = 5;

    // The device path currently grabbed, or null.
    String currentDevice() = 6;

    void registerStatus(ITouchStatusCallback cb) = 7;
    void unregisterStatus(ITouchStatusCallback cb) = 8;

    // --- Macro / auto-fire (independent of the sensitivity grab; runs alongside it) ---

    // Auto-clicker: tap repeatedly at normalized screen position (nx,ny in 0..1) at
    // clicksPerSecond (10..100). When loop is false it stops after burstCount taps.
    void startAutoClick(float nx, float ny, int clicksPerSecond, boolean loop, int burstCount) = 9;

    // Replay a serialized macro (MacroCodec text). speedPercent scales timing (100 = as recorded).
    void startMacroPlayback(String macroData, int speedPercent, boolean loop) = 10;

    // Begin recording the user's touches via `binPath --monitor` (no grab; game still responds).
    void startRecording(String binPath) = 11;

    // Stop recording and return the captured macro serialized via MacroCodec (empty string if none).
    String stopRecording() = 12;

    // Stop any running auto-click / playback (does not affect the sensitivity grab).
    void stopMacro() = 13;

    // True while an auto-click, playback, or recording is active.
    boolean isMacroActive() = 14;
}
