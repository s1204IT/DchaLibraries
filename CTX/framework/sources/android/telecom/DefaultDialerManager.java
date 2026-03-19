package android.telecom;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class DefaultDialerManager {
    private static final String TAG = "DefaultDialerManager";

    public static boolean setDefaultDialerApplication(Context context, String str) {
        return setDefaultDialerApplication(context, str, ActivityManager.getCurrentUser());
    }

    public static boolean setDefaultDialerApplication(Context context, String str, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.DIALER_DEFAULT_APPLICATION, i);
        if ((str != null && stringForUser != null && str.equals(stringForUser)) || !getInstalledDialerApplications(context).contains(str)) {
            return false;
        }
        Settings.Secure.putStringForUser(context.getContentResolver(), Settings.Secure.DIALER_DEFAULT_APPLICATION, str, i);
        return true;
    }

    public static String getDefaultDialerApplication(Context context) {
        return getDefaultDialerApplication(context, context.getUserId());
    }

    public static String getDefaultDialerApplication(Context context, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.DIALER_DEFAULT_APPLICATION, i);
        List<String> installedDialerApplications = getInstalledDialerApplications(context, i);
        if (installedDialerApplications.contains(stringForUser)) {
            return stringForUser;
        }
        String systemDialerPackage = getTelecomManager(context).getSystemDialerPackage();
        if (!TextUtils.isEmpty(systemDialerPackage) && installedDialerApplications.contains(systemDialerPackage)) {
            return systemDialerPackage;
        }
        return null;
    }

    public static List<String> getInstalledDialerApplications(Context context, int i) {
        List<ResolveInfo> listQueryIntentActivitiesAsUser = context.getPackageManager().queryIntentActivitiesAsUser(new Intent(Intent.ACTION_DIAL), 0, i);
        ArrayList arrayList = new ArrayList();
        for (ResolveInfo resolveInfo : listQueryIntentActivitiesAsUser) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && !arrayList.contains(activityInfo.packageName) && resolveInfo.targetUserId == -2) {
                arrayList.add(activityInfo.packageName);
            }
        }
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.fromParts(PhoneAccount.SCHEME_TEL, "", null));
        return filterByIntent(context, arrayList, intent, i);
    }

    public static List<String> getInstalledDialerApplications(Context context) {
        return getInstalledDialerApplications(context, Process.myUserHandle().getIdentifier());
    }

    public static boolean isDefaultOrSystemDialer(Context context, String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        TelecomManager telecomManager = getTelecomManager(context);
        return str.equals(telecomManager.getDefaultDialerPackage()) || str.equals(telecomManager.getSystemDialerPackage());
    }

    private static List<String> filterByIntent(Context context, List<String> list, Intent intent, int i) {
        if (list == null || list.isEmpty()) {
            return new ArrayList();
        }
        ArrayList arrayList = new ArrayList();
        List<ResolveInfo> listQueryIntentActivitiesAsUser = context.getPackageManager().queryIntentActivitiesAsUser(intent, 0, i);
        int size = listQueryIntentActivitiesAsUser.size();
        for (int i2 = 0; i2 < size; i2++) {
            ActivityInfo activityInfo = listQueryIntentActivitiesAsUser.get(i2).activityInfo;
            if (activityInfo != null && list.contains(activityInfo.packageName) && !arrayList.contains(activityInfo.packageName)) {
                arrayList.add(activityInfo.packageName);
            }
        }
        return arrayList;
    }

    private static TelecomManager getTelecomManager(Context context) {
        return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }
}
