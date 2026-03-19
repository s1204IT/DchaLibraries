package com.android.server.telecom;

public class HandoverState {
    public static String stateToString(int i) {
        switch (i) {
            case 1:
                return "NONE";
            case CallState.SELECT_PHONE_ACCOUNT:
                return "HANDOVER_TO_STARTED";
            case CallState.DIALING:
                return "HANDOVER_FROM_STARTED";
            case CallState.RINGING:
                return "HANDOVER_ACCEPTED";
            case CallState.ACTIVE:
                return "HANDOVER_COMPLETE";
            case CallState.ON_HOLD:
                return "HANDOVER_FAILED";
            default:
                return "";
        }
    }
}
