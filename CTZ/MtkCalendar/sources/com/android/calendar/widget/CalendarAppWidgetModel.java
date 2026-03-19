package com.android.calendar.widget;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

class CalendarAppWidgetModel {
    private static final String TAG = CalendarAppWidgetModel.class.getSimpleName();
    final Context mContext;
    final List<DayInfo> mDayInfos;
    final List<EventInfo> mEventInfos;
    private String mHomeTZName;
    final int mMaxJulianDay;
    final long mNow = System.currentTimeMillis();
    final List<RowInfo> mRowInfos;
    private boolean mShowTZ;
    final int mTodayJulianDay;

    static class RowInfo {
        final int mIndex;
        final int mType;

        RowInfo(int i, int i2) {
            this.mType = i;
            this.mIndex = i2;
        }
    }

    static class EventInfo {
        boolean allDay;
        int color;
        long end;
        long id;
        int selfAttendeeStatus;
        long start;
        String title;
        String when;
        String where;
        int visibWhen = 8;
        int visibWhere = 8;
        int visibTitle = 8;

        public String toString() {
            return "EventInfo [visibTitle=" + this.visibTitle + ", title=" + this.title + ", visibWhen=" + this.visibWhen + ", id=" + this.id + ", when=" + this.when + ", visibWhere=" + this.visibWhere + ", where=" + this.where + ", color=" + String.format("0x%x", Integer.valueOf(this.color)) + ", selfAttendeeStatus=" + this.selfAttendeeStatus + "]";
        }

        public int hashCode() {
            return (31 * ((((((((((((((((((((((this.allDay ? 1231 : 1237) + 31) * 31) + ((int) (this.id ^ (this.id >>> 32)))) * 31) + ((int) (this.end ^ (this.end >>> 32)))) * 31) + ((int) (this.start ^ (this.start >>> 32)))) * 31) + (this.title == null ? 0 : this.title.hashCode())) * 31) + this.visibTitle) * 31) + this.visibWhen) * 31) + this.visibWhere) * 31) + (this.when == null ? 0 : this.when.hashCode())) * 31) + (this.where != null ? this.where.hashCode() : 0)) * 31) + this.color)) + this.selfAttendeeStatus;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            EventInfo eventInfo = (EventInfo) obj;
            if (this.id != eventInfo.id || this.allDay != eventInfo.allDay || this.end != eventInfo.end || this.start != eventInfo.start) {
                return false;
            }
            if (this.title == null) {
                if (eventInfo.title != null) {
                    return false;
                }
            } else if (!this.title.equals(eventInfo.title)) {
                return false;
            }
            if (this.visibTitle != eventInfo.visibTitle || this.visibWhen != eventInfo.visibWhen || this.visibWhere != eventInfo.visibWhere) {
                return false;
            }
            if (this.when == null) {
                if (eventInfo.when != null) {
                    return false;
                }
            } else if (!this.when.equals(eventInfo.when)) {
                return false;
            }
            if (this.where == null) {
                if (eventInfo.where != null) {
                    return false;
                }
            } else if (!this.where.equals(eventInfo.where)) {
                return false;
            }
            if (this.color == eventInfo.color && this.selfAttendeeStatus == eventInfo.selfAttendeeStatus) {
                return true;
            }
            return false;
        }
    }

    static class DayInfo {
        final String mDayLabel;
        final int mJulianDay;

        DayInfo(int i, String str) {
            this.mJulianDay = i;
            this.mDayLabel = str;
        }

        public String toString() {
            return this.mDayLabel;
        }

        public int hashCode() {
            return (31 * ((this.mDayLabel == null ? 0 : this.mDayLabel.hashCode()) + 31)) + this.mJulianDay;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            DayInfo dayInfo = (DayInfo) obj;
            if (this.mDayLabel == null) {
                if (dayInfo.mDayLabel != null) {
                    return false;
                }
            } else if (!this.mDayLabel.equals(dayInfo.mDayLabel)) {
                return false;
            }
            if (this.mJulianDay == dayInfo.mJulianDay) {
                return true;
            }
            return false;
        }
    }

    public CalendarAppWidgetModel(Context context, String str) {
        Time time = new Time(str);
        time.setToNow();
        this.mTodayJulianDay = Time.getJulianDay(this.mNow, time.gmtoff);
        this.mMaxJulianDay = (this.mTodayJulianDay + 7) - 1;
        this.mEventInfos = new ArrayList(50);
        this.mRowInfos = new ArrayList(50);
        this.mDayInfos = new ArrayList(8);
        this.mContext = context;
    }

    public void buildFromCursor(Cursor cursor, String str) {
        Time time;
        Cursor cursor2 = cursor;
        Time time2 = new Time(str);
        int i = 7;
        ArrayList<LinkedList> arrayList = new ArrayList(7);
        int i2 = 0;
        for (int i3 = 0; i3 < 7; i3++) {
            arrayList.add(new LinkedList());
        }
        time2.setToNow();
        int i4 = 1;
        this.mShowTZ = !TextUtils.equals(str, Time.getCurrentTimezone());
        if (this.mShowTZ) {
            this.mHomeTZName = TimeZone.getTimeZone(str).getDisplayName(time2.isDst != 0, 1);
        }
        cursor2.moveToPosition(-1);
        String timeZone = Utils.getTimeZone(this.mContext, null);
        while (cursor.moveToNext()) {
            int position = cursor.getPosition();
            long j = cursor2.getLong(5);
            boolean z = cursor2.getInt(i2) != 0 ? i4 : i2;
            long jConvertAlldayUtcToLocal = cursor2.getLong(i4);
            long jConvertAlldayUtcToLocal2 = cursor2.getLong(2);
            String string = cursor2.getString(3);
            String string2 = cursor2.getString(4);
            int i5 = cursor2.getInt(6);
            int i6 = cursor2.getInt(i);
            int i7 = cursor2.getInt(8);
            int i8 = cursor2.getInt(9);
            if (z != 0) {
                jConvertAlldayUtcToLocal = Utils.convertAlldayUtcToLocal(time2, jConvertAlldayUtcToLocal, timeZone);
                jConvertAlldayUtcToLocal2 = Utils.convertAlldayUtcToLocal(time2, jConvertAlldayUtcToLocal2, timeZone);
            }
            long j2 = jConvertAlldayUtcToLocal;
            long j3 = jConvertAlldayUtcToLocal2;
            Log.d(TAG, "Row #" + position + " allDay:" + z + " start:" + j2 + " end:" + j3 + " eventId:" + j);
            String str2 = timeZone;
            if (j3 < this.mNow) {
                timeZone = str2;
                i4 = 1;
                i2 = 0;
                i = 7;
            } else {
                boolean z2 = z;
                int size = this.mEventInfos.size();
                ArrayList arrayList2 = arrayList;
                Time time3 = time2;
                this.mEventInfos.add(populateEventInfo(j, z, j2, j3, i5, i6, string, string2, i7, i8));
                int iMax = Math.max(i5, this.mTodayJulianDay);
                int iMin = Math.min(i6, this.mMaxJulianDay);
                while (iMax <= iMin) {
                    ArrayList arrayList3 = arrayList2;
                    LinkedList linkedList = (LinkedList) arrayList3.get(iMax - this.mTodayJulianDay);
                    int i9 = size;
                    RowInfo rowInfo = new RowInfo(1, i9);
                    if (z2) {
                        linkedList.addFirst(rowInfo);
                    } else {
                        linkedList.add(rowInfo);
                    }
                    iMax++;
                    arrayList2 = arrayList3;
                    size = i9;
                }
                arrayList = arrayList2;
                i4 = 1;
                i = 7;
                timeZone = str2;
                time2 = time3;
                i2 = 0;
                cursor2 = cursor;
            }
        }
        Time time4 = time2;
        int i10 = this.mTodayJulianDay;
        int size2 = 0;
        for (LinkedList linkedList2 : arrayList) {
            if (!linkedList2.isEmpty()) {
                if (i10 != this.mTodayJulianDay) {
                    time = time4;
                    DayInfo dayInfoPopulateDayInfo = populateDayInfo(i10, time);
                    int size3 = this.mDayInfos.size();
                    this.mDayInfos.add(dayInfoPopulateDayInfo);
                    this.mRowInfos.add(new RowInfo(0, size3));
                } else {
                    time = time4;
                }
                this.mRowInfos.addAll(linkedList2);
                size2 += linkedList2.size();
            } else {
                time = time4;
            }
            i10++;
            if (size2 < 20) {
                time4 = time;
            } else {
                return;
            }
        }
    }

    private EventInfo populateEventInfo(long j, boolean z, long j2, long j3, int i, int i2, String str, String str2, int i3, int i4) {
        EventInfo eventInfo = new EventInfo();
        StringBuilder sb = new StringBuilder();
        if (z) {
            sb.append(Utils.formatDateRange(this.mContext, j2, j3, 98320));
        } else {
            int i5 = 98305;
            if (DateFormat.is24HourFormat(this.mContext)) {
                i5 = 98433;
            }
            if (i2 > i) {
                i5 |= 16;
            }
            sb.append(Utils.formatDateRange(this.mContext, j2, j3, i5));
            if (this.mShowTZ) {
                sb.append(" ");
                sb.append(this.mHomeTZName);
            }
        }
        eventInfo.id = j;
        eventInfo.start = j2;
        eventInfo.end = j3;
        eventInfo.allDay = z;
        eventInfo.when = sb.toString();
        eventInfo.visibWhen = 0;
        eventInfo.color = i3;
        eventInfo.selfAttendeeStatus = i4;
        if (TextUtils.isEmpty(str)) {
            eventInfo.title = this.mContext.getString(R.string.no_title_label);
        } else {
            eventInfo.title = str;
        }
        eventInfo.visibTitle = 0;
        if (!TextUtils.isEmpty(str2)) {
            eventInfo.visibWhere = 0;
            eventInfo.where = str2;
        } else {
            eventInfo.visibWhere = 8;
        }
        return eventInfo;
    }

    private DayInfo populateDayInfo(int i, Time time) {
        String dateRange;
        long julianDay = time.setJulianDay(i);
        if (i == this.mTodayJulianDay + 1) {
            dateRange = this.mContext.getString(R.string.agenda_tomorrow, Utils.formatDateRange(this.mContext, julianDay, julianDay, 524304).toString());
        } else {
            dateRange = Utils.formatDateRange(this.mContext, julianDay, julianDay, 524306);
        }
        return new DayInfo(i, dateRange);
    }

    public String toString() {
        return "\nCalendarAppWidgetModel [eventInfos=" + this.mEventInfos + "]";
    }
}
