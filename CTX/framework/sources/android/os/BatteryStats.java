package android.os;

import android.app.backup.FullBackup;
import android.app.job.JobParameters;
import android.app.slice.Slice;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.hardware.contexthub.V1_0.HostEndPoint;
import android.location.LocationManager;
import android.media.TtmlUtils;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiScanner;
import android.os.SystemProto;
import android.provider.SettingsStringUtil;
import android.provider.Telephony;
import android.telephony.SignalStrength;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.Pair;
import android.util.Printer;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.telephony.PhoneConstants;
import java.io.FileDescriptor;
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
    private static final String AGGREGATED_WAKELOCK_DATA = "awl";
    public static final int AGGREGATED_WAKE_TYPE_PARTIAL = 20;
    private static final String APK_DATA = "apk";
    private static final String AUDIO_DATA = "aud";
    public static final int AUDIO_TURNED_ON = 15;
    private static final String BATTERY_DATA = "bt";
    private static final String BATTERY_DISCHARGE_DATA = "dc";
    private static final String BATTERY_LEVEL_DATA = "lv";
    private static final int BATTERY_STATS_CHECKIN_VERSION = 9;
    private static final String BLUETOOTH_CONTROLLER_DATA = "ble";
    private static final String BLUETOOTH_MISC_DATA = "blem";
    public static final int BLUETOOTH_SCAN_ON = 19;
    public static final int BLUETOOTH_UNOPTIMIZED_SCAN_ON = 21;
    private static final long BYTES_PER_GB = 1073741824;
    private static final long BYTES_PER_KB = 1024;
    private static final long BYTES_PER_MB = 1048576;
    private static final String CAMERA_DATA = "cam";
    public static final int CAMERA_TURNED_ON = 17;
    private static final String CELLULAR_CONTROLLER_NAME = "Cellular";
    private static final String CHARGE_STEP_DATA = "csd";
    private static final String CHARGE_TIME_REMAIN_DATA = "ctr";
    static final int CHECKIN_VERSION = 32;
    private static final String CPU_DATA = "cpu";
    private static final String CPU_TIMES_AT_FREQ_DATA = "ctf";
    private static final String DATA_CONNECTION_COUNT_DATA = "dcc";
    public static final int DATA_CONNECTION_NONE = 0;
    public static final int DATA_CONNECTION_OTHER = 20;
    private static final String DATA_CONNECTION_TIME_DATA = "dct";
    public static final int DEVICE_IDLE_MODE_DEEP = 2;
    public static final int DEVICE_IDLE_MODE_LIGHT = 1;
    public static final int DEVICE_IDLE_MODE_OFF = 0;
    private static final String DISCHARGE_STEP_DATA = "dsd";
    private static final String DISCHARGE_TIME_REMAIN_DATA = "dtr";
    public static final int DUMP_CHARGED_ONLY = 2;
    public static final int DUMP_DAILY_ONLY = 4;
    public static final int DUMP_DEVICE_WIFI_ONLY = 64;
    public static final int DUMP_HISTORY_ONLY = 8;
    public static final int DUMP_INCLUDE_HISTORY = 16;
    public static final int DUMP_VERBOSE = 32;
    private static final String FLASHLIGHT_DATA = "fla";
    public static final int FLASHLIGHT_TURNED_ON = 16;
    public static final int FOREGROUND_ACTIVITY = 10;
    public static final int FOREGROUND_SERVICE = 22;
    private static final String FOREGROUND_SERVICE_DATA = "fgs";
    public static final int FULL_WIFI_LOCK = 5;
    private static final String GLOBAL_BLUETOOTH_CONTROLLER_DATA = "gble";
    private static final String GLOBAL_CPU_FREQ_DATA = "gcf";
    private static final String GLOBAL_MODEM_CONTROLLER_DATA = "gmcd";
    private static final String GLOBAL_NETWORK_DATA = "gn";
    private static final String GLOBAL_WIFI_CONTROLLER_DATA = "gwfcd";
    private static final String GLOBAL_WIFI_DATA = "gwfl";
    private static final String HISTORY_DATA = "h";
    private static final String HISTORY_STRING_POOL = "hsp";
    public static final int JOB = 14;
    private static final String JOBS_DEFERRED_DATA = "jbd";
    private static final String JOB_COMPLETION_DATA = "jbc";
    private static final String JOB_DATA = "jb";
    private static final String KERNEL_WAKELOCK_DATA = "kwl";
    private static final boolean LOCAL_LOGV = false;
    public static final int MAX_TRACKED_SCREEN_STATE = 4;
    private static final String MISC_DATA = "m";
    private static final String MODEM_CONTROLLER_DATA = "mcd";
    public static final int NETWORK_BT_RX_DATA = 4;
    public static final int NETWORK_BT_TX_DATA = 5;
    private static final String NETWORK_DATA = "nt";
    public static final int NETWORK_MOBILE_BG_RX_DATA = 6;
    public static final int NETWORK_MOBILE_BG_TX_DATA = 7;
    public static final int NETWORK_MOBILE_RX_DATA = 0;
    public static final int NETWORK_MOBILE_TX_DATA = 1;
    public static final int NETWORK_WIFI_BG_RX_DATA = 8;
    public static final int NETWORK_WIFI_BG_TX_DATA = 9;
    public static final int NETWORK_WIFI_RX_DATA = 2;
    public static final int NETWORK_WIFI_TX_DATA = 3;
    public static final int NUM_DATA_CONNECTION_TYPES = 21;
    public static final int NUM_NETWORK_ACTIVITY_TYPES = 10;
    public static final int NUM_SCREEN_BRIGHTNESS_BINS = 5;
    public static final int NUM_WIFI_SIGNAL_STRENGTH_BINS = 5;
    public static final int NUM_WIFI_STATES = 8;
    public static final int NUM_WIFI_SUPPL_STATES = 13;
    private static final String POWER_USE_ITEM_DATA = "pwi";
    private static final String POWER_USE_SUMMARY_DATA = "pws";
    private static final String PROCESS_DATA = "pr";
    public static final int PROCESS_STATE = 12;
    private static final String RESOURCE_POWER_MANAGER_DATA = "rpm";
    public static final String RESULT_RECEIVER_CONTROLLER_KEY = "controller_activity";
    public static final int SCREEN_BRIGHTNESS_BRIGHT = 4;
    public static final int SCREEN_BRIGHTNESS_DARK = 0;
    private static final String SCREEN_BRIGHTNESS_DATA = "br";
    public static final int SCREEN_BRIGHTNESS_DIM = 1;
    public static final int SCREEN_BRIGHTNESS_LIGHT = 3;
    public static final int SCREEN_BRIGHTNESS_MEDIUM = 2;
    protected static final boolean SCREEN_OFF_RPM_STATS_ENABLED = false;
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
    public static final int STEP_LEVEL_INITIAL_MODE_SHIFT = 48;
    public static final long STEP_LEVEL_LEVEL_MASK = 280375465082880L;
    public static final int STEP_LEVEL_LEVEL_SHIFT = 40;
    public static final int STEP_LEVEL_MODE_DEVICE_IDLE = 8;
    public static final int STEP_LEVEL_MODE_POWER_SAVE = 4;
    public static final int STEP_LEVEL_MODE_SCREEN_STATE = 3;
    public static final long STEP_LEVEL_MODIFIED_MODE_MASK = -72057594037927936L;
    public static final int STEP_LEVEL_MODIFIED_MODE_SHIFT = 56;
    public static final long STEP_LEVEL_TIME_MASK = 1099511627775L;
    public static final int SYNC = 13;
    private static final String SYNC_DATA = "sy";
    private static final String TAG = "BatteryStats";
    private static final String UID_DATA = "uid";

    @VisibleForTesting
    public static final String UID_TIMES_TYPE_ALL = "A";
    private static final String USER_ACTIVITY_DATA = "ua";
    private static final String VERSION_DATA = "vers";
    private static final String VIBRATOR_DATA = "vib";
    public static final int VIBRATOR_ON = 9;
    private static final String VIDEO_DATA = "vid";
    public static final int VIDEO_TURNED_ON = 8;
    private static final String WAKELOCK_DATA = "wl";
    private static final String WAKEUP_ALARM_DATA = "wua";
    private static final String WAKEUP_REASON_DATA = "wr";
    public static final int WAKE_TYPE_DRAW = 18;
    public static final int WAKE_TYPE_FULL = 1;
    public static final int WAKE_TYPE_PARTIAL = 0;
    public static final int WAKE_TYPE_WINDOW = 2;
    public static final int WIFI_AGGREGATE_MULTICAST_ENABLED = 23;
    public static final int WIFI_BATCHED_SCAN = 11;
    private static final String WIFI_CONTROLLER_DATA = "wfcd";
    private static final String WIFI_CONTROLLER_NAME = "WiFi";
    private static final String WIFI_DATA = "wfl";
    private static final String WIFI_MULTICAST_DATA = "wmc";
    public static final int WIFI_MULTICAST_ENABLED = 7;
    private static final String WIFI_MULTICAST_TOTAL_DATA = "wmct";
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
    public static final long[] JOB_FRESHNESS_BUCKETS = {3600000, 7200000, 14400000, 28800000, Long.MAX_VALUE};
    static final String[] SCREEN_BRIGHTNESS_NAMES = {"dark", "dim", "medium", "light", "bright"};
    static final String[] SCREEN_BRIGHTNESS_SHORT_NAMES = {WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"};
    static final String[] DATA_CONNECTION_NAMES = {"none", "gprs", "edge", "umts", "cdma", "evdo_0", "evdo_A", "1xrtt", "hsdpa", "hsupa", "hspa", "iden", "evdo_b", "lte", "ehrpd", "hspap", "gsm", "td_scdma", "iwlan", "lte_ca", "other"};
    static final String[] WIFI_SUPPL_STATE_NAMES = {"invalid", "disconn", "disabled", "inactive", "scanning", "authenticating", "associating", "associated", "4-way-handshake", "group-handshake", "completed", "dormant", "uninit"};
    static final String[] WIFI_SUPPL_STATE_SHORT_NAMES = {"inv", "dsc", "dis", "inact", "scan", "auth", "ascing", "asced", "4-way", WifiConfiguration.GroupCipher.varName, "compl", "dorm", "uninit"};
    public static final BitDescription[] HISTORY_STATE_DESCRIPTIONS = {new BitDescription(Integer.MIN_VALUE, "running", FullBackup.ROOT_TREE_TOKEN), new BitDescription(1073741824, "wake_lock", "w"), new BitDescription(8388608, Context.SENSOR_SERVICE, "s"), new BitDescription(536870912, LocationManager.GPS_PROVIDER, "g"), new BitDescription(268435456, "wifi_full_lock", "Wl"), new BitDescription(134217728, "wifi_scan", "Ws"), new BitDescription(65536, "wifi_multicast", "Wm"), new BitDescription(67108864, "wifi_radio", "Wr"), new BitDescription(33554432, "mobile_radio", "Pr"), new BitDescription(2097152, "phone_scanning", "Psc"), new BitDescription(4194304, "audio", FullBackup.APK_TREE_TOKEN), new BitDescription(1048576, "screen", "S"), new BitDescription(524288, BatteryManager.EXTRA_PLUGGED, "BP"), new BitDescription(262144, "screen_doze", "Sd"), new BitDescription(HistoryItem.STATE_DATA_CONNECTION_MASK, 9, "data_conn", "Pcn", DATA_CONNECTION_NAMES, DATA_CONNECTION_NAMES), new BitDescription(448, 6, "phone_state", "Pst", new String[]{"in", "out", PhoneConstants.APN_TYPE_EMERGENCY, "off"}, new String[]{"in", "out", "em", "off"}), new BitDescription(56, 3, "phone_signal_strength", "Pss", SignalStrength.SIGNAL_STRENGTH_NAMES, new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"}), new BitDescription(7, 0, "brightness", "Sb", SCREEN_BRIGHTNESS_NAMES, SCREEN_BRIGHTNESS_SHORT_NAMES)};
    public static final BitDescription[] HISTORY_STATE2_DESCRIPTIONS = {new BitDescription(Integer.MIN_VALUE, "power_save", "ps"), new BitDescription(1073741824, "video", Telephony.BaseMmsColumns.MMS_VERSION), new BitDescription(536870912, "wifi_running", "Ww"), new BitDescription(268435456, "wifi", "W"), new BitDescription(134217728, "flashlight", "fl"), new BitDescription(HistoryItem.STATE2_DEVICE_IDLE_MASK, 25, "device_idle", "di", new String[]{"off", "light", "full", "???"}, new String[]{"off", "light", "full", "???"}), new BitDescription(16777216, "charging", "ch"), new BitDescription(262144, "usb_data", "Ud"), new BitDescription(8388608, "phone_in_call", "Pcl"), new BitDescription(4194304, "bluetooth", "b"), new BitDescription(112, 4, "wifi_signal_strength", "Wss", new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"}, new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE, "2", "3", "4"}), new BitDescription(15, 0, "wifi_suppl", "Wsp", WIFI_SUPPL_STATE_NAMES, WIFI_SUPPL_STATE_SHORT_NAMES), new BitDescription(2097152, Context.CAMERA_SERVICE, "ca"), new BitDescription(1048576, "ble_scan", "bles"), new BitDescription(524288, "cellular_high_tx_power", "Chtp"), new BitDescription(128, 7, "gps_signal_quality", "Gss", new String[]{"poor", "good"}, new String[]{"poor", "good"})};
    private static final String FOREGROUND_ACTIVITY_DATA = "fg";
    public static final String[] HISTORY_EVENT_NAMES = {"null", "proc", FOREGROUND_ACTIVITY_DATA, "top", "sync", "wake_lock_in", "job", "user", "userfg", "conn", "active", "pkginst", "pkgunin", "alarm", Context.STATS_MANAGER, "pkginactive", "pkgactive", "tmpwhitelist", "screenwake", "wakeupap", "longwake", "est_capacity"};
    public static final String[] HISTORY_EVENT_CHECKIN_NAMES = {"Enl", "Epr", "Efg", "Etp", "Esy", "Ewl", "Ejb", "Eur", "Euf", "Ecn", "Eac", "Epi", "Epu", "Eal", "Est", "Eai", "Eaa", "Etw", "Esw", "Ewa", "Elw", "Eec"};
    private static final IntToString sUidToString = new IntToString() {
        @Override
        public final String applyAsString(int i) {
            return UserHandle.formatUid(i);
        }
    };
    private static final IntToString sIntToString = new IntToString() {
        @Override
        public final String applyAsString(int i) {
            return Integer.toString(i);
        }
    };
    public static final IntToString[] HISTORY_EVENT_INT_FORMATTERS = {sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sIntToString};
    static final String[] WIFI_STATE_NAMES = {"off", "scanning", "no_net", "disconn", "sta", "p2p", "sta_p2p", "soft_ap"};
    public static final int[] STEP_LEVEL_MODES_OF_INTEREST = {7, 15, 11, 7, 7, 7, 7, 7, 15, 11};
    public static final int[] STEP_LEVEL_MODE_VALUES = {0, 4, 8, 1, 5, 2, 6, 3, 7, 11};
    public static final String[] STEP_LEVEL_MODE_LABELS = {"screen off", "screen off power save", "screen off device idle", "screen on", "screen on power save", "screen doze", "screen doze power save", "screen doze-suspend", "screen doze-suspend power save", "screen doze-suspend device idle"};

    public static abstract class ControllerActivityCounter {
        public abstract LongCounter getIdleTimeCounter();

        public abstract LongCounter getPowerCounter();

        public abstract LongCounter getRxTimeCounter();

        public abstract LongCounter getScanTimeCounter();

        public abstract LongCounter getSleepTimeCounter();

        public abstract LongCounter[] getTxTimeCounters();
    }

    public static abstract class Counter {
        public abstract int getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static final class DailyItem {
        public LevelStepTracker mChargeSteps;
        public LevelStepTracker mDischargeSteps;
        public long mEndTime;
        public ArrayList<PackageChange> mPackageChanges;
        public long mStartTime;
    }

    @FunctionalInterface
    public interface IntToString {
        String applyAsString(int i);
    }

    public static abstract class LongCounter {
        public abstract long getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static abstract class LongCounterArray {
        public abstract long[] getCountsLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static final class PackageChange {
        public String mPackageName;
        public boolean mUpdate;
        public long mVersionCode;
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

    public abstract ControllerActivityCounter getBluetoothControllerActivity();

    public abstract long getBluetoothScanTime(long j, int i);

    public abstract long getCameraOnTime(long j, int i);

    public abstract LevelStepTracker getChargeLevelStepTracker();

    public abstract long[] getCpuFreqs();

    public abstract long getCurrentDailyStartTime();

    public abstract LevelStepTracker getDailyChargeLevelStepTracker();

    public abstract LevelStepTracker getDailyDischargeLevelStepTracker();

    public abstract DailyItem getDailyItemLocked(int i);

    public abstract ArrayList<PackageChange> getDailyPackageChanges();

    public abstract int getDeviceIdleModeCount(int i, int i2);

    public abstract long getDeviceIdleModeTime(int i, long j, int i2);

    public abstract int getDeviceIdlingCount(int i, int i2);

    public abstract long getDeviceIdlingTime(int i, long j, int i2);

    public abstract int getDischargeAmount(int i);

    public abstract int getDischargeAmountScreenDoze();

    public abstract int getDischargeAmountScreenDozeSinceCharge();

    public abstract int getDischargeAmountScreenOff();

    public abstract int getDischargeAmountScreenOffSinceCharge();

    public abstract int getDischargeAmountScreenOn();

    public abstract int getDischargeAmountScreenOnSinceCharge();

    public abstract int getDischargeCurrentLevel();

    public abstract LevelStepTracker getDischargeLevelStepTracker();

    public abstract int getDischargeStartLevel();

    public abstract String getEndPlatformVersion();

    public abstract int getEstimatedBatteryCapacity();

    public abstract long getFlashlightOnCount(int i);

    public abstract long getFlashlightOnTime(long j, int i);

    public abstract long getGlobalWifiRunningTime(long j, int i);

    public abstract long getGpsBatteryDrainMaMs();

    public abstract long getGpsSignalQualityTime(int i, long j, int i2);

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

    public abstract LongSparseArray<? extends Timer> getKernelMemoryStats();

    public abstract Map<String, ? extends Timer> getKernelWakelockStats();

    public abstract long getLongestDeviceIdleModeTime(int i);

    public abstract int getLowDischargeAmountSinceCharge();

    public abstract int getMaxLearnedBatteryCapacity();

    public abstract int getMinLearnedBatteryCapacity();

    public abstract long getMobileRadioActiveAdjustedTime(int i);

    public abstract int getMobileRadioActiveCount(int i);

    public abstract long getMobileRadioActiveTime(long j, int i);

    public abstract int getMobileRadioActiveUnknownCount(int i);

    public abstract long getMobileRadioActiveUnknownTime(int i);

    public abstract ControllerActivityCounter getModemControllerActivity();

    public abstract long getNetworkActivityBytes(int i, int i2);

    public abstract long getNetworkActivityPackets(int i, int i2);

    public abstract boolean getNextHistoryLocked(HistoryItem historyItem);

    public abstract long getNextMaxDailyDeadline();

    public abstract long getNextMinDailyDeadline();

    public abstract boolean getNextOldHistoryLocked(HistoryItem historyItem);

    public abstract int getNumConnectivityChange(int i);

    public abstract int getParcelVersion();

    public abstract int getPhoneDataConnectionCount(int i, int i2);

    public abstract long getPhoneDataConnectionTime(int i, long j, int i2);

    public abstract Timer getPhoneDataConnectionTimer(int i);

    public abstract int getPhoneOnCount(int i);

    public abstract long getPhoneOnTime(long j, int i);

    public abstract long getPhoneSignalScanningTime(long j, int i);

    public abstract Timer getPhoneSignalScanningTimer();

    public abstract int getPhoneSignalStrengthCount(int i, int i2);

    public abstract long getPhoneSignalStrengthTime(int i, long j, int i2);

    protected abstract Timer getPhoneSignalStrengthTimer(int i);

    public abstract int getPowerSaveModeEnabledCount(int i);

    public abstract long getPowerSaveModeEnabledTime(long j, int i);

    public abstract Map<String, ? extends Timer> getRpmStats();

    public abstract long getScreenBrightnessTime(int i, long j, int i2);

    public abstract Timer getScreenBrightnessTimer(int i);

    public abstract int getScreenDozeCount(int i);

    public abstract long getScreenDozeTime(long j, int i);

    public abstract Map<String, ? extends Timer> getScreenOffRpmStats();

    public abstract int getScreenOnCount(int i);

    public abstract long getScreenOnTime(long j, int i);

    public abstract long getStartClockTime();

    public abstract int getStartCount();

    public abstract String getStartPlatformVersion();

    public abstract long getUahDischarge(int i);

    public abstract long getUahDischargeDeepDoze(int i);

    public abstract long getUahDischargeLightDoze(int i);

    public abstract long getUahDischargeScreenDoze(int i);

    public abstract long getUahDischargeScreenOff(int i);

    public abstract SparseArray<? extends Uid> getUidStats();

    public abstract Map<String, ? extends Timer> getWakeupReasonStats();

    public abstract long getWifiActiveTime(long j, int i);

    public abstract ControllerActivityCounter getWifiControllerActivity();

    public abstract int getWifiMulticastWakelockCount(int i);

    public abstract long getWifiMulticastWakelockTime(long j, int i);

    public abstract long getWifiOnTime(long j, int i);

    public abstract int getWifiSignalStrengthCount(int i, int i2);

    public abstract long getWifiSignalStrengthTime(int i, long j, int i2);

    public abstract Timer getWifiSignalStrengthTimer(int i);

    public abstract int getWifiStateCount(int i, int i2);

    public abstract long getWifiStateTime(int i, long j, int i2);

    public abstract Timer getWifiStateTimer(int i);

    public abstract int getWifiSupplStateCount(int i, int i2);

    public abstract long getWifiSupplStateTime(int i, long j, int i2);

    public abstract Timer getWifiSupplStateTimer(int i);

    public abstract boolean hasBluetoothActivityReporting();

    public abstract boolean hasModemActivityReporting();

    public abstract boolean hasWifiActivityReporting();

    public abstract boolean startIteratingHistoryLocked();

    public abstract boolean startIteratingOldHistoryLocked();

    public abstract void writeToParcelWithoutUids(Parcel parcel, int i);

    public static abstract class Timer {
        public abstract int getCountLocked(int i);

        public abstract long getTimeSinceMarkLocked(long j);

        public abstract long getTotalTimeLocked(long j, int i);

        public abstract void logState(Printer printer, String str);

        public long getMaxDurationMsLocked(long j) {
            return -1L;
        }

        public long getCurrentDurationMsLocked(long j) {
            return -1L;
        }

        public long getTotalDurationMsLocked(long j) {
            return -1L;
        }

        public Timer getSubTimer() {
            return null;
        }

        public boolean isRunningLocked() {
            return false;
        }
    }

    public static int mapToInternalProcessState(int i) {
        if (i == 19) {
            return 19;
        }
        if (i == 2) {
            return 0;
        }
        if (i == 3) {
            return 1;
        }
        if (i <= 5) {
            return 2;
        }
        if (i <= 10) {
            return 3;
        }
        if (i <= 11) {
            return 4;
        }
        if (i <= 12) {
            return 5;
        }
        return 6;
    }

    public static abstract class Uid {
        public static final int NUM_PROCESS_STATE = 7;
        public static final int NUM_USER_ACTIVITY_TYPES = 4;
        public static final int NUM_WIFI_BATCHED_SCAN_BINS = 5;
        public static final int PROCESS_STATE_BACKGROUND = 3;
        public static final int PROCESS_STATE_CACHED = 6;
        public static final int PROCESS_STATE_FOREGROUND = 2;
        public static final int PROCESS_STATE_FOREGROUND_SERVICE = 1;
        public static final int PROCESS_STATE_HEAVY_WEIGHT = 5;
        public static final int PROCESS_STATE_TOP = 0;
        public static final int PROCESS_STATE_TOP_SLEEPING = 4;
        static final String[] PROCESS_STATE_NAMES = {"Top", "Fg Service", "Foreground", "Background", "Top Sleeping", "Heavy Weight", "Cached"};

        @VisibleForTesting
        public static final String[] UID_PROCESS_TYPES = {"T", "FS", "F", "B", "TS", "HW", "C"};
        public static final int[] CRITICAL_PROC_STATES = {0, 1, 2};
        static final String[] USER_ACTIVITY_TYPES = {"other", "button", "touch", Context.ACCESSIBILITY_SERVICE};

        public static abstract class Pkg {

            public static abstract class Serv {
                public abstract int getLaunches(int i);

                public abstract long getStartTime(long j, int i);

                public abstract int getStarts(int i);
            }

            public abstract ArrayMap<String, ? extends Serv> getServiceStats();

            public abstract ArrayMap<String, ? extends Counter> getWakeupAlarmStats();
        }

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

            public abstract long getUserTime(int i);

            public abstract boolean isActive();
        }

        public static abstract class Sensor {
            public static final int GPS = -10000;

            public abstract int getHandle();

            public abstract Timer getSensorBackgroundTime();

            public abstract Timer getSensorTime();
        }

        public static abstract class Wakelock {
            public abstract Timer getWakeTime(int i);
        }

        public abstract Timer getAggregatedPartialWakelockTimer();

        public abstract Timer getAudioTurnedOnTimer();

        public abstract ControllerActivityCounter getBluetoothControllerActivity();

        public abstract Timer getBluetoothScanBackgroundTimer();

        public abstract Counter getBluetoothScanResultBgCounter();

        public abstract Counter getBluetoothScanResultCounter();

        public abstract Timer getBluetoothScanTimer();

        public abstract Timer getBluetoothUnoptimizedScanBackgroundTimer();

        public abstract Timer getBluetoothUnoptimizedScanTimer();

        public abstract Timer getCameraTurnedOnTimer();

        public abstract long getCpuActiveTime();

        public abstract long[] getCpuClusterTimes();

        public abstract long[] getCpuFreqTimes(int i);

        public abstract long[] getCpuFreqTimes(int i, int i2);

        public abstract void getDeferredJobsCheckinLineLocked(StringBuilder sb, int i);

        public abstract void getDeferredJobsLineLocked(StringBuilder sb, int i);

        public abstract Timer getFlashlightTurnedOnTimer();

        public abstract Timer getForegroundActivityTimer();

        public abstract Timer getForegroundServiceTimer();

        public abstract long getFullWifiLockTime(long j, int i);

        public abstract ArrayMap<String, SparseIntArray> getJobCompletionStats();

        public abstract ArrayMap<String, ? extends Timer> getJobStats();

        public abstract int getMobileRadioActiveCount(int i);

        public abstract long getMobileRadioActiveTime(int i);

        public abstract long getMobileRadioApWakeupCount(int i);

        public abstract ControllerActivityCounter getModemControllerActivity();

        public abstract Timer getMulticastWakelockStats();

        public abstract long getNetworkActivityBytes(int i, int i2);

        public abstract long getNetworkActivityPackets(int i, int i2);

        public abstract ArrayMap<String, ? extends Pkg> getPackageStats();

        public abstract SparseArray<? extends Pid> getPidStats();

        public abstract long getProcessStateTime(int i, long j, int i2);

        public abstract Timer getProcessStateTimer(int i);

        public abstract ArrayMap<String, ? extends Proc> getProcessStats();

        public abstract long[] getScreenOffCpuFreqTimes(int i);

        public abstract long[] getScreenOffCpuFreqTimes(int i, int i2);

        public abstract SparseArray<? extends Sensor> getSensorStats();

        public abstract ArrayMap<String, ? extends Timer> getSyncStats();

        public abstract long getSystemCpuTimeUs(int i);

        public abstract long getTimeAtCpuSpeed(int i, int i2, int i3);

        public abstract int getUid();

        public abstract int getUserActivityCount(int i, int i2);

        public abstract long getUserCpuTimeUs(int i);

        public abstract Timer getVibratorOnTimer();

        public abstract Timer getVideoTurnedOnTimer();

        public abstract ArrayMap<String, ? extends Wakelock> getWakelockStats();

        public abstract int getWifiBatchedScanCount(int i, int i2);

        public abstract long getWifiBatchedScanTime(int i, long j, int i2);

        public abstract ControllerActivityCounter getWifiControllerActivity();

        public abstract long getWifiMulticastTime(long j, int i);

        public abstract long getWifiRadioApWakeupCount(int i);

        public abstract long getWifiRunningTime(long j, int i);

        public abstract long getWifiScanActualTime(long j);

        public abstract int getWifiScanBackgroundCount(int i);

        public abstract long getWifiScanBackgroundTime(long j);

        public abstract Timer getWifiScanBackgroundTimer();

        public abstract int getWifiScanCount(int i);

        public abstract long getWifiScanTime(long j, int i);

        public abstract Timer getWifiScanTimer();

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
    }

    public static final class LevelStepTracker {
        public long mLastStepTime = -1;
        public int mNumStepDurations;
        public final long[] mStepDurations;

        public LevelStepTracker(int i) {
            this.mStepDurations = new long[i];
        }

        public LevelStepTracker(int i, long[] jArr) {
            this.mNumStepDurations = i;
            this.mStepDurations = new long[i];
            System.arraycopy(jArr, 0, this.mStepDurations, 0, i);
        }

        public long getDurationAt(int i) {
            return this.mStepDurations[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
        }

        public int getLevelAt(int i) {
            return (int) ((this.mStepDurations[i] & BatteryStats.STEP_LEVEL_LEVEL_MASK) >> 40);
        }

        public int getInitModeAt(int i) {
            return (int) ((this.mStepDurations[i] & BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK) >> 48);
        }

        public int getModModeAt(int i) {
            return (int) ((this.mStepDurations[i] & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56);
        }

        private void appendHex(long j, int i, StringBuilder sb) {
            boolean z = false;
            while (i >= 0) {
                int i2 = (int) ((j >> i) & 15);
                i -= 4;
                if (z || i2 != 0) {
                    z = true;
                    if (i2 >= 0 && i2 <= 9) {
                        sb.append((char) (48 + i2));
                    } else {
                        sb.append((char) ((97 + i2) - 10));
                    }
                }
            }
        }

        public void encodeEntryAt(int i, StringBuilder sb) {
            long j = this.mStepDurations[i];
            long j2 = BatteryStats.STEP_LEVEL_TIME_MASK & j;
            int i2 = (int) ((BatteryStats.STEP_LEVEL_LEVEL_MASK & j) >> 40);
            int i3 = (int) ((BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK & j) >> 48);
            int i4 = (int) ((j & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56);
            switch ((i3 & 3) + 1) {
                case 1:
                    sb.append('f');
                    break;
                case 2:
                    sb.append('o');
                    break;
                case 3:
                    sb.append(DateFormat.DATE);
                    break;
                case 4:
                    sb.append(DateFormat.TIME_ZONE);
                    break;
            }
            if ((i3 & 4) != 0) {
                sb.append('p');
            }
            if ((i3 & 8) != 0) {
                sb.append('i');
            }
            switch ((i4 & 3) + 1) {
                case 1:
                    sb.append('F');
                    break;
                case 2:
                    sb.append('O');
                    break;
                case 3:
                    sb.append('D');
                    break;
                case 4:
                    sb.append('Z');
                    break;
            }
            if ((i4 & 4) != 0) {
                sb.append('P');
            }
            if ((i4 & 8) != 0) {
                sb.append('I');
            }
            sb.append('-');
            appendHex(i2, 4, sb);
            sb.append('-');
            appendHex(j2, 36, sb);
        }

        public void decodeEntryAt(int i, String str) {
            char cCharAt;
            char cCharAt2;
            int length = str.length();
            int i2 = 0;
            long j = 0;
            while (i2 < length && (cCharAt2 = str.charAt(i2)) != '-') {
                i2++;
                switch (cCharAt2) {
                    case 'D':
                        j |= 144115188075855872L;
                        break;
                    case 'F':
                        j |= 0;
                        break;
                    case 'I':
                        j |= 576460752303423488L;
                        break;
                    case 'O':
                        j |= 72057594037927936L;
                        break;
                    case 'P':
                        j |= 288230376151711744L;
                        break;
                    case 'Z':
                        j |= 216172782113783808L;
                        break;
                    case 'd':
                        j |= 562949953421312L;
                        break;
                    case 'f':
                        j |= 0;
                        break;
                    case 'i':
                        j |= 2251799813685248L;
                        break;
                    case 'o':
                        j |= 281474976710656L;
                        break;
                    case 'p':
                        j |= TrafficStats.PB_IN_BYTES;
                        break;
                    case 'z':
                        j |= 844424930131968L;
                        break;
                }
            }
            int i3 = i2 + 1;
            long j2 = 0;
            while (i3 < length && (cCharAt = str.charAt(i3)) != '-') {
                i3++;
                j2 <<= 4;
                if (cCharAt >= '0' && cCharAt <= '9') {
                    j2 += (long) (cCharAt - '0');
                } else if (cCharAt >= 'a' && cCharAt <= 'f') {
                    j2 += (long) ((cCharAt - 'a') + 10);
                } else if (cCharAt >= 'A' && cCharAt <= 'F') {
                    j2 += (long) ((cCharAt - 'A') + 10);
                }
            }
            int i4 = i3 + 1;
            long j3 = j | ((j2 << 40) & BatteryStats.STEP_LEVEL_LEVEL_MASK);
            long j4 = 0;
            while (i4 < length) {
                char cCharAt3 = str.charAt(i4);
                if (cCharAt3 != '-') {
                    i4++;
                    j4 <<= 4;
                    if (cCharAt3 >= '0' && cCharAt3 <= '9') {
                        j4 += (long) (cCharAt3 - '0');
                    } else if (cCharAt3 >= 'a' && cCharAt3 <= 'f') {
                        j4 += (long) ((cCharAt3 - 'a') + 10);
                    } else if (cCharAt3 >= 'A' && cCharAt3 <= 'F') {
                        j4 += (long) ((cCharAt3 - 'A') + 10);
                    }
                } else {
                    this.mStepDurations[i] = (j4 & BatteryStats.STEP_LEVEL_TIME_MASK) | j3;
                }
            }
            this.mStepDurations[i] = (j4 & BatteryStats.STEP_LEVEL_TIME_MASK) | j3;
        }

        public void init() {
            this.mLastStepTime = -1L;
            this.mNumStepDurations = 0;
        }

        public void clearTime() {
            this.mLastStepTime = -1L;
        }

        public long computeTimePerLevel() {
            long[] jArr = this.mStepDurations;
            int i = this.mNumStepDurations;
            if (i <= 0) {
                return -1L;
            }
            long j = 0;
            for (int i2 = 0; i2 < i; i2++) {
                j += jArr[i2] & BatteryStats.STEP_LEVEL_TIME_MASK;
            }
            return j / ((long) i);
        }

        public long computeTimeEstimate(long j, long j2, int[] iArr) {
            long[] jArr = this.mStepDurations;
            int i = this.mNumStepDurations;
            if (i <= 0) {
                return -1L;
            }
            long j3 = 0;
            int i2 = 0;
            for (int i3 = 0; i3 < i; i3++) {
                long j4 = (jArr[i3] & BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK) >> 48;
                if ((((jArr[i3] & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56) & j) == 0 && (j4 & j) == j2) {
                    i2++;
                    j3 += jArr[i3] & BatteryStats.STEP_LEVEL_TIME_MASK;
                }
            }
            if (i2 <= 0) {
                return -1L;
            }
            if (iArr != null) {
                iArr[0] = i2;
            }
            return (j3 / ((long) i2)) * 100;
        }

        public void addLevelSteps(int i, long j, long j2) {
            int length = this.mNumStepDurations;
            long j3 = this.mLastStepTime;
            if (j3 >= 0 && i > 0) {
                long[] jArr = this.mStepDurations;
                long j4 = j2 - j3;
                for (int i2 = 0; i2 < i; i2++) {
                    System.arraycopy(jArr, 0, jArr, 1, jArr.length - 1);
                    long j5 = j4 / ((long) (i - i2));
                    j4 -= j5;
                    if (j5 > BatteryStats.STEP_LEVEL_TIME_MASK) {
                        j5 = 1099511627775L;
                    }
                    jArr[0] = j5 | j;
                }
                length += i;
                if (length > jArr.length) {
                    length = jArr.length;
                }
            }
            this.mNumStepDurations = length;
            this.mLastStepTime = j2;
        }

        public void readFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            if (i > this.mStepDurations.length) {
                throw new ParcelFormatException("more step durations than available: " + i);
            }
            this.mNumStepDurations = i;
            for (int i2 = 0; i2 < i; i2++) {
                this.mStepDurations[i2] = parcel.readLong();
            }
        }

        public void writeToParcel(Parcel parcel) {
            int i = this.mNumStepDurations;
            parcel.writeInt(i);
            for (int i2 = 0; i2 < i; i2++) {
                parcel.writeLong(this.mStepDurations[i2]);
            }
        }
    }

    public static final class HistoryTag {
        public int poolIdx;
        public String string;
        public int uid;

        public void setTo(HistoryTag historyTag) {
            this.string = historyTag.string;
            this.uid = historyTag.uid;
            this.poolIdx = historyTag.poolIdx;
        }

        public void setTo(String str, int i) {
            this.string = str;
            this.uid = i;
            this.poolIdx = -1;
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.string);
            parcel.writeInt(this.uid);
        }

        public void readFromParcel(Parcel parcel) {
            this.string = parcel.readString();
            this.uid = parcel.readInt();
            this.poolIdx = -1;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoryTag historyTag = (HistoryTag) obj;
            if (this.uid == historyTag.uid && this.string.equals(historyTag.string)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.string.hashCode()) + this.uid;
        }
    }

    public static final class HistoryStepDetails {
        public int appCpuSTime1;
        public int appCpuSTime2;
        public int appCpuSTime3;
        public int appCpuUTime1;
        public int appCpuUTime2;
        public int appCpuUTime3;
        public int appCpuUid1;
        public int appCpuUid2;
        public int appCpuUid3;
        public int statIOWaitTime;
        public int statIdlTime;
        public int statIrqTime;
        public String statPlatformIdleState;
        public int statSoftIrqTime;
        public String statSubsystemPowerState;
        public int statSystemTime;
        public int statUserTime;
        public int systemTime;
        public int userTime;

        public HistoryStepDetails() {
            clear();
        }

        public void clear() {
            this.systemTime = 0;
            this.userTime = 0;
            this.appCpuUid3 = -1;
            this.appCpuUid2 = -1;
            this.appCpuUid1 = -1;
            this.appCpuSTime3 = 0;
            this.appCpuUTime3 = 0;
            this.appCpuSTime2 = 0;
            this.appCpuUTime2 = 0;
            this.appCpuSTime1 = 0;
            this.appCpuUTime1 = 0;
        }

        public void writeToParcel(Parcel parcel) {
            parcel.writeInt(this.userTime);
            parcel.writeInt(this.systemTime);
            parcel.writeInt(this.appCpuUid1);
            parcel.writeInt(this.appCpuUTime1);
            parcel.writeInt(this.appCpuSTime1);
            parcel.writeInt(this.appCpuUid2);
            parcel.writeInt(this.appCpuUTime2);
            parcel.writeInt(this.appCpuSTime2);
            parcel.writeInt(this.appCpuUid3);
            parcel.writeInt(this.appCpuUTime3);
            parcel.writeInt(this.appCpuSTime3);
            parcel.writeInt(this.statUserTime);
            parcel.writeInt(this.statSystemTime);
            parcel.writeInt(this.statIOWaitTime);
            parcel.writeInt(this.statIrqTime);
            parcel.writeInt(this.statSoftIrqTime);
            parcel.writeInt(this.statIdlTime);
            parcel.writeString(this.statPlatformIdleState);
            parcel.writeString(this.statSubsystemPowerState);
        }

        public void readFromParcel(Parcel parcel) {
            this.userTime = parcel.readInt();
            this.systemTime = parcel.readInt();
            this.appCpuUid1 = parcel.readInt();
            this.appCpuUTime1 = parcel.readInt();
            this.appCpuSTime1 = parcel.readInt();
            this.appCpuUid2 = parcel.readInt();
            this.appCpuUTime2 = parcel.readInt();
            this.appCpuSTime2 = parcel.readInt();
            this.appCpuUid3 = parcel.readInt();
            this.appCpuUTime3 = parcel.readInt();
            this.appCpuSTime3 = parcel.readInt();
            this.statUserTime = parcel.readInt();
            this.statSystemTime = parcel.readInt();
            this.statIOWaitTime = parcel.readInt();
            this.statIrqTime = parcel.readInt();
            this.statSoftIrqTime = parcel.readInt();
            this.statIdlTime = parcel.readInt();
            this.statPlatformIdleState = parcel.readString();
            this.statSubsystemPowerState = parcel.readString();
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
        public static final int EVENT_ACTIVE = 10;
        public static final int EVENT_ALARM = 13;
        public static final int EVENT_ALARM_FINISH = 16397;
        public static final int EVENT_ALARM_START = 32781;
        public static final int EVENT_COLLECT_EXTERNAL_STATS = 14;
        public static final int EVENT_CONNECTIVITY_CHANGED = 9;
        public static final int EVENT_COUNT = 22;
        public static final int EVENT_FLAG_FINISH = 16384;
        public static final int EVENT_FLAG_START = 32768;
        public static final int EVENT_FOREGROUND = 2;
        public static final int EVENT_FOREGROUND_FINISH = 16386;
        public static final int EVENT_FOREGROUND_START = 32770;
        public static final int EVENT_JOB = 6;
        public static final int EVENT_JOB_FINISH = 16390;
        public static final int EVENT_JOB_START = 32774;
        public static final int EVENT_LONG_WAKE_LOCK = 20;
        public static final int EVENT_LONG_WAKE_LOCK_FINISH = 16404;
        public static final int EVENT_LONG_WAKE_LOCK_START = 32788;
        public static final int EVENT_NONE = 0;
        public static final int EVENT_PACKAGE_ACTIVE = 16;
        public static final int EVENT_PACKAGE_INACTIVE = 15;
        public static final int EVENT_PACKAGE_INSTALLED = 11;
        public static final int EVENT_PACKAGE_UNINSTALLED = 12;
        public static final int EVENT_PROC = 1;
        public static final int EVENT_PROC_FINISH = 16385;
        public static final int EVENT_PROC_START = 32769;
        public static final int EVENT_SCREEN_WAKE_UP = 18;
        public static final int EVENT_SYNC = 4;
        public static final int EVENT_SYNC_FINISH = 16388;
        public static final int EVENT_SYNC_START = 32772;
        public static final int EVENT_TEMP_WHITELIST = 17;
        public static final int EVENT_TEMP_WHITELIST_FINISH = 16401;
        public static final int EVENT_TEMP_WHITELIST_START = 32785;
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
        public static final int EVENT_WAKEUP_AP = 19;
        public static final int EVENT_WAKE_LOCK = 5;
        public static final int EVENT_WAKE_LOCK_FINISH = 16389;
        public static final int EVENT_WAKE_LOCK_START = 32773;
        public static final int MOST_INTERESTING_STATES = 1835008;
        public static final int MOST_INTERESTING_STATES2 = -1749024768;
        public static final int SETTLE_TO_ZERO_STATES = -1900544;
        public static final int SETTLE_TO_ZERO_STATES2 = 1748959232;
        public static final int STATE2_BLUETOOTH_ON_FLAG = 4194304;
        public static final int STATE2_BLUETOOTH_SCAN_FLAG = 1048576;
        public static final int STATE2_CAMERA_FLAG = 2097152;
        public static final int STATE2_CELLULAR_HIGH_TX_POWER_FLAG = 524288;
        public static final int STATE2_CHARGING_FLAG = 16777216;
        public static final int STATE2_DEVICE_IDLE_MASK = 100663296;
        public static final int STATE2_DEVICE_IDLE_SHIFT = 25;
        public static final int STATE2_FLASHLIGHT_FLAG = 134217728;
        public static final int STATE2_GPS_SIGNAL_QUALITY_MASK = 128;
        public static final int STATE2_GPS_SIGNAL_QUALITY_SHIFT = 7;
        public static final int STATE2_PHONE_IN_CALL_FLAG = 8388608;
        public static final int STATE2_POWER_SAVE_FLAG = Integer.MIN_VALUE;
        public static final int STATE2_USB_DATA_LINK_FLAG = 262144;
        public static final int STATE2_VIDEO_ON_FLAG = 1073741824;
        public static final int STATE2_WIFI_ON_FLAG = 268435456;
        public static final int STATE2_WIFI_RUNNING_FLAG = 536870912;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_MASK = 112;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_SHIFT = 4;
        public static final int STATE2_WIFI_SUPPL_STATE_MASK = 15;
        public static final int STATE2_WIFI_SUPPL_STATE_SHIFT = 0;
        public static final int STATE_AUDIO_ON_FLAG = 4194304;
        public static final int STATE_BATTERY_PLUGGED_FLAG = 524288;
        public static final int STATE_BRIGHTNESS_MASK = 7;
        public static final int STATE_BRIGHTNESS_SHIFT = 0;
        public static final int STATE_CPU_RUNNING_FLAG = Integer.MIN_VALUE;
        public static final int STATE_DATA_CONNECTION_MASK = 15872;
        public static final int STATE_DATA_CONNECTION_SHIFT = 9;
        public static final int STATE_GPS_ON_FLAG = 536870912;
        public static final int STATE_MOBILE_RADIO_ACTIVE_FLAG = 33554432;
        public static final int STATE_PHONE_SCANNING_FLAG = 2097152;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_MASK = 56;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_SHIFT = 3;
        public static final int STATE_PHONE_STATE_MASK = 448;
        public static final int STATE_PHONE_STATE_SHIFT = 6;
        private static final int STATE_RESERVED_0 = 16777216;
        public static final int STATE_SCREEN_DOZE_FLAG = 262144;
        public static final int STATE_SCREEN_ON_FLAG = 1048576;
        public static final int STATE_SENSOR_ON_FLAG = 8388608;
        public static final int STATE_WAKE_LOCK_FLAG = 1073741824;
        public static final int STATE_WIFI_FULL_LOCK_FLAG = 268435456;
        public static final int STATE_WIFI_MULTICAST_ON_FLAG = 65536;
        public static final int STATE_WIFI_RADIO_ACTIVE_FLAG = 67108864;
        public static final int STATE_WIFI_SCAN_FLAG = 134217728;
        public int batteryChargeUAh;
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
        public HistoryStepDetails stepDetails;
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

        public HistoryItem(long j, Parcel parcel) {
            this.cmd = (byte) -1;
            this.localWakelockTag = new HistoryTag();
            this.localWakeReasonTag = new HistoryTag();
            this.localEventTag = new HistoryTag();
            this.time = j;
            this.numReadInts = 2;
            readFromParcel(parcel);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.time);
            parcel.writeInt((this.cmd & 255) | ((this.batteryLevel << 8) & 65280) | ((this.batteryStatus << WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK) & SurfaceControl.FX_SURFACE_MASK) | ((this.batteryHealth << 20) & 15728640) | ((this.batteryPlugType << 24) & 251658240) | (this.wakelockTag != null ? 268435456 : 0) | (this.wakeReasonTag != null ? 536870912 : 0) | (this.eventCode != 0 ? 1073741824 : 0));
            parcel.writeInt((this.batteryTemperature & HostEndPoint.BROADCAST) | ((this.batteryVoltage << 16) & (-65536)));
            parcel.writeInt(this.batteryChargeUAh);
            parcel.writeInt(this.states);
            parcel.writeInt(this.states2);
            if (this.wakelockTag != null) {
                this.wakelockTag.writeToParcel(parcel, i);
            }
            if (this.wakeReasonTag != null) {
                this.wakeReasonTag.writeToParcel(parcel, i);
            }
            if (this.eventCode != 0) {
                parcel.writeInt(this.eventCode);
                this.eventTag.writeToParcel(parcel, i);
            }
            if (this.cmd == 5 || this.cmd == 7) {
                parcel.writeLong(this.currentTime);
            }
        }

        public void readFromParcel(Parcel parcel) {
            int iDataPosition = parcel.dataPosition();
            int i = parcel.readInt();
            this.cmd = (byte) (i & 255);
            this.batteryLevel = (byte) ((i >> 8) & 255);
            this.batteryStatus = (byte) ((i >> 16) & 15);
            this.batteryHealth = (byte) ((i >> 20) & 15);
            this.batteryPlugType = (byte) ((i >> 24) & 15);
            int i2 = parcel.readInt();
            this.batteryTemperature = (short) (i2 & 65535);
            this.batteryVoltage = (char) ((i2 >> 16) & 65535);
            this.batteryChargeUAh = parcel.readInt();
            this.states = parcel.readInt();
            this.states2 = parcel.readInt();
            if ((268435456 & i) != 0) {
                this.wakelockTag = this.localWakelockTag;
                this.wakelockTag.readFromParcel(parcel);
            } else {
                this.wakelockTag = null;
            }
            if ((536870912 & i) != 0) {
                this.wakeReasonTag = this.localWakeReasonTag;
                this.wakeReasonTag.readFromParcel(parcel);
            } else {
                this.wakeReasonTag = null;
            }
            if ((i & 1073741824) != 0) {
                this.eventCode = parcel.readInt();
                this.eventTag = this.localEventTag;
                this.eventTag.readFromParcel(parcel);
            } else {
                this.eventCode = 0;
                this.eventTag = null;
            }
            if (this.cmd == 5 || this.cmd == 7) {
                this.currentTime = parcel.readLong();
            } else {
                this.currentTime = 0L;
            }
            this.numReadInts += (parcel.dataPosition() - iDataPosition) / 4;
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
            this.batteryChargeUAh = 0;
            this.states = 0;
            this.states2 = 0;
            this.wakelockTag = null;
            this.wakeReasonTag = null;
            this.eventCode = 0;
            this.eventTag = null;
        }

        public void setTo(HistoryItem historyItem) {
            this.time = historyItem.time;
            this.cmd = historyItem.cmd;
            setToCommon(historyItem);
        }

        public void setTo(long j, byte b, HistoryItem historyItem) {
            this.time = j;
            this.cmd = b;
            setToCommon(historyItem);
        }

        private void setToCommon(HistoryItem historyItem) {
            this.batteryLevel = historyItem.batteryLevel;
            this.batteryStatus = historyItem.batteryStatus;
            this.batteryHealth = historyItem.batteryHealth;
            this.batteryPlugType = historyItem.batteryPlugType;
            this.batteryTemperature = historyItem.batteryTemperature;
            this.batteryVoltage = historyItem.batteryVoltage;
            this.batteryChargeUAh = historyItem.batteryChargeUAh;
            this.states = historyItem.states;
            this.states2 = historyItem.states2;
            if (historyItem.wakelockTag != null) {
                this.wakelockTag = this.localWakelockTag;
                this.wakelockTag.setTo(historyItem.wakelockTag);
            } else {
                this.wakelockTag = null;
            }
            if (historyItem.wakeReasonTag != null) {
                this.wakeReasonTag = this.localWakeReasonTag;
                this.wakeReasonTag.setTo(historyItem.wakeReasonTag);
            } else {
                this.wakeReasonTag = null;
            }
            this.eventCode = historyItem.eventCode;
            if (historyItem.eventTag != null) {
                this.eventTag = this.localEventTag;
                this.eventTag.setTo(historyItem.eventTag);
            } else {
                this.eventTag = null;
            }
            this.currentTime = historyItem.currentTime;
        }

        public boolean sameNonEvent(HistoryItem historyItem) {
            return this.batteryLevel == historyItem.batteryLevel && this.batteryStatus == historyItem.batteryStatus && this.batteryHealth == historyItem.batteryHealth && this.batteryPlugType == historyItem.batteryPlugType && this.batteryTemperature == historyItem.batteryTemperature && this.batteryVoltage == historyItem.batteryVoltage && this.batteryChargeUAh == historyItem.batteryChargeUAh && this.states == historyItem.states && this.states2 == historyItem.states2 && this.currentTime == historyItem.currentTime;
        }

        public boolean same(HistoryItem historyItem) {
            if (!sameNonEvent(historyItem) || this.eventCode != historyItem.eventCode) {
                return false;
            }
            if (this.wakelockTag != historyItem.wakelockTag && (this.wakelockTag == null || historyItem.wakelockTag == null || !this.wakelockTag.equals(historyItem.wakelockTag))) {
                return false;
            }
            if (this.wakeReasonTag != historyItem.wakeReasonTag && (this.wakeReasonTag == null || historyItem.wakeReasonTag == null || !this.wakeReasonTag.equals(historyItem.wakeReasonTag))) {
                return false;
            }
            if (this.eventTag != historyItem.eventTag) {
                return (this.eventTag == null || historyItem.eventTag == null || !this.eventTag.equals(historyItem.eventTag)) ? false : true;
            }
            return true;
        }
    }

    public static final class HistoryEventTracker {
        private final HashMap<String, SparseIntArray>[] mActiveEvents = new HashMap[22];

        public boolean updateState(int i, String str, int i2, int i3) {
            SparseIntArray sparseIntArray;
            int iIndexOfKey;
            if ((32768 & i) == 0) {
                if ((i & 16384) != 0) {
                    HashMap<String, SparseIntArray> map = this.mActiveEvents[i & HistoryItem.EVENT_TYPE_MASK];
                    if (map == null || (sparseIntArray = map.get(str)) == null || (iIndexOfKey = sparseIntArray.indexOfKey(i2)) < 0) {
                        return false;
                    }
                    sparseIntArray.removeAt(iIndexOfKey);
                    if (sparseIntArray.size() <= 0) {
                        map.remove(str);
                        return true;
                    }
                    return true;
                }
                return true;
            }
            int i4 = i & HistoryItem.EVENT_TYPE_MASK;
            HashMap<String, SparseIntArray> map2 = this.mActiveEvents[i4];
            if (map2 == null) {
                map2 = new HashMap<>();
                this.mActiveEvents[i4] = map2;
            }
            SparseIntArray sparseIntArray2 = map2.get(str);
            if (sparseIntArray2 == null) {
                sparseIntArray2 = new SparseIntArray();
                map2.put(str, sparseIntArray2);
            }
            if (sparseIntArray2.indexOfKey(i2) >= 0) {
                return false;
            }
            sparseIntArray2.put(i2, i3);
            return true;
        }

        public void removeEvents(int i) {
            this.mActiveEvents[i & HistoryItem.EVENT_TYPE_MASK] = null;
        }

        public HashMap<String, SparseIntArray> getStateForEvent(int i) {
            return this.mActiveEvents[i];
        }
    }

    public static final class BitDescription {
        public final int mask;
        public final String name;
        public final int shift;
        public final String shortName;
        public final String[] shortValues;
        public final String[] values;

        public BitDescription(int i, String str, String str2) {
            this.mask = i;
            this.shift = -1;
            this.name = str;
            this.shortName = str2;
            this.values = null;
            this.shortValues = null;
        }

        public BitDescription(int i, int i2, String str, String str2, String[] strArr, String[] strArr2) {
            this.mask = i;
            this.shift = i2;
            this.name = str;
            this.shortName = str2;
            this.values = strArr;
            this.shortValues = strArr2;
        }
    }

    private static final void formatTimeRaw(StringBuilder sb, long j) {
        long j2 = j / 86400;
        if (j2 != 0) {
            sb.append(j2);
            sb.append("d ");
        }
        long j3 = j2 * 60 * 60 * 24;
        long j4 = (j - j3) / 3600;
        if (j4 != 0 || j3 != 0) {
            sb.append(j4);
            sb.append("h ");
        }
        long j5 = j3 + (j4 * 60 * 60);
        long j6 = (j - j5) / 60;
        if (j6 != 0 || j5 != 0) {
            sb.append(j6);
            sb.append("m ");
        }
        long j7 = j5 + (j6 * 60);
        if (j != 0 || j7 != 0) {
            sb.append(j - j7);
            sb.append("s ");
        }
    }

    public static final void formatTimeMs(StringBuilder sb, long j) {
        long j2 = j / 1000;
        formatTimeRaw(sb, j2);
        sb.append(j - (j2 * 1000));
        sb.append("ms ");
    }

    public static final void formatTimeMsNoSpace(StringBuilder sb, long j) {
        long j2 = j / 1000;
        formatTimeRaw(sb, j2);
        sb.append(j - (j2 * 1000));
        sb.append("ms");
    }

    public final String formatRatioLocked(long j, long j2) {
        if (j2 == 0) {
            return "--%";
        }
        this.mFormatBuilder.setLength(0);
        this.mFormatter.format("%.1f%%", Float.valueOf((j / j2) * 100.0f));
        return this.mFormatBuilder.toString();
    }

    final String formatBytesLocked(long j) {
        this.mFormatBuilder.setLength(0);
        if (j < 1024) {
            return j + "B";
        }
        if (j < 1048576) {
            this.mFormatter.format("%.2fKB", Double.valueOf(j / 1024.0d));
            return this.mFormatBuilder.toString();
        }
        if (j < 1073741824) {
            this.mFormatter.format("%.2fMB", Double.valueOf(j / 1048576.0d));
            return this.mFormatBuilder.toString();
        }
        this.mFormatter.format("%.2fGB", Double.valueOf(j / 1.073741824E9d));
        return this.mFormatBuilder.toString();
    }

    private static long roundUsToMs(long j) {
        return (j + 500) / 1000;
    }

    private static long computeWakeLock(Timer timer, long j, int i) {
        if (timer != null) {
            return (timer.getTotalTimeLocked(j, i) + 500) / 1000;
        }
        return 0L;
    }

    private static final String printWakeLock(StringBuilder sb, Timer timer, long j, String str, int i, String str2) {
        if (timer != null) {
            long jComputeWakeLock = computeWakeLock(timer, j, i);
            int countLocked = timer.getCountLocked(i);
            if (jComputeWakeLock != 0) {
                sb.append(str2);
                formatTimeMs(sb, jComputeWakeLock);
                if (str != null) {
                    sb.append(str);
                    sb.append(' ');
                }
                sb.append('(');
                sb.append(countLocked);
                sb.append(" times)");
                long j2 = j / 1000;
                long maxDurationMsLocked = timer.getMaxDurationMsLocked(j2);
                if (maxDurationMsLocked >= 0) {
                    sb.append(" max=");
                    sb.append(maxDurationMsLocked);
                }
                long totalDurationMsLocked = timer.getTotalDurationMsLocked(j2);
                if (totalDurationMsLocked > jComputeWakeLock) {
                    sb.append(" actual=");
                    sb.append(totalDurationMsLocked);
                }
                if (timer.isRunningLocked()) {
                    long currentDurationMsLocked = timer.getCurrentDurationMsLocked(j2);
                    if (currentDurationMsLocked >= 0) {
                        sb.append(" (running for ");
                        sb.append(currentDurationMsLocked);
                        sb.append("ms)");
                        return ", ";
                    }
                    sb.append(" (running)");
                    return ", ";
                }
                return ", ";
            }
        }
        return str2;
    }

    private static final boolean printTimer(PrintWriter printWriter, StringBuilder sb, Timer timer, long j, int i, String str, String str2) {
        if (timer != null) {
            long totalTimeLocked = (timer.getTotalTimeLocked(j, i) + 500) / 1000;
            int countLocked = timer.getCountLocked(i);
            if (totalTimeLocked != 0) {
                sb.setLength(0);
                sb.append(str);
                sb.append("    ");
                sb.append(str2);
                sb.append(": ");
                formatTimeMs(sb, totalTimeLocked);
                sb.append("realtime (");
                sb.append(countLocked);
                sb.append(" times)");
                long j2 = j / 1000;
                long maxDurationMsLocked = timer.getMaxDurationMsLocked(j2);
                if (maxDurationMsLocked >= 0) {
                    sb.append(" max=");
                    sb.append(maxDurationMsLocked);
                }
                if (timer.isRunningLocked()) {
                    long currentDurationMsLocked = timer.getCurrentDurationMsLocked(j2);
                    if (currentDurationMsLocked >= 0) {
                        sb.append(" (running for ");
                        sb.append(currentDurationMsLocked);
                        sb.append("ms)");
                    } else {
                        sb.append(" (running)");
                    }
                }
                printWriter.println(sb.toString());
                return true;
            }
        }
        return false;
    }

    private static final String printWakeLockCheckin(StringBuilder sb, Timer timer, long j, String str, int i, String str2) {
        int countLocked;
        long totalDurationMsLocked;
        long currentDurationMsLocked;
        long maxDurationMsLocked;
        String str3;
        long totalTimeLocked = 0;
        if (timer != null) {
            totalTimeLocked = timer.getTotalTimeLocked(j, i);
            countLocked = timer.getCountLocked(i);
            long j2 = j / 1000;
            currentDurationMsLocked = timer.getCurrentDurationMsLocked(j2);
            maxDurationMsLocked = timer.getMaxDurationMsLocked(j2);
            totalDurationMsLocked = timer.getTotalDurationMsLocked(j2);
        } else {
            countLocked = 0;
            totalDurationMsLocked = 0;
            currentDurationMsLocked = 0;
            maxDurationMsLocked = 0;
        }
        sb.append(str2);
        sb.append((totalTimeLocked + 500) / 1000);
        sb.append(',');
        if (str != null) {
            str3 = str + ",";
        } else {
            str3 = "";
        }
        sb.append(str3);
        sb.append(countLocked);
        sb.append(',');
        sb.append(currentDurationMsLocked);
        sb.append(',');
        sb.append(maxDurationMsLocked);
        if (str != null) {
            sb.append(',');
            sb.append(totalDurationMsLocked);
            return ",";
        }
        return ",";
    }

    private static final void dumpLineHeader(PrintWriter printWriter, int i, String str, String str2) {
        printWriter.print(9);
        printWriter.print(',');
        printWriter.print(i);
        printWriter.print(',');
        printWriter.print(str);
        printWriter.print(',');
        printWriter.print(str2);
    }

    private static final void dumpLine(PrintWriter printWriter, int i, String str, String str2, Object... objArr) {
        dumpLineHeader(printWriter, i, str, str2);
        for (Object obj : objArr) {
            printWriter.print(',');
            printWriter.print(obj);
        }
        printWriter.println();
    }

    private static final void dumpTimer(PrintWriter printWriter, int i, String str, String str2, Timer timer, long j, int i2) {
        if (timer != null) {
            long jRoundUsToMs = roundUsToMs(timer.getTotalTimeLocked(j, i2));
            int countLocked = timer.getCountLocked(i2);
            if (jRoundUsToMs != 0 || countLocked != 0) {
                dumpLine(printWriter, i, str, str2, Long.valueOf(jRoundUsToMs), Integer.valueOf(countLocked));
            }
        }
    }

    private static void dumpTimer(ProtoOutputStream protoOutputStream, long j, Timer timer, long j2, int i) {
        if (timer == null) {
            return;
        }
        long jRoundUsToMs = roundUsToMs(timer.getTotalTimeLocked(j2, i));
        int countLocked = timer.getCountLocked(i);
        long j3 = j2 / 1000;
        long maxDurationMsLocked = timer.getMaxDurationMsLocked(j3);
        long currentDurationMsLocked = timer.getCurrentDurationMsLocked(j3);
        long totalDurationMsLocked = timer.getTotalDurationMsLocked(j3);
        if (jRoundUsToMs != 0 || countLocked != 0 || maxDurationMsLocked != -1 || currentDurationMsLocked != -1 || totalDurationMsLocked != -1) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1112396529665L, jRoundUsToMs);
            protoOutputStream.write(1112396529666L, countLocked);
            if (maxDurationMsLocked != -1) {
                protoOutputStream.write(1112396529667L, maxDurationMsLocked);
            }
            if (currentDurationMsLocked != -1) {
                protoOutputStream.write(1112396529668L, currentDurationMsLocked);
            }
            if (totalDurationMsLocked != -1) {
                protoOutputStream.write(1112396529669L, totalDurationMsLocked);
            }
            protoOutputStream.end(jStart);
        }
    }

    private static boolean controllerActivityHasData(ControllerActivityCounter controllerActivityCounter, int i) {
        if (controllerActivityCounter == null) {
            return false;
        }
        if (controllerActivityCounter.getIdleTimeCounter().getCountLocked(i) != 0 || controllerActivityCounter.getRxTimeCounter().getCountLocked(i) != 0 || controllerActivityCounter.getPowerCounter().getCountLocked(i) != 0) {
            return true;
        }
        for (LongCounter longCounter : controllerActivityCounter.getTxTimeCounters()) {
            if (longCounter.getCountLocked(i) != 0) {
                return true;
            }
        }
        return false;
    }

    private static final void dumpControllerActivityLine(PrintWriter printWriter, int i, String str, String str2, ControllerActivityCounter controllerActivityCounter, int i2) {
        if (!controllerActivityHasData(controllerActivityCounter, i2)) {
            return;
        }
        dumpLineHeader(printWriter, i, str, str2);
        printWriter.print(",");
        printWriter.print(controllerActivityCounter.getIdleTimeCounter().getCountLocked(i2));
        printWriter.print(",");
        printWriter.print(controllerActivityCounter.getRxTimeCounter().getCountLocked(i2));
        printWriter.print(",");
        printWriter.print(controllerActivityCounter.getPowerCounter().getCountLocked(i2) / 3600000);
        for (LongCounter longCounter : controllerActivityCounter.getTxTimeCounters()) {
            printWriter.print(",");
            printWriter.print(longCounter.getCountLocked(i2));
        }
        printWriter.println();
    }

    private static void dumpControllerActivityProto(ProtoOutputStream protoOutputStream, long j, ControllerActivityCounter controllerActivityCounter, int i) {
        if (!controllerActivityHasData(controllerActivityCounter, i)) {
            return;
        }
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1112396529665L, controllerActivityCounter.getIdleTimeCounter().getCountLocked(i));
        protoOutputStream.write(1112396529666L, controllerActivityCounter.getRxTimeCounter().getCountLocked(i));
        protoOutputStream.write(1112396529667L, controllerActivityCounter.getPowerCounter().getCountLocked(i) / 3600000);
        LongCounter[] txTimeCounters = controllerActivityCounter.getTxTimeCounters();
        for (int i2 = 0; i2 < txTimeCounters.length; i2++) {
            LongCounter longCounter = txTimeCounters[i2];
            long jStart2 = protoOutputStream.start(2246267895812L);
            protoOutputStream.write(1120986464257L, i2);
            protoOutputStream.write(1112396529666L, longCounter.getCountLocked(i));
            protoOutputStream.end(jStart2);
        }
        protoOutputStream.end(jStart);
    }

    private final void printControllerActivityIfInteresting(PrintWriter printWriter, StringBuilder sb, String str, String str2, ControllerActivityCounter controllerActivityCounter, int i) {
        if (controllerActivityHasData(controllerActivityCounter, i)) {
            printControllerActivity(printWriter, sb, str, str2, controllerActivityCounter, i);
        }
    }

    private final void printControllerActivity(PrintWriter printWriter, StringBuilder sb, String str, String str2, ControllerActivityCounter controllerActivityCounter, int i) {
        String[] strArr;
        long countLocked = controllerActivityCounter.getIdleTimeCounter().getCountLocked(i);
        long countLocked2 = controllerActivityCounter.getRxTimeCounter().getCountLocked(i);
        long countLocked3 = controllerActivityCounter.getPowerCounter().getCountLocked(i);
        long jComputeBatteryRealtime = computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, i) / 1000;
        LongCounter[] txTimeCounters = controllerActivityCounter.getTxTimeCounters();
        int i2 = 0;
        long countLocked4 = 0;
        for (int length = txTimeCounters.length; i2 < length; length = length) {
            countLocked4 += txTimeCounters[i2].getCountLocked(i);
            i2++;
        }
        if (str2.equals(WIFI_CONTROLLER_NAME)) {
            long countLocked5 = controllerActivityCounter.getScanTimeCounter().getCountLocked(i);
            sb.setLength(0);
            sb.append(str);
            sb.append("     ");
            sb.append(str2);
            sb.append(" Scan time:  ");
            formatTimeMs(sb, countLocked5);
            sb.append("(");
            sb.append(formatRatioLocked(countLocked5, jComputeBatteryRealtime));
            sb.append(")");
            printWriter.println(sb.toString());
            long j = jComputeBatteryRealtime - ((countLocked + countLocked2) + countLocked4);
            sb.setLength(0);
            sb.append(str);
            sb.append("     ");
            sb.append(str2);
            sb.append(" Sleep time:  ");
            formatTimeMs(sb, j);
            sb.append("(");
            sb.append(formatRatioLocked(j, jComputeBatteryRealtime));
            sb.append(")");
            printWriter.println(sb.toString());
        }
        if (str2.equals(CELLULAR_CONTROLLER_NAME)) {
            long countLocked6 = controllerActivityCounter.getSleepTimeCounter().getCountLocked(i);
            sb.setLength(0);
            sb.append(str);
            sb.append("     ");
            sb.append(str2);
            sb.append(" Sleep time:  ");
            formatTimeMs(sb, countLocked6);
            sb.append("(");
            sb.append(formatRatioLocked(countLocked6, jComputeBatteryRealtime));
            sb.append(")");
            printWriter.println(sb.toString());
        }
        sb.setLength(0);
        sb.append(str);
        sb.append("     ");
        sb.append(str2);
        sb.append(" Idle time:   ");
        formatTimeMs(sb, countLocked);
        sb.append("(");
        sb.append(formatRatioLocked(countLocked, jComputeBatteryRealtime));
        sb.append(")");
        printWriter.println(sb.toString());
        sb.setLength(0);
        sb.append(str);
        sb.append("     ");
        sb.append(str2);
        sb.append(" Rx time:     ");
        formatTimeMs(sb, countLocked2);
        sb.append("(");
        sb.append(formatRatioLocked(countLocked2, jComputeBatteryRealtime));
        sb.append(")");
        printWriter.println(sb.toString());
        sb.setLength(0);
        sb.append(str);
        sb.append("     ");
        sb.append(str2);
        sb.append(" Tx time:     ");
        byte b = -1;
        if (str2.hashCode() == -851952246 && str2.equals(CELLULAR_CONTROLLER_NAME)) {
            b = 0;
        }
        if (b == 0) {
            strArr = new String[]{"   less than 0dBm: ", "   0dBm to 8dBm: ", "   8dBm to 15dBm: ", "   15dBm to 20dBm: ", "   above 20dBm: "};
        } else {
            strArr = new String[]{"[0]", "[1]", "[2]", "[3]", "[4]"};
        }
        int iMin = Math.min(controllerActivityCounter.getTxTimeCounters().length, strArr.length);
        if (iMin <= 1) {
            long countLocked7 = controllerActivityCounter.getTxTimeCounters()[0].getCountLocked(i);
            formatTimeMs(sb, countLocked7);
            sb.append("(");
            sb.append(formatRatioLocked(countLocked7, jComputeBatteryRealtime));
            sb.append(")");
            printWriter.println(sb.toString());
        } else {
            printWriter.println(sb.toString());
            for (int i3 = 0; i3 < iMin; i3++) {
                long countLocked8 = controllerActivityCounter.getTxTimeCounters()[i3].getCountLocked(i);
                sb.setLength(0);
                sb.append(str);
                sb.append("    ");
                sb.append(strArr[i3]);
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb, countLocked8);
                sb.append("(");
                sb.append(formatRatioLocked(countLocked8, jComputeBatteryRealtime));
                sb.append(")");
                printWriter.println(sb.toString());
            }
        }
        if (countLocked3 > 0) {
            sb.setLength(0);
            sb.append(str);
            sb.append("     ");
            sb.append(str2);
            sb.append(" Battery drain: ");
            sb.append(BatteryStatsHelper.makemAh(countLocked3 / 3600000.0d));
            sb.append("mAh");
            printWriter.println(sb.toString());
        }
    }

    public final void dumpCheckinLocked(Context context, PrintWriter printWriter, int i, int i2) {
        dumpCheckinLocked(context, printWriter, i, i2, BatteryStatsHelper.checkWifiOnly(context));
    }

    public final void dumpCheckinLocked(Context context, PrintWriter printWriter, int i, int i2, boolean z) {
        StringBuilder sb;
        Object objValueOf;
        int i3;
        StringBuilder sb2;
        int i4;
        int i5;
        boolean z2;
        int i6;
        String str;
        PrintWriter printWriter2;
        SparseArray<? extends Uid> sparseArray;
        long j;
        int i7;
        Uid uid;
        int i8;
        long j2;
        long j3;
        String str2;
        PrintWriter printWriter3;
        Uid uid2;
        long j4;
        int i9;
        int size;
        int i10;
        Timer multicastWakelockStats;
        long j5;
        int i11;
        int size2;
        int size3;
        int size4;
        StringBuilder sb3;
        int size5;
        int i12;
        int i13;
        int i14;
        long j6;
        long userCpuTimeUs;
        long systemCpuTimeUs;
        long j7;
        long j8;
        long[] jArr;
        StringBuilder sb4;
        int size6;
        StringBuilder sb5;
        long[] jArr2;
        PrintWriter printWriter4;
        int size7;
        long j9;
        int i15;
        long j10;
        Uid uid3;
        PrintWriter printWriter5;
        int i16;
        long j11;
        SparseArray<? extends Uid.Sensor> sparseArray2;
        int i17;
        StringBuilder sb6;
        Uid uid4;
        long j12;
        long j13;
        int countLocked;
        long totalDurationMsLocked;
        long j14;
        long totalDurationMsLocked2;
        long j15;
        StringBuilder sb7;
        long j16;
        long totalDurationMsLocked3;
        long j17;
        long totalDurationMsLocked4;
        String str3;
        int i18;
        long totalDurationMsLocked5;
        int countLocked2;
        int countLocked3;
        long totalDurationMsLocked6;
        long maxDurationMsLocked;
        long totalDurationMsLocked7;
        long maxDurationMsLocked2;
        String str4;
        PrintWriter printWriter6 = printWriter;
        long jUptimeMillis = SystemClock.uptimeMillis() * 1000;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j18 = jElapsedRealtime * 1000;
        long batteryUptime = getBatteryUptime(jUptimeMillis);
        long jComputeBatteryUptime = computeBatteryUptime(jUptimeMillis, i);
        long jComputeBatteryRealtime = computeBatteryRealtime(j18, i);
        long jComputeBatteryScreenOffUptime = computeBatteryScreenOffUptime(jUptimeMillis, i);
        long jComputeBatteryScreenOffRealtime = computeBatteryScreenOffRealtime(j18, i);
        long jComputeRealtime = computeRealtime(j18, i);
        long jComputeUptime = computeUptime(jUptimeMillis, i);
        long screenOnTime = getScreenOnTime(j18, i);
        long screenDozeTime = getScreenDozeTime(j18, i);
        long interactiveTime = getInteractiveTime(j18, i);
        long powerSaveModeEnabledTime = getPowerSaveModeEnabledTime(j18, i);
        long deviceIdleModeTime = getDeviceIdleModeTime(1, j18, i);
        long deviceIdleModeTime2 = getDeviceIdleModeTime(2, j18, i);
        long deviceIdlingTime = getDeviceIdlingTime(1, j18, i);
        long deviceIdlingTime2 = getDeviceIdlingTime(2, j18, i);
        int numConnectivityChange = getNumConnectivityChange(i);
        long phoneOnTime = getPhoneOnTime(j18, i);
        long uahDischarge = getUahDischarge(i);
        long uahDischargeScreenOff = getUahDischargeScreenOff(i);
        long uahDischargeScreenDoze = getUahDischargeScreenDoze(i);
        long uahDischargeLightDoze = getUahDischargeLightDoze(i);
        long uahDischargeDeepDoze = getUahDischargeDeepDoze(i);
        StringBuilder sb8 = new StringBuilder(128);
        SparseArray<? extends Uid> uidStats = getUidStats();
        long j19 = jElapsedRealtime;
        int size8 = uidStats.size();
        String str5 = STAT_NAMES[i];
        Object[] objArr = new Object[12];
        if (i == 0) {
            sb = sb8;
            objValueOf = Integer.valueOf(getStartCount());
        } else {
            sb = sb8;
            objValueOf = "N/A";
        }
        objArr[0] = objValueOf;
        objArr[1] = Long.valueOf(jComputeBatteryRealtime / 1000);
        objArr[2] = Long.valueOf(jComputeBatteryUptime / 1000);
        objArr[3] = Long.valueOf(jComputeRealtime / 1000);
        objArr[4] = Long.valueOf(jComputeUptime / 1000);
        objArr[5] = Long.valueOf(getStartClockTime());
        objArr[6] = Long.valueOf(jComputeBatteryScreenOffRealtime / 1000);
        objArr[7] = Long.valueOf(jComputeBatteryScreenOffUptime / 1000);
        objArr[8] = Integer.valueOf(getEstimatedBatteryCapacity());
        objArr[9] = Integer.valueOf(getMinLearnedBatteryCapacity());
        objArr[10] = Integer.valueOf(getMaxLearnedBatteryCapacity());
        objArr[11] = Long.valueOf(screenDozeTime / 1000);
        dumpLine(printWriter6, 0, str5, BATTERY_DATA, objArr);
        long totalTimeLocked = 0;
        long totalTimeLocked2 = 0;
        for (int i19 = 0; i19 < size8; i19++) {
            ArrayMap<String, ? extends Uid.Wakelock> wakelockStats = uidStats.valueAt(i19).getWakelockStats();
            int i20 = 1;
            int size9 = wakelockStats.size() - 1;
            while (size9 >= 0) {
                Uid.Wakelock wakelockValueAt = wakelockStats.valueAt(size9);
                Timer wakeTime = wakelockValueAt.getWakeTime(i20);
                if (wakeTime != null) {
                    totalTimeLocked += wakeTime.getTotalTimeLocked(j18, i);
                }
                Timer wakeTime2 = wakelockValueAt.getWakeTime(0);
                if (wakeTime2 != null) {
                    totalTimeLocked2 += wakeTime2.getTotalTimeLocked(j18, i);
                }
                size9--;
                i20 = 1;
            }
        }
        int i21 = size8;
        String str6 = str5;
        dumpLine(printWriter6, 0, str6, GLOBAL_NETWORK_DATA, Long.valueOf(getNetworkActivityBytes(0, i)), Long.valueOf(getNetworkActivityBytes(1, i)), Long.valueOf(getNetworkActivityBytes(2, i)), Long.valueOf(getNetworkActivityBytes(3, i)), Long.valueOf(getNetworkActivityPackets(0, i)), Long.valueOf(getNetworkActivityPackets(1, i)), Long.valueOf(getNetworkActivityPackets(2, i)), Long.valueOf(getNetworkActivityPackets(3, i)), Long.valueOf(getNetworkActivityBytes(4, i)), Long.valueOf(getNetworkActivityBytes(5, i)));
        SparseArray<? extends Uid> sparseArray3 = uidStats;
        long j20 = j18;
        int i22 = i;
        dumpControllerActivityLine(printWriter6, 0, str6, GLOBAL_MODEM_CONTROLLER_DATA, getModemControllerActivity(), i22);
        dumpLine(printWriter6, 0, str6, GLOBAL_WIFI_DATA, Long.valueOf(getWifiOnTime(j20, i22) / 1000), Long.valueOf(getGlobalWifiRunningTime(j20, i22) / 1000), 0, 0, 0);
        dumpControllerActivityLine(printWriter6, 0, str6, GLOBAL_WIFI_CONTROLLER_DATA, getWifiControllerActivity(), i22);
        dumpControllerActivityLine(printWriter6, 0, str6, GLOBAL_BLUETOOTH_CONTROLLER_DATA, getBluetoothControllerActivity(), i22);
        long j21 = batteryUptime;
        dumpLine(printWriter6, 0, str6, MISC_DATA, Long.valueOf(screenOnTime / 1000), Long.valueOf(phoneOnTime / 1000), Long.valueOf(totalTimeLocked / 1000), Long.valueOf(totalTimeLocked2 / 1000), Long.valueOf(getMobileRadioActiveTime(j20, i22) / 1000), Long.valueOf(getMobileRadioActiveAdjustedTime(i22) / 1000), Long.valueOf(interactiveTime / 1000), Long.valueOf(powerSaveModeEnabledTime / 1000), Integer.valueOf(numConnectivityChange), Long.valueOf(deviceIdleModeTime2 / 1000), Integer.valueOf(getDeviceIdleModeCount(2, i22)), Long.valueOf(deviceIdlingTime2 / 1000), Integer.valueOf(getDeviceIdlingCount(2, i22)), Integer.valueOf(getMobileRadioActiveCount(i22)), Long.valueOf(getMobileRadioActiveUnknownTime(i22) / 1000), Long.valueOf(deviceIdleModeTime / 1000), Integer.valueOf(getDeviceIdleModeCount(1, i22)), Long.valueOf(deviceIdlingTime / 1000), Integer.valueOf(getDeviceIdlingCount(1, i22)), Long.valueOf(getLongestDeviceIdleModeTime(1)), Long.valueOf(getLongestDeviceIdleModeTime(2)));
        Object[] objArr2 = new Object[5];
        int i23 = 0;
        for (int i24 = 5; i23 < i24; i24 = 5) {
            objArr2[i23] = Long.valueOf(getScreenBrightnessTime(i23, j20, i22) / 1000);
            i23++;
        }
        dumpLine(printWriter6, 0, str6, "br", objArr2);
        Object[] objArr3 = new Object[5];
        int i25 = 0;
        for (int i26 = 5; i25 < i26; i26 = 5) {
            objArr3[i25] = Long.valueOf(getPhoneSignalStrengthTime(i25, j20, i22) / 1000);
            i25++;
        }
        dumpLine(printWriter6, 0, str6, SIGNAL_STRENGTH_TIME_DATA, objArr3);
        dumpLine(printWriter6, 0, str6, SIGNAL_SCANNING_TIME_DATA, Long.valueOf(getPhoneSignalScanningTime(j20, i22) / 1000));
        for (int i27 = 0; i27 < 5; i27++) {
            objArr3[i27] = Integer.valueOf(getPhoneSignalStrengthCount(i27, i22));
        }
        dumpLine(printWriter6, 0, str6, SIGNAL_STRENGTH_COUNT_DATA, objArr3);
        Object[] objArr4 = new Object[21];
        for (int i28 = 0; i28 < 21; i28++) {
            objArr4[i28] = Long.valueOf(getPhoneDataConnectionTime(i28, j20, i22) / 1000);
        }
        dumpLine(printWriter6, 0, str6, DATA_CONNECTION_TIME_DATA, objArr4);
        for (int i29 = 0; i29 < 21; i29++) {
            objArr4[i29] = Integer.valueOf(getPhoneDataConnectionCount(i29, i22));
        }
        dumpLine(printWriter6, 0, str6, DATA_CONNECTION_COUNT_DATA, objArr4);
        Object[] objArr5 = new Object[8];
        int i30 = 0;
        for (int i31 = 8; i30 < i31; i31 = 8) {
            objArr5[i30] = Long.valueOf(getWifiStateTime(i30, j20, i22) / 1000);
            i30++;
        }
        dumpLine(printWriter6, 0, str6, WIFI_STATE_TIME_DATA, objArr5);
        for (int i32 = 0; i32 < 8; i32++) {
            objArr5[i32] = Integer.valueOf(getWifiStateCount(i32, i22));
        }
        dumpLine(printWriter6, 0, str6, WIFI_STATE_COUNT_DATA, objArr5);
        Object[] objArr6 = new Object[13];
        for (int i33 = 0; i33 < 13; i33++) {
            objArr6[i33] = Long.valueOf(getWifiSupplStateTime(i33, j20, i22) / 1000);
        }
        dumpLine(printWriter6, 0, str6, WIFI_SUPPL_STATE_TIME_DATA, objArr6);
        for (int i34 = 0; i34 < 13; i34++) {
            objArr6[i34] = Integer.valueOf(getWifiSupplStateCount(i34, i22));
        }
        dumpLine(printWriter6, 0, str6, WIFI_SUPPL_STATE_COUNT_DATA, objArr6);
        Object[] objArr7 = new Object[5];
        int i35 = 0;
        for (int i36 = 5; i35 < i36; i36 = 5) {
            objArr7[i35] = Long.valueOf(getWifiSignalStrengthTime(i35, j20, i22) / 1000);
            i35++;
        }
        dumpLine(printWriter6, 0, str6, WIFI_SIGNAL_STRENGTH_TIME_DATA, objArr7);
        for (int i37 = 0; i37 < 5; i37++) {
            objArr7[i37] = Integer.valueOf(getWifiSignalStrengthCount(i37, i22));
        }
        dumpLine(printWriter6, 0, str6, WIFI_SIGNAL_STRENGTH_COUNT_DATA, objArr7);
        dumpLine(printWriter6, 0, str6, WIFI_MULTICAST_TOTAL_DATA, Long.valueOf(getWifiMulticastWakelockTime(j20, i22) / 1000), Integer.valueOf(getWifiMulticastWakelockCount(i22)));
        if (i22 == 2) {
            dumpLine(printWriter6, 0, str6, BATTERY_LEVEL_DATA, Integer.valueOf(getDischargeStartLevel()), Integer.valueOf(getDischargeCurrentLevel()));
        }
        if (i22 == 2) {
            i3 = 0;
            dumpLine(printWriter6, 0, str6, BATTERY_DISCHARGE_DATA, Integer.valueOf(getDischargeStartLevel() - getDischargeCurrentLevel()), Integer.valueOf(getDischargeStartLevel() - getDischargeCurrentLevel()), Integer.valueOf(getDischargeAmountScreenOn()), Integer.valueOf(getDischargeAmountScreenOff()), Long.valueOf(uahDischarge / 1000), Long.valueOf(uahDischargeScreenOff / 1000), Integer.valueOf(getDischargeAmountScreenDoze()), Long.valueOf(uahDischargeScreenDoze / 1000), Long.valueOf(uahDischargeLightDoze / 1000), Long.valueOf(uahDischargeDeepDoze / 1000));
        } else {
            i3 = 0;
            dumpLine(printWriter6, 0, str6, BATTERY_DISCHARGE_DATA, Integer.valueOf(getLowDischargeAmountSinceCharge()), Integer.valueOf(getHighDischargeAmountSinceCharge()), Integer.valueOf(getDischargeAmountScreenOnSinceCharge()), Integer.valueOf(getDischargeAmountScreenOffSinceCharge()), Long.valueOf(uahDischarge / 1000), Long.valueOf(uahDischargeScreenOff / 1000), Integer.valueOf(getDischargeAmountScreenDozeSinceCharge()), Long.valueOf(uahDischargeScreenDoze / 1000), Long.valueOf(uahDischargeLightDoze / 1000), Long.valueOf(uahDischargeDeepDoze / 1000));
        }
        int i38 = i3;
        if (i2 < 0) {
            Map<String, ? extends Timer> kernelWakelockStats = getKernelWakelockStats();
            if (kernelWakelockStats.size() > 0) {
                for (Iterator<Map.Entry<String, ? extends Timer>> it = kernelWakelockStats.entrySet().iterator(); it.hasNext(); it = it) {
                    Map.Entry<String, ? extends Timer> next = it.next();
                    StringBuilder sb9 = sb;
                    sb9.setLength(i38);
                    int i39 = i38;
                    printWakeLockCheckin(sb9, next.getValue(), j20, null, i22, "");
                    Object[] objArr8 = new Object[2];
                    objArr8[i39] = "\"" + next.getKey() + "\"";
                    objArr8[1] = sb9.toString();
                    dumpLine(printWriter6, i39, str6, KERNEL_WAKELOCK_DATA, objArr8);
                    sb = sb9;
                    i38 = i39;
                }
            }
            sb2 = sb;
            Map<String, ? extends Timer> wakeupReasonStats = getWakeupReasonStats();
            if (wakeupReasonStats.size() > 0) {
                for (Map.Entry<String, ? extends Timer> entry : wakeupReasonStats.entrySet()) {
                    dumpLine(printWriter6, 0, str6, WAKEUP_REASON_DATA, "\"" + entry.getKey() + "\"", Long.valueOf((entry.getValue().getTotalTimeLocked(j20, i22) + 500) / 1000), Integer.valueOf(entry.getValue().getCountLocked(i22)));
                }
            }
        } else {
            sb2 = sb;
        }
        Map<String, ? extends Timer> rpmStats = getRpmStats();
        Map<String, ? extends Timer> screenOffRpmStats = getScreenOffRpmStats();
        if (rpmStats.size() > 0) {
            for (Iterator<Map.Entry<String, ? extends Timer>> it2 = rpmStats.entrySet().iterator(); it2.hasNext(); it2 = it2) {
                Map.Entry<String, ? extends Timer> next2 = it2.next();
                sb2.setLength(0);
                Timer value = next2.getValue();
                long totalTimeLocked3 = (value.getTotalTimeLocked(j20, i22) + 500) / 1000;
                int countLocked4 = value.getCountLocked(i22);
                Timer timer = screenOffRpmStats.get(next2.getKey());
                if (timer != null) {
                    long totalTimeLocked4 = (timer.getTotalTimeLocked(j20, i22) + 500) / 1000;
                }
                if (timer != null) {
                    timer.getCountLocked(i22);
                }
                dumpLine(printWriter6, 0, str6, RESOURCE_POWER_MANAGER_DATA, "\"" + next2.getKey() + "\"", Long.valueOf(totalTimeLocked3), Integer.valueOf(countLocked4));
            }
        }
        BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, false, z);
        batteryStatsHelper.create(this);
        batteryStatsHelper.refreshStats(i22, -1);
        List<BatterySipper> usageList = batteryStatsHelper.getUsageList();
        if (usageList != null && usageList.size() > 0) {
            dumpLine(printWriter6, 0, str6, POWER_USE_SUMMARY_DATA, BatteryStatsHelper.makemAh(batteryStatsHelper.getPowerProfile().getBatteryCapacity()), BatteryStatsHelper.makemAh(batteryStatsHelper.getComputedPower()), BatteryStatsHelper.makemAh(batteryStatsHelper.getMinDrainedPower()), BatteryStatsHelper.makemAh(batteryStatsHelper.getMaxDrainedPower()));
            int uid5 = 0;
            for (int i40 = 0; i40 < usageList.size(); i40++) {
                BatterySipper batterySipper = usageList.get(i40);
                switch (batterySipper.drainType) {
                    case AMBIENT_DISPLAY:
                        str4 = "ambi";
                        break;
                    case IDLE:
                        str4 = "idle";
                        break;
                    case CELL:
                        str4 = "cell";
                        break;
                    case PHONE:
                        str4 = "phone";
                        break;
                    case WIFI:
                        str4 = "wifi";
                        break;
                    case BLUETOOTH:
                        str4 = "blue";
                        break;
                    case SCREEN:
                        str4 = "scrn";
                        break;
                    case FLASHLIGHT:
                        str4 = "flashlight";
                        break;
                    case APP:
                        uid5 = batterySipper.uidObj.getUid();
                        str4 = "uid";
                        break;
                    case USER:
                        uid5 = UserHandle.getUid(batterySipper.userId, 0);
                        str4 = "user";
                        break;
                    case UNACCOUNTED:
                        str4 = "unacc";
                        break;
                    case OVERCOUNTED:
                        str4 = "over";
                        break;
                    case CAMERA:
                        str4 = Context.CAMERA_SERVICE;
                        break;
                    case MEMORY:
                        str4 = "memory";
                        break;
                    default:
                        str4 = "???";
                        break;
                }
                dumpLine(printWriter6, uid5, str6, POWER_USE_ITEM_DATA, str4, BatteryStatsHelper.makemAh(batterySipper.totalPowerMah), Integer.valueOf(batterySipper.shouldHide ? 1 : 0), BatteryStatsHelper.makemAh(batterySipper.screenPowerMah), BatteryStatsHelper.makemAh(batterySipper.proportionalSmearMah));
            }
        }
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            sb2.setLength(0);
            int i41 = 0;
            while (i41 < cpuFreqs.length) {
                StringBuilder sb10 = new StringBuilder();
                sb10.append(i41 == 0 ? "" : ",");
                sb10.append(cpuFreqs[i41]);
                sb2.append(sb10.toString());
                i41++;
            }
            i4 = 0;
            dumpLine(printWriter6, 0, str6, GLOBAL_CPU_FREQ_DATA, sb2.toString());
        } else {
            i4 = 0;
        }
        int i42 = i4;
        while (true) {
            int i43 = i21;
            if (i42 < i43) {
                SparseArray<? extends Uid> sparseArray4 = sparseArray3;
                int iKeyAt = sparseArray4.keyAt(i42);
                int i44 = i4;
                if (i2 >= 0 && iKeyAt != i2) {
                    i15 = i44;
                    sparseArray = sparseArray4;
                    i10 = i43;
                    printWriter4 = printWriter6;
                    j7 = j20;
                    sb5 = sb2;
                    jArr2 = cpuFreqs;
                    i5 = i42;
                    j8 = j19;
                    j9 = j21;
                    str2 = str6;
                } else {
                    Uid uidValueAt = sparseArray4.valueAt(i42);
                    long networkActivityBytes = uidValueAt.getNetworkActivityBytes(i44, i22);
                    int i45 = i43;
                    long networkActivityBytes2 = uidValueAt.getNetworkActivityBytes(1, i22);
                    long[] jArr3 = cpuFreqs;
                    i5 = i42;
                    long networkActivityBytes3 = uidValueAt.getNetworkActivityBytes(2, i22);
                    long j22 = j20;
                    long networkActivityBytes4 = uidValueAt.getNetworkActivityBytes(3, i22);
                    long networkActivityPackets = uidValueAt.getNetworkActivityPackets(0, i22);
                    long networkActivityPackets2 = uidValueAt.getNetworkActivityPackets(1, i22);
                    long mobileRadioActiveTime = uidValueAt.getMobileRadioActiveTime(i22);
                    StringBuilder sb11 = sb2;
                    int mobileRadioActiveCount = uidValueAt.getMobileRadioActiveCount(i22);
                    long mobileRadioApWakeupCount = uidValueAt.getMobileRadioApWakeupCount(i22);
                    String str7 = str6;
                    long networkActivityPackets3 = uidValueAt.getNetworkActivityPackets(2, i22);
                    long networkActivityPackets4 = uidValueAt.getNetworkActivityPackets(3, i22);
                    long wifiRadioApWakeupCount = uidValueAt.getWifiRadioApWakeupCount(i22);
                    long networkActivityBytes5 = uidValueAt.getNetworkActivityBytes(4, i22);
                    long networkActivityBytes6 = uidValueAt.getNetworkActivityBytes(5, i22);
                    long networkActivityBytes7 = uidValueAt.getNetworkActivityBytes(6, i22);
                    long networkActivityBytes8 = uidValueAt.getNetworkActivityBytes(7, i22);
                    long networkActivityBytes9 = uidValueAt.getNetworkActivityBytes(8, i22);
                    long networkActivityBytes10 = uidValueAt.getNetworkActivityBytes(9, i22);
                    long networkActivityPackets5 = uidValueAt.getNetworkActivityPackets(6, i22);
                    long networkActivityPackets6 = uidValueAt.getNetworkActivityPackets(7, i22);
                    long networkActivityPackets7 = uidValueAt.getNetworkActivityPackets(8, i22);
                    long networkActivityPackets8 = uidValueAt.getNetworkActivityPackets(9, i22);
                    if (networkActivityBytes > 0 || networkActivityBytes2 > 0 || networkActivityBytes3 > 0 || networkActivityBytes4 > 0 || networkActivityPackets > 0 || networkActivityPackets2 > 0 || networkActivityPackets3 > 0 || networkActivityPackets4 > 0 || mobileRadioActiveTime > 0 || mobileRadioActiveCount > 0 || networkActivityBytes5 > 0 || networkActivityBytes6 > 0 || mobileRadioApWakeupCount > 0 || wifiRadioApWakeupCount > 0 || networkActivityBytes7 > 0 || networkActivityBytes8 > 0 || networkActivityBytes9 > 0 || networkActivityBytes10 > 0 || networkActivityPackets5 > 0 || networkActivityPackets6 > 0 || networkActivityPackets7 > 0 || networkActivityPackets8 > 0) {
                        z2 = false;
                        Object[] objArr9 = {Long.valueOf(networkActivityBytes), Long.valueOf(networkActivityBytes2), Long.valueOf(networkActivityBytes3), Long.valueOf(networkActivityBytes4), Long.valueOf(networkActivityPackets), Long.valueOf(networkActivityPackets2), Long.valueOf(networkActivityPackets3), Long.valueOf(networkActivityPackets4), Long.valueOf(mobileRadioActiveTime), Integer.valueOf(mobileRadioActiveCount), Long.valueOf(networkActivityBytes5), Long.valueOf(networkActivityBytes6), Long.valueOf(mobileRadioApWakeupCount), Long.valueOf(wifiRadioApWakeupCount), Long.valueOf(networkActivityBytes7), Long.valueOf(networkActivityBytes8), Long.valueOf(networkActivityBytes9), Long.valueOf(networkActivityBytes10), Long.valueOf(networkActivityPackets5), Long.valueOf(networkActivityPackets6), Long.valueOf(networkActivityPackets7), Long.valueOf(networkActivityPackets8)};
                        i6 = iKeyAt;
                        str = str7;
                        printWriter2 = printWriter;
                        dumpLine(printWriter2, i6, str, NETWORK_DATA, objArr9);
                    } else {
                        i6 = iKeyAt;
                        str = str7;
                        printWriter2 = printWriter;
                        z2 = false;
                    }
                    int i46 = i6;
                    dumpControllerActivityLine(printWriter2, i6, str, MODEM_CONTROLLER_DATA, uidValueAt.getModemControllerActivity(), i);
                    long fullWifiLockTime = uidValueAt.getFullWifiLockTime(j22, i);
                    long wifiScanTime = uidValueAt.getWifiScanTime(j22, i);
                    int wifiScanCount = uidValueAt.getWifiScanCount(i);
                    int wifiScanBackgroundCount = uidValueAt.getWifiScanBackgroundCount(i);
                    long wifiScanActualTime = (uidValueAt.getWifiScanActualTime(j22) + 500) / 1000;
                    sparseArray = sparseArray4;
                    long wifiScanBackgroundTime = (uidValueAt.getWifiScanBackgroundTime(j22) + 500) / 1000;
                    long wifiRunningTime = uidValueAt.getWifiRunningTime(j22, i);
                    if (fullWifiLockTime != 0 || wifiScanTime != 0 || wifiScanCount != 0 || wifiScanBackgroundCount != 0 || wifiScanActualTime != 0 || wifiScanBackgroundTime != 0 || wifiRunningTime != 0) {
                        j = j22;
                        Object[] objArr10 = {Long.valueOf(fullWifiLockTime), Long.valueOf(wifiScanTime), Long.valueOf(wifiRunningTime), Integer.valueOf(wifiScanCount), 0, 0, 0, Integer.valueOf(wifiScanBackgroundCount), Long.valueOf(wifiScanActualTime), Long.valueOf(wifiScanBackgroundTime)};
                        i7 = i46;
                        dumpLine(printWriter2, i7, str, WIFI_DATA, objArr10);
                    } else {
                        j = j22;
                        i7 = i46;
                    }
                    dumpControllerActivityLine(printWriter2, i7, str, WIFI_CONTROLLER_DATA, uidValueAt.getWifiControllerActivity(), i);
                    Timer bluetoothScanTimer = uidValueAt.getBluetoothScanTimer();
                    if (bluetoothScanTimer != null) {
                        long j23 = j;
                        long totalTimeLocked5 = (bluetoothScanTimer.getTotalTimeLocked(j23, i) + 500) / 1000;
                        if (totalTimeLocked5 != 0) {
                            int countLocked5 = bluetoothScanTimer.getCountLocked(i);
                            Timer bluetoothScanBackgroundTimer = uidValueAt.getBluetoothScanBackgroundTimer();
                            int countLocked6 = bluetoothScanBackgroundTimer != null ? bluetoothScanBackgroundTimer.getCountLocked(i) : 0;
                            j3 = j23;
                            long j24 = j19;
                            long totalDurationMsLocked8 = bluetoothScanTimer.getTotalDurationMsLocked(j24);
                            if (bluetoothScanBackgroundTimer != null) {
                                str3 = str;
                                i18 = i7;
                                totalDurationMsLocked5 = bluetoothScanBackgroundTimer.getTotalDurationMsLocked(j24);
                            } else {
                                str3 = str;
                                i18 = i7;
                                totalDurationMsLocked5 = 0;
                            }
                            if (uidValueAt.getBluetoothScanResultCounter() != null) {
                                countLocked2 = uidValueAt.getBluetoothScanResultCounter().getCountLocked(i);
                            } else {
                                countLocked2 = 0;
                            }
                            if (uidValueAt.getBluetoothScanResultBgCounter() != null) {
                                countLocked3 = uidValueAt.getBluetoothScanResultBgCounter().getCountLocked(i);
                            } else {
                                countLocked3 = 0;
                            }
                            Timer bluetoothUnoptimizedScanTimer = uidValueAt.getBluetoothUnoptimizedScanTimer();
                            if (bluetoothUnoptimizedScanTimer != null) {
                                totalDurationMsLocked6 = bluetoothUnoptimizedScanTimer.getTotalDurationMsLocked(j24);
                            } else {
                                totalDurationMsLocked6 = 0;
                            }
                            if (bluetoothUnoptimizedScanTimer != null) {
                                maxDurationMsLocked = bluetoothUnoptimizedScanTimer.getMaxDurationMsLocked(j24);
                            } else {
                                maxDurationMsLocked = 0;
                            }
                            Timer bluetoothUnoptimizedScanBackgroundTimer = uidValueAt.getBluetoothUnoptimizedScanBackgroundTimer();
                            if (bluetoothUnoptimizedScanBackgroundTimer != null) {
                                uid = uidValueAt;
                                totalDurationMsLocked7 = bluetoothUnoptimizedScanBackgroundTimer.getTotalDurationMsLocked(j24);
                            } else {
                                uid = uidValueAt;
                                totalDurationMsLocked7 = 0;
                            }
                            if (bluetoothUnoptimizedScanBackgroundTimer != null) {
                                j2 = j24;
                                maxDurationMsLocked2 = bluetoothUnoptimizedScanBackgroundTimer.getMaxDurationMsLocked(j24);
                            } else {
                                j2 = j24;
                                maxDurationMsLocked2 = 0;
                            }
                            Object[] objArr11 = {Long.valueOf(totalTimeLocked5), Integer.valueOf(countLocked5), Integer.valueOf(countLocked6), Long.valueOf(totalDurationMsLocked8), Long.valueOf(totalDurationMsLocked5), Integer.valueOf(countLocked2), Integer.valueOf(countLocked3), Long.valueOf(totalDurationMsLocked6), Long.valueOf(totalDurationMsLocked7), Long.valueOf(maxDurationMsLocked), Long.valueOf(maxDurationMsLocked2)};
                            str2 = str3;
                            i8 = i18;
                            printWriter3 = printWriter;
                            dumpLine(printWriter3, i8, str2, BLUETOOTH_MISC_DATA, objArr11);
                            uid2 = uid;
                            int i47 = i;
                            dumpControllerActivityLine(printWriter3, i8, str2, BLUETOOTH_CONTROLLER_DATA, uid2.getBluetoothControllerActivity(), i47);
                            if (uid2.hasUserActivity()) {
                                Object[] objArr12 = new Object[4];
                                int i48 = 0;
                                boolean z3 = false;
                                for (int i49 = 4; i48 < i49; i49 = 4) {
                                    int userActivityCount = uid2.getUserActivityCount(i48, i47);
                                    objArr12[i48] = Integer.valueOf(userActivityCount);
                                    if (userActivityCount != 0) {
                                        z3 = true;
                                    }
                                    i48++;
                                }
                                if (z3) {
                                    dumpLine(printWriter3, i8, str2, USER_ACTIVITY_DATA, objArr12);
                                }
                            }
                            if (uid2.getAggregatedPartialWakelockTimer() == null) {
                                Timer aggregatedPartialWakelockTimer = uid2.getAggregatedPartialWakelockTimer();
                                j4 = j2;
                                long totalDurationMsLocked9 = aggregatedPartialWakelockTimer.getTotalDurationMsLocked(j4);
                                Timer subTimer = aggregatedPartialWakelockTimer.getSubTimer();
                                if (subTimer != null) {
                                    totalDurationMsLocked4 = subTimer.getTotalDurationMsLocked(j4);
                                } else {
                                    totalDurationMsLocked4 = 0;
                                }
                                i9 = 1;
                                dumpLine(printWriter3, i8, str2, AGGREGATED_WAKELOCK_DATA, Long.valueOf(totalDurationMsLocked9), Long.valueOf(totalDurationMsLocked4));
                            } else {
                                j4 = j2;
                                i9 = 1;
                            }
                            ArrayMap<String, ? extends Uid.Wakelock> wakelockStats2 = uid2.getWakelockStats();
                            size = wakelockStats2.size() - i9;
                            while (size >= 0) {
                                Uid.Wakelock wakelockValueAt2 = wakelockStats2.valueAt(size);
                                StringBuilder sb12 = sb11;
                                sb12.setLength(0);
                                int i50 = size;
                                long j25 = j4;
                                int i51 = i45;
                                long j26 = j3;
                                String strPrintWakeLockCheckin = printWakeLockCheckin(sb12, wakelockValueAt2.getWakeTime(1), j3, FullBackup.FILES_TREE_TOKEN, i47, "");
                                Timer wakeTime3 = wakelockValueAt2.getWakeTime(0);
                                printWakeLockCheckin(sb12, wakelockValueAt2.getWakeTime(2), j26, "w", i47, printWakeLockCheckin(sb12, wakeTime3 != null ? wakeTime3.getSubTimer() : null, j26, "bp", i47, printWakeLockCheckin(sb12, wakeTime3, j26, TtmlUtils.TAG_P, i47, strPrintWakeLockCheckin)));
                                if (sb12.length() > 0) {
                                    String strKeyAt = wakelockStats2.keyAt(i50);
                                    if (strKeyAt.indexOf(44) >= 0) {
                                        strKeyAt = strKeyAt.replace(',', '_');
                                    }
                                    if (strKeyAt.indexOf(10) >= 0) {
                                        strKeyAt = strKeyAt.replace('\n', '_');
                                    }
                                    if (strKeyAt.indexOf(13) >= 0) {
                                        strKeyAt = strKeyAt.replace('\r', '_');
                                    }
                                    dumpLine(printWriter3, i8, str2, WAKELOCK_DATA, strKeyAt, sb12.toString());
                                }
                                size = i50 - 1;
                                sb11 = sb12;
                                i45 = i51;
                                j4 = j25;
                                j3 = j26;
                            }
                            long j27 = j4;
                            i10 = i45;
                            StringBuilder sb13 = sb11;
                            long j28 = j3;
                            multicastWakelockStats = uid2.getMulticastWakelockStats();
                            if (multicastWakelockStats == null) {
                                j5 = j28;
                                long totalTimeLocked6 = multicastWakelockStats.getTotalTimeLocked(j5, i47) / 1000;
                                int countLocked7 = multicastWakelockStats.getCountLocked(i47);
                                if (totalTimeLocked6 > 0) {
                                    i11 = 1;
                                    dumpLine(printWriter3, i8, str2, WIFI_MULTICAST_DATA, Long.valueOf(totalTimeLocked6), Integer.valueOf(countLocked7));
                                }
                                ArrayMap<String, ? extends Timer> syncStats = uid2.getSyncStats();
                                size2 = syncStats.size() - i11;
                                while (size2 >= 0) {
                                    Timer timerValueAt = syncStats.valueAt(size2);
                                    long totalTimeLocked7 = (timerValueAt.getTotalTimeLocked(j5, i47) + 500) / 1000;
                                    int countLocked8 = timerValueAt.getCountLocked(i47);
                                    Timer subTimer2 = timerValueAt.getSubTimer();
                                    if (subTimer2 != null) {
                                        sb7 = sb13;
                                        j16 = j27;
                                        totalDurationMsLocked3 = subTimer2.getTotalDurationMsLocked(j16);
                                    } else {
                                        sb7 = sb13;
                                        j16 = j27;
                                        totalDurationMsLocked3 = -1;
                                    }
                                    long j29 = j16;
                                    long j30 = totalDurationMsLocked3;
                                    int countLocked9 = subTimer2 != null ? subTimer2.getCountLocked(i47) : -1;
                                    if (totalTimeLocked7 == 0) {
                                        j17 = j5;
                                    } else {
                                        j17 = j5;
                                        dumpLine(printWriter3, i8, str2, SYNC_DATA, "\"" + syncStats.keyAt(size2) + "\"", Long.valueOf(totalTimeLocked7), Integer.valueOf(countLocked8), Long.valueOf(j30), Integer.valueOf(countLocked9));
                                    }
                                    size2--;
                                    sb13 = sb7;
                                    j27 = j29;
                                    j5 = j17;
                                    i47 = i;
                                }
                                long j31 = j5;
                                StringBuilder sb14 = sb13;
                                long j32 = j27;
                                ArrayMap<String, ? extends Timer> jobStats = uid2.getJobStats();
                                size3 = jobStats.size() - 1;
                                while (size3 >= 0) {
                                    Timer timerValueAt2 = jobStats.valueAt(size3);
                                    long j33 = j31;
                                    long totalTimeLocked8 = (timerValueAt2.getTotalTimeLocked(j33, i) + 500) / 1000;
                                    int countLocked10 = timerValueAt2.getCountLocked(i);
                                    Timer subTimer3 = timerValueAt2.getSubTimer();
                                    if (subTimer3 != null) {
                                        j14 = j32;
                                        totalDurationMsLocked2 = subTimer3.getTotalDurationMsLocked(j14);
                                    } else {
                                        j14 = j32;
                                        totalDurationMsLocked2 = -1;
                                    }
                                    long j34 = totalDurationMsLocked2;
                                    int countLocked11 = subTimer3 != null ? subTimer3.getCountLocked(i) : -1;
                                    if (totalTimeLocked8 == 0) {
                                        j15 = j14;
                                    } else {
                                        j15 = j14;
                                        dumpLine(printWriter3, i8, str2, JOB_DATA, "\"" + jobStats.keyAt(size3) + "\"", Long.valueOf(totalTimeLocked8), Integer.valueOf(countLocked10), Long.valueOf(j34), Integer.valueOf(countLocked11));
                                    }
                                    size3--;
                                    j31 = j33;
                                    j32 = j15;
                                }
                                long j35 = j32;
                                long j36 = j31;
                                ArrayMap<String, SparseIntArray> jobCompletionStats = uid2.getJobCompletionStats();
                                for (size4 = jobCompletionStats.size() - 1; size4 >= 0; size4--) {
                                    SparseIntArray sparseIntArrayValueAt = jobCompletionStats.valueAt(size4);
                                    if (sparseIntArrayValueAt != null) {
                                        dumpLine(printWriter3, i8, str2, JOB_COMPLETION_DATA, "\"" + jobCompletionStats.keyAt(size4) + "\"", Integer.valueOf(sparseIntArrayValueAt.get(0, 0)), Integer.valueOf(sparseIntArrayValueAt.get(1, 0)), Integer.valueOf(sparseIntArrayValueAt.get(2, 0)), Integer.valueOf(sparseIntArrayValueAt.get(3, 0)), Integer.valueOf(sparseIntArrayValueAt.get(4, 0)));
                                    }
                                }
                                sb3 = sb14;
                                int i52 = i;
                                uid2.getDeferredJobsCheckinLineLocked(sb3, i52);
                                if (sb3.length() > 0) {
                                    dumpLine(printWriter3, i8, str2, JOBS_DEFERRED_DATA, sb3.toString());
                                }
                                PrintWriter printWriter7 = printWriter3;
                                int i53 = i8;
                                String str8 = str2;
                                long j37 = j36;
                                long j38 = j35;
                                dumpTimer(printWriter7, i53, str8, FLASHLIGHT_DATA, uid2.getFlashlightTurnedOnTimer(), j37, i52);
                                dumpTimer(printWriter7, i53, str8, CAMERA_DATA, uid2.getCameraTurnedOnTimer(), j37, i52);
                                dumpTimer(printWriter7, i53, str8, VIDEO_DATA, uid2.getVideoTurnedOnTimer(), j37, i52);
                                dumpTimer(printWriter7, i53, str8, AUDIO_DATA, uid2.getAudioTurnedOnTimer(), j37, i52);
                                SparseArray<? extends Uid.Sensor> sensorStats = uid2.getSensorStats();
                                size5 = sensorStats.size();
                                i12 = 0;
                                while (i12 < size5) {
                                    Uid.Sensor sensorValueAt = sensorStats.valueAt(i12);
                                    int iKeyAt2 = sensorStats.keyAt(i12);
                                    Timer sensorTime = sensorValueAt.getSensorTime();
                                    if (sensorTime != null) {
                                        long totalTimeLocked9 = (sensorTime.getTotalTimeLocked(j37, i52) + 500) / 1000;
                                        if (totalTimeLocked9 == 0) {
                                            sparseArray2 = sensorStats;
                                            i17 = size5;
                                            sb6 = sb3;
                                            uid4 = uid2;
                                            j12 = j37;
                                            j13 = j38;
                                        } else {
                                            sparseArray2 = sensorStats;
                                            int countLocked12 = sensorTime.getCountLocked(i52);
                                            Timer sensorBackgroundTime = sensorValueAt.getSensorBackgroundTime();
                                            if (sensorBackgroundTime != null) {
                                                i17 = size5;
                                                countLocked = sensorBackgroundTime.getCountLocked(i52);
                                            } else {
                                                i17 = size5;
                                                countLocked = 0;
                                            }
                                            sb6 = sb3;
                                            j12 = j37;
                                            long j39 = j38;
                                            long totalDurationMsLocked10 = sensorTime.getTotalDurationMsLocked(j39);
                                            if (sensorBackgroundTime != null) {
                                                j13 = j39;
                                                totalDurationMsLocked = sensorBackgroundTime.getTotalDurationMsLocked(j39);
                                            } else {
                                                j13 = j39;
                                                totalDurationMsLocked = 0;
                                            }
                                            uid4 = uid2;
                                            dumpLine(printWriter3, i8, str2, SENSOR_DATA, Integer.valueOf(iKeyAt2), Long.valueOf(totalTimeLocked9), Integer.valueOf(countLocked12), Integer.valueOf(countLocked), Long.valueOf(totalDurationMsLocked10), Long.valueOf(totalDurationMsLocked));
                                        }
                                    }
                                    i12++;
                                    sensorStats = sparseArray2;
                                    size5 = i17;
                                    sb3 = sb6;
                                    j37 = j12;
                                    j38 = j13;
                                    uid2 = uid4;
                                    i52 = i;
                                }
                                StringBuilder sb15 = sb3;
                                long j40 = j37;
                                Uid uid6 = uid2;
                                PrintWriter printWriter8 = printWriter3;
                                int i54 = i8;
                                String str9 = str2;
                                long j41 = j38;
                                int i55 = i;
                                dumpTimer(printWriter8, i54, str9, VIBRATOR_DATA, uid6.getVibratorOnTimer(), j40, i55);
                                dumpTimer(printWriter8, i54, str9, FOREGROUND_ACTIVITY_DATA, uid6.getForegroundActivityTimer(), j40, i55);
                                dumpTimer(printWriter8, i54, str9, FOREGROUND_SERVICE_DATA, uid6.getForegroundServiceTimer(), j40, i55);
                                Object[] objArr13 = new Object[7];
                                i14 = 0;
                                j6 = 0;
                                for (i13 = 7; i14 < i13; i13 = 7) {
                                    long processStateTime = uid6.getProcessStateTime(i14, j40, i55);
                                    objArr13[i14] = Long.valueOf((processStateTime + 500) / 1000);
                                    i14++;
                                    j6 += processStateTime;
                                }
                                long j42 = j40;
                                if (j6 > 0) {
                                    dumpLine(printWriter3, i8, str2, "st", objArr13);
                                }
                                userCpuTimeUs = uid6.getUserCpuTimeUs(i55);
                                systemCpuTimeUs = uid6.getSystemCpuTimeUs(i55);
                                if (userCpuTimeUs > 0 || systemCpuTimeUs > 0) {
                                    dumpLine(printWriter3, i8, str2, CPU_DATA, Long.valueOf(userCpuTimeUs / 1000), Long.valueOf(systemCpuTimeUs / 1000), 0);
                                }
                                if (jArr3 == null) {
                                    long[] cpuFreqTimes = uid6.getCpuFreqTimes(i55);
                                    if (cpuFreqTimes != null) {
                                        jArr = jArr3;
                                        if (cpuFreqTimes.length == jArr.length) {
                                            sb4 = sb15;
                                            sb4.setLength(0);
                                            int i56 = 0;
                                            while (i56 < cpuFreqTimes.length) {
                                                StringBuilder sb16 = new StringBuilder();
                                                sb16.append(i56 == 0 ? "" : ",");
                                                sb16.append(cpuFreqTimes[i56]);
                                                sb4.append(sb16.toString());
                                                i56++;
                                                j42 = j42;
                                            }
                                            j7 = j42;
                                            long[] screenOffCpuFreqTimes = uid6.getScreenOffCpuFreqTimes(i55);
                                            if (screenOffCpuFreqTimes != null) {
                                                for (long j43 : screenOffCpuFreqTimes) {
                                                    sb4.append("," + j43);
                                                }
                                            } else {
                                                for (int i57 = 0; i57 < cpuFreqTimes.length; i57++) {
                                                    sb4.append(",0");
                                                }
                                            }
                                            dumpLine(printWriter3, i8, str2, CPU_TIMES_AT_FREQ_DATA, UID_TIMES_TYPE_ALL, Integer.valueOf(cpuFreqTimes.length), sb4.toString());
                                            i16 = 0;
                                            while (i16 < 7) {
                                                long[] cpuFreqTimes2 = uid6.getCpuFreqTimes(i55, i16);
                                                if (cpuFreqTimes2 == null || cpuFreqTimes2.length != jArr.length) {
                                                    j11 = j41;
                                                } else {
                                                    sb4.setLength(0);
                                                    int i58 = 0;
                                                    while (i58 < cpuFreqTimes2.length) {
                                                        StringBuilder sb17 = new StringBuilder();
                                                        sb17.append(i58 == 0 ? "" : ",");
                                                        sb17.append(cpuFreqTimes2[i58]);
                                                        sb4.append(sb17.toString());
                                                        i58++;
                                                    }
                                                    long[] screenOffCpuFreqTimes2 = uid6.getScreenOffCpuFreqTimes(i55, i16);
                                                    if (screenOffCpuFreqTimes2 != null) {
                                                        int i59 = 0;
                                                        while (i59 < screenOffCpuFreqTimes2.length) {
                                                            sb4.append("," + screenOffCpuFreqTimes2[i59]);
                                                            i59++;
                                                            j41 = j41;
                                                        }
                                                        j11 = j41;
                                                    } else {
                                                        j11 = j41;
                                                        for (int i60 = 0; i60 < cpuFreqTimes2.length; i60++) {
                                                            sb4.append(",0");
                                                        }
                                                    }
                                                    dumpLine(printWriter3, i8, str2, CPU_TIMES_AT_FREQ_DATA, Uid.UID_PROCESS_TYPES[i16], Integer.valueOf(cpuFreqTimes2.length), sb4.toString());
                                                }
                                                i16++;
                                                j41 = j11;
                                            }
                                            j8 = j41;
                                        } else {
                                            j7 = j42;
                                        }
                                    } else {
                                        j7 = j42;
                                        jArr = jArr3;
                                    }
                                    sb4 = sb15;
                                    i16 = 0;
                                    while (i16 < 7) {
                                    }
                                    j8 = j41;
                                } else {
                                    j7 = j42;
                                    j8 = j41;
                                    jArr = jArr3;
                                    sb4 = sb15;
                                }
                                ArrayMap<String, ? extends Uid.Proc> processStats = uid6.getProcessStats();
                                size6 = processStats.size() - 1;
                                while (size6 >= 0) {
                                    Uid.Proc procValueAt = processStats.valueAt(size6);
                                    long userTime = procValueAt.getUserTime(i55);
                                    long systemTime = procValueAt.getSystemTime(i55);
                                    StringBuilder sb18 = sb4;
                                    long[] jArr4 = jArr;
                                    long foregroundTime = procValueAt.getForegroundTime(i55);
                                    int starts = procValueAt.getStarts(i55);
                                    int numCrashes = procValueAt.getNumCrashes(i55);
                                    int numAnrs = procValueAt.getNumAnrs(i55);
                                    if (userTime == 0 && systemTime == 0 && foregroundTime == 0 && starts == 0 && numAnrs == 0 && numCrashes == 0) {
                                        uid3 = uid6;
                                        printWriter5 = printWriter3;
                                    } else {
                                        uid3 = uid6;
                                        Object[] objArr14 = {"\"" + processStats.keyAt(size6) + "\"", Long.valueOf(userTime), Long.valueOf(systemTime), Long.valueOf(foregroundTime), Integer.valueOf(starts), Integer.valueOf(numAnrs), Integer.valueOf(numCrashes)};
                                        printWriter5 = printWriter;
                                        dumpLine(printWriter5, i8, str2, PROCESS_DATA, objArr14);
                                    }
                                    size6--;
                                    printWriter3 = printWriter5;
                                    jArr = jArr4;
                                    sb4 = sb18;
                                    uid6 = uid3;
                                    i55 = i;
                                }
                                sb5 = sb4;
                                jArr2 = jArr;
                                printWriter4 = printWriter3;
                                ArrayMap<String, ? extends Uid.Pkg> packageStats = uid6.getPackageStats();
                                for (size7 = packageStats.size() - 1; size7 >= 0; size7--) {
                                    Uid.Pkg pkgValueAt = packageStats.valueAt(size7);
                                    ArrayMap<String, ? extends Counter> wakeupAlarmStats = pkgValueAt.getWakeupAlarmStats();
                                    int i61 = 0;
                                    for (int size10 = wakeupAlarmStats.size() - 1; size10 >= 0; size10--) {
                                        int countLocked13 = wakeupAlarmStats.valueAt(size10).getCountLocked(i);
                                        i61 += countLocked13;
                                        dumpLine(printWriter4, i8, str2, WAKEUP_ALARM_DATA, wakeupAlarmStats.keyAt(size10).replace(',', '_'), Integer.valueOf(countLocked13));
                                    }
                                    int i62 = i;
                                    ArrayMap<String, ? extends Uid.Pkg.Serv> serviceStats = pkgValueAt.getServiceStats();
                                    int size11 = serviceStats.size() - 1;
                                    while (size11 >= 0) {
                                        Uid.Pkg.Serv servValueAt = serviceStats.valueAt(size11);
                                        long j44 = j21;
                                        long startTime = servValueAt.getStartTime(j44, i62);
                                        int starts2 = servValueAt.getStarts(i62);
                                        int launches = servValueAt.getLaunches(i62);
                                        if (startTime == 0 && starts2 == 0 && launches == 0) {
                                            j10 = j44;
                                        } else {
                                            j10 = j44;
                                            dumpLine(printWriter4, i8, str2, APK_DATA, Integer.valueOf(i61), packageStats.keyAt(size7), serviceStats.keyAt(size11), Long.valueOf(startTime / 1000), Integer.valueOf(starts2), Integer.valueOf(launches));
                                        }
                                        size11--;
                                        j21 = j10;
                                        i62 = i;
                                    }
                                }
                                j9 = j21;
                                i15 = 0;
                            } else {
                                j5 = j28;
                            }
                            i11 = 1;
                            ArrayMap<String, ? extends Timer> syncStats2 = uid2.getSyncStats();
                            size2 = syncStats2.size() - i11;
                            while (size2 >= 0) {
                            }
                            long j312 = j5;
                            StringBuilder sb142 = sb13;
                            long j322 = j27;
                            ArrayMap<String, ? extends Timer> jobStats2 = uid2.getJobStats();
                            size3 = jobStats2.size() - 1;
                            while (size3 >= 0) {
                            }
                            long j352 = j322;
                            long j362 = j312;
                            ArrayMap<String, SparseIntArray> jobCompletionStats2 = uid2.getJobCompletionStats();
                            while (size4 >= 0) {
                            }
                            sb3 = sb142;
                            int i522 = i;
                            uid2.getDeferredJobsCheckinLineLocked(sb3, i522);
                            if (sb3.length() > 0) {
                            }
                            PrintWriter printWriter72 = printWriter3;
                            int i532 = i8;
                            String str82 = str2;
                            long j372 = j362;
                            long j382 = j352;
                            dumpTimer(printWriter72, i532, str82, FLASHLIGHT_DATA, uid2.getFlashlightTurnedOnTimer(), j372, i522);
                            dumpTimer(printWriter72, i532, str82, CAMERA_DATA, uid2.getCameraTurnedOnTimer(), j372, i522);
                            dumpTimer(printWriter72, i532, str82, VIDEO_DATA, uid2.getVideoTurnedOnTimer(), j372, i522);
                            dumpTimer(printWriter72, i532, str82, AUDIO_DATA, uid2.getAudioTurnedOnTimer(), j372, i522);
                            SparseArray<? extends Uid.Sensor> sensorStats2 = uid2.getSensorStats();
                            size5 = sensorStats2.size();
                            i12 = 0;
                            while (i12 < size5) {
                            }
                            StringBuilder sb152 = sb3;
                            long j402 = j372;
                            Uid uid62 = uid2;
                            PrintWriter printWriter82 = printWriter3;
                            int i542 = i8;
                            String str92 = str2;
                            long j412 = j382;
                            int i552 = i;
                            dumpTimer(printWriter82, i542, str92, VIBRATOR_DATA, uid62.getVibratorOnTimer(), j402, i552);
                            dumpTimer(printWriter82, i542, str92, FOREGROUND_ACTIVITY_DATA, uid62.getForegroundActivityTimer(), j402, i552);
                            dumpTimer(printWriter82, i542, str92, FOREGROUND_SERVICE_DATA, uid62.getForegroundServiceTimer(), j402, i552);
                            Object[] objArr132 = new Object[7];
                            i14 = 0;
                            j6 = 0;
                            while (i14 < i13) {
                            }
                            long j422 = j402;
                            if (j6 > 0) {
                            }
                            userCpuTimeUs = uid62.getUserCpuTimeUs(i552);
                            systemCpuTimeUs = uid62.getSystemCpuTimeUs(i552);
                            if (userCpuTimeUs > 0) {
                                dumpLine(printWriter3, i8, str2, CPU_DATA, Long.valueOf(userCpuTimeUs / 1000), Long.valueOf(systemCpuTimeUs / 1000), 0);
                                if (jArr3 == null) {
                                }
                                ArrayMap<String, ? extends Uid.Proc> processStats2 = uid62.getProcessStats();
                                size6 = processStats2.size() - 1;
                                while (size6 >= 0) {
                                }
                                sb5 = sb4;
                                jArr2 = jArr;
                                printWriter4 = printWriter3;
                                ArrayMap<String, ? extends Uid.Pkg> packageStats2 = uid62.getPackageStats();
                                while (size7 >= 0) {
                                }
                                j9 = j21;
                                i15 = 0;
                            }
                        } else {
                            uid = uidValueAt;
                            j3 = j23;
                            j2 = j19;
                            i8 = i7;
                        }
                    } else {
                        uid = uidValueAt;
                        i8 = i7;
                        j2 = j19;
                        j3 = j;
                    }
                    str2 = str;
                    printWriter3 = printWriter2;
                    uid2 = uid;
                    int i472 = i;
                    dumpControllerActivityLine(printWriter3, i8, str2, BLUETOOTH_CONTROLLER_DATA, uid2.getBluetoothControllerActivity(), i472);
                    if (uid2.hasUserActivity()) {
                    }
                    if (uid2.getAggregatedPartialWakelockTimer() == null) {
                    }
                    ArrayMap<String, ? extends Uid.Wakelock> wakelockStats22 = uid2.getWakelockStats();
                    size = wakelockStats22.size() - i9;
                    while (size >= 0) {
                    }
                    long j272 = j4;
                    i10 = i45;
                    StringBuilder sb132 = sb11;
                    long j282 = j3;
                    multicastWakelockStats = uid2.getMulticastWakelockStats();
                    if (multicastWakelockStats == null) {
                    }
                    i11 = 1;
                    ArrayMap<String, ? extends Timer> syncStats22 = uid2.getSyncStats();
                    size2 = syncStats22.size() - i11;
                    while (size2 >= 0) {
                    }
                    long j3122 = j5;
                    StringBuilder sb1422 = sb132;
                    long j3222 = j272;
                    ArrayMap<String, ? extends Timer> jobStats22 = uid2.getJobStats();
                    size3 = jobStats22.size() - 1;
                    while (size3 >= 0) {
                    }
                    long j3522 = j3222;
                    long j3622 = j3122;
                    ArrayMap<String, SparseIntArray> jobCompletionStats22 = uid2.getJobCompletionStats();
                    while (size4 >= 0) {
                    }
                    sb3 = sb1422;
                    int i5222 = i;
                    uid2.getDeferredJobsCheckinLineLocked(sb3, i5222);
                    if (sb3.length() > 0) {
                    }
                    PrintWriter printWriter722 = printWriter3;
                    int i5322 = i8;
                    String str822 = str2;
                    long j3722 = j3622;
                    long j3822 = j3522;
                    dumpTimer(printWriter722, i5322, str822, FLASHLIGHT_DATA, uid2.getFlashlightTurnedOnTimer(), j3722, i5222);
                    dumpTimer(printWriter722, i5322, str822, CAMERA_DATA, uid2.getCameraTurnedOnTimer(), j3722, i5222);
                    dumpTimer(printWriter722, i5322, str822, VIDEO_DATA, uid2.getVideoTurnedOnTimer(), j3722, i5222);
                    dumpTimer(printWriter722, i5322, str822, AUDIO_DATA, uid2.getAudioTurnedOnTimer(), j3722, i5222);
                    SparseArray<? extends Uid.Sensor> sensorStats22 = uid2.getSensorStats();
                    size5 = sensorStats22.size();
                    i12 = 0;
                    while (i12 < size5) {
                    }
                    StringBuilder sb1522 = sb3;
                    long j4022 = j3722;
                    Uid uid622 = uid2;
                    PrintWriter printWriter822 = printWriter3;
                    int i5422 = i8;
                    String str922 = str2;
                    long j4122 = j3822;
                    int i5522 = i;
                    dumpTimer(printWriter822, i5422, str922, VIBRATOR_DATA, uid622.getVibratorOnTimer(), j4022, i5522);
                    dumpTimer(printWriter822, i5422, str922, FOREGROUND_ACTIVITY_DATA, uid622.getForegroundActivityTimer(), j4022, i5522);
                    dumpTimer(printWriter822, i5422, str922, FOREGROUND_SERVICE_DATA, uid622.getForegroundServiceTimer(), j4022, i5522);
                    Object[] objArr1322 = new Object[7];
                    i14 = 0;
                    j6 = 0;
                    while (i14 < i13) {
                    }
                    long j4222 = j4022;
                    if (j6 > 0) {
                    }
                    userCpuTimeUs = uid622.getUserCpuTimeUs(i5522);
                    systemCpuTimeUs = uid622.getSystemCpuTimeUs(i5522);
                    if (userCpuTimeUs > 0) {
                    }
                }
                i42 = i5 + 1;
                printWriter6 = printWriter4;
                str6 = str2;
                i21 = i10;
                i4 = i15;
                sparseArray3 = sparseArray;
                j20 = j7;
                j19 = j8;
                cpuFreqs = jArr2;
                sb2 = sb5;
                j21 = j9;
                i22 = i;
            } else {
                return;
            }
        }
    }

    static final class TimerEntry {
        final int mId;
        final String mName;
        final long mTime;
        final Timer mTimer;

        TimerEntry(String str, int i, Timer timer, long j) {
            this.mName = str;
            this.mId = i;
            this.mTimer = timer;
            this.mTime = j;
        }
    }

    private void printmAh(PrintWriter printWriter, double d) {
        printWriter.print(BatteryStatsHelper.makemAh(d));
    }

    private void printmAh(StringBuilder sb, double d) {
        sb.append(BatteryStatsHelper.makemAh(d));
    }

    public final void dumpLocked(Context context, PrintWriter printWriter, String str, int i, int i2) {
        dumpLocked(context, printWriter, str, i, i2, BatteryStatsHelper.checkWifiOnly(context));
    }

    public final void dumpLocked(Context context, PrintWriter printWriter, String str, int i, int i2, boolean z) {
        int i3;
        int i4;
        long j;
        int i5;
        long j2;
        PrintWriter printWriter2;
        long j3;
        long j4;
        long j5;
        int i6;
        int i7;
        int i8;
        long j6;
        long j7;
        StringBuilder sb;
        long j8;
        int i9;
        int i10;
        int i11;
        long j9;
        int i12;
        SparseArray<? extends Uid> sparseArray;
        PrintWriter printWriter3;
        long j10;
        StringBuilder sb2;
        String str2;
        long j11;
        Uid uid;
        int i13;
        long j12;
        boolean z2;
        int i14;
        SparseArray<? extends Uid> sparseArray2;
        long j13;
        long j14;
        long j15;
        long j16;
        long j17;
        Timer bluetoothScanTimer;
        Uid uid2;
        long j18;
        PrintWriter printWriter4;
        long j19;
        int i15;
        int i16;
        int i17;
        long j20;
        int i18;
        long j21;
        Uid uid3;
        String str3;
        Timer multicastWakelockStats;
        long j22;
        int i19;
        int size;
        int size2;
        int size3;
        long j23;
        int i20;
        StringBuilder sb3;
        String str4;
        long j24;
        int i21;
        int size4;
        int i22;
        boolean z3;
        long j25;
        int i23;
        long userCpuTimeUs;
        long systemCpuTimeUs;
        long[] cpuFreqTimes;
        long[] screenOffCpuFreqTimes;
        int i24;
        int size5;
        long j26;
        long j27;
        int size6;
        int i25;
        long j28;
        int i26;
        boolean z4;
        int iCountExcessivePowers;
        int i27;
        long j29;
        int i28;
        SparseArray<? extends Uid.Sensor> sparseArray3;
        int i29;
        long j30;
        long totalDurationMsLocked;
        long j31;
        long totalDurationMsLocked2;
        long j32;
        long totalDurationMsLocked3;
        long j33;
        long j34;
        int i30;
        int i31;
        long totalDurationMsLocked4;
        int countLocked;
        Timer timer;
        int i32;
        long totalDurationMsLocked5;
        int countLocked2;
        int countLocked3;
        long totalDurationMsLocked6;
        long maxDurationMsLocked;
        long totalDurationMsLocked7;
        long maxDurationMsLocked2;
        long j35;
        Map<String, ? extends Timer> wakeupReasonStats;
        long j36;
        long j37;
        String[] strArr;
        int i33;
        BatteryStats batteryStats = this;
        long jUptimeMillis = SystemClock.uptimeMillis() * 1000;
        long jElapsedRealtime = SystemClock.elapsedRealtime() * 1000;
        long j38 = (jElapsedRealtime + 500) / 1000;
        long batteryUptime = batteryStats.getBatteryUptime(jUptimeMillis);
        long jComputeBatteryUptime = batteryStats.computeBatteryUptime(jUptimeMillis, i);
        long jComputeBatteryRealtime = batteryStats.computeBatteryRealtime(jElapsedRealtime, i);
        long jComputeRealtime = batteryStats.computeRealtime(jElapsedRealtime, i);
        long jComputeUptime = batteryStats.computeUptime(jUptimeMillis, i);
        long jComputeBatteryScreenOffUptime = batteryStats.computeBatteryScreenOffUptime(jUptimeMillis, i);
        long jComputeBatteryScreenOffRealtime = batteryStats.computeBatteryScreenOffRealtime(jElapsedRealtime, i);
        long jComputeBatteryTimeRemaining = batteryStats.computeBatteryTimeRemaining(jElapsedRealtime);
        long jComputeChargeTimeRemaining = batteryStats.computeChargeTimeRemaining(jElapsedRealtime);
        long screenDozeTime = batteryStats.getScreenDozeTime(jElapsedRealtime, i);
        StringBuilder sb4 = new StringBuilder(128);
        SparseArray<? extends Uid> uidStats = getUidStats();
        int size7 = uidStats.size();
        int estimatedBatteryCapacity = getEstimatedBatteryCapacity();
        SparseArray<? extends Uid> sparseArray4 = uidStats;
        if (estimatedBatteryCapacity > 0) {
            sb4.setLength(0);
            sb4.append(str);
            i3 = 0;
            sb4.append("  Estimated battery capacity: ");
            i4 = size7;
            sb4.append(BatteryStatsHelper.makemAh(estimatedBatteryCapacity));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        } else {
            i3 = 0;
            i4 = size7;
        }
        if (getMinLearnedBatteryCapacity() > 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Min learned battery capacity: ");
            j = jComputeRealtime;
            sb4.append(BatteryStatsHelper.makemAh(r4 / 1000));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        } else {
            j = jComputeRealtime;
        }
        if (getMaxLearnedBatteryCapacity() > 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Max learned battery capacity: ");
            sb4.append(BatteryStatsHelper.makemAh(r4 / 1000));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        sb4.setLength(i3);
        sb4.append(str);
        sb4.append("  Time on battery: ");
        long j39 = jComputeBatteryRealtime / 1000;
        formatTimeMs(sb4, j39);
        sb4.append("(");
        long j40 = j;
        sb4.append(batteryStats.formatRatioLocked(jComputeBatteryRealtime, j40));
        sb4.append(") realtime, ");
        formatTimeMs(sb4, jComputeBatteryUptime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(jComputeBatteryUptime, jComputeBatteryRealtime));
        sb4.append(") uptime");
        printWriter.println(sb4.toString());
        sb4.setLength(i3);
        sb4.append(str);
        sb4.append("  Time on battery screen off: ");
        formatTimeMs(sb4, jComputeBatteryScreenOffRealtime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(jComputeBatteryScreenOffRealtime, jComputeBatteryRealtime));
        sb4.append(") realtime, ");
        formatTimeMs(sb4, jComputeBatteryScreenOffUptime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(jComputeBatteryScreenOffUptime, jComputeBatteryRealtime));
        sb4.append(") uptime");
        printWriter.println(sb4.toString());
        sb4.setLength(i3);
        sb4.append(str);
        sb4.append("  Time on battery screen doze: ");
        formatTimeMs(sb4, screenDozeTime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(screenDozeTime, jComputeBatteryRealtime));
        sb4.append(")");
        printWriter.println(sb4.toString());
        sb4.setLength(i3);
        sb4.append(str);
        sb4.append("  Total run time: ");
        formatTimeMs(sb4, j40 / 1000);
        sb4.append("realtime, ");
        formatTimeMs(sb4, jComputeUptime / 1000);
        sb4.append("uptime");
        printWriter.println(sb4.toString());
        if (jComputeBatteryTimeRemaining >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Battery time remaining: ");
            formatTimeMs(sb4, jComputeBatteryTimeRemaining / 1000);
            printWriter.println(sb4.toString());
        }
        if (jComputeChargeTimeRemaining >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Charge time remaining: ");
            formatTimeMs(sb4, jComputeChargeTimeRemaining / 1000);
            printWriter.println(sb4.toString());
        }
        int i34 = i;
        long uahDischarge = batteryStats.getUahDischarge(i34);
        if (uahDischarge >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Discharge: ");
            sb4.append(BatteryStatsHelper.makemAh(uahDischarge / 1000.0d));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        long uahDischargeScreenOff = batteryStats.getUahDischargeScreenOff(i34);
        if (uahDischargeScreenOff >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Screen off discharge: ");
            sb4.append(BatteryStatsHelper.makemAh(uahDischargeScreenOff / 1000.0d));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        long uahDischargeScreenDoze = batteryStats.getUahDischargeScreenDoze(i34);
        if (uahDischargeScreenDoze >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Screen doze discharge: ");
            sb4.append(BatteryStatsHelper.makemAh(uahDischargeScreenDoze / 1000.0d));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        long j41 = uahDischarge - uahDischargeScreenOff;
        if (j41 >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Screen on discharge: ");
            sb4.append(BatteryStatsHelper.makemAh(j41 / 1000.0d));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        long uahDischargeLightDoze = batteryStats.getUahDischargeLightDoze(i34);
        if (uahDischargeLightDoze >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Device light doze discharge: ");
            sb4.append(BatteryStatsHelper.makemAh(uahDischargeLightDoze / 1000.0d));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        long uahDischargeDeepDoze = batteryStats.getUahDischargeDeepDoze(i34);
        if (uahDischargeDeepDoze >= 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Device deep doze discharge: ");
            sb4.append(BatteryStatsHelper.makemAh(uahDischargeDeepDoze / 1000.0d));
            sb4.append(" mAh");
            printWriter.println(sb4.toString());
        }
        printWriter.print("  Start clock time: ");
        printWriter.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getStartClockTime()).toString());
        long screenOnTime = batteryStats.getScreenOnTime(jElapsedRealtime, i34);
        long interactiveTime = batteryStats.getInteractiveTime(jElapsedRealtime, i34);
        long powerSaveModeEnabledTime = batteryStats.getPowerSaveModeEnabledTime(jElapsedRealtime, i34);
        long deviceIdleModeTime = batteryStats.getDeviceIdleModeTime(1, jElapsedRealtime, i34);
        int i35 = 1;
        long deviceIdleModeTime2 = batteryStats.getDeviceIdleModeTime(2, jElapsedRealtime, i34);
        long deviceIdlingTime = batteryStats.getDeviceIdlingTime(1, jElapsedRealtime, i34);
        long deviceIdlingTime2 = batteryStats.getDeviceIdlingTime(2, jElapsedRealtime, i34);
        long phoneOnTime = batteryStats.getPhoneOnTime(jElapsedRealtime, i34);
        batteryStats.getGlobalWifiRunningTime(jElapsedRealtime, i34);
        batteryStats.getWifiOnTime(jElapsedRealtime, i34);
        sb4.setLength(i3);
        sb4.append(str);
        sb4.append("  Screen on: ");
        formatTimeMs(sb4, screenOnTime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(screenOnTime, jComputeBatteryRealtime));
        sb4.append(") ");
        sb4.append(batteryStats.getScreenOnCount(i34));
        sb4.append("x, Interactive: ");
        formatTimeMs(sb4, interactiveTime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(interactiveTime, jComputeBatteryRealtime));
        sb4.append(")");
        printWriter.println(sb4.toString());
        sb4.setLength(i3);
        sb4.append(str);
        sb4.append("  Screen brightnesses:");
        int i36 = i3;
        int i37 = i36;
        while (i36 < 5) {
            long screenBrightnessTime = batteryStats.getScreenBrightnessTime(i36, jElapsedRealtime, i34);
            if (screenBrightnessTime != 0) {
                sb4.append("\n    ");
                sb4.append(str);
                sb4.append(SCREEN_BRIGHTNESS_NAMES[i36]);
                sb4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb4, screenBrightnessTime / 1000);
                sb4.append("(");
                sb4.append(batteryStats.formatRatioLocked(screenBrightnessTime, screenOnTime));
                sb4.append(")");
                i37 = 1;
            }
            i36++;
        }
        if (i37 == 0) {
            sb4.append(" (no activity)");
        }
        printWriter.println(sb4.toString());
        if (powerSaveModeEnabledTime != 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Power save mode enabled: ");
            formatTimeMs(sb4, powerSaveModeEnabledTime / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(powerSaveModeEnabledTime, jComputeBatteryRealtime));
            sb4.append(")");
            printWriter.println(sb4.toString());
        }
        if (deviceIdlingTime != 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Device light idling: ");
            formatTimeMs(sb4, deviceIdlingTime / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(deviceIdlingTime, jComputeBatteryRealtime));
            sb4.append(") ");
            sb4.append(batteryStats.getDeviceIdlingCount(1, i34));
            sb4.append("x");
            printWriter.println(sb4.toString());
        }
        if (deviceIdleModeTime != 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Idle mode light time: ");
            formatTimeMs(sb4, deviceIdleModeTime / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(deviceIdleModeTime, jComputeBatteryRealtime));
            sb4.append(") ");
            sb4.append(batteryStats.getDeviceIdleModeCount(1, i34));
            sb4.append("x");
            sb4.append(" -- longest ");
            formatTimeMs(sb4, batteryStats.getLongestDeviceIdleModeTime(1));
            printWriter.println(sb4.toString());
        }
        if (deviceIdlingTime2 != 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Device full idling: ");
            formatTimeMs(sb4, deviceIdlingTime2 / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(deviceIdlingTime2, jComputeBatteryRealtime));
            sb4.append(") ");
            sb4.append(batteryStats.getDeviceIdlingCount(2, i34));
            sb4.append("x");
            printWriter.println(sb4.toString());
        }
        if (deviceIdleModeTime2 != 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Idle mode full time: ");
            formatTimeMs(sb4, deviceIdleModeTime2 / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(deviceIdleModeTime2, jComputeBatteryRealtime));
            sb4.append(") ");
            sb4.append(batteryStats.getDeviceIdleModeCount(2, i34));
            sb4.append("x");
            sb4.append(" -- longest ");
            formatTimeMs(sb4, batteryStats.getLongestDeviceIdleModeTime(2));
            printWriter.println(sb4.toString());
        }
        if (phoneOnTime != 0) {
            sb4.setLength(i3);
            sb4.append(str);
            sb4.append("  Active phone call: ");
            formatTimeMs(sb4, phoneOnTime / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(phoneOnTime, jComputeBatteryRealtime));
            sb4.append(") ");
            sb4.append(batteryStats.getPhoneOnCount(i34));
            sb4.append("x");
        }
        int numConnectivityChange = batteryStats.getNumConnectivityChange(i34);
        if (numConnectivityChange != 0) {
            printWriter.print(str);
            printWriter.print("  Connectivity changes: ");
            printWriter.println(numConnectivityChange);
        }
        ArrayList arrayList = new ArrayList();
        long totalTimeLocked = 0;
        long j42 = 0;
        int i38 = i3;
        while (true) {
            i5 = i4;
            if (i38 >= i5) {
                break;
            }
            SparseArray<? extends Uid> sparseArray5 = sparseArray4;
            Uid uidValueAt = sparseArray5.valueAt(i38);
            long j43 = jComputeBatteryRealtime;
            ArrayMap<String, ? extends Uid.Wakelock> wakelockStats = uidValueAt.getWakelockStats();
            long j44 = j42;
            int i39 = i35;
            int size8 = wakelockStats.size() - i39;
            while (size8 >= 0) {
                Uid.Wakelock wakelockValueAt = wakelockStats.valueAt(size8);
                Timer wakeTime = wakelockValueAt.getWakeTime(i39);
                int i40 = i39;
                if (wakeTime != null) {
                    totalTimeLocked += wakeTime.getTotalTimeLocked(jElapsedRealtime, i34);
                }
                Timer wakeTime2 = wakelockValueAt.getWakeTime(i3);
                if (wakeTime2 != null) {
                    long totalTimeLocked2 = wakeTime2.getTotalTimeLocked(jElapsedRealtime, i34);
                    if (totalTimeLocked2 > 0) {
                        if (i2 < 0) {
                            arrayList.add(new TimerEntry(wakelockStats.keyAt(size8), uidValueAt.getUid(), wakeTime2, totalTimeLocked2));
                        }
                        j44 += totalTimeLocked2;
                    }
                }
                size8--;
                i39 = i40;
            }
            i35 = i39;
            i38++;
            i4 = i5;
            sparseArray4 = sparseArray5;
            jComputeBatteryRealtime = j43;
            j42 = j44;
        }
        long j45 = j42;
        long j46 = jComputeBatteryRealtime;
        SparseArray<? extends Uid> sparseArray6 = sparseArray4;
        int i41 = i3;
        long networkActivityBytes = batteryStats.getNetworkActivityBytes(i41, i34);
        long j47 = totalTimeLocked;
        int i42 = i35;
        long networkActivityBytes2 = batteryStats.getNetworkActivityBytes(i42, i34);
        long networkActivityBytes3 = batteryStats.getNetworkActivityBytes(2, i34);
        long networkActivityBytes4 = batteryStats.getNetworkActivityBytes(3, i34);
        long networkActivityPackets = batteryStats.getNetworkActivityPackets(i41, i34);
        long networkActivityPackets2 = batteryStats.getNetworkActivityPackets(i42, i34);
        long networkActivityPackets3 = batteryStats.getNetworkActivityPackets(2, i34);
        long networkActivityPackets4 = batteryStats.getNetworkActivityPackets(3, i34);
        long networkActivityBytes5 = batteryStats.getNetworkActivityBytes(4, i34);
        long networkActivityBytes6 = batteryStats.getNetworkActivityBytes(5, i34);
        if (j47 != 0) {
            sb4.setLength(i41);
            sb4.append(str);
            sb4.append("  Total full wakelock time: ");
            j2 = networkActivityBytes6;
            formatTimeMsNoSpace(sb4, (j47 + 500) / 1000);
            printWriter2 = printWriter;
            printWriter2.println(sb4.toString());
        } else {
            j2 = networkActivityBytes6;
            printWriter2 = printWriter;
        }
        if (j45 != 0) {
            sb4.setLength(i41);
            sb4.append(str);
            sb4.append("  Total partial wakelock time: ");
            j3 = networkActivityPackets;
            formatTimeMsNoSpace(sb4, (j45 + 500) / 1000);
            printWriter2.println(sb4.toString());
        } else {
            j3 = networkActivityPackets;
        }
        long wifiMulticastWakelockTime = batteryStats.getWifiMulticastWakelockTime(jElapsedRealtime, i34);
        int wifiMulticastWakelockCount = batteryStats.getWifiMulticastWakelockCount(i34);
        if (wifiMulticastWakelockTime != 0) {
            sb4.setLength(i41);
            sb4.append(str);
            sb4.append("  Total WiFi Multicast wakelock Count: ");
            sb4.append(wifiMulticastWakelockCount);
            printWriter2.println(sb4.toString());
            sb4.setLength(i41);
            sb4.append(str);
            sb4.append("  Total WiFi Multicast wakelock time: ");
            formatTimeMsNoSpace(sb4, (wifiMulticastWakelockTime + 500) / 1000);
            printWriter2.println(sb4.toString());
        }
        printWriter2.println("");
        printWriter.print(str);
        sb4.setLength(i41);
        sb4.append(str);
        sb4.append("  CONNECTIVITY POWER SUMMARY START");
        printWriter2.println(sb4.toString());
        printWriter.print(str);
        sb4.setLength(i41);
        sb4.append(str);
        sb4.append("  Logging duration for connectivity statistics: ");
        formatTimeMs(sb4, j39);
        printWriter2.println(sb4.toString());
        sb4.setLength(i41);
        sb4.append(str);
        sb4.append("  Cellular Statistics:");
        printWriter2.println(sb4.toString());
        printWriter.print(str);
        sb4.setLength(i41);
        sb4.append(str);
        sb4.append("     Cellular kernel active time: ");
        long mobileRadioActiveTime = batteryStats.getMobileRadioActiveTime(jElapsedRealtime, i34);
        long j48 = jElapsedRealtime;
        formatTimeMs(sb4, mobileRadioActiveTime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(mobileRadioActiveTime, j46));
        sb4.append(")");
        printWriter2.println(sb4.toString());
        printWriter2.print("     Cellular data received: ");
        printWriter2.println(batteryStats.formatBytesLocked(networkActivityBytes));
        printWriter2.print("     Cellular data sent: ");
        printWriter2.println(batteryStats.formatBytesLocked(networkActivityBytes2));
        printWriter2.print("     Cellular packets received: ");
        printWriter2.println(j3);
        printWriter2.print("     Cellular packets sent: ");
        printWriter2.println(networkActivityPackets2);
        sb4.setLength(i41);
        sb4.append(str);
        sb4.append("     Cellular Radio Access Technology:");
        int i43 = i41;
        int i44 = i43;
        while (i43 < 21) {
            long j49 = mobileRadioActiveTime;
            long j50 = j48;
            long phoneDataConnectionTime = batteryStats.getPhoneDataConnectionTime(i43, j50, i34);
            if (phoneDataConnectionTime != 0) {
                sb4.append("\n       ");
                sb4.append(str);
                sb4.append(DATA_CONNECTION_NAMES[i43]);
                sb4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb4, phoneDataConnectionTime / 1000);
                sb4.append("(");
                sb4.append(batteryStats.formatRatioLocked(phoneDataConnectionTime, j46));
                sb4.append(") ");
                i44 = i42;
            }
            i43++;
            j48 = j50;
            mobileRadioActiveTime = j49;
        }
        long j51 = mobileRadioActiveTime;
        long j52 = j48;
        if (i44 == 0) {
            sb4.append(" (no activity)");
        }
        printWriter2.println(sb4.toString());
        int i45 = i41;
        sb4.setLength(i45);
        sb4.append(str);
        sb4.append("     Cellular Rx signal strength (RSRP):");
        String[] strArr2 = {"very poor (less than -128dBm): ", "poor (-128dBm to -118dBm): ", "moderate (-118dBm to -108dBm): ", "good (-108dBm to -98dBm): ", "great (greater than -98dBm): "};
        int iMin = Math.min(5, strArr2.length);
        int i46 = i45;
        int i47 = i46;
        while (i46 < iMin) {
            int i48 = i45;
            int i49 = i47;
            long phoneSignalStrengthTime = batteryStats.getPhoneSignalStrengthTime(i46, j52, i34);
            if (phoneSignalStrengthTime == 0) {
                strArr = strArr2;
                i33 = iMin;
                i47 = i49;
            } else {
                sb4.append("\n       ");
                sb4.append(str);
                sb4.append(strArr2[i46]);
                sb4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                strArr = strArr2;
                i33 = iMin;
                formatTimeMs(sb4, phoneSignalStrengthTime / 1000);
                sb4.append("(");
                sb4.append(batteryStats.formatRatioLocked(phoneSignalStrengthTime, j46));
                sb4.append(") ");
                i47 = i42;
            }
            i46++;
            i45 = i48;
            strArr2 = strArr;
            iMin = i33;
        }
        boolean z5 = i45;
        if (i47 == 0) {
            sb4.append(" (no activity)");
        }
        printWriter2.println(sb4.toString());
        long j53 = batteryUptime;
        int i50 = i42;
        SparseArray<? extends Uid> sparseArray7 = sparseArray6;
        long j54 = j38;
        long j55 = j51;
        batteryStats.printControllerActivity(printWriter2, sb4, str, CELLULAR_CONTROLLER_NAME, getModemControllerActivity(), i34);
        printWriter.print(str);
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("  Wifi Statistics:");
        printWriter2.println(sb4.toString());
        printWriter.print(str);
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("     Wifi kernel active time: ");
        long j56 = j52;
        long wifiActiveTime = batteryStats.getWifiActiveTime(j56, i34);
        formatTimeMs(sb4, wifiActiveTime / 1000);
        sb4.append("(");
        sb4.append(batteryStats.formatRatioLocked(wifiActiveTime, j46));
        sb4.append(")");
        printWriter2.println(sb4.toString());
        printWriter2.print("     Wifi data received: ");
        printWriter2.println(batteryStats.formatBytesLocked(networkActivityBytes3));
        printWriter2.print("     Wifi data sent: ");
        printWriter2.println(batteryStats.formatBytesLocked(networkActivityBytes4));
        printWriter2.print("     Wifi packets received: ");
        printWriter2.println(networkActivityPackets3);
        printWriter2.print("     Wifi packets sent: ");
        printWriter2.println(networkActivityPackets4);
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("     Wifi states:");
        int i51 = z5 ? 1 : 0;
        int i52 = i51;
        while (i51 < 8) {
            long wifiStateTime = batteryStats.getWifiStateTime(i51, j56, i34);
            if (wifiStateTime == 0) {
                j37 = j56;
            } else {
                sb4.append("\n       ");
                sb4.append(WIFI_STATE_NAMES[i51]);
                sb4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                j37 = j56;
                formatTimeMs(sb4, wifiStateTime / 1000);
                sb4.append("(");
                sb4.append(batteryStats.formatRatioLocked(wifiStateTime, j46));
                sb4.append(") ");
                i52 = i50;
            }
            i51++;
            j56 = j37;
        }
        long j57 = j56;
        if (i52 == 0) {
            sb4.append(" (no activity)");
        }
        printWriter2.println(sb4.toString());
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("     Wifi supplicant states:");
        int i53 = z5 ? 1 : 0;
        int i54 = i53;
        while (i53 < 13) {
            long j58 = j57;
            long wifiSupplStateTime = batteryStats.getWifiSupplStateTime(i53, j58, i34);
            if (wifiSupplStateTime == 0) {
                j36 = j58;
            } else {
                sb4.append("\n       ");
                sb4.append(WIFI_SUPPL_STATE_NAMES[i53]);
                sb4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                j36 = j58;
                formatTimeMs(sb4, wifiSupplStateTime / 1000);
                sb4.append("(");
                sb4.append(batteryStats.formatRatioLocked(wifiSupplStateTime, j46));
                sb4.append(") ");
                i54 = i50;
            }
            i53++;
            j57 = j36;
        }
        long j59 = j57;
        if (i54 == 0) {
            sb4.append(" (no activity)");
        }
        printWriter2.println(sb4.toString());
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("     Wifi Rx signal strength (RSSI):");
        String[] strArr3 = {"very poor (less than -88.75dBm): ", "poor (-88.75 to -77.5dBm): ", "moderate (-77.5dBm to -66.25dBm): ", "good (-66.25dBm to -55dBm): ", "great (greater than -55dBm): "};
        int iMin2 = Math.min(5, strArr3.length);
        int i55 = z5 ? 1 : 0;
        int i56 = i55;
        while (i55 < iMin2) {
            SparseArray<? extends Uid> sparseArray8 = sparseArray7;
            long j60 = j59;
            long wifiSignalStrengthTime = batteryStats.getWifiSignalStrengthTime(i55, j60, i34);
            if (wifiSignalStrengthTime != 0) {
                sb4.append("\n    ");
                sb4.append(str);
                sb4.append("     ");
                sb4.append(strArr3[i55]);
                formatTimeMs(sb4, wifiSignalStrengthTime / 1000);
                sb4.append("(");
                batteryStats = this;
                sb4.append(batteryStats.formatRatioLocked(wifiSignalStrengthTime, j46));
                sb4.append(") ");
                i56 = i50;
            }
            i55++;
            j59 = j60;
            sparseArray7 = sparseArray8;
        }
        SparseArray<? extends Uid> sparseArray9 = sparseArray7;
        long j61 = j59;
        if (i56 == 0) {
            sb4.append(" (no activity)");
        }
        PrintWriter printWriter5 = printWriter;
        printWriter5.println(sb4.toString());
        long j62 = j61;
        batteryStats.printControllerActivity(printWriter5, sb4, str, WIFI_CONTROLLER_NAME, getWifiControllerActivity(), i34);
        printWriter.print(str);
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("  GPS Statistics:");
        printWriter5.println(sb4.toString());
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("     GPS signal quality (Top 4 Average CN0):");
        String[] strArr4 = {"poor (less than 20 dBHz): ", "good (greater than 20 dBHz): "};
        int iMin3 = Math.min(2, strArr4.length);
        int i57 = z5 ? 1 : 0;
        while (i57 < iMin3) {
            long gpsSignalQualityTime = batteryStats.getGpsSignalQualityTime(i57, j62, i34);
            sb4.append("\n    ");
            sb4.append(str);
            sb4.append("  ");
            sb4.append(strArr4[i57]);
            formatTimeMs(sb4, gpsSignalQualityTime / 1000);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(gpsSignalQualityTime, j46));
            sb4.append(") ");
            i57++;
            i34 = i;
        }
        long j63 = j46;
        printWriter5.println(sb4.toString());
        long gpsBatteryDrainMaMs = getGpsBatteryDrainMaMs();
        if (gpsBatteryDrainMaMs > 0) {
            printWriter.print(str);
            sb4.setLength(z5 ? 1 : 0);
            sb4.append(str);
            sb4.append("     Battery Drain (mAh): ");
            sb4.append(Double.toString(gpsBatteryDrainMaMs / 3600000.0d));
            printWriter5.println(sb4.toString());
        }
        printWriter.print(str);
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("  CONNECTIVITY POWER SUMMARY END");
        printWriter5.println(sb4.toString());
        printWriter5.println("");
        printWriter.print(str);
        printWriter5.print("  Bluetooth total received: ");
        printWriter5.print(batteryStats.formatBytesLocked(networkActivityBytes5));
        printWriter5.print(", sent: ");
        printWriter5.println(batteryStats.formatBytesLocked(j2));
        long bluetoothScanTime = batteryStats.getBluetoothScanTime(j62, i) / 1000;
        sb4.setLength(z5 ? 1 : 0);
        sb4.append(str);
        sb4.append("  Bluetooth scan time: ");
        formatTimeMs(sb4, bluetoothScanTime);
        printWriter5.println(sb4.toString());
        batteryStats.printControllerActivity(printWriter5, sb4, str, "Bluetooth", getBluetoothControllerActivity(), i);
        printWriter.println();
        if (i == 2) {
            if (getIsOnBattery()) {
                printWriter.print(str);
                printWriter5.println("  Device is currently unplugged");
                printWriter.print(str);
                printWriter5.print("    Discharge cycle start level: ");
                printWriter5.println(getDischargeStartLevel());
                printWriter.print(str);
                printWriter5.print("    Discharge cycle current level: ");
                printWriter5.println(getDischargeCurrentLevel());
            } else {
                printWriter.print(str);
                printWriter5.println("  Device is currently plugged into power");
                printWriter.print(str);
                printWriter5.print("    Last discharge cycle start level: ");
                printWriter5.println(getDischargeStartLevel());
                printWriter.print(str);
                printWriter5.print("    Last discharge cycle end level: ");
                printWriter5.println(getDischargeCurrentLevel());
            }
            printWriter.print(str);
            printWriter5.print("    Amount discharged while screen on: ");
            printWriter5.println(getDischargeAmountScreenOn());
            printWriter.print(str);
            printWriter5.print("    Amount discharged while screen off: ");
            printWriter5.println(getDischargeAmountScreenOff());
            printWriter.print(str);
            printWriter5.print("    Amount discharged while screen doze: ");
            printWriter5.println(getDischargeAmountScreenDoze());
            printWriter5.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        } else {
            printWriter.print(str);
            printWriter5.println("  Device battery use since last full charge");
            printWriter.print(str);
            printWriter5.print("    Amount discharged (lower bound): ");
            printWriter5.println(getLowDischargeAmountSinceCharge());
            printWriter.print(str);
            printWriter5.print("    Amount discharged (upper bound): ");
            printWriter5.println(getHighDischargeAmountSinceCharge());
            printWriter.print(str);
            printWriter5.print("    Amount discharged while screen on: ");
            printWriter5.println(getDischargeAmountScreenOnSinceCharge());
            printWriter.print(str);
            printWriter5.print("    Amount discharged while screen off: ");
            printWriter5.println(getDischargeAmountScreenOffSinceCharge());
            printWriter.print(str);
            printWriter5.print("    Amount discharged while screen doze: ");
            printWriter5.println(getDischargeAmountScreenDozeSinceCharge());
            printWriter.println();
        }
        BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, z5, z);
        batteryStatsHelper.create(batteryStats);
        batteryStatsHelper.refreshStats(i, -1);
        List<BatterySipper> usageList = batteryStatsHelper.getUsageList();
        if (usageList != null && usageList.size() > 0) {
            printWriter.print(str);
            printWriter5.println("  Estimated power use (mAh):");
            printWriter.print(str);
            printWriter5.print("    Capacity: ");
            batteryStats.printmAh(printWriter5, batteryStatsHelper.getPowerProfile().getBatteryCapacity());
            printWriter5.print(", Computed drain: ");
            batteryStats.printmAh(printWriter5, batteryStatsHelper.getComputedPower());
            printWriter5.print(", actual drain: ");
            batteryStats.printmAh(printWriter5, batteryStatsHelper.getMinDrainedPower());
            if (batteryStatsHelper.getMinDrainedPower() != batteryStatsHelper.getMaxDrainedPower()) {
                printWriter5.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                batteryStats.printmAh(printWriter5, batteryStatsHelper.getMaxDrainedPower());
            }
            printWriter.println();
            int i58 = z5 ? 1 : 0;
            while (i58 < usageList.size()) {
                BatterySipper batterySipper = usageList.get(i58);
                printWriter.print(str);
                switch (batterySipper.drainType) {
                    case AMBIENT_DISPLAY:
                        printWriter5.print("    Ambient display: ");
                        break;
                    case IDLE:
                        printWriter5.print("    Idle: ");
                        break;
                    case CELL:
                        printWriter5.print("    Cell standby: ");
                        break;
                    case PHONE:
                        printWriter5.print("    Phone calls: ");
                        break;
                    case WIFI:
                        printWriter5.print("    Wifi: ");
                        break;
                    case BLUETOOTH:
                        printWriter5.print("    Bluetooth: ");
                        break;
                    case SCREEN:
                        printWriter5.print("    Screen: ");
                        break;
                    case FLASHLIGHT:
                        printWriter5.print("    Flashlight: ");
                        break;
                    case APP:
                        printWriter5.print("    Uid ");
                        UserHandle.formatUid(printWriter5, batterySipper.uidObj.getUid());
                        printWriter5.print(": ");
                        break;
                    case USER:
                        printWriter5.print("    User ");
                        printWriter5.print(batterySipper.userId);
                        printWriter5.print(": ");
                        break;
                    case UNACCOUNTED:
                        printWriter5.print("    Unaccounted: ");
                        break;
                    case OVERCOUNTED:
                        printWriter5.print("    Over-counted: ");
                        break;
                    case CAMERA:
                        printWriter5.print("    Camera: ");
                        break;
                    default:
                        printWriter5.print("    ???: ");
                        break;
                }
                batteryStats.printmAh(printWriter5, batterySipper.totalPowerMah);
                long j64 = j62;
                if (batterySipper.usagePowerMah != batterySipper.totalPowerMah) {
                    printWriter5.print(" (");
                    if (batterySipper.usagePowerMah != 0.0d) {
                        printWriter5.print(" usage=");
                        batteryStats.printmAh(printWriter5, batterySipper.usagePowerMah);
                    }
                    if (batterySipper.cpuPowerMah != 0.0d) {
                        printWriter5.print(" cpu=");
                        batteryStats.printmAh(printWriter5, batterySipper.cpuPowerMah);
                    }
                    if (batterySipper.wakeLockPowerMah != 0.0d) {
                        printWriter5.print(" wake=");
                        batteryStats.printmAh(printWriter5, batterySipper.wakeLockPowerMah);
                    }
                    if (batterySipper.mobileRadioPowerMah != 0.0d) {
                        printWriter5.print(" radio=");
                        batteryStats.printmAh(printWriter5, batterySipper.mobileRadioPowerMah);
                    }
                    if (batterySipper.wifiPowerMah != 0.0d) {
                        printWriter5.print(" wifi=");
                        batteryStats.printmAh(printWriter5, batterySipper.wifiPowerMah);
                    }
                    if (batterySipper.bluetoothPowerMah != 0.0d) {
                        printWriter5.print(" bt=");
                        batteryStats.printmAh(printWriter5, batterySipper.bluetoothPowerMah);
                    }
                    if (batterySipper.gpsPowerMah != 0.0d) {
                        printWriter5.print(" gps=");
                        batteryStats.printmAh(printWriter5, batterySipper.gpsPowerMah);
                    }
                    if (batterySipper.sensorPowerMah != 0.0d) {
                        printWriter5.print(" sensor=");
                        batteryStats.printmAh(printWriter5, batterySipper.sensorPowerMah);
                    }
                    if (batterySipper.cameraPowerMah != 0.0d) {
                        printWriter5.print(" camera=");
                        batteryStats.printmAh(printWriter5, batterySipper.cameraPowerMah);
                    }
                    if (batterySipper.flashlightPowerMah != 0.0d) {
                        printWriter5.print(" flash=");
                        batteryStats.printmAh(printWriter5, batterySipper.flashlightPowerMah);
                    }
                    printWriter5.print(" )");
                }
                if (batterySipper.totalSmearedPowerMah != batterySipper.totalPowerMah) {
                    printWriter5.print(" Including smearing: ");
                    batteryStats.printmAh(printWriter5, batterySipper.totalSmearedPowerMah);
                    printWriter5.print(" (");
                    if (batterySipper.screenPowerMah != 0.0d) {
                        printWriter5.print(" screen=");
                        batteryStats.printmAh(printWriter5, batterySipper.screenPowerMah);
                    }
                    if (batterySipper.proportionalSmearMah != 0.0d) {
                        printWriter5.print(" proportional=");
                        batteryStats.printmAh(printWriter5, batterySipper.proportionalSmearMah);
                    }
                    printWriter5.print(" )");
                }
                if (batterySipper.shouldHide) {
                    printWriter5.print(" Excluded from smearing");
                }
                printWriter.println();
                i58++;
                j62 = j64;
            }
            j4 = j62;
            printWriter.println();
        } else {
            j4 = j62;
        }
        List<BatterySipper> mobilemsppList = batteryStatsHelper.getMobilemsppList();
        if (mobilemsppList != null && mobilemsppList.size() > 0) {
            printWriter.print(str);
            printWriter5.println("  Per-app mobile ms per packet:");
            long j65 = 0;
            for (int i59 = z5 ? 1 : 0; i59 < mobilemsppList.size(); i59++) {
                BatterySipper batterySipper2 = mobilemsppList.get(i59);
                sb4.setLength(z5 ? 1 : 0);
                sb4.append(str);
                sb4.append("    Uid ");
                UserHandle.formatUid(sb4, batterySipper2.uidObj.getUid());
                sb4.append(": ");
                sb4.append(BatteryStatsHelper.makemAh(batterySipper2.mobilemspp));
                sb4.append(" (");
                sb4.append(batterySipper2.mobileRxPackets + batterySipper2.mobileTxPackets);
                sb4.append(" packets over ");
                formatTimeMsNoSpace(sb4, batterySipper2.mobileActive);
                sb4.append(") ");
                sb4.append(batterySipper2.mobileActiveCount);
                sb4.append("x");
                printWriter5.println(sb4.toString());
                j65 += batterySipper2.mobileActive;
            }
            sb4.setLength(z5 ? 1 : 0);
            sb4.append(str);
            sb4.append("    TOTAL TIME: ");
            formatTimeMs(sb4, j65);
            sb4.append("(");
            sb4.append(batteryStats.formatRatioLocked(j65, j63));
            sb4.append(")");
            printWriter5.println(sb4.toString());
            printWriter.println();
        }
        Comparator<TimerEntry> comparator = new Comparator<TimerEntry>() {
            @Override
            public int compare(TimerEntry timerEntry, TimerEntry timerEntry2) {
                long j66 = timerEntry.mTime;
                long j67 = timerEntry2.mTime;
                if (j66 < j67) {
                    return 1;
                }
                if (j66 > j67) {
                    return -1;
                }
                return 0;
            }
        };
        int i60 = i2;
        if (i60 < 0) {
            Map<String, ? extends Timer> kernelWakelockStats = getKernelWakelockStats();
            if (kernelWakelockStats.size() > 0) {
                ArrayList arrayList2 = new ArrayList();
                for (Map.Entry<String, ? extends Timer> entry : kernelWakelockStats.entrySet()) {
                    Timer value = entry.getValue();
                    long j66 = j4;
                    long jComputeWakeLock = computeWakeLock(value, j66, i);
                    if (jComputeWakeLock > 0) {
                        arrayList2.add(new TimerEntry(entry.getKey(), 0, value, jComputeWakeLock));
                    }
                    j4 = j66;
                }
                long j67 = j4;
                int i61 = i;
                if (arrayList2.size() > 0) {
                    Collections.sort(arrayList2, comparator);
                    printWriter.print(str);
                    printWriter5.println("  All kernel wake locks:");
                    int i62 = z5 ? 1 : 0;
                    while (i62 < arrayList2.size()) {
                        TimerEntry timerEntry = (TimerEntry) arrayList2.get(i62);
                        sb4.setLength(z5 ? 1 : 0);
                        sb4.append(str);
                        sb4.append("  Kernel Wake lock ");
                        sb4.append(timerEntry.mName);
                        int i63 = i62;
                        long j68 = j67;
                        if (!printWakeLock(sb4, timerEntry.mTimer, j67, null, i61, ": ").equals(": ")) {
                            sb4.append(" realtime");
                            printWriter5.println(sb4.toString());
                        }
                        i62 = (i63 == true ? 1 : 0) + 1;
                        i61 = i;
                        j67 = j68;
                    }
                    j5 = j67;
                    i7 = -1;
                    printWriter.println();
                    if (arrayList.size() > 0) {
                        Collections.sort(arrayList, comparator);
                        printWriter.print(str);
                        printWriter5.println("  All partial wake locks:");
                        int i64 = z5 ? 1 : 0;
                        while (i64 < arrayList.size()) {
                            TimerEntry timerEntry2 = (TimerEntry) arrayList.get(i64);
                            sb4.setLength(z5 ? 1 : 0);
                            sb4.append("  Wake lock ");
                            UserHandle.formatUid(sb4, timerEntry2.mId);
                            sb4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                            sb4.append(timerEntry2.mName);
                            printWakeLock(sb4, timerEntry2.mTimer, j5, null, i, ": ");
                            sb4.append(" realtime");
                            printWriter5.println(sb4.toString());
                            i64 = (i64 == true ? 1 : 0) + 1;
                        }
                        arrayList.clear();
                        printWriter.println();
                    }
                    wakeupReasonStats = getWakeupReasonStats();
                    if (wakeupReasonStats.size() <= 0) {
                        printWriter.print(str);
                        printWriter5.println("  All wakeup reasons:");
                        ArrayList arrayList3 = new ArrayList();
                        for (Map.Entry<String, ? extends Timer> entry2 : wakeupReasonStats.entrySet()) {
                            arrayList3.add(new TimerEntry(entry2.getKey(), 0, entry2.getValue(), r2.getCountLocked(i)));
                        }
                        int i65 = i;
                        Collections.sort(arrayList3, comparator);
                        int i66 = z5 ? 1 : 0;
                        while (i66 < arrayList3.size()) {
                            TimerEntry timerEntry3 = (TimerEntry) arrayList3.get(i66);
                            sb4.setLength(z5 ? 1 : 0);
                            sb4.append(str);
                            sb4.append("  Wakeup reason ");
                            sb4.append(timerEntry3.mName);
                            printWakeLock(sb4, timerEntry3.mTimer, j5, null, i65, ": ");
                            sb4.append(" realtime");
                            printWriter5.println(sb4.toString());
                            i66++;
                            i65 = i65;
                        }
                        i6 = i65;
                        printWriter.println();
                    } else {
                        i6 = i;
                    }
                } else {
                    j5 = j67;
                }
            } else {
                j5 = j4;
            }
            i7 = -1;
            if (arrayList.size() > 0) {
            }
            wakeupReasonStats = getWakeupReasonStats();
            if (wakeupReasonStats.size() <= 0) {
            }
        } else {
            j5 = j4;
            i6 = i;
            i7 = -1;
        }
        LongSparseArray<? extends Timer> kernelMemoryStats = getKernelMemoryStats();
        if (kernelMemoryStats.size() > 0) {
            printWriter5.println("  Memory Stats");
            for (int i67 = z5 ? 1 : 0; i67 < kernelMemoryStats.size(); i67++) {
                sb4.setLength(z5 ? 1 : 0);
                sb4.append("  Bandwidth ");
                sb4.append(kernelMemoryStats.keyAt(i67));
                sb4.append(" Time ");
                sb4.append(kernelMemoryStats.valueAt(i67).getTotalTimeLocked(j5, i6));
                printWriter5.println(sb4.toString());
            }
            i8 = z5 ? 1 : 0;
            j6 = j5;
            printWriter.println();
        } else {
            i8 = z5 ? 1 : 0;
            j6 = j5;
        }
        Map<String, ? extends Timer> rpmStats = getRpmStats();
        if (rpmStats.size() > 0) {
            printWriter.print(str);
            printWriter5.println("  Resource Power Manager Stats");
            if (rpmStats.size() > 0) {
                Iterator<Map.Entry<String, ? extends Timer>> it = rpmStats.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, ? extends Timer> next = it.next();
                    String key = next.getKey();
                    Timer value2 = next.getValue();
                    StringBuilder sb5 = sb4;
                    boolean z6 = i8 == true ? 1 : 0;
                    printTimer(printWriter5, sb5, value2, j6, i6, str, key);
                    printWriter5 = printWriter;
                    i60 = i60;
                    i6 = i6;
                    i8 = z6 ? 1 : 0;
                    sb4 = sb5;
                    j63 = j63;
                    j54 = j54;
                    it = it;
                    j6 = j6;
                }
            }
            j7 = j6;
            sb = sb4;
            j8 = j63;
            i9 = i6;
            i10 = i60;
            i11 = i5;
            j9 = j54;
            i12 = i8;
            sparseArray = sparseArray9;
            printWriter.println();
        } else {
            j7 = j6;
            sb = sb4;
            j8 = j63;
            i9 = i6;
            i10 = i60;
            i11 = i5;
            j9 = j54;
            i12 = i8 == true ? 1 : 0;
            sparseArray = sparseArray9;
        }
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            sb.setLength(i12);
            sb.append("  CPU freqs:");
            for (int i68 = i12; i68 < cpuFreqs.length; i68++) {
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + cpuFreqs[i68]);
            }
            printWriter3 = printWriter;
            printWriter3.println(sb.toString());
            printWriter.println();
        } else {
            printWriter3 = printWriter;
        }
        int i69 = i12;
        while (i69 < i11) {
            int iKeyAt = sparseArray.keyAt(i69);
            if (i10 >= 0 && iKeyAt != i10) {
                if (iKeyAt != 1000) {
                    i14 = i11;
                    sparseArray2 = sparseArray;
                    i26 = i9;
                    sb3 = sb;
                    i21 = i69;
                    printWriter4 = printWriter3;
                    j28 = j53;
                    i20 = i50;
                    j24 = j55;
                    j23 = j8;
                    j26 = j9;
                    j27 = j7;
                }
                i69 = i21 + 1;
                i10 = i2;
                sb = sb3;
                printWriter3 = printWriter4;
                i9 = i26;
                j53 = j28;
                i11 = i14;
                sparseArray = sparseArray2;
                j8 = j23;
                i50 = i20;
                j55 = j24;
                j9 = j26;
                j7 = j27;
                batteryStats = this;
            }
            Uid uidValueAt2 = sparseArray.valueAt(i69);
            printWriter.print(str);
            printWriter3.print("  ");
            UserHandle.formatUid(printWriter3, iKeyAt);
            printWriter3.println(SettingsStringUtil.DELIMITER);
            long networkActivityBytes7 = uidValueAt2.getNetworkActivityBytes(i12 == true ? 1 : 0, i9);
            int i70 = i50;
            long networkActivityBytes8 = uidValueAt2.getNetworkActivityBytes(i70, i9);
            boolean z7 = i12 == true ? 1 : 0;
            StringBuilder sb6 = sb;
            long networkActivityBytes9 = uidValueAt2.getNetworkActivityBytes(2, i9);
            long networkActivityBytes10 = uidValueAt2.getNetworkActivityBytes(3, i9);
            long networkActivityBytes11 = uidValueAt2.getNetworkActivityBytes(4, i9);
            long networkActivityBytes12 = uidValueAt2.getNetworkActivityBytes(5, i9);
            int i71 = i11;
            long networkActivityPackets5 = uidValueAt2.getNetworkActivityPackets(z7 ? 1 : 0, i9);
            long networkActivityPackets6 = uidValueAt2.getNetworkActivityPackets(i70, i9);
            long networkActivityPackets7 = uidValueAt2.getNetworkActivityPackets(2, i9);
            long networkActivityPackets8 = uidValueAt2.getNetworkActivityPackets(3, i9);
            SparseArray<? extends Uid> sparseArray10 = sparseArray;
            long mobileRadioActiveTime2 = uidValueAt2.getMobileRadioActiveTime(i9);
            int mobileRadioActiveCount = uidValueAt2.getMobileRadioActiveCount(i9);
            long j69 = j7;
            long fullWifiLockTime = uidValueAt2.getFullWifiLockTime(j69, i9);
            long wifiScanTime = uidValueAt2.getWifiScanTime(j69, i9);
            int i72 = i69;
            int wifiScanCount = uidValueAt2.getWifiScanCount(i9);
            int wifiScanBackgroundCount = uidValueAt2.getWifiScanBackgroundCount(i9);
            long wifiScanActualTime = uidValueAt2.getWifiScanActualTime(j69);
            long wifiScanBackgroundTime = uidValueAt2.getWifiScanBackgroundTime(j69);
            long wifiRunningTime = uidValueAt2.getWifiRunningTime(j69, i9);
            long mobileRadioApWakeupCount = uidValueAt2.getMobileRadioApWakeupCount(i9);
            long wifiRadioApWakeupCount = uidValueAt2.getWifiRadioApWakeupCount(i9);
            if (networkActivityBytes7 > 0 || networkActivityBytes8 > 0 || networkActivityPackets5 > 0 || networkActivityPackets6 > 0) {
                printWriter.print(str);
                printWriter3.print("    Mobile network: ");
                j10 = wifiRadioApWakeupCount;
                printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes7));
                printWriter3.print(" received, ");
                printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes8));
                printWriter3.print(" sent (packets ");
                printWriter3.print(networkActivityPackets5);
                printWriter3.print(" received, ");
                printWriter3.print(networkActivityPackets6);
                printWriter3.println(" sent)");
            } else {
                j10 = wifiRadioApWakeupCount;
            }
            if (mobileRadioActiveTime2 > 0 || mobileRadioActiveCount > 0) {
                sb2 = sb6;
                sb2.setLength(z7 ? 1 : 0);
                str2 = str;
                sb2.append(str2);
                sb2.append("    Mobile radio active: ");
                long j70 = mobileRadioActiveTime2 / 1000;
                formatTimeMs(sb2, j70);
                sb2.append("(");
                j11 = mobileRadioApWakeupCount;
                uid = uidValueAt2;
                i13 = wifiScanBackgroundCount;
                j12 = j55;
                sb2.append(batteryStats.formatRatioLocked(mobileRadioActiveTime2, j12));
                sb2.append(") ");
                sb2.append(mobileRadioActiveCount);
                sb2.append("x");
                long j71 = networkActivityPackets5 + networkActivityPackets6;
                if (j71 == 0) {
                    j71 = 1;
                }
                sb2.append(" @ ");
                sb2.append(BatteryStatsHelper.makemAh(j70 / j71));
                sb2.append(" mspp");
                printWriter3.println(sb2.toString());
            } else {
                str2 = str;
                j11 = mobileRadioApWakeupCount;
                uid = uidValueAt2;
                i13 = wifiScanBackgroundCount;
                j12 = j55;
                sb2 = sb6;
            }
            if (j11 > 0) {
                boolean z8 = z7 ? 1 : 0;
                sb2.setLength(z8 ? 1 : 0);
                sb2.append(str2);
                sb2.append("    Mobile radio AP wakeups: ");
                sb2.append(j11);
                printWriter3.println(sb2.toString());
                z2 = z8;
            } else {
                z2 = z7 ? 1 : 0;
            }
            Uid uid4 = uid;
            ControllerActivityCounter modemControllerActivity = uid4.getModemControllerActivity();
            i14 = i71;
            long j72 = j12;
            sparseArray2 = sparseArray10;
            StringBuilder sb7 = sb2;
            boolean z9 = z2 ? 1 : 0;
            batteryStats.printControllerActivityIfInteresting(printWriter3, sb2, str2 + "  ", CELLULAR_CONTROLLER_NAME, modemControllerActivity, i);
            if (networkActivityBytes9 > 0 || networkActivityBytes10 > 0 || networkActivityPackets7 > 0 || networkActivityPackets8 > 0) {
                printWriter.print(str);
                printWriter3.print("    Wi-Fi network: ");
                printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes9));
                printWriter3.print(" received, ");
                printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes10));
                printWriter3.print(" sent (packets ");
                printWriter3.print(networkActivityPackets7);
                printWriter3.print(" received, ");
                printWriter3.print(networkActivityPackets8);
                printWriter3.println(" sent)");
            }
            if (fullWifiLockTime == 0 && wifiScanTime == 0 && wifiScanCount == 0 && i13 == 0) {
                j13 = wifiScanActualTime;
                if (j13 == 0) {
                    j14 = wifiScanBackgroundTime;
                    if (j14 == 0) {
                        j15 = wifiRunningTime;
                        if (j15 == 0) {
                            j16 = j8;
                            j17 = j69;
                        }
                        if (j10 > 0) {
                            sb7.setLength(z9 ? 1 : 0);
                            sb7.append(str2);
                            sb7.append("    WiFi AP wakeups: ");
                            sb7.append(j10);
                            printWriter3.println(sb7.toString());
                        }
                        batteryStats.printControllerActivityIfInteresting(printWriter3, sb7, str2 + "  ", WIFI_CONTROLLER_NAME, uid4.getWifiControllerActivity(), i);
                        if (networkActivityBytes11 <= 0 || networkActivityBytes12 > 0) {
                            printWriter.print(str);
                            printWriter3.print("    Bluetooth network: ");
                            printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes11));
                            printWriter3.print(" received, ");
                            printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes12));
                            printWriter3.println(" sent");
                        }
                        bluetoothScanTimer = uid4.getBluetoothScanTimer();
                        if (bluetoothScanTimer == null) {
                            long totalTimeLocked3 = (bluetoothScanTimer.getTotalTimeLocked(j17, i) + 500) / 1000;
                            if (totalTimeLocked3 != 0) {
                                int countLocked4 = bluetoothScanTimer.getCountLocked(i);
                                Timer bluetoothScanBackgroundTimer = uid4.getBluetoothScanBackgroundTimer();
                                if (bluetoothScanBackgroundTimer != null) {
                                    countLocked = bluetoothScanBackgroundTimer.getCountLocked(i);
                                } else {
                                    countLocked = z9 ? 1 : 0;
                                }
                                j18 = j17;
                                long j73 = j9;
                                long totalDurationMsLocked8 = bluetoothScanTimer.getTotalDurationMsLocked(j73);
                                if (bluetoothScanBackgroundTimer != null) {
                                    timer = bluetoothScanBackgroundTimer;
                                    i32 = countLocked;
                                    totalDurationMsLocked5 = bluetoothScanBackgroundTimer.getTotalDurationMsLocked(j73);
                                } else {
                                    timer = bluetoothScanBackgroundTimer;
                                    i32 = countLocked;
                                    totalDurationMsLocked5 = 0;
                                }
                                if (uid4.getBluetoothScanResultCounter() != null) {
                                    countLocked2 = uid4.getBluetoothScanResultCounter().getCountLocked(i);
                                } else {
                                    countLocked2 = z9 ? 1 : 0;
                                }
                                if (uid4.getBluetoothScanResultBgCounter() != null) {
                                    countLocked3 = uid4.getBluetoothScanResultBgCounter().getCountLocked(i);
                                } else {
                                    countLocked3 = z9 ? 1 : 0;
                                }
                                Timer bluetoothUnoptimizedScanTimer = uid4.getBluetoothUnoptimizedScanTimer();
                                if (bluetoothUnoptimizedScanTimer != null) {
                                    totalDurationMsLocked6 = bluetoothUnoptimizedScanTimer.getTotalDurationMsLocked(j73);
                                } else {
                                    totalDurationMsLocked6 = 0;
                                }
                                if (bluetoothUnoptimizedScanTimer != null) {
                                    maxDurationMsLocked = bluetoothUnoptimizedScanTimer.getMaxDurationMsLocked(j73);
                                } else {
                                    maxDurationMsLocked = 0;
                                }
                                Timer bluetoothUnoptimizedScanBackgroundTimer = uid4.getBluetoothUnoptimizedScanBackgroundTimer();
                                if (bluetoothUnoptimizedScanBackgroundTimer != null) {
                                    totalDurationMsLocked7 = bluetoothUnoptimizedScanBackgroundTimer.getTotalDurationMsLocked(j73);
                                } else {
                                    totalDurationMsLocked7 = 0;
                                }
                                if (bluetoothUnoptimizedScanBackgroundTimer != null) {
                                    j19 = j73;
                                    maxDurationMsLocked2 = bluetoothUnoptimizedScanBackgroundTimer.getMaxDurationMsLocked(j73);
                                } else {
                                    j19 = j73;
                                    maxDurationMsLocked2 = 0;
                                }
                                uid2 = uid4;
                                sb7.setLength(z9 ? 1 : 0);
                                if (totalDurationMsLocked8 != totalTimeLocked3) {
                                    sb7.append(str2);
                                    sb7.append("    Bluetooth Scan (total blamed realtime): ");
                                    formatTimeMs(sb7, totalTimeLocked3);
                                    sb7.append(" (");
                                    sb7.append(countLocked4);
                                    sb7.append(" times)");
                                    if (bluetoothScanTimer.isRunningLocked()) {
                                        sb7.append(" (currently running)");
                                    }
                                    sb7.append("\n");
                                }
                                sb7.append(str2);
                                sb7.append("    Bluetooth Scan (total actual realtime): ");
                                formatTimeMs(sb7, totalDurationMsLocked8);
                                sb7.append(" (");
                                sb7.append(countLocked4);
                                sb7.append(" times)");
                                if (bluetoothScanTimer.isRunningLocked()) {
                                    sb7.append(" (currently running)");
                                }
                                sb7.append("\n");
                                if (totalDurationMsLocked5 > 0 || i32 > 0) {
                                    sb7.append(str2);
                                    sb7.append("    Bluetooth Scan (background realtime): ");
                                    formatTimeMs(sb7, totalDurationMsLocked5);
                                    sb7.append(" (");
                                    sb7.append(i32 == true ? 1 : 0);
                                    sb7.append(" times)");
                                    if (timer != null && timer.isRunningLocked()) {
                                        sb7.append(" (currently running in background)");
                                    }
                                    sb7.append("\n");
                                }
                                sb7.append(str2);
                                sb7.append("    Bluetooth Scan Results: ");
                                sb7.append(countLocked2);
                                sb7.append(" (");
                                sb7.append(countLocked3);
                                sb7.append(" in background)");
                                long j74 = totalDurationMsLocked6;
                                if (j74 > 0) {
                                    j35 = totalDurationMsLocked7;
                                } else {
                                    j35 = totalDurationMsLocked7;
                                    if (j35 > 0) {
                                    }
                                    printWriter4 = printWriter;
                                    printWriter4.println(sb7.toString());
                                    i15 = i70;
                                }
                                sb7.append("\n");
                                sb7.append(str2);
                                sb7.append("    Unoptimized Bluetooth Scan (realtime): ");
                                formatTimeMs(sb7, j74);
                                sb7.append(" (max ");
                                formatTimeMs(sb7, maxDurationMsLocked);
                                sb7.append(")");
                                if (bluetoothUnoptimizedScanTimer != null && bluetoothUnoptimizedScanTimer.isRunningLocked()) {
                                    sb7.append(" (currently running unoptimized)");
                                }
                                if (bluetoothUnoptimizedScanBackgroundTimer != null && j35 > 0) {
                                    sb7.append("\n");
                                    sb7.append(str2);
                                    sb7.append("    Unoptimized Bluetooth Scan (background realtime): ");
                                    formatTimeMs(sb7, j35);
                                    sb7.append(" (max ");
                                    formatTimeMs(sb7, maxDurationMsLocked2);
                                    sb7.append(")");
                                    if (bluetoothUnoptimizedScanBackgroundTimer.isRunningLocked()) {
                                        sb7.append(" (currently running unoptimized in background)");
                                    }
                                }
                                printWriter4 = printWriter;
                                printWriter4.println(sb7.toString());
                                i15 = i70;
                            } else {
                                uid2 = uid4;
                                j18 = j17;
                                printWriter4 = printWriter3;
                                j19 = j9;
                                i15 = z9 ? 1 : 0;
                            }
                            Uid uid5 = uid2;
                            if (uid5.hasUserActivity()) {
                                int i73 = z9 ? 1 : 0;
                                int i74 = i73;
                                while (i73 < 4) {
                                    int userActivityCount = uid5.getUserActivityCount(i73, i);
                                    if (userActivityCount != 0) {
                                        if (i74 == 0) {
                                            sb7.setLength(z9 ? 1 : 0);
                                            sb7.append("    User activity: ");
                                            i74 = i70;
                                        } else {
                                            sb7.append(", ");
                                        }
                                        sb7.append(userActivityCount);
                                        sb7.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                        sb7.append(Uid.USER_ACTIVITY_TYPES[i73]);
                                    }
                                    i73++;
                                }
                                i16 = i;
                                if (i74 != 0) {
                                    printWriter4.println(sb7.toString());
                                }
                            } else {
                                i16 = i;
                            }
                            ArrayMap<String, ? extends Uid.Wakelock> wakelockStats2 = uid5.getWakelockStats();
                            int size9 = wakelockStats2.size() - i70;
                            long j75 = 0;
                            long j76 = 0;
                            long jComputeWakeLock2 = 0;
                            long jComputeWakeLock3 = 0;
                            int i75 = z9 ? 1 : 0;
                            while (size9 >= 0) {
                                Uid.Wakelock wakelockValueAt2 = wakelockStats2.valueAt(size9);
                                sb7.setLength(z9 ? 1 : 0);
                                sb7.append(str2);
                                sb7.append("    Wake lock ");
                                sb7.append(wakelockStats2.keyAt(size9));
                                Timer wakeTime3 = wakelockValueAt2.getWakeTime(i70);
                                Uid uid6 = uid5;
                                long j77 = j75;
                                ArrayMap<String, ? extends Uid.Wakelock> arrayMap = wakelockStats2;
                                int i76 = size9;
                                long j78 = j76;
                                long j79 = j18;
                                long j80 = jComputeWakeLock2;
                                int i77 = i16;
                                String strPrintWakeLock = printWakeLock(sb7, wakeTime3, j79, "full", i77, ": ");
                                Timer wakeTime4 = wakelockValueAt2.getWakeTime(z9 ? 1 : 0);
                                String strPrintWakeLock2 = printWakeLock(sb7, wakeTime4, j79, Slice.HINT_PARTIAL, i77, strPrintWakeLock);
                                long j81 = j18;
                                int i78 = i16;
                                printWakeLock(sb7, wakelockValueAt2.getWakeTime(18), j81, "draw", i78, printWakeLock(sb7, wakelockValueAt2.getWakeTime(2), j81, Context.WINDOW_SERVICE, i78, printWakeLock(sb7, wakeTime4 != null ? wakeTime4.getSubTimer() : null, j81, "background partial", i78, strPrintWakeLock2)));
                                sb7.append(" realtime");
                                printWriter4.println(sb7.toString());
                                jComputeWakeLock2 = j80 + computeWakeLock(wakelockValueAt2.getWakeTime(i70), j81, i16);
                                long jComputeWakeLock4 = j78 + computeWakeLock(wakelockValueAt2.getWakeTime(z9 ? 1 : 0), j81, i16);
                                long jComputeWakeLock5 = j77 + computeWakeLock(wakelockValueAt2.getWakeTime(2), j81, i16);
                                jComputeWakeLock3 += computeWakeLock(wakelockValueAt2.getWakeTime(18), j81, i16);
                                size9 = i76 - 1;
                                i15 = i70;
                                i75++;
                                str2 = str;
                                j76 = jComputeWakeLock4;
                                j75 = jComputeWakeLock5;
                                uid5 = uid6;
                                wakelockStats2 = arrayMap;
                            }
                            long j82 = j75;
                            long j83 = j76;
                            Uid uid7 = uid5;
                            long j84 = j18;
                            long j85 = jComputeWakeLock3;
                            long j86 = jComputeWakeLock2;
                            if (i75 > i70) {
                                i17 = i70;
                                uid3 = uid7;
                                if (uid3.getAggregatedPartialWakelockTimer() == null) {
                                    j20 = j84;
                                    i18 = i15;
                                    j33 = 0;
                                    j34 = 0;
                                    j21 = j19;
                                } else {
                                    Timer aggregatedPartialWakelockTimer = uid3.getAggregatedPartialWakelockTimer();
                                    long j87 = j19;
                                    long totalDurationMsLocked9 = aggregatedPartialWakelockTimer.getTotalDurationMsLocked(j87);
                                    Timer subTimer = aggregatedPartialWakelockTimer.getSubTimer();
                                    if (subTimer != null) {
                                        totalDurationMsLocked4 = subTimer.getTotalDurationMsLocked(j87);
                                    } else {
                                        totalDurationMsLocked4 = 0;
                                    }
                                    j20 = j84;
                                    i18 = i15;
                                    j21 = j87;
                                    j34 = totalDurationMsLocked9;
                                    j33 = totalDurationMsLocked4;
                                }
                                if (j34 != 0 || j33 != 0 || j86 != 0 || j83 != 0 || j82 != 0) {
                                    sb7.setLength(z9 ? 1 : 0);
                                    str3 = str;
                                    sb7.append(str3);
                                    sb7.append("    TOTAL wake: ");
                                    if (j86 != 0) {
                                        formatTimeMs(sb7, j86);
                                        sb7.append("full");
                                        i30 = i17;
                                    } else {
                                        i30 = z9 ? 1 : 0;
                                    }
                                    if (j83 != 0) {
                                        if (i30 != 0) {
                                            sb7.append(", ");
                                        }
                                        formatTimeMs(sb7, j83);
                                        sb7.append("blamed partial");
                                        i30 = i17;
                                    }
                                    if (j34 != 0) {
                                        if (i30 != 0) {
                                            sb7.append(", ");
                                        }
                                        formatTimeMs(sb7, j34);
                                        sb7.append("actual partial");
                                        i31 = i17;
                                    } else {
                                        i31 = i30;
                                    }
                                    if (j33 != 0) {
                                        if (i31 != 0) {
                                            sb7.append(", ");
                                        }
                                        formatTimeMs(sb7, j33);
                                        sb7.append("actual background partial");
                                        i31 = i17;
                                    }
                                    if (j82 != 0) {
                                        if (i31 != 0) {
                                            sb7.append(", ");
                                        }
                                        formatTimeMs(sb7, j82);
                                        sb7.append(Context.WINDOW_SERVICE);
                                        i31 = i17;
                                    }
                                    if (j85 != 0) {
                                        if (i31 != 0) {
                                            sb7.append(",");
                                        }
                                        formatTimeMs(sb7, j85);
                                        sb7.append("draw");
                                    }
                                    sb7.append(" realtime");
                                    printWriter4.println(sb7.toString());
                                }
                                multicastWakelockStats = uid3.getMulticastWakelockStats();
                                if (multicastWakelockStats == null) {
                                    j22 = j20;
                                    i19 = i;
                                    long totalTimeLocked4 = multicastWakelockStats.getTotalTimeLocked(j22, i19);
                                    int countLocked5 = multicastWakelockStats.getCountLocked(i19);
                                    if (totalTimeLocked4 > 0) {
                                        sb7.setLength(z9 ? 1 : 0);
                                        sb7.append(str3);
                                        sb7.append("    WiFi Multicast Wakelock");
                                        sb7.append(" count = ");
                                        sb7.append(countLocked5);
                                        sb7.append(" time = ");
                                        formatTimeMsNoSpace(sb7, (totalTimeLocked4 + 500) / 1000);
                                        printWriter4.println(sb7.toString());
                                    }
                                } else {
                                    j22 = j20;
                                    i19 = i;
                                }
                                ArrayMap<String, ? extends Timer> syncStats = uid3.getSyncStats();
                                size = syncStats.size() - i17;
                                while (size >= 0) {
                                    Timer timerValueAt = syncStats.valueAt(size);
                                    long totalTimeLocked5 = (timerValueAt.getTotalTimeLocked(j22, i19) + 500) / 1000;
                                    int countLocked6 = timerValueAt.getCountLocked(i19);
                                    Timer subTimer2 = timerValueAt.getSubTimer();
                                    if (subTimer2 != null) {
                                        long j88 = j21;
                                        j32 = j88;
                                        totalDurationMsLocked3 = subTimer2.getTotalDurationMsLocked(j88);
                                    } else {
                                        j32 = j21;
                                        totalDurationMsLocked3 = -1;
                                    }
                                    int countLocked7 = subTimer2 != null ? subTimer2.getCountLocked(i19) : i7;
                                    sb7.setLength(z9 ? 1 : 0);
                                    sb7.append(str3);
                                    sb7.append("    Sync ");
                                    sb7.append(syncStats.keyAt(size));
                                    sb7.append(": ");
                                    if (totalTimeLocked5 != 0) {
                                        formatTimeMs(sb7, totalTimeLocked5);
                                        sb7.append("realtime (");
                                        sb7.append(countLocked6);
                                        sb7.append(" times)");
                                        if (totalDurationMsLocked3 > 0) {
                                            sb7.append(", ");
                                            formatTimeMs(sb7, totalDurationMsLocked3);
                                            sb7.append("background (");
                                            sb7.append(countLocked7);
                                            sb7.append(" times)");
                                        }
                                    } else {
                                        sb7.append("(not used)");
                                    }
                                    printWriter4.println(sb7.toString());
                                    size--;
                                    i18 = i17;
                                    j21 = j32;
                                }
                                long j89 = j21;
                                ArrayMap<String, ? extends Timer> jobStats = uid3.getJobStats();
                                size2 = jobStats.size() - i17;
                                boolean z10 = z9;
                                while (size2 >= 0) {
                                    Timer timerValueAt2 = jobStats.valueAt(size2);
                                    long totalTimeLocked6 = (timerValueAt2.getTotalTimeLocked(j22, i19) + 500) / 1000;
                                    int countLocked8 = timerValueAt2.getCountLocked(i19);
                                    Timer subTimer3 = timerValueAt2.getSubTimer();
                                    if (subTimer3 != null) {
                                        long j90 = j89;
                                        j31 = j90;
                                        totalDurationMsLocked2 = subTimer3.getTotalDurationMsLocked(j90);
                                    } else {
                                        j31 = j89;
                                        totalDurationMsLocked2 = -1;
                                    }
                                    int countLocked9 = subTimer3 != null ? subTimer3.getCountLocked(i19) : i7;
                                    boolean z11 = z10;
                                    sb7.setLength(z11 ? 1 : 0);
                                    sb7.append(str3);
                                    sb7.append("    Job ");
                                    sb7.append(jobStats.keyAt(size2));
                                    sb7.append(": ");
                                    if (totalTimeLocked6 != 0) {
                                        formatTimeMs(sb7, totalTimeLocked6);
                                        sb7.append("realtime (");
                                        sb7.append(countLocked8);
                                        sb7.append(" times)");
                                        if (totalDurationMsLocked2 > 0) {
                                            sb7.append(", ");
                                            formatTimeMs(sb7, totalDurationMsLocked2);
                                            sb7.append("background (");
                                            sb7.append(countLocked9);
                                            sb7.append(" times)");
                                        }
                                    } else {
                                        sb7.append("(not used)");
                                    }
                                    printWriter4.println(sb7.toString());
                                    size2--;
                                    i18 = i17;
                                    j89 = j31;
                                    z10 = z11 ? 1 : 0;
                                }
                                boolean z12 = z10;
                                long j91 = j89;
                                ArrayMap<String, SparseIntArray> jobCompletionStats = uid3.getJobCompletionStats();
                                int i79 = i17;
                                for (size3 = jobCompletionStats.size() - i79; size3 >= 0; size3--) {
                                    SparseIntArray sparseIntArrayValueAt = jobCompletionStats.valueAt(size3);
                                    if (sparseIntArrayValueAt != null) {
                                        printWriter.print(str);
                                        printWriter4.print("    Job Completions ");
                                        printWriter4.print(jobCompletionStats.keyAt(size3));
                                        printWriter4.print(SettingsStringUtil.DELIMITER);
                                        for (int i80 = z12 ? 1 : 0; i80 < sparseIntArrayValueAt.size(); i80++) {
                                            printWriter4.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                            printWriter4.print(JobParameters.getReasonName(sparseIntArrayValueAt.keyAt(i80)));
                                            printWriter4.print("(");
                                            printWriter4.print(sparseIntArrayValueAt.valueAt(i80));
                                            printWriter4.print("x)");
                                        }
                                        printWriter.println();
                                    }
                                }
                                uid3.getDeferredJobsLineLocked(sb7, i19);
                                if (sb7.length() > 0) {
                                    printWriter4.print("    Jobs deferred on launch ");
                                    printWriter4.println(sb7.toString());
                                }
                                Timer flashlightTurnedOnTimer = uid3.getFlashlightTurnedOnTimer();
                                j23 = j16;
                                int i81 = z12 ? 1 : 0;
                                i20 = i79;
                                sb3 = sb7;
                                str4 = str3;
                                Uid uid8 = uid3;
                                long j92 = j22;
                                long j93 = j91;
                                j24 = j72;
                                int i82 = i19;
                                i21 = i72 == true ? 1 : 0;
                                int i83 = i18 | (printTimer(printWriter4, sb7, flashlightTurnedOnTimer, j92, i82, str4, "Flashlight") ? 1 : 0) | (printTimer(printWriter4, sb3, uid8.getCameraTurnedOnTimer(), j92, i82, str4, "Camera") ? 1 : 0) | (printTimer(printWriter4, sb3, uid8.getVideoTurnedOnTimer(), j92, i82, str4, "Video") ? 1 : 0) | (printTimer(printWriter4, sb3, uid8.getAudioTurnedOnTimer(), j92, i82, str4, "Audio") ? 1 : 0);
                                SparseArray<? extends Uid.Sensor> sensorStats = uid8.getSensorStats();
                                size4 = sensorStats.size();
                                int i84 = i83;
                                i22 = i81 == true ? 1 : 0;
                                while (i22 < size4) {
                                    Uid.Sensor sensorValueAt = sensorStats.valueAt(i22);
                                    sensorStats.keyAt(i22);
                                    sb3.setLength(i81 == true ? 1 : 0);
                                    sb3.append(str4);
                                    sb3.append("    Sensor ");
                                    int handle = sensorValueAt.getHandle();
                                    if (handle == -10000) {
                                        sb3.append("GPS");
                                    } else {
                                        sb3.append(handle);
                                    }
                                    sb3.append(": ");
                                    Timer sensorTime = sensorValueAt.getSensorTime();
                                    if (sensorTime != null) {
                                        long totalTimeLocked7 = (sensorTime.getTotalTimeLocked(j22, i19) + 500) / 1000;
                                        int countLocked10 = sensorTime.getCountLocked(i19);
                                        Timer sensorBackgroundTime = sensorValueAt.getSensorBackgroundTime();
                                        if (sensorBackgroundTime != null) {
                                            int countLocked11 = sensorBackgroundTime.getCountLocked(i19);
                                            i28 = i81 == true ? 1 : 0;
                                            i81 = countLocked11;
                                        } else {
                                            i28 = i81 == true ? 1 : 0;
                                        }
                                        j29 = j22;
                                        sparseArray3 = sensorStats;
                                        i29 = size4;
                                        j30 = j93;
                                        long totalDurationMsLocked10 = sensorTime.getTotalDurationMsLocked(j30);
                                        if (sensorBackgroundTime != null) {
                                            totalDurationMsLocked = sensorBackgroundTime.getTotalDurationMsLocked(j30);
                                        } else {
                                            totalDurationMsLocked = 0;
                                        }
                                        if (totalTimeLocked7 != 0) {
                                            if (totalDurationMsLocked10 != totalTimeLocked7) {
                                                formatTimeMs(sb3, totalTimeLocked7);
                                                sb3.append("blamed realtime, ");
                                            }
                                            formatTimeMs(sb3, totalDurationMsLocked10);
                                            sb3.append("realtime (");
                                            sb3.append(countLocked10);
                                            sb3.append(" times)");
                                            if (totalDurationMsLocked != 0 || i81 > 0) {
                                                sb3.append(", ");
                                                formatTimeMs(sb3, totalDurationMsLocked);
                                                sb3.append("background (");
                                                sb3.append(i81);
                                                sb3.append(" times)");
                                            }
                                        } else {
                                            sb3.append("(not used)");
                                        }
                                    } else {
                                        j29 = j22;
                                        i28 = i81 == true ? 1 : 0;
                                        sparseArray3 = sensorStats;
                                        i29 = size4;
                                        j30 = j93;
                                        sb3.append("(not used)");
                                    }
                                    printWriter4.println(sb3.toString());
                                    i22++;
                                    j93 = j30;
                                    i84 = i20;
                                    i81 = i28;
                                    j22 = j29;
                                    sensorStats = sparseArray3;
                                    size4 = i29;
                                    i19 = i;
                                }
                                long j94 = j22;
                                z3 = i81 == true ? 1 : 0;
                                PrintWriter printWriter6 = printWriter4;
                                long j95 = j93;
                                int i85 = i;
                                int i86 = i84 | (printTimer(printWriter6, sb3, uid8.getVibratorOnTimer(), j94, i85, str4, "Vibrator") ? 1 : 0) | (printTimer(printWriter6, sb3, uid8.getForegroundActivityTimer(), j94, i85, str4, "Foreground activities") ? 1 : 0) | (printTimer(printWriter4, sb3, uid8.getForegroundServiceTimer(), j94, i85, str4, "Foreground services") ? 1 : 0);
                                j25 = 0;
                                i23 = z3 ? 1 : 0;
                                while (i23 < 7) {
                                    long j96 = j94;
                                    long processStateTime = uid8.getProcessStateTime(i23, j96, i85);
                                    if (processStateTime > 0) {
                                        j25 += processStateTime;
                                        sb3.setLength(z3 ? 1 : 0);
                                        sb3.append(str4);
                                        sb3.append("    ");
                                        sb3.append(Uid.PROCESS_STATE_NAMES[i23]);
                                        sb3.append(" for: ");
                                        formatTimeMs(sb3, (processStateTime + 500) / 1000);
                                        printWriter4.println(sb3.toString());
                                        i86 = i20;
                                    }
                                    i23++;
                                    j94 = j96;
                                }
                                long j97 = j94;
                                if (j25 > 0) {
                                    sb3.setLength(z3 ? 1 : 0);
                                    sb3.append(str4);
                                    sb3.append("    Total running: ");
                                    formatTimeMs(sb3, (j25 + 500) / 1000);
                                    printWriter4.println(sb3.toString());
                                }
                                userCpuTimeUs = uid8.getUserCpuTimeUs(i85);
                                systemCpuTimeUs = uid8.getSystemCpuTimeUs(i85);
                                if (userCpuTimeUs <= 0 || systemCpuTimeUs > 0) {
                                    sb3.setLength(z3 ? 1 : 0);
                                    sb3.append(str4);
                                    sb3.append("    Total cpu time: u=");
                                    formatTimeMs(sb3, userCpuTimeUs / 1000);
                                    sb3.append("s=");
                                    formatTimeMs(sb3, systemCpuTimeUs / 1000);
                                    printWriter4.println(sb3.toString());
                                }
                                cpuFreqTimes = uid8.getCpuFreqTimes(i85);
                                if (cpuFreqTimes != null) {
                                    sb3.setLength(z3 ? 1 : 0);
                                    sb3.append("    Total cpu time per freq:");
                                    for (int i87 = z3 ? 1 : 0; i87 < cpuFreqTimes.length; i87++) {
                                        sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + cpuFreqTimes[i87]);
                                    }
                                    printWriter4.println(sb3.toString());
                                }
                                screenOffCpuFreqTimes = uid8.getScreenOffCpuFreqTimes(i85);
                                if (screenOffCpuFreqTimes != null) {
                                    sb3.setLength(z3 ? 1 : 0);
                                    sb3.append("    Total screen-off cpu time per freq:");
                                    for (int i88 = z3 ? 1 : 0; i88 < screenOffCpuFreqTimes.length; i88++) {
                                        sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + screenOffCpuFreqTimes[i88]);
                                    }
                                    printWriter4.println(sb3.toString());
                                }
                                for (i24 = z3 ? 1 : 0; i24 < 7; i24++) {
                                    long[] cpuFreqTimes2 = uid8.getCpuFreqTimes(i85, i24);
                                    if (cpuFreqTimes2 != null) {
                                        sb3.setLength(z3 ? 1 : 0);
                                        sb3.append("    Cpu times per freq at state " + Uid.PROCESS_STATE_NAMES[i24] + SettingsStringUtil.DELIMITER);
                                        for (int i89 = z3 ? 1 : 0; i89 < cpuFreqTimes2.length; i89++) {
                                            sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + cpuFreqTimes2[i89]);
                                        }
                                        printWriter4.println(sb3.toString());
                                    }
                                    long[] screenOffCpuFreqTimes2 = uid8.getScreenOffCpuFreqTimes(i85, i24);
                                    if (screenOffCpuFreqTimes2 != null) {
                                        sb3.setLength(z3 ? 1 : 0);
                                        sb3.append("   Screen-off cpu times per freq at state " + Uid.PROCESS_STATE_NAMES[i24] + SettingsStringUtil.DELIMITER);
                                        for (int i90 = z3 ? 1 : 0; i90 < screenOffCpuFreqTimes2.length; i90++) {
                                            sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + screenOffCpuFreqTimes2[i90]);
                                        }
                                        printWriter4.println(sb3.toString());
                                    }
                                }
                                ArrayMap<String, ? extends Uid.Proc> processStats = uid8.getProcessStats();
                                size5 = processStats.size() - 1;
                                while (size5 >= 0) {
                                    Uid.Proc procValueAt = processStats.valueAt(size5);
                                    long userTime = procValueAt.getUserTime(i85);
                                    long j98 = j95;
                                    long systemTime = procValueAt.getSystemTime(i85);
                                    int i91 = i86;
                                    long j99 = j97;
                                    long foregroundTime = procValueAt.getForegroundTime(i85);
                                    int starts = procValueAt.getStarts(i85);
                                    int numCrashes = procValueAt.getNumCrashes(i85);
                                    Uid uid9 = uid8;
                                    int numAnrs = procValueAt.getNumAnrs(i85);
                                    if (i85 == 0) {
                                        iCountExcessivePowers = procValueAt.countExcessivePowers();
                                    } else {
                                        iCountExcessivePowers = z3 ? 1 : 0;
                                    }
                                    if (userTime != 0 || systemTime != 0 || foregroundTime != 0 || starts != 0 || iCountExcessivePowers != 0 || numCrashes != 0 || numAnrs != 0) {
                                        Uid.Proc proc = procValueAt;
                                        sb3.setLength(z3 ? 1 : 0);
                                        sb3.append(str4);
                                        sb3.append("    Proc ");
                                        sb3.append(processStats.keyAt(size5));
                                        sb3.append(":\n");
                                        sb3.append(str4);
                                        sb3.append("      CPU: ");
                                        formatTimeMs(sb3, userTime);
                                        sb3.append("usr + ");
                                        formatTimeMs(sb3, systemTime);
                                        sb3.append("krn ; ");
                                        formatTimeMs(sb3, foregroundTime);
                                        sb3.append(FOREGROUND_ACTIVITY_DATA);
                                        if (starts != 0 || numCrashes != 0 || numAnrs != 0) {
                                            sb3.append("\n");
                                            sb3.append(str4);
                                            sb3.append("      ");
                                            if (starts != 0) {
                                                sb3.append(starts);
                                                sb3.append(" starts");
                                                i27 = i20;
                                            } else {
                                                i27 = z3 ? 1 : 0;
                                            }
                                            if (numCrashes != 0) {
                                                if (i27 != 0) {
                                                    sb3.append(", ");
                                                }
                                                sb3.append(numCrashes);
                                                sb3.append(" crashes");
                                                i27 = i20;
                                            }
                                            if (numAnrs != 0) {
                                                if (i27 != 0) {
                                                    sb3.append(", ");
                                                }
                                                sb3.append(numAnrs);
                                                sb3.append(" anrs");
                                            }
                                        }
                                        printWriter4.println(sb3.toString());
                                        int i92 = z3 ? 1 : 0;
                                        while (i92 < iCountExcessivePowers) {
                                            Uid.Proc proc2 = proc;
                                            Uid.Proc.ExcessivePower excessivePower = proc2.getExcessivePower(i92);
                                            if (excessivePower != null) {
                                                printWriter.print(str);
                                                printWriter4.print("      * Killed for ");
                                                if (excessivePower.type == 2) {
                                                    printWriter4.print(CPU_DATA);
                                                } else {
                                                    printWriter4.print("unknown");
                                                }
                                                printWriter4.print(" use: ");
                                                TimeUtils.formatDuration(excessivePower.usedTime, printWriter4);
                                                printWriter4.print(" over ");
                                                TimeUtils.formatDuration(excessivePower.overTime, printWriter4);
                                                if (excessivePower.overTime != 0) {
                                                    printWriter4.print(" (");
                                                    printWriter4.print((excessivePower.usedTime * 100) / excessivePower.overTime);
                                                    printWriter4.println("%)");
                                                }
                                            }
                                            i92++;
                                            proc = proc2;
                                        }
                                        i91 = i20;
                                    }
                                    size5--;
                                    j95 = j98;
                                    j97 = j99;
                                    i86 = i91;
                                    uid8 = uid9;
                                    i85 = i;
                                }
                                j26 = j95;
                                j27 = j97;
                                ArrayMap<String, ? extends Uid.Pkg> packageStats = uid8.getPackageStats();
                                size6 = packageStats.size() - 1;
                                i25 = i86;
                                boolean z13 = z3;
                                while (size6 >= 0) {
                                    printWriter.print(str);
                                    printWriter4.print("    Apk ");
                                    printWriter4.print(packageStats.keyAt(size6));
                                    printWriter4.println(SettingsStringUtil.DELIMITER);
                                    Uid.Pkg pkgValueAt = packageStats.valueAt(size6);
                                    ArrayMap<String, ? extends Counter> wakeupAlarmStats = pkgValueAt.getWakeupAlarmStats();
                                    int size10 = wakeupAlarmStats.size() - 1;
                                    int i93 = z13 ? 1 : 0;
                                    while (size10 >= 0) {
                                        printWriter.print(str);
                                        printWriter4.print("      Wakeup alarm ");
                                        printWriter4.print(wakeupAlarmStats.keyAt(size10));
                                        printWriter4.print(": ");
                                        printWriter4.print(wakeupAlarmStats.valueAt(size10).getCountLocked(i));
                                        printWriter4.println(" times");
                                        size10--;
                                        i93 = i20;
                                    }
                                    ArrayMap<String, ? extends Uid.Pkg.Serv> serviceStats = pkgValueAt.getServiceStats();
                                    int size11 = serviceStats.size() - 1;
                                    boolean z14 = z13;
                                    while (size11 >= 0) {
                                        Uid.Pkg.Serv servValueAt = serviceStats.valueAt(size11);
                                        long j100 = j53;
                                        long startTime = servValueAt.getStartTime(j100, i);
                                        int starts2 = servValueAt.getStarts(i);
                                        int launches = servValueAt.getLaunches(i);
                                        if (startTime == 0 && starts2 == 0 && launches == 0) {
                                            z4 = z14;
                                        } else {
                                            boolean z15 = z14;
                                            sb3.setLength(z15 ? 1 : 0);
                                            sb3.append(str4);
                                            z4 = z15 ? 1 : 0;
                                            sb3.append("      Service ");
                                            sb3.append(serviceStats.keyAt(size11));
                                            sb3.append(":\n");
                                            sb3.append(str4);
                                            sb3.append("        Created for: ");
                                            formatTimeMs(sb3, startTime / 1000);
                                            sb3.append("uptime\n");
                                            sb3.append(str4);
                                            sb3.append("        Starts: ");
                                            sb3.append(starts2);
                                            sb3.append(", launches: ");
                                            sb3.append(launches);
                                            printWriter4.println(sb3.toString());
                                            i93 = i20;
                                        }
                                        size11--;
                                        j53 = j100;
                                        z14 = z4;
                                    }
                                    long j101 = j53;
                                    boolean z16 = z14;
                                    if (i93 == 0) {
                                        printWriter.print(str);
                                        printWriter4.println("      (nothing executed)");
                                    }
                                    size6--;
                                    z13 = z16 ? 1 : 0;
                                    j53 = j101;
                                    i25 = i20;
                                }
                                j28 = j53;
                                i12 = z13 ? 1 : 0;
                                i26 = i;
                                if (i25 != 0) {
                                    printWriter.print(str);
                                    printWriter4.println("    (nothing executed)");
                                }
                            } else {
                                i17 = i70;
                                j20 = j84;
                                i18 = i15;
                                j21 = j19;
                                uid3 = uid7;
                            }
                            str3 = str;
                            multicastWakelockStats = uid3.getMulticastWakelockStats();
                            if (multicastWakelockStats == null) {
                            }
                            ArrayMap<String, ? extends Timer> syncStats2 = uid3.getSyncStats();
                            size = syncStats2.size() - i17;
                            while (size >= 0) {
                            }
                            long j892 = j21;
                            ArrayMap<String, ? extends Timer> jobStats2 = uid3.getJobStats();
                            size2 = jobStats2.size() - i17;
                            boolean z102 = z9;
                            while (size2 >= 0) {
                            }
                            boolean z122 = z102;
                            long j912 = j892;
                            ArrayMap<String, SparseIntArray> jobCompletionStats2 = uid3.getJobCompletionStats();
                            int i792 = i17;
                            while (size3 >= 0) {
                            }
                            uid3.getDeferredJobsLineLocked(sb7, i19);
                            if (sb7.length() > 0) {
                            }
                            Timer flashlightTurnedOnTimer2 = uid3.getFlashlightTurnedOnTimer();
                            j23 = j16;
                            int i812 = z122 ? 1 : 0;
                            i20 = i792;
                            sb3 = sb7;
                            str4 = str3;
                            Uid uid82 = uid3;
                            long j922 = j22;
                            long j932 = j912;
                            j24 = j72;
                            int i822 = i19;
                            i21 = i72 == true ? 1 : 0;
                            int i832 = i18 | (printTimer(printWriter4, sb7, flashlightTurnedOnTimer2, j922, i822, str4, "Flashlight") ? 1 : 0) | (printTimer(printWriter4, sb3, uid82.getCameraTurnedOnTimer(), j922, i822, str4, "Camera") ? 1 : 0) | (printTimer(printWriter4, sb3, uid82.getVideoTurnedOnTimer(), j922, i822, str4, "Video") ? 1 : 0) | (printTimer(printWriter4, sb3, uid82.getAudioTurnedOnTimer(), j922, i822, str4, "Audio") ? 1 : 0);
                            SparseArray<? extends Uid.Sensor> sensorStats2 = uid82.getSensorStats();
                            size4 = sensorStats2.size();
                            int i842 = i832;
                            i22 = i812 == true ? 1 : 0;
                            while (i22 < size4) {
                            }
                            long j942 = j22;
                            z3 = i812 == true ? 1 : 0;
                            PrintWriter printWriter62 = printWriter4;
                            long j952 = j932;
                            int i852 = i;
                            int i862 = i842 | (printTimer(printWriter62, sb3, uid82.getVibratorOnTimer(), j942, i852, str4, "Vibrator") ? 1 : 0) | (printTimer(printWriter62, sb3, uid82.getForegroundActivityTimer(), j942, i852, str4, "Foreground activities") ? 1 : 0) | (printTimer(printWriter4, sb3, uid82.getForegroundServiceTimer(), j942, i852, str4, "Foreground services") ? 1 : 0);
                            j25 = 0;
                            i23 = z3 ? 1 : 0;
                            while (i23 < 7) {
                            }
                            long j972 = j942;
                            if (j25 > 0) {
                            }
                            userCpuTimeUs = uid82.getUserCpuTimeUs(i852);
                            systemCpuTimeUs = uid82.getSystemCpuTimeUs(i852);
                            if (userCpuTimeUs <= 0) {
                                sb3.setLength(z3 ? 1 : 0);
                                sb3.append(str4);
                                sb3.append("    Total cpu time: u=");
                                formatTimeMs(sb3, userCpuTimeUs / 1000);
                                sb3.append("s=");
                                formatTimeMs(sb3, systemCpuTimeUs / 1000);
                                printWriter4.println(sb3.toString());
                                cpuFreqTimes = uid82.getCpuFreqTimes(i852);
                                if (cpuFreqTimes != null) {
                                }
                                screenOffCpuFreqTimes = uid82.getScreenOffCpuFreqTimes(i852);
                                if (screenOffCpuFreqTimes != null) {
                                }
                                while (i24 < 7) {
                                }
                                ArrayMap<String, ? extends Uid.Proc> processStats2 = uid82.getProcessStats();
                                size5 = processStats2.size() - 1;
                                while (size5 >= 0) {
                                }
                                j26 = j952;
                                j27 = j972;
                                ArrayMap<String, ? extends Uid.Pkg> packageStats2 = uid82.getPackageStats();
                                size6 = packageStats2.size() - 1;
                                i25 = i862;
                                boolean z132 = z3;
                                while (size6 >= 0) {
                                }
                                j28 = j53;
                                i12 = z132 ? 1 : 0;
                                i26 = i;
                                if (i25 != 0) {
                                }
                            }
                        }
                        i69 = i21 + 1;
                        i10 = i2;
                        sb = sb3;
                        printWriter3 = printWriter4;
                        i9 = i26;
                        j53 = j28;
                        i11 = i14;
                        sparseArray = sparseArray2;
                        j8 = j23;
                        i50 = i20;
                        j55 = j24;
                        j9 = j26;
                        j7 = j27;
                        batteryStats = this;
                    }
                    sb7.setLength(z9 ? 1 : 0);
                    sb7.append(str2);
                    sb7.append("    Wifi Running: ");
                    formatTimeMs(sb7, j15 / 1000);
                    sb7.append("(");
                    long j102 = j8;
                    sb7.append(batteryStats.formatRatioLocked(j15, j102));
                    sb7.append(")\n");
                    sb7.append(str2);
                    sb7.append("    Full Wifi Lock: ");
                    formatTimeMs(sb7, fullWifiLockTime / 1000);
                    sb7.append("(");
                    sb7.append(batteryStats.formatRatioLocked(fullWifiLockTime, j102));
                    sb7.append(")\n");
                    sb7.append(str2);
                    sb7.append("    Wifi Scan (blamed): ");
                    formatTimeMs(sb7, wifiScanTime / 1000);
                    sb7.append("(");
                    sb7.append(batteryStats.formatRatioLocked(wifiScanTime, j102));
                    sb7.append(") ");
                    sb7.append(wifiScanCount);
                    sb7.append("x\n");
                    sb7.append(str2);
                    sb7.append("    Wifi Scan (actual): ");
                    formatTimeMs(sb7, j13 / 1000);
                    sb7.append("(");
                    j16 = j102;
                    j17 = j69;
                    sb7.append(batteryStats.formatRatioLocked(j13, batteryStats.computeBatteryRealtime(j17, z9 ? 1 : 0)));
                    sb7.append(") ");
                    sb7.append(wifiScanCount);
                    sb7.append("x\n");
                    sb7.append(str2);
                    sb7.append("    Background Wifi Scan: ");
                    formatTimeMs(sb7, j14 / 1000);
                    sb7.append("(");
                    sb7.append(batteryStats.formatRatioLocked(j14, batteryStats.computeBatteryRealtime(j17, z9 ? 1 : 0)));
                    sb7.append(") ");
                    sb7.append(i13);
                    sb7.append("x");
                    printWriter3.println(sb7.toString());
                    if (j10 > 0) {
                    }
                    batteryStats.printControllerActivityIfInteresting(printWriter3, sb7, str2 + "  ", WIFI_CONTROLLER_NAME, uid4.getWifiControllerActivity(), i);
                    if (networkActivityBytes11 <= 0) {
                        printWriter.print(str);
                        printWriter3.print("    Bluetooth network: ");
                        printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes11));
                        printWriter3.print(" received, ");
                        printWriter3.print(batteryStats.formatBytesLocked(networkActivityBytes12));
                        printWriter3.println(" sent");
                        bluetoothScanTimer = uid4.getBluetoothScanTimer();
                        if (bluetoothScanTimer == null) {
                        }
                    }
                    i69 = i21 + 1;
                    i10 = i2;
                    sb = sb3;
                    printWriter3 = printWriter4;
                    i9 = i26;
                    j53 = j28;
                    i11 = i14;
                    sparseArray = sparseArray2;
                    j8 = j23;
                    i50 = i20;
                    j55 = j24;
                    j9 = j26;
                    j7 = j27;
                    batteryStats = this;
                }
                j15 = wifiRunningTime;
                sb7.setLength(z9 ? 1 : 0);
                sb7.append(str2);
                sb7.append("    Wifi Running: ");
                formatTimeMs(sb7, j15 / 1000);
                sb7.append("(");
                long j1022 = j8;
                sb7.append(batteryStats.formatRatioLocked(j15, j1022));
                sb7.append(")\n");
                sb7.append(str2);
                sb7.append("    Full Wifi Lock: ");
                formatTimeMs(sb7, fullWifiLockTime / 1000);
                sb7.append("(");
                sb7.append(batteryStats.formatRatioLocked(fullWifiLockTime, j1022));
                sb7.append(")\n");
                sb7.append(str2);
                sb7.append("    Wifi Scan (blamed): ");
                formatTimeMs(sb7, wifiScanTime / 1000);
                sb7.append("(");
                sb7.append(batteryStats.formatRatioLocked(wifiScanTime, j1022));
                sb7.append(") ");
                sb7.append(wifiScanCount);
                sb7.append("x\n");
                sb7.append(str2);
                sb7.append("    Wifi Scan (actual): ");
                formatTimeMs(sb7, j13 / 1000);
                sb7.append("(");
                j16 = j1022;
                j17 = j69;
                sb7.append(batteryStats.formatRatioLocked(j13, batteryStats.computeBatteryRealtime(j17, z9 ? 1 : 0)));
                sb7.append(") ");
                sb7.append(wifiScanCount);
                sb7.append("x\n");
                sb7.append(str2);
                sb7.append("    Background Wifi Scan: ");
                formatTimeMs(sb7, j14 / 1000);
                sb7.append("(");
                sb7.append(batteryStats.formatRatioLocked(j14, batteryStats.computeBatteryRealtime(j17, z9 ? 1 : 0)));
                sb7.append(") ");
                sb7.append(i13);
                sb7.append("x");
                printWriter3.println(sb7.toString());
                if (j10 > 0) {
                }
                batteryStats.printControllerActivityIfInteresting(printWriter3, sb7, str2 + "  ", WIFI_CONTROLLER_NAME, uid4.getWifiControllerActivity(), i);
                if (networkActivityBytes11 <= 0) {
                }
                i69 = i21 + 1;
                i10 = i2;
                sb = sb3;
                printWriter3 = printWriter4;
                i9 = i26;
                j53 = j28;
                i11 = i14;
                sparseArray = sparseArray2;
                j8 = j23;
                i50 = i20;
                j55 = j24;
                j9 = j26;
                j7 = j27;
                batteryStats = this;
            } else {
                j13 = wifiScanActualTime;
            }
            j14 = wifiScanBackgroundTime;
            j15 = wifiRunningTime;
            sb7.setLength(z9 ? 1 : 0);
            sb7.append(str2);
            sb7.append("    Wifi Running: ");
            formatTimeMs(sb7, j15 / 1000);
            sb7.append("(");
            long j10222 = j8;
            sb7.append(batteryStats.formatRatioLocked(j15, j10222));
            sb7.append(")\n");
            sb7.append(str2);
            sb7.append("    Full Wifi Lock: ");
            formatTimeMs(sb7, fullWifiLockTime / 1000);
            sb7.append("(");
            sb7.append(batteryStats.formatRatioLocked(fullWifiLockTime, j10222));
            sb7.append(")\n");
            sb7.append(str2);
            sb7.append("    Wifi Scan (blamed): ");
            formatTimeMs(sb7, wifiScanTime / 1000);
            sb7.append("(");
            sb7.append(batteryStats.formatRatioLocked(wifiScanTime, j10222));
            sb7.append(") ");
            sb7.append(wifiScanCount);
            sb7.append("x\n");
            sb7.append(str2);
            sb7.append("    Wifi Scan (actual): ");
            formatTimeMs(sb7, j13 / 1000);
            sb7.append("(");
            j16 = j10222;
            j17 = j69;
            sb7.append(batteryStats.formatRatioLocked(j13, batteryStats.computeBatteryRealtime(j17, z9 ? 1 : 0)));
            sb7.append(") ");
            sb7.append(wifiScanCount);
            sb7.append("x\n");
            sb7.append(str2);
            sb7.append("    Background Wifi Scan: ");
            formatTimeMs(sb7, j14 / 1000);
            sb7.append("(");
            sb7.append(batteryStats.formatRatioLocked(j14, batteryStats.computeBatteryRealtime(j17, z9 ? 1 : 0)));
            sb7.append(") ");
            sb7.append(i13);
            sb7.append("x");
            printWriter3.println(sb7.toString());
            if (j10 > 0) {
            }
            batteryStats.printControllerActivityIfInteresting(printWriter3, sb7, str2 + "  ", WIFI_CONTROLLER_NAME, uid4.getWifiControllerActivity(), i);
            if (networkActivityBytes11 <= 0) {
            }
            i69 = i21 + 1;
            i10 = i2;
            sb = sb3;
            printWriter3 = printWriter4;
            i9 = i26;
            j53 = j28;
            i11 = i14;
            sparseArray = sparseArray2;
            j8 = j23;
            i50 = i20;
            j55 = j24;
            j9 = j26;
            j7 = j27;
            batteryStats = this;
        }
    }

    static void printBitDescriptions(StringBuilder sb, int i, int i2, HistoryTag historyTag, BitDescription[] bitDescriptionArr, boolean z) {
        int i3 = i ^ i2;
        if (i3 == 0) {
            return;
        }
        boolean z2 = false;
        for (BitDescription bitDescription : bitDescriptionArr) {
            if ((bitDescription.mask & i3) != 0) {
                sb.append(z ? WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER : ",");
                if (bitDescription.shift < 0) {
                    sb.append((bitDescription.mask & i2) != 0 ? "+" : NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                    sb.append(z ? bitDescription.name : bitDescription.shortName);
                    if (bitDescription.mask == 1073741824 && historyTag != null) {
                        sb.append("=");
                        if (z) {
                            UserHandle.formatUid(sb, historyTag.uid);
                            sb.append(":\"");
                            sb.append(historyTag.string);
                            sb.append("\"");
                        } else {
                            sb.append(historyTag.poolIdx);
                        }
                        z2 = true;
                    }
                } else {
                    sb.append(z ? bitDescription.name : bitDescription.shortName);
                    sb.append("=");
                    int i4 = (bitDescription.mask & i2) >> bitDescription.shift;
                    if (bitDescription.values != null && i4 >= 0 && i4 < bitDescription.values.length) {
                        sb.append(z ? bitDescription.values[i4] : bitDescription.shortValues[i4]);
                    } else {
                        sb.append(i4);
                    }
                }
            }
        }
        if (!z2 && historyTag != null) {
            sb.append(z ? " wake_lock=" : ",w=");
            if (z) {
                UserHandle.formatUid(sb, historyTag.uid);
                sb.append(":\"");
                sb.append(historyTag.string);
                sb.append("\"");
                return;
            }
            sb.append(historyTag.poolIdx);
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
        int oldChargeMAh = -1;
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
            this.oldChargeMAh = -1;
        }

        public void printNextItem(PrintWriter printWriter, HistoryItem historyItem, long j, boolean z, boolean z2) {
            printWriter.print(printNextItem(historyItem, j, z, z2));
        }

        public void printNextItem(ProtoOutputStream protoOutputStream, HistoryItem historyItem, long j, boolean z) {
            for (String str : printNextItem(historyItem, j, true, z).split("\n")) {
                protoOutputStream.write(2237677961222L, str);
            }
        }

        private String printNextItem(HistoryItem historyItem, long j, boolean z, boolean z2) {
            StringBuilder sb = new StringBuilder();
            if (!z) {
                sb.append("  ");
                TimeUtils.formatDuration(historyItem.time - j, sb, 19);
                sb.append(" (");
                sb.append(historyItem.numReadInts);
                sb.append(") ");
            } else {
                sb.append(9);
                sb.append(',');
                sb.append(BatteryStats.HISTORY_DATA);
                sb.append(',');
                if (this.lastTime < 0) {
                    sb.append(historyItem.time - j);
                } else {
                    sb.append(historyItem.time - this.lastTime);
                }
                this.lastTime = historyItem.time;
            }
            if (historyItem.cmd == 4) {
                if (z) {
                    sb.append(SettingsStringUtil.DELIMITER);
                }
                sb.append("START\n");
                reset();
            } else if (historyItem.cmd == 5 || historyItem.cmd == 7) {
                if (z) {
                    sb.append(SettingsStringUtil.DELIMITER);
                }
                if (historyItem.cmd == 7) {
                    sb.append("RESET:");
                    reset();
                }
                sb.append("TIME:");
                if (z) {
                    sb.append(historyItem.currentTime);
                    sb.append("\n");
                } else {
                    sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    sb.append(DateFormat.format("yyyy-MM-dd-HH-mm-ss", historyItem.currentTime).toString());
                    sb.append("\n");
                }
            } else if (historyItem.cmd == 8) {
                if (z) {
                    sb.append(SettingsStringUtil.DELIMITER);
                }
                sb.append("SHUTDOWN\n");
            } else if (historyItem.cmd == 6) {
                if (z) {
                    sb.append(SettingsStringUtil.DELIMITER);
                }
                sb.append("*OVERFLOW*\n");
            } else {
                if (!z) {
                    if (historyItem.batteryLevel < 10) {
                        sb.append("00");
                    } else if (historyItem.batteryLevel < 100) {
                        sb.append(WifiEnterpriseConfig.ENGINE_DISABLE);
                    }
                    sb.append(historyItem.batteryLevel);
                    if (z2) {
                        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        if (historyItem.states >= 0) {
                            if (historyItem.states < 16) {
                                sb.append("0000000");
                            } else if (historyItem.states < 256) {
                                sb.append("000000");
                            } else if (historyItem.states < 4096) {
                                sb.append("00000");
                            } else if (historyItem.states < 65536) {
                                sb.append("0000");
                            } else if (historyItem.states < 1048576) {
                                sb.append("000");
                            } else if (historyItem.states < 16777216) {
                                sb.append("00");
                            } else if (historyItem.states < 268435456) {
                                sb.append(WifiEnterpriseConfig.ENGINE_DISABLE);
                            }
                        }
                        sb.append(Integer.toHexString(historyItem.states));
                    }
                } else if (this.oldLevel != historyItem.batteryLevel) {
                    this.oldLevel = historyItem.batteryLevel;
                    sb.append(",Bl=");
                    sb.append(historyItem.batteryLevel);
                }
                if (this.oldStatus != historyItem.batteryStatus) {
                    this.oldStatus = historyItem.batteryStatus;
                    sb.append(z ? ",Bs=" : " status=");
                    switch (this.oldStatus) {
                        case 1:
                            sb.append(z ? "?" : "unknown");
                            break;
                        case 2:
                            sb.append(z ? FullBackup.CACHE_TREE_TOKEN : "charging");
                            break;
                        case 3:
                            sb.append(z ? "d" : "discharging");
                            break;
                        case 4:
                            sb.append(z ? "n" : "not-charging");
                            break;
                        case 5:
                            sb.append(z ? FullBackup.FILES_TREE_TOKEN : "full");
                            break;
                        default:
                            sb.append(this.oldStatus);
                            break;
                    }
                }
                if (this.oldHealth != historyItem.batteryHealth) {
                    this.oldHealth = historyItem.batteryHealth;
                    sb.append(z ? ",Bh=" : " health=");
                    switch (this.oldHealth) {
                        case 1:
                            sb.append(z ? "?" : "unknown");
                            break;
                        case 2:
                            sb.append(z ? "g" : "good");
                            break;
                        case 3:
                            sb.append(z ? BatteryStats.HISTORY_DATA : "overheat");
                            break;
                        case 4:
                            sb.append(z ? "d" : "dead");
                            break;
                        case 5:
                            sb.append(z ? Telephony.BaseMmsColumns.MMS_VERSION : "over-voltage");
                            break;
                        case 6:
                            sb.append(z ? FullBackup.FILES_TREE_TOKEN : "failure");
                            break;
                        case 7:
                            sb.append(z ? FullBackup.CACHE_TREE_TOKEN : "cold");
                            break;
                        default:
                            sb.append(this.oldHealth);
                            break;
                    }
                }
                if (this.oldPlug != historyItem.batteryPlugType) {
                    this.oldPlug = historyItem.batteryPlugType;
                    sb.append(z ? ",Bp=" : " plug=");
                    int i = this.oldPlug;
                    if (i != 4) {
                        switch (i) {
                            case 0:
                                sb.append(z ? "n" : "none");
                                break;
                            case 1:
                                sb.append(z ? FullBackup.APK_TREE_TOKEN : "ac");
                                break;
                            case 2:
                                sb.append(z ? "u" : Context.USB_SERVICE);
                                break;
                            default:
                                sb.append(this.oldPlug);
                                break;
                        }
                    } else {
                        sb.append(z ? "w" : "wireless");
                    }
                }
                if (this.oldTemp != historyItem.batteryTemperature) {
                    this.oldTemp = historyItem.batteryTemperature;
                    sb.append(z ? ",Bt=" : " temp=");
                    sb.append(this.oldTemp);
                }
                if (this.oldVolt != historyItem.batteryVoltage) {
                    this.oldVolt = historyItem.batteryVoltage;
                    sb.append(z ? ",Bv=" : " volt=");
                    sb.append(this.oldVolt);
                }
                int i2 = historyItem.batteryChargeUAh / 1000;
                if (this.oldChargeMAh != i2) {
                    this.oldChargeMAh = i2;
                    sb.append(z ? ",Bcc=" : " charge=");
                    sb.append(this.oldChargeMAh);
                }
                BatteryStats.printBitDescriptions(sb, this.oldState, historyItem.states, historyItem.wakelockTag, BatteryStats.HISTORY_STATE_DESCRIPTIONS, !z);
                BatteryStats.printBitDescriptions(sb, this.oldState2, historyItem.states2, null, BatteryStats.HISTORY_STATE2_DESCRIPTIONS, !z);
                if (historyItem.wakeReasonTag != null) {
                    if (z) {
                        sb.append(",wr=");
                        sb.append(historyItem.wakeReasonTag.poolIdx);
                    } else {
                        sb.append(" wake_reason=");
                        sb.append(historyItem.wakeReasonTag.uid);
                        sb.append(":\"");
                        sb.append(historyItem.wakeReasonTag.string);
                        sb.append("\"");
                    }
                }
                if (historyItem.eventCode != 0) {
                    sb.append(z ? "," : WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    if ((historyItem.eventCode & 32768) != 0) {
                        sb.append("+");
                    } else if ((historyItem.eventCode & 16384) != 0) {
                        sb.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                    }
                    String[] strArr = z ? BatteryStats.HISTORY_EVENT_CHECKIN_NAMES : BatteryStats.HISTORY_EVENT_NAMES;
                    int i3 = historyItem.eventCode & HistoryItem.EVENT_TYPE_MASK;
                    if (i3 >= 0 && i3 < strArr.length) {
                        sb.append(strArr[i3]);
                    } else {
                        sb.append(z ? "Ev" : "event");
                        sb.append(i3);
                    }
                    sb.append("=");
                    if (z) {
                        sb.append(historyItem.eventTag.poolIdx);
                    } else {
                        sb.append(BatteryStats.HISTORY_EVENT_INT_FORMATTERS[i3].applyAsString(historyItem.eventTag.uid));
                        sb.append(":\"");
                        sb.append(historyItem.eventTag.string);
                        sb.append("\"");
                    }
                }
                sb.append("\n");
                if (historyItem.stepDetails != null) {
                    if (!z) {
                        sb.append("                 Details: cpu=");
                        sb.append(historyItem.stepDetails.userTime);
                        sb.append("u+");
                        sb.append(historyItem.stepDetails.systemTime);
                        sb.append("s");
                        if (historyItem.stepDetails.appCpuUid1 >= 0) {
                            sb.append(" (");
                            printStepCpuUidDetails(sb, historyItem.stepDetails.appCpuUid1, historyItem.stepDetails.appCpuUTime1, historyItem.stepDetails.appCpuSTime1);
                            if (historyItem.stepDetails.appCpuUid2 >= 0) {
                                sb.append(", ");
                                printStepCpuUidDetails(sb, historyItem.stepDetails.appCpuUid2, historyItem.stepDetails.appCpuUTime2, historyItem.stepDetails.appCpuSTime2);
                            }
                            if (historyItem.stepDetails.appCpuUid3 >= 0) {
                                sb.append(", ");
                                printStepCpuUidDetails(sb, historyItem.stepDetails.appCpuUid3, historyItem.stepDetails.appCpuUTime3, historyItem.stepDetails.appCpuSTime3);
                            }
                            sb.append(')');
                        }
                        sb.append("\n");
                        sb.append("                          /proc/stat=");
                        sb.append(historyItem.stepDetails.statUserTime);
                        sb.append(" usr, ");
                        sb.append(historyItem.stepDetails.statSystemTime);
                        sb.append(" sys, ");
                        sb.append(historyItem.stepDetails.statIOWaitTime);
                        sb.append(" io, ");
                        sb.append(historyItem.stepDetails.statIrqTime);
                        sb.append(" irq, ");
                        sb.append(historyItem.stepDetails.statSoftIrqTime);
                        sb.append(" sirq, ");
                        sb.append(historyItem.stepDetails.statIdlTime);
                        sb.append(" idle");
                        int i4 = historyItem.stepDetails.statUserTime + historyItem.stepDetails.statSystemTime + historyItem.stepDetails.statIOWaitTime + historyItem.stepDetails.statIrqTime + historyItem.stepDetails.statSoftIrqTime;
                        int i5 = historyItem.stepDetails.statIdlTime + i4;
                        if (i5 > 0) {
                            sb.append(" (");
                            sb.append(String.format("%.1f%%", Float.valueOf((i4 / i5) * 100.0f)));
                            sb.append(" of ");
                            StringBuilder sb2 = new StringBuilder(64);
                            BatteryStats.formatTimeMsNoSpace(sb2, i5 * 10);
                            sb.append((CharSequence) sb2);
                            sb.append(")");
                        }
                        sb.append(", PlatformIdleStat ");
                        sb.append(historyItem.stepDetails.statPlatformIdleState);
                        sb.append("\n");
                        sb.append(", SubsystemPowerState ");
                        sb.append(historyItem.stepDetails.statSubsystemPowerState);
                        sb.append("\n");
                    } else {
                        sb.append(9);
                        sb.append(',');
                        sb.append(BatteryStats.HISTORY_DATA);
                        sb.append(",0,Dcpu=");
                        sb.append(historyItem.stepDetails.userTime);
                        sb.append(SettingsStringUtil.DELIMITER);
                        sb.append(historyItem.stepDetails.systemTime);
                        if (historyItem.stepDetails.appCpuUid1 >= 0) {
                            printStepCpuUidCheckinDetails(sb, historyItem.stepDetails.appCpuUid1, historyItem.stepDetails.appCpuUTime1, historyItem.stepDetails.appCpuSTime1);
                            if (historyItem.stepDetails.appCpuUid2 >= 0) {
                                printStepCpuUidCheckinDetails(sb, historyItem.stepDetails.appCpuUid2, historyItem.stepDetails.appCpuUTime2, historyItem.stepDetails.appCpuSTime2);
                            }
                            if (historyItem.stepDetails.appCpuUid3 >= 0) {
                                printStepCpuUidCheckinDetails(sb, historyItem.stepDetails.appCpuUid3, historyItem.stepDetails.appCpuUTime3, historyItem.stepDetails.appCpuSTime3);
                            }
                        }
                        sb.append("\n");
                        sb.append(9);
                        sb.append(',');
                        sb.append(BatteryStats.HISTORY_DATA);
                        sb.append(",0,Dpst=");
                        sb.append(historyItem.stepDetails.statUserTime);
                        sb.append(',');
                        sb.append(historyItem.stepDetails.statSystemTime);
                        sb.append(',');
                        sb.append(historyItem.stepDetails.statIOWaitTime);
                        sb.append(',');
                        sb.append(historyItem.stepDetails.statIrqTime);
                        sb.append(',');
                        sb.append(historyItem.stepDetails.statSoftIrqTime);
                        sb.append(',');
                        sb.append(historyItem.stepDetails.statIdlTime);
                        sb.append(',');
                        if (historyItem.stepDetails.statPlatformIdleState != null) {
                            sb.append(historyItem.stepDetails.statPlatformIdleState);
                            if (historyItem.stepDetails.statSubsystemPowerState != null) {
                                sb.append(',');
                            }
                        }
                        if (historyItem.stepDetails.statSubsystemPowerState != null) {
                            sb.append(historyItem.stepDetails.statSubsystemPowerState);
                        }
                        sb.append("\n");
                    }
                }
                this.oldState = historyItem.states;
                this.oldState2 = historyItem.states2;
            }
            return sb.toString();
        }

        private void printStepCpuUidDetails(StringBuilder sb, int i, int i2, int i3) {
            UserHandle.formatUid(sb, i);
            sb.append("=");
            sb.append(i2);
            sb.append("u+");
            sb.append(i3);
            sb.append("s");
        }

        private void printStepCpuUidCheckinDetails(StringBuilder sb, int i, int i2, int i3) {
            sb.append('/');
            sb.append(i);
            sb.append(SettingsStringUtil.DELIMITER);
            sb.append(i2);
            sb.append(SettingsStringUtil.DELIMITER);
            sb.append(i3);
        }
    }

    private void printSizeValue(PrintWriter printWriter, long j) {
        float f = j;
        String str = "";
        if (f >= 10240.0f) {
            str = "KB";
            f /= 1024.0f;
        }
        if (f >= 10240.0f) {
            str = "MB";
            f /= 1024.0f;
        }
        if (f >= 10240.0f) {
            str = "GB";
            f /= 1024.0f;
        }
        if (f >= 10240.0f) {
            str = "TB";
            f /= 1024.0f;
        }
        if (f >= 10240.0f) {
            str = "PB";
            f /= 1024.0f;
        }
        printWriter.print((int) f);
        printWriter.print(str);
    }

    private static boolean dumpTimeEstimate(PrintWriter printWriter, String str, String str2, String str3, long j) {
        if (j < 0) {
            return false;
        }
        printWriter.print(str);
        printWriter.print(str2);
        printWriter.print(str3);
        StringBuilder sb = new StringBuilder(64);
        formatTimeMs(sb, j);
        printWriter.print(sb);
        printWriter.println();
        return true;
    }

    private static boolean dumpDurationSteps(PrintWriter printWriter, String str, String str2, LevelStepTracker levelStepTracker, boolean z) {
        int i;
        int i2;
        char c;
        char c2 = 0;
        if (levelStepTracker == null || (i = levelStepTracker.mNumStepDurations) <= 0) {
            return false;
        }
        if (!z) {
            printWriter.println(str2);
        }
        String[] strArr = new String[5];
        int i3 = 0;
        for (i = levelStepTracker.mNumStepDurations; i3 < i; i = i2) {
            long durationAt = levelStepTracker.getDurationAt(i3);
            int levelAt = levelStepTracker.getLevelAt(i3);
            long initModeAt = levelStepTracker.getInitModeAt(i3);
            long modModeAt = levelStepTracker.getModModeAt(i3);
            if (z) {
                strArr[c2] = Long.toString(durationAt);
                strArr[1] = Integer.toString(levelAt);
                if ((modModeAt & 3) == 0) {
                    i2 = i;
                    switch (((int) (initModeAt & 3)) + 1) {
                        case 1:
                            strArr[2] = "s-";
                            break;
                        case 2:
                            strArr[2] = "s+";
                            break;
                        case 3:
                            strArr[2] = "sd";
                            break;
                        case 4:
                            strArr[2] = "sds";
                            break;
                        default:
                            strArr[2] = "?";
                            break;
                    }
                } else {
                    i2 = i;
                    strArr[2] = "";
                }
                if ((modModeAt & 4) == 0) {
                    strArr[3] = (initModeAt & 4) != 0 ? "p+" : "p-";
                } else {
                    strArr[3] = "";
                }
                if ((modModeAt & 8) == 0) {
                    strArr[4] = (initModeAt & 8) != 0 ? "i+" : "i-";
                } else {
                    strArr[4] = "";
                }
                dumpLine(printWriter, 0, "i", str2, strArr);
                c2 = 0;
            } else {
                i2 = i;
                printWriter.print(str);
                printWriter.print("#");
                printWriter.print(i3);
                printWriter.print(": ");
                TimeUtils.formatDuration(durationAt, printWriter);
                printWriter.print(" to ");
                printWriter.print(levelAt);
                if ((modModeAt & 3) == 0) {
                    printWriter.print(" (");
                    switch (((int) (initModeAt & 3)) + 1) {
                        case 1:
                            printWriter.print("screen-off");
                            break;
                        case 2:
                            printWriter.print("screen-on");
                            break;
                        case 3:
                            printWriter.print("screen-doze");
                            break;
                        case 4:
                            printWriter.print("screen-doze-suspend");
                            break;
                        default:
                            printWriter.print("screen-?");
                            break;
                    }
                    c = 1;
                } else {
                    c = c2;
                }
                if ((modModeAt & 4) == 0) {
                    printWriter.print(c != 0 ? ", " : " (");
                    printWriter.print((initModeAt & 4) != 0 ? "power-save-on" : "power-save-off");
                    c = 1;
                }
                if ((modModeAt & 8) == 0) {
                    printWriter.print(c != 0 ? ", " : " (");
                    printWriter.print((initModeAt & 8) != 0 ? "device-idle-on" : "device-idle-off");
                    c = 1;
                }
                if (c != 0) {
                    printWriter.print(")");
                }
                printWriter.println();
            }
            i3++;
        }
        return true;
    }

    private static void dumpDurationSteps(ProtoOutputStream protoOutputStream, long j, LevelStepTracker levelStepTracker) {
        int i;
        int i2;
        if (levelStepTracker == null) {
            return;
        }
        int i3 = levelStepTracker.mNumStepDurations;
        for (int i4 = 0; i4 < i3; i4++) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1112396529665L, levelStepTracker.getDurationAt(i4));
            protoOutputStream.write(1120986464258L, levelStepTracker.getLevelAt(i4));
            long initModeAt = levelStepTracker.getInitModeAt(i4);
            long modModeAt = levelStepTracker.getModModeAt(i4);
            int i5 = 3;
            if ((modModeAt & 3) != 0) {
                i = 0;
            } else {
                switch (((int) (3 & initModeAt)) + 1) {
                    case 1:
                        i = 2;
                        break;
                    case 2:
                        i = 1;
                        break;
                    case 3:
                        i = 3;
                        break;
                    case 4:
                        i = 4;
                        break;
                    default:
                        i = 5;
                        break;
                }
            }
            protoOutputStream.write(1159641169923L, i);
            if ((modModeAt & 4) != 0) {
                i2 = 0;
            } else {
                i2 = (4 & initModeAt) == 0 ? 2 : 1;
            }
            protoOutputStream.write(1159641169924L, i2);
            if ((modModeAt & 8) != 0) {
                i5 = 0;
            } else if ((initModeAt & 8) != 0) {
                i5 = 2;
            }
            protoOutputStream.write(1159641169925L, i5);
            protoOutputStream.end(jStart);
        }
    }

    private void dumpHistoryLocked(PrintWriter printWriter, int i, long j, boolean z) {
        long j2;
        HistoryPrinter historyPrinter = new HistoryPrinter();
        HistoryItem historyItem = new HistoryItem();
        long j3 = -1;
        long j4 = -1;
        boolean z2 = false;
        while (getNextHistoryLocked(historyItem)) {
            long j5 = historyItem.time;
            long j6 = j3 < 0 ? j5 : j3;
            if (historyItem.time >= j) {
                if (j >= 0 && !z2) {
                    if (historyItem.cmd == 5 || historyItem.cmd == 7 || historyItem.cmd == 4 || historyItem.cmd == 8) {
                        j2 = j5;
                        historyPrinter.printNextItem(printWriter, historyItem, j6, z, (i & 32) != 0);
                        historyItem.cmd = (byte) 0;
                    } else if (historyItem.currentTime != 0) {
                        byte b = historyItem.cmd;
                        historyItem.cmd = (byte) 5;
                        j2 = j5;
                        historyPrinter.printNextItem(printWriter, historyItem, j6, z, (i & 32) != 0);
                        historyItem.cmd = b;
                    } else {
                        j2 = j5;
                    }
                    z2 = true;
                } else {
                    j2 = j5;
                }
                boolean z3 = z2;
                historyPrinter.printNextItem(printWriter, historyItem, j6, z, (i & 32) != 0);
                z2 = z3;
            } else {
                j2 = j5;
            }
            j3 = j6;
            j4 = j2;
        }
        if (j >= 0) {
            commitCurrentHistoryBatchLocked();
            printWriter.print(z ? "NEXT: " : "  NEXT: ");
            printWriter.println(j4 + 1);
        }
    }

    private void dumpDailyLevelStepSummary(PrintWriter printWriter, String str, String str2, LevelStepTracker levelStepTracker, StringBuilder sb, int[] iArr) {
        if (levelStepTracker == null) {
            return;
        }
        long jComputeTimeEstimate = levelStepTracker.computeTimeEstimate(0L, 0L, iArr);
        if (jComputeTimeEstimate >= 0) {
            printWriter.print(str);
            printWriter.print(str2);
            printWriter.print(" total time: ");
            sb.setLength(0);
            formatTimeMs(sb, jComputeTimeEstimate);
            printWriter.print(sb);
            printWriter.print(" (from ");
            printWriter.print(iArr[0]);
            printWriter.println(" steps)");
        }
        for (int i = 0; i < STEP_LEVEL_MODES_OF_INTEREST.length; i++) {
            long jComputeTimeEstimate2 = levelStepTracker.computeTimeEstimate(STEP_LEVEL_MODES_OF_INTEREST[i], STEP_LEVEL_MODE_VALUES[i], iArr);
            if (jComputeTimeEstimate2 > 0) {
                printWriter.print(str);
                printWriter.print(str2);
                printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                printWriter.print(STEP_LEVEL_MODE_LABELS[i]);
                printWriter.print(" time: ");
                sb.setLength(0);
                formatTimeMs(sb, jComputeTimeEstimate2);
                printWriter.print(sb);
                printWriter.print(" (from ");
                printWriter.print(iArr[0]);
                printWriter.println(" steps)");
            }
        }
    }

    private void dumpDailyPackageChanges(PrintWriter printWriter, String str, ArrayList<PackageChange> arrayList) {
        if (arrayList == null) {
            return;
        }
        printWriter.print(str);
        printWriter.println("Package changes:");
        for (int i = 0; i < arrayList.size(); i++) {
            PackageChange packageChange = arrayList.get(i);
            if (packageChange.mUpdate) {
                printWriter.print(str);
                printWriter.print("  Update ");
                printWriter.print(packageChange.mPackageName);
                printWriter.print(" vers=");
                printWriter.println(packageChange.mVersionCode);
            } else {
                printWriter.print(str);
                printWriter.print("  Uninstall ");
                printWriter.println(packageChange.mPackageName);
            }
        }
    }

    public void dumpLocked(Context context, PrintWriter printWriter, int i, int i2, long j) {
        ArrayList<PackageChange> arrayList;
        boolean z;
        prepareForDumpLocked();
        boolean z2 = (i & 14) != 0;
        if ((i & 8) != 0 || !z2) {
            long historyTotalSize = getHistoryTotalSize();
            long historyUsedSize = getHistoryUsedSize();
            if (startIteratingHistoryLocked()) {
                try {
                    printWriter.print("Battery History (");
                    printWriter.print((100 * historyUsedSize) / historyTotalSize);
                    printWriter.print("% used, ");
                    printSizeValue(printWriter, historyUsedSize);
                    printWriter.print(" used of ");
                    printSizeValue(printWriter, historyTotalSize);
                    printWriter.print(", ");
                    printWriter.print(getHistoryStringPoolSize());
                    printWriter.print(" strings using ");
                    printSizeValue(printWriter, getHistoryStringPoolBytes());
                    printWriter.println("):");
                    dumpHistoryLocked(printWriter, i, j, false);
                    printWriter.println();
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
            if (startIteratingOldHistoryLocked()) {
                try {
                    HistoryItem historyItem = new HistoryItem();
                    printWriter.println("Old battery History:");
                    HistoryPrinter historyPrinter = new HistoryPrinter();
                    long j2 = -1;
                    while (getNextOldHistoryLocked(historyItem)) {
                        if (j2 < 0) {
                            j2 = historyItem.time;
                        }
                        long j3 = j2;
                        historyPrinter.printNextItem(printWriter, historyItem, j3, false, (i & 32) != 0);
                        j2 = j3;
                        historyPrinter = historyPrinter;
                    }
                    printWriter.println();
                } finally {
                    finishIteratingOldHistoryLocked();
                }
            }
        }
        if (z2 && (i & 6) == 0) {
            return;
        }
        if (!z2) {
            SparseArray<? extends Uid> uidStats = getUidStats();
            int size = uidStats.size();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            boolean z3 = false;
            for (int i3 = 0; i3 < size; i3++) {
                SparseArray<? extends Uid.Pid> pidStats = uidStats.valueAt(i3).getPidStats();
                if (pidStats != null) {
                    boolean z4 = z3;
                    for (int i4 = 0; i4 < pidStats.size(); i4++) {
                        Uid.Pid pidValueAt = pidStats.valueAt(i4);
                        if (!z4) {
                            printWriter.println("Per-PID Stats:");
                            z4 = true;
                        }
                        long j4 = pidValueAt.mWakeSumMs + (pidValueAt.mWakeNesting > 0 ? jElapsedRealtime - pidValueAt.mWakeStartMs : 0L);
                        printWriter.print("  PID ");
                        printWriter.print(pidStats.keyAt(i4));
                        printWriter.print(" wake time: ");
                        TimeUtils.formatDuration(j4, printWriter);
                        printWriter.println("");
                    }
                    z3 = z4;
                }
            }
            if (z3) {
                printWriter.println();
            }
        }
        if (!z2 || (i & 2) != 0) {
            if (dumpDurationSteps(printWriter, "  ", "Discharge step durations:", getDischargeLevelStepTracker(), false)) {
                long jComputeBatteryTimeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                if (jComputeBatteryTimeRemaining >= 0) {
                    printWriter.print("  Estimated discharge time remaining: ");
                    TimeUtils.formatDuration(jComputeBatteryTimeRemaining / 1000, printWriter);
                    printWriter.println();
                }
                LevelStepTracker dischargeLevelStepTracker = getDischargeLevelStepTracker();
                for (int i5 = 0; i5 < STEP_LEVEL_MODES_OF_INTEREST.length; i5++) {
                    dumpTimeEstimate(printWriter, "  Estimated ", STEP_LEVEL_MODE_LABELS[i5], " time: ", dischargeLevelStepTracker.computeTimeEstimate(STEP_LEVEL_MODES_OF_INTEREST[i5], STEP_LEVEL_MODE_VALUES[i5], null));
                }
                printWriter.println();
            }
            if (dumpDurationSteps(printWriter, "  ", "Charge step durations:", getChargeLevelStepTracker(), false)) {
                long jComputeChargeTimeRemaining = computeChargeTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                if (jComputeChargeTimeRemaining >= 0) {
                    printWriter.print("  Estimated charge time remaining: ");
                    TimeUtils.formatDuration(jComputeChargeTimeRemaining / 1000, printWriter);
                    printWriter.println();
                }
                printWriter.println();
            }
        }
        if (!z2 || (i & 4) != 0) {
            printWriter.println("Daily stats:");
            printWriter.print("  Current start time: ");
            printWriter.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getCurrentDailyStartTime()).toString());
            printWriter.print("  Next min deadline: ");
            printWriter.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getNextMinDailyDeadline()).toString());
            printWriter.print("  Next max deadline: ");
            printWriter.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getNextMaxDailyDeadline()).toString());
            StringBuilder sb = new StringBuilder(64);
            int[] iArr = new int[1];
            LevelStepTracker dailyDischargeLevelStepTracker = getDailyDischargeLevelStepTracker();
            LevelStepTracker dailyChargeLevelStepTracker = getDailyChargeLevelStepTracker();
            ArrayList<PackageChange> dailyPackageChanges = getDailyPackageChanges();
            if (dailyDischargeLevelStepTracker.mNumStepDurations > 0 || dailyChargeLevelStepTracker.mNumStepDurations > 0 || dailyPackageChanges != null) {
                if ((i & 4) != 0 || !z2) {
                    if (dumpDurationSteps(printWriter, "    ", "  Current daily discharge step durations:", dailyDischargeLevelStepTracker, false)) {
                        arrayList = dailyPackageChanges;
                        dumpDailyLevelStepSummary(printWriter, "      ", "Discharge", dailyDischargeLevelStepTracker, sb, iArr);
                    } else {
                        arrayList = dailyPackageChanges;
                    }
                    if (dumpDurationSteps(printWriter, "    ", "  Current daily charge step durations:", dailyChargeLevelStepTracker, false)) {
                        dumpDailyLevelStepSummary(printWriter, "      ", "Charge", dailyChargeLevelStepTracker, sb, iArr);
                    }
                    dumpDailyPackageChanges(printWriter, "    ", arrayList);
                } else {
                    printWriter.println("  Current daily steps:");
                    dumpDailyLevelStepSummary(printWriter, "    ", "Discharge", dailyDischargeLevelStepTracker, sb, iArr);
                    dumpDailyLevelStepSummary(printWriter, "    ", "Charge", dailyChargeLevelStepTracker, sb, iArr);
                }
            }
            int i6 = 0;
            while (true) {
                DailyItem dailyItemLocked = getDailyItemLocked(i6);
                if (dailyItemLocked == null) {
                    break;
                }
                int i7 = i6 + 1;
                int i8 = i & 4;
                if (i8 != 0) {
                    printWriter.println();
                }
                printWriter.print("  Daily from ");
                printWriter.print(DateFormat.format("yyyy-MM-dd-HH-mm-ss", dailyItemLocked.mStartTime).toString());
                printWriter.print(" to ");
                printWriter.print(DateFormat.format("yyyy-MM-dd-HH-mm-ss", dailyItemLocked.mEndTime).toString());
                printWriter.println(SettingsStringUtil.DELIMITER);
                if (i8 != 0 || !z2) {
                    if (dumpDurationSteps(printWriter, "      ", "    Discharge step durations:", dailyItemLocked.mDischargeSteps, false)) {
                        dumpDailyLevelStepSummary(printWriter, "        ", "Discharge", dailyItemLocked.mDischargeSteps, sb, iArr);
                    }
                    if (dumpDurationSteps(printWriter, "      ", "    Charge step durations:", dailyItemLocked.mChargeSteps, false)) {
                        dumpDailyLevelStepSummary(printWriter, "        ", "Charge", dailyItemLocked.mChargeSteps, sb, iArr);
                    }
                    dumpDailyPackageChanges(printWriter, "    ", dailyItemLocked.mPackageChanges);
                } else {
                    dumpDailyLevelStepSummary(printWriter, "    ", "Discharge", dailyItemLocked.mDischargeSteps, sb, iArr);
                    dumpDailyLevelStepSummary(printWriter, "    ", "Charge", dailyItemLocked.mChargeSteps, sb, iArr);
                }
                i6 = i7;
            }
            z = false;
            printWriter.println();
        } else {
            z = false;
        }
        if (!z2 || (i & 2) != 0) {
            printWriter.println("Statistics since last charge:");
            printWriter.println("  System starts: " + getStartCount() + ", currently on battery: " + getIsOnBattery());
            dumpLocked(context, printWriter, "", 0, i2, (i & 64) != 0 ? true : z);
            printWriter.println();
        }
    }

    public void dumpCheckinLocked(Context context, PrintWriter printWriter, List<ApplicationInfo> list, int i, long j) {
        prepareForDumpLocked();
        dumpLine(printWriter, 0, "i", VERSION_DATA, 32, Integer.valueOf(getParcelVersion()), getStartPlatformVersion(), getEndPlatformVersion());
        getHistoryBaseTime();
        SystemClock.elapsedRealtime();
        if ((i & 24) != 0 && startIteratingHistoryLocked()) {
            for (int i2 = 0; i2 < getHistoryStringPoolSize(); i2++) {
                try {
                    printWriter.print(9);
                    printWriter.print(',');
                    printWriter.print(HISTORY_STRING_POOL);
                    printWriter.print(',');
                    printWriter.print(i2);
                    printWriter.print(",");
                    printWriter.print(getHistoryTagPoolUid(i2));
                    printWriter.print(",\"");
                    printWriter.print(getHistoryTagPoolString(i2).replace("\\", "\\\\").replace("\"", "\\\""));
                    printWriter.print("\"");
                    printWriter.println();
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
            dumpHistoryLocked(printWriter, i, j, true);
        }
        if ((i & 8) != 0) {
            return;
        }
        if (list != null) {
            SparseArray sparseArray = new SparseArray();
            for (int i3 = 0; i3 < list.size(); i3++) {
                ApplicationInfo applicationInfo = list.get(i3);
                Pair pair = (Pair) sparseArray.get(UserHandle.getAppId(applicationInfo.uid));
                if (pair == null) {
                    pair = new Pair(new ArrayList(), new MutableBoolean(false));
                    sparseArray.put(UserHandle.getAppId(applicationInfo.uid), pair);
                }
                ((ArrayList) pair.first).add(applicationInfo.packageName);
            }
            SparseArray<? extends Uid> uidStats = getUidStats();
            int size = uidStats.size();
            String[] strArr = new String[2];
            for (int i4 = 0; i4 < size; i4++) {
                int appId = UserHandle.getAppId(uidStats.keyAt(i4));
                Pair pair2 = (Pair) sparseArray.get(appId);
                if (pair2 != null && !((MutableBoolean) pair2.second).value) {
                    ((MutableBoolean) pair2.second).value = true;
                    for (int i5 = 0; i5 < ((ArrayList) pair2.first).size(); i5++) {
                        strArr[0] = Integer.toString(appId);
                        strArr[1] = (String) ((ArrayList) pair2.first).get(i5);
                        dumpLine(printWriter, 0, "i", "uid", strArr);
                    }
                }
            }
        }
        if ((i & 4) == 0) {
            dumpDurationSteps(printWriter, "", DISCHARGE_STEP_DATA, getDischargeLevelStepTracker(), true);
            String[] strArr2 = new String[1];
            long jComputeBatteryTimeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000);
            if (jComputeBatteryTimeRemaining >= 0) {
                strArr2[0] = Long.toString(jComputeBatteryTimeRemaining);
                dumpLine(printWriter, 0, "i", DISCHARGE_TIME_REMAIN_DATA, strArr2);
            }
            dumpDurationSteps(printWriter, "", CHARGE_STEP_DATA, getChargeLevelStepTracker(), true);
            long jComputeChargeTimeRemaining = computeChargeTimeRemaining(SystemClock.elapsedRealtime() * 1000);
            if (jComputeChargeTimeRemaining >= 0) {
                strArr2[0] = Long.toString(jComputeChargeTimeRemaining);
                dumpLine(printWriter, 0, "i", CHARGE_TIME_REMAIN_DATA, strArr2);
            }
            dumpCheckinLocked(context, printWriter, 0, -1, (i & 64) != 0);
        }
    }

    public void dumpProtoLocked(Context context, FileDescriptor fileDescriptor, List<ApplicationInfo> list, int i, long j) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        prepareForDumpLocked();
        if ((i & 24) != 0) {
            dumpProtoHistoryLocked(protoOutputStream, i, j);
            protoOutputStream.flush();
            return;
        }
        long jStart = protoOutputStream.start(1146756268033L);
        protoOutputStream.write(1120986464257L, 32);
        protoOutputStream.write(1112396529666L, getParcelVersion());
        protoOutputStream.write(1138166333443L, getStartPlatformVersion());
        protoOutputStream.write(1138166333444L, getEndPlatformVersion());
        if ((i & 4) == 0) {
            BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, false, (i & 64) != 0);
            batteryStatsHelper.create(this);
            batteryStatsHelper.refreshStats(0, -1);
            dumpProtoAppsLocked(protoOutputStream, batteryStatsHelper, list);
            dumpProtoSystemLocked(protoOutputStream, batteryStatsHelper);
        }
        protoOutputStream.end(jStart);
        protoOutputStream.flush();
    }

    private void dumpProtoAppsLocked(ProtoOutputStream protoOutputStream, BatteryStatsHelper batteryStatsHelper, List<ApplicationInfo> list) {
        ArrayList<String> arrayList;
        long j;
        ArrayMap<String, ? extends Uid.Wakelock> arrayMap;
        ArrayMap<String, SparseIntArray> arrayMap2;
        int[] iArr;
        long j2;
        long[] cpuFreqTimes;
        int countLocked;
        int countLocked2;
        long totalDurationMsLocked;
        long j3;
        ArrayMap<String, ? extends Uid.Pkg> arrayMap3;
        ArrayList arrayList2;
        int i;
        long j4;
        long j5;
        SparseArray sparseArray;
        SparseArray sparseArray2;
        ArrayList arrayList3;
        int i2;
        long jUptimeMillis = SystemClock.uptimeMillis() * 1000;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j6 = jElapsedRealtime * 1000;
        long batteryUptime = getBatteryUptime(jUptimeMillis);
        SparseArray sparseArray3 = new SparseArray();
        if (list != null) {
            for (int i3 = 0; i3 < list.size(); i3++) {
                ApplicationInfo applicationInfo = list.get(i3);
                int appId = UserHandle.getAppId(applicationInfo.uid);
                ArrayList arrayList4 = (ArrayList) sparseArray3.get(appId);
                if (arrayList4 == null) {
                    arrayList4 = new ArrayList();
                    sparseArray3.put(appId, arrayList4);
                }
                arrayList4.add(applicationInfo.packageName);
            }
        }
        SparseArray sparseArray4 = new SparseArray();
        List<BatterySipper> usageList = batteryStatsHelper.getUsageList();
        if (usageList != null) {
            for (int i4 = 0; i4 < usageList.size(); i4++) {
                BatterySipper batterySipper = usageList.get(i4);
                if (batterySipper.drainType == BatterySipper.DrainType.APP) {
                    sparseArray4.put(batterySipper.uidObj.getUid(), batterySipper);
                }
            }
        }
        SparseArray<? extends Uid> uidStats = getUidStats();
        int size = uidStats.size();
        int i5 = 0;
        while (i5 < size) {
            int i6 = size;
            long jStart = protoOutputStream.start(2246267895813L);
            Uid uidValueAt = uidStats.valueAt(i5);
            int iKeyAt = uidStats.keyAt(i5);
            SparseArray sparseArray5 = sparseArray4;
            SparseArray<? extends Uid> sparseArray6 = uidStats;
            protoOutputStream.write(1120986464257L, iKeyAt);
            ArrayList arrayList5 = (ArrayList) sparseArray3.get(UserHandle.getAppId(iKeyAt));
            if (arrayList5 == null) {
                arrayList5 = new ArrayList();
            }
            int i7 = i5;
            ArrayMap<String, ? extends Uid.Pkg> packageStats = uidValueAt.getPackageStats();
            int size2 = packageStats.size() - 1;
            while (true) {
                arrayList = arrayList5;
                if (size2 < 0) {
                    break;
                }
                String strKeyAt = packageStats.keyAt(size2);
                ArrayMap<String, ? extends Uid.Pkg.Serv> serviceStats = packageStats.valueAt(size2).getServiceStats();
                if (serviceStats.size() == 0) {
                    arrayMap3 = packageStats;
                    i = size2;
                    j4 = jElapsedRealtime;
                    j3 = j6;
                    j5 = batteryUptime;
                    sparseArray = sparseArray3;
                    arrayList2 = arrayList;
                } else {
                    j3 = j6;
                    long jStart2 = protoOutputStream.start(2246267895810L);
                    protoOutputStream.write(1138166333441L, strKeyAt);
                    ArrayList arrayList6 = arrayList;
                    arrayList6.remove(strKeyAt);
                    int size3 = serviceStats.size() - 1;
                    while (size3 >= 0) {
                        Uid.Pkg.Serv servValueAt = serviceStats.valueAt(size3);
                        ArrayMap<String, ? extends Uid.Pkg> arrayMap4 = packageStats;
                        long j7 = jElapsedRealtime;
                        long jRoundUsToMs = roundUsToMs(servValueAt.getStartTime(batteryUptime, 0));
                        long j8 = batteryUptime;
                        int starts = servValueAt.getStarts(0);
                        int launches = servValueAt.getLaunches(0);
                        if (jRoundUsToMs == 0 && starts == 0 && launches == 0) {
                            arrayList3 = arrayList6;
                            i2 = size2;
                            sparseArray2 = sparseArray3;
                        } else {
                            sparseArray2 = sparseArray3;
                            long jStart3 = protoOutputStream.start(2246267895810L);
                            size3 = size3;
                            arrayList3 = arrayList6;
                            i2 = size2;
                            protoOutputStream.write(1138166333441L, serviceStats.keyAt(size3));
                            protoOutputStream.write(1112396529666L, jRoundUsToMs);
                            protoOutputStream.write(1120986464259L, starts);
                            protoOutputStream.write(1120986464260L, launches);
                            protoOutputStream.end(jStart3);
                        }
                        size3--;
                        packageStats = arrayMap4;
                        jElapsedRealtime = j7;
                        batteryUptime = j8;
                        sparseArray3 = sparseArray2;
                        size2 = i2;
                        arrayList6 = arrayList3;
                    }
                    arrayMap3 = packageStats;
                    arrayList2 = arrayList6;
                    i = size2;
                    j4 = jElapsedRealtime;
                    j5 = batteryUptime;
                    sparseArray = sparseArray3;
                    protoOutputStream.end(jStart2);
                }
                size2 = i - 1;
                j6 = j3;
                packageStats = arrayMap3;
                jElapsedRealtime = j4;
                batteryUptime = j5;
                sparseArray3 = sparseArray;
                arrayList5 = arrayList2;
            }
            ArrayMap<String, ? extends Uid.Pkg> arrayMap5 = packageStats;
            long j9 = jElapsedRealtime;
            long j10 = j6;
            long j11 = batteryUptime;
            SparseArray sparseArray7 = sparseArray3;
            for (String str : arrayList) {
                long jStart4 = protoOutputStream.start(2246267895810L);
                protoOutputStream.write(1138166333441L, str);
                protoOutputStream.end(jStart4);
            }
            if (uidValueAt.getAggregatedPartialWakelockTimer() != null) {
                Timer aggregatedPartialWakelockTimer = uidValueAt.getAggregatedPartialWakelockTimer();
                j = j9;
                long totalDurationMsLocked2 = aggregatedPartialWakelockTimer.getTotalDurationMsLocked(j);
                Timer subTimer = aggregatedPartialWakelockTimer.getSubTimer();
                if (subTimer != null) {
                    totalDurationMsLocked = subTimer.getTotalDurationMsLocked(j);
                } else {
                    totalDurationMsLocked = 0;
                }
                long jStart5 = protoOutputStream.start(1146756268056L);
                protoOutputStream.write(1112396529665L, totalDurationMsLocked2);
                protoOutputStream.write(1112396529666L, totalDurationMsLocked);
                protoOutputStream.end(jStart5);
            } else {
                j = j9;
            }
            int i8 = i7;
            int i9 = iKeyAt;
            SparseArray sparseArray8 = sparseArray5;
            long j12 = j;
            dumpTimer(protoOutputStream, 1146756268040L, uidValueAt.getAudioTurnedOnTimer(), j10, 0);
            dumpControllerActivityProto(protoOutputStream, 1146756268035L, uidValueAt.getBluetoothControllerActivity(), 0);
            Timer bluetoothScanTimer = uidValueAt.getBluetoothScanTimer();
            if (bluetoothScanTimer != null) {
                long jStart6 = protoOutputStream.start(1146756268038L);
                dumpTimer(protoOutputStream, 1146756268033L, bluetoothScanTimer, j10, 0);
                dumpTimer(protoOutputStream, 1146756268034L, uidValueAt.getBluetoothScanBackgroundTimer(), j10, 0);
                dumpTimer(protoOutputStream, 1146756268035L, uidValueAt.getBluetoothUnoptimizedScanTimer(), j10, 0);
                dumpTimer(protoOutputStream, 1146756268036L, uidValueAt.getBluetoothUnoptimizedScanBackgroundTimer(), j10, 0);
                if (uidValueAt.getBluetoothScanResultCounter() == null) {
                    countLocked = 0;
                } else {
                    countLocked = uidValueAt.getBluetoothScanResultCounter().getCountLocked(0);
                }
                protoOutputStream.write(1120986464261L, countLocked);
                if (uidValueAt.getBluetoothScanResultBgCounter() == null) {
                    countLocked2 = 0;
                } else {
                    countLocked2 = uidValueAt.getBluetoothScanResultBgCounter().getCountLocked(0);
                }
                protoOutputStream.write(1120986464262L, countLocked2);
                protoOutputStream.end(jStart6);
            }
            dumpTimer(protoOutputStream, 1146756268041L, uidValueAt.getCameraTurnedOnTimer(), j10, 0);
            long jStart7 = protoOutputStream.start(1146756268039L);
            protoOutputStream.write(1112396529665L, roundUsToMs(uidValueAt.getUserCpuTimeUs(0)));
            protoOutputStream.write(1112396529666L, roundUsToMs(uidValueAt.getSystemCpuTimeUs(0)));
            long[] cpuFreqs = getCpuFreqs();
            if (cpuFreqs != null && (cpuFreqTimes = uidValueAt.getCpuFreqTimes(0)) != null && cpuFreqTimes.length == cpuFreqs.length) {
                long[] screenOffCpuFreqTimes = uidValueAt.getScreenOffCpuFreqTimes(0);
                if (screenOffCpuFreqTimes == null) {
                    screenOffCpuFreqTimes = new long[cpuFreqTimes.length];
                }
                int i10 = 0;
                while (i10 < cpuFreqTimes.length) {
                    long jStart8 = protoOutputStream.start(2246267895811L);
                    int i11 = i10 + 1;
                    protoOutputStream.write(1120986464257L, i11);
                    protoOutputStream.write(1112396529666L, cpuFreqTimes[i10]);
                    protoOutputStream.write(1112396529667L, screenOffCpuFreqTimes[i10]);
                    protoOutputStream.end(jStart8);
                    i10 = i11;
                    i8 = i8;
                    i9 = i9;
                    sparseArray8 = sparseArray8;
                }
            }
            int i12 = i8;
            int i13 = i9;
            SparseArray sparseArray9 = sparseArray8;
            int i14 = 0;
            while (i14 < 7) {
                long[] cpuFreqTimes2 = uidValueAt.getCpuFreqTimes(0, i14);
                if (cpuFreqTimes2 == null || cpuFreqTimes2.length != cpuFreqs.length) {
                    j2 = jStart7;
                } else {
                    long[] screenOffCpuFreqTimes2 = uidValueAt.getScreenOffCpuFreqTimes(0, i14);
                    if (screenOffCpuFreqTimes2 == null) {
                        screenOffCpuFreqTimes2 = new long[cpuFreqTimes2.length];
                    }
                    long jStart9 = protoOutputStream.start(2246267895812L);
                    protoOutputStream.write(1159641169921L, i14);
                    int i15 = 0;
                    while (i15 < cpuFreqTimes2.length) {
                        long jStart10 = protoOutputStream.start(2246267895810L);
                        int i16 = i15 + 1;
                        protoOutputStream.write(1120986464257L, i16);
                        protoOutputStream.write(1112396529666L, cpuFreqTimes2[i15]);
                        protoOutputStream.write(1112396529667L, screenOffCpuFreqTimes2[i15]);
                        protoOutputStream.end(jStart10);
                        i15 = i16;
                        jStart7 = jStart7;
                    }
                    j2 = jStart7;
                    protoOutputStream.end(jStart9);
                }
                i14++;
                jStart7 = j2;
            }
            protoOutputStream.end(jStart7);
            dumpTimer(protoOutputStream, 1146756268042L, uidValueAt.getFlashlightTurnedOnTimer(), j10, 0);
            dumpTimer(protoOutputStream, 1146756268043L, uidValueAt.getForegroundActivityTimer(), j10, 0);
            dumpTimer(protoOutputStream, 1146756268044L, uidValueAt.getForegroundServiceTimer(), j10, 0);
            ArrayMap<String, SparseIntArray> jobCompletionStats = uidValueAt.getJobCompletionStats();
            int[] iArr2 = {0, 1, 2, 3, 4};
            int i17 = 0;
            while (i17 < jobCompletionStats.size()) {
                SparseIntArray sparseIntArrayValueAt = jobCompletionStats.valueAt(i17);
                if (sparseIntArrayValueAt == null) {
                    arrayMap2 = jobCompletionStats;
                    iArr = iArr2;
                } else {
                    long jStart11 = protoOutputStream.start(2246267895824L);
                    protoOutputStream.write(1138166333441L, jobCompletionStats.keyAt(i17));
                    int length = iArr2.length;
                    int i18 = 0;
                    while (i18 < length) {
                        int i19 = iArr2[i18];
                        ArrayMap<String, SparseIntArray> arrayMap6 = jobCompletionStats;
                        long jStart12 = protoOutputStream.start(2246267895810L);
                        protoOutputStream.write(1159641169921L, i19);
                        protoOutputStream.write(1120986464258L, sparseIntArrayValueAt.get(i19, 0));
                        protoOutputStream.end(jStart12);
                        i18++;
                        jobCompletionStats = arrayMap6;
                        iArr2 = iArr2;
                    }
                    arrayMap2 = jobCompletionStats;
                    iArr = iArr2;
                    protoOutputStream.end(jStart11);
                }
                i17++;
                jobCompletionStats = arrayMap2;
                iArr2 = iArr;
            }
            ArrayMap<String, ? extends Timer> jobStats = uidValueAt.getJobStats();
            for (int size4 = jobStats.size() - 1; size4 >= 0; size4--) {
                Timer timerValueAt = jobStats.valueAt(size4);
                Timer subTimer2 = timerValueAt.getSubTimer();
                long jStart13 = protoOutputStream.start(2246267895823L);
                protoOutputStream.write(1138166333441L, jobStats.keyAt(size4));
                dumpTimer(protoOutputStream, 1146756268034L, timerValueAt, j10, 0);
                dumpTimer(protoOutputStream, 1146756268035L, subTimer2, j10, 0);
                protoOutputStream.end(jStart13);
            }
            dumpControllerActivityProto(protoOutputStream, 1146756268036L, uidValueAt.getModemControllerActivity(), 0);
            long jStart14 = protoOutputStream.start(1146756268049L);
            protoOutputStream.write(1112396529665L, uidValueAt.getNetworkActivityBytes(0, 0));
            protoOutputStream.write(1112396529666L, uidValueAt.getNetworkActivityBytes(1, 0));
            protoOutputStream.write(1112396529667L, uidValueAt.getNetworkActivityBytes(2, 0));
            protoOutputStream.write(1112396529668L, uidValueAt.getNetworkActivityBytes(3, 0));
            protoOutputStream.write(1112396529669L, uidValueAt.getNetworkActivityBytes(4, 0));
            protoOutputStream.write(1112396529670L, uidValueAt.getNetworkActivityBytes(5, 0));
            protoOutputStream.write(1112396529671L, uidValueAt.getNetworkActivityPackets(0, 0));
            protoOutputStream.write(1112396529672L, uidValueAt.getNetworkActivityPackets(1, 0));
            protoOutputStream.write(1112396529673L, uidValueAt.getNetworkActivityPackets(2, 0));
            protoOutputStream.write(1112396529674L, uidValueAt.getNetworkActivityPackets(3, 0));
            protoOutputStream.write(1112396529675L, roundUsToMs(uidValueAt.getMobileRadioActiveTime(0)));
            protoOutputStream.write(1120986464268L, uidValueAt.getMobileRadioActiveCount(0));
            protoOutputStream.write(1120986464269L, uidValueAt.getMobileRadioApWakeupCount(0));
            protoOutputStream.write(1120986464270L, uidValueAt.getWifiRadioApWakeupCount(0));
            protoOutputStream.write(1112396529679L, uidValueAt.getNetworkActivityBytes(6, 0));
            protoOutputStream.write(1112396529680L, uidValueAt.getNetworkActivityBytes(7, 0));
            protoOutputStream.write(1112396529681L, uidValueAt.getNetworkActivityBytes(8, 0));
            protoOutputStream.write(1112396529682L, uidValueAt.getNetworkActivityBytes(9, 0));
            protoOutputStream.write(1112396529683L, uidValueAt.getNetworkActivityPackets(6, 0));
            protoOutputStream.write(1112396529684L, uidValueAt.getNetworkActivityPackets(7, 0));
            protoOutputStream.write(1112396529685L, uidValueAt.getNetworkActivityPackets(8, 0));
            protoOutputStream.write(1112396529686L, uidValueAt.getNetworkActivityPackets(9, 0));
            protoOutputStream.end(jStart14);
            SparseArray sparseArray10 = sparseArray9;
            BatterySipper batterySipper2 = (BatterySipper) sparseArray10.get(i13);
            if (batterySipper2 != null) {
                long jStart15 = protoOutputStream.start(1146756268050L);
                protoOutputStream.write(1103806595073L, batterySipper2.totalPowerMah);
                protoOutputStream.write(1133871366146L, batterySipper2.shouldHide);
                protoOutputStream.write(1103806595075L, batterySipper2.screenPowerMah);
                protoOutputStream.write(1103806595076L, batterySipper2.proportionalSmearMah);
                protoOutputStream.end(jStart15);
            }
            ArrayMap<String, ? extends Uid.Proc> processStats = uidValueAt.getProcessStats();
            int size5 = processStats.size() - 1;
            while (size5 >= 0) {
                Uid.Proc procValueAt = processStats.valueAt(size5);
                long jStart16 = protoOutputStream.start(2246267895827L);
                protoOutputStream.write(1138166333441L, processStats.keyAt(size5));
                protoOutputStream.write(1112396529666L, procValueAt.getUserTime(0));
                protoOutputStream.write(1112396529667L, procValueAt.getSystemTime(0));
                protoOutputStream.write(1112396529668L, procValueAt.getForegroundTime(0));
                protoOutputStream.write(1120986464261L, procValueAt.getStarts(0));
                protoOutputStream.write(1120986464262L, procValueAt.getNumAnrs(0));
                protoOutputStream.write(1120986464263L, procValueAt.getNumCrashes(0));
                protoOutputStream.end(jStart16);
                size5--;
                sparseArray10 = sparseArray10;
            }
            SparseArray sparseArray11 = sparseArray10;
            SparseArray<? extends Uid.Sensor> sensorStats = uidValueAt.getSensorStats();
            for (int i20 = 0; i20 < sensorStats.size(); i20++) {
                Uid.Sensor sensorValueAt = sensorStats.valueAt(i20);
                Timer sensorTime = sensorValueAt.getSensorTime();
                if (sensorTime != null) {
                    Timer sensorBackgroundTime = sensorValueAt.getSensorBackgroundTime();
                    int iKeyAt2 = sensorStats.keyAt(i20);
                    long jStart17 = protoOutputStream.start(UidProto.SENSORS);
                    protoOutputStream.write(1120986464257L, iKeyAt2);
                    dumpTimer(protoOutputStream, 1146756268034L, sensorTime, j10, 0);
                    dumpTimer(protoOutputStream, 1146756268035L, sensorBackgroundTime, j10, 0);
                    protoOutputStream.end(jStart17);
                }
            }
            int i21 = 0;
            while (i21 < 7) {
                long j13 = j10;
                long jRoundUsToMs2 = roundUsToMs(uidValueAt.getProcessStateTime(i21, j13, 0));
                if (jRoundUsToMs2 != 0) {
                    long jStart18 = protoOutputStream.start(2246267895828L);
                    protoOutputStream.write(1159641169921L, i21);
                    protoOutputStream.write(1112396529666L, jRoundUsToMs2);
                    protoOutputStream.end(jStart18);
                }
                i21++;
                j10 = j13;
            }
            long j14 = j10;
            ArrayMap<String, ? extends Timer> syncStats = uidValueAt.getSyncStats();
            for (int size6 = syncStats.size() - 1; size6 >= 0; size6--) {
                Timer timerValueAt2 = syncStats.valueAt(size6);
                Timer subTimer3 = timerValueAt2.getSubTimer();
                long jStart19 = protoOutputStream.start(2246267895830L);
                protoOutputStream.write(1138166333441L, syncStats.keyAt(size6));
                dumpTimer(protoOutputStream, 1146756268034L, timerValueAt2, j14, 0);
                dumpTimer(protoOutputStream, 1146756268035L, subTimer3, j14, 0);
                protoOutputStream.end(jStart19);
            }
            if (uidValueAt.hasUserActivity()) {
                for (int i22 = 0; i22 < 4; i22++) {
                    int userActivityCount = uidValueAt.getUserActivityCount(i22, 0);
                    if (userActivityCount != 0) {
                        long jStart20 = protoOutputStream.start(2246267895831L);
                        protoOutputStream.write(1159641169921L, i22);
                        protoOutputStream.write(1120986464258L, userActivityCount);
                        protoOutputStream.end(jStart20);
                    }
                }
            }
            dumpTimer(protoOutputStream, 1146756268045L, uidValueAt.getVibratorOnTimer(), j14, 0);
            dumpTimer(protoOutputStream, 1146756268046L, uidValueAt.getVideoTurnedOnTimer(), j14, 0);
            ArrayMap<String, ? extends Uid.Wakelock> wakelockStats = uidValueAt.getWakelockStats();
            int size7 = wakelockStats.size() - 1;
            while (size7 >= 0) {
                Uid.Wakelock wakelockValueAt = wakelockStats.valueAt(size7);
                long jStart21 = protoOutputStream.start(2246267895833L);
                protoOutputStream.write(1138166333441L, wakelockStats.keyAt(size7));
                dumpTimer(protoOutputStream, 1146756268034L, wakelockValueAt.getWakeTime(1), j14, 0);
                Timer wakeTime = wakelockValueAt.getWakeTime(0);
                if (wakeTime != null) {
                    arrayMap = wakelockStats;
                    dumpTimer(protoOutputStream, 1146756268035L, wakeTime, j14, 0);
                    dumpTimer(protoOutputStream, 1146756268036L, wakeTime.getSubTimer(), j14, 0);
                } else {
                    arrayMap = wakelockStats;
                }
                dumpTimer(protoOutputStream, 1146756268037L, wakelockValueAt.getWakeTime(2), j14, 0);
                protoOutputStream.end(jStart21);
                size7--;
                wakelockStats = arrayMap;
            }
            dumpTimer(protoOutputStream, 1146756268060L, uidValueAt.getMulticastWakelockStats(), j14, 0);
            for (int size8 = arrayMap5.size() - 1; size8 >= 0; size8--) {
                ArrayMap<String, ? extends Counter> wakeupAlarmStats = arrayMap5.valueAt(size8).getWakeupAlarmStats();
                for (int size9 = wakeupAlarmStats.size() - 1; size9 >= 0; size9--) {
                    long jStart22 = protoOutputStream.start(2246267895834L);
                    protoOutputStream.write(1138166333441L, wakeupAlarmStats.keyAt(size9));
                    protoOutputStream.write(1120986464258L, wakeupAlarmStats.valueAt(size9).getCountLocked(0));
                    protoOutputStream.end(jStart22);
                }
            }
            dumpControllerActivityProto(protoOutputStream, 1146756268037L, uidValueAt.getWifiControllerActivity(), 0);
            long jStart23 = protoOutputStream.start(1146756268059L);
            protoOutputStream.write(1112396529665L, roundUsToMs(uidValueAt.getFullWifiLockTime(j14, 0)));
            dumpTimer(protoOutputStream, 1146756268035L, uidValueAt.getWifiScanTimer(), j14, 0);
            protoOutputStream.write(1112396529666L, roundUsToMs(uidValueAt.getWifiRunningTime(j14, 0)));
            dumpTimer(protoOutputStream, 1146756268036L, uidValueAt.getWifiScanBackgroundTimer(), j14, 0);
            protoOutputStream.end(jStart23);
            protoOutputStream.end(jStart);
            i5 = i12 + 1;
            j6 = j14;
            size = i6;
            uidStats = sparseArray6;
            batteryUptime = j11;
            sparseArray3 = sparseArray7;
            jElapsedRealtime = j12;
            sparseArray4 = sparseArray11;
        }
    }

    private void dumpProtoHistoryLocked(ProtoOutputStream protoOutputStream, int i, long j) {
        long j2;
        HistoryPrinter historyPrinter;
        boolean z;
        if (!startIteratingHistoryLocked()) {
            return;
        }
        protoOutputStream.write(1120986464257L, 32);
        protoOutputStream.write(1112396529666L, getParcelVersion());
        protoOutputStream.write(1138166333443L, getStartPlatformVersion());
        protoOutputStream.write(1138166333444L, getEndPlatformVersion());
        for (int i2 = 0; i2 < getHistoryStringPoolSize(); i2++) {
            try {
                long jStart = protoOutputStream.start(2246267895813L);
                protoOutputStream.write(1120986464257L, i2);
                protoOutputStream.write(1120986464258L, getHistoryTagPoolUid(i2));
                protoOutputStream.write(1138166333443L, getHistoryTagPoolString(i2));
                protoOutputStream.end(jStart);
            } finally {
                finishIteratingHistoryLocked();
            }
        }
        HistoryPrinter historyPrinter2 = new HistoryPrinter();
        HistoryItem historyItem = new HistoryItem();
        long j3 = -1;
        long j4 = -1;
        boolean z2 = false;
        while (getNextHistoryLocked(historyItem)) {
            j4 = historyItem.time;
            long j5 = j3 < 0 ? j4 : j3;
            if (historyItem.time >= j) {
                if (j >= 0 && !z2) {
                    if (historyItem.cmd == 5 || historyItem.cmd == 7 || historyItem.cmd == 4 || historyItem.cmd == 8) {
                        j2 = j4;
                        historyPrinter = historyPrinter2;
                        historyPrinter.printNextItem(protoOutputStream, historyItem, j5, (i & 32) != 0);
                        z = false;
                        historyItem.cmd = (byte) 0;
                        z2 = true;
                    } else {
                        historyPrinter = historyPrinter2;
                        if (historyItem.currentTime != 0) {
                            byte b = historyItem.cmd;
                            historyItem.cmd = (byte) 5;
                            j2 = j4;
                            historyPrinter.printNextItem(protoOutputStream, historyItem, j5, (i & 32) != 0);
                            historyItem.cmd = b;
                            z2 = true;
                        } else {
                            j2 = j4;
                        }
                        z = false;
                    }
                } else {
                    j2 = j4;
                    historyPrinter = historyPrinter2;
                    z = false;
                }
                boolean z3 = z2;
                historyPrinter.printNextItem(protoOutputStream, historyItem, j5, (i & 32) != 0 ? true : z);
                z2 = z3;
                j4 = j2;
                j3 = j5;
                historyPrinter2 = historyPrinter;
            } else {
                j3 = j5;
            }
        }
        if (j >= 0) {
            commitCurrentHistoryBatchLocked();
            protoOutputStream.write(2237677961222L, "NEXT: " + (j4 + 1));
        }
    }

    private void dumpProtoSystemLocked(ProtoOutputStream protoOutputStream, BatteryStatsHelper batteryStatsHelper) {
        boolean z;
        int i;
        int i2;
        int uid;
        long j;
        int i3;
        long jStart = protoOutputStream.start(1146756268038L);
        long jUptimeMillis = SystemClock.uptimeMillis() * 1000;
        long jElapsedRealtime = SystemClock.elapsedRealtime() * 1000;
        long jStart2 = protoOutputStream.start(1146756268033L);
        protoOutputStream.write(1112396529665L, getStartClockTime());
        protoOutputStream.write(1112396529666L, getStartCount());
        protoOutputStream.write(1112396529667L, computeRealtime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529668L, computeUptime(jUptimeMillis, 0) / 1000);
        protoOutputStream.write(1112396529669L, computeBatteryRealtime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529670L, computeBatteryUptime(jUptimeMillis, 0) / 1000);
        protoOutputStream.write(1112396529671L, computeBatteryScreenOffRealtime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529672L, computeBatteryScreenOffUptime(jUptimeMillis, 0) / 1000);
        protoOutputStream.write(1112396529673L, getScreenDozeTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529674L, getEstimatedBatteryCapacity());
        protoOutputStream.write(1112396529675L, getMinLearnedBatteryCapacity());
        protoOutputStream.write(1112396529676L, getMaxLearnedBatteryCapacity());
        protoOutputStream.end(jStart2);
        long jStart3 = protoOutputStream.start(1146756268034L);
        protoOutputStream.write(1120986464257L, getLowDischargeAmountSinceCharge());
        protoOutputStream.write(1120986464258L, getHighDischargeAmountSinceCharge());
        protoOutputStream.write(1120986464259L, getDischargeAmountScreenOnSinceCharge());
        protoOutputStream.write(1120986464260L, getDischargeAmountScreenOffSinceCharge());
        protoOutputStream.write(1120986464261L, getDischargeAmountScreenDozeSinceCharge());
        protoOutputStream.write(1112396529670L, getUahDischarge(0) / 1000);
        protoOutputStream.write(1112396529671L, getUahDischargeScreenOff(0) / 1000);
        protoOutputStream.write(1112396529672L, getUahDischargeScreenDoze(0) / 1000);
        protoOutputStream.write(1112396529673L, getUahDischargeLightDoze(0) / 1000);
        protoOutputStream.write(1112396529674L, getUahDischargeDeepDoze(0) / 1000);
        protoOutputStream.end(jStart3);
        long jComputeChargeTimeRemaining = computeChargeTimeRemaining(jElapsedRealtime);
        if (jComputeChargeTimeRemaining >= 0) {
            protoOutputStream.write(1112396529667L, jComputeChargeTimeRemaining / 1000);
        } else {
            long jComputeBatteryTimeRemaining = computeBatteryTimeRemaining(jElapsedRealtime);
            if (jComputeBatteryTimeRemaining >= 0) {
                protoOutputStream.write(1112396529668L, jComputeBatteryTimeRemaining / 1000);
            } else {
                protoOutputStream.write(1112396529668L, -1);
            }
        }
        dumpDurationSteps(protoOutputStream, 2246267895813L, getChargeLevelStepTracker());
        int i4 = 0;
        while (true) {
            if (i4 >= 21) {
                break;
            }
            z = i4 == 0;
            int i5 = i4 == 20 ? 0 : i4;
            long jStart4 = protoOutputStream.start(2246267895816L);
            if (z) {
                protoOutputStream.write(1133871366146L, z);
            } else {
                protoOutputStream.write(1159641169921L, i5);
            }
            dumpTimer(protoOutputStream, 1146756268035L, getPhoneDataConnectionTimer(i4), jElapsedRealtime, 0);
            protoOutputStream.end(jStart4);
            i4++;
        }
        dumpDurationSteps(protoOutputStream, 2246267895814L, getDischargeLevelStepTracker());
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            for (long j2 : cpuFreqs) {
                protoOutputStream.write(SystemProto.CPU_FREQUENCY, j2);
            }
        }
        dumpControllerActivityProto(protoOutputStream, 1146756268041L, getBluetoothControllerActivity(), 0);
        dumpControllerActivityProto(protoOutputStream, 1146756268042L, getModemControllerActivity(), 0);
        long jStart5 = protoOutputStream.start(1146756268044L);
        protoOutputStream.write(1112396529665L, getNetworkActivityBytes(0, 0));
        protoOutputStream.write(1112396529666L, getNetworkActivityBytes(1, 0));
        protoOutputStream.write(1112396529669L, getNetworkActivityPackets(0, 0));
        protoOutputStream.write(1112396529670L, getNetworkActivityPackets(1, 0));
        protoOutputStream.write(1112396529667L, getNetworkActivityBytes(2, 0));
        protoOutputStream.write(1112396529668L, getNetworkActivityBytes(3, 0));
        protoOutputStream.write(1112396529671L, getNetworkActivityPackets(2, 0));
        protoOutputStream.write(1112396529672L, getNetworkActivityPackets(3, 0));
        protoOutputStream.write(1112396529673L, getNetworkActivityBytes(4, 0));
        protoOutputStream.write(1112396529674L, getNetworkActivityBytes(5, 0));
        protoOutputStream.end(jStart5);
        dumpControllerActivityProto(protoOutputStream, 1146756268043L, getWifiControllerActivity(), 0);
        long jStart6 = protoOutputStream.start(1146756268045L);
        protoOutputStream.write(1112396529665L, getWifiOnTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529666L, getGlobalWifiRunningTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.end(jStart6);
        for (Map.Entry<String, ? extends Timer> entry : getKernelWakelockStats().entrySet()) {
            long jStart7 = protoOutputStream.start(2246267895822L);
            protoOutputStream.write(1138166333441L, entry.getKey());
            dumpTimer(protoOutputStream, 1146756268034L, entry.getValue(), jElapsedRealtime, 0);
            protoOutputStream.end(jStart7);
            z = z;
        }
        boolean z2 = z;
        SparseArray<? extends Uid> uidStats = getUidStats();
        long totalTimeLocked = 0;
        long totalTimeLocked2 = 0;
        for (int i6 = 0; i6 < uidStats.size(); i6++) {
            ArrayMap<String, ? extends Uid.Wakelock> wakelockStats = uidStats.valueAt(i6).getWakelockStats();
            for (int size = wakelockStats.size() - (z2 ? 1 : 0); size >= 0; size--) {
                Uid.Wakelock wakelockValueAt = wakelockStats.valueAt(size);
                Timer wakeTime = wakelockValueAt.getWakeTime(z2 ? 1 : 0);
                if (wakeTime != null) {
                    i3 = 0;
                    totalTimeLocked2 += wakeTime.getTotalTimeLocked(jElapsedRealtime, 0);
                } else {
                    i3 = 0;
                }
                Timer wakeTime2 = wakelockValueAt.getWakeTime(i3);
                if (wakeTime2 != null) {
                    totalTimeLocked += wakeTime2.getTotalTimeLocked(jElapsedRealtime, i3);
                }
            }
        }
        long jStart8 = protoOutputStream.start(1146756268047L);
        protoOutputStream.write(1112396529665L, getScreenOnTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529666L, getPhoneOnTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529667L, totalTimeLocked2 / 1000);
        protoOutputStream.write(1112396529668L, totalTimeLocked / 1000);
        protoOutputStream.write(1112396529669L, getMobileRadioActiveTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529670L, getMobileRadioActiveAdjustedTime(0) / 1000);
        protoOutputStream.write(1120986464263L, getMobileRadioActiveCount(0));
        protoOutputStream.write(1120986464264L, getMobileRadioActiveUnknownTime(0) / 1000);
        protoOutputStream.write(1112396529673L, getInteractiveTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1112396529674L, getPowerSaveModeEnabledTime(jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1120986464267L, getNumConnectivityChange(0));
        protoOutputStream.write(1112396529676L, getDeviceIdleModeTime(2, jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1120986464269L, getDeviceIdleModeCount(2, 0));
        protoOutputStream.write(1112396529678L, getDeviceIdlingTime(2, jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1120986464271L, getDeviceIdlingCount(2, 0));
        protoOutputStream.write(1112396529680L, getLongestDeviceIdleModeTime(2));
        protoOutputStream.write(1112396529681L, getDeviceIdleModeTime(z2 ? 1 : 0, jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1120986464274L, getDeviceIdleModeCount(z2 ? 1 : 0, 0));
        protoOutputStream.write(1112396529683L, getDeviceIdlingTime(z2 ? 1 : 0, jElapsedRealtime, 0) / 1000);
        protoOutputStream.write(1120986464276L, getDeviceIdlingCount(z2 ? 1 : 0, 0));
        protoOutputStream.write(1112396529685L, getLongestDeviceIdleModeTime(z2 ? 1 : 0));
        protoOutputStream.end(jStart8);
        long wifiMulticastWakelockTime = getWifiMulticastWakelockTime(jElapsedRealtime, 0);
        int wifiMulticastWakelockCount = getWifiMulticastWakelockCount(0);
        long jStart9 = protoOutputStream.start(1146756268055L);
        protoOutputStream.write(1112396529665L, wifiMulticastWakelockTime / 1000);
        protoOutputStream.write(1120986464258L, wifiMulticastWakelockCount);
        protoOutputStream.end(jStart9);
        List<BatterySipper> usageList = batteryStatsHelper.getUsageList();
        if (usageList != null) {
            int i7 = 0;
            while (i7 < usageList.size()) {
                BatterySipper batterySipper = usageList.get(i7);
                switch (batterySipper.drainType) {
                    case AMBIENT_DISPLAY:
                        i = 0;
                        i2 = 13;
                        uid = i;
                        long jStart10 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart10);
                        break;
                    case IDLE:
                        uid = 0;
                        i2 = z2 ? 1 : 0;
                        long jStart102 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart102);
                        break;
                    case CELL:
                        uid = 0;
                        i2 = 2;
                        long jStart1022 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart1022);
                        break;
                    case PHONE:
                        i = 0;
                        i2 = 3;
                        uid = i;
                        long jStart10222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart10222);
                        break;
                    case WIFI:
                        i = 0;
                        i2 = 4;
                        uid = i;
                        long jStart102222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart102222);
                        break;
                    case BLUETOOTH:
                        uid = 0;
                        i2 = 5;
                        long jStart1022222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart1022222);
                        break;
                    case SCREEN:
                        i = 0;
                        i2 = 7;
                        uid = i;
                        long jStart10222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart10222222);
                        break;
                    case FLASHLIGHT:
                        i = 0;
                        i2 = 6;
                        uid = i;
                        long jStart102222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart102222222);
                        break;
                    case APP:
                        j = jElapsedRealtime;
                        break;
                    case USER:
                        i2 = 8;
                        uid = UserHandle.getUid(batterySipper.userId, 0);
                        long jStart1022222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart1022222222);
                        break;
                    case UNACCOUNTED:
                        i2 = 9;
                        uid = 0;
                        long jStart10222222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart10222222222);
                        break;
                    case OVERCOUNTED:
                        i2 = 10;
                        uid = 0;
                        long jStart102222222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart102222222222);
                        break;
                    case CAMERA:
                        i2 = 11;
                        uid = 0;
                        long jStart1022222222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart1022222222222);
                        break;
                    case MEMORY:
                        i2 = 12;
                        uid = 0;
                        long jStart10222222222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart10222222222222);
                        break;
                    default:
                        uid = 0;
                        i2 = 0;
                        long jStart102222222222222 = protoOutputStream.start(SystemProto.POWER_USE_ITEM);
                        j = jElapsedRealtime;
                        protoOutputStream.write(1159641169921L, i2);
                        protoOutputStream.write(1120986464258L, uid);
                        protoOutputStream.write(1103806595075L, batterySipper.totalPowerMah);
                        protoOutputStream.write(1133871366148L, batterySipper.shouldHide);
                        protoOutputStream.write(SystemProto.PowerUseItem.SCREEN_POWER_MAH, batterySipper.screenPowerMah);
                        protoOutputStream.write(SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, batterySipper.proportionalSmearMah);
                        protoOutputStream.end(jStart102222222222222);
                        break;
                }
                i7++;
                jElapsedRealtime = j;
            }
        }
        long j3 = jElapsedRealtime;
        long jStart11 = protoOutputStream.start(1146756268050L);
        protoOutputStream.write(1103806595073L, batteryStatsHelper.getPowerProfile().getBatteryCapacity());
        protoOutputStream.write(SystemProto.PowerUseSummary.COMPUTED_POWER_MAH, batteryStatsHelper.getComputedPower());
        protoOutputStream.write(1103806595075L, batteryStatsHelper.getMinDrainedPower());
        protoOutputStream.write(1103806595076L, batteryStatsHelper.getMaxDrainedPower());
        protoOutputStream.end(jStart11);
        Map<String, ? extends Timer> rpmStats = getRpmStats();
        Map<String, ? extends Timer> screenOffRpmStats = getScreenOffRpmStats();
        for (Map.Entry<String, ? extends Timer> entry2 : rpmStats.entrySet()) {
            long jStart12 = protoOutputStream.start(2246267895827L);
            protoOutputStream.write(1138166333441L, entry2.getKey());
            dumpTimer(protoOutputStream, 1146756268034L, entry2.getValue(), j3, 0);
            dumpTimer(protoOutputStream, 1146756268035L, screenOffRpmStats.get(entry2.getKey()), j3, 0);
            protoOutputStream.end(jStart12);
        }
        for (int i8 = 0; i8 < 5; i8++) {
            long jStart13 = protoOutputStream.start(2246267895828L);
            protoOutputStream.write(1159641169921L, i8);
            dumpTimer(protoOutputStream, 1146756268034L, getScreenBrightnessTimer(i8), j3, 0);
            protoOutputStream.end(jStart13);
        }
        dumpTimer(protoOutputStream, 1146756268053L, getPhoneSignalScanningTimer(), j3, 0);
        for (int i9 = 0; i9 < 5; i9++) {
            long jStart14 = protoOutputStream.start(2246267895824L);
            protoOutputStream.write(1159641169921L, i9);
            dumpTimer(protoOutputStream, 1146756268034L, getPhoneSignalStrengthTimer(i9), j3, 0);
            protoOutputStream.end(jStart14);
        }
        for (Map.Entry<String, ? extends Timer> entry3 : getWakeupReasonStats().entrySet()) {
            long jStart15 = protoOutputStream.start(2246267895830L);
            protoOutputStream.write(1138166333441L, entry3.getKey());
            dumpTimer(protoOutputStream, 1146756268034L, entry3.getValue(), j3, 0);
            protoOutputStream.end(jStart15);
        }
        for (int i10 = 0; i10 < 5; i10++) {
            long jStart16 = protoOutputStream.start(2246267895832L);
            protoOutputStream.write(1159641169921L, i10);
            dumpTimer(protoOutputStream, 1146756268034L, getWifiSignalStrengthTimer(i10), j3, 0);
            protoOutputStream.end(jStart16);
        }
        for (int i11 = 0; i11 < 8; i11++) {
            long jStart17 = protoOutputStream.start(2246267895833L);
            protoOutputStream.write(1159641169921L, i11);
            dumpTimer(protoOutputStream, 1146756268034L, getWifiStateTimer(i11), j3, 0);
            protoOutputStream.end(jStart17);
        }
        for (int i12 = 0; i12 < 13; i12++) {
            long jStart18 = protoOutputStream.start(2246267895834L);
            protoOutputStream.write(1159641169921L, i12);
            dumpTimer(protoOutputStream, 1146756268034L, getWifiSupplStateTimer(i12), j3, 0);
            protoOutputStream.end(jStart18);
        }
        protoOutputStream.end(jStart);
    }
}
