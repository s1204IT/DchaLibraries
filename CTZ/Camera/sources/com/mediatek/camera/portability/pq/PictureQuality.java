package com.mediatek.camera.portability.pq;

import android.util.Log;

public class PictureQuality {
    private static final String TAG = PictureQuality.class.getSimpleName();

    public static void enterCameraMode() {
        if (isSupported()) {
            Log.d(TAG, "[enterCameraMode]");
            PictureQualityWrapper.enterCameraMode();
        }
    }

    public static void exitCameraMode() {
        if (isSupported()) {
            Log.d(TAG, "[exitCameraMode]");
            PictureQualityWrapper.exitCameraMode();
        }
    }

    public static int getMinStepOfESSLED() {
        if (isSupported()) {
            Log.d(TAG, "[getMinStepOfESSLED]");
            return PictureQualityWrapper.getMinStepOfESSLED();
        }
        return -1;
    }

    public static int getMinStepOfESSOLED() {
        if (isSupported()) {
            Log.d(TAG, "[getMinStepOfESSOLED]");
            return PictureQualityWrapper.getMinStepOfESSOLED();
        }
        return -1;
    }

    public static void setMinStepOfESSLED(int i) {
        if (isSupported()) {
            Log.d(TAG, "[setMinStepOfESSLED]");
            PictureQualityWrapper.setMinStepOfESSLED(i);
        }
    }

    public static void setMinStepOfESSOLED(int i) {
        if (isSupported()) {
            Log.d(TAG, "[setMinStepOfESSOLED]");
            PictureQualityWrapper.setMinStepOfESSOLED(i);
        }
    }

    private static boolean isSupported() {
        boolean z;
        try {
            Class.forName("com.mediatek.pq.PictureQuality");
            z = true;
        } catch (ClassNotFoundException e) {
            z = false;
        }
        Log.d(TAG, "[isSupported], return:" + z);
        return z;
    }
}
