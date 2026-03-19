package com.android.server.wifi;

import android.content.pm.UserInfo;
import android.net.IpConfiguration;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiScanner;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.NativeUtil;
import com.mediatek.server.wifi.MtkEapSimUtility;
import com.mediatek.server.wifi.MtkGbkSsid;
import com.mediatek.server.wifi.MtkWapi;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class WifiConfigurationUtil {
    private static final int ENCLOSING_QUTOES_LEN = 2;

    @VisibleForTesting
    public static final String PASSWORD_MASK = "*";
    private static final int PSK_ASCII_MAX_LEN = 65;
    private static final int PSK_ASCII_MIN_LEN = 10;
    private static final int PSK_HEX_LEN = 64;
    private static final int SSID_HEX_MAX_LEN = 64;
    private static final int SSID_HEX_MIN_LEN = 2;
    private static final int SSID_UTF_8_MAX_LEN = 34;
    private static final int SSID_UTF_8_MIN_LEN = 3;
    private static final String TAG = "WifiConfigurationUtil";
    public static final boolean VALIDATE_FOR_ADD = true;
    public static final boolean VALIDATE_FOR_UPDATE = false;

    public static boolean isVisibleToAnyProfile(WifiConfiguration wifiConfiguration, List<UserInfo> list) {
        return wifiConfiguration.shared || doesUidBelongToAnyProfile(wifiConfiguration.creatorUid, list);
    }

    public static boolean doesUidBelongToAnyProfile(int i, List<UserInfo> list) {
        int userId = UserHandle.getUserId(i);
        Iterator<UserInfo> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().id == userId) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnyValidWepKey(String[] strArr) {
        for (String str : strArr) {
            if (str != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConfigForPskNetwork(WifiConfiguration wifiConfiguration) {
        return wifiConfiguration.allowedKeyManagement.get(1);
    }

    public static boolean isConfigForEapNetwork(WifiConfiguration wifiConfiguration) {
        return wifiConfiguration.allowedKeyManagement.get(2) || wifiConfiguration.allowedKeyManagement.get(3);
    }

    public static boolean isConfigForWepNetwork(WifiConfiguration wifiConfiguration) {
        return wifiConfiguration.allowedKeyManagement.get(0) && hasAnyValidWepKey(wifiConfiguration.wepKeys);
    }

    public static boolean isConfigForOpenNetwork(WifiConfiguration wifiConfiguration) {
        return (isConfigForWepNetwork(wifiConfiguration) || isConfigForPskNetwork(wifiConfiguration) || MtkWapi.isConfigForWapiNetwork(wifiConfiguration) || isConfigForEapNetwork(wifiConfiguration)) ? false : true;
    }

    public static boolean hasIpChanged(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (wifiConfiguration.getIpAssignment() != wifiConfiguration2.getIpAssignment()) {
            return true;
        }
        if (wifiConfiguration2.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
            return !Objects.equals(wifiConfiguration.getStaticIpConfiguration(), wifiConfiguration2.getStaticIpConfiguration());
        }
        return false;
    }

    public static boolean hasProxyChanged(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (wifiConfiguration == null) {
            return wifiConfiguration2.getProxySettings() != IpConfiguration.ProxySettings.NONE;
        }
        if (wifiConfiguration2.getProxySettings() != wifiConfiguration.getProxySettings()) {
            return true;
        }
        return !Objects.equals(wifiConfiguration.getHttpProxy(), wifiConfiguration2.getHttpProxy());
    }

    @VisibleForTesting
    public static boolean hasEnterpriseConfigChanged(WifiEnterpriseConfig wifiEnterpriseConfig, WifiEnterpriseConfig wifiEnterpriseConfig2) {
        if (wifiEnterpriseConfig != null && wifiEnterpriseConfig2 != null) {
            if (wifiEnterpriseConfig.getEapMethod() != wifiEnterpriseConfig2.getEapMethod() || wifiEnterpriseConfig.getPhase2Method() != wifiEnterpriseConfig2.getPhase2Method() || !TextUtils.equals(wifiEnterpriseConfig.getIdentity(), wifiEnterpriseConfig2.getIdentity()) || !TextUtils.equals(wifiEnterpriseConfig.getAnonymousIdentity(), wifiEnterpriseConfig2.getAnonymousIdentity()) || !TextUtils.equals(wifiEnterpriseConfig.getPassword(), wifiEnterpriseConfig2.getPassword()) || !Arrays.equals(wifiEnterpriseConfig.getCaCertificates(), wifiEnterpriseConfig2.getCaCertificates())) {
                return true;
            }
            return false;
        }
        if (wifiEnterpriseConfig != null || wifiEnterpriseConfig2 != null) {
            return true;
        }
        return false;
    }

    public static boolean hasCredentialChanged(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        return (Objects.equals(wifiConfiguration.allowedKeyManagement, wifiConfiguration2.allowedKeyManagement) && Objects.equals(wifiConfiguration.allowedProtocols, wifiConfiguration2.allowedProtocols) && Objects.equals(wifiConfiguration.allowedAuthAlgorithms, wifiConfiguration2.allowedAuthAlgorithms) && Objects.equals(wifiConfiguration.allowedPairwiseCiphers, wifiConfiguration2.allowedPairwiseCiphers) && Objects.equals(wifiConfiguration.allowedGroupCiphers, wifiConfiguration2.allowedGroupCiphers) && Objects.equals(wifiConfiguration.preSharedKey, wifiConfiguration2.preSharedKey) && Arrays.equals(wifiConfiguration.wepKeys, wifiConfiguration2.wepKeys) && wifiConfiguration.wepTxKeyIndex == wifiConfiguration2.wepTxKeyIndex && wifiConfiguration.hiddenSSID == wifiConfiguration2.hiddenSSID && !hasEnterpriseConfigChanged(wifiConfiguration.enterpriseConfig, wifiConfiguration2.enterpriseConfig) && MtkEapSimUtility.getIntSimSlot(wifiConfiguration) == MtkEapSimUtility.getIntSimSlot(wifiConfiguration2) && !MtkWapi.hasWapiConfigChanged(wifiConfiguration, wifiConfiguration2)) ? false : true;
    }

    private static boolean validateSsid(String str, boolean z) {
        if (z) {
            if (str == null) {
                Log.e(TAG, "validateSsid : null string");
                return false;
            }
        } else if (str == null) {
            return true;
        }
        if (str.isEmpty()) {
            Log.e(TAG, "validateSsid failed: empty string");
            return false;
        }
        if (str.startsWith("\"")) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            if (MtkGbkSsid.isGbkSsid(str)) {
                bytes = MtkGbkSsid.stringToByteArray(str);
            }
            if (bytes.length < 3) {
                Log.e(TAG, "validateSsid failed: utf-8 ssid string size too small: " + bytes.length);
                return false;
            }
            if (bytes.length > 34) {
                Log.e(TAG, "validateSsid failed: utf-8 ssid string size too large: " + bytes.length);
                return false;
            }
        } else {
            if (str.length() < 2) {
                Log.e(TAG, "validateSsid failed: hex string size too small: " + str.length());
                return false;
            }
            if (str.length() > 64) {
                Log.e(TAG, "validateSsid failed: hex string size too large: " + str.length());
                return false;
            }
        }
        try {
            NativeUtil.decodeSsid(str);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "validateSsid failed: malformed string: " + str);
            return false;
        }
    }

    private static boolean validatePsk(String str, boolean z) {
        if (z) {
            if (str == null) {
                Log.e(TAG, "validatePsk: null string");
                return false;
            }
        } else if (str == null || str.equals("*")) {
            return true;
        }
        if (str.isEmpty()) {
            Log.e(TAG, "validatePsk failed: empty string");
            return false;
        }
        if (str.startsWith("\"")) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            if (bytes.length < 10) {
                Log.e(TAG, "validatePsk failed: ascii string size too small: " + bytes.length);
                return false;
            }
            if (bytes.length > 65) {
                Log.e(TAG, "validatePsk failed: ascii string size too large: " + bytes.length);
                return false;
            }
        } else if (str.length() != 64) {
            Log.e(TAG, "validatePsk failed: hex string size mismatch: " + str.length());
            return false;
        }
        try {
            NativeUtil.hexOrQuotedStringToBytes(str);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "validatePsk failed: malformed string: " + str);
            return false;
        }
    }

    private static boolean validateBitSet(BitSet bitSet, int i) {
        if (bitSet == null) {
            return false;
        }
        BitSet bitSet2 = (BitSet) bitSet.clone();
        bitSet2.clear(0, i);
        return bitSet2.isEmpty();
    }

    private static boolean validateBitSets(WifiConfiguration wifiConfiguration) {
        if (!validateBitSet(wifiConfiguration.allowedKeyManagement, WifiConfiguration.KeyMgmt.strings.length)) {
            Log.e(TAG, "validateBitsets failed: invalid allowedKeyManagement bitset " + wifiConfiguration.allowedKeyManagement);
            return false;
        }
        if (!validateBitSet(wifiConfiguration.allowedProtocols, WifiConfiguration.Protocol.strings.length)) {
            Log.e(TAG, "validateBitsets failed: invalid allowedProtocols bitset " + wifiConfiguration.allowedProtocols);
            return false;
        }
        if (!validateBitSet(wifiConfiguration.allowedAuthAlgorithms, WifiConfiguration.AuthAlgorithm.strings.length)) {
            Log.e(TAG, "validateBitsets failed: invalid allowedAuthAlgorithms bitset " + wifiConfiguration.allowedAuthAlgorithms);
            return false;
        }
        if (!validateBitSet(wifiConfiguration.allowedGroupCiphers, WifiConfiguration.GroupCipher.strings.length)) {
            Log.e(TAG, "validateBitsets failed: invalid allowedGroupCiphers bitset " + wifiConfiguration.allowedGroupCiphers);
            return false;
        }
        if (!validateBitSet(wifiConfiguration.allowedPairwiseCiphers, WifiConfiguration.PairwiseCipher.strings.length)) {
            Log.e(TAG, "validateBitsets failed: invalid allowedPairwiseCiphers bitset " + wifiConfiguration.allowedPairwiseCiphers);
            return false;
        }
        return true;
    }

    private static boolean validateKeyMgmt(BitSet bitSet) {
        if (bitSet.cardinality() > 1) {
            if (bitSet.cardinality() != 2) {
                Log.e(TAG, "validateKeyMgmt failed: cardinality != 2");
                return false;
            }
            if (!bitSet.get(2)) {
                Log.e(TAG, "validateKeyMgmt failed: not WPA_EAP");
                return false;
            }
            if (!bitSet.get(3) && !bitSet.get(1)) {
                Log.e(TAG, "validateKeyMgmt failed: not PSK or 8021X");
                return false;
            }
        }
        return true;
    }

    private static boolean validateIpConfiguration(IpConfiguration ipConfiguration) {
        if (ipConfiguration == null) {
            Log.e(TAG, "validateIpConfiguration failed: null IpConfiguration");
            return false;
        }
        if (ipConfiguration.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
            StaticIpConfiguration staticIpConfiguration = ipConfiguration.getStaticIpConfiguration();
            if (staticIpConfiguration == null) {
                Log.e(TAG, "validateIpConfiguration failed: null StaticIpConfiguration");
                return false;
            }
            if (staticIpConfiguration.ipAddress == null) {
                Log.e(TAG, "validateIpConfiguration failed: null static ip Address");
                return false;
            }
            return true;
        }
        return true;
    }

    public static boolean validate(WifiConfiguration wifiConfiguration, boolean z) {
        if (validateSsid(wifiConfiguration.SSID, z) && validateBitSets(wifiConfiguration) && validateKeyMgmt(wifiConfiguration.allowedKeyManagement)) {
            return (!wifiConfiguration.allowedKeyManagement.get(1) || validatePsk(wifiConfiguration.preSharedKey, z)) && validateIpConfiguration(wifiConfiguration.getIpConfiguration());
        }
        return false;
    }

    public static boolean isSameNetwork(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (wifiConfiguration == null && wifiConfiguration2 == null) {
            return true;
        }
        if (wifiConfiguration != null && wifiConfiguration2 != null && wifiConfiguration.networkId == wifiConfiguration2.networkId && Objects.equals(wifiConfiguration.SSID, wifiConfiguration2.SSID) && !hasCredentialChanged(wifiConfiguration, wifiConfiguration2)) {
            return true;
        }
        return false;
    }

    public static WifiScanner.PnoSettings.PnoNetwork createPnoNetwork(WifiConfiguration wifiConfiguration) {
        WifiScanner.PnoSettings.PnoNetwork pnoNetwork = new WifiScanner.PnoSettings.PnoNetwork(wifiConfiguration.SSID);
        if (wifiConfiguration.hiddenSSID) {
            pnoNetwork.flags = (byte) (pnoNetwork.flags | 1);
        }
        pnoNetwork.flags = (byte) (pnoNetwork.flags | 2);
        pnoNetwork.flags = (byte) (pnoNetwork.flags | 4);
        if (wifiConfiguration.allowedKeyManagement.get(1)) {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 2);
        } else if (wifiConfiguration.allowedKeyManagement.get(2) || wifiConfiguration.allowedKeyManagement.get(3)) {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 4);
        } else {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 1);
        }
        return pnoNetwork;
    }

    public static abstract class WifiConfigurationComparator implements Comparator<WifiConfiguration> {
        private static final int ENABLED_NETWORK_SCORE = 3;
        private static final int PERMANENTLY_DISABLED_NETWORK_SCORE = 1;
        private static final int TEMPORARY_DISABLED_NETWORK_SCORE = 2;

        abstract int compareNetworksWithSameStatus(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2);

        @Override
        public int compare(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
            int networkStatusScore = getNetworkStatusScore(wifiConfiguration);
            int networkStatusScore2 = getNetworkStatusScore(wifiConfiguration2);
            if (networkStatusScore == networkStatusScore2) {
                return compareNetworksWithSameStatus(wifiConfiguration, wifiConfiguration2);
            }
            return Integer.compare(networkStatusScore2, networkStatusScore);
        }

        private int getNetworkStatusScore(WifiConfiguration wifiConfiguration) {
            if (wifiConfiguration.getNetworkSelectionStatus().isNetworkEnabled()) {
                return 3;
            }
            if (wifiConfiguration.getNetworkSelectionStatus().isNetworkTemporaryDisabled()) {
                return 2;
            }
            return 1;
        }
    }
}
