package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.ExtendedDigest;
import com.android.org.bouncycastle.crypto.PBEParametersGenerator;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;

public class PKCS12ParametersGenerator extends PBEParametersGenerator {
    public static final int IV_MATERIAL = 2;
    public static final int KEY_MATERIAL = 1;
    public static final int MAC_MATERIAL = 3;
    private Digest digest;
    private int u;
    private int v;

    public PKCS12ParametersGenerator(Digest digest) {
        this.digest = digest;
        if (digest instanceof ExtendedDigest) {
            this.u = digest.getDigestSize();
            this.v = ((ExtendedDigest) digest).getByteLength();
        } else {
            throw new IllegalArgumentException("Digest " + digest.getAlgorithmName() + " unsupported");
        }
    }

    private void adjust(byte[] bArr, int i, byte[] bArr2) {
        int i2 = (bArr2[bArr2.length - 1] & 255) + (bArr[(bArr2.length + i) - 1] & 255) + 1;
        bArr[(bArr2.length + i) - 1] = (byte) i2;
        int i3 = i2 >>> 8;
        for (int length = bArr2.length - 2; length >= 0; length--) {
            int i4 = i + length;
            int i5 = i3 + (bArr2[length] & 255) + (bArr[i4] & 255);
            bArr[i4] = (byte) i5;
            i3 = i5 >>> 8;
        }
    }

    private byte[] generateDerivedKey(int i, int i2) {
        byte[] bArr;
        byte[] bArr2;
        byte[] bArr3 = new byte[this.v];
        byte[] bArr4 = new byte[i2];
        for (int i3 = 0; i3 != bArr3.length; i3++) {
            bArr3[i3] = (byte) i;
        }
        if (this.salt != null && this.salt.length != 0) {
            bArr = new byte[this.v * (((this.salt.length + this.v) - 1) / this.v)];
            for (int i4 = 0; i4 != bArr.length; i4++) {
                bArr[i4] = this.salt[i4 % this.salt.length];
            }
        } else {
            bArr = new byte[0];
        }
        if (this.password != null && this.password.length != 0) {
            bArr2 = new byte[this.v * (((this.password.length + this.v) - 1) / this.v)];
            for (int i5 = 0; i5 != bArr2.length; i5++) {
                bArr2[i5] = this.password[i5 % this.password.length];
            }
        } else {
            bArr2 = new byte[0];
        }
        byte[] bArr5 = new byte[bArr.length + bArr2.length];
        System.arraycopy(bArr, 0, bArr5, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr5, bArr.length, bArr2.length);
        byte[] bArr6 = new byte[this.v];
        int i6 = ((i2 + this.u) - 1) / this.u;
        byte[] bArr7 = new byte[this.u];
        for (int i7 = 1; i7 <= i6; i7++) {
            this.digest.update(bArr3, 0, bArr3.length);
            this.digest.update(bArr5, 0, bArr5.length);
            this.digest.doFinal(bArr7, 0);
            for (int i8 = 1; i8 < this.iterationCount; i8++) {
                this.digest.update(bArr7, 0, bArr7.length);
                this.digest.doFinal(bArr7, 0);
            }
            for (int i9 = 0; i9 != bArr6.length; i9++) {
                bArr6[i9] = bArr7[i9 % bArr7.length];
            }
            for (int i10 = 0; i10 != bArr5.length / this.v; i10++) {
                adjust(bArr5, this.v * i10, bArr6);
            }
            if (i7 != i6) {
                System.arraycopy(bArr7, 0, bArr4, (i7 - 1) * this.u, bArr7.length);
            } else {
                int i11 = i7 - 1;
                System.arraycopy(bArr7, 0, bArr4, this.u * i11, bArr4.length - (i11 * this.u));
            }
        }
        return bArr4;
    }

    @Override
    public CipherParameters generateDerivedParameters(int i) {
        int i2 = i / 8;
        return new KeyParameter(generateDerivedKey(1, i2), 0, i2);
    }

    @Override
    public CipherParameters generateDerivedParameters(int i, int i2) {
        int i3 = i / 8;
        int i4 = i2 / 8;
        byte[] bArrGenerateDerivedKey = generateDerivedKey(1, i3);
        return new ParametersWithIV(new KeyParameter(bArrGenerateDerivedKey, 0, i3), generateDerivedKey(2, i4), 0, i4);
    }

    @Override
    public CipherParameters generateDerivedMacParameters(int i) {
        int i2 = i / 8;
        return new KeyParameter(generateDerivedKey(3, i2), 0, i2);
    }
}
