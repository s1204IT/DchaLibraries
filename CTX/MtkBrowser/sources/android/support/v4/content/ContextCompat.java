package android.support.v4.content;

import android.content.Context;
import android.os.Build;
import java.io.File;

public class ContextCompat {
    private static final Object sLock = new Object();

    public static File[] getExternalCacheDirs(Context context) {
        return Build.VERSION.SDK_INT >= 19 ? context.getExternalCacheDirs() : new File[]{context.getExternalCacheDir()};
    }

    public static File[] getExternalFilesDirs(Context context, String str) {
        return Build.VERSION.SDK_INT >= 19 ? context.getExternalFilesDirs(str) : new File[]{context.getExternalFilesDir(str)};
    }
}
