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
    public void update(byte in) {
        this.bOut.write(in);
    }

    @Override
    public void update(byte[] in, int inOff, int len) {
        this.bOut.write(in, inOff, len);
    }

    @Override
    public int doFinal(byte[] out, int outOff) {
        byte[] res = this.bOut.toByteArray();
        System.arraycopy(res, 0, out, outOff, res.length);
        reset();
        return res.length;
    }

    @Override
    public void reset() {
        this.bOut.reset();
    }
}
