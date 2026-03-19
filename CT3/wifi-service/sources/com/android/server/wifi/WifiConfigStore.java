package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiSsid;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.os.FileObserver;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.wifi.hotspot2.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiConfigStore {
    private static final boolean DBG = true;
    public static final String ID_STRING_KEY_CONFIG_KEY = "configKey";
    public static final String ID_STRING_KEY_CREATOR_UID = "creatorUid";
    public static final String ID_STRING_KEY_FQDN = "fqdn";
    public static final String ID_STRING_VAR_NAME = "id_str";
    public static final int STORED_VALUE_FOR_REQUIRE_PMF = 2;
    public static final String SUPPLICANT_CONFIG_FILE = "/data/misc/wifi/wpa_supplicant.conf";
    public static final String SUPPLICANT_CONFIG_FILE_BACKUP = "/data/misc/wifi/wpa_supplicant.conf.tmp";
    public static final String TAG = "WifiConfigStore";
    private static boolean VDBG = false;
    private final WpaConfigFileObserver mFileObserver;
    private final KeyStore mKeyStore;
    private final LocalLog mLocalLog;
    private final boolean mShowNetworks;
    private final WifiNative mWifiNative;
    private final HashSet<String> mBssidBlacklist = new HashSet<>();
    private final BackupManagerProxy mBackupManagerProxy = new BackupManagerProxy();

    WifiConfigStore(WifiNative wifiNative, KeyStore keyStore, LocalLog localLog, boolean showNetworks, boolean verboseDebug) {
        this.mWifiNative = wifiNative;
        this.mKeyStore = keyStore;
        this.mShowNetworks = showNetworks;
        if (this.mShowNetworks) {
            this.mLocalLog = localLog;
            this.mFileObserver = new WpaConfigFileObserver();
            this.mFileObserver.startWatching();
        } else {
            this.mLocalLog = null;
            this.mFileObserver = null;
        }
        VDBG = verboseDebug;
    }

    private static String removeDoubleQuotes(String string) {
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private static String makeString(BitSet set, String[] strings) {
        return makeStringWithException(set, strings, null);
    }

    private static String makeStringWithException(BitSet set, String[] strings, String exception) {
        new StringBuilder();
        BitSet trimmedSet = set.get(0, strings.length);
        List<String> valueSet = new ArrayList<>();
        for (int bit = trimmedSet.nextSetBit(0); bit >= 0; bit = trimmedSet.nextSetBit(bit + 1)) {
            String currentName = strings[bit];
            if (exception != null && currentName.equals(exception)) {
                valueSet.add(currentName);
            } else {
                valueSet.add(currentName.replace('_', '-'));
            }
        }
        return TextUtils.join(" ", valueSet);
    }

    private static String encodeSSID(String str) {
        return Utils.toHex(removeDoubleQuotes(str).getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeSSID(String str, boolean isGbkEncoding) {
        Log.d(TAG, "isGbkEncoding: " + isGbkEncoding);
        if (isGbkEncoding) {
            try {
                return Utils.toHex(removeDoubleQuotes(str).getBytes("GBK"));
            } catch (Exception e) {
                Log.d(TAG, "UnsupportedEncodingException: " + e.toString());
            }
        }
        return Utils.toHex(removeDoubleQuotes(str).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean needsKeyStore(WifiEnterpriseConfig config) {
        return (config.getClientCertificate() == null && config.getCaCertificate() == null) ? false : true;
    }

    private static boolean isHardwareBackedKey(PrivateKey key) {
        return KeyChain.isBoundKeyAlgorithm(key.getAlgorithm());
    }

    private static boolean hasHardwareBackedKey(Certificate certificate) {
        return KeyChain.isBoundKeyAlgorithm(certificate.getPublicKey().getAlgorithm());
    }

    private static boolean needsSoftwareBackedKeyStore(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        if (!TextUtils.isEmpty(client)) {
            return true;
        }
        return false;
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

    private void readNetworkBitsetVariable(int netId, BitSet variable, String varName, String[] strings) {
        String value = this.mWifiNative.getNetworkVariable(netId, varName);
        if (TextUtils.isEmpty(value)) {
            return;
        }
        variable.clear();
        String[] vals = value.split(" ");
        for (String val : vals) {
            int index = lookupString(val, strings);
            if (index >= 0) {
                variable.set(index);
            }
        }
    }

    public void readNetworkVariables(WifiConfiguration config) {
        if (config == null) {
            return;
        }
        if (VDBG) {
            localLog("readNetworkVariables: " + config.networkId);
        }
        int netId = config.networkId;
        if (netId < 0) {
            return;
        }
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
            config.getNetworkSelectionStatus().setNetworkSelectionBSSID(value2);
        } else {
            config.getNetworkSelectionStatus().setNetworkSelectionBSSID((String) null);
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
        String value5 = this.mWifiNative.getNetworkVariable(netId, "ieee80211w");
        config.requirePMF = false;
        if (!TextUtils.isEmpty(value5)) {
            try {
                config.requirePMF = Integer.parseInt(value5) == 2;
            } catch (NumberFormatException e3) {
            }
        }
        String value6 = this.mWifiNative.getNetworkVariable(netId, "wep_tx_keyidx");
        config.wepTxKeyIndex = -1;
        if (!TextUtils.isEmpty(value6)) {
            try {
                config.wepTxKeyIndex = Integer.parseInt(value6);
            } catch (NumberFormatException e4) {
            }
        }
        for (int i = 0; i < 4; i++) {
            String value7 = this.mWifiNative.getNetworkVariable(netId, WifiConfiguration.wepKeyVarNames[i]);
            if (!TextUtils.isEmpty(value7)) {
                config.wepKeys[i] = value7;
            } else {
                config.wepKeys[i] = null;
            }
        }
        String value8 = this.mWifiNative.getNetworkVariable(netId, "psk");
        if (!TextUtils.isEmpty(value8)) {
            config.preSharedKey = value8;
        } else {
            config.preSharedKey = null;
        }
        readNetworkBitsetVariable(config.networkId, config.allowedProtocols, "proto", WifiConfiguration.Protocol.strings);
        readNetworkBitsetVariable(config.networkId, config.allowedKeyManagement, "key_mgmt", WifiConfiguration.KeyMgmt.strings);
        readNetworkBitsetVariable(config.networkId, config.allowedAuthAlgorithms, "auth_alg", WifiConfiguration.AuthAlgorithm.strings);
        readNetworkBitsetVariable(config.networkId, config.allowedPairwiseCiphers, "pairwise", WifiConfiguration.PairwiseCipher.strings);
        readNetworkBitsetVariable(config.networkId, config.allowedGroupCiphers, "group", WifiConfiguration.GroupCipher.strings);
        if (config.enterpriseConfig == null) {
            config.enterpriseConfig = new WifiEnterpriseConfig();
        }
        config.enterpriseConfig.loadFromSupplicant(new SupplicantLoader(netId));
        String value9 = this.mWifiNative.getNetworkVariable(netId, "sim_num");
        if (!TextUtils.isEmpty(value9)) {
            config.simSlot = value9;
        } else {
            config.simSlot = null;
        }
        String value10 = this.mWifiNative.getNetworkVariable(netId, "pac_file");
        if (!TextUtils.isEmpty(value10)) {
            config.pacFile = value10;
        } else {
            config.pacFile = null;
        }
        String value11 = this.mWifiNative.getNetworkVariable(netId, "phase1");
        if (!TextUtils.isEmpty(value11)) {
            config.phase1 = value11;
        } else {
            config.phase1 = null;
        }
    }

    public int loadNetworks(Map<String, WifiConfiguration> configs, SparseArray<Map<String, String>> networkExtras) {
        int lastPriority = 0;
        int last_id = -1;
        boolean done = false;
        while (!done) {
            String listStr = this.mWifiNative.listNetworks(last_id);
            if (listStr == null) {
                return lastPriority;
            }
            String[] lines = listStr.split("\n");
            if (this.mShowNetworks) {
                localLog("loadNetworks:  ");
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
                    config.status = 1;
                    readNetworkVariables(config);
                    Map<String, String> extras = this.mWifiNative.getNetworkExtra(config.networkId, ID_STRING_VAR_NAME);
                    if (extras == null) {
                        extras = new HashMap<>();
                        String fqdn = Utils.unquote(this.mWifiNative.getNetworkVariable(config.networkId, ID_STRING_VAR_NAME));
                        if (fqdn != null) {
                            extras.put(ID_STRING_KEY_FQDN, fqdn);
                            config.FQDN = fqdn;
                            config.providerFriendlyName = "";
                        }
                    }
                    networkExtras.put(config.networkId, extras);
                    if (config.priority > lastPriority) {
                        lastPriority = config.priority;
                    }
                    config.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                    config.setProxySettings(IpConfiguration.ProxySettings.NONE);
                    if (WifiServiceImpl.isValid(config)) {
                        String configKey = extras.get(ID_STRING_KEY_CONFIG_KEY);
                        if (configKey == null) {
                            configKey = config.configKey();
                            saveNetworkMetadata(config);
                        }
                        WifiConfiguration duplicateConfig = configs.put(configKey, config);
                        if (duplicateConfig != null) {
                            if (this.mShowNetworks) {
                                localLog("Replacing duplicate network " + duplicateConfig.networkId + " with " + config.networkId + ".");
                            }
                            this.mWifiNative.removeNetwork(duplicateConfig.networkId);
                        }
                    } else if (this.mShowNetworks) {
                        localLog("Ignoring network " + config.networkId + " because configuration loaded from wpa_supplicant.conf is not valid.");
                    }
                } catch (NumberFormatException e) {
                    loge("Failed to read network-id '" + result[0] + "'");
                }
            }
            done = lines.length == 1;
        }
        return lastPriority;
    }

    private boolean installKeys(WifiEnterpriseConfig existingConfig, WifiEnterpriseConfig config, String name) {
        boolean ret;
        boolean ret2 = true;
        String privKeyName = "USRPKEY_" + name;
        String userCertName = "USRCERT_" + name;
        if (config.getClientCertificate() != null) {
            byte[] privKeyData = config.getClientPrivateKey().getEncoded();
            if (isHardwareBackedKey(config.getClientPrivateKey())) {
                Log.d(TAG, "importing keys " + name + " in hardware backed store");
            } else {
                Log.d(TAG, "importing keys " + name + " in software backed store");
            }
            boolean ret3 = this.mKeyStore.importKey(privKeyName, privKeyData, 1010, 0);
            if (!ret3) {
                return ret3;
            }
            ret2 = putCertInKeyStore(userCertName, config.getClientCertificate());
            if (!ret2) {
                this.mKeyStore.delete(privKeyName, 1010);
                return ret2;
            }
        }
        X509Certificate[] caCertificates = config.getCaCertificates();
        Set<String> oldCaCertificatesToRemove = new ArraySet<>();
        if (existingConfig != null && existingConfig.getCaCertificateAliases() != null) {
            oldCaCertificatesToRemove.addAll(Arrays.asList(existingConfig.getCaCertificateAliases()));
        }
        List<String> caCertificateAliases = null;
        if (caCertificates != null) {
            caCertificateAliases = new ArrayList<>();
            for (int i = 0; i < caCertificates.length; i++) {
                String alias = caCertificates.length == 1 ? name : String.format("%s_%d", name, Integer.valueOf(i));
                oldCaCertificatesToRemove.remove(alias);
                ret2 = putCertInKeyStore("CACERT_" + alias, caCertificates[i]);
                if (!ret2) {
                    if (config.getClientCertificate() != null) {
                        this.mKeyStore.delete(privKeyName, 1010);
                        this.mKeyStore.delete(userCertName, 1010);
                    }
                    for (String addedAlias : caCertificateAliases) {
                        this.mKeyStore.delete("CACERT_" + addedAlias, 1010);
                    }
                    return ret2;
                }
                caCertificateAliases.add(alias);
            }
        }
        for (String oldAlias : oldCaCertificatesToRemove) {
            this.mKeyStore.delete("CACERT_" + oldAlias, 1010);
        }
        if (config.getClientCertificate() != null) {
            config.setClientCertificateAlias(name);
            config.resetClientKeyEntry();
        }
        if (caCertificates != null) {
            config.setCaCertificateAliases((String[]) caCertificateAliases.toArray(new String[caCertificateAliases.size()]));
            config.resetCaCertificate();
        }
        String privKeyName2 = "keystore://WAPIUSERCERT_" + name;
        String userCertName2 = "WAPIUSERCERT_" + name;
        String caCertName2 = "WAPISERVERCERT_" + name;
        if (config.getClientCertificate() != null) {
            byte[] privKeyData2 = config.getClientPrivateKey().getEncoded();
            if (isHardwareBackedKey(config.getClientPrivateKey())) {
                Log.d(TAG, "importing keys " + name + " in hardware backed store");
                ret = this.mKeyStore.importKey(privKeyName2, privKeyData2, 1010, 0);
            } else {
                Log.d(TAG, "importing keys " + name + " in software backed store");
                ret = this.mKeyStore.importKey(privKeyName, privKeyData2, 1010, 1);
            }
            if (!ret) {
                return ret;
            }
            ret2 = putCertInKeyStore(userCertName2, config.getClientCertificate());
            if (!ret2) {
                return ret2;
            }
        }
        if (config.getCaCertificate() != null && !(ret2 = putCertInKeyStore(caCertName2, config.getCaCertificate()))) {
            if (config.getClientCertificate() != null) {
                this.mKeyStore.delete(userCertName2, 1010);
            }
            return ret2;
        }
        if (config.getClientCertificate() != null) {
            config.setClientCertificateWapiAlias(name);
            config.resetClientKeyEntry();
        }
        if (config.getCaCertificate() != null) {
            config.setCaCertificateWapiAlias(name);
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

    private void removeKeys(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        if (!TextUtils.isEmpty(client)) {
            Log.d(TAG, "removing client private key and user cert");
            this.mKeyStore.delete("USRPKEY_" + client, 1010);
            this.mKeyStore.delete("USRCERT_" + client, 1010);
        }
        String[] aliases = config.getCaCertificateAliases();
        if (aliases != null) {
            for (String ca : aliases) {
                if (!TextUtils.isEmpty(ca)) {
                    Log.d(TAG, "removing CA cert: " + ca);
                    this.mKeyStore.delete("CACERT_" + ca, 1010);
                }
            }
        }
        String client2 = config.getClientCertificateWapiAlias();
        if (!TextUtils.isEmpty(client2)) {
            this.mKeyStore.delete("WAPIUSERCERT_" + client2, 1010);
        }
        String ca2 = config.getCaCertificateWapiAlias();
        if (TextUtils.isEmpty(ca2)) {
            return;
        }
        this.mKeyStore.delete("WAPISERVERCERT_" + ca2, 1010);
    }

    public boolean saveNetworkMetadata(WifiConfiguration config) {
        Map<String, String> metadata = new HashMap<>();
        if (config.isPasspoint()) {
            metadata.put(ID_STRING_KEY_FQDN, config.FQDN);
        }
        metadata.put(ID_STRING_KEY_CONFIG_KEY, config.configKey());
        metadata.put(ID_STRING_KEY_CREATOR_UID, Integer.toString(config.creatorUid));
        if (!this.mWifiNative.setNetworkExtra(config.networkId, ID_STRING_VAR_NAME, metadata)) {
            loge("failed to set id_str: " + metadata.toString());
            return false;
        }
        return true;
    }

    private boolean saveNetwork(WifiConfiguration config, int netId) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("saveNetwork: " + netId);
        }
        if (config.SSID != null && !this.mWifiNative.setNetworkVariable(netId, "ssid", encodeSSID(config.SSID, config.isGbkEncoding))) {
            loge("failed to set SSID: " + config.SSID);
            return false;
        }
        if (!saveNetworkMetadata(config)) {
            return false;
        }
        if (config.getNetworkSelectionStatus().getNetworkSelectionBSSID() != null) {
            String bssid = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            if (!this.mWifiNative.setNetworkVariable(netId, "bssid", bssid)) {
                loge("failed to set BSSID: " + bssid);
                return false;
            }
        }
        String allowedKeyManagementString = makeString(config.allowedKeyManagement, WifiConfiguration.KeyMgmt.strings);
        if (config.allowedKeyManagement.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "key_mgmt", allowedKeyManagementString)) {
            loge("failed to set key_mgmt: " + allowedKeyManagementString);
            return false;
        }
        String allowedProtocolsString = makeString(config.allowedProtocols, WifiConfiguration.Protocol.strings);
        if (config.allowedProtocols.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "proto", allowedProtocolsString)) {
            loge("failed to set proto: " + allowedProtocolsString);
            return false;
        }
        String allowedAuthAlgorithmsString = makeString(config.allowedAuthAlgorithms, WifiConfiguration.AuthAlgorithm.strings);
        if (config.allowedAuthAlgorithms.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "auth_alg", allowedAuthAlgorithmsString)) {
            loge("failed to set auth_alg: " + allowedAuthAlgorithmsString);
            return false;
        }
        String allowedPairwiseCiphersString = makeString(config.allowedPairwiseCiphers, WifiConfiguration.PairwiseCipher.strings);
        if (config.allowedPairwiseCiphers.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "pairwise", allowedPairwiseCiphersString)) {
            loge("failed to set pairwise: " + allowedPairwiseCiphersString);
            return false;
        }
        String allowedGroupCiphersString = makeStringWithException(config.allowedGroupCiphers, WifiConfiguration.GroupCipher.strings, WifiConfiguration.GroupCipher.strings[4]);
        if (config.allowedGroupCiphers.cardinality() != 0 && !this.mWifiNative.setNetworkVariable(netId, "group", allowedGroupCiphersString)) {
            loge("failed to set group: " + allowedGroupCiphersString);
            return false;
        }
        if (config.preSharedKey != null && !config.preSharedKey.equals("*") && !this.mWifiNative.setNetworkVariable(netId, "psk", config.preSharedKey)) {
            loge("failed to set psk");
            return false;
        }
        boolean hasSetKey = false;
        if (config.wepKeys != null) {
            for (int i = 0; i < config.wepKeys.length; i++) {
                if (config.wepKeys[i] != null && !config.wepKeys[i].equals("*")) {
                    if (!this.mWifiNative.setNetworkVariable(netId, WifiConfiguration.wepKeyVarNames[i], config.wepKeys[i])) {
                        loge("failed to set wep_key" + i + ": " + config.wepKeys[i]);
                        return false;
                    }
                    hasSetKey = true;
                }
            }
        }
        if (hasSetKey && !this.mWifiNative.setNetworkVariable(netId, "wep_tx_keyidx", Integer.toString(config.wepTxKeyIndex))) {
            loge("failed to set wep_tx_keyidx: " + config.wepTxKeyIndex);
            return false;
        }
        if (!this.mWifiNative.setNetworkVariable(netId, "priority", Integer.toString(config.priority))) {
            loge(config.SSID + ": failed to set priority: " + config.priority);
            return false;
        }
        if (config.hiddenSSID) {
            if (!this.mWifiNative.setNetworkVariable(netId, "scan_ssid", Integer.toString(config.hiddenSSID ? 1 : 0))) {
                loge(config.SSID + ": failed to set hiddenSSID: " + config.hiddenSSID);
                return false;
            }
        }
        if (config.requirePMF && !this.mWifiNative.setNetworkVariable(netId, "ieee80211w", Integer.toString(2))) {
            loge(config.SSID + ": failed to set requirePMF: " + config.requirePMF);
            return false;
        }
        if (config.updateIdentifier != null && !this.mWifiNative.setNetworkVariable(netId, "update_identifier", config.updateIdentifier)) {
            loge(config.SSID + ": failed to set updateIdentifier: " + config.updateIdentifier);
            return false;
        }
        return true;
    }

    private boolean updateNetworkKeys(WifiConfiguration config, WifiConfiguration existingConfig) {
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        if (needsKeyStore(enterpriseConfig)) {
            try {
                String keyId = config.getKeyIdForCredentials(existingConfig);
                if (!installKeys(existingConfig != null ? existingConfig.enterpriseConfig : null, enterpriseConfig, keyId)) {
                    loge(config.SSID + ": failed to install keys");
                    return false;
                }
            } catch (IllegalStateException e) {
                loge(config.SSID + " invalid config for key installation: " + e.getMessage());
                return false;
            }
        }
        if (config.simSlot != null && !this.mWifiNative.setNetworkVariable(config.networkId, "sim_num", removeDoubleQuotes(config.simSlot))) {
            Log.e(TAG, "failed to set simSlot: " + removeDoubleQuotes(config.simSlot));
            return false;
        }
        if (config.pacFile != null && !this.mWifiNative.setNetworkVariable(config.networkId, "pac_file", config.pacFile)) {
            Log.e(TAG, "failed to set pacFile: " + config.pacFile);
            return false;
        }
        if (config.phase1 != null && !this.mWifiNative.setNetworkVariable(config.networkId, "phase1", config.phase1)) {
            Log.e(TAG, "failed to set phase1: " + config.phase1);
            return false;
        }
        if (!enterpriseConfig.saveToSupplicant(new SupplicantSaver(config.networkId, config.SSID), config)) {
            removeKeys(enterpriseConfig);
            return false;
        }
        return true;
    }

    public boolean addOrUpdateNetwork(WifiConfiguration config, WifiConfiguration existingConfig) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("addOrUpdateNetwork: " + config.networkId);
        }
        int netId = config.networkId;
        boolean newNetwork = false;
        if (netId == -1) {
            newNetwork = true;
            netId = this.mWifiNative.addNetwork();
            if (netId < 0) {
                loge("Failed to add a network!");
                return false;
            }
            logi("addOrUpdateNetwork created netId=" + netId);
            config.networkId = netId;
        }
        if (!saveNetwork(config, netId)) {
            if (newNetwork) {
                this.mWifiNative.removeNetwork(netId);
                loge("Failed to set a network variable, removed network: " + netId);
            }
            return false;
        }
        if ((config.enterpriseConfig != null && config.enterpriseConfig.getEapMethod() != -1) || config.isWapi()) {
            return updateNetworkKeys(config, existingConfig);
        }
        this.mBackupManagerProxy.notifyDataChanged();
        return true;
    }

    public boolean removeNetwork(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("removeNetwork: " + config.networkId);
        }
        if (!this.mWifiNative.removeNetwork(config.networkId)) {
            loge("Remove network in wpa_supplicant failed on " + config.networkId);
            return false;
        }
        if (config.enterpriseConfig != null) {
            removeKeys(config.enterpriseConfig);
        }
        this.mBackupManagerProxy.notifyDataChanged();
        return true;
    }

    public boolean selectNetwork(WifiConfiguration config, Collection<WifiConfiguration> configs) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("selectNetwork: " + config.networkId);
        }
        if (!this.mWifiNative.selectNetwork(config.networkId)) {
            loge("Select network in wpa_supplicant failed on " + config.networkId);
            return false;
        }
        config.status = 2;
        markAllNetworksDisabledExcept(config.networkId, configs);
        return true;
    }

    boolean disableNetwork(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("disableNetwork: " + config.networkId);
        }
        if (!this.mWifiNative.disableNetwork(config.networkId)) {
            loge("Disable network in wpa_supplicant failed on " + config.networkId);
            return false;
        }
        config.status = 1;
        return true;
    }

    public boolean setNetworkPriority(WifiConfiguration config, int priority) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("setNetworkPriority: " + config.networkId);
        }
        if (!this.mWifiNative.setNetworkVariable(config.networkId, "priority", Integer.toString(priority))) {
            loge("Set priority of network in wpa_supplicant failed on " + config.networkId);
            return false;
        }
        config.priority = priority;
        return true;
    }

    public boolean setNetworkSSID(WifiConfiguration config, String ssid) {
        if (config == null) {
            return false;
        }
        if (VDBG) {
            localLog("setNetworkSSID: " + config.networkId);
        }
        if (!this.mWifiNative.setNetworkVariable(config.networkId, "ssid", encodeSSID(ssid))) {
            loge("Set SSID of network in wpa_supplicant failed on " + config.networkId);
            return false;
        }
        config.SSID = ssid;
        return true;
    }

    public boolean setNetworkBSSID(WifiConfiguration config, String bssid) {
        if (config == null || (config.networkId == -1 && config.SSID == null)) {
            return false;
        }
        if (VDBG) {
            localLog("setNetworkBSSID: " + config.networkId);
        }
        if (!this.mWifiNative.setNetworkVariable(config.networkId, "bssid", bssid)) {
            loge("Set BSSID of network in wpa_supplicant failed on " + config.networkId);
            return false;
        }
        config.getNetworkSelectionStatus().setNetworkSelectionBSSID(bssid);
        return true;
    }

    public void enableHS20(boolean enable) {
        this.mWifiNative.setHs20(enable);
    }

    public boolean disableAllNetworks(Collection<WifiConfiguration> configs) {
        if (VDBG) {
            localLog("disableAllNetworks");
        }
        boolean networkDisabled = false;
        for (WifiConfiguration enabled : configs) {
            if (disableNetwork(enabled)) {
                networkDisabled = true;
            }
        }
        saveConfig();
        return networkDisabled;
    }

    public boolean saveConfig() {
        return this.mWifiNative.saveConfig();
    }

    public Map<String, String> readNetworkVariablesFromSupplicantFile(String key) throws Throwable {
        BufferedReader reader;
        Map<String, String> result = new HashMap<>();
        BufferedReader reader2 = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(SUPPLICANT_CONFIG_FILE));
            } catch (Throwable th) {
                th = th;
            }
            try {
                result = readNetworkVariablesFromReader(reader, key);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        if (VDBG) {
                            loge("Could not close reader for /data/misc/wifi/wpa_supplicant.conf, " + e);
                        }
                    }
                }
            } catch (FileNotFoundException e2) {
                e = e2;
                reader2 = reader;
                if (VDBG) {
                    loge("Could not open /data/misc/wifi/wpa_supplicant.conf, " + e);
                }
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e3) {
                        if (VDBG) {
                            loge("Could not close reader for /data/misc/wifi/wpa_supplicant.conf, " + e3);
                        }
                    }
                }
            } catch (IOException e4) {
                e = e4;
                reader2 = reader;
                if (VDBG) {
                    loge("Could not read /data/misc/wifi/wpa_supplicant.conf, " + e);
                }
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e5) {
                        if (VDBG) {
                            loge("Could not close reader for /data/misc/wifi/wpa_supplicant.conf, " + e5);
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e6) {
                        if (VDBG) {
                            loge("Could not close reader for /data/misc/wifi/wpa_supplicant.conf, " + e6);
                        }
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
            e = e7;
        } catch (IOException e8) {
            e = e8;
        }
        return result;
    }

    public Map<String, String> readNetworkVariablesFromReader(BufferedReader reader, String key) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (VDBG) {
            localLog("readNetworkVariablesFromReader key=" + key);
        }
        boolean found = false;
        String configKey = null;
        String value = null;
        String line = reader.readLine();
        while (line != null) {
            if (line.matches("[ \\t]*network=\\{")) {
                found = true;
                configKey = null;
                value = null;
            } else if (line.matches("[ \\t]*\\}")) {
                found = false;
                configKey = null;
                value = null;
            }
            if (found) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("id_str=")) {
                    try {
                        String encodedExtras = trimmedLine.substring(8, trimmedLine.length() - 1);
                        JSONObject json = new JSONObject(URLDecoder.decode(encodedExtras, "UTF-8"));
                        if (json.has(ID_STRING_KEY_CONFIG_KEY)) {
                            Object configKeyFromJson = json.get(ID_STRING_KEY_CONFIG_KEY);
                            if (configKeyFromJson instanceof String) {
                                configKey = (String) configKeyFromJson;
                            }
                        }
                    } catch (JSONException e) {
                        if (VDBG) {
                            loge("Could not get configKey, " + e);
                        }
                    }
                }
                if (trimmedLine.startsWith(key + "=")) {
                    value = trimmedLine.substring(key.length() + 1);
                }
                if (configKey != null && value != null) {
                    result.put(configKey, value);
                }
            }
            line = reader.readLine();
        }
        return result;
    }

    public boolean isSimConfig(WifiConfiguration config) {
        if (config == null || config.enterpriseConfig == null) {
            return false;
        }
        int method = config.enterpriseConfig.getEapMethod();
        return method == 4 || method == 5 || method == 6;
    }

    public void resetSimNetworks(Collection<WifiConfiguration> configs, int simSlot) {
        if (VDBG) {
            localLog("resetSimNetworks, simSlot: " + simSlot);
        }
        for (WifiConfiguration config : configs) {
            if (isSimConfig(config) && WifiConfigurationUtil.getIntSimSlot(config) == simSlot) {
                this.mWifiNative.setNetworkVariable(config.networkId, "identity", "NULL");
                this.mWifiNative.setNetworkVariable(config.networkId, "anonymous_identity", "NULL");
            }
        }
    }

    public void clearBssidBlacklist() {
        if (VDBG) {
            localLog("clearBlacklist");
        }
        this.mBssidBlacklist.clear();
        this.mWifiNative.clearBlacklist();
        this.mWifiNative.setBssidBlacklist(null);
    }

    public void blackListBssid(String bssid) {
        if (bssid == null) {
            return;
        }
        if (VDBG) {
            localLog("blackListBssid: " + bssid);
        }
        this.mBssidBlacklist.add(bssid);
        this.mWifiNative.addToBlacklist(bssid);
        String[] list = (String[]) this.mBssidBlacklist.toArray(new String[this.mBssidBlacklist.size()]);
        this.mWifiNative.setBssidBlacklist(list);
    }

    public boolean isBssidBlacklisted(String bssid) {
        return this.mBssidBlacklist.contains(bssid);
    }

    private void markAllNetworksDisabledExcept(int netId, Collection<WifiConfiguration> configs) {
        for (WifiConfiguration config : configs) {
            if (config != null && config.networkId != netId && config.status != 1) {
                config.status = 1;
            }
        }
    }

    private void markAllNetworksDisabled(Collection<WifiConfiguration> configs) {
        markAllNetworksDisabledExcept(-1, configs);
    }

    public WpsResult startWpsWithPinFromAccessPoint(WpsInfo config, Collection<WifiConfiguration> configs) {
        WpsResult result = new WpsResult();
        if (this.mWifiNative.startWpsRegistrar(config.BSSID, config.pin)) {
            markAllNetworksDisabled(configs);
            result.status = WpsResult.Status.SUCCESS;
        } else {
            loge("Failed to start WPS pin method configuration");
            result.status = WpsResult.Status.FAILURE;
        }
        return result;
    }

    public WpsResult startWpsWithPinFromDevice(WpsInfo config, Collection<WifiConfiguration> configs) {
        WpsResult result = new WpsResult();
        result.pin = this.mWifiNative.startWpsPinDisplay(config.BSSID);
        if (!TextUtils.isEmpty(result.pin)) {
            markAllNetworksDisabled(configs);
            result.status = WpsResult.Status.SUCCESS;
        } else {
            loge("Failed to start WPS pin method configuration");
            result.status = WpsResult.Status.FAILURE;
        }
        return result;
    }

    public WpsResult startWpsPbc(WpsInfo config, Collection<WifiConfiguration> configs) {
        WpsResult result = new WpsResult();
        if (this.mWifiNative.startWpsPbc(config.BSSID)) {
            markAllNetworksDisabled(configs);
            result.status = WpsResult.Status.SUCCESS;
        } else {
            loge("Failed to start WPS push button configuration");
            result.status = WpsResult.Status.FAILURE;
        }
        return result;
    }

    protected void logd(String s) {
        Log.d(TAG, s);
    }

    protected void logi(String s) {
        Log.i(TAG, s);
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
            this.mLocalLog.log("WifiConfigStore: " + s);
        }
        Log.d(TAG, s);
    }

    private void localLogAndLogcat(String s) {
        localLog(s);
        Log.d(TAG, s);
    }

    private class SupplicantSaver implements WifiEnterpriseConfig.SupplicantSaver {
        private final int mNetId;
        private final String mSetterSSID;

        SupplicantSaver(int netId, String setterSSID) {
            this.mNetId = netId;
            this.mSetterSSID = setterSSID;
        }

        public boolean saveValue(String key, String value) {
            if ((key.equals("password") && value != null && value.equals("*")) || key.equals("realm") || key.equals("plmn")) {
                return true;
            }
            if (value == null) {
                value = "\"\"";
            }
            if (WifiConfigStore.this.mWifiNative.setNetworkVariable(this.mNetId, key, value)) {
                return true;
            }
            WifiConfigStore.this.loge(this.mSetterSSID + ": failed to set " + key + ": " + value);
            return false;
        }
    }

    private class SupplicantLoader implements WifiEnterpriseConfig.SupplicantLoader {
        private final int mNetId;

        SupplicantLoader(int netId) {
            this.mNetId = netId;
        }

        public String loadValue(String key) {
            String value = WifiConfigStore.this.mWifiNative.getNetworkVariable(this.mNetId, key);
            if (!TextUtils.isEmpty(value)) {
                if (!enterpriseConfigKeyShouldBeQuoted(key)) {
                    return WifiConfigStore.removeDoubleQuotes(value);
                }
                return value;
            }
            return null;
        }

        private boolean enterpriseConfigKeyShouldBeQuoted(String key) {
            if (key.equals("eap") || key.equals("engine")) {
                return false;
            }
            return true;
        }
    }

    private class WpaConfigFileObserver extends FileObserver {
        WpaConfigFileObserver() {
            super(WifiConfigStore.SUPPLICANT_CONFIG_FILE, 8);
        }

        @Override
        public void onEvent(int event, String path) {
            if (event != 8) {
                return;
            }
            File file = new File(WifiConfigStore.SUPPLICANT_CONFIG_FILE);
            if (WifiConfigStore.VDBG) {
                WifiConfigStore.this.localLog("wpa_supplicant.conf changed; new size = " + file.length());
            }
        }
    }

    private boolean isWapiConfig(WifiConfiguration config) {
        boolean isWapi = false;
        for (int p = 0; p < config.allowedProtocols.size(); p++) {
            if (config.allowedProtocols.get(p) && p == 3) {
                Log.e(TAG, "this is WAPI");
                isWapi = true;
            }
        }
        return isWapi;
    }
}
