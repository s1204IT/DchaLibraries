package com.android.deskclock.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.ClockContract;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class AlarmInstance implements ClockContract.InstancesColumns {
    private static final String[] QUERY_COLUMNS = {"_id", "year", "month", "day", "hour", "minutes", "label", "vibrate", "ringtone", "alarm_id", "alarm_state"};
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

    public static ContentValues createContentValues(AlarmInstance instance) {
        ContentValues values = new ContentValues(11);
        if (instance.mId != -1) {
            values.put("_id", Long.valueOf(instance.mId));
        }
        values.put("year", Integer.valueOf(instance.mYear));
        values.put("month", Integer.valueOf(instance.mMonth));
        values.put("day", Integer.valueOf(instance.mDay));
        values.put("hour", Integer.valueOf(instance.mHour));
        values.put("minutes", Integer.valueOf(instance.mMinute));
        values.put("label", instance.mLabel);
        values.put("vibrate", Integer.valueOf(instance.mVibrate ? 1 : 0));
        if (instance.mRingtone == null) {
            values.putNull("ringtone");
        } else {
            values.put("ringtone", instance.mRingtone.toString());
        }
        values.put("alarm_id", instance.mAlarmId);
        values.put("alarm_state", Integer.valueOf(instance.mAlarmState));
        return values;
    }

    public static Intent createIntent(Context context, Class<?> cls, long instanceId) {
        return new Intent(context, cls).setData(getUri(instanceId));
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    public static Uri getUri(long instanceId) {
        return ContentUris.withAppendedId(CONTENT_URI, instanceId);
    }

    public static AlarmInstance getInstance(ContentResolver contentResolver, long instanceId) {
        Cursor cursor = contentResolver.query(getUri(instanceId), QUERY_COLUMNS, null, null, null);
        AlarmInstance result = null;
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                result = new AlarmInstance(cursor);
            }
            cursor.close();
            return result;
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    public static List<AlarmInstance> getInstancesByAlarmId(ContentResolver contentResolver, long alarmId) {
        return getInstances(contentResolver, "alarm_id=" + alarmId, new String[0]);
    }

    public static List<AlarmInstance> getInstances(ContentResolver contentResolver, String selection, String... selectionArgs) {
        Cursor cursor = contentResolver.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, null);
        List<AlarmInstance> result = new LinkedList<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.add(new AlarmInstance(cursor));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    public static AlarmInstance addInstance(ContentResolver contentResolver, AlarmInstance instance) {
        String dupSelector = "alarm_id = " + instance.mAlarmId;
        Iterator<AlarmInstance> it = getInstances(contentResolver, dupSelector, new String[0]).iterator();
        while (true) {
            if (it.hasNext()) {
                AlarmInstance otherInstances = it.next();
                if (otherInstances.getAlarmTime().equals(instance.getAlarmTime())) {
                    LogUtils.i("Detected duplicate instance in DB. Updating " + otherInstances + " to " + instance, new Object[0]);
                    instance.mId = otherInstances.mId;
                    updateInstance(contentResolver, instance);
                    break;
                }
            } else {
                ContentValues values = createContentValues(instance);
                Uri uri = contentResolver.insert(CONTENT_URI, values);
                instance.mId = getId(uri);
                break;
            }
        }
        return instance;
    }

    public static boolean updateInstance(ContentResolver contentResolver, AlarmInstance instance) {
        if (instance.mId == -1) {
            return false;
        }
        ContentValues values = createContentValues(instance);
        long rowsUpdated = contentResolver.update(getUri(instance.mId), values, null, null);
        return rowsUpdated == 1;
    }

    public static boolean deleteInstance(ContentResolver contentResolver, long instanceId) {
        if (instanceId == -1) {
            return false;
        }
        int deletedRows = contentResolver.delete(getUri(instanceId), "", null);
        return deletedRows == 1;
    }

    public AlarmInstance(Calendar calendar, Long alarmId) {
        this(calendar);
        this.mAlarmId = alarmId;
    }

    public AlarmInstance(Calendar calendar) {
        this.mId = -1L;
        setAlarmTime(calendar);
        this.mLabel = "";
        this.mVibrate = false;
        this.mRingtone = null;
        this.mAlarmState = 0;
    }

    public AlarmInstance(Cursor c) {
        this.mId = c.getLong(0);
        this.mYear = c.getInt(1);
        this.mMonth = c.getInt(2);
        this.mDay = c.getInt(3);
        this.mHour = c.getInt(4);
        this.mMinute = c.getInt(5);
        this.mLabel = c.getString(6);
        this.mVibrate = c.getInt(7) == 1;
        if (c.isNull(8)) {
            this.mRingtone = RingtoneManager.getDefaultUri(4);
        } else {
            this.mRingtone = Uri.parse(c.getString(8));
        }
        if (!c.isNull(9)) {
            this.mAlarmId = Long.valueOf(c.getLong(9));
        }
        this.mAlarmState = c.getInt(10);
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
        Calendar calendar = getAlarmTime();
        calendar.add(11, -2);
        return calendar;
    }

    public Calendar getHighNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(12, -30);
        return calendar;
    }

    public Calendar getMissedTimeToLive() {
        Calendar calendar = getAlarmTime();
        calendar.add(10, 12);
        return calendar;
    }

    public Calendar getTimeout(Context context) {
        String timeoutSetting = PreferenceManager.getDefaultSharedPreferences(context).getString("auto_silence", "10");
        int timeoutMinutes = Integer.parseInt(timeoutSetting);
        if (timeoutMinutes < 0) {
            return null;
        }
        Calendar calendar = getAlarmTime();
        calendar.add(12, timeoutMinutes);
        return calendar;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AlarmInstance)) {
            return false;
        }
        AlarmInstance other = (AlarmInstance) o;
        return this.mId == other.mId;
    }

    public int hashCode() {
        return Long.valueOf(this.mId).hashCode();
    }

    public String toString() {
        return "AlarmInstance{mId=" + this.mId + ", mYear=" + this.mYear + ", mMonth=" + this.mMonth + ", mDay=" + this.mDay + ", mHour=" + this.mHour + ", mMinute=" + this.mMinute + ", mLabel=" + this.mLabel + ", mVibrate=" + this.mVibrate + ", mRingtone=" + this.mRingtone + ", mAlarmId=" + this.mAlarmId + ", mAlarmState=" + this.mAlarmState + '}';
    }
}
