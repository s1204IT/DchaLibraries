package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.LocalLog;
import android.util.SparseIntArray;
import android.view.Display;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class DeviceStateMonitor extends Handler {
    protected static final boolean DBG = false;
    private static final int EVENT_CHARGING_STATE_CHANGED = 4;
    private static final int EVENT_POWER_SAVE_MODE_CHANGED = 3;
    private static final int EVENT_RIL_CONNECTED = 0;
    private static final int EVENT_SCREEN_STATE_CHANGED = 2;
    private static final int EVENT_TETHERING_STATE_CHANGED = 5;
    private static final int EVENT_UPDATE_MODE_CHANGED = 1;
    private static final int HYSTERESIS_KBPS = 50;
    private boolean mIsCharging;
    private boolean mIsLowDataExpected;
    private boolean mIsPowerSaveOn;
    private boolean mIsScreenOn;
    private boolean mIsTetheringOn;
    private final Phone mPhone;
    protected static final String TAG = DeviceStateMonitor.class.getSimpleName();
    private static final int[] LINK_CAPACITY_DOWNLINK_THRESHOLDS = {500, 1000, 5000, 10000, 20000};
    private static final int[] LINK_CAPACITY_UPLINK_THRESHOLDS = {100, 500, 1000, 5000, 10000};
    private final LocalLog mLocalLog = new LocalLog(100);
    private SparseIntArray mUpdateModes = new SparseIntArray();
    private int mUnsolicitedResponseFilter = -1;
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int i) {
        }

        @Override
        public void onDisplayRemoved(int i) {
        }

        @Override
        public void onDisplayChanged(int i) {
            boolean zIsScreenOn = DeviceStateMonitor.this.isScreenOn();
            Message messageObtainMessage = DeviceStateMonitor.this.obtainMessage(2);
            messageObtainMessage.arg1 = zIsScreenOn ? 1 : 0;
            DeviceStateMonitor.this.sendMessage(messageObtainMessage);
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            Message messageObtainMessage;
            DeviceStateMonitor.this.log("received: " + intent, true);
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            int i = 0;
            if (iHashCode != -1754841973) {
                if (iHashCode != -54942926) {
                    if (iHashCode != 948344062) {
                        b = (iHashCode == 1779291251 && action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) ? (byte) 0 : (byte) -1;
                    } else if (action.equals("android.os.action.CHARGING")) {
                        b = 1;
                    }
                } else if (action.equals("android.os.action.DISCHARGING")) {
                    b = 2;
                }
            } else if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                b = 3;
            }
            switch (b) {
                case 0:
                    messageObtainMessage = DeviceStateMonitor.this.obtainMessage(3);
                    messageObtainMessage.arg1 = DeviceStateMonitor.this.isPowerSaveModeOn() ? 1 : 0;
                    DeviceStateMonitor deviceStateMonitor = DeviceStateMonitor.this;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Power Save mode ");
                    sb.append(messageObtainMessage.arg1 == 1 ? "on" : "off");
                    deviceStateMonitor.log(sb.toString(), true);
                    break;
                case 1:
                    messageObtainMessage = DeviceStateMonitor.this.obtainMessage(4);
                    messageObtainMessage.arg1 = 1;
                    break;
                case 2:
                    messageObtainMessage = DeviceStateMonitor.this.obtainMessage(4);
                    messageObtainMessage.arg1 = 0;
                    break;
                case 3:
                    ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("tetherArray");
                    if (stringArrayListExtra != null && stringArrayListExtra.size() > 0) {
                        i = 1;
                    }
                    DeviceStateMonitor deviceStateMonitor2 = DeviceStateMonitor.this;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Tethering ");
                    sb2.append(i != 0 ? "on" : "off");
                    deviceStateMonitor2.log(sb2.toString(), true);
                    messageObtainMessage = DeviceStateMonitor.this.obtainMessage(5);
                    messageObtainMessage.arg1 = i;
                    break;
                default:
                    DeviceStateMonitor.this.log("Unexpected broadcast intent: " + intent, false);
                    return;
            }
            DeviceStateMonitor.this.sendMessage(messageObtainMessage);
        }
    };

    public DeviceStateMonitor(Phone phone) {
        this.mPhone = phone;
        ((DisplayManager) phone.getContext().getSystemService("display")).registerDisplayListener(this.mDisplayListener, null);
        this.mIsPowerSaveOn = isPowerSaveModeOn();
        this.mIsCharging = isDeviceCharging();
        this.mIsScreenOn = isScreenOn();
        this.mIsTetheringOn = false;
        this.mIsLowDataExpected = false;
        log("DeviceStateMonitor mIsPowerSaveOn=" + this.mIsPowerSaveOn + ",mIsScreenOn=" + this.mIsScreenOn + ",mIsCharging=" + this.mIsCharging, false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.CHARGING");
        intentFilter.addAction("android.os.action.DISCHARGING");
        intentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mPhone);
        this.mPhone.mCi.registerForRilConnected(this, 0, null);
    }

    private boolean isLowDataExpected() {
        return (this.mIsCharging || this.mIsTetheringOn || this.mIsScreenOn) ? false : true;
    }

    private boolean shouldTurnOffSignalStrength() {
        if (!this.mIsCharging && !this.mIsScreenOn && this.mUpdateModes.get(1) != 2) {
            return true;
        }
        return false;
    }

    private boolean shouldTurnOffFullNetworkUpdate() {
        if (this.mIsCharging || this.mIsScreenOn || this.mIsTetheringOn || this.mUpdateModes.get(2) == 2) {
            return false;
        }
        return true;
    }

    private boolean shouldTurnOffDormancyUpdate() {
        if (this.mIsCharging || this.mIsScreenOn || this.mIsTetheringOn || this.mUpdateModes.get(4) == 2) {
            return false;
        }
        return true;
    }

    private boolean shouldTurnOffLinkCapacityEstimate() {
        if (this.mIsCharging || this.mIsScreenOn || this.mIsTetheringOn || this.mUpdateModes.get(8) == 2) {
            return false;
        }
        return true;
    }

    private boolean shouldTurnOffPhysicalChannelConfig() {
        if (this.mIsCharging || this.mIsScreenOn || this.mIsTetheringOn || this.mUpdateModes.get(16) == 2) {
            return false;
        }
        return true;
    }

    public void setIndicationUpdateMode(int i, int i2) {
        sendMessage(obtainMessage(1, i, i2));
    }

    private void onSetIndicationUpdateMode(int i, int i2) {
        if ((i & 1) != 0) {
            this.mUpdateModes.put(1, i2);
        }
        if ((i & 2) != 0) {
            this.mUpdateModes.put(2, i2);
        }
        if ((i & 4) != 0) {
            this.mUpdateModes.put(4, i2);
        }
        if ((i & 8) != 0) {
            this.mUpdateModes.put(8, i2);
        }
        if ((i & 16) != 0) {
            this.mUpdateModes.put(16, i2);
        }
    }

    @Override
    public void handleMessage(Message message) {
        log("handleMessage msg=" + message, false);
        switch (message.what) {
            case 0:
                onRilConnected();
                return;
            case 1:
                onSetIndicationUpdateMode(message.arg1, message.arg2);
                return;
            case 2:
            case 3:
            case 4:
            case 5:
                onUpdateDeviceState(message.what, message.arg1 != 0);
                return;
            default:
                throw new IllegalStateException("Unexpected message arrives. msg = " + message.what);
        }
    }

    private void onUpdateDeviceState(int i, boolean z) {
        switch (i) {
            case 2:
                if (this.mIsScreenOn == z) {
                    return;
                } else {
                    this.mIsScreenOn = z;
                }
                break;
            case 3:
                if (this.mIsPowerSaveOn == z) {
                    return;
                }
                this.mIsPowerSaveOn = z;
                sendDeviceState(0, this.mIsPowerSaveOn);
                break;
            case 4:
                if (this.mIsCharging == z) {
                    return;
                }
                this.mIsCharging = z;
                sendDeviceState(1, this.mIsCharging);
                break;
            case 5:
                if (this.mIsTetheringOn == z) {
                    return;
                } else {
                    this.mIsTetheringOn = z;
                }
                break;
            default:
                return;
        }
        if (this.mIsLowDataExpected != isLowDataExpected()) {
            this.mIsLowDataExpected = !this.mIsLowDataExpected;
            sendDeviceState(2, this.mIsLowDataExpected);
        }
        int i2 = shouldTurnOffSignalStrength() ? 0 : 1;
        if (!shouldTurnOffFullNetworkUpdate()) {
            i2 |= 2;
        }
        if (!shouldTurnOffDormancyUpdate()) {
            i2 |= 4;
        }
        if (!shouldTurnOffLinkCapacityEstimate()) {
            i2 |= 8;
        }
        if (!shouldTurnOffPhysicalChannelConfig()) {
            i2 |= 16;
        }
        setUnsolResponseFilter(i2, false);
    }

    private void onRilConnected() {
        log("RIL connected.", true);
        sendDeviceState(1, this.mIsCharging);
        sendDeviceState(2, this.mIsLowDataExpected);
        sendDeviceState(0, this.mIsPowerSaveOn);
        setUnsolResponseFilter(this.mUnsolicitedResponseFilter, true);
        setSignalStrengthReportingCriteria();
        setLinkCapacityReportingCriteria();
    }

    private String deviceTypeToString(int i) {
        switch (i) {
            case 0:
                return "POWER_SAVE_MODE";
            case 1:
                return "CHARGING_STATE";
            case 2:
                return "LOW_DATA_EXPECTED";
            default:
                return "UNKNOWN";
        }
    }

    private void sendDeviceState(int i, boolean z) {
        log("send type: " + deviceTypeToString(i) + ", state=" + z, true);
        this.mPhone.mCi.sendDeviceState(i, z, null);
    }

    private void setUnsolResponseFilter(int i, boolean z) {
        if (z || i != this.mUnsolicitedResponseFilter) {
            log("old filter: " + this.mUnsolicitedResponseFilter + ", new filter: " + i, true);
            this.mPhone.mCi.setUnsolResponseFilter(i, null);
            this.mUnsolicitedResponseFilter = i;
        }
    }

    private void setSignalStrengthReportingCriteria() {
        this.mPhone.setSignalStrengthReportingCriteria(AccessNetworkThresholds.GERAN, 1);
        this.mPhone.setSignalStrengthReportingCriteria(AccessNetworkThresholds.UTRAN, 2);
        this.mPhone.setSignalStrengthReportingCriteria(AccessNetworkThresholds.EUTRAN, 3);
        this.mPhone.setSignalStrengthReportingCriteria(AccessNetworkThresholds.CDMA2000, 4);
    }

    private void setLinkCapacityReportingCriteria() {
        this.mPhone.setLinkCapacityReportingCriteria(LINK_CAPACITY_DOWNLINK_THRESHOLDS, LINK_CAPACITY_UPLINK_THRESHOLDS, 1);
        this.mPhone.setLinkCapacityReportingCriteria(LINK_CAPACITY_DOWNLINK_THRESHOLDS, LINK_CAPACITY_UPLINK_THRESHOLDS, 2);
        this.mPhone.setLinkCapacityReportingCriteria(LINK_CAPACITY_DOWNLINK_THRESHOLDS, LINK_CAPACITY_UPLINK_THRESHOLDS, 3);
        this.mPhone.setLinkCapacityReportingCriteria(LINK_CAPACITY_DOWNLINK_THRESHOLDS, LINK_CAPACITY_UPLINK_THRESHOLDS, 4);
    }

    private boolean isPowerSaveModeOn() {
        return ((PowerManager) this.mPhone.getContext().getSystemService("power")).isPowerSaveMode();
    }

    private boolean isDeviceCharging() {
        return ((BatteryManager) this.mPhone.getContext().getSystemService("batterymanager")).isCharging();
    }

    private boolean isScreenOn() {
        Display[] displays = ((DisplayManager) this.mPhone.getContext().getSystemService("display")).getDisplays();
        if (displays != null) {
            for (Display display : displays) {
                if (display.getState() == 2) {
                    log("Screen " + Display.typeToString(display.getType()) + " on", true);
                    return true;
                }
            }
            log("Screens all off", true);
            return false;
        }
        log("No displays found", true);
        return false;
    }

    private void log(String str, boolean z) {
        if (z) {
            this.mLocalLog.log(str);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("mIsTetheringOn=" + this.mIsTetheringOn);
        indentingPrintWriter.println("mIsScreenOn=" + this.mIsScreenOn);
        indentingPrintWriter.println("mIsCharging=" + this.mIsCharging);
        indentingPrintWriter.println("mIsPowerSaveOn=" + this.mIsPowerSaveOn);
        indentingPrintWriter.println("mIsLowDataExpected=" + this.mIsLowDataExpected);
        indentingPrintWriter.println("mUnsolicitedResponseFilter=" + this.mUnsolicitedResponseFilter);
        indentingPrintWriter.println("Local logs:");
        indentingPrintWriter.increaseIndent();
        this.mLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.flush();
    }

    private static final class AccessNetworkThresholds {
        public static final int[] GERAN = {-109, -103, -97, -89};
        public static final int[] UTRAN = {-114, -104, -94, -84};
        public static final int[] EUTRAN = {-140, -128, -118, -108, -98, -44};
        public static final int[] CDMA2000 = {-105, -90, -75, -65};

        private AccessNetworkThresholds() {
        }
    }
}
