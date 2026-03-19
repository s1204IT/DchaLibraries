package org.apache.commons.codec.binary;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.http.protocol.HTTP;

@Deprecated
public class Base64 implements BinaryEncoder, BinaryDecoder {
    static final int CHUNK_SIZE = 76;
    static final int EIGHTBIT = 8;
    static final int FOURBYTE = 4;
    static final byte PAD = 61;
    static final int SIGN = -128;
    static final int SIXTEENBIT = 16;
    static final int TWENTYFOURBITGROUP = 24;
    static final byte[] CHUNK_SEPARATOR = "\r\n".getBytes();
    static final int BASELENGTH = 255;
    private static byte[] base64Alphabet = new byte[BASELENGTH];
    static final int LOOKUPLENGTH = 64;
    private static byte[] lookUpBase64Alphabet = new byte[LOOKUPLENGTH];

    static {
        int i;
        int i2;
        int i3 = 0;
        for (int i4 = 0; i4 < BASELENGTH; i4++) {
            base64Alphabet[i4] = -1;
        }
        for (int i5 = 90; i5 >= 65; i5--) {
            base64Alphabet[i5] = (byte) (i5 - 65);
        }
        int i6 = 122;
        while (true) {
            i = 26;
            if (i6 < 97) {
                break;
            }
            base64Alphabet[i6] = (byte) ((i6 - 97) + 26);
            i6--;
        }
        int i7 = 57;
        while (true) {
            i2 = 52;
            if (i7 < 48) {
                break;
            }
            base64Alphabet[i7] = (byte) ((i7 - 48) + 52);
            i7--;
        }
        base64Alphabet[43] = 62;
        base64Alphabet[47] = 63;
        for (int i8 = 0; i8 <= 25; i8++) {
            lookUpBase64Alphabet[i8] = (byte) (65 + i8);
        }
        int i9 = 0;
        while (i <= 51) {
            lookUpBase64Alphabet[i] = (byte) (97 + i9);
            i++;
            i9++;
        }
        while (i2 <= 61) {
            lookUpBase64Alphabet[i2] = (byte) (48 + i3);
            i2++;
            i3++;
        }
        lookUpBase64Alphabet[62] = 43;
        lookUpBase64Alphabet[63] = 47;
    }

    private static boolean isBase64(byte b) {
        if (b == 61 || base64Alphabet[b] != -1) {
            return true;
        }
        return false;
    }

    public static boolean isArrayByteBase64(byte[] bArr) {
        byte[] bArrDiscardWhitespace = discardWhitespace(bArr);
        if (bArrDiscardWhitespace.length == 0) {
            return true;
        }
        for (byte b : bArrDiscardWhitespace) {
            if (!isBase64(b)) {
                return false;
            }
        }
        return true;
    }

    public static byte[] encodeBase64(byte[] bArr) {
        return encodeBase64(bArr, false);
    }

    public static byte[] encodeBase64Chunked(byte[] bArr) {
        return encodeBase64(bArr, true);
    }

    @Override
    public Object decode(Object obj) throws DecoderException {
        if (obj instanceof byte[]) {
            return decode((byte[]) obj);
        }
        throw new DecoderException("Parameter supplied to Base64 decode is not a byte[]");
    }

    @Override
    public byte[] decode(byte[] bArr) {
        return decodeBase64(bArr);
    }

    public static byte[] encodeBase64(byte[] bArr, boolean z) {
        int length;
        int iCeil;
        int i;
        int i2;
        int length2 = bArr.length * EIGHTBIT;
        int i3 = length2 % TWENTYFOURBITGROUP;
        int i4 = length2 / TWENTYFOURBITGROUP;
        if (i3 != 0) {
            length = (i4 + 1) * 4;
        } else {
            length = i4 * 4;
        }
        if (z) {
            if (CHUNK_SEPARATOR.length != 0) {
                iCeil = (int) Math.ceil(length / 76.0f);
            } else {
                iCeil = 0;
            }
            length += CHUNK_SEPARATOR.length * iCeil;
        } else {
            iCeil = 0;
        }
        byte[] bArr2 = new byte[length];
        int i5 = 0;
        int length3 = 0;
        int i6 = 0;
        int i7 = CHUNK_SIZE;
        while (i5 < i4) {
            int i8 = i5 * 3;
            byte b = bArr[i8];
            byte b2 = bArr[i8 + 1];
            byte b3 = bArr[i8 + 2];
            byte b4 = (byte) (b2 & 15);
            byte b5 = (byte) (b & 3);
            byte b6 = (byte) ((b & (-128)) == 0 ? b >> 2 : (b >> 2) ^ 192);
            byte b7 = (byte) ((b2 & (-128)) == 0 ? b2 >> 4 : (b2 >> 4) ^ 240);
            if ((b3 & (-128)) == 0) {
                i = i4;
                i2 = b3 >> 6;
            } else {
                i = i4;
                i2 = (b3 >> 6) ^ 252;
            }
            byte b8 = (byte) i2;
            bArr2[length3] = lookUpBase64Alphabet[b6];
            bArr2[length3 + 1] = lookUpBase64Alphabet[b7 | (b5 << 4)];
            bArr2[length3 + 2] = lookUpBase64Alphabet[b8 | (b4 << 2)];
            bArr2[length3 + 3] = lookUpBase64Alphabet[b3 & 63];
            length3 += 4;
            if (z && length3 == i7) {
                System.arraycopy(CHUNK_SEPARATOR, 0, bArr2, length3, CHUNK_SEPARATOR.length);
                i6++;
                int length4 = (CHUNK_SIZE * (i6 + 1)) + (CHUNK_SEPARATOR.length * i6);
                length3 += CHUNK_SEPARATOR.length;
                i7 = length4;
            }
            i5++;
            i4 = i;
        }
        int i9 = i5 * 3;
        if (i3 == EIGHTBIT) {
            byte b9 = bArr[i9];
            byte b10 = (byte) (b9 & 3);
            bArr2[length3] = lookUpBase64Alphabet[(byte) ((b9 & (-128)) == 0 ? b9 >> 2 : (b9 >> 2) ^ 192)];
            bArr2[length3 + 1] = lookUpBase64Alphabet[b10 << 4];
            bArr2[length3 + 2] = PAD;
            bArr2[length3 + 3] = PAD;
        } else if (i3 == 16) {
            byte b11 = bArr[i9];
            byte b12 = bArr[i9 + 1];
            byte b13 = (byte) (b12 & 15);
            byte b14 = (byte) (b11 & 3);
            byte b15 = (byte) ((b11 & (-128)) == 0 ? b11 >> 2 : (b11 >> 2) ^ 192);
            byte b16 = (byte) ((b12 & (-128)) == 0 ? b12 >> 4 : (b12 >> 4) ^ 240);
            bArr2[length3] = lookUpBase64Alphabet[b15];
            bArr2[length3 + 1] = lookUpBase64Alphabet[b16 | (b14 << 4)];
            bArr2[length3 + 2] = lookUpBase64Alphabet[b13 << 2];
            bArr2[length3 + 3] = PAD;
        }
        if (z && i6 < iCeil) {
            System.arraycopy(CHUNK_SEPARATOR, 0, bArr2, length - CHUNK_SEPARATOR.length, CHUNK_SEPARATOR.length);
        }
        return bArr2;
    }

    public static byte[] decodeBase64(byte[] bArr) {
        byte[] bArrDiscardNonBase64 = discardNonBase64(bArr);
        if (bArrDiscardNonBase64.length == 0) {
            return new byte[0];
        }
        int length = bArrDiscardNonBase64.length / 4;
        int length2 = bArrDiscardNonBase64.length;
        while (bArrDiscardNonBase64[length2 - 1] == 61) {
            length2--;
            if (length2 == 0) {
                return new byte[0];
            }
        }
        byte[] bArr2 = new byte[length2 - length];
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = i2 * 4;
            byte b = bArrDiscardNonBase64[i3 + 2];
            byte b2 = bArrDiscardNonBase64[i3 + 3];
            byte b3 = base64Alphabet[bArrDiscardNonBase64[i3]];
            byte b4 = base64Alphabet[bArrDiscardNonBase64[i3 + 1]];
            if (b != 61 && b2 != 61) {
                byte b5 = base64Alphabet[b];
                byte b6 = base64Alphabet[b2];
                bArr2[i] = (byte) ((b3 << 2) | (b4 >> 4));
                bArr2[i + 1] = (byte) (((b4 & 15) << 4) | ((b5 >> 2) & 15));
                bArr2[i + 2] = (byte) ((b5 << 6) | b6);
            } else if (b == 61) {
                bArr2[i] = (byte) ((b4 >> 4) | (b3 << 2));
            } else if (b2 == 61) {
                byte b7 = base64Alphabet[b];
                bArr2[i] = (byte) ((b3 << 2) | (b4 >> 4));
                bArr2[i + 1] = (byte) (((b4 & 15) << 4) | ((b7 >> 2) & 15));
            }
            i += 3;
        }
        return bArr2;
    }

    static byte[] discardWhitespace(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length];
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            byte b = bArr[i2];
            if (b != 13 && b != 32) {
                switch (b) {
                    case HTTP.HT:
                    case HTTP.LF:
                        break;
                    default:
                        bArr2[i] = bArr[i2];
                        i++;
                        break;
                }
            }
        }
        byte[] bArr3 = new byte[i];
        System.arraycopy(bArr2, 0, bArr3, 0, i);
        return bArr3;
    }

    static byte[] discardNonBase64(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length];
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            if (isBase64(bArr[i2])) {
                bArr2[i] = bArr[i2];
                i++;
            }
        }
        byte[] bArr3 = new byte[i];
        System.arraycopy(bArr2, 0, bArr3, 0, i);
        return bArr3;
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        if (obj instanceof byte[]) {
            return encode((byte[]) obj);
        }
        throw new EncoderException("Parameter supplied to Base64 encode is not a byte[]");
    }

    @Override
    public byte[] encode(byte[] bArr) {
        return encodeBase64(bArr, false);
    }
}
