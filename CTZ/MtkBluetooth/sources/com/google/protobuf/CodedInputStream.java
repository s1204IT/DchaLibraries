package com.google.protobuf;

import com.google.protobuf.MessageLite;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public final class CodedInputStream {
    private static final int BUFFER_SIZE = 4096;
    private static final int DEFAULT_RECURSION_LIMIT = 100;
    private static final int DEFAULT_SIZE_LIMIT = 67108864;
    private final byte[] buffer;
    private final boolean bufferIsImmutable;
    private int bufferPos;
    private int bufferSize;
    private int bufferSizeAfterLimit;
    private int currentLimit;
    private boolean enableAliasing;
    private final InputStream input;
    private int lastTag;
    private int recursionDepth;
    private int recursionLimit;
    private RefillCallback refillCallback;
    private int sizeLimit;
    private int totalBytesRetired;

    private interface RefillCallback {
        void onRefill();
    }

    public static CodedInputStream newInstance(InputStream inputStream) {
        return new CodedInputStream(inputStream, 4096);
    }

    static CodedInputStream newInstance(InputStream inputStream, int i) {
        return new CodedInputStream(inputStream, i);
    }

    public static CodedInputStream newInstance(byte[] bArr) {
        return newInstance(bArr, 0, bArr.length);
    }

    public static CodedInputStream newInstance(byte[] bArr, int i, int i2) {
        return newInstance(bArr, i, i2, false);
    }

    static CodedInputStream newInstance(byte[] bArr, int i, int i2, boolean z) {
        CodedInputStream codedInputStream = new CodedInputStream(bArr, i, i2, z);
        try {
            codedInputStream.pushLimit(i2);
            return codedInputStream;
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static CodedInputStream newInstance(ByteBuffer byteBuffer) {
        if (byteBuffer.hasArray()) {
            return newInstance(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining());
        }
        ByteBuffer byteBufferDuplicate = byteBuffer.duplicate();
        byte[] bArr = new byte[byteBufferDuplicate.remaining()];
        byteBufferDuplicate.get(bArr);
        return newInstance(bArr);
    }

    public int readTag() throws IOException {
        if (isAtEnd()) {
            this.lastTag = 0;
            return 0;
        }
        this.lastTag = readRawVarint32();
        if (WireFormat.getTagFieldNumber(this.lastTag) == 0) {
            throw InvalidProtocolBufferException.invalidTag();
        }
        return this.lastTag;
    }

    public void checkLastTagWas(int i) throws InvalidProtocolBufferException {
        if (this.lastTag != i) {
            throw InvalidProtocolBufferException.invalidEndTag();
        }
    }

    public int getLastTag() {
        return this.lastTag;
    }

    public boolean skipField(int i) throws IOException {
        switch (WireFormat.getTagWireType(i)) {
            case 0:
                skipRawVarint();
                return true;
            case 1:
                skipRawBytes(8);
                return true;
            case 2:
                skipRawBytes(readRawVarint32());
                return true;
            case 3:
                skipMessage();
                checkLastTagWas(WireFormat.makeTag(WireFormat.getTagFieldNumber(i), 4));
                return true;
            case 4:
                return false;
            case 5:
                skipRawBytes(4);
                return true;
            default:
                throw InvalidProtocolBufferException.invalidWireType();
        }
    }

    public boolean skipField(int i, CodedOutputStream codedOutputStream) throws IOException {
        switch (WireFormat.getTagWireType(i)) {
            case 0:
                long int64 = readInt64();
                codedOutputStream.writeRawVarint32(i);
                codedOutputStream.writeUInt64NoTag(int64);
                return true;
            case 1:
                long rawLittleEndian64 = readRawLittleEndian64();
                codedOutputStream.writeRawVarint32(i);
                codedOutputStream.writeFixed64NoTag(rawLittleEndian64);
                return true;
            case 2:
                ByteString bytes = readBytes();
                codedOutputStream.writeRawVarint32(i);
                codedOutputStream.writeBytesNoTag(bytes);
                return true;
            case 3:
                codedOutputStream.writeRawVarint32(i);
                skipMessage(codedOutputStream);
                int iMakeTag = WireFormat.makeTag(WireFormat.getTagFieldNumber(i), 4);
                checkLastTagWas(iMakeTag);
                codedOutputStream.writeRawVarint32(iMakeTag);
                return true;
            case 4:
                return false;
            case 5:
                int rawLittleEndian32 = readRawLittleEndian32();
                codedOutputStream.writeRawVarint32(i);
                codedOutputStream.writeFixed32NoTag(rawLittleEndian32);
                return true;
            default:
                throw InvalidProtocolBufferException.invalidWireType();
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

    public void skipMessage(CodedOutputStream codedOutputStream) throws IOException {
        int tag;
        do {
            tag = readTag();
            if (tag == 0) {
                return;
            }
        } while (skipField(tag, codedOutputStream));
    }

    private class SkippedDataSink implements RefillCallback {
        private ByteArrayOutputStream byteArrayStream;
        private int lastPos;

        private SkippedDataSink() {
            this.lastPos = CodedInputStream.this.bufferPos;
        }

        @Override
        public void onRefill() {
            if (this.byteArrayStream == null) {
                this.byteArrayStream = new ByteArrayOutputStream();
            }
            this.byteArrayStream.write(CodedInputStream.this.buffer, this.lastPos, CodedInputStream.this.bufferPos - this.lastPos);
            this.lastPos = 0;
        }

        ByteBuffer getSkippedData() {
            if (this.byteArrayStream == null) {
                return ByteBuffer.wrap(CodedInputStream.this.buffer, this.lastPos, CodedInputStream.this.bufferPos - this.lastPos);
            }
            this.byteArrayStream.write(CodedInputStream.this.buffer, this.lastPos, CodedInputStream.this.bufferPos);
            return ByteBuffer.wrap(this.byteArrayStream.toByteArray());
        }
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
        return readRawVarint64() != 0;
    }

    public String readString() throws IOException {
        int rawVarint32 = readRawVarint32();
        if (rawVarint32 <= this.bufferSize - this.bufferPos && rawVarint32 > 0) {
            String str = new String(this.buffer, this.bufferPos, rawVarint32, Internal.UTF_8);
            this.bufferPos += rawVarint32;
            return str;
        }
        if (rawVarint32 == 0) {
            return "";
        }
        if (rawVarint32 <= this.bufferSize) {
            refillBuffer(rawVarint32);
            String str2 = new String(this.buffer, this.bufferPos, rawVarint32, Internal.UTF_8);
            this.bufferPos += rawVarint32;
            return str2;
        }
        return new String(readRawBytesSlowPath(rawVarint32), Internal.UTF_8);
    }

    public String readStringRequireUtf8() throws IOException {
        byte[] rawBytesSlowPath;
        int rawVarint32 = readRawVarint32();
        int i = this.bufferPos;
        int i2 = 0;
        if (rawVarint32 <= this.bufferSize - i && rawVarint32 > 0) {
            rawBytesSlowPath = this.buffer;
            this.bufferPos = i + rawVarint32;
            i2 = i;
        } else {
            if (rawVarint32 == 0) {
                return "";
            }
            if (rawVarint32 <= this.bufferSize) {
                refillBuffer(rawVarint32);
                rawBytesSlowPath = this.buffer;
                this.bufferPos = 0 + rawVarint32;
            } else {
                rawBytesSlowPath = readRawBytesSlowPath(rawVarint32);
            }
        }
        if (!Utf8.isValidUtf8(rawBytesSlowPath, i2, i2 + rawVarint32)) {
            throw InvalidProtocolBufferException.invalidUtf8();
        }
        return new String(rawBytesSlowPath, i2, rawVarint32, Internal.UTF_8);
    }

    public void readGroup(int i, MessageLite.Builder builder, ExtensionRegistryLite extensionRegistryLite) throws IOException {
        if (this.recursionDepth >= this.recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        this.recursionDepth++;
        builder.mergeFrom(this, extensionRegistryLite);
        checkLastTagWas(WireFormat.makeTag(i, 4));
        this.recursionDepth--;
    }

    public <T extends MessageLite> T readGroup(int i, Parser<T> parser, ExtensionRegistryLite extensionRegistryLite) throws IOException {
        if (this.recursionDepth >= this.recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        this.recursionDepth++;
        T partialFrom = parser.parsePartialFrom(this, extensionRegistryLite);
        checkLastTagWas(WireFormat.makeTag(i, 4));
        this.recursionDepth--;
        return partialFrom;
    }

    @Deprecated
    public void readUnknownGroup(int i, MessageLite.Builder builder) throws IOException {
        readGroup(i, builder, (ExtensionRegistryLite) null);
    }

    public void readMessage(MessageLite.Builder builder, ExtensionRegistryLite extensionRegistryLite) throws IOException {
        int rawVarint32 = readRawVarint32();
        if (this.recursionDepth >= this.recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        int iPushLimit = pushLimit(rawVarint32);
        this.recursionDepth++;
        builder.mergeFrom(this, extensionRegistryLite);
        checkLastTagWas(0);
        this.recursionDepth--;
        popLimit(iPushLimit);
    }

    public <T extends MessageLite> T readMessage(Parser<T> parser, ExtensionRegistryLite extensionRegistryLite) throws IOException {
        int rawVarint32 = readRawVarint32();
        if (this.recursionDepth >= this.recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        int iPushLimit = pushLimit(rawVarint32);
        this.recursionDepth++;
        T partialFrom = parser.parsePartialFrom(this, extensionRegistryLite);
        checkLastTagWas(0);
        this.recursionDepth--;
        popLimit(iPushLimit);
        return partialFrom;
    }

    public ByteString readBytes() throws IOException {
        ByteString byteStringCopyFrom;
        int rawVarint32 = readRawVarint32();
        if (rawVarint32 <= this.bufferSize - this.bufferPos && rawVarint32 > 0) {
            if (this.bufferIsImmutable && this.enableAliasing) {
                byteStringCopyFrom = ByteString.wrap(this.buffer, this.bufferPos, rawVarint32);
            } else {
                byteStringCopyFrom = ByteString.copyFrom(this.buffer, this.bufferPos, rawVarint32);
            }
            this.bufferPos += rawVarint32;
            return byteStringCopyFrom;
        }
        if (rawVarint32 == 0) {
            return ByteString.EMPTY;
        }
        return ByteString.wrap(readRawBytesSlowPath(rawVarint32));
    }

    public byte[] readByteArray() throws IOException {
        int rawVarint32 = readRawVarint32();
        if (rawVarint32 <= this.bufferSize - this.bufferPos && rawVarint32 > 0) {
            byte[] bArrCopyOfRange = Arrays.copyOfRange(this.buffer, this.bufferPos, this.bufferPos + rawVarint32);
            this.bufferPos += rawVarint32;
            return bArrCopyOfRange;
        }
        return readRawBytesSlowPath(rawVarint32);
    }

    public ByteBuffer readByteBuffer() throws IOException {
        ByteBuffer byteBufferWrap;
        int rawVarint32 = readRawVarint32();
        if (rawVarint32 <= this.bufferSize - this.bufferPos && rawVarint32 > 0) {
            if (this.input == null && !this.bufferIsImmutable && this.enableAliasing) {
                byteBufferWrap = ByteBuffer.wrap(this.buffer, this.bufferPos, rawVarint32).slice();
            } else {
                byteBufferWrap = ByteBuffer.wrap(Arrays.copyOfRange(this.buffer, this.bufferPos, this.bufferPos + rawVarint32));
            }
            this.bufferPos += rawVarint32;
            return byteBufferWrap;
        }
        if (rawVarint32 == 0) {
            return Internal.EMPTY_BYTE_BUFFER;
        }
        return ByteBuffer.wrap(readRawBytesSlowPath(rawVarint32));
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
        int i;
        int i2 = this.bufferPos;
        if (this.bufferSize != i2) {
            byte[] bArr = this.buffer;
            int i3 = i2 + 1;
            byte b = bArr[i2];
            if (b >= 0) {
                this.bufferPos = i3;
                return b;
            }
            if (this.bufferSize - i3 >= 9) {
                int i4 = i3 + 1;
                int i5 = b ^ (bArr[i3] << 7);
                if (i5 < 0) {
                    i = i5 ^ (-128);
                } else {
                    int i6 = i4 + 1;
                    int i7 = i5 ^ (bArr[i4] << 14);
                    if (i7 >= 0) {
                        i = i7 ^ 16256;
                    } else {
                        i4 = i6 + 1;
                        int i8 = i7 ^ (bArr[i6] << 21);
                        if (i8 < 0) {
                            i = i8 ^ (-2080896);
                        } else {
                            i6 = i4 + 1;
                            byte b2 = bArr[i4];
                            i = (i8 ^ (b2 << 28)) ^ 266354560;
                            if (b2 < 0) {
                                i4 = i6 + 1;
                                if (bArr[i6] < 0) {
                                    i6 = i4 + 1;
                                    if (bArr[i4] < 0) {
                                        i4 = i6 + 1;
                                        if (bArr[i6] < 0) {
                                            i6 = i4 + 1;
                                            if (bArr[i4] < 0) {
                                                i4 = i6 + 1;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    i4 = i6;
                }
                this.bufferPos = i4;
                return i;
            }
        }
        return (int) readRawVarint64SlowPath();
    }

    private void skipRawVarint() throws IOException {
        if (this.bufferSize - this.bufferPos >= 10) {
            byte[] bArr = this.buffer;
            int i = this.bufferPos;
            int i2 = 0;
            while (i2 < 10) {
                int i3 = i + 1;
                if (bArr[i] < 0) {
                    i2++;
                    i = i3;
                } else {
                    this.bufferPos = i3;
                    return;
                }
            }
        }
        skipRawVarintSlowPath();
    }

    private void skipRawVarintSlowPath() throws IOException {
        for (int i = 0; i < 10; i++) {
            if (readRawByte() >= 0) {
                return;
            }
        }
        throw InvalidProtocolBufferException.malformedVarint();
    }

    static int readRawVarint32(InputStream inputStream) throws IOException {
        int i = inputStream.read();
        if (i == -1) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return readRawVarint32(i, inputStream);
    }

    public static int readRawVarint32(int i, InputStream inputStream) throws IOException {
        if ((i & 128) == 0) {
            return i;
        }
        int i2 = i & 127;
        int i3 = 7;
        while (i3 < 32) {
            int i4 = inputStream.read();
            if (i4 == -1) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            i2 |= (i4 & 127) << i3;
            if ((i4 & 128) != 0) {
                i3 += 7;
            } else {
                return i2;
            }
        }
        while (i3 < 64) {
            int i5 = inputStream.read();
            if (i5 == -1) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            if ((i5 & 128) != 0) {
                i3 += 7;
            } else {
                return i2;
            }
        }
        throw InvalidProtocolBufferException.malformedVarint();
    }

    public long readRawVarint64() throws IOException {
        int i;
        long j;
        long j2;
        long j3;
        int i2 = this.bufferPos;
        if (this.bufferSize != i2) {
            byte[] bArr = this.buffer;
            int i3 = i2 + 1;
            byte b = bArr[i2];
            if (b >= 0) {
                this.bufferPos = i3;
                return b;
            }
            if (this.bufferSize - i3 >= 9) {
                int i4 = i3 + 1;
                int i5 = b ^ (bArr[i3] << 7);
                if (i5 >= 0) {
                    int i6 = i4 + 1;
                    int i7 = i5 ^ (bArr[i4] << 14);
                    if (i7 >= 0) {
                        long j4 = i7 ^ 16256;
                        i = i6;
                        j = j4;
                    } else {
                        i4 = i6 + 1;
                        int i8 = i7 ^ (bArr[i6] << 21);
                        if (i8 < 0) {
                            j3 = i8 ^ (-2080896);
                        } else {
                            long j5 = i8;
                            i = i4 + 1;
                            long j6 = (((long) bArr[i4]) << 28) ^ j5;
                            if (j6 >= 0) {
                                j = j6 ^ 266354560;
                            } else {
                                int i9 = i + 1;
                                long j7 = j6 ^ (((long) bArr[i]) << 35);
                                if (j7 < 0) {
                                    j2 = (-34093383808L) ^ j7;
                                } else {
                                    i = i9 + 1;
                                    long j8 = j7 ^ (((long) bArr[i9]) << 42);
                                    if (j8 >= 0) {
                                        j = j8 ^ 4363953127296L;
                                    } else {
                                        i9 = i + 1;
                                        long j9 = j8 ^ (((long) bArr[i]) << 49);
                                        if (j9 < 0) {
                                            j2 = (-558586000294016L) ^ j9;
                                        } else {
                                            i = i9 + 1;
                                            long j10 = (j9 ^ (((long) bArr[i9]) << 56)) ^ 71499008037633920L;
                                            if (j10 < 0) {
                                                i9 = i + 1;
                                                if (bArr[i] >= 0) {
                                                    j = j10;
                                                    i = i9;
                                                }
                                            } else {
                                                j = j10;
                                            }
                                        }
                                    }
                                }
                                j = j2;
                                i = i9;
                            }
                        }
                    }
                    this.bufferPos = i;
                    return j;
                }
                j3 = i5 ^ (-128);
                j = j3;
                i = i4;
                this.bufferPos = i;
                return j;
            }
        }
        return readRawVarint64SlowPath();
    }

    long readRawVarint64SlowPath() throws IOException {
        long j = 0;
        for (int i = 0; i < 64; i += 7) {
            byte rawByte = readRawByte();
            j |= ((long) (rawByte & 127)) << i;
            if ((rawByte & 128) == 0) {
                return j;
            }
        }
        throw InvalidProtocolBufferException.malformedVarint();
    }

    public int readRawLittleEndian32() throws IOException {
        int i = this.bufferPos;
        if (this.bufferSize - i < 4) {
            refillBuffer(4);
            i = this.bufferPos;
        }
        byte[] bArr = this.buffer;
        this.bufferPos = i + 4;
        return ((bArr[i + 3] & 255) << 24) | (bArr[i] & 255) | ((bArr[i + 1] & 255) << 8) | ((bArr[i + 2] & 255) << 16);
    }

    public long readRawLittleEndian64() throws IOException {
        int i = this.bufferPos;
        if (this.bufferSize - i < 8) {
            refillBuffer(8);
            i = this.bufferPos;
        }
        byte[] bArr = this.buffer;
        this.bufferPos = i + 8;
        return ((((long) bArr[i + 7]) & 255) << 56) | (((long) bArr[i]) & 255) | ((((long) bArr[i + 1]) & 255) << 8) | ((((long) bArr[i + 2]) & 255) << 16) | ((((long) bArr[i + 3]) & 255) << 24) | ((((long) bArr[i + 4]) & 255) << 32) | ((((long) bArr[i + 5]) & 255) << 40) | ((((long) bArr[i + 6]) & 255) << 48);
    }

    public static int decodeZigZag32(int i) {
        return (-(i & 1)) ^ (i >>> 1);
    }

    public static long decodeZigZag64(long j) {
        return (-(j & 1)) ^ (j >>> 1);
    }

    private CodedInputStream(byte[] bArr, int i, int i2, boolean z) {
        this.enableAliasing = false;
        this.currentLimit = Integer.MAX_VALUE;
        this.recursionLimit = 100;
        this.sizeLimit = 67108864;
        this.refillCallback = null;
        this.buffer = bArr;
        this.bufferSize = i2 + i;
        this.bufferPos = i;
        this.totalBytesRetired = -i;
        this.input = null;
        this.bufferIsImmutable = z;
    }

    private CodedInputStream(InputStream inputStream, int i) {
        this.enableAliasing = false;
        this.currentLimit = Integer.MAX_VALUE;
        this.recursionLimit = 100;
        this.sizeLimit = 67108864;
        this.refillCallback = null;
        this.buffer = new byte[i];
        this.bufferPos = 0;
        this.totalBytesRetired = 0;
        this.input = inputStream;
        this.bufferIsImmutable = false;
    }

    public void enableAliasing(boolean z) {
        this.enableAliasing = z;
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
        this.totalBytesRetired = -this.bufferPos;
    }

    public int pushLimit(int i) throws InvalidProtocolBufferException {
        if (i < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        }
        int i2 = i + this.totalBytesRetired + this.bufferPos;
        int i3 = this.currentLimit;
        if (i2 > i3) {
            throw InvalidProtocolBufferException.truncatedMessage();
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
        return this.bufferPos == this.bufferSize && !tryRefillBuffer(1);
    }

    public int getTotalBytesRead() {
        return this.totalBytesRetired + this.bufferPos;
    }

    private void refillBuffer(int i) throws IOException {
        if (!tryRefillBuffer(i)) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
    }

    private boolean tryRefillBuffer(int i) throws IOException {
        if (this.bufferPos + i <= this.bufferSize) {
            throw new IllegalStateException("refillBuffer() called when " + i + " bytes were already available in buffer");
        }
        if (this.totalBytesRetired + this.bufferPos + i > this.currentLimit) {
            return false;
        }
        if (this.refillCallback != null) {
            this.refillCallback.onRefill();
        }
        if (this.input != null) {
            int i2 = this.bufferPos;
            if (i2 > 0) {
                if (this.bufferSize > i2) {
                    System.arraycopy(this.buffer, i2, this.buffer, 0, this.bufferSize - i2);
                }
                this.totalBytesRetired += i2;
                this.bufferSize -= i2;
                this.bufferPos = 0;
            }
            int i3 = this.input.read(this.buffer, this.bufferSize, this.buffer.length - this.bufferSize);
            if (i3 == 0 || i3 < -1 || i3 > this.buffer.length) {
                throw new IllegalStateException("InputStream#read(byte[]) returned invalid result: " + i3 + "\nThe InputStream implementation is buggy.");
            }
            if (i3 > 0) {
                this.bufferSize += i3;
                if ((this.totalBytesRetired + i) - this.sizeLimit > 0) {
                    throw InvalidProtocolBufferException.sizeLimitExceeded();
                }
                recomputeBufferSizeAfterLimit();
                if (this.bufferSize >= i) {
                    return true;
                }
                return tryRefillBuffer(i);
            }
        }
        return false;
    }

    public byte readRawByte() throws IOException {
        if (this.bufferPos == this.bufferSize) {
            refillBuffer(1);
        }
        byte[] bArr = this.buffer;
        int i = this.bufferPos;
        this.bufferPos = i + 1;
        return bArr[i];
    }

    public byte[] readRawBytes(int i) throws IOException {
        int i2 = this.bufferPos;
        if (i <= this.bufferSize - i2 && i > 0) {
            int i3 = i + i2;
            this.bufferPos = i3;
            return Arrays.copyOfRange(this.buffer, i2, i3);
        }
        return readRawBytesSlowPath(i);
    }

    private byte[] readRawBytesSlowPath(int i) throws IOException {
        if (i <= 0) {
            if (i == 0) {
                return Internal.EMPTY_BYTE_ARRAY;
            }
            throw InvalidProtocolBufferException.negativeSize();
        }
        int i2 = this.totalBytesRetired + this.bufferPos + i;
        if (i2 > this.sizeLimit) {
            throw InvalidProtocolBufferException.sizeLimitExceeded();
        }
        if (i2 > this.currentLimit) {
            skipRawBytes((this.currentLimit - this.totalBytesRetired) - this.bufferPos);
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        if (this.input == null) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        int i3 = this.bufferPos;
        int length = this.bufferSize - this.bufferPos;
        this.totalBytesRetired += this.bufferSize;
        this.bufferPos = 0;
        this.bufferSize = 0;
        int length2 = i - length;
        if (length2 < 4096 || length2 <= this.input.available()) {
            byte[] bArr = new byte[i];
            System.arraycopy(this.buffer, i3, bArr, 0, length);
            while (length < bArr.length) {
                int i4 = this.input.read(bArr, length, i - length);
                if (i4 == -1) {
                    throw InvalidProtocolBufferException.truncatedMessage();
                }
                this.totalBytesRetired += i4;
                length += i4;
            }
            return bArr;
        }
        ArrayList<byte[]> arrayList = new ArrayList();
        while (length2 > 0) {
            byte[] bArr2 = new byte[Math.min(length2, 4096)];
            int i5 = 0;
            while (i5 < bArr2.length) {
                int i6 = this.input.read(bArr2, i5, bArr2.length - i5);
                if (i6 == -1) {
                    throw InvalidProtocolBufferException.truncatedMessage();
                }
                this.totalBytesRetired += i6;
                i5 += i6;
            }
            length2 -= bArr2.length;
            arrayList.add(bArr2);
        }
        byte[] bArr3 = new byte[i];
        System.arraycopy(this.buffer, i3, bArr3, 0, length);
        for (byte[] bArr4 : arrayList) {
            System.arraycopy(bArr4, 0, bArr3, length, bArr4.length);
            length += bArr4.length;
        }
        return bArr3;
    }

    public void skipRawBytes(int i) throws IOException {
        if (i <= this.bufferSize - this.bufferPos && i >= 0) {
            this.bufferPos += i;
        } else {
            skipRawBytesSlowPath(i);
        }
    }

    private void skipRawBytesSlowPath(int i) throws IOException {
        if (i < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        }
        if (this.totalBytesRetired + this.bufferPos + i > this.currentLimit) {
            skipRawBytes((this.currentLimit - this.totalBytesRetired) - this.bufferPos);
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        int i2 = this.bufferSize - this.bufferPos;
        this.bufferPos = this.bufferSize;
        refillBuffer(1);
        while (true) {
            int i3 = i - i2;
            if (i3 > this.bufferSize) {
                i2 += this.bufferSize;
                this.bufferPos = this.bufferSize;
                refillBuffer(1);
            } else {
                this.bufferPos = i3;
                return;
            }
        }
    }
}
