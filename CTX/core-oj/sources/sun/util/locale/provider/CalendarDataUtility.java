package sun.util.locale.provider;

import android.icu.text.DateFormatSymbols;
import android.icu.util.ULocale;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CalendarDataUtility {
    private static final String BUDDHIST_CALENDAR = "buddhist";
    private static final String GREGORIAN_CALENDAR = "gregorian";
    private static final String ISLAMIC_CALENDAR = "islamic";
    private static final String JAPANESE_CALENDAR = "japanese";
    private static int[] REST_OF_STYLES = {Calendar.SHORT_STANDALONE, 2, Calendar.LONG_STANDALONE, 4, Calendar.NARROW_STANDALONE};

    private CalendarDataUtility() {
    }

    public static String retrieveFieldValueName(String str, int i, int i2, int i3, Locale locale) {
        byte b;
        if (i == 0) {
            String strNormalizeCalendarType = normalizeCalendarType(str);
            int iHashCode = strNormalizeCalendarType.hashCode();
            if (iHashCode == -1581060683) {
                if (strNormalizeCalendarType.equals(BUDDHIST_CALENDAR)) {
                    b = 0;
                }
                switch (b) {
                }
            } else if (iHashCode != -752730191) {
                b = (iHashCode == 2093696456 && strNormalizeCalendarType.equals(ISLAMIC_CALENDAR)) ? (byte) 1 : (byte) -1;
                switch (b) {
                    case 0:
                    case 1:
                        i2--;
                        break;
                    case 2:
                        i2 += 231;
                        break;
                }
            } else {
                if (strNormalizeCalendarType.equals(JAPANESE_CALENDAR)) {
                    b = 2;
                }
                switch (b) {
                }
            }
        }
        if (i2 < 0) {
            return null;
        }
        String[] names = getNames(str, i, i3, locale);
        if (i2 >= names.length) {
            return null;
        }
        return names[i2];
    }

    public static String retrieveJavaTimeFieldValueName(String str, int i, int i2, int i3, Locale locale) {
        return retrieveFieldValueName(str, i, i2, i3, locale);
    }

    public static Map<String, Integer> retrieveFieldValueNames(String str, int i, int i2, Locale locale) {
        Map<String, Integer> mapRetrieveFieldValueNamesImpl;
        if (i2 == 0) {
            mapRetrieveFieldValueNamesImpl = retrieveFieldValueNamesImpl(str, i, 1, locale);
            for (int i3 : REST_OF_STYLES) {
                mapRetrieveFieldValueNamesImpl.putAll(retrieveFieldValueNamesImpl(str, i, i3, locale));
            }
        } else {
            mapRetrieveFieldValueNamesImpl = retrieveFieldValueNamesImpl(str, i, i2, locale);
        }
        if (mapRetrieveFieldValueNamesImpl.isEmpty()) {
            return null;
        }
        return mapRetrieveFieldValueNamesImpl;
    }

    public static Map<String, Integer> retrieveJavaTimeFieldValueNames(String str, int i, int i2, Locale locale) {
        return retrieveFieldValueNames(str, i, i2, locale);
    }

    private static String normalizeCalendarType(String str) {
        if (str.equals("gregory") || str.equals("iso8601")) {
            return GREGORIAN_CALENDAR;
        }
        if (str.startsWith(ISLAMIC_CALENDAR)) {
            return ISLAMIC_CALENDAR;
        }
        return str;
    }

    private static Map<String, Integer> retrieveFieldValueNamesImpl(String str, int i, int i2, Locale locale) {
        byte b;
        String[] names = getNames(str, i, i2, locale);
        int i3 = 1;
        int i4 = 0;
        if (i == 0) {
            String strNormalizeCalendarType = normalizeCalendarType(str);
            int iHashCode = strNormalizeCalendarType.hashCode();
            if (iHashCode == -1581060683) {
                if (strNormalizeCalendarType.equals(BUDDHIST_CALENDAR)) {
                    b = 0;
                }
                switch (b) {
                }
            } else if (iHashCode != -752730191) {
                b = (iHashCode == 2093696456 && strNormalizeCalendarType.equals(ISLAMIC_CALENDAR)) ? (byte) 1 : (byte) -1;
                switch (b) {
                    case 2:
                        i4 = 232;
                        i3 = -231;
                        break;
                }
            } else {
                if (strNormalizeCalendarType.equals(JAPANESE_CALENDAR)) {
                    b = 2;
                }
                switch (b) {
                }
            }
        } else {
            i3 = 0;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        while (i4 < names.length) {
            if (names[i4].isEmpty() || linkedHashMap.put(names[i4], Integer.valueOf(i4 + i3)) == 0) {
                i4++;
            } else {
                return new LinkedHashMap();
            }
        }
        return linkedHashMap;
    }

    private static String[] getNames(String str, int i, int i2, Locale locale) {
        int context = toContext(i2);
        int width = toWidth(i2);
        DateFormatSymbols dateFormatSymbols = getDateFormatSymbols(str, locale);
        if (i == 0) {
            switch (width) {
                case 0:
                    return dateFormatSymbols.getEras();
                case 1:
                    return dateFormatSymbols.getEraNames();
                case 2:
                    return dateFormatSymbols.getNarrowEras();
                default:
                    throw new UnsupportedOperationException("Unknown width: " + width);
            }
        }
        if (i == 2) {
            return dateFormatSymbols.getMonths(context, width);
        }
        if (i == 7) {
            return dateFormatSymbols.getWeekdays(context, width);
        }
        if (i == 9) {
            return dateFormatSymbols.getAmPmStrings();
        }
        throw new UnsupportedOperationException("Unknown field: " + i);
    }

    private static DateFormatSymbols getDateFormatSymbols(String str, Locale locale) {
        return new DateFormatSymbols(ULocale.forLocale(locale), normalizeCalendarType(str));
    }

    private static int toWidth(int i) {
        switch (i) {
            case 1:
            case Calendar.SHORT_STANDALONE:
                return 0;
            case 2:
            case Calendar.LONG_STANDALONE:
                return 1;
            case 4:
            case Calendar.NARROW_STANDALONE:
                return 2;
            default:
                throw new IllegalArgumentException("Invalid style: " + i);
        }
    }

    private static int toContext(int i) {
        switch (i) {
            case 1:
            case 2:
            case 4:
                return 0;
            case Calendar.SHORT_STANDALONE:
            case Calendar.LONG_STANDALONE:
            case Calendar.NARROW_STANDALONE:
                return 1;
            default:
                throw new IllegalArgumentException("Invalid style: " + i);
        }
    }
}
