package com.android.settings.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class NotifyOpenNetworksPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private SettingObserver mSettingObserver;

    public NotifyOpenNetworksPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        lifecycle.addObserver(this);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mSettingObserver = new SettingObserver(preferenceScreen.findPreference("notify_open_networks"));
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
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), "notify_open_networks") || !(preference instanceof SwitchPreference)) {
            return false;
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_networks_available_notification_on", ((SwitchPreference) preference).isChecked() ? 1 : 0);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "notify_open_networks";
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_networks_available_notification_on", 0) == 1);
    }

    class SettingObserver extends ContentObserver {
        private final Uri NETWORKS_AVAILABLE_URI;
        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            this.NETWORKS_AVAILABLE_URI = Settings.Global.getUriFor("wifi_networks_available_notification_on");
            this.mPreference = preference;
        }

        public void register(ContentResolver contentResolver, boolean z) {
            if (z) {
                contentResolver.registerContentObserver(this.NETWORKS_AVAILABLE_URI, false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.NETWORKS_AVAILABLE_URI.equals(uri)) {
                NotifyOpenNetworksPreferenceController.this.updateState(this.mPreference);
            }
        }
    }
}
