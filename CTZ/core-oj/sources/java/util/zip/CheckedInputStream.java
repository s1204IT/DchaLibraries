package java.util.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CheckedInputStream extends FilterInputStream {
    private Checksum cksum;

    public CheckedInputStream(InputStream inputStream, Checksum checksum) {
        super(inputStream);
        this.cksum = checksum;
    }

    @Override
    public int read() throws IOException {
        int i = this.in.read();
        if (i != -1) {
            this.cksum.update(i);
        }
        return i;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3 = this.in.read(bArr, i, i2);
        if (i3 != -1) {
            this.cksum.update(bArr, i, i3);
        }
        return i3;
    }

    @Override
    public long skip(long j) throws IOException {
        byte[] bArr = new byte[512];
        long j2 = 0;
        while (j2 < j) {
            long j3 = j - j2;
            long j4 = read(bArr, 0, j3 < ((long) bArr.length) ? (int) j3 : bArr.length);
            if (j4 == -1) {
                return j2;
            }
            j2 += j4;
        }
        return j2;
    }

    public Checksum getChecksum() {
        return this.cksum;
    }
}
