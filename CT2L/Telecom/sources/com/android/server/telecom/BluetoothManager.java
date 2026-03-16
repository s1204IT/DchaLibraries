package com.android.server.telecom;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import com.android.internal.util.IndentingPrintWriter;
import java.util.List;

public class BluetoothManager {
    private long mBluetoothConnectionRequestTime;
    private BluetoothHeadset mBluetoothHeadset;
    private final CallAudioManager mCallAudioManager;
    private final BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            BluetoothManager.this.mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
            Log.v(this, "- Got BluetoothHeadset: " + BluetoothManager.this.mBluetoothHeadset, new Object[0]);
        }

        @Override
        public void onServiceDisconnected(int i) {
            BluetoothManager.this.mBluetoothHeadset = null;
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")) {
                int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                Log.d(this, "mReceiver: HEADSET_STATE_CHANGED_ACTION", new Object[0]);
                Log.d(this, "==> new state: %s ", Integer.valueOf(intExtra));
                BluetoothManager.this.updateBluetoothState();
                return;
            }
            if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
                int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 10);
                Log.d(this, "mReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION", new Object[0]);
                Log.d(this, "==> new state: %s", Integer.valueOf(intExtra2));
                BluetoothManager.this.updateBluetoothState();
            }
        }
    };
    private boolean mBluetoothConnectionPending = false;
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public BluetoothManager(Context context, CallAudioManager callAudioManager) {
        this.mCallAudioManager = callAudioManager;
        if (this.mBluetoothAdapter != null) {
            this.mBluetoothAdapter.getProfileProxy(context, this.mBluetoothProfileServiceListener, 1);
        }
        IntentFilter intentFilter = new IntentFilter("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter);
    }

    boolean isBluetoothAvailable() {
        boolean z;
        Log.v(this, "isBluetoothAvailable()...", new Object[0]);
        if (this.mBluetoothHeadset != null) {
            List<BluetoothDevice> connectedDevices = this.mBluetoothHeadset.getConnectedDevices();
            if (connectedDevices.size() > 0) {
                for (int i = 0; i < connectedDevices.size(); i++) {
                    BluetoothDevice bluetoothDevice = connectedDevices.get(i);
                    Log.v(this, "state = " + this.mBluetoothHeadset.getConnectionState(bluetoothDevice) + "for headset: " + bluetoothDevice, new Object[0]);
                }
                z = true;
            } else {
                z = false;
            }
        }
        Log.v(this, "  ==> " + z, new Object[0]);
        return z;
    }

    boolean isBluetoothAudioConnected() {
        if (this.mBluetoothHeadset == null) {
            Log.v(this, "isBluetoothAudioConnected: ==> FALSE (null mBluetoothHeadset)", new Object[0]);
            return false;
        }
        List<BluetoothDevice> connectedDevices = this.mBluetoothHeadset.getConnectedDevices();
        if (connectedDevices.isEmpty()) {
            return false;
        }
        for (int i = 0; i < connectedDevices.size(); i++) {
            BluetoothDevice bluetoothDevice = connectedDevices.get(i);
            boolean zIsAudioConnected = this.mBluetoothHeadset.isAudioConnected(bluetoothDevice);
            Log.v(this, "isBluetoothAudioConnected: ==> isAudioOn = " + zIsAudioConnected + "for headset: " + bluetoothDevice, new Object[0]);
            if (zIsAudioConnected) {
                return true;
            }
        }
        return false;
    }

    boolean isBluetoothAudioConnectedOrPending() {
        if (isBluetoothAudioConnected()) {
            Log.v(this, "isBluetoothAudioConnectedOrPending: ==> TRUE (really connected)", new Object[0]);
            return true;
        }
        if (this.mBluetoothConnectionPending) {
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mBluetoothConnectionRequestTime;
            if (jElapsedRealtime < 5000) {
                Log.v(this, "isBluetoothAudioConnectedOrPending: ==> TRUE (requested " + jElapsedRealtime + " msec ago)", new Object[0]);
                return true;
            }
            Log.v(this, "isBluetoothAudioConnectedOrPending: ==> FALSE (request too old: " + jElapsedRealtime + " msec ago)", new Object[0]);
            this.mBluetoothConnectionPending = false;
            return false;
        }
        Log.v(this, "isBluetoothAudioConnectedOrPending: ==> FALSE", new Object[0]);
        return false;
    }

    void updateBluetoothState() {
        this.mCallAudioManager.onBluetoothStateChange(this);
    }

    void connectBluetoothAudio() {
        Log.v(this, "connectBluetoothAudio()...", new Object[0]);
        if (this.mBluetoothHeadset != null) {
            this.mBluetoothHeadset.connectAudio();
        }
        this.mBluetoothConnectionPending = true;
        this.mBluetoothConnectionRequestTime = SystemClock.elapsedRealtime();
    }

    void disconnectBluetoothAudio() {
        Log.v(this, "disconnectBluetoothAudio()...", new Object[0]);
        if (this.mBluetoothHeadset != null) {
            this.mBluetoothHeadset.disconnectAudio();
        }
        this.mBluetoothConnectionPending = false;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("isBluetoothAvailable: " + isBluetoothAvailable());
        indentingPrintWriter.println("isBluetoothAudioConnected: " + isBluetoothAudioConnected());
        indentingPrintWriter.println("isBluetoothAudioConnectedOrPending: " + isBluetoothAudioConnectedOrPending());
        if (this.mBluetoothAdapter != null) {
            if (this.mBluetoothHeadset != null) {
                List<BluetoothDevice> connectedDevices = this.mBluetoothHeadset.getConnectedDevices();
                if (connectedDevices.size() > 0) {
                    BluetoothDevice bluetoothDevice = connectedDevices.get(0);
                    indentingPrintWriter.println("BluetoothHeadset.getCurrentDevice: " + bluetoothDevice);
                    indentingPrintWriter.println("BluetoothHeadset.State: " + this.mBluetoothHeadset.getConnectionState(bluetoothDevice));
                    indentingPrintWriter.println("BluetoothHeadset audio connected: " + this.mBluetoothHeadset.isAudioConnected(bluetoothDevice));
                    return;
                }
                return;
            }
            indentingPrintWriter.println("mBluetoothHeadset is null");
            return;
        }
        indentingPrintWriter.println("mBluetoothAdapter is null; device is not BT capable");
    }
}
