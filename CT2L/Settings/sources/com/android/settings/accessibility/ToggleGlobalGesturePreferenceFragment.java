package com.android.settings.accessibility;

import android.provider.Settings;
import com.android.settings.widget.ToggleSwitch;

public class ToggleGlobalGesturePreferenceFragment extends ToggleFeaturePreferenceFragment {
    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Global.putInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", enabled ? 1 : 0);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                ToggleGlobalGesturePreferenceFragment.this.mSwitchBar.setCheckedInternal(checked);
                ToggleGlobalGesturePreferenceFragment.this.getArguments().putBoolean("checked", checked);
                ToggleGlobalGesturePreferenceFragment.this.onPreferenceToggled(ToggleGlobalGesturePreferenceFragment.this.mPreferenceKey, checked);
                return false;
            }
        });
    }
}
