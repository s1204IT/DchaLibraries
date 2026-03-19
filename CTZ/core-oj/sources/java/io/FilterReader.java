package java.io;

public abstract class FilterReader extends Reader {
    protected Reader in;

    protected FilterReader(Reader reader) {
        super(reader);
        this.in = reader;
    }

    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        return this.in.read(cArr, i, i2);
    }

    @Override
    public long skip(long j) throws IOException {
        return this.in.skip(j);
    }

    @Override
    public boolean ready() throws IOException {
        return this.in.ready();
    }

    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }

    @Override
    public void mark(int i) throws IOException {
        this.in.mark(i);
    }

    @Override
    public void reset() throws IOException {
        this.in.reset();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
    }
}
