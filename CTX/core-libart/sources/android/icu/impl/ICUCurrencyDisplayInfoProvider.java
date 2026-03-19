package android.icu.impl;

import android.icu.impl.CurrencyData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.UResource;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class ICUCurrencyDisplayInfoProvider implements CurrencyData.CurrencyDisplayInfoProvider {
    private volatile ICUCurrencyDisplayInfo currencyDisplayInfoCache = null;

    @Override
    public CurrencyData.CurrencyDisplayInfo getInstance(ULocale uLocale, boolean z) {
        ICUResourceBundle bundleInstance;
        if (uLocale == null) {
            uLocale = ULocale.ROOT;
        }
        ICUCurrencyDisplayInfo iCUCurrencyDisplayInfo = this.currencyDisplayInfoCache;
        if (iCUCurrencyDisplayInfo != null && iCUCurrencyDisplayInfo.locale.equals(uLocale) && iCUCurrencyDisplayInfo.fallback == z) {
            return iCUCurrencyDisplayInfo;
        }
        if (z) {
            bundleInstance = ICUResourceBundle.getBundleInstance(ICUData.ICU_CURR_BASE_NAME, uLocale, ICUResourceBundle.OpenType.LOCALE_DEFAULT_ROOT);
        } else {
            try {
                bundleInstance = ICUResourceBundle.getBundleInstance(ICUData.ICU_CURR_BASE_NAME, uLocale, ICUResourceBundle.OpenType.LOCALE_ONLY);
            } catch (MissingResourceException e) {
                return null;
            }
        }
        ICUCurrencyDisplayInfo iCUCurrencyDisplayInfo2 = new ICUCurrencyDisplayInfo(uLocale, bundleInstance, z);
        this.currencyDisplayInfoCache = iCUCurrencyDisplayInfo2;
        return iCUCurrencyDisplayInfo2;
    }

    @Override
    public boolean hasData() {
        return true;
    }

    static class ICUCurrencyDisplayInfo extends CurrencyData.CurrencyDisplayInfo {
        final boolean fallback;
        final ULocale locale;
        private final ICUResourceBundle rb;
        private volatile FormattingData formattingDataCache = null;
        private volatile NarrowSymbol narrowSymbolCache = null;
        private volatile String[] pluralsDataCache = null;
        private volatile SoftReference<ParsingData> parsingDataCache = new SoftReference<>(null);
        private volatile Map<String, String> unitPatternsCache = null;
        private volatile CurrencyData.CurrencySpacingInfo spacingInfoCache = null;

        static class FormattingData {
            final String isoCode;
            String displayName = null;
            String symbol = null;
            CurrencyData.CurrencyFormatInfo formatInfo = null;

            FormattingData(String str) {
                this.isoCode = str;
            }
        }

        static class NarrowSymbol {
            final String isoCode;
            String narrowSymbol = null;

            NarrowSymbol(String str) {
                this.isoCode = str;
            }
        }

        static class ParsingData {
            Map<String, String> symbolToIsoCode = new HashMap();
            Map<String, String> nameToIsoCode = new HashMap();

            ParsingData() {
            }
        }

        public ICUCurrencyDisplayInfo(ULocale uLocale, ICUResourceBundle iCUResourceBundle, boolean z) {
            this.locale = uLocale;
            this.fallback = z;
            this.rb = iCUResourceBundle;
        }

        @Override
        public ULocale getULocale() {
            return this.rb.getULocale();
        }

        @Override
        public String getName(String str) {
            FormattingData formattingDataFetchFormattingData = fetchFormattingData(str);
            if (formattingDataFetchFormattingData.displayName == null && this.fallback) {
                return str;
            }
            return formattingDataFetchFormattingData.displayName;
        }

        @Override
        public String getSymbol(String str) {
            FormattingData formattingDataFetchFormattingData = fetchFormattingData(str);
            if (formattingDataFetchFormattingData.symbol == null && this.fallback) {
                return str;
            }
            return formattingDataFetchFormattingData.symbol;
        }

        @Override
        public String getNarrowSymbol(String str) {
            NarrowSymbol narrowSymbolFetchNarrowSymbol = fetchNarrowSymbol(str);
            if (narrowSymbolFetchNarrowSymbol.narrowSymbol == null && this.fallback) {
                return str;
            }
            return narrowSymbolFetchNarrowSymbol.narrowSymbol;
        }

        @Override
        public String getPluralName(String str, String str2) {
            String str3;
            StandardPlural standardPluralOrNullFromString = StandardPlural.orNullFromString(str2);
            String[] strArrFetchPluralsData = fetchPluralsData(str);
            if (standardPluralOrNullFromString != null) {
                str3 = strArrFetchPluralsData[standardPluralOrNullFromString.ordinal() + 1];
            } else {
                str3 = null;
            }
            if (str3 == null && this.fallback) {
                str3 = strArrFetchPluralsData[1 + StandardPlural.OTHER.ordinal()];
            }
            if (str3 == null && this.fallback) {
                str3 = fetchFormattingData(str).displayName;
            }
            return (str3 == null && this.fallback) ? str : str3;
        }

        @Override
        public Map<String, String> symbolMap() {
            return fetchParsingData().symbolToIsoCode;
        }

        @Override
        public Map<String, String> nameMap() {
            return fetchParsingData().nameToIsoCode;
        }

        @Override
        public Map<String, String> getUnitPatterns() {
            return fetchUnitPatterns();
        }

        @Override
        public CurrencyData.CurrencyFormatInfo getFormatInfo(String str) {
            return fetchFormattingData(str).formatInfo;
        }

        @Override
        public CurrencyData.CurrencySpacingInfo getSpacingInfo() {
            CurrencyData.CurrencySpacingInfo currencySpacingInfoFetchSpacingInfo = fetchSpacingInfo();
            if ((!currencySpacingInfoFetchSpacingInfo.hasBeforeCurrency || !currencySpacingInfoFetchSpacingInfo.hasAfterCurrency) && this.fallback) {
                return CurrencyData.CurrencySpacingInfo.DEFAULT;
            }
            return currencySpacingInfoFetchSpacingInfo;
        }

        FormattingData fetchFormattingData(String str) {
            FormattingData formattingData = this.formattingDataCache;
            if (formattingData == null || !formattingData.isoCode.equals(str)) {
                FormattingData formattingData2 = new FormattingData(str);
                CurrencySink currencySink = new CurrencySink(!this.fallback, CurrencySink.EntrypointTable.CURRENCIES);
                currencySink.formattingData = formattingData2;
                this.rb.getAllItemsWithFallbackNoFail("Currencies/" + str, currencySink);
                this.formattingDataCache = formattingData2;
                return formattingData2;
            }
            return formattingData;
        }

        NarrowSymbol fetchNarrowSymbol(String str) {
            NarrowSymbol narrowSymbol = this.narrowSymbolCache;
            if (narrowSymbol == null || !narrowSymbol.isoCode.equals(str)) {
                NarrowSymbol narrowSymbol2 = new NarrowSymbol(str);
                CurrencySink currencySink = new CurrencySink(!this.fallback, CurrencySink.EntrypointTable.CURRENCY_NARROW);
                currencySink.narrowSymbol = narrowSymbol2;
                this.rb.getAllItemsWithFallbackNoFail("Currencies%narrow/" + str, currencySink);
                this.narrowSymbolCache = narrowSymbol2;
                return narrowSymbol2;
            }
            return narrowSymbol;
        }

        String[] fetchPluralsData(String str) {
            String[] strArr = this.pluralsDataCache;
            if (strArr == null || !strArr[0].equals(str)) {
                String[] strArr2 = new String[StandardPlural.COUNT + 1];
                strArr2[0] = str;
                CurrencySink currencySink = new CurrencySink(true ^ this.fallback, CurrencySink.EntrypointTable.CURRENCY_PLURALS);
                currencySink.pluralsData = strArr2;
                this.rb.getAllItemsWithFallbackNoFail("CurrencyPlurals/" + str, currencySink);
                this.pluralsDataCache = strArr2;
                return strArr2;
            }
            return strArr;
        }

        ParsingData fetchParsingData() {
            ParsingData parsingData = this.parsingDataCache.get();
            if (parsingData == null) {
                ParsingData parsingData2 = new ParsingData();
                CurrencySink currencySink = new CurrencySink(!this.fallback, CurrencySink.EntrypointTable.TOP);
                currencySink.parsingData = parsingData2;
                this.rb.getAllItemsWithFallback("", currencySink);
                this.parsingDataCache = new SoftReference<>(parsingData2);
                return parsingData2;
            }
            return parsingData;
        }

        Map<String, String> fetchUnitPatterns() {
            Map<String, String> map = this.unitPatternsCache;
            if (map == null) {
                HashMap map2 = new HashMap();
                CurrencySink currencySink = new CurrencySink(!this.fallback, CurrencySink.EntrypointTable.CURRENCY_UNIT_PATTERNS);
                currencySink.unitPatterns = map2;
                this.rb.getAllItemsWithFallback("CurrencyUnitPatterns", currencySink);
                this.unitPatternsCache = map2;
                return map2;
            }
            return map;
        }

        CurrencyData.CurrencySpacingInfo fetchSpacingInfo() {
            CurrencyData.CurrencySpacingInfo currencySpacingInfo = this.spacingInfoCache;
            if (currencySpacingInfo == null) {
                CurrencyData.CurrencySpacingInfo currencySpacingInfo2 = new CurrencyData.CurrencySpacingInfo();
                CurrencySink currencySink = new CurrencySink(!this.fallback, CurrencySink.EntrypointTable.CURRENCY_SPACING);
                currencySink.spacingInfo = currencySpacingInfo2;
                this.rb.getAllItemsWithFallback("currencySpacing", currencySink);
                this.spacingInfoCache = currencySpacingInfo2;
                return currencySpacingInfo2;
            }
            return currencySpacingInfo;
        }

        private static final class CurrencySink extends UResource.Sink {
            static final boolean $assertionsDisabled = false;
            final EntrypointTable entrypointTable;
            final boolean noRoot;
            FormattingData formattingData = null;
            String[] pluralsData = null;
            ParsingData parsingData = null;
            Map<String, String> unitPatterns = null;
            CurrencyData.CurrencySpacingInfo spacingInfo = null;
            NarrowSymbol narrowSymbol = null;

            enum EntrypointTable {
                TOP,
                CURRENCIES,
                CURRENCY_PLURALS,
                CURRENCY_NARROW,
                CURRENCY_SPACING,
                CURRENCY_UNIT_PATTERNS
            }

            CurrencySink(boolean z, EntrypointTable entrypointTable) {
                this.noRoot = z;
                this.entrypointTable = entrypointTable;
            }

            @Override
            public void put(UResource.Key key, UResource.Value value, boolean z) {
                if (this.noRoot && z) {
                }
                switch (this.entrypointTable) {
                    case TOP:
                        consumeTopTable(key, value);
                        break;
                    case CURRENCIES:
                        consumeCurrenciesEntry(key, value);
                        break;
                    case CURRENCY_PLURALS:
                        consumeCurrencyPluralsEntry(key, value);
                        break;
                    case CURRENCY_NARROW:
                        consumeCurrenciesNarrowEntry(key, value);
                        break;
                    case CURRENCY_SPACING:
                        consumeCurrencySpacingTable(key, value);
                        break;
                    case CURRENCY_UNIT_PATTERNS:
                        consumeCurrencyUnitPatternsTable(key, value);
                        break;
                }
            }

            private void consumeTopTable(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    if (key.contentEquals("Currencies")) {
                        consumeCurrenciesTable(key, value);
                    } else if (key.contentEquals("Currencies%variant")) {
                        consumeCurrenciesVariantTable(key, value);
                    } else if (key.contentEquals("CurrencyPlurals")) {
                        consumeCurrencyPluralsTable(key, value);
                    }
                }
            }

            void consumeCurrenciesTable(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String string = key.toString();
                    if (value.getType() != 8) {
                        throw new ICUException("Unexpected data type in Currencies table for " + string);
                    }
                    UResource.Array array = value.getArray();
                    this.parsingData.symbolToIsoCode.put(string, string);
                    array.getValue(0, value);
                    this.parsingData.symbolToIsoCode.put(value.getString(), string);
                    array.getValue(1, value);
                    this.parsingData.nameToIsoCode.put(value.getString(), string);
                }
            }

            void consumeCurrenciesEntry(UResource.Key key, UResource.Value value) {
                String string = key.toString();
                if (value.getType() != 8) {
                    throw new ICUException("Unexpected data type in Currencies table for " + string);
                }
                UResource.Array array = value.getArray();
                if (this.formattingData.symbol == null) {
                    array.getValue(0, value);
                    this.formattingData.symbol = value.getString();
                }
                if (this.formattingData.displayName == null) {
                    array.getValue(1, value);
                    this.formattingData.displayName = value.getString();
                }
                if (array.getSize() > 2 && this.formattingData.formatInfo == null) {
                    array.getValue(2, value);
                    UResource.Array array2 = value.getArray();
                    array2.getValue(0, value);
                    String string2 = value.getString();
                    array2.getValue(1, value);
                    String string3 = value.getString();
                    array2.getValue(2, value);
                    this.formattingData.formatInfo = new CurrencyData.CurrencyFormatInfo(string, string2, string3, value.getString());
                }
            }

            void consumeCurrenciesNarrowEntry(UResource.Key key, UResource.Value value) {
                if (this.narrowSymbol.narrowSymbol == null) {
                    this.narrowSymbol.narrowSymbol = value.getString();
                }
            }

            void consumeCurrenciesVariantTable(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    this.parsingData.symbolToIsoCode.put(value.getString(), key.toString());
                }
            }

            void consumeCurrencyPluralsTable(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String string = key.toString();
                    UResource.Table table2 = value.getTable();
                    for (int i2 = 0; table2.getKeyAndValue(i2, key, value); i2++) {
                        if (StandardPlural.orNullFromString(key.toString()) == null) {
                            throw new ICUException("Could not make StandardPlural from keyword " + ((Object) key));
                        }
                        this.parsingData.nameToIsoCode.put(value.getString(), string);
                    }
                }
            }

            void consumeCurrencyPluralsEntry(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    StandardPlural standardPluralOrNullFromString = StandardPlural.orNullFromString(key.toString());
                    if (standardPluralOrNullFromString == null) {
                        throw new ICUException("Could not make StandardPlural from keyword " + ((Object) key));
                    }
                    if (this.pluralsData[standardPluralOrNullFromString.ordinal() + 1] == null) {
                        this.pluralsData[1 + standardPluralOrNullFromString.ordinal()] = value.getString();
                    }
                }
            }

            void consumeCurrencySpacingTable(UResource.Key key, UResource.Value value) {
                CurrencyData.CurrencySpacingInfo.SpacingType spacingType;
                CurrencyData.CurrencySpacingInfo.SpacingPattern spacingPattern;
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    if (key.contentEquals("beforeCurrency")) {
                        spacingType = CurrencyData.CurrencySpacingInfo.SpacingType.BEFORE;
                        this.spacingInfo.hasBeforeCurrency = true;
                    } else if (key.contentEquals("afterCurrency")) {
                        spacingType = CurrencyData.CurrencySpacingInfo.SpacingType.AFTER;
                        this.spacingInfo.hasAfterCurrency = true;
                    }
                    UResource.Table table2 = value.getTable();
                    for (int i2 = 0; table2.getKeyAndValue(i2, key, value); i2++) {
                        if (key.contentEquals("currencyMatch")) {
                            spacingPattern = CurrencyData.CurrencySpacingInfo.SpacingPattern.CURRENCY_MATCH;
                        } else if (key.contentEquals("surroundingMatch")) {
                            spacingPattern = CurrencyData.CurrencySpacingInfo.SpacingPattern.SURROUNDING_MATCH;
                        } else if (key.contentEquals("insertBetween")) {
                            spacingPattern = CurrencyData.CurrencySpacingInfo.SpacingPattern.INSERT_BETWEEN;
                        }
                        this.spacingInfo.setSymbolIfNull(spacingType, spacingPattern, value.getString());
                    }
                }
            }

            void consumeCurrencyUnitPatternsTable(UResource.Key key, UResource.Value value) {
                UResource.Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String string = key.toString();
                    if (this.unitPatterns.get(string) == null) {
                        this.unitPatterns.put(string, value.getString());
                    }
                }
            }
        }
    }
}
