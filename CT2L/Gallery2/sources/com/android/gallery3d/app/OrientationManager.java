package com.android.gallery3d.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.OrientationEventListener;
import com.android.gallery3d.ui.OrientationSource;

public class OrientationManager implements OrientationSource {
    private Activity mActivity;
    private MyOrientationEventListener mOrientationListener;
    private boolean mOrientationLocked = false;
    private boolean mRotationLockedSetting = false;

    public OrientationManager(Activity activity) {
        this.mActivity = activity;
        this.mOrientationListener = new MyOrientationEventListener(activity);
    }

    public void resume() {
        ContentResolver resolver = this.mActivity.getContentResolver();
        this.mRotationLockedSetting = Settings.System.getInt(resolver, "accelerometer_rotation", 0) != 1;
        this.mOrientationListener.enable();
    }

    public void pause() {
        this.mOrientationListener.disable();
    }

    public void unlockOrientation() {
        if (this.mOrientationLocked) {
            this.mOrientationLocked = false;
            Log.d("OrientationManager", "unlock orientation");
            this.mActivity.setRequestedOrientation(10);
        }
    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation != -1) {
                OrientationManager.roundOrientation(orientation, 0);
            }
        }
    }

    @Override
    public int getDisplayRotation() {
        return getDisplayRotation(this.mActivity);
    }

    @Override
    public int getCompensation() {
        return 0;
    }

    private static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == -1) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            changeOrientation = Math.min(dist, 360 - dist) >= 50;
        }
        if (changeOrientation) {
            return (((orientation + 45) / 90) * 90) % 360;
        }
        return orientationHistory;
    }

    private static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case 0:
            default:
                return 0;
            case 1:
                return 90;
            case 2:
                return 180;
            case 3:
                return 270;
        }
    }
}
