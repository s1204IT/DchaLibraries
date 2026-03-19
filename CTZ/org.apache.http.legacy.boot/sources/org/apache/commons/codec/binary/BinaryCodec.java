package org.apache.commons.codec.binary;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;

@Deprecated
public class BinaryCodec implements BinaryDecoder, BinaryEncoder {
    private static final int BIT_0 = 1;
    private static final int BIT_1 = 2;
    private static final int BIT_2 = 4;
    private static final int BIT_4 = 16;
    private static final int BIT_5 = 32;
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final int BIT_3 = 8;
    private static final int BIT_6 = 64;
    private static final int BIT_7 = 128;
    private static final int[] BITS = {1, 2, 4, BIT_3, 16, 32, BIT_6, BIT_7};

    @Override
    public byte[] encode(byte[] bArr) {
        return toAsciiBytes(bArr);
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        if (obj instanceof byte[]) {
            return toAsciiChars(obj);
        }
        throw new EncoderException("argument not a byte array");
    }

    @Override
    public Object decode(Object obj) throws DecoderException {
        if (obj == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        if (obj instanceof byte[]) {
            return fromAscii((byte[]) obj);
        }
        if (obj instanceof char[]) {
            return fromAscii((char[]) obj);
        }
        if (obj instanceof String) {
            return fromAscii(obj.toCharArray());
        }
        throw new DecoderException("argument not a byte array");
    }

    @Override
    public byte[] decode(byte[] bArr) {
        return fromAscii(bArr);
    }

    public byte[] toByteArray(String str) {
        if (str == null) {
            return EMPTY_BYTE_ARRAY;
        }
        return fromAscii(str.toCharArray());
    }

    public static byte[] fromAscii(char[] cArr) {
        if (cArr == null || cArr.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        byte[] bArr = new byte[cArr.length >> 3];
        int length = cArr.length - 1;
        int i = 0;
        while (i < bArr.length) {
            for (int i2 = 0; i2 < BITS.length; i2++) {
                if (cArr[length - i2] == '1') {
                    bArr[i] = (byte) (bArr[i] | BITS[i2]);
                }
            }
            i++;
            length -= 8;
        }
        return bArr;
    }

    public static byte[] fromAscii(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        byte[] bArr2 = new byte[bArr.length >> 3];
        int length = bArr.length - 1;
        int i = 0;
        while (i < bArr2.length) {
            for (int i2 = 0; i2 < BITS.length; i2++) {
                if (bArr[length - i2] == 49) {
                    bArr2[i] = (byte) (bArr2[i] | BITS[i2]);
                }
            }
            i++;
            length -= 8;
        }
        return bArr2;
    }

    public static byte[] toAsciiBytes(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        byte[] bArr2 = new byte[bArr.length << 3];
        int length = bArr2.length - 1;
        int i = 0;
        while (i < bArr.length) {
            for (int i2 = 0; i2 < BITS.length; i2++) {
                if ((bArr[i] & BITS[i2]) == 0) {
                    bArr2[length - i2] = 48;
                } else {
                    bArr2[length - i2] = 49;
                }
            }
            i++;
            length -= 8;
        }
        return bArr2;
    }

    public static char[] toAsciiChars(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        char[] cArr = new char[bArr.length << 3];
        int length = cArr.length - 1;
        int i = 0;
        while (i < bArr.length) {
            for (int i2 = 0; i2 < BITS.length; i2++) {
                if ((bArr[i] & BITS[i2]) == 0) {
                    cArr[length - i2] = '0';
                } else {
                    cArr[length - i2] = '1';
                }
            }
            i++;
            length -= 8;
        }
        return cArr;
    }

    public static String toAsciiString(byte[] bArr) {
        return new String(toAsciiChars(bArr));
    }
}
