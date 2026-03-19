package android.icu.impl;

import android.icu.impl.Trie;
import java.nio.ByteBuffer;

public class CharTrie extends Trie {

    static final boolean f1assertionsDisabled;
    private char[] m_data_;
    private char m_initialValue_;

    static {
        f1assertionsDisabled = !CharTrie.class.desiredAssertionStatus();
    }

    public CharTrie(ByteBuffer bytes, Trie.DataManipulate dataManipulate) {
        super(bytes, dataManipulate);
        if (isCharTrie()) {
        } else {
            throw new IllegalArgumentException("Data given does not belong to a char trie.");
        }
    }

    public CharTrie(int initialValue, int leadUnitValue, Trie.DataManipulate dataManipulate) {
        super(new char[2080], 512, dataManipulate);
        int dataLength = leadUnitValue != initialValue ? 288 : 256;
        this.m_data_ = new char[dataLength];
        this.m_dataLength_ = dataLength;
        this.m_initialValue_ = (char) initialValue;
        for (int i = 0; i < 256; i++) {
            this.m_data_[i] = (char) initialValue;
        }
        if (leadUnitValue == initialValue) {
            return;
        }
        char block = (char) 64;
        for (int i2 = 1728; i2 < 1760; i2++) {
            this.m_index_[i2] = block;
        }
        for (int i3 = 256; i3 < 288; i3++) {
            this.m_data_[i3] = (char) leadUnitValue;
        }
    }

    public final char getCodePointValue(int ch) {
        if (ch >= 0 && ch < 55296) {
            return this.m_data_[(this.m_index_[ch >> 5] << 2) + (ch & 31)];
        }
        int offset = getCodePointOffset(ch);
        return offset >= 0 ? this.m_data_[offset] : this.m_initialValue_;
    }

    public final char getLeadValue(char ch) {
        return this.m_data_[getLeadOffset(ch)];
    }

    public final char getBMPValue(char ch) {
        return this.m_data_[getBMPOffset(ch)];
    }

    public final char getSurrogateValue(char lead, char trail) {
        int offset = getSurrogateOffset(lead, trail);
        if (offset > 0) {
            return this.m_data_[offset];
        }
        return this.m_initialValue_;
    }

    public final char getTrailValue(int leadvalue, char trail) {
        if (this.m_dataManipulate_ == null) {
            throw new NullPointerException("The field DataManipulate in this Trie is null");
        }
        int offset = this.m_dataManipulate_.getFoldingOffset(leadvalue);
        if (offset > 0) {
            return this.m_data_[getRawOffset(offset, (char) (trail & 1023))];
        }
        return this.m_initialValue_;
    }

    public final char getLatin1LinearValue(char ch) {
        return this.m_data_[this.m_dataOffset_ + 32 + ch];
    }

    @Override
    public boolean equals(Object other) {
        boolean result = super.equals(other);
        if (!result || !(other instanceof CharTrie)) {
            return false;
        }
        CharTrie othertrie = (CharTrie) other;
        return this.m_initialValue_ == othertrie.m_initialValue_;
    }

    @Override
    public int hashCode() {
        if (f1assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    @Override
    protected final void unserialize(ByteBuffer bytes) {
        int indexDataLength = this.m_dataOffset_ + this.m_dataLength_;
        this.m_index_ = ICUBinary.getChars(bytes, indexDataLength, 0);
        this.m_data_ = this.m_index_;
        this.m_initialValue_ = this.m_data_[this.m_dataOffset_];
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
}
