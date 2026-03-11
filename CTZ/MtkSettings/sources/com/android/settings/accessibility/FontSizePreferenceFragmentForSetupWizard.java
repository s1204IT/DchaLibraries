package com.android.settings.accessibility;

public class FontSizePreferenceFragmentForSetupWizard extends ToggleFontSizePreferenceFragment {
    @Override
    public int getMetricsCategory() {
        return 369;
    }

    @Override
    public void onStop() {
        if (this.mCurrentIndex != this.mInitialIndex) {
            this.mMetricsFeatureProvider.action(getContext(), 369, this.mCurrentIndex);
        }
        super.onStop();
    }
}
