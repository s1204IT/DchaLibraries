package com.android.deskclock.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.provider.ClockContract;
import com.google.android.flexbox.BuildConfig;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class Alarm implements Parcelable, ClockContract.AlarmsColumns {
    private static final int ALARM_JOIN_INSTANCE_COLUMN_COUNT = 18;
    private static final int COLUMN_COUNT = 9;
    private static final int DAYS_OF_WEEK_INDEX = 3;
    private static final String DEFAULT_SORT_ORDER = "alarm_templates.hour, alarm_templates.minutes ASC, alarm_templates._id DESC";
    private static final int DELETE_AFTER_USE_INDEX = 8;
    private static final int ENABLED_INDEX = 4;
    private static final int HOUR_INDEX = 1;
    private static final int ID_INDEX = 0;
    public static final int INSTANCE_DAY_INDEX = 13;
    public static final int INSTANCE_HOUR_INDEX = 14;
    public static final int INSTANCE_ID_INDEX = 10;
    public static final int INSTANCE_LABEL_INDEX = 16;
    public static final int INSTANCE_MINUTE_INDEX = 15;
    public static final int INSTANCE_MONTH_INDEX = 12;
    private static final int INSTANCE_STATE_INDEX = 9;
    public static final int INSTANCE_VIBRATE_INDEX = 17;
    public static final int INSTANCE_YEAR_INDEX = 11;
    public static final long INVALID_ID = -1;
    private static final int LABEL_INDEX = 6;
    private static final int MINUTES_INDEX = 2;
    private static final int RINGTONE_INDEX = 7;
    private static final int VIBRATE_INDEX = 5;
    public Uri alert;
    public Weekdays daysOfWeek;
    public boolean deleteAfterUse;
    public boolean enabled;
    public int hour;
    public long id;
    public int instanceId;
    public int instanceState;
    public String label;
    public int minutes;
    public boolean vibrate;
    private static final String[] QUERY_COLUMNS = {"_id", "hour", "minutes", ClockContract.AlarmsColumns.DAYS_OF_WEEK, ClockContract.AlarmsColumns.ENABLED, ClockContract.AlarmSettingColumns.VIBRATE, ClockContract.AlarmSettingColumns.LABEL, ClockContract.AlarmSettingColumns.RINGTONE, ClockContract.AlarmsColumns.DELETE_AFTER_USE};
    private static final String[] QUERY_ALARMS_WITH_INSTANCES_COLUMNS = {"alarm_templates._id", "alarm_templates.hour", "alarm_templates.minutes", "alarm_templates.daysofweek", "alarm_templates.enabled", "alarm_templates.vibrate", "alarm_templates.label", "alarm_templates.ringtone", "alarm_templates.delete_after_use", "alarm_instances.alarm_state", "alarm_instances._id", "alarm_instances.year", "alarm_instances.month", "alarm_instances.day", "alarm_instances.hour", "alarm_instances.minutes", "alarm_instances.label", "alarm_instances.vibrate"};
    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel parcel) {
            return new Alarm(parcel);
        }

        @Override
        public Alarm[] newArray(int i) {
            return new Alarm[i];
        }
    };

    public static ContentValues createContentValues(Alarm alarm) {
        ContentValues contentValues = new ContentValues(9);
        if (alarm.id != -1) {
            contentValues.put("_id", Long.valueOf(alarm.id));
        }
        contentValues.put(ClockContract.AlarmsColumns.ENABLED, Integer.valueOf(alarm.enabled ? 1 : 0));
        contentValues.put("hour", Integer.valueOf(alarm.hour));
        contentValues.put("minutes", Integer.valueOf(alarm.minutes));
        contentValues.put(ClockContract.AlarmsColumns.DAYS_OF_WEEK, Integer.valueOf(alarm.daysOfWeek.getBits()));
        contentValues.put(ClockContract.AlarmSettingColumns.VIBRATE, Integer.valueOf(alarm.vibrate ? 1 : 0));
        contentValues.put(ClockContract.AlarmSettingColumns.LABEL, alarm.label);
        contentValues.put(ClockContract.AlarmsColumns.DELETE_AFTER_USE, Boolean.valueOf(alarm.deleteAfterUse));
        if (alarm.alert == null) {
            contentValues.putNull(ClockContract.AlarmSettingColumns.RINGTONE);
        } else {
            contentValues.put(ClockContract.AlarmSettingColumns.RINGTONE, alarm.alert.toString());
        }
        return contentValues;
    }

    public static Intent createIntent(Context context, Class<?> cls, long j) {
        return new Intent(context, cls).setData(getContentUri(j));
    }

    public static Uri getContentUri(long j) {
        return ContentUris.withAppendedId(CONTENT_URI, j);
    }

    public static long getId(Uri uri) {
        return ContentUris.parseId(uri);
    }

    public static CursorLoader getAlarmsCursorLoader(Context context) {
        return new CursorLoader(context, ALARMS_WITH_INSTANCES_URI, QUERY_ALARMS_WITH_INSTANCES_COLUMNS, null, null, DEFAULT_SORT_ORDER) {
            @Override
            public void onContentChanged() {
                if (isStarted() && !isAbandoned()) {
                    stopLoading();
                    super.onContentChanged();
                    startLoading();
                    return;
                }
                super.onContentChanged();
            }

            @Override
            public Cursor loadInBackground() {
                DataModel.getDataModel().loadRingtoneTitles();
                return super.loadInBackground();
            }
        };
    }

    public static Alarm getAlarm(ContentResolver contentResolver, long j) throws Exception {
        Cursor cursorQuery = contentResolver.query(getContentUri(j), QUERY_COLUMNS, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                return new Alarm(cursorQuery);
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return null;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static Alarm getAlarm(ContentResolver contentResolver, Uri uri) {
        return getAlarm(contentResolver, ContentUris.parseId(uri));
    }

    public static List<Alarm> getAlarms(ContentResolver contentResolver, String str, String... strArr) throws Exception {
        LinkedList linkedList = new LinkedList();
        Cursor cursorQuery = contentResolver.query(CONTENT_URI, QUERY_COLUMNS, str, strArr, null);
        Throwable th = null;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    do {
                        linkedList.add(new Alarm(cursorQuery));
                    } while (cursorQuery.moveToNext());
                }
            } finally {
                if (cursorQuery != null) {
                    $closeResource(th, cursorQuery);
                }
            }
        }
        return linkedList;
    }

    public static boolean isTomorrow(Alarm alarm, Calendar calendar) {
        return alarm.instanceState != 4 && (alarm.hour * 60) + alarm.minutes <= (calendar.get(11) * 60) + calendar.get(12);
    }

    public static Alarm addAlarm(ContentResolver contentResolver, Alarm alarm) {
        alarm.id = getId(contentResolver.insert(CONTENT_URI, createContentValues(alarm)));
        return alarm;
    }

    public static boolean updateAlarm(ContentResolver contentResolver, Alarm alarm) {
        if (alarm.id == -1) {
            return false;
        }
        return ((long) contentResolver.update(getContentUri(alarm.id), createContentValues(alarm), null, null)) == 1;
    }

    public static boolean deleteAlarm(ContentResolver contentResolver, long j) {
        return j != -1 && contentResolver.delete(getContentUri(j), BuildConfig.FLAVOR, null) == 1;
    }

    public Alarm() {
        this(0, 0);
    }

    public Alarm(int i, int i2) {
        this.id = -1L;
        this.hour = i;
        this.minutes = i2;
        this.vibrate = true;
        this.daysOfWeek = Weekdays.NONE;
        this.label = BuildConfig.FLAVOR;
        this.alert = DataModel.getDataModel().getDefaultAlarmRingtoneUri();
        this.deleteAfterUse = false;
    }

    public Alarm(Cursor cursor) {
        this.id = cursor.getLong(0);
        this.enabled = cursor.getInt(4) == 1;
        this.hour = cursor.getInt(1);
        this.minutes = cursor.getInt(2);
        this.daysOfWeek = Weekdays.fromBits(cursor.getInt(3));
        this.vibrate = cursor.getInt(5) == 1;
        this.label = cursor.getString(6);
        this.deleteAfterUse = cursor.getInt(8) == 1;
        if (cursor.getColumnCount() == 18) {
            this.instanceState = cursor.getInt(9);
            this.instanceId = cursor.getInt(10);
        }
        if (cursor.isNull(7)) {
            this.alert = RingtoneManager.getDefaultUri(4);
        } else {
            this.alert = Uri.parse(cursor.getString(7));
        }
    }

    Alarm(Parcel parcel) {
        this.id = parcel.readLong();
        this.enabled = parcel.readInt() == 1;
        this.hour = parcel.readInt();
        this.minutes = parcel.readInt();
        this.daysOfWeek = Weekdays.fromBits(parcel.readInt());
        this.vibrate = parcel.readInt() == 1;
        this.label = parcel.readString();
        this.alert = (Uri) parcel.readParcelable(null);
        this.deleteAfterUse = parcel.readInt() == 1;
    }

    public Uri getContentUri() {
        return getContentUri(this.id);
    }

    public String getLabelOrDefault(Context context) {
        return this.label.isEmpty() ? context.getString(R.string.default_label) : this.label;
    }

    public boolean canPreemptivelyDismiss() {
        return this.instanceState == 4 || this.instanceState == 3 || this.instanceState == 1 || this.instanceState == 2;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.id);
        parcel.writeInt(this.enabled ? 1 : 0);
        parcel.writeInt(this.hour);
        parcel.writeInt(this.minutes);
        parcel.writeInt(this.daysOfWeek.getBits());
        parcel.writeInt(this.vibrate ? 1 : 0);
        parcel.writeString(this.label);
        parcel.writeParcelable(this.alert, i);
        parcel.writeInt(this.deleteAfterUse ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public AlarmInstance createInstanceAfter(Calendar calendar) {
        AlarmInstance alarmInstance = new AlarmInstance(getNextAlarmTime(calendar), Long.valueOf(this.id));
        alarmInstance.mVibrate = this.vibrate;
        alarmInstance.mLabel = this.label;
        alarmInstance.mRingtone = this.alert;
        return alarmInstance;
    }

    public Calendar getPreviousAlarmTime(Calendar calendar) {
        Calendar calendar2 = Calendar.getInstance(calendar.getTimeZone());
        calendar2.set(1, calendar.get(1));
        calendar2.set(2, calendar.get(2));
        calendar2.set(5, calendar.get(5));
        calendar2.set(11, this.hour);
        calendar2.set(12, this.minutes);
        calendar2.set(13, 0);
        calendar2.set(14, 0);
        int distanceToPreviousDay = this.daysOfWeek.getDistanceToPreviousDay(calendar2);
        if (distanceToPreviousDay > 0) {
            calendar2.add(7, -distanceToPreviousDay);
            return calendar2;
        }
        return null;
    }

    public Calendar getNextAlarmTime(Calendar calendar) {
        Calendar calendar2 = Calendar.getInstance(calendar.getTimeZone());
        calendar2.set(1, calendar.get(1));
        calendar2.set(2, calendar.get(2));
        calendar2.set(5, calendar.get(5));
        calendar2.set(11, this.hour);
        calendar2.set(12, this.minutes);
        calendar2.set(13, 0);
        calendar2.set(14, 0);
        if (calendar2.getTimeInMillis() <= calendar.getTimeInMillis()) {
            calendar2.add(6, 1);
        }
        int distanceToNextDay = this.daysOfWeek.getDistanceToNextDay(calendar2);
        if (distanceToNextDay > 0) {
            calendar2.add(7, distanceToNextDay);
        }
        calendar2.set(11, this.hour);
        calendar2.set(12, this.minutes);
        return calendar2;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Alarm) && this.id == ((Alarm) obj).id;
    }

    public int hashCode() {
        return Long.valueOf(this.id).hashCode();
    }

    public String toString() {
        return "Alarm{alert=" + this.alert + ", id=" + this.id + ", enabled=" + this.enabled + ", hour=" + this.hour + ", minutes=" + this.minutes + ", daysOfWeek=" + this.daysOfWeek + ", vibrate=" + this.vibrate + ", label='" + this.label + "', deleteAfterUse=" + this.deleteAfterUse + '}';
    }
}
