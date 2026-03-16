package com.android.okio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

final class RealBufferedSource implements BufferedSource {
    public final OkBuffer buffer;
    private boolean closed;
    public final Source source;

    public RealBufferedSource(Source source, OkBuffer buffer) {
        if (source == null) {
            throw new IllegalArgumentException("source == null");
        }
        this.buffer = buffer;
        this.source = source;
    }

    public RealBufferedSource(Source source) {
        this(source, new OkBuffer());
    }

    @Override
    public OkBuffer buffer() {
        return this.buffer;
    }

    @Override
    public long read(OkBuffer sink, long byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        }
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        if (this.buffer.size == 0) {
            long read = this.source.read(this.buffer, 2048L);
            if (read == -1) {
                return -1L;
            }
        }
        long toRead = Math.min(byteCount, this.buffer.size);
        return this.buffer.read(sink, toRead);
    }

    @Override
    public boolean exhausted() throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        return this.buffer.exhausted() && this.source.read(this.buffer, 2048L) == -1;
    }

    @Override
    public void require(long byteCount) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        while (this.buffer.size < byteCount) {
            if (this.source.read(this.buffer, 2048L) == -1) {
                throw new EOFException();
            }
        }
    }

    @Override
    public byte readByte() throws IOException {
        require(1L);
        return this.buffer.readByte();
    }

    @Override
    public ByteString readByteString(long byteCount) throws IOException {
        require(byteCount);
        return this.buffer.readByteString(byteCount);
    }

    @Override
    public String readUtf8(long byteCount) throws IOException {
        require(byteCount);
        return this.buffer.readUtf8(byteCount);
    }

    @Override
    public String readUtf8Line() throws IOException {
        long newline = indexOf((byte) 10);
        if (newline != -1) {
            return this.buffer.readUtf8Line(newline);
        }
        if (this.buffer.size != 0) {
            return readUtf8(this.buffer.size);
        }
        return null;
    }

    @Override
    public String readUtf8LineStrict() throws IOException {
        long newline = indexOf((byte) 10);
        if (newline == -1) {
            throw new EOFException();
        }
        return this.buffer.readUtf8Line(newline);
    }

    @Override
    public short readShort() throws IOException {
        require(2L);
        return this.buffer.readShort();
    }

    @Override
    public short readShortLe() throws IOException {
        require(2L);
        return this.buffer.readShortLe();
    }

    @Override
    public int readInt() throws IOException {
        require(4L);
        return this.buffer.readInt();
    }

    @Override
    public int readIntLe() throws IOException {
        require(4L);
        return this.buffer.readIntLe();
    }

    @Override
    public long readLong() throws IOException {
        require(8L);
        return this.buffer.readLong();
    }

    @Override
    public long readLongLe() throws IOException {
        require(8L);
        return this.buffer.readLongLe();
    }

    @Override
    public void skip(long byteCount) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        while (byteCount > 0) {
            if (this.buffer.size == 0 && this.source.read(this.buffer, 2048L) == -1) {
                throw new EOFException();
            }
            long toSkip = Math.min(byteCount, this.buffer.size());
            this.buffer.skip(toSkip);
            byteCount -= toSkip;
        }
    }

    @Override
    public long indexOf(byte b) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        long start = 0;
        do {
            long index = this.buffer.indexOf(b, start);
            if (index == -1) {
                start = this.buffer.size;
            } else {
                return index;
            }
        } while (this.source.read(this.buffer, 2048L) != -1);
        return -1L;
    }

    @Override
    public InputStream inputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if (RealBufferedSource.this.closed) {
                    throw new IOException("closed");
                }
                if (RealBufferedSource.this.buffer.size == 0) {
                    long count = RealBufferedSource.this.source.read(RealBufferedSource.this.buffer, 2048L);
                    if (count == -1) {
                        return -1;
                    }
                }
                return RealBufferedSource.this.buffer.readByte() & 255;
            }

            @Override
            public int read(byte[] data, int offset, int byteCount) throws IOException {
                if (RealBufferedSource.this.closed) {
                    throw new IOException("closed");
                }
                Util.checkOffsetAndCount(data.length, offset, byteCount);
                if (RealBufferedSource.this.buffer.size == 0) {
                    long count = RealBufferedSource.this.source.read(RealBufferedSource.this.buffer, 2048L);
                    if (count == -1) {
                        return -1;
                    }
                }
                return RealBufferedSource.this.buffer.read(data, offset, byteCount);
            }

            @Override
            public int available() throws IOException {
                if (RealBufferedSource.this.closed) {
                    throw new IOException("closed");
                }
                return (int) Math.min(RealBufferedSource.this.buffer.size, 2147483647L);
            }

            @Override
            public void close() throws IOException {
                RealBufferedSource.this.close();
            }

            public String toString() {
                return RealBufferedSource.this + ".inputStream()";
            }
        };
    }

    @Override
    public Source mo2deadline(Deadline deadline) {
        this.source.mo2deadline(deadline);
        return this;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            this.source.close();
            this.buffer.clear();
        }
    }

    public String toString() {
        return "buffer(" + this.source + ")";
    }
}
