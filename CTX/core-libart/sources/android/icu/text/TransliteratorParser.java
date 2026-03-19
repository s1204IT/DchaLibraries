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
        private ParseData() {
        }

        @Override
        public char[] lookup(String str) {
            return (char[]) TransliteratorParser.this.variableNames.get(str);
        }

        @Override
        public UnicodeMatcher lookupMatcher(int i) {
            int i2 = i - TransliteratorParser.this.curData.variablesBase;
            if (i2 >= 0 && i2 < TransliteratorParser.this.variablesVector.size()) {
                return (UnicodeMatcher) TransliteratorParser.this.variablesVector.get(i2);
            }
            return null;
        }

        @Override
        public String parseReference(String str, ParsePosition parsePosition, int i) {
            int index = parsePosition.getIndex();
            int i2 = index;
            while (i2 < i) {
                char cCharAt = str.charAt(i2);
                if ((i2 == index && !UCharacter.isUnicodeIdentifierStart(cCharAt)) || !UCharacter.isUnicodeIdentifierPart(cCharAt)) {
                    break;
                }
                i2++;
            }
            if (i2 == index) {
                return null;
            }
            parsePosition.setIndex(i2);
            return str.substring(index, i2);
        }

        public boolean isMatcher(int i) {
            int i2 = i - TransliteratorParser.this.curData.variablesBase;
            if (i2 >= 0 && i2 < TransliteratorParser.this.variablesVector.size()) {
                return TransliteratorParser.this.variablesVector.get(i2) instanceof UnicodeMatcher;
            }
            return true;
        }

        public boolean isReplacer(int i) {
            int i2 = i - TransliteratorParser.this.curData.variablesBase;
            if (i2 >= 0 && i2 < TransliteratorParser.this.variablesVector.size()) {
                return TransliteratorParser.this.variablesVector.get(i2) instanceof UnicodeReplacer;
            }
            return true;
        }
    }

    private static abstract class RuleBody {
        abstract String handleNextLine();

        abstract void reset();

        private RuleBody() {
        }

        String nextLine() {
            String strHandleNextLine;
            String strHandleNextLine2 = handleNextLine();
            if (strHandleNextLine2 != null && strHandleNextLine2.length() > 0 && strHandleNextLine2.charAt(strHandleNextLine2.length() - 1) == '\\') {
                StringBuilder sb = new StringBuilder(strHandleNextLine2);
                do {
                    sb.deleteCharAt(sb.length() - 1);
                    strHandleNextLine = handleNextLine();
                    if (strHandleNextLine != null) {
                        sb.append(strHandleNextLine);
                        if (strHandleNextLine.length() <= 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                } while (strHandleNextLine.charAt(strHandleNextLine.length() - 1) == '\\');
                return sb.toString();
            }
            return strHandleNextLine2;
        }
    }

    private static class RuleArray extends RuleBody {
        String[] array;
        int i;

        public RuleArray(String[] strArr) {
            super();
            this.array = strArr;
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

        public int parse(String str, int i, int i2, TransliteratorParser transliteratorParser) {
            StringBuffer stringBuffer = new StringBuffer();
            int section = parseSection(str, i, i2, transliteratorParser, stringBuffer, TransliteratorParser.ILLEGAL_TOP, false);
            this.text = stringBuffer.toString();
            if (this.cursorOffset > 0 && this.cursor != this.cursorOffsetPos) {
                TransliteratorParser.syntaxError("Misplaced |", str, i);
            }
            return section;
        }

        private int parseSection(String str, int i, int i2, TransliteratorParser transliteratorParser, StringBuffer stringBuffer, UnicodeSet unicodeSet, boolean z) {
            int index;
            int length;
            int i3;
            int i4;
            int[] iArr;
            int i5;
            int i6;
            boolean z2;
            boolean z3;
            int i7;
            int length2;
            int i8;
            int i9;
            int i10;
            int i11 = i;
            int[] iArr2 = new int[1];
            int length3 = stringBuffer.length();
            int i12 = -1;
            ParsePosition parsePosition = null;
            int i13 = -1;
            int length4 = -1;
            int length5 = -1;
            int length6 = -1;
            int section = i11;
            while (section < i2) {
                int i14 = section + 1;
                char cCharAt = str.charAt(section);
                if (!PatternProps.isWhiteSpace(cCharAt)) {
                    if (TransliteratorParser.HALF_ENDERS.indexOf(cCharAt) < 0) {
                        if (this.anchorEnd) {
                            TransliteratorParser.syntaxError("Malformed variable reference", str, i11);
                        }
                        int i15 = i14 - 1;
                        if (UnicodeSet.resemblesPattern(str, i15)) {
                            ParsePosition parsePosition2 = parsePosition == null ? new ParsePosition(0) : parsePosition;
                            parsePosition2.setIndex(i15);
                            stringBuffer.append(transliteratorParser.parseSet(str, parsePosition2));
                            index = parsePosition2.getIndex();
                            parsePosition = parsePosition2;
                        } else if (cCharAt == '\\') {
                            if (i14 == i2) {
                                TransliteratorParser.syntaxError("Trailing backslash", str, i11);
                            }
                            iArr2[0] = i14;
                            int iUnescapeAt = Utility.unescapeAt(str, iArr2);
                            index = iArr2[0];
                            if (iUnescapeAt == i12) {
                                TransliteratorParser.syntaxError("Malformed escape", str, i11);
                            }
                            transliteratorParser.checkVariableRange(iUnescapeAt, str, i11);
                            UTF16.append(stringBuffer, iUnescapeAt);
                        } else if (cCharAt == '\'') {
                            int iIndexOf = str.indexOf(39, i14);
                            if (iIndexOf == i14) {
                                stringBuffer.append(cCharAt);
                                section = i14 + 1;
                            } else {
                                length5 = stringBuffer.length();
                                while (true) {
                                    if (iIndexOf < 0) {
                                        TransliteratorParser.syntaxError("Unterminated quote", str, i11);
                                    }
                                    stringBuffer.append(str.substring(i14, iIndexOf));
                                    i14 = iIndexOf + 1;
                                    if (i14 < i2 && str.charAt(i14) == '\'') {
                                        iIndexOf = str.indexOf(39, i14 + 1);
                                    }
                                }
                                length4 = stringBuffer.length();
                                for (int i16 = length5; i16 < length4; i16++) {
                                    transliteratorParser.checkVariableRange(stringBuffer.charAt(i16), str, i11);
                                }
                                section = i14;
                            }
                        } else {
                            transliteratorParser.checkVariableRange(cCharAt, str, i11);
                            int[] iArr3 = iArr2;
                            if (unicodeSet.contains(cCharAt)) {
                                TransliteratorParser.syntaxError("Illegal character '" + cCharAt + '\'', str, i11);
                            }
                            if (cCharAt == '$') {
                                length = i13;
                                i3 = length4;
                                i4 = length3;
                                iArr = iArr3;
                                i5 = -1;
                                if (i14 == i2) {
                                    z3 = true;
                                    this.anchorEnd = true;
                                    z2 = z3;
                                    section = i14;
                                    i6 = i;
                                    i11 = i6;
                                    iArr2 = iArr;
                                    i13 = length;
                                    length4 = i3;
                                    i12 = i5;
                                    length3 = i4;
                                } else {
                                    int iDigit = UCharacter.digit(str.charAt(i14), 10);
                                    if (iDigit < 1 || iDigit > 9) {
                                        i6 = i;
                                        ParsePosition parsePosition3 = parsePosition == null ? new ParsePosition(0) : parsePosition;
                                        parsePosition3.setIndex(i14);
                                        String reference = transliteratorParser.parseData.parseReference(str, parsePosition3, i2);
                                        if (reference == null) {
                                            z2 = true;
                                            this.anchorEnd = true;
                                            parsePosition = parsePosition3;
                                            section = i14;
                                        } else {
                                            z2 = true;
                                            int index2 = parsePosition3.getIndex();
                                            length6 = stringBuffer.length();
                                            transliteratorParser.appendVariableDef(reference, stringBuffer);
                                            length = stringBuffer.length();
                                            parsePosition = parsePosition3;
                                            section = index2;
                                        }
                                    } else {
                                        iArr[0] = i14;
                                        int number = Utility.parseNumber(str, iArr, 10);
                                        if (number < 0) {
                                            i6 = i;
                                            TransliteratorParser.syntaxError("Undefined segment reference", str, i6);
                                        } else {
                                            i6 = i;
                                        }
                                        section = iArr[0];
                                        stringBuffer.append(transliteratorParser.getSegmentStandin(number));
                                        z2 = true;
                                    }
                                    i11 = i6;
                                    iArr2 = iArr;
                                    i13 = length;
                                    length4 = i3;
                                    i12 = i5;
                                    length3 = i4;
                                }
                            } else if (cCharAt == '&') {
                                length = i13;
                                i3 = length4;
                                i4 = length3;
                                iArr = iArr3;
                                i5 = -1;
                                iArr[0] = i14;
                                TransliteratorIDParser.SingleID filterID = TransliteratorIDParser.parseFilterID(str, iArr);
                                if (filterID == null || !Utility.parseChar(str, iArr, TransliteratorParser.SEGMENT_OPEN)) {
                                    TransliteratorParser.syntaxError("Invalid function", str, i11);
                                }
                                Transliterator singleID = filterID.getInstance();
                                if (singleID == null) {
                                    TransliteratorParser.syntaxError("Invalid function ID", str, i11);
                                }
                                int length7 = stringBuffer.length();
                                section = parseSection(str, iArr[0], i2, transliteratorParser, stringBuffer, TransliteratorParser.ILLEGAL_FUNC, true);
                                FunctionReplacer functionReplacer = new FunctionReplacer(singleID, new StringReplacer(stringBuffer.substring(length7), transliteratorParser.curData));
                                stringBuffer.setLength(length7);
                                stringBuffer.append(transliteratorParser.generateStandInFor(functionReplacer));
                                i6 = i;
                                z2 = true;
                                i11 = i6;
                                iArr2 = iArr;
                                i13 = length;
                                length4 = i3;
                                i12 = i5;
                                length3 = i4;
                            } else {
                                if (cCharAt == '.') {
                                    length = i13;
                                    i3 = length4;
                                    i4 = length3;
                                    iArr = iArr3;
                                    i5 = -1;
                                    stringBuffer.append(transliteratorParser.getDotStandIn());
                                } else if (cCharAt != '^') {
                                    if (cCharAt != 8710) {
                                        switch (cCharAt) {
                                            case '(':
                                                int length8 = stringBuffer.length();
                                                int i17 = this.nextSegmentNumber;
                                                this.nextSegmentNumber = i17 + 1;
                                                length = i13;
                                                i3 = length4;
                                                i5 = -1;
                                                i4 = length3;
                                                iArr = iArr3;
                                                section = parseSection(str, i14, i2, transliteratorParser, stringBuffer, TransliteratorParser.ILLEGAL_SEG, true);
                                                transliteratorParser.setSegmentObject(i17, new StringMatcher(stringBuffer.substring(length8), i17, transliteratorParser.curData));
                                                stringBuffer.setLength(length8);
                                                stringBuffer.append(transliteratorParser.getSegmentStandin(i17));
                                                i6 = i11;
                                                break;
                                            case ')':
                                                break;
                                            default:
                                                switch (cCharAt) {
                                                    case '?':
                                                        break;
                                                    case '@':
                                                        if (this.cursorOffset < 0) {
                                                            if (stringBuffer.length() > 0) {
                                                                TransliteratorParser.syntaxError("Misplaced " + cCharAt, str, i11);
                                                            }
                                                            this.cursorOffset--;
                                                        } else if (this.cursorOffset <= 0) {
                                                            if (this.cursor == 0 && stringBuffer.length() == 0) {
                                                                i7 = -1;
                                                                this.cursorOffset = -1;
                                                            } else {
                                                                i7 = -1;
                                                                if (this.cursor < 0) {
                                                                    this.cursorOffsetPos = stringBuffer.length();
                                                                    z3 = true;
                                                                    this.cursorOffset = 1;
                                                                    length = i13;
                                                                    i3 = length4;
                                                                    i5 = -1;
                                                                    i4 = length3;
                                                                    iArr = iArr3;
                                                                    z2 = z3;
                                                                    section = i14;
                                                                    i6 = i;
                                                                    i11 = i6;
                                                                    iArr2 = iArr;
                                                                    i13 = length;
                                                                    length4 = i3;
                                                                    i12 = i5;
                                                                    length3 = i4;
                                                                } else {
                                                                    TransliteratorParser.syntaxError("Misplaced " + cCharAt, str, i11);
                                                                }
                                                            }
                                                            length = i13;
                                                            i3 = length4;
                                                            i5 = i7;
                                                            i4 = length3;
                                                            iArr = iArr3;
                                                        } else {
                                                            if (stringBuffer.length() != this.cursorOffsetPos || this.cursor >= 0) {
                                                                TransliteratorParser.syntaxError("Misplaced " + cCharAt, str, i11);
                                                            }
                                                            this.cursorOffset++;
                                                        }
                                                        length = i13;
                                                        i3 = length4;
                                                        i4 = length3;
                                                        iArr = iArr3;
                                                        z3 = true;
                                                        i5 = -1;
                                                        z2 = z3;
                                                        section = i14;
                                                        i6 = i;
                                                        i11 = i6;
                                                        iArr2 = iArr;
                                                        i13 = length;
                                                        length4 = i3;
                                                        i12 = i5;
                                                        length3 = i4;
                                                        break;
                                                    default:
                                                        switch (cCharAt) {
                                                            case '{':
                                                                if (this.ante >= 0) {
                                                                    TransliteratorParser.syntaxError("Multiple ante contexts", str, i11);
                                                                }
                                                                this.ante = stringBuffer.length();
                                                                break;
                                                            case '|':
                                                                if (this.cursor >= 0) {
                                                                    TransliteratorParser.syntaxError("Multiple cursors", str, i11);
                                                                }
                                                                this.cursor = stringBuffer.length();
                                                                break;
                                                            case '}':
                                                                if (this.post >= 0) {
                                                                    TransliteratorParser.syntaxError("Multiple post contexts", str, i11);
                                                                }
                                                                this.post = stringBuffer.length();
                                                                break;
                                                            default:
                                                                if (cCharAt >= '!' && cCharAt <= '~' && ((cCharAt < '0' || cCharAt > '9') && ((cCharAt < 'A' || cCharAt > 'Z') && (cCharAt < 'a' || cCharAt > 'z')))) {
                                                                    TransliteratorParser.syntaxError("Unquoted " + cCharAt, str, i11);
                                                                }
                                                                stringBuffer.append(cCharAt);
                                                                break;
                                                        }
                                                        length = i13;
                                                        i3 = length4;
                                                        i4 = length3;
                                                        iArr = iArr3;
                                                        z3 = true;
                                                        i5 = -1;
                                                        z2 = z3;
                                                        section = i14;
                                                        i6 = i;
                                                        i11 = i6;
                                                        iArr2 = iArr;
                                                        i13 = length;
                                                        length4 = i3;
                                                        i12 = i5;
                                                        length3 = i4;
                                                        break;
                                                }
                                            case '*':
                                            case '+':
                                                i7 = -1;
                                                if (z && stringBuffer.length() == length3) {
                                                    TransliteratorParser.syntaxError("Misplaced quantifier", str, i11);
                                                    length = i13;
                                                    i3 = length4;
                                                    i5 = i7;
                                                    i4 = length3;
                                                    iArr = iArr3;
                                                } else {
                                                    if (stringBuffer.length() == length4) {
                                                        i8 = length4;
                                                        length2 = length5;
                                                    } else if (stringBuffer.length() == i13) {
                                                        i8 = i13;
                                                        length2 = length6;
                                                    } else {
                                                        length2 = stringBuffer.length() - 1;
                                                        i8 = length2 + 1;
                                                    }
                                                    try {
                                                        StringMatcher stringMatcher = new StringMatcher(stringBuffer.toString(), length2, i8, 0, transliteratorParser.curData);
                                                        if (cCharAt == '+') {
                                                            i9 = Integer.MAX_VALUE;
                                                            i10 = 1;
                                                        } else if (cCharAt != '?') {
                                                            i9 = Integer.MAX_VALUE;
                                                            i10 = 0;
                                                        } else {
                                                            i10 = 0;
                                                            i9 = 1;
                                                        }
                                                        int i18 = i13;
                                                        Quantifier quantifier = new Quantifier(stringMatcher, i10, i9);
                                                        stringBuffer.setLength(length2);
                                                        stringBuffer.append(transliteratorParser.generateStandInFor(quantifier));
                                                        i3 = length4;
                                                        i4 = length3;
                                                        iArr = iArr3;
                                                        length = i18;
                                                        z3 = true;
                                                        i5 = -1;
                                                        z2 = z3;
                                                        section = i14;
                                                        i6 = i;
                                                        i11 = i6;
                                                        iArr2 = iArr;
                                                        i13 = length;
                                                        length4 = i3;
                                                        i12 = i5;
                                                        length3 = i4;
                                                    } catch (RuntimeException e) {
                                                        throw new IllegalIcuArgumentException("Failure in rule: " + (i14 < 50 ? str.substring(0, i14) : "..." + str.substring(i14 - 50, i14)) + "$$$" + (i2 - i14 <= 50 ? str.substring(i14, i2) : str.substring(i14, i14 + 50) + "...")).initCause((Throwable) e);
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                    z2 = true;
                                    i11 = i6;
                                    iArr2 = iArr;
                                    i13 = length;
                                    length4 = i3;
                                    i12 = i5;
                                    length3 = i4;
                                } else {
                                    length = i13;
                                    i3 = length4;
                                    i4 = length3;
                                    iArr = iArr3;
                                    i5 = -1;
                                    if (stringBuffer.length() != 0 || this.anchorStart) {
                                        TransliteratorParser.syntaxError("Misplaced anchor start", str, i11);
                                    } else {
                                        z3 = true;
                                        this.anchorStart = true;
                                        z2 = z3;
                                        section = i14;
                                        i6 = i;
                                        i11 = i6;
                                        iArr2 = iArr;
                                        i13 = length;
                                        length4 = i3;
                                        i12 = i5;
                                        length3 = i4;
                                    }
                                }
                                z3 = true;
                                z2 = z3;
                                section = i14;
                                i6 = i;
                                i11 = i6;
                                iArr2 = iArr;
                                i13 = length;
                                length4 = i3;
                                i12 = i5;
                                length3 = i4;
                            }
                        }
                        section = index;
                    } else if (z) {
                        TransliteratorParser.syntaxError("Unclosed segment", str, i11);
                    }
                    return i14;
                }
                section = i14;
            }
            return section;
        }

        void removeContext() {
            int i;
            String str = this.text;
            if (this.ante >= 0) {
                i = this.ante;
            } else {
                i = 0;
            }
            this.text = str.substring(i, this.post < 0 ? this.text.length() : this.post);
            this.post = -1;
            this.ante = -1;
            this.anchorEnd = false;
            this.anchorStart = false;
        }

        public boolean isValidOutput(TransliteratorParser transliteratorParser) {
            int charCount = 0;
            while (charCount < this.text.length()) {
                int iCharAt = UTF16.charAt(this.text, charCount);
                charCount += UTF16.getCharCount(iCharAt);
                if (!transliteratorParser.parseData.isReplacer(iCharAt)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isValidInput(TransliteratorParser transliteratorParser) {
            int charCount = 0;
            while (charCount < this.text.length()) {
                int iCharAt = UTF16.charAt(this.text, charCount);
                charCount += UTF16.getCharCount(iCharAt);
                if (!transliteratorParser.parseData.isMatcher(iCharAt)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void parse(String str, int i) {
        parseRules(new RuleArray(new String[]{str}), i);
    }

    void parseRules(RuleBody ruleBody, int i) {
        int i2;
        RuntimeException runtimeException;
        int i3;
        int i4;
        int i5;
        this.dataVector = new ArrayList();
        this.idBlockVector = new ArrayList();
        this.curData = null;
        this.direction = i;
        this.compoundFilter = null;
        this.variablesVector = new ArrayList();
        this.variableNames = new HashMap();
        this.parseData = new ParseData();
        ArrayList arrayList = new ArrayList();
        ruleBody.reset();
        StringBuilder sb = new StringBuilder();
        this.compoundFilter = null;
        int i6 = 1;
        int i7 = 1;
        int i8 = 0;
        int i9 = 0;
        int i10 = -1;
        loop0: while (true) {
            String strNextLine = ruleBody.nextLine();
            if (strNextLine == null) {
                break;
            }
            int length = strNextLine.length();
            int i11 = i9;
            int i12 = i10;
            int i13 = i7;
            int iIndexOf = 0;
            while (iIndexOf < length) {
                int i14 = iIndexOf + 1;
                char cCharAt = strNextLine.charAt(iIndexOf);
                if (!PatternProps.isWhiteSpace(cCharAt)) {
                    if (cCharAt == '#') {
                        iIndexOf = strNextLine.indexOf("\n", i14) + i6;
                        if (iIndexOf == 0) {
                            break;
                        }
                    } else if (cCharAt != ';') {
                        i8++;
                        int i15 = i14 - 1;
                        int i16 = i15 + 2;
                        if (i16 + 1 <= length) {
                            try {
                            } catch (IllegalArgumentException e) {
                                e = e;
                            }
                            if (strNextLine.regionMatches(i15, ID_TOKEN, 0, 2)) {
                                try {
                                    char cCharAt2 = strNextLine.charAt(i16);
                                    int i17 = i16;
                                    while (PatternProps.isWhiteSpace(cCharAt2) && i17 < length) {
                                        i17++;
                                        cCharAt2 = strNextLine.charAt(i17);
                                    }
                                    int[] iArr = new int[i6];
                                    iArr[0] = i17;
                                    if (i13 == 0) {
                                        if (this.curData != null) {
                                            if (this.direction == 0) {
                                                this.dataVector.add(this.curData);
                                            } else {
                                                this.dataVector.add(0, this.curData);
                                            }
                                            this.curData = null;
                                        }
                                        i13 = i6;
                                    }
                                    TransliteratorIDParser.SingleID singleID = TransliteratorIDParser.parseSingleID(strNextLine, iArr, this.direction);
                                    if (iArr[0] != i17 && Utility.parseChar(strNextLine, iArr, END_OF_RULE)) {
                                        if (this.direction == 0) {
                                            sb.append(singleID.canonID);
                                            sb.append(END_OF_RULE);
                                        } else {
                                            sb.insert(0, singleID.canonID + END_OF_RULE);
                                        }
                                    } else {
                                        int[] iArr2 = new int[i6];
                                        iArr2[0] = -1;
                                        UnicodeSet globalFilter = TransliteratorIDParser.parseGlobalFilter(strNextLine, iArr, this.direction, iArr2, null);
                                        if (globalFilter != null && Utility.parseChar(strNextLine, iArr, END_OF_RULE)) {
                                            if ((this.direction == 0) == (iArr2[0] == 0)) {
                                                if (this.compoundFilter != null) {
                                                    syntaxError("Multiple global filters", strNextLine, i17);
                                                }
                                                this.compoundFilter = globalFilter;
                                                i12 = i8;
                                            }
                                        } else {
                                            syntaxError("Invalid ::ID", strNextLine, i17);
                                        }
                                    }
                                    iIndexOf = iArr[0];
                                    i5 = 1;
                                } catch (IllegalArgumentException e2) {
                                    e = e2;
                                    i15 = i16;
                                    if (i11 != 30) {
                                    }
                                }
                                i6 = i5;
                            } else {
                                if (i13 != 0) {
                                    if (this.direction == 0) {
                                        this.idBlockVector.add(sb.toString());
                                    } else {
                                        this.idBlockVector.add(0, sb.toString());
                                    }
                                    sb.delete(0, sb.length());
                                    try {
                                        this.curData = new RuleBasedTransliterator.Data();
                                        setVariableRange(61440, 63743);
                                        i13 = 0;
                                    } catch (IllegalArgumentException e3) {
                                        e = e3;
                                        i13 = 0;
                                        if (i11 != 30) {
                                            IllegalIcuArgumentException illegalIcuArgumentException = new IllegalIcuArgumentException("\nMore than 30 errors; further messages squelched");
                                            illegalIcuArgumentException.initCause((Throwable) e);
                                            arrayList.add(illegalIcuArgumentException);
                                            i7 = i13;
                                            i10 = i12;
                                            if (i7 == 0) {
                                                if (i7 == 0) {
                                                    if (this.direction != 0) {
                                                    }
                                                }
                                            }
                                            while (i2 < this.dataVector.size()) {
                                            }
                                            this.variablesVector = null;
                                            if (this.compoundFilter != null) {
                                            }
                                            while (i3 < this.dataVector.size()) {
                                            }
                                            if (this.idBlockVector.size() == 1) {
                                                this.idBlockVector.remove(0);
                                            }
                                            if (arrayList.size() == 0) {
                                            }
                                        } else {
                                            e.fillInStackTrace();
                                            arrayList.add(e);
                                            i11++;
                                            i5 = 1;
                                            iIndexOf = ruleEnd(strNextLine, i15, length) + 1;
                                        }
                                    }
                                }
                                if (resemblesPragma(strNextLine, i15, length)) {
                                    iIndexOf = parsePragma(strNextLine, i15, length);
                                    if (iIndexOf < 0) {
                                        syntaxError("Unrecognized pragma", strNextLine, i15);
                                    }
                                } else {
                                    iIndexOf = parseRule(strNextLine, i15, length);
                                }
                                i5 = 1;
                                i6 = i5;
                            }
                        }
                    }
                }
                iIndexOf = i14;
            }
            i6 = i6;
            i7 = i13;
            i10 = i12;
            i9 = i11;
        }
        if (i7 == 0 && sb.length() > 0) {
            if (this.direction == 0) {
                this.idBlockVector.add(sb.toString());
            } else {
                this.idBlockVector.add(0, sb.toString());
            }
        } else if (i7 == 0 && this.curData != null) {
            if (this.direction != 0) {
                this.dataVector.add(this.curData);
            } else {
                this.dataVector.add(0, this.curData);
            }
        }
        for (i2 = 0; i2 < this.dataVector.size(); i2++) {
            RuleBasedTransliterator.Data data = this.dataVector.get(i2);
            data.variables = new Object[this.variablesVector.size()];
            this.variablesVector.toArray(data.variables);
            data.variableNames = new HashMap();
            data.variableNames.putAll(this.variableNames);
        }
        this.variablesVector = null;
        try {
            if (this.compoundFilter != null) {
                if (this.direction == 0) {
                    i4 = 1;
                    if (i10 == 1) {
                    }
                    throw new IllegalIcuArgumentException("Compound filters misplaced");
                }
                i4 = 1;
                if (this.direction == i4) {
                    if (i10 == i8) {
                    }
                    throw new IllegalIcuArgumentException("Compound filters misplaced");
                }
            }
            for (i3 = 0; i3 < this.dataVector.size(); i3++) {
                this.dataVector.get(i3).ruleSet.freeze();
            }
            if (this.idBlockVector.size() == 1 && this.idBlockVector.get(0).length() == 0) {
                this.idBlockVector.remove(0);
            }
        } catch (IllegalArgumentException e4) {
            e4.fillInStackTrace();
            arrayList.add(e4);
        }
        if (arrayList.size() == 0) {
            for (int size = arrayList.size() - 1; size > 0; size--) {
                Object cause = arrayList.get(size - 1);
                while (true) {
                    runtimeException = (RuntimeException) cause;
                    if (runtimeException.getCause() != null) {
                        cause = runtimeException.getCause();
                    }
                }
                runtimeException.initCause((Throwable) arrayList.get(size));
            }
            throw ((RuntimeException) arrayList.get(0));
        }
    }

    private int parseRule(String str, int i, int i2) {
        char cCharAt;
        int i3;
        int i4;
        Object[] objArr;
        Object[] objArr2;
        this.segmentStandins = new StringBuffer();
        this.segmentObjects = new ArrayList();
        UnicodeMatcher[] unicodeMatcherArr = null;
        RuleHalf ruleHalf = new RuleHalf();
        RuleHalf ruleHalf2 = new RuleHalf();
        this.undefinedVariableName = null;
        int i5 = ruleHalf.parse(str, i, i2, this);
        if (i5 != i2) {
            i5--;
            cCharAt = str.charAt(i5);
            if (OPERATORS.indexOf(cCharAt) < 0) {
            }
            i3 = i5 + 1;
            if (cCharAt == '<' && i3 < i2 && str.charAt(i3) == '>') {
                i3++;
                cCharAt = '~';
            }
            if (cCharAt != 8592) {
                cCharAt = '<';
            } else if (cCharAt == 8594) {
                cCharAt = '>';
            } else if (cCharAt == 8596) {
                cCharAt = '~';
            }
            i4 = ruleHalf2.parse(str, i3, i2, this);
            if (i4 < i2) {
                i4--;
                if (str.charAt(i4) == ';') {
                    i4++;
                } else {
                    syntaxError("Unquoted operator", str, i);
                }
            }
            if (cCharAt != '=') {
                if (this.undefinedVariableName == null) {
                    syntaxError("Missing '$' or duplicate definition", str, i);
                }
                if (ruleHalf.text.length() != 1 || ruleHalf.text.charAt(0) != this.variableLimit) {
                    syntaxError("Malformed LHS", str, i);
                }
                if (ruleHalf.anchorStart || ruleHalf.anchorEnd || ruleHalf2.anchorStart || ruleHalf2.anchorEnd) {
                    syntaxError("Malformed variable def", str, i);
                }
                int length = ruleHalf2.text.length();
                char[] cArr = new char[length];
                ruleHalf2.text.getChars(0, length, cArr, 0);
                this.variableNames.put(this.undefinedVariableName, cArr);
                this.variableLimit = (char) (this.variableLimit + 1);
                return i4;
            }
            if (this.undefinedVariableName != null) {
                syntaxError("Undefined variable $" + this.undefinedVariableName, str, i);
            }
            if (this.segmentStandins.length() > this.segmentObjects.size()) {
                syntaxError("Undefined segment reference", str, i);
            }
            for (int i6 = 0; i6 < this.segmentStandins.length(); i6++) {
                if (this.segmentStandins.charAt(i6) == 0) {
                    syntaxError("Internal error", str, i);
                }
            }
            for (int i7 = 0; i7 < this.segmentObjects.size(); i7++) {
                if (this.segmentObjects.get(i7) == null) {
                    syntaxError("Internal error", str, i);
                }
            }
            if (cCharAt != '~') {
                if (this.direction != 0) {
                    objArr = false;
                } else {
                    objArr = true;
                }
                if (cCharAt != '>') {
                    objArr2 = false;
                } else {
                    objArr2 = true;
                }
                if (objArr != objArr2) {
                    return i4;
                }
            }
            if (this.direction != 1) {
                ruleHalf2 = ruleHalf;
                ruleHalf = ruleHalf2;
            }
            if (cCharAt == '~') {
                ruleHalf.removeContext();
                ruleHalf2.cursor = -1;
                ruleHalf2.cursorOffset = 0;
            }
            if (ruleHalf2.ante < 0) {
                ruleHalf2.ante = 0;
            }
            if (ruleHalf2.post < 0) {
                ruleHalf2.post = ruleHalf2.text.length();
            }
            if (ruleHalf.ante >= 0 || ruleHalf.post >= 0 || ruleHalf2.cursor >= 0 || ((ruleHalf.cursorOffset != 0 && ruleHalf.cursor < 0) || ruleHalf.anchorStart || ruleHalf.anchorEnd || !ruleHalf2.isValidInput(this) || !ruleHalf.isValidOutput(this) || ruleHalf2.ante > ruleHalf2.post)) {
                syntaxError("Malformed rule", str, i);
            }
            if (this.segmentObjects.size() > 0) {
                unicodeMatcherArr = new UnicodeMatcher[this.segmentObjects.size()];
                this.segmentObjects.toArray(unicodeMatcherArr);
            }
            this.curData.ruleSet.addRule(new TransliterationRule(ruleHalf2.text, ruleHalf2.ante, ruleHalf2.post, ruleHalf.text, ruleHalf.cursor, ruleHalf.cursorOffset, unicodeMatcherArr, ruleHalf2.anchorStart, ruleHalf2.anchorEnd, this.curData));
            return i4;
        }
        cCharAt = 0;
        syntaxError("No operator pos=" + i5, str, i);
        i3 = i5 + 1;
        if (cCharAt == '<') {
            i3++;
            cCharAt = '~';
        }
        if (cCharAt != 8592) {
        }
        i4 = ruleHalf2.parse(str, i3, i2, this);
        if (i4 < i2) {
        }
        if (cCharAt != '=') {
        }
    }

    private void setVariableRange(int i, int i2) {
        if (i > i2 || i < 0 || i2 > 65535) {
            throw new IllegalIcuArgumentException("Invalid variable range " + i + ", " + i2);
        }
        char c = (char) i;
        this.curData.variablesBase = c;
        if (this.dataVector.size() == 0) {
            this.variableNext = c;
            this.variableLimit = (char) (i2 + 1);
        }
    }

    private void checkVariableRange(int i, String str, int i2) {
        if (i >= this.curData.variablesBase && i < this.variableLimit) {
            syntaxError("Variable range character in rule", str, i2);
        }
    }

    private void pragmaMaximumBackup(int i) {
        throw new IllegalIcuArgumentException("use maximum backup pragma not implemented yet");
    }

    private void pragmaNormalizeRules(Normalizer.Mode mode) {
        throw new IllegalIcuArgumentException("use normalize rules pragma not implemented yet");
    }

    static boolean resemblesPragma(String str, int i, int i2) {
        return Utility.parsePattern(str, i, i2, "use ", null) >= 0;
    }

    private int parsePragma(String str, int i, int i2) {
        int[] iArr = new int[2];
        int i3 = i + 4;
        int pattern = Utility.parsePattern(str, i3, i2, "~variable range # #~;", iArr);
        if (pattern >= 0) {
            setVariableRange(iArr[0], iArr[1]);
            return pattern;
        }
        int pattern2 = Utility.parsePattern(str, i3, i2, "~maximum backup #~;", iArr);
        if (pattern2 >= 0) {
            pragmaMaximumBackup(iArr[0]);
            return pattern2;
        }
        int pattern3 = Utility.parsePattern(str, i3, i2, "~nfd rules~;", null);
        if (pattern3 >= 0) {
            pragmaNormalizeRules(Normalizer.NFD);
            return pattern3;
        }
        int pattern4 = Utility.parsePattern(str, i3, i2, "~nfc rules~;", null);
        if (pattern4 >= 0) {
            pragmaNormalizeRules(Normalizer.NFC);
            return pattern4;
        }
        return -1;
    }

    static final void syntaxError(String str, String str2, int i) {
        throw new IllegalIcuArgumentException(str + " in \"" + Utility.escape(str2.substring(i, ruleEnd(str2, i, str2.length()))) + '\"');
    }

    static final int ruleEnd(String str, int i, int i2) {
        int iQuotedIndexOf = Utility.quotedIndexOf(str, i, i2, ";");
        return iQuotedIndexOf < 0 ? i2 : iQuotedIndexOf;
    }

    private final char parseSet(String str, ParsePosition parsePosition) {
        UnicodeSet unicodeSet = new UnicodeSet(str, parsePosition, this.parseData);
        if (this.variableNext >= this.variableLimit) {
            throw new RuntimeException("Private use variables exhausted");
        }
        unicodeSet.compact();
        return generateStandInFor(unicodeSet);
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

    public char getSegmentStandin(int i) {
        if (this.segmentStandins.length() < i) {
            this.segmentStandins.setLength(i);
        }
        int i2 = i - 1;
        char cCharAt = this.segmentStandins.charAt(i2);
        if (cCharAt == 0) {
            if (this.variableNext >= this.variableLimit) {
                throw new RuntimeException("Variable range exhausted");
            }
            char c = this.variableNext;
            this.variableNext = (char) (c + 1);
            this.variablesVector.add(null);
            this.segmentStandins.setCharAt(i2, c);
            return c;
        }
        return cCharAt;
    }

    public void setSegmentObject(int i, StringMatcher stringMatcher) {
        while (this.segmentObjects.size() < i) {
            this.segmentObjects.add(null);
        }
        int segmentStandin = getSegmentStandin(i) - this.curData.variablesBase;
        int i2 = i - 1;
        if (this.segmentObjects.get(i2) != null || this.variablesVector.get(segmentStandin) != null) {
            throw new RuntimeException();
        }
        this.segmentObjects.set(i2, stringMatcher);
        this.variablesVector.set(segmentStandin, stringMatcher);
    }

    char getDotStandIn() {
        if (this.dotStandIn == -1) {
            this.dotStandIn = generateStandInFor(new UnicodeSet(DOT_SET));
        }
        return (char) this.dotStandIn;
    }

    private void appendVariableDef(String str, StringBuffer stringBuffer) {
        char[] cArr = this.variableNames.get(str);
        if (cArr == null) {
            if (this.undefinedVariableName == null) {
                this.undefinedVariableName = str;
                if (this.variableNext >= this.variableLimit) {
                    throw new RuntimeException("Private use variables exhausted");
                }
                char c = (char) (this.variableLimit - 1);
                this.variableLimit = c;
                stringBuffer.append(c);
                return;
            }
            throw new IllegalIcuArgumentException("Undefined variable $" + str);
        }
        stringBuffer.append(cArr);
    }
}
