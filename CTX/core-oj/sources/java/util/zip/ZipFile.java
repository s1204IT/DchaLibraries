package java.util.zip;

import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ZipFile implements ZipConstants, Closeable {
    private static final int DEFLATED = 8;
    private static final int JZENTRY_COMMENT = 2;
    private static final int JZENTRY_EXTRA = 1;
    private static final int JZENTRY_NAME = 0;
    public static final int OPEN_DELETE = 4;
    public static final int OPEN_READ = 1;
    private static final int STORED = 0;
    private static final boolean usemmap = true;
    private volatile boolean closeRequested;
    private final File fileToRemoveOnClose;
    private final CloseGuard guard;
    private Deque<Inflater> inflaterCache;
    private long jzfile;
    private final boolean locsig;
    private final String name;
    private final Map<InputStream, Inflater> streams;
    private final int total;
    private ZipCoder zc;

    private static native void close(long j);

    private static native void freeEntry(long j, long j2);

    private static native byte[] getCommentBytes(long j);

    private static native long getEntry(long j, byte[] bArr, boolean z);

    private static native byte[] getEntryBytes(long j, int i);

    private static native long getEntryCSize(long j);

    private static native long getEntryCrc(long j);

    private static native int getEntryFlag(long j);

    private static native int getEntryMethod(long j);

    private static native long getEntrySize(long j);

    private static native long getEntryTime(long j);

    private static native int getFileDescriptor(long j);

    private static native long getNextEntry(long j, int i);

    private static native int getTotal(long j);

    private static native String getZipMessage(long j);

    private static native long open(String str, int i, long j, boolean z) throws IOException;

    private static native int read(long j, long j2, long j3, byte[] bArr, int i, int i2);

    private static native boolean startsWithLOC(long j);

    public ZipFile(String str) throws IOException {
        this(new File(str), 1);
    }

    public ZipFile(File file, int i) throws IOException {
        this(file, i, StandardCharsets.UTF_8);
    }

    public ZipFile(File file) throws IOException {
        this(file, 1);
    }

    public ZipFile(File file, int i, Charset charset) throws IOException {
        this.closeRequested = false;
        this.guard = CloseGuard.get();
        this.streams = new WeakHashMap();
        this.inflaterCache = new ArrayDeque();
        if ((i & 1) == 0 || (i & (-6)) != 0) {
            throw new IllegalArgumentException("Illegal mode: 0x" + Integer.toHexString(i));
        }
        long length = file.length();
        if (length < 22) {
            if (length == 0 && !file.exists()) {
                throw new FileNotFoundException("File doesn't exist: " + ((Object) file));
            }
            throw new ZipException("File too short to be a zip file: " + file.length());
        }
        this.fileToRemoveOnClose = (i & 4) != 0 ? file : null;
        String path = file.getPath();
        if (charset == null) {
            throw new NullPointerException("charset is null");
        }
        this.zc = ZipCoder.get(charset);
        this.jzfile = open(path, i, file.lastModified(), usemmap);
        this.name = path;
        this.total = getTotal(this.jzfile);
        this.locsig = startsWithLOC(this.jzfile);
        Enumeration<? extends ZipEntry> enumerationEntries = entries();
        this.guard.open("close");
        if (size() == 0 || !enumerationEntries.hasMoreElements()) {
            close();
            throw new ZipException("No entries");
        }
    }

    public ZipFile(String str, Charset charset) throws IOException {
        this(new File(str), 1, charset);
    }

    public ZipFile(File file, Charset charset) throws IOException {
        this(file, 1, charset);
    }

    public String getComment() {
        synchronized (this) {
            ensureOpen();
            byte[] commentBytes = getCommentBytes(this.jzfile);
            if (commentBytes == null) {
                return null;
            }
            return this.zc.toString(commentBytes, commentBytes.length);
        }
    }

    public ZipEntry getEntry(String str) {
        if (str == null) {
            throw new NullPointerException("name");
        }
        synchronized (this) {
            ensureOpen();
            long entry = getEntry(this.jzfile, this.zc.getBytes(str), true);
            if (entry != 0) {
                ZipEntry zipEntry = getZipEntry(str, entry);
                freeEntry(this.jzfile, entry);
                return zipEntry;
            }
            return null;
        }
    }

    public InputStream getInputStream(ZipEntry zipEntry) throws IOException {
        long entry;
        if (zipEntry == null) {
            throw new NullPointerException("entry");
        }
        synchronized (this) {
            ensureOpen();
            if (!this.zc.isUTF8() && (zipEntry.flag & 2048) != 0) {
                entry = getEntry(this.jzfile, this.zc.getBytesUTF8(zipEntry.name), true);
            } else {
                entry = getEntry(this.jzfile, this.zc.getBytes(zipEntry.name), true);
            }
            if (entry == 0) {
                return null;
            }
            ZipFileInputStream zipFileInputStream = new ZipFileInputStream(entry);
            int entryMethod = getEntryMethod(entry);
            if (entryMethod == 0) {
                synchronized (this.streams) {
                    this.streams.put(zipFileInputStream, null);
                }
                return zipFileInputStream;
            }
            if (entryMethod == 8) {
                long entrySize = getEntrySize(entry) + 2;
                if (entrySize > 65536) {
                    entrySize = 65536;
                }
                if (entrySize <= 0) {
                    entrySize = 4096;
                }
                Inflater inflater = getInflater();
                ZipFileInflaterInputStream zipFileInflaterInputStream = new ZipFileInflaterInputStream(zipFileInputStream, inflater, (int) entrySize);
                synchronized (this.streams) {
                    this.streams.put(zipFileInflaterInputStream, inflater);
                }
                return zipFileInflaterInputStream;
            }
            throw new ZipException("invalid compression method");
        }
    }

    private class ZipFileInflaterInputStream extends InflaterInputStream {
        private volatile boolean closeRequested;
        private boolean eof;
        private final ZipFileInputStream zfin;

        ZipFileInflaterInputStream(ZipFileInputStream zipFileInputStream, Inflater inflater, int i) {
            super(zipFileInputStream, inflater, i);
            this.closeRequested = false;
            this.eof = false;
            this.zfin = zipFileInputStream;
        }

        @Override
        public void close() throws IOException {
            Inflater inflater;
            if (this.closeRequested) {
                return;
            }
            this.closeRequested = true;
            super.close();
            synchronized (ZipFile.this.streams) {
                inflater = (Inflater) ZipFile.this.streams.remove(this);
            }
            if (inflater != null) {
                ZipFile.this.releaseInflater(inflater);
            }
        }

        @Override
        protected void fill() throws IOException {
            if (this.eof) {
                throw new EOFException("Unexpected end of ZLIB input stream");
            }
            this.len = this.in.read(this.buf, 0, this.buf.length);
            if (this.len == -1) {
                this.buf[0] = 0;
                this.len = 1;
                this.eof = true;
            }
            this.inf.setInput(this.buf, 0, this.len);
        }

        @Override
        public int available() throws IOException {
            if (this.closeRequested) {
                return 0;
            }
            long size = this.zfin.size() - this.inf.getBytesWritten();
            return size > 2147483647L ? Integer.MAX_VALUE : (int) size;
        }

        protected void finalize() throws Throwable {
            close();
        }
    }

    private Inflater getInflater() {
        Inflater inflaterPoll;
        synchronized (this.inflaterCache) {
            do {
                inflaterPoll = this.inflaterCache.poll();
                if (inflaterPoll == null) {
                    return new Inflater(true);
                }
            } while (inflaterPoll.ended());
            return inflaterPoll;
        }
    }

    private void releaseInflater(Inflater inflater) {
        if (!inflater.ended()) {
            inflater.reset();
            synchronized (this.inflaterCache) {
                this.inflaterCache.add(inflater);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    private class ZipEntryIterator implements Enumeration<ZipEntry>, Iterator<ZipEntry> {
        private int i = 0;

        public ZipEntryIterator() {
            ZipFile.this.ensureOpen();
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public boolean hasNext() {
            boolean z;
            synchronized (ZipFile.this) {
                ZipFile.this.ensureOpen();
                z = this.i < ZipFile.this.total;
            }
            return z;
        }

        @Override
        public ZipEntry nextElement() {
            return next();
        }

        @Override
        public ZipEntry next() {
            ZipEntry zipEntry;
            String zipMessage;
            synchronized (ZipFile.this) {
                ZipFile.this.ensureOpen();
                if (this.i < ZipFile.this.total) {
                    long j = ZipFile.this.jzfile;
                    int i = this.i;
                    this.i = i + 1;
                    long nextEntry = ZipFile.getNextEntry(j, i);
                    if (nextEntry == 0) {
                        if (!ZipFile.this.closeRequested) {
                            zipMessage = ZipFile.getZipMessage(ZipFile.this.jzfile);
                        } else {
                            zipMessage = "ZipFile concurrently closed";
                        }
                        throw new ZipError("jzentry == 0,\n jzfile = " + ZipFile.this.jzfile + ",\n total = " + ZipFile.this.total + ",\n name = " + ZipFile.this.name + ",\n i = " + this.i + ",\n message = " + zipMessage);
                    }
                    zipEntry = ZipFile.this.getZipEntry(null, nextEntry);
                    ZipFile.freeEntry(ZipFile.this.jzfile, nextEntry);
                } else {
                    throw new NoSuchElementException();
                }
            }
            return zipEntry;
        }
    }

    public Enumeration<? extends ZipEntry> entries() {
        return new ZipEntryIterator();
    }

    public Stream<? extends ZipEntry> stream() {
        return StreamSupport.stream(Spliterators.spliterator(new ZipEntryIterator(), size(), 1297), false);
    }

    private ZipEntry getZipEntry(String str, long j) {
        ZipEntry zipEntry = new ZipEntry();
        zipEntry.flag = getEntryFlag(j);
        if (str != null) {
            zipEntry.name = str;
        } else {
            byte[] entryBytes = getEntryBytes(j, 0);
            if (!this.zc.isUTF8() && (zipEntry.flag & 2048) != 0) {
                zipEntry.name = this.zc.toStringUTF8(entryBytes, entryBytes.length);
            } else {
                zipEntry.name = this.zc.toString(entryBytes, entryBytes.length);
            }
        }
        zipEntry.xdostime = getEntryTime(j);
        zipEntry.crc = getEntryCrc(j);
        zipEntry.size = getEntrySize(j);
        zipEntry.csize = getEntryCSize(j);
        zipEntry.method = getEntryMethod(j);
        zipEntry.setExtra0(getEntryBytes(j, 1), false);
        byte[] entryBytes2 = getEntryBytes(j, 2);
        if (entryBytes2 == null) {
            zipEntry.comment = null;
        } else if (!this.zc.isUTF8() && (zipEntry.flag & 2048) != 0) {
            zipEntry.comment = this.zc.toStringUTF8(entryBytes2, entryBytes2.length);
        } else {
            zipEntry.comment = this.zc.toString(entryBytes2, entryBytes2.length);
        }
        return zipEntry;
    }

    public int size() {
        ensureOpen();
        return this.total;
    }

    @Override
    public void close() throws IOException {
        if (this.closeRequested) {
            return;
        }
        this.guard.close();
        this.closeRequested = true;
        synchronized (this) {
            synchronized (this.streams) {
                if (!this.streams.isEmpty()) {
                    HashMap map = new HashMap(this.streams);
                    this.streams.clear();
                    Iterator it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        ((InputStream) entry.getKey()).close();
                        Inflater inflater = (Inflater) entry.getValue();
                        if (inflater != null) {
                            inflater.end();
                        }
                    }
                }
            }
            synchronized (this.inflaterCache) {
                while (true) {
                    Inflater inflaterPoll = this.inflaterCache.poll();
                    if (inflaterPoll == null) {
                        break;
                    } else {
                        inflaterPoll.end();
                    }
                }
            }
            if (this.jzfile != 0) {
                long j = this.jzfile;
                this.jzfile = 0L;
                close(j);
            }
            if (this.fileToRemoveOnClose != null) {
                this.fileToRemoveOnClose.delete();
            }
        }
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
    }

    private void ensureOpen() {
        if (this.closeRequested) {
            throw new IllegalStateException("zip file closed");
        }
        if (this.jzfile == 0) {
            throw new IllegalStateException("The object is not initialized.");
        }
    }

    private void ensureOpenOrZipException() throws IOException {
        if (this.closeRequested) {
            throw new ZipException("ZipFile closed");
        }
    }

    private class ZipFileInputStream extends InputStream {
        protected long jzentry;
        protected long rem;
        protected long size;
        private volatile boolean zfisCloseRequested = false;
        private long pos = 0;

        ZipFileInputStream(long j) {
            this.rem = ZipFile.getEntryCSize(j);
            this.size = ZipFile.getEntrySize(j);
            this.jzentry = j;
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            int i3 = i2;
            ZipFile.this.ensureOpenOrZipException();
            synchronized (ZipFile.this) {
                long j = this.rem;
                long j2 = this.pos;
                if (j == 0) {
                    return -1;
                }
                if (i3 <= 0) {
                    return 0;
                }
                if (i3 > j) {
                    i3 = (int) j;
                }
                int i4 = ZipFile.read(ZipFile.this.jzfile, this.jzentry, j2, bArr, i, i3);
                if (i4 > 0) {
                    long j3 = i4;
                    this.pos = j2 + j3;
                    this.rem = j - j3;
                }
                if (this.rem == 0) {
                    close();
                }
                return i4;
            }
        }

        @Override
        public int read() throws IOException {
            byte[] bArr = new byte[1];
            if (read(bArr, 0, 1) == 1) {
                return bArr[0] & Character.DIRECTIONALITY_UNDEFINED;
            }
            return -1;
        }

        @Override
        public long skip(long j) {
            if (j > this.rem) {
                j = this.rem;
            }
            this.pos += j;
            this.rem -= j;
            if (this.rem == 0) {
                close();
            }
            return j;
        }

        @Override
        public int available() {
            return this.rem > 2147483647L ? Integer.MAX_VALUE : (int) this.rem;
        }

        public long size() {
            return this.size;
        }

        @Override
        public void close() {
            if (this.zfisCloseRequested) {
                return;
            }
            this.zfisCloseRequested = true;
            this.rem = 0L;
            synchronized (ZipFile.this) {
                if (this.jzentry != 0 && ZipFile.this.jzfile != 0) {
                    ZipFile.freeEntry(ZipFile.this.jzfile, this.jzentry);
                    this.jzentry = 0L;
                }
            }
            synchronized (ZipFile.this.streams) {
                ZipFile.this.streams.remove(this);
            }
        }

        protected void finalize() {
            close();
        }
    }

    public boolean startsWithLocHeader() {
        return this.locsig;
    }

    public int getFileDescriptor() {
        return getFileDescriptor(this.jzfile);
    }
}
