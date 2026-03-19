package java.nio;

class HeapFloatBuffer extends FloatBuffer {
    HeapFloatBuffer(int i, int i2) {
        this(i, i2, false);
    }

    HeapFloatBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new float[i], 0);
        this.isReadOnly = z;
    }

    HeapFloatBuffer(float[] fArr, int i, int i2) {
        this(fArr, i, i2, false);
    }

    HeapFloatBuffer(float[] fArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, fArr.length, fArr, 0);
        this.isReadOnly = z;
    }

    protected HeapFloatBuffer(float[] fArr, int i, int i2, int i3, int i4, int i5) {
        this(fArr, i, i2, i3, i4, i5, false);
    }

    protected HeapFloatBuffer(float[] fArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, fArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public FloatBuffer slice() {
        return new HeapFloatBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public FloatBuffer duplicate() {
        return new HeapFloatBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public FloatBuffer asReadOnlyBuffer() {
        return new HeapFloatBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public float get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public float get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    public FloatBuffer get(float[] fArr, int i, int i2) {
        checkBounds(i, i2, fArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy((Object) this.hb, ix(position()), (Object) fArr, i, i2);
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
    public FloatBuffer put(float f) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = f;
        return this;
    }

    @Override
    public FloatBuffer put(int i, float f) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = f;
        return this;
    }

    @Override
    public FloatBuffer put(float[] fArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, fArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy((Object) fArr, i, (Object) this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public FloatBuffer put(FloatBuffer floatBuffer) {
        if (floatBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (floatBuffer instanceof HeapFloatBuffer) {
            HeapFloatBuffer heapFloatBuffer = (HeapFloatBuffer) floatBuffer;
            int iRemaining = heapFloatBuffer.remaining();
            if (iRemaining > remaining()) {
                throw new BufferOverflowException();
            }
            System.arraycopy((Object) heapFloatBuffer.hb, heapFloatBuffer.ix(heapFloatBuffer.position()), (Object) this.hb, ix(position()), iRemaining);
            heapFloatBuffer.position(heapFloatBuffer.position() + iRemaining);
            position(position() + iRemaining);
        } else if (floatBuffer.isDirect()) {
            int iRemaining2 = floatBuffer.remaining();
            if (iRemaining2 > remaining()) {
                throw new BufferOverflowException();
            }
            floatBuffer.get(this.hb, ix(position()), iRemaining2);
            position(position() + iRemaining2);
        } else {
            super.put(floatBuffer);
        }
        return this;
    }

    @Override
    public FloatBuffer compact() {
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
