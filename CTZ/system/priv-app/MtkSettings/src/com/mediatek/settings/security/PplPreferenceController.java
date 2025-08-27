package com.mediatek.settings.security;

import android.content.Context;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.mediatek.settings.ext.DefaultPplSettingsEntryExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt;

/* loaded from: classes.dex */
public class PplPreferenceController extends AbstractPreferenceController implements LifecycleObserver, OnPause, OnResume {
    private IPplSettingsEntryExt mPplSettingsEntryExt;

    public PplPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mPplSettingsEntryExt = DefaultPplSettingsEntryExt.getInstance(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mPplSettingsEntryExt != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "privacy_protection_lock";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        if (this.mPplSettingsEntryExt != null) {
            this.mPplSettingsEntryExt.addPplPrf((PreferenceGroup) preferenceScreen.findPreference("security_settings_device_admin_category"));
        } else {
            Log.e("PplPrefContr", "[displayPreference] mPplSettingsEntryExt should not be null !!!");
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mPplSettingsEntryExt != null) {
            this.mPplSettingsEntryExt.enablerResume();
        } else {
            Log.e("PplPrefContr", "[onResume] mPplSettingsEntryExt should not be null !!!");
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mPplSettingsEntryExt != null) {
            this.mPplSettingsEntryExt.enablerPause();
        } else {
            Log.e("PplPrefContr", "[onPause] mPplSettingsEntryExt should not be null !!!");
        }
    }
}
