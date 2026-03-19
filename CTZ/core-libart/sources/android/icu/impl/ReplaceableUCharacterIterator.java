package android.icu.impl;

import android.icu.text.Replaceable;
import android.icu.text.ReplaceableString;
import android.icu.text.UCharacterIterator;
import android.icu.text.UTF16;

public class ReplaceableUCharacterIterator extends UCharacterIterator {
    private int currentIndex;
    private Replaceable replaceable;

    public ReplaceableUCharacterIterator(Replaceable replaceable) {
        if (replaceable == null) {
            throw new IllegalArgumentException();
        }
        this.replaceable = replaceable;
        this.currentIndex = 0;
    }

    public ReplaceableUCharacterIterator(String str) {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        this.replaceable = new ReplaceableString(str);
        this.currentIndex = 0;
    }

    public ReplaceableUCharacterIterator(StringBuffer stringBuffer) {
        if (stringBuffer == null) {
            throw new IllegalArgumentException();
        }
        this.replaceable = new ReplaceableString(stringBuffer);
        this.currentIndex = 0;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public int current() {
        if (this.currentIndex < this.replaceable.length()) {
            return this.replaceable.charAt(this.currentIndex);
        }
        return -1;
    }

    @Override
    public int currentCodePoint() {
        int iCurrent = current();
        char c = (char) iCurrent;
        if (UTF16.isLeadSurrogate(c)) {
            next();
            int iCurrent2 = current();
            previous();
            char c2 = (char) iCurrent2;
            if (UTF16.isTrailSurrogate(c2)) {
                return Character.toCodePoint(c, c2);
            }
        }
        return iCurrent;
    }

    @Override
    public int getLength() {
        return this.replaceable.length();
    }

    @Override
    public int getIndex() {
        return this.currentIndex;
    }

    @Override
    public int next() {
        if (this.currentIndex < this.replaceable.length()) {
            Replaceable replaceable = this.replaceable;
            int i = this.currentIndex;
            this.currentIndex = i + 1;
            return replaceable.charAt(i);
        }
        return -1;
    }

    @Override
    public int previous() {
        if (this.currentIndex > 0) {
            Replaceable replaceable = this.replaceable;
            int i = this.currentIndex - 1;
            this.currentIndex = i;
            return replaceable.charAt(i);
        }
        return -1;
    }

    @Override
    public void setIndex(int i) throws IndexOutOfBoundsException {
        if (i < 0 || i > this.replaceable.length()) {
            throw new IndexOutOfBoundsException();
        }
        this.currentIndex = i;
    }

    @Override
    public int getText(char[] cArr, int i) {
        int length = this.replaceable.length();
        if (i < 0 || i + length > cArr.length) {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        }
        this.replaceable.getChars(0, length, cArr, i);
        return length;
    }
}
