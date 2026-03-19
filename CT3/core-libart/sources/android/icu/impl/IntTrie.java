package android.icu.impl;

import android.icu.impl.Trie;
import android.icu.text.UTF16;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class IntTrie extends Trie {

    static final boolean f10assertionsDisabled;
    private int[] m_data_;
    private int m_initialValue_;

    static {
        f10assertionsDisabled = !IntTrie.class.desiredAssertionStatus();
    }

    public IntTrie(ByteBuffer bytes, Trie.DataManipulate dataManipulate) throws IOException {
        super(bytes, dataManipulate);
        if (isIntTrie()) {
        } else {
            throw new IllegalArgumentException("Data given does not belong to a int trie.");
        }
    }

    public IntTrie(int initialValue, int leadUnitValue, Trie.DataManipulate dataManipulate) {
        super(new char[2080], 512, dataManipulate);
        int dataLength = leadUnitValue != initialValue ? 288 : 256;
        this.m_data_ = new int[dataLength];
        this.m_dataLength_ = dataLength;
        this.m_initialValue_ = initialValue;
        for (int i = 0; i < 256; i++) {
            this.m_data_[i] = initialValue;
        }
        if (leadUnitValue == initialValue) {
            return;
        }
        char block = (char) 64;
        for (int i2 = 1728; i2 < 1760; i2++) {
            this.m_index_[i2] = block;
        }
        for (int i3 = 256; i3 < 288; i3++) {
            this.m_data_[i3] = leadUnitValue;
        }
    }

    public final int getCodePointValue(int ch) {
        if (ch >= 0 && ch < 55296) {
            return this.m_data_[(this.m_index_[ch >> 5] << 2) + (ch & 31)];
        }
        int offset = getCodePointOffset(ch);
        return offset >= 0 ? this.m_data_[offset] : this.m_initialValue_;
    }

    public final int getLeadValue(char ch) {
        return this.m_data_[getLeadOffset(ch)];
    }

    public final int getBMPValue(char ch) {
        return this.m_data_[getBMPOffset(ch)];
    }

    public final int getSurrogateValue(char lead, char trail) {
        if (!UTF16.isLeadSurrogate(lead) || !UTF16.isTrailSurrogate(trail)) {
            throw new IllegalArgumentException("Argument characters do not form a supplementary character");
        }
        int offset = getSurrogateOffset(lead, trail);
        if (offset > 0) {
            return this.m_data_[offset];
        }
        return this.m_initialValue_;
    }

    public final int getTrailValue(int leadvalue, char trail) {
        if (this.m_dataManipulate_ == null) {
            throw new NullPointerException("The field DataManipulate in this Trie is null");
        }
        int offset = this.m_dataManipulate_.getFoldingOffset(leadvalue);
        if (offset > 0) {
            return this.m_data_[getRawOffset(offset, (char) (trail & 1023))];
        }
        return this.m_initialValue_;
    }

    public final int getLatin1LinearValue(char ch) {
        return this.m_data_[ch + ' '];
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = super.equals(obj);
        return result && (obj instanceof IntTrie) && this.m_initialValue_ == obj.m_initialValue_ && Arrays.equals(this.m_data_, obj.m_data_);
    }

    @Override
    public int hashCode() {
        if (f10assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    @Override
    protected final void unserialize(ByteBuffer bytes) {
        super.unserialize(bytes);
        this.m_data_ = ICUBinary.getInts(bytes, this.m_dataLength_, 0);
        this.m_initialValue_ = this.m_data_[0];
    }

    @Override
    protected final int getSurrogateOffset(char lead, char trail) {
        if (this.m_dataManipulate_ == null) {
            throw new NullPointerException("The field DataManipulate in this Trie is null");
        }
        int offset = this.m_dataManipulate_.getFoldingOffset(getLeadValue(lead));
        if (offset > 0) {
            return getRawOffset(offset, (char) (trail & 1023));
        }
        return -1;
    }

    @Override
    protected final int getValue(int index) {
        return this.m_data_[index];
    }

    @Override
    protected final int getInitialValue() {
        return this.m_initialValue_;
    }

    IntTrie(char[] index, int[] data, int initialvalue, int options, Trie.DataManipulate datamanipulate) {
        super(index, options, datamanipulate);
        this.m_data_ = data;
        this.m_dataLength_ = this.m_data_.length;
        this.m_initialValue_ = initialvalue;
    }
}
