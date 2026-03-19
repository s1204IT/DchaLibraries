package sun.util.locale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InternalLocaleBuilder {
    static final boolean $assertionsDisabled = false;
    private static final CaseInsensitiveChar PRIVATEUSE_KEY = new CaseInsensitiveChar(LanguageTag.PRIVATEUSE);
    private Map<CaseInsensitiveChar, String> extensions;
    private Set<CaseInsensitiveString> uattributes;
    private Map<CaseInsensitiveString, String> ukeywords;
    private String language = "";
    private String script = "";
    private String region = "";
    private String variant = "";

    public InternalLocaleBuilder setLanguage(String str) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(str)) {
            this.language = "";
        } else {
            if (!LanguageTag.isLanguage(str)) {
                throw new LocaleSyntaxException("Ill-formed language: " + str, 0);
            }
            this.language = str;
        }
        return this;
    }

    public InternalLocaleBuilder setScript(String str) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(str)) {
            this.script = "";
        } else {
            if (!LanguageTag.isScript(str)) {
                throw new LocaleSyntaxException("Ill-formed script: " + str, 0);
            }
            this.script = str;
        }
        return this;
    }

    public InternalLocaleBuilder setRegion(String str) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(str)) {
            this.region = "";
        } else {
            if (!LanguageTag.isRegion(str)) {
                throw new LocaleSyntaxException("Ill-formed region: " + str, 0);
            }
            this.region = str;
        }
        return this;
    }

    public InternalLocaleBuilder setVariant(String str) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(str)) {
            this.variant = "";
        } else {
            String strReplaceAll = str.replaceAll(LanguageTag.SEP, BaseLocale.SEP);
            int iCheckVariants = checkVariants(strReplaceAll, BaseLocale.SEP);
            if (iCheckVariants != -1) {
                throw new LocaleSyntaxException("Ill-formed variant: " + str, iCheckVariants);
            }
            this.variant = strReplaceAll;
        }
        return this;
    }

    public InternalLocaleBuilder addUnicodeLocaleAttribute(String str) throws LocaleSyntaxException {
        if (!UnicodeLocaleExtension.isAttribute(str)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale attribute: " + str);
        }
        if (this.uattributes == null) {
            this.uattributes = new HashSet(4);
        }
        this.uattributes.add(new CaseInsensitiveString(str));
        return this;
    }

    public InternalLocaleBuilder removeUnicodeLocaleAttribute(String str) throws LocaleSyntaxException {
        if (str == null || !UnicodeLocaleExtension.isAttribute(str)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale attribute: " + str);
        }
        if (this.uattributes != null) {
            this.uattributes.remove(new CaseInsensitiveString(str));
        }
        return this;
    }

    public InternalLocaleBuilder setUnicodeLocaleKeyword(String str, String str2) throws LocaleSyntaxException {
        if (!UnicodeLocaleExtension.isKey(str)) {
            throw new LocaleSyntaxException("Ill-formed Unicode locale keyword key: " + str);
        }
        CaseInsensitiveString caseInsensitiveString = new CaseInsensitiveString(str);
        if (str2 == null) {
            if (this.ukeywords != null) {
                this.ukeywords.remove(caseInsensitiveString);
            }
        } else {
            if (str2.length() != 0) {
                StringTokenIterator stringTokenIterator = new StringTokenIterator(str2.replaceAll(BaseLocale.SEP, LanguageTag.SEP), LanguageTag.SEP);
                while (!stringTokenIterator.isDone()) {
                    if (!UnicodeLocaleExtension.isTypeSubtag(stringTokenIterator.current())) {
                        throw new LocaleSyntaxException("Ill-formed Unicode locale keyword type: " + str2, stringTokenIterator.currentStart());
                    }
                    stringTokenIterator.next();
                }
            }
            if (this.ukeywords == null) {
                this.ukeywords = new HashMap(4);
            }
            this.ukeywords.put(caseInsensitiveString, str2);
        }
        return this;
    }

    public InternalLocaleBuilder setExtension(char c, String str) throws LocaleSyntaxException {
        boolean zIsExtensionSubtag;
        boolean zIsPrivateusePrefixChar = LanguageTag.isPrivateusePrefixChar(c);
        if (!zIsPrivateusePrefixChar && !LanguageTag.isExtensionSingletonChar(c)) {
            throw new LocaleSyntaxException("Ill-formed extension key: " + c);
        }
        boolean zIsEmpty = LocaleUtils.isEmpty(str);
        CaseInsensitiveChar caseInsensitiveChar = new CaseInsensitiveChar(c);
        if (zIsEmpty) {
            if (UnicodeLocaleExtension.isSingletonChar(caseInsensitiveChar.value())) {
                if (this.uattributes != null) {
                    this.uattributes.clear();
                }
                if (this.ukeywords != null) {
                    this.ukeywords.clear();
                }
            } else if (this.extensions != null && this.extensions.containsKey(caseInsensitiveChar)) {
                this.extensions.remove(caseInsensitiveChar);
            }
        } else {
            String strReplaceAll = str.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
            StringTokenIterator stringTokenIterator = new StringTokenIterator(strReplaceAll, LanguageTag.SEP);
            while (!stringTokenIterator.isDone()) {
                String strCurrent = stringTokenIterator.current();
                if (zIsPrivateusePrefixChar) {
                    zIsExtensionSubtag = LanguageTag.isPrivateuseSubtag(strCurrent);
                } else {
                    zIsExtensionSubtag = LanguageTag.isExtensionSubtag(strCurrent);
                }
                if (!zIsExtensionSubtag) {
                    throw new LocaleSyntaxException("Ill-formed extension value: " + strCurrent, stringTokenIterator.currentStart());
                }
                stringTokenIterator.next();
            }
            if (UnicodeLocaleExtension.isSingletonChar(caseInsensitiveChar.value())) {
                setUnicodeLocaleExtension(strReplaceAll);
            } else {
                if (this.extensions == null) {
                    this.extensions = new HashMap(4);
                }
                this.extensions.put(caseInsensitiveChar, strReplaceAll);
            }
        }
        return this;
    }

    public InternalLocaleBuilder setExtensions(String str) throws LocaleSyntaxException {
        if (LocaleUtils.isEmpty(str)) {
            clearExtensions();
            return this;
        }
        String strReplaceAll = str.replaceAll(BaseLocale.SEP, LanguageTag.SEP);
        StringTokenIterator stringTokenIterator = new StringTokenIterator(strReplaceAll, LanguageTag.SEP);
        int iCurrentEnd = 0;
        String string = null;
        ArrayList arrayList = null;
        while (!stringTokenIterator.isDone()) {
            String strCurrent = stringTokenIterator.current();
            if (!LanguageTag.isExtensionSingleton(strCurrent)) {
                break;
            }
            int iCurrentStart = stringTokenIterator.currentStart();
            StringBuilder sb = new StringBuilder(strCurrent);
            stringTokenIterator.next();
            while (!stringTokenIterator.isDone()) {
                String strCurrent2 = stringTokenIterator.current();
                if (!LanguageTag.isExtensionSubtag(strCurrent2)) {
                    break;
                }
                sb.append(LanguageTag.SEP);
                sb.append(strCurrent2);
                iCurrentEnd = stringTokenIterator.currentEnd();
                stringTokenIterator.next();
            }
            if (iCurrentEnd < iCurrentStart) {
                throw new LocaleSyntaxException("Incomplete extension '" + strCurrent + "'", iCurrentStart);
            }
            if (arrayList == null) {
                arrayList = new ArrayList(4);
            }
            arrayList.add(sb.toString());
        }
        if (!stringTokenIterator.isDone()) {
            String strCurrent3 = stringTokenIterator.current();
            if (LanguageTag.isPrivateusePrefix(strCurrent3)) {
                int iCurrentStart2 = stringTokenIterator.currentStart();
                StringBuilder sb2 = new StringBuilder(strCurrent3);
                stringTokenIterator.next();
                while (!stringTokenIterator.isDone()) {
                    String strCurrent4 = stringTokenIterator.current();
                    if (!LanguageTag.isPrivateuseSubtag(strCurrent4)) {
                        break;
                    }
                    sb2.append(LanguageTag.SEP);
                    sb2.append(strCurrent4);
                    iCurrentEnd = stringTokenIterator.currentEnd();
                    stringTokenIterator.next();
                }
                if (iCurrentEnd <= iCurrentStart2) {
                    throw new LocaleSyntaxException("Incomplete privateuse:" + strReplaceAll.substring(iCurrentStart2), iCurrentStart2);
                }
                string = sb2.toString();
            }
        }
        if (!stringTokenIterator.isDone()) {
            throw new LocaleSyntaxException("Ill-formed extension subtags:" + strReplaceAll.substring(stringTokenIterator.currentStart()), stringTokenIterator.currentStart());
        }
        return setExtensions(arrayList, string);
    }

    private InternalLocaleBuilder setExtensions(List<String> list, String str) {
        clearExtensions();
        if (!LocaleUtils.isEmpty(list)) {
            HashSet hashSet = new HashSet(list.size());
            for (String str2 : list) {
                CaseInsensitiveChar caseInsensitiveChar = new CaseInsensitiveChar(str2);
                if (!hashSet.contains(caseInsensitiveChar)) {
                    if (UnicodeLocaleExtension.isSingletonChar(caseInsensitiveChar.value())) {
                        setUnicodeLocaleExtension(str2.substring(2));
                    } else {
                        if (this.extensions == null) {
                            this.extensions = new HashMap(4);
                        }
                        this.extensions.put(caseInsensitiveChar, str2.substring(2));
                    }
                }
                hashSet.add(caseInsensitiveChar);
            }
        }
        if (str != null && str.length() > 0) {
            if (this.extensions == null) {
                this.extensions = new HashMap(1);
            }
            this.extensions.put(new CaseInsensitiveChar(str), str.substring(2));
        }
        return this;
    }

    public InternalLocaleBuilder setLanguageTag(LanguageTag languageTag) {
        clear();
        if (!languageTag.getExtlangs().isEmpty()) {
            this.language = languageTag.getExtlangs().get(0);
        } else {
            String language = languageTag.getLanguage();
            if (!language.equals(LanguageTag.UNDETERMINED)) {
                this.language = language;
            }
        }
        this.script = languageTag.getScript();
        this.region = languageTag.getRegion();
        List<String> variants = languageTag.getVariants();
        if (!variants.isEmpty()) {
            StringBuilder sb = new StringBuilder(variants.get(0));
            int size = variants.size();
            for (int i = 1; i < size; i++) {
                sb.append(BaseLocale.SEP);
                sb.append(variants.get(i));
            }
            this.variant = sb.toString();
        }
        setExtensions(languageTag.getExtensions(), languageTag.getPrivateuse());
        return this;
    }

    public InternalLocaleBuilder setLocale(BaseLocale baseLocale, LocaleExtensions localeExtensions) throws LocaleSyntaxException {
        int iCheckVariants;
        String language = baseLocale.getLanguage();
        String script = baseLocale.getScript();
        String region = baseLocale.getRegion();
        String variant = baseLocale.getVariant();
        if (language.equals("ja") && region.equals("JP") && variant.equals("JP")) {
            variant = "";
        } else if (language.equals("th") && region.equals("TH") && variant.equals("TH")) {
            variant = "";
        } else if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
            language = "nn";
            variant = "";
        }
        if (language.length() > 0 && !LanguageTag.isLanguage(language)) {
            throw new LocaleSyntaxException("Ill-formed language: " + language);
        }
        if (script.length() > 0 && !LanguageTag.isScript(script)) {
            throw new LocaleSyntaxException("Ill-formed script: " + script);
        }
        if (region.length() > 0 && !LanguageTag.isRegion(region)) {
            throw new LocaleSyntaxException("Ill-formed region: " + region);
        }
        if (variant.length() > 0 && (iCheckVariants = checkVariants((variant = variant.replaceAll(LanguageTag.SEP, BaseLocale.SEP)), BaseLocale.SEP)) != -1) {
            throw new LocaleSyntaxException("Ill-formed variant: " + variant, iCheckVariants);
        }
        this.language = language;
        this.script = script;
        this.region = region;
        this.variant = variant;
        clearExtensions();
        Set<Character> keys = localeExtensions == null ? null : localeExtensions.getKeys();
        if (keys != null) {
            for (Character ch : keys) {
                Extension extension = localeExtensions.getExtension(ch);
                if (extension instanceof UnicodeLocaleExtension) {
                    UnicodeLocaleExtension unicodeLocaleExtension = (UnicodeLocaleExtension) extension;
                    for (String str : unicodeLocaleExtension.getUnicodeLocaleAttributes()) {
                        if (this.uattributes == null) {
                            this.uattributes = new HashSet(4);
                        }
                        this.uattributes.add(new CaseInsensitiveString(str));
                    }
                    for (String str2 : unicodeLocaleExtension.getUnicodeLocaleKeys()) {
                        if (this.ukeywords == null) {
                            this.ukeywords = new HashMap(4);
                        }
                        this.ukeywords.put(new CaseInsensitiveString(str2), unicodeLocaleExtension.getUnicodeLocaleType(str2));
                    }
                } else {
                    if (this.extensions == null) {
                        this.extensions = new HashMap(4);
                    }
                    this.extensions.put(new CaseInsensitiveChar(ch.charValue()), extension.getValue());
                }
            }
        }
        return this;
    }

    public InternalLocaleBuilder clear() {
        this.language = "";
        this.script = "";
        this.region = "";
        this.variant = "";
        clearExtensions();
        return this;
    }

    public InternalLocaleBuilder clearExtensions() {
        if (this.extensions != null) {
            this.extensions.clear();
        }
        if (this.uattributes != null) {
            this.uattributes.clear();
        }
        if (this.ukeywords != null) {
            this.ukeywords.clear();
        }
        return this;
    }

    public BaseLocale getBaseLocale() {
        String str;
        int iCurrentStart;
        String str2 = this.language;
        String str3 = this.script;
        String str4 = this.region;
        String string = this.variant;
        if (this.extensions != null && (str = this.extensions.get(PRIVATEUSE_KEY)) != null) {
            StringTokenIterator stringTokenIterator = new StringTokenIterator(str, LanguageTag.SEP);
            boolean z = false;
            while (true) {
                if (!stringTokenIterator.isDone()) {
                    if (z) {
                        iCurrentStart = stringTokenIterator.currentStart();
                        break;
                    }
                    if (LocaleUtils.caseIgnoreMatch(stringTokenIterator.current(), LanguageTag.PRIVUSE_VARIANT_PREFIX)) {
                        z = true;
                    }
                    stringTokenIterator.next();
                } else {
                    iCurrentStart = -1;
                    break;
                }
            }
            if (iCurrentStart != -1) {
                StringBuilder sb = new StringBuilder(string);
                if (sb.length() != 0) {
                    sb.append(BaseLocale.SEP);
                }
                sb.append(str.substring(iCurrentStart).replaceAll(LanguageTag.SEP, BaseLocale.SEP));
                string = sb.toString();
            }
        }
        return BaseLocale.getInstance(str2, str3, str4, string);
    }

    public LocaleExtensions getLocaleExtensions() {
        if (LocaleUtils.isEmpty(this.extensions) && LocaleUtils.isEmpty(this.uattributes) && LocaleUtils.isEmpty(this.ukeywords)) {
            return null;
        }
        LocaleExtensions localeExtensions = new LocaleExtensions(this.extensions, this.uattributes, this.ukeywords);
        if (localeExtensions.isEmpty()) {
            return null;
        }
        return localeExtensions;
    }

    static String removePrivateuseVariant(String str) {
        boolean z;
        StringTokenIterator stringTokenIterator = new StringTokenIterator(str, LanguageTag.SEP);
        int iCurrentStart = -1;
        while (true) {
            if (!stringTokenIterator.isDone()) {
                if (iCurrentStart == -1) {
                    if (LocaleUtils.caseIgnoreMatch(stringTokenIterator.current(), LanguageTag.PRIVUSE_VARIANT_PREFIX)) {
                        iCurrentStart = stringTokenIterator.currentStart();
                    }
                    stringTokenIterator.next();
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            return str;
        }
        if (iCurrentStart == 0) {
            return null;
        }
        return str.substring(0, iCurrentStart - 1);
    }

    private int checkVariants(String str, String str2) {
        StringTokenIterator stringTokenIterator = new StringTokenIterator(str, str2);
        while (!stringTokenIterator.isDone()) {
            if (!LanguageTag.isVariant(stringTokenIterator.current())) {
                return stringTokenIterator.currentStart();
            }
            stringTokenIterator.next();
        }
        return -1;
    }

    private void setUnicodeLocaleExtension(String str) {
        if (this.uattributes != null) {
            this.uattributes.clear();
        }
        if (this.ukeywords != null) {
            this.ukeywords.clear();
        }
        StringTokenIterator stringTokenIterator = new StringTokenIterator(str, LanguageTag.SEP);
        while (!stringTokenIterator.isDone() && UnicodeLocaleExtension.isAttribute(stringTokenIterator.current())) {
            if (this.uattributes == null) {
                this.uattributes = new HashSet(4);
            }
            this.uattributes.add(new CaseInsensitiveString(stringTokenIterator.current()));
            stringTokenIterator.next();
        }
        CaseInsensitiveString caseInsensitiveString = null;
        int iCurrentStart = -1;
        int iCurrentEnd = -1;
        while (!stringTokenIterator.isDone()) {
            if (caseInsensitiveString != null) {
                if (UnicodeLocaleExtension.isKey(stringTokenIterator.current())) {
                    String strSubstring = iCurrentStart == -1 ? "" : str.substring(iCurrentStart, iCurrentEnd);
                    if (this.ukeywords == null) {
                        this.ukeywords = new HashMap(4);
                    }
                    this.ukeywords.put(caseInsensitiveString, strSubstring);
                    caseInsensitiveString = new CaseInsensitiveString(stringTokenIterator.current());
                    if (this.ukeywords.containsKey(caseInsensitiveString)) {
                        caseInsensitiveString = null;
                    }
                    iCurrentStart = -1;
                    iCurrentEnd = -1;
                } else {
                    if (iCurrentStart == -1) {
                        iCurrentStart = stringTokenIterator.currentStart();
                    }
                    iCurrentEnd = stringTokenIterator.currentEnd();
                }
            } else if (UnicodeLocaleExtension.isKey(stringTokenIterator.current())) {
                caseInsensitiveString = new CaseInsensitiveString(stringTokenIterator.current());
                if (this.ukeywords != null && this.ukeywords.containsKey(caseInsensitiveString)) {
                    caseInsensitiveString = null;
                }
            }
            if (!stringTokenIterator.hasNext()) {
                if (caseInsensitiveString != null) {
                    String strSubstring2 = iCurrentStart == -1 ? "" : str.substring(iCurrentStart, iCurrentEnd);
                    if (this.ukeywords == null) {
                        this.ukeywords = new HashMap(4);
                    }
                    this.ukeywords.put(caseInsensitiveString, strSubstring2);
                    return;
                }
                return;
            }
            stringTokenIterator.next();
        }
    }

    static final class CaseInsensitiveString {
        private final String lowerStr;
        private final String str;

        CaseInsensitiveString(String str) {
            this.str = str;
            this.lowerStr = LocaleUtils.toLowerString(str);
        }

        public String value() {
            return this.str;
        }

        public int hashCode() {
            return this.lowerStr.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CaseInsensitiveString)) {
                return false;
            }
            return this.lowerStr.equals(((CaseInsensitiveString) obj).lowerStr);
        }
    }

    static final class CaseInsensitiveChar {
        private final char ch;
        private final char lowerCh;

        private CaseInsensitiveChar(String str) {
            this(str.charAt(0));
        }

        CaseInsensitiveChar(char c) {
            this.ch = c;
            this.lowerCh = LocaleUtils.toLower(this.ch);
        }

        public char value() {
            return this.ch;
        }

        public int hashCode() {
            return this.lowerCh;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return (obj instanceof CaseInsensitiveChar) && this.lowerCh == ((CaseInsensitiveChar) obj).lowerCh;
        }
    }
}
