package java.util.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ZipInputStream extends InflaterInputStream implements ZipConstants {
    private static final int DEFLATED = 8;
    private static final int STORED = 0;
    private byte[] b;
    private boolean closed;
    private CRC32 crc;
    private ZipEntry entry;
    private boolean entryEOF;
    private int flag;
    private long remaining;
    private byte[] tmpbuf;
    private ZipCoder zc;

    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    public ZipInputStream(InputStream inputStream) {
        this(inputStream, StandardCharsets.UTF_8);
    }

    public ZipInputStream(InputStream inputStream, Charset charset) {
        super(new PushbackInputStream(inputStream, 512), new Inflater(true), 512);
        this.crc = new CRC32();
        this.tmpbuf = new byte[512];
        this.closed = false;
        this.entryEOF = false;
        this.b = new byte[256];
        if (inputStream == null) {
            throw new NullPointerException("in is null");
        }
        if (charset == null) {
            throw new NullPointerException("charset is null");
        }
        this.zc = ZipCoder.get(charset);
    }

    public ZipEntry getNextEntry() throws IOException {
        ensureOpen();
        if (this.entry != null) {
            closeEntry();
        }
        this.crc.reset();
        this.inf.reset();
        ZipEntry loc = readLOC();
        this.entry = loc;
        if (loc == null) {
            return null;
        }
        if (this.entry.method == 0 || this.entry.method == 8) {
            this.remaining = this.entry.size;
        }
        this.entryEOF = false;
        return this.entry;
    }

    public void closeEntry() throws IOException {
        ensureOpen();
        while (read(this.tmpbuf, 0, this.tmpbuf.length) != -1) {
        }
        this.entryEOF = true;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        if (this.entryEOF) {
            return 0;
        }
        if (this.entry != null && this.remaining == 0) {
            return 0;
        }
        return 1;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        ensureOpen();
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        if (this.entry == null) {
            return -1;
        }
        int i3 = this.entry.method;
        if (i3 != 0) {
            if (i3 == 8) {
                int i4 = super.read(bArr, i, i2);
                if (i4 == -1) {
                    readEnd(this.entry);
                    this.entryEOF = true;
                    this.entry = null;
                } else {
                    this.crc.update(bArr, i, i4);
                    this.remaining -= (long) i4;
                }
                return i4;
            }
            throw new ZipException("invalid compression method");
        }
        if (this.remaining <= 0) {
            this.entryEOF = true;
            this.entry = null;
            return -1;
        }
        if (i2 > this.remaining) {
            i2 = (int) this.remaining;
        }
        int i5 = this.in.read(bArr, i, i2);
        if (i5 == -1) {
            throw new ZipException("unexpected EOF");
        }
        this.crc.update(bArr, i, i5);
        this.remaining -= (long) i5;
        if (this.remaining == 0 && this.entry.crc != this.crc.getValue()) {
            throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(this.entry.crc) + " but got 0x" + Long.toHexString(this.crc.getValue()) + ")");
        }
        return i5;
    }

    @Override
    public long skip(long j) throws IOException {
        if (j < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        int iMin = (int) Math.min(j, 2147483647L);
        int i = 0;
        while (true) {
            if (i >= iMin) {
                break;
            }
            int length = iMin - i;
            if (length > this.tmpbuf.length) {
                length = this.tmpbuf.length;
            }
            int i2 = read(this.tmpbuf, 0, length);
            if (i2 == -1) {
                this.entryEOF = true;
                break;
            }
            i += i2;
        }
        return i;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            super.close();
            this.closed = true;
        }
    }

    private ZipEntry readLOC() throws IOException {
        String string;
        try {
            readFully(this.tmpbuf, 0, 30);
            if (ZipUtils.get32(this.tmpbuf, 0) != ZipConstants.LOCSIG) {
                return null;
            }
            this.flag = ZipUtils.get16(this.tmpbuf, 6);
            int i = ZipUtils.get16(this.tmpbuf, 26);
            int length = this.b.length;
            if (i > length) {
                do {
                    length *= 2;
                } while (i > length);
                this.b = new byte[length];
            }
            readFully(this.b, 0, i);
            if ((this.flag & 2048) != 0) {
                string = this.zc.toStringUTF8(this.b, i);
            } else {
                string = this.zc.toString(this.b, i);
            }
            ZipEntry zipEntryCreateZipEntry = createZipEntry(string);
            boolean z = true;
            if ((this.flag & 1) == 1) {
                throw new ZipException("encrypted ZIP entry not supported");
            }
            zipEntryCreateZipEntry.method = ZipUtils.get16(this.tmpbuf, 8);
            zipEntryCreateZipEntry.xdostime = ZipUtils.get32(this.tmpbuf, 10);
            if ((this.flag & 8) == 8) {
                if (zipEntryCreateZipEntry.method != 8) {
                    throw new ZipException("only DEFLATED entries can have EXT descriptor");
                }
            } else {
                zipEntryCreateZipEntry.crc = ZipUtils.get32(this.tmpbuf, 14);
                zipEntryCreateZipEntry.csize = ZipUtils.get32(this.tmpbuf, 18);
                zipEntryCreateZipEntry.size = ZipUtils.get32(this.tmpbuf, 22);
            }
            int i2 = ZipUtils.get16(this.tmpbuf, 28);
            if (i2 > 0) {
                byte[] bArr = new byte[i2];
                readFully(bArr, 0, i2);
                if (zipEntryCreateZipEntry.csize != 4294967295L && zipEntryCreateZipEntry.size != 4294967295L) {
                    z = false;
                }
                zipEntryCreateZipEntry.setExtra0(bArr, z);
            }
            return zipEntryCreateZipEntry;
        } catch (EOFException e) {
            return null;
        }
    }

    protected ZipEntry createZipEntry(String str) {
        return new ZipEntry(str);
    }

    private void readEnd(ZipEntry zipEntry) throws IOException {
        int remaining = this.inf.getRemaining();
        if (remaining > 0) {
            ((PushbackInputStream) this.in).unread(this.buf, this.len - remaining, remaining);
        }
        if ((this.flag & 8) == 8) {
            if (this.inf.getBytesWritten() > 4294967295L || this.inf.getBytesRead() > 4294967295L) {
                readFully(this.tmpbuf, 0, 24);
                long j = ZipUtils.get32(this.tmpbuf, 0);
                if (j != ZipConstants.EXTSIG) {
                    zipEntry.crc = j;
                    zipEntry.csize = ZipUtils.get64(this.tmpbuf, 4);
                    zipEntry.size = ZipUtils.get64(this.tmpbuf, 12);
                    ((PushbackInputStream) this.in).unread(this.tmpbuf, 19, 4);
                } else {
                    zipEntry.crc = ZipUtils.get32(this.tmpbuf, 4);
                    zipEntry.csize = ZipUtils.get64(this.tmpbuf, 8);
                    zipEntry.size = ZipUtils.get64(this.tmpbuf, 16);
                }
            } else {
                readFully(this.tmpbuf, 0, 16);
                long j2 = ZipUtils.get32(this.tmpbuf, 0);
                if (j2 != ZipConstants.EXTSIG) {
                    zipEntry.crc = j2;
                    zipEntry.csize = ZipUtils.get32(this.tmpbuf, 4);
                    zipEntry.size = ZipUtils.get32(this.tmpbuf, 8);
                    ((PushbackInputStream) this.in).unread(this.tmpbuf, 11, 4);
                } else {
                    zipEntry.crc = ZipUtils.get32(this.tmpbuf, 4);
                    zipEntry.csize = ZipUtils.get32(this.tmpbuf, 8);
                    zipEntry.size = ZipUtils.get32(this.tmpbuf, 12);
                }
            }
        }
        if (zipEntry.size != this.inf.getBytesWritten()) {
            throw new ZipException("invalid entry size (expected " + zipEntry.size + " but got " + this.inf.getBytesWritten() + " bytes)");
        }
        if (zipEntry.csize != this.inf.getBytesRead()) {
            throw new ZipException("invalid entry compressed size (expected " + zipEntry.csize + " but got " + this.inf.getBytesRead() + " bytes)");
        }
        if (zipEntry.crc != this.crc.getValue()) {
            throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(zipEntry.crc) + " but got 0x" + Long.toHexString(this.crc.getValue()) + ")");
        }
    }

    private void readFully(byte[] bArr, int i, int i2) throws IOException {
        while (i2 > 0) {
            int i3 = this.in.read(bArr, i, i2);
            if (i3 == -1) {
                throw new EOFException();
            }
            i += i3;
            i2 -= i3;
        }
    }
}
