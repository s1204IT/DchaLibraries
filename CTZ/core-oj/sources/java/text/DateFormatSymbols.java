package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import libcore.icu.ICU;
import libcore.icu.LocaleData;
import libcore.icu.TimeZoneNames;

public class DateFormatSymbols implements Serializable, Cloneable {
    static final int PATTERN_AM_PM = 14;
    static final int PATTERN_DAY_OF_MONTH = 3;
    static final int PATTERN_DAY_OF_WEEK = 9;
    static final int PATTERN_DAY_OF_WEEK_IN_MONTH = 11;
    static final int PATTERN_DAY_OF_YEAR = 10;
    static final int PATTERN_DAY_PERIOD = 24;
    static final int PATTERN_ERA = 0;
    static final int PATTERN_FLEXIBLE_DAY_PERIOD = 25;
    static final int PATTERN_HOUR0 = 16;
    static final int PATTERN_HOUR1 = 15;
    static final int PATTERN_HOUR_OF_DAY0 = 5;
    static final int PATTERN_HOUR_OF_DAY1 = 4;
    static final int PATTERN_ISO_DAY_OF_WEEK = 20;
    static final int PATTERN_ISO_ZONE = 21;
    static final int PATTERN_MILLISECOND = 8;
    static final int PATTERN_MINUTE = 6;
    static final int PATTERN_MONTH = 2;
    static final int PATTERN_MONTH_STANDALONE = 22;
    static final int PATTERN_SECOND = 7;
    static final int PATTERN_STANDALONE_DAY_OF_WEEK = 23;
    static final int PATTERN_WEEK_OF_MONTH = 13;
    static final int PATTERN_WEEK_OF_YEAR = 12;
    static final int PATTERN_WEEK_YEAR = 19;
    static final int PATTERN_YEAR = 1;
    static final int PATTERN_ZONE_NAME = 17;
    static final int PATTERN_ZONE_VALUE = 18;
    private static final ConcurrentMap<Locale, SoftReference<DateFormatSymbols>> cachedInstances = new ConcurrentHashMap(3);
    static final int currentSerialVersion = 1;
    static final int millisPerHour = 3600000;
    static final String patternChars = "GyMdkHmsSEDFwWahKzZYuXLcbB";
    static final long serialVersionUID = -5987973545549424702L;
    private String[] shortStandAloneMonths;
    private String[] shortStandAloneWeekdays;
    private String[] standAloneMonths;
    private String[] standAloneWeekdays;
    private String[] tinyMonths;
    private String[] tinyStandAloneMonths;
    private String[] tinyStandAloneWeekdays;
    private String[] tinyWeekdays;
    String[] eras = null;
    String[] months = null;
    String[] shortMonths = null;
    String[] weekdays = null;
    String[] shortWeekdays = null;
    String[] ampms = null;
    String[][] zoneStrings = null;
    transient boolean isZoneStringsSet = false;
    String localPatternChars = null;
    Locale locale = null;
    private int serialVersionOnStream = 1;
    private transient int lastZoneIndex = 0;
    volatile transient int cachedHashCode = 0;

    public DateFormatSymbols() {
        initializeData(Locale.getDefault(Locale.Category.FORMAT));
    }

    public DateFormatSymbols(Locale locale) {
        initializeData(locale);
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableLocales();
    }

    public static final DateFormatSymbols getInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT));
    }

    public static final DateFormatSymbols getInstance(Locale locale) {
        return (DateFormatSymbols) getCachedInstance(locale).clone();
    }

    static final DateFormatSymbols getInstanceRef(Locale locale) {
        return getCachedInstance(locale);
    }

    private static DateFormatSymbols getCachedInstance(Locale locale) {
        DateFormatSymbols dateFormatSymbols;
        SoftReference<DateFormatSymbols> softReference = cachedInstances.get(locale);
        if (softReference == null || (dateFormatSymbols = softReference.get()) == null) {
            DateFormatSymbols dateFormatSymbols2 = new DateFormatSymbols(locale);
            SoftReference<DateFormatSymbols> softReference2 = new SoftReference<>(dateFormatSymbols2);
            SoftReference<DateFormatSymbols> softReferencePutIfAbsent = cachedInstances.putIfAbsent(locale, softReference2);
            if (softReferencePutIfAbsent != null) {
                DateFormatSymbols dateFormatSymbols3 = softReferencePutIfAbsent.get();
                if (dateFormatSymbols3 == null) {
                    cachedInstances.put(locale, softReference2);
                    return dateFormatSymbols2;
                }
                return dateFormatSymbols3;
            }
            return dateFormatSymbols2;
        }
        return dateFormatSymbols;
    }

    public String[] getEras() {
        return (String[]) Arrays.copyOf(this.eras, this.eras.length);
    }

    public void setEras(String[] strArr) {
        this.eras = (String[]) Arrays.copyOf(strArr, strArr.length);
        this.cachedHashCode = 0;
    }

    public String[] getMonths() {
        return (String[]) Arrays.copyOf(this.months, this.months.length);
    }

    public void setMonths(String[] strArr) {
        this.months = (String[]) Arrays.copyOf(strArr, strArr.length);
        this.cachedHashCode = 0;
    }

    public String[] getShortMonths() {
        return (String[]) Arrays.copyOf(this.shortMonths, this.shortMonths.length);
    }

    public void setShortMonths(String[] strArr) {
        this.shortMonths = (String[]) Arrays.copyOf(strArr, strArr.length);
        this.cachedHashCode = 0;
    }

    public String[] getWeekdays() {
        return (String[]) Arrays.copyOf(this.weekdays, this.weekdays.length);
    }

    public void setWeekdays(String[] strArr) {
        this.weekdays = (String[]) Arrays.copyOf(strArr, strArr.length);
        this.cachedHashCode = 0;
    }

    public String[] getShortWeekdays() {
        return (String[]) Arrays.copyOf(this.shortWeekdays, this.shortWeekdays.length);
    }

    public void setShortWeekdays(String[] strArr) {
        this.shortWeekdays = (String[]) Arrays.copyOf(strArr, strArr.length);
        this.cachedHashCode = 0;
    }

    public String[] getAmPmStrings() {
        return (String[]) Arrays.copyOf(this.ampms, this.ampms.length);
    }

    public void setAmPmStrings(String[] strArr) {
        this.ampms = (String[]) Arrays.copyOf(strArr, strArr.length);
        this.cachedHashCode = 0;
    }

    public String[][] getZoneStrings() {
        return getZoneStringsImpl(true);
    }

    public void setZoneStrings(String[][] strArr) {
        String[][] strArr2 = new String[strArr.length][];
        for (int i = 0; i < strArr.length; i++) {
            int length = strArr[i].length;
            if (length < 5) {
                throw new IllegalArgumentException();
            }
            strArr2[i] = (String[]) Arrays.copyOf(strArr[i], length);
        }
        this.zoneStrings = strArr2;
        this.isZoneStringsSet = true;
    }

    public String getLocalPatternChars() {
        return this.localPatternChars;
    }

    public void setLocalPatternChars(String str) {
        this.localPatternChars = str.toString();
        this.cachedHashCode = 0;
    }

    String[] getTinyMonths() {
        return this.tinyMonths;
    }

    String[] getStandAloneMonths() {
        return this.standAloneMonths;
    }

    String[] getShortStandAloneMonths() {
        return this.shortStandAloneMonths;
    }

    String[] getTinyStandAloneMonths() {
        return this.tinyStandAloneMonths;
    }

    String[] getTinyWeekdays() {
        return this.tinyWeekdays;
    }

    String[] getStandAloneWeekdays() {
        return this.standAloneWeekdays;
    }

    String[] getShortStandAloneWeekdays() {
        return this.shortStandAloneWeekdays;
    }

    String[] getTinyStandAloneWeekdays() {
        return this.tinyStandAloneWeekdays;
    }

    public Object clone() {
        try {
            DateFormatSymbols dateFormatSymbols = (DateFormatSymbols) super.clone();
            copyMembers(this, dateFormatSymbols);
            return dateFormatSymbols;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public int hashCode() {
        int i = this.cachedHashCode;
        if (i == 0) {
            int iHashCode = Objects.hashCode(this.localPatternChars) + (11 * (((((((((((55 + Arrays.hashCode(this.eras)) * 11) + Arrays.hashCode(this.months)) * 11) + Arrays.hashCode(this.shortMonths)) * 11) + Arrays.hashCode(this.weekdays)) * 11) + Arrays.hashCode(this.shortWeekdays)) * 11) + Arrays.hashCode(this.ampms)));
            this.cachedHashCode = iHashCode;
            return iHashCode;
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DateFormatSymbols dateFormatSymbols = (DateFormatSymbols) obj;
        if (!Arrays.equals(this.eras, dateFormatSymbols.eras) || !Arrays.equals(this.months, dateFormatSymbols.months) || !Arrays.equals(this.shortMonths, dateFormatSymbols.shortMonths) || !Arrays.equals(this.tinyMonths, dateFormatSymbols.tinyMonths) || !Arrays.equals(this.weekdays, dateFormatSymbols.weekdays) || !Arrays.equals(this.shortWeekdays, dateFormatSymbols.shortWeekdays) || !Arrays.equals(this.tinyWeekdays, dateFormatSymbols.tinyWeekdays) || !Arrays.equals(this.standAloneMonths, dateFormatSymbols.standAloneMonths) || !Arrays.equals(this.shortStandAloneMonths, dateFormatSymbols.shortStandAloneMonths) || !Arrays.equals(this.tinyStandAloneMonths, dateFormatSymbols.tinyStandAloneMonths) || !Arrays.equals(this.standAloneWeekdays, dateFormatSymbols.standAloneWeekdays) || !Arrays.equals(this.shortStandAloneWeekdays, dateFormatSymbols.shortStandAloneWeekdays) || !Arrays.equals(this.tinyStandAloneWeekdays, dateFormatSymbols.tinyStandAloneWeekdays) || !Arrays.equals(this.ampms, dateFormatSymbols.ampms) || ((this.localPatternChars == null || !this.localPatternChars.equals(dateFormatSymbols.localPatternChars)) && (this.localPatternChars != null || dateFormatSymbols.localPatternChars != null))) {
            return false;
        }
        if (!this.isZoneStringsSet && !dateFormatSymbols.isZoneStringsSet && Objects.equals(this.locale, dateFormatSymbols.locale)) {
            return true;
        }
        return Arrays.deepEquals(getZoneStringsWrapper(), dateFormatSymbols.getZoneStringsWrapper());
    }

    private void initializeData(Locale locale) {
        DateFormatSymbols dateFormatSymbols;
        SoftReference<DateFormatSymbols> softReference = cachedInstances.get(locale);
        if (softReference != null && (dateFormatSymbols = softReference.get()) != null) {
            copyMembers(dateFormatSymbols, this);
            return;
        }
        Locale localeMapInvalidAndNullLocales = LocaleData.mapInvalidAndNullLocales(locale);
        LocaleData localeData = LocaleData.get(localeMapInvalidAndNullLocales);
        this.locale = localeMapInvalidAndNullLocales;
        this.eras = localeData.eras;
        this.months = localeData.longMonthNames;
        this.shortMonths = localeData.shortMonthNames;
        this.ampms = localeData.amPm;
        this.localPatternChars = patternChars;
        this.weekdays = localeData.longWeekdayNames;
        this.shortWeekdays = localeData.shortWeekdayNames;
        initializeSupplementaryData(localeData);
    }

    private void initializeSupplementaryData(LocaleData localeData) {
        this.tinyMonths = localeData.tinyMonthNames;
        this.tinyWeekdays = localeData.tinyWeekdayNames;
        this.standAloneMonths = localeData.longStandAloneMonthNames;
        this.shortStandAloneMonths = localeData.shortStandAloneMonthNames;
        this.tinyStandAloneMonths = localeData.tinyStandAloneMonthNames;
        this.standAloneWeekdays = localeData.longStandAloneWeekdayNames;
        this.shortStandAloneWeekdays = localeData.shortStandAloneWeekdayNames;
        this.tinyStandAloneWeekdays = localeData.tinyStandAloneWeekdayNames;
    }

    final int getZoneIndex(String str) {
        String[][] zoneStringsWrapper = getZoneStringsWrapper();
        if (this.lastZoneIndex < zoneStringsWrapper.length && str.equals(zoneStringsWrapper[this.lastZoneIndex][0])) {
            return this.lastZoneIndex;
        }
        for (int i = 0; i < zoneStringsWrapper.length; i++) {
            if (str.equals(zoneStringsWrapper[i][0])) {
                this.lastZoneIndex = i;
                return i;
            }
        }
        return -1;
    }

    final String[][] getZoneStringsWrapper() {
        if (isSubclassObject()) {
            return getZoneStrings();
        }
        return getZoneStringsImpl(false);
    }

    private synchronized String[][] internalZoneStrings() {
        if (this.zoneStrings == null) {
            this.zoneStrings = TimeZoneNames.getZoneStrings(this.locale);
        }
        return this.zoneStrings;
    }

    private String[][] getZoneStringsImpl(boolean z) {
        String[][] strArrInternalZoneStrings = internalZoneStrings();
        if (!z) {
            return strArrInternalZoneStrings;
        }
        int length = strArrInternalZoneStrings.length;
        String[][] strArr = new String[length][];
        for (int i = 0; i < length; i++) {
            strArr[i] = (String[]) Arrays.copyOf(strArrInternalZoneStrings[i], strArrInternalZoneStrings[i].length);
        }
        return strArr;
    }

    private boolean isSubclassObject() {
        return !getClass().getName().equals("java.text.DateFormatSymbols");
    }

    private void copyMembers(DateFormatSymbols dateFormatSymbols, DateFormatSymbols dateFormatSymbols2) {
        dateFormatSymbols2.locale = dateFormatSymbols.locale;
        dateFormatSymbols2.eras = (String[]) Arrays.copyOf(dateFormatSymbols.eras, dateFormatSymbols.eras.length);
        dateFormatSymbols2.months = (String[]) Arrays.copyOf(dateFormatSymbols.months, dateFormatSymbols.months.length);
        dateFormatSymbols2.shortMonths = (String[]) Arrays.copyOf(dateFormatSymbols.shortMonths, dateFormatSymbols.shortMonths.length);
        dateFormatSymbols2.weekdays = (String[]) Arrays.copyOf(dateFormatSymbols.weekdays, dateFormatSymbols.weekdays.length);
        dateFormatSymbols2.shortWeekdays = (String[]) Arrays.copyOf(dateFormatSymbols.shortWeekdays, dateFormatSymbols.shortWeekdays.length);
        dateFormatSymbols2.ampms = (String[]) Arrays.copyOf(dateFormatSymbols.ampms, dateFormatSymbols.ampms.length);
        if (dateFormatSymbols.zoneStrings != null) {
            dateFormatSymbols2.zoneStrings = dateFormatSymbols.getZoneStringsImpl(true);
        } else {
            dateFormatSymbols2.zoneStrings = null;
        }
        dateFormatSymbols2.localPatternChars = dateFormatSymbols.localPatternChars;
        dateFormatSymbols2.cachedHashCode = 0;
        dateFormatSymbols2.tinyMonths = dateFormatSymbols.tinyMonths;
        dateFormatSymbols2.tinyWeekdays = dateFormatSymbols.tinyWeekdays;
        dateFormatSymbols2.standAloneMonths = dateFormatSymbols.standAloneMonths;
        dateFormatSymbols2.shortStandAloneMonths = dateFormatSymbols.shortStandAloneMonths;
        dateFormatSymbols2.tinyStandAloneMonths = dateFormatSymbols.tinyStandAloneMonths;
        dateFormatSymbols2.standAloneWeekdays = dateFormatSymbols.standAloneWeekdays;
        dateFormatSymbols2.shortStandAloneWeekdays = dateFormatSymbols.shortStandAloneWeekdays;
        dateFormatSymbols2.tinyStandAloneWeekdays = dateFormatSymbols.tinyStandAloneWeekdays;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            initializeSupplementaryData(LocaleData.get(this.locale));
        }
        this.serialVersionOnStream = 1;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        internalZoneStrings();
        objectOutputStream.defaultWriteObject();
    }
}
