package gov.nist.core;

import java.text.ParseException;
import java.util.Vector;

public class StringTokenizer {
    protected String buffer;
    protected int bufferLen;
    protected int ptr;
    protected int savedPtr;

    protected StringTokenizer() {
    }

    public StringTokenizer(String str) {
        this.buffer = str;
        this.bufferLen = str.length();
        this.ptr = 0;
    }

    public String nextToken() {
        int i = this.ptr;
        while (this.ptr < this.bufferLen) {
            char cCharAt = this.buffer.charAt(this.ptr);
            this.ptr++;
            if (cCharAt == '\n') {
                break;
            }
        }
        return this.buffer.substring(i, this.ptr);
    }

    public boolean hasMoreChars() {
        return this.ptr < this.bufferLen;
    }

    public static boolean isHexDigit(char c) {
        return (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f') || isDigit(c);
    }

    public static boolean isAlpha(char c) {
        return c <= 127 ? (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') : Character.isLowerCase(c) || Character.isUpperCase(c);
    }

    public static boolean isDigit(char c) {
        if (c <= 127) {
            return c <= '9' && c >= '0';
        }
        return Character.isDigit(c);
    }

    public static boolean isAlphaDigit(char c) {
        return c <= 127 ? (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c <= '9' && c >= '0') : Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c);
    }

    public String getLine() {
        int i = this.ptr;
        while (this.ptr < this.bufferLen && this.buffer.charAt(this.ptr) != '\n') {
            this.ptr++;
        }
        if (this.ptr < this.bufferLen && this.buffer.charAt(this.ptr) == '\n') {
            this.ptr++;
        }
        return this.buffer.substring(i, this.ptr);
    }

    public String peekLine() {
        int i = this.ptr;
        String line = getLine();
        this.ptr = i;
        return line;
    }

    public char lookAhead() throws ParseException {
        return lookAhead(0);
    }

    public char lookAhead(int i) throws ParseException {
        try {
            return this.buffer.charAt(this.ptr + i);
        } catch (IndexOutOfBoundsException e) {
            return (char) 0;
        }
    }

    public char getNextChar() throws ParseException {
        if (this.ptr >= this.bufferLen) {
            throw new ParseException(this.buffer + " getNextChar: End of buffer", this.ptr);
        }
        String str = this.buffer;
        int i = this.ptr;
        this.ptr = i + 1;
        return str.charAt(i);
    }

    public void consume() {
        this.ptr = this.savedPtr;
    }

    public void consume(int i) {
        this.ptr += i;
    }

    public Vector<String> getLines() {
        Vector<String> vector = new Vector<>();
        while (hasMoreChars()) {
            vector.addElement(getLine());
        }
        return vector;
    }

    public String getNextToken(char c) throws ParseException {
        int i = this.ptr;
        while (true) {
            char cLookAhead = lookAhead(0);
            if (cLookAhead != c) {
                if (cLookAhead == 0) {
                    throw new ParseException("EOL reached", 0);
                }
                consume(1);
            } else {
                return this.buffer.substring(i, this.ptr);
            }
        }
    }

    public static String getSDPFieldName(String str) {
        if (str == null) {
            return null;
        }
        try {
            return str.substring(0, str.indexOf(Separators.EQUALS));
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
