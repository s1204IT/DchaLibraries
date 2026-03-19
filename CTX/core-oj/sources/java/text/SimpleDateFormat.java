package java.text;

import android.icu.text.TimeZoneNames;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import libcore.icu.LocaleData;
import sun.util.calendar.CalendarUtils;

public class SimpleDateFormat extends DateFormat {
    static final boolean $assertionsDisabled = false;
    private static final String GMT = "GMT";
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int TAG_QUOTE_ASCII_CHAR = 100;
    private static final int TAG_QUOTE_CHARS = 101;
    static final int currentSerialVersion = 1;
    static final long serialVersionUID = 4774881970558875024L;
    private transient char[] compiledPattern;
    private Date defaultCenturyStart;
    private transient int defaultCenturyStartYear;
    private DateFormatSymbols formatData;
    private transient boolean hasFollowingMinusSign;
    private Locale locale;
    private transient char minusSign;
    private transient NumberFormat originalNumberFormat;
    private transient String originalNumberPattern;
    private String pattern;
    private int serialVersionOnStream;
    private transient TimeZoneNames timeZoneNames;
    transient boolean useDateFormatSymbols;
    private transient char zeroDigit;
    private static final ConcurrentMap<Locale, NumberFormat> cachedNumberFormatData = new ConcurrentHashMap(3);
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD = {0, 1, 2, 5, 11, 11, 12, 13, 14, 7, 6, 8, 3, 4, 9, 10, 10, 15, 15, 17, 1000, 15, 2, 7, 9, 9};
    private static final int[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 17, 1, 9, 17, 2, 9, 14, 14};
    private static final DateFormat.Field[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD_ID = {DateFormat.Field.ERA, DateFormat.Field.YEAR, DateFormat.Field.MONTH, DateFormat.Field.DAY_OF_MONTH, DateFormat.Field.HOUR_OF_DAY1, DateFormat.Field.HOUR_OF_DAY0, DateFormat.Field.MINUTE, DateFormat.Field.SECOND, DateFormat.Field.MILLISECOND, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.DAY_OF_YEAR, DateFormat.Field.DAY_OF_WEEK_IN_MONTH, DateFormat.Field.WEEK_OF_YEAR, DateFormat.Field.WEEK_OF_MONTH, DateFormat.Field.AM_PM, DateFormat.Field.HOUR1, DateFormat.Field.HOUR0, DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE, DateFormat.Field.YEAR, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.TIME_ZONE, DateFormat.Field.MONTH, DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.AM_PM, DateFormat.Field.AM_PM};
    private static final EnumSet<TimeZoneNames.NameType> NAME_TYPES = EnumSet.of(TimeZoneNames.NameType.LONG_GENERIC, TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_GENERIC, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT);
    private static final Set<TimeZoneNames.NameType> DST_NAME_TYPES = Collections.unmodifiableSet(EnumSet.of(TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_DAYLIGHT));

    public SimpleDateFormat() {
        this(3, 3, Locale.getDefault(Locale.Category.FORMAT));
    }

    SimpleDateFormat(int i, int i2, Locale locale) {
        this(getDateTimeFormat(i, i2, locale), locale);
    }

    private static String getDateTimeFormat(int i, int i2, Locale locale) {
        LocaleData localeData = LocaleData.get(locale);
        if (i >= 0 && i2 >= 0) {
            return MessageFormat.format("{0} {1}", localeData.getDateFormat(i2), localeData.getTimeFormat(i));
        }
        if (i >= 0) {
            return localeData.getTimeFormat(i);
        }
        if (i2 >= 0) {
            return localeData.getDateFormat(i2);
        }
        throw new IllegalArgumentException("No date or time style specified");
    }

    public SimpleDateFormat(String str) {
        this(str, Locale.getDefault(Locale.Category.FORMAT));
    }

    public SimpleDateFormat(String str, Locale locale) {
        this.serialVersionOnStream = 1;
        this.minusSign = '-';
        this.hasFollowingMinusSign = $assertionsDisabled;
        if (str == null || locale == null) {
            throw new NullPointerException();
        }
        initializeCalendar(locale);
        this.pattern = str;
        this.formatData = DateFormatSymbols.getInstanceRef(locale);
        this.locale = locale;
        initialize(locale);
    }

    public SimpleDateFormat(String str, DateFormatSymbols dateFormatSymbols) {
        this.serialVersionOnStream = 1;
        this.minusSign = '-';
        this.hasFollowingMinusSign = $assertionsDisabled;
        if (str == null || dateFormatSymbols == null) {
            throw new NullPointerException();
        }
        this.pattern = str;
        this.formatData = (DateFormatSymbols) dateFormatSymbols.clone();
        this.locale = Locale.getDefault(Locale.Category.FORMAT);
        initializeCalendar(this.locale);
        initialize(this.locale);
        this.useDateFormatSymbols = true;
    }

    private void initialize(Locale locale) {
        this.compiledPattern = compile(this.pattern);
        this.numberFormat = cachedNumberFormatData.get(locale);
        if (this.numberFormat == null) {
            this.numberFormat = NumberFormat.getIntegerInstance(locale);
            this.numberFormat.setGroupingUsed($assertionsDisabled);
            cachedNumberFormatData.putIfAbsent(locale, this.numberFormat);
        }
        this.numberFormat = (NumberFormat) this.numberFormat.clone();
        initializeDefaultCentury();
    }

    private void initializeCalendar(Locale locale) {
        if (this.calendar == null) {
            this.calendar = Calendar.getInstance(TimeZone.getDefault(), locale);
        }
    }

    private char[] compile(String str) {
        char cCharAt;
        int length = str.length();
        StringBuilder sb = new StringBuilder(length * 2);
        int i = 0;
        int i2 = -1;
        int i3 = 0;
        int i4 = 0;
        StringBuilder sb2 = null;
        int i5 = 0;
        while (i5 < length) {
            char cCharAt2 = str.charAt(i5);
            if (cCharAt2 == '\'') {
                int i6 = i5 + 1;
                if (i6 < length && (cCharAt = str.charAt(i6)) == '\'') {
                    if (i4 != 0) {
                        encode(i2, i4, sb);
                        i2 = -1;
                        i4 = i;
                    }
                    if (i3 != 0) {
                        sb2.append(cCharAt);
                    } else {
                        sb.append((char) (25600 | cCharAt));
                    }
                    i5 = i6;
                } else if (i3 == 0) {
                    if (i4 != 0) {
                        encode(i2, i4, sb);
                        i2 = -1;
                        i4 = i;
                    }
                    if (sb2 == null) {
                        sb2 = new StringBuilder(length);
                    } else {
                        sb2.setLength(i);
                    }
                    i3 = 1;
                } else {
                    int length2 = sb2.length();
                    if (length2 == 1) {
                        char cCharAt3 = sb2.charAt(i);
                        if (cCharAt3 < 128) {
                            sb.append((char) (cCharAt3 | 25600));
                        } else {
                            sb.append((char) 25857);
                            sb.append(cCharAt3);
                        }
                    } else {
                        encode(TAG_QUOTE_CHARS, length2, sb);
                        sb.append((CharSequence) sb2);
                    }
                    i3 = i;
                }
            } else if (i3 != 0) {
                sb2.append(cCharAt2);
            } else if ((cCharAt2 < 'a' || cCharAt2 > 'z') && (cCharAt2 < 'A' || cCharAt2 > 'Z')) {
                if (i4 != 0) {
                    encode(i2, i4, sb);
                    i2 = -1;
                    i4 = 0;
                }
                if (cCharAt2 < 128) {
                    sb.append((char) (25600 | cCharAt2));
                } else {
                    int i7 = i5 + 1;
                    while (i7 < length) {
                        char cCharAt4 = str.charAt(i7);
                        if (cCharAt4 != '\'' && (cCharAt4 < 'a' || cCharAt4 > 'z')) {
                            if (cCharAt4 >= 'A' && cCharAt4 <= 'Z') {
                                break;
                            }
                            i7++;
                        } else {
                            break;
                        }
                    }
                    sb.append((char) (25856 | (i7 - i5)));
                    while (i5 < i7) {
                        sb.append(str.charAt(i5));
                        i5++;
                    }
                    i5--;
                }
            } else {
                int iIndexOf = "GyMdkHmsSEDFwWahKzZYuXLcbB".indexOf(cCharAt2);
                if (iIndexOf == -1) {
                    throw new IllegalArgumentException("Illegal pattern character '" + cCharAt2 + "'");
                }
                if (i2 == -1 || i2 == iIndexOf) {
                    i4++;
                    i2 = iIndexOf;
                } else {
                    encode(i2, i4, sb);
                    i2 = iIndexOf;
                    i4 = 1;
                }
            }
            i5++;
            i = 0;
        }
        if (i3 != 0) {
            throw new IllegalArgumentException("Unterminated quote");
        }
        if (i4 != 0) {
            encode(i2, i4, sb);
        }
        int length3 = sb.length();
        char[] cArr = new char[length3];
        sb.getChars(0, length3, cArr, 0);
        return cArr;
    }

    private static void encode(int i, int i2, StringBuilder sb) {
        if (i == 21 && i2 >= 4) {
            throw new IllegalArgumentException("invalid ISO 8601 format: length=" + i2);
        }
        if (i2 >= 255) {
            sb.append((char) ((i << 8) | 255));
            sb.append((char) (i2 >>> 16));
            sb.append((char) (65535 & i2));
            return;
        }
        sb.append((char) ((i << 8) | i2));
    }

    private void initializeDefaultCentury() {
        this.calendar.setTimeInMillis(System.currentTimeMillis());
        this.calendar.add(1, -80);
        parseAmbiguousDatesAsAfter(this.calendar.getTime());
    }

    private void parseAmbiguousDatesAsAfter(Date date) {
        this.defaultCenturyStart = date;
        this.calendar.setTime(date);
        this.defaultCenturyStartYear = this.calendar.get(1);
    }

    public void set2DigitYearStart(Date date) {
        parseAmbiguousDatesAsAfter(new Date(date.getTime()));
    }

    public Date get2DigitYearStart() {
        return (Date) this.defaultCenturyStart.clone();
    }

    @Override
    public StringBuffer format(Date date, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        fieldPosition.endIndex = 0;
        fieldPosition.beginIndex = 0;
        return format(date, stringBuffer, fieldPosition.getFieldDelegate());
    }

    private StringBuffer format(Date date, StringBuffer stringBuffer, Format.FieldDelegate fieldDelegate) {
        int i;
        this.calendar.setTime(date);
        boolean zUseDateFormatSymbols = useDateFormatSymbols();
        int i2 = 0;
        while (i2 < this.compiledPattern.length) {
            int i3 = this.compiledPattern[i2] >>> '\b';
            int i4 = i2 + 1;
            int i5 = this.compiledPattern[i2] & 255;
            if (i5 == 255) {
                int i6 = i4 + 1;
                i = (this.compiledPattern[i4] << 16) | this.compiledPattern[i6];
                i2 = i6 + 1;
            } else {
                i = i5;
                i2 = i4;
            }
            switch (i3) {
                case TAG_QUOTE_ASCII_CHAR:
                    stringBuffer.append((char) i);
                    break;
                case TAG_QUOTE_CHARS:
                    stringBuffer.append(this.compiledPattern, i2, i);
                    i2 += i;
                    break;
                default:
                    subFormat(i3, i, fieldDelegate, stringBuffer, zUseDateFormatSymbols);
                    break;
            }
        }
        return stringBuffer;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        StringBuffer stringBuffer = new StringBuffer();
        CharacterIteratorFieldDelegate characterIteratorFieldDelegate = new CharacterIteratorFieldDelegate();
        if (obj instanceof Date) {
            format((Date) obj, stringBuffer, characterIteratorFieldDelegate);
        } else if (obj instanceof Number) {
            format(new Date(((Number) obj).longValue()), stringBuffer, characterIteratorFieldDelegate);
        } else {
            if (obj == null) {
                throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
            }
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        return characterIteratorFieldDelegate.getIterator(stringBuffer.toString());
    }

    private void subFormat(int i, int i2, Format.FieldDelegate fieldDelegate, StringBuffer stringBuffer, boolean z) {
        int iSODayOfWeek;
        int i3;
        int i4;
        String displayName;
        int i5 = i2;
        int length = stringBuffer.length();
        int i6 = PATTERN_INDEX_TO_CALENDAR_FIELD[i];
        if (i6 != 17) {
            iSODayOfWeek = i6 == 1000 ? CalendarBuilder.toISODayOfWeek(this.calendar.get(7)) : this.calendar.get(i6);
        } else {
            if (!this.calendar.isWeekDateSupported()) {
                i6 = PATTERN_INDEX_TO_CALENDAR_FIELD[1];
                i4 = this.calendar.get(i6);
                i3 = 1;
                int i7 = i5 < 4 ? 2 : 1;
                displayName = (!z || i6 == 1000) ? null : this.calendar.getDisplayName(i6, i7, this.locale);
                boolean z2 = $assertionsDisabled;
                switch (i3) {
                    case 0:
                        if (z) {
                            String[] eras = this.formatData.getEras();
                            if (i4 < eras.length) {
                                displayName = eras[i4];
                            }
                        }
                        if (displayName == null) {
                            displayName = "";
                        }
                        break;
                    case 1:
                    case 19:
                        if (!(this.calendar instanceof GregorianCalendar)) {
                            if (displayName == null) {
                                if (i7 == 2) {
                                    i5 = 1;
                                }
                                zeroPaddingNumber(i4, i5, Integer.MAX_VALUE, stringBuffer);
                            }
                        } else if (i5 == 2) {
                            zeroPaddingNumber(i4, 2, 2, stringBuffer);
                        } else {
                            zeroPaddingNumber(i4, i5, Integer.MAX_VALUE, stringBuffer);
                        }
                        break;
                    case 2:
                        if (z) {
                            displayName = formatMonth(i5, i4, Integer.MAX_VALUE, stringBuffer, z, $assertionsDisabled);
                        }
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 16:
                    case 20:
                    default:
                        if (displayName == null) {
                            zeroPaddingNumber(i4, i5, Integer.MAX_VALUE, stringBuffer);
                        }
                        break;
                    case 4:
                        if (displayName == null) {
                            if (i4 != 0) {
                                zeroPaddingNumber(i4, i5, Integer.MAX_VALUE, stringBuffer);
                            } else {
                                zeroPaddingNumber(this.calendar.getMaximum(11) + 1, i5, Integer.MAX_VALUE, stringBuffer);
                            }
                        }
                        break;
                    case 8:
                        if (displayName == null) {
                            zeroPaddingNumber((int) ((((double) i4) / 1000.0d) * Math.pow(10.0d, i5)), i5, i5, stringBuffer);
                        }
                        break;
                    case 9:
                        if (displayName == null) {
                            displayName = formatWeekday(i5, i4, z, $assertionsDisabled);
                        }
                        break;
                    case 14:
                        if (z) {
                            displayName = this.formatData.getAmPmStrings()[i4];
                        }
                        break;
                    case 15:
                        if (displayName == null) {
                            if (i4 != 0) {
                                zeroPaddingNumber(i4, i5, Integer.MAX_VALUE, stringBuffer);
                            } else {
                                zeroPaddingNumber(this.calendar.getLeastMaximum(10) + 1, i5, Integer.MAX_VALUE, stringBuffer);
                            }
                        }
                        break;
                    case 17:
                        if (displayName == null) {
                            TimeZone timeZone = this.calendar.getTimeZone();
                            boolean z3 = this.calendar.get(16) != 0;
                            String displayName2 = this.formatData.isZoneStringsSet ? libcore.icu.TimeZoneNames.getDisplayName(this.formatData.getZoneStringsWrapper(), timeZone.getID(), z3, i5 >= 4 ? 1 : 0) : getTimeZoneNames().getDisplayName(android.icu.util.TimeZone.getCanonicalID(timeZone.getID()), i5 < 4 ? z3 ? TimeZoneNames.NameType.SHORT_DAYLIGHT : TimeZoneNames.NameType.SHORT_STANDARD : z3 ? TimeZoneNames.NameType.LONG_DAYLIGHT : TimeZoneNames.NameType.LONG_STANDARD, this.calendar.getTimeInMillis());
                            if (displayName2 == null) {
                                stringBuffer.append(TimeZone.createGmtOffsetString(true, true, this.calendar.get(15) + this.calendar.get(16)));
                            } else {
                                stringBuffer.append(displayName2);
                            }
                        }
                        break;
                    case 18:
                        int i8 = this.calendar.get(15) + this.calendar.get(16);
                        boolean z4 = i5 >= 4;
                        if (i5 == 4) {
                            z2 = true;
                        }
                        stringBuffer.append(TimeZone.createGmtOffsetString(z2, z4, i8));
                        break;
                    case 21:
                        int i9 = this.calendar.get(15) + this.calendar.get(16);
                        if (i9 != 0) {
                            int i10 = i9 / MILLIS_PER_MINUTE;
                            if (i10 >= 0) {
                                stringBuffer.append('+');
                            } else {
                                stringBuffer.append('-');
                                i10 = -i10;
                            }
                            CalendarUtils.sprintf0d(stringBuffer, i10 / 60, 2);
                            if (i5 != 1) {
                                if (i5 == 3) {
                                    stringBuffer.append(':');
                                }
                                CalendarUtils.sprintf0d(stringBuffer, i10 % 60, 2);
                            }
                        } else {
                            stringBuffer.append('Z');
                        }
                        break;
                    case 22:
                        if (z) {
                            displayName = formatMonth(i5, i4, Integer.MAX_VALUE, stringBuffer, z, true);
                        }
                        break;
                    case 23:
                        if (displayName == null) {
                            displayName = formatWeekday(i5, i4, z, true);
                        }
                        break;
                    case 24:
                    case 25:
                        displayName = "";
                        break;
                }
                if (displayName != null) {
                    stringBuffer.append(displayName);
                }
                int i11 = PATTERN_INDEX_TO_DATE_FORMAT_FIELD[i3];
                DateFormat.Field field = PATTERN_INDEX_TO_DATE_FORMAT_FIELD_ID[i3];
                fieldDelegate.formatted(i11, field, field, length, stringBuffer.length(), stringBuffer);
            }
            iSODayOfWeek = this.calendar.getWeekYear();
        }
        i4 = iSODayOfWeek;
        i3 = i;
        if (i5 < 4) {
        }
        if (z) {
        }
        boolean z22 = $assertionsDisabled;
        switch (i3) {
        }
        if (displayName != null) {
        }
        int i112 = PATTERN_INDEX_TO_DATE_FORMAT_FIELD[i3];
        DateFormat.Field field2 = PATTERN_INDEX_TO_DATE_FORMAT_FIELD_ID[i3];
        fieldDelegate.formatted(i112, field2, field2, length, stringBuffer.length(), stringBuffer);
    }

    private String formatWeekday(int i, int i2, boolean z, boolean z2) {
        String[] shortStandAloneWeekdays;
        if (z) {
            if (i == 4) {
                shortStandAloneWeekdays = z2 ? this.formatData.getStandAloneWeekdays() : this.formatData.getWeekdays();
            } else if (i == 5) {
                shortStandAloneWeekdays = z2 ? this.formatData.getTinyStandAloneWeekdays() : this.formatData.getTinyWeekdays();
            } else {
                shortStandAloneWeekdays = z2 ? this.formatData.getShortStandAloneWeekdays() : this.formatData.getShortWeekdays();
            }
            return shortStandAloneWeekdays[i2];
        }
        return null;
    }

    private String formatMonth(int i, int i2, int i3, StringBuffer stringBuffer, boolean z, boolean z2) {
        String[] shortStandAloneMonths;
        String str = null;
        if (z) {
            if (i == 4) {
                shortStandAloneMonths = z2 ? this.formatData.getStandAloneMonths() : this.formatData.getMonths();
            } else if (i == 5) {
                shortStandAloneMonths = z2 ? this.formatData.getTinyStandAloneMonths() : this.formatData.getTinyMonths();
            } else if (i == 3) {
                shortStandAloneMonths = z2 ? this.formatData.getShortStandAloneMonths() : this.formatData.getShortMonths();
            } else {
                shortStandAloneMonths = null;
            }
            if (shortStandAloneMonths != null) {
                str = shortStandAloneMonths[i2];
            }
        } else if (i < 3) {
        }
        if (str == null) {
            zeroPaddingNumber(i2 + 1, i, i3, stringBuffer);
        }
        return str;
    }

    private void zeroPaddingNumber(int i, int i2, int i3, StringBuffer stringBuffer) {
        try {
            if (this.zeroDigit == 0) {
                this.zeroDigit = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getZeroDigit();
            }
            if (i >= 0) {
                if (i < TAG_QUOTE_ASCII_CHAR && i2 >= 1 && i2 <= 2) {
                    if (i < 10) {
                        if (i2 == 2) {
                            stringBuffer.append(this.zeroDigit);
                        }
                        stringBuffer.append((char) (this.zeroDigit + i));
                        return;
                    } else {
                        stringBuffer.append((char) (this.zeroDigit + (i / 10)));
                        stringBuffer.append((char) (this.zeroDigit + (i % 10)));
                        return;
                    }
                }
                if (i >= 1000 && i < 10000) {
                    if (i2 == 4) {
                        stringBuffer.append((char) (this.zeroDigit + (i / 1000)));
                        int i4 = i % 1000;
                        try {
                            stringBuffer.append((char) (this.zeroDigit + (i4 / TAG_QUOTE_ASCII_CHAR)));
                            int i5 = i4 % TAG_QUOTE_ASCII_CHAR;
                            stringBuffer.append((char) (this.zeroDigit + (i5 / 10)));
                            stringBuffer.append((char) (this.zeroDigit + (i5 % 10)));
                            return;
                        } catch (Exception e) {
                            i = i4;
                        }
                    } else if (i2 == 2 && i3 == 2) {
                        zeroPaddingNumber(i % TAG_QUOTE_ASCII_CHAR, 2, 2, stringBuffer);
                        return;
                    }
                }
            }
        } catch (Exception e2) {
        }
        this.numberFormat.setMinimumIntegerDigits(i2);
        this.numberFormat.setMaximumIntegerDigits(i3);
        this.numberFormat.format(i, stringBuffer, DontCareFieldPosition.INSTANCE);
    }

    @Override
    public Date parse(String str, ParsePosition parsePosition) {
        TimeZone timeZone = getTimeZone();
        try {
            return parseInternal(str, parsePosition);
        } finally {
            setTimeZone(timeZone);
        }
    }

    private Date parseInternal(String str, ParsePosition parsePosition) {
        int i;
        CalendarBuilder calendarBuilder;
        boolean[] zArr;
        boolean z;
        boolean z2;
        int i2;
        checkNegativeNumberExpression();
        int i3 = parsePosition.index;
        int length = str.length();
        boolean z3 = true;
        boolean[] zArr2 = {$assertionsDisabled};
        CalendarBuilder calendarBuilder2 = new CalendarBuilder();
        int i4 = i3;
        int i5 = 0;
        while (i5 < this.compiledPattern.length) {
            int i6 = this.compiledPattern[i5] >>> '\b';
            int i7 = i5 + 1;
            int i8 = this.compiledPattern[i5] & 255;
            if (i8 == 255) {
                int i9 = i7 + 1;
                i = i9 + 1;
                i8 = (this.compiledPattern[i7] << 16) | this.compiledPattern[i9];
            } else {
                i = i7;
            }
            int i10 = i8;
            switch (i6) {
                case TAG_QUOTE_ASCII_CHAR:
                    if (i4 >= length || str.charAt(i4) != ((char) i10)) {
                        parsePosition.index = i3;
                        parsePosition.errorIndex = i4;
                    } else {
                        i4++;
                        i5 = i;
                        calendarBuilder = calendarBuilder2;
                        zArr = zArr2;
                        zArr2 = zArr;
                        calendarBuilder2 = calendarBuilder;
                        z3 = true;
                    }
                    break;
                case TAG_QUOTE_CHARS:
                    while (true) {
                        int i11 = i10 - 1;
                        if (i10 <= 0) {
                            break;
                        } else if (i4 < length) {
                            int i12 = i + 1;
                            if (str.charAt(i4) == this.compiledPattern[i]) {
                                i4++;
                                i10 = i11;
                                i = i12;
                            }
                        }
                    }
                    parsePosition.index = i3;
                    parsePosition.errorIndex = i4;
                    break;
                default:
                    if (i < this.compiledPattern.length) {
                        int i13 = this.compiledPattern[i] >>> '\b';
                        z2 = (i13 == TAG_QUOTE_ASCII_CHAR || i13 == TAG_QUOTE_CHARS) ? false : z3;
                        if (this.hasFollowingMinusSign && (i13 == TAG_QUOTE_ASCII_CHAR || i13 == TAG_QUOTE_CHARS)) {
                            if (i13 == TAG_QUOTE_ASCII_CHAR) {
                                i2 = this.compiledPattern[i] & 255;
                            } else {
                                i2 = this.compiledPattern[i + 1];
                            }
                            if (i2 == this.minusSign) {
                                z = true;
                            }
                        } else {
                            z = false;
                        }
                        zArr2 = zArr;
                        calendarBuilder2 = calendarBuilder;
                        z3 = true;
                    } else {
                        z = false;
                        z2 = false;
                    }
                    int i14 = i;
                    calendarBuilder = calendarBuilder2;
                    boolean z4 = z;
                    zArr = zArr2;
                    int iSubParse = subParse(str, i4, i6, i10, z2, zArr2, parsePosition, z4, calendarBuilder);
                    if (iSubParse < 0) {
                        parsePosition.index = i3;
                    } else {
                        i4 = iSubParse;
                        i5 = i14;
                        zArr2 = zArr;
                        calendarBuilder2 = calendarBuilder;
                        z3 = true;
                    }
                    break;
            }
            return null;
        }
        CalendarBuilder calendarBuilder3 = calendarBuilder2;
        boolean[] zArr3 = zArr2;
        parsePosition.index = i4;
        try {
            Date time = calendarBuilder3.establish(this.calendar).getTime();
            if (zArr3[0] && time.before(this.defaultCenturyStart)) {
                return calendarBuilder3.addYear(TAG_QUOTE_ASCII_CHAR).establish(this.calendar).getTime();
            }
            return time;
        } catch (IllegalArgumentException e) {
            parsePosition.errorIndex = i4;
            parsePosition.index = i3;
            return null;
        }
    }

    private int matchString(String str, int i, int i2, String[] strArr, CalendarBuilder calendarBuilder) {
        int i3;
        int length = strArr.length;
        if (i2 != 7) {
            i3 = 0;
        } else {
            i3 = 1;
        }
        int i4 = 0;
        int i5 = -1;
        for (int i6 = i3; i6 < length; i6++) {
            int length2 = strArr[i6].length();
            if (length2 > i4 && str.regionMatches(true, i, strArr[i6], 0, length2)) {
                i5 = i6;
                i4 = length2;
            }
            int i7 = length2 - 1;
            if (strArr[i6].charAt(i7) == '.' && i7 > i4 && str.regionMatches(true, i, strArr[i6], 0, i7)) {
                i5 = i6;
                i4 = i7;
            }
        }
        if (i5 >= 0) {
            calendarBuilder.set(i2, i5);
            return i + i4;
        }
        return -i;
    }

    private int matchString(String str, int i, int i2, Map<String, Integer> map, CalendarBuilder calendarBuilder) {
        if (map != null) {
            String str2 = null;
            for (String str3 : map.keySet()) {
                int length = str3.length();
                if (str2 == null || length > str2.length()) {
                    if (str.regionMatches(true, i, str3, 0, length)) {
                        str2 = str3;
                    }
                }
            }
            if (str2 != null) {
                calendarBuilder.set(i2, map.get(str2).intValue());
                return i + str2.length();
            }
        }
        return -i;
    }

    private int matchZoneString(String str, int i, String[] strArr) {
        for (int i2 = 1; i2 <= 4; i2++) {
            String str2 = strArr[i2];
            if (str.regionMatches(true, i, str2, 0, str2.length())) {
                return i2;
            }
        }
        return -1;
    }

    private int subParseZoneString(String str, int i, CalendarBuilder calendarBuilder) {
        if (this.formatData.isZoneStringsSet) {
            return subParseZoneStringFromSymbols(str, i, calendarBuilder);
        }
        return subParseZoneStringFromICU(str, i, calendarBuilder);
    }

    private TimeZoneNames getTimeZoneNames() {
        if (this.timeZoneNames == null) {
            this.timeZoneNames = TimeZoneNames.getInstance(this.locale);
        }
        return this.timeZoneNames;
    }

    private int subParseZoneStringFromICU(String str, int i, CalendarBuilder calendarBuilder) {
        TimeZoneNames.MatchInfo matchInfo;
        String canonicalID = android.icu.util.TimeZone.getCanonicalID(getTimeZone().getID());
        TimeZoneNames timeZoneNames = getTimeZoneNames();
        Iterator it = timeZoneNames.find(str, i, NAME_TYPES).iterator();
        TimeZoneNames.MatchInfo matchInfo2 = null;
        Set<String> availableMetaZoneIDs = null;
        while (true) {
            if (it.hasNext()) {
                matchInfo = (TimeZoneNames.MatchInfo) it.next();
                if (matchInfo2 != null && matchInfo2.matchLength() >= matchInfo.matchLength()) {
                    if (matchInfo2.matchLength() == matchInfo.matchLength()) {
                        if (canonicalID.equals(matchInfo.tzID())) {
                            break;
                        }
                        if (matchInfo.mzID() == null) {
                            continue;
                        } else {
                            if (availableMetaZoneIDs == null) {
                                availableMetaZoneIDs = timeZoneNames.getAvailableMetaZoneIDs(canonicalID);
                            }
                            if (availableMetaZoneIDs.contains(matchInfo.mzID())) {
                                break;
                            }
                        }
                    } else {
                        continue;
                    }
                } else {
                    matchInfo2 = matchInfo;
                }
            } else {
                matchInfo = matchInfo2;
                break;
            }
        }
        if (matchInfo == null) {
            return -i;
        }
        String strTzID = matchInfo.tzID();
        String referenceZoneID = strTzID;
        if (strTzID == null) {
            if (availableMetaZoneIDs == null) {
                availableMetaZoneIDs = timeZoneNames.getAvailableMetaZoneIDs(canonicalID);
            }
            if (!availableMetaZoneIDs.contains(matchInfo.mzID())) {
                ULocale uLocaleForLocale = ULocale.forLocale(this.locale);
                String country = uLocaleForLocale.getCountry();
                if (country.length() == 0) {
                    country = ULocale.addLikelySubtags(uLocaleForLocale).getCountry();
                }
                referenceZoneID = timeZoneNames.getReferenceZoneID(matchInfo.mzID(), country);
            } else {
                referenceZoneID = canonicalID;
            }
        }
        TimeZone timeZone = TimeZone.getTimeZone(referenceZoneID);
        if (!canonicalID.equals(referenceZoneID)) {
            setTimeZone(timeZone);
        }
        boolean zContains = DST_NAME_TYPES.contains(matchInfo.nameType());
        int dSTSavings = zContains ? timeZone.getDSTSavings() : 0;
        if (!zContains || dSTSavings != 0) {
            calendarBuilder.clear(15).set(16, dSTSavings);
        }
        return matchInfo.matchLength() + i;
    }

    private int subParseZoneStringFromSymbols(String str, int i, CalendarBuilder calendarBuilder) {
        String[] strArr;
        boolean zEqualsIgnoreCase;
        int iMatchZoneString;
        String[] strArr2;
        int iMatchZoneString2;
        int zoneIndex;
        boolean zEqualsIgnoreCase2;
        TimeZone timeZone = getTimeZone();
        int zoneIndex2 = this.formatData.getZoneIndex(timeZone.getID());
        String[][] zoneStringsWrapper = this.formatData.getZoneStringsWrapper();
        TimeZone timeZone2 = null;
        if (zoneIndex2 != -1) {
            String[] strArr3 = zoneStringsWrapper[zoneIndex2];
            iMatchZoneString = matchZoneString(str, i, strArr3);
            if (iMatchZoneString > 0) {
                if (iMatchZoneString <= 2) {
                    zEqualsIgnoreCase2 = strArr3[iMatchZoneString].equalsIgnoreCase(strArr3[iMatchZoneString + 2]);
                } else {
                    zEqualsIgnoreCase2 = false;
                }
                TimeZone timeZone3 = TimeZone.getTimeZone(strArr3[0]);
                strArr = strArr3;
                zEqualsIgnoreCase = zEqualsIgnoreCase2;
                timeZone2 = timeZone3;
            } else {
                strArr = strArr3;
                zEqualsIgnoreCase = false;
            }
        } else {
            strArr = null;
            zEqualsIgnoreCase = false;
            iMatchZoneString = 0;
        }
        if (timeZone2 == null && (zoneIndex = this.formatData.getZoneIndex(TimeZone.getDefault().getID())) != -1 && (iMatchZoneString = matchZoneString(str, i, (strArr = zoneStringsWrapper[zoneIndex]))) > 0) {
            if (iMatchZoneString <= 2) {
                zEqualsIgnoreCase = strArr[iMatchZoneString].equalsIgnoreCase(strArr[iMatchZoneString + 2]);
            }
            timeZone2 = TimeZone.getTimeZone(strArr[0]);
        }
        if (timeZone2 == null) {
            int length = zoneStringsWrapper.length;
            strArr2 = strArr;
            iMatchZoneString2 = iMatchZoneString;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                strArr2 = zoneStringsWrapper[i2];
                iMatchZoneString2 = matchZoneString(str, i, strArr2);
                if (iMatchZoneString2 <= 0) {
                    i2++;
                } else {
                    if (iMatchZoneString2 <= 2) {
                        zEqualsIgnoreCase = strArr2[iMatchZoneString2].equalsIgnoreCase(strArr2[iMatchZoneString2 + 2]);
                    }
                    timeZone2 = TimeZone.getTimeZone(strArr2[0]);
                }
            }
        } else {
            strArr2 = strArr;
            iMatchZoneString2 = iMatchZoneString;
        }
        if (timeZone2 != null) {
            if (!timeZone2.equals(timeZone)) {
                setTimeZone(timeZone2);
            }
            int dSTSavings = iMatchZoneString2 >= 3 ? timeZone2.getDSTSavings() : 0;
            if (!zEqualsIgnoreCase && (iMatchZoneString2 < 3 || dSTSavings != 0)) {
                calendarBuilder.clear(15).set(16, dSTSavings);
            }
            return i + strArr2[iMatchZoneString2].length();
        }
        return -i;
    }

    private int subParseNumericZone(String str, int i, int i2, int i3, boolean z, CalendarBuilder calendarBuilder) {
        int i4;
        char cCharAt;
        int i5 = i + 1;
        try {
            char cCharAt2 = str.charAt(i);
            if (isDigit(cCharAt2)) {
                int i6 = cCharAt2 - 48;
                int i7 = i5 + 1;
                try {
                    char cCharAt3 = str.charAt(i5);
                    if (isDigit(cCharAt3)) {
                        i6 = (i6 * 10) + (cCharAt3 - 48);
                    } else {
                        i7--;
                    }
                    i5 = i7;
                    if (i6 <= 23) {
                        if (i3 != 1) {
                            int i8 = i5 + 1;
                            try {
                                char cCharAt4 = str.charAt(i5);
                                if (cCharAt4 == ':') {
                                    i5 = i8 + 1;
                                    cCharAt = str.charAt(i8);
                                } else if (!z) {
                                    i5 = i8;
                                    cCharAt = cCharAt4;
                                } else {
                                    i5 = i8;
                                }
                                if (isDigit(cCharAt)) {
                                    int i9 = cCharAt - 48;
                                    int i10 = i5 + 1;
                                    try {
                                        char cCharAt5 = str.charAt(i5);
                                        if (isDigit(cCharAt5)) {
                                            i4 = (cCharAt5 - 48) + (i9 * 10);
                                            if (i4 <= 59) {
                                                i5 = i10;
                                            }
                                        }
                                        i5 = i10;
                                    } catch (IndexOutOfBoundsException e) {
                                        i5 = i10;
                                    }
                                }
                            } catch (IndexOutOfBoundsException e2) {
                                i5 = i8;
                            }
                        } else {
                            i4 = 0;
                        }
                        calendarBuilder.set(15, (i4 + (i6 * 60)) * MILLIS_PER_MINUTE * i2).set(16, 0);
                        return i5;
                    }
                } catch (IndexOutOfBoundsException e3) {
                    i5 = i7;
                }
            }
        } catch (IndexOutOfBoundsException e4) {
        }
        return 1 - i5;
    }

    private boolean isDigit(char c) {
        if (c < '0' || c > '9') {
            return $assertionsDisabled;
        }
        return true;
    }

    private int subParse(String str, int i, int i2, int i3, boolean z, boolean[] zArr, ParsePosition parsePosition, boolean z2, CalendarBuilder calendarBuilder) {
        Number number;
        int i4;
        int i5;
        int iMatchString;
        int i6;
        Number number2;
        int iIntValue;
        int i7;
        ParsePosition parsePosition2 = new ParsePosition(0);
        parsePosition2.index = i;
        int i8 = 1;
        int i9 = i2;
        if (i9 == 19 && !this.calendar.isWeekDateSupported()) {
            i9 = 1;
        }
        int i10 = PATTERN_INDEX_TO_CALENDAR_FIELD[i9];
        while (parsePosition2.index < str.length()) {
            char cCharAt = str.charAt(parsePosition2.index);
            if (cCharAt == ' ' || cCharAt == '\t') {
                parsePosition2.index++;
                i8 = 1;
            } else {
                if (i9 == 4 || i9 == 15 || ((i9 == 2 && i3 <= 2) || i9 == i8 || i9 == 19)) {
                    if (z) {
                        int i11 = i + i3;
                        if (i11 <= str.length()) {
                            number = this.numberFormat.parse(str.substring(0, i11), parsePosition2);
                        }
                    } else {
                        number = this.numberFormat.parse(str, parsePosition2);
                    }
                    if (number == null) {
                        if (i9 == i8) {
                        }
                    } else {
                        int iIntValue2 = number.intValue();
                        if (z2 && iIntValue2 < 0 && ((parsePosition2.index < str.length() && str.charAt(parsePosition2.index) != this.minusSign) || (parsePosition2.index == str.length() && str.charAt(parsePosition2.index - i8) == this.minusSign))) {
                            iIntValue2 = -iIntValue2;
                            parsePosition2.index -= i8;
                        }
                        i4 = iIntValue2;
                        boolean zUseDateFormatSymbols = useDateFormatSymbols();
                        switch (i9) {
                            case 0:
                                if (zUseDateFormatSymbols) {
                                    int iMatchString2 = matchString(str, i, 0, this.formatData.getEras(), calendarBuilder);
                                    if (iMatchString2 > 0) {
                                        return iMatchString2;
                                    }
                                } else {
                                    int iMatchString3 = matchString(str, i, i10, this.calendar.getDisplayNames(i10, 0, this.locale), calendarBuilder);
                                    if (iMatchString3 > 0) {
                                        return iMatchString3;
                                    }
                                }
                            case 1:
                            case 19:
                                if (!(this.calendar instanceof GregorianCalendar)) {
                                    Map<String, Integer> displayNames = this.calendar.getDisplayNames(i10, i3 >= 4 ? 2 : 1, this.locale);
                                    if (displayNames != null && (iMatchString = matchString(str, i, i10, displayNames, calendarBuilder)) > 0) {
                                        return iMatchString;
                                    }
                                    calendarBuilder.set(i10, i4);
                                    return parsePosition2.index;
                                }
                                if (i3 <= 2 && parsePosition2.index - i == 2 && Character.isDigit(str.charAt(i)) && Character.isDigit(str.charAt(i + 1))) {
                                    int i12 = this.defaultCenturyStartYear % TAG_QUOTE_ASCII_CHAR;
                                    zArr[0] = i4 == i12 ? true : $assertionsDisabled;
                                    i4 += ((this.defaultCenturyStartYear / TAG_QUOTE_ASCII_CHAR) * TAG_QUOTE_ASCII_CHAR) + (i4 < i12 ? TAG_QUOTE_ASCII_CHAR : 0);
                                }
                                calendarBuilder.set(i10, i4);
                                return parsePosition2.index;
                            case 2:
                                int month = parseMonth(str, i3, i4, i, i10, parsePosition2, zUseDateFormatSymbols, $assertionsDisabled, calendarBuilder);
                                if (month > 0) {
                                    return month;
                                }
                                break;
                            case 3:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 16:
                            case 20:
                            default:
                                int index = parsePosition2.getIndex();
                                if (z) {
                                    int i13 = i + i3;
                                    if (i13 <= str.length()) {
                                        number2 = this.numberFormat.parse(str.substring(0, i13), parsePosition2);
                                    }
                                } else {
                                    number2 = this.numberFormat.parse(str, parsePosition2);
                                }
                                if (number2 != null) {
                                    if (i9 == 8) {
                                        iIntValue = (int) ((number2.doubleValue() / Math.pow(10.0d, parsePosition2.getIndex() - index)) * 1000.0d);
                                    } else {
                                        iIntValue = number2.intValue();
                                    }
                                    if (z2 && iIntValue < 0) {
                                        if (parsePosition2.index >= str.length() || str.charAt(parsePosition2.index) == this.minusSign) {
                                            if (parsePosition2.index == str.length()) {
                                                i7 = 1;
                                                if (str.charAt(parsePosition2.index - 1) == this.minusSign) {
                                                }
                                            }
                                        } else {
                                            i7 = 1;
                                        }
                                        iIntValue = -iIntValue;
                                        parsePosition2.index -= i7;
                                    }
                                    calendarBuilder.set(i10, iIntValue);
                                    return parsePosition2.index;
                                }
                                break;
                            case 4:
                                if (isLenient() || (i4 >= 1 && i4 <= 24)) {
                                    if (i4 == this.calendar.getMaximum(11) + 1) {
                                        i4 = 0;
                                    }
                                    calendarBuilder.set(11, i4);
                                    return parsePosition2.index;
                                }
                                break;
                            case 9:
                                int weekday = parseWeekday(str, i, i10, zUseDateFormatSymbols, $assertionsDisabled, calendarBuilder);
                                if (weekday > 0) {
                                    return weekday;
                                }
                                break;
                            case 14:
                                if (zUseDateFormatSymbols) {
                                    int iMatchString4 = matchString(str, i, 9, this.formatData.getAmPmStrings(), calendarBuilder);
                                    if (iMatchString4 > 0) {
                                        return iMatchString4;
                                    }
                                } else {
                                    int iMatchString5 = matchString(str, i, i10, this.calendar.getDisplayNames(i10, 0, this.locale), calendarBuilder);
                                    if (iMatchString5 > 0) {
                                        return iMatchString5;
                                    }
                                }
                            case 15:
                                if (isLenient() || (i4 >= 1 && i4 <= 12)) {
                                    if (i4 == this.calendar.getLeastMaximum(10) + 1) {
                                        i4 = 0;
                                    }
                                    calendarBuilder.set(10, i4);
                                    return parsePosition2.index;
                                }
                                break;
                            case 17:
                            case 18:
                                try {
                                    char cCharAt2 = str.charAt(parsePosition2.index);
                                    int i14 = cCharAt2 == '+' ? i8 : cCharAt2 == '-' ? -1 : 0;
                                    if (i14 == 0) {
                                        if ((cCharAt2 != 'G' && cCharAt2 != 'g') || str.length() - i < GMT.length() || !str.regionMatches(true, i, GMT, 0, GMT.length())) {
                                            int iSubParseZoneString = subParseZoneString(str, parsePosition2.index, calendarBuilder);
                                            if (iSubParseZoneString > 0) {
                                                return iSubParseZoneString;
                                            }
                                            parsePosition2.index = -iSubParseZoneString;
                                            break;
                                        } else {
                                            parsePosition2.index = GMT.length() + i;
                                            if (str.length() - parsePosition2.index > 0) {
                                                char cCharAt3 = str.charAt(parsePosition2.index);
                                                if (cCharAt3 != '+') {
                                                    if (cCharAt3 == '-') {
                                                        i5 = -1;
                                                    }
                                                } else {
                                                    i5 = 1;
                                                }
                                                if (i5 != 0) {
                                                }
                                            } else {
                                                i5 = i14;
                                                if (i5 != 0) {
                                                    calendarBuilder.set(15, 0).set(16, 0);
                                                    return parsePosition2.index;
                                                }
                                                int i15 = parsePosition2.index + 1;
                                                parsePosition2.index = i15;
                                                int iSubParseNumericZone = subParseNumericZone(str, i15, i5, 0, $assertionsDisabled, calendarBuilder);
                                                if (iSubParseNumericZone > 0) {
                                                    return iSubParseNumericZone;
                                                }
                                                parsePosition2.index = -iSubParseNumericZone;
                                                break;
                                            }
                                        }
                                    } else {
                                        int i16 = parsePosition2.index + 1;
                                        parsePosition2.index = i16;
                                        int iSubParseNumericZone2 = subParseNumericZone(str, i16, i14, 0, $assertionsDisabled, calendarBuilder);
                                        if (iSubParseNumericZone2 > 0) {
                                            return iSubParseNumericZone2;
                                        }
                                        parsePosition2.index = -iSubParseNumericZone2;
                                        break;
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    break;
                                }
                                break;
                            case 21:
                                if (str.length() - parsePosition2.index > 0) {
                                    char cCharAt4 = str.charAt(parsePosition2.index);
                                    if (cCharAt4 == 'Z') {
                                        calendarBuilder.set(15, 0).set(16, 0);
                                        int i17 = parsePosition2.index + i8;
                                        parsePosition2.index = i17;
                                        return i17;
                                    }
                                    if (cCharAt4 != '+') {
                                        if (cCharAt4 != '-') {
                                            parsePosition2.index += i8;
                                        } else {
                                            i6 = -1;
                                        }
                                    } else {
                                        i6 = i8;
                                    }
                                    int i18 = parsePosition2.index + 1;
                                    parsePosition2.index = i18;
                                    int iSubParseNumericZone3 = subParseNumericZone(str, i18, i6, i3, i3 == 3 ? i8 : $assertionsDisabled, calendarBuilder);
                                    if (iSubParseNumericZone3 > 0) {
                                        return iSubParseNumericZone3;
                                    }
                                    parsePosition2.index = -iSubParseNumericZone3;
                                }
                                break;
                            case 22:
                                int month2 = parseMonth(str, i3, i4, i, i10, parsePosition2, zUseDateFormatSymbols, true, calendarBuilder);
                                if (month2 > 0) {
                                    return month2;
                                }
                                break;
                            case 23:
                                int weekday2 = parseWeekday(str, i, i10, zUseDateFormatSymbols, true, calendarBuilder);
                                if (weekday2 > 0) {
                                    return weekday2;
                                }
                                break;
                        }
                    }
                } else {
                    i4 = 0;
                    boolean zUseDateFormatSymbols2 = useDateFormatSymbols();
                    switch (i9) {
                    }
                }
                parsePosition.errorIndex = parsePosition2.index;
                return -1;
            }
        }
        parsePosition.errorIndex = i;
        return -1;
    }

    private int parseMonth(String str, int i, int i2, int i3, int i4, ParsePosition parsePosition, boolean z, boolean z2, CalendarBuilder calendarBuilder) {
        int iMatchString;
        if (i <= 2) {
            calendarBuilder.set(2, i2 - 1);
            return parsePosition.index;
        }
        if (z) {
            int iMatchString2 = matchString(str, i3, 2, z2 ? this.formatData.getStandAloneMonths() : this.formatData.getMonths(), calendarBuilder);
            if (iMatchString2 > 0) {
                return iMatchString2;
            }
            iMatchString = matchString(str, i3, 2, z2 ? this.formatData.getShortStandAloneMonths() : this.formatData.getShortMonths(), calendarBuilder);
            if (iMatchString > 0) {
                return iMatchString;
            }
        } else {
            iMatchString = matchString(str, i3, i4, this.calendar.getDisplayNames(i4, 0, this.locale), calendarBuilder);
            if (iMatchString > 0) {
                return iMatchString;
            }
        }
        return iMatchString;
    }

    private int parseWeekday(String str, int i, int i2, boolean z, boolean z2, CalendarBuilder calendarBuilder) {
        int iMatchString;
        if (z) {
            int iMatchString2 = matchString(str, i, 7, z2 ? this.formatData.getStandAloneWeekdays() : this.formatData.getWeekdays(), calendarBuilder);
            if (iMatchString2 > 0) {
                return iMatchString2;
            }
            iMatchString = matchString(str, i, 7, z2 ? this.formatData.getShortStandAloneWeekdays() : this.formatData.getShortWeekdays(), calendarBuilder);
            if (iMatchString > 0) {
                return iMatchString;
            }
        } else {
            iMatchString = -1;
            for (int i3 : new int[]{2, 1}) {
                iMatchString = matchString(str, i, i2, this.calendar.getDisplayNames(i2, i3, this.locale), calendarBuilder);
                if (iMatchString > 0) {
                    return iMatchString;
                }
            }
        }
        return iMatchString;
    }

    private final String getCalendarName() {
        return this.calendar.getClass().getName();
    }

    private boolean useDateFormatSymbols() {
        if (this.useDateFormatSymbols || isGregorianCalendar() || this.locale == null) {
            return true;
        }
        return $assertionsDisabled;
    }

    private boolean isGregorianCalendar() {
        return "java.util.GregorianCalendar".equals(getCalendarName());
    }

    private String translatePattern(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (z) {
                if (cCharAt == '\'') {
                    z = false;
                }
            } else if (cCharAt != '\'') {
                if ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z')) {
                    int iIndexOf = str2.indexOf(cCharAt);
                    if (iIndexOf >= 0) {
                        if (iIndexOf < str3.length()) {
                            cCharAt = str3.charAt(iIndexOf);
                        }
                    } else {
                        throw new IllegalArgumentException("Illegal pattern  character '" + cCharAt + "'");
                    }
                }
            } else {
                z = true;
            }
            sb.append(cCharAt);
        }
        if (z) {
            throw new IllegalArgumentException("Unfinished quote in pattern");
        }
        return sb.toString();
    }

    public String toPattern() {
        return this.pattern;
    }

    public String toLocalizedPattern() {
        return translatePattern(this.pattern, "GyMdkHmsSEDFwWahKzZYuXLcbB", this.formatData.getLocalPatternChars());
    }

    public void applyPattern(String str) {
        this.compiledPattern = compile(str);
        this.pattern = str;
    }

    public void applyLocalizedPattern(String str) {
        String strTranslatePattern = translatePattern(str, this.formatData.getLocalPatternChars(), "GyMdkHmsSEDFwWahKzZYuXLcbB");
        this.compiledPattern = compile(strTranslatePattern);
        this.pattern = strTranslatePattern;
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols dateFormatSymbols) {
        this.formatData = (DateFormatSymbols) dateFormatSymbols.clone();
        this.useDateFormatSymbols = true;
    }

    @Override
    public Object clone() {
        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) super.clone();
        simpleDateFormat.formatData = (DateFormatSymbols) this.formatData.clone();
        return simpleDateFormat;
    }

    @Override
    public int hashCode() {
        return this.pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return $assertionsDisabled;
        }
        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) obj;
        if (this.pattern.equals(simpleDateFormat.pattern) && this.formatData.equals(simpleDateFormat.formatData)) {
            return true;
        }
        return $assertionsDisabled;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        String id;
        TimeZone timeZone;
        objectInputStream.defaultReadObject();
        try {
            this.compiledPattern = compile(this.pattern);
            if (this.serialVersionOnStream < 1) {
                initializeDefaultCentury();
            } else {
                parseAmbiguousDatesAsAfter(this.defaultCenturyStart);
            }
            this.serialVersionOnStream = 1;
            TimeZone timeZone2 = getTimeZone();
            if ((timeZone2 instanceof SimpleTimeZone) && (timeZone = TimeZone.getTimeZone((id = timeZone2.getID()))) != null && timeZone.hasSameRules(timeZone2) && timeZone.getID().equals(id)) {
                setTimeZone(timeZone);
            }
        } catch (Exception e) {
            throw new InvalidObjectException("invalid pattern");
        }
    }

    private void checkNegativeNumberExpression() {
        int iIndexOf;
        if ((this.numberFormat instanceof DecimalFormat) && !this.numberFormat.equals(this.originalNumberFormat)) {
            String pattern = ((DecimalFormat) this.numberFormat).toPattern();
            if (!pattern.equals(this.originalNumberPattern)) {
                this.hasFollowingMinusSign = $assertionsDisabled;
                int iIndexOf2 = pattern.indexOf(59);
                if (iIndexOf2 > -1 && (iIndexOf = pattern.indexOf(45, iIndexOf2)) > pattern.lastIndexOf(48) && iIndexOf > pattern.lastIndexOf(35)) {
                    this.hasFollowingMinusSign = true;
                    this.minusSign = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getMinusSign();
                }
                this.originalNumberPattern = pattern;
            }
            this.originalNumberFormat = this.numberFormat;
        }
    }
}
