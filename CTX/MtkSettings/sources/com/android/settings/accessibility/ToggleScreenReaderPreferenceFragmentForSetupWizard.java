package com.android.settings.accessibility;

import android.os.Bundle;

public class ToggleScreenReaderPreferenceFragmentForSetupWizard extends ToggleAccessibilityServicePreferenceFragment {
    private boolean mToggleSwitchWasInitiallyChecked;

    @Override
    protected void onProcessArguments(Bundle bundle) {
        super.onProcessArguments(bundle);
        this.mToggleSwitchWasInitiallyChecked = this.mToggleSwitch.isChecked();
    }

    @Override
    public int getMetricsCategory() {
        return 371;
    }

    @Override
    public void onStop() {
        if (this.mToggleSwitch.isChecked() != this.mToggleSwitchWasInitiallyChecked) {
            this.mMetricsFeatureProvider.action(getContext(), 371, this.mToggleSwitch.isChecked());
        }
        super.onStop();
    }
}
