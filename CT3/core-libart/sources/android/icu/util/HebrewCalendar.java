package android.icu.util;

import android.icu.impl.CalendarCache;
import android.icu.impl.coll.CollationFastLatin;
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
    private static final int[][] MONTH_START = {new int[]{0, 0, 0}, new int[]{30, 30, 30}, new int[]{59, 59, 60}, new int[]{88, 89, 90}, new int[]{117, 118, 119}, new int[]{147, 148, 149}, new int[]{147, 148, 149}, new int[]{176, 177, 178}, new int[]{206, 207, 208}, new int[]{235, 236, 237}, new int[]{265, 266, 267}, new int[]{294, 295, 296}, new int[]{324, 325, 326}, new int[]{353, 354, 355}};
    private static final int[][] LEAP_MONTH_START = {new int[]{0, 0, 0}, new int[]{30, 30, 30}, new int[]{59, 59, 60}, new int[]{88, 89, 90}, new int[]{117, 118, 119}, new int[]{147, 148, 149}, new int[]{177, 178, 179}, new int[]{206, 207, 208}, new int[]{236, 237, 238}, new int[]{265, 266, 267}, new int[]{295, 296, 297}, new int[]{324, 325, 326}, new int[]{354, 355, 356}, new int[]{CollationFastLatin.LATIN_MAX, CollationFastLatin.LATIN_LIMIT, 385}};
    private static CalendarCache cache = new CalendarCache();

    public HebrewCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public HebrewCalendar(TimeZone zone) {
        this(zone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public HebrewCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    public HebrewCalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public HebrewCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public HebrewCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public HebrewCalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, year);
        set(2, month);
        set(5, date);
    }

    public HebrewCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    public HebrewCalendar(int year, int month, int date, int hour, int minute, int second) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, year);
        set(2, month);
        set(5, date);
        set(11, hour);
        set(12, minute);
        set(13, second);
    }

    @Override
    public void add(int field, int amount) {
        int month;
        switch (field) {
            case 2:
                int month2 = get(2);
                int year = get(1);
                if (amount > 0) {
                    boolean acrossAdar1 = month2 < 5;
                    month = month2 + amount;
                    while (true) {
                        if (acrossAdar1 && month >= 5 && !isLeapYear(year)) {
                            month++;
                        }
                        if (month > 12) {
                            month -= 13;
                            year++;
                            acrossAdar1 = true;
                        }
                    }
                } else {
                    boolean acrossAdar12 = month2 > 5;
                    month = month2 + amount;
                    while (true) {
                        if (acrossAdar12 && month <= 5 && !isLeapYear(year)) {
                            month--;
                        }
                        if (month < 0) {
                            month += 13;
                            year--;
                            acrossAdar12 = true;
                        }
                    }
                }
                set(2, month);
                set(1, year);
                pinField(5);
                break;
            default:
                super.add(field, amount);
                break;
        }
    }

    @Override
    public void roll(int field, int amount) {
        switch (field) {
            case 2:
                int month = get(2);
                int year = get(1);
                boolean leapYear = isLeapYear(year);
                int yearLength = monthsInYear(year);
                int newMonth = month + (amount % yearLength);
                if (!leapYear) {
                    if (amount > 0 && month < 5 && newMonth >= 5) {
                        newMonth++;
                    } else if (amount < 0 && month > 5 && newMonth <= 5) {
                        newMonth--;
                    }
                }
                set(2, (newMonth + 13) % 13);
                pinField(5);
                break;
            default:
                super.roll(field, amount);
                break;
        }
    }

    private static long startOfYear(int year) {
        long day = cache.get(year);
        if (day == CalendarCache.EMPTY) {
            int months = ((year * 235) - 234) / 19;
            long frac = (((long) months) * MONTH_FRACT) + BAHARAD;
            day = ((long) (months * 29)) + (frac / DAY_PARTS);
            long frac2 = frac % DAY_PARTS;
            int wd = (int) (day % 7);
            if (wd == 2 || wd == 4 || wd == 6) {
                day++;
                wd = (int) (day % 7);
            }
            if (wd == 1 && frac2 > 16404 && !isLeapYear(year)) {
                day += 2;
            } else if (wd == 0 && frac2 > 23269 && isLeapYear(year - 1)) {
                day++;
            }
            cache.put(year, day);
        }
        return day;
    }

    private final int yearType(int year) {
        int yearLength = handleGetYearLength(year);
        if (yearLength > 380) {
            yearLength -= 30;
        }
        switch (yearLength) {
            case 353:
                return 0;
            case 354:
                return 1;
            case 355:
                return 2;
            default:
                throw new IllegalArgumentException("Illegal year length " + yearLength + " in year " + year);
        }
    }

    @Deprecated
    public static boolean isLeapYear(int year) {
        int x = ((year * 12) + 17) % 19;
        return x >= (x < 0 ? -7 : 12);
    }

    private static int monthsInYear(int year) {
        return isLeapYear(year) ? 13 : 12;
    }

    @Override
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    @Override
    protected int handleGetMonthLength(int extendedYear, int month) {
        int extendedYear2;
        while (month < 0) {
            extendedYear--;
            month += monthsInYear(extendedYear);
        }
        while (true) {
            extendedYear2 = extendedYear;
            if (month <= 12) {
                break;
            }
            extendedYear = extendedYear2 + 1;
            month -= monthsInYear(extendedYear2);
        }
        switch (month) {
            case 1:
            case 2:
                return MONTH_LENGTH[month][yearType(extendedYear2)];
            default:
                return MONTH_LENGTH[month][0];
        }
    }

    @Override
    protected int handleGetYearLength(int eyear) {
        return (int) (startOfYear(eyear + 1) - startOfYear(eyear));
    }

    @Override
    @Deprecated
    protected void validateField(int field) {
        if (field == 2 && !isLeapYear(handleGetExtendedYear()) && internalGet(2) == 5) {
            throw new IllegalArgumentException("MONTH cannot be ADAR_1(5) except leap years");
        }
        super.validateField(field);
    }

    @Override
    protected void handleComputeFields(int julianDay) {
        int dayOfYear;
        long d = julianDay - 347997;
        long m = (DAY_PARTS * d) / MONTH_PARTS;
        int year = ((int) (((19 * m) + 234) / 235)) + 1;
        long ys = startOfYear(year);
        while (true) {
            dayOfYear = (int) (d - ys);
            if (dayOfYear >= 1) {
                break;
            }
            year--;
            ys = startOfYear(year);
        }
        int yearType = yearType(year);
        int[][] monthStart = isLeapYear(year) ? LEAP_MONTH_START : MONTH_START;
        int month = 0;
        while (dayOfYear > monthStart[month][yearType]) {
            month++;
        }
        int month2 = month - 1;
        int dayOfMonth = dayOfYear - monthStart[month2][yearType];
        internalSet(0, 0);
        internalSet(1, year);
        internalSet(19, year);
        internalSet(2, month2);
        internalSet(5, dayOfMonth);
        internalSet(6, dayOfYear);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            int year = internalGet(19, 1);
            return year;
        }
        int year2 = internalGet(1, 1);
        return year2;
    }

    @Override
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        int eyear2;
        while (month < 0) {
            eyear--;
            month += monthsInYear(eyear);
        }
        while (true) {
            eyear2 = eyear;
            if (month <= 12) {
                break;
            }
            eyear = eyear2 + 1;
            month -= monthsInYear(eyear2);
        }
        long day = startOfYear(eyear2);
        if (month != 0) {
            if (isLeapYear(eyear2)) {
                day += (long) LEAP_MONTH_START[month][yearType(eyear2)];
            } else {
                day += (long) MONTH_START[month][yearType(eyear2)];
            }
        }
        return (int) (347997 + day);
    }

    @Override
    public String getType() {
        return "hebrew";
    }
}
