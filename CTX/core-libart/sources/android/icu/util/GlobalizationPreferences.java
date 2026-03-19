package android.icu.util;

import android.icu.impl.Utility;
import android.icu.impl.locale.BaseLocale;
import android.icu.text.BreakIterator;
import android.icu.text.Collator;
import android.icu.text.DateFormat;
import android.icu.text.NumberFormat;
import android.icu.text.SimpleDateFormat;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class GlobalizationPreferences implements Freezable<GlobalizationPreferences> {
    public static final int BI_CHARACTER = 0;
    private static final int BI_LIMIT = 5;
    public static final int BI_LINE = 2;
    public static final int BI_SENTENCE = 3;
    public static final int BI_TITLE = 4;
    public static final int BI_WORD = 1;
    public static final int DF_FULL = 0;
    private static final int DF_LIMIT = 5;
    public static final int DF_LONG = 1;
    public static final int DF_MEDIUM = 2;
    public static final int DF_NONE = 4;
    public static final int DF_SHORT = 3;
    public static final int ID_CURRENCY = 7;
    public static final int ID_CURRENCY_SYMBOL = 8;
    public static final int ID_KEYWORD = 5;
    public static final int ID_KEYWORD_VALUE = 6;
    public static final int ID_LANGUAGE = 1;
    public static final int ID_LOCALE = 0;
    public static final int ID_SCRIPT = 2;
    public static final int ID_TERRITORY = 3;
    public static final int ID_TIMEZONE = 9;
    public static final int ID_VARIANT = 4;
    public static final int NF_CURRENCY = 1;
    public static final int NF_INTEGER = 4;
    private static final int NF_LIMIT = 5;
    public static final int NF_NUMBER = 0;
    public static final int NF_PERCENT = 2;
    public static final int NF_SCIENTIFIC = 3;
    private static final int TYPE_BREAKITERATOR = 5;
    private static final int TYPE_CALENDAR = 1;
    private static final int TYPE_COLLATOR = 4;
    private static final int TYPE_DATEFORMAT = 2;
    private static final int TYPE_GENERIC = 0;
    private static final int TYPE_LIMIT = 6;
    private static final int TYPE_NUMBERFORMAT = 3;
    private static final HashMap<ULocale, BitSet> available_locales = new HashMap<>();
    private static final String[][] language_territory_hack;
    private static final Map<String, String> language_territory_hack_map;
    static final String[][] territory_tzid_hack;
    static final Map<String, String> territory_tzid_hack_map;
    private BreakIterator[] breakIterators;
    private Calendar calendar;
    private Collator collator;
    private Currency currency;
    private DateFormat[][] dateFormats;
    private volatile boolean frozen;
    private List<ULocale> implicitLocales;
    private List<ULocale> locales;
    private NumberFormat[] numberFormats;
    private String territory;
    private TimeZone timezone;

    public GlobalizationPreferences() {
        reset();
    }

    public GlobalizationPreferences setLocales(List<ULocale> list) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.locales = processLocales(list);
        return this;
    }

    public List<ULocale> getLocales() {
        if (this.locales == null) {
            return guessLocales();
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.locales);
        return arrayList;
    }

    public ULocale getLocale(int i) {
        List<ULocale> listGuessLocales = this.locales;
        if (listGuessLocales == null) {
            listGuessLocales = guessLocales();
        }
        if (i >= 0 && i < listGuessLocales.size()) {
            return listGuessLocales.get(i);
        }
        return null;
    }

    public GlobalizationPreferences setLocales(ULocale[] uLocaleArr) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        return setLocales(Arrays.asList(uLocaleArr));
    }

    public GlobalizationPreferences setLocale(ULocale uLocale) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        return setLocales(new ULocale[]{uLocale});
    }

    public GlobalizationPreferences setLocales(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        try {
            return setLocales(ULocale.parseAcceptLanguage(str, true));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid Accept-Language string");
        }
    }

    public ResourceBundle getResourceBundle(String str) {
        return getResourceBundle(str, null);
    }

    public ResourceBundle getResourceBundle(String str, ClassLoader classLoader) {
        UResourceBundle bundleInstance;
        List<ULocale> locales = getLocales();
        int i = 0;
        ?? name = 0;
        UResourceBundle uResourceBundle = null;
        UResourceBundle uResourceBundle2 = null;
        while (true) {
            if (i < locales.size()) {
                ?? string = locales.get(i).toString();
                if (name != 0 && string.equals(name)) {
                    break;
                }
                if (classLoader == null) {
                    try {
                        bundleInstance = UResourceBundle.getBundleInstance(str, (String) string);
                    } catch (MissingResourceException e) {
                        name = 0;
                    }
                } else {
                    bundleInstance = UResourceBundle.getBundleInstance(str, (String) string, classLoader);
                }
                uResourceBundle = bundleInstance;
                if (uResourceBundle != null) {
                    name = uResourceBundle.getULocale().getName();
                    if (name.equals(string)) {
                        break;
                    }
                    if (uResourceBundle2 == null) {
                        uResourceBundle2 = uResourceBundle;
                    }
                } else {
                    continue;
                }
                i++;
                name = name;
            } else {
                uResourceBundle = uResourceBundle2;
                break;
            }
        }
        if (uResourceBundle == null) {
            throw new MissingResourceException("Can't find bundle for base name " + str, str, "");
        }
        return uResourceBundle;
    }

    public GlobalizationPreferences setTerritory(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.territory = str;
        return this;
    }

    public String getTerritory() {
        if (this.territory == null) {
            return guessTerritory();
        }
        return this.territory;
    }

    public GlobalizationPreferences setCurrency(Currency currency) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.currency = currency;
        return this;
    }

    public Currency getCurrency() {
        if (this.currency == null) {
            return guessCurrency();
        }
        return this.currency;
    }

    public GlobalizationPreferences setCalendar(Calendar calendar) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.calendar = (Calendar) calendar.clone();
        return this;
    }

    public Calendar getCalendar() {
        if (this.calendar == null) {
            return guessCalendar();
        }
        Calendar calendar = (Calendar) this.calendar.clone();
        calendar.setTimeZone(getTimeZone());
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar;
    }

    public GlobalizationPreferences setTimeZone(TimeZone timeZone) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.timezone = (TimeZone) timeZone.clone();
        return this;
    }

    public TimeZone getTimeZone() {
        if (this.timezone == null) {
            return guessTimeZone();
        }
        return this.timezone.cloneAsThawed();
    }

    public Collator getCollator() {
        if (this.collator == null) {
            return guessCollator();
        }
        try {
            return (Collator) this.collator.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Error in cloning collator", e);
        }
    }

    public GlobalizationPreferences setCollator(Collator collator) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        try {
            this.collator = (Collator) collator.clone();
            return this;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Error in cloning collator", e);
        }
    }

    public BreakIterator getBreakIterator(int i) {
        if (i < 0 || i >= 5) {
            throw new IllegalArgumentException("Illegal break iterator type");
        }
        if (this.breakIterators == null || this.breakIterators[i] == null) {
            return guessBreakIterator(i);
        }
        return (BreakIterator) this.breakIterators[i].clone();
    }

    public GlobalizationPreferences setBreakIterator(int i, BreakIterator breakIterator) {
        if (i < 0 || i >= 5) {
            throw new IllegalArgumentException("Illegal break iterator type");
        }
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (this.breakIterators == null) {
            this.breakIterators = new BreakIterator[5];
        }
        this.breakIterators[i] = (BreakIterator) breakIterator.clone();
        return this;
    }

    public String getDisplayName(String str, int i) {
        String displayKeywordValue;
        String strSubstring;
        String displayName = str;
        for (ULocale uLocale : getLocales()) {
            int i2 = 0;
            if (isAvailableLocale(uLocale, 0)) {
                switch (i) {
                    case 0:
                        displayName = ULocale.getDisplayName(str, uLocale);
                        if (!str.equals(displayName)) {
                            return displayName;
                        }
                        break;
                        break;
                    case 1:
                        displayName = ULocale.getDisplayLanguage(str, uLocale);
                        if (!str.equals(displayName)) {
                        }
                        break;
                    case 2:
                        displayName = ULocale.getDisplayScript("und-" + str, uLocale);
                        if (!str.equals(displayName)) {
                        }
                        break;
                    case 3:
                        displayName = ULocale.getDisplayCountry("und-" + str, uLocale);
                        if (!str.equals(displayName)) {
                        }
                        break;
                    case 4:
                        displayName = ULocale.getDisplayVariant("und-QQ-" + str, uLocale);
                        if (!str.equals(displayName)) {
                        }
                        break;
                    case 5:
                        displayName = ULocale.getDisplayKeyword(str, uLocale);
                        if (!str.equals(displayName)) {
                        }
                        break;
                    case 6:
                        String[] strArr = new String[2];
                        Utility.split(str, '=', strArr);
                        displayKeywordValue = ULocale.getDisplayKeywordValue("und@" + str, strArr[0], uLocale);
                        if (displayKeywordValue.equals(strArr[1])) {
                            displayName = displayKeywordValue;
                            break;
                        } else {
                            displayName = displayKeywordValue;
                            if (!str.equals(displayName)) {
                            }
                        }
                        break;
                    case 7:
                    case 8:
                        Currency currency = new Currency(str);
                        if (i == 7) {
                            i2 = 1;
                        }
                        displayName = currency.getName(uLocale, i2, new boolean[1]);
                        if (!str.equals(displayName)) {
                        }
                        break;
                    case 9:
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateFormat.GENERIC_TZ, uLocale);
                        simpleDateFormat.setTimeZone(TimeZone.getFrozenTimeZone(str));
                        displayKeywordValue = simpleDateFormat.format(new Date());
                        int iIndexOf = displayKeywordValue.indexOf(40);
                        int iIndexOf2 = displayKeywordValue.indexOf(41);
                        if (iIndexOf != -1 && iIndexOf2 != -1 && iIndexOf2 - iIndexOf == 3) {
                            strSubstring = displayKeywordValue.substring(iIndexOf + 1, iIndexOf2);
                        } else {
                            strSubstring = displayKeywordValue;
                        }
                        if (strSubstring.length() == 2) {
                            int i3 = 0;
                            while (true) {
                                if (i3 < 2) {
                                    char cCharAt = strSubstring.charAt(i3);
                                    if (cCharAt >= 'A' && 'Z' >= cCharAt) {
                                        i3++;
                                    }
                                } else {
                                    i2 = 1;
                                }
                            }
                        }
                        if (i2 != 0) {
                            displayName = displayKeywordValue;
                            break;
                        } else {
                            displayName = displayKeywordValue;
                            if (!str.equals(displayName)) {
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown type: " + i);
                }
            }
        }
        return displayName;
    }

    public GlobalizationPreferences setDateFormat(int i, int i2, DateFormat dateFormat) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (this.dateFormats == null) {
            this.dateFormats = (DateFormat[][]) Array.newInstance((Class<?>) DateFormat.class, 5, 5);
        }
        this.dateFormats[i][i2] = (DateFormat) dateFormat.clone();
        return this;
    }

    public DateFormat getDateFormat(int i, int i2) {
        if ((i == 4 && i2 == 4) || i < 0 || i >= 5 || i2 < 0 || i2 >= 5) {
            throw new IllegalArgumentException("Illegal date format style arguments");
        }
        DateFormat dateFormat = null;
        if (this.dateFormats != null) {
            dateFormat = this.dateFormats[i][i2];
        }
        if (dateFormat != null) {
            DateFormat dateFormat2 = (DateFormat) dateFormat.clone();
            dateFormat2.setTimeZone(getTimeZone());
            return dateFormat2;
        }
        return guessDateFormat(i, i2);
    }

    public NumberFormat getNumberFormat(int i) {
        if (i < 0 || i >= 5) {
            throw new IllegalArgumentException("Illegal number format type");
        }
        NumberFormat numberFormat = null;
        if (this.numberFormats != null) {
            numberFormat = this.numberFormats[i];
        }
        if (numberFormat != null) {
            return (NumberFormat) numberFormat.clone();
        }
        return guessNumberFormat(i);
    }

    public GlobalizationPreferences setNumberFormat(int i, NumberFormat numberFormat) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        if (this.numberFormats == null) {
            this.numberFormats = new NumberFormat[5];
        }
        this.numberFormats[i] = (NumberFormat) numberFormat.clone();
        return this;
    }

    public GlobalizationPreferences reset() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify immutable object");
        }
        this.locales = null;
        this.territory = null;
        this.calendar = null;
        this.collator = null;
        this.breakIterators = null;
        this.timezone = null;
        this.currency = null;
        this.dateFormats = null;
        this.numberFormats = null;
        this.implicitLocales = null;
        return this;
    }

    protected List<ULocale> processLocales(List<ULocale> list) {
        boolean z;
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (true) {
            boolean z2 = true;
            if (i >= list.size()) {
                break;
            }
            ULocale uLocale = list.get(i);
            String language = uLocale.getLanguage();
            String script = uLocale.getScript();
            String country = uLocale.getCountry();
            String variant = uLocale.getVariant();
            int i2 = 0;
            while (true) {
                if (i2 < arrayList.size()) {
                    ULocale uLocale2 = (ULocale) arrayList.get(i2);
                    if (uLocale2.getLanguage().equals(language)) {
                        String script2 = uLocale2.getScript();
                        String country2 = uLocale2.getCountry();
                        String variant2 = uLocale2.getVariant();
                        if (!script2.equals(script)) {
                            if (script2.length() == 0 && country2.length() == 0 && variant2.length() == 0) {
                                arrayList.add(i2, uLocale);
                                break;
                            }
                            if (script2.length() == 0 && country2.equals(country)) {
                                arrayList.add(i2, uLocale);
                                break;
                            }
                            if (script.length() == 0 && country.length() > 0 && country2.length() == 0) {
                                arrayList.add(i2, uLocale);
                                break;
                            }
                        } else {
                            if (!country2.equals(country) && country2.length() == 0 && variant2.length() == 0) {
                                arrayList.add(i2, uLocale);
                                break;
                            }
                            if (!variant2.equals(variant) && variant2.length() == 0) {
                                arrayList.add(i2, uLocale);
                                break;
                            }
                        }
                    }
                    i2++;
                } else {
                    z2 = false;
                    break;
                }
            }
            if (!z2) {
                arrayList.add(uLocale);
            }
            i++;
        }
        int i3 = 0;
        while (i3 < arrayList.size()) {
            ULocale fallback = (ULocale) arrayList.get(i3);
            while (true) {
                fallback = fallback.getFallback();
                if (fallback == null || fallback.getLanguage().length() == 0) {
                    break;
                }
                i3++;
                arrayList.add(i3, fallback);
            }
            i3++;
        }
        int i4 = 0;
        while (i4 < arrayList.size() - 1) {
            ULocale uLocale3 = (ULocale) arrayList.get(i4);
            int i5 = i4 + 1;
            int i6 = i5;
            while (true) {
                if (i6 < arrayList.size()) {
                    if (!uLocale3.equals(arrayList.get(i6))) {
                        i6++;
                    } else {
                        arrayList.remove(i4);
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                i4 = i5;
            }
        }
        return arrayList;
    }

    protected DateFormat guessDateFormat(int i, int i2) {
        ULocale availableLocale = getAvailableLocale(2);
        if (availableLocale == null) {
            availableLocale = ULocale.ROOT;
        }
        if (i2 == 4) {
            return DateFormat.getDateInstance(getCalendar(), i, availableLocale);
        }
        if (i == 4) {
            return DateFormat.getTimeInstance(getCalendar(), i2, availableLocale);
        }
        return DateFormat.getDateTimeInstance(getCalendar(), i, i2, availableLocale);
    }

    protected NumberFormat guessNumberFormat(int i) {
        ULocale availableLocale = getAvailableLocale(3);
        if (availableLocale == null) {
            availableLocale = ULocale.ROOT;
        }
        switch (i) {
            case 0:
                return NumberFormat.getInstance(availableLocale);
            case 1:
                NumberFormat currencyInstance = NumberFormat.getCurrencyInstance(availableLocale);
                currencyInstance.setCurrency(getCurrency());
                return currencyInstance;
            case 2:
                return NumberFormat.getPercentInstance(availableLocale);
            case 3:
                return NumberFormat.getScientificInstance(availableLocale);
            case 4:
                return NumberFormat.getIntegerInstance(availableLocale);
            default:
                throw new IllegalArgumentException("Unknown number format style");
        }
    }

    protected String guessTerritory() {
        Iterator<ULocale> it = getLocales().iterator();
        while (it.hasNext()) {
            String country = it.next().getCountry();
            if (country.length() != 0) {
                return country;
            }
        }
        ULocale locale = getLocale(0);
        String language = locale.getLanguage();
        String script = locale.getScript();
        String str = null;
        if (script.length() != 0) {
            str = language_territory_hack_map.get(language + BaseLocale.SEP + script);
        }
        if (str == null) {
            str = language_territory_hack_map.get(language);
        }
        if (str == null) {
            return "US";
        }
        return str;
    }

    protected Currency guessCurrency() {
        return Currency.getInstance(new ULocale("und-" + getTerritory()));
    }

    protected List<ULocale> guessLocales() {
        if (this.implicitLocales == null) {
            ArrayList arrayList = new ArrayList(1);
            arrayList.add(ULocale.getDefault());
            this.implicitLocales = processLocales(arrayList);
        }
        return this.implicitLocales;
    }

    protected Collator guessCollator() {
        ULocale availableLocale = getAvailableLocale(4);
        if (availableLocale == null) {
            availableLocale = ULocale.ROOT;
        }
        return Collator.getInstance(availableLocale);
    }

    protected BreakIterator guessBreakIterator(int i) {
        ULocale availableLocale = getAvailableLocale(5);
        if (availableLocale == null) {
            availableLocale = ULocale.ROOT;
        }
        switch (i) {
            case 0:
                return BreakIterator.getCharacterInstance(availableLocale);
            case 1:
                return BreakIterator.getWordInstance(availableLocale);
            case 2:
                return BreakIterator.getLineInstance(availableLocale);
            case 3:
                return BreakIterator.getSentenceInstance(availableLocale);
            case 4:
                return BreakIterator.getTitleInstance(availableLocale);
            default:
                throw new IllegalArgumentException("Unknown break iterator type");
        }
    }

    protected TimeZone guessTimeZone() {
        String str = territory_tzid_hack_map.get(getTerritory());
        if (str == null) {
            String[] availableIDs = TimeZone.getAvailableIDs(getTerritory());
            if (availableIDs.length == 0) {
                str = "Etc/GMT";
            } else {
                int i = 0;
                int i2 = 0;
                while (i2 < availableIDs.length && availableIDs[i2].indexOf("/") < 0) {
                    i2++;
                }
                if (i2 <= availableIDs.length) {
                    i = i2;
                }
                str = availableIDs[i];
            }
        }
        return TimeZone.getTimeZone(str);
    }

    protected Calendar guessCalendar() {
        ULocale availableLocale = getAvailableLocale(1);
        if (availableLocale == null) {
            availableLocale = ULocale.US;
        }
        return Calendar.getInstance(getTimeZone(), availableLocale);
    }

    private ULocale getAvailableLocale(int i) {
        List<ULocale> locales = getLocales();
        for (int i2 = 0; i2 < locales.size(); i2++) {
            ULocale uLocale = locales.get(i2);
            if (isAvailableLocale(uLocale, i)) {
                return uLocale;
            }
        }
        return null;
    }

    private boolean isAvailableLocale(ULocale uLocale, int i) {
        BitSet bitSet = available_locales.get(uLocale);
        if (bitSet != null && bitSet.get(i)) {
            return true;
        }
        return false;
    }

    static {
        ULocale[] availableLocales = ULocale.getAvailableLocales();
        for (ULocale uLocale : availableLocales) {
            BitSet bitSet = new BitSet(6);
            available_locales.put(uLocale, bitSet);
            bitSet.set(0);
        }
        ULocale[] availableULocales = Calendar.getAvailableULocales();
        for (int i = 0; i < availableULocales.length; i++) {
            BitSet bitSet2 = available_locales.get(availableULocales[i]);
            if (bitSet2 == null) {
                bitSet2 = new BitSet(6);
                available_locales.put(availableLocales[i], bitSet2);
            }
            bitSet2.set(1);
        }
        ULocale[] availableULocales2 = DateFormat.getAvailableULocales();
        for (int i2 = 0; i2 < availableULocales2.length; i2++) {
            BitSet bitSet3 = available_locales.get(availableULocales2[i2]);
            if (bitSet3 == null) {
                bitSet3 = new BitSet(6);
                available_locales.put(availableLocales[i2], bitSet3);
            }
            bitSet3.set(2);
        }
        ULocale[] availableULocales3 = NumberFormat.getAvailableULocales();
        for (int i3 = 0; i3 < availableULocales3.length; i3++) {
            BitSet bitSet4 = available_locales.get(availableULocales3[i3]);
            if (bitSet4 == null) {
                bitSet4 = new BitSet(6);
                available_locales.put(availableLocales[i3], bitSet4);
            }
            bitSet4.set(3);
        }
        ULocale[] availableULocales4 = Collator.getAvailableULocales();
        for (int i4 = 0; i4 < availableULocales4.length; i4++) {
            BitSet bitSet5 = available_locales.get(availableULocales4[i4]);
            if (bitSet5 == null) {
                bitSet5 = new BitSet(6);
                available_locales.put(availableLocales[i4], bitSet5);
            }
            bitSet5.set(4);
        }
        for (ULocale uLocale2 : BreakIterator.getAvailableULocales()) {
            available_locales.get(uLocale2).set(5);
        }
        language_territory_hack_map = new HashMap();
        language_territory_hack = new String[][]{new String[]{"af", "ZA"}, new String[]{"am", "ET"}, new String[]{"ar", "SA"}, new String[]{"as", "IN"}, new String[]{"ay", "PE"}, new String[]{"az", "AZ"}, new String[]{"bal", "PK"}, new String[]{"be", "BY"}, new String[]{"bg", "BG"}, new String[]{"bn", "IN"}, new String[]{"bs", "BA"}, new String[]{"ca", "ES"}, new String[]{"ch", "MP"}, new String[]{"cpe", "SL"}, new String[]{"cs", "CZ"}, new String[]{"cy", "GB"}, new String[]{"da", "DK"}, new String[]{"de", "DE"}, new String[]{"dv", "MV"}, new String[]{"dz", "BT"}, new String[]{"el", "GR"}, new String[]{"en", "US"}, new String[]{"es", "ES"}, new String[]{"et", "EE"}, new String[]{"eu", "ES"}, new String[]{"fa", "IR"}, new String[]{"fi", "FI"}, new String[]{"fil", "PH"}, new String[]{"fj", "FJ"}, new String[]{"fo", "FO"}, new String[]{"fr", "FR"}, new String[]{"ga", "IE"}, new String[]{"gd", "GB"}, new String[]{"gl", "ES"}, new String[]{"gn", "PY"}, new String[]{"gu", "IN"}, new String[]{"gv", "GB"}, new String[]{"ha", "NG"}, new String[]{"he", "IL"}, new String[]{"hi", "IN"}, new String[]{"ho", "PG"}, new String[]{"hr", "HR"}, new String[]{"ht", "HT"}, new String[]{"hu", "HU"}, new String[]{"hy", "AM"}, new String[]{"id", "ID"}, new String[]{"is", "IS"}, new String[]{"it", "IT"}, new String[]{"ja", "JP"}, new String[]{"ka", "GE"}, new String[]{"kk", "KZ"}, new String[]{"kl", "GL"}, new String[]{"km", "KH"}, new String[]{"kn", "IN"}, new String[]{"ko", "KR"}, new String[]{"kok", "IN"}, new String[]{"ks", "IN"}, new String[]{"ku", "TR"}, new String[]{"ky", "KG"}, new String[]{"la", "VA"}, new String[]{"lb", "LU"}, new String[]{"ln", "CG"}, new String[]{"lo", "LA"}, new String[]{"lt", "LT"}, new String[]{"lv", "LV"}, new String[]{"mai", "IN"}, new String[]{"men", "GN"}, new String[]{"mg", "MG"}, new String[]{"mh", "MH"}, new String[]{"mk", "MK"}, new String[]{"ml", "IN"}, new String[]{"mn", "MN"}, new String[]{"mni", "IN"}, new String[]{"mo", "MD"}, new String[]{"mr", "IN"}, new String[]{DateFormat.MINUTE_SECOND, "MY"}, new String[]{"mt", "MT"}, new String[]{"my", "MM"}, new String[]{"na", "NR"}, new String[]{"nb", "NO"}, new String[]{"nd", "ZA"}, new String[]{"ne", "NP"}, new String[]{"niu", "NU"}, new String[]{"nl", "NL"}, new String[]{"nn", "NO"}, new String[]{"no", "NO"}, new String[]{"nr", "ZA"}, new String[]{"nso", "ZA"}, new String[]{"ny", "MW"}, new String[]{"om", "KE"}, new String[]{"or", "IN"}, new String[]{"pa", "IN"}, new String[]{"pau", "PW"}, new String[]{"pl", "PL"}, new String[]{"ps", "PK"}, new String[]{"pt", "BR"}, new String[]{"qu", "PE"}, new String[]{"rn", "BI"}, new String[]{"ro", "RO"}, new String[]{"ru", "RU"}, new String[]{"rw", "RW"}, new String[]{"sd", "IN"}, new String[]{"sg", "CF"}, new String[]{"si", "LK"}, new String[]{"sk", "SK"}, new String[]{"sl", "SI"}, new String[]{"sm", "WS"}, new String[]{"so", "DJ"}, new String[]{"sq", "CS"}, new String[]{"sr", "CS"}, new String[]{"ss", "ZA"}, new String[]{"st", "ZA"}, new String[]{"sv", "SE"}, new String[]{"sw", "KE"}, new String[]{"ta", "IN"}, new String[]{"te", "IN"}, new String[]{"tem", "SL"}, new String[]{"tet", "TL"}, new String[]{"th", "TH"}, new String[]{"ti", "ET"}, new String[]{"tg", "TJ"}, new String[]{"tk", "TM"}, new String[]{"tkl", "TK"}, new String[]{"tvl", "TV"}, new String[]{"tl", "PH"}, new String[]{"tn", "ZA"}, new String[]{"to", "TO"}, new String[]{"tpi", "PG"}, new String[]{"tr", "TR"}, new String[]{"ts", "ZA"}, new String[]{"uk", "UA"}, new String[]{"ur", "IN"}, new String[]{"uz", "UZ"}, new String[]{"ve", "ZA"}, new String[]{"vi", "VN"}, new String[]{"wo", "SN"}, new String[]{"xh", "ZA"}, new String[]{"zh", "CN"}, new String[]{"zh_Hant", "TW"}, new String[]{"zu", "ZA"}, new String[]{"aa", "ET"}, new String[]{"byn", "ER"}, new String[]{"eo", "DE"}, new String[]{"gez", "ET"}, new String[]{"haw", "US"}, new String[]{"iu", "CA"}, new String[]{"kw", "GB"}, new String[]{"sa", "IN"}, new String[]{"sh", "HR"}, new String[]{"sid", "ET"}, new String[]{"syr", "SY"}, new String[]{"tig", "ER"}, new String[]{"tt", "RU"}, new String[]{"wal", "ET"}};
        for (int i5 = 0; i5 < language_territory_hack.length; i5++) {
            language_territory_hack_map.put(language_territory_hack[i5][0], language_territory_hack[i5][1]);
        }
        territory_tzid_hack_map = new HashMap();
        territory_tzid_hack = new String[][]{new String[]{"AQ", "Antarctica/McMurdo"}, new String[]{"AR", "America/Buenos_Aires"}, new String[]{"AU", "Australia/Sydney"}, new String[]{"BR", "America/Sao_Paulo"}, new String[]{"CA", "America/Toronto"}, new String[]{"CD", "Africa/Kinshasa"}, new String[]{"CL", "America/Santiago"}, new String[]{"CN", "Asia/Shanghai"}, new String[]{"EC", "America/Guayaquil"}, new String[]{"ES", "Europe/Madrid"}, new String[]{"GB", "Europe/London"}, new String[]{"GL", "America/Godthab"}, new String[]{"ID", "Asia/Jakarta"}, new String[]{"ML", "Africa/Bamako"}, new String[]{"MX", "America/Mexico_City"}, new String[]{"MY", "Asia/Kuala_Lumpur"}, new String[]{"NZ", "Pacific/Auckland"}, new String[]{"PT", "Europe/Lisbon"}, new String[]{"RU", "Europe/Moscow"}, new String[]{"UA", "Europe/Kiev"}, new String[]{"US", "America/New_York"}, new String[]{"UZ", "Asia/Tashkent"}, new String[]{"PF", "Pacific/Tahiti"}, new String[]{"FM", "Pacific/Kosrae"}, new String[]{"KI", "Pacific/Tarawa"}, new String[]{"KZ", "Asia/Almaty"}, new String[]{"MH", "Pacific/Majuro"}, new String[]{"MN", "Asia/Ulaanbaatar"}, new String[]{"SJ", "Arctic/Longyearbyen"}, new String[]{"UM", "Pacific/Midway"}};
        for (int i6 = 0; i6 < territory_tzid_hack.length; i6++) {
            territory_tzid_hack_map.put(territory_tzid_hack[i6][0], territory_tzid_hack[i6][1]);
        }
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    @Override
    public GlobalizationPreferences freeze() {
        this.frozen = true;
        return this;
    }

    @Override
    public GlobalizationPreferences cloneAsThawed() {
        try {
            GlobalizationPreferences globalizationPreferences = (GlobalizationPreferences) clone();
            globalizationPreferences.frozen = false;
            return globalizationPreferences;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
