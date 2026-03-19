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

    BaseLocale(String language, String script, String region, String variant, BaseLocale baseLocale) {
        this(language, script, region, variant);
    }

    private BaseLocale(String language, String script, String region, String variant) {
        this._language = "";
        this._script = "";
        this._region = "";
        this._variant = "";
        this._hash = 0;
        if (language != null) {
            this._language = AsciiUtil.toLowerString(language).intern();
        }
        if (script != null) {
            this._script = AsciiUtil.toTitleString(script).intern();
        }
        if (region != null) {
            this._region = AsciiUtil.toUpperString(region).intern();
        }
        if (variant == null) {
            return;
        }
        this._variant = AsciiUtil.toUpperString(variant).intern();
    }

    public static BaseLocale getInstance(String language, String script, String region, String variant) {
        Key key = new Key(language, script, region, variant);
        BaseLocale baseLocale = CACHE.get(key);
        return baseLocale;
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
        BaseLocale other = (BaseLocale) obj;
        if (hashCode() == other.hashCode() && this._language.equals(other._language) && this._script.equals(other._script) && this._region.equals(other._region)) {
            return this._variant.equals(other._variant);
        }
        return false;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (this._language.length() > 0) {
            buf.append("language=");
            buf.append(this._language);
        }
        if (this._script.length() > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append("script=");
            buf.append(this._script);
        }
        if (this._region.length() > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append("region=");
            buf.append(this._region);
        }
        if (this._variant.length() > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append("variant=");
            buf.append(this._variant);
        }
        return buf.toString();
    }

    public int hashCode() {
        int h = this._hash;
        if (h == 0) {
            for (int i = 0; i < this._language.length(); i++) {
                h = (h * 31) + this._language.charAt(i);
            }
            for (int i2 = 0; i2 < this._script.length(); i2++) {
                h = (h * 31) + this._script.charAt(i2);
            }
            for (int i3 = 0; i3 < this._region.length(); i3++) {
                h = (h * 31) + this._region.charAt(i3);
            }
            for (int i4 = 0; i4 < this._variant.length(); i4++) {
                h = (h * 31) + this._variant.charAt(i4);
            }
            this._hash = h;
        }
        return h;
    }

    private static class Key implements Comparable<Key> {
        private volatile int _hash;
        private String _lang;
        private String _regn;
        private String _scrt;
        private String _vart;

        public Key(String language, String script, String region, String variant) {
            this._lang = "";
            this._scrt = "";
            this._regn = "";
            this._vart = "";
            if (language != null) {
                this._lang = language;
            }
            if (script != null) {
                this._scrt = script;
            }
            if (region != null) {
                this._regn = region;
            }
            if (variant == null) {
                return;
            }
            this._vart = variant;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj instanceof Key) && AsciiUtil.caseIgnoreMatch(((Key) obj)._lang, this._lang) && AsciiUtil.caseIgnoreMatch(((Key) obj)._scrt, this._scrt) && AsciiUtil.caseIgnoreMatch(((Key) obj)._regn, this._regn)) {
                return AsciiUtil.caseIgnoreMatch(((Key) obj)._vart, this._vart);
            }
            return false;
        }

        @Override
        public int compareTo(Key other) {
            int res = AsciiUtil.caseIgnoreCompare(this._lang, other._lang);
            if (res == 0) {
                int res2 = AsciiUtil.caseIgnoreCompare(this._scrt, other._scrt);
                if (res2 == 0) {
                    int res3 = AsciiUtil.caseIgnoreCompare(this._regn, other._regn);
                    if (res3 == 0) {
                        return AsciiUtil.caseIgnoreCompare(this._vart, other._vart);
                    }
                    return res3;
                }
                return res2;
            }
            return res;
        }

        public int hashCode() {
            int h = this._hash;
            if (h == 0) {
                for (int i = 0; i < this._lang.length(); i++) {
                    h = (h * 31) + AsciiUtil.toLower(this._lang.charAt(i));
                }
                for (int i2 = 0; i2 < this._scrt.length(); i2++) {
                    h = (h * 31) + AsciiUtil.toLower(this._scrt.charAt(i2));
                }
                for (int i3 = 0; i3 < this._regn.length(); i3++) {
                    h = (h * 31) + AsciiUtil.toLower(this._regn.charAt(i3));
                }
                for (int i4 = 0; i4 < this._vart.length(); i4++) {
                    h = (h * 31) + AsciiUtil.toLower(this._vart.charAt(i4));
                }
                this._hash = h;
            }
            return h;
        }

        public static Key normalize(Key key) {
            String lang = AsciiUtil.toLowerString(key._lang).intern();
            String scrt = AsciiUtil.toTitleString(key._scrt).intern();
            String regn = AsciiUtil.toUpperString(key._regn).intern();
            String vart = AsciiUtil.toUpperString(key._vart).intern();
            return new Key(lang, scrt, regn, vart);
        }
    }

    private static class Cache extends LocaleObjectCache<Key, BaseLocale> {
        @Override
        protected Key normalizeKey(Key key) {
            return Key.normalize(key);
        }

        @Override
        protected BaseLocale createObject(Key key) {
            return new BaseLocale(key._lang, key._scrt, key._regn, key._vart, null);
        }
    }
}
