package android.icu.impl;

import android.icu.impl.locale.AsciiUtil;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class LocaleIDParser {
    private static final char COMMA = ',';
    private static final char DONE = 65535;
    private static final char DOT = '.';
    private static final char HYPHEN = '-';
    private static final char ITEM_SEPARATOR = ';';
    private static final char KEYWORD_ASSIGN = '=';
    private static final char KEYWORD_SEPARATOR = '@';
    private static final char UNDERSCORE = '_';
    String baseName;
    private StringBuilder buffer;
    private boolean canonicalize;
    private boolean hadCountry;
    private char[] id;
    private int index;
    Map<String, String> keywords;

    public LocaleIDParser(String str) {
        this(str, false);
    }

    public LocaleIDParser(String str, boolean z) {
        this.id = str.toCharArray();
        this.index = 0;
        this.buffer = new StringBuilder(this.id.length + 5);
        this.canonicalize = z;
    }

    private void reset() {
        this.index = 0;
        this.buffer = new StringBuilder(this.id.length + 5);
    }

    private void append(char c) {
        this.buffer.append(c);
    }

    private void addSeparator() {
        append(UNDERSCORE);
    }

    private String getString(int i) {
        return this.buffer.substring(i);
    }

    private void set(int i, String str) {
        this.buffer.delete(i, this.buffer.length());
        this.buffer.insert(i, str);
    }

    private void append(String str) {
        this.buffer.append(str);
    }

    private char next() {
        if (this.index == this.id.length) {
            this.index++;
            return (char) 65535;
        }
        char[] cArr = this.id;
        int i = this.index;
        this.index = i + 1;
        return cArr[i];
    }

    private void skipUntilTerminatorOrIDSeparator() {
        while (!isTerminatorOrIDSeparator(next())) {
        }
        this.index--;
    }

    private boolean atTerminator() {
        return this.index >= this.id.length || isTerminator(this.id[this.index]);
    }

    private boolean isTerminator(char c) {
        return c == '@' || c == 65535 || c == '.';
    }

    private boolean isTerminatorOrIDSeparator(char c) {
        return c == '_' || c == '-' || isTerminator(c);
    }

    private boolean haveExperimentalLanguagePrefix() {
        char c;
        if (this.id.length <= 2 || !((c = this.id[1]) == '-' || c == '_')) {
            return false;
        }
        char c2 = this.id[0];
        return c2 == 'x' || c2 == 'X' || c2 == 'i' || c2 == 'I';
    }

    private boolean haveKeywordAssign() {
        for (int i = this.index; i < this.id.length; i++) {
            if (this.id[i] == '=') {
                return true;
            }
        }
        return false;
    }

    private int parseLanguage() {
        String strThreeToTwoLetterLanguage;
        int length = this.buffer.length();
        if (haveExperimentalLanguagePrefix()) {
            append(AsciiUtil.toLower(this.id[0]));
            append(HYPHEN);
            this.index = 2;
        }
        while (true) {
            char next = next();
            if (isTerminatorOrIDSeparator(next)) {
                break;
            }
            append(AsciiUtil.toLower(next));
        }
        this.index--;
        if (this.buffer.length() - length == 3 && (strThreeToTwoLetterLanguage = LocaleIDs.threeToTwoLetterLanguage(getString(0))) != null) {
            set(0, strThreeToTwoLetterLanguage);
        }
        return 0;
    }

    private void skipLanguage() {
        if (haveExperimentalLanguagePrefix()) {
            this.index = 2;
        }
        skipUntilTerminatorOrIDSeparator();
    }

    private int parseScript() {
        if (!atTerminator()) {
            int i = this.index;
            this.index++;
            int length = this.buffer.length();
            boolean z = true;
            while (true) {
                char next = next();
                if (isTerminatorOrIDSeparator(next) || !AsciiUtil.isAlpha(next)) {
                    break;
                }
                if (z) {
                    addSeparator();
                    append(AsciiUtil.toUpper(next));
                    z = false;
                } else {
                    append(AsciiUtil.toLower(next));
                }
            }
            this.index--;
            if (this.index - i != 5) {
                this.index = i;
                this.buffer.delete(length, this.buffer.length());
                return length;
            }
            return length + 1;
        }
        return this.buffer.length();
    }

    private void skipScript() {
        char next;
        if (!atTerminator()) {
            int i = this.index;
            this.index++;
            do {
                next = next();
                if (isTerminatorOrIDSeparator(next)) {
                    break;
                }
            } while (AsciiUtil.isAlpha(next));
            this.index--;
            if (this.index - i != 5) {
                this.index = i;
            }
        }
    }

    private int parseCountry() {
        String strThreeToTwoLetterRegion;
        if (!atTerminator()) {
            int i = this.index;
            this.index++;
            int length = this.buffer.length();
            boolean z = true;
            while (true) {
                char next = next();
                if (isTerminatorOrIDSeparator(next)) {
                    break;
                }
                if (z) {
                    this.hadCountry = true;
                    addSeparator();
                    length++;
                    z = false;
                }
                append(AsciiUtil.toUpper(next));
            }
            this.index--;
            int length2 = this.buffer.length() - length;
            if (length2 != 0) {
                if (length2 < 2 || length2 > 3) {
                    this.index = i;
                    int i2 = length - 1;
                    this.buffer.delete(i2, this.buffer.length());
                    this.hadCountry = false;
                    return i2;
                }
                if (length2 == 3 && (strThreeToTwoLetterRegion = LocaleIDs.threeToTwoLetterRegion(getString(length))) != null) {
                    set(length, strThreeToTwoLetterRegion);
                    return length;
                }
                return length;
            }
            return length;
        }
        return this.buffer.length();
    }

    private void skipCountry() {
        if (!atTerminator()) {
            if (this.id[this.index] == '_' || this.id[this.index] == '-') {
                this.index++;
            }
            int i = this.index;
            skipUntilTerminatorOrIDSeparator();
            int i2 = this.index - i;
            if (i2 < 2 || i2 > 3) {
                this.index = i;
            }
        }
    }

    private int parseVariant() {
        int length = this.buffer.length();
        boolean z = false;
        boolean z2 = true;
        boolean z3 = true;
        boolean z4 = true;
        while (true) {
            char next = next();
            if (next == 65535) {
                break;
            }
            if (next == '.') {
                z2 = false;
                z = true;
            } else if (next == '@') {
                if (haveKeywordAssign()) {
                    break;
                }
                z2 = false;
                z = false;
                z3 = true;
            } else if (z2) {
                if (next != '_' && next != '-') {
                    this.index--;
                }
                z2 = false;
            } else if (!z) {
                if (z3) {
                    if (z4 && !this.hadCountry) {
                        addSeparator();
                        length++;
                    }
                    addSeparator();
                    if (z4) {
                        length++;
                        z3 = false;
                        z4 = false;
                    } else {
                        z3 = false;
                    }
                }
                char upper = AsciiUtil.toUpper(next);
                if (upper == '-' || upper == ',') {
                    upper = '_';
                }
                append(upper);
            }
        }
        this.index--;
        return length;
    }

    public String getLanguage() {
        reset();
        return getString(parseLanguage());
    }

    public String getScript() {
        reset();
        skipLanguage();
        return getString(parseScript());
    }

    public String getCountry() {
        reset();
        skipLanguage();
        skipScript();
        return getString(parseCountry());
    }

    public String getVariant() {
        reset();
        skipLanguage();
        skipScript();
        skipCountry();
        return getString(parseVariant());
    }

    public String[] getLanguageScriptCountryVariant() {
        reset();
        return new String[]{getString(parseLanguage()), getString(parseScript()), getString(parseCountry()), getString(parseVariant())};
    }

    public void setBaseName(String str) {
        this.baseName = str;
    }

    public void parseBaseName() {
        if (this.baseName != null) {
            set(0, this.baseName);
            return;
        }
        reset();
        parseLanguage();
        parseScript();
        parseCountry();
        parseVariant();
        int length = this.buffer.length();
        if (length > 0) {
            int i = length - 1;
            if (this.buffer.charAt(i) == '_') {
                this.buffer.deleteCharAt(i);
            }
        }
    }

    public String getBaseName() {
        if (this.baseName != null) {
            return this.baseName;
        }
        parseBaseName();
        return getString(0);
    }

    public String getName() {
        parseBaseName();
        parseKeywords();
        return getString(0);
    }

    private boolean setToKeywordStart() {
        for (int i = this.index; i < this.id.length; i++) {
            if (this.id[i] == '@') {
                if (this.canonicalize) {
                    int i2 = i + 1;
                    for (int i3 = i2; i3 < this.id.length; i3++) {
                        if (this.id[i3] == '=') {
                            this.index = i2;
                            return true;
                        }
                    }
                    return false;
                }
                int i4 = i + 1;
                if (i4 < this.id.length) {
                    this.index = i4;
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private static boolean isDoneOrKeywordAssign(char c) {
        return c == 65535 || c == '=';
    }

    private static boolean isDoneOrItemSeparator(char c) {
        return c == 65535 || c == ';';
    }

    private String getKeyword() {
        int i = this.index;
        while (!isDoneOrKeywordAssign(next())) {
        }
        this.index--;
        return AsciiUtil.toLowerString(new String(this.id, i, this.index - i).trim());
    }

    private String getValue() {
        int i = this.index;
        while (!isDoneOrItemSeparator(next())) {
        }
        this.index--;
        return new String(this.id, i, this.index - i).trim();
    }

    private Comparator<String> getKeyComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String str, String str2) {
                return str.compareTo(str2);
            }
        };
    }

    public Map<String, String> getKeywordMap() {
        ?? treeMap;
        if (this.keywords == null) {
            ?? EmptyMap = 0;
            EmptyMap = 0;
            if (setToKeywordStart()) {
                do {
                    String keyword = getKeyword();
                    if (keyword.length() == 0) {
                        break;
                    }
                    char next = next();
                    if (next != '=') {
                        EmptyMap = EmptyMap;
                        if (next != 65535) {
                            break;
                            break;
                        }
                        break;
                    }
                    String value = getValue();
                    EmptyMap = EmptyMap;
                    if (value.length() != 0) {
                        if (EmptyMap == 0) {
                            treeMap = new TreeMap(getKeyComparator());
                        } else {
                            boolean zContainsKey = EmptyMap.containsKey(keyword);
                            EmptyMap = EmptyMap;
                            treeMap = EmptyMap;
                            if (!zContainsKey) {
                            }
                        }
                        treeMap.put(keyword, value);
                        EmptyMap = treeMap;
                    }
                } while (next() == ';');
            }
            if (EmptyMap == 0) {
                EmptyMap = Collections.emptyMap();
            }
            this.keywords = EmptyMap;
        }
        return this.keywords;
    }

    private int parseKeywords() {
        int length = this.buffer.length();
        Map<String, String> keywordMap = getKeywordMap();
        if (!keywordMap.isEmpty()) {
            boolean z = true;
            for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
                append(z ? KEYWORD_SEPARATOR : ITEM_SEPARATOR);
                z = false;
                append(entry.getKey());
                append(KEYWORD_ASSIGN);
                append(entry.getValue());
            }
            if (!z) {
                return length + 1;
            }
            return length;
        }
        return length;
    }

    public Iterator<String> getKeywords() {
        Map<String, String> keywordMap = getKeywordMap();
        if (keywordMap.isEmpty()) {
            return null;
        }
        return keywordMap.keySet().iterator();
    }

    public String getKeywordValue(String str) {
        Map<String, String> keywordMap = getKeywordMap();
        if (keywordMap.isEmpty()) {
            return null;
        }
        return keywordMap.get(AsciiUtil.toLowerString(str.trim()));
    }

    public void defaultKeywordValue(String str, String str2) {
        setKeywordValue(str, str2, false);
    }

    public void setKeywordValue(String str, String str2) {
        setKeywordValue(str, str2, true);
    }

    private void setKeywordValue(String str, String str2, boolean z) {
        if (str == null) {
            if (z) {
                this.keywords = Collections.emptyMap();
                return;
            }
            return;
        }
        String lowerString = AsciiUtil.toLowerString(str.trim());
        if (lowerString.length() == 0) {
            throw new IllegalArgumentException("keyword must not be empty");
        }
        if (str2 != null) {
            str2 = str2.trim();
            if (str2.length() == 0) {
                throw new IllegalArgumentException("value must not be empty");
            }
        }
        Map<String, String> keywordMap = getKeywordMap();
        if (keywordMap.isEmpty()) {
            if (str2 != null) {
                this.keywords = new TreeMap(getKeyComparator());
                this.keywords.put(lowerString, str2.trim());
                return;
            }
            return;
        }
        if (z || !keywordMap.containsKey(lowerString)) {
            if (str2 != null) {
                keywordMap.put(lowerString, str2);
                return;
            }
            keywordMap.remove(lowerString);
            if (keywordMap.isEmpty()) {
                this.keywords = Collections.emptyMap();
            }
        }
    }
}
