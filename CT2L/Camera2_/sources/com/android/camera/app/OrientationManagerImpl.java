package com.android.camera.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.view.OrientationEventListener;
import com.android.camera.app.OrientationManager;
import com.android.camera.debug.Log;
import com.android.camera.util.ApiHelper;
import java.util.ArrayList;
import java.util.List;

public class OrientationManagerImpl implements OrientationManager {
    private static final int ORIENTATION_HYSTERESIS = 5;
    private static final Log.Tag TAG = new Log.Tag("OrientMgrImpl");
    private final Activity mActivity;
    private final MyOrientationEventListener mOrientationListener;
    private boolean mOrientationLocked = false;
    private boolean mRotationLockedSetting = false;
    private final List<OrientationChangeCallback> mListeners = new ArrayList();

    private static class OrientationChangeCallback {
        private final Handler mHandler;
        private final OrientationManager.OnOrientationChangeListener mListener;

        OrientationChangeCallback(Handler handler, OrientationManager.OnOrientationChangeListener listener) {
            this.mHandler = handler;
            this.mListener = listener;
        }

        public void postOrientationChangeCallback(final int orientation) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    OrientationChangeCallback.this.mListener.onOrientationChanged(orientation);
                }
            });
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof OrientationChangeCallback)) {
                return false;
            }
            OrientationChangeCallback c = (OrientationChangeCallback) o;
            return this.mHandler == c.mHandler && this.mListener == c.mListener;
        }
    }

    public OrientationManagerImpl(Activity activity) {
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

    @Override
    public void addOnOrientationChangeListener(Handler handler, OrientationManager.OnOrientationChangeListener listener) {
        OrientationChangeCallback callback = new OrientationChangeCallback(handler, listener);
        if (!this.mListeners.contains(callback)) {
            this.mListeners.add(callback);
        }
    }

    @Override
    public void removeOnOrientationChangeListener(Handler handler, OrientationManager.OnOrientationChangeListener listener) {
        OrientationChangeCallback callback = new OrientationChangeCallback(handler, listener);
        if (!this.mListeners.remove(callback)) {
            Log.v(TAG, "Removing non-existing listener.");
        }
    }

    @Override
    public void lockOrientation() {
        if (!this.mOrientationLocked && !this.mRotationLockedSetting) {
            this.mOrientationLocked = true;
            if (ApiHelper.HAS_ORIENTATION_LOCK) {
                this.mActivity.setRequestedOrientation(14);
            } else {
                this.mActivity.setRequestedOrientation(calculateCurrentScreenOrientation());
            }
        }
    }

    @Override
    public void unlockOrientation() {
        if (this.mOrientationLocked && !this.mRotationLockedSetting) {
            this.mOrientationLocked = false;
            Log.d(TAG, "unlock orientation");
            this.mActivity.setRequestedOrientation(10);
        }
    }

    @Override
    public boolean isOrientationLocked() {
        return this.mOrientationLocked || this.mRotationLockedSetting;
    }

    private int calculateCurrentScreenOrientation() {
        int displayRotation = getDisplayRotation();
        boolean standard = displayRotation < 180;
        if (this.mActivity.getResources().getConfiguration().orientation == 2) {
            return standard ? 0 : 8;
        }
        if (displayRotation == 90 || displayRotation == 270) {
            standard = !standard;
        }
        return standard ? 1 : 9;
    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation != -1) {
                int roundedOrientation = OrientationManagerImpl.roundOrientation(orientation, 0);
                for (OrientationChangeCallback l : OrientationManagerImpl.this.mListeners) {
                    l.postOrientationChangeCallback(roundedOrientation);
                }
            }
        }
    }

    @Override
    public int getDisplayRotation() {
        return getDisplayRotation(this.mActivity);
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
