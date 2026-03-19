package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.MessageFormat;
import libcore.icu.ICU;
import sun.security.x509.PolicyInformation;
import sun.util.locale.BaseLocale;
import sun.util.locale.InternalLocaleBuilder;
import sun.util.locale.LanguageTag;
import sun.util.locale.LocaleExtensions;
import sun.util.locale.LocaleMatcher;
import sun.util.locale.LocaleObjectCache;
import sun.util.locale.LocaleSyntaxException;
import sun.util.locale.LocaleUtils;
import sun.util.locale.ParseStatus;

public final class Locale implements Cloneable, Serializable {
    static final boolean $assertionsDisabled = false;
    private static final int DISPLAY_COUNTRY = 1;
    private static final int DISPLAY_LANGUAGE = 0;
    private static final int DISPLAY_SCRIPT = 3;
    private static final int DISPLAY_VARIANT = 2;
    public static final char PRIVATE_USE_EXTENSION = 'x';
    private static final String UNDETERMINED_LANGUAGE = "und";
    public static final char UNICODE_LOCALE_EXTENSION = 'u';
    static final long serialVersionUID = 9149081749638150636L;
    private transient BaseLocale baseLocale;
    private volatile transient int hashCodeValue;
    private volatile transient String languageTag;
    private transient LocaleExtensions localeExtensions;
    private static final Cache LOCALECACHE = new Cache();
    public static final Locale ENGLISH = createConstant("en", "");
    public static final Locale FRENCH = createConstant("fr", "");
    public static final Locale GERMAN = createConstant("de", "");
    public static final Locale ITALIAN = createConstant("it", "");
    public static final Locale JAPANESE = createConstant("ja", "");
    public static final Locale KOREAN = createConstant("ko", "");
    public static final Locale CHINESE = createConstant("zh", "");
    public static final Locale SIMPLIFIED_CHINESE = createConstant("zh", "CN");
    public static final Locale TRADITIONAL_CHINESE = createConstant("zh", "TW");
    public static final Locale FRANCE = createConstant("fr", "FR");
    public static final Locale GERMANY = createConstant("de", "DE");
    public static final Locale ITALY = createConstant("it", "IT");
    public static final Locale JAPAN = createConstant("ja", "JP");
    public static final Locale KOREA = createConstant("ko", "KR");
    public static final Locale CHINA = SIMPLIFIED_CHINESE;
    public static final Locale PRC = SIMPLIFIED_CHINESE;
    public static final Locale TAIWAN = TRADITIONAL_CHINESE;
    public static final Locale UK = createConstant("en", "GB");
    public static final Locale US = createConstant("en", "US");
    public static final Locale CANADA = createConstant("en", "CA");
    public static final Locale CANADA_FRENCH = createConstant("fr", "CA");
    public static final Locale ROOT = createConstant("", "");
    private static volatile Locale defaultDisplayLocale = null;
    private static volatile Locale defaultFormatLocale = null;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("language", String.class), new ObjectStreamField("country", String.class), new ObjectStreamField("variant", String.class), new ObjectStreamField("hashcode", Integer.TYPE), new ObjectStreamField("script", String.class), new ObjectStreamField("extensions", String.class)};
    private static volatile String[] isoLanguages = null;
    private static volatile String[] isoCountries = null;

    public enum FilteringMode {
        AUTOSELECT_FILTERING,
        EXTENDED_FILTERING,
        IGNORE_EXTENDED_RANGES,
        MAP_EXTENDED_RANGES,
        REJECT_EXTENDED_RANGES
    }

    private Locale(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        this.hashCodeValue = 0;
        this.baseLocale = baseLocale;
        this.localeExtensions = localeExtensions;
    }

    public Locale(String str, String str2, String str3) {
        this.hashCodeValue = 0;
        if (str == null || str2 == null || str3 == null) {
            throw new NullPointerException();
        }
        this.baseLocale = BaseLocale.getInstance(convertOldISOCodes(str), "", str2, str3);
        this.localeExtensions = getCompatibilityExtensions(str, "", str2, str3);
    }

    public Locale(String str, String str2) {
        this(str, str2, "");
    }

    public Locale(String str) {
        this(str, "", "");
    }

    private static Locale createConstant(String str, String str2) {
        return getInstance(BaseLocale.createInstance(str, str2), null);
    }

    static Locale getInstance(String str, String str2, String str3) {
        return getInstance(str, "", str2, str3, null);
    }

    static Locale getInstance(String str, String str2, String str3, String str4, LocaleExtensions localeExtensions) {
        if (str == null || str2 == null || str3 == null || str4 == null) {
            throw new NullPointerException();
        }
        if (localeExtensions == null) {
            localeExtensions = getCompatibilityExtensions(str, str2, str3, str4);
        }
        return getInstance(BaseLocale.getInstance(str, str2, str3, str4), localeExtensions);
    }

    static Locale getInstance(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        return LOCALECACHE.get(new LocaleKey(baseLocale, localeExtensions));
    }

    private static class Cache extends LocaleObjectCache<LocaleKey, Locale> {
        private Cache() {
        }

        @Override
        protected Locale createObject(LocaleKey localeKey) {
            return new Locale(localeKey.base, localeKey.exts);
        }
    }

    private static final class LocaleKey {
        private final BaseLocale base;
        private final LocaleExtensions exts;
        private final int hash;

        private LocaleKey(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
            this.base = baseLocale;
            this.exts = localeExtensions;
            int iHashCode = this.base.hashCode();
            this.hash = this.exts != null ? iHashCode ^ this.exts.hashCode() : iHashCode;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LocaleKey)) {
                return Locale.$assertionsDisabled;
            }
            LocaleKey localeKey = (LocaleKey) obj;
            if (this.hash != localeKey.hash || !this.base.equals(localeKey.base)) {
                return Locale.$assertionsDisabled;
            }
            if (this.exts == null) {
                if (localeKey.exts == null) {
                    return true;
                }
                return Locale.$assertionsDisabled;
            }
            return this.exts.equals(localeKey.exts);
        }

        public int hashCode() {
            return this.hash;
        }
    }

    public static Locale getDefault() {
        return NoImagePreloadHolder.defaultLocale;
    }

    public static Locale getDefault(Category category) {
        switch (category) {
            case DISPLAY:
                if (defaultDisplayLocale == null) {
                    synchronized (Locale.class) {
                        if (defaultDisplayLocale == null) {
                            defaultDisplayLocale = initDefault(category);
                        }
                        break;
                    }
                }
                return defaultDisplayLocale;
            case FORMAT:
                if (defaultFormatLocale == null) {
                    synchronized (Locale.class) {
                        if (defaultFormatLocale == null) {
                            defaultFormatLocale = initDefault(category);
                        }
                        break;
                    }
                }
                return defaultFormatLocale;
            default:
                return getDefault();
        }
    }

    public static Locale initDefault() {
        String property;
        String property2;
        String property3 = System.getProperty("user.locale", "");
        if (!property3.isEmpty()) {
            return forLanguageTag(property3);
        }
        String property4 = System.getProperty("user.language", "en");
        String property5 = System.getProperty("user.region");
        if (property5 != null) {
            int iIndexOf = property5.indexOf(95);
            if (iIndexOf >= 0) {
                String strSubstring = property5.substring(0, iIndexOf);
                property2 = property5.substring(iIndexOf + 1);
                property5 = strSubstring;
            } else {
                property2 = "";
            }
            property = "";
        } else {
            property = System.getProperty("user.script", "");
            property5 = System.getProperty("user.country", "");
            property2 = System.getProperty("user.variant", "");
        }
        return getInstance(property4, property, property5, property2, null);
    }

    private static Locale initDefault(Category category) {
        Locale locale = NoImagePreloadHolder.defaultLocale;
        return getInstance(System.getProperty(category.languageKey, locale.getLanguage()), System.getProperty(category.scriptKey, locale.getScript()), System.getProperty(category.countryKey, locale.getCountry()), System.getProperty(category.variantKey, locale.getVariant()), null);
    }

    public static synchronized void setDefault(Locale locale) {
        setDefault(Category.DISPLAY, locale);
        setDefault(Category.FORMAT, locale);
        NoImagePreloadHolder.defaultLocale = locale;
        ICU.setDefaultLocale(locale.toLanguageTag());
    }

    public static synchronized void setDefault(Category category, Locale locale) {
        if (category == null) {
            throw new NullPointerException("Category cannot be NULL");
        }
        if (locale == null) {
            throw new NullPointerException("Can't set default locale to NULL");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new PropertyPermission("user.language", "write"));
        }
        switch (category) {
            case DISPLAY:
                defaultDisplayLocale = locale;
                break;
            case FORMAT:
                defaultFormatLocale = locale;
                break;
        }
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableLocales();
    }

    public static String[] getISOCountries() {
        return ICU.getISOCountries();
    }

    public static String[] getISOLanguages() {
        return ICU.getISOLanguages();
    }

    public String getLanguage() {
        return this.baseLocale.getLanguage();
    }

    public String getScript() {
        return this.baseLocale.getScript();
    }

    public String getCountry() {
        return this.baseLocale.getRegion();
    }

    public String getVariant() {
        return this.baseLocale.getVariant();
    }

    public boolean hasExtensions() {
        if (this.localeExtensions != null) {
            return true;
        }
        return $assertionsDisabled;
    }

    public Locale stripExtensions() {
        return hasExtensions() ? getInstance(this.baseLocale, null) : this;
    }

    public String getExtension(char c) {
        if (!LocaleExtensions.isValidKey(c)) {
            throw new IllegalArgumentException("Ill-formed extension key: " + c);
        }
        if (hasExtensions()) {
            return this.localeExtensions.getExtensionValue(Character.valueOf(c));
        }
        return null;
    }

    public Set<Character> getExtensionKeys() {
        if (!hasExtensions()) {
            return Collections.emptySet();
        }
        return this.localeExtensions.getKeys();
    }

    public Set<String> getUnicodeLocaleAttributes() {
        if (!hasExtensions()) {
            return Collections.emptySet();
        }
        return this.localeExtensions.getUnicodeLocaleAttributes();
    }

    public String getUnicodeLocaleType(String str) {
        if (!isUnicodeExtensionKey(str)) {
            throw new IllegalArgumentException("Ill-formed Unicode locale key: " + str);
        }
        if (hasExtensions()) {
            return this.localeExtensions.getUnicodeLocaleType(str);
        }
        return null;
    }

    public Set<String> getUnicodeLocaleKeys() {
        if (this.localeExtensions == null) {
            return Collections.emptySet();
        }
        return this.localeExtensions.getUnicodeLocaleKeys();
    }

    BaseLocale getBaseLocale() {
        return this.baseLocale;
    }

    LocaleExtensions getLocaleExtensions() {
        return this.localeExtensions;
    }

    public final String toString() {
        int length = this.baseLocale.getLanguage().length();
        boolean z = $assertionsDisabled;
        boolean z2 = length != 0;
        boolean z3 = this.baseLocale.getScript().length() != 0;
        boolean z4 = this.baseLocale.getRegion().length() != 0;
        boolean z5 = this.baseLocale.getVariant().length() != 0;
        if (this.localeExtensions != null && this.localeExtensions.getID().length() != 0) {
            z = true;
        }
        StringBuilder sb = new StringBuilder(this.baseLocale.getLanguage());
        if (z4 || (z2 && (z5 || z3 || z))) {
            sb.append('_');
            sb.append(this.baseLocale.getRegion());
        }
        if (z5 && (z2 || z4)) {
            sb.append('_');
            sb.append(this.baseLocale.getVariant());
        }
        if (z3 && (z2 || z4)) {
            sb.append("_#");
            sb.append(this.baseLocale.getScript());
        }
        if (z && (z2 || z4)) {
            sb.append('_');
            if (!z3) {
                sb.append('#');
            }
            sb.append(this.localeExtensions.getID());
        }
        return sb.toString();
    }

    public String toLanguageTag() {
        if (this.languageTag != null) {
            return this.languageTag;
        }
        LanguageTag locale = LanguageTag.parseLocale(this.baseLocale, this.localeExtensions);
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
            sb.append(str);
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
            sb.append(privateuse);
        }
        String string = sb.toString();
        synchronized (this) {
            if (this.languageTag == null) {
                this.languageTag = string;
            }
        }
        return this.languageTag;
    }

    public static Locale forLanguageTag(String str) {
        LanguageTag languageTag = LanguageTag.parse(str, null);
        InternalLocaleBuilder internalLocaleBuilder = new InternalLocaleBuilder();
        internalLocaleBuilder.setLanguageTag(languageTag);
        BaseLocale baseLocale = internalLocaleBuilder.getBaseLocale();
        LocaleExtensions localeExtensions = internalLocaleBuilder.getLocaleExtensions();
        if (localeExtensions == null && baseLocale.getVariant().length() > 0) {
            localeExtensions = getCompatibilityExtensions(baseLocale.getLanguage(), baseLocale.getScript(), baseLocale.getRegion(), baseLocale.getVariant());
        }
        return getInstance(baseLocale, localeExtensions);
    }

    public String getISO3Language() throws MissingResourceException {
        String language = this.baseLocale.getLanguage();
        if (language.length() == 3) {
            return language;
        }
        if (language.isEmpty()) {
            return "";
        }
        String iSO3Language = ICU.getISO3Language(language);
        if (!language.isEmpty() && iSO3Language.isEmpty()) {
            throw new MissingResourceException("Couldn't find 3-letter language code for " + language, "FormatData_" + toString(), "ShortLanguage");
        }
        return iSO3Language;
    }

    public String getISO3Country() throws MissingResourceException {
        String region = this.baseLocale.getRegion();
        if (region.length() == 3) {
            return this.baseLocale.getRegion();
        }
        if (region.isEmpty()) {
            return "";
        }
        String iSO3Country = ICU.getISO3Country("en-" + region);
        if (!region.isEmpty() && iSO3Country.isEmpty()) {
            throw new MissingResourceException("Couldn't find 3-letter country code for " + this.baseLocale.getRegion(), "FormatData_" + toString(), "ShortCountry");
        }
        return iSO3Country;
    }

    public final String getDisplayLanguage() {
        return getDisplayLanguage(getDefault(Category.DISPLAY));
    }

    public String getDisplayLanguage(Locale locale) {
        String language = this.baseLocale.getLanguage();
        if (language.isEmpty()) {
            return "";
        }
        if ("und".equals(normalizeAndValidateLanguage(language, $assertionsDisabled))) {
            return language;
        }
        String displayLanguage = ICU.getDisplayLanguage(this, locale);
        if (displayLanguage == null) {
            return ICU.getDisplayLanguage(this, getDefault());
        }
        return displayLanguage;
    }

    private static String normalizeAndValidateLanguage(String str, boolean z) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        String lowerCase = str.toLowerCase(ROOT);
        if (!isValidBcp47Alpha(lowerCase, 2, 3)) {
            if (z) {
                throw new IllformedLocaleException("Invalid language: " + str);
            }
            return "und";
        }
        return lowerCase;
    }

    private static boolean isAsciiAlphaNum(String str) {
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if ((cCharAt < 'a' || cCharAt > 'z') && ((cCharAt < 'A' || cCharAt > 'Z') && (cCharAt < '0' || cCharAt > '9'))) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    public String getDisplayScript() {
        return getDisplayScript(getDefault(Category.DISPLAY));
    }

    public String getDisplayScript(Locale locale) {
        if (this.baseLocale.getScript().isEmpty()) {
            return "";
        }
        String displayScript = ICU.getDisplayScript(this, locale);
        if (displayScript == null) {
            return ICU.getDisplayScript(this, getDefault(Category.DISPLAY));
        }
        return displayScript;
    }

    public final String getDisplayCountry() {
        return getDisplayCountry(getDefault(Category.DISPLAY));
    }

    public String getDisplayCountry(Locale locale) {
        String region = this.baseLocale.getRegion();
        if (region.isEmpty()) {
            return "";
        }
        if (normalizeAndValidateRegion(region, $assertionsDisabled).isEmpty()) {
            return region;
        }
        String displayCountry = ICU.getDisplayCountry(this, locale);
        if (displayCountry == null) {
            return ICU.getDisplayCountry(this, getDefault());
        }
        return displayCountry;
    }

    private static String normalizeAndValidateRegion(String str, boolean z) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        String upperCase = str.toUpperCase(ROOT);
        if (!isValidBcp47Alpha(upperCase, 2, 2) && !isUnM49AreaCode(upperCase)) {
            if (z) {
                throw new IllformedLocaleException("Invalid region: " + str);
            }
            return "";
        }
        return upperCase;
    }

    private static boolean isValidBcp47Alpha(String str, int i, int i2) {
        int length = str.length();
        if (length < i || length > i2) {
            return $assertionsDisabled;
        }
        for (int i3 = 0; i3 < length; i3++) {
            char cCharAt = str.charAt(i3);
            if ((cCharAt < 'a' || cCharAt > 'z') && (cCharAt < 'A' || cCharAt > 'Z')) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    private static boolean isUnM49AreaCode(String str) {
        if (str.length() != 3) {
            return $assertionsDisabled;
        }
        for (int i = 0; i < 3; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    public final String getDisplayVariant() {
        return getDisplayVariant(getDefault(Category.DISPLAY));
    }

    public String getDisplayVariant(Locale locale) {
        String variant = this.baseLocale.getVariant();
        if (variant.isEmpty()) {
            return "";
        }
        try {
            normalizeAndValidateVariant(variant);
            String displayVariant = ICU.getDisplayVariant(this, locale);
            if (displayVariant == null) {
                displayVariant = ICU.getDisplayVariant(this, getDefault());
            }
            if (displayVariant.isEmpty()) {
                return variant;
            }
            return displayVariant;
        } catch (IllformedLocaleException e) {
            return variant;
        }
    }

    private static String normalizeAndValidateVariant(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        String strReplace = str.replace('-', '_');
        for (String str2 : strReplace.split(BaseLocale.SEP)) {
            if (!isValidVariantSubtag(str2)) {
                throw new IllformedLocaleException("Invalid variant: " + str);
            }
        }
        return strReplace;
    }

    private static boolean isValidVariantSubtag(String str) {
        char cCharAt;
        if (str.length() >= 5 && str.length() <= 8) {
            if (isAsciiAlphaNum(str)) {
                return true;
            }
        } else if (str.length() == 4 && (cCharAt = str.charAt(0)) >= '0' && cCharAt <= '9' && isAsciiAlphaNum(str)) {
            return true;
        }
        return $assertionsDisabled;
    }

    public final String getDisplayName() {
        return getDisplayName(getDefault(Category.DISPLAY));
    }

    public String getDisplayName(Locale locale) {
        int i;
        StringBuilder sb = new StringBuilder();
        String language = this.baseLocale.getLanguage();
        if (language.isEmpty()) {
            i = 0;
        } else {
            String displayLanguage = getDisplayLanguage(locale);
            if (!displayLanguage.isEmpty()) {
                language = displayLanguage;
            }
            sb.append(language);
            i = 1;
        }
        String script = this.baseLocale.getScript();
        if (!script.isEmpty()) {
            if (i == 1) {
                sb.append(" (");
            }
            String displayScript = getDisplayScript(locale);
            if (!displayScript.isEmpty()) {
                script = displayScript;
            }
            sb.append(script);
            i++;
        }
        String region = this.baseLocale.getRegion();
        if (!region.isEmpty()) {
            if (i == 1) {
                sb.append(" (");
            } else if (i == 2) {
                sb.append(",");
            }
            String displayCountry = getDisplayCountry(locale);
            if (!displayCountry.isEmpty()) {
                region = displayCountry;
            }
            sb.append(region);
            i++;
        }
        String variant = this.baseLocale.getVariant();
        if (!variant.isEmpty()) {
            if (i == 1) {
                sb.append(" (");
            } else if (i == 2 || i == 3) {
                sb.append(",");
            }
            String displayVariant = getDisplayVariant(locale);
            if (displayVariant.isEmpty()) {
                displayVariant = variant;
            }
            sb.append(displayVariant);
            i++;
        }
        if (i > 1) {
            sb.append(")");
        }
        return sb.toString();
    }

    public Object clone() {
        try {
            return (Locale) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public int hashCode() {
        int iHashCode = this.hashCodeValue;
        if (iHashCode == 0) {
            iHashCode = this.baseLocale.hashCode();
            if (this.localeExtensions != null) {
                iHashCode ^= this.localeExtensions.hashCode();
            }
            this.hashCodeValue = iHashCode;
        }
        return iHashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Locale)) {
            return $assertionsDisabled;
        }
        Locale locale = (Locale) obj;
        if (!this.baseLocale.equals(locale.baseLocale)) {
            return $assertionsDisabled;
        }
        if (this.localeExtensions == null) {
            if (locale.localeExtensions == null) {
                return true;
            }
            return $assertionsDisabled;
        }
        return this.localeExtensions.equals(locale.localeExtensions);
    }

    private static class NoImagePreloadHolder {
        public static volatile Locale defaultLocale = Locale.initDefault();

        private NoImagePreloadHolder() {
        }
    }

    private static String formatList(String[] strArr, String str, String str2) {
        if (str == null || str2 == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < strArr.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(strArr[i]);
            }
            return sb.toString();
        }
        if (strArr.length > 3) {
            strArr = composeList(new MessageFormat(str2), strArr);
        }
        Object[] objArr = new Object[strArr.length + 1];
        System.arraycopy(strArr, 0, objArr, 1, strArr.length);
        objArr[0] = new Integer(strArr.length);
        return new MessageFormat(str).format(objArr);
    }

    private static String[] composeList(MessageFormat messageFormat, String[] strArr) {
        if (strArr.length <= 3) {
            return strArr;
        }
        String str = messageFormat.format(new String[]{strArr[0], strArr[1]});
        String[] strArr2 = new String[strArr.length - 1];
        System.arraycopy(strArr, 2, strArr2, 1, strArr2.length - 1);
        strArr2[0] = str;
        return composeList(messageFormat, strArr2);
    }

    private static boolean isUnicodeExtensionKey(String str) {
        if (str.length() == 2 && LocaleUtils.isAlphaNumericString(str)) {
            return true;
        }
        return $assertionsDisabled;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("language", this.baseLocale.getLanguage());
        putFieldPutFields.put("script", this.baseLocale.getScript());
        putFieldPutFields.put("country", this.baseLocale.getRegion());
        putFieldPutFields.put("variant", this.baseLocale.getVariant());
        putFieldPutFields.put("extensions", this.localeExtensions == null ? "" : this.localeExtensions.getID());
        putFieldPutFields.put("hashcode", -1);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        String str = (String) fields.get("language", "");
        String str2 = (String) fields.get("script", "");
        String str3 = (String) fields.get("country", "");
        String str4 = (String) fields.get("variant", "");
        String str5 = (String) fields.get("extensions", "");
        this.baseLocale = BaseLocale.getInstance(convertOldISOCodes(str), str2, str3, str4);
        if (str5 != null && str5.length() > 0) {
            try {
                InternalLocaleBuilder internalLocaleBuilder = new InternalLocaleBuilder();
                internalLocaleBuilder.setExtensions(str5);
                this.localeExtensions = internalLocaleBuilder.getLocaleExtensions();
                return;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage());
            }
        }
        this.localeExtensions = null;
    }

    private Object readResolve() throws ObjectStreamException {
        return getInstance(this.baseLocale.getLanguage(), this.baseLocale.getScript(), this.baseLocale.getRegion(), this.baseLocale.getVariant(), this.localeExtensions);
    }

    private static String convertOldISOCodes(String str) {
        String strIntern = LocaleUtils.toLowerString(str).intern();
        if (strIntern == "he") {
            return "iw";
        }
        if (strIntern == "yi") {
            return "ji";
        }
        if (strIntern == PolicyInformation.ID) {
            return "in";
        }
        return strIntern;
    }

    private static LocaleExtensions getCompatibilityExtensions(String str, String str2, String str3, String str4) {
        if (LocaleUtils.caseIgnoreMatch(str, "ja") && str2.length() == 0 && LocaleUtils.caseIgnoreMatch(str3, "jp") && "JP".equals(str4)) {
            return LocaleExtensions.CALENDAR_JAPANESE;
        }
        if (LocaleUtils.caseIgnoreMatch(str, "th") && str2.length() == 0 && LocaleUtils.caseIgnoreMatch(str3, "th") && "TH".equals(str4)) {
            return LocaleExtensions.NUMBER_THAI;
        }
        return null;
    }

    public static String adjustLanguageCode(String str) {
        String lowerCase = str.toLowerCase(US);
        if (str.equals("he")) {
            return "iw";
        }
        if (str.equals(PolicyInformation.ID)) {
            return "in";
        }
        if (str.equals("yi")) {
            return "ji";
        }
        return lowerCase;
    }

    public enum Category {
        DISPLAY("user.language.display", "user.script.display", "user.country.display", "user.variant.display"),
        FORMAT("user.language.format", "user.script.format", "user.country.format", "user.variant.format");

        final String countryKey;
        final String languageKey;
        final String scriptKey;
        final String variantKey;

        Category(String str, String str2, String str3, String str4) {
            this.languageKey = str;
            this.scriptKey = str2;
            this.countryKey = str3;
            this.variantKey = str4;
        }
    }

    public static final class Builder {
        private final InternalLocaleBuilder localeBuilder = new InternalLocaleBuilder();

        public Builder setLocale(Locale locale) {
            try {
                this.localeBuilder.setLocale(locale.baseLocale, locale.localeExtensions);
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
            this.localeBuilder.setLanguageTag(languageTag);
            return this;
        }

        public Builder setLanguage(String str) {
            try {
                this.localeBuilder.setLanguage(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setScript(String str) {
            try {
                this.localeBuilder.setScript(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setRegion(String str) {
            try {
                this.localeBuilder.setRegion(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setVariant(String str) {
            try {
                this.localeBuilder.setVariant(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setExtension(char c, String str) {
            try {
                this.localeBuilder.setExtension(c, str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder setUnicodeLocaleKeyword(String str, String str2) {
            try {
                this.localeBuilder.setUnicodeLocaleKeyword(str, str2);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder addUnicodeLocaleAttribute(String str) {
            try {
                this.localeBuilder.addUnicodeLocaleAttribute(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder removeUnicodeLocaleAttribute(String str) {
            if (str == null) {
                throw new NullPointerException("attribute == null");
            }
            try {
                this.localeBuilder.removeUnicodeLocaleAttribute(str);
                return this;
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
        }

        public Builder clear() {
            this.localeBuilder.clear();
            return this;
        }

        public Builder clearExtensions() {
            this.localeBuilder.clearExtensions();
            return this;
        }

        public Locale build() {
            BaseLocale baseLocale = this.localeBuilder.getBaseLocale();
            LocaleExtensions localeExtensions = this.localeBuilder.getLocaleExtensions();
            if (localeExtensions == null && baseLocale.getVariant().length() > 0) {
                localeExtensions = Locale.getCompatibilityExtensions(baseLocale.getLanguage(), baseLocale.getScript(), baseLocale.getRegion(), baseLocale.getVariant());
            }
            return Locale.getInstance(baseLocale, localeExtensions);
        }
    }

    public static final class LanguageRange {
        public static final double MAX_WEIGHT = 1.0d;
        public static final double MIN_WEIGHT = 0.0d;
        private volatile int hash;
        private final String range;
        private final double weight;

        public LanguageRange(String str) {
            this(str, 1.0d);
        }

        public LanguageRange(String str, double d) {
            this.hash = 0;
            if (str == null) {
                throw new NullPointerException();
            }
            if (d < 0.0d || d > 1.0d) {
                throw new IllegalArgumentException("weight=" + d);
            }
            String lowerCase = str.toLowerCase();
            String[] strArrSplit = lowerCase.split(LanguageTag.SEP);
            boolean z = true;
            if (!isSubtagIllFormed(strArrSplit[0], true) && !lowerCase.endsWith(LanguageTag.SEP)) {
                int i = 1;
                while (true) {
                    if (i < strArrSplit.length) {
                        if (isSubtagIllFormed(strArrSplit[i], Locale.$assertionsDisabled)) {
                            break;
                        } else {
                            i++;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
            }
            if (z) {
                throw new IllegalArgumentException("range=" + lowerCase);
            }
            this.range = lowerCase;
            this.weight = d;
        }

        private static boolean isSubtagIllFormed(String str, boolean z) {
            if (str.equals("") || str.length() > 8) {
                return true;
            }
            if (str.equals("*")) {
                return Locale.$assertionsDisabled;
            }
            char[] charArray = str.toCharArray();
            if (z) {
                for (char c : charArray) {
                    if (c < 'a' || c > 'z') {
                        return true;
                    }
                }
            } else {
                for (char c2 : charArray) {
                    if (c2 < '0' || ((c2 > '9' && c2 < 'a') || c2 > 'z')) {
                        return true;
                    }
                }
            }
            return Locale.$assertionsDisabled;
        }

        public String getRange() {
            return this.range;
        }

        public double getWeight() {
            return this.weight;
        }

        public static List<LanguageRange> parse(String str) {
            return LocaleMatcher.parse(str);
        }

        public static List<LanguageRange> parse(String str, Map<String, List<String>> map) {
            return mapEquivalents(parse(str), map);
        }

        public static List<LanguageRange> mapEquivalents(List<LanguageRange> list, Map<String, List<String>> map) {
            return LocaleMatcher.mapEquivalents(list, map);
        }

        public int hashCode() {
            if (this.hash == 0) {
                int iHashCode = 629 + this.range.hashCode();
                long jDoubleToLongBits = Double.doubleToLongBits(this.weight);
                this.hash = (37 * iHashCode) + ((int) (jDoubleToLongBits ^ (jDoubleToLongBits >>> 32)));
            }
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LanguageRange)) {
                return Locale.$assertionsDisabled;
            }
            LanguageRange languageRange = (LanguageRange) obj;
            if (this.hash == languageRange.hash && this.range.equals(languageRange.range) && this.weight == languageRange.weight) {
                return true;
            }
            return Locale.$assertionsDisabled;
        }
    }

    public static List<Locale> filter(List<LanguageRange> list, Collection<Locale> collection, FilteringMode filteringMode) {
        return LocaleMatcher.filter(list, collection, filteringMode);
    }

    public static List<Locale> filter(List<LanguageRange> list, Collection<Locale> collection) {
        return filter(list, collection, FilteringMode.AUTOSELECT_FILTERING);
    }

    public static List<String> filterTags(List<LanguageRange> list, Collection<String> collection, FilteringMode filteringMode) {
        return LocaleMatcher.filterTags(list, collection, filteringMode);
    }

    public static List<String> filterTags(List<LanguageRange> list, Collection<String> collection) {
        return filterTags(list, collection, FilteringMode.AUTOSELECT_FILTERING);
    }

    public static Locale lookup(List<LanguageRange> list, Collection<Locale> collection) {
        return LocaleMatcher.lookup(list, collection);
    }

    public static String lookupTag(List<LanguageRange> list, Collection<String> collection) {
        return LocaleMatcher.lookupTag(list, collection);
    }
}
