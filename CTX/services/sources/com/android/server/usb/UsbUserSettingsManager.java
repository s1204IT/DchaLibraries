package com.android.server.usb;

import android.R;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import java.util.HashMap;
import java.util.Iterator;

class UsbUserSettingsManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbUserSettingsManager";
    private final boolean mDisablePermissionDialogs;
    private final PackageManager mPackageManager;
    private final UserHandle mUser;
    private final Context mUserContext;
    private final HashMap<String, SparseBooleanArray> mDevicePermissionMap = new HashMap<>();
    private final HashMap<UsbAccessory, SparseBooleanArray> mAccessoryPermissionMap = new HashMap<>();
    private final Object mLock = new Object();

    public UsbUserSettingsManager(Context context, UserHandle userHandle) {
        try {
            this.mUserContext = context.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, userHandle);
            this.mPackageManager = this.mUserContext.getPackageManager();
            this.mUser = userHandle;
            this.mDisablePermissionDialogs = context.getResources().getBoolean(R.^attr-private.dreamActivityOpenExitAnimation);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Missing android package");
        }
    }

    void removeDevicePermissions(UsbDevice usbDevice) {
        synchronized (this.mLock) {
            this.mDevicePermissionMap.remove(usbDevice.getDeviceName());
        }
    }

    void removeAccessoryPermissions(UsbAccessory usbAccessory) {
        synchronized (this.mLock) {
            this.mAccessoryPermissionMap.remove(usbAccessory);
        }
    }

    private boolean isCameraDevicePresent(UsbDevice usbDevice) {
        if (usbDevice.getDeviceClass() == 14) {
            return true;
        }
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            if (usbDevice.getInterface(i).getInterfaceClass() == 14) {
                return true;
            }
        }
        return false;
    }

    private boolean isCameraPermissionGranted(String str, int i) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 0);
            if (applicationInfo.uid != i) {
                Slog.i(TAG, "Package " + str + " does not match caller's uid " + i);
                return false;
            }
            if (applicationInfo.targetSdkVersion >= 28 && -1 == this.mUserContext.checkCallingPermission("android.permission.CAMERA")) {
                Slog.i(TAG, "Camera permission required for USB video class devices");
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Package not found, likely due to invalid package name!");
            return false;
        }
    }

    public boolean hasPermission(UsbDevice usbDevice, String str, int i) {
        synchronized (this.mLock) {
            if (isCameraDevicePresent(usbDevice) && !isCameraPermissionGranted(str, i)) {
                return false;
            }
            if (i != 1000 && !this.mDisablePermissionDialogs) {
                SparseBooleanArray sparseBooleanArray = this.mDevicePermissionMap.get(usbDevice.getDeviceName());
                if (sparseBooleanArray == null) {
                    return false;
                }
                return sparseBooleanArray.get(i);
            }
            return true;
        }
    }

    public boolean hasPermission(UsbAccessory usbAccessory) {
        synchronized (this.mLock) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000 && !this.mDisablePermissionDialogs) {
                SparseBooleanArray sparseBooleanArray = this.mAccessoryPermissionMap.get(usbAccessory);
                if (sparseBooleanArray == null) {
                    return false;
                }
                return sparseBooleanArray.get(callingUid);
            }
            return true;
        }
    }

    public void checkPermission(UsbDevice usbDevice, String str, int i) {
        if (!hasPermission(usbDevice, str, i)) {
            throw new SecurityException("User has not given permission to device " + usbDevice);
        }
    }

    public void checkPermission(UsbAccessory usbAccessory) {
        if (!hasPermission(usbAccessory)) {
            throw new SecurityException("User has not given permission to accessory " + usbAccessory);
        }
    }

    private void requestPermissionDialog(Intent intent, String str, PendingIntent pendingIntent) {
        int callingUid = Binder.getCallingUid();
        try {
            if (this.mPackageManager.getApplicationInfo(str, 0).uid != callingUid) {
                throw new IllegalArgumentException("package " + str + " does not match caller's uid " + callingUid);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            intent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbPermissionActivity");
            intent.addFlags(268435456);
            intent.putExtra("android.intent.extra.INTENT", pendingIntent);
            intent.putExtra(Settings.ATTR_PACKAGE, str);
            intent.putExtra("android.intent.extra.UID", callingUid);
            try {
                try {
                    this.mUserContext.startActivityAsUser(intent, this.mUser);
                } catch (ActivityNotFoundException e) {
                    Slog.e(TAG, "unable to start UsbPermissionActivity");
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e2) {
            throw new IllegalArgumentException("package " + str + " not found");
        }
    }

    public void requestPermission(UsbDevice usbDevice, String str, PendingIntent pendingIntent, int i) {
        Intent intent = new Intent();
        if (hasPermission(usbDevice, str, i)) {
            intent.putExtra("device", usbDevice);
            intent.putExtra("permission", true);
            try {
                pendingIntent.send(this.mUserContext, 0, intent);
                return;
            } catch (PendingIntent.CanceledException e) {
                return;
            }
        }
        if (isCameraDevicePresent(usbDevice) && !isCameraPermissionGranted(str, i)) {
            intent.putExtra("device", usbDevice);
            intent.putExtra("permission", false);
            try {
                pendingIntent.send(this.mUserContext, 0, intent);
                return;
            } catch (PendingIntent.CanceledException e2) {
                return;
            }
        }
        intent.putExtra("device", usbDevice);
        requestPermissionDialog(intent, str, pendingIntent);
    }

    public void requestPermission(UsbAccessory usbAccessory, String str, PendingIntent pendingIntent) {
        Intent intent = new Intent();
        if (hasPermission(usbAccessory)) {
            intent.putExtra("accessory", usbAccessory);
            intent.putExtra("permission", true);
            try {
                pendingIntent.send(this.mUserContext, 0, intent);
                return;
            } catch (PendingIntent.CanceledException e) {
                return;
            }
        }
        intent.putExtra("accessory", usbAccessory);
        requestPermissionDialog(intent, str, pendingIntent);
    }

    public void grantDevicePermission(UsbDevice usbDevice, int i) {
        synchronized (this.mLock) {
            String deviceName = usbDevice.getDeviceName();
            SparseBooleanArray sparseBooleanArray = this.mDevicePermissionMap.get(deviceName);
            if (sparseBooleanArray == null) {
                sparseBooleanArray = new SparseBooleanArray(1);
                this.mDevicePermissionMap.put(deviceName, sparseBooleanArray);
            }
            sparseBooleanArray.put(i, true);
        }
    }

    public void grantAccessoryPermission(UsbAccessory usbAccessory, int i) {
        synchronized (this.mLock) {
            SparseBooleanArray sparseBooleanArray = this.mAccessoryPermissionMap.get(usbAccessory);
            if (sparseBooleanArray == null) {
                sparseBooleanArray = new SparseBooleanArray(1);
                this.mAccessoryPermissionMap.put(usbAccessory, sparseBooleanArray);
            }
            sparseBooleanArray.put(i, true);
        }
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long j2;
        long jStart = dualDumpOutputStream.start(str, j);
        synchronized (this.mLock) {
            dualDumpOutputStream.write("user_id", 1120986464257L, this.mUser.getIdentifier());
            Iterator<String> it = this.mDevicePermissionMap.keySet().iterator();
            while (true) {
                j2 = 1138166333441L;
                if (!it.hasNext()) {
                    break;
                }
                String next = it.next();
                long jStart2 = dualDumpOutputStream.start("device_permissions", 2246267895810L);
                dualDumpOutputStream.write("device_name", 1138166333441L, next);
                SparseBooleanArray sparseBooleanArray = this.mDevicePermissionMap.get(next);
                int size = sparseBooleanArray.size();
                for (int i = 0; i < size; i++) {
                    dualDumpOutputStream.write("uids", 2220498092034L, sparseBooleanArray.keyAt(i));
                }
                dualDumpOutputStream.end(jStart2);
            }
            for (UsbAccessory usbAccessory : this.mAccessoryPermissionMap.keySet()) {
                long jStart3 = dualDumpOutputStream.start("accessory_permissions", 2246267895811L);
                dualDumpOutputStream.write("accessory_description", j2, usbAccessory.getDescription());
                SparseBooleanArray sparseBooleanArray2 = this.mAccessoryPermissionMap.get(usbAccessory);
                int size2 = sparseBooleanArray2.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    dualDumpOutputStream.write("uids", 2220498092034L, sparseBooleanArray2.keyAt(i2));
                }
                dualDumpOutputStream.end(jStart3);
                j2 = 1138166333441L;
            }
        }
        dualDumpOutputStream.end(jStart);
    }
}
