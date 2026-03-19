package com.android.server.connectivity.tethering;

import android.net.LinkProperties;

public class IControlsTethering {
    public static final int STATE_AVAILABLE = 1;
    public static final int STATE_LOCAL_ONLY = 3;
    public static final int STATE_TETHERED = 2;
    public static final int STATE_UNAVAILABLE = 0;

    public static String getStateString(int i) {
        switch (i) {
            case 0:
                return "UNAVAILABLE";
            case 1:
                return "AVAILABLE";
            case 2:
                return "TETHERED";
            case 3:
                return "LOCAL_ONLY";
            default:
                return "UNKNOWN: " + i;
        }
    }

    public void updateInterfaceState(TetherInterfaceStateMachine tetherInterfaceStateMachine, int i, int i2) {
    }

    public void updateLinkProperties(TetherInterfaceStateMachine tetherInterfaceStateMachine, LinkProperties linkProperties) {
    }
}
