package com.android.settings.accessibility;

import android.os.Vibrator;
import android.provider.Settings;
import com.android.settings.R;

public class TouchVibrationPreferenceFragment extends VibrationPreferenceFragment {
    @Override
    public int getMetricsCategory() {
        return 1294;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_touch_vibration_settings;
    }

    @Override
    protected String getVibrationIntensitySetting() {
        return "haptic_feedback_intensity";
    }

    @Override
    protected int getDefaultVibrationIntensity() {
        return ((Vibrator) getContext().getSystemService(Vibrator.class)).getDefaultHapticFeedbackIntensity();
    }

    @Override
    protected int getPreviewVibrationAudioAttributesUsage() {
        return 13;
    }

    @Override
    public void onVibrationIntensitySelected(int i) {
        Settings.System.putInt(getContext().getContentResolver(), "haptic_feedback_enabled", i != 0 ? 1 : 0);
    }
}
