package java.util.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class ZipOutputStream extends DeflaterOutputStream implements ZipConstants {
    public static final int DEFLATED = 8;
    public static final int STORED = 0;
    private static final boolean inhibitZip64 = false;
    private boolean closed;
    private byte[] comment;
    private CRC32 crc;
    private XEntry current;
    private boolean finished;
    private long locoff;
    private int method;
    private HashSet<String> names;
    private long written;
    private Vector<XEntry> xentries;
    private final ZipCoder zc;

    private static class XEntry {
        final ZipEntry entry;
        final long offset;

        public XEntry(ZipEntry zipEntry, long j) {
            this.entry = zipEntry;
            this.offset = j;
        }
    }

    private static int version(ZipEntry zipEntry) throws ZipException {
        int i = zipEntry.method;
        if (i == 0) {
            return 10;
        }
        if (i == 8) {
            return 20;
        }
        throw new ZipException("unsupported compression method");
    }

    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    public ZipOutputStream(OutputStream outputStream) {
        this(outputStream, StandardCharsets.UTF_8);
    }

    public ZipOutputStream(OutputStream outputStream, Charset charset) {
        super(outputStream, new Deflater(-1, true));
        this.xentries = new Vector<>();
        this.names = new HashSet<>();
        this.crc = new CRC32();
        this.written = 0L;
        this.locoff = 0L;
        this.method = 8;
        this.closed = inhibitZip64;
        if (charset == null) {
            throw new NullPointerException("charset is null");
        }
        this.zc = ZipCoder.get(charset);
        this.usesDefaultDeflater = true;
    }

    public void setComment(String str) {
        if (str != null) {
            this.comment = this.zc.getBytes(str);
            if (this.comment.length > 65535) {
                throw new IllegalArgumentException("ZIP file comment too long.");
            }
        }
    }

    public void setMethod(int i) {
        if (i != 8 && i != 0) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = i;
    }

    public void setLevel(int i) {
        this.def.setLevel(i);
    }

    public void putNextEntry(ZipEntry zipEntry) throws IOException {
        ensureOpen();
        if (this.current != null) {
            closeEntry();
        }
        if (zipEntry.xdostime == -1) {
            zipEntry.setTime(System.currentTimeMillis());
        }
        if (zipEntry.method == -1) {
            zipEntry.method = this.method;
        }
        zipEntry.flag = 0;
        int i = zipEntry.method;
        if (i == 0) {
            if (zipEntry.size == -1) {
                zipEntry.size = zipEntry.csize;
            } else if (zipEntry.csize == -1) {
                zipEntry.csize = zipEntry.size;
            } else if (zipEntry.size != zipEntry.csize) {
                throw new ZipException("STORED entry where compressed != uncompressed size");
            }
            if (zipEntry.size == -1 || zipEntry.crc == -1) {
                throw new ZipException("STORED entry missing size, compressed size, or crc-32");
            }
        } else {
            if (i != 8) {
                throw new ZipException("unsupported compression method");
            }
            if (zipEntry.size == -1 || zipEntry.csize == -1 || zipEntry.crc == -1) {
                zipEntry.flag = 8;
            }
        }
        if (!this.names.add(zipEntry.name)) {
            throw new ZipException("duplicate entry: " + zipEntry.name);
        }
        if (this.zc.isUTF8()) {
            zipEntry.flag |= 2048;
        }
        this.current = new XEntry(zipEntry, this.written);
        this.xentries.add(this.current);
        writeLOC(this.current);
    }

    public void closeEntry() throws IOException {
        ensureOpen();
        if (this.current != null) {
            ZipEntry zipEntry = this.current.entry;
            int i = zipEntry.method;
            if (i != 0) {
                if (i == 8) {
                    this.def.finish();
                    while (!this.def.finished()) {
                        deflate();
                    }
                    if ((zipEntry.flag & 8) != 0) {
                        zipEntry.size = this.def.getBytesRead();
                        zipEntry.csize = this.def.getBytesWritten();
                        zipEntry.crc = this.crc.getValue();
                        writeEXT(zipEntry);
                    } else {
                        if (zipEntry.size != this.def.getBytesRead()) {
                            throw new ZipException("invalid entry size (expected " + zipEntry.size + " but got " + this.def.getBytesRead() + " bytes)");
                        }
                        if (zipEntry.csize != this.def.getBytesWritten()) {
                            throw new ZipException("invalid entry compressed size (expected " + zipEntry.csize + " but got " + this.def.getBytesWritten() + " bytes)");
                        }
                        if (zipEntry.crc != this.crc.getValue()) {
                            throw new ZipException("invalid entry CRC-32 (expected 0x" + Long.toHexString(zipEntry.crc) + " but got 0x" + Long.toHexString(this.crc.getValue()) + ")");
                        }
                    }
                    this.def.reset();
                    this.written += zipEntry.csize;
                } else {
                    throw new ZipException("invalid compression method");
                }
            } else {
                if (zipEntry.size != this.written - this.locoff) {
                    throw new ZipException("invalid entry size (expected " + zipEntry.size + " but got " + (this.written - this.locoff) + " bytes)");
                }
                if (zipEntry.crc != this.crc.getValue()) {
                    throw new ZipException("invalid entry crc-32 (expected 0x" + Long.toHexString(zipEntry.crc) + " but got 0x" + Long.toHexString(this.crc.getValue()) + ")");
                }
            }
            this.crc.reset();
            this.current = null;
        }
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
        ensureOpen();
        if (i < 0 || i2 < 0 || i > bArr.length - i2) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return;
        }
        if (this.current == null) {
            throw new ZipException("no current ZIP entry");
        }
        ZipEntry zipEntry = this.current.entry;
        int i3 = zipEntry.method;
        if (i3 == 0) {
            this.written += (long) i2;
            if (this.written - this.locoff > zipEntry.size) {
                throw new ZipException("attempt to write past end of STORED entry");
            }
            this.out.write(bArr, i, i2);
        } else if (i3 == 8) {
            super.write(bArr, i, i2);
        } else {
            throw new ZipException("invalid compression method");
        }
        this.crc.update(bArr, i, i2);
    }

    @Override
    public void finish() throws IOException {
        ensureOpen();
        if (this.finished) {
            return;
        }
        if (this.xentries.isEmpty()) {
            throw new ZipException("No entries");
        }
        if (this.current != null) {
            closeEntry();
        }
        long j = this.written;
        Iterator<XEntry> it = this.xentries.iterator();
        while (it.hasNext()) {
            writeCEN(it.next());
        }
        writeEND(j, this.written - j);
        this.finished = true;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            super.close();
            this.closed = true;
        }
    }

    private void writeLOC(XEntry xEntry) throws IOException {
        boolean z;
        int i;
        int i2;
        ZipEntry zipEntry = xEntry.entry;
        int i3 = zipEntry.flag;
        int extraLen = getExtraLen(zipEntry.extra);
        writeInt(ZipConstants.LOCSIG);
        if ((i3 & 8) == 8) {
            writeShort(version(zipEntry));
            writeShort(i3);
            writeShort(zipEntry.method);
            writeInt(zipEntry.xdostime);
            writeInt(0L);
            writeInt(0L);
            writeInt(0L);
            z = false;
        } else {
            if (zipEntry.csize >= 4294967295L || zipEntry.size >= 4294967295L) {
                writeShort(45);
                z = true;
            } else {
                writeShort(version(zipEntry));
                z = false;
            }
            writeShort(i3);
            writeShort(zipEntry.method);
            writeInt(zipEntry.xdostime);
            writeInt(zipEntry.crc);
            if (z) {
                writeInt(4294967295L);
                writeInt(4294967295L);
                extraLen += 20;
            } else {
                writeInt(zipEntry.csize);
                writeInt(zipEntry.size);
            }
        }
        byte[] bytes = this.zc.getBytes(zipEntry.name);
        writeShort(bytes.length);
        if (zipEntry.mtime != null) {
            i = 4;
            i2 = 1;
        } else {
            i = 0;
            i2 = 0;
        }
        if (zipEntry.atime != null) {
            i += 4;
            i2 |= 2;
        }
        if (zipEntry.ctime != null) {
            i += 4;
            i2 |= 4;
        }
        if (i2 != 0) {
            extraLen += i + 5;
        }
        writeShort(extraLen);
        writeBytes(bytes, 0, bytes.length);
        if (z) {
            writeShort(1);
            writeShort(16);
            writeLong(zipEntry.size);
            writeLong(zipEntry.csize);
        }
        if (i2 != 0) {
            writeShort(21589);
            writeShort(i + 1);
            writeByte(i2);
            if (zipEntry.mtime != null) {
                writeInt(ZipUtils.fileTimeToUnixTime(zipEntry.mtime));
            }
            if (zipEntry.atime != null) {
                writeInt(ZipUtils.fileTimeToUnixTime(zipEntry.atime));
            }
            if (zipEntry.ctime != null) {
                writeInt(ZipUtils.fileTimeToUnixTime(zipEntry.ctime));
            }
        }
        writeExtra(zipEntry.extra);
        this.locoff = this.written;
    }

    private void writeEXT(ZipEntry zipEntry) throws IOException {
        writeInt(ZipConstants.EXTSIG);
        writeInt(zipEntry.crc);
        if (zipEntry.csize >= 4294967295L || zipEntry.size >= 4294967295L) {
            writeLong(zipEntry.csize);
            writeLong(zipEntry.size);
        } else {
            writeInt(zipEntry.csize);
            writeInt(zipEntry.size);
        }
    }

    private void writeCEN(XEntry xEntry) throws IOException {
        long j;
        int i;
        boolean z;
        long j2;
        long j3;
        int i2;
        int i3;
        byte[] bytes;
        ZipEntry zipEntry = xEntry.entry;
        int i4 = zipEntry.flag;
        int iVersion = version(zipEntry);
        long j4 = zipEntry.csize;
        long j5 = zipEntry.size;
        long j6 = xEntry.offset;
        if (zipEntry.csize >= 4294967295L) {
            i = 8;
            j = 4294967295L;
            z = true;
        } else {
            j = j4;
            i = 0;
            z = false;
        }
        boolean z2 = z;
        if (zipEntry.size >= 4294967295L) {
            i += 8;
            j2 = 4294967295L;
            z2 = true;
        } else {
            j2 = j5;
        }
        if (xEntry.offset >= 4294967295L) {
            i += 8;
            j3 = 4294967295L;
            z2 = true;
        } else {
            j3 = j6;
        }
        writeInt(ZipConstants.CENSIG);
        if (z2) {
            writeShort(45);
            writeShort(45);
        } else {
            writeShort(iVersion);
            writeShort(iVersion);
        }
        writeShort(i4);
        writeShort(zipEntry.method);
        writeInt(zipEntry.xdostime);
        writeInt(zipEntry.crc);
        long j7 = j;
        writeInt(j7);
        writeInt(j2);
        byte[] bytes2 = this.zc.getBytes(zipEntry.name);
        writeShort(bytes2.length);
        int extraLen = getExtraLen(zipEntry.extra);
        if (z2) {
            extraLen += i + 4;
        }
        if (zipEntry.mtime != null) {
            i2 = extraLen + 4;
            i3 = 1;
        } else {
            i2 = extraLen;
            i3 = 0;
        }
        if (zipEntry.atime != null) {
            i3 |= 2;
        }
        if (zipEntry.ctime != null) {
            i3 |= 4;
        }
        if (i3 != 0) {
            i2 += 5;
        }
        writeShort(i2);
        if (zipEntry.comment != null) {
            bytes = this.zc.getBytes(zipEntry.comment);
            writeShort(Math.min(bytes.length, 65535));
        } else {
            bytes = null;
            writeShort(0);
        }
        writeShort(0);
        writeShort(0);
        writeInt(0L);
        writeInt(j3);
        writeBytes(bytes2, 0, bytes2.length);
        if (z2) {
            writeShort(1);
            writeShort(i);
            if (j2 == 4294967295L) {
                writeLong(zipEntry.size);
            }
            if (j7 == 4294967295L) {
                writeLong(zipEntry.csize);
            }
            if (j3 == 4294967295L) {
                writeLong(xEntry.offset);
            }
        }
        if (i3 != 0) {
            writeShort(21589);
            if (zipEntry.mtime != null) {
                writeShort(5);
                writeByte(i3);
                writeInt(ZipUtils.fileTimeToUnixTime(zipEntry.mtime));
            } else {
                writeShort(1);
                writeByte(i3);
            }
        }
        writeExtra(zipEntry.extra);
        if (bytes != null) {
            writeBytes(bytes, 0, Math.min(bytes.length, 65535));
        }
    }

    private void writeEND(long j, long j2) throws IOException {
        long j3;
        boolean z;
        long j4;
        long j5 = 4294967295L;
        if (j2 < 4294967295L) {
            j3 = j2;
            z = inhibitZip64;
        } else {
            j3 = 4294967295L;
            z = true;
        }
        if (j >= 4294967295L) {
            z = true;
        } else {
            j5 = j;
        }
        int size = this.xentries.size();
        if (size >= 65535 && ((z = z | true))) {
            size = 65535;
        }
        if (z) {
            long j6 = this.written;
            writeInt(101075792L);
            writeLong(44L);
            writeShort(45);
            writeShort(45);
            writeInt(0L);
            writeInt(0L);
            j4 = j3;
            writeLong(this.xentries.size());
            writeLong(this.xentries.size());
            writeLong(j2);
            writeLong(j);
            writeInt(117853008L);
            writeInt(0L);
            writeLong(j6);
            writeInt(1L);
        } else {
            j4 = j3;
        }
        writeInt(ZipConstants.ENDSIG);
        writeShort(0);
        writeShort(0);
        writeShort(size);
        writeShort(size);
        writeInt(j4);
        writeInt(j5);
        if (this.comment != null) {
            writeShort(this.comment.length);
            writeBytes(this.comment, 0, this.comment.length);
        } else {
            writeShort(0);
        }
    }

    private int getExtraLen(byte[] bArr) {
        int i = 0;
        if (bArr == null) {
            return 0;
        }
        int length = bArr.length;
        int i2 = 0;
        while (true) {
            int i3 = i + 4;
            if (i3 > length) {
                break;
            }
            int i4 = ZipUtils.get16(bArr, i);
            int i5 = ZipUtils.get16(bArr, i + 2);
            if (i5 < 0 || i3 + i5 > length) {
                break;
            }
            if (i4 == 21589 || i4 == 1) {
                i2 += i5 + 4;
            }
            i += i5 + 4;
        }
        return length - i2;
    }

    private void writeExtra(byte[] bArr) throws IOException {
        if (bArr != null) {
            int length = bArr.length;
            int i = 0;
            while (true) {
                int i2 = i + 4;
                if (i2 <= length) {
                    int i3 = ZipUtils.get16(bArr, i);
                    int i4 = ZipUtils.get16(bArr, i + 2);
                    if (i4 < 0 || i2 + i4 > length) {
                        break;
                    }
                    if (i3 != 21589 && i3 != 1) {
                        writeBytes(bArr, i, i4 + 4);
                    }
                    i += i4 + 4;
                } else {
                    if (i < length) {
                        writeBytes(bArr, i, length - i);
                        return;
                    }
                    return;
                }
            }
            writeBytes(bArr, i, length - i);
        }
    }

    private void writeByte(int i) throws IOException {
        this.out.write(i & 255);
        this.written++;
    }

    private void writeShort(int i) throws IOException {
        OutputStream outputStream = this.out;
        outputStream.write((i >>> 0) & 255);
        outputStream.write((i >>> 8) & 255);
        this.written += 2;
    }

    private void writeInt(long j) throws IOException {
        OutputStream outputStream = this.out;
        outputStream.write((int) ((j >>> 0) & 255));
        outputStream.write((int) ((j >>> 8) & 255));
        outputStream.write((int) ((j >>> 16) & 255));
        outputStream.write((int) ((j >>> 24) & 255));
        this.written += 4;
    }

    private void writeLong(long j) throws IOException {
        OutputStream outputStream = this.out;
        outputStream.write((int) ((j >>> 0) & 255));
        outputStream.write((int) ((j >>> 8) & 255));
        outputStream.write((int) ((j >>> 16) & 255));
        outputStream.write((int) ((j >>> 24) & 255));
        outputStream.write((int) ((j >>> 32) & 255));
        outputStream.write((int) ((j >>> 40) & 255));
        outputStream.write((int) ((j >>> 48) & 255));
        outputStream.write((int) ((j >>> 56) & 255));
        this.written += 8;
    }

    private void writeBytes(byte[] bArr, int i, int i2) throws IOException {
        this.out.write(bArr, i, i2);
        this.written += (long) i2;
    }
}
