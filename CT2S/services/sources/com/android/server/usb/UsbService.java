package com.android.server.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UsbService extends IUsbManager.Stub {
    private static final String TAG = "UsbService";
    private final Context mContext;
    private UsbDeviceManager mDeviceManager;
    private UsbHostManager mHostManager;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final SparseArray<UsbSettingsManager> mSettingsByUser = new SparseArray<>();
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                UsbService.this.setCurrentUser(userId);
            } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                synchronized (UsbService.this.mLock) {
                    UsbService.this.mSettingsByUser.remove(userId);
                }
            }
        }
    };

    public static class Lifecycle extends SystemService {
        private UsbService mUsbService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mUsbService = new UsbService(getContext());
            publishBinderService("usb", this.mUsbService);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mUsbService.systemReady();
            }
        }
    }

    private UsbSettingsManager getSettingsForUser(int userId) {
        UsbSettingsManager settings;
        synchronized (this.mLock) {
            settings = this.mSettingsByUser.get(userId);
            if (settings == null) {
                settings = new UsbSettingsManager(this.mContext, new UserHandle(userId));
                this.mSettingsByUser.put(userId, settings);
            }
        }
        return settings;
    }

    public UsbService(Context context) {
        this.mContext = context;
        PackageManager pm = this.mContext.getPackageManager();
        if (pm.hasSystemFeature("android.hardware.usb.host")) {
            this.mHostManager = new UsbHostManager(context);
        }
        if (new File("/sys/class/android_usb").exists()) {
            this.mDeviceManager = new UsbDeviceManager(context);
        }
        setCurrentUser(0);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_SWITCHED");
        userFilter.addAction("android.intent.action.USER_STOPPED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, null);
    }

    private void setCurrentUser(int userId) {
        UsbSettingsManager userSettings = getSettingsForUser(userId);
        if (this.mHostManager != null) {
            this.mHostManager.setCurrentSettings(userSettings);
        }
        if (this.mDeviceManager != null) {
            this.mDeviceManager.setCurrentSettings(userSettings);
        }
    }

    public void systemReady() {
        if (this.mDeviceManager != null) {
            this.mDeviceManager.systemReady();
        }
        if (this.mHostManager != null) {
            this.mHostManager.systemReady();
        }
    }

    public void getDeviceList(Bundle devices) {
        if (this.mHostManager != null) {
            this.mHostManager.getDeviceList(devices);
        }
    }

    public ParcelFileDescriptor openDevice(String deviceName) {
        if (this.mHostManager != null) {
            return this.mHostManager.openDevice(deviceName);
        }
        return null;
    }

    public UsbAccessory getCurrentAccessory() {
        if (this.mDeviceManager != null) {
            return this.mDeviceManager.getCurrentAccessory();
        }
        return null;
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        if (this.mDeviceManager != null) {
            return this.mDeviceManager.openAccessory(accessory);
        }
        return null;
    }

    public void setDevicePackage(UsbDevice device, String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(userId).setDevicePackage(device, packageName);
    }

    public void setAccessoryPackage(UsbAccessory accessory, String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(userId).setAccessoryPackage(accessory, packageName);
    }

    public boolean hasDevicePermission(UsbDevice device) {
        int userId = UserHandle.getCallingUserId();
        return getSettingsForUser(userId).hasPermission(device);
    }

    public boolean hasAccessoryPermission(UsbAccessory accessory) {
        int userId = UserHandle.getCallingUserId();
        return getSettingsForUser(userId).hasPermission(accessory);
    }

    public void requestDevicePermission(UsbDevice device, String packageName, PendingIntent pi) {
        int userId = UserHandle.getCallingUserId();
        getSettingsForUser(userId).requestPermission(device, packageName, pi);
    }

    public void requestAccessoryPermission(UsbAccessory accessory, String packageName, PendingIntent pi) {
        int userId = UserHandle.getCallingUserId();
        getSettingsForUser(userId).requestPermission(accessory, packageName, pi);
    }

    public void grantDevicePermission(UsbDevice device, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        int userId = UserHandle.getUserId(uid);
        getSettingsForUser(userId).grantDevicePermission(device, uid);
    }

    public void grantAccessoryPermission(UsbAccessory accessory, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        int userId = UserHandle.getUserId(uid);
        getSettingsForUser(userId).grantAccessoryPermission(accessory, uid);
    }

    public boolean hasDefaults(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        return getSettingsForUser(userId).hasDefaults(packageName);
    }

    public void clearDefaults(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(userId).clearDefaults(packageName);
    }

    public void setCurrentFunction(String function, boolean makeDefault) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager.hasUserRestriction("no_usb_file_transfer")) {
            if (this.mDeviceManager != null) {
                this.mDeviceManager.setCurrentFunctions("none", false);
            }
        } else {
            if (this.mDeviceManager != null) {
                this.mDeviceManager.setCurrentFunctions(function, makeDefault);
                return;
            }
            throw new IllegalStateException("USB device mode not supported");
        }
    }

    public void setMassStorageBackingFile(String path) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        if (this.mDeviceManager != null) {
            this.mDeviceManager.setMassStorageBackingFile(path);
            return;
        }
        throw new IllegalStateException("USB device mode not supported");
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.allowUsbDebugging(alwaysAllow, publicKey);
    }

    public void denyUsbDebugging() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.denyUsbDebugging();
    }

    public void clearUsbDebuggingKeys() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.clearUsbDebuggingKeys();
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(writer, "  ");
        indentingPrintWriter.println("USB Manager State:");
        if (this.mDeviceManager != null) {
            this.mDeviceManager.dump(fd, indentingPrintWriter);
        }
        if (this.mHostManager != null) {
            this.mHostManager.dump(fd, indentingPrintWriter);
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mSettingsByUser.size(); i++) {
                int userId = this.mSettingsByUser.keyAt(i);
                UsbSettingsManager settings = this.mSettingsByUser.valueAt(i);
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("Settings for user " + userId + ":");
                settings.dump(fd, indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
            }
        }
        indentingPrintWriter.decreaseIndent();
    }
}
