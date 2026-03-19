package org.apache.james.mime4j.decoder;

public class ByteQueue {
    private int initialCapacity = -1;
    private UnboundedFifoByteBuffer buf = new UnboundedFifoByteBuffer();

    public void enqueue(byte b) {
        this.buf.add(b);
    }

    public byte dequeue() {
        return this.buf.remove();
    }

    public int count() {
        return this.buf.size();
    }

    public void clear() {
        if (this.initialCapacity != -1) {
            this.buf = new UnboundedFifoByteBuffer(this.initialCapacity);
        } else {
            this.buf = new UnboundedFifoByteBuffer();
        }
    }
}
