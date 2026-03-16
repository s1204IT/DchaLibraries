package com.android.internal.telephony.dataconnection;

import android.R;
import android.content.res.Resources;
import java.util.HashMap;

public enum DcFailCause {
    NONE(0),
    OPERATOR_BARRED(8),
    INSUFFICIENT_RESOURCES(26),
    MISSING_UNKNOWN_APN(27),
    UNKNOWN_PDP_ADDRESS_TYPE(28),
    USER_AUTHENTICATION(29),
    ACTIVATION_REJECT_GGSN(30),
    ACTIVATION_REJECT_UNSPECIFIED(31),
    SERVICE_OPTION_NOT_SUPPORTED(32),
    SERVICE_OPTION_NOT_SUBSCRIBED(33),
    SERVICE_OPTION_OUT_OF_ORDER(34),
    NSAPI_IN_USE(35),
    REGULAR_DEACTIVATION(36),
    ONLY_IPV4_ALLOWED(50),
    ONLY_IPV6_ALLOWED(51),
    ONLY_SINGLE_BEARER_ALLOWED(52),
    PROTOCOL_ERRORS(111),
    REGISTRATION_FAIL(-1),
    GPRS_REGISTRATION_FAIL(-2),
    SIGNAL_LOST(-3),
    PREF_RADIO_TECH_CHANGED(-4),
    RADIO_POWER_OFF(-5),
    TETHERED_CALL_ACTIVE(-6),
    ERROR_UNSPECIFIED(65535),
    UNKNOWN(65536),
    RADIO_NOT_AVAILABLE(65537),
    UNACCEPTABLE_NETWORK_PARAMETER(65538),
    CONNECTION_TO_DATACONNECTIONAC_BROKEN(65539),
    LOST_CONNECTION(65540),
    RESET_BY_FRAMEWORK(65541);

    private static final HashMap<Integer, DcFailCause> sErrorCodeToFailCauseMap = new HashMap<>();
    private final int mErrorCode;
    private final boolean mRestartRadioOnRegularDeactivation = Resources.getSystem().getBoolean(R.^attr-private.magnifierElevation);

    static {
        DcFailCause[] arr$ = values();
        for (DcFailCause fc : arr$) {
            sErrorCodeToFailCauseMap.put(Integer.valueOf(fc.getErrorCode()), fc);
        }
    }

    DcFailCause(int errorCode) {
        this.mErrorCode = errorCode;
    }

    public int getErrorCode() {
        return this.mErrorCode;
    }

    public boolean isRestartRadioFail() {
        return this == REGULAR_DEACTIVATION && this.mRestartRadioOnRegularDeactivation;
    }

    public boolean isPermanentFail() {
        return this == OPERATOR_BARRED || this == MISSING_UNKNOWN_APN || this == UNKNOWN_PDP_ADDRESS_TYPE || this == USER_AUTHENTICATION || this == ACTIVATION_REJECT_GGSN || this == SERVICE_OPTION_NOT_SUPPORTED || this == SERVICE_OPTION_NOT_SUBSCRIBED || this == NSAPI_IN_USE || this == ONLY_IPV4_ALLOWED || this == ONLY_IPV6_ALLOWED || this == PROTOCOL_ERRORS || this == RADIO_POWER_OFF || this == TETHERED_CALL_ACTIVE || this == RADIO_NOT_AVAILABLE || this == UNACCEPTABLE_NETWORK_PARAMETER || this == SIGNAL_LOST;
    }

    public boolean isEventLoggable() {
        return this == OPERATOR_BARRED || this == INSUFFICIENT_RESOURCES || this == UNKNOWN_PDP_ADDRESS_TYPE || this == USER_AUTHENTICATION || this == ACTIVATION_REJECT_GGSN || this == ACTIVATION_REJECT_UNSPECIFIED || this == SERVICE_OPTION_NOT_SUBSCRIBED || this == SERVICE_OPTION_NOT_SUPPORTED || this == SERVICE_OPTION_OUT_OF_ORDER || this == NSAPI_IN_USE || this == ONLY_IPV4_ALLOWED || this == ONLY_IPV6_ALLOWED || this == PROTOCOL_ERRORS || this == SIGNAL_LOST || this == RADIO_POWER_OFF || this == TETHERED_CALL_ACTIVE || this == UNACCEPTABLE_NETWORK_PARAMETER;
    }

    public static DcFailCause fromInt(int errorCode) {
        DcFailCause fc = sErrorCodeToFailCauseMap.get(Integer.valueOf(errorCode));
        if (fc == null) {
            return UNKNOWN;
        }
        return fc;
    }
}
