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
    public StringCharacterIterator(String str) {
        this(str, 0);
    }

    @Deprecated
    public StringCharacterIterator(String str, int i) {
        this(str, 0, str.length(), i);
    }

    @Deprecated
    public StringCharacterIterator(String str, int i, int i2, int i3) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.text = str;
        if (i < 0 || i > i2 || i2 > str.length()) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (i3 < i || i3 > i2) {
            throw new IllegalArgumentException("Invalid position");
        }
        this.begin = i;
        this.end = i2;
        this.pos = i3;
    }

    @Deprecated
    public void setText(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.text = str;
        this.begin = 0;
        this.end = str.length();
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
    public char setIndex(int i) {
        if (i < this.begin || i > this.end) {
            throw new IllegalArgumentException("Invalid index");
        }
        this.pos = i;
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
        if (!(obj instanceof StringCharacterIterator)) {
            return false;
        }
        StringCharacterIterator stringCharacterIterator = (StringCharacterIterator) obj;
        return hashCode() == stringCharacterIterator.hashCode() && this.text.equals(stringCharacterIterator.text) && this.pos == stringCharacterIterator.pos && this.begin == stringCharacterIterator.begin && this.end == stringCharacterIterator.end;
    }

    @Deprecated
    public int hashCode() {
        return ((this.text.hashCode() ^ this.pos) ^ this.begin) ^ this.end;
    }

    @Override
    @Deprecated
    public Object clone() {
        try {
            return (StringCharacterIterator) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }
}
