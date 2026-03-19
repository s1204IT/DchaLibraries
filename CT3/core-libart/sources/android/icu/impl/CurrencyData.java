package android.icu.impl;

import android.icu.text.CurrencyDisplayNames;
import android.icu.util.ULocale;
import java.util.Collections;
import java.util.Map;

public class CurrencyData {
    public static final CurrencyDisplayInfoProvider provider;

    public static abstract class CurrencyDisplayInfo extends CurrencyDisplayNames {
        public abstract CurrencyFormatInfo getFormatInfo(String str);

        public abstract CurrencySpacingInfo getSpacingInfo();

        public abstract Map<String, String> getUnitPatterns();
    }

    public interface CurrencyDisplayInfoProvider {
        CurrencyDisplayInfo getInstance(ULocale uLocale, boolean z);

        boolean hasData();
    }

    public static final class CurrencyFormatInfo {
        public final String currencyPattern;
        public final char monetaryGroupingSeparator;
        public final char monetarySeparator;

        public CurrencyFormatInfo(String currencyPattern, char monetarySeparator, char monetaryGroupingSeparator) {
            this.currencyPattern = currencyPattern;
            this.monetarySeparator = monetarySeparator;
            this.monetaryGroupingSeparator = monetaryGroupingSeparator;
        }
    }

    public static final class CurrencySpacingInfo {
        public final String afterContextMatch;
        public final String afterCurrencyMatch;
        public final String afterInsert;
        public final String beforeContextMatch;
        public final String beforeCurrencyMatch;
        public final String beforeInsert;
        private static final String DEFAULT_CUR_MATCH = "[:letter:]";
        private static final String DEFAULT_CTX_MATCH = "[:digit:]";
        private static final String DEFAULT_INSERT = " ";
        public static final CurrencySpacingInfo DEFAULT = new CurrencySpacingInfo(DEFAULT_CUR_MATCH, DEFAULT_CTX_MATCH, DEFAULT_INSERT, DEFAULT_CUR_MATCH, DEFAULT_CTX_MATCH, DEFAULT_INSERT);

        public CurrencySpacingInfo(String beforeCurrencyMatch, String beforeContextMatch, String beforeInsert, String afterCurrencyMatch, String afterContextMatch, String afterInsert) {
            this.beforeCurrencyMatch = beforeCurrencyMatch;
            this.beforeContextMatch = beforeContextMatch;
            this.beforeInsert = beforeInsert;
            this.afterCurrencyMatch = afterCurrencyMatch;
            this.afterContextMatch = afterContextMatch;
            this.afterInsert = afterInsert;
        }
    }

    static {
        CurrencyDisplayInfoProvider temp;
        try {
            Class<?> clzz = Class.forName("android.icu.impl.ICUCurrencyDisplayInfoProvider");
            temp = (CurrencyDisplayInfoProvider) clzz.newInstance();
        } catch (Throwable th) {
            temp = new CurrencyDisplayInfoProvider() {
                @Override
                public CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
                    return DefaultInfo.getWithFallback(withFallback);
                }

                @Override
                public boolean hasData() {
                    return false;
                }
            };
        }
        provider = temp;
    }

    public static class DefaultInfo extends CurrencyDisplayInfo {
        private static final CurrencyDisplayInfo FALLBACK_INSTANCE = new DefaultInfo(true);
        private static final CurrencyDisplayInfo NO_FALLBACK_INSTANCE = new DefaultInfo(false);
        private final boolean fallback;

        private DefaultInfo(boolean fallback) {
            this.fallback = fallback;
        }

        public static final CurrencyDisplayInfo getWithFallback(boolean fallback) {
            return fallback ? FALLBACK_INSTANCE : NO_FALLBACK_INSTANCE;
        }

        @Override
        public String getName(String isoCode) {
            if (this.fallback) {
                return isoCode;
            }
            return null;
        }

        @Override
        public String getPluralName(String isoCode, String pluralType) {
            if (this.fallback) {
                return isoCode;
            }
            return null;
        }

        @Override
        public String getSymbol(String isoCode) {
            if (this.fallback) {
                return isoCode;
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
        public CurrencyFormatInfo getFormatInfo(String isoCode) {
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
