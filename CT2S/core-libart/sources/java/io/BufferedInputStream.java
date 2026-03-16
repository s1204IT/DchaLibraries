package java.io;

import java.util.Arrays;

public class BufferedInputStream extends FilterInputStream {
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    protected volatile byte[] buf;
    protected int count;
    protected int marklimit;
    protected int markpos;
    protected int pos;

    public BufferedInputStream(InputStream in) {
        this(in, 8192);
    }

    public BufferedInputStream(InputStream in, int size) {
        super(in);
        this.markpos = -1;
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.buf = new byte[size];
    }

    @Override
    public synchronized int available() throws IOException {
        InputStream localIn;
        localIn = this.in;
        if (this.buf == null || localIn == null) {
            throw streamClosed();
        }
        return (this.count - this.pos) + localIn.available();
    }

    private IOException streamClosed() throws IOException {
        throw new IOException("BufferedInputStream is closed");
    }

    @Override
    public void close() throws IOException {
        this.buf = null;
        InputStream localIn = this.in;
        this.in = null;
        if (localIn != null) {
            localIn.close();
        }
    }

    private int fillbuf(InputStream localIn, byte[] localBuf) throws IOException {
        if (this.markpos == -1 || this.pos - this.markpos >= this.marklimit) {
            int result = localIn.read(localBuf);
            if (result > 0) {
                this.markpos = -1;
                this.pos = 0;
                this.count = result != -1 ? result : 0;
            }
            return result;
        }
        if (this.markpos == 0 && this.marklimit > localBuf.length) {
            int newLength = localBuf.length * 2;
            if (newLength > this.marklimit) {
                newLength = this.marklimit;
            }
            byte[] newbuf = new byte[newLength];
            System.arraycopy(localBuf, 0, newbuf, 0, localBuf.length);
            this.buf = newbuf;
            localBuf = newbuf;
        } else if (this.markpos > 0) {
            System.arraycopy(localBuf, this.markpos, localBuf, 0, localBuf.length - this.markpos);
        }
        this.pos -= this.markpos;
        this.markpos = 0;
        this.count = 0;
        int bytesread = localIn.read(localBuf, this.pos, localBuf.length - this.pos);
        this.count = bytesread <= 0 ? this.pos : this.pos + bytesread;
        return bytesread;
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.marklimit = readlimit;
        this.markpos = this.pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized int read() throws IOException {
        int i = -1;
        synchronized (this) {
            byte[] localBuf = this.buf;
            InputStream localIn = this.in;
            if (localBuf == null || localIn == null) {
                throw streamClosed();
            }
            if (this.pos < this.count || fillbuf(localIn, localBuf) != -1) {
                if (localBuf != this.buf && (localBuf = this.buf) == null) {
                    throw streamClosed();
                }
                if (this.count - this.pos > 0) {
                    int i2 = this.pos;
                    this.pos = i2 + 1;
                    i = localBuf[i2] & Character.DIRECTIONALITY_UNDEFINED;
                }
            }
        }
        return i;
    }

    @Override
    public synchronized int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int required;
        int read;
        int i = -1;
        synchronized (this) {
            byte[] localBuf = this.buf;
            if (localBuf == null) {
                throw streamClosed();
            }
            Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
            if (byteCount == 0) {
                i = 0;
            } else {
                InputStream localIn = this.in;
                if (localIn == null) {
                    throw streamClosed();
                }
                if (this.pos < this.count) {
                    int copylength = this.count - this.pos >= byteCount ? byteCount : this.count - this.pos;
                    System.arraycopy(localBuf, this.pos, buffer, byteOffset, copylength);
                    this.pos += copylength;
                    if (copylength == byteCount || localIn.available() == 0) {
                        i = copylength;
                    } else {
                        byteOffset += copylength;
                        required = byteCount - copylength;
                    }
                } else {
                    required = byteCount;
                }
                while (true) {
                    if (this.markpos == -1 && required >= localBuf.length) {
                        read = localIn.read(buffer, byteOffset, required);
                        if (read == -1) {
                            if (required != byteCount) {
                                i = byteCount - required;
                            }
                        } else {
                            required -= read;
                            if (required != 0) {
                            }
                        }
                    } else if (fillbuf(localIn, localBuf) == -1) {
                        if (required != byteCount) {
                            i = byteCount - required;
                        }
                    } else {
                        if (localBuf != this.buf && (localBuf = this.buf) == null) {
                            throw streamClosed();
                        }
                        read = this.count - this.pos >= required ? required : this.count - this.pos;
                        System.arraycopy(localBuf, this.pos, buffer, byteOffset, read);
                        this.pos += read;
                        required -= read;
                        if (required != 0) {
                            i = byteCount;
                            break;
                        }
                        if (localIn.available() == 0) {
                            i = byteCount - required;
                            break;
                        }
                        byteOffset += read;
                    }
                }
            }
        }
        return i;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (this.buf == null) {
            throw new IOException("Stream is closed");
        }
        if (this.markpos == -1) {
            throw new IOException("Mark has been invalidated.");
        }
        this.pos = this.markpos;
    }

    @Override
    public synchronized long skip(long byteCount) throws IOException {
        byte[] localBuf = this.buf;
        InputStream localIn = this.in;
        if (localBuf == null) {
            throw streamClosed();
        }
        if (byteCount < 1) {
            byteCount = 0;
        } else {
            if (localIn == null) {
                throw streamClosed();
            }
            if (this.count - this.pos >= byteCount) {
                this.pos = (int) (((long) this.pos) + byteCount);
            } else {
                long read = this.count - this.pos;
                this.pos = this.count;
                if (this.markpos != -1 && byteCount <= this.marklimit) {
                    if (fillbuf(localIn, localBuf) == -1) {
                        byteCount = read;
                    } else if (this.count - this.pos >= byteCount - read) {
                        this.pos = (int) (((long) this.pos) + (byteCount - read));
                    } else {
                        long read2 = read + ((long) (this.count - this.pos));
                        this.pos = this.count;
                        byteCount = read2;
                    }
                } else {
                    byteCount = read + localIn.skip(byteCount - read);
                }
            }
        }
        return byteCount;
    }
}
