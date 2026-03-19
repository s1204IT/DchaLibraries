package org.tukaani.xz;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.lz.LZDecoder;
import org.tukaani.xz.lzma.LZMADecoder;
import org.tukaani.xz.rangecoder.RangeDecoderFromStream;

public class LZMAInputStream extends InputStream {
    static final boolean $assertionsDisabled = false;
    private boolean endReached;
    private IOException exception;
    private InputStream in;
    private LZDecoder lz;
    private LZMADecoder lzma;
    private RangeDecoderFromStream rc;
    private long remainingSize;
    private final byte[] tempBuf;

    public static int getMemoryUsage(int i, byte b) throws CorruptedInputException, UnsupportedOptionsException {
        if (i < 0 || i > 2147483632) {
            throw new UnsupportedOptionsException("LZMA dictionary is too big for this implementation");
        }
        int i2 = b & 255;
        if (i2 > 224) {
            throw new CorruptedInputException("Invalid LZMA properties byte");
        }
        int i3 = i2 % 45;
        int i4 = i3 / 9;
        return getMemoryUsage(i, i3 - (i4 * 9), i4);
    }

    public static int getMemoryUsage(int i, int i2, int i3) {
        if (i2 < 0 || i2 > 8 || i3 < 0 || i3 > 4) {
            throw new IllegalArgumentException("Invalid lc or lp");
        }
        return 10 + (getDictSize(i) / 1024) + ((1536 << (i2 + i3)) / 1024);
    }

    private static int getDictSize(int i) {
        if (i < 0 || i > 2147483632) {
            throw new IllegalArgumentException("LZMA dictionary is too big for this implementation");
        }
        if (i < 4096) {
            i = 4096;
        }
        return (i + 15) & (-16);
    }

    public LZMAInputStream(InputStream inputStream) throws IOException {
        this(inputStream, -1);
    }

    public LZMAInputStream(InputStream inputStream, int i) throws IOException {
        this.endReached = false;
        this.tempBuf = new byte[1];
        this.exception = null;
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        byte b = dataInputStream.readByte();
        int unsignedByte = 0;
        for (int i2 = 0; i2 < 4; i2++) {
            unsignedByte |= dataInputStream.readUnsignedByte() << (8 * i2);
        }
        long unsignedByte2 = 0;
        for (int i3 = 0; i3 < 8; i3++) {
            unsignedByte2 |= ((long) dataInputStream.readUnsignedByte()) << (8 * i3);
        }
        int memoryUsage = getMemoryUsage(unsignedByte, b);
        if (i != -1 && memoryUsage > i) {
            throw new MemoryLimitException(memoryUsage, i);
        }
        initialize(inputStream, unsignedByte2, b, unsignedByte, null);
    }

    private void initialize(InputStream inputStream, long j, byte b, int i, byte[] bArr) throws IOException {
        if (j < -1) {
            throw new UnsupportedOptionsException("Uncompressed size is too big");
        }
        int i2 = b & 255;
        if (i2 > 224) {
            throw new CorruptedInputException("Invalid LZMA properties byte");
        }
        int i3 = i2 / 45;
        int i4 = i2 - ((i3 * 9) * 5);
        int i5 = i4 / 9;
        int i6 = i4 - (i5 * 9);
        if (i < 0 || i > 2147483632) {
            throw new UnsupportedOptionsException("LZMA dictionary is too big for this implementation");
        }
        initialize(inputStream, j, i6, i5, i3, i, bArr);
    }

    private void initialize(InputStream inputStream, long j, int i, int i2, int i3, int i4, byte[] bArr) throws IOException {
        if (j < -1 || i < 0 || i > 8 || i2 < 0 || i2 > 4 || i3 < 0 || i3 > 4) {
            throw new IllegalArgumentException();
        }
        this.in = inputStream;
        int dictSize = getDictSize(i4);
        if (j >= 0 && dictSize > j) {
            dictSize = getDictSize((int) j);
        }
        this.lz = new LZDecoder(getDictSize(dictSize), bArr);
        this.rc = new RangeDecoderFromStream(inputStream);
        this.lzma = new LZMADecoder(this.lz, this.rc, i, i2, i3);
        this.remainingSize = j;
    }

    @Override
    public int read() throws IOException {
        if (read(this.tempBuf, 0, 1) == -1) {
            return -1;
        }
        return this.tempBuf[0] & 255;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        int i4;
        if (i < 0 || i2 < 0 || (i3 = i + i2) < 0 || i3 > bArr.length) {
            throw new IndexOutOfBoundsException();
        }
        int i5 = 0;
        if (i2 == 0) {
            return 0;
        }
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        if (this.endReached) {
            return -1;
        }
        while (i2 > 0) {
            try {
                if (this.remainingSize >= 0 && this.remainingSize < i2) {
                    i4 = (int) this.remainingSize;
                } else {
                    i4 = i2;
                }
                this.lz.setLimit(i4);
                try {
                    this.lzma.decode();
                } catch (CorruptedInputException e) {
                    if (this.remainingSize != -1 || !this.lzma.endMarkerDetected()) {
                        throw e;
                    }
                    this.endReached = true;
                    this.rc.normalize();
                }
                int iFlush = this.lz.flush(bArr, i);
                i += iFlush;
                i2 -= iFlush;
                i5 += iFlush;
                if (this.remainingSize >= 0) {
                    this.remainingSize -= (long) iFlush;
                    if (this.remainingSize == 0) {
                        this.endReached = true;
                    }
                }
                if (this.endReached) {
                    if (!this.rc.isFinished() || this.lz.hasPending()) {
                        throw new CorruptedInputException();
                    }
                    if (i5 == 0) {
                        return -1;
                    }
                    return i5;
                }
            } catch (IOException e2) {
                this.exception = e2;
                throw e2;
            }
        }
        return i5;
    }

    @Override
    public void close() throws IOException {
        if (this.in != null) {
            try {
                this.in.close();
            } finally {
                this.in = null;
            }
        }
    }
}
