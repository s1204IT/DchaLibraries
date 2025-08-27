package com.android.settings.accessibility;

/* loaded from: classes.dex */
public class FontSizePreferenceFragmentForSetupWizard extends ToggleFontSizePreferenceFragment {
    @Override // com.android.settings.accessibility.ToggleFontSizePreferenceFragment, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 369;
    }

    @Override // com.android.settings.PreviewSeekBarPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        if (this.mCurrentIndex != this.mInitialIndex) {
            this.mMetricsFeatureProvider.action(getContext(), 369, this.mCurrentIndex);
        }
        super.onStop();
    }
}
