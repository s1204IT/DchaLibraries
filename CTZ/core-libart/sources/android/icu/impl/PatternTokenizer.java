package android.icu.impl;

import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;

public class PatternTokenizer {
    private static final int AFTER_QUOTE = -1;
    public static final char BACK_SLASH = '\\';
    public static final int BROKEN_ESCAPE = 4;
    public static final int BROKEN_QUOTE = 3;
    public static final int DONE = 0;
    private static final int HEX = 4;
    public static final int LITERAL = 2;
    private static final int NONE = 0;
    private static final int NORMAL_QUOTE = 2;
    public static final char SINGLE_QUOTE = '\'';
    private static final int SLASH_START = 3;
    private static final int START_QUOTE = 1;
    public static final int SYNTAX = 1;
    public static final int UNKNOWN = 5;
    private int limit;
    private String pattern;
    private int start;
    private static int NO_QUOTE = -1;
    private static int IN_QUOTE = -2;
    private UnicodeSet ignorableCharacters = new UnicodeSet();
    private UnicodeSet syntaxCharacters = new UnicodeSet();
    private UnicodeSet extraQuotingCharacters = new UnicodeSet();
    private UnicodeSet escapeCharacters = new UnicodeSet();
    private boolean usingSlash = false;
    private boolean usingQuote = false;
    private transient UnicodeSet needingQuoteCharacters = null;

    public UnicodeSet getIgnorableCharacters() {
        return (UnicodeSet) this.ignorableCharacters.clone();
    }

    public PatternTokenizer setIgnorableCharacters(UnicodeSet unicodeSet) {
        this.ignorableCharacters = (UnicodeSet) unicodeSet.clone();
        this.needingQuoteCharacters = null;
        return this;
    }

    public UnicodeSet getSyntaxCharacters() {
        return (UnicodeSet) this.syntaxCharacters.clone();
    }

    public UnicodeSet getExtraQuotingCharacters() {
        return (UnicodeSet) this.extraQuotingCharacters.clone();
    }

    public PatternTokenizer setSyntaxCharacters(UnicodeSet unicodeSet) {
        this.syntaxCharacters = (UnicodeSet) unicodeSet.clone();
        this.needingQuoteCharacters = null;
        return this;
    }

    public PatternTokenizer setExtraQuotingCharacters(UnicodeSet unicodeSet) {
        this.extraQuotingCharacters = (UnicodeSet) unicodeSet.clone();
        this.needingQuoteCharacters = null;
        return this;
    }

    public UnicodeSet getEscapeCharacters() {
        return (UnicodeSet) this.escapeCharacters.clone();
    }

    public PatternTokenizer setEscapeCharacters(UnicodeSet unicodeSet) {
        this.escapeCharacters = (UnicodeSet) unicodeSet.clone();
        return this;
    }

    public boolean isUsingQuote() {
        return this.usingQuote;
    }

    public PatternTokenizer setUsingQuote(boolean z) {
        this.usingQuote = z;
        this.needingQuoteCharacters = null;
        return this;
    }

    public boolean isUsingSlash() {
        return this.usingSlash;
    }

    public PatternTokenizer setUsingSlash(boolean z) {
        this.usingSlash = z;
        this.needingQuoteCharacters = null;
        return this;
    }

    public int getLimit() {
        return this.limit;
    }

    public PatternTokenizer setLimit(int i) {
        this.limit = i;
        return this;
    }

    public int getStart() {
        return this.start;
    }

    public PatternTokenizer setStart(int i) {
        this.start = i;
        return this;
    }

    public PatternTokenizer setPattern(CharSequence charSequence) {
        return setPattern(charSequence.toString());
    }

    public PatternTokenizer setPattern(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Inconsistent arguments");
        }
        this.start = 0;
        this.limit = str.length();
        this.pattern = str;
        return this;
    }

    public String quoteLiteral(CharSequence charSequence) {
        return quoteLiteral(charSequence.toString());
    }

    public String quoteLiteral(String str) {
        if (this.needingQuoteCharacters == null) {
            this.needingQuoteCharacters = new UnicodeSet().addAll(this.syntaxCharacters).addAll(this.ignorableCharacters).addAll(this.extraQuotingCharacters);
            if (this.usingSlash) {
                this.needingQuoteCharacters.add(92);
            }
            if (this.usingQuote) {
                this.needingQuoteCharacters.add(39);
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        int i = NO_QUOTE;
        int charCount = 0;
        while (charCount < str.length()) {
            int iCharAt = UTF16.charAt(str, charCount);
            if (this.escapeCharacters.contains(iCharAt)) {
                if (i == IN_QUOTE) {
                    stringBuffer.append(SINGLE_QUOTE);
                    i = NO_QUOTE;
                }
                appendEscaped(stringBuffer, iCharAt);
            } else if (this.needingQuoteCharacters.contains(iCharAt)) {
                if (i == IN_QUOTE) {
                    UTF16.append(stringBuffer, iCharAt);
                    if (this.usingQuote && iCharAt == 39) {
                        stringBuffer.append(SINGLE_QUOTE);
                    }
                } else if (this.usingSlash) {
                    stringBuffer.append(BACK_SLASH);
                    UTF16.append(stringBuffer, iCharAt);
                } else if (this.usingQuote) {
                    if (iCharAt == 39) {
                        stringBuffer.append(SINGLE_QUOTE);
                        stringBuffer.append(SINGLE_QUOTE);
                    } else {
                        stringBuffer.append(SINGLE_QUOTE);
                        UTF16.append(stringBuffer, iCharAt);
                        i = IN_QUOTE;
                    }
                } else {
                    appendEscaped(stringBuffer, iCharAt);
                }
            } else {
                if (i == IN_QUOTE) {
                    stringBuffer.append(SINGLE_QUOTE);
                    i = NO_QUOTE;
                }
                UTF16.append(stringBuffer, iCharAt);
            }
            charCount += UTF16.getCharCount(iCharAt);
        }
        if (i == IN_QUOTE) {
            stringBuffer.append(SINGLE_QUOTE);
        }
        return stringBuffer.toString();
    }

    private void appendEscaped(StringBuffer stringBuffer, int i) {
        if (i <= 65535) {
            stringBuffer.append("\\u");
            stringBuffer.append(Utility.hex(i, 4));
        } else {
            stringBuffer.append("\\U");
            stringBuffer.append(Utility.hex(i, 8));
        }
    }

    public String normalize() {
        int i = this.start;
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer stringBuffer2 = new StringBuffer();
        while (true) {
            stringBuffer2.setLength(0);
            int next = next(stringBuffer2);
            if (next == 0) {
                this.start = i;
                return stringBuffer.toString();
            }
            if (next != 1) {
                stringBuffer.append(quoteLiteral(stringBuffer2));
            } else {
                stringBuffer.append(stringBuffer2);
            }
        }
    }

    public int next(StringBuffer stringBuffer) {
        int i;
        if (this.start >= this.limit) {
            return 0;
        }
        int charCount = this.start;
        int i2 = 5;
        int i3 = 5;
        byte b = 0;
        int i4 = 0;
        int i5 = 0;
        while (charCount < this.limit) {
            int iCharAt = UTF16.charAt(this.pattern, charCount);
            if (b != -1) {
                switch (b) {
                    case 1:
                        if (iCharAt != i2) {
                            UTF16.append(stringBuffer, iCharAt);
                            b = 2;
                        } else {
                            UTF16.append(stringBuffer, iCharAt);
                            b = 0;
                        }
                        break;
                    case 2:
                        if (iCharAt != i2) {
                            UTF16.append(stringBuffer, iCharAt);
                        } else {
                            b = -1;
                        }
                        break;
                    case 3:
                        if (iCharAt == 85) {
                            i5 = 8;
                            b = 4;
                        } else if (iCharAt == 117) {
                            b = 4;
                            i5 = 4;
                        } else if (!this.usingSlash) {
                            stringBuffer.append(BACK_SLASH);
                            b = 0;
                            if (this.ignorableCharacters.contains(iCharAt)) {
                                continue;
                            } else {
                                if (this.syntaxCharacters.contains(iCharAt)) {
                                    if (i3 != 5) {
                                        this.start = charCount;
                                        return i3;
                                    }
                                    UTF16.append(stringBuffer, iCharAt);
                                    this.start = charCount + UTF16.getCharCount(iCharAt);
                                    return 1;
                                }
                                if (iCharAt == 92) {
                                    b = 3;
                                } else if (this.usingQuote && iCharAt == 39) {
                                    i2 = iCharAt;
                                    b = 1;
                                } else {
                                    UTF16.append(stringBuffer, iCharAt);
                                }
                                i3 = 2;
                            }
                        } else {
                            UTF16.append(stringBuffer, iCharAt);
                            b = 0;
                        }
                        i4 = 0;
                        break;
                    case 4:
                        int i6 = (i4 << 4) + iCharAt;
                        switch (iCharAt) {
                            case 48:
                            case 49:
                            case 50:
                            case 51:
                            case 52:
                            case 53:
                            case 54:
                            case 55:
                            case 56:
                            case 57:
                                i = i6 - 48;
                                break;
                            default:
                                switch (iCharAt) {
                                    case 65:
                                    case 66:
                                    case 67:
                                    case 68:
                                    case 69:
                                    case 70:
                                        i = i6 - 55;
                                        break;
                                    default:
                                        switch (iCharAt) {
                                            case 97:
                                            case 98:
                                            case 99:
                                            case 100:
                                            case 101:
                                            case 102:
                                                i = i6 - 87;
                                                break;
                                            default:
                                                this.start = charCount;
                                                return 4;
                                        }
                                        break;
                                }
                                break;
                        }
                        i5--;
                        if (i5 != 0) {
                            i4 = i;
                        } else {
                            UTF16.append(stringBuffer, i);
                            i4 = i;
                            b = 0;
                        }
                        break;
                    default:
                        if (this.ignorableCharacters.contains(iCharAt)) {
                        }
                        break;
                }
            } else {
                if (iCharAt == i2) {
                    UTF16.append(stringBuffer, iCharAt);
                    b = 2;
                }
                b = 0;
                if (this.ignorableCharacters.contains(iCharAt)) {
                }
            }
            charCount += UTF16.getCharCount(iCharAt);
        }
        this.start = this.limit;
        switch (b) {
            case 1:
            case 2:
                return 3;
            case 3:
                if (this.usingSlash) {
                    return 4;
                }
                stringBuffer.append(BACK_SLASH);
                break;
            case 4:
                return 4;
        }
        return i3;
    }
}
