package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.text.DecimalFormat;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

class CompactDecimalDataCache {

    private static final int[] f66x60516cb6 = null;

    private static final int[] f67xf770d843 = null;
    private static final String LATIN_NUMBERING_SYSTEM = "latn";
    private static final String LONG_STYLE = "long";
    static final int MAX_DIGITS = 15;
    private static final String NUMBER_ELEMENTS = "NumberElements";
    static final String OTHER = "other";
    private static final String PATTERNS_SHORT_PATH = "patternsShort/decimalFormat";
    private static final String PATTERN_LONG_PATH = "patternsLong/decimalFormat";
    private static final String SHORT_STYLE = "short";
    private final ICUCache<ULocale, DataBundle> cache = new SimpleCache();

    private static int[] m103xe26cc45a() {
        if (f66x60516cb6 != null) {
            return f66x60516cb6;
        }
        int[] iArr = new int[QuoteState.valuesCustom().length];
        try {
            iArr[QuoteState.INSIDE_EMPTY.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[QuoteState.INSIDE_FULL.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[QuoteState.OUTSIDE.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f66x60516cb6 = iArr;
        return iArr;
    }

    private static int[] m104x5679fc1f() {
        if (f67xf770d843 != null) {
            return f67xf770d843;
        }
        int[] iArr = new int[UResFlags.valuesCustom().length];
        try {
            iArr[UResFlags.ANY.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[UResFlags.NOT_ROOT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f67xf770d843 = iArr;
        return iArr;
    }

    CompactDecimalDataCache() {
    }

    static class Data {
        long[] divisors;
        Map<String, DecimalFormat.Unit[]> units;

        Data(long[] divisors, Map<String, DecimalFormat.Unit[]> units) {
            this.divisors = divisors;
            this.units = units;
        }
    }

    static class DataBundle {
        Data longData;
        Data shortData;

        DataBundle(Data shortData, Data longData) {
            this.shortData = shortData;
            this.longData = longData;
        }
    }

    private enum QuoteState {
        OUTSIDE,
        INSIDE_EMPTY,
        INSIDE_FULL;

        public static QuoteState[] valuesCustom() {
            return values();
        }
    }

    private enum UResFlags {
        ANY,
        NOT_ROOT;

        public static UResFlags[] valuesCustom() {
            return values();
        }
    }

    DataBundle get(ULocale locale) {
        DataBundle result = this.cache.get(locale);
        if (result == null) {
            DataBundle result2 = load(locale);
            this.cache.put(locale, result2);
            return result2;
        }
        return result;
    }

    private static DataBundle load(ULocale ulocale) {
        Data longData;
        NumberingSystem ns = NumberingSystem.getInstance(ulocale);
        ICUResourceBundle r = ((ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ulocale)).getWithFallback(NUMBER_ELEMENTS);
        String numberingSystemName = ns.getName();
        ICUResourceBundle shortDataBundle = null;
        ICUResourceBundle longDataBundle = null;
        if (!LATIN_NUMBERING_SYSTEM.equals(numberingSystemName)) {
            ICUResourceBundle bundle = findWithFallback(r, numberingSystemName, UResFlags.NOT_ROOT);
            shortDataBundle = findWithFallback(bundle, PATTERNS_SHORT_PATH, UResFlags.NOT_ROOT);
            longDataBundle = findWithFallback(bundle, PATTERN_LONG_PATH, UResFlags.NOT_ROOT);
        }
        if (shortDataBundle == null) {
            ICUResourceBundle bundle2 = getWithFallback(r, LATIN_NUMBERING_SYSTEM, UResFlags.ANY);
            shortDataBundle = getWithFallback(bundle2, PATTERNS_SHORT_PATH, UResFlags.ANY);
            if (longDataBundle == null && (longDataBundle = findWithFallback(bundle2, PATTERN_LONG_PATH, UResFlags.ANY)) != null && isRoot(longDataBundle) && !isRoot(shortDataBundle)) {
                longDataBundle = null;
            }
        }
        Data shortData = loadStyle(shortDataBundle, ulocale, SHORT_STYLE);
        if (longDataBundle == null) {
            longData = shortData;
        } else {
            longData = loadStyle(longDataBundle, ulocale, LONG_STYLE);
        }
        return new DataBundle(shortData, longData);
    }

    private static ICUResourceBundle findWithFallback(ICUResourceBundle r, String path, UResFlags flags) {
        ICUResourceBundle result;
        if (r == null || (result = r.findWithFallback(path)) == null) {
            return null;
        }
        switch (m104x5679fc1f()[flags.ordinal()]) {
            case 1:
                return result;
            case 2:
                if (isRoot(result)) {
                    return null;
                }
                return result;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static ICUResourceBundle getWithFallback(ICUResourceBundle r, String path, UResFlags flags) {
        ICUResourceBundle result = findWithFallback(r, path, flags);
        if (result == null) {
            throw new MissingResourceException("Cannot find " + path, ICUResourceBundle.class.getName(), path);
        }
        return result;
    }

    private static boolean isRoot(ICUResourceBundle r) {
        ULocale bundleLocale = r.getULocale();
        if (bundleLocale.equals(ULocale.ROOT)) {
            return true;
        }
        return bundleLocale.toString().equals("root");
    }

    private static Data loadStyle(ICUResourceBundle r, ULocale locale, String style) {
        int size = r.getSize();
        Data result = new Data(new long[15], new HashMap());
        for (int i = 0; i < size; i++) {
            populateData(r.get(i), locale, style, result);
        }
        fillInMissing(result);
        return result;
    }

    private static void populateData(UResourceBundle divisorData, ULocale locale, String style, Data result) {
        long magnitude = Long.parseLong(divisorData.getKey());
        int thisIndex = (int) Math.log10(magnitude);
        if (thisIndex >= 15) {
            return;
        }
        int size = divisorData.getSize();
        int numZeros = 0;
        boolean otherVariantDefined = false;
        for (int i = 0; i < size; i++) {
            UResourceBundle pluralVariantData = divisorData.get(i);
            String pluralVariant = pluralVariantData.getKey();
            String template = pluralVariantData.getString();
            if (pluralVariant.equals("other")) {
                otherVariantDefined = true;
            }
            int nz = populatePrefixSuffix(pluralVariant, thisIndex, template, locale, style, result);
            if (nz != numZeros) {
                if (numZeros != 0) {
                    throw new IllegalArgumentException("Plural variant '" + pluralVariant + "' template '" + template + "' for 10^" + thisIndex + " has wrong number of zeros in " + localeAndStyle(locale, style));
                }
                numZeros = nz;
            }
        }
        if (!otherVariantDefined) {
            throw new IllegalArgumentException("No 'other' plural variant defined for 10^" + thisIndex + "in " + localeAndStyle(locale, style));
        }
        long divisor = magnitude;
        for (int i2 = 1; i2 < numZeros; i2++) {
            divisor /= 10;
        }
        result.divisors[thisIndex] = divisor;
    }

    private static int populatePrefixSuffix(String pluralVariant, int idx, String template, ULocale locale, String style, Data result) {
        int firstIdx = template.indexOf(AndroidHardcodedSystemProperties.JAVA_VERSION);
        int lastIdx = template.lastIndexOf(AndroidHardcodedSystemProperties.JAVA_VERSION);
        if (firstIdx == -1) {
            throw new IllegalArgumentException("Expect at least one zero in template '" + template + "' for variant '" + pluralVariant + "' for 10^" + idx + " in " + localeAndStyle(locale, style));
        }
        String prefix = fixQuotes(template.substring(0, firstIdx));
        String suffix = fixQuotes(template.substring(lastIdx + 1));
        saveUnit(new DecimalFormat.Unit(prefix, suffix), pluralVariant, idx, result.units);
        if (prefix.trim().length() == 0 && suffix.trim().length() == 0) {
            return idx + 1;
        }
        int i = firstIdx + 1;
        while (i <= lastIdx && template.charAt(i) == '0') {
            i++;
        }
        return i - firstIdx;
    }

    private static String fixQuotes(String prefixOrSuffix) {
        StringBuilder result = new StringBuilder();
        int len = prefixOrSuffix.length();
        QuoteState state = QuoteState.OUTSIDE;
        for (int idx = 0; idx < len; idx++) {
            char ch = prefixOrSuffix.charAt(idx);
            if (ch == '\'') {
                if (state == QuoteState.INSIDE_EMPTY) {
                    result.append(PatternTokenizer.SINGLE_QUOTE);
                }
            } else {
                result.append(ch);
            }
            switch (m103xe26cc45a()[state.ordinal()]) {
                case 1:
                case 2:
                    state = ch == '\'' ? QuoteState.OUTSIDE : QuoteState.INSIDE_FULL;
                    break;
                case 3:
                    state = ch == '\'' ? QuoteState.INSIDE_EMPTY : QuoteState.OUTSIDE;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        return result.toString();
    }

    private static String localeAndStyle(ULocale locale, String style) {
        return "locale '" + locale + "' style '" + style + "'";
    }

    private static void fillInMissing(Data result) {
        long lastDivisor = 1;
        for (int i = 0; i < result.divisors.length; i++) {
            if (result.units.get("other")[i] == null) {
                result.divisors[i] = lastDivisor;
                copyFromPreviousIndex(i, result.units);
            } else {
                lastDivisor = result.divisors[i];
                propagateOtherToMissing(i, result.units);
            }
        }
    }

    private static void propagateOtherToMissing(int idx, Map<String, DecimalFormat.Unit[]> units) {
        DecimalFormat.Unit otherVariantValue = units.get("other")[idx];
        for (DecimalFormat.Unit[] byBase : units.values()) {
            if (byBase[idx] == null) {
                byBase[idx] = otherVariantValue;
            }
        }
    }

    private static void copyFromPreviousIndex(int idx, Map<String, DecimalFormat.Unit[]> units) {
        for (DecimalFormat.Unit[] byBase : units.values()) {
            if (idx == 0) {
                byBase[idx] = DecimalFormat.NULL_UNIT;
            } else {
                byBase[idx] = byBase[idx - 1];
            }
        }
    }

    private static void saveUnit(DecimalFormat.Unit unit, String pluralVariant, int idx, Map<String, DecimalFormat.Unit[]> units) {
        DecimalFormat.Unit[] byBase = units.get(pluralVariant);
        if (byBase == null) {
            byBase = new DecimalFormat.Unit[15];
            units.put(pluralVariant, byBase);
        }
        byBase[idx] = unit;
    }

    static DecimalFormat.Unit getUnit(Map<String, DecimalFormat.Unit[]> units, String variant, int base) {
        DecimalFormat.Unit[] byBase = units.get(variant);
        if (byBase == null) {
            byBase = units.get("other");
        }
        return byBase[base];
    }
}
