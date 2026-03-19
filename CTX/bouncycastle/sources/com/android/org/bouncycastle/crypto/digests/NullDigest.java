package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.crypto.Digest;
import java.io.ByteArrayOutputStream;

public class NullDigest implements Digest {
    private ByteArrayOutputStream bOut = new ByteArrayOutputStream();

    @Override
    public String getAlgorithmName() {
        return "NULL";
    }

    @Override
    public int getDigestSize() {
        return this.bOut.size();
    }

    @Override
    public void update(byte b) {
        this.bOut.write(b);
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
        this.bOut.write(bArr, i, i2);
    }

    @Override
    public int doFinal(byte[] bArr, int i) {
        byte[] byteArray = this.bOut.toByteArray();
        System.arraycopy(byteArray, 0, bArr, i, byteArray.length);
        reset();
        return byteArray.length;
    }

    @Override
    public void reset() {
        this.bOut.reset();
    }
}
