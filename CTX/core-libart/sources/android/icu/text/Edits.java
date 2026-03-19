package android.icu.text;

import java.nio.BufferOverflowException;
import java.util.Arrays;

public final class Edits {
    private static final int LENGTH_IN_1TRAIL = 61;
    private static final int LENGTH_IN_2TRAIL = 62;
    private static final int MAX_SHORT_CHANGE = 28671;
    private static final int MAX_SHORT_CHANGE_NEW_LENGTH = 7;
    private static final int MAX_SHORT_CHANGE_OLD_LENGTH = 6;
    private static final int MAX_UNCHANGED = 4095;
    private static final int MAX_UNCHANGED_LENGTH = 4096;
    private static final int SHORT_CHANGE_NUM_MASK = 511;
    private static final int STACK_CAPACITY = 100;
    private char[] array = new char[100];
    private int delta;
    private int length;
    private int numChanges;

    public void reset() {
        this.numChanges = 0;
        this.delta = 0;
        this.length = 0;
    }

    private void setLastUnit(int i) {
        this.array[this.length - 1] = (char) i;
    }

    private int lastUnit() {
        return this.length > 0 ? this.array[this.length - 1] : DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
    }

    public void addUnchanged(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("addUnchanged(" + i + "): length must not be negative");
        }
        int iLastUnit = lastUnit();
        if (iLastUnit < 4095) {
            int i2 = 4095 - iLastUnit;
            if (i2 >= i) {
                setLastUnit(iLastUnit + i);
                return;
            } else {
                setLastUnit(4095);
                i -= i2;
            }
        }
        while (i >= 4096) {
            append(4095);
            i -= 4096;
        }
        if (i > 0) {
            append(i - 1);
        }
    }

    public void addReplace(int i, int i2) {
        int i3;
        int i4;
        if (i < 0 || i2 < 0) {
            throw new IllegalArgumentException("addReplace(" + i + ", " + i2 + "): both lengths must be non-negative");
        }
        if (i == 0 && i2 == 0) {
            return;
        }
        this.numChanges++;
        int i5 = i2 - i;
        if (i5 != 0) {
            if ((i5 > 0 && this.delta >= 0 && i5 > Integer.MAX_VALUE - this.delta) || (i5 < 0 && this.delta < 0 && i5 < Integer.MIN_VALUE - this.delta)) {
                throw new IndexOutOfBoundsException();
            }
            this.delta += i5;
        }
        if (i > 0 && i <= 6 && i2 <= 7) {
            int i6 = (i << 12) | (i2 << 9);
            int iLastUnit = lastUnit();
            if (4095 < iLastUnit && iLastUnit < MAX_SHORT_CHANGE && (iLastUnit & (-512)) == i6 && (iLastUnit & 511) < 511) {
                setLastUnit(iLastUnit + 1);
                return;
            } else {
                append(i6);
                return;
            }
        }
        if (i < 61 && i2 < 61) {
            append((i << 6) | 28672 | i2);
            return;
        }
        if (this.array.length - this.length >= 5 || growArray()) {
            int i7 = this.length + 1;
            if (i < 61) {
                i3 = (i << 6) | 28672;
            } else if (i > 32767) {
                i3 = (((i >> 30) + 62) << 6) | 28672;
                int i8 = i7 + 1;
                this.array[i7] = (char) ((i >> 15) | 32768);
                i7 = i8 + 1;
                this.array[i8] = (char) (i | 32768);
            } else {
                i3 = 32576;
                this.array[i7] = (char) (i | 32768);
                i7++;
            }
            if (i2 < 61) {
                i4 = i3 | i2;
            } else if (i2 > 32767) {
                i4 = i3 | (62 + (i2 >> 30));
                int i9 = i7 + 1;
                this.array[i7] = (char) ((i2 >> 15) | 32768);
                i7 = i9 + 1;
                this.array[i9] = (char) (i2 | 32768);
            } else {
                i4 = i3 | 61;
                this.array[i7] = (char) (i2 | 32768);
                i7++;
            }
            this.array[this.length] = (char) i4;
            this.length = i7;
        }
    }

    private void append(int i) {
        if (this.length < this.array.length || growArray()) {
            char[] cArr = this.array;
            int i2 = this.length;
            this.length = i2 + 1;
            cArr[i2] = (char) i;
        }
    }

    private boolean growArray() {
        int length = Integer.MAX_VALUE;
        if (this.array.length == 100) {
            length = 2000;
        } else {
            if (this.array.length == Integer.MAX_VALUE) {
                throw new BufferOverflowException();
            }
            if (this.array.length < 1073741823) {
                length = this.array.length * 2;
            }
        }
        if (length - this.array.length < 5) {
            throw new BufferOverflowException();
        }
        this.array = Arrays.copyOf(this.array, length);
        return true;
    }

    public int lengthDelta() {
        return this.delta;
    }

    public boolean hasChanges() {
        return this.numChanges != 0;
    }

    public int numberOfChanges() {
        return this.numChanges;
    }

    public static final class Iterator {
        static final boolean $assertionsDisabled = false;
        private final char[] array;
        private boolean changed;
        private final boolean coarse;
        private int destIndex;
        private int dir;
        private int index;
        private final int length;
        private int newLength_;
        private int oldLength_;
        private final boolean onlyChanges_;
        private int remaining;
        private int replIndex;
        private int srcIndex;

        private Iterator(char[] cArr, int i, boolean z, boolean z2) {
            this.array = cArr;
            this.length = i;
            this.onlyChanges_ = z;
            this.coarse = z2;
        }

        private int readLength(int i) {
            if (i < 61) {
                return i;
            }
            if (i < 62) {
                char[] cArr = this.array;
                int i2 = this.index;
                this.index = i2 + 1;
                return cArr[i2] & 32767;
            }
            int i3 = ((i & 1) << 30) | ((this.array[this.index] & 32767) << 15) | (this.array[this.index + 1] & 32767);
            this.index += 2;
            return i3;
        }

        private void updateNextIndexes() {
            this.srcIndex += this.oldLength_;
            if (this.changed) {
                this.replIndex += this.newLength_;
            }
            this.destIndex += this.newLength_;
        }

        private void updatePreviousIndexes() {
            this.srcIndex -= this.oldLength_;
            if (this.changed) {
                this.replIndex -= this.newLength_;
            }
            this.destIndex -= this.newLength_;
        }

        private boolean noNext() {
            this.dir = 0;
            this.changed = false;
            this.newLength_ = 0;
            this.oldLength_ = 0;
            return false;
        }

        public boolean next() {
            return next(this.onlyChanges_);
        }

        private boolean next(boolean z) {
            char c;
            if (this.dir > 0) {
                updateNextIndexes();
            } else {
                if (this.dir < 0 && this.remaining > 0) {
                    this.index++;
                    this.dir = 1;
                    return true;
                }
                this.dir = 1;
            }
            if (this.remaining >= 1) {
                if (this.remaining > 1) {
                    this.remaining--;
                    return true;
                }
                this.remaining = 0;
            }
            if (this.index >= this.length) {
                return noNext();
            }
            char[] cArr = this.array;
            int i = this.index;
            this.index = i + 1;
            char c2 = cArr[i];
            if (c2 <= 4095) {
                this.changed = false;
                this.oldLength_ = c2 + 1;
                while (this.index < this.length && (c2 = this.array[this.index]) <= 4095) {
                    this.index++;
                    this.oldLength_ += c2 + 1;
                }
                this.newLength_ = this.oldLength_;
                if (!z) {
                    return true;
                }
                updateNextIndexes();
                if (this.index >= this.length) {
                    return noNext();
                }
                this.index++;
            }
            this.changed = true;
            if (c2 <= Edits.MAX_SHORT_CHANGE) {
                int i2 = c2 >> '\f';
                int i3 = (c2 >> '\t') & 7;
                int i4 = (c2 & 511) + 1;
                if (this.coarse) {
                    this.oldLength_ = i2 * i4;
                    this.newLength_ = i4 * i3;
                } else {
                    this.oldLength_ = i2;
                    this.newLength_ = i3;
                    if (i4 > 1) {
                        this.remaining = i4;
                    }
                    return true;
                }
            } else {
                this.oldLength_ = readLength((c2 >> 6) & 63);
                this.newLength_ = readLength(c2 & '?');
                if (!this.coarse) {
                    return true;
                }
            }
            while (this.index < this.length && (c = this.array[this.index]) > 4095) {
                this.index++;
                if (c <= Edits.MAX_SHORT_CHANGE) {
                    int i5 = (c & 511) + 1;
                    this.oldLength_ += (c >> '\f') * i5;
                    this.newLength_ += ((c >> '\t') & 7) * i5;
                } else {
                    this.oldLength_ += readLength((c >> 6) & 63);
                    this.newLength_ += readLength(c & '?');
                }
            }
            return true;
        }

        private boolean previous() {
            char c;
            char c2;
            char c3;
            if (this.dir >= 0) {
                if (this.dir > 0) {
                    if (this.remaining > 0) {
                        this.index--;
                        this.dir = -1;
                        return true;
                    }
                    updateNextIndexes();
                }
                this.dir = -1;
            }
            if (this.remaining > 0) {
                if (this.remaining <= (this.array[this.index] & 511)) {
                    this.remaining++;
                    updatePreviousIndexes();
                    return true;
                }
                this.remaining = 0;
            }
            if (this.index <= 0) {
                return noNext();
            }
            char[] cArr = this.array;
            int i = this.index - 1;
            this.index = i;
            char c4 = cArr[i];
            if (c4 <= 4095) {
                this.changed = false;
                this.oldLength_ = c4 + 1;
                while (this.index > 0 && (c3 = this.array[this.index - 1]) <= 4095) {
                    this.index--;
                    this.oldLength_ += c3 + 1;
                }
                this.newLength_ = this.oldLength_;
                updatePreviousIndexes();
                return true;
            }
            this.changed = true;
            if (c4 <= Edits.MAX_SHORT_CHANGE) {
                int i2 = c4 >> '\f';
                int i3 = (c4 >> '\t') & 7;
                int i4 = (c4 & 511) + 1;
                if (this.coarse) {
                    this.oldLength_ = i2 * i4;
                    this.newLength_ = i4 * i3;
                } else {
                    this.oldLength_ = i2;
                    this.newLength_ = i3;
                    if (i4 > 1) {
                        this.remaining = 1;
                    }
                    updatePreviousIndexes();
                    return true;
                }
            } else {
                if (c4 <= 32767) {
                    this.oldLength_ = readLength((c4 >> 6) & 63);
                    this.newLength_ = readLength(c4 & '?');
                } else {
                    do {
                        char[] cArr2 = this.array;
                        int i5 = this.index - 1;
                        this.index = i5;
                        c = cArr2[i5];
                    } while (c > 32767);
                    int i6 = this.index;
                    this.index = i6 + 1;
                    this.oldLength_ = readLength((c >> 6) & 63);
                    this.newLength_ = readLength(c & '?');
                    this.index = i6;
                }
                if (!this.coarse) {
                    updatePreviousIndexes();
                    return true;
                }
            }
            while (this.index > 0 && (c2 = this.array[this.index - 1]) > 4095) {
                this.index--;
                if (c2 <= Edits.MAX_SHORT_CHANGE) {
                    int i7 = (c2 & 511) + 1;
                    this.oldLength_ += (c2 >> '\f') * i7;
                    this.newLength_ += ((c2 >> '\t') & 7) * i7;
                } else if (c2 <= 32767) {
                    int i8 = this.index;
                    this.index = i8 + 1;
                    this.oldLength_ += readLength((c2 >> 6) & 63);
                    this.newLength_ += readLength(c2 & '?');
                    this.index = i8;
                }
            }
            updatePreviousIndexes();
            return true;
        }

        public boolean findSourceIndex(int i) {
            return findIndex(i, true) == 0;
        }

        public boolean findDestinationIndex(int i) {
            return findIndex(i, false) == 0;
        }

        private int findIndex(int i, boolean z) {
            int i2;
            int i3;
            int i4;
            int i5;
            if (i < 0) {
                return -1;
            }
            if (z) {
                i2 = this.srcIndex;
                i3 = this.oldLength_;
            } else {
                i2 = this.destIndex;
                i3 = this.newLength_;
            }
            if (i < i2) {
                if (i >= i2 / 2) {
                    while (true) {
                        previous();
                        int i6 = z ? this.srcIndex : this.destIndex;
                        if (i >= i6) {
                            return 0;
                        }
                        if (this.remaining > 0) {
                            int i7 = z ? this.oldLength_ : this.newLength_;
                            int i8 = ((this.array[this.index] & 511) + 1) - this.remaining;
                            if (i >= i6 - (i8 * i7)) {
                                int i9 = (((i6 - i) - 1) / i7) + 1;
                                this.srcIndex -= this.oldLength_ * i9;
                                this.replIndex -= this.newLength_ * i9;
                                this.destIndex -= this.newLength_ * i9;
                                this.remaining += i9;
                                return 0;
                            }
                            this.srcIndex -= this.oldLength_ * i8;
                            this.replIndex -= this.newLength_ * i8;
                            this.destIndex -= i8 * this.newLength_;
                            this.remaining = 0;
                        }
                    }
                } else {
                    this.dir = 0;
                    this.destIndex = 0;
                    this.replIndex = 0;
                    this.srcIndex = 0;
                    this.newLength_ = 0;
                    this.oldLength_ = 0;
                    this.remaining = 0;
                    this.index = 0;
                }
            } else if (i < i2 + i3) {
                return 0;
            }
            while (next(false)) {
                if (z) {
                    i4 = this.srcIndex;
                    i5 = this.oldLength_;
                } else {
                    i4 = this.destIndex;
                    i5 = this.newLength_;
                }
                if (i < i4 + i5) {
                    return 0;
                }
                if (this.remaining > 1) {
                    if (i < (this.remaining * i5) + i4) {
                        int i10 = (i - i4) / i5;
                        this.srcIndex += this.oldLength_ * i10;
                        this.replIndex += this.newLength_ * i10;
                        this.destIndex += this.newLength_ * i10;
                        this.remaining -= i10;
                        return 0;
                    }
                    this.oldLength_ *= this.remaining;
                    this.newLength_ *= this.remaining;
                    this.remaining = 0;
                }
            }
            return 1;
        }

        public int destinationIndexFromSourceIndex(int i) {
            int iFindIndex = findIndex(i, true);
            if (iFindIndex < 0) {
                return 0;
            }
            if (iFindIndex > 0 || i == this.srcIndex) {
                return this.destIndex;
            }
            if (this.changed) {
                return this.destIndex + this.newLength_;
            }
            return this.destIndex + (i - this.srcIndex);
        }

        public int sourceIndexFromDestinationIndex(int i) {
            int iFindIndex = findIndex(i, false);
            if (iFindIndex < 0) {
                return 0;
            }
            if (iFindIndex > 0 || i == this.destIndex) {
                return this.srcIndex;
            }
            if (this.changed) {
                return this.srcIndex + this.oldLength_;
            }
            return this.srcIndex + (i - this.destIndex);
        }

        public boolean hasChange() {
            return this.changed;
        }

        public int oldLength() {
            return this.oldLength_;
        }

        public int newLength() {
            return this.newLength_;
        }

        public int sourceIndex() {
            return this.srcIndex;
        }

        public int replacementIndex() {
            return this.replIndex;
        }

        public int destinationIndex() {
            return this.destIndex;
        }
    }

    public Iterator getCoarseChangesIterator() {
        return new Iterator(this.array, this.length, true, true);
    }

    public Iterator getCoarseIterator() {
        return new Iterator(this.array, this.length, false, true);
    }

    public Iterator getFineChangesIterator() {
        return new Iterator(this.array, this.length, true, false);
    }

    public Iterator getFineIterator() {
        return new Iterator(this.array, this.length, false, false);
    }

    public Edits mergeAndAppend(Edits edits, Edits edits2) {
        Iterator fineIterator = edits.getFineIterator();
        Iterator fineIterator2 = edits2.getFineIterator();
        boolean next = true;
        boolean next2 = true;
        int iOldLength = 0;
        int iNewLength = 0;
        int i = 0;
        int i2 = 0;
        int iOldLength2 = 0;
        int iNewLength2 = 0;
        while (true) {
            if (iOldLength == 0 && next) {
                next = fineIterator2.next();
                if (next) {
                    iOldLength = fineIterator2.oldLength();
                    iNewLength2 = fineIterator2.newLength();
                    if (iOldLength == 0) {
                        if (iNewLength == 0 || !fineIterator.hasChange()) {
                            addReplace(i, i2 + iNewLength2);
                            i = 0;
                            i2 = i;
                        } else {
                            i2 += iNewLength2;
                        }
                    }
                }
            }
            if (iNewLength == 0) {
                if (!next2 || !(next2 = fineIterator.next())) {
                    break;
                }
                iOldLength2 = fineIterator.oldLength();
                iNewLength = fineIterator.newLength();
                if (iNewLength == 0) {
                    if (iOldLength == fineIterator2.oldLength() || !fineIterator2.hasChange()) {
                        addReplace(i + iOldLength2, i2);
                        i = 0;
                        i2 = i;
                    } else {
                        i += iOldLength2;
                    }
                }
            }
            if (iOldLength == 0) {
                throw new IllegalArgumentException("The bc input string is shorter than the ab output string.");
            }
            if (!fineIterator.hasChange() && !fineIterator2.hasChange()) {
                if (i != 0 || i2 != 0) {
                    addReplace(i, i2);
                    i = 0;
                    i2 = 0;
                }
                int i3 = iOldLength2 <= iNewLength2 ? iOldLength2 : iNewLength2;
                addUnchanged(i3);
                iOldLength2 -= i3;
                iNewLength2 -= i3;
                iNewLength = iOldLength2;
            } else if (fineIterator.hasChange() || !fineIterator2.hasChange()) {
                if (!fineIterator.hasChange() || fineIterator2.hasChange()) {
                    if (iNewLength == iOldLength) {
                        addReplace(i + iOldLength2, i2 + iNewLength2);
                        iOldLength = 0;
                        iNewLength = 0;
                        i = 0;
                        i2 = i;
                    } else {
                        i += iOldLength2;
                        i2 += iNewLength2;
                        if (iNewLength < iOldLength) {
                            iOldLength -= iNewLength;
                            iNewLength = 0;
                            iNewLength2 = 0;
                        } else {
                            iNewLength -= iOldLength;
                            iOldLength = 0;
                            iOldLength2 = 0;
                        }
                    }
                } else if (iNewLength <= iOldLength) {
                    addReplace(i + iOldLength2, i2 + iNewLength);
                    iNewLength2 = iOldLength - iNewLength;
                    iNewLength = 0;
                    i = 0;
                    i2 = 0;
                } else {
                    i += iOldLength2;
                    i2 += iNewLength2;
                    if (iNewLength < iOldLength) {
                    }
                }
            } else if (iNewLength >= iOldLength) {
                addReplace(i + iOldLength, i2 + iNewLength2);
                iOldLength2 = iNewLength - iOldLength;
                iOldLength = 0;
                i = 0;
                i2 = 0;
                iNewLength = iOldLength2;
            } else {
                i += iOldLength2;
                i2 += iNewLength2;
                if (iNewLength < iOldLength) {
                }
            }
            iOldLength = iNewLength2;
        }
    }
}
