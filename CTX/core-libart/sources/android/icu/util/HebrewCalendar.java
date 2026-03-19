package android.icu.util;

import android.icu.impl.CalendarCache;
import android.icu.impl.coll.CollationFastLatin;
import android.icu.lang.UCharacter;
import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;

public class HebrewCalendar extends Calendar {
    public static final int ADAR = 6;
    public static final int ADAR_1 = 5;
    public static final int AV = 11;
    private static final long BAHARAD = 12084;
    private static final long DAY_PARTS = 25920;
    public static final int ELUL = 12;
    public static final int HESHVAN = 1;
    private static final long HOUR_PARTS = 1080;
    public static final int IYAR = 8;
    public static final int KISLEV = 2;
    private static final int MONTH_DAYS = 29;
    private static final long MONTH_FRACT = 13753;
    private static final long MONTH_PARTS = 765433;
    public static final int NISAN = 7;
    public static final int SHEVAT = 4;
    public static final int SIVAN = 9;
    public static final int TAMUZ = 10;
    public static final int TEVET = 3;
    public static final int TISHRI = 0;
    private static final long serialVersionUID = -1952524560588825816L;
    private static final int[][] LIMITS = {new int[]{0, 0, 0, 0}, new int[]{-5000000, -5000000, 5000000, 5000000}, new int[]{0, 0, 12, 12}, new int[]{1, 1, 51, 56}, new int[0], new int[]{1, 1, 29, 30}, new int[]{1, 1, 353, 385}, new int[0], new int[]{-1, -1, 5, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[0]};
    private static final int[][] MONTH_LENGTH = {new int[]{30, 30, 30}, new int[]{29, 29, 30}, new int[]{29, 30, 30}, new int[]{29, 29, 29}, new int[]{30, 30, 30}, new int[]{30, 30, 30}, new int[]{29, 29, 29}, new int[]{30, 30, 30}, new int[]{29, 29, 29}, new int[]{30, 30, 30}, new int[]{29, 29, 29}, new int[]{30, 30, 30}, new int[]{29, 29, 29}};
    private static final int[][] MONTH_START = {new int[]{0, 0, 0}, new int[]{30, 30, 30}, new int[]{59, 59, 60}, new int[]{88, 89, 90}, new int[]{117, 118, 119}, new int[]{147, 148, 149}, new int[]{147, 148, 149}, new int[]{176, 177, 178}, new int[]{206, 207, 208}, new int[]{235, 236, 237}, new int[]{UCharacter.UnicodeBlock.CYRILLIC_EXTENDED_C_ID, UCharacter.UnicodeBlock.GLAGOLITIC_SUPPLEMENT_ID, UCharacter.UnicodeBlock.IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION_ID}, new int[]{294, 295, 296}, new int[]{324, 325, 326}, new int[]{353, 354, 355}};
    private static final int[][] LEAP_MONTH_START = {new int[]{0, 0, 0}, new int[]{30, 30, 30}, new int[]{59, 59, 60}, new int[]{88, 89, 90}, new int[]{117, 118, 119}, new int[]{147, 148, 149}, new int[]{177, 178, 179}, new int[]{206, 207, 208}, new int[]{236, 237, 238}, new int[]{UCharacter.UnicodeBlock.CYRILLIC_EXTENDED_C_ID, UCharacter.UnicodeBlock.GLAGOLITIC_SUPPLEMENT_ID, UCharacter.UnicodeBlock.IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION_ID}, new int[]{295, 296, 297}, new int[]{324, 325, 326}, new int[]{354, 355, 356}, new int[]{CollationFastLatin.LATIN_MAX, CollationFastLatin.LATIN_LIMIT, 385}};
    private static CalendarCache cache = new CalendarCache();

    public HebrewCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public HebrewCalendar(TimeZone timeZone) {
        this(timeZone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public HebrewCalendar(Locale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public HebrewCalendar(ULocale uLocale) {
        this(TimeZone.getDefault(), uLocale);
    }

    public HebrewCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public HebrewCalendar(TimeZone timeZone, ULocale uLocale) {
        super(timeZone, uLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public HebrewCalendar(int i, int i2, int i3) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    public HebrewCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    public HebrewCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
    }

    @Override
    public void add(int i, int i2) {
        int i3;
        if (i == 2) {
            int i4 = get(2);
            int i5 = get(1);
            boolean z = false;
            if (i2 > 0) {
                if (i4 < 5) {
                    z = true;
                }
                i3 = i4 + i2;
                while (true) {
                    if (z && i3 >= 5 && !isLeapYear(i5)) {
                        i3++;
                    }
                    if (i3 <= 12) {
                        break;
                    }
                    i3 -= 13;
                    i5++;
                    z = true;
                }
            } else {
                if (i4 > 5) {
                    z = true;
                }
                i3 = i4 + i2;
                while (true) {
                    if (z && i3 <= 5 && !isLeapYear(i5)) {
                        i3--;
                    }
                    if (i3 >= 0) {
                        break;
                    }
                    i3 += 13;
                    i5--;
                    z = true;
                }
            }
            set(2, i3);
            set(1, i5);
            pinField(5);
            return;
        }
        super.add(i, i2);
    }

    @Override
    public void roll(int i, int i2) {
        if (i == 2) {
            int i3 = get(2);
            int i4 = get(1);
            boolean zIsLeapYear = isLeapYear(i4);
            int iMonthsInYear = (i2 % monthsInYear(i4)) + i3;
            if (!zIsLeapYear) {
                if (i2 > 0 && i3 < 5 && iMonthsInYear >= 5) {
                    iMonthsInYear++;
                } else if (i2 < 0 && i3 > 5 && iMonthsInYear <= 5) {
                    iMonthsInYear--;
                }
            }
            set(2, (iMonthsInYear + 13) % 13);
            pinField(5);
            return;
        }
        super.roll(i, i2);
    }

    private static long startOfYear(int i) {
        long j = i;
        long j2 = cache.get(j);
        if (j2 == CalendarCache.EMPTY) {
            int i2 = ((235 * i) - 234) / 19;
            long j3 = (((long) i2) * MONTH_FRACT) + BAHARAD;
            long j4 = ((long) (i2 * 29)) + (j3 / DAY_PARTS);
            long j5 = j3 % DAY_PARTS;
            int i3 = (int) (j4 % 7);
            if (i3 == 2 || i3 == 4 || i3 == 6) {
                j4++;
                i3 = (int) (j4 % 7);
            }
            if (i3 == 1 && j5 > 16404 && !isLeapYear(i)) {
                j4 += 2;
            } else if (i3 == 0 && j5 > 23269 && isLeapYear(i - 1)) {
                j4++;
            }
            long j6 = j4;
            cache.put(j, j6);
            return j6;
        }
        return j2;
    }

    private final int yearType(int i) {
        int iHandleGetYearLength = handleGetYearLength(i);
        if (iHandleGetYearLength > 380) {
            iHandleGetYearLength -= 30;
        }
        switch (iHandleGetYearLength) {
            case 353:
                return 0;
            case 354:
                return 1;
            case 355:
                return 2;
            default:
                throw new IllegalArgumentException("Illegal year length " + iHandleGetYearLength + " in year " + i);
        }
    }

    @Deprecated
    public static boolean isLeapYear(int i) {
        int i2 = ((i * 12) + 17) % 19;
        return i2 >= (i2 < 0 ? -7 : 12);
    }

    private static int monthsInYear(int i) {
        return isLeapYear(i) ? 13 : 12;
    }

    @Override
    protected int handleGetLimit(int i, int i2) {
        return LIMITS[i][i2];
    }

    @Override
    protected int handleGetMonthLength(int i, int i2) {
        while (i2 < 0) {
            i--;
            i2 += monthsInYear(i);
        }
        while (i2 > 12) {
            i2 -= monthsInYear(i);
            i++;
        }
        switch (i2) {
            case 1:
            case 2:
                return MONTH_LENGTH[i2][yearType(i)];
            default:
                return MONTH_LENGTH[i2][0];
        }
    }

    @Override
    protected int handleGetYearLength(int i) {
        return (int) (startOfYear(i + 1) - startOfYear(i));
    }

    @Override
    @Deprecated
    protected void validateField(int i) {
        if (i == 2 && !isLeapYear(handleGetExtendedYear()) && internalGet(2) == 5) {
            throw new IllegalArgumentException("MONTH cannot be ADAR_1(5) except leap years");
        }
        super.validateField(i);
    }

    @Override
    protected void handleComputeFields(int i) {
        long j = i - 347997;
        int i2 = ((int) (((19 * ((DAY_PARTS * j) / MONTH_PARTS)) + 234) / 235)) + 1;
        int iStartOfYear = (int) (j - startOfYear(i2));
        while (iStartOfYear < 1) {
            i2--;
            iStartOfYear = (int) (j - startOfYear(i2));
        }
        int iYearType = yearType(i2);
        int[][] iArr = isLeapYear(i2) ? LEAP_MONTH_START : MONTH_START;
        int i3 = 0;
        while (iStartOfYear > iArr[i3][iYearType]) {
            i3++;
        }
        int i4 = i3 - 1;
        int i5 = iStartOfYear - iArr[i4][iYearType];
        internalSet(0, 0);
        internalSet(1, i2);
        internalSet(19, i2);
        internalSet(2, i4);
        internalSet(5, i5);
        internalSet(6, iStartOfYear);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            return internalGet(19, 1);
        }
        return internalGet(1, 1);
    }

    @Override
    protected int handleComputeMonthStart(int i, int i2, boolean z) {
        while (i2 < 0) {
            i--;
            i2 += monthsInYear(i);
        }
        while (i2 > 12) {
            i2 -= monthsInYear(i);
            i++;
        }
        long jStartOfYear = startOfYear(i);
        if (i2 != 0) {
            if (isLeapYear(i)) {
                jStartOfYear += (long) LEAP_MONTH_START[i2][yearType(i)];
            } else {
                jStartOfYear += (long) MONTH_START[i2][yearType(i)];
            }
        }
        return (int) (jStartOfYear + 347997);
    }

    @Override
    public String getType() {
        return "hebrew";
    }
}
