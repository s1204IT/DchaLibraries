package android.icu.text;

import android.icu.impl.ICUConfig;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import java.util.ArrayList;
import java.util.Locale;

public final class MessagePattern implements Cloneable, Freezable<MessagePattern> {
    static final boolean $assertionsDisabled = false;
    public static final int ARG_NAME_NOT_NUMBER = -1;
    public static final int ARG_NAME_NOT_VALID = -2;
    private static final int MAX_PREFIX_LENGTH = 24;
    public static final double NO_NUMERIC_VALUE = -1.23456789E8d;
    private ApostropheMode aposMode;
    private volatile boolean frozen;
    private boolean hasArgNames;
    private boolean hasArgNumbers;
    private String msg;
    private boolean needsAutoQuoting;
    private ArrayList<Double> numericValues;
    private ArrayList<Part> parts;
    private static final ApostropheMode defaultAposMode = ApostropheMode.valueOf(ICUConfig.get("android.icu.text.MessagePattern.ApostropheMode", "DOUBLE_OPTIONAL"));
    private static final ArgType[] argTypes = ArgType.values();

    public enum ApostropheMode {
        DOUBLE_OPTIONAL,
        DOUBLE_REQUIRED
    }

    public MessagePattern() {
        this.parts = new ArrayList<>();
        this.aposMode = defaultAposMode;
    }

    public MessagePattern(ApostropheMode apostropheMode) {
        this.parts = new ArrayList<>();
        this.aposMode = apostropheMode;
    }

    public MessagePattern(String str) {
        this.parts = new ArrayList<>();
        this.aposMode = defaultAposMode;
        parse(str);
    }

    public MessagePattern parse(String str) {
        preParse(str);
        parseMessage(0, 0, 0, ArgType.NONE);
        postParse();
        return this;
    }

    public MessagePattern parseChoiceStyle(String str) {
        preParse(str);
        parseChoiceStyle(0, 0);
        postParse();
        return this;
    }

    public MessagePattern parsePluralStyle(String str) {
        preParse(str);
        parsePluralOrSelectStyle(ArgType.PLURAL, 0, 0);
        postParse();
        return this;
    }

    public MessagePattern parseSelectStyle(String str) {
        preParse(str);
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
        if (this.numericValues != null) {
            this.numericValues.clear();
        }
    }

    public void clearPatternAndSetApostropheMode(ApostropheMode apostropheMode) {
        clear();
        this.aposMode = apostropheMode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessagePattern messagePattern = (MessagePattern) obj;
        if (this.aposMode.equals(messagePattern.aposMode) && (this.msg != null ? this.msg.equals(messagePattern.msg) : messagePattern.msg == null) && this.parts.equals(messagePattern.parts)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (((this.aposMode.hashCode() * 37) + (this.msg != null ? this.msg.hashCode() : 0)) * 37) + this.parts.hashCode();
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

    public static int validateArgumentName(String str) {
        if (!PatternProps.isIdentifier(str)) {
            return -2;
        }
        return parseArgNumber(str, 0, str.length());
    }

    public String autoQuoteApostropheDeep() {
        if (!this.needsAutoQuoting) {
            return this.msg;
        }
        StringBuilder sb = null;
        int iCountParts = countParts();
        while (iCountParts > 0) {
            iCountParts--;
            Part part = getPart(iCountParts);
            if (part.getType() == Part.Type.INSERT_CHAR) {
                if (sb == null) {
                    sb = new StringBuilder(this.msg.length() + 10);
                    sb.append(this.msg);
                }
                sb.insert(part.index, (char) part.value);
            }
        }
        if (sb == null) {
            return this.msg;
        }
        return sb.toString();
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

    public int getPatternIndex(int i) {
        return this.parts.get(i).index;
    }

    public String getSubstring(Part part) {
        int i = part.index;
        return this.msg.substring(i, part.length + i);
    }

    public boolean partSubstringMatches(Part part, String str) {
        return part.length == str.length() && this.msg.regionMatches(part.index, str, 0, part.length);
    }

    public double getNumericValue(Part part) {
        Part.Type type = part.type;
        if (type != Part.Type.ARG_INT) {
            if (type != Part.Type.ARG_DOUBLE) {
                return -1.23456789E8d;
            }
            return this.numericValues.get(part.value).doubleValue();
        }
        return part.value;
    }

    public double getPluralOffset(int i) {
        Part part = this.parts.get(i);
        if (part.type.hasNumericValue()) {
            return getNumericValue(part);
        }
        return 0.0d;
    }

    public int getLimitPartIndex(int i) {
        int i2 = this.parts.get(i).limitPartIndex;
        if (i2 < i) {
            return i;
        }
        return i2;
    }

    public static final class Part {
        private static final int MAX_LENGTH = 65535;
        private static final int MAX_VALUE = 32767;
        private final int index;
        private final char length;
        private int limitPartIndex;
        private final Type type;
        private short value;

        private Part(Type type, int i, int i2, int i3) {
            this.type = type;
            this.index = i;
            this.length = (char) i2;
            this.value = (short) i3;
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

            public boolean hasNumericValue() {
                return this == ARG_INT || this == ARG_DOUBLE;
            }
        }

        public String toString() {
            return this.type.name() + "(" + ((this.type == Type.ARG_START || this.type == Type.ARG_LIMIT) ? getArgType().name() : Integer.toString(this.value)) + ")@" + this.index;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Part part = (Part) obj;
            if (this.type.equals(part.type) && this.index == part.index && this.length == part.length && this.value == part.value && this.limitPartIndex == part.limitPartIndex) {
                return true;
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
            MessagePattern messagePattern = (MessagePattern) super.clone();
            messagePattern.parts = (ArrayList) this.parts.clone();
            if (this.numericValues != null) {
                messagePattern.numericValues = (ArrayList) this.numericValues.clone();
            }
            messagePattern.frozen = false;
            return messagePattern;
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

    private void preParse(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to parse(" + prefix(str) + ") on frozen MessagePattern instance.");
        }
        this.msg = str;
        this.hasArgNumbers = false;
        this.hasArgNames = false;
        this.needsAutoQuoting = false;
        this.parts.clear();
        if (this.numericValues != null) {
            this.numericValues.clear();
        }
    }

    private void postParse() {
    }

    private int parseMessage(int i, int i2, int i3, ArgType argType) {
        int iIndexOf;
        if (i3 > 32767) {
            throw new IndexOutOfBoundsException();
        }
        int size = this.parts.size();
        addPart(Part.Type.MSG_START, i, i2, i3);
        int length = i + i2;
        while (length < this.msg.length()) {
            int i4 = length + 1;
            char cCharAt = this.msg.charAt(length);
            if (cCharAt == '\'') {
                if (i4 == this.msg.length()) {
                    addPart(Part.Type.INSERT_CHAR, i4, 0, 39);
                    this.needsAutoQuoting = true;
                } else {
                    char cCharAt2 = this.msg.charAt(i4);
                    if (cCharAt2 == '\'') {
                        addPart(Part.Type.SKIP_SYNTAX, i4, 1, 0);
                        length = i4 + 1;
                    } else if (this.aposMode == ApostropheMode.DOUBLE_REQUIRED || cCharAt2 == '{' || cCharAt2 == '}' || ((argType == ArgType.CHOICE && cCharAt2 == '|') || (argType.hasPluralStyle() && cCharAt2 == '#'))) {
                        addPart(Part.Type.SKIP_SYNTAX, i4 - 1, 1, 0);
                        while (true) {
                            iIndexOf = this.msg.indexOf(39, i4 + 1);
                            if (iIndexOf >= 0) {
                                i4 = iIndexOf + 1;
                                if (i4 >= this.msg.length() || this.msg.charAt(i4) != '\'') {
                                    break;
                                }
                                addPart(Part.Type.SKIP_SYNTAX, i4, 1, 0);
                            } else {
                                length = this.msg.length();
                                addPart(Part.Type.INSERT_CHAR, length, 0, 39);
                                this.needsAutoQuoting = true;
                                break;
                            }
                        }
                        addPart(Part.Type.SKIP_SYNTAX, iIndexOf, 1, 0);
                    } else {
                        addPart(Part.Type.INSERT_CHAR, i4, 0, 39);
                        this.needsAutoQuoting = true;
                    }
                }
                length = i4;
            } else {
                if (argType.hasPluralStyle() && cCharAt == '#') {
                    addPart(Part.Type.REPLACE_NUMBER, i4 - 1, 1, 0);
                } else if (cCharAt == '{') {
                    length = parseArg(i4 - 1, 1, i3);
                } else if ((i3 > 0 && cCharAt == '}') || (argType == ArgType.CHOICE && cCharAt == '|')) {
                    int i5 = (argType == ArgType.CHOICE && cCharAt == '}') ? 0 : 1;
                    int i6 = i4 - 1;
                    addLimitPart(size, Part.Type.MSG_LIMIT, i6, i5, i3);
                    if (argType == ArgType.CHOICE) {
                        return i6;
                    }
                    return i4;
                }
                length = i4;
            }
        }
        if (i3 > 0 && !inTopLevelChoiceMessage(i3, argType)) {
            throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
        }
        addLimitPart(size, Part.Type.MSG_LIMIT, length, 0, i3);
        return length;
    }

    private int parseArg(int i, int i2, int i3) {
        char cCharAt;
        int simpleStyle;
        int size = this.parts.size();
        ArgType argType = ArgType.NONE;
        addPart(Part.Type.ARG_START, i, i2, argType.ordinal());
        int iSkipWhiteSpace = skipWhiteSpace(i + i2);
        if (iSkipWhiteSpace == this.msg.length()) {
            throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
        }
        int iSkipIdentifier = skipIdentifier(iSkipWhiteSpace);
        int argNumber = parseArgNumber(iSkipWhiteSpace, iSkipIdentifier);
        if (argNumber >= 0) {
            int i4 = iSkipIdentifier - iSkipWhiteSpace;
            if (i4 > 65535 || argNumber > 32767) {
                throw new IndexOutOfBoundsException("Argument number too large: " + prefix(iSkipWhiteSpace));
            }
            this.hasArgNumbers = true;
            addPart(Part.Type.ARG_NUMBER, iSkipWhiteSpace, i4, argNumber);
        } else if (argNumber == -1) {
            int i5 = iSkipIdentifier - iSkipWhiteSpace;
            if (i5 > 65535) {
                throw new IndexOutOfBoundsException("Argument name too long: " + prefix(iSkipWhiteSpace));
            }
            this.hasArgNames = true;
            addPart(Part.Type.ARG_NAME, iSkipWhiteSpace, i5, 0);
        } else {
            throw new IllegalArgumentException("Bad argument syntax: " + prefix(iSkipWhiteSpace));
        }
        int iSkipWhiteSpace2 = skipWhiteSpace(iSkipIdentifier);
        if (iSkipWhiteSpace2 == this.msg.length()) {
            throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
        }
        char cCharAt2 = this.msg.charAt(iSkipWhiteSpace2);
        if (cCharAt2 != '}') {
            if (cCharAt2 != ',') {
                throw new IllegalArgumentException("Bad argument syntax: " + prefix(iSkipWhiteSpace));
            }
            int iSkipWhiteSpace3 = skipWhiteSpace(iSkipWhiteSpace2 + 1);
            int i6 = iSkipWhiteSpace3;
            while (i6 < this.msg.length() && isArgTypeChar(this.msg.charAt(i6))) {
                i6++;
            }
            int i7 = i6 - iSkipWhiteSpace3;
            int iSkipWhiteSpace4 = skipWhiteSpace(i6);
            if (iSkipWhiteSpace4 == this.msg.length()) {
                throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
            }
            if (i7 == 0 || ((cCharAt = this.msg.charAt(iSkipWhiteSpace4)) != ',' && cCharAt != '}')) {
                throw new IllegalArgumentException("Bad argument syntax: " + prefix(iSkipWhiteSpace));
            }
            if (i7 > 65535) {
                throw new IndexOutOfBoundsException("Argument type name too long: " + prefix(iSkipWhiteSpace));
            }
            argType = ArgType.SIMPLE;
            if (i7 == 6) {
                if (isChoice(iSkipWhiteSpace3)) {
                    argType = ArgType.CHOICE;
                } else if (isPlural(iSkipWhiteSpace3)) {
                    argType = ArgType.PLURAL;
                } else if (isSelect(iSkipWhiteSpace3)) {
                    argType = ArgType.SELECT;
                }
            } else if (i7 == 13 && isSelect(iSkipWhiteSpace3) && isOrdinal(iSkipWhiteSpace3 + 6)) {
                argType = ArgType.SELECTORDINAL;
            }
            this.parts.get(size).value = (short) argType.ordinal();
            if (argType == ArgType.SIMPLE) {
                addPart(Part.Type.ARG_TYPE, iSkipWhiteSpace3, i7, 0);
            }
            if (cCharAt == '}') {
                if (argType != ArgType.SIMPLE) {
                    throw new IllegalArgumentException("No style field for complex argument: " + prefix(iSkipWhiteSpace));
                }
                simpleStyle = iSkipWhiteSpace4;
            } else {
                int i8 = iSkipWhiteSpace4 + 1;
                simpleStyle = argType == ArgType.SIMPLE ? parseSimpleStyle(i8) : argType == ArgType.CHOICE ? parseChoiceStyle(i8, i3) : parsePluralOrSelectStyle(argType, i8, i3);
            }
        } else {
            simpleStyle = iSkipWhiteSpace2;
        }
        addLimitPart(size, Part.Type.ARG_LIMIT, simpleStyle, 1, argType.ordinal());
        return simpleStyle + 1;
    }

    private int parseSimpleStyle(int i) {
        int i2 = i;
        int i3 = 0;
        while (i2 < this.msg.length()) {
            int i4 = i2 + 1;
            char cCharAt = this.msg.charAt(i2);
            if (cCharAt == '\'') {
                int iIndexOf = this.msg.indexOf(39, i4);
                if (iIndexOf < 0) {
                    throw new IllegalArgumentException("Quoted literal argument style text reaches to the end of the message: " + prefix(i));
                }
                i2 = iIndexOf + 1;
            } else {
                if (cCharAt == '{') {
                    i3++;
                } else if (cCharAt == '}') {
                    if (i3 > 0) {
                        i3--;
                    } else {
                        int i5 = i4 - 1;
                        int i6 = i5 - i;
                        if (i6 <= 65535) {
                            addPart(Part.Type.ARG_STYLE, i, i6, 0);
                            return i5;
                        }
                        throw new IndexOutOfBoundsException("Argument style text too long: " + prefix(i));
                    }
                }
                i2 = i4;
            }
        }
        throw new IllegalArgumentException("Unmatched '{' braces in message " + prefix());
    }

    private int parseChoiceStyle(int i, int i2) {
        int iSkipWhiteSpace = skipWhiteSpace(i);
        if (iSkipWhiteSpace == this.msg.length() || this.msg.charAt(iSkipWhiteSpace) == '}') {
            throw new IllegalArgumentException("Missing choice argument pattern in " + prefix());
        }
        while (true) {
            int iSkipDouble = skipDouble(iSkipWhiteSpace);
            int i3 = iSkipDouble - iSkipWhiteSpace;
            if (i3 == 0) {
                throw new IllegalArgumentException("Bad choice pattern syntax: " + prefix(i));
            }
            if (i3 > 65535) {
                throw new IndexOutOfBoundsException("Choice number too long: " + prefix(iSkipWhiteSpace));
            }
            parseDouble(iSkipWhiteSpace, iSkipDouble, true);
            int iSkipWhiteSpace2 = skipWhiteSpace(iSkipDouble);
            if (iSkipWhiteSpace2 == this.msg.length()) {
                throw new IllegalArgumentException("Bad choice pattern syntax: " + prefix(i));
            }
            char cCharAt = this.msg.charAt(iSkipWhiteSpace2);
            if (cCharAt == '#' || cCharAt == '<' || cCharAt == 8804) {
                addPart(Part.Type.ARG_SELECTOR, iSkipWhiteSpace2, 1, 0);
                int message = parseMessage(iSkipWhiteSpace2 + 1, 0, i2 + 1, ArgType.CHOICE);
                if (message == this.msg.length()) {
                    return message;
                }
                if (this.msg.charAt(message) == '}') {
                    if (!inMessageFormatPattern(i2)) {
                        throw new IllegalArgumentException("Bad choice pattern syntax: " + prefix(i));
                    }
                    return message;
                }
                iSkipWhiteSpace = skipWhiteSpace(message + 1);
            } else {
                throw new IllegalArgumentException("Expected choice separator (#<≤) instead of '" + cCharAt + "' in choice pattern " + prefix(i));
            }
        }
    }

    private int parsePluralOrSelectStyle(ArgType argType, int i, int i2) {
        int iSkipWhiteSpace;
        int iSkipIdentifier;
        int message = i;
        boolean z = true;
        boolean z2 = false;
        while (true) {
            iSkipWhiteSpace = skipWhiteSpace(message);
            boolean z3 = iSkipWhiteSpace == this.msg.length();
            if (z3 || this.msg.charAt(iSkipWhiteSpace) == '}') {
                break;
            }
            if (argType.hasPluralStyle() && this.msg.charAt(iSkipWhiteSpace) == '=') {
                int i3 = iSkipWhiteSpace + 1;
                iSkipIdentifier = skipDouble(i3);
                int i4 = iSkipIdentifier - iSkipWhiteSpace;
                if (i4 == 1) {
                    throw new IllegalArgumentException("Bad " + argType.toString().toLowerCase(Locale.ENGLISH) + " pattern syntax: " + prefix(i));
                }
                if (i4 > 65535) {
                    throw new IndexOutOfBoundsException("Argument selector too long: " + prefix(iSkipWhiteSpace));
                }
                addPart(Part.Type.ARG_SELECTOR, iSkipWhiteSpace, i4, 0);
                parseDouble(i3, iSkipIdentifier, false);
            } else {
                iSkipIdentifier = skipIdentifier(iSkipWhiteSpace);
                int i5 = iSkipIdentifier - iSkipWhiteSpace;
                if (i5 == 0) {
                    throw new IllegalArgumentException("Bad " + argType.toString().toLowerCase(Locale.ENGLISH) + " pattern syntax: " + prefix(i));
                }
                if (argType.hasPluralStyle() && i5 == 6 && iSkipIdentifier < this.msg.length() && this.msg.regionMatches(iSkipWhiteSpace, "offset:", 0, 7)) {
                    if (!z) {
                        throw new IllegalArgumentException("Plural argument 'offset:' (if present) must precede key-message pairs: " + prefix(i));
                    }
                    int iSkipWhiteSpace2 = skipWhiteSpace(iSkipIdentifier + 1);
                    int iSkipDouble = skipDouble(iSkipWhiteSpace2);
                    if (iSkipDouble == iSkipWhiteSpace2) {
                        throw new IllegalArgumentException("Missing value for plural 'offset:' " + prefix(i));
                    }
                    if (iSkipDouble - iSkipWhiteSpace2 > 65535) {
                        throw new IndexOutOfBoundsException("Plural offset value too long: " + prefix(iSkipWhiteSpace2));
                    }
                    parseDouble(iSkipWhiteSpace2, iSkipDouble, false);
                    message = iSkipDouble;
                    z = false;
                } else {
                    if (i5 > 65535) {
                        throw new IndexOutOfBoundsException("Argument selector too long: " + prefix(iSkipWhiteSpace));
                    }
                    addPart(Part.Type.ARG_SELECTOR, iSkipWhiteSpace, i5, 0);
                    if (this.msg.regionMatches(iSkipWhiteSpace, PluralRules.KEYWORD_OTHER, 0, i5)) {
                        z2 = true;
                    }
                }
            }
            int iSkipWhiteSpace3 = skipWhiteSpace(iSkipIdentifier);
            if (iSkipWhiteSpace3 == this.msg.length() || this.msg.charAt(iSkipWhiteSpace3) != '{') {
                break;
            }
            message = parseMessage(iSkipWhiteSpace3, 1, i2 + 1, argType);
            z = false;
        }
        throw new IllegalArgumentException("No message fragment after " + argType.toString().toLowerCase(Locale.ENGLISH) + " selector: " + prefix(iSkipWhiteSpace));
    }

    private static int parseArgNumber(CharSequence charSequence, int i, int i2) {
        int i3;
        if (i >= i2) {
            return -2;
        }
        int i4 = i + 1;
        char cCharAt = charSequence.charAt(i);
        boolean z = false;
        if (cCharAt == '0') {
            if (i4 == i2) {
                return 0;
            }
            i3 = 0;
            z = true;
        } else {
            if ('1' > cCharAt || cCharAt > '9') {
                return -1;
            }
            i3 = cCharAt - '0';
        }
        while (i4 < i2) {
            int i5 = i4 + 1;
            char cCharAt2 = charSequence.charAt(i4);
            if ('0' > cCharAt2 || cCharAt2 > '9') {
                return -1;
            }
            if (i3 >= 214748364) {
                z = true;
            }
            i3 = (i3 * 10) + (cCharAt2 - '0');
            i4 = i5;
        }
        if (z) {
            return -2;
        }
        return i3;
    }

    private int parseArgNumber(int i, int i2) {
        return parseArgNumber(this.msg, i, i2);
    }

    private void parseDouble(int i, int i2, boolean z) {
        int i3;
        int i4;
        int i5 = i + 1;
        char cCharAt = this.msg.charAt(i);
        int i6 = 0;
        if (cCharAt == '-') {
            if (i5 != i2) {
                i3 = i5 + 1;
                cCharAt = this.msg.charAt(i5);
                i4 = 1;
                if (cCharAt != 8734) {
                    if (z && i3 == i2) {
                        addArgDoublePart(i4 != 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY, i, i2 - i);
                        return;
                    }
                } else {
                    while ('0' <= cCharAt && cCharAt <= '9' && (i6 = (i6 * 10) + (cCharAt - '0')) <= 32767 + i4) {
                        if (i3 == i2) {
                            Part.Type type = Part.Type.ARG_INT;
                            int i7 = i2 - i;
                            if (i4 != 0) {
                                i6 = -i6;
                            }
                            addPart(type, i, i7, i6);
                            return;
                        }
                        char cCharAt2 = this.msg.charAt(i3);
                        i3++;
                        cCharAt = cCharAt2;
                    }
                    addArgDoublePart(Double.parseDouble(this.msg.substring(i, i2)), i, i2 - i);
                    return;
                }
            }
        } else {
            if (cCharAt == '+') {
                if (i5 != i2) {
                    i3 = i5 + 1;
                    cCharAt = this.msg.charAt(i5);
                }
            } else {
                i3 = i5;
            }
            i4 = 0;
            if (cCharAt != 8734) {
            }
        }
        throw new NumberFormatException("Bad syntax for numeric value: " + this.msg.substring(i, i2));
    }

    static void appendReducedApostrophes(String str, int i, int i2, StringBuilder sb) {
        int i3 = -1;
        while (true) {
            int iIndexOf = str.indexOf(39, i);
            if (iIndexOf < 0 || iIndexOf >= i2) {
                break;
            }
            if (iIndexOf == i3) {
                sb.append(PatternTokenizer.SINGLE_QUOTE);
                i++;
                i3 = -1;
            } else {
                sb.append((CharSequence) str, i, iIndexOf);
                i = iIndexOf + 1;
                i3 = i;
            }
        }
        sb.append((CharSequence) str, i, i2);
    }

    private int skipWhiteSpace(int i) {
        return PatternProps.skipWhiteSpace(this.msg, i);
    }

    private int skipIdentifier(int i) {
        return PatternProps.skipIdentifier(this.msg, i);
    }

    private int skipDouble(int i) {
        char cCharAt;
        while (i < this.msg.length() && (((cCharAt = this.msg.charAt(i)) >= '0' || "+-.".indexOf(cCharAt) >= 0) && (cCharAt <= '9' || cCharAt == 'e' || cCharAt == 'E' || cCharAt == 8734))) {
            i++;
        }
        return i;
    }

    private static boolean isArgTypeChar(int i) {
        return (97 <= i && i <= 122) || (65 <= i && i <= 90);
    }

    private boolean isChoice(int i) {
        char cCharAt;
        int i2 = i + 1;
        char cCharAt2 = this.msg.charAt(i);
        if (cCharAt2 == 'c' || cCharAt2 == 'C') {
            int i3 = i2 + 1;
            char cCharAt3 = this.msg.charAt(i2);
            if (cCharAt3 == 'h' || cCharAt3 == 'H') {
                int i4 = i3 + 1;
                char cCharAt4 = this.msg.charAt(i3);
                if (cCharAt4 == 'o' || cCharAt4 == 'O') {
                    int i5 = i4 + 1;
                    char cCharAt5 = this.msg.charAt(i4);
                    if (cCharAt5 == 'i' || cCharAt5 == 'I') {
                        int i6 = i5 + 1;
                        char cCharAt6 = this.msg.charAt(i5);
                        if ((cCharAt6 == 'c' || cCharAt6 == 'C') && ((cCharAt = this.msg.charAt(i6)) == 'e' || cCharAt == 'E')) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isPlural(int i) {
        char cCharAt;
        int i2 = i + 1;
        char cCharAt2 = this.msg.charAt(i);
        if (cCharAt2 == 'p' || cCharAt2 == 'P') {
            int i3 = i2 + 1;
            char cCharAt3 = this.msg.charAt(i2);
            if (cCharAt3 == 'l' || cCharAt3 == 'L') {
                int i4 = i3 + 1;
                char cCharAt4 = this.msg.charAt(i3);
                if (cCharAt4 == 'u' || cCharAt4 == 'U') {
                    int i5 = i4 + 1;
                    char cCharAt5 = this.msg.charAt(i4);
                    if (cCharAt5 == 'r' || cCharAt5 == 'R') {
                        int i6 = i5 + 1;
                        char cCharAt6 = this.msg.charAt(i5);
                        if ((cCharAt6 == 'a' || cCharAt6 == 'A') && ((cCharAt = this.msg.charAt(i6)) == 'l' || cCharAt == 'L')) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isSelect(int i) {
        char cCharAt;
        int i2 = i + 1;
        char cCharAt2 = this.msg.charAt(i);
        if (cCharAt2 == 's' || cCharAt2 == 'S') {
            int i3 = i2 + 1;
            char cCharAt3 = this.msg.charAt(i2);
            if (cCharAt3 == 'e' || cCharAt3 == 'E') {
                int i4 = i3 + 1;
                char cCharAt4 = this.msg.charAt(i3);
                if (cCharAt4 == 'l' || cCharAt4 == 'L') {
                    int i5 = i4 + 1;
                    char cCharAt5 = this.msg.charAt(i4);
                    if (cCharAt5 == 'e' || cCharAt5 == 'E') {
                        int i6 = i5 + 1;
                        char cCharAt6 = this.msg.charAt(i5);
                        if ((cCharAt6 == 'c' || cCharAt6 == 'C') && ((cCharAt = this.msg.charAt(i6)) == 't' || cCharAt == 'T')) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isOrdinal(int i) {
        char cCharAt;
        int i2 = i + 1;
        char cCharAt2 = this.msg.charAt(i);
        if (cCharAt2 == 'o' || cCharAt2 == 'O') {
            int i3 = i2 + 1;
            char cCharAt3 = this.msg.charAt(i2);
            if (cCharAt3 == 'r' || cCharAt3 == 'R') {
                int i4 = i3 + 1;
                char cCharAt4 = this.msg.charAt(i3);
                if (cCharAt4 == 'd' || cCharAt4 == 'D') {
                    int i5 = i4 + 1;
                    char cCharAt5 = this.msg.charAt(i4);
                    if (cCharAt5 == 'i' || cCharAt5 == 'I') {
                        int i6 = i5 + 1;
                        char cCharAt6 = this.msg.charAt(i5);
                        if (cCharAt6 == 'n' || cCharAt6 == 'N') {
                            int i7 = i6 + 1;
                            char cCharAt7 = this.msg.charAt(i6);
                            if ((cCharAt7 == 'a' || cCharAt7 == 'A') && ((cCharAt = this.msg.charAt(i7)) == 'l' || cCharAt == 'L')) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean inMessageFormatPattern(int i) {
        return i > 0 || this.parts.get(0).type == Part.Type.MSG_START;
    }

    private boolean inTopLevelChoiceMessage(int i, ArgType argType) {
        return i == 1 && argType == ArgType.CHOICE && this.parts.get(0).type != Part.Type.MSG_START;
    }

    private void addPart(Part.Type type, int i, int i2, int i3) {
        this.parts.add(new Part(type, i, i2, i3));
    }

    private void addLimitPart(int i, Part.Type type, int i2, int i3, int i4) {
        this.parts.get(i).limitPartIndex = this.parts.size();
        addPart(type, i2, i3, i4);
    }

    private void addArgDoublePart(double d, int i, int i2) {
        int size;
        if (this.numericValues == null) {
            this.numericValues = new ArrayList<>();
            size = 0;
        } else {
            size = this.numericValues.size();
            if (size > 32767) {
                throw new IndexOutOfBoundsException("Too many numeric values");
            }
        }
        this.numericValues.add(Double.valueOf(d));
        addPart(Part.Type.ARG_DOUBLE, i, i2, size);
    }

    private static String prefix(String str, int i) {
        StringBuilder sb = new StringBuilder(44);
        if (i == 0) {
            sb.append("\"");
        } else {
            sb.append("[at pattern index ");
            sb.append(i);
            sb.append("] \"");
        }
        if (str.length() - i <= 24) {
            if (i != 0) {
                str = str.substring(i);
            }
            sb.append(str);
        } else {
            int i2 = (i + 24) - 4;
            if (Character.isHighSurrogate(str.charAt(i2 - 1))) {
                i2--;
            }
            sb.append((CharSequence) str, i, i2);
            sb.append(" ...");
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String prefix(String str) {
        return prefix(str, 0);
    }

    private String prefix(int i) {
        return prefix(this.msg, i);
    }

    private String prefix() {
        return prefix(this.msg, 0);
    }
}
