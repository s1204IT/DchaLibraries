package sun.util.calendar;

import java.util.Locale;
import java.util.TimeZone;
import sun.util.calendar.BaseCalendar;

class ImmutableGregorianDate extends BaseCalendar.Date {
    private final BaseCalendar.Date date;

    ImmutableGregorianDate(BaseCalendar.Date date) {
        if (date == null) {
            throw new NullPointerException();
        }
        this.date = date;
    }

    @Override
    public Era getEra() {
        return this.date.getEra();
    }

    @Override
    public CalendarDate setEra(Era era) {
        unsupported();
        return this;
    }

    @Override
    public int getYear() {
        return this.date.getYear();
    }

    @Override
    public CalendarDate setYear(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addYear(int i) {
        unsupported();
        return this;
    }

    @Override
    public boolean isLeapYear() {
        return this.date.isLeapYear();
    }

    @Override
    void setLeapYear(boolean z) {
        unsupported();
    }

    @Override
    public int getMonth() {
        return this.date.getMonth();
    }

    @Override
    public CalendarDate setMonth(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addMonth(int i) {
        unsupported();
        return this;
    }

    @Override
    public int getDayOfMonth() {
        return this.date.getDayOfMonth();
    }

    @Override
    public CalendarDate setDayOfMonth(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addDayOfMonth(int i) {
        unsupported();
        return this;
    }

    @Override
    public int getDayOfWeek() {
        return this.date.getDayOfWeek();
    }

    @Override
    public int getHours() {
        return this.date.getHours();
    }

    @Override
    public CalendarDate setHours(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addHours(int i) {
        unsupported();
        return this;
    }

    @Override
    public int getMinutes() {
        return this.date.getMinutes();
    }

    @Override
    public CalendarDate setMinutes(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addMinutes(int i) {
        unsupported();
        return this;
    }

    @Override
    public int getSeconds() {
        return this.date.getSeconds();
    }

    @Override
    public CalendarDate setSeconds(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addSeconds(int i) {
        unsupported();
        return this;
    }

    @Override
    public int getMillis() {
        return this.date.getMillis();
    }

    @Override
    public CalendarDate setMillis(int i) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addMillis(int i) {
        unsupported();
        return this;
    }

    @Override
    public long getTimeOfDay() {
        return this.date.getTimeOfDay();
    }

    @Override
    public CalendarDate setDate(int i, int i2, int i3) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addDate(int i, int i2, int i3) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate setTimeOfDay(int i, int i2, int i3, int i4) {
        unsupported();
        return this;
    }

    @Override
    public CalendarDate addTimeOfDay(int i, int i2, int i3, int i4) {
        unsupported();
        return this;
    }

    @Override
    protected void setTimeOfDay(long j) {
        unsupported();
    }

    @Override
    public boolean isNormalized() {
        return this.date.isNormalized();
    }

    @Override
    public boolean isStandardTime() {
        return this.date.isStandardTime();
    }

    @Override
    public void setStandardTime(boolean z) {
        unsupported();
    }

    @Override
    public boolean isDaylightTime() {
        return this.date.isDaylightTime();
    }

    @Override
    protected void setLocale(Locale locale) {
        unsupported();
    }

    @Override
    public TimeZone getZone() {
        return this.date.getZone();
    }

    @Override
    public CalendarDate setZone(TimeZone timeZone) {
        unsupported();
        return this;
    }

    @Override
    public boolean isSameDate(CalendarDate calendarDate) {
        return calendarDate.isSameDate(calendarDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ImmutableGregorianDate)) {
            return false;
        }
        return this.date.equals(((ImmutableGregorianDate) obj).date);
    }

    @Override
    public int hashCode() {
        return this.date.hashCode();
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public String toString() {
        return this.date.toString();
    }

    @Override
    protected void setDayOfWeek(int i) {
        unsupported();
    }

    @Override
    protected void setNormalized(boolean z) {
        unsupported();
    }

    @Override
    public int getZoneOffset() {
        return this.date.getZoneOffset();
    }

    @Override
    protected void setZoneOffset(int i) {
        unsupported();
    }

    @Override
    public int getDaylightSaving() {
        return this.date.getDaylightSaving();
    }

    @Override
    protected void setDaylightSaving(int i) {
        unsupported();
    }

    @Override
    public int getNormalizedYear() {
        return this.date.getNormalizedYear();
    }

    @Override
    public void setNormalizedYear(int i) {
        unsupported();
    }

    private void unsupported() {
        throw new UnsupportedOperationException();
    }
}
