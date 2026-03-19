package com.mediatek.server.wifi;

import android.util.Log;
import com.android.server.wifi.util.NativeUtil;
import vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapdCallback;

public class MtkHostapdHalCallback extends IHostapdCallback.Stub {
    private static final String TAG = "MtkHostapdHalCallback";

    @Override
    public void onStaAuthorized(byte[] bArr) {
        try {
            String strMacAddressFromByteArray = NativeUtil.macAddressFromByteArray(bArr);
            Log.e(TAG, "STA: " + strMacAddressFromByteArray + "is connected");
            MtkWifiApMonitor.broadcastApStaConnected(MtkHostapdHal.getIfaceName(), strMacAddressFromByteArray);
        } catch (Exception e) {
            Log.e(TAG, "Could not decode MAC address.", e);
        }
    }

    @Override
    public void onStaDeauthorized(byte[] bArr) {
        try {
            String strMacAddressFromByteArray = NativeUtil.macAddressFromByteArray(bArr);
            Log.e(TAG, "STA: " + strMacAddressFromByteArray + "is disconnected");
            MtkWifiApMonitor.broadcastApStaDisconnected(MtkHostapdHal.getIfaceName(), strMacAddressFromByteArray);
        } catch (Exception e) {
            Log.e(TAG, "Could not decode MAC address.", e);
        }
    }
}
