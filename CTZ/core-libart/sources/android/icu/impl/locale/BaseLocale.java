package android.icu.impl.locale;

public final class BaseLocale {
    private static final boolean JDKIMPL = false;
    public static final String SEP = "_";
    private volatile transient int _hash;
    private String _language;
    private String _region;
    private String _script;
    private String _variant;
    private static final Cache CACHE = new Cache();
    public static final BaseLocale ROOT = getInstance("", "", "", "");

    private BaseLocale(String str, String str2, String str3, String str4) {
        this._language = "";
        this._script = "";
        this._region = "";
        this._variant = "";
        this._hash = 0;
        if (str != null) {
            this._language = AsciiUtil.toLowerString(str).intern();
        }
        if (str2 != null) {
            this._script = AsciiUtil.toTitleString(str2).intern();
        }
        if (str3 != null) {
            this._region = AsciiUtil.toUpperString(str3).intern();
        }
        if (str4 != null) {
            this._variant = AsciiUtil.toUpperString(str4).intern();
        }
    }

    public static BaseLocale getInstance(String str, String str2, String str3, String str4) {
        return CACHE.get(new Key(str, str2, str3, str4));
    }

    public String getLanguage() {
        return this._language;
    }

    public String getScript() {
        return this._script;
    }

    public String getRegion() {
        return this._region;
    }

    public String getVariant() {
        return this._variant;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseLocale)) {
            return false;
        }
        BaseLocale baseLocale = (BaseLocale) obj;
        return hashCode() == baseLocale.hashCode() && this._language.equals(baseLocale._language) && this._script.equals(baseLocale._script) && this._region.equals(baseLocale._region) && this._variant.equals(baseLocale._variant);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this._language.length() > 0) {
            sb.append("language=");
            sb.append(this._language);
        }
        if (this._script.length() > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("script=");
            sb.append(this._script);
        }
        if (this._region.length() > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("region=");
            sb.append(this._region);
        }
        if (this._variant.length() > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("variant=");
            sb.append(this._variant);
        }
        return sb.toString();
    }

    public int hashCode() {
        int iCharAt = this._hash;
        if (iCharAt == 0) {
            int iCharAt2 = iCharAt;
            for (int i = 0; i < this._language.length(); i++) {
                iCharAt2 = this._language.charAt(i) + (31 * iCharAt2);
            }
            for (int i2 = 0; i2 < this._script.length(); i2++) {
                iCharAt2 = (iCharAt2 * 31) + this._script.charAt(i2);
            }
            for (int i3 = 0; i3 < this._region.length(); i3++) {
                iCharAt2 = (iCharAt2 * 31) + this._region.charAt(i3);
            }
            iCharAt = iCharAt2;
            for (int i4 = 0; i4 < this._variant.length(); i4++) {
                iCharAt = (iCharAt * 31) + this._variant.charAt(i4);
            }
            this._hash = iCharAt;
        }
        return iCharAt;
    }

    private static class Key implements Comparable<Key> {
        private volatile int _hash;
        private String _lang;
        private String _regn;
        private String _scrt;
        private String _vart;

        public Key(String str, String str2, String str3, String str4) {
            this._lang = "";
            this._scrt = "";
            this._regn = "";
            this._vart = "";
            if (str != null) {
                this._lang = str;
            }
            if (str2 != null) {
                this._scrt = str2;
            }
            if (str3 != null) {
                this._regn = str3;
            }
            if (str4 != null) {
                this._vart = str4;
            }
        }

        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj instanceof Key) {
                    Key key = (Key) obj;
                    if (!AsciiUtil.caseIgnoreMatch(key._lang, this._lang) || !AsciiUtil.caseIgnoreMatch(key._scrt, this._scrt) || !AsciiUtil.caseIgnoreMatch(key._regn, this._regn) || !AsciiUtil.caseIgnoreMatch(key._vart, this._vart)) {
                    }
                }
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(Key key) {
            int iCaseIgnoreCompare = AsciiUtil.caseIgnoreCompare(this._lang, key._lang);
            if (iCaseIgnoreCompare == 0) {
                int iCaseIgnoreCompare2 = AsciiUtil.caseIgnoreCompare(this._scrt, key._scrt);
                if (iCaseIgnoreCompare2 == 0) {
                    int iCaseIgnoreCompare3 = AsciiUtil.caseIgnoreCompare(this._regn, key._regn);
                    if (iCaseIgnoreCompare3 == 0) {
                        return AsciiUtil.caseIgnoreCompare(this._vart, key._vart);
                    }
                    return iCaseIgnoreCompare3;
                }
                return iCaseIgnoreCompare2;
            }
            return iCaseIgnoreCompare;
        }

        public int hashCode() {
            int lower = this._hash;
            if (lower == 0) {
                int lower2 = lower;
                for (int i = 0; i < this._lang.length(); i++) {
                    lower2 = AsciiUtil.toLower(this._lang.charAt(i)) + (31 * lower2);
                }
                for (int i2 = 0; i2 < this._scrt.length(); i2++) {
                    lower2 = (lower2 * 31) + AsciiUtil.toLower(this._scrt.charAt(i2));
                }
                for (int i3 = 0; i3 < this._regn.length(); i3++) {
                    lower2 = (lower2 * 31) + AsciiUtil.toLower(this._regn.charAt(i3));
                }
                lower = lower2;
                for (int i4 = 0; i4 < this._vart.length(); i4++) {
                    lower = (lower * 31) + AsciiUtil.toLower(this._vart.charAt(i4));
                }
                this._hash = lower;
            }
            return lower;
        }

        public static Key normalize(Key key) {
            return new Key(AsciiUtil.toLowerString(key._lang).intern(), AsciiUtil.toTitleString(key._scrt).intern(), AsciiUtil.toUpperString(key._regn).intern(), AsciiUtil.toUpperString(key._vart).intern());
        }
    }

    private static class Cache extends LocaleObjectCache<Key, BaseLocale> {
        @Override
        protected Key normalizeKey(Key key) {
            return Key.normalize(key);
        }

        @Override
        protected BaseLocale createObject(Key key) {
            return new BaseLocale(key._lang, key._scrt, key._regn, key._vart);
        }
    }
}
