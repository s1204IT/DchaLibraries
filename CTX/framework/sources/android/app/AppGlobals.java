package android.app;

import android.content.pm.IPackageManager;

public class AppGlobals {
    public static Application getInitialApplication() {
        return ActivityThread.currentApplication();
    }

    public static String getInitialPackage() {
        return ActivityThread.currentPackageName();
    }

    public static IPackageManager getPackageManager() {
        return ActivityThread.getPackageManager();
    }

    public static int getIntCoreSetting(String str, int i) {
        ActivityThread activityThreadCurrentActivityThread = ActivityThread.currentActivityThread();
        if (activityThreadCurrentActivityThread != null) {
            return activityThreadCurrentActivityThread.getIntCoreSetting(str, i);
        }
        return i;
    }
}
