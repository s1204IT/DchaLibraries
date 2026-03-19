package org.json;

public class JSONTokener {
    private final String in;
    private int pos;

    public JSONTokener(String str) {
        if (str != null && str.startsWith("\ufeff")) {
            str = str.substring(1);
        }
        this.in = str;
    }

    public Object nextValue() throws JSONException {
        int iNextCleanInternal = nextCleanInternal();
        if (iNextCleanInternal == -1) {
            throw syntaxError("End of input");
        }
        if (iNextCleanInternal == 34 || iNextCleanInternal == 39) {
            return nextString((char) iNextCleanInternal);
        }
        if (iNextCleanInternal == 91) {
            return readArray();
        }
        if (iNextCleanInternal == 123) {
            return readObject();
        }
        this.pos--;
        return readLiteral();
    }

    private int nextCleanInternal() throws JSONException {
        while (this.pos < this.in.length()) {
            String str = this.in;
            int i = this.pos;
            this.pos = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt != '\r' && cCharAt != ' ') {
                if (cCharAt == '#') {
                    skipToEndOfLine();
                } else if (cCharAt == '/') {
                    if (this.pos == this.in.length()) {
                        return cCharAt;
                    }
                    char cCharAt2 = this.in.charAt(this.pos);
                    if (cCharAt2 == '*') {
                        this.pos++;
                        int iIndexOf = this.in.indexOf("*/", this.pos);
                        if (iIndexOf == -1) {
                            throw syntaxError("Unterminated comment");
                        }
                        this.pos = iIndexOf + 2;
                    } else if (cCharAt2 == '/') {
                        this.pos++;
                        skipToEndOfLine();
                    } else {
                        return cCharAt;
                    }
                } else {
                    switch (cCharAt) {
                        case '\t':
                        case '\n':
                            break;
                        default:
                            return cCharAt;
                    }
                }
            }
        }
        return -1;
    }

    private void skipToEndOfLine() {
        while (this.pos < this.in.length()) {
            char cCharAt = this.in.charAt(this.pos);
            if (cCharAt != '\r' && cCharAt != '\n') {
                this.pos++;
            } else {
                this.pos++;
                return;
            }
        }
    }

    public String nextString(char c) throws JSONException {
        int i = this.pos;
        StringBuilder sb = null;
        while (this.pos < this.in.length()) {
            String str = this.in;
            int i2 = this.pos;
            this.pos = i2 + 1;
            char cCharAt = str.charAt(i2);
            if (cCharAt == c) {
                if (sb == null) {
                    return new String(this.in.substring(i, this.pos - 1));
                }
                sb.append((CharSequence) this.in, i, this.pos - 1);
                return sb.toString();
            }
            if (cCharAt == '\\') {
                if (this.pos == this.in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) this.in, i, this.pos - 1);
                sb.append(readEscapeCharacter());
                i = this.pos;
            }
        }
        throw syntaxError("Unterminated string");
    }

    private char readEscapeCharacter() throws JSONException {
        String str = this.in;
        int i = this.pos;
        this.pos = i + 1;
        char cCharAt = str.charAt(i);
        if (cCharAt == 'b') {
            return '\b';
        }
        if (cCharAt == 'f') {
            return '\f';
        }
        if (cCharAt == 'n') {
            return '\n';
        }
        if (cCharAt != 'r') {
            switch (cCharAt) {
                case 't':
                    return '\t';
                case 'u':
                    if (this.pos + 4 > this.in.length()) {
                        throw syntaxError("Unterminated escape sequence");
                    }
                    String strSubstring = this.in.substring(this.pos, this.pos + 4);
                    this.pos += 4;
                    try {
                        return (char) Integer.parseInt(strSubstring, 16);
                    } catch (NumberFormatException e) {
                        throw syntaxError("Invalid escape sequence: " + strSubstring);
                    }
                default:
                    return cCharAt;
            }
        }
        return '\r';
    }

    private Object readLiteral() throws JSONException {
        String strSubstring;
        int i;
        String strNextToInternal = nextToInternal("{}[]/\\:,=;# \t\f");
        if (strNextToInternal.length() == 0) {
            throw syntaxError("Expected literal value");
        }
        if ("null".equalsIgnoreCase(strNextToInternal)) {
            return JSONObject.NULL;
        }
        if ("true".equalsIgnoreCase(strNextToInternal)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(strNextToInternal)) {
            return Boolean.FALSE;
        }
        if (strNextToInternal.indexOf(46) == -1) {
            if (strNextToInternal.startsWith("0x") || strNextToInternal.startsWith("0X")) {
                strSubstring = strNextToInternal.substring(2);
                i = 16;
            } else if (strNextToInternal.startsWith(AndroidHardcodedSystemProperties.JAVA_VERSION) && strNextToInternal.length() > 1) {
                strSubstring = strNextToInternal.substring(1);
                i = 8;
            } else {
                i = 10;
                strSubstring = strNextToInternal;
            }
            try {
                long j = Long.parseLong(strSubstring, i);
                if (j <= 2147483647L && j >= -2147483648L) {
                    return Integer.valueOf((int) j);
                }
                return Long.valueOf(j);
            } catch (NumberFormatException e) {
            }
        }
        try {
            return Double.valueOf(strNextToInternal);
        } catch (NumberFormatException e2) {
            return new String(strNextToInternal);
        }
    }

    private String nextToInternal(String str) {
        int i = this.pos;
        while (this.pos < this.in.length()) {
            char cCharAt = this.in.charAt(this.pos);
            if (cCharAt != '\r' && cCharAt != '\n' && str.indexOf(cCharAt) == -1) {
                this.pos++;
            } else {
                return this.in.substring(i, this.pos);
            }
        }
        return this.in.substring(i);
    }

    private JSONObject readObject() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        int iNextCleanInternal = nextCleanInternal();
        if (iNextCleanInternal == 125) {
            return jSONObject;
        }
        if (iNextCleanInternal != -1) {
            this.pos--;
        }
        while (true) {
            Object objNextValue = nextValue();
            if (!(objNextValue instanceof String)) {
                if (objNextValue == null) {
                    throw syntaxError("Names cannot be null");
                }
                throw syntaxError("Names must be strings, but " + objNextValue + " is of type " + objNextValue.getClass().getName());
            }
            int iNextCleanInternal2 = nextCleanInternal();
            if (iNextCleanInternal2 != 58 && iNextCleanInternal2 != 61) {
                throw syntaxError("Expected ':' after " + objNextValue);
            }
            if (this.pos < this.in.length() && this.in.charAt(this.pos) == '>') {
                this.pos++;
            }
            jSONObject.put((String) objNextValue, nextValue());
            int iNextCleanInternal3 = nextCleanInternal();
            if (iNextCleanInternal3 != 44 && iNextCleanInternal3 != 59) {
                if (iNextCleanInternal3 == 125) {
                    return jSONObject;
                }
                throw syntaxError("Unterminated object");
            }
        }
    }

    private JSONArray readArray() throws JSONException {
        JSONArray jSONArray = new JSONArray();
        boolean z = false;
        while (true) {
            int iNextCleanInternal = nextCleanInternal();
            if (iNextCleanInternal != -1) {
                if (iNextCleanInternal == 44 || iNextCleanInternal == 59) {
                    jSONArray.put((Object) null);
                } else if (iNextCleanInternal != 93) {
                    this.pos--;
                    jSONArray.put(nextValue());
                    int iNextCleanInternal2 = nextCleanInternal();
                    if (iNextCleanInternal2 != 44 && iNextCleanInternal2 != 59) {
                        if (iNextCleanInternal2 == 93) {
                            return jSONArray;
                        }
                        throw syntaxError("Unterminated array");
                    }
                } else {
                    if (z) {
                        jSONArray.put((Object) null);
                    }
                    return jSONArray;
                }
                z = true;
            } else {
                throw syntaxError("Unterminated array");
            }
        }
    }

    public JSONException syntaxError(String str) {
        return new JSONException(str + this);
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
        char next = next();
        if (next != c) {
            throw syntaxError("Expected " + c + " but was " + next);
        }
        return next;
    }

    public char nextClean() throws JSONException {
        int iNextCleanInternal = nextCleanInternal();
        if (iNextCleanInternal == -1) {
            return (char) 0;
        }
        return (char) iNextCleanInternal;
    }

    public String next(int i) throws JSONException {
        if (this.pos + i > this.in.length()) {
            throw syntaxError(i + " is out of bounds");
        }
        String strSubstring = this.in.substring(this.pos, this.pos + i);
        this.pos += i;
        return strSubstring;
    }

    public String nextTo(String str) {
        if (str == null) {
            throw new NullPointerException("excluded == null");
        }
        return nextToInternal(str).trim();
    }

    public String nextTo(char c) {
        return nextToInternal(String.valueOf(c)).trim();
    }

    public void skipPast(String str) {
        int iIndexOf = this.in.indexOf(str, this.pos);
        this.pos = iIndexOf == -1 ? this.in.length() : str.length() + iIndexOf;
    }

    public char skipTo(char c) {
        int iIndexOf = this.in.indexOf(c, this.pos);
        if (iIndexOf != -1) {
            this.pos = iIndexOf;
            return c;
        }
        return (char) 0;
    }

    public void back() {
        int i = this.pos - 1;
        this.pos = i;
        if (i == -1) {
            this.pos = 0;
        }
    }

    public static int dehexchar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A') + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 'a') + 10;
        }
        return -1;
    }
}
