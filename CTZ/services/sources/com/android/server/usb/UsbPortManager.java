package com.android.server.usb;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.hardware.usb.V1_0.IUsb;
import android.hardware.usb.V1_0.PortRole;
import android.hardware.usb.V1_0.PortStatus;
import android.hardware.usb.V1_1.IUsbCallback;
import android.hardware.usb.V1_1.PortStatus_1_1;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Bundle;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.usb.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.FgThread;
import com.android.server.backup.BackupManagerConstants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class UsbPortManager {
    private static final int MSG_UPDATE_PORTS = 1;
    private static final String PORT_INFO = "port_info";
    private static final String TAG = "UsbPortManager";
    private static final int USB_HAL_DEATH_COOKIE = 1000;
    private final Context mContext;
    private boolean mSystemReady;
    private static final int COMBO_SOURCE_HOST = UsbPort.combineRolesAsBit(1, 1);
    private static final int COMBO_SOURCE_DEVICE = UsbPort.combineRolesAsBit(1, 2);
    private static final int COMBO_SINK_HOST = UsbPort.combineRolesAsBit(2, 1);
    private static final int COMBO_SINK_DEVICE = UsbPort.combineRolesAsBit(2, 2);

    @GuardedBy("mLock")
    private IUsb mProxy = null;
    private HALCallback mHALCallback = new HALCallback(null, this);
    private final Object mLock = new Object();
    private final ArrayMap<String, PortInfo> mPorts = new ArrayMap<>();
    private final ArrayMap<String, RawPortInfo> mSimulatedPorts = new ArrayMap<>();
    private final Handler mHandler = new Handler(FgThread.get().getLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                ArrayList parcelableArrayList = message.getData().getParcelableArrayList(UsbPortManager.PORT_INFO);
                synchronized (UsbPortManager.this.mLock) {
                    UsbPortManager.this.updatePortsLocked(null, parcelableArrayList);
                }
            }
        }
    };

    public UsbPortManager(Context context) {
        this.mContext = context;
        try {
            if (!IServiceManager.getService().registerForNotifications(IUsb.kInterfaceName, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new ServiceNotification())) {
                logAndPrint(6, null, "Failed to register service start notification");
            }
            connectToProxy(null);
        } catch (RemoteException e) {
            logAndPrintException(null, "Failed to register service start notification", e);
        }
    }

    public void systemReady() {
        this.mSystemReady = true;
        if (this.mProxy != null) {
            try {
                this.mProxy.queryPortStatus();
            } catch (RemoteException e) {
                logAndPrintException(null, "ServiceStart: Failed to query port status", e);
            }
        }
    }

    public UsbPort[] getPorts() {
        UsbPort[] usbPortArr;
        synchronized (this.mLock) {
            int size = this.mPorts.size();
            usbPortArr = new UsbPort[size];
            for (int i = 0; i < size; i++) {
                usbPortArr[i] = this.mPorts.valueAt(i).mUsbPort;
            }
        }
        return usbPortArr;
    }

    public UsbPortStatus getPortStatus(String str) {
        UsbPortStatus usbPortStatus;
        synchronized (this.mLock) {
            PortInfo portInfo = this.mPorts.get(str);
            usbPortStatus = portInfo != null ? portInfo.mUsbPortStatus : null;
        }
        return usbPortStatus;
    }

    public void setPortRoles(String str, int i, int i2, IndentingPrintWriter indentingPrintWriter) {
        int i3;
        synchronized (this.mLock) {
            PortInfo portInfo = this.mPorts.get(str);
            if (portInfo == null) {
                if (indentingPrintWriter != null) {
                    indentingPrintWriter.println("No such USB port: " + str);
                }
                return;
            }
            if (!portInfo.mUsbPortStatus.isRoleCombinationSupported(i, i2)) {
                logAndPrint(6, indentingPrintWriter, "Attempted to set USB port into unsupported role combination: portId=" + str + ", newPowerRole=" + UsbPort.powerRoleToString(i) + ", newDataRole=" + UsbPort.dataRoleToString(i2));
                return;
            }
            int currentDataRole = portInfo.mUsbPortStatus.getCurrentDataRole();
            int currentPowerRole = portInfo.mUsbPortStatus.getCurrentPowerRole();
            if (currentDataRole == i2 && currentPowerRole == i) {
                if (indentingPrintWriter != null) {
                    indentingPrintWriter.println("No change.");
                }
                return;
            }
            boolean z = portInfo.mCanChangeMode;
            boolean z2 = portInfo.mCanChangePowerRole;
            boolean z3 = portInfo.mCanChangeDataRole;
            int currentMode = portInfo.mUsbPortStatus.getCurrentMode();
            if ((z2 || currentPowerRole == i) && (z3 || currentDataRole == i2)) {
                i3 = currentMode;
            } else if (z && i == 1 && i2 == 1) {
                i3 = 2;
            } else {
                if (!z || i != 2 || i2 != 2) {
                    logAndPrint(6, indentingPrintWriter, "Found mismatch in supported USB role combinations while attempting to change role: " + portInfo + ", newPowerRole=" + UsbPort.powerRoleToString(i) + ", newDataRole=" + UsbPort.dataRoleToString(i2));
                    return;
                }
                i3 = 1;
            }
            logAndPrint(4, indentingPrintWriter, "Setting USB port mode and role: portId=" + str + ", currentMode=" + UsbPort.modeToString(currentMode) + ", currentPowerRole=" + UsbPort.powerRoleToString(currentPowerRole) + ", currentDataRole=" + UsbPort.dataRoleToString(currentDataRole) + ", newMode=" + UsbPort.modeToString(i3) + ", newPowerRole=" + UsbPort.powerRoleToString(i) + ", newDataRole=" + UsbPort.dataRoleToString(i2));
            RawPortInfo rawPortInfo = this.mSimulatedPorts.get(str);
            if (rawPortInfo != null) {
                rawPortInfo.currentMode = i3;
                rawPortInfo.currentPowerRole = i;
                rawPortInfo.currentDataRole = i2;
                updatePortsLocked(indentingPrintWriter, null);
            } else if (this.mProxy != null) {
                if (currentMode != i3) {
                    logAndPrint(6, indentingPrintWriter, "Trying to set the USB port mode: portId=" + str + ", newMode=" + UsbPort.modeToString(i3));
                    PortRole portRole = new PortRole();
                    portRole.type = 2;
                    portRole.role = i3;
                    try {
                        this.mProxy.switchRole(str, portRole);
                    } catch (RemoteException e) {
                        logAndPrintException(indentingPrintWriter, "Failed to set the USB port mode: portId=" + str + ", newMode=" + UsbPort.modeToString(portRole.role), e);
                    }
                } else {
                    if (currentPowerRole != i) {
                        PortRole portRole2 = new PortRole();
                        portRole2.type = 1;
                        portRole2.role = i;
                        try {
                            this.mProxy.switchRole(str, portRole2);
                        } catch (RemoteException e2) {
                            logAndPrintException(indentingPrintWriter, "Failed to set the USB port power role: portId=" + str + ", newPowerRole=" + UsbPort.powerRoleToString(portRole2.role), e2);
                            return;
                        }
                    }
                    if (currentDataRole != i2) {
                        PortRole portRole3 = new PortRole();
                        portRole3.type = 0;
                        portRole3.role = i2;
                        try {
                            this.mProxy.switchRole(str, portRole3);
                        } catch (RemoteException e3) {
                            logAndPrintException(indentingPrintWriter, "Failed to set the USB port data role: portId=" + str + ", newDataRole=" + UsbPort.dataRoleToString(portRole3.role), e3);
                        }
                    }
                }
            }
        }
    }

    public void addSimulatedPort(String str, int i, IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            if (this.mSimulatedPorts.containsKey(str)) {
                indentingPrintWriter.println("Port with same name already exists.  Please remove it first.");
                return;
            }
            indentingPrintWriter.println("Adding simulated port: portId=" + str + ", supportedModes=" + UsbPort.modeToString(i));
            this.mSimulatedPorts.put(str, new RawPortInfo(str, i));
            updatePortsLocked(indentingPrintWriter, null);
        }
    }

    public void connectSimulatedPort(String str, int i, boolean z, int i2, boolean z2, int i3, boolean z3, IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            RawPortInfo rawPortInfo = this.mSimulatedPorts.get(str);
            if (rawPortInfo == null) {
                indentingPrintWriter.println("Cannot connect simulated port which does not exist.");
                return;
            }
            if (i != 0 && i2 != 0 && i3 != 0) {
                if ((rawPortInfo.supportedModes & i) == 0) {
                    indentingPrintWriter.println("Simulated port does not support mode: " + UsbPort.modeToString(i));
                    return;
                }
                indentingPrintWriter.println("Connecting simulated port: portId=" + str + ", mode=" + UsbPort.modeToString(i) + ", canChangeMode=" + z + ", powerRole=" + UsbPort.powerRoleToString(i2) + ", canChangePowerRole=" + z2 + ", dataRole=" + UsbPort.dataRoleToString(i3) + ", canChangeDataRole=" + z3);
                rawPortInfo.currentMode = i;
                rawPortInfo.canChangeMode = z;
                rawPortInfo.currentPowerRole = i2;
                rawPortInfo.canChangePowerRole = z2;
                rawPortInfo.currentDataRole = i3;
                rawPortInfo.canChangeDataRole = z3;
                updatePortsLocked(indentingPrintWriter, null);
                return;
            }
            indentingPrintWriter.println("Cannot connect simulated port in null mode, power role, or data role.");
        }
    }

    public void disconnectSimulatedPort(String str, IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            RawPortInfo rawPortInfo = this.mSimulatedPorts.get(str);
            if (rawPortInfo == null) {
                indentingPrintWriter.println("Cannot disconnect simulated port which does not exist.");
                return;
            }
            indentingPrintWriter.println("Disconnecting simulated port: portId=" + str);
            rawPortInfo.currentMode = 0;
            rawPortInfo.canChangeMode = false;
            rawPortInfo.currentPowerRole = 0;
            rawPortInfo.canChangePowerRole = false;
            rawPortInfo.currentDataRole = 0;
            rawPortInfo.canChangeDataRole = false;
            updatePortsLocked(indentingPrintWriter, null);
        }
    }

    public void removeSimulatedPort(String str, IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            int iIndexOfKey = this.mSimulatedPorts.indexOfKey(str);
            if (iIndexOfKey < 0) {
                indentingPrintWriter.println("Cannot remove simulated port which does not exist.");
                return;
            }
            indentingPrintWriter.println("Disconnecting simulated port: portId=" + str);
            this.mSimulatedPorts.removeAt(iIndexOfKey);
            updatePortsLocked(indentingPrintWriter, null);
        }
    }

    public void resetSimulation(IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            indentingPrintWriter.println("Removing all simulated ports and ending simulation.");
            if (!this.mSimulatedPorts.isEmpty()) {
                this.mSimulatedPorts.clear();
                updatePortsLocked(indentingPrintWriter, null);
            }
        }
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        synchronized (this.mLock) {
            dualDumpOutputStream.write("is_simulation_active", 1133871366145L, !this.mSimulatedPorts.isEmpty());
            Iterator<PortInfo> it = this.mPorts.values().iterator();
            while (it.hasNext()) {
                it.next().dump(dualDumpOutputStream, "usb_ports", 2246267895810L);
            }
        }
        dualDumpOutputStream.end(jStart);
    }

    private static class HALCallback extends IUsbCallback.Stub {
        public UsbPortManager portManager;
        public IndentingPrintWriter pw;

        HALCallback(IndentingPrintWriter indentingPrintWriter, UsbPortManager usbPortManager) {
            this.pw = indentingPrintWriter;
            this.portManager = usbPortManager;
        }

        @Override
        public void notifyPortStatusChange(ArrayList<PortStatus> arrayList, int i) {
            if (!this.portManager.mSystemReady) {
                return;
            }
            if (i != 0) {
                UsbPortManager.logAndPrint(6, this.pw, "port status enquiry failed");
                return;
            }
            ArrayList<? extends Parcelable> arrayList2 = new ArrayList<>();
            for (PortStatus portStatus : arrayList) {
                arrayList2.add(new RawPortInfo(portStatus.portName, portStatus.supportedModes, portStatus.currentMode, portStatus.canChangeMode, portStatus.currentPowerRole, portStatus.canChangePowerRole, portStatus.currentDataRole, portStatus.canChangeDataRole));
                UsbPortManager.logAndPrint(4, this.pw, "ClientCallback: " + portStatus.portName);
            }
            Message messageObtainMessage = this.portManager.mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(UsbPortManager.PORT_INFO, arrayList2);
            messageObtainMessage.what = 1;
            messageObtainMessage.setData(bundle);
            this.portManager.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void notifyPortStatusChange_1_1(ArrayList<PortStatus_1_1> arrayList, int i) {
            if (!this.portManager.mSystemReady) {
                return;
            }
            if (i != 0) {
                UsbPortManager.logAndPrint(6, this.pw, "port status enquiry failed");
                return;
            }
            ArrayList<? extends Parcelable> arrayList2 = new ArrayList<>();
            for (PortStatus_1_1 portStatus_1_1 : arrayList) {
                arrayList2.add(new RawPortInfo(portStatus_1_1.status.portName, portStatus_1_1.supportedModes, portStatus_1_1.currentMode, portStatus_1_1.status.canChangeMode, portStatus_1_1.status.currentPowerRole, portStatus_1_1.status.canChangePowerRole, portStatus_1_1.status.currentDataRole, portStatus_1_1.status.canChangeDataRole));
                UsbPortManager.logAndPrint(4, this.pw, "ClientCallback: " + portStatus_1_1.status.portName);
            }
            Message messageObtainMessage = this.portManager.mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(UsbPortManager.PORT_INFO, arrayList2);
            messageObtainMessage.what = 1;
            messageObtainMessage.setData(bundle);
            this.portManager.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void notifyRoleSwitchStatus(String str, PortRole portRole, int i) {
            if (i == 0) {
                UsbPortManager.logAndPrint(4, this.pw, str + " role switch successful");
                return;
            }
            UsbPortManager.logAndPrint(6, this.pw, str + " role switch failed");
        }
    }

    final class DeathRecipient implements IHwBinder.DeathRecipient {
        public IndentingPrintWriter pw;

        DeathRecipient(IndentingPrintWriter indentingPrintWriter) {
            this.pw = indentingPrintWriter;
        }

        @Override
        public void serviceDied(long j) {
            if (j == 1000) {
                UsbPortManager.logAndPrint(6, this.pw, "Usb hal service died cookie: " + j);
                synchronized (UsbPortManager.this.mLock) {
                    UsbPortManager.this.mProxy = null;
                }
            }
        }
    }

    final class ServiceNotification extends IServiceNotification.Stub {
        ServiceNotification() {
        }

        public void onRegistration(String str, String str2, boolean z) {
            UsbPortManager.logAndPrint(4, null, "Usb hal service started " + str + " " + str2);
            UsbPortManager.this.connectToProxy(null);
        }
    }

    private void connectToProxy(IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            if (this.mProxy != null) {
                return;
            }
            try {
                this.mProxy = IUsb.getService();
                this.mProxy.linkToDeath(new DeathRecipient(indentingPrintWriter), 1000L);
                this.mProxy.setCallback(this.mHALCallback);
                this.mProxy.queryPortStatus();
            } catch (RemoteException e) {
                logAndPrintException(indentingPrintWriter, "connectToProxy: usb hal service not responding", e);
            } catch (NoSuchElementException e2) {
                logAndPrintException(indentingPrintWriter, "connectToProxy: usb hal service not found. Did the service fail to start?", e2);
            }
        }
    }

    private void updatePortsLocked(IndentingPrintWriter indentingPrintWriter, ArrayList<RawPortInfo> arrayList) {
        int size = this.mPorts.size();
        while (true) {
            int i = size - 1;
            if (size <= 0) {
                break;
            }
            this.mPorts.valueAt(i).mDisposition = 3;
            size = i;
        }
        if (!this.mSimulatedPorts.isEmpty()) {
            int size2 = this.mSimulatedPorts.size();
            for (int i2 = 0; i2 < size2; i2++) {
                RawPortInfo rawPortInfoValueAt = this.mSimulatedPorts.valueAt(i2);
                addOrUpdatePortLocked(rawPortInfoValueAt.portId, rawPortInfoValueAt.supportedModes, rawPortInfoValueAt.currentMode, rawPortInfoValueAt.canChangeMode, rawPortInfoValueAt.currentPowerRole, rawPortInfoValueAt.canChangePowerRole, rawPortInfoValueAt.currentDataRole, rawPortInfoValueAt.canChangeDataRole, indentingPrintWriter);
            }
        } else {
            for (RawPortInfo rawPortInfo : arrayList) {
                addOrUpdatePortLocked(rawPortInfo.portId, rawPortInfo.supportedModes, rawPortInfo.currentMode, rawPortInfo.canChangeMode, rawPortInfo.currentPowerRole, rawPortInfo.canChangePowerRole, rawPortInfo.currentDataRole, rawPortInfo.canChangeDataRole, indentingPrintWriter);
            }
        }
        int size3 = this.mPorts.size();
        while (true) {
            int i3 = size3 - 1;
            if (size3 > 0) {
                PortInfo portInfoValueAt = this.mPorts.valueAt(i3);
                int i4 = portInfoValueAt.mDisposition;
                if (i4 != 3) {
                    switch (i4) {
                        case 0:
                            handlePortAddedLocked(portInfoValueAt, indentingPrintWriter);
                            portInfoValueAt.mDisposition = 2;
                            break;
                        case 1:
                            handlePortChangedLocked(portInfoValueAt, indentingPrintWriter);
                            portInfoValueAt.mDisposition = 2;
                            break;
                    }
                } else {
                    this.mPorts.removeAt(i3);
                    portInfoValueAt.mUsbPortStatus = null;
                    handlePortRemovedLocked(portInfoValueAt, indentingPrintWriter);
                }
                size3 = i3;
            } else {
                return;
            }
        }
    }

    private void addOrUpdatePortLocked(String str, int i, int i2, boolean z, int i3, boolean z2, int i4, boolean z3, IndentingPrintWriter indentingPrintWriter) {
        int i5 = i2;
        boolean z4 = false;
        if ((i & 3) == 3) {
            z4 = z;
        } else if (i5 != 0 && i5 != i) {
            logAndPrint(5, indentingPrintWriter, "Ignoring inconsistent current mode from USB port driver: supportedModes=" + UsbPort.modeToString(i) + ", currentMode=" + UsbPort.modeToString(i2));
            i5 = 0;
        }
        int iCombineRolesAsBit = UsbPort.combineRolesAsBit(i3, i4);
        if (i5 != 0 && i3 != 0 && i4 != 0) {
            if (z2 && z3) {
                iCombineRolesAsBit |= COMBO_SOURCE_HOST | COMBO_SOURCE_DEVICE | COMBO_SINK_HOST | COMBO_SINK_DEVICE;
            } else if (z2) {
                iCombineRolesAsBit = iCombineRolesAsBit | UsbPort.combineRolesAsBit(1, i4) | UsbPort.combineRolesAsBit(2, i4);
            } else if (z3) {
                iCombineRolesAsBit = iCombineRolesAsBit | UsbPort.combineRolesAsBit(i3, 1) | UsbPort.combineRolesAsBit(i3, 2);
            } else if (z4) {
                iCombineRolesAsBit |= COMBO_SOURCE_HOST | COMBO_SINK_DEVICE;
            }
        }
        PortInfo portInfo = this.mPorts.get(str);
        if (portInfo == null) {
            PortInfo portInfo2 = new PortInfo(str, i);
            portInfo2.setStatus(i5, z4, i3, z2, i4, z3, iCombineRolesAsBit);
            this.mPorts.put(str, portInfo2);
            return;
        }
        if (i != portInfo.mUsbPort.getSupportedModes()) {
            logAndPrint(5, indentingPrintWriter, "Ignoring inconsistent list of supported modes from USB port driver (should be immutable): previous=" + UsbPort.modeToString(portInfo.mUsbPort.getSupportedModes()) + ", current=" + UsbPort.modeToString(i));
        }
        if (portInfo.setStatus(i5, z4, i3, z2, i4, z3, iCombineRolesAsBit)) {
            portInfo.mDisposition = 1;
        } else {
            portInfo.mDisposition = 2;
        }
    }

    private void handlePortAddedLocked(PortInfo portInfo, IndentingPrintWriter indentingPrintWriter) {
        logAndPrint(4, indentingPrintWriter, "USB port added: " + portInfo);
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void handlePortChangedLocked(PortInfo portInfo, IndentingPrintWriter indentingPrintWriter) {
        logAndPrint(4, indentingPrintWriter, "USB port changed: " + portInfo);
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void handlePortRemovedLocked(PortInfo portInfo, IndentingPrintWriter indentingPrintWriter) {
        logAndPrint(4, indentingPrintWriter, "USB port removed: " + portInfo);
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void sendPortChangedBroadcastLocked(PortInfo portInfo) {
        final Intent intent = new Intent("android.hardware.usb.action.USB_PORT_CHANGED");
        intent.addFlags(285212672);
        intent.putExtra("port", (Parcelable) portInfo.mUsbPort);
        intent.putExtra("portStatus", (Parcelable) portInfo.mUsbPortStatus);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        });
    }

    private static void logAndPrint(int i, IndentingPrintWriter indentingPrintWriter, String str) {
        Slog.println(i, TAG, str);
        if (indentingPrintWriter != null) {
            indentingPrintWriter.println(str);
        }
    }

    private static void logAndPrintException(IndentingPrintWriter indentingPrintWriter, String str, Exception exc) {
        Slog.e(TAG, str, exc);
        if (indentingPrintWriter != null) {
            indentingPrintWriter.println(str + exc);
        }
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

        public PortInfo(String str, int i) {
            this.mUsbPort = new UsbPort(str, i);
        }

        public boolean setStatus(int i, boolean z, int i2, boolean z2, int i3, boolean z3, int i4) {
            this.mCanChangeMode = z;
            this.mCanChangePowerRole = z2;
            this.mCanChangeDataRole = z3;
            if (this.mUsbPortStatus == null || this.mUsbPortStatus.getCurrentMode() != i || this.mUsbPortStatus.getCurrentPowerRole() != i2 || this.mUsbPortStatus.getCurrentDataRole() != i3 || this.mUsbPortStatus.getSupportedRoleCombinations() != i4) {
                this.mUsbPortStatus = new UsbPortStatus(i, i2, i3, i4);
                return true;
            }
            return false;
        }

        void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
            long jStart = dualDumpOutputStream.start(str, j);
            DumpUtils.writePort(dualDumpOutputStream, "port", 1146756268033L, this.mUsbPort);
            DumpUtils.writePortStatus(dualDumpOutputStream, "status", 1146756268034L, this.mUsbPortStatus);
            dualDumpOutputStream.write("can_change_mode", 1133871366147L, this.mCanChangeMode);
            dualDumpOutputStream.write("can_change_power_role", 1133871366148L, this.mCanChangePowerRole);
            dualDumpOutputStream.write("can_change_data_role", 1133871366149L, this.mCanChangeDataRole);
            dualDumpOutputStream.end(jStart);
        }

        public String toString() {
            return "port=" + this.mUsbPort + ", status=" + this.mUsbPortStatus + ", canChangeMode=" + this.mCanChangeMode + ", canChangePowerRole=" + this.mCanChangePowerRole + ", canChangeDataRole=" + this.mCanChangeDataRole;
        }
    }

    private static final class RawPortInfo implements Parcelable {
        public static final Parcelable.Creator<RawPortInfo> CREATOR = new Parcelable.Creator<RawPortInfo>() {
            @Override
            public RawPortInfo createFromParcel(Parcel parcel) {
                return new RawPortInfo(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readByte() != 0, parcel.readInt(), parcel.readByte() != 0, parcel.readInt(), parcel.readByte() != 0);
            }

            @Override
            public RawPortInfo[] newArray(int i) {
                return new RawPortInfo[i];
            }
        };
        public boolean canChangeDataRole;
        public boolean canChangeMode;
        public boolean canChangePowerRole;
        public int currentDataRole;
        public int currentMode;
        public int currentPowerRole;
        public final String portId;
        public final int supportedModes;

        RawPortInfo(String str, int i) {
            this.portId = str;
            this.supportedModes = i;
        }

        RawPortInfo(String str, int i, int i2, boolean z, int i3, boolean z2, int i4, boolean z3) {
            this.portId = str;
            this.supportedModes = i;
            this.currentMode = i2;
            this.canChangeMode = z;
            this.currentPowerRole = i3;
            this.canChangePowerRole = z2;
            this.currentDataRole = i4;
            this.canChangeDataRole = z3;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.portId);
            parcel.writeInt(this.supportedModes);
            parcel.writeInt(this.currentMode);
            parcel.writeByte(this.canChangeMode ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.currentPowerRole);
            parcel.writeByte(this.canChangePowerRole ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.currentDataRole);
            parcel.writeByte(this.canChangeDataRole ? (byte) 1 : (byte) 0);
        }
    }
}
