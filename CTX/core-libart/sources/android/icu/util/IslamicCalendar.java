package android.icu.util;

import android.icu.impl.CalendarAstronomer;
import android.icu.impl.CalendarCache;
import android.icu.impl.CalendarUtil;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.Locale;

public class IslamicCalendar extends Calendar {
    private static final long ASTRONOMICAL_EPOC = 1948439;
    private static final long CIVIL_EPOC = 1948440;
    public static final int DHU_AL_HIJJAH = 11;
    public static final int DHU_AL_QIDAH = 10;
    private static final long HIJRA_MILLIS = -42521587200000L;
    public static final int JUMADA_1 = 4;
    public static final int JUMADA_2 = 5;
    public static final int MUHARRAM = 0;
    public static final int RABI_1 = 2;
    public static final int RABI_2 = 3;
    public static final int RAJAB = 6;
    public static final int RAMADAN = 8;
    public static final int SAFAR = 1;
    public static final int SHABAN = 7;
    public static final int SHAWWAL = 9;
    private static final int UMALQURA_YEAR_END = 1600;
    private static final int UMALQURA_YEAR_START = 1300;
    private static final long serialVersionUID = -6253365474073869325L;
    private CalculationType cType;
    private boolean civil;
    private static final int[][] LIMITS = {new int[]{0, 0, 0, 0}, new int[]{1, 1, 5000000, 5000000}, new int[]{0, 0, 11, 11}, new int[]{1, 1, 50, 51}, new int[0], new int[]{1, 1, 29, 30}, new int[]{1, 1, 354, 355}, new int[0], new int[]{-1, -1, 5, 5}, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[0], new int[]{1, 1, 5000000, 5000000}, new int[0], new int[]{1, 1, 5000000, 5000000}, new int[0], new int[0]};
    private static final int[] UMALQURA_MONTHLENGTH = {2730, 3412, 3785, 1748, 1770, 876, 2733, 1365, 1705, 1938, 2985, 1492, 2778, 1372, 3373, 1685, 1866, 2900, 2922, 1453, 1198, 2639, 1303, 1675, 1701, 2773, 726, 2395, 1181, 2637, 3366, 3477, 1452, 2486, 698, 2651, 1323, 2709, 1738, 2793, 756, 2422, 694, 2390, 2762, 2980, 3026, 1497, 732, 2413, 1357, 2725, 2898, 2981, 1460, 2486, 1367, 663, 1355, 1699, 1874, 2917, 1386, 2731, 1323, 3221, 3402, 3493, 1482, 2774, 2391, 1195, 2379, 2725, 2898, 2922, 1397, 630, 2231, 1115, 1365, 1449, 1460, 2522, 1245, 622, 2358, 2730, 3412, 3506, 1493, 730, 2395, 1195, 2645, 2889, 2916, 2929, 1460, 2741, 2645, 3365, 3730, 3785, 1748, 2793, 2411, 1195, 2707, 3401, 3492, 3506, 2745, 1210, 2651, 1323, 2709, 2858, 2901, 1372, 1213, 573, 2333, 2709, 2890, 2906, 1389, 694, 2363, 1179, 1621, 1705, 1876, 2922, 1388, 2733, 1365, 2857, 2962, 2985, 1492, 2778, 1370, 2731, 1429, 1865, 1892, 2986, 1461, 694, 2646, 3661, 2853, 2898, 2922, 1453, 686, 2351, 1175, 1611, 1701, 1708, 2774, 1373, 1181, 2637, 3350, 3477, 1450, 1461, 730, 2395, 1197, 1429, 1738, 1764, 2794, 1269, 694, 2390, 2730, 2900, 3026, 1497, 746, 2413, 1197, 2709, 2890, 2981, 1458, 2485, 1238, 2711, 1351, 1683, 1865, 2901, 1386, 2667, 1323, 2699, 3398, 3491, 1482, 2774, 1243, 619, 2379, 2725, 2898, 2921, 1397, 374, 2231, 603, 1323, 1381, 1460, 2522, 1261, 365, 2230, 2726, 3410, 3497, 1492, 2778, 2395, 1195, 1619, 1833, 1890, 2985, 1458, 2741, 1365, 2853, 3474, 3785, 1746, 2793, 1387, 1195, 2645, 3369, 3412, 3498, 2485, 1210, 2619, 1179, 2637, 2730, 2773, 730, 2397, 1118, 2606, 3226, 3413, 1714, 1721, 1210, 2653, 1325, 2709, 2898, 2984, 2996, 1465, 730, 2394, 2890, 3492, 3793, 1768, 2922, 1389, 1333, 1685, 3402, 3496, 3540, 1754, 1371, 669, 1579, 2837, 2890, 2965, 1450, 2734, 2350, 3215, 1319, 1685, 1706, 2774, 1373, 669};
    private static final byte[] UMALQURA_YEAR_START_ESTIMATE_FIX = {0, 0, -1, 0, -1, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, -1, -1, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 1, 1, 0, 0, -1, 0, 1, 0, 1, 1, 0, 0, -1, 0, 1, 0, 0, 0, -1, 0, 1, 0, 1, 0, 0, 0, -1, 0, 0, 0, 0, -1, -1, 0, -1, 0, 1, 0, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, -1, -1, 0, 0, 0, 1, 0, 0, -1, -1, 0, -1, 0, 0, -1, -1, 0, -1, 0, -1, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0, 1, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, -1, 0, 1, 0, 0, -1, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 1, 1, 0, 0, -1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, -1, 0, 0, 0, 0, 0, -1, 0, -1, 0, 1, 0, 0, 0, -1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 0, -1, 0, 0, 0, 0, -1, -1, 0, -1, 0, 1, 0, 0, -1, -1, 0, 0, 1, 1, 0, 0, -1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    private static CalendarAstronomer astro = new CalendarAstronomer();
    private static CalendarCache cache = new CalendarCache();

    public IslamicCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public IslamicCalendar(TimeZone timeZone) {
        this(timeZone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public IslamicCalendar(Locale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public IslamicCalendar(ULocale uLocale) {
        this(TimeZone.getDefault(), uLocale);
    }

    public IslamicCalendar(TimeZone timeZone, Locale locale) {
        this(timeZone, ULocale.forLocale(locale));
    }

    public IslamicCalendar(TimeZone timeZone, ULocale uLocale) {
        super(timeZone, uLocale);
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        setCalcTypeForLocale(uLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public IslamicCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        setTime(date);
    }

    public IslamicCalendar(int i, int i2, int i3) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        set(1, i);
        set(2, i2);
        set(5, i3);
    }

    public IslamicCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        set(1, i);
        set(2, i2);
        set(5, i3);
        set(11, i4);
        set(12, i5);
        set(13, i6);
    }

    public void setCivil(boolean z) {
        this.civil = z;
        if (z && this.cType != CalculationType.ISLAMIC_CIVIL) {
            long timeInMillis = getTimeInMillis();
            this.cType = CalculationType.ISLAMIC_CIVIL;
            clear();
            setTimeInMillis(timeInMillis);
            return;
        }
        if (!z && this.cType != CalculationType.ISLAMIC) {
            long timeInMillis2 = getTimeInMillis();
            this.cType = CalculationType.ISLAMIC;
            clear();
            setTimeInMillis(timeInMillis2);
        }
    }

    public boolean isCivil() {
        if (this.cType == CalculationType.ISLAMIC_CIVIL) {
            return true;
        }
        return false;
    }

    @Override
    protected int handleGetLimit(int i, int i2) {
        return LIMITS[i][i2];
    }

    private static final boolean civilLeapYear(int i) {
        return (14 + (i * 11)) % 30 < 11;
    }

    private long yearStart(int i) {
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && (i < UMALQURA_YEAR_START || i > UMALQURA_YEAR_END))) {
            return ((long) ((i - 1) * 354)) + ((long) Math.floor(((double) (3 + (11 * i))) / 30.0d));
        }
        if (this.cType == CalculationType.ISLAMIC) {
            return trueMonthStart(12 * (i - 1));
        }
        if (this.cType == CalculationType.ISLAMIC_UMALQURA) {
            int i2 = i - 1300;
            return ((int) ((354.3672d * ((double) i2)) + 460322.05d + 0.5d)) + UMALQURA_YEAR_START_ESTIMATE_FIX[i2];
        }
        return 0L;
    }

    private long monthStart(int i, int i2) {
        int i3 = (i2 / 12) + i;
        int i4 = i2 % 12;
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && i < UMALQURA_YEAR_START)) {
            return ((long) Math.ceil(29.5d * ((double) i4))) + ((long) ((i3 - 1) * 354)) + ((long) Math.floor(((double) (3 + (11 * i3))) / 30.0d));
        }
        if (this.cType == CalculationType.ISLAMIC) {
            return trueMonthStart((12 * (i3 - 1)) + i4);
        }
        if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            return 0L;
        }
        long jYearStart = yearStart(i);
        for (int i5 = 0; i5 < i2; i5++) {
            jYearStart += (long) handleGetMonthLength(i, i5);
        }
        return jYearStart;
    }

    private static final long trueMonthStart(long j) {
        long j2 = cache.get(j);
        if (j2 == CalendarCache.EMPTY) {
            long jFloor = (((long) Math.floor(j * 29.530588853d)) * 86400000) + HIJRA_MILLIS;
            moonAge(jFloor);
            if (moonAge(jFloor) >= 0.0d) {
                do {
                    jFloor -= 86400000;
                } while (moonAge(jFloor) >= 0.0d);
            } else {
                do {
                    jFloor += 86400000;
                } while (moonAge(jFloor) < 0.0d);
            }
            long j3 = ((jFloor - HIJRA_MILLIS) / 86400000) + 1;
            cache.put(j, j3);
            return j3;
        }
        return j2;
    }

    static final double moonAge(long j) {
        double moonAge;
        synchronized (astro) {
            astro.setTime(j);
            moonAge = astro.getMoonAge();
        }
        double d = (moonAge * 180.0d) / 3.141592653589793d;
        if (d > 180.0d) {
            return d - 360.0d;
        }
        return d;
    }

    @Override
    protected int handleGetMonthLength(int i, int i2) {
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && (i < UMALQURA_YEAR_START || i > UMALQURA_YEAR_END))) {
            int i3 = 29 + ((i2 + 1) % 2);
            if (i2 == 11 && civilLeapYear(i)) {
                return i3 + 1;
            }
            return i3;
        }
        if (this.cType != CalculationType.ISLAMIC) {
            return (UMALQURA_MONTHLENGTH[i - UMALQURA_YEAR_START] & (1 << (11 - i2))) == 0 ? 29 : 30;
        }
        return (int) (trueMonthStart(r0 + 1) - trueMonthStart((12 * (i - 1)) + i2));
    }

    @Override
    protected int handleGetYearLength(int i) {
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && (i < UMALQURA_YEAR_START || i > UMALQURA_YEAR_END))) {
            return 354 + (civilLeapYear(i) ? 1 : 0);
        }
        if (this.cType == CalculationType.ISLAMIC) {
            return (int) (trueMonthStart(r3 + 12) - trueMonthStart(12 * (i - 1)));
        }
        if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            return 0;
        }
        int iHandleGetMonthLength = 0;
        for (int i2 = 0; i2 < 12; i2++) {
            iHandleGetMonthLength += handleGetMonthLength(i, i2);
        }
        return iHandleGetMonthLength;
    }

    @Override
    protected int handleComputeMonthStart(int i, int i2, boolean z) {
        return (int) ((monthStart(i, i2) + (this.cType == CalculationType.ISLAMIC_TBLA ? ASTRONOMICAL_EPOC : CIVIL_EPOC)) - 1);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19) {
            return internalGet(19, 1);
        }
        return internalGet(1, 1);
    }

    @Override
    protected void handleComputeFields(int i) {
        int iFloor;
        int iMin;
        int i2;
        long j = i;
        long j2 = j - CIVIL_EPOC;
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA) {
            if (this.cType == CalculationType.ISLAMIC_TBLA) {
                j2 = j - ASTRONOMICAL_EPOC;
            }
            iFloor = (int) Math.floor(((30 * j2) + 10646) / 10631.0d);
            iMin = Math.min((int) Math.ceil(((j2 - 29) - yearStart(iFloor)) / 29.5d), 11);
        } else if (this.cType == CalculationType.ISLAMIC) {
            int iFloor2 = (int) Math.floor(j2 / 29.530588853d);
            if (j2 - ((long) Math.floor((((double) iFloor2) * 29.530588853d) - 1.0d)) >= 25 && moonAge(internalGetTimeInMillis()) > 0.0d) {
                iFloor2++;
            }
            while (trueMonthStart(iFloor2) > j2) {
                iFloor2--;
            }
            iMin = iFloor2 % 12;
            iFloor = (iFloor2 / 12) + 1;
        } else if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            iFloor = 0;
            iMin = 0;
        } else if (j2 < yearStart(UMALQURA_YEAR_START)) {
            iFloor = (int) Math.floor(((30 * j2) + 10646) / 10631.0d);
            iMin = Math.min((int) Math.ceil(((j2 - 29) - yearStart(iFloor)) / 29.5d), 11);
        } else {
            int i3 = 1299;
            long jYearStart = 1;
            while (true) {
                if (jYearStart <= 0) {
                    i2 = 0;
                    break;
                }
                i3++;
                jYearStart = (j2 - yearStart(i3)) + 1;
                if (jYearStart == handleGetYearLength(i3)) {
                    i2 = 11;
                    break;
                }
                if (jYearStart < handleGetYearLength(i3)) {
                    int iHandleGetMonthLength = handleGetMonthLength(i3, 0);
                    int i4 = 0;
                    while (true) {
                        long j3 = iHandleGetMonthLength;
                        if (jYearStart <= j3) {
                            break;
                        }
                        jYearStart -= j3;
                        i4++;
                        iHandleGetMonthLength = handleGetMonthLength(i3, i4);
                    }
                    i2 = i4;
                }
            }
            iFloor = i3;
            iMin = i2;
        }
        int iMonthStart = ((int) (j2 - monthStart(iFloor, iMin))) + 1;
        int iMonthStart2 = (int) ((j2 - monthStart(iFloor, 0)) + 1);
        internalSet(0, 0);
        internalSet(1, iFloor);
        internalSet(19, iFloor);
        internalSet(2, iMin);
        internalSet(5, iMonthStart);
        internalSet(6, iMonthStart2);
    }

    public enum CalculationType {
        ISLAMIC("islamic"),
        ISLAMIC_CIVIL("islamic-civil"),
        ISLAMIC_UMALQURA("islamic-umalqura"),
        ISLAMIC_TBLA("islamic-tbla");

        private String bcpType;

        CalculationType(String str) {
            this.bcpType = str;
        }

        String bcpType() {
            return this.bcpType;
        }
    }

    public void setCalculationType(CalculationType calculationType) {
        this.cType = calculationType;
        if (this.cType == CalculationType.ISLAMIC_CIVIL) {
            this.civil = true;
        } else {
            this.civil = false;
        }
    }

    public CalculationType getCalculationType() {
        return this.cType;
    }

    private void setCalcTypeForLocale(ULocale uLocale) {
        String calendarType = CalendarUtil.getCalendarType(uLocale);
        if ("islamic-civil".equals(calendarType)) {
            setCalculationType(CalculationType.ISLAMIC_CIVIL);
            return;
        }
        if ("islamic-umalqura".equals(calendarType)) {
            setCalculationType(CalculationType.ISLAMIC_UMALQURA);
            return;
        }
        if ("islamic-tbla".equals(calendarType)) {
            setCalculationType(CalculationType.ISLAMIC_TBLA);
        } else if (calendarType.startsWith("islamic")) {
            setCalculationType(CalculationType.ISLAMIC);
        } else {
            setCalculationType(CalculationType.ISLAMIC_CIVIL);
        }
    }

    @Override
    public String getType() {
        if (this.cType == null) {
            return "islamic";
        }
        return this.cType.bcpType();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.cType == null) {
            this.cType = this.civil ? CalculationType.ISLAMIC_CIVIL : CalculationType.ISLAMIC;
        } else {
            this.civil = this.cType == CalculationType.ISLAMIC_CIVIL;
        }
    }
}
