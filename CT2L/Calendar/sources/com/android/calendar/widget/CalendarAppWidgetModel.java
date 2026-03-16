package com.android.calendar.widget;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
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

        RowInfo(int type, int index) {
            this.mType = type;
            this.mIndex = index;
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
            int result = (this.allDay ? 1231 : 1237) + 31;
            return (((((((((((((((((((((result * 31) + ((int) (this.id ^ (this.id >>> 32)))) * 31) + ((int) (this.end ^ (this.end >>> 32)))) * 31) + ((int) (this.start ^ (this.start >>> 32)))) * 31) + (this.title == null ? 0 : this.title.hashCode())) * 31) + this.visibTitle) * 31) + this.visibWhen) * 31) + this.visibWhere) * 31) + (this.when == null ? 0 : this.when.hashCode())) * 31) + (this.where != null ? this.where.hashCode() : 0)) * 31) + this.color) * 31) + this.selfAttendeeStatus;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                EventInfo other = (EventInfo) obj;
                if (this.id == other.id && this.allDay == other.allDay && this.end == other.end && this.start == other.start) {
                    if (this.title == null) {
                        if (other.title != null) {
                            return false;
                        }
                    } else if (!this.title.equals(other.title)) {
                        return false;
                    }
                    if (this.visibTitle == other.visibTitle && this.visibWhen == other.visibWhen && this.visibWhere == other.visibWhere) {
                        if (this.when == null) {
                            if (other.when != null) {
                                return false;
                            }
                        } else if (!this.when.equals(other.when)) {
                            return false;
                        }
                        if (this.where == null) {
                            if (other.where != null) {
                                return false;
                            }
                        } else if (!this.where.equals(other.where)) {
                            return false;
                        }
                        return this.color == other.color && this.selfAttendeeStatus == other.selfAttendeeStatus;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
    }

    static class DayInfo {
        final String mDayLabel;
        final int mJulianDay;

        DayInfo(int julianDay, String label) {
            this.mJulianDay = julianDay;
            this.mDayLabel = label;
        }

        public String toString() {
            return this.mDayLabel;
        }

        public int hashCode() {
            int result = (this.mDayLabel == null ? 0 : this.mDayLabel.hashCode()) + 31;
            return (result * 31) + this.mJulianDay;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                DayInfo other = (DayInfo) obj;
                if (this.mDayLabel == null) {
                    if (other.mDayLabel != null) {
                        return false;
                    }
                } else if (!this.mDayLabel.equals(other.mDayLabel)) {
                    return false;
                }
                return this.mJulianDay == other.mJulianDay;
            }
            return false;
        }
    }

    public CalendarAppWidgetModel(Context context, String timeZone) {
        Time time = new Time(timeZone);
        time.setToNow();
        this.mTodayJulianDay = Time.getJulianDay(this.mNow, time.gmtoff);
        this.mMaxJulianDay = (this.mTodayJulianDay + 7) - 1;
        this.mEventInfos = new ArrayList(50);
        this.mRowInfos = new ArrayList(50);
        this.mDayInfos = new ArrayList(8);
        this.mContext = context;
    }

    public void buildFromCursor(Cursor cursor, String timeZone) {
        Time recycle = new Time(timeZone);
        ArrayList<LinkedList<RowInfo>> mBuckets = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            mBuckets.add(new LinkedList<>());
        }
        recycle.setToNow();
        this.mShowTZ = !TextUtils.equals(timeZone, Time.getCurrentTimezone());
        if (this.mShowTZ) {
            this.mHomeTZName = TimeZone.getTimeZone(timeZone).getDisplayName(recycle.isDst != 0, 0);
        }
        cursor.moveToPosition(-1);
        String tz = Utils.getTimeZone(this.mContext, null);
        while (cursor.moveToNext()) {
            cursor.getPosition();
            long eventId = cursor.getLong(5);
            boolean allDay = cursor.getInt(0) != 0;
            long start = cursor.getLong(1);
            long end = cursor.getLong(2);
            String title = cursor.getString(3);
            String location = cursor.getString(4);
            int startDay = cursor.getInt(6);
            int endDay = cursor.getInt(7);
            int color = cursor.getInt(8);
            int selfStatus = cursor.getInt(9);
            if (allDay) {
                start = Utils.convertAlldayUtcToLocal(recycle, start, tz);
                end = Utils.convertAlldayUtcToLocal(recycle, end, tz);
            }
            if (end >= this.mNow) {
                int i2 = this.mEventInfos.size();
                this.mEventInfos.add(populateEventInfo(eventId, allDay, start, end, startDay, endDay, title, location, color, selfStatus));
                int from = Math.max(startDay, this.mTodayJulianDay);
                int to = Math.min(endDay, this.mMaxJulianDay);
                for (int day = from; day <= to; day++) {
                    LinkedList<RowInfo> bucket = mBuckets.get(day - this.mTodayJulianDay);
                    RowInfo rowInfo = new RowInfo(1, i2);
                    if (allDay) {
                        bucket.addFirst(rowInfo);
                    } else {
                        bucket.add(rowInfo);
                    }
                }
            }
        }
        int day2 = this.mTodayJulianDay;
        int count = 0;
        for (LinkedList<RowInfo> bucket2 : mBuckets) {
            if (!bucket2.isEmpty()) {
                if (day2 != this.mTodayJulianDay) {
                    DayInfo dayInfo = populateDayInfo(day2, recycle);
                    int dayIndex = this.mDayInfos.size();
                    this.mDayInfos.add(dayInfo);
                    this.mRowInfos.add(new RowInfo(0, dayIndex));
                }
                this.mRowInfos.addAll(bucket2);
                count += bucket2.size();
            }
            day2++;
            if (count >= 20) {
                return;
            }
        }
    }

    private EventInfo populateEventInfo(long eventId, boolean allDay, long start, long end, int startDay, int endDay, String title, String location, int color, int selfStatus) {
        EventInfo eventInfo = new EventInfo();
        StringBuilder whenString = new StringBuilder();
        if (allDay) {
            whenString.append(Utils.formatDateRange(this.mContext, start, end, 524288 | 16));
        } else {
            int flags = 524288 | 1;
            if (DateFormat.is24HourFormat(this.mContext)) {
                flags |= 128;
            }
            if (endDay > startDay) {
                flags |= 16;
            }
            whenString.append(Utils.formatDateRange(this.mContext, start, end, flags));
            if (this.mShowTZ) {
                whenString.append(" ").append(this.mHomeTZName);
            }
        }
        eventInfo.id = eventId;
        eventInfo.start = start;
        eventInfo.end = end;
        eventInfo.allDay = allDay;
        eventInfo.when = whenString.toString();
        eventInfo.visibWhen = 0;
        eventInfo.color = color;
        eventInfo.selfAttendeeStatus = selfStatus;
        if (TextUtils.isEmpty(title)) {
            eventInfo.title = this.mContext.getString(R.string.no_title_label);
        } else {
            eventInfo.title = title;
        }
        eventInfo.visibTitle = 0;
        if (!TextUtils.isEmpty(location)) {
            eventInfo.visibWhere = 0;
            eventInfo.where = location;
        } else {
            eventInfo.visibWhere = 8;
        }
        return eventInfo;
    }

    private DayInfo populateDayInfo(int julianDay, Time recycle) {
        String label;
        long millis = recycle.setJulianDay(julianDay);
        if (julianDay == this.mTodayJulianDay + 1) {
            label = this.mContext.getString(R.string.agenda_tomorrow, Utils.formatDateRange(this.mContext, millis, millis, 524304).toString());
        } else {
            int flags = 524304 | 2;
            label = Utils.formatDateRange(this.mContext, millis, millis, flags);
        }
        return new DayInfo(julianDay, label);
    }

    public String toString() {
        return "\nCalendarAppWidgetModel [eventInfos=" + this.mEventInfos + "]";
    }
}
