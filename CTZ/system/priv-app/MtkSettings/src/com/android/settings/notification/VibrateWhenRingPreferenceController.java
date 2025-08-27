package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class VibrateWhenRingPreferenceController extends TogglePreferenceController implements LifecycleObserver, OnPause, OnResume {
    private static final String KEY_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    private final int DEFAULT_VALUE;
    private final int NOTIFICATION_VIBRATE_WHEN_RINGING;
    private SettingObserver mSettingObserver;

    public VibrateWhenRingPreferenceController(Context context, String str) {
        super(context, str);
        this.DEFAULT_VALUE = 0;
        this.NOTIFICATION_VIBRATE_WHEN_RINGING = 1;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.System.getInt(this.mContext.getContentResolver(), KEY_VIBRATE_WHEN_RINGING, 0) != 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return Settings.System.putInt(this.mContext.getContentResolver(), KEY_VIBRATE_WHEN_RINGING, z ? 1 : 0);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return Utils.isVoiceCapable(this.mContext) ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_VIBRATE_WHEN_RINGING);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference(KEY_VIBRATE_WHEN_RINGING);
        if (preferenceFindPreference != null) {
            this.mSettingObserver = new SettingObserver(preferenceFindPreference);
            preferenceFindPreference.setPersistent(false);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(true);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(false);
        }
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri VIBRATE_WHEN_RINGING_URI;
        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            this.VIBRATE_WHEN_RINGING_URI = Settings.System.getUriFor(VibrateWhenRingPreferenceController.KEY_VIBRATE_WHEN_RINGING);
            this.mPreference = preference;
        }

        public void register(boolean z) {
            ContentResolver contentResolver = VibrateWhenRingPreferenceController.this.mContext.getContentResolver();
            if (z) {
                contentResolver.registerContentObserver(this.VIBRATE_WHEN_RINGING_URI, false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                VibrateWhenRingPreferenceController.this.updateState(this.mPreference);
            }
        }
    }
}
