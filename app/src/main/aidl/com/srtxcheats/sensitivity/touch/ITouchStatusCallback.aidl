// Status/event callback from the privileged touch service back to the app process.
// oneway so the uid-2000 service never blocks on the app's main thread.
package com.srtxcheats.sensitivity.touch;

interface ITouchStatusCallback {
    // A parsed TOUCHGRAB_STATUS line, e.g. "grabbed", "heartbeat_timeout",
    // "detect_complete_no_grab". Free-form so new native statuses need no AIDL change.
    oneway void onStatus(String status, String detail);

    // The active touchscreen was identified. Ranges come from TOUCHGRAB_RANGE.
    oneway void onDeviceResolved(String path, String name, int xMin, int xMax, int yMin, int yMax);

    // A --detect candidate line (device enumeration; no grab taken).
    oneway void onCandidate(String path, int score, String name, int xMin, int xMax, int yMin, int yMax);

    // A TOUCHGRAB_ERROR line, or a service-side failure (exec failed, injector missing, ...).
    oneway void onError(String code, String message);

    // The grab loop ended and the touchscreen was released. Always fires on teardown.
    oneway void onStopped(String reason);
}
