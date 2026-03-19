package libcore.icu;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import libcore.util.BasicLruCache;
import libcore.util.ZoneInfoDB;

public final class TimeZoneNames {
    public static final int LONG_NAME = 1;
    public static final int LONG_NAME_DST = 3;
    public static final int NAME_COUNT = 5;
    public static final int OLSON_NAME = 0;
    public static final int SHORT_NAME = 2;
    public static final int SHORT_NAME_DST = 4;
    private static final String[] availableTimeZoneIds = TimeZone.getAvailableIDs();
    private static final ZoneStringsCache cachedZoneStrings = new ZoneStringsCache();
    private static final Comparator<String[]> ZONE_STRINGS_COMPARATOR = new Comparator<String[]>() {
        @Override
        public int compare(String[] strArr, String[] strArr2) {
            return strArr[0].compareTo(strArr2[0]);
        }
    };

    private static native void fillZoneStrings(String str, String[][] strArr);

    public static class ZoneStringsCache extends BasicLruCache<Locale, String[][]> {
        public ZoneStringsCache() {
            super(5);
        }

        @Override
        protected String[][] create(Locale locale) {
            long jNanoTime = System.nanoTime();
            String[][] strArr = (String[][]) Array.newInstance((Class<?>) String.class, TimeZoneNames.availableTimeZoneIds.length, 5);
            for (int i = 0; i < TimeZoneNames.availableTimeZoneIds.length; i++) {
                strArr[i][0] = TimeZoneNames.availableTimeZoneIds[i];
            }
            long jNanoTime2 = System.nanoTime();
            TimeZoneNames.fillZoneStrings(locale.toLanguageTag(), strArr);
            long jNanoTime3 = System.nanoTime();
            addOffsetStrings(strArr);
            internStrings(strArr);
            long jNanoTime4 = System.nanoTime();
            long millis = TimeUnit.NANOSECONDS.toMillis(jNanoTime3 - jNanoTime2);
            System.logI("Loaded time zone names for \"" + locale + "\" in " + TimeUnit.NANOSECONDS.toMillis(jNanoTime4 - jNanoTime) + "ms (" + millis + "ms in ICU)");
            return strArr;
        }

        private void addOffsetStrings(String[][] strArr) {
            for (int i = 0; i < strArr.length; i++) {
                TimeZone timeZone = null;
                for (int i2 = 1; i2 < 5; i2++) {
                    if (strArr[i][i2] == null) {
                        if (timeZone == null) {
                            timeZone = TimeZone.getTimeZone(strArr[i][0]);
                        }
                        int rawOffset = timeZone.getRawOffset();
                        if (i2 == 3 || i2 == 4) {
                            rawOffset += timeZone.getDSTSavings();
                        }
                        strArr[i][i2] = TimeZone.createGmtOffsetString(true, true, rawOffset);
                    }
                }
            }
        }

        private void internStrings(String[][] strArr) {
            HashMap map = new HashMap();
            for (int i = 0; i < strArr.length; i++) {
                for (int i2 = 1; i2 < 5; i2++) {
                    String str = strArr[i][i2];
                    String str2 = (String) map.get(str);
                    if (str2 == null) {
                        map.put(str, str);
                    } else {
                        strArr[i][i2] = str2;
                    }
                }
            }
        }
    }

    private TimeZoneNames() {
    }

    public static String getDisplayName(String[][] strArr, String str, boolean z, int i) {
        int iBinarySearch = Arrays.binarySearch(strArr, new String[]{str}, ZONE_STRINGS_COMPARATOR);
        if (iBinarySearch >= 0) {
            String[] strArr2 = strArr[iBinarySearch];
            return z ? i == 1 ? strArr2[3] : strArr2[4] : i == 1 ? strArr2[1] : strArr2[2];
        }
        return null;
    }

    public static String[][] getZoneStrings(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return cachedZoneStrings.get(locale);
    }

    public static String[] forLocale(Locale locale) {
        String country = locale.getCountry();
        ArrayList arrayList = new ArrayList();
        for (String str : ZoneInfoDB.getInstance().getZoneTab().split("\n")) {
            if (str.startsWith(country)) {
                int iIndexOf = str.indexOf(9, 4) + 1;
                int iIndexOf2 = str.indexOf(9, iIndexOf);
                if (iIndexOf2 == -1) {
                    iIndexOf2 = str.length();
                }
                arrayList.add(str.substring(iIndexOf, iIndexOf2));
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }
}
