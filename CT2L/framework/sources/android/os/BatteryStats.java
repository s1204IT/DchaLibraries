package android.os;

import android.app.Notification;
import android.app.backup.FullBackup;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.location.LocationManager;
import android.media.TtmlUtils;
import android.net.ProxyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.nfc.cardemulation.CardEmulation;
import android.powerhint.PowerHintManager;
import android.provider.Settings;
import android.telephony.SignalStrength;
import android.text.format.DateFormat;
import android.util.Printer;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.view.SurfaceControl;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.telephony.PhoneConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class BatteryStats implements Parcelable {
    private static final String APK_DATA = "apk";
    public static final int AUDIO_TURNED_ON = 15;
    private static final String BATTERY_DATA = "bt";
    private static final String BATTERY_DISCHARGE_DATA = "dc";
    private static final String BATTERY_LEVEL_DATA = "lv";
    private static final int BATTERY_STATS_CHECKIN_VERSION = 9;
    private static final String BLUETOOTH_STATE_COUNT_DATA = "bsc";
    public static final int BLUETOOTH_STATE_HIGH = 3;
    public static final int BLUETOOTH_STATE_INACTIVE = 0;
    public static final int BLUETOOTH_STATE_LOW = 1;
    public static final int BLUETOOTH_STATE_MEDIUM = 2;
    private static final String BLUETOOTH_STATE_TIME_DATA = "bst";
    private static final long BYTES_PER_GB = 1073741824;
    private static final long BYTES_PER_KB = 1024;
    private static final long BYTES_PER_MB = 1048576;
    private static final String CHARGE_STEP_DATA = "csd";
    private static final String CHARGE_TIME_REMAIN_DATA = "ctr";
    public static final int DATA_CONNECTION_1xRTT = 7;
    public static final int DATA_CONNECTION_CDMA = 4;
    private static final String DATA_CONNECTION_COUNT_DATA = "dcc";
    public static final int DATA_CONNECTION_EDGE = 2;
    public static final int DATA_CONNECTION_EHRPD = 14;
    public static final int DATA_CONNECTION_EVDO_0 = 5;
    public static final int DATA_CONNECTION_EVDO_A = 6;
    public static final int DATA_CONNECTION_EVDO_B = 12;
    public static final int DATA_CONNECTION_GPRS = 1;
    public static final int DATA_CONNECTION_HSDPA = 8;
    public static final int DATA_CONNECTION_HSPA = 10;
    public static final int DATA_CONNECTION_HSPAP = 15;
    public static final int DATA_CONNECTION_HSUPA = 9;
    public static final int DATA_CONNECTION_IDEN = 11;
    public static final int DATA_CONNECTION_LTE = 13;
    public static final int DATA_CONNECTION_NONE = 0;
    public static final int DATA_CONNECTION_OTHER = 16;
    private static final String DATA_CONNECTION_TIME_DATA = "dct";
    public static final int DATA_CONNECTION_UMTS = 3;
    private static final String DISCHARGE_STEP_DATA = "dsd";
    private static final String DISCHARGE_TIME_REMAIN_DATA = "dtr";
    public static final int DUMP_CHARGED_ONLY = 2;
    public static final int DUMP_DEVICE_WIFI_ONLY = 32;
    public static final int DUMP_HISTORY_ONLY = 4;
    public static final int DUMP_INCLUDE_HISTORY = 8;
    public static final int DUMP_UNPLUGGED_ONLY = 1;
    public static final int DUMP_VERBOSE = 16;
    public static final int FOREGROUND_ACTIVITY = 10;
    public static final int FULL_WIFI_LOCK = 5;
    private static final String GLOBAL_NETWORK_DATA = "gn";
    private static final String HISTORY_DATA = "h";
    private static final String HISTORY_STRING_POOL = "hsp";
    public static final int JOB = 14;
    private static final String JOB_DATA = "jb";
    private static final String KERNEL_WAKELOCK_DATA = "kwl";
    private static final boolean LOCAL_LOGV = false;
    private static final String MISC_DATA = "m";
    private static final String NETWORK_DATA = "nt";
    public static final int NETWORK_MOBILE_RX_DATA = 0;
    public static final int NETWORK_MOBILE_TX_DATA = 1;
    public static final int NETWORK_WIFI_RX_DATA = 2;
    public static final int NETWORK_WIFI_TX_DATA = 3;
    public static final int NUM_BLUETOOTH_STATES = 4;
    public static final int NUM_DATA_CONNECTION_TYPES = 17;
    public static final int NUM_NETWORK_ACTIVITY_TYPES = 4;
    public static final int NUM_SCREEN_BRIGHTNESS_BINS = 5;
    public static final int NUM_WIFI_SIGNAL_STRENGTH_BINS = 5;
    public static final int NUM_WIFI_STATES = 8;
    public static final int NUM_WIFI_SUPPL_STATES = 13;
    private static final String POWER_USE_ITEM_DATA = "pwi";
    private static final String POWER_USE_SUMMARY_DATA = "pws";
    private static final String PROCESS_DATA = "pr";
    public static final int PROCESS_STATE = 12;
    public static final int SCREEN_BRIGHTNESS_BRIGHT = 4;
    public static final int SCREEN_BRIGHTNESS_DARK = 0;
    private static final String SCREEN_BRIGHTNESS_DATA = "br";
    public static final int SCREEN_BRIGHTNESS_DIM = 1;
    public static final int SCREEN_BRIGHTNESS_LIGHT = 3;
    public static final int SCREEN_BRIGHTNESS_MEDIUM = 2;
    public static final int SENSOR = 3;
    private static final String SENSOR_DATA = "sr";
    public static final String SERVICE_NAME = "batterystats";
    private static final String SIGNAL_SCANNING_TIME_DATA = "sst";
    private static final String SIGNAL_STRENGTH_COUNT_DATA = "sgc";
    private static final String SIGNAL_STRENGTH_TIME_DATA = "sgt";
    private static final String STATE_TIME_DATA = "st";
    public static final int STATS_CURRENT = 1;
    public static final int STATS_SINCE_CHARGED = 0;
    public static final int STATS_SINCE_UNPLUGGED = 2;
    public static final long STEP_LEVEL_INITIAL_MODE_MASK = 71776119061217280L;
    public static final long STEP_LEVEL_INITIAL_MODE_SHIFT = 48;
    public static final long STEP_LEVEL_LEVEL_MASK = 280375465082880L;
    public static final long STEP_LEVEL_LEVEL_SHIFT = 40;
    public static final int STEP_LEVEL_MODE_POWER_SAVE = 4;
    public static final int STEP_LEVEL_MODE_SCREEN_STATE = 3;
    public static final long STEP_LEVEL_MODIFIED_MODE_MASK = -72057594037927936L;
    public static final long STEP_LEVEL_MODIFIED_MODE_SHIFT = 56;
    public static final long STEP_LEVEL_TIME_MASK = 1099511627775L;
    public static final int SYNC = 13;
    private static final String SYNC_DATA = "sy";
    private static final String UID_DATA = "uid";
    private static final String USER_ACTIVITY_DATA = "ua";
    private static final String VERSION_DATA = "vers";
    private static final String VIBRATOR_DATA = "vib";
    public static final int VIBRATOR_ON = 9;
    public static final int VIDEO_TURNED_ON = 8;
    private static final String WAKELOCK_DATA = "wl";
    private static final String WAKEUP_REASON_DATA = "wr";
    public static final int WAKE_TYPE_FULL = 1;
    public static final int WAKE_TYPE_PARTIAL = 0;
    public static final int WAKE_TYPE_WINDOW = 2;
    public static final int WIFI_BATCHED_SCAN = 11;
    private static final String WIFI_DATA = "wfl";
    public static final int WIFI_MULTICAST_ENABLED = 7;
    public static final int WIFI_RUNNING = 4;
    public static final int WIFI_SCAN = 6;
    private static final String WIFI_SIGNAL_STRENGTH_COUNT_DATA = "wsgc";
    private static final String WIFI_SIGNAL_STRENGTH_TIME_DATA = "wsgt";
    private static final String WIFI_STATE_COUNT_DATA = "wsc";
    public static final int WIFI_STATE_OFF = 0;
    public static final int WIFI_STATE_OFF_SCANNING = 1;
    public static final int WIFI_STATE_ON_CONNECTED_P2P = 5;
    public static final int WIFI_STATE_ON_CONNECTED_STA = 4;
    public static final int WIFI_STATE_ON_CONNECTED_STA_P2P = 6;
    public static final int WIFI_STATE_ON_DISCONNECTED = 3;
    public static final int WIFI_STATE_ON_NO_NETWORKS = 2;
    public static final int WIFI_STATE_SOFT_AP = 7;
    private static final String WIFI_STATE_TIME_DATA = "wst";
    public static final int WIFI_SUPPL_STATE_ASSOCIATED = 7;
    public static final int WIFI_SUPPL_STATE_ASSOCIATING = 6;
    public static final int WIFI_SUPPL_STATE_AUTHENTICATING = 5;
    public static final int WIFI_SUPPL_STATE_COMPLETED = 10;
    private static final String WIFI_SUPPL_STATE_COUNT_DATA = "wssc";
    public static final int WIFI_SUPPL_STATE_DISCONNECTED = 1;
    public static final int WIFI_SUPPL_STATE_DORMANT = 11;
    public static final int WIFI_SUPPL_STATE_FOUR_WAY_HANDSHAKE = 8;
    public static final int WIFI_SUPPL_STATE_GROUP_HANDSHAKE = 9;
    public static final int WIFI_SUPPL_STATE_INACTIVE = 3;
    public static final int WIFI_SUPPL_STATE_INTERFACE_DISABLED = 2;
    public static final int WIFI_SUPPL_STATE_INVALID = 0;
    public static final int WIFI_SUPPL_STATE_SCANNING = 4;
    private static final String WIFI_SUPPL_STATE_TIME_DATA = "wsst";
    public static final int WIFI_SUPPL_STATE_UNINITIALIZED = 12;
    private final StringBuilder mFormatBuilder = new StringBuilder(32);
    private final Formatter mFormatter = new Formatter(this.mFormatBuilder);
    private static final String[] STAT_NAMES = {"l", FullBackup.CACHE_TREE_TOKEN, "u"};
    static final String[] SCREEN_BRIGHTNESS_NAMES = {"dark", "dim", "medium", "light", "bright"};
    static final String[] SCREEN_BRIGHTNESS_SHORT_NAMES = {WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"};
    static final String[] DATA_CONNECTION_NAMES = {"none", "gprs", "edge", "umts", "cdma", "evdo_0", "evdo_A", "1xrtt", "hsdpa", "hsupa", "hspa", "iden", "evdo_b", "lte", "ehrpd", "hspap", CardEmulation.CATEGORY_OTHER};
    static final String[] WIFI_SUPPL_STATE_NAMES = {"invalid", "disconn", "disabled", "inactive", "scanning", "authenticating", "associating", "associated", "4-way-handshake", "group-handshake", "completed", "dormant", "uninit"};
    static final String[] WIFI_SUPPL_STATE_SHORT_NAMES = {"inv", "dsc", "dis", "inact", "scan", "auth", "ascing", "asced", "4-way", WifiConfiguration.GroupCipher.varName, "compl", "dorm", "uninit"};
    public static final BitDescription[] HISTORY_STATE_DESCRIPTIONS = {new BitDescription(Integer.MIN_VALUE, "running", FullBackup.ROOT_TREE_TOKEN), new BitDescription(1073741824, "wake_lock", "w"), new BitDescription(8388608, Context.SENSOR_SERVICE, "s"), new BitDescription(536870912, LocationManager.GPS_PROVIDER, "g"), new BitDescription(268435456, "wifi_full_lock", "Wl"), new BitDescription(134217728, "wifi_scan", "Ws"), new BitDescription(67108864, "wifi_multicast", "Wm"), new BitDescription(33554432, "mobile_radio", "Pr"), new BitDescription(2097152, "phone_scanning", "Psc"), new BitDescription(4194304, Context.AUDIO_SERVICE, FullBackup.APK_TREE_TOKEN), new BitDescription(1048576, PowerHintManager.HINT_SCREEN, "S"), new BitDescription(524288, BatteryManager.EXTRA_PLUGGED, "BP"), new BitDescription(262144, "phone_in_call", "Pcl"), new BitDescription(65536, "bluetooth", "b"), new BitDescription(HistoryItem.STATE_DATA_CONNECTION_MASK, 9, "data_conn", "Pcn", DATA_CONNECTION_NAMES, DATA_CONNECTION_NAMES), new BitDescription(448, 6, "phone_state", "Pst", new String[]{"in", "out", PhoneConstants.APN_TYPE_EMERGENCY, "off"}, new String[]{"in", "out", "em", "off"}), new BitDescription(56, 3, "phone_signal_strength", "Pss", SignalStrength.SIGNAL_STRENGTH_NAMES, new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"}), new BitDescription(7, 0, "brightness", "Sb", SCREEN_BRIGHTNESS_NAMES, SCREEN_BRIGHTNESS_SHORT_NAMES)};
    public static final BitDescription[] HISTORY_STATE2_DESCRIPTIONS = {new BitDescription(Integer.MIN_VALUE, Settings.Global.LOW_POWER_MODE, "lp"), new BitDescription(1073741824, "video", "v"), new BitDescription(536870912, "wifi_running", "Wr"), new BitDescription(268435456, "wifi", "W"), new BitDescription(134217728, "flashlight", "fl"), new BitDescription(112, 4, "wifi_signal_strength", "Wss", new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"}, new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"}), new BitDescription(15, 0, "wifi_suppl", "Wsp", WIFI_SUPPL_STATE_NAMES, WIFI_SUPPL_STATE_SHORT_NAMES)};
    private static final String FOREGROUND_DATA = "fg";
    public static final String[] HISTORY_EVENT_NAMES = {"null", "proc", FOREGROUND_DATA, "top", "sync", "wake_lock_in", "job", "user", "userfg", "conn"};
    public static final String[] HISTORY_EVENT_CHECKIN_NAMES = {"Enl", "Epr", "Efg", "Etp", "Esy", "Ewl", "Ejb", "Eur", "Euf", "Ecn"};
    static final String[] WIFI_STATE_NAMES = {"off", "scanning", "no_net", "disconn", "sta", "p2p", "sta_p2p", "soft_ap"};
    static final String[] BLUETOOTH_STATE_NAMES = {"inactive", "low", "med", "high"};

    public static abstract class Counter {
        public abstract int getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static abstract class LongCounter {
        public abstract long getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static abstract class Timer {
        public abstract int getCountLocked(int i);

        public abstract long getTotalTimeLocked(long j, int i);

        public abstract void logState(Printer printer, String str);
    }

    public abstract void commitCurrentHistoryBatchLocked();

    public abstract long computeBatteryRealtime(long j, int i);

    public abstract long computeBatteryScreenOffRealtime(long j, int i);

    public abstract long computeBatteryScreenOffUptime(long j, int i);

    public abstract long computeBatteryTimeRemaining(long j);

    public abstract long computeBatteryUptime(long j, int i);

    public abstract long computeChargeTimeRemaining(long j);

    public abstract long computeRealtime(long j, int i);

    public abstract long computeUptime(long j, int i);

    public abstract void finishIteratingHistoryLocked();

    public abstract void finishIteratingOldHistoryLocked();

    public abstract long getBatteryRealtime(long j);

    public abstract long getBatteryUptime(long j);

    public abstract long getBluetoothOnTime(long j, int i);

    public abstract int getBluetoothPingCount();

    public abstract int getBluetoothStateCount(int i, int i2);

    public abstract long getBluetoothStateTime(int i, long j, int i2);

    public abstract long[] getChargeStepDurationsArray();

    public abstract int getCpuSpeedSteps();

    public abstract int getDischargeAmount(int i);

    public abstract int getDischargeAmountScreenOff();

    public abstract int getDischargeAmountScreenOffSinceCharge();

    public abstract int getDischargeAmountScreenOn();

    public abstract int getDischargeAmountScreenOnSinceCharge();

    public abstract int getDischargeCurrentLevel();

    public abstract int getDischargeStartLevel();

    public abstract long[] getDischargeStepDurationsArray();

    public abstract String getEndPlatformVersion();

    public abstract long getFlashlightOnCount(int i);

    public abstract long getFlashlightOnTime(long j, int i);

    public abstract long getGlobalWifiRunningTime(long j, int i);

    public abstract int getHighDischargeAmountSinceCharge();

    public abstract long getHistoryBaseTime();

    public abstract int getHistoryStringPoolBytes();

    public abstract int getHistoryStringPoolSize();

    public abstract String getHistoryTagPoolString(int i);

    public abstract int getHistoryTagPoolUid(int i);

    public abstract int getHistoryTotalSize();

    public abstract int getHistoryUsedSize();

    public abstract long getInteractiveTime(long j, int i);

    public abstract boolean getIsOnBattery();

    public abstract Map<String, ? extends Timer> getKernelWakelockStats();

    public abstract int getLowDischargeAmountSinceCharge();

    public abstract int getLowPowerModeEnabledCount(int i);

    public abstract long getLowPowerModeEnabledTime(long j, int i);

    public abstract long getMobileRadioActiveAdjustedTime(int i);

    public abstract int getMobileRadioActiveCount(int i);

    public abstract long getMobileRadioActiveTime(long j, int i);

    public abstract int getMobileRadioActiveUnknownCount(int i);

    public abstract long getMobileRadioActiveUnknownTime(int i);

    public abstract long getNetworkActivityBytes(int i, int i2);

    public abstract long getNetworkActivityPackets(int i, int i2);

    public abstract boolean getNextHistoryLocked(HistoryItem historyItem);

    public abstract boolean getNextOldHistoryLocked(HistoryItem historyItem);

    public abstract int getNumChargeStepDurations();

    public abstract int getNumConnectivityChange(int i);

    public abstract int getNumDischargeStepDurations();

    public abstract int getParcelVersion();

    public abstract int getPhoneDataConnectionCount(int i, int i2);

    public abstract long getPhoneDataConnectionTime(int i, long j, int i2);

    public abstract int getPhoneOnCount(int i);

    public abstract long getPhoneOnTime(long j, int i);

    public abstract long getPhoneSignalScanningTime(long j, int i);

    public abstract int getPhoneSignalStrengthCount(int i, int i2);

    public abstract long getPhoneSignalStrengthTime(int i, long j, int i2);

    public abstract long getScreenBrightnessTime(int i, long j, int i2);

    public abstract int getScreenOnCount(int i);

    public abstract long getScreenOnTime(long j, int i);

    public abstract long getStartClockTime();

    public abstract int getStartCount();

    public abstract String getStartPlatformVersion();

    public abstract SparseArray<? extends Uid> getUidStats();

    public abstract Map<String, ? extends Timer> getWakeupReasonStats();

    public abstract long getWifiOnTime(long j, int i);

    public abstract int getWifiSignalStrengthCount(int i, int i2);

    public abstract long getWifiSignalStrengthTime(int i, long j, int i2);

    public abstract int getWifiStateCount(int i, int i2);

    public abstract long getWifiStateTime(int i, long j, int i2);

    public abstract int getWifiSupplStateCount(int i, int i2);

    public abstract long getWifiSupplStateTime(int i, long j, int i2);

    public abstract boolean startIteratingHistoryLocked();

    public abstract boolean startIteratingOldHistoryLocked();

    public abstract void writeToParcelWithoutUids(Parcel parcel, int i);

    public static abstract class Uid {
        public static final int NUM_PROCESS_STATE = 3;
        public static final int NUM_USER_ACTIVITY_TYPES = 3;
        public static final int NUM_WIFI_BATCHED_SCAN_BINS = 5;
        public static final int PROCESS_STATE_ACTIVE = 1;
        public static final int PROCESS_STATE_FOREGROUND = 0;
        public static final int PROCESS_STATE_RUNNING = 2;
        static final String[] PROCESS_STATE_NAMES = {"Foreground", "Active", "Running"};
        static final String[] USER_ACTIVITY_TYPES = {CardEmulation.CATEGORY_OTHER, "button", "touch"};

        public static abstract class Proc {

            public static class ExcessivePower {
                public static final int TYPE_CPU = 2;
                public static final int TYPE_WAKE = 1;
                public long overTime;
                public int type;
                public long usedTime;
            }

            public abstract int countExcessivePowers();

            public abstract ExcessivePower getExcessivePower(int i);

            public abstract long getForegroundTime(int i);

            public abstract int getNumAnrs(int i);

            public abstract int getNumCrashes(int i);

            public abstract int getStarts(int i);

            public abstract long getSystemTime(int i);

            public abstract long getTimeAtCpuSpeedStep(int i, int i2);

            public abstract long getUserTime(int i);

            public abstract boolean isActive();
        }

        public static abstract class Sensor {
            public static final int GPS = -10000;

            public abstract int getHandle();

            public abstract Timer getSensorTime();
        }

        public static abstract class Wakelock {
            public abstract Timer getWakeTime(int i);
        }

        public abstract long getAudioTurnedOnTime(long j, int i);

        public abstract Timer getForegroundActivityTimer();

        public abstract long getFullWifiLockTime(long j, int i);

        public abstract Map<String, ? extends Timer> getJobStats();

        public abstract int getMobileRadioActiveCount(int i);

        public abstract long getMobileRadioActiveTime(int i);

        public abstract long getNetworkActivityBytes(int i, int i2);

        public abstract long getNetworkActivityPackets(int i, int i2);

        public abstract Map<String, ? extends Pkg> getPackageStats();

        public abstract SparseArray<? extends Pid> getPidStats();

        public abstract long getProcessStateTime(int i, long j, int i2);

        public abstract Map<String, ? extends Proc> getProcessStats();

        public abstract SparseArray<? extends Sensor> getSensorStats();

        public abstract Map<String, ? extends Timer> getSyncStats();

        public abstract int getUid();

        public abstract int getUserActivityCount(int i, int i2);

        public abstract Timer getVibratorOnTimer();

        public abstract long getVideoTurnedOnTime(long j, int i);

        public abstract Map<String, ? extends Wakelock> getWakelockStats();

        public abstract long getWifiBatchedScanTime(int i, long j, int i2);

        public abstract long getWifiMulticastTime(long j, int i);

        public abstract long getWifiRunningTime(long j, int i);

        public abstract long getWifiScanTime(long j, int i);

        public abstract boolean hasNetworkActivity();

        public abstract boolean hasUserActivity();

        public abstract void noteActivityPausedLocked(long j);

        public abstract void noteActivityResumedLocked(long j);

        public abstract void noteFullWifiLockAcquiredLocked(long j);

        public abstract void noteFullWifiLockReleasedLocked(long j);

        public abstract void noteUserActivityLocked(int i);

        public abstract void noteWifiBatchedScanStartedLocked(int i, long j);

        public abstract void noteWifiBatchedScanStoppedLocked(long j);

        public abstract void noteWifiMulticastDisabledLocked(long j);

        public abstract void noteWifiMulticastEnabledLocked(long j);

        public abstract void noteWifiRunningLocked(long j);

        public abstract void noteWifiScanStartedLocked(long j);

        public abstract void noteWifiScanStoppedLocked(long j);

        public abstract void noteWifiStoppedLocked(long j);

        public class Pid {
            public int mWakeNesting;
            public long mWakeStartMs;
            public long mWakeSumMs;

            public Pid() {
            }
        }

        public static abstract class Pkg {
            public abstract Map<String, ? extends Serv> getServiceStats();

            public abstract int getWakeups(int i);

            public abstract class Serv {
                public abstract int getLaunches(int i);

                public abstract long getStartTime(long j, int i);

                public abstract int getStarts(int i);

                public Serv() {
                }
            }
        }
    }

    public static final class HistoryTag {
        public int poolIdx;
        public String string;
        public int uid;

        public void setTo(HistoryTag o) {
            this.string = o.string;
            this.uid = o.uid;
            this.poolIdx = o.poolIdx;
        }

        public void setTo(String _string, int _uid) {
            this.string = _string;
            this.uid = _uid;
            this.poolIdx = -1;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.string);
            dest.writeInt(this.uid);
        }

        public void readFromParcel(Parcel src) {
            this.string = src.readString();
            this.uid = src.readInt();
            this.poolIdx = -1;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HistoryTag that = (HistoryTag) o;
            return this.uid == that.uid && this.string.equals(that.string);
        }

        public int hashCode() {
            int result = this.string.hashCode();
            return (result * 31) + this.uid;
        }
    }

    public static final class HistoryItem implements Parcelable {
        public static final byte CMD_CURRENT_TIME = 5;
        public static final byte CMD_NULL = -1;
        public static final byte CMD_OVERFLOW = 6;
        public static final byte CMD_RESET = 7;
        public static final byte CMD_SHUTDOWN = 8;
        public static final byte CMD_START = 4;
        public static final byte CMD_UPDATE = 0;
        public static final int EVENT_CONNECTIVITY_CHANGED = 9;
        public static final int EVENT_COUNT = 10;
        public static final int EVENT_FLAG_FINISH = 16384;
        public static final int EVENT_FLAG_START = 32768;
        public static final int EVENT_FOREGROUND = 2;
        public static final int EVENT_FOREGROUND_FINISH = 16386;
        public static final int EVENT_FOREGROUND_START = 32770;
        public static final int EVENT_JOB = 6;
        public static final int EVENT_JOB_FINISH = 16390;
        public static final int EVENT_JOB_START = 32774;
        public static final int EVENT_NONE = 0;
        public static final int EVENT_PROC = 1;
        public static final int EVENT_PROC_FINISH = 16385;
        public static final int EVENT_PROC_START = 32769;
        public static final int EVENT_SYNC = 4;
        public static final int EVENT_SYNC_FINISH = 16388;
        public static final int EVENT_SYNC_START = 32772;
        public static final int EVENT_TOP = 3;
        public static final int EVENT_TOP_FINISH = 16387;
        public static final int EVENT_TOP_START = 32771;
        public static final int EVENT_TYPE_MASK = -49153;
        public static final int EVENT_USER_FOREGROUND = 8;
        public static final int EVENT_USER_FOREGROUND_FINISH = 16392;
        public static final int EVENT_USER_FOREGROUND_START = 32776;
        public static final int EVENT_USER_RUNNING = 7;
        public static final int EVENT_USER_RUNNING_FINISH = 16391;
        public static final int EVENT_USER_RUNNING_START = 32775;
        public static final int EVENT_WAKE_LOCK = 5;
        public static final int EVENT_WAKE_LOCK_FINISH = 16389;
        public static final int EVENT_WAKE_LOCK_START = 32773;
        public static final int MOST_INTERESTING_STATES = 1900544;
        public static final int MOST_INTERESTING_STATES2 = -1879048192;
        public static final int STATE2_FLASHLIGHT_FLAG = 134217728;
        public static final int STATE2_LOW_POWER_FLAG = Integer.MIN_VALUE;
        public static final int STATE2_VIDEO_ON_FLAG = 1073741824;
        public static final int STATE2_WIFI_ON_FLAG = 268435456;
        public static final int STATE2_WIFI_RUNNING_FLAG = 536870912;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_MASK = 112;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_SHIFT = 4;
        public static final int STATE2_WIFI_SUPPL_STATE_MASK = 15;
        public static final int STATE2_WIFI_SUPPL_STATE_SHIFT = 0;
        public static final int STATE_AUDIO_ON_FLAG = 4194304;
        public static final int STATE_BATTERY_PLUGGED_FLAG = 524288;
        public static final int STATE_BLUETOOTH_ON_FLAG = 65536;
        public static final int STATE_BRIGHTNESS_MASK = 7;
        public static final int STATE_BRIGHTNESS_SHIFT = 0;
        public static final int STATE_CPU_RUNNING_FLAG = Integer.MIN_VALUE;
        public static final int STATE_DATA_CONNECTION_MASK = 15872;
        public static final int STATE_DATA_CONNECTION_SHIFT = 9;
        public static final int STATE_GPS_ON_FLAG = 536870912;
        public static final int STATE_MOBILE_RADIO_ACTIVE_FLAG = 33554432;
        public static final int STATE_PHONE_IN_CALL_FLAG = 262144;
        public static final int STATE_PHONE_SCANNING_FLAG = 2097152;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_MASK = 56;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_SHIFT = 3;
        public static final int STATE_PHONE_STATE_MASK = 448;
        public static final int STATE_PHONE_STATE_SHIFT = 6;
        public static final int STATE_SCREEN_ON_FLAG = 1048576;
        public static final int STATE_SENSOR_ON_FLAG = 8388608;
        public static final int STATE_WAKE_LOCK_FLAG = 1073741824;
        public static final int STATE_WIFI_FULL_LOCK_FLAG = 268435456;
        public static final int STATE_WIFI_MULTICAST_ON_FLAG = 67108864;
        public static final int STATE_WIFI_SCAN_FLAG = 134217728;
        public byte batteryHealth;
        public byte batteryLevel;
        public byte batteryPlugType;
        public byte batteryStatus;
        public short batteryTemperature;
        public char batteryVoltage;
        public byte cmd;
        public long currentTime;
        public int eventCode;
        public HistoryTag eventTag;
        public final HistoryTag localEventTag;
        public final HistoryTag localWakeReasonTag;
        public final HistoryTag localWakelockTag;
        public HistoryItem next;
        public int numReadInts;
        public int states;
        public int states2;
        public long time;
        public HistoryTag wakeReasonTag;
        public HistoryTag wakelockTag;

        public boolean isDeltaData() {
            return this.cmd == 0;
        }

        public HistoryItem() {
            this.cmd = (byte) -1;
            this.localWakelockTag = new HistoryTag();
            this.localWakeReasonTag = new HistoryTag();
            this.localEventTag = new HistoryTag();
        }

        public HistoryItem(long time, Parcel src) {
            this.cmd = (byte) -1;
            this.localWakelockTag = new HistoryTag();
            this.localWakeReasonTag = new HistoryTag();
            this.localEventTag = new HistoryTag();
            this.time = time;
            this.numReadInts = 2;
            readFromParcel(src);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.time);
            int bat = (this.wakeReasonTag != null ? 536870912 : 0) | ((this.batteryPlugType << 24) & 251658240) | (this.cmd & CMD_NULL) | ((this.batteryLevel << 8) & 65280) | ((this.batteryStatus << 16) & SurfaceControl.FX_SURFACE_MASK) | ((this.batteryHealth << 20) & 15728640) | (this.wakelockTag != null ? 268435456 : 0) | (this.eventCode != 0 ? 1073741824 : 0);
            dest.writeInt(bat);
            int bat2 = (this.batteryTemperature & 65535) | ((this.batteryVoltage << 16) & (-65536));
            dest.writeInt(bat2);
            dest.writeInt(this.states);
            dest.writeInt(this.states2);
            if (this.wakelockTag != null) {
                this.wakelockTag.writeToParcel(dest, flags);
            }
            if (this.wakeReasonTag != null) {
                this.wakeReasonTag.writeToParcel(dest, flags);
            }
            if (this.eventCode != 0) {
                dest.writeInt(this.eventCode);
                this.eventTag.writeToParcel(dest, flags);
            }
            if (this.cmd == 5 || this.cmd == 7) {
                dest.writeLong(this.currentTime);
            }
        }

        public void readFromParcel(Parcel src) {
            int start = src.dataPosition();
            int bat = src.readInt();
            this.cmd = (byte) (bat & 255);
            this.batteryLevel = (byte) ((bat >> 8) & 255);
            this.batteryStatus = (byte) ((bat >> 16) & 15);
            this.batteryHealth = (byte) ((bat >> 20) & 15);
            this.batteryPlugType = (byte) ((bat >> 24) & 15);
            int bat2 = src.readInt();
            this.batteryTemperature = (short) (bat2 & 65535);
            this.batteryVoltage = (char) ((bat2 >> 16) & 65535);
            this.states = src.readInt();
            this.states2 = src.readInt();
            if ((268435456 & bat) != 0) {
                this.wakelockTag = this.localWakelockTag;
                this.wakelockTag.readFromParcel(src);
            } else {
                this.wakelockTag = null;
            }
            if ((536870912 & bat) != 0) {
                this.wakeReasonTag = this.localWakeReasonTag;
                this.wakeReasonTag.readFromParcel(src);
            } else {
                this.wakeReasonTag = null;
            }
            if ((1073741824 & bat) != 0) {
                this.eventCode = src.readInt();
                this.eventTag = this.localEventTag;
                this.eventTag.readFromParcel(src);
            } else {
                this.eventCode = 0;
                this.eventTag = null;
            }
            if (this.cmd == 5 || this.cmd == 7) {
                this.currentTime = src.readLong();
            } else {
                this.currentTime = 0L;
            }
            this.numReadInts += (src.dataPosition() - start) / 4;
        }

        public void clear() {
            this.time = 0L;
            this.cmd = (byte) -1;
            this.batteryLevel = (byte) 0;
            this.batteryStatus = (byte) 0;
            this.batteryHealth = (byte) 0;
            this.batteryPlugType = (byte) 0;
            this.batteryTemperature = (short) 0;
            this.batteryVoltage = (char) 0;
            this.states = 0;
            this.states2 = 0;
            this.wakelockTag = null;
            this.wakeReasonTag = null;
            this.eventCode = 0;
            this.eventTag = null;
        }

        public void setTo(HistoryItem o) {
            this.time = o.time;
            this.cmd = o.cmd;
            setToCommon(o);
        }

        public void setTo(long time, byte cmd, HistoryItem o) {
            this.time = time;
            this.cmd = cmd;
            setToCommon(o);
        }

        private void setToCommon(HistoryItem o) {
            this.batteryLevel = o.batteryLevel;
            this.batteryStatus = o.batteryStatus;
            this.batteryHealth = o.batteryHealth;
            this.batteryPlugType = o.batteryPlugType;
            this.batteryTemperature = o.batteryTemperature;
            this.batteryVoltage = o.batteryVoltage;
            this.states = o.states;
            this.states2 = o.states2;
            if (o.wakelockTag != null) {
                this.wakelockTag = this.localWakelockTag;
                this.wakelockTag.setTo(o.wakelockTag);
            } else {
                this.wakelockTag = null;
            }
            if (o.wakeReasonTag != null) {
                this.wakeReasonTag = this.localWakeReasonTag;
                this.wakeReasonTag.setTo(o.wakeReasonTag);
            } else {
                this.wakeReasonTag = null;
            }
            this.eventCode = o.eventCode;
            if (o.eventTag != null) {
                this.eventTag = this.localEventTag;
                this.eventTag.setTo(o.eventTag);
            } else {
                this.eventTag = null;
            }
            this.currentTime = o.currentTime;
        }

        public boolean sameNonEvent(HistoryItem o) {
            return this.batteryLevel == o.batteryLevel && this.batteryStatus == o.batteryStatus && this.batteryHealth == o.batteryHealth && this.batteryPlugType == o.batteryPlugType && this.batteryTemperature == o.batteryTemperature && this.batteryVoltage == o.batteryVoltage && this.states == o.states && this.states2 == o.states2 && this.currentTime == o.currentTime;
        }

        public boolean same(HistoryItem o) {
            if (!sameNonEvent(o) || this.eventCode != o.eventCode) {
                return false;
            }
            if (this.wakelockTag != o.wakelockTag && (this.wakelockTag == null || o.wakelockTag == null || !this.wakelockTag.equals(o.wakelockTag))) {
                return false;
            }
            if (this.wakeReasonTag == o.wakeReasonTag || !(this.wakeReasonTag == null || o.wakeReasonTag == null || !this.wakeReasonTag.equals(o.wakeReasonTag))) {
                return this.eventTag == o.eventTag || !(this.eventTag == null || o.eventTag == null || !this.eventTag.equals(o.eventTag));
            }
            return false;
        }
    }

    public static final class HistoryEventTracker {
        private final HashMap<String, SparseIntArray>[] mActiveEvents = new HashMap[10];

        public boolean updateState(int code, String name, int uid, int poolIdx) {
            SparseIntArray uids;
            int idx;
            if ((32768 & code) != 0) {
                int idx2 = code & HistoryItem.EVENT_TYPE_MASK;
                HashMap<String, SparseIntArray> active = this.mActiveEvents[idx2];
                if (active == null) {
                    active = new HashMap<>();
                    this.mActiveEvents[idx2] = active;
                }
                SparseIntArray uids2 = active.get(name);
                if (uids2 == null) {
                    uids2 = new SparseIntArray();
                    active.put(name, uids2);
                }
                if (uids2.indexOfKey(uid) >= 0) {
                    return false;
                }
                uids2.put(uid, poolIdx);
            } else if ((code & 16384) != 0) {
                HashMap<String, SparseIntArray> active2 = this.mActiveEvents[code & HistoryItem.EVENT_TYPE_MASK];
                if (active2 == null || (uids = active2.get(name)) == null || (idx = uids.indexOfKey(uid)) < 0) {
                    return false;
                }
                uids.removeAt(idx);
                if (uids.size() <= 0) {
                    active2.remove(name);
                }
            }
            return true;
        }

        public void removeEvents(int code) {
            int idx = code & HistoryItem.EVENT_TYPE_MASK;
            this.mActiveEvents[idx] = null;
        }

        public HashMap<String, SparseIntArray> getStateForEvent(int code) {
            return this.mActiveEvents[code];
        }
    }

    public static final class BitDescription {
        public final int mask;
        public final String name;
        public final int shift;
        public final String shortName;
        public final String[] shortValues;
        public final String[] values;

        public BitDescription(int mask, String name, String shortName) {
            this.mask = mask;
            this.shift = -1;
            this.name = name;
            this.shortName = shortName;
            this.values = null;
            this.shortValues = null;
        }

        public BitDescription(int mask, int shift, String name, String shortName, String[] values, String[] shortValues) {
            this.mask = mask;
            this.shift = shift;
            this.name = name;
            this.shortName = shortName;
            this.values = values;
            this.shortValues = shortValues;
        }
    }

    private static final void formatTimeRaw(StringBuilder out, long seconds) {
        long days = seconds / 86400;
        if (days != 0) {
            out.append(days);
            out.append("d ");
        }
        long used = 60 * days * 60 * 24;
        long hours = (seconds - used) / 3600;
        if (hours != 0 || used != 0) {
            out.append(hours);
            out.append("h ");
        }
        long used2 = used + (60 * hours * 60);
        long mins = (seconds - used2) / 60;
        if (mins != 0 || used2 != 0) {
            out.append(mins);
            out.append("m ");
        }
        long used3 = used2 + (60 * mins);
        if (seconds != 0 || used3 != 0) {
            out.append(seconds - used3);
            out.append("s ");
        }
    }

    public static final void formatTime(StringBuilder sb, long time) {
        long sec = time / 100;
        formatTimeRaw(sb, sec);
        sb.append((time - (100 * sec)) * 10);
        sb.append("ms ");
    }

    public static final void formatTimeMs(StringBuilder sb, long time) {
        long sec = time / 1000;
        formatTimeRaw(sb, sec);
        sb.append(time - (1000 * sec));
        sb.append("ms ");
    }

    public static final void formatTimeMsNoSpace(StringBuilder sb, long time) {
        long sec = time / 1000;
        formatTimeRaw(sb, sec);
        sb.append(time - (1000 * sec));
        sb.append("ms");
    }

    public final String formatRatioLocked(long num, long den) {
        if (den == 0) {
            return "--%";
        }
        float perc = (num / den) * 100.0f;
        this.mFormatBuilder.setLength(0);
        this.mFormatter.format("%.1f%%", Float.valueOf(perc));
        return this.mFormatBuilder.toString();
    }

    final String formatBytesLocked(long bytes) {
        this.mFormatBuilder.setLength(0);
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1048576) {
            this.mFormatter.format("%.2fKB", Double.valueOf(bytes / 1024.0d));
            return this.mFormatBuilder.toString();
        }
        if (bytes < 1073741824) {
            this.mFormatter.format("%.2fMB", Double.valueOf(bytes / 1048576.0d));
            return this.mFormatBuilder.toString();
        }
        this.mFormatter.format("%.2fGB", Double.valueOf(bytes / 1.073741824E9d));
        return this.mFormatBuilder.toString();
    }

    private static long computeWakeLock(Timer timer, long elapsedRealtimeUs, int which) {
        if (timer == null) {
            return 0L;
        }
        long totalTimeMicros = timer.getTotalTimeLocked(elapsedRealtimeUs, which);
        return (500 + totalTimeMicros) / 1000;
    }

    private static final String printWakeLock(StringBuilder sb, Timer timer, long elapsedRealtimeUs, String name, int which, String linePrefix) {
        if (timer != null) {
            long totalTimeMillis = computeWakeLock(timer, elapsedRealtimeUs, which);
            int count = timer.getCountLocked(which);
            if (totalTimeMillis != 0) {
                sb.append(linePrefix);
                formatTimeMs(sb, totalTimeMillis);
                if (name != null) {
                    sb.append(name);
                    sb.append(' ');
                }
                sb.append('(');
                sb.append(count);
                sb.append(" times)");
                return ", ";
            }
            return linePrefix;
        }
        return linePrefix;
    }

    private static final String printWakeLockCheckin(StringBuilder sb, Timer timer, long elapsedRealtimeUs, String name, int which, String linePrefix) {
        long totalTimeMicros = 0;
        int count = 0;
        if (timer != null) {
            totalTimeMicros = timer.getTotalTimeLocked(elapsedRealtimeUs, which);
            count = timer.getCountLocked(which);
        }
        sb.append(linePrefix);
        sb.append((500 + totalTimeMicros) / 1000);
        sb.append(',');
        sb.append(name != null ? name + "," : ProxyInfo.LOCAL_EXCL_LIST);
        sb.append(count);
        return ",";
    }

    private static final void dumpLine(PrintWriter pw, int uid, String category, String type, Object... args) {
        pw.print(9);
        pw.print(',');
        pw.print(uid);
        pw.print(',');
        pw.print(category);
        pw.print(',');
        pw.print(type);
        for (Object arg : args) {
            pw.print(',');
            pw.print(arg);
        }
        pw.println();
    }

    public final void dumpCheckinLocked(Context context, PrintWriter pw, int which, int reqUid) {
        dumpCheckinLocked(context, pw, which, reqUid, BatteryStatsHelper.checkWifiOnly(context));
    }

    public final void dumpCheckinLocked(Context context, PrintWriter pw, int which, int reqUid, boolean wifiOnly) {
        String label;
        long rawUptime = SystemClock.uptimeMillis() * 1000;
        long rawRealtime = SystemClock.elapsedRealtime() * 1000;
        long batteryUptime = getBatteryUptime(rawUptime);
        long whichBatteryUptime = computeBatteryUptime(rawUptime, which);
        long whichBatteryRealtime = computeBatteryRealtime(rawRealtime, which);
        long whichBatteryScreenOffUptime = computeBatteryScreenOffUptime(rawUptime, which);
        long whichBatteryScreenOffRealtime = computeBatteryScreenOffRealtime(rawRealtime, which);
        long totalRealtime = computeRealtime(rawRealtime, which);
        long totalUptime = computeUptime(rawUptime, which);
        long screenOnTime = getScreenOnTime(rawRealtime, which);
        long interactiveTime = getInteractiveTime(rawRealtime, which);
        long lowPowerModeEnabledTime = getLowPowerModeEnabledTime(rawRealtime, which);
        int connChanges = getNumConnectivityChange(which);
        long phoneOnTime = getPhoneOnTime(rawRealtime, which);
        long wifiOnTime = getWifiOnTime(rawRealtime, which);
        long wifiRunningTime = getGlobalWifiRunningTime(rawRealtime, which);
        long bluetoothOnTime = getBluetoothOnTime(rawRealtime, which);
        StringBuilder sb = new StringBuilder(128);
        SparseArray<? extends Uid> uidStats = getUidStats();
        int NU = uidStats.size();
        String category = STAT_NAMES[which];
        Object[] objArr = new Object[8];
        objArr[0] = which == 0 ? Integer.valueOf(getStartCount()) : "N/A";
        objArr[1] = Long.valueOf(whichBatteryRealtime / 1000);
        objArr[2] = Long.valueOf(whichBatteryUptime / 1000);
        objArr[3] = Long.valueOf(totalRealtime / 1000);
        objArr[4] = Long.valueOf(totalUptime / 1000);
        objArr[5] = Long.valueOf(getStartClockTime());
        objArr[6] = Long.valueOf(whichBatteryScreenOffRealtime / 1000);
        objArr[7] = Long.valueOf(whichBatteryScreenOffUptime / 1000);
        dumpLine(pw, 0, category, BATTERY_DATA, objArr);
        long fullWakeLockTimeTotal = 0;
        long partialWakeLockTimeTotal = 0;
        for (int iu = 0; iu < NU; iu++) {
            Map<String, ? extends Uid.Wakelock> wakelocks = uidStats.valueAt(iu).getWakelockStats();
            if (wakelocks.size() > 0) {
                Iterator<Map.Entry<String, ? extends Uid.Wakelock>> it = wakelocks.entrySet().iterator();
                while (it.hasNext()) {
                    Uid.Wakelock wl = it.next().getValue();
                    Timer fullWakeTimer = wl.getWakeTime(1);
                    if (fullWakeTimer != null) {
                        fullWakeLockTimeTotal += fullWakeTimer.getTotalTimeLocked(rawRealtime, which);
                    }
                    Timer partialWakeTimer = wl.getWakeTime(0);
                    if (partialWakeTimer != null) {
                        partialWakeLockTimeTotal += partialWakeTimer.getTotalTimeLocked(rawRealtime, which);
                    }
                }
            }
        }
        long mobileRxTotalBytes = getNetworkActivityBytes(0, which);
        long mobileTxTotalBytes = getNetworkActivityBytes(1, which);
        long wifiRxTotalBytes = getNetworkActivityBytes(2, which);
        long wifiTxTotalBytes = getNetworkActivityBytes(3, which);
        long mobileRxTotalPackets = getNetworkActivityPackets(0, which);
        long mobileTxTotalPackets = getNetworkActivityPackets(1, which);
        long wifiRxTotalPackets = getNetworkActivityPackets(2, which);
        long wifiTxTotalPackets = getNetworkActivityPackets(3, which);
        dumpLine(pw, 0, category, GLOBAL_NETWORK_DATA, Long.valueOf(mobileRxTotalBytes), Long.valueOf(mobileTxTotalBytes), Long.valueOf(wifiRxTotalBytes), Long.valueOf(wifiTxTotalBytes), Long.valueOf(mobileRxTotalPackets), Long.valueOf(mobileTxTotalPackets), Long.valueOf(wifiRxTotalPackets), Long.valueOf(wifiTxTotalPackets));
        dumpLine(pw, 0, category, MISC_DATA, Long.valueOf(screenOnTime / 1000), Long.valueOf(phoneOnTime / 1000), Long.valueOf(wifiOnTime / 1000), Long.valueOf(wifiRunningTime / 1000), Long.valueOf(bluetoothOnTime / 1000), Long.valueOf(mobileRxTotalBytes), Long.valueOf(mobileTxTotalBytes), Long.valueOf(wifiRxTotalBytes), Long.valueOf(wifiTxTotalBytes), Long.valueOf(fullWakeLockTimeTotal / 1000), Long.valueOf(partialWakeLockTimeTotal / 1000), 0, Long.valueOf(getMobileRadioActiveTime(rawRealtime, which) / 1000), Long.valueOf(getMobileRadioActiveAdjustedTime(which) / 1000), Long.valueOf(interactiveTime / 1000), Long.valueOf(lowPowerModeEnabledTime / 1000), Integer.valueOf(connChanges));
        Object[] args = new Object[5];
        for (int i = 0; i < 5; i++) {
            args[i] = Long.valueOf(getScreenBrightnessTime(i, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, "br", args);
        Object[] args2 = new Object[5];
        for (int i2 = 0; i2 < 5; i2++) {
            args2[i2] = Long.valueOf(getPhoneSignalStrengthTime(i2, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, SIGNAL_STRENGTH_TIME_DATA, args2);
        dumpLine(pw, 0, category, SIGNAL_SCANNING_TIME_DATA, Long.valueOf(getPhoneSignalScanningTime(rawRealtime, which) / 1000));
        for (int i3 = 0; i3 < 5; i3++) {
            args2[i3] = Integer.valueOf(getPhoneSignalStrengthCount(i3, which));
        }
        dumpLine(pw, 0, category, SIGNAL_STRENGTH_COUNT_DATA, args2);
        Object[] args3 = new Object[17];
        for (int i4 = 0; i4 < 17; i4++) {
            args3[i4] = Long.valueOf(getPhoneDataConnectionTime(i4, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, DATA_CONNECTION_TIME_DATA, args3);
        for (int i5 = 0; i5 < 17; i5++) {
            args3[i5] = Integer.valueOf(getPhoneDataConnectionCount(i5, which));
        }
        dumpLine(pw, 0, category, DATA_CONNECTION_COUNT_DATA, args3);
        Object[] args4 = new Object[8];
        for (int i6 = 0; i6 < 8; i6++) {
            args4[i6] = Long.valueOf(getWifiStateTime(i6, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, WIFI_STATE_TIME_DATA, args4);
        for (int i7 = 0; i7 < 8; i7++) {
            args4[i7] = Integer.valueOf(getWifiStateCount(i7, which));
        }
        dumpLine(pw, 0, category, WIFI_STATE_COUNT_DATA, args4);
        Object[] args5 = new Object[13];
        for (int i8 = 0; i8 < 13; i8++) {
            args5[i8] = Long.valueOf(getWifiSupplStateTime(i8, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, WIFI_SUPPL_STATE_TIME_DATA, args5);
        for (int i9 = 0; i9 < 13; i9++) {
            args5[i9] = Integer.valueOf(getWifiSupplStateCount(i9, which));
        }
        dumpLine(pw, 0, category, WIFI_SUPPL_STATE_COUNT_DATA, args5);
        Object[] args6 = new Object[5];
        for (int i10 = 0; i10 < 5; i10++) {
            args6[i10] = Long.valueOf(getWifiSignalStrengthTime(i10, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, WIFI_SIGNAL_STRENGTH_TIME_DATA, args6);
        for (int i11 = 0; i11 < 5; i11++) {
            args6[i11] = Integer.valueOf(getWifiSignalStrengthCount(i11, which));
        }
        dumpLine(pw, 0, category, WIFI_SIGNAL_STRENGTH_COUNT_DATA, args6);
        Object[] args7 = new Object[4];
        for (int i12 = 0; i12 < 4; i12++) {
            args7[i12] = Long.valueOf(getBluetoothStateTime(i12, rawRealtime, which) / 1000);
        }
        dumpLine(pw, 0, category, BLUETOOTH_STATE_TIME_DATA, args7);
        for (int i13 = 0; i13 < 4; i13++) {
            args7[i13] = Integer.valueOf(getBluetoothStateCount(i13, which));
        }
        dumpLine(pw, 0, category, BLUETOOTH_STATE_COUNT_DATA, args7);
        if (which == 2) {
            dumpLine(pw, 0, category, BATTERY_LEVEL_DATA, Integer.valueOf(getDischargeStartLevel()), Integer.valueOf(getDischargeCurrentLevel()));
        }
        if (which == 2) {
            dumpLine(pw, 0, category, BATTERY_DISCHARGE_DATA, Integer.valueOf(getDischargeStartLevel() - getDischargeCurrentLevel()), Integer.valueOf(getDischargeStartLevel() - getDischargeCurrentLevel()), Integer.valueOf(getDischargeAmountScreenOn()), Integer.valueOf(getDischargeAmountScreenOff()));
        } else {
            dumpLine(pw, 0, category, BATTERY_DISCHARGE_DATA, Integer.valueOf(getLowDischargeAmountSinceCharge()), Integer.valueOf(getHighDischargeAmountSinceCharge()), Integer.valueOf(getDischargeAmountScreenOnSinceCharge()), Integer.valueOf(getDischargeAmountScreenOffSinceCharge()));
        }
        if (reqUid < 0) {
            Map<String, ? extends Timer> kernelWakelocks = getKernelWakelockStats();
            if (kernelWakelocks.size() > 0) {
                for (Map.Entry<String, ? extends Timer> ent : kernelWakelocks.entrySet()) {
                    sb.setLength(0);
                    printWakeLockCheckin(sb, ent.getValue(), rawRealtime, null, which, ProxyInfo.LOCAL_EXCL_LIST);
                    dumpLine(pw, 0, category, KERNEL_WAKELOCK_DATA, ent.getKey(), sb.toString());
                }
            }
            Map<String, ? extends Timer> wakeupReasons = getWakeupReasonStats();
            if (wakeupReasons.size() > 0) {
                for (Map.Entry<String, ? extends Timer> ent2 : wakeupReasons.entrySet()) {
                    long totalTimeMicros = ent2.getValue().getTotalTimeLocked(rawRealtime, which);
                    int count = ent2.getValue().getCountLocked(which);
                    dumpLine(pw, 0, category, WAKEUP_REASON_DATA, "\"" + ent2.getKey() + "\"", Long.valueOf((500 + totalTimeMicros) / 1000), Integer.valueOf(count));
                }
            }
        }
        BatteryStatsHelper helper = new BatteryStatsHelper(context, false, wifiOnly);
        helper.create(this);
        helper.refreshStats(which, -1);
        List<BatterySipper> sippers = helper.getUsageList();
        if (sippers != null && sippers.size() > 0) {
            dumpLine(pw, 0, category, POWER_USE_SUMMARY_DATA, BatteryStatsHelper.makemAh(helper.getPowerProfile().getBatteryCapacity()), BatteryStatsHelper.makemAh(helper.getComputedPower()), BatteryStatsHelper.makemAh(helper.getMinDrainedPower()), BatteryStatsHelper.makemAh(helper.getMaxDrainedPower()));
            for (int i14 = 0; i14 < sippers.size(); i14++) {
                BatterySipper bs = sippers.get(i14);
                int uid = 0;
                switch (bs.drainType) {
                    case IDLE:
                        label = "idle";
                        break;
                    case CELL:
                        label = "cell";
                        break;
                    case PHONE:
                        label = "phone";
                        break;
                    case WIFI:
                        label = "wifi";
                        break;
                    case BLUETOOTH:
                        label = "blue";
                        break;
                    case SCREEN:
                        label = "scrn";
                        break;
                    case FLASHLIGHT:
                        label = "flashlight";
                        break;
                    case APP:
                        uid = bs.uidObj.getUid();
                        label = "uid";
                        break;
                    case USER:
                        uid = UserHandle.getUid(bs.userId, 0);
                        label = "user";
                        break;
                    case UNACCOUNTED:
                        label = "unacc";
                        break;
                    case OVERCOUNTED:
                        label = "over";
                        break;
                    default:
                        label = "???";
                        break;
                }
                dumpLine(pw, uid, category, POWER_USE_ITEM_DATA, label, BatteryStatsHelper.makemAh(bs.value));
            }
        }
        for (int iu2 = 0; iu2 < NU; iu2++) {
            int uid2 = uidStats.keyAt(iu2);
            if (reqUid < 0 || uid2 == reqUid) {
                Uid u = uidStats.valueAt(iu2);
                long mobileBytesRx = u.getNetworkActivityBytes(0, which);
                long mobileBytesTx = u.getNetworkActivityBytes(1, which);
                long wifiBytesRx = u.getNetworkActivityBytes(2, which);
                long wifiBytesTx = u.getNetworkActivityBytes(3, which);
                long mobilePacketsRx = u.getNetworkActivityPackets(0, which);
                long mobilePacketsTx = u.getNetworkActivityPackets(1, which);
                long mobileActiveTime = u.getMobileRadioActiveTime(which);
                int mobileActiveCount = u.getMobileRadioActiveCount(which);
                long wifiPacketsRx = u.getNetworkActivityPackets(2, which);
                long wifiPacketsTx = u.getNetworkActivityPackets(3, which);
                long fullWifiLockOnTime = u.getFullWifiLockTime(rawRealtime, which);
                long wifiScanTime = u.getWifiScanTime(rawRealtime, which);
                long uidWifiRunningTime = u.getWifiRunningTime(rawRealtime, which);
                if (mobileBytesRx > 0 || mobileBytesTx > 0 || wifiBytesRx > 0 || wifiBytesTx > 0 || mobilePacketsRx > 0 || mobilePacketsTx > 0 || wifiPacketsRx > 0 || wifiPacketsTx > 0 || mobileActiveTime > 0 || mobileActiveCount > 0) {
                    dumpLine(pw, uid2, category, NETWORK_DATA, Long.valueOf(mobileBytesRx), Long.valueOf(mobileBytesTx), Long.valueOf(wifiBytesRx), Long.valueOf(wifiBytesTx), Long.valueOf(mobilePacketsRx), Long.valueOf(mobilePacketsTx), Long.valueOf(wifiPacketsRx), Long.valueOf(wifiPacketsTx), Long.valueOf(mobileActiveTime), Integer.valueOf(mobileActiveCount));
                }
                if (fullWifiLockOnTime != 0 || wifiScanTime != 0 || uidWifiRunningTime != 0) {
                    dumpLine(pw, uid2, category, WIFI_DATA, Long.valueOf(fullWifiLockOnTime), Long.valueOf(wifiScanTime), Long.valueOf(uidWifiRunningTime));
                }
                if (u.hasUserActivity()) {
                    Object[] args8 = new Object[3];
                    boolean hasData = false;
                    for (int i15 = 0; i15 < 3; i15++) {
                        int val = u.getUserActivityCount(i15, which);
                        args8[i15] = Integer.valueOf(val);
                        if (val != 0) {
                            hasData = true;
                        }
                    }
                    if (hasData) {
                        dumpLine(pw, uid2, category, USER_ACTIVITY_DATA, args8);
                    }
                }
                Map<String, ? extends Uid.Wakelock> wakelocks2 = u.getWakelockStats();
                if (wakelocks2.size() > 0) {
                    for (Map.Entry<String, ? extends Uid.Wakelock> ent3 : wakelocks2.entrySet()) {
                        Uid.Wakelock wl2 = ent3.getValue();
                        sb.setLength(0);
                        String linePrefix = printWakeLockCheckin(sb, wl2.getWakeTime(1), rawRealtime, FullBackup.DATA_TREE_TOKEN, which, ProxyInfo.LOCAL_EXCL_LIST);
                        printWakeLockCheckin(sb, wl2.getWakeTime(2), rawRealtime, "w", which, printWakeLockCheckin(sb, wl2.getWakeTime(0), rawRealtime, TtmlUtils.TAG_P, which, linePrefix));
                        if (sb.length() > 0) {
                            String name = ent3.getKey();
                            if (name.indexOf(44) >= 0) {
                                name = name.replace(',', '_');
                            }
                            dumpLine(pw, uid2, category, WAKELOCK_DATA, name, sb.toString());
                        }
                    }
                }
                Map<String, ? extends Timer> syncs = u.getSyncStats();
                if (syncs.size() > 0) {
                    for (Map.Entry<String, ? extends Timer> ent4 : syncs.entrySet()) {
                        Timer timer = ent4.getValue();
                        long totalTime = (timer.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                        int count2 = timer.getCountLocked(which);
                        if (totalTime != 0) {
                            dumpLine(pw, uid2, category, SYNC_DATA, ent4.getKey(), Long.valueOf(totalTime), Integer.valueOf(count2));
                        }
                    }
                }
                Map<String, ? extends Timer> jobs = u.getJobStats();
                if (jobs.size() > 0) {
                    for (Map.Entry<String, ? extends Timer> ent5 : jobs.entrySet()) {
                        Timer timer2 = ent5.getValue();
                        long totalTime2 = (timer2.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                        int count3 = timer2.getCountLocked(which);
                        if (totalTime2 != 0) {
                            dumpLine(pw, uid2, category, JOB_DATA, ent5.getKey(), Long.valueOf(totalTime2), Integer.valueOf(count3));
                        }
                    }
                }
                SparseArray<? extends Uid.Sensor> sensors = u.getSensorStats();
                int NSE = sensors.size();
                for (int ise = 0; ise < NSE; ise++) {
                    Uid.Sensor se = sensors.valueAt(ise);
                    int sensorNumber = sensors.keyAt(ise);
                    Timer timer3 = se.getSensorTime();
                    if (timer3 != null) {
                        long totalTime3 = (timer3.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                        int count4 = timer3.getCountLocked(which);
                        if (totalTime3 != 0) {
                            dumpLine(pw, uid2, category, SENSOR_DATA, Integer.valueOf(sensorNumber), Long.valueOf(totalTime3), Integer.valueOf(count4));
                        }
                    }
                }
                Timer vibTimer = u.getVibratorOnTimer();
                if (vibTimer != null) {
                    long totalTime4 = (vibTimer.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                    int count5 = vibTimer.getCountLocked(which);
                    if (totalTime4 != 0) {
                        dumpLine(pw, uid2, category, VIBRATOR_DATA, Long.valueOf(totalTime4), Integer.valueOf(count5));
                    }
                }
                Timer fgTimer = u.getForegroundActivityTimer();
                if (fgTimer != null) {
                    long totalTime5 = (fgTimer.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                    int count6 = fgTimer.getCountLocked(which);
                    if (totalTime5 != 0) {
                        dumpLine(pw, uid2, category, FOREGROUND_DATA, Long.valueOf(totalTime5), Integer.valueOf(count6));
                    }
                }
                Object[] stateTimes = new Object[3];
                long totalStateTime = 0;
                for (int ips = 0; ips < 3; ips++) {
                    totalStateTime += u.getProcessStateTime(ips, rawRealtime, which);
                    stateTimes[ips] = Long.valueOf((500 + totalStateTime) / 1000);
                }
                if (totalStateTime > 0) {
                    dumpLine(pw, uid2, category, STATE_TIME_DATA, stateTimes);
                }
                Map<String, ? extends Uid.Proc> processStats = u.getProcessStats();
                if (processStats.size() > 0) {
                    for (Map.Entry<String, ? extends Uid.Proc> ent6 : processStats.entrySet()) {
                        Uid.Proc ps = ent6.getValue();
                        long userMillis = ps.getUserTime(which) * 10;
                        long systemMillis = ps.getSystemTime(which) * 10;
                        long foregroundMillis = ps.getForegroundTime(which) * 10;
                        int starts = ps.getStarts(which);
                        int numCrashes = ps.getNumCrashes(which);
                        int numAnrs = ps.getNumAnrs(which);
                        if (userMillis != 0 || systemMillis != 0 || foregroundMillis != 0 || starts != 0 || numAnrs != 0 || numCrashes != 0) {
                            dumpLine(pw, uid2, category, PROCESS_DATA, ent6.getKey(), Long.valueOf(userMillis), Long.valueOf(systemMillis), Long.valueOf(foregroundMillis), Integer.valueOf(starts), Integer.valueOf(numAnrs), Integer.valueOf(numCrashes));
                        }
                    }
                }
                Map<String, ? extends Uid.Pkg> packageStats = u.getPackageStats();
                if (packageStats.size() > 0) {
                    for (Map.Entry<String, ? extends Uid.Pkg> ent7 : packageStats.entrySet()) {
                        Uid.Pkg ps2 = ent7.getValue();
                        int wakeups = ps2.getWakeups(which);
                        Map<String, ? extends Uid.Pkg.Serv> serviceStats = ps2.getServiceStats();
                        for (Map.Entry<String, ? extends Uid.Pkg.Serv> sent : serviceStats.entrySet()) {
                            Uid.Pkg.Serv ss = sent.getValue();
                            long startTime = ss.getStartTime(batteryUptime, which);
                            int starts2 = ss.getStarts(which);
                            int launches = ss.getLaunches(which);
                            if (startTime != 0 || starts2 != 0 || launches != 0) {
                                dumpLine(pw, uid2, category, APK_DATA, Integer.valueOf(wakeups), ent7.getKey(), sent.getKey(), Long.valueOf(startTime / 1000), Integer.valueOf(starts2), Integer.valueOf(launches));
                            }
                        }
                    }
                }
            }
        }
    }

    static final class TimerEntry {
        final int mId;
        final String mName;
        final long mTime;
        final Timer mTimer;

        TimerEntry(String name, int id, Timer timer, long time) {
            this.mName = name;
            this.mId = id;
            this.mTimer = timer;
            this.mTime = time;
        }
    }

    private void printmAh(PrintWriter printer, double power) {
        printer.print(BatteryStatsHelper.makemAh(power));
    }

    public final void dumpLocked(Context context, PrintWriter pw, String prefix, int which, int reqUid) {
        dumpLocked(context, pw, prefix, which, reqUid, BatteryStatsHelper.checkWifiOnly(context));
    }

    public final void dumpLocked(Context context, PrintWriter pw, String prefix, int which, int reqUid, boolean wifiOnly) {
        long rawUptime = SystemClock.uptimeMillis() * 1000;
        long rawRealtime = SystemClock.elapsedRealtime() * 1000;
        long batteryUptime = getBatteryUptime(rawUptime);
        long whichBatteryUptime = computeBatteryUptime(rawUptime, which);
        long whichBatteryRealtime = computeBatteryRealtime(rawRealtime, which);
        long totalRealtime = computeRealtime(rawRealtime, which);
        long totalUptime = computeUptime(rawUptime, which);
        long whichBatteryScreenOffUptime = computeBatteryScreenOffUptime(rawUptime, which);
        long whichBatteryScreenOffRealtime = computeBatteryScreenOffRealtime(rawRealtime, which);
        long batteryTimeRemaining = computeBatteryTimeRemaining(rawRealtime);
        long chargeTimeRemaining = computeChargeTimeRemaining(rawRealtime);
        StringBuilder sb = new StringBuilder(128);
        SparseArray<? extends Uid> uidStats = getUidStats();
        int NU = uidStats.size();
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Time on battery: ");
        formatTimeMs(sb, whichBatteryRealtime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(whichBatteryRealtime, totalRealtime));
        sb.append(") realtime, ");
        formatTimeMs(sb, whichBatteryUptime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(whichBatteryUptime, totalRealtime));
        sb.append(") uptime");
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Time on battery screen off: ");
        formatTimeMs(sb, whichBatteryScreenOffRealtime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(whichBatteryScreenOffRealtime, totalRealtime));
        sb.append(") realtime, ");
        formatTimeMs(sb, whichBatteryScreenOffUptime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(whichBatteryScreenOffUptime, totalRealtime));
        sb.append(") uptime");
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Total run time: ");
        formatTimeMs(sb, totalRealtime / 1000);
        sb.append("realtime, ");
        formatTimeMs(sb, totalUptime / 1000);
        sb.append("uptime");
        pw.println(sb.toString());
        if (batteryTimeRemaining >= 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Battery time remaining: ");
            formatTimeMs(sb, batteryTimeRemaining / 1000);
            pw.println(sb.toString());
        }
        if (chargeTimeRemaining >= 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Charge time remaining: ");
            formatTimeMs(sb, chargeTimeRemaining / 1000);
            pw.println(sb.toString());
        }
        pw.print("  Start clock time: ");
        pw.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getStartClockTime()).toString());
        long screenOnTime = getScreenOnTime(rawRealtime, which);
        long interactiveTime = getInteractiveTime(rawRealtime, which);
        long lowPowerModeEnabledTime = getLowPowerModeEnabledTime(rawRealtime, which);
        long phoneOnTime = getPhoneOnTime(rawRealtime, which);
        long wifiRunningTime = getGlobalWifiRunningTime(rawRealtime, which);
        long wifiOnTime = getWifiOnTime(rawRealtime, which);
        long bluetoothOnTime = getBluetoothOnTime(rawRealtime, which);
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Screen on: ");
        formatTimeMs(sb, screenOnTime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(screenOnTime, whichBatteryRealtime));
        sb.append(") ");
        sb.append(getScreenOnCount(which));
        sb.append("x, Interactive: ");
        formatTimeMs(sb, interactiveTime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(interactiveTime, whichBatteryRealtime));
        sb.append(")");
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Screen brightnesses:");
        boolean didOne = false;
        for (int i = 0; i < 5; i++) {
            long time = getScreenBrightnessTime(i, rawRealtime, which);
            if (time != 0) {
                sb.append("\n    ");
                sb.append(prefix);
                didOne = true;
                sb.append(SCREEN_BRIGHTNESS_NAMES[i]);
                sb.append(" ");
                formatTimeMs(sb, time / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time, screenOnTime));
                sb.append(")");
            }
        }
        if (!didOne) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        if (lowPowerModeEnabledTime != 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Low power mode enabled: ");
            formatTimeMs(sb, lowPowerModeEnabledTime / 1000);
            sb.append("(");
            sb.append(formatRatioLocked(lowPowerModeEnabledTime, whichBatteryRealtime));
            sb.append(")");
            pw.println(sb.toString());
        }
        if (phoneOnTime != 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Active phone call: ");
            formatTimeMs(sb, phoneOnTime / 1000);
            sb.append("(");
            sb.append(formatRatioLocked(phoneOnTime, whichBatteryRealtime));
            sb.append(") ");
            sb.append(getPhoneOnCount(which));
        }
        int connChanges = getNumConnectivityChange(which);
        if (connChanges != 0) {
            pw.print(prefix);
            pw.print("  Connectivity changes: ");
            pw.println(connChanges);
        }
        long fullWakeLockTimeTotalMicros = 0;
        long partialWakeLockTimeTotalMicros = 0;
        ArrayList<TimerEntry> timers = new ArrayList<>();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            Map<String, ? extends Uid.Wakelock> wakelocks = u.getWakelockStats();
            if (wakelocks.size() > 0) {
                for (Map.Entry<String, ? extends Uid.Wakelock> ent : wakelocks.entrySet()) {
                    Uid.Wakelock wl = ent.getValue();
                    Timer fullWakeTimer = wl.getWakeTime(1);
                    if (fullWakeTimer != null) {
                        fullWakeLockTimeTotalMicros += fullWakeTimer.getTotalTimeLocked(rawRealtime, which);
                    }
                    Timer partialWakeTimer = wl.getWakeTime(0);
                    if (partialWakeTimer != null) {
                        long totalTimeMicros = partialWakeTimer.getTotalTimeLocked(rawRealtime, which);
                        if (totalTimeMicros > 0) {
                            if (reqUid < 0) {
                                timers.add(new TimerEntry(ent.getKey(), u.getUid(), partialWakeTimer, totalTimeMicros));
                            }
                            partialWakeLockTimeTotalMicros += totalTimeMicros;
                        }
                    }
                }
            }
        }
        long mobileRxTotalBytes = getNetworkActivityBytes(0, which);
        long mobileTxTotalBytes = getNetworkActivityBytes(1, which);
        long wifiRxTotalBytes = getNetworkActivityBytes(2, which);
        long wifiTxTotalBytes = getNetworkActivityBytes(3, which);
        long mobileRxTotalPackets = getNetworkActivityPackets(0, which);
        long mobileTxTotalPackets = getNetworkActivityPackets(1, which);
        long wifiRxTotalPackets = getNetworkActivityPackets(2, which);
        long wifiTxTotalPackets = getNetworkActivityPackets(3, which);
        if (fullWakeLockTimeTotalMicros != 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Total full wakelock time: ");
            formatTimeMsNoSpace(sb, (500 + fullWakeLockTimeTotalMicros) / 1000);
            pw.println(sb.toString());
        }
        if (partialWakeLockTimeTotalMicros != 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Total partial wakelock time: ");
            formatTimeMsNoSpace(sb, (500 + partialWakeLockTimeTotalMicros) / 1000);
            pw.println(sb.toString());
        }
        pw.print(prefix);
        pw.print("  Mobile total received: ");
        pw.print(formatBytesLocked(mobileRxTotalBytes));
        pw.print(", sent: ");
        pw.print(formatBytesLocked(mobileTxTotalBytes));
        pw.print(" (packets received ");
        pw.print(mobileRxTotalPackets);
        pw.print(", sent ");
        pw.print(mobileTxTotalPackets);
        pw.println(")");
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Phone signal levels:");
        boolean didOne2 = false;
        for (int i2 = 0; i2 < 5; i2++) {
            long time2 = getPhoneSignalStrengthTime(i2, rawRealtime, which);
            if (time2 != 0) {
                sb.append("\n    ");
                sb.append(prefix);
                didOne2 = true;
                sb.append(SignalStrength.SIGNAL_STRENGTH_NAMES[i2]);
                sb.append(" ");
                formatTimeMs(sb, time2 / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time2, whichBatteryRealtime));
                sb.append(") ");
                sb.append(getPhoneSignalStrengthCount(i2, which));
                sb.append("x");
            }
        }
        if (!didOne2) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Signal scanning time: ");
        formatTimeMsNoSpace(sb, getPhoneSignalScanningTime(rawRealtime, which) / 1000);
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Radio types:");
        boolean didOne3 = false;
        for (int i3 = 0; i3 < 17; i3++) {
            long time3 = getPhoneDataConnectionTime(i3, rawRealtime, which);
            if (time3 != 0) {
                sb.append("\n    ");
                sb.append(prefix);
                didOne3 = true;
                sb.append(DATA_CONNECTION_NAMES[i3]);
                sb.append(" ");
                formatTimeMs(sb, time3 / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time3, whichBatteryRealtime));
                sb.append(") ");
                sb.append(getPhoneDataConnectionCount(i3, which));
                sb.append("x");
            }
        }
        if (!didOne3) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Mobile radio active time: ");
        long mobileActiveTime = getMobileRadioActiveTime(rawRealtime, which);
        formatTimeMs(sb, mobileActiveTime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(mobileActiveTime, whichBatteryRealtime));
        sb.append(") ");
        sb.append(getMobileRadioActiveCount(which));
        sb.append("x");
        pw.println(sb.toString());
        long mobileActiveUnknownTime = getMobileRadioActiveUnknownTime(which);
        if (mobileActiveUnknownTime != 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Mobile radio active unknown time: ");
            formatTimeMs(sb, mobileActiveUnknownTime / 1000);
            sb.append("(");
            sb.append(formatRatioLocked(mobileActiveUnknownTime, whichBatteryRealtime));
            sb.append(") ");
            sb.append(getMobileRadioActiveUnknownCount(which));
            sb.append("x");
            pw.println(sb.toString());
        }
        long mobileActiveAdjustedTime = getMobileRadioActiveAdjustedTime(which);
        if (mobileActiveAdjustedTime != 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("  Mobile radio active adjusted time: ");
            formatTimeMs(sb, mobileActiveAdjustedTime / 1000);
            sb.append("(");
            sb.append(formatRatioLocked(mobileActiveAdjustedTime, whichBatteryRealtime));
            sb.append(")");
            pw.println(sb.toString());
        }
        pw.print(prefix);
        pw.print("  Wi-Fi total received: ");
        pw.print(formatBytesLocked(wifiRxTotalBytes));
        pw.print(", sent: ");
        pw.print(formatBytesLocked(wifiTxTotalBytes));
        pw.print(" (packets received ");
        pw.print(wifiRxTotalPackets);
        pw.print(", sent ");
        pw.print(wifiTxTotalPackets);
        pw.println(")");
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Wifi on: ");
        formatTimeMs(sb, wifiOnTime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(wifiOnTime, whichBatteryRealtime));
        sb.append("), Wifi running: ");
        formatTimeMs(sb, wifiRunningTime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(wifiRunningTime, whichBatteryRealtime));
        sb.append(")");
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Wifi states:");
        boolean didOne4 = false;
        for (int i4 = 0; i4 < 8; i4++) {
            long time4 = getWifiStateTime(i4, rawRealtime, which);
            if (time4 != 0) {
                sb.append("\n    ");
                didOne4 = true;
                sb.append(WIFI_STATE_NAMES[i4]);
                sb.append(" ");
                formatTimeMs(sb, time4 / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time4, whichBatteryRealtime));
                sb.append(") ");
                sb.append(getWifiStateCount(i4, which));
                sb.append("x");
            }
        }
        if (!didOne4) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Wifi supplicant states:");
        boolean didOne5 = false;
        for (int i5 = 0; i5 < 13; i5++) {
            long time5 = getWifiSupplStateTime(i5, rawRealtime, which);
            if (time5 != 0) {
                sb.append("\n    ");
                didOne5 = true;
                sb.append(WIFI_SUPPL_STATE_NAMES[i5]);
                sb.append(" ");
                formatTimeMs(sb, time5 / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time5, whichBatteryRealtime));
                sb.append(") ");
                sb.append(getWifiSupplStateCount(i5, which));
                sb.append("x");
            }
        }
        if (!didOne5) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Wifi signal levels:");
        boolean didOne6 = false;
        for (int i6 = 0; i6 < 5; i6++) {
            long time6 = getWifiSignalStrengthTime(i6, rawRealtime, which);
            if (time6 != 0) {
                sb.append("\n    ");
                sb.append(prefix);
                didOne6 = true;
                sb.append("level(");
                sb.append(i6);
                sb.append(") ");
                formatTimeMs(sb, time6 / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time6, whichBatteryRealtime));
                sb.append(") ");
                sb.append(getWifiSignalStrengthCount(i6, which));
                sb.append("x");
            }
        }
        if (!didOne6) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Bluetooth on: ");
        formatTimeMs(sb, bluetoothOnTime / 1000);
        sb.append("(");
        sb.append(formatRatioLocked(bluetoothOnTime, whichBatteryRealtime));
        sb.append(")");
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Bluetooth states:");
        boolean didOne7 = false;
        for (int i7 = 0; i7 < 4; i7++) {
            long time7 = getBluetoothStateTime(i7, rawRealtime, which);
            if (time7 != 0) {
                sb.append("\n    ");
                didOne7 = true;
                sb.append(BLUETOOTH_STATE_NAMES[i7]);
                sb.append(" ");
                formatTimeMs(sb, time7 / 1000);
                sb.append("(");
                sb.append(formatRatioLocked(time7, whichBatteryRealtime));
                sb.append(") ");
                sb.append(getPhoneDataConnectionCount(i7, which));
                sb.append("x");
            }
        }
        if (!didOne7) {
            sb.append(" (no activity)");
        }
        pw.println(sb.toString());
        pw.println();
        if (which == 2) {
            if (getIsOnBattery()) {
                pw.print(prefix);
                pw.println("  Device is currently unplugged");
                pw.print(prefix);
                pw.print("    Discharge cycle start level: ");
                pw.println(getDischargeStartLevel());
                pw.print(prefix);
                pw.print("    Discharge cycle current level: ");
                pw.println(getDischargeCurrentLevel());
            } else {
                pw.print(prefix);
                pw.println("  Device is currently plugged into power");
                pw.print(prefix);
                pw.print("    Last discharge cycle start level: ");
                pw.println(getDischargeStartLevel());
                pw.print(prefix);
                pw.print("    Last discharge cycle end level: ");
                pw.println(getDischargeCurrentLevel());
            }
            pw.print(prefix);
            pw.print("    Amount discharged while screen on: ");
            pw.println(getDischargeAmountScreenOn());
            pw.print(prefix);
            pw.print("    Amount discharged while screen off: ");
            pw.println(getDischargeAmountScreenOff());
            pw.println(" ");
        } else {
            pw.print(prefix);
            pw.println("  Device battery use since last full charge");
            pw.print(prefix);
            pw.print("    Amount discharged (lower bound): ");
            pw.println(getLowDischargeAmountSinceCharge());
            pw.print(prefix);
            pw.print("    Amount discharged (upper bound): ");
            pw.println(getHighDischargeAmountSinceCharge());
            pw.print(prefix);
            pw.print("    Amount discharged while screen on: ");
            pw.println(getDischargeAmountScreenOnSinceCharge());
            pw.print(prefix);
            pw.print("    Amount discharged while screen off: ");
            pw.println(getDischargeAmountScreenOffSinceCharge());
            pw.println();
        }
        BatteryStatsHelper helper = new BatteryStatsHelper(context, false, wifiOnly);
        helper.create(this);
        helper.refreshStats(which, -1);
        List<BatterySipper> sippers = helper.getUsageList();
        if (sippers != null && sippers.size() > 0) {
            pw.print(prefix);
            pw.println("  Estimated power use (mAh):");
            pw.print(prefix);
            pw.print("    Capacity: ");
            printmAh(pw, helper.getPowerProfile().getBatteryCapacity());
            pw.print(", Computed drain: ");
            printmAh(pw, helper.getComputedPower());
            pw.print(", actual drain: ");
            printmAh(pw, helper.getMinDrainedPower());
            if (helper.getMinDrainedPower() != helper.getMaxDrainedPower()) {
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                printmAh(pw, helper.getMaxDrainedPower());
            }
            pw.println();
            for (int i8 = 0; i8 < sippers.size(); i8++) {
                BatterySipper bs = sippers.get(i8);
                switch (bs.drainType) {
                    case IDLE:
                        pw.print(prefix);
                        pw.print("    Idle: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case CELL:
                        pw.print(prefix);
                        pw.print("    Cell standby: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case PHONE:
                        pw.print(prefix);
                        pw.print("    Phone calls: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case WIFI:
                        pw.print(prefix);
                        pw.print("    Wifi: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case BLUETOOTH:
                        pw.print(prefix);
                        pw.print("    Bluetooth: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case SCREEN:
                        pw.print(prefix);
                        pw.print("    Screen: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case FLASHLIGHT:
                        pw.print(prefix);
                        pw.print("    Flashlight: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case APP:
                        pw.print(prefix);
                        pw.print("    Uid ");
                        UserHandle.formatUid(pw, bs.uidObj.getUid());
                        pw.print(": ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case USER:
                        pw.print(prefix);
                        pw.print("    User ");
                        pw.print(bs.userId);
                        pw.print(": ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case UNACCOUNTED:
                        pw.print(prefix);
                        pw.print("    Unaccounted: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                    case OVERCOUNTED:
                        pw.print(prefix);
                        pw.print("    Over-counted: ");
                        printmAh(pw, bs.value);
                        pw.println();
                        break;
                }
            }
            pw.println();
        }
        List<BatterySipper> sippers2 = helper.getMobilemsppList();
        if (sippers2 != null && sippers2.size() > 0) {
            pw.print(prefix);
            pw.println("  Per-app mobile ms per packet:");
            long totalTime = 0;
            for (int i9 = 0; i9 < sippers2.size(); i9++) {
                BatterySipper bs2 = sippers2.get(i9);
                sb.setLength(0);
                sb.append(prefix);
                sb.append("    Uid ");
                UserHandle.formatUid(sb, bs2.uidObj.getUid());
                sb.append(": ");
                sb.append(BatteryStatsHelper.makemAh(bs2.mobilemspp));
                sb.append(" (");
                sb.append(bs2.mobileRxPackets + bs2.mobileTxPackets);
                sb.append(" packets over ");
                formatTimeMsNoSpace(sb, bs2.mobileActive);
                sb.append(") ");
                sb.append(bs2.mobileActiveCount);
                sb.append("x");
                pw.println(sb.toString());
                totalTime += bs2.mobileActive;
            }
            sb.setLength(0);
            sb.append(prefix);
            sb.append("    TOTAL TIME: ");
            formatTimeMs(sb, totalTime);
            sb.append("(");
            sb.append(formatRatioLocked(totalTime, whichBatteryRealtime));
            sb.append(")");
            pw.println(sb.toString());
            pw.println();
        }
        Comparator<TimerEntry> timerComparator = new Comparator<TimerEntry>() {
            @Override
            public int compare(TimerEntry lhs, TimerEntry rhs) {
                long lhsTime = lhs.mTime;
                long rhsTime = rhs.mTime;
                if (lhsTime < rhsTime) {
                    return 1;
                }
                if (lhsTime > rhsTime) {
                    return -1;
                }
                return 0;
            }
        };
        if (reqUid < 0) {
            Map<String, ? extends Timer> kernelWakelocks = getKernelWakelockStats();
            if (kernelWakelocks.size() > 0) {
                ArrayList<TimerEntry> ktimers = new ArrayList<>();
                for (Map.Entry<String, ? extends Timer> ent2 : kernelWakelocks.entrySet()) {
                    Timer timer = ent2.getValue();
                    long totalTimeMillis = computeWakeLock(timer, rawRealtime, which);
                    if (totalTimeMillis > 0) {
                        ktimers.add(new TimerEntry(ent2.getKey(), 0, timer, totalTimeMillis));
                    }
                }
                if (ktimers.size() > 0) {
                    Collections.sort(ktimers, timerComparator);
                    pw.print(prefix);
                    pw.println("  All kernel wake locks:");
                    for (int i10 = 0; i10 < ktimers.size(); i10++) {
                        TimerEntry timer2 = ktimers.get(i10);
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("  Kernel Wake lock ");
                        sb.append(timer2.mName);
                        String linePrefix = printWakeLock(sb, timer2.mTimer, rawRealtime, null, which, ": ");
                        if (!linePrefix.equals(": ")) {
                            sb.append(" realtime");
                            pw.println(sb.toString());
                        }
                    }
                    pw.println();
                }
            }
            if (timers.size() > 0) {
                Collections.sort(timers, timerComparator);
                pw.print(prefix);
                pw.println("  All partial wake locks:");
                for (int i11 = 0; i11 < timers.size(); i11++) {
                    TimerEntry timer3 = timers.get(i11);
                    sb.setLength(0);
                    sb.append("  Wake lock ");
                    UserHandle.formatUid(sb, timer3.mId);
                    sb.append(" ");
                    sb.append(timer3.mName);
                    printWakeLock(sb, timer3.mTimer, rawRealtime, null, which, ": ");
                    sb.append(" realtime");
                    pw.println(sb.toString());
                }
                timers.clear();
                pw.println();
            }
            Map<String, ? extends Timer> wakeupReasons = getWakeupReasonStats();
            if (wakeupReasons.size() > 0) {
                pw.print(prefix);
                pw.println("  All wakeup reasons:");
                ArrayList<TimerEntry> reasons = new ArrayList<>();
                for (Map.Entry<String, ? extends Timer> ent3 : wakeupReasons.entrySet()) {
                    reasons.add(new TimerEntry(ent3.getKey(), 0, ent3.getValue(), r15.getCountLocked(which)));
                }
                Collections.sort(reasons, timerComparator);
                for (int i12 = 0; i12 < reasons.size(); i12++) {
                    TimerEntry timer4 = reasons.get(i12);
                    sb.setLength(0);
                    sb.append(prefix);
                    sb.append("  Wakeup reason ");
                    sb.append(timer4.mName);
                    printWakeLock(sb, timer4.mTimer, rawRealtime, null, which, ": ");
                    sb.append(" realtime");
                    pw.println(sb.toString());
                }
                pw.println();
            }
        }
        for (int iu2 = 0; iu2 < NU; iu2++) {
            int uid = uidStats.keyAt(iu2);
            if (reqUid < 0 || uid == reqUid || uid == 1000) {
                Uid u2 = uidStats.valueAt(iu2);
                pw.print(prefix);
                pw.print("  ");
                UserHandle.formatUid(pw, uid);
                pw.println(":");
                boolean uidActivity = false;
                long mobileRxBytes = u2.getNetworkActivityBytes(0, which);
                long mobileTxBytes = u2.getNetworkActivityBytes(1, which);
                long wifiRxBytes = u2.getNetworkActivityBytes(2, which);
                long wifiTxBytes = u2.getNetworkActivityBytes(3, which);
                long mobileRxPackets = u2.getNetworkActivityPackets(0, which);
                long mobileTxPackets = u2.getNetworkActivityPackets(1, which);
                long uidMobileActiveTime = u2.getMobileRadioActiveTime(which);
                int uidMobileActiveCount = u2.getMobileRadioActiveCount(which);
                long wifiRxPackets = u2.getNetworkActivityPackets(2, which);
                long wifiTxPackets = u2.getNetworkActivityPackets(3, which);
                long fullWifiLockOnTime = u2.getFullWifiLockTime(rawRealtime, which);
                long wifiScanTime = u2.getWifiScanTime(rawRealtime, which);
                long uidWifiRunningTime = u2.getWifiRunningTime(rawRealtime, which);
                if (mobileRxBytes > 0 || mobileTxBytes > 0 || mobileRxPackets > 0 || mobileTxPackets > 0) {
                    pw.print(prefix);
                    pw.print("    Mobile network: ");
                    pw.print(formatBytesLocked(mobileRxBytes));
                    pw.print(" received, ");
                    pw.print(formatBytesLocked(mobileTxBytes));
                    pw.print(" sent (packets ");
                    pw.print(mobileRxPackets);
                    pw.print(" received, ");
                    pw.print(mobileTxPackets);
                    pw.println(" sent)");
                }
                if (uidMobileActiveTime > 0 || uidMobileActiveCount > 0) {
                    sb.setLength(0);
                    sb.append(prefix);
                    sb.append("    Mobile radio active: ");
                    formatTimeMs(sb, uidMobileActiveTime / 1000);
                    sb.append("(");
                    sb.append(formatRatioLocked(uidMobileActiveTime, mobileActiveTime));
                    sb.append(") ");
                    sb.append(uidMobileActiveCount);
                    sb.append("x");
                    long packets = mobileRxPackets + mobileTxPackets;
                    if (packets == 0) {
                        packets = 1;
                    }
                    sb.append(" @ ");
                    sb.append(BatteryStatsHelper.makemAh((uidMobileActiveTime / 1000) / packets));
                    sb.append(" mspp");
                    pw.println(sb.toString());
                }
                if (wifiRxBytes > 0 || wifiTxBytes > 0 || wifiRxPackets > 0 || wifiTxPackets > 0) {
                    pw.print(prefix);
                    pw.print("    Wi-Fi network: ");
                    pw.print(formatBytesLocked(wifiRxBytes));
                    pw.print(" received, ");
                    pw.print(formatBytesLocked(wifiTxBytes));
                    pw.print(" sent (packets ");
                    pw.print(wifiRxPackets);
                    pw.print(" received, ");
                    pw.print(wifiTxPackets);
                    pw.println(" sent)");
                }
                if (fullWifiLockOnTime != 0 || wifiScanTime != 0 || uidWifiRunningTime != 0) {
                    sb.setLength(0);
                    sb.append(prefix);
                    sb.append("    Wifi Running: ");
                    formatTimeMs(sb, uidWifiRunningTime / 1000);
                    sb.append("(");
                    sb.append(formatRatioLocked(uidWifiRunningTime, whichBatteryRealtime));
                    sb.append(")\n");
                    sb.append(prefix);
                    sb.append("    Full Wifi Lock: ");
                    formatTimeMs(sb, fullWifiLockOnTime / 1000);
                    sb.append("(");
                    sb.append(formatRatioLocked(fullWifiLockOnTime, whichBatteryRealtime));
                    sb.append(")\n");
                    sb.append(prefix);
                    sb.append("    Wifi Scan: ");
                    formatTimeMs(sb, wifiScanTime / 1000);
                    sb.append("(");
                    sb.append(formatRatioLocked(wifiScanTime, whichBatteryRealtime));
                    sb.append(")");
                    pw.println(sb.toString());
                }
                if (u2.hasUserActivity()) {
                    boolean hasData = false;
                    for (int i13 = 0; i13 < 3; i13++) {
                        int val = u2.getUserActivityCount(i13, which);
                        if (val != 0) {
                            if (!hasData) {
                                sb.setLength(0);
                                sb.append("    User activity: ");
                                hasData = true;
                            } else {
                                sb.append(", ");
                            }
                            sb.append(val);
                            sb.append(" ");
                            sb.append(Uid.USER_ACTIVITY_TYPES[i13]);
                        }
                    }
                    if (hasData) {
                        pw.println(sb.toString());
                    }
                }
                Map<String, ? extends Uid.Wakelock> wakelocks2 = u2.getWakelockStats();
                if (wakelocks2.size() > 0) {
                    long totalFull = 0;
                    long totalPartial = 0;
                    long totalWindow = 0;
                    int count = 0;
                    for (Map.Entry<String, ? extends Uid.Wakelock> ent4 : wakelocks2.entrySet()) {
                        Uid.Wakelock wl2 = ent4.getValue();
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    Wake lock ");
                        sb.append(ent4.getKey());
                        String linePrefix2 = printWakeLock(sb, wl2.getWakeTime(1), rawRealtime, "full", which, ": ");
                        printWakeLock(sb, wl2.getWakeTime(2), rawRealtime, Context.WINDOW_SERVICE, which, printWakeLock(sb, wl2.getWakeTime(0), rawRealtime, "partial", which, linePrefix2));
                        sb.append(" realtime");
                        pw.println(sb.toString());
                        uidActivity = true;
                        count++;
                        totalFull += computeWakeLock(wl2.getWakeTime(1), rawRealtime, which);
                        totalPartial += computeWakeLock(wl2.getWakeTime(0), rawRealtime, which);
                        totalWindow += computeWakeLock(wl2.getWakeTime(2), rawRealtime, which);
                    }
                    if (count > 1 && (totalFull != 0 || totalPartial != 0 || totalWindow != 0)) {
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    TOTAL wake: ");
                        boolean needComma = false;
                        if (totalFull != 0) {
                            needComma = true;
                            formatTimeMs(sb, totalFull);
                            sb.append("full");
                        }
                        if (totalPartial != 0) {
                            if (needComma) {
                                sb.append(", ");
                            }
                            needComma = true;
                            formatTimeMs(sb, totalPartial);
                            sb.append("partial");
                        }
                        if (totalWindow != 0) {
                            if (needComma) {
                                sb.append(", ");
                            }
                            formatTimeMs(sb, totalWindow);
                            sb.append(Context.WINDOW_SERVICE);
                        }
                        sb.append(" realtime");
                        pw.println(sb.toString());
                    }
                }
                Map<String, ? extends Timer> syncs = u2.getSyncStats();
                if (syncs.size() > 0) {
                    for (Map.Entry<String, ? extends Timer> ent5 : syncs.entrySet()) {
                        Timer timer5 = ent5.getValue();
                        long totalTime2 = (timer5.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                        int count2 = timer5.getCountLocked(which);
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    Sync ");
                        sb.append(ent5.getKey());
                        sb.append(": ");
                        if (totalTime2 != 0) {
                            formatTimeMs(sb, totalTime2);
                            sb.append("realtime (");
                            sb.append(count2);
                            sb.append(" times)");
                        } else {
                            sb.append("(not used)");
                        }
                        pw.println(sb.toString());
                        uidActivity = true;
                    }
                }
                Map<String, ? extends Timer> jobs = u2.getJobStats();
                if (jobs.size() > 0) {
                    for (Map.Entry<String, ? extends Timer> ent6 : jobs.entrySet()) {
                        Timer timer6 = ent6.getValue();
                        long totalTime3 = (timer6.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                        int count3 = timer6.getCountLocked(which);
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    Job ");
                        sb.append(ent6.getKey());
                        sb.append(": ");
                        if (totalTime3 != 0) {
                            formatTimeMs(sb, totalTime3);
                            sb.append("realtime (");
                            sb.append(count3);
                            sb.append(" times)");
                        } else {
                            sb.append("(not used)");
                        }
                        pw.println(sb.toString());
                        uidActivity = true;
                    }
                }
                SparseArray<? extends Uid.Sensor> sensors = u2.getSensorStats();
                int NSE = sensors.size();
                for (int ise = 0; ise < NSE; ise++) {
                    Uid.Sensor se = sensors.valueAt(ise);
                    sensors.keyAt(ise);
                    sb.setLength(0);
                    sb.append(prefix);
                    sb.append("    Sensor ");
                    int handle = se.getHandle();
                    if (handle == -10000) {
                        sb.append("GPS");
                    } else {
                        sb.append(handle);
                    }
                    sb.append(": ");
                    Timer timer7 = se.getSensorTime();
                    if (timer7 != null) {
                        long totalTime4 = (timer7.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                        int count4 = timer7.getCountLocked(which);
                        if (totalTime4 != 0) {
                            formatTimeMs(sb, totalTime4);
                            sb.append("realtime (");
                            sb.append(count4);
                            sb.append(" times)");
                        } else {
                            sb.append("(not used)");
                        }
                    } else {
                        sb.append("(not used)");
                    }
                    pw.println(sb.toString());
                    uidActivity = true;
                }
                Timer vibTimer = u2.getVibratorOnTimer();
                if (vibTimer != null) {
                    long totalTime5 = (vibTimer.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                    int count5 = vibTimer.getCountLocked(which);
                    if (totalTime5 != 0) {
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    Vibrator: ");
                        formatTimeMs(sb, totalTime5);
                        sb.append("realtime (");
                        sb.append(count5);
                        sb.append(" times)");
                        pw.println(sb.toString());
                        uidActivity = true;
                    }
                }
                Timer fgTimer = u2.getForegroundActivityTimer();
                if (fgTimer != null) {
                    long totalTime6 = (fgTimer.getTotalTimeLocked(rawRealtime, which) + 500) / 1000;
                    int count6 = fgTimer.getCountLocked(which);
                    if (totalTime6 != 0) {
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    Foreground activities: ");
                        formatTimeMs(sb, totalTime6);
                        sb.append("realtime (");
                        sb.append(count6);
                        sb.append(" times)");
                        pw.println(sb.toString());
                        uidActivity = true;
                    }
                }
                long totalStateTime = 0;
                for (int ips = 0; ips < 3; ips++) {
                    long time8 = u2.getProcessStateTime(ips, rawRealtime, which);
                    if (time8 > 0) {
                        totalStateTime += time8;
                        sb.setLength(0);
                        sb.append(prefix);
                        sb.append("    ");
                        sb.append(Uid.PROCESS_STATE_NAMES[ips]);
                        sb.append(" for: ");
                        formatTimeMs(sb, (500 + totalStateTime) / 1000);
                        pw.println(sb.toString());
                        uidActivity = true;
                    }
                }
                Map<String, ? extends Uid.Proc> processStats = u2.getProcessStats();
                if (processStats.size() > 0) {
                    for (Map.Entry<String, ? extends Uid.Proc> ent7 : processStats.entrySet()) {
                        Uid.Proc ps = ent7.getValue();
                        long userTime = ps.getUserTime(which);
                        long systemTime = ps.getSystemTime(which);
                        long foregroundTime = ps.getForegroundTime(which);
                        int starts = ps.getStarts(which);
                        int numCrashes = ps.getNumCrashes(which);
                        int numAnrs = ps.getNumAnrs(which);
                        int numExcessive = which == 0 ? ps.countExcessivePowers() : 0;
                        if (userTime != 0 || systemTime != 0 || foregroundTime != 0 || starts != 0 || numExcessive != 0 || numCrashes != 0 || numAnrs != 0) {
                            sb.setLength(0);
                            sb.append(prefix);
                            sb.append("    Proc ");
                            sb.append(ent7.getKey());
                            sb.append(":\n");
                            sb.append(prefix);
                            sb.append("      CPU: ");
                            formatTime(sb, userTime);
                            sb.append("usr + ");
                            formatTime(sb, systemTime);
                            sb.append("krn ; ");
                            formatTime(sb, foregroundTime);
                            sb.append(FOREGROUND_DATA);
                            if (starts != 0 || numCrashes != 0 || numAnrs != 0) {
                                sb.append("\n");
                                sb.append(prefix);
                                sb.append("      ");
                                boolean hasOne = false;
                                if (starts != 0) {
                                    hasOne = true;
                                    sb.append(starts);
                                    sb.append(" starts");
                                }
                                if (numCrashes != 0) {
                                    if (hasOne) {
                                        sb.append(", ");
                                    }
                                    hasOne = true;
                                    sb.append(numCrashes);
                                    sb.append(" crashes");
                                }
                                if (numAnrs != 0) {
                                    if (hasOne) {
                                        sb.append(", ");
                                    }
                                    sb.append(numAnrs);
                                    sb.append(" anrs");
                                }
                            }
                            pw.println(sb.toString());
                            for (int e = 0; e < numExcessive; e++) {
                                Uid.Proc.ExcessivePower ew = ps.getExcessivePower(e);
                                if (ew != null) {
                                    pw.print(prefix);
                                    pw.print("      * Killed for ");
                                    if (ew.type == 1) {
                                        pw.print("wake lock");
                                    } else if (ew.type == 2) {
                                        pw.print("cpu");
                                    } else {
                                        pw.print("unknown");
                                    }
                                    pw.print(" use: ");
                                    TimeUtils.formatDuration(ew.usedTime, pw);
                                    pw.print(" over ");
                                    TimeUtils.formatDuration(ew.overTime, pw);
                                    if (ew.overTime != 0) {
                                        pw.print(" (");
                                        pw.print((ew.usedTime * 100) / ew.overTime);
                                        pw.println("%)");
                                    }
                                }
                            }
                            uidActivity = true;
                        }
                    }
                }
                Map<String, ? extends Uid.Pkg> packageStats = u2.getPackageStats();
                if (packageStats.size() > 0) {
                    for (Map.Entry<String, ? extends Uid.Pkg> ent8 : packageStats.entrySet()) {
                        pw.print(prefix);
                        pw.print("    Apk ");
                        pw.print(ent8.getKey());
                        pw.println(":");
                        boolean apkActivity = false;
                        Uid.Pkg ps2 = ent8.getValue();
                        int wakeups = ps2.getWakeups(which);
                        if (wakeups != 0) {
                            pw.print(prefix);
                            pw.print("      ");
                            pw.print(wakeups);
                            pw.println(" wakeup alarms");
                            apkActivity = true;
                        }
                        Map<String, ? extends Uid.Pkg.Serv> serviceStats = ps2.getServiceStats();
                        if (serviceStats.size() > 0) {
                            for (Map.Entry<String, ? extends Uid.Pkg.Serv> sent : serviceStats.entrySet()) {
                                Uid.Pkg.Serv ss = sent.getValue();
                                long startTime = ss.getStartTime(batteryUptime, which);
                                int starts2 = ss.getStarts(which);
                                int launches = ss.getLaunches(which);
                                if (startTime != 0 || starts2 != 0 || launches != 0) {
                                    sb.setLength(0);
                                    sb.append(prefix);
                                    sb.append("      Service ");
                                    sb.append(sent.getKey());
                                    sb.append(":\n");
                                    sb.append(prefix);
                                    sb.append("        Created for: ");
                                    formatTimeMs(sb, startTime / 1000);
                                    sb.append("uptime\n");
                                    sb.append(prefix);
                                    sb.append("        Starts: ");
                                    sb.append(starts2);
                                    sb.append(", launches: ");
                                    sb.append(launches);
                                    pw.println(sb.toString());
                                    apkActivity = true;
                                }
                            }
                        }
                        if (!apkActivity) {
                            pw.print(prefix);
                            pw.println("      (nothing executed)");
                        }
                        uidActivity = true;
                    }
                }
                if (!uidActivity) {
                    pw.print(prefix);
                    pw.println("    (nothing executed)");
                }
            }
        }
    }

    static void printBitDescriptions(PrintWriter pw, int oldval, int newval, HistoryTag wakelockTag, BitDescription[] descriptions, boolean longNames) {
        int diff = oldval ^ newval;
        if (diff != 0) {
            boolean didWake = false;
            for (BitDescription bd : descriptions) {
                if ((bd.mask & diff) != 0) {
                    pw.print(longNames ? " " : ",");
                    if (bd.shift < 0) {
                        pw.print((bd.mask & newval) != 0 ? "+" : NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                        pw.print(longNames ? bd.name : bd.shortName);
                        if (bd.mask == 1073741824 && wakelockTag != null) {
                            didWake = true;
                            pw.print("=");
                            if (longNames) {
                                UserHandle.formatUid(pw, wakelockTag.uid);
                                pw.print(":\"");
                                pw.print(wakelockTag.string);
                                pw.print("\"");
                            } else {
                                pw.print(wakelockTag.poolIdx);
                            }
                        }
                    } else {
                        pw.print(longNames ? bd.name : bd.shortName);
                        pw.print("=");
                        int val = (bd.mask & newval) >> bd.shift;
                        if (bd.values != null && val >= 0 && val < bd.values.length) {
                            pw.print(longNames ? bd.values[val] : bd.shortValues[val]);
                        } else {
                            pw.print(val);
                        }
                    }
                }
            }
            if (!didWake && wakelockTag != null) {
                pw.print(longNames ? " wake_lock=" : ",w=");
                if (longNames) {
                    UserHandle.formatUid(pw, wakelockTag.uid);
                    pw.print(":\"");
                    pw.print(wakelockTag.string);
                    pw.print("\"");
                    return;
                }
                pw.print(wakelockTag.poolIdx);
            }
        }
    }

    public void prepareForDumpLocked() {
    }

    public static class HistoryPrinter {
        int oldState = 0;
        int oldState2 = 0;
        int oldLevel = -1;
        int oldStatus = -1;
        int oldHealth = -1;
        int oldPlug = -1;
        int oldTemp = -1;
        int oldVolt = -1;
        long lastTime = -1;

        void reset() {
            this.oldState2 = 0;
            this.oldState = 0;
            this.oldLevel = -1;
            this.oldStatus = -1;
            this.oldHealth = -1;
            this.oldPlug = -1;
            this.oldTemp = -1;
            this.oldVolt = -1;
        }

        public void printNextItem(PrintWriter pw, HistoryItem rec, long baseTime, boolean checkin, boolean verbose) {
            if (!checkin) {
                pw.print("  ");
                TimeUtils.formatDuration(rec.time - baseTime, pw, 19);
                pw.print(" (");
                pw.print(rec.numReadInts);
                pw.print(") ");
            } else {
                pw.print(9);
                pw.print(',');
                pw.print(BatteryStats.HISTORY_DATA);
                pw.print(',');
                if (this.lastTime < 0) {
                    pw.print(rec.time - baseTime);
                } else {
                    pw.print(rec.time - this.lastTime);
                }
                this.lastTime = rec.time;
            }
            if (rec.cmd == 4) {
                if (checkin) {
                    pw.print(":");
                }
                pw.println("START");
                reset();
                return;
            }
            if (rec.cmd == 5 || rec.cmd == 7) {
                if (checkin) {
                    pw.print(":");
                }
                if (rec.cmd == 7) {
                    pw.print("RESET:");
                    reset();
                }
                pw.print("TIME:");
                if (checkin) {
                    pw.println(rec.currentTime);
                    return;
                } else {
                    pw.print(" ");
                    pw.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", rec.currentTime).toString());
                    return;
                }
            }
            if (rec.cmd == 8) {
                if (checkin) {
                    pw.print(":");
                }
                pw.println("SHUTDOWN");
                return;
            }
            if (rec.cmd == 6) {
                if (checkin) {
                    pw.print(":");
                }
                pw.println("*OVERFLOW*");
                return;
            }
            if (!checkin) {
                if (rec.batteryLevel < 10) {
                    pw.print("00");
                } else if (rec.batteryLevel < 100) {
                    pw.print(WifiEnterpriseConfig.ENGINE_DISABLE);
                }
                pw.print((int) rec.batteryLevel);
                if (verbose) {
                    pw.print(" ");
                    if (rec.states >= 0) {
                        if (rec.states < 16) {
                            pw.print("0000000");
                        } else if (rec.states < 256) {
                            pw.print("000000");
                        } else if (rec.states < 4096) {
                            pw.print("00000");
                        } else if (rec.states < 65536) {
                            pw.print("0000");
                        } else if (rec.states < 1048576) {
                            pw.print("000");
                        } else if (rec.states < 16777216) {
                            pw.print("00");
                        } else if (rec.states < 268435456) {
                            pw.print(WifiEnterpriseConfig.ENGINE_DISABLE);
                        }
                    }
                    pw.print(Integer.toHexString(rec.states));
                }
            } else if (this.oldLevel != rec.batteryLevel) {
                this.oldLevel = rec.batteryLevel;
                pw.print(",Bl=");
                pw.print((int) rec.batteryLevel);
            }
            if (this.oldStatus != rec.batteryStatus) {
                this.oldStatus = rec.batteryStatus;
                pw.print(checkin ? ",Bs=" : " status=");
                switch (this.oldStatus) {
                    case 1:
                        pw.print(checkin ? "?" : "unknown");
                        break;
                    case 2:
                        pw.print(checkin ? FullBackup.CACHE_TREE_TOKEN : "charging");
                        break;
                    case 3:
                        pw.print(checkin ? "d" : "discharging");
                        break;
                    case 4:
                        pw.print(checkin ? "n" : "not-charging");
                        break;
                    case 5:
                        pw.print(checkin ? FullBackup.DATA_TREE_TOKEN : "full");
                        break;
                    default:
                        pw.print(this.oldStatus);
                        break;
                }
            }
            if (this.oldHealth != rec.batteryHealth) {
                this.oldHealth = rec.batteryHealth;
                pw.print(checkin ? ",Bh=" : " health=");
                switch (this.oldHealth) {
                    case 1:
                        pw.print(checkin ? "?" : "unknown");
                        break;
                    case 2:
                        pw.print(checkin ? "g" : "good");
                        break;
                    case 3:
                        pw.print(checkin ? BatteryStats.HISTORY_DATA : "overheat");
                        break;
                    case 4:
                        pw.print(checkin ? "d" : "dead");
                        break;
                    case 5:
                        pw.print(checkin ? "v" : "over-voltage");
                        break;
                    case 6:
                        pw.print(checkin ? FullBackup.DATA_TREE_TOKEN : "failure");
                        break;
                    case 7:
                        pw.print(checkin ? FullBackup.CACHE_TREE_TOKEN : "cold");
                        break;
                    default:
                        pw.print(this.oldHealth);
                        break;
                }
            }
            if (this.oldPlug != rec.batteryPlugType) {
                this.oldPlug = rec.batteryPlugType;
                pw.print(checkin ? ",Bp=" : " plug=");
                switch (this.oldPlug) {
                    case 0:
                        pw.print(checkin ? "n" : "none");
                        break;
                    case 1:
                        pw.print(checkin ? FullBackup.APK_TREE_TOKEN : "ac");
                        break;
                    case 2:
                        pw.print(checkin ? "u" : Context.USB_SERVICE);
                        break;
                    case 3:
                    default:
                        pw.print(this.oldPlug);
                        break;
                    case 4:
                        pw.print(checkin ? "w" : "wireless");
                        break;
                }
            }
            if (this.oldTemp != rec.batteryTemperature) {
                this.oldTemp = rec.batteryTemperature;
                pw.print(checkin ? ",Bt=" : " temp=");
                pw.print(this.oldTemp);
            }
            if (this.oldVolt != rec.batteryVoltage) {
                this.oldVolt = rec.batteryVoltage;
                pw.print(checkin ? ",Bv=" : " volt=");
                pw.print(this.oldVolt);
            }
            BatteryStats.printBitDescriptions(pw, this.oldState, rec.states, rec.wakelockTag, BatteryStats.HISTORY_STATE_DESCRIPTIONS, !checkin);
            BatteryStats.printBitDescriptions(pw, this.oldState2, rec.states2, null, BatteryStats.HISTORY_STATE2_DESCRIPTIONS, !checkin);
            if (rec.wakeReasonTag != null) {
                if (checkin) {
                    pw.print(",wr=");
                    pw.print(rec.wakeReasonTag.poolIdx);
                } else {
                    pw.print(" wake_reason=");
                    pw.print(rec.wakeReasonTag.uid);
                    pw.print(":\"");
                    pw.print(rec.wakeReasonTag.string);
                    pw.print("\"");
                }
            }
            if (rec.eventCode != 0) {
                pw.print(checkin ? "," : " ");
                if ((rec.eventCode & 32768) != 0) {
                    pw.print("+");
                } else if ((rec.eventCode & 16384) != 0) {
                    pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                }
                String[] eventNames = checkin ? BatteryStats.HISTORY_EVENT_CHECKIN_NAMES : BatteryStats.HISTORY_EVENT_NAMES;
                int idx = rec.eventCode & HistoryItem.EVENT_TYPE_MASK;
                if (idx >= 0 && idx < eventNames.length) {
                    pw.print(eventNames[idx]);
                } else {
                    pw.print(checkin ? "Ev" : Notification.CATEGORY_EVENT);
                    pw.print(idx);
                }
                pw.print("=");
                if (checkin) {
                    pw.print(rec.eventTag.poolIdx);
                } else {
                    UserHandle.formatUid(pw, rec.eventTag.uid);
                    pw.print(":\"");
                    pw.print(rec.eventTag.string);
                    pw.print("\"");
                }
            }
            pw.println();
            this.oldState = rec.states;
            this.oldState2 = rec.states2;
        }
    }

    private void printSizeValue(PrintWriter pw, long size) {
        float result = size;
        String suffix = ProxyInfo.LOCAL_EXCL_LIST;
        if (result >= 10240.0f) {
            suffix = "KB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "MB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "GB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "TB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "PB";
            result /= 1024.0f;
        }
        pw.print((int) result);
        pw.print(suffix);
    }

    private static boolean dumpTimeEstimate(PrintWriter pw, String label, long[] steps, int count, long modesOfInterest, long modeValues) {
        if (count <= 0) {
            return false;
        }
        long total = 0;
        int numOfInterest = 0;
        for (int i = 0; i < count; i++) {
            long initMode = (steps[i] & STEP_LEVEL_INITIAL_MODE_MASK) >> 48;
            long modMode = (steps[i] & STEP_LEVEL_MODIFIED_MODE_MASK) >> 56;
            if ((modMode & modesOfInterest) == 0 && (initMode & modesOfInterest) == modeValues) {
                numOfInterest++;
                total += steps[i] & STEP_LEVEL_TIME_MASK;
            }
        }
        if (numOfInterest <= 0) {
            return false;
        }
        long estimatedTime = (total / ((long) numOfInterest)) * 100;
        pw.print(label);
        StringBuilder sb = new StringBuilder(64);
        formatTimeMs(sb, estimatedTime);
        pw.print(sb);
        pw.println();
        return true;
    }

    private static boolean dumpDurationSteps(PrintWriter pw, String header, long[] steps, int count, boolean checkin) {
        if (count <= 0) {
            return false;
        }
        if (!checkin) {
            pw.println(header);
        }
        String[] lineArgs = new String[4];
        for (int i = 0; i < count; i++) {
            long duration = steps[i] & STEP_LEVEL_TIME_MASK;
            int level = (int) ((steps[i] & STEP_LEVEL_LEVEL_MASK) >> 40);
            long initMode = (steps[i] & STEP_LEVEL_INITIAL_MODE_MASK) >> 48;
            long modMode = (steps[i] & STEP_LEVEL_MODIFIED_MODE_MASK) >> 56;
            if (checkin) {
                lineArgs[0] = Long.toString(duration);
                lineArgs[1] = Integer.toString(level);
                if ((3 & modMode) == 0) {
                    switch (((int) (3 & initMode)) + 1) {
                        case 1:
                            lineArgs[2] = "s-";
                            break;
                        case 2:
                            lineArgs[2] = "s+";
                            break;
                        case 3:
                            lineArgs[2] = "sd";
                            break;
                        case 4:
                            lineArgs[2] = "sds";
                            break;
                        default:
                            lineArgs[1] = "?";
                            break;
                    }
                } else {
                    lineArgs[2] = ProxyInfo.LOCAL_EXCL_LIST;
                }
                if ((4 & modMode) == 0) {
                    lineArgs[3] = (4 & initMode) != 0 ? "p+" : "p-";
                } else {
                    lineArgs[3] = ProxyInfo.LOCAL_EXCL_LIST;
                }
                dumpLine(pw, 0, "i", header, lineArgs);
            } else {
                pw.print("  #");
                pw.print(i);
                pw.print(": ");
                TimeUtils.formatDuration(duration, pw);
                pw.print(" to ");
                pw.print(level);
                boolean haveModes = false;
                if ((3 & modMode) == 0) {
                    pw.print(" (");
                    switch (((int) (3 & initMode)) + 1) {
                        case 1:
                            pw.print("screen-off");
                            break;
                        case 2:
                            pw.print("screen-on");
                            break;
                        case 3:
                            pw.print("screen-doze");
                            break;
                        case 4:
                            pw.print("screen-doze-suspend");
                            break;
                        default:
                            lineArgs[1] = "screen-?";
                            break;
                    }
                    haveModes = true;
                }
                if ((4 & modMode) == 0) {
                    pw.print(haveModes ? ", " : " (");
                    pw.print((4 & initMode) != 0 ? "power-save-on" : "power-save-off");
                    haveModes = true;
                }
                if (haveModes) {
                    pw.print(")");
                }
                pw.println();
            }
        }
        return true;
    }

    private void dumpHistoryLocked(PrintWriter pw, int flags, long histStart, boolean checkin) {
        HistoryPrinter hprinter = new HistoryPrinter();
        HistoryItem rec = new HistoryItem();
        long lastTime = -1;
        long baseTime = -1;
        boolean printed = false;
        HistoryEventTracker tracker = null;
        while (getNextHistoryLocked(rec)) {
            lastTime = rec.time;
            if (baseTime < 0) {
                baseTime = lastTime;
            }
            if (rec.time >= histStart) {
                if (histStart >= 0 && !printed) {
                    if (rec.cmd == 5 || rec.cmd == 7 || rec.cmd == 4 || rec.cmd == 8) {
                        printed = true;
                        hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 16) != 0);
                        rec.cmd = (byte) 0;
                    } else if (rec.currentTime != 0) {
                        printed = true;
                        byte cmd = rec.cmd;
                        rec.cmd = (byte) 5;
                        hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 16) != 0);
                        rec.cmd = cmd;
                    }
                    if (tracker != null) {
                        if (rec.cmd != 0) {
                            hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 16) != 0);
                            rec.cmd = (byte) 0;
                        }
                        int oldEventCode = rec.eventCode;
                        HistoryTag oldEventTag = rec.eventTag;
                        rec.eventTag = new HistoryTag();
                        for (int i = 0; i < 10; i++) {
                            HashMap<String, SparseIntArray> active = tracker.getStateForEvent(i);
                            if (active != null) {
                                for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                                    SparseIntArray uids = ent.getValue();
                                    for (int j = 0; j < uids.size(); j++) {
                                        rec.eventCode = i;
                                        rec.eventTag.string = ent.getKey();
                                        rec.eventTag.uid = uids.keyAt(j);
                                        rec.eventTag.poolIdx = uids.valueAt(j);
                                        hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 16) != 0);
                                        rec.wakeReasonTag = null;
                                        rec.wakelockTag = null;
                                    }
                                }
                            }
                        }
                        rec.eventCode = oldEventCode;
                        rec.eventTag = oldEventTag;
                        tracker = null;
                    }
                }
                hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 16) != 0);
            }
        }
        if (histStart >= 0) {
            commitCurrentHistoryBatchLocked();
            pw.print(checkin ? "NEXT: " : "  NEXT: ");
            pw.println(1 + lastTime);
        }
    }

    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        prepareForDumpLocked();
        boolean filtering = (flags & 7) != 0;
        if ((flags & 4) != 0 || !filtering) {
            long historyTotalSize = getHistoryTotalSize();
            long historyUsedSize = getHistoryUsedSize();
            if (startIteratingHistoryLocked()) {
                try {
                    pw.print("Battery History (");
                    pw.print((100 * historyUsedSize) / historyTotalSize);
                    pw.print("% used, ");
                    printSizeValue(pw, historyUsedSize);
                    pw.print(" used of ");
                    printSizeValue(pw, historyTotalSize);
                    pw.print(", ");
                    pw.print(getHistoryStringPoolSize());
                    pw.print(" strings using ");
                    printSizeValue(pw, getHistoryStringPoolBytes());
                    pw.println("):");
                    dumpHistoryLocked(pw, flags, histStart, false);
                    pw.println();
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
            if (startIteratingOldHistoryLocked()) {
                try {
                    HistoryItem rec = new HistoryItem();
                    pw.println("Old battery History:");
                    HistoryPrinter hprinter = new HistoryPrinter();
                    long baseTime = -1;
                    while (getNextOldHistoryLocked(rec)) {
                        if (baseTime < 0) {
                            baseTime = rec.time;
                        }
                        hprinter.printNextItem(pw, rec, baseTime, false, (flags & 16) != 0);
                    }
                    pw.println();
                } finally {
                    finishIteratingOldHistoryLocked();
                }
            }
        }
        if (!filtering || (flags & 3) != 0) {
            if (!filtering) {
                SparseArray<? extends Uid> uidStats = getUidStats();
                int NU = uidStats.size();
                boolean didPid = false;
                long nowRealtime = SystemClock.elapsedRealtime();
                for (int i = 0; i < NU; i++) {
                    Uid uid = uidStats.valueAt(i);
                    SparseArray<? extends Uid.Pid> pids = uid.getPidStats();
                    if (pids != null) {
                        for (int j = 0; j < pids.size(); j++) {
                            Uid.Pid pid = pids.valueAt(j);
                            if (!didPid) {
                                pw.println("Per-PID Stats:");
                                didPid = true;
                            }
                            long time = pid.mWakeSumMs + (pid.mWakeNesting > 0 ? nowRealtime - pid.mWakeStartMs : 0L);
                            pw.print("  PID ");
                            pw.print(pids.keyAt(j));
                            pw.print(" wake time: ");
                            TimeUtils.formatDuration(time, pw);
                            pw.println(ProxyInfo.LOCAL_EXCL_LIST);
                        }
                    }
                }
                if (didPid) {
                    pw.println();
                }
            }
            if (!filtering || (flags & 2) != 0) {
                if (dumpDurationSteps(pw, "Discharge step durations:", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), false)) {
                    long timeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime());
                    if (timeRemaining >= 0) {
                        pw.print("  Estimated discharge time remaining: ");
                        TimeUtils.formatDuration(timeRemaining / 1000, pw);
                        pw.println();
                    }
                    dumpTimeEstimate(pw, "  Estimated screen off time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 0L);
                    dumpTimeEstimate(pw, "  Estimated screen off power save time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 4L);
                    dumpTimeEstimate(pw, "  Estimated screen on time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 1L);
                    dumpTimeEstimate(pw, "  Estimated screen on power save time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 5L);
                    dumpTimeEstimate(pw, "  Estimated screen doze time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 2L);
                    dumpTimeEstimate(pw, "  Estimated screen doze power save time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 6L);
                    dumpTimeEstimate(pw, "  Estimated screen doze suspend time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 3L);
                    dumpTimeEstimate(pw, "  Estimated screen doze suspend power save time: ", getDischargeStepDurationsArray(), getNumDischargeStepDurations(), 7L, 7L);
                    pw.println();
                }
                if (dumpDurationSteps(pw, "Charge step durations:", getChargeStepDurationsArray(), getNumChargeStepDurations(), false)) {
                    long timeRemaining2 = computeChargeTimeRemaining(SystemClock.elapsedRealtime());
                    if (timeRemaining2 >= 0) {
                        pw.print("  Estimated charge time remaining: ");
                        TimeUtils.formatDuration(timeRemaining2 / 1000, pw);
                        pw.println();
                    }
                    pw.println();
                }
                pw.println("Statistics since last charge:");
                pw.println("  System starts: " + getStartCount() + ", currently on battery: " + getIsOnBattery());
                dumpLocked(context, pw, ProxyInfo.LOCAL_EXCL_LIST, 0, reqUid, (flags & 32) != 0);
                pw.println();
            }
            if (!filtering || (flags & 1) != 0) {
                pw.println("Statistics since last unplugged:");
                dumpLocked(context, pw, ProxyInfo.LOCAL_EXCL_LIST, 2, reqUid, (flags & 32) != 0);
            }
        }
    }

    public void dumpCheckinLocked(Context context, PrintWriter pw, List<ApplicationInfo> apps, int flags, long histStart) {
        prepareForDumpLocked();
        dumpLine(pw, 0, "i", VERSION_DATA, "12", Integer.valueOf(getParcelVersion()), getStartPlatformVersion(), getEndPlatformVersion());
        long historyBaseTime = getHistoryBaseTime() + SystemClock.elapsedRealtime();
        boolean filtering = (flags & 7) != 0;
        if (((flags & 8) != 0 || (flags & 4) != 0) && startIteratingHistoryLocked()) {
            for (int i = 0; i < getHistoryStringPoolSize(); i++) {
                try {
                    pw.print(9);
                    pw.print(',');
                    pw.print(HISTORY_STRING_POOL);
                    pw.print(',');
                    pw.print(i);
                    pw.print(",");
                    pw.print(getHistoryTagPoolUid(i));
                    pw.print(",\"");
                    String str = getHistoryTagPoolString(i);
                    pw.print(str.replace("\\", "\\\\").replace("\"", "\\\""));
                    pw.print("\"");
                    pw.println();
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
            dumpHistoryLocked(pw, flags, histStart, true);
        }
        if (!filtering || (flags & 3) != 0) {
            if (apps != null) {
                SparseArray<ArrayList<String>> uids = new SparseArray<>();
                for (int i2 = 0; i2 < apps.size(); i2++) {
                    ApplicationInfo ai = apps.get(i2);
                    ArrayList<String> pkgs = uids.get(ai.uid);
                    if (pkgs == null) {
                        pkgs = new ArrayList<>();
                        uids.put(ai.uid, pkgs);
                    }
                    pkgs.add(ai.packageName);
                }
                SparseArray<? extends Uid> uidStats = getUidStats();
                int NU = uidStats.size();
                String[] lineArgs = new String[2];
                for (int i3 = 0; i3 < NU; i3++) {
                    int uid = uidStats.keyAt(i3);
                    ArrayList<String> pkgs2 = uids.get(uid);
                    if (pkgs2 != null) {
                        for (int j = 0; j < pkgs2.size(); j++) {
                            lineArgs[0] = Integer.toString(uid);
                            lineArgs[1] = pkgs2.get(j);
                            dumpLine(pw, 0, "i", "uid", lineArgs);
                        }
                    }
                }
            }
            if (!filtering || (flags & 2) != 0) {
                dumpDurationSteps(pw, DISCHARGE_STEP_DATA, getDischargeStepDurationsArray(), getNumDischargeStepDurations(), true);
                String[] lineArgs2 = new String[1];
                long timeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime());
                if (timeRemaining >= 0) {
                    lineArgs2[0] = Long.toString(timeRemaining);
                    dumpLine(pw, 0, "i", DISCHARGE_TIME_REMAIN_DATA, lineArgs2);
                }
                dumpDurationSteps(pw, CHARGE_STEP_DATA, getChargeStepDurationsArray(), getNumChargeStepDurations(), true);
                long timeRemaining2 = computeChargeTimeRemaining(SystemClock.elapsedRealtime());
                if (timeRemaining2 >= 0) {
                    lineArgs2[0] = Long.toString(timeRemaining2);
                    dumpLine(pw, 0, "i", CHARGE_TIME_REMAIN_DATA, lineArgs2);
                }
                dumpCheckinLocked(context, pw, 0, -1, (flags & 32) != 0);
            }
            if (!filtering || (flags & 1) != 0) {
                dumpCheckinLocked(context, pw, 2, -1, (flags & 32) != 0);
            }
        }
    }
}
