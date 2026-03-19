package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.hotspot2.LegacyPasspointConfig;
import com.android.server.wifi.hotspot2.LegacyPasspointConfigParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WifiConfigStoreLegacy {
    private static final String TAG = "WifiConfigStoreLegacy";
    private final IpConfigStoreWrapper mIpconfigStoreWrapper;
    private final LegacyPasspointConfigParser mPasspointConfigParser;
    private final WifiNative mWifiNative;
    private final WifiNetworkHistory mWifiNetworkHistory;
    private static final File NETWORK_HISTORY_FILE = new File(WifiNetworkHistory.NETWORK_HISTORY_CONFIG_FILE);
    private static final File PPS_FILE = new File(Environment.getDataMiscDirectory(), "wifi/PerProviderSubscription.conf");
    private static final File IP_CONFIG_FILE = new File(Environment.getDataMiscDirectory(), "wifi/ipconfig.txt");

    private interface MaskedWpaSupplicantFieldSetter {
        void setValue(WifiConfiguration wifiConfiguration, String str);
    }

    public static class IpConfigStoreWrapper {
        public SparseArray<IpConfiguration> readIpAndProxyConfigurations(String str) {
            return IpConfigStore.readIpAndProxyConfigurations(str);
        }
    }

    WifiConfigStoreLegacy(WifiNetworkHistory wifiNetworkHistory, WifiNative wifiNative, IpConfigStoreWrapper ipConfigStoreWrapper, LegacyPasspointConfigParser legacyPasspointConfigParser) {
        this.mWifiNetworkHistory = wifiNetworkHistory;
        this.mWifiNative = wifiNative;
        this.mIpconfigStoreWrapper = ipConfigStoreWrapper;
        this.mPasspointConfigParser = legacyPasspointConfigParser;
    }

    private static WifiConfiguration lookupWifiConfigurationUsingConfigKeyHash(Map<String, WifiConfiguration> map, int i) {
        for (Map.Entry<String, WifiConfiguration> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().hashCode() == i) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void loadFromIpConfigStore(Map<String, WifiConfiguration> map) {
        SparseArray<IpConfiguration> ipAndProxyConfigurations = this.mIpconfigStoreWrapper.readIpAndProxyConfigurations(IP_CONFIG_FILE.getAbsolutePath());
        if (ipAndProxyConfigurations == null || ipAndProxyConfigurations.size() == 0) {
            Log.w(TAG, "No ip configurations found in ipconfig store");
            return;
        }
        for (int i = 0; i < ipAndProxyConfigurations.size(); i++) {
            int iKeyAt = ipAndProxyConfigurations.keyAt(i);
            WifiConfiguration wifiConfigurationLookupWifiConfigurationUsingConfigKeyHash = lookupWifiConfigurationUsingConfigKeyHash(map, iKeyAt);
            if (wifiConfigurationLookupWifiConfigurationUsingConfigKeyHash == null || wifiConfigurationLookupWifiConfigurationUsingConfigKeyHash.ephemeral) {
                Log.w(TAG, "configuration found for missing network, nid=" + iKeyAt + ", ignored, networks.size=" + Integer.toString(ipAndProxyConfigurations.size()));
            } else {
                wifiConfigurationLookupWifiConfigurationUsingConfigKeyHash.setIpConfiguration(ipAndProxyConfigurations.valueAt(i));
            }
        }
    }

    private void loadFromNetworkHistory(Map<String, WifiConfiguration> map, Set<String> set) {
        this.mWifiNetworkHistory.readNetworkHistory(map, new HashMap(), set);
    }

    private void loadFromWpaSupplicant(Map<String, WifiConfiguration> map, SparseArray<Map<String, String>> sparseArray) {
        if (!this.mWifiNative.migrateNetworksFromSupplicant(this.mWifiNative.getClientInterfaceName(), map, sparseArray)) {
            Log.wtf(TAG, "Failed to load wifi configurations from wpa_supplicant");
        } else if (map.isEmpty()) {
            Log.w(TAG, "No wifi configurations found in wpa_supplicant");
        }
    }

    private void loadFromPasspointConfigStore(Map<String, WifiConfiguration> map, SparseArray<Map<String, String>> sparseArray) {
        Map<String, LegacyPasspointConfig> config;
        Map<String, String> map2;
        try {
            config = this.mPasspointConfigParser.parseConfig(PPS_FILE.getAbsolutePath());
        } catch (IOException e) {
            Log.w(TAG, "Failed to read/parse Passpoint config file: " + e.getMessage());
            config = null;
        }
        ArrayList<String> arrayList = new ArrayList();
        for (Map.Entry<String, WifiConfiguration> entry : map.entrySet()) {
            WifiConfiguration value = entry.getValue();
            if (value.enterpriseConfig != null && value.enterpriseConfig.getEapMethod() != -1 && (map2 = sparseArray.get(value.networkId)) != null && map2.containsKey(SupplicantStaNetworkHal.ID_STRING_KEY_FQDN)) {
                String str = sparseArray.get(value.networkId).get(SupplicantStaNetworkHal.ID_STRING_KEY_FQDN);
                if (config == null || !config.containsKey(str)) {
                    arrayList.add(entry.getKey());
                } else {
                    LegacyPasspointConfig legacyPasspointConfig = config.get(str);
                    value.isLegacyPasspointConfig = true;
                    value.FQDN = str;
                    value.providerFriendlyName = legacyPasspointConfig.mFriendlyName;
                    if (legacyPasspointConfig.mRoamingConsortiumOis != null) {
                        value.roamingConsortiumIds = Arrays.copyOf(legacyPasspointConfig.mRoamingConsortiumOis, legacyPasspointConfig.mRoamingConsortiumOis.length);
                    }
                    if (legacyPasspointConfig.mImsi != null) {
                        value.enterpriseConfig.setPlmn(legacyPasspointConfig.mImsi);
                    }
                    if (legacyPasspointConfig.mRealm != null) {
                        value.enterpriseConfig.setRealm(legacyPasspointConfig.mRealm);
                    }
                }
            }
        }
        for (String str2 : arrayList) {
            Log.w(TAG, "Remove incomplete Passpoint configuration: " + str2);
            map.remove(str2);
        }
    }

    public WifiConfigStoreDataLegacy read() {
        HashMap map = new HashMap();
        SparseArray<Map<String, String>> sparseArray = new SparseArray<>();
        HashSet hashSet = new HashSet();
        loadFromWpaSupplicant(map, sparseArray);
        loadFromNetworkHistory(map, hashSet);
        loadFromIpConfigStore(map);
        loadFromPasspointConfigStore(map, sparseArray);
        return new WifiConfigStoreDataLegacy(new ArrayList(map.values()), hashSet);
    }

    public boolean areStoresPresent() {
        return new File(WifiNetworkHistory.NETWORK_HISTORY_CONFIG_FILE).exists();
    }

    public boolean removeStores() {
        if (!this.mWifiNative.removeAllNetworks(this.mWifiNative.getClientInterfaceName())) {
            Log.e(TAG, "Removing networks from wpa_supplicant failed");
        }
        if (!IP_CONFIG_FILE.delete()) {
            Log.e(TAG, "Removing ipconfig.txt failed");
        }
        if (!NETWORK_HISTORY_FILE.delete()) {
            Log.e(TAG, "Removing networkHistory.txt failed");
        }
        if (!PPS_FILE.delete()) {
            Log.e(TAG, "Removing PerProviderSubscription.conf failed");
        }
        Log.i(TAG, "All legacy stores removed!");
        return true;
    }

    public static class WifiConfigStoreDataLegacy {
        private List<WifiConfiguration> mConfigurations;
        private Set<String> mDeletedEphemeralSSIDs;

        WifiConfigStoreDataLegacy(List<WifiConfiguration> list, Set<String> set) {
            this.mConfigurations = list;
            this.mDeletedEphemeralSSIDs = set;
        }

        public List<WifiConfiguration> getConfigurations() {
            return this.mConfigurations;
        }

        public Set<String> getDeletedEphemeralSSIDs() {
            return this.mDeletedEphemeralSSIDs;
        }
    }
}
