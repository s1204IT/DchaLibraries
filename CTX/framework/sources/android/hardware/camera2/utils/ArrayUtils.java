package android.hardware.camera2.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ArrayUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "ArrayUtils";

    public static <T> int getArrayIndex(T[] tArr, T t) {
        if (tArr == null) {
            return -1;
        }
        int i = 0;
        for (T t2 : tArr) {
            if (Objects.equals(t2, t)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static int getArrayIndex(int[] iArr, int i) {
        if (iArr == null) {
            return -1;
        }
        for (int i2 = 0; i2 < iArr.length; i2++) {
            if (iArr[i2] == i) {
                return i2;
            }
        }
        return -1;
    }

    public static int[] convertStringListToIntArray(List<String> list, String[] strArr, int[] iArr) {
        if (list == null) {
            return null;
        }
        List<Integer> listConvertStringListToIntList = convertStringListToIntList(list, strArr, iArr);
        int[] iArr2 = new int[listConvertStringListToIntList.size()];
        for (int i = 0; i < iArr2.length; i++) {
            iArr2[i] = listConvertStringListToIntList.get(i).intValue();
        }
        return iArr2;
    }

    public static List<Integer> convertStringListToIntList(List<String> list, String[] strArr, int[] iArr) {
        if (list == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            int arrayIndex = getArrayIndex(strArr, it.next());
            if (arrayIndex >= 0 && arrayIndex < iArr.length) {
                arrayList.add(Integer.valueOf(iArr[arrayIndex]));
            }
        }
        return arrayList;
    }

    public static int[] toIntArray(List<Integer> list) {
        if (list == null) {
            return null;
        }
        int[] iArr = new int[list.size()];
        int i = 0;
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            iArr[i] = it.next().intValue();
            i++;
        }
        return iArr;
    }

    public static boolean contains(int[] iArr, int i) {
        return getArrayIndex(iArr, i) != -1;
    }

    public static <T> boolean contains(T[] tArr, T t) {
        return getArrayIndex(tArr, t) != -1;
    }

    private ArrayUtils() {
        throw new AssertionError();
    }
}
