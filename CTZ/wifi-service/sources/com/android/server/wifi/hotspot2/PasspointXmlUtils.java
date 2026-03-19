package com.android.server.wifi.hotspot2;

import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.net.wifi.hotspot2.pps.Policy;
import android.net.wifi.hotspot2.pps.UpdateParameter;
import com.android.internal.util.XmlUtils;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PasspointXmlUtils {
    private static final String XML_TAG_ABLE_TO_SHARE = "AbleToShare";
    private static final String XML_TAG_CERT_SHA256_FINGERPRINT = "CertSHA256Fingerprint";
    private static final String XML_TAG_CERT_TYPE = "CertType";
    private static final String XML_TAG_CHECK_AAA_SERVER_CERT_STATUS = "CheckAAAServerCertStatus";
    private static final String XML_TAG_COUNTRIES = "Countries";
    private static final String XML_TAG_CREATION_TIME = "CreationTime";
    private static final String XML_TAG_CREDENTIAL_PRIORITY = "CredentialPriority";
    private static final String XML_TAG_EAP_TYPE = "EAPType";
    private static final String XML_TAG_EXCLUDED_SSID_LIST = "ExcludedSSIDList";
    private static final String XML_TAG_EXPIRATION_TIME = "ExpirationTime";
    private static final String XML_TAG_FQDN = "FQDN";
    private static final String XML_TAG_FQDN_EXACT_MATCH = "FQDNExactMatch";
    private static final String XML_TAG_FRIENDLY_NAME = "FriendlyName";
    private static final String XML_TAG_HOME_NETWORK_IDS = "HomeNetworkIDs";
    private static final String XML_TAG_ICON_URL = "IconURL";
    private static final String XML_TAG_IMSI = "IMSI";
    private static final String XML_TAG_MACHINE_MANAGED = "MachineManaged";
    private static final String XML_TAG_MATCH_ALL_OIS = "MatchAllOIs";
    private static final String XML_TAG_MATCH_ANY_OIS = "MatchAnyOIs";
    private static final String XML_TAG_MAXIMUM_BSS_LOAD_VALUE = "MaximumBSSLoadValue";
    private static final String XML_TAG_MIN_HOME_DOWNLINK_BANDWIDTH = "MinHomeDownlinkBandwidth";
    private static final String XML_TAG_MIN_HOME_UPLINK_BANDWIDTH = "MinHomeUplinkBandwidth";
    private static final String XML_TAG_MIN_ROAMING_DOWNLINK_BANDWIDTH = "MinRoamingDownlinkBandwidth";
    private static final String XML_TAG_MIN_ROAMING_UPLINK_BANDWIDTH = "MinRoamingUplinkBandwidth";
    private static final String XML_TAG_NON_EAP_INNER_METHOD = "NonEAPInnerMethod";
    private static final String XML_TAG_OTHER_HOME_PARTNERS = "OtherHomePartners";
    private static final String XML_TAG_PASSWORD = "Password";
    private static final String XML_TAG_PORTS = "Ports";
    private static final String XML_TAG_PRIORITY = "Priority";
    private static final String XML_TAG_PROTO = "Proto";
    private static final String XML_TAG_REALM = "Realm";
    private static final String XML_TAG_RESTRICTION = "Restriction";
    private static final String XML_TAG_ROAMING_CONSORTIUM_OIS = "RoamingConsortiumOIs";
    private static final String XML_TAG_SECTION_HEADER_CERT_CREDENTIAL = "CertCredential";
    private static final String XML_TAG_SECTION_HEADER_CREDENTIAL = "Credential";
    private static final String XML_TAG_SECTION_HEADER_HOMESP = "HomeSP";
    private static final String XML_TAG_SECTION_HEADER_POLICY = "Policy";
    private static final String XML_TAG_SECTION_HEADER_POLICY_UPDATE = "PolicyUpdate";
    private static final String XML_TAG_SECTION_HEADER_PREFERRED_ROAMING_PARTNER_LIST = "RoamingPartnerList";
    private static final String XML_TAG_SECTION_HEADER_PROTO_PORT = "ProtoPort";
    private static final String XML_TAG_SECTION_HEADER_REQUIRED_PROTO_PORT_MAP = "RequiredProtoPortMap";
    private static final String XML_TAG_SECTION_HEADER_ROAMING_PARTNER = "RoamingPartner";
    private static final String XML_TAG_SECTION_HEADER_SIM_CREDENTIAL = "SimCredential";
    private static final String XML_TAG_SECTION_HEADER_SUBSCRIPTION_UPDATE = "SubscriptionUpdate";
    private static final String XML_TAG_SECTION_HEADER_USER_CREDENTIAL = "UserCredential";
    private static final String XML_TAG_SERVER_URI = "ServerURI";
    private static final String XML_TAG_SOFT_TOKEN_APP = "SoftTokenApp";
    private static final String XML_TAG_SUBSCRIPTION_CREATION_TIME = "SubscriptionCreationTime";
    private static final String XML_TAG_SUBSCRIPTION_EXPIRATION_TIME = "SubscriptionExpirationTime";
    private static final String XML_TAG_SUBSCRIPTION_TYPE = "SubscriptionType";
    private static final String XML_TAG_TRUST_ROOT_CERT_LIST = "TrustRootCertList";
    private static final String XML_TAG_TRUST_ROOT_CERT_SHA256_FINGERPRINT = "TrustRootCertSHA256Fingerprint";
    private static final String XML_TAG_TRUST_ROOT_CERT_URL = "TrustRootCertURL";
    private static final String XML_TAG_UPDATE_IDENTIFIER = "UpdateIdentifier";
    private static final String XML_TAG_UPDATE_INTERVAL = "UpdateInterval";
    private static final String XML_TAG_UPDATE_METHOD = "UpdateMethod";
    private static final String XML_TAG_USAGE_LIMIT_DATA_LIMIT = "UsageLimitDataLimit";
    private static final String XML_TAG_USAGE_LIMIT_START_TIME = "UsageLimitStartTime";
    private static final String XML_TAG_USAGE_LIMIT_TIME_LIMIT = "UsageLimitTimeLimit";
    private static final String XML_TAG_USAGE_LIMIT_TIME_PERIOD = "UsageLimitTimePeriod";
    private static final String XML_TAG_USERNAME = "Username";

    public static void serializePasspointConfiguration(XmlSerializer xmlSerializer, PasspointConfiguration passpointConfiguration) throws XmlPullParserException, IOException {
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_UPDATE_IDENTIFIER, Integer.valueOf(passpointConfiguration.getUpdateIdentifier()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CREDENTIAL_PRIORITY, Integer.valueOf(passpointConfiguration.getCredentialPriority()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_TRUST_ROOT_CERT_LIST, passpointConfiguration.getTrustRootCertList());
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SUBSCRIPTION_CREATION_TIME, Long.valueOf(passpointConfiguration.getSubscriptionCreationTimeInMillis()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SUBSCRIPTION_EXPIRATION_TIME, Long.valueOf(passpointConfiguration.getSubscriptionExpirationTimeInMillis()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SUBSCRIPTION_TYPE, passpointConfiguration.getSubscriptionType());
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USAGE_LIMIT_TIME_PERIOD, Long.valueOf(passpointConfiguration.getUsageLimitUsageTimePeriodInMinutes()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USAGE_LIMIT_START_TIME, Long.valueOf(passpointConfiguration.getUsageLimitStartTimeInMillis()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USAGE_LIMIT_DATA_LIMIT, Long.valueOf(passpointConfiguration.getUsageLimitDataLimit()));
        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USAGE_LIMIT_TIME_LIMIT, Long.valueOf(passpointConfiguration.getUsageLimitTimeLimitInMinutes()));
        serializeHomeSp(xmlSerializer, passpointConfiguration.getHomeSp());
        serializeCredential(xmlSerializer, passpointConfiguration.getCredential());
        serializePolicy(xmlSerializer, passpointConfiguration.getPolicy());
        serializeUpdateParameter(xmlSerializer, XML_TAG_SECTION_HEADER_SUBSCRIPTION_UPDATE, passpointConfiguration.getSubscriptionUpdate());
    }

    public static PasspointConfiguration deserializePasspointConfiguration(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        Object currentValue;
        PasspointConfiguration passpointConfiguration = new PasspointConfiguration();
        while (XmlUtils.nextElementWithin(xmlPullParser, i)) {
            if (isValueElement(xmlPullParser)) {
                String[] strArr = new String[1];
                currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                switch (strArr[0]) {
                    case "UpdateIdentifier":
                        passpointConfiguration.setUpdateIdentifier(((Integer) currentValue).intValue());
                        break;
                    case "CredentialPriority":
                        passpointConfiguration.setCredentialPriority(((Integer) currentValue).intValue());
                        break;
                    case "TrustRootCertList":
                        passpointConfiguration.setTrustRootCertList((Map) currentValue);
                        break;
                    case "SubscriptionCreationTime":
                        passpointConfiguration.setSubscriptionCreationTimeInMillis(((Long) currentValue).longValue());
                        break;
                    case "SubscriptionExpirationTime":
                        passpointConfiguration.setSubscriptionExpirationTimeInMillis(((Long) currentValue).longValue());
                        break;
                    case "SubscriptionType":
                        passpointConfiguration.setSubscriptionType((String) currentValue);
                        break;
                    case "UsageLimitTimePeriod":
                        passpointConfiguration.setUsageLimitUsageTimePeriodInMinutes(((Long) currentValue).longValue());
                        break;
                    case "UsageLimitStartTime":
                        passpointConfiguration.setUsageLimitStartTimeInMillis(((Long) currentValue).longValue());
                        break;
                    case "UsageLimitDataLimit":
                        passpointConfiguration.setUsageLimitDataLimit(((Long) currentValue).longValue());
                        break;
                    case "UsageLimitTimeLimit":
                        passpointConfiguration.setUsageLimitTimeLimitInMinutes(((Long) currentValue).longValue());
                        break;
                    default:
                        throw new XmlPullParserException("Unknown value under PasspointConfiguration: " + xmlPullParser.getName());
                }
            } else {
                String name = xmlPullParser.getName();
                int iHashCode = name.hashCode();
                if (iHashCode == -2127810660) {
                    if (name.equals(XML_TAG_SECTION_HEADER_HOMESP)) {
                    }
                } else if (iHashCode == -1898802862) {
                    if (name.equals(XML_TAG_SECTION_HEADER_POLICY)) {
                    }
                } else if (iHashCode != 162345062) {
                    throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.PasspointXmlUtils.deserializePasspointConfiguration(org.xmlpull.v1.XmlPullParser, int):android.net.wifi.hotspot2.PasspointConfiguration");
                }

                private static void serializeHomeSp(XmlSerializer xmlSerializer, HomeSp homeSp) throws XmlPullParserException, IOException {
                    if (homeSp == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_HOMESP);
                    XmlUtil.writeNextValue(xmlSerializer, "FQDN", homeSp.getFqdn());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_FRIENDLY_NAME, homeSp.getFriendlyName());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ICON_URL, homeSp.getIconUrl());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_HOME_NETWORK_IDS, homeSp.getHomeNetworkIds());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MATCH_ALL_OIS, homeSp.getMatchAllOis());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MATCH_ANY_OIS, homeSp.getMatchAnyOis());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_OTHER_HOME_PARTNERS, homeSp.getOtherHomePartners());
                    XmlUtil.writeNextValue(xmlSerializer, "RoamingConsortiumOIs", homeSp.getRoamingConsortiumOis());
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_HOMESP);
                }

                private static void serializeCredential(XmlSerializer xmlSerializer, Credential credential) throws XmlPullParserException, IOException {
                    if (credential == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_CREDENTIAL);
                    XmlUtil.writeNextValue(xmlSerializer, "CreationTime", Long.valueOf(credential.getCreationTimeInMillis()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_EXPIRATION_TIME, Long.valueOf(credential.getExpirationTimeInMillis()));
                    XmlUtil.writeNextValue(xmlSerializer, "Realm", credential.getRealm());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CHECK_AAA_SERVER_CERT_STATUS, Boolean.valueOf(credential.getCheckAaaServerCertStatus()));
                    serializeUserCredential(xmlSerializer, credential.getUserCredential());
                    serializeCertCredential(xmlSerializer, credential.getCertCredential());
                    serializeSimCredential(xmlSerializer, credential.getSimCredential());
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_CREDENTIAL);
                }

                private static void serializePolicy(XmlSerializer xmlSerializer, Policy policy) throws XmlPullParserException, IOException {
                    if (policy == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_POLICY);
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MIN_HOME_DOWNLINK_BANDWIDTH, Long.valueOf(policy.getMinHomeDownlinkBandwidth()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MIN_HOME_UPLINK_BANDWIDTH, Long.valueOf(policy.getMinHomeUplinkBandwidth()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MIN_ROAMING_DOWNLINK_BANDWIDTH, Long.valueOf(policy.getMinRoamingDownlinkBandwidth()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MIN_ROAMING_UPLINK_BANDWIDTH, Long.valueOf(policy.getMinRoamingUplinkBandwidth()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_EXCLUDED_SSID_LIST, policy.getExcludedSsidList());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MAXIMUM_BSS_LOAD_VALUE, Integer.valueOf(policy.getMaximumBssLoadValue()));
                    serializeProtoPortMap(xmlSerializer, policy.getRequiredProtoPortMap());
                    serializeUpdateParameter(xmlSerializer, XML_TAG_SECTION_HEADER_POLICY_UPDATE, policy.getPolicyUpdate());
                    serializePreferredRoamingPartnerList(xmlSerializer, policy.getPreferredRoamingPartnerList());
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_POLICY);
                }

                private static void serializeUserCredential(XmlSerializer xmlSerializer, Credential.UserCredential userCredential) throws XmlPullParserException, IOException {
                    if (userCredential == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_USER_CREDENTIAL);
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USERNAME, userCredential.getUsername());
                    XmlUtil.writeNextValue(xmlSerializer, "Password", userCredential.getPassword());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_MACHINE_MANAGED, Boolean.valueOf(userCredential.getMachineManaged()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SOFT_TOKEN_APP, userCredential.getSoftTokenApp());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_ABLE_TO_SHARE, Boolean.valueOf(userCredential.getAbleToShare()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_EAP_TYPE, Integer.valueOf(userCredential.getEapType()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_NON_EAP_INNER_METHOD, userCredential.getNonEapInnerMethod());
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_USER_CREDENTIAL);
                }

                private static void serializeCertCredential(XmlSerializer xmlSerializer, Credential.CertificateCredential certificateCredential) throws XmlPullParserException, IOException {
                    if (certificateCredential == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_CERT_CREDENTIAL);
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CERT_TYPE, certificateCredential.getCertType());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_CERT_SHA256_FINGERPRINT, certificateCredential.getCertSha256Fingerprint());
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_CERT_CREDENTIAL);
                }

                private static void serializeSimCredential(XmlSerializer xmlSerializer, Credential.SimCredential simCredential) throws XmlPullParserException, IOException {
                    if (simCredential == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_SIM_CREDENTIAL);
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_IMSI, simCredential.getImsi());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_EAP_TYPE, Integer.valueOf(simCredential.getEapType()));
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_SIM_CREDENTIAL);
                }

                private static void serializePreferredRoamingPartnerList(XmlSerializer xmlSerializer, List<Policy.RoamingPartner> list) throws XmlPullParserException, IOException {
                    if (list == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_PREFERRED_ROAMING_PARTNER_LIST);
                    for (Policy.RoamingPartner roamingPartner : list) {
                        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_ROAMING_PARTNER);
                        XmlUtil.writeNextValue(xmlSerializer, "FQDN", roamingPartner.getFqdn());
                        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_FQDN_EXACT_MATCH, Boolean.valueOf(roamingPartner.getFqdnExactMatch()));
                        XmlUtil.writeNextValue(xmlSerializer, "Priority", Integer.valueOf(roamingPartner.getPriority()));
                        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_COUNTRIES, roamingPartner.getCountries());
                        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_ROAMING_PARTNER);
                    }
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_PREFERRED_ROAMING_PARTNER_LIST);
                }

                private static void serializeUpdateParameter(XmlSerializer xmlSerializer, String str, UpdateParameter updateParameter) throws XmlPullParserException, IOException {
                    if (updateParameter == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, str);
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_UPDATE_INTERVAL, Long.valueOf(updateParameter.getUpdateIntervalInMinutes()));
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_UPDATE_METHOD, updateParameter.getUpdateMethod());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_RESTRICTION, updateParameter.getRestriction());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_SERVER_URI, updateParameter.getServerUri());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_USERNAME, updateParameter.getUsername());
                    XmlUtil.writeNextValue(xmlSerializer, "Password", updateParameter.getBase64EncodedPassword());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_TRUST_ROOT_CERT_URL, updateParameter.getTrustRootCertUrl());
                    XmlUtil.writeNextValue(xmlSerializer, XML_TAG_TRUST_ROOT_CERT_SHA256_FINGERPRINT, updateParameter.getTrustRootCertSha256Fingerprint());
                    XmlUtil.writeNextSectionEnd(xmlSerializer, str);
                }

                private static void serializeProtoPortMap(XmlSerializer xmlSerializer, Map<Integer, String> map) throws XmlPullParserException, IOException {
                    if (map == null) {
                        return;
                    }
                    XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_REQUIRED_PROTO_PORT_MAP);
                    for (Map.Entry<Integer, String> entry : map.entrySet()) {
                        XmlUtil.writeNextSectionStart(xmlSerializer, XML_TAG_SECTION_HEADER_PROTO_PORT);
                        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PROTO, entry.getKey());
                        XmlUtil.writeNextValue(xmlSerializer, XML_TAG_PORTS, entry.getValue());
                        XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_PROTO_PORT);
                    }
                    XmlUtil.writeNextSectionEnd(xmlSerializer, XML_TAG_SECTION_HEADER_REQUIRED_PROTO_PORT_MAP);
                }

                private static HomeSp deserializeHomeSP(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                    HomeSp homeSp = new HomeSp();
                    while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                        String[] strArr = new String[1];
                        Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                        if (strArr[0] == null) {
                            throw new XmlPullParserException("Missing value name");
                        }
                        switch (strArr[0]) {
                            case "FQDN":
                                homeSp.setFqdn((String) currentValue);
                                break;
                            case "FriendlyName":
                                homeSp.setFriendlyName((String) currentValue);
                                break;
                            case "IconURL":
                                homeSp.setIconUrl((String) currentValue);
                                break;
                            case "HomeNetworkIDs":
                                homeSp.setHomeNetworkIds((Map) currentValue);
                                break;
                            case "MatchAllOIs":
                                homeSp.setMatchAllOis((long[]) currentValue);
                                break;
                            case "MatchAnyOIs":
                                homeSp.setMatchAnyOis((long[]) currentValue);
                                break;
                            case "RoamingConsortiumOIs":
                                homeSp.setRoamingConsortiumOis((long[]) currentValue);
                                break;
                            case "OtherHomePartners":
                                homeSp.setOtherHomePartners((String[]) currentValue);
                                break;
                            default:
                                throw new XmlPullParserException("Unknown data under HomeSP: " + strArr[0]);
                        }
                    }
                    return homeSp;
                }

                private static Credential deserializeCredential(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                    Credential credential = new Credential();
                    while (XmlUtils.nextElementWithin(xmlPullParser, i)) {
                        byte b = 2;
                        if (isValueElement(xmlPullParser)) {
                            String[] strArr = new String[1];
                            Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                            String str = strArr[0];
                            int iHashCode = str.hashCode();
                            if (iHashCode == -1670320580) {
                                if (str.equals(XML_TAG_EXPIRATION_TIME)) {
                                    b = 1;
                                }
                                switch (b) {
                                }
                            } else if (iHashCode == 78834287) {
                                if (!str.equals("Realm")) {
                                }
                                switch (b) {
                                }
                            } else if (iHashCode != 646045490) {
                                b = (iHashCode == 1750336108 && str.equals("CreationTime")) ? (byte) 0 : (byte) -1;
                                switch (b) {
                                    case 0:
                                        credential.setCreationTimeInMillis(((Long) currentValue).longValue());
                                        break;
                                    case 1:
                                        credential.setExpirationTimeInMillis(((Long) currentValue).longValue());
                                        break;
                                    case 2:
                                        credential.setRealm((String) currentValue);
                                        break;
                                    case 3:
                                        credential.setCheckAaaServerCertStatus(((Boolean) currentValue).booleanValue());
                                        break;
                                    default:
                                        throw new XmlPullParserException("Unknown value under Credential: " + strArr[0]);
                                }
                            } else {
                                if (str.equals(XML_TAG_CHECK_AAA_SERVER_CERT_STATUS)) {
                                    b = 3;
                                }
                                switch (b) {
                                }
                            }
                        } else {
                            String name = xmlPullParser.getName();
                            int iHashCode2 = name.hashCode();
                            if (iHashCode2 == -930907486) {
                                if (name.equals(XML_TAG_SECTION_HEADER_USER_CREDENTIAL)) {
                                    b = 0;
                                }
                                switch (b) {
                                }
                            } else if (iHashCode2 != 802017390) {
                                b = (iHashCode2 == 1771027899 && name.equals(XML_TAG_SECTION_HEADER_CERT_CREDENTIAL)) ? (byte) 1 : (byte) -1;
                                switch (b) {
                                    case 0:
                                        credential.setUserCredential(deserializeUserCredential(xmlPullParser, i + 1));
                                        break;
                                    case 1:
                                        credential.setCertCredential(deserializeCertCredential(xmlPullParser, i + 1));
                                        break;
                                    case 2:
                                        credential.setSimCredential(deserializeSimCredential(xmlPullParser, i + 1));
                                        break;
                                    default:
                                        throw new XmlPullParserException("Unknown section under Credential: " + xmlPullParser.getName());
                                }
                            } else {
                                if (!name.equals(XML_TAG_SECTION_HEADER_SIM_CREDENTIAL)) {
                                }
                                switch (b) {
                                }
                            }
                        }
                    }
                    return credential;
                }

                private static Policy deserializePolicy(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                    Object currentValue;
                    Policy policy = new Policy();
                    while (XmlUtils.nextElementWithin(xmlPullParser, i)) {
                        if (isValueElement(xmlPullParser)) {
                            String[] strArr = new String[1];
                            currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                            switch (strArr[0]) {
                                case "MinHomeDownlinkBandwidth":
                                    policy.setMinHomeDownlinkBandwidth(((Long) currentValue).longValue());
                                    break;
                                case "MinHomeUplinkBandwidth":
                                    policy.setMinHomeUplinkBandwidth(((Long) currentValue).longValue());
                                    break;
                                case "MinRoamingDownlinkBandwidth":
                                    policy.setMinRoamingDownlinkBandwidth(((Long) currentValue).longValue());
                                    break;
                                case "MinRoamingUplinkBandwidth":
                                    policy.setMinRoamingUplinkBandwidth(((Long) currentValue).longValue());
                                    break;
                                case "ExcludedSSIDList":
                                    policy.setExcludedSsidList((String[]) currentValue);
                                    break;
                                case "MaximumBSSLoadValue":
                                    policy.setMaximumBssLoadValue(((Integer) currentValue).intValue());
                                    break;
                            }
                        } else {
                            String name = xmlPullParser.getName();
                            int iHashCode = name.hashCode();
                            if (iHashCode == -2125460531) {
                                if (!name.equals(XML_TAG_SECTION_HEADER_PREFERRED_ROAMING_PARTNER_LIST)) {
                                }
                            } else if (iHashCode != -1710886725) {
                                throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.PasspointXmlUtils.deserializePolicy(org.xmlpull.v1.XmlPullParser, int):android.net.wifi.hotspot2.pps.Policy");
                            }

                            private static Credential.UserCredential deserializeUserCredential(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                Credential.UserCredential userCredential = new Credential.UserCredential();
                                while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                                    String[] strArr = new String[1];
                                    Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                                    if (strArr[0] == null) {
                                        throw new XmlPullParserException("Missing value name");
                                    }
                                    switch (strArr[0]) {
                                        case "Username":
                                            userCredential.setUsername((String) currentValue);
                                            break;
                                        case "Password":
                                            userCredential.setPassword((String) currentValue);
                                            break;
                                        case "MachineManaged":
                                            userCredential.setMachineManaged(((Boolean) currentValue).booleanValue());
                                            break;
                                        case "SoftTokenApp":
                                            userCredential.setSoftTokenApp((String) currentValue);
                                            break;
                                        case "AbleToShare":
                                            userCredential.setAbleToShare(((Boolean) currentValue).booleanValue());
                                            break;
                                        case "EAPType":
                                            userCredential.setEapType(((Integer) currentValue).intValue());
                                            break;
                                        case "NonEAPInnerMethod":
                                            userCredential.setNonEapInnerMethod((String) currentValue);
                                            break;
                                        default:
                                            throw new XmlPullParserException("Unknown value under UserCredential: " + strArr[0]);
                                    }
                                }
                                return userCredential;
                            }

                            private static Credential.CertificateCredential deserializeCertCredential(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                Credential.CertificateCredential certificateCredential = new Credential.CertificateCredential();
                                while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                                    byte b = 1;
                                    String[] strArr = new String[1];
                                    Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                                    if (strArr[0] == null) {
                                        throw new XmlPullParserException("Missing value name");
                                    }
                                    String str = strArr[0];
                                    int iHashCode = str.hashCode();
                                    if (iHashCode != -673759330) {
                                        if (iHashCode != -285451687 || !str.equals(XML_TAG_CERT_SHA256_FINGERPRINT)) {
                                            b = -1;
                                        }
                                    } else if (str.equals(XML_TAG_CERT_TYPE)) {
                                        b = 0;
                                    }
                                    switch (b) {
                                        case 0:
                                            certificateCredential.setCertType((String) currentValue);
                                            break;
                                        case 1:
                                            certificateCredential.setCertSha256Fingerprint((byte[]) currentValue);
                                            break;
                                        default:
                                            throw new XmlPullParserException("Unknown value under CertCredential: " + strArr[0]);
                                    }
                                }
                                return certificateCredential;
                            }

                            private static Credential.SimCredential deserializeSimCredential(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                Credential.SimCredential simCredential = new Credential.SimCredential();
                                while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                                    byte b = 1;
                                    String[] strArr = new String[1];
                                    Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                                    if (strArr[0] == null) {
                                        throw new XmlPullParserException("Missing value name");
                                    }
                                    String str = strArr[0];
                                    int iHashCode = str.hashCode();
                                    if (iHashCode != -1249356658) {
                                        b = (iHashCode == 2251386 && str.equals(XML_TAG_IMSI)) ? (byte) 0 : (byte) -1;
                                    } else if (!str.equals(XML_TAG_EAP_TYPE)) {
                                    }
                                    switch (b) {
                                        case 0:
                                            simCredential.setImsi((String) currentValue);
                                            break;
                                        case 1:
                                            simCredential.setEapType(((Integer) currentValue).intValue());
                                            break;
                                        default:
                                            throw new XmlPullParserException("Unknown value under CertCredential: " + strArr[0]);
                                    }
                                }
                                return simCredential;
                            }

                            private static List<Policy.RoamingPartner> deserializePreferredRoamingPartnerList(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                ArrayList arrayList = new ArrayList();
                                while (XmlUtil.gotoNextSectionWithNameOrEnd(xmlPullParser, XML_TAG_SECTION_HEADER_ROAMING_PARTNER, i)) {
                                    arrayList.add(deserializeRoamingPartner(xmlPullParser, i + 1));
                                }
                                return arrayList;
                            }

                            private static Policy.RoamingPartner deserializeRoamingPartner(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                Policy.RoamingPartner roamingPartner = new Policy.RoamingPartner();
                                while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                                    byte b = 1;
                                    String[] strArr = new String[1];
                                    Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                                    if (strArr[0] == null) {
                                        throw new XmlPullParserException("Missing value name");
                                    }
                                    String str = strArr[0];
                                    int iHashCode = str.hashCode();
                                    if (iHashCode != -1768941957) {
                                        if (iHashCode != -1100816956) {
                                            if (iHashCode != -938362220) {
                                                b = (iHashCode == 2165397 && str.equals("FQDN")) ? (byte) 0 : (byte) -1;
                                            } else if (str.equals(XML_TAG_COUNTRIES)) {
                                                b = 3;
                                            }
                                        } else if (str.equals("Priority")) {
                                            b = 2;
                                        }
                                    } else if (!str.equals(XML_TAG_FQDN_EXACT_MATCH)) {
                                    }
                                    switch (b) {
                                        case 0:
                                            roamingPartner.setFqdn((String) currentValue);
                                            break;
                                        case 1:
                                            roamingPartner.setFqdnExactMatch(((Boolean) currentValue).booleanValue());
                                            break;
                                        case 2:
                                            roamingPartner.setPriority(((Integer) currentValue).intValue());
                                            break;
                                        case 3:
                                            roamingPartner.setCountries((String) currentValue);
                                            break;
                                        default:
                                            throw new XmlPullParserException("Unknown value under RoamingPartner: " + strArr[0]);
                                    }
                                }
                                return roamingPartner;
                            }

                            private static UpdateParameter deserializeUpdateParameter(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                UpdateParameter updateParameter = new UpdateParameter();
                                while (!XmlUtil.isNextSectionEnd(xmlPullParser, i)) {
                                    String[] strArr = new String[1];
                                    Object currentValue = XmlUtil.readCurrentValue(xmlPullParser, strArr);
                                    if (strArr[0] == null) {
                                        throw new XmlPullParserException("Missing value name");
                                    }
                                    switch (strArr[0]) {
                                        case "UpdateInterval":
                                            updateParameter.setUpdateIntervalInMinutes(((Long) currentValue).longValue());
                                            break;
                                        case "UpdateMethod":
                                            updateParameter.setUpdateMethod((String) currentValue);
                                            break;
                                        case "Restriction":
                                            updateParameter.setRestriction((String) currentValue);
                                            break;
                                        case "ServerURI":
                                            updateParameter.setServerUri((String) currentValue);
                                            break;
                                        case "Username":
                                            updateParameter.setUsername((String) currentValue);
                                            break;
                                        case "Password":
                                            updateParameter.setBase64EncodedPassword((String) currentValue);
                                            break;
                                        case "TrustRootCertURL":
                                            updateParameter.setTrustRootCertUrl((String) currentValue);
                                            break;
                                        case "TrustRootCertSHA256Fingerprint":
                                            updateParameter.setTrustRootCertSha256Fingerprint((byte[]) currentValue);
                                            break;
                                        default:
                                            throw new XmlPullParserException("Unknown value under UpdateParameter: " + strArr[0]);
                                    }
                                }
                                return updateParameter;
                            }

                            private static Map<Integer, String> deserializeProtoPortMap(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
                                HashMap map = new HashMap();
                                while (XmlUtil.gotoNextSectionWithNameOrEnd(xmlPullParser, XML_TAG_SECTION_HEADER_PROTO_PORT, i)) {
                                    int iIntValue = ((Integer) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROTO)).intValue();
                                    map.put(Integer.valueOf(iIntValue), (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PORTS));
                                }
                                return map;
                            }

                            private static boolean isValueElement(XmlPullParser xmlPullParser) {
                                return xmlPullParser.getAttributeValue(null, "name") != null;
                            }
                        }
