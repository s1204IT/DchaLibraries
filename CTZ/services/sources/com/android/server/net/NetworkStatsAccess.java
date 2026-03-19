package com.android.server.net;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import com.android.server.LocalServices;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class NetworkStatsAccess {

    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {
        public static final int DEFAULT = 0;
        public static final int DEVICE = 3;
        public static final int DEVICESUMMARY = 2;
        public static final int USER = 1;
    }

    private NetworkStatsAccess() {
    }

    public static int checkAccessLevel(Context context, int i, String str) {
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        boolean z = telephonyManager != null && telephonyManager.checkCarrierPrivilegesForPackage(str) == 1;
        boolean z2 = devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(i, -2);
        if (z || z2 || UserHandle.getAppId(i) == 1000) {
            return 3;
        }
        if (hasAppOpsPermission(context, i, str) || context.checkCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY") == 0) {
            return 2;
        }
        return devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(i, -1) ? 1 : 0;
    }

    public static boolean isAccessibleToUser(int i, int i2, int i3) {
        switch (i3) {
            case 1:
                return i == 1000 || i == -4 || i == -5 || UserHandle.getUserId(i) == UserHandle.getUserId(i2);
            case 2:
                return i == 1000 || i == -4 || i == -5 || i == -1 || UserHandle.getUserId(i) == UserHandle.getUserId(i2);
            case 3:
                return true;
            default:
                return i == i2;
        }
    }

    private static boolean hasAppOpsPermission(Context context, int i, String str) {
        if (str == null) {
            return false;
        }
        int iNoteOp = ((AppOpsManager) context.getSystemService("appops")).noteOp(43, i, str);
        return iNoteOp == 3 ? context.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0 : iNoteOp == 0;
    }
}
