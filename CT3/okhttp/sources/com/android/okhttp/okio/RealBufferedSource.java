package com.android.okhttp.okio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

final class RealBufferedSource implements BufferedSource {
    public final Buffer buffer;
    private boolean closed;
    public final Source source;

    public RealBufferedSource(Source source, Buffer buffer) {
        if (source == null) {
            throw new IllegalArgumentException("source == null");
        }
        this.buffer = buffer;
        this.source = source;
    }

    public RealBufferedSource(Source source) {
        this(source, new Buffer());
    }

    @Override
    public Buffer buffer() {
        return this.buffer;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (sink == null) {
            throw new IllegalArgumentException("sink == null");
        }
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        }
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        if (this.buffer.size == 0) {
            long read = this.source.read(this.buffer, 8192L);
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
        return this.buffer.exhausted() && this.source.read(this.buffer, 8192L) == -1;
    }

    @Override
    public void require(long byteCount) throws IOException {
        if (!request(byteCount)) {
            throw new EOFException();
        }
    }

    @Override
    public boolean request(long byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        }
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        while (this.buffer.size < byteCount) {
            if (this.source.read(this.buffer, 8192L) == -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public byte readByte() throws IOException {
        require(1L);
        return this.buffer.readByte();
    }

    @Override
    public ByteString readByteString() throws IOException {
        this.buffer.writeAll(this.source);
        return this.buffer.readByteString();
    }

    @Override
    public ByteString readByteString(long byteCount) throws IOException {
        require(byteCount);
        return this.buffer.readByteString(byteCount);
    }

    @Override
    public byte[] readByteArray() throws IOException {
        this.buffer.writeAll(this.source);
        return this.buffer.readByteArray();
    }

    @Override
    public byte[] readByteArray(long byteCount) throws IOException {
        require(byteCount);
        return this.buffer.readByteArray(byteCount);
    }

    @Override
    public int read(byte[] sink) throws IOException {
        return read(sink, 0, sink.length);
    }

    @Override
    public void readFully(byte[] sink) throws IOException {
        try {
            require(sink.length);
            this.buffer.readFully(sink);
        } catch (EOFException e) {
            int offset = 0;
            while (this.buffer.size > 0) {
                int read = this.buffer.read(sink, offset, (int) this.buffer.size);
                if (read == -1) {
                    throw new AssertionError();
                }
                offset += read;
            }
            throw e;
        }
    }

    @Override
    public int read(byte[] sink, int offset, int byteCount) throws IOException {
        Util.checkOffsetAndCount(sink.length, offset, byteCount);
        if (this.buffer.size == 0) {
            long read = this.source.read(this.buffer, 8192L);
            if (read == -1) {
                return -1;
            }
        }
        int toRead = (int) Math.min(byteCount, this.buffer.size);
        return this.buffer.read(sink, offset, toRead);
    }

    @Override
    public void readFully(Buffer sink, long byteCount) throws IOException {
        try {
            require(byteCount);
            this.buffer.readFully(sink, byteCount);
        } catch (EOFException e) {
            sink.writeAll(this.buffer);
            throw e;
        }
    }

    @Override
    public long readAll(Sink sink) throws IOException {
        if (sink == null) {
            throw new IllegalArgumentException("sink == null");
        }
        long totalBytesWritten = 0;
        while (this.source.read(this.buffer, 8192L) != -1) {
            long emitByteCount = this.buffer.completeSegmentByteCount();
            if (emitByteCount > 0) {
                totalBytesWritten += emitByteCount;
                sink.write(this.buffer, emitByteCount);
            }
        }
        if (this.buffer.size() > 0) {
            long totalBytesWritten2 = totalBytesWritten + this.buffer.size();
            sink.write(this.buffer, this.buffer.size());
            return totalBytesWritten2;
        }
        return totalBytesWritten;
    }

    @Override
    public String readUtf8() throws IOException {
        this.buffer.writeAll(this.source);
        return this.buffer.readUtf8();
    }

    @Override
    public String readUtf8(long byteCount) throws IOException {
        require(byteCount);
        return this.buffer.readUtf8(byteCount);
    }

    @Override
    public String readString(Charset charset) throws IOException {
        if (charset == null) {
            throw new IllegalArgumentException("charset == null");
        }
        this.buffer.writeAll(this.source);
        return this.buffer.readString(charset);
    }

    @Override
    public String readString(long byteCount, Charset charset) throws IOException {
        require(byteCount);
        if (charset == null) {
            throw new IllegalArgumentException("charset == null");
        }
        return this.buffer.readString(byteCount, charset);
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
        if (newline != -1) {
            return this.buffer.readUtf8Line(newline);
        }
        Buffer data = new Buffer();
        this.buffer.copyTo(data, 0L, Math.min(32L, this.buffer.size()));
        throw new EOFException("\\n not found: size=" + this.buffer.size() + " content=" + data.readByteString().hex() + "...");
    }

    @Override
    public int readUtf8CodePoint() throws IOException {
        require(1L);
        byte b0 = this.buffer.getByte(0L);
        if ((b0 & 224) == 192) {
            require(2L);
        } else if ((b0 & 240) == 224) {
            require(3L);
        } else if ((b0 & 248) == 240) {
            require(4L);
        }
        return this.buffer.readUtf8CodePoint();
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
    public long readDecimalLong() throws IOException {
        require(1L);
        for (int pos = 0; request(pos + 1); pos++) {
            byte b = this.buffer.getByte(pos);
            if ((b < 48 || b > 57) && (pos != 0 || b != 45)) {
                if (pos == 0) {
                    throw new NumberFormatException(String.format("Expected leading [0-9] or '-' character but was %#x", Byte.valueOf(b)));
                }
                return this.buffer.readDecimalLong();
            }
        }
        return this.buffer.readDecimalLong();
    }

    @Override
    public long readHexadecimalUnsignedLong() throws IOException {
        require(1L);
        for (int pos = 0; request(pos + 1); pos++) {
            byte b = this.buffer.getByte(pos);
            if ((b < 48 || b > 57) && ((b < 97 || b > 102) && (b < 65 || b > 70))) {
                if (pos == 0) {
                    throw new NumberFormatException(String.format("Expected leading [0-9a-fA-F] character but was %#x", Byte.valueOf(b)));
                }
                return this.buffer.readHexadecimalUnsignedLong();
            }
        }
        return this.buffer.readHexadecimalUnsignedLong();
    }

    @Override
    public void skip(long byteCount) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        while (byteCount > 0) {
            if (this.buffer.size == 0 && this.source.read(this.buffer, 8192L) == -1) {
                throw new EOFException();
            }
            long toSkip = Math.min(byteCount, this.buffer.size());
            this.buffer.skip(toSkip);
            byteCount -= toSkip;
        }
    }

    @Override
    public long indexOf(byte b) throws IOException {
        return indexOf(b, 0L);
    }

    @Override
    public long indexOf(byte b, long fromIndex) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        while (fromIndex >= this.buffer.size) {
            if (this.source.read(this.buffer, 8192L) == -1) {
                return -1L;
            }
        }
        do {
            long index = this.buffer.indexOf(b, fromIndex);
            if (index == -1) {
                fromIndex = this.buffer.size;
            } else {
                return index;
            }
        } while (this.source.read(this.buffer, 8192L) != -1);
        return -1L;
    }

    @Override
    public long indexOf(ByteString bytes) throws IOException {
        return indexOf(bytes, 0L);
    }

    @Override
    public long indexOf(ByteString bytes, long fromIndex) throws IOException {
        if (bytes.size() == 0) {
            throw new IllegalArgumentException("bytes is empty");
        }
        while (true) {
            long fromIndex2 = indexOf(bytes.getByte(0), fromIndex);
            if (fromIndex2 == -1) {
                return -1L;
            }
            if (rangeEquals(fromIndex2, bytes)) {
                return fromIndex2;
            }
            fromIndex = fromIndex2 + 1;
        }
    }

    @Override
    public long indexOfElement(ByteString targetBytes) throws IOException {
        return indexOfElement(targetBytes, 0L);
    }

    @Override
    public long indexOfElement(ByteString targetBytes, long fromIndex) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        while (fromIndex >= this.buffer.size) {
            if (this.source.read(this.buffer, 8192L) == -1) {
                return -1L;
            }
        }
        do {
            long index = this.buffer.indexOfElement(targetBytes, fromIndex);
            if (index == -1) {
                fromIndex = this.buffer.size;
            } else {
                return index;
            }
        } while (this.source.read(this.buffer, 8192L) != -1);
        return -1L;
    }

    private boolean rangeEquals(long offset, ByteString bytes) throws IOException {
        if (request(((long) bytes.size()) + offset)) {
            return this.buffer.rangeEquals(offset, bytes);
        }
        return false;
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
                    long count = RealBufferedSource.this.source.read(RealBufferedSource.this.buffer, 8192L);
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
                    long count = RealBufferedSource.this.source.read(RealBufferedSource.this.buffer, 8192L);
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
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.source.close();
        this.buffer.clear();
    }

    @Override
    public Timeout timeout() {
        return this.source.timeout();
    }

    public String toString() {
        return "buffer(" + this.source + ")";
    }
}
