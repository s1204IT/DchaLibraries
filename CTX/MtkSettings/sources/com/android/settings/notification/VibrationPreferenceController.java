package com.android.settings.notification;

import android.content.Context;
import android.os.Vibrator;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;

public class VibrationPreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final Vibrator mVibrator;

    public VibrationPreferenceController(Context context, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
    }

    @Override
    public String getPreferenceKey() {
        return "vibrate";
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && this.mChannel != null && checkCanBeVisible(3) && !isDefaultChannel() && this.mVibrator != null && this.mVibrator.hasVibrator();
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mChannel != null) {
            RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
            restrictedSwitchPreference.setDisabledByAdmin(this.mAdmin);
            restrictedSwitchPreference.setEnabled(!restrictedSwitchPreference.isDisabledByAdmin() && isChannelConfigurable());
            restrictedSwitchPreference.setChecked(this.mChannel.shouldVibrate());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            this.mChannel.enableVibration(((Boolean) obj).booleanValue());
            saveChannel();
            return true;
        }
        return true;
    }
}
