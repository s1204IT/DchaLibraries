package org.tukaani.xz;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.tukaani.xz.check.Check;
import org.tukaani.xz.common.DecoderUtil;

class BlockInputStream extends InputStream {
    private final Check check;
    private long compressedSizeInHeader;
    private long compressedSizeLimit;
    private InputStream filterChain;
    private final int headerSize;
    private final CountingInputStream inCounted;
    private final DataInputStream inData;
    private long uncompressedSizeInHeader;
    private final boolean verifyCheck;
    private long uncompressedSize = 0;
    private boolean endReached = false;
    private final byte[] tempBuf = new byte[1];

    public BlockInputStream(InputStream inputStream, Check check, boolean z, int i, long j, long j2) throws IndexIndicatorException, IOException {
        this.uncompressedSizeInHeader = -1L;
        this.compressedSizeInHeader = -1L;
        this.check = check;
        this.verifyCheck = z;
        this.inData = new DataInputStream(inputStream);
        byte[] bArr = new byte[1024];
        this.inData.readFully(bArr, 0, 1);
        if (bArr[0] != 0) {
            this.headerSize = ((bArr[0] & 255) + 1) * 4;
            this.inData.readFully(bArr, 1, this.headerSize - 1);
            if (!DecoderUtil.isCRC32Valid(bArr, 0, this.headerSize - 4, this.headerSize - 4)) {
                throw new CorruptedInputException("XZ Block Header is corrupt");
            }
            if ((bArr[1] & 60) == 0) {
                int i2 = (bArr[1] & 3) + 1;
                long[] jArr = new long[i2];
                byte[][] bArr2 = new byte[i2][];
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr, 2, this.headerSize - 6);
                try {
                    this.compressedSizeLimit = (9223372036854775804L - ((long) this.headerSize)) - ((long) check.getSize());
                    if ((bArr[1] & 64) != 0) {
                        this.compressedSizeInHeader = DecoderUtil.decodeVLI(byteArrayInputStream);
                        if (this.compressedSizeInHeader == 0 || this.compressedSizeInHeader > this.compressedSizeLimit) {
                            throw new CorruptedInputException();
                        }
                        this.compressedSizeLimit = this.compressedSizeInHeader;
                    }
                    if ((bArr[1] & 128) != 0) {
                        this.uncompressedSizeInHeader = DecoderUtil.decodeVLI(byteArrayInputStream);
                    }
                    int i3 = 0;
                    while (i3 < i2) {
                        jArr[i3] = DecoderUtil.decodeVLI(byteArrayInputStream);
                        long jDecodeVLI = DecoderUtil.decodeVLI(byteArrayInputStream);
                        int i4 = i2;
                        long[] jArr2 = jArr;
                        if (jDecodeVLI > byteArrayInputStream.available()) {
                            throw new CorruptedInputException();
                        }
                        bArr2[i3] = new byte[(int) jDecodeVLI];
                        byteArrayInputStream.read(bArr2[i3]);
                        i3++;
                        i2 = i4;
                        jArr = jArr2;
                    }
                    long[] jArr3 = jArr;
                    for (int iAvailable = byteArrayInputStream.available(); iAvailable > 0; iAvailable--) {
                        if (byteArrayInputStream.read() != 0) {
                            throw new UnsupportedOptionsException("Unsupported options in XZ Block Header");
                        }
                    }
                    if (j != -1) {
                        long size = this.headerSize + check.getSize();
                        if (size >= j) {
                            throw new CorruptedInputException("XZ Index does not match a Block Header");
                        }
                        long j3 = j - size;
                        if (j3 > this.compressedSizeLimit || !(this.compressedSizeInHeader == -1 || this.compressedSizeInHeader == j3)) {
                            throw new CorruptedInputException("XZ Index does not match a Block Header");
                        }
                        if (this.uncompressedSizeInHeader != -1 && this.uncompressedSizeInHeader != j2) {
                            throw new CorruptedInputException("XZ Index does not match a Block Header");
                        }
                        this.compressedSizeLimit = j3;
                        this.compressedSizeInHeader = j3;
                        this.uncompressedSizeInHeader = j2;
                    }
                    FilterDecoder[] filterDecoderArr = new FilterDecoder[jArr3.length];
                    for (int i5 = 0; i5 < filterDecoderArr.length; i5++) {
                        if (jArr3[i5] == 33) {
                            filterDecoderArr[i5] = new LZMA2Decoder(bArr2[i5]);
                        } else if (jArr3[i5] == 3) {
                            filterDecoderArr[i5] = new DeltaDecoder(bArr2[i5]);
                        } else {
                            if (!BCJDecoder.isBCJFilterID(jArr3[i5])) {
                                throw new UnsupportedOptionsException("Unknown Filter ID " + jArr3[i5]);
                            }
                            filterDecoderArr[i5] = new BCJDecoder(jArr3[i5], bArr2[i5]);
                        }
                    }
                    RawCoder.validate(filterDecoderArr);
                    if (i >= 0) {
                        int memoryUsage = 0;
                        for (FilterDecoder filterDecoder : filterDecoderArr) {
                            memoryUsage += filterDecoder.getMemoryUsage();
                        }
                        if (memoryUsage > i) {
                            throw new MemoryLimitException(memoryUsage, i);
                        }
                    }
                    this.inCounted = new CountingInputStream(inputStream);
                    this.filterChain = this.inCounted;
                    for (int length = filterDecoderArr.length - 1; length >= 0; length--) {
                        this.filterChain = filterDecoderArr[length].getInputStream(this.filterChain);
                    }
                    return;
                } catch (IOException e) {
                    throw new CorruptedInputException("XZ Block Header is corrupt");
                }
            }
            throw new UnsupportedOptionsException("Unsupported options in XZ Block Header");
        }
        throw new IndexIndicatorException();
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
        if (this.endReached) {
            return -1;
        }
        int i3 = this.filterChain.read(bArr, i, i2);
        if (i3 > 0) {
            if (this.verifyCheck) {
                this.check.update(bArr, i, i3);
            }
            this.uncompressedSize += (long) i3;
            long size = this.inCounted.getSize();
            if (size < 0 || size > this.compressedSizeLimit || this.uncompressedSize < 0 || (this.uncompressedSizeInHeader != -1 && this.uncompressedSize > this.uncompressedSizeInHeader)) {
                throw new CorruptedInputException();
            }
            if (i3 < i2 || this.uncompressedSize == this.uncompressedSizeInHeader) {
                if (this.filterChain.read() != -1) {
                    throw new CorruptedInputException();
                }
                validate();
                this.endReached = true;
            }
        } else if (i3 == -1) {
            validate();
            this.endReached = true;
        }
        return i3;
    }

    private void validate() throws IOException {
        long size = this.inCounted.getSize();
        if ((this.compressedSizeInHeader != -1 && this.compressedSizeInHeader != size) || (this.uncompressedSizeInHeader != -1 && this.uncompressedSizeInHeader != this.uncompressedSize)) {
            throw new CorruptedInputException();
        }
        while (true) {
            long j = 1 + size;
            if ((size & 3) != 0) {
                if (this.inData.readUnsignedByte() == 0) {
                    size = j;
                } else {
                    throw new CorruptedInputException();
                }
            } else {
                byte[] bArr = new byte[this.check.getSize()];
                this.inData.readFully(bArr);
                if (this.verifyCheck && !Arrays.equals(this.check.finish(), bArr)) {
                    throw new CorruptedInputException("Integrity check (" + this.check.getName() + ") does not match");
                }
                return;
            }
        }
    }

    @Override
    public int available() throws IOException {
        return this.filterChain.available();
    }

    public long getUnpaddedSize() {
        return ((long) this.headerSize) + this.inCounted.getSize() + ((long) this.check.getSize());
    }

    public long getUncompressedSize() {
        return this.uncompressedSize;
    }
}
