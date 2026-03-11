package com.android.settings.accessibility;

import android.os.Bundle;

public class ToggleScreenMagnificationPreferenceFragmentForSetupWizard extends ToggleScreenMagnificationPreferenceFragment {
    @Override
    public int getMetricsCategory() {
        return 368;
    }

    @Override
    public void onStop() {
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("checked") && this.mToggleSwitch.isChecked() != arguments.getBoolean("checked")) {
            this.mMetricsFeatureProvider.action(getContext(), 368, this.mToggleSwitch.isChecked());
        }
        super.onStop();
    }
}
