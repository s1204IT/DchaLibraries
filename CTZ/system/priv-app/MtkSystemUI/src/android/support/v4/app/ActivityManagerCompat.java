package android.support.v4.app;

import android.app.ActivityManager;
import android.os.Build;
/* loaded from: classes.dex */
public final class ActivityManagerCompat {
    public static boolean isLowRamDevice(ActivityManager activityManager) {
        if (Build.VERSION.SDK_INT >= 19) {
            return activityManager.isLowRamDevice();
        }
        return false;
    }
}
