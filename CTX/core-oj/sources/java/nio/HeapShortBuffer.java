package java.nio;

class HeapShortBuffer extends ShortBuffer {
    HeapShortBuffer(int i, int i2) {
        this(i, i2, false);
    }

    HeapShortBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new short[i], 0);
        this.isReadOnly = z;
    }

    HeapShortBuffer(short[] sArr, int i, int i2) {
        this(sArr, i, i2, false);
    }

    HeapShortBuffer(short[] sArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, sArr.length, sArr, 0);
        this.isReadOnly = z;
    }

    protected HeapShortBuffer(short[] sArr, int i, int i2, int i3, int i4, int i5) {
        this(sArr, i, i2, i3, i4, i5, false);
    }

    protected HeapShortBuffer(short[] sArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, sArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public ShortBuffer slice() {
        return new HeapShortBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public ShortBuffer duplicate() {
        return new HeapShortBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public ShortBuffer asReadOnlyBuffer() {
        return new HeapShortBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public short get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public short get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    public ShortBuffer get(short[] sArr, int i, int i2) {
        checkBounds(i, i2, sArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) sArr, i, i2);
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
    public ShortBuffer put(short s) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = s;
        return this;
    }

    @Override
    public ShortBuffer put(int i, short s) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = s;
        return this;
    }

    @Override
    public ShortBuffer put(short[] sArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, sArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy((Object) sArr, i, (Object) this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public ShortBuffer put(ShortBuffer shortBuffer) {
        if (shortBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (shortBuffer instanceof HeapShortBuffer) {
            HeapShortBuffer heapShortBuffer = (HeapShortBuffer) shortBuffer;
            int iRemaining = heapShortBuffer.remaining();
            if (iRemaining > remaining()) {
                throw new BufferOverflowException();
            }
            System.arraycopy((Object) heapShortBuffer.hb, heapShortBuffer.ix(heapShortBuffer.position()), (Object) this.hb, ix(position()), iRemaining);
            heapShortBuffer.position(heapShortBuffer.position() + iRemaining);
            position(position() + iRemaining);
        } else if (shortBuffer.isDirect()) {
            int iRemaining2 = shortBuffer.remaining();
            if (iRemaining2 > remaining()) {
                throw new BufferOverflowException();
            }
            shortBuffer.get(this.hb, ix(position()), iRemaining2);
            position(position() + iRemaining2);
        } else {
            super.put(shortBuffer);
        }
        return this;
    }

    @Override
    public ShortBuffer compact() {
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
