package com.android.settings.accessibility;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public abstract class VibrationIntensityPreferenceController extends BasePreferenceController implements LifecycleObserver, OnStart, OnStop {
    private Preference mPreference;
    private final String mSettingKey;
    private final SettingObserver mSettingsContentObserver;
    protected final Vibrator mVibrator;

    protected abstract int getDefaultIntensity();

    public VibrationIntensityPreferenceController(Context context, String str, String str2) {
        super(context, str);
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        this.mSettingKey = str2;
        this.mSettingsContentObserver = new SettingObserver(str2) {
            @Override
            public void onChange(boolean z, Uri uri) {
                VibrationIntensityPreferenceController.this.updateState(VibrationIntensityPreferenceController.this.mPreference);
            }
        };
    }

    @Override
    public void onStart() {
        this.mContext.getContentResolver().registerContentObserver(this.mSettingsContentObserver.uri, false, this.mSettingsContentObserver);
    }

    @Override
    public void onStop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsContentObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        return getIntensityString(this.mContext, Settings.System.getInt(this.mContext.getContentResolver(), this.mSettingKey, getDefaultIntensity()));
    }

    public static CharSequence getIntensityString(Context context, int i) {
        if (context.getResources().getBoolean(R.bool.config_vibration_supports_multiple_intensities)) {
            switch (i) {
                case 0:
                    return context.getString(R.string.accessibility_vibration_intensity_off);
                case 1:
                    return context.getString(R.string.accessibility_vibration_intensity_low);
                case 2:
                    return context.getString(R.string.accessibility_vibration_intensity_medium);
                case 3:
                    return context.getString(R.string.accessibility_vibration_intensity_high);
                default:
                    return "";
            }
        }
        if (i == 0) {
            return context.getString(R.string.switch_off_text);
        }
        return context.getString(R.string.switch_on_text);
    }

    private static class SettingObserver extends ContentObserver {
        public final Uri uri;

        public SettingObserver(String str) {
            super(new Handler(Looper.getMainLooper()));
            this.uri = Settings.System.getUriFor(str);
        }
    }
}
