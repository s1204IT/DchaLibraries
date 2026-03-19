package java.nio;

import libcore.io.Memory;

class ByteBufferAsShortBuffer extends ShortBuffer {
    static final boolean $assertionsDisabled = false;
    protected final ByteBuffer bb;
    protected final int offset;
    private final ByteOrder order;

    ByteBufferAsShortBuffer(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, ByteOrder byteOrder) {
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
    public ShortBuffer slice() {
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        return new ByteBufferAsShortBuffer(this.bb, -1, 0, i, i, (iPosition << 1) + this.offset, this.order);
    }

    @Override
    public ShortBuffer duplicate() {
        return new ByteBufferAsShortBuffer(this.bb, markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    @Override
    public ShortBuffer asReadOnlyBuffer() {
        return new ByteBufferAsShortBuffer(this.bb.asReadOnlyBuffer(), markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    protected int ix(int i) {
        return (i << 1) + this.offset;
    }

    @Override
    public short get() {
        return get(nextGetIndex());
    }

    @Override
    public short get(int i) {
        return this.bb.getShortUnchecked(ix(checkIndex(i)));
    }

    @Override
    public ShortBuffer get(short[] sArr, int i, int i2) {
        checkBounds(i, i2, sArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        this.bb.getUnchecked(ix(this.position), sArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public ShortBuffer put(short s) {
        put(nextPutIndex(), s);
        return this;
    }

    @Override
    public ShortBuffer put(int i, short s) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.bb.putShortUnchecked(ix(checkIndex(i)), s);
        return this;
    }

    @Override
    public ShortBuffer put(short[] sArr, int i, int i2) {
        checkBounds(i, i2, sArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        this.bb.putUnchecked(ix(this.position), sArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public ShortBuffer compact() {
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
    public ByteOrder order() {
        return this.order;
    }
}
