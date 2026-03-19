package java.nio;

class HeapDoubleBuffer extends DoubleBuffer {
    HeapDoubleBuffer(int i, int i2) {
        this(i, i2, false);
    }

    HeapDoubleBuffer(double[] dArr, int i, int i2) {
        this(dArr, i, i2, false);
    }

    protected HeapDoubleBuffer(double[] dArr, int i, int i2, int i3, int i4, int i5) {
        this(dArr, i, i2, i3, i4, i5, false);
    }

    HeapDoubleBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new double[i], 0);
        this.isReadOnly = z;
    }

    HeapDoubleBuffer(double[] dArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, dArr.length, dArr, 0);
        this.isReadOnly = z;
    }

    protected HeapDoubleBuffer(double[] dArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, dArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public DoubleBuffer slice() {
        return new HeapDoubleBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public DoubleBuffer duplicate() {
        return new HeapDoubleBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public DoubleBuffer asReadOnlyBuffer() {
        return new HeapDoubleBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public double get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public double get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    public DoubleBuffer get(double[] dArr, int i, int i2) {
        checkBounds(i, i2, dArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) dArr, i, i2);
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
    public DoubleBuffer put(double d) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = d;
        return this;
    }

    @Override
    public DoubleBuffer put(int i, double d) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = d;
        return this;
    }

    @Override
    public DoubleBuffer put(double[] dArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, dArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy((Object) dArr, i, (Object) this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public DoubleBuffer put(DoubleBuffer doubleBuffer) {
        if (doubleBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (doubleBuffer instanceof HeapDoubleBuffer) {
            HeapDoubleBuffer heapDoubleBuffer = (HeapDoubleBuffer) doubleBuffer;
            int iRemaining = heapDoubleBuffer.remaining();
            if (iRemaining > remaining()) {
                throw new BufferOverflowException();
            }
            System.arraycopy((Object) heapDoubleBuffer.hb, heapDoubleBuffer.ix(heapDoubleBuffer.position()), (Object) this.hb, ix(position()), iRemaining);
            heapDoubleBuffer.position(heapDoubleBuffer.position() + iRemaining);
            position(position() + iRemaining);
        } else if (doubleBuffer.isDirect()) {
            int iRemaining2 = doubleBuffer.remaining();
            if (iRemaining2 > remaining()) {
                throw new BufferOverflowException();
            }
            doubleBuffer.get(this.hb, ix(position()), iRemaining2);
            position(position() + iRemaining2);
        } else {
            super.put(doubleBuffer);
        }
        return this;
    }

    @Override
    public DoubleBuffer compact() {
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
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
}
