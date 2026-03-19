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

    public IndianCalendar(TimeZone timeZone) {
        this(timeZone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public IndianCalendar(Locale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public IndianCalendar(ULocale uLocale) {
        this(TimeZone.getDefault(), uLocale);
    }

    public IndianCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public IndianCalendar(TimeZone timeZone, ULocale uLocale) {
        super(timeZone, uLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public IndianCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        setTime(date);
    }

    public IndianCalendar(int i, int i2, int i3) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    public IndianCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            return internalGet(19, 1);
        }
        return internalGet(1, 1);
    }

    @Override
    protected int handleGetYearLength(int i) {
        return super.handleGetYearLength(i);
    }

    @Override
    protected int handleGetMonthLength(int i, int i2) {
        if (i2 < 0 || i2 > 11) {
            int[] iArr = new int[1];
            i += floorDivide(i2, 12, iArr);
            i2 = iArr[0];
        }
        if (isGregorianLeap(i + 78) && i2 == 0) {
            return 31;
        }
        return (i2 < 1 || i2 > 5) ? 30 : 31;
    }

    @Override
    protected void handleComputeFields(int i) {
        int i2;
        int i3;
        int i4;
        int i5;
        double d = i;
        int[] iArrJdToGregorian = jdToGregorian(d);
        int i6 = iArrJdToGregorian[0] - 78;
        int iGregorianToJD = (int) (d - gregorianToJD(iArrJdToGregorian[0], 1, 1));
        if (iGregorianToJD < 80) {
            i6--;
            i2 = isGregorianLeap(iArrJdToGregorian[0] - 1) ? 31 : 30;
            i3 = iGregorianToJD + i2 + 155 + 90 + 10;
        } else {
            i2 = isGregorianLeap(iArrJdToGregorian[0]) ? 31 : 30;
            i3 = iGregorianToJD - 80;
        }
        if (i3 < i2) {
            i4 = i3 + 1;
            i5 = 0;
        } else {
            int i7 = i3 - i2;
            if (i7 < 155) {
                i5 = (i7 / 31) + 1;
                i4 = (i7 % 31) + 1;
            } else {
                int i8 = i7 - 155;
                int i9 = (i8 / 30) + 6;
                i4 = (i8 % 30) + 1;
                i5 = i9;
            }
        }
        internalSet(0, 0);
        internalSet(19, i6);
        internalSet(1, i6);
        internalSet(2, i5);
        internalSet(5, i4);
        internalSet(6, i3 + 1);
    }

    @Override
    protected int handleGetLimit(int i, int i2) {
        return LIMITS[i][i2];
    }

    @Override
    protected int handleComputeMonthStart(int i, int i2, boolean z) {
        if (i2 < 0 || i2 > 11) {
            i += i2 / 12;
            i2 %= 12;
        }
        return (int) IndianToJD(i, i2 + 1, 1);
    }

    private static double IndianToJD(int i, int i2, int i3) {
        double dGregorianToJD;
        int i4;
        int i5 = i + 78;
        if (isGregorianLeap(i5)) {
            dGregorianToJD = gregorianToJD(i5, 3, 21);
            i4 = 31;
        } else {
            dGregorianToJD = gregorianToJD(i5, 3, 22);
            i4 = 30;
        }
        if (i2 == 1) {
            return dGregorianToJD + ((double) (i3 - 1));
        }
        double dMin = dGregorianToJD + ((double) i4) + ((double) (Math.min(i2 - 2, 5) * 31));
        if (i2 >= 8) {
            dMin += (double) ((i2 - 7) * 30);
        }
        return dMin + ((double) (i3 - 1));
    }

    private static double gregorianToJD(int i, int i2, int i3) {
        int i4 = i - 1;
        return ((double) ((((((((365 * i4) + (i4 / 4)) - (i4 / 100)) + (i4 / 400)) + (((367 * i2) - 362) / 12)) + (i2 <= 2 ? 0 : isGregorianLeap(i) ? -1 : -2)) + i3) - 1)) + 1721425.5d;
    }

    private static int[] jdToGregorian(double d) {
        int i;
        double dFloor = Math.floor(d - 0.5d) + 0.5d;
        double d2 = dFloor - 1721425.5d;
        double dFloor2 = Math.floor(d2 / 146097.0d);
        double d3 = d2 % 146097.0d;
        double dFloor3 = Math.floor(d3 / 36524.0d);
        double d4 = d3 % 36524.0d;
        double dFloor4 = Math.floor(d4 / 1461.0d);
        double dFloor5 = Math.floor((d4 % 1461.0d) / 365.0d);
        int i2 = (int) ((dFloor2 * 400.0d) + (100.0d * dFloor3) + (dFloor4 * 4.0d) + dFloor5);
        if (dFloor3 != 4.0d && dFloor5 != 4.0d) {
            i2++;
        }
        double dGregorianToJD = dFloor - gregorianToJD(i2, 1, 1);
        if (dFloor >= gregorianToJD(i2, 3, 1)) {
            i = isGregorianLeap(i2) ? 1 : 2;
        } else {
            i = 0;
        }
        int iFloor = (int) Math.floor((((dGregorianToJD + ((double) i)) * 12.0d) + 373.0d) / 367.0d);
        return new int[]{i2, iFloor, ((int) (dFloor - gregorianToJD(i2, iFloor, 1))) + 1};
    }

    private static boolean isGregorianLeap(int i) {
        return i % 4 == 0 && (i % 100 != 0 || i % 400 == 0);
    }

    @Override
    public String getType() {
        return "indian";
    }
}
