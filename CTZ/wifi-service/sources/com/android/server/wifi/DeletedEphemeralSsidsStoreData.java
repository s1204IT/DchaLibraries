package com.android.server.wifi;

import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DeletedEphemeralSsidsStoreData implements WifiConfigStore.StoreData {
    private static final String XML_TAG_SECTION_HEADER_DELETED_EPHEMERAL_SSID_LIST = "DeletedEphemeralSSIDList";
    private static final String XML_TAG_SSID_LIST = "SSIDList";
    private Set<String> mSsidList;

    DeletedEphemeralSsidsStoreData() {
    }

    @Override
    public void serializeData(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        if (z) {
            throw new XmlPullParserException("Share data not supported");
        }
        if (this.mSsidList != null) {
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SSID_LIST, this.mSsidList);
        }
    }

    @Override
    public void deserializeData(XmlPullParser xmlPullParser, int i, boolean z) throws XmlPullParserException, IOException {
        if (xmlPullParser == null) {
            return;
        }
        if (z) {
            throw new XmlPullParserException("Share data not supported");
        }
        while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
            String[] strArr = new String[1];
            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
            if (strArr[0] == null) {
                throw new XmlPullParserException("Missing value name");
            }
            String str = strArr[0];
            byte b = -1;
            if (str.hashCode() == 1427827385 && str.equals(XML_TAG_SSID_LIST)) {
                b = 0;
            }
            if (b != 0) {
                throw new XmlPullParserException("Unknown tag under DeletedEphemeralSSIDList: " + strArr[0]);
            }
            this.mSsidList = (Set) currentValue;
        }
    }

    @Override
    public void resetData(boolean z) {
        if (!z) {
            this.mSsidList = null;
        }
    }

    @Override
    public String getName() {
        return XML_TAG_SECTION_HEADER_DELETED_EPHEMERAL_SSID_LIST;
    }

    @Override
    public boolean supportShareData() {
        return false;
    }

    public Set<String> getSsidList() {
        if (this.mSsidList == null) {
            return new HashSet();
        }
        return this.mSsidList;
    }

    public void setSsidList(Set<String> set) {
        this.mSsidList = set;
    }
}
