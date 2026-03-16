package libcore.icu;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import libcore.util.BasicLruCache;
import libcore.util.ZoneInfoDB;

public final class TimeZoneNames {
    public static final int LONG_NAME = 1;
    public static final int LONG_NAME_DST = 3;
    public static final int NAME_COUNT = 5;
    public static final int OLSON_NAME = 0;
    public static final int SHORT_NAME = 2;
    public static final int SHORT_NAME_DST = 4;
    private static final Comparator<String[]> ZONE_STRINGS_COMPARATOR;
    private static final String[] availableTimeZoneIds = TimeZone.getAvailableIDs();
    private static final ZoneStringsCache cachedZoneStrings = new ZoneStringsCache();

    private static native void fillZoneStrings(String str, String[][] strArr);

    public static native String getExemplarLocation(String str, String str2);

    static {
        cachedZoneStrings.get(Locale.ROOT);
        cachedZoneStrings.get(Locale.US);
        cachedZoneStrings.get(Locale.getDefault());
        ZONE_STRINGS_COMPARATOR = new Comparator<String[]>() {
            @Override
            public int compare(String[] lhs, String[] rhs) {
                return lhs[0].compareTo(rhs[0]);
            }
        };
    }

    public static class ZoneStringsCache extends BasicLruCache<Locale, String[][]> {
        public ZoneStringsCache() {
            super(5);
        }

        @Override
        protected String[][] create(Locale locale) {
            long start = System.currentTimeMillis();
            String[][] result = (String[][]) Array.newInstance((Class<?>) String.class, TimeZoneNames.availableTimeZoneIds.length, 5);
            for (int i = 0; i < TimeZoneNames.availableTimeZoneIds.length; i++) {
                result[i][0] = TimeZoneNames.availableTimeZoneIds[i];
            }
            long nativeStart = System.currentTimeMillis();
            TimeZoneNames.fillZoneStrings(locale.toString(), result);
            long nativeEnd = System.currentTimeMillis();
            internStrings(result);
            long end = System.currentTimeMillis();
            long nativeDuration = nativeEnd - nativeStart;
            long duration = end - start;
            System.logI("Loaded time zone names for \"" + locale + "\" in " + duration + "ms (" + nativeDuration + "ms in ICU)");
            return result;
        }

        private synchronized void internStrings(String[][] result) {
            HashMap<String, String> internTable = new HashMap<>();
            for (int i = 0; i < result.length; i++) {
                for (int j = 1; j < 5; j++) {
                    String original = result[i][j];
                    String nonDuplicate = internTable.get(original);
                    if (nonDuplicate == null) {
                        internTable.put(original, original);
                    } else {
                        result[i][j] = nonDuplicate;
                    }
                }
            }
        }
    }

    private TimeZoneNames() {
    }

    public static String getDisplayName(String[][] zoneStrings, String id, boolean daylight, int style) {
        String[] needle = {id};
        int index = Arrays.binarySearch(zoneStrings, needle, ZONE_STRINGS_COMPARATOR);
        if (index >= 0) {
            String[] row = zoneStrings[index];
            return daylight ? style == 1 ? row[3] : row[4] : style == 1 ? row[1] : row[2];
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
        String countryCode = locale.getCountry();
        ArrayList<String> ids = new ArrayList<>();
        String[] arr$ = ZoneInfoDB.getInstance().getZoneTab().split("\n");
        for (String line : arr$) {
            if (line.startsWith(countryCode)) {
                int olsonIdStart = line.indexOf(9, 4) + 1;
                int olsonIdEnd = line.indexOf(9, olsonIdStart);
                if (olsonIdEnd == -1) {
                    olsonIdEnd = line.length();
                }
                ids.add(line.substring(olsonIdStart, olsonIdEnd));
            }
        }
        return (String[]) ids.toArray(new String[ids.size()]);
    }
}
