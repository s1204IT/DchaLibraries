package com.android.server.telecom;

public final class CallState {
    public static final int ABORTED = 8;
    public static final int ACTIVE = 5;
    public static final int CONNECTING = 1;
    public static final int DIALING = 3;
    public static final int DISCONNECTED = 7;
    public static final int DISCONNECTING = 9;
    public static final int NEW = 0;
    public static final int ON_HOLD = 6;
    public static final int PULLING = 10;
    public static final int RINGING = 4;
    public static final int SELECT_PHONE_ACCOUNT = 2;

    private CallState() {
    }

    public static String toString(int i) {
        switch (i) {
            case NEW:
                return "NEW";
            case 1:
                return "CONNECTING";
            case SELECT_PHONE_ACCOUNT:
                return "SELECT_PHONE_ACCOUNT";
            case DIALING:
                return "DIALING";
            case RINGING:
                return "RINGING";
            case ACTIVE:
                return "ACTIVE";
            case ON_HOLD:
                return "ON_HOLD";
            case DISCONNECTED:
                return "DISCONNECTED";
            case ABORTED:
                return "ABORTED";
            case 9:
                return "DISCONNECTING";
            case PULLING:
                return "PULLING";
            default:
                return "UNKNOWN";
        }
    }
}
