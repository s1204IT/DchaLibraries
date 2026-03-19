package java.nio;

class HeapLongBuffer extends LongBuffer {
    HeapLongBuffer(int i, int i2) {
        this(i, i2, false);
    }

    HeapLongBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new long[i], 0);
        this.isReadOnly = z;
    }

    HeapLongBuffer(long[] jArr, int i, int i2) {
        this(jArr, i, i2, false);
    }

    HeapLongBuffer(long[] jArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, jArr.length, jArr, 0);
        this.isReadOnly = z;
    }

    protected HeapLongBuffer(long[] jArr, int i, int i2, int i3, int i4, int i5) {
        this(jArr, i, i2, i3, i4, i5, false);
    }

    protected HeapLongBuffer(long[] jArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, jArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public LongBuffer slice() {
        return new HeapLongBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public LongBuffer duplicate() {
        return new HeapLongBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public LongBuffer asReadOnlyBuffer() {
        return new HeapLongBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public long get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public long get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    public LongBuffer get(long[] jArr, int i, int i2) {
        checkBounds(i, i2, jArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) jArr, i, i2);
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
    public LongBuffer put(long j) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = j;
        return this;
    }

    @Override
    public LongBuffer put(int i, long j) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = j;
        return this;
    }

    @Override
    public LongBuffer put(long[] jArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, jArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy((Object) jArr, i, (Object) this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public LongBuffer put(LongBuffer longBuffer) {
        if (longBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (longBuffer instanceof HeapLongBuffer) {
            HeapLongBuffer heapLongBuffer = (HeapLongBuffer) longBuffer;
            int iRemaining = heapLongBuffer.remaining();
            if (iRemaining > remaining()) {
                throw new BufferOverflowException();
            }
            System.arraycopy((Object) heapLongBuffer.hb, heapLongBuffer.ix(heapLongBuffer.position()), (Object) this.hb, ix(position()), iRemaining);
            heapLongBuffer.position(heapLongBuffer.position() + iRemaining);
            position(position() + iRemaining);
        } else if (longBuffer.isDirect()) {
            int iRemaining2 = longBuffer.remaining();
            if (iRemaining2 > remaining()) {
                throw new BufferOverflowException();
            }
            longBuffer.get(this.hb, ix(position()), iRemaining2);
            position(position() + iRemaining2);
        } else {
            super.put(longBuffer);
        }
        return this;
    }

    @Override
    public LongBuffer compact() {
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
