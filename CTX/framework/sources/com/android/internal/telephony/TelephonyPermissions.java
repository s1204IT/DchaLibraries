package com.android.internal.telephony;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ITelephony;
import java.util.function.Supplier;

public final class TelephonyPermissions {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "TelephonyPermissions";
    private static final Supplier<ITelephony> TELEPHONY_SUPPLIER = new Supplier() {
        @Override
        public final Object get() {
            return ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    };

    private TelephonyPermissions() {
    }

    public static boolean checkCallingOrSelfReadPhoneState(Context context, int i, String str, String str2) {
        return checkReadPhoneState(context, i, Binder.getCallingPid(), Binder.getCallingUid(), str, str2);
    }

    public static boolean checkReadPhoneState(Context context, int i, int i2, int i3, String str, String str2) {
        return checkReadPhoneState(context, TELEPHONY_SUPPLIER, i, i2, i3, str, str2);
    }

    @VisibleForTesting
    public static boolean checkReadPhoneState(Context context, Supplier<ITelephony> supplier, int i, int i2, int i3, String str, String str2) {
        try {
            context.enforcePermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE, i2, i3, str2);
            return true;
        } catch (SecurityException e) {
            try {
                context.enforcePermission(Manifest.permission.READ_PHONE_STATE, i2, i3, str2);
                return ((AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE)).noteOp(51, i3, str) == 0;
            } catch (SecurityException e2) {
                if (SubscriptionManager.isValidSubscriptionId(i)) {
                    enforceCarrierPrivilege(supplier, i, i3, str2);
                    return true;
                }
                throw e2;
            }
        }
    }

    public static boolean checkReadCallLog(Context context, int i, int i2, int i3, String str) {
        return checkReadCallLog(context, TELEPHONY_SUPPLIER, i, i2, i3, str);
    }

    @VisibleForTesting
    public static boolean checkReadCallLog(Context context, Supplier<ITelephony> supplier, int i, int i2, int i3, String str) {
        if (context.checkPermission(Manifest.permission.READ_CALL_LOG, i2, i3) == 0) {
            return ((AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE)).noteOp(6, i3, str) == 0;
        }
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            return false;
        }
        enforceCarrierPrivilege(supplier, i, i3, "readCallLog");
        return true;
    }

    public static boolean checkCallingOrSelfReadPhoneNumber(Context context, int i, String str, String str2) {
        return checkReadPhoneNumber(context, TELEPHONY_SUPPLIER, i, Binder.getCallingPid(), Binder.getCallingUid(), str, str2);
    }

    @VisibleForTesting
    public static boolean checkReadPhoneNumber(Context context, Supplier<ITelephony> supplier, int i, int i2, int i3, String str, String str2) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsManager.noteOp(15, i3, str) == 0) {
            return true;
        }
        try {
            return checkReadPhoneState(context, supplier, i, i2, i3, str, str2);
        } catch (SecurityException e) {
            try {
                context.enforcePermission(Manifest.permission.READ_SMS, i2, i3, str2);
                int iPermissionToOpCode = AppOpsManager.permissionToOpCode(Manifest.permission.READ_SMS);
                if (iPermissionToOpCode != -1) {
                    return appOpsManager.noteOp(iPermissionToOpCode, i3, str) == 0;
                }
                return true;
            } catch (SecurityException e2) {
                try {
                    context.enforcePermission(Manifest.permission.READ_PHONE_NUMBERS, i2, i3, str2);
                    int iPermissionToOpCode2 = AppOpsManager.permissionToOpCode(Manifest.permission.READ_PHONE_NUMBERS);
                    if (iPermissionToOpCode2 != -1) {
                        return appOpsManager.noteOp(iPermissionToOpCode2, i3, str) == 0;
                    }
                    return true;
                } catch (SecurityException e3) {
                    throw new SecurityException(str2 + ": Neither user " + i3 + " nor current process has " + Manifest.permission.READ_PHONE_STATE + ", " + Manifest.permission.READ_SMS + ", or " + Manifest.permission.READ_PHONE_NUMBERS);
                }
            }
        }
    }

    public static void enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(Context context, int i, String str) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.MODIFY_PHONE_STATE) == 0) {
            return;
        }
        enforceCallingOrSelfCarrierPrivilege(i, str);
    }

    public static void enforceCallingOrSelfCarrierPrivilege(int i, String str) {
        enforceCarrierPrivilege(i, Binder.getCallingUid(), str);
    }

    private static void enforceCarrierPrivilege(int i, int i2, String str) {
        enforceCarrierPrivilege(TELEPHONY_SUPPLIER, i, i2, str);
    }

    private static void enforceCarrierPrivilege(Supplier<ITelephony> supplier, int i, int i2, String str) {
        if (getCarrierPrivilegeStatus(supplier, i, i2) != 1) {
            throw new SecurityException(str);
        }
    }

    private static int getCarrierPrivilegeStatus(Supplier<ITelephony> supplier, int i, int i2) {
        ITelephony iTelephony = supplier.get();
        if (iTelephony != null) {
            try {
                return iTelephony.getCarrierPrivilegeStatusForUid(i, i2);
            } catch (RemoteException e) {
            }
        }
        Rlog.e(LOG_TAG, "Phone process is down, cannot check carrier privileges");
        return 0;
    }
}
