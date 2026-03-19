package java.text;

public final class StringCharacterIterator implements CharacterIterator {
    private int begin;
    private int end;
    private int pos;
    private String text;

    public StringCharacterIterator(String str) {
        this(str, 0);
    }

    public StringCharacterIterator(String str, int i) {
        this(str, 0, str.length(), i);
    }

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
    public char first() {
        this.pos = this.begin;
        return current();
    }

    @Override
    public char last() {
        if (this.end != this.begin) {
            this.pos = this.end - 1;
        } else {
            this.pos = this.end;
        }
        return current();
    }

    @Override
    public char setIndex(int i) {
        if (i < this.begin || i > this.end) {
            throw new IllegalArgumentException("Invalid index");
        }
        this.pos = i;
        return current();
    }

    @Override
    public char current() {
        if (this.pos >= this.begin && this.pos < this.end) {
            return this.text.charAt(this.pos);
        }
        return (char) 65535;
    }

    @Override
    public char next() {
        if (this.pos < this.end - 1) {
            this.pos++;
            return this.text.charAt(this.pos);
        }
        this.pos = this.end;
        return (char) 65535;
    }

    @Override
    public char previous() {
        if (this.pos > this.begin) {
            this.pos--;
            return this.text.charAt(this.pos);
        }
        return (char) 65535;
    }

    @Override
    public int getBeginIndex() {
        return this.begin;
    }

    @Override
    public int getEndIndex() {
        return this.end;
    }

    @Override
    public int getIndex() {
        return this.pos;
    }

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

    public int hashCode() {
        return ((this.text.hashCode() ^ this.pos) ^ this.begin) ^ this.end;
    }

    @Override
    public Object clone() {
        try {
            return (StringCharacterIterator) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
