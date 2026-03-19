package com.android.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.display.BrightnessUtils;
import java.text.NumberFormat;

public class BrightnessLevelPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private ContentObserver mBrightnessObserver;
    private final ContentResolver mContentResolver;
    private final int mMaxBrightness;
    private final int mMaxVrBrightness;
    private final int mMinBrightness;
    private final int mMinVrBrightness;
    private Preference mPreference;
    private static final Uri BRIGHTNESS_URI = Settings.System.getUriFor("screen_brightness");
    private static final Uri BRIGHTNESS_FOR_VR_URI = Settings.System.getUriFor("screen_brightness_for_vr");
    private static final Uri BRIGHTNESS_ADJ_URI = Settings.System.getUriFor("screen_auto_brightness_adj");

    public BrightnessLevelPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mBrightnessObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean z) {
                BrightnessLevelPreferenceController.this.updatedSummary(BrightnessLevelPreferenceController.this.mPreference);
            }
        };
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mMinBrightness = powerManager.getMinimumScreenBrightnessSetting();
        this.mMaxBrightness = powerManager.getMaximumScreenBrightnessSetting();
        this.mMinVrBrightness = powerManager.getMinimumScreenBrightnessForVrSetting();
        this.mMaxVrBrightness = powerManager.getMaximumScreenBrightnessForVrSetting();
        this.mContentResolver = this.mContext.getContentResolver();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "brightness";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("brightness");
    }

    @Override
    public void updateState(Preference preference) {
        updatedSummary(preference);
    }

    @Override
    public void onStart() {
        this.mContentResolver.registerContentObserver(BRIGHTNESS_URI, false, this.mBrightnessObserver);
        this.mContentResolver.registerContentObserver(BRIGHTNESS_FOR_VR_URI, false, this.mBrightnessObserver);
        this.mContentResolver.registerContentObserver(BRIGHTNESS_ADJ_URI, false, this.mBrightnessObserver);
    }

    @Override
    public void onStop() {
        this.mContentResolver.unregisterContentObserver(this.mBrightnessObserver);
    }

    private void updatedSummary(Preference preference) {
        if (preference != null) {
            preference.setSummary(NumberFormat.getPercentInstance().format(getCurrentBrightness()));
        }
    }

    private double getCurrentBrightness() {
        int iConvertLinearToGamma;
        if (isInVrMode()) {
            iConvertLinearToGamma = BrightnessUtils.convertLinearToGamma(Settings.System.getInt(this.mContentResolver, "screen_brightness_for_vr", this.mMaxBrightness), this.mMinVrBrightness, this.mMaxVrBrightness);
        } else {
            iConvertLinearToGamma = BrightnessUtils.convertLinearToGamma(Settings.System.getInt(this.mContentResolver, "screen_brightness", this.mMinBrightness), this.mMinBrightness, this.mMaxBrightness);
        }
        return getPercentage(iConvertLinearToGamma, 0, 1023);
    }

    private double getPercentage(double d, int i, int i2) {
        if (d > i2) {
            return 1.0d;
        }
        double d2 = i;
        if (d < d2) {
            return 0.0d;
        }
        return (d - d2) / ((double) (i2 - i));
    }

    boolean isInVrMode() {
        try {
            return IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager")).getVrModeState();
        } catch (RemoteException e) {
            Log.e("BrightnessPrefCtrl", "Failed to check vr mode!", e);
            return false;
        }
    }
}
