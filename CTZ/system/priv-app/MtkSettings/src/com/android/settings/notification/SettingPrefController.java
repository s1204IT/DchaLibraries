package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public abstract class SettingPrefController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private SettingsPreferenceFragment mParent;
    protected SettingPref mPreference;
    protected SettingsObserver mSettingsObserver;

    public SettingPrefController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, Lifecycle lifecycle) {
        super(context);
        this.mParent = settingsPreferenceFragment;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mPreference.init(this.mParent);
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mSettingsObserver = new SettingsObserver();
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mPreference.getKey();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mPreference.isApplicable(this.mContext);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        this.mPreference.update(this.mContext);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mSettingsObserver != null) {
            this.mSettingsObserver.register(true);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mSettingsObserver != null) {
            this.mSettingsObserver.register(false);
        }
    }

    @VisibleForTesting
    final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean z) {
            ContentResolver contentResolver = SettingPrefController.this.mContext.getContentResolver();
            if (z) {
                contentResolver.registerContentObserver(SettingPrefController.this.mPreference.getUri(), false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (SettingPrefController.this.mPreference.getUri().equals(uri)) {
                SettingPrefController.this.mPreference.update(SettingPrefController.this.mContext);
            }
        }
    }
}
