package android.icu.util;

import android.icu.impl.Grego;
import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;

public class GregorianCalendar extends Calendar {
    public static final int AD = 1;
    public static final int BC = 0;
    private static final int EPOCH_YEAR = 1970;
    private static final long serialVersionUID = 9199388694351062137L;
    private transient int cutoverJulianDay;
    private long gregorianCutover;
    private transient int gregorianCutoverYear;
    protected transient boolean invertGregorian;
    protected transient boolean isGregorian;
    private static final int[][] MONTH_COUNT = {new int[]{31, 31, 0, 0}, new int[]{28, 29, 31, 31}, new int[]{31, 31, 59, 60}, new int[]{30, 30, 90, 91}, new int[]{31, 31, 120, 121}, new int[]{30, 30, 151, 152}, new int[]{31, 31, 181, 182}, new int[]{31, 31, 212, 213}, new int[]{30, 30, 243, 244}, new int[]{31, 31, 273, 274}, new int[]{30, 30, 304, 305}, new int[]{31, 31, 334, 335}};
    private static final int[][] LIMITS = {new int[]{0, 0, 1, 1}, new int[]{1, 1, 5828963, 5838270}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 52, 53}, new int[0], new int[]{1, 1, 28, 31}, new int[]{1, 1, 365, 366}, new int[0], new int[]{-1, -1, 4, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5838270, -5838270, 5828964, 5838271}, new int[0], new int[]{-5838269, -5838269, 5828963, 5838270}, new int[0], new int[0], new int[0]};

    @Override
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    public GregorianCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public GregorianCalendar(TimeZone zone) {
        this(zone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public GregorianCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    public GregorianCalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        set(0, 1);
        set(1, year);
        set(2, month);
        set(5, date);
    }

    public GregorianCalendar(int year, int month, int date, int hour, int minute) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        set(0, 1);
        set(1, year);
        set(2, month);
        set(5, date);
        set(11, hour);
        set(12, minute);
    }

    public GregorianCalendar(int year, int month, int date, int hour, int minute, int second) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        set(0, 1);
        set(1, year);
        set(2, month);
        set(5, date);
        set(11, hour);
        set(12, minute);
        set(13, second);
    }

    public void setGregorianChange(Date date) {
        this.gregorianCutover = date.getTime();
        if (this.gregorianCutover <= Grego.MIN_MILLIS) {
            this.cutoverJulianDay = Integer.MIN_VALUE;
            this.gregorianCutoverYear = Integer.MIN_VALUE;
        } else if (this.gregorianCutover >= Grego.MAX_MILLIS) {
            this.cutoverJulianDay = Integer.MAX_VALUE;
            this.gregorianCutoverYear = Integer.MAX_VALUE;
        } else {
            this.cutoverJulianDay = (int) floorDivide(this.gregorianCutover, 86400000L);
            GregorianCalendar cal = new GregorianCalendar(getTimeZone());
            cal.setTime(date);
            this.gregorianCutoverYear = cal.get(19);
        }
    }

    public final Date getGregorianChange() {
        return new Date(this.gregorianCutover);
    }

    public boolean isLeapYear(int year) {
        return year >= this.gregorianCutoverYear ? year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) : year % 4 == 0;
    }

    @Override
    public boolean isEquivalentTo(Calendar other) {
        return super.isEquivalentTo(other) && this.gregorianCutover == ((GregorianCalendar) other).gregorianCutover;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ ((int) this.gregorianCutover);
    }

    @Override
    public void roll(int field, int amount) {
        switch (field) {
            case 3:
                int woy = get(3);
                int isoYear = get(17);
                int isoDoy = internalGet(6);
                if (internalGet(2) == 0) {
                    if (woy >= 52) {
                        isoDoy += handleGetYearLength(isoYear);
                    }
                } else if (woy == 1) {
                    isoDoy -= handleGetYearLength(isoYear - 1);
                }
                int woy2 = woy + amount;
                if (woy2 < 1 || woy2 > 52) {
                    int lastDoy = handleGetYearLength(isoYear);
                    int lastRelDow = (((lastDoy - isoDoy) + internalGet(7)) - getFirstDayOfWeek()) % 7;
                    if (lastRelDow < 0) {
                        lastRelDow += 7;
                    }
                    if (6 - lastRelDow >= getMinimalDaysInFirstWeek()) {
                        lastDoy -= 7;
                    }
                    int lastWoy = weekNumber(lastDoy, lastRelDow + 1);
                    woy2 = (((woy2 + lastWoy) - 1) % lastWoy) + 1;
                }
                set(3, woy2);
                set(1, isoYear);
                break;
            default:
                super.roll(field, amount);
                break;
        }
    }

    @Override
    public int getActualMinimum(int field) {
        return getMinimum(field);
    }

    @Override
    public int getActualMaximum(int field) {
        switch (field) {
            case 1:
                Calendar cal = (Calendar) clone();
                cal.setLenient(true);
                int era = cal.get(0);
                Date d = cal.getTime();
                int lowGood = LIMITS[1][1];
                int highBad = LIMITS[1][2] + 1;
                while (lowGood + 1 < highBad) {
                    int y = (lowGood + highBad) / 2;
                    cal.set(1, y);
                    if (cal.get(1) == y && cal.get(0) == era) {
                        lowGood = y;
                    } else {
                        highBad = y;
                        cal.setTime(d);
                    }
                }
                return lowGood;
            default:
                return super.getActualMaximum(field);
        }
    }

    boolean inDaylightTime() {
        if (!getTimeZone().useDaylightTime()) {
            return false;
        }
        complete();
        return internalGet(16) != 0;
    }

    @Override
    protected int handleGetMonthLength(int extendedYear, int month) {
        if (month < 0 || month > 11) {
            int[] rem = new int[1];
            extendedYear += floorDivide(month, 12, rem);
            month = rem[0];
        }
        return MONTH_COUNT[month][isLeapYear(extendedYear) ? (char) 1 : (char) 0];
    }

    @Override
    protected int handleGetYearLength(int eyear) {
        return isLeapYear(eyear) ? 366 : 365;
    }

    @Override
    protected void handleComputeFields(int julianDay) {
        int eyear;
        int month;
        int dayOfMonth;
        int dayOfYear;
        if (julianDay >= this.cutoverJulianDay) {
            month = getGregorianMonth();
            dayOfMonth = getGregorianDayOfMonth();
            dayOfYear = getGregorianDayOfYear();
            eyear = getGregorianYear();
        } else {
            long julianEpochDay = julianDay - 1721424;
            eyear = (int) floorDivide((4 * julianEpochDay) + 1464, 1461L);
            long january1 = ((eyear - 1) * 365) + floorDivide(eyear - 1, 4);
            int dayOfYear2 = (int) (julianEpochDay - january1);
            boolean isLeap = (eyear & 3) == 0;
            int correction = 0;
            int march1 = isLeap ? 60 : 59;
            if (dayOfYear2 >= march1) {
                correction = isLeap ? 1 : 2;
            }
            month = (((dayOfYear2 + correction) * 12) + 6) / 367;
            dayOfMonth = (dayOfYear2 - MONTH_COUNT[month][isLeap ? (char) 3 : (char) 2]) + 1;
            dayOfYear = dayOfYear2 + 1;
        }
        internalSet(2, month);
        internalSet(5, dayOfMonth);
        internalSet(6, dayOfYear);
        internalSet(19, eyear);
        int era = 1;
        if (eyear < 1) {
            era = 0;
            eyear = 1 - eyear;
        }
        internalSet(0, era);
        internalSet(1, eyear);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            int year = internalGet(19, EPOCH_YEAR);
            return year;
        }
        int era = internalGet(0, 1);
        if (era == 0) {
            int year2 = 1 - internalGet(1, 1);
            return year2;
        }
        int year3 = internalGet(1, EPOCH_YEAR);
        return year3;
    }

    @Override
    protected int handleComputeJulianDay(int bestField) {
        this.invertGregorian = false;
        int jd = super.handleComputeJulianDay(bestField);
        if (this.isGregorian != (jd >= this.cutoverJulianDay)) {
            this.invertGregorian = true;
            return super.handleComputeJulianDay(bestField);
        }
        return jd;
    }

    @Override
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        if (month < 0 || month > 11) {
            int[] rem = new int[1];
            eyear += floorDivide(month, 12, rem);
            month = rem[0];
        }
        boolean isLeap = eyear % 4 == 0;
        int y = eyear - 1;
        int julianDay = (y * 365) + floorDivide(y, 4) + 1721423;
        this.isGregorian = eyear >= this.gregorianCutoverYear;
        if (this.invertGregorian) {
            this.isGregorian = this.isGregorian ? false : true;
        }
        if (this.isGregorian) {
            isLeap = isLeap && (eyear % 100 != 0 || eyear % 400 == 0);
            julianDay += (floorDivide(y, 400) - floorDivide(y, 100)) + 2;
        }
        if (month != 0) {
            return julianDay + MONTH_COUNT[month][isLeap ? (char) 3 : (char) 2];
        }
        return julianDay;
    }

    @Override
    public String getType() {
        return "gregorian";
    }
}
