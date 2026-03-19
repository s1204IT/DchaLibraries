package mediatek.telephony;

import android.telephony.DisconnectCause;

public class MtkDisconnectCause extends DisconnectCause {
    public static final int ACCESS_INFORMATION_DISCARDED = 1024;
    public static final int BEARER_NOT_AUTHORIZED = 1011;
    public static final int BEARER_NOT_AVAIL = 1012;
    public static final int BEARER_NOT_IMPLEMENT = 1014;
    public static final int CALL_REJECTED = 1004;
    public static final int CAUSE_ADDRESS_INCOMPLETE = 1524;
    public static final int CAUSE_AMBIGUOUS = 1525;
    public static final int CAUSE_BAD_EXTENSION = 1517;
    public static final int CAUSE_BAD_GATEWAY = 1531;
    public static final int CAUSE_BAD_REQUEST = 1501;
    public static final int CAUSE_BUSY_EVERYWHERE = 1536;
    public static final int CAUSE_BUSY_HERE = 1526;
    public static final int CAUSE_CALL_TRANSACTION_NOT_EXIST = 1521;
    public static final int CAUSE_CONFLICT = 1510;
    public static final int CAUSE_DECLINE = 1537;
    public static final int CAUSE_DOES_NOT_EXIST_ANYWHERE = 1538;
    public static final int CAUSE_EXTENSION_REQUIRED = 1518;
    public static final int CAUSE_FORBIDDEN = 1504;
    public static final int CAUSE_GATEWAY_TIMEOUT = 1533;
    public static final int CAUSE_GONE = 1511;
    public static final int CAUSE_INTERVAL_TOO_BRIEF = 1519;
    public static final int CAUSE_LENGTH_REQUIRED = 1512;
    public static final int CAUSE_LOOP_DETECTED = 1522;
    public static final int CAUSE_MESSAGE_TOO_LONG = 1535;
    public static final int CAUSE_METHOD_NOT_ALLOWED = 1506;
    public static final int CAUSE_MOVED_PERMANENTLY = 1500;
    public static final int CAUSE_NOT_ACCEPTABLE = 1507;
    public static final int CAUSE_NOT_ACCEPTABLE_HERE = 1528;
    public static final int CAUSE_NOT_FOUND = 1505;
    public static final int CAUSE_NOT_IMPLEMENTED = 1530;
    public static final int CAUSE_PAYMENT_REQUIRED = 1503;
    public static final int CAUSE_PROXY_AUTHENTICATION_REQUIRED = 1508;
    public static final int CAUSE_REQUEST_ENTRY_TOO_LONG = 1513;
    public static final int CAUSE_REQUEST_TERMINATED = 1527;
    public static final int CAUSE_REQUEST_TIMEOUT = 1509;
    public static final int CAUSE_REQUEST_URI_TOO_LONG = 1514;
    public static final int CAUSE_SERVER_INTERNAL_ERROR = 1529;
    public static final int CAUSE_SERVICE_UNAVAILABLE = 1532;
    public static final int CAUSE_SESSION_NOT_ACCEPTABLE = 1539;
    public static final int CAUSE_TEMPORARILY_UNAVAILABLE = 1520;
    public static final int CAUSE_TOO_MANY_HOPS = 1523;
    public static final int CAUSE_UNAUTHORIZED = 1502;
    public static final int CAUSE_UNSUPPORTED_MEDIA_TYPE = 1515;
    public static final int CAUSE_UNSUPPORTED_URI_SCHEME = 1516;
    public static final int CAUSE_VERSION_NOT_SUPPORTED = 1534;
    public static final int CHANNEL_UNACCEPTABLE = 1019;
    public static final int CM_MM_RR_CONNECTION_RELEASE = 1040;
    public static final int CONDITIONAL_IE_ERROR = 1035;
    public static final int DESTINATION_OUT_OF_ORDER = 1023;
    public static final int ECC_OVER_WIFI_UNSUPPORTED = 1042;
    public static final int FACILITY_NOT_IMPLEMENT = 1015;
    public static final int FACILITY_REJECTED = 1006;
    public static final int IE_NON_EXISTENT_OR_NOT_IMPLEMENTED = 1034;
    public static final int IMS_EMERGENCY_REREG = 380;
    public static final int INCOMING_CALL_BARRED_WITHIN_CUG = 1026;
    public static final int INCOMPATIBLE_DESTINATION = 1018;
    public static final int INTERWORKING_UNSPECIFIED = 1039;
    public static final int INVALID_MANDATORY_INFORMATION = 1031;
    public static final int INVALID_NUMBER_FORMAT = 1005;
    public static final int INVALID_TRANSACTION_ID_VALUE = 1027;
    public static final int INVALID_TRANSIT_NETWORK_SELECTION = 1029;
    public static final int MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE = 1036;
    public static final int MESSAGE_TYPE_NON_EXISTENT = 1032;
    public static final int MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE = 1033;
    public static final int MTK_DISCONNECTED_CAUSE_BASE = 1000;
    public static final int NON_SELECTED_USER_CLEARING = 1022;
    public static final int NORMAL_UNSPECIFIED = 1007;
    public static final int NO_CIRCUIT_AVAIL = 1008;
    public static final int NO_ROUTE_TO_DESTINATION = 1001;
    public static final int NO_USER_RESPONDING = 1002;
    public static final int OPERATOR_DETERMINED_BARRING = 1020;
    public static final int OPTION_NOT_AVAILABLE = 1017;
    public static final int OUTGOING_CANCELED_BY_SERVICE = 1041;
    public static final int PRE_EMPTION = 1021;
    public static final int PROTOCOL_ERROR_UNSPECIFIED = 1038;
    public static final int RECOVERY_ON_TIMER_EXPIRY = 1037;
    public static final int REQUESTED_FACILITY_NOT_SUBSCRIBED = 1025;
    public static final int RESOURCE_UNAVAILABLE = 1010;
    public static final int RESTRICTED_BEARER_AVAILABLE = 1016;
    public static final int SEMANTICALLY_INCORRECT_MESSAGE = 1030;
    public static final int SERVICE_NOT_AVAILABLE = 1013;
    public static final int SWITCHING_CONGESTION = 1009;
    public static final int USER_ALERTING_NO_ANSWER = 1003;
    public static final int USER_NOT_MEMBER_OF_CUG = 1028;
    public static final int VOLTE_SS_DATA_OFF = 1044;
    public static final int WFC_CALL_DROP_BACKHAUL_CONGESTION = 1046;
    public static final int WFC_CALL_DROP_BAD_RSSI = 1045;
    public static final int WFC_HANDOVER_LTE_FAIL = 403;
    public static final int WFC_HANDOVER_WIFI_FAIL = 402;
    public static final int WFC_ISP_PROBLEM = 401;
    public static final int WFC_UNAVAILABLE_IN_CURRENT_LOCATION = 1043;
    public static final int WFC_WIFI_SIGNAL_LOST = 400;

    public static String toString(int i) {
        switch (i) {
            case 1001:
                return "NO_ROUTE_TO_DESTINATION";
            case 1002:
                return "NO_USER_RESPONDING";
            case 1003:
                return "USER_ALERTING_NO_ANSWER";
            case 1004:
                return "CALL_REJECTED";
            case 1005:
                return "INVALID_NUMBER_FORMAT";
            case 1006:
                return "FACILITY_REJECTED";
            case 1007:
                return "NORMAL_UNSPECIFIED";
            case 1008:
                return "NO_CIRCUIT_AVAIL";
            case 1009:
                return "SWITCHING_CONGESTION";
            case 1010:
                return "RESOURCE_UNAVAILABLE";
            case BEARER_NOT_AUTHORIZED:
                return "BEARER_NOT_AUTHORIZED";
            case BEARER_NOT_AVAIL:
                return "BEARER_NOT_AVAIL";
            case SERVICE_NOT_AVAILABLE:
                return "SERVICE_NOT_AVAILABLE";
            case BEARER_NOT_IMPLEMENT:
                return "BEARER_NOT_IMPLEMENT";
            case FACILITY_NOT_IMPLEMENT:
                return "FACILITY_NOT_IMPLEMENT";
            case RESTRICTED_BEARER_AVAILABLE:
                return "RESTRICTED_BEARER_AVAILABLE";
            case OPTION_NOT_AVAILABLE:
                return "OPTION_NOT_AVAILABLE";
            case INCOMPATIBLE_DESTINATION:
                return "INCOMPATIBLE_DESTINATION";
            case CHANNEL_UNACCEPTABLE:
                return "CHANNEL_UNACCEPTABLE";
            case OPERATOR_DETERMINED_BARRING:
                return "OPERATOR_DETERMINED_BARRING";
            case PRE_EMPTION:
                return "PRE_EMPTION";
            case NON_SELECTED_USER_CLEARING:
                return "NON_SELECTED_USER_CLEARING";
            case DESTINATION_OUT_OF_ORDER:
                return "DESTINATION_OUT_OF_ORDER";
            case 1024:
                return "ACCESS_INFORMATION_DISCARDED";
            case REQUESTED_FACILITY_NOT_SUBSCRIBED:
                return "REQUESTED_FACILITY_NOT_SUBSCRIBED";
            case INCOMING_CALL_BARRED_WITHIN_CUG:
                return "INCOMING_CALL_BARRED_WITHIN_CUG";
            case INVALID_TRANSACTION_ID_VALUE:
                return "INVALID_TRANSACTION_ID_VALUE";
            case USER_NOT_MEMBER_OF_CUG:
                return "USER_NOT_MEMBER_OF_CUG";
            case INVALID_TRANSIT_NETWORK_SELECTION:
                return "INVALID_TRANSIT_NETWORK_SELECTION";
            case SEMANTICALLY_INCORRECT_MESSAGE:
                return "SEMANTICALLY_INCORRECT_MESSAGE";
            case INVALID_MANDATORY_INFORMATION:
                return "INVALID_MANDATORY_INFORMATION";
            case MESSAGE_TYPE_NON_EXISTENT:
                return "MESSAGE_TYPE_NON_EXISTENT";
            case MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
                return "MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE";
            case IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
                return "IE_NON_EXISTENT_OR_NOT_IMPLEMENTED";
            case CONDITIONAL_IE_ERROR:
                return "CONDITIONAL_IE_ERROR";
            case MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE:
                return "MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE";
            case RECOVERY_ON_TIMER_EXPIRY:
                return "RECOVERY_ON_TIMER_EXPIRY";
            case PROTOCOL_ERROR_UNSPECIFIED:
                return "PROTOCOL_ERROR_UNSPECIFIED";
            case INTERWORKING_UNSPECIFIED:
                return "INTERWORKING_UNSPECIFIED";
            case CM_MM_RR_CONNECTION_RELEASE:
                return "CM_MM_RR_CONNECTION_RELEASE";
            case OUTGOING_CANCELED_BY_SERVICE:
                return "OUTGOING_CANCELED_BY_SERVICE";
            case ECC_OVER_WIFI_UNSUPPORTED:
                return "ECC_OVER_WIFI_UNSUPPORTED";
            case WFC_UNAVAILABLE_IN_CURRENT_LOCATION:
                return "WFC_UNAVAILABLE_IN_CURRENT_LOCATION";
            case VOLTE_SS_DATA_OFF:
                return "VOLTE_SS_DATA_OFF";
            case WFC_CALL_DROP_BAD_RSSI:
                return "WFC_CALL_DROP_BAD_RSSI";
            case WFC_CALL_DROP_BACKHAUL_CONGESTION:
                return "WFC_CALL_DROP_BACKHAUL_CONGESTION";
            default:
                switch (i) {
                    case CAUSE_MOVED_PERMANENTLY:
                        return "CAUSE_MOVED_PERMANENTLY";
                    case CAUSE_BAD_REQUEST:
                        return "CAUSE_BAD_REQUEST";
                    case CAUSE_UNAUTHORIZED:
                        return "CAUSE_UNAUTHORIZED";
                    case CAUSE_PAYMENT_REQUIRED:
                        return "CAUSE_PAYMENT_REQUIRED";
                    case CAUSE_FORBIDDEN:
                        return "CAUSE_FORBIDDEN";
                    case CAUSE_NOT_FOUND:
                        return "CAUSE_NOT_FOUND";
                    case CAUSE_METHOD_NOT_ALLOWED:
                        return "CAUSE_METHOD_NOT_ALLOWED";
                    case CAUSE_NOT_ACCEPTABLE:
                        return "CAUSE_NOT_ACCEPTABLE";
                    case CAUSE_PROXY_AUTHENTICATION_REQUIRED:
                        return "CAUSE_PROXY_AUTHENTICATION_REQUIRED";
                    case CAUSE_REQUEST_TIMEOUT:
                        return "CAUSE_REQUEST_TIMEOUT";
                    case CAUSE_CONFLICT:
                        return "CAUSE_CONFLICT";
                    case CAUSE_GONE:
                        return "CAUSE_GONE";
                    case CAUSE_LENGTH_REQUIRED:
                        return "CAUSE_LENGTH_REQUIRED";
                    case CAUSE_REQUEST_ENTRY_TOO_LONG:
                        return "CAUSE_REQUEST_ENTRY_TOO_LONG";
                    case CAUSE_REQUEST_URI_TOO_LONG:
                        return "CAUSE_REQUEST_URI_TOO_LONG";
                    case CAUSE_UNSUPPORTED_MEDIA_TYPE:
                        return "CAUSE_UNSUPPORTED_MEDIA_TYPE";
                    case CAUSE_UNSUPPORTED_URI_SCHEME:
                        return "CAUSE_UNSUPPORTED_URI_SCHEME";
                    case CAUSE_BAD_EXTENSION:
                        return "CAUSE_BAD_EXTENSION";
                    case CAUSE_EXTENSION_REQUIRED:
                        return "CAUSE_EXTENSION_REQUIRED";
                    case CAUSE_INTERVAL_TOO_BRIEF:
                        return "CAUSE_INTERVAL_TOO_BRIEF";
                    case CAUSE_TEMPORARILY_UNAVAILABLE:
                        return "CAUSE_TEMPORARILY_UNAVAILABLE";
                    case CAUSE_CALL_TRANSACTION_NOT_EXIST:
                        return "CAUSE_CALL_TRANSACTION_NOT_EXIST";
                    case CAUSE_LOOP_DETECTED:
                        return "CAUSE_LOOP_DETECTED";
                    case CAUSE_TOO_MANY_HOPS:
                        return "CAUSE_TOO_MANY_HOPS";
                    case CAUSE_ADDRESS_INCOMPLETE:
                        return "CAUSE_ADDRESS_INCOMPLETE";
                    case CAUSE_AMBIGUOUS:
                        return "CAUSE_AMBIGUOUS";
                    case CAUSE_BUSY_HERE:
                        return "CAUSE_BUSY_HERE";
                    case CAUSE_REQUEST_TERMINATED:
                        return "CAUSE_REQUEST_TERMINATED";
                    case CAUSE_NOT_ACCEPTABLE_HERE:
                        return "CAUSE_NOT_ACCEPTABLE_HERE";
                    case CAUSE_SERVER_INTERNAL_ERROR:
                        return "CAUSE_SERVER_INTERNAL_ERROR";
                    case CAUSE_NOT_IMPLEMENTED:
                        return "CAUSE_NOT_IMPLEMENTED";
                    case CAUSE_BAD_GATEWAY:
                        return "CAUSE_BAD_GATEWAY";
                    case CAUSE_SERVICE_UNAVAILABLE:
                        return "CAUSE_SERVICE_UNAVAILABLE";
                    case CAUSE_GATEWAY_TIMEOUT:
                        return "CAUSE_GATEWAY_TIMEOUT";
                    case CAUSE_VERSION_NOT_SUPPORTED:
                        return "CAUSE_VERSION_NOT_SUPPORTED";
                    case CAUSE_MESSAGE_TOO_LONG:
                        return "CAUSE_MESSAGE_TOO_LONG";
                    case CAUSE_BUSY_EVERYWHERE:
                        return "CAUSE_BUSY_EVERYWHERE";
                    case CAUSE_DECLINE:
                        return "CAUSE_DECLINE";
                    case CAUSE_DOES_NOT_EXIST_ANYWHERE:
                        return "CAUSE_DOES_NOT_EXIST_ANYWHERE";
                    case CAUSE_SESSION_NOT_ACCEPTABLE:
                        return "CAUSE_SESSION_NOT_ACCEPTABLE";
                    default:
                        return DisconnectCause.toString(i);
                }
        }
    }
}
