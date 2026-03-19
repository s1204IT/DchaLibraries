package java.util.zip;

import java.io.IOException;
import java.io.OutputStream;

public class GZIPOutputStream extends DeflaterOutputStream {
    private static final int GZIP_MAGIC = 35615;
    private static final int TRAILER_SIZE = 8;
    protected CRC32 crc;

    public GZIPOutputStream(OutputStream outputStream, int i) throws IOException {
        this(outputStream, i, false);
    }

    public GZIPOutputStream(OutputStream outputStream, int i, boolean z) throws IOException {
        super(outputStream, new Deflater(-1, true), i, z);
        this.crc = new CRC32();
        this.usesDefaultDeflater = true;
        writeHeader();
        this.crc.reset();
    }

    public GZIPOutputStream(OutputStream outputStream) throws IOException {
        this(outputStream, 512, false);
    }

    public GZIPOutputStream(OutputStream outputStream, boolean z) throws IOException {
        this(outputStream, 512, z);
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
        super.write(bArr, i, i2);
        this.crc.update(bArr, i, i2);
    }

    @Override
    public void finish() throws IOException {
        if (!this.def.finished()) {
            this.def.finish();
            while (!this.def.finished()) {
                int iDeflate = this.def.deflate(this.buf, 0, this.buf.length);
                if (this.def.finished() && iDeflate <= this.buf.length - 8) {
                    writeTrailer(this.buf, iDeflate);
                    this.out.write(this.buf, 0, iDeflate + 8);
                    return;
                }
                if (iDeflate > 0) {
                    this.out.write(this.buf, 0, iDeflate);
                }
            }
            byte[] bArr = new byte[8];
            writeTrailer(bArr, 0);
            this.out.write(bArr);
        }
    }

    private void writeHeader() throws IOException {
        this.out.write(new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0});
    }

    private void writeTrailer(byte[] bArr, int i) throws IOException {
        writeInt((int) this.crc.getValue(), bArr, i);
        writeInt(this.def.getTotalIn(), bArr, i + 4);
    }

    private void writeInt(int i, byte[] bArr, int i2) throws IOException {
        writeShort(i & 65535, bArr, i2);
        writeShort((i >> 16) & 65535, bArr, i2 + 2);
    }

    private void writeShort(int i, byte[] bArr, int i2) throws IOException {
        bArr[i2] = (byte) (i & 255);
        bArr[i2 + 1] = (byte) ((i >> 8) & 255);
    }
}
