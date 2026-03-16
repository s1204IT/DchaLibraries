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
import java.util.Set;

public class CalendarInstancesHelper {
    private static final String[] EXPAND_COLUMNS = {"_id", "_sync_id", "eventStatus", "dtstart", "dtend", "eventTimezone", "rrule", "rdate", "exrule", "exdate", "duration", "allDay", "original_sync_id", "originalInstanceTime", "calendar_id", "deleted"};
    private CalendarCache mCalendarCache;
    private CalendarDatabaseHelper mDbHelper;
    private MetaData mMetaData;

    public static final class InstancesList extends ArrayList<ContentValues> {
    }

    public static final class EventInstancesMap extends HashMap<String, InstancesList> {
        public void add(String syncIdKey, ContentValues values) {
            InstancesList instances = get(syncIdKey);
            if (instances == null) {
                instances = new InstancesList();
                put(syncIdKey, instances);
            }
            instances.add(values);
        }
    }

    public CalendarInstancesHelper(CalendarDatabaseHelper calendarDbHelper, MetaData metaData) {
        this.mDbHelper = calendarDbHelper;
        this.mMetaData = metaData;
        this.mCalendarCache = new CalendarCache(this.mDbHelper);
    }

    private static String getEventValue(SQLiteDatabase db, long rowId, String columnName) {
        String where = "SELECT " + columnName + " FROM Events WHERE _id=?";
        return DatabaseUtils.stringForQuery(db, where, new String[]{String.valueOf(rowId)});
    }

    protected void performInstanceExpansion(long begin, long end, String localTimezone, Cursor entries) {
        boolean allDay;
        String eventTimezone;
        long dtstartMillis;
        Long eventId;
        String durationStr;
        String syncId;
        String originalEvent;
        long originalInstanceTimeMillis;
        int status;
        boolean deleted;
        long calendarId;
        String syncIdKey;
        RecurrenceSet recur;
        RecurrenceProcessor rp = new RecurrenceProcessor();
        int statusColumn = entries.getColumnIndex("eventStatus");
        int dtstartColumn = entries.getColumnIndex("dtstart");
        int dtendColumn = entries.getColumnIndex("dtend");
        int eventTimezoneColumn = entries.getColumnIndex("eventTimezone");
        int durationColumn = entries.getColumnIndex("duration");
        int rruleColumn = entries.getColumnIndex("rrule");
        int rdateColumn = entries.getColumnIndex("rdate");
        int exruleColumn = entries.getColumnIndex("exrule");
        int exdateColumn = entries.getColumnIndex("exdate");
        int allDayColumn = entries.getColumnIndex("allDay");
        int idColumn = entries.getColumnIndex("_id");
        int syncIdColumn = entries.getColumnIndex("_sync_id");
        int originalEventColumn = entries.getColumnIndex("original_sync_id");
        int originalInstanceTimeColumn = entries.getColumnIndex("originalInstanceTime");
        int calendarIdColumn = entries.getColumnIndex("calendar_id");
        int deletedColumn = entries.getColumnIndex("deleted");
        EventInstancesMap instancesMap = new EventInstancesMap();
        Duration duration = new Duration();
        Time eventTime = new Time();
        while (entries.moveToNext()) {
            try {
                try {
                    allDay = entries.getInt(allDayColumn) != 0;
                    eventTimezone = entries.getString(eventTimezoneColumn);
                    if (allDay || TextUtils.isEmpty(eventTimezone)) {
                        eventTimezone = "UTC";
                    }
                    dtstartMillis = entries.getLong(dtstartColumn);
                    eventId = Long.valueOf(entries.getLong(idColumn));
                    durationStr = entries.getString(durationColumn);
                    if (durationStr != null) {
                        try {
                            duration.parse(durationStr);
                        } catch (DateException e) {
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                                Log.w("CalendarProvider2", "error parsing duration for event " + eventId + "'" + durationStr + "'", e);
                            }
                            duration.sign = 1;
                            duration.weeks = 0;
                            duration.days = 0;
                            duration.hours = 0;
                            duration.minutes = 0;
                            duration.seconds = 0;
                            durationStr = "+P0S";
                        }
                    }
                    syncId = entries.getString(syncIdColumn);
                    originalEvent = entries.getString(originalEventColumn);
                    originalInstanceTimeMillis = -1;
                    if (!entries.isNull(originalInstanceTimeColumn)) {
                        originalInstanceTimeMillis = entries.getLong(originalInstanceTimeColumn);
                    }
                    status = entries.getInt(statusColumn);
                    deleted = entries.getInt(deletedColumn) != 0;
                    String rruleStr = entries.getString(rruleColumn);
                    String rdateStr = entries.getString(rdateColumn);
                    String exruleStr = entries.getString(exruleColumn);
                    String exdateStr = entries.getString(exdateColumn);
                    calendarId = entries.getLong(calendarIdColumn);
                    syncIdKey = getSyncIdKey(syncId, calendarId);
                    try {
                        recur = new RecurrenceSet(rruleStr, rdateStr, exruleStr, exdateStr);
                    } catch (EventRecurrence.InvalidFormatException e2) {
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                            Log.w("CalendarProvider2", "Could not parse RRULE recurrence string: " + rruleStr, e2);
                        }
                    }
                } catch (TimeFormatException e3) {
                    e = e3;
                }
            } catch (DateException e4) {
                e = e4;
            }
            if (recur != null && recur.hasRecurrence()) {
                if (status == 2) {
                    if (Log.isLoggable("CalendarProvider2", 6)) {
                        Log.e("CalendarProvider2", "Found canceled recurring event in Events table.  Ignoring.");
                    }
                } else if (deleted) {
                    if (Log.isLoggable("CalendarProvider2", 3)) {
                        Log.d("CalendarProvider2", "Found deleted recurring event in Events table.  Ignoring.");
                    }
                } else {
                    eventTime.timezone = eventTimezone;
                    eventTime.set(dtstartMillis);
                    eventTime.allDay = allDay;
                    if (durationStr == null) {
                        if (Log.isLoggable("CalendarProvider2", 6)) {
                            Log.e("CalendarProvider2", "Repeating event has no duration -- should not happen.");
                        }
                        if (allDay) {
                            duration.sign = 1;
                            duration.weeks = 0;
                            duration.days = 1;
                            duration.hours = 0;
                            duration.minutes = 0;
                            duration.seconds = 0;
                        } else {
                            duration.sign = 1;
                            duration.weeks = 0;
                            duration.days = 0;
                            duration.hours = 0;
                            duration.minutes = 0;
                            if (!entries.isNull(dtendColumn)) {
                                long dtendMillis = entries.getLong(dtendColumn);
                                duration.seconds = (int) ((dtendMillis - dtstartMillis) / 1000);
                                String str = "+P" + duration.seconds + "S";
                            } else {
                                duration.seconds = 0;
                            }
                        }
                    }
                    long[] dates = rp.expand(eventTime, recur, begin, end);
                    if (allDay) {
                        eventTime.timezone = "UTC";
                    } else {
                        eventTime.timezone = localTimezone;
                    }
                    long durationMillis = duration.getMillis();
                    int len$ = dates.length;
                    int i$ = 0;
                    ContentValues initialValues = null;
                    while (i$ < len$) {
                        try {
                            long date = dates[i$];
                            ContentValues initialValues2 = new ContentValues();
                            initialValues2.put("event_id", eventId);
                            initialValues2.put("begin", Long.valueOf(date));
                            long dtendMillis2 = date + durationMillis;
                            initialValues2.put("end", Long.valueOf(dtendMillis2));
                            computeTimezoneDependentFields(date, dtendMillis2, eventTime, initialValues2);
                            instancesMap.add(syncIdKey, initialValues2);
                            i$++;
                            initialValues = initialValues2;
                        } catch (TimeFormatException e5) {
                            e = e5;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                                Log.w("CalendarProvider2", "RecurrenceProcessor error ", e);
                            }
                        } catch (DateException e6) {
                            e = e6;
                            if (Log.isLoggable("CalendarProvider2", 6)) {
                                Log.w("CalendarProvider2", "RecurrenceProcessor error ", e);
                            }
                        }
                    }
                }
            } else {
                ContentValues initialValues3 = new ContentValues();
                if (originalEvent != null && originalInstanceTimeMillis != -1) {
                    initialValues3.put("ORIGINAL_EVENT_AND_CALENDAR", getSyncIdKey(originalEvent, calendarId));
                    initialValues3.put("originalInstanceTime", Long.valueOf(originalInstanceTimeMillis));
                    initialValues3.put("eventStatus", Integer.valueOf(status));
                }
                long dtendMillis3 = dtstartMillis;
                if (durationStr == null) {
                    if (!entries.isNull(dtendColumn)) {
                        dtendMillis3 = entries.getLong(dtendColumn);
                    }
                } else {
                    dtendMillis3 = duration.addTo(dtstartMillis);
                }
                if (dtendMillis3 < begin || dtstartMillis > end) {
                    if (originalEvent != null && originalInstanceTimeMillis != -1) {
                        initialValues3.put("eventStatus", (Integer) 2);
                    } else if (Log.isLoggable("CalendarProvider2", 6)) {
                        Log.w("CalendarProvider2", "Unexpected event outside window: " + syncId);
                    }
                }
                initialValues3.put("event_id", eventId);
                initialValues3.put("begin", Long.valueOf(dtstartMillis));
                initialValues3.put("end", Long.valueOf(dtendMillis3));
                initialValues3.put("deleted", Boolean.valueOf(deleted));
                if (allDay) {
                    eventTime.timezone = "UTC";
                } else {
                    eventTime.timezone = localTimezone;
                }
                computeTimezoneDependentFields(dtstartMillis, dtendMillis3, eventTime, initialValues3);
                instancesMap.add(syncIdKey, initialValues3);
            }
        }
        Set<String> keys = instancesMap.keySet();
        for (String syncIdKey2 : keys) {
            InstancesList list = instancesMap.get(syncIdKey2);
            for (ContentValues values : list) {
                if (values.containsKey("ORIGINAL_EVENT_AND_CALENDAR")) {
                    String originalEventPlusCalendar = values.getAsString("ORIGINAL_EVENT_AND_CALENDAR");
                    long originalTime = values.getAsLong("originalInstanceTime").longValue();
                    InstancesList originalList = instancesMap.get(originalEventPlusCalendar);
                    if (originalList != null) {
                        for (int num = originalList.size() - 1; num >= 0; num--) {
                            ContentValues originalValues = originalList.get(num);
                            long beginTime = originalValues.getAsLong("begin").longValue();
                            if (beginTime == originalTime) {
                                originalList.remove(num);
                            }
                        }
                    }
                }
            }
        }
        for (String syncIdKey3 : keys) {
            InstancesList list2 = instancesMap.get(syncIdKey3);
            for (ContentValues values2 : list2) {
                Integer status2 = values2.getAsInteger("eventStatus");
                boolean deleted2 = values2.containsKey("deleted") ? values2.getAsBoolean("deleted").booleanValue() : false;
                if (status2 == null || status2.intValue() != 2) {
                    if (!deleted2) {
                        values2.remove("deleted");
                        values2.remove("ORIGINAL_EVENT_AND_CALENDAR");
                        values2.remove("originalInstanceTime");
                        values2.remove("eventStatus");
                        this.mDbHelper.instancesReplace(values2);
                    }
                }
            }
        }
    }

    protected void expandInstanceRangeLocked(long begin, long end, String localTimezone) {
        if (Log.isLoggable("CalInstances", 2)) {
            Log.v("CalInstances", "Expanding events between " + begin + " and " + end);
        }
        Cursor entries = getEntries(begin, end);
        try {
            performInstanceExpansion(begin, end, localTimezone, entries);
        } finally {
            if (entries != null) {
                entries.close();
            }
        }
    }

    private Cursor getEntries(long begin, long end) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("view_events");
        qb.setProjectionMap(CalendarProvider2.sEventsProjectionMap);
        String beginString = String.valueOf(begin);
        String endString = String.valueOf(end);
        qb.appendWhere("((dtstart <= ? AND (lastDate IS NULL OR lastDate >= ?)) OR (originalInstanceTime IS NOT NULL AND originalInstanceTime <= ? AND originalInstanceTime >= ?)) AND (sync_events != ?) AND (lastSynced = ?)");
        String[] selectionArgs = {endString, beginString, endString, String.valueOf(begin - 604800000), "0", "0"};
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, EXPAND_COLUMNS, null, selectionArgs, null, null, null);
        if (Log.isLoggable("CalInstances", 2)) {
            Log.v("CalInstances", "Instance expansion:  got " + c.getCount() + " entries");
        }
        return c;
    }

    public void updateInstancesLocked(ContentValues values, long rowId, boolean newEvent, SQLiteDatabase db) {
        MetaData.Fields fields = this.mMetaData.getFieldsLocked();
        if (fields.maxInstance != 0) {
            Long dtstartMillis = values.getAsLong("dtstart");
            if (dtstartMillis == null) {
                if (newEvent) {
                    throw new RuntimeException("DTSTART missing.");
                }
                if (Log.isLoggable("CalInstances", 2)) {
                    Log.v("CalInstances", "Missing DTSTART.  No need to update instance.");
                    return;
                }
                return;
            }
            if (!newEvent) {
                db.delete("Instances", "event_id=?", new String[]{String.valueOf(rowId)});
            }
            String rrule = values.getAsString("rrule");
            String rdate = values.getAsString("rdate");
            String originalId = values.getAsString("original_id");
            String originalSyncId = values.getAsString("original_sync_id");
            if (CalendarProvider2.isRecurrenceEvent(rrule, rdate, originalId, originalSyncId)) {
                Long lastDateMillis = values.getAsLong("lastDate");
                Long originalInstanceTime = values.getAsLong("originalInstanceTime");
                boolean insideWindow = dtstartMillis.longValue() <= fields.maxInstance && (lastDateMillis == null || lastDateMillis.longValue() >= fields.minInstance);
                boolean affectsWindow = originalInstanceTime != null && originalInstanceTime.longValue() <= fields.maxInstance && originalInstanceTime.longValue() >= fields.minInstance - 604800000;
                if (insideWindow || affectsWindow) {
                    updateRecurrenceInstancesLocked(values, rowId, db);
                    return;
                }
                return;
            }
            Long dtendMillis = values.getAsLong("dtend");
            if (dtendMillis == null) {
                dtendMillis = dtstartMillis;
            }
            if (dtstartMillis.longValue() <= fields.maxInstance && dtendMillis.longValue() >= fields.minInstance) {
                ContentValues instanceValues = new ContentValues();
                instanceValues.put("event_id", Long.valueOf(rowId));
                instanceValues.put("begin", dtstartMillis);
                instanceValues.put("end", dtendMillis);
                boolean allDay = false;
                Integer allDayInteger = values.getAsInteger("allDay");
                if (allDayInteger != null) {
                    allDay = allDayInteger.intValue() != 0;
                }
                Time local = new Time();
                if (allDay) {
                    local.timezone = "UTC";
                } else {
                    local.timezone = fields.timezone;
                }
                computeTimezoneDependentFields(dtstartMillis.longValue(), dtendMillis.longValue(), local, instanceValues);
                this.mDbHelper.instancesInsert(instanceValues);
            }
        }
    }

    private void updateRecurrenceInstancesLocked(ContentValues values, long rowId, SQLiteDatabase db) {
        String recurrenceSyncId;
        String recurrenceId;
        MetaData.Fields fields = this.mMetaData.getFieldsLocked();
        String instancesTimezone = this.mCalendarCache.readTimezoneInstances();
        String originalSyncId = values.getAsString("original_sync_id");
        if (originalSyncId == null) {
            originalSyncId = getEventValue(db, rowId, "original_sync_id");
        }
        if (originalSyncId != null) {
            recurrenceSyncId = originalSyncId;
        } else {
            recurrenceSyncId = values.getAsString("_sync_id");
            if (recurrenceSyncId == null) {
                recurrenceSyncId = getEventValue(db, rowId, "_sync_id");
            }
        }
        if (recurrenceSyncId == null) {
            String originalId = values.getAsString("original_id");
            if (originalId == null) {
                originalId = getEventValue(db, rowId, "original_id");
            }
            if (originalId != null) {
                recurrenceId = originalId;
            } else {
                recurrenceId = String.valueOf(rowId);
            }
            db.delete("Instances", "_id IN (SELECT Instances._id as _id FROM Instances INNER JOIN Events ON (Events._id=Instances.event_id) WHERE Events._id=? OR Events.original_id=?)", new String[]{recurrenceId, recurrenceId});
        } else {
            db.delete("Instances", "_id IN (SELECT Instances._id as _id FROM Instances INNER JOIN Events ON (Events._id=Instances.event_id) WHERE Events._sync_id=? OR Events.original_sync_id=?)", new String[]{recurrenceSyncId, recurrenceSyncId});
        }
        Cursor entries = getRelevantRecurrenceEntries(recurrenceSyncId, rowId);
        try {
            performInstanceExpansion(fields.minInstance, fields.maxInstance, instancesTimezone, entries);
        } finally {
            if (entries != null) {
                entries.close();
            }
        }
    }

    private Cursor getRelevantRecurrenceEntries(String recurrenceSyncId, long rowId) {
        String[] selectionArgs;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("view_events");
        qb.setProjectionMap(CalendarProvider2.sEventsProjectionMap);
        if (recurrenceSyncId == null) {
            qb.appendWhere("_id=?");
            selectionArgs = new String[]{String.valueOf(rowId)};
        } else {
            qb.appendWhere("(_sync_id=? OR original_sync_id=?) AND lastSynced = ?");
            selectionArgs = new String[]{recurrenceSyncId, recurrenceSyncId, "0"};
        }
        if (Log.isLoggable("CalInstances", 2)) {
            Log.v("CalInstances", "Retrieving events to expand: " + qb.toString());
        }
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        return qb.query(db, EXPAND_COLUMNS, null, selectionArgs, null, null, null);
    }

    static String getSyncIdKey(String syncId, long calendarId) {
        return calendarId + ":" + syncId;
    }

    static void computeTimezoneDependentFields(long begin, long end, Time local, ContentValues values) {
        local.set(begin);
        int startDay = Time.getJulianDay(begin, local.gmtoff);
        int startMinute = (local.hour * 60) + local.minute;
        local.set(end);
        int endDay = Time.getJulianDay(end, local.gmtoff);
        int endMinute = (local.hour * 60) + local.minute;
        if (endMinute == 0 && endDay > startDay) {
            endMinute = 1440;
            endDay--;
        }
        values.put("startDay", Integer.valueOf(startDay));
        values.put("endDay", Integer.valueOf(endDay));
        values.put("startMinute", Integer.valueOf(startMinute));
        values.put("endMinute", Integer.valueOf(endMinute));
    }
}
