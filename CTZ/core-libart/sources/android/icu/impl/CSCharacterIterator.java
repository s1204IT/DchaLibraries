package android.icu.impl;

import java.text.CharacterIterator;

public class CSCharacterIterator implements CharacterIterator {
    private int index;
    private CharSequence seq;

    public CSCharacterIterator(CharSequence charSequence) {
        if (charSequence == null) {
            throw new NullPointerException();
        }
        this.seq = charSequence;
        this.index = 0;
    }

    @Override
    public char first() {
        this.index = 0;
        return current();
    }

    @Override
    public char last() {
        this.index = this.seq.length();
        return previous();
    }

    @Override
    public char current() {
        if (this.index == this.seq.length()) {
            return (char) 65535;
        }
        return this.seq.charAt(this.index);
    }

    @Override
    public char next() {
        if (this.index < this.seq.length()) {
            this.index++;
        }
        return current();
    }

    @Override
    public char previous() {
        if (this.index == 0) {
            return (char) 65535;
        }
        this.index--;
        return current();
    }

    @Override
    public char setIndex(int i) {
        if (i < 0 || i > this.seq.length()) {
            throw new IllegalArgumentException();
        }
        this.index = i;
        return current();
    }

    @Override
    public int getBeginIndex() {
        return 0;
    }

    @Override
    public int getEndIndex() {
        return this.seq.length();
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public Object clone() {
        CSCharacterIterator cSCharacterIterator = new CSCharacterIterator(this.seq);
        cSCharacterIterator.setIndex(this.index);
        return cSCharacterIterator;
    }
}
