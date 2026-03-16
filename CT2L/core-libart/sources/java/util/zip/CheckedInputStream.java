package java.util.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import libcore.io.Streams;

public class CheckedInputStream extends FilterInputStream {
    private final Checksum check;

    public CheckedInputStream(InputStream is, Checksum csum) {
        super(is);
        this.check = csum;
    }

    @Override
    public int read() throws IOException {
        int x = this.in.read();
        if (x != -1) {
            this.check.update(x);
        }
        return x;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int bytesRead = this.in.read(buffer, byteOffset, byteCount);
        if (bytesRead != -1) {
            this.check.update(buffer, byteOffset, bytesRead);
        }
        return bytesRead;
    }

    public Checksum getChecksum() {
        return this.check;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return Streams.skipByReading(this, byteCount);
    }
}
