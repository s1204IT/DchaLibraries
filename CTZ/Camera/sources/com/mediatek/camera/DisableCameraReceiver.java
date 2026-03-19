package com.mediatek.camera;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.AsyncTask;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.FeatureLoader;
import com.mediatek.camera.common.mode.CameraApiHelper;

public class DisableCameraReceiver extends BroadcastReceiver {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DisableCameraReceiver.class.getSimpleName());
    private static final String[] ACTIVITIES = {"com.mediatek.camera.CameraLauncher", "com.mediatek.camera.VideoCamera", "com.mediatek.camera.CameraActivity", "com.mediatek.camera.SecureCameraActivity", "com.mediatek.camera.CaptureActivity"};

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!hasCamera()) {
            LogHelper.i(TAG, "disable all camera activities");
            for (int i = 0; i < ACTIVITIES.length; i++) {
                disableComponent(context, ACTIVITIES[i]);
            }
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                CameraApiHelper.getCameraApiType(null);
                CameraApiHelper.getDeviceSpec(context);
                FeatureLoader.loadBuildInFeatures(context);
                FeatureLoader.loadPluginFeatures(context);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private boolean hasCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        LogHelper.i(TAG, "number of camera: " + numberOfCameras);
        return numberOfCameras > 0;
    }

    private void disableComponent(Context context, String str) {
        context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, str), 2, 1);
    }
}
