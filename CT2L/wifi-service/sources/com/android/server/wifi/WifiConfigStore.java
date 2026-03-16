package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.net.IpConfiguration;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiSsid;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.os.Environment;
import android.os.FileObserver;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.net.DelayedDiskWrite;
import com.android.server.net.IpConfigStore;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class WifiConfigStore extends IpConfigStore {
    private static final String ALWAYS_ENABLE_SCAN_WHILE_ASSOCIATED_KEY = "ALWAYS_ENABLE_SCAN_WHILE_ASSOCIATED:   ";
    private static final String ASSOCIATED_FULL_SCAN_BACKOFF_KEY = "ASSOCIATED_FULL_SCAN_BACKOFF_PERIOD:   ";
    private static final String ASSOCIATED_PARTIAL_SCAN_PERIOD_KEY = "ASSOCIATED_PARTIAL_SCAN_PERIOD:   ";
    private static final String AUTH_KEY = "AUTH:  ";
    private static final String A_BAND_PREFERENCE_RSSI_THRESHOLD_KEY = "A_BAND_PREFERENCE_RSSI_THRESHOLD:   ";
    private static final String A_BAND_PREFERENCE_RSSI_THRESHOLD_LOW_KEY = "A_BAND_PREFERENCE_RSSI_THRESHOLD_LOW:   ";
    private static final String BLACKLIST_MILLI_KEY = "BLACKLIST_MILLI:  ";
    private static final String BSSID_KEY = "BSSID:  ";
    private static final String BSSID_KEY_END = "/BSSID:  ";
    private static final String BSSID_STATUS_KEY = "BSSID_STATUS:  ";
    private static final String CHOICE_KEY = "CHOICE:  ";
    private static final String CONFIG_KEY = "CONFIG:  ";
    private static final String CONNECT_UID_KEY = "CONNECT_UID_KEY:  ";
    private static final String CREATOR_UID_KEY = "CREATOR_UID_KEY:  ";
    private static final String DATE_KEY = "DATE:  ";
    private static final boolean DBG = true;
    private static final String DEFAULT_GW_KEY = "DEFAULT_GW:  ";
    private static final int DEFAULT_MAX_DHCP_RETRIES = 9;
    private static final String DELETED_CONFIG_PSK = "Mjkd86jEMGn79KhKll298Uu7-deleted";
    private static final String DELETED_CRC32_KEY = "DELETED_CRC32:  ";
    private static final String DELETED_EPHEMERAL_KEY = "DELETED_EPHEMERAL:  ";
    private static final String DID_SELF_ADD_KEY = "DID_SELF_ADD:  ";
    static final String EMPTY_VALUE = "NULL";
    private static final String ENABLE_AUTOJOIN_WHILE_ASSOCIATED_KEY = "ENABLE_AUTOJOIN_WHILE_ASSOCIATED:   ";
    private static final String ENABLE_AUTO_JOIN_SCAN_WHILE_ASSOCIATED_KEY = "ENABLE_AUTO_JOIN_SCAN_WHILE_ASSOCIATED:   ";
    private static final String ENABLE_AUTO_JOIN_WHILE_ASSOCIATED_KEY = "ENABLE_AUTO_JOIN_WHILE_ASSOCIATED:   ";
    private static final String ENABLE_CHIP_WAKE_UP_WHILE_ASSOCIATED_KEY = "ENABLE_CHIP_WAKE_UP_WHILE_ASSOCIATED:   ";
    private static final String ENABLE_FULL_BAND_SCAN_WHEN_ASSOCIATED_KEY = "ENABLE_FULL_BAND_SCAN_WHEN_ASSOCIATED:   ";
    private static final String ENABLE_RSSI_POLL_WHILE_ASSOCIATED_KEY = "ENABLE_RSSI_POLL_WHILE_ASSOCIATED_KEY:   ";
    private static final String EPHEMERAL_KEY = "EPHEMERAL:   ";
    private static final String FAILURE_KEY = "FAILURE:  ";
    private static final String FQDN_KEY = "FQDN:  ";
    private static final String FREQ_KEY = "FREQ:  ";
    private static final String G_BAND_PREFERENCE_RSSI_THRESHOLD_KEY = "G_BAND_PREFERENCE_RSSI_THRESHOLD:   ";
    private static final String JOIN_ATTEMPT_BOOST_KEY = "JOIN_ATTEMPT_BOOST:  ";
    private static final String LINK_KEY = "LINK:  ";
    private static final String MAX_NUM_ACTIVE_CHANNELS_FOR_PARTIAL_SCANS_KEY = "MAX_NUM_ACTIVE_CHANNELS_FOR_PARTIAL_SCANS:   ";
    private static final String MAX_NUM_PASSIVE_CHANNELS_FOR_PARTIAL_SCANS_KEY = "MAX_NUM_PASSIVE_CHANNELS_FOR_PARTIAL_SCANS:   ";
    private static final String MILLI_KEY = "MILLI:  ";
    private static final String NETWORK_ID_KEY = "ID:  ";
    private static final String NO_INTERNET_ACCESS_REPORTS_KEY = "NO_INTERNET_ACCESS_REPORTS :   ";
    private static final String NUM_ASSOCIATION_KEY = "NUM_ASSOCIATION:  ";
    private static final String NUM_AUTH_FAILURES_KEY = "AUTH_FAILURES:  ";
    private static final String NUM_CONNECTION_FAILURES_KEY = "CONNECT_FAILURES:  ";
    private static final String NUM_IP_CONFIG_FAILURES_KEY = "IP_CONFIG_FAILURES:  ";
    public static final String OLD_PRIVATE_KEY_NAME = "private_key";
    private static final String ONLY_LINK_SAME_CREDENTIAL_CONFIGURATIONS_KEY = "ONLY_LINK_SAME_CREDENTIAL_CONFIGURATIONS:   ";
    private static final String PEER_CONFIGURATION_KEY = "PEER_CONFIGURATION:  ";
    private static final String PRIORITY_KEY = "PRIORITY:  ";
    private static final String RSSI_KEY = "RSSI:  ";
    private static final String SCORER_OVERRIDE_AND_SWITCH_KEY = "SCORER_OVERRIDE_AND_SWITCH:  ";
    private static final String SCORER_OVERRIDE_KEY = "SCORER_OVERRIDE:  ";
    private static final String SELF_ADDED_KEY = "SELF_ADDED:  ";
    private static final String SEPARATOR_KEY = "\n";
    private static final String SSID_KEY = "SSID:  ";
    private static final String STATUS_KEY = "AUTO_JOIN_STATUS:  ";
    private static final String SUPPLICANT_CONFIG_FILE = "/data/misc/wifi/wpa_supplicant.conf";
    private static final String SUPPLICANT_DISABLE_REASON_KEY = "SUP_DIS_REASON:  ";
    private static final String SUPPLICANT_STATUS_KEY = "SUP_STATUS:  ";
    private static final String TAG = "WifiConfigStore";
    private static final String THRESHOLD_BAD_RSSI_24_KEY = "THRESHOLD_BAD_RSSI_24:  ";
    private static final String THRESHOLD_BAD_RSSI_5_KEY = "THRESHOLD_BAD_RSSI_5:  ";
    private static final String THRESHOLD_GOOD_RSSI_24_KEY = "THRESHOLD_GOOD_RSSI_24:  ";
    private static final String THRESHOLD_GOOD_RSSI_5_KEY = "THRESHOLD_GOOD_RSSI_5:  ";
    private static final String THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_24G_KEY = "THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_24G:  ";
    private static final String THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_5G_KEY = "THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_5G:  ";
    private static final String THRESHOLD_LOW_RSSI_24_KEY = "THRESHOLD_LOW_RSSI_24:  ";
    private static final String THRESHOLD_LOW_RSSI_5_KEY = "THRESHOLD_LOW_RSSI_5:  ";
    private static final String THRESHOLD_MAX_RX_PACKETS_FOR_FULL_SCANS_KEY = "THRESHOLD_MAX_RX_PACKETS_FOR_FULL_SCANS:   ";
    private static final String THRESHOLD_MAX_RX_PACKETS_FOR_NETWORK_SWITCHING_KEY = "THRESHOLD_MAX_RX_PACKETS_FOR_NETWORK_SWITCHING:   ";
    private static final String THRESHOLD_MAX_RX_PACKETS_FOR_PARTIAL_SCANS_KEY = "THRESHOLD_MAX_RX_PACKETS_FOR_PARTIAL_SCANS:   ";
    private static final String THRESHOLD_MAX_TX_PACKETS_FOR_FULL_SCANS_KEY = "THRESHOLD_MAX_TX_PACKETS_FOR_FULL_SCANS:   ";
    private static final String THRESHOLD_MAX_TX_PACKETS_FOR_NETWORK_SWITCHING_KEY = "THRESHOLD_MAX_TX_PACKETS_FOR_NETWORK_SWITCHING:   ";
    private static final String THRESHOLD_MAX_TX_PACKETS_FOR_PARTIAL_SCANS_KEY = "THRESHOLD_MAX_TX_PACKETS_FOR_PARTIAL_SCANS:   ";
    private static final String THRESHOLD_UNBLACKLIST_HARD_24G_KEY = "THRESHOLD_UNBLACKLIST_HARD_24G:  ";
    private static final String THRESHOLD_UNBLACKLIST_HARD_5G_KEY = "THRESHOLD_UNBLACKLIST_HARD_5G:  ";
    private static final String THRESHOLD_UNBLACKLIST_SOFT_24G_KEY = "THRESHOLD_UNBLACKLIST_SOFT_24G:  ";
    private static final String THRESHOLD_UNBLACKLIST_SOFT_5G_KEY = "THRESHOLD_UNBLACKLIST_SOFT_5G:  ";
    private static final String UPDATE_UID_KEY = "UPDATE_UID:  ";
    private static final String VALIDATED_INTERNET_ACCESS_KEY = "VALIDATED_INTERNET_ACCESS:  ";
    private static final String WIFI_VERBOSE_LOGS_KEY = "WIFI_VERBOSE_LOGS:   ";
    public static final int maxNumScanCacheEntries = 128;
    public int associatedFullScanBackoff;
    public int associatedFullScanMaxIntervalMilli;
    public int associatedHysteresisHigh;
    public int associatedHysteresisLow;
    public int associatedPartialScanPeriodMilli;
    public int badLinkSpeed24;
    public int badLinkSpeed5;
    public int bandPreferenceBoostFactor5;
    public int bandPreferenceBoostThreshold5;
    public int bandPreferencePenaltyFactor5;
    public int bandPreferencePenaltyThreshold5;
    public int currentNetworkBoost;
    public boolean enable5GHzPreference;
    public boolean enableAutoJoinScanWhenAssociated;
    public boolean enableAutoJoinWhenAssociated;
    public boolean enableLinkDebouncing;
    public boolean enableWifiCellularHandoverUserTriggeredAdjustment;
    public int goodLinkSpeed24;
    public int goodLinkSpeed5;
    private Context mContext;
    private final WpaConfigFileObserver mFileObserver;
    private final LocalLog mLocalLog;
    private WifiNative mWifiNative;
    public int maxAuthErrorsToBlacklist;
    public int maxConnectionErrorsToBlacklist;
    public int maxNumActiveChannelsForPartialScans;
    public int maxNumPassiveChannelsForPartialScans;
    public int networkSwitchingBlackListPeriodMilli;
    public boolean onlyLinkSameCredentialConfigurations;
    public int scanResultRssiLevelPatchUp;
    public int thresholdBadRssi24;
    public int thresholdBadRssi5;
    public int thresholdGoodRssi24;
    public int thresholdGoodRssi5;
    public int thresholdLowRssi24;
    public int thresholdLowRssi5;
    public int wifiConfigBlacklistMinTimeMilli;
    private static boolean VDBG = false;
    private static boolean VVDBG = false;
    private static final String ipConfigFile = Environment.getDataDirectory() + "/misc/wifi/ipconfig.txt";
    private static final String networkHistoryConfigFile = Environment.getDataDirectory() + "/misc/wifi/networkHistory.txt";
    private static final String autoJoinConfigFile = Environment.getDataDirectory() + "/misc/wifi/autojoinconfig.txt";
    private static Pattern mConnectChoice = Pattern.compile("(.*)=([0-9]+)");
    private static final String[] ENTERPRISE_CONFIG_SUPPLICANT_KEYS = {"eap", "phase2", "identity", "anonymous_identity", "password", "client_cert", "ca_cert", "subject_match", "engine", "engine_id", "key_id"};
    private HashMap<Integer, WifiConfiguration> mConfiguredNetworks = new HashMap<>();
    private HashMap<Integer, Integer> mNetworkIds = new HashMap<>();
    private Set<Long> mDeletedSSIDs = new HashSet();
    public Set<String> mDeletedEphemeralSSIDs = new HashSet();
    private int mLastPriority = -1;
    public boolean enableChipWakeUpWhenAssociated = true;
    public boolean enableRssiPollWhenAssociated = true;
    public int maxTxPacketForNetworkSwitching = 40;
    public int maxRxPacketForNetworkSwitching = 80;
    public int maxTxPacketForFullScans = 8;
    public int maxRxPacketForFullScans = 16;
    public int maxTxPacketForPartialScans = 40;
    public int maxRxPacketForPartialScans = 80;
    public boolean enableFullBandScanWhenAssociated = true;
    public int thresholdInitialAutoJoinAttemptMin5RSSI = WifiConfiguration.INITIAL_AUTO_JOIN_ATTEMPT_MIN_5;
    public int thresholdInitialAutoJoinAttemptMin24RSSI = WifiConfiguration.INITIAL_AUTO_JOIN_ATTEMPT_MIN_24;
    public int wifiConfigLastSelectionHysteresis = 180000;
    public int thresholdUnblacklistThreshold5Hard = WifiConfiguration.UNBLACKLIST_THRESHOLD_5_HARD;
    public int thresholdUnblacklistThreshold5Soft = WifiConfiguration.UNBLACKLIST_THRESHOLD_5_SOFT;
    public int thresholdUnblacklistThreshold24Hard = WifiConfiguration.UNBLACKLIST_THRESHOLD_24_HARD;
    public int thresholdUnblacklistThreshold24Soft = WifiConfiguration.UNBLACKLIST_THRESHOLD_24_SOFT;
    public int enableVerboseLogging = 0;
    boolean showNetworks = true;
    public int alwaysEnableScansWhileAssociated = 0;
    public boolean roamOnAny = false;
    public long lastUnwantedNetworkDisconnectTimestamp = 0;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private String lastSelectedConfiguration = null;

    WifiConfigStore(Context c, WifiNative wn) {
        this.enableAutoJoinScanWhenAssociated = true;
        this.enableAutoJoinWhenAssociated = true;
        this.thresholdBadRssi5 = WifiConfiguration.BAD_RSSI_5;
        this.thresholdLowRssi5 = WifiConfiguration.LOW_RSSI_5;
        this.thresholdGoodRssi5 = WifiConfiguration.GOOD_RSSI_5;
        this.thresholdBadRssi24 = WifiConfiguration.BAD_RSSI_24;
        this.thresholdLowRssi24 = WifiConfiguration.LOW_RSSI_24;
        this.thresholdGoodRssi24 = WifiConfiguration.GOOD_RSSI_24;
        this.associatedFullScanBackoff = 12;
        this.associatedFullScanMaxIntervalMilli = 300000;
        this.networkSwitchingBlackListPeriodMilli = 172800000;
        this.bandPreferenceBoostFactor5 = 5;
        this.bandPreferencePenaltyFactor5 = 2;
        this.bandPreferencePenaltyThreshold5 = WifiConfiguration.G_BAND_PREFERENCE_RSSI_THRESHOLD;
        this.bandPreferenceBoostThreshold5 = WifiConfiguration.A_BAND_PREFERENCE_RSSI_THRESHOLD;
        this.badLinkSpeed24 = 6;
        this.badLinkSpeed5 = 12;
        this.goodLinkSpeed24 = 24;
        this.goodLinkSpeed5 = 36;
        this.maxAuthErrorsToBlacklist = 4;
        this.maxConnectionErrorsToBlacklist = 4;
        this.wifiConfigBlacklistMinTimeMilli = 300000;
        this.associatedHysteresisHigh = 14;
        this.associatedHysteresisLow = 8;
        this.maxNumActiveChannelsForPartialScans = 6;
        this.maxNumPassiveChannelsForPartialScans = 2;
        this.onlyLinkSameCredentialConfigurations = true;
        this.enableLinkDebouncing = true;
        this.enable5GHzPreference = true;
        this.enableWifiCellularHandoverUserTriggeredAdjustment = true;
        this.currentNetworkBoost = 25;
        this.scanResultRssiLevelPatchUp = -85;
        this.mContext = c;
        this.mWifiNative = wn;
        if (this.showNetworks) {
            this.mLocalLog = this.mWifiNative.getLocalLog();
            this.mFileObserver = new WpaConfigFileObserver();
            this.mFileObserver.startWatching();
        } else {
            this.mLocalLog = null;
            this.mFileObserver = null;
        }
        this.associatedPartialScanPeriodMilli = this.mContext.getResources().getInteger(R.integer.config_backgroundUserScheduledStopTimeSecs);
        loge("associatedPartialScanPeriodMilli set to " + this.associatedPartialScanPeriodMilli);
        this.onlyLinkSameCredentialConfigurations = this.mContext.getResources().getBoolean(R.^attr-private.borderBottom);
        this.maxNumActiveChannelsForPartialScans = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_exempted_types);
        this.maxNumPassiveChannelsForPartialScans = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_location_min_duration);
        this.associatedFullScanMaxIntervalMilli = this.mContext.getResources().getInteger(R.integer.config_batterySaver_full_locationMode);
        this.associatedFullScanBackoff = this.mContext.getResources().getInteger(R.integer.config_batteryHistoryStorageSize);
        this.enableLinkDebouncing = this.mContext.getResources().getBoolean(R.^attr-private.autofillSaveCustomSubtitleMaxHeight);
        this.enable5GHzPreference = this.mContext.getResources().getBoolean(R.^attr-private.backgroundLeft);
        this.bandPreferenceBoostFactor5 = this.mContext.getResources().getInteger(R.integer.config_accumulatedBatteryUsageStatsSpanSize);
        this.bandPreferencePenaltyFactor5 = this.mContext.getResources().getInteger(R.integer.config_aggregatedPowerStatsSpanDuration);
        this.bandPreferencePenaltyThreshold5 = this.mContext.getResources().getInteger(R.integer.config_activityShortDur);
        this.bandPreferenceBoostThreshold5 = this.mContext.getResources().getInteger(R.integer.config_accessibilityColorMode);
        this.associatedHysteresisHigh = this.mContext.getResources().getInteger(R.integer.config_activeTaskDurationHours);
        this.associatedHysteresisLow = this.mContext.getResources().getInteger(R.integer.config_activityDefaultDur);
        this.thresholdBadRssi5 = this.mContext.getResources().getInteger(R.integer.config_alertDialogController);
        this.thresholdLowRssi5 = this.mContext.getResources().getInteger(R.integer.config_allowedUnprivilegedKeepalivePerUid);
        this.thresholdGoodRssi5 = this.mContext.getResources().getInteger(R.integer.config_am_tieredCachedAdjUiTierSize);
        this.thresholdBadRssi24 = this.mContext.getResources().getInteger(R.integer.config_app_exit_info_history_list_size);
        this.thresholdLowRssi24 = this.mContext.getResources().getInteger(R.integer.config_attentionMaximumExtension);
        this.thresholdGoodRssi24 = this.mContext.getResources().getInteger(R.integer.config_attentiveTimeout);
        this.enableWifiCellularHandoverUserTriggeredAdjustment = this.mContext.getResources().getBoolean(R.^attr-private.backgroundRequest);
        this.badLinkSpeed24 = this.mContext.getResources().getInteger(R.integer.config_attentiveWarningDuration);
        this.badLinkSpeed5 = this.mContext.getResources().getInteger(R.integer.config_audio_alarm_min_vol);
        this.goodLinkSpeed24 = this.mContext.getResources().getInteger(R.integer.config_audio_notif_vol_default);
        this.goodLinkSpeed5 = this.mContext.getResources().getInteger(R.integer.config_audio_notif_vol_steps);
        this.maxAuthErrorsToBlacklist = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_power_components);
        this.maxConnectionErrorsToBlacklist = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_media_playback_min_duration);
        this.wifiConfigBlacklistMinTimeMilli = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_types_to_bg_restricted);
        this.enableAutoJoinScanWhenAssociated = this.mContext.getResources().getBoolean(R.^attr-private.backgroundRequestDetail);
        this.enableAutoJoinWhenAssociated = this.mContext.getResources().getBoolean(R.^attr-private.backgroundRight);
        this.currentNetworkBoost = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_types_to_restricted_bucket);
        this.scanResultRssiLevelPatchUp = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_window);
        this.networkSwitchingBlackListPeriodMilli = this.mContext.getResources().getInteger(R.integer.config_autoGroupAtCount);
    }

    void enableVerboseLogging(int verbose) {
        this.enableVerboseLogging = verbose;
        if (verbose > 0) {
            VDBG = true;
            this.showNetworks = true;
        } else {
            VDBG = false;
        }
        if (verbose > 1) {
            VVDBG = true;
        } else {
            VVDBG = false;
        }
    }

    class WpaConfigFileObserver extends FileObserver {
        public WpaConfigFileObserver() {
            super(WifiConfigStore.SUPPLICANT_CONFIG_FILE, 8);
        }

        @Override
        public void onEvent(int event, String path) {
            if (event == 8) {
                File file = new File(WifiConfigStore.SUPPLICANT_CONFIG_FILE);
                if (WifiConfigStore.VDBG) {
                    WifiConfigStore.this.localLog("wpa_supplicant.conf changed; new size = " + file.length());
                }
            }
        }
    }

    void loadAndEnableAllNetworks() throws Throwable {
        log("Loading config and enabling all networks ");
        loadConfiguredNetworks();
        enableAllNetworks();
    }

    int getConfiguredNetworksSize() {
        return this.mConfiguredNetworks.size();
    }

    private List<WifiConfiguration> getConfiguredNetworks(Map<String, String> pskMap) {
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            WifiConfiguration newConfig = new WifiConfiguration(config);
            if (config.autoJoinStatus != 200 && !config.ephemeral) {
                if (pskMap != null && config.allowedKeyManagement != null && config.allowedKeyManagement.get(1) && pskMap.containsKey(config.SSID)) {
                    newConfig.preSharedKey = pskMap.get(config.SSID);
                }
                networks.add(newConfig);
            }
        }
        return networks;
    }

    List<WifiConfiguration> getConfiguredNetworks() {
        return getConfiguredNetworks(null);
    }

    List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        Map<String, String> pskMap = getCredentialsBySsidMap();
        return getConfiguredNetworks(pskMap);
    }

    private Map<String, String> getCredentialsBySsidMap() {
        return readNetworkVariablesFromSupplicantFile("psk");
    }

    int getconfiguredNetworkSize() {
        if (this.mConfiguredNetworks == null) {
            return 0;
        }
        return this.mConfiguredNetworks.size();
    }

    List<WifiConfiguration> getRecentConfiguredNetworks(int milli, boolean copy) {
        List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (config.autoJoinStatus != 200 && !config.ephemeral) {
                config.setVisibility(milli);
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
        ScanResult result;
        WifiConfiguration config = getWifiConfiguration(info.getNetworkId());
        if (config != null && config.scanResultCache != null && (result = (ScanResult) config.scanResultCache.get(info.getBSSID())) != null) {
            long previousSeen = result.seen;
            int previousRssi = result.level;
            result.seen = System.currentTimeMillis();
            result.level = info.getRssi();
            result.averageRssi(previousRssi, previousSeen, WifiAutoJoinController.mScanResultMaximumAge);
            if (VDBG) {
                loge("updateConfiguration freq=" + result.frequency + " BSSID=" + result.BSSID + " RSSI=" + result.level + " " + config.configKey());
            }
        }
    }

    WifiConfiguration getWifiConfiguration(int netId) {
        if (this.mConfiguredNetworks == null) {
            return null;
        }
        return this.mConfiguredNetworks.get(Integer.valueOf(netId));
    }

    WifiConfiguration getWifiConfiguration(String key) {
        Integer n;
        if (key == null) {
            return null;
        }
        int hash = key.hashCode();
        if (this.mNetworkIds == null || (n = this.mNetworkIds.get(Integer.valueOf(hash))) == null) {
            return null;
        }
        int netId = n.intValue();
        return getWifiConfiguration(netId);
    }

    void enableAllNetworks() {
        long now = System.currentTimeMillis();
        boolean networkEnabledStateChanged = false;
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (config != null && config.status == 1 && !config.ephemeral && config.autoJoinStatus <= 128 && ((config.disableReason != 2 && config.disableReason != 4 && config.disableReason != 3) || config.blackListTimestamp == 0 || now <= config.blackListTimestamp || now - config.blackListTimestamp >= this.wifiConfigBlacklistMinTimeMilli)) {
                if (this.mWifiNative.enableNetwork(config.networkId, false)) {
                    networkEnabledStateChanged = true;
                    config.status = 2;
                    config.numConnectionFailures = 0;
                    config.numIpConfigFailures = 0;
                    config.numAuthFailures = 0;
                    config.setAutoJoinStatus(0);
                } else {
                    loge("Enable network failed on " + config.networkId);
                }
            }
        }
        if (networkEnabledStateChanged) {
            this.mWifiNative.saveConfig();
            sendConfiguredNetworksChangedBroadcast();
        }
    }

    boolean selectNetwork(int netId) {
        if (VDBG) {
            localLog("selectNetwork", netId);
        }
        if (netId == -1) {
            return false;
        }
        if (this.mLastPriority == -1 || this.mLastPriority > 1000000) {
            for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
                if (config.networkId != -1) {
                    config.priority = 0;
                    addOrUpdateNetworkNative(config, -1);
                }
            }
            this.mLastPriority = 0;
        }
        WifiConfiguration config2 = new WifiConfiguration();
        config2.networkId = netId;
        int i = this.mLastPriority + 1;
        this.mLastPriority = i;
        config2.priority = i;
        addOrUpdateNetworkNative(config2, -1);
        this.mWifiNative.saveConfig();
        enableNetworkWithoutBroadcast(netId, true);
        return true;
    }

    NetworkUpdateResult saveNetwork(WifiConfiguration config, int uid) {
        if (config == null || (config.networkId == -1 && config.SSID == null)) {
            return new NetworkUpdateResult(-1);
        }
        if (VDBG) {
            localLog("WifiConfigStore: saveNetwork netId", config.networkId);
        }
        if (VDBG) {
            loge("WifiConfigStore saveNetwork, size=" + this.mConfiguredNetworks.size() + " SSID=" + config.SSID + " Uid=" + Integer.toString(config.creatorUid) + "/" + Integer.toString(config.lastUpdateUid));
        }
        if (this.mDeletedEphemeralSSIDs.remove(config.SSID) && VDBG) {
            loge("WifiConfigStore: removed from ephemeral blacklist: " + config.SSID);
        }
        boolean newNetwork = config.networkId == -1;
        NetworkUpdateResult result = addOrUpdateNetworkNative(config, uid);
        int netId = result.getNetworkId();
        if (VDBG) {
            localLog("WifiConfigStore: saveNetwork got it back netId=", netId);
        }
        if (newNetwork && netId != -1) {
            if (VDBG) {
                localLog("WifiConfigStore: will enable netId=", netId);
            }
            this.mWifiNative.enableNetwork(netId, false);
            WifiConfiguration conf = this.mConfiguredNetworks.get(Integer.valueOf(netId));
            if (conf != null) {
                conf.status = 2;
            }
        }
        WifiConfiguration conf2 = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (conf2 != null) {
            if (conf2.autoJoinStatus != 0) {
                if (VDBG) {
                    localLog("WifiConfigStore: re-enabling: " + conf2.SSID);
                }
                conf2.setAutoJoinStatus(0);
                enableNetworkWithoutBroadcast(conf2.networkId, false);
            }
            if (VDBG) {
                loge("WifiConfigStore: saveNetwork got config back netId=" + Integer.toString(netId) + " uid=" + Integer.toString(config.creatorUid));
            }
        }
        this.mWifiNative.saveConfig();
        sendConfiguredNetworksChangedBroadcast(conf2, result.isNewNetwork() ? 0 : 2);
        return result;
    }

    void driverRoamedFrom(WifiInfo info) {
        WifiConfiguration config;
        ScanResult result;
        if (info != null && info.getBSSID() != null && ScanResult.is5GHz(info.getFrequency()) && info.getRssi() > this.bandPreferenceBoostThreshold5 + 3 && (config = getWifiConfiguration(info.getNetworkId())) != null && config.scanResultCache != null && (result = (ScanResult) config.scanResultCache.get(info.getBSSID())) != null) {
            result.setAutoJoinStatus(17);
        }
    }

    void noteRoamingFailure(WifiConfiguration config, int reason) {
        if (config != null) {
            config.lastRoamingFailure = System.currentTimeMillis();
            config.roamingFailureBlackListTimeMilli = 2 * (config.roamingFailureBlackListTimeMilli + 1000);
            if (config.roamingFailureBlackListTimeMilli > this.networkSwitchingBlackListPeriodMilli) {
                config.roamingFailureBlackListTimeMilli = this.networkSwitchingBlackListPeriodMilli;
            }
            config.lastRoamingFailureReason = reason;
        }
    }

    void saveWifiConfigBSSID(WifiConfiguration config) {
        if (config != null) {
            if (config.networkId != -1 || config.SSID != null) {
                if ((config.BSSID == null || config.BSSID == "any") && config.autoJoinBSSID != null) {
                    loge("saveWifiConfigBSSID Setting BSSID for " + config.configKey() + " to " + config.autoJoinBSSID);
                    if (!this.mWifiNative.setNetworkVariable(config.networkId, "bssid", config.autoJoinBSSID)) {
                        loge("failed to set BSSID: " + config.autoJoinBSSID);
                    } else if (config.autoJoinBSSID.equals("any")) {
                        this.mWifiNative.saveConfig();
                    }
                }
            }
        }
    }

    void updateStatus(int netId, NetworkInfo.DetailedState state) {
        WifiConfiguration config;
        if (netId != -1 && (config = this.mConfiguredNetworks.get(Integer.valueOf(netId))) != null) {
            switch (AnonymousClass2.$SwitchMap$android$net$NetworkInfo$DetailedState[state.ordinal()]) {
                case 1:
                    config.status = 0;
                    config.setAutoJoinStatus(0);
                    break;
                case 2:
                    if (config.status == 0) {
                        config.status = 2;
                    }
                    break;
            }
        }
    }

    WifiConfiguration disableEphemeralNetwork(String SSID) {
        if (SSID == null) {
            return null;
        }
        WifiConfiguration foundConfig = null;
        this.mDeletedEphemeralSSIDs.add(SSID);
        loge("Forget ephemeral SSID " + SSID + " num=" + this.mDeletedEphemeralSSIDs.size());
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (SSID.equals(config.SSID) && config.ephemeral) {
                loge("Found ephemeral config in disableEphemeralNetwork: " + config.networkId);
                foundConfig = config;
            }
        }
        writeKnownNetworkHistory(true);
        return foundConfig;
    }

    boolean forgetNetwork(int netId) {
        if (this.showNetworks) {
            localLog("forgetNetwork", netId);
        }
        boolean remove = removeConfigAndSendBroadcastIfNeeded(netId);
        if (!remove) {
            return true;
        }
        if (this.mWifiNative.removeNetwork(netId)) {
            this.mWifiNative.saveConfig();
            return true;
        }
        loge("Failed to remove network " + netId);
        return false;
    }

    int addOrUpdateNetwork(WifiConfiguration config, int uid) {
        WifiConfiguration conf;
        if (this.showNetworks) {
            localLog("addOrUpdateNetwork id=", config.networkId);
        }
        Log.e(TAG, " key=" + config.configKey() + " netId=" + Integer.toString(config.networkId) + " uid=" + Integer.toString(config.creatorUid) + "/" + Integer.toString(config.lastUpdateUid));
        NetworkUpdateResult result = addOrUpdateNetworkNative(config, uid);
        if (result.getNetworkId() != -1 && (conf = this.mConfiguredNetworks.get(Integer.valueOf(result.getNetworkId()))) != null) {
            sendConfiguredNetworksChangedBroadcast(conf, result.isNewNetwork ? 0 : 2);
        }
        return result.getNetworkId();
    }

    boolean removeNetwork(int netId) {
        if (this.showNetworks) {
            localLog("removeNetwork", netId);
        }
        boolean ret = this.mWifiNative.removeNetwork(netId);
        if (ret) {
            removeConfigAndSendBroadcastIfNeeded(netId);
        }
        return ret;
    }

    private boolean removeConfigAndSendBroadcastIfNeeded(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            if (VDBG) {
                loge("removeNetwork " + Integer.toString(netId) + " key=" + config.configKey() + " config.id=" + Integer.toString(config.networkId));
            }
            if (config.configKey().equals(this.lastSelectedConfiguration)) {
                this.lastSelectedConfiguration = null;
            }
            if (config.enterpriseConfig != null) {
                removeKeys(config.enterpriseConfig);
            }
            if ((config.selfAdded || config.linkedConfigurations != null || config.allowedKeyManagement.get(1)) && !TextUtils.isEmpty(config.SSID)) {
                Checksum csum = new CRC32();
                if (config.SSID != null) {
                    csum.update(config.SSID.getBytes(), 0, config.SSID.getBytes().length);
                    this.mDeletedSSIDs.add(Long.valueOf(csum.getValue()));
                }
                loge("removeNetwork " + Integer.toString(netId) + " key=" + config.configKey() + " config.id=" + Integer.toString(config.networkId) + "  crc=" + csum.getValue());
            }
            this.mConfiguredNetworks.remove(Integer.valueOf(netId));
            this.mNetworkIds.remove(Integer.valueOf(configKey(config)));
            writeIpAndProxyConfigurations();
            sendConfiguredNetworksChangedBroadcast(config, 1);
            writeKnownNetworkHistory(true);
        }
        return true;
    }

    boolean enableNetwork(int netId, boolean disableOthers) {
        WifiConfiguration enabledNetwork;
        boolean ret = enableNetworkWithoutBroadcast(netId, disableOthers);
        if (disableOthers) {
            if (VDBG) {
                localLog("enableNetwork(disableOthers=true) ", netId);
            }
            sendConfiguredNetworksChangedBroadcast();
        } else {
            if (VDBG) {
                localLog("enableNetwork(disableOthers=false) ", netId);
            }
            synchronized (this.mConfiguredNetworks) {
                enabledNetwork = this.mConfiguredNetworks.get(Integer.valueOf(netId));
            }
            if (enabledNetwork != null) {
                sendConfiguredNetworksChangedBroadcast(enabledNetwork, 2);
            }
        }
        return ret;
    }

    boolean enableNetworkWithoutBroadcast(int netId, boolean disableOthers) {
        boolean ret = this.mWifiNative.enableNetwork(netId, disableOthers);
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            config.status = 2;
        }
        if (disableOthers) {
            markAllNetworksDisabledExcept(netId);
        }
        return ret;
    }

    void disableAllNetworks() {
        if (VDBG) {
            localLog("disableAllNetworks");
        }
        boolean networkDisabled = false;
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (config != null && config.status != 1) {
                if (this.mWifiNative.disableNetwork(config.networkId)) {
                    networkDisabled = true;
                    config.status = 1;
                } else {
                    loge("Disable network failed on " + config.networkId);
                }
            }
        }
        if (networkDisabled) {
            sendConfiguredNetworksChangedBroadcast();
        }
    }

    boolean disableNetwork(int netId) {
        return disableNetwork(netId, 0);
    }

    boolean disableNetwork(int netId, int reason) {
        if (VDBG) {
            localLog("disableNetwork", netId);
        }
        boolean ret = this.mWifiNative.disableNetwork(netId);
        WifiConfiguration network = null;
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (VDBG && config != null) {
            loge("disableNetwork netId=" + Integer.toString(netId) + " SSID=" + config.SSID + " disabled=" + (config.status == 1) + " reason=" + Integer.toString(config.disableReason));
        }
        if (config != null) {
            if (config.status != 1 && config.disableReason != 5) {
                config.status = 1;
                config.disableReason = reason;
                network = config;
            }
            if (reason == 5) {
                config.status = 1;
                config.autoJoinStatus = 161;
            }
        }
        if (network != null) {
            sendConfiguredNetworksChangedBroadcast(network, 2);
        }
        return ret;
    }

    boolean saveConfig() {
        return this.mWifiNative.saveConfig();
    }

    WpsResult startWpsWithPinFromAccessPoint(WpsInfo config) {
        WpsResult result = new WpsResult();
        if (this.mWifiNative.startWpsRegistrar(config.BSSID, config.pin)) {
            markAllNetworksDisabled();
            result.status = WpsResult.Status.SUCCESS;
        } else {
            loge("Failed to start WPS pin method configuration");
            result.status = WpsResult.Status.FAILURE;
        }
        return result;
    }

    WpsResult startWpsWithPinFromDevice(WpsInfo config) {
        WpsResult result = new WpsResult();
        result.pin = this.mWifiNative.startWpsPinDisplay(config.BSSID);
        if (!TextUtils.isEmpty(result.pin)) {
            markAllNetworksDisabled();
            result.status = WpsResult.Status.SUCCESS;
        } else {
            loge("Failed to start WPS pin method configuration");
            result.status = WpsResult.Status.FAILURE;
        }
        return result;
    }

    WpsResult startWpsPbc(WpsInfo config) {
        WpsResult result = new WpsResult();
        if (this.mWifiNative.startWpsPbc(config.BSSID)) {
            markAllNetworksDisabled();
            result.status = WpsResult.Status.SUCCESS;
        } else {
            loge("Failed to start WPS push button configuration");
            result.status = WpsResult.Status.FAILURE;
        }
        return result;
    }

    StaticIpConfiguration getStaticIpConfiguration(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            return config.getStaticIpConfiguration();
        }
        return null;
    }

    void setStaticIpConfiguration(int netId, StaticIpConfiguration staticIpConfiguration) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            config.setStaticIpConfiguration(staticIpConfiguration);
        }
    }

    void setDefaultGwMacAddress(int netId, String macAddress) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            config.defaultGwMacAddress = macAddress;
        }
    }

    ProxyInfo getProxyProperties(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            return config.getHttpProxy();
        }
        return null;
    }

    boolean isUsingStaticIp(int netId) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        return config != null && config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
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

    void loadConfiguredNetworks() throws Throwable {
        BufferedReader reader;
        this.mLastPriority = 0;
        this.mConfiguredNetworks.clear();
        this.mNetworkIds.clear();
        int last_id = -1;
        boolean done = false;
        while (!done) {
            String listStr = this.mWifiNative.listNetworks(last_id);
            if (listStr == null) {
                return;
            }
            String[] lines = listStr.split(SEPARATOR_KEY);
            if (this.showNetworks) {
                localLog("WifiConfigStore: loadConfiguredNetworks:  ");
                for (String net : lines) {
                    localLog(net);
                }
            }
            for (int i = 1; i < lines.length; i++) {
                String[] result = lines[i].split("\t");
                WifiConfiguration config = new WifiConfiguration();
                try {
                    config.networkId = Integer.parseInt(result[0]);
                    last_id = config.networkId;
                    if (result.length <= 3) {
                        config.status = 2;
                    } else if (result[3].indexOf("[CURRENT]") != -1) {
                        config.status = 0;
                    } else if (result[3].indexOf("[DISABLED]") != -1) {
                        config.status = 1;
                    } else {
                        config.status = 2;
                    }
                    readNetworkVariables(config);
                    Checksum csum = new CRC32();
                    if (config.SSID != null) {
                        csum.update(config.SSID.getBytes(), 0, config.SSID.getBytes().length);
                        long d = csum.getValue();
                        if (this.mDeletedSSIDs.contains(Long.valueOf(d))) {
                            loge(" got CRC for SSID " + config.SSID + " -> " + d + ", was deleted");
                        }
                    }
                    if (config.priority > this.mLastPriority) {
                        this.mLastPriority = config.priority;
                    }
                    config.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                    config.setProxySettings(IpConfiguration.ProxySettings.NONE);
                    if (this.mNetworkIds.containsKey(Integer.valueOf(configKey(config)))) {
                        if (this.showNetworks) {
                            localLog("discarded duplicate network ", config.networkId);
                        }
                    } else if (config.isValid()) {
                        this.mConfiguredNetworks.put(Integer.valueOf(config.networkId), config);
                        this.mNetworkIds.put(Integer.valueOf(configKey(config)), Integer.valueOf(config.networkId));
                        if (this.showNetworks) {
                            localLog("loaded configured network", config.networkId);
                        }
                    } else if (this.showNetworks) {
                        log("Ignoring loaded configured for network " + config.networkId + " because config are not valid");
                    }
                } catch (NumberFormatException e) {
                    loge("Failed to read network-id '" + result[0] + "'");
                }
            }
            done = lines.length == 1;
        }
        readIpAndProxyConfigurations();
        readNetworkHistory();
        readAutoJoinConfig();
        sendConfiguredNetworksChangedBroadcast();
        if (this.showNetworks) {
            localLog("loadConfiguredNetworks loaded " + this.mNetworkIds.size() + " networks");
        }
        if (this.mNetworkIds.size() == 0) {
            BufferedReader reader2 = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(SUPPLICANT_CONFIG_FILE));
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e2) {
                e = e2;
            } catch (IOException e3) {
                e = e3;
            }
            try {
                localLog("--- Begin wpa_supplicant.conf Contents ---", true);
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    localLog(line, true);
                }
                localLog("--- End wpa_supplicant.conf Contents ---", true);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (FileNotFoundException e5) {
                e = e5;
                reader2 = reader;
                localLog("Could not open /data/misc/wifi/wpa_supplicant.conf, " + e, true);
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e6) {
                    }
                }
            } catch (IOException e7) {
                e = e7;
                reader2 = reader;
                localLog("Could not read /data/misc/wifi/wpa_supplicant.conf, " + e, true);
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e8) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e9) {
                    }
                }
                throw th;
            }
        }
    }

    private Map<String, String> readNetworkVariablesFromSupplicantFile(String key) throws Throwable {
        BufferedReader reader;
        boolean found;
        String networkSsid;
        String value;
        Map<String, String> result = new HashMap<>();
        BufferedReader reader2 = null;
        if (VDBG) {
            loge("readNetworkVariablesFromSupplicantFile key=" + key);
        }
        try {
            try {
                reader = new BufferedReader(new FileReader(SUPPLICANT_CONFIG_FILE));
                found = false;
                networkSsid = null;
                value = null;
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
                if (line.matches("[ \\t]*network=\\{")) {
                    found = true;
                    networkSsid = null;
                    value = null;
                } else if (line.matches("[ \\t]*\\}")) {
                    found = false;
                    networkSsid = null;
                    value = null;
                }
                if (found) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("ssid=")) {
                        networkSsid = trimmedLine.substring(5);
                    } else if (trimmedLine.startsWith(key + "=")) {
                        value = trimmedLine.substring(key.length() + 1);
                    }
                    if (networkSsid != null && value != null) {
                        result.put(networkSsid, value);
                    }
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                }
            }
        } catch (FileNotFoundException e4) {
            e = e4;
            reader2 = reader;
            if (VDBG) {
                loge("Could not open /data/misc/wifi/wpa_supplicant.conf, " + e);
            }
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e5) {
                }
            }
        } catch (IOException e6) {
            e = e6;
            reader2 = reader;
            if (VDBG) {
                loge("Could not read /data/misc/wifi/wpa_supplicant.conf, " + e);
            }
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
        return result;
    }

    private String readNetworkVariableFromSupplicantFile(String ssid, String key) throws Throwable {
        long start = SystemClock.elapsedRealtimeNanos();
        Map<String, String> data = readNetworkVariablesFromSupplicantFile(key);
        long end = SystemClock.elapsedRealtimeNanos();
        if (VDBG) {
            loge("readNetworkVariableFromSupplicantFile ssid=[" + ssid + "] key=" + key + " duration=" + (end - start));
        }
        return data.get(ssid);
    }

    private void markAllNetworksDisabledExcept(int netId) {
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (config != null && config.networkId != netId && config.status != 1) {
                config.status = 1;
                config.disableReason = 0;
            }
        }
    }

    private void markAllNetworksDisabled() {
        markAllNetworksDisabledExcept(-1);
    }

    boolean needsUnlockedKeyStore() {
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (config.allowedKeyManagement.get(2) && config.allowedKeyManagement.get(3) && needsSoftwareBackedKeyStore(config.enterpriseConfig)) {
                return true;
            }
        }
        return false;
    }

    public void writeKnownNetworkHistory(boolean force) {
        boolean needUpdate = force;
        final List<WifiConfiguration> networks = new ArrayList<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            networks.add(new WifiConfiguration(config));
            if (config.dirty) {
                loge(" rewrite network history for " + config.configKey());
                config.dirty = false;
                needUpdate = true;
            }
        }
        if (VDBG) {
            loge(" writeKnownNetworkHistory() num networks:" + this.mConfiguredNetworks.size() + " needWrite=" + needUpdate);
        }
        if (needUpdate) {
            this.mWriter.write(networkHistoryConfigFile, new DelayedDiskWrite.Writer() {
                public void onWriteCalled(DataOutputStream out) throws IOException {
                    for (WifiConfiguration config2 : networks) {
                        if (WifiConfigStore.VDBG) {
                            int num = 0;
                            int numlink = 0;
                            if (config2.connectChoices != null) {
                                num = config2.connectChoices.size();
                            }
                            if (config2.linkedConfigurations != null) {
                                numlink = config2.linkedConfigurations.size();
                            }
                            WifiConfigStore.this.loge("saving network history: " + config2.configKey() + " gw: " + config2.defaultGwMacAddress + " autojoin-status: " + config2.autoJoinStatus + " ephemeral=" + config2.ephemeral + " choices:" + Integer.toString(num) + " link:" + Integer.toString(numlink) + " status:" + Integer.toString(config2.status) + " nid:" + Integer.toString(config2.networkId));
                        }
                        if (config2.isValid()) {
                            if (config2.SSID == null) {
                                if (WifiConfigStore.VDBG) {
                                    WifiConfigStore.this.loge("writeKnownNetworkHistory trying to write config with null SSID");
                                }
                            } else {
                                if (WifiConfigStore.VDBG) {
                                    WifiConfigStore.this.loge("writeKnownNetworkHistory write config " + config2.configKey());
                                }
                                out.writeUTF(WifiConfigStore.CONFIG_KEY + config2.configKey() + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SSID_KEY + config2.SSID + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.FQDN_KEY + config2.FQDN + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.PRIORITY_KEY + Integer.toString(config2.priority) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.STATUS_KEY + Integer.toString(config2.autoJoinStatus) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SUPPLICANT_STATUS_KEY + Integer.toString(config2.status) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SUPPLICANT_DISABLE_REASON_KEY + Integer.toString(config2.disableReason) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.NETWORK_ID_KEY + Integer.toString(config2.networkId) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SELF_ADDED_KEY + Boolean.toString(config2.selfAdded) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.DID_SELF_ADD_KEY + Boolean.toString(config2.didSelfAdd) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.NO_INTERNET_ACCESS_REPORTS_KEY + Integer.toString(config2.numNoInternetAccessReports) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.VALIDATED_INTERNET_ACCESS_KEY + Boolean.toString(config2.validatedInternetAccess) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.EPHEMERAL_KEY + Boolean.toString(config2.ephemeral) + WifiConfigStore.SEPARATOR_KEY);
                                if (config2.peerWifiConfiguration != null) {
                                    out.writeUTF(WifiConfigStore.PEER_CONFIGURATION_KEY + config2.peerWifiConfiguration + WifiConfigStore.SEPARATOR_KEY);
                                }
                                out.writeUTF(WifiConfigStore.NUM_CONNECTION_FAILURES_KEY + Integer.toString(config2.numConnectionFailures) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.NUM_AUTH_FAILURES_KEY + Integer.toString(config2.numAuthFailures) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.NUM_IP_CONFIG_FAILURES_KEY + Integer.toString(config2.numIpConfigFailures) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SCORER_OVERRIDE_KEY + Integer.toString(config2.numScorerOverride) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SCORER_OVERRIDE_AND_SWITCH_KEY + Integer.toString(config2.numScorerOverrideAndSwitchedNetwork) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.NUM_ASSOCIATION_KEY + Integer.toString(config2.numAssociation) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.JOIN_ATTEMPT_BOOST_KEY + Integer.toString(config2.autoJoinUseAggressiveJoinAttemptThreshold) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.CREATOR_UID_KEY + Integer.toString(config2.creatorUid) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.CONNECT_UID_KEY + Integer.toString(config2.lastConnectUid) + WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.UPDATE_UID_KEY + Integer.toString(config2.lastUpdateUid) + WifiConfigStore.SEPARATOR_KEY);
                                String allowedKeyManagementString = WifiConfigStore.makeString(config2.allowedKeyManagement, WifiConfiguration.KeyMgmt.strings);
                                out.writeUTF(WifiConfigStore.AUTH_KEY + allowedKeyManagementString + WifiConfigStore.SEPARATOR_KEY);
                                if (config2.connectChoices != null) {
                                    for (String key : config2.connectChoices.keySet()) {
                                        Integer choice = (Integer) config2.connectChoices.get(key);
                                        out.writeUTF(WifiConfigStore.CHOICE_KEY + key + "=" + choice.toString() + WifiConfigStore.SEPARATOR_KEY);
                                    }
                                }
                                if (config2.linkedConfigurations != null) {
                                    WifiConfigStore.this.loge("writeKnownNetworkHistory write linked " + config2.linkedConfigurations.size());
                                    Iterator i$ = config2.linkedConfigurations.keySet().iterator();
                                    while (i$.hasNext()) {
                                        out.writeUTF(WifiConfigStore.LINK_KEY + ((String) i$.next()) + WifiConfigStore.SEPARATOR_KEY);
                                    }
                                }
                                String macAddress = config2.defaultGwMacAddress;
                                if (macAddress != null) {
                                    out.writeUTF(WifiConfigStore.DEFAULT_GW_KEY + macAddress + WifiConfigStore.SEPARATOR_KEY);
                                }
                                if (config2.scanResultCache != null) {
                                    for (ScanResult result : config2.scanResultCache.values()) {
                                        out.writeUTF(WifiConfigStore.BSSID_KEY + result.BSSID + WifiConfigStore.SEPARATOR_KEY);
                                        out.writeUTF(WifiConfigStore.FREQ_KEY + Integer.toString(result.frequency) + WifiConfigStore.SEPARATOR_KEY);
                                        out.writeUTF(WifiConfigStore.RSSI_KEY + Integer.toString(result.level) + WifiConfigStore.SEPARATOR_KEY);
                                        out.writeUTF(WifiConfigStore.BSSID_STATUS_KEY + Integer.toString(result.autoJoinStatus) + WifiConfigStore.SEPARATOR_KEY);
                                        out.writeUTF("/BSSID:  \n");
                                    }
                                }
                                if (config2.lastFailure != null) {
                                    out.writeUTF(WifiConfigStore.FAILURE_KEY + config2.lastFailure + WifiConfigStore.SEPARATOR_KEY);
                                }
                                out.writeUTF(WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SEPARATOR_KEY);
                                out.writeUTF(WifiConfigStore.SEPARATOR_KEY);
                            }
                        }
                    }
                    if (WifiConfigStore.this.mDeletedSSIDs != null && WifiConfigStore.this.mDeletedSSIDs.size() > 0) {
                        for (Long i : WifiConfigStore.this.mDeletedSSIDs) {
                            out.writeUTF(WifiConfigStore.DELETED_CRC32_KEY);
                            out.writeUTF(String.valueOf(i));
                            out.writeUTF(WifiConfigStore.SEPARATOR_KEY);
                        }
                    }
                    if (WifiConfigStore.this.mDeletedEphemeralSSIDs != null && WifiConfigStore.this.mDeletedEphemeralSSIDs.size() > 0) {
                        for (String ssid : WifiConfigStore.this.mDeletedEphemeralSSIDs) {
                            out.writeUTF(WifiConfigStore.DELETED_EPHEMERAL_KEY);
                            out.writeUTF(ssid);
                            out.writeUTF(WifiConfigStore.SEPARATOR_KEY);
                        }
                    }
                }
            });
        }
    }

    public void setLastSelectedConfiguration(int netId) {
        if (VDBG) {
            loge("setLastSelectedConfiguration " + Integer.toString(netId));
        }
        if (netId == -1) {
            this.lastSelectedConfiguration = null;
            return;
        }
        WifiConfiguration selected = getWifiConfiguration(netId);
        if (selected == null) {
            this.lastSelectedConfiguration = null;
            return;
        }
        this.lastSelectedConfiguration = selected.configKey();
        selected.numConnectionFailures = 0;
        selected.numIpConfigFailures = 0;
        selected.numAuthFailures = 0;
        selected.numNoInternetAccessReports = 0;
        if (VDBG) {
            loge("setLastSelectedConfiguration now: " + this.lastSelectedConfiguration);
        }
    }

    public String getLastSelectedConfiguration() {
        return this.lastSelectedConfiguration;
    }

    public boolean isLastSelectedConfiguration(WifiConfiguration config) {
        return (this.lastSelectedConfiguration == null || config == null || !this.lastSelectedConfiguration.equals(config.configKey())) ? false : true;
    }

    private void readNetworkHistory() {
        int choice;
        if (this.showNetworks) {
            localLog("readNetworkHistory() path:" + networkHistoryConfigFile);
        }
        DataInputStream in = null;
        try {
            DataInputStream in2 = new DataInputStream(new BufferedInputStream(new FileInputStream(networkHistoryConfigFile)));
            WifiConfiguration config = null;
            while (true) {
                try {
                    String key = in2.readUTF();
                    String bssid = null;
                    String ssid = null;
                    int freq = 0;
                    int status = 0;
                    long seen = 0;
                    int rssi = WifiConfiguration.INVALID_RSSI;
                    String caps = null;
                    if (key.startsWith(CONFIG_KEY)) {
                        if (config != null) {
                            config = null;
                        }
                        String configKey = key.replace(CONFIG_KEY, "").replace(SEPARATOR_KEY, "");
                        Integer n = this.mNetworkIds.get(Integer.valueOf(configKey.hashCode()));
                        if (n == null) {
                            localLog("readNetworkHistory didnt find netid for hash=" + Integer.toString(configKey.hashCode()) + " key: " + configKey);
                        } else {
                            WifiConfiguration config2 = this.mConfiguredNetworks.get(n);
                            config = config2;
                            if (config == null) {
                                localLog("readNetworkHistory didnt find config for netid=" + n.toString() + " key: " + configKey);
                            }
                            int rssi2 = WifiConfiguration.INVALID_RSSI;
                        }
                    } else if (config != null) {
                        if (key.startsWith(SSID_KEY)) {
                            String ssid2 = key.replace(SSID_KEY, "");
                            ssid = ssid2.replace(SEPARATOR_KEY, "");
                            if (config.SSID != null && !config.SSID.equals(ssid)) {
                                loge("Error parsing network history file, mismatched SSIDs");
                                config = null;
                                ssid = null;
                            } else {
                                config.SSID = ssid;
                            }
                        }
                        if (key.startsWith(FQDN_KEY)) {
                            String fqdn = key.replace(FQDN_KEY, "");
                            config.FQDN = fqdn.replace(SEPARATOR_KEY, "");
                        }
                        if (key.startsWith(DEFAULT_GW_KEY)) {
                            String gateway = key.replace(DEFAULT_GW_KEY, "");
                            config.defaultGwMacAddress = gateway.replace(SEPARATOR_KEY, "");
                        }
                        if (key.startsWith(STATUS_KEY)) {
                            String st = key.replace(STATUS_KEY, "");
                            config.autoJoinStatus = Integer.parseInt(st.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(SUPPLICANT_DISABLE_REASON_KEY)) {
                            String reason = key.replace(SUPPLICANT_DISABLE_REASON_KEY, "");
                            config.disableReason = Integer.parseInt(reason.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(SELF_ADDED_KEY)) {
                            String selfAdded = key.replace(SELF_ADDED_KEY, "");
                            config.selfAdded = Boolean.parseBoolean(selfAdded.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(DID_SELF_ADD_KEY)) {
                            String didSelfAdd = key.replace(DID_SELF_ADD_KEY, "");
                            config.didSelfAdd = Boolean.parseBoolean(didSelfAdd.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(NO_INTERNET_ACCESS_REPORTS_KEY)) {
                            String access = key.replace(NO_INTERNET_ACCESS_REPORTS_KEY, "");
                            config.numNoInternetAccessReports = Integer.parseInt(access.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(VALIDATED_INTERNET_ACCESS_KEY)) {
                            String access2 = key.replace(VALIDATED_INTERNET_ACCESS_KEY, "");
                            config.validatedInternetAccess = Boolean.parseBoolean(access2.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(EPHEMERAL_KEY)) {
                            String access3 = key.replace(EPHEMERAL_KEY, "");
                            config.ephemeral = Boolean.parseBoolean(access3.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(CREATOR_UID_KEY)) {
                            String uid = key.replace(CREATOR_UID_KEY, "");
                            config.creatorUid = Integer.parseInt(uid.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(BLACKLIST_MILLI_KEY)) {
                            String milli = key.replace(BLACKLIST_MILLI_KEY, "");
                            config.blackListTimestamp = Long.parseLong(milli.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(NUM_CONNECTION_FAILURES_KEY)) {
                            String num = key.replace(NUM_CONNECTION_FAILURES_KEY, "");
                            config.numConnectionFailures = Integer.parseInt(num.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(NUM_IP_CONFIG_FAILURES_KEY)) {
                            String num2 = key.replace(NUM_IP_CONFIG_FAILURES_KEY, "");
                            config.numIpConfigFailures = Integer.parseInt(num2.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(NUM_AUTH_FAILURES_KEY)) {
                            String num3 = key.replace(NUM_AUTH_FAILURES_KEY, "");
                            config.numIpConfigFailures = Integer.parseInt(num3.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(SCORER_OVERRIDE_KEY)) {
                            String num4 = key.replace(SCORER_OVERRIDE_KEY, "");
                            config.numScorerOverride = Integer.parseInt(num4.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(SCORER_OVERRIDE_AND_SWITCH_KEY)) {
                            String num5 = key.replace(SCORER_OVERRIDE_AND_SWITCH_KEY, "");
                            config.numScorerOverrideAndSwitchedNetwork = Integer.parseInt(num5.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(NUM_ASSOCIATION_KEY)) {
                            String num6 = key.replace(NUM_ASSOCIATION_KEY, "");
                            config.numAssociation = Integer.parseInt(num6.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(JOIN_ATTEMPT_BOOST_KEY)) {
                            String num7 = key.replace(JOIN_ATTEMPT_BOOST_KEY, "");
                            config.autoJoinUseAggressiveJoinAttemptThreshold = Integer.parseInt(num7.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(CONNECT_UID_KEY)) {
                            String uid2 = key.replace(CONNECT_UID_KEY, "");
                            config.lastConnectUid = Integer.parseInt(uid2.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(UPDATE_UID_KEY)) {
                            String uid3 = key.replace(UPDATE_UID_KEY, "");
                            config.lastUpdateUid = Integer.parseInt(uid3.replace(SEPARATOR_KEY, ""));
                        }
                        if (key.startsWith(FAILURE_KEY)) {
                            config.lastFailure = key.replace(FAILURE_KEY, "");
                            config.lastFailure = config.lastFailure.replace(SEPARATOR_KEY, "");
                        }
                        if (key.startsWith(PEER_CONFIGURATION_KEY)) {
                            config.peerWifiConfiguration = key.replace(PEER_CONFIGURATION_KEY, "");
                            config.peerWifiConfiguration = config.peerWifiConfiguration.replace(SEPARATOR_KEY, "");
                        }
                        if (key.startsWith(CHOICE_KEY)) {
                            String choiceStr = key.replace(CHOICE_KEY, "").replace(SEPARATOR_KEY, "");
                            Matcher match = mConnectChoice.matcher(choiceStr);
                            if (!match.find()) {
                                Log.d(TAG, "WifiConfigStore: connectChoice:  Couldnt match pattern : " + choiceStr);
                            } else {
                                String configKey2 = match.group(1);
                                try {
                                    choice = Integer.parseInt(match.group(2));
                                } catch (NumberFormatException e) {
                                    choice = 0;
                                }
                                if (choice > 0) {
                                    if (config.connectChoices == null) {
                                        config.connectChoices = new HashMap();
                                    }
                                    config.connectChoices.put(configKey2, Integer.valueOf(choice));
                                }
                            }
                        }
                        if (key.startsWith(LINK_KEY)) {
                            String configKey3 = key.replace(LINK_KEY, "").replace(SEPARATOR_KEY, "");
                            if (config.linkedConfigurations == null) {
                                config.linkedConfigurations = new HashMap();
                            }
                            if (config.linkedConfigurations != null) {
                                config.linkedConfigurations.put(configKey3, -1);
                            }
                        }
                        if (key.startsWith(BSSID_KEY)) {
                            if (key.startsWith(BSSID_KEY)) {
                                String bssid2 = key.replace(BSSID_KEY, "");
                                bssid = bssid2.replace(SEPARATOR_KEY, "");
                                freq = 0;
                                seen = 0;
                                rssi = WifiConfiguration.INVALID_RSSI;
                                caps = "";
                                status = 0;
                            }
                            if (key.startsWith(RSSI_KEY)) {
                                String lvl = key.replace(RSSI_KEY, "");
                                rssi = Integer.parseInt(lvl.replace(SEPARATOR_KEY, ""));
                            }
                            if (key.startsWith(BSSID_STATUS_KEY)) {
                                String st2 = key.replace(BSSID_STATUS_KEY, "");
                                status = Integer.parseInt(st2.replace(SEPARATOR_KEY, ""));
                            }
                            if (key.startsWith(FREQ_KEY)) {
                                String channel = key.replace(FREQ_KEY, "");
                                freq = Integer.parseInt(channel.replace(SEPARATOR_KEY, ""));
                            }
                            if (key.startsWith(DATE_KEY)) {
                            }
                            if (key.startsWith(BSSID_KEY_END) && bssid != null && ssid != null) {
                                if (config.scanResultCache == null) {
                                    config.scanResultCache = new HashMap();
                                }
                                WifiSsid wssid = WifiSsid.createFromAsciiEncoded(ssid);
                                ScanResult result = new ScanResult(wssid, bssid, caps, rssi, freq, 0L);
                                result.seen = seen;
                                config.scanResultCache.put(bssid, result);
                                result.autoJoinStatus = status;
                            }
                            if (key.startsWith(DELETED_CRC32_KEY)) {
                                String crc = key.replace(DELETED_CRC32_KEY, "");
                                Long c = Long.valueOf(Long.parseLong(crc));
                                this.mDeletedSSIDs.add(c);
                            }
                            if (key.startsWith(DELETED_EPHEMERAL_KEY)) {
                                String s = key.replace(DELETED_EPHEMERAL_KEY, "");
                                if (!TextUtils.isEmpty(s)) {
                                    this.mDeletedEphemeralSSIDs.add(s.replace(SEPARATOR_KEY, ""));
                                }
                            }
                        }
                    }
                } catch (EOFException e2) {
                    in = in2;
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception e3) {
                            loge("readNetworkHistory: Error reading file" + e3);
                        }
                    }
                    if (in == null) {
                        try {
                            in.close();
                            return;
                        } catch (Exception e4) {
                            loge("readNetworkHistory: Error closing file" + e4);
                            return;
                        }
                    }
                    return;
                } catch (IOException e5) {
                    e = e5;
                    in = in2;
                    loge("readNetworkHistory: No config file, revert to default" + e);
                    if (in == null) {
                    }
                }
            }
        } catch (EOFException e6) {
        } catch (IOException e7) {
            e = e7;
        }
    }

    private void readAutoJoinConfig() {
        BufferedReader reader;
        BufferedReader reader2 = null;
        try {
            reader = new BufferedReader(new FileReader(autoJoinConfigFile));
        } catch (EOFException e) {
        } catch (FileNotFoundException e2) {
        } catch (IOException e3) {
            e = e3;
        }
        try {
            for (String key = reader.readLine(); key != null; key = reader.readLine()) {
                if (key != null) {
                    Log.d(TAG, "readAutoJoinConfig line: " + key);
                }
                if (key.startsWith(ENABLE_AUTO_JOIN_WHILE_ASSOCIATED_KEY)) {
                    String st = key.replace(ENABLE_AUTO_JOIN_WHILE_ASSOCIATED_KEY, "");
                    try {
                        this.enableAutoJoinWhenAssociated = Integer.parseInt(st.replace(SEPARATOR_KEY, "")) != 0;
                        Log.d(TAG, "readAutoJoinConfig: enabled = " + this.enableAutoJoinWhenAssociated);
                    } catch (NumberFormatException e4) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ENABLE_FULL_BAND_SCAN_WHEN_ASSOCIATED_KEY)) {
                    String st2 = key.replace(ENABLE_FULL_BAND_SCAN_WHEN_ASSOCIATED_KEY, "");
                    try {
                        this.enableFullBandScanWhenAssociated = Integer.parseInt(st2.replace(SEPARATOR_KEY, "")) != 0;
                        Log.d(TAG, "readAutoJoinConfig: enableFullBandScanWhenAssociated = " + this.enableFullBandScanWhenAssociated);
                    } catch (NumberFormatException e5) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ENABLE_AUTO_JOIN_SCAN_WHILE_ASSOCIATED_KEY)) {
                    String st3 = key.replace(ENABLE_AUTO_JOIN_SCAN_WHILE_ASSOCIATED_KEY, "");
                    try {
                        this.enableAutoJoinScanWhenAssociated = Integer.parseInt(st3.replace(SEPARATOR_KEY, "")) != 0;
                        Log.d(TAG, "readAutoJoinConfig: enabled = " + this.enableAutoJoinScanWhenAssociated);
                    } catch (NumberFormatException e6) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ENABLE_CHIP_WAKE_UP_WHILE_ASSOCIATED_KEY)) {
                    String st4 = key.replace(ENABLE_CHIP_WAKE_UP_WHILE_ASSOCIATED_KEY, "");
                    try {
                        this.enableChipWakeUpWhenAssociated = Integer.parseInt(st4.replace(SEPARATOR_KEY, "")) != 0;
                        Log.d(TAG, "readAutoJoinConfig: enabled = " + this.enableChipWakeUpWhenAssociated);
                    } catch (NumberFormatException e7) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ENABLE_RSSI_POLL_WHILE_ASSOCIATED_KEY)) {
                    String st5 = key.replace(ENABLE_RSSI_POLL_WHILE_ASSOCIATED_KEY, "");
                    try {
                        this.enableRssiPollWhenAssociated = Integer.parseInt(st5.replace(SEPARATOR_KEY, "")) != 0;
                        Log.d(TAG, "readAutoJoinConfig: enabled = " + this.enableRssiPollWhenAssociated);
                    } catch (NumberFormatException e8) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_5G_KEY)) {
                    String st6 = key.replace(THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_5G_KEY, "");
                    try {
                        this.thresholdInitialAutoJoinAttemptMin5RSSI = Integer.parseInt(st6.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdInitialAutoJoinAttemptMin5RSSI = " + Integer.toString(this.thresholdInitialAutoJoinAttemptMin5RSSI));
                    } catch (NumberFormatException e9) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_24G_KEY)) {
                    String st7 = key.replace(THRESHOLD_INITIAL_AUTO_JOIN_ATTEMPT_RSSI_MIN_24G_KEY, "");
                    try {
                        this.thresholdInitialAutoJoinAttemptMin24RSSI = Integer.parseInt(st7.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdInitialAutoJoinAttemptMin24RSSI = " + Integer.toString(this.thresholdInitialAutoJoinAttemptMin24RSSI));
                    } catch (NumberFormatException e10) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_UNBLACKLIST_HARD_5G_KEY)) {
                    String st8 = key.replace(THRESHOLD_UNBLACKLIST_HARD_5G_KEY, "");
                    try {
                        this.thresholdUnblacklistThreshold5Hard = Integer.parseInt(st8.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdUnblacklistThreshold5Hard = " + Integer.toString(this.thresholdUnblacklistThreshold5Hard));
                    } catch (NumberFormatException e11) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_UNBLACKLIST_SOFT_5G_KEY)) {
                    String st9 = key.replace(THRESHOLD_UNBLACKLIST_SOFT_5G_KEY, "");
                    try {
                        this.thresholdUnblacklistThreshold5Soft = Integer.parseInt(st9.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdUnblacklistThreshold5Soft = " + Integer.toString(this.thresholdUnblacklistThreshold5Soft));
                    } catch (NumberFormatException e12) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_UNBLACKLIST_HARD_24G_KEY)) {
                    String st10 = key.replace(THRESHOLD_UNBLACKLIST_HARD_24G_KEY, "");
                    try {
                        this.thresholdUnblacklistThreshold24Hard = Integer.parseInt(st10.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdUnblacklistThreshold24Hard = " + Integer.toString(this.thresholdUnblacklistThreshold24Hard));
                    } catch (NumberFormatException e13) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_UNBLACKLIST_SOFT_24G_KEY)) {
                    String st11 = key.replace(THRESHOLD_UNBLACKLIST_SOFT_24G_KEY, "");
                    try {
                        this.thresholdUnblacklistThreshold24Soft = Integer.parseInt(st11.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdUnblacklistThreshold24Soft = " + Integer.toString(this.thresholdUnblacklistThreshold24Soft));
                    } catch (NumberFormatException e14) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_GOOD_RSSI_5_KEY)) {
                    String st12 = key.replace(THRESHOLD_GOOD_RSSI_5_KEY, "");
                    try {
                        this.thresholdGoodRssi5 = Integer.parseInt(st12.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdGoodRssi5 = " + Integer.toString(this.thresholdGoodRssi5));
                    } catch (NumberFormatException e15) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_LOW_RSSI_5_KEY)) {
                    String st13 = key.replace(THRESHOLD_LOW_RSSI_5_KEY, "");
                    try {
                        this.thresholdLowRssi5 = Integer.parseInt(st13.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdLowRssi5 = " + Integer.toString(this.thresholdLowRssi5));
                    } catch (NumberFormatException e16) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_BAD_RSSI_5_KEY)) {
                    String st14 = key.replace(THRESHOLD_BAD_RSSI_5_KEY, "");
                    try {
                        this.thresholdBadRssi5 = Integer.parseInt(st14.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdBadRssi5 = " + Integer.toString(this.thresholdBadRssi5));
                    } catch (NumberFormatException e17) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_GOOD_RSSI_24_KEY)) {
                    String st15 = key.replace(THRESHOLD_GOOD_RSSI_24_KEY, "");
                    try {
                        this.thresholdGoodRssi24 = Integer.parseInt(st15.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdGoodRssi24 = " + Integer.toString(this.thresholdGoodRssi24));
                    } catch (NumberFormatException e18) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_LOW_RSSI_24_KEY)) {
                    String st16 = key.replace(THRESHOLD_LOW_RSSI_24_KEY, "");
                    try {
                        this.thresholdLowRssi24 = Integer.parseInt(st16.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdLowRssi24 = " + Integer.toString(this.thresholdLowRssi24));
                    } catch (NumberFormatException e19) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_BAD_RSSI_24_KEY)) {
                    String st17 = key.replace(THRESHOLD_BAD_RSSI_24_KEY, "");
                    try {
                        this.thresholdBadRssi24 = Integer.parseInt(st17.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: thresholdBadRssi24 = " + Integer.toString(this.thresholdBadRssi24));
                    } catch (NumberFormatException e20) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_MAX_TX_PACKETS_FOR_NETWORK_SWITCHING_KEY)) {
                    String st18 = key.replace(THRESHOLD_MAX_TX_PACKETS_FOR_NETWORK_SWITCHING_KEY, "");
                    try {
                        this.maxTxPacketForNetworkSwitching = Integer.parseInt(st18.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxTxPacketForNetworkSwitching = " + Integer.toString(this.maxTxPacketForNetworkSwitching));
                    } catch (NumberFormatException e21) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_MAX_RX_PACKETS_FOR_NETWORK_SWITCHING_KEY)) {
                    String st19 = key.replace(THRESHOLD_MAX_RX_PACKETS_FOR_NETWORK_SWITCHING_KEY, "");
                    try {
                        this.maxRxPacketForNetworkSwitching = Integer.parseInt(st19.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxRxPacketForNetworkSwitching = " + Integer.toString(this.maxRxPacketForNetworkSwitching));
                    } catch (NumberFormatException e22) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_MAX_TX_PACKETS_FOR_FULL_SCANS_KEY)) {
                    String st20 = key.replace(THRESHOLD_MAX_TX_PACKETS_FOR_FULL_SCANS_KEY, "");
                    try {
                        this.maxTxPacketForNetworkSwitching = Integer.parseInt(st20.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxTxPacketForFullScans = " + Integer.toString(this.maxTxPacketForFullScans));
                    } catch (NumberFormatException e23) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_MAX_RX_PACKETS_FOR_FULL_SCANS_KEY)) {
                    String st21 = key.replace(THRESHOLD_MAX_RX_PACKETS_FOR_FULL_SCANS_KEY, "");
                    try {
                        this.maxRxPacketForNetworkSwitching = Integer.parseInt(st21.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxRxPacketForFullScans = " + Integer.toString(this.maxRxPacketForFullScans));
                    } catch (NumberFormatException e24) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_MAX_TX_PACKETS_FOR_PARTIAL_SCANS_KEY)) {
                    String st22 = key.replace(THRESHOLD_MAX_TX_PACKETS_FOR_PARTIAL_SCANS_KEY, "");
                    try {
                        this.maxTxPacketForNetworkSwitching = Integer.parseInt(st22.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxTxPacketForPartialScans = " + Integer.toString(this.maxTxPacketForPartialScans));
                    } catch (NumberFormatException e25) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(THRESHOLD_MAX_RX_PACKETS_FOR_PARTIAL_SCANS_KEY)) {
                    String st23 = key.replace(THRESHOLD_MAX_RX_PACKETS_FOR_PARTIAL_SCANS_KEY, "");
                    try {
                        this.maxRxPacketForNetworkSwitching = Integer.parseInt(st23.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxRxPacketForPartialScans = " + Integer.toString(this.maxRxPacketForPartialScans));
                    } catch (NumberFormatException e26) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(WIFI_VERBOSE_LOGS_KEY)) {
                    String st24 = key.replace(WIFI_VERBOSE_LOGS_KEY, "");
                    try {
                        this.enableVerboseLogging = Integer.parseInt(st24.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: enable verbose logs = " + Integer.toString(this.enableVerboseLogging));
                    } catch (NumberFormatException e27) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(A_BAND_PREFERENCE_RSSI_THRESHOLD_KEY)) {
                    String st25 = key.replace(A_BAND_PREFERENCE_RSSI_THRESHOLD_KEY, "");
                    try {
                        this.bandPreferenceBoostThreshold5 = Integer.parseInt(st25.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: bandPreferenceBoostThreshold5 = " + Integer.toString(this.bandPreferenceBoostThreshold5));
                    } catch (NumberFormatException e28) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ASSOCIATED_PARTIAL_SCAN_PERIOD_KEY)) {
                    String st26 = key.replace(ASSOCIATED_PARTIAL_SCAN_PERIOD_KEY, "");
                    try {
                        this.associatedPartialScanPeriodMilli = Integer.parseInt(st26.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: associatedScanPeriod = " + Integer.toString(this.associatedPartialScanPeriodMilli));
                    } catch (NumberFormatException e29) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ASSOCIATED_FULL_SCAN_BACKOFF_KEY)) {
                    String st27 = key.replace(ASSOCIATED_FULL_SCAN_BACKOFF_KEY, "");
                    try {
                        this.associatedFullScanBackoff = Integer.parseInt(st27.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: associatedFullScanBackoff = " + Integer.toString(this.associatedFullScanBackoff));
                    } catch (NumberFormatException e30) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(G_BAND_PREFERENCE_RSSI_THRESHOLD_KEY)) {
                    String st28 = key.replace(G_BAND_PREFERENCE_RSSI_THRESHOLD_KEY, "");
                    try {
                        this.bandPreferencePenaltyThreshold5 = Integer.parseInt(st28.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: bandPreferencePenaltyThreshold5 = " + Integer.toString(this.bandPreferencePenaltyThreshold5));
                    } catch (NumberFormatException e31) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(ALWAYS_ENABLE_SCAN_WHILE_ASSOCIATED_KEY)) {
                    String st29 = key.replace(ALWAYS_ENABLE_SCAN_WHILE_ASSOCIATED_KEY, "");
                    try {
                        this.alwaysEnableScansWhileAssociated = Integer.parseInt(st29.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: alwaysEnableScansWhileAssociated = " + Integer.toString(this.alwaysEnableScansWhileAssociated));
                    } catch (NumberFormatException e32) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(MAX_NUM_PASSIVE_CHANNELS_FOR_PARTIAL_SCANS_KEY)) {
                    String st30 = key.replace(MAX_NUM_PASSIVE_CHANNELS_FOR_PARTIAL_SCANS_KEY, "");
                    try {
                        this.maxNumPassiveChannelsForPartialScans = Integer.parseInt(st30.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxNumPassiveChannelsForPartialScans = " + Integer.toString(this.maxNumPassiveChannelsForPartialScans));
                    } catch (NumberFormatException e33) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
                if (key.startsWith(MAX_NUM_ACTIVE_CHANNELS_FOR_PARTIAL_SCANS_KEY)) {
                    String st31 = key.replace(MAX_NUM_ACTIVE_CHANNELS_FOR_PARTIAL_SCANS_KEY, "");
                    try {
                        this.maxNumActiveChannelsForPartialScans = Integer.parseInt(st31.replace(SEPARATOR_KEY, ""));
                        Log.d(TAG, "readAutoJoinConfig: maxNumActiveChannelsForPartialScans = " + Integer.toString(this.maxNumActiveChannelsForPartialScans));
                    } catch (NumberFormatException e34) {
                        Log.d(TAG, "readAutoJoinConfig: incorrect format :" + key);
                    }
                }
            }
            reader2 = reader;
        } catch (EOFException e35) {
            reader2 = reader;
            if (reader2 != null) {
                try {
                    reader2.close();
                    reader2 = null;
                } catch (Exception e36) {
                    loge("readAutoJoinStatus: Error closing file" + e36);
                }
            }
        } catch (FileNotFoundException e37) {
            reader2 = reader;
            if (reader2 != null) {
                try {
                    reader2.close();
                    reader2 = null;
                } catch (Exception e38) {
                    loge("readAutoJoinStatus: Error closing file" + e38);
                }
            }
        } catch (IOException e39) {
            e = e39;
            reader2 = reader;
            loge("readAutoJoinStatus: Error parsing configuration" + e);
        }
        if (reader2 != null) {
            try {
                reader2.close();
            } catch (Exception e40) {
                loge("readAutoJoinStatus: Error closing file" + e40);
            }
        }
    }

    private void writeIpAndProxyConfigurations() {
        SparseArray<IpConfiguration> networks = new SparseArray<>();
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (!config.ephemeral && config.autoJoinStatus != 200) {
                networks.put(configKey(config), config.getIpConfiguration());
            }
        }
        super.writeIpAndProxyConfigurations(ipConfigFile, networks);
    }

    private void readIpAndProxyConfigurations() {
        SparseArray<IpConfiguration> networks = super.readIpAndProxyConfigurations(ipConfigFile);
        if (networks != null && networks.size() != 0) {
            for (int i = 0; i < networks.size(); i++) {
                int id = networks.keyAt(i);
                WifiConfiguration config = this.mConfiguredNetworks.get(this.mNetworkIds.get(Integer.valueOf(id)));
                if (config == null || config.autoJoinStatus == 200 || config.ephemeral) {
                    loge("configuration found for missing network, nid=" + id + ", ignored, networks.size=" + Integer.toString(networks.size()));
                } else {
                    config.setIpConfiguration(networks.valueAt(i));
                }
            }
        }
    }

    private String encodeSSID(String str) {
        String tmp = removeDoubleQuotes(str);
        return String.format("%x", new BigInteger(1, tmp.getBytes(Charset.forName("UTF-8"))));
    }

    private NetworkUpdateResult addOrUpdateNetworkNative(WifiConfiguration config, int uid) {
        if (VDBG) {
            localLog("addOrUpdateNetworkNative " + config.getPrintableSsid());
        }
        int netId = config.networkId;
        boolean newNetwork = false;
        if (netId == -1) {
            Integer savedNetId = this.mNetworkIds.get(Integer.valueOf(configKey(config)));
            if (savedNetId == null) {
                Iterator<WifiConfiguration> it = this.mConfiguredNetworks.values().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    WifiConfiguration test = it.next();
                    if (test.configKey().equals(config.configKey())) {
                        savedNetId = Integer.valueOf(test.networkId);
                        loge("addOrUpdateNetworkNative " + config.configKey() + " was found, but no network Id");
                        break;
                    }
                }
            }
            if (savedNetId != null) {
                netId = savedNetId.intValue();
            } else {
                newNetwork = true;
                netId = this.mWifiNative.addNetwork();
                if (netId < 0) {
                    loge("Failed to add a network!");
                    return new NetworkUpdateResult(-1);
                }
                loge("addOrUpdateNetworkNative created netId=" + netId);
            }
        }
        boolean updateFailed = true;
        if (config.SSID != null && config.NOT_UTF8) {
            byte[] ssid_value = null;
            try {
                ssid_value = config.SSID.getBytes("GBK");
            } catch (UnsupportedEncodingException e) {
                loge("Unsupported Encoding Exception");
            }
            if (!this.mWifiNative.setNetworkVariable(netId, "ssid", ssid_value)) {
                loge("failed to set converted SSID: " + config.SSID);
            }
        } else if (config.SSID != null) {
            if (!this.mWifiNative.setNetworkVariable(netId, "ssid", encodeSSID(config.SSID))) {
                loge("failed to set SSID: " + config.SSID);
            } else if (config.BSSID != null) {
                loge("Setting BSSID for " + config.configKey() + " to " + config.BSSID);
                if (!this.mWifiNative.setNetworkVariable(netId, "bssid", config.BSSID)) {
                    loge("failed to set BSSID: " + config.BSSID);
                } else {
                    String allowedKeyManagementString = makeString(config.allowedKeyManagement, WifiConfiguration.KeyMgmt.strings);
                    if (config.allowedKeyManagement.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "key_mgmt", allowedKeyManagementString)) {
                        loge("failed to set key_mgmt: " + allowedKeyManagementString);
                    } else {
                        String allowedProtocolsString = makeString(config.allowedProtocols, WifiConfiguration.Protocol.strings);
                        if (config.allowedProtocols.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "proto", allowedProtocolsString)) {
                            loge("failed to set proto: " + allowedProtocolsString);
                        } else {
                            String allowedAuthAlgorithmsString = makeString(config.allowedAuthAlgorithms, WifiConfiguration.AuthAlgorithm.strings);
                            if (config.allowedAuthAlgorithms.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "auth_alg", allowedAuthAlgorithmsString)) {
                                loge("failed to set auth_alg: " + allowedAuthAlgorithmsString);
                            } else {
                                String allowedPairwiseCiphersString = makeString(config.allowedPairwiseCiphers, WifiConfiguration.PairwiseCipher.strings);
                                if (config.allowedPairwiseCiphers.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "pairwise", allowedPairwiseCiphersString)) {
                                    loge("failed to set pairwise: " + allowedPairwiseCiphersString);
                                } else {
                                    String allowedGroupCiphersString = makeString(config.allowedGroupCiphers, WifiConfiguration.GroupCipher.strings);
                                    if (config.allowedGroupCiphers.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "group", allowedGroupCiphersString)) {
                                        loge("failed to set group: " + allowedGroupCiphersString);
                                    } else if (config.preSharedKey == null || config.preSharedKey.equals("*")) {
                                        if (config.wapiPsk == null || config.wapiPsk.equals("*")) {
                                            if (config.wapiPskType != null) {
                                                if (!this.mWifiNative.setNetworkVariable(netId, "wapi_psk_type", config.wapiPskType)) {
                                                    loge("failed to set wapiPskType: " + config.wapiPskType);
                                                } else if (config.wapiRootCert == null || config.wapiRootCert.equals("*")) {
                                                    if (config.wapiUserCert == null || config.wapiUserCert.equals("*")) {
                                                        if (config.wapiPkcs12Key == null || config.wapiPkcs12Key.equals("*")) {
                                                            boolean hasSetKey = false;
                                                            if (config.wepKeys != null) {
                                                                for (int i = 0; i < config.wepKeys.length; i++) {
                                                                    if (config.wepKeys[i] != null && !config.wepKeys[i].equals("*")) {
                                                                        if (!this.mWifiNative.setNetworkVariable(netId, WifiConfiguration.wepKeyVarNames[i], config.wepKeys[i])) {
                                                                            loge("failed to set wep_key" + i + ": " + config.wepKeys[i]);
                                                                            break;
                                                                        }
                                                                        hasSetKey = true;
                                                                    }
                                                                }
                                                                if (!hasSetKey) {
                                                                    if (!this.mWifiNative.setNetworkVariable(netId, "wep_tx_keyidx", Integer.toString(config.wepTxKeyIndex))) {
                                                                        loge("failed to set wep_tx_keyidx: " + config.wepTxKeyIndex);
                                                                    } else if (!this.mWifiNative.setNetworkVariable(netId, "priority", Integer.toString(config.priority))) {
                                                                        loge(config.SSID + ": failed to set priority: " + config.priority);
                                                                    } else if (config.hiddenSSID) {
                                                                        if (!this.mWifiNative.setNetworkVariable(netId, "scan_ssid", Integer.toString(config.hiddenSSID ? 1 : 0))) {
                                                                            loge(config.SSID + ": failed to set hiddenSSID: " + config.hiddenSSID);
                                                                        } else if (config.requirePMF && !this.mWifiNative.setNetworkVariable(netId, "ieee80211w", "2")) {
                                                                            loge(config.SSID + ": failed to set requirePMF: " + config.requirePMF);
                                                                        } else if (config.updateIdentifier != null) {
                                                                            if (!this.mWifiNative.setNetworkVariable(netId, "update_identifier", config.updateIdentifier)) {
                                                                                loge(config.SSID + ": failed to set updateIdentifier: " + config.updateIdentifier);
                                                                            } else if (config.enterpriseConfig != null && config.enterpriseConfig.getEapMethod() != -1) {
                                                                                WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
                                                                                if (needsKeyStore(enterpriseConfig)) {
                                                                                    if (this.mKeyStore.state() != KeyStore.State.UNLOCKED) {
                                                                                        loge(config.SSID + ": key store is locked");
                                                                                    } else {
                                                                                        try {
                                                                                            String keyId = config.getKeyIdForCredentials(this.mConfiguredNetworks.get(Integer.valueOf(netId)));
                                                                                            if (!installKeys(enterpriseConfig, keyId)) {
                                                                                                loge(config.SSID + ": failed to install keys");
                                                                                            }
                                                                                        } catch (IllegalStateException e2) {
                                                                                            loge(config.SSID + " invalid config for key installation");
                                                                                        }
                                                                                    }
                                                                                } else {
                                                                                    HashMap<String, String> enterpriseFields = enterpriseConfig.getFields();
                                                                                    for (String key : enterpriseFields.keySet()) {
                                                                                        String value = enterpriseFields.get(key);
                                                                                        if (!key.equals("password") || value == null || !value.equals("*")) {
                                                                                            if (!this.mWifiNative.setNetworkVariable(netId, key, value)) {
                                                                                                removeKeys(enterpriseConfig);
                                                                                                loge(config.SSID + ": failed to set " + key + ": " + value);
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    updateFailed = false;
                                                                                }
                                                                            } else {
                                                                                updateFailed = false;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            } else if (!hasSetKey) {
                                                            }
                                                        } else if (!this.mWifiNative.setNetworkVariable(netId, "wapi_pkcs12_key", config.wapiPkcs12Key)) {
                                                            loge("failed to set wapi_pkcs12_key: " + config.wapiPkcs12Key);
                                                        }
                                                    } else if (!this.mWifiNative.setNetworkVariable(netId, "wapi_user_cert", config.wapiUserCert)) {
                                                        loge("failed to set wapiuser: " + config.wapiUserCert);
                                                    }
                                                } else if (!this.mWifiNative.setNetworkVariable(netId, "wapi_root_cert", config.wapiRootCert)) {
                                                    loge("failed to set wapiroot: " + config.wapiRootCert);
                                                }
                                            }
                                        } else if (!this.mWifiNative.setNetworkVariable(netId, "wapi_psk", config.wapiPsk)) {
                                            loge("failed to set wapipsk: " + config.wapiPsk);
                                        }
                                    } else if (!this.mWifiNative.setNetworkVariable(netId, "psk", config.preSharedKey)) {
                                        loge("failed to set psk");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (updateFailed) {
            if (newNetwork) {
                this.mWifiNative.removeNetwork(netId);
                loge("Failed to set a network variable, removed network: " + netId);
            }
            return new NetworkUpdateResult(-1);
        }
        WifiConfiguration currentConfig = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (currentConfig == null) {
            currentConfig = new WifiConfiguration();
            currentConfig.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
            currentConfig.setProxySettings(IpConfiguration.ProxySettings.NONE);
            currentConfig.networkId = netId;
            if (config != null) {
                currentConfig.selfAdded = config.selfAdded;
                currentConfig.didSelfAdd = config.didSelfAdd;
                currentConfig.ephemeral = config.ephemeral;
                currentConfig.autoJoinUseAggressiveJoinAttemptThreshold = config.autoJoinUseAggressiveJoinAttemptThreshold;
                currentConfig.lastConnectUid = config.lastConnectUid;
                currentConfig.lastUpdateUid = config.lastUpdateUid;
                currentConfig.creatorUid = config.creatorUid;
                currentConfig.peerWifiConfiguration = config.peerWifiConfiguration;
            }
            loge("created new config netId=" + Integer.toString(netId) + " uid=" + Integer.toString(currentConfig.creatorUid));
        }
        if (uid >= 0) {
            if (newNetwork) {
                currentConfig.creatorUid = uid;
            } else {
                currentConfig.lastUpdateUid = uid;
            }
        }
        if (newNetwork) {
            currentConfig.dirty = true;
        }
        if (currentConfig.autoJoinStatus == 200) {
            currentConfig.setAutoJoinStatus(0);
            currentConfig.selfAdded = false;
            currentConfig.didSelfAdd = false;
            loge("remove deleted status netId=" + Integer.toString(netId) + " " + currentConfig.configKey());
        }
        if (currentConfig.status == 2) {
            currentConfig.setAutoJoinStatus(0);
        }
        if (currentConfig.configKey().equals(getLastSelectedConfiguration()) && currentConfig.ephemeral) {
            currentConfig.ephemeral = false;
            loge("remove ephemeral status netId=" + Integer.toString(netId) + " " + currentConfig.configKey());
        }
        loge("will read network variables netId=" + Integer.toString(netId));
        readNetworkVariables(currentConfig);
        this.mConfiguredNetworks.put(Integer.valueOf(netId), currentConfig);
        this.mNetworkIds.put(Integer.valueOf(configKey(currentConfig)), Integer.valueOf(netId));
        NetworkUpdateResult result = writeIpAndProxyConfigurationsOnChange(currentConfig, config);
        result.setIsNewNetwork(newNetwork);
        result.setNetworkId(netId);
        writeKnownNetworkHistory(false);
        return result;
    }

    public void linkConfiguration(WifiConfiguration config) throws Throwable {
        if ((config.scanResultCache == null || config.scanResultCache.size() <= 6) && config.allowedKeyManagement.get(1)) {
            for (WifiConfiguration link : this.mConfiguredNetworks.values()) {
                boolean doLink = false;
                if (!link.configKey().equals(config.configKey()) && link.autoJoinStatus != 200 && !link.ephemeral && link.allowedKeyManagement.equals(config.allowedKeyManagement) && (link.scanResultCache == null || link.scanResultCache.size() <= 6)) {
                    if (config.defaultGwMacAddress != null && link.defaultGwMacAddress != null) {
                        if (config.defaultGwMacAddress.equals(link.defaultGwMacAddress)) {
                            if (VDBG) {
                                loge("linkConfiguration link due to same gw " + link.SSID + " and " + config.SSID + " GW " + config.defaultGwMacAddress);
                            }
                            doLink = true;
                        }
                    } else if (config.scanResultCache != null && config.scanResultCache.size() <= 6 && link.scanResultCache != null && link.scanResultCache.size() <= 6) {
                        for (String abssid : config.scanResultCache.keySet()) {
                            for (String bbssid : link.scanResultCache.keySet()) {
                                if (VVDBG) {
                                    loge("linkConfiguration try to link due to DBDC BSSID match " + link.SSID + " and " + config.SSID + " bssida " + abssid + " bssidb " + bbssid);
                                }
                                if (abssid.regionMatches(true, 0, bbssid, 0, 16)) {
                                    doLink = true;
                                }
                            }
                        }
                    }
                    if (doLink && this.onlyLinkSameCredentialConfigurations) {
                        String apsk = readNetworkVariableFromSupplicantFile(link.SSID, "psk");
                        String bpsk = readNetworkVariableFromSupplicantFile(config.SSID, "psk");
                        if (apsk == null || bpsk == null || TextUtils.isEmpty(apsk) || TextUtils.isEmpty(apsk) || apsk.equals("*") || apsk.equals(DELETED_CONFIG_PSK) || !apsk.equals(bpsk)) {
                            doLink = false;
                        }
                    }
                    if (doLink) {
                        if (VDBG) {
                            loge("linkConfiguration: will link " + link.configKey() + " and " + config.configKey());
                        }
                        if (link.linkedConfigurations == null) {
                            link.linkedConfigurations = new HashMap();
                        }
                        if (config.linkedConfigurations == null) {
                            config.linkedConfigurations = new HashMap();
                        }
                        if (link.linkedConfigurations.get(config.configKey()) == null) {
                            link.linkedConfigurations.put(config.configKey(), 1);
                            link.dirty = true;
                        }
                        if (config.linkedConfigurations.get(link.configKey()) == null) {
                            config.linkedConfigurations.put(link.configKey(), 1);
                            config.dirty = true;
                        }
                    } else {
                        if (link.linkedConfigurations != null && link.linkedConfigurations.get(config.configKey()) != null) {
                            if (VDBG) {
                                loge("linkConfiguration: un-link " + config.configKey() + " from " + link.configKey());
                            }
                            link.dirty = true;
                            link.linkedConfigurations.remove(config.configKey());
                        }
                        if (config.linkedConfigurations != null && config.linkedConfigurations.get(link.configKey()) != null) {
                            if (VDBG) {
                                loge("linkConfiguration: un-link " + link.configKey() + " from " + config.configKey());
                            }
                            config.dirty = true;
                            config.linkedConfigurations.remove(link.configKey());
                        }
                    }
                }
            }
        }
    }

    public WifiConfiguration associateWithConfiguration(ScanResult result) throws Throwable {
        boolean doNotAdd = false;
        String configKey = WifiConfiguration.configKey(result);
        if (configKey == null) {
            loge("associateWithConfiguration(): no config key ");
            return null;
        }
        String SSID = "\"" + result.SSID + "\"";
        if (VVDBG) {
            loge("associateWithConfiguration(): try " + configKey);
        }
        Checksum csum = new CRC32();
        csum.update(SSID.getBytes(), 0, SSID.getBytes().length);
        if (this.mDeletedSSIDs.contains(Long.valueOf(csum.getValue()))) {
            doNotAdd = true;
        }
        WifiConfiguration config = null;
        for (WifiConfiguration link : this.mConfiguredNetworks.values()) {
            boolean doLink = false;
            if (link.autoJoinStatus == 200 || link.selfAdded || link.ephemeral) {
                if (VVDBG) {
                    loge("associateWithConfiguration(): skip selfadd " + link.configKey());
                }
            } else if (!link.allowedKeyManagement.get(1)) {
                if (VVDBG) {
                    loge("associateWithConfiguration(): skip non-PSK " + link.configKey());
                }
            } else {
                if (configKey.equals(link.configKey())) {
                    if (VVDBG) {
                        loge("associateWithConfiguration(): found it!!! " + configKey);
                        return link;
                    }
                    return link;
                }
                if (!doNotAdd && link.scanResultCache != null && link.scanResultCache.size() <= 6) {
                    Iterator i$ = link.scanResultCache.keySet().iterator();
                    while (true) {
                        if (!i$.hasNext()) {
                            break;
                        }
                        String bssid = (String) i$.next();
                        if (result.BSSID.regionMatches(true, 0, bssid, 0, 16) && SSID.regionMatches(false, 0, link.SSID, 0, 4)) {
                            doLink = true;
                            break;
                        }
                    }
                }
                if (doLink) {
                    if (VDBG) {
                        loge("associateWithConfiguration: try to create " + result.SSID + " and associate it with: " + link.SSID + " key " + link.configKey());
                    }
                    config = wifiConfigurationFromScanResult(result);
                    if (config != null) {
                        config.selfAdded = true;
                        config.didSelfAdd = true;
                        config.dirty = true;
                        config.peerWifiConfiguration = link.configKey();
                        if (config.allowedKeyManagement.equals(link.allowedKeyManagement) && config.allowedKeyManagement.get(1)) {
                            if (VDBG && config != null) {
                                loge("associateWithConfiguration: got a config from beacon" + config.SSID + " key " + config.configKey());
                            }
                            String psk = readNetworkVariableFromSupplicantFile(link.SSID, "psk");
                            if (psk != null) {
                                config.preSharedKey = psk;
                                if (VDBG && config.preSharedKey != null) {
                                    loge(" transfer PSK : " + config.preSharedKey);
                                }
                                if (link.linkedConfigurations == null) {
                                    link.linkedConfigurations = new HashMap();
                                }
                                if (config.linkedConfigurations == null) {
                                    config.linkedConfigurations = new HashMap();
                                }
                                link.linkedConfigurations.put(config.configKey(), 1);
                                config.linkedConfigurations.put(link.configKey(), 1);
                                if (link.getIpConfiguration() != null) {
                                    config.setIpConfiguration(link.getIpConfiguration());
                                }
                            } else {
                                config = null;
                            }
                        } else {
                            config = null;
                        }
                        if (config != null) {
                            break;
                        }
                    }
                    if (VDBG && config != null) {
                        loge("associateWithConfiguration: success, created: " + config.SSID + " key " + config.configKey());
                    }
                } else {
                    continue;
                }
            }
        }
        return config;
    }

    public HashSet<Integer> makeChannelList(WifiConfiguration config, int age, boolean restrict) {
        if (config == null) {
            return null;
        }
        long now_ms = System.currentTimeMillis();
        HashSet<Integer> channels = new HashSet<>();
        if (config.scanResultCache == null && config.linkedConfigurations == null) {
            return null;
        }
        if (VDBG) {
            StringBuilder dbg = new StringBuilder();
            dbg.append("makeChannelList age=" + Integer.toString(age) + " for " + config.configKey() + " max=" + this.maxNumActiveChannelsForPartialScans);
            if (config.scanResultCache != null) {
                dbg.append(" bssids=" + config.scanResultCache.size());
            }
            if (config.linkedConfigurations != null) {
                dbg.append(" linked=" + config.linkedConfigurations.size());
            }
            loge(dbg.toString());
        }
        int numChannels = 0;
        if (config.scanResultCache != null && config.scanResultCache.size() > 0) {
            for (ScanResult result : config.scanResultCache.values()) {
                if (numChannels > this.maxNumActiveChannelsForPartialScans) {
                    break;
                }
                if (VDBG) {
                    boolean test = now_ms - result.seen < ((long) age);
                    loge("has " + result.BSSID + " freq=" + Integer.toString(result.frequency) + " age=" + Long.toString(now_ms - result.seen) + " ?=" + test);
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
                if (linked != null && linked.scanResultCache != null) {
                    for (ScanResult result2 : linked.scanResultCache.values()) {
                        if (VDBG) {
                            loge("has link: " + result2.BSSID + " freq=" + Integer.toString(result2.frequency) + " age=" + Long.toString(now_ms - result2.seen));
                        }
                        if (numChannels <= this.maxNumActiveChannelsForPartialScans) {
                            if (now_ms - result2.seen < age) {
                                channels.add(Integer.valueOf(result2.frequency));
                                numChannels++;
                            }
                        }
                    }
                }
            }
            return channels;
        }
        return channels;
    }

    public boolean updateSavedNetworkHistory(ScanResult scanResult) throws Throwable {
        int numConfigFound = 0;
        if (scanResult == null) {
            return false;
        }
        String SSID = "\"" + scanResult.SSID + "\"";
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            boolean found = false;
            if (config.SSID == null || !config.SSID.equals(SSID)) {
                if (VVDBG) {
                    loge("updateSavedNetworkHistory(): SSID mismatch " + config.configKey() + " SSID=" + config.SSID + " " + SSID);
                }
            } else {
                if (VDBG) {
                    loge("updateSavedNetworkHistory(): try " + config.configKey() + " SSID=" + config.SSID + " " + scanResult.SSID + " " + scanResult.capabilities + " ajst=" + config.autoJoinStatus);
                }
                if (scanResult.capabilities.contains("WEP") && config.configKey().contains("WEP")) {
                    found = true;
                } else if (scanResult.capabilities.contains("PSK") && config.configKey().contains("PSK")) {
                    found = true;
                } else if (scanResult.capabilities.contains("EAP") && config.configKey().contains("EAP")) {
                    found = true;
                } else if (!scanResult.capabilities.contains("WEP") && !scanResult.capabilities.contains("PSK") && !scanResult.capabilities.contains("EAP") && !config.configKey().contains("WEP") && !config.configKey().contains("PSK") && !config.configKey().contains("EAP")) {
                    found = true;
                }
                if (found) {
                    numConfigFound++;
                    if (config.autoJoinStatus >= 200) {
                        if (VVDBG) {
                            loge("updateSavedNetworkHistory(): found a deleted, skip it...  " + config.configKey());
                        }
                    } else {
                        if (config.scanResultCache == null) {
                            config.scanResultCache = new HashMap();
                        }
                        ScanResult result = (ScanResult) config.scanResultCache.get(scanResult.BSSID);
                        if (result == null) {
                            config.dirty = true;
                        } else {
                            scanResult.autoJoinStatus = result.autoJoinStatus;
                            scanResult.blackListTimestamp = result.blackListTimestamp;
                            scanResult.numIpConfigFailures = result.numIpConfigFailures;
                            scanResult.numConnection = result.numConnection;
                            scanResult.isAutoJoinCandidate = result.isAutoJoinCandidate;
                        }
                        if (config.ephemeral) {
                            scanResult.untrusted = true;
                        }
                        if (config.scanResultCache.size() > 192) {
                            long now_dbg = 0;
                            if (VVDBG) {
                                loge(" Will trim config " + config.configKey() + " size " + config.scanResultCache.size());
                                for (ScanResult scanResult2 : config.scanResultCache.values()) {
                                    loge("     " + result.BSSID + " " + result.seen);
                                }
                                now_dbg = SystemClock.elapsedRealtimeNanos();
                            }
                            config.trimScanResultsCache(maxNumScanCacheEntries);
                            if (VVDBG) {
                                long diff = SystemClock.elapsedRealtimeNanos() - now_dbg;
                                loge(" Finished trimming config, time(ns) " + diff);
                                for (ScanResult r : config.scanResultCache.values()) {
                                    loge("     " + r.BSSID + " " + r.seen);
                                }
                            }
                        }
                        config.scanResultCache.put(scanResult.BSSID, scanResult);
                        linkConfiguration(config);
                    }
                }
                if (VDBG && found) {
                    String status = "";
                    if (scanResult.autoJoinStatus > 0) {
                        status = " status=" + Integer.toString(scanResult.autoJoinStatus);
                    }
                    loge("        got known scan result " + scanResult.BSSID + " key : " + config.configKey() + " num: " + Integer.toString(config.scanResultCache.size()) + " rssi=" + Integer.toString(scanResult.level) + " freq=" + Integer.toString(scanResult.frequency) + status);
                }
            }
        }
        return numConfigFound != 0;
    }

    private NetworkUpdateResult writeIpAndProxyConfigurationsOnChange(WifiConfiguration currentConfig, WifiConfiguration newConfig) {
        boolean ipChanged = false;
        boolean proxyChanged = false;
        if (VDBG) {
            loge("writeIpAndProxyConfigurationsOnChange: " + currentConfig.SSID + " -> " + newConfig.SSID + " path: " + ipConfigFile);
        }
        switch (AnonymousClass2.$SwitchMap$android$net$IpConfiguration$IpAssignment[newConfig.getIpAssignment().ordinal()]) {
            case 1:
                if (currentConfig.getIpAssignment() != newConfig.getIpAssignment()) {
                    ipChanged = true;
                } else if (!Objects.equals(currentConfig.getStaticIpConfiguration(), newConfig.getStaticIpConfiguration())) {
                    ipChanged = true;
                } else {
                    ipChanged = false;
                }
                break;
            case 2:
                if (currentConfig.getIpAssignment() != newConfig.getIpAssignment()) {
                    ipChanged = true;
                }
                break;
            case 3:
                break;
            default:
                loge("Ignore invalid ip assignment during write");
                break;
        }
        switch (AnonymousClass2.$SwitchMap$android$net$IpConfiguration$ProxySettings[newConfig.getProxySettings().ordinal()]) {
            case 1:
            case 2:
                ProxyInfo newHttpProxy = newConfig.getHttpProxy();
                ProxyInfo currentHttpProxy = currentConfig.getHttpProxy();
                if (newHttpProxy != null) {
                    proxyChanged = !newHttpProxy.equals(currentHttpProxy);
                } else if (currentHttpProxy == null) {
                    proxyChanged = false;
                } else {
                    proxyChanged = true;
                }
                break;
            case 3:
                if (currentConfig.getProxySettings() != newConfig.getProxySettings()) {
                    proxyChanged = true;
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
        if (ipChanged || proxyChanged) {
            writeIpAndProxyConfigurations();
            sendConfiguredNetworksChangedBroadcast(currentConfig, 2);
        }
        return new NetworkUpdateResult(ipChanged, proxyChanged);
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$android$net$IpConfiguration$IpAssignment;
        static final int[] $SwitchMap$android$net$IpConfiguration$ProxySettings = new int[IpConfiguration.ProxySettings.values().length];
        static final int[] $SwitchMap$android$net$NetworkInfo$DetailedState;

        static {
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.PAC.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.NONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.UNASSIGNED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$android$net$IpConfiguration$IpAssignment = new int[IpConfiguration.IpAssignment.values().length];
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.DHCP.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.UNASSIGNED.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            $SwitchMap$android$net$NetworkInfo$DetailedState = new int[NetworkInfo.DetailedState.values().length];
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e9) {
            }
        }
    }

    private boolean enterpriseConfigKeyShouldBeQuoted(String key) {
        switch (key) {
            case "eap":
            case "engine":
                return false;
            default:
                return true;
        }
    }

    private void readNetworkVariables(WifiConfiguration config) {
        int netId = config.networkId;
        if (netId >= 0) {
            String value = this.mWifiNative.getNetworkVariable(netId, "ssid");
            if (!TextUtils.isEmpty(value)) {
                if (value.charAt(0) != '\"') {
                    config.SSID = "\"" + WifiSsid.createFromHex(value).toString() + "\"";
                } else {
                    config.SSID = value;
                }
            } else {
                config.SSID = null;
            }
            String value2 = this.mWifiNative.getNetworkVariable(netId, "bssid");
            if (!TextUtils.isEmpty(value2)) {
                config.BSSID = value2;
            } else {
                config.BSSID = null;
            }
            String value3 = this.mWifiNative.getNetworkVariable(netId, "priority");
            config.priority = -1;
            if (!TextUtils.isEmpty(value3)) {
                try {
                    config.priority = Integer.parseInt(value3);
                } catch (NumberFormatException e) {
                }
            }
            String value4 = this.mWifiNative.getNetworkVariable(netId, "scan_ssid");
            config.hiddenSSID = false;
            if (!TextUtils.isEmpty(value4)) {
                try {
                    config.hiddenSSID = Integer.parseInt(value4) != 0;
                } catch (NumberFormatException e2) {
                }
            }
            String value5 = this.mWifiNative.getNetworkVariable(netId, "wep_tx_keyidx");
            config.wepTxKeyIndex = -1;
            if (!TextUtils.isEmpty(value5)) {
                try {
                    config.wepTxKeyIndex = Integer.parseInt(value5);
                } catch (NumberFormatException e3) {
                }
            }
            for (int i = 0; i < 4; i++) {
                String value6 = this.mWifiNative.getNetworkVariable(netId, WifiConfiguration.wepKeyVarNames[i]);
                if (!TextUtils.isEmpty(value6)) {
                    config.wepKeys[i] = value6;
                } else {
                    config.wepKeys[i] = null;
                }
            }
            String value7 = this.mWifiNative.getNetworkVariable(netId, "psk");
            if (!TextUtils.isEmpty(value7)) {
                config.preSharedKey = value7;
            } else {
                config.preSharedKey = null;
            }
            String value8 = this.mWifiNative.getNetworkVariable(netId, "wapi_psk");
            if (!TextUtils.isEmpty(value8)) {
                config.wapiPsk = value8;
            } else {
                config.wapiPsk = null;
            }
            String value9 = this.mWifiNative.getNetworkVariable(netId, "wapi_psk_type");
            if (!TextUtils.isEmpty(value9)) {
                config.wapiPskType = value9;
            } else {
                config.wapiPskType = null;
            }
            String value10 = this.mWifiNative.getNetworkVariable(netId, "wapi_pkcs12_key");
            if (!TextUtils.isEmpty(value10)) {
                config.wapiPkcs12Key = value10;
            } else {
                config.wapiPkcs12Key = null;
            }
            String value11 = this.mWifiNative.getNetworkVariable(netId, "wapi_root_cert");
            if (!TextUtils.isEmpty(value11)) {
                config.wapiRootCert = value11;
            } else {
                config.wapiRootCert = null;
            }
            String value12 = this.mWifiNative.getNetworkVariable(netId, "wapi_user_cert");
            if (!TextUtils.isEmpty(value12)) {
                config.wapiUserCert = value12;
            } else {
                config.wapiUserCert = null;
            }
            String value13 = this.mWifiNative.getNetworkVariable(config.networkId, "proto");
            if (!TextUtils.isEmpty(value13)) {
                String[] vals = value13.split(" ");
                for (String val : vals) {
                    int index = lookupString(val, WifiConfiguration.Protocol.strings);
                    if (index >= 0) {
                        config.allowedProtocols.set(index);
                    }
                }
            }
            String value14 = this.mWifiNative.getNetworkVariable(config.networkId, "key_mgmt");
            if (!TextUtils.isEmpty(value14)) {
                String[] vals2 = value14.split(" ");
                for (String val2 : vals2) {
                    int index2 = lookupString(val2, WifiConfiguration.KeyMgmt.strings);
                    if (index2 >= 0) {
                        config.allowedKeyManagement.set(index2);
                    }
                }
            }
            String value15 = this.mWifiNative.getNetworkVariable(config.networkId, "auth_alg");
            if (!TextUtils.isEmpty(value15)) {
                String[] vals3 = value15.split(" ");
                for (String val3 : vals3) {
                    int index3 = lookupString(val3, WifiConfiguration.AuthAlgorithm.strings);
                    if (index3 >= 0) {
                        config.allowedAuthAlgorithms.set(index3);
                    }
                }
            }
            String value16 = this.mWifiNative.getNetworkVariable(config.networkId, "pairwise");
            if (!TextUtils.isEmpty(value16)) {
                String[] vals4 = value16.split(" ");
                for (String val4 : vals4) {
                    int index4 = lookupString(val4, WifiConfiguration.PairwiseCipher.strings);
                    if (index4 >= 0) {
                        config.allowedPairwiseCiphers.set(index4);
                    }
                }
            }
            String value17 = this.mWifiNative.getNetworkVariable(config.networkId, "group");
            if (!TextUtils.isEmpty(value17)) {
                String[] vals5 = value17.split(" ");
                for (String val5 : vals5) {
                    int index5 = lookupString(val5, WifiConfiguration.GroupCipher.strings);
                    if (index5 >= 0) {
                        config.allowedGroupCiphers.set(index5);
                    }
                }
            }
            if (config.enterpriseConfig == null) {
                config.enterpriseConfig = new WifiEnterpriseConfig();
            }
            HashMap<String, String> enterpriseFields = config.enterpriseConfig.getFields();
            String[] arr$ = ENTERPRISE_CONFIG_SUPPLICANT_KEYS;
            for (String key : arr$) {
                String value18 = this.mWifiNative.getNetworkVariable(netId, key);
                if (!TextUtils.isEmpty(value18)) {
                    if (!enterpriseConfigKeyShouldBeQuoted(key)) {
                        value18 = removeDoubleQuotes(value18);
                    }
                    enterpriseFields.put(key, value18);
                } else {
                    enterpriseFields.put(key, EMPTY_VALUE);
                }
            }
            if (migrateOldEapTlsNative(config.enterpriseConfig, netId)) {
                saveConfig();
            }
            migrateCerts(config.enterpriseConfig);
        }
    }

    private static String removeDoubleQuotes(String string) {
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private static String makeString(BitSet set, String[] strings) {
        StringBuffer buf = new StringBuffer();
        int nextSetBit = -1;
        BitSet set2 = set.get(0, strings.length);
        while (true) {
            nextSetBit = set2.nextSetBit(nextSetBit + 1);
            if (nextSetBit == -1) {
                break;
            }
            buf.append(strings[nextSetBit].replace('_', '-')).append(' ');
        }
        if (set2.cardinality() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    private int lookupString(String string, String[] strings) {
        int size = strings.length;
        String string2 = string.replace('-', '_');
        for (int i = 0; i < size; i++) {
            if (string2.equals(strings[i])) {
                return i;
            }
        }
        loge("Failed to look-up a string: " + string2);
        return -1;
    }

    public WifiConfiguration wifiConfigurationFromScanResult(ScanResult result) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + result.SSID + "\"";
        if (VDBG) {
            loge("WifiConfiguration from scan results " + config.SSID + " cap " + result.capabilities);
        }
        if (result.capabilities.contains("WEP")) {
            config.allowedKeyManagement.set(0);
            config.allowedAuthAlgorithms.set(0);
            config.allowedAuthAlgorithms.set(1);
        }
        if (result.capabilities.contains("PSK")) {
            config.allowedKeyManagement.set(1);
        }
        if (result.capabilities.contains("EAP")) {
            config.allowedKeyManagement.set(2);
            config.allowedKeyManagement.set(3);
        }
        config.scanResultCache = new HashMap();
        if (config.scanResultCache == null) {
            return null;
        }
        config.scanResultCache.put(result.BSSID, result);
        return config;
    }

    private static int configKey(WifiConfiguration config) {
        String key = config.configKey();
        return key.hashCode();
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiConfigStore");
        pw.println("mLastPriority " + this.mLastPriority);
        pw.println("Configured networks");
        for (WifiConfiguration conf : getConfiguredNetworks()) {
            pw.println(conf);
        }
        pw.println();
        if (this.mLocalLog != null) {
            pw.println("WifiConfigStore - Log Begin ----");
            this.mLocalLog.dump(fd, pw, args);
            pw.println("WifiConfigStore - Log End ----");
        }
    }

    public String getConfigFile() {
        return ipConfigFile;
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

    protected void log(String s) {
        Log.d(TAG, s);
    }

    private void localLog(String s) {
        if (this.mLocalLog != null) {
            this.mLocalLog.log(s);
        }
    }

    private void localLog(String s, boolean force) {
        localLog(s);
        if (force) {
            loge(s);
        }
    }

    private void localLog(String s, int netId) {
        WifiConfiguration config;
        if (this.mLocalLog != null) {
            synchronized (this.mConfiguredNetworks) {
                config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
            }
            if (config != null) {
                this.mLocalLog.log(s + " " + config.getPrintableSsid() + " " + netId + " status=" + config.status + " key=" + config.configKey());
            } else {
                this.mLocalLog.log(s + " " + netId);
            }
        }
    }

    static boolean needsKeyStore(WifiEnterpriseConfig config) {
        return (config.getClientCertificate() == null && config.getCaCertificate() == null) ? false : true;
    }

    static boolean isHardwareBackedKey(PrivateKey key) {
        return KeyChain.isBoundKeyAlgorithm(key.getAlgorithm());
    }

    static boolean hasHardwareBackedKey(Certificate certificate) {
        return KeyChain.isBoundKeyAlgorithm(certificate.getPublicKey().getAlgorithm());
    }

    static boolean needsSoftwareBackedKeyStore(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        return !TextUtils.isEmpty(client);
    }

    void handleBadNetworkDisconnectReport(int netId, WifiInfo info) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            if ((info.getRssi() < WifiConfiguration.UNWANTED_BLACKLIST_SOFT_RSSI_24 && info.is24GHz()) || (info.getRssi() < WifiConfiguration.UNWANTED_BLACKLIST_SOFT_RSSI_5 && info.is5GHz())) {
                config.setAutoJoinStatus(WifiConfiguration.UNWANTED_BLACKLIST_SOFT_BUMP + 1);
                loge("handleBadNetworkDisconnectReport (+4) " + Integer.toString(netId) + " " + info);
            } else {
                config.setAutoJoinStatus(WifiConfiguration.UNWANTED_BLACKLIST_HARD_BUMP + 1);
                loge("handleBadNetworkDisconnectReport (+8) " + Integer.toString(netId) + " " + info);
            }
        }
        this.lastUnwantedNetworkDisconnectTimestamp = System.currentTimeMillis();
    }

    boolean handleBSSIDBlackList(int netId, String BSSID, boolean enable) {
        boolean found = false;
        if (BSSID == null) {
            return false;
        }
        for (WifiConfiguration config : this.mConfiguredNetworks.values()) {
            if (config.scanResultCache != null) {
                for (ScanResult result : config.scanResultCache.values()) {
                    if (result.BSSID.equals(BSSID)) {
                        if (enable) {
                            result.setAutoJoinStatus(0);
                        } else {
                            result.setAutoJoinStatus(16);
                            found = true;
                        }
                    }
                }
            }
        }
        return found;
    }

    int getMaxDhcpRetries() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_max_dhcp_retry_count", 9);
    }

    void handleSSIDStateChange(int netId, boolean enabled, String message, String BSSID) {
        WifiConfiguration config = this.mConfiguredNetworks.get(Integer.valueOf(netId));
        if (config != null) {
            if (enabled) {
                loge("SSID re-enabled for  " + config.configKey() + " had autoJoinStatus=" + Integer.toString(config.autoJoinStatus) + " self added " + config.selfAdded + " ephemeral " + config.ephemeral);
                if (config.autoJoinStatus == 128) {
                    config.setAutoJoinStatus(0);
                    return;
                }
                return;
            }
            loge("SSID temp disabled for  " + config.configKey() + " had autoJoinStatus=" + Integer.toString(config.autoJoinStatus) + " self added " + config.selfAdded + " ephemeral " + config.ephemeral);
            if (message != null) {
                loge(" message=" + message);
            }
            if (config.selfAdded && config.lastConnected == 0) {
                removeConfigAndSendBroadcastIfNeeded(config.networkId);
                return;
            }
            if (message != null) {
                if (message.contains("no identity")) {
                    config.setAutoJoinStatus(160);
                    loge("no identity blacklisted " + config.configKey() + " to " + Integer.toString(config.autoJoinStatus));
                } else if (message.contains("WRONG_KEY") || message.contains("AUTH_FAILED")) {
                    config.numAuthFailures++;
                    if (config.numAuthFailures > this.maxAuthErrorsToBlacklist) {
                        config.setAutoJoinStatus(maxNumScanCacheEntries);
                        disableNetwork(netId, 3);
                        loge("Authentication failure, blacklist " + config.configKey() + " " + Integer.toString(config.networkId) + " num failures " + config.numAuthFailures);
                    }
                } else if (message.contains("DHCP FAILURE")) {
                    config.numIpConfigFailures++;
                    config.lastConnectionFailure = System.currentTimeMillis();
                    int maxRetries = getMaxDhcpRetries();
                    if (maxRetries > 0 && config.numIpConfigFailures > maxRetries) {
                        config.setAutoJoinStatus(maxNumScanCacheEntries);
                        disableNetwork(netId, 2);
                        loge("DHCP failure, blacklist " + config.configKey() + " " + Integer.toString(config.networkId) + " num failures " + config.numIpConfigFailures);
                    }
                    ScanResult result = null;
                    String bssidDbg = "";
                    if (config.scanResultCache != null && BSSID != null) {
                        result = (ScanResult) config.scanResultCache.get(BSSID);
                    }
                    if (result != null) {
                        result.numIpConfigFailures++;
                        bssidDbg = BSSID + " ipfail=" + result.numIpConfigFailures;
                        if (result.numIpConfigFailures > 3) {
                            this.mWifiNative.addToBlacklist(BSSID);
                            result.setAutoJoinStatus(32);
                        }
                    }
                    loge("blacklisted " + config.configKey() + " to " + config.autoJoinStatus + " due to IP config failures, count=" + config.numIpConfigFailures + " disableReason=" + config.disableReason + " " + bssidDbg);
                } else if (message.contains("CONN_FAILED")) {
                    config.numConnectionFailures++;
                    if (config.numConnectionFailures > this.maxConnectionErrorsToBlacklist) {
                        config.setAutoJoinStatus(maxNumScanCacheEntries);
                        disableNetwork(netId, 4);
                        loge("Connection failure, blacklist " + config.configKey() + " " + config.networkId + " num failures " + config.numConnectionFailures);
                    }
                }
                message.replace(SEPARATOR_KEY, "");
                message.replace("\r", "");
                config.lastFailure = message;
            }
        }
    }

    boolean installKeys(WifiEnterpriseConfig config, String name) {
        boolean ret;
        boolean ret2 = true;
        String privKeyName = "USRPKEY_" + name;
        String userCertName = "USRCERT_" + name;
        String caCertName = "CACERT_" + name;
        if (config.getClientCertificate() != null) {
            byte[] privKeyData = config.getClientPrivateKey().getEncoded();
            if (isHardwareBackedKey(config.getClientPrivateKey())) {
                Log.d(TAG, "importing keys " + name + " in hardware backed store");
                ret = this.mKeyStore.importKey(privKeyName, privKeyData, 1010, 0);
            } else {
                Log.d(TAG, "importing keys " + name + " in software backed store");
                ret = this.mKeyStore.importKey(privKeyName, privKeyData, 1010, 1);
            }
            if (!ret) {
                return ret;
            }
            ret2 = putCertInKeyStore(userCertName, config.getClientCertificate());
            if (!ret2) {
                this.mKeyStore.delKey(privKeyName, 1010);
                return ret2;
            }
        }
        if (config.getCaCertificate() != null && !(ret2 = putCertInKeyStore(caCertName, config.getCaCertificate()))) {
            if (config.getClientCertificate() != null) {
                this.mKeyStore.delKey(privKeyName, 1010);
                this.mKeyStore.delete(userCertName, 1010);
            }
            return ret2;
        }
        if (config.getClientCertificate() != null) {
            config.setClientCertificateAlias(name);
            config.resetClientKeyEntry();
        }
        if (config.getCaCertificate() != null) {
            config.setCaCertificateAlias(name);
            config.resetCaCertificate();
        }
        return ret2;
    }

    private boolean putCertInKeyStore(String name, Certificate cert) {
        try {
            byte[] certData = Credentials.convertToPem(new Certificate[]{cert});
            Log.d(TAG, "putting certificate " + name + " in keystore");
            return this.mKeyStore.put(name, certData, 1010, 0);
        } catch (IOException e) {
            return false;
        } catch (CertificateException e2) {
            return false;
        }
    }

    void removeKeys(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        if (!TextUtils.isEmpty(client)) {
            Log.d(TAG, "removing client private key and user cert");
            this.mKeyStore.delKey("USRPKEY_" + client, 1010);
            this.mKeyStore.delete("USRCERT_" + client, 1010);
        }
        String ca = config.getCaCertificateAlias();
        if (!TextUtils.isEmpty(ca)) {
            Log.d(TAG, "removing CA cert");
            this.mKeyStore.delete("CACERT_" + ca, 1010);
        }
    }

    boolean migrateOldEapTlsNative(WifiEnterpriseConfig config, int netId) {
        String keyName;
        String oldPrivateKey = this.mWifiNative.getNetworkVariable(netId, OLD_PRIVATE_KEY_NAME);
        if (TextUtils.isEmpty(oldPrivateKey)) {
            return false;
        }
        String oldPrivateKey2 = removeDoubleQuotes(oldPrivateKey);
        if (TextUtils.isEmpty(oldPrivateKey2)) {
            return false;
        }
        config.setFieldValue("engine", "1");
        config.setFieldValue("engine_id", "keystore");
        if (oldPrivateKey2.startsWith("keystore://")) {
            keyName = new String(oldPrivateKey2.substring("keystore://".length()));
        } else {
            keyName = oldPrivateKey2;
        }
        config.setFieldValue("key_id", keyName);
        this.mWifiNative.setNetworkVariable(netId, "engine", config.getFieldValue("engine", ""));
        this.mWifiNative.setNetworkVariable(netId, "engine_id", config.getFieldValue("engine_id", ""));
        this.mWifiNative.setNetworkVariable(netId, "key_id", config.getFieldValue("key_id", ""));
        this.mWifiNative.setNetworkVariable(netId, OLD_PRIVATE_KEY_NAME, EMPTY_VALUE);
        return true;
    }

    void migrateCerts(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        if (!TextUtils.isEmpty(client) && !this.mKeyStore.contains("USRPKEY_" + client, 1010)) {
            this.mKeyStore.duplicate("USRPKEY_" + client, -1, "USRPKEY_" + client, 1010);
            this.mKeyStore.duplicate("USRCERT_" + client, -1, "USRCERT_" + client, 1010);
        }
        String ca = config.getCaCertificateAlias();
        if (!TextUtils.isEmpty(ca) && !this.mKeyStore.contains("CACERT_" + ca, 1010)) {
            this.mKeyStore.duplicate("CACERT_" + ca, -1, "CACERT_" + ca, 1010);
        }
    }
}
