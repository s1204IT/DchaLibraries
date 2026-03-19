package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.IpConfiguration;
import android.net.MacAddress;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiConfigStoreLegacy;
import com.android.server.wifi.WifiConfigurationUtil;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.mediatek.server.wifi.MtkWapi;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

public class WifiConfigManager {

    @VisibleForTesting
    public static final int LINK_CONFIGURATION_BSSID_MATCH_LENGTH = 16;

    @VisibleForTesting
    public static final int LINK_CONFIGURATION_MAX_SCAN_CACHE_ENTRIES = 6;

    @VisibleForTesting
    public static final String PASSWORD_MASK = "*";

    @VisibleForTesting
    public static final int SCAN_CACHE_ENTRIES_MAX_SIZE = 192;

    @VisibleForTesting
    public static final int SCAN_CACHE_ENTRIES_TRIM_SIZE = 128;
    private static final int SCAN_RESULT_MAXIMUM_AGE_MS = 40000;

    @VisibleForTesting
    public static final String SYSUI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "WifiConfigManager";
    private final BackupManagerProxy mBackupManagerProxy;
    private final Clock mClock;
    private final ConfigurationMap mConfiguredNetworks;
    private final Context mContext;
    private int mCurrentUserId;
    private boolean mDeferredUserUnlockRead;
    private final Set<String> mDeletedEphemeralSSIDs;
    private final DeletedEphemeralSsidsStoreData mDeletedEphemeralSsidsStoreData;
    private int mLastSelectedNetworkId;
    private long mLastSelectedTimeStamp;
    private OnSavedNetworkUpdateListener mListener;
    private final LocalLog mLocalLog;
    private final int mMaxNumActiveChannelsForPartialScans;
    private final NetworkListStoreData mNetworkListStoreData;
    private int mNextNetworkId;
    private final boolean mOnlyLinkSameCredentialConfigurations;
    private boolean mPendingStoreRead;
    private boolean mPendingUnlockStoreRead;
    private final Map<Integer, ScanDetailCache> mScanDetailCaches;
    private boolean mSimPresent;
    private int mSystemUiUid;
    private final TelephonyManager mTelephonyManager;
    private final UserManager mUserManager;
    private boolean mVerboseLoggingEnabled;
    private final WifiConfigStore mWifiConfigStore;
    private final WifiConfigStoreLegacy mWifiConfigStoreLegacy;
    private final WifiKeyStore mWifiKeyStore;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;

    @VisibleForTesting
    public static final int[] NETWORK_SELECTION_DISABLE_THRESHOLD = {-1, 1, 5, 5, 5, 5, 1, 1, 6, 1, 1, 1, 1, 1};

    @VisibleForTesting
    public static final int[] NETWORK_SELECTION_DISABLE_TIMEOUT_MS = {ScoringParams.Values.MAX_EXPID, 900000, WifiConnectivityManager.BSSID_BLACKLIST_EXPIRE_TIME_MS, WifiConnectivityManager.BSSID_BLACKLIST_EXPIRE_TIME_MS, WifiConnectivityManager.BSSID_BLACKLIST_EXPIRE_TIME_MS, WifiConnectivityManager.BSSID_BLACKLIST_EXPIRE_TIME_MS, 600000, 0, ScoringParams.Values.MAX_EXPID, ScoringParams.Values.MAX_EXPID, ScoringParams.Values.MAX_EXPID, ScoringParams.Values.MAX_EXPID, ScoringParams.Values.MAX_EXPID, ScoringParams.Values.MAX_EXPID};
    private static final WifiConfigurationUtil.WifiConfigurationComparator sScanListComparator = new WifiConfigurationUtil.WifiConfigurationComparator() {
        @Override
        public int compareNetworksWithSameStatus(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
            if (wifiConfiguration.numAssociation != wifiConfiguration2.numAssociation) {
                return Long.compare(wifiConfiguration2.numAssociation, wifiConfiguration.numAssociation);
            }
            return Boolean.compare(wifiConfiguration2.getNetworkSelectionStatus().getSeenInLastQualifiedNetworkSelection(), wifiConfiguration.getNetworkSelectionStatus().getSeenInLastQualifiedNetworkSelection());
        }
    };

    public interface OnSavedNetworkUpdateListener {
        void onSavedNetworkAdded(int i);

        void onSavedNetworkEnabled(int i);

        void onSavedNetworkPermanentlyDisabled(int i, int i2);

        void onSavedNetworkRemoved(int i);

        void onSavedNetworkTemporarilyDisabled(int i, int i2);

        void onSavedNetworkUpdated(int i);
    }

    WifiConfigManager(Context context, Clock clock, UserManager userManager, TelephonyManager telephonyManager, WifiKeyStore wifiKeyStore, WifiConfigStore wifiConfigStore, WifiConfigStoreLegacy wifiConfigStoreLegacy, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper wifiPermissionsWrapper, NetworkListStoreData networkListStoreData, DeletedEphemeralSsidsStoreData deletedEphemeralSsidsStoreData) {
        this.mLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 128 : 256);
        this.mVerboseLoggingEnabled = false;
        this.mCurrentUserId = 0;
        this.mPendingUnlockStoreRead = true;
        this.mPendingStoreRead = true;
        this.mDeferredUserUnlockRead = false;
        this.mSimPresent = false;
        this.mNextNetworkId = 0;
        this.mSystemUiUid = -1;
        this.mLastSelectedNetworkId = -1;
        this.mLastSelectedTimeStamp = -1L;
        this.mListener = null;
        this.mContext = context;
        this.mClock = clock;
        this.mUserManager = userManager;
        this.mBackupManagerProxy = new BackupManagerProxy();
        this.mTelephonyManager = telephonyManager;
        this.mWifiKeyStore = wifiKeyStore;
        this.mWifiConfigStore = wifiConfigStore;
        this.mWifiConfigStoreLegacy = wifiConfigStoreLegacy;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mWifiPermissionsWrapper = wifiPermissionsWrapper;
        this.mConfiguredNetworks = new ConfigurationMap(userManager);
        this.mScanDetailCaches = new HashMap(16, 0.75f);
        this.mDeletedEphemeralSSIDs = new HashSet();
        this.mNetworkListStoreData = networkListStoreData;
        this.mDeletedEphemeralSsidsStoreData = deletedEphemeralSsidsStoreData;
        this.mWifiConfigStore.registerStoreData(this.mNetworkListStoreData);
        this.mWifiConfigStore.registerStoreData(this.mDeletedEphemeralSsidsStoreData);
        this.mOnlyLinkSameCredentialConfigurations = this.mContext.getResources().getBoolean(R.^attr-private.quickContactBadgeOverlay);
        this.mMaxNumActiveChannelsForPartialScans = this.mContext.getResources().getInteger(R.integer.config_letterboxBackgroundType);
        try {
            this.mSystemUiUid = this.mContext.getPackageManager().getPackageUidAsUser(SYSUI_PACKAGE_NAME, 1048576, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to resolve SystemUI's UID.");
        }
    }

    @VisibleForTesting
    public static String createDebugTimeStampString(long j) {
        StringBuilder sb = new StringBuilder();
        sb.append("time=");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar));
        return sb.toString();
    }

    public void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
        this.mWifiConfigStore.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiKeyStore.enableVerboseLogging(this.mVerboseLoggingEnabled);
    }

    private void maskPasswordsInWifiConfiguration(WifiConfiguration wifiConfiguration) {
        if (!TextUtils.isEmpty(wifiConfiguration.preSharedKey)) {
            wifiConfiguration.preSharedKey = "*";
        }
        if (wifiConfiguration.wepKeys != null) {
            for (int i = 0; i < wifiConfiguration.wepKeys.length; i++) {
                if (!TextUtils.isEmpty(wifiConfiguration.wepKeys[i])) {
                    wifiConfiguration.wepKeys[i] = "*";
                }
            }
        }
        if (!TextUtils.isEmpty(wifiConfiguration.enterpriseConfig.getPassword())) {
            wifiConfiguration.enterpriseConfig.setPassword("*");
        }
    }

    private void maskRandomizedMacAddressInWifiConfiguration(WifiConfiguration wifiConfiguration) {
        wifiConfiguration.setRandomizedMacAddress(MacAddress.fromString("02:00:00:00:00:00"));
    }

    private WifiConfiguration createExternalWifiConfiguration(WifiConfiguration wifiConfiguration, boolean z) {
        WifiConfiguration wifiConfiguration2 = new WifiConfiguration(wifiConfiguration);
        if (z) {
            maskPasswordsInWifiConfiguration(wifiConfiguration2);
        }
        maskRandomizedMacAddressInWifiConfiguration(wifiConfiguration2);
        return wifiConfiguration2;
    }

    private List<WifiConfiguration> getConfiguredNetworks(boolean z, boolean z2) {
        ArrayList arrayList = new ArrayList();
        for (WifiConfiguration wifiConfiguration : getInternalConfiguredNetworks()) {
            if (!z || !wifiConfiguration.ephemeral) {
                arrayList.add(createExternalWifiConfiguration(wifiConfiguration, z2));
            }
        }
        return arrayList;
    }

    public List<WifiConfiguration> getConfiguredNetworks() {
        return getConfiguredNetworks(false, true);
    }

    public List<WifiConfiguration> getConfiguredNetworksWithPasswords() {
        return getConfiguredNetworks(false, false);
    }

    public List<WifiConfiguration> getSavedNetworks() {
        return getConfiguredNetworks(true, true);
    }

    public WifiConfiguration getConfiguredNetwork(int i) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return null;
        }
        return createExternalWifiConfiguration(internalConfiguredNetwork, true);
    }

    public WifiConfiguration getConfiguredNetwork(String str) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(str);
        if (internalConfiguredNetwork == null) {
            return null;
        }
        return createExternalWifiConfiguration(internalConfiguredNetwork, true);
    }

    public WifiConfiguration getConfiguredNetworkWithPassword(int i) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return null;
        }
        return createExternalWifiConfiguration(internalConfiguredNetwork, false);
    }

    public WifiConfiguration getConfiguredNetworkWithoutMasking(int i) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return null;
        }
        return new WifiConfiguration(internalConfiguredNetwork);
    }

    private Collection<WifiConfiguration> getInternalConfiguredNetworks() {
        return this.mConfiguredNetworks.valuesForCurrentUser();
    }

    private WifiConfiguration getInternalConfiguredNetwork(WifiConfiguration wifiConfiguration) {
        WifiConfiguration forCurrentUser = this.mConfiguredNetworks.getForCurrentUser(wifiConfiguration.networkId);
        if (forCurrentUser != null) {
            return forCurrentUser;
        }
        WifiConfiguration byConfigKeyForCurrentUser = this.mConfiguredNetworks.getByConfigKeyForCurrentUser(wifiConfiguration.configKey());
        if (byConfigKeyForCurrentUser == null) {
            Log.e(TAG, "Cannot find network with networkId " + wifiConfiguration.networkId + " or configKey " + wifiConfiguration.configKey());
        }
        return byConfigKeyForCurrentUser;
    }

    private WifiConfiguration getInternalConfiguredNetwork(int i) {
        if (i == -1) {
            return null;
        }
        WifiConfiguration forCurrentUser = this.mConfiguredNetworks.getForCurrentUser(i);
        if (forCurrentUser == null) {
            Log.e(TAG, "Cannot find network with networkId " + i);
        }
        return forCurrentUser;
    }

    private WifiConfiguration getInternalConfiguredNetwork(String str) {
        WifiConfiguration byConfigKeyForCurrentUser = this.mConfiguredNetworks.getByConfigKeyForCurrentUser(str);
        if (byConfigKeyForCurrentUser == null) {
            Log.e(TAG, "Cannot find network with configKey " + str);
        }
        return byConfigKeyForCurrentUser;
    }

    private void sendConfiguredNetworkChangedBroadcast(WifiConfiguration wifiConfiguration, int i) {
        Intent intent = new Intent("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("multipleChanges", false);
        WifiConfiguration wifiConfiguration2 = new WifiConfiguration(wifiConfiguration);
        maskPasswordsInWifiConfiguration(wifiConfiguration2);
        intent.putExtra("wifiConfiguration", wifiConfiguration2);
        intent.putExtra("changeReason", i);
        this.mContext.sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, new String[]{"android.permission.ACCESS_WIFI_STATE", "android.permission.ACCESS_FINE_LOCATION"});
    }

    private void sendConfiguredNetworksChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("multipleChanges", true);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean canModifyNetwork(WifiConfiguration wifiConfiguration, int i) {
        if (i == 1000) {
            return true;
        }
        if (wifiConfiguration.isPasspoint() && i == 1010) {
            return true;
        }
        if (wifiConfiguration.enterpriseConfig != null && i == 1010 && TelephonyUtil.isSimEapMethod(wifiConfiguration.enterpriseConfig.getEapMethod())) {
            return true;
        }
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(i, -2)) {
            return true;
        }
        boolean z = wifiConfiguration.creatorUid == i;
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin") && devicePolicyManagerInternal == null) {
            Log.w(TAG, "Error retrieving DPMI service.");
            return false;
        }
        if (devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(wifiConfiguration.creatorUid, -2)) {
            return !(Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_device_owner_configs_lockdown", 0) != 0) && this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i);
        }
        return z || this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i);
    }

    private void mergeWithInternalWifiConfiguration(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (wifiConfiguration2.SSID != null) {
            wifiConfiguration.SSID = wifiConfiguration2.SSID;
        }
        if (wifiConfiguration2.BSSID != null) {
            wifiConfiguration.BSSID = wifiConfiguration2.BSSID.toLowerCase();
        }
        wifiConfiguration.hiddenSSID = wifiConfiguration2.hiddenSSID;
        if (wifiConfiguration2.preSharedKey != null && !wifiConfiguration2.preSharedKey.equals("*")) {
            wifiConfiguration.preSharedKey = wifiConfiguration2.preSharedKey;
        }
        if (wifiConfiguration2.wepKeys != null) {
            boolean z = false;
            for (int i = 0; i < wifiConfiguration.wepKeys.length; i++) {
                if (wifiConfiguration2.wepKeys[i] != null && !wifiConfiguration2.wepKeys[i].equals("*")) {
                    wifiConfiguration.wepKeys[i] = wifiConfiguration2.wepKeys[i];
                    z = true;
                }
            }
            if (z) {
                wifiConfiguration.wepTxKeyIndex = wifiConfiguration2.wepTxKeyIndex;
            }
        }
        if (wifiConfiguration2.FQDN != null) {
            wifiConfiguration.FQDN = wifiConfiguration2.FQDN;
        }
        if (wifiConfiguration2.providerFriendlyName != null) {
            wifiConfiguration.providerFriendlyName = wifiConfiguration2.providerFriendlyName;
        }
        if (wifiConfiguration2.roamingConsortiumIds != null) {
            wifiConfiguration.roamingConsortiumIds = (long[]) wifiConfiguration2.roamingConsortiumIds.clone();
        }
        if (wifiConfiguration2.allowedAuthAlgorithms != null && !wifiConfiguration2.allowedAuthAlgorithms.isEmpty()) {
            wifiConfiguration.allowedAuthAlgorithms = (BitSet) wifiConfiguration2.allowedAuthAlgorithms.clone();
        }
        if (wifiConfiguration2.allowedProtocols != null && !wifiConfiguration2.allowedProtocols.isEmpty()) {
            wifiConfiguration.allowedProtocols = (BitSet) wifiConfiguration2.allowedProtocols.clone();
        }
        if (wifiConfiguration2.allowedKeyManagement != null && !wifiConfiguration2.allowedKeyManagement.isEmpty()) {
            wifiConfiguration.allowedKeyManagement = (BitSet) wifiConfiguration2.allowedKeyManagement.clone();
            if (MtkWapi.isConfigForWapiNetwork(wifiConfiguration)) {
                wifiConfiguration.allowedProtocols.clear();
                wifiConfiguration.allowedProtocols.set(3);
            }
        }
        if (wifiConfiguration2.allowedPairwiseCiphers != null && !wifiConfiguration2.allowedPairwiseCiphers.isEmpty()) {
            wifiConfiguration.allowedPairwiseCiphers = (BitSet) wifiConfiguration2.allowedPairwiseCiphers.clone();
        }
        if (wifiConfiguration2.allowedGroupCiphers != null && !wifiConfiguration2.allowedGroupCiphers.isEmpty()) {
            wifiConfiguration.allowedGroupCiphers = (BitSet) wifiConfiguration2.allowedGroupCiphers.clone();
        }
        if (wifiConfiguration2.getIpConfiguration() != null) {
            IpConfiguration.IpAssignment ipAssignment = wifiConfiguration2.getIpAssignment();
            if (ipAssignment != IpConfiguration.IpAssignment.UNASSIGNED) {
                wifiConfiguration.setIpAssignment(ipAssignment);
                if (ipAssignment == IpConfiguration.IpAssignment.STATIC) {
                    wifiConfiguration.setStaticIpConfiguration(new StaticIpConfiguration(wifiConfiguration2.getStaticIpConfiguration()));
                }
            }
            IpConfiguration.ProxySettings proxySettings = wifiConfiguration2.getProxySettings();
            if (proxySettings != IpConfiguration.ProxySettings.UNASSIGNED) {
                wifiConfiguration.setProxySettings(proxySettings);
                if (proxySettings == IpConfiguration.ProxySettings.PAC || proxySettings == IpConfiguration.ProxySettings.STATIC) {
                    wifiConfiguration.setHttpProxy(new ProxyInfo(wifiConfiguration2.getHttpProxy()));
                }
            }
        }
        if (wifiConfiguration2.enterpriseConfig != null) {
            wifiConfiguration.enterpriseConfig.copyFromExternal(wifiConfiguration2.enterpriseConfig, "*");
        }
        wifiConfiguration.meteredHint = wifiConfiguration2.meteredHint;
        wifiConfiguration.meteredOverride = wifiConfiguration2.meteredOverride;
        wifiConfiguration.wapiCertSelMode = wifiConfiguration2.wapiCertSelMode;
        wifiConfiguration.wapiCertSel = wifiConfiguration2.wapiCertSel;
        wifiConfiguration.wapiPskType = wifiConfiguration2.wapiPskType;
        if (wifiConfiguration2.wapiPsk != null) {
            if (wifiConfiguration.wapiPskType == 0) {
                wifiConfiguration.wapiPsk = "\"" + wifiConfiguration2.wapiPsk + "\"";
                wifiConfiguration.preSharedKey = "\"" + wifiConfiguration2.wapiPsk + "\"";
                return;
            }
            wifiConfiguration.wapiPsk = wifiConfiguration2.wapiPsk;
            wifiConfiguration.preSharedKey = wifiConfiguration2.wapiPsk;
        }
    }

    private void setDefaultsInWifiConfiguration(WifiConfiguration wifiConfiguration) {
        wifiConfiguration.allowedAuthAlgorithms.set(0);
        wifiConfiguration.allowedProtocols.set(1);
        wifiConfiguration.allowedProtocols.set(0);
        wifiConfiguration.allowedKeyManagement.set(1);
        wifiConfiguration.allowedKeyManagement.set(2);
        wifiConfiguration.allowedPairwiseCiphers.set(2);
        wifiConfiguration.allowedPairwiseCiphers.set(1);
        wifiConfiguration.allowedGroupCiphers.set(3);
        wifiConfiguration.allowedGroupCiphers.set(2);
        wifiConfiguration.allowedGroupCiphers.set(0);
        wifiConfiguration.allowedGroupCiphers.set(1);
        wifiConfiguration.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
        wifiConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
        wifiConfiguration.status = 1;
        wifiConfiguration.getNetworkSelectionStatus().setNetworkSelectionStatus(2);
        wifiConfiguration.getNetworkSelectionStatus().setNetworkSelectionDisableReason(11);
    }

    private WifiConfiguration createNewInternalWifiConfigurationFromExternal(WifiConfiguration wifiConfiguration, int i) {
        WifiConfiguration wifiConfiguration2 = new WifiConfiguration();
        int i2 = this.mNextNetworkId;
        this.mNextNetworkId = i2 + 1;
        wifiConfiguration2.networkId = i2;
        setDefaultsInWifiConfiguration(wifiConfiguration2);
        mergeWithInternalWifiConfiguration(wifiConfiguration2, wifiConfiguration);
        wifiConfiguration2.requirePMF = wifiConfiguration.requirePMF;
        wifiConfiguration2.noInternetAccessExpected = wifiConfiguration.noInternetAccessExpected;
        wifiConfiguration2.ephemeral = wifiConfiguration.ephemeral;
        wifiConfiguration2.useExternalScores = wifiConfiguration.useExternalScores;
        wifiConfiguration2.shared = wifiConfiguration.shared;
        wifiConfiguration2.lastUpdateUid = i;
        wifiConfiguration2.creatorUid = i;
        String nameForUid = this.mContext.getPackageManager().getNameForUid(i);
        wifiConfiguration2.lastUpdateName = nameForUid;
        wifiConfiguration2.creatorName = nameForUid;
        String strCreateDebugTimeStampString = createDebugTimeStampString(this.mClock.getWallClockMillis());
        wifiConfiguration2.updateTime = strCreateDebugTimeStampString;
        wifiConfiguration2.creationTime = strCreateDebugTimeStampString;
        return wifiConfiguration2;
    }

    private WifiConfiguration updateExistingInternalWifiConfigurationFromExternal(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2, int i) {
        WifiConfiguration wifiConfiguration3 = new WifiConfiguration(wifiConfiguration);
        mergeWithInternalWifiConfiguration(wifiConfiguration3, wifiConfiguration2);
        wifiConfiguration3.lastUpdateUid = i;
        wifiConfiguration3.lastUpdateName = this.mContext.getPackageManager().getNameForUid(i);
        wifiConfiguration3.updateTime = createDebugTimeStampString(this.mClock.getWallClockMillis());
        return wifiConfiguration3;
    }

    private NetworkUpdateResult addOrUpdateNetworkInternal(WifiConfiguration wifiConfiguration, int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Adding/Updating network " + wifiConfiguration.getPrintableSsid());
        }
        WifiConfiguration wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal = null;
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(wifiConfiguration);
        boolean z = true;
        if (internalConfiguredNetwork == null) {
            if (!WifiConfigurationUtil.validate(wifiConfiguration, true)) {
                Log.e(TAG, "Cannot add network with invalid config");
                return new NetworkUpdateResult(-1);
            }
            wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal = createNewInternalWifiConfigurationFromExternal(wifiConfiguration, i);
            internalConfiguredNetwork = getInternalConfiguredNetwork(wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.configKey());
        }
        if (internalConfiguredNetwork != null) {
            if (!WifiConfigurationUtil.validate(wifiConfiguration, false)) {
                Log.e(TAG, "Cannot update network with invalid config");
                return new NetworkUpdateResult(-1);
            }
            if (!canModifyNetwork(internalConfiguredNetwork, i)) {
                Log.e(TAG, "UID " + i + " does not have permission to update configuration " + wifiConfiguration.configKey());
                return new NetworkUpdateResult(-1);
            }
            wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal = updateExistingInternalWifiConfigurationFromExternal(internalConfiguredNetwork, wifiConfiguration, i);
        }
        if (WifiConfigurationUtil.hasProxyChanged(internalConfiguredNetwork, wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal) && !canModifyProxySettings(i)) {
            Log.e(TAG, "UID " + i + " does not have permission to modify proxy Settings " + wifiConfiguration.configKey() + ". Must have NETWORK_SETTINGS, or be device or profile owner.");
            return new NetworkUpdateResult(-1);
        }
        if (wifiConfiguration.enterpriseConfig != null && wifiConfiguration.enterpriseConfig.getEapMethod() != -1 && !wifiConfiguration.isPasspoint() && !this.mWifiKeyStore.updateNetworkKeys(wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal, internalConfiguredNetwork)) {
            return new NetworkUpdateResult(-1);
        }
        boolean z2 = internalConfiguredNetwork == null;
        boolean z3 = z2 || WifiConfigurationUtil.hasIpChanged(internalConfiguredNetwork, wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal);
        boolean z4 = z2 || WifiConfigurationUtil.hasProxyChanged(internalConfiguredNetwork, wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal);
        if (!z2 && !WifiConfigurationUtil.hasCredentialChanged(internalConfiguredNetwork, wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal)) {
            z = false;
        }
        if (z) {
            wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.getNetworkSelectionStatus().setHasEverConnected(false);
        }
        try {
            this.mConfiguredNetworks.put(wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal);
            if (this.mDeletedEphemeralSSIDs.remove(wifiConfiguration.SSID) && this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Removed from ephemeral blacklist: " + wifiConfiguration.SSID);
            }
            this.mBackupManagerProxy.notifyDataChanged();
            NetworkUpdateResult networkUpdateResult = new NetworkUpdateResult(z3, z4, z);
            networkUpdateResult.setIsNewNetwork(z2);
            networkUpdateResult.setNetworkId(wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.networkId);
            localLog("addOrUpdateNetworkInternal: added/updated config. netId=" + wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.networkId + " configKey=" + wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.configKey() + " uid=" + Integer.toString(wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.creatorUid) + " name=" + wifiConfigurationUpdateExistingInternalWifiConfigurationFromExternal.creatorName);
            return networkUpdateResult;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to add network to config map", e);
            return new NetworkUpdateResult(-1);
        }
    }

    public NetworkUpdateResult addOrUpdateNetwork(WifiConfiguration wifiConfiguration, int i) {
        int i2;
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i)) {
            Log.e(TAG, "UID " + i + " not visible to the current user");
            return new NetworkUpdateResult(-1);
        }
        if (wifiConfiguration == null) {
            Log.e(TAG, "Cannot add/update network with null config");
            return new NetworkUpdateResult(-1);
        }
        if (this.mPendingStoreRead) {
            Log.e(TAG, "Cannot add/update network before store is read!");
            return new NetworkUpdateResult(-1);
        }
        NetworkUpdateResult networkUpdateResultAddOrUpdateNetworkInternal = addOrUpdateNetworkInternal(wifiConfiguration, i);
        if (!networkUpdateResultAddOrUpdateNetworkInternal.isSuccess()) {
            Log.e(TAG, "Failed to add/update network " + wifiConfiguration.getPrintableSsid());
            return networkUpdateResultAddOrUpdateNetworkInternal;
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(networkUpdateResultAddOrUpdateNetworkInternal.getNetworkId());
        if (networkUpdateResultAddOrUpdateNetworkInternal.isNewNetwork()) {
            i2 = 0;
        } else {
            i2 = 2;
        }
        sendConfiguredNetworkChangedBroadcast(internalConfiguredNetwork, i2);
        if (!wifiConfiguration.ephemeral && !wifiConfiguration.isPasspoint()) {
            saveToStore(true);
            if (this.mListener != null) {
                if (networkUpdateResultAddOrUpdateNetworkInternal.isNewNetwork()) {
                    this.mListener.onSavedNetworkAdded(internalConfiguredNetwork.networkId);
                } else {
                    this.mListener.onSavedNetworkUpdated(internalConfiguredNetwork.networkId);
                }
            }
        }
        return networkUpdateResultAddOrUpdateNetworkInternal;
    }

    private boolean removeNetworkInternal(WifiConfiguration wifiConfiguration, int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Removing network " + wifiConfiguration.getPrintableSsid());
        }
        if (!wifiConfiguration.isPasspoint() && wifiConfiguration.enterpriseConfig != null && wifiConfiguration.enterpriseConfig.getEapMethod() != -1) {
            this.mWifiKeyStore.removeKeys(wifiConfiguration.enterpriseConfig);
        }
        removeConnectChoiceFromAllNetworks(wifiConfiguration.configKey());
        this.mConfiguredNetworks.remove(wifiConfiguration.networkId);
        this.mScanDetailCaches.remove(Integer.valueOf(wifiConfiguration.networkId));
        this.mBackupManagerProxy.notifyDataChanged();
        localLog("removeNetworkInternal: removed config. netId=" + wifiConfiguration.networkId + " configKey=" + wifiConfiguration.configKey() + " uid=" + Integer.toString(i) + " name=" + this.mContext.getPackageManager().getNameForUid(i));
        return true;
    }

    public boolean removeNetwork(int i, int i2) {
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i2)) {
            Log.e(TAG, "UID " + i2 + " not visible to the current user");
            return false;
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        if (!canModifyNetwork(internalConfiguredNetwork, i2)) {
            Log.e(TAG, "UID " + i2 + " does not have permission to delete configuration " + internalConfiguredNetwork.configKey());
            return false;
        }
        if (!removeNetworkInternal(internalConfiguredNetwork, i2)) {
            Log.e(TAG, "Failed to remove network " + internalConfiguredNetwork.getPrintableSsid());
            return false;
        }
        if (i == this.mLastSelectedNetworkId) {
            clearLastSelectedNetwork();
        }
        sendConfiguredNetworkChangedBroadcast(internalConfiguredNetwork, 1);
        if (!internalConfiguredNetwork.ephemeral && !internalConfiguredNetwork.isPasspoint()) {
            saveToStore(true);
            if (this.mListener != null) {
                this.mListener.onSavedNetworkRemoved(i);
            }
        }
        return true;
    }

    public Set<Integer> removeNetworksForApp(ApplicationInfo applicationInfo) {
        if (applicationInfo == null || applicationInfo.packageName == null) {
            return Collections.emptySet();
        }
        Log.d(TAG, "Remove all networks for app " + applicationInfo);
        ArraySet arraySet = new ArraySet();
        for (WifiConfiguration wifiConfiguration : (WifiConfiguration[]) this.mConfiguredNetworks.valuesForAllUsers().toArray(new WifiConfiguration[0])) {
            if (applicationInfo.uid == wifiConfiguration.creatorUid && applicationInfo.packageName.equals(wifiConfiguration.creatorName)) {
                localLog("Removing network " + wifiConfiguration.SSID + ", application \"" + applicationInfo.packageName + "\" uninstalled from user " + UserHandle.getUserId(applicationInfo.uid));
                if (removeNetwork(wifiConfiguration.networkId, this.mSystemUiUid)) {
                    arraySet.add(Integer.valueOf(wifiConfiguration.networkId));
                }
            }
        }
        return arraySet;
    }

    Set<Integer> removeNetworksForUser(int i) {
        Log.d(TAG, "Remove all networks for user " + i);
        ArraySet arraySet = new ArraySet();
        for (WifiConfiguration wifiConfiguration : (WifiConfiguration[]) this.mConfiguredNetworks.valuesForAllUsers().toArray(new WifiConfiguration[0])) {
            if (i == UserHandle.getUserId(wifiConfiguration.creatorUid)) {
                localLog("Removing network " + wifiConfiguration.SSID + ", user " + i + " removed");
                if (removeNetwork(wifiConfiguration.networkId, this.mSystemUiUid)) {
                    arraySet.add(Integer.valueOf(wifiConfiguration.networkId));
                }
            }
        }
        return arraySet;
    }

    public boolean removeAllEphemeralOrPasspointConfiguredNetworks() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Removing all passpoint or ephemeral configured networks");
        }
        boolean z = false;
        for (WifiConfiguration wifiConfiguration : (WifiConfiguration[]) this.mConfiguredNetworks.valuesForAllUsers().toArray(new WifiConfiguration[0])) {
            if (wifiConfiguration.isPasspoint()) {
                Log.d(TAG, "Removing passpoint network config " + wifiConfiguration.configKey());
                removeNetwork(wifiConfiguration.networkId, this.mSystemUiUid);
            } else if (wifiConfiguration.ephemeral) {
                Log.d(TAG, "Removing ephemeral network config " + wifiConfiguration.configKey());
                removeNetwork(wifiConfiguration.networkId, this.mSystemUiUid);
            }
            z = true;
        }
        return z;
    }

    private void setNetworkSelectionEnabled(WifiConfiguration wifiConfiguration) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
        networkSelectionStatus.setNetworkSelectionStatus(0);
        networkSelectionStatus.setDisableTime(-1L);
        networkSelectionStatus.setNetworkSelectionDisableReason(0);
        networkSelectionStatus.clearDisableReasonCounter();
        if (this.mListener != null) {
            this.mListener.onSavedNetworkEnabled(wifiConfiguration.networkId);
        }
    }

    private void setNetworkSelectionTemporarilyDisabled(WifiConfiguration wifiConfiguration, int i) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
        networkSelectionStatus.setNetworkSelectionStatus(1);
        networkSelectionStatus.setDisableTime(this.mClock.getElapsedSinceBootMillis());
        networkSelectionStatus.setNetworkSelectionDisableReason(i);
        if (this.mListener != null) {
            this.mListener.onSavedNetworkTemporarilyDisabled(wifiConfiguration.networkId, i);
        }
    }

    private void setNetworkSelectionPermanentlyDisabled(WifiConfiguration wifiConfiguration, int i) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
        networkSelectionStatus.setNetworkSelectionStatus(2);
        networkSelectionStatus.setDisableTime(-1L);
        networkSelectionStatus.setNetworkSelectionDisableReason(i);
        if (this.mListener != null) {
            this.mListener.onSavedNetworkPermanentlyDisabled(wifiConfiguration.networkId, i);
        }
    }

    private void setNetworkStatus(WifiConfiguration wifiConfiguration, int i) {
        wifiConfiguration.status = i;
        sendConfiguredNetworkChangedBroadcast(wifiConfiguration, 2);
    }

    private boolean setNetworkSelectionStatus(WifiConfiguration wifiConfiguration, int i) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
        if (i < 0 || i >= 14) {
            Log.e(TAG, "Invalid Network disable reason " + i);
            return false;
        }
        if (i == 0) {
            setNetworkSelectionEnabled(wifiConfiguration);
            setNetworkStatus(wifiConfiguration, 2);
        } else if (i < 8) {
            setNetworkSelectionTemporarilyDisabled(wifiConfiguration, i);
        } else {
            setNetworkSelectionPermanentlyDisabled(wifiConfiguration, i);
            setNetworkStatus(wifiConfiguration, 1);
        }
        localLog("setNetworkSelectionStatus: configKey=" + wifiConfiguration.configKey() + " networkStatus=" + networkSelectionStatus.getNetworkStatusString() + " disableReason=" + networkSelectionStatus.getNetworkDisableReasonString() + " at=" + createDebugTimeStampString(this.mClock.getWallClockMillis()));
        saveToStore(false);
        return true;
    }

    private boolean updateNetworkSelectionStatus(WifiConfiguration wifiConfiguration, int i) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
        if (i != 0) {
            networkSelectionStatus.incrementDisableReasonCounter(i);
            int disableReasonCounter = networkSelectionStatus.getDisableReasonCounter(i);
            int i2 = NETWORK_SELECTION_DISABLE_THRESHOLD[i];
            if (disableReasonCounter < i2) {
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Disable counter for network " + wifiConfiguration.getPrintableSsid() + " for reason " + WifiConfiguration.NetworkSelectionStatus.getNetworkDisableReasonString(i) + " is " + networkSelectionStatus.getDisableReasonCounter(i) + " and threshold is " + i2);
                    return true;
                }
                return true;
            }
        }
        return setNetworkSelectionStatus(wifiConfiguration, i);
    }

    public boolean updateNetworkSelectionStatus(int i, int i2) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        return updateNetworkSelectionStatus(internalConfiguredNetwork, i2);
    }

    public boolean updateNetworkNotRecommended(int i, boolean z) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.getNetworkSelectionStatus().setNotRecommended(z);
        if (this.mVerboseLoggingEnabled) {
            localLog("updateNetworkRecommendation: configKey=" + internalConfiguredNetwork.configKey() + " notRecommended=" + z);
        }
        saveToStore(false);
        return true;
    }

    private boolean tryEnableNetwork(WifiConfiguration wifiConfiguration) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration.getNetworkSelectionStatus();
        if (networkSelectionStatus.isNetworkTemporaryDisabled()) {
            if (this.mClock.getElapsedSinceBootMillis() - networkSelectionStatus.getDisableTime() >= NETWORK_SELECTION_DISABLE_TIMEOUT_MS[networkSelectionStatus.getNetworkSelectionDisableReason()]) {
                return updateNetworkSelectionStatus(wifiConfiguration, 0);
            }
        } else if (networkSelectionStatus.isDisabledByReason(12)) {
            return updateNetworkSelectionStatus(wifiConfiguration, 0);
        }
        return false;
    }

    public boolean tryEnableNetwork(int i) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        return tryEnableNetwork(internalConfiguredNetwork);
    }

    public boolean enableNetwork(int i, boolean z, int i2) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Enabling network " + i + " (disableOthers " + z + ")");
        }
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i2)) {
            Log.e(TAG, "UID " + i2 + " not visible to the current user");
            return false;
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        if (!canModifyNetwork(internalConfiguredNetwork, i2)) {
            Log.e(TAG, "UID " + i2 + " does not have permission to update configuration " + internalConfiguredNetwork.configKey());
            return false;
        }
        if (!updateNetworkSelectionStatus(i, 0)) {
            return false;
        }
        if (z) {
            setLastSelectedNetwork(i);
        }
        saveToStore(true);
        return true;
    }

    public boolean disableNetwork(int i, int i2) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Disabling network " + i);
        }
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i2)) {
            Log.e(TAG, "UID " + i2 + " not visible to the current user");
            return false;
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        if (!canModifyNetwork(internalConfiguredNetwork, i2)) {
            Log.e(TAG, "UID " + i2 + " does not have permission to update configuration " + internalConfiguredNetwork.configKey());
            return false;
        }
        if (!updateNetworkSelectionStatus(i, 11)) {
            return false;
        }
        if (i == this.mLastSelectedNetworkId) {
            clearLastSelectedNetwork();
        }
        saveToStore(true);
        return true;
    }

    public boolean updateLastConnectUid(int i, int i2) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Update network last connect UID for " + i);
        }
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i2)) {
            Log.e(TAG, "UID " + i2 + " not visible to the current user");
            return false;
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.lastConnectUid = i2;
        return true;
    }

    public boolean updateNetworkAfterConnect(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Update network after connect for " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.lastConnected = this.mClock.getWallClockMillis();
        internalConfiguredNetwork.numAssociation++;
        internalConfiguredNetwork.getNetworkSelectionStatus().clearDisableReasonCounter();
        internalConfiguredNetwork.getNetworkSelectionStatus().setHasEverConnected(true);
        setNetworkStatus(internalConfiguredNetwork, 0);
        saveToStore(false);
        return true;
    }

    public boolean updateNetworkAfterDisconnect(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Update network after disconnect for " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.lastDisconnected = this.mClock.getWallClockMillis();
        if (internalConfiguredNetwork.status == 0) {
            setNetworkStatus(internalConfiguredNetwork, 2);
        }
        saveToStore(false);
        return true;
    }

    public boolean setNetworkDefaultGwMacAddress(int i, String str) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.defaultGwMacAddress = str;
        return true;
    }

    public boolean setNetworkRandomizedMacAddress(int i, MacAddress macAddress) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.setRandomizedMacAddress(macAddress);
        return true;
    }

    public boolean clearNetworkCandidateScanResult(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Clear network candidate scan result for " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.getNetworkSelectionStatus().setCandidate((ScanResult) null);
        internalConfiguredNetwork.getNetworkSelectionStatus().setCandidateScore(Integer.MIN_VALUE);
        internalConfiguredNetwork.getNetworkSelectionStatus().setSeenInLastQualifiedNetworkSelection(false);
        return true;
    }

    public boolean setNetworkCandidateScanResult(int i, ScanResult scanResult, int i2) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Set network candidate scan result " + scanResult + " for " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.getNetworkSelectionStatus().setCandidate(scanResult);
        internalConfiguredNetwork.getNetworkSelectionStatus().setCandidateScore(i2);
        internalConfiguredNetwork.getNetworkSelectionStatus().setSeenInLastQualifiedNetworkSelection(true);
        return true;
    }

    private void removeConnectChoiceFromAllNetworks(String str) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Removing connect choice from all networks " + str);
        }
        if (str == null) {
            return;
        }
        for (WifiConfiguration wifiConfiguration : this.mConfiguredNetworks.valuesForCurrentUser()) {
            String connectChoice = wifiConfiguration.getNetworkSelectionStatus().getConnectChoice();
            if (TextUtils.equals(connectChoice, str)) {
                Log.d(TAG, "remove connect choice:" + connectChoice + " from " + wifiConfiguration.SSID + " : " + wifiConfiguration.networkId);
                clearNetworkConnectChoice(wifiConfiguration.networkId);
            }
        }
    }

    public boolean clearNetworkConnectChoice(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Clear network connect choice for " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.getNetworkSelectionStatus().setConnectChoice((String) null);
        internalConfiguredNetwork.getNetworkSelectionStatus().setConnectChoiceTimestamp(-1L);
        saveToStore(false);
        return true;
    }

    public boolean setNetworkConnectChoice(int i, String str, long j) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Set network connect choice " + str + " for " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.getNetworkSelectionStatus().setConnectChoice(str);
        internalConfiguredNetwork.getNetworkSelectionStatus().setConnectChoiceTimestamp(j);
        saveToStore(false);
        return true;
    }

    public boolean incrementNetworkNoInternetAccessReports(int i) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.numNoInternetAccessReports++;
        return true;
    }

    public boolean setNetworkValidatedInternetAccess(int i, boolean z) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.validatedInternetAccess = z;
        internalConfiguredNetwork.numNoInternetAccessReports = 0;
        saveToStore(false);
        return true;
    }

    public boolean setNetworkNoInternetAccessExpected(int i, boolean z) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return false;
        }
        internalConfiguredNetwork.noInternetAccessExpected = z;
        return true;
    }

    private void clearLastSelectedNetwork() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Clearing last selected network");
        }
        this.mLastSelectedNetworkId = -1;
        this.mLastSelectedTimeStamp = -1L;
    }

    private void setLastSelectedNetwork(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Setting last selected network to " + i);
        }
        this.mLastSelectedNetworkId = i;
        this.mLastSelectedTimeStamp = this.mClock.getElapsedSinceBootMillis();
    }

    public int getLastSelectedNetwork() {
        return this.mLastSelectedNetworkId;
    }

    public String getLastSelectedNetworkConfigKey() {
        WifiConfiguration internalConfiguredNetwork;
        if (this.mLastSelectedNetworkId == -1 || (internalConfiguredNetwork = getInternalConfiguredNetwork(this.mLastSelectedNetworkId)) == null) {
            return "";
        }
        return internalConfiguredNetwork.configKey();
    }

    public long getLastSelectedTimeStamp() {
        return this.mLastSelectedTimeStamp;
    }

    public ScanDetailCache getScanDetailCacheForNetwork(int i) {
        return this.mScanDetailCaches.get(Integer.valueOf(i));
    }

    private ScanDetailCache getOrCreateScanDetailCacheForNetwork(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            return null;
        }
        ScanDetailCache scanDetailCacheForNetwork = getScanDetailCacheForNetwork(wifiConfiguration.networkId);
        if (scanDetailCacheForNetwork == null && wifiConfiguration.networkId != -1) {
            ScanDetailCache scanDetailCache = new ScanDetailCache(wifiConfiguration, SCAN_CACHE_ENTRIES_MAX_SIZE, 128);
            this.mScanDetailCaches.put(Integer.valueOf(wifiConfiguration.networkId), scanDetailCache);
            return scanDetailCache;
        }
        return scanDetailCacheForNetwork;
    }

    private void saveToScanDetailCacheForNetwork(WifiConfiguration wifiConfiguration, ScanDetail scanDetail) {
        ScanResult scanResult = scanDetail.getScanResult();
        ScanDetailCache orCreateScanDetailCacheForNetwork = getOrCreateScanDetailCacheForNetwork(wifiConfiguration);
        if (orCreateScanDetailCacheForNetwork == null) {
            Log.e(TAG, "Could not allocate scan cache for " + wifiConfiguration.getPrintableSsid());
            return;
        }
        if (wifiConfiguration.ephemeral) {
            scanResult.untrusted = true;
        }
        orCreateScanDetailCacheForNetwork.put(scanDetail);
        attemptNetworkLinking(wifiConfiguration);
    }

    public WifiConfiguration getConfiguredNetworkForScanDetail(ScanDetail scanDetail) {
        ScanResult scanResult = scanDetail.getScanResult();
        WifiConfiguration byScanResultForCurrentUser = null;
        if (scanResult == null) {
            Log.e(TAG, "No scan result found in scan detail");
            return null;
        }
        try {
            byScanResultForCurrentUser = this.mConfiguredNetworks.getByScanResultForCurrentUser(scanResult);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to lookup network from config map", e);
        }
        if (byScanResultForCurrentUser != null && this.mVerboseLoggingEnabled) {
            Log.v(TAG, "getSavedNetworkFromScanDetail Found " + byScanResultForCurrentUser.configKey() + " for " + scanResult.SSID + "[" + scanResult.capabilities + "]");
        }
        return byScanResultForCurrentUser;
    }

    public WifiConfiguration getConfiguredNetworkForScanDetailAndCache(ScanDetail scanDetail) {
        WifiConfiguration configuredNetworkForScanDetail = getConfiguredNetworkForScanDetail(scanDetail);
        if (configuredNetworkForScanDetail == null) {
            return null;
        }
        saveToScanDetailCacheForNetwork(configuredNetworkForScanDetail, scanDetail);
        if (scanDetail.getNetworkDetail() != null && scanDetail.getNetworkDetail().getDtimInterval() > 0) {
            configuredNetworkForScanDetail.dtimInterval = scanDetail.getNetworkDetail().getDtimInterval();
        }
        return createExternalWifiConfiguration(configuredNetworkForScanDetail, true);
    }

    public void updateScanDetailCacheFromWifiInfo(WifiInfo wifiInfo) {
        ScanDetail scanDetail;
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(wifiInfo.getNetworkId());
        ScanDetailCache scanDetailCacheForNetwork = getScanDetailCacheForNetwork(wifiInfo.getNetworkId());
        if (internalConfiguredNetwork != null && scanDetailCacheForNetwork != null && (scanDetail = scanDetailCacheForNetwork.getScanDetail(wifiInfo.getBSSID())) != null) {
            ScanResult scanResult = scanDetail.getScanResult();
            long j = scanResult.seen;
            int i = scanResult.level;
            scanDetail.setSeen();
            scanResult.level = wifiInfo.getRssi();
            long j2 = scanResult.seen - j;
            if (j > 0 && j2 > 0 && j2 < 20000) {
                double d = 0.5d - (j2 / 40000);
                scanResult.level = (int) ((((double) scanResult.level) * (1.0d - d)) + (((double) i) * d));
            }
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Updating scan detail cache freq=" + scanResult.frequency + " BSSID=" + scanResult.BSSID + " RSSI=" + scanResult.level + " for " + internalConfiguredNetwork.configKey());
            }
        }
    }

    public void updateScanDetailForNetwork(int i, ScanDetail scanDetail) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return;
        }
        saveToScanDetailCacheForNetwork(internalConfiguredNetwork, scanDetail);
    }

    private boolean shouldNetworksBeLinked(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2, ScanDetailCache scanDetailCache, ScanDetailCache scanDetailCache2) {
        if (this.mOnlyLinkSameCredentialConfigurations && !TextUtils.equals(wifiConfiguration.preSharedKey, wifiConfiguration2.preSharedKey)) {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "shouldNetworksBeLinked unlink due to password mismatch");
            }
            return false;
        }
        if (wifiConfiguration.defaultGwMacAddress != null && wifiConfiguration2.defaultGwMacAddress != null) {
            if (wifiConfiguration.defaultGwMacAddress.equals(wifiConfiguration2.defaultGwMacAddress)) {
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "shouldNetworksBeLinked link due to same gw " + wifiConfiguration2.SSID + " and " + wifiConfiguration.SSID + " GW " + wifiConfiguration.defaultGwMacAddress);
                }
                return true;
            }
        } else if (scanDetailCache != null && scanDetailCache2 != null) {
            for (String str : scanDetailCache.keySet()) {
                for (String str2 : scanDetailCache2.keySet()) {
                    if (str.regionMatches(true, 0, str2, 0, 16)) {
                        if (this.mVerboseLoggingEnabled) {
                            Log.v(TAG, "shouldNetworksBeLinked link due to DBDC BSSID match " + wifiConfiguration2.SSID + " and " + wifiConfiguration.SSID + " bssida " + str + " bssidb " + str2);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void linkNetworks(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "linkNetworks will link " + wifiConfiguration2.configKey() + " and " + wifiConfiguration.configKey());
        }
        if (wifiConfiguration2.linkedConfigurations == null) {
            wifiConfiguration2.linkedConfigurations = new HashMap();
        }
        if (wifiConfiguration.linkedConfigurations == null) {
            wifiConfiguration.linkedConfigurations = new HashMap();
        }
        wifiConfiguration2.linkedConfigurations.put(wifiConfiguration.configKey(), 1);
        wifiConfiguration.linkedConfigurations.put(wifiConfiguration2.configKey(), 1);
    }

    private void unlinkNetworks(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (wifiConfiguration2.linkedConfigurations != null && wifiConfiguration2.linkedConfigurations.get(wifiConfiguration.configKey()) != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "unlinkNetworks un-link " + wifiConfiguration.configKey() + " from " + wifiConfiguration2.configKey());
            }
            wifiConfiguration2.linkedConfigurations.remove(wifiConfiguration.configKey());
        }
        if (wifiConfiguration.linkedConfigurations != null && wifiConfiguration.linkedConfigurations.get(wifiConfiguration2.configKey()) != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "unlinkNetworks un-link " + wifiConfiguration2.configKey() + " from " + wifiConfiguration.configKey());
            }
            wifiConfiguration.linkedConfigurations.remove(wifiConfiguration2.configKey());
        }
    }

    private void attemptNetworkLinking(WifiConfiguration wifiConfiguration) {
        ScanDetailCache scanDetailCacheForNetwork;
        if (!wifiConfiguration.allowedKeyManagement.get(1)) {
            return;
        }
        ScanDetailCache scanDetailCacheForNetwork2 = getScanDetailCacheForNetwork(wifiConfiguration.networkId);
        if (scanDetailCacheForNetwork2 != null && scanDetailCacheForNetwork2.size() > 6) {
            return;
        }
        for (WifiConfiguration wifiConfiguration2 : getInternalConfiguredNetworks()) {
            if (!wifiConfiguration2.configKey().equals(wifiConfiguration.configKey()) && !wifiConfiguration2.ephemeral && wifiConfiguration2.allowedKeyManagement.get(1) && ((scanDetailCacheForNetwork = getScanDetailCacheForNetwork(wifiConfiguration2.networkId)) == null || scanDetailCacheForNetwork.size() <= 6)) {
                if (shouldNetworksBeLinked(wifiConfiguration, wifiConfiguration2, scanDetailCacheForNetwork2, scanDetailCacheForNetwork)) {
                    linkNetworks(wifiConfiguration, wifiConfiguration2);
                } else {
                    unlinkNetworks(wifiConfiguration, wifiConfiguration2);
                }
            }
        }
    }

    private boolean addToChannelSetForNetworkFromScanDetailCache(Set<Integer> set, ScanDetailCache scanDetailCache, long j, long j2, int i) {
        if (scanDetailCache != null && scanDetailCache.size() > 0) {
            Iterator<ScanDetail> it = scanDetailCache.values().iterator();
            while (it.hasNext()) {
                ScanResult scanResult = it.next().getScanResult();
                boolean z = j - scanResult.seen < j2;
                if (this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "fetchChannelSetForNetwork has " + scanResult.BSSID + " freq " + scanResult.frequency + " age " + (j - scanResult.seen) + " ?=" + z);
                }
                if (z) {
                    set.add(Integer.valueOf(scanResult.frequency));
                }
                if (set.size() >= i) {
                    return false;
                }
            }
        }
        return true;
    }

    public Set<Integer> fetchChannelSetForNetworkForPartialScan(int i, long j, int i2) {
        long j2;
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return null;
        }
        ScanDetailCache scanDetailCacheForNetwork = getScanDetailCacheForNetwork(i);
        if (scanDetailCacheForNetwork == null && internalConfiguredNetwork.linkedConfigurations == null) {
            Log.i(TAG, "No scan detail and linked configs associated with networkId " + i);
            return null;
        }
        if (this.mVerboseLoggingEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("fetchChannelSetForNetworkForPartialScan ageInMillis ");
            j2 = j;
            sb.append(j2);
            sb.append(" for ");
            sb.append(internalConfiguredNetwork.configKey());
            sb.append(" max ");
            sb.append(this.mMaxNumActiveChannelsForPartialScans);
            if (scanDetailCacheForNetwork != null) {
                sb.append(" bssids " + scanDetailCacheForNetwork.size());
            }
            if (internalConfiguredNetwork.linkedConfigurations != null) {
                sb.append(" linked " + internalConfiguredNetwork.linkedConfigurations.size());
            }
            Log.v(TAG, sb.toString());
        } else {
            j2 = j;
        }
        HashSet hashSet = new HashSet();
        if (i2 > 0) {
            hashSet.add(Integer.valueOf(i2));
            if (hashSet.size() >= this.mMaxNumActiveChannelsForPartialScans) {
                return hashSet;
            }
        }
        long wallClockMillis = this.mClock.getWallClockMillis();
        if (addToChannelSetForNetworkFromScanDetailCache(hashSet, scanDetailCacheForNetwork, wallClockMillis, j2, this.mMaxNumActiveChannelsForPartialScans) && internalConfiguredNetwork.linkedConfigurations != null) {
            Iterator it = internalConfiguredNetwork.linkedConfigurations.keySet().iterator();
            while (it.hasNext()) {
                WifiConfiguration internalConfiguredNetwork2 = getInternalConfiguredNetwork((String) it.next());
                if (internalConfiguredNetwork2 != null) {
                    if (!addToChannelSetForNetworkFromScanDetailCache(hashSet, getScanDetailCacheForNetwork(internalConfiguredNetwork2.networkId), wallClockMillis, j2, this.mMaxNumActiveChannelsForPartialScans)) {
                        break;
                    }
                }
            }
        }
        return hashSet;
    }

    public List<WifiScanner.PnoSettings.PnoNetwork> retrievePnoNetworkList() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList(getInternalConfiguredNetworks());
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            WifiConfiguration wifiConfiguration = (WifiConfiguration) it.next();
            if (wifiConfiguration.ephemeral || wifiConfiguration.isPasspoint() || wifiConfiguration.getNetworkSelectionStatus().isNetworkPermanentlyDisabled() || wifiConfiguration.getNetworkSelectionStatus().isNetworkTemporaryDisabled()) {
                it.remove();
            }
        }
        Collections.sort(arrayList2, sScanListComparator);
        Iterator it2 = arrayList2.iterator();
        while (it2.hasNext()) {
            arrayList.add(WifiConfigurationUtil.createPnoNetwork((WifiConfiguration) it2.next()));
        }
        return arrayList;
    }

    public List<WifiScanner.ScanSettings.HiddenNetwork> retrieveHiddenNetworkList() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList(getInternalConfiguredNetworks());
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            if (!((WifiConfiguration) it.next()).hiddenSSID) {
                it.remove();
            }
        }
        Collections.sort(arrayList2, sScanListComparator);
        Iterator it2 = arrayList2.iterator();
        while (it2.hasNext()) {
            arrayList.add(new WifiScanner.ScanSettings.HiddenNetwork(((WifiConfiguration) it2.next()).SSID));
        }
        return arrayList;
    }

    public boolean wasEphemeralNetworkDeleted(String str) {
        return this.mDeletedEphemeralSSIDs.contains(str);
    }

    public WifiConfiguration disableEphemeralNetwork(String str) {
        WifiConfiguration wifiConfiguration = null;
        if (str == null) {
            return null;
        }
        Iterator<WifiConfiguration> it = getInternalConfiguredNetworks().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            WifiConfiguration next = it.next();
            if (next.ephemeral && TextUtils.equals(next.SSID, str)) {
                wifiConfiguration = next;
                break;
            }
        }
        this.mDeletedEphemeralSSIDs.add(str);
        Log.d(TAG, "Forget ephemeral SSID " + str + " num=" + this.mDeletedEphemeralSSIDs.size());
        if (wifiConfiguration != null) {
            Log.d(TAG, "Found ephemeral config in disableEphemeralNetwork: " + wifiConfiguration.networkId);
        }
        return wifiConfiguration;
    }

    public void resetSimNetworks(boolean z) {
        if (this.mVerboseLoggingEnabled) {
            localLog("resetSimNetworks");
        }
        for (WifiConfiguration wifiConfiguration : getInternalConfiguredNetworks()) {
            if (TelephonyUtil.isSimConfig(wifiConfiguration)) {
                Pair<String, String> simIdentity = null;
                if (z) {
                    simIdentity = TelephonyUtil.getSimIdentity(this.mTelephonyManager, new TelephonyUtil(), wifiConfiguration);
                }
                if (simIdentity == null) {
                    Log.d(TAG, "Identity is null");
                    return;
                } else {
                    wifiConfiguration.enterpriseConfig.setIdentity((String) simIdentity.first);
                    if (wifiConfiguration.enterpriseConfig.getEapMethod() != 0) {
                        wifiConfiguration.enterpriseConfig.setAnonymousIdentity("");
                    }
                }
            }
        }
        this.mSimPresent = z;
    }

    public boolean isSimPresent() {
        return this.mSimPresent;
    }

    public boolean needsUnlockedKeyStore() {
        for (WifiConfiguration wifiConfiguration : getInternalConfiguredNetworks()) {
            if (WifiConfigurationUtil.isConfigForEapNetwork(wifiConfiguration)) {
                WifiKeyStore wifiKeyStore = this.mWifiKeyStore;
                if (WifiKeyStore.needsSoftwareBackedKeyStore(wifiConfiguration.enterpriseConfig)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleUserUnlockOrSwitch(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Loading from store after user switch/unlock for " + i);
        }
        if (loadFromUserStoreAfterUnlockOrSwitch(i)) {
            saveToStore(true);
            this.mPendingUnlockStoreRead = false;
        }
    }

    public Set<Integer> handleUserSwitch(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Handling user switch for " + i);
        }
        if (i == this.mCurrentUserId) {
            Log.w(TAG, "User already in foreground " + i);
            return new HashSet();
        }
        if (this.mPendingStoreRead) {
            Log.wtf(TAG, "Unexpected user switch before store is read!");
            return new HashSet();
        }
        if (this.mUserManager.isUserUnlockingOrUnlocked(this.mCurrentUserId)) {
            saveToStore(true);
        }
        Set<Integer> setClearInternalUserData = clearInternalUserData(this.mCurrentUserId);
        this.mConfiguredNetworks.setNewUser(i);
        this.mCurrentUserId = i;
        if (this.mUserManager.isUserUnlockingOrUnlocked(this.mCurrentUserId)) {
            handleUserUnlockOrSwitch(this.mCurrentUserId);
        } else {
            this.mPendingUnlockStoreRead = true;
            Log.i(TAG, "Waiting for user unlock to load from store");
        }
        return setClearInternalUserData;
    }

    public void handleUserUnlock(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Handling user unlock for " + i);
        }
        if (this.mPendingStoreRead) {
            Log.w(TAG, "Ignore user unlock until store is read!");
            this.mDeferredUserUnlockRead = true;
        } else if (i == this.mCurrentUserId && this.mPendingUnlockStoreRead) {
            handleUserUnlockOrSwitch(this.mCurrentUserId);
        }
    }

    public void handleUserStop(int i) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "Handling user stop for " + i);
        }
        if (i == this.mCurrentUserId && this.mUserManager.isUserUnlockingOrUnlocked(this.mCurrentUserId)) {
            saveToStore(true);
            clearInternalUserData(this.mCurrentUserId);
        }
    }

    private void clearInternalData() {
        localLog("clearInternalData: Clearing all internal data");
        this.mConfiguredNetworks.clear();
        this.mDeletedEphemeralSSIDs.clear();
        this.mScanDetailCaches.clear();
        clearLastSelectedNetwork();
    }

    private Set<Integer> clearInternalUserData(int i) {
        localLog("clearInternalUserData: Clearing user internal data for " + i);
        HashSet hashSet = new HashSet();
        for (WifiConfiguration wifiConfiguration : getInternalConfiguredNetworks()) {
            if (!wifiConfiguration.shared && !this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(wifiConfiguration.creatorUid)) {
                hashSet.add(Integer.valueOf(wifiConfiguration.networkId));
                localLog("clearInternalUserData: removed config. netId=" + wifiConfiguration.networkId + " configKey=" + wifiConfiguration.configKey());
                this.mConfiguredNetworks.remove(wifiConfiguration.networkId);
            }
        }
        this.mDeletedEphemeralSSIDs.clear();
        this.mScanDetailCaches.clear();
        clearLastSelectedNetwork();
        return hashSet;
    }

    private void loadInternalDataFromSharedStore(List<WifiConfiguration> list) {
        for (WifiConfiguration wifiConfiguration : list) {
            int i = this.mNextNetworkId;
            this.mNextNetworkId = i + 1;
            wifiConfiguration.networkId = i;
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Adding network from shared store " + wifiConfiguration.configKey());
            }
            try {
                this.mConfiguredNetworks.put(wifiConfiguration);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to add network to config map", e);
            }
        }
    }

    private void loadInternalDataFromUserStore(List<WifiConfiguration> list, Set<String> set) {
        for (WifiConfiguration wifiConfiguration : list) {
            int i = this.mNextNetworkId;
            this.mNextNetworkId = i + 1;
            wifiConfiguration.networkId = i;
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Adding network from user store " + wifiConfiguration.configKey());
            }
            try {
                this.mConfiguredNetworks.put(wifiConfiguration);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to add network to config map", e);
            }
        }
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            this.mDeletedEphemeralSSIDs.add(it.next());
        }
    }

    private void loadInternalData(List<WifiConfiguration> list, List<WifiConfiguration> list2, Set<String> set) {
        clearInternalData();
        loadInternalDataFromSharedStore(list);
        loadInternalDataFromUserStore(list2, set);
        if (this.mConfiguredNetworks.sizeForAllUsers() == 0) {
            Log.w(TAG, "No stored networks found.");
        }
        sendConfiguredNetworksChangedBroadcast();
        this.mPendingStoreRead = false;
    }

    public boolean migrateFromLegacyStore() {
        if (!this.mWifiConfigStoreLegacy.areStoresPresent()) {
            Log.d(TAG, "Legacy store files not found. No migration needed!");
            return true;
        }
        if (this.mWifiConfigStore.areStoresPresent()) {
            Log.d(TAG, "New store files found. No migration needed! Remove legacy store files");
            this.mWifiConfigStoreLegacy.removeStores();
            return true;
        }
        WifiConfigStoreLegacy.WifiConfigStoreDataLegacy wifiConfigStoreDataLegacy = this.mWifiConfigStoreLegacy.read();
        Log.d(TAG, "Reading from legacy store completed");
        loadInternalData(wifiConfigStoreDataLegacy.getConfigurations(), new ArrayList(), wifiConfigStoreDataLegacy.getDeletedEphemeralSSIDs());
        if (this.mDeferredUserUnlockRead) {
            this.mWifiConfigStore.setUserStore(WifiConfigStore.createUserFile(this.mCurrentUserId));
            this.mDeferredUserUnlockRead = false;
        }
        if (!saveToStore(true)) {
            return false;
        }
        this.mWifiConfigStoreLegacy.removeStores();
        Log.d(TAG, "Migration from legacy store completed");
        return true;
    }

    public boolean loadFromStore() {
        if (this.mDeferredUserUnlockRead) {
            Log.i(TAG, "Handling user unlock before loading from store.");
            this.mWifiConfigStore.setUserStore(WifiConfigStore.createUserFile(this.mCurrentUserId));
            this.mDeferredUserUnlockRead = false;
        }
        if (!this.mWifiConfigStore.areStoresPresent()) {
            Log.d(TAG, "New store files not found. No saved networks loaded!");
            if (!this.mWifiConfigStoreLegacy.areStoresPresent()) {
                this.mPendingStoreRead = false;
            }
            return true;
        }
        try {
            this.mWifiConfigStore.read();
            loadInternalData(this.mNetworkListStoreData.getSharedConfigurations(), this.mNetworkListStoreData.getUserConfigurations(), this.mDeletedEphemeralSsidsStoreData.getSsidList());
            return true;
        } catch (IOException e) {
            Log.wtf(TAG, "Reading from new store failed. All saved networks are lost!", e);
            return false;
        } catch (XmlPullParserException e2) {
            Log.wtf(TAG, "XML deserialization of store failed. All saved networks are lost!", e2);
            return false;
        }
    }

    public boolean loadFromUserStoreAfterUnlockOrSwitch(int i) {
        try {
            this.mWifiConfigStore.switchUserStoreAndRead(WifiConfigStore.createUserFile(i));
            loadInternalDataFromUserStore(this.mNetworkListStoreData.getUserConfigurations(), this.mDeletedEphemeralSsidsStoreData.getSsidList());
            return true;
        } catch (IOException e) {
            Log.wtf(TAG, "Reading from new store failed. All saved private networks are lost!", e);
            return false;
        } catch (XmlPullParserException e2) {
            Log.wtf(TAG, "XML deserialization of store failed. All saved private networks arelost!", e2);
            return false;
        }
    }

    public boolean saveToStore(boolean z) {
        if (this.mPendingStoreRead) {
            Log.e(TAG, "Cannot save to store before store is read!");
            return false;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (WifiConfiguration wifiConfiguration : this.mConfiguredNetworks.valuesForAllUsers()) {
            if (!wifiConfiguration.ephemeral && (!wifiConfiguration.isPasspoint() || wifiConfiguration.isLegacyPasspointConfig)) {
                if (wifiConfiguration.isLegacyPasspointConfig && !this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(wifiConfiguration.creatorUid)) {
                    arrayList3.add(Integer.valueOf(wifiConfiguration.networkId));
                    if (!PasspointManager.addLegacyPasspointConfig(wifiConfiguration)) {
                        Log.e(TAG, "Failed to migrate legacy Passpoint config: " + wifiConfiguration.FQDN);
                    }
                } else if (wifiConfiguration.shared || !this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(wifiConfiguration.creatorUid)) {
                    arrayList.add(wifiConfiguration);
                } else {
                    arrayList2.add(wifiConfiguration);
                }
            }
        }
        Iterator it = arrayList3.iterator();
        while (it.hasNext()) {
            this.mConfiguredNetworks.remove(((Integer) it.next()).intValue());
        }
        this.mNetworkListStoreData.setSharedConfigurations(arrayList);
        this.mNetworkListStoreData.setUserConfigurations(arrayList2);
        this.mDeletedEphemeralSsidsStoreData.setSsidList(this.mDeletedEphemeralSSIDs);
        try {
            this.mWifiConfigStore.write(z);
            return true;
        } catch (IOException e) {
            Log.wtf(TAG, "Writing to store failed. Saved networks maybe lost!", e);
            return false;
        } catch (XmlPullParserException e2) {
            Log.wtf(TAG, "XML serialization for store failed. Saved networks maybe lost!", e2);
            return false;
        }
    }

    private void localLog(String str) {
        if (this.mLocalLog != null) {
            this.mLocalLog.log(str);
        }
        Log.d(TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Dump of WifiConfigManager");
        printWriter.println("WifiConfigManager - Log Begin ----");
        this.mLocalLog.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("WifiConfigManager - Log End ----");
        printWriter.println("WifiConfigManager - Configured networks Begin ----");
        Iterator<WifiConfiguration> it = getInternalConfiguredNetworks().iterator();
        while (it.hasNext()) {
            printWriter.println(it.next());
        }
        printWriter.println("WifiConfigManager - Configured networks End ----");
        printWriter.println("WifiConfigManager - Next network ID to be allocated " + this.mNextNetworkId);
        printWriter.println("WifiConfigManager - Last selected network ID " + this.mLastSelectedNetworkId);
    }

    private boolean canModifyProxySettings(int i) {
        DevicePolicyManagerInternal devicePolicyManagerInternal = this.mWifiPermissionsWrapper.getDevicePolicyManagerInternal();
        boolean z = devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(i, -1);
        boolean z2 = devicePolicyManagerInternal != null && devicePolicyManagerInternal.isActiveAdminWithPolicy(i, -2);
        boolean zCheckNetworkSettingsPermission = this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i);
        if (z2 || z || zCheckNetworkSettingsPermission) {
            return true;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "UID: " + i + " cannot modify WifiConfiguration proxy settings. ConfigOverride=" + zCheckNetworkSettingsPermission + " DeviceOwner=" + z2 + " ProfileOwner=" + z);
        }
        return false;
    }

    public void setOnSavedNetworkUpdateListener(OnSavedNetworkUpdateListener onSavedNetworkUpdateListener) {
        this.mListener = onSavedNetworkUpdateListener;
    }

    public void setRecentFailureAssociationStatus(int i, int i2) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return;
        }
        internalConfiguredNetwork.recentFailure.setAssociationStatus(i2);
    }

    public void clearRecentFailureReason(int i) {
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null) {
            return;
        }
        internalConfiguredNetwork.recentFailure.clear();
    }
}
