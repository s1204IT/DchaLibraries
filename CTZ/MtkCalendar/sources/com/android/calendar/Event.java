package com.android.calendar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.calendar.PDebug;
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
        Event event = new Event();
        event.title = this.title;
        event.color = this.color;
        event.location = this.location;
        event.allDay = this.allDay;
        event.startDay = this.startDay;
        event.endDay = this.endDay;
        event.startTime = this.startTime;
        event.endTime = this.endTime;
        event.startMillis = this.startMillis;
        event.endMillis = this.endMillis;
        event.hasAlarm = this.hasAlarm;
        event.isRepeating = this.isRepeating;
        event.selfAttendeeStatus = this.selfAttendeeStatus;
        event.organizer = this.organizer;
        event.guestsCanModify = this.guestsCanModify;
        return event;
    }

    public final void copyTo(Event event) {
        event.id = this.id;
        event.title = this.title;
        event.color = this.color;
        event.location = this.location;
        event.allDay = this.allDay;
        event.startDay = this.startDay;
        event.endDay = this.endDay;
        event.startTime = this.startTime;
        event.endTime = this.endTime;
        event.startMillis = this.startMillis;
        event.endMillis = this.endMillis;
        event.hasAlarm = this.hasAlarm;
        event.isRepeating = this.isRepeating;
        event.selfAttendeeStatus = this.selfAttendeeStatus;
        event.organizer = this.organizer;
        event.guestsCanModify = this.guestsCanModify;
    }

    public static void loadEvents(Context context, ArrayList<Event> arrayList, int i, int i2, int i3, AtomicInteger atomicInteger) throws Throwable {
        Cursor cursorInstancesQuery;
        String str;
        String str2;
        PDebug.Start("Event.loadEvents");
        arrayList.clear();
        int i4 = (i2 + i) - 1;
        Cursor cursor = null;
        try {
            if (!GeneralPreferences.getSharedPreferences(context).getBoolean("preferences_hide_declined", false)) {
                str = "dispAllday=0";
                str2 = "dispAllday=1";
            } else {
                str2 = "dispAllday=1 AND selfAttendeeStatus!=2";
                str = "dispAllday=0 AND selfAttendeeStatus!=2";
            }
            PDebug.Start("Event.loadEvents.queryNormalEvents");
            Cursor cursorInstancesQuery2 = instancesQuery(context.getContentResolver(), EVENT_PROJECTION, i, i4, str, null, "begin ASC, end DESC, title ASC");
            try {
                PDebug.EndAndStart("Event.loadEvents.queryNormalEvents", "Event.loadEvents.queryAlldayEvents");
                cursorInstancesQuery = instancesQuery(context.getContentResolver(), EVENT_PROJECTION, i, i4, str2, null, "startDay ASC, endDay DESC, title ASC");
                try {
                    PDebug.End("Event.loadEvents.queryAlldayEvents");
                    if (i3 == atomicInteger.get()) {
                        buildEventsFromCursor(arrayList, cursorInstancesQuery2, context, i, i4);
                        buildEventsFromCursor(arrayList, cursorInstancesQuery, context, i, i4);
                        if (cursorInstancesQuery2 != null) {
                            cursorInstancesQuery2.close();
                        }
                        if (cursorInstancesQuery != null) {
                            cursorInstancesQuery.close();
                        }
                        PDebug.End("Event.loadEvents");
                        return;
                    }
                    if (cursorInstancesQuery2 != null) {
                        cursorInstancesQuery2.close();
                    }
                    if (cursorInstancesQuery != null) {
                        cursorInstancesQuery.close();
                    }
                    PDebug.End("Event.loadEvents");
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorInstancesQuery2;
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (cursorInstancesQuery != null) {
                        cursorInstancesQuery.close();
                    }
                    PDebug.End("Event.loadEvents");
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                cursorInstancesQuery = null;
            }
        } catch (Throwable th3) {
            th = th3;
            cursorInstancesQuery = null;
        }
    }

    private static final Cursor instancesQuery(ContentResolver contentResolver, String[] strArr, int i, int i2, String str, String[] strArr2, String str2) {
        String[] strArr3;
        String str3;
        String str4 = "visible=?";
        String[] strArr4 = {"1"};
        Uri.Builder builderBuildUpon = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, i);
        ContentUris.appendId(builderBuildUpon, i2);
        if (TextUtils.isEmpty(str)) {
            str3 = str4;
            strArr3 = strArr4;
        } else {
            str4 = "(" + str + ") AND visible=?";
            if (strArr2 != null && strArr2.length > 0) {
                String[] strArr5 = (String[]) Arrays.copyOf(strArr2, strArr2.length + 1);
                strArr5[strArr5.length - 1] = strArr4[0];
                strArr3 = strArr5;
                str3 = str4;
            }
        }
        return contentResolver.query(builderBuildUpon.build(), strArr, str3, strArr3, str2 == null ? "begin ASC" : str2);
    }

    public static void buildEventsFromCursor(ArrayList<Event> arrayList, Cursor cursor, Context context, int i, int i2) {
        if (cursor == null || arrayList == null) {
            Log.e("CalEvent", "buildEventsFromCursor: null cursor or null events list!");
            return;
        }
        if (cursor.getCount() == 0) {
            return;
        }
        Resources resources = context.getResources();
        mNoTitleString = resources.getString(R.string.no_title_label);
        mNoColorColor = resources.getColor(R.color.event_center);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            Event eventGenerateEventFromCursor = generateEventFromCursor(cursor);
            if (eventGenerateEventFromCursor.startDay <= i2 && eventGenerateEventFromCursor.endDay >= i) {
                arrayList.add(eventGenerateEventFromCursor);
            }
        }
    }

    private static Event generateEventFromCursor(Cursor cursor) {
        boolean z;
        boolean z2;
        boolean z3;
        Event event = new Event();
        event.id = cursor.getLong(5);
        event.title = cursor.getString(0);
        event.location = cursor.getString(1);
        if (cursor.getInt(2) == 0) {
            z = false;
        } else {
            z = true;
        }
        event.allDay = z;
        event.organizer = cursor.getString(17);
        if (cursor.getInt(18) == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        event.guestsCanModify = z2;
        if (event.title == null || event.title.length() == 0) {
            event.title = mNoTitleString;
        }
        if (!cursor.isNull(3)) {
            event.color = Utils.getDisplayColorFromColor(cursor.getInt(3));
        } else {
            event.color = mNoColorColor;
        }
        long j = cursor.getLong(6);
        long j2 = cursor.getLong(7);
        event.startMillis = j;
        event.startTime = cursor.getInt(11);
        event.startDay = cursor.getInt(9);
        event.endMillis = j2;
        event.endTime = cursor.getInt(12);
        event.endDay = cursor.getInt(10);
        if (cursor.getInt(13) == 0) {
            z3 = false;
        } else {
            z3 = true;
        }
        event.hasAlarm = z3;
        String string = cursor.getString(14);
        String string2 = cursor.getString(15);
        if (!TextUtils.isEmpty(string) || !TextUtils.isEmpty(string2)) {
            event.isRepeating = true;
        } else {
            event.isRepeating = false;
        }
        event.selfAttendeeStatus = cursor.getInt(16);
        if (event.selfAttendeeStatus == 2) {
            event.color = Utils.getDeclinedColorFromColor(event.color, 136, -1);
        }
        return event;
    }

    static void computePositions(ArrayList<Event> arrayList, long j) {
        if (arrayList == null) {
            return;
        }
        doComputePositions(arrayList, j, false);
        doComputePositions(arrayList, j, true);
    }

    private static void doComputePositions(ArrayList<Event> arrayList, long j, boolean z) {
        Event event;
        long jRemoveAlldayActiveEvents;
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        long j2 = j < 0 ? 0L : j;
        long j3 = 0;
        int i = 0;
        for (Event event2 : arrayList) {
            if (event2.drawAsAllday() == z) {
                if (!z) {
                    event = event2;
                    jRemoveAlldayActiveEvents = removeNonAlldayActiveEvents(event2, arrayList2.iterator(), j2, j3);
                } else {
                    event = event2;
                    jRemoveAlldayActiveEvents = removeAlldayActiveEvents(event, arrayList2.iterator(), j3);
                }
                if (arrayList2.isEmpty()) {
                    Iterator it = arrayList3.iterator();
                    while (it.hasNext()) {
                        ((Event) it.next()).setMaxColumns(i);
                    }
                    arrayList3.clear();
                    i = 0;
                    jRemoveAlldayActiveEvents = 0;
                }
                int iFindFirstZeroBit = findFirstZeroBit(jRemoveAlldayActiveEvents);
                if (iFindFirstZeroBit == 64) {
                    iFindFirstZeroBit = 63;
                }
                j3 = jRemoveAlldayActiveEvents | (1 << iFindFirstZeroBit);
                event.setColumn(iFindFirstZeroBit);
                arrayList2.add(event);
                arrayList3.add(event);
                int size = arrayList2.size();
                if (i < size) {
                    i = size;
                }
            }
        }
        Iterator it2 = arrayList3.iterator();
        while (it2.hasNext()) {
            ((Event) it2.next()).setMaxColumns(i);
        }
    }

    private static long removeAlldayActiveEvents(Event event, Iterator<Event> it, long j) {
        while (it.hasNext()) {
            Event next = it.next();
            if (next.endDay < event.startDay) {
                j &= ~(1 << next.getColumn());
                it.remove();
            }
        }
        return j;
    }

    private static long removeNonAlldayActiveEvents(Event event, Iterator<Event> it, long j, long j2) {
        long startMillis = event.getStartMillis();
        while (it.hasNext()) {
            Event next = it.next();
            if (next.getStartMillis() + Math.max(next.getEndMillis() - next.getStartMillis(), j) <= startMillis) {
                j2 &= ~(1 << next.getColumn());
                it.remove();
            }
        }
        return j2;
    }

    public static int findFirstZeroBit(long j) {
        for (int i = 0; i < 64; i++) {
            if (((1 << i) & j) == 0) {
                return i;
            }
        }
        return 64;
    }

    public String getTitleAndLocation() {
        String string = this.title.toString();
        if (this.location != null) {
            String string2 = this.location.toString();
            if (!string.endsWith(string2)) {
                return string + ", " + string2;
            }
            return string;
        }
        return string;
    }

    public void setColumn(int i) {
        this.mColumn = i;
    }

    public int getColumn() {
        return this.mColumn;
    }

    public void setMaxColumns(int i) {
        this.mMaxColumns = i;
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
