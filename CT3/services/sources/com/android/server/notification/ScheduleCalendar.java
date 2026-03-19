package com.android.server.notification;

import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

public class ScheduleCalendar {
    private ZenModeConfig.ScheduleInfo mSchedule;
    private final ArraySet<Integer> mDays = new ArraySet<>();
    private final Calendar mCalendar = Calendar.getInstance();

    public String toString() {
        return "ScheduleCalendar[mDays=" + this.mDays + ", mSchedule=" + this.mSchedule + "]";
    }

    public void setSchedule(ZenModeConfig.ScheduleInfo schedule) {
        if (Objects.equals(this.mSchedule, schedule)) {
            return;
        }
        this.mSchedule = schedule;
        updateDays();
    }

    public void maybeSetNextAlarm(long now, long nextAlarm) {
        if (this.mSchedule == null || !this.mSchedule.exitAtAlarm || now <= this.mSchedule.nextAlarm) {
            return;
        }
        this.mSchedule.nextAlarm = nextAlarm;
    }

    public void setTimeZone(TimeZone tz) {
        this.mCalendar.setTimeZone(tz);
    }

    public long getNextChangeTime(long now) {
        if (this.mSchedule == null) {
            return 0L;
        }
        long nextStart = getNextTime(now, this.mSchedule.startHour, this.mSchedule.startMinute);
        long nextEnd = getNextTime(now, this.mSchedule.endHour, this.mSchedule.endMinute);
        long nextScheduleTime = Math.min(nextStart, nextEnd);
        return nextScheduleTime;
    }

    private long getNextTime(long now, int hr, int min) {
        long time = getTime(now, hr, min);
        return time <= now ? addDays(time, 1) : time;
    }

    private long getTime(long millis, int hour, int min) {
        this.mCalendar.setTimeInMillis(millis);
        this.mCalendar.set(11, hour);
        this.mCalendar.set(12, min);
        this.mCalendar.set(13, 0);
        this.mCalendar.set(14, 0);
        return this.mCalendar.getTimeInMillis();
    }

    public boolean isInSchedule(long time) {
        if (this.mSchedule == null || this.mDays.size() == 0) {
            return false;
        }
        long start = getTime(time, this.mSchedule.startHour, this.mSchedule.startMinute);
        long end = getTime(time, this.mSchedule.endHour, this.mSchedule.endMinute);
        if (end <= start) {
            end = addDays(end, 1);
        }
        boolean isInSchedule = isInSchedule(-1, time, start, end) ? true : isInSchedule(0, time, start, end);
        if (!isInSchedule || !this.mSchedule.exitAtAlarm || this.mSchedule.nextAlarm == 0 || time < this.mSchedule.nextAlarm) {
            return isInSchedule;
        }
        return false;
    }

    private boolean isInSchedule(int daysOffset, long time, long start, long end) {
        int day = ((((getDayOfWeek(time) - 1) + (daysOffset % 7)) + 7) % 7) + 1;
        return this.mDays.contains(Integer.valueOf(day)) && time >= addDays(start, daysOffset) && time < addDays(end, daysOffset);
    }

    private int getDayOfWeek(long time) {
        this.mCalendar.setTimeInMillis(time);
        return this.mCalendar.get(7);
    }

    private void updateDays() {
        this.mDays.clear();
        if (this.mSchedule == null || this.mSchedule.days == null) {
            return;
        }
        for (int i = 0; i < this.mSchedule.days.length; i++) {
            this.mDays.add(Integer.valueOf(this.mSchedule.days[i]));
        }
    }

    private long addDays(long time, int days) {
        this.mCalendar.setTimeInMillis(time);
        this.mCalendar.add(5, days);
        return this.mCalendar.getTimeInMillis();
    }
}
