package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationSettingsBase;
import com.android.settingslib.RestrictedSwitchPreference;

public class AllowSoundPreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private NotificationSettingsBase.ImportanceListener mImportanceListener;

    public AllowSoundPreferenceController(Context context, NotificationSettingsBase.ImportanceListener importanceListener, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
        this.mImportanceListener = importanceListener;
    }

    @Override
    public String getPreferenceKey() {
        return "allow_sound";
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && this.mChannel != null && "miscellaneous".equals(this.mChannel.getId());
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mChannel != null) {
            RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
            restrictedSwitchPreference.setDisabledByAdmin(this.mAdmin);
            restrictedSwitchPreference.setEnabled(isChannelConfigurable() && !restrictedSwitchPreference.isDisabledByAdmin());
            restrictedSwitchPreference.setChecked(this.mChannel.getImportance() >= 3 || this.mChannel.getImportance() == -1000);
            return;
        }
        Log.i("AllowSoundPrefContr", "tried to updatestate on a null channel?!");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            this.mChannel.setImportance(((Boolean) obj).booleanValue() ? -1000 : 2);
            this.mChannel.lockFields(4);
            saveChannel();
            this.mImportanceListener.onImportanceChanged();
            return true;
        }
        return true;
    }
}
