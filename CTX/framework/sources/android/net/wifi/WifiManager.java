package android.net.wifi;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.ISoftApCallback;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.ProvisioningCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.server.net.NetworkPinner;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import mediatek.net.wifi.WifiHotspotManager;

public class WifiManager {
    public static final String ACTION_PASSPOINT_DEAUTH_IMMINENT = "android.net.wifi.action.PASSPOINT_DEAUTH_IMMINENT";
    public static final String ACTION_PASSPOINT_ICON = "android.net.wifi.action.PASSPOINT_ICON";
    public static final String ACTION_PASSPOINT_OSU_PROVIDERS_LIST = "android.net.wifi.action.PASSPOINT_OSU_PROVIDERS_LIST";
    public static final String ACTION_PASSPOINT_SUBSCRIPTION_REMEDIATION = "android.net.wifi.action.PASSPOINT_SUBSCRIPTION_REMEDIATION";
    public static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
    public static final String ACTION_REQUEST_DISABLE = "android.net.wifi.action.REQUEST_DISABLE";
    public static final String ACTION_REQUEST_ENABLE = "android.net.wifi.action.REQUEST_ENABLE";
    public static final String ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE = "android.net.wifi.action.REQUEST_SCAN_ALWAYS_AVAILABLE";
    private static final int BASE = 151552;

    @Deprecated
    public static final String BATCHED_SCAN_RESULTS_AVAILABLE_ACTION = "android.net.wifi.BATCHED_RESULTS";
    public static final int BUSY = 2;
    public static final int CANCEL_WPS = 151566;
    public static final int CANCEL_WPS_FAILED = 151567;
    public static final int CANCEL_WPS_SUCCEDED = 151568;

    @SystemApi
    public static final int CHANGE_REASON_ADDED = 0;

    @SystemApi
    public static final int CHANGE_REASON_CONFIG_CHANGE = 2;

    @SystemApi
    public static final int CHANGE_REASON_REMOVED = 1;

    @SystemApi
    public static final String CONFIGURED_NETWORKS_CHANGED_ACTION = "android.net.wifi.CONFIGURED_NETWORKS_CHANGE";
    public static final int CONNECT_NETWORK = 151553;
    public static final int CONNECT_NETWORK_FAILED = 151554;
    public static final int CONNECT_NETWORK_SUCCEEDED = 151555;
    public static final int DATA_ACTIVITY_IN = 1;
    public static final int DATA_ACTIVITY_INOUT = 3;
    public static final int DATA_ACTIVITY_NONE = 0;
    public static final int DATA_ACTIVITY_NOTIFICATION = 1;
    public static final int DATA_ACTIVITY_OUT = 2;
    public static final boolean DEFAULT_POOR_NETWORK_AVOIDANCE_ENABLED = false;
    public static final int DISABLE_NETWORK = 151569;
    public static final int DISABLE_NETWORK_FAILED = 151570;
    public static final int DISABLE_NETWORK_SUCCEEDED = 151571;
    public static final int ERROR = 0;

    @Deprecated
    public static final int ERROR_AUTHENTICATING = 1;

    @Deprecated
    public static final int ERROR_AUTH_FAILURE_EAP_FAILURE = 3;

    @Deprecated
    public static final int ERROR_AUTH_FAILURE_NONE = 0;

    @Deprecated
    public static final int ERROR_AUTH_FAILURE_TIMEOUT = 1;

    @Deprecated
    public static final int ERROR_AUTH_FAILURE_WRONG_PSWD = 2;
    public static final String EXTRA_ANQP_ELEMENT_DATA = "android.net.wifi.extra.ANQP_ELEMENT_DATA";

    @Deprecated
    public static final String EXTRA_BSSID = "bssid";
    public static final String EXTRA_BSSID_LONG = "android.net.wifi.extra.BSSID_LONG";

    @SystemApi
    public static final String EXTRA_CHANGE_REASON = "changeReason";
    public static final String EXTRA_DELAY = "android.net.wifi.extra.DELAY";
    public static final String EXTRA_ESS = "android.net.wifi.extra.ESS";
    public static final String EXTRA_FILENAME = "android.net.wifi.extra.FILENAME";
    public static final String EXTRA_ICON = "android.net.wifi.extra.ICON";
    public static final String EXTRA_LINK_PROPERTIES = "linkProperties";

    @SystemApi
    public static final String EXTRA_MULTIPLE_NETWORKS_CHANGED = "multipleChanges";
    public static final String EXTRA_NETWORK_CAPABILITIES = "networkCapabilities";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_NEW_RSSI = "newRssi";

    @Deprecated
    public static final String EXTRA_NEW_STATE = "newState";

    @SystemApi
    public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";
    public static final String EXTRA_PREVIOUS_WIFI_STATE = "previous_wifi_state";
    public static final String EXTRA_RESULTS_UPDATED = "resultsUpdated";
    public static final String EXTRA_SCAN_AVAILABLE = "scan_enabled";
    public static final String EXTRA_SUBSCRIPTION_REMEDIATION_METHOD = "android.net.wifi.extra.SUBSCRIPTION_REMEDIATION_METHOD";

    @Deprecated
    public static final String EXTRA_SUPPLICANT_CONNECTED = "connected";

    @Deprecated
    public static final String EXTRA_SUPPLICANT_ERROR = "supplicantError";

    @Deprecated
    public static final String EXTRA_SUPPLICANT_ERROR_REASON = "supplicantErrorReason";
    public static final String EXTRA_URL = "android.net.wifi.extra.URL";
    public static final String EXTRA_WIFI_AP_FAILURE_REASON = "wifi_ap_error_code";
    public static final String EXTRA_WIFI_AP_INTERFACE_NAME = "wifi_ap_interface_name";
    public static final String EXTRA_WIFI_AP_MODE = "wifi_ap_mode";

    @SystemApi
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";

    @SystemApi
    public static final String EXTRA_WIFI_CONFIGURATION = "wifiConfiguration";

    @SystemApi
    public static final String EXTRA_WIFI_CREDENTIAL_EVENT_TYPE = "et";

    @SystemApi
    public static final String EXTRA_WIFI_CREDENTIAL_SSID = "ssid";

    @Deprecated
    public static final String EXTRA_WIFI_INFO = "wifiInfo";
    public static final String EXTRA_WIFI_STATE = "wifi_state";
    public static final int FORGET_NETWORK = 151556;
    public static final int FORGET_NETWORK_FAILED = 151557;
    public static final int FORGET_NETWORK_SUCCEEDED = 151558;
    public static final int HOTSPOT_FAILED = 2;
    public static final int HOTSPOT_OBSERVER_REGISTERED = 3;
    public static final int HOTSPOT_STARTED = 0;
    public static final int HOTSPOT_STOPPED = 1;
    public static final int IFACE_IP_MODE_CONFIGURATION_ERROR = 0;
    public static final int IFACE_IP_MODE_LOCAL_ONLY = 2;
    public static final int IFACE_IP_MODE_TETHERED = 1;
    public static final int IFACE_IP_MODE_UNSPECIFIED = -1;
    public static final int INVALID_ARGS = 8;
    private static final int INVALID_KEY = 0;
    public static final int IN_PROGRESS = 1;
    public static final String LINK_CONFIGURATION_CHANGED_ACTION = "android.net.wifi.LINK_CONFIGURATION_CHANGED";
    private static final int MAX_ACTIVE_LOCKS = 50;
    private static final int MAX_RSSI = -55;
    private static final int MIN_RSSI = -100;
    public static final String NETWORK_IDS_CHANGED_ACTION = "android.net.wifi.NETWORK_IDS_CHANGED";
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.wifi.STATE_CHANGE";
    public static final int NOT_AUTHORIZED = 9;
    public static final String RSSI_CHANGED_ACTION = "android.net.wifi.RSSI_CHANGED";
    public static final int RSSI_LEVELS = 5;
    public static final int RSSI_PKTCNT_FETCH = 151572;
    public static final int RSSI_PKTCNT_FETCH_FAILED = 151574;
    public static final int RSSI_PKTCNT_FETCH_SUCCEEDED = 151573;
    public static final int SAP_START_FAILURE_GENERAL = 0;
    public static final int SAP_START_FAILURE_NO_CHANNEL = 1;
    public static final int SAVE_NETWORK = 151559;
    public static final int SAVE_NETWORK_FAILED = 151560;
    public static final int SAVE_NETWORK_SUCCEEDED = 151561;
    public static final String SCAN_RESULTS_AVAILABLE_ACTION = "android.net.wifi.SCAN_RESULTS";
    public static final int SET_WIFI_NOT_RECONNECT_AND_SCAN = 151612;
    public static final int START_WPS = 151562;
    public static final int START_WPS_SUCCEEDED = 151563;

    @Deprecated
    public static final String SUPPLICANT_CONNECTION_CHANGE_ACTION = "android.net.wifi.supplicant.CONNECTION_CHANGE";

    @Deprecated
    public static final String SUPPLICANT_STATE_CHANGED_ACTION = "android.net.wifi.supplicant.STATE_CHANGE";
    private static final String TAG = "WifiManager";

    @SystemApi
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    @SystemApi
    public static final int WIFI_AP_STATE_DISABLED = 11;

    @SystemApi
    public static final int WIFI_AP_STATE_DISABLING = 10;

    @SystemApi
    public static final int WIFI_AP_STATE_ENABLED = 13;

    @SystemApi
    public static final int WIFI_AP_STATE_ENABLING = 12;

    @SystemApi
    public static final int WIFI_AP_STATE_FAILED = 14;

    @SystemApi
    public static final String WIFI_CREDENTIAL_CHANGED_ACTION = "android.net.wifi.WIFI_CREDENTIAL_CHANGED";

    @SystemApi
    public static final int WIFI_CREDENTIAL_FORGOT = 1;

    @SystemApi
    public static final int WIFI_CREDENTIAL_SAVED = 0;
    public static final int WIFI_FEATURE_ADDITIONAL_STA = 2048;
    public static final int WIFI_FEATURE_AP_STA = 32768;
    public static final int WIFI_FEATURE_AWARE = 64;
    public static final int WIFI_FEATURE_BATCH_SCAN = 512;
    public static final int WIFI_FEATURE_CONFIG_NDO = 2097152;
    public static final int WIFI_FEATURE_CONTROL_ROAMING = 8388608;
    public static final int WIFI_FEATURE_D2AP_RTT = 256;
    public static final int WIFI_FEATURE_D2D_RTT = 128;
    public static final int WIFI_FEATURE_EPR = 16384;
    public static final int WIFI_FEATURE_HAL_EPNO = 262144;
    public static final int WIFI_FEATURE_IE_WHITELIST = 16777216;
    public static final int WIFI_FEATURE_INFRA = 1;
    public static final int WIFI_FEATURE_INFRA_5G = 2;
    public static final int WIFI_FEATURE_LINK_LAYER_STATS = 65536;
    public static final int WIFI_FEATURE_LOGGER = 131072;
    public static final int WIFI_FEATURE_MKEEP_ALIVE = 1048576;
    public static final int WIFI_FEATURE_MOBILE_HOTSPOT = 16;
    public static final int WIFI_FEATURE_P2P = 8;
    public static final int WIFI_FEATURE_PASSPOINT = 4;
    public static final int WIFI_FEATURE_PNO = 1024;
    public static final int WIFI_FEATURE_RSSI_MONITOR = 524288;
    public static final int WIFI_FEATURE_SCANNER = 32;
    public static final int WIFI_FEATURE_SCAN_RAND = 33554432;
    public static final int WIFI_FEATURE_TDLS = 4096;
    public static final int WIFI_FEATURE_TDLS_OFFCHANNEL = 8192;
    public static final int WIFI_FEATURE_TRANSMIT_POWER = 4194304;
    public static final int WIFI_FEATURE_TX_POWER_LIMIT = 67108864;
    public static final int WIFI_FREQUENCY_BAND_2GHZ = 2;
    public static final int WIFI_FREQUENCY_BAND_5GHZ = 1;
    public static final int WIFI_FREQUENCY_BAND_AUTO = 0;
    public static final int WIFI_MODE_FULL = 1;
    public static final int WIFI_MODE_FULL_HIGH_PERF = 3;
    public static final int WIFI_MODE_NO_LOCKS_HELD = 0;
    public static final int WIFI_MODE_SCAN_ONLY = 2;
    public static final String WIFI_SCAN_AVAILABLE = "wifi_scan_available";
    public static final String WIFI_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_STATE_CHANGED";
    public static final int WIFI_STATE_DISABLED = 1;
    public static final int WIFI_STATE_DISABLING = 0;
    public static final int WIFI_STATE_ENABLED = 3;
    public static final int WIFI_STATE_ENABLING = 2;
    public static final int WIFI_STATE_UNKNOWN = 4;
    public static final int WPS_AUTH_FAILURE = 6;
    public static final int WPS_COMPLETED = 151565;
    public static final int WPS_FAILED = 151564;
    public static final int WPS_OVERLAP_ERROR = 3;
    public static final int WPS_TIMED_OUT = 7;
    public static final int WPS_TKIP_ONLY_PROHIBITED = 5;
    public static final int WPS_WEP_PROHIBITED = 4;
    private static final Object sServiceHandlerDispatchLock = new Object();
    private int mActiveLockCount;
    private AsyncChannel mAsyncChannel;
    private CountDownLatch mConnected;
    private Context mContext;

    @GuardedBy("mLock")
    private LocalOnlyHotspotCallbackProxy mLOHSCallbackProxy;

    @GuardedBy("mLock")
    private LocalOnlyHotspotObserverProxy mLOHSObserverProxy;
    private int mListenerKey = 1;
    private final SparseArray mListenerMap = new SparseArray();
    private final Object mListenerMapLock = new Object();
    private final Object mLock = new Object();
    private Looper mLooper;
    IWifiManager mService;
    private final int mTargetSdkVersion;
    private final WifiHotspotManager mWifiHotspotManager;

    @SystemApi
    public interface ActionListener {
        void onFailure(int i);

        void onSuccess();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SapStartFailure {
    }

    public interface SoftApCallback {
        void onNumClientsChanged(int i);

        void onStateChanged(int i, int i2);
    }

    public interface TxPacketCountListener {
        void onFailure(int i);

        void onSuccess(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiApState {
    }

    public static abstract class WpsCallback {
        public abstract void onFailed(int i);

        public abstract void onStarted(String str);

        public abstract void onSucceeded();
    }

    static int access$708(WifiManager wifiManager) {
        int i = wifiManager.mActiveLockCount;
        wifiManager.mActiveLockCount = i + 1;
        return i;
    }

    static int access$710(WifiManager wifiManager) {
        int i = wifiManager.mActiveLockCount;
        wifiManager.mActiveLockCount = i - 1;
        return i;
    }

    public WifiManager(Context context, IWifiManager iWifiManager, Looper looper) {
        this.mContext = context;
        this.mService = iWifiManager;
        this.mLooper = looper;
        this.mTargetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        this.mWifiHotspotManager = new WifiHotspotManager(iWifiManager);
    }

    public List<WifiConfiguration> getConfiguredNetworks() {
        try {
            ParceledListSlice configuredNetworks = this.mService.getConfiguredNetworks();
            if (configuredNetworks == null) {
                return Collections.emptyList();
            }
            return configuredNetworks.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        try {
            ParceledListSlice privilegedConfiguredNetworks = this.mService.getPrivilegedConfiguredNetworks();
            if (privilegedConfiguredNetworks == null) {
                return Collections.emptyList();
            }
            return privilegedConfiguredNetworks.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) {
        try {
            return this.mService.getMatchingWifiConfig(scanResult);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) {
        try {
            return this.mService.getAllMatchingWifiConfigs(scanResult);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) {
        try {
            return this.mService.getMatchingOsuProviders(scanResult);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int addNetwork(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            return -1;
        }
        wifiConfiguration.networkId = -1;
        return addOrUpdateNetwork(wifiConfiguration);
    }

    public int updateNetwork(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null || wifiConfiguration.networkId < 0) {
            return -1;
        }
        return addOrUpdateNetwork(wifiConfiguration);
    }

    private int addOrUpdateNetwork(WifiConfiguration wifiConfiguration) {
        try {
            return this.mService.addOrUpdateNetwork(wifiConfiguration, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addOrUpdatePasspointConfiguration(PasspointConfiguration passpointConfiguration) {
        try {
            if (!this.mService.addOrUpdatePasspointConfiguration(passpointConfiguration, this.mContext.getOpPackageName())) {
                throw new IllegalArgumentException();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removePasspointConfiguration(String str) {
        try {
            if (!this.mService.removePasspointConfiguration(str, this.mContext.getOpPackageName())) {
                throw new IllegalArgumentException();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<PasspointConfiguration> getPasspointConfigurations() {
        try {
            return this.mService.getPasspointConfigurations();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void queryPasspointIcon(long j, String str) {
        try {
            this.mService.queryPasspointIcon(j, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int matchProviderWithCurrentNetwork(String str) {
        try {
            return this.mService.matchProviderWithCurrentNetwork(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deauthenticateNetwork(long j, boolean z) {
        try {
            this.mService.deauthenticateNetwork(j, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeNetwork(int i) {
        try {
            return this.mService.removeNetwork(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean enableNetwork(int i, boolean z) {
        boolean z2;
        if (!z || this.mTargetSdkVersion >= 21) {
            z2 = false;
        } else {
            z2 = true;
        }
        if (z2) {
            NetworkPinner.pin(this.mContext, new NetworkRequest.Builder().clearCapabilities().addCapability(15).addTransportType(1).build());
        }
        try {
            boolean zEnableNetwork = this.mService.enableNetwork(i, z, this.mContext.getOpPackageName());
            if (z2 && !zEnableNetwork) {
                NetworkPinner.unpin();
            }
            return zEnableNetwork;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean disableNetwork(int i) {
        try {
            return this.mService.disableNetwork(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean disconnect() {
        try {
            this.mService.disconnect(this.mContext.getOpPackageName());
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean reconnect() {
        try {
            this.mService.reconnect(this.mContext.getOpPackageName());
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean reassociate() {
        try {
            this.mService.reassociate(this.mContext.getOpPackageName());
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean pingSupplicant() {
        return isWifiEnabled();
    }

    private int getSupportedFeatures() {
        try {
            return this.mService.getSupportedFeatures();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean isFeatureSupported(int i) {
        return (getSupportedFeatures() & i) == i;
    }

    public boolean is5GHzBandSupported() {
        return isFeatureSupported(2);
    }

    public boolean isPasspointSupported() {
        return isFeatureSupported(4);
    }

    public boolean isP2pSupported() {
        return isFeatureSupported(8);
    }

    @SystemApi
    public boolean isPortableHotspotSupported() {
        return isFeatureSupported(16);
    }

    @SystemApi
    public boolean isWifiScannerSupported() {
        return isFeatureSupported(32);
    }

    public boolean isWifiAwareSupported() {
        return isFeatureSupported(64);
    }

    @SystemApi
    public boolean isDeviceToDeviceRttSupported() {
        return isFeatureSupported(128);
    }

    @SystemApi
    public boolean isDeviceToApRttSupported() {
        return isFeatureSupported(256);
    }

    public boolean isPreferredNetworkOffloadSupported() {
        return isFeatureSupported(1024);
    }

    public boolean isAdditionalStaSupported() {
        return isFeatureSupported(2048);
    }

    public boolean isTdlsSupported() {
        return isFeatureSupported(4096);
    }

    public boolean isOffChannelTdlsSupported() {
        return isFeatureSupported(8192);
    }

    public boolean isEnhancedPowerReportingSupported() {
        return isFeatureSupported(65536);
    }

    public WifiActivityEnergyInfo getControllerActivityEnergyInfo(int i) {
        WifiActivityEnergyInfo wifiActivityEnergyInfoReportActivityInfo;
        if (this.mService == null) {
            return null;
        }
        try {
            synchronized (this) {
                wifiActivityEnergyInfoReportActivityInfo = this.mService.reportActivityInfo();
            }
            return wifiActivityEnergyInfoReportActivityInfo;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean startScan() {
        return startScan(null);
    }

    @SystemApi
    public boolean startScan(WorkSource workSource) {
        try {
            return this.mService.startScan(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken() {
        return null;
    }

    public WifiInfo getConnectionInfo() {
        try {
            return this.mService.getConnectionInfo(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ScanResult> getScanResults() {
        try {
            return this.mService.getScanResults(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isScanAlwaysAvailable() {
        try {
            return this.mService.isScanAlwaysAvailable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean saveConfiguration() {
        return true;
    }

    public void setCountryCode(String str) {
        try {
            this.mService.setCountryCode(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getCountryCode() {
        try {
            return this.mService.getCountryCode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isDualBandSupported() {
        try {
            return this.mService.isDualBandSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isDualModeSupported() {
        try {
            return this.mService.needs5GHzToAnyApBandConversion();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public DhcpInfo getDhcpInfo() {
        try {
            return this.mService.getDhcpInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setWifiEnabled(boolean z) {
        try {
            return this.mService.setWifiEnabled(this.mContext.getOpPackageName(), z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getWifiState() {
        try {
            return this.mService.getWifiEnabledState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isWifiEnabled() {
        return getWifiState() == 3;
    }

    public void getTxPacketCount(TxPacketCountListener txPacketCountListener) {
        getChannel().sendMessage(RSSI_PKTCNT_FETCH, 0, putListener(txPacketCountListener));
    }

    public static int calculateSignalLevel(int i, int i2) {
        if (i <= -100) {
            return 0;
        }
        if (i >= -55) {
            return i2 - 1;
        }
        return (int) (((i - (-100)) * (i2 - 1)) / 45.0f);
    }

    public static int compareSignalLevel(int i, int i2) {
        return i - i2;
    }

    public void updateInterfaceIpState(String str, int i) {
        try {
            this.mService.updateInterfaceIpState(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean startSoftAp(WifiConfiguration wifiConfiguration) {
        try {
            return this.mService.startSoftAp(wifiConfiguration);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean stopSoftAp() {
        try {
            return this.mService.stopSoftAp();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startLocalOnlyHotspot(LocalOnlyHotspotCallback localOnlyHotspotCallback, Handler handler) {
        synchronized (this.mLock) {
            try {
                LocalOnlyHotspotCallbackProxy localOnlyHotspotCallbackProxy = new LocalOnlyHotspotCallbackProxy(this, handler == null ? this.mContext.getMainLooper() : handler.getLooper(), localOnlyHotspotCallback);
                try {
                    int iStartLocalOnlyHotspot = this.mService.startLocalOnlyHotspot(localOnlyHotspotCallbackProxy.getMessenger(), new Binder(), this.mContext.getOpPackageName());
                    if (iStartLocalOnlyHotspot != 0) {
                        localOnlyHotspotCallbackProxy.notifyFailed(iStartLocalOnlyHotspot);
                    } else {
                        this.mLOHSCallbackProxy = localOnlyHotspotCallbackProxy;
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void cancelLocalOnlyHotspotRequest() {
        synchronized (this.mLock) {
            stopLocalOnlyHotspot();
        }
    }

    private void stopLocalOnlyHotspot() {
        synchronized (this.mLock) {
            if (this.mLOHSCallbackProxy == null) {
                return;
            }
            this.mLOHSCallbackProxy = null;
            try {
                this.mService.stopLocalOnlyHotspot();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void watchLocalOnlyHotspot(LocalOnlyHotspotObserver localOnlyHotspotObserver, Handler handler) {
        synchronized (this.mLock) {
            try {
                this.mLOHSObserverProxy = new LocalOnlyHotspotObserverProxy(this, handler == null ? this.mContext.getMainLooper() : handler.getLooper(), localOnlyHotspotObserver);
                try {
                    this.mService.startWatchLocalOnlyHotspot(this.mLOHSObserverProxy.getMessenger(), new Binder());
                    this.mLOHSObserverProxy.registered();
                } catch (RemoteException e) {
                    this.mLOHSObserverProxy = null;
                    throw e.rethrowFromSystemServer();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void unregisterLocalOnlyHotspotObserver() {
        synchronized (this.mLock) {
            if (this.mLOHSObserverProxy == null) {
                return;
            }
            this.mLOHSObserverProxy = null;
            try {
                this.mService.stopWatchLocalOnlyHotspot();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public int getWifiApState() {
        try {
            return this.mService.getWifiApEnabledState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isWifiApEnabled() {
        return getWifiApState() == 13;
    }

    @SystemApi
    public WifiConfiguration getWifiApConfiguration() {
        try {
            return this.mService.getWifiApConfiguration();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean setWifiApConfiguration(WifiConfiguration wifiConfiguration) {
        try {
            return this.mService.setWifiApConfiguration(wifiConfiguration, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTdlsEnabled(InetAddress inetAddress, boolean z) {
        try {
            this.mService.enableTdls(inetAddress.getHostAddress(), z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTdlsEnabledWithMacAddress(String str, boolean z) {
        try {
            this.mService.enableTdlsWithMacAddress(str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class SoftApCallbackProxy extends ISoftApCallback.Stub {
        private final SoftApCallback mCallback;
        private final Handler mHandler;

        SoftApCallbackProxy(Looper looper, SoftApCallback softApCallback) {
            this.mHandler = new Handler(looper);
            this.mCallback = softApCallback;
        }

        @Override
        public void onStateChanged(final int i, final int i2) throws RemoteException {
            Log.v(WifiManager.TAG, "SoftApCallbackProxy: onStateChanged: state=" + i + ", failureReason=" + i2);
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onStateChanged(i, i2);
                }
            });
        }

        @Override
        public void onNumClientsChanged(final int i) throws RemoteException {
            Log.v(WifiManager.TAG, "SoftApCallbackProxy: onNumClientsChanged: numClients=" + i);
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onNumClientsChanged(i);
                }
            });
        }
    }

    public void registerSoftApCallback(SoftApCallback softApCallback, Handler handler) {
        if (softApCallback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        Log.v(TAG, "registerSoftApCallback: callback=" + softApCallback + ", handler=" + handler);
        try {
            this.mService.registerSoftApCallback(new Binder(), new SoftApCallbackProxy(handler == null ? this.mContext.getMainLooper() : handler.getLooper(), softApCallback), softApCallback.hashCode());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterSoftApCallback(SoftApCallback softApCallback) {
        if (softApCallback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        Log.v(TAG, "unregisterSoftApCallback: callback=" + softApCallback);
        try {
            this.mService.unregisterSoftApCallback(softApCallback.hashCode());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public class LocalOnlyHotspotReservation implements AutoCloseable {
        private final CloseGuard mCloseGuard = CloseGuard.get();
        private final WifiConfiguration mConfig;

        @VisibleForTesting
        public LocalOnlyHotspotReservation(WifiConfiguration wifiConfiguration) {
            this.mConfig = wifiConfiguration;
            this.mCloseGuard.open("close");
        }

        public WifiConfiguration getWifiConfiguration() {
            return this.mConfig;
        }

        @Override
        public void close() {
            try {
                WifiManager.this.stopLocalOnlyHotspot();
                this.mCloseGuard.close();
            } catch (Exception e) {
                Log.e(WifiManager.TAG, "Failed to stop Local Only Hotspot.");
            }
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }
    }

    public static class LocalOnlyHotspotCallback {
        public static final int ERROR_GENERIC = 2;
        public static final int ERROR_INCOMPATIBLE_MODE = 3;
        public static final int ERROR_NO_CHANNEL = 1;
        public static final int ERROR_TETHERING_DISALLOWED = 4;
        public static final int REQUEST_REGISTERED = 0;

        public void onStarted(LocalOnlyHotspotReservation localOnlyHotspotReservation) {
        }

        public void onStopped() {
        }

        public void onFailed(int i) {
        }
    }

    private static class LocalOnlyHotspotCallbackProxy {
        private final Handler mHandler;
        private final Looper mLooper;
        private final Messenger mMessenger;
        private final WeakReference<WifiManager> mWifiManager;

        LocalOnlyHotspotCallbackProxy(WifiManager wifiManager, Looper looper, final LocalOnlyHotspotCallback localOnlyHotspotCallback) {
            this.mWifiManager = new WeakReference<>(wifiManager);
            this.mLooper = looper;
            this.mHandler = new Handler(looper) {
                @Override
                public void handleMessage(Message message) {
                    Log.d(WifiManager.TAG, "LocalOnlyHotspotCallbackProxy: handle message what: " + message.what + " msg: " + message);
                    WifiManager wifiManager2 = (WifiManager) LocalOnlyHotspotCallbackProxy.this.mWifiManager.get();
                    if (wifiManager2 == null) {
                        Log.w(WifiManager.TAG, "LocalOnlyHotspotCallbackProxy: handle message post GC");
                    }
                    switch (message.what) {
                        case 0:
                            WifiConfiguration wifiConfiguration = (WifiConfiguration) message.obj;
                            if (wifiConfiguration == null) {
                                Log.e(WifiManager.TAG, "LocalOnlyHotspotCallbackProxy: config cannot be null.");
                                localOnlyHotspotCallback.onFailed(2);
                            } else {
                                LocalOnlyHotspotCallback localOnlyHotspotCallback2 = localOnlyHotspotCallback;
                                Objects.requireNonNull(wifiManager2);
                                localOnlyHotspotCallback2.onStarted(wifiManager2.new LocalOnlyHotspotReservation(wifiConfiguration));
                            }
                            break;
                        case 1:
                            Log.w(WifiManager.TAG, "LocalOnlyHotspotCallbackProxy: hotspot stopped");
                            localOnlyHotspotCallback.onStopped();
                            break;
                        case 2:
                            int i = message.arg1;
                            Log.w(WifiManager.TAG, "LocalOnlyHotspotCallbackProxy: failed to start.  reason: " + i);
                            localOnlyHotspotCallback.onFailed(i);
                            Log.w(WifiManager.TAG, "done with the callback...");
                            break;
                        default:
                            Log.e(WifiManager.TAG, "LocalOnlyHotspotCallbackProxy unhandled message.  type: " + message.what);
                            break;
                    }
                }
            };
            this.mMessenger = new Messenger(this.mHandler);
        }

        public Messenger getMessenger() {
            return this.mMessenger;
        }

        public void notifyFailed(int i) throws RemoteException {
            Message messageObtain = Message.obtain();
            messageObtain.what = 2;
            messageObtain.arg1 = i;
            this.mMessenger.send(messageObtain);
        }
    }

    public class LocalOnlyHotspotSubscription implements AutoCloseable {
        private final CloseGuard mCloseGuard = CloseGuard.get();

        @VisibleForTesting
        public LocalOnlyHotspotSubscription() {
            this.mCloseGuard.open("close");
        }

        @Override
        public void close() {
            try {
                WifiManager.this.unregisterLocalOnlyHotspotObserver();
                this.mCloseGuard.close();
            } catch (Exception e) {
                Log.e(WifiManager.TAG, "Failed to unregister LocalOnlyHotspotObserver.");
            }
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }
    }

    public static class LocalOnlyHotspotObserver {
        public void onRegistered(LocalOnlyHotspotSubscription localOnlyHotspotSubscription) {
        }

        public void onStarted(WifiConfiguration wifiConfiguration) {
        }

        public void onStopped() {
        }
    }

    private static class LocalOnlyHotspotObserverProxy {
        private final Handler mHandler;
        private final Looper mLooper;
        private final Messenger mMessenger;
        private final WeakReference<WifiManager> mWifiManager;

        LocalOnlyHotspotObserverProxy(WifiManager wifiManager, Looper looper, final LocalOnlyHotspotObserver localOnlyHotspotObserver) {
            this.mWifiManager = new WeakReference<>(wifiManager);
            this.mLooper = looper;
            this.mHandler = new Handler(looper) {
                @Override
                public void handleMessage(Message message) {
                    Log.d(WifiManager.TAG, "LocalOnlyHotspotObserverProxy: handle message what: " + message.what + " msg: " + message);
                    WifiManager wifiManager2 = (WifiManager) LocalOnlyHotspotObserverProxy.this.mWifiManager.get();
                    if (wifiManager2 == null) {
                        Log.w(WifiManager.TAG, "LocalOnlyHotspotObserverProxy: handle message post GC");
                    }
                    int i = message.what;
                    if (i == 3) {
                        LocalOnlyHotspotObserver localOnlyHotspotObserver2 = localOnlyHotspotObserver;
                        Objects.requireNonNull(wifiManager2);
                        localOnlyHotspotObserver2.onRegistered(wifiManager2.new LocalOnlyHotspotSubscription());
                        return;
                    }
                    switch (i) {
                        case 0:
                            WifiConfiguration wifiConfiguration = (WifiConfiguration) message.obj;
                            if (wifiConfiguration == null) {
                                Log.e(WifiManager.TAG, "LocalOnlyHotspotObserverProxy: config cannot be null.");
                            } else {
                                localOnlyHotspotObserver.onStarted(wifiConfiguration);
                            }
                            break;
                        case 1:
                            localOnlyHotspotObserver.onStopped();
                            break;
                        default:
                            Log.e(WifiManager.TAG, "LocalOnlyHotspotObserverProxy unhandled message.  type: " + message.what);
                            break;
                    }
                }
            };
            this.mMessenger = new Messenger(this.mHandler);
        }

        public Messenger getMessenger() {
            return this.mMessenger;
        }

        public void registered() throws RemoteException {
            Message messageObtain = Message.obtain();
            messageObtain.what = 3;
            this.mMessenger.send(messageObtain);
        }
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            synchronized (WifiManager.sServiceHandlerDispatchLock) {
                dispatchMessageToListeners(message);
            }
        }

        private void dispatchMessageToListeners(Message message) {
            Object objRemoveListener = WifiManager.this.removeListener(message.arg2);
            switch (message.what) {
                case 69632:
                    if (message.arg1 == 0) {
                        WifiManager.this.mAsyncChannel.sendMessage(AsyncChannel.CMD_CHANNEL_FULL_CONNECTION);
                    } else {
                        Log.e(WifiManager.TAG, "Failed to set up channel connection");
                        WifiManager.this.mAsyncChannel = null;
                    }
                    WifiManager.this.mConnected.countDown();
                    break;
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                    Log.e(WifiManager.TAG, "Channel connection lost");
                    WifiManager.this.mAsyncChannel = null;
                    getLooper().quit();
                    break;
                case WifiManager.CONNECT_NETWORK_FAILED:
                case WifiManager.FORGET_NETWORK_FAILED:
                case WifiManager.SAVE_NETWORK_FAILED:
                case WifiManager.DISABLE_NETWORK_FAILED:
                    if (objRemoveListener != null) {
                        ((ActionListener) objRemoveListener).onFailure(message.arg1);
                    }
                    break;
                case WifiManager.CONNECT_NETWORK_SUCCEEDED:
                case WifiManager.FORGET_NETWORK_SUCCEEDED:
                case WifiManager.SAVE_NETWORK_SUCCEEDED:
                case WifiManager.DISABLE_NETWORK_SUCCEEDED:
                    if (objRemoveListener != null) {
                        ((ActionListener) objRemoveListener).onSuccess();
                    }
                    break;
                case WifiManager.RSSI_PKTCNT_FETCH_SUCCEEDED:
                    if (objRemoveListener != null) {
                        RssiPacketCountInfo rssiPacketCountInfo = (RssiPacketCountInfo) message.obj;
                        if (rssiPacketCountInfo != null) {
                            ((TxPacketCountListener) objRemoveListener).onSuccess(rssiPacketCountInfo.txgood + rssiPacketCountInfo.txbad);
                        } else {
                            ((TxPacketCountListener) objRemoveListener).onFailure(0);
                        }
                    }
                    break;
                case WifiManager.RSSI_PKTCNT_FETCH_FAILED:
                    if (objRemoveListener != null) {
                        ((TxPacketCountListener) objRemoveListener).onFailure(message.arg1);
                    }
                    break;
            }
        }
    }

    private int putListener(Object obj) {
        int i;
        if (obj == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            do {
                i = this.mListenerKey;
                this.mListenerKey = i + 1;
            } while (i == 0);
            this.mListenerMap.put(i, obj);
        }
        return i;
    }

    private Object removeListener(int i) {
        Object obj;
        if (i == 0) {
            return null;
        }
        synchronized (this.mListenerMapLock) {
            obj = this.mListenerMap.get(i);
            this.mListenerMap.remove(i);
        }
        return obj;
    }

    private synchronized AsyncChannel getChannel() {
        if (this.mAsyncChannel == null) {
            Messenger wifiServiceMessenger = getWifiServiceMessenger();
            if (wifiServiceMessenger == null) {
                throw new IllegalStateException("getWifiServiceMessenger() returned null!  This is invalid.");
            }
            this.mAsyncChannel = new AsyncChannel();
            this.mConnected = new CountDownLatch(1);
            this.mAsyncChannel.connect(this.mContext, new ServiceHandler(this.mLooper), wifiServiceMessenger);
            try {
                this.mConnected.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted wait at init");
            }
        }
        return this.mAsyncChannel;
    }

    @SystemApi
    public void connect(WifiConfiguration wifiConfiguration, ActionListener actionListener) {
        if (wifiConfiguration == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        getChannel().sendMessage(CONNECT_NETWORK, -1, putListener(actionListener), wifiConfiguration);
    }

    public void connect(int i, ActionListener actionListener) {
        if (i < 0) {
            throw new IllegalArgumentException("Network id cannot be negative");
        }
        getChannel().sendMessage(CONNECT_NETWORK, i, putListener(actionListener));
    }

    public void save(WifiConfiguration wifiConfiguration, ActionListener actionListener) {
        if (wifiConfiguration == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        getChannel().sendMessage(SAVE_NETWORK, 0, putListener(actionListener), wifiConfiguration);
    }

    public void forget(int i, ActionListener actionListener) {
        if (i < 0) {
            throw new IllegalArgumentException("Network id cannot be negative");
        }
        getChannel().sendMessage(FORGET_NETWORK, i, putListener(actionListener));
    }

    public void disable(int i, ActionListener actionListener) {
        if (i < 0) {
            throw new IllegalArgumentException("Network id cannot be negative");
        }
        getChannel().sendMessage(DISABLE_NETWORK, i, putListener(actionListener));
    }

    public void disableEphemeralNetwork(String str) {
        if (str == null) {
            throw new IllegalArgumentException("SSID cannot be null");
        }
        try {
            this.mService.disableEphemeralNetwork(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startWps(WpsInfo wpsInfo, WpsCallback wpsCallback) {
        if (wpsCallback != null) {
            wpsCallback.onFailed(0);
        }
    }

    public void cancelWps(WpsCallback wpsCallback) {
        if (wpsCallback != null) {
            wpsCallback.onFailed(0);
        }
    }

    public Messenger getWifiServiceMessenger() {
        try {
            return this.mService.getWifiServiceMessenger(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public class WifiLock {
        private final IBinder mBinder;
        private boolean mHeld;
        int mLockType;
        private int mRefCount;
        private boolean mRefCounted;
        private String mTag;
        private WorkSource mWorkSource;

        private WifiLock(int i, String str) {
            this.mTag = str;
            this.mLockType = i;
            this.mBinder = new Binder();
            this.mRefCount = 0;
            this.mRefCounted = true;
            this.mHeld = false;
        }

        public void acquire() {
            synchronized (this.mBinder) {
                if (this.mRefCounted) {
                    int i = this.mRefCount + 1;
                    this.mRefCount = i;
                    if (i == 1) {
                        try {
                            WifiManager.this.mService.acquireWifiLock(this.mBinder, this.mLockType, this.mTag, this.mWorkSource);
                            synchronized (WifiManager.this) {
                                if (WifiManager.this.mActiveLockCount >= 50) {
                                    WifiManager.this.mService.releaseWifiLock(this.mBinder);
                                    throw new UnsupportedOperationException("Exceeded maximum number of wifi locks");
                                }
                                WifiManager.access$708(WifiManager.this);
                            }
                            this.mHeld = true;
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                } else if (!this.mHeld) {
                    WifiManager.this.mService.acquireWifiLock(this.mBinder, this.mLockType, this.mTag, this.mWorkSource);
                    synchronized (WifiManager.this) {
                    }
                }
            }
        }

        public void release() {
            synchronized (this.mBinder) {
                if (this.mRefCounted) {
                    int i = this.mRefCount - 1;
                    this.mRefCount = i;
                    if (i == 0) {
                        try {
                            WifiManager.this.mService.releaseWifiLock(this.mBinder);
                            synchronized (WifiManager.this) {
                                WifiManager.access$710(WifiManager.this);
                            }
                            this.mHeld = false;
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                    if (this.mRefCount >= 0) {
                        throw new RuntimeException("WifiLock under-locked " + this.mTag);
                    }
                } else {
                    if (this.mHeld) {
                        WifiManager.this.mService.releaseWifiLock(this.mBinder);
                        synchronized (WifiManager.this) {
                        }
                    }
                    if (this.mRefCount >= 0) {
                    }
                }
            }
        }

        public void setReferenceCounted(boolean z) {
            this.mRefCounted = z;
        }

        public boolean isHeld() {
            boolean z;
            synchronized (this.mBinder) {
                z = this.mHeld;
            }
            return z;
        }

        public void setWorkSource(WorkSource workSource) {
            synchronized (this.mBinder) {
                if (workSource != null) {
                    try {
                        if (workSource.isEmpty()) {
                            workSource = null;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                boolean zEquals = true;
                if (workSource == null) {
                    this.mWorkSource = null;
                } else {
                    workSource.clearNames();
                    if (this.mWorkSource == null) {
                        if (this.mWorkSource == null) {
                            zEquals = false;
                        }
                        this.mWorkSource = new WorkSource(workSource);
                    } else {
                        zEquals = true ^ this.mWorkSource.equals(workSource);
                        if (zEquals) {
                            this.mWorkSource.set(workSource);
                        }
                    }
                }
                if (zEquals && this.mHeld) {
                    try {
                        WifiManager.this.mService.updateWifiLockWorkSource(this.mBinder, this.mWorkSource);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }

        public String toString() {
            String str;
            String str2;
            synchronized (this.mBinder) {
                String hexString = Integer.toHexString(System.identityHashCode(this));
                String str3 = this.mHeld ? "held; " : "";
                if (this.mRefCounted) {
                    str = "refcounted: refcount = " + this.mRefCount;
                } else {
                    str = "not refcounted";
                }
                str2 = "WifiLock{ " + hexString + "; " + str3 + str + " }";
            }
            return str2;
        }

        protected void finalize() throws Throwable {
            super.finalize();
            synchronized (this.mBinder) {
                if (this.mHeld) {
                    try {
                        WifiManager.this.mService.releaseWifiLock(this.mBinder);
                        synchronized (WifiManager.this) {
                            WifiManager.access$710(WifiManager.this);
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    public WifiLock createWifiLock(int i, String str) {
        return new WifiLock(i, str);
    }

    public WifiLock createWifiLock(String str) {
        return new WifiLock(1, str);
    }

    public MulticastLock createMulticastLock(String str) {
        return new MulticastLock(str);
    }

    public class MulticastLock {
        private final IBinder mBinder;
        private boolean mHeld;
        private int mRefCount;
        private boolean mRefCounted;
        private String mTag;

        private MulticastLock(String str) {
            this.mTag = str;
            this.mBinder = new Binder();
            this.mRefCount = 0;
            this.mRefCounted = true;
            this.mHeld = false;
        }

        public void acquire() {
            synchronized (this.mBinder) {
                if (this.mRefCounted) {
                    int i = this.mRefCount + 1;
                    this.mRefCount = i;
                    if (i == 1) {
                        try {
                            WifiManager.this.mService.acquireMulticastLock(this.mBinder, this.mTag);
                            synchronized (WifiManager.this) {
                                if (WifiManager.this.mActiveLockCount >= 50) {
                                    WifiManager.this.mService.releaseMulticastLock();
                                    throw new UnsupportedOperationException("Exceeded maximum number of wifi locks");
                                }
                                WifiManager.access$708(WifiManager.this);
                            }
                            this.mHeld = true;
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                } else if (!this.mHeld) {
                    WifiManager.this.mService.acquireMulticastLock(this.mBinder, this.mTag);
                    synchronized (WifiManager.this) {
                    }
                }
            }
        }

        public void release() {
            synchronized (this.mBinder) {
                if (this.mRefCounted) {
                    int i = this.mRefCount - 1;
                    this.mRefCount = i;
                    if (i == 0) {
                        try {
                            WifiManager.this.mService.releaseMulticastLock();
                            synchronized (WifiManager.this) {
                                WifiManager.access$710(WifiManager.this);
                            }
                            this.mHeld = false;
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                    if (this.mRefCount >= 0) {
                        throw new RuntimeException("MulticastLock under-locked " + this.mTag);
                    }
                } else {
                    if (this.mHeld) {
                        WifiManager.this.mService.releaseMulticastLock();
                        synchronized (WifiManager.this) {
                        }
                    }
                    if (this.mRefCount >= 0) {
                    }
                }
            }
        }

        public void setReferenceCounted(boolean z) {
            this.mRefCounted = z;
        }

        public boolean isHeld() {
            boolean z;
            synchronized (this.mBinder) {
                z = this.mHeld;
            }
            return z;
        }

        public String toString() {
            String str;
            String str2;
            synchronized (this.mBinder) {
                String hexString = Integer.toHexString(System.identityHashCode(this));
                String str3 = this.mHeld ? "held; " : "";
                if (this.mRefCounted) {
                    str = "refcounted: refcount = " + this.mRefCount;
                } else {
                    str = "not refcounted";
                }
                str2 = "MulticastLock{ " + hexString + "; " + str3 + str + " }";
            }
            return str2;
        }

        protected void finalize() throws Throwable {
            super.finalize();
            setReferenceCounted(false);
            release();
        }
    }

    public boolean isMulticastEnabled() {
        try {
            return this.mService.isMulticastEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean initializeMulticastFiltering() {
        try {
            this.mService.initializeMulticastFiltering();
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mAsyncChannel != null) {
                this.mAsyncChannel.disconnect();
            }
        } finally {
            super.finalize();
        }
    }

    public void enableVerboseLogging(int i) {
        try {
            this.mService.enableVerboseLogging(i);
        } catch (Exception e) {
            Log.e(TAG, "enableVerboseLogging " + e.toString());
        }
    }

    public int getVerboseLoggingLevel() {
        try {
            return this.mService.getVerboseLoggingLevel();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void factoryReset() {
        try {
            this.mService.factoryReset(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network getCurrentNetwork() {
        try {
            return this.mService.getCurrentNetwork();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEnableAutoJoinWhenAssociated(boolean z) {
        return false;
    }

    public boolean getEnableAutoJoinWhenAssociated() {
        return false;
    }

    public void enableWifiConnectivityManager(boolean z) {
        try {
            this.mService.enableWifiConnectivityManager(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public byte[] retrieveBackupData() {
        try {
            return this.mService.retrieveBackupData();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void restoreBackupData(byte[] bArr) {
        try {
            this.mService.restoreBackupData(bArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPowerSavingMode(boolean z) {
        if (this.mService == null) {
            Log.d(TAG, "setPowerSavingMode, fail, null == mService");
        } else {
            try {
                this.mService.setPowerSavingMode(z);
            } catch (RemoteException e) {
            }
        }
    }

    @Deprecated
    public void restoreSupplicantBackupData(byte[] bArr, byte[] bArr2) {
        try {
            this.mService.restoreSupplicantBackupData(bArr, bArr2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startSubscriptionProvisioning(OsuProvider osuProvider, ProvisioningCallback provisioningCallback, Handler handler) {
        try {
            this.mService.startSubscriptionProvisioning(osuProvider, new ProvisioningCallbackProxy(handler == null ? Looper.getMainLooper() : handler.getLooper(), provisioningCallback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class ProvisioningCallbackProxy extends IProvisioningCallback.Stub {
        private final ProvisioningCallback mCallback;
        private final Handler mHandler;

        ProvisioningCallbackProxy(Looper looper, ProvisioningCallback provisioningCallback) {
            this.mHandler = new Handler(looper);
            this.mCallback = provisioningCallback;
        }

        @Override
        public void onProvisioningStatus(final int i) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onProvisioningStatus(i);
                }
            });
        }

        @Override
        public void onProvisioningFailure(final int i) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onProvisioningFailure(i);
                }
            });
        }
    }

    public WifiHotspotManager getWifiHotspotManager() {
        return this.mWifiHotspotManager;
    }

    public boolean stopReconnectAndScan(boolean z, int i) {
        stopReconnectAndScan(z, i, false);
        return true;
    }

    public boolean stopReconnectAndScan(boolean z, int i, boolean z2) {
        Log.d(TAG, "stopReconnectAndScan, " + z + " period=" + i + " isAllowReconnect=" + z2);
        if (z && z2) {
            getChannel().sendMessage(SET_WIFI_NOT_RECONNECT_AND_SCAN, 1, i);
        } else if (!z || z2) {
            getChannel().sendMessage(SET_WIFI_NOT_RECONNECT_AND_SCAN, 0, 0);
        } else {
            getChannel().sendMessage(SET_WIFI_NOT_RECONNECT_AND_SCAN, 2, i);
        }
        return true;
    }
}
