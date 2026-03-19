package android.icu.impl.coll;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2_32;
import android.icu.impl.USerializedSet;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUException;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

final class CollationDataReader {
    static final boolean $assertionsDisabled = false;
    private static final int DATA_FORMAT = 1430482796;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
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

    static void read(CollationTailoring collationTailoring, ByteBuffer byteBuffer, CollationTailoring collationTailoring2) throws IOException {
        int i;
        CollationData collationData;
        int[] ints;
        int i2;
        byte[] bArr;
        int[] iArr;
        CollationData collationData2;
        boolean z;
        int[] iArr2;
        collationTailoring2.version = ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
        if (collationTailoring != null && collationTailoring.getUCAVersion() != collationTailoring2.getUCAVersion()) {
            throw new ICUException("Tailoring UCA version differs from base data UCA version");
        }
        int iRemaining = byteBuffer.remaining();
        if (iRemaining < 8) {
            throw new ICUException("not enough bytes");
        }
        int i3 = byteBuffer.getInt();
        if (i3 < 2 || iRemaining < i3 * 4) {
            throw new ICUException("not enough indexes");
        }
        int[] iArr3 = new int[20];
        iArr3[0] = i3;
        for (int i4 = 1; i4 < i3 && i4 < iArr3.length; i4++) {
            iArr3[i4] = byteBuffer.getInt();
        }
        for (int i5 = i3; i5 < iArr3.length; i5++) {
            iArr3[i5] = -1;
        }
        if (i3 > iArr3.length) {
            ICUBinary.skipBytes(byteBuffer, (i3 - iArr3.length) * 4);
        }
        if (i3 > 19) {
            i = iArr3[19];
        } else if (i3 > 5) {
            i = iArr3[i3 - 1];
        } else {
            i = 0;
        }
        if (iRemaining < i) {
            throw new ICUException("not enough bytes");
        }
        if (collationTailoring != null) {
            collationData = collationTailoring.data;
        } else {
            collationData = null;
        }
        int i6 = iArr3[6] - iArr3[5];
        if (i6 >= 4) {
            if (collationData == null) {
                throw new ICUException("Collation base data must not reorder scripts");
            }
            int i7 = i6 / 4;
            ints = ICUBinary.getInts(byteBuffer, i7, i6 & 3);
            int i8 = 0;
            while (i8 < i7 && (ints[(i7 - i8) - 1] & (-65536)) != 0) {
                i8++;
            }
            i2 = i7 - i8;
        } else {
            ICUBinary.skipBytes(byteBuffer, i6);
            ints = new int[0];
            i2 = 0;
        }
        int i9 = iArr3[7] - iArr3[6];
        if (i9 >= 256) {
            if (i2 == 0) {
                throw new ICUException("Reordering table without reordering codes");
            }
            bArr = new byte[256];
            byteBuffer.get(bArr);
            i9 -= 256;
        } else {
            bArr = null;
        }
        ICUBinary.skipBytes(byteBuffer, i9);
        if (collationData != null) {
            iArr = ints;
            if (collationData.numericPrimary != (((long) iArr3[1]) & 4278190080L)) {
                throw new ICUException("Tailoring numeric primary weight differs from base data");
            }
        } else {
            iArr = ints;
        }
        int i10 = iArr3[8] - iArr3[7];
        if (i10 >= 8) {
            collationTailoring2.ensureOwnedData();
            collationData2 = collationTailoring2.ownedData;
            collationData2.base = collationData;
            collationData2.numericPrimary = ((long) iArr3[1]) & 4278190080L;
            Trie2_32 trie2_32CreateFromSerialized = Trie2_32.createFromSerialized(byteBuffer);
            collationTailoring2.trie = trie2_32CreateFromSerialized;
            collationData2.trie = trie2_32CreateFromSerialized;
            int serializedLength = collationData2.trie.getSerializedLength();
            if (serializedLength > i10) {
                throw new ICUException("Not enough bytes for the mappings trie");
            }
            i10 -= serializedLength;
        } else if (collationData != null) {
            collationTailoring2.data = collationData;
            collationData2 = null;
        } else {
            throw new ICUException("Missing collation data mappings");
        }
        ICUBinary.skipBytes(byteBuffer, i10);
        ICUBinary.skipBytes(byteBuffer, iArr3[9] - iArr3[8]);
        int i11 = iArr3[10] - iArr3[9];
        if (i11 >= 8) {
            if (collationData2 != null) {
                collationData2.ces = ICUBinary.getLongs(byteBuffer, i11 / 8, i11 & 7);
            } else {
                throw new ICUException("Tailored ces without tailored trie");
            }
        } else {
            ICUBinary.skipBytes(byteBuffer, i11);
        }
        ICUBinary.skipBytes(byteBuffer, iArr3[11] - iArr3[10]);
        int i12 = iArr3[12] - iArr3[11];
        if (i12 >= 4) {
            if (collationData2 == null) {
                throw new ICUException("Tailored ce32s without tailored trie");
            }
            collationData2.ce32s = ICUBinary.getInts(byteBuffer, i12 / 4, i12 & 3);
        } else {
            ICUBinary.skipBytes(byteBuffer, i12);
        }
        int i13 = iArr3[4];
        if (i13 >= 0) {
            if (collationData2 == null || collationData2.ce32s == null) {
                throw new ICUException("JamoCE32sStart index into non-existent ce32s[]");
            }
            collationData2.jamoCE32s = new int[67];
            System.arraycopy(collationData2.ce32s, i13, collationData2.jamoCE32s, 0, 67);
        } else if (collationData2 != null) {
            if (collationData != null) {
                collationData2.jamoCE32s = collationData.jamoCE32s;
            } else {
                throw new ICUException("Missing Jamo CE32s for Hangul processing");
            }
        }
        int i14 = iArr3[13] - iArr3[12];
        if (i14 >= 4) {
            int i15 = i14 / 4;
            if (collationData2 == null) {
                throw new ICUException("Root elements but no mappings");
            }
            if (i15 <= 4) {
                throw new ICUException("Root elements array too short");
            }
            collationData2.rootElements = new long[i15];
            for (int i16 = 0; i16 < i15; i16++) {
                collationData2.rootElements[i16] = ((long) byteBuffer.getInt()) & 4294967295L;
            }
            if (collationData2.rootElements[3] != 83887360) {
                throw new ICUException("Common sec/ter weights in base data differ from the hardcoded value");
            }
            if ((collationData2.rootElements[4] >>> 24) < 69) {
                throw new ICUException("[fixed last secondary common byte] is too low");
            }
            i14 &= 3;
        }
        ICUBinary.skipBytes(byteBuffer, i14);
        int i17 = iArr3[14] - iArr3[13];
        if (i17 >= 2) {
            if (collationData2 == null) {
                throw new ICUException("Tailored contexts without tailored trie");
            }
            collationData2.contexts = ICUBinary.getString(byteBuffer, i17 / 2, i17 & 1);
        } else {
            ICUBinary.skipBytes(byteBuffer, i17);
        }
        int i18 = iArr3[15] - iArr3[14];
        if (i18 >= 2) {
            if (collationData2 == null) {
                throw new ICUException("Unsafe-backward-set but no mappings");
            }
            if (collationData == null) {
                collationTailoring2.unsafeBackwardSet = new UnicodeSet(UTF16.TRAIL_SURROGATE_MIN_VALUE, 57343);
                collationData2.nfcImpl.addLcccChars(collationTailoring2.unsafeBackwardSet);
            } else {
                collationTailoring2.unsafeBackwardSet = collationData.unsafeBackwardSet.cloneAsThawed();
            }
            USerializedSet uSerializedSet = new USerializedSet();
            uSerializedSet.getSet(ICUBinary.getChars(byteBuffer, i18 / 2, i18 & 1), 0);
            int iCountRanges = uSerializedSet.countRanges();
            int[] iArr4 = new int[2];
            for (int i19 = 0; i19 < iCountRanges; i19++) {
                uSerializedSet.getRange(i19, iArr4);
                collationTailoring2.unsafeBackwardSet.add(iArr4[0], iArr4[1]);
            }
            int i20 = 65536;
            int i21 = 55296;
            while (i21 < 56320) {
                if (!collationTailoring2.unsafeBackwardSet.containsNone(i20, i20 + Opcodes.OP_NEW_INSTANCE_JUMBO)) {
                    collationTailoring2.unsafeBackwardSet.add(i21);
                }
                i21++;
                i20 += 1024;
            }
            collationTailoring2.unsafeBackwardSet.freeze();
            collationData2.unsafeBackwardSet = collationTailoring2.unsafeBackwardSet;
            i18 = 0;
        } else if (collationData2 != null) {
            if (collationData != null) {
                collationData2.unsafeBackwardSet = collationData.unsafeBackwardSet;
            } else {
                throw new ICUException("Missing unsafe-backward-set");
            }
        }
        ICUBinary.skipBytes(byteBuffer, i18);
        int i22 = iArr3[16] - iArr3[15];
        if (collationData2 != null) {
            collationData2.fastLatinTable = null;
            collationData2.fastLatinTableHeader = null;
            if (((iArr3[1] >> 16) & 255) == 2) {
                if (i22 >= 2) {
                    char c = byteBuffer.getChar();
                    int i23 = c & 255;
                    collationData2.fastLatinTableHeader = new char[i23];
                    collationData2.fastLatinTableHeader[0] = c;
                    for (int i24 = 1; i24 < i23; i24++) {
                        collationData2.fastLatinTableHeader[i24] = byteBuffer.getChar();
                    }
                    collationData2.fastLatinTable = ICUBinary.getChars(byteBuffer, (i22 / 2) - i23, i22 & 1);
                    if ((c >> '\b') != 2) {
                        throw new ICUException("Fast-Latin table version differs from version in data header");
                    }
                    i22 = 0;
                } else if (collationData != null) {
                    collationData2.fastLatinTable = collationData.fastLatinTable;
                    collationData2.fastLatinTableHeader = collationData.fastLatinTableHeader;
                }
            }
        }
        ICUBinary.skipBytes(byteBuffer, i22);
        int i25 = iArr3[17] - iArr3[16];
        if (i25 >= 2) {
            if (collationData2 == null) {
                throw new ICUException("Script order data but no mappings");
            }
            CharBuffer charBufferAsCharBuffer = byteBuffer.asCharBuffer();
            collationData2.numScripts = charBufferAsCharBuffer.get();
            int i26 = (i25 / 2) - ((collationData2.numScripts + 1) + 16);
            if (i26 > 2) {
                char[] cArr = new char[collationData2.numScripts + 16];
                collationData2.scriptsIndex = cArr;
                charBufferAsCharBuffer.get(cArr);
                char[] cArr2 = new char[i26];
                collationData2.scriptStarts = cArr2;
                charBufferAsCharBuffer.get(cArr2);
                z = false;
                if (collationData2.scriptStarts[0] != 0 || collationData2.scriptStarts[1] != 768 || collationData2.scriptStarts[i26 - 1] != 65280) {
                    throw new ICUException("Script order data not valid");
                }
            } else {
                throw new ICUException("Script order data too short");
            }
        } else {
            z = false;
            if (collationData2 != null && collationData != null) {
                collationData2.numScripts = collationData.numScripts;
                collationData2.scriptsIndex = collationData.scriptsIndex;
                collationData2.scriptStarts = collationData.scriptStarts;
            }
        }
        ICUBinary.skipBytes(byteBuffer, i25);
        int i27 = iArr3[18] - iArr3[17];
        if (i27 >= 256) {
            if (collationData2 == null) {
                throw new ICUException("Data for compressible primary lead bytes but no mappings");
            }
            collationData2.compressibleBytes = new boolean[256];
            for (?? r8 = z; r8 < 256; r8++) {
                collationData2.compressibleBytes[r8] = byteBuffer.get() != 0 ? true : z;
            }
            i27 -= 256;
        } else if (collationData2 != null) {
            if (collationData != null) {
                collationData2.compressibleBytes = collationData.compressibleBytes;
            } else {
                throw new ICUException("Missing data for compressible primary lead bytes");
            }
        }
        ICUBinary.skipBytes(byteBuffer, i27);
        ICUBinary.skipBytes(byteBuffer, iArr3[19] - iArr3[18]);
        CollationSettings collationSettings = (CollationSettings) collationTailoring2.settings.readOnly();
        int i28 = iArr3[1] & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
        char[] cArr3 = new char[CollationFastLatin.LATIN_LIMIT];
        int options = CollationFastLatin.getOptions(collationTailoring2.data, collationSettings, cArr3);
        if (i28 == collationSettings.options && collationSettings.variableTop != 0) {
            iArr2 = iArr;
            if (Arrays.equals(iArr2, collationSettings.reorderCodes) && options == collationSettings.fastLatinOptions && (options < 0 || Arrays.equals(cArr3, collationSettings.fastLatinPrimaries))) {
                return;
            }
        } else {
            iArr2 = iArr;
        }
        CollationSettings collationSettings2 = (CollationSettings) collationTailoring2.settings.copyOnWrite();
        collationSettings2.options = i28;
        collationSettings2.variableTop = collationTailoring2.data.getLastPrimaryForGroup(4096 + collationSettings2.getMaxVariable());
        if (collationSettings2.variableTop == 0) {
            throw new ICUException("The maxVariable could not be mapped to a variableTop");
        }
        if (i2 != 0) {
            collationSettings2.aliasReordering(collationData, iArr2, i2, bArr);
        }
        collationSettings2.fastLatinOptions = CollationFastLatin.getOptions(collationTailoring2.data, collationSettings2, collationSettings2.fastLatinPrimaries);
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return bArr[0] == 5;
        }
    }

    private CollationDataReader() {
    }
}
