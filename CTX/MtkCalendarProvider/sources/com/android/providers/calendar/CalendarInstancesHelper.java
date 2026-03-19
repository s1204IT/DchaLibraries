package com.android.providers.calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.RecurrenceProcessor;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.providers.calendar.MetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CalendarInstancesHelper {
    private static final String[] EXPAND_COLUMNS = {"_id", "_sync_id", "eventStatus", "dtstart", "dtend", "eventTimezone", "rrule", "rdate", "exrule", "exdate", "duration", "allDay", "original_sync_id", "originalInstanceTime", "calendar_id", "deleted"};
    private final CalendarCache mCalendarCache;
    private final CalendarDatabaseHelper mDbHelper;
    private final MetaData mMetaData;

    public static final class InstancesList extends ArrayList<ContentValues> {
    }

    public static final class EventInstancesMap extends HashMap<String, InstancesList> {
        public void add(String str, ContentValues contentValues) {
            InstancesList instancesList = get(str);
            if (instancesList == null) {
                instancesList = new InstancesList();
                put(str, instancesList);
            }
            instancesList.add(contentValues);
        }
    }

    public CalendarInstancesHelper(CalendarDatabaseHelper calendarDatabaseHelper, MetaData metaData) {
        this.mDbHelper = calendarDatabaseHelper;
        this.mMetaData = metaData;
        this.mCalendarCache = new CalendarCache(this.mDbHelper);
    }

    private static String getEventValue(SQLiteDatabase sQLiteDatabase, long j, String str) {
        return DatabaseUtils.stringForQuery(sQLiteDatabase, "SELECT " + str + " FROM Events WHERE _id=?", new String[]{String.valueOf(j)});
    }

    protected void performInstanceExpansion(long j, long j2, String str, Cursor cursor) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        EventInstancesMap eventInstancesMap;
        int i11;
        int i12;
        int i13;
        Duration duration;
        Time time;
        int i14;
        int i15;
        int i16;
        int i17;
        String str2;
        boolean z;
        long j3;
        Long lValueOf;
        String string;
        int i18;
        String string2;
        long j4;
        int i19;
        int i20;
        boolean z2;
        int i21;
        String string3;
        int i22;
        int i23;
        int i24;
        int i25;
        long j5;
        int i26;
        long j6;
        String syncIdKey;
        RecurrenceSet recurrenceSet;
        Duration duration2;
        ContentValues contentValues;
        String str3;
        long j7;
        long j8;
        char c;
        int i27;
        int i28;
        int i29;
        String str4;
        long j9;
        ContentValues contentValues2;
        long[] jArr;
        int i30;
        int i31;
        Long l;
        long j10;
        RecurrenceProcessor recurrenceProcessor = new RecurrenceProcessor();
        int columnIndex = cursor.getColumnIndex("eventStatus");
        int columnIndex2 = cursor.getColumnIndex("dtstart");
        int columnIndex3 = cursor.getColumnIndex("dtend");
        int columnIndex4 = cursor.getColumnIndex("eventTimezone");
        int columnIndex5 = cursor.getColumnIndex("duration");
        int columnIndex6 = cursor.getColumnIndex("rrule");
        int columnIndex7 = cursor.getColumnIndex("rdate");
        int columnIndex8 = cursor.getColumnIndex("exrule");
        int columnIndex9 = cursor.getColumnIndex("exdate");
        int columnIndex10 = cursor.getColumnIndex("allDay");
        int columnIndex11 = cursor.getColumnIndex("_id");
        int columnIndex12 = cursor.getColumnIndex("_sync_id");
        int columnIndex13 = cursor.getColumnIndex("original_sync_id");
        int columnIndex14 = cursor.getColumnIndex("originalInstanceTime");
        int i32 = columnIndex3;
        int columnIndex15 = cursor.getColumnIndex("calendar_id");
        int columnIndex16 = cursor.getColumnIndex("deleted");
        int i33 = columnIndex9;
        EventInstancesMap eventInstancesMap2 = new EventInstancesMap();
        Duration duration3 = new Duration();
        int i34 = columnIndex8;
        Time time2 = new Time();
        while (true) {
            Time time3 = time2;
            if (!cursor.moveToNext()) {
                break;
            }
            try {
                boolean z3 = cursor.getInt(columnIndex10) != 0;
                String string4 = cursor.getString(columnIndex4);
                if (z3 || TextUtils.isEmpty(string4)) {
                    string4 = "UTC";
                }
                i17 = columnIndex10;
                str2 = string4;
                z = z3;
                try {
                    try {
                        j3 = cursor.getLong(columnIndex2);
                        lValueOf = Long.valueOf(cursor.getLong(columnIndex11));
                        string = cursor.getString(columnIndex5);
                    } catch (TimeFormatException e) {
                        e = e;
                        i = columnIndex13;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i5 = columnIndex2;
                        i6 = columnIndex16;
                        i7 = columnIndex4;
                        i8 = columnIndex5;
                        i9 = columnIndex15;
                        time = time3;
                        i10 = columnIndex11;
                        i14 = columnIndex6;
                        i15 = i32;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        i16 = columnIndex12;
                        i12 = i33;
                        i13 = i17;
                    }
                } catch (DateException e2) {
                    e = e2;
                    i = columnIndex13;
                    duration = duration3;
                    i2 = columnIndex7;
                    i3 = columnIndex14;
                    i4 = columnIndex;
                    i5 = columnIndex2;
                    i6 = columnIndex16;
                    i7 = columnIndex4;
                    i8 = columnIndex5;
                    i9 = columnIndex15;
                    time = time3;
                    i10 = columnIndex11;
                    i14 = columnIndex6;
                    i15 = i32;
                    eventInstancesMap = eventInstancesMap2;
                    i11 = i34;
                    i16 = columnIndex12;
                    i12 = i33;
                    i13 = i17;
                }
            } catch (TimeFormatException e3) {
                e = e3;
                i = columnIndex13;
                i2 = columnIndex7;
                i3 = columnIndex14;
                i4 = columnIndex;
                i5 = columnIndex2;
                i6 = columnIndex16;
                i7 = columnIndex4;
                i8 = columnIndex5;
                i9 = columnIndex15;
                i10 = columnIndex11;
                eventInstancesMap = eventInstancesMap2;
                i11 = i34;
                i12 = i33;
                i13 = columnIndex10;
                duration = duration3;
                time = time3;
                i14 = columnIndex6;
                i15 = i32;
            } catch (DateException e4) {
                e = e4;
                i = columnIndex13;
                i2 = columnIndex7;
                i3 = columnIndex14;
                i4 = columnIndex;
                i5 = columnIndex2;
                i6 = columnIndex16;
                i7 = columnIndex4;
                i8 = columnIndex5;
                i9 = columnIndex15;
                i10 = columnIndex11;
                eventInstancesMap = eventInstancesMap2;
                i11 = i34;
                i12 = i33;
                i13 = columnIndex10;
                duration = duration3;
                time = time3;
                i14 = columnIndex6;
                i15 = i32;
            }
            if (string != null) {
                try {
                    duration3.parse(string);
                    i18 = columnIndex11;
                    i5 = columnIndex2;
                    i7 = columnIndex4;
                } catch (DateException e5) {
                    i18 = columnIndex11;
                    i5 = columnIndex2;
                    try {
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                            StringBuilder sb = new StringBuilder();
                            i7 = columnIndex4;
                            sb.append("error parsing duration for event ");
                            sb.append(lValueOf);
                            sb.append("'");
                            sb.append(string);
                            sb.append("'");
                            Log.w("CalendarProvider2", sb.toString(), e5);
                        } else {
                            i7 = columnIndex4;
                        }
                        duration3.sign = 1;
                        duration3.weeks = 0;
                        duration3.days = 0;
                        duration3.hours = 0;
                        duration3.minutes = 0;
                        duration3.seconds = 0;
                        string = "+P0S";
                    } catch (TimeFormatException e6) {
                        e = e6;
                        i7 = columnIndex4;
                        i = columnIndex13;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i6 = columnIndex16;
                        i8 = columnIndex5;
                        i9 = columnIndex15;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        time = time3;
                        i10 = i18;
                        i14 = columnIndex6;
                        i15 = i32;
                        i12 = i33;
                        i13 = i17;
                        i16 = columnIndex12;
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                        }
                        time2 = time;
                        columnIndex12 = i16;
                        columnIndex7 = i2;
                        columnIndex2 = i5;
                        columnIndex4 = i7;
                        columnIndex5 = i8;
                        columnIndex14 = i3;
                        columnIndex = i4;
                        columnIndex16 = i6;
                        columnIndex13 = i;
                        duration3 = duration;
                        i32 = i15;
                        columnIndex10 = i13;
                        i33 = i12;
                        columnIndex6 = i14;
                        i34 = i11;
                        eventInstancesMap2 = eventInstancesMap;
                        columnIndex11 = i10;
                        columnIndex15 = i9;
                    } catch (DateException e7) {
                        e = e7;
                        i7 = columnIndex4;
                        i = columnIndex13;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i6 = columnIndex16;
                        i8 = columnIndex5;
                        i9 = columnIndex15;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        time = time3;
                        i10 = i18;
                        i14 = columnIndex6;
                        i15 = i32;
                        i12 = i33;
                        i13 = i17;
                        i16 = columnIndex12;
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                        }
                        time2 = time;
                        columnIndex12 = i16;
                        columnIndex7 = i2;
                        columnIndex2 = i5;
                        columnIndex4 = i7;
                        columnIndex5 = i8;
                        columnIndex14 = i3;
                        columnIndex = i4;
                        columnIndex16 = i6;
                        columnIndex13 = i;
                        duration3 = duration;
                        i32 = i15;
                        columnIndex10 = i13;
                        i33 = i12;
                        columnIndex6 = i14;
                        i34 = i11;
                        eventInstancesMap2 = eventInstancesMap;
                        columnIndex11 = i10;
                        columnIndex15 = i9;
                    }
                }
                string2 = cursor.getString(columnIndex12);
                String string5 = cursor.getString(columnIndex13);
                if (cursor.isNull(columnIndex14)) {
                    try {
                        i8 = columnIndex5;
                        j4 = cursor.getLong(columnIndex14);
                    } catch (TimeFormatException e8) {
                        e = e8;
                        i = columnIndex13;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i6 = columnIndex16;
                        i8 = columnIndex5;
                        i9 = columnIndex15;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        time = time3;
                        i10 = i18;
                        i14 = columnIndex6;
                        i15 = i32;
                        i12 = i33;
                        i13 = i17;
                        i16 = columnIndex12;
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                        }
                        time2 = time;
                        columnIndex12 = i16;
                        columnIndex7 = i2;
                        columnIndex2 = i5;
                        columnIndex4 = i7;
                        columnIndex5 = i8;
                        columnIndex14 = i3;
                        columnIndex = i4;
                        columnIndex16 = i6;
                        columnIndex13 = i;
                        duration3 = duration;
                        i32 = i15;
                        columnIndex10 = i13;
                        i33 = i12;
                        columnIndex6 = i14;
                        i34 = i11;
                        eventInstancesMap2 = eventInstancesMap;
                        columnIndex11 = i10;
                        columnIndex15 = i9;
                    } catch (DateException e9) {
                        e = e9;
                        i = columnIndex13;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i6 = columnIndex16;
                        i8 = columnIndex5;
                        i9 = columnIndex15;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        time = time3;
                        i10 = i18;
                        i14 = columnIndex6;
                        i15 = i32;
                        i12 = i33;
                        i13 = i17;
                        i16 = columnIndex12;
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                        }
                        time2 = time;
                        columnIndex12 = i16;
                        columnIndex7 = i2;
                        columnIndex2 = i5;
                        columnIndex4 = i7;
                        columnIndex5 = i8;
                        columnIndex14 = i3;
                        columnIndex = i4;
                        columnIndex16 = i6;
                        columnIndex13 = i;
                        duration3 = duration;
                        i32 = i15;
                        columnIndex10 = i13;
                        i33 = i12;
                        columnIndex6 = i14;
                        i34 = i11;
                        eventInstancesMap2 = eventInstancesMap;
                        columnIndex11 = i10;
                        columnIndex15 = i9;
                    }
                } else {
                    i8 = columnIndex5;
                    j4 = -1;
                }
                try {
                    i19 = cursor.getInt(columnIndex);
                    if (cursor.getInt(columnIndex16) == 0) {
                        i20 = columnIndex13;
                        z2 = true;
                    } else {
                        i20 = columnIndex13;
                        z2 = false;
                    }
                    i21 = columnIndex12;
                    try {
                        string3 = cursor.getString(columnIndex6);
                        String string6 = cursor.getString(columnIndex7);
                        i22 = columnIndex6;
                        i23 = columnIndex7;
                        i24 = i34;
                        try {
                            String string7 = cursor.getString(i24);
                            i3 = columnIndex14;
                            i25 = i33;
                            try {
                                String string8 = cursor.getString(i25);
                                i4 = columnIndex;
                                i6 = columnIndex16;
                                j5 = j4;
                                i26 = columnIndex15;
                                try {
                                    j6 = cursor.getLong(i26);
                                    syncIdKey = getSyncIdKey(string2, j6);
                                    try {
                                        try {
                                            try {
                                                recurrenceSet = new RecurrenceSet(string3, string6, string7, string8);
                                            } catch (TimeFormatException e10) {
                                                e = e10;
                                                duration2 = duration3;
                                                i9 = i26;
                                                i15 = i32;
                                                eventInstancesMap = eventInstancesMap2;
                                                time = time3;
                                                i13 = i17;
                                                i10 = i18;
                                                i = i20;
                                                i16 = i21;
                                                i14 = i22;
                                                i2 = i23;
                                                i11 = i24;
                                                i12 = i25;
                                            } catch (DateException e11) {
                                                e = e11;
                                                duration2 = duration3;
                                                i9 = i26;
                                                i15 = i32;
                                                eventInstancesMap = eventInstancesMap2;
                                                time = time3;
                                                i13 = i17;
                                                i10 = i18;
                                                i = i20;
                                                i16 = i21;
                                                i14 = i22;
                                                i2 = i23;
                                                i11 = i24;
                                                i12 = i25;
                                            }
                                        } catch (EventRecurrence.InvalidFormatException e12) {
                                            duration = duration3;
                                            i9 = i26;
                                            i15 = i32;
                                            eventInstancesMap = eventInstancesMap2;
                                            time = time3;
                                            i13 = i17;
                                            i10 = i18;
                                            i = i20;
                                            i16 = i21;
                                            i14 = i22;
                                            i2 = i23;
                                            i11 = i24;
                                            i12 = i25;
                                            try {
                                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                                    Log.w("CalendarProvider2", "Could not parse RRULE recurrence string: " + string3, e12);
                                                }
                                            } catch (TimeFormatException e13) {
                                                e = e13;
                                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                                }
                                                time2 = time;
                                                columnIndex12 = i16;
                                                columnIndex7 = i2;
                                                columnIndex2 = i5;
                                                columnIndex4 = i7;
                                                columnIndex5 = i8;
                                                columnIndex14 = i3;
                                                columnIndex = i4;
                                                columnIndex16 = i6;
                                                columnIndex13 = i;
                                                duration3 = duration;
                                                i32 = i15;
                                                columnIndex10 = i13;
                                                i33 = i12;
                                                columnIndex6 = i14;
                                                i34 = i11;
                                                eventInstancesMap2 = eventInstancesMap;
                                                columnIndex11 = i10;
                                                columnIndex15 = i9;
                                            } catch (DateException e14) {
                                                e = e14;
                                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                                }
                                                time2 = time;
                                                columnIndex12 = i16;
                                                columnIndex7 = i2;
                                                columnIndex2 = i5;
                                                columnIndex4 = i7;
                                                columnIndex5 = i8;
                                                columnIndex14 = i3;
                                                columnIndex = i4;
                                                columnIndex16 = i6;
                                                columnIndex13 = i;
                                                duration3 = duration;
                                                i32 = i15;
                                                columnIndex10 = i13;
                                                i33 = i12;
                                                columnIndex6 = i14;
                                                i34 = i11;
                                                eventInstancesMap2 = eventInstancesMap;
                                                columnIndex11 = i10;
                                                columnIndex15 = i9;
                                            }
                                        }
                                    } catch (TimeFormatException e15) {
                                        e = e15;
                                        duration = duration3;
                                        i9 = i26;
                                        i15 = i32;
                                        eventInstancesMap = eventInstancesMap2;
                                        time = time3;
                                        i13 = i17;
                                        i10 = i18;
                                        i = i20;
                                        i16 = i21;
                                        i14 = i22;
                                        i2 = i23;
                                        i11 = i24;
                                        i12 = i25;
                                    } catch (DateException e16) {
                                        e = e16;
                                        duration = duration3;
                                        i9 = i26;
                                        i15 = i32;
                                        eventInstancesMap = eventInstancesMap2;
                                        time = time3;
                                        i13 = i17;
                                        i10 = i18;
                                        i = i20;
                                        i16 = i21;
                                        i14 = i22;
                                        i2 = i23;
                                        i11 = i24;
                                        i12 = i25;
                                    }
                                } catch (TimeFormatException e17) {
                                    e = e17;
                                    duration = duration3;
                                    i12 = i25;
                                    i9 = i26;
                                    i15 = i32;
                                    eventInstancesMap = eventInstancesMap2;
                                    time = time3;
                                    i13 = i17;
                                    i10 = i18;
                                    i = i20;
                                    i16 = i21;
                                    i14 = i22;
                                    i2 = i23;
                                    i11 = i24;
                                    if (Log.isLoggable("CalendarProvider2", 6)) {
                                    }
                                    time2 = time;
                                    columnIndex12 = i16;
                                    columnIndex7 = i2;
                                    columnIndex2 = i5;
                                    columnIndex4 = i7;
                                    columnIndex5 = i8;
                                    columnIndex14 = i3;
                                    columnIndex = i4;
                                    columnIndex16 = i6;
                                    columnIndex13 = i;
                                    duration3 = duration;
                                    i32 = i15;
                                    columnIndex10 = i13;
                                    i33 = i12;
                                    columnIndex6 = i14;
                                    i34 = i11;
                                    eventInstancesMap2 = eventInstancesMap;
                                    columnIndex11 = i10;
                                    columnIndex15 = i9;
                                } catch (DateException e18) {
                                    e = e18;
                                    duration = duration3;
                                    i12 = i25;
                                    i9 = i26;
                                    i15 = i32;
                                    eventInstancesMap = eventInstancesMap2;
                                    time = time3;
                                    i13 = i17;
                                    i10 = i18;
                                    i = i20;
                                    i16 = i21;
                                    i14 = i22;
                                    i2 = i23;
                                    i11 = i24;
                                    if (Log.isLoggable("CalendarProvider2", 6)) {
                                    }
                                    time2 = time;
                                    columnIndex12 = i16;
                                    columnIndex7 = i2;
                                    columnIndex2 = i5;
                                    columnIndex4 = i7;
                                    columnIndex5 = i8;
                                    columnIndex14 = i3;
                                    columnIndex = i4;
                                    columnIndex16 = i6;
                                    columnIndex13 = i;
                                    duration3 = duration;
                                    i32 = i15;
                                    columnIndex10 = i13;
                                    i33 = i12;
                                    columnIndex6 = i14;
                                    i34 = i11;
                                    eventInstancesMap2 = eventInstancesMap;
                                    columnIndex11 = i10;
                                    columnIndex15 = i9;
                                }
                            } catch (TimeFormatException e19) {
                                e = e19;
                                duration = duration3;
                                i12 = i25;
                                i4 = columnIndex;
                                i6 = columnIndex16;
                                i15 = i32;
                                i9 = columnIndex15;
                            } catch (DateException e20) {
                                e = e20;
                                duration = duration3;
                                i12 = i25;
                                i4 = columnIndex;
                                i6 = columnIndex16;
                                i15 = i32;
                                i9 = columnIndex15;
                            }
                        } catch (TimeFormatException e21) {
                            e = e21;
                            duration = duration3;
                            i3 = columnIndex14;
                            i4 = columnIndex;
                            i6 = columnIndex16;
                            i9 = columnIndex15;
                            i12 = i33;
                            eventInstancesMap = eventInstancesMap2;
                            time = time3;
                            i13 = i17;
                            i10 = i18;
                            i = i20;
                            i14 = i22;
                            i2 = i23;
                            i11 = i24;
                            i15 = i32;
                            i16 = i21;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                            }
                            time2 = time;
                            columnIndex12 = i16;
                            columnIndex7 = i2;
                            columnIndex2 = i5;
                            columnIndex4 = i7;
                            columnIndex5 = i8;
                            columnIndex14 = i3;
                            columnIndex = i4;
                            columnIndex16 = i6;
                            columnIndex13 = i;
                            duration3 = duration;
                            i32 = i15;
                            columnIndex10 = i13;
                            i33 = i12;
                            columnIndex6 = i14;
                            i34 = i11;
                            eventInstancesMap2 = eventInstancesMap;
                            columnIndex11 = i10;
                            columnIndex15 = i9;
                        } catch (DateException e22) {
                            e = e22;
                            duration = duration3;
                            i3 = columnIndex14;
                            i4 = columnIndex;
                            i6 = columnIndex16;
                            i9 = columnIndex15;
                            i12 = i33;
                            eventInstancesMap = eventInstancesMap2;
                            time = time3;
                            i13 = i17;
                            i10 = i18;
                            i = i20;
                            i14 = i22;
                            i2 = i23;
                            i11 = i24;
                            i15 = i32;
                            i16 = i21;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                            }
                            time2 = time;
                            columnIndex12 = i16;
                            columnIndex7 = i2;
                            columnIndex2 = i5;
                            columnIndex4 = i7;
                            columnIndex5 = i8;
                            columnIndex14 = i3;
                            columnIndex = i4;
                            columnIndex16 = i6;
                            columnIndex13 = i;
                            duration3 = duration;
                            i32 = i15;
                            columnIndex10 = i13;
                            i33 = i12;
                            columnIndex6 = i14;
                            i34 = i11;
                            eventInstancesMap2 = eventInstancesMap;
                            columnIndex11 = i10;
                            columnIndex15 = i9;
                        }
                    } catch (TimeFormatException e23) {
                        e = e23;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i6 = columnIndex16;
                        i9 = columnIndex15;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        time = time3;
                        i10 = i18;
                        i = i20;
                        i14 = columnIndex6;
                        i15 = i32;
                        i12 = i33;
                        i13 = i17;
                    } catch (DateException e24) {
                        e = e24;
                        duration = duration3;
                        i2 = columnIndex7;
                        i3 = columnIndex14;
                        i4 = columnIndex;
                        i6 = columnIndex16;
                        i9 = columnIndex15;
                        eventInstancesMap = eventInstancesMap2;
                        i11 = i34;
                        time = time3;
                        i10 = i18;
                        i = i20;
                        i14 = columnIndex6;
                        i15 = i32;
                        i12 = i33;
                        i13 = i17;
                    }
                } catch (TimeFormatException e25) {
                    e = e25;
                    i = columnIndex13;
                    duration = duration3;
                    i2 = columnIndex7;
                    i3 = columnIndex14;
                    i4 = columnIndex;
                    i6 = columnIndex16;
                    i9 = columnIndex15;
                    eventInstancesMap = eventInstancesMap2;
                    i11 = i34;
                    time = time3;
                    i10 = i18;
                    i14 = columnIndex6;
                    i15 = i32;
                    i12 = i33;
                    i13 = i17;
                    i16 = columnIndex12;
                    if (Log.isLoggable("CalendarProvider2", 6)) {
                    }
                    time2 = time;
                    columnIndex12 = i16;
                    columnIndex7 = i2;
                    columnIndex2 = i5;
                    columnIndex4 = i7;
                    columnIndex5 = i8;
                    columnIndex14 = i3;
                    columnIndex = i4;
                    columnIndex16 = i6;
                    columnIndex13 = i;
                    duration3 = duration;
                    i32 = i15;
                    columnIndex10 = i13;
                    i33 = i12;
                    columnIndex6 = i14;
                    i34 = i11;
                    eventInstancesMap2 = eventInstancesMap;
                    columnIndex11 = i10;
                    columnIndex15 = i9;
                } catch (DateException e26) {
                    e = e26;
                    i = columnIndex13;
                    duration = duration3;
                    i2 = columnIndex7;
                    i3 = columnIndex14;
                    i4 = columnIndex;
                    i6 = columnIndex16;
                    i9 = columnIndex15;
                    eventInstancesMap = eventInstancesMap2;
                    i11 = i34;
                    time = time3;
                    i10 = i18;
                    i14 = columnIndex6;
                    i15 = i32;
                    i12 = i33;
                    i13 = i17;
                    i16 = columnIndex12;
                    if (Log.isLoggable("CalendarProvider2", 6)) {
                    }
                    time2 = time;
                    columnIndex12 = i16;
                    columnIndex7 = i2;
                    columnIndex2 = i5;
                    columnIndex4 = i7;
                    columnIndex5 = i8;
                    columnIndex14 = i3;
                    columnIndex = i4;
                    columnIndex16 = i6;
                    columnIndex13 = i;
                    duration3 = duration;
                    i32 = i15;
                    columnIndex10 = i13;
                    i33 = i12;
                    columnIndex6 = i14;
                    i34 = i11;
                    eventInstancesMap2 = eventInstancesMap;
                    columnIndex11 = i10;
                    columnIndex15 = i9;
                }
                if (recurrenceSet.hasRecurrence()) {
                    duration2 = duration3;
                    i9 = i26;
                    i15 = i32;
                    EventInstancesMap eventInstancesMap3 = eventInstancesMap2;
                    time = time3;
                    i13 = i17;
                    i10 = i18;
                    i = i20;
                    i16 = i21;
                    i14 = i22;
                    i2 = i23;
                    i11 = i24;
                    i12 = i25;
                    try {
                        contentValues = new ContentValues();
                        if (string5 == null || j5 == -1) {
                            str3 = syncIdKey;
                            j7 = j5;
                        } else {
                            str3 = syncIdKey;
                            try {
                                contentValues.put("ORIGINAL_EVENT_AND_CALENDAR", getSyncIdKey(string5, j6));
                                j7 = j5;
                                try {
                                    contentValues.put("originalInstanceTime", Long.valueOf(j7));
                                    contentValues.put("eventStatus", Integer.valueOf(i19));
                                } catch (TimeFormatException e27) {
                                    e = e27;
                                    duration = duration2;
                                    eventInstancesMap = eventInstancesMap3;
                                    if (Log.isLoggable("CalendarProvider2", 6)) {
                                    }
                                    time2 = time;
                                    columnIndex12 = i16;
                                    columnIndex7 = i2;
                                    columnIndex2 = i5;
                                    columnIndex4 = i7;
                                    columnIndex5 = i8;
                                    columnIndex14 = i3;
                                    columnIndex = i4;
                                    columnIndex16 = i6;
                                    columnIndex13 = i;
                                    duration3 = duration;
                                    i32 = i15;
                                    columnIndex10 = i13;
                                    i33 = i12;
                                    columnIndex6 = i14;
                                    i34 = i11;
                                    eventInstancesMap2 = eventInstancesMap;
                                    columnIndex11 = i10;
                                } catch (DateException e28) {
                                    e = e28;
                                    duration = duration2;
                                    eventInstancesMap = eventInstancesMap3;
                                    if (Log.isLoggable("CalendarProvider2", 6)) {
                                    }
                                    time2 = time;
                                    columnIndex12 = i16;
                                    columnIndex7 = i2;
                                    columnIndex2 = i5;
                                    columnIndex4 = i7;
                                    columnIndex5 = i8;
                                    columnIndex14 = i3;
                                    columnIndex = i4;
                                    columnIndex16 = i6;
                                    columnIndex13 = i;
                                    duration3 = duration;
                                    i32 = i15;
                                    columnIndex10 = i13;
                                    i33 = i12;
                                    columnIndex6 = i14;
                                    i34 = i11;
                                    eventInstancesMap2 = eventInstancesMap;
                                    columnIndex11 = i10;
                                }
                            } catch (TimeFormatException e29) {
                                e = e29;
                                duration = duration2;
                                eventInstancesMap = eventInstancesMap3;
                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                }
                                time2 = time;
                                columnIndex12 = i16;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration;
                                i32 = i15;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex6 = i14;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                columnIndex11 = i10;
                            } catch (DateException e30) {
                                e = e30;
                                duration = duration2;
                                eventInstancesMap = eventInstancesMap3;
                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                }
                                time2 = time;
                                columnIndex12 = i16;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration;
                                i32 = i15;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex6 = i14;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                columnIndex11 = i10;
                            }
                        }
                        j8 = string == null ? !cursor.isNull(i15) ? cursor.getLong(i15) : j3 : duration2.addTo(j3);
                    } catch (TimeFormatException e31) {
                        e = e31;
                    } catch (DateException e32) {
                        e = e32;
                    }
                    if (j8 < j || j3 > j2) {
                        if (string5 == null || j7 == -1) {
                            eventInstancesMap = eventInstancesMap3;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                                Log.w("CalendarProvider2", "Unexpected event outside window: " + string2);
                            }
                            time2 = time;
                            columnIndex10 = i13;
                            i33 = i12;
                            columnIndex7 = i2;
                            columnIndex2 = i5;
                            columnIndex4 = i7;
                            columnIndex5 = i8;
                            columnIndex14 = i3;
                            columnIndex = i4;
                            columnIndex16 = i6;
                            columnIndex13 = i;
                            duration3 = duration2;
                            columnIndex12 = i16;
                            i34 = i11;
                            eventInstancesMap2 = eventInstancesMap;
                            i32 = i15;
                            columnIndex11 = i10;
                            columnIndex6 = i14;
                            columnIndex15 = i9;
                        } else {
                            try {
                                contentValues.put("eventStatus", (Integer) 2);
                            } catch (TimeFormatException e33) {
                                e = e33;
                                eventInstancesMap = eventInstancesMap3;
                                duration = duration2;
                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                }
                                time2 = time;
                                columnIndex12 = i16;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration;
                                i32 = i15;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex6 = i14;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                columnIndex11 = i10;
                                columnIndex15 = i9;
                            } catch (DateException e34) {
                                e = e34;
                                eventInstancesMap = eventInstancesMap3;
                                duration = duration2;
                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                }
                                time2 = time;
                                columnIndex12 = i16;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration;
                                i32 = i15;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex6 = i14;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                columnIndex11 = i10;
                                columnIndex15 = i9;
                            }
                        }
                    }
                    contentValues.put("event_id", lValueOf);
                    contentValues.put("begin", Long.valueOf(j3));
                    contentValues.put("end", Long.valueOf(j8));
                    contentValues.put("deleted", Boolean.valueOf(z2));
                    if (z) {
                        time.timezone = "UTC";
                    } else {
                        try {
                            time.timezone = str;
                        } catch (TimeFormatException e35) {
                            e = e35;
                            eventInstancesMap = eventInstancesMap3;
                            duration = duration2;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                            }
                            time2 = time;
                            columnIndex12 = i16;
                            columnIndex7 = i2;
                            columnIndex2 = i5;
                            columnIndex4 = i7;
                            columnIndex5 = i8;
                            columnIndex14 = i3;
                            columnIndex = i4;
                            columnIndex16 = i6;
                            columnIndex13 = i;
                            duration3 = duration;
                            i32 = i15;
                            columnIndex10 = i13;
                            i33 = i12;
                            columnIndex6 = i14;
                            i34 = i11;
                            eventInstancesMap2 = eventInstancesMap;
                            columnIndex11 = i10;
                            columnIndex15 = i9;
                        } catch (DateException e36) {
                            e = e36;
                            eventInstancesMap = eventInstancesMap3;
                            duration = duration2;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                            }
                            time2 = time;
                            columnIndex12 = i16;
                            columnIndex7 = i2;
                            columnIndex2 = i5;
                            columnIndex4 = i7;
                            columnIndex5 = i8;
                            columnIndex14 = i3;
                            columnIndex = i4;
                            columnIndex16 = i6;
                            columnIndex13 = i;
                            duration3 = duration;
                            i32 = i15;
                            columnIndex10 = i13;
                            i33 = i12;
                            columnIndex6 = i14;
                            i34 = i11;
                            eventInstancesMap2 = eventInstancesMap;
                            columnIndex11 = i10;
                            columnIndex15 = i9;
                        }
                    }
                    computeTimezoneDependentFields(j3, j8, time, contentValues);
                    eventInstancesMap = eventInstancesMap3;
                    eventInstancesMap.add(str3, contentValues);
                    time2 = time;
                    columnIndex10 = i13;
                    i33 = i12;
                    columnIndex7 = i2;
                    columnIndex2 = i5;
                    columnIndex4 = i7;
                    columnIndex5 = i8;
                    columnIndex14 = i3;
                    columnIndex = i4;
                    columnIndex16 = i6;
                    columnIndex13 = i;
                    duration3 = duration2;
                    columnIndex12 = i16;
                    i34 = i11;
                    eventInstancesMap2 = eventInstancesMap;
                    i32 = i15;
                    columnIndex11 = i10;
                    columnIndex6 = i14;
                    columnIndex15 = i9;
                } else {
                    if (i19 == 2) {
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                            Log.e("CalendarProvider2", "Found canceled recurring event in Events table.  Ignoring.");
                        }
                    } else if (!z2) {
                        try {
                            time3.timezone = str2;
                            time3.set(j3);
                            time3.allDay = z;
                            if (string == null) {
                                try {
                                    c = 6;
                                    if (Log.isLoggable("CalendarProvider2", 6)) {
                                        Log.e("CalendarProvider2", "Repeating event has no duration -- should not happen.");
                                    }
                                    if (z) {
                                        duration3.sign = 1;
                                        duration3.weeks = 0;
                                        duration3.days = 1;
                                        duration3.hours = 0;
                                        duration3.minutes = 0;
                                        duration3.seconds = 0;
                                        i27 = i32;
                                    } else {
                                        duration3.sign = 1;
                                        duration3.weeks = 0;
                                        duration3.days = 0;
                                        duration3.hours = 0;
                                        duration3.minutes = 0;
                                        i27 = i32;
                                        try {
                                            if (cursor.isNull(i27)) {
                                                i28 = 0;
                                                duration3.seconds = 0;
                                            } else {
                                                duration3.seconds = (int) ((cursor.getLong(i27) - j3) / 1000);
                                                String str5 = "+P" + duration3.seconds + "S";
                                            }
                                        } catch (TimeFormatException e37) {
                                            e = e37;
                                            duration = duration3;
                                            i9 = i26;
                                            i15 = i27;
                                            eventInstancesMap = eventInstancesMap2;
                                            i13 = i17;
                                            i10 = i18;
                                            i = i20;
                                            i16 = i21;
                                            i14 = i22;
                                            i2 = i23;
                                            i11 = i24;
                                            i12 = i25;
                                            time = time3;
                                            if (Log.isLoggable("CalendarProvider2", 6)) {
                                            }
                                            time2 = time;
                                            columnIndex12 = i16;
                                            columnIndex7 = i2;
                                            columnIndex2 = i5;
                                            columnIndex4 = i7;
                                            columnIndex5 = i8;
                                            columnIndex14 = i3;
                                            columnIndex = i4;
                                            columnIndex16 = i6;
                                            columnIndex13 = i;
                                            duration3 = duration;
                                            i32 = i15;
                                            columnIndex10 = i13;
                                            i33 = i12;
                                            columnIndex6 = i14;
                                            i34 = i11;
                                            eventInstancesMap2 = eventInstancesMap;
                                            columnIndex11 = i10;
                                            columnIndex15 = i9;
                                        } catch (DateException e38) {
                                            e = e38;
                                            duration = duration3;
                                            i9 = i26;
                                            i15 = i27;
                                            eventInstancesMap = eventInstancesMap2;
                                            i13 = i17;
                                            i10 = i18;
                                            i = i20;
                                            i16 = i21;
                                            i14 = i22;
                                            i2 = i23;
                                            i11 = i24;
                                            i12 = i25;
                                            time = time3;
                                            if (Log.isLoggable("CalendarProvider2", 6)) {
                                            }
                                            time2 = time;
                                            columnIndex12 = i16;
                                            columnIndex7 = i2;
                                            columnIndex2 = i5;
                                            columnIndex4 = i7;
                                            columnIndex5 = i8;
                                            columnIndex14 = i3;
                                            columnIndex = i4;
                                            columnIndex16 = i6;
                                            columnIndex13 = i;
                                            duration3 = duration;
                                            i32 = i15;
                                            columnIndex10 = i13;
                                            i33 = i12;
                                            columnIndex6 = i14;
                                            i34 = i11;
                                            eventInstancesMap2 = eventInstancesMap;
                                            columnIndex11 = i10;
                                            columnIndex15 = i9;
                                        }
                                    }
                                    i28 = 0;
                                } catch (TimeFormatException e39) {
                                    e = e39;
                                    duration = duration3;
                                    i9 = i26;
                                    i15 = i32;
                                } catch (DateException e40) {
                                    e = e40;
                                    duration = duration3;
                                    i9 = i26;
                                    i15 = i32;
                                }
                            } else {
                                i27 = i32;
                                i28 = 0;
                                c = 6;
                            }
                            i16 = i21;
                            i10 = i18;
                            i13 = i17;
                            Duration duration4 = duration3;
                            int i35 = i28;
                            Long l2 = lValueOf;
                            i12 = i25;
                            Time time4 = time3;
                            EventInstancesMap eventInstancesMap4 = eventInstancesMap2;
                            i11 = i24;
                            i9 = i26;
                            i14 = i22;
                            i2 = i23;
                            try {
                                long[] jArrExpand = recurrenceProcessor.expand(time3, recurrenceSet, j, j2);
                                if (z) {
                                    time4.timezone = "UTC";
                                    i29 = i20;
                                    str4 = str;
                                } else {
                                    i29 = i20;
                                    str4 = str;
                                    try {
                                        time4.timezone = str4;
                                    } catch (TimeFormatException e41) {
                                        e = e41;
                                        eventInstancesMap = eventInstancesMap4;
                                        i = i29;
                                        duration = duration4;
                                        time = time4;
                                        i15 = i27;
                                        if (Log.isLoggable("CalendarProvider2", 6)) {
                                        }
                                        time2 = time;
                                        columnIndex12 = i16;
                                        columnIndex7 = i2;
                                        columnIndex2 = i5;
                                        columnIndex4 = i7;
                                        columnIndex5 = i8;
                                        columnIndex14 = i3;
                                        columnIndex = i4;
                                        columnIndex16 = i6;
                                        columnIndex13 = i;
                                        duration3 = duration;
                                        i32 = i15;
                                        columnIndex10 = i13;
                                        i33 = i12;
                                        columnIndex6 = i14;
                                        i34 = i11;
                                        eventInstancesMap2 = eventInstancesMap;
                                        columnIndex11 = i10;
                                        columnIndex15 = i9;
                                    } catch (DateException e42) {
                                        e = e42;
                                        eventInstancesMap = eventInstancesMap4;
                                        i = i29;
                                        duration = duration4;
                                        time = time4;
                                        i15 = i27;
                                        if (Log.isLoggable("CalendarProvider2", 6)) {
                                        }
                                        time2 = time;
                                        columnIndex12 = i16;
                                        columnIndex7 = i2;
                                        columnIndex2 = i5;
                                        columnIndex4 = i7;
                                        columnIndex5 = i8;
                                        columnIndex14 = i3;
                                        columnIndex = i4;
                                        columnIndex16 = i6;
                                        columnIndex13 = i;
                                        duration3 = duration;
                                        i32 = i15;
                                        columnIndex10 = i13;
                                        i33 = i12;
                                        columnIndex6 = i14;
                                        i34 = i11;
                                        eventInstancesMap2 = eventInstancesMap;
                                        columnIndex11 = i10;
                                        columnIndex15 = i9;
                                    }
                                }
                                long millis = duration4.getMillis();
                                int length = jArrExpand.length;
                                int i36 = i35;
                                while (i36 < length) {
                                    Time time5 = time4;
                                    try {
                                        j9 = jArrExpand[i36];
                                        contentValues2 = new ContentValues();
                                        jArr = jArrExpand;
                                        contentValues2.put("event_id", l2);
                                        i30 = length;
                                        contentValues2.put("begin", Long.valueOf(j9));
                                        i31 = i27;
                                        l = l2;
                                        j10 = j9 + millis;
                                    } catch (TimeFormatException e43) {
                                        e = e43;
                                        eventInstancesMap = eventInstancesMap4;
                                        i = i29;
                                        duration = duration4;
                                        i15 = i27;
                                        time = time5;
                                    } catch (DateException e44) {
                                        e = e44;
                                        eventInstancesMap = eventInstancesMap4;
                                        i = i29;
                                        duration = duration4;
                                        i15 = i27;
                                        time = time5;
                                    }
                                    try {
                                        contentValues2.put("end", Long.valueOf(j10));
                                        computeTimezoneDependentFields(j9, j10, time5, contentValues2);
                                        eventInstancesMap4.add(syncIdKey, contentValues2);
                                        i36++;
                                        time4 = time5;
                                        jArrExpand = jArr;
                                        length = i30;
                                        i27 = i31;
                                        l2 = l;
                                    } catch (TimeFormatException e45) {
                                        e = e45;
                                        eventInstancesMap = eventInstancesMap4;
                                        i = i29;
                                        duration = duration4;
                                        time = time5;
                                        i15 = i31;
                                        if (Log.isLoggable("CalendarProvider2", 6)) {
                                        }
                                        time2 = time;
                                        columnIndex12 = i16;
                                        columnIndex7 = i2;
                                        columnIndex2 = i5;
                                        columnIndex4 = i7;
                                        columnIndex5 = i8;
                                        columnIndex14 = i3;
                                        columnIndex = i4;
                                        columnIndex16 = i6;
                                        columnIndex13 = i;
                                        duration3 = duration;
                                        i32 = i15;
                                        columnIndex10 = i13;
                                        i33 = i12;
                                        columnIndex6 = i14;
                                        i34 = i11;
                                        eventInstancesMap2 = eventInstancesMap;
                                        columnIndex11 = i10;
                                        columnIndex15 = i9;
                                    } catch (DateException e46) {
                                        e = e46;
                                        eventInstancesMap = eventInstancesMap4;
                                        i = i29;
                                        duration = duration4;
                                        time = time5;
                                        i15 = i31;
                                        if (Log.isLoggable("CalendarProvider2", 6)) {
                                        }
                                        time2 = time;
                                        columnIndex12 = i16;
                                        columnIndex7 = i2;
                                        columnIndex2 = i5;
                                        columnIndex4 = i7;
                                        columnIndex5 = i8;
                                        columnIndex14 = i3;
                                        columnIndex = i4;
                                        columnIndex16 = i6;
                                        columnIndex13 = i;
                                        duration3 = duration;
                                        i32 = i15;
                                        columnIndex10 = i13;
                                        i33 = i12;
                                        columnIndex6 = i14;
                                        i34 = i11;
                                        eventInstancesMap2 = eventInstancesMap;
                                        columnIndex11 = i10;
                                        columnIndex15 = i9;
                                    }
                                }
                                eventInstancesMap = eventInstancesMap4;
                                i = i29;
                                duration2 = duration4;
                                time = time4;
                                i15 = i27;
                                time2 = time;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration2;
                                columnIndex12 = i16;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                i32 = i15;
                                columnIndex11 = i10;
                                columnIndex6 = i14;
                            } catch (TimeFormatException e47) {
                                e = e47;
                                eventInstancesMap = eventInstancesMap4;
                                duration = duration4;
                                time = time4;
                                i15 = i27;
                                i = i20;
                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                }
                                time2 = time;
                                columnIndex12 = i16;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration;
                                i32 = i15;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex6 = i14;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                columnIndex11 = i10;
                                columnIndex15 = i9;
                            } catch (DateException e48) {
                                e = e48;
                                eventInstancesMap = eventInstancesMap4;
                                duration = duration4;
                                time = time4;
                                i15 = i27;
                                i = i20;
                                if (Log.isLoggable("CalendarProvider2", 6)) {
                                }
                                time2 = time;
                                columnIndex12 = i16;
                                columnIndex7 = i2;
                                columnIndex2 = i5;
                                columnIndex4 = i7;
                                columnIndex5 = i8;
                                columnIndex14 = i3;
                                columnIndex = i4;
                                columnIndex16 = i6;
                                columnIndex13 = i;
                                duration3 = duration;
                                i32 = i15;
                                columnIndex10 = i13;
                                i33 = i12;
                                columnIndex6 = i14;
                                i34 = i11;
                                eventInstancesMap2 = eventInstancesMap;
                                columnIndex11 = i10;
                                columnIndex15 = i9;
                            }
                        } catch (TimeFormatException e49) {
                            e = e49;
                            i9 = i26;
                            int i37 = i32;
                            EventInstancesMap eventInstancesMap5 = eventInstancesMap2;
                            i13 = i17;
                            i10 = i18;
                            i16 = i21;
                            i14 = i22;
                            i2 = i23;
                            i11 = i24;
                            i12 = i25;
                            eventInstancesMap = eventInstancesMap5;
                            duration = duration3;
                            i = i20;
                            time = time3;
                            i15 = i37;
                        } catch (DateException e50) {
                            e = e50;
                            i9 = i26;
                            int i38 = i32;
                            EventInstancesMap eventInstancesMap6 = eventInstancesMap2;
                            i13 = i17;
                            i10 = i18;
                            i16 = i21;
                            i14 = i22;
                            i2 = i23;
                            i11 = i24;
                            i12 = i25;
                            eventInstancesMap = eventInstancesMap6;
                            duration = duration3;
                            i = i20;
                            time = time3;
                            i15 = i38;
                        }
                        columnIndex15 = i9;
                    } else if (Log.isLoggable("CalendarProvider2", 3)) {
                        Log.d("CalendarProvider2", "Found deleted recurring event in Events table.  Ignoring.");
                    }
                    columnIndex15 = i26;
                    time2 = time3;
                    columnIndex10 = i17;
                    columnIndex11 = i18;
                    columnIndex2 = i5;
                    columnIndex4 = i7;
                    columnIndex5 = i8;
                    columnIndex13 = i20;
                    columnIndex12 = i21;
                    columnIndex6 = i22;
                    columnIndex7 = i23;
                    i34 = i24;
                    columnIndex14 = i3;
                    columnIndex = i4;
                    columnIndex16 = i6;
                    i33 = i25;
                }
            } else {
                i18 = columnIndex11;
                i5 = columnIndex2;
                i7 = columnIndex4;
                string2 = cursor.getString(columnIndex12);
                String string52 = cursor.getString(columnIndex13);
                if (cursor.isNull(columnIndex14)) {
                }
                i19 = cursor.getInt(columnIndex);
                if (cursor.getInt(columnIndex16) == 0) {
                }
                i21 = columnIndex12;
                string3 = cursor.getString(columnIndex6);
                String string62 = cursor.getString(columnIndex7);
                i22 = columnIndex6;
                i23 = columnIndex7;
                i24 = i34;
                String string72 = cursor.getString(i24);
                i3 = columnIndex14;
                i25 = i33;
                String string82 = cursor.getString(i25);
                i4 = columnIndex;
                i6 = columnIndex16;
                j5 = j4;
                i26 = columnIndex15;
                j6 = cursor.getLong(i26);
                syncIdKey = getSyncIdKey(string2, j6);
                recurrenceSet = new RecurrenceSet(string3, string62, string72, string82);
                if (recurrenceSet.hasRecurrence()) {
                }
            }
        }
        EventInstancesMap eventInstancesMap7 = eventInstancesMap2;
        Set<String> setKeySet = eventInstancesMap7.keySet();
        Iterator<String> it = setKeySet.iterator();
        while (it.hasNext()) {
            for (ContentValues contentValues3 : eventInstancesMap7.get(it.next())) {
                if (contentValues3.containsKey("ORIGINAL_EVENT_AND_CALENDAR")) {
                    String asString = contentValues3.getAsString("ORIGINAL_EVENT_AND_CALENDAR");
                    long jLongValue = contentValues3.getAsLong("originalInstanceTime").longValue();
                    InstancesList instancesList = eventInstancesMap7.get(asString);
                    if (instancesList != null) {
                        for (int size = instancesList.size() - 1; size >= 0; size--) {
                            if (instancesList.get(size).getAsLong("begin").longValue() == jLongValue) {
                                instancesList.remove(size);
                            }
                        }
                    }
                }
            }
        }
        Iterator<String> it2 = setKeySet.iterator();
        while (it2.hasNext()) {
            for (ContentValues contentValues4 : eventInstancesMap7.get(it2.next())) {
                Integer asInteger = contentValues4.getAsInteger("eventStatus");
                boolean zBooleanValue = contentValues4.containsKey("deleted") ? contentValues4.getAsBoolean("deleted").booleanValue() : false;
                if (asInteger == null || asInteger.intValue() != 2) {
                    if (!zBooleanValue) {
                        contentValues4.remove("deleted");
                        contentValues4.remove("ORIGINAL_EVENT_AND_CALENDAR");
                        contentValues4.remove("originalInstanceTime");
                        contentValues4.remove("eventStatus");
                        this.mDbHelper.instancesReplace(contentValues4);
                    }
                }
            }
        }
    }

    protected void expandInstanceRangeLocked(long j, long j2, String str) {
        if (Log.isLoggable("CalInstances", 2)) {
            Log.v("CalInstances", "Expanding events between " + j + " and " + j2);
        }
        Cursor entries = getEntries(j, j2);
        try {
            performInstanceExpansion(j, j2, str, entries);
        } finally {
            if (entries != null) {
                entries.close();
            }
        }
    }

    private Cursor getEntries(long j, long j2) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("view_events");
        sQLiteQueryBuilder.setProjectionMap(CalendarProvider2.sEventsProjectionMap);
        String strValueOf = String.valueOf(j);
        String strValueOf2 = String.valueOf(j2);
        sQLiteQueryBuilder.appendWhere("((dtstart <= ? AND (lastDate IS NULL OR lastDate >= ?)) OR (originalInstanceTime IS NOT NULL AND originalInstanceTime <= ? AND originalInstanceTime >= ?)) AND (sync_events != ?) AND (lastSynced = ?)");
        Cursor cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), EXPAND_COLUMNS, null, new String[]{strValueOf2, strValueOf, strValueOf2, String.valueOf(j - 604800000), "0", "0"}, null, null, null);
        if (Log.isLoggable("CalInstances", 2)) {
            Log.v("CalInstances", "Instance expansion:  got " + cursorQuery.getCount() + " entries");
        }
        return cursorQuery;
    }

    public void updateInstancesLocked(ContentValues contentValues, long j, boolean z, SQLiteDatabase sQLiteDatabase) {
        MetaData.Fields fieldsLocked = this.mMetaData.getFieldsLocked();
        if (fieldsLocked.maxInstance == 0) {
            return;
        }
        Long asLong = contentValues.getAsLong("dtstart");
        if (asLong == null) {
            if (z) {
                throw new RuntimeException("DTSTART missing.");
            }
            if (Log.isLoggable("CalInstances", 2)) {
                Log.v("CalInstances", "Missing DTSTART.  No need to update instance.");
                return;
            }
            return;
        }
        boolean z2 = true;
        boolean z3 = false;
        if (!z) {
            sQLiteDatabase.delete("Instances", "event_id=?", new String[]{String.valueOf(j)});
        }
        if (CalendarProvider2.isRecurrenceEvent(contentValues.getAsString("rrule"), contentValues.getAsString("rdate"), contentValues.getAsString("original_id"), contentValues.getAsString("original_sync_id"))) {
            Long asLong2 = contentValues.getAsLong("lastDate");
            Long asLong3 = contentValues.getAsLong("originalInstanceTime");
            boolean z4 = asLong.longValue() <= fieldsLocked.maxInstance && (asLong2 == null || asLong2.longValue() >= fieldsLocked.minInstance);
            if (asLong3 == null || asLong3.longValue() > fieldsLocked.maxInstance || asLong3.longValue() < fieldsLocked.minInstance - 604800000) {
                z2 = false;
            }
            if (CalendarProvider2.DEBUG_INSTANCES) {
                Log.d("CalInstances-i", "Recurrence: inside=" + z4 + ", affects=" + z2);
            }
            if (z4 || z2) {
                updateRecurrenceInstancesLocked(contentValues, j, sQLiteDatabase);
                return;
            }
            return;
        }
        Long asLong4 = contentValues.getAsLong("dtend");
        if (asLong4 == null) {
            asLong4 = asLong;
        }
        if (asLong.longValue() <= fieldsLocked.maxInstance && asLong4.longValue() >= fieldsLocked.minInstance) {
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("event_id", Long.valueOf(j));
            contentValues2.put("begin", asLong);
            contentValues2.put("end", asLong4);
            Integer asInteger = contentValues.getAsInteger("allDay");
            if (asInteger != null && asInteger.intValue() != 0) {
                z3 = true;
            }
            Time time = new Time();
            if (z3) {
                time.timezone = "UTC";
            } else {
                time.timezone = fieldsLocked.timezone;
            }
            computeTimezoneDependentFields(asLong.longValue(), asLong4.longValue(), time, contentValues2);
            this.mDbHelper.instancesInsert(contentValues2);
        }
    }

    private void updateRecurrenceInstancesLocked(ContentValues contentValues, long j, SQLiteDatabase sQLiteDatabase) {
        MetaData.Fields fieldsLocked = this.mMetaData.getFieldsLocked();
        String timezoneInstances = this.mCalendarCache.readTimezoneInstances();
        String asString = contentValues.getAsString("original_sync_id");
        if (asString == null) {
            asString = getEventValue(sQLiteDatabase, j, "original_sync_id");
        }
        if (asString == null && (asString = contentValues.getAsString("_sync_id")) == null) {
            asString = getEventValue(sQLiteDatabase, j, "_sync_id");
        }
        if (asString == null) {
            String asString2 = contentValues.getAsString("original_id");
            if (asString2 == null) {
                asString2 = getEventValue(sQLiteDatabase, j, "original_id");
            }
            if (asString2 == null) {
                asString2 = String.valueOf(j);
            }
            sQLiteDatabase.delete("Instances", "_id IN (SELECT Instances._id as _id FROM Instances INNER JOIN Events ON (Events._id=Instances.event_id) WHERE Events._id=? OR Events.original_id=?)", new String[]{asString2, asString2});
        } else {
            sQLiteDatabase.delete("Instances", "_id IN (SELECT Instances._id as _id FROM Instances INNER JOIN Events ON (Events._id=Instances.event_id) WHERE Events._sync_id=? OR Events.original_sync_id=?)", new String[]{asString, asString});
        }
        Cursor relevantRecurrenceEntries = getRelevantRecurrenceEntries(asString, j);
        try {
            performInstanceExpansion(fieldsLocked.minInstance, fieldsLocked.maxInstance, timezoneInstances, relevantRecurrenceEntries);
        } finally {
            if (relevantRecurrenceEntries != null) {
                relevantRecurrenceEntries.close();
            }
        }
    }

    private Cursor getRelevantRecurrenceEntries(String str, long j) {
        String[] strArr;
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("view_events");
        sQLiteQueryBuilder.setProjectionMap(CalendarProvider2.sEventsProjectionMap);
        if (str == null) {
            sQLiteQueryBuilder.appendWhere("_id=?");
            strArr = new String[]{String.valueOf(j)};
        } else {
            sQLiteQueryBuilder.appendWhere("(_sync_id=? OR original_sync_id=?) AND lastSynced = ?");
            strArr = new String[]{str, str, "0"};
        }
        if (Log.isLoggable("CalInstances", 2)) {
            Log.v("CalInstances", "Retrieving events to expand: " + sQLiteQueryBuilder.toString());
        }
        return sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), EXPAND_COLUMNS, null, strArr, null, null, null);
    }

    static String getSyncIdKey(String str, long j) {
        return j + ":" + str;
    }

    static void computeTimezoneDependentFields(long j, long j2, Time time, ContentValues contentValues) {
        time.set(j);
        int julianDay = Time.getJulianDay(j, time.gmtoff);
        int i = (time.hour * 60) + time.minute;
        time.set(j2);
        int julianDay2 = Time.getJulianDay(j2, time.gmtoff);
        int i2 = (time.hour * 60) + time.minute;
        if (i2 == 0 && julianDay2 > julianDay) {
            i2 = 1440;
            julianDay2--;
        }
        contentValues.put("startDay", Integer.valueOf(julianDay));
        contentValues.put("endDay", Integer.valueOf(julianDay2));
        contentValues.put("startMinute", Integer.valueOf(i));
        contentValues.put("endMinute", Integer.valueOf(i2));
    }
}
