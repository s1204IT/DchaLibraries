package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CheckedOutputStream extends FilterOutputStream {
    private Checksum cksum;

    public CheckedOutputStream(OutputStream outputStream, Checksum checksum) {
        super(outputStream);
        this.cksum = checksum;
    }

    @Override
    public void write(int i) throws IOException {
        this.out.write(i);
        this.cksum.update(i);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        this.out.write(bArr, i, i2);
        this.cksum.update(bArr, i, i2);
    }

    public Checksum getChecksum() {
        return this.cksum;
    }
}
