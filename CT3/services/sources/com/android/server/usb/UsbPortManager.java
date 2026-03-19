package com.android.server.usb;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.FgThread;
import java.io.File;
import java.io.IOException;
import libcore.io.IoUtils;

public class UsbPortManager {
    private static final int MSG_UPDATE_PORTS = 1;
    private static final String PORT_DATA_ROLE_DEVICE = "device";
    private static final String PORT_DATA_ROLE_HOST = "host";
    private static final String PORT_MODE_DFP = "dfp";
    private static final String PORT_MODE_UFP = "ufp";
    private static final String PORT_POWER_ROLE_SINK = "sink";
    private static final String PORT_POWER_ROLE_SOURCE = "source";
    private static final String SYSFS_CLASS = "/sys/class/dual_role_usb";
    private static final String SYSFS_PORT_DATA_ROLE = "data_role";
    private static final String SYSFS_PORT_MODE = "mode";
    private static final String SYSFS_PORT_POWER_ROLE = "power_role";
    private static final String SYSFS_PORT_SUPPORTED_MODES = "supported_modes";
    private static final String TAG = "UsbPortManager";
    private static final String UEVENT_FILTER = "SUBSYSTEM=dual_role_usb";
    private static final String USB_TYPEC_PROP_PREFIX = "sys.usb.typec.";
    private static final String USB_TYPEC_STATE = "sys.usb.typec.state";
    private final Context mContext;
    private static final int COMBO_SOURCE_HOST = UsbPort.combineRolesAsBit(1, 1);
    private static final int COMBO_SOURCE_DEVICE = UsbPort.combineRolesAsBit(1, 2);
    private static final int COMBO_SINK_HOST = UsbPort.combineRolesAsBit(2, 1);
    private static final int COMBO_SINK_DEVICE = UsbPort.combineRolesAsBit(2, 2);
    private final Object mLock = new Object();
    private final ArrayMap<String, PortInfo> mPorts = new ArrayMap<>();
    private final ArrayMap<String, SimulatedPortInfo> mSimulatedPorts = new ArrayMap<>();
    private final Handler mHandler = new Handler(FgThread.get().getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (UsbPortManager.this.mLock) {
                        UsbPortManager.this.updatePortsLocked(null);
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private final UEventObserver mUEventObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent event) {
            UsbPortManager.this.scheduleUpdatePorts();
        }
    };
    private final boolean mHaveKernelSupport = new File(SYSFS_CLASS).exists();

    public UsbPortManager(Context context) {
        this.mContext = context;
    }

    public void systemReady() {
        this.mUEventObserver.startObserving(UEVENT_FILTER);
        scheduleUpdatePorts();
    }

    public UsbPort[] getPorts() {
        UsbPort[] result;
        synchronized (this.mLock) {
            int count = this.mPorts.size();
            result = new UsbPort[count];
            for (int i = 0; i < count; i++) {
                result[i] = this.mPorts.valueAt(i).mUsbPort;
            }
        }
        return result;
    }

    public UsbPortStatus getPortStatus(String portId) {
        UsbPortStatus usbPortStatus;
        synchronized (this.mLock) {
            PortInfo portInfo = this.mPorts.get(portId);
            usbPortStatus = portInfo != null ? portInfo.mUsbPortStatus : null;
        }
        return usbPortStatus;
    }

    public void setPortRoles(String portId, int newPowerRole, int newDataRole, IndentingPrintWriter pw) {
        int newMode;
        synchronized (this.mLock) {
            PortInfo portInfo = this.mPorts.get(portId);
            if (portInfo == null) {
                if (pw != null) {
                    pw.println("No such USB port: " + portId);
                }
                return;
            }
            if (!portInfo.mUsbPortStatus.isRoleCombinationSupported(newPowerRole, newDataRole)) {
                logAndPrint(6, pw, "Attempted to set USB port into unsupported role combination: portId=" + portId + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
                return;
            }
            int currentDataRole = portInfo.mUsbPortStatus.getCurrentDataRole();
            int currentPowerRole = portInfo.mUsbPortStatus.getCurrentPowerRole();
            if (currentDataRole == newDataRole && currentPowerRole == newPowerRole) {
                if (pw != null) {
                    pw.println("No change.");
                }
                return;
            }
            boolean canChangeMode = portInfo.mCanChangeMode;
            boolean canChangePowerRole = portInfo.mCanChangePowerRole;
            boolean canChangeDataRole = portInfo.mCanChangeDataRole;
            int currentMode = portInfo.mUsbPortStatus.getCurrentMode();
            if ((canChangePowerRole || currentPowerRole == newPowerRole) && (canChangeDataRole || currentDataRole == newDataRole)) {
                newMode = currentMode;
            } else if (canChangeMode && newPowerRole == 1 && newDataRole == 1) {
                newMode = 1;
            } else {
                if (!canChangeMode || newPowerRole != 2 || newDataRole != 2) {
                    logAndPrint(6, pw, "Found mismatch in supported USB role combinations while attempting to change role: " + portInfo + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
                    return;
                }
                newMode = 2;
            }
            logAndPrint(4, pw, "Setting USB port mode and role: portId=" + portId + ", currentMode=" + UsbPort.modeToString(currentMode) + ", currentPowerRole=" + UsbPort.powerRoleToString(currentPowerRole) + ", currentDataRole=" + UsbPort.dataRoleToString(currentDataRole) + ", newMode=" + UsbPort.modeToString(newMode) + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
            SimulatedPortInfo sim = this.mSimulatedPorts.get(portId);
            if (sim != null) {
                sim.mCurrentMode = newMode;
                sim.mCurrentPowerRole = newPowerRole;
                sim.mCurrentDataRole = newDataRole;
            } else if (this.mHaveKernelSupport) {
                File portDir = new File(SYSFS_CLASS, portId);
                if (!portDir.exists()) {
                    logAndPrint(6, pw, "USB port not found: portId=" + portId);
                    return;
                }
                if (currentMode != newMode) {
                    if (!writeFile(portDir, SYSFS_PORT_MODE, newMode == 1 ? PORT_MODE_DFP : PORT_MODE_UFP)) {
                        logAndPrint(6, pw, "Failed to set the USB port mode: portId=" + portId + ", newMode=" + UsbPort.modeToString(newMode));
                        return;
                    }
                } else {
                    if (currentPowerRole != newPowerRole) {
                        if (!writeFile(portDir, SYSFS_PORT_POWER_ROLE, newPowerRole == 1 ? PORT_POWER_ROLE_SOURCE : PORT_POWER_ROLE_SINK)) {
                            logAndPrint(6, pw, "Failed to set the USB port power role: portId=" + portId + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole));
                            return;
                        }
                    }
                    if (currentDataRole != newDataRole) {
                        if (!writeFile(portDir, SYSFS_PORT_DATA_ROLE, newDataRole == 1 ? PORT_DATA_ROLE_HOST : PORT_DATA_ROLE_DEVICE)) {
                            logAndPrint(6, pw, "Failed to set the USB port data role: portId=" + portId + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
                            return;
                        }
                    }
                }
            }
            updatePortsLocked(pw);
        }
    }

    public void addSimulatedPort(String portId, int supportedModes, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mSimulatedPorts.containsKey(portId)) {
                pw.println("Port with same name already exists.  Please remove it first.");
                return;
            }
            pw.println("Adding simulated port: portId=" + portId + ", supportedModes=" + UsbPort.modeToString(supportedModes));
            this.mSimulatedPorts.put(portId, new SimulatedPortInfo(portId, supportedModes));
            updatePortsLocked(pw);
        }
    }

    public void connectSimulatedPort(String portId, int mode, boolean canChangeMode, int powerRole, boolean canChangePowerRole, int dataRole, boolean canChangeDataRole, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            SimulatedPortInfo portInfo = this.mSimulatedPorts.get(portId);
            if (portInfo == null) {
                pw.println("Cannot connect simulated port which does not exist.");
                return;
            }
            if (mode == 0 || powerRole == 0 || dataRole == 0) {
                pw.println("Cannot connect simulated port in null mode, power role, or data role.");
                return;
            }
            if ((portInfo.mSupportedModes & mode) == 0) {
                pw.println("Simulated port does not support mode: " + UsbPort.modeToString(mode));
                return;
            }
            pw.println("Connecting simulated port: portId=" + portId + ", mode=" + UsbPort.modeToString(mode) + ", canChangeMode=" + canChangeMode + ", powerRole=" + UsbPort.powerRoleToString(powerRole) + ", canChangePowerRole=" + canChangePowerRole + ", dataRole=" + UsbPort.dataRoleToString(dataRole) + ", canChangeDataRole=" + canChangeDataRole);
            portInfo.mCurrentMode = mode;
            portInfo.mCanChangeMode = canChangeMode;
            portInfo.mCurrentPowerRole = powerRole;
            portInfo.mCanChangePowerRole = canChangePowerRole;
            portInfo.mCurrentDataRole = dataRole;
            portInfo.mCanChangeDataRole = canChangeDataRole;
            updatePortsLocked(pw);
        }
    }

    public void disconnectSimulatedPort(String portId, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            SimulatedPortInfo portInfo = this.mSimulatedPorts.get(portId);
            if (portInfo == null) {
                pw.println("Cannot disconnect simulated port which does not exist.");
                return;
            }
            pw.println("Disconnecting simulated port: portId=" + portId);
            portInfo.mCurrentMode = 0;
            portInfo.mCanChangeMode = false;
            portInfo.mCurrentPowerRole = 0;
            portInfo.mCanChangePowerRole = false;
            portInfo.mCurrentDataRole = 0;
            portInfo.mCanChangeDataRole = false;
            updatePortsLocked(pw);
        }
    }

    public void removeSimulatedPort(String portId, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            int index = this.mSimulatedPorts.indexOfKey(portId);
            if (index < 0) {
                pw.println("Cannot remove simulated port which does not exist.");
                return;
            }
            pw.println("Disconnecting simulated port: portId=" + portId);
            this.mSimulatedPorts.removeAt(index);
            updatePortsLocked(pw);
        }
    }

    public void resetSimulation(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Removing all simulated ports and ending simulation.");
            if (!this.mSimulatedPorts.isEmpty()) {
                this.mSimulatedPorts.clear();
                updatePortsLocked(pw);
            }
        }
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.print("USB Port State:");
            if (!this.mSimulatedPorts.isEmpty()) {
                pw.print(" (simulation active; end with 'dumpsys usb reset')");
            }
            pw.println();
            if (this.mPorts.isEmpty()) {
                pw.println("  <no ports>");
            } else {
                for (PortInfo portInfo : this.mPorts.values()) {
                    pw.println("  " + portInfo.mUsbPort.getId() + ": " + portInfo);
                }
            }
        }
    }

    private void updatePortsLocked(IndentingPrintWriter pw) {
        File[] portDirs;
        int i = this.mPorts.size();
        while (true) {
            int i2 = i;
            i = i2 - 1;
            if (i2 <= 0) {
                break;
            } else {
                this.mPorts.valueAt(i).mDisposition = 3;
            }
        }
        if (!this.mSimulatedPorts.isEmpty()) {
            int count = this.mSimulatedPorts.size();
            for (int i3 = 0; i3 < count; i3++) {
                SimulatedPortInfo portInfo = this.mSimulatedPorts.valueAt(i3);
                addOrUpdatePortLocked(portInfo.mPortId, portInfo.mSupportedModes, portInfo.mCurrentMode, portInfo.mCanChangeMode, portInfo.mCurrentPowerRole, portInfo.mCanChangePowerRole, portInfo.mCurrentDataRole, portInfo.mCanChangeDataRole, pw);
            }
        } else if (this.mHaveKernelSupport && (portDirs = new File(SYSFS_CLASS).listFiles()) != null) {
            int i4 = 0;
            int length = portDirs.length;
            while (true) {
                int i5 = i4;
                if (i5 >= length) {
                    break;
                }
                File portDir = portDirs[i5];
                if (portDir.isDirectory()) {
                    String portId = portDir.getName();
                    int supportedModes = readSupportedModes(portDir);
                    int currentMode = readCurrentMode(portDir);
                    boolean canChangeMode = canChangeMode(portDir);
                    int currentPowerRole = readCurrentPowerRole(portDir);
                    boolean canChangePowerRole = canChangePowerRole(portDir);
                    int currentDataRole = readCurrentDataRole(portDir);
                    boolean canChangeDataRole = canChangeDataRole(portDir);
                    addOrUpdatePortLocked(portId, supportedModes, currentMode, canChangeMode, currentPowerRole, canChangePowerRole, currentDataRole, canChangeDataRole, pw);
                }
                i4 = i5 + 1;
            }
        }
        int i6 = this.mPorts.size();
        while (true) {
            int i7 = i6;
            i6 = i7 - 1;
            if (i7 <= 0) {
                return;
            }
            PortInfo portInfo2 = this.mPorts.valueAt(i6);
            switch (portInfo2.mDisposition) {
                case 0:
                    handlePortAddedLocked(portInfo2, pw);
                    portInfo2.mDisposition = 2;
                    break;
                case 1:
                    handlePortChangedLocked(portInfo2, pw);
                    portInfo2.mDisposition = 2;
                    break;
                case 3:
                    this.mPorts.removeAt(i6);
                    portInfo2.mUsbPortStatus = null;
                    handlePortRemovedLocked(portInfo2, pw);
                    break;
            }
        }
    }

    private void addOrUpdatePortLocked(String portId, int supportedModes, int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, IndentingPrintWriter pw) {
        if (supportedModes != 3) {
            canChangeMode = false;
            if (currentMode != 0 && currentMode != supportedModes) {
                logAndPrint(5, pw, "Ignoring inconsistent current mode from USB port driver: supportedModes=" + UsbPort.modeToString(supportedModes) + ", currentMode=" + UsbPort.modeToString(currentMode));
                currentMode = 0;
            }
        }
        int supportedRoleCombinations = UsbPort.combineRolesAsBit(currentPowerRole, currentDataRole);
        if (currentMode != 0 && currentPowerRole != 0 && currentDataRole != 0) {
            if (canChangePowerRole && canChangeDataRole) {
                supportedRoleCombinations |= COMBO_SOURCE_HOST | COMBO_SOURCE_DEVICE | COMBO_SINK_HOST | COMBO_SINK_DEVICE;
            } else if (canChangePowerRole) {
                supportedRoleCombinations = supportedRoleCombinations | UsbPort.combineRolesAsBit(1, currentDataRole) | UsbPort.combineRolesAsBit(2, currentDataRole);
            } else if (canChangeDataRole) {
                supportedRoleCombinations = supportedRoleCombinations | UsbPort.combineRolesAsBit(currentPowerRole, 1) | UsbPort.combineRolesAsBit(currentPowerRole, 2);
            } else if (canChangeMode) {
                supportedRoleCombinations |= COMBO_SOURCE_HOST | COMBO_SINK_DEVICE;
            }
        }
        PortInfo portInfo = this.mPorts.get(portId);
        if (portInfo == null) {
            PortInfo portInfo2 = new PortInfo(portId, supportedModes);
            portInfo2.setStatus(currentMode, canChangeMode, currentPowerRole, canChangePowerRole, currentDataRole, canChangeDataRole, supportedRoleCombinations);
            this.mPorts.put(portId, portInfo2);
        } else {
            if (supportedModes != portInfo.mUsbPort.getSupportedModes()) {
                logAndPrint(5, pw, "Ignoring inconsistent list of supported modes from USB port driver (should be immutable): previous=" + UsbPort.modeToString(portInfo.mUsbPort.getSupportedModes()) + ", current=" + UsbPort.modeToString(supportedModes));
            }
            if (portInfo.setStatus(currentMode, canChangeMode, currentPowerRole, canChangePowerRole, currentDataRole, canChangeDataRole, supportedRoleCombinations)) {
                portInfo.mDisposition = 1;
            } else {
                portInfo.mDisposition = 2;
            }
        }
    }

    private void handlePortAddedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        logAndPrint(4, pw, "USB port added: " + portInfo);
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void handlePortChangedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        logAndPrint(4, pw, "USB port changed: " + portInfo);
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void handlePortRemovedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        logAndPrint(4, pw, "USB port removed: " + portInfo);
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void sendPortChangedBroadcastLocked(PortInfo portInfo) {
        final Intent intent = new Intent("android.hardware.usb.action.USB_PORT_CHANGED");
        intent.addFlags(268435456);
        intent.putExtra("port", (Parcelable) portInfo.mUsbPort);
        intent.putExtra("portStatus", (Parcelable) portInfo.mUsbPortStatus);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                UsbPortManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        });
    }

    private void scheduleUpdatePorts() {
        if (this.mHandler.hasMessages(1)) {
            return;
        }
        this.mHandler.sendEmptyMessage(1);
    }

    private static int readSupportedModes(File portDir) {
        int modes = 0;
        String contents = readFile(portDir, SYSFS_PORT_SUPPORTED_MODES);
        if (contents == null) {
            return 0;
        }
        if (contents.contains(PORT_MODE_DFP)) {
            modes = 1;
        }
        if (contents.contains(PORT_MODE_UFP)) {
            return modes | 2;
        }
        return modes;
    }

    private static int readCurrentMode(File portDir) {
        String contents = readFile(portDir, SYSFS_PORT_MODE);
        if (contents != null) {
            if (contents.equals(PORT_MODE_DFP)) {
                return 1;
            }
            if (contents.equals(PORT_MODE_UFP)) {
                return 2;
            }
            return 0;
        }
        return 0;
    }

    private static int readCurrentPowerRole(File portDir) {
        String contents = readFile(portDir, SYSFS_PORT_POWER_ROLE);
        if (contents != null) {
            if (contents.equals(PORT_POWER_ROLE_SOURCE)) {
                return 1;
            }
            if (contents.equals(PORT_POWER_ROLE_SINK)) {
                return 2;
            }
            return 0;
        }
        return 0;
    }

    private static int readCurrentDataRole(File portDir) {
        String contents = readFile(portDir, SYSFS_PORT_DATA_ROLE);
        if (contents != null) {
            if (contents.equals(PORT_DATA_ROLE_HOST)) {
                return 1;
            }
            if (contents.equals(PORT_DATA_ROLE_DEVICE)) {
                return 2;
            }
            return 0;
        }
        return 0;
    }

    private static boolean fileIsRootWritable(String path) {
        try {
            return (Os.stat(path).st_mode & OsConstants.S_IWUSR) != 0;
        } catch (ErrnoException e) {
            return false;
        }
    }

    private static boolean canChangeMode(File portDir) {
        return fileIsRootWritable(new File(portDir, SYSFS_PORT_MODE).getPath());
    }

    private static boolean canChangePowerRole(File portDir) {
        return fileIsRootWritable(new File(portDir, SYSFS_PORT_POWER_ROLE).getPath());
    }

    private static boolean canChangeDataRole(File portDir) {
        return fileIsRootWritable(new File(portDir, SYSFS_PORT_DATA_ROLE).getPath());
    }

    private static String readFile(File dir, String filename) {
        File file = new File(dir, filename);
        try {
            return IoUtils.readFileAsString(file.getAbsolutePath()).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean waitForState(String property, String state) {
        String value = null;
        for (int i = 0; i < 100; i++) {
            value = SystemProperties.get(property);
            if (state.equals(value)) {
                return true;
            }
            SystemClock.sleep(50L);
        }
        Slog.e(TAG, "waitForState(" + state + ") for " + property + " FAILED: got " + value);
        return false;
    }

    private static String propertyFromFilename(String filename) {
        return USB_TYPEC_PROP_PREFIX + filename;
    }

    private static boolean writeFile(File dir, String filename, String contents) {
        SystemProperties.set(propertyFromFilename(filename), contents);
        return waitForState(USB_TYPEC_STATE, contents);
    }

    private static void logAndPrint(int priority, IndentingPrintWriter pw, String msg) {
        Slog.println(priority, TAG, msg);
        if (pw == null) {
            return;
        }
        pw.println(msg);
    }

    private static final class PortInfo {
        public static final int DISPOSITION_ADDED = 0;
        public static final int DISPOSITION_CHANGED = 1;
        public static final int DISPOSITION_READY = 2;
        public static final int DISPOSITION_REMOVED = 3;
        public boolean mCanChangeDataRole;
        public boolean mCanChangeMode;
        public boolean mCanChangePowerRole;
        public int mDisposition;
        public final UsbPort mUsbPort;
        public UsbPortStatus mUsbPortStatus;

        public PortInfo(String portId, int supportedModes) {
            this.mUsbPort = new UsbPort(portId, supportedModes);
        }

        public boolean setStatus(int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, int supportedRoleCombinations) {
            this.mCanChangeMode = canChangeMode;
            this.mCanChangePowerRole = canChangePowerRole;
            this.mCanChangeDataRole = canChangeDataRole;
            if (this.mUsbPortStatus == null || this.mUsbPortStatus.getCurrentMode() != currentMode || this.mUsbPortStatus.getCurrentPowerRole() != currentPowerRole || this.mUsbPortStatus.getCurrentDataRole() != currentDataRole || this.mUsbPortStatus.getSupportedRoleCombinations() != supportedRoleCombinations) {
                this.mUsbPortStatus = new UsbPortStatus(currentMode, currentPowerRole, currentDataRole, supportedRoleCombinations);
                return true;
            }
            return false;
        }

        public String toString() {
            return "port=" + this.mUsbPort + ", status=" + this.mUsbPortStatus + ", canChangeMode=" + this.mCanChangeMode + ", canChangePowerRole=" + this.mCanChangePowerRole + ", canChangeDataRole=" + this.mCanChangeDataRole;
        }
    }

    private static final class SimulatedPortInfo {
        public boolean mCanChangeDataRole;
        public boolean mCanChangeMode;
        public boolean mCanChangePowerRole;
        public int mCurrentDataRole;
        public int mCurrentMode;
        public int mCurrentPowerRole;
        public final String mPortId;
        public final int mSupportedModes;

        public SimulatedPortInfo(String portId, int supportedModes) {
            this.mPortId = portId;
            this.mSupportedModes = supportedModes;
        }
    }
}
