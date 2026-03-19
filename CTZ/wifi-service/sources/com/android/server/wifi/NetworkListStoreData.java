package com.android.server.wifi;

import android.content.Context;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class NetworkListStoreData implements WifiConfigStore.StoreData {
    private static final String TAG = "NetworkListStoreData";
    private static final String XML_TAG_SECTION_HEADER_IP_CONFIGURATION = "IpConfiguration";
    private static final String XML_TAG_SECTION_HEADER_NETWORK = "Network";
    private static final String XML_TAG_SECTION_HEADER_NETWORK_LIST = "NetworkList";
    private static final String XML_TAG_SECTION_HEADER_NETWORK_STATUS = "NetworkStatus";
    private static final String XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION = "WifiConfiguration";
    private static final String XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION = "WifiEnterpriseConfiguration";
    private final Context mContext;
    private List<WifiConfiguration> mSharedConfigurations;
    private List<WifiConfiguration> mUserConfigurations;

    NetworkListStoreData(Context context) {
        this.mContext = context;
    }

    @Override
    public void serializeData(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        if (z) {
            serializeNetworkList(xmlSerializer, this.mSharedConfigurations);
        } else {
            serializeNetworkList(xmlSerializer, this.mUserConfigurations);
        }
    }

    @Override
    public void deserializeData(XmlPullParser xmlPullParser, int i, boolean z) throws XmlPullParserException, IOException {
        if (xmlPullParser == null) {
            return;
        }
        if (z) {
            this.mSharedConfigurations = parseNetworkList(xmlPullParser, i);
        } else {
            this.mUserConfigurations = parseNetworkList(xmlPullParser, i);
        }
    }

    @Override
    public void resetData(boolean z) {
        if (z) {
            this.mSharedConfigurations = null;
        } else {
            this.mUserConfigurations = null;
        }
    }

    @Override
    public String getName() {
        return XML_TAG_SECTION_HEADER_NETWORK_LIST;
    }

    @Override
    public boolean supportShareData() {
        return true;
    }

    public void setSharedConfigurations(List<WifiConfiguration> list) {
        this.mSharedConfigurations = list;
    }

    public List<WifiConfiguration> getSharedConfigurations() {
        if (this.mSharedConfigurations == null) {
            return new ArrayList();
        }
        return this.mSharedConfigurations;
    }

    public void setUserConfigurations(List<WifiConfiguration> list) {
        this.mUserConfigurations = list;
    }

    public List<WifiConfiguration> getUserConfigurations() {
        if (this.mUserConfigurations == null) {
            return new ArrayList();
        }
        return this.mUserConfigurations;
    }

    private void serializeNetworkList(XmlSerializer xmlSerializer, List<WifiConfiguration> list) throws XmlPullParserException, IOException {
        if (list == null) {
            return;
        }
        Iterator<WifiConfiguration> it = list.iterator();
        while (it.hasNext()) {
            serializeNetwork(xmlSerializer, it.next());
        }
    }

    private void serializeNetwork(XmlSerializer xmlSerializer, WifiConfiguration wifiConfiguration) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK);
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.WifiConfigurationXmlUtil.writeToXmlForConfigStore(xmlSerializer, wifiConfiguration);
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK_STATUS);
        XmlUtil.NetworkSelectionStatusXmlUtil.writeToXml(xmlSerializer, wifiConfiguration.getNetworkSelectionStatus());
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK_STATUS);
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        XmlUtil.IpConfigurationXmlUtil.writeToXml(xmlSerializer, wifiConfiguration.getIpConfiguration());
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        if (wifiConfiguration.enterpriseConfig != null && wifiConfiguration.enterpriseConfig.getEapMethod() != -1) {
            XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION);
            XmlUtil.WifiEnterpriseConfigXmlUtil.writeToXml(xmlSerializer, wifiConfiguration.enterpriseConfig);
            XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION);
        }
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_NETWORK);
    }

    private List<WifiConfiguration> parseNetworkList(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        while (XmlUtil.gotoNextSectionWithNameOrEnd(xmlPullParser, XML_TAG_SECTION_HEADER_NETWORK, i)) {
            try {
                arrayList.add(parseNetwork(xmlPullParser, i + 1));
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to parse network config. Skipping...", e);
            }
        }
        return arrayList;
    }

    private WifiConfiguration parseNetwork(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        byte b;
        String[] strArr = new String[1];
        Pair<String, WifiConfiguration> fromXml = null;
        WifiConfiguration.NetworkSelectionStatus fromXml2 = null;
        IpConfiguration fromXml3 = null;
        WifiEnterpriseConfig fromXml4 = null;
        while (XmlUtil.gotoNextSectionOrEnd(xmlPullParser, strArr, i)) {
            String str = strArr[0];
            int iHashCode = str.hashCode();
            if (iHashCode != -148477024) {
                if (iHashCode != 46473153) {
                    if (iHashCode != 325854959) {
                        b = (iHashCode == 1285464096 && str.equals(XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION)) ? (byte) 3 : (byte) -1;
                    } else if (str.equals(XML_TAG_SECTION_HEADER_IP_CONFIGURATION)) {
                        b = 2;
                    }
                } else if (str.equals(XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION)) {
                    b = 0;
                }
            } else if (str.equals(XML_TAG_SECTION_HEADER_NETWORK_STATUS)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (fromXml != null) {
                        throw new XmlPullParserException("Detected duplicate tag for: WifiConfiguration");
                    }
                    fromXml = XmlUtil.WifiConfigurationXmlUtil.parseFromXml(xmlPullParser, i + 1);
                    break;
                    break;
                case 1:
                    if (fromXml2 != null) {
                        throw new XmlPullParserException("Detected duplicate tag for: NetworkStatus");
                    }
                    fromXml2 = XmlUtil.NetworkSelectionStatusXmlUtil.parseFromXml(xmlPullParser, i + 1);
                    break;
                    break;
                case 2:
                    if (fromXml3 != null) {
                        throw new XmlPullParserException("Detected duplicate tag for: IpConfiguration");
                    }
                    fromXml3 = XmlUtil.IpConfigurationXmlUtil.parseFromXml(xmlPullParser, i + 1);
                    break;
                    break;
                case 3:
                    if (fromXml4 != null) {
                        throw new XmlPullParserException("Detected duplicate tag for: WifiEnterpriseConfiguration");
                    }
                    fromXml4 = XmlUtil.WifiEnterpriseConfigXmlUtil.parseFromXml(xmlPullParser, i + 1);
                    break;
                    break;
                default:
                    throw new XmlPullParserException("Unknown tag under Network: " + strArr[0]);
            }
        }
        if (fromXml == null || fromXml.first == null || fromXml.second == null) {
            throw new XmlPullParserException("XML parsing of wifi configuration failed");
        }
        String str2 = (String) fromXml.first;
        WifiConfiguration wifiConfiguration = (WifiConfiguration) fromXml.second;
        String strConfigKey = wifiConfiguration.configKey();
        if (!str2.equals(strConfigKey)) {
            throw new XmlPullParserException("Configuration key does not match. Retrieved: " + str2 + ", Calculated: " + strConfigKey);
        }
        String nameForUid = this.mContext.getPackageManager().getNameForUid(wifiConfiguration.creatorUid);
        if (nameForUid == null) {
            Log.e(TAG, "Invalid creatorUid for saved network " + wifiConfiguration.configKey() + ", creatorUid=" + wifiConfiguration.creatorUid);
            wifiConfiguration.creatorUid = 1000;
            wifiConfiguration.creatorName = this.mContext.getPackageManager().getNameForUid(1000);
        } else if (!nameForUid.equals(wifiConfiguration.creatorName)) {
            Log.w(TAG, "Invalid creatorName for saved network " + wifiConfiguration.configKey() + ", creatorUid=" + wifiConfiguration.creatorUid + ", creatorName=" + wifiConfiguration.creatorName);
            wifiConfiguration.creatorName = nameForUid;
        }
        wifiConfiguration.setNetworkSelectionStatus(fromXml2);
        wifiConfiguration.setIpConfiguration(fromXml3);
        if (fromXml4 != null) {
            wifiConfiguration.enterpriseConfig = fromXml4;
        }
        return wifiConfiguration;
    }
}
