package android.icu.impl.number;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource;
import android.icu.text.CompactDecimalFormat;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CompactData implements MultiplierProducer {
    static final boolean $assertionsDisabled = false;
    private static final int COMPACT_MAX_DIGITS = 15;
    private static final String USE_FALLBACK = "<USE FALLBACK>";
    private final String[] patterns = new String[StandardPlural.COUNT * 16];
    private final byte[] multipliers = new byte[16];
    private byte largestMagnitude = 0;
    private boolean isEmpty = true;

    public enum CompactType {
        DECIMAL,
        CURRENCY
    }

    public void populate(ULocale uLocale, String str, CompactDecimalFormat.CompactStyle compactStyle, CompactType compactType) {
        CompactDataSink compactDataSink = new CompactDataSink(this);
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        boolean zEquals = str.equals("latn");
        boolean z = compactStyle == CompactDecimalFormat.CompactStyle.SHORT;
        StringBuilder sb = new StringBuilder();
        getResourceBundleKey(str, compactStyle, compactType, sb);
        iCUResourceBundle.getAllItemsWithFallbackNoFail(sb.toString(), compactDataSink);
        if (this.isEmpty && !zEquals) {
            getResourceBundleKey("latn", compactStyle, compactType, sb);
            iCUResourceBundle.getAllItemsWithFallbackNoFail(sb.toString(), compactDataSink);
        }
        if (this.isEmpty && !z) {
            getResourceBundleKey(str, CompactDecimalFormat.CompactStyle.SHORT, compactType, sb);
            iCUResourceBundle.getAllItemsWithFallbackNoFail(sb.toString(), compactDataSink);
        }
        if (this.isEmpty && !zEquals && !z) {
            getResourceBundleKey("latn", CompactDecimalFormat.CompactStyle.SHORT, compactType, sb);
            iCUResourceBundle.getAllItemsWithFallbackNoFail(sb.toString(), compactDataSink);
        }
        if (this.isEmpty) {
            throw new ICUException("Could not load compact decimal data for locale " + uLocale);
        }
    }

    private static void getResourceBundleKey(String str, CompactDecimalFormat.CompactStyle compactStyle, CompactType compactType, StringBuilder sb) {
        sb.setLength(0);
        sb.append("NumberElements/");
        sb.append(str);
        sb.append(compactStyle == CompactDecimalFormat.CompactStyle.SHORT ? "/patternsShort" : "/patternsLong");
        sb.append(compactType == CompactType.DECIMAL ? "/decimalFormat" : "/currencyFormat");
    }

    public void populate(Map<String, Map<String, String>> map) {
        Iterator<Map.Entry<String, Map<String, String>>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            byte length = (byte) (r0.getKey().length() - 1);
            for (Map.Entry<String, String> entry : it.next().getValue().entrySet()) {
                StandardPlural standardPluralFromString = StandardPlural.fromString(entry.getKey().toString());
                String string = entry.getValue().toString();
                this.patterns[getIndex(length, standardPluralFromString)] = string;
                if (countZeros(string) > 0) {
                    this.multipliers[length] = (byte) ((r2 - length) - 1);
                    if (length > this.largestMagnitude) {
                        this.largestMagnitude = length;
                    }
                    this.isEmpty = false;
                }
            }
        }
    }

    @Override
    public int getMultiplier(int i) {
        if (i < 0) {
            return 0;
        }
        if (i > this.largestMagnitude) {
            i = this.largestMagnitude;
        }
        return this.multipliers[i];
    }

    public String getPattern(int i, StandardPlural standardPlural) {
        String str;
        if (i < 0) {
            return null;
        }
        if (i > this.largestMagnitude) {
            i = this.largestMagnitude;
        }
        String str2 = this.patterns[getIndex(i, standardPlural)];
        if (str2 == null && standardPlural != StandardPlural.OTHER) {
            str = this.patterns[getIndex(i, StandardPlural.OTHER)];
        } else {
            str = str2;
        }
        if (str == USE_FALLBACK) {
            return null;
        }
        return str;
    }

    public void getUniquePatterns(Set<String> set) {
        set.addAll(Arrays.asList(this.patterns));
        set.remove(USE_FALLBACK);
        set.remove(null);
    }

    private static final class CompactDataSink extends UResource.Sink {
        static final boolean $assertionsDisabled = false;
        CompactData data;

        public CompactDataSink(CompactData compactData) {
            this.data = compactData;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            int iCountZeros;
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                byte length = (byte) (key.length() - 1);
                byte b = this.data.multipliers[length];
                UResource.Table table2 = value.getTable();
                byte b2 = b;
                for (int i2 = 0; table2.getKeyAndValue(i2, key, value); i2++) {
                    StandardPlural standardPluralFromString = StandardPlural.fromString(key.toString());
                    if (this.data.patterns[CompactData.getIndex(length, standardPluralFromString)] == null) {
                        String string = value.toString();
                        if (string.equals(AndroidHardcodedSystemProperties.JAVA_VERSION)) {
                            string = CompactData.USE_FALLBACK;
                        }
                        this.data.patterns[CompactData.getIndex(length, standardPluralFromString)] = string;
                        if (b2 == 0 && (iCountZeros = CompactData.countZeros(string)) > 0) {
                            b2 = (byte) ((iCountZeros - length) - 1);
                        }
                    }
                }
                if (this.data.multipliers[length] == 0) {
                    this.data.multipliers[length] = b2;
                    if (length > this.data.largestMagnitude) {
                        this.data.largestMagnitude = length;
                    }
                    this.data.isEmpty = false;
                }
            }
        }
    }

    private static final int getIndex(int i, StandardPlural standardPlural) {
        return (i * StandardPlural.COUNT) + standardPlural.ordinal();
    }

    private static final int countZeros(String str) {
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            if (str.charAt(i2) == '0') {
                i++;
            } else if (i > 0) {
                break;
            }
        }
        return i;
    }
}
