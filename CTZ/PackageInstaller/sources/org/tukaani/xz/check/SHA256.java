package org.tukaani.xz.check;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 extends Check {
    private final MessageDigest sha256;

    public SHA256() throws NoSuchAlgorithmException {
        this.size = 32;
        this.name = "SHA-256";
        this.sha256 = MessageDigest.getInstance("SHA-256");
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
        this.sha256.update(bArr, i, i2);
    }

    @Override
    public byte[] finish() {
        byte[] bArrDigest = this.sha256.digest();
        this.sha256.reset();
        return bArrDigest;
    }
}
