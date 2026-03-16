package java.nio;

final class LongArrayBuffer extends LongBuffer {
    private final int arrayOffset;
    private final long[] backingArray;
    private final boolean isReadOnly;

    LongArrayBuffer(long[] array) {
        this(array.length, array, 0, false);
    }

    private LongArrayBuffer(int capacity, long[] backingArray, int arrayOffset, boolean isReadOnly) {
        super(capacity, 0L);
        this.backingArray = backingArray;
        this.arrayOffset = arrayOffset;
        this.isReadOnly = isReadOnly;
    }

    private static LongArrayBuffer copy(LongArrayBuffer other, int markOfOther, boolean isReadOnly) {
        LongArrayBuffer buf = new LongArrayBuffer(other.capacity(), other.backingArray, other.arrayOffset, isReadOnly);
        buf.limit = other.limit;
        buf.position = other.position();
        buf.mark = markOfOther;
        return buf;
    }

    @Override
    public LongBuffer asReadOnlyBuffer() {
        return copy(this, this.mark, true);
    }

    @Override
    public LongBuffer compact() {
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
    public LongBuffer duplicate() {
        return copy(this, this.mark, this.isReadOnly);
    }

    @Override
    public LongBuffer slice() {
        return new LongArrayBuffer(remaining(), this.backingArray, this.arrayOffset + this.position, this.isReadOnly);
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    long[] protectedArray() {
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
    public final long get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        long[] jArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        return jArr[i + i2];
    }

    @Override
    public final long get(int index) {
        checkIndex(index);
        return this.backingArray[this.arrayOffset + index];
    }

    @Override
    public final LongBuffer get(long[] dst, int dstOffset, int longCount) {
        if (longCount > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(this.backingArray, this.arrayOffset + this.position, dst, dstOffset, longCount);
        this.position += longCount;
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
    public LongBuffer put(long c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        long[] jArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        jArr[i + i2] = c;
        return this;
    }

    @Override
    public LongBuffer put(int index, long c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkIndex(index);
        this.backingArray[this.arrayOffset + index] = c;
        return this;
    }

    @Override
    public LongBuffer put(long[] src, int srcOffset, int longCount) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (longCount > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(src, srcOffset, this.backingArray, this.arrayOffset + this.position, longCount);
        this.position += longCount;
        return this;
    }
}
