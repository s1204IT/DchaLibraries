package com.android.org.bouncycastle.crypto.modes.gcm;

import com.android.org.bouncycastle.crypto.util.Pack;
import com.android.org.bouncycastle.util.Arrays;
import java.lang.reflect.Array;

public class Tables8kGCMMultiplier implements GCMMultiplier {
    private byte[] H;
    private int[][][] M;

    @Override
    public void init(byte[] H) {
        if (this.M == null) {
            this.M = (int[][][]) Array.newInstance((Class<?>) Integer.TYPE, 32, 16, 4);
        } else if (Arrays.areEqual(this.H, H)) {
            return;
        }
        this.H = Arrays.clone(H);
        GCMUtil.asInts(H, this.M[1][8]);
        for (int j = 4; j >= 1; j >>= 1) {
            GCMUtil.multiplyP(this.M[1][j + j], this.M[1][j]);
        }
        GCMUtil.multiplyP(this.M[1][1], this.M[0][8]);
        for (int j2 = 4; j2 >= 1; j2 >>= 1) {
            GCMUtil.multiplyP(this.M[0][j2 + j2], this.M[0][j2]);
        }
        int i = 0;
        while (true) {
            for (int j3 = 2; j3 < 16; j3 += j3) {
                for (int k = 1; k < j3; k++) {
                    GCMUtil.xor(this.M[i][j3], this.M[i][k], this.M[i][j3 + k]);
                }
            }
            i++;
            if (i == 32) {
                return;
            }
            if (i > 1) {
                for (int j4 = 8; j4 > 0; j4 >>= 1) {
                    GCMUtil.multiplyP8(this.M[i - 2][j4], this.M[i][j4]);
                }
            }
        }
    }

    @Override
    public void multiplyH(byte[] x) {
        int[] z = new int[4];
        for (int i = 15; i >= 0; i--) {
            int[] m = this.M[i + i][x[i] & 15];
            z[0] = z[0] ^ m[0];
            z[1] = z[1] ^ m[1];
            z[2] = z[2] ^ m[2];
            z[3] = z[3] ^ m[3];
            int[] m2 = this.M[i + i + 1][(x[i] & 240) >>> 4];
            z[0] = z[0] ^ m2[0];
            z[1] = z[1] ^ m2[1];
            z[2] = z[2] ^ m2[2];
            z[3] = z[3] ^ m2[3];
        }
        Pack.intToBigEndian(z, x, 0);
    }
}
