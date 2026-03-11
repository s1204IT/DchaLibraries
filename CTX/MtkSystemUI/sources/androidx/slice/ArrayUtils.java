package androidx.slice;

import android.support.v4.util.ObjectsCompat;
import java.lang.reflect.Array;

class ArrayUtils {
    public static <T> boolean contains(T[] array, T item) {
        for (T t : array) {
            if (ObjectsCompat.equals(t, item)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T[] appendElement(Class<T> cls, T[] tArr, T t) {
        int length;
        T[] tArr2;
        if (tArr != null) {
            length = tArr.length;
            tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, length + 1));
            System.arraycopy(tArr, 0, tArr2, 0, length);
        } else {
            length = 0;
            tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, 1));
        }
        tArr2[length] = t;
        return tArr2;
    }
}
