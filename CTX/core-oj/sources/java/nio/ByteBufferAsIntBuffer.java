package java.nio;

import libcore.io.Memory;

class ByteBufferAsIntBuffer extends IntBuffer {
    static final boolean $assertionsDisabled = false;
    protected final ByteBuffer bb;
    protected final int offset;
    private final ByteOrder order;

    ByteBufferAsIntBuffer(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, ByteOrder byteOrder) {
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
    public IntBuffer slice() {
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        return new ByteBufferAsIntBuffer(this.bb, -1, 0, i, i, (iPosition << 2) + this.offset, this.order);
    }

    @Override
    public IntBuffer duplicate() {
        return new ByteBufferAsIntBuffer(this.bb, markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    @Override
    public IntBuffer asReadOnlyBuffer() {
        return new ByteBufferAsIntBuffer(this.bb.asReadOnlyBuffer(), markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    protected int ix(int i) {
        return (i << 2) + this.offset;
    }

    @Override
    public int get() {
        return get(nextGetIndex());
    }

    @Override
    public int get(int i) {
        return this.bb.getIntUnchecked(ix(checkIndex(i)));
    }

    @Override
    public IntBuffer get(int[] iArr, int i, int i2) {
        checkBounds(i, i2, iArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        this.bb.getUnchecked(ix(this.position), iArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public IntBuffer put(int i) {
        put(nextPutIndex(), i);
        return this;
    }

    @Override
    public IntBuffer put(int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.bb.putIntUnchecked(ix(checkIndex(i)), i2);
        return this;
    }

    @Override
    public IntBuffer put(int[] iArr, int i, int i2) {
        checkBounds(i, i2, iArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        this.bb.putUnchecked(ix(this.position), iArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public IntBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (!(this.bb instanceof DirectByteBuffer)) {
            System.arraycopy(this.bb.array(), ix(iPosition), this.bb.array(), ix(0), i << 2);
        } else {
            Memory.memmove(this, ix(0), this, ix(iPosition), i << 2);
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
