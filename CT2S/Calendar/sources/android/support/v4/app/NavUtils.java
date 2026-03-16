package android.support.v4.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.IntentCompat;

public class NavUtils {
    private static final NavUtilsImpl IMPL;

    interface NavUtilsImpl {
        String getParentActivityName(Context context, ActivityInfo activityInfo);
    }

    static class NavUtilsImplBase implements NavUtilsImpl {
        NavUtilsImplBase() {
        }

        @Override
        public String getParentActivityName(Context context, ActivityInfo info) {
            String parentActivity;
            if (info.metaData != null && (parentActivity = info.metaData.getString("android.support.PARENT_ACTIVITY")) != null) {
                if (parentActivity.charAt(0) == '.') {
                    return context.getPackageName() + parentActivity;
                }
                return parentActivity;
            }
            return null;
        }
    }

    static class NavUtilsImplJB extends NavUtilsImplBase {
        NavUtilsImplJB() {
        }

        @Override
        public String getParentActivityName(Context context, ActivityInfo info) {
            String result = NavUtilsJB.getParentActivityName(info);
            if (result == null) {
                return super.getParentActivityName(context, info);
            }
            return result;
        }
    }

    static {
        int version = Build.VERSION.SDK_INT;
        if (version >= 16) {
            IMPL = new NavUtilsImplJB();
        } else {
            IMPL = new NavUtilsImplBase();
        }
    }

    public static Intent getParentActivityIntent(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException {
        String parentActivity = getParentActivityName(context, componentName);
        if (parentActivity == null) {
            return null;
        }
        ComponentName target = new ComponentName(componentName.getPackageName(), parentActivity);
        String grandparent = getParentActivityName(context, target);
        return grandparent == null ? IntentCompat.makeMainActivity(target) : new Intent().setComponent(target);
    }

    public static String getParentActivityName(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        ActivityInfo info = pm.getActivityInfo(componentName, 128);
        String parentActivity = IMPL.getParentActivityName(context, info);
        return parentActivity;
    }
}
