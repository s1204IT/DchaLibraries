package java.util.zip;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;

public class Deflater {
    static final boolean $assertionsDisabled = false;
    public static final int BEST_COMPRESSION = 9;
    public static final int BEST_SPEED = 1;
    public static final int DEFAULT_COMPRESSION = -1;
    public static final int DEFAULT_STRATEGY = 0;
    public static final int DEFLATED = 8;
    public static final int FILTERED = 1;
    public static final int FULL_FLUSH = 3;
    public static final int HUFFMAN_ONLY = 2;
    public static final int NO_COMPRESSION = 0;
    public static final int NO_FLUSH = 0;
    public static final int SYNC_FLUSH = 2;
    private byte[] buf;
    private long bytesRead;
    private long bytesWritten;
    private boolean finish;
    private boolean finished;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private int len;
    private int level;
    private int off;
    private boolean setParams;
    private int strategy;

    @ReachabilitySensitive
    private final ZStreamRef zsRef;

    private native int deflateBytes(long j, byte[] bArr, int i, int i2, int i3);

    private static native void end(long j);

    private static native int getAdler(long j);

    private static native long init(int i, int i2, boolean z);

    private static native void reset(long j);

    private static native void setDictionary(long j, byte[] bArr, int i, int i2);

    public Deflater(int i, boolean z) {
        this.buf = new byte[0];
        this.guard = CloseGuard.get();
        this.level = i;
        this.strategy = 0;
        this.zsRef = new ZStreamRef(init(i, 0, z));
        this.guard.open("end");
    }

    public Deflater(int i) {
        this(i, $assertionsDisabled);
    }

    public Deflater() {
        this(-1, $assertionsDisabled);
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
        }
    }

    public void setDictionary(byte[] bArr) {
        setDictionary(bArr, 0, bArr.length);
    }

    public void setStrategy(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
                synchronized (this.zsRef) {
                    if (this.strategy != i) {
                        this.strategy = i;
                        this.setParams = true;
                    }
                    break;
                }
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setLevel(int i) {
        if ((i < 0 || i > 9) && i != -1) {
            throw new IllegalArgumentException("invalid compression level");
        }
        synchronized (this.zsRef) {
            if (this.level != i) {
                this.level = i;
                this.setParams = true;
            }
        }
    }

    public boolean needsInput() {
        boolean z;
        synchronized (this.zsRef) {
            z = this.len <= 0 ? true : $assertionsDisabled;
        }
        return z;
    }

    public void finish() {
        synchronized (this.zsRef) {
            this.finish = true;
        }
    }

    public boolean finished() {
        boolean z;
        synchronized (this.zsRef) {
            z = this.finished;
        }
        return z;
    }

    public int deflate(byte[] bArr, int i, int i2) {
        return deflate(bArr, i, i2, 0);
    }

    public int deflate(byte[] bArr) {
        return deflate(bArr, 0, bArr.length, 0);
    }

    public int deflate(byte[] bArr, int i, int i2, int i3) {
        int iDeflateBytes;
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (this.zsRef) {
            ensureOpen();
            if (i3 != 0 && i3 != 2 && i3 != 3) {
                throw new IllegalArgumentException();
            }
            int i4 = this.len;
            iDeflateBytes = deflateBytes(this.zsRef.address(), bArr, i, i2, i3);
            this.bytesWritten += (long) iDeflateBytes;
            this.bytesRead += (long) (i4 - this.len);
        }
        return iDeflateBytes;
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
            this.finish = $assertionsDisabled;
            this.finished = $assertionsDisabled;
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
            throw new NullPointerException("Deflater has been closed");
        }
    }
}
