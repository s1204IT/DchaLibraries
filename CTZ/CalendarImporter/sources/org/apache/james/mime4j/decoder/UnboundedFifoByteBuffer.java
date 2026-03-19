package org.apache.james.mime4j.decoder;

import java.util.Iterator;
import java.util.NoSuchElementException;

class UnboundedFifoByteBuffer {
    protected byte[] buffer;
    protected int head;
    protected int tail;

    public UnboundedFifoByteBuffer() {
        this(32);
    }

    public UnboundedFifoByteBuffer(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        this.buffer = new byte[i + 1];
        this.head = 0;
        this.tail = 0;
    }

    public int size() {
        if (this.tail < this.head) {
            return (this.buffer.length - this.head) + this.tail;
        }
        return this.tail - this.head;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean add(byte b) {
        if (size() + 1 >= this.buffer.length) {
            byte[] bArr = new byte[((this.buffer.length - 1) * 2) + 1];
            int i = this.head;
            int i2 = 0;
            while (i != this.tail) {
                bArr[i2] = this.buffer[i];
                this.buffer[i] = 0;
                i2++;
                i++;
                if (i == this.buffer.length) {
                    i = 0;
                }
            }
            this.buffer = bArr;
            this.head = 0;
            this.tail = i2;
        }
        this.buffer[this.tail] = b;
        this.tail++;
        if (this.tail >= this.buffer.length) {
            this.tail = 0;
        }
        return true;
    }

    public byte get() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }
        return this.buffer[this.head];
    }

    public byte remove() {
        if (isEmpty()) {
            throw new IllegalStateException("The buffer is already empty");
        }
        byte b = this.buffer[this.head];
        this.head++;
        if (this.head >= this.buffer.length) {
            this.head = 0;
        }
        return b;
    }

    private int increment(int i) {
        int i2 = i + 1;
        if (i2 >= this.buffer.length) {
            return 0;
        }
        return i2;
    }

    private int decrement(int i) {
        int i2 = i - 1;
        if (i2 < 0) {
            return this.buffer.length - 1;
        }
        return i2;
    }

    public Iterator iterator() {
        return new Iterator() {
            private int index;
            private int lastReturnedIndex = -1;

            {
                this.index = UnboundedFifoByteBuffer.this.head;
            }

            @Override
            public boolean hasNext() {
                return this.index != UnboundedFifoByteBuffer.this.tail;
            }

            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                this.lastReturnedIndex = this.index;
                this.index = UnboundedFifoByteBuffer.this.increment(this.index);
                return new Byte(UnboundedFifoByteBuffer.this.buffer[this.lastReturnedIndex]);
            }

            @Override
            public void remove() {
                if (this.lastReturnedIndex == -1) {
                    throw new IllegalStateException();
                }
                if (this.lastReturnedIndex == UnboundedFifoByteBuffer.this.head) {
                    UnboundedFifoByteBuffer.this.remove();
                    this.lastReturnedIndex = -1;
                    return;
                }
                int i = this.lastReturnedIndex + 1;
                while (i != UnboundedFifoByteBuffer.this.tail) {
                    if (i >= UnboundedFifoByteBuffer.this.buffer.length) {
                        UnboundedFifoByteBuffer.this.buffer[i - 1] = UnboundedFifoByteBuffer.this.buffer[0];
                        i = 0;
                    } else {
                        UnboundedFifoByteBuffer.this.buffer[i - 1] = UnboundedFifoByteBuffer.this.buffer[i];
                        i++;
                    }
                }
                this.lastReturnedIndex = -1;
                UnboundedFifoByteBuffer.this.tail = UnboundedFifoByteBuffer.this.decrement(UnboundedFifoByteBuffer.this.tail);
                UnboundedFifoByteBuffer.this.buffer[UnboundedFifoByteBuffer.this.tail] = 0;
                this.index = UnboundedFifoByteBuffer.this.decrement(this.index);
            }
        };
    }
}
