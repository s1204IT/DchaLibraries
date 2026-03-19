package android.icu.util;

import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;

public class IndianCalendar extends Calendar {
    public static final int AGRAHAYANA = 8;
    public static final int ASADHA = 3;
    public static final int ASVINA = 6;
    public static final int BHADRA = 5;
    public static final int CHAITRA = 0;
    public static final int IE = 0;
    private static final int INDIAN_ERA_START = 78;
    private static final int INDIAN_YEAR_START = 80;
    public static final int JYAISTHA = 2;
    public static final int KARTIKA = 7;
    private static final int[][] LIMITS = {new int[]{0, 0, 0, 0}, new int[]{-5000000, -5000000, 5000000, 5000000}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 52, 53}, new int[0], new int[]{1, 1, 30, 31}, new int[]{1, 1, 365, 366}, new int[0], new int[]{-1, -1, 5, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[0]};
    public static final int MAGHA = 10;
    public static final int PAUSA = 9;
    public static final int PHALGUNA = 11;
    public static final int SRAVANA = 4;
    public static final int VAISAKHA = 1;
    private static final long serialVersionUID = 3617859668165014834L;

    public IndianCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public IndianCalendar(TimeZone zone) {
        this(zone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public IndianCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    public IndianCalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public IndianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public IndianCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public IndianCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    public IndianCalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, year);
        set(2, month);
        set(5, date);
    }

    public IndianCalendar(int year, int month, int date, int hour, int minute, int second) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, year);
        set(2, month);
        set(5, date);
        set(11, hour);
        set(12, minute);
        set(13, second);
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
    protected int handleGetYearLength(int extendedYear) {
        return super.handleGetYearLength(extendedYear);
    }

    @Override
    protected int handleGetMonthLength(int extendedYear, int month) {
        if (month < 0 || month > 11) {
            int[] remainder = new int[1];
            extendedYear += floorDivide(month, 12, remainder);
            month = remainder[0];
        }
        if (isGregorianLeap(extendedYear + 78) && month == 0) {
            return 31;
        }
        return (month < 1 || month > 5) ? 30 : 31;
    }

    @Override
    protected void handleComputeFields(int julianDay) {
        int leapMonth;
        int yday;
        int IndianMonth;
        int IndianDayOfMonth;
        int[] gregorianDay = jdToGregorian(julianDay);
        int IndianYear = gregorianDay[0] - 78;
        double jdAtStartOfGregYear = gregorianToJD(gregorianDay[0], 1, 1);
        int yday2 = (int) (((double) julianDay) - jdAtStartOfGregYear);
        if (yday2 < 80) {
            IndianYear--;
            leapMonth = isGregorianLeap(gregorianDay[0] + (-1)) ? 31 : 30;
            yday = yday2 + leapMonth + 155 + 90 + 10;
        } else {
            leapMonth = isGregorianLeap(gregorianDay[0]) ? 31 : 30;
            yday = yday2 - 80;
        }
        if (yday < leapMonth) {
            IndianMonth = 0;
            IndianDayOfMonth = yday + 1;
        } else {
            int mday = yday - leapMonth;
            if (mday < 155) {
                IndianMonth = (mday / 31) + 1;
                IndianDayOfMonth = (mday % 31) + 1;
            } else {
                int mday2 = mday - 155;
                IndianMonth = (mday2 / 30) + 6;
                IndianDayOfMonth = (mday2 % 30) + 1;
            }
        }
        internalSet(0, 0);
        internalSet(19, IndianYear);
        internalSet(1, IndianYear);
        internalSet(2, IndianMonth);
        internalSet(5, IndianDayOfMonth);
        internalSet(6, yday + 1);
    }

    @Override
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    @Override
    protected int handleComputeMonthStart(int year, int month, boolean useMonth) {
        int imonth;
        if (month < 0 || month > 11) {
            year += month / 12;
            month %= 12;
        }
        if (month == 12) {
            imonth = 1;
        } else {
            imonth = month + 1;
        }
        double jd = IndianToJD(year, imonth, 1);
        return (int) jd;
    }

    private static double IndianToJD(int year, int month, int date) {
        int leapMonth;
        double start;
        int gyear = year + 78;
        if (isGregorianLeap(gyear)) {
            leapMonth = 31;
            start = gregorianToJD(gyear, 3, 21);
        } else {
            leapMonth = 30;
            start = gregorianToJD(gyear, 3, 22);
        }
        if (month == 1) {
            return start + ((double) (date - 1));
        }
        int m = month - 2;
        double jd = start + ((double) leapMonth) + ((double) (Math.min(m, 5) * 31));
        if (month >= 8) {
            int m2 = month - 7;
            jd += (double) (m2 * 30);
        }
        return jd + ((double) (date - 1));
    }

    private static double gregorianToJD(int year, int month, int date) {
        int y = year - 1;
        int result = (month <= 2 ? 0 : isGregorianLeap(year) ? -1 : -2) + (((month * 367) - 362) / 12) + (((y * 365) + (y / 4)) - (y / 100)) + (y / 400) + date;
        return ((double) (result - 1)) + 1721425.5d;
    }

    private static int[] jdToGregorian(double jd) {
        int i;
        double wjd = Math.floor(jd - 0.5d) + 0.5d;
        double depoch = wjd - 1721425.5d;
        double quadricent = Math.floor(depoch / 146097.0d);
        double dqc = depoch % 146097.0d;
        double cent = Math.floor(dqc / 36524.0d);
        double dcent = dqc % 36524.0d;
        double quad = Math.floor(dcent / 1461.0d);
        double dquad = dcent % 1461.0d;
        double yindex = Math.floor(dquad / 365.0d);
        int year = (int) ((400.0d * quadricent) + (100.0d * cent) + (4.0d * quad) + yindex);
        if (!(cent == 4.0d || yindex == 4.0d)) {
            year++;
        }
        double yearday = wjd - gregorianToJD(year, 1, 1);
        if (wjd < gregorianToJD(year, 3, 1)) {
            i = 0;
        } else {
            i = isGregorianLeap(year) ? 1 : 2;
        }
        double leapadj = i;
        int month = (int) Math.floor((((yearday + leapadj) * 12.0d) + 373.0d) / 367.0d);
        int day = ((int) (wjd - gregorianToJD(year, month, 1))) + 1;
        int[] julianDate = {year, month, day};
        return julianDate;
    }

    private static boolean isGregorianLeap(int year) {
        if (year % 4 == 0) {
            return year % 100 != 0 || year % 400 == 0;
        }
        return false;
    }

    @Override
    public String getType() {
        return "indian";
    }
}
