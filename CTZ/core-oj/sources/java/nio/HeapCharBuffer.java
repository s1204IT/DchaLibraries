package java.nio;

class HeapCharBuffer extends CharBuffer {
    HeapCharBuffer(int i, int i2) {
        this(i, i2, false);
    }

    HeapCharBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new char[i], 0);
        this.isReadOnly = z;
    }

    HeapCharBuffer(char[] cArr, int i, int i2) {
        this(cArr, i, i2, false);
    }

    HeapCharBuffer(char[] cArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, cArr.length, cArr, 0);
        this.isReadOnly = z;
    }

    protected HeapCharBuffer(char[] cArr, int i, int i2, int i3, int i4, int i5) {
        this(cArr, i, i2, i3, i4, i5, false);
    }

    protected HeapCharBuffer(char[] cArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, cArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public CharBuffer slice() {
        return new HeapCharBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public CharBuffer duplicate() {
        return new HeapCharBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public CharBuffer asReadOnlyBuffer() {
        return new HeapCharBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public char get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public char get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    char getUnchecked(int i) {
        return this.hb[ix(i)];
    }

    @Override
    public CharBuffer get(char[] cArr, int i, int i2) {
        checkBounds(i, i2, cArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) cArr, i, i2);
        position(position() + i2);
        return this;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public CharBuffer put(char c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = c;
        return this;
    }

    @Override
    public CharBuffer put(int i, char c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = c;
        return this;
    }

    @Override
    public CharBuffer put(char[] cArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, cArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy((Object) cArr, i, (Object) this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public CharBuffer put(CharBuffer charBuffer) {
        if (charBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (charBuffer instanceof HeapCharBuffer) {
            HeapCharBuffer heapCharBuffer = (HeapCharBuffer) charBuffer;
            int iRemaining = heapCharBuffer.remaining();
            if (iRemaining > remaining()) {
                throw new BufferOverflowException();
            }
            System.arraycopy((Object) heapCharBuffer.hb, heapCharBuffer.ix(heapCharBuffer.position()), (Object) this.hb, ix(position()), iRemaining);
            heapCharBuffer.position(heapCharBuffer.position() + iRemaining);
            position(position() + iRemaining);
        } else if (charBuffer.isDirect()) {
            int iRemaining2 = charBuffer.remaining();
            if (iRemaining2 > remaining()) {
                throw new BufferOverflowException();
            }
            charBuffer.get(this.hb, ix(position()), iRemaining2);
            position(position() + iRemaining2);
        } else {
            super.put(charBuffer);
        }
        return this;
    }

    @Override
    public CharBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) this.hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();
        return this;
    }

    @Override
    String toString(int i, int i2) {
        try {
            return new String(this.hb, this.offset + i, i2 - i);
        } catch (StringIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public CharBuffer subSequence(int i, int i2) {
        if (i < 0 || i2 > length() || i > i2) {
            throw new IndexOutOfBoundsException();
        }
        int iPosition = position();
        return new HeapCharBuffer(this.hb, -1, iPosition + i, iPosition + i2, capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
}
