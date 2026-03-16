package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.RC2Parameters;
import com.android.org.bouncycastle.math.ec.Tnaf;

public class RC2Engine implements BlockCipher {
    private static final int BLOCK_SIZE = 8;
    private static byte[] piTable = {-39, 120, -7, -60, 25, -35, -75, -19, 40, -23, -3, 121, 74, -96, -40, -99, -58, 126, 55, -125, 43, 118, 83, -114, 98, 76, 100, -120, 68, -117, -5, -94, 23, -102, 89, -11, -121, -77, 79, 19, 97, 69, 109, -115, 9, -127, 125, 50, -67, -113, 64, -21, -122, -73, 123, 11, -16, -107, 33, 34, 92, 107, 78, -126, 84, -42, 101, -109, -50, 96, -78, 28, 115, 86, -64, 20, -89, -116, -15, -36, 18, 117, -54, 31, 59, -66, -28, -47, 66, 61, -44, 48, -93, 60, -74, 38, 111, -65, 14, -38, 70, 105, 7, 87, 39, -14, 29, -101, -68, -108, 67, 3, -8, 17, -57, -10, -112, -17, 62, -25, 6, -61, -43, 47, -56, 102, 30, -41, 8, -24, -22, -34, -128, 82, -18, -9, -124, -86, 114, -84, 53, 77, 106, 42, -106, 26, -46, 113, 90, 21, 73, 116, 75, -97, -48, 94, 4, 24, -92, -20, -62, -32, 65, 110, 15, 81, -53, -52, 36, -111, -81, 80, -95, -12, 112, 57, -103, 124, 58, -123, 35, -72, -76, 122, -4, 2, 54, 91, 37, 85, -105, 49, 45, 93, -6, -104, -29, -118, -110, -82, 5, -33, 41, Tnaf.POW_2_WIDTH, 103, 108, -70, -55, -45, 0, -26, -49, -31, -98, -88, 44, 99, 22, 1, 63, 88, -30, -119, -87, 13, 56, 52, 27, -85, 51, -1, -80, -69, 72, 12, 95, -71, -79, -51, 46, -59, -13, -37, 71, -27, -91, -100, 119, 10, -90, 32, 104, -2, 127, -63, -83};
    private boolean encrypting;
    private int[] workingKey;

    private int[] generateWorkingKey(byte[] key, int bits) {
        int[] xKey = new int[128];
        for (int i = 0; i != key.length; i++) {
            xKey[i] = key[i] & 255;
        }
        int len = key.length;
        if (len < 128) {
            int index = 0;
            int x = xKey[len - 1];
            while (true) {
                int index2 = index + 1;
                x = piTable[(xKey[index] + x) & 255] & 255;
                int len2 = len + 1;
                xKey[len] = x;
                if (len2 >= 128) {
                    break;
                }
                index = index2;
                len = len2;
            }
        }
        int len3 = (bits + 7) >> 3;
        int x2 = piTable[xKey[128 - len3] & (255 >> ((-bits) & 7))] & 255;
        xKey[128 - len3] = x2;
        for (int i2 = (128 - len3) - 1; i2 >= 0; i2--) {
            x2 = piTable[xKey[i2 + len3] ^ x2] & 255;
            xKey[i2] = x2;
        }
        int[] newKey = new int[64];
        for (int i3 = 0; i3 != newKey.length; i3++) {
            newKey[i3] = xKey[i3 * 2] + (xKey[(i3 * 2) + 1] << 8);
        }
        return newKey;
    }

    @Override
    public void init(boolean encrypting, CipherParameters params) {
        this.encrypting = encrypting;
        if (params instanceof RC2Parameters) {
            RC2Parameters param = (RC2Parameters) params;
            this.workingKey = generateWorkingKey(param.getKey(), param.getEffectiveKeyBits());
        } else {
            if (params instanceof KeyParameter) {
                byte[] key = ((KeyParameter) params).getKey();
                this.workingKey = generateWorkingKey(key, key.length * 8);
                return;
            }
            throw new IllegalArgumentException("invalid parameter passed to RC2 init - " + params.getClass().getName());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public String getAlgorithmName() {
        return "RC2";
    }

    @Override
    public int getBlockSize() {
        return 8;
    }

    @Override
    public final int processBlock(byte[] in, int inOff, byte[] out, int outOff) {
        if (this.workingKey == null) {
            throw new IllegalStateException("RC2 engine not initialised");
        }
        if (inOff + 8 > in.length) {
            throw new DataLengthException("input buffer too short");
        }
        if (outOff + 8 > out.length) {
            throw new OutputLengthException("output buffer too short");
        }
        if (this.encrypting) {
            encryptBlock(in, inOff, out, outOff);
            return 8;
        }
        decryptBlock(in, inOff, out, outOff);
        return 8;
    }

    private int rotateWordLeft(int x, int y) {
        int x2 = x & 65535;
        return (x2 << y) | (x2 >> (16 - y));
    }

    private void encryptBlock(byte[] in, int inOff, byte[] out, int outOff) {
        int x76 = ((in[inOff + 7] & 255) << 8) + (in[inOff + 6] & 255);
        int x54 = ((in[inOff + 5] & 255) << 8) + (in[inOff + 4] & 255);
        int x32 = ((in[inOff + 3] & 255) << 8) + (in[inOff + 2] & 255);
        int x10 = ((in[inOff + 1] & 255) << 8) + (in[inOff + 0] & 255);
        for (int i = 0; i <= 16; i += 4) {
            x10 = rotateWordLeft(((x76 ^ (-1)) & x32) + x10 + (x54 & x76) + this.workingKey[i], 1);
            x32 = rotateWordLeft(((x10 ^ (-1)) & x54) + x32 + (x76 & x10) + this.workingKey[i + 1], 2);
            x54 = rotateWordLeft(((x32 ^ (-1)) & x76) + x54 + (x10 & x32) + this.workingKey[i + 2], 3);
            x76 = rotateWordLeft(((x54 ^ (-1)) & x10) + x76 + (x32 & x54) + this.workingKey[i + 3], 5);
        }
        int x102 = x10 + this.workingKey[x76 & 63];
        int x322 = x32 + this.workingKey[x102 & 63];
        int x542 = x54 + this.workingKey[x322 & 63];
        int x762 = x76 + this.workingKey[x542 & 63];
        for (int i2 = 20; i2 <= 40; i2 += 4) {
            x102 = rotateWordLeft(((x762 ^ (-1)) & x322) + x102 + (x542 & x762) + this.workingKey[i2], 1);
            x322 = rotateWordLeft(((x102 ^ (-1)) & x542) + x322 + (x762 & x102) + this.workingKey[i2 + 1], 2);
            x542 = rotateWordLeft(((x322 ^ (-1)) & x762) + x542 + (x102 & x322) + this.workingKey[i2 + 2], 3);
            x762 = rotateWordLeft(((x542 ^ (-1)) & x102) + x762 + (x322 & x542) + this.workingKey[i2 + 3], 5);
        }
        int x103 = x102 + this.workingKey[x762 & 63];
        int x323 = x322 + this.workingKey[x103 & 63];
        int x543 = x542 + this.workingKey[x323 & 63];
        int x763 = x762 + this.workingKey[x543 & 63];
        for (int i3 = 44; i3 < 64; i3 += 4) {
            x103 = rotateWordLeft(((x763 ^ (-1)) & x323) + x103 + (x543 & x763) + this.workingKey[i3], 1);
            x323 = rotateWordLeft(((x103 ^ (-1)) & x543) + x323 + (x763 & x103) + this.workingKey[i3 + 1], 2);
            x543 = rotateWordLeft(((x323 ^ (-1)) & x763) + x543 + (x103 & x323) + this.workingKey[i3 + 2], 3);
            x763 = rotateWordLeft(((x543 ^ (-1)) & x103) + x763 + (x323 & x543) + this.workingKey[i3 + 3], 5);
        }
        out[outOff + 0] = (byte) x103;
        out[outOff + 1] = (byte) (x103 >> 8);
        out[outOff + 2] = (byte) x323;
        out[outOff + 3] = (byte) (x323 >> 8);
        out[outOff + 4] = (byte) x543;
        out[outOff + 5] = (byte) (x543 >> 8);
        out[outOff + 6] = (byte) x763;
        out[outOff + 7] = (byte) (x763 >> 8);
    }

    private void decryptBlock(byte[] in, int inOff, byte[] out, int outOff) {
        int x76 = ((in[inOff + 7] & 255) << 8) + (in[inOff + 6] & 255);
        int x54 = ((in[inOff + 5] & 255) << 8) + (in[inOff + 4] & 255);
        int x32 = ((in[inOff + 3] & 255) << 8) + (in[inOff + 2] & 255);
        int x10 = ((in[inOff + 1] & 255) << 8) + (in[inOff + 0] & 255);
        for (int i = 60; i >= 44; i -= 4) {
            x76 = rotateWordLeft(x76, 11) - ((((x54 ^ (-1)) & x10) + (x32 & x54)) + this.workingKey[i + 3]);
            x54 = rotateWordLeft(x54, 13) - ((((x32 ^ (-1)) & x76) + (x10 & x32)) + this.workingKey[i + 2]);
            x32 = rotateWordLeft(x32, 14) - ((((x10 ^ (-1)) & x54) + (x76 & x10)) + this.workingKey[i + 1]);
            x10 = rotateWordLeft(x10, 15) - ((((x76 ^ (-1)) & x32) + (x54 & x76)) + this.workingKey[i]);
        }
        int x762 = x76 - this.workingKey[x54 & 63];
        int x542 = x54 - this.workingKey[x32 & 63];
        int x322 = x32 - this.workingKey[x10 & 63];
        int x102 = x10 - this.workingKey[x762 & 63];
        for (int i2 = 40; i2 >= 20; i2 -= 4) {
            x762 = rotateWordLeft(x762, 11) - ((((x542 ^ (-1)) & x102) + (x322 & x542)) + this.workingKey[i2 + 3]);
            x542 = rotateWordLeft(x542, 13) - ((((x322 ^ (-1)) & x762) + (x102 & x322)) + this.workingKey[i2 + 2]);
            x322 = rotateWordLeft(x322, 14) - ((((x102 ^ (-1)) & x542) + (x762 & x102)) + this.workingKey[i2 + 1]);
            x102 = rotateWordLeft(x102, 15) - ((((x762 ^ (-1)) & x322) + (x542 & x762)) + this.workingKey[i2]);
        }
        int x763 = x762 - this.workingKey[x542 & 63];
        int x543 = x542 - this.workingKey[x322 & 63];
        int x323 = x322 - this.workingKey[x102 & 63];
        int x103 = x102 - this.workingKey[x763 & 63];
        for (int i3 = 16; i3 >= 0; i3 -= 4) {
            x763 = rotateWordLeft(x763, 11) - ((((x543 ^ (-1)) & x103) + (x323 & x543)) + this.workingKey[i3 + 3]);
            x543 = rotateWordLeft(x543, 13) - ((((x323 ^ (-1)) & x763) + (x103 & x323)) + this.workingKey[i3 + 2]);
            x323 = rotateWordLeft(x323, 14) - ((((x103 ^ (-1)) & x543) + (x763 & x103)) + this.workingKey[i3 + 1]);
            x103 = rotateWordLeft(x103, 15) - ((((x763 ^ (-1)) & x323) + (x543 & x763)) + this.workingKey[i3]);
        }
        out[outOff + 0] = (byte) x103;
        out[outOff + 1] = (byte) (x103 >> 8);
        out[outOff + 2] = (byte) x323;
        out[outOff + 3] = (byte) (x323 >> 8);
        out[outOff + 4] = (byte) x543;
        out[outOff + 5] = (byte) (x543 >> 8);
        out[outOff + 6] = (byte) x763;
        out[outOff + 7] = (byte) (x763 >> 8);
    }
}
