package android.icu.text;

import android.icu.impl.ICUConfig;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import java.util.ArrayList;
import java.util.Locale;

public final class MessagePattern implements Cloneable, Freezable<MessagePattern> {

    static final boolean f73assertionsDisabled;
    public static final int ARG_NAME_NOT_NUMBER = -1;
    public static final int ARG_NAME_NOT_VALID = -2;
    private static final int MAX_PREFIX_LENGTH = 24;
    public static final double NO_NUMERIC_VALUE = -1.23456789E8d;
    private static final ArgType[] argTypes;
    private static final ApostropheMode defaultAposMode;
    private ApostropheMode aposMode;
    private volatile boolean frozen;
    private boolean hasArgNames;
    private boolean hasArgNumbers;
    private String msg;
    private boolean needsAutoQuoting;
    private ArrayList<Double> numericValues;
    private ArrayList<Part> parts;

    public enum ApostropheMode {
        DOUBLE_OPTIONAL,
        DOUBLE_REQUIRED;

        public static ApostropheMode[] valuesCustom() {
            return values();
        }
    }

    public MessagePattern() {
        this.parts = new ArrayList<>();
        this.aposMode = defaultAposMode;
    }

    public MessagePattern(ApostropheMode mode) {
        this.parts = new ArrayList<>();
        this.aposMode = mode;
    }

    public MessagePattern(String pattern) {
        this.parts = new ArrayList<>();
        this.aposMode = defaultAposMode;
        parse(pattern);
    }

    public MessagePattern parse(String pattern) {
        preParse(pattern);
        parseMessage(0, 0, 0, ArgType.NONE);
        postParse();
        return this;
    }

    public MessagePattern parseChoiceStyle(String pattern) {
        preParse(pattern);
        parseChoiceStyle(0, 0);
        postParse();
        return this;
    }

    public MessagePattern parsePluralStyle(String pattern) {
        preParse(pattern);
        parsePluralOrSelectStyle(ArgType.PLURAL, 0, 0);
        postParse();
        return this;
    }

    public MessagePattern parseSelectStyle(String pattern) {
        preParse(pattern);
        parsePluralOrSelectStyle(ArgType.SELECT, 0, 0);
        postParse();
        return this;
    }

    public void clear() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to clear() a frozen MessagePattern instance.");
        }
        this.msg = null;
        this.hasArgNumbers = false;
        this.hasArgNames = false;
        this.needsAutoQuoting = false;
        this.parts.clear();
        if (this.numericValues == null) {
            return;
        }
        this.numericValues.clear();
    }

    public void clearPatternAndSetApostropheMode(ApostropheMode mode) {
        clear();
        this.aposMode = mode;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MessagePattern o = (MessagePattern) other;
        if (!this.aposMode.equals(o.aposMode)) {
            return false;
        }
        if (this.msg == null) {
            if (o.msg != null) {
                return false;
            }
        } else if (!this.msg.equals(o.msg)) {
            return false;
        }
        return this.parts.equals(o.parts);
    }

    public int hashCode() {
        return (((this.msg != null ? this.msg.hashCode() : 0) + (this.aposMode.hashCode() * 37)) * 37) + this.parts.hashCode();
    }

    public ApostropheMode getApostropheMode() {
        return this.aposMode;
    }

    boolean jdkAposMode() {
        return this.aposMode == ApostropheMode.DOUBLE_REQUIRED;
    }

    public String getPatternString() {
        return this.msg;
    }

    public boolean hasNamedArguments() {
        return this.hasArgNames;
    }

    public boolean hasNumberedArguments() {
        return this.hasArgNumbers;
    }

    public String toString() {
        return this.msg;
    }

    public static int validateArgumentName(String name) {
        if (!PatternProps.isIdentifier(name)) {
            return -2;
        }
        return parseArgNumber(name, 0, name.length());
    }

    public String autoQuoteApostropheDeep() {
        if (!this.needsAutoQuoting) {
            return this.msg;
        }
        StringBuilder modified = null;
        int count = countParts();
        int i = count;
        while (i > 0) {
            i--;
            Part part = getPart(i);
            if (part.getType() == Part.Type.INSERT_CHAR) {
                if (modified == null) {
                    modified = new StringBuilder(this.msg.length() + 10).append(this.msg);
                }
                modified.insert(part.index, (char) part.value);
            }
        }
        if (modified == null) {
            return this.msg;
        }
        return modified.toString();
    }

    public int countParts() {
        return this.parts.size();
    }

    public Part getPart(int i) {
        return this.parts.get(i);
    }

    public Part.Type getPartType(int i) {
        return this.parts.get(i).type;
    }

    public int getPatternIndex(int partIndex) {
        return this.parts.get(partIndex).index;
    }

    public String getSubstring(Part part) {
        int index = part.index;
        return this.msg.substring(index, part.length + index);
    }

    public boolean partSubstringMatches(Part part, String s) {
        return this.msg.regionMatches(part.index, s, 0, part.length);
    }

    public double getNumericValue(Part part) {
        Part.Type type = part.type;
        if (type == Part.Type.ARG_INT) {
            return part.value;
        }
        if (type == Part.Type.ARG_DOUBLE) {
            return this.numericValues.get(part.value).doubleValue();
        }
        return -1.23456789E8d;
    }

    public double getPluralOffset(int pluralStart) {
        Part part = this.parts.get(pluralStart);
        if (part.type.hasNumericValue()) {
            return getNumericValue(part);
        }
        return 0.0d;
    }

    public int getLimitPartIndex(int start) {
        int limit = this.parts.get(start).limitPartIndex;
        if (limit < start) {
            return start;
        }
        return limit;
    }

    public static final class Part {
        private static final int MAX_LENGTH = 65535;
        private static final int MAX_VALUE = 32767;
        private final int index;
        private final char length;
        private int limitPartIndex;
        private final Type type;
        private short value;

        Part(Type t, int i, int l, int v, Part part) {
            this(t, i, l, v);
        }

        private Part(Type t, int i, int l, int v) {
            this.type = t;
            this.index = i;
            this.length = (char) l;
            this.value = (short) v;
        }

        public Type getType() {
            return this.type;
        }

        public int getIndex() {
            return this.index;
        }

        public int getLength() {
            return this.length;
        }

        public int getLimit() {
            return this.index + this.length;
        }

        public int getValue() {
            return this.value;
        }

        public ArgType getArgType() {
            Type type = getType();
            if (type == Type.ARG_START || type == Type.ARG_LIMIT) {
                return MessagePattern.argTypes[this.value];
            }
            return ArgType.NONE;
        }

        public enum Type {
            MSG_START,
            MSG_LIMIT,
            SKIP_SYNTAX,
            INSERT_CHAR,
            REPLACE_NUMBER,
            ARG_START,
            ARG_LIMIT,
            ARG_NUMBER,
            ARG_NAME,
            ARG_TYPE,
            ARG_STYLE,
            ARG_SELECTOR,
            ARG_INT,
            ARG_DOUBLE;

            public static Type[] valuesCustom() {
                return values();
            }

            public boolean hasNumericValue() {
                return this == ARG_INT || this == ARG_DOUBLE;
            }
        }

        public String toString() {
            String valueString = (this.type == Type.ARG_START || this.type == Type.ARG_LIMIT) ? getArgType().name() : Integer.toString(this.value);
            return this.type.name() + "(" + valueString + ")@" + this.index;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Part o = (Part) other;
            if (this.type.equals(o.type) && this.index == o.index && this.length == o.length && this.value == o.value) {
                return this.limitPartIndex == o.limitPartIndex;
            }
            return false;
        }

        public int hashCode() {
            return (((((this.type.hashCode() * 37) + this.index) * 37) + this.length) * 37) + this.value;
        }
    }

    public enum ArgType {
        NONE,
        SIMPLE,
        CHOICE,
        PLURAL,
        SELECT,
        SELECTORDINAL;

        public static ArgType[] valuesCustom() {
            return values();
        }

        public boolean hasPluralStyle() {
            return this == PLURAL || this == SELECTORDINAL;
        }
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    @Override
    public MessagePattern cloneAsThawed() {
        try {
            MessagePattern newMsg = (MessagePattern) super.clone();
            newMsg.parts = (ArrayList) this.parts.clone();
            if (this.numericValues != null) {
                newMsg.numericValues = (ArrayList) this.numericValues.clone();
            }
            newMsg.frozen = false;
            return newMsg;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    @Override
    public MessagePattern freeze() {
        this.frozen = true;
        return this;
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    private void preParse(String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to parse(" + prefix(pattern) + ") on frozen MessagePattern instance.");
        }
        this.msg = pattern;
        this.hasArgNumbers = false;
        this.hasArgNames = false;
        this.needsAutoQuoting = false;
        this.parts.clear();
        if (this.numericValues == null) {
            return;
        }
        this.numericValues.clear();
    }

    private void postParse() {
    }

    private int parseMessage(int index, int msgStartLength, int nestingLevel, ArgType parentType) {
        int index2;
        if (nestingLevel > 32767) {
            throw new IndexOutOfBoundsException();
        }
        int msgStart = this.parts.size();
        addPart(Part.Type.MSG_START, index, msgStartLength, nestingLevel);
        int index3 = index + msgStartLength;
        while (index3 < this.msg.length()) {
            int index4 = index3 + 1;
            char c = this.msg.charAt(index3);
            if (c == '\'') {
                if (index4 == this.msg.length()) {
                    addPart(Part.Type.INSERT_CHAR, index4, 0, 39);
                    this.needsAutoQuoting = true;
                    index3 = index4;
                } else {
                    char c2 = this.msg.charAt(index4);
                    if (c2 == '\'') {
                        index3 = index4 + 1;
                        addPart(Part.Type.SKIP_SYNTAX, index4, 1, 0);
                    } else if (this.aposMode == ApostropheMode.DOUBLE_REQUIRED || c2 == '{' || c2 == '}' || ((parentType == ArgType.CHOICE && c2 == '|') || (parentType.hasPluralStyle() && c2 == '#'))) {
                        addPart(Part.Type.SKIP_SYNTAX, index4 - 1, 1, 0);
                        int index5 = index4;
                        while (true) {
                            index2 = this.msg.indexOf(39, index5 + 1);
                            if (index2 >= 0) {
                                if (index2 + 1 >= this.msg.length() || this.msg.charAt(index2 + 1) != '\'') {
                                    break;
                                }
                                index5 = index2 + 1;
                                addPart(Part.Type.SKIP_SYNTAX, index5, 1, 0);
                            } else {
                                index3 = this.msg.length();
                                addPart(Part.Type.INSERT_CHAR, index3, 0, 39);
                                this.needsAutoQuoting = true;
                                break;
                            }
                        }
                        addPart(Part.Type.SKIP_SYNTAX, index2, 1, 0);
                        index3 = index2 + 1;
                    } else {
                        addPart(Part.Type.INSERT_CHAR, index4, 0, 39);
                        this.needsAutoQuoting = true;
                        index3 = index4;
                    }
                }
            } else if (parentType.hasPluralStyle() && c == '#') {
                addPart(Part.Type.REPLACE_NUMBER, index4 - 1, 1, 0);
                index3 = index4;
            } else if (c == '{') {
                index3 = parseArg(index4 - 1, 1, nestingLevel);
            } else {
                if ((nestingLevel > 0 && c == '}') || (parentType == ArgType.CHOICE && c == '|')) {
                    int limitLength = (parentType == ArgType.CHOICE && c == '}') ? 0 : 1;
                    addLimitPart(msgStart, Part.Type.MSG_LIMIT, index4 - 1, limitLength, nestingLevel);
                    if (parentType == ArgType.CHOICE) {
                        return index4 - 1;
                    }
                    return index4;
                }
                index3 = index4;
            }
        }
        if (nestingLevel > 0 && !inTopLevelChoiceMessage(nestingLevel, parentType)) {
            throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
        }
        addLimitPart(msgStart, Part.Type.MSG_LIMIT, index3, 0, nestingLevel);
        return index3;
    }

    private int parseArg(int index, int argStartLength, int nestingLevel) {
        char c;
        int index2;
        int argStart = this.parts.size();
        ArgType argType = ArgType.NONE;
        addPart(Part.Type.ARG_START, index, argStartLength, argType.ordinal());
        int index3 = skipWhiteSpace(index + argStartLength);
        if (index3 == this.msg.length()) {
            throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
        }
        int index4 = skipIdentifier(index3);
        int number = parseArgNumber(index3, index4);
        if (number >= 0) {
            int length = index4 - index3;
            if (length > 65535 || number > 32767) {
                throw new IndexOutOfBoundsException("Argument number too large: " + prefix(index3));
            }
            this.hasArgNumbers = true;
            addPart(Part.Type.ARG_NUMBER, index3, length, number);
        } else if (number == -1) {
            int length2 = index4 - index3;
            if (length2 > 65535) {
                throw new IndexOutOfBoundsException("Argument name too long: " + prefix(index3));
            }
            this.hasArgNames = true;
            addPart(Part.Type.ARG_NAME, index3, length2, 0);
        } else {
            throw new IllegalArgumentException("Bad argument syntax: " + prefix(index3));
        }
        int index5 = skipWhiteSpace(index4);
        if (index5 == this.msg.length()) {
            throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
        }
        char c2 = this.msg.charAt(index5);
        if (c2 == '}') {
            index2 = index5;
        } else {
            if (c2 != ',') {
                throw new IllegalArgumentException("Bad argument syntax: " + prefix(index3));
            }
            int index6 = skipWhiteSpace(index5 + 1);
            int index7 = index6;
            while (index7 < this.msg.length() && isArgTypeChar(this.msg.charAt(index7))) {
                index7++;
            }
            int length3 = index7 - index6;
            int index8 = skipWhiteSpace(index7);
            if (index8 == this.msg.length()) {
                throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
            }
            if (length3 == 0 || ((c = this.msg.charAt(index8)) != ',' && c != '}')) {
                throw new IllegalArgumentException("Bad argument syntax: " + prefix(index3));
            }
            if (length3 > 65535) {
                throw new IndexOutOfBoundsException("Argument type name too long: " + prefix(index3));
            }
            argType = ArgType.SIMPLE;
            if (length3 == 6) {
                if (isChoice(index6)) {
                    argType = ArgType.CHOICE;
                } else if (isPlural(index6)) {
                    argType = ArgType.PLURAL;
                } else if (isSelect(index6)) {
                    argType = ArgType.SELECT;
                }
            } else if (length3 == 13 && isSelect(index6) && isOrdinal(index6 + 6)) {
                argType = ArgType.SELECTORDINAL;
            }
            this.parts.get(argStart).value = (short) argType.ordinal();
            if (argType == ArgType.SIMPLE) {
                addPart(Part.Type.ARG_TYPE, index6, length3, 0);
            }
            if (c == '}') {
                if (argType != ArgType.SIMPLE) {
                    throw new IllegalArgumentException("No style field for complex argument: " + prefix(index3));
                }
                index2 = index8;
            } else {
                int index9 = index8 + 1;
                if (argType == ArgType.SIMPLE) {
                    index2 = parseSimpleStyle(index9);
                } else if (argType == ArgType.CHOICE) {
                    index2 = parseChoiceStyle(index9, nestingLevel);
                } else {
                    index2 = parsePluralOrSelectStyle(argType, index9, nestingLevel);
                }
            }
        }
        addLimitPart(argStart, Part.Type.ARG_LIMIT, index2, 1, argType.ordinal());
        return index2 + 1;
    }

    private int parseSimpleStyle(int index) {
        int nestedBraces = 0;
        while (index < this.msg.length()) {
            int index2 = index + 1;
            char c = this.msg.charAt(index);
            if (c == '\'') {
                int index3 = this.msg.indexOf(39, index2);
                if (index3 < 0) {
                    throw new IllegalArgumentException("Quoted literal argument style text reaches to the end of the message: " + prefix(index));
                }
                index = index3 + 1;
            } else if (c == '{') {
                nestedBraces++;
                index = index2;
            } else if (c != '}') {
                index = index2;
            } else if (nestedBraces > 0) {
                nestedBraces--;
                index = index2;
            } else {
                int index4 = index2 - 1;
                int length = index4 - index;
                if (length <= 65535) {
                    addPart(Part.Type.ARG_STYLE, index, length, 0);
                    return index4;
                }
                throw new IndexOutOfBoundsException("Argument style text too long: " + prefix(index));
            }
        }
        throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
    }

    private int parseChoiceStyle(int index, int nestingLevel) {
        int index2 = skipWhiteSpace(index);
        if (index2 == this.msg.length() || this.msg.charAt(index2) == '}') {
            throw new IllegalArgumentException("Missing choice argument pattern in " + prefix());
        }
        while (true) {
            int numberIndex = index2;
            int index3 = skipDouble(index2);
            int length = index3 - numberIndex;
            if (length == 0) {
                throw new IllegalArgumentException("Bad choice pattern syntax: " + prefix(index));
            }
            if (length > 65535) {
                throw new IndexOutOfBoundsException("Choice number too long: " + prefix(numberIndex));
            }
            parseDouble(numberIndex, index3, true);
            int index4 = skipWhiteSpace(index3);
            if (index4 == this.msg.length()) {
                throw new IllegalArgumentException("Bad choice pattern syntax: " + prefix(index));
            }
            char c = this.msg.charAt(index4);
            if (c != '#' && c != '<' && c != 8804) {
                throw new IllegalArgumentException("Expected choice separator (#<≤) instead of '" + c + "' in choice pattern " + prefix(index));
            }
            addPart(Part.Type.ARG_SELECTOR, index4, 1, 0);
            int index5 = parseMessage(index4 + 1, 0, nestingLevel + 1, ArgType.CHOICE);
            if (index5 == this.msg.length()) {
                return index5;
            }
            if (this.msg.charAt(index5) == '}') {
                if (!inMessageFormatPattern(nestingLevel)) {
                    throw new IllegalArgumentException("Bad choice pattern syntax: " + prefix(index));
                }
                return index5;
            }
            index2 = skipWhiteSpace(index5 + 1);
        }
    }

    private int parsePluralOrSelectStyle(ArgType argType, int index, int nestingLevel) {
        int index2;
        boolean isEmpty = true;
        boolean hasOther = false;
        while (true) {
            int index3 = skipWhiteSpace(index);
            boolean eos = index3 == this.msg.length();
            if (eos || this.msg.charAt(index3) == '}') {
                break;
            }
            if (argType.hasPluralStyle() && this.msg.charAt(index3) == '=') {
                index2 = skipDouble(index3 + 1);
                int length = index2 - index3;
                if (length == 1) {
                    throw new IllegalArgumentException("Bad " + argType.toString().toLowerCase(Locale.ENGLISH) + " pattern syntax: " + prefix(index));
                }
                if (length > 65535) {
                    throw new IndexOutOfBoundsException("Argument selector too long: " + prefix(index3));
                }
                addPart(Part.Type.ARG_SELECTOR, index3, length, 0);
                parseDouble(index3 + 1, index2, false);
            } else {
                index2 = skipIdentifier(index3);
                int length2 = index2 - index3;
                if (length2 == 0) {
                    throw new IllegalArgumentException("Bad " + argType.toString().toLowerCase(Locale.ENGLISH) + " pattern syntax: " + prefix(index));
                }
                if (argType.hasPluralStyle() && length2 == 6 && index2 < this.msg.length() && this.msg.regionMatches(index3, "offset:", 0, 7)) {
                    if (!isEmpty) {
                        throw new IllegalArgumentException("Plural argument 'offset:' (if present) must precede key-message pairs: " + prefix(index));
                    }
                    int valueIndex = skipWhiteSpace(index2 + 1);
                    index = skipDouble(valueIndex);
                    if (index == valueIndex) {
                        throw new IllegalArgumentException("Missing value for plural 'offset:' " + prefix(index));
                    }
                    if (index - valueIndex > 65535) {
                        throw new IndexOutOfBoundsException("Plural offset value too long: " + prefix(valueIndex));
                    }
                    parseDouble(valueIndex, index, false);
                    isEmpty = false;
                } else {
                    if (length2 > 65535) {
                        throw new IndexOutOfBoundsException("Argument selector too long: " + prefix(index3));
                    }
                    addPart(Part.Type.ARG_SELECTOR, index3, length2, 0);
                    if (this.msg.regionMatches(index3, PluralRules.KEYWORD_OTHER, 0, length2)) {
                        hasOther = true;
                    }
                }
            }
            int index4 = skipWhiteSpace(index2);
            if (index4 == this.msg.length() || this.msg.charAt(index4) != '{') {
                break;
            }
            index = parseMessage(index4, 1, nestingLevel + 1, argType);
            isEmpty = false;
        }
    }

    private static int parseArgNumber(CharSequence s, int start, int limit) {
        int number;
        boolean badNumber;
        if (start >= limit) {
            return -2;
        }
        int start2 = start + 1;
        char c = s.charAt(start);
        if (c == '0') {
            if (start2 == limit) {
                return 0;
            }
            number = 0;
            badNumber = true;
        } else {
            if ('1' > c || c > '9') {
                return -1;
            }
            number = c - '0';
            badNumber = false;
        }
        while (start2 < limit) {
            int start3 = start2 + 1;
            char c2 = s.charAt(start2);
            if ('0' > c2 || c2 > '9') {
                return -1;
            }
            if (number >= 214748364) {
                badNumber = true;
            }
            number = (number * 10) + (c2 - '0');
            start2 = start3;
        }
        if (badNumber) {
            return -2;
        }
        return number;
    }

    private int parseArgNumber(int start, int limit) {
        return parseArgNumber(this.msg, start, limit);
    }

    private void parseDouble(int start, int limit, boolean allowInfinity) {
        if (!f73assertionsDisabled) {
            if (!(start < limit)) {
                throw new AssertionError();
            }
        }
        int value = 0;
        int isNegative = 0;
        int index = start + 1;
        char c = this.msg.charAt(start);
        if (c == '-') {
            isNegative = 1;
            if (index != limit) {
                c = this.msg.charAt(index);
                index++;
                if (c == 8734) {
                    if (allowInfinity && index == limit) {
                        addArgDoublePart(isNegative != 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY, start, limit - start);
                        return;
                    }
                } else {
                    while (true) {
                        int index2 = index;
                        if ('0' > c || c > '9' || (value = (value * 10) + (c - '0')) > isNegative + 32767) {
                            break;
                        }
                        if (index2 == limit) {
                            Part.Type type = Part.Type.ARG_INT;
                            int i = limit - start;
                            if (isNegative != 0) {
                                value = -value;
                            }
                            addPart(type, start, i, value);
                            return;
                        }
                        index = index2 + 1;
                        c = this.msg.charAt(index2);
                    }
                    double numericValue = Double.parseDouble(this.msg.substring(start, limit));
                    addArgDoublePart(numericValue, start, limit - start);
                    return;
                }
            }
        } else if (c == '+') {
            if (index != limit) {
                c = this.msg.charAt(index);
                index++;
                if (c == 8734) {
                }
            }
        } else if (c == 8734) {
        }
        throw new NumberFormatException("Bad syntax for numeric value: " + this.msg.substring(start, limit));
    }

    static void appendReducedApostrophes(String s, int start, int limit, StringBuilder sb) {
        int doubleApos = -1;
        while (true) {
            int i = s.indexOf(39, start);
            if (i < 0 || i >= limit) {
                break;
            }
            if (i == doubleApos) {
                sb.append(PatternTokenizer.SINGLE_QUOTE);
                start++;
                doubleApos = -1;
            } else {
                sb.append((CharSequence) s, start, i);
                start = i + 1;
                doubleApos = start;
            }
        }
        sb.append((CharSequence) s, start, limit);
    }

    private int skipWhiteSpace(int index) {
        return PatternProps.skipWhiteSpace(this.msg, index);
    }

    private int skipIdentifier(int index) {
        return PatternProps.skipIdentifier(this.msg, index);
    }

    private int skipDouble(int index) {
        char c;
        while (index < this.msg.length() && (((c = this.msg.charAt(index)) >= '0' || "+-.".indexOf(c) >= 0) && (c <= '9' || c == 'e' || c == 'E' || c == 8734))) {
            index++;
        }
        return index;
    }

    private static boolean isArgTypeChar(int c) {
        if (97 > c || c > 122) {
            return 65 <= c && c <= 90;
        }
        return true;
    }

    private boolean isChoice(int index) {
        boolean z = true;
        int index2 = index + 1;
        char c = this.msg.charAt(index);
        if (c == 'c' || c == 'C') {
            int index3 = index2 + 1;
            char c2 = this.msg.charAt(index2);
            if (c2 == 'h' || c2 == 'H') {
                int index4 = index3 + 1;
                char c3 = this.msg.charAt(index3);
                if (c3 == 'o' || c3 == 'O') {
                    int index5 = index4 + 1;
                    char c4 = this.msg.charAt(index4);
                    if (c4 == 'i' || c4 == 'I') {
                        int index6 = index5 + 1;
                        char c5 = this.msg.charAt(index5);
                        if (c5 == 'c' || c5 == 'C') {
                            char c6 = this.msg.charAt(index6);
                            if (c6 != 'e' && c6 != 'E') {
                                z = false;
                            }
                            return z;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isPlural(int index) {
        boolean z = true;
        int index2 = index + 1;
        char c = this.msg.charAt(index);
        if (c == 'p' || c == 'P') {
            int index3 = index2 + 1;
            char c2 = this.msg.charAt(index2);
            if (c2 == 'l' || c2 == 'L') {
                int index4 = index3 + 1;
                char c3 = this.msg.charAt(index3);
                if (c3 == 'u' || c3 == 'U') {
                    int index5 = index4 + 1;
                    char c4 = this.msg.charAt(index4);
                    if (c4 == 'r' || c4 == 'R') {
                        int index6 = index5 + 1;
                        char c5 = this.msg.charAt(index5);
                        if (c5 == 'a' || c5 == 'A') {
                            char c6 = this.msg.charAt(index6);
                            if (c6 != 'l' && c6 != 'L') {
                                z = false;
                            }
                            return z;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isSelect(int index) {
        boolean z = true;
        int index2 = index + 1;
        char c = this.msg.charAt(index);
        if (c == 's' || c == 'S') {
            int index3 = index2 + 1;
            char c2 = this.msg.charAt(index2);
            if (c2 == 'e' || c2 == 'E') {
                int index4 = index3 + 1;
                char c3 = this.msg.charAt(index3);
                if (c3 == 'l' || c3 == 'L') {
                    int index5 = index4 + 1;
                    char c4 = this.msg.charAt(index4);
                    if (c4 == 'e' || c4 == 'E') {
                        int index6 = index5 + 1;
                        char c5 = this.msg.charAt(index5);
                        if (c5 == 'c' || c5 == 'C') {
                            char c6 = this.msg.charAt(index6);
                            if (c6 != 't' && c6 != 'T') {
                                z = false;
                            }
                            return z;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isOrdinal(int index) {
        int index2 = index + 1;
        char c = this.msg.charAt(index);
        if (c == 'o' || c == 'O') {
            int index3 = index2 + 1;
            char c2 = this.msg.charAt(index2);
            if (c2 == 'r' || c2 == 'R') {
                int index4 = index3 + 1;
                char c3 = this.msg.charAt(index3);
                if (c3 == 'd' || c3 == 'D') {
                    int index5 = index4 + 1;
                    char c4 = this.msg.charAt(index4);
                    if (c4 == 'i' || c4 == 'I') {
                        int index6 = index5 + 1;
                        char c5 = this.msg.charAt(index5);
                        if (c5 == 'n' || c5 == 'N') {
                            int index7 = index6 + 1;
                            char c6 = this.msg.charAt(index6);
                            if (c6 == 'a' || c6 == 'A') {
                                char c7 = this.msg.charAt(index7);
                                return c7 == 'l' || c7 == 'L';
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean inMessageFormatPattern(int nestingLevel) {
        return nestingLevel > 0 || this.parts.get(0).type == Part.Type.MSG_START;
    }

    private boolean inTopLevelChoiceMessage(int nestingLevel, ArgType parentType) {
        if (nestingLevel == 1 && parentType == ArgType.CHOICE) {
            return this.parts.get(0).type != Part.Type.MSG_START;
        }
        return false;
    }

    private void addPart(Part.Type type, int index, int length, int value) {
        this.parts.add(new Part(type, index, length, value, null));
    }

    private void addLimitPart(int start, Part.Type type, int index, int length, int value) {
        this.parts.get(start).limitPartIndex = this.parts.size();
        addPart(type, index, length, value);
    }

    private void addArgDoublePart(double numericValue, int start, int length) {
        int numericIndex;
        if (this.numericValues == null) {
            this.numericValues = new ArrayList<>();
            numericIndex = 0;
        } else {
            numericIndex = this.numericValues.size();
            if (numericIndex > 32767) {
                throw new IndexOutOfBoundsException("Too many numeric values");
            }
        }
        this.numericValues.add(Double.valueOf(numericValue));
        addPart(Part.Type.ARG_DOUBLE, start, length, numericIndex);
    }

    private static String prefix(String s, int start) {
        StringBuilder prefix = new StringBuilder(44);
        if (start == 0) {
            prefix.append("\"");
        } else {
            prefix.append("[at pattern index ").append(start).append("] \"");
        }
        int substringLength = s.length() - start;
        if (substringLength <= 24) {
            if (start != 0) {
                s = s.substring(start);
            }
            prefix.append(s);
        } else {
            int limit = (start + 24) - 4;
            if (Character.isHighSurrogate(s.charAt(limit - 1))) {
                limit--;
            }
            prefix.append((CharSequence) s, start, limit).append(" ...");
        }
        return prefix.append("\"").toString();
    }

    private static String prefix(String s) {
        return prefix(s, 0);
    }

    private String prefix(int start) {
        return prefix(this.msg, start);
    }

    private String prefix() {
        return prefix(this.msg, 0);
    }

    static {
        f73assertionsDisabled = !MessagePattern.class.desiredAssertionStatus();
        defaultAposMode = ApostropheMode.valueOf(ICUConfig.get("android.icu.text.MessagePattern.ApostropheMode", "DOUBLE_OPTIONAL"));
        argTypes = ArgType.valuesCustom();
    }
}
