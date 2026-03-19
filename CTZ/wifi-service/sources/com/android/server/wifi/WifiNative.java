package com.android.server.wifi;

import android.net.InterfaceConfiguration;
import android.net.MacAddress;
import android.net.TrafficStats;
import android.net.apf.ApfCapabilities;
import android.net.wifi.RttManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiWakeReasonAndCounts;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.HexDump;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.util.FrameParser;
import com.android.server.wifi.util.NativeUtil;
import com.mediatek.server.wifi.MtkHostapdHal;
import com.mediatek.server.wifi.MtkWifiApmDelegate;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

public class WifiNative {
    public static final int BLUETOOTH_COEXISTENCE_MODE_DISABLED = 1;
    public static final int BLUETOOTH_COEXISTENCE_MODE_ENABLED = 0;
    public static final int BLUETOOTH_COEXISTENCE_MODE_SENSE = 2;
    private static final int CONNECT_TO_HOSTAPD_RETRY_INTERVAL_MS = 100;
    private static final int CONNECT_TO_HOSTAPD_RETRY_TIMES = 50;
    private static final int CONNECT_TO_SUPPLICANT_RETRY_INTERVAL_MS = 100;
    private static final int CONNECT_TO_SUPPLICANT_RETRY_TIMES = 50;
    public static final int DISABLE_FIRMWARE_ROAMING = 0;
    public static final int EAP_SIM_VENDOR_SPECIFIC_CERT_EXPIRED = 16385;
    public static final int ENABLE_FIRMWARE_ROAMING = 1;
    public static final int RX_FILTER_TYPE_V4_MULTICAST = 0;
    public static final int RX_FILTER_TYPE_V6_MULTICAST = 1;
    public static final int SCAN_TYPE_HIGH_ACCURACY = 2;
    public static final int SCAN_TYPE_LOW_LATENCY = 0;
    public static final int SCAN_TYPE_LOW_POWER = 1;
    public static final String SIM_AUTH_RESP_TYPE_GSM_AUTH = "GSM-AUTH";
    public static final String SIM_AUTH_RESP_TYPE_UMTS_AUTH = "UMTS-AUTH";
    public static final String SIM_AUTH_RESP_TYPE_UMTS_AUTS = "UMTS-AUTS";
    private static final String TAG = "WifiNative";
    public static final int TX_POWER_SCENARIO_NORMAL = 0;
    public static final int TX_POWER_SCENARIO_VOICE_CALL = 1;
    public static final int WIFI_SCAN_FAILED = 3;
    public static final int WIFI_SCAN_RESULTS_AVAILABLE = 0;
    public static final int WIFI_SCAN_THRESHOLD_NUM_SCANS = 1;
    public static final int WIFI_SCAN_THRESHOLD_PERCENT = 2;
    private final HostapdHal mHostapdHal;
    private final INetworkManagementService mNwManagementService;
    private final PropertyService mPropertyService;
    private final SupplicantStaIfaceHal mSupplicantStaIfaceHal;
    private final WifiMetrics mWifiMetrics;
    private final WifiMonitor mWifiMonitor;
    private final WifiVendorHal mWifiVendorHal;
    private final WificondControl mWificondControl;
    private boolean mVerboseLoggingEnabled = false;
    private Object mLock = new Object();
    private final IfaceManager mIfaceMgr = new IfaceManager();
    private HashSet<StatusListener> mStatusListeners = new HashSet<>();

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

    public interface HostapdDeathEventHandler {
        void onDeath();
    }

    public interface InterfaceCallback {
        void onDestroyed(String str);

        void onDown(String str);

        void onUp(String str);
    }

    public interface PnoEventHandler {
        void onPnoNetworkFound(ScanResult[] scanResultArr);

        void onPnoScanFailed();
    }

    public static class PnoSettings {
        public int band5GHzBonus;
        public int currentConnectionBonus;
        public int initialScoreMax;
        public boolean isConnected;
        public int min24GHzRssi;
        public int min5GHzRssi;
        public PnoNetwork[] networkList;
        public int periodInMs;
        public int sameNetworkBonus;
        public int secureBonus;
    }

    public static class RoamingCapabilities {
        public int maxBlacklistSize;
        public int maxWhitelistSize;
    }

    public static class RoamingConfig {
        public ArrayList<String> blacklistBssids;
        public ArrayList<String> whitelistSsids;
    }

    public interface RttEventHandler {
        void onRttResults(RttManager.RttResult[] rttResultArr);
    }

    public static class ScanCapabilities {
        public int max_ap_cache_per_scan;
        public int max_rssi_sample_size;
        public int max_scan_buckets;
        public int max_scan_cache_size;
        public int max_scan_reporting_threshold;
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
        public HiddenNetwork[] hiddenNetworks;
        public int max_ap_per_scan;
        public int num_buckets;
        public int report_threshold_num_scans;
        public int report_threshold_percent;
        public int scanType;
    }

    public static class SignalPollResult {
        public int associationFrequency;
        public int currentRssi;
        public int txBitrate;
    }

    public interface SoftApListener {
        void onNumAssociatedStationsChanged(int i);

        void onSoftApChannelSwitched(int i, int i2);
    }

    public interface StatusListener {
        void onStatusChanged(boolean z);
    }

    public interface SupplicantDeathEventHandler {
        void onDeath();
    }

    public static class TxPacketCounters {
        public int txFailed;
        public int txSucceeded;
    }

    public interface VendorHalDeathEventHandler {
        void onDeath();
    }

    public interface VendorHalRadioModeChangeEventHandler {
        void onDbs();

        void onMcc(int i);

        void onSbs(int i);

        void onScc(int i);
    }

    public interface WifiLoggerEventHandler {
        void onRingBufferData(RingBufferStatus ringBufferStatus, byte[] bArr);

        void onWifiAlert(int i, byte[] bArr);
    }

    public interface WifiRssiEventHandler {
        void onRssiThresholdBreached(byte b);
    }

    public interface WificondDeathEventHandler {
        void onDeath();
    }

    private static native byte[] readKernelLogNative();

    private static native int registerNatives();

    public WifiNative(WifiVendorHal wifiVendorHal, SupplicantStaIfaceHal supplicantStaIfaceHal, HostapdHal hostapdHal, WificondControl wificondControl, WifiMonitor wifiMonitor, INetworkManagementService iNetworkManagementService, PropertyService propertyService, WifiMetrics wifiMetrics) {
        this.mWifiVendorHal = wifiVendorHal;
        this.mSupplicantStaIfaceHal = supplicantStaIfaceHal;
        this.mHostapdHal = hostapdHal;
        this.mWificondControl = wificondControl;
        this.mWifiMonitor = wifiMonitor;
        this.mNwManagementService = iNetworkManagementService;
        this.mPropertyService = propertyService;
        this.mWifiMetrics = wifiMetrics;
    }

    public void enableVerboseLogging(int i) {
        this.mVerboseLoggingEnabled = i > 0;
        this.mWificondControl.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mSupplicantStaIfaceHal.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiVendorHal.enableVerboseLogging(this.mVerboseLoggingEnabled);
    }

    private static class Iface {
        public static final int IFACE_TYPE_AP = 0;
        public static final int IFACE_TYPE_STA = 1;
        public InterfaceCallback externalListener;
        public final int id;
        public boolean isUp;
        public String name;
        public NetworkObserverInternal networkObserver;
        public final int type;

        @Retention(RetentionPolicy.SOURCE)
        public @interface IfaceType {
        }

        Iface(int i, int i2) {
            this.id = i;
            this.type = i2;
        }

        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("Iface:");
            stringBuffer.append("{");
            stringBuffer.append("Name=");
            stringBuffer.append(this.name);
            stringBuffer.append(",");
            stringBuffer.append("Id=");
            stringBuffer.append(this.id);
            stringBuffer.append(",");
            stringBuffer.append("Type=");
            stringBuffer.append(this.type == 1 ? "STA" : "AP");
            stringBuffer.append("}");
            return stringBuffer.toString();
        }
    }

    private static class IfaceManager {
        private HashMap<Integer, Iface> mIfaces;
        private int mNextId;

        private IfaceManager() {
            this.mIfaces = new HashMap<>();
        }

        private Iface allocateIface(int i) {
            Iface iface = new Iface(this.mNextId, i);
            this.mIfaces.put(Integer.valueOf(this.mNextId), iface);
            this.mNextId++;
            return iface;
        }

        private Iface removeIface(int i) {
            return this.mIfaces.remove(Integer.valueOf(i));
        }

        private Iface getIface(int i) {
            return this.mIfaces.get(Integer.valueOf(i));
        }

        private Iface getIface(String str) {
            for (Iface iface : this.mIfaces.values()) {
                if (TextUtils.equals(iface.name, str)) {
                    return iface;
                }
            }
            return null;
        }

        private Iterator<Integer> getIfaceIdIter() {
            return this.mIfaces.keySet().iterator();
        }

        private boolean hasAnyIface() {
            return !this.mIfaces.isEmpty();
        }

        private boolean hasAnyIfaceOfType(int i) {
            Iterator<Iface> it = this.mIfaces.values().iterator();
            while (it.hasNext()) {
                if (it.next().type == i) {
                    return true;
                }
            }
            return false;
        }

        private Iface findAnyIfaceOfType(int i) {
            for (Iface iface : this.mIfaces.values()) {
                if (iface.type == i) {
                    return iface;
                }
            }
            return null;
        }

        private boolean hasAnyStaIface() {
            return hasAnyIfaceOfType(1);
        }

        private boolean hasAnyApIface() {
            return hasAnyIfaceOfType(0);
        }

        private String findAnyStaIfaceName() {
            Iface ifaceFindAnyIfaceOfType = findAnyIfaceOfType(1);
            if (ifaceFindAnyIfaceOfType == null) {
                return null;
            }
            return ifaceFindAnyIfaceOfType.name;
        }

        private String findAnyApIfaceName() {
            Iface ifaceFindAnyIfaceOfType = findAnyIfaceOfType(0);
            if (ifaceFindAnyIfaceOfType == null) {
                return null;
            }
            return ifaceFindAnyIfaceOfType.name;
        }

        public Iface removeExistingIface(int i) {
            if (this.mIfaces.size() > 2) {
                Log.wtf(WifiNative.TAG, "More than 1 existing interface found");
            }
            Iterator<Map.Entry<Integer, Iface>> it = this.mIfaces.entrySet().iterator();
            Iface value = null;
            while (it.hasNext()) {
                Map.Entry<Integer, Iface> next = it.next();
                if (next.getKey().intValue() != i) {
                    value = next.getValue();
                    it.remove();
                }
            }
            return value;
        }
    }

    private boolean startHal() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyIface()) {
                if (this.mWifiVendorHal.isVendorHalSupported()) {
                    if (!this.mWifiVendorHal.startVendorHal()) {
                        Log.e(TAG, "Failed to start vendor HAL");
                        return false;
                    }
                } else {
                    Log.i(TAG, "Vendor Hal not supported, ignoring start.");
                }
            }
            return true;
        }
    }

    private void stopHalAndWificondIfNecessary() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyIface()) {
                if (!this.mWificondControl.tearDownInterfaces()) {
                    Log.e(TAG, "Failed to teardown ifaces from wificond");
                }
                if (this.mWifiVendorHal.isVendorHalSupported()) {
                    this.mWifiVendorHal.stopVendorHal();
                } else {
                    Log.i(TAG, "Vendor Hal not supported, ignoring stop.");
                }
            }
        }
    }

    private boolean waitForSupplicantConnection() {
        boolean zIsInitializationComplete = false;
        if (!this.mSupplicantStaIfaceHal.isInitializationStarted() && !this.mSupplicantStaIfaceHal.initialize()) {
            return false;
        }
        int i = 0;
        while (!zIsInitializationComplete) {
            int i2 = i + 1;
            if (i >= 50 || (zIsInitializationComplete = this.mSupplicantStaIfaceHal.isInitializationComplete())) {
                break;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
            i = i2;
        }
        return zIsInitializationComplete;
    }

    private boolean startSupplicant() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyStaIface()) {
                if (!this.mWificondControl.enableSupplicant()) {
                    Log.e(TAG, "Failed to enable supplicant");
                    return false;
                }
                if (!waitForSupplicantConnection()) {
                    Log.e(TAG, "Failed to connect to supplicant");
                    return false;
                }
                if (!this.mSupplicantStaIfaceHal.registerDeathHandler(new SupplicantDeathHandlerInternal())) {
                    Log.e(TAG, "Failed to register supplicant death handler");
                    return false;
                }
            }
            return true;
        }
    }

    private void stopSupplicantIfNecessary() {
        synchronized (this.mLock) {
            if (!this.mIfaceMgr.hasAnyStaIface()) {
                if (!this.mSupplicantStaIfaceHal.deregisterDeathHandler()) {
                    Log.e(TAG, "Failed to deregister supplicant death handler");
                }
                if (!this.mWificondControl.disableSupplicant()) {
                    Log.e(TAG, "Failed to disable supplicant");
                }
            }
        }
    }

    private boolean registerNetworkObserver(NetworkObserverInternal networkObserverInternal) {
        if (networkObserverInternal == null) {
            return false;
        }
        try {
            this.mNwManagementService.registerObserver(networkObserverInternal);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean unregisterNetworkObserver(NetworkObserverInternal networkObserverInternal) {
        if (networkObserverInternal == null) {
            return false;
        }
        try {
            this.mNwManagementService.unregisterObserver(networkObserverInternal);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private void onClientInterfaceDestroyed(Iface iface) {
        synchronized (this.mLock) {
            this.mWifiMonitor.stopMonitoring(iface.name);
            if (!unregisterNetworkObserver(iface.networkObserver)) {
                Log.e(TAG, "Failed to unregister network observer on " + iface);
            }
            if (!this.mSupplicantStaIfaceHal.teardownIface(iface.name)) {
                Log.e(TAG, "Failed to teardown iface in supplicant on " + iface);
            }
            if (!this.mWificondControl.tearDownClientInterface(iface.name)) {
                Log.e(TAG, "Failed to teardown iface in wificond on " + iface);
            }
            stopSupplicantIfNecessary();
            stopHalAndWificondIfNecessary();
        }
    }

    private void onSoftApInterfaceDestroyed(Iface iface) {
        synchronized (this.mLock) {
            if (!unregisterNetworkObserver(iface.networkObserver)) {
                Log.e(TAG, "Failed to unregister network observer on " + iface);
            }
            if (!this.mHostapdHal.removeAccessPoint(iface.name)) {
                Log.e(TAG, "Failed to remove access point on " + iface);
            }
            if (!this.mHostapdHal.deregisterDeathHandler()) {
                Log.e(TAG, "Failed to deregister supplicant death handler");
            }
            if (!this.mWificondControl.stopHostapd(iface.name)) {
                Log.e(TAG, "Failed to stop hostapd on " + iface);
            }
            if (!this.mWificondControl.tearDownSoftApInterface(iface.name)) {
                Log.e(TAG, "Failed to teardown iface in wificond on " + iface);
            }
            stopHalAndWificondIfNecessary();
        }
    }

    private void onInterfaceDestroyed(Iface iface) {
        synchronized (this.mLock) {
            if (iface.type == 1) {
                onClientInterfaceDestroyed(iface);
            } else if (iface.type == 0) {
                onSoftApInterfaceDestroyed(iface);
            }
            iface.externalListener.onDestroyed(iface.name);
        }
    }

    private class InterfaceDestoyedListenerInternal implements HalDeviceManager.InterfaceDestroyedListener {
        private final int mInterfaceId;

        InterfaceDestoyedListenerInternal(int i) {
            this.mInterfaceId = i;
        }

        @Override
        public void onDestroyed(String str) {
            synchronized (WifiNative.this.mLock) {
                Iface ifaceRemoveIface = WifiNative.this.mIfaceMgr.removeIface(this.mInterfaceId);
                if (ifaceRemoveIface == null) {
                    if (WifiNative.this.mVerboseLoggingEnabled) {
                        Log.v(WifiNative.TAG, "Received iface destroyed notification on an invalid iface=" + str);
                    }
                    return;
                }
                WifiNative.this.onInterfaceDestroyed(ifaceRemoveIface);
                Log.i(WifiNative.TAG, "Successfully torn down " + ifaceRemoveIface);
            }
        }
    }

    private void onNativeDaemonDeath() {
        synchronized (this.mLock) {
            Iterator<StatusListener> it = this.mStatusListeners.iterator();
            while (it.hasNext()) {
                it.next().onStatusChanged(false);
            }
            Iterator<StatusListener> it2 = this.mStatusListeners.iterator();
            while (it2.hasNext()) {
                it2.next().onStatusChanged(true);
            }
        }
    }

    private class VendorHalDeathHandlerInternal implements VendorHalDeathEventHandler {
        private VendorHalDeathHandlerInternal() {
        }

        @Override
        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Vendor HAL died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumHalCrashes();
            }
        }
    }

    private class WificondDeathHandlerInternal implements WificondDeathEventHandler {
        private WificondDeathHandlerInternal() {
        }

        @Override
        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "wificond died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumWificondCrashes();
            }
        }
    }

    private class SupplicantDeathHandlerInternal implements SupplicantDeathEventHandler {
        private SupplicantDeathHandlerInternal() {
        }

        @Override
        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "wpa_supplicant died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumSupplicantCrashes();
            }
        }
    }

    private class HostapdDeathHandlerInternal implements HostapdDeathEventHandler {
        private HostapdDeathHandlerInternal() {
        }

        @Override
        public void onDeath() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "hostapd died. Cleaning up internal state.");
                WifiNative.this.onNativeDaemonDeath();
                WifiNative.this.mWifiMetrics.incrementNumHostapdCrashes();
            }
        }
    }

    private void onInterfaceStateChanged(Iface iface, boolean z) {
        synchronized (this.mLock) {
            if (z == iface.isUp) {
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Interface status unchanged on " + iface + " from " + z + ", Ignoring...");
                }
                return;
            }
            Log.i(TAG, "Interface state changed on " + iface + ", isUp=" + z);
            if (z) {
                iface.externalListener.onUp(iface.name);
            } else {
                iface.externalListener.onDown(iface.name);
                if (iface.type == 1) {
                    this.mWifiMetrics.incrementNumClientInterfaceDown();
                } else if (iface.type == 0) {
                    this.mWifiMetrics.incrementNumSoftApInterfaceDown();
                }
            }
            iface.isUp = z;
        }
    }

    private class NetworkObserverInternal extends BaseNetworkObserver {
        private final int mInterfaceId;

        NetworkObserverInternal(int i) {
            this.mInterfaceId = i;
        }

        public void interfaceLinkStateChanged(String str, boolean z) {
            synchronized (WifiNative.this.mLock) {
                Iface iface = WifiNative.this.mIfaceMgr.getIface(this.mInterfaceId);
                if (iface == null) {
                    if (WifiNative.this.mVerboseLoggingEnabled) {
                        Log.v(WifiNative.TAG, "Received iface link up/down notification on an invalid iface=" + this.mInterfaceId);
                    }
                    return;
                }
                Iface iface2 = WifiNative.this.mIfaceMgr.getIface(str);
                if (iface2 != null && iface2 == iface) {
                    WifiNative.this.onInterfaceStateChanged(iface2, WifiNative.this.isInterfaceUp(str));
                    return;
                }
                if (WifiNative.this.mVerboseLoggingEnabled) {
                    Log.v(WifiNative.TAG, "Received iface link up/down notification on an invalid iface=" + str);
                }
            }
        }
    }

    private class VendorHalRadioModeChangeHandlerInternal implements VendorHalRadioModeChangeEventHandler {
        private VendorHalRadioModeChangeHandlerInternal() {
        }

        @Override
        public void onMcc(int i) {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in MCC mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToMcc();
            }
        }

        @Override
        public void onScc(int i) {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in SCC mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToScc();
            }
        }

        @Override
        public void onSbs(int i) {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in SBS mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToSbs();
            }
        }

        @Override
        public void onDbs() {
            synchronized (WifiNative.this.mLock) {
                Log.i(WifiNative.TAG, "Device is in DBS mode now");
                WifiNative.this.mWifiMetrics.incrementNumRadioModeChangeToDbs();
            }
        }
    }

    private String handleIfaceCreationWhenVendorHalNotSupported(Iface iface) {
        String string;
        synchronized (this.mLock) {
            Iface ifaceRemoveExistingIface = this.mIfaceMgr.removeExistingIface(iface.id);
            if (ifaceRemoveExistingIface != null) {
                onInterfaceDestroyed(ifaceRemoveExistingIface);
                Log.i(TAG, "Successfully torn down " + ifaceRemoveExistingIface);
            }
            string = this.mPropertyService.getString("wifi.interface", "wlan0");
        }
        return string;
    }

    private String createStaIface(Iface iface, boolean z) {
        synchronized (this.mLock) {
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                return this.mWifiVendorHal.createStaIface(z, new InterfaceDestoyedListenerInternal(iface.id));
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring createStaIface.");
            return handleIfaceCreationWhenVendorHalNotSupported(iface);
        }
    }

    private String createApIface(Iface iface) {
        synchronized (this.mLock) {
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                return this.mWifiVendorHal.createApIface(new InterfaceDestoyedListenerInternal(iface.id));
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring createApIface.");
            return handleIfaceCreationWhenVendorHalNotSupported(iface);
        }
    }

    private boolean handleIfaceRemovalWhenVendorHalNotSupported(Iface iface) {
        synchronized (this.mLock) {
            this.mIfaceMgr.removeIface(iface.id);
            onInterfaceDestroyed(iface);
            Log.i(TAG, "Successfully torn down " + iface);
        }
        return true;
    }

    private boolean removeStaIface(Iface iface) {
        synchronized (this.mLock) {
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                return this.mWifiVendorHal.removeStaIface(iface.name);
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring removeStaIface.");
            return handleIfaceRemovalWhenVendorHalNotSupported(iface);
        }
    }

    private boolean removeApIface(Iface iface) {
        synchronized (this.mLock) {
            if (this.mWifiVendorHal.isVendorHalSupported()) {
                return this.mWifiVendorHal.removeApIface(iface.name);
            }
            Log.i(TAG, "Vendor Hal not supported, ignoring removeApIface.");
            return handleIfaceRemovalWhenVendorHalNotSupported(iface);
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (!this.mWifiVendorHal.initialize(new VendorHalDeathHandlerInternal())) {
                Log.e(TAG, "Failed to initialize vendor HAL");
                return false;
            }
            if (!this.mWificondControl.initialize(new WificondDeathHandlerInternal())) {
                Log.e(TAG, "Failed to initialize wificond");
                return false;
            }
            this.mWifiVendorHal.registerRadioModeChangeHandler(new VendorHalRadioModeChangeHandlerInternal());
            return true;
        }
    }

    public void registerStatusListener(StatusListener statusListener) {
        this.mStatusListeners.add(statusListener);
    }

    private void initializeNwParamsForClientInterface(String str) {
        try {
            this.mNwManagementService.clearInterfaceAddresses(str);
            this.mNwManagementService.setInterfaceIpv6PrivacyExtensions(str, true);
            this.mNwManagementService.disableIpv6(str);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to change interface settings: " + e);
        } catch (IllegalStateException e2) {
            Log.e(TAG, "Unable to change interface settings: " + e2);
        }
    }

    public String setupInterfaceForClientMode(boolean z, InterfaceCallback interfaceCallback) {
        synchronized (this.mLock) {
            if (!startHal()) {
                Log.e(TAG, "Failed to start Hal");
                this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToHal();
                return null;
            }
            if (startSupplicant()) {
                Iface ifaceAllocateIface = this.mIfaceMgr.allocateIface(1);
                if (ifaceAllocateIface == null) {
                    Log.e(TAG, "Failed to allocate new STA iface");
                    return null;
                }
                ifaceAllocateIface.externalListener = interfaceCallback;
                ifaceAllocateIface.name = createStaIface(ifaceAllocateIface, z);
                if (TextUtils.isEmpty(ifaceAllocateIface.name)) {
                    Log.e(TAG, "Failed to create STA iface in vendor HAL");
                    this.mIfaceMgr.removeIface(ifaceAllocateIface.id);
                    this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToHal();
                    return null;
                }
                if (this.mWificondControl.setupInterfaceForClientMode(ifaceAllocateIface.name) == null) {
                    Log.e(TAG, "Failed to setup iface in wificond on " + ifaceAllocateIface);
                    teardownInterface(ifaceAllocateIface.name);
                    this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToWificond();
                    return null;
                }
                if (!this.mSupplicantStaIfaceHal.setupIface(ifaceAllocateIface.name)) {
                    Log.e(TAG, "Failed to setup iface in supplicant on " + ifaceAllocateIface);
                    teardownInterface(ifaceAllocateIface.name);
                    this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToSupplicant();
                    return null;
                }
                ifaceAllocateIface.networkObserver = new NetworkObserverInternal(ifaceAllocateIface.id);
                if (!registerNetworkObserver(ifaceAllocateIface.networkObserver)) {
                    Log.e(TAG, "Failed to register network observer on " + ifaceAllocateIface);
                    teardownInterface(ifaceAllocateIface.name);
                    return null;
                }
                this.mWifiMonitor.startMonitoring(ifaceAllocateIface.name);
                onInterfaceStateChanged(ifaceAllocateIface, isInterfaceUp(ifaceAllocateIface.name));
                initializeNwParamsForClientInterface(ifaceAllocateIface.name);
                Log.i(TAG, "Successfully setup " + ifaceAllocateIface);
                return ifaceAllocateIface.name;
            }
            Log.e(TAG, "Failed to start supplicant");
            this.mWifiMetrics.incrementNumSetupClientInterfaceFailureDueToSupplicant();
            return null;
        }
    }

    public String setupInterfaceForSoftApMode(InterfaceCallback interfaceCallback) {
        synchronized (this.mLock) {
            if (startHal()) {
                Iface ifaceAllocateIface = this.mIfaceMgr.allocateIface(0);
                if (ifaceAllocateIface == null) {
                    Log.e(TAG, "Failed to allocate new AP iface");
                    return null;
                }
                ifaceAllocateIface.externalListener = interfaceCallback;
                ifaceAllocateIface.name = createApIface(ifaceAllocateIface);
                if (TextUtils.isEmpty(ifaceAllocateIface.name)) {
                    Log.e(TAG, "Failed to create AP iface in vendor HAL");
                    this.mIfaceMgr.removeIface(ifaceAllocateIface.id);
                    this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHal();
                    return null;
                }
                if (this.mWificondControl.setupInterfaceForSoftApMode(ifaceAllocateIface.name) == null) {
                    Log.e(TAG, "Failed to setup iface in wificond on " + ifaceAllocateIface);
                    teardownInterface(ifaceAllocateIface.name);
                    this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToWificond();
                    return null;
                }
                ifaceAllocateIface.networkObserver = new NetworkObserverInternal(ifaceAllocateIface.id);
                if (!registerNetworkObserver(ifaceAllocateIface.networkObserver)) {
                    Log.e(TAG, "Failed to register network observer on " + ifaceAllocateIface);
                    teardownInterface(ifaceAllocateIface.name);
                    return null;
                }
                onInterfaceStateChanged(ifaceAllocateIface, isInterfaceUp(ifaceAllocateIface.name));
                Log.i(TAG, "Successfully setup " + ifaceAllocateIface);
                return ifaceAllocateIface.name;
            }
            Log.e(TAG, "Failed to start Hal");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHal();
            return null;
        }
    }

    public boolean isInterfaceUp(String str) {
        InterfaceConfiguration interfaceConfig;
        synchronized (this.mLock) {
            if (this.mIfaceMgr.getIface(str) == null) {
                Log.e(TAG, "Trying to get iface state on invalid iface=" + str);
                return false;
            }
            try {
                interfaceConfig = this.mNwManagementService.getInterfaceConfig(str);
            } catch (RemoteException e) {
                interfaceConfig = null;
            }
            if (interfaceConfig == null) {
                return false;
            }
            return interfaceConfig.isUp();
        }
    }

    public void teardownInterface(String str) {
        synchronized (this.mLock) {
            Iface iface = this.mIfaceMgr.getIface(str);
            if (iface == null) {
                Log.e(TAG, "Trying to teardown an invalid iface=" + str);
                return;
            }
            if (iface.type == 1) {
                if (!removeStaIface(iface)) {
                    Log.e(TAG, "Failed to remove iface in vendor HAL=" + str);
                    return;
                }
            } else if (iface.type == 0 && !removeApIface(iface)) {
                Log.e(TAG, "Failed to remove iface in vendor HAL=" + str);
                return;
            }
            Log.i(TAG, "Successfully initiated teardown for iface=" + str);
        }
    }

    public void teardownAllInterfaces() {
        synchronized (this.mLock) {
            Iterator ifaceIdIter = this.mIfaceMgr.getIfaceIdIter();
            while (ifaceIdIter.hasNext()) {
                Iface iface = this.mIfaceMgr.getIface(((Integer) ifaceIdIter.next()).intValue());
                ifaceIdIter.remove();
                onInterfaceDestroyed(iface);
                Log.i(TAG, "Successfully torn down " + iface);
            }
            Log.i(TAG, "Successfully torn down all ifaces");
        }
    }

    public String getClientInterfaceName() {
        String strFindAnyStaIfaceName;
        synchronized (this.mLock) {
            strFindAnyStaIfaceName = this.mIfaceMgr.findAnyStaIfaceName();
        }
        return strFindAnyStaIfaceName;
    }

    public String getSoftApInterfaceName() {
        String strFindAnyApIfaceName;
        synchronized (this.mLock) {
            strFindAnyApIfaceName = this.mIfaceMgr.findAnyApIfaceName();
        }
        return strFindAnyApIfaceName;
    }

    public SignalPollResult signalPoll(String str) {
        return this.mWificondControl.signalPoll(str);
    }

    public TxPacketCounters getTxPacketCounters(String str) {
        return this.mWificondControl.getTxPacketCounters(str);
    }

    public int[] getChannelsForBand(int i) {
        return this.mWificondControl.getChannelsForBand(i);
    }

    public boolean scan(String str, int i, Set<Integer> set, List<String> list) {
        return this.mWificondControl.scan(str, i, set, list);
    }

    public ArrayList<ScanDetail> getScanResults(String str) {
        return this.mWificondControl.getScanResults(str, 0);
    }

    public ArrayList<ScanDetail> getPnoScanResults(String str) {
        return this.mWificondControl.getScanResults(str, 1);
    }

    public boolean startPnoScan(String str, PnoSettings pnoSettings) {
        return this.mWificondControl.startPnoScan(str, pnoSettings);
    }

    public boolean stopPnoScan(String str) {
        return this.mWificondControl.stopPnoScan(str);
    }

    private boolean waitForHostapdConnection() {
        boolean zIsInitializationComplete = false;
        if (!this.mHostapdHal.isInitializationStarted() && !this.mHostapdHal.initialize()) {
            return false;
        }
        int i = 0;
        while (!zIsInitializationComplete) {
            int i2 = i + 1;
            if (i >= 50 || (zIsInitializationComplete = this.mHostapdHal.isInitializationComplete())) {
                break;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
            i = i2;
        }
        return zIsInitializationComplete;
    }

    public boolean startSoftAp(String str, WifiConfiguration wifiConfiguration, SoftApListener softApListener) {
        if (!this.mWificondControl.startHostapd(str, softApListener)) {
            Log.e(TAG, "Failed to start hostapd");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        }
        if (!waitForHostapdConnection()) {
            Log.e(TAG, "Failed to establish connection to hostapd");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        }
        if (!this.mHostapdHal.registerDeathHandler(new HostapdDeathHandlerInternal())) {
            Log.e(TAG, "Failed to register hostapd death handler");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        }
        if (!MtkHostapdHal.addAccessPoint(str, wifiConfiguration)) {
            Log.e(TAG, "Failed to add acccess point");
            this.mWifiMetrics.incrementNumSetupSoftApInterfaceFailureDueToHostapd();
            return false;
        }
        return true;
    }

    public boolean stopSoftAp(String str) {
        if (!this.mHostapdHal.removeAccessPoint(str)) {
            Log.e(TAG, "Failed to remove access point");
        }
        return this.mWificondControl.stopHostapd(str);
    }

    public boolean setMacAddress(String str, MacAddress macAddress) {
        return this.mWifiVendorHal.setMacAddress(str, macAddress);
    }

    public void setSupplicantLogLevel(boolean z) {
        this.mSupplicantStaIfaceHal.setLogLevel(z);
    }

    public boolean reconnect(String str) {
        return this.mSupplicantStaIfaceHal.reconnect(str);
    }

    public boolean reassociate(String str) {
        return this.mSupplicantStaIfaceHal.reassociate(str);
    }

    public boolean disconnect(String str) {
        return this.mSupplicantStaIfaceHal.disconnect(str);
    }

    public String getMacAddress(String str) {
        return this.mSupplicantStaIfaceHal.getMacAddress(str);
    }

    public boolean startFilteringMulticastV4Packets(String str) {
        return this.mSupplicantStaIfaceHal.stopRxFilter(str) && this.mSupplicantStaIfaceHal.removeRxFilter(str, 0) && this.mSupplicantStaIfaceHal.startRxFilter(str);
    }

    public boolean stopFilteringMulticastV4Packets(String str) {
        return this.mSupplicantStaIfaceHal.stopRxFilter(str) && this.mSupplicantStaIfaceHal.addRxFilter(str, 0) && this.mSupplicantStaIfaceHal.startRxFilter(str);
    }

    public boolean startFilteringMulticastV6Packets(String str) {
        return this.mSupplicantStaIfaceHal.stopRxFilter(str) && this.mSupplicantStaIfaceHal.removeRxFilter(str, 1) && this.mSupplicantStaIfaceHal.startRxFilter(str);
    }

    public boolean stopFilteringMulticastV6Packets(String str) {
        return this.mSupplicantStaIfaceHal.stopRxFilter(str) && this.mSupplicantStaIfaceHal.addRxFilter(str, 1) && this.mSupplicantStaIfaceHal.startRxFilter(str);
    }

    public boolean setBluetoothCoexistenceMode(String str, int i) {
        return this.mSupplicantStaIfaceHal.setBtCoexistenceMode(str, i);
    }

    public boolean setBluetoothCoexistenceScanMode(String str, boolean z) {
        return this.mSupplicantStaIfaceHal.setBtCoexistenceScanModeEnabled(str, z);
    }

    public boolean setSuspendOptimizations(String str, boolean z) {
        return this.mSupplicantStaIfaceHal.setSuspendModeEnabled(str, z);
    }

    public boolean setCountryCode(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setCountryCode(str, str2);
    }

    public void startTdls(String str, String str2, boolean z) {
        if (z) {
            this.mSupplicantStaIfaceHal.initiateTdlsDiscover(str, str2);
            this.mSupplicantStaIfaceHal.initiateTdlsSetup(str, str2);
        } else {
            this.mSupplicantStaIfaceHal.initiateTdlsTeardown(str, str2);
        }
    }

    public boolean startWpsPbc(String str, String str2) {
        return this.mSupplicantStaIfaceHal.startWpsPbc(str, str2);
    }

    public boolean startWpsPinKeypad(String str, String str2) {
        return this.mSupplicantStaIfaceHal.startWpsPinKeypad(str, str2);
    }

    public String startWpsPinDisplay(String str, String str2) {
        return this.mSupplicantStaIfaceHal.startWpsPinDisplay(str, str2);
    }

    public boolean setExternalSim(String str, boolean z) {
        return this.mSupplicantStaIfaceHal.setExternalSim(str, z);
    }

    public boolean simAuthResponse(String str, int i, String str2, String str3) {
        if (SIM_AUTH_RESP_TYPE_GSM_AUTH.equals(str2)) {
            return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimGsmAuthResponse(str, str3);
        }
        if (SIM_AUTH_RESP_TYPE_UMTS_AUTH.equals(str2)) {
            return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimUmtsAuthResponse(str, str3);
        }
        if (SIM_AUTH_RESP_TYPE_UMTS_AUTS.equals(str2)) {
            return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimUmtsAutsResponse(str, str3);
        }
        return false;
    }

    public boolean simAuthFailedResponse(String str, int i) {
        return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimGsmAuthFailure(str);
    }

    public boolean umtsAuthFailedResponse(String str, int i) {
        return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapSimUmtsAuthFailure(str);
    }

    public boolean simIdentityResponse(String str, int i, String str2, String str3) {
        return this.mSupplicantStaIfaceHal.sendCurrentNetworkEapIdentityResponse(str, str2, str3);
    }

    public String getEapAnonymousIdentity(String str) {
        return this.mSupplicantStaIfaceHal.getCurrentNetworkEapAnonymousIdentity(str);
    }

    public boolean startWpsRegistrar(String str, String str2, String str3) {
        return this.mSupplicantStaIfaceHal.startWpsRegistrar(str, str2, str3);
    }

    public boolean cancelWps(String str) {
        return this.mSupplicantStaIfaceHal.cancelWps(str);
    }

    public boolean setDeviceName(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsDeviceName(str, str2);
    }

    public boolean setDeviceType(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsDeviceType(str, str2);
    }

    public boolean setConfigMethods(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsConfigMethods(str, str2);
    }

    public boolean setManufacturer(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsManufacturer(str, str2);
    }

    public boolean setModelName(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsModelName(str, str2);
    }

    public boolean setModelNumber(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsModelNumber(str, str2);
    }

    public boolean setSerialNumber(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setWpsSerialNumber(str, str2);
    }

    public void setPowerSave(String str, boolean z) {
        this.mSupplicantStaIfaceHal.setPowerSave(str, z);
        MtkWifiApmDelegate.getInstance().broadcastPowerSaveModeChanged(z);
    }

    public boolean setConcurrencyPriority(boolean z) {
        return this.mSupplicantStaIfaceHal.setConcurrencyPriority(z);
    }

    public boolean enableStaAutoReconnect(String str, boolean z) {
        return this.mSupplicantStaIfaceHal.enableAutoReconnect(str, z);
    }

    public boolean migrateNetworksFromSupplicant(String str, Map<String, WifiConfiguration> map, SparseArray<Map<String, String>> sparseArray) {
        return this.mSupplicantStaIfaceHal.loadNetworks(str, map, sparseArray);
    }

    public boolean connectToNetwork(String str, WifiConfiguration wifiConfiguration) {
        this.mWificondControl.abortScan(str);
        return this.mSupplicantStaIfaceHal.connectToNetwork(str, wifiConfiguration);
    }

    public boolean roamToNetwork(String str, WifiConfiguration wifiConfiguration) {
        this.mWificondControl.abortScan(str);
        return this.mSupplicantStaIfaceHal.roamToNetwork(str, wifiConfiguration);
    }

    public boolean removeAllNetworks(String str) {
        return this.mSupplicantStaIfaceHal.removeAllNetworks(str);
    }

    public boolean setConfiguredNetworkBSSID(String str, String str2) {
        return this.mSupplicantStaIfaceHal.setCurrentNetworkBssid(str, str2);
    }

    public boolean requestAnqp(String str, String str2, Set<Integer> set, Set<Integer> set2) {
        if (str2 == null || ((set == null || set.isEmpty()) && (set2 == null || set2.isEmpty()))) {
            Log.e(TAG, "Invalid arguments for ANQP request.");
            return false;
        }
        ArrayList<Short> arrayList = new ArrayList<>();
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            arrayList.add(Short.valueOf(it.next().shortValue()));
        }
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        arrayList2.addAll(set2);
        return this.mSupplicantStaIfaceHal.initiateAnqpQuery(str, str2, arrayList, arrayList2);
    }

    public boolean requestIcon(String str, String str2, String str3) {
        if (str2 == null || str3 == null) {
            Log.e(TAG, "Invalid arguments for Icon request.");
            return false;
        }
        return this.mSupplicantStaIfaceHal.initiateHs20IconQuery(str, str2, str3);
    }

    public String getCurrentNetworkWpsNfcConfigurationToken(String str) {
        return this.mSupplicantStaIfaceHal.getCurrentNetworkWpsNfcConfigurationToken(str);
    }

    public void removeNetworkIfCurrent(String str, int i) {
        this.mSupplicantStaIfaceHal.removeNetworkIfCurrent(str, i);
    }

    public boolean isHalStarted() {
        return this.mWifiVendorHal.isHalStarted();
    }

    public boolean getBgScanCapabilities(String str, ScanCapabilities scanCapabilities) {
        return this.mWifiVendorHal.getBgScanCapabilities(str, scanCapabilities);
    }

    public static class HiddenNetwork {
        public String ssid;

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return Objects.equals(this.ssid, ((HiddenNetwork) obj).ssid);
        }

        public int hashCode() {
            if (this.ssid == null) {
                return 0;
            }
            return this.ssid.hashCode();
        }
    }

    public static class PnoNetwork {
        public byte auth_bit_field;
        public byte flags;
        public String ssid;

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            PnoNetwork pnoNetwork = (PnoNetwork) obj;
            if (Objects.equals(this.ssid, pnoNetwork.ssid) && this.flags == pnoNetwork.flags && this.auth_bit_field == pnoNetwork.auth_bit_field) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (this.ssid == null ? 0 : this.ssid.hashCode()) ^ ((this.flags * 31) + (this.auth_bit_field << 8));
        }
    }

    public boolean startBgScan(String str, ScanSettings scanSettings, ScanEventHandler scanEventHandler) {
        return this.mWifiVendorHal.startBgScan(str, scanSettings, scanEventHandler);
    }

    public void stopBgScan(String str) {
        this.mWifiVendorHal.stopBgScan(str);
    }

    public void pauseBgScan(String str) {
        this.mWifiVendorHal.pauseBgScan(str);
    }

    public void restartBgScan(String str) {
        this.mWifiVendorHal.restartBgScan(str);
    }

    public WifiScanner.ScanData[] getBgScanResults(String str) {
        return this.mWifiVendorHal.getBgScanResults(str);
    }

    public WifiLinkLayerStats getWifiLinkLayerStats(String str) {
        return this.mWifiVendorHal.getWifiLinkLayerStats(str);
    }

    public int getSupportedFeatureSet(String str) {
        return this.mWifiVendorHal.getSupportedFeatureSet(str);
    }

    public boolean requestRtt(RttManager.RttParams[] rttParamsArr, RttEventHandler rttEventHandler) {
        return this.mWifiVendorHal.requestRtt(rttParamsArr, rttEventHandler);
    }

    public boolean cancelRtt(RttManager.RttParams[] rttParamsArr) {
        return this.mWifiVendorHal.cancelRtt(rttParamsArr);
    }

    public RttManager.ResponderConfig enableRttResponder(int i) {
        return this.mWifiVendorHal.enableRttResponder(i);
    }

    public boolean disableRttResponder() {
        return this.mWifiVendorHal.disableRttResponder();
    }

    public boolean setScanningMacOui(String str, byte[] bArr) {
        return this.mWifiVendorHal.setScanningMacOui(str, bArr);
    }

    public RttManager.RttCapabilities getRttCapabilities() {
        return this.mWifiVendorHal.getRttCapabilities();
    }

    public ApfCapabilities getApfCapabilities(String str) {
        return this.mWifiVendorHal.getApfCapabilities(str);
    }

    public boolean installPacketFilter(String str, byte[] bArr) {
        return this.mWifiVendorHal.installPacketFilter(str, bArr);
    }

    public byte[] readPacketFilter(String str) {
        return this.mWifiVendorHal.readPacketFilter(str);
    }

    public boolean setCountryCodeHal(String str, String str2) {
        return this.mWifiVendorHal.setCountryCodeHal(str, str2);
    }

    public boolean setLoggingEventHandler(WifiLoggerEventHandler wifiLoggerEventHandler) {
        return this.mWifiVendorHal.setLoggingEventHandler(wifiLoggerEventHandler);
    }

    public boolean startLoggingRingBuffer(int i, int i2, int i3, int i4, String str) {
        return this.mWifiVendorHal.startLoggingRingBuffer(i, i2, i3, i4, str);
    }

    public int getSupportedLoggerFeatureSet() {
        return this.mWifiVendorHal.getSupportedLoggerFeatureSet();
    }

    public boolean resetLogHandler() {
        return this.mWifiVendorHal.resetLogHandler();
    }

    public String getDriverVersion() {
        return this.mWifiVendorHal.getDriverVersion();
    }

    public String getFirmwareVersion() {
        return this.mWifiVendorHal.getFirmwareVersion();
    }

    public static class RingBufferStatus {
        public static final int HAS_ASCII_ENTRIES = 2;
        public static final int HAS_BINARY_ENTRIES = 1;
        public static final int HAS_PER_PACKET_ENTRIES = 4;
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
        return this.mWifiVendorHal.getRingBufferStatus();
    }

    public boolean getRingBufferData(String str) {
        return this.mWifiVendorHal.getRingBufferData(str);
    }

    public byte[] getFwMemoryDump() {
        return this.mWifiVendorHal.getFwMemoryDump();
    }

    public byte[] getDriverStateDump() {
        return this.mWifiVendorHal.getDriverStateDump();
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

        FateReport(byte b, long j, byte b2, byte[] bArr) {
            this.mFate = b;
            this.mDriverTimestampUSec = j;
            this.mEstimatedWallclockMSec = convertDriverTimestampUSecToWallclockMSec(this.mDriverTimestampUSec);
            this.mFrameType = b2;
            this.mFrameBytes = bArr;
        }

        public String toTableRowString() {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            FrameParser frameParser = new FrameParser(this.mFrameType, this.mFrameBytes);
            dateFormatter.setTimeZone(TimeZone.getDefault());
            printWriter.format("%-15s  %12s  %-9s  %-32s  %-12s  %-23s  %s\n", Long.valueOf(this.mDriverTimestampUSec), dateFormatter.format(new Date(this.mEstimatedWallclockMSec)), directionToString(), fateToString(), frameParser.mMostSpecificProtocolString, frameParser.mTypeString, frameParser.mResultString);
            return stringWriter.toString();
        }

        public String toVerboseStringWithPiiAllowed() {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            FrameParser frameParser = new FrameParser(this.mFrameType, this.mFrameBytes);
            printWriter.format("Frame direction: %s\n", directionToString());
            printWriter.format("Frame timestamp: %d\n", Long.valueOf(this.mDriverTimestampUSec));
            printWriter.format("Frame fate: %s\n", fateToString());
            printWriter.format("Frame type: %s\n", frameTypeToString(this.mFrameType));
            printWriter.format("Frame protocol: %s\n", frameParser.mMostSpecificProtocolString);
            printWriter.format("Frame protocol type: %s\n", frameParser.mTypeString);
            printWriter.format("Frame length: %d\n", Integer.valueOf(this.mFrameBytes.length));
            printWriter.append((CharSequence) "Frame bytes");
            printWriter.append((CharSequence) HexDump.dumpHexString(this.mFrameBytes));
            printWriter.append((CharSequence) "\n");
            return stringWriter.toString();
        }

        public static String getTableHeader() {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.format("\n%-15s  %-12s  %-9s  %-32s  %-12s  %-23s  %s\n", "Time usec", "Walltime", "Direction", "Fate", "Protocol", "Type", "Result");
            printWriter.format("%-15s  %-12s  %-9s  %-32s  %-12s  %-23s  %s\n", "---------", "--------", "---------", "----", "--------", "----", "------");
            return stringWriter.toString();
        }

        private static String frameTypeToString(byte b) {
            switch (b) {
                case 0:
                    return "unknown";
                case 1:
                    return "data";
                case 2:
                    return "802.11 management";
                default:
                    return Byte.toString(b);
            }
        }

        private static long convertDriverTimestampUSecToWallclockMSec(long j) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            long j2 = j / 1000;
            long jElapsedRealtime = SystemClock.elapsedRealtime() % 4294967;
            if (jElapsedRealtime < j2) {
                jElapsedRealtime += 4294967;
            }
            return jCurrentTimeMillis - (jElapsedRealtime - j2);
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

        TxFateReport(byte b, long j, byte b2, byte[] bArr) {
            super(b, j, b2, bArr);
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
                    return "driver dropped (no bufs)";
                case 9:
                    return "driver dropped (other)";
                default:
                    return Byte.toString(this.mFate);
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

        RxFateReport(byte b, long j, byte b2, byte[] bArr) {
            super(b, j, b2, bArr);
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

    public boolean startPktFateMonitoring(String str) {
        return this.mWifiVendorHal.startPktFateMonitoring(str);
    }

    public boolean getTxPktFates(String str, TxFateReport[] txFateReportArr) {
        return this.mWifiVendorHal.getTxPktFates(str, txFateReportArr);
    }

    public boolean getRxPktFates(String str, RxFateReport[] rxFateReportArr) {
        return this.mWifiVendorHal.getRxPktFates(str, rxFateReportArr);
    }

    public long getTxPackets(String str) {
        return TrafficStats.getTxPackets(str);
    }

    public long getRxPackets(String str) {
        return TrafficStats.getRxPackets(str);
    }

    public int startSendingOffloadedPacket(String str, int i, byte[] bArr, byte[] bArr2, int i2, int i3) {
        return this.mWifiVendorHal.startSendingOffloadedPacket(str, i, NativeUtil.macAddressToByteArray(getMacAddress(str)), bArr, bArr2, i2, i3);
    }

    public int stopSendingOffloadedPacket(String str, int i) {
        return this.mWifiVendorHal.stopSendingOffloadedPacket(str, i);
    }

    public int startRssiMonitoring(String str, byte b, byte b2, WifiRssiEventHandler wifiRssiEventHandler) {
        return this.mWifiVendorHal.startRssiMonitoring(str, b, b2, wifiRssiEventHandler);
    }

    public int stopRssiMonitoring(String str) {
        return this.mWifiVendorHal.stopRssiMonitoring(str);
    }

    public WifiWakeReasonAndCounts getWlanWakeReasonCount() {
        return this.mWifiVendorHal.getWlanWakeReasonCount();
    }

    public boolean configureNeighborDiscoveryOffload(String str, boolean z) {
        return this.mWifiVendorHal.configureNeighborDiscoveryOffload(str, z);
    }

    public boolean getRoamingCapabilities(String str, RoamingCapabilities roamingCapabilities) {
        return this.mWifiVendorHal.getRoamingCapabilities(str, roamingCapabilities);
    }

    public int enableFirmwareRoaming(String str, int i) {
        return this.mWifiVendorHal.enableFirmwareRoaming(str, i);
    }

    public boolean configureRoaming(String str, RoamingConfig roamingConfig) {
        return this.mWifiVendorHal.configureRoaming(str, roamingConfig);
    }

    public boolean resetRoamingConfiguration(String str) {
        return this.mWifiVendorHal.configureRoaming(str, new RoamingConfig());
    }

    public boolean selectTxPowerScenario(int i) {
        return this.mWifiVendorHal.selectTxPowerScenario(i);
    }

    static {
        System.loadLibrary("wifi-service");
        registerNatives();
    }

    public synchronized String readKernelLog() {
        byte[] kernelLogNative = readKernelLogNative();
        if (kernelLogNative != null) {
            try {
                return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(kernelLogNative)).toString();
            } catch (CharacterCodingException e) {
                return new String(kernelLogNative, StandardCharsets.ISO_8859_1);
            }
        }
        return "*** failed to read kernel log ***";
    }
}
