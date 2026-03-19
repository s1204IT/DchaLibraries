package com.android.server.audio;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

class RotationHelper {
    private static final String TAG = "AudioService.RotationHelper";
    private static Context sContext;
    private static AudioDisplayListener sDisplayListener;
    private static Handler sHandler;
    private static final Object sRotationLock = new Object();
    private static int sDeviceRotation = 0;

    RotationHelper() {
    }

    static void init(Context context, Handler handler) {
        if (context == null) {
            throw new IllegalArgumentException("Invalid null context");
        }
        sContext = context;
        sHandler = handler;
        sDisplayListener = new AudioDisplayListener();
        enable();
    }

    static void enable() {
        ((DisplayManager) sContext.getSystemService("display")).registerDisplayListener(sDisplayListener, sHandler);
        updateOrientation();
    }

    static void disable() {
        ((DisplayManager) sContext.getSystemService("display")).unregisterDisplayListener(sDisplayListener);
    }

    static void updateOrientation() {
        int rotation = ((WindowManager) sContext.getSystemService("window")).getDefaultDisplay().getRotation();
        synchronized (sRotationLock) {
            if (rotation != sDeviceRotation) {
                sDeviceRotation = rotation;
                publishRotation(sDeviceRotation);
            }
        }
    }

    private static void publishRotation(int i) {
        Log.v(TAG, "publishing device rotation =" + i + " (x90deg)");
        switch (i) {
            case 0:
                AudioSystem.setParameters("rotation=0");
                break;
            case 1:
                AudioSystem.setParameters("rotation=90");
                break;
            case 2:
                AudioSystem.setParameters("rotation=180");
                break;
            case 3:
                AudioSystem.setParameters("rotation=270");
                break;
            default:
                Log.e(TAG, "Unknown device rotation");
                break;
        }
    }

    static final class AudioDisplayListener implements DisplayManager.DisplayListener {
        AudioDisplayListener() {
        }

        @Override
        public void onDisplayAdded(int i) {
        }

        @Override
        public void onDisplayRemoved(int i) {
        }

        @Override
        public void onDisplayChanged(int i) {
            RotationHelper.updateOrientation();
        }
    }
}
