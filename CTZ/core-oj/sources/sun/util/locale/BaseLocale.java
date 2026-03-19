package sun.util.locale;

import java.lang.ref.SoftReference;
import sun.security.x509.PolicyInformation;

public final class BaseLocale {
    private static final Cache CACHE = new Cache();
    public static final String SEP = "_";
    private volatile int hash;
    private final String language;
    private final String region;
    private final String script;
    private final String variant;

    private BaseLocale(String str, String str2) {
        this.hash = 0;
        this.language = str;
        this.script = "";
        this.region = str2;
        this.variant = "";
    }

    private BaseLocale(String str, String str2, String str3, String str4) {
        this.hash = 0;
        this.language = str != null ? LocaleUtils.toLowerString(str).intern() : "";
        this.script = str2 != null ? LocaleUtils.toTitleString(str2).intern() : "";
        this.region = str3 != null ? LocaleUtils.toUpperString(str3).intern() : "";
        this.variant = str4 != null ? str4.intern() : "";
    }

    public static BaseLocale createInstance(String str, String str2) {
        BaseLocale baseLocale = new BaseLocale(str, str2);
        CACHE.put(new Key(str, str2), baseLocale);
        return baseLocale;
    }

    public static BaseLocale getInstance(String str, String str2, String str3, String str4) {
        if (str != null) {
            if (LocaleUtils.caseIgnoreMatch(str, "he")) {
                str = "iw";
            } else if (LocaleUtils.caseIgnoreMatch(str, "yi")) {
                str = "ji";
            } else if (LocaleUtils.caseIgnoreMatch(str, PolicyInformation.ID)) {
                str = "in";
            }
        }
        return CACHE.get(new Key(str, str2, str3, str4));
    }

    public String getLanguage() {
        return this.language;
    }

    public String getScript() {
        return this.script;
    }

    public String getRegion() {
        return this.region;
    }

    public String getVariant() {
        return this.variant;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseLocale)) {
            return false;
        }
        BaseLocale baseLocale = (BaseLocale) obj;
        return this.language == baseLocale.language && this.script == baseLocale.script && this.region == baseLocale.region && this.variant == baseLocale.variant;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.language.length() > 0) {
            sb.append("language=");
            sb.append(this.language);
        }
        if (this.script.length() > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("script=");
            sb.append(this.script);
        }
        if (this.region.length() > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("region=");
            sb.append(this.region);
        }
        if (this.variant.length() > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("variant=");
            sb.append(this.variant);
        }
        return sb.toString();
    }

    public int hashCode() {
        int i = this.hash;
        if (i == 0) {
            int iHashCode = this.variant.hashCode() + (31 * ((((this.language.hashCode() * 31) + this.script.hashCode()) * 31) + this.region.hashCode()));
            this.hash = iHashCode;
            return iHashCode;
        }
        return i;
    }

    private static final class Key {
        static final boolean $assertionsDisabled = false;
        private final int hash;
        private final SoftReference<String> lang;
        private final boolean normalized;
        private final SoftReference<String> regn;
        private final SoftReference<String> scrt;
        private final SoftReference<String> vart;

        private Key(String str, String str2) {
            this.lang = new SoftReference<>(str);
            this.scrt = new SoftReference<>("");
            this.regn = new SoftReference<>(str2);
            this.vart = new SoftReference<>("");
            this.normalized = true;
            int iHashCode = str.hashCode();
            if (str2 != "") {
                int length = str2.length();
                for (int i = 0; i < length; i++) {
                    iHashCode = LocaleUtils.toLower(str2.charAt(i)) + (31 * iHashCode);
                }
            }
            this.hash = iHashCode;
        }

        public Key(String str, String str2, String str3, String str4) {
            this(str, str2, str3, str4, false);
        }

        private Key(String str, String str2, String str3, String str4, boolean z) {
            int iCharAt;
            if (str != null) {
                this.lang = new SoftReference<>(str);
                int length = str.length();
                iCharAt = 0;
                for (int i = 0; i < length; i++) {
                    iCharAt = (iCharAt * 31) + LocaleUtils.toLower(str.charAt(i));
                }
            } else {
                this.lang = new SoftReference<>("");
                iCharAt = 0;
            }
            if (str2 != null) {
                this.scrt = new SoftReference<>(str2);
                int length2 = str2.length();
                for (int i2 = 0; i2 < length2; i2++) {
                    iCharAt = (iCharAt * 31) + LocaleUtils.toLower(str2.charAt(i2));
                }
            } else {
                this.scrt = new SoftReference<>("");
            }
            if (str3 != null) {
                this.regn = new SoftReference<>(str3);
                int length3 = str3.length();
                for (int i3 = 0; i3 < length3; i3++) {
                    iCharAt = (iCharAt * 31) + LocaleUtils.toLower(str3.charAt(i3));
                }
            } else {
                this.regn = new SoftReference<>("");
            }
            if (str4 != null) {
                this.vart = new SoftReference<>(str4);
                int length4 = str4.length();
                for (int i4 = 0; i4 < length4; i4++) {
                    iCharAt = (iCharAt * 31) + str4.charAt(i4);
                }
            } else {
                this.vart = new SoftReference<>("");
            }
            this.hash = iCharAt;
            this.normalized = z;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Key) {
                Key key = (Key) obj;
                if (this.hash == key.hash) {
                    String str = this.lang.get();
                    String str2 = key.lang.get();
                    if (str != null && str2 != null && LocaleUtils.caseIgnoreMatch(str2, str)) {
                        String str3 = this.scrt.get();
                        String str4 = key.scrt.get();
                        if (str3 != null && str4 != null && LocaleUtils.caseIgnoreMatch(str4, str3)) {
                            String str5 = this.regn.get();
                            String str6 = key.regn.get();
                            if (str5 != null && str6 != null && LocaleUtils.caseIgnoreMatch(str6, str5)) {
                                String str7 = this.vart.get();
                                String str8 = key.vart.get();
                                return str8 != null && str8.equals(str7);
                            }
                        }
                    }
                }
            }
            return false;
        }

        public int hashCode() {
            return this.hash;
        }

        public static Key normalize(Key key) {
            if (key.normalized) {
                return key;
            }
            return new Key(LocaleUtils.toLowerString(key.lang.get()).intern(), LocaleUtils.toTitleString(key.scrt.get()).intern(), LocaleUtils.toUpperString(key.regn.get()).intern(), key.vart.get().intern(), true);
        }
    }

    private static class Cache extends LocaleObjectCache<Key, BaseLocale> {
        static final boolean $assertionsDisabled = false;

        @Override
        protected Key normalizeKey(Key key) {
            return Key.normalize(key);
        }

        @Override
        protected BaseLocale createObject(Key key) {
            return new BaseLocale((String) key.lang.get(), (String) key.scrt.get(), (String) key.regn.get(), (String) key.vart.get());
        }
    }
}
