package com.android.server.wifi.util;

import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.MacAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.XmlUtils;
import com.android.server.wifi.WifiBackupRestore;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XmlUtil {
    private static final String TAG = "WifiXmlUtil";
    private static String mSimSlot;

    private static void gotoStartTag(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        while (eventType != 2 && eventType != 1) {
            eventType = xmlPullParser.next();
        }
    }

    private static void gotoEndTag(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        while (eventType != 3 && eventType != 1) {
            eventType = xmlPullParser.next();
        }
    }

    public static void gotoDocumentStart(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(xmlPullParser, str);
    }

    public static boolean gotoNextSectionOrEnd(XmlPullParser xmlPullParser, String[] strArr, int i) throws XmlPullParserException, IOException {
        if (!XmlUtils.nextElementWithin(xmlPullParser, i)) {
            return false;
        }
        strArr[0] = xmlPullParser.getName();
        return true;
    }

    public static boolean gotoNextSectionWithNameOrEnd(XmlPullParser xmlPullParser, String str, int i) throws XmlPullParserException, IOException {
        String[] strArr = new String[1];
        if (!gotoNextSectionOrEnd(xmlPullParser, strArr, i)) {
            return false;
        }
        if (strArr[0].equals(str)) {
            return true;
        }
        throw new XmlPullParserException("Next section name does not match expected name: " + str);
    }

    public static void gotoNextSectionWithName(XmlPullParser xmlPullParser, String str, int i) throws XmlPullParserException, IOException {
        if (!gotoNextSectionWithNameOrEnd(xmlPullParser, str, i)) {
            throw new XmlPullParserException("Section not found. Expected: " + str);
        }
    }

    public static boolean isNextSectionEnd(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        return !XmlUtils.nextElementWithin(xmlPullParser, i);
    }

    public static Object readCurrentValue(XmlPullParser xmlPullParser, String[] strArr) throws XmlPullParserException, IOException {
        Object valueXml = XmlUtils.readValueXml(xmlPullParser, strArr);
        gotoEndTag(xmlPullParser);
        return valueXml;
    }

    public static Object readNextValueWithName(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        String[] strArr = new String[1];
        XmlUtils.nextElement(xmlPullParser);
        Object currentValue = readCurrentValue(xmlPullParser, strArr);
        if (!strArr[0].equals(str)) {
            throw new XmlPullParserException("Value not found. Expected: " + str + ", but got: " + strArr[0]);
        }
        return currentValue;
    }

    public static void writeDocumentStart(XmlSerializer xmlSerializer, String str) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.startTag(null, str);
    }

    public static void writeDocumentEnd(XmlSerializer xmlSerializer, String str) throws IOException {
        xmlSerializer.endTag(null, str);
        xmlSerializer.endDocument();
    }

    public static void writeNextSectionStart(XmlSerializer xmlSerializer, String str) throws IOException {
        xmlSerializer.startTag(null, str);
    }

    public static void writeNextSectionEnd(XmlSerializer xmlSerializer, String str) throws IOException {
        xmlSerializer.endTag(null, str);
    }

    public static void writeNextValue(XmlSerializer xmlSerializer, String str, Object obj) throws XmlPullParserException, IOException {
        XmlUtils.writeValueXml(obj, str, xmlSerializer);
    }

    public static class WifiConfigurationXmlUtil {
        public static final String XML_TAG_ALIASES = "Aliases";
        public static final String XML_TAG_ALLOWED_AUTH_ALGOS = "AllowedAuthAlgos";
        public static final String XML_TAG_ALLOWED_GROUP_CIPHERS = "AllowedGroupCiphers";
        public static final String XML_TAG_ALLOWED_KEY_MGMT = "AllowedKeyMgmt";
        public static final String XML_TAG_ALLOWED_PAIRWISE_CIPHERS = "AllowedPairwiseCiphers";
        public static final String XML_TAG_ALLOWED_PROTOCOLS = "AllowedProtocols";
        public static final String XML_TAG_BSSID = "BSSID";
        public static final String XML_TAG_CONFIG_KEY = "ConfigKey";
        public static final String XML_TAG_CREATION_TIME = "CreationTime";
        public static final String XML_TAG_CREATOR_NAME = "CreatorName";
        public static final String XML_TAG_CREATOR_UID = "CreatorUid";
        public static final String XML_TAG_DEFAULT_GW_MAC_ADDRESS = "DefaultGwMacAddress";
        public static final String XML_TAG_FQDN = "FQDN";
        public static final String XML_TAG_HIDDEN_SSID = "HiddenSSID";
        public static final String XML_TAG_IS_LEGACY_PASSPOINT_CONFIG = "IsLegacyPasspointConfig";
        public static final String XML_TAG_LAST_CONNECT_UID = "LastConnectUid";
        public static final String XML_TAG_LAST_UPDATE_NAME = "LastUpdateName";
        public static final String XML_TAG_LAST_UPDATE_UID = "LastUpdateUid";
        public static final String XML_TAG_LINKED_NETWORKS_LIST = "LinkedNetworksList";
        public static final String XML_TAG_METERED_HINT = "MeteredHint";
        public static final String XML_TAG_METERED_OVERRIDE = "MeteredOverride";
        public static final String XML_TAG_NO_INTERNET_ACCESS_EXPECTED = "NoInternetAccessExpected";
        public static final String XML_TAG_NUM_ASSOCIATION = "NumAssociation";
        public static final String XML_TAG_PRE_SHARED_KEY = "PreSharedKey";
        public static final String XML_TAG_PRIORITY = "Priority";
        public static final String XML_TAG_PROVIDER_FRIENDLY_NAME = "ProviderFriendlyName";
        public static final String XML_TAG_RANDOMIZED_MAC_ADDRESS = "RandomizedMacAddress";
        public static final String XML_TAG_REQUIRE_PMF = "RequirePMF";
        public static final String XML_TAG_ROAMING_CONSORTIUM_OIS = "RoamingConsortiumOIs";
        public static final String XML_TAG_SHARED = "Shared";
        public static final String XML_TAG_SIM_SLOT = "SimSlot";
        public static final String XML_TAG_SSID = "SSID";
        public static final String XML_TAG_STATUS = "Status";
        public static final String XML_TAG_USER_APPROVED = "UserApproved";
        public static final String XML_TAG_USE_EXTERNAL_SCORES = "UseExternalScores";
        public static final String XML_TAG_VALIDATED_INTERNET_ACCESS = "ValidatedInternetAccess";
        public static final String XML_TAG_WAPI_CERT_SEL = "WapiCertSel";
        public static final String XML_TAG_WAPI_CERT_SEL_MODE = "WapiCertSelMode";
        public static final String XML_TAG_WAPI_PSK = "WapiPsk";
        public static final String XML_TAG_WAPI_PSK_TYPE = "WapiPskType";
        public static final String XML_TAG_WEP_KEYS = "WEPKeys";
        public static final String XML_TAG_WEP_TX_KEY_INDEX = "WEPTxKeyIndex";

        private static void writeWepKeysToXml(XmlSerializer xmlSerializer, String[] strArr) throws XmlPullParserException, IOException {
            String[] strArr2 = new String[strArr.length];
            boolean z = false;
            for (int i = 0; i < strArr.length; i++) {
                if (strArr[i] == null) {
                    strArr2[i] = new String();
                } else {
                    strArr2[i] = strArr[i];
                    z = true;
                }
            }
            if (z) {
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WEP_KEYS, strArr2);
            } else {
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WEP_KEYS, null);
            }
        }

        public static void writeCommonElementsToXml(XmlSerializer xmlSerializer, WifiConfiguration wifiConfiguration) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CONFIG_KEY, wifiConfiguration.configKey());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SSID, wifiConfiguration.SSID);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_BSSID, wifiConfiguration.BSSID);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PRE_SHARED_KEY, wifiConfiguration.preSharedKey);
            writeWepKeysToXml(xmlSerializer, wifiConfiguration.wepKeys);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WEP_TX_KEY_INDEX, Integer.valueOf(wifiConfiguration.wepTxKeyIndex));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_HIDDEN_SSID, Boolean.valueOf(wifiConfiguration.hiddenSSID));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_REQUIRE_PMF, Boolean.valueOf(wifiConfiguration.requirePMF));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ALLOWED_KEY_MGMT, wifiConfiguration.allowedKeyManagement.toByteArray());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ALLOWED_PROTOCOLS, wifiConfiguration.allowedProtocols.toByteArray());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ALLOWED_AUTH_ALGOS, wifiConfiguration.allowedAuthAlgorithms.toByteArray());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ALLOWED_GROUP_CIPHERS, wifiConfiguration.allowedGroupCiphers.toByteArray());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ALLOWED_PAIRWISE_CIPHERS, wifiConfiguration.allowedPairwiseCiphers.toByteArray());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SHARED, Boolean.valueOf(wifiConfiguration.shared));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WAPI_CERT_SEL_MODE, Integer.valueOf(wifiConfiguration.wapiCertSelMode));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WAPI_CERT_SEL, wifiConfiguration.wapiCertSel);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WAPI_PSK_TYPE, Integer.valueOf(wifiConfiguration.wapiPskType));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_WAPI_PSK, wifiConfiguration.wapiPsk);
        }

        public static void writeToXmlForBackup(XmlSerializer xmlSerializer, WifiConfiguration wifiConfiguration) throws XmlPullParserException, IOException {
            writeCommonElementsToXml(xmlSerializer, wifiConfiguration);
        }

        public static void writeToXmlForConfigStore(XmlSerializer xmlSerializer, WifiConfiguration wifiConfiguration) throws XmlPullParserException, IOException {
            writeCommonElementsToXml(xmlSerializer, wifiConfiguration);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_STATUS, Integer.valueOf(wifiConfiguration.status));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_FQDN, wifiConfiguration.FQDN);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROVIDER_FRIENDLY_NAME, wifiConfiguration.providerFriendlyName);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LINKED_NETWORKS_LIST, wifiConfiguration.linkedConfigurations);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_DEFAULT_GW_MAC_ADDRESS, wifiConfiguration.defaultGwMacAddress);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_VALIDATED_INTERNET_ACCESS, Boolean.valueOf(wifiConfiguration.validatedInternetAccess));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_NO_INTERNET_ACCESS_EXPECTED, Boolean.valueOf(wifiConfiguration.noInternetAccessExpected));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USER_APPROVED, Integer.valueOf(wifiConfiguration.userApproved));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_METERED_HINT, Boolean.valueOf(wifiConfiguration.meteredHint));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_METERED_OVERRIDE, Integer.valueOf(wifiConfiguration.meteredOverride));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USE_EXTERNAL_SCORES, Boolean.valueOf(wifiConfiguration.useExternalScores));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_NUM_ASSOCIATION, Integer.valueOf(wifiConfiguration.numAssociation));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CREATOR_UID, Integer.valueOf(wifiConfiguration.creatorUid));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CREATOR_NAME, wifiConfiguration.creatorName);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CREATION_TIME, wifiConfiguration.creationTime);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LAST_UPDATE_UID, Integer.valueOf(wifiConfiguration.lastUpdateUid));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LAST_UPDATE_NAME, wifiConfiguration.lastUpdateName);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LAST_CONNECT_UID, Integer.valueOf(wifiConfiguration.lastConnectUid));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_IS_LEGACY_PASSPOINT_CONFIG, Boolean.valueOf(wifiConfiguration.isLegacyPasspointConfig));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ROAMING_CONSORTIUM_OIS, wifiConfiguration.roamingConsortiumIds);
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_RANDOMIZED_MAC_ADDRESS, wifiConfiguration.getRandomizedMacAddress().toString());
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

        public static Pair<String, WifiConfiguration> parseFromXml(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            String str = null;
            while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                String[] strArr = new String[1];
                Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                if (strArr[0] == null) {
                    throw new XmlPullParserException("Missing value name");
                }
                switch (strArr[0]) {
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
                    case "Status":
                        int iIntValue = ((Integer) currentValue).intValue();
                        if (iIntValue == 0) {
                            iIntValue = 2;
                        }
                        wifiConfiguration.status = iIntValue;
                        break;
                    case "FQDN":
                        wifiConfiguration.FQDN = (String) currentValue;
                        break;
                    case "ProviderFriendlyName":
                        wifiConfiguration.providerFriendlyName = (String) currentValue;
                        break;
                    case "LinkedNetworksList":
                        wifiConfiguration.linkedConfigurations = (HashMap) currentValue;
                        break;
                    case "DefaultGwMacAddress":
                        wifiConfiguration.defaultGwMacAddress = (String) currentValue;
                        break;
                    case "ValidatedInternetAccess":
                        wifiConfiguration.validatedInternetAccess = ((Boolean) currentValue).booleanValue();
                        break;
                    case "NoInternetAccessExpected":
                        wifiConfiguration.noInternetAccessExpected = ((Boolean) currentValue).booleanValue();
                        break;
                    case "UserApproved":
                        wifiConfiguration.userApproved = ((Integer) currentValue).intValue();
                        break;
                    case "MeteredHint":
                        wifiConfiguration.meteredHint = ((Boolean) currentValue).booleanValue();
                        break;
                    case "MeteredOverride":
                        wifiConfiguration.meteredOverride = ((Integer) currentValue).intValue();
                        break;
                    case "UseExternalScores":
                        wifiConfiguration.useExternalScores = ((Boolean) currentValue).booleanValue();
                        break;
                    case "NumAssociation":
                        wifiConfiguration.numAssociation = ((Integer) currentValue).intValue();
                        break;
                    case "CreatorUid":
                        wifiConfiguration.creatorUid = ((Integer) currentValue).intValue();
                        break;
                    case "CreatorName":
                        wifiConfiguration.creatorName = (String) currentValue;
                        break;
                    case "CreationTime":
                        wifiConfiguration.creationTime = (String) currentValue;
                        break;
                    case "LastUpdateUid":
                        wifiConfiguration.lastUpdateUid = ((Integer) currentValue).intValue();
                        break;
                    case "LastUpdateName":
                        wifiConfiguration.lastUpdateName = (String) currentValue;
                        break;
                    case "LastConnectUid":
                        wifiConfiguration.lastConnectUid = ((Integer) currentValue).intValue();
                        break;
                    case "IsLegacyPasspointConfig":
                        wifiConfiguration.isLegacyPasspointConfig = ((Boolean) currentValue).booleanValue();
                        break;
                    case "RoamingConsortiumOIs":
                        wifiConfiguration.roamingConsortiumIds = (long[]) currentValue;
                        break;
                    case "RandomizedMacAddress":
                        wifiConfiguration.setRandomizedMacAddress(MacAddress.fromString((String) currentValue));
                        break;
                    case "SimSlot":
                        String unused = XmlUtil.mSimSlot = (String) currentValue;
                        break;
                    case "WapiCertSelMode":
                        wifiConfiguration.wapiCertSelMode = ((Integer) currentValue).intValue();
                        break;
                    case "WapiCertSel":
                        wifiConfiguration.wapiCertSel = (String) currentValue;
                        break;
                    case "WapiPskType":
                        wifiConfiguration.wapiPskType = ((Integer) currentValue).intValue();
                        break;
                    case "WapiPsk":
                        String str2 = (String) currentValue;
                        wifiConfiguration.wapiPsk = str2;
                        if (wifiConfiguration.wapiPsk == null) {
                            break;
                        } else {
                            wifiConfiguration.preSharedKey = str2;
                            break;
                        }
                        break;
                    case "Aliases":
                        wifiConfiguration.wapiCertSel = (String) currentValue;
                        break;
                    case "Priority":
                        break;
                    default:
                        throw new XmlPullParserException("Unknown value name found: " + strArr[0]);
                }
            }
            return Pair.create(str, wifiConfiguration);
        }
    }

    public static class IpConfigurationXmlUtil {
        public static final String XML_TAG_DNS_SERVER_ADDRESSES = "DNSServers";
        public static final String XML_TAG_GATEWAY_ADDRESS = "GatewayAddress";
        public static final String XML_TAG_IP_ASSIGNMENT = "IpAssignment";
        public static final String XML_TAG_LINK_ADDRESS = "LinkAddress";
        public static final String XML_TAG_LINK_PREFIX_LENGTH = "LinkPrefixLength";
        public static final String XML_TAG_PROXY_EXCLUSION_LIST = "ProxyExclusionList";
        public static final String XML_TAG_PROXY_HOST = "ProxyHost";
        public static final String XML_TAG_PROXY_PAC_FILE = "ProxyPac";
        public static final String XML_TAG_PROXY_PORT = "ProxyPort";
        public static final String XML_TAG_PROXY_SETTINGS = "ProxySettings";

        private static void writeStaticIpConfigurationToXml(XmlSerializer xmlSerializer, StaticIpConfiguration staticIpConfiguration) throws XmlPullParserException, IOException {
            if (staticIpConfiguration.ipAddress != null) {
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LINK_ADDRESS, staticIpConfiguration.ipAddress.getAddress().getHostAddress());
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LINK_PREFIX_LENGTH, Integer.valueOf(staticIpConfiguration.ipAddress.getPrefixLength()));
            } else {
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LINK_ADDRESS, null);
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_LINK_PREFIX_LENGTH, null);
            }
            if (staticIpConfiguration.gateway != null) {
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_GATEWAY_ADDRESS, staticIpConfiguration.gateway.getHostAddress());
            } else {
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_GATEWAY_ADDRESS, null);
            }
            if (staticIpConfiguration.dnsServers != null) {
                String[] strArr = new String[staticIpConfiguration.dnsServers.size()];
                int i = 0;
                Iterator it = staticIpConfiguration.dnsServers.iterator();
                while (it.hasNext()) {
                    strArr[i] = ((InetAddress) it.next()).getHostAddress();
                    i++;
                }
                XmlUtil.writeNextValue(xmlSerializer, XML_TAG_DNS_SERVER_ADDRESSES, strArr);
                return;
            }
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_DNS_SERVER_ADDRESSES, null);
        }

        public static void writeToXml(XmlSerializer xmlSerializer, IpConfiguration ipConfiguration) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_IP_ASSIGNMENT, ipConfiguration.ipAssignment.toString());
            if (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipConfiguration.ipAssignment.ordinal()] == 1) {
                writeStaticIpConfigurationToXml(xmlSerializer, ipConfiguration.getStaticIpConfiguration());
            }
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROXY_SETTINGS, ipConfiguration.proxySettings.toString());
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[ipConfiguration.proxySettings.ordinal()]) {
                case 1:
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROXY_HOST, ipConfiguration.httpProxy.getHost());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROXY_PORT, Integer.valueOf(ipConfiguration.httpProxy.getPort()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROXY_EXCLUSION_LIST, ipConfiguration.httpProxy.getExclusionListAsString());
                    break;
                case 2:
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROXY_PAC_FILE, ipConfiguration.httpProxy.getPacFileUrl().toString());
                    break;
            }
        }

        private static StaticIpConfiguration parseStaticIpConfigurationFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            String str = (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_LINK_ADDRESS);
            Integer num = (Integer) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_LINK_PREFIX_LENGTH);
            if (str != null && num != null) {
                LinkAddress linkAddress = new LinkAddress(NetworkUtils.numericToInetAddress(str), num.intValue());
                if (linkAddress.getAddress() instanceof Inet4Address) {
                    staticIpConfiguration.ipAddress = linkAddress;
                } else {
                    Log.w(XmlUtil.TAG, "Non-IPv4 address: " + linkAddress);
                }
            }
            String str2 = (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_GATEWAY_ADDRESS);
            if (str2 != null) {
                InetAddress inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(str2);
                RouteInfo routeInfo = new RouteInfo(null, inetAddressNumericToInetAddress);
                if (routeInfo.isIPv4Default()) {
                    staticIpConfiguration.gateway = inetAddressNumericToInetAddress;
                } else {
                    Log.w(XmlUtil.TAG, "Non-IPv4 default route: " + routeInfo);
                }
            }
            String[] strArr = (String[]) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_DNS_SERVER_ADDRESSES);
            if (strArr != null) {
                for (String str3 : strArr) {
                    staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(str3));
                }
            }
            return staticIpConfiguration;
        }

        public static IpConfiguration parseFromXml(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
            IpConfiguration ipConfiguration = new IpConfiguration();
            IpConfiguration.IpAssignment ipAssignmentValueOf = IpConfiguration.IpAssignment.valueOf((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_IP_ASSIGNMENT));
            ipConfiguration.setIpAssignment(ipAssignmentValueOf);
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipAssignmentValueOf.ordinal()]) {
                case 1:
                    ipConfiguration.setStaticIpConfiguration(parseStaticIpConfigurationFromXml(xmlPullParser));
                    break;
                case 2:
                case 3:
                    break;
                default:
                    throw new XmlPullParserException("Unknown ip assignment type: " + ipAssignmentValueOf);
            }
            IpConfiguration.ProxySettings proxySettingsValueOf = IpConfiguration.ProxySettings.valueOf((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_SETTINGS));
            ipConfiguration.setProxySettings(proxySettingsValueOf);
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[proxySettingsValueOf.ordinal()]) {
                case 1:
                    ipConfiguration.setHttpProxy(new ProxyInfo((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_HOST), ((Integer) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_PORT)).intValue(), (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_EXCLUSION_LIST)));
                    return ipConfiguration;
                case 2:
                    ipConfiguration.setHttpProxy(new ProxyInfo((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_PAC_FILE)));
                    return ipConfiguration;
                case 3:
                case 4:
                    return ipConfiguration;
                default:
                    throw new XmlPullParserException("Unknown proxy settings type: " + proxySettingsValueOf);
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

    public static class NetworkSelectionStatusXmlUtil {
        public static final String XML_TAG_CONNECT_CHOICE = "ConnectChoice";
        public static final String XML_TAG_CONNECT_CHOICE_TIMESTAMP = "ConnectChoiceTimeStamp";
        public static final String XML_TAG_DISABLE_REASON = "DisableReason";
        public static final String XML_TAG_HAS_EVER_CONNECTED = "HasEverConnected";
        public static final String XML_TAG_SELECTION_STATUS = "SelectionStatus";

        public static void writeToXml(XmlSerializer xmlSerializer, WifiConfiguration.NetworkSelectionStatus networkSelectionStatus) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SELECTION_STATUS, networkSelectionStatus.getNetworkStatusString());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_DISABLE_REASON, networkSelectionStatus.getNetworkDisableReasonString());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CONNECT_CHOICE, networkSelectionStatus.getConnectChoice());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CONNECT_CHOICE_TIMESTAMP, Long.valueOf(networkSelectionStatus.getConnectChoiceTimestamp()));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_HAS_EVER_CONNECTED, Boolean.valueOf(networkSelectionStatus.getHasEverConnected()));
        }

        public static WifiConfiguration.NetworkSelectionStatus parseFromXml(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
            WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = new WifiConfiguration.NetworkSelectionStatus();
            String str = "";
            String str2 = "";
            while (true) {
                if (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                    String[] strArr = new String[1];
                    Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                    if (strArr[0] == null) {
                        throw new XmlPullParserException("Missing value name");
                    }
                    switch (strArr[0]) {
                        case "SelectionStatus":
                            str = (String) currentValue;
                            break;
                        case "DisableReason":
                            str2 = (String) currentValue;
                            break;
                        case "ConnectChoice":
                            networkSelectionStatus.setConnectChoice((String) currentValue);
                            break;
                        case "ConnectChoiceTimeStamp":
                            networkSelectionStatus.setConnectChoiceTimestamp(((Long) currentValue).longValue());
                            break;
                        case "HasEverConnected":
                            networkSelectionStatus.setHasEverConnected(((Boolean) currentValue).booleanValue());
                            break;
                        default:
                            throw new XmlPullParserException("Unknown value name found: " + strArr[0]);
                    }
                } else {
                    int iIndexOf = Arrays.asList(WifiConfiguration.NetworkSelectionStatus.QUALITY_NETWORK_SELECTION_STATUS).indexOf(str);
                    int iIndexOf2 = Arrays.asList(WifiConfiguration.NetworkSelectionStatus.QUALITY_NETWORK_SELECTION_DISABLE_REASON).indexOf(str2);
                    if (iIndexOf == -1 || iIndexOf2 == -1 || iIndexOf == 1) {
                        iIndexOf = 0;
                        iIndexOf2 = 0;
                    }
                    networkSelectionStatus.setNetworkSelectionStatus(iIndexOf);
                    networkSelectionStatus.setNetworkSelectionDisableReason(iIndexOf2);
                    return networkSelectionStatus;
                }
            }
        }
    }

    public static class WifiEnterpriseConfigXmlUtil {
        public static final String XML_TAG_ALT_SUBJECT_MATCH = "AltSubjectMatch";
        public static final String XML_TAG_ANON_IDENTITY = "AnonIdentity";
        public static final String XML_TAG_CA_CERT = "CaCert";
        public static final String XML_TAG_CA_PATH = "CaPath";
        public static final String XML_TAG_CLIENT_CERT = "ClientCert";
        public static final String XML_TAG_DOM_SUFFIX_MATCH = "DomSuffixMatch";
        public static final String XML_TAG_EAP_METHOD = "EapMethod";
        public static final String XML_TAG_ENGINE = "Engine";
        public static final String XML_TAG_ENGINE_ID = "EngineId";
        public static final String XML_TAG_IDENTITY = "Identity";
        public static final String XML_TAG_PASSWORD = "Password";
        public static final String XML_TAG_PHASE2_METHOD = "Phase2Method";
        public static final String XML_TAG_PLMN = "PLMN";
        public static final String XML_TAG_PRIVATE_KEY_ID = "PrivateKeyId";
        public static final String XML_TAG_REALM = "Realm";
        public static final String XML_TAG_SIMNUM = "SimNum";
        public static final String XML_TAG_SUBJECT_MATCH = "SubjectMatch";

        public static void writeToXml(XmlSerializer xmlSerializer, WifiEnterpriseConfig wifiEnterpriseConfig) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_IDENTITY, wifiEnterpriseConfig.getFieldValue("identity"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ANON_IDENTITY, wifiEnterpriseConfig.getFieldValue("anonymous_identity"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PASSWORD, wifiEnterpriseConfig.getFieldValue("password"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CLIENT_CERT, wifiEnterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CA_CERT, wifiEnterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SUBJECT_MATCH, wifiEnterpriseConfig.getFieldValue("subject_match"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ENGINE, wifiEnterpriseConfig.getFieldValue("engine"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ENGINE_ID, wifiEnterpriseConfig.getFieldValue("engine_id"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PRIVATE_KEY_ID, wifiEnterpriseConfig.getFieldValue("key_id"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ALT_SUBJECT_MATCH, wifiEnterpriseConfig.getFieldValue("altsubject_match"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_DOM_SUFFIX_MATCH, wifiEnterpriseConfig.getFieldValue("domain_suffix_match"));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CA_PATH, wifiEnterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_EAP_METHOD, Integer.valueOf(wifiEnterpriseConfig.getEapMethod()));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PHASE2_METHOD, Integer.valueOf(wifiEnterpriseConfig.getPhase2Method()));
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PLMN, wifiEnterpriseConfig.getPlmn());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_REALM, wifiEnterpriseConfig.getRealm());
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SIMNUM, wifiEnterpriseConfig.getSimNum());
        }

        public static WifiEnterpriseConfig parseFromXml(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
            WifiEnterpriseConfig wifiEnterpriseConfig = new WifiEnterpriseConfig();
            while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                String[] strArr = new String[1];
                Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                if (strArr[0] == null) {
                    throw new XmlPullParserException("Missing value name");
                }
                switch (strArr[0]) {
                    case "Identity":
                        wifiEnterpriseConfig.setFieldValue("identity", (String) currentValue);
                        break;
                    case "AnonIdentity":
                        wifiEnterpriseConfig.setFieldValue("anonymous_identity", (String) currentValue);
                        break;
                    case "Password":
                        wifiEnterpriseConfig.setFieldValue("password", (String) currentValue);
                        break;
                    case "ClientCert":
                        wifiEnterpriseConfig.setFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT, (String) currentValue);
                        break;
                    case "CaCert":
                        wifiEnterpriseConfig.setFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT, (String) currentValue);
                        break;
                    case "SubjectMatch":
                        wifiEnterpriseConfig.setFieldValue("subject_match", (String) currentValue);
                        break;
                    case "Engine":
                        wifiEnterpriseConfig.setFieldValue("engine", (String) currentValue);
                        break;
                    case "EngineId":
                        wifiEnterpriseConfig.setFieldValue("engine_id", (String) currentValue);
                        break;
                    case "PrivateKeyId":
                        wifiEnterpriseConfig.setFieldValue("key_id", (String) currentValue);
                        break;
                    case "AltSubjectMatch":
                        wifiEnterpriseConfig.setFieldValue("altsubject_match", (String) currentValue);
                        break;
                    case "DomSuffixMatch":
                        wifiEnterpriseConfig.setFieldValue("domain_suffix_match", (String) currentValue);
                        break;
                    case "CaPath":
                        wifiEnterpriseConfig.setFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH, (String) currentValue);
                        break;
                    case "EapMethod":
                        wifiEnterpriseConfig.setEapMethod(((Integer) currentValue).intValue());
                        break;
                    case "Phase2Method":
                        wifiEnterpriseConfig.setPhase2Method(((Integer) currentValue).intValue());
                        break;
                    case "PLMN":
                        wifiEnterpriseConfig.setPlmn((String) currentValue);
                        break;
                    case "Realm":
                        wifiEnterpriseConfig.setRealm((String) currentValue);
                        break;
                    case "SimNum":
                        wifiEnterpriseConfig.setSimNum((String) currentValue);
                        break;
                    default:
                        throw new XmlPullParserException("Unknown value name found: " + strArr[0]);
                }
            }
            if (XmlUtil.mSimSlot != null) {
                wifiEnterpriseConfig.setSimNum(XmlUtil.mSimSlot);
            }
            return wifiEnterpriseConfig;
        }
    }
}
