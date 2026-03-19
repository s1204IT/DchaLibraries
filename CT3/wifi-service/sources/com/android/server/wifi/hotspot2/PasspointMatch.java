package com.android.server.wifi.hotspot2;

public enum PasspointMatch {
    HomeProvider,
    RoamingProvider,
    Incomplete,
    None,
    Declined;

    public static PasspointMatch[] valuesCustom() {
        return values();
    }
}
