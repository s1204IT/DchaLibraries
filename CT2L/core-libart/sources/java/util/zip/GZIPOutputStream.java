package java.util.zip;

import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.io.OutputStream;

public class GZIPOutputStream extends DeflaterOutputStream {
    protected CRC32 crc;

    public GZIPOutputStream(OutputStream os) throws IOException {
        this(os, 512, false);
    }

    public GZIPOutputStream(OutputStream os, boolean syncFlush) throws IOException {
        this(os, 512, syncFlush);
    }

    public GZIPOutputStream(OutputStream os, int bufferSize) throws IOException {
        this(os, bufferSize, false);
    }

    public GZIPOutputStream(OutputStream os, int bufferSize, boolean syncFlush) throws IOException {
        super(os, new Deflater(-1, true), bufferSize, syncFlush);
        this.crc = new CRC32();
        writeShort(GZIPInputStream.GZIP_MAGIC);
        this.out.write(8);
        this.out.write(0);
        writeLong(0L);
        this.out.write(0);
        this.out.write(0);
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        writeLong(this.crc.getValue());
        writeLong(this.crc.tbytes);
    }

    @Override
    public void write(byte[] buffer, int off, int nbytes) throws IOException {
        super.write(buffer, off, nbytes);
        this.crc.update(buffer, off, nbytes);
    }

    private long writeLong(long i) throws IOException {
        int unsigned = (int) i;
        this.out.write(unsigned & Opcodes.OP_CONST_CLASS_JUMBO);
        this.out.write((unsigned >> 8) & Opcodes.OP_CONST_CLASS_JUMBO);
        this.out.write((unsigned >> 16) & Opcodes.OP_CONST_CLASS_JUMBO);
        this.out.write((unsigned >> 24) & Opcodes.OP_CONST_CLASS_JUMBO);
        return i;
    }

    private int writeShort(int i) throws IOException {
        this.out.write(i & Opcodes.OP_CONST_CLASS_JUMBO);
        this.out.write((i >> 8) & Opcodes.OP_CONST_CLASS_JUMBO);
        return i;
    }
}
