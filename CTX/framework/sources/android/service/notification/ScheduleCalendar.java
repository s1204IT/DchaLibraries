package android.service.notification;

import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import android.util.Log;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

public class ScheduleCalendar {
    public static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    public static final String TAG = "ScheduleCalendar";
    private ZenModeConfig.ScheduleInfo mSchedule;
    private final ArraySet<Integer> mDays = new ArraySet<>();
    private final Calendar mCalendar = Calendar.getInstance();

    public String toString() {
        return "ScheduleCalendar[mDays=" + this.mDays + ", mSchedule=" + this.mSchedule + "]";
    }

    public boolean exitAtAlarm() {
        return this.mSchedule.exitAtAlarm;
    }

    public void setSchedule(ZenModeConfig.ScheduleInfo scheduleInfo) {
        if (Objects.equals(this.mSchedule, scheduleInfo)) {
            return;
        }
        this.mSchedule = scheduleInfo;
        updateDays();
    }

    public void maybeSetNextAlarm(long j, long j2) {
        if (this.mSchedule == null || !this.mSchedule.exitAtAlarm) {
            return;
        }
        if (j2 == 0) {
            this.mSchedule.nextAlarm = 0L;
        }
        if (j2 > j) {
            if (this.mSchedule.nextAlarm == 0) {
                this.mSchedule.nextAlarm = j2;
                return;
            } else {
                this.mSchedule.nextAlarm = Math.min(this.mSchedule.nextAlarm, j2);
                return;
            }
        }
        if (this.mSchedule.nextAlarm < j) {
            if (DEBUG) {
                Log.d(TAG, "All alarms are in the past " + this.mSchedule.nextAlarm);
            }
            this.mSchedule.nextAlarm = 0L;
        }
    }

    public void setTimeZone(TimeZone timeZone) {
        this.mCalendar.setTimeZone(timeZone);
    }

    public long getNextChangeTime(long j) {
        if (this.mSchedule == null) {
            return 0L;
        }
        return Math.min(getNextTime(j, this.mSchedule.startHour, this.mSchedule.startMinute), getNextTime(j, this.mSchedule.endHour, this.mSchedule.endMinute));
    }

    private long getNextTime(long j, int i, int i2) {
        long time = getTime(j, i, i2);
        return time <= j ? addDays(time, 1) : time;
    }

    private long getTime(long j, int i, int i2) {
        this.mCalendar.setTimeInMillis(j);
        this.mCalendar.set(11, i);
        this.mCalendar.set(12, i2);
        this.mCalendar.set(13, 0);
        this.mCalendar.set(14, 0);
        return this.mCalendar.getTimeInMillis();
    }

    public boolean isInSchedule(long j) {
        if (this.mSchedule == null || this.mDays.size() == 0) {
            return false;
        }
        long time = getTime(j, this.mSchedule.startHour, this.mSchedule.startMinute);
        long time2 = getTime(j, this.mSchedule.endHour, this.mSchedule.endMinute);
        if (time2 <= time) {
            time2 = addDays(time2, 1);
        }
        long j2 = time2;
        return isInSchedule(-1, j, time, j2) || isInSchedule(0, j, time, j2);
    }

    public boolean isAlarmInSchedule(long j, long j2) {
        if (this.mSchedule == null || this.mDays.size() == 0) {
            return false;
        }
        long time = getTime(j, this.mSchedule.startHour, this.mSchedule.startMinute);
        long time2 = getTime(j, this.mSchedule.endHour, this.mSchedule.endMinute);
        if (time2 <= time) {
            time2 = addDays(time2, 1);
        }
        long j3 = time2;
        return (isInSchedule(-1, j, time, j3) && isInSchedule(-1, j2, time, j3)) || (isInSchedule(0, j, time, j3) && isInSchedule(0, j2, time, j3));
    }

    public boolean shouldExitForAlarm(long j) {
        return this.mSchedule != null && this.mSchedule.exitAtAlarm && this.mSchedule.nextAlarm != 0 && j >= this.mSchedule.nextAlarm && isAlarmInSchedule(this.mSchedule.nextAlarm, j);
    }

    private boolean isInSchedule(int i, long j, long j2, long j3) {
        return this.mDays.contains(Integer.valueOf(((((getDayOfWeek(j) - 1) + (i % 7)) + 7) % 7) + 1)) && j >= addDays(j2, i) && j < addDays(j3, i);
    }

    private int getDayOfWeek(long j) {
        this.mCalendar.setTimeInMillis(j);
        return this.mCalendar.get(7);
    }

    private void updateDays() {
        this.mDays.clear();
        if (this.mSchedule != null && this.mSchedule.days != null) {
            for (int i = 0; i < this.mSchedule.days.length; i++) {
                this.mDays.add(Integer.valueOf(this.mSchedule.days[i]));
            }
        }
    }

    private long addDays(long j, int i) {
        this.mCalendar.setTimeInMillis(j);
        this.mCalendar.add(5, i);
        return this.mCalendar.getTimeInMillis();
    }
}
