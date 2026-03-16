package java.nio;

final class IntArrayBuffer extends IntBuffer {
    private final int arrayOffset;
    private final int[] backingArray;
    private final boolean isReadOnly;

    IntArrayBuffer(int[] array) {
        this(array.length, array, 0, false);
    }

    private IntArrayBuffer(int capacity, int[] backingArray, int arrayOffset, boolean isReadOnly) {
        super(capacity, 0L);
        this.backingArray = backingArray;
        this.arrayOffset = arrayOffset;
        this.isReadOnly = isReadOnly;
    }

    private static IntArrayBuffer copy(IntArrayBuffer other, int markOfOther, boolean isReadOnly) {
        IntArrayBuffer buf = new IntArrayBuffer(other.capacity(), other.backingArray, other.arrayOffset, isReadOnly);
        buf.limit = other.limit;
        buf.position = other.position();
        buf.mark = markOfOther;
        return buf;
    }

    @Override
    public IntBuffer asReadOnlyBuffer() {
        return copy(this, this.mark, true);
    }

    @Override
    public IntBuffer compact() {
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
    public IntBuffer duplicate() {
        return copy(this, this.mark, this.isReadOnly);
    }

    @Override
    public IntBuffer slice() {
        return new IntArrayBuffer(remaining(), this.backingArray, this.arrayOffset + this.position, this.isReadOnly);
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    int[] protectedArray() {
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
    public final int get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        int[] iArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        return iArr[i + i2];
    }

    @Override
    public final int get(int index) {
        checkIndex(index);
        return this.backingArray[this.arrayOffset + index];
    }

    @Override
    public final IntBuffer get(int[] dst, int dstOffset, int intCount) {
        if (intCount > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(this.backingArray, this.arrayOffset + this.position, dst, dstOffset, intCount);
        this.position += intCount;
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
    public IntBuffer put(int c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        int[] iArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        iArr[i + i2] = c;
        return this;
    }

    @Override
    public IntBuffer put(int index, int c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkIndex(index);
        this.backingArray[this.arrayOffset + index] = c;
        return this;
    }

    @Override
    public IntBuffer put(int[] src, int srcOffset, int intCount) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (intCount > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(src, srcOffset, this.backingArray, this.arrayOffset + this.position, intCount);
        this.position += intCount;
        return this;
    }
}
