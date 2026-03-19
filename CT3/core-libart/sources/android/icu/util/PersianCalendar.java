package android.icu.util;

import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;

@Deprecated
public class PersianCalendar extends Calendar {
    private static final int PERSIAN_EPOCH = 1948320;
    private static final long serialVersionUID = -6727306982975111643L;
    private static final int[][] MONTH_COUNT = {new int[]{31, 31, 0}, new int[]{31, 31, 31}, new int[]{31, 31, 62}, new int[]{31, 31, 93}, new int[]{31, 31, 124}, new int[]{31, 31, 155}, new int[]{30, 30, 186}, new int[]{30, 30, 216}, new int[]{30, 30, 246}, new int[]{30, 30, 276}, new int[]{30, 30, 306}, new int[]{29, 30, 336}};
    private static final int[][] LIMITS = {new int[]{0, 0, 0, 0}, new int[]{-5000000, -5000000, 5000000, 5000000}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 52, 53}, new int[0], new int[]{1, 1, 29, 31}, new int[]{1, 1, 365, 366}, new int[0], new int[]{-1, -1, 5, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[0]};

    @Deprecated
    public PersianCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    @Deprecated
    public PersianCalendar(TimeZone zone) {
        this(zone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    @Deprecated
    public PersianCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    @Deprecated
    public PersianCalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    @Deprecated
    public PersianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    @Deprecated
    public PersianCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    @Deprecated
    public PersianCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    @Deprecated
    public PersianCalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, year);
        set(2, month);
        set(5, date);
    }

    @Deprecated
    public PersianCalendar(int year, int month, int date, int hour, int minute, int second) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, year);
        set(2, month);
        set(5, date);
        set(11, hour);
        set(12, minute);
        set(13, second);
    }

    @Override
    @Deprecated
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    private static final boolean isLeapYear(int year) {
        int[] remainder = new int[1];
        floorDivide((year * 25) + 11, 33, remainder);
        return remainder[0] < 8;
    }

    @Override
    @Deprecated
    protected int handleGetMonthLength(int extendedYear, int month) {
        if (month < 0 || month > 11) {
            int[] rem = new int[1];
            extendedYear += floorDivide(month, 12, rem);
            month = rem[0];
        }
        return MONTH_COUNT[month][isLeapYear(extendedYear) ? (char) 1 : (char) 0];
    }

    @Override
    @Deprecated
    protected int handleGetYearLength(int extendedYear) {
        return isLeapYear(extendedYear) ? 366 : 365;
    }

    @Override
    @Deprecated
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        if (month < 0 || month > 11) {
            int[] rem = new int[1];
            eyear += floorDivide(month, 12, rem);
            month = rem[0];
        }
        int julianDay = ((eyear - 1) * 365) + 1948319 + floorDivide((eyear * 8) + 21, 33);
        if (month != 0) {
            return julianDay + MONTH_COUNT[month][2];
        }
        return julianDay;
    }

    @Override
    @Deprecated
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            int year = internalGet(19, 1);
            return year;
        }
        int year2 = internalGet(1, 1);
        return year2;
    }

    @Override
    @Deprecated
    protected void handleComputeFields(int julianDay) {
        int month;
        long daysSinceEpoch = julianDay - PERSIAN_EPOCH;
        int year = ((int) floorDivide((33 * daysSinceEpoch) + 3, 12053L)) + 1;
        long farvardin1 = ((year - 1) * 365) + floorDivide((year * 8) + 21, 33);
        int dayOfYear = (int) (daysSinceEpoch - farvardin1);
        if (dayOfYear < 216) {
            month = dayOfYear / 31;
        } else {
            month = (dayOfYear - 6) / 30;
        }
        int dayOfMonth = (dayOfYear - MONTH_COUNT[month][2]) + 1;
        internalSet(0, 0);
        internalSet(1, year);
        internalSet(19, year);
        internalSet(2, month);
        internalSet(5, dayOfMonth);
        internalSet(6, dayOfYear + 1);
    }

    @Override
    @Deprecated
    public String getType() {
        return "persian";
    }
}
