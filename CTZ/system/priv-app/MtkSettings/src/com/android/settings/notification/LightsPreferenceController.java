package com.android.settings.notification;

import android.R;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;

/* loaded from: classes.dex */
public class LightsPreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public LightsPreferenceController(Context context, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "lights";
    }

    @Override // com.android.settings.notification.NotificationPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return super.isAvailable() && this.mChannel != null && checkCanBeVisible(3) && canPulseLight() && !isDefaultChannel();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mChannel != null) {
            RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
            restrictedSwitchPreference.setDisabledByAdmin(this.mAdmin);
            restrictedSwitchPreference.setEnabled(isChannelConfigurable() && !restrictedSwitchPreference.isDisabledByAdmin());
            restrictedSwitchPreference.setChecked(this.mChannel.shouldShowLights());
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            this.mChannel.enableLights(((Boolean) obj).booleanValue());
            saveChannel();
            return true;
        }
        return true;
    }

    boolean canPulseLight() {
        return this.mContext.getResources().getBoolean(R.^attr-private.headerLayout) && Settings.System.getInt(this.mContext.getContentResolver(), "notification_light_pulse", 0) == 1;
    }
}
