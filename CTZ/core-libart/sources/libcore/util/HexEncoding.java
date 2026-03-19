package libcore.util;

public class HexEncoding {
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private HexEncoding() {
    }

    public static char[] encode(byte[] bArr) {
        return encode(bArr, 0, bArr.length);
    }

    public static char[] encode(byte[] bArr, int i, int i2) {
        char[] cArr = new char[i2 * 2];
        for (int i3 = 0; i3 < i2; i3++) {
            byte b = bArr[i + i3];
            int i4 = 2 * i3;
            cArr[i4] = HEX_DIGITS[(b >>> 4) & 15];
            cArr[i4 + 1] = HEX_DIGITS[b & 15];
        }
        return cArr;
    }

    public static String encodeToString(byte[] bArr) {
        return new String(encode(bArr));
    }

    public static byte[] decode(String str) throws IllegalArgumentException {
        return decode(str.toCharArray());
    }

    public static byte[] decode(String str, boolean z) throws IllegalArgumentException {
        return decode(str.toCharArray(), z);
    }

    public static byte[] decode(char[] cArr) throws IllegalArgumentException {
        return decode(cArr, false);
    }

    public static byte[] decode(char[] cArr, boolean z) throws IllegalArgumentException {
        int length;
        int i = 1;
        byte[] bArr = new byte[(cArr.length + 1) / 2];
        int i2 = 0;
        if (z) {
            if (cArr.length % 2 != 0) {
                bArr[0] = (byte) toDigit(cArr, 0);
                i2 = 1;
            }
            length = cArr.length;
            while (i2 < length) {
                bArr[i] = (byte) ((toDigit(cArr, i2) << 4) | toDigit(cArr, i2 + 1));
                i2 += 2;
                i++;
            }
            return bArr;
        }
        if (cArr.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid input length: " + cArr.length);
        }
        i = 0;
        length = cArr.length;
        while (i2 < length) {
        }
        return bArr;
    }

    private static int toDigit(char[] cArr, int i) throws IllegalArgumentException {
        char c = cArr[i];
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        if ('a' <= c && c <= 'f') {
            return 10 + (c - 'a');
        }
        if ('A' <= c && c <= 'F') {
            return 10 + (c - 'A');
        }
        throw new IllegalArgumentException("Illegal char: " + cArr[i] + " at offset " + i);
    }
}
