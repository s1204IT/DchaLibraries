package com.android.internal.telephony;

public class ATResponseParser {
    private String mLine;
    private int mNext = 0;
    private int mTokEnd;
    private int mTokStart;

    public ATResponseParser(String str) {
        this.mLine = str;
    }

    public boolean nextBoolean() {
        nextTok();
        if (this.mTokEnd - this.mTokStart > 1) {
            throw new ATParseEx();
        }
        char cCharAt = this.mLine.charAt(this.mTokStart);
        if (cCharAt == '0') {
            return false;
        }
        if (cCharAt == '1') {
            return true;
        }
        throw new ATParseEx();
    }

    public int nextInt() {
        nextTok();
        int i = 0;
        for (int i2 = this.mTokStart; i2 < this.mTokEnd; i2++) {
            char cCharAt = this.mLine.charAt(i2);
            if (cCharAt < '0' || cCharAt > '9') {
                throw new ATParseEx();
            }
            i = (i * 10) + (cCharAt - '0');
        }
        return i;
    }

    public String nextString() {
        nextTok();
        return this.mLine.substring(this.mTokStart, this.mTokEnd);
    }

    public boolean hasMore() {
        return this.mNext < this.mLine.length();
    }

    private void nextTok() {
        int length = this.mLine.length();
        if (this.mNext == 0) {
            skipPrefix();
        }
        if (this.mNext >= length) {
            throw new ATParseEx();
        }
        try {
            String str = this.mLine;
            int i = this.mNext;
            this.mNext = i + 1;
            char cSkipWhiteSpace = skipWhiteSpace(str.charAt(i));
            if (cSkipWhiteSpace == '\"') {
                if (this.mNext >= length) {
                    throw new ATParseEx();
                }
                String str2 = this.mLine;
                int i2 = this.mNext;
                this.mNext = i2 + 1;
                char cCharAt = str2.charAt(i2);
                this.mTokStart = this.mNext - 1;
                while (cCharAt != '\"' && this.mNext < length) {
                    String str3 = this.mLine;
                    int i3 = this.mNext;
                    this.mNext = i3 + 1;
                    cCharAt = str3.charAt(i3);
                }
                if (cCharAt != '\"') {
                    throw new ATParseEx();
                }
                this.mTokEnd = this.mNext - 1;
                if (this.mNext < length) {
                    String str4 = this.mLine;
                    int i4 = this.mNext;
                    this.mNext = i4 + 1;
                    if (str4.charAt(i4) != ',') {
                        throw new ATParseEx();
                    }
                    return;
                }
                return;
            }
            this.mTokStart = this.mNext - 1;
            this.mTokEnd = this.mTokStart;
            while (cSkipWhiteSpace != ',') {
                if (!Character.isWhitespace(cSkipWhiteSpace)) {
                    this.mTokEnd = this.mNext;
                }
                if (this.mNext != length) {
                    String str5 = this.mLine;
                    int i5 = this.mNext;
                    this.mNext = i5 + 1;
                    cSkipWhiteSpace = str5.charAt(i5);
                } else {
                    return;
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ATParseEx();
        }
    }

    private char skipWhiteSpace(char c) {
        int length = this.mLine.length();
        while (this.mNext < length && Character.isWhitespace(c)) {
            String str = this.mLine;
            int i = this.mNext;
            this.mNext = i + 1;
            c = str.charAt(i);
        }
        if (Character.isWhitespace(c)) {
            throw new ATParseEx();
        }
        return c;
    }

    private void skipPrefix() {
        this.mNext = 0;
        int length = this.mLine.length();
        while (this.mNext < length) {
            String str = this.mLine;
            int i = this.mNext;
            this.mNext = i + 1;
            if (str.charAt(i) == ':') {
                return;
            }
        }
        throw new ATParseEx("missing prefix");
    }
}
