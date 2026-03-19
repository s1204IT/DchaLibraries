package android.icu.impl.coll;

import android.icu.impl.IllegalIcuArgumentException;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.text.Normalizer2;
import android.icu.text.PluralRules;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

public final class CollationRuleParser {

    static final boolean f49assertionsDisabled;
    private static final String BEFORE = "[before";
    private static final int OFFSET_SHIFT = 8;
    static final Position[] POSITION_VALUES;
    static final char POS_BASE = 10240;
    static final char POS_LEAD = 65534;
    private static final int STARRED_FLAG = 16;
    private static final int STRENGTH_MASK = 15;
    private static final int UCOL_DEFAULT = -1;
    private static final int UCOL_OFF = 0;
    private static final int UCOL_ON = 1;
    private static final int U_PARSE_CONTEXT_LEN = 16;
    private static final String[] gSpecialReorderCodes;
    private static final String[] positions;
    private final CollationData baseData;
    private Importer importer;
    private int ruleIndex;
    private String rules;
    private CollationSettings settings;
    private Sink sink;
    private final StringBuilder rawBuilder = new StringBuilder();
    private Normalizer2 nfd = Normalizer2.getNFDInstance();
    private Normalizer2 nfc = Normalizer2.getNFCInstance();

    interface Importer {
        String getRules(String str, String str2);
    }

    enum Position {
        FIRST_TERTIARY_IGNORABLE,
        LAST_TERTIARY_IGNORABLE,
        FIRST_SECONDARY_IGNORABLE,
        LAST_SECONDARY_IGNORABLE,
        FIRST_PRIMARY_IGNORABLE,
        LAST_PRIMARY_IGNORABLE,
        FIRST_VARIABLE,
        LAST_VARIABLE,
        FIRST_REGULAR,
        LAST_REGULAR,
        FIRST_IMPLICIT,
        LAST_IMPLICIT,
        FIRST_TRAILING,
        LAST_TRAILING;

        public static Position[] valuesCustom() {
            return values();
        }
    }

    static {
        f49assertionsDisabled = !CollationRuleParser.class.desiredAssertionStatus();
        POSITION_VALUES = Position.valuesCustom();
        positions = new String[]{"first tertiary ignorable", "last tertiary ignorable", "first secondary ignorable", "last secondary ignorable", "first primary ignorable", "last primary ignorable", "first variable", "last variable", "first regular", "last regular", "first implicit", "last implicit", "first trailing", "last trailing"};
        gSpecialReorderCodes = new String[]{"space", "punct", "symbol", "currency", "digit"};
    }

    static abstract class Sink {
        abstract void addRelation(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3);

        abstract void addReset(int i, CharSequence charSequence);

        Sink() {
        }

        void suppressContractions(UnicodeSet set) {
        }

        void optimize(UnicodeSet set) {
        }
    }

    CollationRuleParser(CollationData base) {
        this.baseData = base;
    }

    void setSink(Sink sinkAlias) {
        this.sink = sinkAlias;
    }

    void setImporter(Importer importerAlias) {
        this.importer = importerAlias;
    }

    void parse(String ruleString, CollationSettings outSettings) throws ParseException {
        this.settings = outSettings;
        parse(ruleString);
    }

    private void parse(String ruleString) throws ParseException {
        this.rules = ruleString;
        this.ruleIndex = 0;
        while (this.ruleIndex < this.rules.length()) {
            char c = this.rules.charAt(this.ruleIndex);
            if (PatternProps.isWhiteSpace(c)) {
                this.ruleIndex++;
            } else {
                switch (c) {
                    case '!':
                        this.ruleIndex++;
                        break;
                    case '#':
                        this.ruleIndex = skipComment(this.ruleIndex + 1);
                        break;
                    case '&':
                        parseRuleChain();
                        break;
                    case '@':
                        this.settings.setFlag(2048, true);
                        this.ruleIndex++;
                        break;
                    case '[':
                        parseSetting();
                        break;
                    default:
                        setParseError("expected a reset or setting or comment");
                        break;
                }
            }
        }
    }

    private void parseRuleChain() throws ParseException {
        int resetStrength = parseResetAndPosition();
        boolean isFirstRelation = true;
        while (true) {
            int result = parseRelationOperator();
            if (result < 0) {
                if (this.ruleIndex >= this.rules.length() || this.rules.charAt(this.ruleIndex) != '#') {
                    break;
                } else {
                    this.ruleIndex = skipComment(this.ruleIndex + 1);
                }
            } else {
                int strength = result & 15;
                if (resetStrength < 15) {
                    if (isFirstRelation) {
                        if (strength != resetStrength) {
                            setParseError("reset-before strength differs from its first relation");
                            return;
                        }
                    } else if (strength < resetStrength) {
                        setParseError("reset-before strength followed by a stronger relation");
                        return;
                    }
                }
                int i = this.ruleIndex + (result >> 8);
                if ((result & 16) == 0) {
                    parseRelationStrings(strength, i);
                } else {
                    parseStarredCharacters(strength, i);
                }
                isFirstRelation = false;
            }
        }
    }

    private int parseResetAndPosition() throws ParseException {
        int resetStrength;
        int i;
        int j;
        char c;
        int i2 = skipWhiteSpace(this.ruleIndex + 1);
        if (this.rules.regionMatches(i2, BEFORE, 0, BEFORE.length()) && (j = i2 + BEFORE.length()) < this.rules.length() && PatternProps.isWhiteSpace(this.rules.charAt(j))) {
            int j2 = skipWhiteSpace(j + 1);
            if (j2 + 1 < this.rules.length() && '1' <= (c = this.rules.charAt(j2)) && c <= '3' && this.rules.charAt(j2 + 1) == ']') {
                resetStrength = (c - '1') + 0;
                i2 = skipWhiteSpace(j2 + 2);
            }
        } else {
            resetStrength = 15;
        }
        if (i2 >= this.rules.length()) {
            setParseError("reset without position");
            return -1;
        }
        if (this.rules.charAt(i2) == '[') {
            i = parseSpecialPosition(i2, this.rawBuilder);
        } else {
            i = parseTailoringString(i2, this.rawBuilder);
        }
        try {
            this.sink.addReset(resetStrength, this.rawBuilder);
            this.ruleIndex = i;
            return resetStrength;
        } catch (Exception e) {
            setParseError("adding reset failed", e);
            return -1;
        }
    }

    private int parseRelationOperator() {
        int strength;
        int i;
        this.ruleIndex = skipWhiteSpace(this.ruleIndex);
        if (this.ruleIndex >= this.rules.length()) {
            return -1;
        }
        int i2 = this.ruleIndex;
        int i3 = i2 + 1;
        char c = this.rules.charAt(i2);
        switch (c) {
            case ',':
                strength = 2;
                i = i3;
                break;
            case ';':
                strength = 1;
                i = i3;
                break;
            case '<':
                if (i3 < this.rules.length() && this.rules.charAt(i3) == '<') {
                    i = i3 + 1;
                    if (i < this.rules.length() && this.rules.charAt(i) == '<') {
                        i++;
                        if (i < this.rules.length() && this.rules.charAt(i) == '<') {
                            i++;
                            strength = 3;
                        } else {
                            strength = 2;
                        }
                    } else {
                        strength = 1;
                    }
                } else {
                    strength = 0;
                    i = i3;
                }
                if (i < this.rules.length() && this.rules.charAt(i) == '*') {
                    i++;
                    strength |= 16;
                }
                break;
            case '=':
                strength = 15;
                if (i3 < this.rules.length() && this.rules.charAt(i3) == '*') {
                    i = i3 + 1;
                    strength = 31;
                } else {
                    i = i3;
                }
                break;
            default:
                return -1;
        }
        return ((i - this.ruleIndex) << 8) | strength;
    }

    private void parseRelationStrings(int strength, int i) throws ParseException {
        String prefix = "";
        CharSequence extension = "";
        int i2 = parseTailoringString(i, this.rawBuilder);
        char next = i2 < this.rules.length() ? this.rules.charAt(i2) : (char) 0;
        if (next == '|') {
            prefix = this.rawBuilder.toString();
            i2 = parseTailoringString(i2 + 1, this.rawBuilder);
            next = i2 < this.rules.length() ? this.rules.charAt(i2) : (char) 0;
        }
        if (next == '/') {
            StringBuilder extBuilder = new StringBuilder();
            i2 = parseTailoringString(i2 + 1, extBuilder);
            extension = extBuilder;
        }
        if (prefix.length() != 0) {
            int prefix0 = prefix.codePointAt(0);
            int c = this.rawBuilder.codePointAt(0);
            if (!this.nfc.hasBoundaryBefore(prefix0) || !this.nfc.hasBoundaryBefore(c)) {
                setParseError("in 'prefix|str', prefix and str must each start with an NFC boundary");
                return;
            }
        }
        try {
            this.sink.addRelation(strength, prefix, this.rawBuilder, extension);
            this.ruleIndex = i2;
        } catch (Exception e) {
            setParseError("adding relation failed", e);
        }
    }

    private void parseStarredCharacters(int strength, int i) throws ParseException {
        int i2 = parseString(skipWhiteSpace(i), this.rawBuilder);
        if (this.rawBuilder.length() == 0) {
            setParseError("missing starred-relation string");
            return;
        }
        int prev = -1;
        int j = 0;
        while (true) {
            if (j < this.rawBuilder.length()) {
                int c = this.rawBuilder.codePointAt(j);
                if (this.nfd.isInert(c)) {
                    try {
                        this.sink.addRelation(strength, "", UTF16.valueOf(c), "");
                        j += Character.charCount(c);
                        prev = c;
                    } catch (Exception e) {
                        setParseError("adding relation failed", e);
                        return;
                    }
                } else {
                    setParseError("starred-relation string is not all NFD-inert");
                    return;
                }
            } else if (i2 < this.rules.length() && this.rules.charAt(i2) == '-') {
                if (prev < 0) {
                    setParseError("range without start in starred-relation string");
                    return;
                }
                i2 = parseString(i2 + 1, this.rawBuilder);
                if (this.rawBuilder.length() == 0) {
                    setParseError("range without end in starred-relation string");
                    return;
                }
                int c2 = this.rawBuilder.codePointAt(0);
                if (c2 >= prev) {
                    while (true) {
                        prev++;
                        if (prev <= c2) {
                            if (!this.nfd.isInert(prev)) {
                                setParseError("starred-relation string range is not all NFD-inert");
                                return;
                            }
                            if (isSurrogate(prev)) {
                                setParseError("starred-relation string range contains a surrogate");
                                return;
                            }
                            if (65533 > prev || prev > 65535) {
                                try {
                                    this.sink.addRelation(strength, "", UTF16.valueOf(prev), "");
                                } catch (Exception e2) {
                                    setParseError("adding relation failed", e2);
                                    return;
                                }
                            } else {
                                setParseError("starred-relation string range contains U+FFFD, U+FFFE or U+FFFF");
                                return;
                            }
                        } else {
                            prev = -1;
                            j = Character.charCount(c2);
                            break;
                        }
                    }
                } else {
                    setParseError("range start greater than end in starred-relation string");
                    return;
                }
            } else {
                break;
            }
        }
    }

    private int parseTailoringString(int i, StringBuilder raw) throws ParseException {
        int i2 = parseString(skipWhiteSpace(i), raw);
        if (raw.length() == 0) {
            setParseError("missing relation string");
        }
        return skipWhiteSpace(i2);
    }

    private int parseString(int i, StringBuilder raw) throws ParseException {
        raw.setLength(0);
        while (true) {
            if (i >= this.rules.length()) {
                break;
            }
            int i2 = i + 1;
            char c = this.rules.charAt(i);
            if (isSyntaxChar(c)) {
                if (c == '\'') {
                    if (i2 < this.rules.length() && this.rules.charAt(i2) == '\'') {
                        raw.append(PatternTokenizer.SINGLE_QUOTE);
                        i = i2 + 1;
                    } else {
                        int i3 = i2;
                        while (i3 != this.rules.length()) {
                            int i4 = i3 + 1;
                            char c2 = this.rules.charAt(i3);
                            if (c2 != '\'') {
                                i3 = i4;
                            } else if (i4 >= this.rules.length() || this.rules.charAt(i4) != '\'') {
                                i = i4;
                            } else {
                                i3 = i4 + 1;
                            }
                            raw.append(c2);
                        }
                        setParseError("quoted literal text missing terminating apostrophe");
                        return i3;
                    }
                } else if (c == '\\') {
                    if (i2 == this.rules.length()) {
                        setParseError("backslash escape at the end of the rule string");
                        return i2;
                    }
                    int cp = this.rules.codePointAt(i2);
                    raw.appendCodePoint(cp);
                    i = i2 + Character.charCount(cp);
                } else {
                    i = i2 - 1;
                    break;
                }
            } else {
                if (PatternProps.isWhiteSpace(c)) {
                    i = i2 - 1;
                    break;
                }
                raw.append(c);
                i = i2;
            }
        }
        int j = 0;
        while (j < raw.length()) {
            int c3 = raw.codePointAt(j);
            if (isSurrogate(c3)) {
                setParseError("string contains an unpaired surrogate");
                return i;
            }
            if (65533 <= c3 && c3 <= 65535) {
                setParseError("string contains U+FFFD, U+FFFE or U+FFFF");
                return i;
            }
            j += Character.charCount(c3);
        }
        return i;
    }

    private static final boolean isSurrogate(int c) {
        return (c & (-2048)) == 55296;
    }

    private int parseSpecialPosition(int i, StringBuilder str) throws ParseException {
        int j = readWords(i + 1, this.rawBuilder);
        if (j > i && this.rules.charAt(j) == ']' && this.rawBuilder.length() != 0) {
            int j2 = j + 1;
            String raw = this.rawBuilder.toString();
            str.setLength(0);
            for (int pos = 0; pos < positions.length; pos++) {
                if (raw.equals(positions[pos])) {
                    str.append(POS_LEAD).append((char) (pos + 10240));
                    return j2;
                }
            }
            if (raw.equals("top")) {
                str.append(POS_LEAD).append((char) (Position.LAST_REGULAR.ordinal() + 10240));
                return j2;
            }
            if (raw.equals("variable top")) {
                str.append(POS_LEAD).append((char) (Position.LAST_VARIABLE.ordinal() + 10240));
                return j2;
            }
        }
        setParseError("not a valid special reset position");
        return i;
    }

    private void parseSetting() throws ParseException {
        String v;
        int i = this.ruleIndex + 1;
        int j = readWords(i, this.rawBuilder);
        if (j <= i || this.rawBuilder.length() == 0) {
            setParseError("expected a setting/option at '['");
        }
        String raw = this.rawBuilder.toString();
        if (this.rules.charAt(j) == ']') {
            int j2 = j + 1;
            if (raw.startsWith("reorder") && (raw.length() == 7 || raw.charAt(7) == ' ')) {
                parseReordering(raw);
                this.ruleIndex = j2;
                return;
            }
            if (raw.equals("backwards 2")) {
                this.settings.setFlag(2048, true);
                this.ruleIndex = j2;
                return;
            }
            int valueIndex = raw.lastIndexOf(32);
            if (valueIndex >= 0) {
                v = raw.substring(valueIndex + 1);
                raw = raw.substring(0, valueIndex);
            } else {
                v = "";
            }
            if (raw.equals("strength") && v.length() == 1) {
                int value = -1;
                char c = v.charAt(0);
                if ('1' <= c && c <= '4') {
                    value = (c - '1') + 0;
                } else if (c == 'I') {
                    value = 15;
                }
                if (value != -1) {
                    this.settings.setStrength(value);
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("alternate")) {
                int value2 = -1;
                if (v.equals("non-ignorable")) {
                    value2 = 0;
                } else if (v.equals("shifted")) {
                    value2 = 1;
                }
                if (value2 != -1) {
                    this.settings.setAlternateHandlingShifted(value2 > 0);
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("maxVariable")) {
                int value3 = -1;
                if (v.equals("space")) {
                    value3 = 0;
                } else if (v.equals("punct")) {
                    value3 = 1;
                } else if (v.equals("symbol")) {
                    value3 = 2;
                } else if (v.equals("currency")) {
                    value3 = 3;
                }
                if (value3 != -1) {
                    this.settings.setMaxVariable(value3, 0);
                    this.settings.variableTop = this.baseData.getLastPrimaryForGroup(value3 + 4096);
                    if (!f49assertionsDisabled) {
                        if (!(this.settings.variableTop != 0)) {
                            throw new AssertionError();
                        }
                    }
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("caseFirst")) {
                int value4 = -1;
                if (v.equals("off")) {
                    value4 = 0;
                } else if (v.equals("lower")) {
                    value4 = 512;
                } else if (v.equals("upper")) {
                    value4 = 768;
                }
                if (value4 != -1) {
                    this.settings.setCaseFirst(value4);
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("caseLevel")) {
                int value5 = getOnOffValue(v);
                if (value5 != -1) {
                    this.settings.setFlag(1024, value5 > 0);
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("normalization")) {
                int value6 = getOnOffValue(v);
                if (value6 != -1) {
                    this.settings.setFlag(1, value6 > 0);
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("numericOrdering")) {
                int value7 = getOnOffValue(v);
                if (value7 != -1) {
                    this.settings.setFlag(2, value7 > 0);
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("hiraganaQ")) {
                int value8 = getOnOffValue(v);
                if (value8 != -1) {
                    if (value8 == 1) {
                        setParseError("[hiraganaQ on] is not supported");
                    }
                    this.ruleIndex = j2;
                    return;
                }
            } else if (raw.equals("import")) {
                try {
                    ULocale localeID = new ULocale.Builder().setLanguageTag(v).build();
                    String baseID = localeID.getBaseName();
                    String collationType = localeID.getKeywordValue("collation");
                    if (this.importer == null) {
                        setParseError("[import langTag] is not supported");
                        return;
                    }
                    try {
                        Importer importer = this.importer;
                        if (collationType == null) {
                            collationType = "standard";
                        }
                        String importedRules = importer.getRules(baseID, collationType);
                        String outerRules = this.rules;
                        int outerRuleIndex = this.ruleIndex;
                        try {
                            parse(importedRules);
                        } catch (Exception e) {
                            this.ruleIndex = outerRuleIndex;
                            setParseError("parsing imported rules failed", e);
                        }
                        this.rules = outerRules;
                        this.ruleIndex = j2;
                        return;
                    } catch (Exception e2) {
                        setParseError("[import langTag] failed", e2);
                        return;
                    }
                } catch (Exception e3) {
                    setParseError("expected language tag in [import langTag]", e3);
                    return;
                }
            }
        } else if (this.rules.charAt(j) == '[') {
            UnicodeSet set = new UnicodeSet();
            int j3 = parseUnicodeSet(j, set);
            if (raw.equals("optimize")) {
                try {
                    this.sink.optimize(set);
                } catch (Exception e4) {
                    setParseError("[optimize set] failed", e4);
                }
                this.ruleIndex = j3;
                return;
            }
            if (raw.equals("suppressContractions")) {
                try {
                    this.sink.suppressContractions(set);
                } catch (Exception e5) {
                    setParseError("[suppressContractions set] failed", e5);
                }
                this.ruleIndex = j3;
                return;
            }
        }
        setParseError("not a valid setting/option");
    }

    private void parseReordering(CharSequence raw) throws ParseException {
        int limit;
        if (7 == raw.length()) {
            this.settings.resetReordering();
            return;
        }
        ArrayList<Integer> reorderCodes = new ArrayList<>();
        for (int i = 7; i < raw.length(); i = limit) {
            int i2 = i + 1;
            limit = i2;
            while (limit < raw.length() && raw.charAt(limit) != ' ') {
                limit++;
            }
            String word = raw.subSequence(i2, limit).toString();
            int code = getReorderCode(word);
            if (code < 0) {
                setParseError("unknown script or reorder code");
                return;
            }
            reorderCodes.add(Integer.valueOf(code));
        }
        if (reorderCodes.isEmpty()) {
            this.settings.resetReordering();
            return;
        }
        int[] codes = new int[reorderCodes.size()];
        int j = 0;
        Iterator code$iterator = reorderCodes.iterator();
        while (code$iterator.hasNext()) {
            codes[j] = ((Integer) code$iterator.next()).intValue();
            j++;
        }
        this.settings.setReordering(this.baseData, codes);
    }

    public static int getReorderCode(String word) {
        for (int i = 0; i < gSpecialReorderCodes.length; i++) {
            if (word.equalsIgnoreCase(gSpecialReorderCodes[i])) {
                return i + 4096;
            }
        }
        try {
            int script = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, word);
            if (script >= 0) {
                return script;
            }
        } catch (IllegalIcuArgumentException e) {
        }
        if (word.equalsIgnoreCase("others")) {
            return 103;
        }
        return -1;
    }

    private static int getOnOffValue(String s) {
        if (s.equals("on")) {
            return 1;
        }
        if (s.equals("off")) {
            return 0;
        }
        return -1;
    }

    private int parseUnicodeSet(int i, UnicodeSet set) throws ParseException {
        int level = 0;
        int j = i;
        while (j != this.rules.length()) {
            int j2 = j + 1;
            char c = this.rules.charAt(j);
            if (c == '[') {
                level++;
            } else if (c == ']' && level - 1 == 0) {
                try {
                    set.applyPattern(this.rules.substring(i, j2));
                } catch (Exception e) {
                    setParseError("not a valid UnicodeSet pattern: " + e.getMessage());
                }
                int j3 = skipWhiteSpace(j2);
                if (j3 == this.rules.length() || this.rules.charAt(j3) != ']') {
                    setParseError("missing option-terminating ']' after UnicodeSet pattern");
                    return j3;
                }
                return j3 + 1;
            }
            j = j2;
        }
        setParseError("unbalanced UnicodeSet pattern brackets");
        return j;
    }

    private int readWords(int i, StringBuilder raw) {
        raw.setLength(0);
        int i2 = skipWhiteSpace(i);
        while (i2 < this.rules.length()) {
            char c = this.rules.charAt(i2);
            if (isSyntaxChar(c) && c != '-' && c != '_') {
                if (raw.length() == 0) {
                    return i2;
                }
                int lastIndex = raw.length() - 1;
                if (raw.charAt(lastIndex) == ' ') {
                    raw.setLength(lastIndex);
                }
                return i2;
            }
            if (PatternProps.isWhiteSpace(c)) {
                raw.append(' ');
                i2 = skipWhiteSpace(i2 + 1);
            } else {
                raw.append(c);
                i2++;
            }
        }
        return 0;
    }

    private int skipComment(int i) {
        while (i < this.rules.length()) {
            int i2 = i + 1;
            char c = this.rules.charAt(i);
            if (c == '\n' || c == '\f' || c == '\r' || c == 133 || c == 8232 || c == 8233) {
                return i2;
            }
            i = i2;
        }
        return i;
    }

    private void setParseError(String reason) throws ParseException {
        throw makeParseException(reason);
    }

    private void setParseError(String reason, Exception e) throws ParseException {
        ParseException newExc = makeParseException(reason + PluralRules.KEYWORD_RULE_SEPARATOR + e.getMessage());
        newExc.initCause(e);
        throw newExc;
    }

    private ParseException makeParseException(String reason) {
        return new ParseException(appendErrorContext(reason), this.ruleIndex);
    }

    private String appendErrorContext(String reason) {
        StringBuilder msg = new StringBuilder(reason);
        msg.append(" at index ").append(this.ruleIndex);
        msg.append(" near \"");
        int start = this.ruleIndex - 15;
        if (start < 0) {
            start = 0;
        } else if (start > 0 && Character.isLowSurrogate(this.rules.charAt(start))) {
            start++;
        }
        msg.append((CharSequence) this.rules, start, this.ruleIndex);
        msg.append('!');
        int length = this.rules.length() - this.ruleIndex;
        if (length >= 16) {
            length = 15;
            if (Character.isHighSurrogate(this.rules.charAt((this.ruleIndex + 15) - 1))) {
                length = 14;
            }
        }
        msg.append((CharSequence) this.rules, this.ruleIndex, this.ruleIndex + length);
        return msg.append('\"').toString();
    }

    private static boolean isSyntaxChar(int c) {
        if (33 > c || c > 126) {
            return false;
        }
        if (c <= 47) {
            return true;
        }
        if (58 > c || c > 64) {
            return (91 <= c && c <= 96) || 123 <= c;
        }
        return true;
    }

    private int skipWhiteSpace(int i) {
        while (i < this.rules.length() && PatternProps.isWhiteSpace(this.rules.charAt(i))) {
            i++;
        }
        return i;
    }
}
