package android.icu.util;

import android.icu.lang.UCharacter;
import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;

@Deprecated
public class PersianCalendar extends Calendar {
    private static final int PERSIAN_EPOCH = 1948320;
    private static final long serialVersionUID = -6727306982975111643L;
    private static final int[][] MONTH_COUNT = {new int[]{31, 31, 0}, new int[]{31, 31, 31}, new int[]{31, 31, 62}, new int[]{31, 31, 93}, new int[]{31, 31, 124}, new int[]{31, 31, 155}, new int[]{30, 30, 186}, new int[]{30, 30, 216}, new int[]{30, 30, 246}, new int[]{30, 30, UCharacter.UnicodeBlock.MASARAM_GONDI_ID}, new int[]{30, 30, 306}, new int[]{29, 30, 336}};
    private static final int[][] LIMITS = {new int[]{0, 0, 0, 0}, new int[]{-5000000, -5000000, 5000000, 5000000}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 52, 53}, new int[0], new int[]{1, 1, 29, 31}, new int[]{1, 1, 365, 366}, new int[0], new int[]{-1, -1, 5, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[]{-5000000, -5000000, 5000000, 5000000}, new int[0], new int[0]};

    @Deprecated
    public PersianCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    @Deprecated
    public PersianCalendar(TimeZone timeZone) {
        this(timeZone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    @Deprecated
    public PersianCalendar(Locale locale) {
        this(TimeZone.getDefault(), locale);
    }

    @Deprecated
    public PersianCalendar(ULocale uLocale) {
        this(TimeZone.getDefault(), uLocale);
    }

    @Deprecated
    public PersianCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    @Deprecated
    public PersianCalendar(TimeZone timeZone, ULocale uLocale) {
        super(timeZone, uLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    @Deprecated
    public PersianCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    @Deprecated
    public PersianCalendar(int i, int i2, int i3) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    @Deprecated
    public PersianCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
    }

    @Override
    @Deprecated
    protected int handleGetLimit(int i, int i2) {
        return LIMITS[i][i2];
    }

    private static final boolean isLeapYear(int i) {
        int[] iArr = new int[1];
        floorDivide((25 * i) + 11, 33, iArr);
        if (iArr[0] < 8) {
            return true;
        }
        return false;
    }

    @Override
    @Deprecated
    protected int handleGetMonthLength(int i, int i2) {
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        return MONTH_COUNT[i2][isLeapYear(i) ? 1 : 0];
    }

    @Override
    @Deprecated
    protected int handleGetYearLength(int i) {
        return isLeapYear(i) ? 366 : 365;
    }

    @Override
    @Deprecated
    protected int handleComputeMonthStart(int i, int i2, boolean z) {
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        int iFloorDivide = 1948319 + (365 * (i - 1)) + floorDivide((8 * i) + 21, 33);
        if (i2 != 0) {
            return iFloorDivide + MONTH_COUNT[i2][2];
        }
        return iFloorDivide;
    }

    @Override
    @Deprecated
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            return internalGet(19, 1);
        }
        return internalGet(1, 1);
    }

    @Override
    @Deprecated
    protected void handleComputeFields(int i) {
        int i2;
        long j = i - PERSIAN_EPOCH;
        int iFloorDivide = ((int) floorDivide((33 * j) + 3, 12053L)) + 1;
        long j2 = iFloorDivide;
        int iFloorDivide2 = (int) (j - ((365 * (j2 - 1)) + floorDivide((8 * j2) + 21, 33L)));
        if (iFloorDivide2 < 216) {
            i2 = iFloorDivide2 / 31;
        } else {
            i2 = (iFloorDivide2 - 6) / 30;
        }
        int i3 = (iFloorDivide2 - MONTH_COUNT[i2][2]) + 1;
        internalSet(0, 0);
        internalSet(1, iFloorDivide);
        internalSet(19, iFloorDivide);
        internalSet(2, i2);
        internalSet(5, i3);
        internalSet(6, iFloorDivide2 + 1);
    }

    @Override
    @Deprecated
    public String getType() {
        return "persian";
    }
}
