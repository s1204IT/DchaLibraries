package com.mediatek.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDun;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

public final class DunServerProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] DUN_CLIENT_UUIDS = {BluetoothUuid.DUN};
    private boolean mIsProfileReady;
    private BluetoothDun mService;

    private final class DunServiceListener implements BluetoothDun.ServiceListener {
        DunServiceListener(DunServerProfile this$0, DunServiceListener dunServiceListener) {
            this();
        }

        private DunServiceListener() {
        }

        public void onServiceConnected(BluetoothDun proxy) {
            Log.d("DunServerProfile", "Bluetooth Dun service connected");
            DunServerProfile.this.mService = proxy;
            DunServerProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected() {
            Log.d("DunServerProfile", "Bluetooth Dun service disconnected");
            DunServerProfile.this.mIsProfileReady = false;
        }
    }

    public DunServerProfile(Context context) {
        new BluetoothDun(context, new DunServiceListener(this, null));
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
    public boolean connect(BluetoothDevice device) {
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.disconnect(device);
    }

    @Override
    public int getConnectionStatus(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        return this.mService.getState(device);
    }

    @Override
    public boolean isPreferred(BluetoothDevice device) {
        return false;
    }

    @Override
    public void setPreferred(BluetoothDevice device, boolean preferred) {
    }

    public String toString() {
        return "DUN Server";
    }

    protected void finalize() {
        Log.d("DunServerProfile", "finalize()");
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.close();
            this.mService = null;
        } catch (Throwable t) {
            Log.w("DunServerProfile", "Error cleaning up PBAP proxy", t);
        }
    }
}
