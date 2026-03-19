package org.apache.james.mime4j.decoder;

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
}
