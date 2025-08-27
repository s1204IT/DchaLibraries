package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;

/* loaded from: classes.dex */
public class UsbBackend {
    private final boolean mFileTransferRestricted;
    private final boolean mFileTransferRestrictedBySystem;
    private final boolean mMidiSupported;
    private UsbPort mPort;
    private UsbPortStatus mPortStatus;
    private final boolean mTetheringRestricted;
    private final boolean mTetheringRestrictedBySystem;
    private final boolean mTetheringSupported;
    private UsbManager mUsbManager;

    public UsbBackend(Context context) {
        this(context, (UserManager) context.getSystemService("user"));
    }

    public UsbBackend(Context context, UserManager userManager) {
        this.mUsbManager = (UsbManager) context.getSystemService(UsbManager.class);
        this.mFileTransferRestricted = isUsbFileTransferRestricted(userManager);
        this.mFileTransferRestrictedBySystem = isUsbFileTransferRestrictedBySystem(userManager);
        this.mTetheringRestricted = isUsbTetheringRestricted(userManager);
        this.mTetheringRestrictedBySystem = isUsbTetheringRestrictedBySystem(userManager);
        this.mMidiSupported = context.getPackageManager().hasSystemFeature("android.software.midi");
        this.mTetheringSupported = ((ConnectivityManager) context.getSystemService("connectivity")).isTetheringSupported();
        updatePorts();
    }

    public long getCurrentFunctions() {
        return this.mUsbManager.getCurrentFunctions();
    }

    public void setCurrentFunctions(long j) {
        this.mUsbManager.setCurrentFunctions(j);
    }

    public long getDefaultUsbFunctions() {
        return this.mUsbManager.getScreenUnlockedFunctions();
    }

    public void setDefaultUsbFunctions(long j) {
        this.mUsbManager.setScreenUnlockedFunctions(j);
    }

    public boolean areFunctionsSupported(long j) {
        return (this.mMidiSupported || (8 & j) == 0) && !((!this.mTetheringSupported && (32 & j) != 0) || areFunctionDisallowed(j) || areFunctionsDisallowedBySystem(j));
    }

    public int getPowerRole() {
        updatePorts();
        if (this.mPortStatus == null) {
            return 0;
        }
        return this.mPortStatus.getCurrentPowerRole();
    }

    public int getDataRole() {
        updatePorts();
        if (this.mPortStatus == null) {
            return 0;
        }
        return this.mPortStatus.getCurrentDataRole();
    }

    public void setPowerRole(int i) {
        int dataRole = getDataRole();
        if (!areAllRolesSupported()) {
            switch (i) {
                case 1:
                    dataRole = 1;
                    break;
                case 2:
                    dataRole = 2;
                    break;
                default:
                    dataRole = 0;
                    break;
            }
        }
        if (this.mPort != null) {
            this.mUsbManager.setPortRoles(this.mPort, i, dataRole);
        }
    }

    public void setDataRole(int i) {
        int powerRole = getPowerRole();
        if (!areAllRolesSupported()) {
            switch (i) {
                case 1:
                    powerRole = 1;
                    break;
                case 2:
                    powerRole = 2;
                    break;
                default:
                    powerRole = 0;
                    break;
            }
        }
        if (this.mPort != null) {
            this.mUsbManager.setPortRoles(this.mPort, powerRole, i);
        }
    }

    public boolean areAllRolesSupported() {
        return this.mPort != null && this.mPortStatus != null && this.mPortStatus.isRoleCombinationSupported(2, 2) && this.mPortStatus.isRoleCombinationSupported(2, 1) && this.mPortStatus.isRoleCombinationSupported(1, 2) && this.mPortStatus.isRoleCombinationSupported(1, 1);
    }

    public static String usbFunctionsToString(long j) {
        return Long.toBinaryString(j);
    }

    public static long usbFunctionsFromString(String str) {
        return Long.parseLong(str, 2);
    }

    public static String dataRoleToString(int i) {
        return Integer.toString(i);
    }

    public static int dataRoleFromString(String str) {
        return Integer.parseInt(str);
    }

    private static boolean isUsbFileTransferRestricted(UserManager userManager) {
        return userManager.hasUserRestriction("no_usb_file_transfer");
    }

    private static boolean isUsbTetheringRestricted(UserManager userManager) {
        return userManager.hasUserRestriction("no_config_tethering");
    }

    private static boolean isUsbFileTransferRestrictedBySystem(UserManager userManager) {
        return userManager.hasBaseUserRestriction("no_usb_file_transfer", UserHandle.of(UserHandle.myUserId()));
    }

    private static boolean isUsbTetheringRestrictedBySystem(UserManager userManager) {
        return userManager.hasBaseUserRestriction("no_config_tethering", UserHandle.of(UserHandle.myUserId()));
    }

    private boolean areFunctionDisallowed(long j) {
        return (this.mFileTransferRestricted && !((4 & j) == 0 && (16 & j) == 0)) || (this.mTetheringRestricted && (j & 32) != 0);
    }

    private boolean areFunctionsDisallowedBySystem(long j) {
        return (this.mFileTransferRestrictedBySystem && !((4 & j) == 0 && (16 & j) == 0)) || (this.mTetheringRestrictedBySystem && (j & 32) != 0);
    }

    private void updatePorts() {
        this.mPort = null;
        this.mPortStatus = null;
        UsbPort[] ports = this.mUsbManager.getPorts();
        if (ports == null) {
            return;
        }
        int length = ports.length;
        for (int i = 0; i < length; i++) {
            UsbPortStatus portStatus = this.mUsbManager.getPortStatus(ports[i]);
            if (portStatus.isConnected()) {
                this.mPort = ports[i];
                this.mPortStatus = portStatus;
                return;
            }
        }
    }
}
