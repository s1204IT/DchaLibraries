package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.impl.UResource;
import android.icu.impl.locale.BaseLocale;
import android.icu.lang.UCharacterEnums;
import android.icu.util.ICUException;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import android.icu.util.UResourceTypeMismatchException;
import android.icu.util.VersionInfo;
import com.android.dex.DexFormat;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;

public final class ICUResourceBundleReader {

    static final boolean f5assertionsDisabled;
    private static ReaderCache CACHE = null;
    private static final int DATA_FORMAT = 1382380354;
    private static final boolean DEBUG = false;
    private static final CharBuffer EMPTY_16_BIT_UNITS;
    private static final Array EMPTY_ARRAY;
    private static final Table EMPTY_TABLE;
    private static final String ICU_RESOURCE_SUFFIX = ".res";
    private static final IsAcceptable IS_ACCEPTABLE;
    static final int LARGE_SIZE = 24;
    private static final ICUResourceBundleReader NULL_READER;
    private static int[] PUBLIC_TYPES = null;
    private static final int URES_ATT_IS_POOL_BUNDLE = 2;
    private static final int URES_ATT_NO_FALLBACK = 1;
    private static final int URES_ATT_USES_POOL_BUNDLE = 4;
    private static final int URES_INDEX_16BIT_TOP = 6;
    private static final int URES_INDEX_ATTRIBUTES = 5;
    private static final int URES_INDEX_BUNDLE_TOP = 3;
    private static final int URES_INDEX_KEYS_TOP = 1;
    private static final int URES_INDEX_LENGTH = 0;
    private static final int URES_INDEX_MAX_TABLE_LENGTH = 4;
    private static final int URES_INDEX_POOL_CHECKSUM = 7;
    private static final ByteBuffer emptyByteBuffer;
    private static final byte[] emptyBytes;
    private static final char[] emptyChars;
    private static final int[] emptyInts;
    private static final String emptyString = "";
    private CharBuffer b16BitUnits;
    private ByteBuffer bytes;
    private int dataVersion;
    private boolean isPoolBundle;
    private byte[] keyBytes;
    private int localKeyLimit;
    private boolean noFallback;
    private ICUResourceBundleReader poolBundleReader;
    private int poolCheckSum;
    private int poolStringIndex16Limit;
    private int poolStringIndexLimit;
    private ResourceCache resourceCache;
    private int rootRes;
    private boolean usesPoolBundle;

    ICUResourceBundleReader(ByteBuffer inBytes, String baseName, String localeID, ClassLoader loader, ICUResourceBundleReader iCUResourceBundleReader) {
        this(inBytes, baseName, localeID, loader);
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        IsAcceptable(IsAcceptable isAcceptable) {
            this();
        }

        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] formatVersion) {
            if (formatVersion[0] != 1 || (formatVersion[1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 1) {
                return 2 <= formatVersion[0] && formatVersion[0] <= 3;
            }
            return true;
        }
    }

    static {
        IsAcceptable isAcceptable = null;
        Object[] objArr = 0;
        f5assertionsDisabled = !ICUResourceBundleReader.class.desiredAssertionStatus();
        IS_ACCEPTABLE = new IsAcceptable(isAcceptable);
        EMPTY_16_BIT_UNITS = CharBuffer.wrap(DexFormat.MAGIC_SUFFIX);
        CACHE = new ReaderCache(objArr == true ? 1 : 0);
        NULL_READER = new ICUResourceBundleReader();
        emptyBytes = new byte[0];
        emptyByteBuffer = ByteBuffer.allocate(0).asReadOnlyBuffer();
        emptyChars = new char[0];
        emptyInts = new int[0];
        EMPTY_ARRAY = new Array();
        EMPTY_TABLE = new Table();
        PUBLIC_TYPES = new int[]{0, 1, 2, 3, 2, 2, 0, 7, 8, 8, -1, -1, -1, -1, 14, -1};
    }

    private static class ReaderCacheKey {
        final String baseName;
        final String localeID;

        ReaderCacheKey(String baseName, String localeID) {
            this.baseName = baseName == null ? "" : baseName;
            this.localeID = localeID == null ? "" : localeID;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReaderCacheKey)) {
                return false;
            }
            ReaderCacheKey info = (ReaderCacheKey) obj;
            if (this.baseName.equals(info.baseName)) {
                return this.localeID.equals(info.localeID);
            }
            return false;
        }

        public int hashCode() {
            return this.baseName.hashCode() ^ this.localeID.hashCode();
        }
    }

    private static class ReaderCache extends SoftCache<ReaderCacheKey, ICUResourceBundleReader, ClassLoader> {
        ReaderCache(ReaderCache readerCache) {
            this();
        }

        private ReaderCache() {
        }

        @Override
        protected ICUResourceBundleReader createInstance(ReaderCacheKey key, ClassLoader loader) {
            ByteBuffer inBytes;
            String fullName = ICUResourceBundleReader.getFullName(key.baseName, key.localeID);
            try {
                if (key.baseName != null && key.baseName.startsWith("android/icu/impl/data/icudt56b")) {
                    String itemPath = fullName.substring("android/icu/impl/data/icudt56b".length() + 1);
                    inBytes = ICUBinary.getData(loader, fullName, itemPath);
                    if (inBytes == null) {
                        return ICUResourceBundleReader.NULL_READER;
                    }
                } else {
                    InputStream stream = ICUData.getStream(loader, fullName);
                    if (stream == null) {
                        return ICUResourceBundleReader.NULL_READER;
                    }
                    inBytes = ICUBinary.getByteBufferFromInputStreamAndCloseStream(stream);
                }
                return new ICUResourceBundleReader(inBytes, key.baseName, key.localeID, loader, null);
            } catch (IOException ex) {
                throw new ICUUncheckedIOException("Data file " + fullName + " is corrupt - " + ex.getMessage(), ex);
            }
        }
    }

    private ICUResourceBundleReader() {
    }

    private ICUResourceBundleReader(ByteBuffer inBytes, String baseName, String localeID, ClassLoader loader) throws IOException {
        init(inBytes);
        if (!this.usesPoolBundle) {
            return;
        }
        this.poolBundleReader = getReader(baseName, "pool", loader);
        if (!this.poolBundleReader.isPoolBundle) {
            throw new IllegalStateException("pool.res is not a pool bundle");
        }
        if (this.poolBundleReader.poolCheckSum == this.poolCheckSum) {
        } else {
            throw new IllegalStateException("pool.res has a different checksum than this bundle");
        }
    }

    static ICUResourceBundleReader getReader(String baseName, String localeID, ClassLoader root) {
        ReaderCacheKey info = new ReaderCacheKey(baseName, localeID);
        ICUResourceBundleReader reader = CACHE.getInstance(info, root);
        if (reader == NULL_READER) {
            return null;
        }
        return reader;
    }

    private void init(ByteBuffer inBytes) throws IOException {
        int _16BitTop;
        this.dataVersion = ICUBinary.readHeader(inBytes, DATA_FORMAT, IS_ACCEPTABLE);
        int majorFormatVersion = inBytes.get(16);
        this.bytes = ICUBinary.sliceWithOrder(inBytes);
        int dataLength = this.bytes.remaining();
        this.rootRes = this.bytes.getInt(0);
        int indexes0 = getIndexesInt(0);
        int indexLength = indexes0 & 255;
        if (indexLength <= 4) {
            throw new ICUException("not enough indexes");
        }
        if (dataLength >= ((indexLength + 1) << 2)) {
            int bundleTop = getIndexesInt(3);
            if (dataLength >= (bundleTop << 2)) {
                int maxOffset = bundleTop - 1;
                if (majorFormatVersion >= 3) {
                    this.poolStringIndexLimit = indexes0 >>> 8;
                }
                if (indexLength > 5) {
                    int att = getIndexesInt(5);
                    this.noFallback = (att & 1) != 0;
                    this.isPoolBundle = (att & 2) != 0;
                    this.usesPoolBundle = (att & 4) != 0;
                    this.poolStringIndexLimit |= (61440 & att) << 12;
                    this.poolStringIndex16Limit = att >>> 16;
                }
                int keysBottom = indexLength + 1;
                int keysTop = getIndexesInt(1);
                if (keysTop > keysBottom) {
                    if (this.isPoolBundle) {
                        this.keyBytes = new byte[(keysTop - keysBottom) << 2];
                        this.bytes.position(keysBottom << 2);
                    } else {
                        this.localKeyLimit = keysTop << 2;
                        this.keyBytes = new byte[this.localKeyLimit];
                    }
                    this.bytes.get(this.keyBytes);
                }
                if (indexLength > 6 && (_16BitTop = getIndexesInt(6)) > keysTop) {
                    int num16BitUnits = (_16BitTop - keysTop) * 2;
                    this.bytes.position(keysTop << 2);
                    this.b16BitUnits = this.bytes.asCharBuffer();
                    this.b16BitUnits.limit(num16BitUnits);
                    maxOffset |= num16BitUnits - 1;
                } else {
                    this.b16BitUnits = EMPTY_16_BIT_UNITS;
                }
                if (indexLength > 7) {
                    this.poolCheckSum = getIndexesInt(7);
                }
                if (!this.isPoolBundle || this.b16BitUnits.length() > 1) {
                    this.resourceCache = new ResourceCache(maxOffset);
                }
                this.bytes.position(0);
                return;
            }
        }
        throw new ICUException("not enough bytes");
    }

    private int getIndexesInt(int i) {
        return this.bytes.getInt((i + 1) << 2);
    }

    VersionInfo getVersion() {
        return ICUBinary.getVersionInfoFromCompactInt(this.dataVersion);
    }

    int getRootResource() {
        return this.rootRes;
    }

    boolean getNoFallback() {
        return this.noFallback;
    }

    boolean getUsesPoolBundle() {
        return this.usesPoolBundle;
    }

    static int RES_GET_TYPE(int res) {
        return res >>> 28;
    }

    private static int RES_GET_OFFSET(int res) {
        return 268435455 & res;
    }

    private int getResourceByteOffset(int offset) {
        return offset << 2;
    }

    static int RES_GET_INT(int res) {
        return (res << 4) >> 4;
    }

    static int RES_GET_UINT(int res) {
        return 268435455 & res;
    }

    static boolean URES_IS_ARRAY(int type) {
        return type == 8 || type == 9;
    }

    static boolean URES_IS_TABLE(int type) {
        return type == 2 || type == 5 || type == 4;
    }

    private char[] getChars(int offset, int count) {
        char[] chars = new char[count];
        if (count <= 16) {
            for (int i = 0; i < count; i++) {
                chars[i] = this.bytes.getChar(offset);
                offset += 2;
            }
        } else {
            CharBuffer temp = this.bytes.asCharBuffer();
            temp.position(offset / 2);
            temp.get(chars);
        }
        return chars;
    }

    private int getInt(int offset) {
        return this.bytes.getInt(offset);
    }

    private int[] getInts(int offset, int count) {
        int[] ints = new int[count];
        if (count <= 16) {
            for (int i = 0; i < count; i++) {
                ints[i] = this.bytes.getInt(offset);
                offset += 4;
            }
        } else {
            IntBuffer temp = this.bytes.asIntBuffer();
            temp.position(offset / 4);
            temp.get(ints);
        }
        return ints;
    }

    private char[] getTable16KeyOffsets(int offset) {
        int offset2 = offset + 1;
        int length = this.b16BitUnits.charAt(offset);
        if (length > 0) {
            char[] result = new char[length];
            if (length <= 16) {
                int i = 0;
                while (i < length) {
                    result[i] = this.b16BitUnits.charAt(offset2);
                    i++;
                    offset2++;
                }
            } else {
                CharBuffer temp = this.b16BitUnits.duplicate();
                temp.position(offset2);
                temp.get(result);
            }
            return result;
        }
        return emptyChars;
    }

    private char[] getTableKeyOffsets(int offset) {
        int length = this.bytes.getChar(offset);
        if (length > 0) {
            return getChars(offset + 2, length);
        }
        return emptyChars;
    }

    private int[] getTable32KeyOffsets(int offset) {
        int length = getInt(offset);
        if (length > 0) {
            return getInts(offset + 4, length);
        }
        return emptyInts;
    }

    private static String makeKeyStringFromBytes(byte[] keyBytes, int keyOffset) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            byte b = keyBytes[keyOffset];
            if (b != 0) {
                keyOffset++;
                sb.append((char) b);
            } else {
                return sb.toString();
            }
        }
    }

    private String getKey16String(int keyOffset) {
        if (keyOffset < this.localKeyLimit) {
            return makeKeyStringFromBytes(this.keyBytes, keyOffset);
        }
        return makeKeyStringFromBytes(this.poolBundleReader.keyBytes, keyOffset - this.localKeyLimit);
    }

    private String getKey32String(int keyOffset) {
        if (keyOffset >= 0) {
            return makeKeyStringFromBytes(this.keyBytes, keyOffset);
        }
        return makeKeyStringFromBytes(this.poolBundleReader.keyBytes, Integer.MAX_VALUE & keyOffset);
    }

    private void setKeyFromKey16(int keyOffset, UResource.Key key) {
        if (keyOffset < this.localKeyLimit) {
            key.setBytes(this.keyBytes, keyOffset);
        } else {
            key.setBytes(this.poolBundleReader.keyBytes, keyOffset - this.localKeyLimit);
        }
    }

    private void setKeyFromKey32(int keyOffset, UResource.Key key) {
        if (keyOffset >= 0) {
            key.setBytes(this.keyBytes, keyOffset);
        } else {
            key.setBytes(this.poolBundleReader.keyBytes, Integer.MAX_VALUE & keyOffset);
        }
    }

    private int compareKeys(CharSequence key, char keyOffset) {
        if (keyOffset < this.localKeyLimit) {
            return ICUBinary.compareKeys(key, this.keyBytes, keyOffset);
        }
        return ICUBinary.compareKeys(key, this.poolBundleReader.keyBytes, keyOffset - this.localKeyLimit);
    }

    private int compareKeys32(CharSequence key, int keyOffset) {
        if (keyOffset >= 0) {
            return ICUBinary.compareKeys(key, this.keyBytes, keyOffset);
        }
        return ICUBinary.compareKeys(key, this.poolBundleReader.keyBytes, Integer.MAX_VALUE & keyOffset);
    }

    String getStringV2(int res) {
        int length;
        int offset;
        String s;
        if (!f5assertionsDisabled) {
            if (!(RES_GET_TYPE(res) == 6)) {
                throw new AssertionError();
            }
        }
        int offset2 = RES_GET_OFFSET(res);
        if (!f5assertionsDisabled) {
            if (!(offset2 != 0)) {
                throw new AssertionError();
            }
        }
        Object value = this.resourceCache.get(res);
        if (value != null) {
            return (String) value;
        }
        int first = this.b16BitUnits.charAt(offset2);
        if ((first & (-1024)) != 56320) {
            if (first == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append((char) first);
            while (true) {
                offset2++;
                char c = this.b16BitUnits.charAt(offset2);
                if (c == 0) {
                    break;
                }
                sb.append(c);
            }
            s = sb.toString();
        } else {
            if (first < 57327) {
                length = first & Opcodes.OP_NEW_INSTANCE_JUMBO;
                offset = offset2 + 1;
            } else if (first < 57343) {
                length = ((first - 57327) << 16) | this.b16BitUnits.charAt(offset2 + 1);
                offset = offset2 + 2;
            } else {
                length = (this.b16BitUnits.charAt(offset2 + 1) << 16) | this.b16BitUnits.charAt(offset2 + 2);
                offset = offset2 + 3;
            }
            s = this.b16BitUnits.subSequence(offset, offset + length).toString();
        }
        return (String) this.resourceCache.putIfAbsent(res, s, s.length() * 2);
    }

    private String makeStringFromBytes(int offset, int length) {
        if (length <= 16) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(this.bytes.getChar(offset));
                offset += 2;
            }
            return sb.toString();
        }
        CharSequence cs = this.bytes.asCharBuffer();
        int offset2 = offset / 2;
        return cs.subSequence(offset2, offset2 + length).toString();
    }

    String getString(int res) {
        int offset = RES_GET_OFFSET(res);
        if (res != offset && RES_GET_TYPE(res) != 6) {
            return null;
        }
        if (offset == 0) {
            return "";
        }
        if (res != offset) {
            if (offset < this.poolStringIndexLimit) {
                return this.poolBundleReader.getStringV2(res);
            }
            return getStringV2(res - this.poolStringIndexLimit);
        }
        Object value = this.resourceCache.get(res);
        if (value != null) {
            return (String) value;
        }
        int offset2 = getResourceByteOffset(offset);
        int length = getInt(offset2);
        String s = makeStringFromBytes(offset2 + 4, length);
        return (String) this.resourceCache.putIfAbsent(res, s, s.length() * 2);
    }

    private boolean isNoInheritanceMarker(int res) {
        int offset = RES_GET_OFFSET(res);
        if (offset != 0) {
            if (res == offset) {
                int offset2 = getResourceByteOffset(offset);
                return getInt(offset2) == 3 && this.bytes.getChar(offset2 + 4) == 8709 && this.bytes.getChar(offset2 + 6) == 8709 && this.bytes.getChar(offset2 + 8) == 8709;
            }
            if (RES_GET_TYPE(res) == 6) {
                if (offset < this.poolStringIndexLimit) {
                    return this.poolBundleReader.isStringV2NoInheritanceMarker(offset);
                }
                return isStringV2NoInheritanceMarker(offset - this.poolStringIndexLimit);
            }
        }
        return false;
    }

    private boolean isStringV2NoInheritanceMarker(int offset) {
        int first = this.b16BitUnits.charAt(offset);
        if (first == 8709) {
            if (this.b16BitUnits.charAt(offset + 1) == 8709 && this.b16BitUnits.charAt(offset + 2) == 8709) {
                return this.b16BitUnits.charAt(offset + 3) == 0;
            }
            return false;
        }
        if (first != 56323) {
            return false;
        }
        if (this.b16BitUnits.charAt(offset + 1) == 8709 && this.b16BitUnits.charAt(offset + 2) == 8709) {
            return this.b16BitUnits.charAt(offset + 3) == 8709;
        }
        return false;
    }

    String getAlias(int res) {
        int offset = RES_GET_OFFSET(res);
        if (RES_GET_TYPE(res) != 3) {
            return null;
        }
        if (offset == 0) {
            return "";
        }
        Object value = this.resourceCache.get(res);
        if (value != null) {
            return (String) value;
        }
        int offset2 = getResourceByteOffset(offset);
        int length = getInt(offset2);
        String s = makeStringFromBytes(offset2 + 4, length);
        return (String) this.resourceCache.putIfAbsent(res, s, length * 2);
    }

    byte[] getBinary(int res, byte[] ba) {
        int offset = RES_GET_OFFSET(res);
        if (RES_GET_TYPE(res) != 1) {
            return null;
        }
        if (offset == 0) {
            return emptyBytes;
        }
        int offset2 = getResourceByteOffset(offset);
        int length = getInt(offset2);
        if (length == 0) {
            return emptyBytes;
        }
        if (ba == null || ba.length != length) {
            ba = new byte[length];
        }
        int offset3 = offset2 + 4;
        if (length <= 16) {
            int i = 0;
            int offset4 = offset3;
            while (i < length) {
                ba[i] = this.bytes.get(offset4);
                i++;
                offset4++;
            }
        } else {
            ByteBuffer temp = this.bytes.duplicate();
            temp.position(offset3);
            temp.get(ba);
        }
        return ba;
    }

    ByteBuffer getBinary(int res) {
        int offset = RES_GET_OFFSET(res);
        if (RES_GET_TYPE(res) == 1) {
            if (offset == 0) {
                return emptyByteBuffer.duplicate();
            }
            int offset2 = getResourceByteOffset(offset);
            int length = getInt(offset2);
            if (length == 0) {
                return emptyByteBuffer.duplicate();
            }
            int offset3 = offset2 + 4;
            ByteBuffer result = this.bytes.duplicate();
            result.position(offset3).limit(offset3 + length);
            ByteBuffer result2 = ICUBinary.sliceWithOrder(result);
            if (!result2.isReadOnly()) {
                return result2.asReadOnlyBuffer();
            }
            return result2;
        }
        return null;
    }

    int[] getIntVector(int res) {
        int offset = RES_GET_OFFSET(res);
        if (RES_GET_TYPE(res) == 14) {
            if (offset == 0) {
                return emptyInts;
            }
            int offset2 = getResourceByteOffset(offset);
            int length = getInt(offset2);
            return getInts(offset2 + 4, length);
        }
        return null;
    }

    private int getArrayLength(int res) {
        int offset = RES_GET_OFFSET(res);
        if (offset == 0) {
            return 0;
        }
        int type = RES_GET_TYPE(res);
        if (type == 8) {
            return getInt(getResourceByteOffset(offset));
        }
        if (type == 9) {
            return this.b16BitUnits.charAt(offset);
        }
        return 0;
    }

    Array getArray(int res) {
        int type = RES_GET_TYPE(res);
        if (!URES_IS_ARRAY(type)) {
            return null;
        }
        int offset = RES_GET_OFFSET(res);
        if (offset == 0) {
            return EMPTY_ARRAY;
        }
        Object value = this.resourceCache.get(res);
        if (value != null) {
            return (Array) value;
        }
        Array array = type == 8 ? new Array32(this, offset) : new Array16(this, offset);
        return (Array) this.resourceCache.putIfAbsent(res, array, 0);
    }

    private int getTableLength(int res) {
        int offset = RES_GET_OFFSET(res);
        if (offset == 0) {
            return 0;
        }
        int type = RES_GET_TYPE(res);
        if (type == 2) {
            return this.bytes.getChar(getResourceByteOffset(offset));
        }
        if (type == 5) {
            return this.b16BitUnits.charAt(offset);
        }
        if (type == 4) {
            return getInt(getResourceByteOffset(offset));
        }
        return 0;
    }

    Table getTable(int res) {
        Table table;
        int size;
        int type = RES_GET_TYPE(res);
        if (!URES_IS_TABLE(type)) {
            return null;
        }
        int offset = RES_GET_OFFSET(res);
        if (offset == 0) {
            return EMPTY_TABLE;
        }
        Object value = this.resourceCache.get(res);
        if (value != null) {
            return (Table) value;
        }
        if (type == 2) {
            table = new Table1632(this, offset);
            size = table.getSize() * 2;
        } else if (type == 5) {
            table = new Table16(this, offset);
            size = table.getSize() * 2;
        } else {
            table = new Table32(this, offset);
            size = table.getSize() * 4;
        }
        return (Table) this.resourceCache.putIfAbsent(res, table, size);
    }

    static class ReaderValue extends UResource.Value {
        ICUResourceBundleReader reader;
        private int res;

        ReaderValue() {
        }

        @Override
        public int getType() {
            return ICUResourceBundleReader.PUBLIC_TYPES[ICUResourceBundleReader.RES_GET_TYPE(this.res)];
        }

        @Override
        public String getString() {
            String s = this.reader.getString(this.res);
            if (s == null) {
                throw new UResourceTypeMismatchException("");
            }
            return s;
        }

        @Override
        public String getAliasString() {
            String s = this.reader.getAlias(this.res);
            if (s == null) {
                throw new UResourceTypeMismatchException("");
            }
            return s;
        }

        @Override
        public int getInt() {
            if (ICUResourceBundleReader.RES_GET_TYPE(this.res) != 7) {
                throw new UResourceTypeMismatchException("");
            }
            return ICUResourceBundleReader.RES_GET_INT(this.res);
        }

        @Override
        public int getUInt() {
            if (ICUResourceBundleReader.RES_GET_TYPE(this.res) != 7) {
                throw new UResourceTypeMismatchException("");
            }
            return ICUResourceBundleReader.RES_GET_UINT(this.res);
        }

        @Override
        public int[] getIntVector() {
            int[] iv = this.reader.getIntVector(this.res);
            if (iv == null) {
                throw new UResourceTypeMismatchException("");
            }
            return iv;
        }

        @Override
        public ByteBuffer getBinary() {
            ByteBuffer bb = this.reader.getBinary(this.res);
            if (bb == null) {
                throw new UResourceTypeMismatchException("");
            }
            return bb;
        }
    }

    static class Container {
        protected int itemsOffset;
        protected int size;

        final int getSize() {
            return this.size;
        }

        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return -1;
        }

        protected int getContainer16Resource(ICUResourceBundleReader reader, int index) {
            if (index < 0 || this.size <= index) {
                return -1;
            }
            int res16 = reader.b16BitUnits.charAt(this.itemsOffset + index);
            if (res16 >= reader.poolStringIndex16Limit) {
                res16 = (res16 - reader.poolStringIndex16Limit) + reader.poolStringIndexLimit;
            }
            return 1610612736 | res16;
        }

        protected int getContainer32Resource(ICUResourceBundleReader reader, int index) {
            if (index < 0 || this.size <= index) {
                return -1;
            }
            return reader.getInt(this.itemsOffset + (index * 4));
        }

        int getResource(ICUResourceBundleReader reader, String resKey) {
            return getContainerResource(reader, Integer.parseInt(resKey));
        }

        Container() {
        }
    }

    static class Array extends Container {

        static final boolean f6assertionsDisabled;

        static {
            f6assertionsDisabled = !Array.class.desiredAssertionStatus();
        }

        Array() {
        }

        void getAllItems(ICUResourceBundleReader reader, UResource.Key key, ReaderValue value, UResource.ArraySink sink) {
            for (int i = 0; i < this.size; i++) {
                int res = getContainerResource(reader, i);
                int type = ICUResourceBundleReader.RES_GET_TYPE(res);
                if (ICUResourceBundleReader.URES_IS_ARRAY(type)) {
                    int numItems = reader.getArrayLength(res);
                    UResource.ArraySink subSink = sink.getOrCreateArraySink(i, numItems);
                    if (subSink != null) {
                        Array array = reader.getArray(res);
                        if (!f6assertionsDisabled) {
                            if (!(array.size == numItems)) {
                                throw new AssertionError();
                            }
                        }
                        array.getAllItems(reader, key, value, subSink);
                    } else {
                        continue;
                    }
                } else if (ICUResourceBundleReader.URES_IS_TABLE(type)) {
                    int numItems2 = reader.getTableLength(res);
                    UResource.TableSink subSink2 = sink.getOrCreateTableSink(i, numItems2);
                    if (subSink2 != null) {
                        Table table = reader.getTable(res);
                        if (!f6assertionsDisabled) {
                            if (!(table.size == numItems2)) {
                                throw new AssertionError();
                            }
                        }
                        table.getAllItems(reader, key, value, subSink2);
                    } else {
                        continue;
                    }
                } else {
                    value.res = res;
                    sink.put(i, value);
                }
            }
            sink.leave();
        }
    }

    private static final class Array32 extends Array {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer32Resource(reader, index);
        }

        Array32(ICUResourceBundleReader reader, int offset) {
            int offset2 = reader.getResourceByteOffset(offset);
            this.size = reader.getInt(offset2);
            this.itemsOffset = offset2 + 4;
        }
    }

    private static final class Array16 extends Array {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer16Resource(reader, index);
        }

        Array16(ICUResourceBundleReader reader, int offset) {
            this.size = reader.b16BitUnits.charAt(offset);
            this.itemsOffset = offset + 1;
        }
    }

    static class Table extends Container {

        static final boolean f9assertionsDisabled;
        private static final int URESDATA_ITEM_NOT_FOUND = -1;
        protected int[] key32Offsets;
        protected char[] keyOffsets;

        static {
            f9assertionsDisabled = !Table.class.desiredAssertionStatus();
        }

        String getKey(ICUResourceBundleReader reader, int index) {
            if (index < 0 || this.size <= index) {
                return null;
            }
            if (this.keyOffsets != null) {
                return reader.getKey16String(this.keyOffsets[index]);
            }
            return reader.getKey32String(this.key32Offsets[index]);
        }

        int findTableItem(ICUResourceBundleReader reader, CharSequence key) {
            int result;
            int start = 0;
            int limit = this.size;
            while (start < limit) {
                int mid = (start + limit) >>> 1;
                if (this.keyOffsets != null) {
                    result = reader.compareKeys(key, this.keyOffsets[mid]);
                } else {
                    result = reader.compareKeys32(key, this.key32Offsets[mid]);
                }
                if (result < 0) {
                    limit = mid;
                } else if (result > 0) {
                    start = mid + 1;
                } else {
                    return mid;
                }
            }
            return -1;
        }

        @Override
        int getResource(ICUResourceBundleReader reader, String resKey) {
            return getContainerResource(reader, findTableItem(reader, resKey));
        }

        void getAllItems(ICUResourceBundleReader reader, UResource.Key key, ReaderValue value, UResource.TableSink sink) {
            for (int i = 0; i < this.size; i++) {
                if (this.keyOffsets != null) {
                    reader.setKeyFromKey16(this.keyOffsets[i], key);
                } else {
                    reader.setKeyFromKey32(this.key32Offsets[i], key);
                }
                int res = getContainerResource(reader, i);
                int type = ICUResourceBundleReader.RES_GET_TYPE(res);
                if (ICUResourceBundleReader.URES_IS_ARRAY(type)) {
                    int numItems = reader.getArrayLength(res);
                    UResource.ArraySink subSink = sink.getOrCreateArraySink(key, numItems);
                    if (subSink != null) {
                        Array array = reader.getArray(res);
                        if (!f9assertionsDisabled) {
                            if (!(array.size == numItems)) {
                                throw new AssertionError();
                            }
                        }
                        array.getAllItems(reader, key, value, subSink);
                    } else {
                        continue;
                    }
                } else if (ICUResourceBundleReader.URES_IS_TABLE(type)) {
                    int numItems2 = reader.getTableLength(res);
                    UResource.TableSink subSink2 = sink.getOrCreateTableSink(key, numItems2);
                    if (subSink2 != null) {
                        Table table = reader.getTable(res);
                        if (!f9assertionsDisabled) {
                            if (!(table.size == numItems2)) {
                                throw new AssertionError();
                            }
                        }
                        table.getAllItems(reader, key, value, subSink2);
                    } else {
                        continue;
                    }
                } else if (reader.isNoInheritanceMarker(res)) {
                    sink.putNoFallback(key);
                } else {
                    value.res = res;
                    sink.put(key, value);
                }
            }
            sink.leave();
        }

        Table() {
        }
    }

    private static final class Table1632 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer32Resource(reader, index);
        }

        Table1632(ICUResourceBundleReader reader, int offset) {
            int offset2 = reader.getResourceByteOffset(offset);
            this.keyOffsets = reader.getTableKeyOffsets(offset2);
            this.size = this.keyOffsets.length;
            this.itemsOffset = (((this.size + 2) & (-2)) * 2) + offset2;
        }
    }

    private static final class Table16 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer16Resource(reader, index);
        }

        Table16(ICUResourceBundleReader reader, int offset) {
            this.keyOffsets = reader.getTable16KeyOffsets(offset);
            this.size = this.keyOffsets.length;
            this.itemsOffset = offset + 1 + this.size;
        }
    }

    private static final class Table32 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer32Resource(reader, index);
        }

        Table32(ICUResourceBundleReader reader, int offset) {
            int offset2 = reader.getResourceByteOffset(offset);
            this.key32Offsets = reader.getTable32KeyOffsets(offset2);
            this.size = this.key32Offsets.length;
            this.itemsOffset = ((this.size + 1) * 4) + offset2;
        }
    }

    private static final class ResourceCache {

        static final boolean f7assertionsDisabled;
        private static final int NEXT_BITS = 6;
        private static final int ROOT_BITS = 7;
        private static final int SIMPLE_LENGTH = 32;
        private int length;
        private int levelBitsList;
        private int maxOffsetBits;
        private Level rootLevel;
        private int[] keys = new int[32];
        private Object[] values = new Object[32];

        static {
            f7assertionsDisabled = !ResourceCache.class.desiredAssertionStatus();
        }

        private static boolean storeDirectly(int size) {
            if (size >= 24) {
                return CacheValue.futureInstancesWillBeStrong();
            }
            return true;
        }

        private static final Object putIfCleared(Object[] values, int index, Object item, int size) {
            Object value = values[index];
            if (!(value instanceof SoftReference)) {
                return value;
            }
            if (!f7assertionsDisabled) {
                if (!(size >= 24)) {
                    throw new AssertionError();
                }
            }
            Object value2 = ((SoftReference) value).get();
            if (value2 != null) {
                return value2;
            }
            values[index] = CacheValue.futureInstancesWillBeStrong() ? item : new SoftReference(item);
            return item;
        }

        private static final class Level {

            static final boolean f8assertionsDisabled;
            int[] keys;
            int levelBitsList;
            int mask;
            int shift;
            Object[] values;

            static {
                f8assertionsDisabled = !Level.class.desiredAssertionStatus();
            }

            Level(int levelBitsList, int shift) {
                this.levelBitsList = levelBitsList;
                this.shift = shift;
                int bits = levelBitsList & 15;
                if (!f8assertionsDisabled) {
                    if (!(bits != 0)) {
                        throw new AssertionError();
                    }
                }
                int length = 1 << bits;
                this.mask = length - 1;
                this.keys = new int[length];
                this.values = new Object[length];
            }

            Object get(int key) {
                Level level;
                int index = (key >> this.shift) & this.mask;
                int k = this.keys[index];
                if (k == key) {
                    return this.values[index];
                }
                if (k != 0 || (level = (Level) this.values[index]) == null) {
                    return null;
                }
                return level.get(key);
            }

            Object putIfAbsent(int key, Object item, int size) {
                int index = (key >> this.shift) & this.mask;
                int k = this.keys[index];
                if (k == key) {
                    return ResourceCache.putIfCleared(this.values, index, item, size);
                }
                if (k == 0) {
                    Level level = (Level) this.values[index];
                    if (level != null) {
                        return level.putIfAbsent(key, item, size);
                    }
                    this.keys[index] = key;
                    this.values[index] = ResourceCache.storeDirectly(size) ? item : new SoftReference(item);
                    return item;
                }
                Level level2 = new Level(this.levelBitsList >> 4, this.shift + (this.levelBitsList & 15));
                int i = (k >> level2.shift) & level2.mask;
                level2.keys[i] = k;
                level2.values[i] = this.values[index];
                this.keys[index] = 0;
                this.values[index] = level2;
                return level2.putIfAbsent(key, item, size);
            }
        }

        ResourceCache(int maxOffset) {
            if (!f7assertionsDisabled) {
                if (!(maxOffset != 0)) {
                    throw new AssertionError();
                }
            }
            this.maxOffsetBits = 28;
            while (maxOffset <= 134217727) {
                maxOffset <<= 1;
                this.maxOffsetBits--;
            }
            int keyBits = this.maxOffsetBits + 2;
            if (keyBits <= 7) {
                this.levelBitsList = keyBits;
                return;
            }
            if (keyBits < 10) {
                this.levelBitsList = (keyBits - 3) | 48;
                return;
            }
            this.levelBitsList = 7;
            int keyBits2 = keyBits - 7;
            int shift = 4;
            while (keyBits2 > 6) {
                if (keyBits2 < 9) {
                    this.levelBitsList |= ((keyBits2 - 3) | 48) << shift;
                    return;
                } else {
                    this.levelBitsList |= 6 << shift;
                    keyBits2 -= 6;
                    shift += 4;
                }
            }
            this.levelBitsList |= keyBits2 << shift;
        }

        private int makeKey(int res) {
            int miniType;
            int type = ICUResourceBundleReader.RES_GET_TYPE(res);
            if (type == 6) {
                miniType = 1;
            } else if (type == 5) {
                miniType = 3;
            } else {
                miniType = type == 9 ? 2 : 0;
            }
            return ICUResourceBundleReader.RES_GET_OFFSET(res) | (miniType << this.maxOffsetBits);
        }

        private int findSimple(int key) {
            int start = 0;
            int limit = this.length;
            while (limit - start > 8) {
                int mid = (start + limit) / 2;
                if (key < this.keys[mid]) {
                    limit = mid;
                } else {
                    start = mid;
                }
            }
            while (start < limit) {
                int k = this.keys[start];
                if (key < k) {
                    return ~start;
                }
                if (key == k) {
                    return start;
                }
                start++;
            }
            return ~start;
        }

        synchronized Object get(int res) {
            Object value;
            synchronized (this) {
                if (!f7assertionsDisabled) {
                    if (!(ICUResourceBundleReader.RES_GET_OFFSET(res) != 0)) {
                        throw new AssertionError();
                    }
                }
                if (this.length >= 0) {
                    int index = findSimple(res);
                    if (index < 0) {
                        return null;
                    }
                    value = this.values[index];
                } else {
                    value = this.rootLevel.get(makeKey(res));
                    if (value == null) {
                        return null;
                    }
                }
                if (value instanceof SoftReference) {
                    value = ((SoftReference) value).get();
                }
                return value;
            }
        }

        synchronized Object putIfAbsent(int res, Object item, int size) {
            if (this.length >= 0) {
                int index = findSimple(res);
                if (index >= 0) {
                    return putIfCleared(this.values, index, item, size);
                }
                if (this.length < 32) {
                    int index2 = ~index;
                    if (index2 < this.length) {
                        System.arraycopy(this.keys, index2, this.keys, index2 + 1, this.length - index2);
                        System.arraycopy(this.values, index2, this.values, index2 + 1, this.length - index2);
                    }
                    this.length++;
                    this.keys[index2] = res;
                    this.values[index2] = storeDirectly(size) ? item : new SoftReference(item);
                    return item;
                }
                this.rootLevel = new Level(this.levelBitsList, 0);
                for (int i = 0; i < 32; i++) {
                    this.rootLevel.putIfAbsent(makeKey(this.keys[i]), this.values[i], 0);
                }
                this.keys = null;
                this.values = null;
                this.length = -1;
            }
            return this.rootLevel.putIfAbsent(makeKey(res), item, size);
        }
    }

    public static String getFullName(String baseName, String localeName) {
        if (baseName == null || baseName.length() == 0) {
            if (localeName.length() == 0) {
                return ULocale.getDefault().toString();
            }
            return localeName + ICU_RESOURCE_SUFFIX;
        }
        if (baseName.indexOf(46) == -1) {
            if (baseName.charAt(baseName.length() - 1) != '/') {
                return baseName + "/" + localeName + ICU_RESOURCE_SUFFIX;
            }
            return baseName + localeName + ICU_RESOURCE_SUFFIX;
        }
        String baseName2 = baseName.replace('.', '/');
        if (localeName.length() == 0) {
            return baseName2 + ICU_RESOURCE_SUFFIX;
        }
        return baseName2 + BaseLocale.SEP + localeName + ICU_RESOURCE_SUFFIX;
    }
}
