package com.mediatek.camera.common.debug;

import android.os.Environment;
import com.mediatek.camera.portability.CameraPerformanceTrace;
import java.io.File;

public class CameraSysTrace {
    private static String sFilePath = Environment.getExternalStorageDirectory().toString() + "/cameraPerformance.txt";
    private static final boolean DEBUG = new File(sFilePath).exists();

    public static void onEventSystrace(String str, boolean z) {
        if (!DEBUG || LogUtil.getAndroidSDKVersion() < 18) {
            return;
        }
        String str2 = "[CamPtracker]" + str;
        if (z) {
            CameraPerformanceTrace.beginSection(str2);
        } else {
            CameraPerformanceTrace.endSection();
        }
    }
}
