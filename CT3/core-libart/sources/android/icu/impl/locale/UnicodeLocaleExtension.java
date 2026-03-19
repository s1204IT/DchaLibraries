package android.icu.impl.locale;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class UnicodeLocaleExtension extends Extension {
    public static final UnicodeLocaleExtension NU_THAI;
    public static final char SINGLETON = 'u';
    private SortedSet<String> _attributes;
    private SortedMap<String, String> _keywords;
    private static final SortedSet<String> EMPTY_SORTED_SET = new TreeSet();
    private static final SortedMap<String, String> EMPTY_SORTED_MAP = new TreeMap();
    public static final UnicodeLocaleExtension CA_JAPANESE = new UnicodeLocaleExtension();

    static {
        CA_JAPANESE._keywords = new TreeMap();
        CA_JAPANESE._keywords.put("ca", "japanese");
        CA_JAPANESE._value = "ca-japanese";
        NU_THAI = new UnicodeLocaleExtension();
        NU_THAI._keywords = new TreeMap();
        NU_THAI._keywords.put("nu", "thai");
        NU_THAI._value = "nu-thai";
    }

    private UnicodeLocaleExtension() {
        super('u');
        this._attributes = EMPTY_SORTED_SET;
        this._keywords = EMPTY_SORTED_MAP;
    }

    UnicodeLocaleExtension(SortedSet<String> attributes, SortedMap<String, String> keywords) {
        this();
        if (attributes != null && attributes.size() > 0) {
            this._attributes = attributes;
        }
        if (keywords != null && keywords.size() > 0) {
            this._keywords = keywords;
        }
        if (this._attributes.size() <= 0 && this._keywords.size() <= 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String attribute : this._attributes) {
            sb.append(LanguageTag.SEP).append(attribute);
        }
        for (Map.Entry<String, String> keyword : this._keywords.entrySet()) {
            String key = keyword.getKey();
            String value = keyword.getValue();
            sb.append(LanguageTag.SEP).append(key);
            if (value.length() > 0) {
                sb.append(LanguageTag.SEP).append(value);
            }
        }
        this._value = sb.substring(1);
    }

    public Set<String> getUnicodeLocaleAttributes() {
        return Collections.unmodifiableSet(this._attributes);
    }

    public Set<String> getUnicodeLocaleKeys() {
        return Collections.unmodifiableSet(this._keywords.keySet());
    }

    public String getUnicodeLocaleType(String unicodeLocaleKey) {
        return this._keywords.get(unicodeLocaleKey);
    }

    public static boolean isSingletonChar(char c) {
        return 'u' == AsciiUtil.toLower(c);
    }

    public static boolean isAttribute(String s) {
        if (s.length() < 3 || s.length() > 8) {
            return false;
        }
        return AsciiUtil.isAlphaNumericString(s);
    }

    public static boolean isKey(String s) {
        if (s.length() == 2) {
            return AsciiUtil.isAlphaNumericString(s);
        }
        return false;
    }

    public static boolean isTypeSubtag(String s) {
        if (s.length() < 3 || s.length() > 8) {
            return false;
        }
        return AsciiUtil.isAlphaNumericString(s);
    }

    public static boolean isType(String s) {
        int startIdx = 0;
        while (true) {
            int idx = s.indexOf(LanguageTag.SEP, startIdx);
            String subtag = idx < 0 ? s.substring(startIdx) : s.substring(startIdx, idx);
            if (!isTypeSubtag(subtag)) {
                return false;
            }
            if (idx < 0) {
                return 1 != 0 && startIdx < s.length();
            }
            startIdx = idx + 1;
        }
    }
}
