package java.nio;

final class DoubleArrayBuffer extends DoubleBuffer {
    private final int arrayOffset;
    private final double[] backingArray;
    private final boolean isReadOnly;

    DoubleArrayBuffer(double[] array) {
        this(array.length, array, 0, false);
    }

    private DoubleArrayBuffer(int capacity, double[] backingArray, int arrayOffset, boolean isReadOnly) {
        super(capacity, 0L);
        this.backingArray = backingArray;
        this.arrayOffset = arrayOffset;
        this.isReadOnly = isReadOnly;
    }

    private static DoubleArrayBuffer copy(DoubleArrayBuffer other, int markOfOther, boolean isReadOnly) {
        DoubleArrayBuffer buf = new DoubleArrayBuffer(other.capacity(), other.backingArray, other.arrayOffset, isReadOnly);
        buf.limit = other.limit;
        buf.position = other.position();
        buf.mark = markOfOther;
        return buf;
    }

    @Override
    public DoubleBuffer asReadOnlyBuffer() {
        return copy(this, this.mark, true);
    }

    @Override
    public DoubleBuffer compact() {
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
    public DoubleBuffer duplicate() {
        return copy(this, this.mark, this.isReadOnly);
    }

    @Override
    public DoubleBuffer slice() {
        return new DoubleArrayBuffer(remaining(), this.backingArray, this.arrayOffset + this.position, this.isReadOnly);
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    double[] protectedArray() {
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
    public final double get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        double[] dArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        return dArr[i + i2];
    }

    @Override
    public final double get(int index) {
        checkIndex(index);
        return this.backingArray[this.arrayOffset + index];
    }

    @Override
    public final DoubleBuffer get(double[] dst, int dstOffset, int doubleCount) {
        if (doubleCount > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(this.backingArray, this.arrayOffset + this.position, dst, dstOffset, doubleCount);
        this.position += doubleCount;
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
    public DoubleBuffer put(double c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        double[] dArr = this.backingArray;
        int i = this.arrayOffset;
        int i2 = this.position;
        this.position = i2 + 1;
        dArr[i + i2] = c;
        return this;
    }

    @Override
    public DoubleBuffer put(int index, double c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkIndex(index);
        this.backingArray[this.arrayOffset + index] = c;
        return this;
    }

    @Override
    public DoubleBuffer put(double[] src, int srcOffset, int doubleCount) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        if (doubleCount > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(src, srcOffset, this.backingArray, this.arrayOffset + this.position, doubleCount);
        this.position += doubleCount;
        return this;
    }
}
