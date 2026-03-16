package com.android.settings.accessibility;

import android.provider.Settings;
import com.android.settings.widget.ToggleSwitch;

public class ToggleScreenMagnificationPreferenceFragment extends ToggleFeaturePreferenceFragment {
    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_magnification_enabled", enabled ? 1 : 0);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                ToggleScreenMagnificationPreferenceFragment.this.mSwitchBar.setCheckedInternal(checked);
                ToggleScreenMagnificationPreferenceFragment.this.getArguments().putBoolean("checked", checked);
                ToggleScreenMagnificationPreferenceFragment.this.onPreferenceToggled(ToggleScreenMagnificationPreferenceFragment.this.mPreferenceKey, checked);
                return false;
            }
        });
    }
}
