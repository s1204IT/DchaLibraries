package com.android.server.wifi.hotspot2;

import android.net.wifi.hotspot2.PasspointConfiguration;
import android.text.TextUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PasspointConfigStoreData implements WifiConfigStore.StoreData {
    private static final String XML_TAG_CA_CERTIFICATE_ALIAS = "CaCertificateAlias";
    private static final String XML_TAG_CLIENT_CERTIFICATE_ALIAS = "ClientCertificateAlias";
    private static final String XML_TAG_CLIENT_PRIVATE_KEY_ALIAS = "ClientPrivateKeyAlias";
    private static final String XML_TAG_CREATOR_UID = "CreatorUID";
    private static final String XML_TAG_HAS_EVER_CONNECTED = "HasEverConnected";
    private static final String XML_TAG_PROVIDER_ID = "ProviderID";
    private static final String XML_TAG_PROVIDER_INDEX = "ProviderIndex";
    private static final String XML_TAG_SECTION_HEADER_PASSPOINT_CONFIGURATION = "Configuration";
    private static final String XML_TAG_SECTION_HEADER_PASSPOINT_CONFIG_DATA = "PasspointConfigData";
    private static final String XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER = "Provider";
    private static final String XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER_LIST = "ProviderList";
    private final DataSource mDataSource;
    private final WifiKeyStore mKeyStore;
    private final SIMAccessor mSimAccessor;

    public interface DataSource {
        long getProviderIndex();

        List<PasspointProvider> getProviders();

        void setProviderIndex(long j);

        void setProviders(List<PasspointProvider> list);
    }

    PasspointConfigStoreData(WifiKeyStore wifiKeyStore, SIMAccessor sIMAccessor, DataSource dataSource) {
        this.mKeyStore = wifiKeyStore;
        this.mSimAccessor = sIMAccessor;
        this.mDataSource = dataSource;
    }

    @Override
    public void serializeData(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        if (z) {
            serializeShareData(xmlSerializer);
        } else {
            serializeUserData(xmlSerializer);
        }
    }

    @Override
    public void deserializeData(XmlPullParser xmlPullParser, int i, boolean z) throws XmlPullParserException, IOException {
        if (xmlPullParser == null) {
            return;
        }
        if (z) {
            deserializeShareData(xmlPullParser, i);
        } else {
            deserializeUserData(xmlPullParser, i);
        }
    }

    @Override
    public void resetData(boolean z) {
        if (z) {
            resetShareData();
        } else {
            resetUserData();
        }
    }

    @Override
    public String getName() {
        return XML_TAG_SECTION_HEADER_PASSPOINT_CONFIG_DATA;
    }

    @Override
    public boolean supportShareData() {
        return true;
    }

    private void serializeShareData(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROVIDER_INDEX, Long.valueOf(this.mDataSource.getProviderIndex()));
    }

    private void serializeUserData(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        serializeProviderList(xmlSerializer, this.mDataSource.getProviders());
    }

    private void serializeProviderList(XmlSerializer xmlSerializer, List<PasspointProvider> list) throws XmlPullParserException, IOException {
        if (list == null) {
            return;
        }
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER_LIST);
        Iterator<PasspointProvider> it = list.iterator();
        while (it.hasNext()) {
            serializeProvider(xmlSerializer, it.next());
        }
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER_LIST);
    }

    private void serializeProvider(XmlSerializer xmlSerializer, PasspointProvider passpointProvider) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER);
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROVIDER_ID, Long.valueOf(passpointProvider.getProviderId()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CREATOR_UID, Integer.valueOf(passpointProvider.getCreatorUid()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CA_CERTIFICATE_ALIAS, passpointProvider.getCaCertificateAlias());
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CLIENT_CERTIFICATE_ALIAS, passpointProvider.getClientCertificateAlias());
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CLIENT_PRIVATE_KEY_ALIAS, passpointProvider.getClientPrivateKeyAlias());
        XmlUtil.writeNextValue(xmlSerializer, "HasEverConnected", Boolean.valueOf(passpointProvider.getHasEverConnected()));
        if (passpointProvider.getConfig() != null) {
            XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_PASSPOINT_CONFIGURATION);
            PasspointXmlUtils.serializePasspointConfiguration(xmlSerializer, passpointProvider.getConfig());
            XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_PASSPOINT_CONFIGURATION);
        }
        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER);
    }

    private void deserializeShareData(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
            String[] strArr = new String[1];
            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
            if (strArr[0] == null) {
                throw new XmlPullParserException("Missing value name");
            }
            String str = strArr[0];
            byte b = -1;
            if (str.hashCode() == 682520897 && str.equals(XML_TAG_PROVIDER_INDEX)) {
                b = 0;
            }
            if (b != 0) {
                throw new XmlPullParserException("Unknown value under share store data " + strArr[0]);
            }
            this.mDataSource.setProviderIndex(((Long) currentValue).longValue());
        }
    }

    private void deserializeUserData(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        String[] strArr = new String[1];
        while (XmlUtil.gotoNextSectionOrEnd(xmlPullParser, strArr, i)) {
            String str = strArr[0];
            byte b = -1;
            if (str.hashCode() == -254992817 && str.equals(XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER_LIST)) {
                b = 0;
            }
            if (b != 0) {
                throw new XmlPullParserException("Unknown Passpoint user store data " + strArr[0]);
            }
            this.mDataSource.setProviders(deserializeProviderList(xmlPullParser, i + 1));
        }
    }

    private List<PasspointProvider> deserializeProviderList(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        while (XmlUtil.gotoNextSectionWithNameOrEnd(xmlPullParser, XML_TAG_SECTION_HEADER_PASSPOINT_PROVIDER, i)) {
            arrayList.add(deserializeProvider(xmlPullParser, i + 1));
        }
        return arrayList;
    }

    private PasspointProvider deserializeProvider(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        byte b;
        char c = 0;
        long jLongValue = Long.MIN_VALUE;
        boolean zBooleanValue = false;
        PasspointConfiguration passpointConfigurationDeserializePasspointConfiguration = null;
        String str = null;
        String str2 = null;
        String str3 = null;
        int iIntValue = Integer.MIN_VALUE;
        while (XmlUtils.nextElementWithin(xmlPullParser, i)) {
            if (xmlPullParser.getAttributeValue(null, "name") != null) {
                byte b2 = 1;
                String[] strArr = new String[1];
                Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                String str4 = strArr[c];
                switch (str4.hashCode()) {
                    case -2096352532:
                        b2 = !str4.equals(XML_TAG_PROVIDER_ID) ? (byte) -1 : (byte) 0;
                        break;
                    case -1882773911:
                        if (!str4.equals(XML_TAG_CLIENT_PRIVATE_KEY_ALIAS)) {
                            b2 = -1;
                        } else {
                            b = 4;
                            b2 = b;
                        }
                        break;
                    case -1529270479:
                        if (!str4.equals("HasEverConnected")) {
                            b2 = -1;
                        } else {
                            b = 5;
                            b2 = b;
                        }
                        break;
                    case -922180444:
                        if (!str4.equals(XML_TAG_CREATOR_UID)) {
                            b2 = -1;
                        }
                        break;
                    case -603932412:
                        b2 = !str4.equals(XML_TAG_CLIENT_CERTIFICATE_ALIAS) ? (byte) -1 : (byte) 3;
                        break;
                    case 801332119:
                        b2 = !str4.equals(XML_TAG_CA_CERTIFICATE_ALIAS) ? (byte) -1 : (byte) 2;
                        break;
                    default:
                        b2 = -1;
                        break;
                }
                switch (b2) {
                    case 0:
                        jLongValue = ((Long) currentValue).longValue();
                        break;
                    case 1:
                        iIntValue = ((Integer) currentValue).intValue();
                        break;
                    case 2:
                        str = (String) currentValue;
                        break;
                    case 3:
                        str2 = (String) currentValue;
                        break;
                    case 4:
                        str3 = (String) currentValue;
                        break;
                    case 5:
                        zBooleanValue = ((Boolean) currentValue).booleanValue();
                        break;
                }
            } else {
                if (!TextUtils.equals(xmlPullParser.getName(), XML_TAG_SECTION_HEADER_PASSPOINT_CONFIGURATION)) {
                    throw new XmlPullParserException("Unexpected section under Provider: " + xmlPullParser.getName());
                }
                passpointConfigurationDeserializePasspointConfiguration = PasspointXmlUtils.deserializePasspointConfiguration(xmlPullParser, i + 1);
            }
            c = 0;
        }
        if (jLongValue == Long.MIN_VALUE) {
            throw new XmlPullParserException("Missing provider ID");
        }
        if (passpointConfigurationDeserializePasspointConfiguration != null) {
            return new PasspointProvider(passpointConfigurationDeserializePasspointConfiguration, this.mKeyStore, this.mSimAccessor, jLongValue, iIntValue, str, str2, str3, zBooleanValue, false);
        }
        throw new XmlPullParserException("Missing Passpoint configuration");
    }

    private void resetShareData() {
        this.mDataSource.setProviderIndex(0L);
    }

    private void resetUserData() {
        this.mDataSource.setProviders(new ArrayList());
    }
}
