package com.mediatek.camera.portability;

import android.os.Trace;

public class CameraPerformanceTrace {
    public static void beginSection(String str) {
        Trace.traceBegin(8L, str);
    }

    public static void endSection() {
        Trace.traceEnd(8L);
    }
}
