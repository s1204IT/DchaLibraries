package libcore.util;

import android.icu.lang.UCharacterEnums;
import android.system.ErrnoException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libcore.io.BufferIterator;
import libcore.io.MemoryMappedFile;

public final class ZoneInfoDB {
    private static final TzData DATA = new TzData(System.getenv("ANDROID_DATA") + "/misc/zoneinfo/current/tzdata", System.getenv("ANDROID_ROOT") + "/usr/share/zoneinfo/tzdata");

    public static class TzData {
        private static final int CACHE_SIZE = 1;
        private int[] byteOffsets;
        private final BasicLruCache<String, ZoneInfo> cache = new BasicLruCache<String, ZoneInfo>(1) {
            @Override
            protected ZoneInfo create(String id) {
                BufferIterator it = TzData.this.getBufferIterator(id);
                if (it == null) {
                    return null;
                }
                return ZoneInfo.makeTimeZone(id, it);
            }
        };
        private String[] ids;
        private MemoryMappedFile mappedFile;
        private int[] rawUtcOffsetsCache;
        private String version;
        private String zoneTab;

        public TzData(String... paths) {
            for (String path : paths) {
                if (loadData(path)) {
                    return;
                }
            }
            System.logE("Couldn't find any tzdata!");
            this.version = "missing";
            this.zoneTab = "# Emergency fallback data.\n";
            this.ids = new String[]{"GMT"};
            int[] iArr = new int[1];
            this.rawUtcOffsetsCache = iArr;
            this.byteOffsets = iArr;
        }

        public BufferIterator getBufferIterator(String id) {
            int index = Arrays.binarySearch(this.ids, id);
            if (index < 0) {
                return null;
            }
            BufferIterator it = this.mappedFile.bigEndianIterator();
            it.skip(this.byteOffsets[index]);
            return it;
        }

        private boolean loadData(String path) {
            try {
                this.mappedFile = MemoryMappedFile.mmapRO(path);
                try {
                    readHeader();
                    return true;
                } catch (Exception ex) {
                    System.logE("tzdata file \"" + path + "\" was present but invalid!", ex);
                    return false;
                }
            } catch (ErrnoException e) {
                return false;
            }
        }

        private void readHeader() {
            BufferIterator it = this.mappedFile.bigEndianIterator();
            byte[] tzdata_version = new byte[12];
            it.readByteArray(tzdata_version, 0, tzdata_version.length);
            String magic = new String(tzdata_version, 0, 6, StandardCharsets.US_ASCII);
            if (!magic.equals("tzdata") || tzdata_version[11] != 0) {
                throw new RuntimeException("bad tzdata magic: " + Arrays.toString(tzdata_version));
            }
            this.version = new String(tzdata_version, 6, 5, StandardCharsets.US_ASCII);
            int index_offset = it.readInt();
            int data_offset = it.readInt();
            int zonetab_offset = it.readInt();
            readIndex(it, index_offset, data_offset);
            readZoneTab(it, zonetab_offset, ((int) this.mappedFile.size()) - zonetab_offset);
        }

        private void readZoneTab(BufferIterator it, int zoneTabOffset, int zoneTabSize) {
            byte[] bytes = new byte[zoneTabSize];
            it.seek(zoneTabOffset);
            it.readByteArray(bytes, 0, bytes.length);
            this.zoneTab = new String(bytes, 0, bytes.length, StandardCharsets.US_ASCII);
        }

        private void readIndex(BufferIterator it, int indexOffset, int dataOffset) {
            it.seek(indexOffset);
            byte[] idBytes = new byte[40];
            int indexSize = dataOffset - indexOffset;
            int entryCount = indexSize / 52;
            char[] idChars = new char[entryCount * 40];
            int[] idEnd = new int[entryCount];
            int idOffset = 0;
            this.byteOffsets = new int[entryCount];
            int i = 0;
            while (i < entryCount) {
                it.readByteArray(idBytes, 0, idBytes.length);
                this.byteOffsets[i] = it.readInt();
                int[] iArr = this.byteOffsets;
                iArr[i] = iArr[i] + dataOffset;
                int length = it.readInt();
                if (length < 44) {
                    throw new AssertionError("length in index file < sizeof(tzhead)");
                }
                it.skip(4);
                int len = idBytes.length;
                int j = 0;
                int idOffset2 = idOffset;
                while (j < len && idBytes[j] != 0) {
                    idChars[idOffset2] = (char) (idBytes[j] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                    j++;
                    idOffset2++;
                }
                idEnd[i] = idOffset2;
                i++;
                idOffset = idOffset2;
            }
            String allIds = new String(idChars, 0, idOffset);
            this.ids = new String[entryCount];
            int i2 = 0;
            while (i2 < entryCount) {
                this.ids[i2] = allIds.substring(i2 == 0 ? 0 : idEnd[i2 - 1], idEnd[i2]);
                i2++;
            }
        }

        public String[] getAvailableIDs() {
            return (String[]) this.ids.clone();
        }

        public String[] getAvailableIDs(int rawUtcOffset) {
            List<String> matches = new ArrayList<>();
            int[] rawUtcOffsets = getRawUtcOffsets();
            for (int i = 0; i < rawUtcOffsets.length; i++) {
                if (rawUtcOffsets[i] == rawUtcOffset) {
                    matches.add(this.ids[i]);
                }
            }
            return (String[]) matches.toArray(new String[matches.size()]);
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
            return this.version;
        }

        public String getZoneTab() {
            return this.zoneTab;
        }

        public ZoneInfo makeTimeZone(String id) throws IOException {
            ZoneInfo zoneInfo = this.cache.get(id);
            if (zoneInfo == null) {
                return null;
            }
            return (ZoneInfo) zoneInfo.clone();
        }

        public boolean hasTimeZone(String id) throws IOException {
            return this.cache.get(id) != null;
        }

        protected void finalize() throws Throwable {
            if (this.mappedFile != null) {
                this.mappedFile.close();
            }
            super.finalize();
        }
    }

    private ZoneInfoDB() {
    }

    public static TzData getInstance() {
        return DATA;
    }
}
