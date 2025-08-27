package com.android.settings.accessibility;

import android.content.Context;

/* loaded from: classes.dex */
public class NotificationVibrationIntensityPreferenceController extends VibrationIntensityPreferenceController {
    static final String PREF_KEY = "notification_vibration_preference_screen";

    public NotificationVibrationIntensityPreferenceController(Context context) {
        super(context, PREF_KEY, "notification_vibration_intensity");
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settings.accessibility.VibrationIntensityPreferenceController
    protected int getDefaultIntensity() {
        return this.mVibrator.getDefaultNotificationVibrationIntensity();
    }
}
