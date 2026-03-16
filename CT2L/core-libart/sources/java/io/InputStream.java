package java.io;

import java.util.Arrays;
import libcore.io.Streams;

public abstract class InputStream implements Closeable {
    public abstract int read() throws IOException;

    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
    }

    public void mark(int readlimit) {
    }

    public boolean markSupported() {
        return false;
    }

    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        for (int i = 0; i < byteCount; i++) {
            try {
                int c = read();
                if (c == -1) {
                    if (i == 0) {
                        return -1;
                    }
                    return i;
                }
                buffer[byteOffset + i] = (byte) c;
            } catch (IOException e) {
                if (i == 0) {
                    throw e;
                }
                return i;
            }
        }
        return byteCount;
    }

    public synchronized void reset() throws IOException {
        throw new IOException();
    }

    public long skip(long byteCount) throws IOException {
        return Streams.skipByReading(this, byteCount);
    }
}
