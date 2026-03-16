package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;

class ParseState {
    private int pos = 0;
    private String str;

    public ParseState(String str) {
        this.str = str;
    }

    public int length() {
        return this.str.length();
    }

    public boolean hasNext() {
        return this.pos < this.str.length();
    }

    public char ch(int index) {
        if (index < this.str.length()) {
            return this.str.charAt(index);
        }
        return (char) 0;
    }

    public char ch() {
        if (this.pos < this.str.length()) {
            return this.str.charAt(this.pos);
        }
        return (char) 0;
    }

    public void skip() {
        this.pos++;
    }

    public int pos() {
        return this.pos;
    }

    public int gatherInt(String errorMsg, int maxValue) throws XMPException {
        int value = 0;
        boolean success = false;
        char ch = ch(this.pos);
        while ('0' <= ch && ch <= '9') {
            value = (value * 10) + (ch - '0');
            success = true;
            this.pos++;
            ch = ch(this.pos);
        }
        if (success) {
            if (value <= maxValue) {
                if (value < 0) {
                    return 0;
                }
                return value;
            }
            return maxValue;
        }
        throw new XMPException(errorMsg, 5);
    }
}
