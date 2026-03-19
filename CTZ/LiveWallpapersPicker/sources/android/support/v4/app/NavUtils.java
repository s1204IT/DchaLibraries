package android.support.v4.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NavUtils {
    public static String getParentActivityName(Activity sourceActivity) {
        try {
            return getParentActivityName(sourceActivity, sourceActivity.getComponentName());
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getParentActivityName(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException {
        String parentActivity;
        String result;
        PackageManager pm = context.getPackageManager();
        ActivityInfo info = pm.getActivityInfo(componentName, 128);
        if (Build.VERSION.SDK_INT >= 16 && (result = info.parentActivityName) != null) {
            return result;
        }
        if (((PackageItemInfo) info).metaData == null || (parentActivity = ((PackageItemInfo) info).metaData.getString("android.support.PARENT_ACTIVITY")) == null) {
            return null;
        }
        if (parentActivity.charAt(0) == '.') {
            return context.getPackageName() + parentActivity;
        }
        return parentActivity;
    }
}
