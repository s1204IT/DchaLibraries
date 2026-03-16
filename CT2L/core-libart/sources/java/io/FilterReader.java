package java.io;

public abstract class FilterReader extends Reader {
    protected Reader in;

    protected FilterReader(Reader in) {
        super(in);
        this.in = in;
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            this.in.close();
        }
    }

    @Override
    public synchronized void mark(int readlimit) throws IOException {
        synchronized (this.lock) {
            this.in.mark(readlimit);
        }
    }

    @Override
    public boolean markSupported() {
        boolean zMarkSupported;
        synchronized (this.lock) {
            zMarkSupported = this.in.markSupported();
        }
        return zMarkSupported;
    }

    @Override
    public int read() throws IOException {
        int i;
        synchronized (this.lock) {
            i = this.in.read();
        }
        return i;
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        int i;
        synchronized (this.lock) {
            i = this.in.read(buffer, offset, count);
        }
        return i;
    }

    @Override
    public boolean ready() throws IOException {
        boolean yVar;
        synchronized (this.lock) {
            yVar = this.in.ready();
        }
        return yVar;
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            this.in.reset();
        }
    }

    @Override
    public long skip(long charCount) throws IOException {
        long jSkip;
        synchronized (this.lock) {
            jSkip = this.in.skip(charCount);
        }
        return jSkip;
    }
}
