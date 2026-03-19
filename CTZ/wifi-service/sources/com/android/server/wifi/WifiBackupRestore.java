package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.XmlUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WifiBackupRestore {
    private static final float CURRENT_BACKUP_DATA_VERSION = 1.0f;
    private static final int INITIAL_BACKUP_DATA_VERSION = 1;
    private static final String PSK_MASK_LINE_MATCH_PATTERN = "<.*PreSharedKey.*>.*<.*>";
    private static final String PSK_MASK_REPLACE_PATTERN = "$1*$3";
    private static final String PSK_MASK_SEARCH_PATTERN = "(<.*PreSharedKey.*>)(.*)(<.*>)";
    private static final String TAG = "WifiBackupRestore";
    private static final String WEP_KEYS_MASK_LINE_END_MATCH_PATTERN = "</string-array>";
    private static final String WEP_KEYS_MASK_LINE_START_MATCH_PATTERN = "<string-array.*WEPKeys.*num=\"[0-9]\">";
    private static final String WEP_KEYS_MASK_REPLACE_PATTERN = "$1*$3";
    private static final String WEP_KEYS_MASK_SEARCH_PATTERN = "(<.*=)(.*)(/>)";
    private static final String XML_TAG_DOCUMENT_HEADER = "WifiBackupData";
    static final String XML_TAG_SECTION_HEADER_IP_CONFIGURATION = "IpConfiguration";
    static final String XML_TAG_SECTION_HEADER_NETWORK = "Network";
    static final String XML_TAG_SECTION_HEADER_NETWORK_LIST = "NetworkList";
    static final String XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION = "WifiConfiguration";
    private static final String XML_TAG_VERSION = "Version";
    private byte[] mDebugLastBackupDataRestored;
    private byte[] mDebugLastBackupDataRetrieved;
    private byte[] mDebugLastSupplicantBackupDataRestored;
    private boolean mVerboseLoggingEnabled = false;
    private final WifiPermissionsUtil mWifiPermissionsUtil;

    public WifiBackupRestore(WifiPermissionsUtil wifiPermissionsUtil) {
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
    }

    public byte[] retrieveBackupDataFromConfigurations(List<WifiConfiguration> list) {
        if (list == null) {
            Log.e(TAG, "Invalid configuration list received");
            return new byte[0];
        }
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
            XmlUtil.writeDocumentStart(fastXmlSerializer, XML_TAG_DOCUMENT_HEADER);
            XmlUtil.writeNextValue(fastXmlSerializer, XML_TAG_VERSION, Float.valueOf(CURRENT_BACKUP_DATA_VERSION));
            writeNetworkConfigurationsToXml(fastXmlSerializer, list);
            XmlUtil.writeDocumentEnd(fastXmlSerializer, XML_TAG_DOCUMENT_HEADER);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            if (this.mVerboseLoggingEnabled) {
                this.mDebugLastBackupDataRetrieved = byteArray;
            }
            return byteArray;
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving the backup data: " + e);
            return new byte[0];
        } catch (XmlPullParserException e2) {
            Log.e(TAG, "Error retrieving the backup data: " + e2);
            return new byte[0];
        }
    }

    private void writeNetworkConfigurationsToXml(XmlSerializer xmlSerializer, List<WifiConfiguration> list) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK_LIST);
        for (WifiConfiguration wifiConfiguration : list) {
            if (!wifiConfiguration.isEnterprise() && !wifiConfiguration.isPasspoint()) {
                if (!this.mWifiPermissionsUtil.checkConfigOverridePermission(wifiConfiguration.creatorUid)) {
                    Log.d(TAG, "Ignoring network from an app with no config override permission: " + wifiConfiguration.configKey());
                } else {
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK);
                    writeNetworkConfigurationToXml(xmlSerializer, wifiConfiguration);
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK);
                }
            }
        }
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK_LIST);
    }

    private void writeNetworkConfigurationToXml(XmlSerializer xmlSerializer, WifiConfiguration wifiConfiguration) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.WifiConfigurationXmlUtil.writeToXmlForBackup(xmlSerializer, wifiConfiguration);
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        XmlUtil.IpConfigurationXmlUtil.writeToXml(xmlSerializer, wifiConfiguration.getIpConfiguration());
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
    }

    public List<WifiConfiguration> retrieveConfigurationsFromBackupData(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            Log.e(TAG, "Invalid backup data received");
            return null;
        }
        try {
            if (this.mVerboseLoggingEnabled) {
                this.mDebugLastBackupDataRestored = bArr;
            }
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(new ByteArrayInputStream(bArr), StandardCharsets.UTF_8.name());
            XmlUtil.gotoDocumentStart(xmlPullParserNewPullParser, XML_TAG_DOCUMENT_HEADER);
            int depth = xmlPullParserNewPullParser.getDepth();
            int i = 1;
            int i2 = 0;
            try {
                String string = new Float(((Float) XmlUtil.readNextValueWithName(xmlPullParserNewPullParser, XML_TAG_VERSION)).floatValue()).toString();
                int iIndexOf = string.indexOf(46);
                if (iIndexOf == -1) {
                    i = Integer.parseInt(string);
                } else {
                    int i3 = Integer.parseInt(string.substring(0, iIndexOf));
                    i2 = Integer.parseInt(string.substring(iIndexOf + 1));
                    i = i3;
                }
            } catch (ClassCastException e) {
            }
            Log.d(TAG, "Version of backup data - major: " + i + "; minor: " + i2);
            WifiBackupDataParser wifiBackupDataParser = getWifiBackupDataParser(i);
            if (wifiBackupDataParser == null) {
                Log.w(TAG, "Major version of backup data is unknown to this Android version; not restoring");
                return null;
            }
            return wifiBackupDataParser.parseNetworkConfigurationsFromXml(xmlPullParserNewPullParser, depth, i2);
        } catch (IOException | ClassCastException | IllegalArgumentException | XmlPullParserException e2) {
            Log.e(TAG, "Error parsing the backup data: " + e2);
            return null;
        }
    }

    private WifiBackupDataParser getWifiBackupDataParser(int i) {
        if (i == 1) {
            return new WifiBackupDataV1Parser();
        }
        Log.e(TAG, "Unrecognized majorVersion of backup data: " + i);
        return null;
    }

    private String createLogFromBackupData(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        try {
            String[] strArrSplit = new String(bArr, StandardCharsets.UTF_8.name()).split("\n");
            int length = strArrSplit.length;
            boolean z = false;
            for (int i = 0; i < length; i++) {
                String strReplaceAll = strArrSplit[i];
                if (strReplaceAll.matches(PSK_MASK_LINE_MATCH_PATTERN)) {
                    strReplaceAll = strReplaceAll.replaceAll(PSK_MASK_SEARCH_PATTERN, "$1*$3");
                }
                if (!strReplaceAll.matches(WEP_KEYS_MASK_LINE_START_MATCH_PATTERN)) {
                    if (!strReplaceAll.matches(WEP_KEYS_MASK_LINE_END_MATCH_PATTERN)) {
                        if (z) {
                            strReplaceAll = strReplaceAll.replaceAll(WEP_KEYS_MASK_SEARCH_PATTERN, "$1*$3");
                        }
                    } else {
                        z = false;
                    }
                } else {
                    z = true;
                }
                sb.append(strReplaceAll);
                sb.append("\n");
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public List<WifiConfiguration> retrieveConfigurationsFromSupplicantBackupData(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr.length == 0) {
            Log.e(TAG, "Invalid supplicant backup data received");
            return null;
        }
        if (this.mVerboseLoggingEnabled) {
            this.mDebugLastSupplicantBackupDataRestored = bArr;
        }
        SupplicantBackupMigration.SupplicantNetworks supplicantNetworks = new SupplicantBackupMigration.SupplicantNetworks();
        char[] cArr = new char[bArr.length];
        for (int i = 0; i < bArr.length; i++) {
            cArr[i] = (char) bArr[i];
        }
        supplicantNetworks.readNetworksFromStream(new BufferedReader(new CharArrayReader(cArr)));
        List<WifiConfiguration> listRetrieveWifiConfigurations = supplicantNetworks.retrieveWifiConfigurations();
        if (bArr2 != null && bArr2.length != 0) {
            SparseArray ipAndProxyConfigurations = IpConfigStore.readIpAndProxyConfigurations(new ByteArrayInputStream(bArr2));
            if (ipAndProxyConfigurations != null) {
                for (int i2 = 0; i2 < ipAndProxyConfigurations.size(); i2++) {
                    int iKeyAt = ipAndProxyConfigurations.keyAt(i2);
                    for (WifiConfiguration wifiConfiguration : listRetrieveWifiConfigurations) {
                        if (wifiConfiguration.configKey().hashCode() == iKeyAt) {
                            wifiConfiguration.setIpConfiguration((IpConfiguration) ipAndProxyConfigurations.valueAt(i2));
                        }
                    }
                }
            } else {
                Log.e(TAG, "Failed to parse ipconfig data");
            }
        } else {
            Log.e(TAG, "Invalid ipconfig backup data received");
        }
        return listRetrieveWifiConfigurations;
    }

    public void enableVerboseLogging(int i) {
        this.mVerboseLoggingEnabled = i > 0;
        if (!this.mVerboseLoggingEnabled) {
            this.mDebugLastBackupDataRetrieved = null;
            this.mDebugLastBackupDataRestored = null;
            this.mDebugLastSupplicantBackupDataRestored = null;
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Dump of WifiBackupRestore");
        if (this.mDebugLastBackupDataRetrieved != null) {
            printWriter.println("Last backup data retrieved: " + createLogFromBackupData(this.mDebugLastBackupDataRetrieved));
        }
        if (this.mDebugLastBackupDataRestored != null) {
            printWriter.println("Last backup data restored: " + createLogFromBackupData(this.mDebugLastBackupDataRestored));
        }
        if (this.mDebugLastSupplicantBackupDataRestored != null) {
            printWriter.println("Last old backup data restored: " + SupplicantBackupMigration.createLogFromBackupData(this.mDebugLastSupplicantBackupDataRestored));
        }
    }

    public static class SupplicantBackupMigration {
        private static final String PSK_MASK_LINE_MATCH_PATTERN = ".*psk.*=.*";
        private static final String PSK_MASK_REPLACE_PATTERN = "$1*";
        private static final String PSK_MASK_SEARCH_PATTERN = "(.*psk.*=)(.*)";
        public static final String SUPPLICANT_KEY_CA_CERT = "ca_cert";
        public static final String SUPPLICANT_KEY_CA_PATH = "ca_path";
        public static final String SUPPLICANT_KEY_CLIENT_CERT = "client_cert";
        public static final String SUPPLICANT_KEY_EAP = "eap";
        public static final String SUPPLICANT_KEY_HIDDEN = "scan_ssid";
        public static final String SUPPLICANT_KEY_ID_STR = "id_str";
        public static final String SUPPLICANT_KEY_KEY_MGMT = "key_mgmt";
        public static final String SUPPLICANT_KEY_PSK = "psk";
        public static final String SUPPLICANT_KEY_SSID = "ssid";
        public static final String SUPPLICANT_KEY_WEP_KEY_IDX = "wep_tx_keyidx";
        private static final String WEP_KEYS_MASK_REPLACE_PATTERN = "$1*";
        public static final String SUPPLICANT_KEY_WEP_KEY0 = WifiConfiguration.wepKeyVarNames[0];
        public static final String SUPPLICANT_KEY_WEP_KEY1 = WifiConfiguration.wepKeyVarNames[1];
        public static final String SUPPLICANT_KEY_WEP_KEY2 = WifiConfiguration.wepKeyVarNames[2];
        public static final String SUPPLICANT_KEY_WEP_KEY3 = WifiConfiguration.wepKeyVarNames[3];
        private static final String WEP_KEYS_MASK_LINE_MATCH_PATTERN = ".*" + SUPPLICANT_KEY_WEP_KEY0.replace("0", "") + ".*=.*";
        private static final String WEP_KEYS_MASK_SEARCH_PATTERN = "(.*" + SUPPLICANT_KEY_WEP_KEY0.replace("0", "") + ".*=)(.*)";

        public static String createLogFromBackupData(byte[] bArr) {
            StringBuilder sb = new StringBuilder();
            try {
                String[] strArrSplit = new String(bArr, StandardCharsets.UTF_8.name()).split("\n");
                int length = strArrSplit.length;
                for (int i = 0; i < length; i++) {
                    String strReplaceAll = strArrSplit[i];
                    if (strReplaceAll.matches(PSK_MASK_LINE_MATCH_PATTERN)) {
                        strReplaceAll = strReplaceAll.replaceAll(PSK_MASK_SEARCH_PATTERN, "$1*");
                    }
                    if (strReplaceAll.matches(WEP_KEYS_MASK_LINE_MATCH_PATTERN)) {
                        strReplaceAll = strReplaceAll.replaceAll(WEP_KEYS_MASK_SEARCH_PATTERN, "$1*");
                    }
                    sb.append(strReplaceAll);
                    sb.append("\n");
                }
                return sb.toString();
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }

        static class SupplicantNetwork {
            private String mParsedHiddenLine;
            private String mParsedIdStrLine;
            private String mParsedKeyMgmtLine;
            private String mParsedPskLine;
            private String mParsedSSIDLine;
            private String mParsedWepTxKeyIdxLine;
            private String[] mParsedWepKeyLines = new String[4];
            public boolean certUsed = false;
            public boolean isEap = false;

            SupplicantNetwork() {
            }

            public static SupplicantNetwork readNetworkFromStream(BufferedReader bufferedReader) {
                String line;
                SupplicantNetwork supplicantNetwork = new SupplicantNetwork();
                while (bufferedReader.ready() && (line = bufferedReader.readLine()) != null && !line.startsWith("}")) {
                    try {
                        supplicantNetwork.parseLine(line);
                    } catch (IOException e) {
                        return null;
                    }
                }
                return supplicantNetwork;
            }

            void parseLine(String str) {
                String strTrim = str.trim();
                if (strTrim.isEmpty()) {
                    return;
                }
                if (strTrim.startsWith("ssid=")) {
                    this.mParsedSSIDLine = strTrim;
                    return;
                }
                if (strTrim.startsWith("scan_ssid=")) {
                    this.mParsedHiddenLine = strTrim;
                    return;
                }
                if (strTrim.startsWith("key_mgmt=")) {
                    this.mParsedKeyMgmtLine = strTrim;
                    if (strTrim.contains("EAP")) {
                        this.isEap = true;
                        return;
                    }
                    return;
                }
                if (strTrim.startsWith("client_cert=")) {
                    this.certUsed = true;
                    return;
                }
                if (strTrim.startsWith("ca_cert=")) {
                    this.certUsed = true;
                    return;
                }
                if (strTrim.startsWith("ca_path=")) {
                    this.certUsed = true;
                    return;
                }
                if (strTrim.startsWith("eap=")) {
                    this.isEap = true;
                    return;
                }
                if (strTrim.startsWith("psk=")) {
                    this.mParsedPskLine = strTrim;
                    return;
                }
                if (strTrim.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY0 + "=")) {
                    this.mParsedWepKeyLines[0] = strTrim;
                    return;
                }
                if (strTrim.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY1 + "=")) {
                    this.mParsedWepKeyLines[1] = strTrim;
                    return;
                }
                if (strTrim.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY2 + "=")) {
                    this.mParsedWepKeyLines[2] = strTrim;
                    return;
                }
                if (strTrim.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY3 + "=")) {
                    this.mParsedWepKeyLines[3] = strTrim;
                } else if (strTrim.startsWith("wep_tx_keyidx=")) {
                    this.mParsedWepTxKeyIdxLine = strTrim;
                } else if (strTrim.startsWith("id_str=")) {
                    this.mParsedIdStrLine = strTrim;
                }
            }

            public WifiConfiguration createWifiConfiguration() {
                String strSubstring;
                if (this.mParsedSSIDLine == null) {
                    return null;
                }
                WifiConfiguration wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = this.mParsedSSIDLine.substring(this.mParsedSSIDLine.indexOf(61) + 1);
                if (this.mParsedHiddenLine != null) {
                    wifiConfiguration.hiddenSSID = Integer.parseInt(this.mParsedHiddenLine.substring(this.mParsedHiddenLine.indexOf(61) + 1)) != 0;
                }
                if (this.mParsedKeyMgmtLine == null) {
                    wifiConfiguration.allowedKeyManagement.set(1);
                    wifiConfiguration.allowedKeyManagement.set(2);
                } else {
                    for (String str : this.mParsedKeyMgmtLine.substring(this.mParsedKeyMgmtLine.indexOf(61) + 1).split("\\s+")) {
                        if (str.equals("NONE")) {
                            wifiConfiguration.allowedKeyManagement.set(0);
                        } else if (str.equals("WPA-PSK")) {
                            wifiConfiguration.allowedKeyManagement.set(1);
                        } else if (str.equals("WPA-EAP")) {
                            wifiConfiguration.allowedKeyManagement.set(2);
                        } else if (str.equals("IEEE8021X")) {
                            wifiConfiguration.allowedKeyManagement.set(3);
                        }
                    }
                }
                if (this.mParsedPskLine != null) {
                    wifiConfiguration.preSharedKey = this.mParsedPskLine.substring(this.mParsedPskLine.indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[0] != null) {
                    wifiConfiguration.wepKeys[0] = this.mParsedWepKeyLines[0].substring(this.mParsedWepKeyLines[0].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[1] != null) {
                    wifiConfiguration.wepKeys[1] = this.mParsedWepKeyLines[1].substring(this.mParsedWepKeyLines[1].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[2] != null) {
                    wifiConfiguration.wepKeys[2] = this.mParsedWepKeyLines[2].substring(this.mParsedWepKeyLines[2].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[3] != null) {
                    wifiConfiguration.wepKeys[3] = this.mParsedWepKeyLines[3].substring(this.mParsedWepKeyLines[3].indexOf(61) + 1);
                }
                if (this.mParsedWepTxKeyIdxLine != null) {
                    wifiConfiguration.wepTxKeyIndex = Integer.valueOf(this.mParsedWepTxKeyIdxLine.substring(this.mParsedWepTxKeyIdxLine.indexOf(61) + 1)).intValue();
                }
                if (this.mParsedIdStrLine != null && (strSubstring = this.mParsedIdStrLine.substring(this.mParsedIdStrLine.indexOf(61) + 1)) != null) {
                    Map<String, String> networkExtra = SupplicantStaNetworkHal.parseNetworkExtra(NativeUtil.removeEnclosingQuotes(strSubstring));
                    if (networkExtra == null) {
                        Log.e(WifiBackupRestore.TAG, "Error parsing network extras, ignoring network.");
                        return null;
                    }
                    String str2 = networkExtra.get(SupplicantStaNetworkHal.ID_STRING_KEY_CONFIG_KEY);
                    if (str2 == null) {
                        Log.e(WifiBackupRestore.TAG, "Configuration key was not passed, ignoring network.");
                        return null;
                    }
                    if (!str2.equals(wifiConfiguration.configKey())) {
                        Log.w(WifiBackupRestore.TAG, "Configuration key does not match. Retrieved: " + str2 + ", Calculated: " + wifiConfiguration.configKey());
                    }
                    if (Integer.parseInt(networkExtra.get(SupplicantStaNetworkHal.ID_STRING_KEY_CREATOR_UID)) >= 10000) {
                        Log.d(WifiBackupRestore.TAG, "Ignoring network from non-system app: " + wifiConfiguration.configKey());
                        return null;
                    }
                }
                return wifiConfiguration;
            }
        }

        static class SupplicantNetworks {
            final ArrayList<SupplicantNetwork> mNetworks = new ArrayList<>(8);

            SupplicantNetworks() {
            }

            public void readNetworksFromStream(BufferedReader bufferedReader) {
                while (bufferedReader.ready()) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null && line.startsWith("network")) {
                            SupplicantNetwork networkFromStream = SupplicantNetwork.readNetworkFromStream(bufferedReader);
                            if (networkFromStream == null) {
                                Log.e(WifiBackupRestore.TAG, "Error while parsing the network.");
                            } else if (networkFromStream.isEap || networkFromStream.certUsed) {
                                Log.d(WifiBackupRestore.TAG, "Skipping enterprise network for restore: " + networkFromStream.mParsedSSIDLine + " / " + networkFromStream.mParsedKeyMgmtLine);
                            } else {
                                this.mNetworks.add(networkFromStream);
                            }
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            }

            public List<WifiConfiguration> retrieveWifiConfigurations() {
                ArrayList arrayList = new ArrayList();
                Iterator<SupplicantNetwork> it = this.mNetworks.iterator();
                while (it.hasNext()) {
                    try {
                        WifiConfiguration wifiConfigurationCreateWifiConfiguration = it.next().createWifiConfiguration();
                        if (wifiConfigurationCreateWifiConfiguration != null) {
                            Log.v(WifiBackupRestore.TAG, "Parsed Configuration: " + wifiConfigurationCreateWifiConfiguration.configKey());
                            arrayList.add(wifiConfigurationCreateWifiConfiguration);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(WifiBackupRestore.TAG, "Error parsing wifi configuration: " + e);
                        return null;
                    }
                }
                return arrayList;
            }
        }
    }
}
