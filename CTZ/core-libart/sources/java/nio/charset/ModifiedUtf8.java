package java.nio.charset;

import android.icu.lang.UCharacterEnums;
import android.icu.text.Bidi;
import java.io.UTFDataFormatException;

public class ModifiedUtf8 {
    public static long countBytes(String str, boolean z) throws UTFDataFormatException {
        int length = str.length();
        long j = 0;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < 128) {
                j++;
                if (cCharAt == 0) {
                    j++;
                }
            } else if (cCharAt < 2048) {
                j += 2;
            } else {
                j += 3;
            }
        }
        if (z && j > 65535) {
            throw new UTFDataFormatException("Size of the encoded string doesn't fit in two bytes");
        }
        return j;
    }

    public static void encode(byte[] bArr, int i, String str) {
        int length = str.length();
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt < 128) {
                if (cCharAt == 0) {
                    int i3 = i + 1;
                    bArr[i] = -64;
                    i = i3 + 1;
                    bArr[i3] = Bidi.LEVEL_OVERRIDE;
                } else {
                    bArr[i] = (byte) cCharAt;
                    i++;
                }
            } else if (cCharAt < 2048) {
                int i4 = i + 1;
                bArr[i] = (byte) ((cCharAt >>> 6) | 192);
                i = i4 + 1;
                bArr[i4] = (byte) ((cCharAt & '?') | 128);
            } else {
                int i5 = i + 1;
                bArr[i] = (byte) ((cCharAt >>> '\f') | 224);
                int i6 = i5 + 1;
                bArr[i5] = (byte) (((cCharAt >>> 6) & 63) | 128);
                bArr[i6] = (byte) ((cCharAt & '?') | 128);
                i = i6 + 1;
            }
        }
    }

    public static byte[] encode(String str) throws UTFDataFormatException {
        int iCountBytes = (int) countBytes(str, true);
        byte[] bArr = new byte[iCountBytes + 2];
        encode(bArr, 2, str);
        bArr[0] = (byte) (r1 >>> 8);
        bArr[1] = (byte) iCountBytes;
        return bArr;
    }

    public static String decode(byte[] bArr, char[] cArr, int i, int i2) throws UTFDataFormatException {
        if (i < 0 || i2 < 0) {
            throw new IllegalArgumentException("Illegal arguments: offset " + i + ". Length: " + i2);
        }
        int i3 = i2 + i;
        int i4 = 0;
        while (i < i3) {
            int i5 = bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            i++;
            if (i5 < 128) {
                cArr[i4] = (char) i5;
                i4++;
            } else if (192 <= i5 && i5 < 224) {
                int i6 = (i5 & 31) << 6;
                if (i != i3) {
                    if ((192 & bArr[i]) != 128) {
                        throw new UTFDataFormatException("bad second byte at " + i);
                    }
                    cArr[i4] = (char) (i6 | (bArr[i] & 63));
                    i++;
                    i4++;
                } else {
                    throw new UTFDataFormatException("unexpected end of input");
                }
            } else if (i5 < 240) {
                int i7 = (i5 & 31) << 12;
                int i8 = i + 1;
                if (i8 < i3) {
                    if ((bArr[i] & 192) != 128) {
                        throw new UTFDataFormatException("bad second byte at " + i);
                    }
                    int i9 = ((bArr[i] & 63) << 6) | i7;
                    if ((bArr[i8] & 192) != 128) {
                        throw new UTFDataFormatException("bad third byte at " + i8);
                    }
                    cArr[i4] = (char) (i9 | (bArr[i8] & 63));
                    i4++;
                    i = i8 + 1;
                } else {
                    throw new UTFDataFormatException("unexpected end of input");
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Invalid UTF8 byte ");
                sb.append(i5);
                sb.append(" at position ");
                sb.append(i - 1);
                throw new UTFDataFormatException(sb.toString());
            }
        }
        return String.valueOf(cArr, 0, i4);
    }
}
