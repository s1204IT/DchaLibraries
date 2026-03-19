package android.icu.impl;

import android.icu.text.UCharacterIterator;

public final class UCharArrayIterator extends UCharacterIterator {
    private final int limit;
    private int pos;
    private final int start;
    private final char[] text;

    public UCharArrayIterator(char[] cArr, int i, int i2) {
        if (i < 0 || i2 > cArr.length || i > i2) {
            throw new IllegalArgumentException("start: " + i + " or limit: " + i2 + " out of range [0, " + cArr.length + ")");
        }
        this.text = cArr;
        this.start = i;
        this.limit = i2;
        this.pos = i;
    }

    @Override
    public int current() {
        if (this.pos < this.limit) {
            return this.text[this.pos];
        }
        return -1;
    }

    @Override
    public int getLength() {
        return this.limit - this.start;
    }

    @Override
    public int getIndex() {
        return this.pos - this.start;
    }

    @Override
    public int next() {
        if (this.pos >= this.limit) {
            return -1;
        }
        char[] cArr = this.text;
        int i = this.pos;
        this.pos = i + 1;
        return cArr[i];
    }

    @Override
    public int previous() {
        if (this.pos <= this.start) {
            return -1;
        }
        char[] cArr = this.text;
        int i = this.pos - 1;
        this.pos = i;
        return cArr[i];
    }

    @Override
    public void setIndex(int i) {
        if (i < 0 || i > this.limit - this.start) {
            throw new IndexOutOfBoundsException("index: " + i + " out of range [0, " + (this.limit - this.start) + ")");
        }
        this.pos = this.start + i;
    }

    @Override
    public int getText(char[] cArr, int i) {
        int i2 = this.limit - this.start;
        System.arraycopy(this.text, this.start, cArr, i, i2);
        return i2;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
