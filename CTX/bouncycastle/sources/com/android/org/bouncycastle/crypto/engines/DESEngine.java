package com.android.org.bouncycastle.crypto.engines;

import android.R;
import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.math.ec.Tnaf;

public class DESEngine implements BlockCipher {
    protected static final int BLOCK_SIZE = 8;
    private int[] workingKey = null;
    private static final short[] bytebit = {128, 64, 32, 16, 8, 4, 2, 1};
    private static final int[] bigbyte = {8388608, 4194304, 2097152, 1048576, 524288, 262144, 131072, 65536, 32768, 16384, 8192, 4096, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1};
    private static final byte[] pc1 = {56, 48, 40, 32, 24, Tnaf.POW_2_WIDTH, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3};
    private static final byte[] totrot = {1, 2, 4, 6, 8, 10, 12, 14, 15, 17, 19, 21, 23, 25, 27, 28};
    private static final byte[] pc2 = {13, Tnaf.POW_2_WIDTH, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1, 40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31};
    private static final int[] SP1 = {R.attr.transitionName, 0, 65536, R.attr.fillColor, R.attr.manageSpaceActivity, 66564, 4, 65536, 1024, R.attr.transitionName, R.attr.fillColor, 1024, 16778244, R.attr.manageSpaceActivity, 16777216, 4, 1028, 16778240, 16778240, 66560, 66560, R.attr.theme, R.attr.theme, 16778244, 65540, 16777220, 16777220, 65540, 0, 1028, 66564, 16777216, 65536, R.attr.fillColor, 4, R.attr.theme, R.attr.transitionName, 16777216, 16777216, 1024, R.attr.manageSpaceActivity, 65536, 66560, 16777220, 1024, 4, 16778244, 66564, R.attr.fillColor, 65540, R.attr.theme, 16778244, 16777220, 1028, 66564, R.attr.transitionName, 1028, 16778240, 16778240, 0, 65540, 66560, 0, R.attr.manageSpaceActivity};
    private static final int[] SP2 = {-2146402272, -2147450880, 32768, 1081376, 1048576, 32, -2146435040, -2147450848, -2147483616, -2146402272, -2146402304, Integer.MIN_VALUE, -2147450880, 1048576, 32, -2146435040, 1081344, 1048608, -2147450848, 0, Integer.MIN_VALUE, 32768, 1081376, -2146435072, 1048608, -2147483616, 0, 1081344, 32800, -2146402304, -2146435072, 32800, 0, 1081376, -2146435040, 1048576, -2147450848, -2146435072, -2146402304, 32768, -2146435072, -2147450880, 32, -2146402272, 1081376, 32, 32768, Integer.MIN_VALUE, 32800, -2146402304, 1048576, -2147483616, 1048608, -2147450848, -2147483616, 1048608, 1081344, 0, -2147450880, 32800, Integer.MIN_VALUE, -2146435040, -2146402272, 1081344};
    private static final int[] SP3 = {520, 134349312, 0, 134348808, 134218240, 0, 131592, 134218240, 131080, 134217736, 134217736, 131072, 134349320, 131080, 134348800, 520, 134217728, 8, 134349312, 512, 131584, 134348800, 134348808, 131592, 134218248, 131584, 131072, 134218248, 8, 134349320, 512, 134217728, 134349312, 134217728, 131080, 520, 131072, 134349312, 134218240, 0, 512, 131080, 134349320, 134218240, 134217736, 512, 0, 134348808, 134218248, 131072, 134217728, 134349320, 8, 131592, 131584, 134217736, 134348800, 134218248, 520, 134348800, 131592, 8, 134348808, 131584};
    private static final int[] SP4 = {8396801, 8321, 8321, 128, 8396928, 8388737, 8388609, 8193, 0, 8396800, 8396800, 8396929, 129, 0, 8388736, 8388609, 1, 8192, 8388608, 8396801, 128, 8388608, 8193, 8320, 8388737, 1, 8320, 8388736, 8192, 8396928, 8396929, 129, 8388736, 8388609, 8396800, 8396929, 129, 0, 0, 8396800, 8320, 8388736, 8388737, 1, 8396801, 8321, 8321, 128, 8396929, 129, 1, 8192, 8388609, 8193, 8396928, 8388737, 8193, 8320, 8388608, 8396801, 128, 8388608, 8192, 8396928};
    private static final int[] SP5 = {256, 34078976, 34078720, 1107296512, 524288, 256, 1073741824, 34078720, 1074266368, 524288, 33554688, 1074266368, 1107296512, 1107820544, 524544, 1073741824, 33554432, 1074266112, 1074266112, 0, 1073742080, 1107820800, 1107820800, 33554688, 1107820544, 1073742080, 0, 1107296256, 34078976, 33554432, 1107296256, 524544, 524288, 1107296512, 256, 33554432, 1073741824, 34078720, 1107296512, 1074266368, 33554688, 1073741824, 1107820544, 34078976, 1074266368, 256, 33554432, 1107820544, 1107820800, 524544, 1107296256, 1107820800, 34078720, 0, 1074266112, 1107296256, 524544, 33554688, 1073742080, 524288, 0, 1074266112, 34078976, 1073742080};
    private static final int[] SP6 = {536870928, 541065216, 16384, 541081616, 541065216, 16, 541081616, 4194304, 536887296, 4210704, 4194304, 536870928, 4194320, 536887296, 536870912, 16400, 0, 4194320, 536887312, 16384, 4210688, 536887312, 16, 541065232, 541065232, 0, 4210704, 541081600, 16400, 4210688, 541081600, 536870912, 536887296, 16, 541065232, 4210688, 541081616, 4194304, 16400, 536870928, 4194304, 536887296, 536870912, 16400, 536870928, 541081616, 4210688, 541065216, 4210704, 541081600, 0, 541065232, 16, 16384, 541065216, 4210704, 16384, 4194320, 536887312, 0, 541081600, 536870912, 4194320, 536887312};
    private static final int[] SP7 = {2097152, 69206018, 67110914, 0, 2048, 67110914, 2099202, 69208064, 69208066, 2097152, 0, 67108866, 2, 67108864, 69206018, 2050, 67110912, 2099202, 2097154, 67110912, 67108866, 69206016, 69208064, 2097154, 69206016, 2048, 2050, 69208066, 2099200, 2, 67108864, 2099200, 67108864, 2099200, 2097152, 67110914, 67110914, 69206018, 69206018, 2, 2097154, 67108864, 67110912, 2097152, 69208064, 2050, 2099202, 69208064, 2050, 67108866, 69208066, 69206016, 2099200, 0, 2, 69208066, 0, 2099202, 69206016, 2048, 67108866, 67110912, 2048, 2097154};
    private static final int[] SP8 = {268439616, 4096, 262144, 268701760, 268435456, 268439616, 64, 268435456, 262208, 268697600, 268701760, 266240, 268701696, 266304, 4096, 64, 268697600, 268435520, 268439552, 4160, 266240, 262208, 268697664, 268701696, 4160, 0, 0, 268697664, 268435520, 268439552, 266304, 262144, 266304, 262144, 268701696, 4096, 64, 268697664, 4096, 266304, 268439552, 64, 268435520, 268697600, 268697664, 268435456, 262144, 268439616, 0, 268701760, 262208, 268435520, 268697600, 268439552, 268439616, 0, 268701760, 266240, 266240, 4160, 4160, 262208, 268435456, 268701696};

    @Override
    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            KeyParameter keyParameter = (KeyParameter) cipherParameters;
            if (keyParameter.getKey().length > 8) {
                throw new IllegalArgumentException("DES key too long - should be 8 bytes");
            }
            this.workingKey = generateWorkingKey(z, keyParameter.getKey());
            return;
        }
        throw new IllegalArgumentException("invalid parameter passed to DES init - " + cipherParameters.getClass().getName());
    }

    @Override
    public String getAlgorithmName() {
        return "DES";
    }

    @Override
    public int getBlockSize() {
        return 8;
    }

    @Override
    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("DES engine not initialised");
        }
        if (i + 8 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        }
        if (i2 + 8 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        }
        desFunc(this.workingKey, bArr, i, bArr2, i2);
        return 8;
    }

    @Override
    public void reset() {
    }

    protected int[] generateWorkingKey(boolean z, byte[] bArr) {
        int i;
        int i2;
        int[] iArr = new int[32];
        boolean[] zArr = new boolean[56];
        boolean[] zArr2 = new boolean[56];
        int i3 = 0;
        while (true) {
            boolean z2 = true;
            if (i3 >= 56) {
                break;
            }
            byte b = pc1[i3];
            if ((bytebit[b & 7] & bArr[b >>> 3]) == 0) {
                z2 = false;
            }
            zArr[i3] = z2;
            i3++;
        }
        for (int i4 = 0; i4 < 16; i4++) {
            if (z) {
                i = i4 << 1;
            } else {
                i = (15 - i4) << 1;
            }
            int i5 = i + 1;
            iArr[i5] = 0;
            iArr[i] = 0;
            int i6 = 0;
            while (true) {
                if (i6 >= 28) {
                    break;
                }
                int i7 = totrot[i4] + i6;
                if (i7 < 28) {
                    zArr2[i6] = zArr[i7];
                } else {
                    zArr2[i6] = zArr[i7 - 28];
                }
                i6++;
            }
            for (i2 = 28; i2 < 56; i2++) {
                int i8 = totrot[i4] + i2;
                if (i8 < 56) {
                    zArr2[i2] = zArr[i8];
                } else {
                    zArr2[i2] = zArr[i8 - 28];
                }
            }
            for (int i9 = 0; i9 < 24; i9++) {
                if (zArr2[pc2[i9]]) {
                    iArr[i] = iArr[i] | bigbyte[i9];
                }
                if (zArr2[pc2[i9 + 24]]) {
                    iArr[i5] = iArr[i5] | bigbyte[i9];
                }
            }
        }
        for (int i10 = 0; i10 != 32; i10 += 2) {
            int i11 = iArr[i10];
            int i12 = i10 + 1;
            int i13 = iArr[i12];
            iArr[i10] = ((16515072 & i13) >>> 10) | ((i11 & 16515072) << 6) | ((i11 & 4032) << 10) | ((i13 & 4032) >>> 6);
            iArr[i12] = ((i11 & 63) << 16) | ((i11 & 258048) << 12) | ((258048 & i13) >>> 4) | (i13 & 63);
        }
        return iArr;
    }

    protected void desFunc(int[] iArr, byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3 = ((bArr[i + 0] & 255) << 24) | ((bArr[i + 1] & 255) << 16) | ((bArr[i + 2] & 255) << 8) | (bArr[i + 3] & 255);
        int i4 = (bArr[i + 7] & 255) | ((bArr[i + 4] & 255) << 24) | ((bArr[i + 5] & 255) << 16) | ((bArr[i + 6] & 255) << 8);
        int i5 = ((i3 >>> 4) ^ i4) & 252645135;
        int i6 = i4 ^ i5;
        int i7 = (i5 << 4) ^ i3;
        int i8 = ((i7 >>> 16) ^ i6) & 65535;
        int i9 = i6 ^ i8;
        int i10 = i7 ^ (i8 << 16);
        int i11 = ((i9 >>> 2) ^ i10) & 858993459;
        int i12 = i10 ^ i11;
        int i13 = i9 ^ (i11 << 2);
        int i14 = ((i13 >>> 8) ^ i12) & 16711935;
        int i15 = i12 ^ i14;
        int i16 = i13 ^ (i14 << 8);
        int i17 = (((i16 >>> 31) & 1) | (i16 << 1)) & (-1);
        int i18 = (i15 ^ i17) & (-1431655766);
        int i19 = i15 ^ i18;
        int i20 = i17 ^ i18;
        int i21 = (((i19 >>> 31) & 1) | (i19 << 1)) & (-1);
        for (int i22 = 0; i22 < 8; i22++) {
            int i23 = i22 * 4;
            int i24 = ((i20 << 28) | (i20 >>> 4)) ^ iArr[i23 + 0];
            int i25 = SP1[(i24 >>> 24) & 63] | SP7[i24 & 63] | SP5[(i24 >>> 8) & 63] | SP3[(i24 >>> 16) & 63];
            int i26 = iArr[i23 + 1] ^ i20;
            i21 ^= (((i25 | SP8[i26 & 63]) | SP6[(i26 >>> 8) & 63]) | SP4[(i26 >>> 16) & 63]) | SP2[(i26 >>> 24) & 63];
            int i27 = ((i21 << 28) | (i21 >>> 4)) ^ iArr[i23 + 2];
            int i28 = SP1[(i27 >>> 24) & 63] | SP7[i27 & 63] | SP5[(i27 >>> 8) & 63] | SP3[(i27 >>> 16) & 63];
            int i29 = iArr[i23 + 3] ^ i21;
            i20 ^= (((i28 | SP8[i29 & 63]) | SP6[(i29 >>> 8) & 63]) | SP4[(i29 >>> 16) & 63]) | SP2[(i29 >>> 24) & 63];
        }
        int i30 = (i20 << 31) | (i20 >>> 1);
        int i31 = (i21 ^ i30) & (-1431655766);
        int i32 = i21 ^ i31;
        int i33 = i30 ^ i31;
        int i34 = (i32 << 31) | (i32 >>> 1);
        int i35 = ((i34 >>> 8) ^ i33) & 16711935;
        int i36 = i33 ^ i35;
        int i37 = i34 ^ (i35 << 8);
        int i38 = ((i37 >>> 2) ^ i36) & 858993459;
        int i39 = i36 ^ i38;
        int i40 = i37 ^ (i38 << 2);
        int i41 = ((i39 >>> 16) ^ i40) & 65535;
        int i42 = i40 ^ i41;
        int i43 = i39 ^ (i41 << 16);
        int i44 = ((i43 >>> 4) ^ i42) & 252645135;
        int i45 = i42 ^ i44;
        int i46 = i43 ^ (i44 << 4);
        bArr2[i2 + 0] = (byte) ((i46 >>> 24) & 255);
        bArr2[i2 + 1] = (byte) ((i46 >>> 16) & 255);
        bArr2[i2 + 2] = (byte) ((i46 >>> 8) & 255);
        bArr2[i2 + 3] = (byte) (i46 & 255);
        bArr2[i2 + 4] = (byte) ((i45 >>> 24) & 255);
        bArr2[i2 + 5] = (byte) ((i45 >>> 16) & 255);
        bArr2[i2 + 6] = (byte) ((i45 >>> 8) & 255);
        bArr2[i2 + 7] = (byte) (i45 & 255);
    }
}
