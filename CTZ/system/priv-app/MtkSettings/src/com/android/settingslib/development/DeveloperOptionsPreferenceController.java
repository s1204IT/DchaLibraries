package com.android.settingslib.development;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public abstract class DeveloperOptionsPreferenceController extends AbstractPreferenceController {
    protected Preference mPreference;

    public DeveloperOptionsPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    public void onDeveloperOptionsEnabled() {
        if (isAvailable()) {
            onDeveloperOptionsSwitchEnabled();
        }
    }

    public void onDeveloperOptionsDisabled() {
        if (isAvailable()) {
            onDeveloperOptionsSwitchDisabled();
        }
    }

    protected void onDeveloperOptionsSwitchEnabled() {
        this.mPreference.setEnabled(true);
    }

    protected void onDeveloperOptionsSwitchDisabled() {
        this.mPreference.setEnabled(false);
    }
}
