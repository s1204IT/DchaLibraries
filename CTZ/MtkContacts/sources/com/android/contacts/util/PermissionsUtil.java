package com.android.contacts.util;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.mediatek.contacts.util.Log;
import java.util.Arrays;
import java.util.List;

public class PermissionsUtil {
    public static final String LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String PHONE = "android.permission.CALL_PHONE";
    public static final String[] PHONE_FULL_GROUP = {PHONE, "android.permission.READ_PHONE_STATE", "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG", "com.android.voicemail.permission.ADD_VOICEMAIL", "android.permission.USE_SIP", "android.permission.PROCESS_OUTGOING_CALLS"};
    public static final String CONTACTS = "android.permission.READ_CONTACTS";
    public static final String[] CONTACTS_FULL_GROUP = {CONTACTS, "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS"};

    public static boolean hasPhonePermissions(Context context) {
        return hasPermissions(context, PHONE_FULL_GROUP);
    }

    public static boolean hasContactsPermissions(Context context) {
        return hasPermissions(context, CONTACTS_FULL_GROUP);
    }

    public static boolean hasLocationPermissions(Context context) {
        return hasPermission(context, LOCATION);
    }

    public static boolean hasPermission(Context context, String str) {
        return ContextCompat.checkSelfPermission(context, str) == 0;
    }

    public static boolean hasAppOp(Context context, String str) {
        return ((AppOpsManager) context.getSystemService("appops")).checkOpNoThrow(str, Process.myUid(), context.getPackageName()) == 0;
    }

    public static void registerPermissionReceiver(Context context, BroadcastReceiver broadcastReceiver, String str) {
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(str));
    }

    public static void unregisterPermissionReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    public static void notifyPermissionGranted(Context context, String str) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(str));
    }

    private static boolean hasPermissions(Context context, String[] strArr) {
        PackageInfo packageInfo;
        boolean z;
        if (strArr == null || strArr.length == 0) {
            return false;
        }
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationInfo().packageName, 4096);
            z = true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (UnsupportedOperationException e2) {
            Log.d(PermissionsUtil.class.getSimpleName(), "NOT SUPPORTED : " + e2.toString());
            packageInfo = null;
            z = false;
        }
        if (!z) {
            for (String str : strArr) {
                if (ContextCompat.checkSelfPermission(context, str) != 0) {
                    Log.d(PermissionsUtil.class.getSimpleName(), "NOT GRANTED : " + str);
                    return false;
                }
            }
        } else {
            if (packageInfo == null || packageInfo.requestedPermissions == null) {
                return false;
            }
            List listAsList = Arrays.asList(packageInfo.requestedPermissions);
            for (String str2 : strArr) {
                if (listAsList.contains(str2) && ContextCompat.checkSelfPermission(context, str2) != 0) {
                    Log.d(PermissionsUtil.class.getSimpleName(), "NOT GRANTED : " + str2);
                    return false;
                }
            }
        }
        return true;
    }
}
