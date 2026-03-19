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
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;

public final class ICUResourceBundleReader {
    static final boolean $assertionsDisabled = false;
    private static ReaderCache CACHE = null;
    private static final int DATA_FORMAT = 1382380354;
    private static final boolean DEBUG = false;
    private static final String ICU_RESOURCE_SUFFIX = ".res";
    private static final IsAcceptable IS_ACCEPTABLE;
    static final int LARGE_SIZE = 24;
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
    private static final CharBuffer EMPTY_16_BIT_UNITS = CharBuffer.wrap("\u0000");
    private static final ICUResourceBundleReader NULL_READER = new ICUResourceBundleReader();
    private static final byte[] emptyBytes = new byte[0];
    private static final ByteBuffer emptyByteBuffer = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static final char[] emptyChars = new char[0];
    private static final int[] emptyInts = new int[0];
    private static final Array EMPTY_ARRAY = new Array();
    private static final Table EMPTY_TABLE = new Table();
    private static int[] PUBLIC_TYPES = {0, 1, 2, 3, 2, 2, 0, 7, 8, 8, -1, -1, -1, -1, 14, -1};

    static {
        IS_ACCEPTABLE = new IsAcceptable();
        CACHE = new ReaderCache();
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return (bArr[0] == 1 && (bArr[1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) >= 1) || (2 <= bArr[0] && bArr[0] <= 3);
        }
    }

    private static class ReaderCacheKey {
        final String baseName;
        final String localeID;

        ReaderCacheKey(String str, String str2) {
            this.baseName = str == null ? "" : str;
            this.localeID = str2 == null ? "" : str2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReaderCacheKey)) {
                return false;
            }
            ReaderCacheKey readerCacheKey = (ReaderCacheKey) obj;
            return this.baseName.equals(readerCacheKey.baseName) && this.localeID.equals(readerCacheKey.localeID);
        }

        public int hashCode() {
            return this.baseName.hashCode() ^ this.localeID.hashCode();
        }
    }

    private static class ReaderCache extends SoftCache<ReaderCacheKey, ICUResourceBundleReader, ClassLoader> {
        private ReaderCache() {
        }

        @Override
        protected ICUResourceBundleReader createInstance(ReaderCacheKey readerCacheKey, ClassLoader classLoader) {
            ByteBuffer byteBufferFromInputStreamAndCloseStream;
            String fullName = ICUResourceBundleReader.getFullName(readerCacheKey.baseName, readerCacheKey.localeID);
            try {
                if (readerCacheKey.baseName != null && readerCacheKey.baseName.startsWith(ICUData.ICU_BASE_NAME)) {
                    byteBufferFromInputStreamAndCloseStream = ICUBinary.getData(classLoader, fullName, fullName.substring(ICUData.ICU_BASE_NAME.length() + 1));
                    if (byteBufferFromInputStreamAndCloseStream == null) {
                        return ICUResourceBundleReader.NULL_READER;
                    }
                } else {
                    InputStream stream = ICUData.getStream(classLoader, fullName);
                    if (stream == null) {
                        return ICUResourceBundleReader.NULL_READER;
                    }
                    byteBufferFromInputStreamAndCloseStream = ICUBinary.getByteBufferFromInputStreamAndCloseStream(stream);
                }
                return new ICUResourceBundleReader(byteBufferFromInputStreamAndCloseStream, readerCacheKey.baseName, readerCacheKey.localeID, classLoader);
            } catch (IOException e) {
                throw new ICUUncheckedIOException("Data file " + fullName + " is corrupt - " + e.getMessage(), e);
            }
        }
    }

    private ICUResourceBundleReader() {
    }

    private ICUResourceBundleReader(ByteBuffer byteBuffer, String str, String str2, ClassLoader classLoader) throws IOException {
        init(byteBuffer);
        if (this.usesPoolBundle) {
            this.poolBundleReader = getReader(str, "pool", classLoader);
            if (this.poolBundleReader == null || !this.poolBundleReader.isPoolBundle) {
                throw new IllegalStateException("pool.res is not a pool bundle");
            }
            if (this.poolBundleReader.poolCheckSum != this.poolCheckSum) {
                throw new IllegalStateException("pool.res has a different checksum than this bundle");
            }
        }
    }

    static ICUResourceBundleReader getReader(String str, String str2, ClassLoader classLoader) {
        ICUResourceBundleReader readerCache = CACHE.getInstance(new ReaderCacheKey(str, str2), classLoader);
        if (readerCache == NULL_READER) {
            return null;
        }
        return readerCache;
    }

    private void init(ByteBuffer byteBuffer) throws IOException {
        int indexesInt;
        this.dataVersion = ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
        byte b = byteBuffer.get(16);
        this.bytes = ICUBinary.sliceWithOrder(byteBuffer);
        int iRemaining = this.bytes.remaining();
        this.rootRes = this.bytes.getInt(0);
        int indexesInt2 = getIndexesInt(0);
        int i = indexesInt2 & 255;
        if (i <= 4) {
            throw new ICUException("not enough indexes");
        }
        int i2 = 1 + i;
        int i3 = i2 << 2;
        if (iRemaining >= i3) {
            int indexesInt3 = getIndexesInt(3);
            if (iRemaining >= (indexesInt3 << 2)) {
                int i4 = indexesInt3 - 1;
                if (b >= 3) {
                    this.poolStringIndexLimit = indexesInt2 >>> 8;
                }
                if (i > 5) {
                    int indexesInt4 = getIndexesInt(5);
                    this.noFallback = (indexesInt4 & 1) != 0;
                    this.isPoolBundle = (indexesInt4 & 2) != 0;
                    this.usesPoolBundle = (indexesInt4 & 4) != 0;
                    this.poolStringIndexLimit |= (61440 & indexesInt4) << 12;
                    this.poolStringIndex16Limit = indexesInt4 >>> 16;
                }
                int indexesInt5 = getIndexesInt(1);
                if (indexesInt5 > i2) {
                    if (this.isPoolBundle) {
                        this.keyBytes = new byte[(indexesInt5 - i2) << 2];
                        this.bytes.position(i3);
                    } else {
                        this.localKeyLimit = indexesInt5 << 2;
                        this.keyBytes = new byte[this.localKeyLimit];
                    }
                    this.bytes.get(this.keyBytes);
                }
                if (i > 6 && (indexesInt = getIndexesInt(6)) > indexesInt5) {
                    int i5 = (indexesInt - indexesInt5) * 2;
                    this.bytes.position(indexesInt5 << 2);
                    this.b16BitUnits = this.bytes.asCharBuffer();
                    this.b16BitUnits.limit(i5);
                    i4 |= i5 - 1;
                } else {
                    this.b16BitUnits = EMPTY_16_BIT_UNITS;
                }
                if (i > 7) {
                    this.poolCheckSum = getIndexesInt(7);
                }
                if (!this.isPoolBundle || this.b16BitUnits.length() > 1) {
                    this.resourceCache = new ResourceCache(i4);
                }
                this.bytes.position(0);
                return;
            }
        }
        throw new ICUException("not enough bytes");
    }

    private int getIndexesInt(int i) {
        return this.bytes.getInt((1 + i) << 2);
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

    static int RES_GET_TYPE(int i) {
        return i >>> 28;
    }

    private static int RES_GET_OFFSET(int i) {
        return i & 268435455;
    }

    private int getResourceByteOffset(int i) {
        return i << 2;
    }

    static int RES_GET_INT(int i) {
        return (i << 4) >> 4;
    }

    static int RES_GET_UINT(int i) {
        return i & 268435455;
    }

    static boolean URES_IS_ARRAY(int i) {
        return i == 8 || i == 9;
    }

    static boolean URES_IS_TABLE(int i) {
        return i == 2 || i == 5 || i == 4;
    }

    private char[] getChars(int i, int i2) {
        char[] cArr = new char[i2];
        if (i2 <= 16) {
            for (int i3 = 0; i3 < i2; i3++) {
                cArr[i3] = this.bytes.getChar(i);
                i += 2;
            }
        } else {
            CharBuffer charBufferAsCharBuffer = this.bytes.asCharBuffer();
            charBufferAsCharBuffer.position(i / 2);
            charBufferAsCharBuffer.get(cArr);
        }
        return cArr;
    }

    private int getInt(int i) {
        return this.bytes.getInt(i);
    }

    private int[] getInts(int i, int i2) {
        int[] iArr = new int[i2];
        if (i2 <= 16) {
            for (int i3 = 0; i3 < i2; i3++) {
                iArr[i3] = this.bytes.getInt(i);
                i += 4;
            }
        } else {
            IntBuffer intBufferAsIntBuffer = this.bytes.asIntBuffer();
            intBufferAsIntBuffer.position(i / 4);
            intBufferAsIntBuffer.get(iArr);
        }
        return iArr;
    }

    private char[] getTable16KeyOffsets(int i) {
        int i2 = i + 1;
        int iCharAt = this.b16BitUnits.charAt(i);
        if (iCharAt > 0) {
            char[] cArr = new char[iCharAt];
            if (iCharAt <= 16) {
                int i3 = 0;
                while (i3 < iCharAt) {
                    cArr[i3] = this.b16BitUnits.charAt(i2);
                    i3++;
                    i2++;
                }
            } else {
                CharBuffer charBufferDuplicate = this.b16BitUnits.duplicate();
                charBufferDuplicate.position(i2);
                charBufferDuplicate.get(cArr);
            }
            return cArr;
        }
        return emptyChars;
    }

    private char[] getTableKeyOffsets(int i) {
        char c = this.bytes.getChar(i);
        if (c > 0) {
            return getChars(i + 2, c);
        }
        return emptyChars;
    }

    private int[] getTable32KeyOffsets(int i) {
        int i2 = getInt(i);
        if (i2 > 0) {
            return getInts(i + 4, i2);
        }
        return emptyInts;
    }

    private static String makeKeyStringFromBytes(byte[] bArr, int i) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            byte b = bArr[i];
            if (b != 0) {
                i++;
                sb.append((char) b);
            } else {
                return sb.toString();
            }
        }
    }

    private String getKey16String(int i) {
        if (i < this.localKeyLimit) {
            return makeKeyStringFromBytes(this.keyBytes, i);
        }
        return makeKeyStringFromBytes(this.poolBundleReader.keyBytes, i - this.localKeyLimit);
    }

    private String getKey32String(int i) {
        if (i >= 0) {
            return makeKeyStringFromBytes(this.keyBytes, i);
        }
        return makeKeyStringFromBytes(this.poolBundleReader.keyBytes, i & Integer.MAX_VALUE);
    }

    private void setKeyFromKey16(int i, UResource.Key key) {
        if (i < this.localKeyLimit) {
            key.setBytes(this.keyBytes, i);
        } else {
            key.setBytes(this.poolBundleReader.keyBytes, i - this.localKeyLimit);
        }
    }

    private void setKeyFromKey32(int i, UResource.Key key) {
        if (i >= 0) {
            key.setBytes(this.keyBytes, i);
        } else {
            key.setBytes(this.poolBundleReader.keyBytes, i & Integer.MAX_VALUE);
        }
    }

    private int compareKeys(CharSequence charSequence, char c) {
        if (c < this.localKeyLimit) {
            return ICUBinary.compareKeys(charSequence, this.keyBytes, c);
        }
        return ICUBinary.compareKeys(charSequence, this.poolBundleReader.keyBytes, c - this.localKeyLimit);
    }

    private int compareKeys32(CharSequence charSequence, int i) {
        if (i >= 0) {
            return ICUBinary.compareKeys(charSequence, this.keyBytes, i);
        }
        return ICUBinary.compareKeys(charSequence, this.poolBundleReader.keyBytes, i & Integer.MAX_VALUE);
    }

    String getStringV2(int i) {
        int iCharAt;
        int i2;
        String string;
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        Object obj = this.resourceCache.get(i);
        if (obj != null) {
            return (String) obj;
        }
        char cCharAt = this.b16BitUnits.charAt(iRES_GET_OFFSET);
        if ((cCharAt & 64512) != 56320) {
            if (cCharAt == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(cCharAt);
            while (true) {
                iRES_GET_OFFSET++;
                char cCharAt2 = this.b16BitUnits.charAt(iRES_GET_OFFSET);
                if (cCharAt2 == 0) {
                    break;
                }
                sb.append(cCharAt2);
            }
            string = sb.toString();
        } else {
            if (cCharAt < 57327) {
                iCharAt = cCharAt & 1023;
                i2 = iRES_GET_OFFSET + 1;
            } else if (cCharAt < 57343) {
                iCharAt = ((cCharAt - 57327) << 16) | this.b16BitUnits.charAt(iRES_GET_OFFSET + 1);
                i2 = iRES_GET_OFFSET + 2;
            } else {
                iCharAt = (this.b16BitUnits.charAt(iRES_GET_OFFSET + 1) << 16) | this.b16BitUnits.charAt(iRES_GET_OFFSET + 2);
                i2 = iRES_GET_OFFSET + 3;
            }
            string = this.b16BitUnits.subSequence(i2, iCharAt + i2).toString();
        }
        return (String) this.resourceCache.putIfAbsent(i, string, string.length() * 2);
    }

    private String makeStringFromBytes(int i, int i2) {
        if (i2 <= 16) {
            StringBuilder sb = new StringBuilder(i2);
            for (int i3 = 0; i3 < i2; i3++) {
                sb.append(this.bytes.getChar(i));
                i += 2;
            }
            return sb.toString();
        }
        int i4 = i / 2;
        return this.bytes.asCharBuffer().subSequence(i4, i2 + i4).toString();
    }

    String getString(int i) {
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (i != iRES_GET_OFFSET && RES_GET_TYPE(i) != 6) {
            return null;
        }
        if (iRES_GET_OFFSET == 0) {
            return "";
        }
        if (i != iRES_GET_OFFSET) {
            if (iRES_GET_OFFSET < this.poolStringIndexLimit) {
                return this.poolBundleReader.getStringV2(i);
            }
            return getStringV2(i - this.poolStringIndexLimit);
        }
        Object obj = this.resourceCache.get(i);
        if (obj != null) {
            return (String) obj;
        }
        int resourceByteOffset = getResourceByteOffset(iRES_GET_OFFSET);
        String strMakeStringFromBytes = makeStringFromBytes(resourceByteOffset + 4, getInt(resourceByteOffset));
        return (String) this.resourceCache.putIfAbsent(i, strMakeStringFromBytes, strMakeStringFromBytes.length() * 2);
    }

    private boolean isNoInheritanceMarker(int i) {
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (iRES_GET_OFFSET != 0) {
            if (i == iRES_GET_OFFSET) {
                int resourceByteOffset = getResourceByteOffset(iRES_GET_OFFSET);
                return getInt(resourceByteOffset) == 3 && this.bytes.getChar(resourceByteOffset + 4) == 8709 && this.bytes.getChar(resourceByteOffset + 6) == 8709 && this.bytes.getChar(resourceByteOffset + 8) == 8709;
            }
            if (RES_GET_TYPE(i) == 6) {
                if (iRES_GET_OFFSET < this.poolStringIndexLimit) {
                    return this.poolBundleReader.isStringV2NoInheritanceMarker(iRES_GET_OFFSET);
                }
                return isStringV2NoInheritanceMarker(iRES_GET_OFFSET - this.poolStringIndexLimit);
            }
        }
        return false;
    }

    private boolean isStringV2NoInheritanceMarker(int i) {
        char cCharAt = this.b16BitUnits.charAt(i);
        return cCharAt == 8709 ? this.b16BitUnits.charAt(i + 1) == 8709 && this.b16BitUnits.charAt(i + 2) == 8709 && this.b16BitUnits.charAt(i + 3) == 0 : cCharAt == 56323 && this.b16BitUnits.charAt(i + 1) == 8709 && this.b16BitUnits.charAt(i + 2) == 8709 && this.b16BitUnits.charAt(i + 3) == 8709;
    }

    String getAlias(int i) {
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (RES_GET_TYPE(i) == 3) {
            if (iRES_GET_OFFSET == 0) {
                return "";
            }
            Object obj = this.resourceCache.get(i);
            if (obj != null) {
                return (String) obj;
            }
            int resourceByteOffset = getResourceByteOffset(iRES_GET_OFFSET);
            int i2 = getInt(resourceByteOffset);
            return (String) this.resourceCache.putIfAbsent(i, makeStringFromBytes(resourceByteOffset + 4, i2), i2 * 2);
        }
        return null;
    }

    byte[] getBinary(int i, byte[] bArr) {
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (RES_GET_TYPE(i) == 1) {
            if (iRES_GET_OFFSET == 0) {
                return emptyBytes;
            }
            int resourceByteOffset = getResourceByteOffset(iRES_GET_OFFSET);
            int i2 = getInt(resourceByteOffset);
            if (i2 == 0) {
                return emptyBytes;
            }
            if (bArr == null || bArr.length != i2) {
                bArr = new byte[i2];
            }
            int i3 = resourceByteOffset + 4;
            if (i2 <= 16) {
                int i4 = 0;
                while (i4 < i2) {
                    bArr[i4] = this.bytes.get(i3);
                    i4++;
                    i3++;
                }
            } else {
                ByteBuffer byteBufferDuplicate = this.bytes.duplicate();
                byteBufferDuplicate.position(i3);
                byteBufferDuplicate.get(bArr);
            }
            return bArr;
        }
        return null;
    }

    ByteBuffer getBinary(int i) {
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (RES_GET_TYPE(i) == 1) {
            if (iRES_GET_OFFSET == 0) {
                return emptyByteBuffer.duplicate();
            }
            int resourceByteOffset = getResourceByteOffset(iRES_GET_OFFSET);
            int i2 = getInt(resourceByteOffset);
            if (i2 == 0) {
                return emptyByteBuffer.duplicate();
            }
            int i3 = resourceByteOffset + 4;
            ByteBuffer byteBufferDuplicate = this.bytes.duplicate();
            byteBufferDuplicate.position(i3).limit(i3 + i2);
            ByteBuffer byteBufferSliceWithOrder = ICUBinary.sliceWithOrder(byteBufferDuplicate);
            if (!byteBufferSliceWithOrder.isReadOnly()) {
                return byteBufferSliceWithOrder.asReadOnlyBuffer();
            }
            return byteBufferSliceWithOrder;
        }
        return null;
    }

    int[] getIntVector(int i) {
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (RES_GET_TYPE(i) == 14) {
            if (iRES_GET_OFFSET == 0) {
                return emptyInts;
            }
            int resourceByteOffset = getResourceByteOffset(iRES_GET_OFFSET);
            return getInts(resourceByteOffset + 4, getInt(resourceByteOffset));
        }
        return null;
    }

    Array getArray(int i) {
        int iRES_GET_TYPE = RES_GET_TYPE(i);
        if (!URES_IS_ARRAY(iRES_GET_TYPE)) {
            return null;
        }
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (iRES_GET_OFFSET == 0) {
            return EMPTY_ARRAY;
        }
        Object obj = this.resourceCache.get(i);
        if (obj != null) {
            return (Array) obj;
        }
        return (Array) this.resourceCache.putIfAbsent(i, iRES_GET_TYPE == 8 ? new Array32(this, iRES_GET_OFFSET) : new Array16(this, iRES_GET_OFFSET), 0);
    }

    Table getTable(int i) {
        Container table32;
        int size;
        int iRES_GET_TYPE = RES_GET_TYPE(i);
        if (!URES_IS_TABLE(iRES_GET_TYPE)) {
            return null;
        }
        int iRES_GET_OFFSET = RES_GET_OFFSET(i);
        if (iRES_GET_OFFSET == 0) {
            return EMPTY_TABLE;
        }
        Object obj = this.resourceCache.get(i);
        if (obj != null) {
            return (Table) obj;
        }
        if (iRES_GET_TYPE == 2) {
            table32 = new Table1632(this, iRES_GET_OFFSET);
            size = table32.getSize() * 2;
        } else if (iRES_GET_TYPE == 5) {
            table32 = new Table16(this, iRES_GET_OFFSET);
            size = table32.getSize() * 2;
        } else {
            table32 = new Table32(this, iRES_GET_OFFSET);
            size = table32.getSize() * 4;
        }
        return (Table) this.resourceCache.putIfAbsent(i, table32, size);
    }

    static class ReaderValue extends UResource.Value {
        ICUResourceBundleReader reader;
        int res;

        ReaderValue() {
        }

        @Override
        public int getType() {
            return ICUResourceBundleReader.PUBLIC_TYPES[ICUResourceBundleReader.RES_GET_TYPE(this.res)];
        }

        @Override
        public String getString() {
            String string = this.reader.getString(this.res);
            if (string == null) {
                throw new UResourceTypeMismatchException("");
            }
            return string;
        }

        @Override
        public String getAliasString() {
            String alias = this.reader.getAlias(this.res);
            if (alias == null) {
                throw new UResourceTypeMismatchException("");
            }
            return alias;
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
            int[] intVector = this.reader.getIntVector(this.res);
            if (intVector == null) {
                throw new UResourceTypeMismatchException("");
            }
            return intVector;
        }

        @Override
        public ByteBuffer getBinary() {
            ByteBuffer binary = this.reader.getBinary(this.res);
            if (binary == null) {
                throw new UResourceTypeMismatchException("");
            }
            return binary;
        }

        @Override
        public UResource.Array getArray() {
            Array array = this.reader.getArray(this.res);
            if (array == null) {
                throw new UResourceTypeMismatchException("");
            }
            return array;
        }

        @Override
        public UResource.Table getTable() {
            Table table = this.reader.getTable(this.res);
            if (table == null) {
                throw new UResourceTypeMismatchException("");
            }
            return table;
        }

        @Override
        public boolean isNoInheritanceMarker() {
            return this.reader.isNoInheritanceMarker(this.res);
        }

        @Override
        public String[] getStringArray() {
            Array array = this.reader.getArray(this.res);
            if (array == null) {
                throw new UResourceTypeMismatchException("");
            }
            return getStringArray(array);
        }

        @Override
        public String[] getStringArrayOrStringAsArray() {
            Array array = this.reader.getArray(this.res);
            if (array != null) {
                return getStringArray(array);
            }
            String string = this.reader.getString(this.res);
            if (string != null) {
                return new String[]{string};
            }
            throw new UResourceTypeMismatchException("");
        }

        @Override
        public String getStringOrFirstOfArray() {
            String string = this.reader.getString(this.res);
            if (string != null) {
                return string;
            }
            Array array = this.reader.getArray(this.res);
            if (array != null && array.size > 0) {
                String string2 = this.reader.getString(array.getContainerResource(this.reader, 0));
                if (string2 != null) {
                    return string2;
                }
            }
            throw new UResourceTypeMismatchException("");
        }

        private String[] getStringArray(Array array) {
            String[] strArr = new String[array.size];
            for (int i = 0; i < array.size; i++) {
                String string = this.reader.getString(array.getContainerResource(this.reader, i));
                if (string == null) {
                    throw new UResourceTypeMismatchException("");
                }
                strArr[i] = string;
            }
            return strArr;
        }
    }

    static class Container {
        protected int itemsOffset;
        protected int size;

        public final int getSize() {
            return this.size;
        }

        int getContainerResource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            return -1;
        }

        protected int getContainer16Resource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            if (i >= 0 && this.size > i) {
                int iCharAt = iCUResourceBundleReader.b16BitUnits.charAt(this.itemsOffset + i);
                if (iCharAt >= iCUResourceBundleReader.poolStringIndex16Limit) {
                    iCharAt = (iCharAt - iCUResourceBundleReader.poolStringIndex16Limit) + iCUResourceBundleReader.poolStringIndexLimit;
                }
                return 1610612736 | iCharAt;
            }
            return -1;
        }

        protected int getContainer32Resource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            if (i >= 0 && this.size > i) {
                return iCUResourceBundleReader.getInt(this.itemsOffset + (4 * i));
            }
            return -1;
        }

        int getResource(ICUResourceBundleReader iCUResourceBundleReader, String str) {
            return getContainerResource(iCUResourceBundleReader, Integer.parseInt(str));
        }

        Container() {
        }
    }

    static class Array extends Container implements UResource.Array {
        Array() {
        }

        @Override
        public boolean getValue(int i, UResource.Value value) {
            if (i >= 0 && i < this.size) {
                ReaderValue readerValue = (ReaderValue) value;
                readerValue.res = getContainerResource(readerValue.reader, i);
                return true;
            }
            return false;
        }
    }

    private static final class Array32 extends Array {
        @Override
        int getContainerResource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            return getContainer32Resource(iCUResourceBundleReader, i);
        }

        Array32(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            int resourceByteOffset = iCUResourceBundleReader.getResourceByteOffset(i);
            this.size = iCUResourceBundleReader.getInt(resourceByteOffset);
            this.itemsOffset = resourceByteOffset + 4;
        }
    }

    private static final class Array16 extends Array {
        @Override
        int getContainerResource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            return getContainer16Resource(iCUResourceBundleReader, i);
        }

        Array16(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            this.size = iCUResourceBundleReader.b16BitUnits.charAt(i);
            this.itemsOffset = i + 1;
        }
    }

    static class Table extends Container implements UResource.Table {
        private static final int URESDATA_ITEM_NOT_FOUND = -1;
        protected int[] key32Offsets;
        protected char[] keyOffsets;

        Table() {
        }

        String getKey(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            if (i < 0 || this.size <= i) {
                return null;
            }
            return this.keyOffsets != null ? iCUResourceBundleReader.getKey16String(this.keyOffsets[i]) : iCUResourceBundleReader.getKey32String(this.key32Offsets[i]);
        }

        int findTableItem(ICUResourceBundleReader iCUResourceBundleReader, CharSequence charSequence) {
            int i = this.size;
            int i2 = 0;
            while (i2 < i) {
                int i3 = (i2 + i) >>> 1;
                int iCompareKeys = this.keyOffsets != null ? iCUResourceBundleReader.compareKeys(charSequence, this.keyOffsets[i3]) : iCUResourceBundleReader.compareKeys32(charSequence, this.key32Offsets[i3]);
                if (iCompareKeys < 0) {
                    i = i3;
                } else if (iCompareKeys > 0) {
                    i2 = i3 + 1;
                } else {
                    return i3;
                }
            }
            return -1;
        }

        @Override
        int getResource(ICUResourceBundleReader iCUResourceBundleReader, String str) {
            return getContainerResource(iCUResourceBundleReader, findTableItem(iCUResourceBundleReader, str));
        }

        @Override
        public boolean getKeyAndValue(int i, UResource.Key key, UResource.Value value) {
            if (i >= 0 && i < this.size) {
                ReaderValue readerValue = (ReaderValue) value;
                if (this.keyOffsets != null) {
                    readerValue.reader.setKeyFromKey16(this.keyOffsets[i], key);
                } else {
                    readerValue.reader.setKeyFromKey32(this.key32Offsets[i], key);
                }
                readerValue.res = getContainerResource(readerValue.reader, i);
                return true;
            }
            return false;
        }
    }

    private static final class Table1632 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            return getContainer32Resource(iCUResourceBundleReader, i);
        }

        Table1632(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            int resourceByteOffset = iCUResourceBundleReader.getResourceByteOffset(i);
            this.keyOffsets = iCUResourceBundleReader.getTableKeyOffsets(resourceByteOffset);
            this.size = this.keyOffsets.length;
            this.itemsOffset = resourceByteOffset + (2 * ((this.size + 2) & (-2)));
        }
    }

    private static final class Table16 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            return getContainer16Resource(iCUResourceBundleReader, i);
        }

        Table16(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            this.keyOffsets = iCUResourceBundleReader.getTable16KeyOffsets(i);
            this.size = this.keyOffsets.length;
            this.itemsOffset = i + 1 + this.size;
        }
    }

    private static final class Table32 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            return getContainer32Resource(iCUResourceBundleReader, i);
        }

        Table32(ICUResourceBundleReader iCUResourceBundleReader, int i) {
            int resourceByteOffset = iCUResourceBundleReader.getResourceByteOffset(i);
            this.key32Offsets = iCUResourceBundleReader.getTable32KeyOffsets(resourceByteOffset);
            this.size = this.key32Offsets.length;
            this.itemsOffset = resourceByteOffset + (4 * (1 + this.size));
        }
    }

    private static final class ResourceCache {
        static final boolean $assertionsDisabled = false;
        private static final int NEXT_BITS = 6;
        private static final int ROOT_BITS = 7;
        private static final int SIMPLE_LENGTH = 32;
        private int length;
        private int levelBitsList;
        private Level rootLevel;
        private int[] keys = new int[32];
        private Object[] values = new Object[32];
        private int maxOffsetBits = 28;

        private static boolean storeDirectly(int i) {
            return i < 24 || CacheValue.futureInstancesWillBeStrong();
        }

        private static final Object putIfCleared(Object[] objArr, int i, Object obj, int i2) {
            Object obj2 = objArr[i];
            if (!(obj2 instanceof SoftReference)) {
                return obj2;
            }
            Object obj3 = ((SoftReference) obj2).get();
            if (obj3 != null) {
                return obj3;
            }
            objArr[i] = CacheValue.futureInstancesWillBeStrong() ? obj : new SoftReference(obj);
            return obj;
        }

        private static final class Level {
            static final boolean $assertionsDisabled = false;
            int[] keys;
            int levelBitsList;
            int mask;
            int shift;
            Object[] values;

            Level(int i, int i2) {
                this.levelBitsList = i;
                this.shift = i2;
                int i3 = 1 << (i & 15);
                this.mask = i3 - 1;
                this.keys = new int[i3];
                this.values = new Object[i3];
            }

            Object get(int i) {
                Level level;
                int i2 = (i >> this.shift) & this.mask;
                int i3 = this.keys[i2];
                if (i3 == i) {
                    return this.values[i2];
                }
                if (i3 == 0 && (level = (Level) this.values[i2]) != null) {
                    return level.get(i);
                }
                return null;
            }

            Object putIfAbsent(int i, Object obj, int i2) {
                int i3 = (i >> this.shift) & this.mask;
                int i4 = this.keys[i3];
                if (i4 == i) {
                    return ResourceCache.putIfCleared(this.values, i3, obj, i2);
                }
                if (i4 == 0) {
                    Level level = (Level) this.values[i3];
                    if (level != null) {
                        return level.putIfAbsent(i, obj, i2);
                    }
                    this.keys[i3] = i;
                    this.values[i3] = ResourceCache.storeDirectly(i2) ? obj : new SoftReference(obj);
                    return obj;
                }
                Level level2 = new Level(this.levelBitsList >> 4, this.shift + (this.levelBitsList & 15));
                int i5 = (i4 >> level2.shift) & level2.mask;
                level2.keys[i5] = i4;
                level2.values[i5] = this.values[i3];
                this.keys[i3] = 0;
                this.values[i3] = level2;
                return level2.putIfAbsent(i, obj, i2);
            }
        }

        ResourceCache(int i) {
            while (i <= 134217727) {
                i <<= 1;
                this.maxOffsetBits--;
            }
            int i2 = this.maxOffsetBits + 2;
            if (i2 <= 7) {
                this.levelBitsList = i2;
                return;
            }
            if (i2 < 10) {
                this.levelBitsList = (i2 - 3) | 48;
                return;
            }
            this.levelBitsList = 7;
            int i3 = i2 - 7;
            int i4 = 4;
            while (i3 > 6) {
                if (i3 < 9) {
                    this.levelBitsList = (((i3 - 3) | 48) << i4) | this.levelBitsList;
                    return;
                } else {
                    this.levelBitsList = (6 << i4) | this.levelBitsList;
                    i3 -= 6;
                    i4 += 4;
                }
            }
            this.levelBitsList = (i3 << i4) | this.levelBitsList;
        }

        private int makeKey(int i) {
            int i2;
            int iRES_GET_TYPE = ICUResourceBundleReader.RES_GET_TYPE(i);
            if (iRES_GET_TYPE == 6) {
                i2 = 1;
            } else if (iRES_GET_TYPE == 5) {
                i2 = 3;
            } else {
                i2 = iRES_GET_TYPE == 9 ? 2 : 0;
            }
            return ICUResourceBundleReader.RES_GET_OFFSET(i) | (i2 << this.maxOffsetBits);
        }

        private int findSimple(int i) {
            int i2 = this.length;
            int i3 = 0;
            while (i2 - i3 > 8) {
                int i4 = (i3 + i2) / 2;
                if (i < this.keys[i4]) {
                    i2 = i4;
                } else {
                    i3 = i4;
                }
            }
            while (i3 < i2) {
                int i5 = this.keys[i3];
                if (i < i5) {
                    return ~i3;
                }
                if (i == i5) {
                    return i3;
                }
                i3++;
            }
            return ~i3;
        }

        synchronized Object get(int i) {
            Object obj;
            if (this.length >= 0) {
                int iFindSimple = findSimple(i);
                if (iFindSimple < 0) {
                    return null;
                }
                obj = this.values[iFindSimple];
            } else {
                obj = this.rootLevel.get(makeKey(i));
                if (obj == null) {
                    return null;
                }
            }
            if (obj instanceof SoftReference) {
                obj = ((SoftReference) obj).get();
            }
            return obj;
        }

        synchronized Object putIfAbsent(int i, Object obj, int i2) {
            if (this.length >= 0) {
                int iFindSimple = findSimple(i);
                if (iFindSimple >= 0) {
                    return putIfCleared(this.values, iFindSimple, obj, i2);
                }
                if (this.length < 32) {
                    int i3 = ~iFindSimple;
                    if (i3 < this.length) {
                        int i4 = i3 + 1;
                        System.arraycopy(this.keys, i3, this.keys, i4, this.length - i3);
                        System.arraycopy(this.values, i3, this.values, i4, this.length - i3);
                    }
                    this.length++;
                    this.keys[i3] = i;
                    this.values[i3] = storeDirectly(i2) ? obj : new SoftReference(obj);
                    return obj;
                }
                this.rootLevel = new Level(this.levelBitsList, 0);
                for (int i5 = 0; i5 < 32; i5++) {
                    this.rootLevel.putIfAbsent(makeKey(this.keys[i5]), this.values[i5], 0);
                }
                this.keys = null;
                this.values = null;
                this.length = -1;
            }
            return this.rootLevel.putIfAbsent(makeKey(i), obj, i2);
        }
    }

    public static String getFullName(String str, String str2) {
        if (str == null || str.length() == 0) {
            if (str2.length() == 0) {
                return ULocale.getDefault().toString();
            }
            return str2 + ICU_RESOURCE_SUFFIX;
        }
        if (str.indexOf(46) == -1) {
            if (str.charAt(str.length() - 1) != '/') {
                return str + "/" + str2 + ICU_RESOURCE_SUFFIX;
            }
            return str + str2 + ICU_RESOURCE_SUFFIX;
        }
        String strReplace = str.replace('.', '/');
        if (str2.length() == 0) {
            return strReplace + ICU_RESOURCE_SUFFIX;
        }
        return strReplace + BaseLocale.SEP + str2 + ICU_RESOURCE_SUFFIX;
    }
}
