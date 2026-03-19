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

    public IslamicCalendar(TimeZone zone) {
        this(zone, ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public IslamicCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    public IslamicCalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    public IslamicCalendar(TimeZone zone, Locale aLocale) {
        this(zone, ULocale.forLocale(aLocale));
    }

    public IslamicCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        setCalcTypeForLocale(locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    public IslamicCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        setTime(date);
    }

    public IslamicCalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        set(1, year);
        set(2, month);
        set(5, date);
    }

    public IslamicCalendar(int year, int month, int date, int hour, int minute, int second) {
        super(TimeZone.getDefault(), ULocale.getDefault(ULocale.Category.FORMAT));
        this.civil = true;
        this.cType = CalculationType.ISLAMIC_CIVIL;
        set(1, year);
        set(2, month);
        set(5, date);
        set(11, hour);
        set(12, minute);
        set(13, second);
    }

    public void setCivil(boolean beCivil) {
        this.civil = beCivil;
        if (beCivil && this.cType != CalculationType.ISLAMIC_CIVIL) {
            long m = getTimeInMillis();
            this.cType = CalculationType.ISLAMIC_CIVIL;
            clear();
            setTimeInMillis(m);
            return;
        }
        if (beCivil || this.cType == CalculationType.ISLAMIC) {
            return;
        }
        long m2 = getTimeInMillis();
        this.cType = CalculationType.ISLAMIC;
        clear();
        setTimeInMillis(m2);
    }

    public boolean isCivil() {
        if (this.cType == CalculationType.ISLAMIC_CIVIL) {
            return true;
        }
        return false;
    }

    @Override
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    private static final boolean civilLeapYear(int year) {
        return ((year * 11) + 14) % 30 < 11;
    }

    private long yearStart(int year) {
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && (year < UMALQURA_YEAR_START || year > UMALQURA_YEAR_END))) {
            long ys = ((long) ((year - 1) * 354)) + ((long) Math.floor(((double) ((year * 11) + 3)) / 30.0d));
            return ys;
        }
        if (this.cType == CalculationType.ISLAMIC) {
            long ys2 = trueMonthStart((year - 1) * 12);
            return ys2;
        }
        if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            return 0L;
        }
        int year2 = year - 1300;
        int yrStartLinearEstimate = (int) ((((double) year2) * 354.3672d) + 460322.05d + 0.5d);
        long ys3 = UMALQURA_YEAR_START_ESTIMATE_FIX[year2] + yrStartLinearEstimate;
        return ys3;
    }

    private long monthStart(int year, int month) {
        int realYear = year + (month / 12);
        int realMonth = month % 12;
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && year < UMALQURA_YEAR_START)) {
            long ms = ((long) Math.ceil(((double) realMonth) * 29.5d)) + ((long) ((realYear - 1) * 354)) + ((long) Math.floor(((double) ((realYear * 11) + 3)) / 30.0d));
            return ms;
        }
        if (this.cType == CalculationType.ISLAMIC) {
            long ms2 = trueMonthStart(((realYear - 1) * 12) + realMonth);
            return ms2;
        }
        if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            return 0L;
        }
        long ms3 = yearStart(year);
        for (int i = 0; i < month; i++) {
            ms3 += (long) handleGetMonthLength(year, i);
        }
        return ms3;
    }

    private static final long trueMonthStart(long month) {
        double age;
        double age2;
        long start = cache.get(month);
        if (start == CalendarCache.EMPTY) {
            long origin = HIJRA_MILLIS + (((long) Math.floor(month * 29.530588853d)) * 86400000);
            moonAge(origin);
            if (moonAge(origin) >= 0.0d) {
                do {
                    origin -= 86400000;
                    age2 = moonAge(origin);
                } while (age2 >= 0.0d);
            } else {
                do {
                    origin += 86400000;
                    age = moonAge(origin);
                } while (age < 0.0d);
            }
            long start2 = ((origin - HIJRA_MILLIS) / 86400000) + 1;
            cache.put(month, start2);
            return start2;
        }
        return start;
    }

    static final double moonAge(long time) {
        double age;
        synchronized (astro) {
            astro.setTime(time);
            age = astro.getMoonAge();
        }
        double age2 = (age * 180.0d) / 3.141592653589793d;
        if (age2 > 180.0d) {
            return age2 - 360.0d;
        }
        return age2;
    }

    @Override
    protected int handleGetMonthLength(int extendedYear, int month) {
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && (extendedYear < UMALQURA_YEAR_START || extendedYear > UMALQURA_YEAR_END))) {
            int length = ((month + 1) % 2) + 29;
            if (month == 11 && civilLeapYear(extendedYear)) {
                return length + 1;
            }
            return length;
        }
        if (this.cType == CalculationType.ISLAMIC) {
            return (int) (trueMonthStart(r10 + 1) - trueMonthStart(month + ((extendedYear - 1) * 12)));
        }
        if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            return 0;
        }
        int idx = extendedYear - 1300;
        int mask = 1 << (11 - month);
        if ((UMALQURA_MONTHLENGTH[idx] & mask) == 0) {
            return 29;
        }
        return 30;
    }

    @Override
    protected int handleGetYearLength(int extendedYear) {
        int length = 0;
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA || (this.cType == CalculationType.ISLAMIC_UMALQURA && (extendedYear < UMALQURA_YEAR_START || extendedYear > UMALQURA_YEAR_END))) {
            int length2 = (civilLeapYear(extendedYear) ? 1 : 0) + 354;
            return length2;
        }
        if (this.cType == CalculationType.ISLAMIC) {
            int month = (extendedYear - 1) * 12;
            int length3 = (int) (trueMonthStart(month + 12) - trueMonthStart(month));
            return length3;
        }
        if (this.cType != CalculationType.ISLAMIC_UMALQURA) {
            return 0;
        }
        for (int i = 0; i < 12; i++) {
            length += handleGetMonthLength(extendedYear, i);
        }
        return length;
    }

    @Override
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        return (int) (((this.cType == CalculationType.ISLAMIC_TBLA ? ASTRONOMICAL_EPOC : CIVIL_EPOC) + monthStart(eyear, month)) - 1);
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
    protected void handleComputeFields(int julianDay) {
        int year = 0;
        int month = 0;
        long days = ((long) julianDay) - CIVIL_EPOC;
        if (this.cType == CalculationType.ISLAMIC_CIVIL || this.cType == CalculationType.ISLAMIC_TBLA) {
            if (this.cType == CalculationType.ISLAMIC_TBLA) {
                days = ((long) julianDay) - ASTRONOMICAL_EPOC;
            }
            year = (int) Math.floor(((30 * days) + 10646) / 10631.0d);
            int month2 = (int) Math.ceil(((days - 29) - yearStart(year)) / 29.5d);
            month = Math.min(month2, 11);
        } else if (this.cType == CalculationType.ISLAMIC) {
            int months = (int) Math.floor(days / 29.530588853d);
            long monthStart = (long) Math.floor((((double) months) * 29.530588853d) - 1.0d);
            if (days - monthStart >= 25 && moonAge(internalGetTimeInMillis()) > 0.0d) {
                months++;
            }
            while (monthStart > days) {
                months--;
            }
            year = (months / 12) + 1;
            month = months % 12;
        } else if (this.cType == CalculationType.ISLAMIC_UMALQURA) {
            long umalquraStartdays = yearStart(UMALQURA_YEAR_START);
            if (days < umalquraStartdays) {
                year = (int) Math.floor(((30 * days) + 10646) / 10631.0d);
                int month3 = (int) Math.ceil(((days - 29) - yearStart(year)) / 29.5d);
                month = Math.min(month3, 11);
            } else {
                int y = 1299;
                int m = 0;
                long d = 1;
                while (true) {
                    if (d <= 0) {
                        break;
                    }
                    y++;
                    d = (days - yearStart(y)) + 1;
                    if (d == handleGetYearLength(y)) {
                        m = 11;
                        break;
                    } else if (d < handleGetYearLength(y)) {
                        int monthLen = handleGetMonthLength(y, 0);
                        m = 0;
                        while (d > monthLen) {
                            d -= (long) monthLen;
                            m++;
                            monthLen = handleGetMonthLength(y, m);
                        }
                    }
                }
                year = y;
                month = m;
            }
        }
        int dayOfMonth = ((int) (days - monthStart(year, month))) + 1;
        int dayOfYear = (int) ((days - monthStart(year, 0)) + 1);
        internalSet(0, 0);
        internalSet(1, year);
        internalSet(19, year);
        internalSet(2, month);
        internalSet(5, dayOfMonth);
        internalSet(6, dayOfYear);
    }

    public enum CalculationType {
        ISLAMIC("islamic"),
        ISLAMIC_CIVIL("islamic-civil"),
        ISLAMIC_UMALQURA("islamic-umalqura"),
        ISLAMIC_TBLA("islamic-tbla");

        private String bcpType;

        public static CalculationType[] valuesCustom() {
            return values();
        }

        CalculationType(String bcpType) {
            this.bcpType = bcpType;
        }

        String bcpType() {
            return this.bcpType;
        }
    }

    public void setCalculationType(CalculationType type) {
        this.cType = type;
        if (this.cType == CalculationType.ISLAMIC_CIVIL) {
            this.civil = true;
        } else {
            this.civil = false;
        }
    }

    public CalculationType getCalculationType() {
        return this.cType;
    }

    private void setCalcTypeForLocale(ULocale locale) {
        String localeCalType = CalendarUtil.getCalendarType(locale);
        if ("islamic-civil".equals(localeCalType)) {
            setCalculationType(CalculationType.ISLAMIC_CIVIL);
            return;
        }
        if ("islamic-umalqura".equals(localeCalType)) {
            setCalculationType(CalculationType.ISLAMIC_UMALQURA);
            return;
        }
        if ("islamic-tbla".equals(localeCalType)) {
            setCalculationType(CalculationType.ISLAMIC_TBLA);
        } else if (localeCalType.startsWith("islamic")) {
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

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        if (this.cType != CalculationType.ISLAMIC_CIVIL || this.civil) {
            return;
        }
        this.cType = CalculationType.ISLAMIC;
    }
}
