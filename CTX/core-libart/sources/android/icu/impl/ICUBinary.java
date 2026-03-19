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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class ICUBinary {
    static final boolean $assertionsDisabled = false;
    private static final byte CHAR_SET_ = 0;
    private static final byte CHAR_SIZE_ = 2;
    private static final String HEADER_AUTHENTICATION_FAILED_ = "ICU data file error: Header authentication failed, please check if you have a valid ICU data file";
    private static final byte MAGIC1 = -38;
    private static final byte MAGIC2 = 39;
    private static final String MAGIC_NUMBER_AUTHENTICATION_FAILED_ = "ICU data file error: Not an ICU data file";
    private static final List<DataFile> icuDataFiles = new ArrayList();

    public interface Authenticate {
        boolean isDataVersionAcceptable(byte[] bArr);
    }

    static {
        String str = ICUConfig.get(ICUBinary.class.getName() + ".dataPath");
        if (str != null) {
            addDataFilesFromPath(str, icuDataFiles);
        }
    }

    private static final class DatPackageReader {
        static final boolean $assertionsDisabled = false;
        private static final int DATA_FORMAT = 1131245124;
        private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();

        private DatPackageReader() {
        }

        private static final class IsAcceptable implements Authenticate {
            private IsAcceptable() {
            }

            @Override
            public boolean isDataVersionAcceptable(byte[] bArr) {
                return bArr[0] == 1;
            }
        }

        static boolean validate(ByteBuffer byteBuffer) {
            try {
                ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
                int i = byteBuffer.getInt(byteBuffer.position());
                return i > 0 && (byteBuffer.position() + 4) + (i * 24) <= byteBuffer.capacity() && startsWithPackageName(byteBuffer, getNameOffset(byteBuffer, 0)) && startsWithPackageName(byteBuffer, getNameOffset(byteBuffer, i - 1));
            } catch (IOException e) {
                return false;
            }
        }

        private static boolean startsWithPackageName(ByteBuffer byteBuffer, int i) {
            int length = "icudt60b".length() - 1;
            for (int i2 = 0; i2 < length; i2++) {
                if (byteBuffer.get(i + i2) != "icudt60b".charAt(i2)) {
                    return false;
                }
            }
            int i3 = length + 1;
            byte b = byteBuffer.get(length + i);
            return (b == 98 || b == 108) && byteBuffer.get(i + i3) == 47;
        }

        static ByteBuffer getData(ByteBuffer byteBuffer, CharSequence charSequence) {
            int iBinarySearch = binarySearch(byteBuffer, charSequence);
            if (iBinarySearch >= 0) {
                ByteBuffer byteBufferDuplicate = byteBuffer.duplicate();
                byteBufferDuplicate.position(getDataOffset(byteBuffer, iBinarySearch));
                byteBufferDuplicate.limit(getDataOffset(byteBuffer, iBinarySearch + 1));
                return ICUBinary.sliceWithOrder(byteBufferDuplicate);
            }
            return null;
        }

        static void addBaseNamesInFolder(ByteBuffer byteBuffer, String str, String str2, Set<String> set) {
            int iBinarySearch = binarySearch(byteBuffer, str);
            if (iBinarySearch < 0) {
                iBinarySearch = ~iBinarySearch;
            }
            int i = byteBuffer.getInt(byteBuffer.position());
            StringBuilder sb = new StringBuilder();
            while (iBinarySearch < i && addBaseName(byteBuffer, iBinarySearch, str, str2, sb, set)) {
                iBinarySearch++;
            }
        }

        private static int binarySearch(ByteBuffer byteBuffer, CharSequence charSequence) {
            int i = byteBuffer.getInt(byteBuffer.position());
            int i2 = 0;
            while (i2 < i) {
                int i3 = (i2 + i) >>> 1;
                int iCompareKeys = ICUBinary.compareKeys(charSequence, byteBuffer, getNameOffset(byteBuffer, i3) + "icudt60b".length() + 1);
                if (iCompareKeys >= 0) {
                    if (iCompareKeys > 0) {
                        i2 = i3 + 1;
                    } else {
                        return i3;
                    }
                } else {
                    i = i3;
                }
            }
            return ~i2;
        }

        private static int getNameOffset(ByteBuffer byteBuffer, int i) {
            int iPosition = byteBuffer.position();
            return iPosition + byteBuffer.getInt(iPosition + 4 + (i * 8));
        }

        private static int getDataOffset(ByteBuffer byteBuffer, int i) {
            int iPosition = byteBuffer.position();
            if (i == byteBuffer.getInt(iPosition)) {
                return byteBuffer.capacity();
            }
            return iPosition + byteBuffer.getInt(iPosition + 4 + 4 + (i * 8));
        }

        static boolean addBaseName(ByteBuffer byteBuffer, int i, String str, String str2, StringBuilder sb, Set<String> set) {
            int nameOffset = getNameOffset(byteBuffer, i) + "icudt60b".length() + 1;
            if (str.length() != 0) {
                int i2 = nameOffset;
                int i3 = 0;
                while (i3 < str.length()) {
                    if (byteBuffer.get(i2) != str.charAt(i3)) {
                        return false;
                    }
                    i3++;
                    i2++;
                }
                nameOffset = i2 + 1;
                if (byteBuffer.get(i2) != 47) {
                    return false;
                }
            }
            sb.setLength(0);
            while (true) {
                int i4 = nameOffset + 1;
                byte b = byteBuffer.get(nameOffset);
                if (b != 0) {
                    char c = (char) b;
                    if (c == '/') {
                        return true;
                    }
                    sb.append(c);
                    nameOffset = i4;
                } else {
                    int length = sb.length() - str2.length();
                    if (sb.lastIndexOf(str2, length) >= 0) {
                        set.add(sb.substring(0, length));
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

        DataFile(String str) {
            this.itemPath = str;
        }

        public String toString() {
            return this.itemPath;
        }
    }

    private static final class SingleDataFile extends DataFile {
        private final File path;

        SingleDataFile(String str, File file) {
            super(str);
            this.path = file;
        }

        @Override
        public String toString() {
            return this.path.toString();
        }

        @Override
        ByteBuffer getData(String str) {
            if (str.equals(this.itemPath)) {
                return ICUBinary.mapFile(this.path);
            }
            return null;
        }

        @Override
        void addBaseNamesInFolder(String str, String str2, Set<String> set) {
            if (this.itemPath.length() > str.length() + str2.length() && this.itemPath.startsWith(str) && this.itemPath.endsWith(str2) && this.itemPath.charAt(str.length()) == '/' && this.itemPath.indexOf(47, str.length() + 1) < 0) {
                set.add(this.itemPath.substring(str.length() + 1, this.itemPath.length() - str2.length()));
            }
        }
    }

    private static final class PackageDataFile extends DataFile {
        private final ByteBuffer pkgBytes;

        PackageDataFile(String str, ByteBuffer byteBuffer) {
            super(str);
            this.pkgBytes = byteBuffer;
        }

        @Override
        ByteBuffer getData(String str) {
            return DatPackageReader.getData(this.pkgBytes, str);
        }

        @Override
        void addBaseNamesInFolder(String str, String str2, Set<String> set) {
            DatPackageReader.addBaseNamesInFolder(this.pkgBytes, str, str2, set);
        }
    }

    private static void addDataFilesFromPath(String str, List<DataFile> list) {
        int length;
        int i = 0;
        while (i < str.length()) {
            int iIndexOf = str.indexOf(File.pathSeparatorChar, i);
            if (iIndexOf < 0) {
                length = str.length();
            } else {
                length = iIndexOf;
            }
            String strTrim = str.substring(i, length).trim();
            if (strTrim.endsWith(File.separator)) {
                strTrim = strTrim.substring(0, strTrim.length() - 1);
            }
            if (strTrim.length() != 0) {
                addDataFilesFromFolder(new File(strTrim), new StringBuilder(), icuDataFiles);
            }
            if (iIndexOf >= 0) {
                i = iIndexOf + 1;
            } else {
                return;
            }
        }
    }

    private static void addDataFilesFromFolder(File file, StringBuilder sb, List<DataFile> list) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null || fileArrListFiles.length == 0) {
            return;
        }
        int length = sb.length();
        if (length > 0) {
            sb.append('/');
            length++;
        }
        for (File file2 : fileArrListFiles) {
            String name = file2.getName();
            if (!name.endsWith(".txt")) {
                sb.append(name);
                if (file2.isDirectory()) {
                    addDataFilesFromFolder(file2, sb, list);
                } else if (name.endsWith(".dat")) {
                    ByteBuffer byteBufferMapFile = mapFile(file2);
                    if (byteBufferMapFile != null && DatPackageReader.validate(byteBufferMapFile)) {
                        list.add(new PackageDataFile(sb.toString(), byteBufferMapFile));
                    }
                } else {
                    list.add(new SingleDataFile(sb.toString(), file2));
                }
                sb.setLength(length);
            }
        }
    }

    static int compareKeys(CharSequence charSequence, ByteBuffer byteBuffer, int i) {
        int i2 = 0;
        while (true) {
            byte b = byteBuffer.get(i);
            if (b == 0) {
                if (i2 == charSequence.length()) {
                    return 0;
                }
                return 1;
            }
            if (i2 == charSequence.length()) {
                return -1;
            }
            int iCharAt = charSequence.charAt(i2) - b;
            if (iCharAt == 0) {
                i2++;
                i++;
            } else {
                return iCharAt;
            }
        }
    }

    static int compareKeys(CharSequence charSequence, byte[] bArr, int i) {
        int i2 = 0;
        while (true) {
            byte b = bArr[i];
            if (b == 0) {
                if (i2 == charSequence.length()) {
                    return 0;
                }
                return 1;
            }
            if (i2 == charSequence.length()) {
                return -1;
            }
            int iCharAt = charSequence.charAt(i2) - b;
            if (iCharAt == 0) {
                i2++;
                i++;
            } else {
                return iCharAt;
            }
        }
    }

    public static ByteBuffer getData(String str) {
        return getData(null, null, str, false);
    }

    public static ByteBuffer getData(ClassLoader classLoader, String str, String str2) {
        return getData(classLoader, str, str2, false);
    }

    public static ByteBuffer getRequiredData(String str) {
        return getData(null, null, str, true);
    }

    private static ByteBuffer getData(ClassLoader classLoader, String str, String str2, boolean z) {
        ByteBuffer dataFromFile = getDataFromFile(str2);
        if (dataFromFile != null) {
            return dataFromFile;
        }
        if (classLoader == null) {
            classLoader = ClassLoaderUtil.getClassLoader(ICUData.class);
        }
        if (str == null) {
            str = "android/icu/impl/data/icudt60b/" + str2;
        }
        try {
            InputStream stream = ICUData.getStream(classLoader, str, z);
            if (stream == null) {
                return null;
            }
            return getByteBufferFromInputStreamAndCloseStream(stream);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static ByteBuffer getDataFromFile(String str) {
        Iterator<DataFile> it = icuDataFiles.iterator();
        while (it.hasNext()) {
            ByteBuffer data = it.next().getData(str);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private static ByteBuffer mapFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            FileChannel channel = fileInputStream.getChannel();
            try {
                return channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
            } finally {
                fileInputStream.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
            return null;
        } catch (IOException e2) {
            System.err.println(e2);
            return null;
        }
    }

    public static void addBaseNamesInFileFolder(String str, String str2, Set<String> set) {
        Iterator<DataFile> it = icuDataFiles.iterator();
        while (it.hasNext()) {
            it.next().addBaseNamesInFolder(str, str2, set);
        }
    }

    public static VersionInfo readHeaderAndDataVersion(ByteBuffer byteBuffer, int i, Authenticate authenticate) throws IOException {
        return getVersionInfoFromCompactInt(readHeader(byteBuffer, i, authenticate));
    }

    public static int readHeader(ByteBuffer byteBuffer, int i, Authenticate authenticate) throws IOException {
        byte b = byteBuffer.get(2);
        byte b2 = byteBuffer.get(3);
        if (b != -38 || b2 != 39) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }
        byte b3 = byteBuffer.get(8);
        byte b4 = byteBuffer.get(9);
        byte b5 = byteBuffer.get(10);
        if (b3 < 0 || 1 < b3 || b4 != 0 || b5 != 2) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        byteBuffer.order(b3 != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        char c = byteBuffer.getChar(0);
        char c2 = byteBuffer.getChar(4);
        if (c2 < 20 || c < c2 + 4) {
            throw new IOException("Internal Error: Header size error");
        }
        byte[] bArr = {byteBuffer.get(16), byteBuffer.get(17), byteBuffer.get(18), byteBuffer.get(19)};
        if (byteBuffer.get(12) != ((byte) (i >> 24)) || byteBuffer.get(13) != ((byte) (i >> 16)) || byteBuffer.get(14) != ((byte) (i >> 8)) || byteBuffer.get(15) != ((byte) i) || (authenticate != null && !authenticate.isDataVersionAcceptable(bArr))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_ + String.format("; data format %02x%02x%02x%02x, format version %d.%d.%d.%d", Byte.valueOf(byteBuffer.get(12)), Byte.valueOf(byteBuffer.get(13)), Byte.valueOf(byteBuffer.get(14)), Byte.valueOf(byteBuffer.get(15)), Integer.valueOf(bArr[0] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED), Integer.valueOf(bArr[1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED), Integer.valueOf(bArr[2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED), Integer.valueOf(bArr[3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)));
        }
        byteBuffer.position(c);
        return (byteBuffer.get(23) & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | (byteBuffer.get(20) << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) | ((byteBuffer.get(21) & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((byteBuffer.get(22) & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
    }

    public static int writeHeader(int i, int i2, int i3, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeChar(32);
        dataOutputStream.writeByte(-38);
        dataOutputStream.writeByte(39);
        dataOutputStream.writeChar(20);
        dataOutputStream.writeChar(0);
        dataOutputStream.writeByte(1);
        dataOutputStream.writeByte(0);
        dataOutputStream.writeByte(2);
        dataOutputStream.writeByte(0);
        dataOutputStream.writeInt(i);
        dataOutputStream.writeInt(i2);
        dataOutputStream.writeInt(i3);
        dataOutputStream.writeLong(0L);
        return 32;
    }

    public static void skipBytes(ByteBuffer byteBuffer, int i) {
        if (i > 0) {
            byteBuffer.position(byteBuffer.position() + i);
        }
    }

    public static String getString(ByteBuffer byteBuffer, int i, int i2) {
        String string = byteBuffer.asCharBuffer().subSequence(0, i).toString();
        skipBytes(byteBuffer, (i * 2) + i2);
        return string;
    }

    public static char[] getChars(ByteBuffer byteBuffer, int i, int i2) {
        char[] cArr = new char[i];
        byteBuffer.asCharBuffer().get(cArr);
        skipBytes(byteBuffer, (i * 2) + i2);
        return cArr;
    }

    public static short[] getShorts(ByteBuffer byteBuffer, int i, int i2) {
        short[] sArr = new short[i];
        byteBuffer.asShortBuffer().get(sArr);
        skipBytes(byteBuffer, (i * 2) + i2);
        return sArr;
    }

    public static int[] getInts(ByteBuffer byteBuffer, int i, int i2) {
        int[] iArr = new int[i];
        byteBuffer.asIntBuffer().get(iArr);
        skipBytes(byteBuffer, (i * 4) + i2);
        return iArr;
    }

    public static long[] getLongs(ByteBuffer byteBuffer, int i, int i2) {
        long[] jArr = new long[i];
        byteBuffer.asLongBuffer().get(jArr);
        skipBytes(byteBuffer, (i * 8) + i2);
        return jArr;
    }

    public static ByteBuffer sliceWithOrder(ByteBuffer byteBuffer) {
        return byteBuffer.slice().order(byteBuffer.order());
    }

    public static ByteBuffer getByteBufferFromInputStreamAndCloseStream(InputStream inputStream) throws IOException {
        byte[] bArr;
        try {
            int iAvailable = inputStream.available();
            if (iAvailable > 32) {
                bArr = new byte[iAvailable];
            } else {
                bArr = new byte[128];
            }
            byte[] bArr2 = bArr;
            int i = 0;
            while (true) {
                if (i < bArr2.length) {
                    int i2 = inputStream.read(bArr2, i, bArr2.length - i);
                    if (i2 < 0) {
                        break;
                    }
                    i += i2;
                } else {
                    int i3 = inputStream.read();
                    if (i3 < 0) {
                        break;
                    }
                    int length = bArr2.length * 2;
                    if (length >= 128) {
                        if (length < 16384) {
                            length *= 2;
                        }
                    } else {
                        length = 128;
                    }
                    byte[] bArr3 = new byte[length];
                    System.arraycopy(bArr2, 0, bArr3, 0, i);
                    int i4 = i + 1;
                    bArr3[i] = (byte) i3;
                    i = i4;
                    bArr2 = bArr3;
                }
            }
            return ByteBuffer.wrap(bArr2, 0, i);
        } finally {
            inputStream.close();
        }
    }

    public static VersionInfo getVersionInfoFromCompactInt(int i) {
        return VersionInfo.getInstance(i >>> 24, (i >> 16) & 255, (i >> 8) & 255, i & 255);
    }

    public static byte[] getVersionByteArrayFromCompactInt(int i) {
        return new byte[]{(byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i};
    }
}
