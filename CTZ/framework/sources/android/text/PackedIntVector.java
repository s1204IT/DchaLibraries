package android.text;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public class PackedIntVector {
    private final int mColumns;
    private int[] mValueGap;
    private int mRows = 0;
    private int mRowGapStart = 0;
    private int mRowGapLength = this.mRows;
    private int[] mValues = null;

    public PackedIntVector(int i) {
        this.mColumns = i;
        this.mValueGap = new int[2 * i];
    }

    public int getValue(int i, int i2) {
        int i3 = this.mColumns;
        if ((i | i2) < 0 || i >= size() || i2 >= i3) {
            throw new IndexOutOfBoundsException(i + ", " + i2);
        }
        if (i >= this.mRowGapStart) {
            i += this.mRowGapLength;
        }
        int i4 = this.mValues[(i * i3) + i2];
        int[] iArr = this.mValueGap;
        if (i >= iArr[i2]) {
            return i4 + iArr[i2 + i3];
        }
        return i4;
    }

    public void setValue(int i, int i2, int i3) {
        if ((i | i2) < 0 || i >= size() || i2 >= this.mColumns) {
            throw new IndexOutOfBoundsException(i + ", " + i2);
        }
        if (i >= this.mRowGapStart) {
            i += this.mRowGapLength;
        }
        int[] iArr = this.mValueGap;
        if (i >= iArr[i2]) {
            i3 -= iArr[this.mColumns + i2];
        }
        this.mValues[(i * this.mColumns) + i2] = i3;
    }

    private void setValueInternal(int i, int i2, int i3) {
        if (i >= this.mRowGapStart) {
            i += this.mRowGapLength;
        }
        int[] iArr = this.mValueGap;
        if (i >= iArr[i2]) {
            i3 -= iArr[this.mColumns + i2];
        }
        this.mValues[(i * this.mColumns) + i2] = i3;
    }

    public void adjustValuesBelow(int i, int i2, int i3) {
        if ((i | i2) < 0 || i > size() || i2 >= width()) {
            throw new IndexOutOfBoundsException(i + ", " + i2);
        }
        if (i >= this.mRowGapStart) {
            i += this.mRowGapLength;
        }
        moveValueGapTo(i2, i);
        int[] iArr = this.mValueGap;
        int i4 = i2 + this.mColumns;
        iArr[i4] = iArr[i4] + i3;
    }

    public void insertAt(int i, int[] iArr) {
        if (i < 0 || i > size()) {
            throw new IndexOutOfBoundsException("row " + i);
        }
        if (iArr != null && iArr.length < width()) {
            throw new IndexOutOfBoundsException("value count " + iArr.length);
        }
        moveRowGapTo(i);
        if (this.mRowGapLength == 0) {
            growBuffer();
        }
        this.mRowGapStart++;
        this.mRowGapLength--;
        if (iArr == null) {
            for (int i2 = this.mColumns - 1; i2 >= 0; i2--) {
                setValueInternal(i, i2, 0);
            }
            return;
        }
        for (int i3 = this.mColumns - 1; i3 >= 0; i3--) {
            setValueInternal(i, i3, iArr[i3]);
        }
    }

    public void deleteAt(int i, int i2) {
        int i3;
        if ((i | i2) < 0 || (i3 = i + i2) > size()) {
            throw new IndexOutOfBoundsException(i + ", " + i2);
        }
        moveRowGapTo(i3);
        this.mRowGapStart -= i2;
        this.mRowGapLength += i2;
    }

    public int size() {
        return this.mRows - this.mRowGapLength;
    }

    public int width() {
        return this.mColumns;
    }

    private final void growBuffer() {
        int i = this.mColumns;
        int[] iArrNewUnpaddedIntArray = ArrayUtils.newUnpaddedIntArray(GrowingArrayUtils.growSize(size()) * i);
        int length = iArrNewUnpaddedIntArray.length / i;
        int[] iArr = this.mValueGap;
        int i2 = this.mRowGapStart;
        int i3 = this.mRows - (this.mRowGapLength + i2);
        if (this.mValues != null) {
            System.arraycopy(this.mValues, 0, iArrNewUnpaddedIntArray, 0, i * i2);
            System.arraycopy(this.mValues, (this.mRows - i3) * i, iArrNewUnpaddedIntArray, (length - i3) * i, i3 * i);
        }
        for (int i4 = 0; i4 < i; i4++) {
            if (iArr[i4] >= i2) {
                iArr[i4] = iArr[i4] + (length - this.mRows);
                if (iArr[i4] < i2) {
                    iArr[i4] = i2;
                }
            }
        }
        this.mRowGapLength += length - this.mRows;
        this.mRows = length;
        this.mValues = iArrNewUnpaddedIntArray;
    }

    private final void moveValueGapTo(int i, int i2) {
        int[] iArr = this.mValueGap;
        int[] iArr2 = this.mValues;
        int i3 = this.mColumns;
        if (i2 == iArr[i]) {
            return;
        }
        if (i2 > iArr[i]) {
            for (int i4 = iArr[i]; i4 < i2; i4++) {
                int i5 = (i4 * i3) + i;
                iArr2[i5] = iArr2[i5] + iArr[i + i3];
            }
        } else {
            for (int i6 = i2; i6 < iArr[i]; i6++) {
                int i7 = (i6 * i3) + i;
                iArr2[i7] = iArr2[i7] - iArr[i + i3];
            }
        }
        iArr[i] = i2;
    }

    private final void moveRowGapTo(int i) {
        if (i == this.mRowGapStart) {
            return;
        }
        if (i > this.mRowGapStart) {
            int i2 = (this.mRowGapLength + i) - (this.mRowGapStart + this.mRowGapLength);
            int i3 = this.mColumns;
            int[] iArr = this.mValueGap;
            int[] iArr2 = this.mValues;
            int i4 = this.mRowGapStart + this.mRowGapLength;
            for (int i5 = i4; i5 < i4 + i2; i5++) {
                int i6 = (i5 - i4) + this.mRowGapStart;
                for (int i7 = 0; i7 < i3; i7++) {
                    int i8 = iArr2[(i5 * i3) + i7];
                    if (i5 >= iArr[i7]) {
                        i8 += iArr[i7 + i3];
                    }
                    if (i6 >= iArr[i7]) {
                        i8 -= iArr[i7 + i3];
                    }
                    iArr2[(i6 * i3) + i7] = i8;
                }
            }
        } else {
            int i9 = this.mRowGapStart - i;
            int i10 = this.mColumns;
            int[] iArr3 = this.mValueGap;
            int[] iArr4 = this.mValues;
            int i11 = this.mRowGapStart + this.mRowGapLength;
            for (int i12 = (i + i9) - 1; i12 >= i; i12--) {
                int i13 = ((i12 - i) + i11) - i9;
                for (int i14 = 0; i14 < i10; i14++) {
                    int i15 = iArr4[(i12 * i10) + i14];
                    if (i12 >= iArr3[i14]) {
                        i15 += iArr3[i14 + i10];
                    }
                    if (i13 >= iArr3[i14]) {
                        i15 -= iArr3[i14 + i10];
                    }
                    iArr4[(i13 * i10) + i14] = i15;
                }
            }
        }
        this.mRowGapStart = i;
    }
}
