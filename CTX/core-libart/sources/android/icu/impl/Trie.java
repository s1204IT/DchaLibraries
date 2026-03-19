package android.icu.impl;

import android.icu.text.UTF16;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class Trie {
    static final boolean $assertionsDisabled = false;
    protected static final int BMP_INDEX_LENGTH = 2048;
    protected static final int DATA_BLOCK_LENGTH = 32;
    protected static final int HEADER_LENGTH_ = 16;
    protected static final int HEADER_OPTIONS_DATA_IS_32_BIT_ = 256;
    protected static final int HEADER_OPTIONS_INDEX_SHIFT_ = 4;
    protected static final int HEADER_OPTIONS_LATIN1_IS_LINEAR_MASK_ = 512;
    private static final int HEADER_OPTIONS_SHIFT_MASK_ = 15;
    protected static final int HEADER_SIGNATURE_ = 1416784229;
    protected static final int INDEX_STAGE_1_SHIFT_ = 5;
    protected static final int INDEX_STAGE_2_SHIFT_ = 2;
    protected static final int INDEX_STAGE_3_MASK_ = 31;
    protected static final int LEAD_INDEX_OFFSET_ = 320;
    protected static final int SURROGATE_BLOCK_BITS = 5;
    protected static final int SURROGATE_BLOCK_COUNT = 32;
    protected static final int SURROGATE_MASK_ = 1023;
    protected int m_dataLength_;
    protected DataManipulate m_dataManipulate_;
    protected int m_dataOffset_;
    protected char[] m_index_;
    private boolean m_isLatin1Linear_;
    private int m_options_;

    public interface DataManipulate {
        int getFoldingOffset(int i);
    }

    protected abstract int getInitialValue();

    protected abstract int getSurrogateOffset(char c, char c2);

    protected abstract int getValue(int i);

    private static class DefaultGetFoldingOffset implements DataManipulate {
        private DefaultGetFoldingOffset() {
        }

        @Override
        public int getFoldingOffset(int i) {
            return i;
        }
    }

    public final boolean isLatin1Linear() {
        return this.m_isLatin1Linear_;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Trie)) {
            return false;
        }
        Trie trie = (Trie) obj;
        return this.m_isLatin1Linear_ == trie.m_isLatin1Linear_ && this.m_options_ == trie.m_options_ && this.m_dataLength_ == trie.m_dataLength_ && Arrays.equals(this.m_index_, trie.m_index_);
    }

    public int hashCode() {
        return 42;
    }

    public int getSerializedDataSize() {
        int i = 16 + (this.m_dataOffset_ << 1);
        if (isCharTrie()) {
            return i + (this.m_dataLength_ << 1);
        }
        if (isIntTrie()) {
            return i + (this.m_dataLength_ << 2);
        }
        return i;
    }

    protected Trie(ByteBuffer byteBuffer, DataManipulate dataManipulate) {
        int i = byteBuffer.getInt();
        this.m_options_ = byteBuffer.getInt();
        if (!checkHeader(i)) {
            throw new IllegalArgumentException("ICU data file error: Trie header authentication failed, please check if you have the most updated ICU data file");
        }
        if (dataManipulate != null) {
            this.m_dataManipulate_ = dataManipulate;
        } else {
            this.m_dataManipulate_ = new DefaultGetFoldingOffset();
        }
        this.m_isLatin1Linear_ = (this.m_options_ & 512) != 0;
        this.m_dataOffset_ = byteBuffer.getInt();
        this.m_dataLength_ = byteBuffer.getInt();
        unserialize(byteBuffer);
    }

    protected Trie(char[] cArr, int i, DataManipulate dataManipulate) {
        this.m_options_ = i;
        if (dataManipulate != null) {
            this.m_dataManipulate_ = dataManipulate;
        } else {
            this.m_dataManipulate_ = new DefaultGetFoldingOffset();
        }
        this.m_isLatin1Linear_ = (this.m_options_ & 512) != 0;
        this.m_index_ = cArr;
        this.m_dataOffset_ = this.m_index_.length;
    }

    protected final int getRawOffset(int i, char c) {
        return (this.m_index_[i + (c >> 5)] << 2) + (c & 31);
    }

    protected final int getBMPOffset(char c) {
        if (c >= 55296 && c <= 56319) {
            return getRawOffset(LEAD_INDEX_OFFSET_, c);
        }
        return getRawOffset(0, c);
    }

    protected final int getLeadOffset(char c) {
        return getRawOffset(0, c);
    }

    protected final int getCodePointOffset(int i) {
        if (i < 0) {
            return -1;
        }
        if (i < 55296) {
            return getRawOffset(0, (char) i);
        }
        if (i < 65536) {
            return getBMPOffset((char) i);
        }
        if (i > 1114111) {
            return -1;
        }
        return getSurrogateOffset(UTF16.getLeadSurrogate(i), (char) (i & 1023));
    }

    protected void unserialize(ByteBuffer byteBuffer) {
        this.m_index_ = ICUBinary.getChars(byteBuffer, this.m_dataOffset_, 0);
    }

    protected final boolean isIntTrie() {
        return (this.m_options_ & 256) != 0;
    }

    protected final boolean isCharTrie() {
        return (this.m_options_ & 256) == 0;
    }

    private final boolean checkHeader(int i) {
        if (i != HEADER_SIGNATURE_ || (this.m_options_ & 15) != 5 || ((this.m_options_ >> 4) & 15) != 2) {
            return false;
        }
        return true;
    }
}
