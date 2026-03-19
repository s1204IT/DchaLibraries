package java.nio;

import libcore.io.Memory;

class ByteBufferAsDoubleBuffer extends DoubleBuffer {
    static final boolean $assertionsDisabled = false;
    protected final ByteBuffer bb;
    protected final int offset;
    private final ByteOrder order;

    ByteBufferAsDoubleBuffer(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, ByteOrder byteOrder) {
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
    public DoubleBuffer slice() {
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        return new ByteBufferAsDoubleBuffer(this.bb, -1, 0, i, i, (iPosition << 3) + this.offset, this.order);
    }

    @Override
    public DoubleBuffer duplicate() {
        return new ByteBufferAsDoubleBuffer(this.bb, markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    @Override
    public DoubleBuffer asReadOnlyBuffer() {
        return new ByteBufferAsDoubleBuffer(this.bb.asReadOnlyBuffer(), markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    protected int ix(int i) {
        return (i << 3) + this.offset;
    }

    @Override
    public double get() {
        return get(nextGetIndex());
    }

    @Override
    public double get(int i) {
        return this.bb.getDoubleUnchecked(ix(checkIndex(i)));
    }

    @Override
    public DoubleBuffer get(double[] dArr, int i, int i2) {
        checkBounds(i, i2, dArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        this.bb.getUnchecked(ix(this.position), dArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public DoubleBuffer put(double d) {
        put(nextPutIndex(), d);
        return this;
    }

    @Override
    public DoubleBuffer put(int i, double d) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.bb.putDoubleUnchecked(ix(checkIndex(i)), d);
        return this;
    }

    @Override
    public DoubleBuffer put(double[] dArr, int i, int i2) {
        checkBounds(i, i2, dArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        this.bb.putUnchecked(ix(this.position), dArr, i, i2);
        this.position += i2;
        return this;
    }

    @Override
    public DoubleBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (!(this.bb instanceof DirectByteBuffer)) {
            System.arraycopy(this.bb.array(), ix(iPosition), this.bb.array(), ix(0), i << 3);
        } else {
            Memory.memmove(this, ix(0), this, ix(iPosition), i << 3);
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
