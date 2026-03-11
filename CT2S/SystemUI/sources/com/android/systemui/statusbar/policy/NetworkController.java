package com.android.systemui.statusbar.policy;

import android.content.Intent;

public interface NetworkController {

    public interface AccessPointController {

        public static class AccessPoint {
            public boolean hasSecurity;
            public int iconId;
            public boolean isConfigured;
            public boolean isConnected;
            public int level;
            public int networkId;
            public String ssid;
        }

        public interface AccessPointCallback {
            void onAccessPointsChanged(AccessPoint[] accessPointArr);

            void onSettingsActivityTriggered(Intent intent);
        }

        void addAccessPointCallback(AccessPointCallback accessPointCallback);

        boolean canConfigWifi();

        boolean connect(AccessPoint accessPoint);

        void removeAccessPointCallback(AccessPointCallback accessPointCallback);

        void scanForAccessPoints();
    }

    public interface MobileDataController {

        public static class DataUsageInfo {
            public String carrier;
            public long limitLevel;
            public String period;
            public long usageLevel;
            public long warningLevel;
        }

        DataUsageInfo getDataUsageInfo();

        boolean isMobileDataEnabled();

        boolean isMobileDataSupported();

        void setMobileDataEnabled(boolean z);
    }

    public interface NetworkSignalChangedCallback {
        void onAirplaneModeChanged(boolean z);

        void onMobileDataEnabled(boolean z);

        void onMobileDataSignalChanged(boolean z, int i, String str, int i2, boolean z2, boolean z3, String str2, String str3, boolean z4);

        void onNoSimVisibleChanged(boolean z);

        void onWifiSignalChanged(boolean z, boolean z2, int i, boolean z3, boolean z4, String str, String str2);
    }

    void addNetworkSignalChangedCallback(NetworkSignalChangedCallback networkSignalChangedCallback);

    AccessPointController getAccessPointController();

    MobileDataController getMobileDataController();

    boolean hasMobileDataFeature();

    void onUserSwitched(int i);

    void removeNetworkSignalChangedCallback(NetworkSignalChangedCallback networkSignalChangedCallback);

    void setWifiEnabled(boolean z);
}
