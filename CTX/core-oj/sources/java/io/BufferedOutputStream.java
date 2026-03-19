package java.io;

public class BufferedOutputStream extends FilterOutputStream {
    protected byte[] buf;
    protected int count;

    public BufferedOutputStream(OutputStream outputStream) {
        this(outputStream, 8192);
    }

    public BufferedOutputStream(OutputStream outputStream, int i) {
        super(outputStream);
        if (i <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.buf = new byte[i];
    }

    private void flushBuffer() throws IOException {
        if (this.count > 0) {
            this.out.write(this.buf, 0, this.count);
            this.count = 0;
        }
    }

    @Override
    public synchronized void write(int i) throws IOException {
        if (this.count >= this.buf.length) {
            flushBuffer();
        }
        byte[] bArr = this.buf;
        int i2 = this.count;
        this.count = i2 + 1;
        bArr[i2] = (byte) i;
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
        if (i2 >= this.buf.length) {
            flushBuffer();
            this.out.write(bArr, i, i2);
        } else {
            if (i2 > this.buf.length - this.count) {
                flushBuffer();
            }
            System.arraycopy(bArr, i, this.buf, this.count, i2);
            this.count += i2;
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        flushBuffer();
        this.out.flush();
    }
}
