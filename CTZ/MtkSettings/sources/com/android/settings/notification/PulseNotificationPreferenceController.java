package com.android.settings.notification;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class PulseNotificationPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private SettingObserver mSettingObserver;

    public PulseNotificationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("notification_pulse");
        if (preferenceFindPreference != null) {
            this.mSettingObserver = new SettingObserver(preferenceFindPreference);
        }
    }

    @Override
    public void onResume() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        }
    }

    @Override
    public void onPause() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), false);
        }
    }

    @Override
    public String getPreferenceKey() {
        return "notification_pulse";
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.^attr-private.keyboardViewStyle);
    }

    @Override
    public void updateState(Preference preference) {
        try {
            boolean z = true;
            if (Settings.System.getInt(this.mContext.getContentResolver(), "notification_light_pulse") != 1) {
                z = false;
            }
            ((TwoStatePreference) preference).setChecked(z);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("PulseNotifPrefContr", "notification_light_pulse not found");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return Settings.System.putInt(this.mContext.getContentResolver(), "notification_light_pulse", ((Boolean) obj).booleanValue() ? 1 : 0);
    }

    class SettingObserver extends ContentObserver {
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;
        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
            this.mPreference = preference;
        }

        public void register(ContentResolver contentResolver, boolean z) {
            if (z) {
                contentResolver.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                PulseNotificationPreferenceController.this.updateState(this.mPreference);
            }
        }
    }
}
