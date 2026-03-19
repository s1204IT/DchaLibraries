package java.nio;

class StringCharBuffer extends CharBuffer {
    CharSequence str;

    StringCharBuffer(CharSequence charSequence, int i, int i2) {
        super(-1, i, i2, charSequence.length());
        int length = charSequence.length();
        if (i < 0 || i > length || i2 < i || i2 > length) {
            throw new IndexOutOfBoundsException();
        }
        this.str = charSequence;
    }

    @Override
    public CharBuffer slice() {
        return new StringCharBuffer(this.str, -1, 0, remaining(), remaining(), this.offset + position());
    }

    private StringCharBuffer(CharSequence charSequence, int i, int i2, int i3, int i4, int i5) {
        super(i, i2, i3, i4, null, i5);
        this.str = charSequence;
    }

    @Override
    public CharBuffer duplicate() {
        return new StringCharBuffer(this.str, markValue(), position(), limit(), capacity(), this.offset);
    }

    @Override
    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }

    @Override
    public final char get() {
        return this.str.charAt(nextGetIndex() + this.offset);
    }

    @Override
    public final char get(int i) {
        return this.str.charAt(checkIndex(i) + this.offset);
    }

    @Override
    char getUnchecked(int i) {
        return this.str.charAt(i + this.offset);
    }

    @Override
    public final CharBuffer put(char c) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public final CharBuffer put(int i, char c) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public final CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }

    @Override
    public final boolean isReadOnly() {
        return true;
    }

    @Override
    final String toString(int i, int i2) {
        return this.str.toString().substring(i + this.offset, i2 + this.offset);
    }

    @Override
    public final CharBuffer subSequence(int i, int i2) {
        try {
            int iPosition = position();
            return new StringCharBuffer(this.str, -1, iPosition + checkIndex(i, iPosition), iPosition + checkIndex(i2, iPosition), capacity(), this.offset);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
}
