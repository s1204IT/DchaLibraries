package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.ZenCustomRadioButtonPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeVisEffectsCustomPreferenceController extends AbstractZenModePreferenceController {
    private final String KEY;
    private ZenCustomRadioButtonPreference mPreference;

    public ZenModeVisEffectsCustomPreferenceController(Context context, Lifecycle lifecycle, String str) {
        super(context, str, lifecycle);
        this.KEY = str;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (ZenCustomRadioButtonPreference) preferenceScreen.findPreference(this.KEY);
        this.mPreference.setOnGearClickListener(new ZenCustomRadioButtonPreference.OnGearClickListener() {
            @Override
            public final void onGearClick(ZenCustomRadioButtonPreference zenCustomRadioButtonPreference) {
                this.f$0.launchCustomSettings();
            }
        });
        this.mPreference.setOnRadioButtonClickListener(new ZenCustomRadioButtonPreference.OnRadioButtonClickListener() {
            @Override
            public final void onRadioButtonClick(ZenCustomRadioButtonPreference zenCustomRadioButtonPreference) {
                this.f$0.launchCustomSettings();
            }
        });
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mPreference.setChecked(areCustomOptionsSelected());
    }

    protected boolean areCustomOptionsSelected() {
        return (NotificationManager.Policy.areAllVisualEffectsSuppressed(this.mBackend.mPolicy.suppressedVisualEffects) || (this.mBackend.mPolicy.suppressedVisualEffects == 0)) ? false : true;
    }

    protected void select() {
        this.mMetricsFeatureProvider.action(this.mContext, 1399, true);
    }

    private void launchCustomSettings() {
        select();
        new SubSettingLauncher(this.mContext).setDestination(ZenModeBlockedEffectsSettings.class.getName()).setTitle(R.string.zen_mode_what_to_block_title).setSourceMetricsCategory(1400).launch();
    }
}
