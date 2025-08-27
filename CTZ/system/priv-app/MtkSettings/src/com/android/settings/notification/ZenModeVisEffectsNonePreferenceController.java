package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.notification.ZenCustomRadioButtonPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeVisEffectsNonePreferenceController extends AbstractZenModePreferenceController implements ZenCustomRadioButtonPreference.OnRadioButtonClickListener {
    private final String KEY;
    private ZenCustomRadioButtonPreference mPreference;

    public ZenModeVisEffectsNonePreferenceController(Context context, Lifecycle lifecycle, String str) {
        super(context, str, lifecycle);
        this.KEY = str;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (ZenCustomRadioButtonPreference) preferenceScreen.findPreference(this.KEY);
        this.mPreference.setOnRadioButtonClickListener(this);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mPreference.setChecked(this.mBackend.mPolicy.suppressedVisualEffects == 0);
    }

    @Override // com.android.settings.notification.ZenCustomRadioButtonPreference.OnRadioButtonClickListener
    public void onRadioButtonClick(ZenCustomRadioButtonPreference zenCustomRadioButtonPreference) {
        this.mMetricsFeatureProvider.action(this.mContext, 1396, true);
        this.mBackend.saveVisualEffectsPolicy(511, false);
    }
}
