package com.android.deskclock.data;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;

final class AlarmModel {
    private Uri mDefaultAlarmRingtoneUri;
    private final SettingsModel mSettingsModel;

    AlarmModel(Context context, SettingsModel settingsModel) {
        this.mSettingsModel = settingsModel;
        context.getContentResolver().registerContentObserver(Settings.System.DEFAULT_ALARM_ALERT_URI, false, new SystemAlarmAlertChangeObserver());
    }

    Uri getDefaultAlarmRingtoneUri() {
        if (this.mDefaultAlarmRingtoneUri == null) {
            this.mDefaultAlarmRingtoneUri = this.mSettingsModel.getDefaultAlarmRingtoneUri();
        }
        return this.mDefaultAlarmRingtoneUri;
    }

    void setDefaultAlarmRingtoneUri(Uri uri) {
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            this.mSettingsModel.setDefaultAlarmRingtoneUri(uri);
            this.mDefaultAlarmRingtoneUri = uri;
        }
    }

    long getAlarmCrescendoDuration() {
        return this.mSettingsModel.getAlarmCrescendoDuration();
    }

    DataModel.AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        return this.mSettingsModel.getAlarmVolumeButtonBehavior();
    }

    int getAlarmTimeout() {
        return this.mSettingsModel.getAlarmTimeout();
    }

    int getSnoozeLength() {
        return this.mSettingsModel.getSnoozeLength();
    }

    private final class SystemAlarmAlertChangeObserver extends ContentObserver {
        private SystemAlarmAlertChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            AlarmModel.this.mDefaultAlarmRingtoneUri = null;
        }
    }
}
