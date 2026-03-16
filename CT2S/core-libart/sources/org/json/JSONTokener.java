package org.json;

import dalvik.bytecode.Opcodes;

public class JSONTokener {
    private final String in;
    private int pos;

    public JSONTokener(String in) {
        if (in != null && in.startsWith("\ufeff")) {
            in = in.substring(1);
        }
        this.in = in;
    }

    public Object nextValue() throws JSONException {
        int c = nextCleanInternal();
        switch (c) {
            case -1:
                throw syntaxError("End of input");
            case 34:
            case Opcodes.OP_THROW:
                return nextString((char) c);
            case 91:
                return readArray();
            case Opcodes.OP_NEG_INT:
                return readObject();
            default:
                this.pos--;
                return readLiteral();
        }
    }

    private int nextCleanInternal() throws JSONException {
        while (this.pos < this.in.length()) {
            String str = this.in;
            int i = this.pos;
            this.pos = i + 1;
            int c = str.charAt(i);
            switch (c) {
                case 9:
                case 10:
                case 13:
                case 32:
                    break;
                case 35:
                    skipToEndOfLine();
                    break;
                case Opcodes.OP_CMPL_DOUBLE:
                    if (this.pos != this.in.length()) {
                        char peek = this.in.charAt(this.pos);
                        switch (peek) {
                            case '*':
                                this.pos++;
                                int commentEnd = this.in.indexOf("*/", this.pos);
                                if (commentEnd == -1) {
                                    throw syntaxError("Unterminated comment");
                                }
                                this.pos = commentEnd + 2;
                                break;
                                break;
                            case Opcodes.OP_CMPL_DOUBLE:
                                this.pos++;
                                skipToEndOfLine();
                                break;
                            default:
                                return c;
                        }
                    } else {
                        return c;
                    }
                    break;
                default:
                    return c;
            }
        }
        return -1;
    }

    private void skipToEndOfLine() {
        while (this.pos < this.in.length()) {
            char c = this.in.charAt(this.pos);
            if (c != '\r' && c != '\n') {
                this.pos++;
            } else {
                this.pos++;
                return;
            }
        }
    }

    public String nextString(char quote) throws JSONException {
        StringBuilder builder = null;
        int start = this.pos;
        while (this.pos < this.in.length()) {
            String str = this.in;
            int i = this.pos;
            this.pos = i + 1;
            int c = str.charAt(i);
            if (c == quote) {
                if (builder == null) {
                    return new String(this.in.substring(start, this.pos - 1));
                }
                builder.append((CharSequence) this.in, start, this.pos - 1);
                return builder.toString();
            }
            if (c == 92) {
                if (this.pos == this.in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append((CharSequence) this.in, start, this.pos - 1);
                builder.append(readEscapeCharacter());
                start = this.pos;
            }
        }
        throw syntaxError("Unterminated string");
    }

    private char readEscapeCharacter() throws JSONException {
        String str = this.in;
        int i = this.pos;
        this.pos = i + 1;
        char escaped = str.charAt(i);
        switch (escaped) {
            case Opcodes.OP_SGET_OBJECT:
                return '\b';
            case Opcodes.OP_SGET_SHORT:
                return '\f';
            case Opcodes.OP_INVOKE_VIRTUAL:
                return '\n';
            case Opcodes.OP_INVOKE_INTERFACE:
                return '\r';
            case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                return '\t';
            case Opcodes.OP_INVOKE_SUPER_RANGE:
                if (this.pos + 4 > this.in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                String hex = this.in.substring(this.pos, this.pos + 4);
                this.pos += 4;
                return (char) Integer.parseInt(hex, 16);
            default:
                return escaped;
        }
    }

    private Object readLiteral() throws JSONException {
        Object objValueOf;
        String literal = nextToInternal("{}[]/\\:,=;# \t\f");
        if (literal.length() == 0) {
            throw syntaxError("Expected literal value");
        }
        if ("null".equalsIgnoreCase(literal)) {
            return JSONObject.NULL;
        }
        if ("true".equalsIgnoreCase(literal)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(literal)) {
            return Boolean.FALSE;
        }
        if (literal.indexOf(46) == -1) {
            int base = 10;
            String number = literal;
            if (number.startsWith("0x") || number.startsWith("0X")) {
                number = number.substring(2);
                base = 16;
            } else if (number.startsWith("0") && number.length() > 1) {
                number = number.substring(1);
                base = 8;
            }
            try {
                long longValue = Long.parseLong(number, base);
                if (longValue <= 2147483647L && longValue >= -2147483648L) {
                    objValueOf = Integer.valueOf((int) longValue);
                } else {
                    objValueOf = Long.valueOf(longValue);
                }
                return objValueOf;
            } catch (NumberFormatException e) {
            }
        }
        try {
            return Double.valueOf(literal);
        } catch (NumberFormatException e2) {
            return new String(literal);
        }
    }

    private String nextToInternal(String excluded) {
        int start = this.pos;
        while (this.pos < this.in.length()) {
            char c = this.in.charAt(this.pos);
            if (c != '\r' && c != '\n' && excluded.indexOf(c) == -1) {
                this.pos++;
            } else {
                return this.in.substring(start, this.pos);
            }
        }
        return this.in.substring(start);
    }

    private JSONObject readObject() throws JSONException {
        JSONObject result = new JSONObject();
        int first = nextCleanInternal();
        if (first != 125) {
            if (first != -1) {
                this.pos--;
            }
            while (true) {
                Object name = nextValue();
                if (!(name instanceof String)) {
                    if (name == null) {
                        throw syntaxError("Names cannot be null");
                    }
                    throw syntaxError("Names must be strings, but " + name + " is of type " + name.getClass().getName());
                }
                int separator = nextCleanInternal();
                if (separator != 58 && separator != 61) {
                    throw syntaxError("Expected ':' after " + name);
                }
                if (this.pos < this.in.length() && this.in.charAt(this.pos) == '>') {
                    this.pos++;
                }
                result.put((String) name, nextValue());
                switch (nextCleanInternal()) {
                    case 44:
                    case Opcodes.OP_IF_GEZ:
                        break;
                    case Opcodes.OP_NEG_LONG:
                        break;
                    default:
                        throw syntaxError("Unterminated object");
                }
            }
        }
        return result;
    }

    private JSONArray readArray() throws JSONException {
        JSONArray result = new JSONArray();
        boolean hasTrailingSeparator = false;
        while (true) {
            switch (nextCleanInternal()) {
                case -1:
                    throw syntaxError("Unterminated array");
                case 44:
                case Opcodes.OP_IF_GEZ:
                    result.put((Object) null);
                    hasTrailingSeparator = true;
                    continue;
                case 93:
                    if (hasTrailingSeparator) {
                        result.put((Object) null);
                    }
                    break;
                default:
                    this.pos--;
                    result.put(nextValue());
                    switch (nextCleanInternal()) {
                        case 44:
                        case Opcodes.OP_IF_GEZ:
                            hasTrailingSeparator = true;
                            continue;
                        case 93:
                            break;
                        default:
                            throw syntaxError("Unterminated array");
                    }
                    break;
            }
        }
        return result;
    }

    public JSONException syntaxError(String message) {
        return new JSONException(message + this);
    }

    public String toString() {
        return " at character " + this.pos + " of " + this.in;
    }

    public boolean more() {
        return this.pos < this.in.length();
    }

    public char next() {
        if (this.pos >= this.in.length()) {
            return (char) 0;
        }
        String str = this.in;
        int i = this.pos;
        this.pos = i + 1;
        return str.charAt(i);
    }

    public char next(char c) throws JSONException {
        char result = next();
        if (result != c) {
            throw syntaxError("Expected " + c + " but was " + result);
        }
        return result;
    }

    public char nextClean() throws JSONException {
        int nextCleanInt = nextCleanInternal();
        if (nextCleanInt == -1) {
            return (char) 0;
        }
        return (char) nextCleanInt;
    }

    public String next(int length) throws JSONException {
        if (this.pos + length > this.in.length()) {
            throw syntaxError(length + " is out of bounds");
        }
        String result = this.in.substring(this.pos, this.pos + length);
        this.pos += length;
        return result;
    }

    public String nextTo(String excluded) {
        if (excluded == null) {
            throw new NullPointerException("excluded == null");
        }
        return nextToInternal(excluded).trim();
    }

    public String nextTo(char excluded) {
        return nextToInternal(String.valueOf(excluded)).trim();
    }

    public void skipPast(String thru) {
        int thruStart = this.in.indexOf(thru, this.pos);
        this.pos = thruStart == -1 ? this.in.length() : thru.length() + thruStart;
    }

    public char skipTo(char to) {
        int index = this.in.indexOf(to, this.pos);
        if (index == -1) {
            return (char) 0;
        }
        this.pos = index;
        return to;
    }

    public void back() {
        int i = this.pos - 1;
        this.pos = i;
        if (i == -1) {
            this.pos = 0;
        }
    }

    public static int dehexchar(char hex) {
        if (hex >= '0' && hex <= '9') {
            return hex - '0';
        }
        if (hex >= 'A' && hex <= 'F') {
            return (hex - 'A') + 10;
        }
        if (hex >= 'a' && hex <= 'f') {
            return (hex - 'a') + 10;
        }
        return -1;
    }
}
