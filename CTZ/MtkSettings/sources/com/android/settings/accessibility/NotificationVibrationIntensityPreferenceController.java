package com.android.settings.accessibility;

import android.content.Context;

public class NotificationVibrationIntensityPreferenceController extends VibrationIntensityPreferenceController {
    static final String PREF_KEY = "notification_vibration_preference_screen";

    public NotificationVibrationIntensityPreferenceController(Context context) {
        super(context, PREF_KEY, "notification_vibration_intensity");
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    protected int getDefaultIntensity() {
        return this.mVibrator.getDefaultNotificationVibrationIntensity();
    }
}
