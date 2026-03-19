package android.icu.text;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.RelativeDateFormat;
import android.icu.text.DisplayContext;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public abstract class DateFormat extends UFormat {

    @Deprecated
    public static final String ABBR_STANDALONE_MONTH = "LLL";
    public static final int AM_PM_FIELD = 14;
    public static final int AM_PM_MIDNIGHT_NOON_FIELD = 35;
    public static final int DATE_FIELD = 3;
    public static final int DAY_OF_WEEK_FIELD = 9;
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public static final int DAY_OF_YEAR_FIELD = 10;
    public static final int DEFAULT = 2;
    public static final int DOW_LOCAL_FIELD = 19;
    public static final int ERA_FIELD = 0;
    public static final int EXTENDED_YEAR_FIELD = 20;

    @Deprecated
    public static final int FIELD_COUNT = 38;
    public static final int FLEXIBLE_DAY_PERIOD_FIELD = 36;
    public static final int FRACTIONAL_SECOND_FIELD = 8;
    public static final int FULL = 0;
    public static final int HOUR0_FIELD = 16;
    public static final int HOUR1_FIELD = 15;

    @Deprecated
    public static final String HOUR_GENERIC_TZ = "jv";

    @Deprecated
    public static final String HOUR_MINUTE_GENERIC_TZ = "jmv";

    @Deprecated
    public static final String HOUR_MINUTE_TZ = "jmz";
    public static final int HOUR_OF_DAY0_FIELD = 5;
    public static final int HOUR_OF_DAY1_FIELD = 4;

    @Deprecated
    public static final String HOUR_TZ = "jz";
    public static final int JULIAN_DAY_FIELD = 21;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int MILLISECONDS_IN_DAY_FIELD = 22;
    public static final int MILLISECOND_FIELD = 8;
    public static final int MINUTE_FIELD = 6;
    public static final int MONTH_FIELD = 2;
    public static final int NONE = -1;
    public static final int QUARTER_FIELD = 27;

    @Deprecated
    static final int RELATED_YEAR = 34;
    public static final int RELATIVE = 128;
    public static final int RELATIVE_DEFAULT = 130;
    public static final int RELATIVE_FULL = 128;
    public static final int RELATIVE_LONG = 129;
    public static final int RELATIVE_MEDIUM = 130;
    public static final int RELATIVE_SHORT = 131;
    public static final int SECOND_FIELD = 7;
    public static final int SHORT = 3;
    public static final int STANDALONE_DAY_FIELD = 25;

    @Deprecated
    public static final String STANDALONE_MONTH = "LLLL";
    public static final int STANDALONE_MONTH_FIELD = 26;
    public static final int STANDALONE_QUARTER_FIELD = 28;
    public static final int TIMEZONE_FIELD = 17;
    public static final int TIMEZONE_GENERIC_FIELD = 24;
    public static final int TIMEZONE_ISO_FIELD = 32;
    public static final int TIMEZONE_ISO_LOCAL_FIELD = 33;
    public static final int TIMEZONE_LOCALIZED_GMT_OFFSET_FIELD = 31;
    public static final int TIMEZONE_RFC_FIELD = 23;
    public static final int TIMEZONE_SPECIAL_FIELD = 29;

    @Deprecated
    public static final int TIME_SEPARATOR = 37;
    public static final int WEEK_OF_MONTH_FIELD = 13;
    public static final int WEEK_OF_YEAR_FIELD = 12;
    public static final int YEAR_FIELD = 1;
    public static final int YEAR_NAME_FIELD = 30;
    public static final int YEAR_WOY_FIELD = 18;
    static final int currentSerialVersion = 1;
    private static final long serialVersionUID = 7218322306649953788L;
    protected Calendar calendar;
    protected NumberFormat numberFormat;
    public static final String YEAR = "y";
    public static final String QUARTER = "QQQQ";
    public static final String ABBR_QUARTER = "QQQ";
    public static final String YEAR_QUARTER = "yQQQQ";
    public static final String YEAR_ABBR_QUARTER = "yQQQ";
    public static final String MONTH = "MMMM";
    public static final String ABBR_MONTH = "MMM";
    public static final String NUM_MONTH = "M";
    public static final String YEAR_MONTH = "yMMMM";
    public static final String YEAR_ABBR_MONTH = "yMMM";
    public static final String YEAR_NUM_MONTH = "yM";
    public static final String DAY = "d";
    public static final String YEAR_MONTH_DAY = "yMMMMd";
    public static final String YEAR_ABBR_MONTH_DAY = "yMMMd";
    public static final String YEAR_NUM_MONTH_DAY = "yMd";
    public static final String WEEKDAY = "EEEE";
    public static final String ABBR_WEEKDAY = "E";
    public static final String YEAR_MONTH_WEEKDAY_DAY = "yMMMMEEEEd";
    public static final String YEAR_ABBR_MONTH_WEEKDAY_DAY = "yMMMEd";
    public static final String YEAR_NUM_MONTH_WEEKDAY_DAY = "yMEd";
    public static final String MONTH_DAY = "MMMMd";
    public static final String ABBR_MONTH_DAY = "MMMd";
    public static final String NUM_MONTH_DAY = "Md";
    public static final String MONTH_WEEKDAY_DAY = "MMMMEEEEd";
    public static final String ABBR_MONTH_WEEKDAY_DAY = "MMMEd";
    public static final String NUM_MONTH_WEEKDAY_DAY = "MEd";

    @Deprecated
    public static final List<String> DATE_SKELETONS = Arrays.asList(YEAR, QUARTER, ABBR_QUARTER, YEAR_QUARTER, YEAR_ABBR_QUARTER, MONTH, ABBR_MONTH, NUM_MONTH, YEAR_MONTH, YEAR_ABBR_MONTH, YEAR_NUM_MONTH, DAY, YEAR_MONTH_DAY, YEAR_ABBR_MONTH_DAY, YEAR_NUM_MONTH_DAY, WEEKDAY, ABBR_WEEKDAY, YEAR_MONTH_WEEKDAY_DAY, YEAR_ABBR_MONTH_WEEKDAY_DAY, YEAR_NUM_MONTH_WEEKDAY_DAY, MONTH_DAY, ABBR_MONTH_DAY, NUM_MONTH_DAY, MONTH_WEEKDAY_DAY, ABBR_MONTH_WEEKDAY_DAY, NUM_MONTH_WEEKDAY_DAY);
    public static final String HOUR = "j";
    public static final String HOUR24 = "H";
    public static final String MINUTE = "m";
    public static final String HOUR_MINUTE = "jm";
    public static final String HOUR24_MINUTE = "Hm";
    public static final String SECOND = "s";
    public static final String HOUR_MINUTE_SECOND = "jms";
    public static final String HOUR24_MINUTE_SECOND = "Hms";
    public static final String MINUTE_SECOND = "ms";

    @Deprecated
    public static final List<String> TIME_SKELETONS = Arrays.asList(HOUR, HOUR24, MINUTE, HOUR_MINUTE, HOUR24_MINUTE, SECOND, HOUR_MINUTE_SECOND, HOUR24_MINUTE_SECOND, MINUTE_SECOND);
    public static final String LOCATION_TZ = "VVVV";
    public static final String GENERIC_TZ = "vvvv";
    public static final String ABBR_GENERIC_TZ = "v";
    public static final String SPECIFIC_TZ = "zzzz";
    public static final String ABBR_SPECIFIC_TZ = "z";
    public static final String ABBR_UTC_TZ = "ZZZZ";

    @Deprecated
    public static final List<String> ZONE_SKELETONS = Arrays.asList(LOCATION_TZ, GENERIC_TZ, ABBR_GENERIC_TZ, SPECIFIC_TZ, ABBR_SPECIFIC_TZ, ABBR_UTC_TZ);
    private EnumSet<BooleanAttribute> booleanAttributes = EnumSet.allOf(BooleanAttribute.class);
    private DisplayContext capitalizationSetting = DisplayContext.CAPITALIZATION_NONE;
    private int serialVersionOnStream = 1;

    public enum BooleanAttribute {
        PARSE_ALLOW_WHITESPACE,
        PARSE_ALLOW_NUMERIC,
        PARSE_MULTIPLE_PATTERNS_FOR_MATCH,
        PARSE_PARTIAL_LITERAL_MATCH,
        PARSE_PARTIAL_MATCH
    }

    public abstract StringBuffer format(Calendar calendar, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract void parse(String str, Calendar calendar, ParsePosition parsePosition);

    @Override
    public final StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (obj instanceof Calendar) {
            return format((Calendar) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof Date) {
            return format((Date) obj, stringBuffer, fieldPosition);
        }
        if (obj instanceof Number) {
            return format(new Date(((Number) obj).longValue()), stringBuffer, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a Date");
    }

    public StringBuffer format(Date date, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        this.calendar.setTime(date);
        return format(this.calendar, stringBuffer, fieldPosition);
    }

    public final String format(Date date) {
        return format(date, new StringBuffer(64), new FieldPosition(0)).toString();
    }

    public Date parse(String str) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Date date = parse(str, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable date: \"" + str + "\"", parsePosition.getErrorIndex());
        }
        return date;
    }

    public Date parse(String str, ParsePosition parsePosition) {
        Date time;
        int index = parsePosition.getIndex();
        TimeZone timeZone = this.calendar.getTimeZone();
        this.calendar.clear();
        parse(str, this.calendar, parsePosition);
        if (parsePosition.getIndex() != index) {
            try {
                time = this.calendar.getTime();
            } catch (IllegalArgumentException e) {
                parsePosition.setIndex(index);
                parsePosition.setErrorIndex(index);
                time = null;
            }
        } else {
            time = null;
        }
        this.calendar.setTimeZone(timeZone);
        return time;
    }

    @Override
    public Object parseObject(String str, ParsePosition parsePosition) {
        return parse(str, parsePosition);
    }

    public static final DateFormat getTimeInstance() {
        return get(-1, 2, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public static final DateFormat getTimeInstance(int i) {
        return get(-1, i, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public static final DateFormat getTimeInstance(int i, Locale locale) {
        return get(-1, i, ULocale.forLocale(locale), null);
    }

    public static final DateFormat getTimeInstance(int i, ULocale uLocale) {
        return get(-1, i, uLocale, null);
    }

    public static final DateFormat getDateInstance() {
        return get(2, -1, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public static final DateFormat getDateInstance(int i) {
        return get(i, -1, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public static final DateFormat getDateInstance(int i, Locale locale) {
        return get(i, -1, ULocale.forLocale(locale), null);
    }

    public static final DateFormat getDateInstance(int i, ULocale uLocale) {
        return get(i, -1, uLocale, null);
    }

    public static final DateFormat getDateTimeInstance() {
        return get(2, 2, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public static final DateFormat getDateTimeInstance(int i, int i2) {
        return get(i, i2, ULocale.getDefault(ULocale.Category.FORMAT), null);
    }

    public static final DateFormat getDateTimeInstance(int i, int i2, Locale locale) {
        return get(i, i2, ULocale.forLocale(locale), null);
    }

    public static final DateFormat getDateTimeInstance(int i, int i2, ULocale uLocale) {
        return get(i, i2, uLocale, null);
    }

    public static final DateFormat getInstance() {
        return getDateTimeInstance(3, 3);
    }

    public static Locale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public Calendar getCalendar() {
        return this.calendar;
    }

    public void setNumberFormat(NumberFormat numberFormat) {
        this.numberFormat = (NumberFormat) numberFormat.clone();
        fixNumberFormatForDates(this.numberFormat);
    }

    static void fixNumberFormatForDates(NumberFormat numberFormat) {
        numberFormat.setGroupingUsed(false);
        if (numberFormat instanceof DecimalFormat) {
            ((DecimalFormat) numberFormat).setDecimalSeparatorAlwaysShown(false);
        }
        numberFormat.setParseIntegerOnly(true);
        numberFormat.setMinimumFractionDigits(0);
    }

    public NumberFormat getNumberFormat() {
        return this.numberFormat;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.calendar.setTimeZone(timeZone);
    }

    public TimeZone getTimeZone() {
        return this.calendar.getTimeZone();
    }

    public void setLenient(boolean z) {
        this.calendar.setLenient(z);
        setBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC, z);
        setBooleanAttribute(BooleanAttribute.PARSE_ALLOW_WHITESPACE, z);
    }

    public boolean isLenient() {
        return this.calendar.isLenient() && getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC) && getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_WHITESPACE);
    }

    public void setCalendarLenient(boolean z) {
        this.calendar.setLenient(z);
    }

    public boolean isCalendarLenient() {
        return this.calendar.isLenient();
    }

    public DateFormat setBooleanAttribute(BooleanAttribute booleanAttribute, boolean z) {
        if (booleanAttribute.equals(BooleanAttribute.PARSE_PARTIAL_MATCH)) {
            booleanAttribute = BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH;
        }
        if (z) {
            this.booleanAttributes.add(booleanAttribute);
        } else {
            this.booleanAttributes.remove(booleanAttribute);
        }
        return this;
    }

    public boolean getBooleanAttribute(BooleanAttribute booleanAttribute) {
        if (booleanAttribute == BooleanAttribute.PARSE_PARTIAL_MATCH) {
            booleanAttribute = BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH;
        }
        return this.booleanAttributes.contains(booleanAttribute);
    }

    public void setContext(DisplayContext displayContext) {
        if (displayContext.type() == DisplayContext.Type.CAPITALIZATION) {
            this.capitalizationSetting = displayContext;
        }
    }

    public DisplayContext getContext(DisplayContext.Type type) {
        return (type != DisplayContext.Type.CAPITALIZATION || this.capitalizationSetting == null) ? DisplayContext.CAPITALIZATION_NONE : this.capitalizationSetting;
    }

    public int hashCode() {
        return this.numberFormat.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DateFormat dateFormat = (DateFormat) obj;
        if (((this.calendar == null && dateFormat.calendar == null) || (this.calendar != null && dateFormat.calendar != null && this.calendar.isEquivalentTo(dateFormat.calendar))) && (((this.numberFormat == null && dateFormat.numberFormat == null) || (this.numberFormat != null && dateFormat.numberFormat != null && this.numberFormat.equals(dateFormat.numberFormat))) && this.capitalizationSetting == dateFormat.capitalizationSetting)) {
            return true;
        }
        return false;
    }

    @Override
    public Object clone() {
        DateFormat dateFormat = (DateFormat) super.clone();
        dateFormat.calendar = (Calendar) this.calendar.clone();
        if (this.numberFormat != null) {
            dateFormat.numberFormat = (NumberFormat) this.numberFormat.clone();
        }
        return dateFormat;
    }

    private static DateFormat get(int i, int i2, ULocale uLocale, Calendar calendar) {
        if ((i2 != -1 && (i2 & 128) > 0) || (i != -1 && (i & 128) > 0)) {
            return new RelativeDateFormat(i2, i, uLocale, calendar);
        }
        if (i2 < -1 || i2 > 3) {
            throw new IllegalArgumentException("Illegal time style " + i2);
        }
        if (i < -1 || i > 3) {
            throw new IllegalArgumentException("Illegal date style " + i);
        }
        if (calendar == null) {
            calendar = Calendar.getInstance(uLocale);
        }
        try {
            DateFormat dateTimeFormat = calendar.getDateTimeFormat(i, i2, uLocale);
            dateTimeFormat.setLocale(calendar.getLocale(ULocale.VALID_LOCALE), calendar.getLocale(ULocale.ACTUAL_LOCALE));
            return dateTimeFormat;
        } catch (MissingResourceException e) {
            return new SimpleDateFormat("M/d/yy h:mm a");
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            this.capitalizationSetting = DisplayContext.CAPITALIZATION_NONE;
        }
        if (this.booleanAttributes == null) {
            this.booleanAttributes = EnumSet.allOf(BooleanAttribute.class);
        }
        this.serialVersionOnStream = 1;
    }

    protected DateFormat() {
    }

    public static final DateFormat getDateInstance(Calendar calendar, int i, Locale locale) {
        return getDateTimeInstance(calendar, i, -1, ULocale.forLocale(locale));
    }

    public static final DateFormat getDateInstance(Calendar calendar, int i, ULocale uLocale) {
        return getDateTimeInstance(calendar, i, -1, uLocale);
    }

    public static final DateFormat getTimeInstance(Calendar calendar, int i, Locale locale) {
        return getDateTimeInstance(calendar, -1, i, ULocale.forLocale(locale));
    }

    public static final DateFormat getTimeInstance(Calendar calendar, int i, ULocale uLocale) {
        return getDateTimeInstance(calendar, -1, i, uLocale);
    }

    public static final DateFormat getDateTimeInstance(Calendar calendar, int i, int i2, Locale locale) {
        return getDateTimeInstance(calendar, i, i2, ULocale.forLocale(locale));
    }

    public static final DateFormat getDateTimeInstance(Calendar calendar, int i, int i2, ULocale uLocale) {
        if (calendar == null) {
            throw new IllegalArgumentException("Calendar must be supplied");
        }
        return get(i, i2, uLocale, calendar);
    }

    public static final DateFormat getInstance(Calendar calendar, Locale locale) {
        return getDateTimeInstance(calendar, 3, 3, ULocale.forLocale(locale));
    }

    public static final DateFormat getInstance(Calendar calendar, ULocale uLocale) {
        return getDateTimeInstance(calendar, 3, 3, uLocale);
    }

    public static final DateFormat getInstance(Calendar calendar) {
        return getInstance(calendar, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateFormat getDateInstance(Calendar calendar, int i) {
        return getDateInstance(calendar, i, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateFormat getTimeInstance(Calendar calendar, int i) {
        return getTimeInstance(calendar, i, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateFormat getDateTimeInstance(Calendar calendar, int i, int i2) {
        return getDateTimeInstance(calendar, i, i2, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateFormat getInstanceForSkeleton(String str) {
        return getPatternInstance(str, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static final DateFormat getInstanceForSkeleton(String str, Locale locale) {
        return getPatternInstance(str, ULocale.forLocale(locale));
    }

    public static final DateFormat getInstanceForSkeleton(String str, ULocale uLocale) {
        return new SimpleDateFormat(DateTimePatternGenerator.getInstance(uLocale).getBestPattern(str), uLocale);
    }

    public static final DateFormat getInstanceForSkeleton(Calendar calendar, String str, Locale locale) {
        return getPatternInstance(calendar, str, ULocale.forLocale(locale));
    }

    public static final DateFormat getInstanceForSkeleton(Calendar calendar, String str, ULocale uLocale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateTimePatternGenerator.getInstance(uLocale).getBestPattern(str), uLocale);
        simpleDateFormat.setCalendar(calendar);
        return simpleDateFormat;
    }

    public static final DateFormat getPatternInstance(String str) {
        return getInstanceForSkeleton(str);
    }

    public static final DateFormat getPatternInstance(String str, Locale locale) {
        return getInstanceForSkeleton(str, locale);
    }

    public static final DateFormat getPatternInstance(String str, ULocale uLocale) {
        return getInstanceForSkeleton(str, uLocale);
    }

    public static final DateFormat getPatternInstance(Calendar calendar, String str, Locale locale) {
        return getInstanceForSkeleton(calendar, str, locale);
    }

    public static final DateFormat getPatternInstance(Calendar calendar, String str, ULocale uLocale) {
        return getInstanceForSkeleton(calendar, str, uLocale);
    }

    public static class Field extends Format.Field {
        private static final long serialVersionUID = -3627456821000730829L;
        private final int calendarField;
        private static final int CAL_FIELD_COUNT = new GregorianCalendar().getFieldCount();
        private static final Field[] CAL_FIELDS = new Field[CAL_FIELD_COUNT];
        private static final Map<String, Field> FIELD_NAME_MAP = new HashMap(CAL_FIELD_COUNT);
        public static final Field AM_PM = new Field("am pm", 9);
        public static final Field DAY_OF_MONTH = new Field("day of month", 5);
        public static final Field DAY_OF_WEEK = new Field("day of week", 7);
        public static final Field DAY_OF_WEEK_IN_MONTH = new Field("day of week in month", 8);
        public static final Field DAY_OF_YEAR = new Field("day of year", 6);
        public static final Field ERA = new Field("era", 0);
        public static final Field HOUR_OF_DAY0 = new Field("hour of day", 11);
        public static final Field HOUR_OF_DAY1 = new Field("hour of day 1", -1);
        public static final Field HOUR0 = new Field("hour", 10);
        public static final Field HOUR1 = new Field("hour 1", -1);
        public static final Field MILLISECOND = new Field("millisecond", 14);
        public static final Field MINUTE = new Field("minute", 12);
        public static final Field MONTH = new Field("month", 2);
        public static final Field SECOND = new Field("second", 13);
        public static final Field TIME_ZONE = new Field("time zone", -1);
        public static final Field WEEK_OF_MONTH = new Field("week of month", 4);
        public static final Field WEEK_OF_YEAR = new Field("week of year", 3);
        public static final Field YEAR = new Field("year", 1);
        public static final Field DOW_LOCAL = new Field("local day of week", 18);
        public static final Field EXTENDED_YEAR = new Field("extended year", 19);
        public static final Field JULIAN_DAY = new Field("Julian day", 20);
        public static final Field MILLISECONDS_IN_DAY = new Field("milliseconds in day", 21);
        public static final Field YEAR_WOY = new Field("year for week of year", 17);
        public static final Field QUARTER = new Field("quarter", -1);

        @Deprecated
        public static final Field RELATED_YEAR = new Field("related year", -1);
        public static final Field AM_PM_MIDNIGHT_NOON = new Field("am/pm/midnight/noon", -1);
        public static final Field FLEXIBLE_DAY_PERIOD = new Field("flexible day period", -1);

        @Deprecated
        public static final Field TIME_SEPARATOR = new Field("time separator", -1);

        protected Field(String str, int i) {
            super(str);
            this.calendarField = i;
            if (getClass() == Field.class) {
                FIELD_NAME_MAP.put(str, this);
                if (i >= 0 && i < CAL_FIELD_COUNT) {
                    CAL_FIELDS[i] = this;
                }
            }
        }

        public static Field ofCalendarField(int i) {
            if (i < 0 || i >= CAL_FIELD_COUNT) {
                throw new IllegalArgumentException("Calendar field number is out of range");
            }
            return CAL_FIELDS[i];
        }

        public int getCalendarField() {
            return this.calendarField;
        }

        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (getClass() != Field.class) {
                throw new InvalidObjectException("A subclass of DateFormat.Field must implement readResolve.");
            }
            Field field = FIELD_NAME_MAP.get(getName());
            if (field == null) {
                throw new InvalidObjectException("Unknown attribute name.");
            }
            return field;
        }
    }
}
