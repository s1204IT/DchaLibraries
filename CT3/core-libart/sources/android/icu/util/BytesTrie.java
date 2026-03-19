package android.icu.util;

import android.icu.lang.UCharacterEnums;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public final class BytesTrie implements Cloneable, Iterable<Entry> {

    static final boolean f102assertionsDisabled;
    static final int kFiveByteDeltaLead = 255;
    static final int kFiveByteValueLead = 127;
    static final int kFourByteDeltaLead = 254;
    static final int kFourByteValueLead = 126;
    static final int kMaxBranchLinearSubNodeLength = 5;
    static final int kMaxLinearMatchLength = 16;
    static final int kMaxOneByteDelta = 191;
    static final int kMaxOneByteValue = 64;
    static final int kMaxThreeByteDelta = 917503;
    static final int kMaxThreeByteValue = 1179647;
    static final int kMaxTwoByteDelta = 12287;
    static final int kMaxTwoByteValue = 6911;
    static final int kMinLinearMatch = 16;
    static final int kMinOneByteValueLead = 16;
    static final int kMinThreeByteDeltaLead = 240;
    static final int kMinThreeByteValueLead = 108;
    static final int kMinTwoByteDeltaLead = 192;
    static final int kMinTwoByteValueLead = 81;
    static final int kMinValueLead = 32;
    private static final int kValueIsFinal = 1;
    private static Result[] valueResults_;
    private byte[] bytes_;
    private int pos_;
    private int remainingMatchLength_ = -1;
    private int root_;

    public static final class State {
        private byte[] bytes;
        private int pos;
        private int remainingMatchLength;
        private int root;
    }

    public BytesTrie(byte[] trieBytes, int offset) {
        this.bytes_ = trieBytes;
        this.root_ = offset;
        this.pos_ = offset;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public BytesTrie reset() {
        this.pos_ = this.root_;
        this.remainingMatchLength_ = -1;
        return this;
    }

    public BytesTrie saveState(State state) {
        state.bytes = this.bytes_;
        state.root = this.root_;
        state.pos = this.pos_;
        state.remainingMatchLength = this.remainingMatchLength_;
        return this;
    }

    public BytesTrie resetToState(State state) {
        if (this.bytes_ == state.bytes && this.bytes_ != null && this.root_ == state.root) {
            this.pos_ = state.pos;
            this.remainingMatchLength_ = state.remainingMatchLength;
            return this;
        }
        throw new IllegalArgumentException("incompatible trie state");
    }

    public enum Result {
        NO_MATCH,
        NO_VALUE,
        FINAL_VALUE,
        INTERMEDIATE_VALUE;

        public static Result[] valuesCustom() {
            return values();
        }

        public boolean matches() {
            return this != NO_MATCH;
        }

        public boolean hasValue() {
            return ordinal() >= 2;
        }

        public boolean hasNext() {
            return (ordinal() & 1) != 0;
        }
    }

    public Result current() {
        int node;
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        return (this.remainingMatchLength_ >= 0 || (node = this.bytes_[pos] & 255) < 32) ? Result.NO_VALUE : valueResults_[node & 1];
    }

    public Result first(int inByte) {
        this.remainingMatchLength_ = -1;
        if (inByte < 0) {
            inByte += 256;
        }
        return nextImpl(this.root_, inByte);
    }

    public Result next(int inByte) {
        int node;
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        if (inByte < 0) {
            inByte += 256;
        }
        int length = this.remainingMatchLength_;
        if (length >= 0) {
            int pos2 = pos + 1;
            if (inByte == (this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                int length2 = length - 1;
                this.remainingMatchLength_ = length2;
                this.pos_ = pos2;
                return (length2 >= 0 || (node = this.bytes_[pos2] & 255) < 32) ? Result.NO_VALUE : valueResults_[node & 1];
            }
            stop();
            return Result.NO_MATCH;
        }
        return nextImpl(pos, inByte);
    }

    public Result next(byte[] s, int sIndex, int sLimit) {
        int node;
        int sIndex2;
        if (sIndex >= sLimit) {
            return current();
        }
        int pos = this.pos_;
        if (pos < 0) {
            return Result.NO_MATCH;
        }
        int length = this.remainingMatchLength_;
        for (int sIndex3 = sIndex; sIndex3 != sLimit; sIndex3 = sIndex2) {
            sIndex2 = sIndex3 + 1;
            byte inByte = s[sIndex3];
            if (length < 0) {
                this.remainingMatchLength_ = length;
                while (true) {
                    int pos2 = pos + 1;
                    int node2 = this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    if (node2 < 16) {
                        Result result = branchNext(pos2, node2, inByte & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        if (result == Result.NO_MATCH) {
                            return Result.NO_MATCH;
                        }
                        if (sIndex2 == sLimit) {
                            return result;
                        }
                        if (result == Result.FINAL_VALUE) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        inByte = s[sIndex2];
                        pos = this.pos_;
                        sIndex2++;
                    } else if (node2 < 32) {
                        length = node2 - 16;
                        if (inByte != this.bytes_[pos2]) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        pos = pos2 + 1;
                    } else {
                        if ((node2 & 1) != 0) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        pos = skipValue(pos2, node2);
                        if (f102assertionsDisabled) {
                            continue;
                        } else {
                            if (!((this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 32)) {
                                throw new AssertionError();
                            }
                        }
                    }
                }
            } else {
                if (inByte != this.bytes_[pos]) {
                    stop();
                    return Result.NO_MATCH;
                }
                pos++;
            }
            length--;
        }
        this.remainingMatchLength_ = length;
        this.pos_ = pos;
        return (length >= 0 || (node = this.bytes_[pos] & 255) < 32) ? Result.NO_VALUE : valueResults_[node & 1];
    }

    public int getValue() {
        int pos = this.pos_;
        int pos2 = pos + 1;
        int leadByte = this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
        if (!f102assertionsDisabled) {
            if (!(leadByte >= 32)) {
                throw new AssertionError();
            }
        }
        return readValue(this.bytes_, pos2, leadByte >> 1);
    }

    public long getUniqueValue() {
        int pos = this.pos_;
        if (pos < 0) {
            return 0L;
        }
        long uniqueValue = findUniqueValue(this.bytes_, this.remainingMatchLength_ + pos + 1, 0L);
        return (uniqueValue << 31) >> 31;
    }

    public int getNextBytes(Appendable out) {
        int pos;
        int pos2 = this.pos_;
        if (pos2 < 0) {
            return 0;
        }
        if (this.remainingMatchLength_ >= 0) {
            append(out, this.bytes_[pos2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            return 1;
        }
        int pos3 = pos2 + 1;
        int node = this.bytes_[pos2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
        if (node >= 32) {
            if ((node & 1) != 0) {
                return 0;
            }
            int pos4 = skipValue(pos3, node);
            pos3 = pos4 + 1;
            node = this.bytes_[pos4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if (!f102assertionsDisabled) {
                if (!(node < 32)) {
                    throw new AssertionError();
                }
            }
        }
        if (node < 16) {
            if (node == 0) {
                pos = pos3 + 1;
                node = this.bytes_[pos3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            } else {
                pos = pos3;
            }
            int node2 = node + 1;
            getNextBranchBytes(this.bytes_, pos, node2, out);
            return node2;
        }
        append(out, this.bytes_[pos3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
        return 1;
    }

    @Override
    public java.util.Iterator<Entry> iterator2() {
        return new Iterator(this.bytes_, this.pos_, this.remainingMatchLength_, 0, null);
    }

    public Iterator iterator(int maxStringLength) {
        return new Iterator(this.bytes_, this.pos_, this.remainingMatchLength_, maxStringLength, null);
    }

    public static Iterator iterator(byte[] trieBytes, int offset, int maxStringLength) {
        return new Iterator(trieBytes, offset, -1, maxStringLength, null);
    }

    public static final class Entry {
        private byte[] bytes;
        private int length;
        public int value;

        Entry(int capacity, Entry entry) {
            this(capacity);
        }

        private Entry(int capacity) {
            this.bytes = new byte[capacity];
        }

        public int bytesLength() {
            return this.length;
        }

        public byte byteAt(int index) {
            return this.bytes[index];
        }

        public void copyBytesTo(byte[] dest, int destOffset) {
            System.arraycopy(this.bytes, 0, dest, destOffset, this.length);
        }

        public ByteBuffer bytesAsByteBuffer() {
            return ByteBuffer.wrap(this.bytes, 0, this.length).asReadOnlyBuffer();
        }

        private void ensureCapacity(int len) {
            if (this.bytes.length >= len) {
                return;
            }
            byte[] newBytes = new byte[Math.min(this.bytes.length * 2, len * 2)];
            System.arraycopy(this.bytes, 0, newBytes, 0, this.length);
            this.bytes = newBytes;
        }

        private void append(byte b) {
            ensureCapacity(this.length + 1);
            byte[] bArr = this.bytes;
            int i = this.length;
            this.length = i + 1;
            bArr[i] = b;
        }

        private void append(byte[] b, int off, int len) {
            ensureCapacity(this.length + len);
            System.arraycopy(b, off, this.bytes, this.length, len);
            this.length += len;
        }

        private void truncateString(int newLength) {
            this.length = newLength;
        }
    }

    public static final class Iterator implements java.util.Iterator<Entry> {
        private byte[] bytes_;
        private Entry entry_;
        private int initialPos_;
        private int initialRemainingMatchLength_;
        private int maxLength_;
        private int pos_;
        private int remainingMatchLength_;
        private ArrayList<Long> stack_;

        Iterator(byte[] trieBytes, int offset, int remainingMatchLength, int maxStringLength, Iterator iterator) {
            this(trieBytes, offset, remainingMatchLength, maxStringLength);
        }

        private Iterator(byte[] trieBytes, int offset, int remainingMatchLength, int maxStringLength) {
            this.stack_ = new ArrayList<>();
            this.bytes_ = trieBytes;
            this.initialPos_ = offset;
            this.pos_ = offset;
            this.initialRemainingMatchLength_ = remainingMatchLength;
            this.remainingMatchLength_ = remainingMatchLength;
            this.maxLength_ = maxStringLength;
            this.entry_ = new Entry(this.maxLength_ != 0 ? this.maxLength_ : 32, null);
            int length = this.remainingMatchLength_;
            if (length < 0) {
                return;
            }
            int length2 = length + 1;
            if (this.maxLength_ > 0 && length2 > this.maxLength_) {
                length2 = this.maxLength_;
            }
            this.entry_.append(this.bytes_, this.pos_, length2);
            this.pos_ += length2;
            this.remainingMatchLength_ -= length2;
        }

        public Iterator reset() {
            this.pos_ = this.initialPos_;
            this.remainingMatchLength_ = this.initialRemainingMatchLength_;
            int length = this.remainingMatchLength_ + 1;
            if (this.maxLength_ > 0 && length > this.maxLength_) {
                length = this.maxLength_;
            }
            this.entry_.truncateString(length);
            this.pos_ += length;
            this.remainingMatchLength_ -= length;
            this.stack_.clear();
            return this;
        }

        @Override
        public boolean hasNext() {
            return this.pos_ >= 0 || !this.stack_.isEmpty();
        }

        @Override
        public Entry next() {
            int pos;
            int pos2 = this.pos_;
            if (pos2 < 0) {
                if (this.stack_.isEmpty()) {
                    throw new NoSuchElementException();
                }
                long top = this.stack_.remove(this.stack_.size() - 1).longValue();
                int length = (int) top;
                int pos3 = (int) (top >> 32);
                this.entry_.truncateString(65535 & length);
                int length2 = length >>> 16;
                if (length2 > 1) {
                    pos2 = branchNext(pos3, length2);
                    if (pos2 < 0) {
                        return this.entry_;
                    }
                } else {
                    this.entry_.append(this.bytes_[pos3]);
                    pos2 = pos3 + 1;
                }
            }
            if (this.remainingMatchLength_ >= 0) {
                return truncateAndStop();
            }
            while (true) {
                int pos4 = pos2 + 1;
                int node = this.bytes_[pos2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                if (node >= 32) {
                    boolean isFinal = (node & 1) != 0;
                    this.entry_.value = BytesTrie.readValue(this.bytes_, pos4, node >> 1);
                    if (isFinal || (this.maxLength_ > 0 && this.entry_.length == this.maxLength_)) {
                        this.pos_ = -1;
                    } else {
                        this.pos_ = BytesTrie.skipValue(pos4, node);
                    }
                    return this.entry_;
                }
                if (this.maxLength_ > 0 && this.entry_.length == this.maxLength_) {
                    return truncateAndStop();
                }
                if (node < 16) {
                    if (node == 0) {
                        pos = pos4 + 1;
                        node = this.bytes_[pos4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    } else {
                        pos = pos4;
                    }
                    pos2 = branchNext(pos, node + 1);
                    if (pos2 < 0) {
                        return this.entry_;
                    }
                } else {
                    int length3 = (node - 16) + 1;
                    if (this.maxLength_ > 0 && this.entry_.length + length3 > this.maxLength_) {
                        this.entry_.append(this.bytes_, pos4, this.maxLength_ - this.entry_.length);
                        return truncateAndStop();
                    }
                    this.entry_.append(this.bytes_, pos4, length3);
                    pos2 = pos4 + length3;
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Entry truncateAndStop() {
            this.pos_ = -1;
            this.entry_.value = -1;
            return this.entry_;
        }

        private int branchNext(int pos, int length) {
            int pos2;
            while (true) {
                pos2 = pos;
                if (length <= 5) {
                    break;
                }
                int pos3 = pos2 + 1;
                this.stack_.add(Long.valueOf((((long) BytesTrie.skipDelta(this.bytes_, pos3)) << 32) | ((long) ((length - (length >> 1)) << 16)) | ((long) this.entry_.length)));
                length >>= 1;
                pos = BytesTrie.jumpByDelta(this.bytes_, pos3);
            }
            int pos4 = pos2 + 1;
            byte trieByte = this.bytes_[pos2];
            int pos5 = pos4 + 1;
            int node = this.bytes_[pos4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            boolean isFinal = (node & 1) != 0;
            int value = BytesTrie.readValue(this.bytes_, pos5, node >> 1);
            int pos6 = BytesTrie.skipValue(pos5, node);
            this.stack_.add(Long.valueOf((((long) pos6) << 32) | ((long) ((length - 1) << 16)) | ((long) this.entry_.length)));
            this.entry_.append(trieByte);
            if (isFinal) {
                this.pos_ = -1;
                this.entry_.value = value;
                return -1;
            }
            return pos6 + value;
        }
    }

    private void stop() {
        this.pos_ = -1;
    }

    private static int readValue(byte[] bytes, int pos, int leadByte) {
        if (leadByte < 81) {
            int value = leadByte - 16;
            return value;
        }
        if (leadByte < 108) {
            int value2 = ((leadByte - 81) << 8) | (bytes[pos] & 255);
            return value2;
        }
        if (leadByte < 126) {
            int value3 = ((leadByte - 108) << 16) | ((bytes[pos] & 255) << 8) | (bytes[pos + 1] & 255);
            return value3;
        }
        if (leadByte == 126) {
            int value4 = ((bytes[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bytes[pos + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (bytes[pos + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            return value4;
        }
        int value5 = (bytes[pos] << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) | ((bytes[pos + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bytes[pos + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (bytes[pos + 3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
        return value5;
    }

    private static int skipValue(int pos, int leadByte) {
        if (!f102assertionsDisabled) {
            if (!(leadByte >= 32)) {
                throw new AssertionError();
            }
        }
        if (leadByte >= 162) {
            if (leadByte < 216) {
                return pos + 1;
            }
            if (leadByte < 252) {
                return pos + 2;
            }
            return pos + ((leadByte >> 1) & 1) + 3;
        }
        return pos;
    }

    private static int skipValue(byte[] bytes, int pos) {
        int leadByte = bytes[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
        return skipValue(pos + 1, leadByte);
    }

    private static int jumpByDelta(byte[] bytes, int pos) {
        int pos2;
        int pos3 = pos + 1;
        int delta = bytes[pos] & 255;
        if (delta < 192) {
            pos2 = pos3;
        } else if (delta < 240) {
            pos2 = pos3 + 1;
            delta = ((delta - 192) << 8) | (bytes[pos3] & 255);
        } else if (delta < 254) {
            delta = ((delta - 240) << 16) | ((bytes[pos3] & 255) << 8) | (bytes[pos3 + 1] & 255);
            pos2 = pos3 + 2;
        } else if (delta == 254) {
            delta = ((bytes[pos3] & 255) << 16) | ((bytes[pos3 + 1] & 255) << 8) | (bytes[pos3 + 2] & 255);
            pos2 = pos3 + 3;
        } else {
            delta = (bytes[pos3] << 24) | ((bytes[pos3 + 1] & 255) << 16) | ((bytes[pos3 + 2] & 255) << 8) | (bytes[pos3 + 3] & 255);
            pos2 = pos3 + 4;
        }
        return pos2 + delta;
    }

    private static int skipDelta(byte[] bytes, int pos) {
        int pos2 = pos + 1;
        int delta = bytes[pos] & 255;
        if (delta < 192) {
            return pos2;
        }
        if (delta < 240) {
            return pos2 + 1;
        }
        if (delta < 254) {
            return pos2 + 2;
        }
        return pos2 + (delta & 1) + 3;
    }

    static {
        f102assertionsDisabled = !BytesTrie.class.desiredAssertionStatus();
        valueResults_ = new Result[]{Result.INTERMEDIATE_VALUE, Result.FINAL_VALUE};
    }

    private Result branchNext(int pos, int length, int inByte) {
        int pos2;
        int delta;
        int pos3;
        Result result;
        if (length == 0) {
            length = this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            pos++;
        }
        int length2 = length + 1;
        while (true) {
            pos2 = pos;
            if (length2 <= 5) {
                break;
            }
            int pos4 = pos2 + 1;
            if (inByte < (this.bytes_[pos2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                length2 >>= 1;
                pos = jumpByDelta(this.bytes_, pos4);
            } else {
                length2 -= length2 >> 1;
                pos = skipDelta(this.bytes_, pos4);
            }
        }
        int pos5 = pos2;
        do {
            int pos6 = pos5 + 1;
            if (inByte == (this.bytes_[pos5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                int node = this.bytes_[pos6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                if (!f102assertionsDisabled) {
                    if (!(node >= 32)) {
                        throw new AssertionError();
                    }
                }
                if ((node & 1) != 0) {
                    result = Result.FINAL_VALUE;
                    pos3 = pos6;
                } else {
                    int pos7 = pos6 + 1;
                    int node2 = node >> 1;
                    if (node2 < 81) {
                        delta = node2 - 16;
                    } else if (node2 < 108) {
                        delta = ((node2 - 81) << 8) | (this.bytes_[pos7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        pos7++;
                    } else if (node2 < 126) {
                        delta = ((node2 - 108) << 16) | ((this.bytes_[pos7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (this.bytes_[pos7 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        pos7 += 2;
                    } else if (node2 == 126) {
                        delta = ((this.bytes_[pos7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((this.bytes_[pos7 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (this.bytes_[pos7 + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        pos7 += 3;
                    } else {
                        delta = (this.bytes_[pos7] << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) | ((this.bytes_[pos7 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((this.bytes_[pos7 + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (this.bytes_[pos7 + 3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        pos7 += 4;
                    }
                    pos3 = pos7 + delta;
                    int node3 = this.bytes_[pos3] & 255;
                    result = node3 >= 32 ? valueResults_[node3 & 1] : Result.NO_VALUE;
                }
                this.pos_ = pos3;
                return result;
            }
            length2--;
            pos5 = skipValue(this.bytes_, pos6);
        } while (length2 > 1);
        int pos8 = pos5 + 1;
        if (inByte == (this.bytes_[pos5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
            this.pos_ = pos8;
            int node4 = this.bytes_[pos8] & 255;
            return node4 >= 32 ? valueResults_[node4 & 1] : Result.NO_VALUE;
        }
        stop();
        return Result.NO_MATCH;
    }

    private Result nextImpl(int pos, int inByte) {
        int node;
        while (true) {
            int pos2 = pos + 1;
            int node2 = this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if (node2 < 16) {
                return branchNext(pos2, node2, inByte);
            }
            if (node2 < 32) {
                int length = node2 - 16;
                int pos3 = pos2 + 1;
                if (inByte == (this.bytes_[pos2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                    int length2 = length - 1;
                    this.remainingMatchLength_ = length2;
                    this.pos_ = pos3;
                    return (length2 >= 0 || (node = this.bytes_[pos3] & 255) < 32) ? Result.NO_VALUE : valueResults_[node & 1];
                }
            } else {
                if ((node2 & 1) != 0) {
                    break;
                }
                pos = skipValue(pos2, node2);
                if (!f102assertionsDisabled) {
                    if (!((this.bytes_[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 32)) {
                        throw new AssertionError();
                    }
                }
            }
        }
    }

    private static long findUniqueValueFromBranch(byte[] bytes, int pos, int length, long uniqueValue) {
        while (length > 5) {
            int pos2 = pos + 1;
            uniqueValue = findUniqueValueFromBranch(bytes, jumpByDelta(bytes, pos2), length >> 1, uniqueValue);
            if (uniqueValue == 0) {
                return 0L;
            }
            length -= length >> 1;
            pos = skipDelta(bytes, pos2);
        }
        do {
            int pos3 = pos + 1;
            int pos4 = pos3 + 1;
            int node = bytes[pos3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            boolean isFinal = (node & 1) != 0;
            int value = readValue(bytes, pos4, node >> 1);
            pos = skipValue(pos4, node);
            if (!isFinal) {
                uniqueValue = findUniqueValue(bytes, pos + value, uniqueValue);
                if (uniqueValue == 0) {
                    return 0L;
                }
            } else if (uniqueValue != 0) {
                if (value != ((int) (uniqueValue >> 1))) {
                    return 0L;
                }
            } else {
                uniqueValue = (((long) value) << 1) | 1;
            }
            length--;
        } while (length > 1);
        return (((long) (pos + 1)) << 33) | (8589934591L & uniqueValue);
    }

    private static long findUniqueValue(byte[] bytes, int pos, long uniqueValue) {
        int pos2;
        while (true) {
            int pos3 = pos + 1;
            int node = bytes[pos] & 255;
            if (node < 16) {
                if (node == 0) {
                    pos2 = pos3 + 1;
                    node = bytes[pos3] & 255;
                } else {
                    pos2 = pos3;
                }
                uniqueValue = findUniqueValueFromBranch(bytes, pos2, node + 1, uniqueValue);
                if (uniqueValue == 0) {
                    return 0L;
                }
                pos = (int) (uniqueValue >>> 33);
            } else if (node < 32) {
                pos = pos3 + (node - 16) + 1;
            } else {
                boolean isFinal = (node & 1) != 0;
                int value = readValue(bytes, pos3, node >> 1);
                if (uniqueValue != 0) {
                    if (value != ((int) (uniqueValue >> 1))) {
                        return 0L;
                    }
                } else {
                    uniqueValue = (((long) value) << 1) | 1;
                }
                if (isFinal) {
                    return uniqueValue;
                }
                pos = skipValue(pos3, node);
            }
        }
    }

    private static void getNextBranchBytes(byte[] bytes, int pos, int length, Appendable out) {
        while (length > 5) {
            int pos2 = pos + 1;
            getNextBranchBytes(bytes, jumpByDelta(bytes, pos2), length >> 1, out);
            length -= length >> 1;
            pos = skipDelta(bytes, pos2);
        }
        do {
            append(out, bytes[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            pos = skipValue(bytes, pos + 1);
            length--;
        } while (length > 1);
        append(out, bytes[pos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
    }

    private static void append(Appendable out, int c) {
        try {
            out.append((char) c);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
