package com.android.settings.notification;

import android.app.NotificationChannel;
import android.content.Context;
import android.media.RingtoneManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationSettingsBase;

public class ImportancePreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private NotificationSettingsBase.ImportanceListener mImportanceListener;

    public ImportancePreferenceController(Context context, NotificationSettingsBase.ImportanceListener importanceListener, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
        this.mImportanceListener = importanceListener;
    }

    @Override
    public String getPreferenceKey() {
        return "importance";
    }

    @Override
    public boolean isAvailable() {
        if (super.isAvailable() && this.mChannel != null) {
            return !isDefaultChannel();
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mAppRow != null && this.mChannel != null) {
            int i = 0;
            preference.setEnabled(this.mAdmin == null && isChannelConfigurable());
            preference.setSummary(getImportanceSummary(this.mChannel));
            CharSequence[] charSequenceArr = new CharSequence[4];
            CharSequence[] charSequenceArr2 = new CharSequence[4];
            for (int i2 = 4; i2 >= 1; i2--) {
                charSequenceArr[i] = getImportanceSummary(new NotificationChannel("", "", i2));
                charSequenceArr2[i] = String.valueOf(i2);
                i++;
            }
            RestrictedListPreference restrictedListPreference = (RestrictedListPreference) preference;
            restrictedListPreference.setEntries(charSequenceArr);
            restrictedListPreference.setEntryValues(charSequenceArr2);
            restrictedListPreference.setValue(String.valueOf(this.mChannel.getImportance()));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            int i = Integer.parseInt((String) obj);
            if (this.mChannel.getImportance() < 3 && !SoundPreferenceController.hasValidSound(this.mChannel) && i >= 3) {
                this.mChannel.setSound(RingtoneManager.getDefaultUri(2), this.mChannel.getAudioAttributes());
                this.mChannel.lockFields(32);
            }
            this.mChannel.setImportance(i);
            this.mChannel.lockFields(4);
            saveChannel();
            this.mImportanceListener.onImportanceChanged();
            return true;
        }
        return true;
    }

    protected String getImportanceSummary(NotificationChannel notificationChannel) {
        int importance = notificationChannel.getImportance();
        if (importance == -1000) {
            return this.mContext.getString(R.string.notification_importance_unspecified);
        }
        switch (importance) {
            case 1:
                return this.mContext.getString(R.string.notification_importance_min);
            case 2:
                return this.mContext.getString(R.string.notification_importance_low);
            case 3:
                if (SoundPreferenceController.hasValidSound(notificationChannel)) {
                    return this.mContext.getString(R.string.notification_importance_default);
                }
                return this.mContext.getString(R.string.notification_importance_low);
            case 4:
            case 5:
                if (SoundPreferenceController.hasValidSound(notificationChannel)) {
                    return this.mContext.getString(R.string.notification_importance_high);
                }
                return this.mContext.getString(R.string.notification_importance_high_silent);
            default:
                return "";
        }
    }
}
