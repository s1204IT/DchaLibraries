package java.nio;

final class ShortArrayBuffer extends ShortBuffer {
    private final int arrayOffset;
    private final short[] backingArray;
    private final boolean isReadOnly;

    ShortArrayBuffer(short[] array) {
        this(array.length, array, 0, false);
    }

    private ShortArrayBuffer(int capacity, short[] backingArray, int arrayOffset, boolean isReadOnly) {
        super(capacity, 0L);
        this.backingArray = backingArray;
        this.arrayOffset = arrayOffset;
        this.isReadOnly = isReadOnly;
    }

    private static ShortArrayBuffer copy(ShortArrayBuffer other, int markOfOther, boolean isReadOnly) {
        ShortArrayBuffer buf = new ShortArrayBuffer(other.capacity(), other.backingArray, other.arrayOffset, isReadOnly);
        buf.limit = other.limit;
        buf.position = other.position();
        buf.mark = markOfOther;
        return buf;
    }

    @Override
    public ShortBuffer asReadOnlyBuffer() {
        return copy(this, this.mark, true);
    }

    @Override
    public ShortBuffer compact() {
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
    public ShortBuffer duplicate() {
        return copy(this, this.mark, this.isReadOnly);
    }

    @Override
    public ShortBuffer slice() {
        return new ShortArrayBuffer(remaining(), this.backingArray, this.arrayOffset + this.position, this.isReadOnly);
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    short[] protectedArray() {
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
    public final short get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        short[] sArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        return sArr[i + i2];
    }

    @Override
    public final short get(int index) {
        checkIndex(index);
        return this.backingArray[this.arrayOffset + index];
    }

    @Override
    public final ShortBuffer get(short[] dst, int dstOffset, int shortCount) {
        if (shortCount > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(this.backingArray, this.arrayOffset + this.position, dst, dstOffset, shortCount);
        this.position += shortCount;
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
    public ShortBuffer put(short c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        short[] sArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        sArr[i + i2] = c;
        return this;
    }

    @Override
    public ShortBuffer put(int index, short c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkIndex(index);
        this.backingArray[this.arrayOffset + index] = c;
        return this;
    }

    @Override
    public ShortBuffer put(short[] src, int srcOffset, int shortCount) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (shortCount > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(src, srcOffset, this.backingArray, this.arrayOffset + this.position, shortCount);
        this.position += shortCount;
        return this;
    }
}
