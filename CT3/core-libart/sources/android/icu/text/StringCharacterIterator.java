package android.icu.text;

import android.icu.util.ICUCloneNotSupportedException;
import java.text.CharacterIterator;

@Deprecated
public final class StringCharacterIterator implements CharacterIterator {
    private int begin;
    private int end;
    private int pos;
    private String text;

    @Deprecated
    public StringCharacterIterator(String text) {
        this(text, 0);
    }

    @Deprecated
    public StringCharacterIterator(String text, int pos) {
        this(text, 0, text.length(), pos);
    }

    @Deprecated
    public StringCharacterIterator(String text, int begin, int end, int pos) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
        if (begin < 0 || begin > end || end > text.length()) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (pos < begin || pos > end) {
            throw new IllegalArgumentException("Invalid position");
        }
        this.begin = begin;
        this.end = end;
        this.pos = pos;
    }

    @Deprecated
    public void setText(String text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
        this.begin = 0;
        this.end = text.length();
        this.pos = 0;
    }

    @Override
    @Deprecated
    public char first() {
        this.pos = this.begin;
        return current();
    }

    @Override
    @Deprecated
    public char last() {
        if (this.end != this.begin) {
            this.pos = this.end - 1;
        } else {
            this.pos = this.end;
        }
        return current();
    }

    @Override
    @Deprecated
    public char setIndex(int p) {
        if (p < this.begin || p > this.end) {
            throw new IllegalArgumentException("Invalid index");
        }
        this.pos = p;
        return current();
    }

    @Override
    @Deprecated
    public char current() {
        if (this.pos >= this.begin && this.pos < this.end) {
            return this.text.charAt(this.pos);
        }
        return (char) 65535;
    }

    @Override
    @Deprecated
    public char next() {
        if (this.pos < this.end - 1) {
            this.pos++;
            return this.text.charAt(this.pos);
        }
        this.pos = this.end;
        return (char) 65535;
    }

    @Override
    @Deprecated
    public char previous() {
        if (this.pos > this.begin) {
            this.pos--;
            return this.text.charAt(this.pos);
        }
        return (char) 65535;
    }

    @Override
    @Deprecated
    public int getBeginIndex() {
        return this.begin;
    }

    @Override
    @Deprecated
    public int getEndIndex() {
        return this.end;
    }

    @Override
    @Deprecated
    public int getIndex() {
        return this.pos;
    }

    @Deprecated
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof StringCharacterIterator) && hashCode() == obj.hashCode() && this.text.equals(obj.text) && this.pos == obj.pos && this.begin == obj.begin && this.end == obj.end;
    }

    @Deprecated
    public int hashCode() {
        return ((this.text.hashCode() ^ this.pos) ^ this.begin) ^ this.end;
    }

    @Override
    @Deprecated
    public Object clone() {
        try {
            StringCharacterIterator other = (StringCharacterIterator) super.clone();
            return other;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }
}
