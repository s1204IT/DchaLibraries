package android.icu.impl.coll;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2_32;
import android.icu.impl.USerializedSet;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUException;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

final class CollationDataReader {

    static final boolean f40assertionsDisabled;
    private static final int DATA_FORMAT = 1430482796;
    private static final IsAcceptable IS_ACCEPTABLE;
    static final int IX_CE32S_OFFSET = 11;
    static final int IX_CES_OFFSET = 9;
    static final int IX_COMPRESSIBLE_BYTES_OFFSET = 17;
    static final int IX_CONTEXTS_OFFSET = 13;
    static final int IX_FAST_LATIN_TABLE_OFFSET = 15;
    static final int IX_INDEXES_LENGTH = 0;
    static final int IX_JAMO_CE32S_START = 4;
    static final int IX_OPTIONS = 1;
    static final int IX_REORDER_CODES_OFFSET = 5;
    static final int IX_REORDER_TABLE_OFFSET = 6;
    static final int IX_RESERVED10_OFFSET = 10;
    static final int IX_RESERVED18_OFFSET = 18;
    static final int IX_RESERVED2 = 2;
    static final int IX_RESERVED3 = 3;
    static final int IX_RESERVED8_OFFSET = 8;
    static final int IX_ROOT_ELEMENTS_OFFSET = 12;
    static final int IX_SCRIPTS_OFFSET = 16;
    static final int IX_TOTAL_SIZE = 19;
    static final int IX_TRIE_OFFSET = 7;
    static final int IX_UNSAFE_BWD_OFFSET = 14;

    static void read(CollationTailoring base, ByteBuffer inBytes, CollationTailoring tailoring) throws IOException {
        int length;
        int[] reorderCodes;
        int reorderCodesLength;
        tailoring.version = ICUBinary.readHeader(inBytes, DATA_FORMAT, IS_ACCEPTABLE);
        if (base != null && base.getUCAVersion() != tailoring.getUCAVersion()) {
            throw new ICUException("Tailoring UCA version differs from base data UCA version");
        }
        int inLength = inBytes.remaining();
        if (inLength < 8) {
            throw new ICUException("not enough bytes");
        }
        int indexesLength = inBytes.getInt();
        if (indexesLength < 2 || inLength < indexesLength * 4) {
            throw new ICUException("not enough indexes");
        }
        int[] inIndexes = new int[20];
        inIndexes[0] = indexesLength;
        for (int i = 1; i < indexesLength; i++) {
            if (i >= inIndexes.length) {
                break;
            }
            inIndexes[i] = inBytes.getInt();
        }
        for (int i2 = indexesLength; i2 < inIndexes.length; i2++) {
            inIndexes[i2] = -1;
        }
        if (indexesLength > inIndexes.length) {
            ICUBinary.skipBytes(inBytes, (indexesLength - inIndexes.length) * 4);
        }
        if (indexesLength > 19) {
            length = inIndexes[19];
        } else if (indexesLength > 5) {
            length = inIndexes[indexesLength - 1];
        } else {
            length = 0;
        }
        if (inLength < length) {
            throw new ICUException("not enough bytes");
        }
        CollationData collationData = base == null ? null : base.data;
        int offset = inIndexes[5];
        int length2 = inIndexes[6] - offset;
        if (length2 >= 4) {
            if (collationData == null) {
                throw new ICUException("Collation base data must not reorder scripts");
            }
            int reorderCodesLength2 = length2 / 4;
            reorderCodes = ICUBinary.getInts(inBytes, reorderCodesLength2, length2 & 3);
            int reorderRangesLength = 0;
            while (reorderRangesLength < reorderCodesLength2 && (reorderCodes[(reorderCodesLength2 - reorderRangesLength) - 1] & (-65536)) != 0) {
                reorderRangesLength++;
            }
            if (!f40assertionsDisabled) {
                if (!(reorderRangesLength < reorderCodesLength2)) {
                    throw new AssertionError();
                }
            }
            reorderCodesLength = reorderCodesLength2 - reorderRangesLength;
        } else {
            reorderCodes = new int[0];
            reorderCodesLength = 0;
            ICUBinary.skipBytes(inBytes, length2);
        }
        byte[] reorderTable = null;
        int offset2 = inIndexes[6];
        int length3 = inIndexes[7] - offset2;
        if (length3 >= 256) {
            if (reorderCodesLength == 0) {
                throw new ICUException("Reordering table without reordering codes");
            }
            reorderTable = new byte[256];
            inBytes.get(reorderTable);
            length3 -= 256;
        }
        ICUBinary.skipBytes(inBytes, length3);
        if (collationData != null && collationData.numericPrimary != (((long) inIndexes[1]) & 4278190080L)) {
            throw new ICUException("Tailoring numeric primary weight differs from base data");
        }
        CollationData data = null;
        int offset3 = inIndexes[7];
        int length4 = inIndexes[8] - offset3;
        if (length4 >= 8) {
            tailoring.ensureOwnedData();
            data = tailoring.ownedData;
            data.base = collationData;
            data.numericPrimary = ((long) inIndexes[1]) & 4278190080L;
            Trie2_32 trie2_32CreateFromSerialized = Trie2_32.createFromSerialized(inBytes);
            tailoring.trie = trie2_32CreateFromSerialized;
            data.trie = trie2_32CreateFromSerialized;
            int trieLength = data.trie.getSerializedLength();
            if (trieLength > length4) {
                throw new ICUException("Not enough bytes for the mappings trie");
            }
            length4 -= trieLength;
        } else if (collationData != null) {
            tailoring.data = collationData;
        } else {
            throw new ICUException("Missing collation data mappings");
        }
        ICUBinary.skipBytes(inBytes, length4);
        int offset4 = inIndexes[8];
        int length5 = inIndexes[9] - offset4;
        ICUBinary.skipBytes(inBytes, length5);
        int offset5 = inIndexes[9];
        int length6 = inIndexes[10] - offset5;
        if (length6 >= 8) {
            if (data == null) {
                throw new ICUException("Tailored ces without tailored trie");
            }
            data.ces = ICUBinary.getLongs(inBytes, length6 / 8, length6 & 7);
        } else {
            ICUBinary.skipBytes(inBytes, length6);
        }
        int offset6 = inIndexes[10];
        int length7 = inIndexes[11] - offset6;
        ICUBinary.skipBytes(inBytes, length7);
        int offset7 = inIndexes[11];
        int length8 = inIndexes[12] - offset7;
        if (length8 >= 4) {
            if (data == null) {
                throw new ICUException("Tailored ce32s without tailored trie");
            }
            data.ce32s = ICUBinary.getInts(inBytes, length8 / 4, length8 & 3);
        } else {
            ICUBinary.skipBytes(inBytes, length8);
        }
        int jamoCE32sStart = inIndexes[4];
        if (jamoCE32sStart >= 0) {
            if (data == null || data.ce32s == null) {
                throw new ICUException("JamoCE32sStart index into non-existent ce32s[]");
            }
            data.jamoCE32s = new int[67];
            System.arraycopy(data.ce32s, jamoCE32sStart, data.jamoCE32s, 0, 67);
        } else if (data != null) {
            if (collationData != null) {
                data.jamoCE32s = collationData.jamoCE32s;
            } else {
                throw new ICUException("Missing Jamo CE32s for Hangul processing");
            }
        }
        int offset8 = inIndexes[12];
        int length9 = inIndexes[13] - offset8;
        if (length9 >= 4) {
            int rootElementsLength = length9 / 4;
            if (data == null) {
                throw new ICUException("Root elements but no mappings");
            }
            if (rootElementsLength <= 4) {
                throw new ICUException("Root elements array too short");
            }
            data.rootElements = new long[rootElementsLength];
            for (int i3 = 0; i3 < rootElementsLength; i3++) {
                data.rootElements[i3] = ((long) inBytes.getInt()) & 4294967295L;
            }
            long commonSecTer = data.rootElements[3];
            if (commonSecTer != 83887360) {
                throw new ICUException("Common sec/ter weights in base data differ from the hardcoded value");
            }
            long secTerBoundaries = data.rootElements[4];
            if ((secTerBoundaries >>> 24) < 69) {
                throw new ICUException("[fixed last secondary common byte] is too low");
            }
            length9 &= 3;
        }
        ICUBinary.skipBytes(inBytes, length9);
        int offset9 = inIndexes[13];
        int length10 = inIndexes[14] - offset9;
        if (length10 >= 2) {
            if (data == null) {
                throw new ICUException("Tailored contexts without tailored trie");
            }
            data.contexts = ICUBinary.getString(inBytes, length10 / 2, length10 & 1);
        } else {
            ICUBinary.skipBytes(inBytes, length10);
        }
        int offset10 = inIndexes[14];
        int length11 = inIndexes[15] - offset10;
        if (length11 >= 2) {
            if (data == null) {
                throw new ICUException("Unsafe-backward-set but no mappings");
            }
            if (collationData == null) {
                tailoring.unsafeBackwardSet = new UnicodeSet(UTF16.TRAIL_SURROGATE_MIN_VALUE, 57343);
                data.nfcImpl.addLcccChars(tailoring.unsafeBackwardSet);
            } else {
                tailoring.unsafeBackwardSet = collationData.unsafeBackwardSet.cloneAsThawed();
            }
            USerializedSet sset = new USerializedSet();
            char[] unsafeData = ICUBinary.getChars(inBytes, length11 / 2, length11 & 1);
            length11 = 0;
            sset.getSet(unsafeData, 0);
            int count = sset.countRanges();
            int[] range = new int[2];
            for (int i4 = 0; i4 < count; i4++) {
                sset.getRange(i4, range);
                tailoring.unsafeBackwardSet.add(range[0], range[1]);
            }
            int c = 65536;
            int lead = 55296;
            while (lead < 56320) {
                if (!tailoring.unsafeBackwardSet.containsNone(c, c + Opcodes.OP_NEW_INSTANCE_JUMBO)) {
                    tailoring.unsafeBackwardSet.add(lead);
                }
                lead++;
                c += 1024;
            }
            tailoring.unsafeBackwardSet.freeze();
            data.unsafeBackwardSet = tailoring.unsafeBackwardSet;
        } else if (data != null) {
            if (collationData != null) {
                data.unsafeBackwardSet = collationData.unsafeBackwardSet;
            } else {
                throw new ICUException("Missing unsafe-backward-set");
            }
        }
        ICUBinary.skipBytes(inBytes, length11);
        int offset11 = inIndexes[15];
        int length12 = inIndexes[16] - offset11;
        if (data != null) {
            data.fastLatinTable = null;
            data.fastLatinTableHeader = null;
            if (((inIndexes[1] >> 16) & 255) == 2) {
                if (length12 >= 2) {
                    char header0 = inBytes.getChar();
                    int headerLength = header0 & 255;
                    data.fastLatinTableHeader = new char[headerLength];
                    data.fastLatinTableHeader[0] = header0;
                    for (int i5 = 1; i5 < headerLength; i5++) {
                        data.fastLatinTableHeader[i5] = inBytes.getChar();
                    }
                    int tableLength = (length12 / 2) - headerLength;
                    data.fastLatinTable = ICUBinary.getChars(inBytes, tableLength, length12 & 1);
                    length12 = 0;
                    if ((header0 >> '\b') != 2) {
                        throw new ICUException("Fast-Latin table version differs from version in data header");
                    }
                } else if (collationData != null) {
                    data.fastLatinTable = collationData.fastLatinTable;
                    data.fastLatinTableHeader = collationData.fastLatinTableHeader;
                }
            }
        }
        ICUBinary.skipBytes(inBytes, length12);
        int offset12 = inIndexes[16];
        int length13 = inIndexes[17] - offset12;
        if (length13 >= 2) {
            if (data == null) {
                throw new ICUException("Script order data but no mappings");
            }
            int scriptsLength = length13 / 2;
            CharBuffer inChars = inBytes.asCharBuffer();
            data.numScripts = inChars.get();
            int scriptStartsLength = scriptsLength - ((data.numScripts + 1) + 16);
            if (scriptStartsLength <= 2) {
                throw new ICUException("Script order data too short");
            }
            char[] cArr = new char[data.numScripts + 16];
            data.scriptsIndex = cArr;
            inChars.get(cArr);
            char[] cArr2 = new char[scriptStartsLength];
            data.scriptStarts = cArr2;
            inChars.get(cArr2);
            if (data.scriptStarts[0] != 0 || data.scriptStarts[1] != 768 || data.scriptStarts[scriptStartsLength - 1] != 65280) {
                throw new ICUException("Script order data not valid");
            }
        } else if (data != null && collationData != null) {
            data.numScripts = collationData.numScripts;
            data.scriptsIndex = collationData.scriptsIndex;
            data.scriptStarts = collationData.scriptStarts;
        }
        ICUBinary.skipBytes(inBytes, length13);
        int offset13 = inIndexes[17];
        int length14 = inIndexes[18] - offset13;
        if (length14 >= 256) {
            if (data == null) {
                throw new ICUException("Data for compressible primary lead bytes but no mappings");
            }
            data.compressibleBytes = new boolean[256];
            for (int i6 = 0; i6 < 256; i6++) {
                data.compressibleBytes[i6] = inBytes.get() != 0;
            }
            length14 -= 256;
        } else if (data != null) {
            if (collationData != null) {
                data.compressibleBytes = collationData.compressibleBytes;
            } else {
                throw new ICUException("Missing data for compressible primary lead bytes");
            }
        }
        ICUBinary.skipBytes(inBytes, length14);
        int offset14 = inIndexes[18];
        int length15 = inIndexes[19] - offset14;
        ICUBinary.skipBytes(inBytes, length15);
        CollationSettings ts = (CollationSettings) tailoring.settings.readOnly();
        int options = inIndexes[1] & 65535;
        char[] fastLatinPrimaries = new char[CollationFastLatin.LATIN_LIMIT];
        int fastLatinOptions = CollationFastLatin.getOptions(tailoring.data, ts, fastLatinPrimaries);
        if (options == ts.options && ts.variableTop != 0) {
            if (Arrays.equals(reorderCodes, ts.reorderCodes) && fastLatinOptions == ts.fastLatinOptions && (fastLatinOptions < 0 || Arrays.equals(fastLatinPrimaries, ts.fastLatinPrimaries))) {
                return;
            }
        }
        CollationSettings settings = (CollationSettings) tailoring.settings.copyOnWrite();
        settings.options = options;
        settings.variableTop = tailoring.data.getLastPrimaryForGroup(settings.getMaxVariable() + 4096);
        if (settings.variableTop == 0) {
            throw new ICUException("The maxVariable could not be mapped to a variableTop");
        }
        if (reorderCodesLength != 0) {
            settings.aliasReordering(collationData, reorderCodes, reorderCodesLength, reorderTable);
        }
        settings.fastLatinOptions = CollationFastLatin.getOptions(tailoring.data, settings, settings.fastLatinPrimaries);
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        IsAcceptable(IsAcceptable isAcceptable) {
            this();
        }

        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == 5;
        }
    }

    static {
        f40assertionsDisabled = !CollationDataReader.class.desiredAssertionStatus();
        IS_ACCEPTABLE = new IsAcceptable(null);
    }

    private CollationDataReader() {
    }
}
