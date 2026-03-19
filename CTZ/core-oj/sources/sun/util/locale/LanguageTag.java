package sun.util.locale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.security.x509.PolicyInformation;

public class LanguageTag {
    private static final Map<String, String[]> GRANDFATHERED = new HashMap();
    public static final String PRIVATEUSE = "x";
    public static final String PRIVUSE_VARIANT_PREFIX = "lvariant";
    public static final String SEP = "-";
    public static final String UNDETERMINED = "und";
    private String language = "";
    private String script = "";
    private String region = "";
    private String privateuse = "";
    private List<String> extlangs = Collections.emptyList();
    private List<String> variants = Collections.emptyList();
    private List<String> extensions = Collections.emptyList();

    static {
        for (String[] strArr : new String[][]{new String[]{"art-lojban", "jbo"}, new String[]{"cel-gaulish", "xtg-x-cel-gaulish"}, new String[]{"en-GB-oed", "en-GB-x-oed"}, new String[]{"i-ami", "ami"}, new String[]{"i-bnn", "bnn"}, new String[]{"i-default", "en-x-i-default"}, new String[]{"i-enochian", "und-x-i-enochian"}, new String[]{"i-hak", "hak"}, new String[]{"i-klingon", "tlh"}, new String[]{"i-lux", "lb"}, new String[]{"i-mingo", "see-x-i-mingo"}, new String[]{"i-navajo", "nv"}, new String[]{"i-pwn", "pwn"}, new String[]{"i-tao", "tao"}, new String[]{"i-tay", "tay"}, new String[]{"i-tsu", "tsu"}, new String[]{"no-bok", "nb"}, new String[]{"no-nyn", "nn"}, new String[]{"sgn-BE-FR", "sfb"}, new String[]{"sgn-BE-NL", "vgt"}, new String[]{"sgn-CH-DE", "sgg"}, new String[]{"zh-guoyu", "cmn"}, new String[]{"zh-hakka", "hak"}, new String[]{"zh-min", "nan-x-zh-min"}, new String[]{"zh-min-nan", "nan"}, new String[]{"zh-xiang", "hsn"}}) {
            GRANDFATHERED.put(LocaleUtils.toLowerString(strArr[0]), strArr);
        }
    }

    private LanguageTag() {
    }

    public static LanguageTag parse(String str, ParseStatus parseStatus) {
        StringTokenIterator stringTokenIterator;
        if (parseStatus == null) {
            parseStatus = new ParseStatus();
        } else {
            parseStatus.reset();
        }
        String[] strArr = GRANDFATHERED.get(LocaleUtils.toLowerString(str));
        if (strArr != null) {
            stringTokenIterator = new StringTokenIterator(strArr[1], SEP);
        } else {
            stringTokenIterator = new StringTokenIterator(str, SEP);
        }
        LanguageTag languageTag = new LanguageTag();
        if (languageTag.parseLanguage(stringTokenIterator, parseStatus)) {
            languageTag.parseExtlangs(stringTokenIterator, parseStatus);
            languageTag.parseScript(stringTokenIterator, parseStatus);
            languageTag.parseRegion(stringTokenIterator, parseStatus);
            languageTag.parseVariants(stringTokenIterator, parseStatus);
            languageTag.parseExtensions(stringTokenIterator, parseStatus);
        }
        languageTag.parsePrivateuse(stringTokenIterator, parseStatus);
        if (!stringTokenIterator.isDone() && !parseStatus.isError()) {
            String strCurrent = stringTokenIterator.current();
            parseStatus.errorIndex = stringTokenIterator.currentStart();
            if (strCurrent.length() == 0) {
                parseStatus.errorMsg = "Empty subtag";
            } else {
                parseStatus.errorMsg = "Invalid subtag: " + strCurrent;
            }
        }
        return languageTag;
    }

    private boolean parseLanguage(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        String strCurrent = stringTokenIterator.current();
        if (!isLanguage(strCurrent)) {
            return false;
        }
        this.language = strCurrent;
        parseStatus.parseLength = stringTokenIterator.currentEnd();
        stringTokenIterator.next();
        return true;
    }

    private boolean parseExtlangs(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        boolean z = false;
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        while (!stringTokenIterator.isDone()) {
            String strCurrent = stringTokenIterator.current();
            if (!isExtlang(strCurrent)) {
                break;
            }
            z = true;
            if (this.extlangs.isEmpty()) {
                this.extlangs = new ArrayList(3);
            }
            this.extlangs.add(strCurrent);
            parseStatus.parseLength = stringTokenIterator.currentEnd();
            stringTokenIterator.next();
            if (this.extlangs.size() == 3) {
                break;
            }
        }
        return z;
    }

    private boolean parseScript(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        String strCurrent = stringTokenIterator.current();
        if (!isScript(strCurrent)) {
            return false;
        }
        this.script = strCurrent;
        parseStatus.parseLength = stringTokenIterator.currentEnd();
        stringTokenIterator.next();
        return true;
    }

    private boolean parseRegion(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        String strCurrent = stringTokenIterator.current();
        if (!isRegion(strCurrent)) {
            return false;
        }
        this.region = strCurrent;
        parseStatus.parseLength = stringTokenIterator.currentEnd();
        stringTokenIterator.next();
        return true;
    }

    private boolean parseVariants(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        boolean z = false;
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        while (!stringTokenIterator.isDone()) {
            String strCurrent = stringTokenIterator.current();
            if (!isVariant(strCurrent)) {
                break;
            }
            z = true;
            if (this.variants.isEmpty()) {
                this.variants = new ArrayList(3);
            }
            this.variants.add(strCurrent);
            parseStatus.parseLength = stringTokenIterator.currentEnd();
            stringTokenIterator.next();
        }
        return z;
    }

    private boolean parseExtensions(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        boolean z = false;
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        while (true) {
            if (!stringTokenIterator.isDone()) {
                String strCurrent = stringTokenIterator.current();
                if (!isExtensionSingleton(strCurrent)) {
                    break;
                }
                int iCurrentStart = stringTokenIterator.currentStart();
                StringBuilder sb = new StringBuilder(strCurrent);
                stringTokenIterator.next();
                while (!stringTokenIterator.isDone()) {
                    String strCurrent2 = stringTokenIterator.current();
                    if (!isExtensionSubtag(strCurrent2)) {
                        break;
                    }
                    sb.append(SEP);
                    sb.append(strCurrent2);
                    parseStatus.parseLength = stringTokenIterator.currentEnd();
                    stringTokenIterator.next();
                }
                if (parseStatus.parseLength <= iCurrentStart) {
                    parseStatus.errorIndex = iCurrentStart;
                    parseStatus.errorMsg = "Incomplete extension '" + strCurrent + "'";
                    break;
                }
                if (this.extensions.isEmpty()) {
                    this.extensions = new ArrayList(4);
                }
                this.extensions.add(sb.toString());
                z = true;
            } else {
                break;
            }
        }
        return z;
    }

    private boolean parsePrivateuse(StringTokenIterator stringTokenIterator, ParseStatus parseStatus) {
        if (stringTokenIterator.isDone() || parseStatus.isError()) {
            return false;
        }
        String strCurrent = stringTokenIterator.current();
        if (!isPrivateusePrefix(strCurrent)) {
            return false;
        }
        int iCurrentStart = stringTokenIterator.currentStart();
        StringBuilder sb = new StringBuilder(strCurrent);
        stringTokenIterator.next();
        while (!stringTokenIterator.isDone()) {
            String strCurrent2 = stringTokenIterator.current();
            if (!isPrivateuseSubtag(strCurrent2)) {
                break;
            }
            sb.append(SEP);
            sb.append(strCurrent2);
            parseStatus.parseLength = stringTokenIterator.currentEnd();
            stringTokenIterator.next();
        }
        if (parseStatus.parseLength <= iCurrentStart) {
            parseStatus.errorIndex = iCurrentStart;
            parseStatus.errorMsg = "Incomplete privateuse";
            return false;
        }
        this.privateuse = sb.toString();
        return true;
    }

    public static LanguageTag parseLocale(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        boolean z;
        String string;
        String value;
        LanguageTag languageTag = new LanguageTag();
        String language = baseLocale.getLanguage();
        String script = baseLocale.getScript();
        String region = baseLocale.getRegion();
        String variant = baseLocale.getVariant();
        if (isLanguage(language)) {
            if (language.equals("iw")) {
                language = "he";
            } else if (language.equals("ji")) {
                language = "yi";
            } else if (language.equals("in")) {
                language = PolicyInformation.ID;
            }
            languageTag.language = language;
        }
        if (isScript(script)) {
            languageTag.script = canonicalizeScript(script);
            z = true;
        } else {
            z = false;
        }
        if (isRegion(region)) {
            languageTag.region = canonicalizeRegion(region);
            z = true;
        }
        if (languageTag.language.equals("no") && languageTag.region.equals("NO") && variant.equals("NY")) {
            languageTag.language = "nn";
            variant = "";
        }
        ArrayList arrayList = null;
        if (variant.length() > 0) {
            StringTokenIterator stringTokenIterator = new StringTokenIterator(variant, BaseLocale.SEP);
            ArrayList arrayList2 = null;
            while (!stringTokenIterator.isDone()) {
                String strCurrent = stringTokenIterator.current();
                if (!isVariant(strCurrent)) {
                    break;
                }
                if (arrayList2 == null) {
                    arrayList2 = new ArrayList();
                }
                arrayList2.add(strCurrent);
                stringTokenIterator.next();
            }
            if (arrayList2 != null) {
                languageTag.variants = arrayList2;
                z = true;
            }
            if (!stringTokenIterator.isDone()) {
                StringBuilder sb = new StringBuilder();
                while (!stringTokenIterator.isDone()) {
                    String strCurrent2 = stringTokenIterator.current();
                    if (!isPrivateuseSubtag(strCurrent2)) {
                        break;
                    }
                    if (sb.length() > 0) {
                        sb.append(SEP);
                    }
                    sb.append(strCurrent2);
                    stringTokenIterator.next();
                }
                if (sb.length() > 0) {
                    string = sb.toString();
                } else {
                    string = null;
                }
            }
        }
        if (localeExtensions != null) {
            value = null;
            for (Character ch : localeExtensions.getKeys()) {
                Extension extension = localeExtensions.getExtension(ch);
                if (isPrivateusePrefixChar(ch.charValue())) {
                    value = extension.getValue();
                } else {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(ch.toString() + SEP + extension.getValue());
                }
            }
        } else {
            value = null;
        }
        if (arrayList != null) {
            languageTag.extensions = arrayList;
            z = true;
        }
        if (string != null) {
            value = value == null ? "lvariant-" + string : value + SEP + PRIVUSE_VARIANT_PREFIX + SEP + string.replace(BaseLocale.SEP, SEP);
        }
        if (value != null) {
            languageTag.privateuse = value;
        }
        if (languageTag.language.length() == 0 && (z || value == null)) {
            languageTag.language = UNDETERMINED;
        }
        return languageTag;
    }

    public String getLanguage() {
        return this.language;
    }

    public List<String> getExtlangs() {
        if (this.extlangs.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.extlangs);
    }

    public String getScript() {
        return this.script;
    }

    public String getRegion() {
        return this.region;
    }

    public List<String> getVariants() {
        if (this.variants.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.variants);
    }

    public List<String> getExtensions() {
        if (this.extensions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.extensions);
    }

    public String getPrivateuse() {
        return this.privateuse;
    }

    public static boolean isLanguage(String str) {
        int length = str.length();
        return length >= 2 && length <= 8 && LocaleUtils.isAlphaString(str);
    }

    public static boolean isExtlang(String str) {
        return str.length() == 3 && LocaleUtils.isAlphaString(str);
    }

    public static boolean isScript(String str) {
        return str.length() == 4 && LocaleUtils.isAlphaString(str);
    }

    public static boolean isRegion(String str) {
        return (str.length() == 2 && LocaleUtils.isAlphaString(str)) || (str.length() == 3 && LocaleUtils.isNumericString(str));
    }

    public static boolean isVariant(String str) {
        int length = str.length();
        if (length >= 5 && length <= 8) {
            return LocaleUtils.isAlphaNumericString(str);
        }
        if (length == 4) {
            return LocaleUtils.isNumeric(str.charAt(0)) && LocaleUtils.isAlphaNumeric(str.charAt(1)) && LocaleUtils.isAlphaNumeric(str.charAt(2)) && LocaleUtils.isAlphaNumeric(str.charAt(3));
        }
        return false;
    }

    public static boolean isExtensionSingleton(String str) {
        return str.length() == 1 && LocaleUtils.isAlphaString(str) && !LocaleUtils.caseIgnoreMatch(PRIVATEUSE, str);
    }

    public static boolean isExtensionSingletonChar(char c) {
        return isExtensionSingleton(String.valueOf(c));
    }

    public static boolean isExtensionSubtag(String str) {
        int length = str.length();
        return length >= 2 && length <= 8 && LocaleUtils.isAlphaNumericString(str);
    }

    public static boolean isPrivateusePrefix(String str) {
        return str.length() == 1 && LocaleUtils.caseIgnoreMatch(PRIVATEUSE, str);
    }

    public static boolean isPrivateusePrefixChar(char c) {
        return LocaleUtils.caseIgnoreMatch(PRIVATEUSE, String.valueOf(c));
    }

    public static boolean isPrivateuseSubtag(String str) {
        int length = str.length();
        return length >= 1 && length <= 8 && LocaleUtils.isAlphaNumericString(str);
    }

    public static String canonicalizeLanguage(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizeExtlang(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizeScript(String str) {
        return LocaleUtils.toTitleString(str);
    }

    public static String canonicalizeRegion(String str) {
        return LocaleUtils.toUpperString(str);
    }

    public static String canonicalizeVariant(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizeExtension(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizeExtensionSingleton(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizeExtensionSubtag(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizePrivateuse(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public static String canonicalizePrivateuseSubtag(String str) {
        return LocaleUtils.toLowerString(str);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.language.length() > 0) {
            sb.append(this.language);
            for (String str : this.extlangs) {
                sb.append(SEP);
                sb.append(str);
            }
            if (this.script.length() > 0) {
                sb.append(SEP);
                sb.append(this.script);
            }
            if (this.region.length() > 0) {
                sb.append(SEP);
                sb.append(this.region);
            }
            for (String str2 : this.variants) {
                sb.append(SEP);
                sb.append(str2);
            }
            for (String str3 : this.extensions) {
                sb.append(SEP);
                sb.append(str3);
            }
        }
        if (this.privateuse.length() > 0) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(this.privateuse);
        }
        return sb.toString();
    }
}
