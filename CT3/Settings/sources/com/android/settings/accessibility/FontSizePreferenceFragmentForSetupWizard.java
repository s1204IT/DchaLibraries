package com.android.settings.accessibility;

import com.android.internal.logging.MetricsLogger;

public class FontSizePreferenceFragmentForSetupWizard extends ToggleFontSizePreferenceFragment {
    @Override
    protected int getMetricsCategory() {
        return 369;
    }

    @Override
    public void onStop() {
        if (this.mCurrentIndex != this.mInitialIndex) {
            MetricsLogger.action(getContext(), 369, this.mCurrentIndex);
        }
        super.onStop();
    }
}
