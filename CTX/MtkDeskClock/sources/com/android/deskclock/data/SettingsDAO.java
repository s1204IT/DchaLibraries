package com.android.deskclock.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.settings.ScreensaverSettingsActivity;
import com.android.deskclock.settings.SettingsActivity;
import com.google.android.flexbox.BuildConfig;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

final class SettingsDAO {
    private static final String KEY_ALARM_GLOBAL_ID = "intent.extra.alarm.global.id";
    private static final String KEY_DEFAULT_ALARM_RINGTONE_URI = "default_alarm_ringtone_uri";
    private static final String KEY_RESTORE_BACKUP_FINISHED = "restore_finished";
    private static final String KEY_SORT_PREFERENCE = "sort_preference";

    private SettingsDAO() {
    }

    static int getGlobalIntentId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt(KEY_ALARM_GLOBAL_ID, -1);
    }

    static void updateGlobalIntentId(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(KEY_ALARM_GLOBAL_ID, sharedPreferences.getInt(KEY_ALARM_GLOBAL_ID, -1) + 1).apply();
    }

    static DataModel.CitySort getCitySort(SharedPreferences sharedPreferences) {
        return DataModel.CitySort.values()[sharedPreferences.getInt(KEY_SORT_PREFERENCE, DataModel.CitySort.NAME.ordinal())];
    }

    static void toggleCitySort(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt(KEY_SORT_PREFERENCE, (getCitySort(sharedPreferences) == DataModel.CitySort.NAME ? DataModel.CitySort.UTC_OFFSET : DataModel.CitySort.NAME).ordinal()).apply();
    }

    static boolean getAutoShowHomeClock(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, true);
    }

    static TimeZone getHomeTimeZone(Context context, SharedPreferences sharedPreferences, TimeZone timeZone) {
        String string = sharedPreferences.getString(SettingsActivity.KEY_HOME_TZ, null);
        TimeZones timeZones = getTimeZones(context, System.currentTimeMillis());
        if (timeZones.contains(string)) {
            return TimeZone.getTimeZone(string);
        }
        String id = timeZone.getID();
        if (timeZones.contains(id)) {
            sharedPreferences.edit().putString(SettingsActivity.KEY_HOME_TZ, id).apply();
        }
        return timeZone;
    }

    static DataModel.ClockStyle getClockStyle(Context context, SharedPreferences sharedPreferences) {
        return getClockStyle(context, sharedPreferences, SettingsActivity.KEY_CLOCK_STYLE);
    }

    static boolean getDisplayClockSeconds(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS, false);
    }

    static void setDisplayClockSeconds(SharedPreferences sharedPreferences, boolean z) {
        sharedPreferences.edit().putBoolean(SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS, z).apply();
    }

    static void setDefaultDisplayClockSeconds(Context context, SharedPreferences sharedPreferences) {
        if (!sharedPreferences.contains(SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS)) {
            setDisplayClockSeconds(sharedPreferences, getClockStyle(context, sharedPreferences) == DataModel.ClockStyle.ANALOG);
        }
    }

    static DataModel.ClockStyle getScreensaverClockStyle(Context context, SharedPreferences sharedPreferences) {
        return getClockStyle(context, sharedPreferences, ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
    }

    static boolean getScreensaverNightModeOn(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(ScreensaverSettingsActivity.KEY_NIGHT_MODE, false);
    }

    static Uri getTimerRingtoneUri(SharedPreferences sharedPreferences, Uri uri) {
        String string = sharedPreferences.getString(SettingsActivity.KEY_TIMER_RINGTONE, null);
        return string == null ? uri : Uri.parse(string);
    }

    static boolean getTimerVibrate(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(SettingsActivity.KEY_TIMER_VIBRATE, false);
    }

    static void setTimerVibrate(SharedPreferences sharedPreferences, boolean z) {
        sharedPreferences.edit().putBoolean(SettingsActivity.KEY_TIMER_VIBRATE, z).apply();
    }

    static void setTimerRingtoneUri(SharedPreferences sharedPreferences, Uri uri) {
        sharedPreferences.edit().putString(SettingsActivity.KEY_TIMER_RINGTONE, uri.toString()).apply();
    }

    static Uri getDefaultAlarmRingtoneUri(SharedPreferences sharedPreferences) {
        String string = sharedPreferences.getString(KEY_DEFAULT_ALARM_RINGTONE_URI, null);
        return string == null ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(string);
    }

    static void setDefaultAlarmRingtoneUri(SharedPreferences sharedPreferences, Uri uri) {
        if (uri != null) {
            sharedPreferences.edit().putString(KEY_DEFAULT_ALARM_RINGTONE_URI, uri.toString()).apply();
        }
    }

    static long getAlarmCrescendoDuration(SharedPreferences sharedPreferences) {
        return ((long) Integer.parseInt(sharedPreferences.getString(SettingsActivity.KEY_ALARM_CRESCENDO, SettingsActivity.DEFAULT_VOLUME_BEHAVIOR))) * 1000;
    }

    static long getTimerCrescendoDuration(SharedPreferences sharedPreferences) {
        return ((long) Integer.parseInt(sharedPreferences.getString(SettingsActivity.KEY_TIMER_CRESCENDO, SettingsActivity.DEFAULT_VOLUME_BEHAVIOR))) * 1000;
    }

    static Weekdays.Order getWeekdayOrder(SharedPreferences sharedPreferences) {
        int i = Integer.parseInt(sharedPreferences.getString(SettingsActivity.KEY_WEEK_START, String.valueOf(Calendar.getInstance().getFirstDayOfWeek())));
        if (i == 7) {
            return Weekdays.Order.SAT_TO_FRI;
        }
        switch (i) {
            case 1:
                return Weekdays.Order.SUN_TO_SAT;
            case 2:
                return Weekdays.Order.MON_TO_SUN;
            default:
                throw new IllegalArgumentException("Unknown weekday: " + i);
        }
    }

    static boolean isRestoreBackupFinished(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(KEY_RESTORE_BACKUP_FINISHED, false);
    }

    static void setRestoreBackupFinished(SharedPreferences sharedPreferences, boolean z) {
        if (z) {
            sharedPreferences.edit().putBoolean(KEY_RESTORE_BACKUP_FINISHED, true).apply();
        } else {
            sharedPreferences.edit().remove(KEY_RESTORE_BACKUP_FINISHED).apply();
        }
    }

    static DataModel.AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior(SharedPreferences sharedPreferences) {
        String string;
        string = sharedPreferences.getString(SettingsActivity.KEY_VOLUME_BUTTONS, SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        switch (string) {
            case "0":
                return DataModel.AlarmVolumeButtonBehavior.NOTHING;
            case "1":
                return DataModel.AlarmVolumeButtonBehavior.SNOOZE;
            case "2":
                return DataModel.AlarmVolumeButtonBehavior.DISMISS;
            default:
                throw new IllegalArgumentException("Unknown volume button behavior: " + string);
        }
    }

    static int getAlarmTimeout(SharedPreferences sharedPreferences) {
        return Integer.parseInt(sharedPreferences.getString(SettingsActivity.KEY_AUTO_SILENCE, "10"));
    }

    static int getSnoozeLength(SharedPreferences sharedPreferences) {
        return Integer.parseInt(sharedPreferences.getString(SettingsActivity.KEY_ALARM_SNOOZE, "10"));
    }

    static TimeZones getTimeZones(Context context, long j) {
        Locale locale = Locale.getDefault();
        Resources resources = context.getResources();
        String[] stringArray = resources.getStringArray(R.array.timezone_values);
        String[] stringArray2 = resources.getStringArray(R.array.timezone_labels);
        if (stringArray.length != stringArray2.length) {
            throw new IllegalStateException(String.format(Locale.US, "id count (%d) does not match name count (%d) for locale %s", Integer.valueOf(stringArray.length), Integer.valueOf(stringArray2.length), locale));
        }
        TimeZoneDescriptor[] timeZoneDescriptorArr = new TimeZoneDescriptor[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            timeZoneDescriptorArr[i] = new TimeZoneDescriptor(locale, stringArray[i], stringArray2[i].replaceAll("\"", BuildConfig.FLAVOR), j);
        }
        Arrays.sort(timeZoneDescriptorArr);
        CharSequence[] charSequenceArr = new CharSequence[timeZoneDescriptorArr.length];
        CharSequence[] charSequenceArr2 = new CharSequence[timeZoneDescriptorArr.length];
        for (int i2 = 0; i2 < timeZoneDescriptorArr.length; i2++) {
            TimeZoneDescriptor timeZoneDescriptor = timeZoneDescriptorArr[i2];
            charSequenceArr[i2] = timeZoneDescriptor.mTimeZoneId;
            charSequenceArr2[i2] = timeZoneDescriptor.mTimeZoneName;
        }
        return new TimeZones(charSequenceArr, charSequenceArr2);
    }

    private static DataModel.ClockStyle getClockStyle(Context context, SharedPreferences sharedPreferences, String str) {
        return DataModel.ClockStyle.valueOf(sharedPreferences.getString(str, context.getString(R.string.default_clock_style)).toUpperCase(Locale.US));
    }

    private static class TimeZoneDescriptor implements Comparable<TimeZoneDescriptor> {
        private final int mOffset;
        private final String mTimeZoneId;
        private final String mTimeZoneName;

        private TimeZoneDescriptor(Locale locale, String str, String str2, long j) {
            this.mTimeZoneId = str;
            this.mOffset = TimeZone.getTimeZone(str).getOffset(j);
            char c = this.mOffset < 0 ? '-' : '+';
            long jAbs = Math.abs(this.mOffset);
            this.mTimeZoneName = String.format(locale, "(GMT%s%d:%02d) %s", Character.valueOf(c), Long.valueOf(jAbs / 3600000), Long.valueOf((jAbs / 60000) % 60), str2);
        }

        @Override
        public int compareTo(@NonNull TimeZoneDescriptor timeZoneDescriptor) {
            return this.mOffset - timeZoneDescriptor.mOffset;
        }
    }
}
