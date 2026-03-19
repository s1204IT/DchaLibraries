package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.util.Memoable;
import com.android.org.bouncycastle.util.Pack;

public class SHA1Digest extends GeneralDigest implements EncodableDigest {
    private static final int DIGEST_LENGTH = 20;
    private static final int Y1 = 1518500249;
    private static final int Y2 = 1859775393;
    private static final int Y3 = -1894007588;
    private static final int Y4 = -899497514;
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int H5;
    private int[] X;
    private int xOff;

    public SHA1Digest() {
        this.X = new int[80];
        reset();
    }

    public SHA1Digest(SHA1Digest sHA1Digest) {
        super(sHA1Digest);
        this.X = new int[80];
        copyIn(sHA1Digest);
    }

    public SHA1Digest(byte[] bArr) {
        super(bArr);
        this.X = new int[80];
        this.H1 = Pack.bigEndianToInt(bArr, 16);
        this.H2 = Pack.bigEndianToInt(bArr, 20);
        this.H3 = Pack.bigEndianToInt(bArr, 24);
        this.H4 = Pack.bigEndianToInt(bArr, 28);
        this.H5 = Pack.bigEndianToInt(bArr, 32);
        this.xOff = Pack.bigEndianToInt(bArr, 36);
        for (int i = 0; i != this.xOff; i++) {
            this.X[i] = Pack.bigEndianToInt(bArr, 40 + (i * 4));
        }
    }

    private void copyIn(SHA1Digest sHA1Digest) {
        this.H1 = sHA1Digest.H1;
        this.H2 = sHA1Digest.H2;
        this.H3 = sHA1Digest.H3;
        this.H4 = sHA1Digest.H4;
        this.H5 = sHA1Digest.H5;
        System.arraycopy(sHA1Digest.X, 0, this.X, 0, sHA1Digest.X.length);
        this.xOff = sHA1Digest.xOff;
    }

    @Override
    public String getAlgorithmName() {
        return "SHA-1";
    }

    @Override
    public int getDigestSize() {
        return 20;
    }

    @Override
    protected void processWord(byte[] bArr, int i) {
        int i2 = bArr[i] << 24;
        int i3 = i + 1;
        int i4 = i2 | ((bArr[i3] & 255) << 16);
        int i5 = i3 + 1;
        this.X[this.xOff] = (bArr[i5 + 1] & 255) | i4 | ((bArr[i5] & 255) << 8);
        int i6 = this.xOff + 1;
        this.xOff = i6;
        if (i6 == 16) {
            processBlock();
        }
    }

    @Override
    protected void processLength(long j) {
        if (this.xOff > 14) {
            processBlock();
        }
        this.X[14] = (int) (j >>> 32);
        this.X[15] = (int) (j & (-1));
    }

    @Override
    public int doFinal(byte[] bArr, int i) {
        finish();
        Pack.intToBigEndian(this.H1, bArr, i);
        Pack.intToBigEndian(this.H2, bArr, i + 4);
        Pack.intToBigEndian(this.H3, bArr, i + 8);
        Pack.intToBigEndian(this.H4, bArr, i + 12);
        Pack.intToBigEndian(this.H5, bArr, i + 16);
        reset();
        return 20;
    }

    @Override
    public void reset() {
        super.reset();
        this.H1 = 1732584193;
        this.H2 = -271733879;
        this.H3 = -1732584194;
        this.H4 = 271733878;
        this.H5 = -1009589776;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    private int f(int i, int i2, int i3) {
        return ((~i) & i3) | (i2 & i);
    }

    private int h(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    private int g(int i, int i2, int i3) {
        return (i & i3) | (i & i2) | (i2 & i3);
    }

    @Override
    protected void processBlock() {
        for (int i = 16; i < 80; i++) {
            int i2 = ((this.X[i - 3] ^ this.X[i - 8]) ^ this.X[i - 14]) ^ this.X[i - 16];
            this.X[i] = (i2 >>> 31) | (i2 << 1);
        }
        int i3 = this.H1;
        int i4 = this.H2;
        int i5 = this.H3;
        int i6 = this.H4;
        int i7 = this.H5;
        int i8 = i6;
        int i9 = 0;
        int i10 = i5;
        int iH = i4;
        int iH2 = i3;
        int i11 = 0;
        while (i11 < 4) {
            int i12 = i9 + 1;
            int iF = i7 + ((iH2 << 5) | (iH2 >>> 27)) + f(iH, i10, i8) + this.X[i9] + Y1;
            int i13 = (iH >>> 2) | (iH << 30);
            int i14 = i12 + 1;
            int iF2 = i8 + ((iF << 5) | (iF >>> 27)) + f(iH2, i13, i10) + this.X[i12] + Y1;
            int i15 = (iH2 >>> 2) | (iH2 << 30);
            int i16 = i14 + 1;
            int iF3 = i10 + ((iF2 << 5) | (iF2 >>> 27)) + f(iF, i15, i13) + this.X[i14] + Y1;
            i7 = (iF >>> 2) | (iF << 30);
            int i17 = i16 + 1;
            iH = i13 + ((iF3 << 5) | (iF3 >>> 27)) + f(iF2, i7, i15) + this.X[i16] + Y1;
            i8 = (iF2 >>> 2) | (iF2 << 30);
            iH2 = i15 + ((iH << 5) | (iH >>> 27)) + f(iF3, i8, i7) + this.X[i17] + Y1;
            i10 = (iF3 >>> 2) | (iF3 << 30);
            i11++;
            i9 = i17 + 1;
        }
        int i18 = 0;
        while (i18 < 4) {
            int i19 = i9 + 1;
            int iH3 = i7 + ((iH2 << 5) | (iH2 >>> 27)) + h(iH, i10, i8) + this.X[i9] + Y2;
            int i20 = (iH >>> 2) | (iH << 30);
            int i21 = i19 + 1;
            int iH4 = i8 + ((iH3 << 5) | (iH3 >>> 27)) + h(iH2, i20, i10) + this.X[i19] + Y2;
            int i22 = (iH2 >>> 2) | (iH2 << 30);
            int i23 = i21 + 1;
            int iH5 = i10 + ((iH4 << 5) | (iH4 >>> 27)) + h(iH3, i22, i20) + this.X[i21] + Y2;
            i7 = (iH3 >>> 2) | (iH3 << 30);
            int i24 = i23 + 1;
            iH = i20 + ((iH5 << 5) | (iH5 >>> 27)) + h(iH4, i7, i22) + this.X[i23] + Y2;
            i8 = (iH4 >>> 2) | (iH4 << 30);
            iH2 = i22 + ((iH << 5) | (iH >>> 27)) + h(iH5, i8, i7) + this.X[i24] + Y2;
            i10 = (iH5 >>> 2) | (iH5 << 30);
            i18++;
            i9 = i24 + 1;
        }
        int i25 = 0;
        while (i25 < 4) {
            int i26 = i9 + 1;
            int iG = i7 + ((iH2 << 5) | (iH2 >>> 27)) + g(iH, i10, i8) + this.X[i9] + Y3;
            int i27 = (iH >>> 2) | (iH << 30);
            int i28 = i26 + 1;
            int iG2 = i8 + ((iG << 5) | (iG >>> 27)) + g(iH2, i27, i10) + this.X[i26] + Y3;
            int i29 = (iH2 >>> 2) | (iH2 << 30);
            int i30 = i28 + 1;
            int iG3 = i10 + ((iG2 << 5) | (iG2 >>> 27)) + g(iG, i29, i27) + this.X[i28] + Y3;
            i7 = (iG >>> 2) | (iG << 30);
            int i31 = i30 + 1;
            iH = i27 + ((iG3 << 5) | (iG3 >>> 27)) + g(iG2, i7, i29) + this.X[i30] + Y3;
            i8 = (iG2 >>> 2) | (iG2 << 30);
            iH2 = i29 + ((iH << 5) | (iH >>> 27)) + g(iG3, i8, i7) + this.X[i31] + Y3;
            i10 = (iG3 >>> 2) | (iG3 << 30);
            i25++;
            i9 = i31 + 1;
        }
        int i32 = 0;
        while (i32 <= 3) {
            int i33 = i9 + 1;
            int iH6 = i7 + ((iH2 << 5) | (iH2 >>> 27)) + h(iH, i10, i8) + this.X[i9] + Y4;
            int i34 = (iH >>> 2) | (iH << 30);
            int i35 = i33 + 1;
            int iH7 = i8 + ((iH6 << 5) | (iH6 >>> 27)) + h(iH2, i34, i10) + this.X[i33] + Y4;
            int i36 = (iH2 >>> 2) | (iH2 << 30);
            int i37 = i35 + 1;
            int iH8 = i10 + ((iH7 << 5) | (iH7 >>> 27)) + h(iH6, i36, i34) + this.X[i35] + Y4;
            i7 = (iH6 >>> 2) | (iH6 << 30);
            int i38 = i37 + 1;
            iH = i34 + ((iH8 << 5) | (iH8 >>> 27)) + h(iH7, i7, i36) + this.X[i37] + Y4;
            i8 = (iH7 >>> 2) | (iH7 << 30);
            iH2 = i36 + ((iH << 5) | (iH >>> 27)) + h(iH8, i8, i7) + this.X[i38] + Y4;
            i10 = (iH8 >>> 2) | (iH8 << 30);
            i32++;
            i9 = i38 + 1;
        }
        this.H1 += iH2;
        this.H2 += iH;
        this.H3 += i10;
        this.H4 += i8;
        this.H5 += i7;
        this.xOff = 0;
        for (int i39 = 0; i39 < 16; i39++) {
            this.X[i39] = 0;
        }
    }

    @Override
    public Memoable copy() {
        return new SHA1Digest(this);
    }

    @Override
    public void reset(Memoable memoable) {
        SHA1Digest sHA1Digest = (SHA1Digest) memoable;
        super.copyIn((GeneralDigest) sHA1Digest);
        copyIn(sHA1Digest);
    }

    @Override
    public byte[] getEncodedState() {
        byte[] bArr = new byte[(this.xOff * 4) + 40];
        super.populateState(bArr);
        Pack.intToBigEndian(this.H1, bArr, 16);
        Pack.intToBigEndian(this.H2, bArr, 20);
        Pack.intToBigEndian(this.H3, bArr, 24);
        Pack.intToBigEndian(this.H4, bArr, 28);
        Pack.intToBigEndian(this.H5, bArr, 32);
        Pack.intToBigEndian(this.xOff, bArr, 36);
        for (int i = 0; i != this.xOff; i++) {
            Pack.intToBigEndian(this.X[i], bArr, (i * 4) + 40);
        }
        return bArr;
    }
}
