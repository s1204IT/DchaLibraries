package android.icu.impl;

import android.icu.impl.CurrencyData;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ICUCurrencyDisplayInfoProvider implements CurrencyData.CurrencyDisplayInfoProvider {
    @Override
    public CurrencyData.CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
        int status;
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/curr", locale);
        if (!withFallback && ((status = rb.getLoadingStatus()) == 3 || status == 2)) {
            return null;
        }
        return new ICUCurrencyDisplayInfo(rb, withFallback);
    }

    @Override
    public boolean hasData() {
        return true;
    }

    static class ICUCurrencyDisplayInfo extends CurrencyData.CurrencyDisplayInfo {
        private SoftReference<Map<String, String>> _nameMapRef;
        private SoftReference<Map<String, String>> _symbolMapRef;
        private final ICUResourceBundle currencies;
        private final boolean fallback;
        private final ICUResourceBundle plurals;
        private final ICUResourceBundle rb;

        public ICUCurrencyDisplayInfo(ICUResourceBundle rb, boolean fallback) {
            this.fallback = fallback;
            this.rb = rb;
            this.currencies = rb.findTopLevel("Currencies");
            this.plurals = rb.findTopLevel("CurrencyPlurals");
        }

        @Override
        public ULocale getULocale() {
            return this.rb.getULocale();
        }

        @Override
        public String getName(String isoCode) {
            return getName(isoCode, false);
        }

        @Override
        public String getSymbol(String isoCode) {
            return getName(isoCode, true);
        }

        private String getName(String isoCode, boolean symbolName) {
            ICUResourceBundle result;
            int status;
            if (this.currencies != null && (result = this.currencies.findWithFallback(isoCode)) != null) {
                if (this.fallback || !((status = result.getLoadingStatus()) == 3 || status == 2)) {
                    return result.getString(symbolName ? 0 : 1);
                }
                return null;
            }
            if (this.fallback) {
                return isoCode;
            }
            return null;
        }

        @Override
        public String getPluralName(String isoCode, String pluralKey) {
            ICUResourceBundle pluralsBundle;
            if (this.plurals != null && (pluralsBundle = this.plurals.findWithFallback(isoCode)) != null) {
                String pluralName = pluralsBundle.findStringWithFallback(pluralKey);
                if (pluralName == null) {
                    if (!this.fallback) {
                        return null;
                    }
                    pluralName = pluralsBundle.findStringWithFallback(PluralRules.KEYWORD_OTHER);
                    if (pluralName == null) {
                        return getName(isoCode);
                    }
                }
                return pluralName;
            }
            if (this.fallback) {
                return getName(isoCode);
            }
            return null;
        }

        @Override
        public Map<String, String> symbolMap() {
            Map<String, String> map = this._symbolMapRef != null ? this._symbolMapRef.get() : null;
            if (map == null) {
                Map<String, String> map2 = _createSymbolMap();
                this._symbolMapRef = new SoftReference<>(map2);
                return map2;
            }
            return map;
        }

        @Override
        public Map<String, String> nameMap() {
            Map<String, String> map = this._nameMapRef != null ? this._nameMapRef.get() : null;
            if (map == null) {
                Map<String, String> map2 = _createNameMap();
                this._nameMapRef = new SoftReference<>(map2);
                return map2;
            }
            return map;
        }

        @Override
        public Map<String, String> getUnitPatterns() {
            ICUResourceBundle cr;
            Map<String, String> result = new HashMap<>();
            for (ULocale locale = this.rb.getULocale(); locale != null; locale = locale.getFallback()) {
                ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/curr", locale);
                if (r != null && (cr = r.findWithFallback("CurrencyUnitPatterns")) != null) {
                    int size = cr.getSize();
                    for (int index = 0; index < size; index++) {
                        ICUResourceBundle b = (ICUResourceBundle) cr.get(index);
                        String key = b.getKey();
                        if (!result.containsKey(key)) {
                            result.put(key, b.getString());
                        }
                    }
                }
            }
            return Collections.unmodifiableMap(result);
        }

        @Override
        public CurrencyData.CurrencyFormatInfo getFormatInfo(String isoCode) {
            ICUResourceBundle crb;
            ICUResourceBundle crb2 = this.currencies.findWithFallback(isoCode);
            if (crb2 == null || crb2.getSize() <= 2 || (crb = crb2.at(2)) == null) {
                return null;
            }
            String pattern = crb.getString(0);
            char separator = crb.getString(1).charAt(0);
            char groupingSeparator = crb.getString(2).charAt(0);
            return new CurrencyData.CurrencyFormatInfo(pattern, separator, groupingSeparator);
        }

        @Override
        public CurrencyData.CurrencySpacingInfo getSpacingInfo() {
            ICUResourceBundle srb = this.rb.findWithFallback("currencySpacing");
            if (srb != null) {
                ICUResourceBundle brb = srb.findWithFallback("beforeCurrency");
                ICUResourceBundle arb = srb.findWithFallback("afterCurrency");
                if (arb != null && brb != null) {
                    String beforeCurrencyMatch = brb.findStringWithFallback("currencyMatch");
                    String beforeContextMatch = brb.findStringWithFallback("surroundingMatch");
                    String beforeInsert = brb.findStringWithFallback("insertBetween");
                    String afterCurrencyMatch = arb.findStringWithFallback("currencyMatch");
                    String afterContextMatch = arb.findStringWithFallback("surroundingMatch");
                    String afterInsert = arb.findStringWithFallback("insertBetween");
                    return new CurrencyData.CurrencySpacingInfo(beforeCurrencyMatch, beforeContextMatch, beforeInsert, afterCurrencyMatch, afterContextMatch, afterInsert);
                }
            }
            if (this.fallback) {
                return CurrencyData.CurrencySpacingInfo.DEFAULT;
            }
            return null;
        }

        private Map<String, String> _createSymbolMap() {
            Map<String, String> result = new HashMap<>();
            for (ULocale locale = this.rb.getULocale(); locale != null; locale = locale.getFallback()) {
                ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/curr", locale);
                ICUResourceBundle curr = bundle.findTopLevel("Currencies");
                if (curr != null) {
                    for (int i = 0; i < curr.getSize(); i++) {
                        ICUResourceBundle item = curr.at(i);
                        String isoCode = item.getKey();
                        if (!result.containsKey(isoCode)) {
                            result.put(isoCode, isoCode);
                            String symbol = item.getString(0);
                            result.put(symbol, isoCode);
                        }
                    }
                }
            }
            return Collections.unmodifiableMap(result);
        }

        private Map<String, String> _createNameMap() {
            Map<String, String> result = new TreeMap<>((Comparator<? super String>) String.CASE_INSENSITIVE_ORDER);
            Set<String> visited = new HashSet<>();
            Map<String, Set<String>> visitedPlurals = new HashMap<>();
            for (ULocale locale = this.rb.getULocale(); locale != null; locale = locale.getFallback()) {
                ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/curr", locale);
                ICUResourceBundle curr = bundle.findTopLevel("Currencies");
                if (curr != null) {
                    for (int i = 0; i < curr.getSize(); i++) {
                        ICUResourceBundle item = curr.at(i);
                        String isoCode = item.getKey();
                        if (!visited.contains(isoCode)) {
                            visited.add(isoCode);
                            String name = item.getString(1);
                            result.put(name, isoCode);
                        }
                    }
                }
                ICUResourceBundle plurals = bundle.findTopLevel("CurrencyPlurals");
                if (plurals != null) {
                    for (int i2 = 0; i2 < plurals.getSize(); i2++) {
                        ICUResourceBundle item2 = plurals.at(i2);
                        String isoCode2 = item2.getKey();
                        Set<String> pluralSet = visitedPlurals.get(isoCode2);
                        if (pluralSet == null) {
                            pluralSet = new HashSet<>();
                            visitedPlurals.put(isoCode2, pluralSet);
                        }
                        for (int j = 0; j < item2.getSize(); j++) {
                            ICUResourceBundle plural = item2.at(j);
                            String pluralType = plural.getKey();
                            if (!pluralSet.contains(pluralType)) {
                                String pluralName = plural.getString();
                                result.put(pluralName, isoCode2);
                                pluralSet.add(pluralType);
                            }
                        }
                    }
                }
            }
            return Collections.unmodifiableMap(result);
        }
    }
}
