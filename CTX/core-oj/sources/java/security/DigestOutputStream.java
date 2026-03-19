package java.security;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DigestOutputStream extends FilterOutputStream {
    protected MessageDigest digest;
    private boolean on;

    public DigestOutputStream(OutputStream outputStream, MessageDigest messageDigest) {
        super(outputStream);
        this.on = true;
        setMessageDigest(messageDigest);
    }

    public MessageDigest getMessageDigest() {
        return this.digest;
    }

    public void setMessageDigest(MessageDigest messageDigest) {
        this.digest = messageDigest;
    }

    @Override
    public void write(int i) throws IOException {
        this.out.write(i);
        if (this.on) {
            this.digest.update((byte) i);
        }
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null || i + i2 > bArr.length) {
            throw new IllegalArgumentException("wrong parameters for write");
        }
        if (i < 0 || i2 < 0) {
            throw new IndexOutOfBoundsException("wrong index for write");
        }
        this.out.write(bArr, i, i2);
        if (this.on) {
            this.digest.update(bArr, i, i2);
        }
    }

    public void on(boolean z) {
        this.on = z;
    }

    public String toString() {
        return "[Digest Output Stream] " + this.digest.toString();
    }
}
