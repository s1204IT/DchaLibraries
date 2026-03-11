package com.google.protobuf.nano;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

public final class CodedOutputByteBufferNano {
    private final ByteBuffer buffer;

    private CodedOutputByteBufferNano(byte[] bArr, int i, int i2) {
        this(ByteBuffer.wrap(bArr, i, i2));
    }

    private CodedOutputByteBufferNano(ByteBuffer byteBuffer) {
        this.buffer = byteBuffer;
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public static CodedOutputByteBufferNano newInstance(byte[] bArr) {
        return newInstance(bArr, 0, bArr.length);
    }

    public static CodedOutputByteBufferNano newInstance(byte[] bArr, int i, int i2) {
        return new CodedOutputByteBufferNano(bArr, i, i2);
    }

    public void writeFloat(int i, float f) throws IOException {
        writeTag(i, 5);
        writeFloatNoTag(f);
    }

    public void writeUInt64(int i, long j) throws IOException {
        writeTag(i, 0);
        writeUInt64NoTag(j);
    }

    public void writeInt32(int i, int i2) throws IOException {
        writeTag(i, 0);
        writeInt32NoTag(i2);
    }

    public void writeBool(int i, boolean z) throws IOException {
        writeTag(i, 0);
        writeBoolNoTag(z);
    }

    public void writeString(int i, String str) throws IOException {
        writeTag(i, 2);
        writeStringNoTag(str);
    }

    public void writeMessage(int i, MessageNano messageNano) throws IOException {
        writeTag(i, 2);
        writeMessageNoTag(messageNano);
    }

    public void writeFloatNoTag(float f) throws IOException {
        writeRawLittleEndian32(Float.floatToIntBits(f));
    }

    public void writeUInt64NoTag(long j) throws IOException {
        writeRawVarint64(j);
    }

    public void writeInt32NoTag(int i) throws IOException {
        if (i >= 0) {
            writeRawVarint32(i);
        } else {
            writeRawVarint64(i);
        }
    }

    public void writeBoolNoTag(boolean z) throws IOException {
        writeRawByte(z ? 1 : 0);
    }

    public void writeStringNoTag(String str) throws IOException {
        try {
            int iComputeRawVarint32Size = computeRawVarint32Size(str.length());
            if (iComputeRawVarint32Size == computeRawVarint32Size(str.length() * 3)) {
                int iPosition = this.buffer.position();
                if (this.buffer.remaining() < iComputeRawVarint32Size) {
                    throw new OutOfSpaceException(iPosition + iComputeRawVarint32Size, this.buffer.limit());
                }
                this.buffer.position(iPosition + iComputeRawVarint32Size);
                encode(str, this.buffer);
                int iPosition2 = this.buffer.position();
                this.buffer.position(iPosition);
                writeRawVarint32((iPosition2 - iPosition) - iComputeRawVarint32Size);
                this.buffer.position(iPosition2);
                return;
            }
            writeRawVarint32(encodedLength(str));
            encode(str, this.buffer);
        } catch (BufferOverflowException e) {
            OutOfSpaceException outOfSpaceException = new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

    private static int encodedLength(CharSequence charSequence) {
        int length = charSequence.length();
        int i = 0;
        while (i < length && charSequence.charAt(i) < 128) {
            i++;
        }
        int iEncodedLengthGeneral = length;
        while (true) {
            if (i < length) {
                char cCharAt = charSequence.charAt(i);
                if (cCharAt < 2048) {
                    iEncodedLengthGeneral += (127 - cCharAt) >>> 31;
                    i++;
                } else {
                    iEncodedLengthGeneral += encodedLengthGeneral(charSequence, i);
                    break;
                }
            } else {
                break;
            }
        }
        if (iEncodedLengthGeneral < length) {
            throw new IllegalArgumentException("UTF-8 length does not fit in int: " + (((long) iEncodedLengthGeneral) + 4294967296L));
        }
        return iEncodedLengthGeneral;
    }

    private static int encodedLengthGeneral(CharSequence charSequence, int i) {
        int length = charSequence.length();
        int i2 = 0;
        while (i < length) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt < 2048) {
                i2 += (127 - cCharAt) >>> 31;
            } else {
                i2 += 2;
                if (55296 <= cCharAt && cCharAt <= 57343) {
                    if (Character.codePointAt(charSequence, i) < 65536) {
                        throw new IllegalArgumentException("Unpaired surrogate at index " + i);
                    }
                    i++;
                }
            }
            i++;
        }
        return i2;
    }

    private static void encode(CharSequence charSequence, ByteBuffer byteBuffer) {
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (byteBuffer.hasArray()) {
            try {
                byteBuffer.position(encode(charSequence, byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining()) - byteBuffer.arrayOffset());
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                BufferOverflowException bufferOverflowException = new BufferOverflowException();
                bufferOverflowException.initCause(e);
                throw bufferOverflowException;
            }
        }
        encodeDirect(charSequence, byteBuffer);
    }

    private static void encodeDirect(CharSequence charSequence, ByteBuffer byteBuffer) {
        int length = charSequence.length();
        int i = 0;
        while (i < length) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt < 128) {
                byteBuffer.put((byte) cCharAt);
            } else if (cCharAt < 2048) {
                byteBuffer.put((byte) (960 | (cCharAt >>> 6)));
                byteBuffer.put((byte) ((cCharAt & '?') | 128));
            } else if (cCharAt < 55296 || 57343 < cCharAt) {
                byteBuffer.put((byte) (480 | (cCharAt >>> '\f')));
                byteBuffer.put((byte) (((cCharAt >>> 6) & 63) | 128));
                byteBuffer.put((byte) ((cCharAt & '?') | 128));
            } else {
                int i2 = i + 1;
                if (i2 != charSequence.length()) {
                    char cCharAt2 = charSequence.charAt(i2);
                    if (!Character.isSurrogatePair(cCharAt, cCharAt2)) {
                        i = i2;
                    } else {
                        int codePoint = Character.toCodePoint(cCharAt, cCharAt2);
                        byteBuffer.put((byte) (240 | (codePoint >>> 18)));
                        byteBuffer.put((byte) (((codePoint >>> 12) & 63) | 128));
                        byteBuffer.put((byte) (((codePoint >>> 6) & 63) | 128));
                        byteBuffer.put((byte) ((codePoint & 63) | 128));
                        i = i2;
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Unpaired surrogate at index ");
                sb.append(i - 1);
                throw new IllegalArgumentException(sb.toString());
            }
            i++;
        }
    }

    private static int encode(CharSequence charSequence, byte[] bArr, int i, int i2) {
        int i3;
        int i4;
        char cCharAt;
        int length = charSequence.length();
        int i5 = i2 + i;
        int i6 = 0;
        while (i6 < length && (i4 = i6 + i) < i5 && (cCharAt = charSequence.charAt(i6)) < 128) {
            bArr[i4] = (byte) cCharAt;
            i6++;
        }
        if (i6 == length) {
            return i + length;
        }
        int i7 = i + i6;
        while (i6 < length) {
            char cCharAt2 = charSequence.charAt(i6);
            if (cCharAt2 < 128 && i7 < i5) {
                i3 = i7 + 1;
                bArr[i7] = (byte) cCharAt2;
            } else {
                if (cCharAt2 < 2048 && i7 <= i5 - 2) {
                    int i8 = i7 + 1;
                    bArr[i7] = (byte) (960 | (cCharAt2 >>> 6));
                    i7 = i8 + 1;
                    bArr[i8] = (byte) ((cCharAt2 & '?') | 128);
                } else if ((cCharAt2 < 55296 || 57343 < cCharAt2) && i7 <= i5 - 3) {
                    int i9 = i7 + 1;
                    bArr[i7] = (byte) (480 | (cCharAt2 >>> '\f'));
                    int i10 = i9 + 1;
                    bArr[i9] = (byte) (((cCharAt2 >>> 6) & 63) | 128);
                    i3 = i10 + 1;
                    bArr[i10] = (byte) ((cCharAt2 & '?') | 128);
                } else {
                    if (i7 <= i5 - 4) {
                        int i11 = i6 + 1;
                        if (i11 != charSequence.length()) {
                            char cCharAt3 = charSequence.charAt(i11);
                            if (!Character.isSurrogatePair(cCharAt2, cCharAt3)) {
                                i6 = i11;
                            } else {
                                int codePoint = Character.toCodePoint(cCharAt2, cCharAt3);
                                int i12 = i7 + 1;
                                bArr[i7] = (byte) (240 | (codePoint >>> 18));
                                int i13 = i12 + 1;
                                bArr[i12] = (byte) (((codePoint >>> 12) & 63) | 128);
                                int i14 = i13 + 1;
                                bArr[i13] = (byte) (((codePoint >>> 6) & 63) | 128);
                                i7 = i14 + 1;
                                bArr[i14] = (byte) ((codePoint & 63) | 128);
                                i6 = i11;
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Unpaired surrogate at index ");
                        sb.append(i6 - 1);
                        throw new IllegalArgumentException(sb.toString());
                    }
                    throw new ArrayIndexOutOfBoundsException("Failed writing " + cCharAt2 + " at index " + i7);
                }
                i6++;
            }
            i7 = i3;
            i6++;
        }
        return i7;
    }

    public void writeGroupNoTag(MessageNano messageNano) throws IOException {
        messageNano.writeTo(this);
    }

    public void writeMessageNoTag(MessageNano messageNano) throws IOException {
        writeRawVarint32(messageNano.getCachedSize());
        messageNano.writeTo(this);
    }

    public static int computeFloatSize(int i, float f) {
        return computeTagSize(i) + computeFloatSizeNoTag(f);
    }

    public static int computeUInt64Size(int i, long j) {
        return computeTagSize(i) + computeUInt64SizeNoTag(j);
    }

    public static int computeInt32Size(int i, int i2) {
        return computeTagSize(i) + computeInt32SizeNoTag(i2);
    }

    public static int computeBoolSize(int i, boolean z) {
        return computeTagSize(i) + computeBoolSizeNoTag(z);
    }

    public static int computeStringSize(int i, String str) {
        return computeTagSize(i) + computeStringSizeNoTag(str);
    }

    public static int computeGroupSize(int i, MessageNano messageNano) {
        return (computeTagSize(i) * 2) + computeGroupSizeNoTag(messageNano);
    }

    public static int computeMessageSize(int i, MessageNano messageNano) {
        return computeTagSize(i) + computeMessageSizeNoTag(messageNano);
    }

    public static int computeFloatSizeNoTag(float f) {
        return 4;
    }

    public static int computeUInt64SizeNoTag(long j) {
        return computeRawVarint64Size(j);
    }

    public static int computeInt32SizeNoTag(int i) {
        if (i >= 0) {
            return computeRawVarint32Size(i);
        }
        return 10;
    }

    public static int computeBoolSizeNoTag(boolean z) {
        return 1;
    }

    public static int computeStringSizeNoTag(String str) {
        int iEncodedLength = encodedLength(str);
        return computeRawVarint32Size(iEncodedLength) + iEncodedLength;
    }

    public static int computeGroupSizeNoTag(MessageNano messageNano) {
        return messageNano.getSerializedSize();
    }

    public static int computeMessageSizeNoTag(MessageNano messageNano) {
        int serializedSize = messageNano.getSerializedSize();
        return computeRawVarint32Size(serializedSize) + serializedSize;
    }

    public int spaceLeft() {
        return this.buffer.remaining();
    }

    public void checkNoSpaceLeft() {
        if (spaceLeft() != 0) {
            throw new IllegalStateException("Did not write as much data as expected.");
        }
    }

    public static class OutOfSpaceException extends IOException {
        private static final long serialVersionUID = -6947486886997889499L;

        OutOfSpaceException(int i, int i2) {
            super("CodedOutputStream was writing to a flat byte array and ran out of space (pos " + i + " limit " + i2 + ").");
        }
    }

    public void writeRawByte(byte b) throws IOException {
        if (!this.buffer.hasRemaining()) {
            throw new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.put(b);
    }

    public void writeRawByte(int i) throws IOException {
        writeRawByte((byte) i);
    }

    public void writeRawBytes(byte[] bArr) throws IOException {
        writeRawBytes(bArr, 0, bArr.length);
    }

    public void writeRawBytes(byte[] bArr, int i, int i2) throws IOException {
        if (this.buffer.remaining() >= i2) {
            this.buffer.put(bArr, i, i2);
            return;
        }
        throw new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
    }

    public void writeTag(int i, int i2) throws IOException {
        writeRawVarint32(WireFormatNano.makeTag(i, i2));
    }

    public static int computeTagSize(int i) {
        return computeRawVarint32Size(WireFormatNano.makeTag(i, 0));
    }

    public void writeRawVarint32(int i) throws IOException {
        while ((i & (-128)) != 0) {
            writeRawByte((i & 127) | 128);
            i >>>= 7;
        }
        writeRawByte(i);
    }

    public static int computeRawVarint32Size(int i) {
        if ((i & (-128)) == 0) {
            return 1;
        }
        if ((i & (-16384)) == 0) {
            return 2;
        }
        if (((-2097152) & i) == 0) {
            return 3;
        }
        return (i & (-268435456)) == 0 ? 4 : 5;
    }

    public void writeRawVarint64(long j) throws IOException {
        while (((-128) & j) != 0) {
            writeRawByte((((int) j) & 127) | 128);
            j >>>= 7;
        }
        writeRawByte((int) j);
    }

    public static int computeRawVarint64Size(long j) {
        if (((-128) & j) == 0) {
            return 1;
        }
        if (((-16384) & j) == 0) {
            return 2;
        }
        if (((-2097152) & j) == 0) {
            return 3;
        }
        if (((-268435456) & j) == 0) {
            return 4;
        }
        if (((-34359738368L) & j) == 0) {
            return 5;
        }
        if (((-4398046511104L) & j) == 0) {
            return 6;
        }
        if (((-562949953421312L) & j) == 0) {
            return 7;
        }
        if (((-72057594037927936L) & j) == 0) {
            return 8;
        }
        return (j & Long.MIN_VALUE) == 0 ? 9 : 10;
    }

    public void writeRawLittleEndian32(int i) throws IOException {
        if (this.buffer.remaining() < 4) {
            throw new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.putInt(i);
    }
}
