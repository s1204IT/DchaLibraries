package android.icu.impl;

import android.icu.impl.Trie;
import android.icu.impl.TrieBuilder;
import android.icu.text.UTF16;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class IntTrieBuilder extends TrieBuilder {
    protected int[] m_data_;
    protected int m_initialValue_;
    private int m_leadUnitValue_;

    public IntTrieBuilder(IntTrieBuilder table) {
        super(table);
        this.m_data_ = new int[this.m_dataCapacity_];
        System.arraycopy(table.m_data_, 0, this.m_data_, 0, this.m_dataLength_);
        this.m_initialValue_ = table.m_initialValue_;
        this.m_leadUnitValue_ = table.m_leadUnitValue_;
    }

    public IntTrieBuilder(int[] aliasdata, int maxdatalength, int initialvalue, int leadunitvalue, boolean latin1linear) {
        if (maxdatalength < 32 || (latin1linear && maxdatalength < 1024)) {
            throw new IllegalArgumentException("Argument maxdatalength is too small");
        }
        if (aliasdata != null) {
            this.m_data_ = aliasdata;
        } else {
            this.m_data_ = new int[maxdatalength];
        }
        int j = 32;
        if (latin1linear) {
            int i = 0;
            while (true) {
                int i2 = i + 1;
                this.m_index_[i] = j;
                j += 32;
                if (i2 >= 8) {
                    break;
                } else {
                    i = i2;
                }
            }
        }
        this.m_dataLength_ = j;
        Arrays.fill(this.m_data_, 0, this.m_dataLength_, initialvalue);
        this.m_initialValue_ = initialvalue;
        this.m_leadUnitValue_ = leadunitvalue;
        this.m_dataCapacity_ = maxdatalength;
        this.m_isLatin1Linear_ = latin1linear;
        this.m_isCompacted_ = false;
    }

    public int getValue(int ch) {
        if (this.m_isCompacted_ || ch > 1114111 || ch < 0) {
            return 0;
        }
        int block = this.m_index_[ch >> 5];
        return this.m_data_[Math.abs(block) + (ch & 31)];
    }

    public int getValue(int ch, boolean[] inBlockZero) {
        if (this.m_isCompacted_ || ch > 1114111 || ch < 0) {
            if (inBlockZero != null) {
                inBlockZero[0] = true;
            }
            return 0;
        }
        int block = this.m_index_[ch >> 5];
        if (inBlockZero != null) {
            inBlockZero[0] = block == 0;
        }
        return this.m_data_[Math.abs(block) + (ch & 31)];
    }

    public boolean setValue(int ch, int value) {
        int block;
        if (this.m_isCompacted_ || ch > 1114111 || ch < 0 || (block = getDataBlock(ch)) < 0) {
            return false;
        }
        this.m_data_[(ch & 31) + block] = value;
        return true;
    }

    public IntTrie serialize(TrieBuilder.DataManipulate datamanipulate, Trie.DataManipulate triedatamanipulate) {
        if (datamanipulate == null) {
            throw new IllegalArgumentException("Parameters can not be null");
        }
        if (!this.m_isCompacted_) {
            compact(false);
            fold(datamanipulate);
            compact(true);
            this.m_isCompacted_ = true;
        }
        if (this.m_dataLength_ >= 262144) {
            throw new ArrayIndexOutOfBoundsException("Data length too small");
        }
        char[] index = new char[this.m_indexLength_];
        int[] data = new int[this.m_dataLength_];
        for (int i = 0; i < this.m_indexLength_; i++) {
            index[i] = (char) (this.m_index_[i] >>> 2);
        }
        System.arraycopy(this.m_data_, 0, data, 0, this.m_dataLength_);
        int options = 293;
        if (this.m_isLatin1Linear_) {
            options = 293 | 512;
        }
        return new IntTrie(index, data, this.m_initialValue_, options, triedatamanipulate);
    }

    public int serialize(OutputStream os, boolean reduceTo16Bits, TrieBuilder.DataManipulate datamanipulate) throws IOException {
        int length;
        int length2;
        if (datamanipulate == null) {
            throw new IllegalArgumentException("Parameters can not be null");
        }
        if (!this.m_isCompacted_) {
            compact(false);
            fold(datamanipulate);
            compact(true);
            this.m_isCompacted_ = true;
        }
        if (reduceTo16Bits) {
            length = this.m_dataLength_ + this.m_indexLength_;
        } else {
            length = this.m_dataLength_;
        }
        if (length >= 262144) {
            throw new ArrayIndexOutOfBoundsException("Data length too small");
        }
        int length3 = (this.m_indexLength_ * 2) + 16;
        if (reduceTo16Bits) {
            length2 = length3 + (this.m_dataLength_ * 2);
        } else {
            length2 = length3 + (this.m_dataLength_ * 4);
        }
        if (os == null) {
            return length2;
        }
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(1416784229);
        int options = 37;
        if (!reduceTo16Bits) {
            options = 293;
        }
        if (this.m_isLatin1Linear_) {
            options |= 512;
        }
        dos.writeInt(options);
        dos.writeInt(this.m_indexLength_);
        dos.writeInt(this.m_dataLength_);
        if (reduceTo16Bits) {
            for (int i = 0; i < this.m_indexLength_; i++) {
                int v = (this.m_index_[i] + this.m_indexLength_) >>> 2;
                dos.writeChar(v);
            }
            for (int i2 = 0; i2 < this.m_dataLength_; i2++) {
                int v2 = this.m_data_[i2] & 65535;
                dos.writeChar(v2);
            }
        } else {
            for (int i3 = 0; i3 < this.m_indexLength_; i3++) {
                int v3 = this.m_index_[i3] >>> 2;
                dos.writeChar(v3);
            }
            for (int i4 = 0; i4 < this.m_dataLength_; i4++) {
                dos.writeInt(this.m_data_[i4]);
            }
        }
        return length2;
    }

    public boolean setRange(int start, int limit, int value, boolean overwrite) {
        if (this.m_isCompacted_ || start < 0 || start > 1114111 || limit < 0 || limit > 1114112 || start > limit) {
            return false;
        }
        if (start == limit) {
            return true;
        }
        if ((start & 31) != 0) {
            int block = getDataBlock(start);
            if (block < 0) {
                return false;
            }
            int nextStart = (start + 32) & (-32);
            if (nextStart <= limit) {
                fillBlock(block, start & 31, 32, value, overwrite);
                start = nextStart;
            } else {
                fillBlock(block, start & 31, limit & 31, value, overwrite);
                return true;
            }
        }
        int rest = limit & 31;
        int limit2 = limit & (-32);
        int repeatBlock = 0;
        if (value != this.m_initialValue_) {
            repeatBlock = -1;
        }
        while (true) {
            int repeatBlock2 = repeatBlock;
            if (start < limit2) {
                int block2 = this.m_index_[start >> 5];
                if (block2 > 0) {
                    fillBlock(block2, 0, 32, value, overwrite);
                    repeatBlock = repeatBlock2;
                } else if (this.m_data_[-block2] == value) {
                    repeatBlock = repeatBlock2;
                } else if (block2 != 0 && !overwrite) {
                    repeatBlock = repeatBlock2;
                } else if (repeatBlock2 >= 0) {
                    this.m_index_[start >> 5] = -repeatBlock2;
                    repeatBlock = repeatBlock2;
                } else {
                    repeatBlock = getDataBlock(start);
                    if (repeatBlock < 0) {
                        return false;
                    }
                    this.m_index_[start >> 5] = -repeatBlock;
                    fillBlock(repeatBlock, 0, 32, value, true);
                }
                start += 32;
            } else {
                if (rest > 0) {
                    int block3 = getDataBlock(start);
                    if (block3 < 0) {
                        return false;
                    }
                    fillBlock(block3, 0, rest, value, overwrite);
                    return true;
                }
                return true;
            }
        }
    }

    private int allocDataBlock() {
        int newBlock = this.m_dataLength_;
        int newTop = newBlock + 32;
        if (newTop > this.m_dataCapacity_) {
            return -1;
        }
        this.m_dataLength_ = newTop;
        return newBlock;
    }

    private int getDataBlock(int ch) {
        int ch2 = ch >> 5;
        int indexValue = this.m_index_[ch2];
        if (indexValue > 0) {
            return indexValue;
        }
        int newBlock = allocDataBlock();
        if (newBlock < 0) {
            return -1;
        }
        this.m_index_[ch2] = newBlock;
        System.arraycopy(this.m_data_, Math.abs(indexValue), this.m_data_, newBlock, 128);
        return newBlock;
    }

    private void compact(boolean overlap) {
        int i;
        int start;
        int newStart;
        if (this.m_isCompacted_) {
            return;
        }
        findUnusedBlocks();
        int overlapStart = 32;
        if (this.m_isLatin1Linear_) {
            overlapStart = 288;
        }
        int newStart2 = 32;
        int start2 = 32;
        while (start2 < this.m_dataLength_) {
            if (this.m_map_[start2 >>> 5] < 0) {
                start2 += 32;
            } else {
                if (start2 >= overlapStart) {
                    int i2 = findSameDataBlock(this.m_data_, newStart2, start2, overlap ? 4 : 32);
                    if (i2 >= 0) {
                        this.m_map_[start2 >>> 5] = i2;
                        start2 += 32;
                    }
                }
                if (overlap && start2 >= overlapStart) {
                    i = 28;
                    while (i > 0 && !equal_int(this.m_data_, newStart2 - i, start2, i)) {
                        i -= 4;
                    }
                } else {
                    i = 0;
                }
                if (i > 0) {
                    this.m_map_[start2 >>> 5] = newStart2 - i;
                    int start3 = start2 + i;
                    int i3 = 32 - i;
                    start = start3;
                    newStart = newStart2;
                    while (i3 > 0) {
                        this.m_data_[newStart] = this.m_data_[start];
                        i3--;
                        start++;
                        newStart++;
                    }
                } else if (newStart2 < start2) {
                    this.m_map_[start2 >>> 5] = newStart2;
                    int i4 = 32;
                    start = start2;
                    newStart = newStart2;
                    while (i4 > 0) {
                        this.m_data_[newStart] = this.m_data_[start];
                        i4--;
                        start++;
                        newStart++;
                    }
                } else {
                    this.m_map_[start2 >>> 5] = start2;
                    newStart2 += 32;
                    start2 = newStart2;
                }
                start2 = start;
                newStart2 = newStart;
            }
        }
        for (int i5 = 0; i5 < this.m_indexLength_; i5++) {
            this.m_index_[i5] = this.m_map_[Math.abs(this.m_index_[i5]) >>> 5];
        }
        this.m_dataLength_ = newStart2;
    }

    private static final int findSameDataBlock(int[] data, int dataLength, int otherBlock, int step) {
        int dataLength2 = dataLength - 32;
        int block = 0;
        while (block <= dataLength2) {
            if (!equal_int(data, block, otherBlock, 32)) {
                block += step;
            } else {
                return block;
            }
        }
        return -1;
    }

    private final void fold(TrieBuilder.DataManipulate manipulate) {
        int[] leadIndexes = new int[32];
        int[] index = this.m_index_;
        System.arraycopy(index, 1728, leadIndexes, 0, 32);
        int block = 0;
        if (this.m_leadUnitValue_ != this.m_initialValue_) {
            int block2 = allocDataBlock();
            if (block2 < 0) {
                throw new IllegalStateException("Internal error: Out of memory space");
            }
            fillBlock(block2, 0, 32, this.m_leadUnitValue_, true);
            block = -block2;
        }
        for (int c = 1728; c < 1760; c++) {
            this.m_index_[c] = block;
        }
        int indexLength = 2048;
        int c2 = 65536;
        while (c2 < 1114112) {
            if (index[c2 >> 5] != 0) {
                int c3 = c2 & (-1024);
                int block3 = findSameIndexBlock(index, indexLength, c3 >> 5);
                int value = manipulate.getFoldedValue(c3, block3 + 32);
                if (value != getValue(UTF16.getLeadSurrogate(c3))) {
                    if (!setValue(UTF16.getLeadSurrogate(c3), value)) {
                        throw new ArrayIndexOutOfBoundsException("Data table overflow");
                    }
                    if (block3 == indexLength) {
                        System.arraycopy(index, c3 >> 5, index, indexLength, 32);
                        indexLength += 32;
                    }
                }
                c2 = c3 + 1024;
            } else {
                c2 += 32;
            }
        }
        if (indexLength >= 34816) {
            throw new ArrayIndexOutOfBoundsException("Index table overflow");
        }
        System.arraycopy(index, 2048, index, 2080, indexLength - 2048);
        System.arraycopy(leadIndexes, 0, index, 2048, 32);
        this.m_indexLength_ = indexLength + 32;
    }

    private void fillBlock(int block, int start, int limit, int value, boolean overwrite) {
        int limit2 = limit + block;
        int block2 = block + start;
        if (!overwrite) {
            while (block2 < limit2) {
                if (this.m_data_[block2] == this.m_initialValue_) {
                    this.m_data_[block2] = value;
                }
                block2++;
            }
            return;
        }
        while (true) {
            int block3 = block2;
            if (block3 >= limit2) {
                return;
            }
            block2 = block3 + 1;
            this.m_data_[block3] = value;
        }
    }
}
