package android.icu.impl;

import android.icu.text.UCharacterIterator;
import java.text.CharacterIterator;

public class CharacterIteratorWrapper extends UCharacterIterator {
    private CharacterIterator iterator;

    public CharacterIteratorWrapper(CharacterIterator iter) {
        if (iter == null) {
            throw new IllegalArgumentException();
        }
        this.iterator = iter;
    }

    @Override
    public int current() {
        int c = this.iterator.current();
        if (c == 65535) {
            return -1;
        }
        return c;
    }

    @Override
    public int getLength() {
        return this.iterator.getEndIndex() - this.iterator.getBeginIndex();
    }

    @Override
    public int getIndex() {
        return this.iterator.getIndex();
    }

    @Override
    public int next() {
        int i = this.iterator.current();
        this.iterator.next();
        if (i == 65535) {
            return -1;
        }
        return i;
    }

    @Override
    public int previous() {
        int i = this.iterator.previous();
        if (i == 65535) {
            return -1;
        }
        return i;
    }

    @Override
    public void setIndex(int index) {
        try {
            this.iterator.setIndex(index);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void setToLimit() {
        this.iterator.setIndex(this.iterator.getEndIndex());
    }

    @Override
    public int getText(char[] fillIn, int offset) {
        int length = this.iterator.getEndIndex() - this.iterator.getBeginIndex();
        int currentIndex = this.iterator.getIndex();
        if (offset < 0 || offset + length > fillIn.length) {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        }
        char ch = this.iterator.first();
        while (true) {
            int offset2 = offset;
            if (ch != 65535) {
                offset = offset2 + 1;
                fillIn[offset2] = ch;
                ch = this.iterator.next();
            } else {
                this.iterator.setIndex(currentIndex);
                return length;
            }
        }
    }

    @Override
    public Object clone() {
        try {
            CharacterIteratorWrapper result = (CharacterIteratorWrapper) super.clone();
            result.iterator = (CharacterIterator) this.iterator.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public int moveIndex(int delta) {
        int length = this.iterator.getEndIndex() - this.iterator.getBeginIndex();
        int idx = this.iterator.getIndex() + delta;
        if (idx < 0) {
            idx = 0;
        } else if (idx > length) {
            idx = length;
        }
        return this.iterator.setIndex(idx);
    }

    @Override
    public CharacterIterator getCharacterIterator() {
        return (CharacterIterator) this.iterator.clone();
    }
}
