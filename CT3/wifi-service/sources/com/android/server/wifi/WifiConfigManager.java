package com.android.server.wifi;

import android.R;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.net.IpConfiguration;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.PasspointManagementObjectDefinition;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.net.DelayedDiskWrite;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.anqp.ANQPElement;
import com.android.server.wifi.anqp.ANQPFactory;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.hotspot2.ANQPData;
import com.android.server.wifi.hotspot2.AnqpCache;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointMatch;
import com.android.server.wifi.hotspot2.SupplicantBridge;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.omadm.PasspointManagementObjectManager;
import com.android.server.wifi.hotspot2.pps.Credential;
import com.android.server.wifi.hotspot2.pps.HomeSP;
import com.mediatek.common.wifi.IWifiFwkExt;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.xml.sax.SAXException;

public class WifiConfigManager {

    private static final int[] f1androidnetIpConfiguration$IpAssignmentSwitchesValues = null;

    private static final int[] f2androidnetIpConfiguration$ProxySettingsSwitchesValues = null;

    private static final int[] f3androidnetNetworkInfo$DetailedStateSwitchesValues = null;
    private static final boolean DBG = true;
    private static final int DEFAULT_MAX_DHCP_RETRIES = 9;
    private static final String DELETED_CONFIG_PSK = "Mjkd86jEMGn79KhKll298Uu7-deleted";
    public static final int MAX_NUM_SCAN_CACHE_ENTRIES = 128;
    public static final int MAX_RX_PACKET_FOR_FULL_SCANS = 16;
    public static final int MAX_RX_PACKET_FOR_PARTIAL_SCANS = 80;
    public static final int MAX_TX_PACKET_FOR_FULL_SCANS = 8;
    public static final int MAX_TX_PACKET_FOR_PARTIAL_SCANS = 40;
    private static final String PPS_FILE = "/data/misc/wifi/PerProviderSubscription.conf";
    public static final boolean ROAM_ON_ANY = false;
    public static final String TAG = "WifiConfigManager";
    private static final String WIFI_VERBOSE_LOGS_KEY = "WIFI_VERBOSE_LOGS";
    private ScanDetail mActiveScanDetail;
    private final AnqpCache mAnqpCache;
    public int mBadLinkSpeed24;
    public int mBadLinkSpeed5;
    private Clock mClock;
    private final ConfigurationMap mConfiguredNetworks;
    private Context mContext;
    public boolean mEnableLinkDebouncing;
    private final boolean mEnableOsuQueries;
    public boolean mEnableWifiCellularHandoverUserTriggeredAdjustment;
    private FrameworkFacade mFacade;
    public int mGoodLinkSpeed24;
    public int mGoodLinkSpeed5;
    private IpConfigStore mIpconfigStore;
    private final KeyStore mKeyStore;
    private final LocalLog mLocalLog;
    private final PasspointManagementObjectManager mMOManager;
    public int mNetworkSwitchingBlackListPeriodMs;
    private boolean mOnlyLinkSameCredentialConfigurations;
    private final SIMAccessor mSIMAccessor;
    private ConcurrentHashMap<Integer, ScanDetailCache> mScanDetailCaches;
    private final SupplicantBridge mSupplicantBridge;
    private final SupplicantBridgeCallbacks mSupplicantBridgeCallbacks;
    private final UserManager mUserManager;
    private final WifiConfigStore mWifiConfigStore;
    private IWifiFwkExt mWifiFwkExt;
    private final WifiNetworkHistory mWifiNetworkHistory;
    private DelayedDiskWrite mWriter;
    private static boolean sVDBG = true;
    private static boolean sVVDBG = true;
    private static final String IP_CONFIG_FILE = Environment.getDataDirectory() + "/misc/wifi/ipconfig.txt";
    private static final int[] NETWORK_SELECTION_DISABLE_THRESHOLD = {-1, 1, 5, 2, 5, 5, 6, 1, 1, 1, 1};
    private static final int[] NETWORK_SELECTION_DISABLE_TIMEOUT = {Integer.MAX_VALUE, 15, 5, 5, 5, 5, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
    private static final PnoListComparator sDisconnectedPnoListComparator = new PnoListComparator() {
        @Override
        public int compareConfigurations(WifiConfiguration a, WifiConfiguration b) {
            if (a.numAssociation != b.numAssociation) {
                return Long.compare(b.numAssociation, a.numAssociation);
            }
            return Integer.compare(b.priority, a.priority);
        }
    };
    private static final PnoListComparator sConnectedPnoListComparator = new PnoListComparator() {
        @Override
        public int compareConfigurations(WifiConfiguration a, WifiConfiguration b) {
            boolean isConfigALastSeen = a.getNetworkSelectionStatus().getSeenInLastQualifiedNetworkSelection();
            boolean isConfigBLastSeen = b.getNetworkSelectionStatus().getSeenInLastQualifiedNetworkSelection();
            if (isConfigALastSeen != isConfigBLastSeen) {
                return Boolean.compare(isConfigBLastSeen, isConfigALastSeen);
            }
            return Long.compare(b.numAssociation, a.numAssociation);
        }
    };
    public final AtomicBoolean mEnableAutoJoinWhenAssociated = new AtomicBoolean();
    public final AtomicBoolean mEnableChipWakeUpWhenAssociated = new AtomicBoolean(true);
    public final AtomicBoolean mEnableRssiPollWhenAssociated = new AtomicBoolean(true);
    public final AtomicInteger mThresholdSaturatedRssi5 = new AtomicInteger();
    public final AtomicInteger mThresholdQualifiedRssi24 = new AtomicInteger();
    public final AtomicInteger mEnableVerboseLogging = new AtomicInteger(0);
    public final AtomicInteger mAlwaysEnableScansWhileAssociated = new AtomicInteger(0);
    public final AtomicInteger mMaxNumActiveChannelsForPartialScans = new AtomicInteger();
    public AtomicInteger mThresholdQualifiedRssi5 = new AtomicInteger();
    public AtomicInteger mThresholdMinimumRssi5 = new AtomicInteger();
    public AtomicInteger mThresholdSaturatedRssi24 = new AtomicInteger();
    public AtomicInteger mThresholdMinimumRssi24 = new AtomicInteger();
    public AtomicInteger mCurrentNetworkBoost = new AtomicInteger();
    public AtomicInteger mBandAward5Ghz = new AtomicInteger();
    public long mLastUnwantedNetworkDisconnectTimestamp = 0;
    public Set<String> mDeletedEphemeralSSIDs = new HashSet();
    private final Object mActiveScanDetailLock = new Object();
    private boolean mShowNetworks = false;
    private int mCurrentUserId = 0;
    private List<Integer> mDisconnectNetworks = new ArrayList();
    private int mLastPriority = -1;
    private String mLastSelectedConfiguration = null;
    private long mLastSelectedTimeStamp = -1;
    private HashSet<String> mLostConfigsDbg = new HashSet<>();

    private static int[] m39getandroidnetIpConfiguration$IpAssignmentSwitchesValues() {
        if (f1androidnetIpConfiguration$IpAssignmentSwitchesValues != null) {
            return f1androidnetIpConfiguration$IpAssignmentSwitchesValues;
        }
        int[] iArr = new int[IpConfiguration.IpAssignment.values().length];
        try {
            iArr[IpConfiguration.IpAssignment.DHCP.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IpConfiguration.IpAssignment.STATIC.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IpConfiguration.IpAssignment.UNASSIGNED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f1androidnetIpConfiguration$IpAssignmentSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m40getandroidnetIpConfiguration$ProxySettingsSwitchesValues() {
        if (f2androidnetIpConfiguration$ProxySettingsSwitchesValues != null) {
            return f2androidnetIpConfiguration$ProxySettingsSwitchesValues;
        }
        int[] iArr = new int[IpConfiguration.ProxySettings.values().length];
        try {
            iArr[IpConfiguration.ProxySettings.NONE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IpConfiguration.ProxySettings.PAC.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IpConfiguration.ProxySettings.STATIC.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IpConfiguration.ProxySettings.UNASSIGNED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f2androidnetIpConfiguration$ProxySettingsSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m41getandroidnetNetworkInfo$DetailedStateSwitchesValues() {
        if (f3androidnetNetworkInfo$DetailedStateSwitchesValues != null) {
            return f3androidnetNetworkInfo$DetailedStateSwitchesValues;
        }
        int[] iArr = new int[NetworkInfo.DetailedState.values().length];
        try {
            iArr[NetworkInfo.DetailedState.AUTHENTICATING.ordinal()] = 10;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[NetworkInfo.DetailedState.BLOCKED.ordinal()] = 11;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK.ordinal()] = 12;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[NetworkInfo.DetailedState.CONNECTED.ordinal()] = 1;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[NetworkInfo.DetailedState.CONNECTING.ordinal()] = 13;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[NetworkInfo.DetailedState.DISCONNECTED.ordinal()] = 2;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[NetworkInfo.DetailedState.DISCONNECTING.ordinal()] = 14;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[NetworkInfo.DetailedState.FAILED.ordinal()] = 15;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[NetworkInfo.DetailedState.IDLE.ordinal()] = 16;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[NetworkInfo.DetailedState.OBTAINING_IPADDR.ordinal()] = 17;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[NetworkInfo.DetailedState.SCANNING.ordinal()] = 18;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[NetworkInfo.DetailedState.SUSPENDED.ordinal()] = 19;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[NetworkInfo.DetailedState.VERIFYING_POOR_LINK.ordinal()] = 20;
        } catch (NoSuchFieldError e13) {
        }
        f3androidnetNetworkInfo$DetailedStateSwitchesValues = iArr;
        return iArr;
    }

    private class SupplicantBridgeCallbacks implements SupplicantBridge.SupplicantBridgeCallbacks {
        SupplicantBridgeCallbacks(WifiConfigManager this$0, SupplicantBridgeCallbacks supplicantBridgeCallbacks) {
            this();
        }

        private SupplicantBridgeCallbacks() {
        }

        @Override
        public void notifyANQPResponse(ScanDetail scanDetail, Map<Constants.ANQPElementType, ANQPElement> anqpElements) throws Throwable {
            WifiConfigManager.this.updateAnqpCache(scanDetail, anqpElements);
            if (anqpElements == null || anqpElements.isEmpty()) {
                return;
            }
            scanDetail.propagateANQPInfo(anqpElements);
            Map<HomeSP, PasspointMatch> matches = WifiConfigManager.this.matchNetwork(scanDetail, false);
            Log.d(Utils.hs2LogTag(getClass()), scanDetail.getSSID() + " pass 2 matches: " + WifiConfigManager.toMatchString(matches));
            WifiConfigManager.this.cacheScanResultForPasspointConfigs(scanDetail, matches, null);
        }

        @Override
        public void notifyIconFailed(long bssid) {
            Intent intent = new Intent("android.net.wifi.PASSPOINT_ICON_RECEIVED");
            intent.addFlags(67108864);
            intent.putExtra("bssid", bssid);
            WifiConfigManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    WifiConfigManager(Context context, WifiNative wifiNative, FrameworkFacade facade, Clock clock, UserManager userManager, KeyStore keyStore) {
        SupplicantBridgeCallbacks supplicantBridgeCallbacks = null;
        this.mContext = context;
        this.mFacade = facade;
        this.mClock = clock;
        this.mKeyStore = keyStore;
        this.mUserManager = userManager;
        if (this.mShowNetworks) {
            this.mLocalLog = WifiNative.getLocalLog();
        } else {
            this.mLocalLog = null;
        }
        this.mOnlyLinkSameCredentialConfigurations = this.mContext.getResources().getBoolean(R.^attr-private.borderTop);
        this.mMaxNumActiveChannelsForPartialScans.set(this.mContext.getResources().getInteger(R.integer.config_bluetooth_operating_voltage_mv));
        this.mEnableLinkDebouncing = this.mContext.getResources().getBoolean(R.^attr-private.backgroundRequest);
        this.mBandAward5Ghz.set(this.mContext.getResources().getInteger(R.integer.config_activityShortDur));
        this.mThresholdMinimumRssi5.set(this.mContext.getResources().getInteger(R.integer.config_attentiveWarningDuration));
        this.mThresholdQualifiedRssi5.set(this.mContext.getResources().getInteger(R.integer.config_audio_alarm_min_vol));
        this.mThresholdSaturatedRssi5.set(this.mContext.getResources().getInteger(R.integer.config_audio_notif_vol_default));
        this.mThresholdMinimumRssi24.set(this.mContext.getResources().getInteger(R.integer.config_audio_notif_vol_steps));
        this.mThresholdQualifiedRssi24.set(this.mContext.getResources().getInteger(R.integer.config_audio_ring_vol_default));
        this.mThresholdSaturatedRssi24.set(this.mContext.getResources().getInteger(R.integer.config_audio_ring_vol_steps));
        this.mEnableWifiCellularHandoverUserTriggeredAdjustment = this.mContext.getResources().getBoolean(R.^attr-private.borderLeft);
        this.mBadLinkSpeed24 = this.mContext.getResources().getInteger(R.integer.config_autoBrightnessBrighteningLightDebounce);
        this.mBadLinkSpeed5 = this.mContext.getResources().getInteger(R.integer.config_autoBrightnessDarkeningLightDebounce);
        this.mGoodLinkSpeed24 = this.mContext.getResources().getInteger(R.integer.config_autoBrightnessInitialLightSensorRate);
        this.mGoodLinkSpeed5 = this.mContext.getResources().getInteger(R.integer.config_autoBrightnessLightSensorRate);
        this.mEnableAutoJoinWhenAssociated.set(this.mContext.getResources().getBoolean(R.^attr-private.borderRight));
        this.mCurrentNetworkBoost.set(this.mContext.getResources().getInteger(R.integer.config_bluetooth_rx_cur_ma));
        this.mNetworkSwitchingBlackListPeriodMs = this.mContext.getResources().getInteger(R.integer.config_batterySaver_full_soundTriggerMode);
        boolean hs2on = this.mContext.getResources().getBoolean(R.^attr-private.autofillSaveCustomSubtitleMaxHeight);
        Log.d(Utils.hs2LogTag(getClass()), "Passpoint is " + (hs2on ? "enabled" : "disabled"));
        if (SystemProperties.get("persist.wifi.hs20.test.mode").equals("1")) {
            log("In HS20 test mode. enable hs2on");
            hs2on = true;
        }
        this.mConfiguredNetworks = new ConfigurationMap(userManager);
        this.mMOManager = new PasspointManagementObjectManager(new File(PPS_FILE), hs2on);
        this.mEnableOsuQueries = true;
        this.mAnqpCache = new AnqpCache(this.mClock);
        this.mSupplicantBridgeCallbacks = new SupplicantBridgeCallbacks(this, supplicantBridgeCallbacks);
        this.mSupplicantBridge = new SupplicantBridge(wifiNative, this.mSupplicantBridgeCallbacks);
        this.mScanDetailCaches = new ConcurrentHashMap<>(16, 0.75f, 2);
        this.mSIMAccessor = new SIMAccessor(this.mContext);
        this.mWriter = new DelayedDiskWrite();
        this.mIpconfigStore = new IpConfigStore(this.mWriter);
        this.mWifiNetworkHistory = new WifiNetworkHistory(context, this.mLocalLog, this.mWriter);
        this.mWifiConfigStore = new WifiConfigStore(wifiNative, this.mKeyStore, this.mLocalLog, this.mShowNetworks, true);
    }

    public void trimANQPCache(boolean all) {
        this.mAnqpCache.clear(all, true);
    }

    void enableVerboseLogging(int verbose) {
        this.mEnableVerboseLogging.set(verbose);
        if (verbose > 0) {
            sVDBG = true;
            this.mShowNetworks = true;
        } else {
            sVDBG = false;
        }
        if (verbose > 0) {
            sVVDBG = true;
        } else {
            sVVDBG = false;
        }
        this.mShowNetworks = true;
        sVDBG = true;
    }

    void loadAndEnableAllNetworks() {
        log("Loading config and enabling all networks ");
        loadConfiguredNetworks();
        synchronized (this.mDisconnectNetworks) {
            this.mDisconnectNetworks.clear();
        }
        enableAllNetworks();
        if (this.mWifiFwkExt == null || this.mWifiFwkExt.hasNetworkSelection() != 3) {
            return;
        }
        removeUserSelectionPreference("ALL");
    }

    int getConfiguredNetworksSize() {
        return this.mConfiguredNetworks.sizeForCurrentUser();
    }

    List<WifiConfiguration> getConfiguredNetworks() {
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            WifiConfiguration newConfig = new WifiConfiguration(config);
            if (!config.ephemeral) {
                networks.add(newConfig);
            }
        }
        return networks;
    }

    private List<WifiConfiguration> getSavedNetworks(Map<String, String> pskMap) {
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            WifiConfiguration newConfig = new WifiConfiguration(config);
            if (!config.ephemeral) {
                if (pskMap != null && config.allowedKeyManagement != null && config.allowedKeyManagement.get(1) && pskMap.containsKey(config.configKey(true))) {
                    newConfig.preSharedKey = pskMap.get(config.configKey(true));
                }
                networks.add(newConfig);
            }
        }
        return networks;
    }

    private List<WifiConfiguration> getAllConfiguredNetworks() {
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            WifiConfiguration newConfig = new WifiConfiguration(config);
            networks.add(newConfig);
        }
        return networks;
    }

    public List<WifiConfiguration> getSavedNetworks() {
        return getSavedNetworks(null);
    }

    List<WifiConfiguration> getPrivilegedSavedNetworks() {
        Map<String, String> pskMap = getCredentialsByConfigKeyMap();
        List<WifiConfiguration> configurations = getSavedNetworks(pskMap);
        for (WifiConfiguration configuration : configurations) {
            try {
                configuration.setPasspointManagementObjectTree(this.mMOManager.getMOTree(configuration.FQDN));
            } catch (IOException ioe) {
                Log.w(TAG, "Failed to parse MO from " + configuration.FQDN + ": " + ioe);
            }
        }
        return configurations;
    }

    public Set<Integer> getHiddenConfiguredNetworkIds() {
        return this.mConfiguredNetworks.getHiddenNetworkIdsForCurrentUser();
    }

    WifiConfiguration getMatchingConfig(ScanResult scanResult) {
        if (scanResult == null) {
            return null;
        }
        Iterator entry$iterator = this.mScanDetailCaches.entrySet().iterator();
        while (entry$iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) entry$iterator.next();
            Integer netId = (Integer) entry.getKey();
            ScanDetailCache cache = (ScanDetailCache) entry.getValue();
            WifiConfiguration config = getWifiConfiguration(netId.intValue());
            if (config != null && cache.get(scanResult.BSSID) != null) {
                return config;
            }
        }
        return null;
    }

    private Map<String, String> getCredentialsByConfigKeyMap() {
        return readNetworkVariablesFromSupplicantFile("psk");
    }

    List<WifiConfiguration> getRecentSavedNetworks(int scanResultAgeMs, boolean copy) {
        ScanDetailCache cache;
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            if (!config.ephemeral && (cache = getScanDetailCache(config)) != null) {
                config.setVisibility(cache.getVisibility(scanResultAgeMs));
                if (config.visibility != null && (config.visibility.rssi5 != WifiConfiguration.INVALID_RSSI || config.visibility.rssi24 != WifiConfiguration.INVALID_RSSI)) {
                    if (copy) {
                        networks.add(new WifiConfiguration(config));
                    } else {
                        networks.add(config);
                    }
                }
            }
        }
        return networks;
    }

    void updateConfiguration(WifiInfo info) {
        ScanDetail scanDetail;
        WifiConfiguration config = getWifiConfiguration(info.getNetworkId());
        if (config == null || getScanDetailCache(config) == null || (scanDetail = getScanDetailCache(config).getScanDetail(info.getBSSID())) == null) {
            return;
        }
        ScanResult result = scanDetail.getScanResult();
        long previousSeen = result.seen;
        int previousRssi = result.level;
        scanDetail.setSeen();
        result.level = info.getRssi();
        result.averageRssi(previousRssi, previousSeen, WifiQualifiedNetworkSelector.SCAN_RESULT_MAXIMUNM_AGE);
        if (sVDBG) {
            logd("updateConfiguration freq=" + result.frequency + " BSSID=" + result.BSSID + " RSSI=" + result.level + " " + config.configKey());
        }
    }

    public WifiConfiguration getWifiConfiguration(int netId) {
        return this.mConfiguredNetworks.getForCurrentUser(netId);
    }

    public WifiConfiguration getWifiConfiguration(String key) {
        return this.mConfiguredNetworks.getByConfigKeyForCurrentUser(key);
    }

    void enableAllNetworks() {
        boolean networkEnabledStateChanged = false;
        if (this.mWifiFwkExt != null && this.mWifiFwkExt.hasCustomizedAutoConnect()) {
            if (this.mWifiFwkExt.shouldAutoConnect()) {
                List<Integer> disconnectNetworks = getDisconnectNetworks();
                for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
                    if (config != null && !config.ephemeral && !config.getNetworkSelectionStatus().isNetworkEnabled() && !disconnectNetworks.contains(Integer.valueOf(config.networkId)) && tryEnableQualifiedNetwork(config)) {
                        networkEnabledStateChanged = true;
                    }
                }
            }
        } else {
            for (WifiConfiguration config2 : this.mConfiguredNetworks.valuesForCurrentUser()) {
                if (config2 != null && !config2.ephemeral && !config2.getNetworkSelectionStatus().isNetworkEnabled() && tryEnableQualifiedNetwork(config2)) {
                    networkEnabledStateChanged = true;
                }
            }
        }
        if (!networkEnabledStateChanged) {
            return;
        }
        saveConfig();
        sendConfiguredNetworksChangedBroadcast();
    }

    private boolean setNetworkPriorityNative(WifiConfiguration config, int priority) {
        return this.mWifiConfigStore.setNetworkPriority(config, priority);
    }

    private boolean setSSIDNative(WifiConfiguration config, String ssid) {
        return this.mWifiConfigStore.setNetworkSSID(config, ssid);
    }

    public boolean updateLastConnectUid(WifiConfiguration config, int uid) {
        if (config != null && config.lastConnectUid != uid) {
            config.lastConnectUid = uid;
            return true;
        }
        return false;
    }

    boolean selectNetwork(WifiConfiguration config, boolean updatePriorities, int uid) {
        if (sVDBG) {
            localLogNetwork("selectNetwork", config.networkId);
        }
        if (config.networkId == -1) {
            return false;
        }
        if (!WifiConfigurationUtil.isVisibleToAnyProfile(config, this.mUserManager.getProfiles(this.mCurrentUserId))) {
            loge("selectNetwork " + Integer.toString(config.networkId) + ": Network config is not visible to current user.");
            return false;
        }
        if (this.mLastPriority == -1 || this.mLastPriority > 1000000) {
            if (updatePriorities) {
                for (WifiConfiguration config2 : this.mConfiguredNetworks.valuesForCurrentUser()) {
                    if (config2.networkId != -1) {
                        setNetworkPriorityNative(config2, 0);
                    }
                }
            }
            this.mLastPriority = 0;
        }
        if (updatePriorities) {
            int i = this.mLastPriority + 1;
            this.mLastPriority = i;
            setNetworkPriorityNative(config, i);
        }
        if (config.isPasspoint()) {
            if (getScanDetailCache(config).size() != 0) {
                ScanDetail result = getScanDetailCache(config).getFirst();
                if (result == null) {
                    loge("Could not find scan result for " + config.BSSID);
                } else {
                    logd("Setting SSID for " + config.networkId + " to" + result.getSSID());
                    setSSIDNative(config, result.getSSID());
                }
            } else {
                loge("Could not find bssid for " + config);
            }
        }
        this.mWifiConfigStore.enableHS20(config.isPasspoint());
        if (updatePriorities) {
            saveConfig();
        }
        updateLastConnectUid(config, uid);
        writeKnownNetworkHistory();
        selectNetworkWithoutBroadcast(config.networkId);
        return true;
    }

    NetworkUpdateResult saveNetwork(WifiConfiguration config, int uid) {
        if (config == null || (config.networkId == -1 && config.SSID == null)) {
            return new NetworkUpdateResult(-1);
        }
        if (!WifiConfigurationUtil.isVisibleToAnyProfile(config, this.mUserManager.getProfiles(this.mCurrentUserId))) {
            return new NetworkUpdateResult(-1);
        }
        if (sVDBG) {
            localLogNetwork("WifiConfigManager: saveNetwork netId", config.networkId);
        }
        if (sVDBG) {
            logd("WifiConfigManager saveNetwork, size=" + Integer.toString(this.mConfiguredNetworks.sizeForAllUsers()) + " (for all users) SSID=" + config.SSID + " Uid=" + Integer.toString(config.creatorUid) + "/" + Integer.toString(config.lastUpdateUid));
        }
        if (this.mDeletedEphemeralSSIDs.remove(config.SSID) && sVDBG) {
            logd("WifiConfigManager: removed from ephemeral blacklist: " + config.SSID);
        }
        if (config.networkId == -1) {
        }
        NetworkUpdateResult result = addOrUpdateNetworkNative(config, uid);
        int netId = result.getNetworkId();
        if (sVDBG) {
            localLogNetwork("WifiConfigManager: saveNetwork got it back netId=", netId);
        }
        WifiConfiguration conf = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (conf != null) {
            if (!conf.getNetworkSelectionStatus().isNetworkEnabled()) {
                if (sVDBG) {
                    localLog("WifiConfigManager: re-enabling: " + conf.SSID);
                }
                updateNetworkSelectionStatus(netId, 0);
            }
            if (sVDBG) {
                logd("WifiConfigManager: saveNetwork got config back netId=" + Integer.toString(netId) + " uid=" + Integer.toString(config.creatorUid));
            }
        }
        saveConfig();
        sendConfiguredNetworksChangedBroadcast(conf, result.isNewNetwork() ? 0 : 2);
        return result;
    }

    void noteRoamingFailure(WifiConfiguration config, int reason) {
        if (config == null) {
            return;
        }
        config.lastRoamingFailure = this.mClock.currentTimeMillis();
        config.roamingFailureBlackListTimeMilli = (config.roamingFailureBlackListTimeMilli + 1000) * 2;
        if (config.roamingFailureBlackListTimeMilli > this.mNetworkSwitchingBlackListPeriodMs) {
            config.roamingFailureBlackListTimeMilli = this.mNetworkSwitchingBlackListPeriodMs;
        }
        config.lastRoamingFailureReason = reason;
    }

    void saveWifiConfigBSSID(WifiConfiguration config, String bssid) {
        this.mWifiConfigStore.setNetworkBSSID(config, bssid);
    }

    void updateStatus(int netId, NetworkInfo.DetailedState state) {
        WifiConfiguration config;
        if (netId == -1 || (config = this.mConfiguredNetworks.getForAllUsers(netId)) == null) {
        }
        switch (m41getandroidnetNetworkInfo$DetailedStateSwitchesValues()[state.ordinal()]) {
            case 1:
                config.status = 0;
                updateNetworkSelectionStatus(netId, 0);
                break;
            case 2:
                if (config.status == 0) {
                    config.status = 2;
                }
                break;
        }
    }

    WifiConfiguration disableEphemeralNetwork(String ssid) {
        if (ssid == null) {
            return null;
        }
        WifiConfiguration foundConfig = this.mConfiguredNetworks.getEphemeralForCurrentUser(ssid);
        this.mDeletedEphemeralSSIDs.add(ssid);
        logd("Forget ephemeral SSID " + ssid + " num=" + this.mDeletedEphemeralSSIDs.size());
        if (foundConfig != null) {
            logd("Found ephemeral config in disableEphemeralNetwork: " + foundConfig.networkId);
        }
        writeKnownNetworkHistory();
        return foundConfig;
    }

    boolean forgetNetwork(int netId) {
        if (this.mShowNetworks) {
            localLogNetwork("forgetNetwork", netId);
        }
        if (!removeNetwork(netId)) {
            loge("Failed to forget network " + netId);
            return false;
        }
        saveConfig();
        writeKnownNetworkHistory();
        return true;
    }

    int addOrUpdateNetwork(WifiConfiguration config, int uid) {
        WifiConfiguration conf;
        int i;
        if (config == null || !WifiConfigurationUtil.isVisibleToAnyProfile(config, this.mUserManager.getProfiles(this.mCurrentUserId))) {
            return -1;
        }
        if (this.mShowNetworks) {
            localLogNetwork("addOrUpdateNetwork id=", config.networkId);
        }
        if (config.isPasspoint()) {
            Long csum = getChecksum(config.FQDN);
            config.SSID = csum.toString();
            config.enterpriseConfig.setDomainSuffixMatch(config.FQDN);
        }
        NetworkUpdateResult result = addOrUpdateNetworkNative(config, uid);
        if (result.getNetworkId() != -1 && (conf = this.mConfiguredNetworks.getForCurrentUser(result.getNetworkId())) != null) {
            if (result.isNewNetwork) {
                i = 0;
            } else {
                i = 2;
            }
            sendConfiguredNetworksChangedBroadcast(conf, i);
        }
        return result.getNetworkId();
    }

    public int addPasspointManagementObject(String managementObject) {
        try {
            this.mMOManager.addSP(managementObject);
            return 0;
        } catch (IOException | SAXException e) {
            return -1;
        }
    }

    public int modifyPasspointMo(String fqdn, List<PasspointManagementObjectDefinition> mos) {
        try {
            return this.mMOManager.modifySP(fqdn, mos);
        } catch (IOException | SAXException e) {
            return -1;
        }
    }

    public boolean queryPasspointIcon(long bssid, String fileName) {
        return this.mSupplicantBridge.doIconQuery(bssid, fileName);
    }

    public int matchProviderWithCurrentNetwork(String fqdn) {
        ScanDetail scanDetail;
        synchronized (this.mActiveScanDetailLock) {
            scanDetail = this.mActiveScanDetail;
        }
        if (scanDetail == null) {
            return PasspointMatch.None.ordinal();
        }
        HomeSP homeSP = this.mMOManager.getHomeSP(fqdn);
        if (homeSP == null) {
            return PasspointMatch.None.ordinal();
        }
        ANQPData anqpData = this.mAnqpCache.getEntry(scanDetail.getNetworkDetail());
        return homeSP.match(scanDetail.getNetworkDetail(), anqpData != null ? anqpData.getANQPElements() : null, this.mSIMAccessor).ordinal();
    }

    private static class PnoListComparator implements Comparator<WifiConfiguration> {
        public final int ENABLED_NETWORK_SCORE;
        public final int PERMANENTLY_DISABLED_NETWORK_SCORE;
        public final int TEMPORARY_DISABLED_NETWORK_SCORE;

        PnoListComparator(PnoListComparator pnoListComparator) {
            this();
        }

        private PnoListComparator() {
            this.ENABLED_NETWORK_SCORE = 3;
            this.TEMPORARY_DISABLED_NETWORK_SCORE = 2;
            this.PERMANENTLY_DISABLED_NETWORK_SCORE = 1;
        }

        @Override
        public int compare(WifiConfiguration a, WifiConfiguration b) {
            int configAScore = getPnoNetworkSortScore(a);
            int configBScore = getPnoNetworkSortScore(b);
            if (configAScore == configBScore) {
                return compareConfigurations(a, b);
            }
            return Integer.compare(configBScore, configAScore);
        }

        public int compareConfigurations(WifiConfiguration a, WifiConfiguration b) {
            return 0;
        }

        private int getPnoNetworkSortScore(WifiConfiguration config) {
            if (config.getNetworkSelectionStatus().isNetworkEnabled()) {
                return 3;
            }
            if (config.getNetworkSelectionStatus().isNetworkTemporaryDisabled()) {
                return 2;
            }
            return 1;
        }
    }

    public ArrayList<WifiScanner.PnoSettings.PnoNetwork> retrieveDisconnectedPnoNetworkList() {
        return retrievePnoNetworkList(sDisconnectedPnoListComparator);
    }

    public ArrayList<WifiScanner.PnoSettings.PnoNetwork> retrieveConnectedPnoNetworkList() {
        return retrievePnoNetworkList(sConnectedPnoListComparator);
    }

    private static WifiScanner.PnoSettings.PnoNetwork createPnoNetworkFromWifiConfiguration(WifiConfiguration config, int newPriority) {
        WifiScanner.PnoSettings.PnoNetwork pnoNetwork = new WifiScanner.PnoSettings.PnoNetwork(config.SSID);
        pnoNetwork.networkId = config.networkId;
        pnoNetwork.priority = newPriority;
        if (config.hiddenSSID) {
            pnoNetwork.flags = (byte) (pnoNetwork.flags | 1);
        }
        pnoNetwork.flags = (byte) (pnoNetwork.flags | 2);
        pnoNetwork.flags = (byte) (pnoNetwork.flags | 4);
        if (config.allowedKeyManagement.get(1)) {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 2);
        } else if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 4);
        } else {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 1);
        }
        return pnoNetwork;
    }

    private ArrayList<WifiScanner.PnoSettings.PnoNetwork> retrievePnoNetworkList(PnoListComparator pnoListComparator) {
        ArrayList<WifiScanner.PnoSettings.PnoNetwork> pnoList = new ArrayList<>();
        ArrayList<WifiConfiguration> wifiConfigurations = new ArrayList<>(this.mConfiguredNetworks.valuesForCurrentUser());
        Collections.sort(wifiConfigurations, pnoListComparator);
        int priority = wifiConfigurations.size();
        for (WifiConfiguration config : wifiConfigurations) {
            pnoList.add(createPnoNetworkFromWifiConfiguration(config, priority));
            priority--;
        }
        return pnoList;
    }

    boolean removeNetwork(int netId) {
        if (this.mShowNetworks) {
            localLogNetwork("removeNetwork", netId);
        }
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (!removeConfigAndSendBroadcastIfNeeded(config)) {
            return false;
        }
        if (config.isPasspoint()) {
            writePasspointConfigs(config.FQDN, null);
            return true;
        }
        return true;
    }

    private static Long getChecksum(String source) {
        Checksum csum = new CRC32();
        csum.update(source.getBytes(), 0, source.getBytes().length);
        return Long.valueOf(csum.getValue());
    }

    private boolean removeConfigWithoutBroadcast(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        if (!this.mWifiConfigStore.removeNetwork(config)) {
            loge("Failed to remove network " + config.networkId);
            return false;
        }
        if (config.configKey().equals(this.mLastSelectedConfiguration)) {
            this.mLastSelectedConfiguration = null;
        }
        this.mConfiguredNetworks.remove(config.networkId);
        this.mScanDetailCaches.remove(Integer.valueOf(config.networkId));
        return true;
    }

    private boolean removeConfigAndSendBroadcastIfNeeded(WifiConfiguration config) {
        if (!removeConfigWithoutBroadcast(config)) {
            return false;
        }
        String key = config.configKey();
        if (sVDBG) {
            logd("removeNetwork  key=" + key + " config.id=" + config.networkId);
        }
        writeIpAndProxyConfigurations();
        sendConfiguredNetworksChangedBroadcast(config, 1);
        if (!config.ephemeral) {
            removeUserSelectionPreference(key);
        }
        writeKnownNetworkHistory();
        return true;
    }

    private void removeUserSelectionPreference(String configKey) {
        Log.d(TAG, "removeUserSelectionPreference: key is " + configKey);
        if (configKey == null) {
            return;
        }
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            WifiConfiguration.NetworkSelectionStatus status = config.getNetworkSelectionStatus();
            String connectChoice = status.getConnectChoice();
            if (connectChoice != null && (configKey.equals("ALL") || connectChoice.equals(configKey))) {
                Log.d(TAG, "remove connect choice:" + connectChoice + " from " + config.SSID + " : " + config.networkId);
                status.setConnectChoice((String) null);
                status.setConnectChoiceTimestamp(-1L);
            }
        }
    }

    boolean removeNetworksForApp(ApplicationInfo app) {
        if (app == null || app.packageName == null) {
            return false;
        }
        boolean success = true;
        WifiConfiguration[] copiedConfigs = (WifiConfiguration[]) this.mConfiguredNetworks.valuesForCurrentUser().toArray(new WifiConfiguration[0]);
        for (WifiConfiguration config : copiedConfigs) {
            if (app.uid == config.creatorUid && app.packageName.equals(config.creatorName)) {
                if (this.mShowNetworks) {
                    localLog("Removing network " + config.SSID + ", application \"" + app.packageName + "\" uninstalled from user " + UserHandle.getUserId(app.uid));
                }
                success &= removeNetwork(config.networkId);
            }
        }
        saveConfig();
        return success;
    }

    boolean removeNetworksForUser(int userId) {
        boolean success = true;
        WifiConfiguration[] copiedConfigs = (WifiConfiguration[]) this.mConfiguredNetworks.valuesForAllUsers().toArray(new WifiConfiguration[0]);
        for (WifiConfiguration config : copiedConfigs) {
            if (userId == UserHandle.getUserId(config.creatorUid)) {
                success &= removeNetwork(config.networkId);
                if (this.mShowNetworks) {
                    localLog("Removing network " + config.SSID + ", user " + userId + " removed");
                }
            }
        }
        saveConfig();
        return success;
    }

    boolean enableNetwork(WifiConfiguration config, boolean disableOthers, int uid) {
        if (config == null) {
            return false;
        }
        updateNetworkSelectionStatus(config, 0);
        setLatestUserSelectedConfiguration(config);
        boolean ret = true;
        if (disableOthers) {
            ret = selectNetworkWithoutBroadcast(config.networkId);
            if (sVDBG) {
                localLogNetwork("enableNetwork(disableOthers=true, uid=" + uid + ") ", config.networkId);
            }
            updateLastConnectUid(config, uid);
            writeKnownNetworkHistory();
            sendConfiguredNetworksChangedBroadcast();
        } else {
            if (sVDBG) {
                localLogNetwork("enableNetwork(disableOthers=false) ", config.networkId);
            }
            sendConfiguredNetworksChangedBroadcast(config, 2);
        }
        return ret;
    }

    boolean selectNetworkWithoutBroadcast(int netId) {
        return this.mWifiConfigStore.selectNetwork(this.mConfiguredNetworks.getForCurrentUser(netId), this.mConfiguredNetworks.valuesForCurrentUser());
    }

    boolean disableNetworkNative(WifiConfiguration config) {
        return this.mWifiConfigStore.disableNetwork(config);
    }

    void disableAllNetworksNative() {
        this.mWifiConfigStore.disableAllNetworks(this.mConfiguredNetworks.valuesForCurrentUser());
    }

    boolean disableNetwork(int netId) {
        return this.mWifiConfigStore.disableNetwork(this.mConfiguredNetworks.getForCurrentUser(netId));
    }

    boolean updateNetworkSelectionStatus(int netId, int reason) {
        WifiConfiguration config = getWifiConfiguration(netId);
        return updateNetworkSelectionStatus(config, reason);
    }

    boolean updateNetworkSelectionStatus(WifiConfiguration config, int reason) {
        if (config == null) {
            return false;
        }
        WifiConfiguration.NetworkSelectionStatus networkStatus = config.getNetworkSelectionStatus();
        if (reason == 0) {
            updateNetworkStatus(config, 0);
            localLog("Enable network:" + config.configKey());
            return true;
        }
        networkStatus.incrementDisableReasonCounter(reason);
        localLog("Network:" + config.SSID + "disable counter of " + WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(reason) + " is: " + networkStatus.getDisableReasonCounter(reason) + "and threshold is: " + NETWORK_SELECTION_DISABLE_THRESHOLD[reason]);
        if (networkStatus.getDisableReasonCounter(reason) >= NETWORK_SELECTION_DISABLE_THRESHOLD[reason]) {
            return updateNetworkStatus(config, reason);
        }
        return true;
    }

    public boolean tryEnableQualifiedNetwork(int networkId) {
        WifiConfiguration config = getWifiConfiguration(networkId);
        if (config == null) {
            localLog("updateQualifiedNetworkstatus invalid network.");
            return false;
        }
        return tryEnableQualifiedNetwork(config);
    }

    private boolean tryEnableQualifiedNetwork(WifiConfiguration config) {
        WifiConfiguration.NetworkSelectionStatus networkStatus = config.getNetworkSelectionStatus();
        if (networkStatus.isNetworkTemporaryDisabled()) {
            long timeDifference = ((this.mClock.elapsedRealtime() - networkStatus.getDisableTime()) / 1000) / 60;
            if (timeDifference < 0 || timeDifference >= NETWORK_SELECTION_DISABLE_TIMEOUT[networkStatus.getNetworkSelectionDisableReason()]) {
                updateNetworkSelectionStatus(config.networkId, 0);
                return true;
            }
        }
        return false;
    }

    boolean updateNetworkStatus(WifiConfiguration config, int reason) {
        localLog("updateNetworkStatus:" + (config != null ? config.SSID : null));
        if (config == null) {
            return false;
        }
        WifiConfiguration.NetworkSelectionStatus networkStatus = config.getNetworkSelectionStatus();
        if (reason < 0 || reason >= 11) {
            localLog("Invalid Network disable reason:" + reason);
            return false;
        }
        if (reason == 0) {
            if (networkStatus.isNetworkEnabled()) {
                localLog("Need not change Qualified network Selection status since already enabled");
                return false;
            }
            networkStatus.setNetworkSelectionStatus(0);
            networkStatus.setNetworkSelectionDisableReason(reason);
            networkStatus.setDisableTime(-1L);
            networkStatus.clearDisableReasonCounter();
            String disableTime = DateFormat.getDateTimeInstance().format(new Date());
            localLog("Re-enable network: " + config.SSID + " at " + disableTime);
            sendConfiguredNetworksChangedBroadcast(config, 2);
        } else {
            if (networkStatus.isNetworkPermanentlyDisabled()) {
                localLog("Do nothing. Alreay permanent disabled! " + WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(reason));
                return false;
            }
            if (networkStatus.isNetworkTemporaryDisabled() && reason < 6) {
                localLog("Do nothing. Already temporarily disabled! " + WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(reason));
                return false;
            }
            if (networkStatus.isNetworkEnabled()) {
                if (reason == 9) {
                    disableNetworkNative(config);
                } else {
                    localLog("Don't disable native for prevent supplicant cancel sched scan");
                    config.status = 1;
                }
                sendConfiguredNetworksChangedBroadcast(config, 2);
                localLog("Disable network " + config.SSID + " reason:" + WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(reason));
            }
            if (reason < 6) {
                networkStatus.setNetworkSelectionStatus(1);
                networkStatus.setDisableTime(this.mClock.elapsedRealtime());
            } else {
                networkStatus.setNetworkSelectionStatus(2);
            }
            networkStatus.setNetworkSelectionDisableReason(reason);
            String disableTime2 = DateFormat.getDateTimeInstance().format(new Date());
            localLog("Network:" + config.SSID + "Configure new status:" + networkStatus.getNetworkStatusString() + " with reason:" + networkStatus.getNetworkDisableReasonString() + " at: " + disableTime2);
        }
        return true;
    }

    boolean saveConfig() {
        return this.mWifiConfigStore.saveConfig();
    }

    WpsResult startWpsWithPinFromAccessPoint(WpsInfo config) {
        return this.mWifiConfigStore.startWpsWithPinFromAccessPoint(config, this.mConfiguredNetworks.valuesForCurrentUser());
    }

    WpsResult startWpsWithPinFromDevice(WpsInfo config) {
        return this.mWifiConfigStore.startWpsWithPinFromDevice(config, this.mConfiguredNetworks.valuesForCurrentUser());
    }

    WpsResult startWpsPbc(WpsInfo config) {
        return this.mWifiConfigStore.startWpsPbc(config, this.mConfiguredNetworks.valuesForCurrentUser());
    }

    StaticIpConfiguration getStaticIpConfiguration(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config != null) {
            return config.getStaticIpConfiguration();
        }
        return null;
    }

    void setStaticIpConfiguration(int netId, StaticIpConfiguration staticIpConfiguration) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config == null) {
            return;
        }
        config.setStaticIpConfiguration(staticIpConfiguration);
    }

    void setDefaultGwMacAddress(int netId, String macAddress) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config == null) {
            return;
        }
        config.defaultGwMacAddress = macAddress;
    }

    ProxyInfo getProxyProperties(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config != null) {
            return config.getHttpProxy();
        }
        return null;
    }

    boolean isUsingStaticIp(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config != null && config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
            return true;
        }
        return false;
    }

    boolean isEphemeral(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config != null) {
            return config.ephemeral;
        }
        return false;
    }

    boolean getMeteredHint(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config != null) {
            return config.meteredHint;
        }
        return false;
    }

    private void sendConfiguredNetworksChangedBroadcast(WifiConfiguration network, int reason) {
        Intent intent = new Intent("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("multipleChanges", false);
        intent.putExtra("wifiConfiguration", network);
        intent.putExtra("changeReason", reason);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendConfiguredNetworksChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("multipleChanges", true);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    void loadConfiguredNetworks() {
        Map<String, WifiConfiguration> configs = new HashMap<>();
        SparseArray<Map<String, String>> networkExtras = new SparseArray<>();
        this.mLastPriority = this.mWifiConfigStore.loadNetworks(configs, networkExtras);
        readNetworkHistory(configs);
        readPasspointConfig(configs, networkExtras);
        this.mConfiguredNetworks.clear();
        for (Map.Entry<String, WifiConfiguration> entry : configs.entrySet()) {
            String configKey = entry.getKey();
            WifiConfiguration config = entry.getValue();
            if (!configKey.equals(config.configKey())) {
                if (this.mShowNetworks) {
                    log("Ignoring network " + config.networkId + " because the configKey loaded from wpa_supplicant.conf is not valid.");
                }
                this.mWifiConfigStore.removeNetwork(config);
            } else {
                this.mConfiguredNetworks.put(config);
            }
        }
        readIpAndProxyConfigurations();
        sendConfiguredNetworksChangedBroadcast();
        if (this.mShowNetworks) {
            localLog("loadConfiguredNetworks loaded " + this.mConfiguredNetworks.sizeForAllUsers() + " networks (for all users)");
        }
        if (this.mConfiguredNetworks.sizeForAllUsers() != 0) {
            return;
        }
        logKernelTime();
        logContents(WifiConfigStore.SUPPLICANT_CONFIG_FILE);
        logContents(WifiConfigStore.SUPPLICANT_CONFIG_FILE_BACKUP);
        logContents(WifiNetworkHistory.NETWORK_HISTORY_CONFIG_FILE);
    }

    private void logContents(String file) throws Throwable {
        BufferedReader reader;
        localLogAndLogcat("--- Begin " + file + " ---");
        BufferedReader reader2 = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(file));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (IOException e2) {
            e = e2;
        }
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                localLogAndLogcat(line);
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                }
            }
            reader2 = reader;
        } catch (FileNotFoundException e4) {
            e = e4;
            reader2 = reader;
            localLog("Could not open " + file + ", " + e);
            Log.w(TAG, "Could not open " + file + ", " + e);
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e5) {
                }
            }
        } catch (IOException e6) {
            e = e6;
            reader2 = reader;
            localLog("Could not read " + file + ", " + e);
            Log.w(TAG, "Could not read " + file + ", " + e);
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e7) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            reader2 = reader;
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
        localLogAndLogcat("--- End " + file + " Contents ---");
    }

    private Map<String, String> readNetworkVariablesFromSupplicantFile(String key) {
        return this.mWifiConfigStore.readNetworkVariablesFromSupplicantFile(key);
    }

    private String readNetworkVariableFromSupplicantFile(String configKey, String key) throws Throwable {
        long start = SystemClock.elapsedRealtimeNanos();
        Map<String, String> data = this.mWifiConfigStore.readNetworkVariablesFromSupplicantFile(key);
        long end = SystemClock.elapsedRealtimeNanos();
        if (sVDBG) {
            localLog("readNetworkVariableFromSupplicantFile configKey=[" + configKey + "] key=" + key + " duration=" + (end - start));
        }
        return data.get(configKey);
    }

    boolean needsUnlockedKeyStore() {
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            if (config.allowedKeyManagement.get(2) && config.allowedKeyManagement.get(3) && needsSoftwareBackedKeyStore(config.enterpriseConfig)) {
                return true;
            }
        }
        return false;
    }

    void readPasspointConfig(Map<String, WifiConfiguration> configs, SparseArray<Map<String, String>> networkExtras) throws Throwable {
        String configFqdn;
        try {
            List<HomeSP> homeSPs = this.mMOManager.loadAllSPs();
            int matchedConfigs = 0;
            for (HomeSP homeSp : homeSPs) {
                String fqdn = homeSp.getFQDN();
                Log.d(TAG, "Looking for " + fqdn);
                for (WifiConfiguration config : configs.values()) {
                    Log.d(TAG, "Testing " + config.SSID);
                    if (config.enterpriseConfig != null && (configFqdn = networkExtras.get(config.networkId).get(WifiConfigStore.ID_STRING_KEY_FQDN)) != null && configFqdn.equals(fqdn)) {
                        Log.d(TAG, "Matched " + configFqdn + " with " + config.networkId);
                        matchedConfigs++;
                        config.FQDN = fqdn;
                        config.providerFriendlyName = homeSp.getFriendlyName();
                        HashSet<Long> roamingConsortiumIds = homeSp.getRoamingConsortiums();
                        config.roamingConsortiumIds = new long[roamingConsortiumIds.size()];
                        int i = 0;
                        Iterator id$iterator = roamingConsortiumIds.iterator();
                        while (id$iterator.hasNext()) {
                            long id = ((Long) id$iterator.next()).longValue();
                            config.roamingConsortiumIds[i] = id;
                            i++;
                        }
                        IMSIParameter imsiParameter = homeSp.getCredential().getImsi();
                        config.enterpriseConfig.setPlmn(imsiParameter != null ? imsiParameter.toString() : null);
                        config.enterpriseConfig.setRealm(homeSp.getCredential().getRealm());
                    }
                }
            }
            Log.d(TAG, "loaded " + matchedConfigs + " passpoint configs");
        } catch (IOException e) {
            loge("Could not read /data/misc/wifi/PerProviderSubscription.conf : " + e);
        }
    }

    public void writePasspointConfigs(final String fqdn, final HomeSP homeSP) {
        this.mWriter.write(PPS_FILE, new DelayedDiskWrite.Writer() {
            public void onWriteCalled(DataOutputStream out) throws Throwable {
                try {
                    if (homeSP != null) {
                        WifiConfigManager.this.mMOManager.addSP(homeSP);
                    } else {
                        WifiConfigManager.this.mMOManager.removeSP(fqdn);
                    }
                } catch (IOException e) {
                    WifiConfigManager.this.loge("Could not write /data/misc/wifi/PerProviderSubscription.conf : " + e);
                }
            }
        }, false);
    }

    private void readNetworkHistory(Map<String, WifiConfiguration> configs) throws Throwable {
        this.mWifiNetworkHistory.readNetworkHistory(configs, this.mScanDetailCaches, this.mDeletedEphemeralSSIDs);
    }

    public void writeKnownNetworkHistory() {
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForAllUsers()) {
            networks.add(new WifiConfiguration(config));
        }
        this.mWifiNetworkHistory.writeKnownNetworkHistory(networks, this.mScanDetailCaches, this.mDeletedEphemeralSSIDs);
    }

    public void setAndEnableLastSelectedConfiguration(int netId) {
        if (sVDBG) {
            logd("setLastSelectedConfiguration " + Integer.toString(netId));
        }
        if (netId == -1) {
            this.mLastSelectedConfiguration = null;
            this.mLastSelectedTimeStamp = -1L;
            return;
        }
        WifiConfiguration selected = getWifiConfiguration(netId);
        if (selected == null) {
            this.mLastSelectedConfiguration = null;
            this.mLastSelectedTimeStamp = -1L;
            return;
        }
        this.mLastSelectedConfiguration = selected.configKey();
        this.mLastSelectedTimeStamp = this.mClock.elapsedRealtime();
        updateNetworkSelectionStatus(netId, 0);
        if (!sVDBG) {
            return;
        }
        logd("setLastSelectedConfiguration now: " + this.mLastSelectedConfiguration);
    }

    public void setLatestUserSelectedConfiguration(WifiConfiguration network) {
        if (network == null) {
            return;
        }
        this.mLastSelectedConfiguration = network.configKey();
        this.mLastSelectedTimeStamp = this.mClock.elapsedRealtime();
    }

    public String getLastSelectedConfiguration() {
        return this.mLastSelectedConfiguration;
    }

    public long getLastSelectedTimeStamp() {
        return this.mLastSelectedTimeStamp;
    }

    public boolean isLastSelectedConfiguration(WifiConfiguration config) {
        if (this.mLastSelectedConfiguration == null || config == null) {
            return false;
        }
        return this.mLastSelectedConfiguration.equals(config.configKey());
    }

    private void writeIpAndProxyConfigurations() {
        SparseArray<IpConfiguration> networks = new SparseArray<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForAllUsers()) {
            if (!config.ephemeral) {
                networks.put(configKey(config), config.getIpConfiguration());
            }
        }
        this.mIpconfigStore.writeIpAndProxyConfigurations(IP_CONFIG_FILE, networks);
    }

    private void readIpAndProxyConfigurations() {
        SparseArray<IpConfiguration> networks = this.mIpconfigStore.readIpAndProxyConfigurations(IP_CONFIG_FILE);
        if (networks == null || networks.size() == 0) {
            return;
        }
        for (int i = 0; i < networks.size(); i++) {
            int id = networks.keyAt(i);
            WifiConfiguration config = this.mConfiguredNetworks.getByConfigKeyIDForAllUsers(id);
            if (config == null || config.ephemeral) {
                logd("configuration found for missing network, nid=" + id + ", ignored, networks.size=" + Integer.toString(networks.size()));
            } else {
                config.setIpConfiguration(networks.valueAt(i));
            }
        }
    }

    private NetworkUpdateResult addOrUpdateNetworkNative(WifiConfiguration config, int uid) {
        WifiConfiguration currentConfig;
        HomeSP homeSP;
        if (sVDBG) {
            localLog("addOrUpdateNetworkNative " + config.getPrintableSsid());
        }
        if (config.isPasspoint() && !this.mMOManager.isEnabled()) {
            Log.e(TAG, "Passpoint is not enabled");
            return new NetworkUpdateResult(-1);
        }
        boolean newNetwork = false;
        boolean existingMO = false;
        if (config.networkId == -1) {
            currentConfig = this.mConfiguredNetworks.getByConfigKeyForCurrentUser(config.configKey());
            if (currentConfig != null) {
                config.networkId = currentConfig.networkId;
            } else {
                if (this.mMOManager.getHomeSP(config.FQDN) != null) {
                    logd("addOrUpdateNetworkNative passpoint " + config.FQDN + " was found, but no network Id");
                    existingMO = true;
                }
                newNetwork = true;
            }
        } else {
            currentConfig = this.mConfiguredNetworks.getForCurrentUser(config.networkId);
        }
        WifiConfiguration originalConfig = new WifiConfiguration(currentConfig);
        if (!this.mWifiConfigStore.addOrUpdateNetwork(config, currentConfig)) {
            return new NetworkUpdateResult(-1);
        }
        int netId = config.networkId;
        String savedConfigKey = config.configKey();
        if (currentConfig == null) {
            currentConfig = new WifiConfiguration();
            currentConfig.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
            currentConfig.setProxySettings(IpConfiguration.ProxySettings.NONE);
            currentConfig.networkId = netId;
            if (config != null) {
                currentConfig.selfAdded = config.selfAdded;
                currentConfig.didSelfAdd = config.didSelfAdd;
                currentConfig.ephemeral = config.ephemeral;
                currentConfig.meteredHint = config.meteredHint;
                currentConfig.useExternalScores = config.useExternalScores;
                currentConfig.lastConnectUid = config.lastConnectUid;
                currentConfig.lastUpdateUid = config.lastUpdateUid;
                currentConfig.creatorUid = config.creatorUid;
                currentConfig.creatorName = config.creatorName;
                currentConfig.lastUpdateName = config.lastUpdateName;
                currentConfig.peerWifiConfiguration = config.peerWifiConfiguration;
                currentConfig.FQDN = config.FQDN;
                currentConfig.providerFriendlyName = config.providerFriendlyName;
                currentConfig.roamingConsortiumIds = config.roamingConsortiumIds;
                currentConfig.validatedInternetAccess = config.validatedInternetAccess;
                currentConfig.numNoInternetAccessReports = config.numNoInternetAccessReports;
                currentConfig.updateTime = config.updateTime;
                currentConfig.creationTime = config.creationTime;
                currentConfig.shared = config.shared;
            }
            log("created new config netId=" + Integer.toString(netId) + " uid=" + Integer.toString(currentConfig.creatorUid) + " name=" + currentConfig.creatorName);
        }
        if (existingMO || !config.isPasspoint()) {
            homeSP = null;
        } else {
            try {
                if (config.updateIdentifier == null) {
                    Credential credential = new Credential(config.enterpriseConfig, this.mKeyStore, !newNetwork);
                    HashSet<Long> roamingConsortiumIds = new HashSet<>();
                    for (long j : config.roamingConsortiumIds) {
                        Long roamingConsortiumId = Long.valueOf(j);
                        roamingConsortiumIds.add(roamingConsortiumId);
                    }
                    homeSP = new HomeSP(Collections.emptyMap(), config.FQDN, roamingConsortiumIds, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), config.providerFriendlyName, null, credential);
                    try {
                        log("created a homeSP object for " + config.networkId + ":" + config.SSID);
                    } catch (IOException e) {
                        ioe = e;
                        Log.e(TAG, "Failed to create Passpoint config: " + ioe);
                        return new NetworkUpdateResult(-1);
                    }
                } else {
                    homeSP = null;
                }
                currentConfig.enterpriseConfig.setRealm(config.enterpriseConfig.getRealm());
                currentConfig.enterpriseConfig.setPlmn(config.enterpriseConfig.getPlmn());
            } catch (IOException e2) {
                ioe = e2;
            }
        }
        if (uid != -1) {
            if (newNetwork) {
                currentConfig.creatorUid = uid;
            } else {
                currentConfig.lastUpdateUid = uid;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("time=");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.mClock.currentTimeMillis());
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
        if (newNetwork) {
            currentConfig.creationTime = sb.toString();
        } else {
            currentConfig.updateTime = sb.toString();
        }
        if (currentConfig.status == 2) {
            updateNetworkSelectionStatus(currentConfig.networkId, 0);
        }
        if (currentConfig.configKey().equals(getLastSelectedConfiguration()) && currentConfig.ephemeral) {
            currentConfig.ephemeral = false;
            log("remove ephemeral status netId=" + Integer.toString(netId) + " " + currentConfig.configKey());
        }
        if (sVDBG) {
            log("will read network variables netId=" + Integer.toString(netId));
        }
        readNetworkVariables(currentConfig);
        if (!savedConfigKey.equals(currentConfig.configKey()) && !this.mWifiConfigStore.saveNetworkMetadata(currentConfig)) {
            loge("Failed to set network metadata. Removing config " + config.networkId);
            this.mWifiConfigStore.removeNetwork(config);
            return new NetworkUpdateResult(-1);
        }
        boolean passwordChanged = false;
        if (!newNetwork && config.preSharedKey != null && !config.preSharedKey.equals("*")) {
            passwordChanged = true;
        }
        if (newNetwork || passwordChanged || wasCredentialChange(originalConfig, currentConfig)) {
            currentConfig.getNetworkSelectionStatus().setHasEverConnected(false);
        }
        if (config.lastUpdateName != null) {
            currentConfig.lastUpdateName = config.lastUpdateName;
        }
        if (config.lastUpdateUid != -1) {
            currentConfig.lastUpdateUid = config.lastUpdateUid;
        }
        this.mConfiguredNetworks.put(currentConfig);
        NetworkUpdateResult result = writeIpAndProxyConfigurationsOnChange(currentConfig, config, newNetwork);
        result.setIsNewNetwork(newNetwork);
        result.setNetworkId(netId);
        if (homeSP != null) {
            writePasspointConfigs(null, homeSP);
        }
        saveConfig();
        writeKnownNetworkHistory();
        return result;
    }

    private boolean wasBitSetUpdated(BitSet originalBitSet, BitSet currentBitSet) {
        return (originalBitSet == null || currentBitSet == null) ? (originalBitSet == null && currentBitSet == null) ? false : true : !originalBitSet.equals(currentBitSet);
    }

    private boolean wasCredentialChange(WifiConfiguration originalConfig, WifiConfiguration currentConfig) {
        if (originalConfig == null || wasBitSetUpdated(originalConfig.allowedKeyManagement, currentConfig.allowedKeyManagement) || wasBitSetUpdated(originalConfig.allowedProtocols, currentConfig.allowedProtocols) || wasBitSetUpdated(originalConfig.allowedAuthAlgorithms, currentConfig.allowedAuthAlgorithms) || wasBitSetUpdated(originalConfig.allowedPairwiseCiphers, currentConfig.allowedPairwiseCiphers) || wasBitSetUpdated(originalConfig.allowedGroupCiphers, currentConfig.allowedGroupCiphers)) {
            return true;
        }
        if (originalConfig.wepKeys != null && currentConfig.wepKeys != null) {
            if (originalConfig.wepKeys.length != currentConfig.wepKeys.length) {
                return true;
            }
            for (int i = 0; i < originalConfig.wepKeys.length; i++) {
                if (!Objects.equals(originalConfig.wepKeys[i], currentConfig.wepKeys[i])) {
                    return true;
                }
            }
        }
        return (originalConfig.hiddenSSID == currentConfig.hiddenSSID && originalConfig.requirePMF == currentConfig.requirePMF && !wasEnterpriseConfigChange(originalConfig.enterpriseConfig, currentConfig.enterpriseConfig)) ? false : true;
    }

    protected boolean wasEnterpriseConfigChange(WifiEnterpriseConfig originalEnterpriseConfig, WifiEnterpriseConfig currentEnterpriseConfig) {
        if (originalEnterpriseConfig == null || currentEnterpriseConfig == null) {
            return (originalEnterpriseConfig == null && currentEnterpriseConfig == null) ? false : true;
        }
        if (originalEnterpriseConfig.getEapMethod() != currentEnterpriseConfig.getEapMethod() || originalEnterpriseConfig.getPhase2Method() != currentEnterpriseConfig.getPhase2Method()) {
            return true;
        }
        X509Certificate[] originalCaCerts = originalEnterpriseConfig.getCaCertificates();
        X509Certificate[] currentCaCerts = currentEnterpriseConfig.getCaCertificates();
        if (originalCaCerts == null || currentCaCerts == null) {
            return (originalCaCerts == null && currentCaCerts == null) ? false : true;
        }
        if (originalCaCerts.length != currentCaCerts.length) {
            return true;
        }
        for (int i = 0; i < originalCaCerts.length; i++) {
            if (!originalCaCerts[i].equals(currentCaCerts[i])) {
                return true;
            }
        }
        return false;
    }

    public WifiConfiguration getWifiConfigForHomeSP(HomeSP homeSP) {
        WifiConfiguration config = this.mConfiguredNetworks.getByFQDNForCurrentUser(homeSP.getFQDN());
        if (config == null) {
            Log.e(TAG, "Could not find network for homeSP " + homeSP.getFQDN());
        }
        return config;
    }

    public HomeSP getHomeSPForConfig(WifiConfiguration config) {
        WifiConfiguration storedConfig = this.mConfiguredNetworks.getForCurrentUser(config.networkId);
        if (storedConfig == null || !storedConfig.isPasspoint()) {
            return null;
        }
        return this.mMOManager.getHomeSP(storedConfig.FQDN);
    }

    public ScanDetailCache getScanDetailCache(WifiConfiguration config) {
        if (config == null) {
            return null;
        }
        ScanDetailCache cache = this.mScanDetailCaches.get(Integer.valueOf(config.networkId));
        if (cache == null && config.networkId != -1) {
            ScanDetailCache cache2 = new ScanDetailCache(config);
            this.mScanDetailCaches.put(Integer.valueOf(config.networkId), cache2);
            return cache2;
        }
        return cache;
    }

    public void linkConfiguration(WifiConfiguration config) throws Throwable {
        ScanDetailCache linkedScanDetailCache;
        if (!WifiConfigurationUtil.isVisibleToAnyProfile(config, this.mUserManager.getProfiles(this.mCurrentUserId))) {
            logd("linkConfiguration: Attempting to link config " + config.configKey() + " that is not visible to the current user.");
            return;
        }
        if ((getScanDetailCache(config) == null || getScanDetailCache(config).size() <= 6) && config.allowedKeyManagement.get(1)) {
            for (WifiConfiguration link : this.mConfiguredNetworks.valuesForCurrentUser()) {
                boolean doLink = false;
                if (!link.configKey().equals(config.configKey()) && !link.ephemeral && link.allowedKeyManagement.equals(config.allowedKeyManagement) && ((linkedScanDetailCache = getScanDetailCache(link)) == null || linkedScanDetailCache.size() <= 6)) {
                    if (config.defaultGwMacAddress == null || link.defaultGwMacAddress == null) {
                        if (getScanDetailCache(config) != null && getScanDetailCache(config).size() <= 6) {
                            for (String abssid : getScanDetailCache(config).keySet()) {
                                for (String bbssid : linkedScanDetailCache.keySet()) {
                                    if (sVVDBG) {
                                        logd("linkConfiguration try to link due to DBDC BSSID match " + link.SSID + " and " + config.SSID + " bssida " + abssid + " bssidb " + bbssid);
                                    }
                                    if (abssid.regionMatches(true, 0, bbssid, 0, 16)) {
                                        doLink = true;
                                    }
                                }
                            }
                        }
                    } else if (config.defaultGwMacAddress.equals(link.defaultGwMacAddress)) {
                        if (sVDBG) {
                            logd("linkConfiguration link due to same gw " + link.SSID + " and " + config.SSID + " GW " + config.defaultGwMacAddress);
                        }
                        doLink = true;
                    }
                    if (doLink && this.mOnlyLinkSameCredentialConfigurations) {
                        String apsk = readNetworkVariableFromSupplicantFile(link.configKey(), "psk");
                        String bpsk = readNetworkVariableFromSupplicantFile(config.configKey(), "psk");
                        if (apsk == null || bpsk == null || TextUtils.isEmpty(apsk) || TextUtils.isEmpty(apsk) || apsk.equals("*") || apsk.equals(DELETED_CONFIG_PSK) || !apsk.equals(bpsk)) {
                            doLink = false;
                        }
                    }
                    if (doLink) {
                        if (sVDBG) {
                            logd("linkConfiguration: will link " + link.configKey() + " and " + config.configKey());
                        }
                        if (link.linkedConfigurations == null) {
                            link.linkedConfigurations = new HashMap();
                        }
                        if (config.linkedConfigurations == null) {
                            config.linkedConfigurations = new HashMap();
                        }
                        if (link.linkedConfigurations.get(config.configKey()) == null) {
                            link.linkedConfigurations.put(config.configKey(), 1);
                        }
                        if (config.linkedConfigurations.get(link.configKey()) == null) {
                            config.linkedConfigurations.put(link.configKey(), 1);
                        }
                    } else {
                        if (link.linkedConfigurations != null && link.linkedConfigurations.get(config.configKey()) != null) {
                            if (sVDBG) {
                                logd("linkConfiguration: un-link " + config.configKey() + " from " + link.configKey());
                            }
                            link.linkedConfigurations.remove(config.configKey());
                        }
                        if (config.linkedConfigurations != null && config.linkedConfigurations.get(link.configKey()) != null) {
                            if (sVDBG) {
                                logd("linkConfiguration: un-link " + link.configKey() + " from " + config.configKey());
                            }
                            config.linkedConfigurations.remove(link.configKey());
                        }
                    }
                }
            }
        }
    }

    public HashSet<Integer> makeChannelList(WifiConfiguration config, int age) {
        if (config == null) {
            return null;
        }
        long now_ms = this.mClock.currentTimeMillis();
        HashSet<Integer> channels = new HashSet<>();
        if (getScanDetailCache(config) == null && config.linkedConfigurations == null) {
            return null;
        }
        if (sVDBG) {
            StringBuilder dbg = new StringBuilder();
            dbg.append("makeChannelList age=").append(Integer.toString(age)).append(" for ").append(config.configKey()).append(" max=").append(this.mMaxNumActiveChannelsForPartialScans);
            if (getScanDetailCache(config) != null) {
                dbg.append(" bssids=").append(getScanDetailCache(config).size());
            }
            if (config.linkedConfigurations != null) {
                dbg.append(" linked=").append(config.linkedConfigurations.size());
            }
            logd(dbg.toString());
        }
        int numChannels = 0;
        if (getScanDetailCache(config) != null && getScanDetailCache(config).size() > 0) {
            for (ScanDetail scanDetail : getScanDetailCache(config).values()) {
                ScanResult result = scanDetail.getScanResult();
                if (numChannels > this.mMaxNumActiveChannelsForPartialScans.get()) {
                    break;
                }
                if (sVDBG) {
                    boolean test = now_ms - result.seen < ((long) age);
                    logd("has " + result.BSSID + " freq=" + Integer.toString(result.frequency) + " age=" + Long.toString(now_ms - result.seen) + " ?=" + test);
                }
                if (now_ms - result.seen < age) {
                    channels.add(Integer.valueOf(result.frequency));
                    numChannels++;
                }
            }
        }
        if (config.linkedConfigurations != null) {
            for (String key : config.linkedConfigurations.keySet()) {
                WifiConfiguration linked = getWifiConfiguration(key);
                if (linked != null && getScanDetailCache(linked) != null) {
                    for (ScanDetail scanDetail2 : getScanDetailCache(linked).values()) {
                        ScanResult result2 = scanDetail2.getScanResult();
                        if (sVDBG) {
                            logd("has link: " + result2.BSSID + " freq=" + Integer.toString(result2.frequency) + " age=" + Long.toString(now_ms - result2.seen));
                        }
                        if (numChannels <= this.mMaxNumActiveChannelsForPartialScans.get()) {
                            if (now_ms - result2.seen < age) {
                                channels.add(Integer.valueOf(result2.frequency));
                                numChannels++;
                            }
                        }
                    }
                }
            }
        }
        return channels;
    }

    private Map<HomeSP, PasspointMatch> matchPasspointNetworks(ScanDetail scanDetail) {
        if (!this.mMOManager.isConfigured()) {
            if (this.mEnableOsuQueries) {
                NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                List<Constants.ANQPElementType> querySet = ANQPFactory.buildQueryList(networkDetail, false, true);
                if (networkDetail.queriable(querySet)) {
                    List<Constants.ANQPElementType> querySet2 = this.mAnqpCache.initiate(networkDetail, querySet);
                    if (querySet2 != null) {
                        this.mSupplicantBridge.startANQP(scanDetail, querySet2);
                    }
                    updateAnqpCache(scanDetail, networkDetail.getANQPElements());
                }
            }
            return null;
        }
        NetworkDetail networkDetail2 = scanDetail.getNetworkDetail();
        if (!networkDetail2.hasInterworking()) {
            return null;
        }
        updateAnqpCache(scanDetail, networkDetail2.getANQPElements());
        Map<HomeSP, PasspointMatch> matches = matchNetwork(scanDetail, true);
        Log.d(Utils.hs2LogTag(getClass()), scanDetail.getSSID() + " pass 1 matches: " + toMatchString(matches));
        return matches;
    }

    private Map<HomeSP, PasspointMatch> matchNetwork(ScanDetail scanDetail, boolean query) {
        List<Constants.ANQPElementType> querySet;
        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
        ANQPData anqpData = this.mAnqpCache.getEntry(networkDetail);
        Map<Constants.ANQPElementType, ANQPElement> aNQPElements = anqpData != null ? anqpData.getANQPElements() : null;
        boolean queried = !query;
        Collection<HomeSP> homeSPs = this.mMOManager.getLoadedSPs().values();
        Map<HomeSP, PasspointMatch> matches = new HashMap<>(homeSPs.size());
        Log.d(Utils.hs2LogTag(getClass()), "match nwk " + scanDetail.toKeyString() + ", anqp " + (anqpData != null ? "present" : "missing") + ", query " + query + ", home sps: " + homeSPs.size());
        for (HomeSP homeSP : homeSPs) {
            PasspointMatch match = homeSP.match(networkDetail, aNQPElements, this.mSIMAccessor);
            Log.d(Utils.hs2LogTag(getClass()), " -- " + homeSP.getFQDN() + ": match " + match + ", queried " + queried);
            if ((match == PasspointMatch.Incomplete || this.mEnableOsuQueries) && !queried) {
                boolean matchSet = match == PasspointMatch.Incomplete;
                boolean osu = this.mEnableOsuQueries;
                List<Constants.ANQPElementType> querySet2 = ANQPFactory.buildQueryList(networkDetail, matchSet, osu);
                if (networkDetail.queriable(querySet2) && (querySet = this.mAnqpCache.initiate(networkDetail, querySet2)) != null) {
                    this.mSupplicantBridge.startANQP(scanDetail, querySet);
                }
                queried = true;
            }
            matches.put(homeSP, match);
        }
        return matches;
    }

    public Map<Constants.ANQPElementType, ANQPElement> getANQPData(NetworkDetail network) {
        ANQPData data = this.mAnqpCache.getEntry(network);
        if (data != null) {
            return data.getANQPElements();
        }
        return null;
    }

    public SIMAccessor getSIMAccessor() {
        return this.mSIMAccessor;
    }

    public void notifyANQPDone(Long bssid, boolean success) {
        this.mSupplicantBridge.notifyANQPDone(bssid, success);
    }

    public void notifyIconReceived(IconEvent iconEvent) {
        Intent intent = new Intent("android.net.wifi.PASSPOINT_ICON_RECEIVED");
        intent.addFlags(67108864);
        intent.putExtra("bssid", iconEvent.getBSSID());
        intent.putExtra("file", iconEvent.getFileName());
        try {
            intent.putExtra("icon", this.mSupplicantBridge.retrieveIcon(iconEvent));
        } catch (IOException e) {
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void updateAnqpCache(ScanDetail scanDetail, Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
        if (anqpElements == null) {
            ANQPData data = this.mAnqpCache.getEntry(networkDetail);
            if (data != null) {
                scanDetail.propagateANQPInfo(data.getANQPElements());
                return;
            }
            return;
        }
        this.mAnqpCache.update(networkDetail, anqpElements);
    }

    private static String toMatchString(Map<HomeSP, PasspointMatch> matches) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<HomeSP, PasspointMatch> entry : matches.entrySet()) {
            sb.append(' ').append(entry.getKey().getFQDN()).append("->").append(entry.getValue());
        }
        return sb.toString();
    }

    private void cacheScanResultForPasspointConfigs(ScanDetail scanDetail, Map<HomeSP, PasspointMatch> matches, List<WifiConfiguration> associatedWifiConfigurations) throws Throwable {
        for (Map.Entry<HomeSP, PasspointMatch> entry : matches.entrySet()) {
            PasspointMatch match = entry.getValue();
            if (match == PasspointMatch.HomeProvider || match == PasspointMatch.RoamingProvider) {
                WifiConfiguration config = getWifiConfigForHomeSP(entry.getKey());
                if (config != null) {
                    cacheScanResultForConfig(config, scanDetail, entry.getValue());
                    if (associatedWifiConfigurations != null) {
                        associatedWifiConfigurations.add(config);
                    }
                } else {
                    Log.w(Utils.hs2LogTag(getClass()), "Failed to find config for '" + entry.getKey().getFQDN() + "'");
                }
            }
        }
    }

    private void cacheScanResultForConfig(WifiConfiguration config, ScanDetail scanDetail, PasspointMatch passpointMatch) throws Throwable {
        ScanResult scanResult = scanDetail.getScanResult();
        ScanDetailCache scanDetailCache = getScanDetailCache(config);
        if (scanDetailCache == null) {
            Log.w(TAG, "Could not allocate scan cache for " + config.SSID);
            return;
        }
        ScanResult result = scanDetailCache.get(scanResult.BSSID);
        if (result != null) {
            scanResult.blackListTimestamp = result.blackListTimestamp;
            scanResult.numIpConfigFailures = result.numIpConfigFailures;
            scanResult.numConnection = result.numConnection;
            scanResult.isAutoJoinCandidate = result.isAutoJoinCandidate;
        }
        if (config.ephemeral) {
            scanResult.untrusted = true;
        }
        if (scanDetailCache.size() > 192) {
            long now_dbg = 0;
            if (sVVDBG) {
                logd(" Will trim config " + config.configKey() + " size " + scanDetailCache.size());
                for (ScanDetail sd : scanDetailCache.values()) {
                    logd("     " + sd.getBSSIDString() + " " + sd.getSeen());
                }
                now_dbg = SystemClock.elapsedRealtimeNanos();
            }
            scanDetailCache.trim(128);
            if (sVVDBG) {
                long diff = SystemClock.elapsedRealtimeNanos() - now_dbg;
                logd(" Finished trimming config, time(ns) " + diff);
                for (ScanDetail sd2 : scanDetailCache.values()) {
                    logd("     " + sd2.getBSSIDString() + " " + sd2.getSeen());
                }
            }
        }
        if (passpointMatch != null) {
            scanDetailCache.put(scanDetail, passpointMatch, getHomeSPForConfig(config));
        } else {
            scanDetailCache.put(scanDetail);
        }
        linkConfiguration(config);
    }

    private boolean isEncryptionWep(String encryption) {
        return encryption.contains("WEP");
    }

    private boolean isEncryptionPsk(String encryption) {
        return encryption.contains("PSK");
    }

    private boolean isEncryptionEap(String encryption) {
        return encryption.contains("EAP");
    }

    private boolean isEncryptionWapi(String encryption) {
        return encryption.contains("WAPI");
    }

    public boolean isOpenNetwork(String encryption) {
        if (!isEncryptionWep(encryption) && !isEncryptionPsk(encryption) && !isEncryptionEap(encryption) && !isEncryptionWapi(encryption)) {
            return true;
        }
        return false;
    }

    public boolean isOpenNetwork(ScanResult scan) {
        String scanResultEncrypt = scan.capabilities;
        return isOpenNetwork(scanResultEncrypt);
    }

    public boolean isOpenNetwork(WifiConfiguration config) {
        String configEncrypt = config.configKey();
        return isOpenNetwork(configEncrypt);
    }

    public List<WifiConfiguration> getSavedNetworkFromScanDetail(ScanDetail scanDetail) {
        ScanResult scanResult = scanDetail.getScanResult();
        if (scanResult == null) {
            return null;
        }
        List<WifiConfiguration> savedWifiConfigurations = new ArrayList<>();
        String ssid = "\"" + scanResult.SSID + "\"";
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            if (config.SSID != null && config.SSID.equals(ssid)) {
                localLog("getSavedNetworkFromScanDetail(): try " + config.configKey() + " SSID=" + config.SSID + " " + scanResult.SSID + " " + scanResult.capabilities);
                String scanResultEncrypt = scanResult.capabilities;
                String configEncrypt = config.configKey();
                if ((isEncryptionWep(scanResultEncrypt) && isEncryptionWep(configEncrypt)) || ((isEncryptionPsk(scanResultEncrypt) && isEncryptionPsk(configEncrypt)) || ((isEncryptionEap(scanResultEncrypt) && isEncryptionEap(configEncrypt)) || ((isOpenNetwork(scanResultEncrypt) && isOpenNetwork(configEncrypt)) || this.mWifiFwkExt.getSecurity(scanResult) == this.mWifiFwkExt.getSecurity(config))))) {
                    savedWifiConfigurations.add(config);
                }
            }
        }
        return savedWifiConfigurations;
    }

    public List<WifiConfiguration> updateSavedNetworkWithNewScanDetail(ScanDetail scanDetail, boolean isConnectingOrConnected) throws Throwable {
        Map<HomeSP, PasspointMatch> matches;
        ScanResult scanResult = scanDetail.getScanResult();
        if (scanResult == null) {
            return null;
        }
        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
        List<WifiConfiguration> associatedWifiConfigurations = new ArrayList<>();
        if (networkDetail.hasInterworking() && !isConnectingOrConnected && (matches = matchPasspointNetworks(scanDetail)) != null) {
            cacheScanResultForPasspointConfigs(scanDetail, matches, associatedWifiConfigurations);
        }
        List<WifiConfiguration> savedConfigurations = getSavedNetworkFromScanDetail(scanDetail);
        if (savedConfigurations != null) {
            for (WifiConfiguration config : savedConfigurations) {
                cacheScanResultForConfig(config, scanDetail, null);
                associatedWifiConfigurations.add(config);
            }
        }
        if (associatedWifiConfigurations.size() == 0) {
            return null;
        }
        return associatedWifiConfigurations;
    }

    public void handleUserSwitch(int userId) {
        this.mCurrentUserId = userId;
        Set<WifiConfiguration> ephemeralConfigs = new HashSet<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.valuesForCurrentUser()) {
            if (config.ephemeral) {
                ephemeralConfigs.add(config);
            }
        }
        if (!ephemeralConfigs.isEmpty()) {
            Iterator config$iterator = ephemeralConfigs.iterator();
            while (config$iterator.hasNext()) {
                removeConfigWithoutBroadcast((WifiConfiguration) config$iterator.next());
            }
            saveConfig();
            writeKnownNetworkHistory();
        }
        List<WifiConfiguration> hiddenConfigurations = this.mConfiguredNetworks.handleUserSwitch(this.mCurrentUserId);
        for (WifiConfiguration network : hiddenConfigurations) {
            disableNetworkNative(network);
        }
        enableAllNetworks();
        sendConfiguredNetworksChangedBroadcast();
    }

    public int getCurrentUserId() {
        return this.mCurrentUserId;
    }

    public boolean isCurrentUserProfile(int userId) {
        if (userId == this.mCurrentUserId) {
            return true;
        }
        UserInfo parent = this.mUserManager.getProfileParent(userId);
        return parent != null && parent.id == this.mCurrentUserId;
    }

    private NetworkUpdateResult writeIpAndProxyConfigurationsOnChange(WifiConfiguration currentConfig, WifiConfiguration newConfig, boolean isNewNetwork) {
        boolean ipChanged = false;
        boolean proxyChanged = false;
        switch (m39getandroidnetIpConfiguration$IpAssignmentSwitchesValues()[newConfig.getIpAssignment().ordinal()]) {
            case 1:
                if (currentConfig.getIpAssignment() != newConfig.getIpAssignment()) {
                    ipChanged = true;
                }
                break;
            case 2:
                ipChanged = currentConfig.getIpAssignment() != newConfig.getIpAssignment() || !Objects.equals(currentConfig.getStaticIpConfiguration(), newConfig.getStaticIpConfiguration());
                break;
            case 3:
                break;
            default:
                loge("Ignore invalid ip assignment during write");
                break;
        }
        switch (m40getandroidnetIpConfiguration$ProxySettingsSwitchesValues()[newConfig.getProxySettings().ordinal()]) {
            case 1:
                if (currentConfig.getProxySettings() != newConfig.getProxySettings()) {
                    proxyChanged = true;
                }
                break;
            case 2:
            case 3:
                ProxyInfo newHttpProxy = newConfig.getHttpProxy();
                ProxyInfo currentHttpProxy = currentConfig.getHttpProxy();
                if (newHttpProxy == null) {
                    proxyChanged = currentHttpProxy != null;
                } else if (!newHttpProxy.equals(currentHttpProxy)) {
                    proxyChanged = true;
                } else {
                    proxyChanged = false;
                }
                break;
            case 4:
                break;
            default:
                loge("Ignore invalid proxy configuration during write");
                break;
        }
        if (ipChanged) {
            currentConfig.setIpAssignment(newConfig.getIpAssignment());
            currentConfig.setStaticIpConfiguration(newConfig.getStaticIpConfiguration());
            log("IP config changed SSID = " + currentConfig.SSID);
            if (currentConfig.getStaticIpConfiguration() != null) {
                log(" static configuration: " + currentConfig.getStaticIpConfiguration().toString());
            }
        }
        if (proxyChanged) {
            currentConfig.setProxySettings(newConfig.getProxySettings());
            currentConfig.setHttpProxy(newConfig.getHttpProxy());
            log("proxy changed SSID = " + currentConfig.SSID);
            if (currentConfig.getHttpProxy() != null) {
                log(" proxyProperties: " + currentConfig.getHttpProxy().toString());
            }
        }
        if (ipChanged || proxyChanged || isNewNetwork) {
            if (sVDBG) {
                logd("writeIpAndProxyConfigurationsOnChange: " + currentConfig.SSID + " -> " + newConfig.SSID + " path: " + IP_CONFIG_FILE);
            }
            writeIpAndProxyConfigurations();
        }
        return new NetworkUpdateResult(ipChanged, proxyChanged);
    }

    private void readNetworkVariables(WifiConfiguration config) {
        this.mWifiConfigStore.readNetworkVariables(config);
    }

    public WifiConfiguration wifiConfigurationFromScanResult(ScanResult result) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + result.SSID + "\"";
        if (sVDBG) {
            logd("WifiConfiguration from scan results " + config.SSID + " cap " + result.capabilities);
        }
        if (result.capabilities.contains("PSK") || result.capabilities.contains("EAP") || result.capabilities.contains("WEP")) {
            if (result.capabilities.contains("PSK")) {
                config.allowedKeyManagement.set(1);
            }
            if (result.capabilities.contains("EAP")) {
                config.allowedKeyManagement.set(2);
                config.allowedKeyManagement.set(3);
            }
            if (result.capabilities.contains("WEP")) {
                config.allowedKeyManagement.set(0);
                config.allowedAuthAlgorithms.set(0);
                config.allowedAuthAlgorithms.set(1);
            }
        } else {
            config.allowedKeyManagement.set(0);
        }
        return config;
    }

    public WifiConfiguration wifiConfigurationFromScanResult(ScanDetail scanDetail) {
        ScanResult result = scanDetail.getScanResult();
        return wifiConfigurationFromScanResult(result);
    }

    private static int configKey(WifiConfiguration config) {
        String key = config.configKey();
        return key.hashCode();
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiConfigManager");
        pw.println("mLastPriority " + this.mLastPriority);
        pw.println("Configured networks");
        for (WifiConfiguration conf : getAllConfiguredNetworks()) {
            pw.println(conf);
        }
        pw.println();
        if (this.mLostConfigsDbg != null && this.mLostConfigsDbg.size() > 0) {
            pw.println("LostConfigs: ");
            for (String s : this.mLostConfigsDbg) {
                pw.println(s);
            }
        }
        if (this.mLocalLog != null) {
            pw.println("WifiConfigManager - Log Begin ----");
            this.mLocalLog.dump(fd, pw, args);
            pw.println("WifiConfigManager - Log End ----");
        }
        if (!this.mMOManager.isConfigured()) {
            return;
        }
        pw.println("Begin dump of ANQP Cache");
        this.mAnqpCache.dump(pw);
        pw.println("End dump of ANQP Cache");
    }

    public String getConfigFile() {
        return IP_CONFIG_FILE;
    }

    protected void logd(String s) {
        Log.d(TAG, s);
    }

    protected void loge(String s) {
        loge(s, false);
    }

    protected void loge(String s, boolean stack) {
        if (stack) {
            Log.e(TAG, s + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        } else {
            Log.e(TAG, s);
        }
    }

    private void logKernelTime() {
        long kernelTimeMs = System.nanoTime() / 1000000;
        StringBuilder builder = new StringBuilder();
        builder.append("kernel time = ").append(kernelTimeMs / 1000).append(".").append(kernelTimeMs % 1000).append("\n");
        localLog(builder.toString());
    }

    protected void log(String s) {
        Log.d(TAG, s);
    }

    private void localLog(String s) {
        if (this.mLocalLog != null) {
            this.mLocalLog.log(s);
        }
        Log.d(TAG, s);
    }

    private void localLogAndLogcat(String s) {
        localLog(s);
        Log.d(TAG, s);
    }

    private void localLogNetwork(String s, int netId) {
        WifiConfiguration config;
        if (this.mLocalLog == null) {
            return;
        }
        synchronized (this.mConfiguredNetworks) {
            config = this.mConfiguredNetworks.getForAllUsers(netId);
        }
        if (config != null) {
            localLogAndLogcat(s + " " + config.getPrintableSsid() + " " + netId + " status=" + config.status + " key=" + config.configKey());
        } else {
            localLogAndLogcat(s + " " + netId);
        }
    }

    static boolean needsSoftwareBackedKeyStore(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        if (!TextUtils.isEmpty(client)) {
            return true;
        }
        return false;
    }

    public boolean isSimConfig(WifiConfiguration config) {
        return this.mWifiConfigStore.isSimConfig(config);
    }

    public void resetSimNetworks(int simSlot) {
        this.mWifiConfigStore.resetSimNetworks(this.mConfiguredNetworks.valuesForCurrentUser(), simSlot);
    }

    boolean isNetworkConfigured(WifiConfiguration config) {
        return config.networkId != -1 ? this.mConfiguredNetworks.getForCurrentUser(config.networkId) != null : this.mConfiguredNetworks.getByConfigKeyForCurrentUser(config.configKey()) != null;
    }

    boolean canModifyNetwork(int uid, int networkId, boolean onlyAnnotate) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(networkId);
        if (config == null) {
            loge("canModifyNetwork: cannot find config networkId " + networkId);
            return false;
        }
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        boolean isUidDeviceOwner = dpmi != null ? dpmi.isActiveAdminWithPolicy(uid, -2) : false;
        if (isUidDeviceOwner) {
            return true;
        }
        boolean isCreator = config.creatorUid == uid;
        if (onlyAnnotate) {
            if (isCreator) {
                return true;
            }
            return checkConfigOverridePermission(uid);
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin") && dpmi == null) {
            return false;
        }
        boolean isConfigEligibleForLockdown = dpmi != null ? dpmi.isActiveAdminWithPolicy(config.creatorUid, -2) : false;
        if (!isConfigEligibleForLockdown) {
            if (isCreator) {
                return true;
            }
            return checkConfigOverridePermission(uid);
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver, "wifi_device_owner_configs_lockdown", 0) != 0;
        if (isLockdownFeatureEnabled) {
            return false;
        }
        return checkConfigOverridePermission(uid);
    }

    boolean canModifyNetwork(int uid, WifiConfiguration config, boolean onlyAnnotate) {
        int netid;
        if (config == null) {
            loge("canModifyNetowrk recieved null configuration");
            return false;
        }
        if (config.networkId != -1) {
            netid = config.networkId;
        } else {
            WifiConfiguration test = this.mConfiguredNetworks.getByConfigKeyForCurrentUser(config.configKey());
            if (test == null) {
                return false;
            }
            netid = test.networkId;
        }
        return canModifyNetwork(uid, netid, onlyAnnotate);
    }

    boolean checkConfigOverridePermission(int uid) {
        try {
            return this.mFacade.checkUidPermission("android.permission.OVERRIDE_WIFI_CONFIG", uid) == 0;
        } catch (RemoteException e) {
            return false;
        }
    }

    void handleBadNetworkDisconnectReport(int netId, WifiInfo info) {
        WifiConfiguration config = this.mConfiguredNetworks.getForCurrentUser(netId);
        if (config != null && ((!info.is24GHz() || info.getRssi() > -73) && (!info.is5GHz() || info.getRssi() > -70))) {
            updateNetworkSelectionStatus(config, 1);
        }
        this.mLastUnwantedNetworkDisconnectTimestamp = this.mClock.currentTimeMillis();
    }

    int getMaxDhcpRetries() {
        return this.mFacade.getIntegerSetting(this.mContext, "wifi_max_dhcp_retry_count", 9);
    }

    void clearBssidBlacklist() {
        this.mWifiConfigStore.clearBssidBlacklist();
    }

    void blackListBssid(String bssid) {
        this.mWifiConfigStore.blackListBssid(bssid);
    }

    public boolean isBssidBlacklisted(String bssid) {
        return this.mWifiConfigStore.isBssidBlacklisted(bssid);
    }

    public boolean getEnableAutoJoinWhenAssociated() {
        return this.mEnableAutoJoinWhenAssociated.get();
    }

    public void setEnableAutoJoinWhenAssociated(boolean enabled) {
        this.mEnableAutoJoinWhenAssociated.set(enabled);
    }

    public void setActiveScanDetail(ScanDetail activeScanDetail) {
        synchronized (this.mActiveScanDetailLock) {
            this.mActiveScanDetail = activeScanDetail;
        }
    }

    public boolean wasEphemeralNetworkDeleted(String ssid) {
        return this.mDeletedEphemeralSSIDs.contains(ssid);
    }

    public void resetSimNetwork(WifiConfiguration config) {
        ArrayList<WifiConfiguration> configs = new ArrayList<>();
        configs.add(config);
        this.mWifiConfigStore.resetSimNetworks(configs, WifiConfigurationUtil.getIntSimSlot(config));
    }

    void addDisconnectNetwork(int netId) {
        synchronized (this.mDisconnectNetworks) {
            this.mDisconnectNetworks.add(Integer.valueOf(netId));
        }
    }

    void removeDisconnectNetwork(int netId) {
        synchronized (this.mDisconnectNetworks) {
            this.mDisconnectNetworks.remove(Integer.valueOf(netId));
        }
    }

    List<Integer> getDisconnectNetworks() {
        List<Integer> networks = new ArrayList<>();
        synchronized (this.mDisconnectNetworks) {
            for (Integer netId : this.mDisconnectNetworks) {
                networks.add(netId);
            }
        }
        return networks;
    }

    void setWifiFwkExt(IWifiFwkExt wifiExt) {
        this.mWifiFwkExt = wifiExt;
    }
}
