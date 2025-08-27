package com.android.settings.fuelgauge.batterysaver;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class AutoBatterySeekBarPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop {
    static final String KEY_AUTO_BATTERY_SEEK_BAR = "battery_saver_seek_bar";
    private static final String TAG = "AutoBatterySeekBarPreferenceController";
    private AutoBatterySaverSettingObserver mContentObserver;
    private SeekBarPreference mPreference;

    public AutoBatterySeekBarPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY_AUTO_BATTERY_SEEK_BAR);
        this.mContentObserver = new AutoBatterySaverSettingObserver(new Handler(Looper.getMainLooper()));
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (SeekBarPreference) preferenceScreen.findPreference(KEY_AUTO_BATTERY_SEEK_BAR);
        this.mPreference.setContinuousUpdates(true);
        this.mPreference.setAccessibilityRangeInfoType(2);
        updatePreference(this.mPreference);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        updatePreference(preference);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mContentObserver.registerContentObserver();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mContentObserver.unRegisterContentObserver();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "low_power_trigger_level", ((Integer) obj).intValue());
        return true;
    }

    void updatePreference(Preference preference) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int i = Settings.Global.getInt(contentResolver, "low_power_trigger_level_max", 0);
        if (i > 0) {
            if (!(preference instanceof SeekBarPreference)) {
                Log.e(TAG, "Unexpected preference class: " + preference.getClass());
            } else {
                SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                if (i < seekBarPreference.getMin()) {
                    Log.e(TAG, "LOW_POWER_MODE_TRIGGER_LEVEL_MAX too low; ignored.");
                } else {
                    seekBarPreference.setMax(i);
                }
            }
        }
        int i2 = Settings.Global.getInt(contentResolver, "low_power_trigger_level", 0);
        if (i2 == 0) {
            preference.setVisible(false);
            return;
        }
        preference.setVisible(true);
        preference.setTitle(this.mContext.getString(R.string.battery_saver_seekbar_title, Utils.formatPercentage(i2)));
        SeekBarPreference seekBarPreference2 = (SeekBarPreference) preference;
        seekBarPreference2.setProgress(i2);
        seekBarPreference2.setSeekBarContentDescription(this.mContext.getString(R.string.battery_saver_turn_on_automatically_title));
    }

    private final class AutoBatterySaverSettingObserver extends ContentObserver {
        private final ContentResolver mContentResolver;
        private final Uri mUri;

        public AutoBatterySaverSettingObserver(Handler handler) {
            super(handler);
            this.mUri = Settings.Global.getUriFor("low_power_trigger_level");
            this.mContentResolver = AutoBatterySeekBarPreferenceController.this.mContext.getContentResolver();
        }

        public void registerContentObserver() {
            this.mContentResolver.registerContentObserver(this.mUri, false, this);
        }

        public void unRegisterContentObserver() {
            this.mContentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri, int i) {
            if (this.mUri.equals(uri)) {
                AutoBatterySeekBarPreferenceController.this.updatePreference(AutoBatterySeekBarPreferenceController.this.mPreference);
            }
        }
    }
}
