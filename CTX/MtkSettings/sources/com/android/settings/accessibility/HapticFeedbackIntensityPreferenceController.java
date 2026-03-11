package com.android.settings.accessibility;

import android.content.Context;

public class HapticFeedbackIntensityPreferenceController extends VibrationIntensityPreferenceController {
    static final String PREF_KEY = "touch_vibration_preference_screen";

    public HapticFeedbackIntensityPreferenceController(Context context) {
        super(context, PREF_KEY, "haptic_feedback_intensity");
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    protected int getDefaultIntensity() {
        return this.mVibrator.getDefaultHapticFeedbackIntensity();
    }
}
