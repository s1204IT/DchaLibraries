package android.util;

import com.android.internal.app.DumpHeapActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import libcore.util.EmptyArray;

public class LongArray implements Cloneable {
    private static final int MIN_CAPACITY_INCREMENT = 12;
    private int mSize;
    private long[] mValues;

    private LongArray(long[] jArr, int i) {
        this.mValues = jArr;
        this.mSize = Preconditions.checkArgumentInRange(i, 0, jArr.length, DumpHeapActivity.KEY_SIZE);
    }

    public LongArray() {
        this(10);
    }

    public LongArray(int i) {
        if (i == 0) {
            this.mValues = EmptyArray.LONG;
        } else {
            this.mValues = ArrayUtils.newUnpaddedLongArray(i);
        }
        this.mSize = 0;
    }

    public static LongArray wrap(long[] jArr) {
        return new LongArray(jArr, jArr.length);
    }

    public static LongArray fromArray(long[] jArr, int i) {
        return wrap(Arrays.copyOf(jArr, i));
    }

    public void resize(int i) {
        Preconditions.checkArgumentNonnegative(i);
        if (i <= this.mValues.length) {
            Arrays.fill(this.mValues, i, this.mValues.length, 0L);
        } else {
            ensureCapacity(i - this.mSize);
        }
        this.mSize = i;
    }

    public void add(long j) {
        add(this.mSize, j);
    }

    public void add(int i, long j) {
        ensureCapacity(1);
        int i2 = this.mSize - i;
        this.mSize++;
        checkBounds(i);
        if (i2 != 0) {
            System.arraycopy(this.mValues, i, this.mValues, i + 1, i2);
        }
        this.mValues[i] = j;
    }

    public void addAll(LongArray longArray) {
        int i = longArray.mSize;
        ensureCapacity(i);
        System.arraycopy(longArray.mValues, 0, this.mValues, this.mSize, i);
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
            long[] jArrNewUnpaddedLongArray = ArrayUtils.newUnpaddedLongArray(i3);
            System.arraycopy(this.mValues, 0, jArrNewUnpaddedLongArray, 0, i2);
            this.mValues = jArrNewUnpaddedLongArray;
        }
    }

    public void clear() {
        this.mSize = 0;
    }

    public LongArray m32clone() {
        try {
            LongArray longArray = (LongArray) super.clone();
            try {
                longArray.mValues = (long[]) this.mValues.clone();
                return longArray;
            } catch (CloneNotSupportedException e) {
                return longArray;
            }
        } catch (CloneNotSupportedException e2) {
            return null;
        }
    }

    public long get(int i) {
        checkBounds(i);
        return this.mValues[i];
    }

    public void set(int i, long j) {
        checkBounds(i);
        this.mValues[i] = j;
    }

    public int indexOf(long j) {
        int i = this.mSize;
        for (int i2 = 0; i2 < i; i2++) {
            if (this.mValues[i2] == j) {
                return i2;
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

    public long[] toArray() {
        return Arrays.copyOf(this.mValues, this.mSize);
    }

    private void checkBounds(int i) {
        if (i < 0 || this.mSize <= i) {
            throw new ArrayIndexOutOfBoundsException(this.mSize, i);
        }
    }

    public static boolean elementsEqual(LongArray longArray, LongArray longArray2) {
        if (longArray == null || longArray2 == null) {
            return longArray == longArray2;
        }
        if (longArray.mSize != longArray2.mSize) {
            return false;
        }
        for (int i = 0; i < longArray.mSize; i++) {
            if (longArray.get(i) != longArray2.get(i)) {
                return false;
            }
        }
        return true;
    }
}
