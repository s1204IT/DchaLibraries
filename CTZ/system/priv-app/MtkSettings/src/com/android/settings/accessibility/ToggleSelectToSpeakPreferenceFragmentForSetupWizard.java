package com.android.settings.accessibility;

import android.os.Bundle;

/* loaded from: classes.dex */
public class ToggleSelectToSpeakPreferenceFragmentForSetupWizard extends ToggleAccessibilityServicePreferenceFragment {
    private boolean mToggleSwitchWasInitiallyChecked;

    @Override // com.android.settings.accessibility.ToggleAccessibilityServicePreferenceFragment, com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onProcessArguments(Bundle bundle) {
        super.onProcessArguments(bundle);
        this.mToggleSwitchWasInitiallyChecked = this.mToggleSwitch.isChecked();
    }

    @Override // com.android.settings.accessibility.ToggleAccessibilityServicePreferenceFragment, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 371;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        if (this.mToggleSwitch.isChecked() != this.mToggleSwitchWasInitiallyChecked) {
            this.mMetricsFeatureProvider.action(getContext(), 817, this.mToggleSwitch.isChecked());
        }
        super.onStop();
    }
}
