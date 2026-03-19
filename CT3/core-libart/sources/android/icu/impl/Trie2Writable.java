package android.icu.impl;

import android.icu.impl.Trie2;
import dalvik.bytecode.Opcodes;

public class Trie2Writable extends Trie2 {

    private static final int[] f23androidicuimplTrie2$ValueWidthSwitchesValues = null;

    static final boolean f24assertionsDisabled;
    private static final int UNEWTRIE2_DATA_0800_OFFSET = 2176;
    private static final int UNEWTRIE2_DATA_NULL_OFFSET = 192;
    private static final int UNEWTRIE2_DATA_START_OFFSET = 256;
    private static final int UNEWTRIE2_INDEX_2_NULL_OFFSET = 2656;
    private static final int UNEWTRIE2_INDEX_2_START_OFFSET = 2720;
    private static final int UNEWTRIE2_INITIAL_DATA_LENGTH = 16384;
    private static final int UNEWTRIE2_MEDIUM_DATA_LENGTH = 131072;
    private static final int UTRIE2_MAX_DATA_LENGTH = 262140;
    private static final int UTRIE2_MAX_INDEX_LENGTH = 65535;
    private boolean UTRIE2_DEBUG;
    private int[] data;
    private int dataCapacity;
    private int firstFreeBlock;
    private int[] index1;
    private int[] index2;
    private int index2Length;
    private int index2NullOffset;
    private boolean isCompacted;
    private int[] map;

    private static int[] m59getandroidicuimplTrie2$ValueWidthSwitchesValues() {
        if (f23androidicuimplTrie2$ValueWidthSwitchesValues != null) {
            return f23androidicuimplTrie2$ValueWidthSwitchesValues;
        }
        int[] iArr = new int[Trie2.ValueWidth.valuesCustom().length];
        try {
            iArr[Trie2.ValueWidth.BITS_16.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Trie2.ValueWidth.BITS_32.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f23androidicuimplTrie2$ValueWidthSwitchesValues = iArr;
        return iArr;
    }

    static {
        f24assertionsDisabled = !Trie2Writable.class.desiredAssertionStatus();
    }

    public Trie2Writable(int initialValueP, int errorValueP) {
        this.index1 = new int[544];
        this.index2 = new int[35488];
        this.map = new int[34852];
        this.UTRIE2_DEBUG = false;
        init(initialValueP, errorValueP);
    }

    private void init(int initialValueP, int errorValueP) {
        this.initialValue = initialValueP;
        this.errorValue = errorValueP;
        this.highStart = 1114112;
        this.data = new int[16384];
        this.dataCapacity = 16384;
        this.initialValue = initialValueP;
        this.errorValue = errorValueP;
        this.highStart = 1114112;
        this.firstFreeBlock = 0;
        this.isCompacted = false;
        int i = 0;
        while (i < 128) {
            this.data[i] = this.initialValue;
            i++;
        }
        while (i < 192) {
            this.data[i] = this.errorValue;
            i++;
        }
        for (int i2 = 192; i2 < 256; i2++) {
            this.data[i2] = this.initialValue;
        }
        this.dataNullOffset = 192;
        this.dataLength = 256;
        int i3 = 0;
        int j = 0;
        while (j < 128) {
            this.index2[i3] = j;
            this.map[i3] = 1;
            i3++;
            j += 32;
        }
        int i4 = i3;
        while (j < 192) {
            this.map[i4] = 0;
            j += 32;
            i4++;
        }
        int i5 = i4 + 1;
        this.map[i4] = 34845;
        for (int j2 = j + 32; j2 < 256; j2 += 32) {
            this.map[i5] = 0;
            i5++;
        }
        for (int i6 = 4; i6 < 2080; i6++) {
            this.index2[i6] = 192;
        }
        for (int i7 = 0; i7 < 576; i7++) {
            this.index2[i7 + 2080] = -1;
        }
        for (int i8 = 0; i8 < 64; i8++) {
            this.index2[i8 + UNEWTRIE2_INDEX_2_NULL_OFFSET] = 192;
        }
        this.index2NullOffset = UNEWTRIE2_INDEX_2_NULL_OFFSET;
        this.index2Length = UNEWTRIE2_INDEX_2_START_OFFSET;
        int i9 = 0;
        int j3 = 0;
        while (i9 < 32) {
            this.index1[i9] = j3;
            i9++;
            j3 += 64;
        }
        while (i9 < 544) {
            this.index1[i9] = UNEWTRIE2_INDEX_2_NULL_OFFSET;
            i9++;
        }
        for (int i10 = 128; i10 < 2048; i10 += 32) {
            set(i10, this.initialValue);
        }
    }

    public Trie2Writable(Trie2 source) {
        this.index1 = new int[544];
        this.index2 = new int[35488];
        this.map = new int[34852];
        this.UTRIE2_DEBUG = false;
        init(source.initialValue, source.errorValue);
        for (Trie2.Range r : source) {
            setRange(r, true);
        }
    }

    private boolean isInNullBlock(int c, boolean forLSCP) {
        int i2;
        if (Character.isHighSurrogate((char) c) && forLSCP) {
            i2 = (c >> 5) + 320;
        } else {
            i2 = this.index1[c >> 11] + ((c >> 5) & 63);
        }
        int block = this.index2[i2];
        return block == this.dataNullOffset;
    }

    private int allocIndex2Block() {
        int newBlock = this.index2Length;
        int newTop = newBlock + 64;
        if (newTop > this.index2.length) {
            throw new IllegalStateException("Internal error in Trie2 creation.");
        }
        this.index2Length = newTop;
        System.arraycopy(this.index2, this.index2NullOffset, this.index2, newBlock, 64);
        return newBlock;
    }

    private int getIndex2Block(int c, boolean forLSCP) {
        if (c >= 55296 && c < 56320 && forLSCP) {
            return 2048;
        }
        int i1 = c >> 11;
        int i2 = this.index1[i1];
        if (i2 == this.index2NullOffset) {
            int i22 = allocIndex2Block();
            this.index1[i1] = i22;
            return i22;
        }
        return i2;
    }

    private int allocDataBlock(int copyBlock) {
        int newBlock;
        int capacity;
        if (this.firstFreeBlock != 0) {
            newBlock = this.firstFreeBlock;
            this.firstFreeBlock = -this.map[newBlock >> 5];
        } else {
            newBlock = this.dataLength;
            int newTop = newBlock + 32;
            if (newTop > this.dataCapacity) {
                if (this.dataCapacity < 131072) {
                    capacity = 131072;
                } else if (this.dataCapacity < 1115264) {
                    capacity = 1115264;
                } else {
                    throw new IllegalStateException("Internal error in Trie2 creation.");
                }
                int[] newData = new int[capacity];
                System.arraycopy(this.data, 0, newData, 0, this.dataLength);
                this.data = newData;
                this.dataCapacity = capacity;
            }
            this.dataLength = newTop;
        }
        System.arraycopy(this.data, copyBlock, this.data, newBlock, 32);
        this.map[newBlock >> 5] = 0;
        return newBlock;
    }

    private void releaseDataBlock(int block) {
        this.map[block >> 5] = -this.firstFreeBlock;
        this.firstFreeBlock = block;
    }

    private boolean isWritableBlock(int block) {
        return block != this.dataNullOffset && 1 == this.map[block >> 5];
    }

    private void setIndex2Entry(int i2, int block) {
        int[] iArr = this.map;
        int i = block >> 5;
        iArr[i] = iArr[i] + 1;
        int oldBlock = this.index2[i2];
        int[] iArr2 = this.map;
        int i3 = oldBlock >> 5;
        int i4 = iArr2[i3] - 1;
        iArr2[i3] = i4;
        if (i4 == 0) {
            releaseDataBlock(oldBlock);
        }
        this.index2[i2] = block;
    }

    private int getDataBlock(int c, boolean forLSCP) {
        int i2 = getIndex2Block(c, forLSCP) + ((c >> 5) & 63);
        int oldBlock = this.index2[i2];
        if (isWritableBlock(oldBlock)) {
            return oldBlock;
        }
        int newBlock = allocDataBlock(oldBlock);
        setIndex2Entry(i2, newBlock);
        return newBlock;
    }

    public Trie2Writable set(int c, int value) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point.");
        }
        set(c, true, value);
        this.fHash = 0;
        return this;
    }

    private Trie2Writable set(int c, boolean forLSCP, int value) {
        if (this.isCompacted) {
            uncompact();
        }
        int block = getDataBlock(c, forLSCP);
        this.data[(c & 31) + block] = value;
        return this;
    }

    private void uncompact() {
        Trie2Writable tempTrie = new Trie2Writable(this);
        this.index1 = tempTrie.index1;
        this.index2 = tempTrie.index2;
        this.data = tempTrie.data;
        this.index2Length = tempTrie.index2Length;
        this.dataCapacity = tempTrie.dataCapacity;
        this.isCompacted = tempTrie.isCompacted;
        this.header = tempTrie.header;
        this.index = tempTrie.index;
        this.data16 = tempTrie.data16;
        this.data32 = tempTrie.data32;
        this.indexLength = tempTrie.indexLength;
        this.dataLength = tempTrie.dataLength;
        this.index2NullOffset = tempTrie.index2NullOffset;
        this.initialValue = tempTrie.initialValue;
        this.errorValue = tempTrie.errorValue;
        this.highStart = tempTrie.highStart;
        this.highValueIndex = tempTrie.highValueIndex;
        this.dataNullOffset = tempTrie.dataNullOffset;
    }

    private void writeBlock(int block, int value) {
        int limit = block + 32;
        for (int block2 = block; block2 < limit; block2++) {
            this.data[block2] = value;
        }
    }

    private void fillBlock(int block, int start, int limit, int value, int initialValue, boolean overwrite) {
        int pLimit = block + limit;
        if (overwrite) {
            for (int i = block + start; i < pLimit; i++) {
                this.data[i] = value;
            }
            return;
        }
        for (int i2 = block + start; i2 < pLimit; i2++) {
            if (this.data[i2] == initialValue) {
                this.data[i2] = value;
            }
        }
    }

    public Trie2Writable setRange(int start, int end, int value, boolean overwrite) {
        int repeatBlock;
        if (start > 1114111 || start < 0 || end > 1114111 || end < 0 || start > end) {
            throw new IllegalArgumentException("Invalid code point range.");
        }
        if (!overwrite && value == this.initialValue) {
            return this;
        }
        this.fHash = 0;
        if (this.isCompacted) {
            uncompact();
        }
        int limit = end + 1;
        if ((start & 31) != 0) {
            int block = getDataBlock(start, true);
            int nextStart = (start + 32) & (-32);
            if (nextStart <= limit) {
                fillBlock(block, start & 31, 32, value, this.initialValue, overwrite);
                start = nextStart;
            } else {
                fillBlock(block, start & 31, limit & 31, value, this.initialValue, overwrite);
                return this;
            }
        }
        int rest = limit & 31;
        int limit2 = limit & (-32);
        if (value == this.initialValue) {
            repeatBlock = this.dataNullOffset;
        } else {
            repeatBlock = -1;
        }
        while (start < limit2) {
            boolean setRepeatBlock = false;
            if (value == this.initialValue && isInNullBlock(start, true)) {
                start += 32;
            } else {
                int i2 = getIndex2Block(start, true) + ((start >> 5) & 63);
                int block2 = this.index2[i2];
                if (isWritableBlock(block2)) {
                    if (overwrite && block2 >= UNEWTRIE2_DATA_0800_OFFSET) {
                        setRepeatBlock = true;
                    } else {
                        fillBlock(block2, 0, 32, value, this.initialValue, overwrite);
                    }
                } else if (this.data[block2] != value && (overwrite || block2 == this.dataNullOffset)) {
                    setRepeatBlock = true;
                }
                if (setRepeatBlock) {
                    if (repeatBlock >= 0) {
                        setIndex2Entry(i2, repeatBlock);
                    } else {
                        repeatBlock = getDataBlock(start, true);
                        writeBlock(repeatBlock, value);
                    }
                }
                start += 32;
            }
        }
        if (rest > 0) {
            fillBlock(getDataBlock(start, true), 0, rest, value, this.initialValue, overwrite);
        }
        return this;
    }

    public Trie2Writable setRange(Trie2.Range range, boolean overwrite) {
        this.fHash = 0;
        if (range.leadSurrogate) {
            for (int c = range.startCodePoint; c <= range.endCodePoint; c++) {
                if (overwrite || getFromU16SingleLead((char) c) == this.initialValue) {
                    setForLeadSurrogateCodeUnit((char) c, range.value);
                }
            }
        } else {
            setRange(range.startCodePoint, range.endCodePoint, range.value, overwrite);
        }
        return this;
    }

    public Trie2Writable setForLeadSurrogateCodeUnit(char codeUnit, int value) {
        this.fHash = 0;
        set(codeUnit, false, value);
        return this;
    }

    @Override
    public int get(int codePoint) {
        if (codePoint < 0 || codePoint > 1114111) {
            return this.errorValue;
        }
        return get(codePoint, true);
    }

    private int get(int c, boolean fromLSCP) {
        int i2;
        if (c >= this.highStart && (c < 55296 || c >= 56320 || fromLSCP)) {
            return this.data[this.dataLength - 4];
        }
        if (c >= 55296 && c < 56320 && fromLSCP) {
            i2 = (c >> 5) + 320;
        } else {
            i2 = this.index1[c >> 11] + ((c >> 5) & 63);
        }
        int block = this.index2[i2];
        return this.data[(c & 31) + block];
    }

    @Override
    public int getFromU16SingleLead(char c) {
        return get(c, false);
    }

    private boolean equal_int(int[] a, int s, int t, int length) {
        for (int i = 0; i < length; i++) {
            if (a[s + i] != a[t + i]) {
                return false;
            }
        }
        return true;
    }

    private int findSameIndex2Block(int index2Length, int otherBlock) {
        int index2Length2 = index2Length - 64;
        for (int block = 0; block <= index2Length2; block++) {
            if (equal_int(this.index2, block, otherBlock, 64)) {
                return block;
            }
        }
        return -1;
    }

    private int findSameDataBlock(int dataLength, int otherBlock, int blockLength) {
        int dataLength2 = dataLength - blockLength;
        for (int block = 0; block <= dataLength2; block += 4) {
            if (equal_int(this.data, block, otherBlock, blockLength)) {
                return block;
            }
        }
        return -1;
    }

    private int findHighStart(int highValue) {
        int prevI2Block;
        int prevBlock;
        if (highValue == this.initialValue) {
            prevI2Block = this.index2NullOffset;
            prevBlock = this.dataNullOffset;
        } else {
            prevI2Block = -1;
            prevBlock = -1;
        }
        int i1 = 544;
        int c = 1114112;
        while (c > 0) {
            i1--;
            int i2Block = this.index1[i1];
            if (i2Block == prevI2Block) {
                c -= 2048;
            } else {
                prevI2Block = i2Block;
                if (i2Block == this.index2NullOffset) {
                    if (highValue != this.initialValue) {
                        return c;
                    }
                    c -= 2048;
                } else {
                    int i2 = 64;
                    while (i2 > 0) {
                        i2--;
                        int block = this.index2[i2Block + i2];
                        if (block == prevBlock) {
                            c -= 32;
                        } else {
                            prevBlock = block;
                            if (block == this.dataNullOffset) {
                                if (highValue != this.initialValue) {
                                    return c;
                                }
                                c -= 32;
                            } else {
                                int j = 32;
                                while (j > 0) {
                                    j--;
                                    int value = this.data[block + j];
                                    if (value != highValue) {
                                        return c;
                                    }
                                    c--;
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    private void compactData() {
        int newStart;
        int newStart2 = 192;
        int start = 0;
        int i = 0;
        while (start < 192) {
            this.map[i] = start;
            start += 32;
            i++;
        }
        int blockLength = 64;
        int blockCount = 2;
        int start2 = 192;
        while (start2 < this.dataLength) {
            if (start2 == UNEWTRIE2_DATA_0800_OFFSET) {
                blockLength = 32;
                blockCount = 1;
            }
            if (this.map[start2 >> 5] <= 0) {
                start2 += blockLength;
            } else {
                int movedStart = findSameDataBlock(newStart2, start2, blockLength);
                if (movedStart >= 0) {
                    int i2 = blockCount;
                    int mapIndex = start2 >> 5;
                    int mapIndex2 = mapIndex;
                    while (i2 > 0) {
                        this.map[mapIndex2] = movedStart;
                        movedStart += 32;
                        i2--;
                        mapIndex2++;
                    }
                    start2 += blockLength;
                } else {
                    int overlap = blockLength - 4;
                    while (overlap > 0 && !equal_int(this.data, newStart2 - overlap, start2, overlap)) {
                        overlap -= 4;
                    }
                    if (overlap > 0 || newStart2 < start2) {
                        int movedStart2 = newStart2 - overlap;
                        int i3 = blockCount;
                        int mapIndex3 = start2 >> 5;
                        int mapIndex4 = mapIndex3;
                        while (i3 > 0) {
                            this.map[mapIndex4] = movedStart2;
                            movedStart2 += 32;
                            i3--;
                            mapIndex4++;
                        }
                        int i4 = blockLength - overlap;
                        int newStart3 = newStart2;
                        int start3 = start2 + overlap;
                        while (i4 > 0) {
                            this.data[newStart3] = this.data[start3];
                            i4--;
                            newStart3++;
                            start3++;
                        }
                        newStart2 = newStart3;
                        start2 = start3;
                    } else {
                        int i5 = blockCount;
                        int mapIndex5 = start2 >> 5;
                        int mapIndex6 = mapIndex5;
                        while (i5 > 0) {
                            this.map[mapIndex6] = start2;
                            start2 += 32;
                            i5--;
                            mapIndex6++;
                        }
                        newStart2 = start2;
                    }
                }
            }
        }
        int i6 = 0;
        while (i6 < this.index2Length) {
            if (i6 == 2080) {
                i6 += 576;
            }
            this.index2[i6] = this.map[this.index2[i6] >> 5];
            i6++;
        }
        this.dataNullOffset = this.map[this.dataNullOffset >> 5];
        while (true) {
            newStart = newStart2;
            if ((newStart & 3) == 0) {
                break;
            }
            newStart2 = newStart + 1;
            this.data[newStart] = this.initialValue;
        }
        if (this.UTRIE2_DEBUG) {
            System.out.printf("compacting UTrie2: count of 32-bit data words %d->%d\n", Integer.valueOf(this.dataLength), Integer.valueOf(newStart));
        }
        this.dataLength = newStart;
    }

    private void compactIndex2() {
        int newStart;
        int start = 0;
        int i = 0;
        while (start < 2080) {
            this.map[i] = start;
            start += 64;
            i++;
        }
        int newStart2 = ((this.highStart - 65536) >> 11) + 32 + 2080;
        int start2 = UNEWTRIE2_INDEX_2_NULL_OFFSET;
        while (start2 < this.index2Length) {
            int movedStart = findSameIndex2Block(newStart2, start2);
            if (movedStart >= 0) {
                this.map[start2 >> 6] = movedStart;
                start2 += 64;
            } else {
                int overlap = 63;
                while (overlap > 0 && !equal_int(this.index2, newStart2 - overlap, start2, overlap)) {
                    overlap--;
                }
                if (overlap > 0 || newStart2 < start2) {
                    this.map[start2 >> 6] = newStart2 - overlap;
                    int i2 = 64 - overlap;
                    int newStart3 = newStart2;
                    int start3 = start2 + overlap;
                    while (i2 > 0) {
                        this.index2[newStart3] = this.index2[start3];
                        i2--;
                        newStart3++;
                        start3++;
                    }
                    newStart2 = newStart3;
                    start2 = start3;
                } else {
                    this.map[start2 >> 6] = start2;
                    start2 += 64;
                    newStart2 = start2;
                }
            }
        }
        for (int i3 = 0; i3 < 544; i3++) {
            this.index1[i3] = this.map[this.index1[i3] >> 6];
        }
        this.index2NullOffset = this.map[this.index2NullOffset >> 6];
        while (true) {
            newStart = newStart2;
            if ((newStart & 3) == 0) {
                break;
            }
            newStart2 = newStart + 1;
            this.index2[newStart] = UTRIE2_MAX_DATA_LENGTH;
        }
        if (this.UTRIE2_DEBUG) {
            System.out.printf("compacting UTrie2: count of 16-bit index-2 words %d->%d\n", Integer.valueOf(this.index2Length), Integer.valueOf(newStart));
        }
        this.index2Length = newStart;
    }

    private void compactTrie() {
        int highValue = get(1114111);
        int localHighStart = (findHighStart(highValue) + Opcodes.OP_IGET_WIDE_JUMBO) & (-2048);
        if (localHighStart == 1114112) {
            highValue = this.errorValue;
        }
        this.highStart = localHighStart;
        if (this.UTRIE2_DEBUG) {
            System.out.printf("UTrie2: highStart U+%04x  highValue 0x%x  initialValue 0x%x\n", Integer.valueOf(this.highStart), Integer.valueOf(highValue), Integer.valueOf(this.initialValue));
        }
        if (this.highStart < 1114112) {
            int suppHighStart = this.highStart <= 65536 ? 65536 : this.highStart;
            setRange(suppHighStart, 1114111, this.initialValue, true);
        }
        compactData();
        if (this.highStart > 65536) {
            compactIndex2();
        } else if (this.UTRIE2_DEBUG) {
            System.out.printf("UTrie2: highStart U+%04x  count of 16-bit index-2 words %d->%d\n", Integer.valueOf(this.highStart), Integer.valueOf(this.index2Length), 2112);
        }
        int[] iArr = this.data;
        int i = this.dataLength;
        this.dataLength = i + 1;
        iArr[i] = highValue;
        while ((this.dataLength & 3) != 0) {
            int[] iArr2 = this.data;
            int i2 = this.dataLength;
            this.dataLength = i2 + 1;
            iArr2[i2] = this.initialValue;
        }
        this.isCompacted = true;
    }

    public Trie2_16 toTrie2_16() {
        Trie2_16 frozenTrie = new Trie2_16();
        freeze(frozenTrie, Trie2.ValueWidth.BITS_16);
        return frozenTrie;
    }

    public Trie2_32 toTrie2_32() {
        Trie2_32 frozenTrie = new Trie2_32();
        freeze(frozenTrie, Trie2.ValueWidth.BITS_32);
        return frozenTrie;
    }

    private void freeze(Trie2 dest, Trie2.ValueWidth valueBits) {
        int allIndexesLength;
        int dataMove;
        int destIdx;
        int destIdx2;
        if (!this.isCompacted) {
            compactTrie();
        }
        if (this.highStart <= 65536) {
            allIndexesLength = 2112;
        } else {
            allIndexesLength = this.index2Length;
        }
        if (valueBits == Trie2.ValueWidth.BITS_16) {
            dataMove = allIndexesLength;
        } else {
            dataMove = 0;
        }
        if (allIndexesLength > 65535 || this.dataNullOffset + dataMove > 65535 || dataMove + UNEWTRIE2_DATA_0800_OFFSET > 65535 || this.dataLength + dataMove > UTRIE2_MAX_DATA_LENGTH) {
            throw new UnsupportedOperationException("Trie2 data is too large.");
        }
        int indexLength = allIndexesLength;
        if (valueBits == Trie2.ValueWidth.BITS_16) {
            indexLength += this.dataLength;
        } else {
            dest.data32 = new int[this.dataLength];
        }
        dest.index = new char[indexLength];
        dest.indexLength = allIndexesLength;
        dest.dataLength = this.dataLength;
        if (this.highStart <= 65536) {
            dest.index2NullOffset = 65535;
        } else {
            dest.index2NullOffset = this.index2NullOffset + 0;
        }
        dest.initialValue = this.initialValue;
        dest.errorValue = this.errorValue;
        dest.highStart = this.highStart;
        dest.highValueIndex = (this.dataLength + dataMove) - 4;
        dest.dataNullOffset = this.dataNullOffset + dataMove;
        dest.header = new Trie2.UTrie2Header();
        dest.header.signature = 1416784178;
        dest.header.options = valueBits == Trie2.ValueWidth.BITS_16 ? 0 : 1;
        dest.header.indexLength = dest.indexLength;
        dest.header.shiftedDataLength = dest.dataLength >> 2;
        dest.header.index2NullOffset = dest.index2NullOffset;
        dest.header.dataNullOffset = dest.dataNullOffset;
        dest.header.shiftedHighStart = dest.highStart >> 11;
        int destIdx3 = 0;
        int i = 0;
        while (true) {
            destIdx = destIdx3;
            if (i >= 2080) {
                break;
            }
            destIdx3 = destIdx + 1;
            dest.index[destIdx] = (char) ((this.index2[i] + dataMove) >> 2);
            i++;
        }
        if (this.UTRIE2_DEBUG) {
            System.out.println("\n\nIndex2 for BMP limit is " + Integer.toHexString(destIdx));
        }
        int i2 = 0;
        while (i2 < 2) {
            dest.index[destIdx] = (char) (dataMove + 128);
            i2++;
            destIdx++;
        }
        while (i2 < 32) {
            dest.index[destIdx] = (char) (this.index2[i2 << 1] + dataMove);
            i2++;
            destIdx++;
        }
        if (this.UTRIE2_DEBUG) {
            System.out.println("Index2 for UTF-8 2byte values limit is " + Integer.toHexString(destIdx));
        }
        if (this.highStart > 65536) {
            int index1Length = (this.highStart - 65536) >> 11;
            int index2Offset = index1Length + 2112;
            int i3 = 0;
            while (i3 < index1Length) {
                dest.index[destIdx] = (char) (this.index1[i3 + 32] + 0);
                i3++;
                destIdx++;
            }
            if (this.UTRIE2_DEBUG) {
                System.out.println("Index 1 for supplementals, limit is " + Integer.toHexString(destIdx));
            }
            int i4 = 0;
            while (true) {
                destIdx2 = destIdx;
                if (i4 >= this.index2Length - index2Offset) {
                    break;
                }
                destIdx = destIdx2 + 1;
                dest.index[destIdx2] = (char) ((this.index2[index2Offset + i4] + dataMove) >> 2);
                i4++;
            }
            if (this.UTRIE2_DEBUG) {
                System.out.println("Index 2 for supplementals, limit is " + Integer.toHexString(destIdx2));
            }
        } else {
            destIdx2 = destIdx;
        }
        switch (m59getandroidicuimplTrie2$ValueWidthSwitchesValues()[valueBits.ordinal()]) {
            case 1:
                if (!f24assertionsDisabled) {
                    if (!(destIdx2 == dataMove)) {
                        throw new AssertionError();
                    }
                }
                dest.data16 = destIdx2;
                int i5 = 0;
                while (i5 < this.dataLength) {
                    dest.index[destIdx2] = (char) this.data[i5];
                    i5++;
                    destIdx2++;
                }
                return;
            case 2:
                for (int i6 = 0; i6 < this.dataLength; i6++) {
                    dest.data32[i6] = this.data[i6];
                }
                return;
            default:
                return;
        }
    }
}
