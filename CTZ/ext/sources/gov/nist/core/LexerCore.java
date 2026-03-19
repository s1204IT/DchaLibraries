package gov.nist.core;

import java.text.ParseException;
import java.util.Hashtable;

public class LexerCore extends StringTokenizer {
    public static final int ALPHA = 4099;
    static final char ALPHADIGIT_VALID_CHARS = 65533;
    static final char ALPHA_VALID_CHARS = 65535;
    public static final int AND = 38;
    public static final int AT = 64;
    public static final int BACKSLASH = 92;
    public static final int BACK_QUOTE = 96;
    public static final int BAR = 124;
    public static final int COLON = 58;
    public static final int DIGIT = 4098;
    static final char DIGIT_VALID_CHARS = 65534;
    public static final int DOLLAR = 36;
    public static final int DOT = 46;
    public static final int DOUBLEQUOTE = 34;
    public static final int END = 4096;
    public static final int EQUALS = 61;
    public static final int EXCLAMATION = 33;
    public static final int GREATER_THAN = 62;
    public static final int HAT = 94;
    public static final int HT = 9;
    public static final int ID = 4095;
    public static final int LESS_THAN = 60;
    public static final int LPAREN = 40;
    public static final int L_CURLY = 123;
    public static final int L_SQUARE_BRACKET = 91;
    public static final int MINUS = 45;
    public static final int NULL = 0;
    public static final int PERCENT = 37;
    public static final int PLUS = 43;
    public static final int POUND = 35;
    public static final int QUESTION = 63;
    public static final int QUOTE = 39;
    public static final int RPAREN = 41;
    public static final int R_CURLY = 125;
    public static final int R_SQUARE_BRACKET = 93;
    public static final int SAFE = 4094;
    public static final int SEMICOLON = 59;
    public static final int SLASH = 47;
    public static final int SP = 32;
    public static final int STAR = 42;
    public static final int START = 2048;
    public static final int TILDE = 126;
    public static final int UNDERSCORE = 95;
    public static final int WHITESPACE = 4097;
    protected static final Hashtable globalSymbolTable = new Hashtable();
    protected static final Hashtable lexerTables = new Hashtable();
    protected Hashtable currentLexer;
    protected String currentLexerName;
    protected Token currentMatch;

    protected void addKeyword(String str, int i) {
        Integer numValueOf = Integer.valueOf(i);
        this.currentLexer.put(str, numValueOf);
        if (!globalSymbolTable.containsKey(numValueOf)) {
            globalSymbolTable.put(numValueOf, str);
        }
    }

    public String lookupToken(int i) {
        if (i > 2048) {
            return (String) globalSymbolTable.get(Integer.valueOf(i));
        }
        return Character.valueOf((char) i).toString();
    }

    protected Hashtable addLexer(String str) {
        this.currentLexer = (Hashtable) lexerTables.get(str);
        if (this.currentLexer == null) {
            this.currentLexer = new Hashtable();
            lexerTables.put(str, this.currentLexer);
        }
        return this.currentLexer;
    }

    public void selectLexer(String str) {
        this.currentLexerName = str;
    }

    protected LexerCore() {
        this.currentLexer = new Hashtable();
        this.currentLexerName = "charLexer";
    }

    public LexerCore(String str, String str2) {
        super(str2);
        this.currentLexerName = str;
    }

    public String peekNextId() {
        int i = this.ptr;
        String strTtoken = ttoken();
        this.savedPtr = this.ptr;
        this.ptr = i;
        return strTtoken;
    }

    public String getNextId() {
        return ttoken();
    }

    public Token getNextToken() {
        return this.currentMatch;
    }

    public Token peekNextToken() throws ParseException {
        return peekNextToken(1)[0];
    }

    public Token[] peekNextToken(int i) throws ParseException {
        int i2 = this.ptr;
        Token[] tokenArr = new Token[i];
        for (int i3 = 0; i3 < i; i3++) {
            Token token = new Token();
            if (startsId()) {
                String strTtoken = ttoken();
                token.tokenValue = strTtoken;
                String upperCase = strTtoken.toUpperCase();
                if (this.currentLexer.containsKey(upperCase)) {
                    token.tokenType = ((Integer) this.currentLexer.get(upperCase)).intValue();
                } else {
                    token.tokenType = 4095;
                }
            } else {
                char nextChar = getNextChar();
                token.tokenValue = String.valueOf(nextChar);
                if (isAlpha(nextChar)) {
                    token.tokenType = 4099;
                } else if (isDigit(nextChar)) {
                    token.tokenType = 4098;
                } else {
                    token.tokenType = nextChar;
                }
            }
            tokenArr[i3] = token;
        }
        this.savedPtr = this.ptr;
        this.ptr = i2;
        return tokenArr;
    }

    public Token match(int i) throws ParseException {
        if (Debug.parserDebug) {
            Debug.println("match " + i);
        }
        if (i <= 2048 || i >= 4096) {
            if (i > 4096) {
                char cLookAhead = lookAhead(0);
                if (i == 4098) {
                    if (!isDigit(cLookAhead)) {
                        throw new ParseException(this.buffer + "\nExpecting DIGIT", this.ptr);
                    }
                    this.currentMatch = new Token();
                    this.currentMatch.tokenValue = String.valueOf(cLookAhead);
                    this.currentMatch.tokenType = i;
                    consume(1);
                } else if (i == 4099) {
                    if (!isAlpha(cLookAhead)) {
                        throw new ParseException(this.buffer + "\nExpecting ALPHA", this.ptr);
                    }
                    this.currentMatch = new Token();
                    this.currentMatch.tokenValue = String.valueOf(cLookAhead);
                    this.currentMatch.tokenType = i;
                    consume(1);
                }
            } else {
                char c = (char) i;
                char cLookAhead2 = lookAhead(0);
                if (cLookAhead2 == c) {
                    consume(1);
                } else {
                    throw new ParseException(this.buffer + "\nExpecting  >>>" + c + "<<< got >>>" + cLookAhead2 + "<<<", this.ptr);
                }
            }
        } else if (i == 4095) {
            if (!startsId()) {
                throw new ParseException(this.buffer + "\nID expected", this.ptr);
            }
            String nextId = getNextId();
            this.currentMatch = new Token();
            this.currentMatch.tokenValue = nextId;
            this.currentMatch.tokenType = 4095;
        } else if (i == 4094) {
            if (!startsSafeToken()) {
                throw new ParseException(this.buffer + "\nID expected", this.ptr);
            }
            String strTtokenSafe = ttokenSafe();
            this.currentMatch = new Token();
            this.currentMatch.tokenValue = strTtokenSafe;
            this.currentMatch.tokenType = SAFE;
        } else {
            String nextId2 = getNextId();
            Integer num = (Integer) this.currentLexer.get(nextId2.toUpperCase());
            if (num == null || num.intValue() != i) {
                throw new ParseException(this.buffer + "\nUnexpected Token : " + nextId2, this.ptr);
            }
            this.currentMatch = new Token();
            this.currentMatch.tokenValue = nextId2;
            this.currentMatch.tokenType = i;
        }
        return this.currentMatch;
    }

    public void SPorHT() {
        try {
            char cLookAhead = lookAhead(0);
            while (true) {
                if (cLookAhead != ' ' && cLookAhead != '\t') {
                    return;
                }
                consume(1);
                cLookAhead = lookAhead(0);
            }
        } catch (ParseException e) {
        }
    }

    public static final boolean isTokenChar(char c) {
        if (isAlphaDigit(c)) {
            return true;
        }
        switch (c) {
            case '!':
            case '%':
            case '\'':
            case '*':
            case '+':
            case '-':
            case '.':
            case '_':
            case '`':
            case '~':
                break;
        }
        return true;
    }

    public boolean startsId() {
        try {
            return isTokenChar(lookAhead(0));
        } catch (ParseException e) {
            return false;
        }
    }

    public boolean startsSafeToken() {
        try {
            char cLookAhead = lookAhead(0);
            if (isAlphaDigit(cLookAhead)) {
                return true;
            }
            switch (cLookAhead) {
                case '!':
                case '\"':
                case '#':
                case '$':
                case '%':
                case '\'':
                case '*':
                case '+':
                case '-':
                case '.':
                case '/':
                case ':':
                case ';':
                case '=':
                case '?':
                case '@':
                case '[':
                case ']':
                case '^':
                case '_':
                case '`':
                case '{':
                case '|':
                case '}':
                case '~':
                    return true;
                default:
                    return false;
            }
        } catch (ParseException e) {
            return false;
        }
    }

    public String ttoken() {
        int i = this.ptr;
        while (hasMoreChars() && isTokenChar(lookAhead(0))) {
            try {
                consume(1);
            } catch (ParseException e) {
                return null;
            }
        }
        return this.buffer.substring(i, this.ptr);
    }

    public String ttokenSafe() {
        int i = this.ptr;
        while (hasMoreChars()) {
            try {
                boolean z = false;
                char cLookAhead = lookAhead(0);
                if (isAlphaDigit(cLookAhead)) {
                    consume(1);
                } else {
                    switch (cLookAhead) {
                        case '!':
                        case '\"':
                        case '#':
                        case '$':
                        case '%':
                        case '\'':
                        case '*':
                        case '+':
                        case '-':
                        case '.':
                        case '/':
                        case ':':
                        case ';':
                        case '?':
                        case '@':
                        case '[':
                        case ']':
                        case '^':
                        case '_':
                        case '`':
                        case '{':
                        case '|':
                        case '}':
                        case '~':
                            z = true;
                            break;
                    }
                    if (z) {
                        consume(1);
                    } else {
                        return this.buffer.substring(i, this.ptr);
                    }
                }
            } catch (ParseException e) {
                return null;
            }
        }
        return this.buffer.substring(i, this.ptr);
    }

    public void consumeValidChars(char[] cArr) {
        while (hasMoreChars()) {
            try {
                char cLookAhead = lookAhead(0);
                boolean zIsAlphaDigit = false;
                for (char c : cArr) {
                    switch (c) {
                        case 65533:
                            zIsAlphaDigit = isAlphaDigit(cLookAhead);
                            break;
                        case 65534:
                            zIsAlphaDigit = isDigit(cLookAhead);
                            break;
                        case 65535:
                            zIsAlphaDigit = isAlpha(cLookAhead);
                            break;
                        default:
                            zIsAlphaDigit = cLookAhead == c;
                            break;
                    }
                    if (zIsAlphaDigit) {
                        if (!zIsAlphaDigit) {
                            consume(1);
                        } else {
                            return;
                        }
                    }
                }
                if (!zIsAlphaDigit) {
                }
            } catch (ParseException e) {
                return;
            }
        }
    }

    public String quotedString() throws ParseException {
        int i = this.ptr + 1;
        if (lookAhead(0) != '\"') {
            return null;
        }
        consume(1);
        while (true) {
            char nextChar = getNextChar();
            if (nextChar != '\"') {
                if (nextChar == 0) {
                    throw new ParseException(this.buffer + " :unexpected EOL", this.ptr);
                }
                if (nextChar == '\\') {
                    consume(1);
                }
            } else {
                return this.buffer.substring(i, this.ptr - 1);
            }
        }
    }

    public String comment() throws ParseException {
        StringBuffer stringBuffer = new StringBuffer();
        if (lookAhead(0) != '(') {
            return null;
        }
        consume(1);
        while (true) {
            char nextChar = getNextChar();
            if (nextChar != ')') {
                if (nextChar == 0) {
                    throw new ParseException(this.buffer + " :unexpected EOL", this.ptr);
                }
                if (nextChar == '\\') {
                    stringBuffer.append(nextChar);
                    char nextChar2 = getNextChar();
                    if (nextChar2 == 0) {
                        throw new ParseException(this.buffer + " : unexpected EOL", this.ptr);
                    }
                    stringBuffer.append(nextChar2);
                } else {
                    stringBuffer.append(nextChar);
                }
            } else {
                return stringBuffer.toString();
            }
        }
    }

    public String byteStringNoSemicolon() {
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            try {
                char cLookAhead = lookAhead(0);
                if (cLookAhead == 0 || cLookAhead == '\n' || cLookAhead == ';' || cLookAhead == ',') {
                    break;
                }
                consume(1);
                stringBuffer.append(cLookAhead);
            } catch (ParseException e) {
                return stringBuffer.toString();
            }
        }
        return stringBuffer.toString();
    }

    public String byteStringNoSlash() {
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            try {
                char cLookAhead = lookAhead(0);
                if (cLookAhead == 0 || cLookAhead == '\n' || cLookAhead == '/') {
                    break;
                }
                consume(1);
                stringBuffer.append(cLookAhead);
            } catch (ParseException e) {
                return stringBuffer.toString();
            }
        }
        return stringBuffer.toString();
    }

    public String byteStringNoComma() {
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            try {
                char cLookAhead = lookAhead(0);
                if (cLookAhead == '\n' || cLookAhead == ',') {
                    break;
                }
                consume(1);
                stringBuffer.append(cLookAhead);
            } catch (ParseException e) {
            }
        }
        return stringBuffer.toString();
    }

    public static String charAsString(char c) {
        return String.valueOf(c);
    }

    public String charAsString(int i) {
        return this.buffer.substring(this.ptr, this.ptr + i);
    }

    public String number() throws ParseException {
        int i = this.ptr;
        try {
            if (!isDigit(lookAhead(0))) {
                throw new ParseException(this.buffer + ": Unexpected token at " + lookAhead(0), this.ptr);
            }
            consume(1);
            while (isDigit(lookAhead(0))) {
                consume(1);
            }
            return this.buffer.substring(i, this.ptr);
        } catch (ParseException e) {
            return this.buffer.substring(i, this.ptr);
        }
    }

    public int markInputPosition() {
        return this.ptr;
    }

    public void rewindInputPosition(int i) {
        this.ptr = i;
    }

    public String getRest() {
        if (this.ptr >= this.buffer.length()) {
            return null;
        }
        return this.buffer.substring(this.ptr);
    }

    public String getString(char c) throws ParseException {
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            char cLookAhead = lookAhead(0);
            if (cLookAhead == 0) {
                throw new ParseException(this.buffer + "unexpected EOL", this.ptr);
            }
            if (cLookAhead == c) {
                consume(1);
                return stringBuffer.toString();
            }
            if (cLookAhead == '\\') {
                consume(1);
                char cLookAhead2 = lookAhead(0);
                if (cLookAhead2 == 0) {
                    throw new ParseException(this.buffer + "unexpected EOL", this.ptr);
                }
                consume(1);
                stringBuffer.append(cLookAhead2);
            } else {
                consume(1);
                stringBuffer.append(cLookAhead);
            }
        }
    }

    public int getPtr() {
        return this.ptr;
    }

    public String getBuffer() {
        return this.buffer;
    }

    public ParseException createParseException() {
        return new ParseException(this.buffer, this.ptr);
    }
}
