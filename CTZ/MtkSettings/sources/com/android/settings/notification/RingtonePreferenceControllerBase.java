package com.android.settings.notification;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class RingtonePreferenceControllerBase extends AbstractPreferenceController implements PreferenceControllerMixin {
    public abstract int getRingtoneType();

    public RingtonePreferenceControllerBase(Context context) {
        super(context);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        String title = Ringtone.getTitle(this.mContext, RingtoneManager.getActualDefaultRingtoneUri(this.mContext, getRingtoneType()), false, true);
        if (title != null) {
            preference.setSummary(title);
        }
    }
}
