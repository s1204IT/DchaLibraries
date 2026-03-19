package java.nio;

public abstract class Buffer {
    static final int SPLITERATOR_CHARACTERISTICS = 16464;
    final int _elementSizeShift;
    long address;
    private int capacity;
    private int limit;
    private int mark;
    int position = 0;

    public abstract Object array();

    public abstract int arrayOffset();

    public abstract boolean hasArray();

    public abstract boolean isDirect();

    public abstract boolean isReadOnly();

    Buffer(int i, int i2, int i3, int i4, int i5) {
        this.mark = -1;
        if (i4 < 0) {
            throw new IllegalArgumentException("Negative capacity: " + i4);
        }
        this.capacity = i4;
        limit(i3);
        position(i2);
        if (i >= 0) {
            if (i > i2) {
                throw new IllegalArgumentException("mark > position: (" + i + " > " + i2 + ")");
            }
            this.mark = i;
        }
        this._elementSizeShift = i5;
    }

    public final int capacity() {
        return this.capacity;
    }

    public final int position() {
        return this.position;
    }

    public final Buffer position(int i) {
        if (i > this.limit || i < 0) {
            throw new IllegalArgumentException("Bad position " + i + "/" + this.limit);
        }
        this.position = i;
        if (this.mark > this.position) {
            this.mark = -1;
        }
        return this;
    }

    public final int limit() {
        return this.limit;
    }

    public final Buffer limit(int i) {
        if (i > this.capacity || i < 0) {
            throw new IllegalArgumentException();
        }
        this.limit = i;
        if (this.position > this.limit) {
            this.position = this.limit;
        }
        if (this.mark > this.limit) {
            this.mark = -1;
        }
        return this;
    }

    public final Buffer mark() {
        this.mark = this.position;
        return this;
    }

    public final Buffer reset() {
        int i = this.mark;
        if (i < 0) {
            throw new InvalidMarkException();
        }
        this.position = i;
        return this;
    }

    public final Buffer clear() {
        this.position = 0;
        this.limit = this.capacity;
        this.mark = -1;
        return this;
    }

    public final Buffer flip() {
        this.limit = this.position;
        this.position = 0;
        this.mark = -1;
        return this;
    }

    public final Buffer rewind() {
        this.position = 0;
        this.mark = -1;
        return this;
    }

    public final int remaining() {
        return this.limit - this.position;
    }

    public final boolean hasRemaining() {
        return this.position < this.limit;
    }

    final int nextGetIndex() {
        if (this.position >= this.limit) {
            throw new BufferUnderflowException();
        }
        int i = this.position;
        this.position = i + 1;
        return i;
    }

    final int nextGetIndex(int i) {
        if (this.limit - this.position < i) {
            throw new BufferUnderflowException();
        }
        int i2 = this.position;
        this.position += i;
        return i2;
    }

    final int nextPutIndex() {
        if (this.position >= this.limit) {
            throw new BufferOverflowException();
        }
        int i = this.position;
        this.position = i + 1;
        return i;
    }

    final int nextPutIndex(int i) {
        if (this.limit - this.position < i) {
            throw new BufferOverflowException();
        }
        int i2 = this.position;
        this.position += i;
        return i2;
    }

    final int checkIndex(int i) {
        if (i < 0 || i >= this.limit) {
            throw new IndexOutOfBoundsException("index=" + i + " out of bounds (limit=" + this.limit + ")");
        }
        return i;
    }

    final int checkIndex(int i, int i2) {
        if (i < 0 || i2 > this.limit - i) {
            throw new IndexOutOfBoundsException("index=" + i + " out of bounds (limit=" + this.limit + ", nb=" + i2 + ")");
        }
        return i;
    }

    final int markValue() {
        return this.mark;
    }

    final void truncate() {
        this.mark = -1;
        this.position = 0;
        this.limit = 0;
        this.capacity = 0;
    }

    final void discardMark() {
        this.mark = -1;
    }

    static void checkBounds(int i, int i2, int i3) {
        int i4 = i + i2;
        if ((i | i2 | i4 | (i3 - i4)) < 0) {
            throw new IndexOutOfBoundsException("off=" + i + ", len=" + i2 + " out of bounds (size=" + i3 + ")");
        }
    }

    public int getElementSizeShift() {
        return this._elementSizeShift;
    }
}
