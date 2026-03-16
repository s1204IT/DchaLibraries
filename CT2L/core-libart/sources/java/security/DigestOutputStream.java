package java.security;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DigestOutputStream extends FilterOutputStream {
    protected MessageDigest digest;
    private boolean isOn;

    public DigestOutputStream(OutputStream stream, MessageDigest digest) {
        super(stream);
        this.isOn = true;
        this.digest = digest;
    }

    public MessageDigest getMessageDigest() {
        return this.digest;
    }

    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public void write(int b) throws IOException {
        if (this.isOn) {
            this.digest.update((byte) b);
        }
        this.out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (this.isOn) {
            this.digest.update(b, off, len);
        }
        this.out.write(b, off, len);
    }

    public void on(boolean on) {
        this.isOn = on;
    }

    public String toString() {
        return super.toString() + ", " + this.digest.toString() + (this.isOn ? ", is on" : ", is off");
    }
}
