package com.android.gallery3d.jpegstream;

import java.nio.ByteOrder;

public class StreamUtils {
    private StreamUtils() {
    }

    public static boolean byteToIntArray(int[] iArr, byte[] bArr, ByteOrder byteOrder) {
        if (iArr.length * 4 < bArr.length - (bArr.length % 4)) {
            throw new ArrayIndexOutOfBoundsException("Output array is too short to hold input");
        }
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            int i = 0;
            int i2 = 0;
            while (i < iArr.length) {
                iArr[i] = ((bArr[i2] & 255) << 24) | ((bArr[i2 + 1] & 255) << 16) | ((bArr[i2 + 2] & 255) << 8) | (bArr[i2 + 3] & 255);
                i++;
                i2 += 4;
            }
        } else {
            int i3 = 0;
            int i4 = 0;
            while (i3 < iArr.length) {
                iArr[i3] = ((bArr[i4 + 3] & 255) << 24) | ((bArr[i4 + 2] & 255) << 16) | ((bArr[i4 + 1] & 255) << 8) | (bArr[i4] & 255);
                i3++;
                i4 += 4;
            }
        }
        return bArr.length % 4 != 0;
    }

    public static int[] byteToIntArray(byte[] bArr, ByteOrder byteOrder) {
        int[] iArr = new int[bArr.length / 4];
        byteToIntArray(iArr, bArr, byteOrder);
        return iArr;
    }

    public static int[] byteToIntArray(byte[] bArr) {
        return byteToIntArray(bArr, ByteOrder.nativeOrder());
    }

    public static int pixelSize(int i) {
        if (i == 1) {
            return 1;
        }
        if (i == 260) {
            return 4;
        }
        switch (i) {
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                return -1;
        }
    }
}
