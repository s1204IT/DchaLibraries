package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import libcore.icu.ICU;

public final class Locale implements Cloneable, Serializable {
    private static final TreeMap<String, String> GRANDFATHERED_LOCALES;
    public static final char PRIVATE_USE_EXTENSION = 'x';
    private static final String UNDETERMINED_LANGUAGE = "und";
    public static final char UNICODE_LOCALE_EXTENSION = 'u';
    private static Locale defaultLocale = null;
    private static final ObjectStreamField[] serialPersistentFields;
    private static final long serialVersionUID = 9149081749638150636L;
    private transient String cachedIcuLocaleId;
    private transient String cachedLanguageTag;
    private transient String cachedToStringResult;
    private transient String countryCode;
    private transient Map<Character, String> extensions;
    private final transient boolean hasValidatedFields;
    private transient String languageCode;
    private transient String scriptCode;
    private transient Set<String> unicodeAttributes;
    private transient Map<String, String> unicodeKeywords;
    private transient String variantCode;
    public static final Locale CANADA = new Locale(true, "en", "CA");
    public static final Locale CANADA_FRENCH = new Locale(true, "fr", "CA");
    public static final Locale CHINA = new Locale(true, "zh", "CN");
    public static final Locale CHINESE = new Locale(true, "zh", "");
    public static final Locale ENGLISH = new Locale(true, "en", "");
    public static final Locale FRANCE = new Locale(true, "fr", "FR");
    public static final Locale FRENCH = new Locale(true, "fr", "");
    public static final Locale GERMAN = new Locale(true, "de", "");
    public static final Locale GERMANY = new Locale(true, "de", "DE");
    public static final Locale ITALIAN = new Locale(true, "it", "");
    public static final Locale ITALY = new Locale(true, "it", "IT");
    public static final Locale JAPAN = new Locale(true, "ja", "JP");
    public static final Locale JAPANESE = new Locale(true, "ja", "");
    public static final Locale KOREA = new Locale(true, "ko", "KR");
    public static final Locale KOREAN = new Locale(true, "ko", "");
    public static final Locale PRC = new Locale(true, "zh", "CN");
    public static final Locale ROOT = new Locale(true, "", "");
    public static final Locale SIMPLIFIED_CHINESE = new Locale(true, "zh", "CN");
    public static final Locale TAIWAN = new Locale(true, "zh", "TW");
    public static final Locale TRADITIONAL_CHINESE = new Locale(true, "zh", "TW");
    public static final Locale UK = new Locale(true, "en", "GB");
    public static final Locale US = new Locale(true, "en", "US");

    static {
        defaultLocale = US;
        String language = System.getProperty("user.language", "en");
        String region = System.getProperty("user.region", "US");
        String variant = System.getProperty("user.variant", "");
        defaultLocale = new Locale(language, region, variant);
        serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("country", (Class<?>) String.class), new ObjectStreamField("hashcode", Integer.TYPE), new ObjectStreamField("language", (Class<?>) String.class), new ObjectStreamField("variant", (Class<?>) String.class), new ObjectStreamField("script", (Class<?>) String.class), new ObjectStreamField("extensions", (Class<?>) String.class)};
        GRANDFATHERED_LOCALES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        GRANDFATHERED_LOCALES.put("en-GB-oed", "en-GB-x-oed");
        GRANDFATHERED_LOCALES.put("i-ami", "ami");
        GRANDFATHERED_LOCALES.put("i-bnn", "bnn");
        GRANDFATHERED_LOCALES.put("i-default", "en-x-i-default");
        GRANDFATHERED_LOCALES.put("i-enochian", "und-x-i-enochian");
        GRANDFATHERED_LOCALES.put("i-hak", "hak");
        GRANDFATHERED_LOCALES.put("i-klingon", "tlh");
        GRANDFATHERED_LOCALES.put("i-lux", "lb");
        GRANDFATHERED_LOCALES.put("i-mingo", "see-x-i-mingo");
        GRANDFATHERED_LOCALES.put("i-navajo", "nv");
        GRANDFATHERED_LOCALES.put("i-pwn", "pwn");
        GRANDFATHERED_LOCALES.put("i-tao", "tao");
        GRANDFATHERED_LOCALES.put("i-tay", "tay");
        GRANDFATHERED_LOCALES.put("i-tsu", "tsu");
        GRANDFATHERED_LOCALES.put("sgn-BE-FR", "sfb");
        GRANDFATHERED_LOCALES.put("sgn-BE-NL", "vgt");
        GRANDFATHERED_LOCALES.put("sgn-CH-DE", "sgg");
        GRANDFATHERED_LOCALES.put("art-lojban", "jbo");
        GRANDFATHERED_LOCALES.put("cel-gaulish", "xtg-x-cel-gaulish");
        GRANDFATHERED_LOCALES.put("no-bok", "nb");
        GRANDFATHERED_LOCALES.put("no-nyn", "nn");
        GRANDFATHERED_LOCALES.put("zh-guoyu", "cmn");
        GRANDFATHERED_LOCALES.put("zh-hakka", "hak");
        GRANDFATHERED_LOCALES.put("zh-min", "nan-x-zh-min");
        GRANDFATHERED_LOCALES.put("zh-min-nan", "nan");
        GRANDFATHERED_LOCALES.put("zh-xiang", "hsn");
    }

    public static final class Builder {
        private String script = "";
        private String variant = "";
        private String region = "";
        private String language = "";
        private final Set<String> attributes = new TreeSet();
        private final Map<String, String> keywords = new TreeMap();
        private final Map<Character, String> extensions = new TreeMap();

        public Builder setLanguage(String language) {
            this.language = normalizeAndValidateLanguage(language, true);
            return this;
        }

        private static String normalizeAndValidateLanguage(String language, boolean strict) {
            if (language == null || language.isEmpty()) {
                return "";
            }
            String lowercaseLanguage = language.toLowerCase(Locale.ROOT);
            if (!Locale.isValidBcp47Alpha(lowercaseLanguage, 2, 3)) {
                if (strict) {
                    throw new IllformedLocaleException("Invalid language: " + language);
                }
                return Locale.UNDETERMINED_LANGUAGE;
            }
            return lowercaseLanguage;
        }

        public Builder setLanguageTag(String languageTag) {
            if (languageTag != null && !languageTag.isEmpty()) {
                Locale fromIcu = Locale.forLanguageTag(languageTag, true);
                if (fromIcu == null) {
                    throw new IllformedLocaleException("Invalid languageTag: " + languageTag);
                }
                setLocale(fromIcu);
            } else {
                clear();
            }
            return this;
        }

        public Builder setRegion(String region) {
            this.region = normalizeAndValidateRegion(region, true);
            return this;
        }

        private static String normalizeAndValidateRegion(String region, boolean strict) {
            if (region == null || region.isEmpty()) {
                return "";
            }
            String uppercaseRegion = region.toUpperCase(Locale.ROOT);
            if (!Locale.isValidBcp47Alpha(uppercaseRegion, 2, 2) && !Locale.isUnM49AreaCode(uppercaseRegion)) {
                if (strict) {
                    throw new IllformedLocaleException("Invalid region: " + region);
                }
                return "";
            }
            return uppercaseRegion;
        }

        public Builder setVariant(String variant) {
            this.variant = normalizeAndValidateVariant(variant);
            return this;
        }

        private static String normalizeAndValidateVariant(String variant) {
            if (variant == null || variant.isEmpty()) {
                return "";
            }
            String normalizedVariant = variant.replace('-', '_');
            String[] subTags = normalizedVariant.split("_");
            for (String subTag : subTags) {
                if (!isValidVariantSubtag(subTag)) {
                    throw new IllformedLocaleException("Invalid variant: " + variant);
                }
            }
            return normalizedVariant;
        }

        private static boolean isValidVariantSubtag(String subTag) {
            char firstChar;
            if (subTag.length() >= 5 && subTag.length() <= 8) {
                if (Locale.isAsciiAlphaNum(subTag)) {
                    return true;
                }
            } else if (subTag.length() == 4 && (firstChar = subTag.charAt(0)) >= '0' && firstChar <= '9' && Locale.isAsciiAlphaNum(subTag)) {
                return true;
            }
            return false;
        }

        public Builder setScript(String script) {
            this.script = normalizeAndValidateScript(script, true);
            return this;
        }

        private static String normalizeAndValidateScript(String script, boolean strict) {
            if (script != null && !script.isEmpty()) {
                if (Locale.isValidBcp47Alpha(script, 4, 4)) {
                    return Locale.titleCaseAsciiWord(script);
                }
                if (strict) {
                    throw new IllformedLocaleException("Invalid script: " + script);
                }
                return "";
            }
            return "";
        }

        public Builder setLocale(Locale locale) {
            if (locale == null) {
                throw new NullPointerException("locale == null");
            }
            String backupLanguage = this.language;
            String backupRegion = this.region;
            String backupVariant = this.variant;
            try {
                setLanguage(locale.getLanguage());
                setRegion(locale.getCountry());
                setVariant(locale.getVariant());
                this.script = locale.getScript();
                this.extensions.clear();
                this.extensions.putAll(locale.extensions);
                this.keywords.clear();
                this.keywords.putAll(locale.unicodeKeywords);
                this.attributes.clear();
                this.attributes.addAll(locale.unicodeAttributes);
                return this;
            } catch (IllformedLocaleException ifle) {
                this.language = backupLanguage;
                this.region = backupRegion;
                this.variant = backupVariant;
                throw ifle;
            }
        }

        public Builder addUnicodeLocaleAttribute(String attribute) {
            if (attribute == null) {
                throw new NullPointerException("attribute == null");
            }
            String lowercaseAttribute = attribute.toLowerCase(Locale.ROOT);
            if (!Locale.isValidBcp47Alphanum(lowercaseAttribute, 3, 8)) {
                throw new IllformedLocaleException("Invalid locale attribute: " + attribute);
            }
            this.attributes.add(lowercaseAttribute);
            return this;
        }

        public Builder removeUnicodeLocaleAttribute(String attribute) {
            if (attribute == null) {
                throw new NullPointerException("attribute == null");
            }
            String lowercaseAttribute = attribute.toLowerCase(Locale.ROOT);
            if (!Locale.isValidBcp47Alphanum(lowercaseAttribute, 3, 8)) {
                throw new IllformedLocaleException("Invalid locale attribute: " + attribute);
            }
            this.attributes.remove(attribute);
            return this;
        }

        public Builder setExtension(char key, String value) {
            if (value == null || value.isEmpty()) {
                this.extensions.remove(Character.valueOf(key));
            } else {
                String normalizedValue = value.toLowerCase(Locale.ROOT).replace('_', '-');
                String[] subtags = normalizedValue.split("-");
                int minimumLength = key == 'x' ? 1 : 2;
                for (String subtag : subtags) {
                    if (!Locale.isValidBcp47Alphanum(subtag, minimumLength, 8)) {
                        throw new IllformedLocaleException("Invalid private use extension : " + value);
                    }
                }
                if (key == 'u') {
                    this.extensions.clear();
                    this.attributes.clear();
                    Locale.parseUnicodeExtension(subtags, this.keywords, this.attributes);
                } else {
                    this.extensions.put(Character.valueOf(key), normalizedValue);
                }
            }
            return this;
        }

        public Builder clearExtensions() {
            this.extensions.clear();
            this.attributes.clear();
            this.keywords.clear();
            return this;
        }

        public Builder setUnicodeLocaleKeyword(String key, String type) {
            if (key == null) {
                throw new NullPointerException("key == null");
            }
            if (type == null && this.keywords != null) {
                this.keywords.remove(key);
            } else {
                String lowerCaseKey = key.toLowerCase(Locale.ROOT);
                if (lowerCaseKey.length() != 2 || !Locale.isAsciiAlphaNum(lowerCaseKey)) {
                    throw new IllformedLocaleException("Invalid unicode locale keyword: " + key);
                }
                String lowerCaseType = type.toLowerCase(Locale.ROOT).replace("_", "-");
                if (!Locale.isValidTypeList(lowerCaseType)) {
                    throw new IllformedLocaleException("Invalid unicode locale type: " + type);
                }
                this.keywords.put(lowerCaseKey, lowerCaseType);
            }
            return this;
        }

        public Builder clear() {
            clearExtensions();
            this.script = "";
            this.variant = "";
            this.region = "";
            this.language = "";
            return this;
        }

        public Locale build() {
            return new Locale(this.language, this.region, this.variant, this.script, this.attributes, this.keywords, this.extensions, true);
        }
    }

    public static Locale forLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException("languageTag == null");
        }
        return forLanguageTag(languageTag, false);
    }

    private Locale(boolean hasValidatedFields, String lowerCaseLanguageCode, String upperCaseCountryCode) {
        this.languageCode = lowerCaseLanguageCode;
        this.countryCode = upperCaseCountryCode;
        this.variantCode = "";
        this.scriptCode = "";
        this.unicodeAttributes = Collections.EMPTY_SET;
        this.unicodeKeywords = Collections.EMPTY_MAP;
        this.extensions = Collections.EMPTY_MAP;
        this.hasValidatedFields = hasValidatedFields;
    }

    public Locale(String language) {
        this(language, "", "", "", Collections.EMPTY_SET, Collections.EMPTY_MAP, Collections.EMPTY_MAP, false);
    }

    public Locale(String language, String country) {
        this(language, country, "", "", Collections.EMPTY_SET, Collections.EMPTY_MAP, Collections.EMPTY_MAP, false);
    }

    public Locale(String language, String country, String variant, String scriptCode, Set<String> unicodeAttributes, Map<String, String> unicodeKeywords, Map<Character, String> extensions, boolean hasValidatedFields) {
        if (language == null || country == null || variant == null) {
            throw new NullPointerException("language=" + language + ",country=" + country + ",variant=" + variant);
        }
        if (hasValidatedFields) {
            this.languageCode = adjustLanguageCode(language);
            this.countryCode = country;
            this.variantCode = variant;
        } else if (language.isEmpty() && country.isEmpty()) {
            this.languageCode = "";
            this.countryCode = "";
            this.variantCode = variant;
        } else {
            this.languageCode = adjustLanguageCode(language);
            this.countryCode = country.toUpperCase(US);
            this.variantCode = variant;
        }
        this.scriptCode = scriptCode;
        if (hasValidatedFields) {
            Set<String> attribsCopy = new TreeSet<>(unicodeAttributes);
            Map<String, String> keywordsCopy = new TreeMap<>(unicodeKeywords);
            Map<Character, String> extensionsCopy = new TreeMap<>(extensions);
            addUnicodeExtensionToExtensionsMap(attribsCopy, keywordsCopy, extensionsCopy);
            this.unicodeAttributes = Collections.unmodifiableSet(attribsCopy);
            this.unicodeKeywords = Collections.unmodifiableMap(keywordsCopy);
            this.extensions = Collections.unmodifiableMap(extensionsCopy);
        } else {
            this.unicodeAttributes = unicodeAttributes;
            this.unicodeKeywords = unicodeKeywords;
            this.extensions = extensions;
        }
        this.hasValidatedFields = hasValidatedFields;
    }

    public Locale(String language, String country, String variant) {
        this(language, country, variant, "", Collections.EMPTY_SET, Collections.EMPTY_MAP, Collections.EMPTY_MAP, false);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Locale)) {
            return false;
        }
        Locale o = (Locale) object;
        return this.languageCode.equals(o.languageCode) && this.countryCode.equals(o.countryCode) && this.variantCode.equals(o.variantCode) && this.scriptCode.equals(o.scriptCode) && this.extensions.equals(o.extensions);
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableLocales();
    }

    public String getCountry() {
        return this.countryCode;
    }

    public static Locale getDefault() {
        return defaultLocale;
    }

    public final String getDisplayCountry() {
        return getDisplayCountry(getDefault());
    }

    public String getDisplayCountry(Locale locale) {
        if (!this.countryCode.isEmpty()) {
            String normalizedRegion = Builder.normalizeAndValidateRegion(this.countryCode, false);
            if (normalizedRegion.isEmpty()) {
                return this.countryCode;
            }
            String result = ICU.getDisplayCountry(this, locale);
            if (result == null) {
                return ICU.getDisplayCountry(this, getDefault());
            }
            return result;
        }
        return "";
    }

    public final String getDisplayLanguage() {
        return getDisplayLanguage(getDefault());
    }

    public String getDisplayLanguage(Locale locale) {
        if (!this.languageCode.isEmpty()) {
            String normalizedLanguage = Builder.normalizeAndValidateLanguage(this.languageCode, false);
            if (UNDETERMINED_LANGUAGE.equals(normalizedLanguage)) {
                return this.languageCode;
            }
            String result = ICU.getDisplayLanguage(this, locale);
            if (result == null) {
                return ICU.getDisplayLanguage(this, getDefault());
            }
            return result;
        }
        return "";
    }

    public final String getDisplayName() {
        return getDisplayName(getDefault());
    }

    public String getDisplayName(Locale locale) {
        int count = 0;
        StringBuilder buffer = new StringBuilder();
        if (!this.languageCode.isEmpty()) {
            String displayLanguage = getDisplayLanguage(locale);
            if (displayLanguage.isEmpty()) {
                displayLanguage = this.languageCode;
            }
            buffer.append(displayLanguage);
            count = 0 + 1;
        }
        if (!this.scriptCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            }
            String displayScript = getDisplayScript(locale);
            if (displayScript.isEmpty()) {
                displayScript = this.scriptCode;
            }
            buffer.append(displayScript);
            count++;
        }
        if (!this.countryCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            } else if (count == 2) {
                buffer.append(",");
            }
            String displayCountry = getDisplayCountry(locale);
            if (displayCountry.isEmpty()) {
                displayCountry = this.countryCode;
            }
            buffer.append(displayCountry);
            count++;
        }
        if (!this.variantCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            } else if (count == 2 || count == 3) {
                buffer.append(",");
            }
            String displayVariant = getDisplayVariant(locale);
            if (displayVariant.isEmpty()) {
                displayVariant = this.variantCode;
            }
            buffer.append(displayVariant);
            count++;
        }
        if (count > 1) {
            buffer.append(")");
        }
        return buffer.toString();
    }

    public final String getDisplayVariant() {
        return getDisplayVariant(getDefault());
    }

    public String getDisplayVariant(Locale locale) {
        if (!this.variantCode.isEmpty()) {
            try {
                Builder.normalizeAndValidateVariant(this.variantCode);
                String result = ICU.getDisplayVariant(this, locale);
                if (result == null) {
                    result = ICU.getDisplayVariant(this, getDefault());
                }
                if (result.isEmpty()) {
                    return this.variantCode;
                }
                return result;
            } catch (IllformedLocaleException e) {
                return this.variantCode;
            }
        }
        return "";
    }

    public String getISO3Country() {
        String code = ICU.getISO3Country("en-" + this.countryCode);
        if (!this.countryCode.isEmpty() && code.isEmpty()) {
            throw new MissingResourceException("No 3-letter country code for locale: " + this, "FormatData_" + this, "ShortCountry");
        }
        return code;
    }

    public String getISO3Language() {
        if (this.languageCode.isEmpty()) {
            return "";
        }
        String code = ICU.getISO3Language(this.languageCode);
        if (!this.languageCode.isEmpty() && code.isEmpty()) {
            throw new MissingResourceException("No 3-letter language code for locale: " + this, "FormatData_" + this, "ShortLanguage");
        }
        return code;
    }

    public static String[] getISOCountries() {
        return ICU.getISOCountries();
    }

    public static String[] getISOLanguages() {
        return ICU.getISOLanguages();
    }

    public String getLanguage() {
        return this.languageCode;
    }

    public String getVariant() {
        return this.variantCode;
    }

    public String getScript() {
        return this.scriptCode;
    }

    public String getDisplayScript() {
        return getDisplayScript(getDefault());
    }

    public String getDisplayScript(Locale locale) {
        if (this.scriptCode.isEmpty()) {
            return "";
        }
        String result = ICU.getDisplayScript(this, locale);
        if (result == null) {
            return ICU.getDisplayScript(this, getDefault());
        }
        return result;
    }

    public String toLanguageTag() {
        if (this.cachedLanguageTag == null) {
            this.cachedLanguageTag = makeLanguageTag();
        }
        return this.cachedLanguageTag;
    }

    private String makeLanguageTag() {
        String language;
        String region;
        String variant;
        String illFormedVariantSubtags = "";
        if (!this.hasValidatedFields) {
            language = Builder.normalizeAndValidateLanguage(this.languageCode, false);
            region = Builder.normalizeAndValidateRegion(this.countryCode, false);
            try {
                variant = Builder.normalizeAndValidateVariant(this.variantCode);
            } catch (IllformedLocaleException e) {
                String[] split = splitIllformedVariant(this.variantCode);
                variant = split[0];
                illFormedVariantSubtags = split[1];
            }
        } else {
            language = this.languageCode;
            region = this.countryCode;
            variant = this.variantCode.replace('_', '-');
        }
        if (language.isEmpty()) {
            language = UNDETERMINED_LANGUAGE;
        }
        if ("no".equals(language) && "NO".equals(region) && "NY".equals(variant)) {
            language = "nn";
            region = "NO";
            variant = "";
        }
        StringBuilder sb = new StringBuilder(16);
        sb.append(language);
        if (!this.scriptCode.isEmpty()) {
            sb.append('-');
            sb.append(this.scriptCode);
        }
        if (!region.isEmpty()) {
            sb.append('-');
            sb.append(region);
        }
        if (!variant.isEmpty()) {
            sb.append('-');
            sb.append(variant);
        }
        for (Map.Entry<Character, String> extension : this.extensions.entrySet()) {
            if (!extension.getKey().equals(Character.valueOf(PRIVATE_USE_EXTENSION))) {
                sb.append('-').append(extension.getKey());
                sb.append('-').append(extension.getValue());
            }
        }
        String privateUse = this.extensions.get(Character.valueOf(PRIVATE_USE_EXTENSION));
        if (privateUse != null) {
            sb.append("-x-");
            sb.append(privateUse);
        }
        if (!illFormedVariantSubtags.isEmpty()) {
            if (privateUse == null) {
                sb.append("-x-lvariant-");
            } else {
                sb.append('-');
            }
            sb.append(illFormedVariantSubtags);
        }
        return sb.toString();
    }

    private static String[] splitIllformedVariant(String variant) {
        String normalizedVariant = variant.replace('_', '-');
        String[] subTags = normalizedVariant.split("-");
        String[] split = {"", ""};
        int firstInvalidSubtag = subTags.length;
        int i = 0;
        while (true) {
            if (i >= subTags.length) {
                break;
            }
            if (isValidBcp47Alphanum(subTags[i], 1, 8)) {
                i++;
            } else {
                firstInvalidSubtag = i;
                break;
            }
        }
        if (firstInvalidSubtag != 0) {
            int firstIllformedSubtag = firstInvalidSubtag;
            for (int i2 = 0; i2 < firstInvalidSubtag; i2++) {
                String subTag = subTags[i2];
                if (subTag.length() >= 5 && subTag.length() <= 8) {
                    if (!isAsciiAlphaNum(subTag)) {
                        firstIllformedSubtag = i2;
                    }
                } else if (subTag.length() == 4) {
                    char firstChar = subTag.charAt(0);
                    if (firstChar < '0' || firstChar > '9' || !isAsciiAlphaNum(subTag)) {
                        firstIllformedSubtag = i2;
                    }
                } else {
                    firstIllformedSubtag = i2;
                }
            }
            split[0] = concatenateRange(subTags, 0, firstIllformedSubtag);
            split[1] = concatenateRange(subTags, firstIllformedSubtag, firstInvalidSubtag);
        }
        return split;
    }

    private static String concatenateRange(String[] array, int start, int end) {
        StringBuilder builder = new StringBuilder(32);
        for (int i = start; i < end; i++) {
            if (i != start) {
                builder.append('-');
            }
            builder.append(array[i]);
        }
        return builder.toString();
    }

    public Set<Character> getExtensionKeys() {
        return this.extensions.keySet();
    }

    public String getExtension(char extensionKey) {
        return this.extensions.get(Character.valueOf(extensionKey));
    }

    public String getUnicodeLocaleType(String keyWord) {
        return this.unicodeKeywords.get(keyWord);
    }

    public Set<String> getUnicodeLocaleAttributes() {
        return this.unicodeAttributes;
    }

    public Set<String> getUnicodeLocaleKeys() {
        return this.unicodeKeywords.keySet();
    }

    public synchronized int hashCode() {
        return this.countryCode.hashCode() + this.languageCode.hashCode() + this.variantCode.hashCode() + this.scriptCode.hashCode() + this.extensions.hashCode();
    }

    public static synchronized void setDefault(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        String languageTag = locale.toLanguageTag();
        defaultLocale = locale;
        ICU.setDefaultLocale(languageTag);
    }

    public final String toString() {
        String result = this.cachedToStringResult;
        if (result == null) {
            String result2 = toNewString(this.languageCode, this.countryCode, this.variantCode, this.scriptCode, this.extensions);
            this.cachedToStringResult = result2;
            return result2;
        }
        return result;
    }

    private static String toNewString(String languageCode, String countryCode, String variantCode, String scriptCode, Map<Character, String> extensions) {
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        boolean hasScriptOrExtensions = (scriptCode.isEmpty() && extensions.isEmpty()) ? false : true;
        if (!countryCode.isEmpty() || !variantCode.isEmpty() || hasScriptOrExtensions) {
            result.append('_');
        }
        result.append(countryCode);
        if (!variantCode.isEmpty() || hasScriptOrExtensions) {
            result.append('_');
        }
        result.append(variantCode);
        if (hasScriptOrExtensions) {
            if (!variantCode.isEmpty()) {
                result.append('_');
            }
            result.append("#");
            if (!scriptCode.isEmpty()) {
                result.append(scriptCode);
            }
            if (!extensions.isEmpty()) {
                if (!scriptCode.isEmpty()) {
                    result.append('-');
                }
                result.append(serializeExtensions(extensions));
            }
        }
        return result.toString();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("country", this.countryCode);
        fields.put("hashcode", -1);
        fields.put("language", this.languageCode);
        fields.put("variant", this.variantCode);
        fields.put("script", this.scriptCode);
        if (!this.extensions.isEmpty()) {
            fields.put("extensions", serializeExtensions(this.extensions));
        }
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        this.countryCode = (String) fields.get("country", "");
        this.languageCode = (String) fields.get("language", "");
        this.variantCode = (String) fields.get("variant", "");
        this.scriptCode = (String) fields.get("script", "");
        this.unicodeKeywords = Collections.EMPTY_MAP;
        this.unicodeAttributes = Collections.EMPTY_SET;
        this.extensions = Collections.EMPTY_MAP;
        String extensions = (String) fields.get("extensions", (Object) null);
        if (extensions != null) {
            readExtensions(extensions);
        }
    }

    private void readExtensions(String extensions) {
        Map<Character, String> extensionsMap = new TreeMap<>();
        parseSerializedExtensions(extensions, extensionsMap);
        this.extensions = Collections.unmodifiableMap(extensionsMap);
        if (extensionsMap.containsKey(Character.valueOf(UNICODE_LOCALE_EXTENSION))) {
            String unicodeExtension = extensionsMap.get(Character.valueOf(UNICODE_LOCALE_EXTENSION));
            String[] subTags = unicodeExtension.split("-");
            Map<String, String> unicodeKeywords = new TreeMap<>();
            Set<String> unicodeAttributes = new TreeSet<>();
            parseUnicodeExtension(subTags, unicodeKeywords, unicodeAttributes);
            this.unicodeKeywords = Collections.unmodifiableMap(unicodeKeywords);
            this.unicodeAttributes = Collections.unmodifiableSet(unicodeAttributes);
        }
    }

    public static String serializeExtensions(Map<Character, String> extensionsMap) {
        Iterator<Map.Entry<Character, String>> entryIterator = extensionsMap.entrySet().iterator();
        StringBuilder sb = new StringBuilder(64);
        while (true) {
            Map.Entry<Character, String> entry = entryIterator.next();
            sb.append(entry.getKey());
            sb.append('-');
            sb.append(entry.getValue());
            if (entryIterator.hasNext()) {
                sb.append('-');
            } else {
                return sb.toString();
            }
        }
    }

    public static void parseSerializedExtensions(String extString, Map<Character, String> outputMap) {
        int count;
        String[] subTags = extString.split("-");
        int[] typeStartIndices = new int[subTags.length / 2];
        int length = 0;
        int len$ = subTags.length;
        int i$ = 0;
        int count2 = 0;
        while (i$ < len$) {
            String subTag = subTags[i$];
            if (subTag.length() > 0) {
                length += subTag.length() + 1;
            }
            if (subTag.length() == 1) {
                count = count2 + 1;
                typeStartIndices[count2] = length;
            } else {
                count = count2;
            }
            i$++;
            count2 = count;
        }
        int i = 0;
        while (i < count2) {
            int valueStart = typeStartIndices[i];
            int valueEnd = i == count2 + (-1) ? extString.length() : typeStartIndices[i + 1] - 3;
            outputMap.put(Character.valueOf(extString.charAt(typeStartIndices[i] - 2)), extString.substring(valueStart, valueEnd));
            i++;
        }
    }

    private static boolean isUnM49AreaCode(String code) {
        if (code.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char character = code.charAt(i);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiAlphaNum(String string) {
        for (int i = 0; i < string.length(); i++) {
            char character = string.charAt(i);
            if ((character < 'a' || character > 'z') && ((character < 'A' || character > 'Z') && (character < '0' || character > '9'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidBcp47Alpha(String string, int lowerBound, int upperBound) {
        int length = string.length();
        if (length < lowerBound || length > upperBound) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char character = string.charAt(i);
            if ((character < 'a' || character > 'z') && (character < 'A' || character > 'Z')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidBcp47Alphanum(String attributeOrType, int lowerBound, int upperBound) {
        if (attributeOrType.length() < lowerBound || attributeOrType.length() > upperBound) {
            return false;
        }
        return isAsciiAlphaNum(attributeOrType);
    }

    private static String titleCaseAsciiWord(String word) {
        try {
            byte[] chars = word.toLowerCase(ROOT).getBytes(StandardCharsets.US_ASCII);
            chars[0] = (byte) ((chars[0] + 65) - 97);
            return new String(chars, StandardCharsets.US_ASCII);
        } catch (UnsupportedOperationException uoe) {
            throw new AssertionError(uoe);
        }
    }

    private static boolean isValidTypeList(String lowerCaseTypeList) {
        String[] splitList = lowerCaseTypeList.split("-");
        for (String type : splitList) {
            if (!isValidBcp47Alphanum(type, 3, 8)) {
                return false;
            }
        }
        return true;
    }

    private static void addUnicodeExtensionToExtensionsMap(Set<String> attributes, Map<String, String> keywords, Map<Character, String> extensions) {
        if (!attributes.isEmpty() || !keywords.isEmpty()) {
            StringBuilder sb = new StringBuilder(32);
            if (!attributes.isEmpty()) {
                Iterator<String> attributesIterator = attributes.iterator();
                while (true) {
                    sb.append(attributesIterator.next());
                    if (!attributesIterator.hasNext()) {
                        break;
                    } else {
                        sb.append('-');
                    }
                }
            }
            if (!keywords.isEmpty()) {
                if (!attributes.isEmpty()) {
                    sb.append('-');
                }
                Iterator<Map.Entry<String, String>> keywordsIterator = keywords.entrySet().iterator();
                while (true) {
                    Map.Entry<String, String> keyWord = keywordsIterator.next();
                    sb.append(keyWord.getKey());
                    if (!keyWord.getValue().isEmpty()) {
                        sb.append('-');
                        sb.append(keyWord.getValue());
                    }
                    if (!keywordsIterator.hasNext()) {
                        break;
                    } else {
                        sb.append('-');
                    }
                }
            }
            extensions.put(Character.valueOf(UNICODE_LOCALE_EXTENSION), sb.toString());
        }
    }

    public static void parseUnicodeExtension(String[] subtags, Map<String, String> keywords, Set<String> attributes) {
        String lastKeyword = null;
        List<String> subtagsForKeyword = new ArrayList<>();
        for (String subtag : subtags) {
            if (subtag.length() == 2) {
                if (subtagsForKeyword.size() > 0) {
                    keywords.put(lastKeyword, joinBcp47Subtags(subtagsForKeyword));
                    subtagsForKeyword.clear();
                }
                lastKeyword = subtag;
            } else if (subtag.length() > 2) {
                if (lastKeyword == null) {
                    attributes.add(subtag);
                } else {
                    subtagsForKeyword.add(subtag);
                }
            }
        }
        if (subtagsForKeyword.size() > 0) {
            keywords.put(lastKeyword, joinBcp47Subtags(subtagsForKeyword));
        } else if (lastKeyword != null) {
            keywords.put(lastKeyword, "");
        }
    }

    private static String joinBcp47Subtags(List<String> strings) {
        int size = strings.size();
        StringBuilder sb = new StringBuilder(strings.get(0).length());
        for (int i = 0; i < size; i++) {
            sb.append(strings.get(i));
            if (i != size - 1) {
                sb.append('-');
            }
        }
        return sb.toString();
    }

    public static String adjustLanguageCode(String languageCode) {
        String adjusted = languageCode.toLowerCase(US);
        if (languageCode.equals("he")) {
            return "iw";
        }
        if (languageCode.equals("id")) {
            return "in";
        }
        if (languageCode.equals("yi")) {
            return "ji";
        }
        return adjusted;
    }

    private static String convertGrandfatheredTag(String original) {
        String converted = GRANDFATHERED_LOCALES.get(original);
        return converted != null ? converted : original;
    }

    private static void extractVariantSubtags(String[] subtags, int startIndex, int endIndex, List<String> normalizedVariants) {
        for (int i = startIndex; i < endIndex; i++) {
            String subtag = subtags[i];
            if (Builder.isValidVariantSubtag(subtag)) {
                normalizedVariants.add(subtag);
            } else {
                return;
            }
        }
    }

    private static int extractExtensions(String[] subtags, int startIndex, int endIndex, Map<Character, String> extensions) {
        int privateUseExtensionIndex = -1;
        int extensionKeyIndex = -1;
        int i = startIndex;
        while (i < endIndex) {
            String subtag = subtags[i];
            boolean parsingPrivateUse = privateUseExtensionIndex != -1 && extensionKeyIndex == privateUseExtensionIndex;
            if (subtag.length() == 1 && !parsingPrivateUse) {
                if (extensionKeyIndex != -1) {
                    if (i - 1 != extensionKeyIndex) {
                        String key = subtags[extensionKeyIndex];
                        if (!extensions.containsKey(Character.valueOf(key.charAt(0)))) {
                            String value = concatenateRange(subtags, extensionKeyIndex + 1, i);
                            extensions.put(Character.valueOf(key.charAt(0)), value.toLowerCase(ROOT));
                        } else {
                            int i2 = extensionKeyIndex;
                            return i2;
                        }
                    } else {
                        int i3 = extensionKeyIndex;
                        return i3;
                    }
                }
                extensionKeyIndex = i;
                if ("x".equals(subtag)) {
                    privateUseExtensionIndex = i;
                } else if (privateUseExtensionIndex != -1) {
                    int i4 = privateUseExtensionIndex;
                    return i4;
                }
            } else {
                if (extensionKeyIndex == -1) {
                    return i;
                }
                if (!isValidBcp47Alphanum(subtag, parsingPrivateUse ? 1 : 2, 8)) {
                    return i;
                }
            }
            i++;
        }
        if (extensionKeyIndex != -1) {
            if (i - 1 != extensionKeyIndex) {
                String key2 = subtags[extensionKeyIndex];
                if (!extensions.containsKey(Character.valueOf(key2.charAt(0)))) {
                    String value2 = concatenateRange(subtags, extensionKeyIndex + 1, i);
                    extensions.put(Character.valueOf(key2.charAt(0)), value2.toLowerCase(ROOT));
                    return i;
                }
                int i5 = extensionKeyIndex;
                return i5;
            }
            int i6 = extensionKeyIndex;
            return i6;
        }
        return i;
    }

    private static Locale forLanguageTag(String tag, boolean strict) {
        String scriptCode;
        int nextSubtag;
        String regionCode;
        List<String> variants;
        Map<Character, String> extensions;
        String variantCode;
        int i;
        String converted = convertGrandfatheredTag(tag);
        String[] subtags = converted.split("-");
        int lastSubtag = subtags.length;
        for (int i2 = 0; i2 < subtags.length; i2++) {
            String subtag = subtags[i2];
            if (subtag.isEmpty() || subtag.length() > 8) {
                if (strict) {
                    throw new IllformedLocaleException("Invalid subtag at index: " + i2 + " in tag: " + tag);
                }
                lastSubtag = i2 - 1;
                String languageCode = Builder.normalizeAndValidateLanguage(subtags[0], strict);
                scriptCode = "";
                nextSubtag = 1;
                if (lastSubtag > 1) {
                    scriptCode = Builder.normalizeAndValidateScript(subtags[1], false);
                    if (!scriptCode.isEmpty()) {
                        nextSubtag = 1 + 1;
                    }
                }
                regionCode = "";
                if (lastSubtag > nextSubtag) {
                    regionCode = Builder.normalizeAndValidateRegion(subtags[nextSubtag], false);
                    if (!regionCode.isEmpty()) {
                        nextSubtag++;
                    }
                }
                variants = null;
                if (lastSubtag > nextSubtag) {
                    variants = new ArrayList<>();
                    extractVariantSubtags(subtags, nextSubtag, lastSubtag, variants);
                    nextSubtag += variants.size();
                }
                extensions = Collections.EMPTY_MAP;
                if (lastSubtag > nextSubtag) {
                    extensions = new TreeMap<>();
                    nextSubtag = extractExtensions(subtags, nextSubtag, lastSubtag, extensions);
                }
                if (nextSubtag == lastSubtag && strict) {
                    throw new IllformedLocaleException("Unparseable subtag: " + subtags[nextSubtag] + " from language tag: " + tag);
                }
                Set<String> unicodeKeywords = Collections.EMPTY_SET;
                Map<String, String> unicodeAttributes = Collections.EMPTY_MAP;
                if (extensions.containsKey(Character.valueOf(UNICODE_LOCALE_EXTENSION))) {
                    unicodeKeywords = new TreeSet<>();
                    unicodeAttributes = new TreeMap<>();
                    parseUnicodeExtension(extensions.get(Character.valueOf(UNICODE_LOCALE_EXTENSION)).split("-"), unicodeAttributes, unicodeKeywords);
                }
                variantCode = "";
                if (variants != null && !variants.isEmpty()) {
                    StringBuilder variantsBuilder = new StringBuilder(variants.size() * 8);
                    for (i = 0; i < variants.size(); i++) {
                        if (i != 0) {
                            variantsBuilder.append('_');
                        }
                        variantsBuilder.append(variants.get(i));
                    }
                    variantCode = variantsBuilder.toString();
                }
                return new Locale(languageCode, regionCode, variantCode, scriptCode, unicodeKeywords, unicodeAttributes, extensions, true);
            }
        }
        String languageCode2 = Builder.normalizeAndValidateLanguage(subtags[0], strict);
        scriptCode = "";
        nextSubtag = 1;
        if (lastSubtag > 1) {
        }
        regionCode = "";
        if (lastSubtag > nextSubtag) {
        }
        variants = null;
        if (lastSubtag > nextSubtag) {
        }
        extensions = Collections.EMPTY_MAP;
        if (lastSubtag > nextSubtag) {
        }
        if (nextSubtag == lastSubtag) {
        }
        Set<String> unicodeKeywords2 = Collections.EMPTY_SET;
        Map<String, String> unicodeAttributes2 = Collections.EMPTY_MAP;
        if (extensions.containsKey(Character.valueOf(UNICODE_LOCALE_EXTENSION))) {
        }
        variantCode = "";
        if (variants != null) {
            StringBuilder variantsBuilder2 = new StringBuilder(variants.size() * 8);
            while (i < variants.size()) {
            }
            variantCode = variantsBuilder2.toString();
        }
        return new Locale(languageCode2, regionCode, variantCode, scriptCode, unicodeKeywords2, unicodeAttributes2, extensions, true);
    }
}
