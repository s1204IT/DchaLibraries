package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;

/* loaded from: classes.dex */
public class DndPreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public DndPreferenceController(Context context, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bypass_dnd";
    }

    @Override // com.android.settings.notification.NotificationPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (!super.isAvailable() || this.mChannel == null) {
            return false;
        }
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mChannel != null) {
            RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
            restrictedSwitchPreference.setDisabledByAdmin(this.mAdmin);
            restrictedSwitchPreference.setEnabled(isChannelConfigurable() && !restrictedSwitchPreference.isDisabledByAdmin());
            restrictedSwitchPreference.setChecked(this.mChannel.canBypassDnd());
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            this.mChannel.setBypassDnd(((Boolean) obj).booleanValue());
            this.mChannel.lockFields(1);
            saveChannel();
        }
        return true;
    }
}
