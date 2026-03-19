package com.android.server.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;

public class UsbService extends IUsbManager.Stub {
    private static final String TAG = "UsbService";
    private final UsbAlsaManager mAlsaManager;
    private final Context mContext;

    @GuardedBy("mLock")
    private int mCurrentUserId;
    private UsbDeviceManager mDeviceManager;
    private UsbHostManager mHostManager;
    private final Object mLock = new Object();
    private UsbPortManager mPortManager;
    private final UsbSettingsManager mSettingsManager;
    private final UserManager mUserManager;

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
        public void onBootPhase(int i) {
            if (i == 550) {
                this.mUsbService.systemReady();
            } else if (i == 1000) {
                this.mUsbService.bootCompleted();
            }
        }

        @Override
        public void onSwitchUser(int i) {
            this.mUsbService.onSwitchUser(i);
        }

        @Override
        public void onStopUser(int i) {
            this.mUsbService.onStopUser(UserHandle.of(i));
        }

        @Override
        public void onUnlockUser(int i) {
            this.mUsbService.onUnlockUser(i);
        }
    }

    private UsbUserSettingsManager getSettingsForUser(int i) {
        return this.mSettingsManager.getSettingsForUser(i);
    }

    public UsbService(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mSettingsManager = new UsbSettingsManager(context);
        this.mAlsaManager = new UsbAlsaManager(context);
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.usb.host")) {
            this.mHostManager = new UsbHostManager(context, this.mAlsaManager, this.mSettingsManager);
        }
        if (new File("/sys/class/android_usb").exists()) {
            this.mDeviceManager = new UsbDeviceManager(context, this.mAlsaManager, this.mSettingsManager);
        }
        if (this.mHostManager != null || this.mDeviceManager != null) {
            this.mPortManager = new UsbPortManager(context);
        }
        onSwitchUser(0);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction()) && UsbService.this.mDeviceManager != null) {
                    UsbService.this.mDeviceManager.updateUserRestrictions();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(1000);
        intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(broadcastReceiver, intentFilter, null, null);
    }

    private void onSwitchUser(int i) {
        synchronized (this.mLock) {
            this.mCurrentUserId = i;
            UsbProfileGroupSettingsManager settingsForProfileGroup = this.mSettingsManager.getSettingsForProfileGroup(UserHandle.of(i));
            if (this.mHostManager != null) {
                this.mHostManager.setCurrentUserSettings(settingsForProfileGroup);
            }
            if (this.mDeviceManager != null) {
                this.mDeviceManager.setCurrentUser(i, settingsForProfileGroup);
            }
        }
    }

    private void onStopUser(UserHandle userHandle) {
        this.mSettingsManager.remove(userHandle);
    }

    public void systemReady() {
        this.mAlsaManager.systemReady();
        if (this.mDeviceManager != null) {
            this.mDeviceManager.systemReady();
        }
        if (this.mHostManager != null) {
            this.mHostManager.systemReady();
        }
        if (this.mPortManager != null) {
            this.mPortManager.systemReady();
        }
    }

    public void bootCompleted() {
        if (this.mDeviceManager != null) {
            this.mDeviceManager.bootCompleted();
        }
    }

    public void onUnlockUser(int i) {
        if (this.mDeviceManager != null) {
            this.mDeviceManager.onUnlockUser(i);
        }
    }

    public void getDeviceList(Bundle bundle) {
        if (this.mHostManager != null) {
            this.mHostManager.getDeviceList(bundle);
        }
    }

    @GuardedBy("mLock")
    private boolean isCallerInCurrentUserProfileGroupLocked() {
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return this.mUserManager.isSameProfileGroup(callingUserId, this.mCurrentUserId);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public ParcelFileDescriptor openDevice(String str, String str2) {
        ParcelFileDescriptor parcelFileDescriptorOpenDevice = null;
        if (this.mHostManager != null) {
            synchronized (this.mLock) {
                if (str != null) {
                    try {
                        int callingUserId = UserHandle.getCallingUserId();
                        if (isCallerInCurrentUserProfileGroupLocked()) {
                            parcelFileDescriptorOpenDevice = this.mHostManager.openDevice(str, getSettingsForUser(callingUserId), str2, Binder.getCallingUid());
                        } else {
                            Slog.w(TAG, "Cannot open " + str + " for user " + callingUserId + " as user is not active.");
                        }
                    } finally {
                    }
                }
            }
        }
        return parcelFileDescriptorOpenDevice;
    }

    public UsbAccessory getCurrentAccessory() {
        if (this.mDeviceManager != null) {
            return this.mDeviceManager.getCurrentAccessory();
        }
        return null;
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory usbAccessory) {
        if (this.mDeviceManager != null) {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (this.mLock) {
                if (isCallerInCurrentUserProfileGroupLocked()) {
                    return this.mDeviceManager.openAccessory(usbAccessory, getSettingsForUser(callingUserId));
                }
                Slog.w(TAG, "Cannot open " + usbAccessory + " for user " + callingUserId + " as user is not active.");
                return null;
            }
        }
        return null;
    }

    public ParcelFileDescriptor getControlFd(long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_MTP", null);
        return this.mDeviceManager.getControlFd(j);
    }

    public void setDevicePackage(UsbDevice usbDevice, String str, int i) {
        UsbDevice usbDevice2 = (UsbDevice) Preconditions.checkNotNull(usbDevice);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle userHandleOf = UserHandle.of(i);
        this.mSettingsManager.getSettingsForProfileGroup(userHandleOf).setDevicePackage(usbDevice2, str, userHandleOf);
    }

    public void setAccessoryPackage(UsbAccessory usbAccessory, String str, int i) {
        UsbAccessory usbAccessory2 = (UsbAccessory) Preconditions.checkNotNull(usbAccessory);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle userHandleOf = UserHandle.of(i);
        this.mSettingsManager.getSettingsForProfileGroup(userHandleOf).setAccessoryPackage(usbAccessory2, str, userHandleOf);
    }

    public boolean hasDevicePermission(UsbDevice usbDevice, String str) {
        return getSettingsForUser(UserHandle.getCallingUserId()).hasPermission(usbDevice, str, Binder.getCallingUid());
    }

    public boolean hasAccessoryPermission(UsbAccessory usbAccessory) {
        return getSettingsForUser(UserHandle.getCallingUserId()).hasPermission(usbAccessory);
    }

    public void requestDevicePermission(UsbDevice usbDevice, String str, PendingIntent pendingIntent) {
        getSettingsForUser(UserHandle.getCallingUserId()).requestPermission(usbDevice, str, pendingIntent, Binder.getCallingUid());
    }

    public void requestAccessoryPermission(UsbAccessory usbAccessory, String str, PendingIntent pendingIntent) {
        getSettingsForUser(UserHandle.getCallingUserId()).requestPermission(usbAccessory, str, pendingIntent);
    }

    public void grantDevicePermission(UsbDevice usbDevice, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(UserHandle.getUserId(i)).grantDevicePermission(usbDevice, i);
    }

    public void grantAccessoryPermission(UsbAccessory usbAccessory, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        getSettingsForUser(UserHandle.getUserId(i)).grantAccessoryPermission(usbAccessory, i);
    }

    public boolean hasDefaults(String str, int i) {
        String str2 = (String) Preconditions.checkStringNotEmpty(str);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle userHandleOf = UserHandle.of(i);
        return this.mSettingsManager.getSettingsForProfileGroup(userHandleOf).hasDefaults(str2, userHandleOf);
    }

    public void clearDefaults(String str, int i) {
        String str2 = (String) Preconditions.checkStringNotEmpty(str);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle userHandleOf = UserHandle.of(i);
        this.mSettingsManager.getSettingsForProfileGroup(userHandleOf).clearDefaults(str2, userHandleOf);
    }

    public void setCurrentFunctions(long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkArgument(UsbManager.areSettableFunctions(j));
        Preconditions.checkState(this.mDeviceManager != null);
        this.mDeviceManager.setCurrentFunctions(j);
    }

    public void setCurrentFunction(String str, boolean z) {
        setCurrentFunctions(UsbManager.usbFunctionsFromString(str));
    }

    public boolean isFunctionEnabled(String str) {
        return (getCurrentFunctions() & UsbManager.usbFunctionsFromString(str)) != 0;
    }

    public long getCurrentFunctions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkState(this.mDeviceManager != null);
        return this.mDeviceManager.getCurrentFunctions();
    }

    public void setScreenUnlockedFunctions(long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkArgument(UsbManager.areSettableFunctions(j));
        Preconditions.checkState(this.mDeviceManager != null);
        this.mDeviceManager.setScreenUnlockedFunctions(j);
    }

    public long getScreenUnlockedFunctions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkState(this.mDeviceManager != null);
        return this.mDeviceManager.getScreenUnlockedFunctions();
    }

    public void allowUsbDebugging(boolean z, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.allowUsbDebugging(z, str);
    }

    public void denyUsbDebugging() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.denyUsbDebugging();
    }

    public void clearUsbDebuggingKeys() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.clearUsbDebuggingKeys();
    }

    public UsbPort[] getPorts() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mPortManager != null ? this.mPortManager.getPorts() : null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public UsbPortStatus getPortStatus(String str) {
        Preconditions.checkNotNull(str, "portId must not be null");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mPortManager != null ? this.mPortManager.getPortStatus(str) : null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setPortRoles(String str, int i, int i2) {
        Preconditions.checkNotNull(str, "portId must not be null");
        UsbPort.checkRoles(i, i2);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mPortManager != null) {
                this.mPortManager.setPortRoles(str, i, i2, null);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setUsbDeviceConnectionHandler(ComponentName componentName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        synchronized (this.mLock) {
            if (this.mCurrentUserId == UserHandle.getCallingUserId()) {
                if (this.mHostManager != null) {
                    this.mHostManager.setUsbDeviceConnectionHandler(componentName);
                }
            } else {
                throw new IllegalArgumentException("Only the current user can register a usb connection handler");
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        DualDumpOutputStream dualDumpOutputStream;
        byte b;
        int i;
        int iHashCode;
        byte b2;
        int i2;
        int iHashCode2;
        byte b3;
        byte b4;
        byte b5;
        int i3;
        int iHashCode3;
        byte b6;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ArraySet arraySet = new ArraySet();
                Collections.addAll(arraySet, strArr);
                boolean z = arraySet.contains(PriorityDump.PROTO_ARG);
                if (strArr == null || strArr.length == 0 || strArr[0].equals("-a") || z) {
                    if (z) {
                        dualDumpOutputStream = new DualDumpOutputStream(new ProtoOutputStream(fileDescriptor));
                    } else {
                        indentingPrintWriter.println("USB MANAGER STATE (dumpsys usb):");
                        dualDumpOutputStream = new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  "));
                    }
                    if (this.mDeviceManager != null) {
                        this.mDeviceManager.dump(dualDumpOutputStream, "device_manager", 1146756268033L);
                    }
                    if (this.mHostManager != null) {
                        this.mHostManager.dump(dualDumpOutputStream, "host_manager", 1146756268034L);
                    }
                    if (this.mPortManager != null) {
                        this.mPortManager.dump(dualDumpOutputStream, "port_manager", 1146756268035L);
                    }
                    this.mAlsaManager.dump(dualDumpOutputStream, "alsa_manager", 1146756268036L);
                    this.mSettingsManager.dump(dualDumpOutputStream, "settings_manager", 1146756268037L);
                    dualDumpOutputStream.flush();
                } else {
                    int i4 = 2;
                    if ("set-port-roles".equals(strArr[0]) && strArr.length == 4) {
                        String str = strArr[1];
                        String str2 = strArr[2];
                        int iHashCode4 = str2.hashCode();
                        if (iHashCode4 == -896505829) {
                            if (str2.equals("source")) {
                                b5 = 0;
                            }
                            switch (b5) {
                            }
                            String str3 = strArr[3];
                            iHashCode3 = str3.hashCode();
                            if (iHashCode3 == -1335157162) {
                            }
                        } else if (iHashCode4 != -440560135) {
                            b5 = (iHashCode4 == 3530387 && str2.equals("sink")) ? (byte) 1 : (byte) -1;
                            switch (b5) {
                                case 0:
                                    i3 = 1;
                                    break;
                                case 1:
                                    i3 = 2;
                                    break;
                                case 2:
                                    i3 = 0;
                                    break;
                                default:
                                    indentingPrintWriter.println("Invalid power role: " + strArr[2]);
                                    return;
                            }
                            String str32 = strArr[3];
                            iHashCode3 = str32.hashCode();
                            if (iHashCode3 == -1335157162) {
                                if (str32.equals("device")) {
                                    b6 = 1;
                                }
                                switch (b6) {
                                }
                                if (this.mPortManager != null) {
                                }
                            } else if (iHashCode3 != 3208616) {
                                b6 = (iHashCode3 == 2063627318 && str32.equals("no-data")) ? (byte) 2 : (byte) -1;
                                switch (b6) {
                                    case 0:
                                        i4 = 1;
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        i4 = 0;
                                        break;
                                    default:
                                        indentingPrintWriter.println("Invalid data role: " + strArr[3]);
                                        return;
                                }
                                if (this.mPortManager != null) {
                                    this.mPortManager.setPortRoles(str, i3, i4, indentingPrintWriter);
                                    indentingPrintWriter.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                                }
                            } else {
                                if (str32.equals(WatchlistLoggingHandler.WatchlistEventKeys.HOST)) {
                                    b6 = 0;
                                }
                                switch (b6) {
                                }
                                if (this.mPortManager != null) {
                                }
                            }
                        } else {
                            if (str2.equals("no-power")) {
                                b5 = 2;
                            }
                            switch (b5) {
                            }
                            String str322 = strArr[3];
                            iHashCode3 = str322.hashCode();
                            if (iHashCode3 == -1335157162) {
                            }
                        }
                    } else if ("add-port".equals(strArr[0]) && strArr.length == 3) {
                        String str4 = strArr[1];
                        String str5 = strArr[2];
                        int iHashCode5 = str5.hashCode();
                        if (iHashCode5 == 99374) {
                            if (str5.equals("dfp")) {
                                b4 = 1;
                            }
                            switch (b4) {
                            }
                            if (this.mPortManager != null) {
                            }
                        } else if (iHashCode5 == 115711) {
                            if (str5.equals("ufp")) {
                                b4 = 0;
                            }
                            switch (b4) {
                            }
                            if (this.mPortManager != null) {
                            }
                        } else if (iHashCode5 != 3094652) {
                            b4 = (iHashCode5 == 3387192 && str5.equals("none")) ? (byte) 3 : (byte) -1;
                            switch (b4) {
                                case 0:
                                    i4 = 1;
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    i4 = 3;
                                    break;
                                case 3:
                                    i4 = 0;
                                    break;
                                default:
                                    indentingPrintWriter.println("Invalid mode: " + strArr[2]);
                                    return;
                            }
                            if (this.mPortManager != null) {
                                this.mPortManager.addSimulatedPort(str4, i4, indentingPrintWriter);
                                indentingPrintWriter.println();
                                this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                            }
                        } else {
                            if (str5.equals("dual")) {
                                b4 = 2;
                            }
                            switch (b4) {
                            }
                            if (this.mPortManager != null) {
                            }
                        }
                    } else if ("connect-port".equals(strArr[0]) && strArr.length == 5) {
                        String str6 = strArr[1];
                        boolean zEndsWith = strArr[2].endsWith("?");
                        String strRemoveLastChar = zEndsWith ? removeLastChar(strArr[2]) : strArr[2];
                        int iHashCode6 = strRemoveLastChar.hashCode();
                        if (iHashCode6 != 99374) {
                            b = (iHashCode6 == 115711 && strRemoveLastChar.equals("ufp")) ? (byte) 0 : (byte) -1;
                            switch (b) {
                                case 0:
                                    i = 1;
                                    break;
                                case 1:
                                    i = 2;
                                    break;
                                default:
                                    indentingPrintWriter.println("Invalid mode: " + strArr[2]);
                                    return;
                            }
                            boolean zEndsWith2 = strArr[3].endsWith("?");
                            String strRemoveLastChar2 = !zEndsWith2 ? removeLastChar(strArr[3]) : strArr[3];
                            iHashCode = strRemoveLastChar2.hashCode();
                            if (iHashCode == -896505829) {
                                b2 = (iHashCode == 3530387 && strRemoveLastChar2.equals("sink")) ? (byte) 1 : (byte) -1;
                                switch (b2) {
                                    case 0:
                                        i2 = 1;
                                        break;
                                    case 1:
                                        i2 = 2;
                                        break;
                                    default:
                                        indentingPrintWriter.println("Invalid power role: " + strArr[3]);
                                        return;
                                }
                                boolean zEndsWith3 = strArr[4].endsWith("?");
                                String strRemoveLastChar3 = !zEndsWith3 ? removeLastChar(strArr[4]) : strArr[4];
                                iHashCode2 = strRemoveLastChar3.hashCode();
                                if (iHashCode2 == -1335157162) {
                                    b3 = (iHashCode2 == 3208616 && strRemoveLastChar3.equals(WatchlistLoggingHandler.WatchlistEventKeys.HOST)) ? (byte) 0 : (byte) -1;
                                    switch (b3) {
                                        case 0:
                                            i4 = 1;
                                            break;
                                        case 1:
                                            break;
                                        default:
                                            indentingPrintWriter.println("Invalid data role: " + strArr[4]);
                                            return;
                                    }
                                    if (this.mPortManager != null) {
                                        this.mPortManager.connectSimulatedPort(str6, i, zEndsWith, i2, zEndsWith2, i4, zEndsWith3, indentingPrintWriter);
                                        indentingPrintWriter.println();
                                        this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                                    }
                                } else {
                                    if (strRemoveLastChar3.equals("device")) {
                                        b3 = 1;
                                    }
                                    switch (b3) {
                                    }
                                    if (this.mPortManager != null) {
                                    }
                                }
                            } else {
                                if (strRemoveLastChar2.equals("source")) {
                                    b2 = 0;
                                }
                                switch (b2) {
                                }
                                boolean zEndsWith32 = strArr[4].endsWith("?");
                                if (!zEndsWith32) {
                                }
                                iHashCode2 = strRemoveLastChar3.hashCode();
                                if (iHashCode2 == -1335157162) {
                                }
                            }
                        } else {
                            if (strRemoveLastChar.equals("dfp")) {
                                b = 1;
                            }
                            switch (b) {
                            }
                            boolean zEndsWith22 = strArr[3].endsWith("?");
                            if (!zEndsWith22) {
                            }
                            iHashCode = strRemoveLastChar2.hashCode();
                            if (iHashCode == -896505829) {
                            }
                        }
                    } else if ("disconnect-port".equals(strArr[0]) && strArr.length == 2) {
                        String str7 = strArr[1];
                        if (this.mPortManager != null) {
                            this.mPortManager.disconnectSimulatedPort(str7, indentingPrintWriter);
                            indentingPrintWriter.println();
                            this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                        }
                    } else if ("remove-port".equals(strArr[0]) && strArr.length == 2) {
                        String str8 = strArr[1];
                        if (this.mPortManager != null) {
                            this.mPortManager.removeSimulatedPort(str8, indentingPrintWriter);
                            indentingPrintWriter.println();
                            this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                        }
                    } else if ("reset".equals(strArr[0]) && strArr.length == 1) {
                        if (this.mPortManager != null) {
                            this.mPortManager.resetSimulation(indentingPrintWriter);
                            indentingPrintWriter.println();
                            this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                        }
                    } else if ("ports".equals(strArr[0]) && strArr.length == 1) {
                        if (this.mPortManager != null) {
                            this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(indentingPrintWriter, "  ")), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0L);
                        }
                    } else if ("dump-descriptors".equals(strArr[0])) {
                        this.mHostManager.dumpDescriptors(indentingPrintWriter, strArr);
                    } else {
                        indentingPrintWriter.println("Dump current USB state or issue command:");
                        indentingPrintWriter.println("  ports");
                        indentingPrintWriter.println("  set-port-roles <id> <source|sink|no-power> <host|device|no-data>");
                        indentingPrintWriter.println("  add-port <id> <ufp|dfp|dual|none>");
                        indentingPrintWriter.println("  connect-port <id> <ufp|dfp><?> <source|sink><?> <host|device><?>");
                        indentingPrintWriter.println("    (add ? suffix if mode, power role, or data role can be changed)");
                        indentingPrintWriter.println("  disconnect-port <id>");
                        indentingPrintWriter.println("  remove-port <id>");
                        indentingPrintWriter.println("  reset");
                        indentingPrintWriter.println();
                        indentingPrintWriter.println("Example USB type C port role switch:");
                        indentingPrintWriter.println("  dumpsys usb set-port-roles \"default\" source device");
                        indentingPrintWriter.println();
                        indentingPrintWriter.println("Example USB type C port simulation with full capabilities:");
                        indentingPrintWriter.println("  dumpsys usb add-port \"matrix\" dual");
                        indentingPrintWriter.println("  dumpsys usb connect-port \"matrix\" ufp? sink? device?");
                        indentingPrintWriter.println("  dumpsys usb ports");
                        indentingPrintWriter.println("  dumpsys usb disconnect-port \"matrix\"");
                        indentingPrintWriter.println("  dumpsys usb remove-port \"matrix\"");
                        indentingPrintWriter.println("  dumpsys usb reset");
                        indentingPrintWriter.println();
                        indentingPrintWriter.println("Example USB type C port where only power role can be changed:");
                        indentingPrintWriter.println("  dumpsys usb add-port \"matrix\" dual");
                        indentingPrintWriter.println("  dumpsys usb connect-port \"matrix\" dfp source? host");
                        indentingPrintWriter.println("  dumpsys usb reset");
                        indentingPrintWriter.println();
                        indentingPrintWriter.println("Example USB OTG port where id pin determines function:");
                        indentingPrintWriter.println("  dumpsys usb add-port \"matrix\" dual");
                        indentingPrintWriter.println("  dumpsys usb connect-port \"matrix\" dfp source host");
                        indentingPrintWriter.println("  dumpsys usb reset");
                        indentingPrintWriter.println();
                        indentingPrintWriter.println("Example USB device-only port:");
                        indentingPrintWriter.println("  dumpsys usb add-port \"matrix\" ufp");
                        indentingPrintWriter.println("  dumpsys usb connect-port \"matrix\" ufp sink device");
                        indentingPrintWriter.println("  dumpsys usb reset");
                        indentingPrintWriter.println();
                        indentingPrintWriter.println("Example USB device descriptors:");
                        indentingPrintWriter.println("  dumpsys usb dump-descriptors -dump-short");
                        indentingPrintWriter.println("  dumpsys usb dump-descriptors -dump-tree");
                        indentingPrintWriter.println("  dumpsys usb dump-descriptors -dump-list");
                        indentingPrintWriter.println("  dumpsys usb dump-descriptors -dump-raw");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private static String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }
}
