package java.nio;

final class ByteBufferAsLongBuffer extends LongBuffer {
    private final ByteBuffer byteBuffer;

    static LongBuffer asLongBuffer(ByteBuffer byteBuffer) {
        ByteBuffer slice = byteBuffer.slice();
        slice.order(byteBuffer.order());
        return new ByteBufferAsLongBuffer(slice);
    }

    private ByteBufferAsLongBuffer(ByteBuffer byteBuffer) {
        super(byteBuffer.capacity() / 8, byteBuffer.effectiveDirectAddress);
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
    }

    @Override
    public LongBuffer asReadOnlyBuffer() {
        ByteBufferAsLongBuffer buf = new ByteBufferAsLongBuffer(this.byteBuffer.asReadOnlyBuffer());
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        buf.byteBuffer.order = this.byteBuffer.order;
        return buf;
    }

    @Override
    public LongBuffer compact() {
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
    public LongBuffer duplicate() {
        ByteBuffer bb = this.byteBuffer.duplicate().order(this.byteBuffer.order());
        ByteBufferAsLongBuffer buf = new ByteBufferAsLongBuffer(bb);
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        return buf;
    }

    @Override
    public long get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        ByteBuffer byteBuffer = this.byteBuffer;
        int i = this.position;
        this.position = i + 1;
        return byteBuffer.getLong(i * 8);
    }

    @Override
    public long get(int index) {
        checkIndex(index);
        return this.byteBuffer.getLong(index * 8);
    }

    @Override
    public LongBuffer get(long[] dst, int dstOffset, int longCount) {
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        if (this.byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) this.byteBuffer).get(dst, dstOffset, longCount);
        } else {
            ((ByteArrayBuffer) this.byteBuffer).get(dst, dstOffset, longCount);
        }
        this.position += longCount;
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
    long[] protectedArray() {
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
    public LongBuffer put(long c) {
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        ByteBuffer byteBuffer = this.byteBuffer;
        int i = this.position;
        this.position = i + 1;
        byteBuffer.putLong(i * 8, c);
        return this;
    }

    @Override
    public LongBuffer put(int index, long c) {
        checkIndex(index);
        this.byteBuffer.putLong(index * 8, c);
        return this;
    }

    @Override
    public LongBuffer put(long[] src, int srcOffset, int longCount) {
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        if (this.byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) this.byteBuffer).put(src, srcOffset, longCount);
        } else {
            ((ByteArrayBuffer) this.byteBuffer).put(src, srcOffset, longCount);
        }
        this.position += longCount;
        return this;
    }

    @Override
    public LongBuffer slice() {
        this.byteBuffer.limit(this.limit * 8);
        this.byteBuffer.position(this.position * 8);
        ByteBuffer bb = this.byteBuffer.slice().order(this.byteBuffer.order());
        LongBuffer result = new ByteBufferAsLongBuffer(bb);
        this.byteBuffer.clear();
        return result;
    }
}
