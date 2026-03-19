package java.io;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class BufferedInputStream extends FilterInputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 2147483639;
    private static final AtomicReferenceFieldUpdater<BufferedInputStream, byte[]> bufUpdater = AtomicReferenceFieldUpdater.newUpdater(BufferedInputStream.class, byte[].class, "buf");
    protected volatile byte[] buf;
    protected int count;
    protected int marklimit;
    protected int markpos;
    protected int pos;

    private InputStream getInIfOpen() throws IOException {
        InputStream inputStream = this.in;
        if (inputStream == null) {
            throw new IOException("Stream closed");
        }
        return inputStream;
    }

    private byte[] getBufIfOpen() throws IOException {
        byte[] bArr = this.buf;
        if (bArr == null) {
            throw new IOException("Stream closed");
        }
        return bArr;
    }

    public BufferedInputStream(InputStream inputStream) {
        this(inputStream, 8192);
    }

    public BufferedInputStream(InputStream inputStream, int i) {
        super(inputStream);
        this.markpos = -1;
        if (i <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.buf = new byte[i];
    }

    private void fill() throws IOException {
        byte[] bufIfOpen = getBufIfOpen();
        if (this.markpos < 0) {
            this.pos = 0;
        } else if (this.pos >= bufIfOpen.length) {
            if (this.markpos > 0) {
                int i = this.pos - this.markpos;
                System.arraycopy(bufIfOpen, this.markpos, bufIfOpen, 0, i);
                this.pos = i;
                this.markpos = 0;
            } else if (bufIfOpen.length >= this.marklimit) {
                this.markpos = -1;
                this.pos = 0;
            } else {
                int length = bufIfOpen.length;
                int i2 = MAX_BUFFER_SIZE;
                if (length >= MAX_BUFFER_SIZE) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (this.pos <= MAX_BUFFER_SIZE - this.pos) {
                    i2 = this.pos * 2;
                }
                if (i2 > this.marklimit) {
                    i2 = this.marklimit;
                }
                byte[] bArr = new byte[i2];
                System.arraycopy(bufIfOpen, 0, bArr, 0, this.pos);
                if (!bufUpdater.compareAndSet(this, bufIfOpen, bArr)) {
                    throw new IOException("Stream closed");
                }
                bufIfOpen = bArr;
            }
        }
        this.count = this.pos;
        int i3 = getInIfOpen().read(bufIfOpen, this.pos, bufIfOpen.length - this.pos);
        if (i3 > 0) {
            this.count = i3 + this.pos;
        }
    }

    @Override
    public synchronized int read() throws IOException {
        if (this.pos >= this.count) {
            fill();
            if (this.pos >= this.count) {
                return -1;
            }
        }
        byte[] bufIfOpen = getBufIfOpen();
        int i = this.pos;
        this.pos = i + 1;
        return bufIfOpen[i] & Character.DIRECTIONALITY_UNDEFINED;
    }

    private int read1(byte[] bArr, int i, int i2) throws IOException {
        int i3 = this.count - this.pos;
        if (i3 <= 0) {
            if (i2 >= getBufIfOpen().length && this.markpos < 0) {
                return getInIfOpen().read(bArr, i, i2);
            }
            fill();
            i3 = this.count - this.pos;
            if (i3 <= 0) {
                return -1;
            }
        }
        if (i3 < i2) {
            i2 = i3;
        }
        System.arraycopy(getBufIfOpen(), this.pos, bArr, i, i2);
        this.pos += i2;
        return i2;
    }

    @Override
    public synchronized int read(byte[] bArr, int i, int i2) throws IOException {
        getBufIfOpen();
        int i3 = i + i2;
        if ((i | i2 | i3 | (bArr.length - i3)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        int i4 = 0;
        if (i2 == 0) {
            return 0;
        }
        while (true) {
            int i5 = read1(bArr, i + i4, i2 - i4);
            if (i5 <= 0) {
                if (i4 == 0) {
                    i4 = i5;
                }
                return i4;
            }
            i4 += i5;
            if (i4 >= i2) {
                return i4;
            }
            InputStream inputStream = this.in;
            if (inputStream != null && inputStream.available() <= 0) {
                return i4;
            }
        }
    }

    @Override
    public synchronized long skip(long j) throws IOException {
        getBufIfOpen();
        if (j <= 0) {
            return 0L;
        }
        long j2 = this.count - this.pos;
        if (j2 <= 0) {
            if (this.markpos < 0) {
                return getInIfOpen().skip(j);
            }
            fill();
            j2 = this.count - this.pos;
            if (j2 <= 0) {
                return 0L;
            }
        }
        if (j2 < j) {
            j = j2;
        }
        this.pos = (int) (((long) this.pos) + j);
        return j;
    }

    @Override
    public synchronized int available() throws IOException {
        int i;
        int i2 = this.count - this.pos;
        int iAvailable = getInIfOpen().available();
        i = Integer.MAX_VALUE;
        if (i2 <= Integer.MAX_VALUE - iAvailable) {
            i = i2 + iAvailable;
        }
        return i;
    }

    @Override
    public synchronized void mark(int i) {
        this.marklimit = i;
        this.markpos = this.pos;
    }

    @Override
    public synchronized void reset() throws IOException {
        getBufIfOpen();
        if (this.markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        this.pos = this.markpos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void close() throws IOException {
        byte[] bArr;
        do {
            bArr = this.buf;
            if (bArr == null) {
                return;
            }
        } while (!bufUpdater.compareAndSet(this, bArr, null));
        InputStream inputStream = this.in;
        this.in = null;
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
