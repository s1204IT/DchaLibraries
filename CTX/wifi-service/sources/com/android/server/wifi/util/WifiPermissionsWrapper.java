package com.android.server.wifi.util;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.server.LocalServices;
import java.util.List;

public class WifiPermissionsWrapper {
    private static final String TAG = "WifiPermissionsWrapper";
    private final Context mContext;

    public WifiPermissionsWrapper(Context context) {
        this.mContext = context;
    }

    public int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public int getCallingUserId(int i) {
        return UserHandle.getUserId(i);
    }

    public String getTopPkgName() {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
        if (runningTasks.isEmpty()) {
            return " ";
        }
        return runningTasks.get(0).topActivity.getPackageName();
    }

    public int getUidPermission(String str, int i) {
        return ActivityManager.checkUidPermission(str, i);
    }

    public DevicePolicyManagerInternal getDevicePolicyManagerInternal() {
        return (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
    }

    public int getOverrideWifiConfigPermission(int i) throws RemoteException {
        return AppGlobals.getPackageManager().checkUidPermission("android.permission.OVERRIDE_WIFI_CONFIG", i);
    }

    public int getChangeWifiConfigPermission(int i) throws RemoteException {
        return AppGlobals.getPackageManager().checkUidPermission("android.permission.CHANGE_WIFI_STATE", i);
    }

    public int getAccessWifiStatePermission(int i) throws RemoteException {
        return AppGlobals.getPackageManager().checkUidPermission("android.permission.ACCESS_WIFI_STATE", i);
    }

    public int getLocalMacAddressPermission(int i) throws RemoteException {
        return AppGlobals.getPackageManager().checkUidPermission("android.permission.LOCAL_MAC_ADDRESS", i);
    }
}
