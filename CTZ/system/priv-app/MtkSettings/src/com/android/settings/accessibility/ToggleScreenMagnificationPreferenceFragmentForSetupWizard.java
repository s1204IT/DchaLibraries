package com.android.settings.accessibility;

import android.os.Bundle;

/* loaded from: classes.dex */
public class ToggleScreenMagnificationPreferenceFragmentForSetupWizard extends ToggleScreenMagnificationPreferenceFragment {
    @Override // com.android.settings.accessibility.ToggleScreenMagnificationPreferenceFragment, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 368;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("checked") && this.mToggleSwitch.isChecked() != arguments.getBoolean("checked")) {
            this.mMetricsFeatureProvider.action(getContext(), 368, this.mToggleSwitch.isChecked());
        }
        super.onStop();
    }
}
