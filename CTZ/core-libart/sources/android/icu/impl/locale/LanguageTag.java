package android.icu.impl.locale;

import android.icu.impl.locale.AsciiUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageTag {
    static final boolean $assertionsDisabled = false;
    private static final boolean JDKIMPL = false;
    public static final String PRIVATEUSE = "x";
    public static final String PRIVUSE_VARIANT_PREFIX = "lvariant";
    public static final String SEP = "-";
    public static String UNDETERMINED = "und";
    private static final Map<AsciiUtil.CaseInsensitiveKey, String[]> GRANDFATHERED = new HashMap();
    private String _language = "";
    private String _script = "";
    private String _region = "";
    private String _privateuse = "";
    private List<String> _extlangs = Collections.emptyList();
    private List<String> _variants = Collections.emptyList();
    private List<String> _extensions = Collections.emptyList();

    static {
        for (String[] strArr : new String[][]{new String[]{"art-lojban", "jbo"}, new String[]{"cel-gaulish", "xtg-x-cel-gaulish"}, new String[]{"en-GB-oed", "en-GB-x-oed"}, new String[]{"i-ami", "ami"}, new String[]{"i-bnn", "bnn"}, new String[]{"i-default", "en-x-i-default"}, new String[]{"i-enochian", "und-x-i-enochian"}, new String[]{"i-hak", "hak"}, new String[]{"i-klingon", "tlh"}, new String[]{"i-lux", "lb"}, new String[]{"i-mingo", "see-x-i-mingo"}, new String[]{"i-navajo", "nv"}, new String[]{"i-pwn", "pwn"}, new String[]{"i-tao", "tao"}, new String[]{"i-tay", "tay"}, new String[]{"i-tsu", "tsu"}, new String[]{"no-bok", "nb"}, new String[]{"no-nyn", "nn"}, new String[]{"sgn-BE-FR", "sfb"}, new String[]{"sgn-BE-NL", "vgt"}, new String[]{"sgn-CH-DE", "sgg"}, new String[]{"zh-guoyu", "cmn"}, new String[]{"zh-hakka", "hak"}, new String[]{"zh-min", "nan-x-zh-min"}, new String[]{"zh-min-nan", "nan"}, new String[]{"zh-xiang", "hsn"}}) {
            GRANDFATHERED.put(new AsciiUtil.CaseInsensitiveKey(strArr[0]), strArr);
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
        String[] strArr = GRANDFATHERED.get(new AsciiUtil.CaseInsensitiveKey(str));
        boolean z = true;
        if (strArr != null) {
            stringTokenIterator = new StringTokenIterator(strArr[1], SEP);
        } else {
            z = false;
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
        if (z) {
            parseStatus._parseLength = str.length();
        } else if (!stringTokenIterator.isDone() && !parseStatus.isError()) {
            String strCurrent = stringTokenIterator.current();
            parseStatus._errorIndex = stringTokenIterator.currentStart();
            if (strCurrent.length() == 0) {
                parseStatus._errorMsg = "Empty subtag";
            } else {
                parseStatus._errorMsg = "Invalid subtag: " + strCurrent;
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
        this._language = strCurrent;
        parseStatus._parseLength = stringTokenIterator.currentEnd();
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
            if (this._extlangs.isEmpty()) {
                this._extlangs = new ArrayList(3);
            }
            this._extlangs.add(strCurrent);
            parseStatus._parseLength = stringTokenIterator.currentEnd();
            stringTokenIterator.next();
            if (this._extlangs.size() == 3) {
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
        this._script = strCurrent;
        parseStatus._parseLength = stringTokenIterator.currentEnd();
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
        this._region = strCurrent;
        parseStatus._parseLength = stringTokenIterator.currentEnd();
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
            if (this._variants.isEmpty()) {
                this._variants = new ArrayList(3);
            }
            this._variants.add(strCurrent);
            parseStatus._parseLength = stringTokenIterator.currentEnd();
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
                    parseStatus._parseLength = stringTokenIterator.currentEnd();
                    stringTokenIterator.next();
                }
                if (parseStatus._parseLength <= iCurrentStart) {
                    parseStatus._errorIndex = iCurrentStart;
                    parseStatus._errorMsg = "Incomplete extension '" + strCurrent + "'";
                    break;
                }
                if (this._extensions.size() == 0) {
                    this._extensions = new ArrayList(4);
                }
                this._extensions.add(sb.toString());
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
            parseStatus._parseLength = stringTokenIterator.currentEnd();
            stringTokenIterator.next();
        }
        if (parseStatus._parseLength <= iCurrentStart) {
            parseStatus._errorIndex = iCurrentStart;
            parseStatus._errorMsg = "Incomplete privateuse";
            return false;
        }
        this._privateuse = sb.toString();
        return true;
    }

    public static LanguageTag parseLocale(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        boolean z;
        String string;
        LanguageTag languageTag = new LanguageTag();
        String language = baseLocale.getLanguage();
        String script = baseLocale.getScript();
        String region = baseLocale.getRegion();
        String variant = baseLocale.getVariant();
        if (language.length() > 0 && isLanguage(language)) {
            if (language.equals("iw")) {
                language = "he";
            } else if (language.equals("ji")) {
                language = "yi";
            } else if (language.equals("in")) {
                language = "id";
            }
            languageTag._language = language;
        }
        if (script.length() <= 0 || !isScript(script)) {
            z = false;
        } else {
            languageTag._script = canonicalizeScript(script);
            z = true;
        }
        if (region.length() > 0 && isRegion(region)) {
            languageTag._region = canonicalizeRegion(region);
            z = true;
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
                arrayList2.add(canonicalizeVariant(strCurrent));
                stringTokenIterator.next();
            }
            if (arrayList2 != null) {
                languageTag._variants = arrayList2;
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
                    sb.append(AsciiUtil.toLowerString(strCurrent2));
                    stringTokenIterator.next();
                }
                if (sb.length() > 0) {
                    string = sb.toString();
                } else {
                    string = null;
                }
            }
        }
        String value = null;
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
        if (arrayList != null) {
            languageTag._extensions = arrayList;
            z = true;
        }
        if (string != null) {
            value = value == null ? "lvariant-" + string : value + SEP + PRIVUSE_VARIANT_PREFIX + SEP + string.replace(BaseLocale.SEP, SEP);
        }
        if (value != null) {
            languageTag._privateuse = value;
        }
        if (languageTag._language.length() == 0 && (z || value == null)) {
            languageTag._language = UNDETERMINED;
        }
        return languageTag;
    }

    public String getLanguage() {
        return this._language;
    }

    public List<String> getExtlangs() {
        return Collections.unmodifiableList(this._extlangs);
    }

    public String getScript() {
        return this._script;
    }

    public String getRegion() {
        return this._region;
    }

    public List<String> getVariants() {
        return Collections.unmodifiableList(this._variants);
    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(this._extensions);
    }

    public String getPrivateuse() {
        return this._privateuse;
    }

    public static boolean isLanguage(String str) {
        return str.length() >= 2 && str.length() <= 8 && AsciiUtil.isAlphaString(str);
    }

    public static boolean isExtlang(String str) {
        return str.length() == 3 && AsciiUtil.isAlphaString(str);
    }

    public static boolean isScript(String str) {
        return str.length() == 4 && AsciiUtil.isAlphaString(str);
    }

    public static boolean isRegion(String str) {
        return (str.length() == 2 && AsciiUtil.isAlphaString(str)) || (str.length() == 3 && AsciiUtil.isNumericString(str));
    }

    public static boolean isVariant(String str) {
        int length = str.length();
        if (length >= 5 && length <= 8) {
            return AsciiUtil.isAlphaNumericString(str);
        }
        if (length == 4) {
            return AsciiUtil.isNumeric(str.charAt(0)) && AsciiUtil.isAlphaNumeric(str.charAt(1)) && AsciiUtil.isAlphaNumeric(str.charAt(2)) && AsciiUtil.isAlphaNumeric(str.charAt(3));
        }
        return false;
    }

    public static boolean isExtensionSingleton(String str) {
        return str.length() == 1 && AsciiUtil.isAlphaString(str) && !AsciiUtil.caseIgnoreMatch(PRIVATEUSE, str);
    }

    public static boolean isExtensionSingletonChar(char c) {
        return isExtensionSingleton(String.valueOf(c));
    }

    public static boolean isExtensionSubtag(String str) {
        return str.length() >= 2 && str.length() <= 8 && AsciiUtil.isAlphaNumericString(str);
    }

    public static boolean isPrivateusePrefix(String str) {
        return str.length() == 1 && AsciiUtil.caseIgnoreMatch(PRIVATEUSE, str);
    }

    public static boolean isPrivateusePrefixChar(char c) {
        return AsciiUtil.caseIgnoreMatch(PRIVATEUSE, String.valueOf(c));
    }

    public static boolean isPrivateuseSubtag(String str) {
        return str.length() >= 1 && str.length() <= 8 && AsciiUtil.isAlphaNumericString(str);
    }

    public static String canonicalizeLanguage(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizeExtlang(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizeScript(String str) {
        return AsciiUtil.toTitleString(str);
    }

    public static String canonicalizeRegion(String str) {
        return AsciiUtil.toUpperString(str);
    }

    public static String canonicalizeVariant(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizeExtension(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizeExtensionSingleton(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizeExtensionSubtag(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizePrivateuse(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public static String canonicalizePrivateuseSubtag(String str) {
        return AsciiUtil.toLowerString(str);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this._language.length() > 0) {
            sb.append(this._language);
            for (String str : this._extlangs) {
                sb.append(SEP);
                sb.append(str);
            }
            if (this._script.length() > 0) {
                sb.append(SEP);
                sb.append(this._script);
            }
            if (this._region.length() > 0) {
                sb.append(SEP);
                sb.append(this._region);
            }
            for (String str2 : this._variants) {
                sb.append(SEP);
                sb.append(str2);
            }
            for (String str3 : this._extensions) {
                sb.append(SEP);
                sb.append(str3);
            }
        }
        if (this._privateuse.length() > 0) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(this._privateuse);
        }
        return sb.toString();
    }
}
