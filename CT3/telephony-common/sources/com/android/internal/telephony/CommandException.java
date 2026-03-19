package com.android.internal.telephony;

import android.telephony.Rlog;
import com.android.internal.telephony.cat.CatService;
import com.mediatek.internal.telephony.gsm.GsmVTProvider;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;

public class CommandException extends RuntimeException {
    private Error mError;

    public enum Error {
        INVALID_RESPONSE,
        RADIO_NOT_AVAILABLE,
        GENERIC_FAILURE,
        PASSWORD_INCORRECT,
        SIM_PIN2,
        SIM_PUK2,
        REQUEST_NOT_SUPPORTED,
        OP_NOT_ALLOWED_DURING_VOICE_CALL,
        OP_NOT_ALLOWED_BEFORE_REG_NW,
        SMS_FAIL_RETRY,
        SIM_ABSENT,
        SUBSCRIPTION_NOT_AVAILABLE,
        MODE_NOT_SUPPORTED,
        FDN_CHECK_FAILURE,
        ILLEGAL_SIM_OR_ME,
        MISSING_RESOURCE,
        NO_SUCH_ELEMENT,
        SUBSCRIPTION_NOT_SUPPORTED,
        DIAL_MODIFIED_TO_USSD,
        DIAL_MODIFIED_TO_SS,
        DIAL_MODIFIED_TO_DIAL,
        USSD_MODIFIED_TO_DIAL,
        USSD_MODIFIED_TO_SS,
        USSD_MODIFIED_TO_USSD,
        SS_MODIFIED_TO_DIAL,
        SS_MODIFIED_TO_USSD,
        SS_MODIFIED_TO_SS,
        INVALID_PARAMETER,
        DIAL_STRING_TOO_LONG,
        TEXT_STRING_TOO_LONG,
        SIM_MEM_FULL,
        SIM_ALREADY_POWERED_OFF,
        SIM_ALREADY_POWERED_ON,
        SIM_DATA_NOT_AVAILABLE,
        SIM_SAP_CONNECT_FAILURE,
        SIM_SAP_MSG_SIZE_TOO_LARGE,
        SIM_SAP_MSG_SIZE_TOO_SMALL,
        SIM_SAP_CONNECT_OK_CALL_ONGOING,
        LCE_NOT_SUPPORTED,
        NO_MEMORY,
        INTERNAL_ERR,
        SYSTEM_ERR,
        MODEM_ERR,
        INVALID_STATE,
        NO_RESOURCES,
        SIM_ERR,
        INVALID_ARGUMENTS,
        INVALID_SIM_STATE,
        INVALID_MODEM_STATE,
        INVALID_CALL_ID,
        NO_SMS_TO_ACK,
        NETWORK_ERR,
        REQUEST_RATE_LIMITED,
        SIM_BUSY,
        SIM_FULL,
        NETWORK_REJECT,
        OPERATION_NOT_ALLOWED,
        EMPTY_RECORD,
        INVALID_SMS_FORMAT,
        ENCODING_ERR,
        INVALID_SMSC_ADDRESS,
        NO_SUCH_ENTRY,
        NETWORK_NOT_READY,
        NOT_PROVISIONED,
        NO_SUBSCRIPTION,
        NO_NETWORK_FOUND,
        DEVICE_IN_USE,
        ABORTED,
        OEM_ERROR_1,
        OEM_ERROR_2,
        OEM_ERROR_3,
        OEM_ERROR_4,
        OEM_ERROR_5,
        OEM_ERROR_6,
        OEM_ERROR_7,
        OEM_ERROR_8,
        OEM_ERROR_9,
        OEM_ERROR_10,
        OEM_ERROR_11,
        OEM_ERROR_12,
        OEM_ERROR_13,
        OEM_ERROR_14,
        OEM_ERROR_15,
        OEM_ERROR_16,
        OEM_ERROR_17,
        OEM_ERROR_18,
        OEM_ERROR_19,
        OEM_ERROR_20,
        OEM_ERROR_21,
        OEM_ERROR_22,
        OEM_ERROR_23,
        OEM_ERROR_24,
        OEM_ERROR_25,
        ADDITIONAL_NUMBER_STRING_TOO_LONG,
        ADDITIONAL_NUMBER_SAVE_FAILURE,
        ADN_LIST_NOT_EXIST,
        EMAIL_SIZE_LIMIT,
        EMAIL_NAME_TOOLONG,
        SNE_SIZE_LIMIT,
        SNE_NAME_TOOLONG,
        NOT_READY,
        CALL_BARRED,
        UT_XCAP_403_FORBIDDEN,
        UT_UNKNOWN_HOST,
        UT_XCAP_404_NOT_FOUND,
        UT_XCAP_409_CONFLICT,
        SPECAIL_UT_COMMAND_NOT_SUPPORTED,
        CC_CALL_HOLD_FAILED_CAUSED_BY_TERMINATED;

        public static Error[] valuesCustom() {
            return values();
        }
    }

    public CommandException(Error e) {
        super(e.toString());
        this.mError = e;
    }

    public CommandException(Error e, String errString) {
        super(errString);
        this.mError = e;
    }

    public static CommandException fromRilErrno(int ril_errno) {
        switch (ril_errno) {
            case -1:
                break;
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 8:
                break;
            case 9:
                break;
            case 10:
                break;
            case 11:
                break;
            case 12:
                break;
            case 13:
                break;
            case 14:
                break;
            case 15:
                break;
            case 16:
                break;
            case 17:
                break;
            case 18:
                break;
            case 19:
                break;
            case 20:
                break;
            case 21:
                break;
            case 22:
                break;
            case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
                break;
            case 24:
                break;
            case 25:
                break;
            case 26:
                break;
            case CallFailCause.DESTINATION_OUT_OF_ORDER:
                break;
            case CallFailCause.FACILITY_REJECTED:
                break;
            case 30:
                break;
            case 31:
                break;
            case 32:
                break;
            case 33:
                break;
            case 34:
                break;
            case 35:
                break;
            case 36:
                break;
            case 37:
                break;
            case 38:
                break;
            case 39:
                break;
            case 40:
                break;
            case 41:
                break;
            case 42:
                break;
            case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                break;
            case CallFailCause.CHANNEL_NOT_AVAIL:
                break;
            case 45:
                break;
            case CatService.MSG_ID_CACHED_DISPLAY_TEXT_TIMEOUT:
                break;
            case 47:
                break;
            case 48:
                break;
            case 49:
                break;
            case 50:
                break;
            case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                break;
            case RadioNVItems.RIL_NV_CDMA_BC10:
                break;
            case 53:
                break;
            case RadioNVItems.RIL_NV_CDMA_SO68:
                break;
            case 55:
                break;
            case 56:
                break;
            case 57:
                break;
            case 58:
                break;
            case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
                break;
            case 60:
                break;
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_2:
                break;
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_3:
                break;
            case 63:
                break;
            case 64:
                break;
            case CallFailCause.BEARER_NOT_IMPLEMENT:
                break;
            case 501:
                break;
            case 502:
                break;
            case 503:
                break;
            case 504:
                break;
            case 505:
                break;
            case 506:
                break;
            case 507:
                break;
            case 508:
                break;
            case 509:
                break;
            case 510:
                break;
            case 511:
                break;
            case 512:
                break;
            case 513:
                break;
            case 514:
                break;
            case 515:
                break;
            case 516:
                break;
            case 517:
                break;
            case 518:
                break;
            case 519:
                break;
            case 520:
                break;
            case 521:
                break;
            case 522:
                break;
            case 523:
                break;
            case 524:
                break;
            case 525:
                break;
            case 1001:
                break;
            case 1002:
                break;
            case 1003:
                break;
            case 1005:
                break;
            case 1006:
                break;
            case 1007:
                break;
            case 1008:
                break;
            case GsmVTProvider.SESSION_EVENT_START_COUNTER:
                break;
            case 1011:
                break;
            case 1012:
                break;
            case TelephonyEventLog.TAG_IMS_CALL_ACCEPT:
                break;
            case 6000:
                break;
            default:
                Rlog.e("GSM", "Unrecognized RIL errno " + ril_errno);
                break;
        }
        return new CommandException(Error.INVALID_RESPONSE);
    }

    public Error getCommandError() {
        return this.mError;
    }
}
