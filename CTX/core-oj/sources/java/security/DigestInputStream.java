package java.security;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DigestInputStream extends FilterInputStream {
    protected MessageDigest digest;
    private boolean on;

    public DigestInputStream(InputStream inputStream, MessageDigest messageDigest) {
        super(inputStream);
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
    public int read() throws IOException {
        int i = this.in.read();
        if (this.on && i != -1) {
            this.digest.update((byte) i);
        }
        return i;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3 = this.in.read(bArr, i, i2);
        if (this.on && i3 != -1) {
            this.digest.update(bArr, i, i3);
        }
        return i3;
    }

    public void on(boolean z) {
        this.on = z;
    }

    public String toString() {
        return "[Digest Input Stream] " + this.digest.toString();
    }
}
