package com.android.gallery3d.app;

import android.app.Activity;
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
        this.mRotationLockedSetting = Settings.System.getInt(this.mActivity.getContentResolver(), "accelerometer_rotation", 0) != 1;
        this.mOrientationListener.enable();
    }

    public void pause() {
        this.mOrientationListener.disable();
    }

    public void unlockOrientation() {
        if (this.mOrientationLocked) {
            this.mOrientationLocked = false;
            Log.d("Gallery2/OrientationManager", "unlock orientation");
            this.mActivity.setRequestedOrientation(10);
        }
    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int i) {
            if (i == -1) {
                return;
            }
            OrientationManager.roundOrientation(i, 0);
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

    private static int roundOrientation(int i, int i2) {
        boolean z = true;
        if (i2 != -1) {
            int iAbs = Math.abs(i - i2);
            if (Math.min(iAbs, 360 - iAbs) < 50) {
                z = false;
            }
        }
        if (z) {
            return (((i + 45) / 90) * 90) % 360;
        }
        return i2;
    }

    private static int getDisplayRotation(Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
        }
        return 0;
    }
}
