package com.android.deskclock.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import java.util.TimeZone;

final class SettingsModel {
    private final Context mContext;
    private Uri mDefaultTimerRingtoneUri;
    private final SharedPreferences mPrefs;
    private final TimeModel mTimeModel;

    SettingsModel(Context context, SharedPreferences sharedPreferences, TimeModel timeModel) {
        this.mContext = context;
        this.mPrefs = sharedPreferences;
        this.mTimeModel = timeModel;
        SettingsDAO.setDefaultDisplayClockSeconds(this.mContext, sharedPreferences);
    }

    int getGlobalIntentId() {
        return SettingsDAO.getGlobalIntentId(this.mPrefs);
    }

    void updateGlobalIntentId() {
        SettingsDAO.updateGlobalIntentId(this.mPrefs);
    }

    DataModel.CitySort getCitySort() {
        return SettingsDAO.getCitySort(this.mPrefs);
    }

    void toggleCitySort() {
        SettingsDAO.toggleCitySort(this.mPrefs);
    }

    TimeZone getHomeTimeZone() {
        return SettingsDAO.getHomeTimeZone(this.mContext, this.mPrefs, TimeZone.getDefault());
    }

    DataModel.ClockStyle getClockStyle() {
        return SettingsDAO.getClockStyle(this.mContext, this.mPrefs);
    }

    boolean getDisplayClockSeconds() {
        return SettingsDAO.getDisplayClockSeconds(this.mPrefs);
    }

    void setDisplayClockSeconds(boolean z) {
        SettingsDAO.setDisplayClockSeconds(this.mPrefs, z);
    }

    DataModel.ClockStyle getScreensaverClockStyle() {
        return SettingsDAO.getScreensaverClockStyle(this.mContext, this.mPrefs);
    }

    boolean getScreensaverNightModeOn() {
        return SettingsDAO.getScreensaverNightModeOn(this.mPrefs);
    }

    boolean getShowHomeClock() {
        if (!SettingsDAO.getAutoShowHomeClock(this.mPrefs)) {
            return false;
        }
        TimeZone timeZone = TimeZone.getDefault();
        TimeZone homeTimeZone = SettingsDAO.getHomeTimeZone(this.mContext, this.mPrefs, timeZone);
        long jCurrentTimeMillis = System.currentTimeMillis();
        return homeTimeZone.getOffset(jCurrentTimeMillis) != timeZone.getOffset(jCurrentTimeMillis);
    }

    Uri getDefaultTimerRingtoneUri() {
        if (this.mDefaultTimerRingtoneUri == null) {
            this.mDefaultTimerRingtoneUri = Utils.getResourceUri(this.mContext, R.raw.timer_expire);
        }
        return this.mDefaultTimerRingtoneUri;
    }

    void setTimerRingtoneUri(Uri uri) {
        SettingsDAO.setTimerRingtoneUri(this.mPrefs, uri);
    }

    Uri getTimerRingtoneUri() {
        return SettingsDAO.getTimerRingtoneUri(this.mPrefs, getDefaultTimerRingtoneUri());
    }

    DataModel.AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        return SettingsDAO.getAlarmVolumeButtonBehavior(this.mPrefs);
    }

    int getAlarmTimeout() {
        return SettingsDAO.getAlarmTimeout(this.mPrefs);
    }

    int getSnoozeLength() {
        return SettingsDAO.getSnoozeLength(this.mPrefs);
    }

    Uri getDefaultAlarmRingtoneUri() {
        return SettingsDAO.getDefaultAlarmRingtoneUri(this.mPrefs);
    }

    void setDefaultAlarmRingtoneUri(Uri uri) {
        SettingsDAO.setDefaultAlarmRingtoneUri(this.mPrefs, uri);
    }

    long getAlarmCrescendoDuration() {
        return SettingsDAO.getAlarmCrescendoDuration(this.mPrefs);
    }

    long getTimerCrescendoDuration() {
        return SettingsDAO.getTimerCrescendoDuration(this.mPrefs);
    }

    Weekdays.Order getWeekdayOrder() {
        return SettingsDAO.getWeekdayOrder(this.mPrefs);
    }

    boolean isRestoreBackupFinished() {
        return SettingsDAO.isRestoreBackupFinished(this.mPrefs);
    }

    void setRestoreBackupFinished(boolean z) {
        SettingsDAO.setRestoreBackupFinished(this.mPrefs, z);
    }

    boolean getTimerVibrate() {
        return SettingsDAO.getTimerVibrate(this.mPrefs);
    }

    void setTimerVibrate(boolean z) {
        SettingsDAO.setTimerVibrate(this.mPrefs, z);
    }

    TimeZones getTimeZones() {
        return SettingsDAO.getTimeZones(this.mContext, this.mTimeModel.currentTimeMillis());
    }
}
