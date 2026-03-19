package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class WifiBackupDataV1Parser implements WifiBackupDataParser {
    private static final int HIGHEST_SUPPORTED_MINOR_VERSION = 0;
    private static final String TAG = "WifiBackupDataV1Parser";
    private static final Set<String> WIFI_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS = new HashSet(Arrays.asList(XmlUtil.WifiConfigurationXmlUtil.XML_TAG_CONFIG_KEY, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_SSID, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_BSSID, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_PRE_SHARED_KEY, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_WEP_KEYS, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_WEP_TX_KEY_INDEX, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_HIDDEN_SSID, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_REQUIRE_PMF, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_ALLOWED_KEY_MGMT, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_ALLOWED_PROTOCOLS, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_ALLOWED_AUTH_ALGOS, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_ALLOWED_GROUP_CIPHERS, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_ALLOWED_PAIRWISE_CIPHERS, XmlUtil.WifiConfigurationXmlUtil.XML_TAG_SHARED));
    private static final Set<String> IP_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS = new HashSet(Arrays.asList(XmlUtil.IpConfigurationXmlUtil.XML_TAG_IP_ASSIGNMENT, XmlUtil.IpConfigurationXmlUtil.XML_TAG_LINK_ADDRESS, XmlUtil.IpConfigurationXmlUtil.XML_TAG_LINK_PREFIX_LENGTH, XmlUtil.IpConfigurationXmlUtil.XML_TAG_GATEWAY_ADDRESS, XmlUtil.IpConfigurationXmlUtil.XML_TAG_DNS_SERVER_ADDRESSES, XmlUtil.IpConfigurationXmlUtil.XML_TAG_PROXY_SETTINGS, XmlUtil.IpConfigurationXmlUtil.XML_TAG_PROXY_HOST, XmlUtil.IpConfigurationXmlUtil.XML_TAG_PROXY_PORT, XmlUtil.IpConfigurationXmlUtil.XML_TAG_PROXY_EXCLUSION_LIST, XmlUtil.IpConfigurationXmlUtil.XML_TAG_PROXY_PAC_FILE));

    WifiBackupDataV1Parser() {
    }

    @Override
    public List<WifiConfiguration> parseNetworkConfigurationsFromXml(XmlPullParser xmlPullParser, int i, int i2) throws XmlPullParserException, IOException {
        if (i2 > 0) {
            i2 = 0;
        }
        XmlUtil.gotoNextSectionWithName(xmlPullParser, "NetworkList", i);
        int i3 = i + 1;
        ArrayList arrayList = new ArrayList();
        while (XmlUtil.gotoNextSectionWithNameOrEnd(xmlPullParser, "Network", i3)) {
            WifiConfiguration networkConfigurationFromXml = parseNetworkConfigurationFromXml(xmlPullParser, i2, i3);
            if (networkConfigurationFromXml != null) {
                Log.v(TAG, "Parsed Configuration: " + networkConfigurationFromXml.configKey());
                arrayList.add(networkConfigurationFromXml);
            }
        }
        return arrayList;
    }

    private WifiConfiguration parseNetworkConfigurationFromXml(XmlPullParser xmlPullParser, int i, int i2) throws XmlPullParserException, IOException {
        int i3 = i2 + 1;
        XmlUtil.gotoNextSectionWithName(xmlPullParser, "WifiConfiguration", i3);
        int i4 = i3 + 1;
        WifiConfiguration wifiConfigurationFromXmlAndValidateConfigKey = parseWifiConfigurationFromXmlAndValidateConfigKey(xmlPullParser, i4, i);
        if (wifiConfigurationFromXmlAndValidateConfigKey == null) {
            return null;
        }
        XmlUtil.gotoNextSectionWithName(xmlPullParser, "IpConfiguration", i3);
        wifiConfigurationFromXmlAndValidateConfigKey.setIpConfiguration(parseIpConfigurationFromXml(xmlPullParser, i4, i));
        return wifiConfigurationFromXmlAndValidateConfigKey;
    }

    private WifiConfiguration parseWifiConfigurationFromXmlAndValidateConfigKey(XmlPullParser xmlPullParser, int i, int i2) throws XmlPullParserException, IOException {
        Pair<String, WifiConfiguration> wifiConfigurationFromXml = parseWifiConfigurationFromXml(xmlPullParser, i, i2);
        if (wifiConfigurationFromXml == null || wifiConfigurationFromXml.first == null || wifiConfigurationFromXml.second == null) {
            return null;
        }
        String str = (String) wifiConfigurationFromXml.first;
        WifiConfiguration wifiConfiguration = (WifiConfiguration) wifiConfigurationFromXml.second;
        String strConfigKey = wifiConfiguration.configKey();
        if (!str.equals(strConfigKey)) {
            String str2 = "Configuration key does not match. Retrieved: " + str + ", Calculated: " + strConfigKey;
            if (wifiConfiguration.shared) {
                Log.e(TAG, str2);
                return null;
            }
            Log.w(TAG, str2);
        }
        return wifiConfiguration;
    }

    private static void clearAnyKnownIssuesInParsedConfiguration(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.allowedKeyManagement.length() > WifiConfiguration.KeyMgmt.strings.length) {
            wifiConfiguration.allowedKeyManagement.clear(WifiConfiguration.KeyMgmt.strings.length, wifiConfiguration.allowedKeyManagement.length());
        }
        if (wifiConfiguration.allowedProtocols.length() > WifiConfiguration.Protocol.strings.length) {
            wifiConfiguration.allowedProtocols.clear(WifiConfiguration.Protocol.strings.length, wifiConfiguration.allowedProtocols.length());
        }
        if (wifiConfiguration.allowedAuthAlgorithms.length() > WifiConfiguration.AuthAlgorithm.strings.length) {
            wifiConfiguration.allowedAuthAlgorithms.clear(WifiConfiguration.AuthAlgorithm.strings.length, wifiConfiguration.allowedAuthAlgorithms.length());
        }
        if (wifiConfiguration.allowedGroupCiphers.length() > WifiConfiguration.GroupCipher.strings.length) {
            wifiConfiguration.allowedGroupCiphers.clear(WifiConfiguration.GroupCipher.strings.length, wifiConfiguration.allowedGroupCiphers.length());
        }
        if (wifiConfiguration.allowedPairwiseCiphers.length() > WifiConfiguration.PairwiseCipher.strings.length) {
            wifiConfiguration.allowedPairwiseCiphers.clear(WifiConfiguration.PairwiseCipher.strings.length, wifiConfiguration.allowedPairwiseCiphers.length());
        }
    }

    private static Pair<String, WifiConfiguration> parseWifiConfigurationFromXml(XmlPullParser xmlPullParser, int i, int i2) throws XmlPullParserException, IOException {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        Set<String> supportedWifiConfigurationTags = getSupportedWifiConfigurationTags(i2);
        String str = null;
        while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
            String[] strArr = new String[1];
            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
            String str2 = strArr[0];
            if (str2 == null) {
                throw new XmlPullParserException("Missing value name");
            }
            if (!supportedWifiConfigurationTags.contains(str2)) {
                Log.w(TAG, "Unsupported tag + \"" + str2 + "\" found in <WifiConfiguration> section, ignoring.");
            } else {
                switch (str2) {
                    case "ConfigKey":
                        str = (String) currentValue;
                        break;
                    case "SSID":
                        wifiConfiguration.SSID = (String) currentValue;
                        break;
                    case "BSSID":
                        wifiConfiguration.BSSID = (String) currentValue;
                        break;
                    case "PreSharedKey":
                        wifiConfiguration.preSharedKey = (String) currentValue;
                        break;
                    case "WEPKeys":
                        populateWepKeysFromXmlValue(currentValue, wifiConfiguration.wepKeys);
                        break;
                    case "WEPTxKeyIndex":
                        wifiConfiguration.wepTxKeyIndex = ((Integer) currentValue).intValue();
                        break;
                    case "HiddenSSID":
                        wifiConfiguration.hiddenSSID = ((Boolean) currentValue).booleanValue();
                        break;
                    case "RequirePMF":
                        wifiConfiguration.requirePMF = ((Boolean) currentValue).booleanValue();
                        break;
                    case "AllowedKeyMgmt":
                        wifiConfiguration.allowedKeyManagement = BitSet.valueOf((byte[]) currentValue);
                        break;
                    case "AllowedProtocols":
                        wifiConfiguration.allowedProtocols = BitSet.valueOf((byte[]) currentValue);
                        break;
                    case "AllowedAuthAlgos":
                        wifiConfiguration.allowedAuthAlgorithms = BitSet.valueOf((byte[]) currentValue);
                        break;
                    case "AllowedGroupCiphers":
                        wifiConfiguration.allowedGroupCiphers = BitSet.valueOf((byte[]) currentValue);
                        break;
                    case "AllowedPairwiseCiphers":
                        wifiConfiguration.allowedPairwiseCiphers = BitSet.valueOf((byte[]) currentValue);
                        break;
                    case "Shared":
                        wifiConfiguration.shared = ((Boolean) currentValue).booleanValue();
                        break;
                    default:
                        throw new XmlPullParserException("Unknown value name found: " + strArr[0]);
                }
            }
        }
        clearAnyKnownIssuesInParsedConfiguration(wifiConfiguration);
        return Pair.create(str, wifiConfiguration);
    }

    private static Set<String> getSupportedWifiConfigurationTags(int i) {
        if (i == 0) {
            return WIFI_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS;
        }
        Log.e(TAG, "Invalid minorVersion: " + i);
        return Collections.emptySet();
    }

    private static void populateWepKeysFromXmlValue(Object obj, String[] strArr) throws XmlPullParserException, IOException {
        String[] strArr2 = (String[]) obj;
        if (strArr2 == null) {
            return;
        }
        if (strArr2.length != strArr.length) {
            throw new XmlPullParserException("Invalid Wep Keys length: " + strArr2.length);
        }
        for (int i = 0; i < strArr.length; i++) {
            if (strArr2[i].isEmpty()) {
                strArr[i] = null;
            } else {
                strArr[i] = strArr2[i];
            }
        }
    }

    private static IpConfiguration parseIpConfigurationFromXml(XmlPullParser xmlPullParser, int i, int i2) throws XmlPullParserException, IOException {
        Set<String> supportedIpConfigurationTags = getSupportedIpConfigurationTags(i2);
        String str = null;
        String str2 = null;
        Integer num = null;
        String str3 = null;
        String[] strArr = null;
        String str4 = null;
        String str5 = null;
        String str6 = null;
        int iIntValue = -1;
        String str7 = null;
        while (true) {
            if (XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                IpConfiguration ipConfiguration = new IpConfiguration();
                if (str == null) {
                    throw new XmlPullParserException("IpAssignment was missing in IpConfiguration section");
                }
                IpConfiguration.IpAssignment ipAssignmentValueOf = IpConfiguration.IpAssignment.valueOf(str);
                ipConfiguration.setIpAssignment(ipAssignmentValueOf);
                switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipAssignmentValueOf.ordinal()]) {
                    case 1:
                        StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                        if (str2 != null && num != null) {
                            LinkAddress linkAddress = new LinkAddress(NetworkUtils.numericToInetAddress(str2), num.intValue());
                            if (linkAddress.getAddress() instanceof Inet4Address) {
                                staticIpConfiguration.ipAddress = linkAddress;
                            } else {
                                Log.w(TAG, "Non-IPv4 address: " + linkAddress);
                            }
                        }
                        if (str3 != null) {
                            InetAddress inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(str3);
                            RouteInfo routeInfo = new RouteInfo(null, inetAddressNumericToInetAddress);
                            if (routeInfo.isIPv4Default()) {
                                staticIpConfiguration.gateway = inetAddressNumericToInetAddress;
                            } else {
                                Log.w(TAG, "Non-IPv4 default route: " + routeInfo);
                            }
                        }
                        if (strArr != null) {
                            for (String str8 : strArr) {
                                staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(str8));
                            }
                        }
                        ipConfiguration.setStaticIpConfiguration(staticIpConfiguration);
                        break;
                    case 2:
                    case 3:
                        break;
                    default:
                        throw new XmlPullParserException("Unknown ip assignment type: " + ipAssignmentValueOf);
                }
                if (str4 == null) {
                    throw new XmlPullParserException("ProxySettings was missing in IpConfiguration section");
                }
                IpConfiguration.ProxySettings proxySettingsValueOf = IpConfiguration.ProxySettings.valueOf(str4);
                ipConfiguration.setProxySettings(proxySettingsValueOf);
                switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[proxySettingsValueOf.ordinal()]) {
                    case 1:
                        if (str5 == null) {
                            throw new XmlPullParserException("ProxyHost was missing in IpConfiguration section");
                        }
                        if (iIntValue == -1) {
                            throw new XmlPullParserException("ProxyPort was missing in IpConfiguration section");
                        }
                        if (str7 == null) {
                            throw new XmlPullParserException("ProxyExclusionList was missing in IpConfiguration section");
                        }
                        ipConfiguration.setHttpProxy(new ProxyInfo(str5, iIntValue, str7));
                        return ipConfiguration;
                    case 2:
                        if (str6 == null) {
                            throw new XmlPullParserException("ProxyPac was missing in IpConfiguration section");
                        }
                        ipConfiguration.setHttpProxy(new ProxyInfo(str6));
                        return ipConfiguration;
                    case 3:
                    case 4:
                        return ipConfiguration;
                    default:
                        throw new XmlPullParserException("Unknown proxy settings type: " + proxySettingsValueOf);
                }
            }
            String[] strArr2 = new String[1];
            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr2);
            String str9 = strArr2[0];
            if (str9 == null) {
                throw new XmlPullParserException("Missing value name");
            }
            if (supportedIpConfigurationTags.contains(str9)) {
                switch (str9) {
                    case "IpAssignment":
                        str = (String) currentValue;
                        break;
                    case "LinkAddress":
                        str2 = (String) currentValue;
                        break;
                    case "LinkPrefixLength":
                        num = (Integer) currentValue;
                        break;
                    case "GatewayAddress":
                        str3 = (String) currentValue;
                        break;
                    case "DNSServers":
                        strArr = (String[]) currentValue;
                        break;
                    case "ProxySettings":
                        str4 = (String) currentValue;
                        break;
                    case "ProxyHost":
                        str5 = (String) currentValue;
                        break;
                    case "ProxyPort":
                        iIntValue = ((Integer) currentValue).intValue();
                        break;
                    case "ProxyExclusionList":
                        str7 = (String) currentValue;
                        break;
                    case "ProxyPac":
                        str6 = (String) currentValue;
                        break;
                    default:
                        throw new XmlPullParserException("Unknown value name found: " + strArr2[0]);
                }
            } else {
                Log.w(TAG, "Unsupported tag + \"" + str9 + "\" found in <IpConfiguration> section, ignoring.");
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$net$IpConfiguration$IpAssignment;
        static final int[] $SwitchMap$android$net$IpConfiguration$ProxySettings = new int[IpConfiguration.ProxySettings.values().length];

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
        }
    }

    private static Set<String> getSupportedIpConfigurationTags(int i) {
        if (i == 0) {
            return IP_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS;
        }
        Log.e(TAG, "Invalid minorVersion: " + i);
        return Collections.emptySet();
    }
}
