package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.util.Memoable;
import com.android.org.bouncycastle.util.Pack;

public class SHA256Digest extends GeneralDigest implements EncodableDigest {
    private static final int DIGEST_LENGTH = 32;
    static final int[] K = {1116352408, 1899447441, -1245643825, -373957723, 961987163, 1508970993, -1841331548, -1424204075, -670586216, 310598401, 607225278, 1426881987, 1925078388, -2132889090, -1680079193, -1046744716, -459576895, -272742522, 264347078, 604807628, 770255983, 1249150122, 1555081692, 1996064986, -1740746414, -1473132947, -1341970488, -1084653625, -958395405, -710438585, 113926993, 338241895, 666307205, 773529912, 1294757372, 1396182291, 1695183700, 1986661051, -2117940946, -1838011259, -1564481375, -1474664885, -1035236496, -949202525, -778901479, -694614492, -200395387, 275423344, 430227734, 506948616, 659060556, 883997877, 958139571, 1322822218, 1537002063, 1747873779, 1955562222, 2024104815, -2067236844, -1933114872, -1866530822, -1538233109, -1090935817, -965641998};
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int H5;
    private int H6;
    private int H7;
    private int H8;
    private int[] X;
    private int xOff;

    public SHA256Digest() {
        this.X = new int[64];
        reset();
    }

    public SHA256Digest(SHA256Digest sHA256Digest) {
        super(sHA256Digest);
        this.X = new int[64];
        copyIn(sHA256Digest);
    }

    private void copyIn(SHA256Digest sHA256Digest) {
        super.copyIn((GeneralDigest) sHA256Digest);
        this.H1 = sHA256Digest.H1;
        this.H2 = sHA256Digest.H2;
        this.H3 = sHA256Digest.H3;
        this.H4 = sHA256Digest.H4;
        this.H5 = sHA256Digest.H5;
        this.H6 = sHA256Digest.H6;
        this.H7 = sHA256Digest.H7;
        this.H8 = sHA256Digest.H8;
        System.arraycopy(sHA256Digest.X, 0, this.X, 0, sHA256Digest.X.length);
        this.xOff = sHA256Digest.xOff;
    }

    public SHA256Digest(byte[] bArr) {
        super(bArr);
        this.X = new int[64];
        this.H1 = Pack.bigEndianToInt(bArr, 16);
        this.H2 = Pack.bigEndianToInt(bArr, 20);
        this.H3 = Pack.bigEndianToInt(bArr, 24);
        this.H4 = Pack.bigEndianToInt(bArr, 28);
        this.H5 = Pack.bigEndianToInt(bArr, 32);
        this.H6 = Pack.bigEndianToInt(bArr, 36);
        this.H7 = Pack.bigEndianToInt(bArr, 40);
        this.H8 = Pack.bigEndianToInt(bArr, 44);
        this.xOff = Pack.bigEndianToInt(bArr, 48);
        for (int i = 0; i != this.xOff; i++) {
            this.X[i] = Pack.bigEndianToInt(bArr, 52 + (i * 4));
        }
    }

    @Override
    public String getAlgorithmName() {
        return "SHA-256";
    }

    @Override
    public int getDigestSize() {
        return 32;
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
        Pack.intToBigEndian(this.H6, bArr, i + 20);
        Pack.intToBigEndian(this.H7, bArr, i + 24);
        Pack.intToBigEndian(this.H8, bArr, i + 28);
        reset();
        return 32;
    }

    @Override
    public void reset() {
        super.reset();
        this.H1 = 1779033703;
        this.H2 = -1150833019;
        this.H3 = 1013904242;
        this.H4 = -1521486534;
        this.H5 = 1359893119;
        this.H6 = -1694144372;
        this.H7 = 528734635;
        this.H8 = 1541459225;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    @Override
    protected void processBlock() {
        for (int i = 16; i <= 63; i++) {
            this.X[i] = Theta1(this.X[i - 2]) + this.X[i - 7] + Theta0(this.X[i - 15]) + this.X[i - 16];
        }
        int i2 = this.H1;
        int i3 = this.H2;
        int i4 = this.H3;
        int iSum0 = this.H4;
        int i5 = this.H5;
        int i6 = this.H6;
        int i7 = this.H7;
        int i8 = this.H8;
        int iSum02 = i3;
        int iSum03 = i4;
        int i9 = 0;
        int iSum04 = i2;
        for (int i10 = 0; i10 < 8; i10++) {
            int iSum1 = i8 + Sum1(i5) + Ch(i5, i6, i7) + K[i9] + this.X[i9];
            int i11 = iSum0 + iSum1;
            int iSum05 = iSum1 + Sum0(iSum04) + Maj(iSum04, iSum02, iSum03);
            int i12 = i9 + 1;
            int iSum12 = i7 + Sum1(i11) + Ch(i11, i5, i6) + K[i12] + this.X[i12];
            int i13 = iSum03 + iSum12;
            int iSum06 = iSum12 + Sum0(iSum05) + Maj(iSum05, iSum04, iSum02);
            int i14 = i12 + 1;
            int iSum13 = i6 + Sum1(i13) + Ch(i13, i11, i5) + K[i14] + this.X[i14];
            int i15 = iSum02 + iSum13;
            int iSum07 = iSum13 + Sum0(iSum06) + Maj(iSum06, iSum05, iSum04);
            int i16 = i14 + 1;
            int iSum14 = i5 + Sum1(i15) + Ch(i15, i13, i11) + K[i16] + this.X[i16];
            int i17 = iSum04 + iSum14;
            int iSum08 = iSum14 + Sum0(iSum07) + Maj(iSum07, iSum06, iSum05);
            int i18 = i16 + 1;
            int iSum15 = i11 + Sum1(i17) + Ch(i17, i15, i13) + K[i18] + this.X[i18];
            i8 = iSum05 + iSum15;
            iSum0 = iSum15 + Sum0(iSum08) + Maj(iSum08, iSum07, iSum06);
            int i19 = i18 + 1;
            int iSum16 = i13 + Sum1(i8) + Ch(i8, i17, i15) + K[i19] + this.X[i19];
            i7 = iSum06 + iSum16;
            iSum03 = iSum16 + Sum0(iSum0) + Maj(iSum0, iSum08, iSum07);
            int i20 = i19 + 1;
            int iSum17 = i15 + Sum1(i7) + Ch(i7, i8, i17) + K[i20] + this.X[i20];
            i6 = iSum07 + iSum17;
            iSum02 = iSum17 + Sum0(iSum03) + Maj(iSum03, iSum0, iSum08);
            int i21 = i20 + 1;
            int iSum18 = i17 + Sum1(i6) + Ch(i6, i7, i8) + K[i21] + this.X[i21];
            i5 = iSum08 + iSum18;
            iSum04 = iSum18 + Sum0(iSum02) + Maj(iSum02, iSum03, iSum0);
            i9 = i21 + 1;
        }
        this.H1 += iSum04;
        this.H2 += iSum02;
        this.H3 += iSum03;
        this.H4 += iSum0;
        this.H5 += i5;
        this.H6 += i6;
        this.H7 += i7;
        this.H8 += i8;
        this.xOff = 0;
        for (int i22 = 0; i22 < 16; i22++) {
            this.X[i22] = 0;
        }
    }

    private int Ch(int i, int i2, int i3) {
        return ((~i) & i3) ^ (i2 & i);
    }

    private int Maj(int i, int i2, int i3) {
        return ((i & i3) ^ (i & i2)) ^ (i2 & i3);
    }

    private int Sum0(int i) {
        return ((i << 10) | (i >>> 22)) ^ (((i >>> 2) | (i << 30)) ^ ((i >>> 13) | (i << 19)));
    }

    private int Sum1(int i) {
        return ((i << 7) | (i >>> 25)) ^ (((i >>> 6) | (i << 26)) ^ ((i >>> 11) | (i << 21)));
    }

    private int Theta0(int i) {
        return (i >>> 3) ^ (((i >>> 7) | (i << 25)) ^ ((i >>> 18) | (i << 14)));
    }

    private int Theta1(int i) {
        return (i >>> 10) ^ (((i >>> 17) | (i << 15)) ^ ((i >>> 19) | (i << 13)));
    }

    @Override
    public Memoable copy() {
        return new SHA256Digest(this);
    }

    @Override
    public void reset(Memoable memoable) {
        copyIn((SHA256Digest) memoable);
    }

    @Override
    public byte[] getEncodedState() {
        byte[] bArr = new byte[(this.xOff * 4) + 52];
        super.populateState(bArr);
        Pack.intToBigEndian(this.H1, bArr, 16);
        Pack.intToBigEndian(this.H2, bArr, 20);
        Pack.intToBigEndian(this.H3, bArr, 24);
        Pack.intToBigEndian(this.H4, bArr, 28);
        Pack.intToBigEndian(this.H5, bArr, 32);
        Pack.intToBigEndian(this.H6, bArr, 36);
        Pack.intToBigEndian(this.H7, bArr, 40);
        Pack.intToBigEndian(this.H8, bArr, 44);
        Pack.intToBigEndian(this.xOff, bArr, 48);
        for (int i = 0; i != this.xOff; i++) {
            Pack.intToBigEndian(this.X[i], bArr, (i * 4) + 52);
        }
        return bArr;
    }
}
