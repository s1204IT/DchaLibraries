package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CheckedOutputStream extends FilterOutputStream {
    private final Checksum check;

    public CheckedOutputStream(OutputStream os, Checksum cs) {
        super(os);
        this.check = cs;
    }

    public Checksum getChecksum() {
        return this.check;
    }

    @Override
    public void write(int val) throws IOException {
        this.out.write(val);
        this.check.update(val);
    }

    @Override
    public void write(byte[] buf, int off, int nbytes) throws IOException {
        this.out.write(buf, off, nbytes);
        this.check.update(buf, off, nbytes);
    }
}
