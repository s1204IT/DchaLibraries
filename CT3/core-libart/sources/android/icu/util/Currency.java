package android.icu.util;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUDebug;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.TextTrieMap;
import android.icu.text.CurrencyDisplayNames;
import android.icu.text.CurrencyMetaInfo;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;
import java.io.ObjectStreamException;
import java.lang.ref.SoftReference;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class Currency extends MeasureUnit {
    private static SoftReference<Set<String>> ALL_CODES_AS_SET = null;
    private static SoftReference<List<String>> ALL_TENDER_CODES = null;
    private static final String EUR_STR = "EUR";
    public static final int LONG_NAME = 1;
    public static final int PLURAL_LONG_NAME = 2;
    public static final int SYMBOL_NAME = 0;
    private static final long serialVersionUID = -5839973855554750484L;
    private static ServiceShim shim;
    private final String isoCode;
    private static final boolean DEBUG = ICUDebug.enabled("currency");
    private static ICUCache<ULocale, List<TextTrieMap<CurrencyStringInfo>>> CURRENCY_NAME_CACHE = new SimpleCache();
    private static final EquivalenceRelation<String> EQUIVALENT_CURRENCY_SYMBOLS = new EquivalenceRelation(null).add("¥", "￥").add("$", "﹩", "＄").add("₨", "₹").add("£", "₤");
    private static final ICUCache<ULocale, String> currencyCodeCache = new SimpleCache();
    private static final ULocale UND = new ULocale("und");
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    public enum CurrencyUsage {
        STANDARD,
        CASH;

        public static CurrencyUsage[] valuesCustom() {
            return values();
        }
    }

    static abstract class ServiceShim {
        abstract Currency createInstance(ULocale uLocale);

        abstract Locale[] getAvailableLocales();

        abstract ULocale[] getAvailableULocales();

        abstract Object registerInstance(Currency currency, ULocale uLocale);

        abstract boolean unregister(Object obj);

        ServiceShim() {
        }
    }

    private static ServiceShim getShim() {
        if (shim == null) {
            try {
                Class<?> cls = Class.forName("android.icu.util.CurrencyServiceShim");
                shim = (ServiceShim) cls.newInstance();
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                throw new RuntimeException(e.getMessage());
            }
        }
        return shim;
    }

    public static Currency getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public static Currency getInstance(ULocale locale) {
        String currency = locale.getKeywordValue("currency");
        if (currency != null) {
            return getInstance(currency);
        }
        if (shim == null) {
            return createCurrency(locale);
        }
        return shim.createInstance(locale);
    }

    public static String[] getAvailableCurrencyCodes(ULocale loc, Date d) {
        CurrencyMetaInfo.CurrencyFilter filter = CurrencyMetaInfo.CurrencyFilter.onDate(d).withRegion(loc.getCountry());
        List<String> list = getTenderCurrencies(filter);
        if (list.isEmpty()) {
            return null;
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public static String[] getAvailableCurrencyCodes(Locale loc, Date d) {
        return getAvailableCurrencyCodes(ULocale.forLocale(loc), d);
    }

    public static Set<Currency> getAvailableCurrencies() {
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        List<String> list = info.currencies(CurrencyMetaInfo.CurrencyFilter.all());
        HashSet<Currency> resultSet = new HashSet<>(list.size());
        for (String code : list) {
            resultSet.add(getInstance(code));
        }
        return resultSet;
    }

    static Currency createCurrency(ULocale loc) {
        String variant = loc.getVariant();
        if ("EURO".equals(variant)) {
            return getInstance(EUR_STR);
        }
        String code = currencyCodeCache.get(loc);
        if (code == null) {
            String country = loc.getCountry();
            CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
            List<String> list = info.currencies(CurrencyMetaInfo.CurrencyFilter.onRegion(country));
            if (list.size() <= 0) {
                return null;
            }
            code = list.get(0);
            boolean isPreEuro = "PREEURO".equals(variant);
            if (isPreEuro && EUR_STR.equals(code)) {
                if (list.size() < 2) {
                    return null;
                }
                code = list.get(1);
            }
            currencyCodeCache.put(loc, code);
        }
        return getInstance(code);
    }

    public static Currency getInstance(String theISOCode) {
        if (theISOCode == null) {
            throw new NullPointerException("The input currency code is null.");
        }
        if (!isAlpha3Code(theISOCode)) {
            throw new IllegalArgumentException("The input currency code is not 3-letter alphabetic code.");
        }
        return (Currency) MeasureUnit.internalGetInstance("currency", theISOCode.toUpperCase(Locale.ENGLISH));
    }

    private static boolean isAlpha3Code(String code) {
        if (code.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char ch = code.charAt(i);
            if (ch < 'A' || ((ch > 'Z' && ch < 'a') || ch > 'z')) {
                return false;
            }
        }
        return true;
    }

    public static Object registerInstance(Currency currency, ULocale locale) {
        return getShim().registerInstance(currency, locale);
    }

    public static boolean unregister(Object registryKey) {
        if (registryKey == null) {
            throw new IllegalArgumentException("registryKey must not be null");
        }
        if (shim == null) {
            return false;
        }
        return shim.unregister(registryKey);
    }

    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return shim.getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return shim.getAvailableULocales();
    }

    public static final String[] getKeywordValuesForLocale(String key, ULocale locale, boolean commonlyUsed) {
        if (!"currency".equals(key)) {
            return EMPTY_STRING_ARRAY;
        }
        if (!commonlyUsed) {
            return (String[]) getAllTenderCurrencies().toArray(new String[0]);
        }
        String prefRegion = locale.getCountry();
        if (prefRegion.length() == 0) {
            if (UND.equals(locale)) {
                return EMPTY_STRING_ARRAY;
            }
            ULocale loc = ULocale.addLikelySubtags(locale);
            prefRegion = loc.getCountry();
        }
        CurrencyMetaInfo.CurrencyFilter filter = CurrencyMetaInfo.CurrencyFilter.now().withRegion(prefRegion);
        List<String> result = getTenderCurrencies(filter);
        if (result.size() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public String getCurrencyCode() {
        return this.subType;
    }

    public int getNumericCode() {
        try {
            UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "currencyNumericCodes", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle codeMap = bundle.get("codeMap");
            UResourceBundle numCode = codeMap.get(this.subType);
            int result = numCode.getInt();
            return result;
        } catch (MissingResourceException e) {
            return 0;
        }
    }

    public String getSymbol() {
        return getSymbol(ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getSymbol(Locale loc) {
        return getSymbol(ULocale.forLocale(loc));
    }

    public String getSymbol(ULocale uloc) {
        return getName(uloc, 0, new boolean[1]);
    }

    public String getName(Locale locale, int nameStyle, boolean[] isChoiceFormat) {
        return getName(ULocale.forLocale(locale), nameStyle, isChoiceFormat);
    }

    public String getName(ULocale locale, int nameStyle, boolean[] isChoiceFormat) {
        if (nameStyle != 0 && nameStyle != 1) {
            throw new IllegalArgumentException("bad name style: " + nameStyle);
        }
        if (isChoiceFormat != null) {
            isChoiceFormat[0] = false;
        }
        CurrencyDisplayNames names = CurrencyDisplayNames.getInstance(locale);
        return nameStyle == 0 ? names.getSymbol(this.subType) : names.getName(this.subType);
    }

    public String getName(Locale locale, int nameStyle, String pluralCount, boolean[] isChoiceFormat) {
        return getName(ULocale.forLocale(locale), nameStyle, pluralCount, isChoiceFormat);
    }

    public String getName(ULocale locale, int nameStyle, String pluralCount, boolean[] isChoiceFormat) {
        if (nameStyle != 2) {
            return getName(locale, nameStyle, isChoiceFormat);
        }
        if (isChoiceFormat != null) {
            isChoiceFormat[0] = false;
        }
        CurrencyDisplayNames names = CurrencyDisplayNames.getInstance(locale);
        return names.getPluralName(this.subType, pluralCount);
    }

    public String getDisplayName() {
        return getName(Locale.getDefault(), 1, (boolean[]) null);
    }

    public String getDisplayName(Locale locale) {
        return getName(locale, 1, (boolean[]) null);
    }

    @Deprecated
    public static String parse(ULocale locale, String text, int type, ParsePosition pos) {
        CurrencyNameResultHandler currencyNameResultHandler = null;
        List<TextTrieMap<CurrencyStringInfo>> currencyTrieVec = CURRENCY_NAME_CACHE.get(locale);
        if (currencyTrieVec == null) {
            TextTrieMap<CurrencyStringInfo> currencyNameTrie = new TextTrieMap<>(true);
            TextTrieMap<CurrencyStringInfo> currencySymbolTrie = new TextTrieMap<>(false);
            currencyTrieVec = new ArrayList<>();
            currencyTrieVec.add(currencySymbolTrie);
            currencyTrieVec.add(currencyNameTrie);
            setupCurrencyTrieVec(locale, currencyTrieVec);
            CURRENCY_NAME_CACHE.put(locale, currencyTrieVec);
        }
        TextTrieMap<CurrencyStringInfo> currencyNameTrie2 = currencyTrieVec.get(1);
        CurrencyNameResultHandler handler = new CurrencyNameResultHandler(currencyNameResultHandler);
        currencyNameTrie2.find(text, pos.getIndex(), handler);
        String isoResult = handler.getBestCurrencyISOCode();
        int maxLength = handler.getBestMatchLength();
        if (type != 1) {
            TextTrieMap<CurrencyStringInfo> currencySymbolTrie2 = currencyTrieVec.get(0);
            CurrencyNameResultHandler handler2 = new CurrencyNameResultHandler(currencyNameResultHandler);
            currencySymbolTrie2.find(text, pos.getIndex(), handler2);
            if (handler2.getBestMatchLength() > maxLength) {
                isoResult = handler2.getBestCurrencyISOCode();
                maxLength = handler2.getBestMatchLength();
            }
        }
        int start = pos.getIndex();
        pos.setIndex(start + maxLength);
        return isoResult;
    }

    private static void setupCurrencyTrieVec(ULocale locale, List<TextTrieMap<CurrencyStringInfo>> trieVec) {
        TextTrieMap<CurrencyStringInfo> symTrie = trieVec.get(0);
        TextTrieMap<CurrencyStringInfo> trie = trieVec.get(1);
        CurrencyDisplayNames names = CurrencyDisplayNames.getInstance(locale);
        for (Map.Entry<String, String> e : names.symbolMap().entrySet()) {
            String symbol = e.getKey();
            String isoCode = e.getValue();
            for (String equivalentSymbol : EQUIVALENT_CURRENCY_SYMBOLS.get(symbol)) {
                symTrie.put(equivalentSymbol, new CurrencyStringInfo(isoCode, symbol));
            }
        }
        for (Map.Entry<String, String> e2 : names.nameMap().entrySet()) {
            String name = e2.getKey();
            String isoCode2 = e2.getValue();
            trie.put(name, new CurrencyStringInfo(isoCode2, name));
        }
    }

    private static final class CurrencyStringInfo {
        private String currencyString;
        private String isoCode;

        public CurrencyStringInfo(String isoCode, String currencyString) {
            this.isoCode = isoCode;
            this.currencyString = currencyString;
        }

        public String getISOCode() {
            return this.isoCode;
        }

        public String getCurrencyString() {
            return this.currencyString;
        }
    }

    private static class CurrencyNameResultHandler implements TextTrieMap.ResultHandler<CurrencyStringInfo> {
        private String bestCurrencyISOCode;
        private int bestMatchLength;

        CurrencyNameResultHandler(CurrencyNameResultHandler currencyNameResultHandler) {
            this();
        }

        private CurrencyNameResultHandler() {
        }

        @Override
        public boolean handlePrefixMatch(int matchLength, Iterator<CurrencyStringInfo> values) {
            if (values.hasNext()) {
                this.bestCurrencyISOCode = values.next().getISOCode();
                this.bestMatchLength = matchLength;
                return true;
            }
            return true;
        }

        public String getBestCurrencyISOCode() {
            return this.bestCurrencyISOCode;
        }

        public int getBestMatchLength() {
            return this.bestMatchLength;
        }
    }

    public int getDefaultFractionDigits() {
        return getDefaultFractionDigits(CurrencyUsage.STANDARD);
    }

    public int getDefaultFractionDigits(CurrencyUsage Usage) {
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        CurrencyMetaInfo.CurrencyDigits digits = info.currencyDigits(this.subType, Usage);
        return digits.fractionDigits;
    }

    public double getRoundingIncrement() {
        return getRoundingIncrement(CurrencyUsage.STANDARD);
    }

    public double getRoundingIncrement(CurrencyUsage Usage) {
        int data0;
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        CurrencyMetaInfo.CurrencyDigits digits = info.currencyDigits(this.subType, Usage);
        int data1 = digits.roundingIncrement;
        if (data1 != 0 && (data0 = digits.fractionDigits) >= 0 && data0 < POW10.length) {
            return ((double) data1) / ((double) POW10[data0]);
        }
        return 0.0d;
    }

    @Override
    public String toString() {
        return this.subType;
    }

    protected Currency(String theISOCode) {
        super("currency", theISOCode);
        this.isoCode = theISOCode;
    }

    private static synchronized List<String> getAllTenderCurrencies() {
        List<String> all;
        all = ALL_TENDER_CODES == null ? null : ALL_TENDER_CODES.get();
        if (all == null) {
            CurrencyMetaInfo.CurrencyFilter filter = CurrencyMetaInfo.CurrencyFilter.all();
            all = Collections.unmodifiableList(getTenderCurrencies(filter));
            ALL_TENDER_CODES = new SoftReference<>(all);
        }
        return all;
    }

    private static synchronized Set<String> getAllCurrenciesAsSet() {
        Set<String> all;
        all = ALL_CODES_AS_SET == null ? null : ALL_CODES_AS_SET.get();
        if (all == null) {
            CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
            all = Collections.unmodifiableSet(new HashSet(info.currencies(CurrencyMetaInfo.CurrencyFilter.all())));
            ALL_CODES_AS_SET = new SoftReference<>(all);
        }
        return all;
    }

    public static boolean isAvailable(String code, Date from, Date to) {
        if (!isAlpha3Code(code)) {
            return false;
        }
        if (from != null && to != null && from.after(to)) {
            throw new IllegalArgumentException("To is before from");
        }
        String code2 = code.toUpperCase(Locale.ENGLISH);
        boolean isKnown = getAllCurrenciesAsSet().contains(code2);
        if (!isKnown) {
            return false;
        }
        if (from == null && to == null) {
            return true;
        }
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        List<String> allActive = info.currencies(CurrencyMetaInfo.CurrencyFilter.onDateRange(from, to).withCurrency(code2));
        return allActive.contains(code2);
    }

    private static List<String> getTenderCurrencies(CurrencyMetaInfo.CurrencyFilter filter) {
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        return info.currencies(filter.withTender());
    }

    private static final class EquivalenceRelation<T> {
        private Map<T, Set<T>> data;

        EquivalenceRelation(EquivalenceRelation equivalenceRelation) {
            this();
        }

        private EquivalenceRelation() {
            this.data = new HashMap();
        }

        public EquivalenceRelation<T> add(T... items) {
            Set<T> group = new HashSet<>();
            for (T item : items) {
                if (this.data.containsKey(item)) {
                    throw new IllegalArgumentException("All groups passed to add must be disjoint.");
                }
                group.add(item);
            }
            for (T t : items) {
                this.data.put(t, group);
            }
            return this;
        }

        public Set<T> get(T item) {
            Set<T> result = this.data.get(item);
            if (result == null) {
                return Collections.singleton(item);
            }
            return Collections.unmodifiableSet(result);
        }
    }

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnit.MeasureUnitProxy(this.type, this.subType);
    }

    private Object readResolve() throws ObjectStreamException {
        return getInstance(this.isoCode);
    }
}
