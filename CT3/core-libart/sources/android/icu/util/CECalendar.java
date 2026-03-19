package android.icu.util;

import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;

abstract class CECalendar extends Calendar {
    private static final int[][] LIMITS = {new int[]{0, 0, 1, 1}, new int[]{1, 1, 5000000, 5000000}, new int[]{0, 0, 12, 12}, new int[]{1, 1, 52, 53}, new int[0], new int[]{1, 1, 5, 30}, new int[]{1, 1, 365, 366}, new int[0], new int[]{-1, -1, 1, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[0]};
    private static final long serialVersionUID = -999547623066414271L;

    protected abstract int getJDEpochOffset();

    protected CECalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    protected CECalendar(TimeZone zone) {
        this(zone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    protected CECalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    protected CECalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    protected CECalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    protected CECalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    protected CECalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(year, month, date);
    }

    protected CECalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    protected CECalendar(int year, int month, int date, int hour, int minute, int second) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(year, month, date, hour, minute, second);
    }

    @Override
    protected int handleComputeMonthStart(int eyear, int emonth, boolean useMonth) {
        return ceToJD(eyear, emonth, 0, getJDEpochOffset());
    }

    @Override
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    @Override
    protected int handleGetMonthLength(int extendedYear, int month) {
        if ((month + 1) % 13 != 0) {
            return 30;
        }
        return ((extendedYear % 4) / 3) + 5;
    }

    public static int ceToJD(long year, int month, int day, int jdEpochOffset) {
        long year2;
        int month2;
        if (month >= 0) {
            year2 = year + ((long) (month / 13));
            month2 = month % 13;
        } else {
            year2 = year + ((long) ((r6 / 13) - 1));
            month2 = ((month + 1) % 13) + 12;
        }
        return (int) (((((((long) jdEpochOffset) + (365 * year2)) + floorDivide(year2, 4L)) + ((long) (month2 * 30))) + ((long) day)) - 1);
    }

    public static void jdToCE(int julianDay, int jdEpochOffset, int[] fields) {
        int[] r4 = new int[1];
        int c4 = floorDivide(julianDay - jdEpochOffset, 1461, r4);
        fields[0] = (c4 * 4) + ((r4[0] / 365) - (r4[0] / 1460));
        int doy = r4[0] == 1460 ? 365 : r4[0] % 365;
        fields[1] = doy / 30;
        fields[2] = (doy % 30) + 1;
    }
}
