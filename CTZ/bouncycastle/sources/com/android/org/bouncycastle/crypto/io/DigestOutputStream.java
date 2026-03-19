package com.android.org.bouncycastle.crypto.io;

import com.android.org.bouncycastle.crypto.Digest;
import java.io.IOException;
import java.io.OutputStream;

public class DigestOutputStream extends OutputStream {
    protected Digest digest;

    public DigestOutputStream(Digest digest) {
        this.digest = digest;
    }

    @Override
    public void write(int i) throws IOException {
        this.digest.update((byte) i);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        this.digest.update(bArr, i, i2);
    }

    public byte[] getDigest() {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        return bArr;
    }
}
