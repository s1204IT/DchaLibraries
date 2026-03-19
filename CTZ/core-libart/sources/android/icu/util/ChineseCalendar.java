package android.icu.util;

import android.icu.impl.CalendarAstronomer;
import android.icu.impl.CalendarCache;
import android.icu.text.DateFormat;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.Locale;

public class ChineseCalendar extends Calendar {
    private static final int CHINESE_EPOCH_YEAR = -2636;
    private static final int SYNODIC_GAP = 25;
    private static final long serialVersionUID = 7312110751940929420L;
    private transient CalendarAstronomer astro;
    private int epochYear;
    private transient boolean isLeapYear;
    private transient CalendarCache newYearCache;
    private transient CalendarCache winterSolsticeCache;
    private TimeZone zoneAstro;
    private static final int[][] LIMITS = {new int[]{1, 1, 83333, 83333}, new int[]{1, 1, 60, 60}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 50, 55}, new int[0], new int[]{1, 1, 29, 30}, new int[]{1, 1, 353, 385}, new int[0], new int[]{-1, -1, 5, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[0], new int[]{0, 0, 1, 1}};
    static final int[][][] CHINESE_DATE_PRECEDENCE = {new int[][]{new int[]{5}, new int[]{3, 7}, new int[]{4, 7}, new int[]{8, 7}, new int[]{3, 18}, new int[]{4, 18}, new int[]{8, 18}, new int[]{6}, new int[]{37, 22}}, new int[][]{new int[]{3}, new int[]{4}, new int[]{8}, new int[]{40, 7}, new int[]{40, 18}}};
    private static final TimeZone CHINA_ZONE = new SimpleTimeZone(28800000, "CHINA_ZONE").freeze();

    public ChineseCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT), CHINESE_EPOCH_YEAR, CHINA_ZONE);
    }

    public ChineseCalendar(Date date) {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT), CHINESE_EPOCH_YEAR, CHINA_ZONE);
        setTime(date);
    }

    public ChineseCalendar(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, 0, 0, 0);
    }

    public ChineseCalendar(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT), CHINESE_EPOCH_YEAR, CHINA_ZONE);
        set(14, 0);
        set(1, i);
        set(2, i2);
        set(22, i3);
        set(5, i4);
        set(11, i5);
        set(12, i6);
        set(13, i7);
    }

    public ChineseCalendar(int i, int i2, int i3, int i4, int i5) {
        this(i, i2, i3, i4, i5, 0, 0, 0);
    }

    public ChineseCalendar(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT), CHINESE_EPOCH_YEAR, CHINA_ZONE);
        set(14, 0);
        set(0, i);
        set(1, i2);
        set(2, i3);
        set(22, i4);
        set(5, i5);
        set(11, i6);
        set(12, i7);
        set(13, i8);
    }

    public ChineseCalendar(Locale locale) {
        this(TimeZone.getDefault(), ULocale.forLocale(locale), CHINESE_EPOCH_YEAR, CHINA_ZONE);
    }

    public ChineseCalendar(TimeZone timeZone) {
        this(timeZone, ULocale.getDefault(ULocale.Category.FORMAT), CHINESE_EPOCH_YEAR, CHINA_ZONE);
    }

    public ChineseCalendar(TimeZone timeZone, Locale locale) {
        this(timeZone, ULocale.forLocale(locale), CHINESE_EPOCH_YEAR, CHINA_ZONE);
    }

    public ChineseCalendar(ULocale uLocale) {
        this(TimeZone.getDefault(), uLocale, CHINESE_EPOCH_YEAR, CHINA_ZONE);
    }

    public ChineseCalendar(TimeZone timeZone, ULocale uLocale) {
        this(timeZone, uLocale, CHINESE_EPOCH_YEAR, CHINA_ZONE);
    }

    @Deprecated
    protected ChineseCalendar(TimeZone timeZone, ULocale uLocale, int i, TimeZone timeZone2) {
        super(timeZone, uLocale);
        this.astro = new CalendarAstronomer();
        this.winterSolsticeCache = new CalendarCache();
        this.newYearCache = new CalendarCache();
        this.epochYear = i;
        this.zoneAstro = timeZone2;
        setTimeInMillis(System.currentTimeMillis());
    }

    @Override
    protected int handleGetLimit(int i, int i2) {
        return LIMITS[i][i2];
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newestStamp(0, 1, 0) <= getStamp(19)) {
            return internalGet(19, 1);
        }
        return (((internalGet(0, 1) - 1) * 60) + internalGet(1, 1)) - (this.epochYear + 2636);
    }

    @Override
    protected int handleGetMonthLength(int i, int i2) {
        int iHandleComputeMonthStart = (handleComputeMonthStart(i, i2, true) - 2440588) + 1;
        return newMoonNear(iHandleComputeMonthStart + 25, true) - iHandleComputeMonthStart;
    }

    @Override
    protected DateFormat handleGetDateFormat(String str, String str2, ULocale uLocale) {
        return super.handleGetDateFormat(str, str2, uLocale);
    }

    @Override
    protected int[][][] getFieldResolutionTable() {
        return CHINESE_DATE_PRECEDENCE;
    }

    private void offsetMonth(int i, int i2, int i3) {
        int iNewMoonNear = ((newMoonNear(i + ((int) (29.530588853d * (((double) i3) - 0.5d))), true) + 2440588) - 1) + i2;
        if (i2 > 29) {
            set(20, iNewMoonNear - 1);
            complete();
            if (getActualMaximum(5) >= i2) {
                set(20, iNewMoonNear);
                return;
            }
            return;
        }
        set(20, iNewMoonNear);
    }

    @Override
    public void add(int i, int i2) {
        if (i == 2) {
            if (i2 != 0) {
                int i3 = get(5);
                offsetMonth(((get(20) - 2440588) - i3) + 1, i3, i2);
                return;
            }
            return;
        }
        super.add(i, i2);
    }

    @Override
    public void roll(int i, int i2) {
        if (i == 2) {
            if (i2 != 0) {
                int i3 = get(5);
                int i4 = ((get(20) - 2440588) - i3) + 1;
                int i5 = get(2);
                if (this.isLeapYear && (get(22) == 1 || isLeapMonthBetween(newMoonNear(i4 - ((int) (29.530588853d * (((double) i5) - 0.5d))), true), i4))) {
                    i5++;
                }
                int i6 = this.isLeapYear ? 13 : 12;
                int i7 = (i2 + i5) % i6;
                if (i7 < 0) {
                    i7 += i6;
                }
                if (i7 != i5) {
                    offsetMonth(i4, i3, i7 - i5);
                    return;
                }
                return;
            }
            return;
        }
        super.roll(i, i2);
    }

    private final long daysToMillis(int i) {
        long j = ((long) i) * 86400000;
        return j - ((long) this.zoneAstro.getOffset(j));
    }

    private final int millisToDays(long j) {
        return (int) floorDivide(j + ((long) this.zoneAstro.getOffset(j)), 86400000L);
    }

    private int winterSolstice(int i) {
        long j = i;
        long jMillisToDays = this.winterSolsticeCache.get(j);
        if (jMillisToDays == CalendarCache.EMPTY) {
            this.astro.setTime(daysToMillis((computeGregorianMonthStart(i, 11) + 1) - 2440588));
            jMillisToDays = millisToDays(this.astro.getSunTime(CalendarAstronomer.WINTER_SOLSTICE, true));
            this.winterSolsticeCache.put(j, jMillisToDays);
        }
        return (int) jMillisToDays;
    }

    private int newMoonNear(int i, boolean z) {
        this.astro.setTime(daysToMillis(i));
        return millisToDays(this.astro.getMoonTime(CalendarAstronomer.NEW_MOON, z));
    }

    private int synodicMonthsBetween(int i, int i2) {
        return (int) Math.round(((double) (i2 - i)) / 29.530588853d);
    }

    private int majorSolarTerm(int i) {
        this.astro.setTime(daysToMillis(i));
        int iFloor = (((int) Math.floor((6.0d * this.astro.getSunLongitude()) / 3.141592653589793d)) + 2) % 12;
        if (iFloor < 1) {
            return iFloor + 12;
        }
        return iFloor;
    }

    private boolean hasNoMajorSolarTerm(int i) {
        return majorSolarTerm(i) == majorSolarTerm(newMoonNear(i + 25, true));
    }

    private boolean isLeapMonthBetween(int i, int i2) {
        if (synodicMonthsBetween(i, i2) < 50) {
            if (i2 >= i) {
                return isLeapMonthBetween(i, newMoonNear(i2 + (-25), false)) || hasNoMajorSolarTerm(i2);
            }
            return false;
        }
        throw new IllegalArgumentException("isLeapMonthBetween(" + i + ", " + i2 + "): Invalid parameters");
    }

    @Override
    protected void handleComputeFields(int i) {
        computeChineseFields(i - 2440588, getGregorianYear(), getGregorianMonth(), true);
    }

    private void computeChineseFields(int i, int i2, int i3, boolean z) {
        int iWinterSolstice;
        int iWinterSolstice2 = winterSolstice(i2);
        if (i < iWinterSolstice2) {
            iWinterSolstice = iWinterSolstice2;
            iWinterSolstice2 = winterSolstice(i2 - 1);
        } else {
            iWinterSolstice = winterSolstice(i2 + 1);
        }
        int iNewMoonNear = newMoonNear(iWinterSolstice2 + 1, true);
        int iNewMoonNear2 = newMoonNear(iWinterSolstice + 1, false);
        int iNewMoonNear3 = newMoonNear(i + 1, false);
        this.isLeapYear = synodicMonthsBetween(iNewMoonNear, iNewMoonNear2) == 12;
        int iSynodicMonthsBetween = synodicMonthsBetween(iNewMoonNear, iNewMoonNear3);
        if (this.isLeapYear && isLeapMonthBetween(iNewMoonNear, iNewMoonNear3)) {
            iSynodicMonthsBetween--;
        }
        if (iSynodicMonthsBetween < 1) {
            iSynodicMonthsBetween += 12;
        }
        int i4 = (this.isLeapYear && hasNoMajorSolarTerm(iNewMoonNear3) && !isLeapMonthBetween(iNewMoonNear, newMoonNear(iNewMoonNear3 + (-25), false))) ? 1 : 0;
        internalSet(2, iSynodicMonthsBetween - 1);
        internalSet(22, i4);
        if (z) {
            int i5 = i2 - this.epochYear;
            int i6 = i2 + 2636;
            if (iSynodicMonthsBetween < 11 || i3 >= 6) {
                i5++;
                i6++;
            }
            internalSet(19, i5);
            int[] iArr = new int[1];
            internalSet(0, floorDivide(i6 - 1, 60, iArr) + 1);
            internalSet(1, iArr[0] + 1);
            internalSet(5, (i - iNewMoonNear3) + 1);
            int iNewYear = newYear(i2);
            if (i < iNewYear) {
                iNewYear = newYear(i2 - 1);
            }
            internalSet(6, (i - iNewYear) + 1);
        }
    }

    private int newYear(int i) {
        long j = i;
        long jNewMoonNear = this.newYearCache.get(j);
        if (jNewMoonNear == CalendarCache.EMPTY) {
            int iWinterSolstice = winterSolstice(i - 1);
            int iWinterSolstice2 = winterSolstice(i);
            int iNewMoonNear = newMoonNear(iWinterSolstice + 1, true);
            int iNewMoonNear2 = newMoonNear(iNewMoonNear + 25, true);
            if (synodicMonthsBetween(iNewMoonNear, newMoonNear(iWinterSolstice2 + 1, false)) == 12 && (hasNoMajorSolarTerm(iNewMoonNear) || hasNoMajorSolarTerm(iNewMoonNear2))) {
                jNewMoonNear = newMoonNear(iNewMoonNear2 + 25, true);
            } else {
                jNewMoonNear = iNewMoonNear2;
            }
            this.newYearCache.put(j, jNewMoonNear);
        }
        return (int) jNewMoonNear;
    }

    @Override
    protected int handleComputeMonthStart(int i, int i2, boolean z) {
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        int iNewMoonNear = newMoonNear(newYear((i + this.epochYear) - 1) + (i2 * 29), true);
        int iNewMoonNear2 = iNewMoonNear + 2440588;
        int iInternalGet = internalGet(2);
        int iInternalGet2 = internalGet(22);
        int i3 = z ? iInternalGet2 : 0;
        computeGregorianFields(iNewMoonNear2);
        computeChineseFields(iNewMoonNear, getGregorianYear(), getGregorianMonth(), false);
        if (i2 != internalGet(2) || i3 != internalGet(22)) {
            iNewMoonNear2 = newMoonNear(iNewMoonNear + 25, true) + 2440588;
        }
        internalSet(2, iInternalGet);
        internalSet(22, iInternalGet2);
        return iNewMoonNear2 - 1;
    }

    @Override
    public String getType() {
        return "chinese";
    }

    @Override
    @Deprecated
    public boolean haveDefaultCentury() {
        return false;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        this.epochYear = CHINESE_EPOCH_YEAR;
        this.zoneAstro = CHINA_ZONE;
        objectInputStream.defaultReadObject();
        this.astro = new CalendarAstronomer();
        this.winterSolsticeCache = new CalendarCache();
        this.newYearCache = new CalendarCache();
    }
}
