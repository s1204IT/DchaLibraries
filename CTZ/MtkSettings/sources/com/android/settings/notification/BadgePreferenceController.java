package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;

public class BadgePreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public BadgePreferenceController(Context context, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
    }

    @Override
    public String getPreferenceKey() {
        return "badge";
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if ((this.mAppRow == null && this.mChannel == null) || Settings.Secure.getInt(this.mContext.getContentResolver(), "notification_badging", 1) == 0) {
            return false;
        }
        if (this.mChannel == null || isDefaultChannel()) {
            return true;
        }
        return this.mAppRow.showBadge;
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mAppRow != null) {
            RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
            restrictedSwitchPreference.setDisabledByAdmin(this.mAdmin);
            if (this.mChannel != null) {
                restrictedSwitchPreference.setChecked(this.mChannel.canShowBadge());
                restrictedSwitchPreference.setEnabled(isChannelConfigurable() && !restrictedSwitchPreference.isDisabledByAdmin());
            } else {
                restrictedSwitchPreference.setChecked(this.mAppRow.showBadge);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (this.mChannel != null) {
            this.mChannel.setShowBadge(zBooleanValue);
            saveChannel();
            return true;
        }
        if (this.mAppRow != null) {
            this.mAppRow.showBadge = zBooleanValue;
            this.mBackend.setShowBadge(this.mAppRow.pkg, this.mAppRow.uid, zBooleanValue);
            return true;
        }
        return true;
    }
}
