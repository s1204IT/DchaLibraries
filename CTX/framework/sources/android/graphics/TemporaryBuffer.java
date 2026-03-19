package android.graphics;

import com.android.internal.util.ArrayUtils;

public class TemporaryBuffer {
    private static char[] sTemp = null;

    public static char[] obtain(int i) {
        char[] cArr;
        synchronized (TemporaryBuffer.class) {
            cArr = sTemp;
            sTemp = null;
        }
        if (cArr == null || cArr.length < i) {
            return ArrayUtils.newUnpaddedCharArray(i);
        }
        return cArr;
    }

    public static void recycle(char[] cArr) {
        if (cArr.length > 1000) {
            return;
        }
        synchronized (TemporaryBuffer.class) {
            sTemp = cArr;
        }
    }
}
