package com.android.camera;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;

public class DisableCameraReceiver extends BroadcastReceiver {
    private static final String[] ACTIVITIES = {"com.android.camera.CameraLauncher"};

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean needCameraActivity = hasBackCamera();
        if (!needCameraActivity) {
            Log.i("G:DisableCameraReceiver", "disable all camera activities");
            for (int i = 0; i < ACTIVITIES.length; i++) {
                disableComponent(context, ACTIVITIES[i]);
            }
        }
        disableComponent(context, "com.android.camera.DisableCameraReceiver");
    }

    private boolean hasBackCamera() {
        int n = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < n; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == 0) {
                Log.i("G:DisableCameraReceiver", "back camera found: " + i);
                return true;
            }
        }
        Log.i("G:DisableCameraReceiver", "no back camera");
        return false;
    }

    private void disableComponent(Context context, String klass) {
        ComponentName name = new ComponentName(context, klass);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(name, 2, 1);
    }
}
