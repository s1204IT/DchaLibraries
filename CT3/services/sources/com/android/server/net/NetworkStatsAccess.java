package com.android.server.net;

import android.annotation.IntDef;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import com.android.server.LocalServices;
import com.android.server.notification.ZenModeHelper;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class NetworkStatsAccess {

    @IntDef({0, ZenModeHelper.SUPPRESSED_EFFECT_NOTIFICATIONS, ZenModeHelper.SUPPRESSED_EFFECT_CALLS, ZenModeHelper.SUPPRESSED_EFFECT_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {
        public static final int DEFAULT = 0;
        public static final int DEVICE = 3;
        public static final int DEVICESUMMARY = 2;
        public static final int USER = 1;
    }

    private NetworkStatsAccess() {
    }

    public static int checkAccessLevel(Context context, int callingUid, String callingPackage) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        boolean hasCarrierPrivileges = tm != null && tm.checkCarrierPrivilegesForPackage(callingPackage) == 1;
        boolean zIsActiveAdminWithPolicy = dpmi != null ? dpmi.isActiveAdminWithPolicy(callingUid, -2) : false;
        if (hasCarrierPrivileges || zIsActiveAdminWithPolicy || UserHandle.getAppId(callingUid) == 1000) {
            return 3;
        }
        boolean hasAppOpsPermission = hasAppOpsPermission(context, callingUid, callingPackage);
        if (hasAppOpsPermission || context.checkCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY") == 0) {
            return 2;
        }
        boolean isProfileOwner = dpmi != null ? dpmi.isActiveAdminWithPolicy(callingUid, -1) : false;
        return isProfileOwner ? 1 : 0;
    }

    public static boolean isAccessibleToUser(int uid, int callerUid, int accessLevel) {
        switch (accessLevel) {
            case 1:
                if (uid != 1000 && uid != -4 && uid != -5 && UserHandle.getUserId(uid) != UserHandle.getUserId(callerUid)) {
                    break;
                }
                break;
            case 2:
                if (uid != 1000 && uid != -4 && uid != -5 && uid != -1 && UserHandle.getUserId(uid) != UserHandle.getUserId(callerUid)) {
                    break;
                }
                break;
            case 3:
                break;
            default:
                if (uid != callerUid) {
                    break;
                }
                break;
        }
        return true;
    }

    private static boolean hasAppOpsPermission(Context context, int callingUid, String callingPackage) {
        if (callingPackage == null) {
            return false;
        }
        AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
        int mode = appOps.checkOp(43, callingUid, callingPackage);
        if (mode != 3) {
            return mode == 0;
        }
        int permissionCheck = context.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS");
        return permissionCheck == 0;
    }
}
