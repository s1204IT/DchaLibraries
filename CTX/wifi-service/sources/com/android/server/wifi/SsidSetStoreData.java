package com.android.server.wifi;

import android.text.TextUtils;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SsidSetStoreData implements WifiConfigStore.StoreData {
    private static final String XML_TAG_SECTION_HEADER_SUFFIX = "ConfigData";
    private static final String XML_TAG_SSID_SET = "SSIDSet";
    private final DataSource mDataSource;
    private final String mTagName;

    public interface DataSource {
        Set<String> getSsids();

        void setSsids(Set<String> set);
    }

    SsidSetStoreData(String str, DataSource dataSource) {
        this.mTagName = str + XML_TAG_SECTION_HEADER_SUFFIX;
        this.mDataSource = dataSource;
    }

    @Override
    public void serializeData(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        if (z) {
            throw new XmlPullParserException("Share data not supported");
        }
        Set<String> ssids = this.mDataSource.getSsids();
        if (ssids != null && !ssids.isEmpty()) {
            XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SSID_SET, this.mDataSource.getSsids());
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
            if (TextUtils.isEmpty(strArr[0])) {
                throw new XmlPullParserException("Missing value name");
            }
            String str = strArr[0];
            byte b = -1;
            if (str.hashCode() == -1200860441 && str.equals(XML_TAG_SSID_SET)) {
                b = 0;
            }
            if (b != 0) {
                throw new XmlPullParserException("Unknown tag under " + this.mTagName + ": " + strArr[0]);
            }
            this.mDataSource.setSsids((Set) currentValue);
        }
    }

    @Override
    public void resetData(boolean z) {
        if (!z) {
            this.mDataSource.setSsids(new HashSet());
        }
    }

    @Override
    public String getName() {
        return this.mTagName;
    }

    @Override
    public boolean supportShareData() {
        return false;
    }
}
