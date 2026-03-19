package com.android.server.oemlock;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.oemlock.V1_0.IOemLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.service.oemlock.IOemLockService;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import com.android.server.SystemService;
import com.android.server.pm.UserRestrictionsUtils;

public class OemLockService extends SystemService {
    private static final String FLASH_LOCK_PROP = "ro.boot.flash.locked";
    private static final String FLASH_LOCK_UNLOCKED = "0";
    private static final String TAG = "OemLock";
    private Context mContext;
    private OemLock mOemLock;
    private final IBinder mService;
    private final UserManagerInternal.UserRestrictionsListener mUserRestrictionsListener;

    public static boolean isHalPresent() {
        return VendorLock.getOemLockHalService() != null;
    }

    private static OemLock getOemLock(Context context) {
        IOemLock oemLockHalService = VendorLock.getOemLockHalService();
        if (oemLockHalService != null) {
            Slog.i(TAG, "Using vendor lock via the HAL");
            return new VendorLock(context, oemLockHalService);
        }
        Slog.i(TAG, "Using persistent data block based lock");
        return new PersistentDataBlockLock(context);
    }

    public OemLockService(Context context) {
        this(context, getOemLock(context));
    }

    OemLockService(Context context, OemLock oemLock) {
        super(context);
        this.mUserRestrictionsListener = new UserManagerInternal.UserRestrictionsListener() {
            public void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
                if (UserRestrictionsUtils.restrictionsChanged(bundle2, bundle, "no_factory_reset") && !(!bundle.getBoolean("no_factory_reset"))) {
                    OemLockService.this.mOemLock.setOemUnlockAllowedByDevice(false);
                    OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(false);
                }
            }
        };
        this.mService = new IOemLockService.Stub() {
            public void setOemUnlockAllowedByCarrier(boolean z, byte[] bArr) {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                OemLockService.this.enforceUserIsAdmin();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OemLockService.this.mOemLock.setOemUnlockAllowedByCarrier(z, bArr);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean isOemUnlockAllowedByCarrier() {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void setOemUnlockAllowedByUser(boolean z) {
                if (!ActivityManager.isUserAMonkey()) {
                    OemLockService.this.enforceManageUserOemUnlockPermission();
                    OemLockService.this.enforceUserIsAdmin();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (OemLockService.this.isOemUnlockAllowedByAdmin()) {
                            if (OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier()) {
                                OemLockService.this.mOemLock.setOemUnlockAllowedByDevice(z);
                                OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(z);
                                return;
                            }
                            throw new SecurityException("Carrier does not allow OEM unlock");
                        }
                        throw new SecurityException("Admin does not allow OEM unlock");
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }

            public boolean isOemUnlockAllowedByUser() {
                OemLockService.this.enforceManageUserOemUnlockPermission();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return OemLockService.this.mOemLock.isOemUnlockAllowedByDevice();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean isOemUnlockAllowed() {
                OemLockService.this.enforceOemUnlockReadPermission();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    boolean z = OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier() && OemLockService.this.mOemLock.isOemUnlockAllowedByDevice();
                    OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(z);
                    return z;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean isDeviceOemUnlocked() {
                OemLockService.this.enforceOemUnlockReadPermission();
                String str = SystemProperties.get(OemLockService.FLASH_LOCK_PROP);
                return ((str.hashCode() == 48 && str.equals(OemLockService.FLASH_LOCK_UNLOCKED)) ? (byte) 0 : (byte) -1) == 0;
            }
        };
        this.mContext = context;
        this.mOemLock = oemLock;
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(this.mUserRestrictionsListener);
    }

    @Override
    public void onStart() {
        publishBinderService("oem_lock", this.mService);
    }

    private void setPersistentDataBlockOemUnlockAllowedBit(boolean z) {
        PersistentDataBlockManagerInternal persistentDataBlockManagerInternal = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        if (persistentDataBlockManagerInternal != null && !(this.mOemLock instanceof PersistentDataBlockLock)) {
            Slog.i(TAG, "Update OEM Unlock bit in pst partition to " + z);
            persistentDataBlockManagerInternal.forceOemUnlockEnabled(z);
        }
    }

    private boolean isOemUnlockAllowedByAdmin() {
        return !UserManager.get(this.mContext).hasUserRestriction("no_factory_reset", UserHandle.SYSTEM);
    }

    private void enforceManageCarrierOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CARRIER_OEM_UNLOCK_STATE", "Can't manage OEM unlock allowed by carrier");
    }

    private void enforceManageUserOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USER_OEM_UNLOCK_STATE", "Can't manage OEM unlock allowed by user");
    }

    private void enforceOemUnlockReadPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_OEM_UNLOCK_STATE") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE") == -1) {
            throw new SecurityException("Can't access OEM unlock state. Requires READ_OEM_UNLOCK_STATE or OEM_UNLOCK_STATE permission.");
        }
    }

    private void enforceUserIsAdmin() {
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (!UserManager.get(this.mContext).isUserAdmin(callingUserId)) {
                throw new SecurityException("Must be an admin user");
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
