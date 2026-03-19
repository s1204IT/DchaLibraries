package com.android.server.net;

import android.app.ActivityManager;
import android.net.NetworkPolicyManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.RingBuffer;
import com.android.server.BatteryService;
import com.android.server.am.ProcessList;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

public class NetworkPolicyLogger {
    private static final int EVENT_APP_IDLE_STATE_CHANGED = 8;
    private static final int EVENT_DEVICE_IDLE_MODE_ENABLED = 7;
    private static final int EVENT_FIREWALL_CHAIN_ENABLED = 12;
    private static final int EVENT_METEREDNESS_CHANGED = 4;
    private static final int EVENT_NETWORK_BLOCKED = 1;
    private static final int EVENT_PAROLE_STATE_CHANGED = 9;
    private static final int EVENT_POLICIES_CHANGED = 3;
    private static final int EVENT_RESTRICT_BG_CHANGED = 6;
    private static final int EVENT_TEMP_POWER_SAVE_WL_CHANGED = 10;
    private static final int EVENT_TYPE_GENERIC = 0;
    private static final int EVENT_UID_FIREWALL_RULE_CHANGED = 11;
    private static final int EVENT_UID_STATE_CHANGED = 2;
    private static final int EVENT_UPDATE_METERED_RESTRICTED_PKGS = 13;
    private static final int EVENT_USER_STATE_REMOVED = 5;
    private static final int MAX_LOG_SIZE;
    private static final int MAX_NETWORK_BLOCKED_LOG_SIZE;
    static final int NTWK_ALLOWED_DEFAULT = 6;
    static final int NTWK_ALLOWED_NON_METERED = 1;
    static final int NTWK_ALLOWED_TMP_WHITELIST = 4;
    static final int NTWK_ALLOWED_WHITELIST = 3;
    static final int NTWK_BLOCKED_BG_RESTRICT = 5;
    static final int NTWK_BLOCKED_BLACKLIST = 2;
    static final int NTWK_BLOCKED_POWER = 0;
    static final String TAG = "NetworkPolicy";
    static final boolean LOGD = Log.isLoggable(TAG, 3);
    static final boolean LOGV = Log.isLoggable(TAG, 2);
    private final LogBuffer mNetworkBlockedBuffer = new LogBuffer(MAX_NETWORK_BLOCKED_LOG_SIZE);
    private final LogBuffer mUidStateChangeBuffer = new LogBuffer(MAX_LOG_SIZE);
    private final LogBuffer mEventsBuffer = new LogBuffer(MAX_LOG_SIZE);
    private final Object mLock = new Object();

    static {
        MAX_LOG_SIZE = ActivityManager.isLowRamDeviceStatic() ? 20 : 50;
        MAX_NETWORK_BLOCKED_LOG_SIZE = ActivityManager.isLowRamDeviceStatic() ? 50 : 100;
    }

    void networkBlocked(int i, int i2) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, i + " is " + getBlockedReason(i2));
            }
            this.mNetworkBlockedBuffer.networkBlocked(i, i2);
        }
    }

    void uidStateChanged(int i, int i2, long j) {
        synchronized (this.mLock) {
            if (LOGV) {
                Slog.v(TAG, i + " state changed to " + i2 + " with seq=" + j);
            }
            this.mUidStateChangeBuffer.uidStateChanged(i, i2, j);
        }
    }

    void event(String str) {
        synchronized (this.mLock) {
            if (LOGV) {
                Slog.v(TAG, str);
            }
            this.mEventsBuffer.event(str);
        }
    }

    void uidPolicyChanged(int i, int i2, int i3) {
        synchronized (this.mLock) {
            if (LOGV) {
                Slog.v(TAG, getPolicyChangedLog(i, i2, i3));
            }
            this.mEventsBuffer.uidPolicyChanged(i, i2, i3);
        }
    }

    void meterednessChanged(int i, boolean z) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getMeterednessChangedLog(i, z));
            }
            this.mEventsBuffer.meterednessChanged(i, z);
        }
    }

    void removingUserState(int i) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getUserRemovedLog(i));
            }
            this.mEventsBuffer.userRemoved(i);
        }
    }

    void restrictBackgroundChanged(boolean z, boolean z2) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getRestrictBackgroundChangedLog(z, z2));
            }
            this.mEventsBuffer.restrictBackgroundChanged(z, z2);
        }
    }

    void deviceIdleModeEnabled(boolean z) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getDeviceIdleModeEnabled(z));
            }
            this.mEventsBuffer.deviceIdleModeEnabled(z);
        }
    }

    void appIdleStateChanged(int i, boolean z) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getAppIdleChangedLog(i, z));
            }
            this.mEventsBuffer.appIdleStateChanged(i, z);
        }
    }

    void paroleStateChanged(boolean z) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getParoleStateChanged(z));
            }
            this.mEventsBuffer.paroleStateChanged(z);
        }
    }

    void tempPowerSaveWlChanged(int i, boolean z) {
        synchronized (this.mLock) {
            if (LOGV) {
                Slog.v(TAG, getTempPowerSaveWlChangedLog(i, z));
            }
            this.mEventsBuffer.tempPowerSaveWlChanged(i, z);
        }
    }

    void uidFirewallRuleChanged(int i, int i2, int i3) {
        synchronized (this.mLock) {
            if (LOGV) {
                Slog.v(TAG, getUidFirewallRuleChangedLog(i, i2, i3));
            }
            this.mEventsBuffer.uidFirewallRuleChanged(i, i2, i3);
        }
    }

    void firewallChainEnabled(int i, boolean z) {
        synchronized (this.mLock) {
            if (LOGD) {
                Slog.d(TAG, getFirewallChainEnabledLog(i, z));
            }
            this.mEventsBuffer.firewallChainEnabled(i, z);
        }
    }

    void firewallRulesChanged(int i, int[] iArr, int[] iArr2) {
        synchronized (this.mLock) {
            String str = "Firewall rules changed for " + getFirewallChainName(i) + "; uids=" + Arrays.toString(iArr) + "; rules=" + Arrays.toString(iArr2);
            if (LOGD) {
                Slog.d(TAG, str);
            }
            this.mEventsBuffer.event(str);
        }
    }

    void meteredRestrictedPkgsChanged(Set<Integer> set) {
        synchronized (this.mLock) {
            String str = "Metered restricted uids: " + set;
            if (LOGD) {
                Slog.d(TAG, str);
            }
            this.mEventsBuffer.event(str);
        }
    }

    void dumpLogs(IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            indentingPrintWriter.println();
            indentingPrintWriter.println("mEventLogs (most recent first):");
            indentingPrintWriter.increaseIndent();
            this.mEventsBuffer.reverseDump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("mNetworkBlockedLogs (most recent first):");
            indentingPrintWriter.increaseIndent();
            this.mNetworkBlockedBuffer.reverseDump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("mUidStateChangeLogs (most recent first):");
            indentingPrintWriter.increaseIndent();
            this.mUidStateChangeBuffer.reverseDump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
    }

    private static String getBlockedReason(int i) {
        switch (i) {
            case 0:
                return "blocked by power restrictions";
            case 1:
                return "allowed on unmetered network";
            case 2:
                return "blacklisted on metered network";
            case 3:
                return "whitelisted on metered network";
            case 4:
                return "temporary whitelisted on metered network";
            case 5:
                return "blocked when background is restricted";
            case 6:
                return "allowed by default";
            default:
                return String.valueOf(i);
        }
    }

    private static String getPolicyChangedLog(int i, int i2, int i3) {
        return "Policy for " + i + " changed from " + NetworkPolicyManager.uidPoliciesToString(i2) + " to " + NetworkPolicyManager.uidPoliciesToString(i3);
    }

    private static String getMeterednessChangedLog(int i, boolean z) {
        return "Meteredness of netId=" + i + " changed to " + z;
    }

    private static String getUserRemovedLog(int i) {
        return "Remove state for u" + i;
    }

    private static String getRestrictBackgroundChangedLog(boolean z, boolean z2) {
        return "Changed restrictBackground: " + z + "->" + z2;
    }

    private static String getDeviceIdleModeEnabled(boolean z) {
        return "DeviceIdleMode enabled: " + z;
    }

    private static String getAppIdleChangedLog(int i, boolean z) {
        return "App idle state of uid " + i + ": " + z;
    }

    private static String getParoleStateChanged(boolean z) {
        return "Parole state: " + z;
    }

    private static String getTempPowerSaveWlChangedLog(int i, boolean z) {
        return "temp-power-save whitelist for " + i + " changed to: " + z;
    }

    private static String getUidFirewallRuleChangedLog(int i, int i2, int i3) {
        return String.format("Firewall rule changed: %d-%s-%s", Integer.valueOf(i2), getFirewallChainName(i), getFirewallRuleName(i3));
    }

    private static String getFirewallChainEnabledLog(int i, boolean z) {
        return "Firewall chain " + getFirewallChainName(i) + " state: " + z;
    }

    private static String getFirewallChainName(int i) {
        switch (i) {
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            default:
                return String.valueOf(i);
        }
    }

    private static String getFirewallRuleName(int i) {
        switch (i) {
            case 0:
                return BatteryService.HealthServiceWrapper.INSTANCE_VENDOR;
            case 1:
                return "allow";
            case 2:
                return "deny";
            default:
                return String.valueOf(i);
        }
    }

    private static final class LogBuffer extends RingBuffer<Data> {
        private static final SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS");
        private static final Date sDate = new Date();

        public LogBuffer(int i) {
            super(Data.class, i);
        }

        public void uidStateChanged(int i, int i2, long j) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 2;
            data.ifield1 = i;
            data.ifield2 = i2;
            data.lfield1 = j;
            data.timeStamp = System.currentTimeMillis();
        }

        public void event(String str) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 0;
            data.sfield1 = str;
            data.timeStamp = System.currentTimeMillis();
        }

        public void networkBlocked(int i, int i2) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 1;
            data.ifield1 = i;
            data.ifield2 = i2;
            data.timeStamp = System.currentTimeMillis();
        }

        public void uidPolicyChanged(int i, int i2, int i3) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 3;
            data.ifield1 = i;
            data.ifield2 = i2;
            data.ifield3 = i3;
            data.timeStamp = System.currentTimeMillis();
        }

        public void meterednessChanged(int i, boolean z) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 4;
            data.ifield1 = i;
            data.bfield1 = z;
            data.timeStamp = System.currentTimeMillis();
        }

        public void userRemoved(int i) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 5;
            data.ifield1 = i;
            data.timeStamp = System.currentTimeMillis();
        }

        public void restrictBackgroundChanged(boolean z, boolean z2) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 6;
            data.bfield1 = z;
            data.bfield2 = z2;
            data.timeStamp = System.currentTimeMillis();
        }

        public void deviceIdleModeEnabled(boolean z) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 7;
            data.bfield1 = z;
            data.timeStamp = System.currentTimeMillis();
        }

        public void appIdleStateChanged(int i, boolean z) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 8;
            data.ifield1 = i;
            data.bfield1 = z;
            data.timeStamp = System.currentTimeMillis();
        }

        public void paroleStateChanged(boolean z) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 9;
            data.bfield1 = z;
            data.timeStamp = System.currentTimeMillis();
        }

        public void tempPowerSaveWlChanged(int i, boolean z) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 10;
            data.ifield1 = i;
            data.bfield1 = z;
            data.timeStamp = System.currentTimeMillis();
        }

        public void uidFirewallRuleChanged(int i, int i2, int i3) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 11;
            data.ifield1 = i;
            data.ifield2 = i2;
            data.ifield3 = i3;
            data.timeStamp = System.currentTimeMillis();
        }

        public void firewallChainEnabled(int i, boolean z) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            data.type = 12;
            data.ifield1 = i;
            data.bfield1 = z;
            data.timeStamp = System.currentTimeMillis();
        }

        public void reverseDump(IndentingPrintWriter indentingPrintWriter) {
            Data[] dataArr = (Data[]) toArray();
            for (int length = dataArr.length - 1; length >= 0; length--) {
                if (dataArr[length] == null) {
                    indentingPrintWriter.println("NULL");
                } else {
                    indentingPrintWriter.print(formatDate(dataArr[length].timeStamp));
                    indentingPrintWriter.print(" - ");
                    indentingPrintWriter.println(getContent(dataArr[length]));
                }
            }
        }

        public String getContent(Data data) {
            switch (data.type) {
                case 0:
                    return data.sfield1;
                case 1:
                    return data.ifield1 + "-" + NetworkPolicyLogger.getBlockedReason(data.ifield2);
                case 2:
                    return data.ifield1 + "-" + ProcessList.makeProcStateString(data.ifield2) + "-" + data.lfield1;
                case 3:
                    return NetworkPolicyLogger.getPolicyChangedLog(data.ifield1, data.ifield2, data.ifield3);
                case 4:
                    return NetworkPolicyLogger.getMeterednessChangedLog(data.ifield1, data.bfield1);
                case 5:
                    return NetworkPolicyLogger.getUserRemovedLog(data.ifield1);
                case 6:
                    return NetworkPolicyLogger.getRestrictBackgroundChangedLog(data.bfield1, data.bfield2);
                case 7:
                    return NetworkPolicyLogger.getDeviceIdleModeEnabled(data.bfield1);
                case 8:
                    return NetworkPolicyLogger.getAppIdleChangedLog(data.ifield1, data.bfield1);
                case 9:
                    return NetworkPolicyLogger.getParoleStateChanged(data.bfield1);
                case 10:
                    return NetworkPolicyLogger.getTempPowerSaveWlChangedLog(data.ifield1, data.bfield1);
                case 11:
                    return NetworkPolicyLogger.getUidFirewallRuleChangedLog(data.ifield1, data.ifield2, data.ifield3);
                case 12:
                    return NetworkPolicyLogger.getFirewallChainEnabledLog(data.ifield1, data.bfield1);
                default:
                    return String.valueOf(data.type);
            }
        }

        private String formatDate(long j) {
            sDate.setTime(j);
            return sFormatter.format(sDate);
        }
    }

    public static final class Data {
        boolean bfield1;
        boolean bfield2;
        int ifield1;
        int ifield2;
        int ifield3;
        long lfield1;
        String sfield1;
        long timeStamp;
        int type;

        public void reset() {
            this.sfield1 = null;
        }
    }
}
