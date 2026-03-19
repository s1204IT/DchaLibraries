package java.text;

import java.util.zip.ZipConstants;

class PatternEntry {
    static final int RESET = -2;
    static final int UNSET = -1;
    String chars;
    String extension;
    int strength;

    public void appendQuotedExtension(StringBuffer stringBuffer) {
        appendQuoted(this.extension, stringBuffer);
    }

    public void appendQuotedChars(StringBuffer stringBuffer) {
        appendQuoted(this.chars, stringBuffer);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return this.chars.equals(((PatternEntry) obj).chars);
    }

    public int hashCode() {
        return this.chars.hashCode();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        addToBuffer(stringBuffer, true, false, null);
        return stringBuffer.toString();
    }

    final int getStrength() {
        return this.strength;
    }

    final String getExtension() {
        return this.extension;
    }

    final String getChars() {
        return this.chars;
    }

    void addToBuffer(StringBuffer stringBuffer, boolean z, boolean z2, PatternEntry patternEntry) {
        if (z2 && stringBuffer.length() > 0) {
            if (this.strength == 0 || patternEntry != null) {
                stringBuffer.append('\n');
            } else {
                stringBuffer.append(' ');
            }
        }
        if (patternEntry != null) {
            stringBuffer.append('&');
            if (z2) {
                stringBuffer.append(' ');
            }
            patternEntry.appendQuotedChars(stringBuffer);
            appendQuotedExtension(stringBuffer);
            if (z2) {
                stringBuffer.append(' ');
            }
        }
        switch (this.strength) {
            case -2:
                stringBuffer.append('&');
                break;
            case -1:
                stringBuffer.append('?');
                break;
            case 0:
                stringBuffer.append('<');
                break;
            case 1:
                stringBuffer.append(';');
                break;
            case 2:
                stringBuffer.append(',');
                break;
            case 3:
                stringBuffer.append('=');
                break;
        }
        if (z2) {
            stringBuffer.append(' ');
        }
        appendQuoted(this.chars, stringBuffer);
        if (z && this.extension.length() != 0) {
            stringBuffer.append('/');
            appendQuoted(this.extension, stringBuffer);
        }
    }

    static void appendQuoted(String str, StringBuffer stringBuffer) {
        char cCharAt = str.charAt(0);
        boolean z = true;
        if (Character.isSpaceChar(cCharAt) || isSpecialChar(cCharAt)) {
            stringBuffer.append('\'');
        } else {
            switch (cCharAt) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case 16:
                case '@':
                    stringBuffer.append('\'');
                    break;
                case '\'':
                    stringBuffer.append('\'');
                    break;
                default:
                    z = false;
                    break;
            }
        }
        stringBuffer.append(str);
        if (z) {
            stringBuffer.append('\'');
        }
    }

    PatternEntry(int i, StringBuffer stringBuffer, StringBuffer stringBuffer2) {
        this.strength = -1;
        this.chars = "";
        this.extension = "";
        this.strength = i;
        this.chars = stringBuffer.toString();
        this.extension = stringBuffer2.length() > 0 ? stringBuffer2.toString() : "";
    }

    static class Parser {
        private String pattern;
        private StringBuffer newChars = new StringBuffer();
        private StringBuffer newExtension = new StringBuffer();
        private int i = 0;

        public Parser(String str) {
            this.pattern = str;
        }

        public PatternEntry next() throws ParseException {
            this.newChars.setLength(0);
            this.newExtension.setLength(0);
            boolean z = true;
            boolean z2 = false;
            int i = -1;
            while (this.i < this.pattern.length()) {
                char cCharAt = this.pattern.charAt(this.i);
                if (!z2) {
                    switch (cCharAt) {
                        case '\t':
                        case '\n':
                        case '\f':
                        case '\r':
                        case ' ':
                            continue;
                        case ZipConstants.CENATX:
                            if (i == -1) {
                                i = -2;
                            }
                            break;
                        case '\'':
                            String str = this.pattern;
                            int i2 = this.i + 1;
                            this.i = i2;
                            char cCharAt2 = str.charAt(i2);
                            if (this.newChars.length() == 0 || z) {
                                this.newChars.append(cCharAt2);
                            } else {
                                this.newExtension.append(cCharAt2);
                            }
                            z2 = true;
                            continue;
                        case ',':
                            if (i == -1) {
                                i = 2;
                            }
                            break;
                        case '/':
                            z = false;
                            continue;
                        case ';':
                            if (i == -1) {
                                i = 1;
                            }
                            break;
                        case '<':
                            if (i == -1) {
                                i = 0;
                            }
                            break;
                        case '=':
                            if (i == -1) {
                                i = 3;
                            }
                            break;
                        default:
                            if (i == -1) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("missing char (=,;<&) : ");
                                sb.append(this.pattern.substring(this.i, this.i + 10 < this.pattern.length() ? this.i + 10 : this.pattern.length()));
                                throw new ParseException(sb.toString(), this.i);
                            }
                            if (PatternEntry.isSpecialChar(cCharAt) && !z2) {
                                throw new ParseException("Unquoted punctuation character : " + Integer.toString(cCharAt, 16), this.i);
                            }
                            if (!z) {
                                this.newExtension.append(cCharAt);
                                continue;
                            } else {
                                this.newChars.append(cCharAt);
                            }
                            break;
                            break;
                    }
                    if (i != -1) {
                        return null;
                    }
                    if (this.newChars.length() != 0) {
                        return new PatternEntry(i, this.newChars, this.newExtension);
                    }
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("missing chars (=,;<&): ");
                    sb2.append(this.pattern.substring(this.i, this.i + 10 < this.pattern.length() ? this.i + 10 : this.pattern.length()));
                    throw new ParseException(sb2.toString(), this.i);
                }
                if (cCharAt == '\'') {
                    z2 = false;
                } else if (this.newChars.length() == 0) {
                    this.newChars.append(cCharAt);
                } else if (z) {
                    this.newChars.append(cCharAt);
                } else {
                    this.newExtension.append(cCharAt);
                }
                this.i++;
            }
            if (i != -1) {
            }
        }
    }

    static boolean isSpecialChar(char c) {
        return c == ' ' || (c <= '/' && c >= '\"') || ((c <= '?' && c >= ':') || ((c <= '`' && c >= '[') || (c <= '~' && c >= '{')));
    }
}
