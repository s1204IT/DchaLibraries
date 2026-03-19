package com.android.packageinstaller.permission.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.ArraySet;
import android.util.Log;
import android.util.TypedValue;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.PermissionApps;
import java.util.Iterator;

public final class Utils {
    public static final String[] MODERN_PERMISSION_GROUPS = {"android.permission-group.CALENDAR", "android.permission-group.CALL_LOG", "android.permission-group.CAMERA", "android.permission-group.CONTACTS", "android.permission-group.LOCATION", "android.permission-group.SENSORS", "android.permission-group.SMS", "android.permission-group.PHONE", "android.permission-group.MICROPHONE", "android.permission-group.STORAGE"};
    private static final Intent LAUNCHER_INTENT = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER");

    public static Drawable loadDrawable(PackageManager packageManager, String str, int i) {
        try {
            return packageManager.getResourcesForApplication(str).getDrawable(i, null);
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            Log.d("Utils", "Couldn't get resource", e);
            return null;
        }
    }

    public static boolean isModernPermissionGroup(String str) {
        for (String str2 : MODERN_PERMISSION_GROUPS) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldShowPermission(AppPermissionGroup appPermissionGroup, String str) {
        if ((!appPermissionGroup.isSystemFixed() || LocationUtils.isLocationGroupAndProvider(appPermissionGroup.getName(), str)) && appPermissionGroup.isGrantingAllowed()) {
            return !appPermissionGroup.getDeclaringPackage().equals("android") || isModernPermissionGroup(appPermissionGroup.getName());
        }
        return false;
    }

    public static boolean shouldShowPermission(PermissionApps.PermissionApp permissionApp) {
        if (permissionApp.isSystemFixed() && !LocationUtils.isLocationGroupAndProvider(permissionApp.getPermissionGroup().getName(), permissionApp.getPackageName())) {
            return false;
        }
        return true;
    }

    public static Drawable applyTint(Context context, Drawable drawable, int i) {
        Resources.Theme theme = context.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(i, typedValue, true);
        Drawable drawableMutate = drawable.mutate();
        drawableMutate.setTint(context.getColor(typedValue.resourceId));
        return drawableMutate;
    }

    public static Drawable applyTint(Context context, int i, int i2) {
        return applyTint(context, context.getDrawable(i), i2);
    }

    public static ArraySet<String> getLauncherPackages(Context context) {
        ArraySet<String> arraySet = new ArraySet<>();
        Iterator<ResolveInfo> it = context.getPackageManager().queryIntentActivities(LAUNCHER_INTENT, 0).iterator();
        while (it.hasNext()) {
            arraySet.add(it.next().activityInfo.packageName);
        }
        return arraySet;
    }

    public static boolean isSystem(PermissionApps.PermissionApp permissionApp, ArraySet<String> arraySet) {
        return isSystem(permissionApp.getAppInfo(), arraySet);
    }

    public static boolean isSystem(ApplicationInfo applicationInfo, ArraySet<String> arraySet) {
        return applicationInfo.isSystemApp() && (applicationInfo.flags & 128) == 0 && !arraySet.contains(applicationInfo.packageName);
    }

    public static boolean areGroupPermissionsIndividuallyControlled(Context context, String str) {
        if (context.getPackageManager().isPermissionReviewModeEnabled()) {
            return "android.permission-group.SMS".equals(str) || "android.permission-group.PHONE".equals(str) || "android.permission-group.CONTACTS".equals(str);
        }
        return false;
    }

    public static boolean isPermissionIndividuallyControlled(Context context, String str) {
        if (context.getPackageManager().isPermissionReviewModeEnabled()) {
            return "android.permission.READ_CONTACTS".equals(str) || "android.permission.WRITE_CONTACTS".equals(str) || "android.permission.SEND_SMS".equals(str) || "android.permission.RECEIVE_SMS".equals(str) || "android.permission.READ_SMS".equals(str) || "android.permission.RECEIVE_MMS".equals(str) || "android.permission.CALL_PHONE".equals(str) || "android.permission.READ_CALL_LOG".equals(str) || "android.permission.WRITE_CALL_LOG".equals(str);
        }
        return false;
    }
}
