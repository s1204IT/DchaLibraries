package org.tukaani.xz;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.check.Check;
import org.tukaani.xz.common.DecoderUtil;
import org.tukaani.xz.common.StreamFlags;
import org.tukaani.xz.index.IndexHash;

public class SingleXZInputStream extends InputStream {
    private BlockInputStream blockDecoder;
    private final Check check;
    private boolean endReached;
    private IOException exception;
    private InputStream in;
    private final IndexHash indexHash;
    private final int memoryLimit;
    private final StreamFlags streamHeaderFlags;
    private final byte[] tempBuf;
    private final boolean verifyCheck;

    private static byte[] readStreamHeader(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[12];
        new DataInputStream(inputStream).readFully(bArr);
        return bArr;
    }

    public SingleXZInputStream(InputStream inputStream, int i, boolean z) throws IOException {
        this(inputStream, i, z, readStreamHeader(inputStream));
    }

    SingleXZInputStream(InputStream inputStream, int i, boolean z, byte[] bArr) throws IOException {
        this.blockDecoder = null;
        this.indexHash = new IndexHash();
        this.endReached = false;
        this.exception = null;
        this.tempBuf = new byte[1];
        this.in = inputStream;
        this.memoryLimit = i;
        this.verifyCheck = z;
        this.streamHeaderFlags = DecoderUtil.decodeStreamHeader(bArr);
        this.check = Check.getInstance(this.streamHeaderFlags.checkType);
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
        if (i < 0 || i2 < 0 || (i3 = i + i2) < 0 || i3 > bArr.length) {
            throw new IndexOutOfBoundsException();
        }
        int i4 = 0;
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
                if (this.blockDecoder == null) {
                    try {
                        this.blockDecoder = new BlockInputStream(this.in, this.check, this.verifyCheck, this.memoryLimit, -1L, -1L);
                    } catch (IndexIndicatorException e) {
                        this.indexHash.validate(this.in);
                        validateStreamFooter();
                        this.endReached = true;
                        if (i4 > 0) {
                            return i4;
                        }
                        return -1;
                    }
                }
                int i5 = this.blockDecoder.read(bArr, i, i2);
                if (i5 > 0) {
                    i4 += i5;
                    i += i5;
                    i2 -= i5;
                } else if (i5 == -1) {
                    this.indexHash.add(this.blockDecoder.getUnpaddedSize(), this.blockDecoder.getUncompressedSize());
                    this.blockDecoder = null;
                }
            } catch (IOException e2) {
                this.exception = e2;
                if (i4 == 0) {
                    throw e2;
                }
            }
        }
        return i4;
    }

    private void validateStreamFooter() throws IOException {
        byte[] bArr = new byte[12];
        new DataInputStream(this.in).readFully(bArr);
        StreamFlags streamFlagsDecodeStreamFooter = DecoderUtil.decodeStreamFooter(bArr);
        if (!DecoderUtil.areStreamFlagsEqual(this.streamHeaderFlags, streamFlagsDecodeStreamFooter) || this.indexHash.getIndexSize() != streamFlagsDecodeStreamFooter.backwardSize) {
            throw new CorruptedInputException("XZ Stream Footer does not match Stream Header");
        }
    }

    @Override
    public int available() throws IOException {
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        if (this.blockDecoder == null) {
            return 0;
        }
        return this.blockDecoder.available();
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
