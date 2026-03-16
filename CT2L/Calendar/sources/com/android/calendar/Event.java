package com.android.calendar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class Event implements Cloneable {
    public static final String[] EVENT_PROJECTION = {"title", "eventLocation", "allDay", "displayColor", "eventTimezone", "event_id", "begin", "end", "_id", "startDay", "endDay", "startMinute", "endMinute", "hasAlarm", "rrule", "rdate", "selfAttendeeStatus", "organizer", "guestsCanModify", "allDay=1 OR (end-begin)>=86400000 AS dispAllday"};
    private static int mNoColorColor;
    private static String mNoTitleString;
    public boolean allDay;
    public float bottom;
    public int color;
    public int endDay;
    public long endMillis;
    public int endTime;
    public boolean guestsCanModify;
    public boolean hasAlarm;
    public long id;
    public boolean isRepeating;
    public float left;
    public CharSequence location;
    private int mColumn;
    private int mMaxColumns;
    public Event nextDown;
    public Event nextLeft;
    public Event nextRight;
    public Event nextUp;
    public String organizer;
    public float right;
    public int selfAttendeeStatus;
    public int startDay;
    public long startMillis;
    public int startTime;
    public CharSequence title;
    public float top;

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[3] = "calendar_color";
        }
    }

    public final Object clone() throws CloneNotSupportedException {
        super.clone();
        Event e = new Event();
        e.title = this.title;
        e.color = this.color;
        e.location = this.location;
        e.allDay = this.allDay;
        e.startDay = this.startDay;
        e.endDay = this.endDay;
        e.startTime = this.startTime;
        e.endTime = this.endTime;
        e.startMillis = this.startMillis;
        e.endMillis = this.endMillis;
        e.hasAlarm = this.hasAlarm;
        e.isRepeating = this.isRepeating;
        e.selfAttendeeStatus = this.selfAttendeeStatus;
        e.organizer = this.organizer;
        e.guestsCanModify = this.guestsCanModify;
        return e;
    }

    public final void copyTo(Event dest) {
        dest.id = this.id;
        dest.title = this.title;
        dest.color = this.color;
        dest.location = this.location;
        dest.allDay = this.allDay;
        dest.startDay = this.startDay;
        dest.endDay = this.endDay;
        dest.startTime = this.startTime;
        dest.endTime = this.endTime;
        dest.startMillis = this.startMillis;
        dest.endMillis = this.endMillis;
        dest.hasAlarm = this.hasAlarm;
        dest.isRepeating = this.isRepeating;
        dest.selfAttendeeStatus = this.selfAttendeeStatus;
        dest.organizer = this.organizer;
        dest.guestsCanModify = this.guestsCanModify;
    }

    public static void loadEvents(Context context, ArrayList<Event> events, int startDay, int days, int requestId, AtomicInteger sequenceNumber) {
        Cursor cEvents = null;
        Cursor cAllday = null;
        events.clear();
        int endDay = (startDay + days) - 1;
        try {
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
            boolean hideDeclined = prefs.getBoolean("preferences_hide_declined", false);
            String where = "dispAllday=0";
            String whereAllday = "dispAllday=1";
            if (hideDeclined) {
                where = "dispAllday=0 AND selfAttendeeStatus!=2";
                whereAllday = "dispAllday=1 AND selfAttendeeStatus!=2";
            }
            cEvents = instancesQuery(context.getContentResolver(), EVENT_PROJECTION, startDay, endDay, where, null, "begin ASC, end DESC, title ASC");
            cAllday = instancesQuery(context.getContentResolver(), EVENT_PROJECTION, startDay, endDay, whereAllday, null, "startDay ASC, endDay DESC, title ASC");
            if (requestId == sequenceNumber.get()) {
                buildEventsFromCursor(events, cEvents, context, startDay, endDay);
                buildEventsFromCursor(events, cAllday, context, startDay, endDay);
                if (cEvents != null) {
                    cEvents.close();
                }
                if (cAllday != null) {
                    cAllday.close();
                    return;
                }
                return;
            }
        } finally {
            if (cEvents != null) {
                cEvents.close();
            }
            if (cAllday != null) {
                cAllday.close();
            }
        }
    }

    private static final Cursor instancesQuery(ContentResolver cr, String[] projection, int startDay, int endDay, String selection, String[] selectionArgs, String orderBy) {
        String selection2;
        String[] selectionArgs2;
        String[] WHERE_CALENDARS_ARGS = {"1"};
        Uri.Builder builder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
        ContentUris.appendId(builder, startDay);
        ContentUris.appendId(builder, endDay);
        if (TextUtils.isEmpty(selection)) {
            selection2 = "visible=?";
            selectionArgs2 = WHERE_CALENDARS_ARGS;
        } else {
            selection2 = "(" + selection + ") AND visible=?";
            if (selectionArgs != null && selectionArgs.length > 0) {
                selectionArgs2 = (String[]) Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
                selectionArgs2[selectionArgs2.length - 1] = WHERE_CALENDARS_ARGS[0];
            } else {
                selectionArgs2 = WHERE_CALENDARS_ARGS;
            }
        }
        return cr.query(builder.build(), projection, selection2, selectionArgs2, orderBy == null ? "begin ASC" : orderBy);
    }

    public static void buildEventsFromCursor(ArrayList<Event> events, Cursor cEvents, Context context, int startDay, int endDay) {
        if (cEvents == null || events == null) {
            Log.e("CalEvent", "buildEventsFromCursor: null cursor or null events list!");
            return;
        }
        int count = cEvents.getCount();
        if (count != 0) {
            Resources res = context.getResources();
            mNoTitleString = res.getString(R.string.no_title_label);
            mNoColorColor = res.getColor(R.color.event_center);
            cEvents.moveToPosition(-1);
            while (cEvents.moveToNext()) {
                Event e = generateEventFromCursor(cEvents);
                if (e.startDay <= endDay && e.endDay >= startDay) {
                    events.add(e);
                }
            }
        }
    }

    private static Event generateEventFromCursor(Cursor cEvents) {
        Event e = new Event();
        e.id = cEvents.getLong(5);
        e.title = cEvents.getString(0);
        e.location = cEvents.getString(1);
        e.allDay = cEvents.getInt(2) != 0;
        e.organizer = cEvents.getString(17);
        e.guestsCanModify = cEvents.getInt(18) != 0;
        if (e.title == null || e.title.length() == 0) {
            e.title = mNoTitleString;
        }
        if (!cEvents.isNull(3)) {
            e.color = Utils.getDisplayColorFromColor(cEvents.getInt(3));
        } else {
            e.color = mNoColorColor;
        }
        long eStart = cEvents.getLong(6);
        long eEnd = cEvents.getLong(7);
        e.startMillis = eStart;
        e.startTime = cEvents.getInt(11);
        e.startDay = cEvents.getInt(9);
        e.endMillis = eEnd;
        e.endTime = cEvents.getInt(12);
        e.endDay = cEvents.getInt(10);
        e.hasAlarm = cEvents.getInt(13) != 0;
        String rrule = cEvents.getString(14);
        String rdate = cEvents.getString(15);
        if (!TextUtils.isEmpty(rrule) || !TextUtils.isEmpty(rdate)) {
            e.isRepeating = true;
        } else {
            e.isRepeating = false;
        }
        e.selfAttendeeStatus = cEvents.getInt(16);
        return e;
    }

    static void computePositions(ArrayList<Event> eventsList, long minimumDurationMillis) {
        if (eventsList != null) {
            doComputePositions(eventsList, minimumDurationMillis, false);
            doComputePositions(eventsList, minimumDurationMillis, true);
        }
    }

    private static void doComputePositions(ArrayList<Event> eventsList, long minimumDurationMillis, boolean doAlldayEvents) {
        long colMask;
        ArrayList<Event> activeList = new ArrayList<>();
        ArrayList<Event> groupList = new ArrayList<>();
        if (minimumDurationMillis < 0) {
            minimumDurationMillis = 0;
        }
        long colMask2 = 0;
        int maxCols = 0;
        for (Event event : eventsList) {
            if (event.drawAsAllday() == doAlldayEvents) {
                if (!doAlldayEvents) {
                    colMask = removeNonAlldayActiveEvents(event, activeList.iterator(), minimumDurationMillis, colMask2);
                } else {
                    colMask = removeAlldayActiveEvents(event, activeList.iterator(), colMask2);
                }
                if (activeList.isEmpty()) {
                    for (Event ev : groupList) {
                        ev.setMaxColumns(maxCols);
                    }
                    maxCols = 0;
                    colMask = 0;
                    groupList.clear();
                }
                int col = findFirstZeroBit(colMask);
                if (col == 64) {
                    col = 63;
                }
                colMask2 = colMask | (1 << col);
                event.setColumn(col);
                activeList.add(event);
                groupList.add(event);
                int len = activeList.size();
                if (maxCols < len) {
                    maxCols = len;
                }
            }
        }
        for (Event ev2 : groupList) {
            ev2.setMaxColumns(maxCols);
        }
    }

    private static long removeAlldayActiveEvents(Event event, Iterator<Event> iter, long colMask) {
        while (iter.hasNext()) {
            Event active = iter.next();
            if (active.endDay < event.startDay) {
                colMask &= (1 << active.getColumn()) ^ (-1);
                iter.remove();
            }
        }
        return colMask;
    }

    private static long removeNonAlldayActiveEvents(Event event, Iterator<Event> iter, long minDurationMillis, long colMask) {
        long start = event.getStartMillis();
        while (iter.hasNext()) {
            Event active = iter.next();
            long duration = Math.max(active.getEndMillis() - active.getStartMillis(), minDurationMillis);
            if (active.getStartMillis() + duration <= start) {
                colMask &= (1 << active.getColumn()) ^ (-1);
                iter.remove();
            }
        }
        return colMask;
    }

    public static int findFirstZeroBit(long val) {
        for (int ii = 0; ii < 64; ii++) {
            if (((1 << ii) & val) == 0) {
                return ii;
            }
        }
        return 64;
    }

    public String getTitleAndLocation() {
        String text = this.title.toString();
        if (this.location != null) {
            String locationString = this.location.toString();
            if (!text.endsWith(locationString)) {
                return text + ", " + locationString;
            }
            return text;
        }
        return text;
    }

    public void setColumn(int column) {
        this.mColumn = column;
    }

    public int getColumn() {
        return this.mColumn;
    }

    public void setMaxColumns(int maxColumns) {
        this.mMaxColumns = maxColumns;
    }

    public int getMaxColumns() {
        return this.mMaxColumns;
    }

    public long getStartMillis() {
        return this.startMillis;
    }

    public long getEndMillis() {
        return this.endMillis;
    }

    public boolean drawAsAllday() {
        return this.allDay || this.endMillis - this.startMillis >= 86400000;
    }
}
