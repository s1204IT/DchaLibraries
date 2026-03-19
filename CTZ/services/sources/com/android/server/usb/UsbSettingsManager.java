package com.android.server.usb;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.pm.DumpState;

class UsbSettingsManager {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = UsbSettingsManager.class.getSimpleName();
    private final Context mContext;
    private UserManager mUserManager;

    @GuardedBy("mSettingsByUser")
    private final SparseArray<UsbUserSettingsManager> mSettingsByUser = new SparseArray<>();

    @GuardedBy("mSettingsByProfileGroup")
    private final SparseArray<UsbProfileGroupSettingsManager> mSettingsByProfileGroup = new SparseArray<>();

    public UsbSettingsManager(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    UsbUserSettingsManager getSettingsForUser(int i) {
        UsbUserSettingsManager usbUserSettingsManager;
        synchronized (this.mSettingsByUser) {
            usbUserSettingsManager = this.mSettingsByUser.get(i);
            if (usbUserSettingsManager == null) {
                usbUserSettingsManager = new UsbUserSettingsManager(this.mContext, new UserHandle(i));
                this.mSettingsByUser.put(i, usbUserSettingsManager);
            }
        }
        return usbUserSettingsManager;
    }

    UsbProfileGroupSettingsManager getSettingsForProfileGroup(UserHandle userHandle) {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        UserInfo profileParent = this.mUserManager.getProfileParent(userHandle.getIdentifier());
        if (profileParent != null) {
            userHandle = profileParent.getUserHandle();
        }
        synchronized (this.mSettingsByProfileGroup) {
            usbProfileGroupSettingsManager = this.mSettingsByProfileGroup.get(userHandle.getIdentifier());
            if (usbProfileGroupSettingsManager == null) {
                usbProfileGroupSettingsManager = new UsbProfileGroupSettingsManager(this.mContext, userHandle, this);
                this.mSettingsByProfileGroup.put(userHandle.getIdentifier(), usbProfileGroupSettingsManager);
            }
        }
        return usbProfileGroupSettingsManager;
    }

    void remove(UserHandle userHandle) {
        synchronized (this.mSettingsByUser) {
            this.mSettingsByUser.remove(userHandle.getIdentifier());
        }
        synchronized (this.mSettingsByProfileGroup) {
            if (this.mSettingsByProfileGroup.indexOfKey(userHandle.getIdentifier()) >= 0) {
                this.mSettingsByProfileGroup.remove(userHandle.getIdentifier());
            } else {
                int size = this.mSettingsByProfileGroup.size();
                for (int i = 0; i < size; i++) {
                    this.mSettingsByProfileGroup.valueAt(i).removeAllDefaultsForUser(userHandle);
                }
            }
        }
    }

    void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        int i;
        long jStart = dualDumpOutputStream.start(str, j);
        synchronized (this.mSettingsByUser) {
            int size = this.mSettingsByUser.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mSettingsByUser.valueAt(i2).dump(dualDumpOutputStream, "user_settings", 2246267895809L);
            }
        }
        synchronized (this.mSettingsByProfileGroup) {
            int size2 = this.mSettingsByProfileGroup.size();
            for (i = 0; i < size2; i++) {
                this.mSettingsByProfileGroup.valueAt(i).dump(dualDumpOutputStream, "profile_group_settings", 2246267895810L);
            }
        }
        dualDumpOutputStream.end(jStart);
    }

    void usbDeviceRemoved(UsbDevice usbDevice) {
        synchronized (this.mSettingsByUser) {
            for (int i = 0; i < this.mSettingsByUser.size(); i++) {
                this.mSettingsByUser.valueAt(i).removeDevicePermissions(usbDevice);
            }
        }
        Intent intent = new Intent("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.putExtra("device", usbDevice);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    void usbAccessoryRemoved(UsbAccessory usbAccessory) {
        synchronized (this.mSettingsByUser) {
            for (int i = 0; i < this.mSettingsByUser.size(); i++) {
                this.mSettingsByUser.valueAt(i).removeAccessoryPermissions(usbAccessory);
            }
        }
        Intent intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_DETACHED");
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.putExtra("accessory", usbAccessory);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }
}
