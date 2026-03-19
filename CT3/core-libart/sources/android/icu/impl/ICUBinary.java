package android.icu.impl;

import android.icu.lang.UCharacterEnums;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.VersionInfo;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ICUBinary {

    static final boolean f2assertionsDisabled;
    private static final byte CHAR_SET_ = 0;
    private static final byte CHAR_SIZE_ = 2;
    private static final String HEADER_AUTHENTICATION_FAILED_ = "ICU data file error: Header authentication failed, please check if you have a valid ICU data file";
    private static final byte MAGIC1 = -38;
    private static final byte MAGIC2 = 39;
    private static final String MAGIC_NUMBER_AUTHENTICATION_FAILED_ = "ICU data file error: Not an ICU data file";
    private static final List<DataFile> icuDataFiles;

    public interface Authenticate {
        boolean isDataVersionAcceptable(byte[] bArr);
    }

    private static final class DatPackageReader {

        static final boolean f3assertionsDisabled;
        private static final int DATA_FORMAT = 1131245124;
        private static final IsAcceptable IS_ACCEPTABLE;

        private DatPackageReader() {
        }

        private static final class IsAcceptable implements Authenticate {
            IsAcceptable(IsAcceptable isAcceptable) {
                this();
            }

            private IsAcceptable() {
            }

            @Override
            public boolean isDataVersionAcceptable(byte[] version) {
                return version[0] == 1;
            }
        }

        static {
            f3assertionsDisabled = !DatPackageReader.class.desiredAssertionStatus();
            IS_ACCEPTABLE = new IsAcceptable(null);
        }

        static boolean validate(ByteBuffer bytes) {
            try {
                ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
                int count = bytes.getInt(bytes.position());
                return count > 0 && (bytes.position() + 4) + (count * 24) <= bytes.capacity() && startsWithPackageName(bytes, getNameOffset(bytes, 0)) && startsWithPackageName(bytes, getNameOffset(bytes, count + (-1)));
            } catch (IOException e) {
                return false;
            }
        }

        private static boolean startsWithPackageName(ByteBuffer bytes, int start) {
            int length = "icudt56b".length() - 1;
            for (int i = 0; i < length; i++) {
                if (bytes.get(start + i) != "icudt56b".charAt(i)) {
                    return false;
                }
            }
            int length2 = length + 1;
            byte c = bytes.get(start + length);
            return (c == 98 || c == 108) && bytes.get(start + length2) == 47;
        }

        static ByteBuffer getData(ByteBuffer bytes, CharSequence key) {
            int index = binarySearch(bytes, key);
            if (index >= 0) {
                ByteBuffer data = bytes.duplicate();
                data.position(getDataOffset(bytes, index));
                data.limit(getDataOffset(bytes, index + 1));
                return ICUBinary.sliceWithOrder(data);
            }
            return null;
        }

        static void addBaseNamesInFolder(ByteBuffer bytes, String folder, String suffix, Set<String> names) {
            int index = binarySearch(bytes, folder);
            if (index < 0) {
                index = ~index;
            }
            int base = bytes.position();
            int count = bytes.getInt(base);
            StringBuilder sb = new StringBuilder();
            while (index < count && addBaseName(bytes, index, folder, suffix, sb, names)) {
                index++;
            }
        }

        private static int binarySearch(ByteBuffer bytes, CharSequence key) {
            int base = bytes.position();
            int count = bytes.getInt(base);
            int start = 0;
            int limit = count;
            while (start < limit) {
                int mid = (start + limit) >>> 1;
                int nameOffset = getNameOffset(bytes, mid);
                int result = ICUBinary.compareKeys(key, bytes, nameOffset + "icudt56b".length() + 1);
                if (result < 0) {
                    limit = mid;
                } else if (result > 0) {
                    start = mid + 1;
                } else {
                    return mid;
                }
            }
            return ~start;
        }

        private static int getNameOffset(ByteBuffer bytes, int index) {
            boolean z = false;
            int base = bytes.position();
            if (!f3assertionsDisabled) {
                if (index >= 0 && index < bytes.getInt(base)) {
                    z = true;
                }
                if (!z) {
                    throw new AssertionError();
                }
            }
            return bytes.getInt(base + 4 + (index * 8)) + base;
        }

        private static int getDataOffset(ByteBuffer bytes, int index) {
            boolean z = false;
            int base = bytes.position();
            int count = bytes.getInt(base);
            if (index == count) {
                return bytes.capacity();
            }
            if (!f3assertionsDisabled) {
                if (index >= 0 && index < count) {
                    z = true;
                }
                if (!z) {
                    throw new AssertionError();
                }
            }
            return bytes.getInt(base + 4 + 4 + (index * 8)) + base;
        }

        static boolean addBaseName(ByteBuffer bytes, int index, String folder, String suffix, StringBuilder sb, Set<String> names) {
            int offset = getNameOffset(bytes, index) + "icudt56b".length() + 1;
            if (folder.length() != 0) {
                int i = 0;
                while (i < folder.length()) {
                    if (bytes.get(offset) == folder.charAt(i)) {
                        i++;
                        offset++;
                    } else {
                        return false;
                    }
                }
                int offset2 = offset + 1;
                if (bytes.get(offset) != 47) {
                    return false;
                }
                offset = offset2;
            }
            sb.setLength(0);
            while (true) {
                int offset3 = offset + 1;
                byte b = bytes.get(offset);
                if (b != 0) {
                    char c = (char) b;
                    if (c == '/') {
                        return true;
                    }
                    sb.append(c);
                    offset = offset3;
                } else {
                    int nameLimit = sb.length() - suffix.length();
                    if (sb.lastIndexOf(suffix, nameLimit) >= 0) {
                        names.add(sb.substring(0, nameLimit));
                        return true;
                    }
                    return true;
                }
            }
        }
    }

    private static abstract class DataFile {
        protected final String itemPath;

        abstract void addBaseNamesInFolder(String str, String str2, Set<String> set);

        abstract ByteBuffer getData(String str);

        DataFile(String item) {
            this.itemPath = item;
        }

        public String toString() {
            return this.itemPath;
        }
    }

    private static final class SingleDataFile extends DataFile {
        private final File path;

        SingleDataFile(String item, File path) {
            super(item);
            this.path = path;
        }

        @Override
        public String toString() {
            return this.path.toString();
        }

        @Override
        ByteBuffer getData(String requestedPath) {
            if (requestedPath.equals(this.itemPath)) {
                return ICUBinary.mapFile(this.path);
            }
            return null;
        }

        @Override
        void addBaseNamesInFolder(String folder, String suffix, Set<String> names) {
            if (this.itemPath.length() <= folder.length() + suffix.length() || !this.itemPath.startsWith(folder) || !this.itemPath.endsWith(suffix) || this.itemPath.charAt(folder.length()) != '/' || this.itemPath.indexOf(47, folder.length() + 1) >= 0) {
                return;
            }
            names.add(this.itemPath.substring(folder.length() + 1, this.itemPath.length() - suffix.length()));
        }
    }

    private static final class PackageDataFile extends DataFile {
        private final ByteBuffer pkgBytes;

        PackageDataFile(String item, ByteBuffer bytes) {
            super(item);
            this.pkgBytes = bytes;
        }

        @Override
        ByteBuffer getData(String requestedPath) {
            return DatPackageReader.getData(this.pkgBytes, requestedPath);
        }

        @Override
        void addBaseNamesInFolder(String folder, String suffix, Set<String> names) {
            DatPackageReader.addBaseNamesInFolder(this.pkgBytes, folder, suffix, names);
        }
    }

    static {
        f2assertionsDisabled = !ICUBinary.class.desiredAssertionStatus();
        icuDataFiles = new ArrayList();
        String dataPath = ICUConfig.get(ICUBinary.class.getName() + ".dataPath");
        if (dataPath == null) {
            return;
        }
        addDataFilesFromPath(dataPath, icuDataFiles);
    }

    private static void addDataFilesFromPath(String dataPath, List<DataFile> files) {
        int pathLimit;
        int pathStart = 0;
        while (pathStart < dataPath.length()) {
            int sepIndex = dataPath.indexOf(File.pathSeparatorChar, pathStart);
            if (sepIndex >= 0) {
                pathLimit = sepIndex;
            } else {
                pathLimit = dataPath.length();
            }
            String path = dataPath.substring(pathStart, pathLimit).trim();
            if (path.endsWith(File.separator)) {
                path = path.substring(0, path.length() - 1);
            }
            if (path.length() != 0) {
                addDataFilesFromFolder(new File(path), new StringBuilder(), icuDataFiles);
            }
            if (sepIndex < 0) {
                return;
            } else {
                pathStart = sepIndex + 1;
            }
        }
    }

    private static void addDataFilesFromFolder(File folder, StringBuilder itemPath, List<DataFile> dataFiles) {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        int folderPathLength = itemPath.length();
        if (folderPathLength > 0) {
            itemPath.append('/');
            folderPathLength++;
        }
        for (File file : files) {
            String fileName = file.getName();
            if (!fileName.endsWith(".txt")) {
                itemPath.append(fileName);
                if (file.isDirectory()) {
                    addDataFilesFromFolder(file, itemPath, dataFiles);
                } else if (fileName.endsWith(".dat")) {
                    ByteBuffer pkgBytes = mapFile(file);
                    if (pkgBytes != null && DatPackageReader.validate(pkgBytes)) {
                        dataFiles.add(new PackageDataFile(itemPath.toString(), pkgBytes));
                    }
                } else {
                    dataFiles.add(new SingleDataFile(itemPath.toString(), file));
                }
                itemPath.setLength(folderPathLength);
            }
        }
    }

    static int compareKeys(CharSequence key, ByteBuffer bytes, int offset) {
        int i = 0;
        while (true) {
            int c2 = bytes.get(offset);
            if (c2 == 0) {
                return i == key.length() ? 0 : 1;
            }
            if (i == key.length()) {
                return -1;
            }
            int diff = key.charAt(i) - c2;
            if (diff == 0) {
                i++;
                offset++;
            } else {
                return diff;
            }
        }
    }

    static int compareKeys(CharSequence key, byte[] bytes, int offset) {
        int i = 0;
        while (true) {
            int c2 = bytes[offset];
            if (c2 == 0) {
                return i == key.length() ? 0 : 1;
            }
            if (i == key.length()) {
                return -1;
            }
            int diff = key.charAt(i) - c2;
            if (diff == 0) {
                i++;
                offset++;
            } else {
                return diff;
            }
        }
    }

    public static ByteBuffer getData(String itemPath) {
        return getData(null, null, itemPath, false);
    }

    public static ByteBuffer getData(ClassLoader loader, String resourceName, String itemPath) {
        return getData(loader, resourceName, itemPath, false);
    }

    public static ByteBuffer getRequiredData(String itemPath) {
        return getData(null, null, itemPath, true);
    }

    private static ByteBuffer getData(ClassLoader loader, String resourceName, String itemPath, boolean required) {
        ByteBuffer bytes = getDataFromFile(itemPath);
        if (bytes != null) {
            return bytes;
        }
        if (loader == null) {
            loader = ClassLoaderUtil.getClassLoader(ICUData.class);
        }
        if (resourceName == null) {
            resourceName = "android/icu/impl/data/icudt56b/" + itemPath;
        }
        try {
            InputStream is = ICUData.getStream(loader, resourceName, required);
            if (is == null) {
                return null;
            }
            ByteBuffer buffer = getByteBufferFromInputStreamAndCloseStream(is);
            return buffer;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static ByteBuffer getDataFromFile(String itemPath) {
        for (DataFile dataFile : icuDataFiles) {
            ByteBuffer data = dataFile.getData(itemPath);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private static ByteBuffer mapFile(File path) {
        try {
            FileInputStream file = new FileInputStream(path);
            FileChannel channel = file.getChannel();
            try {
                ByteBuffer bytes = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                return bytes;
            } finally {
                file.close();
            }
        } catch (FileNotFoundException ignored) {
            System.err.println(ignored);
            return null;
        } catch (IOException ignored2) {
            System.err.println(ignored2);
            return null;
        }
    }

    public static void addBaseNamesInFileFolder(String folder, String suffix, Set<String> names) {
        for (DataFile dataFile : icuDataFiles) {
            dataFile.addBaseNamesInFolder(folder, suffix, names);
        }
    }

    public static VersionInfo readHeaderAndDataVersion(ByteBuffer bytes, int dataFormat, Authenticate authenticate) throws IOException {
        return getVersionInfoFromCompactInt(readHeader(bytes, dataFormat, authenticate));
    }

    public static int readHeader(ByteBuffer bytes, int dataFormat, Authenticate authenticate) throws IOException {
        if (!f2assertionsDisabled) {
            if (!(bytes.position() == 0)) {
                throw new AssertionError();
            }
        }
        byte magic1 = bytes.get(2);
        byte magic2 = bytes.get(3);
        if (magic1 != -38 || magic2 != 39) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }
        byte isBigEndian = bytes.get(8);
        byte charsetFamily = bytes.get(9);
        byte sizeofUChar = bytes.get(10);
        if (isBigEndian < 0 || 1 < isBigEndian || charsetFamily != 0 || sizeofUChar != 2) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        bytes.order(isBigEndian != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        int headerSize = bytes.getChar(0);
        int sizeofUDataInfo = bytes.getChar(4);
        if (sizeofUDataInfo < 20 || headerSize < sizeofUDataInfo + 4) {
            throw new IOException("Internal Error: Header size error");
        }
        byte[] formatVersion = {bytes.get(16), bytes.get(17), bytes.get(18), bytes.get(19)};
        if (bytes.get(12) != ((byte) (dataFormat >> 24)) || bytes.get(13) != ((byte) (dataFormat >> 16)) || bytes.get(14) != ((byte) (dataFormat >> 8)) || bytes.get(15) != ((byte) dataFormat) || (authenticate != null && !authenticate.isDataVersionAcceptable(formatVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_ + String.format("; data format %02x%02x%02x%02x, format version %d.%d.%d.%d", Byte.valueOf(bytes.get(12)), Byte.valueOf(bytes.get(13)), Byte.valueOf(bytes.get(14)), Byte.valueOf(bytes.get(15)), Integer.valueOf(formatVersion[0] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED), Integer.valueOf(formatVersion[1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED), Integer.valueOf(formatVersion[2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED), Integer.valueOf(formatVersion[3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)));
        }
        bytes.position(headerSize);
        return (bytes.get(20) << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) | ((bytes.get(21) & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bytes.get(22) & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (bytes.get(23) & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
    }

    public static int writeHeader(int dataFormat, int formatVersion, int dataVersion, DataOutputStream dos) throws IOException {
        dos.writeChar(32);
        dos.writeByte(-38);
        dos.writeByte(39);
        dos.writeChar(20);
        dos.writeChar(0);
        dos.writeByte(1);
        dos.writeByte(0);
        dos.writeByte(2);
        dos.writeByte(0);
        dos.writeInt(dataFormat);
        dos.writeInt(formatVersion);
        dos.writeInt(dataVersion);
        dos.writeLong(0L);
        if (!f2assertionsDisabled) {
            if (!(dos.size() == 32)) {
                throw new AssertionError();
            }
        }
        return 32;
    }

    public static void skipBytes(ByteBuffer bytes, int skipLength) {
        if (skipLength <= 0) {
            return;
        }
        bytes.position(bytes.position() + skipLength);
    }

    public static String getString(ByteBuffer bytes, int length, int additionalSkipLength) {
        CharSequence cs = bytes.asCharBuffer();
        String s = cs.subSequence(0, length).toString();
        skipBytes(bytes, (length * 2) + additionalSkipLength);
        return s;
    }

    public static char[] getChars(ByteBuffer bytes, int length, int additionalSkipLength) {
        char[] dest = new char[length];
        bytes.asCharBuffer().get(dest);
        skipBytes(bytes, (length * 2) + additionalSkipLength);
        return dest;
    }

    public static short[] getShorts(ByteBuffer bytes, int length, int additionalSkipLength) {
        short[] dest = new short[length];
        bytes.asShortBuffer().get(dest);
        skipBytes(bytes, (length * 2) + additionalSkipLength);
        return dest;
    }

    public static int[] getInts(ByteBuffer bytes, int length, int additionalSkipLength) {
        int[] dest = new int[length];
        bytes.asIntBuffer().get(dest);
        skipBytes(bytes, (length * 4) + additionalSkipLength);
        return dest;
    }

    public static long[] getLongs(ByteBuffer bytes, int length, int additionalSkipLength) {
        long[] dest = new long[length];
        bytes.asLongBuffer().get(dest);
        skipBytes(bytes, (length * 8) + additionalSkipLength);
        return dest;
    }

    public static ByteBuffer sliceWithOrder(ByteBuffer bytes) {
        ByteBuffer b = bytes.slice();
        return b.order(bytes.order());
    }

    public static ByteBuffer getByteBufferFromInputStreamAndCloseStream(InputStream is) throws IOException {
        byte[] bytes;
        int length;
        try {
            int avail = is.available();
            if (avail > 32) {
                bytes = new byte[avail];
            } else {
                bytes = new byte[128];
            }
            int length2 = 0;
            while (true) {
                if (length2 < bytes.length) {
                    int numRead = is.read(bytes, length2, bytes.length - length2);
                    if (numRead < 0) {
                        break;
                    }
                    length = length2 + numRead;
                    length2 = length;
                } else {
                    int nextByte = is.read();
                    if (nextByte < 0) {
                        break;
                    }
                    int capacity = bytes.length * 2;
                    if (capacity < 128) {
                        capacity = 128;
                    } else if (capacity < 16384) {
                        capacity *= 2;
                    }
                    byte[] newBytes = new byte[capacity];
                    System.arraycopy(bytes, 0, newBytes, 0, length2);
                    bytes = newBytes;
                    length = length2 + 1;
                    newBytes[length2] = (byte) nextByte;
                    length2 = length;
                }
            }
            return ByteBuffer.wrap(bytes, 0, length2);
        } finally {
            is.close();
        }
    }

    public static VersionInfo getVersionInfoFromCompactInt(int version) {
        return VersionInfo.getInstance(version >>> 24, (version >> 16) & 255, (version >> 8) & 255, version & 255);
    }

    public static byte[] getVersionByteArrayFromCompactInt(int version) {
        return new byte[]{(byte) (version >> 24), (byte) (version >> 16), (byte) (version >> 8), (byte) version};
    }
}
