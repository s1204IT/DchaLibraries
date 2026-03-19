package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.math.ec.Tnaf;

public final class TwofishEngine implements BlockCipher {
    private static final int BLOCK_SIZE = 16;
    private static final int GF256_FDBK = 361;
    private static final int GF256_FDBK_2 = 180;
    private static final int GF256_FDBK_4 = 90;
    private static final int INPUT_WHITEN = 0;
    private static final int MAX_KEY_BITS = 256;
    private static final int MAX_ROUNDS = 16;
    private static final int OUTPUT_WHITEN = 4;
    private static final byte[][] P = {new byte[]{-87, 103, -77, -24, 4, -3, -93, 118, -102, -110, -128, 120, -28, -35, -47, 56, 13, -58, 53, -104, 24, -9, -20, 108, 67, 117, 55, 38, -6, 19, -108, 72, -14, -48, -117, 48, -124, 84, -33, 35, 25, 91, 61, 89, -13, -82, -94, -126, 99, 1, -125, 46, -39, 81, -101, 124, -90, -21, -91, -66, 22, 12, -29, 97, -64, -116, 58, -11, 115, 44, 37, 11, -69, 78, -119, 107, 83, 106, -76, -15, -31, -26, -67, 69, -30, -12, -74, 102, -52, -107, 3, 86, -44, 28, 30, -41, -5, -61, -114, -75, -23, -49, -65, -70, -22, 119, 57, -81, 51, -55, 98, 113, -127, 121, 9, -83, 36, -51, -7, -40, -27, -59, -71, 77, 68, 8, -122, -25, -95, 29, -86, -19, 6, 112, -78, -46, 65, 123, -96, 17, 49, -62, 39, -112, 32, -10, 96, -1, -106, 92, -79, -85, -98, -100, 82, 27, 95, -109, 10, -17, -111, -123, 73, -18, 45, 79, -113, 59, 71, -121, 109, 70, -42, 62, 105, 100, 42, -50, -53, 47, -4, -105, 5, 122, -84, 127, -43, 26, 75, 14, -89, 90, 40, 20, 63, 41, -120, 60, 76, 2, -72, -38, -80, 23, 85, 31, -118, 125, 87, -57, -115, 116, -73, -60, -97, 114, 126, 21, 34, 18, 88, 7, -103, 52, 110, 80, -34, 104, 101, -68, -37, -8, -56, -88, 43, 64, -36, -2, 50, -92, -54, Tnaf.POW_2_WIDTH, 33, -16, -45, 93, 15, 0, 111, -99, 54, 66, 74, 94, -63, -32}, new byte[]{117, -13, -58, -12, -37, 123, -5, -56, 74, -45, -26, 107, 69, 125, -24, 75, -42, 50, -40, -3, 55, 113, -15, -31, 48, 15, -8, 27, -121, -6, 6, 63, 94, -70, -82, 91, -118, 0, -68, -99, 109, -63, -79, 14, -128, 93, -46, -43, -96, -124, 7, 20, -75, -112, 44, -93, -78, 115, 76, 84, -110, 116, 54, 81, 56, -80, -67, 90, -4, 96, 98, -106, 108, 66, -9, Tnaf.POW_2_WIDTH, 124, 40, 39, -116, 19, -107, -100, -57, 36, 70, 59, 112, -54, -29, -123, -53, 17, -48, -109, -72, -90, -125, 32, -1, -97, 119, -61, -52, 3, 111, 8, -65, 64, -25, 43, -30, 121, 12, -86, -126, 65, 58, -22, -71, -28, -102, -92, -105, 126, -38, 122, 23, 102, -108, -95, 29, 61, -16, -34, -77, 11, 114, -89, 28, -17, -47, 83, 62, -113, 51, 38, 95, -20, 118, 42, 73, -127, -120, -18, 33, -60, 26, -21, -39, -59, 57, -103, -51, -83, 49, -117, 1, 24, 35, -35, 31, 78, 45, -7, 72, 79, -14, 101, -114, 120, 92, 88, 25, -115, -27, -104, 87, 103, 127, 5, 100, -81, 99, -74, -2, -11, -73, 60, -91, -50, -23, 104, 68, -32, 77, 67, 105, 41, 46, -84, 21, 89, -88, 10, -98, 110, 71, -33, 52, 53, 106, -49, -36, 34, -55, -64, -101, -119, -44, -19, -85, 18, -94, 13, 82, -69, 2, 47, -87, -41, 97, 30, -76, 80, 4, -10, -62, 22, 37, -122, 86, 85, 9, -66, -111}};
    private static final int P_00 = 1;
    private static final int P_01 = 0;
    private static final int P_02 = 0;
    private static final int P_03 = 1;
    private static final int P_04 = 1;
    private static final int P_10 = 0;
    private static final int P_11 = 0;
    private static final int P_12 = 1;
    private static final int P_13 = 1;
    private static final int P_14 = 0;
    private static final int P_20 = 1;
    private static final int P_21 = 1;
    private static final int P_22 = 0;
    private static final int P_23 = 0;
    private static final int P_24 = 0;
    private static final int P_30 = 0;
    private static final int P_31 = 1;
    private static final int P_32 = 1;
    private static final int P_33 = 0;
    private static final int P_34 = 1;
    private static final int ROUNDS = 16;
    private static final int ROUND_SUBKEYS = 8;
    private static final int RS_GF_FDBK = 333;
    private static final int SK_BUMP = 16843009;
    private static final int SK_ROTL = 9;
    private static final int SK_STEP = 33686018;
    private static final int TOTAL_SUBKEYS = 40;
    private int[] gSBox;
    private int[] gSubKeys;
    private boolean encrypting = false;
    private int[] gMDS0 = new int[MAX_KEY_BITS];
    private int[] gMDS1 = new int[MAX_KEY_BITS];
    private int[] gMDS2 = new int[MAX_KEY_BITS];
    private int[] gMDS3 = new int[MAX_KEY_BITS];
    private int k64Cnt = 0;
    private byte[] workingKey = null;

    public TwofishEngine() {
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        int[] iArr3 = new int[2];
        for (int i = 0; i < MAX_KEY_BITS; i++) {
            int i2 = P[0][i] & 255;
            iArr[0] = i2;
            iArr2[0] = Mx_X(i2) & 255;
            iArr3[0] = Mx_Y(i2) & 255;
            int i3 = P[1][i] & 255;
            iArr[1] = i3;
            iArr2[1] = Mx_X(i3) & 255;
            iArr3[1] = Mx_Y(i3) & 255;
            this.gMDS0[i] = iArr[1] | (iArr2[1] << 8) | (iArr3[1] << 16) | (iArr3[1] << 24);
            this.gMDS1[i] = iArr3[0] | (iArr3[0] << 8) | (iArr2[0] << 16) | (iArr[0] << 24);
            this.gMDS2[i] = (iArr3[1] << 24) | iArr2[1] | (iArr3[1] << 8) | (iArr[1] << 16);
            this.gMDS3[i] = iArr2[0] | (iArr[0] << 8) | (iArr3[0] << 16) | (iArr2[0] << 24);
        }
    }

    @Override
    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.encrypting = z;
            this.workingKey = ((KeyParameter) cipherParameters).getKey();
            this.k64Cnt = this.workingKey.length / 8;
            setKey(this.workingKey);
            return;
        }
        throw new IllegalArgumentException("invalid parameter passed to Twofish init - " + cipherParameters.getClass().getName());
    }

    @Override
    public String getAlgorithmName() {
        return "Twofish";
    }

    @Override
    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("Twofish not initialised");
        }
        if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        }
        if (i2 + 16 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        }
        if (this.encrypting) {
            encryptBlock(bArr, i, bArr2, i2);
            return 16;
        }
        decryptBlock(bArr, i, bArr2, i2);
        return 16;
    }

    @Override
    public void reset() {
        if (this.workingKey != null) {
            setKey(this.workingKey);
        }
    }

    @Override
    public int getBlockSize() {
        return 16;
    }

    private void setKey(byte[] bArr) {
        int iB0;
        int iB1;
        int iB2;
        int iB3;
        int iB02;
        int iB12;
        int iB22;
        int iB32;
        int[] iArr = new int[4];
        int[] iArr2 = new int[4];
        int[] iArr3 = new int[4];
        this.gSubKeys = new int[TOTAL_SUBKEYS];
        if (this.k64Cnt < 1) {
            throw new IllegalArgumentException("Key size less than 64 bits");
        }
        if (this.k64Cnt > 4) {
            throw new IllegalArgumentException("Key size larger than 256 bits");
        }
        for (int i = 0; i < this.k64Cnt; i++) {
            int i2 = i * 8;
            iArr[i] = BytesTo32Bits(bArr, i2);
            iArr2[i] = BytesTo32Bits(bArr, i2 + 4);
            iArr3[(this.k64Cnt - 1) - i] = RS_MDS_Encode(iArr[i], iArr2[i]);
        }
        for (int i3 = 0; i3 < 20; i3++) {
            int i4 = SK_STEP * i3;
            int iF32 = F32(i4, iArr);
            int iF322 = F32(i4 + 16843009, iArr2);
            int i5 = (iF322 >>> 24) | (iF322 << 8);
            int i6 = iF32 + i5;
            int i7 = i3 * 2;
            this.gSubKeys[i7] = i6;
            int i8 = i6 + i5;
            this.gSubKeys[i7 + 1] = (i8 >>> 23) | (i8 << 9);
        }
        int i9 = iArr3[0];
        int i10 = iArr3[1];
        int i11 = iArr3[2];
        int i12 = iArr3[3];
        this.gSBox = new int[1024];
        for (int i13 = 0; i13 < MAX_KEY_BITS; i13++) {
            switch (this.k64Cnt & 3) {
                case 0:
                    iB0 = (P[1][i13] & 255) ^ b0(i12);
                    iB1 = (P[0][i13] & 255) ^ b1(i12);
                    iB2 = (P[0][i13] & 255) ^ b2(i12);
                    iB3 = (P[1][i13] & 255) ^ b3(i12);
                    iB02 = (P[1][iB0] & 255) ^ b0(i11);
                    iB12 = (P[1][iB1] & 255) ^ b1(i11);
                    iB22 = (P[0][iB2] & 255) ^ b2(i11);
                    iB32 = (P[0][iB3] & 255) ^ b3(i11);
                    int i14 = i13 * 2;
                    this.gSBox[i14] = this.gMDS0[(P[0][(P[0][iB02] & 255) ^ b0(i10)] & 255) ^ b0(i9)];
                    this.gSBox[i14 + 1] = this.gMDS1[(P[0][(P[1][iB12] & 255) ^ b1(i10)] & 255) ^ b1(i9)];
                    this.gSBox[i14 + 512] = this.gMDS2[(P[1][(P[0][iB22] & 255) ^ b2(i10)] & 255) ^ b2(i9)];
                    this.gSBox[i14 + 513] = this.gMDS3[(P[1][(P[1][iB32] & 255) ^ b3(i10)] & 255) ^ b3(i9)];
                    break;
                case 1:
                    int i15 = i13 * 2;
                    this.gSBox[i15] = this.gMDS0[(P[0][i13] & 255) ^ b0(i9)];
                    this.gSBox[i15 + 1] = this.gMDS1[(P[0][i13] & 255) ^ b1(i9)];
                    this.gSBox[i15 + 512] = this.gMDS2[(P[1][i13] & 255) ^ b2(i9)];
                    this.gSBox[i15 + 513] = this.gMDS3[(P[1][i13] & 255) ^ b3(i9)];
                    break;
                case 2:
                    iB02 = i13;
                    iB12 = iB02;
                    iB22 = iB12;
                    iB32 = iB22;
                    int i142 = i13 * 2;
                    this.gSBox[i142] = this.gMDS0[(P[0][(P[0][iB02] & 255) ^ b0(i10)] & 255) ^ b0(i9)];
                    this.gSBox[i142 + 1] = this.gMDS1[(P[0][(P[1][iB12] & 255) ^ b1(i10)] & 255) ^ b1(i9)];
                    this.gSBox[i142 + 512] = this.gMDS2[(P[1][(P[0][iB22] & 255) ^ b2(i10)] & 255) ^ b2(i9)];
                    this.gSBox[i142 + 513] = this.gMDS3[(P[1][(P[1][iB32] & 255) ^ b3(i10)] & 255) ^ b3(i9)];
                    break;
                case 3:
                    iB0 = i13;
                    iB1 = iB0;
                    iB2 = iB1;
                    iB3 = iB2;
                    iB02 = (P[1][iB0] & 255) ^ b0(i11);
                    iB12 = (P[1][iB1] & 255) ^ b1(i11);
                    iB22 = (P[0][iB2] & 255) ^ b2(i11);
                    iB32 = (P[0][iB3] & 255) ^ b3(i11);
                    int i1422 = i13 * 2;
                    this.gSBox[i1422] = this.gMDS0[(P[0][(P[0][iB02] & 255) ^ b0(i10)] & 255) ^ b0(i9)];
                    this.gSBox[i1422 + 1] = this.gMDS1[(P[0][(P[1][iB12] & 255) ^ b1(i10)] & 255) ^ b1(i9)];
                    this.gSBox[i1422 + 512] = this.gMDS2[(P[1][(P[0][iB22] & 255) ^ b2(i10)] & 255) ^ b2(i9)];
                    this.gSBox[i1422 + 513] = this.gMDS3[(P[1][(P[1][iB32] & 255) ^ b3(i10)] & 255) ^ b3(i9)];
                    break;
            }
        }
    }

    private void encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3 = 0;
        int iBytesTo32Bits = BytesTo32Bits(bArr, i) ^ this.gSubKeys[0];
        int iBytesTo32Bits2 = BytesTo32Bits(bArr, i + 4) ^ this.gSubKeys[1];
        int iBytesTo32Bits3 = BytesTo32Bits(bArr, i + 8) ^ this.gSubKeys[2];
        int iBytesTo32Bits4 = BytesTo32Bits(bArr, i + 12) ^ this.gSubKeys[3];
        int i4 = 8;
        while (i3 < 16) {
            int iFe32_0 = Fe32_0(iBytesTo32Bits);
            int iFe32_3 = Fe32_3(iBytesTo32Bits2);
            int i5 = i4 + 1;
            int i6 = iBytesTo32Bits3 ^ ((iFe32_0 + iFe32_3) + this.gSubKeys[i4]);
            iBytesTo32Bits3 = (i6 >>> 1) | (i6 << 31);
            int i7 = iFe32_0 + (iFe32_3 * 2);
            int i8 = i5 + 1;
            iBytesTo32Bits4 = ((iBytesTo32Bits4 >>> 31) | (iBytesTo32Bits4 << 1)) ^ (i7 + this.gSubKeys[i5]);
            int iFe32_02 = Fe32_0(iBytesTo32Bits3);
            int iFe32_32 = Fe32_3(iBytesTo32Bits4);
            int i9 = i8 + 1;
            int i10 = iBytesTo32Bits ^ ((iFe32_02 + iFe32_32) + this.gSubKeys[i8]);
            iBytesTo32Bits = (i10 << 31) | (i10 >>> 1);
            iBytesTo32Bits2 = ((iBytesTo32Bits2 >>> 31) | (iBytesTo32Bits2 << 1)) ^ ((iFe32_02 + (iFe32_32 * 2)) + this.gSubKeys[i9]);
            i3 += 2;
            i4 = i9 + 1;
        }
        Bits32ToBytes(this.gSubKeys[4] ^ iBytesTo32Bits3, bArr2, i2);
        Bits32ToBytes(iBytesTo32Bits4 ^ this.gSubKeys[5], bArr2, i2 + 4);
        Bits32ToBytes(this.gSubKeys[6] ^ iBytesTo32Bits, bArr2, i2 + 8);
        Bits32ToBytes(this.gSubKeys[7] ^ iBytesTo32Bits2, bArr2, i2 + 12);
    }

    private void decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int iBytesTo32Bits = BytesTo32Bits(bArr, i) ^ this.gSubKeys[4];
        int iBytesTo32Bits2 = BytesTo32Bits(bArr, i + 4) ^ this.gSubKeys[5];
        int i3 = 39;
        int iBytesTo32Bits3 = BytesTo32Bits(bArr, i + 8) ^ this.gSubKeys[6];
        int iBytesTo32Bits4 = BytesTo32Bits(bArr, i + 12) ^ this.gSubKeys[7];
        int i4 = 0;
        while (i4 < 16) {
            int iFe32_0 = Fe32_0(iBytesTo32Bits);
            int iFe32_3 = Fe32_3(iBytesTo32Bits2);
            int i5 = i3 - 1;
            int i6 = iBytesTo32Bits4 ^ (((2 * iFe32_3) + iFe32_0) + this.gSubKeys[i3]);
            int i7 = iFe32_0 + iFe32_3;
            int i8 = i5 - 1;
            iBytesTo32Bits3 = ((iBytesTo32Bits3 << 1) | (iBytesTo32Bits3 >>> 31)) ^ (i7 + this.gSubKeys[i5]);
            iBytesTo32Bits4 = (i6 << 31) | (i6 >>> 1);
            int iFe32_02 = Fe32_0(iBytesTo32Bits3);
            int iFe32_32 = Fe32_3(iBytesTo32Bits4);
            int i9 = i8 - 1;
            int i10 = iBytesTo32Bits2 ^ (((2 * iFe32_32) + iFe32_02) + this.gSubKeys[i8]);
            iBytesTo32Bits = ((iBytesTo32Bits >>> 31) | (iBytesTo32Bits << 1)) ^ ((iFe32_02 + iFe32_32) + this.gSubKeys[i9]);
            iBytesTo32Bits2 = (i10 << 31) | (i10 >>> 1);
            i4 += 2;
            i3 = i9 - 1;
        }
        Bits32ToBytes(this.gSubKeys[0] ^ iBytesTo32Bits3, bArr2, i2);
        Bits32ToBytes(this.gSubKeys[1] ^ iBytesTo32Bits4, bArr2, i2 + 4);
        Bits32ToBytes(this.gSubKeys[2] ^ iBytesTo32Bits, bArr2, i2 + 8);
        Bits32ToBytes(this.gSubKeys[3] ^ iBytesTo32Bits2, bArr2, i2 + 12);
    }

    private int F32(int i, int[] iArr) {
        int iB0 = b0(i);
        int iB1 = b1(i);
        int iB2 = b2(i);
        int iB3 = b3(i);
        int i2 = iArr[0];
        int i3 = iArr[1];
        int i4 = iArr[2];
        int i5 = iArr[3];
        switch (3 & this.k64Cnt) {
            case 0:
                iB0 = (P[1][iB0] & 255) ^ b0(i5);
                iB1 = (P[0][iB1] & 255) ^ b1(i5);
                iB2 = (P[0][iB2] & 255) ^ b2(i5);
                iB3 = (P[1][iB3] & 255) ^ b3(i5);
                break;
            case 1:
                return ((this.gMDS0[(P[0][iB0] & 255) ^ b0(i2)] ^ this.gMDS1[(P[0][iB1] & 255) ^ b1(i2)]) ^ this.gMDS2[(P[1][iB2] & 255) ^ b2(i2)]) ^ this.gMDS3[(P[1][iB3] & 255) ^ b3(i2)];
            case 3:
                break;
            case 2:
                return ((this.gMDS0[(P[0][(P[0][iB0] & 255) ^ b0(i3)] & 255) ^ b0(i2)] ^ this.gMDS1[(P[0][(P[1][iB1] & 255) ^ b1(i3)] & 255) ^ b1(i2)]) ^ this.gMDS2[(P[1][(P[0][iB2] & 255) ^ b2(i3)] & 255) ^ b2(i2)]) ^ this.gMDS3[(P[1][(P[1][iB3] & 255) ^ b3(i3)] & 255) ^ b3(i2)];
            default:
                return 0;
        }
        iB0 = b0(i4) ^ (P[1][iB0] & 255);
        iB1 = b1(i4) ^ (P[1][iB1] & 255);
        iB2 = b2(i4) ^ (P[0][iB2] & 255);
        iB3 = (P[0][iB3] & 255) ^ b3(i4);
        return ((this.gMDS0[(P[0][(P[0][iB0] & 255) ^ b0(i3)] & 255) ^ b0(i2)] ^ this.gMDS1[(P[0][(P[1][iB1] & 255) ^ b1(i3)] & 255) ^ b1(i2)]) ^ this.gMDS2[(P[1][(P[0][iB2] & 255) ^ b2(i3)] & 255) ^ b2(i2)]) ^ this.gMDS3[(P[1][(P[1][iB3] & 255) ^ b3(i3)] & 255) ^ b3(i2)];
    }

    private int RS_MDS_Encode(int i, int i2) {
        int iRS_rem = i2;
        for (int i3 = 0; i3 < 4; i3++) {
            iRS_rem = RS_rem(iRS_rem);
        }
        int iRS_rem2 = i ^ iRS_rem;
        for (int i4 = 0; i4 < 4; i4++) {
            iRS_rem2 = RS_rem(iRS_rem2);
        }
        return iRS_rem2;
    }

    private int RS_rem(int i) {
        int i2 = (i >>> 24) & 255;
        int i3 = ((i2 << 1) ^ ((i2 & 128) != 0 ? RS_GF_FDBK : 0)) & 255;
        int i4 = ((i2 >>> 1) ^ ((i2 & 1) != 0 ? 166 : 0)) ^ i3;
        return ((((i << 8) ^ (i4 << 24)) ^ (i3 << 16)) ^ (i4 << 8)) ^ i2;
    }

    private int LFSR1(int i) {
        return ((i & 1) != 0 ? GF256_FDBK_2 : 0) ^ (i >> 1);
    }

    private int LFSR2(int i) {
        return ((i >> 2) ^ ((i & 2) != 0 ? GF256_FDBK_2 : 0)) ^ ((i & 1) != 0 ? GF256_FDBK_4 : 0);
    }

    private int Mx_X(int i) {
        return i ^ LFSR2(i);
    }

    private int Mx_Y(int i) {
        return LFSR2(i) ^ (LFSR1(i) ^ i);
    }

    private int b0(int i) {
        return i & 255;
    }

    private int b1(int i) {
        return (i >>> 8) & 255;
    }

    private int b2(int i) {
        return (i >>> 16) & 255;
    }

    private int b3(int i) {
        return (i >>> 24) & 255;
    }

    private int Fe32_0(int i) {
        return this.gSBox[513 + (2 * ((i >>> 24) & 255))] ^ ((this.gSBox[0 + ((i & 255) * 2)] ^ this.gSBox[1 + (((i >>> 8) & 255) * 2)]) ^ this.gSBox[512 + (((i >>> 16) & 255) * 2)]);
    }

    private int Fe32_3(int i) {
        return this.gSBox[513 + (2 * ((i >>> 16) & 255))] ^ ((this.gSBox[0 + (((i >>> 24) & 255) * 2)] ^ this.gSBox[1 + ((i & 255) * 2)]) ^ this.gSBox[512 + (((i >>> 8) & 255) * 2)]);
    }

    private int BytesTo32Bits(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 24) | (bArr[i] & 255) | ((bArr[i + 1] & 255) << 8) | ((bArr[i + 2] & 255) << 16);
    }

    private void Bits32ToBytes(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        bArr[i2 + 1] = (byte) (i >> 8);
        bArr[i2 + 2] = (byte) (i >> 16);
        bArr[i2 + 3] = (byte) (i >> 24);
    }
}
