package com.android.server.wifi;

import android.util.ArraySet;
import android.util.Log;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WakeupConfigStoreData implements WifiConfigStore.StoreData {
    private static final String TAG = "WakeupConfigStoreData";
    private static final String XML_TAG_FEATURE_STATE_SECTION = "FeatureState";
    private static final String XML_TAG_IS_ACTIVE = "IsActive";
    private static final String XML_TAG_IS_ONBOARDED = "IsOnboarded";
    private static final String XML_TAG_NETWORK_SECTION = "Network";
    private static final String XML_TAG_NOTIFICATIONS_SHOWN = "NotificationsShown";
    private static final String XML_TAG_SECURITY = "Security";
    private static final String XML_TAG_SSID = "SSID";
    private boolean mHasBeenRead = false;
    private final DataSource<Boolean> mIsActiveDataSource;
    private final DataSource<Boolean> mIsOnboardedDataSource;
    private final DataSource<Set<ScanResultMatchInfo>> mNetworkDataSource;
    private final DataSource<Integer> mNotificationsDataSource;

    public interface DataSource<T> {
        T getData();

        void setData(T t);
    }

    public WakeupConfigStoreData(DataSource<Boolean> dataSource, DataSource<Boolean> dataSource2, DataSource<Integer> dataSource3, DataSource<Set<ScanResultMatchInfo>> dataSource4) {
        this.mIsActiveDataSource = dataSource;
        this.mIsOnboardedDataSource = dataSource2;
        this.mNotificationsDataSource = dataSource3;
        this.mNetworkDataSource = dataSource4;
    }

    public boolean hasBeenRead() {
        return this.mHasBeenRead;
    }

    @Override
    public void serializeData(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        if (z) {
            throw new XmlPullParserException("Share data not supported");
        }
        writeFeatureState(xmlSerializer);
        Iterator<ScanResultMatchInfo> it = this.mNetworkDataSource.getData().iterator();
        while (it.hasNext()) {
            writeNetwork(xmlSerializer, it.next());
        }
    }

    private void writeFeatureState(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_FEATURE_STATE_SECTION);
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_IS_ACTIVE, this.mIsActiveDataSource.getData());
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_IS_ONBOARDED, this.mIsOnboardedDataSource.getData());
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_NOTIFICATIONS_SHOWN, this.mNotificationsDataSource.getData());
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_FEATURE_STATE_SECTION);
    }

    private void writeNetwork(XmlSerializer xmlSerializer, ScanResultMatchInfo scanResultMatchInfo) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_NETWORK_SECTION);
        XmlUtil.writeNextValue(xmlSerializer, "SSID", scanResultMatchInfo.networkSsid);
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SECURITY, Integer.valueOf(scanResultMatchInfo.networkType));
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_NETWORK_SECTION);
    }

    @Override
    public void deserializeData(XmlPullParser xmlPullParser, int i, boolean z) throws XmlPullParserException, IOException {
        if (!z && !this.mHasBeenRead) {
            Log.d(TAG, "WifiWake user data has been read");
            this.mHasBeenRead = true;
        }
        if (xmlPullParser == null) {
            return;
        }
        if (z) {
            throw new XmlPullParserException("Shared data not supported");
        }
        ArraySet arraySet = new ArraySet();
        String[] strArr = new String[1];
        while (XmlUtil.gotoNextSectionOrEnd(xmlPullParser, strArr, i)) {
            byte b = 0;
            String str = strArr[0];
            int iHashCode = str.hashCode();
            if (iHashCode != -786828786) {
                if (iHashCode != 1362433883 || !str.equals(XML_TAG_FEATURE_STATE_SECTION)) {
                    b = -1;
                }
            } else if (str.equals(XML_TAG_NETWORK_SECTION)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    parseFeatureState(xmlPullParser, i + 1);
                    break;
                case 1:
                    arraySet.add(parseNetwork(xmlPullParser, i + 1));
                    break;
            }
        }
        this.mNetworkDataSource.setData(arraySet);
    }

    private void parseFeatureState(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        boolean zBooleanValue = false;
        boolean zBooleanValue2 = false;
        int iIntValue = 0;
        while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
            byte b = 1;
            String[] strArr = new String[1];
            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
            if (strArr[0] == null) {
                throw new XmlPullParserException("Missing value name");
            }
            String str = strArr[0];
            int iHashCode = str.hashCode();
            if (iHashCode != -1725092580) {
                if (iHashCode != -684272400) {
                    b = (iHashCode == 898665769 && str.equals(XML_TAG_NOTIFICATIONS_SHOWN)) ? (byte) 2 : (byte) -1;
                } else if (str.equals(XML_TAG_IS_ACTIVE)) {
                    b = 0;
                }
            } else if (!str.equals(XML_TAG_IS_ONBOARDED)) {
            }
            switch (b) {
                case 0:
                    zBooleanValue = ((Boolean) currentValue).booleanValue();
                    break;
                case 1:
                    zBooleanValue2 = ((Boolean) currentValue).booleanValue();
                    break;
                case 2:
                    iIntValue = ((Integer) currentValue).intValue();
                    break;
                default:
                    throw new XmlPullParserException("Unknown value found: " + strArr[0]);
            }
        }
        this.mIsActiveDataSource.setData(Boolean.valueOf(zBooleanValue));
        this.mIsOnboardedDataSource.setData(Boolean.valueOf(zBooleanValue2));
        this.mNotificationsDataSource.setData(Integer.valueOf(iIntValue));
    }

    private ScanResultMatchInfo parseNetwork(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        ScanResultMatchInfo scanResultMatchInfo = new ScanResultMatchInfo();
        while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
            byte b = 1;
            String[] strArr = new String[1];
            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
            if (strArr[0] == null) {
                throw new XmlPullParserException("Missing value name");
            }
            String str = strArr[0];
            int iHashCode = str.hashCode();
            if (iHashCode != 2554747) {
                if (iHashCode != 1013767008 || !str.equals(XML_TAG_SECURITY)) {
                    b = -1;
                }
            } else if (str.equals("SSID")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    scanResultMatchInfo.networkSsid = (String) currentValue;
                    break;
                case 1:
                    scanResultMatchInfo.networkType = ((Integer) currentValue).intValue();
                    break;
                default:
                    throw new XmlPullParserException("Unknown tag under WakeupConfigStoreData: " + strArr[0]);
            }
        }
        return scanResultMatchInfo;
    }

    @Override
    public void resetData(boolean z) {
        if (!z) {
            this.mNetworkDataSource.setData(Collections.emptySet());
            this.mIsActiveDataSource.setData(false);
            this.mIsOnboardedDataSource.setData(false);
            this.mNotificationsDataSource.setData(0);
        }
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean supportShareData() {
        return false;
    }
}
