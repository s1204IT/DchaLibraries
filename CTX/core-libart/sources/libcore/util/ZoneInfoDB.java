package libcore.util;

import android.system.ErrnoException;
import dalvik.annotation.optimization.ReachabilitySensitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.io.BufferIterator;
import libcore.io.MemoryMappedFile;

public final class ZoneInfoDB {
    public static final String TZDATA_FILE = "tzdata";
    private static final TzData DATA = TzData.loadTzDataWithFallback(TimeZoneDataFiles.getTimeZoneFilePaths(TZDATA_FILE));

    public static class TzData implements AutoCloseable {
        private static final int CACHE_SIZE = 1;
        public static final int SIZEOF_INDEX_ENTRY = 52;
        private static final int SIZEOF_TZINT = 4;
        private static final int SIZEOF_TZNAME = 40;
        private int[] byteOffsets;
        private final BasicLruCache<String, ZoneInfo> cache = new BasicLruCache<String, ZoneInfo>(1) {
            @Override
            protected ZoneInfo create(String str) {
                try {
                    return TzData.this.makeTimeZoneUncached(str);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to load timezone for ID=" + str, e);
                }
            }
        };
        private boolean closed;
        private String[] ids;

        @ReachabilitySensitive
        private MemoryMappedFile mappedFile;
        private int[] rawUtcOffsetsCache;
        private String version;
        private String zoneTab;

        public static TzData loadTzDataWithFallback(String... strArr) {
            for (String str : strArr) {
                TzData tzData = new TzData();
                if (tzData.loadData(str)) {
                    return tzData;
                }
            }
            System.logE("Couldn't find any tzdata file!");
            return createFallback();
        }

        public static TzData loadTzData(String str) {
            TzData tzData = new TzData();
            if (tzData.loadData(str)) {
                return tzData;
            }
            return null;
        }

        private static TzData createFallback() {
            TzData tzData = new TzData();
            tzData.populateFallback();
            return tzData;
        }

        private TzData() {
        }

        public BufferIterator getBufferIterator(String str) {
            checkNotClosed();
            int iBinarySearch = Arrays.binarySearch(this.ids, str);
            if (iBinarySearch < 0) {
                return null;
            }
            int i = this.byteOffsets[iBinarySearch];
            BufferIterator bufferIteratorBigEndianIterator = this.mappedFile.bigEndianIterator();
            bufferIteratorBigEndianIterator.skip(i);
            return bufferIteratorBigEndianIterator;
        }

        private void populateFallback() {
            this.version = "missing";
            this.zoneTab = "# Emergency fallback data.\n";
            this.ids = new String[]{"GMT"};
            int[] iArr = new int[1];
            this.rawUtcOffsetsCache = iArr;
            this.byteOffsets = iArr;
        }

        private boolean loadData(String str) {
            try {
                this.mappedFile = MemoryMappedFile.mmapRO(str);
                try {
                    readHeader();
                    return true;
                } catch (Exception e) {
                    close();
                    System.logE("tzdata file \"" + str + "\" was present but invalid!", e);
                    return false;
                }
            } catch (ErrnoException e2) {
                return false;
            }
        }

        private void readHeader() throws IOException {
            BufferIterator bufferIteratorBigEndianIterator = this.mappedFile.bigEndianIterator();
            try {
                byte[] bArr = new byte[12];
                bufferIteratorBigEndianIterator.readByteArray(bArr, 0, bArr.length);
                if (!new String(bArr, 0, 6, StandardCharsets.US_ASCII).equals(ZoneInfoDB.TZDATA_FILE) || bArr[11] != 0) {
                    throw new IOException("bad tzdata magic: " + Arrays.toString(bArr));
                }
                this.version = new String(bArr, 6, 5, StandardCharsets.US_ASCII);
                int size = this.mappedFile.size();
                int i = bufferIteratorBigEndianIterator.readInt();
                validateOffset(i, size);
                int i2 = bufferIteratorBigEndianIterator.readInt();
                validateOffset(i2, size);
                int i3 = bufferIteratorBigEndianIterator.readInt();
                validateOffset(i3, size);
                if (i >= i2 || i2 >= i3) {
                    throw new IOException("Invalid offset: index_offset=" + i + ", data_offset=" + i2 + ", zonetab_offset=" + i3 + ", fileSize=" + size);
                }
                readIndex(bufferIteratorBigEndianIterator, i, i2);
                readZoneTab(bufferIteratorBigEndianIterator, i3, size - i3);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("Invalid read from data file", e);
            }
        }

        private static void validateOffset(int i, int i2) throws IOException {
            if (i < 0 || i >= i2) {
                throw new IOException("Invalid offset=" + i + ", size=" + i2);
            }
        }

        private void readZoneTab(BufferIterator bufferIterator, int i, int i2) {
            byte[] bArr = new byte[i2];
            bufferIterator.seek(i);
            bufferIterator.readByteArray(bArr, 0, bArr.length);
            this.zoneTab = new String(bArr, 0, bArr.length, StandardCharsets.US_ASCII);
        }

        private void readIndex(BufferIterator bufferIterator, int i, int i2) throws IOException {
            bufferIterator.seek(i);
            byte[] bArr = new byte[40];
            int i3 = i2 - i;
            if (i3 % 52 != 0) {
                throw new IOException("Index size is not divisible by 52, indexSize=" + i3);
            }
            int i4 = i3 / 52;
            this.byteOffsets = new int[i4];
            this.ids = new String[i4];
            for (int i5 = 0; i5 < i4; i5++) {
                bufferIterator.readByteArray(bArr, 0, bArr.length);
                this.byteOffsets[i5] = bufferIterator.readInt();
                int[] iArr = this.byteOffsets;
                iArr[i5] = iArr[i5] + i2;
                if (bufferIterator.readInt() < 44) {
                    throw new IOException("length in index file < sizeof(tzhead)");
                }
                bufferIterator.skip(4);
                int i6 = 0;
                while (bArr[i6] != 0 && i6 < bArr.length) {
                    i6++;
                }
                if (i6 != 0) {
                    this.ids[i5] = new String(bArr, 0, i6, StandardCharsets.US_ASCII);
                    if (i5 > 0) {
                        int i7 = i5 - 1;
                        if (this.ids[i5].compareTo(this.ids[i7]) <= 0) {
                            throw new IOException("Index not sorted or contains multiple entries with the same ID, index=" + i5 + ", ids[i]=" + this.ids[i5] + ", ids[i - 1]=" + this.ids[i7]);
                        }
                    }
                } else {
                    throw new IOException("Invalid ID at index=" + i5);
                }
            }
        }

        public void validate() throws IOException {
            checkNotClosed();
            for (String str : getAvailableIDs()) {
                if (makeTimeZoneUncached(str) == null) {
                    throw new IOException("Unable to find data for ID=" + str);
                }
            }
        }

        ZoneInfo makeTimeZoneUncached(String str) throws IOException {
            BufferIterator bufferIterator = getBufferIterator(str);
            if (bufferIterator == null) {
                return null;
            }
            return ZoneInfo.readTimeZone(str, bufferIterator, System.currentTimeMillis());
        }

        public String[] getAvailableIDs() {
            checkNotClosed();
            return (String[]) this.ids.clone();
        }

        public String[] getAvailableIDs(int i) {
            checkNotClosed();
            ArrayList arrayList = new ArrayList();
            int[] rawUtcOffsets = getRawUtcOffsets();
            for (int i2 = 0; i2 < rawUtcOffsets.length; i2++) {
                if (rawUtcOffsets[i2] == i) {
                    arrayList.add(this.ids[i2]);
                }
            }
            return (String[]) arrayList.toArray(new String[arrayList.size()]);
        }

        private synchronized int[] getRawUtcOffsets() {
            if (this.rawUtcOffsetsCache != null) {
                return this.rawUtcOffsetsCache;
            }
            this.rawUtcOffsetsCache = new int[this.ids.length];
            for (int i = 0; i < this.ids.length; i++) {
                this.rawUtcOffsetsCache[i] = this.cache.get(this.ids[i]).getRawOffset();
            }
            return this.rawUtcOffsetsCache;
        }

        public String getVersion() {
            checkNotClosed();
            return this.version;
        }

        public String getZoneTab() {
            checkNotClosed();
            return this.zoneTab;
        }

        public ZoneInfo makeTimeZone(String str) throws IOException {
            checkNotClosed();
            ZoneInfo zoneInfo = this.cache.get(str);
            if (zoneInfo == null) {
                return null;
            }
            return (ZoneInfo) zoneInfo.clone();
        }

        public boolean hasTimeZone(String str) throws IOException {
            checkNotClosed();
            return this.cache.get(str) != null;
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                this.ids = null;
                this.byteOffsets = null;
                this.rawUtcOffsetsCache = null;
                this.cache.evictAll();
                if (this.mappedFile != null) {
                    try {
                        this.mappedFile.close();
                    } catch (ErrnoException e) {
                    }
                    this.mappedFile = null;
                }
            }
        }

        private void checkNotClosed() throws IllegalStateException {
            if (this.closed) {
                throw new IllegalStateException("TzData is closed");
            }
        }

        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }

        public static String getRulesVersion(File file) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(file);
            Throwable th = null;
            try {
                byte[] bArr = new byte[12];
                int i = fileInputStream.read(bArr, 0, 12);
                if (i != 12) {
                    throw new IOException("File too short: only able to read " + i + " bytes.");
                }
                if (!new String(bArr, 0, 6, StandardCharsets.US_ASCII).equals(ZoneInfoDB.TZDATA_FILE) || bArr[11] != 0) {
                    throw new IOException("bad tzdata magic: " + Arrays.toString(bArr));
                }
                String str = new String(bArr, 6, 5, StandardCharsets.US_ASCII);
                fileInputStream.close();
                return str;
            } catch (Throwable th2) {
                if (0 != 0) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    fileInputStream.close();
                }
                throw th2;
            }
        }
    }

    private ZoneInfoDB() {
    }

    public static TzData getInstance() {
        return DATA;
    }
}
