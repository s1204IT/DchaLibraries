package com.android.systemui.statusbar.policy;

import java.util.Set;

public interface BluetoothController {

    public interface Callback {
        void onBluetoothPairedDevicesChanged();

        void onBluetoothStateChange(boolean z, boolean z2);
    }

    public static final class PairedDevice {
        public String id;
        public String name;
        public int state = STATE_DISCONNECTED;
        public Object tag;
        public static int STATE_DISCONNECTED = 0;
        public static int STATE_CONNECTING = 1;
        public static int STATE_CONNECTED = 2;
        public static int STATE_DISCONNECTING = 3;
    }

    void addStateChangedCallback(Callback callback);

    void connect(PairedDevice pairedDevice);

    void disconnect(PairedDevice pairedDevice);

    String getLastDeviceName();

    Set<PairedDevice> getPairedDevices();

    boolean isBluetoothConnected();

    boolean isBluetoothConnecting();

    boolean isBluetoothEnabled();

    boolean isBluetoothSupported();

    void removeStateChangedCallback(Callback callback);

    void setBluetoothEnabled(boolean z);
}
