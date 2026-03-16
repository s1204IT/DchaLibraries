package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import libcore.icu.ICU;
import libcore.icu.LocaleData;

public abstract class Calendar implements Serializable, Cloneable, Comparable<Calendar> {
    public static final int ALL_STYLES = 0;
    public static final int AM = 0;
    public static final int AM_PM = 9;
    public static final int APRIL = 3;
    public static final int AUGUST = 7;
    public static final int DATE = 5;
    public static final int DAY_OF_MONTH = 5;
    public static final int DAY_OF_WEEK = 7;
    public static final int DAY_OF_WEEK_IN_MONTH = 8;
    public static final int DAY_OF_YEAR = 6;
    public static final int DECEMBER = 11;
    public static final int DST_OFFSET = 16;
    public static final int ERA = 0;
    public static final int FEBRUARY = 1;
    public static final int FIELD_COUNT = 17;
    public static final int FRIDAY = 6;
    public static final int HOUR = 10;
    public static final int HOUR_OF_DAY = 11;
    public static final int JANUARY = 0;
    public static final int JULY = 6;
    public static final int JUNE = 5;
    public static final int LONG = 2;
    public static final int MARCH = 2;
    public static final int MAY = 4;
    public static final int MILLISECOND = 14;
    public static final int MINUTE = 12;
    public static final int MONDAY = 2;
    public static final int MONTH = 2;
    public static final int NOVEMBER = 10;
    public static final int OCTOBER = 9;
    public static final int PM = 1;
    public static final int SATURDAY = 7;
    public static final int SECOND = 13;
    public static final int SEPTEMBER = 8;
    public static final int SHORT = 1;
    public static final int SUNDAY = 1;
    public static final int THURSDAY = 5;
    public static final int TUESDAY = 3;
    public static final int UNDECIMBER = 12;
    public static final int WEDNESDAY = 4;
    public static final int WEEK_OF_MONTH = 4;
    public static final int WEEK_OF_YEAR = 3;
    public static final int YEAR = 1;
    public static final int ZONE_OFFSET = 15;
    private static final long serialVersionUID = -1807547505821590642L;
    protected boolean areFieldsSet;
    protected int[] fields;
    private int firstDayOfWeek;
    protected boolean[] isSet;
    protected boolean isTimeSet;
    transient int lastDateFieldSet;
    transient int lastTimeFieldSet;
    private boolean lenient;
    private int minimalDaysInFirstWeek;
    protected long time;
    private TimeZone zone;
    private static final String[] FIELD_NAMES = {"ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH", "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR", "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET", "DST_OFFSET"};
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("areFieldsSet", Boolean.TYPE), new ObjectStreamField("fields", (Class<?>) int[].class), new ObjectStreamField("firstDayOfWeek", Integer.TYPE), new ObjectStreamField("isSet", (Class<?>) boolean[].class), new ObjectStreamField("isTimeSet", Boolean.TYPE), new ObjectStreamField("lenient", Boolean.TYPE), new ObjectStreamField("minimalDaysInFirstWeek", Integer.TYPE), new ObjectStreamField("nextStamp", Integer.TYPE), new ObjectStreamField("serialVersionOnStream", Integer.TYPE), new ObjectStreamField("time", Long.TYPE), new ObjectStreamField("zone", (Class<?>) TimeZone.class)};

    public abstract void add(int i, int i2);

    protected abstract void computeFields();

    protected abstract void computeTime();

    public abstract int getGreatestMinimum(int i);

    public abstract int getLeastMaximum(int i);

    public abstract int getMaximum(int i);

    public abstract int getMinimum(int i);

    public abstract void roll(int i, boolean z);

    protected Calendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    Calendar(TimeZone timezone) {
        this.fields = new int[17];
        this.isSet = new boolean[17];
        this.isTimeSet = false;
        this.areFieldsSet = false;
        setLenient(true);
        setTimeZone(timezone);
    }

    protected Calendar(TimeZone timezone, Locale locale) {
        this(timezone);
        LocaleData localeData = LocaleData.get(LocaleData.mapInvalidAndNullLocales(locale));
        setFirstDayOfWeek(localeData.firstDayOfWeek.intValue());
        setMinimalDaysInFirstWeek(localeData.minimalDaysInFirstWeek.intValue());
    }

    public boolean after(Object calendar) {
        return (calendar instanceof Calendar) && getTimeInMillis() > ((Calendar) calendar).getTimeInMillis();
    }

    public boolean before(Object calendar) {
        return (calendar instanceof Calendar) && getTimeInMillis() < ((Calendar) calendar).getTimeInMillis();
    }

    public final void clear() {
        for (int i = 0; i < 17; i++) {
            this.fields[i] = 0;
            this.isSet[i] = false;
        }
        this.isTimeSet = false;
        this.areFieldsSet = false;
    }

    public final void clear(int field) {
        this.fields[field] = 0;
        this.isSet[field] = false;
        this.isTimeSet = false;
        this.areFieldsSet = false;
    }

    public Object clone() {
        try {
            Calendar clone = (Calendar) super.clone();
            clone.fields = (int[]) this.fields.clone();
            clone.isSet = (boolean[]) this.isSet.clone();
            clone.zone = (TimeZone) this.zone.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    protected void complete() {
        if (!this.isTimeSet) {
            computeTime();
            this.isTimeSet = true;
        }
        if (!this.areFieldsSet) {
            computeFields();
            this.areFieldsSet = true;
        }
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Calendar)) {
            return false;
        }
        Calendar cal = (Calendar) object;
        return getTimeInMillis() == cal.getTimeInMillis() && isLenient() == cal.isLenient() && getFirstDayOfWeek() == cal.getFirstDayOfWeek() && getMinimalDaysInFirstWeek() == cal.getMinimalDaysInFirstWeek() && getTimeZone().equals(cal.getTimeZone());
    }

    public int get(int field) {
        complete();
        return this.fields[field];
    }

    public int getActualMaximum(int field) {
        int value;
        int maximum = getMaximum(field);
        int next = getLeastMaximum(field);
        if (maximum == next) {
            return next;
        }
        complete();
        long orgTime = this.time;
        set(field, next);
        do {
            value = next;
            roll(field, true);
            next = get(field);
        } while (next > value);
        this.time = orgTime;
        this.areFieldsSet = false;
        return value;
    }

    public int getActualMinimum(int field) {
        int value;
        int minimum = getMinimum(field);
        int next = getGreatestMinimum(field);
        if (minimum == next) {
            return next;
        }
        complete();
        long orgTime = this.time;
        set(field, next);
        do {
            value = next;
            roll(field, false);
            next = get(field);
        } while (next < value);
        this.time = orgTime;
        this.areFieldsSet = false;
        return value;
    }

    public static synchronized Locale[] getAvailableLocales() {
        return ICU.getAvailableCalendarLocales();
    }

    public int getFirstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    public static synchronized Calendar getInstance() {
        return new GregorianCalendar();
    }

    public static synchronized Calendar getInstance(Locale locale) {
        return new GregorianCalendar(locale);
    }

    public static synchronized Calendar getInstance(TimeZone timezone) {
        return new GregorianCalendar(timezone);
    }

    public static synchronized Calendar getInstance(TimeZone timezone, Locale locale) {
        return new GregorianCalendar(timezone, locale);
    }

    public int getMinimalDaysInFirstWeek() {
        return this.minimalDaysInFirstWeek;
    }

    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    public long getTimeInMillis() {
        if (!this.isTimeSet) {
            computeTime();
            this.isTimeSet = true;
        }
        return this.time;
    }

    public TimeZone getTimeZone() {
        return this.zone;
    }

    public int hashCode() {
        return (isLenient() ? 1237 : 1231) + getFirstDayOfWeek() + getMinimalDaysInFirstWeek() + getTimeZone().hashCode();
    }

    protected final int internalGet(int field) {
        return this.fields[field];
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public final boolean isSet(int field) {
        return this.isSet[field];
    }

    public void roll(int field, int value) {
        boolean increment = value >= 0;
        int count = increment ? value : -value;
        for (int i = 0; i < count; i++) {
            roll(field, increment);
        }
    }

    public void set(int field, int value) {
        this.fields[field] = value;
        this.isSet[field] = true;
        this.isTimeSet = false;
        this.areFieldsSet = false;
        if (field > 2 && field < 9) {
            this.lastDateFieldSet = field;
        }
        if (field == 10 || field == 11) {
            this.lastTimeFieldSet = field;
        }
        if (field == 9) {
            this.lastTimeFieldSet = 10;
        }
    }

    public final void set(int year, int month, int day) {
        set(1, year);
        set(2, month);
        set(5, day);
    }

    public final void set(int year, int month, int day, int hourOfDay, int minute) {
        set(year, month, day);
        set(11, hourOfDay);
        set(12, minute);
    }

    public final void set(int year, int month, int day, int hourOfDay, int minute, int second) {
        set(year, month, day, hourOfDay, minute);
        set(13, second);
    }

    public void setFirstDayOfWeek(int value) {
        this.firstDayOfWeek = value;
    }

    public void setLenient(boolean value) {
        this.lenient = value;
    }

    public void setMinimalDaysInFirstWeek(int value) {
        this.minimalDaysInFirstWeek = value;
    }

    public final void setTime(Date date) {
        setTimeInMillis(date.getTime());
    }

    public void setTimeInMillis(long milliseconds) {
        if (!this.isTimeSet || !this.areFieldsSet || this.time != milliseconds) {
            this.time = milliseconds;
            this.isTimeSet = true;
            this.areFieldsSet = false;
            complete();
        }
    }

    public void setTimeZone(TimeZone timezone) {
        this.zone = timezone;
        this.areFieldsSet = false;
    }

    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getName() + "[time=" + (this.isTimeSet ? String.valueOf(this.time) : "?") + ",areFieldsSet=" + this.areFieldsSet + ",lenient=" + this.lenient + ",zone=" + this.zone.getID() + ",firstDayOfWeek=" + this.firstDayOfWeek + ",minimalDaysInFirstWeek=" + this.minimalDaysInFirstWeek);
        for (int i = 0; i < 17; i++) {
            result.append(',');
            result.append(FIELD_NAMES[i]);
            result.append('=');
            if (this.isSet[i]) {
                result.append(this.fields[i]);
            } else {
                result.append('?');
            }
        }
        result.append(']');
        return result.toString();
    }

    @Override
    public int compareTo(Calendar anotherCalendar) {
        if (anotherCalendar == null) {
            throw new NullPointerException("anotherCalendar == null");
        }
        long timeInMillis = getTimeInMillis();
        long anotherTimeInMillis = anotherCalendar.getTimeInMillis();
        if (timeInMillis > anotherTimeInMillis) {
            return 1;
        }
        if (timeInMillis == anotherTimeInMillis) {
            return 0;
        }
        return -1;
    }

    public String getDisplayName(int field, int style, Locale locale) {
        if (style == 0) {
            style = 1;
        }
        String[] array = getDisplayNameArray(field, style, locale);
        int value = get(field);
        if (array != null) {
            return array[value];
        }
        return null;
    }

    private String[] getDisplayNameArray(int field, int style, Locale locale) {
        if (field < 0 || field >= 17) {
            throw new IllegalArgumentException("bad field " + field);
        }
        checkStyle(style);
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);
        switch (field) {
            case 0:
                return dfs.getEras();
            case 2:
                return style == 2 ? dfs.getMonths() : dfs.getShortMonths();
            case 7:
                return style == 2 ? dfs.getWeekdays() : dfs.getShortWeekdays();
            case 9:
                return dfs.getAmPmStrings();
            default:
                return null;
        }
    }

    private static void checkStyle(int style) {
        if (style != 0 && style != 1 && style != 2) {
            throw new IllegalArgumentException("bad style " + style);
        }
    }

    public Map<String, Integer> getDisplayNames(int field, int style, Locale locale) {
        checkStyle(style);
        complete();
        Map<String, Integer> result = new HashMap<>();
        if (style == 1 || style == 0) {
            insertValuesInMap(result, getDisplayNameArray(field, 1, locale));
        }
        if (style == 2 || style == 0) {
            insertValuesInMap(result, getDisplayNameArray(field, 2, locale));
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static void insertValuesInMap(Map<String, Integer> map, String[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null && !values[i].isEmpty()) {
                    map.put(values[i], Integer.valueOf(i));
                }
            }
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        complete();
        ObjectOutputStream.PutField putFields = stream.putFields();
        putFields.put("areFieldsSet", this.areFieldsSet);
        putFields.put("fields", this.fields);
        putFields.put("firstDayOfWeek", this.firstDayOfWeek);
        putFields.put("isSet", this.isSet);
        putFields.put("isTimeSet", this.isTimeSet);
        putFields.put("lenient", this.lenient);
        putFields.put("minimalDaysInFirstWeek", this.minimalDaysInFirstWeek);
        putFields.put("nextStamp", 2);
        putFields.put("serialVersionOnStream", 1);
        putFields.put("time", this.time);
        putFields.put("zone", this.zone);
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField readFields = stream.readFields();
        this.areFieldsSet = readFields.get("areFieldsSet", false);
        this.fields = (int[]) readFields.get("fields", (Object) null);
        this.firstDayOfWeek = readFields.get("firstDayOfWeek", 1);
        this.isSet = (boolean[]) readFields.get("isSet", (Object) null);
        this.isTimeSet = readFields.get("isTimeSet", false);
        this.lenient = readFields.get("lenient", true);
        this.minimalDaysInFirstWeek = readFields.get("minimalDaysInFirstWeek", 1);
        this.time = readFields.get("time", 0L);
        this.zone = (TimeZone) readFields.get("zone", (Object) null);
    }
}
