package java.nio;

class HeapIntBuffer extends IntBuffer {
    HeapIntBuffer(int i, int i2) {
        this(i, i2, false);
    }

    HeapIntBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new int[i], 0);
        this.isReadOnly = z;
    }

    HeapIntBuffer(int[] iArr, int i, int i2) {
        this(iArr, i, i2, false);
    }

    HeapIntBuffer(int[] iArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, iArr.length, iArr, 0);
        this.isReadOnly = z;
    }

    protected HeapIntBuffer(int[] iArr, int i, int i2, int i3, int i4, int i5) {
        this(iArr, i, i2, i3, i4, i5, false);
    }

    protected HeapIntBuffer(int[] iArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, iArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public IntBuffer slice() {
        return new HeapIntBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public IntBuffer duplicate() {
        return new HeapIntBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public IntBuffer asReadOnlyBuffer() {
        return new HeapIntBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public int get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public int get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    public IntBuffer get(int[] iArr, int i, int i2) {
        checkBounds(i, i2, iArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) iArr, i, i2);
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
    public IntBuffer put(int i) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = i;
        return this;
    }

    @Override
    public IntBuffer put(int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = i2;
        return this;
    }

    @Override
    public IntBuffer put(int[] iArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, iArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy((Object) iArr, i, (Object) this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public IntBuffer put(IntBuffer intBuffer) {
        if (intBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (intBuffer instanceof HeapIntBuffer) {
            HeapIntBuffer heapIntBuffer = (HeapIntBuffer) intBuffer;
            int iRemaining = heapIntBuffer.remaining();
            if (iRemaining > remaining()) {
                throw new BufferOverflowException();
            }
            System.arraycopy((Object) heapIntBuffer.hb, heapIntBuffer.ix(heapIntBuffer.position()), (Object) this.hb, ix(position()), iRemaining);
            heapIntBuffer.position(heapIntBuffer.position() + iRemaining);
            position(position() + iRemaining);
        } else if (intBuffer.isDirect()) {
            int iRemaining2 = intBuffer.remaining();
            if (iRemaining2 > remaining()) {
                throw new BufferOverflowException();
            }
            intBuffer.get(this.hb, ix(position()), iRemaining2);
            position(position() + iRemaining2);
        } else {
            super.put(intBuffer);
        }
        return this;
    }

    @Override
    public IntBuffer compact() {
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
