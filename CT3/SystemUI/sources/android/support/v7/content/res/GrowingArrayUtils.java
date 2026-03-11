package android.support.v7.content.res;

import java.lang.reflect.Array;

final class GrowingArrayUtils {

    static final boolean f0assertionsDisabled;

    static {
        f0assertionsDisabled = !GrowingArrayUtils.class.desiredAssertionStatus();
    }

    public static <T> T[] append(T[] tArr, int i, T t) {
        if (!f0assertionsDisabled) {
            if (!(i <= tArr.length)) {
                throw new AssertionError();
            }
        }
        if (i + 1 > tArr.length) {
            Object[] objArr = (Object[]) Array.newInstance(tArr.getClass().getComponentType(), growSize(i));
            System.arraycopy(tArr, 0, objArr, 0, i);
            tArr = (T[]) objArr;
        }
        tArr[i] = t;
        return tArr;
    }

    public static int[] append(int[] array, int currentSize, int element) {
        if (!f0assertionsDisabled) {
            if (!(currentSize <= array.length)) {
                throw new AssertionError();
            }
        }
        if (currentSize + 1 > array.length) {
            int[] newArray = new int[growSize(currentSize)];
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }

    public static int growSize(int currentSize) {
        if (currentSize <= 4) {
            return 8;
        }
        return currentSize * 2;
    }

    private GrowingArrayUtils() {
    }
}
