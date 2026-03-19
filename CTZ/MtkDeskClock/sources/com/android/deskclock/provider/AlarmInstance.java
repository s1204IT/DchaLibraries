package com.android.deskclock.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.ClockContract;
import com.google.android.flexbox.BuildConfig;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class AlarmInstance implements ClockContract.InstancesColumns {
    private static final int ALARM_ID_INDEX = 9;
    private static final int ALARM_STATE_INDEX = 10;
    private static final int COLUMN_COUNT = 11;
    private static final int DAY_INDEX = 3;
    public static final int HIGH_NOTIFICATION_MINUTE_OFFSET = -30;
    private static final int HOUR_INDEX = 4;
    private static final int ID_INDEX = 0;
    public static final long INVALID_ID = -1;
    private static final int LABEL_INDEX = 6;
    public static final int LOW_NOTIFICATION_HOUR_OFFSET = -2;
    private static final int MINUTES_INDEX = 5;
    private static final int MISSED_TIME_TO_LIVE_HOUR_OFFSET = 12;
    private static final int MONTH_INDEX = 2;
    private static final String[] QUERY_COLUMNS = {"_id", ClockContract.InstancesColumns.YEAR, ClockContract.InstancesColumns.MONTH, ClockContract.InstancesColumns.DAY, "hour", "minutes", ClockContract.AlarmSettingColumns.LABEL, ClockContract.AlarmSettingColumns.VIBRATE, ClockContract.AlarmSettingColumns.RINGTONE, ClockContract.InstancesColumns.ALARM_ID, ClockContract.InstancesColumns.ALARM_STATE};
    private static final int RINGTONE_INDEX = 8;
    private static final int VIBRATE_INDEX = 7;
    private static final int YEAR_INDEX = 1;
    public Long mAlarmId;
    public int mAlarmState;
    public int mDay;
    public int mHour;
    public long mId;
    public String mLabel;
    public int mMinute;
    public int mMonth;
    public Uri mRingtone;
    public boolean mVibrate;
    public int mYear;

    public static ContentValues createContentValues(AlarmInstance alarmInstance) {
        ContentValues contentValues = new ContentValues(11);
        if (alarmInstance.mId != -1) {
            contentValues.put("_id", Long.valueOf(alarmInstance.mId));
        }
        contentValues.put(ClockContract.InstancesColumns.YEAR, Integer.valueOf(alarmInstance.mYear));
        contentValues.put(ClockContract.InstancesColumns.MONTH, Integer.valueOf(alarmInstance.mMonth));
        contentValues.put(ClockContract.InstancesColumns.DAY, Integer.valueOf(alarmInstance.mDay));
        contentValues.put("hour", Integer.valueOf(alarmInstance.mHour));
        contentValues.put("minutes", Integer.valueOf(alarmInstance.mMinute));
        contentValues.put(ClockContract.AlarmSettingColumns.LABEL, alarmInstance.mLabel);
        contentValues.put(ClockContract.AlarmSettingColumns.VIBRATE, Integer.valueOf(alarmInstance.mVibrate ? 1 : 0));
        if (alarmInstance.mRingtone == null) {
            contentValues.putNull(ClockContract.AlarmSettingColumns.RINGTONE);
        } else {
            contentValues.put(ClockContract.AlarmSettingColumns.RINGTONE, alarmInstance.mRingtone.toString());
        }
        contentValues.put(ClockContract.InstancesColumns.ALARM_ID, alarmInstance.mAlarmId);
        contentValues.put(ClockContract.InstancesColumns.ALARM_STATE, Integer.valueOf(alarmInstance.mAlarmState));
        return contentValues;
    }

    public static Intent createIntent(String str, long j) {
        return new Intent(str).setData(getContentUri(j));
    }

    public static Intent createIntent(Context context, Class<?> cls, long j) {
        return new Intent(context, cls).setData(getContentUri(j));
    }

    public static long getId(Uri uri) {
        return ContentUris.parseId(uri);
    }

    public static Uri getContentUri(long j) {
        return ContentUris.withAppendedId(CONTENT_URI, j);
    }

    public static AlarmInstance getInstance(ContentResolver contentResolver, long j) throws Exception {
        Cursor cursorQuery = contentResolver.query(getContentUri(j), QUERY_COLUMNS, null, null, null);
        try {
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    return new AlarmInstance(cursorQuery, false);
                }
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

    public static AlarmInstance getInstance(ContentResolver contentResolver, Uri uri) {
        return getInstance(contentResolver, ContentUris.parseId(uri));
    }

    public static List<AlarmInstance> getInstancesByAlarmId(ContentResolver contentResolver, long j) {
        return getInstances(contentResolver, "alarm_id=" + j, new String[0]);
    }

    public static AlarmInstance getNextUpcomingInstanceByAlarmId(ContentResolver contentResolver, long j) {
        List<AlarmInstance> instancesByAlarmId = getInstancesByAlarmId(contentResolver, j);
        if (instancesByAlarmId.isEmpty()) {
            return null;
        }
        AlarmInstance alarmInstance = instancesByAlarmId.get(0);
        for (AlarmInstance alarmInstance2 : instancesByAlarmId) {
            if (alarmInstance2.getAlarmTime().before(alarmInstance.getAlarmTime())) {
                alarmInstance = alarmInstance2;
            }
        }
        return alarmInstance;
    }

    public static List<AlarmInstance> getInstancesByInstanceIdAndState(ContentResolver contentResolver, long j, int i) {
        return getInstances(contentResolver, "_id=" + j + " AND " + ClockContract.InstancesColumns.ALARM_STATE + "=" + i, new String[0]);
    }

    public static List<AlarmInstance> getInstancesByState(ContentResolver contentResolver, int i) {
        return getInstances(contentResolver, "alarm_state=" + i, new String[0]);
    }

    public static List<AlarmInstance> getInstances(ContentResolver contentResolver, String str, String... strArr) throws Exception {
        LinkedList linkedList = new LinkedList();
        Cursor cursorQuery = contentResolver.query(CONTENT_URI, QUERY_COLUMNS, str, strArr, null);
        Throwable th = null;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    do {
                        linkedList.add(new AlarmInstance(cursorQuery, false));
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

    public static AlarmInstance addInstance(ContentResolver contentResolver, AlarmInstance alarmInstance) {
        for (AlarmInstance alarmInstance2 : getInstances(contentResolver, "alarm_id = " + alarmInstance.mAlarmId, new String[0])) {
            if (alarmInstance2.getAlarmTime().equals(alarmInstance.getAlarmTime())) {
                LogUtils.i("Detected duplicate instance in DB. Updating " + alarmInstance2 + " to " + alarmInstance, new Object[0]);
                alarmInstance.mId = alarmInstance2.mId;
                updateInstance(contentResolver, alarmInstance);
                return alarmInstance;
            }
        }
        alarmInstance.mId = getId(contentResolver.insert(CONTENT_URI, createContentValues(alarmInstance)));
        return alarmInstance;
    }

    public static boolean updateInstance(ContentResolver contentResolver, AlarmInstance alarmInstance) {
        if (alarmInstance.mId == -1) {
            return false;
        }
        return ((long) contentResolver.update(getContentUri(alarmInstance.mId), createContentValues(alarmInstance), null, null)) == 1;
    }

    public static boolean deleteInstance(ContentResolver contentResolver, long j) {
        return j != -1 && contentResolver.delete(getContentUri(j), BuildConfig.FLAVOR, null) == 1;
    }

    public static void deleteOtherInstances(Context context, ContentResolver contentResolver, long j, long j2) {
        for (AlarmInstance alarmInstance : getInstancesByAlarmId(contentResolver, j)) {
            if (alarmInstance.mId != j2) {
                AlarmStateManager.unregisterInstance(context, alarmInstance);
                deleteInstance(contentResolver, alarmInstance.mId);
            }
        }
    }

    public AlarmInstance(Calendar calendar, Long l) {
        this(calendar);
        this.mAlarmId = l;
    }

    public AlarmInstance(Calendar calendar) {
        this.mId = -1L;
        setAlarmTime(calendar);
        this.mLabel = BuildConfig.FLAVOR;
        this.mVibrate = false;
        this.mRingtone = null;
        this.mAlarmState = 0;
    }

    public AlarmInstance(AlarmInstance alarmInstance) {
        this.mId = alarmInstance.mId;
        this.mYear = alarmInstance.mYear;
        this.mMonth = alarmInstance.mMonth;
        this.mDay = alarmInstance.mDay;
        this.mHour = alarmInstance.mHour;
        this.mMinute = alarmInstance.mMinute;
        this.mLabel = alarmInstance.mLabel;
        this.mVibrate = alarmInstance.mVibrate;
        this.mRingtone = alarmInstance.mRingtone;
        this.mAlarmId = alarmInstance.mAlarmId;
        this.mAlarmState = alarmInstance.mAlarmState;
    }

    public AlarmInstance(Cursor cursor, boolean z) {
        if (z) {
            this.mId = cursor.getLong(10);
            this.mYear = cursor.getInt(11);
            this.mMonth = cursor.getInt(12);
            this.mDay = cursor.getInt(13);
            this.mHour = cursor.getInt(14);
            this.mMinute = cursor.getInt(15);
            this.mLabel = cursor.getString(16);
            this.mVibrate = cursor.getInt(17) == 1;
        } else {
            this.mId = cursor.getLong(0);
            this.mYear = cursor.getInt(1);
            this.mMonth = cursor.getInt(2);
            this.mDay = cursor.getInt(3);
            this.mHour = cursor.getInt(4);
            this.mMinute = cursor.getInt(5);
            this.mLabel = cursor.getString(6);
            this.mVibrate = cursor.getInt(7) == 1;
        }
        if (cursor.isNull(8)) {
            this.mRingtone = RingtoneManager.getDefaultUri(4);
        } else {
            this.mRingtone = Uri.parse(cursor.getString(8));
        }
        if (!cursor.isNull(9)) {
            this.mAlarmId = Long.valueOf(cursor.getLong(9));
        }
        this.mAlarmState = cursor.getInt(10);
    }

    public Uri getContentUri() {
        return getContentUri(this.mId);
    }

    public String getLabelOrDefault(Context context) {
        return this.mLabel.isEmpty() ? context.getString(R.string.default_label) : this.mLabel;
    }

    public void setAlarmTime(Calendar calendar) {
        this.mYear = calendar.get(1);
        this.mMonth = calendar.get(2);
        this.mDay = calendar.get(5);
        this.mHour = calendar.get(11);
        this.mMinute = calendar.get(12);
    }

    public Calendar getAlarmTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1, this.mYear);
        calendar.set(2, this.mMonth);
        calendar.set(5, this.mDay);
        calendar.set(11, this.mHour);
        calendar.set(12, this.mMinute);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar;
    }

    public Calendar getLowNotificationTime() {
        Calendar alarmTime = getAlarmTime();
        alarmTime.add(11, -2);
        return alarmTime;
    }

    public Calendar getHighNotificationTime() {
        Calendar alarmTime = getAlarmTime();
        alarmTime.add(12, -30);
        return alarmTime;
    }

    public Calendar getMissedTimeToLive() {
        Calendar alarmTime = getAlarmTime();
        alarmTime.add(10, 12);
        return alarmTime;
    }

    public Calendar getTimeout() {
        int alarmTimeout = DataModel.getDataModel().getAlarmTimeout();
        if (alarmTimeout < 0) {
            return null;
        }
        Calendar alarmTime = getAlarmTime();
        alarmTime.add(12, alarmTimeout);
        return alarmTime;
    }

    public boolean equals(Object obj) {
        return (obj instanceof AlarmInstance) && this.mId == ((AlarmInstance) obj).mId;
    }

    public int hashCode() {
        return Long.valueOf(this.mId).hashCode();
    }

    public String toString() {
        return "AlarmInstance{mId=" + this.mId + ", mYear=" + this.mYear + ", mMonth=" + this.mMonth + ", mDay=" + this.mDay + ", mHour=" + this.mHour + ", mMinute=" + this.mMinute + ", mLabel=" + this.mLabel + ", mVibrate=" + this.mVibrate + ", mRingtone=" + this.mRingtone + ", mAlarmId=" + this.mAlarmId + ", mAlarmState=" + this.mAlarmState + '}';
    }
}
