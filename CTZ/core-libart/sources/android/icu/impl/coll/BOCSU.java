package android.icu.impl.coll;

import android.icu.util.ByteArrayWrapper;

public class BOCSU {
    private static final int SLOPE_LEAD_2_ = 42;
    private static final int SLOPE_LEAD_3_ = 3;
    private static final int SLOPE_MAX_ = 255;
    private static final int SLOPE_MAX_BYTES_ = 4;
    private static final int SLOPE_MIDDLE_ = 129;
    private static final int SLOPE_MIN_ = 3;
    private static final int SLOPE_REACH_NEG_1_ = -80;
    private static final int SLOPE_REACH_NEG_2_ = -10668;
    private static final int SLOPE_REACH_NEG_3_ = -192786;
    private static final int SLOPE_REACH_POS_1_ = 80;
    private static final int SLOPE_REACH_POS_2_ = 10667;
    private static final int SLOPE_REACH_POS_3_ = 192785;
    private static final int SLOPE_SINGLE_ = 80;
    private static final int SLOPE_START_NEG_2_ = 49;
    private static final int SLOPE_START_NEG_3_ = 7;
    private static final int SLOPE_START_POS_2_ = 210;
    private static final int SLOPE_START_POS_3_ = 252;
    private static final int SLOPE_TAIL_COUNT_ = 253;

    public static int writeIdenticalLevelRun(int i, CharSequence charSequence, int i2, int i3, ByteArrayWrapper byteArrayWrapper) {
        int i4;
        while (i2 < i3) {
            ensureAppendCapacity(byteArrayWrapper, 16, charSequence.length() * 2);
            byte[] bArr = byteArrayWrapper.bytes;
            int length = bArr.length;
            int iWriteDiff = byteArrayWrapper.size;
            int i5 = length - 4;
            while (i2 < i3 && iWriteDiff <= i5) {
                if (i < 19968 || i >= 40960) {
                    i4 = (i & (-128)) + 80;
                } else {
                    i4 = 30292;
                }
                int iCodePointAt = Character.codePointAt(charSequence, i2);
                i2 += Character.charCount(iCodePointAt);
                if (iCodePointAt == 65534) {
                    bArr[iWriteDiff] = 2;
                    iWriteDiff++;
                    i = 0;
                } else {
                    iWriteDiff = writeDiff(iCodePointAt - i4, bArr, iWriteDiff);
                    i = iCodePointAt;
                }
            }
            byteArrayWrapper.size = iWriteDiff;
        }
        return i;
    }

    private static void ensureAppendCapacity(ByteArrayWrapper byteArrayWrapper, int i, int i2) {
        if (byteArrayWrapper.bytes.length - byteArrayWrapper.size >= i) {
            return;
        }
        if (i2 >= i) {
            i = i2;
        }
        byteArrayWrapper.ensureCapacity(byteArrayWrapper.size + i);
    }

    private BOCSU() {
    }

    private static final long getNegDivMod(int i, int i2) {
        int i3 = i % i2;
        long j = i / i2;
        if (i3 < 0) {
            j--;
            i3 += i2;
        }
        return (j << 32) | ((long) i3);
    }

    private static final int writeDiff(int i, byte[] bArr, int i2) {
        if (i >= SLOPE_REACH_NEG_1_) {
            if (i <= 80) {
                int i3 = i2 + 1;
                bArr[i2] = (byte) (129 + i);
                return i3;
            }
            if (i <= SLOPE_REACH_POS_2_) {
                int i4 = i2 + 1;
                bArr[i2] = (byte) (210 + (i / 253));
                int i5 = i4 + 1;
                bArr[i4] = (byte) (3 + (i % 253));
                return i5;
            }
            if (i <= SLOPE_REACH_POS_3_) {
                bArr[i2 + 2] = (byte) ((i % 253) + 3);
                int i6 = i / 253;
                bArr[i2 + 1] = (byte) ((i6 % 253) + 3);
                bArr[i2] = (byte) (252 + (i6 / 253));
                return i2 + 3;
            }
            bArr[i2 + 3] = (byte) ((i % 253) + 3);
            int i7 = i / 253;
            bArr[i2 + 2] = (byte) ((i7 % 253) + 3);
            bArr[i2 + 1] = (byte) (3 + ((i7 / 253) % 253));
            bArr[i2] = -1;
            return i2 + 4;
        }
        long negDivMod = getNegDivMod(i, 253);
        int i8 = (int) negDivMod;
        if (i >= SLOPE_REACH_NEG_2_) {
            int i9 = i2 + 1;
            bArr[i2] = (byte) (49 + ((int) (negDivMod >> 32)));
            int i10 = i9 + 1;
            bArr[i9] = (byte) (3 + i8);
            return i10;
        }
        if (i >= SLOPE_REACH_NEG_3_) {
            bArr[i2 + 2] = (byte) (i8 + 3);
            long negDivMod2 = getNegDivMod((int) (negDivMod >> 32), 253);
            bArr[i2 + 1] = (byte) (((int) negDivMod2) + 3);
            bArr[i2] = (byte) (7 + ((int) (negDivMod2 >> 32)));
            return i2 + 3;
        }
        bArr[i2 + 3] = (byte) (i8 + 3);
        long negDivMod3 = getNegDivMod((int) (negDivMod >> 32), 253);
        bArr[i2 + 2] = (byte) (((int) negDivMod3) + 3);
        bArr[i2 + 1] = (byte) (((int) getNegDivMod((int) (negDivMod3 >> 32), 253)) + 3);
        bArr[i2] = 3;
        return i2 + 4;
    }
}
