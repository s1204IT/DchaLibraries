package com.android.internal.telephony;

import android.telephony.Rlog;

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
        INVALID_PARAMETER,
        SUBSCRIPTION_NOT_SUPPORTED,
        DIAL_MODIFIED_TO_USSD,
        DIAL_MODIFIED_TO_SS,
        DIAL_MODIFIED_TO_DIAL,
        USSD_MODIFIED_TO_DIAL,
        USSD_MODIFIED_TO_SS,
        USSD_MODIFIED_TO_USSD,
        SS_MODIFIED_TO_DIAL,
        SS_MODIFIED_TO_USSD,
        SS_MODIFIED_TO_SS
    }

    public CommandException(Error e) {
        super(e.toString());
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
            case SmsHeader.ELT_ID_STANDARD_WVG_OBJECT:
                break;
            case SmsHeader.ELT_ID_CHARACTER_SIZE_WVG_OBJECT:
                break;
            case SmsHeader.ELT_ID_EXTENDED_OBJECT_DATA_REQUEST_CMD:
                break;
            case 27:
                break;
            case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
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
