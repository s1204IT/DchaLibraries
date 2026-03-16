package java.text;

public final class StringCharacterIterator implements CharacterIterator {
    int end;
    int offset;
    int start;
    String string;

    public StringCharacterIterator(String value) {
        this.string = value;
        this.offset = 0;
        this.start = 0;
        this.end = this.string.length();
    }

    public StringCharacterIterator(String value, int location) {
        this.string = value;
        this.start = 0;
        this.end = this.string.length();
        if (location < 0 || location > this.end) {
            throw new IllegalArgumentException();
        }
        this.offset = location;
    }

    public StringCharacterIterator(String value, int start, int end, int location) {
        this.string = value;
        if (start < 0 || end > this.string.length() || start > end || location < start || location > end) {
            throw new IllegalArgumentException();
        }
        this.start = start;
        this.end = end;
        this.offset = location;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public char current() {
        if (this.offset == this.end) {
            return (char) 65535;
        }
        return this.string.charAt(this.offset);
    }

    public boolean equals(Object object) {
        if (!(object instanceof StringCharacterIterator)) {
            return false;
        }
        StringCharacterIterator it = (StringCharacterIterator) object;
        return this.string.equals(it.string) && this.start == it.start && this.end == it.end && this.offset == it.offset;
    }

    @Override
    public char first() {
        if (this.start == this.end) {
            return (char) 65535;
        }
        this.offset = this.start;
        return this.string.charAt(this.offset);
    }

    @Override
    public int getBeginIndex() {
        return this.start;
    }

    @Override
    public int getEndIndex() {
        return this.end;
    }

    @Override
    public int getIndex() {
        return this.offset;
    }

    public int hashCode() {
        return this.string.hashCode() + this.start + this.end + this.offset;
    }

    @Override
    public char last() {
        if (this.start == this.end) {
            return (char) 65535;
        }
        this.offset = this.end - 1;
        return this.string.charAt(this.offset);
    }

    @Override
    public char next() {
        if (this.offset >= this.end - 1) {
            this.offset = this.end;
            return (char) 65535;
        }
        String str = this.string;
        int i = this.offset + 1;
        this.offset = i;
        return str.charAt(i);
    }

    @Override
    public char previous() {
        if (this.offset == this.start) {
            return (char) 65535;
        }
        String str = this.string;
        int i = this.offset - 1;
        this.offset = i;
        return str.charAt(i);
    }

    @Override
    public char setIndex(int location) {
        if (location < this.start || location > this.end) {
            throw new IllegalArgumentException();
        }
        this.offset = location;
        if (this.offset == this.end) {
            return (char) 65535;
        }
        return this.string.charAt(this.offset);
    }

    public void setText(String value) {
        this.string = value;
        this.offset = 0;
        this.start = 0;
        this.end = value.length();
    }
}
