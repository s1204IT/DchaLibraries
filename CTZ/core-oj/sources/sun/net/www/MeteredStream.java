package sun.net.www;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import sun.net.ProgressSource;

public class MeteredStream extends FilterInputStream {
    protected boolean closed;
    protected long count;
    protected long expected;
    protected int markLimit;
    protected long markedCount;
    protected ProgressSource pi;

    public MeteredStream(InputStream inputStream, ProgressSource progressSource, long j) {
        super(inputStream);
        this.closed = false;
        this.count = 0L;
        this.markedCount = 0L;
        this.markLimit = -1;
        this.pi = progressSource;
        this.expected = j;
        if (progressSource != null) {
            progressSource.updateProgress(0L, j);
        }
    }

    private final void justRead(long j) throws IOException {
        if (j == -1) {
            if (!isMarked()) {
                close();
                return;
            }
            return;
        }
        this.count += j;
        if (this.count - this.markedCount > this.markLimit) {
            this.markLimit = -1;
        }
        if (this.pi != null) {
            this.pi.updateProgress(this.count, this.expected);
        }
        if (!isMarked() && this.expected > 0 && this.count >= this.expected) {
            close();
        }
    }

    private boolean isMarked() {
        return this.markLimit >= 0 && this.count - this.markedCount <= ((long) this.markLimit);
    }

    @Override
    public synchronized int read() throws IOException {
        if (this.closed) {
            return -1;
        }
        int i = this.in.read();
        if (i != -1) {
            justRead(1L);
        } else {
            justRead(i);
        }
        return i;
    }

    @Override
    public synchronized int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.closed) {
            return -1;
        }
        int i3 = this.in.read(bArr, i, i2);
        justRead(i3);
        return i3;
    }

    @Override
    public synchronized long skip(long j) throws IOException {
        if (this.closed) {
            return 0L;
        }
        if (j > this.expected - this.count) {
            j = this.expected - this.count;
        }
        long jSkip = this.in.skip(j);
        justRead(jSkip);
        return jSkip;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.closed) {
            return;
        }
        if (this.pi != null) {
            this.pi.finishTracking();
        }
        this.closed = true;
        this.in.close();
    }

    @Override
    public synchronized int available() throws IOException {
        return this.closed ? 0 : this.in.available();
    }

    @Override
    public synchronized void mark(int i) {
        if (this.closed) {
            return;
        }
        super.mark(i);
        this.markedCount = this.count;
        this.markLimit = i;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (this.closed) {
            return;
        }
        if (!isMarked()) {
            throw new IOException("Resetting to an invalid mark");
        }
        this.count = this.markedCount;
        super.reset();
    }

    @Override
    public boolean markSupported() {
        if (this.closed) {
            return false;
        }
        return super.markSupported();
    }

    protected void finalize() throws Throwable {
        try {
            close();
            if (this.pi != null) {
                this.pi.close();
            }
        } finally {
            super.finalize();
        }
    }
}
