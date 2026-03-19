package android.util;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import libcore.util.EmptyArray;

public class LongSparseLongArray implements Cloneable {
    private long[] mKeys;
    private int mSize;
    private long[] mValues;

    public LongSparseLongArray() {
        this(10);
    }

    public LongSparseLongArray(int i) {
        if (i == 0) {
            this.mKeys = EmptyArray.LONG;
            this.mValues = EmptyArray.LONG;
        } else {
            this.mKeys = ArrayUtils.newUnpaddedLongArray(i);
            this.mValues = new long[this.mKeys.length];
        }
        this.mSize = 0;
    }

    public LongSparseLongArray m34clone() {
        try {
            LongSparseLongArray longSparseLongArray = (LongSparseLongArray) super.clone();
            try {
                longSparseLongArray.mKeys = (long[]) this.mKeys.clone();
                longSparseLongArray.mValues = (long[]) this.mValues.clone();
                return longSparseLongArray;
            } catch (CloneNotSupportedException e) {
                return longSparseLongArray;
            }
        } catch (CloneNotSupportedException e2) {
            return null;
        }
    }

    public long get(long j) {
        return get(j, 0L);
    }

    public long get(long j, long j2) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, j);
        if (iBinarySearch < 0) {
            return j2;
        }
        return this.mValues[iBinarySearch];
    }

    public void delete(long j) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, j);
        if (iBinarySearch >= 0) {
            removeAt(iBinarySearch);
        }
    }

    public void removeAt(int i) {
        int i2 = i + 1;
        System.arraycopy(this.mKeys, i2, this.mKeys, i, this.mSize - i2);
        System.arraycopy(this.mValues, i2, this.mValues, i, this.mSize - i2);
        this.mSize--;
    }

    public void put(long j, long j2) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, j);
        if (iBinarySearch >= 0) {
            this.mValues[iBinarySearch] = j2;
            return;
        }
        int i = ~iBinarySearch;
        this.mKeys = GrowingArrayUtils.insert(this.mKeys, this.mSize, i, j);
        this.mValues = GrowingArrayUtils.insert(this.mValues, this.mSize, i, j2);
        this.mSize++;
    }

    public int size() {
        return this.mSize;
    }

    public long keyAt(int i) {
        return this.mKeys[i];
    }

    public long valueAt(int i) {
        return this.mValues[i];
    }

    public int indexOfKey(long j) {
        return ContainerHelpers.binarySearch(this.mKeys, this.mSize, j);
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

    public void append(long j, long j2) {
        if (this.mSize != 0 && j <= this.mKeys[this.mSize - 1]) {
            put(j, j2);
            return;
        }
        this.mKeys = GrowingArrayUtils.append(this.mKeys, this.mSize, j);
        this.mValues = GrowingArrayUtils.append(this.mValues, this.mSize, j2);
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
