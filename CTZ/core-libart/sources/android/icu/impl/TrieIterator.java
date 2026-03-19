package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.UTF16;
import android.icu.util.RangeValueIterator;
import dalvik.bytecode.Opcodes;

public class TrieIterator implements RangeValueIterator {
    private static final int BMP_INDEX_LENGTH_ = 2048;
    private static final int DATA_BLOCK_LENGTH_ = 32;
    private static final int LEAD_SURROGATE_MIN_VALUE_ = 55296;
    private static final int TRAIL_SURROGATE_COUNT_ = 1024;
    private static final int TRAIL_SURROGATE_INDEX_BLOCK_LENGTH_ = 32;
    private static final int TRAIL_SURROGATE_MIN_VALUE_ = 56320;
    private int m_currentCodepoint_;
    private int m_initialValue_;
    private int m_nextBlockIndex_;
    private int m_nextBlock_;
    private int m_nextCodepoint_;
    private int m_nextIndex_;
    private int m_nextTrailIndexOffset_;
    private int m_nextValue_;
    private Trie m_trie_;

    public TrieIterator(Trie trie) {
        if (trie == null) {
            throw new IllegalArgumentException("Argument trie cannot be null");
        }
        this.m_trie_ = trie;
        this.m_initialValue_ = extract(this.m_trie_.getInitialValue());
        reset();
    }

    @Override
    public final boolean next(RangeValueIterator.Element element) {
        if (this.m_nextCodepoint_ > 1114111) {
            return false;
        }
        if (this.m_nextCodepoint_ < 65536 && calculateNextBMPElement(element)) {
            return true;
        }
        calculateNextSupplementaryElement(element);
        return true;
    }

    @Override
    public final void reset() {
        this.m_currentCodepoint_ = 0;
        this.m_nextCodepoint_ = 0;
        this.m_nextIndex_ = 0;
        this.m_nextBlock_ = this.m_trie_.m_index_[0] << 2;
        if (this.m_nextBlock_ == this.m_trie_.m_dataOffset_) {
            this.m_nextValue_ = this.m_initialValue_;
        } else {
            this.m_nextValue_ = extract(this.m_trie_.getValue(this.m_nextBlock_));
        }
        this.m_nextBlockIndex_ = 0;
        this.m_nextTrailIndexOffset_ = 32;
    }

    protected int extract(int i) {
        return i;
    }

    private final void setResult(RangeValueIterator.Element element, int i, int i2, int i3) {
        element.start = i;
        element.limit = i2;
        element.value = i3;
    }

    private final boolean calculateNextBMPElement(RangeValueIterator.Element element) {
        int i = this.m_nextValue_;
        this.m_currentCodepoint_ = this.m_nextCodepoint_;
        this.m_nextCodepoint_++;
        this.m_nextBlockIndex_++;
        if (!checkBlockDetail(i)) {
            setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
            return true;
        }
        while (this.m_nextCodepoint_ < 65536) {
            if (this.m_nextCodepoint_ == 55296) {
                this.m_nextIndex_ = 2048;
            } else if (this.m_nextCodepoint_ == 56320) {
                this.m_nextIndex_ = this.m_nextCodepoint_ >> 5;
            } else {
                this.m_nextIndex_++;
            }
            this.m_nextBlockIndex_ = 0;
            if (!checkBlock(i)) {
                setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
                return true;
            }
        }
        this.m_nextCodepoint_--;
        this.m_nextBlockIndex_--;
        return false;
    }

    private final void calculateNextSupplementaryElement(RangeValueIterator.Element element) {
        int i = this.m_nextValue_;
        this.m_nextCodepoint_++;
        this.m_nextBlockIndex_++;
        if (UTF16.getTrailSurrogate(this.m_nextCodepoint_) != 56320) {
            if (!checkNullNextTrailIndex() && !checkBlockDetail(i)) {
                setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
                this.m_currentCodepoint_ = this.m_nextCodepoint_;
                return;
            }
            this.m_nextIndex_++;
            this.m_nextTrailIndexOffset_++;
            if (!checkTrailBlock(i)) {
                setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
                this.m_currentCodepoint_ = this.m_nextCodepoint_;
                return;
            }
        }
        int leadSurrogate = UTF16.getLeadSurrogate(this.m_nextCodepoint_);
        while (leadSurrogate < 56320) {
            int i2 = this.m_trie_.m_index_[leadSurrogate >> 5] << 2;
            if (i2 == this.m_trie_.m_dataOffset_) {
                if (i != this.m_initialValue_) {
                    this.m_nextValue_ = this.m_initialValue_;
                    this.m_nextBlock_ = i2;
                    this.m_nextBlockIndex_ = 0;
                    setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
                    this.m_currentCodepoint_ = this.m_nextCodepoint_;
                    return;
                }
                leadSurrogate += 32;
                this.m_nextCodepoint_ = Character.toCodePoint((char) leadSurrogate, UCharacter.MIN_LOW_SURROGATE);
            } else {
                if (this.m_trie_.m_dataManipulate_ == null) {
                    throw new NullPointerException("The field DataManipulate in this Trie is null");
                }
                this.m_nextIndex_ = this.m_trie_.m_dataManipulate_.getFoldingOffset(this.m_trie_.getValue(i2 + (leadSurrogate & 31)));
                if (this.m_nextIndex_ <= 0) {
                    if (i != this.m_initialValue_) {
                        this.m_nextValue_ = this.m_initialValue_;
                        this.m_nextBlock_ = this.m_trie_.m_dataOffset_;
                        this.m_nextBlockIndex_ = 0;
                        setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
                        this.m_currentCodepoint_ = this.m_nextCodepoint_;
                        return;
                    }
                    this.m_nextCodepoint_ += 1024;
                } else {
                    this.m_nextTrailIndexOffset_ = 0;
                    if (!checkTrailBlock(i)) {
                        setResult(element, this.m_currentCodepoint_, this.m_nextCodepoint_, i);
                        this.m_currentCodepoint_ = this.m_nextCodepoint_;
                        return;
                    }
                }
                leadSurrogate++;
            }
        }
        setResult(element, this.m_currentCodepoint_, 1114112, i);
    }

    private final boolean checkBlockDetail(int i) {
        while (this.m_nextBlockIndex_ < 32) {
            this.m_nextValue_ = extract(this.m_trie_.getValue(this.m_nextBlock_ + this.m_nextBlockIndex_));
            if (this.m_nextValue_ != i) {
                return false;
            }
            this.m_nextBlockIndex_++;
            this.m_nextCodepoint_++;
        }
        return true;
    }

    private final boolean checkBlock(int i) {
        int i2 = this.m_nextBlock_;
        this.m_nextBlock_ = this.m_trie_.m_index_[this.m_nextIndex_] << 2;
        if (this.m_nextBlock_ == i2 && this.m_nextCodepoint_ - this.m_currentCodepoint_ >= 32) {
            this.m_nextCodepoint_ += 32;
            return true;
        }
        if (this.m_nextBlock_ != this.m_trie_.m_dataOffset_) {
            return checkBlockDetail(i);
        }
        if (i != this.m_initialValue_) {
            this.m_nextValue_ = this.m_initialValue_;
            this.m_nextBlockIndex_ = 0;
            return false;
        }
        this.m_nextCodepoint_ += 32;
        return true;
    }

    private final boolean checkTrailBlock(int i) {
        while (this.m_nextTrailIndexOffset_ < 32) {
            this.m_nextBlockIndex_ = 0;
            if (!checkBlock(i)) {
                return false;
            }
            this.m_nextTrailIndexOffset_++;
            this.m_nextIndex_++;
        }
        return true;
    }

    private final boolean checkNullNextTrailIndex() {
        if (this.m_nextIndex_ <= 0) {
            this.m_nextCodepoint_ += Opcodes.OP_NEW_INSTANCE_JUMBO;
            char leadSurrogate = UTF16.getLeadSurrogate(this.m_nextCodepoint_);
            int i = this.m_trie_.m_index_[leadSurrogate >> 5] << 2;
            if (this.m_trie_.m_dataManipulate_ == null) {
                throw new NullPointerException("The field DataManipulate in this Trie is null");
            }
            this.m_nextIndex_ = this.m_trie_.m_dataManipulate_.getFoldingOffset(this.m_trie_.getValue(i + (leadSurrogate & 31)));
            this.m_nextIndex_--;
            this.m_nextBlockIndex_ = 32;
            return true;
        }
        return false;
    }
}
