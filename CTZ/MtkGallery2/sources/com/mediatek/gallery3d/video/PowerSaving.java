package com.mediatek.gallery3d.video;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.PowerManagerUtils;
import com.mediatek.galleryportable.WindowManagerUtils;

public abstract class PowerSaving {
    public static final int POWER_SAVING_MODE_DIM = 1;
    public static final int POWER_SAVING_MODE_NONE = 2;
    public static final int POWER_SAVING_MODE_OFF = 0;
    private static final String TAG = "VP_PowerSaving";
    protected Context mContext;
    private int mDelayedTime;
    private Handler mHandler;
    private boolean mIsAutoBrithtness;
    private PowerManager mPowerManager;
    private int mPowerSavingMode;
    private final Runnable mPowerSavingRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(PowerSaving.TAG, "mPowerSavingRunnable run");
            PowerSaving.this.adjustBacklight();
        }
    };
    private float mScreenBrightness;
    private Window mWindow;

    protected abstract int getDelayTime();

    protected abstract int getPowerSavingMode();

    public PowerSaving(Context context, Window window) {
        Log.v(TAG, "PowerSaving construct");
        this.mContext = context;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWindow = window;
        this.mHandler = new Handler();
        this.mPowerSavingMode = getPowerSavingMode();
        this.mDelayedTime = getDelayTime();
        initWindowsParameters();
    }

    public void refreshParameter() {
        Log.v(TAG, "refreshParameter()");
        initWindowsParameters();
    }

    private void initWindowsParameters() {
        Log.v(TAG, "initWindowsParameters()");
        this.mIsAutoBrithtness = isAutoBrightnessOn();
        this.mScreenBrightness = this.mWindow.getAttributes().screenBrightness;
    }

    public void startPowerSaving() {
        Log.v(TAG, "startPowerSaving()");
        this.mHandler.removeCallbacks(this.mPowerSavingRunnable);
        if (this.mIsAutoBrithtness) {
            Settings.System.putInt(this.mContext.getContentResolver(), "screen_brightness_mode", 0);
        }
        this.mHandler.postDelayed(this.mPowerSavingRunnable, this.mDelayedTime);
    }

    public void endPowerSaving() {
        Log.v(TAG, "endPowerSaving");
        this.mHandler.removeCallbacks(this.mPowerSavingRunnable);
        if (this.mIsAutoBrithtness) {
            Settings.System.putInt(this.mContext.getContentResolver(), "screen_brightness_mode", 1);
        }
        restoreBacklight();
    }

    protected boolean isAutoBrightnessOn() {
        boolean z = true;
        if (Settings.System.getInt(this.mContext.getContentResolver(), "screen_brightness_mode", 0) != 1) {
            z = false;
        }
        Log.v(TAG, "isAutoBrightnessOn(): " + z);
        return z;
    }

    private void adjustBacklight() {
        Log.v(TAG, "adjustBacklight " + this.mPowerSavingMode);
        switch (this.mPowerSavingMode) {
            case 0:
                PowerManagerUtils.setBacklightOffForWfd(this.mPowerManager, true);
                break;
            case 1:
                WindowManager.LayoutParams attributes = this.mWindow.getAttributes();
                this.mScreenBrightness = attributes.screenBrightness;
                WindowManagerUtils.setSystemUiListeners(attributes);
                if (MtkVideoFeature.isSupperDimmingSupport()) {
                    attributes.screenBrightness = 0.039215688f;
                } else {
                    attributes.screenBrightness = 0.003921569f;
                }
                this.mWindow.setAttributes(attributes);
                break;
        }
    }

    private void restoreBacklight() {
        Log.v(TAG, "restoreBacklight " + this.mPowerSavingMode);
        switch (this.mPowerSavingMode) {
            case 0:
                PowerManagerUtils.setBacklightOffForWfd(this.mPowerManager, false);
                break;
            case 1:
                WindowManager.LayoutParams attributes = this.mWindow.getAttributes();
                attributes.screenBrightness = this.mScreenBrightness;
                WindowManagerUtils.setSystemUiListeners(attributes);
                this.mWindow.setAttributes(attributes);
                break;
        }
    }
}
