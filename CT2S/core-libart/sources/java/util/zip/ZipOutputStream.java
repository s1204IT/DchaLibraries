package java.util.zip;

import dalvik.bytecode.Opcodes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import libcore.util.EmptyArray;

public class ZipOutputStream extends DeflaterOutputStream implements ZipConstants {
    public static final int DEFLATED = 8;
    public static final int STORED = 0;
    private static final int ZIP_VERSION_2_0 = 20;
    private ByteArrayOutputStream cDir;
    private byte[] commentBytes;
    private int compressionLevel;
    private final CRC32 crc;
    private int curOffset;
    private ZipEntry currentEntry;
    private int defaultCompressionMethod;
    private final HashSet<String> entries;
    private byte[] entryCommentBytes;
    private byte[] nameBytes;
    private int offset;

    public ZipOutputStream(OutputStream os) {
        super(os, new Deflater(-1, true));
        this.commentBytes = EmptyArray.BYTE;
        this.entries = new HashSet<>();
        this.defaultCompressionMethod = 8;
        this.compressionLevel = -1;
        this.cDir = new ByteArrayOutputStream();
        this.crc = new CRC32();
        this.offset = 0;
        this.curOffset = 0;
    }

    @Override
    public void close() throws IOException {
        if (this.out != null) {
            finish();
            this.def.end();
            this.out.close();
            this.out = null;
        }
    }

    public void closeEntry() throws IOException {
        checkOpen();
        if (this.currentEntry != null) {
            if (this.currentEntry.getMethod() == 8) {
                super.finish();
            }
            if (this.currentEntry.getMethod() == 0) {
                if (this.crc.getValue() != this.currentEntry.crc) {
                    throw new ZipException("CRC mismatch");
                }
                if (this.currentEntry.size != this.crc.tbytes) {
                    throw new ZipException("Size mismatch");
                }
            }
            this.curOffset = 30;
            if (this.currentEntry.getMethod() != 0) {
                this.curOffset += 16;
                writeLong(this.out, ZipConstants.EXTSIG);
                OutputStream outputStream = this.out;
                ZipEntry zipEntry = this.currentEntry;
                long value = this.crc.getValue();
                zipEntry.crc = value;
                writeLong(outputStream, value);
                OutputStream outputStream2 = this.out;
                ZipEntry zipEntry2 = this.currentEntry;
                long totalOut = this.def.getTotalOut();
                zipEntry2.compressedSize = totalOut;
                writeLong(outputStream2, totalOut);
                OutputStream outputStream3 = this.out;
                ZipEntry zipEntry3 = this.currentEntry;
                long totalIn = this.def.getTotalIn();
                zipEntry3.size = totalIn;
                writeLong(outputStream3, totalIn);
            }
            int flags = this.currentEntry.getMethod() == 0 ? 0 : 8;
            writeLong(this.cDir, ZipConstants.CENSIG);
            writeShort(this.cDir, 20);
            writeShort(this.cDir, 20);
            writeShort(this.cDir, flags | 2048);
            writeShort(this.cDir, this.currentEntry.getMethod());
            writeShort(this.cDir, this.currentEntry.time);
            writeShort(this.cDir, this.currentEntry.modDate);
            writeLong(this.cDir, this.crc.getValue());
            if (this.currentEntry.getMethod() == 8) {
                this.curOffset = (int) (((long) this.curOffset) + writeLong(this.cDir, this.def.getTotalOut()));
                writeLong(this.cDir, this.def.getTotalIn());
            } else {
                this.curOffset = (int) (((long) this.curOffset) + writeLong(this.cDir, this.crc.tbytes));
                writeLong(this.cDir, this.crc.tbytes);
            }
            this.curOffset += writeShort(this.cDir, this.nameBytes.length);
            if (this.currentEntry.extra != null) {
                this.curOffset += writeShort(this.cDir, this.currentEntry.extra.length);
            } else {
                writeShort(this.cDir, 0);
            }
            writeShort(this.cDir, this.entryCommentBytes.length);
            writeShort(this.cDir, 0);
            writeShort(this.cDir, 0);
            writeLong(this.cDir, 0L);
            writeLong(this.cDir, this.offset);
            this.cDir.write(this.nameBytes);
            this.nameBytes = null;
            if (this.currentEntry.extra != null) {
                this.cDir.write(this.currentEntry.extra);
            }
            this.offset += this.curOffset;
            if (this.entryCommentBytes.length > 0) {
                this.cDir.write(this.entryCommentBytes);
                this.entryCommentBytes = EmptyArray.BYTE;
            }
            this.currentEntry = null;
            this.crc.reset();
            this.def.reset();
            this.done = false;
        }
    }

    @Override
    public void finish() throws IOException {
        if (this.out == null) {
            throw new IOException("Stream is closed");
        }
        if (this.cDir != null) {
            if (this.entries.isEmpty()) {
                throw new ZipException("No entries");
            }
            if (this.currentEntry != null) {
                closeEntry();
            }
            int cdirSize = this.cDir.size();
            writeLong(this.cDir, ZipConstants.ENDSIG);
            writeShort(this.cDir, 0);
            writeShort(this.cDir, 0);
            writeShort(this.cDir, this.entries.size());
            writeShort(this.cDir, this.entries.size());
            writeLong(this.cDir, cdirSize);
            writeLong(this.cDir, this.offset);
            writeShort(this.cDir, this.commentBytes.length);
            if (this.commentBytes.length > 0) {
                this.cDir.write(this.commentBytes);
            }
            this.cDir.writeTo(this.out);
            this.cDir = null;
        }
    }

    public void putNextEntry(ZipEntry ze) throws IOException {
        if (this.currentEntry != null) {
            closeEntry();
        }
        int method = ze.getMethod();
        if (method == -1) {
            method = this.defaultCompressionMethod;
        }
        if (method == 0) {
            if (ze.getCompressedSize() == -1) {
                ze.setCompressedSize(ze.getSize());
            } else if (ze.getSize() == -1) {
                ze.setSize(ze.getCompressedSize());
            }
            if (ze.getCrc() == -1) {
                throw new ZipException("STORED entry missing CRC");
            }
            if (ze.getSize() == -1) {
                throw new ZipException("STORED entry missing size");
            }
            if (ze.size != ze.compressedSize) {
                throw new ZipException("STORED entry size/compressed size mismatch");
            }
        }
        checkOpen();
        if (this.entries.contains(ze.name)) {
            throw new ZipException("Entry already exists: " + ze.name);
        }
        if (this.entries.size() == 65535) {
            throw new ZipException("Too many entries for the zip file format's 16-bit entry count");
        }
        this.nameBytes = ze.name.getBytes(StandardCharsets.UTF_8);
        checkSizeIsWithinShort("Name", this.nameBytes);
        this.entryCommentBytes = EmptyArray.BYTE;
        if (ze.comment != null) {
            this.entryCommentBytes = ze.comment.getBytes(StandardCharsets.UTF_8);
            checkSizeIsWithinShort("Comment", this.entryCommentBytes);
        }
        this.def.setLevel(this.compressionLevel);
        ze.setMethod(method);
        this.currentEntry = ze;
        this.entries.add(this.currentEntry.name);
        int flags = method == 0 ? 0 : 8;
        writeLong(this.out, ZipConstants.LOCSIG);
        writeShort(this.out, 20);
        writeShort(this.out, flags | 2048);
        writeShort(this.out, method);
        if (this.currentEntry.getTime() == -1) {
            this.currentEntry.setTime(System.currentTimeMillis());
        }
        writeShort(this.out, this.currentEntry.time);
        writeShort(this.out, this.currentEntry.modDate);
        if (method == 0) {
            writeLong(this.out, this.currentEntry.crc);
            writeLong(this.out, this.currentEntry.size);
            writeLong(this.out, this.currentEntry.size);
        } else {
            writeLong(this.out, 0L);
            writeLong(this.out, 0L);
            writeLong(this.out, 0L);
        }
        writeShort(this.out, this.nameBytes.length);
        if (this.currentEntry.extra != null) {
            writeShort(this.out, this.currentEntry.extra.length);
        } else {
            writeShort(this.out, 0);
        }
        this.out.write(this.nameBytes);
        if (this.currentEntry.extra != null) {
            this.out.write(this.currentEntry.extra);
        }
    }

    public void setComment(String comment) {
        if (comment == null) {
            this.commentBytes = EmptyArray.BYTE;
            return;
        }
        byte[] newCommentBytes = comment.getBytes(StandardCharsets.UTF_8);
        checkSizeIsWithinShort("Comment", newCommentBytes);
        this.commentBytes = newCommentBytes;
    }

    public void setLevel(int level) {
        if (level < -1 || level > 9) {
            throw new IllegalArgumentException("Bad level: " + level);
        }
        this.compressionLevel = level;
    }

    public void setMethod(int method) {
        if (method != 0 && method != 8) {
            throw new IllegalArgumentException("Bad method: " + method);
        }
        this.defaultCompressionMethod = method;
    }

    private long writeLong(OutputStream os, long i) throws IOException {
        os.write((int) (255 & i));
        os.write(((int) (i >> 8)) & Opcodes.OP_CONST_CLASS_JUMBO);
        os.write(((int) (i >> 16)) & Opcodes.OP_CONST_CLASS_JUMBO);
        os.write(((int) (i >> 24)) & Opcodes.OP_CONST_CLASS_JUMBO);
        return i;
    }

    private int writeShort(OutputStream os, int i) throws IOException {
        os.write(i & Opcodes.OP_CONST_CLASS_JUMBO);
        os.write((i >> 8) & Opcodes.OP_CONST_CLASS_JUMBO);
        return i;
    }

    @Override
    public void write(byte[] buffer, int offset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(buffer.length, offset, byteCount);
        if (this.currentEntry == null) {
            throw new ZipException("No active entry");
        }
        if (this.currentEntry.getMethod() == 0) {
            this.out.write(buffer, offset, byteCount);
        } else {
            super.write(buffer, offset, byteCount);
        }
        this.crc.update(buffer, offset, byteCount);
    }

    private void checkOpen() throws IOException {
        if (this.cDir == null) {
            throw new IOException("Stream is closed");
        }
    }

    private void checkSizeIsWithinShort(String property, byte[] bytes) {
        if (bytes.length > 65535) {
            throw new IllegalArgumentException(property + " too long in UTF-8:" + bytes.length + " bytes");
        }
    }
}
