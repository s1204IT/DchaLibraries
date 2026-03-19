package android.util;

import com.android.internal.app.DumpHeapActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import libcore.util.EmptyArray;

public class IntArray implements Cloneable {
    private static final int MIN_CAPACITY_INCREMENT = 12;
    private int mSize;
    private int[] mValues;

    private IntArray(int[] iArr, int i) {
        this.mValues = iArr;
        this.mSize = Preconditions.checkArgumentInRange(i, 0, iArr.length, DumpHeapActivity.KEY_SIZE);
    }

    public IntArray() {
        this(10);
    }

    public IntArray(int i) {
        if (i == 0) {
            this.mValues = EmptyArray.INT;
        } else {
            this.mValues = ArrayUtils.newUnpaddedIntArray(i);
        }
        this.mSize = 0;
    }

    public static IntArray wrap(int[] iArr) {
        return new IntArray(iArr, iArr.length);
    }

    public static IntArray fromArray(int[] iArr, int i) {
        return wrap(Arrays.copyOf(iArr, i));
    }

    public void resize(int i) {
        Preconditions.checkArgumentNonnegative(i);
        if (i <= this.mValues.length) {
            Arrays.fill(this.mValues, i, this.mValues.length, 0);
        } else {
            ensureCapacity(i - this.mSize);
        }
        this.mSize = i;
    }

    public void add(int i) {
        add(this.mSize, i);
    }

    public void add(int i, int i2) {
        ensureCapacity(1);
        int i3 = this.mSize - i;
        this.mSize++;
        checkBounds(i);
        if (i3 != 0) {
            System.arraycopy(this.mValues, i, this.mValues, i + 1, i3);
        }
        this.mValues[i] = i2;
    }

    public int binarySearch(int i) {
        return ContainerHelpers.binarySearch(this.mValues, this.mSize, i);
    }

    public void addAll(IntArray intArray) {
        int i = intArray.mSize;
        ensureCapacity(i);
        System.arraycopy(intArray.mValues, 0, this.mValues, this.mSize, i);
        this.mSize += i;
    }

    private void ensureCapacity(int i) {
        int i2 = this.mSize;
        int i3 = i + i2;
        if (i3 >= this.mValues.length) {
            int i4 = (i2 < 6 ? 12 : i2 >> 1) + i2;
            if (i4 > i3) {
                i3 = i4;
            }
            int[] iArrNewUnpaddedIntArray = ArrayUtils.newUnpaddedIntArray(i3);
            System.arraycopy(this.mValues, 0, iArrNewUnpaddedIntArray, 0, i2);
            this.mValues = iArrNewUnpaddedIntArray;
        }
    }

    public void clear() {
        this.mSize = 0;
    }

    public IntArray m31clone() throws CloneNotSupportedException {
        IntArray intArray = (IntArray) super.clone();
        intArray.mValues = (int[]) this.mValues.clone();
        return intArray;
    }

    public int get(int i) {
        checkBounds(i);
        return this.mValues[i];
    }

    public void set(int i, int i2) {
        checkBounds(i);
        this.mValues[i] = i2;
    }

    public int indexOf(int i) {
        int i2 = this.mSize;
        for (int i3 = 0; i3 < i2; i3++) {
            if (this.mValues[i3] == i) {
                return i3;
            }
        }
        return -1;
    }

    public void remove(int i) {
        checkBounds(i);
        System.arraycopy(this.mValues, i + 1, this.mValues, i, (this.mSize - i) - 1);
        this.mSize--;
    }

    public int size() {
        return this.mSize;
    }

    public int[] toArray() {
        return Arrays.copyOf(this.mValues, this.mSize);
    }

    private void checkBounds(int i) {
        if (i < 0 || this.mSize <= i) {
            throw new ArrayIndexOutOfBoundsException(this.mSize, i);
        }
    }
}
