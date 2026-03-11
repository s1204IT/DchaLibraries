package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class UsbBackend {
    private static boolean sBicrSupport = "yes".equals(SystemProperties.get("ro.sys.usb.bicr", "no"));
    private static boolean sUmsSupport = SystemProperties.get("ro.sys.usb.storage.type", "mtp").equals("mtp,mass_storage");
    private boolean mIsUnlocked;
    private final boolean mMidi;
    private UsbPort mPort;
    private UsbPortStatus mPortStatus;
    private final boolean mRestricted;
    private final boolean mRestrictedBySystem;
    private UsbManager mUsbManager;
    private UserManager mUserManager;

    public UsbBackend(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        this.mIsUnlocked = intent != null ? intent.getBooleanExtra("unlocked", false) : false;
        this.mUserManager = UserManager.get(context);
        this.mUsbManager = (UsbManager) context.getSystemService(UsbManager.class);
        this.mRestricted = this.mUserManager.hasUserRestriction("no_usb_file_transfer");
        this.mRestrictedBySystem = this.mUserManager.hasBaseUserRestriction("no_usb_file_transfer", UserHandle.of(UserHandle.myUserId()));
        this.mMidi = context.getPackageManager().hasSystemFeature("android.software.midi");
        UsbPort[] ports = this.mUsbManager.getPorts();
        int N = ports.length;
        for (int i = 0; i < N; i++) {
            UsbPortStatus status = this.mUsbManager.getPortStatus(ports[i]);
            if (status.isConnected()) {
                this.mPort = ports[i];
                this.mPortStatus = status;
                return;
            }
        }
    }

    public int getCurrentMode() {
        if (this.mPort != null) {
            int power = this.mPortStatus.getCurrentPowerRole() == 1 ? 1 : 0;
            return getUsbDataMode() | power;
        }
        return getUsbDataMode() | 0;
    }

    public int getUsbDataMode() {
        if (!this.mIsUnlocked) {
            return 0;
        }
        if (this.mUsbManager.isFunctionEnabled("mtp")) {
            return 2;
        }
        if (this.mUsbManager.isFunctionEnabled("ptp")) {
            return 4;
        }
        if (this.mUsbManager.isFunctionEnabled("midi")) {
            return 6;
        }
        if (this.mUsbManager.isFunctionEnabled("mass_storage")) {
            return 8;
        }
        return this.mUsbManager.isFunctionEnabled("bicr") ? 10 : 0;
    }

    private void setUsbFunction(int mode) {
        switch (mode) {
            case DefaultWfcSettingsExt.CREATE:
                this.mUsbManager.setCurrentFunction("mtp");
                this.mUsbManager.setUsbDataUnlocked(true);
                break;
            case DefaultWfcSettingsExt.DESTROY:
            case 5:
            case 7:
            case 9:
            default:
                this.mUsbManager.setCurrentFunction(null);
                this.mUsbManager.setUsbDataUnlocked(false);
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                this.mUsbManager.setCurrentFunction("ptp");
                this.mUsbManager.setUsbDataUnlocked(true);
                break;
            case 6:
                this.mUsbManager.setCurrentFunction("midi");
                this.mUsbManager.setUsbDataUnlocked(true);
                break;
            case 8:
                this.mUsbManager.setCurrentFunction("mass_storage");
                this.mUsbManager.setUsbDataUnlocked(true);
                break;
            case 10:
                this.mUsbManager.setCurrentFunction("bicr");
                this.mUsbManager.setUsbDataUnlocked(true);
                break;
        }
    }

    public void setMode(int mode) {
        if (this.mPort != null) {
            int powerRole = modeToPower(mode);
            int dataRole = ((mode & 14) == 0 && this.mPortStatus.isRoleCombinationSupported(powerRole, 1)) ? 1 : 2;
            this.mUsbManager.setPortRoles(this.mPort, powerRole, dataRole);
        }
        setUsbFunction(mode & 14);
    }

    private int modeToPower(int mode) {
        return (mode & 1) == 1 ? 1 : 2;
    }

    public boolean isModeDisallowed(int mode) {
        return (!this.mRestricted || (mode & 14) == 0 || (mode & 14) == 6) ? false : true;
    }

    public boolean isModeDisallowedBySystem(int mode) {
        return (!this.mRestrictedBySystem || (mode & 14) == 0 || (mode & 14) == 6) ? false : true;
    }

    public boolean isModeSupported(int mode) {
        if (!this.mMidi && (mode & 14) == 6) {
            return false;
        }
        if (this.mPort != null) {
            int power = modeToPower(mode);
            if ((mode & 14) != 0) {
                return this.mPortStatus.isRoleCombinationSupported(power, 2);
            }
            if (this.mPortStatus.isRoleCombinationSupported(power, 2)) {
                return true;
            }
            return this.mPortStatus.isRoleCombinationSupported(power, 1);
        }
        boolean added = true;
        switch (mode & 14) {
            case 8:
                added = sUmsSupport;
                break;
            case 10:
                added = sBicrSupport;
                break;
        }
        return added && (mode & 1) != 1;
    }
}
