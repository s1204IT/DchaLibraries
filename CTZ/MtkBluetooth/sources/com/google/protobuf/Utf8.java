package com.google.protobuf;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Unsafe;

final class Utf8 {
    private static final long ASCII_MASK_LONG = -9187201950435737472L;
    public static final int COMPLETE = 0;
    public static final int MALFORMED = -1;
    static final int MAX_BYTES_PER_CHAR = 3;
    private static final int UNSAFE_COUNT_ASCII_THRESHOLD = 16;
    private static final Logger logger = Logger.getLogger(Utf8.class.getName());
    private static final Processor processor;

    static {
        processor = UnsafeProcessor.isAvailable() ? new UnsafeProcessor() : new SafeProcessor();
    }

    public static boolean isValidUtf8(byte[] bArr) {
        return processor.isValidUtf8(bArr, 0, bArr.length);
    }

    public static boolean isValidUtf8(byte[] bArr, int i, int i2) {
        return processor.isValidUtf8(bArr, i, i2);
    }

    public static int partialIsValidUtf8(int i, byte[] bArr, int i2, int i3) {
        return processor.partialIsValidUtf8(i, bArr, i2, i3);
    }

    private static int incompleteStateFor(int i) {
        if (i <= -12) {
            return i;
        }
        return -1;
    }

    private static int incompleteStateFor(int i, int i2) {
        if (i > -12 || i2 > -65) {
            return -1;
        }
        return i ^ (i2 << 8);
    }

    private static int incompleteStateFor(int i, int i2, int i3) {
        if (i > -12 || i2 > -65 || i3 > -65) {
            return -1;
        }
        return (i ^ (i2 << 8)) ^ (i3 << 16);
    }

    private static int incompleteStateFor(byte[] bArr, int i, int i2) {
        byte b = bArr[i - 1];
        switch (i2 - i) {
            case 0:
                return incompleteStateFor(b);
            case 1:
                return incompleteStateFor(b, bArr[i]);
            case 2:
                return incompleteStateFor(b, bArr[i], bArr[i + 1]);
            default:
                throw new AssertionError();
        }
    }

    private static int incompleteStateFor(ByteBuffer byteBuffer, int i, int i2, int i3) {
        switch (i3) {
            case 0:
                return incompleteStateFor(i);
            case 1:
                return incompleteStateFor(i, byteBuffer.get(i2));
            case 2:
                return incompleteStateFor(i, byteBuffer.get(i2), byteBuffer.get(i2 + 1));
            default:
                throw new AssertionError();
        }
    }

    static class UnpairedSurrogateException extends IllegalArgumentException {
        private UnpairedSurrogateException(int i, int i2) {
            super("Unpaired surrogate at index " + i + " of " + i2);
        }
    }

    static int encodedLength(CharSequence charSequence) {
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
                        throw new UnpairedSurrogateException(i, length);
                    }
                    i++;
                }
            }
            i++;
        }
        return i2;
    }

    static int encode(CharSequence charSequence, byte[] bArr, int i, int i2) {
        return processor.encodeUtf8(charSequence, bArr, i, i2);
    }

    static boolean isValidUtf8(ByteBuffer byteBuffer) {
        return processor.isValidUtf8(byteBuffer, byteBuffer.position(), byteBuffer.remaining());
    }

    static int partialIsValidUtf8(int i, ByteBuffer byteBuffer, int i2, int i3) {
        return processor.partialIsValidUtf8(i, byteBuffer, i2, i3);
    }

    static void encodeUtf8(CharSequence charSequence, ByteBuffer byteBuffer) {
        processor.encodeUtf8(charSequence, byteBuffer);
    }

    private static int estimateConsecutiveAscii(ByteBuffer byteBuffer, int i, int i2) {
        int i3 = i2 - 7;
        int i4 = i;
        while (i4 < i3 && (byteBuffer.getLong(i4) & ASCII_MASK_LONG) == 0) {
            i4 += 8;
        }
        return i4 - i;
    }

    static abstract class Processor {
        abstract int encodeUtf8(CharSequence charSequence, byte[] bArr, int i, int i2);

        abstract void encodeUtf8Direct(CharSequence charSequence, ByteBuffer byteBuffer);

        abstract int partialIsValidUtf8(int i, byte[] bArr, int i2, int i3);

        abstract int partialIsValidUtf8Direct(int i, ByteBuffer byteBuffer, int i2, int i3);

        Processor() {
        }

        final boolean isValidUtf8(byte[] bArr, int i, int i2) {
            return partialIsValidUtf8(0, bArr, i, i2) == 0;
        }

        final boolean isValidUtf8(ByteBuffer byteBuffer, int i, int i2) {
            return partialIsValidUtf8(0, byteBuffer, i, i2) == 0;
        }

        final int partialIsValidUtf8(int i, ByteBuffer byteBuffer, int i2, int i3) {
            if (byteBuffer.hasArray()) {
                int iArrayOffset = byteBuffer.arrayOffset();
                return partialIsValidUtf8(i, byteBuffer.array(), i2 + iArrayOffset, iArrayOffset + i3);
            }
            if (byteBuffer.isDirect()) {
                return partialIsValidUtf8Direct(i, byteBuffer, i2, i3);
            }
            return partialIsValidUtf8Default(i, byteBuffer, i2, i3);
        }

        final int partialIsValidUtf8Default(int i, ByteBuffer byteBuffer, int i2, int i3) {
            int i4;
            int i5;
            if (i != 0) {
                if (i2 >= i3) {
                    return i;
                }
                byte b = (byte) i;
                if (b < -32) {
                    if (b >= -62) {
                        i4 = i2 + 1;
                    }
                    return -1;
                }
                if (b < -16) {
                    byte b2 = (byte) (~(i >> 8));
                    if (b2 == 0) {
                        int i6 = i2 + 1;
                        byte b3 = byteBuffer.get(i2);
                        if (i6 >= i3) {
                            return Utf8.incompleteStateFor(b, b3);
                        }
                        i2 = i6;
                        b2 = b3;
                    }
                    if (b2 <= -65 && ((b != -32 || b2 >= -96) && (b != -19 || b2 < -96))) {
                        i4 = i2 + 1;
                    }
                    return -1;
                }
                byte b4 = (byte) (~(i >> 8));
                byte b5 = 0;
                if (b4 == 0) {
                    i5 = i2 + 1;
                    b4 = byteBuffer.get(i2);
                    if (i5 >= i3) {
                        return Utf8.incompleteStateFor(b, b4);
                    }
                } else {
                    b5 = (byte) (i >> 16);
                    i5 = i2;
                }
                if (b5 == 0) {
                    int i7 = i5 + 1;
                    b5 = byteBuffer.get(i5);
                    if (i7 >= i3) {
                        return Utf8.incompleteStateFor(b, b4, b5);
                    }
                    i5 = i7;
                }
                if (b4 <= -65 && (((b << 28) + (b4 + 112)) >> 30) == 0 && b5 <= -65) {
                    i2 = i5 + 1;
                }
                return -1;
            }
            i4 = i2;
            return partialIsValidUtf8(byteBuffer, i4, i3);
        }

        private static int partialIsValidUtf8(ByteBuffer byteBuffer, int i, int i2) {
            int i3;
            int iEstimateConsecutiveAscii = i + Utf8.estimateConsecutiveAscii(byteBuffer, i, i2);
            while (iEstimateConsecutiveAscii < i2) {
                int i4 = iEstimateConsecutiveAscii + 1;
                byte b = byteBuffer.get(iEstimateConsecutiveAscii);
                if (b >= 0) {
                    iEstimateConsecutiveAscii = i4;
                } else {
                    if (b < -32) {
                        if (i4 >= i2) {
                            return b;
                        }
                        if (b < -62 || byteBuffer.get(i4) > -65) {
                            return -1;
                        }
                        i3 = i4 + 1;
                    } else if (b < -16) {
                        if (i4 >= i2 - 1) {
                            return Utf8.incompleteStateFor(byteBuffer, b, i4, i2 - i4);
                        }
                        int i5 = i4 + 1;
                        byte b2 = byteBuffer.get(i4);
                        if (b2 > -65 || ((b == -32 && b2 < -96) || ((b == -19 && b2 >= -96) || byteBuffer.get(i5) > -65))) {
                            return -1;
                        }
                        iEstimateConsecutiveAscii = i5 + 1;
                    } else {
                        if (i4 >= i2 - 2) {
                            return Utf8.incompleteStateFor(byteBuffer, b, i4, i2 - i4);
                        }
                        int i6 = i4 + 1;
                        byte b3 = byteBuffer.get(i4);
                        if (b3 <= -65 && (((b << 28) + (b3 + 112)) >> 30) == 0) {
                            int i7 = i6 + 1;
                            if (byteBuffer.get(i6) <= -65) {
                                i3 = i7 + 1;
                                if (byteBuffer.get(i7) > -65) {
                                }
                            }
                        }
                        return -1;
                    }
                    iEstimateConsecutiveAscii = i3;
                }
            }
            return 0;
        }

        final void encodeUtf8(CharSequence charSequence, ByteBuffer byteBuffer) {
            if (byteBuffer.hasArray()) {
                int iArrayOffset = byteBuffer.arrayOffset();
                byteBuffer.position(Utf8.encode(charSequence, byteBuffer.array(), byteBuffer.position() + iArrayOffset, byteBuffer.remaining()) - iArrayOffset);
            } else if (byteBuffer.isDirect()) {
                encodeUtf8Direct(charSequence, byteBuffer);
            } else {
                encodeUtf8Default(charSequence, byteBuffer);
            }
        }

        final void encodeUtf8Default(CharSequence charSequence, ByteBuffer byteBuffer) {
            int length = charSequence.length();
            int iPosition = byteBuffer.position();
            int i = 0;
            while (i < length) {
                try {
                    char cCharAt = charSequence.charAt(i);
                    if (cCharAt >= 128) {
                        break;
                    }
                    byteBuffer.put(iPosition + i, (byte) cCharAt);
                    i++;
                } catch (IndexOutOfBoundsException e) {
                    throw new ArrayIndexOutOfBoundsException("Failed writing " + charSequence.charAt(i) + " at index " + (byteBuffer.position() + Math.max(i, (iPosition - byteBuffer.position()) + 1)));
                }
            }
            if (i == length) {
                byteBuffer.position(iPosition + i);
                return;
            }
            iPosition += i;
            while (i < length) {
                char cCharAt2 = charSequence.charAt(i);
                if (cCharAt2 < 128) {
                    byteBuffer.put(iPosition, (byte) cCharAt2);
                } else if (cCharAt2 < 2048) {
                    int i2 = iPosition + 1;
                    try {
                        byteBuffer.put(iPosition, (byte) (192 | (cCharAt2 >>> 6)));
                        byteBuffer.put(i2, (byte) (('?' & cCharAt2) | 128));
                        iPosition = i2;
                    } catch (IndexOutOfBoundsException e2) {
                        iPosition = i2;
                        throw new ArrayIndexOutOfBoundsException("Failed writing " + charSequence.charAt(i) + " at index " + (byteBuffer.position() + Math.max(i, (iPosition - byteBuffer.position()) + 1)));
                    }
                } else {
                    if (cCharAt2 >= 55296 && 57343 >= cCharAt2) {
                        int i3 = i + 1;
                        if (i3 != length) {
                            try {
                                char cCharAt3 = charSequence.charAt(i3);
                                if (Character.isSurrogatePair(cCharAt2, cCharAt3)) {
                                    int codePoint = Character.toCodePoint(cCharAt2, cCharAt3);
                                    int i4 = iPosition + 1;
                                    try {
                                        byteBuffer.put(iPosition, (byte) (240 | (codePoint >>> 18)));
                                        int i5 = i4 + 1;
                                        byteBuffer.put(i4, (byte) (((codePoint >>> 12) & 63) | 128));
                                        int i6 = i5 + 1;
                                        byteBuffer.put(i5, (byte) (((codePoint >>> 6) & 63) | 128));
                                        byteBuffer.put(i6, (byte) ((63 & codePoint) | 128));
                                        iPosition = i6;
                                        i = i3;
                                    } catch (IndexOutOfBoundsException e3) {
                                        iPosition = i4;
                                        i = i3;
                                        throw new ArrayIndexOutOfBoundsException("Failed writing " + charSequence.charAt(i) + " at index " + (byteBuffer.position() + Math.max(i, (iPosition - byteBuffer.position()) + 1)));
                                    }
                                } else {
                                    i = i3;
                                }
                            } catch (IndexOutOfBoundsException e4) {
                            }
                        }
                        throw new UnpairedSurrogateException(i, length);
                    }
                    int i7 = iPosition + 1;
                    byteBuffer.put(iPosition, (byte) (224 | (cCharAt2 >>> '\f')));
                    iPosition = i7 + 1;
                    byteBuffer.put(i7, (byte) (((cCharAt2 >>> 6) & 63) | 128));
                    byteBuffer.put(iPosition, (byte) ((cCharAt2 & '?') | 128));
                }
                i++;
                iPosition++;
            }
            byteBuffer.position(iPosition);
        }
    }

    static final class SafeProcessor extends Processor {
        SafeProcessor() {
        }

        @Override
        int partialIsValidUtf8(int i, byte[] bArr, int i2, int i3) {
            int i4;
            int i5;
            if (i != 0) {
                if (i2 >= i3) {
                    return i;
                }
                byte b = (byte) i;
                if (b < -32) {
                    if (b >= -62) {
                        i4 = i2 + 1;
                    }
                    return -1;
                }
                if (b < -16) {
                    byte b2 = (byte) (~(i >> 8));
                    if (b2 == 0) {
                        int i6 = i2 + 1;
                        byte b3 = bArr[i2];
                        if (i6 >= i3) {
                            return Utf8.incompleteStateFor(b, b3);
                        }
                        i2 = i6;
                        b2 = b3;
                    }
                    if (b2 <= -65 && ((b != -32 || b2 >= -96) && (b != -19 || b2 < -96))) {
                        i4 = i2 + 1;
                    }
                    return -1;
                }
                byte b4 = (byte) (~(i >> 8));
                byte b5 = 0;
                if (b4 == 0) {
                    i5 = i2 + 1;
                    b4 = bArr[i2];
                    if (i5 >= i3) {
                        return Utf8.incompleteStateFor(b, b4);
                    }
                } else {
                    b5 = (byte) (i >> 16);
                    i5 = i2;
                }
                if (b5 == 0) {
                    int i7 = i5 + 1;
                    b5 = bArr[i5];
                    if (i7 >= i3) {
                        return Utf8.incompleteStateFor(b, b4, b5);
                    }
                    i5 = i7;
                }
                if (b4 <= -65 && (((b << 28) + (b4 + 112)) >> 30) == 0 && b5 <= -65) {
                    i2 = i5 + 1;
                }
                return -1;
            }
            i4 = i2;
            return partialIsValidUtf8(bArr, i4, i3);
        }

        @Override
        int partialIsValidUtf8Direct(int i, ByteBuffer byteBuffer, int i2, int i3) {
            return partialIsValidUtf8Default(i, byteBuffer, i2, i3);
        }

        @Override
        int encodeUtf8(CharSequence charSequence, byte[] bArr, int i, int i2) {
            int i3;
            int i4;
            int i5;
            char cCharAt;
            int length = charSequence.length();
            int i6 = i2 + i;
            int i7 = 0;
            while (i7 < length && (i5 = i7 + i) < i6 && (cCharAt = charSequence.charAt(i7)) < 128) {
                bArr[i5] = (byte) cCharAt;
                i7++;
            }
            if (i7 == length) {
                return i + length;
            }
            int i8 = i + i7;
            while (i7 < length) {
                char cCharAt2 = charSequence.charAt(i7);
                if (cCharAt2 < 128 && i8 < i6) {
                    i3 = i8 + 1;
                    bArr[i8] = (byte) cCharAt2;
                } else {
                    if (cCharAt2 < 2048 && i8 <= i6 - 2) {
                        int i9 = i8 + 1;
                        bArr[i8] = (byte) (960 | (cCharAt2 >>> 6));
                        i8 = i9 + 1;
                        bArr[i9] = (byte) ((cCharAt2 & '?') | 128);
                    } else if ((cCharAt2 < 55296 || 57343 < cCharAt2) && i8 <= i6 - 3) {
                        int i10 = i8 + 1;
                        bArr[i8] = (byte) (480 | (cCharAt2 >>> '\f'));
                        int i11 = i10 + 1;
                        bArr[i10] = (byte) (((cCharAt2 >>> 6) & 63) | 128);
                        i3 = i11 + 1;
                        bArr[i11] = (byte) ((cCharAt2 & '?') | 128);
                    } else {
                        if (i8 <= i6 - 4) {
                            int i12 = i7 + 1;
                            if (i12 != charSequence.length()) {
                                char cCharAt3 = charSequence.charAt(i12);
                                if (!Character.isSurrogatePair(cCharAt2, cCharAt3)) {
                                    i7 = i12;
                                } else {
                                    int codePoint = Character.toCodePoint(cCharAt2, cCharAt3);
                                    int i13 = i8 + 1;
                                    bArr[i8] = (byte) (240 | (codePoint >>> 18));
                                    int i14 = i13 + 1;
                                    bArr[i13] = (byte) (((codePoint >>> 12) & 63) | 128);
                                    int i15 = i14 + 1;
                                    bArr[i14] = (byte) (((codePoint >>> 6) & 63) | 128);
                                    i8 = i15 + 1;
                                    bArr[i15] = (byte) ((codePoint & 63) | 128);
                                    i7 = i12;
                                }
                            }
                            throw new UnpairedSurrogateException(i7 - 1, length);
                        }
                        if (55296 <= cCharAt2 && cCharAt2 <= 57343 && ((i4 = i7 + 1) == charSequence.length() || !Character.isSurrogatePair(cCharAt2, charSequence.charAt(i4)))) {
                            throw new UnpairedSurrogateException(i7, length);
                        }
                        throw new ArrayIndexOutOfBoundsException("Failed writing " + cCharAt2 + " at index " + i8);
                    }
                    i7++;
                }
                i8 = i3;
                i7++;
            }
            return i8;
        }

        @Override
        void encodeUtf8Direct(CharSequence charSequence, ByteBuffer byteBuffer) {
            encodeUtf8Default(charSequence, byteBuffer);
        }

        private static int partialIsValidUtf8(byte[] bArr, int i, int i2) {
            while (i < i2 && bArr[i] >= 0) {
                i++;
            }
            if (i >= i2) {
                return 0;
            }
            return partialIsValidUtf8NonAscii(bArr, i, i2);
        }

        private static int partialIsValidUtf8NonAscii(byte[] bArr, int i, int i2) {
            while (i < i2) {
                int i3 = i + 1;
                byte b = bArr[i];
                if (b >= 0) {
                    i = i3;
                } else {
                    if (b < -32) {
                        if (i3 >= i2) {
                            return b;
                        }
                        if (b >= -62) {
                            i = i3 + 1;
                            if (bArr[i3] > -65) {
                            }
                        }
                        return -1;
                    }
                    if (b < -16) {
                        if (i3 >= i2 - 1) {
                            return Utf8.incompleteStateFor(bArr, i3, i2);
                        }
                        int i4 = i3 + 1;
                        byte b2 = bArr[i3];
                        if (b2 <= -65 && ((b != -32 || b2 >= -96) && (b != -19 || b2 < -96))) {
                            i = i4 + 1;
                            if (bArr[i4] > -65) {
                            }
                        }
                        return -1;
                    }
                    if (i3 >= i2 - 2) {
                        return Utf8.incompleteStateFor(bArr, i3, i2);
                    }
                    int i5 = i3 + 1;
                    byte b3 = bArr[i3];
                    if (b3 <= -65 && (((b << 28) + (b3 + 112)) >> 30) == 0) {
                        int i6 = i5 + 1;
                        if (bArr[i5] <= -65) {
                            int i7 = i6 + 1;
                            if (bArr[i6] <= -65) {
                                i = i7;
                            }
                        }
                    }
                    return -1;
                }
            }
            return 0;
        }
    }

    static final class UnsafeProcessor extends Processor {
        private static final boolean AVAILABLE;
        private static final Unsafe UNSAFE = getUnsafe();
        private static final long BUFFER_ADDRESS_OFFSET = fieldOffset(field(Buffer.class, "address"));
        private static final int ARRAY_BASE_OFFSET = byteArrayBaseOffset();

        UnsafeProcessor() {
        }

        static {
            AVAILABLE = BUFFER_ADDRESS_OFFSET != -1 && ARRAY_BASE_OFFSET % 8 == 0;
        }

        static boolean isAvailable() {
            return AVAILABLE;
        }

        @Override
        int partialIsValidUtf8(int i, byte[] bArr, int i2, int i3) {
            long j;
            byte b;
            long j2;
            byte b2 = 0;
            if ((i2 | i3 | (bArr.length - i3)) < 0) {
                throw new ArrayIndexOutOfBoundsException(String.format("Array length=%d, index=%d, limit=%d", Integer.valueOf(bArr.length), Integer.valueOf(i2), Integer.valueOf(i3)));
            }
            long j3 = ARRAY_BASE_OFFSET + i2;
            long j4 = ARRAY_BASE_OFFSET + i3;
            if (i != 0) {
                if (j3 >= j4) {
                    return i;
                }
                byte b3 = (byte) i;
                if (b3 < -32) {
                    if (b3 >= -62) {
                        long j5 = j3 + 1;
                        if (UNSAFE.getByte(bArr, j3) <= -65) {
                            j3 = j5;
                        }
                    }
                    return -1;
                }
                if (b3 < -16) {
                    byte b4 = (byte) (~(i >> 8));
                    if (b4 == 0) {
                        j2 = j3 + 1;
                        b4 = UNSAFE.getByte(bArr, j3);
                        if (j2 >= j4) {
                            return Utf8.incompleteStateFor(b3, b4);
                        }
                    } else {
                        j2 = j3;
                    }
                    if (b4 <= -65 && ((b3 != -32 || b4 >= -96) && (b3 != -19 || b4 < -96))) {
                        j3 = j2 + 1;
                    }
                    return -1;
                }
                byte b5 = (byte) (~(i >> 8));
                if (b5 == 0) {
                    j = j3 + 1;
                    b5 = UNSAFE.getByte(bArr, j3);
                    if (j >= j4) {
                        return Utf8.incompleteStateFor(b3, b5);
                    }
                } else {
                    b2 = (byte) (i >> 16);
                    j = j3;
                }
                if (b2 == 0) {
                    long j6 = j + 1;
                    b = UNSAFE.getByte(bArr, j);
                    if (j6 >= j4) {
                        return Utf8.incompleteStateFor(b3, b5, b);
                    }
                    j = j6;
                } else {
                    b = b2;
                }
                if (b5 <= -65 && (((b3 << 28) + (b5 + 112)) >> 30) == 0 && b <= -65) {
                    j3 = j + 1;
                }
                return -1;
            }
            return partialIsValidUtf8(bArr, j3, (int) (j4 - j3));
        }

        @Override
        int partialIsValidUtf8Direct(int i, ByteBuffer byteBuffer, int i2, int i3) {
            long j;
            byte b = 0;
            if ((i2 | i3 | (byteBuffer.limit() - i3)) < 0) {
                throw new ArrayIndexOutOfBoundsException(String.format("buffer limit=%d, index=%d, limit=%d", Integer.valueOf(byteBuffer.limit()), Integer.valueOf(i2), Integer.valueOf(i3)));
            }
            long jAddressOffset = addressOffset(byteBuffer) + ((long) i2);
            long j2 = ((long) (i3 - i2)) + jAddressOffset;
            if (i != 0) {
                if (jAddressOffset >= j2) {
                    return i;
                }
                byte b2 = (byte) i;
                if (b2 < -32) {
                    if (b2 >= -62) {
                        j = jAddressOffset + 1;
                    }
                    return -1;
                }
                if (b2 < -16) {
                    byte b3 = (byte) (~(i >> 8));
                    if (b3 == 0) {
                        long j3 = jAddressOffset + 1;
                        b3 = UNSAFE.getByte(jAddressOffset);
                        if (j3 >= j2) {
                            return Utf8.incompleteStateFor(b2, b3);
                        }
                        jAddressOffset = j3;
                    }
                    if (b3 <= -65 && ((b2 != -32 || b3 >= -96) && (b2 != -19 || b3 < -96))) {
                        j = jAddressOffset + 1;
                    }
                    return -1;
                }
                byte b4 = (byte) (~(i >> 8));
                if (b4 == 0) {
                    long j4 = jAddressOffset + 1;
                    b4 = UNSAFE.getByte(jAddressOffset);
                    if (j4 >= j2) {
                        return Utf8.incompleteStateFor(b2, b4);
                    }
                    jAddressOffset = j4;
                } else {
                    b = (byte) (i >> 16);
                }
                if (b == 0) {
                    long j5 = jAddressOffset + 1;
                    b = UNSAFE.getByte(jAddressOffset);
                    if (j5 >= j2) {
                        return Utf8.incompleteStateFor(b2, b4, b);
                    }
                    jAddressOffset = j5;
                }
                if (b4 <= -65 && (((b2 << 28) + (b4 + 112)) >> 30) == 0 && b <= -65) {
                    j = jAddressOffset + 1;
                }
                return -1;
            }
            j = jAddressOffset;
            return partialIsValidUtf8(j, (int) (j2 - j));
        }

        @Override
        int encodeUtf8(CharSequence charSequence, byte[] bArr, int i, int i2) {
            char c;
            int i3;
            char c2;
            char cCharAt;
            long j = ARRAY_BASE_OFFSET + i;
            long j2 = ((long) i2) + j;
            int length = charSequence.length();
            if (length > i2 || bArr.length - i2 < i) {
                throw new ArrayIndexOutOfBoundsException("Failed writing " + charSequence.charAt(length - 1) + " at index " + (i + i2));
            }
            int i4 = 0;
            while (true) {
                c = 128;
                if (i4 >= length || (cCharAt = charSequence.charAt(i4)) >= 128) {
                    break;
                }
                UNSAFE.putByte(bArr, j, (byte) cCharAt);
                i4++;
                j = 1 + j;
            }
            if (i4 == length) {
                return (int) (j - ((long) ARRAY_BASE_OFFSET));
            }
            while (i4 < length) {
                char cCharAt2 = charSequence.charAt(i4);
                if (cCharAt2 >= c || j >= j2) {
                    if (cCharAt2 < 2048 && j <= j2 - 2) {
                        long j3 = j + 1;
                        UNSAFE.putByte(bArr, j, (byte) (960 | (cCharAt2 >>> 6)));
                        j = j3 + 1;
                        UNSAFE.putByte(bArr, j3, (byte) ((cCharAt2 & '?') | 128));
                    } else {
                        if ((cCharAt2 >= 55296 && 57343 >= cCharAt2) || j > j2 - 3) {
                            if (j > j2 - 4) {
                                if (55296 <= cCharAt2 && cCharAt2 <= 57343 && ((i3 = i4 + 1) == length || !Character.isSurrogatePair(cCharAt2, charSequence.charAt(i3)))) {
                                    throw new UnpairedSurrogateException(i4, length);
                                }
                                throw new ArrayIndexOutOfBoundsException("Failed writing " + cCharAt2 + " at index " + j);
                            }
                            int i5 = i4 + 1;
                            if (i5 != length) {
                                char cCharAt3 = charSequence.charAt(i5);
                                if (Character.isSurrogatePair(cCharAt2, cCharAt3)) {
                                    int codePoint = Character.toCodePoint(cCharAt2, cCharAt3);
                                    long j4 = j + 1;
                                    UNSAFE.putByte(bArr, j, (byte) (240 | (codePoint >>> 18)));
                                    long j5 = j4 + 1;
                                    c2 = 128;
                                    UNSAFE.putByte(bArr, j4, (byte) (128 | ((codePoint >>> 12) & 63)));
                                    long j6 = j5 + 1;
                                    UNSAFE.putByte(bArr, j5, (byte) (128 | ((codePoint >>> 6) & 63)));
                                    UNSAFE.putByte(bArr, j6, (byte) (128 | (codePoint & 63)));
                                    i4 = i5;
                                    j = j6 + 1;
                                } else {
                                    i4 = i5;
                                }
                            }
                            throw new UnpairedSurrogateException(i4 - 1, length);
                        }
                        long j7 = j + 1;
                        UNSAFE.putByte(bArr, j, (byte) (480 | (cCharAt2 >>> '\f')));
                        long j8 = j7 + 1;
                        UNSAFE.putByte(bArr, j7, (byte) (128 | ((cCharAt2 >>> 6) & 63)));
                        UNSAFE.putByte(bArr, j8, (byte) (128 | (cCharAt2 & '?')));
                        j = j8 + 1;
                    }
                    c2 = 128;
                } else {
                    UNSAFE.putByte(bArr, j, (byte) cCharAt2);
                    c2 = c;
                    j++;
                }
                i4++;
                c = c2;
            }
            return (int) (j - ((long) ARRAY_BASE_OFFSET));
        }

        @Override
        void encodeUtf8Direct(CharSequence charSequence, ByteBuffer byteBuffer) {
            long j;
            int i;
            char cCharAt;
            long jAddressOffset = addressOffset(byteBuffer);
            long jPosition = ((long) byteBuffer.position()) + jAddressOffset;
            long jLimit = ((long) byteBuffer.limit()) + jAddressOffset;
            int length = charSequence.length();
            if (length > jLimit - jPosition) {
                throw new ArrayIndexOutOfBoundsException("Failed writing " + charSequence.charAt(length - 1) + " at index " + byteBuffer.limit());
            }
            int i2 = 0;
            while (i2 < length && (cCharAt = charSequence.charAt(i2)) < 128) {
                UNSAFE.putByte(jPosition, (byte) cCharAt);
                i2++;
                jPosition = 1 + jPosition;
            }
            if (i2 == length) {
                byteBuffer.position((int) (jPosition - jAddressOffset));
                return;
            }
            while (i2 < length) {
                char cCharAt2 = charSequence.charAt(i2);
                if (cCharAt2 < 128 && jPosition < jLimit) {
                    UNSAFE.putByte(jPosition, (byte) cCharAt2);
                    j = jAddressOffset;
                    jPosition++;
                } else if (cCharAt2 >= 2048 || jPosition > jLimit - 2) {
                    j = jAddressOffset;
                    if ((cCharAt2 >= 55296 && 57343 >= cCharAt2) || jPosition > jLimit - 3) {
                        if (jPosition > jLimit - 4) {
                            if (55296 <= cCharAt2 && cCharAt2 <= 57343 && ((i = i2 + 1) == length || !Character.isSurrogatePair(cCharAt2, charSequence.charAt(i)))) {
                                throw new UnpairedSurrogateException(i2, length);
                            }
                            throw new ArrayIndexOutOfBoundsException("Failed writing " + cCharAt2 + " at index " + jPosition);
                        }
                        int i3 = i2 + 1;
                        if (i3 != length) {
                            char cCharAt3 = charSequence.charAt(i3);
                            if (Character.isSurrogatePair(cCharAt2, cCharAt3)) {
                                int codePoint = Character.toCodePoint(cCharAt2, cCharAt3);
                                long j2 = jPosition + 1;
                                UNSAFE.putByte(jPosition, (byte) (240 | (codePoint >>> 18)));
                                long j3 = j2 + 1;
                                UNSAFE.putByte(j2, (byte) (((codePoint >>> 12) & 63) | 128));
                                long j4 = j3 + 1;
                                UNSAFE.putByte(j3, (byte) (((codePoint >>> 6) & 63) | 128));
                                jPosition = j4 + 1;
                                UNSAFE.putByte(j4, (byte) ((codePoint & 63) | 128));
                                i2 = i3;
                            }
                        } else {
                            i3 = i2;
                        }
                        throw new UnpairedSurrogateException(i3 - 1, length);
                    }
                    long j5 = jPosition + 1;
                    UNSAFE.putByte(jPosition, (byte) (480 | (cCharAt2 >>> '\f')));
                    long j6 = j5 + 1;
                    UNSAFE.putByte(j5, (byte) (((cCharAt2 >>> 6) & 63) | 128));
                    UNSAFE.putByte(j6, (byte) ((cCharAt2 & '?') | 128));
                    jPosition = j6 + 1;
                } else {
                    j = jAddressOffset;
                    long j7 = jPosition + 1;
                    UNSAFE.putByte(jPosition, (byte) (960 | (cCharAt2 >>> 6)));
                    jPosition = j7 + 1;
                    UNSAFE.putByte(j7, (byte) ((cCharAt2 & '?') | 128));
                }
                i2++;
                jAddressOffset = j;
            }
            byteBuffer.position((int) (jPosition - jAddressOffset));
        }

        private static int unsafeEstimateConsecutiveAscii(byte[] bArr, long j, int i) {
            if (i < 16) {
                return 0;
            }
            int i2 = ((int) j) & 7;
            long j2 = j;
            int i3 = i2;
            while (i3 > 0) {
                long j3 = 1 + j2;
                if (UNSAFE.getByte(bArr, j2) >= 0) {
                    i3--;
                    j2 = j3;
                } else {
                    return i2 - i3;
                }
            }
            int i4 = i - i2;
            while (i4 >= 8 && (UNSAFE.getLong(bArr, j2) & Utf8.ASCII_MASK_LONG) == 0) {
                j2 += 8;
                i4 -= 8;
            }
            return i - i4;
        }

        private static int unsafeEstimateConsecutiveAscii(long j, int i) {
            if (i < 16) {
                return 0;
            }
            int i2 = ((int) j) & 7;
            long j2 = j;
            int i3 = i2;
            while (i3 > 0) {
                long j3 = 1 + j2;
                if (UNSAFE.getByte(j2) >= 0) {
                    i3--;
                    j2 = j3;
                } else {
                    return i2 - i3;
                }
            }
            int i4 = i - i2;
            while (i4 >= 8 && (UNSAFE.getLong(j2) & Utf8.ASCII_MASK_LONG) == 0) {
                j2 += 8;
                i4 -= 8;
            }
            return i - i4;
        }

        private static int partialIsValidUtf8(byte[] bArr, long j, int i) {
            long j2;
            int iUnsafeEstimateConsecutiveAscii = unsafeEstimateConsecutiveAscii(bArr, j, i);
            int i2 = i - iUnsafeEstimateConsecutiveAscii;
            long j3 = j + ((long) iUnsafeEstimateConsecutiveAscii);
            while (true) {
                byte b = 0;
                while (true) {
                    if (i2 <= 0) {
                        break;
                    }
                    long j4 = j3 + 1;
                    b = UNSAFE.getByte(bArr, j3);
                    if (b < 0) {
                        j3 = j4;
                        break;
                    }
                    i2--;
                    j3 = j4;
                }
                if (i2 == 0) {
                    return 0;
                }
                int i3 = i2 - 1;
                if (b < -32) {
                    if (i3 == 0) {
                        return b;
                    }
                    i2 = i3 - 1;
                    if (b < -62) {
                        break;
                    }
                    j2 = 1 + j3;
                    if (UNSAFE.getByte(bArr, j3) > -65) {
                        break;
                    }
                } else if (b < -16) {
                    if (i3 < 2) {
                        return unsafeIncompleteStateFor(bArr, b, j3, i3);
                    }
                    i2 = i3 - 2;
                    long j5 = j3 + 1;
                    byte b2 = UNSAFE.getByte(bArr, j3);
                    if (b2 > -65 || ((b == -32 && b2 < -96) || (b == -19 && b2 >= -96))) {
                        break;
                    }
                    j2 = 1 + j5;
                    if (UNSAFE.getByte(bArr, j5) > -65) {
                        break;
                    }
                } else {
                    if (i3 < 3) {
                        return unsafeIncompleteStateFor(bArr, b, j3, i3);
                    }
                    i2 = i3 - 3;
                    long j6 = j3 + 1;
                    byte b3 = UNSAFE.getByte(bArr, j3);
                    if (b3 <= -65 && (((b << 28) + (b3 + 112)) >> 30) == 0) {
                        long j7 = j6 + 1;
                        if (UNSAFE.getByte(bArr, j6) > -65) {
                            break;
                        }
                        j2 = 1 + j7;
                        if (UNSAFE.getByte(bArr, j7) > -65) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                j3 = j2;
            }
            return -1;
        }

        private static int partialIsValidUtf8(long j, int i) {
            long j2;
            int iUnsafeEstimateConsecutiveAscii = unsafeEstimateConsecutiveAscii(j, i);
            long j3 = j + ((long) iUnsafeEstimateConsecutiveAscii);
            int i2 = i - iUnsafeEstimateConsecutiveAscii;
            while (true) {
                byte b = 0;
                while (true) {
                    if (i2 <= 0) {
                        break;
                    }
                    long j4 = j3 + 1;
                    b = UNSAFE.getByte(j3);
                    if (b < 0) {
                        j3 = j4;
                        break;
                    }
                    i2--;
                    j3 = j4;
                }
                if (i2 == 0) {
                    return 0;
                }
                int i3 = i2 - 1;
                if (b < -32) {
                    if (i3 == 0) {
                        return b;
                    }
                    i2 = i3 - 1;
                    if (b < -62) {
                        break;
                    }
                    j2 = 1 + j3;
                    if (UNSAFE.getByte(j3) > -65) {
                        break;
                    }
                } else if (b < -16) {
                    if (i3 < 2) {
                        return unsafeIncompleteStateFor(j3, b, i3);
                    }
                    i2 = i3 - 2;
                    long j5 = j3 + 1;
                    byte b2 = UNSAFE.getByte(j3);
                    if (b2 > -65 || ((b == -32 && b2 < -96) || (b == -19 && b2 >= -96))) {
                        break;
                    }
                    j2 = 1 + j5;
                    if (UNSAFE.getByte(j5) > -65) {
                        break;
                    }
                } else {
                    if (i3 < 3) {
                        return unsafeIncompleteStateFor(j3, b, i3);
                    }
                    i2 = i3 - 3;
                    long j6 = j3 + 1;
                    byte b3 = UNSAFE.getByte(j3);
                    if (b3 <= -65 && (((b << 28) + (b3 + 112)) >> 30) == 0) {
                        long j7 = j6 + 1;
                        if (UNSAFE.getByte(j6) > -65) {
                            break;
                        }
                        j2 = 1 + j7;
                        if (UNSAFE.getByte(j7) > -65) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                j3 = j2;
            }
            return -1;
        }

        private static int unsafeIncompleteStateFor(byte[] bArr, int i, long j, int i2) {
            switch (i2) {
                case 0:
                    return Utf8.incompleteStateFor(i);
                case 1:
                    return Utf8.incompleteStateFor(i, UNSAFE.getByte(bArr, j));
                case 2:
                    return Utf8.incompleteStateFor(i, UNSAFE.getByte(bArr, j), UNSAFE.getByte(bArr, j + 1));
                default:
                    throw new AssertionError();
            }
        }

        private static int unsafeIncompleteStateFor(long j, int i, int i2) {
            switch (i2) {
                case 0:
                    return Utf8.incompleteStateFor(i);
                case 1:
                    return Utf8.incompleteStateFor(i, UNSAFE.getByte(j));
                case 2:
                    return Utf8.incompleteStateFor(i, UNSAFE.getByte(j), UNSAFE.getByte(j + 1));
                default:
                    throw new AssertionError();
            }
        }

        private static Field field(Class<?> cls, String str) {
            Field declaredField;
            try {
                declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
            } catch (Throwable th) {
                declaredField = null;
            }
            Logger logger = Utf8.logger;
            Level level = Level.FINEST;
            Object[] objArr = new Object[3];
            objArr[0] = cls.getName();
            objArr[1] = str;
            objArr[2] = declaredField != null ? "available" : "unavailable";
            logger.log(level, "{0}.{1}: {2}", objArr);
            return declaredField;
        }

        private static long fieldOffset(Field field) {
            if (field == null || UNSAFE == null) {
                return -1L;
            }
            return UNSAFE.objectFieldOffset(field);
        }

        private static <T> int byteArrayBaseOffset() {
            if (UNSAFE == null) {
                return -1;
            }
            return UNSAFE.arrayBaseOffset(byte[].class);
        }

        private static long addressOffset(ByteBuffer byteBuffer) {
            return UNSAFE.getLong(byteBuffer, BUFFER_ADDRESS_OFFSET);
        }

        private static Unsafe getUnsafe() {
            Unsafe unsafe;
            try {
                unsafe = (Unsafe) AccessController.doPrivileged(new PrivilegedExceptionAction<Unsafe>() {
                    @Override
                    public Unsafe run() throws Exception {
                        UnsafeProcessor.checkRequiredMethods(Unsafe.class);
                        for (Field field : Unsafe.class.getDeclaredFields()) {
                            field.setAccessible(true);
                            Object obj = field.get(null);
                            if (Unsafe.class.isInstance(obj)) {
                                return (Unsafe) Unsafe.class.cast(obj);
                            }
                        }
                        return null;
                    }
                });
            } catch (Throwable th) {
                unsafe = null;
            }
            Utf8.logger.log(Level.FINEST, "sun.misc.Unsafe: {}", unsafe != null ? "available" : "unavailable");
            return unsafe;
        }

        private static void checkRequiredMethods(Class<Unsafe> cls) throws NoSuchMethodException, SecurityException {
            cls.getMethod("arrayBaseOffset", Class.class);
            cls.getMethod("getByte", Object.class, Long.TYPE);
            cls.getMethod("putByte", Object.class, Long.TYPE, Byte.TYPE);
            cls.getMethod("getLong", Object.class, Long.TYPE);
            cls.getMethod("objectFieldOffset", Field.class);
            cls.getMethod("getByte", Long.TYPE);
            cls.getMethod("getLong", Object.class, Long.TYPE);
            cls.getMethod("putByte", Long.TYPE, Byte.TYPE);
            cls.getMethod("getLong", Long.TYPE);
        }
    }

    private Utf8() {
    }
}
