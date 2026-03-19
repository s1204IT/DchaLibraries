package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.PBEParametersGenerator;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;

public class PKCS5S1ParametersGenerator extends PBEParametersGenerator {
    private Digest digest;

    public PKCS5S1ParametersGenerator(Digest digest) {
        this.digest = digest;
    }

    private byte[] generateDerivedKey() {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.update(this.password, 0, this.password.length);
        this.digest.update(this.salt, 0, this.salt.length);
        this.digest.doFinal(bArr, 0);
        for (int i = 1; i < this.iterationCount; i++) {
            this.digest.update(bArr, 0, bArr.length);
            this.digest.doFinal(bArr, 0);
        }
        return bArr;
    }

    @Override
    public CipherParameters generateDerivedParameters(int i) {
        int i2 = i / 8;
        if (i2 > this.digest.getDigestSize()) {
            throw new IllegalArgumentException("Can't generate a derived key " + i2 + " bytes long.");
        }
        return new KeyParameter(generateDerivedKey(), 0, i2);
    }

    @Override
    public CipherParameters generateDerivedParameters(int i, int i2) {
        int i3 = i / 8;
        int i4 = i2 / 8;
        int i5 = i3 + i4;
        if (i5 > this.digest.getDigestSize()) {
            throw new IllegalArgumentException("Can't generate a derived key " + i5 + " bytes long.");
        }
        byte[] bArrGenerateDerivedKey = generateDerivedKey();
        return new ParametersWithIV(new KeyParameter(bArrGenerateDerivedKey, 0, i3), bArrGenerateDerivedKey, i3, i4);
    }

    @Override
    public CipherParameters generateDerivedMacParameters(int i) {
        return generateDerivedParameters(i);
    }
}
