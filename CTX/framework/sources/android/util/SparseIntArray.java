package android.util;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Arrays;
import libcore.util.EmptyArray;

public class SparseIntArray implements Cloneable {
    private int[] mKeys;
    private int mSize;
    private int[] mValues;

    public SparseIntArray() {
        this(10);
    }

    public SparseIntArray(int i) {
        if (i == 0) {
            this.mKeys = EmptyArray.INT;
            this.mValues = EmptyArray.INT;
        } else {
            this.mKeys = ArrayUtils.newUnpaddedIntArray(i);
            this.mValues = new int[this.mKeys.length];
        }
        this.mSize = 0;
    }

    public SparseIntArray m37clone() {
        try {
            SparseIntArray sparseIntArray = (SparseIntArray) super.clone();
            try {
                sparseIntArray.mKeys = (int[]) this.mKeys.clone();
                sparseIntArray.mValues = (int[]) this.mValues.clone();
                return sparseIntArray;
            } catch (CloneNotSupportedException e) {
                return sparseIntArray;
            }
        } catch (CloneNotSupportedException e2) {
            return null;
        }
    }

    public int get(int i) {
        return get(i, 0);
    }

    public int get(int i, int i2) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
        if (iBinarySearch < 0) {
            return i2;
        }
        return this.mValues[iBinarySearch];
    }

    public void delete(int i) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
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

    public void put(int i, int i2) {
        int iBinarySearch = ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
        if (iBinarySearch >= 0) {
            this.mValues[iBinarySearch] = i2;
            return;
        }
        int i3 = ~iBinarySearch;
        this.mKeys = GrowingArrayUtils.insert(this.mKeys, this.mSize, i3, i);
        this.mValues = GrowingArrayUtils.insert(this.mValues, this.mSize, i3, i2);
        this.mSize++;
    }

    public int size() {
        return this.mSize;
    }

    public int keyAt(int i) {
        return this.mKeys[i];
    }

    public int valueAt(int i) {
        return this.mValues[i];
    }

    public void setValueAt(int i, int i2) {
        this.mValues[i] = i2;
    }

    public int indexOfKey(int i) {
        return ContainerHelpers.binarySearch(this.mKeys, this.mSize, i);
    }

    public int indexOfValue(int i) {
        for (int i2 = 0; i2 < this.mSize; i2++) {
            if (this.mValues[i2] == i) {
                return i2;
            }
        }
        return -1;
    }

    public void clear() {
        this.mSize = 0;
    }

    public void append(int i, int i2) {
        if (this.mSize != 0 && i <= this.mKeys[this.mSize - 1]) {
            put(i, i2);
            return;
        }
        this.mKeys = GrowingArrayUtils.append(this.mKeys, this.mSize, i);
        this.mValues = GrowingArrayUtils.append(this.mValues, this.mSize, i2);
        this.mSize++;
    }

    public int[] copyKeys() {
        if (size() == 0) {
            return null;
        }
        return Arrays.copyOf(this.mKeys, size());
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
