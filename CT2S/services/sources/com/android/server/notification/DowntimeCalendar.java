package com.android.server.notification;

import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import com.android.server.job.controllers.JobStatus;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

public class DowntimeCalendar {
    private ZenModeConfig.DowntimeInfo mInfo;
    private final ArraySet<Integer> mDays = new ArraySet<>();
    private final Calendar mCalendar = Calendar.getInstance();

    public String toString() {
        return "DowntimeCalendar[mDays=" + this.mDays + "]";
    }

    public void setDowntimeInfo(ZenModeConfig.DowntimeInfo info) {
        if (!Objects.equals(this.mInfo, info)) {
            this.mInfo = info;
            updateDays();
        }
    }

    public long nextDowntimeStart(long time) {
        if (this.mInfo == null || this.mDays.size() == 0) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        long start = getTime(time, this.mInfo.startHour, this.mInfo.startMinute);
        for (int i = 0; i < 7; i++) {
            long t = addDays(start, i);
            if (t > time && isInDowntime(t)) {
                return t;
            }
        }
        return JobStatus.NO_LATEST_RUNTIME;
    }

    public void setTimeZone(TimeZone tz) {
        this.mCalendar.setTimeZone(tz);
    }

    public long getNextTime(long now, int hr, int min) {
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

    public boolean isInDowntime(long time) {
        if (this.mInfo == null || this.mDays.size() == 0) {
            return false;
        }
        long start = getTime(time, this.mInfo.startHour, this.mInfo.startMinute);
        long end = getTime(time, this.mInfo.endHour, this.mInfo.endMinute);
        if (end <= start) {
            end = addDays(end, 1);
        }
        return isInDowntime(-1, time, start, end) || isInDowntime(0, time, start, end);
    }

    private boolean isInDowntime(int daysOffset, long time, long start, long end) {
        int day = ((((getDayOfWeek(time) - 1) + (daysOffset % 7)) + 7) % 7) + 1;
        return this.mDays.contains(Integer.valueOf(day)) && time >= addDays(start, daysOffset) && time < addDays(end, daysOffset);
    }

    private int getDayOfWeek(long time) {
        this.mCalendar.setTimeInMillis(time);
        return this.mCalendar.get(7);
    }

    private void updateDays() {
        this.mDays.clear();
        if (this.mInfo != null) {
            int[] days = ZenModeConfig.tryParseDays(this.mInfo.mode);
            for (int i = 0; days != null && i < days.length; i++) {
                this.mDays.add(Integer.valueOf(days[i]));
            }
        }
    }

    private long addDays(long time, int days) {
        this.mCalendar.setTimeInMillis(time);
        this.mCalendar.add(5, days);
        return this.mCalendar.getTimeInMillis();
    }
}
