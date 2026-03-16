package com.android.server.wifi;

import android.net.wifi.BatchedScanSettings;
import android.net.wifi.RttManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiScanner;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WifiNative {
    static final int BLUETOOTH_COEXISTENCE_MODE_DISABLED = 1;
    static final int BLUETOOTH_COEXISTENCE_MODE_ENABLED = 0;
    static final int BLUETOOTH_COEXISTENCE_MODE_SENSE = 2;
    private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
    static final int SCAN_WITHOUT_CONNECTION_SETUP = 1;
    static final int SCAN_WITH_CONNECTION_SETUP = 2;
    private static final String TAG = "WifiNative-HAL";
    private static int WIFI_SCAN_BUFFER_FULL;
    private static int WIFI_SCAN_COMPLETE;
    private static final LocalLog mLocalLog;
    private static int sCmdId;
    private static boolean sHalFailed;
    private static boolean sHalIsStarted;
    private static int sHotlistCmdId;
    private static HotlistEventHandler sHotlistEventHandler;
    private static int sP2p0Index;
    private static int sRttCmdId;
    private static RttEventHandler sRttEventHandler;
    private static int sScanCmdId;
    private static ScanEventHandler sScanEventHandler;
    private static ScanSettings sScanSettings;
    private static int sSignificantWifiChangeCmdId;
    private static SignificantWifiChangeEventHandler sSignificantWifiChangeHandler;
    private static long sWifiHalHandle;
    private static long[] sWifiIfaceHandles;
    private static int sWlan0Index;
    public final String mInterfaceName;
    public final String mInterfacePrefix;
    private boolean mSuspendOptEnabled = false;
    private final String mTAG;
    private static boolean DBG = false;
    static final Object mLock = new Object();

    public static class BucketSettings {
        int band;
        int bucket;
        ChannelSettings[] channels;
        int num_channels;
        int period_ms;
        int report_events;
    }

    public static class ChannelSettings {
        int dwell_time_ms;
        int frequency;
        boolean passive;
    }

    public interface HotlistEventHandler {
        void onHotlistApFound(ScanResult[] scanResultArr);
    }

    public interface RttEventHandler {
        void onRttResults(RttManager.RttResult[] rttResultArr);
    }

    public static class ScanCapabilities {
        public int max_ap_cache_per_scan;
        public int max_hotlist_aps;
        public int max_rssi_sample_size;
        public int max_scan_buckets;
        public int max_scan_cache_size;
        public int max_scan_reporting_threshold;
        public int max_significant_wifi_change_aps;
    }

    public interface ScanEventHandler {
        void onFullScanResult(ScanResult scanResult);

        void onScanPaused();

        void onScanRestarted();

        void onScanResultsAvailable();

        void onSingleScanComplete();
    }

    public static class ScanSettings {
        int base_period_ms;
        BucketSettings[] buckets;
        int max_ap_per_scan;
        int num_buckets;
        int report_threshold;
    }

    public interface SignificantWifiChangeEventHandler {
        void onChangesFound(ScanResult[] scanResultArr);
    }

    private static native boolean cancelRangeRequestNative(int i, int i2, RttManager.RttParams[] rttParamsArr);

    private native void closeSupplicantConnectionNative();

    private native boolean connectToSupplicantNative();

    private native boolean doBooleanCommandNative(String str);

    private native boolean doBooleanCommandNative_priv(String str, byte[] bArr);

    private native int doIntCommandNative(String str);

    private native String doStringCommandNative(String str);

    private static native int[] getChannelsForBandNative(int i, int i2);

    private static native String getInterfaceNameNative(int i);

    private static native int getInterfacesNative();

    private static native boolean getScanCapabilitiesNative(int i, ScanCapabilities scanCapabilities);

    private static native ScanResult[] getScanResultsNative(int i, boolean z);

    public static native int getSupportedFeatureSetNative(int i);

    private static native WifiLinkLayerStats getWifiLinkLayerStatsNative(int i);

    public static native boolean isDriverLoaded();

    public static native boolean killSupplicant(int i);

    public static native boolean loadDriver();

    private static native int registerNatives();

    private static native boolean requestRangeNative(int i, int i2, RttManager.RttParams[] rttParamsArr);

    private static native boolean resetHotlistNative(int i, int i2);

    private static native boolean setHotlistNative(int i, int i2, WifiScanner.HotlistSettings hotlistSettings);

    private static native boolean setScanningMacOuiNative(int i, byte[] bArr);

    private static native boolean startHalNative();

    private static native boolean startScanNative(int i, int i2, ScanSettings scanSettings);

    public static native boolean startSupplicant(int i);

    private static native void stopHalNative();

    private static native boolean stopScanNative(int i, int i2);

    private static native boolean trackSignificantWifiChangeNative(int i, int i2, WifiScanner.WifiChangeSettings wifiChangeSettings);

    public static native boolean unloadDriver();

    private static native boolean untrackSignificantWifiChangeNative(int i, int i2);

    private native String waitForEventNative();

    private static native void waitForHalEventNative();

    public native boolean setSupplicantArg(String str);

    public native boolean setWifiDrvArg(String str);

    public native void setWifiHalDbg(int i);

    static {
        System.loadLibrary("wifi-service");
        registerNatives();
        mLocalLog = new LocalLog(1024);
        sWifiHalHandle = 0L;
        sWifiIfaceHandles = null;
        sWlan0Index = -1;
        sP2p0Index = -1;
        sHalIsStarted = false;
        sHalFailed = false;
        WIFI_SCAN_BUFFER_FULL = 0;
        WIFI_SCAN_COMPLETE = 1;
        sScanCmdId = 0;
        sHotlistCmdId = 0;
    }

    public WifiNative(String interfaceName) {
        this.mInterfaceName = interfaceName;
        this.mTAG = "WifiNative-" + interfaceName;
        if (!interfaceName.equals("p2p0")) {
            this.mInterfacePrefix = "IFNAME=" + interfaceName + " ";
        } else {
            this.mInterfacePrefix = "";
        }
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
        } else {
            DBG = false;
        }
    }

    public LocalLog getLocalLog() {
        return mLocalLog;
    }

    private static int getNewCmdIdLocked() {
        int i = sCmdId;
        sCmdId = i + 1;
        return i;
    }

    private void localLog(String s) {
        if (mLocalLog != null) {
            mLocalLog.log(this.mInterfaceName + ": " + s);
        }
    }

    public boolean connectToSupplicant() {
        boolean zConnectToSupplicantNative;
        synchronized (mLock) {
            localLog(this.mInterfacePrefix + "connectToSupplicant");
            zConnectToSupplicantNative = connectToSupplicantNative();
        }
        return zConnectToSupplicantNative;
    }

    public void closeSupplicantConnection() {
        synchronized (mLock) {
            localLog(this.mInterfacePrefix + "closeSupplicantConnection");
            closeSupplicantConnectionNative();
        }
    }

    public String waitForEvent() {
        return waitForEventNative();
    }

    private boolean doBooleanCommand(String command) {
        boolean result;
        if (DBG) {
            Log.d(this.mTAG, "doBoolean: " + command);
        }
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + this.mInterfacePrefix + command;
            result = doBooleanCommandNative(this.mInterfacePrefix + command);
            localLog(toLog + " -> " + result);
            if (DBG) {
                Log.d(this.mTAG, command + ": returned " + result);
            }
        }
        return result;
    }

    private boolean doBooleanCommand(String command, byte[] value) {
        boolean result;
        if (DBG) {
            Log.d(this.mTAG, "doBoolean: " + command);
        }
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            if (DBG) {
                localLog(cmdId + "->" + this.mInterfacePrefix + command);
            }
            result = doBooleanCommandNative_priv(this.mInterfacePrefix + command, value);
            if (DBG) {
                localLog(cmdId + "<-" + result);
            }
            if (DBG) {
                Log.d(this.mTAG, "   returned " + result);
            }
        }
        return result;
    }

    private int doIntCommand(String command) {
        int result;
        if (DBG) {
            Log.d(this.mTAG, "doInt: " + command);
        }
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + this.mInterfacePrefix + command;
            result = doIntCommandNative(this.mInterfacePrefix + command);
            localLog(toLog + " -> " + result);
            if (DBG) {
                Log.d(this.mTAG, "   returned " + result);
            }
        }
        return result;
    }

    private String doStringCommand(String command) {
        String result;
        if (DBG && !command.startsWith("GET_NETWORK")) {
            Log.d(this.mTAG, "doString: [" + command + "]");
        }
        synchronized (mLock) {
            int cmdId = getNewCmdIdLocked();
            String toLog = Integer.toString(cmdId) + ":" + this.mInterfacePrefix + command;
            result = doStringCommandNative(this.mInterfacePrefix + command);
            if (result == null) {
                if (DBG) {
                    Log.d(this.mTAG, "doStringCommandNative no result");
                }
            } else {
                if (!command.startsWith("STATUS-")) {
                    localLog(toLog + " -> " + result);
                }
                if (DBG) {
                    Log.d(this.mTAG, "   returned " + result.replace("\n", " "));
                }
            }
        }
        return result;
    }

    private String doStringCommandWithoutLogging(String command) {
        String strDoStringCommandNative;
        if (DBG && !command.startsWith("GET_NETWORK")) {
            Log.d(this.mTAG, "doString: [" + command + "]");
        }
        synchronized (mLock) {
            strDoStringCommandNative = doStringCommandNative(this.mInterfacePrefix + command);
        }
        return strDoStringCommandNative;
    }

    public boolean ping() {
        String pong = doStringCommand("PING");
        return pong != null && pong.equals("PONG");
    }

    public void setSupplicantLogLevel(String level) {
        doStringCommand("LOG_LEVEL " + level);
    }

    public String getFreqCapability() {
        return doStringCommand("GET_CAPABILITY freq");
    }

    public boolean scan(int type, String freqList) {
        if (type == 1) {
            return freqList == null ? doBooleanCommand("SCAN TYPE=ONLY") : doBooleanCommand("SCAN TYPE=ONLY freq=" + freqList);
        }
        if (type == 2) {
            return freqList == null ? doBooleanCommand("SCAN") : doBooleanCommand("SCAN freq=" + freqList);
        }
        throw new IllegalArgumentException("Invalid scan type");
    }

    public boolean stopSupplicant() {
        return doBooleanCommand("TERMINATE");
    }

    public String listNetworks() {
        return doStringCommand("LIST_NETWORKS");
    }

    public String listNetworks(int last_id) {
        return doStringCommand("LIST_NETWORKS LAST_ID=" + last_id);
    }

    public int addNetwork() {
        return doIntCommand("ADD_NETWORK");
    }

    public boolean setNetworkVariable(int netId, String name, String value) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) {
            return false;
        }
        return doBooleanCommand("SET_NETWORK " + netId + " " + name + " " + value);
    }

    public boolean setNetworkVariable(int netId, String name, byte[] ssid_value) {
        if (TextUtils.isEmpty(name) || ssid_value.length == 0) {
            return false;
        }
        return doBooleanCommand("SET_NETWORK " + netId + " " + name + " ", ssid_value);
    }

    public String getNetworkVariable(int netId, String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        return doStringCommandWithoutLogging("GET_NETWORK " + netId + " " + name);
    }

    public boolean removeNetwork(int netId) {
        return doBooleanCommand("REMOVE_NETWORK " + netId);
    }

    private void logDbg(String debug) {
        long now = SystemClock.elapsedRealtimeNanos();
        String ts = String.format("[%,d us] ", Long.valueOf(now / 1000));
        Log.e("WifiNative: ", ts + debug + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName() + " - " + Thread.currentThread().getStackTrace()[6].getMethodName());
    }

    public boolean enableNetwork(int netId, boolean disableOthers) {
        if (DBG) {
            logDbg("enableNetwork nid=" + Integer.toString(netId) + " disableOthers=" + disableOthers);
        }
        return disableOthers ? doBooleanCommand("SELECT_NETWORK " + netId) : doBooleanCommand("ENABLE_NETWORK " + netId);
    }

    public boolean disableNetwork(int netId) {
        if (DBG) {
            logDbg("disableNetwork nid=" + Integer.toString(netId));
        }
        return doBooleanCommand("DISABLE_NETWORK " + netId);
    }

    public boolean reconnect() {
        if (DBG) {
            logDbg("RECONNECT ");
        }
        return doBooleanCommand("RECONNECT");
    }

    public boolean reassociate() {
        if (DBG) {
            logDbg("REASSOCIATE ");
        }
        return doBooleanCommand("REASSOCIATE");
    }

    public boolean disconnect() {
        if (DBG) {
            logDbg("DISCONNECT ");
        }
        return doBooleanCommand("DISCONNECT");
    }

    public String status() {
        return status(false);
    }

    public String status(boolean noEvents) {
        return noEvents ? doStringCommand("STATUS-NO_EVENTS") : doStringCommand("STATUS");
    }

    public String getMacAddress() {
        String ret = doStringCommand("DRIVER MACADDR");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" = ");
            if (tokens.length == 2) {
                return tokens[1];
            }
        }
        return null;
    }

    public String scanResults(int sid) {
        return doStringCommandWithoutLogging("BSS RANGE=" + sid + "- MASK=0x21987");
    }

    public String scanResult(String bssid) {
        return doStringCommand("BSS " + bssid);
    }

    public String setBatchedScanSettings(BatchedScanSettings settings) {
        if (settings == null) {
            return doStringCommand("DRIVER WLS_BATCHING STOP");
        }
        String cmd = ("DRIVER WLS_BATCHING SET SCANFREQ=" + settings.scanIntervalSec) + " MSCAN=" + settings.maxScansPerBatch;
        if (settings.maxApPerScan != Integer.MAX_VALUE) {
            cmd = cmd + " BESTN=" + settings.maxApPerScan;
        }
        if (settings.channelSet != null && !settings.channelSet.isEmpty()) {
            String cmd2 = cmd + " CHANNEL=<";
            int i = 0;
            for (String channel : settings.channelSet) {
                cmd2 = cmd2 + (i > 0 ? "," : "") + channel;
                i++;
            }
            cmd = cmd2 + ">";
        }
        if (settings.maxApForDistance != Integer.MAX_VALUE) {
            cmd = cmd + " RTT=" + settings.maxApForDistance;
        }
        return doStringCommand(cmd);
    }

    public String getBatchedScanResults() {
        return doStringCommand("DRIVER WLS_BATCHING GET");
    }

    public boolean startDriver() {
        return doBooleanCommand("DRIVER START");
    }

    public boolean stopDriver() {
        return doBooleanCommand("DRIVER STOP");
    }

    public boolean startFilteringMulticastV4Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-REMOVE 2") && doBooleanCommand("DRIVER RXFILTER-START");
    }

    public boolean stopFilteringMulticastV4Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-ADD 2") && doBooleanCommand("DRIVER RXFILTER-START");
    }

    public boolean startFilteringMulticastV6Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-REMOVE 3") && doBooleanCommand("DRIVER RXFILTER-START");
    }

    public boolean stopFilteringMulticastV6Packets() {
        return doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-ADD 3") && doBooleanCommand("DRIVER RXFILTER-START");
    }

    public int getBand() {
        String ret = doStringCommand("DRIVER GETBAND");
        if (TextUtils.isEmpty(ret)) {
            return -1;
        }
        String[] tokens = ret.split(" ");
        try {
            if (tokens.length == 2) {
                return Integer.parseInt(tokens[1]);
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean setBand(int band) {
        return doBooleanCommand("DRIVER SETBAND " + band);
    }

    public boolean setBluetoothCoexistenceMode(int mode) {
        return doBooleanCommand("DRIVER BTCOEXMODE " + mode);
    }

    public boolean setBluetoothCoexistenceScanMode(boolean setCoexScanMode) {
        return setCoexScanMode ? doBooleanCommand("DRIVER BTCOEXSCAN-START") : doBooleanCommand("DRIVER BTCOEXSCAN-STOP");
    }

    public void enableSaveConfig() {
        doBooleanCommand("SET update_config 1");
    }

    public boolean saveConfig() {
        return doBooleanCommand("SAVE_CONFIG");
    }

    public boolean addToBlacklist(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return false;
        }
        return doBooleanCommand("BLACKLIST " + bssid);
    }

    public boolean clearBlacklist() {
        return doBooleanCommand("BLACKLIST clear");
    }

    public boolean setSuspendOptimizations(boolean enabled) {
        this.mSuspendOptEnabled = enabled;
        Log.e("native", "do suspend " + enabled);
        return enabled ? doBooleanCommand("DRIVER SETSUSPENDMODE 1") : doBooleanCommand("DRIVER SETSUSPENDMODE 0");
    }

    public boolean setSleepPeriodCommand(int sleepPeriod) {
        return doBooleanCommand("DRIVER SLEEPPD " + sleepPeriod);
    }

    public boolean setCountryCode(String countryCode) {
        return doBooleanCommand("DRIVER COUNTRY " + countryCode.toUpperCase(Locale.ROOT));
    }

    public void enableBackgroundScan(boolean enable) {
        if (enable) {
            doBooleanCommand("SET pno 1");
        } else {
            doBooleanCommand("SET pno 0");
        }
    }

    public void enableAutoConnect(boolean enable) {
        if (enable) {
            doBooleanCommand("STA_AUTOCONNECT 1");
        } else {
            doBooleanCommand("STA_AUTOCONNECT 0");
        }
    }

    public void setScanInterval(int scanInterval) {
        doBooleanCommand("SCAN_INTERVAL " + scanInterval);
    }

    public void startTdls(String macAddr, boolean enable) {
        if (enable) {
            doBooleanCommand("TDLS_DISCOVER " + macAddr);
            doBooleanCommand("TDLS_SETUP " + macAddr);
        } else {
            doBooleanCommand("TDLS_TEARDOWN " + macAddr);
        }
    }

    public String signalPoll() {
        return doStringCommandWithoutLogging("SIGNAL_POLL");
    }

    public String pktcntPoll() {
        return doStringCommand("PKTCNT_POLL");
    }

    public void bssFlush() {
        doBooleanCommand("BSS_FLUSH 0");
    }

    public boolean startWpsPbc(String bssid) {
        return TextUtils.isEmpty(bssid) ? doBooleanCommand("WPS_PBC") : doBooleanCommand("WPS_PBC " + bssid);
    }

    public boolean startWpsPbc(String iface, String bssid) {
        boolean zDoBooleanCommandNative;
        synchronized (mLock) {
            zDoBooleanCommandNative = TextUtils.isEmpty(bssid) ? doBooleanCommandNative("IFNAME=" + iface + " WPS_PBC") : doBooleanCommandNative("IFNAME=" + iface + " WPS_PBC " + bssid);
        }
        return zDoBooleanCommandNative;
    }

    public boolean startWpsPinKeypad(String pin) {
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        return doBooleanCommand("WPS_PIN any " + pin);
    }

    public boolean startWpsPinKeypad(String iface, String pin) {
        boolean zDoBooleanCommandNative;
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        synchronized (mLock) {
            zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " WPS_PIN any " + pin);
        }
        return zDoBooleanCommandNative;
    }

    public String startWpsPinDisplay(String bssid) {
        return TextUtils.isEmpty(bssid) ? doStringCommand("WPS_PIN any") : doStringCommand("WPS_PIN " + bssid);
    }

    public String startWpsPinDisplay(String iface, String bssid) {
        String strDoStringCommandNative;
        synchronized (mLock) {
            strDoStringCommandNative = TextUtils.isEmpty(bssid) ? doStringCommandNative("IFNAME=" + iface + " WPS_PIN any") : doStringCommandNative("IFNAME=" + iface + " WPS_PIN " + bssid);
        }
        return strDoStringCommandNative;
    }

    public boolean setExternalSim(boolean external) {
        boolean zDoBooleanCommand;
        synchronized (mLock) {
            String value = external ? "1" : "0";
            Log.d(TAG, "Setting external_sim to " + value);
            zDoBooleanCommand = doBooleanCommand("SET external_sim " + value);
        }
        return zDoBooleanCommand;
    }

    public boolean simAuthResponse(int id, String response) {
        boolean zDoBooleanCommand;
        synchronized (mLock) {
            zDoBooleanCommand = doBooleanCommand("CTRL-RSP-SIM-" + id + response);
        }
        return zDoBooleanCommand;
    }

    public boolean simIdentityResponse(int id, String response) {
        boolean zDoBooleanCommand;
        synchronized (mLock) {
            zDoBooleanCommand = doBooleanCommand("CTRL-RSP-IDENTITY-" + id + ":" + response);
        }
        return zDoBooleanCommand;
    }

    public boolean startWpsRegistrar(String bssid, String pin) {
        if (TextUtils.isEmpty(bssid) || TextUtils.isEmpty(pin)) {
            return false;
        }
        return doBooleanCommand("WPS_REG " + bssid + " " + pin);
    }

    public boolean cancelWps() {
        return doBooleanCommand("WPS_CANCEL");
    }

    public boolean setPersistentReconnect(boolean enabled) {
        int value = !enabled ? 0 : 1;
        return doBooleanCommand("SET persistent_reconnect " + value);
    }

    public boolean setDeviceName(String name) {
        return doBooleanCommand("SET device_name " + name);
    }

    public boolean setDeviceType(String type) {
        return doBooleanCommand("SET device_type " + type);
    }

    public boolean setConfigMethods(String cfg) {
        return doBooleanCommand("SET config_methods " + cfg);
    }

    public boolean setManufacturer(String value) {
        return doBooleanCommand("SET manufacturer " + value);
    }

    public boolean setModelName(String value) {
        return doBooleanCommand("SET model_name " + value);
    }

    public boolean setModelNumber(String value) {
        return doBooleanCommand("SET model_number " + value);
    }

    public boolean setSerialNumber(String value) {
        return doBooleanCommand("SET serial_number " + value);
    }

    public boolean setP2pSsidPostfix(String postfix) {
        return doBooleanCommand("SET p2p_ssid_postfix " + postfix);
    }

    public boolean setP2pGroupIdle(String iface, int time) {
        boolean zDoBooleanCommandNative;
        synchronized (mLock) {
            zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " SET p2p_group_idle " + time);
        }
        return zDoBooleanCommandNative;
    }

    private boolean powerSaveDisabled() {
        return "1".equals(SystemProperties.get("persist.radio.wifi.powersave"));
    }

    public void setPowerSave(boolean enabled) {
        if (powerSaveDisabled()) {
            doBooleanCommand("SET ps 0");
        } else if (enabled) {
            doBooleanCommand("SET ps 1");
        } else {
            doBooleanCommand("SET ps 0");
        }
    }

    public boolean setP2pPowerSave(String iface, boolean enabled) {
        boolean zDoBooleanCommandNative;
        if (powerSaveDisabled()) {
            return doBooleanCommand("P2P_SET interface=" + iface + " ps 0");
        }
        synchronized (mLock) {
            if (enabled) {
                zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " P2P_SET ps 1");
            } else {
                zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " P2P_SET ps 0");
            }
        }
        return zDoBooleanCommandNative;
    }

    public boolean setWfdEnable(boolean enable) {
        return doBooleanCommand("SET wifi_display " + (enable ? "1" : "0"));
    }

    public boolean setWfdDeviceInfo(String hex) {
        return doBooleanCommand("WFD_SUBELEM_SET 0 " + hex);
    }

    public boolean setConcurrencyPriority(String s) {
        return doBooleanCommand("P2P_SET conc_pref " + s);
    }

    public boolean p2pFind() {
        return doBooleanCommand("P2P_FIND");
    }

    public boolean p2pFind(int timeout) {
        return timeout <= 0 ? p2pFind() : doBooleanCommand("P2P_FIND " + timeout);
    }

    public boolean p2pStopFind() {
        return doBooleanCommand("P2P_STOP_FIND");
    }

    public boolean p2pListen() {
        return doBooleanCommand("P2P_LISTEN");
    }

    public boolean p2pListen(int timeout) {
        return timeout <= 0 ? p2pListen() : doBooleanCommand("P2P_LISTEN " + timeout);
    }

    public boolean p2pExtListen(boolean enable, int period, int interval) {
        if (!enable || interval >= period) {
            return doBooleanCommand("P2P_EXT_LISTEN" + (enable ? " " + period + " " + interval : ""));
        }
        return false;
    }

    public boolean p2pSetChannel(int lc, int oc) {
        if (DBG) {
            Log.d(this.mTAG, "p2pSetChannel: lc=" + lc + ", oc=" + oc);
        }
        if (lc >= 1 && lc <= 11) {
            if (!doBooleanCommand("P2P_SET listen_channel " + lc)) {
                return false;
            }
        } else if (lc != 0) {
            return false;
        }
        if (oc >= 1 && oc <= 165) {
            int freq = (oc <= 14 ? 2407 : 5000) + (oc * 5);
            return doBooleanCommand("P2P_SET disallow_freq 1000-" + (freq - 5) + "," + (freq + 5) + "-6000");
        }
        if (oc == 0) {
            return doBooleanCommand("P2P_SET disallow_freq \"\"");
        }
        return false;
    }

    public boolean p2pFlush() {
        return doBooleanCommand("P2P_FLUSH");
    }

    public String p2pConnect(WifiP2pConfig config, boolean joinExistingGroup) {
        if (config == null) {
            return null;
        }
        List<String> args = new ArrayList<>();
        WpsInfo wps = config.wps;
        args.add(config.deviceAddress);
        switch (wps.setup) {
            case 0:
                args.add("pbc");
                break;
            case 1:
                if (TextUtils.isEmpty(wps.pin)) {
                    args.add("pin");
                } else {
                    args.add(wps.pin);
                }
                args.add("display");
                break;
            case 2:
                args.add(wps.pin);
                args.add("keypad");
                break;
            case 3:
                args.add(wps.pin);
                args.add("label");
                break;
        }
        if (config.netId == -2) {
            args.add("persistent");
        }
        if (joinExistingGroup) {
            args.add("join");
        } else {
            int groupOwnerIntent = config.groupOwnerIntent;
            if (groupOwnerIntent < 0 || groupOwnerIntent > 15) {
                groupOwnerIntent = 6;
            }
            args.add("go_intent=" + groupOwnerIntent);
        }
        String command = "P2P_CONNECT ";
        for (String s : args) {
            command = command + s + " ";
        }
        return doStringCommand(command);
    }

    public boolean p2pCancelConnect() {
        return doBooleanCommand("P2P_CANCEL");
    }

    public boolean p2pProvisionDiscovery(WifiP2pConfig config) {
        if (config == null) {
            return false;
        }
        switch (config.wps.setup) {
            case 0:
                return doBooleanCommand("P2P_PROV_DISC " + config.deviceAddress + " pbc");
            case 1:
                return doBooleanCommand("P2P_PROV_DISC " + config.deviceAddress + " keypad");
            case 2:
                return doBooleanCommand("P2P_PROV_DISC " + config.deviceAddress + " display");
            default:
                return false;
        }
    }

    public boolean p2pGroupAdd(boolean persistent) {
        return persistent ? doBooleanCommand("P2P_GROUP_ADD persistent") : doBooleanCommand("P2P_GROUP_ADD");
    }

    public boolean p2pGroupAdd(int netId) {
        return doBooleanCommand("P2P_GROUP_ADD persistent=" + netId);
    }

    public boolean p2pGroupRemove(String iface) {
        boolean zDoBooleanCommandNative;
        if (TextUtils.isEmpty(iface)) {
            return false;
        }
        synchronized (mLock) {
            zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " P2P_GROUP_REMOVE " + iface);
        }
        return zDoBooleanCommandNative;
    }

    public boolean p2pReject(String deviceAddress) {
        return doBooleanCommand("P2P_REJECT " + deviceAddress);
    }

    public boolean p2pInvite(WifiP2pGroup group, String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress)) {
            return false;
        }
        if (group == null) {
            return doBooleanCommand("P2P_INVITE peer=" + deviceAddress);
        }
        return doBooleanCommand("P2P_INVITE group=" + group.getInterface() + " peer=" + deviceAddress + " go_dev_addr=" + group.getOwner().deviceAddress);
    }

    public boolean p2pReinvoke(int netId, String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress) || netId < 0) {
            return false;
        }
        return doBooleanCommand("P2P_INVITE persistent=" + netId + " peer=" + deviceAddress);
    }

    public String p2pGetSsid(String deviceAddress) {
        return p2pGetParam(deviceAddress, "oper_ssid");
    }

    public String p2pGetDeviceAddress() {
        String status;
        Log.d(TAG, "p2pGetDeviceAddress");
        synchronized (mLock) {
            status = doStringCommandNative("STATUS");
        }
        String result = "";
        if (status != null) {
            String[] tokens = status.split("\n");
            for (String token : tokens) {
                if (token.startsWith("p2p_device_address=")) {
                    String[] nameValue = token.split("=");
                    if (nameValue.length != 2) {
                        break;
                    }
                    result = nameValue[1];
                }
            }
        }
        Log.d(TAG, "p2pGetDeviceAddress returning " + result);
        return result;
    }

    public int getGroupCapability(String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress)) {
            return 0;
        }
        String peerInfo = p2pPeer(deviceAddress);
        if (TextUtils.isEmpty(peerInfo)) {
            return 0;
        }
        String[] tokens = peerInfo.split("\n");
        for (String token : tokens) {
            if (token.startsWith("group_capab=")) {
                String[] nameValue = token.split("=");
                if (nameValue.length != 2) {
                    return 0;
                }
                try {
                    int gc = Integer.decode(nameValue[1]).intValue();
                    return gc;
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public String p2pPeer(String deviceAddress) {
        return doStringCommand("P2P_PEER " + deviceAddress);
    }

    private String p2pGetParam(String deviceAddress, String key) {
        String peerInfo;
        if (deviceAddress == null || (peerInfo = p2pPeer(deviceAddress)) == null) {
            return null;
        }
        String[] tokens = peerInfo.split("\n");
        String key2 = key + "=";
        for (String token : tokens) {
            if (token.startsWith(key2)) {
                String[] nameValue = token.split("=");
                if (nameValue.length == 2) {
                    return nameValue[1];
                }
                return null;
            }
        }
        return null;
    }

    public boolean p2pServiceAdd(WifiP2pServiceInfo servInfo) {
        for (String s : servInfo.getSupplicantQueryList()) {
            String command = "P2P_SERVICE_ADD " + s;
            if (!doBooleanCommand(command)) {
                return false;
            }
        }
        return true;
    }

    public boolean p2pServiceDel(WifiP2pServiceInfo servInfo) {
        String command;
        for (String s : servInfo.getSupplicantQueryList()) {
            String[] data = s.split(" ");
            if (data.length < 2) {
                return false;
            }
            if ("upnp".equals(data[0])) {
                command = "P2P_SERVICE_DEL " + s;
            } else {
                if (!"bonjour".equals(data[0])) {
                    return false;
                }
                String command2 = "P2P_SERVICE_DEL " + data[0];
                command = command2 + " " + data[1];
            }
            if (!doBooleanCommand(command)) {
                return false;
            }
        }
        return true;
    }

    public boolean p2pServiceFlush() {
        return doBooleanCommand("P2P_SERVICE_FLUSH");
    }

    public String p2pServDiscReq(String addr, String query) {
        String command = "P2P_SERV_DISC_REQ " + addr;
        return doStringCommand(command + " " + query);
    }

    public boolean p2pServDiscCancelReq(String id) {
        return doBooleanCommand("P2P_SERV_DISC_CANCEL_REQ " + id);
    }

    public void setMiracastMode(int mode) {
        doBooleanCommand("DRIVER MIRACAST " + mode);
    }

    public boolean setActiveRoamingCommand(boolean enabled) {
        return doBooleanCommand("DRIVER SETROAMING " + (enabled ? 1 : 0));
    }

    public boolean fetchAnqp(String bssid, String subtypes) {
        return doBooleanCommand("ANQP_GET " + bssid + " " + subtypes);
    }

    private static class MonitorThread extends Thread {
        private MonitorThread() {
        }

        @Override
        public void run() {
            Log.i(WifiNative.TAG, "Waiting for HAL events mWifiHalHandle=" + Long.toString(WifiNative.sWifiHalHandle));
            WifiNative.waitForHalEventNative();
        }
    }

    public static synchronized boolean startHal() {
        boolean z = true;
        synchronized (WifiNative.class) {
            Log.i(TAG, "startHal");
            synchronized (mLock) {
                if (!sHalIsStarted) {
                    if (sHalFailed) {
                        z = false;
                    } else if (startHalNative() && getInterfaces() != 0 && sWlan0Index != -1) {
                        new MonitorThread().start();
                        sHalIsStarted = true;
                    } else {
                        Log.i(TAG, "Could not start hal");
                        sHalIsStarted = false;
                        sHalFailed = true;
                        z = false;
                    }
                }
            }
        }
        return z;
    }

    public static synchronized void stopHal() {
        stopHalNative();
    }

    public static synchronized int getInterfaces() {
        int wifi_num;
        synchronized (mLock) {
            if (sWifiIfaceHandles == null) {
                int num = getInterfacesNative();
                wifi_num = 0;
                for (int i = 0; i < num; i++) {
                    String name = getInterfaceNameNative(i);
                    Log.i(TAG, "interface[" + i + "] = " + name);
                    if (name.equals("wlan0")) {
                        sWlan0Index = i;
                        wifi_num++;
                    } else if (name.equals("p2p0")) {
                        sP2p0Index = i;
                        wifi_num++;
                    }
                }
            } else {
                wifi_num = sWifiIfaceHandles.length;
            }
        }
        return wifi_num;
    }

    public static synchronized String getInterfaceName(int index) {
        return getInterfaceNameNative(index);
    }

    public static boolean getScanCapabilities(ScanCapabilities capabilities) {
        return getScanCapabilitiesNative(sWlan0Index, capabilities);
    }

    static synchronized void onScanResultsAvailable(int id) {
        if (sScanEventHandler != null) {
            sScanEventHandler.onScanResultsAvailable();
        }
    }

    static synchronized void onScanStatus(int status) {
        Log.i(TAG, "Got a scan status changed event, status = " + status);
        if (status != WIFI_SCAN_BUFFER_FULL && status == WIFI_SCAN_COMPLETE && sScanEventHandler != null) {
            sScanEventHandler.onSingleScanComplete();
        }
    }

    static synchronized void onFullScanResult(int id, ScanResult result, byte[] bytes) {
        if (DBG) {
            Log.i(TAG, "Got a full scan results event, ssid = " + result.SSID + ", num = " + bytes.length);
        }
        if (sScanEventHandler != null) {
            int num = 0;
            int i = 0;
            while (true) {
                if (i >= bytes.length) {
                    break;
                }
                int type = bytes[i] & 255;
                int len = bytes[i + 1] & 255;
                if (i + len + 2 > bytes.length) {
                    Log.w(TAG, "bad length " + len + " of IE " + type + " from " + result.BSSID);
                    Log.w(TAG, "ignoring the rest of the IEs");
                    break;
                } else {
                    num++;
                    i += len + 2;
                    if (DBG) {
                        Log.i(TAG, "bytes[" + i + "] = [" + type + ", " + len + "], next = " + i);
                    }
                }
            }
            ScanResult.InformationElement[] elements = new ScanResult.InformationElement[num];
            int index = 0;
            for (int i2 = 0; i2 < num; i2++) {
                int type2 = bytes[index] & 255;
                int len2 = bytes[index + 1] & 255;
                if (DBG) {
                    Log.i(TAG, "index = " + index + ", type = " + type2 + ", len = " + len2);
                }
                ScanResult.InformationElement elem = new ScanResult.InformationElement();
                elem.id = type2;
                elem.bytes = new byte[len2];
                for (int j = 0; j < len2; j++) {
                    elem.bytes[j] = bytes[index + j + 2];
                }
                elements[i2] = elem;
                index += len2 + 2;
            }
            result.informationElements = elements;
            sScanEventHandler.onFullScanResult(result);
        }
    }

    public static synchronized boolean startScan(ScanSettings settings, ScanEventHandler eventHandler) {
        boolean z;
        synchronized (mLock) {
            if (sScanCmdId != 0) {
                stopScan();
            } else if (sScanSettings != null || sScanEventHandler != null) {
            }
            sScanCmdId = getNewCmdIdLocked();
            sScanSettings = settings;
            sScanEventHandler = eventHandler;
            if (!startScanNative(sWlan0Index, sScanCmdId, settings)) {
                sScanEventHandler = null;
                sScanSettings = null;
                z = false;
            } else {
                z = true;
            }
        }
        return z;
    }

    public static synchronized void stopScan() {
        synchronized (mLock) {
            stopScanNative(sWlan0Index, sScanCmdId);
            sScanSettings = null;
            sScanEventHandler = null;
            sScanCmdId = 0;
        }
    }

    public static synchronized void pauseScan() {
        synchronized (mLock) {
            if (sScanCmdId != 0 && sScanSettings != null && sScanEventHandler != null) {
                Log.d(TAG, "Pausing scan");
                stopScanNative(sWlan0Index, sScanCmdId);
                sScanCmdId = 0;
                sScanEventHandler.onScanPaused();
            }
        }
    }

    public static synchronized void restartScan() {
        synchronized (mLock) {
            if (sScanCmdId == 0 && sScanSettings != null && sScanEventHandler != null) {
                Log.d(TAG, "Restarting scan");
                startScan(sScanSettings, sScanEventHandler);
                sScanEventHandler.onScanRestarted();
            }
        }
    }

    public static synchronized ScanResult[] getScanResults() {
        ScanResult[] scanResultsNative;
        synchronized (mLock) {
            scanResultsNative = getScanResultsNative(sWlan0Index, false);
        }
        return scanResultsNative;
    }

    public static synchronized boolean setHotlist(WifiScanner.HotlistSettings settings, HotlistEventHandler eventHandler) {
        boolean z = false;
        synchronized (WifiNative.class) {
            synchronized (mLock) {
                if (sHotlistCmdId == 0) {
                    sHotlistCmdId = getNewCmdIdLocked();
                    sHotlistEventHandler = eventHandler;
                    if (!setHotlistNative(sWlan0Index, sScanCmdId, settings)) {
                        sHotlistEventHandler = null;
                    } else {
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    public static synchronized void resetHotlist() {
        synchronized (mLock) {
            if (sHotlistCmdId != 0) {
                resetHotlistNative(sWlan0Index, sHotlistCmdId);
                sHotlistCmdId = 0;
                sHotlistEventHandler = null;
            }
        }
    }

    public static synchronized void onHotlistApFound(int id, ScanResult[] results) {
        synchronized (mLock) {
            if (sHotlistCmdId != 0) {
                sHotlistEventHandler.onHotlistApFound(results);
            } else {
                Log.d(TAG, "Ignoring hotlist AP found change");
            }
        }
    }

    public static synchronized boolean trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings, SignificantWifiChangeEventHandler handler) {
        boolean z = false;
        synchronized (WifiNative.class) {
            synchronized (mLock) {
                if (sSignificantWifiChangeCmdId == 0) {
                    sSignificantWifiChangeCmdId = getNewCmdIdLocked();
                    sSignificantWifiChangeHandler = handler;
                    if (!trackSignificantWifiChangeNative(sWlan0Index, sScanCmdId, settings)) {
                        sSignificantWifiChangeHandler = null;
                    } else {
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    static synchronized void untrackSignificantWifiChange() {
        synchronized (mLock) {
            if (sSignificantWifiChangeCmdId != 0) {
                untrackSignificantWifiChangeNative(sWlan0Index, sSignificantWifiChangeCmdId);
                sSignificantWifiChangeCmdId = 0;
                sSignificantWifiChangeHandler = null;
            }
        }
    }

    static synchronized void onSignificantWifiChange(int id, ScanResult[] results) {
        synchronized (mLock) {
            if (sSignificantWifiChangeCmdId != 0) {
                sSignificantWifiChangeHandler.onChangesFound(results);
            } else {
                Log.d(TAG, "Ignoring significant wifi change");
            }
        }
    }

    public static synchronized WifiLinkLayerStats getWifiLinkLayerStats(String iface) {
        WifiLinkLayerStats wifiLinkLayerStatsNative = null;
        synchronized (WifiNative.class) {
            if (iface != null) {
                synchronized (mLock) {
                    if (!sHalIsStarted) {
                        startHal();
                    }
                    if (sHalIsStarted) {
                        wifiLinkLayerStatsNative = getWifiLinkLayerStatsNative(sWlan0Index);
                    }
                }
            }
        }
        return wifiLinkLayerStatsNative;
    }

    public String getNfcWpsConfigurationToken(int netId) {
        return doStringCommand("WPS_NFC_CONFIG_TOKEN WPS " + netId);
    }

    public String getNfcHandoverRequest() {
        return doStringCommand("NFC_GET_HANDOVER_REQ NDEF P2P-CR");
    }

    public String getNfcHandoverSelect() {
        return doStringCommand("NFC_GET_HANDOVER_SEL NDEF P2P-CR");
    }

    public boolean initiatorReportNfcHandover(String selectMessage) {
        return doBooleanCommand("NFC_REPORT_HANDOVER INIT P2P 00 " + selectMessage);
    }

    public boolean responderReportNfcHandover(String requestMessage) {
        return doBooleanCommand("NFC_REPORT_HANDOVER RESP P2P " + requestMessage + " 00");
    }

    public static synchronized int getSupportedFeatureSet() {
        return getSupportedFeatureSetNative(sWlan0Index);
    }

    private static synchronized void onRttResults(int id, RttManager.RttResult[] results) {
        if (id == sRttCmdId) {
            Log.d(TAG, "Received " + results.length + " rtt results");
            sRttEventHandler.onRttResults(results);
            sRttCmdId = 0;
        } else {
            Log.d(TAG, "Received event for unknown cmd = " + id + ", current id = " + sRttCmdId);
        }
    }

    public static synchronized boolean requestRtt(RttManager.RttParams[] params, RttEventHandler handler) {
        boolean zRequestRangeNative;
        synchronized (mLock) {
            if (sRttCmdId != 0) {
                zRequestRangeNative = false;
            } else {
                sRttCmdId = getNewCmdIdLocked();
                sRttEventHandler = handler;
                zRequestRangeNative = requestRangeNative(sWlan0Index, sRttCmdId, params);
            }
        }
        return zRequestRangeNative;
    }

    public static synchronized boolean cancelRtt(RttManager.RttParams[] params) {
        boolean z = false;
        synchronized (WifiNative.class) {
            synchronized (mLock) {
                if (sRttCmdId != 0 && cancelRangeRequestNative(sWlan0Index, sRttCmdId, params)) {
                    sRttEventHandler = null;
                    z = true;
                }
            }
        }
        return z;
    }

    public static synchronized boolean setScanningMacOui(byte[] oui) {
        boolean scanningMacOuiNative;
        synchronized (mLock) {
            scanningMacOuiNative = startHal() ? setScanningMacOuiNative(sWlan0Index, oui) : false;
        }
        return scanningMacOuiNative;
    }

    public static synchronized int[] getChannelsForBand(int band) {
        int[] channelsForBandNative;
        synchronized (mLock) {
            channelsForBandNative = startHal() ? getChannelsForBandNative(sWlan0Index, band) : null;
        }
        return channelsForBandNative;
    }

    public boolean setSupplicantLogLevel(int logLevel, int timeStamp) {
        String[] levelString = {"EXCESSIVE", "MSGDUMP", "DEBUG", "INFO", "WARNING", "ERROR"};
        if (logLevel < 0 || logLevel > 5) {
            return false;
        }
        if (timeStamp == 1 || timeStamp == 0) {
            return doBooleanCommand("LOG_LEVEL " + levelString[logLevel] + " " + timeStamp);
        }
        return false;
    }
}
