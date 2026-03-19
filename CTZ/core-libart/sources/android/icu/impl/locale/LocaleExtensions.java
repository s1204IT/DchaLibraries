package android.icu.impl.locale;

import android.icu.impl.locale.InternalLocaleBuilder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class LocaleExtensions {
    static final boolean $assertionsDisabled = false;
    public static final LocaleExtensions CALENDAR_JAPANESE;
    public static final LocaleExtensions NUMBER_THAI;
    private String _id;
    private SortedMap<Character, Extension> _map;
    private static final SortedMap<Character, Extension> EMPTY_MAP = Collections.unmodifiableSortedMap(new TreeMap());
    public static final LocaleExtensions EMPTY_EXTENSIONS = new LocaleExtensions();

    static {
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

    LocaleExtensions(Map<InternalLocaleBuilder.CaseInsensitiveChar, String> map, Set<InternalLocaleBuilder.CaseInsensitiveString> set, Map<InternalLocaleBuilder.CaseInsensitiveString, String> map2) {
        TreeSet treeSet;
        boolean z = false;
        boolean z2 = map != null && map.size() > 0;
        boolean z3 = set != null && set.size() > 0;
        if (map2 != null && map2.size() > 0) {
            z = true;
        }
        if (!z2 && !z3 && !z) {
            this._map = EMPTY_MAP;
            this._id = "";
            return;
        }
        this._map = new TreeMap();
        if (z2) {
            for (Map.Entry<InternalLocaleBuilder.CaseInsensitiveChar, String> entry : map.entrySet()) {
                char lower = AsciiUtil.toLower(entry.getKey().value());
                String value = entry.getValue();
                if (!LanguageTag.isPrivateusePrefixChar(lower) || (value = InternalLocaleBuilder.removePrivateuseVariant(value)) != null) {
                    this._map.put(Character.valueOf(lower), new Extension(lower, AsciiUtil.toLowerString(value)));
                }
            }
        }
        if (z3 || z) {
            TreeMap treeMap = null;
            if (z3) {
                treeSet = new TreeSet();
                Iterator<InternalLocaleBuilder.CaseInsensitiveString> it = set.iterator();
                while (it.hasNext()) {
                    treeSet.add(AsciiUtil.toLowerString(it.next().value()));
                }
            } else {
                treeSet = null;
            }
            if (z) {
                treeMap = new TreeMap();
                for (Map.Entry<InternalLocaleBuilder.CaseInsensitiveString, String> entry2 : map2.entrySet()) {
                    treeMap.put(AsciiUtil.toLowerString(entry2.getKey().value()), AsciiUtil.toLowerString(entry2.getValue()));
                }
            }
            this._map.put('u', new UnicodeLocaleExtension(treeSet, treeMap));
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

    public Extension getExtension(Character ch) {
        return this._map.get(Character.valueOf(AsciiUtil.toLower(ch.charValue())));
    }

    public String getExtensionValue(Character ch) {
        Extension extension = this._map.get(Character.valueOf(AsciiUtil.toLower(ch.charValue())));
        if (extension == null) {
            return null;
        }
        return extension.getValue();
    }

    public Set<String> getUnicodeLocaleAttributes() {
        Extension extension = this._map.get('u');
        if (extension == null) {
            return Collections.emptySet();
        }
        return ((UnicodeLocaleExtension) extension).getUnicodeLocaleAttributes();
    }

    public Set<String> getUnicodeLocaleKeys() {
        Extension extension = this._map.get('u');
        if (extension == null) {
            return Collections.emptySet();
        }
        return ((UnicodeLocaleExtension) extension).getUnicodeLocaleKeys();
    }

    public String getUnicodeLocaleType(String str) {
        Extension extension = this._map.get('u');
        if (extension == null) {
            return null;
        }
        return ((UnicodeLocaleExtension) extension).getUnicodeLocaleType(AsciiUtil.toLowerString(str));
    }

    public boolean isEmpty() {
        return this._map.isEmpty();
    }

    public static boolean isValidKey(char c) {
        return LanguageTag.isExtensionSingletonChar(c) || LanguageTag.isPrivateusePrefixChar(c);
    }

    public static boolean isValidUnicodeLocaleKey(String str) {
        return UnicodeLocaleExtension.isKey(str);
    }

    private static String toID(SortedMap<Character, Extension> sortedMap) {
        StringBuilder sb = new StringBuilder();
        Extension extension = null;
        for (Map.Entry<Character, Extension> entry : sortedMap.entrySet()) {
            char cCharValue = entry.getKey().charValue();
            Extension value = entry.getValue();
            if (!LanguageTag.isPrivateusePrefixChar(cCharValue)) {
                if (sb.length() > 0) {
                    sb.append(LanguageTag.SEP);
                }
                sb.append(value);
            } else {
                extension = value;
            }
        }
        if (extension != null) {
            if (sb.length() > 0) {
                sb.append(LanguageTag.SEP);
            }
            sb.append(extension);
        }
        return sb.toString();
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
        return this._id.equals(((LocaleExtensions) obj)._id);
    }
}
