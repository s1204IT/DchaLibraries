package sun.util.calendar;

import java.util.TimeZone;

public abstract class AbstractCalendar extends CalendarSystem {
    static final int DAY_IN_MILLIS = 86400000;
    static final int EPOCH_OFFSET = 719163;
    static final int HOUR_IN_MILLIS = 3600000;
    static final int MINUTE_IN_MILLIS = 60000;
    static final int SECOND_IN_MILLIS = 1000;
    private Era[] eras;

    protected abstract void getCalendarDateFromFixedDate(CalendarDate calendarDate, long j);

    protected abstract long getFixedDate(CalendarDate calendarDate);

    protected abstract boolean isLeapYear(CalendarDate calendarDate);

    protected AbstractCalendar() {
    }

    @Override
    public Era getEra(String str) {
        if (this.eras != null) {
            for (int i = 0; i < this.eras.length; i++) {
                if (this.eras[i].equals(str)) {
                    return this.eras[i];
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public Era[] getEras() {
        if (this.eras != null) {
            Era[] eraArr = new Era[this.eras.length];
            System.arraycopy(this.eras, 0, eraArr, 0, this.eras.length);
            return eraArr;
        }
        return null;
    }

    @Override
    public void setEra(CalendarDate calendarDate, String str) {
        if (this.eras == null) {
            return;
        }
        for (int i = 0; i < this.eras.length; i++) {
            Era era = this.eras[i];
            if (era != null && era.getName().equals(str)) {
                calendarDate.setEra(era);
                return;
            }
        }
        throw new IllegalArgumentException("unknown era name: " + str);
    }

    protected void setEras(Era[] eraArr) {
        this.eras = eraArr;
    }

    @Override
    public CalendarDate getCalendarDate() {
        return getCalendarDate(System.currentTimeMillis(), newCalendarDate());
    }

    @Override
    public CalendarDate getCalendarDate(long j) {
        return getCalendarDate(j, newCalendarDate());
    }

    @Override
    public CalendarDate getCalendarDate(long j, TimeZone timeZone) {
        return getCalendarDate(j, newCalendarDate(timeZone));
    }

    @Override
    public CalendarDate getCalendarDate(long j, CalendarDate calendarDate) {
        long j2;
        int i;
        int i2;
        TimeZone zone = calendarDate.getZone();
        int i3 = 0;
        if (zone != null) {
            int[] iArr = new int[2];
            int offset = zone.getOffset(j);
            iArr[0] = zone.getRawOffset();
            iArr[1] = offset - iArr[0];
            j2 = offset / DAY_IN_MILLIS;
            int i4 = offset % DAY_IN_MILLIS;
            i = iArr[1];
            i2 = i4;
            i3 = offset;
        } else {
            j2 = 0;
            i = 0;
            i2 = 0;
        }
        calendarDate.setZoneOffset(i3);
        calendarDate.setDaylightSaving(i);
        long j3 = j2 + (j / 86400000);
        int i5 = i2 + ((int) (j % 86400000));
        if (i5 >= DAY_IN_MILLIS) {
            i5 -= DAY_IN_MILLIS;
            j3++;
        } else {
            while (i5 < 0) {
                i5 += DAY_IN_MILLIS;
                j3--;
            }
        }
        getCalendarDateFromFixedDate(calendarDate, j3 + 719163);
        setTimeOfDay(calendarDate, i5);
        calendarDate.setLeapYear(isLeapYear(calendarDate));
        calendarDate.setNormalized(true);
        return calendarDate;
    }

    @Override
    public long getTime(CalendarDate calendarDate) {
        int offset;
        long fixedDate = ((getFixedDate(calendarDate) - 719163) * 86400000) + getTimeOfDay(calendarDate);
        TimeZone zone = calendarDate.getZone();
        if (zone != null) {
            if (calendarDate.isNormalized()) {
                return fixedDate - ((long) calendarDate.getZoneOffset());
            }
            int[] iArr = new int[2];
            if (calendarDate.isStandardTime()) {
                offset = zone.getOffset(fixedDate - ((long) zone.getRawOffset()));
            } else {
                offset = zone.getOffset(fixedDate - ((long) zone.getRawOffset()));
            }
        } else {
            offset = 0;
        }
        long j = fixedDate - ((long) offset);
        getCalendarDate(j, calendarDate);
        return j;
    }

    protected long getTimeOfDay(CalendarDate calendarDate) {
        long timeOfDay = calendarDate.getTimeOfDay();
        if (timeOfDay != Long.MIN_VALUE) {
            return timeOfDay;
        }
        long timeOfDayValue = getTimeOfDayValue(calendarDate);
        calendarDate.setTimeOfDay(timeOfDayValue);
        return timeOfDayValue;
    }

    public long getTimeOfDayValue(CalendarDate calendarDate) {
        return (((((((long) calendarDate.getHours()) * 60) + ((long) calendarDate.getMinutes())) * 60) + ((long) calendarDate.getSeconds())) * 1000) + ((long) calendarDate.getMillis());
    }

    @Override
    public CalendarDate setTimeOfDay(CalendarDate calendarDate, int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        boolean zIsNormalized = calendarDate.isNormalized();
        int i2 = i / HOUR_IN_MILLIS;
        int i3 = i % HOUR_IN_MILLIS;
        int i4 = i3 / MINUTE_IN_MILLIS;
        int i5 = i3 % MINUTE_IN_MILLIS;
        calendarDate.setHours(i2);
        calendarDate.setMinutes(i4);
        calendarDate.setSeconds(i5 / 1000);
        calendarDate.setMillis(i5 % 1000);
        calendarDate.setTimeOfDay(i);
        if (i2 < 24 && zIsNormalized) {
            calendarDate.setNormalized(zIsNormalized);
        }
        return calendarDate;
    }

    @Override
    public int getWeekLength() {
        return 7;
    }

    @Override
    public CalendarDate getNthDayOfWeek(int i, int i2, CalendarDate calendarDate) {
        long dayOfWeekDateAfter;
        CalendarDate calendarDate2 = (CalendarDate) calendarDate.clone();
        normalize(calendarDate2);
        long fixedDate = getFixedDate(calendarDate2);
        if (i > 0) {
            dayOfWeekDateAfter = ((long) (7 * i)) + getDayOfWeekDateBefore(fixedDate, i2);
        } else {
            dayOfWeekDateAfter = ((long) (7 * i)) + getDayOfWeekDateAfter(fixedDate, i2);
        }
        getCalendarDateFromFixedDate(calendarDate2, dayOfWeekDateAfter);
        return calendarDate2;
    }

    static long getDayOfWeekDateBefore(long j, int i) {
        return getDayOfWeekDateOnOrBefore(j - 1, i);
    }

    static long getDayOfWeekDateAfter(long j, int i) {
        return getDayOfWeekDateOnOrBefore(j + 7, i);
    }

    public static long getDayOfWeekDateOnOrBefore(long j, int i) {
        long j2 = j - ((long) (i - 1));
        if (j2 >= 0) {
            return j - (j2 % 7);
        }
        return j - CalendarUtils.mod(j2, 7L);
    }

    public boolean validateTime(CalendarDate calendarDate) {
        int minutes;
        int seconds;
        int millis;
        int hours = calendarDate.getHours();
        if (hours < 0 || hours >= 24 || (minutes = calendarDate.getMinutes()) < 0 || minutes >= 60 || (seconds = calendarDate.getSeconds()) < 0 || seconds >= 60 || (millis = calendarDate.getMillis()) < 0 || millis >= 1000) {
            return false;
        }
        return true;
    }

    int normalizeTime(CalendarDate calendarDate) {
        long jFloorDivide;
        long timeOfDay = getTimeOfDay(calendarDate);
        if (timeOfDay >= 86400000) {
            jFloorDivide = timeOfDay / 86400000;
            timeOfDay %= 86400000;
        } else if (timeOfDay < 0) {
            jFloorDivide = CalendarUtils.floorDivide(timeOfDay, 86400000L);
            if (jFloorDivide != 0) {
                timeOfDay -= 86400000 * jFloorDivide;
            }
        } else {
            jFloorDivide = 0;
        }
        if (jFloorDivide != 0) {
            calendarDate.setTimeOfDay(timeOfDay);
        }
        calendarDate.setMillis((int) (timeOfDay % 1000));
        long j = timeOfDay / 1000;
        calendarDate.setSeconds((int) (j % 60));
        long j2 = j / 60;
        calendarDate.setMinutes((int) (j2 % 60));
        calendarDate.setHours((int) (j2 / 60));
        return (int) jFloorDivide;
    }
}
