package java.util.zip;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;

public class Inflater {
    static final boolean $assertionsDisabled = false;
    private static final byte[] defaultBuf = new byte[0];
    private byte[] buf;
    private long bytesRead;
    private long bytesWritten;
    private boolean finished;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private int len;
    private boolean needDict;
    private int off;

    @ReachabilitySensitive
    private final ZStreamRef zsRef;

    private static native void end(long j);

    private static native int getAdler(long j);

    private native int inflateBytes(long j, byte[] bArr, int i, int i2) throws DataFormatException;

    private static native long init(boolean z);

    private static native void reset(long j);

    private static native void setDictionary(long j, byte[] bArr, int i, int i2);

    public Inflater(boolean z) {
        this.buf = defaultBuf;
        this.guard = CloseGuard.get();
        this.zsRef = new ZStreamRef(init(z));
        this.guard.open("end");
    }

    public Inflater() {
        this(false);
    }

    public void setInput(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (this.zsRef) {
            this.buf = bArr;
            this.off = i;
            this.len = i2;
        }
    }

    public void setInput(byte[] bArr) {
        setInput(bArr, 0, bArr.length);
    }

    public void setDictionary(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (this.zsRef) {
            ensureOpen();
            setDictionary(this.zsRef.address(), bArr, i, i2);
            this.needDict = false;
        }
    }

    public void setDictionary(byte[] bArr) {
        setDictionary(bArr, 0, bArr.length);
    }

    public int getRemaining() {
        int i;
        synchronized (this.zsRef) {
            i = this.len;
        }
        return i;
    }

    public boolean needsInput() {
        boolean z;
        synchronized (this.zsRef) {
            z = this.len <= 0;
        }
        return z;
    }

    public boolean needsDictionary() {
        boolean z;
        synchronized (this.zsRef) {
            z = this.needDict;
        }
        return z;
    }

    public boolean finished() {
        boolean z;
        synchronized (this.zsRef) {
            z = this.finished;
        }
        return z;
    }

    public int inflate(byte[] bArr, int i, int i2) throws DataFormatException {
        int iInflateBytes;
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (this.zsRef) {
            ensureOpen();
            int i3 = this.len;
            iInflateBytes = inflateBytes(this.zsRef.address(), bArr, i, i2);
            this.bytesWritten += (long) iInflateBytes;
            this.bytesRead += (long) (i3 - this.len);
        }
        return iInflateBytes;
    }

    public int inflate(byte[] bArr) throws DataFormatException {
        return inflate(bArr, 0, bArr.length);
    }

    public int getAdler() {
        int adler;
        synchronized (this.zsRef) {
            ensureOpen();
            adler = getAdler(this.zsRef.address());
        }
        return adler;
    }

    public int getTotalIn() {
        return (int) getBytesRead();
    }

    public long getBytesRead() {
        long j;
        synchronized (this.zsRef) {
            ensureOpen();
            j = this.bytesRead;
        }
        return j;
    }

    public int getTotalOut() {
        return (int) getBytesWritten();
    }

    public long getBytesWritten() {
        long j;
        synchronized (this.zsRef) {
            ensureOpen();
            j = this.bytesWritten;
        }
        return j;
    }

    public void reset() {
        synchronized (this.zsRef) {
            ensureOpen();
            reset(this.zsRef.address());
            this.buf = defaultBuf;
            this.finished = false;
            this.needDict = false;
            this.len = 0;
            this.off = 0;
            this.bytesWritten = 0L;
            this.bytesRead = 0L;
        }
    }

    public void end() {
        synchronized (this.zsRef) {
            this.guard.close();
            long jAddress = this.zsRef.address();
            this.zsRef.clear();
            if (jAddress != 0) {
                end(jAddress);
                this.buf = null;
            }
        }
    }

    protected void finalize() {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        end();
    }

    private void ensureOpen() {
        if (this.zsRef.address() == 0) {
            throw new IllegalStateException("Inflater has been closed");
        }
    }

    boolean ended() {
        boolean z;
        synchronized (this.zsRef) {
            z = this.zsRef.address() == 0;
        }
        return z;
    }
}
