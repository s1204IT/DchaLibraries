package android.icu.util;

import android.icu.impl.CacheBase;
import android.icu.impl.CurrencyData;
import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUDebug;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SoftCache;
import android.icu.impl.TextTrieMap;
import android.icu.impl.locale.LanguageTag;
import android.icu.text.CurrencyDisplayNames;
import android.icu.text.CurrencyMetaInfo;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;
import dalvik.system.VMRuntime;
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

    @Deprecated
    public static final int NARROW_SYMBOL_NAME = 3;
    public static final int PLURAL_LONG_NAME = 2;
    public static final int SYMBOL_NAME = 0;
    private static final long serialVersionUID = -5839973855554750484L;
    private static ServiceShim shim;
    private final String isoCode;
    private static final boolean DEBUG = ICUDebug.enabled("currency");
    private static ICUCache<ULocale, List<TextTrieMap<CurrencyStringInfo>>> CURRENCY_NAME_CACHE = new SimpleCache();
    private static final EquivalenceRelation<String> EQUIVALENT_CURRENCY_SYMBOLS = new EquivalenceRelation().add("¥", "￥").add("$", "﹩", "＄").add("₨", "₹").add("£", "₤");
    private static final CacheBase<String, Currency, Void> regionCurrencyCache = new SoftCache<String, Currency, Void>() {
        @Override
        protected Currency createInstance(String str, Void r2) {
            return Currency.loadCurrency(str);
        }
    };
    private static final ULocale UND = new ULocale("und");
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final int[] POW10 = {1, 10, 100, 1000, VMRuntime.SDK_VERSION_CUR_DEVELOPMENT, 100000, 1000000, 10000000, 100000000, 1000000000};

    public enum CurrencyUsage {
        STANDARD,
        CASH
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
                shim = (ServiceShim) Class.forName("android.icu.util.CurrencyServiceShim").newInstance();
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

    public static Currency getInstance(ULocale uLocale) {
        String keywordValue = uLocale.getKeywordValue("currency");
        if (keywordValue != null) {
            return getInstance(keywordValue);
        }
        if (shim == null) {
            return createCurrency(uLocale);
        }
        return shim.createInstance(uLocale);
    }

    public static String[] getAvailableCurrencyCodes(ULocale uLocale, Date date) {
        List<String> tenderCurrencies = getTenderCurrencies(CurrencyMetaInfo.CurrencyFilter.onDate(date).withRegion(ULocale.getRegionForSupplementalData(uLocale, false)));
        if (tenderCurrencies.isEmpty()) {
            return null;
        }
        return (String[]) tenderCurrencies.toArray(new String[tenderCurrencies.size()]);
    }

    public static String[] getAvailableCurrencyCodes(Locale locale, Date date) {
        return getAvailableCurrencyCodes(ULocale.forLocale(locale), date);
    }

    public static Set<Currency> getAvailableCurrencies() {
        List<String> listCurrencies = CurrencyMetaInfo.getInstance().currencies(CurrencyMetaInfo.CurrencyFilter.all());
        HashSet hashSet = new HashSet(listCurrencies.size());
        Iterator<String> it = listCurrencies.iterator();
        while (it.hasNext()) {
            hashSet.add(getInstance(it.next()));
        }
        return hashSet;
    }

    static Currency createCurrency(ULocale uLocale) {
        String variant = uLocale.getVariant();
        if ("EURO".equals(variant)) {
            return getInstance(EUR_STR);
        }
        String regionForSupplementalData = ULocale.getRegionForSupplementalData(uLocale, false);
        if ("PREEURO".equals(variant)) {
            regionForSupplementalData = regionForSupplementalData + '-';
        }
        return regionCurrencyCache.getInstance(regionForSupplementalData, null);
    }

    private static Currency loadCurrency(String str) {
        boolean z;
        if (str.endsWith(LanguageTag.SEP)) {
            str = str.substring(0, str.length() - 1);
            z = true;
        } else {
            z = false;
        }
        List<String> listCurrencies = CurrencyMetaInfo.getInstance().currencies(CurrencyMetaInfo.CurrencyFilter.onRegion(str));
        if (listCurrencies.isEmpty()) {
            return null;
        }
        String str2 = listCurrencies.get(0);
        if (z && EUR_STR.equals(str2)) {
            if (listCurrencies.size() < 2) {
                return null;
            }
            str2 = listCurrencies.get(1);
        }
        return getInstance(str2);
    }

    public static Currency getInstance(String str) {
        if (str == null) {
            throw new NullPointerException("The input currency code is null.");
        }
        if (!isAlpha3Code(str)) {
            throw new IllegalArgumentException("The input currency code is not 3-letter alphabetic code.");
        }
        return (Currency) MeasureUnit.internalGetInstance("currency", str.toUpperCase(Locale.ENGLISH));
    }

    private static boolean isAlpha3Code(String str) {
        if (str.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < 'A' || ((cCharAt > 'Z' && cCharAt < 'a') || cCharAt > 'z')) {
                return false;
            }
        }
        return true;
    }

    public static Currency fromJavaCurrency(java.util.Currency currency) {
        return getInstance(currency.getCurrencyCode());
    }

    public java.util.Currency toJavaCurrency() {
        return java.util.Currency.getInstance(getCurrencyCode());
    }

    public static Object registerInstance(Currency currency, ULocale uLocale) {
        return getShim().registerInstance(currency, uLocale);
    }

    public static boolean unregister(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("registryKey must not be null");
        }
        if (shim == null) {
            return false;
        }
        return shim.unregister(obj);
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

    public static final String[] getKeywordValuesForLocale(String str, ULocale uLocale, boolean z) {
        if (!"currency".equals(str)) {
            return EMPTY_STRING_ARRAY;
        }
        if (!z) {
            return (String[]) getAllTenderCurrencies().toArray(new String[0]);
        }
        if (UND.equals(uLocale)) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> tenderCurrencies = getTenderCurrencies(CurrencyMetaInfo.CurrencyFilter.now().withRegion(ULocale.getRegionForSupplementalData(uLocale, true)));
        if (tenderCurrencies.size() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return (String[]) tenderCurrencies.toArray(new String[tenderCurrencies.size()]);
    }

    public String getCurrencyCode() {
        return this.subType;
    }

    public int getNumericCode() {
        try {
            return UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "currencyNumericCodes", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("codeMap").get(this.subType).getInt();
        } catch (MissingResourceException e) {
            return 0;
        }
    }

    public String getSymbol() {
        return getSymbol(ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getSymbol(Locale locale) {
        return getSymbol(ULocale.forLocale(locale));
    }

    public String getSymbol(ULocale uLocale) {
        return getName(uLocale, 0, new boolean[1]);
    }

    public String getName(Locale locale, int i, boolean[] zArr) {
        return getName(ULocale.forLocale(locale), i, zArr);
    }

    public String getName(ULocale uLocale, int i, boolean[] zArr) {
        if (zArr != null) {
            zArr[0] = false;
        }
        CurrencyDisplayNames currencyDisplayNames = CurrencyDisplayNames.getInstance(uLocale);
        if (i != 3) {
            switch (i) {
                case 0:
                    return currencyDisplayNames.getSymbol(this.subType);
                case 1:
                    return currencyDisplayNames.getName(this.subType);
                default:
                    throw new IllegalArgumentException("bad name style: " + i);
            }
        }
        if (!(currencyDisplayNames instanceof CurrencyData.CurrencyDisplayInfo)) {
            throw new UnsupportedOperationException("Cannot get narrow symbol from custom currency display name provider");
        }
        return ((CurrencyData.CurrencyDisplayInfo) currencyDisplayNames).getNarrowSymbol(this.subType);
    }

    public String getName(Locale locale, int i, String str, boolean[] zArr) {
        return getName(ULocale.forLocale(locale), i, str, zArr);
    }

    public String getName(ULocale uLocale, int i, String str, boolean[] zArr) {
        if (i != 2) {
            return getName(uLocale, i, zArr);
        }
        if (zArr != null) {
            zArr[0] = false;
        }
        return CurrencyDisplayNames.getInstance(uLocale).getPluralName(this.subType, str);
    }

    public String getDisplayName() {
        return getName(Locale.getDefault(), 1, (boolean[]) null);
    }

    public String getDisplayName(Locale locale) {
        return getName(locale, 1, (boolean[]) null);
    }

    @Deprecated
    public static String parse(ULocale uLocale, String str, int i, ParsePosition parsePosition) {
        List<TextTrieMap<CurrencyStringInfo>> currencyTrieVec = getCurrencyTrieVec(uLocale);
        TextTrieMap textTrieMap = currencyTrieVec.get(1);
        CurrencyNameResultHandler currencyNameResultHandler = new CurrencyNameResultHandler();
        textTrieMap.find(str, parsePosition.getIndex(), currencyNameResultHandler);
        String bestCurrencyISOCode = currencyNameResultHandler.getBestCurrencyISOCode();
        int bestMatchLength = currencyNameResultHandler.getBestMatchLength();
        if (i != 1) {
            TextTrieMap textTrieMap2 = currencyTrieVec.get(0);
            CurrencyNameResultHandler currencyNameResultHandler2 = new CurrencyNameResultHandler();
            textTrieMap2.find(str, parsePosition.getIndex(), currencyNameResultHandler2);
            if (currencyNameResultHandler2.getBestMatchLength() > bestMatchLength) {
                bestCurrencyISOCode = currencyNameResultHandler2.getBestCurrencyISOCode();
                bestMatchLength = currencyNameResultHandler2.getBestMatchLength();
            }
        }
        parsePosition.setIndex(parsePosition.getIndex() + bestMatchLength);
        return bestCurrencyISOCode;
    }

    @Deprecated
    public static TextTrieMap<CurrencyStringInfo>.ParseState openParseState(ULocale uLocale, int i, int i2) {
        List<TextTrieMap<CurrencyStringInfo>> currencyTrieVec = getCurrencyTrieVec(uLocale);
        if (i2 == 1) {
            return currencyTrieVec.get(0).openParseState(i);
        }
        return currencyTrieVec.get(1).openParseState(i);
    }

    private static List<TextTrieMap<CurrencyStringInfo>> getCurrencyTrieVec(ULocale uLocale) {
        List<TextTrieMap<CurrencyStringInfo>> list = CURRENCY_NAME_CACHE.get(uLocale);
        if (list != null) {
            return list;
        }
        TextTrieMap textTrieMap = new TextTrieMap(true);
        TextTrieMap textTrieMap2 = new TextTrieMap(false);
        ArrayList arrayList = new ArrayList();
        arrayList.add(textTrieMap2);
        arrayList.add(textTrieMap);
        setupCurrencyTrieVec(uLocale, arrayList);
        CURRENCY_NAME_CACHE.put(uLocale, arrayList);
        return arrayList;
    }

    private static void setupCurrencyTrieVec(ULocale uLocale, List<TextTrieMap<CurrencyStringInfo>> list) {
        TextTrieMap<CurrencyStringInfo> textTrieMap = list.get(0);
        TextTrieMap<CurrencyStringInfo> textTrieMap2 = list.get(1);
        CurrencyDisplayNames currencyDisplayNames = CurrencyDisplayNames.getInstance(uLocale);
        for (Map.Entry<String, String> entry : currencyDisplayNames.symbolMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Iterator<String> it = EQUIVALENT_CURRENCY_SYMBOLS.get(key).iterator();
            while (it.hasNext()) {
                textTrieMap.put(it.next(), new CurrencyStringInfo(value, key));
            }
        }
        for (Map.Entry<String, String> entry2 : currencyDisplayNames.nameMap().entrySet()) {
            String key2 = entry2.getKey();
            textTrieMap2.put(key2, new CurrencyStringInfo(entry2.getValue(), key2));
        }
    }

    @Deprecated
    public static final class CurrencyStringInfo {
        private String currencyString;
        private String isoCode;

        @Deprecated
        public CurrencyStringInfo(String str, String str2) {
            this.isoCode = str;
            this.currencyString = str2;
        }

        @Deprecated
        public String getISOCode() {
            return this.isoCode;
        }

        @Deprecated
        public String getCurrencyString() {
            return this.currencyString;
        }
    }

    private static class CurrencyNameResultHandler implements TextTrieMap.ResultHandler<CurrencyStringInfo> {
        private String bestCurrencyISOCode;
        private int bestMatchLength;

        private CurrencyNameResultHandler() {
        }

        @Override
        public boolean handlePrefixMatch(int i, Iterator<CurrencyStringInfo> it) {
            if (it.hasNext()) {
                this.bestCurrencyISOCode = it.next().getISOCode();
                this.bestMatchLength = i;
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

    public int getDefaultFractionDigits(CurrencyUsage currencyUsage) {
        return CurrencyMetaInfo.getInstance().currencyDigits(this.subType, currencyUsage).fractionDigits;
    }

    public double getRoundingIncrement() {
        return getRoundingIncrement(CurrencyUsage.STANDARD);
    }

    public double getRoundingIncrement(CurrencyUsage currencyUsage) {
        int i;
        CurrencyMetaInfo.CurrencyDigits currencyDigits = CurrencyMetaInfo.getInstance().currencyDigits(this.subType, currencyUsage);
        int i2 = currencyDigits.roundingIncrement;
        if (i2 == 0 || (i = currencyDigits.fractionDigits) < 0 || i >= POW10.length) {
            return 0.0d;
        }
        return ((double) i2) / ((double) POW10[i]);
    }

    @Override
    public String toString() {
        return this.subType;
    }

    protected Currency(String str) {
        super("currency", str);
        this.isoCode = str;
    }

    private static synchronized List<String> getAllTenderCurrencies() {
        List<String> listUnmodifiableList;
        listUnmodifiableList = ALL_TENDER_CODES == null ? null : ALL_TENDER_CODES.get();
        if (listUnmodifiableList == null) {
            listUnmodifiableList = Collections.unmodifiableList(getTenderCurrencies(CurrencyMetaInfo.CurrencyFilter.all()));
            ALL_TENDER_CODES = new SoftReference<>(listUnmodifiableList);
        }
        return listUnmodifiableList;
    }

    private static synchronized Set<String> getAllCurrenciesAsSet() {
        Set<String> setUnmodifiableSet;
        setUnmodifiableSet = ALL_CODES_AS_SET == null ? null : ALL_CODES_AS_SET.get();
        if (setUnmodifiableSet == null) {
            setUnmodifiableSet = Collections.unmodifiableSet(new HashSet(CurrencyMetaInfo.getInstance().currencies(CurrencyMetaInfo.CurrencyFilter.all())));
            ALL_CODES_AS_SET = new SoftReference<>(setUnmodifiableSet);
        }
        return setUnmodifiableSet;
    }

    public static boolean isAvailable(String str, Date date, Date date2) {
        if (!isAlpha3Code(str)) {
            return false;
        }
        if (date != null && date2 != null && date.after(date2)) {
            throw new IllegalArgumentException("To is before from");
        }
        String upperCase = str.toUpperCase(Locale.ENGLISH);
        if (!getAllCurrenciesAsSet().contains(upperCase)) {
            return false;
        }
        if (date == null && date2 == null) {
            return true;
        }
        return CurrencyMetaInfo.getInstance().currencies(CurrencyMetaInfo.CurrencyFilter.onDateRange(date, date2).withCurrency(upperCase)).contains(upperCase);
    }

    private static List<String> getTenderCurrencies(CurrencyMetaInfo.CurrencyFilter currencyFilter) {
        return CurrencyMetaInfo.getInstance().currencies(currencyFilter.withTender());
    }

    private static final class EquivalenceRelation<T> {
        private Map<T, Set<T>> data;

        private EquivalenceRelation() {
            this.data = new HashMap();
        }

        public EquivalenceRelation<T> add(T... tArr) {
            HashSet hashSet = new HashSet();
            for (T t : tArr) {
                if (this.data.containsKey(t)) {
                    throw new IllegalArgumentException("All groups passed to add must be disjoint.");
                }
                hashSet.add(t);
            }
            for (T t2 : tArr) {
                this.data.put(t2, hashSet);
            }
            return this;
        }

        public Set<T> get(T t) {
            Set<T> set = this.data.get(t);
            if (set == null) {
                return Collections.singleton(t);
            }
            return Collections.unmodifiableSet(set);
        }
    }

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnit.MeasureUnitProxy(this.type, this.subType);
    }

    private Object readResolve() throws ObjectStreamException {
        return getInstance(this.isoCode);
    }
}
