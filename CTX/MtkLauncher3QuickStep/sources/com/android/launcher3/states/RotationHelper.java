package com.android.launcher3.states;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

public class RotationHelper implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ALLOW_ROTATION_PREFERENCE_KEY = "pref_allowRotation";
    public static final int REQUEST_LOCK = 2;
    public static final int REQUEST_NONE = 0;
    public static final int REQUEST_ROTATE = 1;
    private final Activity mActivity;
    private boolean mAutoRotateEnabled;
    public boolean mDestroyed;
    private final boolean mIgnoreAutoRotateSettings;
    private boolean mInitialized;
    private final SharedPreferences mPrefs;
    private int mStateHandlerRequest = 0;
    private int mCurrentStateRequest = 0;
    private int mLastActivityFlags = -1;

    public static boolean getAllowRotationDefaultValue() {
        if (!Utilities.ATLEAST_NOUGAT) {
            return false;
        }
        Resources system = Resources.getSystem();
        return (system.getConfiguration().smallestScreenWidthDp * system.getDisplayMetrics().densityDpi) / DisplayMetrics.DENSITY_DEVICE_STABLE >= 600;
    }

    public RotationHelper(Activity activity) {
        this.mActivity = activity;
        this.mIgnoreAutoRotateSettings = this.mActivity.getResources().getBoolean(R.bool.allow_rotation);
        if (!this.mIgnoreAutoRotateSettings) {
            this.mPrefs = Utilities.getPrefs(this.mActivity);
            this.mPrefs.registerOnSharedPreferenceChangeListener(this);
            this.mAutoRotateEnabled = this.mPrefs.getBoolean(ALLOW_ROTATION_PREFERENCE_KEY, getAllowRotationDefaultValue());
            return;
        }
        this.mPrefs = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        this.mAutoRotateEnabled = this.mPrefs.getBoolean(ALLOW_ROTATION_PREFERENCE_KEY, getAllowRotationDefaultValue());
        notifyChange();
    }

    public void setStateHandlerRequest(int i) {
        if (this.mStateHandlerRequest != i) {
            this.mStateHandlerRequest = i;
            notifyChange();
        }
    }

    public void setCurrentStateRequest(int i) {
        if (this.mCurrentStateRequest != i) {
            this.mCurrentStateRequest = i;
            notifyChange();
        }
    }

    public void initialize() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            notifyChange();
        }
    }

    public void destroy() {
        if (!this.mDestroyed) {
            this.mDestroyed = true;
            if (this.mPrefs != null) {
                this.mPrefs.unregisterOnSharedPreferenceChangeListener(this);
            }
        }
    }

    private void notifyChange() {
        if (!this.mInitialized || this.mDestroyed) {
            return;
        }
        int i = -1;
        if (this.mStateHandlerRequest != 0) {
            if (this.mStateHandlerRequest == 2) {
                i = 14;
            }
        } else if (this.mCurrentStateRequest != 2) {
            if (!this.mIgnoreAutoRotateSettings && this.mCurrentStateRequest != 1 && !this.mAutoRotateEnabled) {
                i = 5;
            }
        }
        if (i != this.mLastActivityFlags) {
            this.mLastActivityFlags = i;
            this.mActivity.setRequestedOrientation(i);
        }
    }

    public String toString() {
        return String.format("[mStateHandlerRequest=%d, mCurrentStateRequest=%d, mLastActivityFlags=%d, mIgnoreAutoRotateSettings=%b, mAutoRotateEnabled=%b]", Integer.valueOf(this.mStateHandlerRequest), Integer.valueOf(this.mCurrentStateRequest), Integer.valueOf(this.mLastActivityFlags), Boolean.valueOf(this.mIgnoreAutoRotateSettings), Boolean.valueOf(this.mAutoRotateEnabled));
    }
}
