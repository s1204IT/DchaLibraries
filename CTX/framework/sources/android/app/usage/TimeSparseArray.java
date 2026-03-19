package android.app.usage;

import android.util.LongSparseArray;
import android.util.Slog;

public class TimeSparseArray<E> extends LongSparseArray<E> {
    private static final String TAG = TimeSparseArray.class.getSimpleName();
    private boolean mWtfReported;

    public int closestIndexOnOrAfter(long j) {
        int size = size();
        int i = size - 1;
        int i2 = 0;
        long jKeyAt = -1;
        int i3 = -1;
        while (i2 <= i) {
            i3 = i2 + ((i - i2) / 2);
            jKeyAt = keyAt(i3);
            if (j > jKeyAt) {
                i2 = i3 + 1;
            } else if (j < jKeyAt) {
                i = i3 - 1;
            } else {
                return i3;
            }
        }
        if (j < jKeyAt) {
            return i3;
        }
        if (j <= jKeyAt || i2 >= size) {
            return -1;
        }
        return i2;
    }

    @Override
    public void put(long j, E e) {
        if (indexOfKey(j) >= 0 && !this.mWtfReported) {
            Slog.wtf(TAG, "Overwriting value " + get(j) + " by " + e);
            this.mWtfReported = true;
        }
        super.put(j, e);
    }

    public int closestIndexOnOrBefore(long j) {
        int iClosestIndexOnOrAfter = closestIndexOnOrAfter(j);
        if (iClosestIndexOnOrAfter < 0) {
            return size() - 1;
        }
        if (keyAt(iClosestIndexOnOrAfter) == j) {
            return iClosestIndexOnOrAfter;
        }
        return iClosestIndexOnOrAfter - 1;
    }
}
