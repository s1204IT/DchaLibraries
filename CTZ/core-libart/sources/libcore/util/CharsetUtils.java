package libcore.util;

import dalvik.annotation.optimization.FastNative;

public final class CharsetUtils {
    @FastNative
    public static native void asciiBytesToChars(byte[] bArr, int i, int i2, char[] cArr);

    @FastNative
    public static native void isoLatin1BytesToChars(byte[] bArr, int i, int i2, char[] cArr);

    @FastNative
    public static native byte[] toAsciiBytes(String str, int i, int i2);

    @FastNative
    public static native byte[] toIsoLatin1Bytes(String str, int i, int i2);

    @FastNative
    public static native byte[] toUtf8Bytes(String str, int i, int i2);

    public static byte[] toBigEndianUtf16Bytes(String str, int i, int i2) {
        byte[] bArr = new byte[i2 * 2];
        int i3 = i2 + i;
        int i4 = 0;
        while (i < i3) {
            char cCharAt = str.charAt(i);
            int i5 = i4 + 1;
            bArr[i4] = (byte) (cCharAt >> '\b');
            i4 = i5 + 1;
            bArr[i5] = (byte) cCharAt;
            i++;
        }
        return bArr;
    }

    private CharsetUtils() {
    }
}
