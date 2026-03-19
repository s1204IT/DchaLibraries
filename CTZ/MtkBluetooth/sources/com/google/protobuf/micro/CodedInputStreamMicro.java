package com.google.protobuf.micro;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public final class CodedInputStreamMicro {
    private static final int BUFFER_SIZE = 4096;
    private static final int DEFAULT_RECURSION_LIMIT = 64;
    private static final int DEFAULT_SIZE_LIMIT = 67108864;
    private final byte[] buffer;
    private int bufferPos;
    private int bufferSize;
    private int bufferSizeAfterLimit;
    private int currentLimit;
    private final InputStream input;
    private int lastTag;
    private int recursionDepth;
    private int recursionLimit;
    private int sizeLimit;
    private int totalBytesRetired;

    public static CodedInputStreamMicro newInstance(InputStream inputStream) {
        return new CodedInputStreamMicro(inputStream);
    }

    public static CodedInputStreamMicro newInstance(byte[] bArr) {
        return newInstance(bArr, 0, bArr.length);
    }

    public static CodedInputStreamMicro newInstance(byte[] bArr, int i, int i2) {
        return new CodedInputStreamMicro(bArr, i, i2);
    }

    public int readTag() throws IOException {
        if (isAtEnd()) {
            this.lastTag = 0;
            return 0;
        }
        this.lastTag = readRawVarint32();
        if (this.lastTag == 0) {
            throw InvalidProtocolBufferMicroException.invalidTag();
        }
        return this.lastTag;
    }

    public void checkLastTagWas(int i) throws InvalidProtocolBufferMicroException {
        if (this.lastTag != i) {
            throw InvalidProtocolBufferMicroException.invalidEndTag();
        }
    }

    public boolean skipField(int i) throws IOException {
        switch (WireFormatMicro.getTagWireType(i)) {
            case 0:
                readInt32();
                return true;
            case 1:
                readRawLittleEndian64();
                return true;
            case 2:
                skipRawBytes(readRawVarint32());
                return true;
            case 3:
                skipMessage();
                checkLastTagWas(WireFormatMicro.makeTag(WireFormatMicro.getTagFieldNumber(i), 4));
                return true;
            case 4:
                return false;
            case 5:
                readRawLittleEndian32();
                return true;
            default:
                throw InvalidProtocolBufferMicroException.invalidWireType();
        }
    }

    public void skipMessage() throws IOException {
        int tag;
        do {
            tag = readTag();
            if (tag == 0) {
                return;
            }
        } while (skipField(tag));
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readRawLittleEndian64());
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readRawLittleEndian32());
    }

    public long readUInt64() throws IOException {
        return readRawVarint64();
    }

    public long readInt64() throws IOException {
        return readRawVarint64();
    }

    public int readInt32() throws IOException {
        return readRawVarint32();
    }

    public long readFixed64() throws IOException {
        return readRawLittleEndian64();
    }

    public int readFixed32() throws IOException {
        return readRawLittleEndian32();
    }

    public boolean readBool() throws IOException {
        return readRawVarint32() != 0;
    }

    public String readString() throws IOException {
        int rawVarint32 = readRawVarint32();
        if (rawVarint32 <= this.bufferSize - this.bufferPos && rawVarint32 > 0) {
            String str = new String(this.buffer, this.bufferPos, rawVarint32, "UTF-8");
            this.bufferPos += rawVarint32;
            return str;
        }
        return new String(readRawBytes(rawVarint32), "UTF-8");
    }

    public void readGroup(MessageMicro messageMicro, int i) throws IOException {
        if (this.recursionDepth >= this.recursionLimit) {
            throw InvalidProtocolBufferMicroException.recursionLimitExceeded();
        }
        this.recursionDepth++;
        messageMicro.mergeFrom(this);
        checkLastTagWas(WireFormatMicro.makeTag(i, 4));
        this.recursionDepth--;
    }

    public void readMessage(MessageMicro messageMicro) throws IOException {
        int rawVarint32 = readRawVarint32();
        if (this.recursionDepth >= this.recursionLimit) {
            throw InvalidProtocolBufferMicroException.recursionLimitExceeded();
        }
        int iPushLimit = pushLimit(rawVarint32);
        this.recursionDepth++;
        messageMicro.mergeFrom(this);
        checkLastTagWas(0);
        this.recursionDepth--;
        popLimit(iPushLimit);
    }

    public ByteStringMicro readBytes() throws IOException {
        int rawVarint32 = readRawVarint32();
        if (rawVarint32 <= this.bufferSize - this.bufferPos && rawVarint32 > 0) {
            ByteStringMicro byteStringMicroCopyFrom = ByteStringMicro.copyFrom(this.buffer, this.bufferPos, rawVarint32);
            this.bufferPos += rawVarint32;
            return byteStringMicroCopyFrom;
        }
        if (rawVarint32 == 0) {
            return ByteStringMicro.EMPTY;
        }
        return ByteStringMicro.copyFrom(readRawBytes(rawVarint32));
    }

    public int readUInt32() throws IOException {
        return readRawVarint32();
    }

    public int readEnum() throws IOException {
        return readRawVarint32();
    }

    public int readSFixed32() throws IOException {
        return readRawLittleEndian32();
    }

    public long readSFixed64() throws IOException {
        return readRawLittleEndian64();
    }

    public int readSInt32() throws IOException {
        return decodeZigZag32(readRawVarint32());
    }

    public long readSInt64() throws IOException {
        return decodeZigZag64(readRawVarint64());
    }

    public int readRawVarint32() throws IOException {
        byte rawByte = readRawByte();
        if (rawByte >= 0) {
            return rawByte;
        }
        int i = rawByte & 127;
        byte rawByte2 = readRawByte();
        if (rawByte2 >= 0) {
            return i | (rawByte2 << 7);
        }
        int i2 = i | ((rawByte2 & 127) << 7);
        byte rawByte3 = readRawByte();
        if (rawByte3 >= 0) {
            return i2 | (rawByte3 << 14);
        }
        int i3 = i2 | ((rawByte3 & 127) << 14);
        byte rawByte4 = readRawByte();
        if (rawByte4 >= 0) {
            return i3 | (rawByte4 << 21);
        }
        int i4 = i3 | ((rawByte4 & 127) << 21);
        byte rawByte5 = readRawByte();
        int i5 = i4 | (rawByte5 << 28);
        if (rawByte5 < 0) {
            for (int i6 = 0; i6 < 5; i6++) {
                if (readRawByte() >= 0) {
                    return i5;
                }
            }
            throw InvalidProtocolBufferMicroException.malformedVarint();
        }
        return i5;
    }

    static int readRawVarint32(InputStream inputStream) throws IOException {
        int i = 0;
        int i2 = 0;
        while (i < 32) {
            int i3 = inputStream.read();
            if (i3 == -1) {
                throw InvalidProtocolBufferMicroException.truncatedMessage();
            }
            i2 |= (i3 & 127) << i;
            if ((i3 & 128) != 0) {
                i += 7;
            } else {
                return i2;
            }
        }
        while (i < 64) {
            int i4 = inputStream.read();
            if (i4 == -1) {
                throw InvalidProtocolBufferMicroException.truncatedMessage();
            }
            if ((i4 & 128) != 0) {
                i += 7;
            } else {
                return i2;
            }
        }
        throw InvalidProtocolBufferMicroException.malformedVarint();
    }

    public long readRawVarint64() throws IOException {
        long j = 0;
        for (int i = 0; i < 64; i += 7) {
            byte rawByte = readRawByte();
            j |= ((long) (rawByte & 127)) << i;
            if ((rawByte & 128) == 0) {
                return j;
            }
        }
        throw InvalidProtocolBufferMicroException.malformedVarint();
    }

    public int readRawLittleEndian32() throws IOException {
        return (readRawByte() & 255) | ((readRawByte() & 255) << 8) | ((readRawByte() & 255) << 16) | ((readRawByte() & 255) << 24);
    }

    public long readRawLittleEndian64() throws IOException {
        byte rawByte = readRawByte();
        return ((((long) readRawByte()) & 255) << 8) | (((long) rawByte) & 255) | ((((long) readRawByte()) & 255) << 16) | ((((long) readRawByte()) & 255) << 24) | ((((long) readRawByte()) & 255) << 32) | ((((long) readRawByte()) & 255) << 40) | ((((long) readRawByte()) & 255) << 48) | ((((long) readRawByte()) & 255) << 56);
    }

    public static int decodeZigZag32(int i) {
        return (-(i & 1)) ^ (i >>> 1);
    }

    public static long decodeZigZag64(long j) {
        return (-(j & 1)) ^ (j >>> 1);
    }

    private CodedInputStreamMicro(byte[] bArr, int i, int i2) {
        this.currentLimit = Integer.MAX_VALUE;
        this.recursionLimit = 64;
        this.sizeLimit = 67108864;
        this.buffer = bArr;
        this.bufferSize = i2 + i;
        this.bufferPos = i;
        this.input = null;
    }

    private CodedInputStreamMicro(InputStream inputStream) {
        this.currentLimit = Integer.MAX_VALUE;
        this.recursionLimit = 64;
        this.sizeLimit = 67108864;
        this.buffer = new byte[4096];
        this.bufferSize = 0;
        this.bufferPos = 0;
        this.input = inputStream;
    }

    public int setRecursionLimit(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Recursion limit cannot be negative: " + i);
        }
        int i2 = this.recursionLimit;
        this.recursionLimit = i;
        return i2;
    }

    public int setSizeLimit(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Size limit cannot be negative: " + i);
        }
        int i2 = this.sizeLimit;
        this.sizeLimit = i;
        return i2;
    }

    public void resetSizeCounter() {
        this.totalBytesRetired = 0;
    }

    public int pushLimit(int i) throws InvalidProtocolBufferMicroException {
        if (i < 0) {
            throw InvalidProtocolBufferMicroException.negativeSize();
        }
        int i2 = i + this.totalBytesRetired + this.bufferPos;
        int i3 = this.currentLimit;
        if (i2 > i3) {
            throw InvalidProtocolBufferMicroException.truncatedMessage();
        }
        this.currentLimit = i2;
        recomputeBufferSizeAfterLimit();
        return i3;
    }

    private void recomputeBufferSizeAfterLimit() {
        this.bufferSize += this.bufferSizeAfterLimit;
        int i = this.totalBytesRetired + this.bufferSize;
        if (i > this.currentLimit) {
            this.bufferSizeAfterLimit = i - this.currentLimit;
            this.bufferSize -= this.bufferSizeAfterLimit;
        } else {
            this.bufferSizeAfterLimit = 0;
        }
    }

    public void popLimit(int i) {
        this.currentLimit = i;
        recomputeBufferSizeAfterLimit();
    }

    public int getBytesUntilLimit() {
        if (this.currentLimit == Integer.MAX_VALUE) {
            return -1;
        }
        return this.currentLimit - (this.totalBytesRetired + this.bufferPos);
    }

    public boolean isAtEnd() throws IOException {
        return this.bufferPos == this.bufferSize && !refillBuffer(false);
    }

    private boolean refillBuffer(boolean z) throws IOException {
        if (this.bufferPos < this.bufferSize) {
            throw new IllegalStateException("refillBuffer() called when buffer wasn't empty.");
        }
        if (this.totalBytesRetired + this.bufferSize == this.currentLimit) {
            if (z) {
                throw InvalidProtocolBufferMicroException.truncatedMessage();
            }
            return false;
        }
        this.totalBytesRetired += this.bufferSize;
        this.bufferPos = 0;
        this.bufferSize = this.input == null ? -1 : this.input.read(this.buffer);
        if (this.bufferSize == 0 || this.bufferSize < -1) {
            throw new IllegalStateException("InputStream#read(byte[]) returned invalid result: " + this.bufferSize + "\nThe InputStream implementation is buggy.");
        }
        if (this.bufferSize == -1) {
            this.bufferSize = 0;
            if (z) {
                throw InvalidProtocolBufferMicroException.truncatedMessage();
            }
            return false;
        }
        recomputeBufferSizeAfterLimit();
        int i = this.totalBytesRetired + this.bufferSize + this.bufferSizeAfterLimit;
        if (i > this.sizeLimit || i < 0) {
            throw InvalidProtocolBufferMicroException.sizeLimitExceeded();
        }
        return true;
    }

    public byte readRawByte() throws IOException {
        if (this.bufferPos == this.bufferSize) {
            refillBuffer(true);
        }
        byte[] bArr = this.buffer;
        int i = this.bufferPos;
        this.bufferPos = i + 1;
        return bArr[i];
    }

    public byte[] readRawBytes(int i) throws IOException {
        int i2;
        if (i < 0) {
            throw InvalidProtocolBufferMicroException.negativeSize();
        }
        if (this.totalBytesRetired + this.bufferPos + i > this.currentLimit) {
            skipRawBytes((this.currentLimit - this.totalBytesRetired) - this.bufferPos);
            throw InvalidProtocolBufferMicroException.truncatedMessage();
        }
        if (i <= this.bufferSize - this.bufferPos) {
            byte[] bArr = new byte[i];
            System.arraycopy(this.buffer, this.bufferPos, bArr, 0, i);
            this.bufferPos += i;
            return bArr;
        }
        if (i < 4096) {
            byte[] bArr2 = new byte[i];
            int i3 = this.bufferSize - this.bufferPos;
            System.arraycopy(this.buffer, this.bufferPos, bArr2, 0, i3);
            this.bufferPos = this.bufferSize;
            refillBuffer(true);
            while (true) {
                int i4 = i - i3;
                if (i4 > this.bufferSize) {
                    System.arraycopy(this.buffer, 0, bArr2, i3, this.bufferSize);
                    i3 += this.bufferSize;
                    this.bufferPos = this.bufferSize;
                    refillBuffer(true);
                } else {
                    System.arraycopy(this.buffer, 0, bArr2, i3, i4);
                    this.bufferPos = i4;
                    return bArr2;
                }
            }
        } else {
            int i5 = this.bufferPos;
            int i6 = this.bufferSize;
            this.totalBytesRetired += this.bufferSize;
            this.bufferPos = 0;
            this.bufferSize = 0;
            int length = i6 - i5;
            int length2 = i - length;
            Vector vector = new Vector();
            while (length2 > 0) {
                byte[] bArr3 = new byte[Math.min(length2, 4096)];
                int i7 = 0;
                while (i7 < bArr3.length) {
                    if (this.input != null) {
                        i2 = this.input.read(bArr3, i7, bArr3.length - i7);
                    } else {
                        i2 = -1;
                    }
                    if (i2 == -1) {
                        throw InvalidProtocolBufferMicroException.truncatedMessage();
                    }
                    this.totalBytesRetired += i2;
                    i7 += i2;
                }
                length2 -= bArr3.length;
                vector.addElement(bArr3);
            }
            byte[] bArr4 = new byte[i];
            System.arraycopy(this.buffer, i5, bArr4, 0, length);
            for (int i8 = 0; i8 < vector.size(); i8++) {
                byte[] bArr5 = (byte[]) vector.elementAt(i8);
                System.arraycopy(bArr5, 0, bArr4, length, bArr5.length);
                length += bArr5.length;
            }
            return bArr4;
        }
    }

    public void skipRawBytes(int i) throws IOException {
        if (i < 0) {
            throw InvalidProtocolBufferMicroException.negativeSize();
        }
        if (this.totalBytesRetired + this.bufferPos + i > this.currentLimit) {
            skipRawBytes((this.currentLimit - this.totalBytesRetired) - this.bufferPos);
            throw InvalidProtocolBufferMicroException.truncatedMessage();
        }
        if (i <= this.bufferSize - this.bufferPos) {
            this.bufferPos += i;
            return;
        }
        int i2 = this.bufferSize - this.bufferPos;
        this.totalBytesRetired += this.bufferSize;
        this.bufferPos = 0;
        this.bufferSize = 0;
        while (i2 < i) {
            int iSkip = this.input == null ? -1 : (int) this.input.skip(i - i2);
            if (iSkip <= 0) {
                throw InvalidProtocolBufferMicroException.truncatedMessage();
            }
            i2 += iSkip;
            this.totalBytesRetired += iSkip;
        }
    }
}
