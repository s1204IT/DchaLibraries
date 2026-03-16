package java.nio;

final class ByteBufferAsDoubleBuffer extends DoubleBuffer {
    private final ByteBuffer byteBuffer;

    static DoubleBuffer asDoubleBuffer(ByteBuffer byteBuffer) {
        ByteBuffer slice = byteBuffer.slice();
        slice.order(byteBuffer.order());
        return new ByteBufferAsDoubleBuffer(slice);
    }

    private ByteBufferAsDoubleBuffer(ByteBuffer byteBuffer) {
        super(byteBuffer.capacity() / 8, byteBuffer.effectiveDirectAddress);
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
    }

    @Override
    public DoubleBuffer asReadOnlyBuffer() {
        ByteBufferAsDoubleBuffer buf = new ByteBufferAsDoubleBuffer(this.byteBuffer.asReadOnlyBuffer());
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        buf.byteBuffer.order = this.byteBuffer.order;
        return buf;
    }

    @Override
    public DoubleBuffer compact() {
        if (this.byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        this.byteBuffer.compact();
        this.byteBuffer.clear();
        this.position = this.limit - this.position;
        this.limit = this.capacity;
        this.mark = -1;
        return this;
    }

    @Override
    public DoubleBuffer duplicate() {
        ByteBuffer bb = this.byteBuffer.duplicate().order(this.byteBuffer.order());
        ByteBufferAsDoubleBuffer buf = new ByteBufferAsDoubleBuffer(bb);
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        return buf;
    }

    @Override
    public double get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        ByteBuffer byteBuffer = this.byteBuffer;
        int i = this.position;
        this.position = i + 1;
        return byteBuffer.getDouble(i * 8);
    }

    @Override
    public double get(int index) {
        checkIndex(index);
        return this.byteBuffer.getDouble(index * 8);
    }

    @Override
    public DoubleBuffer get(double[] dst, int dstOffset, int doubleCount) {
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        if (this.byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) this.byteBuffer).get(dst, dstOffset, doubleCount);
        } else {
            ((ByteArrayBuffer) this.byteBuffer).get(dst, dstOffset, doubleCount);
        }
        this.position += doubleCount;
        return this;
    }

    @Override
    public boolean isDirect() {
        return this.byteBuffer.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.byteBuffer.isReadOnly();
    }

    @Override
    public ByteOrder order() {
        return this.byteBuffer.order();
    }

    @Override
    double[] protectedArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    int protectedArrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    boolean protectedHasArray() {
        return false;
    }

    @Override
    public DoubleBuffer put(double c) {
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        ByteBuffer byteBuffer = this.byteBuffer;
        int i = this.position;
        this.position = i + 1;
        byteBuffer.putDouble(i * 8, c);
        return this;
    }

    @Override
    public DoubleBuffer put(int index, double c) {
        checkIndex(index);
        this.byteBuffer.putDouble(index * 8, c);
        return this;
    }

    @Override
    public DoubleBuffer put(double[] src, int srcOffset, int doubleCount) {
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        if (this.byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) this.byteBuffer).put(src, srcOffset, doubleCount);
        } else {
            ((ByteArrayBuffer) this.byteBuffer).put(src, srcOffset, doubleCount);
        }
        this.position += doubleCount;
        return this;
    }

    @Override
    public DoubleBuffer slice() {
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        ByteBuffer bb = this.byteBuffer.slice().order(this.byteBuffer.order());
        DoubleBuffer result = new ByteBufferAsDoubleBuffer(bb);
        this.byteBuffer.clear();
        return result;
    }
}
