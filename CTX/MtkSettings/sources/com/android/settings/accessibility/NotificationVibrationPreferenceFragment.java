package com.android.settings.accessibility;

import android.os.Vibrator;
import com.android.settings.R;

public class NotificationVibrationPreferenceFragment extends VibrationPreferenceFragment {
    @Override
    public int getMetricsCategory() {
        return 1293;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_notification_vibration_settings;
    }

    @Override
    protected String getVibrationIntensitySetting() {
        return "notification_vibration_intensity";
    }

    @Override
    protected int getPreviewVibrationAudioAttributesUsage() {
        return 5;
    }

    @Override
    protected int getDefaultVibrationIntensity() {
        return ((Vibrator) getContext().getSystemService(Vibrator.class)).getDefaultNotificationVibrationIntensity();
    }
}
