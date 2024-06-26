package com.android.systemui.statusbar.policy;

import android.hardware.display.WifiDisplayStatus;
import android.net.wifi.p2p.WifiP2pDevice;
import java.util.Set;
/* loaded from: a.zip:com/android/systemui/statusbar/policy/CastController.class */
public interface CastController extends Listenable {

    /* loaded from: a.zip:com/android/systemui/statusbar/policy/CastController$Callback.class */
    public interface Callback {
        void onCastDevicesChanged();

        void onWfdStatusChanged(WifiDisplayStatus wifiDisplayStatus, boolean z);

        void onWifiP2pDeviceChanged(WifiP2pDevice wifiP2pDevice);
    }

    /* loaded from: a.zip:com/android/systemui/statusbar/policy/CastController$CastDevice.class */
    public static final class CastDevice {
        public String description;
        public String id;
        public String name;
        public int state = 0;
        public Object tag;
    }

    void addCallback(Callback callback);

    Set<CastDevice> getCastDevices();

    WifiP2pDevice getWifiP2pDev();

    boolean isNeedShowWfdSink();

    boolean isWfdSinkSupported();

    void removeCallback(Callback callback);

    void setCurrentUserId(int i);

    void setDiscovering(boolean z);

    void startCasting(CastDevice castDevice);

    void stopCasting(CastDevice castDevice);

    void updateWfdFloatMenu(boolean z);
}
