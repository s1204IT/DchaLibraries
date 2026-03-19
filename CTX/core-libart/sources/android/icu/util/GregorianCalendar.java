package android.icu.util;

import android.icu.impl.Grego;
import android.icu.lang.UCharacter;
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
    private static final int[][] MONTH_COUNT = {new int[]{31, 31, 0, 0}, new int[]{28, 29, 31, 31}, new int[]{31, 31, 59, 60}, new int[]{30, 30, 90, 91}, new int[]{31, 31, 120, 121}, new int[]{30, 30, 151, 152}, new int[]{31, 31, 181, 182}, new int[]{31, 31, 212, 213}, new int[]{30, 30, 243, 244}, new int[]{31, 31, UCharacter.UnicodeBlock.TANGUT_COMPONENTS_ID, UCharacter.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F_ID}, new int[]{30, 30, 304, 305}, new int[]{31, 31, 334, 335}};
    private static final int[][] LIMITS = {new int[]{0, 0, 1, 1}, new int[]{1, 1, 5828963, 5838270}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 52, 53}, new int[0], new int[]{1, 1, 28, 31}, new int[]{1, 1, 365, 366}, new int[0], new int[]{-1, -1, 4, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5838270, -5838270, 5828964, 5838271}, new int[0], new int[]{-5838269, -5838269, 5828963, 5838270}, new int[0], new int[0], new int[0]};

    @Override
    protected int handleGetLimit(int i, int i2) {
        return LIMITS[i][i2];
    }

    public GregorianCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public GregorianCalendar(TimeZone timeZone) {
        this(timeZone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public GregorianCalendar(Locale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public GregorianCalendar(ULocale uLocale) {
        this(TimeZone.getDefault(), uLocale);
    }

    public GregorianCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(TimeZone timeZone, ULocale uLocale) {
        super(timeZone, uLocale);
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(int i, int i2, int i3) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        set(0, 1);
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    public GregorianCalendar(int i, int i2, int i3, int i4, int i5) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        set(0, 1);
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
    }

    public GregorianCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.gregorianCutover = -12219292800000L;
        this.cutoverJulianDay = 2299161;
        this.gregorianCutoverYear = 1582;
        set(0, 1);
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
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
            GregorianCalendar gregorianCalendar = new GregorianCalendar(getTimeZone());
            gregorianCalendar.setTime(date);
            this.gregorianCutoverYear = gregorianCalendar.get(19);
        }
    }

    public final Date getGregorianChange() {
        return new Date(this.gregorianCutover);
    }

    public boolean isLeapYear(int i) {
        if (i >= this.gregorianCutoverYear) {
            if (i % 4 != 0) {
                return false;
            }
            if (i % 100 == 0 && i % 400 != 0) {
                return false;
            }
        } else if (i % 4 != 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isEquivalentTo(Calendar calendar) {
        return super.isEquivalentTo(calendar) && this.gregorianCutover == ((GregorianCalendar) calendar).gregorianCutover;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ ((int) this.gregorianCutover);
    }

    @Override
    public void roll(int i, int i2) {
        if (i == 3) {
            int i3 = get(3);
            int i4 = get(17);
            int iInternalGet = internalGet(6);
            if (internalGet(2) == 0) {
                if (i3 >= 52) {
                    iInternalGet += handleGetYearLength(i4);
                }
            } else if (i3 == 1) {
                iInternalGet -= handleGetYearLength(i4 - 1);
            }
            int i5 = i3 + i2;
            if (i5 < 1 || i5 > 52) {
                int iHandleGetYearLength = handleGetYearLength(i4);
                int iInternalGet2 = (((iHandleGetYearLength - iInternalGet) + internalGet(7)) - getFirstDayOfWeek()) % 7;
                if (iInternalGet2 < 0) {
                    iInternalGet2 += 7;
                }
                if (6 - iInternalGet2 >= getMinimalDaysInFirstWeek()) {
                    iHandleGetYearLength -= 7;
                }
                int iWeekNumber = weekNumber(iHandleGetYearLength, iInternalGet2 + 1);
                i5 = (((i5 + iWeekNumber) - 1) % iWeekNumber) + 1;
            }
            set(3, i5);
            set(1, i4);
            return;
        }
        super.roll(i, i2);
    }

    @Override
    public int getActualMinimum(int i) {
        return getMinimum(i);
    }

    @Override
    public int getActualMaximum(int i) {
        if (i == 1) {
            Calendar calendar = (Calendar) clone();
            calendar.setLenient(true);
            int i2 = calendar.get(0);
            Date time = calendar.getTime();
            int i3 = LIMITS[1][1];
            int i4 = LIMITS[1][2] + 1;
            while (i3 + 1 < i4) {
                int i5 = (i3 + i4) / 2;
                calendar.set(1, i5);
                if (calendar.get(1) != i5 || calendar.get(0) != i2) {
                    calendar.setTime(time);
                    i4 = i5;
                } else {
                    i3 = i5;
                }
            }
            return i3;
        }
        return super.getActualMaximum(i);
    }

    boolean inDaylightTime() {
        if (!getTimeZone().useDaylightTime()) {
            return false;
        }
        complete();
        return internalGet(16) != 0;
    }

    @Override
    protected int handleGetMonthLength(int i, int i2) {
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        return MONTH_COUNT[i2][isLeapYear(i) ? 1 : 0];
    }

    @Override
    protected int handleGetYearLength(int i) {
        return isLeapYear(i) ? 366 : 365;
    }

    @Override
    protected void handleComputeFields(int i) {
        int i2;
        int gregorianDayOfMonth;
        int gregorianYear;
        int gregorianDayOfYear;
        int gregorianMonth;
        int i3;
        if (i < this.cutoverJulianDay) {
            long j = i - 1721424;
            int iFloorDivide = (int) floorDivide((4 * j) + 1464, 1461L);
            long j2 = ((long) iFloorDivide) - 1;
            int iFloorDivide2 = (int) (j - ((365 * j2) + floorDivide(j2, 4L)));
            boolean z = (iFloorDivide & 3) == 0;
            if (iFloorDivide2 >= (z ? 60 : 59)) {
                i2 = z ? 1 : 2;
            } else {
                i2 = 0;
            }
            int i4 = ((12 * (i2 + iFloorDivide2)) + 6) / 367;
            gregorianDayOfMonth = (iFloorDivide2 - MONTH_COUNT[i4][z ? (char) 3 : (char) 2]) + 1;
            gregorianYear = iFloorDivide;
            gregorianDayOfYear = iFloorDivide2 + 1;
            gregorianMonth = i4;
        } else {
            gregorianMonth = getGregorianMonth();
            gregorianDayOfMonth = getGregorianDayOfMonth();
            gregorianDayOfYear = getGregorianDayOfYear();
            gregorianYear = getGregorianYear();
        }
        internalSet(2, gregorianMonth);
        internalSet(5, gregorianDayOfMonth);
        internalSet(6, gregorianDayOfYear);
        internalSet(19, gregorianYear);
        if (gregorianYear < 1) {
            gregorianYear = 1 - gregorianYear;
            i3 = 0;
        } else {
            i3 = 1;
        }
        internalSet(0, i3);
        internalSet(1, gregorianYear);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            return internalGet(19, EPOCH_YEAR);
        }
        if (internalGet(0, 1) == 0) {
            return 1 - internalGet(1, 1);
        }
        return internalGet(1, EPOCH_YEAR);
    }

    @Override
    protected int handleComputeJulianDay(int i) {
        this.invertGregorian = false;
        int iHandleComputeJulianDay = super.handleComputeJulianDay(i);
        if (this.isGregorian != (iHandleComputeJulianDay >= this.cutoverJulianDay)) {
            this.invertGregorian = true;
            return super.handleComputeJulianDay(i);
        }
        return iHandleComputeJulianDay;
    }

    @Override
    protected int handleComputeMonthStart(int i, int i2, boolean z) {
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        boolean z2 = i % 4 == 0;
        int i3 = i - 1;
        int iFloorDivide = (365 * i3) + floorDivide(i3, 4) + 1721423;
        this.isGregorian = i >= this.gregorianCutoverYear;
        if (this.invertGregorian) {
            this.isGregorian = !this.isGregorian;
        }
        if (this.isGregorian) {
            z2 = z2 && (i % 100 != 0 || i % 400 == 0);
            iFloorDivide += (floorDivide(i3, 400) - floorDivide(i3, 100)) + 2;
        }
        if (i2 != 0) {
            return iFloorDivide + MONTH_COUNT[i2][z2 ? (char) 3 : (char) 2];
        }
        return iFloorDivide;
    }

    @Override
    public String getType() {
        return "gregorian";
    }
}
