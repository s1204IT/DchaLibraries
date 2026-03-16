package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.crypto.util.Pack;
import com.android.org.bouncycastle.util.Memoable;

public class SHA1Digest extends GeneralDigest {
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

    public SHA1Digest(SHA1Digest t) {
        super(t);
        this.X = new int[80];
        copyIn(t);
    }

    private void copyIn(SHA1Digest t) {
        this.H1 = t.H1;
        this.H2 = t.H2;
        this.H3 = t.H3;
        this.H4 = t.H4;
        this.H5 = t.H5;
        System.arraycopy(t.X, 0, this.X, 0, t.X.length);
        this.xOff = t.xOff;
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
    protected void processWord(byte[] in, int inOff) {
        int n = in[inOff] << 24;
        int inOff2 = inOff + 1;
        int n2 = n | ((in[inOff2] & 255) << 16);
        int inOff3 = inOff2 + 1;
        this.X[this.xOff] = n2 | ((in[inOff3] & 255) << 8) | (in[inOff3 + 1] & 255);
        int i = this.xOff + 1;
        this.xOff = i;
        if (i == 16) {
            processBlock();
        }
    }

    @Override
    protected void processLength(long bitLength) {
        if (this.xOff > 14) {
            processBlock();
        }
        this.X[14] = (int) (bitLength >>> 32);
        this.X[15] = (int) ((-1) & bitLength);
    }

    @Override
    public int doFinal(byte[] out, int outOff) {
        finish();
        Pack.intToBigEndian(this.H1, out, outOff);
        Pack.intToBigEndian(this.H2, out, outOff + 4);
        Pack.intToBigEndian(this.H3, out, outOff + 8);
        Pack.intToBigEndian(this.H4, out, outOff + 12);
        Pack.intToBigEndian(this.H5, out, outOff + 16);
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

    private int f(int u, int v, int w) {
        return (u & v) | ((u ^ (-1)) & w);
    }

    private int h(int u, int v, int w) {
        return (u ^ v) ^ w;
    }

    private int g(int u, int v, int w) {
        return (u & v) | (u & w) | (v & w);
    }

    @Override
    protected void processBlock() {
        int idx;
        for (int i = 16; i < 80; i++) {
            int t = ((this.X[i - 3] ^ this.X[i - 8]) ^ this.X[i - 14]) ^ this.X[i - 16];
            this.X[i] = (t << 1) | (t >>> 31);
        }
        int A = this.H1;
        int B = this.H2;
        int C = this.H3;
        int D = this.H4;
        int E = this.H5;
        int idx2 = 0;
        int j = 0;
        while (true) {
            idx = idx2;
            if (j >= 4) {
                break;
            }
            int idx3 = idx + 1;
            int E2 = E + ((A << 5) | (A >>> 27)) + f(B, C, D) + this.X[idx] + Y1;
            int B2 = (B << 30) | (B >>> 2);
            int idx4 = idx3 + 1;
            int D2 = D + ((E2 << 5) | (E2 >>> 27)) + f(A, B2, C) + this.X[idx3] + Y1;
            int A2 = (A << 30) | (A >>> 2);
            int idx5 = idx4 + 1;
            int C2 = C + ((D2 << 5) | (D2 >>> 27)) + f(E2, A2, B2) + this.X[idx4] + Y1;
            E = (E2 << 30) | (E2 >>> 2);
            int idx6 = idx5 + 1;
            B = B2 + ((C2 << 5) | (C2 >>> 27)) + f(D2, E, A2) + this.X[idx5] + Y1;
            D = (D2 << 30) | (D2 >>> 2);
            idx2 = idx6 + 1;
            A = A2 + ((B << 5) | (B >>> 27)) + f(C2, D, E) + this.X[idx6] + Y1;
            C = (C2 << 30) | (C2 >>> 2);
            j++;
        }
        int j2 = 0;
        while (j2 < 4) {
            int idx7 = idx + 1;
            int E3 = E + ((A << 5) | (A >>> 27)) + h(B, C, D) + this.X[idx] + Y2;
            int B3 = (B << 30) | (B >>> 2);
            int idx8 = idx7 + 1;
            int D3 = D + ((E3 << 5) | (E3 >>> 27)) + h(A, B3, C) + this.X[idx7] + Y2;
            int A3 = (A << 30) | (A >>> 2);
            int idx9 = idx8 + 1;
            int C3 = C + ((D3 << 5) | (D3 >>> 27)) + h(E3, A3, B3) + this.X[idx8] + Y2;
            E = (E3 << 30) | (E3 >>> 2);
            int idx10 = idx9 + 1;
            B = B3 + ((C3 << 5) | (C3 >>> 27)) + h(D3, E, A3) + this.X[idx9] + Y2;
            D = (D3 << 30) | (D3 >>> 2);
            A = A3 + ((B << 5) | (B >>> 27)) + h(C3, D, E) + this.X[idx10] + Y2;
            C = (C3 << 30) | (C3 >>> 2);
            j2++;
            idx = idx10 + 1;
        }
        int j3 = 0;
        while (j3 < 4) {
            int idx11 = idx + 1;
            int E4 = E + ((A << 5) | (A >>> 27)) + g(B, C, D) + this.X[idx] + Y3;
            int B4 = (B << 30) | (B >>> 2);
            int idx12 = idx11 + 1;
            int D4 = D + ((E4 << 5) | (E4 >>> 27)) + g(A, B4, C) + this.X[idx11] + Y3;
            int A4 = (A << 30) | (A >>> 2);
            int idx13 = idx12 + 1;
            int C4 = C + ((D4 << 5) | (D4 >>> 27)) + g(E4, A4, B4) + this.X[idx12] + Y3;
            E = (E4 << 30) | (E4 >>> 2);
            int idx14 = idx13 + 1;
            B = B4 + ((C4 << 5) | (C4 >>> 27)) + g(D4, E, A4) + this.X[idx13] + Y3;
            D = (D4 << 30) | (D4 >>> 2);
            A = A4 + ((B << 5) | (B >>> 27)) + g(C4, D, E) + this.X[idx14] + Y3;
            C = (C4 << 30) | (C4 >>> 2);
            j3++;
            idx = idx14 + 1;
        }
        int j4 = 0;
        while (j4 <= 3) {
            int idx15 = idx + 1;
            int E5 = E + ((A << 5) | (A >>> 27)) + h(B, C, D) + this.X[idx] + Y4;
            int B5 = (B << 30) | (B >>> 2);
            int idx16 = idx15 + 1;
            int D5 = D + ((E5 << 5) | (E5 >>> 27)) + h(A, B5, C) + this.X[idx15] + Y4;
            int A5 = (A << 30) | (A >>> 2);
            int idx17 = idx16 + 1;
            int C5 = C + ((D5 << 5) | (D5 >>> 27)) + h(E5, A5, B5) + this.X[idx16] + Y4;
            E = (E5 << 30) | (E5 >>> 2);
            int idx18 = idx17 + 1;
            B = B5 + ((C5 << 5) | (C5 >>> 27)) + h(D5, E, A5) + this.X[idx17] + Y4;
            D = (D5 << 30) | (D5 >>> 2);
            A = A5 + ((B << 5) | (B >>> 27)) + h(C5, D, E) + this.X[idx18] + Y4;
            C = (C5 << 30) | (C5 >>> 2);
            j4++;
            idx = idx18 + 1;
        }
        this.H1 += A;
        this.H2 += B;
        this.H3 += C;
        this.H4 += D;
        this.H5 += E;
        this.xOff = 0;
        for (int i2 = 0; i2 < 16; i2++) {
            this.X[i2] = 0;
        }
    }

    @Override
    public Memoable copy() {
        return new SHA1Digest(this);
    }

    @Override
    public void reset(Memoable other) {
        SHA1Digest d = (SHA1Digest) other;
        super.copyIn((GeneralDigest) d);
        copyIn(d);
    }
}
