package android.icu.text;

import android.icu.impl.IllegalIcuArgumentException;
import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Normalizer;
import android.icu.text.RuleBasedTransliterator;
import android.icu.text.TransliteratorIDParser;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransliteratorParser {
    private static final char ALT_FORWARD_RULE_OP = 8594;
    private static final char ALT_FUNCTION = 8710;
    private static final char ALT_FWDREV_RULE_OP = 8596;
    private static final char ALT_REVERSE_RULE_OP = 8592;
    private static final char ANCHOR_START = '^';
    private static final char CONTEXT_ANTE = '{';
    private static final char CONTEXT_POST = '}';
    private static final char CURSOR_OFFSET = '@';
    private static final char CURSOR_POS = '|';
    private static final char DOT = '.';
    private static final String DOT_SET = "[^[:Zp:][:Zl:]\\r\\n$]";
    private static final char END_OF_RULE = ';';
    private static final char ESCAPE = '\\';
    private static final char FORWARD_RULE_OP = '>';
    private static final char FUNCTION = '&';
    private static final char FWDREV_RULE_OP = '~';
    private static final String HALF_ENDERS = "=><←→↔;";
    private static final String ID_TOKEN = "::";
    private static final int ID_TOKEN_LEN = 2;
    private static final char KLEENE_STAR = '*';
    private static final char ONE_OR_MORE = '+';
    private static final String OPERATORS = "=><←→↔";
    private static final char QUOTE = '\'';
    private static final char REVERSE_RULE_OP = '<';
    private static final char RULE_COMMENT_CHAR = '#';
    private static final char SEGMENT_CLOSE = ')';
    private static final char SEGMENT_OPEN = '(';
    private static final char VARIABLE_DEF_OP = '=';
    private static final char ZERO_OR_ONE = '?';
    public UnicodeSet compoundFilter;
    private RuleBasedTransliterator.Data curData;
    public List<RuleBasedTransliterator.Data> dataVector;
    private int direction;
    private int dotStandIn = -1;
    public List<String> idBlockVector;
    private ParseData parseData;
    private List<StringMatcher> segmentObjects;
    private StringBuffer segmentStandins;
    private String undefinedVariableName;
    private char variableLimit;
    private Map<String, char[]> variableNames;
    private char variableNext;
    private List<Object> variablesVector;
    private static UnicodeSet ILLEGAL_TOP = new UnicodeSet("[\\)]");
    private static UnicodeSet ILLEGAL_SEG = new UnicodeSet("[\\{\\}\\|\\@]");
    private static UnicodeSet ILLEGAL_FUNC = new UnicodeSet("[\\^\\(\\.\\*\\+\\?\\{\\}\\|\\@]");

    private class ParseData implements SymbolTable {
        ParseData(TransliteratorParser this$0, ParseData parseData) {
            this();
        }

        private ParseData() {
        }

        @Override
        public char[] lookup(String name) {
            return (char[]) TransliteratorParser.this.variableNames.get(name);
        }

        @Override
        public UnicodeMatcher lookupMatcher(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i >= 0 && i < TransliteratorParser.this.variablesVector.size()) {
                return (UnicodeMatcher) TransliteratorParser.this.variablesVector.get(i);
            }
            return null;
        }

        @Override
        public String parseReference(String text, ParsePosition pos, int limit) {
            int start = pos.getIndex();
            int i = start;
            while (i < limit) {
                char c = text.charAt(i);
                if ((i == start && !UCharacter.isUnicodeIdentifierStart(c)) || !UCharacter.isUnicodeIdentifierPart(c)) {
                    break;
                }
                i++;
            }
            if (i == start) {
                return null;
            }
            pos.setIndex(i);
            return text.substring(start, i);
        }

        public boolean isMatcher(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i >= 0 && i < TransliteratorParser.this.variablesVector.size()) {
                return TransliteratorParser.this.variablesVector.get(i) instanceof UnicodeMatcher;
            }
            return true;
        }

        public boolean isReplacer(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i >= 0 && i < TransliteratorParser.this.variablesVector.size()) {
                return TransliteratorParser.this.variablesVector.get(i) instanceof UnicodeReplacer;
            }
            return true;
        }
    }

    private static abstract class RuleBody {
        RuleBody(RuleBody ruleBody) {
            this();
        }

        abstract String handleNextLine();

        abstract void reset();

        private RuleBody() {
        }

        String nextLine() {
            String s;
            String s2 = handleNextLine();
            if (s2 != null && s2.length() > 0 && s2.charAt(s2.length() - 1) == '\\') {
                StringBuilder b = new StringBuilder(s2);
                do {
                    b.deleteCharAt(b.length() - 1);
                    s = handleNextLine();
                    if (s != null) {
                        b.append(s);
                        if (s.length() <= 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                } while (s.charAt(s.length() - 1) == '\\');
                return b.toString();
            }
            return s2;
        }
    }

    private static class RuleArray extends RuleBody {
        String[] array;
        int i;

        public RuleArray(String[] array) {
            super(null);
            this.array = array;
            this.i = 0;
        }

        @Override
        public String handleNextLine() {
            if (this.i >= this.array.length) {
                return null;
            }
            String[] strArr = this.array;
            int i = this.i;
            this.i = i + 1;
            return strArr[i];
        }

        @Override
        public void reset() {
            this.i = 0;
        }
    }

    private static class RuleHalf {
        public boolean anchorEnd;
        public boolean anchorStart;
        public int ante;
        public int cursor;
        public int cursorOffset;
        private int cursorOffsetPos;
        private int nextSegmentNumber;
        public int post;
        public String text;

        RuleHalf(RuleHalf ruleHalf) {
            this();
        }

        private RuleHalf() {
            this.cursor = -1;
            this.ante = -1;
            this.post = -1;
            this.cursorOffset = 0;
            this.cursorOffsetPos = 0;
            this.anchorStart = false;
            this.anchorEnd = false;
            this.nextSegmentNumber = 1;
        }

        public int parse(String rule, int pos, int limit, TransliteratorParser parser) {
            StringBuffer buf = new StringBuffer();
            int pos2 = parseSection(rule, pos, limit, parser, buf, TransliteratorParser.ILLEGAL_TOP, false);
            this.text = buf.toString();
            if (this.cursorOffset > 0 && this.cursor != this.cursorOffsetPos) {
                TransliteratorParser.syntaxError("Misplaced |", rule, pos);
            }
            return pos2;
        }

        private int parseSection(String rule, int pos, int limit, TransliteratorParser parser, StringBuffer buf, UnicodeSet illegal, boolean isSegment) {
            int qstart;
            int qlimit;
            ParsePosition pp = null;
            int quoteStart = -1;
            int quoteLimit = -1;
            int varStart = -1;
            int varLimit = -1;
            int[] iref = new int[1];
            int bufStart = buf.length();
            while (true) {
                int pos2 = pos;
                if (pos2 >= limit) {
                    return pos2;
                }
                pos = pos2 + 1;
                char c = rule.charAt(pos2);
                if (!PatternProps.isWhiteSpace(c)) {
                    if (TransliteratorParser.HALF_ENDERS.indexOf(c) >= 0) {
                        if (isSegment) {
                            TransliteratorParser.syntaxError("Unclosed segment", rule, pos);
                            return pos;
                        }
                        return pos;
                    }
                    if (this.anchorEnd) {
                        TransliteratorParser.syntaxError("Malformed variable reference", rule, pos);
                    }
                    if (UnicodeSet.resemblesPattern(rule, pos - 1)) {
                        if (pp == null) {
                            pp = new ParsePosition(0);
                        }
                        pp.setIndex(pos - 1);
                        buf.append(parser.parseSet(rule, pp));
                        pos = pp.getIndex();
                    } else if (c == '\\') {
                        if (pos == limit) {
                            TransliteratorParser.syntaxError("Trailing backslash", rule, pos);
                        }
                        iref[0] = pos;
                        int escaped = Utility.unescapeAt(rule, iref);
                        pos = iref[0];
                        if (escaped == -1) {
                            TransliteratorParser.syntaxError("Malformed escape", rule, pos);
                        }
                        parser.checkVariableRange(escaped, rule, pos);
                        UTF16.append(buf, escaped);
                    } else if (c == '\'') {
                        int iq = rule.indexOf(39, pos);
                        if (iq == pos) {
                            buf.append(c);
                            pos++;
                        } else {
                            quoteStart = buf.length();
                            while (true) {
                                if (iq < 0) {
                                    TransliteratorParser.syntaxError("Unterminated quote", rule, pos);
                                }
                                buf.append(rule.substring(pos, iq));
                                pos = iq + 1;
                                if (pos < limit && rule.charAt(pos) == '\'') {
                                    iq = rule.indexOf(39, pos + 1);
                                }
                            }
                            quoteLimit = buf.length();
                            for (int iq2 = quoteStart; iq2 < quoteLimit; iq2++) {
                                parser.checkVariableRange(buf.charAt(iq2), rule, pos);
                            }
                        }
                    } else {
                        parser.checkVariableRange(c, rule, pos);
                        if (illegal.contains(c)) {
                            TransliteratorParser.syntaxError("Illegal character '" + c + '\'', rule, pos);
                        }
                        switch (c) {
                            case '$':
                                if (pos == limit) {
                                    this.anchorEnd = true;
                                } else {
                                    int r = UCharacter.digit(rule.charAt(pos), 10);
                                    if (r >= 1 && r <= 9) {
                                        iref[0] = pos;
                                        int r2 = Utility.parseNumber(rule, iref, 10);
                                        if (r2 < 0) {
                                            TransliteratorParser.syntaxError("Undefined segment reference", rule, pos);
                                        }
                                        pos = iref[0];
                                        buf.append(parser.getSegmentStandin(r2));
                                    } else {
                                        if (pp == null) {
                                            pp = new ParsePosition(0);
                                        }
                                        pp.setIndex(pos);
                                        String name = parser.parseData.parseReference(rule, pp, limit);
                                        if (name == null) {
                                            this.anchorEnd = true;
                                        } else {
                                            pos = pp.getIndex();
                                            varStart = buf.length();
                                            parser.appendVariableDef(name, buf);
                                            varLimit = buf.length();
                                        }
                                    }
                                }
                                break;
                            case '&':
                            case 8710:
                                iref[0] = pos;
                                TransliteratorIDParser.SingleID single = TransliteratorIDParser.parseFilterID(rule, iref);
                                if (single == null || !Utility.parseChar(rule, iref, TransliteratorParser.SEGMENT_OPEN)) {
                                    TransliteratorParser.syntaxError("Invalid function", rule, pos);
                                }
                                Transliterator t = single.getInstance();
                                if (t == null) {
                                    TransliteratorParser.syntaxError("Invalid function ID", rule, pos);
                                }
                                int bufSegStart = buf.length();
                                pos = parseSection(rule, iref[0], limit, parser, buf, TransliteratorParser.ILLEGAL_FUNC, true);
                                FunctionReplacer r3 = new FunctionReplacer(t, new StringReplacer(buf.substring(bufSegStart), parser.curData));
                                buf.setLength(bufSegStart);
                                buf.append(parser.generateStandInFor(r3));
                                break;
                            case '(':
                                int bufSegStart2 = buf.length();
                                int segmentNumber = this.nextSegmentNumber;
                                this.nextSegmentNumber = segmentNumber + 1;
                                pos = parseSection(rule, pos, limit, parser, buf, TransliteratorParser.ILLEGAL_SEG, true);
                                StringMatcher m = new StringMatcher(buf.substring(bufSegStart2), segmentNumber, parser.curData);
                                parser.setSegmentObject(segmentNumber, m);
                                buf.setLength(bufSegStart2);
                                buf.append(parser.getSegmentStandin(segmentNumber));
                                break;
                            case ')':
                                return pos;
                            case '*':
                            case '+':
                            case '?':
                                if (isSegment && buf.length() == bufStart) {
                                    TransliteratorParser.syntaxError("Misplaced quantifier", rule, pos);
                                } else {
                                    if (buf.length() == quoteLimit) {
                                        qstart = quoteStart;
                                        qlimit = quoteLimit;
                                    } else if (buf.length() == varLimit) {
                                        qstart = varStart;
                                        qlimit = varLimit;
                                    } else {
                                        qstart = buf.length() - 1;
                                        qlimit = qstart + 1;
                                    }
                                    try {
                                        UnicodeMatcher m2 = new StringMatcher(buf.toString(), qstart, qlimit, 0, parser.curData);
                                        int min = 0;
                                        int max = Integer.MAX_VALUE;
                                        switch (c) {
                                            case '+':
                                                min = 1;
                                                break;
                                            case '?':
                                                min = 0;
                                                max = 1;
                                                break;
                                        }
                                        UnicodeMatcher m3 = new Quantifier(m2, min, max);
                                        buf.setLength(qstart);
                                        buf.append(parser.generateStandInFor(m3));
                                    } catch (RuntimeException e) {
                                        String precontext = pos < 50 ? rule.substring(0, pos) : "..." + rule.substring(pos - 50, pos);
                                        String postContext = limit - pos <= 50 ? rule.substring(pos, limit) : rule.substring(pos, pos + 50) + "...";
                                        throw new IllegalIcuArgumentException("Failure in rule: " + precontext + "$$$" + postContext).initCause((Throwable) e);
                                    }
                                }
                                break;
                            case '.':
                                buf.append(parser.getDotStandIn());
                                break;
                            case '@':
                                if (this.cursorOffset < 0) {
                                    if (buf.length() > 0) {
                                        TransliteratorParser.syntaxError("Misplaced " + c, rule, pos);
                                    }
                                    this.cursorOffset--;
                                } else if (this.cursorOffset > 0) {
                                    if (buf.length() != this.cursorOffsetPos || this.cursor >= 0) {
                                        TransliteratorParser.syntaxError("Misplaced " + c, rule, pos);
                                    }
                                    this.cursorOffset++;
                                } else if (this.cursor == 0 && buf.length() == 0) {
                                    this.cursorOffset = -1;
                                } else if (this.cursor >= 0) {
                                    TransliteratorParser.syntaxError("Misplaced " + c, rule, pos);
                                } else {
                                    this.cursorOffsetPos = buf.length();
                                    this.cursorOffset = 1;
                                }
                                break;
                            case '^':
                                if (buf.length() != 0 || this.anchorStart) {
                                    TransliteratorParser.syntaxError("Misplaced anchor start", rule, pos);
                                } else {
                                    this.anchorStart = true;
                                }
                                break;
                            case '{':
                                if (this.ante >= 0) {
                                    TransliteratorParser.syntaxError("Multiple ante contexts", rule, pos);
                                }
                                this.ante = buf.length();
                                break;
                            case '|':
                                if (this.cursor >= 0) {
                                    TransliteratorParser.syntaxError("Multiple cursors", rule, pos);
                                }
                                this.cursor = buf.length();
                                break;
                            case '}':
                                if (this.post >= 0) {
                                    TransliteratorParser.syntaxError("Multiple post contexts", rule, pos);
                                }
                                this.post = buf.length();
                                break;
                            default:
                                if (c >= '!' && c <= '~' && ((c < '0' || c > '9') && ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')))) {
                                    TransliteratorParser.syntaxError("Unquoted " + c, rule, pos);
                                }
                                buf.append(c);
                                break;
                        }
                    }
                }
            }
        }

        void removeContext() {
            this.text = this.text.substring(this.ante < 0 ? 0 : this.ante, this.post < 0 ? this.text.length() : this.post);
            this.post = -1;
            this.ante = -1;
            this.anchorEnd = false;
            this.anchorStart = false;
        }

        public boolean isValidOutput(TransliteratorParser parser) {
            int i = 0;
            while (i < this.text.length()) {
                int c = UTF16.charAt(this.text, i);
                i += UTF16.getCharCount(c);
                if (!parser.parseData.isReplacer(c)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isValidInput(TransliteratorParser parser) {
            int i = 0;
            while (i < this.text.length()) {
                int c = UTF16.charAt(this.text, i);
                i += UTF16.getCharCount(c);
                if (!parser.parseData.isMatcher(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void parse(String rules, int dir) {
        parseRules(new RuleArray(new String[]{rules}), dir);
    }

    void parseRules(RuleBody ruleArray, int dir) {
        int i;
        RuntimeException previous;
        int i2;
        int pos;
        boolean parsingIDs = true;
        int ruleCount = 0;
        this.dataVector = new ArrayList();
        this.idBlockVector = new ArrayList();
        this.curData = null;
        this.direction = dir;
        this.compoundFilter = null;
        this.variablesVector = new ArrayList();
        this.variableNames = new HashMap();
        this.parseData = new ParseData(this, null);
        List<RuntimeException> errors = new ArrayList<>();
        int errorCount = 0;
        ruleArray.reset();
        StringBuilder idBlockResult = new StringBuilder();
        this.compoundFilter = null;
        int compoundFilterOffset = -1;
        loop0: while (true) {
            String rule = ruleArray.nextLine();
            if (rule == null) {
                break;
            }
            int limit = rule.length();
            int pos2 = 0;
            while (pos2 < limit) {
                int pos3 = pos2 + 1;
                char c = rule.charAt(pos2);
                if (PatternProps.isWhiteSpace(c)) {
                    pos2 = pos3;
                } else if (c == '#') {
                    int pos4 = rule.indexOf("\n", pos3) + 1;
                    if (pos4 != 0) {
                        pos2 = pos4;
                    }
                } else if (c == ';') {
                    pos2 = pos3;
                } else {
                    ruleCount++;
                    int pos5 = pos3 - 1;
                    if (pos5 + 2 + 1 <= limit) {
                        try {
                            if (rule.regionMatches(pos5, ID_TOKEN, 0, 2)) {
                                int pos6 = pos5 + 2;
                                char c2 = rule.charAt(pos6);
                                while (PatternProps.isWhiteSpace(c2) && pos6 < limit) {
                                    pos6++;
                                    c2 = rule.charAt(pos6);
                                }
                                int[] p = {pos6};
                                if (!parsingIDs) {
                                    if (this.curData != null) {
                                        if (this.direction == 0) {
                                            this.dataVector.add(this.curData);
                                        } else {
                                            this.dataVector.add(0, this.curData);
                                        }
                                        this.curData = null;
                                    }
                                    parsingIDs = true;
                                }
                                TransliteratorIDParser.SingleID id = TransliteratorIDParser.parseSingleID(rule, p, this.direction);
                                if (p[0] != pos6 && Utility.parseChar(rule, p, END_OF_RULE)) {
                                    if (this.direction == 0) {
                                        idBlockResult.append(id.canonID).append(END_OF_RULE);
                                    } else {
                                        idBlockResult.insert(0, id.canonID + END_OF_RULE);
                                    }
                                } else {
                                    int[] withParens = {-1};
                                    UnicodeSet f = TransliteratorIDParser.parseGlobalFilter(rule, p, this.direction, withParens, null);
                                    if (f != null && Utility.parseChar(rule, p, END_OF_RULE)) {
                                        if ((this.direction == 0) == (withParens[0] == 0)) {
                                            if (this.compoundFilter != null) {
                                                syntaxError("Multiple global filters", rule, pos6);
                                            }
                                            this.compoundFilter = f;
                                            compoundFilterOffset = ruleCount;
                                        }
                                    } else {
                                        syntaxError("Invalid ::ID", rule, pos6);
                                    }
                                }
                                pos = p[0];
                            } else {
                                if (parsingIDs) {
                                    if (this.direction == 0) {
                                        this.idBlockVector.add(idBlockResult.toString());
                                    } else {
                                        this.idBlockVector.add(0, idBlockResult.toString());
                                    }
                                    idBlockResult.delete(0, idBlockResult.length());
                                    parsingIDs = false;
                                    this.curData = new RuleBasedTransliterator.Data();
                                    setVariableRange(61440, 63743);
                                }
                                if (resemblesPragma(rule, pos5, limit)) {
                                    int ppp = parsePragma(rule, pos5, limit);
                                    if (ppp < 0) {
                                        syntaxError("Unrecognized pragma", rule, pos5);
                                    }
                                    pos = ppp;
                                } else {
                                    pos = parseRule(rule, pos5, limit);
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            if (errorCount == 30) {
                                IllegalIcuArgumentException icuEx = new IllegalIcuArgumentException("\nMore than 30 errors; further messages squelched");
                                icuEx.initCause((Throwable) e);
                                errors.add(icuEx);
                                if (!parsingIDs) {
                                    if (!parsingIDs) {
                                        if (this.direction != 0) {
                                        }
                                    }
                                }
                                while (i < this.dataVector.size()) {
                                }
                                this.variablesVector = null;
                                if (this.compoundFilter == null) {
                                }
                                while (i2 < this.dataVector.size()) {
                                }
                                if (this.idBlockVector.size() == 1) {
                                    this.idBlockVector.remove(0);
                                }
                                if (errors.size() != 0) {
                                }
                            } else {
                                e.fillInStackTrace();
                                errors.add(e);
                                errorCount++;
                                pos = ruleEnd(rule, pos5, limit) + 1;
                            }
                        }
                        pos2 = pos;
                    }
                }
            }
        }
        if (!parsingIDs && idBlockResult.length() > 0) {
            if (this.direction == 0) {
                this.idBlockVector.add(idBlockResult.toString());
            } else {
                this.idBlockVector.add(0, idBlockResult.toString());
            }
        } else if (!parsingIDs && this.curData != null) {
            if (this.direction != 0) {
                this.dataVector.add(this.curData);
            } else {
                this.dataVector.add(0, this.curData);
            }
        }
        for (i = 0; i < this.dataVector.size(); i++) {
            RuleBasedTransliterator.Data data = this.dataVector.get(i);
            data.variables = new Object[this.variablesVector.size()];
            this.variablesVector.toArray(data.variables);
            data.variableNames = new HashMap();
            data.variableNames.putAll(this.variableNames);
        }
        this.variablesVector = null;
        try {
        } catch (IllegalArgumentException e2) {
            e2.fillInStackTrace();
            errors.add(e2);
        }
        if (this.compoundFilter == null && ((this.direction == 0 && compoundFilterOffset != 1) || (this.direction == 1 && compoundFilterOffset != ruleCount))) {
            throw new IllegalIcuArgumentException("Compound filters misplaced");
        }
        for (i2 = 0; i2 < this.dataVector.size(); i2++) {
            this.dataVector.get(i2).ruleSet.freeze();
        }
        if (this.idBlockVector.size() == 1 && this.idBlockVector.get(0).length() == 0) {
            this.idBlockVector.remove(0);
        }
        if (errors.size() != 0) {
            return;
        }
        for (int i3 = errors.size() - 1; i3 > 0; i3--) {
            Throwable cause = errors.get(i3 - 1);
            while (true) {
                previous = (RuntimeException) cause;
                if (previous.getCause() != null) {
                    cause = previous.getCause();
                }
            }
            previous.initCause(errors.get(i3));
        }
        throw errors.get(0);
    }

    private int parseRule(String rule, int pos, int limit) {
        char operator = 0;
        this.segmentStandins = new StringBuffer();
        this.segmentObjects = new ArrayList();
        RuleHalf left = new RuleHalf(null);
        RuleHalf right = new RuleHalf(null);
        this.undefinedVariableName = null;
        int pos2 = left.parse(rule, pos, limit, this);
        if (pos2 != limit) {
            pos2--;
            operator = rule.charAt(pos2);
            if (OPERATORS.indexOf(operator) < 0) {
                syntaxError("No operator pos=" + pos2, rule, pos);
            }
        }
        int pos3 = pos2 + 1;
        if (operator == '<' && pos3 < limit && rule.charAt(pos3) == '>') {
            pos3++;
            operator = FWDREV_RULE_OP;
        }
        switch (operator) {
            case 8592:
                operator = REVERSE_RULE_OP;
                break;
            case 8594:
                operator = FORWARD_RULE_OP;
                break;
            case 8596:
                operator = FWDREV_RULE_OP;
                break;
        }
        int pos4 = right.parse(rule, pos3, limit, this);
        if (pos4 < limit) {
            pos4--;
            if (rule.charAt(pos4) == ';') {
                pos4++;
            } else {
                syntaxError("Unquoted operator", rule, pos);
            }
        }
        if (operator == '=') {
            if (this.undefinedVariableName == null) {
                syntaxError("Missing '$' or duplicate definition", rule, pos);
            }
            if (left.text.length() != 1 || left.text.charAt(0) != this.variableLimit) {
                syntaxError("Malformed LHS", rule, pos);
            }
            if (left.anchorStart || left.anchorEnd || right.anchorStart || right.anchorEnd) {
                syntaxError("Malformed variable def", rule, pos);
            }
            int n = right.text.length();
            char[] value = new char[n];
            right.text.getChars(0, n, value, 0);
            this.variableNames.put(this.undefinedVariableName, value);
            this.variableLimit = (char) (this.variableLimit + 1);
            return pos4;
        }
        if (this.undefinedVariableName != null) {
            syntaxError("Undefined variable $" + this.undefinedVariableName, rule, pos);
        }
        if (this.segmentStandins.length() > this.segmentObjects.size()) {
            syntaxError("Undefined segment reference", rule, pos);
        }
        for (int i = 0; i < this.segmentStandins.length(); i++) {
            if (this.segmentStandins.charAt(i) == 0) {
                syntaxError("Internal error", rule, pos);
            }
        }
        for (int i2 = 0; i2 < this.segmentObjects.size(); i2++) {
            if (this.segmentObjects.get(i2) == null) {
                syntaxError("Internal error", rule, pos);
            }
        }
        if (operator != '~') {
            if ((this.direction == 0) != (operator == '>')) {
                return pos4;
            }
        }
        if (this.direction == 1) {
            right = left;
            left = right;
        }
        if (operator == '~') {
            right.removeContext();
            left.cursor = -1;
            left.cursorOffset = 0;
        }
        if (left.ante < 0) {
            left.ante = 0;
        }
        if (left.post < 0) {
            left.post = left.text.length();
        }
        if (right.ante >= 0 || right.post >= 0 || left.cursor >= 0 || ((right.cursorOffset != 0 && right.cursor < 0) || right.anchorStart || right.anchorEnd || !left.isValidInput(this) || !right.isValidOutput(this) || left.ante > left.post)) {
            syntaxError("Malformed rule", rule, pos);
        }
        UnicodeMatcher[] segmentsArray = null;
        if (this.segmentObjects.size() > 0) {
            segmentsArray = new UnicodeMatcher[this.segmentObjects.size()];
            this.segmentObjects.toArray(segmentsArray);
        }
        this.curData.ruleSet.addRule(new TransliterationRule(left.text, left.ante, left.post, right.text, right.cursor, right.cursorOffset, segmentsArray, left.anchorStart, left.anchorEnd, this.curData));
        return pos4;
    }

    private void setVariableRange(int start, int end) {
        if (start > end || start < 0 || end > 65535) {
            throw new IllegalIcuArgumentException("Invalid variable range " + start + ", " + end);
        }
        this.curData.variablesBase = (char) start;
        if (this.dataVector.size() != 0) {
            return;
        }
        this.variableNext = (char) start;
        this.variableLimit = (char) (end + 1);
    }

    private void checkVariableRange(int ch, String rule, int start) {
        if (ch < this.curData.variablesBase || ch >= this.variableLimit) {
            return;
        }
        syntaxError("Variable range character in rule", rule, start);
    }

    private void pragmaMaximumBackup(int backup) {
        throw new IllegalIcuArgumentException("use maximum backup pragma not implemented yet");
    }

    private void pragmaNormalizeRules(Normalizer.Mode mode) {
        throw new IllegalIcuArgumentException("use normalize rules pragma not implemented yet");
    }

    static boolean resemblesPragma(String rule, int pos, int limit) {
        return Utility.parsePattern(rule, pos, limit, "use ", null) >= 0;
    }

    private int parsePragma(String rule, int pos, int limit) {
        int[] array = new int[2];
        int pos2 = pos + 4;
        int p = Utility.parsePattern(rule, pos2, limit, "~variable range # #~;", array);
        if (p >= 0) {
            setVariableRange(array[0], array[1]);
            return p;
        }
        int p2 = Utility.parsePattern(rule, pos2, limit, "~maximum backup #~;", array);
        if (p2 >= 0) {
            pragmaMaximumBackup(array[0]);
            return p2;
        }
        int p3 = Utility.parsePattern(rule, pos2, limit, "~nfd rules~;", null);
        if (p3 >= 0) {
            pragmaNormalizeRules(Normalizer.NFD);
            return p3;
        }
        int p4 = Utility.parsePattern(rule, pos2, limit, "~nfc rules~;", null);
        if (p4 >= 0) {
            pragmaNormalizeRules(Normalizer.NFC);
            return p4;
        }
        return -1;
    }

    static final void syntaxError(String msg, String rule, int start) {
        int end = ruleEnd(rule, start, rule.length());
        throw new IllegalIcuArgumentException(msg + " in \"" + Utility.escape(rule.substring(start, end)) + '\"');
    }

    static final int ruleEnd(String rule, int start, int limit) {
        int end = Utility.quotedIndexOf(rule, start, limit, ";");
        if (end < 0) {
            return limit;
        }
        return end;
    }

    private final char parseSet(String rule, ParsePosition pos) {
        UnicodeSet set = new UnicodeSet(rule, pos, this.parseData);
        if (this.variableNext >= this.variableLimit) {
            throw new RuntimeException("Private use variables exhausted");
        }
        set.compact();
        return generateStandInFor(set);
    }

    char generateStandInFor(Object obj) {
        for (int i = 0; i < this.variablesVector.size(); i++) {
            if (this.variablesVector.get(i) == obj) {
                return (char) (this.curData.variablesBase + i);
            }
        }
        if (this.variableNext >= this.variableLimit) {
            throw new RuntimeException("Variable range exhausted");
        }
        this.variablesVector.add(obj);
        char c = this.variableNext;
        this.variableNext = (char) (c + 1);
        return c;
    }

    public char getSegmentStandin(int seg) {
        if (this.segmentStandins.length() < seg) {
            this.segmentStandins.setLength(seg);
        }
        char c = this.segmentStandins.charAt(seg - 1);
        if (c == 0) {
            if (this.variableNext >= this.variableLimit) {
                throw new RuntimeException("Variable range exhausted");
            }
            char c2 = this.variableNext;
            this.variableNext = (char) (c2 + 1);
            char c3 = c2;
            this.variablesVector.add(null);
            this.segmentStandins.setCharAt(seg - 1, c3);
            return c3;
        }
        return c;
    }

    public void setSegmentObject(int seg, StringMatcher obj) {
        while (this.segmentObjects.size() < seg) {
            this.segmentObjects.add(null);
        }
        int index = getSegmentStandin(seg) - this.curData.variablesBase;
        if (this.segmentObjects.get(seg - 1) != null || this.variablesVector.get(index) != null) {
            throw new RuntimeException();
        }
        this.segmentObjects.set(seg - 1, obj);
        this.variablesVector.set(index, obj);
    }

    char getDotStandIn() {
        if (this.dotStandIn == -1) {
            this.dotStandIn = generateStandInFor(new UnicodeSet(DOT_SET));
        }
        return (char) this.dotStandIn;
    }

    private void appendVariableDef(String name, StringBuffer buf) {
        char[] ch = this.variableNames.get(name);
        if (ch == null) {
            if (this.undefinedVariableName == null) {
                this.undefinedVariableName = name;
                if (this.variableNext >= this.variableLimit) {
                    throw new RuntimeException("Private use variables exhausted");
                }
                char c = (char) (this.variableLimit - 1);
                this.variableLimit = c;
                buf.append(c);
                return;
            }
            throw new IllegalIcuArgumentException("Undefined variable $" + name);
        }
        buf.append(ch);
    }
}
