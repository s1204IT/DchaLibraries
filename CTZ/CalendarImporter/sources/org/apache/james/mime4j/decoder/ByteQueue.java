package org.apache.james.mime4j.decoder;

import java.util.Iterator;

public class ByteQueue {
    private UnboundedFifoByteBuffer buf;
    private int initialCapacity;

    public ByteQueue() {
        this.initialCapacity = -1;
        this.buf = new UnboundedFifoByteBuffer();
    }

    public ByteQueue(int i) {
        this.initialCapacity = -1;
        this.buf = new UnboundedFifoByteBuffer(i);
        this.initialCapacity = i;
    }

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

    public Iterator iterator() {
        return this.buf.iterator();
    }
}
