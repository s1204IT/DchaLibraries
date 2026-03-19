package android.icu.impl;

import android.icu.text.UCharacterIterator;
import java.text.CharacterIterator;

public class CharacterIteratorWrapper extends UCharacterIterator {
    private CharacterIterator iterator;

    public CharacterIteratorWrapper(CharacterIterator characterIterator) {
        if (characterIterator == null) {
            throw new IllegalArgumentException();
        }
        this.iterator = characterIterator;
    }

    @Override
    public int current() {
        char cCurrent = this.iterator.current();
        if (cCurrent == 65535) {
            return -1;
        }
        return cCurrent;
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
        char cCurrent = this.iterator.current();
        this.iterator.next();
        if (cCurrent == 65535) {
            return -1;
        }
        return cCurrent;
    }

    @Override
    public int previous() {
        char cPrevious = this.iterator.previous();
        if (cPrevious == 65535) {
            return -1;
        }
        return cPrevious;
    }

    @Override
    public void setIndex(int i) {
        try {
            this.iterator.setIndex(i);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void setToLimit() {
        this.iterator.setIndex(this.iterator.getEndIndex());
    }

    @Override
    public int getText(char[] cArr, int i) {
        int endIndex = this.iterator.getEndIndex() - this.iterator.getBeginIndex();
        int index = this.iterator.getIndex();
        if (i < 0 || i + endIndex > cArr.length) {
            throw new IndexOutOfBoundsException(Integer.toString(endIndex));
        }
        char cFirst = this.iterator.first();
        while (cFirst != 65535) {
            cArr[i] = cFirst;
            cFirst = this.iterator.next();
            i++;
        }
        this.iterator.setIndex(index);
        return endIndex;
    }

    @Override
    public Object clone() {
        try {
            CharacterIteratorWrapper characterIteratorWrapper = (CharacterIteratorWrapper) super.clone();
            characterIteratorWrapper.iterator = (CharacterIterator) this.iterator.clone();
            return characterIteratorWrapper;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public int moveIndex(int i) {
        int endIndex = this.iterator.getEndIndex() - this.iterator.getBeginIndex();
        int index = i + this.iterator.getIndex();
        if (index < 0) {
            endIndex = 0;
        } else if (index <= endIndex) {
            endIndex = index;
        }
        return this.iterator.setIndex(endIndex);
    }

    @Override
    public CharacterIterator getCharacterIterator() {
        return (CharacterIterator) this.iterator.clone();
    }
}
