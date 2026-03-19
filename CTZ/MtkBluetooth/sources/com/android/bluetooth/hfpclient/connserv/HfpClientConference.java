package com.android.bluetooth.hfpclient.connserv;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import android.util.Log;

public class HfpClientConference extends Conference {
    private static final String TAG = "HfpClientConference";
    private BluetoothDevice mDevice;
    private BluetoothHeadsetClient mHeadsetProfile;

    public HfpClientConference(PhoneAccountHandle phoneAccountHandle, BluetoothDevice bluetoothDevice, BluetoothHeadsetClient bluetoothHeadsetClient) {
        super(phoneAccountHandle);
        this.mDevice = bluetoothDevice;
        this.mHeadsetProfile = bluetoothHeadsetClient;
        setConnectionCapabilities((HfpClientConnectionService.hasHfpClientEcc(bluetoothHeadsetClient, bluetoothDevice) ? 128 : 0) | 3);
        setActive();
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "onDisconnect");
        this.mHeadsetProfile.terminateCall(this.mDevice, (BluetoothHeadsetClientCall) null);
    }

    @Override
    public void onMerge(Connection connection) {
        Log.d(TAG, "onMerge " + connection);
        addConnection(connection);
    }

    @Override
    public void onSeparate(Connection connection) {
        Log.d(TAG, "onSeparate " + connection);
        ((HfpClientConnection) connection).enterPrivateMode();
        removeConnection(connection);
    }

    @Override
    public void onHold() {
        Log.d(TAG, "onHold");
        this.mHeadsetProfile.holdCall(this.mDevice);
    }

    @Override
    public void onUnhold() {
        Log.d(TAG, "onUnhold");
        this.mHeadsetProfile.acceptCall(this.mDevice, 1);
    }

    @Override
    public void onPlayDtmfTone(char c) {
        Log.d(TAG, "onPlayDtmfTone " + c);
        if (this.mHeadsetProfile != null) {
            this.mHeadsetProfile.sendDTMF(this.mDevice, (byte) c);
        }
    }

    @Override
    public void onConnectionAdded(Connection connection) {
        Log.d(TAG, "onConnectionAdded " + connection);
        if (connection.getState() == 5 && getState() == 4) {
            connection.onAnswer();
        } else if (connection.getState() == 4 && getState() == 5) {
            this.mHeadsetProfile.acceptCall(this.mDevice, 0);
        }
    }
}
