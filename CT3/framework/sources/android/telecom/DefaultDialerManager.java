package android.telecom;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class DefaultDialerManager {
    private static final String TAG = "DefaultDialerManager";

    public static boolean setDefaultDialerApplication(Context context, String packageName) {
        return setDefaultDialerApplication(context, packageName, ActivityManager.getCurrentUser());
    }

    public static boolean setDefaultDialerApplication(Context context, String packageName, int user) {
        String oldPackageName = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.DIALER_DEFAULT_APPLICATION, user);
        if (packageName != null && oldPackageName != null && packageName.equals(oldPackageName)) {
            return false;
        }
        List<String> packageNames = getInstalledDialerApplications(context);
        if (!packageNames.contains(packageName)) {
            return false;
        }
        Settings.Secure.putStringForUser(context.getContentResolver(), Settings.Secure.DIALER_DEFAULT_APPLICATION, packageName, user);
        return true;
    }

    public static String getDefaultDialerApplication(Context context) {
        return getDefaultDialerApplication(context, context.getUserId());
    }

    public static String getDefaultDialerApplication(Context context, int user) {
        String defaultPackageName = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.DIALER_DEFAULT_APPLICATION, user);
        List<String> packageNames = getInstalledDialerApplications(context);
        if (packageNames.contains(defaultPackageName)) {
            return defaultPackageName;
        }
        String systemDialerPackageName = getTelecomManager(context).getSystemDialerPackage();
        if (!TextUtils.isEmpty(systemDialerPackageName) && packageNames.contains(systemDialerPackageName)) {
            return systemDialerPackageName;
        }
        return null;
    }

    public static List<String> getInstalledDialerApplications(Context context, int userId) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_DIAL);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivitiesAsUser(intent, 0, userId);
        List<String> packageNames = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && !packageNames.contains(activityInfo.packageName)) {
                packageNames.add(activityInfo.packageName);
            }
        }
        Intent dialIntentWithTelScheme = new Intent(Intent.ACTION_DIAL);
        dialIntentWithTelScheme.setData(Uri.fromParts(PhoneAccount.SCHEME_TEL, ProxyInfo.LOCAL_EXCL_LIST, null));
        return filterByIntent(context, packageNames, dialIntentWithTelScheme);
    }

    public static List<String> getInstalledDialerApplications(Context context) {
        return getInstalledDialerApplications(context, Process.myUserHandle().getIdentifier());
    }

    public static boolean isDefaultOrSystemDialer(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        TelecomManager tm = getTelecomManager(context);
        if (packageName.equals(tm.getDefaultDialerPackage())) {
            return true;
        }
        return packageName.equals(tm.getSystemDialerPackage());
    }

    private static List<String> filterByIntent(Context context, List<String> packageNames, Intent intent) {
        if (packageNames == null || packageNames.isEmpty()) {
            return new ArrayList();
        }
        List<String> result = new ArrayList<>();
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(intent, 0);
        int length = resolveInfoList.size();
        for (int i = 0; i < length; i++) {
            ActivityInfo info = resolveInfoList.get(i).activityInfo;
            if (info != null && packageNames.contains(info.packageName) && !result.contains(info.packageName)) {
                result.add(info.packageName);
            }
        }
        return result;
    }

    private static TelecomManager getTelecomManager(Context context) {
        return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }
}
