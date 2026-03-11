package com.android.settings.display;

import com.android.internal.logging.MetricsLogger;

public class ScreenZoomPreferenceFragmentForSetupWizard extends ScreenZoomSettings {
    @Override
    protected int getMetricsCategory() {
        return 370;
    }

    @Override
    public void onStop() {
        if (this.mCurrentIndex != this.mInitialIndex) {
            MetricsLogger.action(getContext(), 370, this.mCurrentIndex);
        }
        super.onStop();
    }
}
