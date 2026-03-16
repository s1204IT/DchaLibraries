package java.text;

import java.text.Format;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.ICU;
import libcore.icu.LocaleData;

public abstract class DateFormat extends Format {
    public static final int AM_PM_FIELD = 14;
    public static final int DATE_FIELD = 3;
    public static final int DAY_OF_WEEK_FIELD = 9;
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public static final int DAY_OF_YEAR_FIELD = 10;
    public static final int DEFAULT = 2;
    public static final int ERA_FIELD = 0;
    public static final int FULL = 0;
    public static final int HOUR0_FIELD = 16;
    public static final int HOUR1_FIELD = 15;
    public static final int HOUR_OF_DAY0_FIELD = 5;
    public static final int HOUR_OF_DAY1_FIELD = 4;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int MILLISECOND_FIELD = 8;
    public static final int MINUTE_FIELD = 6;
    public static final int MONTH_FIELD = 2;
    public static final int SECOND_FIELD = 7;
    public static final int SHORT = 3;
    public static final int TIMEZONE_FIELD = 17;
    public static final int WEEK_OF_MONTH_FIELD = 13;
    public static final int WEEK_OF_YEAR_FIELD = 12;
    public static final int YEAR_FIELD = 1;
    public static Boolean is24Hour = null;
    private static final long serialVersionUID = 7218322306649953788L;
    protected Calendar calendar;
    protected NumberFormat numberFormat;

    public abstract StringBuffer format(Date date, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract Date parse(String str, ParsePosition parsePosition);

    protected DateFormat() {
    }

    @Override
    public Object clone() {
        DateFormat clone = (DateFormat) super.clone();
        clone.calendar = (Calendar) this.calendar.clone();
        clone.numberFormat = (NumberFormat) this.numberFormat.clone();
        return clone;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DateFormat)) {
            return false;
        }
        DateFormat dateFormat = (DateFormat) object;
        return this.numberFormat.equals(dateFormat.numberFormat) && this.calendar.getTimeZone().equals(dateFormat.calendar.getTimeZone()) && this.calendar.getFirstDayOfWeek() == dateFormat.calendar.getFirstDayOfWeek() && this.calendar.getMinimalDaysInFirstWeek() == dateFormat.calendar.getMinimalDaysInFirstWeek() && this.calendar.isLenient() == dateFormat.calendar.isLenient();
    }

    @Override
    public final StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
        if (object instanceof Date) {
            return format((Date) object, buffer, field);
        }
        if (object instanceof Number) {
            return format(new Date(((Number) object).longValue()), buffer, field);
        }
        throw new IllegalArgumentException("Bad class: " + object.getClass());
    }

    public final String format(Date date) {
        return format(date, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableDateFormatLocales();
    }

    public Calendar getCalendar() {
        return this.calendar;
    }

    public static final DateFormat getDateInstance() {
        return getDateInstance(2);
    }

    public static final DateFormat getDateInstance(int style) {
        checkDateStyle(style);
        return getDateInstance(style, Locale.getDefault());
    }

    public static final DateFormat getDateInstance(int style, Locale locale) {
        checkDateStyle(style);
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return new SimpleDateFormat(LocaleData.get(locale).getDateFormat(style), locale);
    }

    public static final DateFormat getDateTimeInstance() {
        return getDateTimeInstance(2, 2);
    }

    public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
        checkTimeStyle(timeStyle);
        checkDateStyle(dateStyle);
        return getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault());
    }

    public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale) {
        checkTimeStyle(timeStyle);
        checkDateStyle(dateStyle);
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        LocaleData localeData = LocaleData.get(locale);
        String pattern = localeData.getDateFormat(dateStyle) + " " + localeData.getTimeFormat(timeStyle);
        return new SimpleDateFormat(pattern, locale);
    }

    public static final DateFormat getInstance() {
        return getDateTimeInstance(3, 3);
    }

    public static final void set24HourTimePref(boolean is24Hour2) {
        is24Hour = Boolean.valueOf(is24Hour2);
    }

    public NumberFormat getNumberFormat() {
        return this.numberFormat;
    }

    public static final DateFormat getTimeInstance() {
        return getTimeInstance(2);
    }

    public static final DateFormat getTimeInstance(int style) {
        checkTimeStyle(style);
        return getTimeInstance(style, Locale.getDefault());
    }

    public static final DateFormat getTimeInstance(int style, Locale locale) {
        checkTimeStyle(style);
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return new SimpleDateFormat(LocaleData.get(locale).getTimeFormat(style), locale);
    }

    public TimeZone getTimeZone() {
        return this.calendar.getTimeZone();
    }

    public int hashCode() {
        return (this.calendar.isLenient() ? 1231 : 1237) + this.calendar.getTimeZone().hashCode() + this.calendar.getFirstDayOfWeek() + this.calendar.getMinimalDaysInFirstWeek() + this.numberFormat.hashCode();
    }

    public boolean isLenient() {
        return this.calendar.isLenient();
    }

    public Date parse(String string) throws ParseException {
        ParsePosition position = new ParsePosition(0);
        Date date = parse(string, position);
        if (position.getIndex() == 0) {
            throw new ParseException("Unparseable date: \"" + string + "\"", position.getErrorIndex());
        }
        return date;
    }

    @Override
    public Object parseObject(String string, ParsePosition position) {
        return parse(string, position);
    }

    public void setCalendar(Calendar cal) {
        this.calendar = cal;
    }

    public void setLenient(boolean value) {
        this.calendar.setLenient(value);
    }

    public void setNumberFormat(NumberFormat format) {
        this.numberFormat = format;
    }

    public void setTimeZone(TimeZone timezone) {
        this.calendar.setTimeZone(timezone);
    }

    public static class Field extends Format.Field {
        private static final long serialVersionUID = 7441350119349544720L;
        private int calendarField;
        private static Hashtable<Integer, Field> table = new Hashtable<>();
        public static final Field ERA = new Field("era", 0);
        public static final Field YEAR = new Field("year", 1);
        public static final Field MONTH = new Field("month", 2);
        public static final Field HOUR_OF_DAY0 = new Field("hour of day", 11);
        public static final Field HOUR_OF_DAY1 = new Field("hour of day 1", -1);
        public static final Field MINUTE = new Field("minute", 12);
        public static final Field SECOND = new Field("second", 13);
        public static final Field MILLISECOND = new Field("millisecond", 14);
        public static final Field DAY_OF_WEEK = new Field("day of week", 7);
        public static final Field DAY_OF_MONTH = new Field("day of month", 5);
        public static final Field DAY_OF_YEAR = new Field("day of year", 6);
        public static final Field DAY_OF_WEEK_IN_MONTH = new Field("day of week in month", 8);
        public static final Field WEEK_OF_YEAR = new Field("week of year", 3);
        public static final Field WEEK_OF_MONTH = new Field("week of month", 4);
        public static final Field AM_PM = new Field("am pm", 9);
        public static final Field HOUR0 = new Field("hour", 10);
        public static final Field HOUR1 = new Field("hour 1", -1);
        public static final Field TIME_ZONE = new Field("time zone", -1);

        protected Field(String fieldName, int calendarField) {
            super(fieldName);
            this.calendarField = -1;
            this.calendarField = calendarField;
            if (calendarField != -1 && table.get(Integer.valueOf(calendarField)) == null) {
                table.put(Integer.valueOf(calendarField), this);
            }
        }

        public int getCalendarField() {
            return this.calendarField;
        }

        public static Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >= 17) {
                throw new IllegalArgumentException("Field out of range: " + calendarField);
            }
            return table.get(Integer.valueOf(calendarField));
        }
    }

    private static void checkDateStyle(int style) {
        if (style != 3 && style != 2 && style != 1 && style != 0 && style != 2) {
            throw new IllegalArgumentException("Illegal date style: " + style);
        }
    }

    private static void checkTimeStyle(int style) {
        if (style != 3 && style != 2 && style != 1 && style != 0 && style != 2) {
            throw new IllegalArgumentException("Illegal time style: " + style);
        }
    }
}
