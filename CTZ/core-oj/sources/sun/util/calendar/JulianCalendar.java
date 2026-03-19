package sun.util.calendar;

import java.util.TimeZone;
import sun.util.calendar.BaseCalendar;

public class JulianCalendar extends BaseCalendar {
    private static final int BCE = 0;
    private static final int CE = 1;
    private static final int JULIAN_EPOCH = -1;
    static final boolean $assertionsDisabled = false;
    private static final Era[] eras = {new Era("BeforeCommonEra", "B.C.E.", Long.MIN_VALUE, $assertionsDisabled), new Era("CommonEra", "C.E.", -62135709175808L, true)};

    private static class Date extends BaseCalendar.Date {
        protected Date() {
            setCache(1, -1L, 365);
        }

        protected Date(TimeZone timeZone) {
            super(timeZone);
            setCache(1, -1L, 365);
        }

        @Override
        public Date setEra(Era era) {
            if (era != null) {
                if (era != JulianCalendar.eras[0] || era != JulianCalendar.eras[1]) {
                    throw new IllegalArgumentException("unknown era: " + ((Object) era));
                }
                super.setEra(era);
                return this;
            }
            throw new NullPointerException();
        }

        protected void setKnownEra(Era era) {
            super.setEra(era);
        }

        @Override
        public int getNormalizedYear() {
            if (getEra() == JulianCalendar.eras[0]) {
                return 1 - getYear();
            }
            return getYear();
        }

        @Override
        public void setNormalizedYear(int i) {
            if (i <= 0) {
                setYear(1 - i);
                setKnownEra(JulianCalendar.eras[0]);
            } else {
                setYear(i);
                setKnownEra(JulianCalendar.eras[1]);
            }
        }

        @Override
        public String toString() {
            String abbreviation;
            String string = super.toString();
            String strSubstring = string.substring(string.indexOf(84));
            StringBuffer stringBuffer = new StringBuffer();
            Era era = getEra();
            if (era != null && (abbreviation = era.getAbbreviation()) != null) {
                stringBuffer.append(abbreviation);
                stringBuffer.append(' ');
            }
            stringBuffer.append(getYear());
            stringBuffer.append('-');
            CalendarUtils.sprintf0d(stringBuffer, getMonth(), 2).append('-');
            CalendarUtils.sprintf0d(stringBuffer, getDayOfMonth(), 2);
            stringBuffer.append(strSubstring);
            return stringBuffer.toString();
        }
    }

    JulianCalendar() {
        setEras(eras);
    }

    @Override
    public String getName() {
        return "julian";
    }

    @Override
    public Date getCalendarDate() {
        return getCalendarDate(System.currentTimeMillis(), (CalendarDate) newCalendarDate());
    }

    @Override
    public Date getCalendarDate(long j) {
        return getCalendarDate(j, (CalendarDate) newCalendarDate());
    }

    @Override
    public Date getCalendarDate(long j, CalendarDate calendarDate) {
        return (Date) super.getCalendarDate(j, calendarDate);
    }

    @Override
    public Date getCalendarDate(long j, TimeZone timeZone) {
        return getCalendarDate(j, (CalendarDate) newCalendarDate(timeZone));
    }

    @Override
    public Date newCalendarDate() {
        return new Date();
    }

    @Override
    public Date newCalendarDate(TimeZone timeZone) {
        return new Date(timeZone);
    }

    @Override
    public long getFixedDate(int i, int i2, int i3, BaseCalendar.Date date) {
        long jFloorDivide;
        boolean z = (i2 == 1 && i3 == 1) ? true : $assertionsDisabled;
        if (date != null && date.hit(i)) {
            if (!z) {
                return (date.getCachedJan1() + getDayOfYear(i, i2, i3)) - 1;
            }
            return date.getCachedJan1();
        }
        long j = i;
        long j2 = j - 1;
        long j3 = (-2) + (365 * j2) + ((long) i3);
        if (j > 0) {
            jFloorDivide = j3 + (j2 / 4);
        } else {
            jFloorDivide = j3 + CalendarUtils.floorDivide(j2, 4L);
        }
        long jFloorDivide2 = i2 > 0 ? jFloorDivide + (((367 * ((long) i2)) - 362) / 12) : jFloorDivide + CalendarUtils.floorDivide((367 * ((long) i2)) - 362, 12L);
        if (i2 > 2) {
            jFloorDivide2 -= CalendarUtils.isJulianLeapYear(i) ? 1L : 2L;
        }
        if (date != null && z) {
            date.setCache(i, jFloorDivide2, CalendarUtils.isJulianLeapYear(i) ? 366 : 365);
        }
        return jFloorDivide2;
    }

    @Override
    public void getCalendarDateFromFixedDate(CalendarDate calendarDate, long j) {
        int iFloorDivide;
        int iFloorDivide2;
        Date date = (Date) calendarDate;
        long j2 = (4 * (j - (-1))) + 1464;
        if (j2 >= 0) {
            iFloorDivide = (int) (j2 / 1461);
        } else {
            iFloorDivide = (int) CalendarUtils.floorDivide(j2, 1461L);
        }
        int fixedDate = (int) (j - getFixedDate(iFloorDivide, 1, 1, date));
        boolean zIsJulianLeapYear = CalendarUtils.isJulianLeapYear(iFloorDivide);
        if (j >= getFixedDate(iFloorDivide, 3, 1, date)) {
            fixedDate += zIsJulianLeapYear ? 1 : 2;
        }
        int i = (12 * fixedDate) + 373;
        if (i > 0) {
            iFloorDivide2 = i / 367;
        } else {
            iFloorDivide2 = CalendarUtils.floorDivide(i, 367);
        }
        int fixedDate2 = ((int) (j - getFixedDate(iFloorDivide, iFloorDivide2, 1, date))) + 1;
        int dayOfWeekFromFixedDate = getDayOfWeekFromFixedDate(j);
        date.setNormalizedYear(iFloorDivide);
        date.setMonth(iFloorDivide2);
        date.setDayOfMonth(fixedDate2);
        date.setDayOfWeek(dayOfWeekFromFixedDate);
        date.setLeapYear(zIsJulianLeapYear);
        date.setNormalized(true);
    }

    @Override
    public int getYearFromFixedDate(long j) {
        return (int) CalendarUtils.floorDivide((4 * (j - (-1))) + 1464, 1461L);
    }

    @Override
    public int getDayOfWeek(CalendarDate calendarDate) {
        return getDayOfWeekFromFixedDate(getFixedDate(calendarDate));
    }

    @Override
    boolean isLeapYear(int i) {
        return CalendarUtils.isJulianLeapYear(i);
    }
}
