package java.io;

public class FilterInputStream extends InputStream {
    protected volatile InputStream in;

    protected FilterInputStream(InputStream inputStream) {
        this.in = inputStream;
    }

    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        return this.in.read(bArr, i, i2);
    }

    @Override
    public long skip(long j) throws IOException {
        return this.in.skip(j);
    }

    @Override
    public int available() throws IOException {
        return this.in.available();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
    }

    @Override
    public synchronized void mark(int i) {
        this.in.mark(i);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.in.reset();
    }

    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }
}
