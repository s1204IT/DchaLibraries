package com.android.settings.display;

/* loaded from: classes.dex */
public class ScreenZoomPreferenceFragmentForSetupWizard extends ScreenZoomSettings {
    @Override // com.android.settings.display.ScreenZoomSettings, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 370;
    }

    @Override // com.android.settings.PreviewSeekBarPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        if (this.mCurrentIndex != this.mInitialIndex) {
            this.mMetricsFeatureProvider.action(getContext(), 370, this.mCurrentIndex);
        }
        super.onStop();
    }
}
