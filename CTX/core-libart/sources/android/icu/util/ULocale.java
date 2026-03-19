package android.icu.util;

import android.icu.impl.CacheBase;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.ICUResourceTableAccess;
import android.icu.impl.LocaleIDParser;
import android.icu.impl.LocaleIDs;
import android.icu.impl.LocaleUtility;
import android.icu.impl.SoftCache;
import android.icu.impl.locale.AsciiUtil;
import android.icu.impl.locale.BaseLocale;
import android.icu.impl.locale.Extension;
import android.icu.impl.locale.InternalLocaleBuilder;
import android.icu.impl.locale.KeyTypeData;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.locale.LocaleExtensions;
import android.icu.impl.locale.LocaleSyntaxException;
import android.icu.impl.locale.ParseStatus;
import android.icu.impl.locale.UnicodeLocaleExtension;
import android.icu.lang.UScript;
import android.icu.text.DateFormat;
import android.icu.text.LocaleDisplayNames;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ULocale implements Serializable, Comparable<ULocale> {
    public static Type ACTUAL_LOCALE = null;
    private static final String EMPTY_STRING = "";
    private static final String LANG_DIR_STRING = "root-en-es-pt-zh-ja-ko-de-fr-it-ar+he+fa+ru-nl-pl-th-tr-";
    private static final String LOCALE_ATTRIBUTE_KEY = "attribute";
    public static final char PRIVATE_USE_EXTENSION = 'x';
    private static final String UNDEFINED_LANGUAGE = "und";
    private static final String UNDEFINED_REGION = "ZZ";
    private static final String UNDEFINED_SCRIPT = "Zzzz";
    private static final char UNDERSCORE = '_';
    public static final char UNICODE_LOCALE_EXTENSION = 'u';
    public static Type VALID_LOCALE = null;
    private static ULocale defaultULocale = null;
    private static final long serialVersionUID = 3715177670352309217L;
    private volatile transient BaseLocale baseLocale;
    private volatile transient LocaleExtensions extensions;
    private volatile transient Locale locale;
    private String localeID;
    private static CacheBase<String, String, Void> nameCache = new SoftCache<String, String, Void>() {
        @Override
        protected String createInstance(String str, Void r2) {
            return new LocaleIDParser(str).getName();
        }
    };
    public static final ULocale ENGLISH = new ULocale("en", Locale.ENGLISH);
    public static final ULocale FRENCH = new ULocale("fr", Locale.FRENCH);
    public static final ULocale GERMAN = new ULocale("de", Locale.GERMAN);
    public static final ULocale ITALIAN = new ULocale("it", Locale.ITALIAN);
    public static final ULocale JAPANESE = new ULocale("ja", Locale.JAPANESE);
    public static final ULocale KOREAN = new ULocale("ko", Locale.KOREAN);
    public static final ULocale CHINESE = new ULocale("zh", Locale.CHINESE);
    public static final ULocale SIMPLIFIED_CHINESE = new ULocale("zh_Hans");
    public static final ULocale TRADITIONAL_CHINESE = new ULocale("zh_Hant");
    public static final ULocale FRANCE = new ULocale("fr_FR", Locale.FRANCE);
    public static final ULocale GERMANY = new ULocale("de_DE", Locale.GERMANY);
    public static final ULocale ITALY = new ULocale("it_IT", Locale.ITALY);
    public static final ULocale JAPAN = new ULocale("ja_JP", Locale.JAPAN);
    public static final ULocale KOREA = new ULocale("ko_KR", Locale.KOREA);
    public static final ULocale CHINA = new ULocale("zh_Hans_CN");
    public static final ULocale PRC = CHINA;
    public static final ULocale TAIWAN = new ULocale("zh_Hant_TW");
    public static final ULocale UK = new ULocale("en_GB", Locale.UK);
    public static final ULocale US = new ULocale("en_US", Locale.US);
    public static final ULocale CANADA = new ULocale("en_CA", Locale.CANADA);
    public static final ULocale CANADA_FRENCH = new ULocale("fr_CA", Locale.CANADA_FRENCH);
    private static final Locale EMPTY_LOCALE = new Locale("", "");
    public static final ULocale ROOT = new ULocale("", EMPTY_LOCALE);
    private static final SoftCache<Locale, ULocale, Void> CACHE = new SoftCache<Locale, ULocale, Void>() {
        @Override
        protected ULocale createInstance(Locale locale, Void r2) {
            return JDKLocaleHelper.toULocale(locale);
        }
    };
    private static String[][] CANONICALIZE_MAP = {new String[]{"C", "en_US_POSIX", null, null}, new String[]{"art_LOJBAN", "jbo", null, null}, new String[]{"az_AZ_CYRL", "az_Cyrl_AZ", null, null}, new String[]{"az_AZ_LATN", "az_Latn_AZ", null, null}, new String[]{"ca_ES_PREEURO", "ca_ES", "currency", "ESP"}, new String[]{"cel_GAULISH", "cel__GAULISH", null, null}, new String[]{"de_1901", "de__1901", null, null}, new String[]{"de_1906", "de__1906", null, null}, new String[]{"de__PHONEBOOK", "de", "collation", "phonebook"}, new String[]{"de_AT_PREEURO", "de_AT", "currency", "ATS"}, new String[]{"de_DE_PREEURO", "de_DE", "currency", "DEM"}, new String[]{"de_LU_PREEURO", "de_LU", "currency", "EUR"}, new String[]{"el_GR_PREEURO", "el_GR", "currency", "GRD"}, new String[]{"en_BOONT", "en__BOONT", null, null}, new String[]{"en_SCOUSE", "en__SCOUSE", null, null}, new String[]{"en_BE_PREEURO", "en_BE", "currency", "BEF"}, new String[]{"en_IE_PREEURO", "en_IE", "currency", "IEP"}, new String[]{"es__TRADITIONAL", "es", "collation", "traditional"}, new String[]{"es_ES_PREEURO", "es_ES", "currency", "ESP"}, new String[]{"eu_ES_PREEURO", "eu_ES", "currency", "ESP"}, new String[]{"fi_FI_PREEURO", "fi_FI", "currency", "FIM"}, new String[]{"fr_BE_PREEURO", "fr_BE", "currency", "BEF"}, new String[]{"fr_FR_PREEURO", "fr_FR", "currency", "FRF"}, new String[]{"fr_LU_PREEURO", "fr_LU", "currency", "LUF"}, new String[]{"ga_IE_PREEURO", "ga_IE", "currency", "IEP"}, new String[]{"gl_ES_PREEURO", "gl_ES", "currency", "ESP"}, new String[]{"hi__DIRECT", "hi", "collation", "direct"}, new String[]{"it_IT_PREEURO", "it_IT", "currency", "ITL"}, new String[]{"ja_JP_TRADITIONAL", "ja_JP", "calendar", "japanese"}, new String[]{"nl_BE_PREEURO", "nl_BE", "currency", "BEF"}, new String[]{"nl_NL_PREEURO", "nl_NL", "currency", "NLG"}, new String[]{"pt_PT_PREEURO", "pt_PT", "currency", "PTE"}, new String[]{"sl_ROZAJ", "sl__ROZAJ", null, null}, new String[]{"sr_SP_CYRL", "sr_Cyrl_RS", null, null}, new String[]{"sr_SP_LATN", "sr_Latn_RS", null, null}, new String[]{"sr_YU_CYRILLIC", "sr_Cyrl_RS", null, null}, new String[]{"th_TH_TRADITIONAL", "th_TH", "calendar", "buddhist"}, new String[]{"uz_UZ_CYRILLIC", "uz_Cyrl_UZ", null, null}, new String[]{"uz_UZ_CYRL", "uz_Cyrl_UZ", null, null}, new String[]{"uz_UZ_LATN", "uz_Latn_UZ", null, null}, new String[]{"zh_CHS", "zh_Hans", null, null}, new String[]{"zh_CHT", "zh_Hant", null, null}, new String[]{"zh_GAN", "zh__GAN", null, null}, new String[]{"zh_GUOYU", "zh", null, null}, new String[]{"zh_HAKKA", "zh__HAKKA", null, null}, new String[]{"zh_MIN", "zh__MIN", null, null}, new String[]{"zh_MIN_NAN", "zh__MINNAN", null, null}, new String[]{"zh_WUU", "zh__WUU", null, null}, new String[]{"zh_XIANG", "zh__XIANG", null, null}, new String[]{"zh_YUE", "zh__YUE", null, null}};
    private static String[][] variantsToKeywords = {new String[]{"EURO", "currency", "EUR"}, new String[]{"PINYIN", "collation", "pinyin"}, new String[]{"STROKE", "collation", "stroke"}};
    private static Locale defaultLocale = Locale.getDefault();
    private static Locale[] defaultCategoryLocales = new Locale[Category.values().length];
    private static ULocale[] defaultCategoryULocales = new ULocale[Category.values().length];

    public enum Category {
        DISPLAY,
        FORMAT
    }

    @Deprecated
    public enum Minimize {
        FAVOR_SCRIPT,
        FAVOR_REGION
    }

    static {
        String systemProperty;
        int i = 0;
        defaultULocale = forLocale(defaultLocale);
        if (JDKLocaleHelper.hasLocaleCategories()) {
            Category[] categoryArrValues = Category.values();
            int length = categoryArrValues.length;
            while (i < length) {
                Category category = categoryArrValues[i];
                int iOrdinal = category.ordinal();
                defaultCategoryLocales[iOrdinal] = JDKLocaleHelper.getDefault(category);
                defaultCategoryULocales[iOrdinal] = forLocale(defaultCategoryLocales[iOrdinal]);
                i++;
            }
        } else {
            if (JDKLocaleHelper.isOriginalDefaultLocale(defaultLocale) && (systemProperty = JDKLocaleHelper.getSystemProperty("user.script")) != null && LanguageTag.isScript(systemProperty)) {
                BaseLocale baseLocaleBase = defaultULocale.base();
                defaultULocale = getInstance(BaseLocale.getInstance(baseLocaleBase.getLanguage(), systemProperty, baseLocaleBase.getRegion(), baseLocaleBase.getVariant()), defaultULocale.extensions());
            }
            Category[] categoryArrValues2 = Category.values();
            int length2 = categoryArrValues2.length;
            while (i < length2) {
                int iOrdinal2 = categoryArrValues2[i].ordinal();
                defaultCategoryLocales[iOrdinal2] = defaultLocale;
                defaultCategoryULocales[iOrdinal2] = defaultULocale;
                i++;
            }
        }
        ACTUAL_LOCALE = new Type();
        VALID_LOCALE = new Type();
    }

    private ULocale(String str, Locale locale) {
        this.localeID = str;
        this.locale = locale;
    }

    private ULocale(Locale locale) {
        this.localeID = getName(forLocale(locale).toString());
        this.locale = locale;
    }

    public static ULocale forLocale(Locale locale) {
        if (locale == null) {
            return null;
        }
        return CACHE.getInstance(locale, null);
    }

    public ULocale(String str) {
        this.localeID = getName(str);
    }

    public ULocale(String str, String str2) {
        this(str, str2, (String) null);
    }

    public ULocale(String str, String str2, String str3) {
        this.localeID = getName(lscvToID(str, str2, str3, ""));
    }

    public static ULocale createCanonical(String str) {
        return new ULocale(canonicalize(str), (Locale) null);
    }

    private static String lscvToID(String str, String str2, String str3, String str4) {
        StringBuilder sb = new StringBuilder();
        if (str != null && str.length() > 0) {
            sb.append(str);
        }
        if (str2 != null && str2.length() > 0) {
            sb.append(UNDERSCORE);
            sb.append(str2);
        }
        if (str3 != null && str3.length() > 0) {
            sb.append(UNDERSCORE);
            sb.append(str3);
        }
        if (str4 != null && str4.length() > 0) {
            if (str3 == null || str3.length() == 0) {
                sb.append(UNDERSCORE);
            }
            sb.append(UNDERSCORE);
            sb.append(str4);
        }
        return sb.toString();
    }

    public Locale toLocale() {
        if (this.locale == null) {
            this.locale = JDKLocaleHelper.toLocale(this);
        }
        return this.locale;
    }

    public static ULocale getDefault() {
        synchronized (ULocale.class) {
            if (defaultULocale == null) {
                return ROOT;
            }
            Locale locale = Locale.getDefault();
            if (!defaultLocale.equals(locale)) {
                defaultLocale = locale;
                defaultULocale = forLocale(locale);
                if (!JDKLocaleHelper.hasLocaleCategories()) {
                    for (Category category : Category.values()) {
                        int iOrdinal = category.ordinal();
                        defaultCategoryLocales[iOrdinal] = locale;
                        defaultCategoryULocales[iOrdinal] = forLocale(locale);
                    }
                }
            }
            return defaultULocale;
        }
    }

    public static synchronized void setDefault(ULocale uLocale) {
        defaultLocale = uLocale.toLocale();
        Locale.setDefault(defaultLocale);
        defaultULocale = uLocale;
        for (Category category : Category.values()) {
            setDefault(category, uLocale);
        }
    }

    public static ULocale getDefault(Category category) {
        synchronized (ULocale.class) {
            int iOrdinal = category.ordinal();
            if (defaultCategoryULocales[iOrdinal] == null) {
                return ROOT;
            }
            if (JDKLocaleHelper.hasLocaleCategories()) {
                Locale locale = JDKLocaleHelper.getDefault(category);
                if (!defaultCategoryLocales[iOrdinal].equals(locale)) {
                    defaultCategoryLocales[iOrdinal] = locale;
                    defaultCategoryULocales[iOrdinal] = forLocale(locale);
                }
            } else {
                Locale locale2 = Locale.getDefault();
                if (!defaultLocale.equals(locale2)) {
                    defaultLocale = locale2;
                    defaultULocale = forLocale(locale2);
                    for (Category category2 : Category.values()) {
                        int iOrdinal2 = category2.ordinal();
                        defaultCategoryLocales[iOrdinal2] = locale2;
                        defaultCategoryULocales[iOrdinal2] = forLocale(locale2);
                    }
                }
            }
            return defaultCategoryULocales[iOrdinal];
        }
    }

    public static synchronized void setDefault(Category category, ULocale uLocale) {
        Locale locale = uLocale.toLocale();
        int iOrdinal = category.ordinal();
        defaultCategoryULocales[iOrdinal] = uLocale;
        defaultCategoryLocales[iOrdinal] = locale;
        JDKLocaleHelper.setDefault(category, locale);
    }

    public Object clone() {
        return this;
    }

    public int hashCode() {
        return this.localeID.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ULocale) {
            return this.localeID.equals(((ULocale) obj).localeID);
        }
        return false;
    }

    @Override
    public int compareTo(ULocale uLocale) {
        if (this == uLocale) {
            return 0;
        }
        int iCompareTo = getLanguage().compareTo(uLocale.getLanguage());
        if (iCompareTo == 0 && (iCompareTo = getScript().compareTo(uLocale.getScript())) == 0 && (iCompareTo = getCountry().compareTo(uLocale.getCountry())) == 0 && (iCompareTo = getVariant().compareTo(uLocale.getVariant())) == 0) {
            Iterator<String> keywords = getKeywords();
            Iterator<String> keywords2 = uLocale.getKeywords();
            if (keywords == null) {
                iCompareTo = keywords2 == null ? 0 : -1;
            } else if (keywords2 == null) {
                iCompareTo = 1;
            } else {
                while (true) {
                    if (iCompareTo != 0 || !keywords.hasNext()) {
                        break;
                    }
                    if (!keywords2.hasNext()) {
                        iCompareTo = 1;
                        break;
                    }
                    String next = keywords.next();
                    String next2 = keywords2.next();
                    int iCompareTo2 = next.compareTo(next2);
                    if (iCompareTo2 == 0) {
                        String keywordValue = getKeywordValue(next);
                        String keywordValue2 = uLocale.getKeywordValue(next2);
                        iCompareTo = keywordValue == null ? keywordValue2 == null ? 0 : -1 : keywordValue2 == null ? 1 : keywordValue.compareTo(keywordValue2);
                    } else {
                        iCompareTo = iCompareTo2;
                    }
                }
                if (iCompareTo == 0 && keywords2.hasNext()) {
                }
            }
        }
        if (iCompareTo < 0) {
            return -1;
        }
        return iCompareTo > 0 ? 1 : 0;
    }

    public static ULocale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    public static String[] getISOCountries() {
        return LocaleIDs.getISOCountries();
    }

    public static String[] getISOLanguages() {
        return LocaleIDs.getISOLanguages();
    }

    public String getLanguage() {
        return base().getLanguage();
    }

    public static String getLanguage(String str) {
        return new LocaleIDParser(str).getLanguage();
    }

    public String getScript() {
        return base().getScript();
    }

    public static String getScript(String str) {
        return new LocaleIDParser(str).getScript();
    }

    public String getCountry() {
        return base().getRegion();
    }

    public static String getCountry(String str) {
        return new LocaleIDParser(str).getCountry();
    }

    @Deprecated
    public static String getRegionForSupplementalData(ULocale uLocale, boolean z) {
        String keywordValue = uLocale.getKeywordValue("rg");
        if (keywordValue != null && keywordValue.length() == 6) {
            String upperString = AsciiUtil.toUpperString(keywordValue);
            if (upperString.endsWith(DateFormat.ABBR_UTC_TZ)) {
                return upperString.substring(0, 2);
            }
        }
        String country = uLocale.getCountry();
        if (country.length() == 0 && z) {
            return addLikelySubtags(uLocale).getCountry();
        }
        return country;
    }

    public String getVariant() {
        return base().getVariant();
    }

    public static String getVariant(String str) {
        return new LocaleIDParser(str).getVariant();
    }

    public static String getFallback(String str) {
        return getFallbackString(getName(str));
    }

    public ULocale getFallback() {
        if (this.localeID.length() == 0 || this.localeID.charAt(0) == '@') {
            return null;
        }
        return new ULocale(getFallbackString(this.localeID), (Locale) null);
    }

    private static String getFallbackString(String str) {
        int iIndexOf = str.indexOf(64);
        if (iIndexOf == -1) {
            iIndexOf = str.length();
        }
        int iLastIndexOf = str.lastIndexOf(95, iIndexOf);
        if (iLastIndexOf != -1) {
            while (iLastIndexOf > 0 && str.charAt(iLastIndexOf - 1) == '_') {
                iLastIndexOf--;
            }
        } else {
            iLastIndexOf = 0;
        }
        return str.substring(0, iLastIndexOf) + str.substring(iIndexOf);
    }

    public String getBaseName() {
        return getBaseName(this.localeID);
    }

    public static String getBaseName(String str) {
        if (str.indexOf(64) == -1) {
            return str;
        }
        return new LocaleIDParser(str).getBaseName();
    }

    public String getName() {
        return this.localeID;
    }

    private static int getShortestSubtagLength(String str) {
        int length = str.length();
        int i = length;
        int i2 = 0;
        boolean z = true;
        for (int i3 = 0; i3 < length; i3++) {
            if (str.charAt(i3) != '_' && str.charAt(i3) != '-') {
                if (z) {
                    i2 = 0;
                    z = false;
                }
                i2++;
            } else {
                if (i2 != 0 && i2 < i) {
                    i = i2;
                }
                z = true;
            }
        }
        return i;
    }

    public static String getName(String str) {
        if (str != null && !str.contains("@") && getShortestSubtagLength(str) == 1) {
            String name = forLanguageTag(str).getName();
            if (name.length() != 0) {
                str = name;
            }
        }
        return nameCache.getInstance(str, null);
    }

    public String toString() {
        return this.localeID;
    }

    public Iterator<String> getKeywords() {
        return getKeywords(this.localeID);
    }

    public static Iterator<String> getKeywords(String str) {
        return new LocaleIDParser(str).getKeywords();
    }

    public String getKeywordValue(String str) {
        return getKeywordValue(this.localeID, str);
    }

    public static String getKeywordValue(String str, String str2) {
        return new LocaleIDParser(str).getKeywordValue(str2);
    }

    public static String canonicalize(String str) {
        boolean z;
        boolean z2 = true;
        LocaleIDParser localeIDParser = new LocaleIDParser(str, true);
        String baseName = localeIDParser.getBaseName();
        if (str.equals("")) {
            return "";
        }
        int i = 0;
        while (true) {
            if (i < variantsToKeywords.length) {
                String[] strArr = variantsToKeywords[i];
                int iLastIndexOf = baseName.lastIndexOf(BaseLocale.SEP + strArr[0]);
                if (iLastIndexOf <= -1) {
                    i++;
                } else {
                    baseName = baseName.substring(0, iLastIndexOf);
                    if (baseName.endsWith(BaseLocale.SEP)) {
                        baseName = baseName.substring(0, iLastIndexOf - 1);
                    }
                    localeIDParser.setBaseName(baseName);
                    localeIDParser.defaultKeywordValue(strArr[1], strArr[2]);
                    z = true;
                }
            } else {
                z = false;
                break;
            }
        }
        int i2 = 0;
        while (true) {
            if (i2 < CANONICALIZE_MAP.length) {
                if (!CANONICALIZE_MAP[i2][0].equals(baseName)) {
                    i2++;
                } else {
                    String[] strArr2 = CANONICALIZE_MAP[i2];
                    localeIDParser.setBaseName(strArr2[1]);
                    if (strArr2[2] != null) {
                        localeIDParser.defaultKeywordValue(strArr2[2], strArr2[3]);
                    }
                }
            } else {
                z2 = z;
                break;
            }
        }
        if (!z2 && localeIDParser.getLanguage().equals("nb") && localeIDParser.getVariant().equals("NY")) {
            localeIDParser.setBaseName(lscvToID("nn", localeIDParser.getScript(), localeIDParser.getCountry(), null));
        }
        return localeIDParser.getName();
    }

    public ULocale setKeywordValue(String str, String str2) {
        return new ULocale(setKeywordValue(this.localeID, str, str2), (Locale) null);
    }

    public static String setKeywordValue(String str, String str2, String str3) {
        LocaleIDParser localeIDParser = new LocaleIDParser(str);
        localeIDParser.setKeywordValue(str2, str3);
        return localeIDParser.getName();
    }

    public String getISO3Language() {
        return getISO3Language(this.localeID);
    }

    public static String getISO3Language(String str) {
        return LocaleIDs.getISO3Language(getLanguage(str));
    }

    public String getISO3Country() {
        return getISO3Country(this.localeID);
    }

    public static String getISO3Country(String str) {
        return LocaleIDs.getISO3Country(getCountry(str));
    }

    public boolean isRightToLeft() {
        String script = getScript();
        if (script.length() == 0) {
            String language = getLanguage();
            if (language.length() == 0) {
                return false;
            }
            int iIndexOf = LANG_DIR_STRING.indexOf(language);
            if (iIndexOf >= 0) {
                char cCharAt = LANG_DIR_STRING.charAt(iIndexOf + language.length());
                if (cCharAt == '+') {
                    return true;
                }
                if (cCharAt == '-') {
                    return false;
                }
            }
            script = addLikelySubtags(this).getScript();
            if (script.length() == 0) {
                return false;
            }
        }
        return UScript.isRightToLeft(UScript.getCodeFromName(script));
    }

    public String getDisplayLanguage() {
        return getDisplayLanguageInternal(this, getDefault(Category.DISPLAY), false);
    }

    public String getDisplayLanguage(ULocale uLocale) {
        return getDisplayLanguageInternal(this, uLocale, false);
    }

    public static String getDisplayLanguage(String str, String str2) {
        return getDisplayLanguageInternal(new ULocale(str), new ULocale(str2), false);
    }

    public static String getDisplayLanguage(String str, ULocale uLocale) {
        return getDisplayLanguageInternal(new ULocale(str), uLocale, false);
    }

    public String getDisplayLanguageWithDialect() {
        return getDisplayLanguageInternal(this, getDefault(Category.DISPLAY), true);
    }

    public String getDisplayLanguageWithDialect(ULocale uLocale) {
        return getDisplayLanguageInternal(this, uLocale, true);
    }

    public static String getDisplayLanguageWithDialect(String str, String str2) {
        return getDisplayLanguageInternal(new ULocale(str), new ULocale(str2), true);
    }

    public static String getDisplayLanguageWithDialect(String str, ULocale uLocale) {
        return getDisplayLanguageInternal(new ULocale(str), uLocale, true);
    }

    private static String getDisplayLanguageInternal(ULocale uLocale, ULocale uLocale2, boolean z) {
        return LocaleDisplayNames.getInstance(uLocale2).languageDisplayName(z ? uLocale.getBaseName() : uLocale.getLanguage());
    }

    public String getDisplayScript() {
        return getDisplayScriptInternal(this, getDefault(Category.DISPLAY));
    }

    @Deprecated
    public String getDisplayScriptInContext() {
        return getDisplayScriptInContextInternal(this, getDefault(Category.DISPLAY));
    }

    public String getDisplayScript(ULocale uLocale) {
        return getDisplayScriptInternal(this, uLocale);
    }

    @Deprecated
    public String getDisplayScriptInContext(ULocale uLocale) {
        return getDisplayScriptInContextInternal(this, uLocale);
    }

    public static String getDisplayScript(String str, String str2) {
        return getDisplayScriptInternal(new ULocale(str), new ULocale(str2));
    }

    @Deprecated
    public static String getDisplayScriptInContext(String str, String str2) {
        return getDisplayScriptInContextInternal(new ULocale(str), new ULocale(str2));
    }

    public static String getDisplayScript(String str, ULocale uLocale) {
        return getDisplayScriptInternal(new ULocale(str), uLocale);
    }

    @Deprecated
    public static String getDisplayScriptInContext(String str, ULocale uLocale) {
        return getDisplayScriptInContextInternal(new ULocale(str), uLocale);
    }

    private static String getDisplayScriptInternal(ULocale uLocale, ULocale uLocale2) {
        return LocaleDisplayNames.getInstance(uLocale2).scriptDisplayName(uLocale.getScript());
    }

    private static String getDisplayScriptInContextInternal(ULocale uLocale, ULocale uLocale2) {
        return LocaleDisplayNames.getInstance(uLocale2).scriptDisplayNameInContext(uLocale.getScript());
    }

    public String getDisplayCountry() {
        return getDisplayCountryInternal(this, getDefault(Category.DISPLAY));
    }

    public String getDisplayCountry(ULocale uLocale) {
        return getDisplayCountryInternal(this, uLocale);
    }

    public static String getDisplayCountry(String str, String str2) {
        return getDisplayCountryInternal(new ULocale(str), new ULocale(str2));
    }

    public static String getDisplayCountry(String str, ULocale uLocale) {
        return getDisplayCountryInternal(new ULocale(str), uLocale);
    }

    private static String getDisplayCountryInternal(ULocale uLocale, ULocale uLocale2) {
        return LocaleDisplayNames.getInstance(uLocale2).regionDisplayName(uLocale.getCountry());
    }

    public String getDisplayVariant() {
        return getDisplayVariantInternal(this, getDefault(Category.DISPLAY));
    }

    public String getDisplayVariant(ULocale uLocale) {
        return getDisplayVariantInternal(this, uLocale);
    }

    public static String getDisplayVariant(String str, String str2) {
        return getDisplayVariantInternal(new ULocale(str), new ULocale(str2));
    }

    public static String getDisplayVariant(String str, ULocale uLocale) {
        return getDisplayVariantInternal(new ULocale(str), uLocale);
    }

    private static String getDisplayVariantInternal(ULocale uLocale, ULocale uLocale2) {
        return LocaleDisplayNames.getInstance(uLocale2).variantDisplayName(uLocale.getVariant());
    }

    public static String getDisplayKeyword(String str) {
        return getDisplayKeywordInternal(str, getDefault(Category.DISPLAY));
    }

    public static String getDisplayKeyword(String str, String str2) {
        return getDisplayKeywordInternal(str, new ULocale(str2));
    }

    public static String getDisplayKeyword(String str, ULocale uLocale) {
        return getDisplayKeywordInternal(str, uLocale);
    }

    private static String getDisplayKeywordInternal(String str, ULocale uLocale) {
        return LocaleDisplayNames.getInstance(uLocale).keyDisplayName(str);
    }

    public String getDisplayKeywordValue(String str) {
        return getDisplayKeywordValueInternal(this, str, getDefault(Category.DISPLAY));
    }

    public String getDisplayKeywordValue(String str, ULocale uLocale) {
        return getDisplayKeywordValueInternal(this, str, uLocale);
    }

    public static String getDisplayKeywordValue(String str, String str2, String str3) {
        return getDisplayKeywordValueInternal(new ULocale(str), str2, new ULocale(str3));
    }

    public static String getDisplayKeywordValue(String str, String str2, ULocale uLocale) {
        return getDisplayKeywordValueInternal(new ULocale(str), str2, uLocale);
    }

    private static String getDisplayKeywordValueInternal(ULocale uLocale, String str, ULocale uLocale2) {
        String lowerString = AsciiUtil.toLowerString(str.trim());
        return LocaleDisplayNames.getInstance(uLocale2).keyValueDisplayName(lowerString, uLocale.getKeywordValue(lowerString));
    }

    public String getDisplayName() {
        return getDisplayNameInternal(this, getDefault(Category.DISPLAY));
    }

    public String getDisplayName(ULocale uLocale) {
        return getDisplayNameInternal(this, uLocale);
    }

    public static String getDisplayName(String str, String str2) {
        return getDisplayNameInternal(new ULocale(str), new ULocale(str2));
    }

    public static String getDisplayName(String str, ULocale uLocale) {
        return getDisplayNameInternal(new ULocale(str), uLocale);
    }

    private static String getDisplayNameInternal(ULocale uLocale, ULocale uLocale2) {
        return LocaleDisplayNames.getInstance(uLocale2).localeDisplayName(uLocale);
    }

    public String getDisplayNameWithDialect() {
        return getDisplayNameWithDialectInternal(this, getDefault(Category.DISPLAY));
    }

    public String getDisplayNameWithDialect(ULocale uLocale) {
        return getDisplayNameWithDialectInternal(this, uLocale);
    }

    public static String getDisplayNameWithDialect(String str, String str2) {
        return getDisplayNameWithDialectInternal(new ULocale(str), new ULocale(str2));
    }

    public static String getDisplayNameWithDialect(String str, ULocale uLocale) {
        return getDisplayNameWithDialectInternal(new ULocale(str), uLocale);
    }

    private static String getDisplayNameWithDialectInternal(ULocale uLocale, ULocale uLocale2) {
        return LocaleDisplayNames.getInstance(uLocale2, LocaleDisplayNames.DialectHandling.DIALECT_NAMES).localeDisplayName(uLocale);
    }

    public String getCharacterOrientation() {
        return ICUResourceTableAccess.getTableString(ICUData.ICU_BASE_NAME, this, "layout", "characters", "characters");
    }

    public String getLineOrientation() {
        return ICUResourceTableAccess.getTableString(ICUData.ICU_BASE_NAME, this, "layout", "lines", "lines");
    }

    public static final class Type {
        private Type() {
        }
    }

    public static ULocale acceptLanguage(String str, ULocale[] uLocaleArr, boolean[] zArr) {
        ULocale[] acceptLanguage;
        if (str == null) {
            throw new NullPointerException();
        }
        try {
            acceptLanguage = parseAcceptLanguage(str, true);
        } catch (ParseException e) {
            acceptLanguage = null;
        }
        if (acceptLanguage == null) {
            return null;
        }
        return acceptLanguage(acceptLanguage, uLocaleArr, zArr);
    }

    public static ULocale acceptLanguage(ULocale[] uLocaleArr, ULocale[] uLocaleArr2, boolean[] zArr) {
        if (zArr != null) {
            zArr[0] = true;
        }
        int i = 0;
        while (i < uLocaleArr.length) {
            ULocale uLocale = uLocaleArr[i];
            boolean[] zArr2 = zArr;
            while (true) {
                for (int i2 = 0; i2 < uLocaleArr2.length; i2++) {
                    if (uLocaleArr2[i2].equals(uLocale)) {
                        if (zArr2 != null) {
                            zArr2[0] = false;
                        }
                        return uLocaleArr2[i2];
                    }
                    if (uLocale.getScript().length() == 0 && uLocaleArr2[i2].getScript().length() > 0 && uLocaleArr2[i2].getLanguage().equals(uLocale.getLanguage()) && uLocaleArr2[i2].getCountry().equals(uLocale.getCountry()) && uLocaleArr2[i2].getVariant().equals(uLocale.getVariant()) && minimizeSubtags(uLocaleArr2[i2]).getScript().length() == 0) {
                        if (zArr2 != null) {
                            zArr2[0] = false;
                        }
                        return uLocale;
                    }
                }
                Locale localeFallback = LocaleUtility.fallback(uLocale.toLocale());
                if (localeFallback != null) {
                    uLocale = new ULocale(localeFallback);
                } else {
                    uLocale = null;
                }
                if (uLocale == null) {
                    break;
                }
                zArr2 = null;
            }
        }
        return null;
    }

    public static ULocale acceptLanguage(String str, boolean[] zArr) {
        return acceptLanguage(str, getAvailableLocales(), zArr);
    }

    public static ULocale acceptLanguage(ULocale[] uLocaleArr, boolean[] zArr) {
        return acceptLanguage(uLocaleArr, getAvailableLocales(), zArr);
    }

    static ULocale[] parseAcceptLanguage(String str, boolean z) throws ParseException {
        double d;
        TreeMap treeMap = new TreeMap();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        String str2 = str + ",";
        byte b = 0;
        int i = 0;
        boolean z2 = false;
        while (i < str2.length()) {
            char cCharAt = str2.charAt(i);
            boolean z3 = true;
            switch (b) {
                case 0:
                    if (('A' <= cCharAt && cCharAt <= 'Z') || ('a' <= cCharAt && cCharAt <= 'z')) {
                        sb.append(cCharAt);
                        b = 1;
                        z2 = false;
                    } else if (cCharAt == '*') {
                        sb.append(cCharAt);
                        b = 2;
                    } else if (cCharAt != ' ' && cCharAt != '\t') {
                        b = -1;
                    }
                    z3 = false;
                    break;
                case 1:
                    if (('A' > cCharAt || cCharAt > 'Z') && ('a' > cCharAt || cCharAt > 'z')) {
                        if (cCharAt == '-') {
                            sb.append(cCharAt);
                        } else {
                            if (cCharAt == '_') {
                                if (z) {
                                    sb.append(cCharAt);
                                }
                            } else if ('0' > cCharAt || cCharAt > '9') {
                                if (cCharAt != ',') {
                                    if (cCharAt == ' ' || cCharAt == '\t') {
                                        b = 3;
                                    } else if (cCharAt == ';') {
                                        b = 4;
                                    }
                                }
                            } else if (z2) {
                                sb.append(cCharAt);
                            }
                            b = -1;
                        }
                        z2 = true;
                    } else {
                        sb.append(cCharAt);
                    }
                    z3 = false;
                    break;
                case 2:
                    if (cCharAt != ',') {
                        if (cCharAt == ' ' || cCharAt == '\t') {
                            b = 3;
                            z3 = false;
                        } else {
                            if (cCharAt == ';') {
                            }
                            z3 = false;
                        }
                    }
                    break;
                case 3:
                    if (cCharAt != ',') {
                        if (cCharAt != ';') {
                            if (cCharAt != ' ' && cCharAt != '\t') {
                            }
                        }
                        z3 = false;
                    }
                    break;
                case 4:
                    if (cCharAt == 'q') {
                        b = 5;
                    } else if (cCharAt != ' ' && cCharAt != '\t') {
                    }
                    z3 = false;
                    break;
                case 5:
                    if (cCharAt == '=') {
                        b = 6;
                    } else if (cCharAt != ' ' && cCharAt != '\t') {
                    }
                    z3 = false;
                    break;
                case 6:
                    if (cCharAt == '0' || cCharAt == '1') {
                        sb2.append(cCharAt);
                        b = 7;
                        z3 = false;
                    } else {
                        if (cCharAt != '.') {
                            if (cCharAt != ' ' && cCharAt != '\t') {
                            }
                            z3 = false;
                        } else if (z) {
                            sb2.append(cCharAt);
                            b = 8;
                            z3 = false;
                        }
                        b = -1;
                        z3 = false;
                    }
                    break;
                case 7:
                    if (cCharAt == '.') {
                        sb2.append(cCharAt);
                        b = 8;
                        z3 = false;
                    } else if (cCharAt != ',') {
                        if (cCharAt == ' ' || cCharAt == '\t') {
                            b = 10;
                        }
                        z3 = false;
                    }
                    break;
                case 8:
                    if ('0' <= cCharAt && cCharAt <= '9') {
                        sb2.append(cCharAt);
                        b = 9;
                    }
                    z3 = false;
                    break;
                case 9:
                    if ('0' <= cCharAt && cCharAt <= '9') {
                        sb2.append(cCharAt);
                        z3 = false;
                    } else if (cCharAt != ',') {
                        if (cCharAt == ' ' || cCharAt == '\t') {
                        }
                        z3 = false;
                    }
                    break;
                case 10:
                    if (cCharAt != ',') {
                        if (cCharAt != ' ' && cCharAt != '\t') {
                        }
                        z3 = false;
                    }
                    break;
                default:
                    z3 = false;
                    break;
            }
            if (b == -1) {
                throw new ParseException("Invalid Accept-Language", i);
            }
            if (z3) {
                double d2 = 1.0d;
                if (sb2.length() != 0) {
                    try {
                        d = Double.parseDouble(sb2.toString());
                    } catch (NumberFormatException e) {
                        d = 1.0d;
                    }
                    if (d <= 1.0d) {
                        d2 = d;
                    }
                }
                if (sb.charAt(0) != '*') {
                    treeMap.put(new Comparable<C1ULocaleAcceptLanguageQ>(d2, treeMap.size()) {
                        private double q;
                        private double serial;

                        {
                            this.q = d2;
                            this.serial = i;
                        }

                        @Override
                        public int compareTo(C1ULocaleAcceptLanguageQ c1ULocaleAcceptLanguageQ) {
                            if (this.q > c1ULocaleAcceptLanguageQ.q) {
                                return -1;
                            }
                            if (this.q < c1ULocaleAcceptLanguageQ.q) {
                                return 1;
                            }
                            if (this.serial < c1ULocaleAcceptLanguageQ.serial) {
                                return -1;
                            }
                            return this.serial > c1ULocaleAcceptLanguageQ.serial ? 1 : 0;
                        }
                    }, new ULocale(canonicalize(sb.toString())));
                }
                sb.setLength(0);
                sb2.setLength(0);
                b = 0;
            }
            i++;
        }
        if (b == 0) {
            return (ULocale[]) treeMap.values().toArray(new ULocale[treeMap.size()]);
        }
        throw new ParseException("Invalid AcceptlLanguage", i);
    }

    public static ULocale addLikelySubtags(ULocale uLocale) {
        String strSubstring;
        String[] strArr = new String[3];
        int tagString = parseTagString(uLocale.localeID, strArr);
        if (tagString < uLocale.localeID.length()) {
            strSubstring = uLocale.localeID.substring(tagString);
        } else {
            strSubstring = null;
        }
        String strCreateLikelySubtagsString = createLikelySubtagsString(strArr[0], strArr[1], strArr[2], strSubstring);
        return strCreateLikelySubtagsString == null ? uLocale : new ULocale(strCreateLikelySubtagsString);
    }

    public static ULocale minimizeSubtags(ULocale uLocale) {
        return minimizeSubtags(uLocale, Minimize.FAVOR_REGION);
    }

    @Deprecated
    public static ULocale minimizeSubtags(ULocale uLocale, Minimize minimize) {
        String strSubstring;
        String[] strArr = new String[3];
        int tagString = parseTagString(uLocale.localeID, strArr);
        String str = strArr[0];
        String str2 = strArr[1];
        String str3 = strArr[2];
        if (tagString < uLocale.localeID.length()) {
            strSubstring = uLocale.localeID.substring(tagString);
        } else {
            strSubstring = null;
        }
        String strCreateLikelySubtagsString = createLikelySubtagsString(str, str2, str3, null);
        if (isEmptyString(strCreateLikelySubtagsString)) {
            return uLocale;
        }
        if (createLikelySubtagsString(str, null, null, null).equals(strCreateLikelySubtagsString)) {
            return new ULocale(createTagString(str, null, null, strSubstring));
        }
        if (minimize == Minimize.FAVOR_REGION) {
            if (str3.length() != 0 && createLikelySubtagsString(str, null, str3, null).equals(strCreateLikelySubtagsString)) {
                return new ULocale(createTagString(str, null, str3, strSubstring));
            }
            if (str2.length() != 0 && createLikelySubtagsString(str, str2, null, null).equals(strCreateLikelySubtagsString)) {
                return new ULocale(createTagString(str, str2, null, strSubstring));
            }
        } else {
            if (str2.length() != 0 && createLikelySubtagsString(str, str2, null, null).equals(strCreateLikelySubtagsString)) {
                return new ULocale(createTagString(str, str2, null, strSubstring));
            }
            if (str3.length() != 0 && createLikelySubtagsString(str, null, str3, null).equals(strCreateLikelySubtagsString)) {
                return new ULocale(createTagString(str, null, str3, strSubstring));
            }
        }
        return uLocale;
    }

    private static boolean isEmptyString(String str) {
        return str == null || str.length() == 0;
    }

    private static void appendTag(String str, StringBuilder sb) {
        if (sb.length() != 0) {
            sb.append(UNDERSCORE);
        }
        sb.append(str);
    }

    private static String createTagString(String str, String str2, String str3, String str4, String str5) {
        LocaleIDParser localeIDParser;
        boolean z;
        StringBuilder sb = new StringBuilder();
        if (!isEmptyString(str)) {
            appendTag(str, sb);
        } else if (isEmptyString(str5)) {
            appendTag(UNDEFINED_LANGUAGE, sb);
        } else {
            localeIDParser = new LocaleIDParser(str5);
            String language = localeIDParser.getLanguage();
            if (isEmptyString(language)) {
                language = UNDEFINED_LANGUAGE;
            }
            appendTag(language, sb);
            if (isEmptyString(str2)) {
                appendTag(str2, sb);
            } else if (!isEmptyString(str5)) {
                if (localeIDParser == null) {
                    localeIDParser = new LocaleIDParser(str5);
                }
                String script = localeIDParser.getScript();
                if (!isEmptyString(script)) {
                    appendTag(script, sb);
                }
            }
            char c = 0;
            if (isEmptyString(str3)) {
                appendTag(str3, sb);
            } else {
                if (!isEmptyString(str5)) {
                    if (localeIDParser == null) {
                        localeIDParser = new LocaleIDParser(str5);
                    }
                    String country = localeIDParser.getCountry();
                    if (!isEmptyString(country)) {
                        appendTag(country, sb);
                    }
                }
                z = false;
                if (str4 != null && str4.length() > 1) {
                    if (str4.charAt(0) != '_') {
                        if (str4.charAt(1) == '_') {
                            c = 2;
                        }
                    } else {
                        c = 1;
                    }
                    if (!z) {
                        if (c == 2) {
                            sb.append(str4.substring(1));
                        } else {
                            sb.append(str4);
                        }
                    } else {
                        if (c == 1) {
                            sb.append(UNDERSCORE);
                        }
                        sb.append(str4);
                    }
                }
                return sb.toString();
            }
            z = true;
            if (str4 != null) {
                if (str4.charAt(0) != '_') {
                }
                if (!z) {
                }
            }
            return sb.toString();
        }
        localeIDParser = null;
        if (isEmptyString(str2)) {
        }
        char c2 = 0;
        if (isEmptyString(str3)) {
        }
        z = true;
        if (str4 != null) {
        }
        return sb.toString();
    }

    static String createTagString(String str, String str2, String str3, String str4) {
        return createTagString(str, str2, str3, str4, null);
    }

    private static int parseTagString(String str, String[] strArr) {
        LocaleIDParser localeIDParser = new LocaleIDParser(str);
        String language = localeIDParser.getLanguage();
        String script = localeIDParser.getScript();
        String country = localeIDParser.getCountry();
        if (isEmptyString(language)) {
            strArr[0] = UNDEFINED_LANGUAGE;
        } else {
            strArr[0] = language;
        }
        if (script.equals(UNDEFINED_SCRIPT)) {
            strArr[1] = "";
        } else {
            strArr[1] = script;
        }
        if (country.equals(UNDEFINED_REGION)) {
            strArr[2] = "";
        } else {
            strArr[2] = country;
        }
        String variant = localeIDParser.getVariant();
        if (!isEmptyString(variant)) {
            int iIndexOf = str.indexOf(variant);
            return iIndexOf > 0 ? iIndexOf - 1 : iIndexOf;
        }
        int iIndexOf2 = str.indexOf(64);
        return iIndexOf2 == -1 ? str.length() : iIndexOf2;
    }

    private static String lookupLikelySubtags(String str) {
        try {
            return UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "likelySubtags").getString(str);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static String createLikelySubtagsString(String str, String str2, String str3, String str4) {
        String strLookupLikelySubtags;
        String strLookupLikelySubtags2;
        String strLookupLikelySubtags3;
        if (!isEmptyString(str2) && !isEmptyString(str3) && (strLookupLikelySubtags3 = lookupLikelySubtags(createTagString(str, str2, str3, null))) != null) {
            return createTagString(null, null, null, str4, strLookupLikelySubtags3);
        }
        if (!isEmptyString(str2) && (strLookupLikelySubtags2 = lookupLikelySubtags(createTagString(str, str2, null, null))) != null) {
            return createTagString(null, null, str3, str4, strLookupLikelySubtags2);
        }
        if (!isEmptyString(str3) && (strLookupLikelySubtags = lookupLikelySubtags(createTagString(str, null, str3, null))) != null) {
            return createTagString(null, str2, null, str4, strLookupLikelySubtags);
        }
        String strLookupLikelySubtags4 = lookupLikelySubtags(createTagString(str, null, null, null));
        if (strLookupLikelySubtags4 != null) {
            return createTagString(null, str2, str3, str4, strLookupLikelySubtags4);
        }
        return null;
    }

    public String getExtension(char c) {
        if (!LocaleExtensions.isValidKey(c)) {
            throw new IllegalArgumentException("Invalid extension key: " + c);
        }
        return extensions().getExtensionValue(Character.valueOf(c));
    }

    public Set<Character> getExtensionKeys() {
        return extensions().getKeys();
    }

    public Set<String> getUnicodeLocaleAttributes() {
        return extensions().getUnicodeLocaleAttributes();
    }

    public String getUnicodeLocaleType(String str) {
        if (!LocaleExtensions.isValidUnicodeLocaleKey(str)) {
            throw new IllegalArgumentException("Invalid Unicode locale key: " + str);
        }
        return extensions().getUnicodeLocaleType(str);
    }

    public Set<String> getUnicodeLocaleKeys() {
        return extensions().getUnicodeLocaleKeys();
    }

    public String toLanguageTag() {
        BaseLocale baseLocaleBase = base();
        LocaleExtensions localeExtensionsExtensions = extensions();
        if (baseLocaleBase.getVariant().equalsIgnoreCase("POSIX")) {
            baseLocaleBase = BaseLocale.getInstance(baseLocaleBase.getLanguage(), baseLocaleBase.getScript(), baseLocaleBase.getRegion(), "");
            if (localeExtensionsExtensions.getUnicodeLocaleType("va") == null) {
                InternalLocaleBuilder internalLocaleBuilder = new InternalLocaleBuilder();
                try {
                    internalLocaleBuilder.setLocale(BaseLocale.ROOT, localeExtensionsExtensions);
                    internalLocaleBuilder.setUnicodeLocaleKeyword("va", "posix");
                    localeExtensionsExtensions = internalLocaleBuilder.getLocaleExtensions();
                } catch (LocaleSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        LanguageTag locale = LanguageTag.parseLocale(baseLocaleBase, localeExtensionsExtensions);
        StringBuilder sb = new StringBuilder();
        String language = locale.getLanguage();
        if (language.length() > 0) {
            sb.append(LanguageTag.canonicalizeLanguage(language));
        }
        String script = locale.getScript();
        if (script.length() > 0) {
            sb.append(LanguageTag.SEP);
            sb.append(LanguageTag.canonicalizeScript(script));
        }
        String region = locale.getRegion();
        if (region.length() > 0) {
            sb.append(LanguageTag.SEP);
            sb.append(LanguageTag.canonicalizeRegion(region));
        }
        for (String str : locale.getVariants()) {
            sb.append(LanguageTag.SEP);
            sb.append(LanguageTag.canonicalizeVariant(str));
        }
        for (String str2 : locale.getExtensions()) {
            sb.append(LanguageTag.SEP);
            sb.append(LanguageTag.canonicalizeExtension(str2));
        }
        String privateuse = locale.getPrivateuse();
        if (privateuse.length() > 0) {
            if (sb.length() > 0) {
                sb.append(LanguageTag.SEP);
            }
            sb.append(LanguageTag.PRIVATEUSE);
            sb.append(LanguageTag.SEP);
            sb.append(LanguageTag.canonicalizePrivateuse(privateuse));
        }
        return sb.toString();
    }

    public static ULocale forLanguageTag(String str) {
        LanguageTag languageTag = LanguageTag.parse(str, null);
        InternalLocaleBuilder internalLocaleBuilder = new InternalLocaleBuilder();
        internalLocaleBuilder.setLanguageTag(languageTag);
        return getInstance(internalLocaleBuilder.getBaseLocale(), internalLocaleBuilder.getLocaleExtensions());
    }

    public static String toUnicodeLocaleKey(String str) {
        String bcpKey = KeyTypeData.toBcpKey(str);
        if (bcpKey == null && UnicodeLocaleExtension.isKey(str)) {
            return AsciiUtil.toLowerString(str);
        }
        return bcpKey;
    }

    public static String toUnicodeLocaleType(String str, String str2) {
        String bcpType = KeyTypeData.toBcpType(str, str2, null, null);
        if (bcpType == null && UnicodeLocaleExtension.isType(str2)) {
            return AsciiUtil.toLowerString(str2);
        }
        return bcpType;
    }

    public static String toLegacyKey(String str) {
        String legacyKey = KeyTypeData.toLegacyKey(str);
        if (legacyKey == null && str.matches("[0-9a-zA-Z]+")) {
            return AsciiUtil.toLowerString(str);
        }
        return legacyKey;
    }

    public static String toLegacyType(String str, String str2) {
        String legacyType = KeyTypeData.toLegacyType(str, str2, null, null);
        if (legacyType == null && str2.matches("[0-9a-zA-Z]+([_/\\-][0-9a-zA-Z]+)*")) {
            return AsciiUtil.toLowerString(str2);
        }
        return legacyType;
    }

    public static final class Builder {
        private final InternalLocaleBuilder _locbld = new InternalLocaleBuilder();

        public Builder setLocale(ULocale uLocale) {
            try {
                this._locbld.setLocale(uLocale.base(), uLocale.extensions());
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setLanguageTag(String str) {
            ParseStatus parseStatus = new ParseStatus();
            LanguageTag languageTag = LanguageTag.parse(str, parseStatus);
            if (parseStatus.isError()) {
                throw new IllformedLocaleException(parseStatus.getErrorMessage(), parseStatus.getErrorIndex());
            }
            this._locbld.setLanguageTag(languageTag);
            return this;
        }

        public Builder setLanguage(String str) {
            try {
                this._locbld.setLanguage(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setScript(String str) {
            try {
                this._locbld.setScript(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setRegion(String str) {
            try {
                this._locbld.setRegion(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setVariant(String str) {
            try {
                this._locbld.setVariant(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setExtension(char c, String str) {
            try {
                this._locbld.setExtension(c, str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setUnicodeLocaleKeyword(String str, String str2) {
            try {
                this._locbld.setUnicodeLocaleKeyword(str, str2);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder addUnicodeLocaleAttribute(String str) {
            try {
                this._locbld.addUnicodeLocaleAttribute(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder removeUnicodeLocaleAttribute(String str) {
            try {
                this._locbld.removeUnicodeLocaleAttribute(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder clear() {
            this._locbld.clear();
            return this;
        }

        public Builder clearExtensions() {
            this._locbld.clearExtensions();
            return this;
        }

        public ULocale build() {
            return ULocale.getInstance(this._locbld.getBaseLocale(), this._locbld.getLocaleExtensions());
        }
    }

    private static ULocale getInstance(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        String strLscvToID = lscvToID(baseLocale.getLanguage(), baseLocale.getScript(), baseLocale.getRegion(), baseLocale.getVariant());
        Set<Character> keys = localeExtensions.getKeys();
        if (!keys.isEmpty()) {
            TreeMap treeMap = new TreeMap();
            for (Character ch : keys) {
                Extension extension = localeExtensions.getExtension(ch);
                if (extension instanceof UnicodeLocaleExtension) {
                    UnicodeLocaleExtension unicodeLocaleExtension = (UnicodeLocaleExtension) extension;
                    for (String str : unicodeLocaleExtension.getUnicodeLocaleKeys()) {
                        String unicodeLocaleType = unicodeLocaleExtension.getUnicodeLocaleType(str);
                        String legacyKey = toLegacyKey(str);
                        if (unicodeLocaleType.length() == 0) {
                            unicodeLocaleType = "yes";
                        }
                        String legacyType = toLegacyType(str, unicodeLocaleType);
                        if (legacyKey.equals("va") && legacyType.equals("posix") && baseLocale.getVariant().length() == 0) {
                            strLscvToID = strLscvToID + "_POSIX";
                        } else {
                            treeMap.put(legacyKey, legacyType);
                        }
                    }
                    Set<String> unicodeLocaleAttributes = unicodeLocaleExtension.getUnicodeLocaleAttributes();
                    if (unicodeLocaleAttributes.size() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (String str2 : unicodeLocaleAttributes) {
                            if (sb.length() > 0) {
                                sb.append('-');
                            }
                            sb.append(str2);
                        }
                        treeMap.put(LOCALE_ATTRIBUTE_KEY, sb.toString());
                    }
                } else {
                    treeMap.put(String.valueOf(ch), extension.getValue());
                }
            }
            if (!treeMap.isEmpty()) {
                StringBuilder sb2 = new StringBuilder(strLscvToID);
                sb2.append("@");
                boolean z = false;
                for (Map.Entry entry : treeMap.entrySet()) {
                    if (z) {
                        sb2.append(";");
                    } else {
                        z = true;
                    }
                    sb2.append((String) entry.getKey());
                    sb2.append("=");
                    sb2.append((String) entry.getValue());
                }
                strLscvToID = sb2.toString();
            }
        }
        return new ULocale(strLscvToID);
    }

    private BaseLocale base() {
        String variant;
        String script;
        String country;
        if (this.baseLocale == null) {
            String str = "";
            if (equals(ROOT)) {
                variant = "";
                script = variant;
                country = script;
            } else {
                LocaleIDParser localeIDParser = new LocaleIDParser(this.localeID);
                String language = localeIDParser.getLanguage();
                script = localeIDParser.getScript();
                country = localeIDParser.getCountry();
                variant = localeIDParser.getVariant();
                str = language;
            }
            this.baseLocale = BaseLocale.getInstance(str, script, country, variant);
        }
        return this.baseLocale;
    }

    private LocaleExtensions extensions() {
        if (this.extensions == null) {
            Iterator<String> keywords = getKeywords();
            if (keywords == null) {
                this.extensions = LocaleExtensions.EMPTY_EXTENSIONS;
            } else {
                InternalLocaleBuilder internalLocaleBuilder = new InternalLocaleBuilder();
                while (keywords.hasNext()) {
                    String next = keywords.next();
                    if (next.equals(LOCALE_ATTRIBUTE_KEY)) {
                        for (String str : getKeywordValue(next).split("[-_]")) {
                            try {
                                internalLocaleBuilder.addUnicodeLocaleAttribute(str);
                            } catch (LocaleSyntaxException e) {
                            }
                        }
                    } else if (next.length() >= 2) {
                        String unicodeLocaleKey = toUnicodeLocaleKey(next);
                        String unicodeLocaleType = toUnicodeLocaleType(next, getKeywordValue(next));
                        if (unicodeLocaleKey != null && unicodeLocaleType != null) {
                            try {
                                internalLocaleBuilder.setUnicodeLocaleKeyword(unicodeLocaleKey, unicodeLocaleType);
                            } catch (LocaleSyntaxException e2) {
                            }
                        }
                    } else if (next.length() == 1 && next.charAt(0) != 'u') {
                        try {
                            internalLocaleBuilder.setExtension(next.charAt(0), getKeywordValue(next).replace(BaseLocale.SEP, LanguageTag.SEP));
                        } catch (LocaleSyntaxException e3) {
                        }
                    }
                }
                this.extensions = internalLocaleBuilder.getLocaleExtensions();
            }
        }
        return this.extensions;
    }

    private static final class JDKLocaleHelper {
        private static final String[][] JAVA6_MAPDATA = {new String[]{"ja_JP_JP", "ja_JP", "calendar", "japanese", "ja"}, new String[]{"no_NO_NY", "nn_NO", null, null, "nn"}, new String[]{"th_TH_TH", "th_TH", "numbers", "thai", "th"}};
        private static Object eDISPLAY;
        private static Object eFORMAT;
        private static boolean hasLocaleCategories;
        private static boolean hasScriptsAndUnicodeExtensions;
        private static Method mForLanguageTag;
        private static Method mGetDefault;
        private static Method mGetExtension;
        private static Method mGetExtensionKeys;
        private static Method mGetScript;
        private static Method mGetUnicodeLocaleAttributes;
        private static Method mGetUnicodeLocaleKeys;
        private static Method mGetUnicodeLocaleType;
        private static Method mSetDefault;

        static {
            Class<?> cls;
            hasScriptsAndUnicodeExtensions = false;
            hasLocaleCategories = false;
            try {
                mGetScript = Locale.class.getMethod("getScript", (Class[]) null);
                mGetExtensionKeys = Locale.class.getMethod("getExtensionKeys", (Class[]) null);
                mGetExtension = Locale.class.getMethod("getExtension", Character.TYPE);
                mGetUnicodeLocaleKeys = Locale.class.getMethod("getUnicodeLocaleKeys", (Class[]) null);
                mGetUnicodeLocaleAttributes = Locale.class.getMethod("getUnicodeLocaleAttributes", (Class[]) null);
                mGetUnicodeLocaleType = Locale.class.getMethod("getUnicodeLocaleType", String.class);
                mForLanguageTag = Locale.class.getMethod("forLanguageTag", String.class);
                hasScriptsAndUnicodeExtensions = true;
            } catch (IllegalArgumentException e) {
            } catch (NoSuchMethodException e2) {
            } catch (SecurityException e3) {
            }
            try {
                Class<?>[] declaredClasses = Locale.class.getDeclaredClasses();
                int length = declaredClasses.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        cls = declaredClasses[i];
                        if (cls.getName().equals("java.util.Locale$Category")) {
                            break;
                        } else {
                            i++;
                        }
                    } else {
                        cls = null;
                        break;
                    }
                }
                if (cls != null) {
                    mGetDefault = Locale.class.getDeclaredMethod("getDefault", cls);
                    mSetDefault = Locale.class.getDeclaredMethod("setDefault", cls, Locale.class);
                    Method method = cls.getMethod("name", (Class[]) null);
                    for (Object obj : cls.getEnumConstants()) {
                        String str = (String) method.invoke(obj, (Object[]) null);
                        if (str.equals("DISPLAY")) {
                            eDISPLAY = obj;
                        } else if (str.equals("FORMAT")) {
                            eFORMAT = obj;
                        }
                    }
                    if (eDISPLAY != null && eFORMAT != null) {
                        hasLocaleCategories = true;
                    }
                }
            } catch (IllegalAccessException e4) {
            } catch (IllegalArgumentException e5) {
            } catch (NoSuchMethodException e6) {
            } catch (SecurityException e7) {
            } catch (InvocationTargetException e8) {
            }
        }

        private JDKLocaleHelper() {
        }

        public static boolean hasLocaleCategories() {
            return hasLocaleCategories;
        }

        public static ULocale toULocale(Locale locale) {
            return hasScriptsAndUnicodeExtensions ? toULocale7(locale) : toULocale6(locale);
        }

        public static Locale toLocale(ULocale uLocale) {
            return hasScriptsAndUnicodeExtensions ? toLocale7(uLocale) : toLocale6(uLocale);
        }

        private static ULocale toULocale7(Locale locale) {
            TreeSet<String> treeSet;
            TreeMap treeMap;
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            try {
                String str = (String) mGetScript.invoke(locale, (Object[]) null);
                Set<Character> set = (Set) mGetExtensionKeys.invoke(locale, (Object[]) null);
                boolean z = false;
                if (!set.isEmpty()) {
                    treeSet = null;
                    treeMap = null;
                    for (Character ch : set) {
                        if (ch.charValue() == 'u') {
                            Set set2 = (Set) mGetUnicodeLocaleAttributes.invoke(locale, (Object[]) null);
                            if (!set2.isEmpty()) {
                                treeSet = new TreeSet();
                                Iterator it = set2.iterator();
                                while (it.hasNext()) {
                                    treeSet.add((String) it.next());
                                }
                            }
                            for (String str2 : (Set) mGetUnicodeLocaleKeys.invoke(locale, (Object[]) null)) {
                                String str3 = (String) mGetUnicodeLocaleType.invoke(locale, str2);
                                if (str3 != null) {
                                    if (!str2.equals("va")) {
                                        if (treeMap == null) {
                                            treeMap = new TreeMap();
                                        }
                                        treeMap.put(str2, str3);
                                    } else {
                                        variant = variant.length() != 0 ? str3 + BaseLocale.SEP + variant : str3;
                                    }
                                }
                            }
                        } else {
                            String str4 = (String) mGetExtension.invoke(locale, ch);
                            if (str4 != null) {
                                if (treeMap == null) {
                                    treeMap = new TreeMap();
                                }
                                treeMap.put(String.valueOf(ch), str4);
                            }
                        }
                    }
                } else {
                    treeSet = null;
                    treeMap = null;
                }
                if (language.equals("no") && country.equals("NO") && variant.equals("NY")) {
                    language = "nn";
                    variant = "";
                }
                StringBuilder sb = new StringBuilder(language);
                if (str.length() > 0) {
                    sb.append(ULocale.UNDERSCORE);
                    sb.append(str);
                }
                if (country.length() > 0) {
                    sb.append(ULocale.UNDERSCORE);
                    sb.append(country);
                }
                if (variant.length() > 0) {
                    if (country.length() == 0) {
                        sb.append(ULocale.UNDERSCORE);
                    }
                    sb.append(ULocale.UNDERSCORE);
                    sb.append(variant);
                }
                if (treeSet != null) {
                    StringBuilder sb2 = new StringBuilder();
                    for (String str5 : treeSet) {
                        if (sb2.length() != 0) {
                            sb2.append('-');
                        }
                        sb2.append(str5);
                    }
                    if (treeMap == null) {
                        treeMap = new TreeMap();
                    }
                    treeMap.put(ULocale.LOCALE_ATTRIBUTE_KEY, sb2.toString());
                }
                if (treeMap != null) {
                    sb.append('@');
                    for (Map.Entry entry : treeMap.entrySet()) {
                        String legacyKey = (String) entry.getKey();
                        String legacyType = (String) entry.getValue();
                        if (legacyKey.length() != 1) {
                            legacyKey = ULocale.toLegacyKey(legacyKey);
                            if (legacyType.length() == 0) {
                                legacyType = "yes";
                            }
                            legacyType = ULocale.toLegacyType(legacyKey, legacyType);
                        }
                        if (z) {
                            sb.append(';');
                        } else {
                            z = true;
                        }
                        sb.append(legacyKey);
                        sb.append('=');
                        sb.append(legacyType);
                    }
                }
                return new ULocale(ULocale.getName(sb.toString()), locale);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2);
            }
        }

        private static ULocale toULocale6(Locale locale) {
            String string = locale.toString();
            if (string.length() == 0) {
                return ULocale.ROOT;
            }
            int i = 0;
            while (true) {
                if (i >= JAVA6_MAPDATA.length) {
                    break;
                }
                if (!JAVA6_MAPDATA[i][0].equals(string)) {
                    i++;
                } else {
                    LocaleIDParser localeIDParser = new LocaleIDParser(JAVA6_MAPDATA[i][1]);
                    localeIDParser.setKeywordValue(JAVA6_MAPDATA[i][2], JAVA6_MAPDATA[i][3]);
                    string = localeIDParser.getName();
                    break;
                }
            }
            return new ULocale(ULocale.getName(string), locale);
        }

        private static Locale toLocale7(ULocale uLocale) {
            String name = uLocale.getName();
            Locale locale = null;
            if (uLocale.getScript().length() > 0 || name.contains("@")) {
                try {
                    locale = (Locale) mForLanguageTag.invoke(null, AsciiUtil.toUpperString(uLocale.toLanguageTag()));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e2) {
                    throw new RuntimeException(e2);
                }
            }
            if (locale == null) {
                return new Locale(uLocale.getLanguage(), uLocale.getCountry(), uLocale.getVariant());
            }
            return locale;
        }

        private static Locale toLocale6(ULocale uLocale) {
            String baseName = uLocale.getBaseName();
            int i = 0;
            while (true) {
                if (i >= JAVA6_MAPDATA.length) {
                    break;
                }
                if (baseName.equals(JAVA6_MAPDATA[i][1]) || baseName.equals(JAVA6_MAPDATA[i][4])) {
                    if (JAVA6_MAPDATA[i][2] == null) {
                        baseName = JAVA6_MAPDATA[i][0];
                        break;
                    }
                    String keywordValue = uLocale.getKeywordValue(JAVA6_MAPDATA[i][2]);
                    if (keywordValue != null && keywordValue.equals(JAVA6_MAPDATA[i][3])) {
                        baseName = JAVA6_MAPDATA[i][0];
                        break;
                    }
                }
                i++;
            }
            String[] languageScriptCountryVariant = new LocaleIDParser(baseName).getLanguageScriptCountryVariant();
            return new Locale(languageScriptCountryVariant[0], languageScriptCountryVariant[2], languageScriptCountryVariant[3]);
        }

        public static Locale getDefault(Category category) {
            Object obj;
            Locale locale = Locale.getDefault();
            if (hasLocaleCategories) {
                switch (category) {
                    case DISPLAY:
                        obj = eDISPLAY;
                        break;
                    case FORMAT:
                        obj = eFORMAT;
                        break;
                    default:
                        obj = null;
                        break;
                }
                if (obj != null) {
                    try {
                        return (Locale) mGetDefault.invoke(null, obj);
                    } catch (IllegalAccessException e) {
                    } catch (IllegalArgumentException e2) {
                    } catch (InvocationTargetException e3) {
                    }
                }
            }
            return locale;
        }

        public static void setDefault(Category category, Locale locale) {
            Object obj;
            if (hasLocaleCategories) {
                switch (category) {
                    case DISPLAY:
                        obj = eDISPLAY;
                        break;
                    case FORMAT:
                        obj = eFORMAT;
                        break;
                    default:
                        obj = null;
                        break;
                }
                if (obj != null) {
                    try {
                        mSetDefault.invoke(null, obj, locale);
                    } catch (IllegalAccessException e) {
                    } catch (IllegalArgumentException e2) {
                    } catch (InvocationTargetException e3) {
                    }
                }
            }
        }

        public static boolean isOriginalDefaultLocale(Locale locale) {
            if (!hasScriptsAndUnicodeExtensions) {
                return locale.getLanguage().equals(getSystemProperty("user.language")) && locale.getCountry().equals(getSystemProperty("user.country")) && locale.getVariant().equals(getSystemProperty("user.variant"));
            }
            try {
                return locale.getLanguage().equals(getSystemProperty("user.language")) && locale.getCountry().equals(getSystemProperty("user.country")) && locale.getVariant().equals(getSystemProperty("user.variant")) && ((String) mGetScript.invoke(locale, (Object[]) null)).equals(getSystemProperty("user.script"));
            } catch (Exception e) {
                return false;
            }
        }

        public static String getSystemProperty(final String str) {
            if (System.getSecurityManager() != null) {
                try {
                    return (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return System.getProperty(str);
                        }
                    });
                } catch (AccessControlException e) {
                    return null;
                }
            }
            return System.getProperty(str);
        }
    }
}
