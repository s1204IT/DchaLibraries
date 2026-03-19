package android.telephony;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import java.util.Iterator;

public final class LocationAccessPolicy {
    private static final String LOG_TAG = LocationAccessPolicy.class.getSimpleName();

    public static boolean canAccessCellLocation(Context context, String str, int i, int i2, boolean z) throws SecurityException {
        Trace.beginSection("TelephonyLohcationCheck");
        boolean z2 = true;
        if (i == 1001) {
            Trace.endSection();
            return true;
        }
        try {
            if (z) {
                context.enforcePermission(Manifest.permission.ACCESS_COARSE_LOCATION, i2, i, "canAccessCellLocation");
            } else if (context.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, i2, i) == -1) {
                Trace.endSection();
                return false;
            }
            int iPermissionToOpCode = AppOpsManager.permissionToOpCode(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (iPermissionToOpCode != -1 && ((AppOpsManager) context.getSystemService(AppOpsManager.class)).noteOpNoThrow(iPermissionToOpCode, i, str) != 0) {
                Trace.endSection();
                return false;
            }
            if (!isLocationModeEnabled(context, UserHandle.getUserId(i))) {
                Trace.endSection();
                return false;
            }
            if (!isCurrentProfile(context, i)) {
                if (!checkInteractAcrossUsersFull(context)) {
                    z2 = false;
                }
            }
            Trace.endSection();
            return z2;
        } catch (Throwable th) {
            Trace.endSection();
            throw th;
        }
    }

    private static boolean isLocationModeEnabled(Context context, int i) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LocationManager.class);
        if (locationManager == null) {
            Log.w(LOG_TAG, "Couldn't get location manager, denying location access");
            return false;
        }
        return locationManager.isLocationEnabledForUser(UserHandle.of(i));
    }

    private static boolean checkInteractAcrossUsersFull(Context context) {
        return context.checkCallingOrSelfPermission(Manifest.permission.INTERACT_ACROSS_USERS_FULL) == 0;
    }

    private static boolean isCurrentProfile(Context context, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            int userId = UserHandle.getUserId(i);
            if (userId == currentUser) {
                return true;
            }
            Iterator<UserInfo> it = ((UserManager) context.getSystemService(UserManager.class)).getProfiles(currentUser).iterator();
            while (it.hasNext()) {
                if (it.next().id == userId) {
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
