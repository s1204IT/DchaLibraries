package android.icu.util;

import android.icu.lang.UCharacterEnums;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public final class BytesTrie implements Cloneable, Iterable<Entry> {
    static final boolean $assertionsDisabled = false;
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
    private static Result[] valueResults_ = {Result.INTERMEDIATE_VALUE, Result.FINAL_VALUE};
    private byte[] bytes_;
    private int pos_;
    private int remainingMatchLength_ = -1;
    private int root_;

    public BytesTrie(byte[] bArr, int i) {
        this.bytes_ = bArr;
        this.root_ = i;
        this.pos_ = i;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public BytesTrie reset() {
        this.pos_ = this.root_;
        this.remainingMatchLength_ = -1;
        return this;
    }

    public static final class State {
        private byte[] bytes;
        private int pos;
        private int remainingMatchLength;
        private int root;
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
        int i;
        int i2 = this.pos_;
        if (i2 < 0) {
            return Result.NO_MATCH;
        }
        return (this.remainingMatchLength_ >= 0 || (i = this.bytes_[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 32) ? Result.NO_VALUE : valueResults_[i & 1];
    }

    public Result first(int i) {
        this.remainingMatchLength_ = -1;
        if (i < 0) {
            i += 256;
        }
        return nextImpl(this.root_, i);
    }

    public Result next(int i) {
        int i2;
        int i3 = this.pos_;
        if (i3 < 0) {
            return Result.NO_MATCH;
        }
        if (i < 0) {
            i += 256;
        }
        int i4 = this.remainingMatchLength_;
        if (i4 >= 0) {
            int i5 = i3 + 1;
            if (i == (this.bytes_[i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                int i6 = i4 - 1;
                this.remainingMatchLength_ = i6;
                this.pos_ = i5;
                return (i6 >= 0 || (i2 = this.bytes_[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 32) ? Result.NO_VALUE : valueResults_[i2 & 1];
            }
            stop();
            return Result.NO_MATCH;
        }
        return nextImpl(i3, i);
    }

    public Result next(byte[] bArr, int i, int i2) {
        int i3;
        if (i >= i2) {
            return current();
        }
        int iSkipValue = this.pos_;
        if (iSkipValue < 0) {
            return Result.NO_MATCH;
        }
        int i4 = this.remainingMatchLength_;
        while (i != i2) {
            int i5 = i + 1;
            byte b = bArr[i];
            if (i4 < 0) {
                this.remainingMatchLength_ = i4;
                while (true) {
                    int i6 = iSkipValue + 1;
                    int i7 = this.bytes_[iSkipValue] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    if (i7 < 16) {
                        Result resultBranchNext = branchNext(i6, i7, b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        if (resultBranchNext == Result.NO_MATCH) {
                            return Result.NO_MATCH;
                        }
                        if (i5 == i2) {
                            return resultBranchNext;
                        }
                        if (resultBranchNext == Result.FINAL_VALUE) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        byte b2 = bArr[i5];
                        i5++;
                        b = b2;
                        iSkipValue = this.pos_;
                    } else if (i7 < 32) {
                        int i8 = i7 - 16;
                        if (b != this.bytes_[i6]) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        i4 = i8 - 1;
                        iSkipValue = i6 + 1;
                    } else {
                        if ((i7 & 1) != 0) {
                            stop();
                            return Result.NO_MATCH;
                        }
                        iSkipValue = skipValue(i6, i7);
                    }
                }
            } else {
                if (b != this.bytes_[iSkipValue]) {
                    stop();
                    return Result.NO_MATCH;
                }
                iSkipValue++;
                i4--;
            }
            i = i5;
        }
        this.remainingMatchLength_ = i4;
        this.pos_ = iSkipValue;
        return (i4 >= 0 || (i3 = this.bytes_[iSkipValue] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 32) ? Result.NO_VALUE : valueResults_[i3 & 1];
    }

    public int getValue() {
        int i = this.pos_;
        return readValue(this.bytes_, i + 1, (this.bytes_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) >> 1);
    }

    public long getUniqueValue() {
        int i = this.pos_;
        if (i < 0) {
            return 0L;
        }
        return (findUniqueValue(this.bytes_, (i + this.remainingMatchLength_) + 1, 0L) << 31) >> 31;
    }

    public int getNextBytes(Appendable appendable) {
        int i;
        int i2 = this.pos_;
        if (i2 < 0) {
            return 0;
        }
        if (this.remainingMatchLength_ >= 0) {
            append(appendable, this.bytes_[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            return 1;
        }
        int i3 = i2 + 1;
        int i4 = this.bytes_[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
        if (i4 >= 32) {
            if ((i4 & 1) != 0) {
                return 0;
            }
            int iSkipValue = skipValue(i3, i4);
            i3 = iSkipValue + 1;
            i4 = this.bytes_[iSkipValue] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
        }
        if (i4 < 16) {
            if (i4 == 0) {
                i = i3 + 1;
                i4 = this.bytes_[i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            } else {
                i = i3;
            }
            int i5 = i4 + 1;
            getNextBranchBytes(this.bytes_, i, i5, appendable);
            return i5;
        }
        append(appendable, this.bytes_[i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
        return 1;
    }

    @Override
    public java.util.Iterator<Entry> iterator2() {
        return new Iterator(this.bytes_, this.pos_, this.remainingMatchLength_, 0);
    }

    public Iterator iterator(int i) {
        return new Iterator(this.bytes_, this.pos_, this.remainingMatchLength_, i);
    }

    public static Iterator iterator(byte[] bArr, int i, int i2) {
        return new Iterator(bArr, i, -1, i2);
    }

    public static final class Entry {
        private byte[] bytes;
        private int length;
        public int value;

        private Entry(int i) {
            this.bytes = new byte[i];
        }

        public int bytesLength() {
            return this.length;
        }

        public byte byteAt(int i) {
            return this.bytes[i];
        }

        public void copyBytesTo(byte[] bArr, int i) {
            System.arraycopy(this.bytes, 0, bArr, i, this.length);
        }

        public ByteBuffer bytesAsByteBuffer() {
            return ByteBuffer.wrap(this.bytes, 0, this.length).asReadOnlyBuffer();
        }

        private void ensureCapacity(int i) {
            if (this.bytes.length < i) {
                byte[] bArr = new byte[Math.min(this.bytes.length * 2, 2 * i)];
                System.arraycopy(this.bytes, 0, bArr, 0, this.length);
                this.bytes = bArr;
            }
        }

        private void append(byte b) {
            ensureCapacity(this.length + 1);
            byte[] bArr = this.bytes;
            int i = this.length;
            this.length = i + 1;
            bArr[i] = b;
        }

        private void append(byte[] bArr, int i, int i2) {
            ensureCapacity(this.length + i2);
            System.arraycopy(bArr, i, this.bytes, this.length, i2);
            this.length += i2;
        }

        private void truncateString(int i) {
            this.length = i;
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

        private Iterator(byte[] bArr, int i, int i2, int i3) {
            this.stack_ = new ArrayList<>();
            this.bytes_ = bArr;
            this.initialPos_ = i;
            this.pos_ = i;
            this.initialRemainingMatchLength_ = i2;
            this.remainingMatchLength_ = i2;
            this.maxLength_ = i3;
            this.entry_ = new Entry(this.maxLength_ != 0 ? this.maxLength_ : 32);
            int i4 = this.remainingMatchLength_;
            if (i4 >= 0) {
                int i5 = i4 + 1;
                if (this.maxLength_ > 0 && i5 > this.maxLength_) {
                    i5 = this.maxLength_;
                }
                this.entry_.append(this.bytes_, this.pos_, i5);
                this.pos_ += i5;
                this.remainingMatchLength_ -= i5;
            }
        }

        public Iterator reset() {
            this.pos_ = this.initialPos_;
            this.remainingMatchLength_ = this.initialRemainingMatchLength_;
            int i = this.remainingMatchLength_ + 1;
            if (this.maxLength_ > 0 && i > this.maxLength_) {
                i = this.maxLength_;
            }
            this.entry_.truncateString(i);
            this.pos_ += i;
            this.remainingMatchLength_ -= i;
            this.stack_.clear();
            return this;
        }

        @Override
        public boolean hasNext() {
            return this.pos_ >= 0 || !this.stack_.isEmpty();
        }

        @Override
        public Entry next() {
            int i;
            int iBranchNext = this.pos_;
            if (iBranchNext < 0) {
                if (this.stack_.isEmpty()) {
                    throw new NoSuchElementException();
                }
                long jLongValue = this.stack_.remove(this.stack_.size() - 1).longValue();
                int i2 = (int) jLongValue;
                int i3 = (int) (jLongValue >> 32);
                this.entry_.truncateString(65535 & i2);
                int i4 = i2 >>> 16;
                if (i4 <= 1) {
                    this.entry_.append(this.bytes_[i3]);
                    iBranchNext = i3 + 1;
                } else {
                    iBranchNext = branchNext(i3, i4);
                    if (iBranchNext < 0) {
                        return this.entry_;
                    }
                }
            }
            if (this.remainingMatchLength_ >= 0) {
                return truncateAndStop();
            }
            while (true) {
                int i5 = iBranchNext + 1;
                int i6 = this.bytes_[iBranchNext] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                if (i6 >= 32) {
                    boolean z = (i6 & 1) != 0;
                    this.entry_.value = BytesTrie.readValue(this.bytes_, i5, i6 >> 1);
                    if (!z && (this.maxLength_ <= 0 || this.entry_.length != this.maxLength_)) {
                        this.pos_ = BytesTrie.skipValue(i5, i6);
                    } else {
                        this.pos_ = -1;
                    }
                    return this.entry_;
                }
                if (this.maxLength_ > 0 && this.entry_.length == this.maxLength_) {
                    return truncateAndStop();
                }
                if (i6 < 16) {
                    if (i6 == 0) {
                        i = i5 + 1;
                        i6 = this.bytes_[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    } else {
                        i = i5;
                    }
                    iBranchNext = branchNext(i, i6 + 1);
                    if (iBranchNext < 0) {
                        return this.entry_;
                    }
                } else {
                    int i7 = (i6 - 16) + 1;
                    if (this.maxLength_ <= 0 || this.entry_.length + i7 <= this.maxLength_) {
                        this.entry_.append(this.bytes_, i5, i7);
                        iBranchNext = i5 + i7;
                    } else {
                        this.entry_.append(this.bytes_, i5, this.maxLength_ - this.entry_.length);
                        return truncateAndStop();
                    }
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

        private int branchNext(int i, int i2) {
            boolean z;
            while (i2 > 5) {
                int i3 = i + 1;
                int i4 = i2 >> 1;
                this.stack_.add(Long.valueOf((((long) BytesTrie.skipDelta(this.bytes_, i3)) << 32) | ((long) ((i2 - i4) << 16)) | ((long) this.entry_.length)));
                i = BytesTrie.jumpByDelta(this.bytes_, i3);
                i2 = i4;
            }
            int i5 = i + 1;
            byte b = this.bytes_[i];
            int i6 = i5 + 1;
            int i7 = this.bytes_[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if ((i7 & 1) == 0) {
                z = false;
            } else {
                z = true;
            }
            int value = BytesTrie.readValue(this.bytes_, i6, i7 >> 1);
            int iSkipValue = BytesTrie.skipValue(i6, i7);
            this.stack_.add(Long.valueOf((((long) iSkipValue) << 32) | ((long) ((i2 - 1) << 16)) | ((long) this.entry_.length)));
            this.entry_.append(b);
            if (z) {
                this.pos_ = -1;
                this.entry_.value = value;
                return -1;
            }
            return iSkipValue + value;
        }
    }

    private void stop() {
        this.pos_ = -1;
    }

    private static int readValue(byte[] bArr, int i, int i2) {
        if (i2 < 81) {
            return i2 - 16;
        }
        if (i2 < 108) {
            return ((i2 - 81) << 8) | (bArr[i] & 255);
        }
        if (i2 < 126) {
            return ((i2 - 108) << 16) | ((bArr[i] & 255) << 8) | (bArr[i + 1] & 255);
        }
        if (i2 == 126) {
            return ((bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bArr[i + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (bArr[i + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
        }
        return (bArr[i] << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) | ((bArr[i + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bArr[i + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (bArr[i + 3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
    }

    private static int skipValue(int i, int i2) {
        if (i2 >= 162) {
            if (i2 < 216) {
                return i + 1;
            }
            if (i2 < 252) {
                return i + 2;
            }
            return i + 3 + ((i2 >> 1) & 1);
        }
        return i;
    }

    private static int skipValue(byte[] bArr, int i) {
        return skipValue(i + 1, bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
    }

    private static int jumpByDelta(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = bArr[i] & 255;
        if (i3 >= 192) {
            if (i3 < 240) {
                i3 = ((i3 - 192) << 8) | (bArr[i2] & 255);
                i2++;
            } else if (i3 < 254) {
                i3 = ((i3 - 240) << 16) | ((bArr[i2] & 255) << 8) | (bArr[i2 + 1] & 255);
                i2 += 2;
            } else if (i3 == 254) {
                i3 = ((bArr[i2] & 255) << 16) | ((bArr[i2 + 1] & 255) << 8) | (bArr[i2 + 2] & 255);
                i2 += 3;
            } else {
                i3 = (bArr[i2] << 24) | ((bArr[i2 + 1] & 255) << 16) | ((bArr[i2 + 2] & 255) << 8) | (bArr[i2 + 3] & 255);
                i2 += 4;
            }
        }
        return i2 + i3;
    }

    private static int skipDelta(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = bArr[i] & 255;
        if (i3 >= 192) {
            if (i3 < 240) {
                return i2 + 1;
            }
            if (i3 < 254) {
                return i2 + 2;
            }
            return i2 + 3 + (i3 & 1);
        }
        return i2;
    }

    private Result branchNext(int i, int i2, int i3) {
        int i4;
        Result result;
        if (i2 == 0) {
            i2 = this.bytes_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            i++;
        }
        int i5 = i2 + 1;
        while (i5 > 5) {
            int i6 = i + 1;
            if (i3 < (this.bytes_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                i5 >>= 1;
                i = jumpByDelta(this.bytes_, i6);
            } else {
                i5 -= i5 >> 1;
                i = skipDelta(this.bytes_, i6);
            }
        }
        do {
            int i7 = i + 1;
            if (i3 == (this.bytes_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                int i8 = this.bytes_[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                if ((i8 & 1) != 0) {
                    result = Result.FINAL_VALUE;
                } else {
                    int i9 = i7 + 1;
                    int i10 = i8 >> 1;
                    if (i10 < 81) {
                        i4 = i10 - 16;
                    } else if (i10 < 108) {
                        i4 = ((i10 - 81) << 8) | (this.bytes_[i9] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        i9++;
                    } else if (i10 < 126) {
                        i4 = ((i10 - 108) << 16) | ((this.bytes_[i9] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (this.bytes_[i9 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        i9 += 2;
                    } else if (i10 == 126) {
                        i4 = ((this.bytes_[i9] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((this.bytes_[i9 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (this.bytes_[i9 + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        i9 += 3;
                    } else {
                        i4 = (this.bytes_[i9] << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) | ((this.bytes_[i9 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((this.bytes_[i9 + 2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | (this.bytes_[i9 + 3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
                        i9 += 4;
                    }
                    i7 = i9 + i4;
                    int i11 = this.bytes_[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    result = i11 >= 32 ? valueResults_[i11 & 1] : Result.NO_VALUE;
                }
                this.pos_ = i7;
                return result;
            }
            i5--;
            i = skipValue(this.bytes_, i7);
        } while (i5 > 1);
        int i12 = i + 1;
        if (i3 == (this.bytes_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
            this.pos_ = i12;
            int i13 = this.bytes_[i12] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            return i13 >= 32 ? valueResults_[i13 & 1] : Result.NO_VALUE;
        }
        stop();
        return Result.NO_MATCH;
    }

    private Result nextImpl(int i, int i2) {
        int i3;
        while (true) {
            int i4 = i + 1;
            int i5 = this.bytes_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if (i5 < 16) {
                return branchNext(i4, i5, i2);
            }
            if (i5 < 32) {
                int i6 = i5 - 16;
                int i7 = i4 + 1;
                if (i2 == (this.bytes_[i4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) {
                    int i8 = i6 - 1;
                    this.remainingMatchLength_ = i8;
                    this.pos_ = i7;
                    return (i8 >= 0 || (i3 = this.bytes_[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) < 32) ? Result.NO_VALUE : valueResults_[i3 & 1];
                }
            } else {
                if ((i5 & 1) != 0) {
                    break;
                }
                i = skipValue(i4, i5);
            }
        }
    }

    private static long findUniqueValueFromBranch(byte[] bArr, int i, int i2, long j) {
        boolean z;
        while (i2 > 5) {
            int i3 = i + 1;
            int i4 = i2 >> 1;
            j = findUniqueValueFromBranch(bArr, jumpByDelta(bArr, i3), i4, j);
            if (j == 0) {
                return 0L;
            }
            i2 -= i4;
            i = skipDelta(bArr, i3);
        }
        do {
            int i5 = i + 1;
            int i6 = i5 + 1;
            int i7 = bArr[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if ((i7 & 1) == 0) {
                z = false;
            } else {
                z = true;
            }
            int value = readValue(bArr, i6, i7 >> 1);
            i = skipValue(i6, i7);
            if (!z) {
                j = findUniqueValue(bArr, value + i, j);
                if (j == 0) {
                    return 0L;
                }
            } else if (j == 0) {
                j = (((long) value) << 1) | 1;
            } else if (value != ((int) (j >> 1))) {
                return 0L;
            }
            i2--;
        } while (i2 > 1);
        return (((long) (i + 1)) << 33) | (j & 8589934591L);
    }

    private static long findUniqueValue(byte[] bArr, int i, long j) {
        int i2;
        int i3;
        boolean z;
        while (true) {
            int i4 = i + 1;
            int i5 = bArr[i] & 255;
            if (i5 < 16) {
                if (i5 == 0) {
                    i3 = i4 + 1;
                    i2 = bArr[i4] & 255;
                } else {
                    i2 = i5;
                    i3 = i4;
                }
                long jFindUniqueValueFromBranch = findUniqueValueFromBranch(bArr, i3, i2 + 1, j);
                if (jFindUniqueValueFromBranch == 0) {
                    return 0L;
                }
                i = (int) (jFindUniqueValueFromBranch >>> 33);
                j = jFindUniqueValueFromBranch;
            } else if (i5 < 32) {
                i = i4 + (i5 - 16) + 1;
            } else {
                if ((i5 & 1) == 0) {
                    z = false;
                } else {
                    z = true;
                }
                int value = readValue(bArr, i4, i5 >> 1);
                if (j != 0) {
                    if (value != ((int) (j >> 1))) {
                        return 0L;
                    }
                } else {
                    j = (((long) value) << 1) | 1;
                }
                if (z) {
                    return j;
                }
                i = skipValue(i4, i5);
            }
        }
    }

    private static void getNextBranchBytes(byte[] bArr, int i, int i2, Appendable appendable) {
        while (i2 > 5) {
            int i3 = i + 1;
            int i4 = i2 >> 1;
            getNextBranchBytes(bArr, jumpByDelta(bArr, i3), i4, appendable);
            i2 -= i4;
            i = skipDelta(bArr, i3);
        }
        do {
            append(appendable, bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            i = skipValue(bArr, i + 1);
            i2--;
        } while (i2 > 1);
        append(appendable, bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
    }

    private static void append(Appendable appendable, int i) {
        try {
            appendable.append((char) i);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
