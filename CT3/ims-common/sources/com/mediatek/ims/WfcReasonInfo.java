package com.mediatek.ims;

public class WfcReasonInfo {
    public static final int CODE_UNSPECIFIED = 999;
    public static final int CODE_WFC_403_AUTH_SCHEME_UNSUPPORTED = 1604;
    public static final int CODE_WFC_403_HANDSET_BLACKLISTED = 1605;
    public static final int CODE_WFC_403_MISMATCH_IDENTITIES = 1603;
    public static final int CODE_WFC_403_ROAMING_NOT_ALLOWED = 1602;
    public static final int CODE_WFC_403_UNKNOWN_USER = 1601;
    public static final int CODE_WFC_911_MISSING = 1701;
    public static final int CODE_WFC_ANY_OTHER_CONN_ERROR = 1407;
    public static final int CODE_WFC_DEFAULT = 100;
    public static final int CODE_WFC_DNS_RECV_NAPTR_QUERY_RSP_ERROR = 1201;
    public static final int CODE_WFC_DNS_RECV_RSP_QUERY_ERROR = 1203;
    public static final int CODE_WFC_DNS_RECV_RSP_SRV_QUERY_ERROR = 1202;
    public static final int CODE_WFC_DNS_RESOLVE_FQDN_ERROR = 1041;
    public static final int CODE_WFC_EPDG_CON_OR_LOCAL_OR_NULL_PTR_ERROR = 1081;
    public static final int CODE_WFC_EPDG_IPSEC_SETUP_ERROR = 1082;
    public static final int CODE_WFC_INCORRECT_SIM_CARD_ERROR = 1301;
    public static final int CODE_WFC_INTERNAL_SERVER_ERROR = 1406;
    public static final int CODE_WFC_LOCAL_OR_NULL_PTR_ERROR = 1401;
    public static final int CODE_WFC_NO_AVAILABLE_QUALIFIED_MOBILE_NETWORK = 2004;
    public static final int CODE_WFC_RNS_ALLOWED_RADIO_DENY = 2006;
    public static final int CODE_WFC_RNS_ALLOWED_RADIO_NONE = 2007;
    public static final int CODE_WFC_SERVER_CERT_INVALID_ERROR = 1504;
    public static final int CODE_WFC_SERVER_CERT_VALIDATION_ERROR = 1501;
    public static final int CODE_WFC_SERVER_IPSEC_CERT_INVALID_ERROR = 1111;
    public static final int CODE_WFC_SERVER_IPSEC_CERT_VALIDATION_ERROR = 1101;
    public static final int CODE_WFC_SUCCESS = 99;
    public static final int CODE_WFC_TLS_CONN_ERROR = 1405;
    public static final int CODE_WFC_UNABLE_TO_COMPLETE_CALL = 2003;
    public static final int CODE_WFC_UNABLE_TO_COMPLETE_CALL_CD = 2005;
    public static final int CODE_WFC_WIFI_SIGNAL_LOST = 2001;

    public static int getImsStatusCodeString(int status) {
        switch (status) {
            case CODE_WFC_SUCCESS:
                return 134545675;
            case CODE_WFC_DEFAULT:
                return 134545676;
            case CODE_WFC_DNS_RESOLVE_FQDN_ERROR:
                return 134545682;
            case CODE_WFC_EPDG_CON_OR_LOCAL_OR_NULL_PTR_ERROR:
                return 134545685;
            case CODE_WFC_EPDG_IPSEC_SETUP_ERROR:
                return 134545686;
            case CODE_WFC_SERVER_IPSEC_CERT_VALIDATION_ERROR:
                return 134545691;
            case CODE_WFC_SERVER_IPSEC_CERT_INVALID_ERROR:
                return 134545692;
            case CODE_WFC_DNS_RECV_NAPTR_QUERY_RSP_ERROR:
                return 134545679;
            case CODE_WFC_DNS_RECV_RSP_SRV_QUERY_ERROR:
                return 134545680;
            case CODE_WFC_DNS_RECV_RSP_QUERY_ERROR:
                return 134545681;
            case CODE_WFC_INCORRECT_SIM_CARD_ERROR:
                return 134545683;
            case CODE_WFC_LOCAL_OR_NULL_PTR_ERROR:
                return 134545684;
            case CODE_WFC_TLS_CONN_ERROR:
                return 134545687;
            case CODE_WFC_INTERNAL_SERVER_ERROR:
                return 134545688;
            case CODE_WFC_SERVER_CERT_VALIDATION_ERROR:
                return 134545690;
            case CODE_WFC_SERVER_CERT_INVALID_ERROR:
                return 134545693;
            case CODE_WFC_403_UNKNOWN_USER:
                return 134545694;
            case CODE_WFC_403_ROAMING_NOT_ALLOWED:
                return 134545695;
            case CODE_WFC_403_MISMATCH_IDENTITIES:
                return 134545696;
            case CODE_WFC_403_AUTH_SCHEME_UNSUPPORTED:
                return 134545697;
            case CODE_WFC_403_HANDSET_BLACKLISTED:
                return 134545698;
            case CODE_WFC_911_MISSING:
                return 134545699;
            case CODE_WFC_RNS_ALLOWED_RADIO_DENY:
                return 134545677;
            case CODE_WFC_RNS_ALLOWED_RADIO_NONE:
                return 134545678;
            default:
                return 134545689;
        }
    }
}
