package com.android.settings.accessibility;

import android.content.Context;

/* loaded from: classes.dex */
public class HapticFeedbackIntensityPreferenceController extends VibrationIntensityPreferenceController {
    static final String PREF_KEY = "touch_vibration_preference_screen";

    public HapticFeedbackIntensityPreferenceController(Context context) {
        super(context, PREF_KEY, "haptic_feedback_intensity");
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settings.accessibility.VibrationIntensityPreferenceController
    protected int getDefaultIntensity() {
        return this.mVibrator.getDefaultHapticFeedbackIntensity();
    }
}
