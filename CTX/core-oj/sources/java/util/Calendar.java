package java.util;

import java.awt.font.NumericShaper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import libcore.icu.LocaleData;
import sun.util.locale.provider.CalendarDataUtility;

public abstract class Calendar implements Serializable, Cloneable, Comparable<Calendar> {
    static final boolean $assertionsDisabled = false;
    static final int ALL_FIELDS = 131071;
    public static final int ALL_STYLES = 0;
    public static final int AM = 0;
    public static final int AM_PM = 9;
    static final int AM_PM_MASK = 512;
    public static final int APRIL = 3;
    public static final int AUGUST = 7;
    private static final int COMPUTED = 1;
    public static final int DATE = 5;
    static final int DATE_MASK = 32;
    public static final int DAY_OF_MONTH = 5;
    static final int DAY_OF_MONTH_MASK = 32;
    public static final int DAY_OF_WEEK = 7;
    public static final int DAY_OF_WEEK_IN_MONTH = 8;
    static final int DAY_OF_WEEK_IN_MONTH_MASK = 256;
    static final int DAY_OF_WEEK_MASK = 128;
    public static final int DAY_OF_YEAR = 6;
    static final int DAY_OF_YEAR_MASK = 64;
    public static final int DECEMBER = 11;
    public static final int DST_OFFSET = 16;
    static final int DST_OFFSET_MASK = 65536;
    public static final int ERA = 0;
    static final int ERA_MASK = 1;
    public static final int FEBRUARY = 1;
    public static final int FIELD_COUNT = 17;
    public static final int FRIDAY = 6;
    public static final int HOUR = 10;
    static final int HOUR_MASK = 1024;
    public static final int HOUR_OF_DAY = 11;
    static final int HOUR_OF_DAY_MASK = 2048;
    public static final int JANUARY = 0;
    public static final int JULY = 6;
    public static final int JUNE = 5;
    public static final int LONG = 2;
    public static final int LONG_FORMAT = 2;
    public static final int LONG_STANDALONE = 32770;
    public static final int MARCH = 2;
    public static final int MAY = 4;
    public static final int MILLISECOND = 14;
    static final int MILLISECOND_MASK = 16384;
    private static final int MINIMUM_USER_STAMP = 2;
    public static final int MINUTE = 12;
    static final int MINUTE_MASK = 4096;
    public static final int MONDAY = 2;
    public static final int MONTH = 2;
    static final int MONTH_MASK = 4;
    public static final int NARROW_FORMAT = 4;
    public static final int NARROW_STANDALONE = 32772;
    public static final int NOVEMBER = 10;
    public static final int OCTOBER = 9;
    public static final int PM = 1;
    public static final int SATURDAY = 7;
    public static final int SECOND = 13;
    static final int SECOND_MASK = 8192;
    public static final int SEPTEMBER = 8;
    public static final int SHORT = 1;
    public static final int SHORT_FORMAT = 1;
    public static final int SHORT_STANDALONE = 32769;
    static final int STANDALONE_MASK = 32768;
    public static final int SUNDAY = 1;
    public static final int THURSDAY = 5;
    public static final int TUESDAY = 3;
    public static final int UNDECIMBER = 12;
    private static final int UNSET = 0;
    public static final int WEDNESDAY = 4;
    public static final int WEEK_OF_MONTH = 4;
    static final int WEEK_OF_MONTH_MASK = 16;
    public static final int WEEK_OF_YEAR = 3;
    static final int WEEK_OF_YEAR_MASK = 8;
    public static final int YEAR = 1;
    static final int YEAR_MASK = 2;
    public static final int ZONE_OFFSET = 15;
    static final int ZONE_OFFSET_MASK = 32768;
    static final int currentSerialVersion = 1;
    static final long serialVersionUID = -1807547505821590642L;
    transient boolean areAllFieldsSet;
    protected boolean areFieldsSet;
    protected int[] fields;
    private int firstDayOfWeek;
    protected boolean[] isSet;
    protected boolean isTimeSet;
    private boolean lenient;
    private int minimalDaysInFirstWeek;
    private int nextStamp;
    private int serialVersionOnStream;
    private transient boolean sharedZone;
    private transient int[] stamp;
    protected long time;
    private TimeZone zone;
    private static final ConcurrentMap<Locale, int[]> cachedLocaleData = new ConcurrentHashMap(3);
    private static final String[] FIELD_NAME = {"ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH", "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR", "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET", "DST_OFFSET"};

    public abstract void add(int i, int i2);

    protected abstract void computeFields();

    protected abstract void computeTime();

    public abstract int getGreatestMinimum(int i);

    public abstract int getLeastMaximum(int i);

    public abstract int getMaximum(int i);

    public abstract int getMinimum(int i);

    public abstract void roll(int i, boolean z);

    public static class Builder {
        private static final int NFIELDS = 18;
        private static final int WEEK_YEAR = 17;
        private int[] fields;
        private int firstDayOfWeek;
        private long instant;
        private boolean lenient = true;
        private Locale locale;
        private int maxFieldIndex;
        private int minimalDaysInFirstWeek;
        private int nextStamp;
        private String type;
        private TimeZone zone;

        public Builder setInstant(long j) {
            if (this.fields != null) {
                throw new IllegalStateException();
            }
            this.instant = j;
            this.nextStamp = 1;
            return this;
        }

        public Builder setInstant(Date date) {
            return setInstant(date.getTime());
        }

        public Builder set(int i, int i2) {
            if (i < 0 || i >= 17) {
                throw new IllegalArgumentException("field is invalid");
            }
            if (isInstantSet()) {
                throw new IllegalStateException("instant has been set");
            }
            allocateFields();
            internalSet(i, i2);
            return this;
        }

        public Builder setFields(int... iArr) {
            int length = iArr.length;
            if (length % 2 != 0) {
                throw new IllegalArgumentException();
            }
            if (isInstantSet()) {
                throw new IllegalStateException("instant has been set");
            }
            if (this.nextStamp + (length / 2) < 0) {
                throw new IllegalStateException("stamp counter overflow");
            }
            allocateFields();
            int i = 0;
            while (i < length) {
                int i2 = i + 1;
                int i3 = iArr[i];
                if (i3 < 0 || i3 >= 17) {
                    throw new IllegalArgumentException("field is invalid");
                }
                internalSet(i3, iArr[i2]);
                i = i2 + 1;
            }
            return this;
        }

        public Builder setDate(int i, int i2, int i3) {
            return setFields(1, i, 2, i2, 5, i3);
        }

        public Builder setTimeOfDay(int i, int i2, int i3) {
            return setTimeOfDay(i, i2, i3, 0);
        }

        public Builder setTimeOfDay(int i, int i2, int i3, int i4) {
            return setFields(11, i, 12, i2, 13, i3, 14, i4);
        }

        public Builder setWeekDate(int i, int i2, int i3) {
            allocateFields();
            internalSet(17, i);
            internalSet(3, i2);
            internalSet(7, i3);
            return this;
        }

        public Builder setTimeZone(TimeZone timeZone) {
            if (timeZone == null) {
                throw new NullPointerException();
            }
            this.zone = timeZone;
            return this;
        }

        public Builder setLenient(boolean z) {
            this.lenient = z;
            return this;
        }

        public Builder setCalendarType(String str) {
            if (str.equals("gregorian")) {
                str = "gregory";
            }
            if (!Calendar.getAvailableCalendarTypes().contains(str) && !str.equals("iso8601")) {
                throw new IllegalArgumentException("unknown calendar type: " + str);
            }
            if (this.type == null) {
                this.type = str;
            } else if (!this.type.equals(str)) {
                throw new IllegalStateException("calendar type override");
            }
            return this;
        }

        public Builder setLocale(Locale locale) {
            if (locale == null) {
                throw new NullPointerException();
            }
            this.locale = locale;
            return this;
        }

        public Builder setWeekDefinition(int i, int i2) {
            if (!isValidWeekParameter(i) || !isValidWeekParameter(i2)) {
                throw new IllegalArgumentException();
            }
            this.firstDayOfWeek = i;
            this.minimalDaysInFirstWeek = i2;
            return this;
        }

        public Calendar build() {
            GregorianCalendar gregorianCalendar;
            if (this.locale == null) {
                this.locale = Locale.getDefault();
            }
            if (this.zone == null) {
                this.zone = TimeZone.getDefault();
            }
            if (this.type == null) {
                this.type = this.locale.getUnicodeLocaleType("ca");
            }
            if (this.type == null) {
                this.type = "gregory";
            }
            String str = this.type;
            byte b = -1;
            int iHashCode = str.hashCode();
            if (iHashCode != 283776265) {
                if (iHashCode == 2095190916 && str.equals("iso8601")) {
                    b = 1;
                }
            } else if (str.equals("gregory")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    gregorianCalendar = new GregorianCalendar(this.zone, this.locale, true);
                    break;
                case 1:
                    gregorianCalendar = new GregorianCalendar(this.zone, this.locale, true);
                    gregorianCalendar.setGregorianChange(new Date(Long.MIN_VALUE));
                    setWeekDefinition(2, 4);
                    break;
                default:
                    throw new IllegalArgumentException("unknown calendar type: " + this.type);
            }
            gregorianCalendar.setLenient(this.lenient);
            if (this.firstDayOfWeek != 0) {
                gregorianCalendar.setFirstDayOfWeek(this.firstDayOfWeek);
                gregorianCalendar.setMinimalDaysInFirstWeek(this.minimalDaysInFirstWeek);
            }
            if (isInstantSet()) {
                gregorianCalendar.setTimeInMillis(this.instant);
                gregorianCalendar.complete();
                return gregorianCalendar;
            }
            if (this.fields != null) {
                boolean z = isSet(17) && this.fields[17] > this.fields[1];
                if (z && !gregorianCalendar.isWeekDateSupported()) {
                    throw new IllegalArgumentException("week date is unsupported by " + this.type);
                }
                for (int i = 2; i < this.nextStamp; i++) {
                    int i2 = 0;
                    while (true) {
                        if (i2 > this.maxFieldIndex) {
                            break;
                        }
                        if (this.fields[i2] != i) {
                            i2++;
                        } else {
                            gregorianCalendar.set(i2, this.fields[18 + i2]);
                        }
                    }
                }
                if (z) {
                    gregorianCalendar.setWeekDate(this.fields[35], isSet(3) ? this.fields[21] : 1, isSet(7) ? this.fields[25] : gregorianCalendar.getFirstDayOfWeek());
                }
                gregorianCalendar.complete();
            }
            return gregorianCalendar;
        }

        private void allocateFields() {
            if (this.fields == null) {
                this.fields = new int[36];
                this.nextStamp = 2;
                this.maxFieldIndex = -1;
            }
        }

        private void internalSet(int i, int i2) {
            int[] iArr = this.fields;
            int i3 = this.nextStamp;
            this.nextStamp = i3 + 1;
            iArr[i] = i3;
            if (this.nextStamp < 0) {
                throw new IllegalStateException("stamp counter overflow");
            }
            this.fields[18 + i] = i2;
            if (i > this.maxFieldIndex && i < 17) {
                this.maxFieldIndex = i;
            }
        }

        private boolean isInstantSet() {
            if (this.nextStamp == 1) {
                return true;
            }
            return Calendar.$assertionsDisabled;
        }

        private boolean isSet(int i) {
            if (this.fields == null || this.fields[i] <= 0) {
                return Calendar.$assertionsDisabled;
            }
            return true;
        }

        private boolean isValidWeekParameter(int i) {
            if (i <= 0 || i > 7) {
                return Calendar.$assertionsDisabled;
            }
            return true;
        }
    }

    protected Calendar() {
        this(TimeZone.getDefaultRef(), Locale.getDefault(Locale.Category.FORMAT));
        this.sharedZone = true;
    }

    protected Calendar(TimeZone timeZone, Locale locale) {
        this.lenient = true;
        this.sharedZone = $assertionsDisabled;
        this.nextStamp = 2;
        this.serialVersionOnStream = 1;
        locale = locale == null ? Locale.getDefault() : locale;
        this.fields = new int[17];
        this.isSet = new boolean[17];
        this.stamp = new int[17];
        this.zone = timeZone;
        setWeekCountData(locale);
    }

    public static Calendar getInstance() {
        return createCalendar(TimeZone.getDefault(), Locale.getDefault(Locale.Category.FORMAT));
    }

    public static Calendar getInstance(TimeZone timeZone) {
        return createCalendar(timeZone, Locale.getDefault(Locale.Category.FORMAT));
    }

    public static Calendar getInstance(Locale locale) {
        return createCalendar(TimeZone.getDefault(), locale);
    }

    public static Calendar getInstance(TimeZone timeZone, Locale locale) {
        return createCalendar(timeZone, locale);
    }

    public static Calendar getJapaneseImperialInstance(TimeZone timeZone, Locale locale) {
        return new JapaneseImperialCalendar(timeZone, locale);
    }

    private static Calendar createCalendar(TimeZone timeZone, Locale locale) {
        return new GregorianCalendar(timeZone, locale);
    }

    public static synchronized Locale[] getAvailableLocales() {
        return DateFormat.getAvailableLocales();
    }

    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    public final void setTime(Date date) {
        setTimeInMillis(date.getTime());
    }

    public long getTimeInMillis() {
        if (!this.isTimeSet) {
            updateTime();
        }
        return this.time;
    }

    public void setTimeInMillis(long j) {
        if (this.time == j && this.isTimeSet && this.areFieldsSet && this.areAllFieldsSet) {
            return;
        }
        this.time = j;
        this.isTimeSet = true;
        this.areFieldsSet = $assertionsDisabled;
        computeFields();
        this.areFieldsSet = true;
        this.areAllFieldsSet = true;
    }

    public int get(int i) {
        complete();
        return internalGet(i);
    }

    protected final int internalGet(int i) {
        return this.fields[i];
    }

    final void internalSet(int i, int i2) {
        this.fields[i] = i2;
    }

    public void set(int i, int i2) {
        if (this.areFieldsSet && !this.areAllFieldsSet) {
            computeFields();
        }
        internalSet(i, i2);
        this.isTimeSet = $assertionsDisabled;
        this.areFieldsSet = $assertionsDisabled;
        this.isSet[i] = true;
        int[] iArr = this.stamp;
        int i3 = this.nextStamp;
        this.nextStamp = i3 + 1;
        iArr[i] = i3;
        if (this.nextStamp == Integer.MAX_VALUE) {
            adjustStamp();
        }
    }

    public final void set(int i, int i2, int i3) {
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    public final void set(int i, int i2, int i3, int i4, int i5) {
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
    }

    public final void set(int i, int i2, int i3, int i4, int i5, int i6) {
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
    }

    public final void clear() {
        for (int i = 0; i < this.fields.length; i++) {
            int[] iArr = this.stamp;
            this.fields[i] = 0;
            iArr[i] = 0;
            this.isSet[i] = $assertionsDisabled;
        }
        this.areFieldsSet = $assertionsDisabled;
        this.areAllFieldsSet = $assertionsDisabled;
        this.isTimeSet = $assertionsDisabled;
    }

    public final void clear(int i) {
        this.fields[i] = 0;
        this.stamp[i] = 0;
        this.isSet[i] = $assertionsDisabled;
        this.areFieldsSet = $assertionsDisabled;
        this.areAllFieldsSet = $assertionsDisabled;
        this.isTimeSet = $assertionsDisabled;
    }

    public final boolean isSet(int i) {
        if (this.stamp[i] != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    public String getDisplayName(int i, int i2, Locale locale) {
        if (i2 == 0) {
            i2 = 1;
        }
        if (!checkDisplayNameParams(i, i2, 1, 4, locale, 645)) {
            return null;
        }
        String calendarType = getCalendarType();
        int i3 = get(i);
        if (isStandaloneStyle(i2) || isNarrowFormatStyle(i2)) {
            String strRetrieveFieldValueName = CalendarDataUtility.retrieveFieldValueName(calendarType, i, i3, i2, locale);
            if (strRetrieveFieldValueName == null) {
                if (isNarrowFormatStyle(i2)) {
                    return CalendarDataUtility.retrieveFieldValueName(calendarType, i, i3, toStandaloneStyle(i2), locale);
                }
                if (isStandaloneStyle(i2)) {
                    return CalendarDataUtility.retrieveFieldValueName(calendarType, i, i3, getBaseStyle(i2), locale);
                }
                return strRetrieveFieldValueName;
            }
            return strRetrieveFieldValueName;
        }
        String[] fieldStrings = getFieldStrings(i, i2, DateFormatSymbols.getInstance(locale));
        if (fieldStrings == null || i3 >= fieldStrings.length) {
            return null;
        }
        return fieldStrings[i3];
    }

    public Map<String, Integer> getDisplayNames(int i, int i2, Locale locale) {
        if (!checkDisplayNameParams(i, i2, 0, 4, locale, 645)) {
            return null;
        }
        complete();
        String calendarType = getCalendarType();
        if (i2 == 0 || isStandaloneStyle(i2) || isNarrowFormatStyle(i2)) {
            Map<String, Integer> mapRetrieveFieldValueNames = CalendarDataUtility.retrieveFieldValueNames(calendarType, i, i2, locale);
            if (mapRetrieveFieldValueNames == null) {
                if (isNarrowFormatStyle(i2)) {
                    return CalendarDataUtility.retrieveFieldValueNames(calendarType, i, toStandaloneStyle(i2), locale);
                }
                if (i2 != 0) {
                    return CalendarDataUtility.retrieveFieldValueNames(calendarType, i, getBaseStyle(i2), locale);
                }
                return mapRetrieveFieldValueNames;
            }
            return mapRetrieveFieldValueNames;
        }
        return getDisplayNamesImpl(i, i2, locale);
    }

    private Map<String, Integer> getDisplayNamesImpl(int i, int i2, Locale locale) {
        String[] fieldStrings = getFieldStrings(i, i2, DateFormatSymbols.getInstance(locale));
        if (fieldStrings != null) {
            HashMap map = new HashMap();
            for (int i3 = 0; i3 < fieldStrings.length; i3++) {
                if (fieldStrings[i3].length() != 0) {
                    map.put(fieldStrings[i3], Integer.valueOf(i3));
                }
            }
            return map;
        }
        return null;
    }

    boolean checkDisplayNameParams(int i, int i2, int i3, int i4, Locale locale, int i5) {
        int baseStyle = getBaseStyle(i2);
        if (i < 0 || i >= this.fields.length || baseStyle < i3 || baseStyle > i4) {
            throw new IllegalArgumentException();
        }
        if (baseStyle == 3) {
            throw new IllegalArgumentException();
        }
        if (locale == null) {
            throw new NullPointerException();
        }
        return isFieldSet(i5, i);
    }

    private String[] getFieldStrings(int i, int i2, DateFormatSymbols dateFormatSymbols) {
        int baseStyle = getBaseStyle(i2);
        if (baseStyle == 4) {
            return null;
        }
        if (i == 0) {
            return dateFormatSymbols.getEras();
        }
        if (i == 2) {
            return baseStyle == 2 ? dateFormatSymbols.getMonths() : dateFormatSymbols.getShortMonths();
        }
        if (i == 7) {
            return baseStyle == 2 ? dateFormatSymbols.getWeekdays() : dateFormatSymbols.getShortWeekdays();
        }
        if (i != 9) {
            return null;
        }
        return dateFormatSymbols.getAmPmStrings();
    }

    protected void complete() {
        if (!this.isTimeSet) {
            updateTime();
        }
        if (!this.areFieldsSet || !this.areAllFieldsSet) {
            computeFields();
            this.areFieldsSet = true;
            this.areAllFieldsSet = true;
        }
    }

    final boolean isExternallySet(int i) {
        if (this.stamp[i] >= 2) {
            return true;
        }
        return $assertionsDisabled;
    }

    final int getSetStateFields() {
        int i = 0;
        for (int i2 = 0; i2 < this.fields.length; i2++) {
            if (this.stamp[i2] != 0) {
                i |= 1 << i2;
            }
        }
        return i;
    }

    final void setFieldsComputed(int i) {
        if (i == ALL_FIELDS) {
            for (int i2 = 0; i2 < this.fields.length; i2++) {
                this.stamp[i2] = 1;
                this.isSet[i2] = true;
            }
            this.areAllFieldsSet = true;
            this.areFieldsSet = true;
            return;
        }
        int i3 = i;
        for (int i4 = 0; i4 < this.fields.length; i4++) {
            if ((i3 & 1) == 1) {
                this.stamp[i4] = 1;
                this.isSet[i4] = true;
            } else if (this.areAllFieldsSet && !this.isSet[i4]) {
                this.areAllFieldsSet = $assertionsDisabled;
            }
            i3 >>>= 1;
        }
    }

    final void setFieldsNormalized(int i) {
        if (i != ALL_FIELDS) {
            int i2 = i;
            for (int i3 = 0; i3 < this.fields.length; i3++) {
                if ((i2 & 1) == 0) {
                    int[] iArr = this.stamp;
                    this.fields[i3] = 0;
                    iArr[i3] = 0;
                    this.isSet[i3] = $assertionsDisabled;
                }
                i2 >>= 1;
            }
        }
        this.areFieldsSet = true;
        this.areAllFieldsSet = $assertionsDisabled;
    }

    final boolean isPartiallyNormalized() {
        if (!this.areFieldsSet || this.areAllFieldsSet) {
            return $assertionsDisabled;
        }
        return true;
    }

    final boolean isFullyNormalized() {
        if (this.areFieldsSet && this.areAllFieldsSet) {
            return true;
        }
        return $assertionsDisabled;
    }

    final void setUnnormalized() {
        this.areAllFieldsSet = $assertionsDisabled;
        this.areFieldsSet = $assertionsDisabled;
    }

    static boolean isFieldSet(int i, int i2) {
        if ((i & (1 << i2)) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    final int selectFields() {
        int i;
        int i2 = this.stamp[0] != 0 ? 3 : 2;
        int i3 = this.stamp[7];
        int i4 = this.stamp[2];
        int i5 = this.stamp[5];
        int iAggregateStamp = aggregateStamp(this.stamp[4], i3);
        int iAggregateStamp2 = aggregateStamp(this.stamp[8], i3);
        int i6 = this.stamp[6];
        int iAggregateStamp3 = aggregateStamp(this.stamp[3], i3);
        int i7 = iAggregateStamp > i5 ? iAggregateStamp : i5;
        if (iAggregateStamp2 > i7) {
            i7 = iAggregateStamp2;
        }
        if (i6 > i7) {
            i7 = i6;
        }
        if (iAggregateStamp3 <= i7) {
            iAggregateStamp3 = i7;
        }
        if (iAggregateStamp3 == 0) {
            iAggregateStamp = this.stamp[4];
            iAggregateStamp2 = Math.max(this.stamp[8], i3);
            iAggregateStamp3 = Math.max(Math.max(iAggregateStamp, iAggregateStamp2), this.stamp[3]);
            if (iAggregateStamp3 == 0) {
                i5 = i4;
                iAggregateStamp3 = i5;
            }
        }
        if (iAggregateStamp3 == i5 || ((iAggregateStamp3 == iAggregateStamp && this.stamp[4] >= this.stamp[3]) || (iAggregateStamp3 == iAggregateStamp2 && this.stamp[8] >= this.stamp[3]))) {
            i = i2 | 4;
            if (iAggregateStamp3 == i5) {
                i |= 32;
            } else {
                if (i3 != 0) {
                    i |= 128;
                }
                if (iAggregateStamp == iAggregateStamp2) {
                    if (this.stamp[4] >= this.stamp[8]) {
                        i |= 16;
                    } else {
                        i |= 256;
                    }
                } else if (iAggregateStamp3 == iAggregateStamp) {
                    i |= 16;
                } else if (this.stamp[8] != 0) {
                    i |= 256;
                }
            }
        } else if (iAggregateStamp3 == i6) {
            i = i2 | 64;
        } else {
            if (i3 != 0) {
                i2 |= 128;
            }
            i = i2 | 8;
        }
        int i8 = this.stamp[11];
        int iAggregateStamp4 = aggregateStamp(this.stamp[10], this.stamp[9]);
        if (iAggregateStamp4 <= i8) {
            iAggregateStamp4 = i8;
        }
        if (iAggregateStamp4 == 0) {
            iAggregateStamp4 = Math.max(this.stamp[10], this.stamp[9]);
        }
        if (iAggregateStamp4 != 0) {
            if (iAggregateStamp4 == i8) {
                i |= 2048;
            } else {
                i |= 1024;
                if (this.stamp[9] != 0) {
                    i |= 512;
                }
            }
        }
        if (this.stamp[12] != 0) {
            i |= 4096;
        }
        if (this.stamp[13] != 0) {
            i |= 8192;
        }
        if (this.stamp[14] != 0) {
            i |= 16384;
        }
        if (this.stamp[15] >= 2) {
            i |= NumericShaper.MYANMAR;
        }
        if (this.stamp[16] >= 2) {
            return i | 65536;
        }
        return i;
    }

    int getBaseStyle(int i) {
        return i & (-32769);
    }

    private int toStandaloneStyle(int i) {
        return i | NumericShaper.MYANMAR;
    }

    private boolean isStandaloneStyle(int i) {
        if ((i & NumericShaper.MYANMAR) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    private boolean isNarrowStyle(int i) {
        if (i == 4 || i == 32772) {
            return true;
        }
        return $assertionsDisabled;
    }

    private boolean isNarrowFormatStyle(int i) {
        if (i == 4) {
            return true;
        }
        return $assertionsDisabled;
    }

    private static int aggregateStamp(int i, int i2) {
        if (i == 0 || i2 == 0) {
            return 0;
        }
        return i > i2 ? i : i2;
    }

    public static Set<String> getAvailableCalendarTypes() {
        return AvailableCalendarTypes.SET;
    }

    private static class AvailableCalendarTypes {
        private static final Set<String> SET;

        static {
            HashSet hashSet = new HashSet(3);
            hashSet.add("gregory");
            SET = Collections.unmodifiableSet(hashSet);
        }

        private AvailableCalendarTypes() {
        }
    }

    public String getCalendarType() {
        return getClass().getName();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            Calendar calendar = (Calendar) obj;
            if (compareTo(getMillisOf(calendar)) == 0 && this.lenient == calendar.lenient && this.firstDayOfWeek == calendar.firstDayOfWeek && this.minimalDaysInFirstWeek == calendar.minimalDaysInFirstWeek) {
                if (this.zone.equals(calendar.zone)) {
                    return true;
                }
            }
            return $assertionsDisabled;
        } catch (Exception e) {
            return $assertionsDisabled;
        }
    }

    public int hashCode() {
        boolean z = this.lenient;
        int iHashCode = (z ? 1 : 0) | (this.firstDayOfWeek << 1) | (this.minimalDaysInFirstWeek << 4) | (this.zone.hashCode() << 7);
        long millisOf = getMillisOf(this);
        return iHashCode ^ (((int) (millisOf >> 32)) ^ ((int) millisOf));
    }

    public boolean before(Object obj) {
        if (!(obj instanceof Calendar) || compareTo((Calendar) obj) >= 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    public boolean after(Object obj) {
        if (!(obj instanceof Calendar) || compareTo((Calendar) obj) <= 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    @Override
    public int compareTo(Calendar calendar) {
        return compareTo(getMillisOf(calendar));
    }

    public void roll(int i, int i2) {
        while (i2 > 0) {
            roll(i, true);
            i2--;
        }
        while (i2 < 0) {
            roll(i, $assertionsDisabled);
            i2++;
        }
    }

    public void setTimeZone(TimeZone timeZone) {
        this.zone = timeZone;
        this.sharedZone = $assertionsDisabled;
        this.areFieldsSet = $assertionsDisabled;
        this.areAllFieldsSet = $assertionsDisabled;
    }

    public TimeZone getTimeZone() {
        if (this.sharedZone) {
            this.zone = (TimeZone) this.zone.clone();
            this.sharedZone = $assertionsDisabled;
        }
        return this.zone;
    }

    TimeZone getZone() {
        return this.zone;
    }

    void setZoneShared(boolean z) {
        this.sharedZone = z;
    }

    public void setLenient(boolean z) {
        this.lenient = z;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public void setFirstDayOfWeek(int i) {
        if (this.firstDayOfWeek == i) {
            return;
        }
        this.firstDayOfWeek = i;
        invalidateWeekFields();
    }

    public int getFirstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    public void setMinimalDaysInFirstWeek(int i) {
        if (this.minimalDaysInFirstWeek == i) {
            return;
        }
        this.minimalDaysInFirstWeek = i;
        invalidateWeekFields();
    }

    public int getMinimalDaysInFirstWeek() {
        return this.minimalDaysInFirstWeek;
    }

    public boolean isWeekDateSupported() {
        return $assertionsDisabled;
    }

    public int getWeekYear() {
        throw new UnsupportedOperationException();
    }

    public void setWeekDate(int i, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    public int getWeeksInWeekYear() {
        throw new UnsupportedOperationException();
    }

    public int getActualMinimum(int i) {
        int greatestMinimum = getGreatestMinimum(i);
        int minimum = getMinimum(i);
        if (greatestMinimum == minimum) {
            return greatestMinimum;
        }
        Calendar calendar = (Calendar) clone();
        calendar.setLenient(true);
        int i2 = greatestMinimum;
        while (true) {
            calendar.set(i, greatestMinimum);
            if (calendar.get(i) == greatestMinimum) {
                int i3 = greatestMinimum - 1;
                if (i3 >= minimum) {
                    i2 = greatestMinimum;
                    greatestMinimum = i3;
                } else {
                    return greatestMinimum;
                }
            } else {
                return i2;
            }
        }
    }

    public int getActualMaximum(int i) {
        int leastMaximum = getLeastMaximum(i);
        int maximum = getMaximum(i);
        if (leastMaximum == maximum) {
            return leastMaximum;
        }
        Calendar calendar = (Calendar) clone();
        calendar.setLenient(true);
        if (i == 3 || i == 4) {
            calendar.set(7, this.firstDayOfWeek);
        }
        int i2 = leastMaximum;
        while (true) {
            calendar.set(i, leastMaximum);
            if (calendar.get(i) == leastMaximum) {
                int i3 = leastMaximum + 1;
                if (i3 <= maximum) {
                    i2 = leastMaximum;
                    leastMaximum = i3;
                } else {
                    return leastMaximum;
                }
            } else {
                return i2;
            }
        }
    }

    public Object clone() {
        try {
            Calendar calendar = (Calendar) super.clone();
            calendar.fields = new int[17];
            calendar.isSet = new boolean[17];
            calendar.stamp = new int[17];
            for (int i = 0; i < 17; i++) {
                calendar.fields[i] = this.fields[i];
                calendar.stamp[i] = this.stamp[i];
                calendar.isSet[i] = this.isSet[i];
            }
            calendar.zone = (TimeZone) this.zone.clone();
            return calendar;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    static String getFieldName(int i) {
        return FIELD_NAME[i];
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(800);
        sb.append(getClass().getName());
        sb.append('[');
        appendValue(sb, "time", this.isTimeSet, this.time);
        sb.append(",areFieldsSet=");
        sb.append(this.areFieldsSet);
        sb.append(",areAllFieldsSet=");
        sb.append(this.areAllFieldsSet);
        sb.append(",lenient=");
        sb.append(this.lenient);
        sb.append(",zone=");
        sb.append((Object) this.zone);
        appendValue(sb, ",firstDayOfWeek", true, this.firstDayOfWeek);
        appendValue(sb, ",minimalDaysInFirstWeek", true, this.minimalDaysInFirstWeek);
        for (int i = 0; i < 17; i++) {
            sb.append(',');
            appendValue(sb, FIELD_NAME[i], isSet(i), this.fields[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static void appendValue(StringBuilder sb, String str, boolean z, long j) {
        sb.append(str);
        sb.append('=');
        if (z) {
            sb.append(j);
        } else {
            sb.append('?');
        }
    }

    private void setWeekCountData(Locale locale) {
        int[] iArr = cachedLocaleData.get(locale);
        if (iArr == null) {
            LocaleData localeData = LocaleData.get(locale);
            iArr = new int[]{localeData.firstDayOfWeek.intValue(), localeData.minimalDaysInFirstWeek.intValue()};
            cachedLocaleData.putIfAbsent(locale, iArr);
        }
        this.firstDayOfWeek = iArr[0];
        this.minimalDaysInFirstWeek = iArr[1];
    }

    private void updateTime() {
        computeTime();
        this.isTimeSet = true;
    }

    private int compareTo(long j) {
        long millisOf = getMillisOf(this);
        if (millisOf > j) {
            return 1;
        }
        return millisOf == j ? 0 : -1;
    }

    private static long getMillisOf(Calendar calendar) {
        if (calendar.isTimeSet) {
            return calendar.time;
        }
        Calendar calendar2 = (Calendar) calendar.clone();
        calendar2.setLenient(true);
        return calendar2.getTimeInMillis();
    }

    private void adjustStamp() {
        int i = 2;
        int i2 = 2;
        while (true) {
            int i3 = i;
            int i4 = Integer.MAX_VALUE;
            for (int i5 = 0; i5 < this.stamp.length; i5++) {
                int i6 = this.stamp[i5];
                if (i6 >= i2 && i4 > i6) {
                    i4 = i6;
                }
                if (i3 < i6) {
                    i3 = i6;
                }
            }
            if (i3 != i4 && i4 == Integer.MAX_VALUE) {
                break;
            }
            for (int i7 = 0; i7 < this.stamp.length; i7++) {
                if (this.stamp[i7] == i4) {
                    this.stamp[i7] = i2;
                }
            }
            i2++;
            if (i4 == i3) {
                break;
            } else {
                i = i3;
            }
        }
        this.nextStamp = i2;
    }

    private void invalidateWeekFields() {
        int i;
        int i2;
        if (this.stamp[4] != 1 && this.stamp[3] != 1) {
            return;
        }
        Calendar calendar = (Calendar) clone();
        calendar.setLenient(true);
        calendar.clear(4);
        calendar.clear(3);
        if (this.stamp[4] == 1 && this.fields[4] != (i2 = calendar.get(4))) {
            this.fields[4] = i2;
        }
        if (this.stamp[3] == 1 && this.fields[3] != (i = calendar.get(3))) {
            this.fields[3] = i;
        }
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        if (!this.isTimeSet) {
            try {
                updateTime();
            } catch (IllegalArgumentException e) {
            }
        }
        objectOutputStream.defaultWriteObject();
    }

    private static class CalendarAccessControlContext {
        private static final AccessControlContext INSTANCE;

        static {
            Permission runtimePermission = new RuntimePermission("accessClassInPackage.sun.util.calendar");
            PermissionCollection permissionCollectionNewPermissionCollection = runtimePermission.newPermissionCollection();
            permissionCollectionNewPermissionCollection.add(runtimePermission);
            INSTANCE = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, permissionCollectionNewPermissionCollection)});
        }

        private CalendarAccessControlContext() {
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        String id;
        TimeZone timeZone;
        objectInputStream.defaultReadObject();
        this.stamp = new int[17];
        if (this.serialVersionOnStream >= 2) {
            this.isTimeSet = true;
            if (this.fields == null) {
                this.fields = new int[17];
            }
            if (this.isSet == null) {
                this.isSet = new boolean[17];
            }
        } else if (this.serialVersionOnStream >= 0) {
            for (int i = 0; i < 17; i++) {
                this.stamp[i] = this.isSet[i] ? 1 : 0;
            }
        }
        this.serialVersionOnStream = 1;
        if ((this.zone instanceof SimpleTimeZone) && (timeZone = TimeZone.getTimeZone((id = this.zone.getID()))) != null && timeZone.hasSameRules(this.zone) && timeZone.getID().equals(id)) {
            this.zone = timeZone;
        }
    }

    public final Instant toInstant() {
        return Instant.ofEpochMilli(getTimeInMillis());
    }
}
