package libcore.icu;

import android.icu.impl.locale.LanguageTag;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.util.BasicLruCache;

public final class ICU {
    private static final BasicLruCache<String, String> CACHED_PATTERNS = new BasicLruCache<>(8);
    private static final int IDX_LANGUAGE = 0;
    private static final int IDX_REGION = 2;
    private static final int IDX_SCRIPT = 1;
    private static final int IDX_VARIANT = 3;
    public static final int U_BUFFER_OVERFLOW_ERROR = 15;
    public static final int U_ILLEGAL_CHAR_FOUND = 12;
    public static final int U_INVALID_CHAR_FOUND = 10;
    public static final int U_TRUNCATED_CHAR_FOUND = 11;
    public static final int U_ZERO_ERROR = 0;
    private static Locale[] availableLocalesCache;
    private static String[] isoCountries;
    private static String[] isoLanguages;

    @Deprecated
    public static native String addLikelySubtags(String str);

    private static native String[] getAvailableBreakIteratorLocalesNative();

    private static native String[] getAvailableCalendarLocalesNative();

    private static native String[] getAvailableCollatorLocalesNative();

    public static native String[] getAvailableCurrencyCodes();

    private static native String[] getAvailableDateFormatLocalesNative();

    private static native String[] getAvailableLocalesNative();

    private static native String[] getAvailableNumberFormatLocalesNative();

    private static native String getBestDateTimePatternNative(String str, String str2);

    public static native String getCldrVersion();

    public static native String getCurrencyCode(String str);

    private static native String getCurrencyDisplayName(String str, String str2);

    public static native int getCurrencyFractionDigits(String str);

    public static native int getCurrencyNumericCode(String str);

    private static native String getCurrencySymbol(String str, String str2);

    public static native String getDefaultLocale();

    private static native String getDisplayCountryNative(String str, String str2);

    private static native String getDisplayLanguageNative(String str, String str2);

    private static native String getDisplayScriptNative(String str, String str2);

    private static native String getDisplayVariantNative(String str, String str2);

    public static native String getISO3Country(String str);

    public static native String getISO3Language(String str);

    private static native String[] getISOCountriesNative();

    private static native String[] getISOLanguagesNative();

    public static native String getIcuVersion();

    @Deprecated
    public static native String getScript(String str);

    public static native String getTZDataVersion();

    public static native String getUnicodeVersion();

    static native boolean initLocaleDataNative(String str, LocaleData localeData);

    public static native void setDefaultLocale(String str);

    private static native String toLowerCase(String str, String str2);

    private static native String toUpperCase(String str, String str2);

    public static String[] getISOLanguages() {
        if (isoLanguages == null) {
            isoLanguages = getISOLanguagesNative();
        }
        return (String[]) isoLanguages.clone();
    }

    public static String[] getISOCountries() {
        if (isoCountries == null) {
            isoCountries = getISOCountriesNative();
        }
        return (String[]) isoCountries.clone();
    }

    private static void parseLangScriptRegionAndVariants(String str, String[] strArr) {
        int iIndexOf = str.indexOf(95);
        int i = iIndexOf + 1;
        int iIndexOf2 = str.indexOf(95, i);
        int i2 = iIndexOf2 + 1;
        int iIndexOf3 = str.indexOf(95, i2);
        if (iIndexOf != -1) {
            if (iIndexOf2 != -1) {
                if (iIndexOf3 == -1) {
                    strArr[0] = str.substring(0, iIndexOf);
                    String strSubstring = str.substring(i, iIndexOf2);
                    String strSubstring2 = str.substring(i2);
                    if (strSubstring.length() == 4) {
                        strArr[1] = strSubstring;
                        if (strSubstring2.length() == 2 || strSubstring2.length() == 3 || strSubstring2.isEmpty()) {
                            strArr[2] = strSubstring2;
                            return;
                        } else {
                            strArr[3] = strSubstring2;
                            return;
                        }
                    }
                    if (strSubstring.isEmpty() || strSubstring.length() == 2 || strSubstring.length() == 3) {
                        strArr[2] = strSubstring;
                        strArr[3] = strSubstring2;
                        return;
                    } else {
                        strArr[3] = str.substring(i);
                        return;
                    }
                }
                strArr[0] = str.substring(0, iIndexOf);
                String strSubstring3 = str.substring(i, iIndexOf2);
                if (strSubstring3.length() == 4) {
                    strArr[1] = strSubstring3;
                    strArr[2] = str.substring(i2, iIndexOf3);
                    strArr[3] = str.substring(iIndexOf3 + 1);
                    return;
                } else {
                    strArr[2] = strSubstring3;
                    strArr[3] = str.substring(i2);
                    return;
                }
            }
            strArr[0] = str.substring(0, iIndexOf);
            String strSubstring4 = str.substring(i);
            if (strSubstring4.length() == 4) {
                strArr[1] = strSubstring4;
                return;
            } else if (strSubstring4.length() == 2 || strSubstring4.length() == 3) {
                strArr[2] = strSubstring4;
                return;
            } else {
                strArr[3] = strSubstring4;
                return;
            }
        }
        strArr[0] = str;
    }

    public static Locale localeFromIcuLocaleId(String str) {
        int iIndexOf = str.indexOf(64);
        Map map = Collections.EMPTY_MAP;
        Map map2 = Collections.EMPTY_MAP;
        Set hashSet = Collections.EMPTY_SET;
        if (iIndexOf != -1) {
            map = new HashMap();
            map2 = new HashMap();
            hashSet = new HashSet();
            for (String str2 : str.substring(iIndexOf + 1).split(";")) {
                if (str2.startsWith("attribute=")) {
                    for (String str3 : str2.substring("attribute=".length()).split(LanguageTag.SEP)) {
                        hashSet.add(str3);
                    }
                } else {
                    int iIndexOf2 = str2.indexOf(61);
                    if (iIndexOf2 == 1) {
                        map.put(Character.valueOf(str2.charAt(0)), str2.substring(2));
                    } else {
                        map2.put(str2.substring(0, iIndexOf2), str2.substring(iIndexOf2 + 1));
                    }
                }
            }
        }
        String[] strArr = {"", "", "", ""};
        if (iIndexOf == -1) {
            parseLangScriptRegionAndVariants(str, strArr);
        } else {
            parseLangScriptRegionAndVariants(str.substring(0, iIndexOf), strArr);
        }
        Locale.Builder builder = new Locale.Builder();
        builder.setLanguage(strArr[0]);
        builder.setRegion(strArr[2]);
        builder.setVariant(strArr[3]);
        builder.setScript(strArr[1]);
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            builder.addUnicodeLocaleAttribute((String) it.next());
        }
        for (Map.Entry entry : map2.entrySet()) {
            builder.setUnicodeLocaleKeyword((String) entry.getKey(), (String) entry.getValue());
        }
        for (Map.Entry entry2 : map.entrySet()) {
            builder.setExtension(((Character) entry2.getKey()).charValue(), (String) entry2.getValue());
        }
        return builder.build();
    }

    public static Locale[] localesFromStrings(String[] strArr) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        for (String str : strArr) {
            linkedHashSet.add(localeFromIcuLocaleId(str));
        }
        return (Locale[]) linkedHashSet.toArray(new Locale[linkedHashSet.size()]);
    }

    public static Locale[] getAvailableLocales() {
        if (availableLocalesCache == null) {
            availableLocalesCache = localesFromStrings(getAvailableLocalesNative());
        }
        return (Locale[]) availableLocalesCache.clone();
    }

    public static Locale[] getAvailableBreakIteratorLocales() {
        return localesFromStrings(getAvailableBreakIteratorLocalesNative());
    }

    public static Locale[] getAvailableCalendarLocales() {
        return localesFromStrings(getAvailableCalendarLocalesNative());
    }

    public static Locale[] getAvailableCollatorLocales() {
        return localesFromStrings(getAvailableCollatorLocalesNative());
    }

    public static Locale[] getAvailableDateFormatLocales() {
        return localesFromStrings(getAvailableDateFormatLocalesNative());
    }

    public static Locale[] getAvailableDateFormatSymbolsLocales() {
        return getAvailableDateFormatLocales();
    }

    public static Locale[] getAvailableDecimalFormatSymbolsLocales() {
        return getAvailableNumberFormatLocales();
    }

    public static Locale[] getAvailableNumberFormatLocales() {
        return localesFromStrings(getAvailableNumberFormatLocalesNative());
    }

    public static String getBestDateTimePattern(String str, Locale locale) {
        String bestDateTimePatternNative;
        String languageTag = locale.toLanguageTag();
        String str2 = str + "\t" + languageTag;
        synchronized (CACHED_PATTERNS) {
            bestDateTimePatternNative = CACHED_PATTERNS.get(str2);
            if (bestDateTimePatternNative == null) {
                bestDateTimePatternNative = getBestDateTimePatternNative(str, languageTag);
                CACHED_PATTERNS.put(str2, bestDateTimePatternNative);
            }
        }
        return bestDateTimePatternNative;
    }

    public static char[] getDateFormatOrder(String str) {
        char[] cArr = new char[3];
        int i = 0;
        boolean z = false;
        int i2 = 0;
        boolean z2 = false;
        boolean z3 = false;
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            if (cCharAt == 'd' || cCharAt == 'L' || cCharAt == 'M' || cCharAt == 'y') {
                if (cCharAt == 'd' && !z) {
                    cArr[i2] = 'd';
                    i2++;
                    z = true;
                } else if ((cCharAt == 'L' || cCharAt == 'M') && !z2) {
                    cArr[i2] = 'M';
                    i2++;
                    z2 = true;
                } else if (cCharAt == 'y' && !z3) {
                    cArr[i2] = 'y';
                    i2++;
                    z3 = true;
                }
            } else if (cCharAt == 'G') {
                continue;
            } else {
                if ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z')) {
                    throw new IllegalArgumentException("Bad pattern character '" + cCharAt + "' in " + str);
                }
                if (cCharAt != '\'') {
                    continue;
                } else if (i < str.length() - 1) {
                    int i3 = i + 1;
                    if (str.charAt(i3) == '\'') {
                        i = i3;
                    } else {
                        int iIndexOf = str.indexOf(39, i + 1);
                        if (iIndexOf == -1) {
                            throw new IllegalArgumentException("Bad quoting in " + str);
                        }
                        i = iIndexOf + 1;
                    }
                }
            }
            i++;
        }
        return cArr;
    }

    public static String toLowerCase(String str, Locale locale) {
        return toLowerCase(str, locale.toLanguageTag());
    }

    public static String toUpperCase(String str, Locale locale) {
        return toUpperCase(str, locale.toLanguageTag());
    }

    public static boolean U_FAILURE(int i) {
        return i > 0;
    }

    public static String getCurrencyDisplayName(Locale locale, String str) {
        return getCurrencyDisplayName(locale.toLanguageTag(), str);
    }

    public static String getCurrencySymbol(Locale locale, String str) {
        return getCurrencySymbol(locale.toLanguageTag(), str);
    }

    public static String getDisplayCountry(Locale locale, Locale locale2) {
        return getDisplayCountryNative(locale.toLanguageTag(), locale2.toLanguageTag());
    }

    public static String getDisplayLanguage(Locale locale, Locale locale2) {
        return getDisplayLanguageNative(locale.toLanguageTag(), locale2.toLanguageTag());
    }

    public static String getDisplayVariant(Locale locale, Locale locale2) {
        return getDisplayVariantNative(locale.toLanguageTag(), locale2.toLanguageTag());
    }

    public static String getDisplayScript(Locale locale, Locale locale2) {
        return getDisplayScriptNative(locale.toLanguageTag(), locale2.toLanguageTag());
    }

    public static Locale addLikelySubtags(Locale locale) {
        return Locale.forLanguageTag(addLikelySubtags(locale.toLanguageTag()).replace('_', '-'));
    }
}
