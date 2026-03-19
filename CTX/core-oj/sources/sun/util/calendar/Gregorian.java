package sun.util.calendar;

import java.util.TimeZone;
import sun.util.calendar.BaseCalendar;

public class Gregorian extends BaseCalendar {

    static class Date extends BaseCalendar.Date {
        protected Date() {
        }

        protected Date(TimeZone timeZone) {
            super(timeZone);
        }

        @Override
        public int getNormalizedYear() {
            return getYear();
        }

        @Override
        public void setNormalizedYear(int i) {
            setYear(i);
        }
    }

    Gregorian() {
    }

    @Override
    public String getName() {
        return "gregorian";
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
}
