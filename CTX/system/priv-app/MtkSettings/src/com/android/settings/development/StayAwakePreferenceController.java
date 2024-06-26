package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
/* loaded from: classes.dex */
public class StayAwakePreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 7;
    private RestrictedSwitchPreference mPreference;
    SettingsObserver mSettingsObserver;

    public StayAwakePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "keep_screen_on";
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (RestrictedSwitchPreference) preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", ((Boolean) obj).booleanValue() ? 7 : 0);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        RestrictedLockUtils.EnforcedAdmin checkIfMaximumTimeToLockSetByAdmin = checkIfMaximumTimeToLockSetByAdmin();
        if (checkIfMaximumTimeToLockSetByAdmin != null) {
            this.mPreference.setDisabledByAdmin(checkIfMaximumTimeToLockSetByAdmin);
            return;
        }
        this.mPreference.setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", 0) != 0);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mPreference == null) {
            return;
        }
        if (this.mSettingsObserver == null) {
            this.mSettingsObserver = new SettingsObserver();
        }
        this.mSettingsObserver.register(true);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mPreference == null || this.mSettingsObserver == null) {
            return;
        }
        this.mSettingsObserver.register(false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    public void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", 0);
        this.mPreference.setChecked(false);
    }

    RestrictedLockUtils.EnforcedAdmin checkIfMaximumTimeToLockSetByAdmin() {
        return RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(this.mContext);
    }

    /* loaded from: classes.dex */
    class SettingsObserver extends ContentObserver {
        private final Uri mStayAwakeUri;

        public SettingsObserver() {
            super(new Handler());
            this.mStayAwakeUri = Settings.Global.getUriFor("stay_on_while_plugged_in");
        }

        public void register(boolean z) {
            ContentResolver contentResolver = StayAwakePreferenceController.this.mContext.getContentResolver();
            if (z) {
                contentResolver.registerContentObserver(this.mStayAwakeUri, false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.mStayAwakeUri.equals(uri)) {
                StayAwakePreferenceController.this.updateState(StayAwakePreferenceController.this.mPreference);
            }
        }
    }
}
