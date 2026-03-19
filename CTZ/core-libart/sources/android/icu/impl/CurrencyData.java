package android.icu.impl;

import android.icu.text.CurrencyDisplayNames;
import android.icu.util.ULocale;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;

public class CurrencyData {
    public static final CurrencyDisplayInfoProvider provider;

    public static abstract class CurrencyDisplayInfo extends CurrencyDisplayNames {
        public abstract CurrencyFormatInfo getFormatInfo(String str);

        public abstract String getNarrowSymbol(String str);

        public abstract CurrencySpacingInfo getSpacingInfo();

        public abstract Map<String, String> getUnitPatterns();
    }

    public interface CurrencyDisplayInfoProvider {
        CurrencyDisplayInfo getInstance(ULocale uLocale, boolean z);

        boolean hasData();
    }

    private CurrencyData() {
    }

    public static final class CurrencyFormatInfo {
        public final String currencyPattern;
        public final String isoCode;
        public final String monetaryDecimalSeparator;
        public final String monetaryGroupingSeparator;

        public CurrencyFormatInfo(String str, String str2, String str3, String str4) {
            this.isoCode = str;
            this.currencyPattern = str2;
            this.monetaryDecimalSeparator = str3;
            this.monetaryGroupingSeparator = str4;
        }
    }

    public static final class CurrencySpacingInfo {
        static final boolean $assertionsDisabled = false;
        private static final String DEFAULT_INSERT = " ";
        private static final String DEFAULT_CUR_MATCH = "[:letter:]";
        private static final String DEFAULT_CTX_MATCH = "[:digit:]";
        public static final CurrencySpacingInfo DEFAULT = new CurrencySpacingInfo(DEFAULT_CUR_MATCH, DEFAULT_CTX_MATCH, " ", DEFAULT_CUR_MATCH, DEFAULT_CTX_MATCH, " ");
        private final String[][] symbols = (String[][]) Array.newInstance((Class<?>) String.class, SpacingType.COUNT.ordinal(), SpacingPattern.COUNT.ordinal());
        public boolean hasBeforeCurrency = false;
        public boolean hasAfterCurrency = false;

        public enum SpacingType {
            BEFORE,
            AFTER,
            COUNT
        }

        public enum SpacingPattern {
            CURRENCY_MATCH(0),
            SURROUNDING_MATCH(1),
            INSERT_BETWEEN(2),
            COUNT;

            static final boolean $assertionsDisabled = false;

            SpacingPattern(int i) {
            }
        }

        public CurrencySpacingInfo() {
        }

        public CurrencySpacingInfo(String... strArr) {
            int i = 0;
            int i2 = 0;
            while (i < SpacingType.COUNT.ordinal()) {
                int i3 = i2;
                for (int i4 = 0; i4 < SpacingPattern.COUNT.ordinal(); i4++) {
                    this.symbols[i][i4] = strArr[i3];
                    i3++;
                }
                i++;
                i2 = i3;
            }
        }

        public void setSymbolIfNull(SpacingType spacingType, SpacingPattern spacingPattern, String str) {
            int iOrdinal = spacingType.ordinal();
            int iOrdinal2 = spacingPattern.ordinal();
            if (this.symbols[iOrdinal][iOrdinal2] == null) {
                this.symbols[iOrdinal][iOrdinal2] = str;
            }
        }

        public String[] getBeforeSymbols() {
            return this.symbols[SpacingType.BEFORE.ordinal()];
        }

        public String[] getAfterSymbols() {
            return this.symbols[SpacingType.AFTER.ordinal()];
        }
    }

    static {
        CurrencyDisplayInfoProvider currencyDisplayInfoProvider;
        try {
            currencyDisplayInfoProvider = (CurrencyDisplayInfoProvider) Class.forName("android.icu.impl.ICUCurrencyDisplayInfoProvider").newInstance();
        } catch (Throwable th) {
            currencyDisplayInfoProvider = new CurrencyDisplayInfoProvider() {
                @Override
                public CurrencyDisplayInfo getInstance(ULocale uLocale, boolean z) {
                    return DefaultInfo.getWithFallback(z);
                }

                @Override
                public boolean hasData() {
                    return false;
                }
            };
        }
        provider = currencyDisplayInfoProvider;
    }

    public static class DefaultInfo extends CurrencyDisplayInfo {
        private static final CurrencyDisplayInfo FALLBACK_INSTANCE = new DefaultInfo(true);
        private static final CurrencyDisplayInfo NO_FALLBACK_INSTANCE = new DefaultInfo(false);
        private final boolean fallback;

        private DefaultInfo(boolean z) {
            this.fallback = z;
        }

        public static final CurrencyDisplayInfo getWithFallback(boolean z) {
            return z ? FALLBACK_INSTANCE : NO_FALLBACK_INSTANCE;
        }

        @Override
        public String getName(String str) {
            if (this.fallback) {
                return str;
            }
            return null;
        }

        @Override
        public String getPluralName(String str, String str2) {
            if (this.fallback) {
                return str;
            }
            return null;
        }

        @Override
        public String getSymbol(String str) {
            if (this.fallback) {
                return str;
            }
            return null;
        }

        @Override
        public String getNarrowSymbol(String str) {
            if (this.fallback) {
                return str;
            }
            return null;
        }

        @Override
        public Map<String, String> symbolMap() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> nameMap() {
            return Collections.emptyMap();
        }

        @Override
        public ULocale getULocale() {
            return ULocale.ROOT;
        }

        @Override
        public Map<String, String> getUnitPatterns() {
            if (this.fallback) {
                return Collections.emptyMap();
            }
            return null;
        }

        @Override
        public CurrencyFormatInfo getFormatInfo(String str) {
            return null;
        }

        @Override
        public CurrencySpacingInfo getSpacingInfo() {
            if (this.fallback) {
                return CurrencySpacingInfo.DEFAULT;
            }
            return null;
        }
    }
}
