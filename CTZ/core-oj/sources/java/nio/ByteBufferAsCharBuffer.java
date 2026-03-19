package java.nio;

import libcore.io.Memory;

class ByteBufferAsCharBuffer extends CharBuffer {
    static final boolean $assertionsDisabled = false;
    protected final ByteBuffer bb;
    protected final int offset;
    private final ByteOrder order;

    ByteBufferAsCharBuffer(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, ByteOrder byteOrder) {
        super(i, i2, i3, i4);
        this.bb = byteBuffer.duplicate();
        this.isReadOnly = byteBuffer.isReadOnly;
        if (byteBuffer instanceof DirectByteBuffer) {
            this.address = byteBuffer.address + ((long) i5);
        }
        this.bb.order(byteOrder);
        this.order = byteOrder;
        this.offset = i5;
    }

    @Override
    public CharBuffer slice() {
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        return new ByteBufferAsCharBuffer(this.bb, -1, 0, i, i, (iPosition << 1) + this.offset, this.order);
    }

    @Override
    public CharBuffer duplicate() {
        return new ByteBufferAsCharBuffer(this.bb, markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    @Override
    public CharBuffer asReadOnlyBuffer() {
        return new ByteBufferAsCharBuffer(this.bb.asReadOnlyBuffer(), markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    protected int ix(int i) {
        return (i << 1) + this.offset;
    }

    @Override
    public char get() {
        return get(nextGetIndex());
    }

    @Override
    public char get(int i) {
        return this.bb.getCharUnchecked(ix(checkIndex(i)));
    }

    @Override
    public CharBuffer get(char[] cArr, int i, int i2) {
        checkBounds(i, i2, cArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        this.bb.getUnchecked(ix(this.position), cArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    char getUnchecked(int i) {
        return this.bb.getCharUnchecked(ix(i));
    }

    @Override
    public CharBuffer put(char c) {
        put(nextPutIndex(), c);
        return this;
    }

    @Override
    public CharBuffer put(int i, char c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.bb.putCharUnchecked(ix(checkIndex(i)), c);
        return this;
    }

    @Override
    public CharBuffer put(char[] cArr, int i, int i2) {
        checkBounds(i, i2, cArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        this.bb.putUnchecked(ix(this.position), cArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public CharBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (!(this.bb instanceof DirectByteBuffer)) {
            System.arraycopy(this.bb.array(), ix(iPosition), this.bb.array(), ix(0), i << 1);
        } else {
            Memory.memmove(this, ix(0), this, ix(iPosition), i << 1);
        }
        position(i);
        limit(capacity());
        discardMark();
        return this;
    }

    @Override
    public boolean isDirect() {
        return this.bb.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public String toString(int i, int i2) {
        if (i2 > limit() || i > i2) {
            throw new IndexOutOfBoundsException();
        }
        try {
            char[] cArr = new char[i2 - i];
            CharBuffer charBufferWrap = CharBuffer.wrap(cArr);
            CharBuffer charBufferDuplicate = duplicate();
            charBufferDuplicate.position(i);
            charBufferDuplicate.limit(i2);
            charBufferWrap.put(charBufferDuplicate);
            return new String(cArr);
        } catch (StringIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public CharBuffer subSequence(int i, int i2) {
        int iPosition = position();
        int iLimit = limit();
        if (iPosition > iLimit) {
            iPosition = iLimit;
        }
        int i3 = iLimit - iPosition;
        if (i < 0 || i2 > i3 || i > i2) {
            throw new IndexOutOfBoundsException();
        }
        return new ByteBufferAsCharBuffer(this.bb, -1, iPosition + i, iPosition + i2, capacity(), this.offset, this.order);
    }

    @Override
    public ByteOrder order() {
        return this.order;
    }
}
