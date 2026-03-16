package java.nio;

final class ByteBufferAsShortBuffer extends ShortBuffer {
    private final ByteBuffer byteBuffer;

    static ShortBuffer asShortBuffer(ByteBuffer byteBuffer) {
        ByteBuffer slice = byteBuffer.slice();
        slice.order(byteBuffer.order());
        return new ByteBufferAsShortBuffer(slice);
    }

    private ByteBufferAsShortBuffer(ByteBuffer byteBuffer) {
        super(byteBuffer.capacity() / 2, byteBuffer.effectiveDirectAddress);
        this.byteBuffer = byteBuffer;
        this.byteBuffer.clear();
    }

    @Override
    public ShortBuffer asReadOnlyBuffer() {
        ByteBufferAsShortBuffer buf = new ByteBufferAsShortBuffer(this.byteBuffer.asReadOnlyBuffer());
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        buf.byteBuffer.order = this.byteBuffer.order;
        return buf;
    }

    @Override
    public ShortBuffer compact() {
        if (this.byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        this.byteBuffer.limit(this.limit * 2);
        this.byteBuffer.position(this.position * 2);
        this.byteBuffer.compact();
        this.byteBuffer.clear();
        this.position = this.limit - this.position;
        this.limit = this.capacity;
        this.mark = -1;
        return this;
    }

    @Override
    public ShortBuffer duplicate() {
        ByteBuffer bb = this.byteBuffer.duplicate().order(this.byteBuffer.order());
        ByteBufferAsShortBuffer buf = new ByteBufferAsShortBuffer(bb);
        buf.limit = this.limit;
        buf.position = this.position;
        buf.mark = this.mark;
        return buf;
    }

    @Override
    public short get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        ByteBuffer byteBuffer = this.byteBuffer;
        int i = this.position;
        this.position = i + 1;
        return byteBuffer.getShort(i * 2);
    }

    @Override
    public short get(int index) {
        checkIndex(index);
        return this.byteBuffer.getShort(index * 2);
    }

    @Override
    public ShortBuffer get(short[] dst, int dstOffset, int shortCount) {
        this.byteBuffer.limit(this.limit * 2);
        this.byteBuffer.position(this.position * 2);
        if (this.byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) this.byteBuffer).get(dst, dstOffset, shortCount);
        } else {
            ((ByteArrayBuffer) this.byteBuffer).get(dst, dstOffset, shortCount);
        }
        this.position += shortCount;
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
    short[] protectedArray() {
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
    public ShortBuffer put(short c) {
        if (this.position == this.limit) {
            throw new BufferOverflowException();
        }
        ByteBuffer byteBuffer = this.byteBuffer;
        int i = this.position;
        this.position = i + 1;
        byteBuffer.putShort(i * 2, c);
        return this;
    }

    @Override
    public ShortBuffer put(int index, short c) {
        checkIndex(index);
        this.byteBuffer.putShort(index * 2, c);
        return this;
    }

    @Override
    public ShortBuffer put(short[] src, int srcOffset, int shortCount) {
        this.byteBuffer.limit(this.limit * 2);
        this.byteBuffer.position(this.position * 2);
        if (this.byteBuffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) this.byteBuffer).put(src, srcOffset, shortCount);
        } else {
            ((ByteArrayBuffer) this.byteBuffer).put(src, srcOffset, shortCount);
        }
        this.position += shortCount;
        return this;
    }

    @Override
    public ShortBuffer slice() {
        this.byteBuffer.limit(this.limit * 2);
        this.byteBuffer.position(this.position * 2);
        ByteBuffer bb = this.byteBuffer.slice().order(this.byteBuffer.order());
        ShortBuffer result = new ByteBufferAsShortBuffer(bb);
        this.byteBuffer.clear();
        return result;
    }
}
