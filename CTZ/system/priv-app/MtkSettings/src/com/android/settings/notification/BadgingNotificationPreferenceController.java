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
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class BadgingNotificationPreferenceController extends TogglePreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    static final int OFF = 0;
    static final int ON = 1;
    private static final String TAG = "BadgeNotifPrefContr";
    private SettingObserver mSettingObserver;

    public BadgingNotificationPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("notification_badging");
        if (preferenceFindPreference != null) {
            this.mSettingObserver = new SettingObserver(preferenceFindPreference);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), false);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return this.mContext.getResources().getBoolean(R.^attr-private.internalMinWidth) ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "notification_badging");
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "notification_badging", 1) == 1;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return Settings.Secure.putInt(this.mContext.getContentResolver(), "notification_badging", z ? 1 : 0);
    }

    class SettingObserver extends ContentObserver {
        private final Uri NOTIFICATION_BADGING_URI;
        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            this.NOTIFICATION_BADGING_URI = Settings.Secure.getUriFor("notification_badging");
            this.mPreference = preference;
        }

        public void register(ContentResolver contentResolver, boolean z) {
            if (z) {
                contentResolver.registerContentObserver(this.NOTIFICATION_BADGING_URI, false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.NOTIFICATION_BADGING_URI.equals(uri)) {
                BadgingNotificationPreferenceController.this.updateState(this.mPreference);
            }
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("notification_badging", 2, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, ConfigureNotificationSettings.class.getName(), getPreferenceKey(), this.mContext.getString(com.android.settings.R.string.configure_notification_settings)), isAvailable(), 1);
    }
}
