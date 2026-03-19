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
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UsbService extends IUsbManager.Stub {
    private static final String TAG = "UsbService";
    private final UsbAlsaManager mAlsaManager;
    private final Context mContext;
    private UsbDeviceManager mDeviceManager;
    private UsbHostManager mHostManager;
    private UsbPortManager mPortManager;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final SparseArray<UsbSettingsManager> mSettingsByUser = new SparseArray<>();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                UsbService.this.setCurrentUser(userId);
                return;
            }
            if ("android.intent.action.USER_STOPPED".equals(action)) {
                synchronized (UsbService.this.mLock) {
                    UsbService.this.mSettingsByUser.remove(userId);
                }
            } else {
                if (!"android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action) || UsbService.this.mDeviceManager == null) {
                    return;
                }
                UsbService.this.mDeviceManager.updateUserRestrictions();
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
            } else {
                if (phase != 1000) {
                    return;
                }
                this.mUsbService.bootCompleted();
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
        this.mAlsaManager = new UsbAlsaManager(context);
        PackageManager pm = this.mContext.getPackageManager();
        if (pm.hasSystemFeature("android.hardware.usb.host")) {
            this.mHostManager = new UsbHostManager(context, this.mAlsaManager);
        }
        if (new File("/sys/class/android_usb").exists()) {
            this.mDeviceManager = new UsbDeviceManager(context, this.mAlsaManager);
        }
        if (this.mHostManager != null || this.mDeviceManager != null) {
            this.mPortManager = new UsbPortManager(context);
        }
        setCurrentUser(0);
        IntentFilter filter = new IntentFilter();
        filter.setPriority(1000);
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter, null, null);
    }

    private void setCurrentUser(int userId) {
        UsbSettingsManager userSettings = getSettingsForUser(userId);
        if (this.mHostManager != null) {
            this.mHostManager.setCurrentSettings(userSettings);
        }
        if (this.mDeviceManager == null) {
            return;
        }
        this.mDeviceManager.setCurrentUser(userId, userSettings);
    }

    public void systemReady() {
        this.mAlsaManager.systemReady();
        if (this.mDeviceManager != null) {
            this.mDeviceManager.systemReady();
        }
        if (this.mHostManager != null) {
            this.mHostManager.systemReady();
        }
        if (this.mPortManager == null) {
            return;
        }
        this.mPortManager.systemReady();
    }

    public void bootCompleted() {
        if (this.mDeviceManager == null) {
            return;
        }
        this.mDeviceManager.bootCompleted();
    }

    public void getDeviceList(Bundle devices) {
        if (this.mHostManager == null) {
            return;
        }
        this.mHostManager.getDeviceList(devices);
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

    public boolean isFunctionEnabled(String function) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        if (this.mDeviceManager != null) {
            return this.mDeviceManager.isFunctionEnabled(function);
        }
        return false;
    }

    public void setCurrentFunction(String function) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        if (!isSupportedCurrentFunction(function)) {
            Slog.w(TAG, "Caller of setCurrentFunction() requested unsupported USB function: " + function);
            function = "none";
        }
        if (this.mDeviceManager != null) {
            this.mDeviceManager.setCurrentFunctions(function);
            return;
        }
        throw new IllegalStateException("USB device mode not supported");
    }

    public int getCurrentState() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        if (this.mDeviceManager != null) {
            return this.mDeviceManager.getCurrentState();
        }
        return 0;
    }

    private static boolean isSupportedCurrentFunction(String function) {
        return function == null || function.equals("none") || function.equals("audio_source") || function.equals("midi") || function.equals("mtp") || function.equals("ptp") || function.equals("rndis") || function.equals("mass_storage") || function.equals("bicr") || function.equals("eem");
    }

    public void setUsbDataUnlocked(boolean unlocked) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mDeviceManager.setUsbDataUnlocked(unlocked);
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

    public UsbPort[] getPorts() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mPortManager != null ? this.mPortManager.getPorts() : null;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public UsbPortStatus getPortStatus(String portId) {
        Preconditions.checkNotNull(portId, "portId must not be null");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mPortManager != null ? this.mPortManager.getPortStatus(portId) : null;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setPortRoles(String portId, int powerRole, int dataRole) {
        Preconditions.checkNotNull(portId, "portId must not be null");
        UsbPort.checkRoles(powerRole, dataRole);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mPortManager != null) {
                this.mPortManager.setPortRoles(portId, powerRole, dataRole, null);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        int mode;
        int powerRole;
        int dataRole;
        int supportedModes;
        int powerRole2;
        int dataRole2;
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        long ident = Binder.clearCallingIdentity();
        if (args != null) {
            try {
                if (args.length == 0 || "-a".equals(args[0])) {
                    pw.println("USB Manager State:");
                    pw.increaseIndent();
                    if (this.mDeviceManager != null) {
                        this.mDeviceManager.dump(pw);
                    }
                    if (this.mHostManager != null) {
                        this.mHostManager.dump(pw);
                    }
                    if (this.mPortManager != null) {
                        this.mPortManager.dump(pw);
                    }
                    this.mAlsaManager.dump(pw);
                    synchronized (this.mLock) {
                        for (int i = 0; i < this.mSettingsByUser.size(); i++) {
                            int userId = this.mSettingsByUser.keyAt(i);
                            UsbSettingsManager settings = this.mSettingsByUser.valueAt(i);
                            pw.println("Settings for user " + userId + ":");
                            pw.increaseIndent();
                            settings.dump(pw);
                            pw.decreaseIndent();
                        }
                    }
                } else if (args.length == 4 && "set-port-roles".equals(args[0])) {
                    String portId = args[1];
                    String str = args[2];
                    if (str.equals("source")) {
                        powerRole2 = 1;
                    } else if (str.equals("sink")) {
                        powerRole2 = 2;
                    } else {
                        if (!str.equals("no-power")) {
                            pw.println("Invalid power role: " + args[2]);
                            return;
                        }
                        powerRole2 = 0;
                    }
                    String str2 = args[3];
                    if (str2.equals("host")) {
                        dataRole2 = 1;
                    } else if (str2.equals("device")) {
                        dataRole2 = 2;
                    } else {
                        if (!str2.equals("no-data")) {
                            pw.println("Invalid data role: " + args[3]);
                            return;
                        }
                        dataRole2 = 0;
                    }
                    if (this.mPortManager != null) {
                        this.mPortManager.setPortRoles(portId, powerRole2, dataRole2, pw);
                        pw.println();
                        this.mPortManager.dump(pw);
                    }
                } else if (args.length == 3 && "add-port".equals(args[0])) {
                    String portId2 = args[1];
                    String str3 = args[2];
                    if (str3.equals("ufp")) {
                        supportedModes = 2;
                    } else if (str3.equals("dfp")) {
                        supportedModes = 1;
                    } else if (str3.equals("dual")) {
                        supportedModes = 3;
                    } else {
                        if (!str3.equals("none")) {
                            pw.println("Invalid mode: " + args[2]);
                            return;
                        }
                        supportedModes = 0;
                    }
                    if (this.mPortManager != null) {
                        this.mPortManager.addSimulatedPort(portId2, supportedModes, pw);
                        pw.println();
                        this.mPortManager.dump(pw);
                    }
                } else if (args.length == 5 && "connect-port".equals(args[0])) {
                    String portId3 = args[1];
                    boolean canChangeMode = args[2].endsWith("?");
                    String strRemoveLastChar = canChangeMode ? removeLastChar(args[2]) : args[2];
                    if (strRemoveLastChar.equals("ufp")) {
                        mode = 2;
                    } else {
                        if (!strRemoveLastChar.equals("dfp")) {
                            pw.println("Invalid mode: " + args[2]);
                            return;
                        }
                        mode = 1;
                    }
                    boolean canChangePowerRole = args[3].endsWith("?");
                    String strRemoveLastChar2 = canChangePowerRole ? removeLastChar(args[3]) : args[3];
                    if (strRemoveLastChar2.equals("source")) {
                        powerRole = 1;
                    } else {
                        if (!strRemoveLastChar2.equals("sink")) {
                            pw.println("Invalid power role: " + args[3]);
                            return;
                        }
                        powerRole = 2;
                    }
                    boolean canChangeDataRole = args[4].endsWith("?");
                    String strRemoveLastChar3 = canChangeDataRole ? removeLastChar(args[4]) : args[4];
                    if (strRemoveLastChar3.equals("host")) {
                        dataRole = 1;
                    } else {
                        if (!strRemoveLastChar3.equals("device")) {
                            pw.println("Invalid data role: " + args[4]);
                            return;
                        }
                        dataRole = 2;
                    }
                    if (this.mPortManager != null) {
                        this.mPortManager.connectSimulatedPort(portId3, mode, canChangeMode, powerRole, canChangePowerRole, dataRole, canChangeDataRole, pw);
                        pw.println();
                        this.mPortManager.dump(pw);
                    }
                } else if (args.length == 2 && "disconnect-port".equals(args[0])) {
                    String portId4 = args[1];
                    if (this.mPortManager != null) {
                        this.mPortManager.disconnectSimulatedPort(portId4, pw);
                        pw.println();
                        this.mPortManager.dump(pw);
                    }
                } else if (args.length == 2 && "remove-port".equals(args[0])) {
                    String portId5 = args[1];
                    if (this.mPortManager != null) {
                        this.mPortManager.removeSimulatedPort(portId5, pw);
                        pw.println();
                        this.mPortManager.dump(pw);
                    }
                } else if (args.length == 1 && "reset".equals(args[0])) {
                    if (this.mPortManager != null) {
                        this.mPortManager.resetSimulation(pw);
                        pw.println();
                        this.mPortManager.dump(pw);
                    }
                } else if (args.length != 1 || !"ports".equals(args[0])) {
                    pw.println("Dump current USB state or issue command:");
                    pw.println("  ports");
                    pw.println("  set-port-roles <id> <source|sink|no-power> <host|device|no-data>");
                    pw.println("  add-port <id> <ufp|dfp|dual|none>");
                    pw.println("  connect-port <id> <ufp|dfp><?> <source|sink><?> <host|device><?>");
                    pw.println("    (add ? suffix if mode, power role, or data role can be changed)");
                    pw.println("  disconnect-port <id>");
                    pw.println("  remove-port <id>");
                    pw.println("  reset");
                    pw.println();
                    pw.println("Example USB type C port role switch:");
                    pw.println("  dumpsys usb set-port-roles \"default\" source device");
                    pw.println();
                    pw.println("Example USB type C port simulation with full capabilities:");
                    pw.println("  dumpsys usb add-port \"matrix\" dual");
                    pw.println("  dumpsys usb connect-port \"matrix\" ufp? sink? device?");
                    pw.println("  dumpsys usb ports");
                    pw.println("  dumpsys usb disconnect-port \"matrix\"");
                    pw.println("  dumpsys usb remove-port \"matrix\"");
                    pw.println("  dumpsys usb reset");
                    pw.println();
                    pw.println("Example USB type C port where only power role can be changed:");
                    pw.println("  dumpsys usb add-port \"matrix\" dual");
                    pw.println("  dumpsys usb connect-port \"matrix\" dfp source? host");
                    pw.println("  dumpsys usb reset");
                    pw.println();
                    pw.println("Example USB OTG port where id pin determines function:");
                    pw.println("  dumpsys usb add-port \"matrix\" dual");
                    pw.println("  dumpsys usb connect-port \"matrix\" dfp source host");
                    pw.println("  dumpsys usb reset");
                    pw.println();
                    pw.println("Example USB device-only port:");
                    pw.println("  dumpsys usb add-port \"matrix\" ufp");
                    pw.println("  dumpsys usb connect-port \"matrix\" ufp sink device");
                    pw.println("  dumpsys usb reset");
                } else if (this.mPortManager != null) {
                    this.mPortManager.dump(pw);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private static final String removeLastChar(String value) {
        return value.substring(0, value.length() - 1);
    }
}
