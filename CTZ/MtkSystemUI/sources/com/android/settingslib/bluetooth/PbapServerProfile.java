package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public class PbapServerProfile implements LocalBluetoothProfile {

    @VisibleForTesting
    public static final String NAME = "PBAP Server";
    private boolean mIsProfileReady;
    private BluetoothPbap mService;
    private static boolean V = true;
    static final ParcelUuid[] PBAB_CLIENT_UUIDS = {BluetoothUuid.HSP, BluetoothUuid.Handsfree, BluetoothUuid.PBAP_PCE};

    private final class PbapServiceListener implements BluetoothPbap.ServiceListener {
        private PbapServiceListener() {
        }

        public void onServiceConnected(BluetoothPbap bluetoothPbap) {
            if (PbapServerProfile.V) {
                Log.d("PbapServerProfile", "Bluetooth service connected");
            }
            PbapServerProfile.this.mService = bluetoothPbap;
            PbapServerProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected() {
            if (PbapServerProfile.V) {
                Log.d("PbapServerProfile", "Bluetooth service disconnected");
            }
            PbapServerProfile.this.mIsProfileReady = false;
        }
    }

    @Override
    public int getProfileId() {
        return 6;
    }

    PbapServerProfile(Context context) {
        new BluetoothPbap(context, new PbapServiceListener());
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isAutoConnectable() {
        return false;
    }

    @Override
    public boolean connect(BluetoothDevice bluetoothDevice) {
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    @Override
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        return (this.mService != null && this.mService.isConnected(bluetoothDevice)) ? 2 : 0;
    }

    @Override
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return false;
    }

    @Override
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
    }

    public String toString() {
        return NAME;
    }

    protected void finalize() {
        if (V) {
            Log.d("PbapServerProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                this.mService.close();
                this.mService = null;
            } catch (Throwable th) {
                Log.w("PbapServerProfile", "Error cleaning up PBAP proxy", th);
            }
        }
    }
}
