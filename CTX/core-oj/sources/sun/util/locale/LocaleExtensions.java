package sun.util.locale;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import sun.util.locale.InternalLocaleBuilder;

public class LocaleExtensions {
    static final boolean $assertionsDisabled = false;
    public static final LocaleExtensions CALENDAR_JAPANESE = new LocaleExtensions("u-ca-japanese", (Character) 'u', (Extension) UnicodeLocaleExtension.CA_JAPANESE);
    public static final LocaleExtensions NUMBER_THAI = new LocaleExtensions("u-nu-thai", (Character) 'u', (Extension) UnicodeLocaleExtension.NU_THAI);
    private final Map<Character, Extension> extensionMap;
    private final String id;

    private LocaleExtensions(String str, Character ch, Extension extension) {
        this.id = str;
        this.extensionMap = Collections.singletonMap(ch, extension);
    }

    LocaleExtensions(Map<InternalLocaleBuilder.CaseInsensitiveChar, String> map, Set<InternalLocaleBuilder.CaseInsensitiveString> set, Map<InternalLocaleBuilder.CaseInsensitiveString, String> map2) {
        TreeSet treeSet;
        boolean z = !LocaleUtils.isEmpty(map);
        boolean z2 = !LocaleUtils.isEmpty(set);
        boolean z3 = !LocaleUtils.isEmpty(map2);
        if (!z && !z2 && !z3) {
            this.id = "";
            this.extensionMap = Collections.emptyMap();
            return;
        }
        TreeMap treeMap = new TreeMap();
        if (z) {
            for (Map.Entry<InternalLocaleBuilder.CaseInsensitiveChar, String> entry : map.entrySet()) {
                char lower = LocaleUtils.toLower(entry.getKey().value());
                String value = entry.getValue();
                if (!LanguageTag.isPrivateusePrefixChar(lower) || (value = InternalLocaleBuilder.removePrivateuseVariant(value)) != null) {
                    treeMap.put(Character.valueOf(lower), new Extension(lower, LocaleUtils.toLowerString(value)));
                }
            }
        }
        if (z2 || z3) {
            TreeMap treeMap2 = null;
            if (z2) {
                treeSet = new TreeSet();
                Iterator<InternalLocaleBuilder.CaseInsensitiveString> it = set.iterator();
                while (it.hasNext()) {
                    treeSet.add(LocaleUtils.toLowerString(it.next().value()));
                }
            } else {
                treeSet = null;
            }
            if (z3) {
                treeMap2 = new TreeMap();
                for (Map.Entry<InternalLocaleBuilder.CaseInsensitiveString, String> entry2 : map2.entrySet()) {
                    treeMap2.put(LocaleUtils.toLowerString(entry2.getKey().value()), LocaleUtils.toLowerString(entry2.getValue()));
                }
            }
            treeMap.put('u', new UnicodeLocaleExtension(treeSet, treeMap2));
        }
        if (treeMap.isEmpty()) {
            this.id = "";
            this.extensionMap = Collections.emptyMap();
        } else {
            this.id = toID(treeMap);
            this.extensionMap = treeMap;
        }
    }

    public Set<Character> getKeys() {
        if (this.extensionMap.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(this.extensionMap.keySet());
    }

    public Extension getExtension(Character ch) {
        return this.extensionMap.get(Character.valueOf(LocaleUtils.toLower(ch.charValue())));
    }

    public String getExtensionValue(Character ch) {
        Extension extension = this.extensionMap.get(Character.valueOf(LocaleUtils.toLower(ch.charValue())));
        if (extension == null) {
            return null;
        }
        return extension.getValue();
    }

    public Set<String> getUnicodeLocaleAttributes() {
        Extension extension = this.extensionMap.get('u');
        if (extension == null) {
            return Collections.emptySet();
        }
        return ((UnicodeLocaleExtension) extension).getUnicodeLocaleAttributes();
    }

    public Set<String> getUnicodeLocaleKeys() {
        Extension extension = this.extensionMap.get('u');
        if (extension == null) {
            return Collections.emptySet();
        }
        return ((UnicodeLocaleExtension) extension).getUnicodeLocaleKeys();
    }

    public String getUnicodeLocaleType(String str) {
        Extension extension = this.extensionMap.get('u');
        if (extension == null) {
            return null;
        }
        return ((UnicodeLocaleExtension) extension).getUnicodeLocaleType(LocaleUtils.toLowerString(str));
    }

    public boolean isEmpty() {
        return this.extensionMap.isEmpty();
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
                sb.append((Object) value);
            } else {
                extension = value;
            }
        }
        if (extension != null) {
            if (sb.length() > 0) {
                sb.append(LanguageTag.SEP);
            }
            sb.append((Object) extension);
        }
        return sb.toString();
    }

    public String toString() {
        return this.id;
    }

    public String getID() {
        return this.id;
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocaleExtensions)) {
            return false;
        }
        return this.id.equals(((LocaleExtensions) obj).id);
    }
}
