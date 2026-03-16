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
import com.android.deskclock.provider.ClockContract;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class Alarm implements Parcelable, ClockContract.AlarmsColumns {
    public Uri alert;
    public DaysOfWeek daysOfWeek;
    public boolean deleteAfterUse;
    public boolean enabled;
    public int hour;
    public long id;
    public String label;
    public int minutes;
    public boolean vibrate;
    private static final String[] QUERY_COLUMNS = {"_id", "hour", "minutes", "daysofweek", "enabled", "vibrate", "label", "ringtone", "delete_after_use"};
    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    public static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(9);
        if (alarm.id != -1) {
            values.put("_id", Long.valueOf(alarm.id));
        }
        values.put("enabled", Integer.valueOf(alarm.enabled ? 1 : 0));
        values.put("hour", Integer.valueOf(alarm.hour));
        values.put("minutes", Integer.valueOf(alarm.minutes));
        values.put("daysofweek", Integer.valueOf(alarm.daysOfWeek.getBitSet()));
        values.put("vibrate", Integer.valueOf(alarm.vibrate ? 1 : 0));
        values.put("label", alarm.label);
        values.put("delete_after_use", Boolean.valueOf(alarm.deleteAfterUse));
        if (alarm.alert == null) {
            values.putNull("ringtone");
        } else {
            values.put("ringtone", alarm.alert.toString());
        }
        return values;
    }

    public static Intent createIntent(Context context, Class<?> cls, long alarmId) {
        return new Intent(context, cls).setData(getUri(alarmId));
    }

    public static Uri getUri(long alarmId) {
        return ContentUris.withAppendedId(CONTENT_URI, alarmId);
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    public static CursorLoader getAlarmsCursorLoader(Context context) {
        return new CursorLoader(context, ClockContract.AlarmsColumns.CONTENT_URI, QUERY_COLUMNS, null, null, "hour, minutes ASC, _id DESC");
    }

    public static Alarm getAlarm(ContentResolver contentResolver, long alarmId) {
        Cursor cursor = contentResolver.query(getUri(alarmId), QUERY_COLUMNS, null, null, null);
        Alarm result = null;
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                result = new Alarm(cursor);
            }
            cursor.close();
            return result;
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    public static List<Alarm> getAlarms(ContentResolver contentResolver, String selection, String... selectionArgs) {
        Cursor cursor = contentResolver.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, null);
        List<Alarm> result = new LinkedList<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.add(new Alarm(cursor));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    public static Alarm addAlarm(ContentResolver contentResolver, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        alarm.id = getId(uri);
        return alarm;
    }

    public static boolean updateAlarm(ContentResolver contentResolver, Alarm alarm) {
        if (alarm.id == -1) {
            return false;
        }
        ContentValues values = createContentValues(alarm);
        long rowsUpdated = contentResolver.update(getUri(alarm.id), values, null, null);
        return rowsUpdated == 1;
    }

    public static boolean deleteAlarm(ContentResolver contentResolver, long alarmId) {
        if (alarmId == -1) {
            return false;
        }
        int deletedRows = contentResolver.delete(getUri(alarmId), "", null);
        return deletedRows == 1;
    }

    public Alarm() {
        this(0, 0);
    }

    public Alarm(int hour, int minutes) {
        this.id = -1L;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = true;
        this.daysOfWeek = new DaysOfWeek(0);
        this.label = "";
        this.alert = RingtoneManager.getDefaultUri(4);
        this.deleteAfterUse = false;
    }

    public Alarm(Cursor c) {
        this.id = c.getLong(0);
        this.enabled = c.getInt(4) == 1;
        this.hour = c.getInt(1);
        this.minutes = c.getInt(2);
        this.daysOfWeek = new DaysOfWeek(c.getInt(3));
        this.vibrate = c.getInt(5) == 1;
        this.label = c.getString(6);
        this.deleteAfterUse = c.getInt(8) == 1;
        if (c.isNull(7)) {
            this.alert = RingtoneManager.getDefaultUri(4);
        } else {
            this.alert = Uri.parse(c.getString(7));
        }
    }

    Alarm(Parcel p) {
        this.id = p.readLong();
        this.enabled = p.readInt() == 1;
        this.hour = p.readInt();
        this.minutes = p.readInt();
        this.daysOfWeek = new DaysOfWeek(p.readInt());
        this.vibrate = p.readInt() == 1;
        this.label = p.readString();
        this.alert = (Uri) p.readParcelable(null);
        this.deleteAfterUse = p.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(this.id);
        p.writeInt(this.enabled ? 1 : 0);
        p.writeInt(this.hour);
        p.writeInt(this.minutes);
        p.writeInt(this.daysOfWeek.getBitSet());
        p.writeInt(this.vibrate ? 1 : 0);
        p.writeString(this.label);
        p.writeParcelable(this.alert, flags);
        p.writeInt(this.deleteAfterUse ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public AlarmInstance createInstanceAfter(Calendar time) {
        Calendar nextInstanceTime = Calendar.getInstance();
        nextInstanceTime.set(1, time.get(1));
        nextInstanceTime.set(2, time.get(2));
        nextInstanceTime.set(5, time.get(5));
        nextInstanceTime.set(11, this.hour);
        nextInstanceTime.set(12, this.minutes);
        nextInstanceTime.set(13, 0);
        nextInstanceTime.set(14, 0);
        if (nextInstanceTime.getTimeInMillis() <= time.getTimeInMillis()) {
            nextInstanceTime.add(6, 1);
        }
        int addDays = this.daysOfWeek.calculateDaysToNextAlarm(nextInstanceTime);
        if (addDays > 0) {
            nextInstanceTime.add(7, addDays);
        }
        AlarmInstance result = new AlarmInstance(nextInstanceTime, Long.valueOf(this.id));
        result.mVibrate = this.vibrate;
        result.mLabel = this.label;
        result.mRingtone = this.alert;
        return result;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Alarm)) {
            return false;
        }
        Alarm other = (Alarm) o;
        return this.id == other.id;
    }

    public int hashCode() {
        return Long.valueOf(this.id).hashCode();
    }

    public String toString() {
        return "Alarm{alert=" + this.alert + ", id=" + this.id + ", enabled=" + this.enabled + ", hour=" + this.hour + ", minutes=" + this.minutes + ", daysOfWeek=" + this.daysOfWeek + ", vibrate=" + this.vibrate + ", label='" + this.label + "', deleteAfterUse=" + this.deleteAfterUse + '}';
    }
}
