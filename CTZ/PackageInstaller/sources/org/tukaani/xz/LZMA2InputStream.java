package org.tukaani.xz;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.lz.LZDecoder;
import org.tukaani.xz.lzma.LZMADecoder;
import org.tukaani.xz.rangecoder.RangeDecoderFromBuffer;

public class LZMA2InputStream extends InputStream {
    private boolean endReached;
    private IOException exception;
    private DataInputStream in;
    private boolean isLZMAChunk;
    private final LZDecoder lz;
    private LZMADecoder lzma;
    private boolean needDictReset;
    private boolean needProps;
    private final RangeDecoderFromBuffer rc;
    private final byte[] tempBuf;
    private int uncompressedSize;

    public static int getMemoryUsage(int i) {
        return 104 + (getDictSize(i) / 1024);
    }

    private static int getDictSize(int i) {
        if (i < 4096 || i > 2147483632) {
            throw new IllegalArgumentException("Unsupported dictionary size " + i);
        }
        return (i + 15) & (-16);
    }

    public LZMA2InputStream(InputStream inputStream, int i) {
        this(inputStream, i, null);
    }

    public LZMA2InputStream(InputStream inputStream, int i, byte[] bArr) {
        this.rc = new RangeDecoderFromBuffer(65536);
        this.uncompressedSize = 0;
        this.needDictReset = true;
        this.needProps = true;
        this.endReached = false;
        this.exception = null;
        this.tempBuf = new byte[1];
        if (inputStream == null) {
            throw new NullPointerException();
        }
        this.in = new DataInputStream(inputStream);
        this.lz = new LZDecoder(getDictSize(i), bArr);
        if (bArr != null && bArr.length > 0) {
            this.needDictReset = false;
        }
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
                if (this.uncompressedSize == 0) {
                    decodeChunkHeader();
                    if (this.endReached) {
                        if (i4 == 0) {
                            return -1;
                        }
                        return i4;
                    }
                }
                int iMin = Math.min(this.uncompressedSize, i2);
                if (!this.isLZMAChunk) {
                    this.lz.copyUncompressed(this.in, iMin);
                } else {
                    this.lz.setLimit(iMin);
                    this.lzma.decode();
                    if (!this.rc.isInBufferOK()) {
                        throw new CorruptedInputException();
                    }
                }
                int iFlush = this.lz.flush(bArr, i);
                i += iFlush;
                i2 -= iFlush;
                i4 += iFlush;
                this.uncompressedSize -= iFlush;
                if (this.uncompressedSize == 0 && (!this.rc.isFinished() || this.lz.hasPending())) {
                    throw new CorruptedInputException();
                }
            } catch (IOException e) {
                this.exception = e;
                throw e;
            }
        }
        return i4;
    }

    private void decodeChunkHeader() throws IOException {
        int unsignedByte = this.in.readUnsignedByte();
        if (unsignedByte == 0) {
            this.endReached = true;
            return;
        }
        if (unsignedByte >= 224 || unsignedByte == 1) {
            this.needProps = true;
            this.needDictReset = false;
            this.lz.reset();
        } else if (this.needDictReset) {
            throw new CorruptedInputException();
        }
        if (unsignedByte >= 128) {
            this.isLZMAChunk = true;
            this.uncompressedSize = (unsignedByte & 31) << 16;
            this.uncompressedSize += this.in.readUnsignedShort() + 1;
            int unsignedShort = this.in.readUnsignedShort() + 1;
            if (unsignedByte >= 192) {
                this.needProps = false;
                decodeProps();
            } else {
                if (this.needProps) {
                    throw new CorruptedInputException();
                }
                if (unsignedByte >= 160) {
                    this.lzma.reset();
                }
            }
            this.rc.prepareInputBuffer(this.in, unsignedShort);
            return;
        }
        if (unsignedByte > 2) {
            throw new CorruptedInputException();
        }
        this.isLZMAChunk = false;
        this.uncompressedSize = this.in.readUnsignedShort() + 1;
    }

    private void decodeProps() throws IOException {
        int unsignedByte = this.in.readUnsignedByte();
        if (unsignedByte > 224) {
            throw new CorruptedInputException();
        }
        int i = unsignedByte / 45;
        int i2 = unsignedByte - ((i * 9) * 5);
        int i3 = i2 / 9;
        int i4 = i2 - (i3 * 9);
        if (i4 + i3 > 4) {
            throw new CorruptedInputException();
        }
        this.lzma = new LZMADecoder(this.lz, this.rc, i4, i3, i);
    }

    @Override
    public int available() throws IOException {
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        if (this.exception != null) {
            throw this.exception;
        }
        return this.uncompressedSize;
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
