package com.android.org.bouncycastle.jcajce.provider.digest;

import com.android.org.bouncycastle.crypto.Digest;
import java.security.MessageDigest;

public class BCMessageDigest extends MessageDigest {
    protected Digest digest;

    protected BCMessageDigest(Digest digest) {
        super(digest.getAlgorithmName());
        this.digest = digest;
    }

    @Override
    public void engineReset() {
        this.digest.reset();
    }

    @Override
    public void engineUpdate(byte b) {
        this.digest.update(b);
    }

    @Override
    public void engineUpdate(byte[] bArr, int i, int i2) {
        this.digest.update(bArr, i, i2);
    }

    @Override
    public byte[] engineDigest() {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        return bArr;
    }
}
