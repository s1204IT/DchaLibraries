package java.nio;

final class FloatArrayBuffer extends FloatBuffer {
    private final int arrayOffset;
    private final float[] backingArray;
    private final boolean isReadOnly;

    FloatArrayBuffer(float[] array) {
        this(array.length, array, 0, false);
    }

    private FloatArrayBuffer(int capacity, float[] backingArray, int arrayOffset, boolean isReadOnly) {
        super(capacity, 0L);
        this.backingArray = backingArray;
        this.arrayOffset = arrayOffset;
        this.isReadOnly = isReadOnly;
    }

    private static FloatArrayBuffer copy(FloatArrayBuffer other, int markOfOther, boolean isReadOnly) {
        FloatArrayBuffer buf = new FloatArrayBuffer(other.capacity(), other.backingArray, other.arrayOffset, isReadOnly);
        buf.limit = other.limit;
        buf.position = other.position();
        buf.mark = markOfOther;
        return buf;
    }

    @Override
    public FloatBuffer asReadOnlyBuffer() {
        return copy(this, this.mark, true);
    }

    @Override
    public FloatBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        System.arraycopy(this.backingArray, this.position + this.arrayOffset, this.backingArray, this.arrayOffset, remaining());
        this.position = this.limit - this.position;
        this.limit = this.capacity;
        this.mark = -1;
        return this;
    }

    @Override
    public FloatBuffer duplicate() {
        return copy(this, this.mark, this.isReadOnly);
    }

    @Override
    public FloatBuffer slice() {
        return new FloatArrayBuffer(remaining(), this.backingArray, this.arrayOffset + this.position, this.isReadOnly);
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    float[] protectedArray() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return this.backingArray;
    }

    @Override
    int protectedArrayOffset() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return this.arrayOffset;
    }

    @Override
    boolean protectedHasArray() {
        return !this.isReadOnly;
    }

    @Override
    public final float get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        float[] fArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        return fArr[i + i2];
    }

    @Override
    public final float get(int index) {
        checkIndex(index);
        return this.backingArray[this.arrayOffset + index];
    }

    @Override
    public final FloatBuffer get(float[] dst, int dstOffset, int floatCount) {
        if (floatCount > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(this.backingArray, this.arrayOffset + this.position, dst, dstOffset, floatCount);
        this.position += floatCount;
        return this;
    }

    @Override
    public final boolean isDirect() {
        return false;
    }

    @Override
    public final ByteOrder order() {
        return ByteOrder.nativeOrder();
    }

    @Override
    public FloatBuffer put(float c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        float[] fArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        fArr[i + i2] = c;
        return this;
    }

    @Override
    public FloatBuffer put(int index, float c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkIndex(index);
        this.backingArray[this.arrayOffset + index] = c;
        return this;
    }

    @Override
    public FloatBuffer put(float[] src, int srcOffset, int floatCount) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (floatCount > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(src, srcOffset, this.backingArray, this.arrayOffset + this.position, floatCount);
        this.position += floatCount;
        return this;
    }
}
