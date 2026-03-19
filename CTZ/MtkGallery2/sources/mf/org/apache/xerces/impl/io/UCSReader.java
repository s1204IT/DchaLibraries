package mf.org.apache.xerces.impl.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public final class UCSReader extends Reader {
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static final short UCS2BE = 2;
    public static final short UCS2LE = 1;
    public static final short UCS4BE = 8;
    public static final short UCS4LE = 4;
    protected final byte[] fBuffer;
    protected final short fEncoding;
    protected final InputStream fInputStream;

    public UCSReader(InputStream inputStream, short encoding) {
        this(inputStream, 8192, encoding);
    }

    public UCSReader(InputStream inputStream, int size, short encoding) {
        this(inputStream, new byte[size], encoding);
    }

    public UCSReader(InputStream inputStream, byte[] buffer, short encoding) {
        this.fInputStream = inputStream;
        this.fBuffer = buffer;
        this.fEncoding = encoding;
    }

    @Override
    public int read() throws IOException {
        int b1;
        int b3;
        int b0 = this.fInputStream.read() & 255;
        if (b0 == 255 || (b1 = this.fInputStream.read() & 255) == 255) {
            return -1;
        }
        if (this.fEncoding >= 4) {
            int b2 = this.fInputStream.read() & 255;
            if (b2 == 255 || (b3 = this.fInputStream.read() & 255) == 255) {
                return -1;
            }
            if (this.fEncoding == 8) {
                return (b0 << 24) + (b1 << 16) + (b2 << 8) + b3;
            }
            return (b3 << 24) + (b2 << 16) + (b1 << 8) + b0;
        }
        if (this.fEncoding == 2) {
            return (b0 << 8) + b1;
        }
        return (b1 << 8) + b0;
    }

    @Override
    public int read(char[] ch, int offset, int length) throws IOException {
        short s = 4;
        int byteLength = length << (this.fEncoding >= 4 ? 2 : 1);
        if (byteLength > this.fBuffer.length) {
            byteLength = this.fBuffer.length;
        }
        int count = this.fInputStream.read(this.fBuffer, 0, byteLength);
        if (count == -1) {
            return -1;
        }
        if (this.fEncoding >= 4) {
            int numToRead = (4 - (count & 3)) & 3;
            int i = 0;
            while (true) {
                if (i >= numToRead) {
                    break;
                }
                int charRead = this.fInputStream.read();
                if (charRead == -1) {
                    for (int j = i; j < numToRead; j++) {
                        this.fBuffer[count + j] = 0;
                    }
                } else {
                    this.fBuffer[count + i] = (byte) charRead;
                    i++;
                }
            }
            count += numToRead;
        } else {
            int numToRead2 = count & 1;
            if (numToRead2 != 0) {
                count++;
                int charRead2 = this.fInputStream.read();
                if (charRead2 == -1) {
                    this.fBuffer[count] = 0;
                } else {
                    this.fBuffer[count] = (byte) charRead2;
                }
            }
        }
        int numToRead3 = this.fEncoding;
        int numChars = count >> (numToRead3 >= 4 ? 2 : 1);
        int b0 = 0;
        int i2 = 0;
        while (i2 < numChars) {
            int curPos = b0 + 1;
            int b02 = this.fBuffer[b0] & 255;
            int curPos2 = curPos + 1;
            int b1 = this.fBuffer[curPos] & 255;
            if (this.fEncoding >= s) {
                int curPos3 = curPos2 + 1;
                int b2 = this.fBuffer[curPos2] & 255;
                int curPos4 = curPos3 + 1;
                int b3 = this.fBuffer[curPos3] & 255;
                if (this.fEncoding == 8) {
                    ch[offset + i2] = (char) ((b02 << 24) + (b1 << 16) + (b2 << 8) + b3);
                } else {
                    ch[offset + i2] = (char) ((b3 << 24) + (b2 << 16) + (b1 << 8) + b02);
                }
                b0 = curPos4;
            } else {
                if (this.fEncoding == 2) {
                    ch[offset + i2] = (char) ((b02 << 8) + b1);
                } else {
                    ch[offset + i2] = (char) ((b1 << 8) + b02);
                }
                b0 = curPos2;
            }
            i2++;
            s = 4;
        }
        return numChars;
    }

    @Override
    public long skip(long n) throws IOException {
        int charWidth = this.fEncoding >= 4 ? 2 : 1;
        long bytesSkipped = this.fInputStream.skip(n << charWidth);
        return (((long) (charWidth | 1)) & bytesSkipped) == 0 ? bytesSkipped >> charWidth : (bytesSkipped >> charWidth) + 1;
    }

    @Override
    public boolean ready() throws IOException {
        return false;
    }

    @Override
    public boolean markSupported() {
        return this.fInputStream.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        this.fInputStream.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        this.fInputStream.reset();
    }

    @Override
    public void close() throws IOException {
        this.fInputStream.close();
    }
}
