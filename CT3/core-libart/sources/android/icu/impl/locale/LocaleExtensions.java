package android.icu.impl.locale;

import android.icu.impl.locale.InternalLocaleBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class LocaleExtensions {

    static final boolean f61assertionsDisabled;
    public static final LocaleExtensions CALENDAR_JAPANESE;
    public static final LocaleExtensions EMPTY_EXTENSIONS;
    private static final SortedMap<Character, Extension> EMPTY_MAP;
    public static final LocaleExtensions NUMBER_THAI;
    private String _id;
    private SortedMap<Character, Extension> _map;

    static {
        f61assertionsDisabled = !LocaleExtensions.class.desiredAssertionStatus();
        EMPTY_MAP = Collections.unmodifiableSortedMap(new TreeMap());
        EMPTY_EXTENSIONS = new LocaleExtensions();
        EMPTY_EXTENSIONS._id = "";
        EMPTY_EXTENSIONS._map = EMPTY_MAP;
        CALENDAR_JAPANESE = new LocaleExtensions();
        CALENDAR_JAPANESE._id = "u-ca-japanese";
        CALENDAR_JAPANESE._map = new TreeMap();
        CALENDAR_JAPANESE._map.put('u', UnicodeLocaleExtension.CA_JAPANESE);
        NUMBER_THAI = new LocaleExtensions();
        NUMBER_THAI._id = "u-nu-thai";
        NUMBER_THAI._map = new TreeMap();
        NUMBER_THAI._map.put('u', UnicodeLocaleExtension.NU_THAI);
    }

    private LocaleExtensions() {
    }

    LocaleExtensions(Map<InternalLocaleBuilder.CaseInsensitiveChar, String> extensions, Set<InternalLocaleBuilder.CaseInsensitiveString> uattributes, Map<InternalLocaleBuilder.CaseInsensitiveString, String> ukeywords) {
        boolean hasExtension = extensions != null && extensions.size() > 0;
        boolean hasUAttributes = uattributes != null && uattributes.size() > 0;
        boolean hasUKeywords = ukeywords != null && ukeywords.size() > 0;
        if (!hasExtension && !hasUAttributes && !hasUKeywords) {
            this._map = EMPTY_MAP;
            this._id = "";
            return;
        }
        this._map = new TreeMap();
        if (hasExtension) {
            for (Map.Entry<InternalLocaleBuilder.CaseInsensitiveChar, String> ext : extensions.entrySet()) {
                char key = AsciiUtil.toLower(ext.getKey().value());
                String value = ext.getValue();
                if (!LanguageTag.isPrivateusePrefixChar(key) || (value = InternalLocaleBuilder.removePrivateuseVariant(value)) != null) {
                    Extension e = new Extension(key, AsciiUtil.toLowerString(value));
                    this._map.put(Character.valueOf(key), e);
                }
            }
        }
        if (hasUAttributes || hasUKeywords) {
            TreeSet<String> uaset = null;
            TreeMap<String, String> ukmap = null;
            if (hasUAttributes) {
                uaset = new TreeSet<>();
                for (InternalLocaleBuilder.CaseInsensitiveString cis : uattributes) {
                    uaset.add(AsciiUtil.toLowerString(cis.value()));
                }
            }
            if (hasUKeywords) {
                ukmap = new TreeMap<>();
                for (Map.Entry<InternalLocaleBuilder.CaseInsensitiveString, String> kwd : ukeywords.entrySet()) {
                    String key2 = AsciiUtil.toLowerString(kwd.getKey().value());
                    String type = AsciiUtil.toLowerString(kwd.getValue());
                    ukmap.put(key2, type);
                }
            }
            UnicodeLocaleExtension ule = new UnicodeLocaleExtension(uaset, ukmap);
            this._map.put('u', ule);
        }
        if (this._map.size() == 0) {
            this._map = EMPTY_MAP;
            this._id = "";
        } else {
            this._id = toID(this._map);
        }
    }

    public Set<Character> getKeys() {
        return Collections.unmodifiableSet(this._map.keySet());
    }

    public Extension getExtension(Character key) {
        return this._map.get(Character.valueOf(AsciiUtil.toLower(key.charValue())));
    }

    public String getExtensionValue(Character key) {
        Extension ext = this._map.get(Character.valueOf(AsciiUtil.toLower(key.charValue())));
        if (ext == null) {
            return null;
        }
        return ext.getValue();
    }

    public Set<String> getUnicodeLocaleAttributes() {
        Extension ext = this._map.get('u');
        if (ext == null) {
            return Collections.emptySet();
        }
        if (f61assertionsDisabled || (ext instanceof UnicodeLocaleExtension)) {
            return ((UnicodeLocaleExtension) ext).getUnicodeLocaleAttributes();
        }
        throw new AssertionError();
    }

    public Set<String> getUnicodeLocaleKeys() {
        Extension ext = this._map.get('u');
        if (ext == null) {
            return Collections.emptySet();
        }
        if (f61assertionsDisabled || (ext instanceof UnicodeLocaleExtension)) {
            return ((UnicodeLocaleExtension) ext).getUnicodeLocaleKeys();
        }
        throw new AssertionError();
    }

    public String getUnicodeLocaleType(String unicodeLocaleKey) {
        Extension ext = this._map.get('u');
        if (ext == null) {
            return null;
        }
        if (f61assertionsDisabled || (ext instanceof UnicodeLocaleExtension)) {
            return ((UnicodeLocaleExtension) ext).getUnicodeLocaleType(AsciiUtil.toLowerString(unicodeLocaleKey));
        }
        throw new AssertionError();
    }

    public boolean isEmpty() {
        return this._map.isEmpty();
    }

    public static boolean isValidKey(char c) {
        if (LanguageTag.isExtensionSingletonChar(c)) {
            return true;
        }
        return LanguageTag.isPrivateusePrefixChar(c);
    }

    public static boolean isValidUnicodeLocaleKey(String ukey) {
        return UnicodeLocaleExtension.isKey(ukey);
    }

    private static String toID(SortedMap<Character, Extension> map) {
        StringBuilder buf = new StringBuilder();
        Extension privuse = null;
        for (Map.Entry<Character, Extension> entry : map.entrySet()) {
            char singleton = entry.getKey().charValue();
            Extension extension = entry.getValue();
            if (LanguageTag.isPrivateusePrefixChar(singleton)) {
                privuse = extension;
            } else {
                if (buf.length() > 0) {
                    buf.append(LanguageTag.SEP);
                }
                buf.append(extension);
            }
        }
        if (privuse != null) {
            if (buf.length() > 0) {
                buf.append(LanguageTag.SEP);
            }
            buf.append(privuse);
        }
        return buf.toString();
    }

    public String toString() {
        return this._id;
    }

    public String getID() {
        return this._id;
    }

    public int hashCode() {
        return this._id.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocaleExtensions)) {
            return false;
        }
        return this._id.equals(obj._id);
    }
}
