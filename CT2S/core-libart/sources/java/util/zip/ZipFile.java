package java.util.zip;

import dalvik.system.CloseGuard;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import libcore.io.BufferIterator;
import libcore.io.HeapBufferIterator;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class ZipFile implements Closeable, ZipConstants {
    static final int GPBF_DATA_DESCRIPTOR_FLAG = 8;
    static final int GPBF_ENCRYPTED_FLAG = 1;
    static final int GPBF_UNSUPPORTED_MASK = 1;
    static final int GPBF_UTF8_FLAG = 2048;
    public static final int OPEN_DELETE = 4;
    public static final int OPEN_READ = 1;
    private String comment;
    private final LinkedHashMap<String, ZipEntry> entries;
    private File fileToDeleteOnClose;
    private final String filename;
    private final CloseGuard guard;
    private RandomAccessFile raf;

    public ZipFile(File file) throws IOException {
        this(file, 1);
    }

    public ZipFile(String name) throws IOException {
        this(new File(name), 1);
    }

    public ZipFile(File file, int mode) throws IOException {
        this.entries = new LinkedHashMap<>();
        this.guard = CloseGuard.get();
        this.filename = file.getPath();
        if (mode != 1 && mode != 5) {
            throw new IllegalArgumentException("Bad mode: " + mode);
        }
        if ((mode & 4) != 0) {
            this.fileToDeleteOnClose = file;
            this.fileToDeleteOnClose.deleteOnExit();
        } else {
            this.fileToDeleteOnClose = null;
        }
        this.raf = new RandomAccessFile(this.filename, "r");
        boolean mustCloseFile = true;
        try {
            readCentralDir();
            mustCloseFile = false;
            this.guard.open("close");
        } finally {
            if (mustCloseFile) {
                IoUtils.closeQuietly(this.raf);
            }
        }
    }

    protected void finalize() throws IOException {
        AssertionError assertionError;
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            try {
                super.finalize();
            } finally {
            }
        } catch (Throwable th) {
            try {
                super.finalize();
                throw th;
            } finally {
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.guard.close();
        RandomAccessFile localRaf = this.raf;
        if (localRaf != null) {
            synchronized (localRaf) {
                this.raf = null;
                localRaf.close();
            }
            if (this.fileToDeleteOnClose != null) {
                this.fileToDeleteOnClose.delete();
                this.fileToDeleteOnClose = null;
            }
        }
    }

    private void checkNotClosed() {
        if (this.raf == null) {
            throw new IllegalStateException("Zip file closed");
        }
    }

    public Enumeration<? extends ZipEntry> entries() {
        checkNotClosed();
        final Iterator<ZipEntry> iterator = this.entries.values().iterator();
        return new Enumeration<ZipEntry>() {
            @Override
            public boolean hasMoreElements() {
                ZipFile.this.checkNotClosed();
                return iterator.hasNext();
            }

            @Override
            public ZipEntry nextElement() {
                ZipFile.this.checkNotClosed();
                return (ZipEntry) iterator.next();
            }
        };
    }

    public String getComment() {
        checkNotClosed();
        return this.comment;
    }

    public ZipEntry getEntry(String entryName) {
        checkNotClosed();
        if (entryName == null) {
            throw new NullPointerException("entryName == null");
        }
        ZipEntry ze = this.entries.get(entryName);
        if (ze == null) {
            return this.entries.get(entryName + "/");
        }
        return ze;
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        ZipEntry entry2 = getEntry(entry.getName());
        if (entry2 == null) {
            return null;
        }
        RandomAccessFile localRaf = this.raf;
        synchronized (localRaf) {
            RAFStream rafStream = new RAFStream(localRaf, entry2.localHeaderRelOffset);
            DataInputStream is = new DataInputStream(rafStream);
            int localMagic = Integer.reverseBytes(is.readInt());
            if (localMagic != ZipConstants.LOCSIG) {
                throwZipException("Local File Header", localMagic);
            }
            is.skipBytes(2);
            int gpbf = Short.reverseBytes(is.readShort()) & 65535;
            if ((gpbf & 1) != 0) {
                throw new ZipException("Invalid General Purpose Bit Flag: " + gpbf);
            }
            is.skipBytes(18);
            int fileNameLength = Short.reverseBytes(is.readShort()) & 65535;
            int extraFieldLength = Short.reverseBytes(is.readShort()) & 65535;
            is.close();
            rafStream.skip(fileNameLength + extraFieldLength);
            if (entry2.compressionMethod == 0) {
                rafStream.endOffset = rafStream.offset + entry2.size;
                return rafStream;
            }
            rafStream.endOffset = rafStream.offset + entry2.compressedSize;
            int bufSize = Math.max(1024, (int) Math.min(entry2.getSize(), 65535L));
            return new ZipInflaterInputStream(rafStream, new Inflater(true), bufSize, entry2);
        }
    }

    public String getName() {
        return this.filename;
    }

    public int size() {
        checkNotClosed();
        return this.entries.size();
    }

    private void readCentralDir() throws IOException {
        long scanOffset = this.raf.length() - 22;
        if (scanOffset < 0) {
            throw new ZipException("File too short to be a zip file: " + this.raf.length());
        }
        this.raf.seek(0L);
        int headerMagic = Integer.reverseBytes(this.raf.readInt());
        if (headerMagic == ZipConstants.ENDSIG) {
            throw new ZipException("Empty zip archive not supported");
        }
        if (headerMagic != ZipConstants.LOCSIG) {
            throw new ZipException("Not a zip archive");
        }
        long stopOffset = scanOffset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }
        do {
            this.raf.seek(scanOffset);
            if (Integer.reverseBytes(this.raf.readInt()) != ZipConstants.ENDSIG) {
                scanOffset--;
            } else {
                byte[] eocd = new byte[18];
                this.raf.readFully(eocd);
                BufferIterator it = HeapBufferIterator.iterator(eocd, 0, eocd.length, ByteOrder.LITTLE_ENDIAN);
                int diskNumber = it.readShort() & 65535;
                int diskWithCentralDir = it.readShort() & 65535;
                int numEntries = it.readShort() & 65535;
                int totalNumEntries = it.readShort() & 65535;
                it.skip(4);
                long centralDirOffset = ((long) it.readInt()) & 4294967295L;
                int commentLength = it.readShort() & 65535;
                if (numEntries != totalNumEntries || diskNumber != 0 || diskWithCentralDir != 0) {
                    throw new ZipException("Spanned archives not supported");
                }
                if (commentLength > 0) {
                    byte[] commentBytes = new byte[commentLength];
                    this.raf.readFully(commentBytes);
                    this.comment = new String(commentBytes, 0, commentBytes.length, StandardCharsets.UTF_8);
                }
                RAFStream rafStream = new RAFStream(this.raf, centralDirOffset);
                BufferedInputStream bufferedStream = new BufferedInputStream(rafStream, 4096);
                byte[] hdrBuf = new byte[46];
                for (int i = 0; i < numEntries; i++) {
                    ZipEntry newEntry = new ZipEntry(hdrBuf, bufferedStream, StandardCharsets.UTF_8);
                    if (newEntry.localHeaderRelOffset >= centralDirOffset) {
                        throw new ZipException("Local file header offset is after central directory");
                    }
                    String entryName = newEntry.getName();
                    if (this.entries.put(entryName, newEntry) != null) {
                        throw new ZipException("Duplicate entry name: " + entryName);
                    }
                }
                return;
            }
        } while (scanOffset >= stopOffset);
        throw new ZipException("End Of Central Directory signature not found");
    }

    static void throwZipException(String msg, int magic) throws ZipException {
        String hexString = IntegralToString.intToHexString(magic, true, 8);
        throw new ZipException(msg + " signature not found; was " + hexString);
    }

    public static class RAFStream extends InputStream {
        private long endOffset;
        private long offset;
        private final RandomAccessFile sharedRaf;

        public RAFStream(RandomAccessFile raf, long initialOffset, long endOffset) {
            this.sharedRaf = raf;
            this.offset = initialOffset;
            this.endOffset = endOffset;
        }

        public RAFStream(RandomAccessFile raf, long initialOffset) throws IOException {
            this(raf, initialOffset, raf.length());
        }

        @Override
        public int available() throws IOException {
            return this.offset < this.endOffset ? 1 : 0;
        }

        @Override
        public int read() throws IOException {
            return Streams.readSingleByte(this);
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int count;
            synchronized (this.sharedRaf) {
                long length = this.endOffset - this.offset;
                if (byteCount > length) {
                    byteCount = (int) length;
                }
                this.sharedRaf.seek(this.offset);
                count = this.sharedRaf.read(buffer, byteOffset, byteCount);
                if (count > 0) {
                    this.offset += (long) count;
                } else {
                    count = -1;
                }
            }
            return count;
        }

        @Override
        public long skip(long byteCount) throws IOException {
            if (byteCount > this.endOffset - this.offset) {
                byteCount = this.endOffset - this.offset;
            }
            this.offset += byteCount;
            return byteCount;
        }

        public int fill(Inflater inflater, int nativeEndBufSize) throws IOException {
            int len;
            synchronized (this.sharedRaf) {
                len = Math.min((int) (this.endOffset - this.offset), nativeEndBufSize);
                int cnt = inflater.setFileInput(this.sharedRaf.getFD(), this.offset, nativeEndBufSize);
                skip(cnt);
            }
            return len;
        }
    }

    public static class ZipInflaterInputStream extends InflaterInputStream {
        private long bytesRead;
        private final ZipEntry entry;

        public ZipInflaterInputStream(InputStream is, Inflater inf, int bsize, ZipEntry entry) {
            super(is, inf, bsize);
            this.bytesRead = 0L;
            this.entry = entry;
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            try {
                int i = super.read(buffer, byteOffset, byteCount);
                if (i != -1) {
                    this.bytesRead += (long) i;
                } else if (this.entry.size != this.bytesRead) {
                    throw new IOException("Size mismatch on inflated file: " + this.bytesRead + " vs " + this.entry.size);
                }
                return i;
            } catch (IOException e) {
                throw new IOException("Error reading data for " + this.entry.getName() + " near offset " + this.bytesRead, e);
            }
        }

        @Override
        public int available() throws IOException {
            if (this.closed || super.available() == 0) {
                return 0;
            }
            return (int) (this.entry.getSize() - this.bytesRead);
        }
    }
}
