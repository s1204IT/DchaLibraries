package com.android.server.wifi;

import android.content.Context;
import android.net.apf.ApfCapabilities;
import android.net.wifi.RttManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiSsid;
import android.net.wifi.WifiWakeReasonAndCounts;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.HexDump;
import com.android.server.connectivity.KeepalivePacketData;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.SupplicantBridge;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.omadm.MOTree;
import com.android.server.wifi.util.FrameParser;
import com.android.server.wifi.util.InformationElementUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import libcore.util.HexEncoding;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiNative {
    public static final int BLUETOOTH_COEXISTENCE_MODE_DISABLED = 1;
    public static final int BLUETOOTH_COEXISTENCE_MODE_ENABLED = 0;
    public static final int BLUETOOTH_COEXISTENCE_MODE_SENSE = 2;
    private static final String BSS_BSSID_STR = "bssid=";
    private static final String BSS_DELIMITER_STR = "====";
    private static final String BSS_END_STR = "####";
    private static final String BSS_FLAGS_STR = "flags=";
    private static final String BSS_FREQ_STR = "freq=";
    private static final String BSS_ID_STR = "id=";
    private static final String BSS_IE_STR = "ie=";
    private static final String BSS_LEVEL_STR = "level=";
    private static final String BSS_SSID_STR = "ssid=";
    private static final String BSS_TSF_STR = "tsf=";
    private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
    private static final int STOP_HAL_TIMEOUT_MS = 1000;
    private static final String TAG = "WifiNative-HAL";
    public static final int WIFI_SCAN_FAILED = 3;
    public static final int WIFI_SCAN_RESULTS_AVAILABLE = 0;
    public static final int WIFI_SCAN_THRESHOLD_NUM_SCANS = 1;
    public static final int WIFI_SCAN_THRESHOLD_PERCENT = 2;
    public static final int WIFI_SUCCESS = 0;
    private static byte[] mFwMemoryDump;
    private static WifiNative p2pNativeInterface;
    private static int sCmdId;
    private static int sHotlistCmdId;
    private static HotlistEventHandler sHotlistEventHandler;
    private static int sLogCmdId;
    private static int sPnoCmdId;
    private static PnoEventHandler sPnoEventHandler;
    private static int sRssiMonitorCmdId;
    private static int sRttCmdId;
    private static RttEventHandler sRttEventHandler;
    private static int sRttResponderCmdId;
    private static int sScanCmdId;
    private static ScanEventHandler sScanEventHandler;
    private static ScanSettings sScanSettings;
    private static int sSignificantWifiChangeCmdId;
    private static SignificantWifiChangeEventHandler sSignificantWifiChangeHandler;
    private static TdlsEventHandler sTdlsEventHandler;
    private static MonitorThread sThread;
    private static long sWifiHalHandle;
    private static long[] sWifiIfaceHandles;
    private static WifiLoggerEventHandler sWifiLoggerEventHandler;
    private static WifiRssiEventHandler sWifiRssiEventHandler;
    public static int sWlan0Index;
    private static WifiNative wlanNativeInterface;
    private Context mContext = null;
    private boolean mDisconnectCalled = false;
    private final String mInterfaceName;
    private final String mInterfacePrefix;
    private final String mTAG;
    private static boolean DBG = false;
    public static final Object sLock = new Object();
    private static final LocalLog sLocalLog = new LocalLog(8192);

    public static class BucketSettings {
        public int band;
        public int bucket;
        public ChannelSettings[] channels;
        public int max_period_ms;
        public int num_channels;
        public int period_ms;
        public int report_events;
        public int step_count;
    }

    public static class ChannelSettings {
        public int dwell_time_ms;
        public int frequency;
        public boolean passive;
    }

    public interface HotlistEventHandler {
        void onHotlistApFound(ScanResult[] scanResultArr);

        void onHotlistApLost(ScanResult[] scanResultArr);
    }

    public interface PnoEventHandler {
        void onPnoNetworkFound(ScanResult[] scanResultArr);

        void onPnoScanFailed();
    }

    public static class PnoNetwork {
        public byte auth_bit_field;
        public byte flags;
        public int networkId;
        public int priority;
        public String ssid;
    }

    public static class PnoSettings {
        public int band5GHzBonus;
        public int currentConnectionBonus;
        public int initialScoreMax;
        public boolean isConnected;
        public int min24GHzRssi;
        public int min5GHzRssi;
        public PnoNetwork[] networkList;
        public int sameNetworkBonus;
        public int secureBonus;
    }

    public interface RttEventHandler {
        void onRttResults(RttManager.RttResult[] rttResultArr);
    }

    public static class ScanCapabilities {
        public int max_ap_cache_per_scan;
        public int max_bssid_history_entries;
        public int max_hotlist_bssids;
        public int max_number_epno_networks;
        public int max_number_epno_networks_by_ssid;
        public int max_number_of_white_listed_ssid;
        public int max_rssi_sample_size;
        public int max_scan_buckets;
        public int max_scan_cache_size;
        public int max_scan_reporting_threshold;
        public int max_significant_wifi_change_aps;
    }

    public interface ScanEventHandler {
        void onFullScanResult(ScanResult scanResult, int i);

        void onScanPaused(WifiScanner.ScanData[] scanDataArr);

        void onScanRestarted();

        void onScanStatus(int i);
    }

    public static class ScanSettings {
        public int base_period_ms;
        public BucketSettings[] buckets;
        public int[] hiddenNetworkIds;
        public int max_ap_per_scan;
        public int num_buckets;
        public int report_threshold_num_scans;
        public int report_threshold_percent;
    }

    public interface SignificantWifiChangeEventHandler {
        void onChangesFound(ScanResult[] scanResultArr);
    }

    public static class TdlsCapabilities {
        boolean isGlobalTdlsSupported;
        boolean isOffChannelTdlsSupported;
        boolean isPerMacTdlsSupported;
        int maxConcurrentTdlsSessionNumber;
    }

    public static class TdlsStatus {
        int channel;
        int global_operating_class;
        int reason;
        int state;
    }

    public static class WifiChannelInfo {
        int mCenterFrequency0;
        int mCenterFrequency1;
        int mChannelWidth;
        int mPrimaryFrequency;
    }

    public interface WifiLoggerEventHandler {
        void onRingBufferData(RingBufferStatus ringBufferStatus, byte[] bArr);

        void onWifiAlert(int i, byte[] bArr);
    }

    public interface WifiRssiEventHandler {
        void onRssiThresholdBreached(byte b);
    }

    private static native boolean cancelRangeRequestNative(int i, int i2, RttManager.RttParams[] rttParamsArr);

    private static native void closeSupplicantConnectionNative();

    private static native int configureNeighborDiscoveryOffload(int i, boolean z);

    private static native boolean connectToSupplicantNative();

    private static native boolean disableRttResponderNative(int i, int i2);

    private native boolean doBooleanCommandNative(String str);

    private native int doIntCommandNative(String str);

    private native String doStringCommandNative(String str);

    private static native boolean enableDisableTdlsNative(int i, boolean z, String str);

    private static native RttManager.ResponderConfig enableRttResponderNative(int i, int i2, int i3, WifiChannelInfo wifiChannelInfo);

    private static native ApfCapabilities getApfCapabilitiesNative(int i);

    private static native int[] getChannelsForBandNative(int i, int i2);

    private static native byte[] getDriverStateDumpNative(int i);

    private static native String getDriverVersionNative(int i);

    private static native String getFirmwareVersionNative(int i);

    private static native boolean getFwMemoryDumpNative(int i);

    private static native String getInterfaceNameNative(int i);

    private static native int getInterfacesNative();

    private static native boolean getRingBufferDataNative(int i, String str);

    private static native RingBufferStatus[] getRingBufferStatusNative(int i);

    private static native RttManager.RttCapabilities getRttCapabilitiesNative(int i);

    private static native int getRxPktFatesNative(int i, RxFateReport[] rxFateReportArr);

    private static native boolean getScanCapabilitiesNative(int i, ScanCapabilities scanCapabilities);

    private static native WifiScanner.ScanData[] getScanResultsNative(int i, boolean z);

    public static native int getSupportedFeatureSetNative(int i);

    private static native int getSupportedLoggerFeatureSetNative(int i);

    private static native TdlsCapabilities getTdlsCapabilitiesNative(int i);

    private static native TdlsStatus getTdlsStatusNative(int i, String str);

    private static native int getTxPktFatesNative(int i, TxFateReport[] txFateReportArr);

    private static native WifiLinkLayerStats getWifiLinkLayerStatsNative(int i);

    private static native WifiWakeReasonAndCounts getWlanWakeReasonCountNative(int i);

    private static native boolean installPacketFilterNative(int i, byte[] bArr);

    private static native boolean isDriverLoadedNative();

    private static native boolean isGetChannelsForBandSupportedNative();

    private static native boolean killSupplicantNative(boolean z);

    private static native boolean loadDriverNative();

    private static native byte[] readKernelLogNative();

    private static native int registerNatives();

    private static native boolean requestRangeNative(int i, int i2, RttManager.RttParams[] rttParamsArr);

    private static native boolean resetHotlistNative(int i, int i2);

    private static native boolean resetLogHandlerNative(int i, int i2);

    private static native boolean resetPnoListNative(int i, int i2);

    private static native boolean setBssidBlacklistNative(int i, int i2, String[] strArr);

    private static native boolean setCountryCodeHalNative(int i, String str);

    private static native boolean setDfsFlagNative(int i, boolean z);

    private static native boolean setHotlistNative(int i, int i2, WifiScanner.HotlistSettings hotlistSettings);

    private static native boolean setInterfaceUpNative(boolean z);

    private static native boolean setLoggingEventHandlerNative(int i, int i2);

    private native boolean setNetworkVariableCommand(String str, int i, String str2, String str3);

    private static native boolean setPnoListNative(int i, int i2, PnoSettings pnoSettings);

    private static native boolean setScanningMacOuiNative(int i, byte[] bArr);

    public static native boolean setTxPower(int i);

    public static native boolean setTxPowerEnabled(boolean z);

    private static native void setWifiLinkLayerStatsNative(int i, int i2);

    private static native boolean startHalNative();

    private static native boolean startLoggingRingBufferNative(int i, int i2, int i3, int i4, int i5, String str);

    private static native int startPktFateMonitoringNative(int i);

    private static native int startRssiMonitoringNative(int i, int i2, byte b, byte b2);

    private static native boolean startScanNative(int i, int i2, ScanSettings scanSettings);

    private static native int startSendingOffloadedPacketNative(int i, int i2, byte[] bArr, byte[] bArr2, byte[] bArr3, int i3);

    private static native boolean startSupplicantNative(boolean z);

    private static native void stopHalNative();

    private static native int stopRssiMonitoringNative(int i, int i2);

    private static native boolean stopScanNative(int i, int i2);

    private static native int stopSendingOffloadedPacketNative(int i, int i2);

    private static native boolean trackSignificantWifiChangeNative(int i, int i2, WifiScanner.WifiChangeSettings wifiChangeSettings);

    private static native boolean unloadDriverNative();

    private static native boolean untrackSignificantWifiChangeNative(int i, int i2);

    private static native String waitForEventNative();

    private static native void waitForHalEventNative();

    static {
        System.loadLibrary("wifi-service");
        registerNatives();
        wlanNativeInterface = new WifiNative(SystemProperties.get("wifi.interface", "wlan0"), true);
        p2pNativeInterface = new WifiNative(SystemProperties.get("wifi.direct.interface", "p2p0"), false);
        sCmdId = 1;
        sWifiHalHandle = 0L;
        sWifiIfaceHandles = null;
        sWlan0Index = -1;
        sScanCmdId = 0;
        sHotlistCmdId = 0;
        sRttResponderCmdId = 0;
        sWifiLoggerEventHandler = null;
        sLogCmdId = -1;
        sPnoCmdId = 0;
        sRssiMonitorCmdId = 0;
    }

    public static LocalLog getLocalLog() {
        return sLocalLog;
    }

    public static WifiNative getWlanNativeInterface() {
        return wlanNativeInterface;
    }

    public static WifiNative getP2pNativeInterface() {
        return p2pNativeInterface;
    }

    public void initContext(Context context) {
        if (this.mContext != null || context == null) {
            return;
        }
        this.mContext = context;
    }

    private WifiNative(String interfaceName, boolean requiresPrefix) {
        this.mInterfaceName = interfaceName;
        this.mTAG = "WifiNative-" + interfaceName;
        if (requiresPrefix) {
            this.mInterfacePrefix = "IFNAME=" + interfaceName + " ";
        } else {
            this.mInterfacePrefix = "";
        }
    }

    public String getInterfaceName() {
        return this.mInterfaceName;
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
        } else {
            DBG = false;
        }
    }

    private void localLog(String s) {
        if (sLocalLog != null) {
            sLocalLog.log(this.mInterfaceName + ": " + s);
        }
    }

    public boolean loadDriver() {
        boolean zLoadDriverNative;
        synchronized (sLock) {
            zLoadDriverNative = loadDriverNative();
        }
        return zLoadDriverNative;
    }

    public boolean isDriverLoaded() {
        boolean zIsDriverLoadedNative;
        synchronized (sLock) {
            zIsDriverLoadedNative = isDriverLoadedNative();
        }
        return zIsDriverLoadedNative;
    }

    public boolean unloadDriver() {
        boolean zUnloadDriverNative;
        synchronized (sLock) {
            zUnloadDriverNative = unloadDriverNative();
        }
        return zUnloadDriverNative;
    }

    public boolean startSupplicant(boolean p2pSupported) {
        boolean zStartSupplicantNative;
        synchronized (sLock) {
            zStartSupplicantNative = startSupplicantNative(p2pSupported);
        }
        return zStartSupplicantNative;
    }

    public boolean killSupplicant(boolean p2pSupported) {
        boolean zKillSupplicantNative;
        synchronized (sLock) {
            zKillSupplicantNative = killSupplicantNative(p2pSupported);
        }
        return zKillSupplicantNative;
    }

    public boolean connectToSupplicant() {
        boolean zConnectToSupplicantNative;
        synchronized (sLock) {
            localLog(this.mInterfacePrefix + "connectToSupplicant");
            zConnectToSupplicantNative = connectToSupplicantNative();
        }
        return zConnectToSupplicantNative;
    }

    public void closeSupplicantConnection() {
        synchronized (sLock) {
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
        synchronized (sLock) {
            String toLog = this.mInterfacePrefix + command;
            result = doBooleanCommandNative(this.mInterfacePrefix + command);
            localLog(toLog + " -> " + result);
            if (DBG) {
                Log.d(this.mTAG, command + ": returned " + result);
            }
        }
        return result;
    }

    private boolean doBooleanCommandWithoutLogging(String command) {
        boolean result;
        if (DBG) {
            Log.d(this.mTAG, "doBooleanCommandWithoutLogging: " + command);
        }
        synchronized (sLock) {
            result = doBooleanCommandNative(this.mInterfacePrefix + command);
            if (DBG) {
                Log.d(this.mTAG, command + ": returned " + result);
            }
        }
        return result;
    }

    private int doIntCommand(String command) {
        int result;
        if (DBG) {
            Log.d(this.mTAG, "doInt: " + command);
        }
        synchronized (sLock) {
            String toLog = this.mInterfacePrefix + command;
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
        synchronized (sLock) {
            String toLog = this.mInterfacePrefix + command;
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
        synchronized (sLock) {
            strDoStringCommandNative = doStringCommandNative(this.mInterfacePrefix + command);
        }
        return strDoStringCommandNative;
    }

    public String doCustomSupplicantCommand(String command) {
        return doStringCommand(command);
    }

    public boolean ping() {
        String pong = doStringCommand("PING");
        if (pong != null) {
            return pong.equals("PONG");
        }
        return false;
    }

    public void setSupplicantLogLevel(String level) {
        doStringCommand("LOG_LEVEL " + level);
    }

    public String getFreqCapability() {
        return doStringCommand("GET_CAPABILITY freq");
    }

    private static String createCSVStringFromIntegerSet(Set<Integer> values) {
        StringBuilder list = new StringBuilder();
        boolean first = true;
        for (Integer value : values) {
            if (!first) {
                list.append(",");
            }
            list.append(value);
            first = false;
        }
        return list.toString();
    }

    public boolean scan(Set<Integer> freqs, Set<Integer> hiddenNetworkIds) {
        String freqList = null;
        String hiddenNetworkIdList = null;
        if (freqs != null && freqs.size() != 0) {
            freqList = createCSVStringFromIntegerSet(freqs);
        }
        if (hiddenNetworkIds != null && hiddenNetworkIds.size() != 0) {
            hiddenNetworkIdList = createCSVStringFromIntegerSet(hiddenNetworkIds);
        }
        return scanWithParams(freqList, hiddenNetworkIdList);
    }

    private boolean scanWithParams(String freqList, String hiddenNetworkIdList) {
        StringBuilder scanCommand = new StringBuilder();
        scanCommand.append("SCAN TYPE=ONLY");
        if (freqList != null) {
            scanCommand.append(" freq=").append(freqList);
        }
        if (hiddenNetworkIdList != null) {
            scanCommand.append(" scan_id=").append(hiddenNetworkIdList);
        }
        return doBooleanCommand(scanCommand.toString());
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
        Log.d(this.mTAG, "addNetwork, mInterfaceName = " + this.mInterfaceName);
        if (this.mInterfaceName.equals("p2p0")) {
            return doIntCommand("IFNAME=" + this.mInterfaceName + " ADD_NETWORK");
        }
        return doIntCommand("ADD_NETWORK");
    }

    public boolean setNetworkExtra(int netId, String name, Map<String, String> values) {
        try {
            String encoded = URLEncoder.encode(new JSONObject(values).toString(), "UTF-8");
            return setNetworkVariable(netId, name, "\"" + encoded + "\"");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e.toString());
            return false;
        } catch (NullPointerException e2) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e2.toString());
            return false;
        }
    }

    public boolean setNetworkVariable(int netId, String name, String value) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) {
            return false;
        }
        if (this.mInterfaceName.equals("p2p0")) {
            String prefix = "IFNAME=" + this.mInterfaceName + " ";
            return doBooleanCommand(prefix + "SET_NETWORK " + netId + " " + name + " " + value);
        }
        if (name.equals("psk") || name.equals("password")) {
            return doBooleanCommandWithoutLogging("SET_NETWORK " + netId + " " + name + " " + value);
        }
        return doBooleanCommand("SET_NETWORK " + netId + " " + name + " " + value);
    }

    public Map<String, String> getNetworkExtra(int netId, String name) {
        String wrapped = getNetworkVariable(netId, name);
        if (wrapped == null || !wrapped.startsWith("\"") || !wrapped.endsWith("\"")) {
            return null;
        }
        try {
            String encoded = wrapped.substring(1, wrapped.length() - 1);
            JSONObject json = new JSONObject(URLDecoder.decode(encoded, "UTF-8"));
            Map<String, String> values = new HashMap<>();
            Iterator<?> it = json.keys();
            while (it.hasNext()) {
                String key = it.next();
                Object value = json.get(key);
                if (value instanceof String) {
                    values.put(key, (String) value);
                }
            }
            return values;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to deserialize networkExtra: " + e.toString());
            return null;
        } catch (JSONException e2) {
            return null;
        }
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

    public boolean enableNetwork(int netId) {
        if (DBG) {
            logDbg("enableNetwork nid=" + Integer.toString(netId));
        }
        return doBooleanCommand("ENABLE_NETWORK " + netId);
    }

    public boolean enableNetworkWithoutConnect(int netId) {
        if (DBG) {
            logDbg("enableNetworkWithoutConnect nid=" + Integer.toString(netId));
        }
        return doBooleanCommand("ENABLE_NETWORK " + netId + " no-connect");
    }

    public boolean disableNetwork(int netId) {
        if (DBG) {
            logDbg("disableNetwork nid=" + Integer.toString(netId));
        }
        return doBooleanCommand("DISABLE_NETWORK " + netId);
    }

    public boolean selectNetwork(int netId) {
        if (DBG) {
            logDbg("selectNetwork nid=" + Integer.toString(netId));
        }
        return doBooleanCommand("SELECT_NETWORK " + netId);
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
        if (noEvents) {
            return doStringCommand("STATUS-NO_EVENTS");
        }
        return doStringCommand("STATUS");
    }

    public String getMacAddress() {
        String ret = doStringCommand("DRIVER MACADDR");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" = ");
            if (tokens.length == 2) {
                return tokens[1];
            }
            return null;
        }
        return null;
    }

    private String getRawScanResults(String range) {
        return doStringCommandWithoutLogging("BSS RANGE=" + range + " MASK=0x29d87");
    }

    public ArrayList<ScanDetail> getScanResults() {
        int next_sid = 0;
        ArrayList<ScanDetail> results = new ArrayList<>();
        while (next_sid >= 0) {
            String rawResult = getRawScanResults(next_sid + "-");
            next_sid = -1;
            if (TextUtils.isEmpty(rawResult)) {
                break;
            }
            String[] lines = rawResult.split("\n");
            int bssidStrLen = BSS_BSSID_STR.length();
            int flagLen = BSS_FLAGS_STR.length();
            String bssid = "";
            int level = 0;
            int freq = 0;
            long tsf = 0;
            String flags = "";
            WifiSsid wifiSsid = null;
            String infoElementsStr = null;
            List<String> anqpLines = null;
            for (String line : lines) {
                if (line.startsWith(BSS_ID_STR)) {
                    try {
                        next_sid = Integer.parseInt(line.substring(BSS_ID_STR.length())) + 1;
                    } catch (NumberFormatException e) {
                    }
                } else if (line.startsWith(BSS_BSSID_STR)) {
                    bssid = new String(line.getBytes(), bssidStrLen, line.length() - bssidStrLen);
                } else if (line.startsWith(BSS_FREQ_STR)) {
                    try {
                        freq = Integer.parseInt(line.substring(BSS_FREQ_STR.length()));
                    } catch (NumberFormatException e2) {
                        freq = 0;
                    }
                } else if (line.startsWith(BSS_LEVEL_STR)) {
                    try {
                        level = Integer.parseInt(line.substring(BSS_LEVEL_STR.length()));
                        if (level > 0) {
                            level -= 256;
                        }
                    } catch (NumberFormatException e3) {
                        level = 0;
                    }
                } else if (line.startsWith(BSS_TSF_STR)) {
                    try {
                        tsf = Long.parseLong(line.substring(BSS_TSF_STR.length()));
                    } catch (NumberFormatException e4) {
                        tsf = 0;
                    }
                } else if (line.startsWith(BSS_FLAGS_STR)) {
                    flags = new String(line.getBytes(), flagLen, line.length() - flagLen);
                } else if (line.startsWith(BSS_SSID_STR)) {
                    wifiSsid = WifiSsid.createFromAsciiEncoded(line.substring(BSS_SSID_STR.length()));
                } else if (line.startsWith(BSS_IE_STR)) {
                    infoElementsStr = line;
                } else if (SupplicantBridge.isAnqpAttribute(line)) {
                    if (anqpLines == null) {
                        anqpLines = new ArrayList<>();
                    }
                    anqpLines.add(line);
                } else if (line.startsWith(BSS_DELIMITER_STR) || line.startsWith(BSS_END_STR)) {
                    if (bssid != null) {
                        if (infoElementsStr == null) {
                            throw new IllegalArgumentException("Null information element data");
                        }
                        try {
                            int seperator = infoElementsStr.indexOf(61);
                            if (seperator < 0) {
                                throw new IllegalArgumentException("No element separator");
                            }
                            ScanResult.InformationElement[] infoElements = InformationElementUtil.parseInformationElements(Utils.hexToBytes(infoElementsStr.substring(seperator + 1)));
                            NetworkDetail networkDetail = new NetworkDetail(bssid, infoElements, anqpLines, freq);
                            String xssid = wifiSsid != null ? wifiSsid.toString() : "<unknown ssid>";
                            if (!xssid.equals(networkDetail.getTrimmedSSID())) {
                                Log.d(TAG, String.format("Inconsistent SSID on BSSID '%s': '%s' vs '%s': %s", bssid, xssid, networkDetail.getSSID(), infoElementsStr));
                            }
                            if (networkDetail.hasInterworking() && DBG) {
                                Log.d(TAG, "HSNwk: '" + networkDetail);
                            }
                            ScanDetail scan = new ScanDetail(networkDetail, wifiSsid, bssid, flags, level, freq, tsf, infoElements, anqpLines);
                            results.add(scan);
                        } catch (IllegalArgumentException iae) {
                            Log.d(TAG, "Failed to parse information elements: " + iae);
                        }
                    }
                    bssid = null;
                    level = 0;
                    freq = 0;
                    tsf = 0;
                    flags = "";
                    wifiSsid = null;
                    infoElementsStr = null;
                    anqpLines = null;
                }
            }
        }
        return results;
    }

    public String scanResult(String bssid) {
        return doStringCommand("BSS " + bssid);
    }

    public boolean startDriver() {
        return doBooleanCommand("DRIVER START");
    }

    public boolean stopDriver() {
        return doBooleanCommand("DRIVER STOP");
    }

    public boolean startFilteringMulticastV4Packets() {
        if (doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-REMOVE 2")) {
            return doBooleanCommand("DRIVER RXFILTER-START");
        }
        return false;
    }

    public boolean stopFilteringMulticastV4Packets() {
        if (doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-ADD 2")) {
            return doBooleanCommand("DRIVER RXFILTER-START");
        }
        return false;
    }

    public boolean startFilteringMulticastV6Packets() {
        if (doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-REMOVE 3")) {
            return doBooleanCommand("DRIVER RXFILTER-START");
        }
        return false;
    }

    public boolean stopFilteringMulticastV6Packets() {
        if (doBooleanCommand("DRIVER RXFILTER-STOP") && doBooleanCommand("DRIVER RXFILTER-ADD 3")) {
            return doBooleanCommand("DRIVER RXFILTER-START");
        }
        return false;
    }

    public boolean setBand(int band) {
        String bandstr;
        if (band == 1) {
            bandstr = "5G";
        } else if (band == 2) {
            bandstr = "2G";
        } else {
            bandstr = "AUTO";
        }
        return doBooleanCommand("SET SETBAND " + bandstr);
    }

    public boolean setBluetoothCoexistenceMode(int mode) {
        return doBooleanCommand("DRIVER BTCOEXMODE " + mode);
    }

    public boolean setBluetoothCoexistenceScanMode(boolean setCoexScanMode) {
        if (setCoexScanMode) {
            return doBooleanCommand("DRIVER BTCOEXSCAN-START");
        }
        return doBooleanCommand("DRIVER BTCOEXSCAN-STOP");
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
        if (enabled) {
            return doBooleanCommand("DRIVER SETSUSPENDMODE 1");
        }
        return doBooleanCommand("DRIVER SETSUSPENDMODE 0");
    }

    public boolean setCountryCode(String countryCode) {
        if (countryCode != null) {
            return doBooleanCommand("DRIVER COUNTRY " + countryCode.toUpperCase(Locale.ROOT));
        }
        return doBooleanCommand("DRIVER COUNTRY");
    }

    public boolean setPnoScan(boolean enable) {
        String cmd = enable ? "SET pno 1" : "SET pno 0";
        return doBooleanCommand(cmd);
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

    public void setHs20(boolean hs20) {
        if (hs20) {
            doBooleanCommand("SET HS20 1");
        } else {
            doBooleanCommand("SET HS20 0");
        }
    }

    public void startTdls(String macAddr, boolean enable) {
        if (enable) {
            synchronized (sLock) {
                doBooleanCommand("TDLS_DISCOVER " + macAddr);
                doBooleanCommand("TDLS_SETUP " + macAddr);
            }
            return;
        }
        doBooleanCommand("TDLS_TEARDOWN " + macAddr);
    }

    public String signalPoll() {
        return doStringCommandWithoutLogging("SIGNAL_POLL");
    }

    public String pktcntPoll() {
        return doStringCommand("PKTCNT_POLL");
    }

    public void bssFlush() {
        if (this.mInterfaceName.equals("p2p0")) {
            doBooleanCommand("IFNAME=" + this.mInterfaceName + " BSS_FLUSH 0");
        } else {
            doBooleanCommand("BSS_FLUSH 0");
        }
    }

    public boolean startWpsPbc(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return doBooleanCommand("WPS_PBC");
        }
        return doBooleanCommand("WPS_PBC " + bssid);
    }

    public boolean startWpsPbc(String iface, String bssid) {
        synchronized (sLock) {
            if (TextUtils.isEmpty(bssid)) {
                return doBooleanCommandNative("IFNAME=" + iface + " WPS_PBC");
            }
            return doBooleanCommandNative("IFNAME=" + iface + " WPS_PBC " + bssid);
        }
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
        synchronized (sLock) {
            zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " WPS_PIN any " + pin);
        }
        return zDoBooleanCommandNative;
    }

    public String startWpsPinDisplay(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            return doStringCommand("WPS_PIN any");
        }
        return doStringCommand("WPS_PIN " + bssid);
    }

    public String startWpsPinDisplay(String iface, String bssid) {
        synchronized (sLock) {
            if (TextUtils.isEmpty(bssid)) {
                return doStringCommandNative("IFNAME=" + iface + " WPS_PIN any");
            }
            return doStringCommandNative("IFNAME=" + iface + " WPS_PIN " + bssid);
        }
    }

    public boolean setExternalSim(boolean external) {
        String value = external ? "1" : "0";
        Log.d(TAG, "Setting external_sim to " + value);
        return doBooleanCommand("SET external_sim " + value);
    }

    public boolean simAuthResponse(int id, String type, String response) {
        return doBooleanCommand("CTRL-RSP-SIM-" + id + ":" + type + response);
    }

    public boolean simAuthFailedResponse(int id) {
        return doBooleanCommand("CTRL-RSP-SIM-" + id + ":GSM-FAIL");
    }

    public boolean umtsAuthFailedResponse(int id) {
        return doBooleanCommand("CTRL-RSP-SIM-" + id + ":UMTS-FAIL");
    }

    public boolean simIdentityResponse(int id, String response) {
        return doBooleanCommand("CTRL-RSP-IDENTITY-" + id + ":" + response);
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
        int value = enabled ? 1 : 0;
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
        synchronized (sLock) {
            zDoBooleanCommandNative = doBooleanCommandNative("IFNAME=" + iface + " SET p2p_group_idle " + time);
        }
        return zDoBooleanCommandNative;
    }

    public void setPowerSave(boolean enabled) {
        if (enabled) {
            doBooleanCommand("SET ps 1");
        } else {
            doBooleanCommand("SET ps 0");
        }
    }

    public boolean setP2pPowerSave(String iface, boolean enabled) {
        synchronized (sLock) {
            if (enabled) {
                return doBooleanCommandNative("IFNAME=" + iface + " P2P_SET ps 1");
            }
            return doBooleanCommandNative("IFNAME=" + iface + " P2P_SET ps 0");
        }
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
        return doBooleanCommand("P2P_FIND type=progressive");
    }

    public boolean p2pFind(int timeout) {
        if (timeout <= 0) {
            return p2pFind();
        }
        return doBooleanCommand("P2P_FIND " + timeout + " type=progressive");
    }

    public boolean p2pStopFind() {
        return doBooleanCommand("P2P_STOP_FIND");
    }

    public boolean p2pListen() {
        return doBooleanCommand("P2P_LISTEN");
    }

    public boolean p2pListen(int timeout) {
        if (timeout <= 0) {
            return p2pListen();
        }
        return doBooleanCommand("P2P_LISTEN " + timeout);
    }

    public boolean p2pExtListen(boolean enable, int period, int interval) {
        if (enable && interval < period) {
            return false;
        }
        return doBooleanCommand("P2P_EXT_LISTEN" + (enable ? " " + period + " " + interval : ""));
    }

    public boolean p2pSetChannel(int lc, int oc) {
        if (DBG) {
            Log.d(this.mTAG, "p2pSetChannel: lc=" + lc + ", oc=" + oc);
        }
        synchronized (sLock) {
            if (lc < 1 || lc > 11) {
                if (lc != 0) {
                    return false;
                }
            } else if (!doBooleanCommand("P2P_SET listen_channel " + lc)) {
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
        int preferOperFreq = config.getPreferOperFreq();
        if (-1 != preferOperFreq) {
            args.add(BSS_FREQ_STR + preferOperFreq);
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
        }
        return false;
    }

    public boolean p2pGroupAdd(boolean persistent) {
        if (persistent) {
            return doBooleanCommand("P2P_GROUP_ADD persistent");
        }
        return doBooleanCommand("P2P_GROUP_ADD");
    }

    public boolean p2pGroupAdd(int netId) {
        return doBooleanCommand("P2P_GROUP_ADD persistent=" + netId);
    }

    public boolean p2pGroupRemove(String iface) {
        boolean zDoBooleanCommandNative;
        if (TextUtils.isEmpty(iface)) {
            return false;
        }
        synchronized (sLock) {
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
        return group == null ? doBooleanCommand("P2P_INVITE peer=" + deviceAddress) : doBooleanCommand("P2P_INVITE group=" + group.getInterface() + " peer=" + deviceAddress + " go_dev_addr=" + group.getOwner().deviceAddress);
    }

    public boolean p2pReinvoke(int netId, String deviceAddress) {
        if (TextUtils.isEmpty(deviceAddress) || netId < 0) {
            return false;
        }
        bssFlush();
        return doBooleanCommand("P2P_INVITE persistent=" + netId + " peer=" + deviceAddress);
    }

    public String p2pGetSsid(String deviceAddress) {
        return p2pGetParam(deviceAddress, "oper_ssid");
    }

    public String p2pGetDeviceAddress() {
        String status;
        Log.d(TAG, "p2pGetDeviceAddress");
        synchronized (sLock) {
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
        int i = 0;
        int length = tokens.length;
        while (true) {
            if (i >= length) {
                break;
            }
            String token = tokens[i];
            if (!token.startsWith("group_capab=")) {
                i++;
            } else {
                String[] nameValue = token.split("=");
                if (nameValue.length == 2) {
                    try {
                        return Integer.decode(nameValue[1]).intValue();
                    } catch (NumberFormatException e) {
                        return 0;
                    }
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
        int i = 0;
        int length = tokens.length;
        while (true) {
            if (i >= length) {
                break;
            }
            String token = tokens[i];
            if (!token.startsWith(key2)) {
                i++;
            } else {
                String[] nameValue = token.split("=");
                if (nameValue.length == 2) {
                    return nameValue[1];
                }
            }
        }
        return null;
    }

    public boolean p2pServiceAdd(WifiP2pServiceInfo servInfo) {
        synchronized (sLock) {
            for (String s : servInfo.getSupplicantQueryList()) {
                String command = "P2P_SERVICE_ADD " + s;
                if (!doBooleanCommand(command)) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean p2pServiceDel(WifiP2pServiceInfo servInfo) {
        String command;
        synchronized (sLock) {
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

    public void setMiracastMode(int mode, int freq) {
        doBooleanCommand("DRIVER MIRACAST " + mode + " freq=" + freq);
    }

    public boolean fetchAnqp(String bssid, String subtypes) {
        return doBooleanCommand("ANQP_GET " + bssid + " " + subtypes);
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

    public synchronized String readKernelLog() {
        byte[] bytes = readKernelLogNative();
        if (bytes != null) {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            try {
                CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
                return decoded.toString();
            } catch (CharacterCodingException e) {
                return new String(bytes, StandardCharsets.ISO_8859_1);
            }
        }
        return "*** failed to read kernel log ***";
    }

    private static int getNewCmdIdLocked() {
        int i = sCmdId;
        sCmdId = i + 1;
        return i;
    }

    private static class MonitorThread extends Thread {
        MonitorThread(MonitorThread monitorThread) {
            this();
        }

        private MonitorThread() {
        }

        @Override
        public void run() {
            Log.i(WifiNative.TAG, "Waiting for HAL events mWifiHalHandle=" + Long.toString(WifiNative.sWifiHalHandle));
            WifiNative.waitForHalEventNative();
        }
    }

    public boolean startHal() {
        String debugLog = "startHal stack: ";
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 2; i < elements.length && i <= 7; i++) {
            debugLog = debugLog + " - " + elements[i].getMethodName();
        }
        sLocalLog.log(debugLog);
        synchronized (sLock) {
            if (startHalNative()) {
                int wlan0Index = queryInterfaceIndex(this.mInterfaceName);
                if (wlan0Index == -1) {
                    if (DBG) {
                        sLocalLog.log("Could not find interface with name: " + this.mInterfaceName);
                    }
                    return false;
                }
                sWlan0Index = wlan0Index;
                sThread = new MonitorThread(null);
                sThread.start();
                return true;
            }
            if (DBG) {
                sLocalLog.log("Could not start hal");
            }
            Log.e(TAG, "Could not start hal");
            return false;
        }
    }

    public void stopHal() {
        synchronized (sLock) {
            if (isHalStarted()) {
                stopHalNative();
                try {
                    sThread.join(1000L);
                    Log.d(TAG, "HAL event thread stopped successfully");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Could not stop HAL cleanly");
                }
                sThread = null;
                sWifiHalHandle = 0L;
                sWifiIfaceHandles = null;
                sWlan0Index = -1;
            }
        }
    }

    public boolean isHalStarted() {
        return sWifiHalHandle != 0;
    }

    public int queryInterfaceIndex(String interfaceName) {
        synchronized (sLock) {
            if (isHalStarted()) {
                int num = getInterfacesNative();
                for (int i = 0; i < num; i++) {
                    String name = getInterfaceNameNative(i);
                    if (name.equals(interfaceName)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }

    public String getInterfaceName(int index) {
        String interfaceNameNative;
        synchronized (sLock) {
            interfaceNameNative = getInterfaceNameNative(index);
        }
        return interfaceNameNative;
    }

    public boolean getScanCapabilities(ScanCapabilities capabilities) {
        boolean scanCapabilitiesNative;
        synchronized (sLock) {
            scanCapabilitiesNative = isHalStarted() ? getScanCapabilitiesNative(sWlan0Index, capabilities) : false;
        }
        return scanCapabilitiesNative;
    }

    private static void onScanStatus(int id, int event) {
        ScanEventHandler handler = sScanEventHandler;
        if (handler == null) {
            return;
        }
        handler.onScanStatus(event);
    }

    public static WifiSsid createWifiSsid(byte[] rawSsid) {
        String ssidHexString = String.valueOf(HexEncoding.encode(rawSsid));
        if (ssidHexString == null) {
            return null;
        }
        WifiSsid wifiSsid = WifiSsid.createFromHex(ssidHexString);
        return wifiSsid;
    }

    public static String ssidConvert(byte[] rawSsid) {
        String string;
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        try {
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(rawSsid));
            string = decoded.toString();
        } catch (CharacterCodingException e) {
            string = null;
        }
        if (string == null) {
            return new String(rawSsid, StandardCharsets.ISO_8859_1);
        }
        return string;
    }

    public static boolean setSsid(byte[] rawSsid, ScanResult result) {
        if (rawSsid == null || rawSsid.length == 0 || result == null) {
            return false;
        }
        result.SSID = ssidConvert(rawSsid);
        result.wifiSsid = createWifiSsid(rawSsid);
        return true;
    }

    private static void populateScanResult(ScanResult result, int beaconCap, String dbg) {
        if (dbg == null) {
            dbg = "";
        }
        InformationElementUtil.HtOperation htOperation = new InformationElementUtil.HtOperation();
        InformationElementUtil.VhtOperation vhtOperation = new InformationElementUtil.VhtOperation();
        InformationElementUtil.ExtendedCapabilities extendedCaps = new InformationElementUtil.ExtendedCapabilities();
        ScanResult.InformationElement[] elements = InformationElementUtil.parseInformationElements(result.bytes);
        for (ScanResult.InformationElement ie : elements) {
            if (ie.id == 61) {
                htOperation.from(ie);
            } else if (ie.id == 192) {
                vhtOperation.from(ie);
            } else if (ie.id == 127) {
                extendedCaps.from(ie);
            }
        }
        if (extendedCaps.is80211McRTTResponder) {
            result.setFlag(2L);
        } else {
            result.clearFlag(2L);
        }
        if (vhtOperation.isValid()) {
            result.channelWidth = vhtOperation.getChannelWidth();
            result.centerFreq0 = vhtOperation.getCenterFreq0();
            result.centerFreq1 = vhtOperation.getCenterFreq1();
        } else {
            result.channelWidth = htOperation.getChannelWidth();
            result.centerFreq0 = htOperation.getCenterFreq0(result.frequency);
            result.centerFreq1 = 0;
        }
        BitSet beaconCapBits = new BitSet(16);
        for (int i = 0; i < 16; i++) {
            if (((1 << i) & beaconCap) != 0) {
                beaconCapBits.set(i);
            }
        }
        result.capabilities = InformationElementUtil.Capabilities.buildCapabilities(elements, beaconCapBits);
        if (DBG) {
            Log.d(TAG, dbg + "SSID: " + result.SSID + " ChannelWidth is: " + result.channelWidth + " PrimaryFreq: " + result.frequency + " mCenterfreq0: " + result.centerFreq0 + " mCenterfreq1: " + result.centerFreq1 + (extendedCaps.is80211McRTTResponder ? "Support RTT reponder: " : "Do not support RTT responder") + " Capabilities: " + result.capabilities);
        }
        result.informationElements = elements;
    }

    private static void onFullScanResult(int id, ScanResult result, int bucketsScanned, int beaconCap) {
        if (DBG) {
            Log.i(TAG, "Got a full scan results event, ssid = " + result.SSID);
        }
        ScanEventHandler handler = sScanEventHandler;
        if (handler == null) {
            return;
        }
        populateScanResult(result, beaconCap, " onFullScanResult ");
        handler.onFullScanResult(result, bucketsScanned);
    }

    public boolean startScan(ScanSettings settings, ScanEventHandler eventHandler) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
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
                sScanCmdId = 0;
                return false;
            }
            return true;
        }
    }

    public void stopScan() {
        synchronized (sLock) {
            if (isHalStarted()) {
                if (sScanCmdId != 0) {
                    stopScanNative(sWlan0Index, sScanCmdId);
                }
                sScanSettings = null;
                sScanEventHandler = null;
                sScanCmdId = 0;
            }
        }
    }

    public void pauseScan() {
        synchronized (sLock) {
            if (isHalStarted() && sScanCmdId != 0 && sScanSettings != null && sScanEventHandler != null) {
                Log.d(TAG, "Pausing scan");
                WifiScanner.ScanData[] scanData = getScanResultsNative(sWlan0Index, true);
                stopScanNative(sWlan0Index, sScanCmdId);
                sScanCmdId = 0;
                sScanEventHandler.onScanPaused(scanData);
            }
        }
    }

    public void restartScan() {
        synchronized (sLock) {
            if (isHalStarted() && sScanCmdId == 0 && sScanSettings != null && sScanEventHandler != null) {
                Log.d(TAG, "Restarting scan");
                ScanEventHandler handler = sScanEventHandler;
                ScanSettings settings = sScanSettings;
                if (startScan(sScanSettings, sScanEventHandler)) {
                    sScanEventHandler.onScanRestarted();
                } else {
                    sScanEventHandler = handler;
                    sScanSettings = settings;
                }
            }
        }
    }

    public WifiScanner.ScanData[] getScanResults(boolean flush) {
        synchronized (sLock) {
            WifiScanner.ScanData[] sd = null;
            if (isHalStarted()) {
                sd = getScanResultsNative(sWlan0Index, flush);
            }
            if (sd != null) {
                return sd;
            }
            return new WifiScanner.ScanData[0];
        }
    }

    public boolean setHotlist(WifiScanner.HotlistSettings settings, HotlistEventHandler eventHandler) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            if (sHotlistCmdId != 0) {
                return false;
            }
            sHotlistCmdId = getNewCmdIdLocked();
            sHotlistEventHandler = eventHandler;
            if (!setHotlistNative(sWlan0Index, sHotlistCmdId, settings)) {
                sHotlistEventHandler = null;
                return false;
            }
            return true;
        }
    }

    public void resetHotlist() {
        synchronized (sLock) {
            if (isHalStarted() && sHotlistCmdId != 0) {
                resetHotlistNative(sWlan0Index, sHotlistCmdId);
                sHotlistCmdId = 0;
                sHotlistEventHandler = null;
            }
        }
    }

    private static void onHotlistApFound(int id, ScanResult[] results) {
        HotlistEventHandler handler = sHotlistEventHandler;
        if (handler != null) {
            handler.onHotlistApFound(results);
        } else {
            Log.d(TAG, "Ignoring hotlist AP found event");
        }
    }

    private static void onHotlistApLost(int id, ScanResult[] results) {
        HotlistEventHandler handler = sHotlistEventHandler;
        if (handler != null) {
            handler.onHotlistApLost(results);
        } else {
            Log.d(TAG, "Ignoring hotlist AP lost event");
        }
    }

    public boolean trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings, SignificantWifiChangeEventHandler handler) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            if (sSignificantWifiChangeCmdId != 0) {
                return false;
            }
            sSignificantWifiChangeCmdId = getNewCmdIdLocked();
            sSignificantWifiChangeHandler = handler;
            if (!trackSignificantWifiChangeNative(sWlan0Index, sSignificantWifiChangeCmdId, settings)) {
                sSignificantWifiChangeHandler = null;
                return false;
            }
            return true;
        }
    }

    public void untrackSignificantWifiChange() {
        synchronized (sLock) {
            if (isHalStarted() && sSignificantWifiChangeCmdId != 0) {
                untrackSignificantWifiChangeNative(sWlan0Index, sSignificantWifiChangeCmdId);
                sSignificantWifiChangeCmdId = 0;
                sSignificantWifiChangeHandler = null;
            }
        }
    }

    private static void onSignificantWifiChange(int id, ScanResult[] results) {
        SignificantWifiChangeEventHandler handler = sSignificantWifiChangeHandler;
        if (handler != null) {
            handler.onChangesFound(results);
        } else {
            Log.d(TAG, "Ignoring significant wifi change");
        }
    }

    public WifiLinkLayerStats getWifiLinkLayerStats(String iface) {
        if (iface == null) {
            return null;
        }
        synchronized (sLock) {
            if (!isHalStarted()) {
                return null;
            }
            return getWifiLinkLayerStatsNative(sWlan0Index);
        }
    }

    public void setWifiLinkLayerStats(String iface, int enable) {
        if (iface == null) {
            return;
        }
        synchronized (sLock) {
            if (isHalStarted()) {
                setWifiLinkLayerStatsNative(sWlan0Index, enable);
            }
        }
    }

    public int getSupportedFeatureSet() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getSupportedFeatureSetNative(sWlan0Index);
            }
            Log.d(TAG, "Failing getSupportedFeatureset because HAL isn't started");
            return 0;
        }
    }

    private static void onRttResults(int id, RttManager.RttResult[] results) {
        RttEventHandler handler = sRttEventHandler;
        if (handler != null && id == sRttCmdId) {
            Log.d(TAG, "Received " + results.length + " rtt results");
            handler.onRttResults(results);
            sRttCmdId = 0;
            return;
        }
        Log.d(TAG, "RTT Received event for unknown cmd = " + id + ", current id = " + sRttCmdId);
    }

    public boolean requestRtt(RttManager.RttParams[] params, RttEventHandler handler) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            if (sRttCmdId != 0) {
                Log.v("TAG", "Last one is still under measurement!");
                return false;
            }
            sRttCmdId = getNewCmdIdLocked();
            sRttEventHandler = handler;
            Log.v(TAG, "native issue RTT request");
            return requestRangeNative(sWlan0Index, sRttCmdId, params);
        }
    }

    public boolean cancelRtt(RttManager.RttParams[] params) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            if (sRttCmdId == 0) {
                return false;
            }
            sRttCmdId = 0;
            if (cancelRangeRequestNative(sWlan0Index, sRttCmdId, params)) {
                sRttEventHandler = null;
                Log.v(TAG, "RTT cancel Request Successfully");
                return true;
            }
            Log.e(TAG, "RTT cancel Request failed");
            return false;
        }
    }

    public RttManager.ResponderConfig enableRttResponder(int timeoutSeconds) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return null;
            }
            if (sRttResponderCmdId != 0) {
                if (DBG) {
                    Log.e(this.mTAG, "responder mode already enabled - this shouldn't happen");
                }
                return null;
            }
            int id = getNewCmdIdLocked();
            RttManager.ResponderConfig config = enableRttResponderNative(sWlan0Index, id, timeoutSeconds, null);
            if (config != null) {
                sRttResponderCmdId = id;
            }
            if (DBG) {
                Log.d(TAG, "enabling rtt " + (config != null));
            }
            return config;
        }
    }

    public boolean disableRttResponder() {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            if (sRttResponderCmdId == 0) {
                Log.e(this.mTAG, "responder role not enabled yet");
                return true;
            }
            sRttResponderCmdId = 0;
            return disableRttResponderNative(sWlan0Index, sRttResponderCmdId);
        }
    }

    public boolean setScanningMacOui(byte[] oui) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return setScanningMacOuiNative(sWlan0Index, oui);
            }
            return false;
        }
    }

    public int[] getChannelsForBand(int band) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getChannelsForBandNative(sWlan0Index, band);
            }
            return null;
        }
    }

    public boolean isGetChannelsForBandSupported() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return isGetChannelsForBandSupportedNative();
            }
            return false;
        }
    }

    public boolean setDfsFlag(boolean dfsOn) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return setDfsFlagNative(sWlan0Index, dfsOn);
            }
            return false;
        }
    }

    public boolean setInterfaceUp(boolean up) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return setInterfaceUpNative(up);
            }
            return false;
        }
    }

    public RttManager.RttCapabilities getRttCapabilities() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getRttCapabilitiesNative(sWlan0Index);
            }
            return null;
        }
    }

    public ApfCapabilities getApfCapabilities() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getApfCapabilitiesNative(sWlan0Index);
            }
            return null;
        }
    }

    public boolean installPacketFilter(byte[] filter) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return installPacketFilterNative(sWlan0Index, filter);
            }
            return false;
        }
    }

    public boolean setCountryCodeHal(String CountryCode) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return setCountryCodeHalNative(sWlan0Index, CountryCode);
            }
            return false;
        }
    }

    public abstract class TdlsEventHandler {
        public abstract void onTdlsStatus(String str, int i, int i2);

        public TdlsEventHandler() {
        }
    }

    public boolean enableDisableTdls(boolean enable, String macAdd, TdlsEventHandler tdlsCallBack) {
        boolean zEnableDisableTdlsNative;
        synchronized (sLock) {
            sTdlsEventHandler = tdlsCallBack;
            zEnableDisableTdlsNative = enableDisableTdlsNative(sWlan0Index, enable, macAdd);
        }
        return zEnableDisableTdlsNative;
    }

    public TdlsStatus getTdlsStatus(String macAdd) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getTdlsStatusNative(sWlan0Index, macAdd);
            }
            return null;
        }
    }

    public TdlsCapabilities getTdlsCapabilities() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getTdlsCapabilitiesNative(sWlan0Index);
            }
            return null;
        }
    }

    private static boolean onTdlsStatus(String macAddr, int status, int reason) {
        TdlsEventHandler handler = sTdlsEventHandler;
        if (handler == null) {
            return false;
        }
        handler.onTdlsStatus(macAddr, status, reason);
        return true;
    }

    private static void onRingBufferData(RingBufferStatus status, byte[] buffer) {
        WifiLoggerEventHandler handler = sWifiLoggerEventHandler;
        if (handler == null) {
            return;
        }
        handler.onRingBufferData(status, buffer);
    }

    private static void onWifiAlert(byte[] buffer, int errorCode) {
        WifiLoggerEventHandler handler = sWifiLoggerEventHandler;
        if (handler == null) {
            return;
        }
        handler.onWifiAlert(errorCode, buffer);
    }

    public boolean setLoggingEventHandler(WifiLoggerEventHandler handler) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            int oldId = sLogCmdId;
            sLogCmdId = getNewCmdIdLocked();
            if (!setLoggingEventHandlerNative(sWlan0Index, sLogCmdId)) {
                sLogCmdId = oldId;
                return false;
            }
            sWifiLoggerEventHandler = handler;
            return true;
        }
    }

    public boolean startLoggingRingBuffer(int verboseLevel, int flags, int maxInterval, int minDataSize, String ringName) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return startLoggingRingBufferNative(sWlan0Index, verboseLevel, flags, maxInterval, minDataSize, ringName);
            }
            return false;
        }
    }

    public int getSupportedLoggerFeatureSet() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getSupportedLoggerFeatureSetNative(sWlan0Index);
            }
            return 0;
        }
    }

    public boolean resetLogHandler() {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            if (sLogCmdId == -1) {
                Log.e(TAG, "Can not reset handler Before set any handler");
                return false;
            }
            sWifiLoggerEventHandler = null;
            if (!resetLogHandlerNative(sWlan0Index, sLogCmdId)) {
                return false;
            }
            sLogCmdId = -1;
            return true;
        }
    }

    public String getDriverVersion() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getDriverVersionNative(sWlan0Index);
            }
            return "";
        }
    }

    public String getFirmwareVersion() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getFirmwareVersionNative(sWlan0Index);
            }
            return "";
        }
    }

    public static class RingBufferStatus {
        int flag;
        String name;
        int readBytes;
        int ringBufferByteSize;
        int ringBufferId;
        int verboseLevel;
        int writtenBytes;
        int writtenRecords;

        public String toString() {
            return "name: " + this.name + " flag: " + this.flag + " ringBufferId: " + this.ringBufferId + " ringBufferByteSize: " + this.ringBufferByteSize + " verboseLevel: " + this.verboseLevel + " writtenBytes: " + this.writtenBytes + " readBytes: " + this.readBytes + " writtenRecords: " + this.writtenRecords;
        }
    }

    public RingBufferStatus[] getRingBufferStatus() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getRingBufferStatusNative(sWlan0Index);
            }
            return null;
        }
    }

    public boolean getRingBufferData(String ringName) {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getRingBufferDataNative(sWlan0Index, ringName);
            }
            return false;
        }
    }

    private static void onWifiFwMemoryAvailable(byte[] buffer) {
        mFwMemoryDump = buffer;
        if (!DBG) {
            return;
        }
        Log.d(TAG, "onWifiFwMemoryAvailable is called and buffer length is: " + (buffer == null ? 0 : buffer.length));
    }

    public byte[] getFwMemoryDump() {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return null;
            }
            if (!getFwMemoryDumpNative(sWlan0Index)) {
                return null;
            }
            byte[] fwMemoryDump = mFwMemoryDump;
            mFwMemoryDump = null;
            return fwMemoryDump;
        }
    }

    public byte[] getDriverStateDump() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return getDriverStateDumpNative(sWlan0Index);
            }
            return null;
        }
    }

    @Immutable
    static abstract class FateReport {
        static final int MAX_DRIVER_TIMESTAMP_MSEC = 4294967;
        static final int USEC_PER_MSEC = 1000;
        static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
        final long mDriverTimestampUSec;
        final long mEstimatedWallclockMSec;
        final byte mFate;
        final byte[] mFrameBytes;
        final byte mFrameType;

        protected abstract String directionToString();

        protected abstract String fateToString();

        FateReport(byte fate, long driverTimestampUSec, byte frameType, byte[] frameBytes) {
            this.mFate = fate;
            this.mDriverTimestampUSec = driverTimestampUSec;
            this.mEstimatedWallclockMSec = convertDriverTimestampUSecToWallclockMSec(this.mDriverTimestampUSec);
            this.mFrameType = frameType;
            this.mFrameBytes = frameBytes;
        }

        public String toTableRowString() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            FrameParser parser = new FrameParser(this.mFrameType, this.mFrameBytes);
            dateFormatter.setTimeZone(TimeZone.getDefault());
            pw.format("%-15s  %12s  %-9s  %-32s  %-12s  %-23s  %s\n", Long.valueOf(this.mDriverTimestampUSec), dateFormatter.format(new Date(this.mEstimatedWallclockMSec)), directionToString(), fateToString(), parser.mMostSpecificProtocolString, parser.mTypeString, parser.mResultString);
            return sw.toString();
        }

        public String toVerboseStringWithPiiAllowed() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            FrameParser parser = new FrameParser(this.mFrameType, this.mFrameBytes);
            pw.format("Frame direction: %s\n", directionToString());
            pw.format("Frame timestamp: %d\n", Long.valueOf(this.mDriverTimestampUSec));
            pw.format("Frame fate: %s\n", fateToString());
            pw.format("Frame type: %s\n", frameTypeToString(this.mFrameType));
            pw.format("Frame protocol: %s\n", parser.mMostSpecificProtocolString);
            pw.format("Frame protocol type: %s\n", parser.mTypeString);
            pw.format("Frame length: %d\n", Integer.valueOf(this.mFrameBytes.length));
            pw.append((CharSequence) "Frame bytes");
            pw.append((CharSequence) HexDump.dumpHexString(this.mFrameBytes));
            pw.append((CharSequence) "\n");
            return sw.toString();
        }

        public static String getTableHeader() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.format("\n%-15s  %-12s  %-9s  %-32s  %-12s  %-23s  %s\n", "Time usec", "Walltime", "Direction", "Fate", "Protocol", MOTree.TypeTag, "Result");
            pw.format("%-15s  %-12s  %-9s  %-32s  %-12s  %-23s  %s\n", "---------", "--------", "---------", "----", "--------", "----", "------");
            return sw.toString();
        }

        private static String frameTypeToString(byte frameType) {
            switch (frameType) {
                case 0:
                    return "unknown";
                case 1:
                    return "data";
                case 2:
                    return "802.11 management";
                default:
                    return Byte.toString(frameType);
            }
        }

        private static long convertDriverTimestampUSecToWallclockMSec(long driverTimestampUSec) {
            long wallclockMillisNow = System.currentTimeMillis();
            long boottimeMillisNow = SystemClock.elapsedRealtime();
            long driverTimestampMillis = driverTimestampUSec / 1000;
            long boottimeTimestampMillis = boottimeMillisNow % 4294967;
            if (boottimeTimestampMillis < driverTimestampMillis) {
                boottimeTimestampMillis += 4294967;
            }
            long millisSincePacketTimestamp = boottimeTimestampMillis - driverTimestampMillis;
            return wallclockMillisNow - millisSincePacketTimestamp;
        }
    }

    @Immutable
    public static final class TxFateReport extends FateReport {
        @Override
        public String toTableRowString() {
            return super.toTableRowString();
        }

        @Override
        public String toVerboseStringWithPiiAllowed() {
            return super.toVerboseStringWithPiiAllowed();
        }

        TxFateReport(byte fate, long driverTimestampUSec, byte frameType, byte[] frameBytes) {
            super(fate, driverTimestampUSec, frameType, frameBytes);
        }

        @Override
        protected String directionToString() {
            return "TX";
        }

        @Override
        protected String fateToString() {
            switch (this.mFate) {
                case 0:
                    return "acked";
                case 1:
                    return "sent";
                case 2:
                    return "firmware queued";
                case 3:
                    return "firmware dropped (invalid frame)";
                case 4:
                    return "firmware dropped (no bufs)";
                case 5:
                    return "firmware dropped (other)";
                case 6:
                    return "driver queued";
                case 7:
                    return "driver dropped (invalid frame)";
                case 8:
                default:
                    return Byte.toString(this.mFate);
                case 9:
                    return "driver dropped (no bufs)";
                case 10:
                    return "driver dropped (other)";
            }
        }
    }

    @Immutable
    public static final class RxFateReport extends FateReport {
        @Override
        public String toTableRowString() {
            return super.toTableRowString();
        }

        @Override
        public String toVerboseStringWithPiiAllowed() {
            return super.toVerboseStringWithPiiAllowed();
        }

        RxFateReport(byte fate, long driverTimestampUSec, byte frameType, byte[] frameBytes) {
            super(fate, driverTimestampUSec, frameType, frameBytes);
        }

        @Override
        protected String directionToString() {
            return "RX";
        }

        @Override
        protected String fateToString() {
            switch (this.mFate) {
                case 0:
                    return "success";
                case 1:
                    return "firmware queued";
                case 2:
                    return "firmware dropped (filter)";
                case 3:
                    return "firmware dropped (invalid frame)";
                case 4:
                    return "firmware dropped (no bufs)";
                case 5:
                    return "firmware dropped (other)";
                case 6:
                    return "driver queued";
                case 7:
                    return "driver dropped (filter)";
                case 8:
                    return "driver dropped (invalid frame)";
                case 9:
                    return "driver dropped (no bufs)";
                case 10:
                    return "driver dropped (other)";
                default:
                    return Byte.toString(this.mFate);
            }
        }
    }

    public boolean startPktFateMonitoring() {
        synchronized (sLock) {
            if (isHalStarted()) {
                return startPktFateMonitoringNative(sWlan0Index) == 0;
            }
            return false;
        }
    }

    public boolean getTxPktFates(TxFateReport[] reportBufs) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            int res = getTxPktFatesNative(sWlan0Index, reportBufs);
            if (res != 0) {
                Log.e(TAG, "getTxPktFatesNative returned " + res);
                return false;
            }
            return true;
        }
    }

    public boolean getRxPktFates(RxFateReport[] reportBufs) {
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            int res = getRxPktFatesNative(sWlan0Index, reportBufs);
            if (res != 0) {
                Log.e(TAG, "getRxPktFatesNative returned " + res);
                return false;
            }
            return true;
        }
    }

    public boolean setPnoList(PnoSettings settings, PnoEventHandler eventHandler) {
        Log.e(TAG, "setPnoList cmd " + sPnoCmdId);
        synchronized (sLock) {
            if (isHalStarted()) {
                sPnoCmdId = getNewCmdIdLocked();
                sPnoEventHandler = eventHandler;
                if (setPnoListNative(sWlan0Index, sPnoCmdId, settings)) {
                    return true;
                }
            }
            sPnoEventHandler = null;
            return false;
        }
    }

    public boolean setPnoList(PnoNetwork[] list, PnoEventHandler eventHandler) {
        PnoSettings settings = new PnoSettings();
        settings.networkList = list;
        return setPnoList(settings, eventHandler);
    }

    public boolean resetPnoList() {
        Log.e(TAG, "resetPnoList cmd " + sPnoCmdId);
        synchronized (sLock) {
            if (isHalStarted()) {
                sPnoCmdId = getNewCmdIdLocked();
                sPnoEventHandler = null;
                if (resetPnoListNative(sWlan0Index, sPnoCmdId)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void onPnoNetworkFound(int id, ScanResult[] results, int[] beaconCaps) {
        if (results == null) {
            Log.e(TAG, "onPnoNetworkFound null results");
            return;
        }
        Log.d(TAG, "WifiNative.onPnoNetworkFound result " + results.length);
        PnoEventHandler handler = sPnoEventHandler;
        if (sPnoCmdId == 0 || handler == null) {
            Log.d(TAG, "Ignoring Pno Network found event");
            return;
        }
        for (int i = 0; i < results.length; i++) {
            Log.e(TAG, "onPnoNetworkFound SSID " + results[i].SSID + " " + results[i].level + " " + results[i].frequency);
            populateScanResult(results[i], beaconCaps[i], "onPnoNetworkFound ");
            results[i].wifiSsid = WifiSsid.createFromAsciiEncoded(results[i].SSID);
        }
        handler.onPnoNetworkFound(results);
    }

    public boolean setBssidBlacklist(String[] list) {
        int size = 0;
        if (list != null) {
            size = list.length;
        }
        Log.e(TAG, "setBssidBlacklist cmd " + sPnoCmdId + " size " + size);
        synchronized (sLock) {
            if (isHalStarted()) {
                sPnoCmdId = getNewCmdIdLocked();
                return setBssidBlacklistNative(sWlan0Index, sPnoCmdId, list);
            }
            return false;
        }
    }

    public int startSendingOffloadedPacket(int slot, KeepalivePacketData keepAlivePacket, int period) {
        Log.d(TAG, "startSendingOffloadedPacket slot=" + slot + " period=" + period);
        String[] macAddrStr = getMacAddress().split(":");
        byte[] srcMac = new byte[6];
        for (int i = 0; i < 6; i++) {
            Integer hexVal = Integer.valueOf(Integer.parseInt(macAddrStr[i], 16));
            srcMac[i] = hexVal.byteValue();
        }
        synchronized (sLock) {
            if (isHalStarted()) {
                return startSendingOffloadedPacketNative(sWlan0Index, slot, srcMac, keepAlivePacket.dstMac, keepAlivePacket.data, period);
            }
            return -1;
        }
    }

    public int stopSendingOffloadedPacket(int slot) {
        Log.d(TAG, "stopSendingOffloadedPacket " + slot);
        synchronized (sLock) {
            if (isHalStarted()) {
                return stopSendingOffloadedPacketNative(sWlan0Index, slot);
            }
            return -1;
        }
    }

    private static void onRssiThresholdBreached(int id, byte curRssi) {
        WifiRssiEventHandler handler = sWifiRssiEventHandler;
        if (handler == null) {
            return;
        }
        handler.onRssiThresholdBreached(curRssi);
    }

    public int startRssiMonitoring(byte maxRssi, byte minRssi, WifiRssiEventHandler rssiEventHandler) {
        Log.d(TAG, "startRssiMonitoring: maxRssi=" + ((int) maxRssi) + " minRssi=" + ((int) minRssi));
        synchronized (sLock) {
            sWifiRssiEventHandler = rssiEventHandler;
            if (isHalStarted()) {
                if (sRssiMonitorCmdId != 0) {
                    stopRssiMonitoring();
                }
                sRssiMonitorCmdId = getNewCmdIdLocked();
                Log.d(TAG, "sRssiMonitorCmdId = " + sRssiMonitorCmdId);
                int ret = startRssiMonitoringNative(sWlan0Index, sRssiMonitorCmdId, maxRssi, minRssi);
                if (ret != 0) {
                    sRssiMonitorCmdId = 0;
                }
                return ret;
            }
            return -1;
        }
    }

    public int stopRssiMonitoring() {
        Log.d(TAG, "stopRssiMonitoring, cmdId " + sRssiMonitorCmdId);
        synchronized (sLock) {
            if (isHalStarted()) {
                int ret = 0;
                if (sRssiMonitorCmdId != 0) {
                    ret = stopRssiMonitoringNative(sWlan0Index, sRssiMonitorCmdId);
                }
                sRssiMonitorCmdId = 0;
                return ret;
            }
            return -1;
        }
    }

    public WifiWakeReasonAndCounts getWlanWakeReasonCount() {
        Log.d(TAG, "getWlanWakeReasonCount " + sWlan0Index);
        synchronized (sLock) {
            if (isHalStarted()) {
                return getWlanWakeReasonCountNative(sWlan0Index);
            }
            return null;
        }
    }

    public boolean configureNeighborDiscoveryOffload(boolean enabled) {
        String logMsg = "configureNeighborDiscoveryOffload(" + enabled + ")";
        Log.d(this.mTAG, logMsg);
        synchronized (sLock) {
            if (!isHalStarted()) {
                return false;
            }
            int ret = configureNeighborDiscoveryOffload(sWlan0Index, enabled);
            if (ret != 0) {
                Log.d(this.mTAG, logMsg + " returned: " + ret);
            }
            return ret == 0;
        }
    }

    public String p2pGetVendorElems(String deviceAddress) {
        return p2pGetParam(deviceAddress, "vendor_elems");
    }

    public boolean doCtiaTestOn() {
        return doBooleanCommand("DRIVER smt-test-on");
    }

    public boolean doCtiaTestOff() {
        return doBooleanCommand("DRIVER smt-test-off");
    }

    public boolean doCtiaTestRate(int rate) {
        return doBooleanCommand("DRIVER smt-rate " + rate);
    }

    public boolean setBssExpireAge(int value) {
        return doBooleanCommand("BSS_EXPIRE_AGE " + value);
    }

    public boolean setBssExpireCount(int value) {
        return doBooleanCommand("BSS_EXPIRE_COUNT " + value);
    }

    public boolean getDisconnectFlag() {
        return this.mDisconnectCalled;
    }

    public boolean setWoWlanNormalModeCommand() {
        return doBooleanCommand("DRIVER_WOWLAN_NORMAL");
    }

    public boolean setWoWlanMagicModeCommand() {
        return doBooleanCommand("DRIVER_WOWLAN_MAGIC");
    }

    public boolean setHotspotOptimization(boolean enable) {
        if (enable) {
            return doBooleanCommand("DRIVER set_chip greenAp 1");
        }
        return doBooleanCommand("DRIVER set_chip greenAp 0");
    }

    public String getTestEnv(int channel) {
        if (channel < -1) {
            return null;
        }
        return doStringCommand("DRIVER CH_ENV_GET" + (channel == -1 ? "" : " " + channel));
    }

    public boolean setTdlsPowerSave(boolean enable) {
        if (enable) {
            return doBooleanCommand("DRIVER TDLS-PS 1");
        }
        return doBooleanCommand("DRIVER TDLS-PS 0");
    }
}
