package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.VersionInfo;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

public final class Normalizer2Impl {
    static final boolean $assertionsDisabled = false;
    private static final int CANON_HAS_COMPOSITIONS = 1073741824;
    private static final int CANON_HAS_SET = 2097152;
    private static final int CANON_NOT_SEGMENT_STARTER = Integer.MIN_VALUE;
    private static final int CANON_VALUE_MASK = 2097151;
    public static final int COMP_1_LAST_TUPLE = 32768;
    public static final int COMP_1_TRAIL_LIMIT = 13312;
    public static final int COMP_1_TRAIL_MASK = 32766;
    public static final int COMP_1_TRAIL_SHIFT = 9;
    public static final int COMP_1_TRIPLE = 1;
    public static final int COMP_2_TRAIL_MASK = 65472;
    public static final int COMP_2_TRAIL_SHIFT = 6;
    private static final int DATA_FORMAT = 1316121906;
    public static final int DELTA_SHIFT = 3;
    public static final int DELTA_TCCC_0 = 0;
    public static final int DELTA_TCCC_1 = 2;
    public static final int DELTA_TCCC_GT_1 = 4;
    public static final int DELTA_TCCC_MASK = 6;
    public static final int HAS_COMP_BOUNDARY_AFTER = 1;
    public static final int INERT = 1;
    public static final int IX_COUNT = 20;
    public static final int IX_EXTRA_DATA_OFFSET = 1;
    public static final int IX_LIMIT_NO_NO = 12;
    public static final int IX_MIN_COMP_NO_MAYBE_CP = 9;
    public static final int IX_MIN_DECOMP_NO_CP = 8;
    public static final int IX_MIN_LCCC_CP = 18;
    public static final int IX_MIN_MAYBE_YES = 13;
    public static final int IX_MIN_NO_NO = 11;
    public static final int IX_MIN_NO_NO_COMP_BOUNDARY_BEFORE = 15;
    public static final int IX_MIN_NO_NO_COMP_NO_MAYBE_CC = 16;
    public static final int IX_MIN_NO_NO_EMPTY = 17;
    public static final int IX_MIN_YES_NO = 10;
    public static final int IX_MIN_YES_NO_MAPPINGS_ONLY = 14;
    public static final int IX_NORM_TRIE_OFFSET = 0;
    public static final int IX_RESERVED3_OFFSET = 3;
    public static final int IX_SMALL_FCD_OFFSET = 2;
    public static final int IX_TOTAL_SIZE = 7;
    public static final int JAMO_L = 2;
    public static final int JAMO_VT = 65024;
    public static final int MAPPING_HAS_CCC_LCCC_WORD = 128;
    public static final int MAPPING_HAS_RAW_MAPPING = 64;
    public static final int MAPPING_LENGTH_MASK = 31;
    public static final int MAX_DELTA = 64;
    public static final int MIN_NORMAL_MAYBE_YES = 64512;
    public static final int MIN_YES_YES_WITH_CC = 65026;
    public static final int OFFSET_SHIFT = 1;
    private Trie2_32 canonIterData;
    private ArrayList<UnicodeSet> canonStartSets;
    private int centerNoNoDelta;
    private VersionInfo dataVersion;
    private String extraData;
    private int limitNoNo;
    private String maybeYesCompositions;
    private int minCompNoMaybeCP;
    private int minDecompNoCP;
    private int minLcccCP;
    private int minMaybeYes;
    private int minNoNo;
    private int minNoNoCompBoundaryBefore;
    private int minNoNoCompNoMaybeCC;
    private int minNoNoEmpty;
    private int minYesNo;
    private int minYesNoMappingsOnly;
    private Trie2_16 normTrie;
    private byte[] smallFCD;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    private static final Trie2.ValueMapper segmentStarterMapper = new Trie2.ValueMapper() {
        @Override
        public int map(int i) {
            return i & Integer.MIN_VALUE;
        }
    };

    public static final class Hangul {
        public static final int HANGUL_BASE = 44032;
        public static final int HANGUL_COUNT = 11172;
        public static final int HANGUL_END = 55203;
        public static final int HANGUL_LIMIT = 55204;
        public static final int JAMO_L_BASE = 4352;
        public static final int JAMO_L_COUNT = 19;
        public static final int JAMO_L_END = 4370;
        public static final int JAMO_L_LIMIT = 4371;
        public static final int JAMO_T_BASE = 4519;
        public static final int JAMO_T_COUNT = 28;
        public static final int JAMO_T_END = 4546;
        public static final int JAMO_VT_COUNT = 588;
        public static final int JAMO_V_BASE = 4449;
        public static final int JAMO_V_COUNT = 21;
        public static final int JAMO_V_END = 4469;
        public static final int JAMO_V_LIMIT = 4470;

        public static boolean isHangul(int i) {
            return 44032 <= i && i < 55204;
        }

        public static boolean isHangulLV(int i) {
            int i2 = i - HANGUL_BASE;
            return i2 >= 0 && i2 < 11172 && i2 % 28 == 0;
        }

        public static boolean isJamoL(int i) {
            return 4352 <= i && i < 4371;
        }

        public static boolean isJamoV(int i) {
            return 4449 <= i && i < 4470;
        }

        public static boolean isJamoT(int i) {
            int i2 = i - 4519;
            return i2 > 0 && i2 < 28;
        }

        public static boolean isJamo(int i) {
            return 4352 <= i && i <= 4546 && (i <= 4370 || ((4449 <= i && i <= 4469) || 4519 < i));
        }

        public static int decompose(int i, Appendable appendable) {
            int i2 = i - HANGUL_BASE;
            try {
                int i3 = i2 % 28;
                int i4 = i2 / 28;
                appendable.append((char) (JAMO_L_BASE + (i4 / 21)));
                appendable.append((char) (JAMO_V_BASE + (i4 % 21)));
                if (i3 == 0) {
                    return 2;
                }
                appendable.append((char) (JAMO_T_BASE + i3));
                return 3;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public static void getRawDecomposition(int i, Appendable appendable) {
            int i2 = i - HANGUL_BASE;
            try {
                int i3 = i2 % 28;
                if (i3 == 0) {
                    int i4 = i2 / 28;
                    appendable.append((char) (JAMO_L_BASE + (i4 / 21)));
                    appendable.append((char) (JAMO_V_BASE + (i4 % 21)));
                } else {
                    appendable.append((char) (i - i3));
                    appendable.append((char) (JAMO_T_BASE + i3));
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
    }

    public static final class ReorderingBuffer implements Appendable {
        private final Appendable app;
        private final boolean appIsStringBuilder;
        private int codePointLimit;
        private int codePointStart;
        private final Normalizer2Impl impl;
        private int lastCC;
        private int reorderStart;
        private final StringBuilder str;

        public ReorderingBuffer(Normalizer2Impl normalizer2Impl, Appendable appendable, int i) {
            this.impl = normalizer2Impl;
            this.app = appendable;
            if (this.app instanceof StringBuilder) {
                this.appIsStringBuilder = true;
                this.str = (StringBuilder) appendable;
                this.str.ensureCapacity(i);
                this.reorderStart = 0;
                if (this.str.length() == 0) {
                    this.lastCC = 0;
                    return;
                }
                setIterator();
                this.lastCC = previousCC();
                if (this.lastCC > 1) {
                    while (previousCC() > 1) {
                    }
                }
                this.reorderStart = this.codePointLimit;
                return;
            }
            this.appIsStringBuilder = false;
            this.str = new StringBuilder();
            this.reorderStart = 0;
            this.lastCC = 0;
        }

        public boolean isEmpty() {
            return this.str.length() == 0;
        }

        public int length() {
            return this.str.length();
        }

        public int getLastCC() {
            return this.lastCC;
        }

        public StringBuilder getStringBuilder() {
            return this.str;
        }

        public boolean equals(CharSequence charSequence, int i, int i2) {
            return UTF16Plus.equal(this.str, 0, this.str.length(), charSequence, i, i2);
        }

        public void append(int i, int i2) {
            if (this.lastCC <= i2 || i2 == 0) {
                this.str.appendCodePoint(i);
                this.lastCC = i2;
                if (i2 <= 1) {
                    this.reorderStart = this.str.length();
                    return;
                }
                return;
            }
            insert(i, i2);
        }

        public void append(CharSequence charSequence, int i, int i2, int i3, int i4) {
            int cCFromYesOrMaybe;
            if (i == i2) {
                return;
            }
            if (this.lastCC <= i3 || i3 == 0) {
                if (i4 <= 1) {
                    this.reorderStart = this.str.length() + (i2 - i);
                } else if (i3 <= 1) {
                    this.reorderStart = this.str.length() + 1;
                }
                this.str.append(charSequence, i, i2);
                this.lastCC = i4;
                return;
            }
            int iCodePointAt = Character.codePointAt(charSequence, i);
            int iCharCount = i + Character.charCount(iCodePointAt);
            insert(iCodePointAt, i3);
            while (iCharCount < i2) {
                int iCodePointAt2 = Character.codePointAt(charSequence, iCharCount);
                iCharCount += Character.charCount(iCodePointAt2);
                if (iCharCount < i2) {
                    cCFromYesOrMaybe = Normalizer2Impl.getCCFromYesOrMaybe(this.impl.getNorm16(iCodePointAt2));
                } else {
                    cCFromYesOrMaybe = i4;
                }
                append(iCodePointAt2, cCFromYesOrMaybe);
            }
        }

        @Override
        public ReorderingBuffer append(char c) {
            this.str.append(c);
            this.lastCC = 0;
            this.reorderStart = this.str.length();
            return this;
        }

        public void appendZeroCC(int i) {
            this.str.appendCodePoint(i);
            this.lastCC = 0;
            this.reorderStart = this.str.length();
        }

        @Override
        public ReorderingBuffer append(CharSequence charSequence) {
            if (charSequence.length() != 0) {
                this.str.append(charSequence);
                this.lastCC = 0;
                this.reorderStart = this.str.length();
            }
            return this;
        }

        @Override
        public ReorderingBuffer append(CharSequence charSequence, int i, int i2) {
            if (i != i2) {
                this.str.append(charSequence, i, i2);
                this.lastCC = 0;
                this.reorderStart = this.str.length();
            }
            return this;
        }

        public void flush() {
            if (this.appIsStringBuilder) {
                this.reorderStart = this.str.length();
            } else {
                try {
                    this.app.append(this.str);
                    this.str.setLength(0);
                    this.reorderStart = 0;
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
            this.lastCC = 0;
        }

        public ReorderingBuffer flushAndAppendZeroCC(CharSequence charSequence, int i, int i2) {
            if (this.appIsStringBuilder) {
                this.str.append(charSequence, i, i2);
                this.reorderStart = this.str.length();
            } else {
                try {
                    this.app.append(this.str).append(charSequence, i, i2);
                    this.str.setLength(0);
                    this.reorderStart = 0;
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
            this.lastCC = 0;
            return this;
        }

        public void remove() {
            this.str.setLength(0);
            this.lastCC = 0;
            this.reorderStart = 0;
        }

        public void removeSuffix(int i) {
            int length = this.str.length();
            this.str.delete(length - i, length);
            this.lastCC = 0;
            this.reorderStart = this.str.length();
        }

        private void insert(int i, int i2) {
            setIterator();
            skipPrevious();
            while (previousCC() > i2) {
            }
            if (i <= 65535) {
                this.str.insert(this.codePointLimit, (char) i);
                if (i2 <= 1) {
                    this.reorderStart = this.codePointLimit + 1;
                    return;
                }
                return;
            }
            this.str.insert(this.codePointLimit, Character.toChars(i));
            if (i2 <= 1) {
                this.reorderStart = this.codePointLimit + 2;
            }
        }

        private void setIterator() {
            this.codePointStart = this.str.length();
        }

        private void skipPrevious() {
            this.codePointLimit = this.codePointStart;
            this.codePointStart = this.str.offsetByCodePoints(this.codePointStart, -1);
        }

        private int previousCC() {
            this.codePointLimit = this.codePointStart;
            if (this.reorderStart >= this.codePointStart) {
                return 0;
            }
            int iCodePointBefore = this.str.codePointBefore(this.codePointStart);
            this.codePointStart -= Character.charCount(iCodePointBefore);
            return this.impl.getCCFromYesOrMaybeCP(iCodePointBefore);
        }
    }

    public static final class UTF16Plus {
        public static boolean isSurrogateLead(int i) {
            return (i & 1024) == 0;
        }

        public static boolean equal(CharSequence charSequence, CharSequence charSequence2) {
            if (charSequence == charSequence2) {
                return true;
            }
            int length = charSequence.length();
            if (length != charSequence2.length()) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (charSequence.charAt(i) != charSequence2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        public static boolean equal(CharSequence charSequence, int i, int i2, CharSequence charSequence2, int i3, int i4) {
            if (i2 - i != i4 - i3) {
                return false;
            }
            if (charSequence == charSequence2 && i == i3) {
                return true;
            }
            while (i < i2) {
                int i5 = i + 1;
                int i6 = i3 + 1;
                if (charSequence.charAt(i) != charSequence2.charAt(i3)) {
                    return false;
                }
                i = i5;
                i3 = i6;
            }
            return true;
        }
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return bArr[0] == 3;
        }
    }

    public Normalizer2Impl load(ByteBuffer byteBuffer) {
        try {
            this.dataVersion = ICUBinary.readHeaderAndDataVersion(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
            int i = byteBuffer.getInt() / 4;
            if (i <= 18) {
                throw new ICUUncheckedIOException("Normalizer2 data: not enough indexes");
            }
            int[] iArr = new int[i];
            iArr[0] = i * 4;
            for (int i2 = 1; i2 < i; i2++) {
                iArr[i2] = byteBuffer.getInt();
            }
            this.minDecompNoCP = iArr[8];
            this.minCompNoMaybeCP = iArr[9];
            this.minLcccCP = iArr[18];
            this.minYesNo = iArr[10];
            this.minYesNoMappingsOnly = iArr[14];
            this.minNoNo = iArr[11];
            this.minNoNoCompBoundaryBefore = iArr[15];
            this.minNoNoCompNoMaybeCC = iArr[16];
            this.minNoNoEmpty = iArr[17];
            this.limitNoNo = iArr[12];
            this.minMaybeYes = iArr[13];
            this.centerNoNoDelta = ((this.minMaybeYes >> 3) - 64) - 1;
            int i3 = iArr[0];
            int i4 = iArr[1];
            this.normTrie = Trie2_16.createFromSerialized(byteBuffer);
            int serializedLength = this.normTrie.getSerializedLength();
            int i5 = i4 - i3;
            if (serializedLength > i5) {
                throw new ICUUncheckedIOException("Normalizer2 data: not enough bytes for normTrie");
            }
            ICUBinary.skipBytes(byteBuffer, i5 - serializedLength);
            int i6 = (iArr[2] - i4) / 2;
            if (i6 != 0) {
                this.maybeYesCompositions = ICUBinary.getString(byteBuffer, i6, 0);
                this.extraData = this.maybeYesCompositions.substring((MIN_NORMAL_MAYBE_YES - this.minMaybeYes) >> 1);
            }
            this.smallFCD = new byte[256];
            byteBuffer.get(this.smallFCD);
            return this;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public Normalizer2Impl load(String str) {
        return load(ICUBinary.getRequiredData(str));
    }

    private void enumLcccRange(int i, int i2, int i3, UnicodeSet unicodeSet) {
        if (i3 > 64512 && i3 != 65024) {
            unicodeSet.add(i, i2);
        } else {
            if (this.minNoNoCompNoMaybeCC > i3 || i3 >= this.limitNoNo || getFCD16(i) <= 255) {
                return;
            }
            unicodeSet.add(i, i2);
        }
    }

    private void enumNorm16PropertyStartsRange(int i, int i2, int i3, UnicodeSet unicodeSet) {
        unicodeSet.add(i);
        if (i != i2 && isAlgorithmicNoNo(i3) && (i3 & 6) > 2) {
            int fcd16 = getFCD16(i);
            while (true) {
                i++;
                if (i <= i2) {
                    int fcd162 = getFCD16(i);
                    if (fcd162 != fcd16) {
                        unicodeSet.add(i);
                        fcd16 = fcd162;
                    }
                } else {
                    return;
                }
            }
        }
    }

    public void addLcccChars(UnicodeSet unicodeSet) {
        for (Trie2.Range range : this.normTrie) {
            if (!range.leadSurrogate) {
                enumLcccRange(range.startCodePoint, range.endCodePoint, range.value, unicodeSet);
            } else {
                return;
            }
        }
    }

    public void addPropertyStarts(UnicodeSet unicodeSet) {
        for (Trie2.Range range : this.normTrie) {
            if (range.leadSurrogate) {
                break;
            } else {
                enumNorm16PropertyStartsRange(range.startCodePoint, range.endCodePoint, range.value, unicodeSet);
            }
        }
        for (int i = Hangul.HANGUL_BASE; i < 55204; i += 28) {
            unicodeSet.add(i);
            unicodeSet.add(i + 1);
        }
        unicodeSet.add(Hangul.HANGUL_LIMIT);
    }

    public void addCanonIterPropertyStarts(UnicodeSet unicodeSet) {
        ensureCanonIterData();
        Iterator<Trie2.Range> it = this.canonIterData.iterator(segmentStarterMapper);
        while (it.hasNext()) {
            Trie2.Range next = it.next();
            if (!next.leadSurrogate) {
                unicodeSet.add(next.startCodePoint);
            } else {
                return;
            }
        }
    }

    public synchronized Normalizer2Impl ensureCanonIterData() {
        int norm16;
        int iMapAlgorithmic;
        int i;
        if (this.canonIterData == null) {
            Trie2Writable trie2Writable = new Trie2Writable(0, 0);
            this.canonStartSets = new ArrayList<>();
            for (Trie2.Range range : this.normTrie) {
                if (range.leadSurrogate) {
                    break;
                }
                int i2 = range.value;
                if (!isInert(i2) && (this.minYesNo > i2 || i2 >= this.minNoNo)) {
                    for (int i3 = range.startCodePoint; i3 <= range.endCodePoint; i3++) {
                        int i4 = trie2Writable.get(i3);
                        if (isMaybeOrNonZeroCC(i2)) {
                            i = i4 | Integer.MIN_VALUE;
                            if (i2 < 64512) {
                                i |= 1073741824;
                            }
                        } else if (i2 < this.minYesNo) {
                            i = i4 | 1073741824;
                        } else {
                            if (isDecompNoAlgorithmic(i2)) {
                                iMapAlgorithmic = mapAlgorithmic(i3, i2);
                                norm16 = getNorm16(iMapAlgorithmic);
                            } else {
                                norm16 = i2;
                                iMapAlgorithmic = i3;
                            }
                            if (norm16 > this.minYesNo) {
                                int i5 = norm16 >> 1;
                                char cCharAt = this.extraData.charAt(i5);
                                int i6 = cCharAt & 31;
                                if ((cCharAt & 128) != 0 && i3 == iMapAlgorithmic && (this.extraData.charAt(i5 - 1) & 255) != 0) {
                                    i = i4 | Integer.MIN_VALUE;
                                } else {
                                    i = i4;
                                }
                                if (i6 != 0) {
                                    int iCharCount = i5 + 1;
                                    int i7 = i6 + iCharCount;
                                    int iCodePointAt = this.extraData.codePointAt(iCharCount);
                                    addToStartSet(trie2Writable, i3, iCodePointAt);
                                    if (norm16 >= this.minNoNo) {
                                        while (true) {
                                            iCharCount += Character.charCount(iCodePointAt);
                                            if (iCharCount >= i7) {
                                                break;
                                            }
                                            iCodePointAt = this.extraData.codePointAt(iCharCount);
                                            int i8 = trie2Writable.get(iCodePointAt);
                                            if ((i8 & Integer.MIN_VALUE) == 0) {
                                                trie2Writable.set(iCodePointAt, i8 | Integer.MIN_VALUE);
                                            }
                                        }
                                    }
                                }
                            } else {
                                addToStartSet(trie2Writable, i3, iMapAlgorithmic);
                                i = i4;
                            }
                        }
                        if (i != i4) {
                            trie2Writable.set(i3, i);
                        }
                    }
                }
            }
            this.canonIterData = trie2Writable.toTrie2_32();
        }
        return this;
    }

    public int getNorm16(int i) {
        return this.normTrie.get(i);
    }

    public int getCompQuickCheck(int i) {
        if (i < this.minNoNo || 65026 <= i) {
            return 1;
        }
        if (this.minMaybeYes <= i) {
            return 2;
        }
        return 0;
    }

    public boolean isAlgorithmicNoNo(int i) {
        return this.limitNoNo <= i && i < this.minMaybeYes;
    }

    public boolean isCompNo(int i) {
        return this.minNoNo <= i && i < this.minMaybeYes;
    }

    public boolean isDecompYes(int i) {
        return i < this.minYesNo || this.minMaybeYes <= i;
    }

    public int getCC(int i) {
        if (i >= 64512) {
            return getCCFromNormalYesOrMaybe(i);
        }
        if (i < this.minNoNo || this.limitNoNo <= i) {
            return 0;
        }
        return getCCFromNoNo(i);
    }

    public static int getCCFromNormalYesOrMaybe(int i) {
        return (i >> 1) & 255;
    }

    public static int getCCFromYesOrMaybe(int i) {
        if (i >= 64512) {
            return getCCFromNormalYesOrMaybe(i);
        }
        return 0;
    }

    public int getCCFromYesOrMaybeCP(int i) {
        if (i < this.minCompNoMaybeCP) {
            return 0;
        }
        return getCCFromYesOrMaybe(getNorm16(i));
    }

    public int getFCD16(int i) {
        if (i < this.minDecompNoCP) {
            return 0;
        }
        if (i > 65535 || singleLeadMightHaveNonZeroFCD16(i)) {
            return getFCD16FromNormData(i);
        }
        return 0;
    }

    public boolean singleLeadMightHaveNonZeroFCD16(int i) {
        byte b = this.smallFCD[i >> 8];
        return (b == 0 || ((b >> ((i >> 5) & 7)) & 1) == 0) ? false : true;
    }

    public int getFCD16FromNormData(int i) {
        int norm16 = getNorm16(i);
        if (norm16 >= this.limitNoNo) {
            if (norm16 >= 64512) {
                int cCFromNormalYesOrMaybe = getCCFromNormalYesOrMaybe(norm16);
                return cCFromNormalYesOrMaybe | (cCFromNormalYesOrMaybe << 8);
            }
            if (norm16 >= this.minMaybeYes) {
                return 0;
            }
            int i2 = norm16 & 6;
            if (i2 <= 2) {
                return i2 >> 1;
            }
            norm16 = getNorm16(mapAlgorithmic(i, norm16));
        }
        if (norm16 <= this.minYesNo || isHangulLVT(norm16)) {
            return 0;
        }
        int i3 = norm16 >> 1;
        char cCharAt = this.extraData.charAt(i3);
        int i4 = cCharAt >> '\b';
        if ((cCharAt & 128) != 0) {
            return i4 | (this.extraData.charAt(i3 - 1) & 65280);
        }
        return i4;
    }

    public String getDecomposition(int i) {
        int i2;
        int iMapAlgorithmic;
        if (i >= this.minDecompNoCP) {
            int norm16 = getNorm16(i);
            if (!isMaybeOrNonZeroCC(norm16)) {
                if (!isDecompNoAlgorithmic(norm16)) {
                    i2 = i;
                    iMapAlgorithmic = -1;
                } else {
                    iMapAlgorithmic = mapAlgorithmic(i, norm16);
                    norm16 = getNorm16(iMapAlgorithmic);
                    i2 = iMapAlgorithmic;
                }
                if (norm16 < this.minYesNo) {
                    if (iMapAlgorithmic < 0) {
                        return null;
                    }
                    return UTF16.valueOf(iMapAlgorithmic);
                }
                if (isHangulLV(norm16) || isHangulLVT(norm16)) {
                    StringBuilder sb = new StringBuilder();
                    Hangul.decompose(i2, sb);
                    return sb.toString();
                }
                int i3 = norm16 >> 1;
                int i4 = i3 + 1;
                return this.extraData.substring(i4, (this.extraData.charAt(i3) & 31) + i4);
            }
        }
        return null;
    }

    public String getRawDecomposition(int i) {
        if (i < this.minDecompNoCP) {
            return null;
        }
        int norm16 = getNorm16(i);
        if (isDecompYes(norm16)) {
            return null;
        }
        if (isHangulLV(norm16) || isHangulLVT(norm16)) {
            StringBuilder sb = new StringBuilder();
            Hangul.getRawDecomposition(i, sb);
            return sb.toString();
        }
        if (isDecompNoAlgorithmic(norm16)) {
            return UTF16.valueOf(mapAlgorithmic(i, norm16));
        }
        int i2 = norm16 >> 1;
        char cCharAt = this.extraData.charAt(i2);
        int i3 = cCharAt & 31;
        if ((cCharAt & '@') != 0) {
            int i4 = (i2 - ((cCharAt >> 7) & 1)) - 1;
            char cCharAt2 = this.extraData.charAt(i4);
            if (cCharAt2 <= 31) {
                return this.extraData.substring(i4 - cCharAt2, i4);
            }
            StringBuilder sb2 = new StringBuilder(i3 - 1);
            sb2.append(cCharAt2);
            sb2.append((CharSequence) this.extraData, i2 + 3, (i3 + r5) - 2);
            return sb2.toString();
        }
        int i5 = i2 + 1;
        return this.extraData.substring(i5, i3 + i5);
    }

    public boolean isCanonSegmentStarter(int i) {
        return this.canonIterData.get(i) >= 0;
    }

    public boolean getCanonStartSet(int i, UnicodeSet unicodeSet) {
        int i2 = this.canonIterData.get(i) & Integer.MAX_VALUE;
        if (i2 == 0) {
            return false;
        }
        unicodeSet.clear();
        int i3 = 2097151 & i2;
        if ((2097152 & i2) != 0) {
            unicodeSet.addAll(this.canonStartSets.get(i3));
        } else if (i3 != 0) {
            unicodeSet.add(i3);
        }
        if ((i2 & 1073741824) != 0) {
            int norm16 = getNorm16(i);
            if (norm16 == 2) {
                int i4 = Hangul.HANGUL_BASE + ((i - 4352) * Hangul.JAMO_VT_COUNT);
                unicodeSet.add(i4, (i4 + Hangul.JAMO_VT_COUNT) - 1);
            } else {
                addComposites(getCompositionsList(norm16), unicodeSet);
            }
        }
        return true;
    }

    public Appendable decompose(CharSequence charSequence, StringBuilder sb) {
        decompose(charSequence, 0, charSequence.length(), sb, charSequence.length());
        return sb;
    }

    public void decompose(CharSequence charSequence, int i, int i2, StringBuilder sb, int i3) {
        if (i3 < 0) {
            i3 = i2 - i;
        }
        sb.setLength(0);
        decompose(charSequence, i, i2, new ReorderingBuffer(this, sb, i3));
    }

    public int decompose(CharSequence charSequence, int i, int i2, ReorderingBuffer reorderingBuffer) {
        int cCFromYesOrMaybe;
        int i3 = this.minDecompNoCP;
        int i4 = i;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        while (true) {
            int fromU16SingleLead = i6;
            int iCharAt = i5;
            int iCharCount = i;
            while (iCharCount != i2) {
                iCharAt = charSequence.charAt(iCharCount);
                if (iCharAt >= i3) {
                    char c = (char) iCharAt;
                    fromU16SingleLead = this.normTrie.getFromU16SingleLead(c);
                    if (!isMostDecompYesAndZeroCC(fromU16SingleLead)) {
                        if (!UTF16.isSurrogate(c)) {
                            break;
                        }
                        if (UTF16Plus.isSurrogateLead(iCharAt)) {
                            int i8 = iCharCount + 1;
                            if (i8 != i2) {
                                char cCharAt = charSequence.charAt(i8);
                                if (Character.isLowSurrogate(cCharAt)) {
                                    iCharAt = Character.toCodePoint(c, cCharAt);
                                }
                            }
                        } else if (i < iCharCount) {
                            char cCharAt2 = charSequence.charAt(iCharCount - 1);
                            if (Character.isHighSurrogate(cCharAt2)) {
                                iCharCount--;
                                iCharAt = Character.toCodePoint(cCharAt2, c);
                            }
                        }
                        fromU16SingleLead = getNorm16(iCharAt);
                        if (!isMostDecompYesAndZeroCC(fromU16SingleLead)) {
                            break;
                        }
                        iCharCount += Character.charCount(iCharAt);
                    }
                }
                iCharCount++;
            }
            if (iCharCount != i) {
                if (reorderingBuffer != null) {
                    reorderingBuffer.flushAndAppendZeroCC(charSequence, i, iCharCount);
                } else {
                    i7 = 0;
                    i4 = iCharCount;
                }
            }
            if (iCharCount != i2) {
                i = Character.charCount(iCharAt) + iCharCount;
                if (reorderingBuffer != null) {
                    decompose(iCharAt, fromU16SingleLead, reorderingBuffer);
                } else {
                    if (!isDecompYes(fromU16SingleLead) || (i7 > (cCFromYesOrMaybe = getCCFromYesOrMaybe(fromU16SingleLead)) && cCFromYesOrMaybe != 0)) {
                        break;
                    }
                    if (cCFromYesOrMaybe <= 1) {
                        i4 = i;
                    }
                    i7 = cCFromYesOrMaybe;
                }
                i5 = iCharAt;
                i6 = fromU16SingleLead;
            } else {
                return iCharCount;
            }
        }
        return i4;
    }

    public void decomposeAndAppend(CharSequence charSequence, boolean z, ReorderingBuffer reorderingBuffer) {
        int i;
        int length = charSequence.length();
        if (length == 0) {
            return;
        }
        int iCharCount = 0;
        if (z) {
            decompose(charSequence, 0, length, reorderingBuffer);
            return;
        }
        int iCodePointAt = Character.codePointAt(charSequence, 0);
        int cc = getCC(getNorm16(iCodePointAt));
        int iCodePointAt2 = iCodePointAt;
        int cc2 = cc;
        int i2 = cc2;
        while (true) {
            if (cc2 != 0) {
                iCharCount += Character.charCount(iCodePointAt2);
                if (iCharCount < length) {
                    iCodePointAt2 = Character.codePointAt(charSequence, iCharCount);
                    i2 = cc2;
                    cc2 = getCC(getNorm16(iCodePointAt2));
                } else {
                    i = cc2;
                    break;
                }
            } else {
                i = i2;
                break;
            }
        }
        reorderingBuffer.append(charSequence, 0, iCharCount, cc, i);
        reorderingBuffer.append(charSequence, iCharCount, length);
    }

    public boolean compose(CharSequence charSequence, int i, int i2, boolean z, boolean z2, ReorderingBuffer reorderingBuffer) {
        int i3;
        int iCodePointBefore;
        int iCharAt;
        int i4 = this.minCompNoMaybeCP;
        int iCharCount = i;
        while (true) {
            int iDecomposeShort = iCharCount;
            while (iCharCount != i2) {
                int iCharAt2 = charSequence.charAt(iCharCount);
                if (iCharAt2 >= i4) {
                    char c = (char) iCharAt2;
                    int fromU16SingleLead = this.normTrie.getFromU16SingleLead(c);
                    if (!isCompYesAndZeroCC(fromU16SingleLead)) {
                        int iCharCount2 = iCharCount + 1;
                        if (UTF16.isSurrogate(c)) {
                            if (UTF16Plus.isSurrogateLead(iCharAt2)) {
                                if (iCharCount2 != i2) {
                                    char cCharAt = charSequence.charAt(iCharCount2);
                                    if (Character.isLowSurrogate(cCharAt)) {
                                        iCharCount2++;
                                        iCharAt2 = Character.toCodePoint(c, cCharAt);
                                    }
                                }
                            } else if (iDecomposeShort < iCharCount) {
                                char cCharAt2 = charSequence.charAt(iCharCount - 1);
                                if (Character.isHighSurrogate(cCharAt2)) {
                                    iCharCount--;
                                    iCharAt2 = Character.toCodePoint(cCharAt2, c);
                                }
                            }
                            int i5 = iCharCount;
                            iCharCount = iCharCount2;
                            int norm16 = getNorm16(iCharAt2);
                            if (isCompYesAndZeroCC(norm16)) {
                                continue;
                            } else {
                                iCharCount2 = iCharCount;
                                iCharCount = i5;
                                fromU16SingleLead = norm16;
                            }
                        }
                        if (isMaybeOrNonZeroCC(fromU16SingleLead)) {
                            if (isJamoVT(fromU16SingleLead) && iDecomposeShort != iCharCount) {
                                char cCharAt3 = charSequence.charAt(iCharCount - 1);
                                if (iCharAt2 < 4519) {
                                    char c2 = (char) (cCharAt3 - 4352);
                                    if (c2 < 19) {
                                        if (!z2) {
                                            return false;
                                        }
                                        if (iCharCount2 == i2 || (iCharAt = charSequence.charAt(iCharCount2) - Hangul.JAMO_T_BASE) <= 0 || iCharAt >= 28) {
                                            iCharAt = hasCompBoundaryBefore(charSequence, iCharCount2, i2) ? 0 : -1;
                                        } else {
                                            iCharCount2++;
                                        }
                                        if (iCharAt >= 0) {
                                            int i6 = Hangul.HANGUL_BASE + (((c2 * 21) + (iCharAt2 - 4449)) * 28) + iCharAt;
                                            int i7 = iCharCount - 1;
                                            if (iDecomposeShort != i7) {
                                                reorderingBuffer.append(charSequence, iDecomposeShort, i7);
                                            }
                                            reorderingBuffer.append((char) i6);
                                            iCharCount = iCharCount2;
                                        }
                                    }
                                } else if (Hangul.isHangulLV(cCharAt3)) {
                                    if (!z2) {
                                        return false;
                                    }
                                    int i8 = (cCharAt3 + iCharAt2) - Hangul.JAMO_T_BASE;
                                    int i9 = iCharCount - 1;
                                    if (iDecomposeShort != i9) {
                                        reorderingBuffer.append(charSequence, iDecomposeShort, i9);
                                    }
                                    reorderingBuffer.append((char) i8);
                                    iCharCount = iCharCount2;
                                }
                            } else if (fromU16SingleLead > 65024) {
                                int cCFromNormalYesOrMaybe = getCCFromNormalYesOrMaybe(fromU16SingleLead);
                                if (!z || getPreviousTrailCC(charSequence, iDecomposeShort, iCharCount) <= cCFromNormalYesOrMaybe) {
                                    while (iCharCount2 != i2) {
                                        int iCodePointAt = Character.codePointAt(charSequence, iCharCount2);
                                        int i10 = this.normTrie.get(iCodePointAt);
                                        if (i10 >= 65026) {
                                            int cCFromNormalYesOrMaybe2 = getCCFromNormalYesOrMaybe(i10);
                                            if (cCFromNormalYesOrMaybe <= cCFromNormalYesOrMaybe2) {
                                                iCharCount2 += Character.charCount(iCodePointAt);
                                                cCFromNormalYesOrMaybe = cCFromNormalYesOrMaybe2;
                                            } else if (!z2) {
                                                return false;
                                            }
                                        }
                                        if (norm16HasCompBoundaryBefore(i10)) {
                                            iCharCount = isCompYesAndZeroCC(i10) ? Character.charCount(iCodePointAt) + iCharCount2 : iCharCount2;
                                        }
                                    }
                                    if (z2) {
                                        reorderingBuffer.append(charSequence, iDecomposeShort, i2);
                                    }
                                    return true;
                                }
                                if (!z2) {
                                    return false;
                                }
                            }
                            int i11 = iCharCount2;
                            if (iDecomposeShort != iCharCount) {
                            }
                            i3 = iCharCount;
                            if (z2) {
                            }
                            int length = reorderingBuffer.length();
                            int i12 = i4;
                            decomposeShort(charSequence, i3, i11, false, z, reorderingBuffer);
                            iDecomposeShort = decomposeShort(charSequence, i11, i2, true, z, reorderingBuffer);
                            recompose(reorderingBuffer, length, z);
                            if (!z2) {
                            }
                            iCharCount = iDecomposeShort;
                            i4 = i12;
                        } else {
                            if (!z2) {
                                return false;
                            }
                            if (isDecompNoAlgorithmic(fromU16SingleLead)) {
                                if (norm16HasCompBoundaryAfter(fromU16SingleLead, z) || hasCompBoundaryBefore(charSequence, iCharCount2, i2)) {
                                    if (iDecomposeShort != iCharCount) {
                                        reorderingBuffer.append(charSequence, iDecomposeShort, iCharCount);
                                    }
                                    reorderingBuffer.append(mapAlgorithmic(iCharAt2, fromU16SingleLead), 0);
                                    iCharCount = iCharCount2;
                                }
                                int i112 = iCharCount2;
                                if (iDecomposeShort != iCharCount && !norm16HasCompBoundaryBefore(fromU16SingleLead)) {
                                    iCodePointBefore = Character.codePointBefore(charSequence, iCharCount);
                                    if (!norm16HasCompBoundaryAfter(this.normTrie.get(iCodePointBefore), z)) {
                                        iCharCount -= Character.charCount(iCodePointBefore);
                                    }
                                }
                                i3 = iCharCount;
                                if (z2 && iDecomposeShort != i3) {
                                    reorderingBuffer.append(charSequence, iDecomposeShort, i3);
                                }
                                int length2 = reorderingBuffer.length();
                                int i122 = i4;
                                decomposeShort(charSequence, i3, i112, false, z, reorderingBuffer);
                                iDecomposeShort = decomposeShort(charSequence, i112, i2, true, z, reorderingBuffer);
                                recompose(reorderingBuffer, length2, z);
                                if (!z2) {
                                    if (!reorderingBuffer.equals(charSequence, i3, iDecomposeShort)) {
                                        return false;
                                    }
                                    reorderingBuffer.remove();
                                }
                                iCharCount = iDecomposeShort;
                                i4 = i122;
                            } else if (fromU16SingleLead < this.minNoNoCompBoundaryBefore) {
                                if (norm16HasCompBoundaryAfter(fromU16SingleLead, z) || hasCompBoundaryBefore(charSequence, iCharCount2, i2)) {
                                    if (iDecomposeShort != iCharCount) {
                                        reorderingBuffer.append(charSequence, iDecomposeShort, iCharCount);
                                    }
                                    int i13 = fromU16SingleLead >> 1;
                                    int i14 = i13 + 1;
                                    reorderingBuffer.append((CharSequence) this.extraData, i14, (this.extraData.charAt(i13) & 31) + i14);
                                    iCharCount = iCharCount2;
                                }
                                int i1122 = iCharCount2;
                                if (iDecomposeShort != iCharCount) {
                                    iCodePointBefore = Character.codePointBefore(charSequence, iCharCount);
                                    if (!norm16HasCompBoundaryAfter(this.normTrie.get(iCodePointBefore), z)) {
                                    }
                                }
                                i3 = iCharCount;
                                if (z2) {
                                    reorderingBuffer.append(charSequence, iDecomposeShort, i3);
                                }
                                int length22 = reorderingBuffer.length();
                                int i1222 = i4;
                                decomposeShort(charSequence, i3, i1122, false, z, reorderingBuffer);
                                iDecomposeShort = decomposeShort(charSequence, i1122, i2, true, z, reorderingBuffer);
                                recompose(reorderingBuffer, length22, z);
                                if (!z2) {
                                }
                                iCharCount = iDecomposeShort;
                                i4 = i1222;
                            } else {
                                if (fromU16SingleLead >= this.minNoNoEmpty && (hasCompBoundaryBefore(charSequence, iCharCount2, i2) || hasCompBoundaryAfter(charSequence, iDecomposeShort, iCharCount, z))) {
                                    if (iDecomposeShort != iCharCount) {
                                        reorderingBuffer.append(charSequence, iDecomposeShort, iCharCount);
                                    }
                                    iCharCount = iCharCount2;
                                }
                                int i11222 = iCharCount2;
                                if (iDecomposeShort != iCharCount) {
                                }
                                i3 = iCharCount;
                                if (z2) {
                                }
                                int length222 = reorderingBuffer.length();
                                int i12222 = i4;
                                decomposeShort(charSequence, i3, i11222, false, z, reorderingBuffer);
                                iDecomposeShort = decomposeShort(charSequence, i11222, i2, true, z, reorderingBuffer);
                                recompose(reorderingBuffer, length222, z);
                                if (!z2) {
                                }
                                iCharCount = iDecomposeShort;
                                i4 = i12222;
                            }
                        }
                    }
                }
                iCharCount++;
                i4 = i4;
            }
            if (iDecomposeShort != i2 && z2) {
                reorderingBuffer.append(charSequence, iDecomposeShort, i2);
            }
            return true;
        }
    }

    public int composeQuickCheck(CharSequence charSequence, int i, int i2, boolean z, boolean z2) {
        int norm16;
        int norm162;
        int iCodePointAt;
        int norm163;
        int cCFromYesOrMaybe;
        int i3 = this.minCompNoMaybeCP;
        int i4 = 0;
        int iCharCount = i;
        while (i != i2) {
            int iCharAt = charSequence.charAt(i);
            if (iCharAt >= i3) {
                char c = (char) iCharAt;
                int fromU16SingleLead = this.normTrie.getFromU16SingleLead(c);
                if (!isCompYesAndZeroCC(fromU16SingleLead)) {
                    int iCharCount2 = i + 1;
                    if (UTF16.isSurrogate(c)) {
                        if (UTF16Plus.isSurrogateLead(iCharAt)) {
                            if (iCharCount2 != i2) {
                                char cCharAt = charSequence.charAt(iCharCount2);
                                if (Character.isLowSurrogate(cCharAt)) {
                                    iCharCount2++;
                                    iCharAt = Character.toCodePoint(c, cCharAt);
                                }
                            }
                        } else if (iCharCount < i) {
                            char cCharAt2 = charSequence.charAt(i - 1);
                            if (Character.isHighSurrogate(cCharAt2)) {
                                i--;
                                iCharAt = Character.toCodePoint(cCharAt2, c);
                            }
                        }
                        int i5 = i;
                        i = iCharCount2;
                        norm16 = getNorm16(iCharAt);
                        if (isCompYesAndZeroCC(norm16)) {
                            continue;
                        } else {
                            iCharCount2 = i;
                            i = i5;
                        }
                    } else {
                        norm16 = fromU16SingleLead;
                    }
                    if (iCharCount == i) {
                        norm162 = 1;
                        if (isMaybeOrNonZeroCC(norm16)) {
                            int cCFromYesOrMaybe2 = getCCFromYesOrMaybe(norm16);
                            if (!z || cCFromYesOrMaybe2 == 0 || getTrailCCFromCompYesAndZeroCC(norm162) <= cCFromYesOrMaybe2) {
                                while (true) {
                                    if (norm16 < 65026) {
                                        if (z2) {
                                            return iCharCount << 1;
                                        }
                                        i4 = 1;
                                    }
                                    if (iCharCount2 == i2) {
                                        return (iCharCount2 << 1) | i4;
                                    }
                                    iCodePointAt = Character.codePointAt(charSequence, iCharCount2);
                                    norm163 = getNorm16(iCodePointAt);
                                    if (!isMaybeOrNonZeroCC(norm163) || (cCFromYesOrMaybe2 > (cCFromYesOrMaybe = getCCFromYesOrMaybe(norm163)) && cCFromYesOrMaybe != 0)) {
                                        break;
                                    }
                                    iCharCount2 += Character.charCount(iCodePointAt);
                                    norm16 = norm163;
                                    cCFromYesOrMaybe2 = cCFromYesOrMaybe;
                                }
                                if (isCompYesAndZeroCC(norm163)) {
                                    i = Character.charCount(iCodePointAt) + iCharCount2;
                                    iCharCount = iCharCount2;
                                }
                            }
                        }
                        return iCharCount << 1;
                    }
                    if (!norm16HasCompBoundaryBefore(norm16)) {
                        int iCodePointBefore = Character.codePointBefore(charSequence, i);
                        norm162 = getNorm16(iCodePointBefore);
                        if (!norm16HasCompBoundaryAfter(norm162, z)) {
                            iCharCount = i - Character.charCount(iCodePointBefore);
                            if (isMaybeOrNonZeroCC(norm16)) {
                            }
                            return iCharCount << 1;
                        }
                    }
                    iCharCount = i;
                    norm162 = 1;
                    if (isMaybeOrNonZeroCC(norm16)) {
                    }
                    return iCharCount << 1;
                }
            }
            i++;
        }
        return (i << 1) | i4;
    }

    public void composeAndAppend(CharSequence charSequence, boolean z, boolean z2, ReorderingBuffer reorderingBuffer) {
        int i;
        int iFindNextCompBoundary;
        int length = charSequence.length();
        if (reorderingBuffer.isEmpty() || (iFindNextCompBoundary = findNextCompBoundary(charSequence, 0, length, z2)) == 0) {
            i = 0;
        } else {
            int iFindPreviousCompBoundary = findPreviousCompBoundary(reorderingBuffer.getStringBuilder(), reorderingBuffer.length(), z2);
            StringBuilder sb = new StringBuilder((reorderingBuffer.length() - iFindPreviousCompBoundary) + iFindNextCompBoundary + 16);
            sb.append((CharSequence) reorderingBuffer.getStringBuilder(), iFindPreviousCompBoundary, reorderingBuffer.length());
            reorderingBuffer.removeSuffix(reorderingBuffer.length() - iFindPreviousCompBoundary);
            sb.append(charSequence, 0, iFindNextCompBoundary);
            compose(sb, 0, sb.length(), z2, true, reorderingBuffer);
            i = iFindNextCompBoundary;
        }
        if (z) {
            compose(charSequence, i, length, z2, true, reorderingBuffer);
        } else {
            reorderingBuffer.append(charSequence, i, length);
        }
    }

    public int makeFCD(CharSequence charSequence, int i, int i2, ReorderingBuffer reorderingBuffer) {
        int fCD16FromNormData;
        int i3;
        int i4 = i;
        int i5 = i4;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        while (true) {
            int fCD16FromNormData2 = i8;
            int iCharAt = i7;
            int fCD16FromNormData3 = i6;
            int iCharCount = i4;
            while (iCharCount != i2) {
                iCharAt = charSequence.charAt(iCharCount);
                if (iCharAt >= this.minLcccCP) {
                    if (singleLeadMightHaveNonZeroFCD16(iCharAt)) {
                        char c = (char) iCharAt;
                        if (UTF16.isSurrogate(c)) {
                            if (UTF16Plus.isSurrogateLead(iCharAt)) {
                                int i9 = iCharCount + 1;
                                if (i9 != i2) {
                                    char cCharAt = charSequence.charAt(i9);
                                    if (Character.isLowSurrogate(cCharAt)) {
                                        iCharAt = Character.toCodePoint(c, cCharAt);
                                    }
                                }
                            } else if (i4 < iCharCount) {
                                char cCharAt2 = charSequence.charAt(iCharCount - 1);
                                if (Character.isHighSurrogate(cCharAt2)) {
                                    iCharCount--;
                                    iCharAt = Character.toCodePoint(cCharAt2, c);
                                }
                            }
                        }
                        fCD16FromNormData2 = getFCD16FromNormData(iCharAt);
                        if (fCD16FromNormData2 > 255) {
                            break;
                        }
                        iCharCount += Character.charCount(iCharAt);
                        fCD16FromNormData3 = fCD16FromNormData2;
                    } else {
                        iCharCount++;
                        fCD16FromNormData3 = 0;
                    }
                } else {
                    fCD16FromNormData3 = ~iCharAt;
                    iCharCount++;
                }
            }
            int i10 = iCharAt;
            int i11 = fCD16FromNormData2;
            if (iCharCount == i4) {
                if (iCharCount == i2) {
                    break;
                }
                int iCharCount2 = iCharCount + Character.charCount(i10);
                if ((fCD16FromNormData3 & 255) > (i11 >> 8)) {
                }
            } else if (iCharCount != i2) {
                if (fCD16FromNormData3 < 0) {
                    int i12 = ~fCD16FromNormData3;
                    if (i12 < this.minDecompNoCP) {
                        i3 = iCharCount;
                        fCD16FromNormData = 0;
                    } else {
                        fCD16FromNormData = getFCD16FromNormData(i12);
                        i3 = fCD16FromNormData > 1 ? iCharCount - 1 : iCharCount;
                    }
                    int i13 = i3;
                    fCD16FromNormData3 = fCD16FromNormData;
                    i5 = i13;
                } else {
                    i5 = iCharCount - 1;
                    if (Character.isLowSurrogate(charSequence.charAt(i5)) && i4 < i5 && Character.isHighSurrogate(charSequence.charAt(i5 - 1))) {
                        i5--;
                        fCD16FromNormData3 = getFCD16FromNormData(Character.toCodePoint(charSequence.charAt(i5), charSequence.charAt(i5 + 1)));
                    }
                    if (fCD16FromNormData3 <= 1) {
                        i5 = iCharCount;
                    }
                }
                if (reorderingBuffer != null) {
                    reorderingBuffer.flushAndAppendZeroCC(charSequence, i4, i5);
                    reorderingBuffer.append(charSequence, i5, iCharCount);
                }
                i4 = iCharCount;
                int iCharCount22 = iCharCount + Character.charCount(i10);
                if ((fCD16FromNormData3 & 255) > (i11 >> 8)) {
                    if ((i11 & 255) <= 1) {
                        i5 = iCharCount22;
                    }
                    if (reorderingBuffer != null) {
                        reorderingBuffer.appendZeroCC(i10);
                    }
                    i4 = iCharCount22;
                    i7 = i10;
                    i6 = i11;
                    i8 = i6;
                } else {
                    if (reorderingBuffer == null) {
                        return i5;
                    }
                    reorderingBuffer.removeSuffix(i4 - i5);
                    int iFindNextFCDBoundary = findNextFCDBoundary(charSequence, iCharCount22, i2);
                    decomposeShort(charSequence, i5, iFindNextFCDBoundary, false, false, reorderingBuffer);
                    i6 = 0;
                    i7 = i10;
                    i8 = i11;
                    i4 = iFindNextFCDBoundary;
                    i5 = i4;
                }
            } else if (reorderingBuffer != null) {
                reorderingBuffer.flushAndAppendZeroCC(charSequence, i4, iCharCount);
            }
        }
    }

    public void makeFCDAndAppend(CharSequence charSequence, boolean z, ReorderingBuffer reorderingBuffer) {
        int iFindNextFCDBoundary;
        int length = charSequence.length();
        if (!reorderingBuffer.isEmpty() && (iFindNextFCDBoundary = findNextFCDBoundary(charSequence, 0, length)) != 0) {
            int iFindPreviousFCDBoundary = findPreviousFCDBoundary(reorderingBuffer.getStringBuilder(), reorderingBuffer.length());
            StringBuilder sb = new StringBuilder((reorderingBuffer.length() - iFindPreviousFCDBoundary) + iFindNextFCDBoundary + 16);
            sb.append((CharSequence) reorderingBuffer.getStringBuilder(), iFindPreviousFCDBoundary, reorderingBuffer.length());
            reorderingBuffer.removeSuffix(reorderingBuffer.length() - iFindPreviousFCDBoundary);
            sb.append(charSequence, 0, iFindNextFCDBoundary);
            makeFCD(sb, 0, sb.length(), reorderingBuffer);
        } else {
            iFindNextFCDBoundary = 0;
        }
        if (z) {
            makeFCD(charSequence, iFindNextFCDBoundary, length, reorderingBuffer);
        } else {
            reorderingBuffer.append(charSequence, iFindNextFCDBoundary, length);
        }
    }

    public boolean hasDecompBoundaryBefore(int i) {
        return i < this.minLcccCP || (i <= 65535 && !singleLeadMightHaveNonZeroFCD16(i)) || norm16HasDecompBoundaryBefore(getNorm16(i));
    }

    public boolean norm16HasDecompBoundaryBefore(int i) {
        if (i < this.minNoNoCompNoMaybeCC) {
            return true;
        }
        if (i >= this.limitNoNo) {
            return i <= 64512 || i == 65024;
        }
        int i2 = i >> 1;
        return (this.extraData.charAt(i2) & 128) == 0 || (this.extraData.charAt(i2 - 1) & 65280) == 0;
    }

    public boolean hasDecompBoundaryAfter(int i) {
        if (i < this.minDecompNoCP) {
            return true;
        }
        if (i > 65535 || singleLeadMightHaveNonZeroFCD16(i)) {
            return norm16HasDecompBoundaryAfter(getNorm16(i));
        }
        return true;
    }

    public boolean norm16HasDecompBoundaryAfter(int i) {
        if (i <= this.minYesNo || isHangulLVT(i)) {
            return true;
        }
        if (i >= this.limitNoNo) {
            return isMaybeOrNonZeroCC(i) ? i <= 64512 || i == 65024 : (i & 6) <= 2;
        }
        int i2 = i >> 1;
        char cCharAt = this.extraData.charAt(i2);
        if (cCharAt > 511) {
            return false;
        }
        return cCharAt <= 255 || (cCharAt & 128) == 0 || (this.extraData.charAt(i2 - 1) & 65280) == 0;
    }

    public boolean isDecompInert(int i) {
        return isDecompYesAndZeroCC(getNorm16(i));
    }

    public boolean hasCompBoundaryBefore(int i) {
        return i < this.minCompNoMaybeCP || norm16HasCompBoundaryBefore(getNorm16(i));
    }

    public boolean hasCompBoundaryAfter(int i, boolean z) {
        return norm16HasCompBoundaryAfter(getNorm16(i), z);
    }

    public boolean isCompInert(int i, boolean z) {
        int norm16 = getNorm16(i);
        return isCompYesAndZeroCC(norm16) && (norm16 & 1) != 0 && (!z || isInert(norm16) || this.extraData.charAt(norm16 >> 1) <= 511);
    }

    public boolean hasFCDBoundaryBefore(int i) {
        return hasDecompBoundaryBefore(i);
    }

    public boolean hasFCDBoundaryAfter(int i) {
        return hasDecompBoundaryAfter(i);
    }

    public boolean isFCDInert(int i) {
        return getFCD16(i) <= 1;
    }

    private boolean isMaybe(int i) {
        return this.minMaybeYes <= i && i <= 65024;
    }

    private boolean isMaybeOrNonZeroCC(int i) {
        return i >= this.minMaybeYes;
    }

    private static boolean isInert(int i) {
        return i == 1;
    }

    private static boolean isJamoL(int i) {
        return i == 2;
    }

    private static boolean isJamoVT(int i) {
        return i == 65024;
    }

    private int hangulLVT() {
        return this.minYesNoMappingsOnly | 1;
    }

    private boolean isHangulLV(int i) {
        return i == this.minYesNo;
    }

    private boolean isHangulLVT(int i) {
        return i == hangulLVT();
    }

    private boolean isCompYesAndZeroCC(int i) {
        return i < this.minNoNo;
    }

    private boolean isDecompYesAndZeroCC(int i) {
        return i < this.minYesNo || i == 65024 || (this.minMaybeYes <= i && i <= 64512);
    }

    private boolean isMostDecompYesAndZeroCC(int i) {
        return i < this.minYesNo || i == 64512 || i == 65024;
    }

    private boolean isDecompNoAlgorithmic(int i) {
        return i >= this.limitNoNo;
    }

    private int getCCFromNoNo(int i) {
        int i2 = i >> 1;
        if ((this.extraData.charAt(i2) & 128) != 0) {
            return this.extraData.charAt(i2 - 1) & 255;
        }
        return 0;
    }

    int getTrailCCFromCompYesAndZeroCC(int i) {
        if (i <= this.minYesNo) {
            return 0;
        }
        return this.extraData.charAt(i >> 1) >> '\b';
    }

    private int mapAlgorithmic(int i, int i2) {
        return (i + (i2 >> 3)) - this.centerNoNoDelta;
    }

    private int getCompositionsListForDecompYes(int i) {
        if (i < 2 || 64512 <= i) {
            return -1;
        }
        int i2 = i - this.minMaybeYes;
        if (i2 < 0) {
            i2 += MIN_NORMAL_MAYBE_YES;
        }
        return i2 >> 1;
    }

    private int getCompositionsListForComposite(int i) {
        int i2 = ((MIN_NORMAL_MAYBE_YES - this.minMaybeYes) + i) >> 1;
        return i2 + 1 + (this.maybeYesCompositions.charAt(i2) & 31);
    }

    private int getCompositionsListForMaybe(int i) {
        return (i - this.minMaybeYes) >> 1;
    }

    private int getCompositionsList(int i) {
        if (isDecompYes(i)) {
            return getCompositionsListForDecompYes(i);
        }
        return getCompositionsListForComposite(i);
    }

    private int decomposeShort(CharSequence charSequence, int i, int i2, boolean z, boolean z2, ReorderingBuffer reorderingBuffer) {
        while (i < i2) {
            int iCodePointAt = Character.codePointAt(charSequence, i);
            if (z && iCodePointAt < this.minCompNoMaybeCP) {
                return i;
            }
            int norm16 = getNorm16(iCodePointAt);
            if (z && norm16HasCompBoundaryBefore(norm16)) {
                return i;
            }
            i += Character.charCount(iCodePointAt);
            decompose(iCodePointAt, norm16, reorderingBuffer);
            if (z && norm16HasCompBoundaryAfter(norm16, z2)) {
                return i;
            }
        }
        return i;
    }

    private void decompose(int i, int i2, ReorderingBuffer reorderingBuffer) {
        if (i2 >= this.limitNoNo) {
            if (isMaybeOrNonZeroCC(i2)) {
                reorderingBuffer.append(i, getCCFromYesOrMaybe(i2));
                return;
            } else {
                i = mapAlgorithmic(i, i2);
                i2 = getNorm16(i);
            }
        }
        if (i2 < this.minYesNo) {
            reorderingBuffer.append(i, 0);
            return;
        }
        if (isHangulLV(i2) || isHangulLVT(i2)) {
            Hangul.decompose(i, reorderingBuffer);
            return;
        }
        int i3 = i2 >> 1;
        char cCharAt = this.extraData.charAt(i3);
        int i4 = i3 + 1;
        reorderingBuffer.append(this.extraData, i4, i4 + (cCharAt & 31), (cCharAt & 128) != 0 ? this.extraData.charAt(i3 - 1) >> '\b' : 0, cCharAt >> '\b');
    }

    private static int combine(String str, int i, int i2) {
        char cCharAt;
        if (i2 >= 13312) {
            int i3 = COMP_1_TRAIL_LIMIT + ((i2 >> 9) & (-2));
            int i4 = (i2 << 6) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
            while (true) {
                char cCharAt2 = str.charAt(i);
                if (i3 > cCharAt2) {
                    i += (cCharAt2 & 1) + 2;
                } else if (i3 == (cCharAt2 & 32766)) {
                    char cCharAt3 = str.charAt(i + 1);
                    if (i4 > cCharAt3) {
                        if ((cCharAt2 & 32768) == 0) {
                            i += 3;
                        } else {
                            return -1;
                        }
                    } else {
                        if (i4 == (65472 & cCharAt3)) {
                            return str.charAt(i + 2) | (('?' & cCharAt3) << 16);
                        }
                        return -1;
                    }
                } else {
                    return -1;
                }
            }
        } else {
            int i5 = i2 << 1;
            while (true) {
                cCharAt = str.charAt(i);
                if (i5 <= cCharAt) {
                    break;
                }
                i += (cCharAt & 1) + 2;
            }
            if (i5 == (cCharAt & 32766)) {
                if ((cCharAt & 1) != 0) {
                    return str.charAt(i + 2) | (str.charAt(i + 1) << 16);
                }
                return str.charAt(i + 1);
            }
            return -1;
        }
    }

    private void addComposites(int i, UnicodeSet unicodeSet) {
        char cCharAt;
        int iCharAt;
        do {
            cCharAt = this.maybeYesCompositions.charAt(i);
            if ((cCharAt & 1) == 0) {
                iCharAt = this.maybeYesCompositions.charAt(i + 1);
                i += 2;
            } else {
                iCharAt = ((this.maybeYesCompositions.charAt(i + 1) & '?') << 16) | this.maybeYesCompositions.charAt(i + 2);
                i += 3;
            }
            int i2 = iCharAt >> 1;
            if ((iCharAt & 1) != 0) {
                addComposites(getCompositionsListForComposite(getNorm16(i2)), unicodeSet);
            }
            unicodeSet.add(i2);
        } while ((cCharAt & 32768) == 0);
    }

    private void recompose(ReorderingBuffer reorderingBuffer, int i, boolean z) {
        char cCharAt;
        char cCharAt2;
        StringBuilder stringBuilder = reorderingBuffer.getStringBuilder();
        int iCharCount = i;
        if (iCharCount == stringBuilder.length()) {
            return;
        }
        int i2 = 0;
        boolean z2 = false;
        int compositionsListForDecompYes = -1;
        int i3 = -1;
        while (true) {
            int iCodePointAt = stringBuilder.codePointAt(iCharCount);
            iCharCount += Character.charCount(iCodePointAt);
            int norm16 = getNorm16(iCodePointAt);
            int cCFromYesOrMaybe = getCCFromYesOrMaybe(norm16);
            if (isMaybe(norm16) && compositionsListForDecompYes >= 0 && (i2 < cCFromYesOrMaybe || i2 == 0)) {
                if (isJamoVT(norm16)) {
                    if (iCodePointAt < 4519 && (cCharAt = (char) (stringBuilder.charAt(i3) - 4352)) < 19) {
                        int i4 = iCharCount - 1;
                        char c = (char) (Hangul.HANGUL_BASE + (((cCharAt * 21) + (iCodePointAt - 4449)) * 28));
                        if (iCharCount != stringBuilder.length() && (cCharAt2 = (char) (stringBuilder.charAt(iCharCount) - Hangul.JAMO_T_BASE)) < 28) {
                            iCharCount++;
                            c = (char) (c + cCharAt2);
                        }
                        stringBuilder.setCharAt(i3, c);
                        stringBuilder.delete(i4, iCharCount);
                        iCharCount = i4;
                    }
                    if (iCharCount == stringBuilder.length()) {
                        break;
                    }
                } else {
                    int iCombine = combine(this.maybeYesCompositions, compositionsListForDecompYes, iCodePointAt);
                    if (iCombine >= 0) {
                        int i5 = iCombine >> 1;
                        int iCharCount2 = iCharCount - Character.charCount(iCodePointAt);
                        stringBuilder.delete(iCharCount2, iCharCount);
                        if (z2) {
                            if (i5 > 65535) {
                                stringBuilder.setCharAt(i3, UTF16.getLeadSurrogate(i5));
                                stringBuilder.setCharAt(i3 + 1, UTF16.getTrailSurrogate(i5));
                            } else {
                                stringBuilder.setCharAt(i3, (char) iCodePointAt);
                                stringBuilder.deleteCharAt(i3 + 1);
                                iCharCount2--;
                                z2 = false;
                            }
                        } else if (i5 > 65535) {
                            stringBuilder.setCharAt(i3, UTF16.getLeadSurrogate(i5));
                            stringBuilder.insert(i3 + 1, UTF16.getTrailSurrogate(i5));
                            iCharCount2++;
                            z2 = true;
                        } else {
                            stringBuilder.setCharAt(i3, (char) i5);
                        }
                        iCharCount = iCharCount2;
                        if (iCharCount == stringBuilder.length()) {
                            break;
                        } else {
                            compositionsListForDecompYes = (iCombine & 1) != 0 ? getCompositionsListForComposite(getNorm16(i5)) : -1;
                        }
                    }
                }
            }
            if (iCharCount == stringBuilder.length()) {
                break;
            }
            if (cCFromYesOrMaybe == 0) {
                compositionsListForDecompYes = getCompositionsListForDecompYes(norm16);
                if (compositionsListForDecompYes >= 0) {
                    if (iCodePointAt <= 65535) {
                        i3 = iCharCount - 1;
                        z2 = false;
                    } else {
                        i3 = iCharCount - 2;
                        z2 = true;
                    }
                }
            } else if (z) {
                compositionsListForDecompYes = -1;
            }
            i2 = cCFromYesOrMaybe;
        }
        reorderingBuffer.flush();
    }

    public int composePair(int i, int i2) {
        int compositionsListForMaybe;
        int norm16 = getNorm16(i);
        if (isInert(norm16)) {
            return -1;
        }
        if (norm16 < this.minYesNoMappingsOnly) {
            if (isJamoL(norm16)) {
                int i3 = i2 - 4449;
                if (i3 < 0 || i3 >= 21) {
                    return -1;
                }
                return Hangul.HANGUL_BASE + ((((i - 4352) * 21) + i3) * 28);
            }
            if (isHangulLV(norm16)) {
                int i4 = i2 - 4519;
                if (i4 <= 0 || i4 >= 28) {
                    return -1;
                }
                return i + i4;
            }
            compositionsListForMaybe = ((MIN_NORMAL_MAYBE_YES - this.minMaybeYes) + norm16) >> 1;
            if (norm16 > this.minYesNo) {
                compositionsListForMaybe += (this.maybeYesCompositions.charAt(compositionsListForMaybe) & 31) + 1;
            }
        } else {
            if (norm16 < this.minMaybeYes || 64512 <= norm16) {
                return -1;
            }
            compositionsListForMaybe = getCompositionsListForMaybe(norm16);
        }
        if (i2 < 0 || 1114111 < i2) {
            return -1;
        }
        return combine(this.maybeYesCompositions, compositionsListForMaybe, i2) >> 1;
    }

    private boolean hasCompBoundaryBefore(int i, int i2) {
        return i < this.minCompNoMaybeCP || norm16HasCompBoundaryBefore(i2);
    }

    private boolean norm16HasCompBoundaryBefore(int i) {
        return i < this.minNoNoCompNoMaybeCC || isAlgorithmicNoNo(i);
    }

    private boolean hasCompBoundaryBefore(CharSequence charSequence, int i, int i2) {
        return i == i2 || hasCompBoundaryBefore(Character.codePointAt(charSequence, i));
    }

    private boolean norm16HasCompBoundaryAfter(int i, boolean z) {
        if ((i & 1) != 0 && (!z || isTrailCC01ForCompBoundaryAfter(i))) {
            return true;
        }
        return false;
    }

    private boolean hasCompBoundaryAfter(CharSequence charSequence, int i, int i2, boolean z) {
        return i == i2 || hasCompBoundaryAfter(Character.codePointBefore(charSequence, i2), z);
    }

    private boolean isTrailCC01ForCompBoundaryAfter(int i) {
        if (isInert(i)) {
            return true;
        }
        if (isDecompNoAlgorithmic(i)) {
            if ((i & 6) <= 2) {
                return true;
            }
        } else if (this.extraData.charAt(i >> 1) <= 511) {
            return true;
        }
        return false;
    }

    private int findPreviousCompBoundary(CharSequence charSequence, int i, boolean z) {
        while (i > 0) {
            int iCodePointBefore = Character.codePointBefore(charSequence, i);
            int norm16 = getNorm16(iCodePointBefore);
            if (norm16HasCompBoundaryAfter(norm16, z)) {
                break;
            }
            i -= Character.charCount(iCodePointBefore);
            if (hasCompBoundaryBefore(iCodePointBefore, norm16)) {
                break;
            }
        }
        return i;
    }

    private int findNextCompBoundary(CharSequence charSequence, int i, int i2, boolean z) {
        while (i < i2) {
            int iCodePointAt = Character.codePointAt(charSequence, i);
            int i3 = this.normTrie.get(iCodePointAt);
            if (hasCompBoundaryBefore(iCodePointAt, i3)) {
                break;
            }
            i += Character.charCount(iCodePointAt);
            if (norm16HasCompBoundaryAfter(i3, z)) {
                break;
            }
        }
        return i;
    }

    private int findPreviousFCDBoundary(CharSequence charSequence, int i) {
        while (i > 0) {
            int iCodePointBefore = Character.codePointBefore(charSequence, i);
            if (iCodePointBefore < this.minDecompNoCP) {
                break;
            }
            int norm16 = getNorm16(iCodePointBefore);
            if (norm16HasDecompBoundaryAfter(norm16)) {
                break;
            }
            i -= Character.charCount(iCodePointBefore);
            if (norm16HasDecompBoundaryBefore(norm16)) {
                break;
            }
        }
        return i;
    }

    private int findNextFCDBoundary(CharSequence charSequence, int i, int i2) {
        while (i < i2) {
            int iCodePointAt = Character.codePointAt(charSequence, i);
            if (iCodePointAt < this.minLcccCP) {
                break;
            }
            int norm16 = getNorm16(iCodePointAt);
            if (norm16HasDecompBoundaryBefore(norm16)) {
                break;
            }
            i += Character.charCount(iCodePointAt);
            if (norm16HasDecompBoundaryAfter(norm16)) {
                break;
            }
        }
        return i;
    }

    private int getPreviousTrailCC(CharSequence charSequence, int i, int i2) {
        if (i == i2) {
            return 0;
        }
        return getFCD16(Character.codePointBefore(charSequence, i2));
    }

    private void addToStartSet(Trie2Writable trie2Writable, int i, int i2) {
        UnicodeSet unicodeSet;
        int i3 = trie2Writable.get(i2);
        if ((4194303 & i3) == 0 && i != 0) {
            trie2Writable.set(i2, i | i3);
            return;
        }
        if ((i3 & 2097152) == 0) {
            int i4 = i3 & 2097151;
            trie2Writable.set(i2, (i3 & (-2097152)) | 2097152 | this.canonStartSets.size());
            ArrayList<UnicodeSet> arrayList = this.canonStartSets;
            unicodeSet = new UnicodeSet();
            arrayList.add(unicodeSet);
            if (i4 != 0) {
                unicodeSet.add(i4);
            }
        } else {
            unicodeSet = this.canonStartSets.get(i3 & 2097151);
        }
        unicodeSet.add(i);
    }
}
