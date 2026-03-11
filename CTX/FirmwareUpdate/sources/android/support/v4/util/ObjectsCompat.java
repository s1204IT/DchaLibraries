package android.support.v4.util;

import android.os.Build;
import java.util.Arrays;
import java.util.Objects;

public class ObjectsCompat {
    public static boolean equals(Object obj, Object obj2) {
        return Build.VERSION.SDK_INT >= 19 ? Objects.equals(obj, obj2) : obj == obj2 || (obj != null && obj.equals(obj2));
    }

    public static int hash(Object... objArr) {
        return Build.VERSION.SDK_INT >= 19 ? Objects.hash(objArr) : Arrays.hashCode(objArr);
    }
}
