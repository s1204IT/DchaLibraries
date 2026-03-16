package java.util.zip;

import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.ByteOrder;
import java.nio.charset.ModifiedUtf8;
import java.util.Arrays;
import libcore.io.Memory;
import libcore.io.Streams;

public class ZipInputStream extends InflaterInputStream implements ZipConstants {
    private static final int ZIPLocalHeaderVersionNeeded = 20;
    private final CRC32 crc;
    private ZipEntry currentEntry;
    private boolean entriesEnd;
    private int entryIn;
    private boolean hasDD;
    private final byte[] hdrBuf;
    private int inRead;
    private int lastRead;
    private byte[] stringBytesBuf;
    private char[] stringCharBuf;

    public ZipInputStream(InputStream stream) {
        super(new PushbackInputStream(stream, 512), new Inflater(true));
        this.entriesEnd = false;
        this.hasDD = false;
        this.entryIn = 0;
        this.lastRead = 0;
        this.hdrBuf = new byte[26];
        this.crc = new CRC32();
        this.stringBytesBuf = new byte[256];
        this.stringCharBuf = new char[256];
        if (stream == null) {
            throw new NullPointerException("stream == null");
        }
    }

    @Override
    public void close() throws Exception {
        if (!this.closed) {
            closeEntry();
            super.close();
        }
    }

    public void closeEntry() throws Exception {
        int inB;
        int out;
        checkClosed();
        if (this.currentEntry != null) {
            Exception failure = null;
            try {
                Streams.skipAll(this);
            } catch (Exception e) {
                failure = e;
            }
            if (this.currentEntry.compressionMethod == 8) {
                inB = this.inf.getTotalIn();
                out = this.inf.getTotalOut();
            } else {
                inB = this.inRead;
                out = this.inRead;
            }
            int diff = this.entryIn - inB;
            if (diff != 0) {
                ((PushbackInputStream) this.in).unread(this.buf, this.len - diff, diff);
            }
            try {
                readAndVerifyDataDescriptor(inB, out);
            } catch (Exception e2) {
                if (failure == null) {
                    failure = e2;
                }
            }
            this.inf.reset();
            this.len = 0;
            this.entryIn = 0;
            this.inRead = 0;
            this.lastRead = 0;
            this.crc.reset();
            this.currentEntry = null;
            if (failure != null) {
                if (failure instanceof IOException) {
                    throw ((IOException) failure);
                }
                if (failure instanceof RuntimeException) {
                    throw ((RuntimeException) failure);
                }
                AssertionError error = new AssertionError();
                error.initCause(failure);
                throw error;
            }
        }
    }

    private void readAndVerifyDataDescriptor(int inB, int out) throws IOException {
        if (this.hasDD) {
            Streams.readFully(this.in, this.hdrBuf, 0, 16);
            int sig = Memory.peekInt(this.hdrBuf, 0, ByteOrder.LITTLE_ENDIAN);
            if (sig != 134695760) {
                throw new ZipException(String.format("unknown format (EXTSIG=%x)", Integer.valueOf(sig)));
            }
            this.currentEntry.crc = ((long) Memory.peekInt(this.hdrBuf, 4, ByteOrder.LITTLE_ENDIAN)) & 4294967295L;
            this.currentEntry.compressedSize = ((long) Memory.peekInt(this.hdrBuf, 8, ByteOrder.LITTLE_ENDIAN)) & 4294967295L;
            this.currentEntry.size = ((long) Memory.peekInt(this.hdrBuf, 12, ByteOrder.LITTLE_ENDIAN)) & 4294967295L;
        }
        if (this.currentEntry.crc != this.crc.getValue()) {
            throw new ZipException("CRC mismatch");
        }
        if (this.currentEntry.compressedSize != inB || this.currentEntry.size != out) {
            throw new ZipException("Size mismatch");
        }
    }

    public ZipEntry getNextEntry() throws Exception {
        closeEntry();
        if (this.entriesEnd) {
            return null;
        }
        Streams.readFully(this.in, this.hdrBuf, 0, 4);
        int hdr = Memory.peekInt(this.hdrBuf, 0, ByteOrder.LITTLE_ENDIAN);
        if (hdr == ZipConstants.CENSIG) {
            this.entriesEnd = true;
            return null;
        }
        if (hdr != ZipConstants.LOCSIG) {
            return null;
        }
        Streams.readFully(this.in, this.hdrBuf, 0, 26);
        int version = peekShort(0) & Opcodes.OP_CONST_CLASS_JUMBO;
        if (version > 20) {
            throw new ZipException("Cannot read local header version " + version);
        }
        int flags = peekShort(2);
        if ((flags & 1) != 0) {
            throw new ZipException("Invalid General Purpose Bit Flag: " + flags);
        }
        this.hasDD = (flags & 8) != 0;
        int ceLastModifiedTime = peekShort(6);
        int ceLastModifiedDate = peekShort(8);
        int ceCompressionMethod = peekShort(4);
        long ceCrc = 0;
        long ceCompressedSize = 0;
        long ceSize = -1;
        if (!this.hasDD) {
            ceCrc = ((long) Memory.peekInt(this.hdrBuf, 10, ByteOrder.LITTLE_ENDIAN)) & 4294967295L;
            ceCompressedSize = ((long) Memory.peekInt(this.hdrBuf, 14, ByteOrder.LITTLE_ENDIAN)) & 4294967295L;
            ceSize = ((long) Memory.peekInt(this.hdrBuf, 18, ByteOrder.LITTLE_ENDIAN)) & 4294967295L;
        }
        int nameLength = peekShort(22);
        if (nameLength == 0) {
            throw new ZipException("Entry is not named");
        }
        int extraLength = peekShort(24);
        String name = readString(nameLength);
        this.currentEntry = createZipEntry(name);
        this.currentEntry.time = ceLastModifiedTime;
        this.currentEntry.modDate = ceLastModifiedDate;
        this.currentEntry.setMethod(ceCompressionMethod);
        if (ceSize != -1) {
            this.currentEntry.setCrc(ceCrc);
            this.currentEntry.setSize(ceSize);
            this.currentEntry.setCompressedSize(ceCompressedSize);
        }
        if (extraLength > 0) {
            byte[] extraData = new byte[extraLength];
            Streams.readFully(this.in, extraData, 0, extraLength);
            this.currentEntry.setExtra(extraData);
        }
        return this.currentEntry;
    }

    private String readString(int byteLength) throws IOException {
        if (byteLength > this.stringBytesBuf.length) {
            this.stringBytesBuf = new byte[byteLength];
        }
        Streams.readFully(this.in, this.stringBytesBuf, 0, byteLength);
        if (byteLength > this.stringCharBuf.length) {
            this.stringCharBuf = new char[byteLength];
        }
        return ModifiedUtf8.decode(this.stringBytesBuf, this.stringCharBuf, 0, byteLength);
    }

    private int peekShort(int offset) {
        return Memory.peekShort(this.hdrBuf, offset, ByteOrder.LITTLE_ENDIAN) & 65535;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        checkClosed();
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        if (this.inf.finished() || this.currentEntry == null) {
            return -1;
        }
        if (this.currentEntry.compressionMethod == 0) {
            int csize = (int) this.currentEntry.size;
            if (this.inRead >= csize) {
                return -1;
            }
            if (this.lastRead >= this.len) {
                this.lastRead = 0;
                int i = this.in.read(this.buf);
                this.len = i;
                if (i == -1) {
                    this.eof = true;
                    return -1;
                }
                this.entryIn += this.len;
            }
            int toRead = byteCount > this.len - this.lastRead ? this.len - this.lastRead : byteCount;
            if (csize - this.inRead < toRead) {
                toRead = csize - this.inRead;
            }
            System.arraycopy(this.buf, this.lastRead, buffer, byteOffset, toRead);
            this.lastRead += toRead;
            this.inRead += toRead;
            this.crc.update(buffer, byteOffset, toRead);
            return toRead;
        }
        if (this.inf.needsInput()) {
            fill();
            if (this.len > 0) {
                this.entryIn += this.len;
            }
        }
        try {
            int read = this.inf.inflate(buffer, byteOffset, byteCount);
            if (read == 0 && this.inf.finished()) {
                return -1;
            }
            this.crc.update(buffer, byteOffset, read);
            return read;
        } catch (DataFormatException e) {
            throw new ZipException(e.getMessage());
        }
    }

    @Override
    public int available() throws IOException {
        checkClosed();
        return (this.currentEntry == null || ((long) this.inRead) < this.currentEntry.size) ? 1 : 0;
    }

    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }

    private void checkClosed() throws IOException {
        if (this.closed) {
            throw new IOException("Stream is closed");
        }
    }
}
