package java.io;

import java.util.Arrays;

public class ByteArrayOutputStream extends OutputStream {
    private static final int MAX_ARRAY_SIZE = 2147483639;
    protected byte[] buf;
    protected int count;

    public ByteArrayOutputStream() {
        this(32);
    }

    public ByteArrayOutputStream(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Negative initial size: " + i);
        }
        this.buf = new byte[i];
    }

    private void ensureCapacity(int i) {
        if (i - this.buf.length > 0) {
            grow(i);
        }
    }

    private void grow(int i) {
        int length = this.buf.length << 1;
        if (length - i < 0) {
            length = i;
        }
        if (length - MAX_ARRAY_SIZE > 0) {
            length = hugeCapacity(i);
        }
        this.buf = Arrays.copyOf(this.buf, length);
    }

    private static int hugeCapacity(int i) {
        if (i < 0) {
            throw new OutOfMemoryError();
        }
        if (i <= MAX_ARRAY_SIZE) {
            return MAX_ARRAY_SIZE;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public synchronized void write(int i) {
        ensureCapacity(this.count + 1);
        this.buf[this.count] = (byte) i;
        this.count++;
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) {
        if (i >= 0) {
            if (i <= bArr.length && i2 >= 0 && (i + i2) - bArr.length <= 0) {
                ensureCapacity(this.count + i2);
                System.arraycopy(bArr, i, this.buf, this.count, i2);
                this.count += i2;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    public synchronized void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(this.buf, 0, this.count);
    }

    public synchronized void reset() {
        this.count = 0;
    }

    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(this.buf, this.count);
    }

    public synchronized int size() {
        return this.count;
    }

    public synchronized String toString() {
        return new String(this.buf, 0, this.count);
    }

    public synchronized String toString(String str) throws UnsupportedEncodingException {
        return new String(this.buf, 0, this.count, str);
    }

    @Deprecated
    public synchronized String toString(int i) {
        return new String(this.buf, i, 0, this.count);
    }

    @Override
    public void close() throws IOException {
    }
}
