package android.util;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import libcore.util.EmptyArray;

public class SparseLongArray implements Cloneable {
    private int[] mKeys;
    private int mSize;
    private long[] mValues;

    public SparseLongArray() {
        this(10);
    }

    public SparseLongArray(int i) {
        if (i == 0) {
            this.mKeys = EmptyArray.INT;
            this.mValues = EmptyArray.LONG;
        } else {
            this.mValues = ArrayUtils.newUnpaddedLongArray(i);
            this.mKeys = new int[this.mValues.length];
        }
        this.mSize = 0;
    }

    public SparseLongArray m38clone() {
        try {
            SparseLongArray sparseLongArray = (SparseLongArray) super.clone();
            try {
                sparseLongArray.mKeys = (int[]) this.mKeys.clone();
                sparseLongArray.mValues = (long[]) this.mValues.clone();
                return sparseLongArray;
            } catch (CloneNotSupportedException e) {
                return sparseLongArray;
            }
        } catch (CloneNotSupportedException e2) {
            return null;
        }
    }

    public long get(int i) {
        return get(i, 0L);
    }

    public long get(int i, long j) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
        if (iBinarySearch < 0) {
            return j;
        }
        return this.mValues[iBinarySearch];
    }

    public void delete(int i) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
        if (iBinarySearch >= 0) {
            removeAt(iBinarySearch);
        }
    }

    public void removeAtRange(int i, int i2) {
        int iMin = Math.min(i2, this.mSize - i);
        int i3 = i + iMin;
        System.arraycopy(this.mKeys, i3, this.mKeys, i, this.mSize - i3);
        System.arraycopy(this.mValues, i3, this.mValues, i, this.mSize - i3);
        this.mSize -= iMin;
    }

    public void removeAt(int i) {
        int i2 = i + 1;
        System.arraycopy(this.mKeys, i2, this.mKeys, i, this.mSize - i2);
        System.arraycopy(this.mValues, i2, this.mValues, i, this.mSize - i2);
        this.mSize--;
    }

    public void put(int i, long j) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
        if (iBinarySearch >= 0) {
            this.mValues[iBinarySearch] = j;
            return;
        }
        int i2 = ~iBinarySearch;
        this.mKeys = GrowingArrayUtils.insert(this.mKeys, this.mSize, i2, i);
        this.mValues = GrowingArrayUtils.insert(this.mValues, this.mSize, i2, j);
        this.mSize++;
    }

    public int size() {
        return this.mSize;
    }

    public int keyAt(int i) {
        return this.mKeys[i];
    }

    public long valueAt(int i) {
        return this.mValues[i];
    }

    public int indexOfKey(int i) {
        return ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
    }

    public int indexOfValue(long j) {
        for (int i = 0; i < this.mSize; i++) {
            if (this.mValues[i] == j) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        this.mSize = 0;
    }

    public void append(int i, long j) {
        if (this.mSize != 0 && i <= this.mKeys[this.mSize - 1]) {
            put(i, j);
            return;
        }
        this.mKeys = GrowingArrayUtils.append(this.mKeys, this.mSize, i);
        this.mValues = GrowingArrayUtils.append(this.mValues, this.mSize, j);
        this.mSize++;
    }

    public String toString() {
        if (size() <= 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder(this.mSize * 28);
        sb.append('{');
        for (int i = 0; i < this.mSize; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keyAt(i));
            sb.append('=');
            sb.append(valueAt(i));
        }
        sb.append('}');
        return sb.toString();
    }
}
