package java.util.zip;

import dalvik.system.CloseGuard;
import java.util.Arrays;
import libcore.util.EmptyArray;

public class Deflater {
    public static final int BEST_COMPRESSION = 9;
    public static final int BEST_SPEED = 1;
    public static final int DEFAULT_COMPRESSION = -1;
    public static final int DEFAULT_STRATEGY = 0;
    public static final int DEFLATED = 8;
    public static final int FILTERED = 1;
    private static final int FINISH = 4;
    public static final int FULL_FLUSH = 3;
    public static final int HUFFMAN_ONLY = 2;
    public static final int NO_COMPRESSION = 0;
    public static final int NO_FLUSH = 0;
    public static final int SYNC_FLUSH = 2;
    private int compressLevel;
    private boolean finished;
    private int flushParm;
    private final CloseGuard guard;
    private int inLength;
    private int inRead;
    private byte[] inputBuffer;
    private int strategy;
    private long streamHandle;

    private native long createStream(int i, int i2, boolean z);

    private native int deflateImpl(byte[] bArr, int i, int i2, long j, int i3);

    private native void endImpl(long j);

    private native int getAdlerImpl(long j);

    private native long getTotalInImpl(long j);

    private native long getTotalOutImpl(long j);

    private native void resetImpl(long j);

    private native void setDictionaryImpl(byte[] bArr, int i, int i2, long j);

    private native void setInputImpl(byte[] bArr, int i, int i2, long j);

    private native void setLevelsImpl(int i, int i2, long j);

    public Deflater() {
        this(-1, false);
    }

    public Deflater(int level) {
        this(level, false);
    }

    public Deflater(int level, boolean noHeader) {
        this.flushParm = 0;
        this.compressLevel = -1;
        this.strategy = 0;
        this.streamHandle = -1L;
        this.guard = CloseGuard.get();
        if (level < -1 || level > 9) {
            throw new IllegalArgumentException("Bad level: " + level);
        }
        this.compressLevel = level;
        this.streamHandle = createStream(this.compressLevel, this.strategy, noHeader);
        this.guard.open("end");
    }

    public int deflate(byte[] buf) {
        return deflate(buf, 0, buf.length);
    }

    public synchronized int deflate(byte[] buf, int offset, int byteCount) {
        return deflateImpl(buf, offset, byteCount, this.flushParm);
    }

    public synchronized int deflate(byte[] buf, int offset, int byteCount, int flush) {
        if (flush != 0 && flush != 2 && flush != 3) {
            throw new IllegalArgumentException("Bad flush value: " + flush);
        }
        return deflateImpl(buf, offset, byteCount, flush);
    }

    private synchronized int deflateImpl(byte[] buf, int offset, int byteCount, int flush) {
        checkOpen();
        Arrays.checkOffsetAndCount(buf.length, offset, byteCount);
        if (this.inputBuffer == null) {
            setInput(EmptyArray.BYTE);
        }
        return deflateImpl(buf, offset, byteCount, this.streamHandle, flush);
    }

    public synchronized void end() {
        this.guard.close();
        endImpl();
    }

    private void endImpl() {
        if (this.streamHandle != -1) {
            endImpl(this.streamHandle);
            this.inputBuffer = null;
            this.streamHandle = -1L;
        }
    }

    protected void finalize() {
        AssertionError assertionError;
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            synchronized (this) {
                end();
                endImpl();
            }
            try {
                super.finalize();
            } finally {
            }
        } catch (Throwable th) {
            try {
                super.finalize();
                throw th;
            } finally {
            }
        }
    }

    public synchronized void finish() {
        this.flushParm = 4;
    }

    public synchronized boolean finished() {
        return this.finished;
    }

    public synchronized int getAdler() {
        checkOpen();
        return getAdlerImpl(this.streamHandle);
    }

    public synchronized int getTotalIn() {
        checkOpen();
        return (int) getTotalInImpl(this.streamHandle);
    }

    public synchronized int getTotalOut() {
        checkOpen();
        return (int) getTotalOutImpl(this.streamHandle);
    }

    public synchronized boolean needsInput() {
        boolean z = true;
        synchronized (this) {
            if (this.inputBuffer != null) {
                if (this.inRead != this.inLength) {
                    z = false;
                }
            }
        }
        return z;
    }

    public synchronized void reset() {
        checkOpen();
        this.flushParm = 0;
        this.finished = false;
        resetImpl(this.streamHandle);
        this.inputBuffer = null;
    }

    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }

    public synchronized void setDictionary(byte[] buf, int offset, int byteCount) {
        checkOpen();
        Arrays.checkOffsetAndCount(buf.length, offset, byteCount);
        setDictionaryImpl(buf, offset, byteCount, this.streamHandle);
    }

    public void setInput(byte[] buf) {
        setInput(buf, 0, buf.length);
    }

    public synchronized void setInput(byte[] buf, int offset, int byteCount) {
        checkOpen();
        Arrays.checkOffsetAndCount(buf.length, offset, byteCount);
        this.inLength = byteCount;
        this.inRead = 0;
        if (this.inputBuffer == null) {
            setLevelsImpl(this.compressLevel, this.strategy, this.streamHandle);
        }
        this.inputBuffer = buf;
        setInputImpl(buf, offset, byteCount, this.streamHandle);
    }

    public synchronized void setLevel(int level) {
        if (level < -1 || level > 9) {
            throw new IllegalArgumentException("Bad level: " + level);
        }
        if (this.inputBuffer != null) {
            throw new IllegalStateException("setLevel cannot be called after setInput");
        }
        this.compressLevel = level;
    }

    public synchronized void setStrategy(int strategy) {
        if (strategy < 0 || strategy > 2) {
            throw new IllegalArgumentException("Bad strategy: " + strategy);
        }
        if (this.inputBuffer != null) {
            throw new IllegalStateException("setStrategy cannot be called after setInput");
        }
        this.strategy = strategy;
    }

    public synchronized long getBytesRead() {
        checkOpen();
        return getTotalInImpl(this.streamHandle);
    }

    public synchronized long getBytesWritten() {
        checkOpen();
        return getTotalOutImpl(this.streamHandle);
    }

    private void checkOpen() {
        if (this.streamHandle == -1) {
            throw new IllegalStateException("attempt to use Deflater after calling end");
        }
    }
}
